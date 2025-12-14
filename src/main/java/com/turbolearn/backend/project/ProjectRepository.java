package com.turbolearn.backend.project;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface ProjectRepository extends MongoRepository<Project, String> {

    List<Project> findByTenantId(String tenantId);
}
