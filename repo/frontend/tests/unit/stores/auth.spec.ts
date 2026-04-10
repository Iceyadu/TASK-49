import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({
  logout: vi.fn().mockResolvedValue(undefined)
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should start unauthenticated', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
    expect(store.token).toBeNull()
  })

  it('should check roles correctly', () => {
    const store = useAuthStore()
    store.roles = ['ADMINISTRATOR', 'INSTRUCTOR']
    expect(store.hasRole('ADMINISTRATOR')).toBe(true)
    expect(store.hasRole('STUDENT')).toBe(false)
    expect(store.isAdmin).toBe(true)
    expect(store.isStudent).toBe(false)
  })

  it('should check permissions correctly', () => {
    const store = useAuthStore()
    store.permissions = ['USER_MANAGE', 'ROLE_ASSIGN']
    expect(store.hasPermission('USER_MANAGE')).toBe(true)
    expect(store.hasPermission('QUIZ_TAKE')).toBe(false)
  })

  it('should clear state on logout', async () => {
    const store = useAuthStore()
    store.token = 'test-token'
    store.user = { id: 1, username: 'admin' }
    store.roles = ['ADMINISTRATOR']
    await store.logout()
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.roles).toEqual([])
    expect(store.isAuthenticated).toBe(false)
  })

  it('should persist token to localStorage', () => {
    const store = useAuthStore()
    store.token = 'jwt-token'
    localStorage.setItem('scholarops_token', 'jwt-token')
    expect(localStorage.getItem('scholarops_token')).toBe('jwt-token')
  })

  it('should report isAuthenticated as true when token is set', () => {
    const store = useAuthStore()
    store.token = 'some-valid-token'
    expect(store.isAuthenticated).toBe(true)
  })

  it('should compute role booleans correctly for all roles', () => {
    const store = useAuthStore()
    store.roles = ['CONTENT_CURATOR']
    expect(store.isCurator).toBe(true)
    expect(store.isAdmin).toBe(false)
    expect(store.isInstructor).toBe(false)
    expect(store.isTA).toBe(false)
    expect(store.isStudent).toBe(false)
  })

  it('should compute isTA for TEACHING_ASSISTANT role', () => {
    const store = useAuthStore()
    store.roles = ['TEACHING_ASSISTANT']
    expect(store.isTA).toBe(true)
  })

  it('should compute isInstructor for INSTRUCTOR role', () => {
    const store = useAuthStore()
    store.roles = ['INSTRUCTOR']
    expect(store.isInstructor).toBe(true)
  })

  it('should remove all localStorage keys on logout', async () => {
    localStorage.setItem('scholarops_token', 'tok')
    localStorage.setItem('scholarops_refresh', 'ref')
    localStorage.setItem('scholarops_user', '{"id":1}')
    localStorage.setItem('scholarops_roles', '["ADMINISTRATOR"]')
    localStorage.setItem('scholarops_permissions', '["USER_MANAGE"]')

    const store = useAuthStore()
    await store.logout()

    expect(localStorage.getItem('scholarops_token')).toBeNull()
    expect(localStorage.getItem('scholarops_refresh')).toBeNull()
    expect(localStorage.getItem('scholarops_user')).toBeNull()
    expect(localStorage.getItem('scholarops_roles')).toBeNull()
    expect(localStorage.getItem('scholarops_permissions')).toBeNull()
  })

  it('should clear refreshToken on logout', async () => {
    const store = useAuthStore()
    store.refreshToken = 'refresh-token-123'
    await store.logout()
    expect(store.refreshToken).toBeNull()
  })

  it('should clear permissions on logout', async () => {
    const store = useAuthStore()
    store.permissions = ['USER_MANAGE', 'AUDIT_VIEW']
    await store.logout()
    expect(store.permissions).toEqual([])
  })

  it('should initialize from localStorage if data is present', () => {
    localStorage.setItem('scholarops_token', 'stored-token')
    localStorage.setItem('scholarops_roles', '["STUDENT"]')
    localStorage.setItem('scholarops_permissions', '["CONTENT_VIEW"]')
    localStorage.setItem('scholarops_user', '{"id":5,"username":"student1"}')

    setActivePinia(createPinia())
    const store = useAuthStore()

    expect(store.token).toBe('stored-token')
    expect(store.isAuthenticated).toBe(true)
    expect(store.roles).toEqual(['STUDENT'])
    expect(store.permissions).toEqual(['CONTENT_VIEW'])
    expect(store.user).toEqual({ id: 5, username: 'student1' })
    expect(store.isStudent).toBe(true)
  })
})
