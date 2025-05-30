package com.taskapp.enums;

public enum TaskPriority {
    LOW("LOW", "Low priority"),
    MEDIUM("MEDIUM", "Medium priority"),
    HIGH("HIGH", "High priority"),
    URGENT("URGENT", "Urgent priority");

    private final String value;
    private final String description;

    TaskPriority(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static TaskPriority fromValue(String value) {
        if (value == null) {
            return MEDIUM; // Default priority
        }
        
        for (TaskPriority priority : values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Invalid task priority: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
