import { expect, test } from '@playwright/test'

test.describe('Catalog browse fullstack boundary', () => {
  test('unauthenticated user is redirected to login when accessing catalog', async ({ page }) => {
    await page.goto('/student/catalog')
    await expect(page).toHaveURL(/\/login/)
  })

  test('student can sign in and reach the catalog view', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)

    await page.goto('/student/catalog')
    await expect(page).toHaveURL(/\/student\/catalog$/)
    await expect(page.locator('.catalog-browser')).toBeVisible()
  })

  test('catalog search input is interactive after login', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/catalog')
    const searchInput = page.locator('.catalog-browser__search-input')
    await expect(searchInput).toBeVisible()
    await searchInput.fill('math')
    await expect(searchInput).toHaveValue('math')
  })

  test('catalog shows empty state or content grid after load', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/catalog')
    // After the catalog loads either items or the empty state should be visible
    await page.waitForFunction(() => {
      return document.querySelector('.catalog-browser__grid') !== null ||
             document.querySelector('.empty-state') !== null
    }, { timeout: 15000 })

    const hasGrid = await page.locator('.catalog-browser__grid').isVisible().catch(() => false)
    const hasEmpty = await page.locator('.empty-state').isVisible().catch(() => false)
    expect(hasGrid || hasEmpty).toBe(true)
  })

  test('catalog keyword filter chip appears after adding a keyword', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/catalog')
    const keywordInput = page.locator('.catalog-browser__keyword-input')
    await expect(keywordInput).toBeVisible()
    await keywordInput.fill('python')
    await keywordInput.press('Enter')

    // A keyword chip with "python" should appear
    await expect(page.locator('.catalog-browser__keyword-chip').filter({ hasText: 'python' })).toBeVisible()
  })

  test('non-student role (admin) is redirected to forbidden when accessing student catalog', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('admin')
    await page.locator('#password').fill('Admin@12345678')
    await page.getByRole('button', { name: 'Sign In' }).click()
    await expect(page).toHaveURL(/\/$/)

    // Admin doesn't have STUDENT role, so they should be redirected to /forbidden
    await page.goto('/student/catalog')
    await expect(page).not.toHaveURL(/\/student\/catalog$/)
  })

  test('sort by dropdown options are present on catalog page', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#username').fill('student.integration')
    await page.locator('#password').fill('Student@12345')
    await page.getByRole('button', { name: 'Sign In' }).click()

    await page.goto('/student/catalog')
    await expect(page.locator('.catalog-browser__sort-select')).toBeVisible()
    const sortOptions = page.locator('.catalog-browser__sort-select option')
    await expect(sortOptions).toHaveCount(5)
  })
})
