import { useContext } from 'react';
import { ErrorNotificationContext } from '../components/ErrorNotificationContext';

export function useErrorNotification() {
  const context = useContext(ErrorNotificationContext);
  if (!context) {
    throw new Error('useErrorNotification must be used within an ErrorNotificationProvider');
  }
  return context;
}
