package com.turbolearn.backend.resource;

import com.turbolearn.backend.tenant.TenantContext;
import jakarta.websocket.OnClose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("/upload")
    public ResponseEntity<Object> uploadResource(@RequestPart("file") MultipartFile file,
                                                 @RequestHeader(value = "X-User-ID", required = false) String ownerId,
                                                 @RequestParam(value = "jobType", required = false, defaultValue = "FLASHCARDS") String jobType){

        if(file == null || file.isEmpty()){
            return ResponseEntity.badRequest().body("File is required");
        }

        Resource resource = resourceService.handleUpload(file,
                file.getOriginalFilename(),
                jobType,
                ownerId
                );
        return ResponseEntity.status(202)
                .body(Map.of("resourceId", resource.getId()));

    }
}
