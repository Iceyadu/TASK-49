<template>
  <div class="admin-pwd-reset">
    <h3 class="admin-pwd-reset__title">Reset Password for {{ username }}</h3>

    <div class="admin-pwd-reset__mode-toggle">
      <label class="admin-pwd-reset__mode-option">
        <input
          type="radio"
          value="standard"
          v-model="resetMode"
          class="admin-pwd-reset__radio"
        />
        Standard Reset (requires old password)
      </label>
      <label class="admin-pwd-reset__mode-option">
        <input
          type="radio"
          value="admin"
          v-model="resetMode"
          class="admin-pwd-reset__radio"
        />
        Admin Override (workstation verification)
      </label>
    </div>

    <form class="admin-pwd-reset__form" @submit.prevent="handleSubmit">
      <div v-if="resetMode === 'standard'" class="admin-pwd-reset__field">
        <label class="admin-pwd-reset__label" for="oldPassword">Old Password</label>
        <input
          id="oldPassword"
          v-model="oldPassword"
          type="password"
          class="admin-pwd-reset__input"
          required
          autocomplete="current-password"
        />
      </div>

      <div v-if="resetMode === 'admin'" class="admin-pwd-reset__field">
        <label class="admin-pwd-reset__label" for="workstationId">Workstation ID</label>
        <input
          id="workstationId"
          v-model="workstationId"
          type="text"
          class="admin-pwd-reset__input"
          placeholder="e.g. WS-LAB-042"
          required
        />
      </div>

      <div v-if="resetMode === 'admin'" class="admin-pwd-reset__field">
        <label class="admin-pwd-reset__label" for="reason">Reason (optional)</label>
        <textarea
          id="reason"
          v-model="reason"
          class="admin-pwd-reset__textarea"
          rows="2"
          placeholder="Reason for admin override..."
        ></textarea>
      </div>

      <div class="admin-pwd-reset__field">
        <label class="admin-pwd-reset__label" for="newPassword">New Password</label>
        <input
          id="newPassword"
          v-model="newPassword"
          type="password"
          class="admin-pwd-reset__input"
          required
          minlength="8"
          autocomplete="new-password"
        />
      </div>

      <div class="admin-pwd-reset__field">
        <label class="admin-pwd-reset__label" for="confirmPassword">Confirm New Password</label>
        <input
          id="confirmPassword"
          v-model="confirmPassword"
          type="password"
          class="admin-pwd-reset__input"
          required
          autocomplete="new-password"
        />
      </div>

      <p v-if="passwordMismatch" class="admin-pwd-reset__error">Passwords do not match.</p>
      <p v-if="errorMessage" class="admin-pwd-reset__error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="admin-pwd-reset__success">{{ successMessage }}</p>

      <div class="admin-pwd-reset__actions">
        <button type="button" class="admin-pwd-reset__btn admin-pwd-reset__btn--cancel" @click="emit('cancel')">Cancel</button>
        <button type="submit" class="admin-pwd-reset__btn admin-pwd-reset__btn--submit" :disabled="passwordMismatch || submitting">
          {{ submitting ? 'Resetting...' : 'Reset Password' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  username: string
  userId: number
}>()

const emit = defineEmits<{
  cancel: []
  submit: [payload: {
    mode: 'standard' | 'admin'
    oldPassword?: string
    newPassword: string
    workstationId?: string
    reason?: string
  }]
}>()

const resetMode = ref<'standard' | 'admin'>('admin')
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const workstationId = ref('')
const reason = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const passwordMismatch = computed(() => {
  return confirmPassword.value.length > 0 && newPassword.value !== confirmPassword.value
})

function handleSubmit() {
  errorMessage.value = ''
  successMessage.value = ''

  if (passwordMismatch.value) return

  if (resetMode.value === 'admin' && !workstationId.value.trim()) {
    errorMessage.value = 'Workstation ID is required for admin override.'
    return
  }

  if (resetMode.value === 'standard' && !oldPassword.value) {
    errorMessage.value = 'Old password is required.'
    return
  }

  submitting.value = true
  emit('submit', {
    mode: resetMode.value,
    oldPassword: resetMode.value === 'standard' ? oldPassword.value : undefined,
    newPassword: newPassword.value,
    workstationId: resetMode.value === 'admin' ? workstationId.value : undefined,
    reason: resetMode.value === 'admin' ? reason.value : undefined,
  })
}
</script>

<style scoped>
.admin-pwd-reset {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 480px;
}

.admin-pwd-reset__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.admin-pwd-reset__mode-toggle {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
}

.admin-pwd-reset__mode-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  color: #334155;
  cursor: pointer;
}

.admin-pwd-reset__radio {
  accent-color: #3b82f6;
}

.admin-pwd-reset__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.admin-pwd-reset__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-pwd-reset__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.admin-pwd-reset__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
}

.admin-pwd-reset__input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.admin-pwd-reset__textarea {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  resize: vertical;
  outline: none;
}

.admin-pwd-reset__textarea:focus {
  border-color: #3b82f6;
}

.admin-pwd-reset__error {
  color: #dc2626;
  font-size: 0.8rem;
}

.admin-pwd-reset__success {
  color: #16a34a;
  font-size: 0.8rem;
}

.admin-pwd-reset__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.admin-pwd-reset__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.admin-pwd-reset__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.admin-pwd-reset__btn--submit {
  background: #3b82f6;
  color: #fff;
}

.admin-pwd-reset__btn--submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
