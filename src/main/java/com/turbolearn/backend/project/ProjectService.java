package com.turbolearn.backend.project;

import com.turbolearn.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {


    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @CacheEvict(value = "projects", key = "T(com.turbolearn.backend.utils.CacheKeyUtil).projectListKey()")
    public Project createProject(Project project){
        String tenantId = TenantContext.getTenant();

        project.setTenantId(tenantId);

        return projectRepository.save(project);

    }

    //List all projects
    @Cacheable(value = "projects", key = "T(com.turbolearn.backend.utils.CacheKeyUtil).projectListKey()")
    public List<Project> getAllProjects(){
        String tenantId = TenantContext.getTenant();
        return projectRepository.findByTenantId(tenantId);
    }

    //Get specific project i.e. belonging to current tenant
    public Optional<Project> getProjectById(String id){
        String tenantId= TenantContext.getTenant();

        return projectRepository.findById(id).
                filter(p -> p.getTenantId().equals(tenantId));
    }

    //Delete project safely
    @CacheEvict(value = "projects", key = "T(com.turbolearn.backend.utils.CacheKeyUtil).projectListKey()")
    public boolean deleteProject(String id){
        String tenantId = TenantContext.getTenant();

        Optional<Project> projectOpt = projectRepository.findById(id).
                filter(p -> p.getTenantId().equals(tenantId));

        if(projectOpt.isPresent()){
            projectRepository.delete(projectOpt.get());
            return true;
        }
        return false;
    }


}
