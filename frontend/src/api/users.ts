import { apiConfig } from '../config';

export interface User {
  id: number;
  username: string;
  email: string;
}

export const getUsers = async (): Promise<User[]> => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    throw new Error('No authentication token found');
  }

  const response = await fetch(apiConfig.endpoints.users.base, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new Error(`Authentication failed: ${response.status}`);
    }
    throw new Error(`Failed to fetch users: ${response.status}`);
  }

  return response.json();
};
