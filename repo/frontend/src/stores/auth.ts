import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/api/client'
import { logout as apiLogout } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('scholarops_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('scholarops_refresh'))
  const user = ref<any>(JSON.parse(localStorage.getItem('scholarops_user') || 'null'))
  const roles = ref<string[]>(JSON.parse(localStorage.getItem('scholarops_roles') || '[]'))
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('scholarops_permissions') || '[]'))

  const isAuthenticated = computed(() => !!token.value)
  const hasRole = (role: string) => roles.value.includes(role)
  const hasPermission = (perm: string) => permissions.value.includes(perm)
  const isAdmin = computed(() => hasRole('ADMINISTRATOR'))
  const isCurator = computed(() => hasRole('CONTENT_CURATOR'))
  const isInstructor = computed(() => hasRole('INSTRUCTOR'))
  const isTA = computed(() => hasRole('TEACHING_ASSISTANT'))
  const isStudent = computed(() => hasRole('STUDENT'))

  async function login(username: string, password: string) {
    const { data } = await apiClient.post('/api/auth/login', { username, password })
    const resp = data.data
    token.value = resp.accessToken
    refreshToken.value = resp.refreshToken
    user.value = resp.user
    roles.value = resp.roles || []
    permissions.value = resp.permissions || []
    localStorage.setItem('scholarops_token', resp.accessToken)
    localStorage.setItem('scholarops_refresh', resp.refreshToken)
    localStorage.setItem('scholarops_user', JSON.stringify(resp.user))
    localStorage.setItem('scholarops_roles', JSON.stringify(roles.value))
    localStorage.setItem('scholarops_permissions', JSON.stringify(permissions.value))
  }

  async function logout() {
    // Send refresh token to backend so it can be blacklisted
    try {
      await apiLogout(refreshToken.value)
    } catch {
      // Best-effort: clear local state even if server call fails
    }
    token.value = null; refreshToken.value = null; user.value = null
    roles.value = []; permissions.value = []
    localStorage.removeItem('scholarops_token')
    localStorage.removeItem('scholarops_refresh')
    localStorage.removeItem('scholarops_user')
    localStorage.removeItem('scholarops_roles')
    localStorage.removeItem('scholarops_permissions')
  }

  return { token, refreshToken, user, roles, permissions, isAuthenticated, hasRole, hasPermission, isAdmin, isCurator, isInstructor, isTA, isStudent, login, logout }
})
