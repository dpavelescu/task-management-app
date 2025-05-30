package com.taskapp.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }
    
    public TaskNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public TaskNotFoundException(Long taskId) {
        super("Task not found with id: " + taskId);
    }
}
