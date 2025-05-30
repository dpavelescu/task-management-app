package com.taskapp.exception;

public class TaskPermissionException extends RuntimeException {
    public TaskPermissionException(String message) {
        super(message);
    }
    
    public TaskPermissionException(String username, Long taskId, String operation) {
        super(String.format("User '%s' does not have permission to %s task %d", username, operation, taskId));
    }
}
