<template>
  <header class="app-header">
    <div class="app-header__left">
      <h1 class="app-header__title">{{ pageTitle }}</h1>
    </div>
    <div class="app-header__right">
      <div class="app-header__roles">
        <span
          v-for="role in authStore.roles"
          :key="role"
          class="app-header__role-badge"
          :class="`app-header__role-badge--${role.toLowerCase()}`"
        >
          {{ formatRole(role) }}
        </span>
      </div>
      <div class="app-header__user">
        <span class="app-header__user-name">{{ authStore.user?.fullName || authStore.user?.username }}</span>
      </div>
      <button class="app-header__logout-btn" @click="handleLogout" type="button">
        Logout
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const pageTitle = computed(() => {
  const name = route.name as string | undefined
  if (!name) return 'ScholarOps'
  return name
    .replace(/-/g, ' ')
    .replace(/\b\w/g, c => c.toUpperCase())
})

function formatRole(role: string): string {
  return role.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  min-height: 56px;
}

.app-header__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-header__roles {
  display: flex;
  gap: 6px;
}

.app-header__role-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: #e2e8f0;
  color: #475569;
}

.app-header__role-badge--administrator {
  background: #fee2e2;
  color: #dc2626;
}

.app-header__role-badge--content_curator {
  background: #dbeafe;
  color: #2563eb;
}

.app-header__role-badge--instructor {
  background: #dcfce7;
  color: #16a34a;
}

.app-header__role-badge--teaching_assistant {
  background: #fef9c3;
  color: #ca8a04;
}

.app-header__role-badge--student {
  background: #f3e8ff;
  color: #9333ea;
}

.app-header__user-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: #334155;
}

.app-header__logout-btn {
  padding: 6px 16px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.app-header__logout-btn:hover {
  background: #dc2626;
}
</style>
