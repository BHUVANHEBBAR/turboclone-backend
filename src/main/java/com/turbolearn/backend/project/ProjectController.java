package com.turbolearn.backend.project;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service){
        this.service = service;
    }

    //CREATE
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project){
        Project saved = service.createProject(project);
        return ResponseEntity.ok(saved);
    }

    //LIST
    @GetMapping
    public ResponseEntity<List<Project>> getAll(){
        List<Project> projectList = service.getAllProjects();
        return ResponseEntity.ok(projectList);
    }

    //GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Project> getById(@PathVariable String id){
        return service.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
        boolean deleted = service.deleteProject(id);
        return deleted
                ?ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
