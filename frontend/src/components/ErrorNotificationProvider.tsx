import { useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import { Snackbar, Alert } from '@mui/material';
import type { AlertProps } from '@mui/material/Alert';
import { ErrorNotificationContext, type ErrorNotificationContextType } from './ErrorNotificationContext';

interface ErrorNotification {
  id: string;
  message: string;
  severity: AlertProps['severity'];
  autoHideDuration?: number;
}

interface Props {
  children: ReactNode;
}

export function ErrorNotificationProvider({ children }: Props) {
  const [notifications, setNotifications] = useState<ErrorNotification[]>([]);
  const addNotification = useCallback((
    message: string,
    severity: AlertProps['severity'],
    autoHideDuration: number = 6000
  ) => {
    const id = Date.now().toString();
    const notification: ErrorNotification = {
      id,
      message,
      severity,
      autoHideDuration,
    };

    setNotifications(prev => [...prev, notification]);

    // Auto-remove notification after duration
    if (autoHideDuration > 0) {
      setTimeout(() => {
        setNotifications(prev => prev.filter(n => n.id !== id));
      }, autoHideDuration);
    }

    return id;
  }, []);

  const showError = useCallback((message: string, autoHideDuration?: number) => {
    return addNotification(message, 'error', autoHideDuration);
  }, [addNotification]);

  const showWarning = useCallback((message: string, autoHideDuration?: number) => {
    return addNotification(message, 'warning', autoHideDuration);
  }, [addNotification]);

  const showSuccess = useCallback((message: string, autoHideDuration?: number) => {
    return addNotification(message, 'success', autoHideDuration);
  }, [addNotification]);

  const showInfo = useCallback((message: string, autoHideDuration?: number) => {
    return addNotification(message, 'info', autoHideDuration);
  }, [addNotification]);

  const hideNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  const handleClose = useCallback((id: string) => {
    hideNotification(id);
  }, [hideNotification]);

  const contextValue: ErrorNotificationContextType = {
    showError,
    showWarning,
    showSuccess,
    showInfo,
    hideNotification,
  };

  return (
    <ErrorNotificationContext.Provider value={contextValue}>
      {children}
      
      {/* Render notifications */}
      {notifications.map((notification, index) => (
        <Snackbar
          key={notification.id}
          open={true}
          autoHideDuration={notification.autoHideDuration}
          onClose={() => handleClose(notification.id)}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          style={{ 
            top: 24 + index * 60, // Stack notifications vertically
          }}
        >
          <Alert
            onClose={() => handleClose(notification.id)}
            severity={notification.severity}
            variant="filled"
            sx={{ width: '100%' }}
          >
            {notification.message}
          </Alert>
        </Snackbar>
      ))}
    </ErrorNotificationContext.Provider>
  );
}
