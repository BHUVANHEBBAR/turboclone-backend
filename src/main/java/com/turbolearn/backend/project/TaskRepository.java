package com.turbolearn.backend.project;

import com.turbolearn.backend.task.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    //Fetch all tasks for a specific tenant + project
    List<Task> findByTenantIdAndProjectId(String tenantId, String projectId);

    //fetch all tasks of a tenant
    List<Task> findByTenantId(String tenantId);
}
