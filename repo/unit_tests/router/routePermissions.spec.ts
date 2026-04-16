import { describe, it, expect, beforeAll } from 'vitest'
import { PERMISSIONS } from '@/utils/permissions'
import router from '@/router'

let capturedRoutes: any[] = []

// Helper to flatten nested route children into a flat list with full paths
function flattenRoutes(routes: any[], parentPath = ''): any[] {
  const result: any[] = []
  for (const route of routes) {
    const fullPath = parentPath
      ? `${parentPath}/${route.path}`.replace(/\/+/g, '/')
      : route.path
    result.push({ ...route, fullPath })
    if (route.children) {
      result.push(...flattenRoutes(route.children, fullPath))
    }
  }
  return result
}

function findRouteByName(routes: any[], name: string): any {
  const flat = flattenRoutes(routes)
  return flat.find((r: any) => r.name === name)
}

describe('Route Permissions Configuration', () => {
  let allRoutes: any[]

  beforeAll(() => {
    capturedRoutes = (router.options.routes as any[]) ?? []
    allRoutes = flattenRoutes(capturedRoutes)
  })

  describe('Public routes', () => {
    it('login route has meta.public = true', () => {
      const login = findRouteByName(capturedRoutes, 'login')
      expect(login).toBeDefined()
      expect(login.meta.public).toBe(true)
    })

    it('login route does not require roles or permissions', () => {
      const login = findRouteByName(capturedRoutes, 'login')
      expect(login.meta.roles).toBeUndefined()
      expect(login.meta.permissions).toBeUndefined()
    })
  })

  describe('Admin routes', () => {
    const adminRoutes = [
      { name: 'admin-users', permission: PERMISSIONS.USER_MANAGE },
      { name: 'admin-roles', permission: PERMISSIONS.ROLE_ASSIGN },
      { name: 'admin-audit', permission: PERMISSIONS.AUDIT_VIEW }
    ]

    adminRoutes.forEach(({ name, permission }) => {
      it(`${name} requires ADMINISTRATOR role`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toContain('ADMINISTRATOR')
      })

      it(`${name} requires ${permission} permission`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toContain(permission)
      })
    })
  })

  describe('Curator routes', () => {
    const curatorRoutes = [
      { name: 'curator-sources', permission: PERMISSIONS.CRAWL_SOURCE_MANAGE },
      { name: 'curator-rules', permission: PERMISSIONS.CRAWL_RULE_MANAGE },
      { name: 'curator-runs', permission: PERMISSIONS.CRAWL_RUN_MANAGE },
      { name: 'curator-content', permission: PERMISSIONS.CONTENT_VIEW }
    ]

    curatorRoutes.forEach(({ name, permission }) => {
      it(`${name} requires CONTENT_CURATOR role`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toContain('CONTENT_CURATOR')
      })

      it(`${name} requires ${permission} permission`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toContain(permission)
      })
    })
  })

  describe('Instructor routes', () => {
    const instructorRoutes = [
      { name: 'instructor-banks', permission: PERMISSIONS.QUESTION_BANK_MANAGE },
      { name: 'instructor-quizzes', permission: PERMISSIONS.QUIZ_MANAGE },
      { name: 'instructor-quiz-detail', permission: PERMISSIONS.QUIZ_MANAGE },
      { name: 'instructor-submissions', permission: PERMISSIONS.SUBMISSION_VIEW_ALL }
    ]

    instructorRoutes.forEach(({ name, permission }) => {
      it(`${name} requires INSTRUCTOR role`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toContain('INSTRUCTOR')
      })

      it(`${name} requires ${permission} permission`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toContain(permission)
      })
    })
  })

  describe('Student routes', () => {
    const studentRoutesWithPermissions = [
      { name: 'student-catalog', permission: PERMISSIONS.CONTENT_VIEW },
      { name: 'student-timetable', permission: PERMISSIONS.SCHEDULE_MANAGE_OWN },
      { name: 'student-assessment', permission: PERMISSIONS.QUIZ_TAKE },
      { name: 'student-wrong-answers', permission: PERMISSIONS.WRONG_ANSWER_VIEW_OWN }
    ]

    it('student-dashboard requires STUDENT role', () => {
      const route = findRouteByName(capturedRoutes, 'student-dashboard')
      expect(route).toBeDefined()
      expect(route.meta.roles).toContain('STUDENT')
    })

    studentRoutesWithPermissions.forEach(({ name, permission }) => {
      it(`${name} requires STUDENT role`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toContain('STUDENT')
      })

      it(`${name} requires ${permission} permission`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toContain(permission)
      })
    })
  })

  describe('TA routes', () => {
    const taRoutes = [
      { name: 'ta-grading', permission: PERMISSIONS.GRADING_VIEW },
      { name: 'ta-grading-detail', permission: PERMISSIONS.GRADING_VIEW }
    ]

    taRoutes.forEach(({ name, permission }) => {
      it(`${name} requires TEACHING_ASSISTANT or INSTRUCTOR role`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toContain('TEACHING_ASSISTANT')
        expect(route.meta.roles).toContain('INSTRUCTOR')
      })

      it(`${name} requires ${permission} permission`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toContain(permission)
      })
    })
  })

  describe('Protected routes have roles and permissions defined', () => {
    // Routes that should be protected (not public, not utility pages)
    const protectedRouteNames = [
      'admin-users', 'admin-roles', 'admin-audit',
      'curator-sources', 'curator-rules', 'curator-runs', 'curator-content',
      'instructor-banks', 'instructor-quizzes', 'instructor-quiz-detail', 'instructor-submissions',
      'student-catalog', 'student-timetable', 'student-assessment', 'student-wrong-answers',
      'ta-grading', 'ta-grading-detail'
    ]

    protectedRouteNames.forEach(name => {
      it(`${name} has roles defined`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.roles).toBeDefined()
        expect(Array.isArray(route.meta.roles)).toBe(true)
        expect(route.meta.roles.length).toBeGreaterThan(0)
      })

      it(`${name} has permissions defined`, () => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route).toBeDefined()
        expect(route.meta.permissions).toBeDefined()
        expect(Array.isArray(route.meta.permissions)).toBe(true)
        expect(route.meta.permissions.length).toBeGreaterThan(0)
      })
    })
  })

  describe('Student dashboard special case', () => {
    it('student-dashboard has roles but no permissions (unrestricted within role)', () => {
      const route = findRouteByName(capturedRoutes, 'student-dashboard')
      expect(route).toBeDefined()
      expect(route.meta.roles).toContain('STUDENT')
      // student-dashboard intentionally does not specify permissions
      expect(route.meta.permissions).toBeUndefined()
    })
  })

  describe('Route structure integrity', () => {
    it('forbidden route exists', () => {
      const route = findRouteByName(capturedRoutes, 'forbidden')
      expect(route).toBeDefined()
    })

    it('not-found catch-all route exists', () => {
      const route = findRouteByName(capturedRoutes, 'not-found')
      expect(route).toBeDefined()
    })

    it('dashboard route exists', () => {
      const route = findRouteByName(capturedRoutes, 'dashboard')
      expect(route).toBeDefined()
    })

    it('admin routes use only ADMINISTRATOR role (not mixed)', () => {
      const adminNames = ['admin-users', 'admin-roles', 'admin-audit']
      adminNames.forEach(name => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route.meta.roles).toEqual(['ADMINISTRATOR'])
      })
    })

    it('curator routes use only CONTENT_CURATOR role (not mixed)', () => {
      const curatorNames = ['curator-sources', 'curator-rules', 'curator-runs', 'curator-content']
      curatorNames.forEach(name => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route.meta.roles).toEqual(['CONTENT_CURATOR'])
      })
    })

    it('instructor routes use only INSTRUCTOR role (not mixed)', () => {
      const instructorNames = ['instructor-banks', 'instructor-quizzes', 'instructor-quiz-detail', 'instructor-submissions']
      instructorNames.forEach(name => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route.meta.roles).toEqual(['INSTRUCTOR'])
      })
    })

    it('TA routes allow both TEACHING_ASSISTANT and INSTRUCTOR', () => {
      const taNames = ['ta-grading', 'ta-grading-detail']
      taNames.forEach(name => {
        const route = findRouteByName(capturedRoutes, name)
        expect(route.meta.roles).toEqual(
          expect.arrayContaining(['TEACHING_ASSISTANT', 'INSTRUCTOR'])
        )
        expect(route.meta.roles).toHaveLength(2)
      })
    })

    it('all permission values reference valid PERMISSIONS constants', () => {
      const validPermissions = Object.values(PERMISSIONS)
      const protectedRoutes = allRoutes.filter(
        (r: any) => r.meta?.permissions && r.meta.permissions.length > 0
      )

      protectedRoutes.forEach((route: any) => {
        route.meta.permissions.forEach((perm: string) => {
          expect(validPermissions).toContain(perm)
        })
      })
    })
  })
})
