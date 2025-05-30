import { createContext } from 'react';

export interface ErrorNotificationContextType {
  showError: (message: string, autoHideDuration?: number) => void;
  showWarning: (message: string, autoHideDuration?: number) => void;
  showSuccess: (message: string, autoHideDuration?: number) => void;
  showInfo: (message: string, autoHideDuration?: number) => void;
  hideNotification: (id: string) => void;
}

export const ErrorNotificationContext = createContext<ErrorNotificationContextType | undefined>(undefined);
