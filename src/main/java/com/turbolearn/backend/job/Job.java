package com.turbolearn.backend.job;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "job")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String resourceId;

    private JobType jobType;
    private JobStatus status;
    private String params;
    private Map<String, Object> result;
    private Instant createdAt;
    private Instant updatedAt;
}
