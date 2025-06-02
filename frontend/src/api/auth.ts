import { apiClient, ApiError } from './apiClient';
import { apiConfig } from '../config';

interface LoginRequest {
  username: string;
  password: string;
}

interface RegisterRequest extends LoginRequest {
  email: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: number;
    username: string;
    email: string;
  };
}

export async function login(data: LoginRequest): Promise<AuthResponse> {
  try {
    const result = await apiClient.post<AuthResponse>(
      apiConfig.endpoints.auth.login, 
      data, 
      { skipAuth: true }
    );
    
    // Validate the response structure
    if (!result.token || !result.user) {
      throw new Error('Invalid response from server');
    }

    return result;
  } catch (error) {
    if (error instanceof ApiError) {
      throw new Error('Login failed');
    }
    throw error;
  }
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  try {
    const result = await apiClient.post<AuthResponse>(
      apiConfig.endpoints.auth.register, 
      data, 
      { skipAuth: true }
    );
    
    // Validate the response structure
    if (!result.token || !result.user) {
      throw new Error('Invalid response from server');
    }

    return result;
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.status === 409) {
        throw new Error('Username or email already exists');
      }
      if (error.status === 400) {
        throw new Error('Invalid registration data');
      }
      throw new Error('Registration failed');
    }
    throw error;
  }
}
