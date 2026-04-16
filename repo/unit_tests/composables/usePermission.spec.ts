import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'
import { ROLE_PERMISSIONS, PERMISSIONS } from '@/utils/permissions'

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should delegate hasRole to the auth store', () => {
    const authStore = useAuthStore()
    authStore.roles = ['ADMINISTRATOR']

    const { hasRole } = usePermission()
    expect(hasRole('ADMINISTRATOR')).toBe(true)
    expect(hasRole('STUDENT')).toBe(false)
  })

  it('should delegate hasPermission to the auth store', () => {
    const authStore = useAuthStore()
    authStore.permissions = [PERMISSIONS.USER_MANAGE, PERMISSIONS.AUDIT_VIEW]

    const { hasPermission } = usePermission()
    expect(hasPermission(PERMISSIONS.USER_MANAGE)).toBe(true)
    expect(hasPermission(PERMISSIONS.QUIZ_MANAGE)).toBe(false)
  })

  it('should check hasAnyRole correctly', () => {
    const authStore = useAuthStore()
    authStore.roles = ['INSTRUCTOR']

    const { hasAnyRole } = usePermission()
    expect(hasAnyRole(['ADMINISTRATOR', 'INSTRUCTOR'])).toBe(true)
    expect(hasAnyRole(['ADMINISTRATOR', 'STUDENT'])).toBe(false)
  })

  it('should check hasAllPermissions correctly', () => {
    const authStore = useAuthStore()
    authStore.permissions = [PERMISSIONS.USER_MANAGE, PERMISSIONS.AUDIT_VIEW, PERMISSIONS.ROLE_ASSIGN]

    const { hasAllPermissions } = usePermission()
    expect(hasAllPermissions([PERMISSIONS.USER_MANAGE, PERMISSIONS.AUDIT_VIEW])).toBe(true)
    expect(hasAllPermissions([PERMISSIONS.USER_MANAGE, PERMISSIONS.QUIZ_MANAGE])).toBe(false)
  })

  it('should return true for hasAllPermissions with empty array', () => {
    const authStore = useAuthStore()
    authStore.permissions = []

    const { hasAllPermissions } = usePermission()
    expect(hasAllPermissions([])).toBe(true)
  })

  describe('canAccess', () => {
    it('should allow ADMINISTRATOR to access admin routes', () => {
      const authStore = useAuthStore()
      authStore.roles = ['ADMINISTRATOR']

      const { canAccess } = usePermission()
      expect(canAccess('admin/users')).toBe(true)
      expect(canAccess('/admin/settings')).toBe(true)
    })

    it('should allow CONTENT_CURATOR to access curator routes', () => {
      const authStore = useAuthStore()
      authStore.roles = ['CONTENT_CURATOR']

      const { canAccess } = usePermission()
      expect(canAccess('curator/sources')).toBe(true)
      expect(canAccess('/curator/rules')).toBe(true)
    })

    it('should allow INSTRUCTOR to access instructor and ta routes', () => {
      const authStore = useAuthStore()
      authStore.roles = ['INSTRUCTOR']

      const { canAccess } = usePermission()
      expect(canAccess('instructor/quizzes')).toBe(true)
      expect(canAccess('ta/grading')).toBe(true)
    })

    it('should allow TEACHING_ASSISTANT to access ta routes', () => {
      const authStore = useAuthStore()
      authStore.roles = ['TEACHING_ASSISTANT']

      const { canAccess } = usePermission()
      expect(canAccess('ta/submissions')).toBe(true)
      expect(canAccess('instructor/quizzes')).toBe(false)
    })

    it('should allow STUDENT to access student routes', () => {
      const authStore = useAuthStore()
      authStore.roles = ['STUDENT']

      const { canAccess } = usePermission()
      expect(canAccess('student/schedule')).toBe(true)
      expect(canAccess('/student/catalog')).toBe(true)
    })

    it('should deny access if role does not match route prefix', () => {
      const authStore = useAuthStore()
      authStore.roles = ['STUDENT']

      const { canAccess } = usePermission()
      expect(canAccess('admin/users')).toBe(false)
      expect(canAccess('curator/sources')).toBe(false)
    })

    it('should return false for unknown roles', () => {
      const authStore = useAuthStore()
      authStore.roles = ['UNKNOWN_ROLE']

      const { canAccess } = usePermission()
      expect(canAccess('admin/anything')).toBe(false)
    })
  })

  describe('effectivePermissions', () => {
    it('should include permissions from both store permissions and role-based permissions', () => {
      const authStore = useAuthStore()
      authStore.roles = ['STUDENT']
      authStore.permissions = ['CUSTOM_EXTRA']

      const { effectivePermissions } = usePermission()
      const perms = effectivePermissions.value

      expect(perms).toContain('CUSTOM_EXTRA')
      expect(perms).toContain(PERMISSIONS.QUIZ_TAKE)
      expect(perms).toContain(PERMISSIONS.CONTENT_VIEW)
      expect(perms).toContain(PERMISSIONS.SCHEDULE_MANAGE_OWN)
    })

    it('should deduplicate permissions', () => {
      const authStore = useAuthStore()
      authStore.roles = ['STUDENT']
      authStore.permissions = [PERMISSIONS.CONTENT_VIEW]

      const { effectivePermissions } = usePermission()
      const perms = effectivePermissions.value
      const contentViewCount = perms.filter(p => p === PERMISSIONS.CONTENT_VIEW).length
      expect(contentViewCount).toBe(1)
    })

    it('should aggregate permissions from multiple roles', () => {
      const authStore = useAuthStore()
      authStore.roles = ['INSTRUCTOR', 'TEACHING_ASSISTANT']
      authStore.permissions = []

      const { effectivePermissions } = usePermission()
      const perms = effectivePermissions.value

      expect(perms).toContain(PERMISSIONS.QUIZ_MANAGE)
      expect(perms).toContain(PERMISSIONS.QUESTION_BANK_MANAGE)
      expect(perms).toContain(PERMISSIONS.GRADING_MANAGE)
    })

    it('should return empty array if no roles and no permissions', () => {
      const authStore = useAuthStore()
      authStore.roles = []
      authStore.permissions = []

      const { effectivePermissions } = usePermission()
      expect(effectivePermissions.value).toEqual([])
    })
  })
})
