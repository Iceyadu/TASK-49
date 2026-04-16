import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

const frontendRoot = resolve(__dirname)
const repoRoot = resolve(frontendRoot, '..')

export default defineConfig({
  root: frontendRoot,
  server: {
    fs: {
      allow: [frontendRoot, repoRoot],
    },
  },
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['../unit_tests/**/*.spec.ts'],
  },
  resolve: {
    alias: {
      '@': resolve(frontendRoot, 'src'),
      '@vue/test-utils': resolve(frontendRoot, 'node_modules/@vue/test-utils/dist/vue-test-utils.esm-bundler.mjs'),
      pinia: resolve(frontendRoot, 'node_modules/pinia/dist/pinia.mjs'),
      'vue-router': resolve(frontendRoot, 'node_modules/vue-router/dist/vue-router.mjs'),
    },
  },
})
