package com.turbolearn.backend.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "resources")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String ownerId;
    private String title;
    private String type;
    private String storagePath;
    private long size;
    private ResourceStatus status;
    private Instant createdAt;


}
