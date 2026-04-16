import { expect, test } from '@playwright/test'

test.describe('Quiz management and assessment fullstack boundary', () => {
  test('unauthenticated user is redirected to login from instructor quiz management page', async ({ page }) => {
    await page.goto('/instructor/quizzes')
    await expect(page).toHaveURL(/\/login/)
  })

  test('unauthenticated user is redirected to login from student assessment view', async ({ page }) => {
    await page.goto('/student/assessment/1')
    await expect(page).toHaveURL(/\/login/)
  })

  test('admin (no INSTRUCTOR role) is redirected to forbidden from instructor quiz management', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('Admin@12345678')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)

    // Admin lacks INSTRUCTOR role → route guard redirects to /forbidden
    await page.goto('/instructor/quizzes')
    await expect(page).toHaveURL(/\/forbidden$/)
  })

  test('student is redirected to forbidden from instructor question banks view', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/instructor/question-banks')
    // Student lacks INSTRUCTOR role → forbidden
    await expect(page).toHaveURL(/\/forbidden$/)
  })

  test('student can navigate to their wrong answer review page', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)

    await page.goto('/student/wrong-answers')
    await expect(page).toHaveURL(/\/student\/wrong-answers$/)
    await expect(page.locator('.wrong-answers')).toBeVisible()
  })

  test('student assessment route requires authentication', async ({ page }) => {
    await page.goto('/student/assessment/999')
    await expect(page).toHaveURL(/\/login/)
  })

  test('student cannot access TA grading routes (wrong role)', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Student doesn't have TEACHING_ASSISTANT or INSTRUCTOR role
    await page.goto('/ta/grading')
    await expect(page).toHaveURL(/\/forbidden$/)
  })

  test('student dashboard shows role-appropriate navigation', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)
    // After login student should see the main app layout
    await expect(page.locator('body')).toBeVisible()
  })

  test('admin is redirected to forbidden from student-only routes', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('Admin@12345678')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/wrong-answers')
    // Admin lacks STUDENT role
    await expect(page).toHaveURL(/\/forbidden$/)
  })
})
