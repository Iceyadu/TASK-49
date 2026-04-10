<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>ScholarOps</h1>
        <p>Offline Learning Platform</p>
      </div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">Username</label>
          <input id="username" v-model="username" type="text" placeholder="Enter username" required autocomplete="username" />
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" v-model="password" type="password" placeholder="Enter password" required autocomplete="current-password" />
        </div>
        <div v-if="error" class="error-message">{{ error }}</div>
        <button type="submit" :disabled="loading" class="login-btn">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await authStore.login(username.value, password.value)
    router.push('/')
  } catch (e: any) {
    error.value = e.response?.data?.error?.message || 'Invalid credentials'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1a365d 0%, #2d3748 100%); }
.login-card { background: white; border-radius: 12px; padding: 48px; width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.login-header { text-align: center; margin-bottom: 32px; }
.login-header h1 { font-size: 28px; color: #1a365d; margin-bottom: 4px; }
.login-header p { color: #718096; font-size: 14px; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 600; color: #4a5568; font-size: 14px; }
.form-group input { width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 14px; }
.form-group input:focus { outline: none; border-color: #3182ce; box-shadow: 0 0 0 3px rgba(49,130,206,0.1); }
.error-message { background: #fed7d7; color: #c53030; padding: 10px; border-radius: 6px; margin-bottom: 16px; font-size: 13px; }
.login-btn { width: 100%; padding: 12px; background: #3182ce; color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; }
.login-btn:hover { background: #2b6cb0; }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
