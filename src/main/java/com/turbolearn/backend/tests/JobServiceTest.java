package com.turbolearn.backend.tests;

import com.turbolearn.backend.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobServiceTest implements CommandLineRunner {

    @Autowired
    private final ProjectRepository projectRepository;

    @Override
    public void run(String... args) throws Exception {

        try{
//            projectRepository.save(new Project("t1","tenant1"));
            System.out.println("Stored in MongoDB test user");
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
}
