import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from './useAuth';

interface SimpleSSEHookProps {
  onTaskUpdate: () => void;
}

const TASK_NOTIFICATION_TYPES = [
  'TASK_CREATED', 'TASK_UPDATED', 'TASK_DELETED', 
  'TASK_ASSIGNED', 'TASK_REASSIGNED'
];

export function useSimpleSSE({ onTaskUpdate }: SimpleSSEHookProps) {
  const { token, user } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const onTaskUpdateRef = useRef(onTaskUpdate);

  // Keep callback ref updated
  useEffect(() => {
    onTaskUpdateRef.current = onTaskUpdate;
  }, [onTaskUpdate]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      setIsConnected(false);
    }
  }, []);

  const connect = useCallback(() => {
    if (!token || eventSourceRef.current) return;

    const baseUrl = import.meta.env.VITE_SSE_URL || 'http://localhost:8080/api/notifications/stream';
    const url = `${baseUrl}?token=${encodeURIComponent(token)}`;
    
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;
    
    eventSource.onopen = () => {
      console.log('SSE: Connected');
      setIsConnected(true);
    };    eventSource.addEventListener('notification', (event) => {
      try {
        // Quick check to avoid unnecessary parsing for non-task notifications
        if (!event.data.includes('TASK_')) {
          return;
        }
          const notification = JSON.parse(event.data);
        // Reduced logging for performance - only log in development
        if (import.meta.env.DEV) {
          console.log('SSE: Received notification:', notification.type);
        }
        
        if (TASK_NOTIFICATION_TYPES.includes(notification.type)) {
          onTaskUpdateRef.current();
        }
      } catch (error) {
        console.error('SSE: Error parsing notification:', error);
      }
    });

    eventSource.addEventListener('connected', () => {
      setIsConnected(true);
    });

    eventSource.onerror = () => {
      console.error('SSE: Connection error');
      setIsConnected(false);
      
      if (eventSource.readyState === EventSource.CLOSED) {
        eventSourceRef.current = null;
        
        // Simple reconnect after 5 seconds
        reconnectTimeoutRef.current = window.setTimeout(() => {
          if (token && user) {
            console.log('SSE: Reconnecting...');
            connect();
          }
        }, 5000);
      }
    };
  }, [token, user]);

  // Main connection effect
  useEffect(() => {
    if (token && user) {
      connect();
    } else {
      disconnect();
    }

    return disconnect;
  }, [token, user, connect, disconnect]);

  // Reconnect on page visibility
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden && !isConnected && token && user) {
        connect();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [isConnected, token, user, connect]);

  return {
    isConnected,
    reconnect: connect,
    disconnect
  };
}
