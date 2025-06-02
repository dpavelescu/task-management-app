import { apiClient } from './apiClient';
import { apiConfig } from '../config';
import type { User } from '../types/api';

export const getUsers = async (): Promise<User[]> => {
  return apiClient.get<User[]>(apiConfig.endpoints.users.base);
};
