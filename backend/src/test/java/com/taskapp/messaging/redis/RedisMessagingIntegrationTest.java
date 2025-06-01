package com.taskapp.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskapp.dto.NotificationEvent;
import com.taskapp.service.SSEConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Redis messaging components.
 * Tests the complete serialization/deserialization flow through Redis pub/sub.
 */
class RedisMessagingIntegrationTest {    private RedisMessagePublisher publisher;
    private RedisMessageConsumer consumer;
    private RedisTemplate<String, String> redisTemplate;
    private SSEConnectionManager sseConnectionManager;
    private ObjectMapper objectMapper;
      @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // Setup ObjectMapper with time module
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());        // Mock Redis components
        redisTemplate = mock(RedisTemplate.class);
        sseConnectionManager = mock(SSEConnectionManager.class);
          
        // Create real instances
        publisher = new RedisMessagePublisher(redisTemplate, objectMapper);
        consumer = new RedisMessageConsumer(sseConnectionManager, objectMapper);
    }
    
    @Test
    @DisplayName("Should publish NotificationEvent as JSON to Redis topic")
    void testPublishNotificationEvent() throws Exception {
        // Given: A notification event
        NotificationEvent event = NotificationEvent.createTaskNotification(
            "TASK_CREATED",
            "Task created: Test Task",
            "user123",
            "456",
            "Test Task",
            "creator123",
            "assignee123"
        );
        
        String topic = "test-topic";
        
        // When: Publish the event
        publisher.publishMessage(topic, event);
          // Then: Redis should receive the serialized JSON
        verify(redisTemplate).convertAndSend(eq(topic), argThat((String json) -> {
            try {
                // Verify the JSON can be deserialized back to NotificationEvent
                NotificationEvent deserializedEvent = objectMapper.readValue(json, NotificationEvent.class);
                return event.getType().equals(deserializedEvent.getType()) &&
                       event.getMessage().equals(deserializedEvent.getMessage()) &&
                       event.getUsername().equals(deserializedEvent.getUsername()) &&
                       event.getTaskId().equals(deserializedEvent.getTaskId()) &&
                       event.getTaskTitle().equals(deserializedEvent.getTaskTitle()) &&
                       event.getCreatorUsername().equals(deserializedEvent.getCreatorUsername()) &&
                       event.getAssignedUsername().equals(deserializedEvent.getAssignedUsername());
            } catch (Exception e) {
                return false;
            }
        }));
    }
      @Test
    @DisplayName("Should consume and deserialize JSON message from Redis")
    void testConsumeNotificationEvent() throws Exception {
        // Given: A JSON message representing a NotificationEvent
        NotificationEvent originalEvent = NotificationEvent.createTaskNotification(
            "TASK_ASSIGNED",
            "New task assigned: Important Task",
            "assignee123",
            "789",
            "Important Task"
        );
        String messageJson = objectMapper.writeValueAsString(originalEvent);
        
        String topic = "test-topic";
        AtomicReference<String> capturedUsername = new AtomicReference<>();
        AtomicReference<NotificationEvent> capturedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Mock SSEConnectionManager to capture the call
        doAnswer(invocation -> {
            String username = invocation.getArgument(0);
            NotificationEvent event = invocation.getArgument(1);
            capturedUsername.set(username);
            capturedEvent.set(event);
            latch.countDown();
            return null;
        }).when(sseConnectionManager).sendToUserLocal(anyString(), any(NotificationEvent.class));
        
        // When: Simulate receiving a Redis message directly
        Message mockMessage = mock(Message.class);
        when(mockMessage.getBody()).thenReturn(messageJson.getBytes());
        when(mockMessage.getChannel()).thenReturn(topic.getBytes());
        
        // Process the message through the consumer
        consumer.onMessage(mockMessage, null);
        
        // Then: The event should be received and deserialized correctly
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        NotificationEvent deserializedEvent = capturedEvent.get();
        String username = capturedUsername.get();
        
        assertAll(
            () -> assertNotNull(deserializedEvent, "Event should be captured"),
            () -> assertEquals("assignee123", username, "Username should match"),
            () -> assertEquals(originalEvent.getId(), deserializedEvent.getId()),
            () -> assertEquals(originalEvent.getType(), deserializedEvent.getType()),
            () -> assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage()),
            () -> assertEquals(originalEvent.getUsername(), deserializedEvent.getUsername()),
            () -> assertEquals(originalEvent.getTaskId(), deserializedEvent.getTaskId()),
            () -> assertEquals(originalEvent.getTaskTitle(), deserializedEvent.getTaskTitle()),
            () -> assertEquals(originalEvent.getTimestamp(), deserializedEvent.getTimestamp())
        );
    }
      @Test
    @DisplayName("Should handle malformed JSON gracefully in consumer")
    void testConsumerHandlesMalformedJson() throws Exception {
        // Given: A malformed JSON message
        String malformedJson = "{ invalid json }";
        String topic = "test-topic";
        
        // When: Simulate receiving a malformed Redis message directly
        Message mockMessage = mock(Message.class);
        when(mockMessage.getBody()).thenReturn(malformedJson.getBytes());
        when(mockMessage.getChannel()).thenReturn(topic.getBytes());
        
        // Then: Should handle gracefully without crashing
        assertDoesNotThrow(() -> {
            consumer.onMessage(mockMessage, null);
        });
        
        // And SSEConnectionManager should not be called for malformed messages
        verify(sseConnectionManager, never()).sendToUserLocal(anyString(), any(NotificationEvent.class));
    }
    
    @Test
    @DisplayName("Should publish minimal notification event successfully")
    void testPublishMinimalNotificationEvent() throws Exception {
        // Given: A minimal notification event
        NotificationEvent event = NotificationEvent.create(
            "SIMPLE_NOTIFICATION",
            "Simple message",
            "user123"
        );
        
        String topic = "test-topic";
        
        // When: Publish the event
        publisher.publishMessage(topic, event);
          // Then: Redis should receive the serialized JSON
        verify(redisTemplate).convertAndSend(eq(topic), argThat((String json) -> {
            try {
                // Verify the JSON contains required fields
                return json.contains("\"type\":\"SIMPLE_NOTIFICATION\"") &&
                       json.contains("\"message\":\"Simple message\"") &&
                       json.contains("\"username\":\"user123\"") &&
                       json.contains("\"timestamp\"") &&
                       json.contains("\"id\"");
            } catch (Exception e) {
                return false;
            }
        }));
    }
    
    @Test
    @DisplayName("Should handle Redis connection failure gracefully in publisher")
    void testPublisherHandlesRedisFailure() {
        // Given: Redis throws exception
        doThrow(new RuntimeException("Redis connection failed"))
            .when(redisTemplate).convertAndSend(anyString(), anyString());
        
        NotificationEvent event = NotificationEvent.create("TEST", "Test message", "user123");
        
        // When/Then: Should throw RuntimeException with meaningful message
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            publisher.publishMessage("test-topic", event);
        });
        
        assertEquals("Message publishing failed", exception.getMessage());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Redis connection failed", exception.getCause().getMessage());
    }
      @Test
    @DisplayName("Should validate notification event fields during consumption")
    void testConsumerValidatesNotificationEvent() throws Exception {
        // Given: A notification event with missing required fields
        String invalidJson = """
            {
                "id": "test-123",
                "type": null,
                "message": "Test message",
                "username": null,
                "timestamp": "2025-05-31T10:30:00"
            }
            """;
        
        String topic = "test-topic";
        
        // When: Simulate receiving an invalid Redis message directly
        Message mockMessage = mock(Message.class);
        when(mockMessage.getBody()).thenReturn(invalidJson.getBytes());
        when(mockMessage.getChannel()).thenReturn(topic.getBytes());
        
        // Then: Should handle validation failure gracefully
        assertDoesNotThrow(() -> {
            consumer.onMessage(mockMessage, null);
        });
        
        // And SSEConnectionManager should not be called for invalid messages
        verify(sseConnectionManager, never()).sendToUserLocal(anyString(), any(NotificationEvent.class));
    }
    
    @Test
    @DisplayName("Should successfully complete round-trip serialization")
    void testCompleteRoundTripSerialization() throws Exception {
        // Given: A complex notification event
        NotificationEvent originalEvent = new NotificationEvent();
        originalEvent.setId("round-trip-test-123");
        originalEvent.setType("TASK_REASSIGNED");
        originalEvent.setMessage("Task reassigned to new user");
        originalEvent.setTaskId("999");
        originalEvent.setTaskTitle("Complex Task");
        originalEvent.setUsername("newuser123");
        originalEvent.setTimestamp(LocalDateTime.of(2025, 5, 31, 15, 45, 30));
        originalEvent.setCreatorUsername("originalcreator");
        originalEvent.setAssignedUsername("newassignee");
        
        // When: Serialize to JSON (simulating Redis publish)
        String json = objectMapper.writeValueAsString(originalEvent);
        
        // When: Deserialize from JSON (simulating Redis consume)
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
}
