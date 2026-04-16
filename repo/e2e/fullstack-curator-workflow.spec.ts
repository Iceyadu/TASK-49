import { expect, test } from '@playwright/test'
import { apiLogin, createUserAsAdmin, uiLogin } from './helpers/fullstack-fixtures'

test.describe('Fullstack curator workflow', () => {
  test('curator creates and edits crawl source from UI', async ({ page, request }) => {
    const adminToken = await apiLogin(request, 'admin', 'Admin@12345678')
    const curator = await createUserAsAdmin(request, adminToken, 'CONTENT_CURATOR', 'e2e.curator')

    await uiLogin(page, curator.username, curator.password)
    await page.goto('/curator/sources')
    await expect(page.getByRole('heading', { name: 'Crawl Sources' })).toBeVisible()

    await page.getByRole('button', { name: 'Add Source' }).click()
    await page.locator('#sourceName').fill('E2E Source')
    await page.locator('#baseUrl').fill('https://example.org')
    await page.locator('#description').fill('Created from Playwright fullstack test')
    await page.locator('#rateLimit').fill('45')
    await page.getByRole('button', { name: 'Create Source' }).click()

    await expect(page.locator('.source-card', { hasText: 'E2E Source' })).toBeVisible()

    const sourceCard = page.locator('.source-card', { hasText: 'E2E Source' }).first()
    await sourceCard.getByRole('button', { name: 'Edit' }).click()
    await page.locator('#description').fill('Updated description from workflow test')
    await page.getByRole('button', { name: 'Update Source' }).click()

    await expect(page.locator('.source-card', { hasText: 'Updated description from workflow test' })).toBeVisible()
  })
})
