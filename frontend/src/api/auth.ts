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

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Login failed:', errorText);
    throw new Error('Login failed');
  }
  const result = await response.json();
  
  // Validate the response structure
  if (!result.token || !result.user) {
    console.error('Invalid login response structure:', result);
    throw new Error('Invalid response from server');
  }

  return result;
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  try {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });

    const errorText = await response.text();
    
    if (!response.ok) {
      console.error('Registration failed:', response.status, errorText);
      
      // Handle specific error cases
      if (response.status === 409) {
        throw new Error('Username or email already exists');
      }
      
      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        throw new Error('Authentication error');
      }
      
      if (response.status === 400) {
        throw new Error('Invalid registration data: ' + errorText);
      }
      
      throw new Error(`Registration failed: ${response.status} ${errorText}`);
    }

    // Try to parse the successful response
    let result;
    try {
      result = JSON.parse(errorText);
    } catch (e) {
      console.error('Failed to parse registration response:', e);
      throw new Error('Invalid response from server');
    }
    
    // Validate the response structure
    if (!result.token || !result.user) {
      console.error('Invalid registration response structure:', result);
      throw new Error('Invalid response from server');
    }

    return result;
  } catch (error) {
    // Add more context to the error
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Failed to complete registration');
  }
}
