import type { Task } from '../types/api';
import { apiConfig } from '../config';

const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  if (!token) {
    console.error('No token found in localStorage');
    throw new Error('Authentication required');
  }
  
  if (typeof token !== 'string' || token === 'undefined' || token === 'null') {
    console.error('Invalid token found in localStorage:', token);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    throw new Error('Invalid authentication token');
  }

  // Additional token validation
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const currentTime = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp <= currentTime) {
      console.error('Token has expired');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      throw new Error('Token has expired');
    }
  } catch (e) {
    console.error('Invalid token format:', e);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    throw new Error('Invalid token format');
  }
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
  
  return headers;
};

export interface CreateTaskData {
  title: string;
  description: string;
  assignedTo?: number;
}

export interface UpdateTaskData extends CreateTaskData {
  status?: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
}

export async function getTasks(): Promise<Task[]> {
  try {
    const headers = getAuthHeaders();
    
    const response = await fetch(apiConfig.endpoints.tasks.base, {
      headers,
      credentials: 'include', // Include cookies if your backend uses them
    });
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('getTasks: Error response:', {
        status: response.status,
        text: errorText
      });
      
      if (response.status === 401 || response.status === 403) {
        throw new Error('Session expired or invalid. Please log in again.');
      }
      
      throw new Error(errorText || `Server error: ${response.status}`);
    }

    let result;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      try {
        result = await response.json();
      } catch (e) {
        console.error('getTasks: Failed to parse JSON response:', e);
        throw new Error('Invalid response format from server');
      }
    } else {
      const text = await response.text();
      console.error('getTasks: Unexpected content type:', contentType, 'Response:', text);
      throw new Error('Invalid response type from server');
    }

    if (!Array.isArray(result)) {
      console.error('getTasks: Expected array response, got:', result);
      throw new Error('Invalid response format: expected array of tasks');
    }

    return result;
  } catch (error) {
    console.error('getTasks: Request failed:', error);
    throw error instanceof Error ? error : new Error('Failed to fetch tasks');
  }
}

export async function createTask(data: CreateTaskData): Promise<Task> {
  const response = await fetch(apiConfig.endpoints.tasks.base, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Failed to create task');
  }

  return response.json();
}

export async function updateTask(id: number, data: UpdateTaskData): Promise<Task> {
  const response = await fetch(apiConfig.endpoints.tasks.byId(id.toString()), {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error('Failed to update task');
  }

  return response.json();
}

export async function deleteTask(id: number): Promise<void> {
  const response = await fetch(apiConfig.endpoints.tasks.byId(id.toString()), {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    throw new Error('Failed to delete task');
  }
}
