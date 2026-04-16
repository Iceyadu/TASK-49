import { expect, test } from '@playwright/test'
import { apiLogin, createUserAsAdmin, uiLogin } from './helpers/fullstack-fixtures'

test.describe('Fullstack admin mutation workflow', () => {
  test('admin disables a user from user management table', async ({ page, request }) => {
    const adminToken = await apiLogin(request, 'admin', 'Admin@12345678')
    const user = await createUserAsAdmin(request, adminToken, 'STUDENT', 'e2e.mutation')

    await uiLogin(page, 'admin', 'Admin@12345678')
    await page.goto('/admin/users')
    await expect(page.getByRole('heading', { name: 'User Management' })).toBeVisible()

    const userRow = page.locator('tr', { hasText: user.username }).first()
    await expect(userRow).toBeVisible()
    await userRow.getByRole('button', { name: 'Disable' }).click()

    await expect(page.locator('tr', { hasText: user.username }).first()).toContainText('Disabled')
  })
})
