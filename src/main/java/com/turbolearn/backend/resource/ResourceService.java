package com.turbolearn.backend.resource;

import com.turbolearn.backend.job.Job;
import com.turbolearn.backend.job.JobRepository;
import com.turbolearn.backend.job.JobStatus;
import com.turbolearn.backend.tenant.TenantContext;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final OkHttpClient httpClient= new OkHttpClient.Builder().build();

    @Value("${redis.queue.name}")
    private String queueName;

    private final String uploadDir;

    public ResourceService(@Value("${app.upload.dir}") String uploadDir){
        this.uploadDir = uploadDir;
    }

    public void enqueueJob(String jobId){
        redisTemplate.opsForList().leftPush(queueName, jobId);
    }


    public String saveFileToCloud(MultipartFile file, String ownerId) throws IOException{
        byte[] fileBytes = file.getBytes();

        RequestBody fileBody = RequestBody.create(
                fileBytes,
                MediaType.parse(Objects.requireNonNull(file.getContentType()))
        );

        MultipartBody requestBody = new MultipartBody.Builder().
                setType(MultipartBody.FORM)
                .addFormDataPart(
                        "userId",
                        ownerId
                )
                .addFormDataPart(
                        "file",
                        file.getOriginalFilename(),
                        fileBody
                ).
                build();

        Request request = new Request.Builder()
                .url(uploadDir+"files/upload")
                .post(requestBody)
                .build();

        System.out.println(requestBody.parts());

        try(Response response = httpClient.newCall(request).execute()){
            if(!response.isSuccessful()){
                String errorBody = response.body().string();

                throw new RuntimeException(
                        "Cloud upload failed. Code=" + response.code() +
                                " Body=" + errorBody
                );
            }

            String responseBody = response.body().string();

            JSONObject json = new JSONObject(responseBody);

            System.out.println(json.getString("url"));

            return uploadDir.concat(json.getString("url"));
        }

    }

    public Resource createResourceRecord(String absolutePath,
                                         String originalName,
                                         long size,
                                         String ownerId){
        String tenantId = TenantContext.getTenant();

        Resource res = new Resource();
        res.setTenantId(tenantId);
        res.setOwnerId(ownerId);
        res.setTitle(originalName);
        res.setType(res.getType());
        res.setStoragePath(absolutePath);
        res.setSize(size);
        res.setStatus(ResourceStatus.UPLOADED);
        res.setCreatedAt(Instant.now());

        return resourceRepository.save(res);//resrepo
    }

    public Job createJobForResource(String resourceId, String jobType){
        String tenantId = TenantContext.getTenant();

        Job job = new Job();
        job.setTenantId(tenantId);
        job.setResourceId(jobType);
        job.setResourceId(resourceId);
        job.setStatus(JobStatus.QUEUED);
        job.setParams("");
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        return jobRepository.save(job);
    }


    public Resource handleUpload(MultipartFile file,String title, String jobType, String ownerId){
        try {
            String cloudUrl = saveFileToCloud(file, ownerId);

            Resource resource = createResourceRecord(
                    cloudUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    ownerId
            );

            Job job = createJobForResource(resource.getId(), jobType);

            enqueueJob(job.getId());

            return resource;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
