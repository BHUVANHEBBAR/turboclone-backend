package com.turbolearn.backend.project;

import com.turbolearn.backend.task.Task;
import com.turbolearn.backend.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;


    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }


    //Create task with tenantId

    public Task createTask(Task task){
        String tenantId = TenantContext.getTenant();
        task.setTenantId(tenantId);
        return taskRepository.save(task);
    }

    //Get all tasks
    public List<Task> listAllTasks(String  projectId){
        String tenantId = TenantContext.getTenant();
        return taskRepository.findByTenantIdAndProjectId(tenantId, projectId);
    }

    //Fetch single task by id, for current tenant
    public Optional<Task> getTaskById(String id){
        String tenantId = TenantContext.getTenant();
        return taskRepository.findById(id).
                filter(t -> tenantId.equals(t.getTenantId()));

    }

    /**
     * Update a task fields (title, description, status) only if it belongs to tenant.
     * Returns Optional.of(updated) or Optional.empty() if not found / not owned.
     */
    public Optional<Task> updateTask(String id, Task updated){
        String tenantId = TenantContext.getTenant();

        return taskRepository.findById(id)
                .filter(t -> tenantId.equals(t.getTenantId()))
                .map(existing -> {
                    if(updated.getTitle()!= null) existing.setTitle(updated.getTitle());
                    if(updated.getDescription() != null) existing.setDescription(updated.getDescription());
                    if(updated.getStatus() !=null) existing.setStatus(updated.getStatus());
                    return taskRepository.save(existing);
                });
    }

    //Delete task safely (with tenant check)

    public boolean deleteTask(String id){
        String tenantId = TenantContext.getTenant();

        return taskRepository.findById(id)
                .filter(t -> tenantId.equals(t.getTenantId()))
                .map(t-> {
                    taskRepository.delete(t);
                    return true;
                })
                .orElse(false);
    }

}
