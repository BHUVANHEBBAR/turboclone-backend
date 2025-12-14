package com.turbolearn.backend.resource;

import com.turbolearn.backend.job.Job;
import com.turbolearn.backend.job.JobRepository;
import com.turbolearn.backend.job.JobStatus;
import com.turbolearn.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.queue.name}")
    private String queueName;

    private final String uploadDir;

    public ResourceService(@Value("${app.upload.dir}") String uploadDir){
        this.uploadDir = uploadDir;
    }

    public void enqueueJob(String jobId){
        redisTemplate.opsForList().leftPush(queueName, jobId);
    }


    public String saveFileToDisk(MultipartFile file) throws IOException{
        String originalFileName = file.getOriginalFilename();

        String uniqueFileName = UUID.randomUUID() + "-" + originalFileName;

        File destination = new File(uploadDir + File.separator + uniqueFileName);

        destination.getParentFile().mkdirs();

        file.transferTo(destination);

        return destination.getAbsolutePath();
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
        res.setType("pdf");
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
            String absolutePath = saveFileToDisk(file);

            Resource resource = createResourceRecord(
                    absolutePath,
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
