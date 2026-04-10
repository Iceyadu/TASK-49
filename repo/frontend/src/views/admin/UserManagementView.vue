<template>
  <div class="user-management">
    <div class="page-header">
      <h1>User Management</h1>
      <button type="button" @click="showCreateModal = true" class="btn-primary">Create User</button>
    </div>
    <UserManagementTable
      :users="users"
      :loading="loading"
      :total-items="totalElements"
      @create="showCreateModal = true"
      @edit-roles="openRoleAssignment"
      @reset-password="openPasswordReset"
      @toggle-status="toggleUserStatus"
    />
    <RoleAssignmentModal
      :visible="showRoleModal"
      :user="selectedUser"
      :available-roles="availableRoles"
      :saving="roleSaving"
      @close="showRoleModal = false"
      @save="onSaveRoles"
    />
    <AdminPasswordReset
      v-if="showPasswordReset && selectedUser"
      :username="selectedUser.username"
      :user-id="selectedUser.id"
      @cancel="showPasswordReset = false"
      @submit="onAdminPasswordSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UserManagementTable from '@/components/admin/UserManagementTable.vue'
import RoleAssignmentModal from '@/components/admin/RoleAssignmentModal.vue'
import AdminPasswordReset from '@/components/admin/AdminPasswordReset.vue'
import { adminResetPassword, getUsers, updateUser } from '@/api/users'
import type { User, Role } from '@/types/user'

const users = ref<User[]>([])
const loading = ref(false)
const totalElements = ref(0)
const showCreateModal = ref(false)
const showRoleModal = ref(false)
const showPasswordReset = ref(false)
const selectedUser = ref<User | null>(null)
const roleSaving = ref(false)

const availableRoles = ref<Role[]>([
  { id: 1, name: 'ADMINISTRATOR', description: 'Full system access' },
  { id: 2, name: 'INSTRUCTOR', description: 'Course and quiz management' },
  { id: 3, name: 'CURATOR', description: 'Content curation' },
  { id: 4, name: 'TEACHING_ASSISTANT', description: 'Grading assistance' },
  { id: 5, name: 'STUDENT', description: 'Learner access' },
])

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsers(0, 20)
    users.value = page.content
    totalElements.value = page.totalElements
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openRoleAssignment(user: User) {
  selectedUser.value = user
  showRoleModal.value = true
}

function openPasswordReset(user: User) {
  selectedUser.value = user
  showPasswordReset.value = true
}

async function toggleUserStatus(user: User) {
  try {
    await updateUser(user.id, { enabled: !user.enabled })
    await loadUsers()
  } catch (e) {
    console.error(e)
  }
}

async function onSaveRoles(roleIds: number[]) {
  if (!selectedUser.value) return
  roleSaving.value = true
  try {
    const roles = availableRoles.value.filter(r => roleIds.includes(r.id))
    await updateUser(selectedUser.value.id, { roles } as Partial<User>)
    showRoleModal.value = false
    await loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    roleSaving.value = false
  }
}

async function onAdminPasswordSubmit(payload: {
  mode: 'standard' | 'admin'
  oldPassword?: string
  newPassword: string
  workstationId?: string
  reason?: string
}) {
  if (!selectedUser.value) return
  try {
    if (payload.mode === 'admin') {
      await adminResetPassword(selectedUser.value.id, {
        newPassword: payload.newPassword,
        workstationId: payload.workstationId || '',
        reason: payload.reason,
      })
    }
    showPasswordReset.value = false
    await loadUsers()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.user-management { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { color: #1a365d; }
.btn-primary { padding: 10px 20px; background: #3182ce; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
</style>
