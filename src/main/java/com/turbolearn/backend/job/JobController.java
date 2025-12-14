package com.turbolearn.backend.job;

import com.turbolearn.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId){
        String tenantId = TenantContext.getTenant();

        Job job = jobRepository.findById(jobId).orElse(null);

        if(job == null)
            return ResponseEntity.status(404)
                    .body(Map.of("error","Job not found"));

        if(!tenantId.equals(job.getTenantId())){
            return ResponseEntity.status(403)
                    .body(Map.of("error","Forbidden"));
        }

        return ResponseEntity.ok(
                Map.of(
                        "jobId", jobId,
                        "status", job.getStatus()
                )
        );
    }

    @GetMapping("/{jobId}/result")
    public ResponseEntity<?> getJobResult(@PathVariable String jobId){

        String tenantId = TenantContext.getTenant();

        Job job = jobRepository.findById(jobId).orElse(null);

        if(job == null){
            return ResponseEntity.status(404)
                    .body(Map.of("error","Job not found"));
        }

        if(!tenantId.equals(job.getTenantId())){
            return ResponseEntity.status(403)
                    .body(Map.of("error","Forbidden"));
        }

        switch (job.getStatus()) {
            case DONE -> {
                return ResponseEntity.ok(
                        Map.of(
                                "jobId", jobId,
                                "resourceId", job.getResourceId(),
                                "result", job.getResult()
                        )
                );
            }

            case PROCESSING -> {
                return ResponseEntity.ok(
                        Map.of(
                                "status", "PROCESSING",
                                "message", "Result not ready yet"
                        )
                );
            }
            case FAILED -> {
                return ResponseEntity.ok(
                        Map.of(
                                "status", "FAILED",
                                "message", "AI processing failed"
                        )
                );
            }

            default -> {
                return ResponseEntity.ok(
                        Map.of(
                                "status", "QUEUED"
                        )
                );
            }

        }
    }
}
