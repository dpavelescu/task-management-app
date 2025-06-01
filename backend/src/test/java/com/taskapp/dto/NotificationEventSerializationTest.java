package com.taskapp.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for NotificationEvent JSON serialization/deserialization.
 * This ensures Redis messaging will work correctly across different pods.
 */
class NotificationEventSerializationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    @DisplayName("Should serialize and deserialize NotificationEvent with all fields")
    void testFullNotificationEventSerialization() throws JsonProcessingException {
        // Given: A complete NotificationEvent
        NotificationEvent originalEvent = new NotificationEvent();
        originalEvent.setId("test-id-123");
        originalEvent.setType("TASK_CREATED");
        originalEvent.setMessage("Task created: Test Task");
        originalEvent.setTaskId("456");
        originalEvent.setTaskTitle("Test Task");
        originalEvent.setUsername("user123");
        originalEvent.setTimestamp(LocalDateTime.of(2025, 5, 31, 10, 30, 0));
        originalEvent.setCreatorUsername("creator123");
        originalEvent.setAssignedUsername("assignee123");
        
        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // Then: JSON should contain all fields
        assertAll(
            () -> assertTrue(json.contains("\"id\":\"test-id-123\"")),
            () -> assertTrue(json.contains("\"type\":\"TASK_CREATED\"")),
            () -> assertTrue(json.contains("\"message\":\"Task created: Test Task\"")),
            () -> assertTrue(json.contains("\"taskId\":\"456\"")),
            () -> assertTrue(json.contains("\"taskTitle\":\"Test Task\"")),
            () -> assertTrue(json.contains("\"username\":\"user123\"")),
            () -> assertTrue(json.contains("\"timestamp\":\"2025-05-31T10:30:00\"")),
            () -> assertTrue(json.contains("\"creatorUsername\":\"creator123\"")),
            () -> assertTrue(json.contains("\"assignedUsername\":\"assignee123\""))
        );
        
        // When: Deserialize back to object
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        
        // Then: All fields should match exactly
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
            () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
            () -> assertEquals(originalEvent.getTaskId(), deserializedEvent.getTaskId()),
            () -> assertEquals(originalEvent.getTaskTitle(), deserializedEvent.getTaskTitle()),
            () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp()),
            () -> assertEquals(originalEvent.getCreatorUsername(), deserializedEvent.getCreatorUsername()),
            () -> assertEquals(originalEvent.getAssignedUsername(), deserializedEvent.getAssignedUsername())
        );
    }
    
    @Test
    @DisplayName("Should serialize and deserialize NotificationEvent with minimal fields")
    void testMinimalNotificationEventSerialization() throws JsonProcessingException {
        // Given: A minimal NotificationEvent
        NotificationEvent originalEvent = NotificationEvent.create(
            "TASK_UPDATED", 
            "Task updated", 
            "user123"
        );
        
        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // Then: JSON should contain required fields
        assertAll(
            () -> assertTrue(json.contains("\"type\":\"TASK_UPDATED\"")),
            () -> assertTrue(json.contains("\"message\":\"Task updated\"")),
            () -> assertTrue(json.contains("\"username\":\"user123\"")),
            () -> assertTrue(json.contains("\"timestamp\"")),
            () -> assertTrue(json.contains("\"id\""))
        );
        
        // When: Deserialize back to object
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        
        // Then: All fields should match
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
            () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
            () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp()),
            () -> assertNull(deserializedEvent.getTaskId()),
            () -> assertNull(deserializedEvent.getTaskTitle()),
            () -> assertNull(deserializedEvent.getCreatorUsername()),
            () -> assertNull(deserializedEvent.getAssignedUsername())
        );
    }
    
    @Test
    @DisplayName("Should serialize and deserialize task notification using factory method")
    void testTaskNotificationFactoryMethodSerialization() throws JsonProcessingException {
        // Given: A task notification created via factory method
        NotificationEvent originalEvent = NotificationEvent.createTaskNotification(
            "TASK_ASSIGNED",
            "New task assigned: Important Task",
            "assignee123",
            "789",
            "Important Task"
        );
        
        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // When: Deserialize back to object
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        
        // Then: All fields should match exactly
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals("TASK_ASSIGNED", deserializedEvent.getType()),
            () -> assertEquals("New task assigned: Important Task", deserializedEvent.getMessage()),
            () -> assertEquals("assignee123", deserializedEvent.getUsername()),
            () -> assertEquals("789", deserializedEvent.getTaskId()),
            () -> assertEquals("Important Task", deserializedEvent.getTaskTitle()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should serialize and deserialize comprehensive task notification")
    void testComprehensiveTaskNotificationSerialization() throws JsonProcessingException {
        // Given: A comprehensive task notification
        NotificationEvent originalEvent = NotificationEvent.createTaskNotification(
            "TASK_CREATED",
            "Task created: Review PR",
            "developer123",
            "101",
            "Review PR",
            "manager456",
            "developer123"
        );
        
        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // When: Deserialize back to object
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        
        // Then: All fields should match exactly
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals("TASK_CREATED", deserializedEvent.getType()),
            () -> assertEquals("Task created: Review PR", deserializedEvent.getMessage()),
            () -> assertEquals("developer123", deserializedEvent.getUsername()),
            () -> assertEquals("101", deserializedEvent.getTaskId()),
            () -> assertEquals("Review PR", deserializedEvent.getTaskTitle()),
            () -> assertEquals("manager456", deserializedEvent.getCreatorUsername()),
            () -> assertEquals("developer123", deserializedEvent.getAssignedUsername()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should handle null fields gracefully during serialization")
    void testNullFieldsSerialization() throws JsonProcessingException {
        // Given: NotificationEvent with some null fields
        NotificationEvent originalEvent = new NotificationEvent();
        originalEvent.setId("test-123");
        originalEvent.setType("TASK_DELETED");
        originalEvent.setMessage("Task deleted");
        originalEvent.setUsername("user123");
        originalEvent.setTimestamp(LocalDateTime.now());
        // Leave taskId, taskTitle, creatorUsername, assignedUsername as null
        
        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // Then: Null fields should not appear in JSON (due to @JsonInclude(Include.NON_NULL))
        assertAll(
            () -> assertFalse(json.contains("\"taskId\"")),
            () -> assertFalse(json.contains("\"taskTitle\"")),
            () -> assertFalse(json.contains("\"creatorUsername\"")),
            () -> assertFalse(json.contains("\"assignedUsername\""))
        );
        
        // When: Deserialize back to object
        NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
        
        // Then: Null fields should remain null
        assertAll(
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
            () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
            () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
            () -> assertNull(deserializedEvent.getTaskId()),
            () -> assertNull(deserializedEvent.getTaskTitle()),
            () -> assertNull(deserializedEvent.getCreatorUsername()),
            () -> assertNull(deserializedEvent.getAssignedUsername())
        );
    }
    
    @Test
    @DisplayName("Should deserialize JSON with unknown fields gracefully")
    void testDeserializationWithUnknownFields() throws JsonProcessingException {
        // Given: JSON with extra unknown fields
        String jsonWithExtraFields = """
            {
                "id": "test-456",
                "type": "TASK_UPDATED",
                "message": "Task status changed",
                "username": "user456",
                "timestamp": "2025-05-31T14:30:00",
                "taskId": "789",
                "taskTitle": "Test Task",
                "unknownField1": "should be ignored",
                "unknownField2": 123,
                "unknownNestedObject": {
                    "nested": "value"
                }
            }
            """;
        
        // When: Deserialize with unknown fields
        NotificationEvent deserializedEvent = objectMapper.readValue(jsonWithExtraFields, NotificationEvent.class);
        
        // Then: Known fields should be correctly deserialized, unknown fields ignored
        assertAll(
            () -> assertEquals("test-456", deserializedEvent.getId()),
            () -> assertEquals("TASK_UPDATED", deserializedEvent.getType()),
            () -> assertEquals("Task status changed", deserializedEvent.getMessage()),
            () -> assertEquals("user456", deserializedEvent.getUsername()),
            () -> assertEquals("789", deserializedEvent.getTaskId()),
            () -> assertEquals("Test Task", deserializedEvent.getTaskTitle()),
            () -> assertEquals(LocalDateTime.of(2025, 5, 31, 14, 30, 0), deserializedEvent.getTimestamp())
        );
    }
    
    @Test
    @DisplayName("Should generate unique IDs for different notification events")
    void testUniqueIdGeneration() {
        // When: Create multiple events
        NotificationEvent event1 = NotificationEvent.create("TYPE1", "Message1", "user1");
        NotificationEvent event2 = NotificationEvent.create("TYPE2", "Message2", "user2");
        NotificationEvent event3 = NotificationEvent.create("TYPE3", "Message3", "user3");
        
        // Then: All IDs should be unique
        assertAll(
            () -> assertNotNull(event1.getId()),
            () -> assertNotNull(event2.getId()),
            () -> assertNotNull(event3.getId()),
            () -> assertNotEquals(event1.getId(), event2.getId()),
            () -> assertNotEquals(event2.getId(), event3.getId()),
            () -> assertNotEquals(event1.getId(), event3.getId())
        );
    }
}
