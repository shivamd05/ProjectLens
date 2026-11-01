package com.projectlense.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectLenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectLenseApplication.class, args);
    }
}
