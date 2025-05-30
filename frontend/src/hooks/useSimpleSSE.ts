import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from './useAuth';

interface SimpleSSEHookProps {
  onTaskUpdate: () => void;
}

export function useSimpleSSE({ onTaskUpdate }: SimpleSSEHookProps) {
  const { token, user } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  const lastEventIdRef = useRef<string | null>(null);
  const onTaskUpdateRef = useRef(onTaskUpdate);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 5;

  // Keep the callback ref updated
  useEffect(() => {
    onTaskUpdateRef.current = onTaskUpdate;
  }, [onTaskUpdate]);

  const disconnect = useCallback(() => {
    // Clear any pending reconnection attempts
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      setIsConnected(false);
    }
  }, []);  const attemptConnection = useCallback((authToken: string) => {
    // Build URL with token and last event ID for message re-delivery
    const baseUrl = import.meta.env.VITE_SSE_URL || 'http://localhost:8080/api/notifications/stream';
    let url = `${baseUrl}?token=${encodeURIComponent(authToken)}`;
    if (lastEventIdRef.current) {
      url += `&lastEventId=${encodeURIComponent(lastEventIdRef.current)}`;
    }
    
    const eventSource = new EventSource(url);
    
    eventSource.onopen = () => {
      console.log('SSE: Connection opened successfully');
      setIsConnected(true);
      reconnectAttemptsRef.current = 0; // Reset reconnection attempts on successful connection
    };

    eventSource.addEventListener('notification', (event) => {
      // Store the last event ID for reconnection
      if (event.lastEventId) {
        lastEventIdRef.current = event.lastEventId;
      }
      
      try {
        const notification = JSON.parse(event.data);
        console.log('SSE: Received notification:', notification);
        
        if (notification.type && (
          notification.type === 'TASK_CREATED' ||
          notification.type === 'TASK_UPDATED' ||
          notification.type === 'TASK_DELETED' ||
          notification.type === 'TASK_ASSIGNED' ||
          notification.type === 'TASK_REASSIGNED'
        )) {
          console.log(`SSE: Processing ${notification.type} notification, calling onTaskUpdate`);
          onTaskUpdateRef.current();
        }
      } catch (error) {
        console.error('SSE: Error parsing notification:', error);
      }
    });

    eventSource.addEventListener('connected', () => {
      console.log('SSE: Received connected event');
      setIsConnected(true);
      reconnectAttemptsRef.current = 0; // Reset reconnection attempts
    });

    eventSource.onerror = (error) => {
      console.error('SSE: Connection error:', error);
      setIsConnected(false);
      
      if (eventSource.readyState === EventSource.CLOSED) {
        console.log('SSE: Connection closed, attempting to reconnect...');
        // Clear the current reference since this connection is dead
        if (eventSourceRef.current === eventSource) {
          eventSourceRef.current = null;
        }
        
        // Schedule automatic reconnection with exponential backoff
        if (reconnectAttemptsRef.current < maxReconnectAttempts && authToken && token && user) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000); // Exponential backoff, max 30 seconds
          console.log(`SSE: Scheduling reconnection attempt ${reconnectAttemptsRef.current + 1} in ${delay}ms`);
          
          reconnectTimeoutRef.current = window.setTimeout(() => {
            if (authToken && token && user) { // Double-check auth is still valid
              console.log(`SSE: Attempting reconnection ${reconnectAttemptsRef.current + 1}/${maxReconnectAttempts}`);
              reconnectAttemptsRef.current++;
              
              try {
                const newEventSource = attemptConnection(authToken);
                eventSourceRef.current = newEventSource;
              } catch (error) {
                console.error('SSE: Failed to create connection during reconnect:', error);
              }
            }
          }, delay);
        } else if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
          console.error('SSE: Max reconnection attempts reached, giving up');
        }
      }
    };

    return eventSource;
  }, [token, user, maxReconnectAttempts]);

  // Main connection effect
  useEffect(() => {
    if (!token || !user) {
      // Disconnect if no auth
      disconnect();
      return;
    }

    // Connect if authenticated and no existing connection
    if (eventSourceRef.current) {
      return; // Already connected
    }

    try {
      const eventSource = attemptConnection(token);
      eventSourceRef.current = eventSource;
    } catch (error) {
      console.error('SSE: Failed to create connection:', error);
      setIsConnected(false);
    }

    // Cleanup function to close connection
    return () => {
      disconnect();
    };
  }, [token, user, attemptConnection, disconnect]);

  // Reconnect when tab becomes visible (but only if not connected)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden && !isConnected && token && user && !eventSourceRef.current) {
        try {
          const eventSource = attemptConnection(token);
          eventSourceRef.current = eventSource;
        } catch (error) {
          console.error('SSE: Failed to create connection on visibility change:', error);
          setIsConnected(false);
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [isConnected, token, user, attemptConnection]);

  const manualReconnect = useCallback(() => {
    // Manual reconnect function that properly closes old connection first
    disconnect();
    
    if (token && user) {
      // Reset reconnection attempts for manual reconnect
      reconnectAttemptsRef.current = 0;
      
      try {
        const eventSource = attemptConnection(token);
        eventSourceRef.current = eventSource;
      } catch (error) {
        console.error('SSE: Failed to create connection on manual reconnect:', error);
        setIsConnected(false);
      }
    }
  }, [token, user, attemptConnection, disconnect]);

  return {
    isConnected,
    reconnect: manualReconnect,
    disconnect,
    lastEventId: lastEventIdRef.current
  };
}
