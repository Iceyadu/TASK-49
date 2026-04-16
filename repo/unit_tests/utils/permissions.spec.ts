import { describe, it, expect } from 'vitest'
import { PERMISSIONS, ROLE_PERMISSIONS, roleHasPermission } from '@/utils/permissions'

describe('Permission Constants', () => {
  it('should define user management permissions', () => {
    expect(PERMISSIONS.USER_MANAGE).toBe('USER_MANAGE')
    expect(PERMISSIONS.PASSWORD_ADMIN_RESET).toBe('PASSWORD_ADMIN_RESET')
  })

  it('should define role management permissions', () => {
    expect(PERMISSIONS.ROLE_ASSIGN).toBe('ROLE_ASSIGN')
  })

  it('should define audit permissions', () => {
    expect(PERMISSIONS.AUDIT_VIEW).toBe('AUDIT_VIEW')
  })

  it('should define crawl source permissions', () => {
    expect(PERMISSIONS.CRAWL_SOURCE_MANAGE).toBe('CRAWL_SOURCE_MANAGE')
  })

  it('should define crawl rule permissions', () => {
    expect(PERMISSIONS.CRAWL_RULE_MANAGE).toBe('CRAWL_RULE_MANAGE')
    expect(PERMISSIONS.CRAWL_RUN_MANAGE).toBe('CRAWL_RUN_MANAGE')
  })

  it('should define quiz and question permissions', () => {
    expect(PERMISSIONS.QUESTION_BANK_MANAGE).toBe('QUESTION_BANK_MANAGE')
    expect(PERMISSIONS.QUIZ_MANAGE).toBe('QUIZ_MANAGE')
    expect(PERMISSIONS.QUIZ_TAKE).toBe('QUIZ_TAKE')
  })

  it('should define submission permissions', () => {
    expect(PERMISSIONS.SUBMISSION_VIEW_OWN).toBe('SUBMISSION_VIEW_OWN')
    expect(PERMISSIONS.SUBMISSION_VIEW_ALL).toBe('SUBMISSION_VIEW_ALL')
  })

  it('should define schedule permissions', () => {
    expect(PERMISSIONS.SCHEDULE_MANAGE_OWN).toBe('SCHEDULE_MANAGE_OWN')
  })

  it('should define catalog and plagiarism permissions', () => {
    expect(PERMISSIONS.CONTENT_VIEW).toBe('CONTENT_VIEW')
    expect(PERMISSIONS.CONTENT_REVIEW).toBe('CONTENT_REVIEW')
    expect(PERMISSIONS.PLAGIARISM_VIEW).toBe('PLAGIARISM_VIEW')
    expect(PERMISSIONS.WRONG_ANSWER_VIEW_OWN).toBe('WRONG_ANSWER_VIEW_OWN')
  })
})

describe('Role-Permission Mapping', () => {
  it('should give ADMINISTRATOR user and role management permissions', () => {
    const adminPerms = ROLE_PERMISSIONS['ADMINISTRATOR']
    expect(adminPerms).toContain(PERMISSIONS.USER_MANAGE)
    expect(adminPerms).toContain(PERMISSIONS.ROLE_ASSIGN)
    expect(adminPerms).toContain(PERMISSIONS.AUDIT_VIEW)
    expect(adminPerms).toContain(PERMISSIONS.PASSWORD_ADMIN_RESET)
  })

  it('should give CONTENT_CURATOR source and crawl permissions', () => {
    const curatorPerms = ROLE_PERMISSIONS['CONTENT_CURATOR']
    expect(curatorPerms).toContain(PERMISSIONS.CRAWL_SOURCE_MANAGE)
    expect(curatorPerms).toContain(PERMISSIONS.CRAWL_RULE_MANAGE)
    expect(curatorPerms).toContain(PERMISSIONS.CRAWL_RUN_MANAGE)
    expect(curatorPerms).toContain(PERMISSIONS.CONTENT_REVIEW)
    expect(curatorPerms).toContain(PERMISSIONS.CONTENT_VIEW)
  })

  it('should give INSTRUCTOR quiz and grading permissions', () => {
    const instructorPerms = ROLE_PERMISSIONS['INSTRUCTOR']
    expect(instructorPerms).toContain(PERMISSIONS.QUESTION_BANK_MANAGE)
    expect(instructorPerms).toContain(PERMISSIONS.QUIZ_MANAGE)
    expect(instructorPerms).toContain(PERMISSIONS.SUBMISSION_VIEW_ALL)
    expect(instructorPerms).toContain(PERMISSIONS.GRADING_MANAGE)
    expect(instructorPerms).toContain(PERMISSIONS.GRADING_VIEW)
    expect(instructorPerms).toContain(PERMISSIONS.PLAGIARISM_VIEW)
  })

  it('should give TEACHING_ASSISTANT grading and submission permissions', () => {
    const taPerms = ROLE_PERMISSIONS['TEACHING_ASSISTANT']
    expect(taPerms).toContain(PERMISSIONS.GRADING_VIEW)
    expect(taPerms).toContain(PERMISSIONS.GRADING_MANAGE)
    expect(taPerms).toContain(PERMISSIONS.SUBMISSION_VIEW_ALL)
    expect(taPerms).toContain(PERMISSIONS.PLAGIARISM_VIEW)
  })

  it('should give STUDENT submission, schedule, and catalog permissions', () => {
    const studentPerms = ROLE_PERMISSIONS['STUDENT']
    expect(studentPerms).toContain(PERMISSIONS.QUIZ_TAKE)
    expect(studentPerms).toContain(PERMISSIONS.SUBMISSION_VIEW_OWN)
    expect(studentPerms).toContain(PERMISSIONS.SCHEDULE_MANAGE_OWN)
    expect(studentPerms).toContain(PERMISSIONS.CONTENT_VIEW)
    expect(studentPerms).toContain(PERMISSIONS.WRONG_ANSWER_VIEW_OWN)
  })

  it('should not give STUDENT admin permissions', () => {
    const studentPerms = ROLE_PERMISSIONS['STUDENT']
    expect(studentPerms).not.toContain(PERMISSIONS.USER_MANAGE)
    expect(studentPerms).not.toContain(PERMISSIONS.ROLE_ASSIGN)
    expect(studentPerms).not.toContain(PERMISSIONS.AUDIT_VIEW)
  })

  it('should not give TEACHING_ASSISTANT quiz creation permissions', () => {
    const taPerms = ROLE_PERMISSIONS['TEACHING_ASSISTANT']
    expect(taPerms).not.toContain(PERMISSIONS.QUIZ_MANAGE)
    expect(taPerms).not.toContain(PERMISSIONS.QUESTION_BANK_MANAGE)
  })
})

describe('roleHasPermission', () => {
  it('should return true when role has the permission', () => {
    expect(roleHasPermission('ADMINISTRATOR', PERMISSIONS.USER_MANAGE)).toBe(true)
    expect(roleHasPermission('STUDENT', PERMISSIONS.CONTENT_VIEW)).toBe(true)
    expect(roleHasPermission('INSTRUCTOR', PERMISSIONS.QUIZ_MANAGE)).toBe(true)
  })

  it('should return false when role does not have the permission', () => {
    expect(roleHasPermission('STUDENT', PERMISSIONS.USER_MANAGE)).toBe(false)
    expect(roleHasPermission('TEACHING_ASSISTANT', PERMISSIONS.QUIZ_MANAGE)).toBe(false)
    expect(roleHasPermission('CONTENT_CURATOR', PERMISSIONS.GRADING_MANAGE)).toBe(false)
  })

  it('should return false for an unknown role', () => {
    expect(roleHasPermission('NONEXISTENT_ROLE', PERMISSIONS.USER_MANAGE)).toBe(false)
  })

  it('should return false for an unknown permission on a valid role', () => {
    expect(roleHasPermission('ADMINISTRATOR', 'NONEXISTENT_PERM')).toBe(false)
  })
})
