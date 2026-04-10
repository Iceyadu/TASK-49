<template>
  <div class="audit-log">
    <SearchFilterBar
      v-model:searchQuery="searchQuery"
      placeholder="Search audit logs..."
      :filters="auditFilters"
      v-model:filterValues="filterValues"
    />

    <LoadingSpinner v-if="loading" message="Loading audit logs..." />

    <table v-else-if="filteredEntries.length > 0" class="audit-log__table">
      <thead>
        <tr>
          <th>Timestamp</th>
          <th>Action</th>
          <th>User</th>
          <th>Entity</th>
          <th>Details</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in filteredEntries" :key="entry.id">
          <td class="audit-log__timestamp">{{ formatDate(entry.timestamp) }}</td>
          <td>
            <span class="audit-log__action-badge" :class="actionClass(entry.action)">
              {{ entry.action }}
            </span>
          </td>
          <td>{{ entry.username }}</td>
          <td>{{ entry.entityType }} #{{ entry.entityId }}</td>
          <td class="audit-log__details">{{ entry.details }}</td>
        </tr>
      </tbody>
    </table>

    <EmptyState v-else title="No audit logs found" description="No log entries match the current filters." />

    <PaginationBar
      v-if="filteredEntries.length > 0"
      v-model:currentPage="currentPage"
      v-model:pageSize="pageSize"
      :totalItems="totalItems"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SearchFilterBar from '@/components/common/SearchFilterBar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

interface AuditEntry {
  id: number
  timestamp: string
  action: string
  username: string
  entityType: string
  entityId: number
  details: string
}

const props = defineProps<{
  entries: AuditEntry[]
  loading: boolean
  totalItems: number
}>()

const searchQuery = ref('')
const filterValues = ref<Record<string, string>>({})
const currentPage = ref(1)
const pageSize = ref(25)

const auditFilters = [
  {
    key: 'action',
    label: 'Action',
    options: [
      { value: 'CREATE', label: 'Create' },
      { value: 'UPDATE', label: 'Update' },
      { value: 'DELETE', label: 'Delete' },
      { value: 'ASSIGN_ROLE', label: 'Assign Role' },
      { value: 'REVOKE_ROLE', label: 'Revoke Role' },
      { value: 'PASSWORD_RESET', label: 'Password Reset' },
    ],
  },
]

const filteredEntries = computed(() => {
  let result = props.entries
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(
      e => e.username.toLowerCase().includes(q) ||
           e.details.toLowerCase().includes(q) ||
           e.entityType.toLowerCase().includes(q)
    )
  }
  if (filterValues.value.action) {
    result = result.filter(e => e.action === filterValues.value.action)
  }
  return result
})

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString()
}

function actionClass(action: string): string {
  const map: Record<string, string> = {
    CREATE: 'audit-log__action-badge--create',
    UPDATE: 'audit-log__action-badge--update',
    DELETE: 'audit-log__action-badge--delete',
    ASSIGN_ROLE: 'audit-log__action-badge--assign',
    REVOKE_ROLE: 'audit-log__action-badge--revoke',
    PASSWORD_RESET: 'audit-log__action-badge--reset',
  }
  return map[action] || ''
}
</script>

<style scoped>
.audit-log__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.audit-log__table th {
  text-align: left;
  padding: 12px 16px;
  background: #f8fafc;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.audit-log__table td {
  padding: 10px 16px;
  font-size: 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.audit-log__timestamp {
  white-space: nowrap;
  font-size: 0.8rem;
  color: #64748b;
}

.audit-log__action-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  background: #e2e8f0;
  color: #475569;
}

.audit-log__action-badge--create { background: #dcfce7; color: #16a34a; }
.audit-log__action-badge--update { background: #dbeafe; color: #2563eb; }
.audit-log__action-badge--delete { background: #fee2e2; color: #dc2626; }
.audit-log__action-badge--assign { background: #f3e8ff; color: #9333ea; }
.audit-log__action-badge--revoke { background: #fef9c3; color: #ca8a04; }
.audit-log__action-badge--reset { background: #ffedd5; color: #ea580c; }

.audit-log__details {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
