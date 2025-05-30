import type { User } from '../types/api';

// Simple interfaces for testing
export interface AuthState {
  token: string | null;
  user: User | null;
  isInitialized: boolean;
  lastTokenCheck: number;
}

export interface AuthContextType {
  token: string | null;
  user: User | null;
  login: (token: string, user: User) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isInitialized: boolean;
  refreshToken: () => Promise<boolean>;
  checkTokenValidity: () => boolean;
}

// Constants
export const TOKEN_CHECK_INTERVAL = 30000;
export const TOKEN_REFRESH_THRESHOLD = 300000;
export const TOKEN_STORAGE_KEY = 'token';
export const USER_STORAGE_KEY = 'user';

// Basic utility functions
export function parseJwtPayload(token: string): any | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Failed to parse JWT payload:', error);
    return null;
  }
}

export function isTokenExpired(token: string | null): boolean {
  if (!token) return true;
  
  try {
    const payload = parseJwtPayload(token);
    if (!payload || !payload.exp) return true;
    
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp <= currentTime;
  } catch (error) {
    console.error('Error checking token expiration:', error);
    return true;
  }
}

export function isTokenExpiringSoon(token: string | null, thresholdMs: number = TOKEN_REFRESH_THRESHOLD): boolean {
  if (!token) return false;
  
  try {
    const payload = parseJwtPayload(token);
    if (!payload || !payload.exp) return false;
    
    const currentTime = Math.floor(Date.now() / 1000);
    const timeUntilExpiry = (payload.exp - currentTime) * 1000;
    
    return timeUntilExpiry <= thresholdMs && timeUntilExpiry > 0;
  } catch (error) {
    console.error('Error checking token expiration threshold:', error);
    return false;
  }
}

export function getTokenExpirationTime(token: string | null): Date | null {
  if (!token) return null;
  
  try {
    const payload = parseJwtPayload(token);
    if (!payload || !payload.exp) return null;
    
    return new Date(payload.exp * 1000);
  } catch (error) {
    console.error('Error getting token expiration time:', error);
    return null;
  }
}

export function getStoredToken(): string | null {
  try {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token || token === 'undefined' || token === 'null') {
      return null;
    }    if (isTokenExpired(token)) {
      clearStoredAuth();
      return null;
    }

    return token;
  } catch (error) {
    console.error('Error retrieving stored token:', error);
    return null;
  }
}

export function getStoredUser(): User | null {
  try {
    if (!getStoredToken()) {
      return null;
    }

    const userStr = localStorage.getItem(USER_STORAGE_KEY);
    if (!userStr || userStr === 'undefined' || userStr === 'null') {
      return null;
    }

    const user = JSON.parse(userStr);
    if (!user || !user.id || !user.username) {
      console.warn('Invalid user data structure:', user);
      clearStoredAuth();
      return null;
    }

    return user;
  } catch (error) {
    console.error('Failed to parse stored user:', error);
    clearStoredAuth();
    return null;
  }
}

export function storeAuthData(token: string, user: User): boolean {
  try {
    if (!token || isTokenExpired(token)) {
      console.error('Attempted to store invalid or expired token');
      return false;
    }

    if (!user || !user.id || !user.username) {
      console.error('Attempted to store invalid user data');
      return false;
    }

    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    return true;
  } catch (error) {
    console.error('Failed to store auth data:', error);
    clearStoredAuth();
    return false;
  }
}

export function clearStoredAuth(): void {
  try {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  } catch (error) {
    console.error('Failed to clear stored auth data:', error);
  }
}

export function validateTokenFormat(token: string): boolean {
  if (!token || typeof token !== 'string') return false;
  
  const parts = token.split('.');
  return parts.length === 3 && parts.every(part => part.length > 0);
}

export function validateUserData(user: any): user is User {
  return user && 
         typeof user.id === 'number' && 
         typeof user.username === 'string' && 
         user.username.length > 0 &&
         typeof user.email === 'string' &&
         user.email.length > 0;
}