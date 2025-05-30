import { useContext } from 'react';
import { AuthContext } from '../contexts/EnhancedAuthProvider';

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an EnhancedAuthProvider');
  }
  return context;
}
