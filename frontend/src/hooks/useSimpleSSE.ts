import { useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from './useAuth';

interface SimpleSSEHookProps {
  onTaskUpdate: () => void;
}

export function useSimpleSSE({ onTaskUpdate }: SimpleSSEHookProps) {
  const { token, user } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!token || !user) {
      // Disconnect if no auth
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
        setIsConnected(false);
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
    };

    eventSource.addEventListener('notification', (event) => {
      try {
        const notification = JSON.parse(event.data);
        if (notification.type && notification.type.includes('TASK_')) {
          onTaskUpdate();
        }
      } catch (error) {
        console.error('Error parsing notification:', error);
      }
    });

    eventSource.onerror = () => {
      setIsConnected(false);
    };

    // Cleanup on unmount or dependency change
    return () => {
      eventSource.close();
    };
  }, [token, user, onTaskUpdate]);

  return {
    isConnected
  };
}
