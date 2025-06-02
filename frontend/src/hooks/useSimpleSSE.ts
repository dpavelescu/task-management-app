import { useEffect, useRef, useState } from 'react';
import { useAuth } from './useAuth';

interface SimpleSSEHookProps {
  onTaskUpdate: () => void;
}

export function useSimpleSSE({ onTaskUpdate }: SimpleSSEHookProps) {
  const { token, user } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const callbackRef = useRef(onTaskUpdate);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  
  // Update callback ref when it changes
  callbackRef.current = onTaskUpdate;

  useEffect(() => {
    if (!token || !user) {
      // Disconnect if no auth
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
        setIsConnected(false);
        setError(null);
      }
      return;
    }

    // Connect to SSE
    const baseUrl = import.meta.env.VITE_SSE_URL || 'http://localhost:8080/api/notifications/stream';
    const url = `${baseUrl}?token=${encodeURIComponent(token)}`;
    
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;
    
    eventSource.onopen = () => {
      setIsConnected(true);
      setError(null);
    };

    eventSource.addEventListener('notification', (event) => {
      try {
        const notification = JSON.parse(event.data);
        if (notification.type && notification.type.includes('TASK_')) {
          callbackRef.current();
        }
      } catch (error) {
        console.error('Error parsing notification:', error);
        setError('Failed to parse notification');
      }
    });    eventSource.onerror = () => {
      setIsConnected(false);
      const errorMessage = 'Connection lost - attempting to reconnect...';
      setError(errorMessage);
      
      // Auto-reconnect after 5 seconds
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      reconnectTimeoutRef.current = setTimeout(() => {
        if (token && user) {
          console.log('Attempting SSE reconnection...');
          setError(null);
        }
      }, 5000);
    };

    // Cleanup on unmount or dependency change
    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      eventSource.close();
    };
  }, [token, user]);

  return {
    isConnected,
    error,
  };
}
