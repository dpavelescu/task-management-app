package com.taskapp.enums;

public enum TaskStatus {
    PENDING("PENDING", "Task is pending"),
    IN_PROGRESS("IN_PROGRESS", "Task is in progress"), 
    COMPLETED("COMPLETED", "Task is completed"),
    CANCELLED("CANCELLED", "Task is cancelled");

    private final String value;
    private final String description;

    TaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static TaskStatus fromValue(String value) {
        if (value == null) {
            return PENDING; // Default status
        }
        
        for (TaskStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid task status: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
