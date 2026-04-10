<template>
  <div class="role-management">
    <h1>Role Management</h1>
    <div class="roles-grid">
      <div v-for="role in roles" :key="role.id" class="role-card">
        <h3>{{ role.name.replace('_', ' ') }}</h3>
        <p>{{ role.description }}</p>
        <div class="permissions">
          <span v-for="perm in role.permissions" :key="perm.id" class="perm-tag">{{ perm.code }}</span>
        </div>
      </div>
    </div>
    <div v-if="loading" class="loading">Loading roles...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/api/client'

const roles = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/api/roles')
    roles.value = data.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})
</script>

<style scoped>
.role-management { padding: 24px; }
.role-management h1 { color: #1a365d; margin-bottom: 24px; }
.roles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 16px; }
.role-card { background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; }
.role-card h3 { color: #2d3748; text-transform: capitalize; margin-bottom: 8px; }
.role-card p { color: #718096; font-size: 14px; margin-bottom: 12px; }
.perm-tag { display: inline-block; background: #f0fff4; color: #38a169; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin: 2px; font-family: monospace; }
.loading { text-align: center; color: #718096; padding: 40px; }
</style>
