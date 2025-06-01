package com.taskapp;

import com.taskapp.messaging.config.MessagingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MessagingProperties.class)
public class TaskAppApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TaskAppApplication.class, args);
    }
}
