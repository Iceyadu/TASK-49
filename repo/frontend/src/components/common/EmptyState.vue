<template>
  <div class="empty-state">
    <div class="empty-state__icon" v-html="icon || defaultIcon"></div>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
}>()

const defaultIcon = '<svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.5"><path d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/></svg>'
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.empty-state__icon {
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.empty-state__description {
  font-size: 0.9rem;
  color: #94a3b8;
  max-width: 360px;
  line-height: 1.5;
}
</style>
