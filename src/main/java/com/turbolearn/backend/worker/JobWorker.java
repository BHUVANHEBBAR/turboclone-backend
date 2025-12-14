package com.turbolearn.backend.worker;

import com.turbolearn.backend.ai.GroqClientService;
import com.turbolearn.backend.ai.prompts.PromptTemplates;
import com.turbolearn.backend.job.Job;
import com.turbolearn.backend.job.JobRepository;
import com.turbolearn.backend.job.JobType;
import com.turbolearn.backend.resource.Resource;
import com.turbolearn.backend.resource.ResourceRepository;
import com.turbolearn.backend.tenant.TenantContext;
import com.turbolearn.backend.job.JobStatus;
import com.turbolearn.backend.utils.PdfUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class JobWorker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String queueName;
    private final JobRepository jobRepository;
    private final ResourceRepository resourceRepository;
    private final GroqClientService groqClientService;


    public JobWorker(RedisTemplate<String, Object> redisTemplate,
                     @Value("${redis.queue.name}") String queueName,
                     JobRepository jobRepository,
                     ResourceRepository resourceRepository,
                     GroqClientService groqClientService
                     ){
        this.redisTemplate=redisTemplate;
        this.queueName=queueName;
        this.jobRepository = jobRepository;
        this.resourceRepository=resourceRepository;
        this.groqClientService=groqClientService;
    }

    @Scheduled(fixedDelay = 2000)
    public void poll(){

        Object raw = redisTemplate.opsForList().rightPop(queueName);

        System.out.println("Redis read queue "+raw);
        if(raw==null) return;

        String jobId = raw.toString();
        System.out.println("Redis jobId "+jobId);
        Job job = jobRepository.findById(jobId).orElse(null);
        if(job == null) return;

        TenantContext.setTenant(job.getTenantId());

        try{
            if(job.getStatus() == JobStatus.PROCESSING){
                System.out.println("JobWorker job already processing "+jobId);
                return;
            }

        job.setStatus(JobStatus.PROCESSING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        Resource resource = resourceRepository.findById(job.getResourceId()).orElse(null);

        if(resource==null){
            markJobFailed(job, "resource not found");
            return;
        }


        String extracted ="";
        String type = resource.getType() == null ? "" : resource.getType().toLowerCase();

            if (type.equals("pdf")) {
                extracted = PdfUtil.extractText(resource.getStoragePath());
            } else if (type.equals("txt") || type.equals("md")) {
                try {
                    extracted = Files.readString(Path.of(resource.getStoragePath()));
                } catch (Exception e) {
                    // fallback
                    extracted = "";
                }
            } else {
                // fallback attempt: try reading as text file
                try {
                    extracted = Files.readString(Path.of(resource.getStoragePath()));
                } catch (Exception e) {
                    extracted = "";
                }
            }

            if (extracted == null || extracted.isBlank()) {
                // nothing to process: mark failed (or you could still set DONE with empty result)
                markJobFailed(job, "no text extracted from resource");
                return;
            }

            // Choose prompt based on jobType (enum)
            String aiOutput;
            JobType jobType = job.getJobType() == null ? JobType.SUMMARY : job.getJobType();

            switch (jobType) {
                case SUMMARY -> {
                    String prompt = PromptTemplates.summary(extracted);
                    aiOutput = groqClientService.generate(prompt);
                    Map<String,Object> result = new HashMap<>();
                    result.put("summary", aiOutput.trim());
                    job.setResult(result);
                }
                case FLASHCARDS -> {
                    String prompt = PromptTemplates.flashcards(extracted);
                    aiOutput = groqClientService.generate(prompt);
                    Map<String,Object> result = new HashMap<>();
                    // keep raw text under "raw" and also attempt small parse into list if output follows Q/A lines
                    result.put("flashcards_raw", aiOutput.trim());
                    result.put("flashcards_parsed", parseFlashcards(aiOutput));
                    job.setResult(result);
                }
                case QUIZ -> {
                    String prompt = PromptTemplates.quiz(extracted);
                    aiOutput = groqClientService.generate(prompt);
                    Map<String,Object> result = new HashMap<>();
                    result.put("quiz_raw", aiOutput.trim());
                    job.setResult(result);
                }
                default -> {
                    Map<String,Object> result = new HashMap<>();
                    result.put("error", "unsupported job type: " + jobType);
                    job.setResult(result);
                }
            }

        job.setStatus(JobStatus.DONE);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);


        redisTemplate.convertAndSend(
                "jobs:completed",
                Map.of("jobId", job.getId(), "tenantId", job.getTenantId())
        );

        TenantContext.clear();
        Thread.sleep(300);

    } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void markJobFailed(Job job, String errorMsg) {
        try{
            job.setStatus(JobStatus.FAILED);
            job.setUpdatedAt(Instant.now());
            Map<String,Object> res = job.getResult() == null ? new HashMap<>() : new HashMap<>(job.getResult());
            res.put("errorMessage", errorMsg);
            job.setResult(res);
            jobRepository.save(job);
            System.out.println("JobWorker job Failed "+ job.getId() + " reason =" +errorMsg);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Object parseFlashcards(String raw){
        try{
            String[] lines = raw.split("\\r?\\n");
            var list = new java.util.ArrayList<Map<String,String>>();
            Map<String,String> current = null;

            for(String ln: lines){
                ln = ln.trim();
                if(ln.startsWith("Q:") || ln.startsWith("q:")){
                    current = new HashMap<>();
                    current.put("question", ln.substring(2).trim());
                    list.add(current);
                }
                else if((ln.startsWith("A:") || ln.startsWith("a:")) && current != null){
                    current.put("answer", ln.substring(2).trim());
                }
            }
            return list;
        }catch (Exception e){
            return Map.of("raw", raw);
        }

    }
    }
