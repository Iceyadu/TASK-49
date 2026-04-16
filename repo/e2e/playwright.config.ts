import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://host.docker.internal:5173',
    headless: true,
    trace: 'retain-on-failure',
  },
})
