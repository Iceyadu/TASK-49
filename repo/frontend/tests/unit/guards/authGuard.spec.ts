import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { authGuard, guestGuard } from '@/guards/authGuard'
import { PERMISSIONS } from '@/utils/permissions'
import type { RouteLocationNormalized, NavigationGuardNext } from 'vue-router'

function createMockRoute(overrides: Partial<RouteLocationNormalized> = {}): RouteLocationNormalized {
  return {
    path: '/test',
    fullPath: '/test',
    name: 'test',
    hash: '',
    query: {},
    params: {},
    matched: [],
    redirectedFrom: undefined,
    meta: {},
    ...overrides
  } as RouteLocationNormalized
}

describe('authGuard', () => {
  let authStore: ReturnType<typeof useAuthStore>
  let next: NavigationGuardNext

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authStore = useAuthStore()
    next = vi.fn() as unknown as NavigationGuardNext
  })

  describe('public routes', () => {
    it('allows access to routes with meta.public = true without authentication', () => {
      const to = createMockRoute({ meta: { public: true } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('allows access to public routes even when authenticated', () => {
      authStore.token = 'valid-token'
      authStore.roles = ['STUDENT']
      const to = createMockRoute({ meta: { public: true } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })
  })

  describe('unauthenticated users', () => {
    it('redirects unauthenticated users to /login', () => {
      const to = createMockRoute({ path: '/admin/users', fullPath: '/admin/users' })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith({
        path: '/login',
        query: { redirect: '/admin/users' }
      })
    })

    it('includes the full path with query params in the redirect', () => {
      const to = createMockRoute({
        path: '/instructor/quizzes',
        fullPath: '/instructor/quizzes?page=2'
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({
        path: '/login',
        query: { redirect: '/instructor/quizzes?page=2' }
      })
    })

    it('redirects to login when token is null', () => {
      authStore.token = null
      const to = createMockRoute({ fullPath: '/dashboard' })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({
        path: '/login',
        query: { redirect: '/dashboard' }
      })
    })
  })

  describe('role-based access control', () => {
    beforeEach(() => {
      authStore.token = 'valid-token'
    })

    it('allows access when user has a required role', () => {
      authStore.roles = ['ADMINISTRATOR']
      const to = createMockRoute({ meta: { roles: ['ADMINISTRATOR'] } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('allows access when user has one of multiple required roles', () => {
      authStore.roles = ['INSTRUCTOR']
      const to = createMockRoute({
        meta: { roles: ['TEACHING_ASSISTANT', 'INSTRUCTOR'] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('blocks access when user is missing the required role', () => {
      authStore.roles = ['STUDENT']
      const to = createMockRoute({ meta: { roles: ['ADMINISTRATOR'] } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('blocks access when user has no roles and route requires a role', () => {
      authStore.roles = []
      const to = createMockRoute({ meta: { roles: ['INSTRUCTOR'] } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('allows access when route has no role requirements', () => {
      authStore.roles = ['STUDENT']
      const to = createMockRoute({ meta: {} })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('allows access when roles meta is an empty array', () => {
      authStore.roles = ['STUDENT']
      const to = createMockRoute({ meta: { roles: [] } })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })
  })

  describe('permission-based access control', () => {
    beforeEach(() => {
      authStore.token = 'valid-token'
      authStore.roles = ['INSTRUCTOR']
    })

    it('allows access when user has all required permissions', () => {
      authStore.permissions = [PERMISSIONS.QUIZ_MANAGE, PERMISSIONS.QUESTION_BANK_MANAGE]
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUIZ_MANAGE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('blocks access when user is missing a required permission', () => {
      authStore.permissions = [PERMISSIONS.QUIZ_MANAGE]
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUIZ_MANAGE, PERMISSIONS.QUIZ_TAKE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('blocks access when user has none of the required permissions', () => {
      authStore.permissions = []
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUESTION_BANK_MANAGE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('requires ALL permissions (every), not just some', () => {
      authStore.permissions = [PERMISSIONS.GRADING_VIEW]
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.GRADING_VIEW, PERMISSIONS.GRADING_MANAGE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('allows access when permissions meta is an empty array', () => {
      authStore.permissions = []
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'], permissions: [] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('allows access when no permissions meta is defined', () => {
      authStore.permissions = []
      const to = createMockRoute({
        meta: { roles: ['INSTRUCTOR'] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })
  })

  describe('combined role and permission checks', () => {
    beforeEach(() => {
      authStore.token = 'valid-token'
    })

    it('checks roles before permissions - blocks on role first', () => {
      authStore.roles = ['STUDENT']
      authStore.permissions = [PERMISSIONS.USER_MANAGE]
      const to = createMockRoute({
        meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.USER_MANAGE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })

    it('allows when both role and permissions match', () => {
      authStore.roles = ['ADMINISTRATOR']
      authStore.permissions = [PERMISSIONS.USER_MANAGE, PERMISSIONS.AUDIT_VIEW]
      const to = createMockRoute({
        meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.USER_MANAGE] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })

    it('blocks when role matches but permissions do not', () => {
      authStore.roles = ['ADMINISTRATOR']
      authStore.permissions = [PERMISSIONS.USER_MANAGE]
      const to = createMockRoute({
        meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.USER_MANAGE, PERMISSIONS.AUDIT_VIEW] }
      })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledWith({ name: 'forbidden' })
    })
  })

  describe('authenticated user without role/permission constraints', () => {
    it('allows authenticated user to access route with no meta restrictions', () => {
      authStore.token = 'valid-token'
      authStore.roles = ['STUDENT']
      const to = createMockRoute({ meta: {} })
      const from = createMockRoute()

      authGuard(to, from, next)

      expect(next).toHaveBeenCalledTimes(1)
      expect(next).toHaveBeenCalledWith()
    })
  })
})

describe('guestGuard', () => {
  let authStore: ReturnType<typeof useAuthStore>
  let next: NavigationGuardNext

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authStore = useAuthStore()
    next = vi.fn() as unknown as NavigationGuardNext
  })

  it('allows unauthenticated users to proceed', () => {
    const to = createMockRoute({ path: '/login' })
    const from = createMockRoute()

    guestGuard(to, from, next)

    expect(next).toHaveBeenCalledTimes(1)
    expect(next).toHaveBeenCalledWith()
  })

  it('redirects authenticated users to dashboard', () => {
    authStore.token = 'valid-token'
    const to = createMockRoute({ path: '/login' })
    const from = createMockRoute()

    guestGuard(to, from, next)

    expect(next).toHaveBeenCalledTimes(1)
    expect(next).toHaveBeenCalledWith({ name: 'dashboard' })
  })

  it('considers user with token as authenticated', () => {
    authStore.token = 'any-token'
    const to = createMockRoute()
    const from = createMockRoute()

    guestGuard(to, from, next)

    expect(next).toHaveBeenCalledWith({ name: 'dashboard' })
  })

  it('considers user without token as unauthenticated', () => {
    authStore.token = null
    const to = createMockRoute()
    const from = createMockRoute()

    guestGuard(to, from, next)

    expect(next).toHaveBeenCalledWith()
  })
})
