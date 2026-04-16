import { expect, type APIRequestContext, type Page } from '@playwright/test'

const apiBase = process.env.E2E_API_BASE || 'http://host.docker.internal:8080'

export async function apiLogin(request: APIRequestContext, username: string, password: string): Promise<string> {
  const response = await request.post(`${apiBase}/api/auth/login`, {
    data: { username, password },
  })
  expect(response.ok()).toBeTruthy()
  const json = await response.json()
  return json.data.accessToken as string
}

export async function createUserAsAdmin(
  request: APIRequestContext,
  adminToken: string,
  roleName: 'INSTRUCTOR' | 'STUDENT' | 'TEACHING_ASSISTANT' | 'CONTENT_CURATOR',
  prefix: string,
): Promise<{ id: number; username: string; password: string }> {
  const suffix = Date.now()
  const username = `${prefix}.${suffix}`
  const password = 'StrongPass@12345'

  const createUserResponse = await request.post(`${apiBase}/api/users`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: {
      username,
      email: `${username}@scholarops.local`,
      password,
      fullName: `${prefix} ${suffix}`,
    },
  })
  expect(createUserResponse.ok()).toBeTruthy()
  const createdUser = await createUserResponse.json()
  const userId = createdUser.data.id as number

  const rolesResponse = await request.get(`${apiBase}/api/roles`, {
    headers: { Authorization: `Bearer ${adminToken}` },
  })
  expect(rolesResponse.ok()).toBeTruthy()
  const rolesJson = await rolesResponse.json()
  const roleId = (rolesJson.data as Array<{ id: number; name: string }>).find((r) => r.name === roleName)?.id
  expect(roleId).toBeTruthy()

  const assignRoleResponse = await request.post(`${apiBase}/api/users/${userId}/roles`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { roleId },
  })
  expect(assignRoleResponse.ok()).toBeTruthy()

  return { id: userId, username, password }
}

export async function createPublishedQuizAsInstructor(
  request: APIRequestContext,
  instructorToken: string,
): Promise<number> {
  const suffix = Date.now()
  const bankResponse = await request.post(`${apiBase}/api/question-banks`, {
    headers: { Authorization: `Bearer ${instructorToken}` },
    data: {
      name: `E2E Bank ${suffix}`,
      description: 'E2E fullstack quiz workflow',
      subject: 'Testing',
    },
  })
  expect(bankResponse.ok()).toBeTruthy()
  const bankJson = await bankResponse.json()
  const bankId = bankJson.data.id as number

  const questionResponse = await request.post(`${apiBase}/api/question-banks/${bankId}/questions`, {
    headers: { Authorization: `Bearer ${instructorToken}` },
    data: {
      questionType: 'MULTIPLE_CHOICE',
      difficultyLevel: 2,
      questionText: 'Which layer should E2E tests validate?',
      options: ['Only mocks', 'Real frontend-backend boundary', 'Only CSS selectors'],
      correctAnswer: 'Real frontend-backend boundary',
      explanation: 'Confidence comes from real boundary behavior.',
      points: 5,
    },
  })
  expect(questionResponse.ok()).toBeTruthy()

  const assembleResponse = await request.post(`${apiBase}/api/quizzes/assemble`, {
    headers: { Authorization: `Bearer ${instructorToken}` },
    data: {
      title: `E2E Quiz ${suffix}`,
      description: 'Fullstack student workflow quiz',
      questionBankId: bankId,
      totalQuestions: 1,
      timeLimitMinutes: 20,
      maxAttempts: 2,
      shuffleQuestions: false,
      showImmediateFeedback: false,
      rules: [],
    },
  })
  expect(assembleResponse.ok()).toBeTruthy()
  const quizJson = await assembleResponse.json()
  const quizId = quizJson.data.id as number

  const publishResponse = await request.put(`${apiBase}/api/quizzes/${quizId}/publish`, {
    headers: { Authorization: `Bearer ${instructorToken}` },
  })
  expect(publishResponse.ok()).toBeTruthy()
  return quizId
}

export async function uiLogin(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.locator('#username').fill(username)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page).toHaveURL(/\/$/)
}

export function apiBaseUrl(): string {
  return apiBase
}

export function expectHttpStatus(actual: number, allowed: number[]) {
  expect(allowed, `Expected HTTP ${actual} in [${allowed.join(', ')}]`).toContain(actual)
}
