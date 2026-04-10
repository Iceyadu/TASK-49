<template>
  <div class="audit-history">
    <h1>Audit History</h1>
    <div class="tabs">
      <button :class="{ active: tab === 'audit' }" @click="tab = 'audit'; loadAuditLogs()">Audit Logs</button>
      <button :class="{ active: tab === 'permissions' }" @click="tab = 'permissions'; loadPermissionLogs()">Permission Changes</button>
    </div>
    <PermissionAuditLog v-if="tab === 'audit'" :entries="auditEntries" :loading="auditLoading" :total-items="auditTotal" />
    <PermissionAuditLog v-else :entries="permissionEntries" :loading="permissionLoading" :total-items="permissionTotal" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PermissionAuditLog from '@/components/admin/PermissionAuditLog.vue'
import apiClient from '@/api/client'

const tab = ref('audit')

const auditEntries = ref<
  { id: number; timestamp: string; action: string; username: string; entityType: string; entityId: number; details: string }[]
>([])
const auditLoading = ref(false)
const auditTotal = ref(0)

const permissionEntries = ref<
  { id: number; timestamp: string; action: string; username: string; entityType: string; entityId: number; details: string }[]
>([])
const permissionLoading = ref(false)
const permissionTotal = ref(0)

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const { data } = await apiClient.get('/api/audit-logs', { params: { page: 0, size: 50 } })
    const page = data.data
    auditEntries.value = page?.content || []
    auditTotal.value = page?.totalElements || 0
  } catch (e) {
    auditEntries.value = []
  } finally {
    auditLoading.value = false
  }
}

async function loadPermissionLogs() {
  permissionLoading.value = true
  try {
    const { data } = await apiClient.get('/api/audit-logs', { params: { page: 0, size: 50, action: 'GRANT,REVOKE' } })
    const page = data.data
    permissionEntries.value = page?.content || []
    permissionTotal.value = page?.totalElements || 0
  } catch (e) {
    permissionEntries.value = []
  } finally {
    permissionLoading.value = false
  }
}

onMounted(loadAuditLogs)
</script>

<style scoped>
.audit-history { padding: 24px; }
.audit-history h1 { color: #1a365d; margin-bottom: 16px; }
.tabs { margin-bottom: 20px; display: flex; gap: 8px; }
.tabs button { padding: 8px 20px; border: 1px solid #e2e8f0; background: white; border-radius: 6px; cursor: pointer; font-weight: 500; }
.tabs button.active { background: #3182ce; color: white; border-color: #3182ce; }
</style>
