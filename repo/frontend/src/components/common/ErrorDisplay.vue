<template>
  <div class="error-display" role="alert">
    <div class="error-display__icon">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    </div>
    <h3 class="error-display__title">Something went wrong</h3>
    <p class="error-display__message">{{ message }}</p>
    <button
      v-if="onRetry"
      class="error-display__retry-btn"
      type="button"
      @click="onRetry"
    >
      Try Again
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message: string
  onRetry?: () => void
}>()
</script>

<style scoped>
.error-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px;
  text-align: center;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
}

.error-display__icon {
  margin-bottom: 12px;
}

.error-display__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #dc2626;
  margin-bottom: 8px;
}

.error-display__message {
  font-size: 0.9rem;
  color: #7f1d1d;
  max-width: 400px;
  line-height: 1.5;
  margin-bottom: 16px;
}

.error-display__retry-btn {
  padding: 8px 20px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.error-display__retry-btn:hover {
  background: #dc2626;
}
</style>
