// Centralized API client with automatic authentication
import { getStoredToken, isTokenExpired, clearStoredAuth } from '../contexts/enhancedAuthUtils';
import { apiConfig } from '../config';

export class ApiError extends Error {
  public status: number;
  public response?: Response;

  constructor(
    message: string,
    status: number,
    response?: Response
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.response = response;
  }
}

export interface ApiClientOptions {
  skipAuth?: boolean;
  timeout?: number;
}

function getAuthHeaders(): Record<string, string> {
  const token = getStoredToken();
  
  if (!token) {
    return {};
  }

  if (isTokenExpired(token)) {
    clearStoredAuth();
    return {};
  }

  return {
    'Authorization': `Bearer ${token}`,
  };
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorMessage = `Request failed with status ${response.status}`;
    
    try {
      const errorText = await response.text();
      if (errorText) {
        errorMessage = errorText;
      }
    } catch {
      // Ignore parsing errors, use default message
    }

    // Handle authentication errors globally
    if (response.status === 401 || response.status === 403) {
      clearStoredAuth();
      // This will trigger the auth provider to redirect to login
      window.dispatchEvent(new CustomEvent('auth:tokenExpired'));
    }

    throw new ApiError(errorMessage, response.status, response);
  }

  // Handle empty responses
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }
  
  const text = await response.text();
  return text as unknown as T;
}

export async function request<T>(
  endpoint: string, 
  options: RequestInit & ApiClientOptions = {}
): Promise<T> {
  const { skipAuth = false, timeout = 10000, ...fetchOptions } = options;
  // Build headers - always include Content-Type for JSON requests
  const baseHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  
  const authHeaders = !skipAuth ? getAuthHeaders() : {};
  
  const headers = {
    ...baseHeaders,
    ...authHeaders,
    ...fetchOptions.headers,  };
  // Build full URL
  const url = endpoint.startsWith('http') ? endpoint : `${apiConfig.baseUrl}${endpoint}`;

  // Create abort controller for timeout
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      headers,
      signal: controller.signal,
    });

    return await handleResponse<T>(response);
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiError('Request timeout', 408);
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

// Convenience methods
export async function get<T>(endpoint: string, options?: ApiClientOptions): Promise<T> {
  return request<T>(endpoint, { ...options, method: 'GET' });
}

export async function post<T>(endpoint: string, data?: unknown, options?: ApiClientOptions): Promise<T> {
  return request<T>(endpoint, {
    ...options,
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  });
}

export async function put<T>(endpoint: string, data?: unknown, options?: ApiClientOptions): Promise<T> {
  return request<T>(endpoint, {
    ...options,
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  });
}

export async function deleteRequest<T>(endpoint: string, options?: ApiClientOptions): Promise<T> {
  return request<T>(endpoint, { ...options, method: 'DELETE' });
}

// Get current token for special cases like SSE
export function getCurrentToken(): string | null {
  return getStoredToken();
}

// Export default client object for backward compatibility
export const apiClient = {
  request,
  get,
  post,
  put,
  delete: deleteRequest,
  getCurrentToken,
};
