import { describe, it, expect } from 'vitest'
import { PERMISSIONS, ROLE_PERMISSIONS, roleHasPermission } from '@/utils/permissions'

describe('Permission Format Alignment', () => {
  it('all PERMISSIONS keys are uppercase underscore format', () => {
    const keys = Object.keys(PERMISSIONS)
    expect(keys.length).toBeGreaterThan(0)

    for (const key of keys) {
      expect(key).toMatch(/^[A-Z][A-Z0-9_]*$/)
    }
  })

  it('all PERMISSIONS values are uppercase underscore format (matching backend PermissionType enum)', () => {
    const values = Object.values(PERMISSIONS)
    expect(values.length).toBeGreaterThan(0)

    for (const value of values) {
      expect(value).toMatch(/^[A-Z][A-Z0-9_]*$/)
    }
  })

  it('PERMISSIONS keys match their values', () => {
    for (const [key, value] of Object.entries(PERMISSIONS)) {
      expect(key).toBe(value)
    }
  })
})

describe('Role-Permission Validity', () => {
  const allValidPermissions = new Set(Object.values(PERMISSIONS))

  it('all defined roles are non-empty arrays', () => {
    for (const [role, perms] of Object.entries(ROLE_PERMISSIONS)) {
      expect(Array.isArray(perms), `${role} permissions should be an array`).toBe(true)
      expect(perms.length, `${role} should have at least one permission`).toBeGreaterThan(0)
    }
  })

  it('ADMINISTRATOR role contains only valid permission codes', () => {
    for (const perm of ROLE_PERMISSIONS['ADMINISTRATOR']) {
      expect(allValidPermissions.has(perm), `Unknown permission: ${perm}`).toBe(true)
    }
  })

  it('CONTENT_CURATOR role contains only valid permission codes', () => {
    for (const perm of ROLE_PERMISSIONS['CONTENT_CURATOR']) {
      expect(allValidPermissions.has(perm), `Unknown permission: ${perm}`).toBe(true)
    }
  })

  it('INSTRUCTOR role contains only valid permission codes', () => {
    for (const perm of ROLE_PERMISSIONS['INSTRUCTOR']) {
      expect(allValidPermissions.has(perm), `Unknown permission: ${perm}`).toBe(true)
    }
  })

  it('TEACHING_ASSISTANT role contains only valid permission codes', () => {
    for (const perm of ROLE_PERMISSIONS['TEACHING_ASSISTANT']) {
      expect(allValidPermissions.has(perm), `Unknown permission: ${perm}`).toBe(true)
    }
  })

  it('STUDENT role contains only valid permission codes', () => {
    for (const perm of ROLE_PERMISSIONS['STUDENT']) {
      expect(allValidPermissions.has(perm), `Unknown permission: ${perm}`).toBe(true)
    }
  })

  it('every role in ROLE_PERMISSIONS contains only valid permission codes', () => {
    for (const [role, perms] of Object.entries(ROLE_PERMISSIONS)) {
      for (const perm of perms) {
        expect(
          allValidPermissions.has(perm),
          `Role "${role}" has unknown permission: "${perm}"`
        ).toBe(true)
      }
    }
  })

  it('no role has duplicate permissions', () => {
    for (const [role, perms] of Object.entries(ROLE_PERMISSIONS)) {
      const unique = new Set(perms)
      expect(
        unique.size,
        `Role "${role}" has duplicate permissions`
      ).toBe(perms.length)
    }
  })
})

describe('roleHasPermission', () => {
  it('returns true when role has the permission', () => {
    expect(roleHasPermission('ADMINISTRATOR', 'USER_MANAGE')).toBe(true)
    expect(roleHasPermission('INSTRUCTOR', 'QUIZ_MANAGE')).toBe(true)
    expect(roleHasPermission('STUDENT', 'QUIZ_TAKE')).toBe(true)
    expect(roleHasPermission('TEACHING_ASSISTANT', 'GRADING_MANAGE')).toBe(true)
    expect(roleHasPermission('CONTENT_CURATOR', 'CONTENT_REVIEW')).toBe(true)
  })

  it('returns false when role does not have the permission', () => {
    expect(roleHasPermission('STUDENT', 'USER_MANAGE')).toBe(false)
    expect(roleHasPermission('TEACHING_ASSISTANT', 'QUIZ_MANAGE')).toBe(false)
    expect(roleHasPermission('CONTENT_CURATOR', 'GRADING_MANAGE')).toBe(false)
  })

  it('returns false for an unknown role', () => {
    expect(roleHasPermission('NONEXISTENT_ROLE', 'USER_MANAGE')).toBe(false)
  })

  it('returns false for an unknown permission on a valid role', () => {
    expect(roleHasPermission('ADMINISTRATOR', 'NONEXISTENT_PERM')).toBe(false)
  })
})
