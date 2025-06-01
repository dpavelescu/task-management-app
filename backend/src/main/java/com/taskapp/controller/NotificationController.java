package com.taskapp.controller;

import com.taskapp.config.CorsProperties;
import com.taskapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final CorsProperties corsProperties;@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestParam(value = "lastEventId", required = false) String lastEventIdParam,
            HttpServletRequest request,
            HttpServletResponse response) {
          // Use Last-Event-ID from header first, then from URL parameter
        String lastEventId = lastEventIdHeader != null ? lastEventIdHeader : lastEventIdParam;
        
        log.debug("New SSE connection request from: {} (Last-Event-ID: {})", request.getRemoteAddr(), lastEventId);
        
        try {
            // Manually set SSE headers to avoid Spring Security wrapper issues
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no"); // Disable nginx buffering
              // Set CORS headers dynamically based on request origin
            String origin = request.getHeader("Origin");
            if (origin != null && Arrays.asList(corsProperties.getAllowedOrigins()).contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", String.valueOf(corsProperties.isAllowCredentials()));
                response.setHeader("Access-Control-Expose-Headers", "Content-Type");
            }
            
            // Extract and validate token
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("Using token from Authorization header");
            } else if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                token = tokenParam.trim();
                log.debug("Using token from URL parameter");
            } else {
                log.error("Missing token in both Authorization header and URL parameter");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Create SSE connection with Last-Event-ID support
            SseEmitter emitter = notificationService.createConnection(token, lastEventId);
            
            // Send initial connection event (no data to avoid interfering with Last-Event-ID)
            try {
                emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}")
                );
            } catch (IOException e) {
                log.debug("Failed to send initial connection event: {}", e.getMessage());
                emitter.completeWithError(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // Return the emitter with proper headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_EVENT_STREAM);
            headers.setCacheControl("no-cache");
            headers.set("Connection", "keep-alive");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(emitter);
                
        } catch (Exception e) {
            log.error("Error creating SSE connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
      @GetMapping("/status")
    public ResponseEntity<NotificationStatus> getStatus() {
        int activeConnections = notificationService.getActiveConnectionCount();
        boolean messagingHealthy = notificationService.isMessagingHealthy();
        return ResponseEntity.ok(new NotificationStatus(activeConnections, messagingHealthy));
    }
    
    // DTO for status response
    public static class NotificationStatus {
        private final int activeConnections;
        private final boolean messagingHealthy;
        
        public NotificationStatus(int activeConnections, boolean messagingHealthy) {
            this.activeConnections = activeConnections;
            this.messagingHealthy = messagingHealthy;
        }
        
        public int getActiveConnections() {
            return activeConnections;
        }
        
        public boolean isMessagingHealthy() {
            return messagingHealthy;
        }
    }
}
