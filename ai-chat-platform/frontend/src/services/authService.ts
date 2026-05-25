import { apiClient } from './apiClient';
import type { LoginRequest, LoginResponse } from '../types/auth';

export const authService = {
  async login(payload: LoginRequest): Promise<LoginResponse> {
    const { data } = await apiClient.post<LoginResponse>('/auth/login', payload);
    return data;
  },
};
