import { expect, test } from '@playwright/test'
import { apiLogin, createPublishedQuizAsInstructor, createUserAsAdmin, uiLogin } from './helpers/fullstack-fixtures'

test.describe('Fullstack quiz journey', () => {
  test('student takes and submits instructor-published quiz', async ({ page, request }) => {
    const adminToken = await apiLogin(request, 'admin', 'Admin@12345678')
    const instructor = await createUserAsAdmin(request, adminToken, 'INSTRUCTOR', 'e2e.instructor')
    const student = await createUserAsAdmin(request, adminToken, 'STUDENT', 'e2e.student')

    const instructorToken = await apiLogin(request, instructor.username, instructor.password)
    const quizId = await createPublishedQuizAsInstructor(request, instructorToken)

    await uiLogin(page, student.username, student.password)

    await page.goto(`/student/assessment/${quizId}`)
    await expect(page.getByRole('button', { name: 'Start Assessment' })).toBeVisible()
    await page.getByRole('button', { name: 'Start Assessment' }).click()

    await expect(page.getByText('Question 1 of 1')).toBeVisible()
    await page.locator('.assessment-view__options input[type="radio"]').first().check()
    await page.getByRole('button', { name: 'Submit Assessment' }).click()

    await expect(page).toHaveURL(/\/student\/dashboard$/)
  })
})
