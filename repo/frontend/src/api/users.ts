import apiClient from '@/api/client'
import type { User, UserCreateRequest, AdminPasswordResetRequest } from '@/types/user'

export async function getUsers(page = 0, size = 20): Promise<{ content: User[]; totalElements: number; totalPages: number }> {
  const { data } = await apiClient.get('/api/users', { params: { page, size } })
  return data.data
}

export async function createUser(request: UserCreateRequest): Promise<User> {
  const { data } = await apiClient.post('/api/users', request)
  return data.data
}

export async function getUser(id: number): Promise<User> {
  const { data } = await apiClient.get(`/api/users/${id}`)
  return data.data
}

export async function updateUser(id: number, updates: Partial<User>): Promise<User> {
  const { data } = await apiClient.put(`/api/users/${id}`, updates)
  return data.data
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/api/users/${id}`)
}

export async function resetPassword(id: number, newPassword: string): Promise<void> {
  await apiClient.post(`/api/users/${id}/reset-password`, { newPassword })
}

export async function adminResetPassword(id: number, request: AdminPasswordResetRequest): Promise<void> {
  await apiClient.post(`/api/users/${id}/admin-reset-password`, request)
}
