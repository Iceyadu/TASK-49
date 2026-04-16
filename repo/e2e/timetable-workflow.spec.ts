import { expect, test } from '@playwright/test'

test.describe('Timetable editing fullstack boundary', () => {
  test('unauthenticated user is redirected to login from timetable page', async ({ page }) => {
    await page.goto('/student/timetable')
    await expect(page).toHaveURL(/\/login/)
  })

  test('student can sign in and navigate to their timetable', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)

    await page.goto('/student/timetable')
    await expect(page).toHaveURL(/\/student\/timetable$/)
  })

  test('timetable view renders the editor grid', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/timetable')
    await expect(page.locator('.timetable-editor')).toBeVisible({ timeout: 10000 })
  })

  test('timetable grid has 7 day header columns', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/timetable')
    await page.locator('.timetable-editor').waitFor({ timeout: 10000 })
    const dayHeaders = page.locator('.timetable-editor__day-header')
    await expect(dayHeaders).toHaveCount(7)
  })

  test('timetable time slots are rendered in the grid', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/timetable')
    await page.locator('.timetable-editor').waitFor({ timeout: 10000 })
    const slots = page.locator('.timetable-editor__slot')
    await expect(slots.first()).toBeVisible()
    const slotCount = await slots.count()
    expect(slotCount).toBeGreaterThan(0)
  })

  test('admin (no STUDENT role) is redirected to forbidden from timetable', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('Admin@12345678')
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Admin doesn't have STUDENT role → route guard sends them to /forbidden
    await page.goto('/student/timetable')
    await expect(page).toHaveURL(/\/forbidden$/)
  })

  test('timetable view has undo and redo controls visible', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/timetable')
    await page.locator('.timetable-editor').waitFor({ timeout: 10000 })
    // Undo and redo buttons should be present in the timetable view
    const undoButton = page.getByRole('button', { name: /undo/i })
    const redoButton = page.getByRole('button', { name: /redo/i })
    await expect(undoButton).toBeVisible()
    await expect(redoButton).toBeVisible()
  })

  test('student curator routes are inaccessible to student role', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/curator/sources')
    await expect(page).toHaveURL(/\/forbidden$/)
  })

  test('admin routes are inaccessible to student role', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/admin/roles')
    await expect(page).toHaveURL(/\/forbidden$/)
  })
})
