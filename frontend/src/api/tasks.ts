import type { Task } from '../types/api';
import { apiClient } from './apiClient';
import { apiConfig } from '../config';

export interface CreateTaskData {
  title: string;
  description: string;
  assignedTo?: number;
}

export interface UpdateTaskData extends CreateTaskData {
  status?: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
}

export async function getTasks(): Promise<Task[]> {
  return apiClient.get<Task[]>(apiConfig.endpoints.tasks.base);
}

export async function createTask(data: CreateTaskData): Promise<Task> {
  return apiClient.post<Task>(apiConfig.endpoints.tasks.base, data);
}

export async function updateTask(id: number, data: UpdateTaskData): Promise<Task> {
  return apiClient.put<Task>(apiConfig.endpoints.tasks.byId(id.toString()), data);
}

export async function deleteTask(id: number): Promise<void> {
  return apiClient.delete<void>(apiConfig.endpoints.tasks.byId(id.toString()));
}
