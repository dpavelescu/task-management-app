export interface Task {
  id: number;
  title: string;
  description: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  assignedToId: number;
  assignedToUsername: string;
  createdById: number;
  createdByUsername: string;
  createdAt: string;
  updatedAt: string;
  creatorUsername: string; // Added for task creation tracking
  assignedUsername: string; // Added for task assignment tracking
}

export interface User {
  id: number;
  username: string;
  email: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
