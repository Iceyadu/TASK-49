<template>
  <Teleport to="body">
    <div v-if="visible" class="confirm-overlay" @click.self="handleCancel">
      <div class="confirm-dialog" role="dialog" aria-modal="true">
        <header class="confirm-dialog__header">
          <h3 class="confirm-dialog__title">{{ title }}</h3>
        </header>
        <div class="confirm-dialog__body">
          <p class="confirm-dialog__message">{{ message }}</p>
        </div>
        <footer class="confirm-dialog__footer">
          <button
            type="button"
            class="confirm-dialog__btn confirm-dialog__btn--cancel"
            @click="handleCancel"
          >
            {{ cancelText }}
          </button>
          <button
            type="button"
            class="confirm-dialog__btn confirm-dialog__btn--confirm"
            :class="{ 'confirm-dialog__btn--danger': destructive }"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  destructive?: boolean
}>(), {
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  destructive: false,
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

function handleConfirm() {
  emit('confirm')
}

function handleCancel() {
  emit('cancel')
}
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-dialog {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 440px;
  overflow: hidden;
}

.confirm-dialog__header {
  padding: 20px 24px 0;
}

.confirm-dialog__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.confirm-dialog__body {
  padding: 12px 24px 20px;
}

.confirm-dialog__message {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.5;
}

.confirm-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.confirm-dialog__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.confirm-dialog__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.confirm-dialog__btn--cancel:hover {
  background: #cbd5e1;
}

.confirm-dialog__btn--confirm {
  background: #3b82f6;
  color: #fff;
}

.confirm-dialog__btn--confirm:hover {
  background: #2563eb;
}

.confirm-dialog__btn--danger {
  background: #ef4444;
}

.confirm-dialog__btn--danger:hover {
  background: #dc2626;
}
</style>
