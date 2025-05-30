package com.taskapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaskAppApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TaskAppApplication.class, args);
    }
}
