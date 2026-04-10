import apiClient from '@/api/client'
import type { LoginRequest, LoginResponse } from '@/types/auth'

export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const { data } = await apiClient.post('/api/auth/login', credentials)
  return data.data
}

export async function refresh(refreshToken: string): Promise<LoginResponse> {
  const { data } = await apiClient.post('/api/auth/refresh', { refreshToken })
  return data.data
}

export async function logout(refreshToken?: string | null): Promise<void> {
  await apiClient.post('/api/auth/logout', refreshToken ? { refreshToken } : undefined)
}
