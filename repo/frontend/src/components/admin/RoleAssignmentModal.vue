<template>
  <Teleport to="body">
    <div v-if="visible" class="role-modal-overlay" @click.self="emit('close')">
      <div class="role-modal" role="dialog" aria-modal="true">
        <header class="role-modal__header">
          <h3 class="role-modal__title">Manage Roles for {{ user?.fullName }}</h3>
          <button type="button" class="role-modal__close" @click="emit('close')">&times;</button>
        </header>

        <div class="role-modal__body">
          <p class="role-modal__subtitle">Username: <strong>{{ user?.username }}</strong></p>

          <div class="role-modal__roles">
            <label
              v-for="role in availableRoles"
              :key="role.id"
              class="role-modal__role-item"
            >
              <input
                type="checkbox"
                :value="role.id"
                :checked="selectedRoleIds.includes(role.id)"
                @change="toggleRole(role.id)"
                class="role-modal__checkbox"
              />
              <div class="role-modal__role-info">
                <span class="role-modal__role-name">{{ role.name.replace(/_/g, ' ') }}</span>
                <span class="role-modal__role-desc">{{ role.description }}</span>
              </div>
            </label>
          </div>
        </div>

        <footer class="role-modal__footer">
          <button type="button" class="role-modal__btn role-modal__btn--cancel" @click="emit('close')">Cancel</button>
          <button type="button" class="role-modal__btn role-modal__btn--save" @click="handleSave" :disabled="saving">
            {{ saving ? 'Saving...' : 'Save Roles' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { User, Role } from '@/types/user'

const props = defineProps<{
  visible: boolean
  user: User | null
  availableRoles: Role[]
  saving: boolean
}>()

const emit = defineEmits<{
  close: []
  save: [roleIds: number[]]
}>()

const selectedRoleIds = ref<number[]>([])

watch(() => props.user, (newUser) => {
  if (newUser) {
    selectedRoleIds.value = newUser.roles.map(r => r.id)
  }
}, { immediate: true })

function toggleRole(roleId: number) {
  const idx = selectedRoleIds.value.indexOf(roleId)
  if (idx >= 0) {
    selectedRoleIds.value.splice(idx, 1)
  } else {
    selectedRoleIds.value.push(roleId)
  }
}

function handleSave() {
  emit('save', [...selectedRoleIds.value])
}
</script>

<style scoped>
.role-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.role-modal {
  background: #fff;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.role-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.role-modal__title {
  font-size: 1.05rem;
  font-weight: 600;
  color: #1e293b;
}

.role-modal__close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #64748b;
  cursor: pointer;
}

.role-modal__body {
  padding: 20px 24px;
}

.role-modal__subtitle {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 16px;
}

.role-modal__roles {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.role-modal__role-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.role-modal__role-item:hover {
  background: #f8fafc;
}

.role-modal__checkbox {
  margin-top: 2px;
  accent-color: #3b82f6;
}

.role-modal__role-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.role-modal__role-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
  text-transform: capitalize;
}

.role-modal__role-desc {
  font-size: 0.78rem;
  color: #94a3b8;
}

.role-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.role-modal__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.role-modal__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.role-modal__btn--save {
  background: #3b82f6;
  color: #fff;
}

.role-modal__btn--save:hover {
  background: #2563eb;
}

.role-modal__btn--save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
