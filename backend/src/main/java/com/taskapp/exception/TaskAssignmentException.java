package com.taskapp.exception;

public class TaskAssignmentException extends RuntimeException {
    public TaskAssignmentException(String message) {
        super(message);
    }
    
    public TaskAssignmentException(Long assigneeId) {
        super("Cannot assign task to user with id: " + assigneeId);
    }
}
