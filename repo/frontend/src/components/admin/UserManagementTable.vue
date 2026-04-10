<template>
  <div class="user-mgmt-table">
    <div class="user-mgmt-table__header">
      <h3 class="user-mgmt-table__title">Users</h3>
      <SearchFilterBar
        v-model:searchQuery="searchQuery"
        :placeholder="'Search users by name or email...'"
        :filters="statusFilter"
        v-model:filterValues="filterValues"
      >
        <template #actions>
          <button type="button" class="user-mgmt-table__add-btn" @click="emit('create')">
            + Add User
          </button>
        </template>
      </SearchFilterBar>
    </div>

    <LoadingSpinner v-if="loading" message="Loading users..." />

    <div v-else-if="filteredUsers.length === 0">
      <EmptyState title="No users found" description="No users match your current filters." />
    </div>

    <table v-else class="user-mgmt-table__table">
      <thead>
        <tr>
          <th>Username</th>
          <th>Email</th>
          <th>Full Name</th>
          <th>Roles</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in filteredUsers" :key="user.id">
          <td class="user-mgmt-table__username">{{ user.username }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.fullName }}</td>
          <td>
            <span
              v-for="role in user.roles"
              :key="role.id"
              class="user-mgmt-table__role-chip"
            >
              {{ role.name.replace(/_/g, ' ') }}
            </span>
          </td>
          <td>
            <span
              class="user-mgmt-table__status"
              :class="{
                'user-mgmt-table__status--active': user.enabled && !user.accountLocked,
                'user-mgmt-table__status--locked': user.accountLocked,
                'user-mgmt-table__status--disabled': !user.enabled,
              }"
            >
              {{ user.accountLocked ? 'Locked' : user.enabled ? 'Active' : 'Disabled' }}
            </span>
          </td>
          <td class="user-mgmt-table__actions">
            <button type="button" class="user-mgmt-table__action-btn" @click="emit('editRoles', user)">Roles</button>
            <button type="button" class="user-mgmt-table__action-btn" @click="emit('resetPassword', user)">Reset Pwd</button>
            <button
              type="button"
              class="user-mgmt-table__action-btn user-mgmt-table__action-btn--danger"
              @click="emit('toggleStatus', user)"
            >
              {{ user.enabled ? 'Disable' : 'Enable' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <PaginationBar
      v-if="filteredUsers.length > 0"
      v-model:currentPage="currentPage"
      v-model:pageSize="pageSize"
      :totalItems="totalItems"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { User } from '@/types/user'
import SearchFilterBar from '@/components/common/SearchFilterBar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const props = defineProps<{
  users: User[]
  loading: boolean
  totalItems: number
}>()

const emit = defineEmits<{
  create: []
  editRoles: [user: User]
  resetPassword: [user: User]
  toggleStatus: [user: User]
}>()

const searchQuery = ref('')
const filterValues = ref<Record<string, string>>({})
const currentPage = ref(1)
const pageSize = ref(25)

const statusFilter = [
  {
    key: 'status',
    label: 'Status',
    options: [
      { value: 'active', label: 'Active' },
      { value: 'disabled', label: 'Disabled' },
      { value: 'locked', label: 'Locked' },
    ],
  },
]

const filteredUsers = computed(() => {
  let result = props.users
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(
      u => u.username.toLowerCase().includes(q) ||
           u.email.toLowerCase().includes(q) ||
           u.fullName.toLowerCase().includes(q)
    )
  }
  const status = filterValues.value.status
  if (status === 'active') result = result.filter(u => u.enabled && !u.accountLocked)
  else if (status === 'disabled') result = result.filter(u => !u.enabled)
  else if (status === 'locked') result = result.filter(u => u.accountLocked)
  return result
})
</script>

<style scoped>
.user-mgmt-table__header {
  margin-bottom: 16px;
}

.user-mgmt-table__title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.user-mgmt-table__add-btn {
  padding: 8px 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
}

.user-mgmt-table__add-btn:hover {
  background: #2563eb;
}

.user-mgmt-table__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.user-mgmt-table__table th {
  text-align: left;
  padding: 12px 16px;
  background: #f8fafc;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.user-mgmt-table__table td {
  padding: 10px 16px;
  font-size: 0.875rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.user-mgmt-table__username {
  font-weight: 600;
}

.user-mgmt-table__role-chip {
  display: inline-block;
  padding: 2px 8px;
  margin: 1px 3px;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: capitalize;
}

.user-mgmt-table__status {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
}

.user-mgmt-table__status--active {
  background: #dcfce7;
  color: #16a34a;
}

.user-mgmt-table__status--disabled {
  background: #f1f5f9;
  color: #64748b;
}

.user-mgmt-table__status--locked {
  background: #fee2e2;
  color: #dc2626;
}

.user-mgmt-table__actions {
  display: flex;
  gap: 6px;
}

.user-mgmt-table__action-btn {
  padding: 4px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.75rem;
  cursor: pointer;
  color: #374151;
  transition: background 0.15s;
}

.user-mgmt-table__action-btn:hover {
  background: #f1f5f9;
}

.user-mgmt-table__action-btn--danger {
  color: #dc2626;
  border-color: #fca5a5;
}

.user-mgmt-table__action-btn--danger:hover {
  background: #fef2f2;
}
</style>
