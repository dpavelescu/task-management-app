import { useState, useCallback, useEffect, useRef, createContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import type { User } from '../types/api';
import type { AuthState, AuthContextType } from './enhancedAuthUtils';
import {
  TOKEN_CHECK_INTERVAL,
  getStoredToken,
  getStoredUser,
  isTokenExpired,
  isTokenExpiringSoon,
  storeAuthData,
  clearStoredAuth,
  validateTokenFormat,
  validateUserData
} from './enhancedAuthUtils';

// Create the AuthContext
export const AuthContext = createContext<AuthContextType | null>(null);

interface EnhancedAuthProviderProps {
  children: ReactNode;
  onAuthenticationRequired?: () => void;
  onTokenExpired?: () => void;
}

export function EnhancedAuthProvider({ 
  children, 
  onAuthenticationRequired,
  onTokenExpired 
}: EnhancedAuthProviderProps) {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Auth state
  const [authState, setAuthState] = useState<AuthState>({
    token: null,
    user: null,
    isInitialized: false,
    lastTokenCheck: 0
  });
  // Refs for cleanup and intervals
  const tokenCheckIntervalRef = useRef<number | null>(null);
  const isNavigatingRef = useRef(false);

  // Computed authentication status
  const isAuthenticated = authState.token !== null && 
                          !isTokenExpired(authState.token) && 
                          authState.user !== null;

  // Handle authentication required
  const handleAuthenticationRequired = useCallback(() => {    if (isNavigatingRef.current) return;
    
    isNavigatingRef.current = true;
    
    onAuthenticationRequired?.();
    
    // Save current location for redirect after login
    const currentPath = location.pathname + location.search;
    if (currentPath !== '/login' && currentPath !== '/register') {
      localStorage.setItem('redirectAfterLogin', currentPath);
    }
    
    navigate('/login', { replace: true });
    
    setTimeout(() => {
      isNavigatingRef.current = false;
    }, 100);
  }, [navigate, location, onAuthenticationRequired]);
  // Handle token expiration
  const handleTokenExpired = useCallback(() => {
    setAuthState((prev: AuthState) => ({
      ...prev,
      token: null,
      user: null,
      lastTokenCheck: Date.now()
    }));
    
    clearStoredAuth();
    onTokenExpired?.();
    handleAuthenticationRequired();
  }, [onTokenExpired, handleAuthenticationRequired]);

  // Check token validity
  const checkTokenValidity = useCallback((): boolean => {
    const { token } = authState;
    
    if (!token) return false;
    
    const isExpired = isTokenExpired(token);
    
    setAuthState((prev: AuthState) => ({
      ...prev,
      lastTokenCheck: Date.now()
    }));
      if (isExpired) {
      handleTokenExpired();
      return false;
    }
    
    // Check if token is expiring soon
    if (isTokenExpiringSoon(token)) {
      // Note: Token refresh would be implemented here if backend supports it
      // For now, we'll log and continue
    }
    
    return true;
    }, [authState, handleTokenExpired]);

  // Initialize auth state from storage
  const initializeAuth = useCallback(async () => {
    try {
      const storedToken = getStoredToken();
      const storedUser = getStoredUser();

      setAuthState((prev: AuthState) => ({
        ...prev,
        token: storedToken,
        user: storedUser,
        isInitialized: true,
        lastTokenCheck: Date.now()
      }));

      // If we have invalid auth state, redirect to login
      if ((!storedToken || !storedUser) && location.pathname !== '/login' && location.pathname !== '/register') {
        handleAuthenticationRequired();
      }
    } catch (error) {
      console.error('EnhancedAuthProvider: Error during initialization:', error);
      setAuthState((prev: AuthState) => ({
        ...prev,
        token: null,
        user: null,
        isInitialized: true,
        lastTokenCheck: Date.now()
      }));
      clearStoredAuth();
      handleAuthenticationRequired();
    }
  }, [location.pathname, handleAuthenticationRequired]);
  // Refresh token (placeholder for future implementation)
  const refreshToken = useCallback(async (): Promise<boolean> => {
    // TODO: Implement token refresh if backend supports it
    return false;
  }, []);
  // Login function
  const login = useCallback(async (newToken: string, newUser: User): Promise<void> => {
    // Validate inputs
    if (!validateTokenFormat(newToken)) {
      throw new Error('Invalid token format');
    }
    
    if (!validateUserData(newUser)) {
      throw new Error('Invalid user data');
    }
    
    if (isTokenExpired(newToken)) {
      throw new Error('Received expired token');
    }

    // Store auth data
    const stored = storeAuthData(newToken, newUser);
    if (!stored) {
      throw new Error('Failed to store authentication data');
    }

    // Update state
    setAuthState({
      token: newToken,
      user: newUser,
      isInitialized: true,
      lastTokenCheck: Date.now()
    });

    // Handle redirect after login
    const redirectPath = localStorage.getItem('redirectAfterLogin');
    localStorage.removeItem('redirectAfterLogin');
    
    if (redirectPath && redirectPath !== '/login' && redirectPath !== '/register') {
      navigate(redirectPath, { replace: true });
    } else {
      navigate('/tasks', { replace: true });
    }
  }, [navigate]);
  // Logout function
  const logout = useCallback(() => {
    setAuthState({
      token: null,
      user: null,
      isInitialized: true,
      lastTokenCheck: Date.now()
    });
    
    clearStoredAuth();
    navigate('/login', { replace: true });
  }, [navigate]);

  // Periodic token checking
  useEffect(() => {
    if (!authState.isInitialized) return;

    const startTokenChecking = () => {      if (tokenCheckIntervalRef.current) {
        clearInterval(tokenCheckIntervalRef.current);
      }
      
      tokenCheckIntervalRef.current = window.setInterval(() => {
        if (authState.token) {
          checkTokenValidity();
        }
      }, TOKEN_CHECK_INTERVAL);
    };

    if (isAuthenticated) {
      startTokenChecking();
    }

    return () => {
      if (tokenCheckIntervalRef.current) {
        clearInterval(tokenCheckIntervalRef.current);
        tokenCheckIntervalRef.current = null;
      }
    };
  }, [authState.isInitialized, authState.token, isAuthenticated, checkTokenValidity]);

  // Initialize on mount
  useEffect(() => {
    initializeAuth();
    
    return () => {
      if (tokenCheckIntervalRef.current) {
        clearInterval(tokenCheckIntervalRef.current);
      }
    };
  }, [initializeAuth]);

  // Listen for storage changes (multiple tabs)
  useEffect(() => {    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === 'token' || event.key === 'user') {
        initializeAuth();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [initializeAuth]);

  // Context value
  const contextValue = {
    token: authState.token,
    user: authState.user,
    login,
    logout,
    isAuthenticated,
    isInitialized: authState.isInitialized,
    refreshToken,
    checkTokenValidity
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}