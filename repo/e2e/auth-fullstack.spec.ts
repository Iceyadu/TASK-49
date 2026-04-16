import { expect, test } from '@playwright/test'

test.describe('Auth fullstack boundary', () => {
  test('unauthenticated user is redirected to login from protected route', async ({ page }) => {
    await page.goto('/admin/users')
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { name: 'ScholarOps' })).toBeVisible()
  })

  test('admin can sign in and access user management page', async ({ page }) => {
    await page.goto('/login')

    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('Admin@12345678')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await expect(page).toHaveURL(/\/$/)
    await page.goto('/admin/users')

    await expect(page).toHaveURL(/\/admin\/users$/)
    await expect(page.getByRole('heading', { name: 'User Management' })).toBeVisible()
  })
})
