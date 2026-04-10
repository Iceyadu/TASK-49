<template>
  <div
    class="session-block"
    :style="{ backgroundColor: session.color || '#3b82f6' }"
    draggable="true"
    @dragstart="$emit('dragstart', $event)"
    @dragend="$emit('dragend', $event)"
    @click="$emit('click')"
  >
    <div class="session-block__drag-handle" title="Drag to move">&#8942;&#8942;</div>
    <div class="session-block__content">
      <span class="session-block__title">{{ session.title }}</span>
      <span class="session-block__time">{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Schedule } from '@/types/schedule'

const props = defineProps<{
  session: Schedule
}>()

defineEmits<{
  click: []
  dragstart: [event: DragEvent]
  dragend: [event: DragEvent]
}>()

function formatTime(time: string): string {
  if (!time) return ''
  const parts = time.split(':')
  const hour = parseInt(parts[0])
  const min = parts[1] || '00'
  const period = hour >= 12 ? 'PM' : 'AM'
  const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
  return `${displayHour}:${min} ${period}`
}
</script>

<style scoped>
.session-block {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  min-height: 24px;
  user-select: none;
  transition: opacity 0.15s;
}

.session-block:hover {
  opacity: 0.9;
}

.session-block__drag-handle {
  cursor: grab;
  font-size: 0.7rem;
  opacity: 0.7;
  line-height: 1;
  padding-top: 2px;
  flex-shrink: 0;
}

.session-block__drag-handle:active {
  cursor: grabbing;
}

.session-block__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.session-block__title {
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-block__time {
  font-size: 0.65rem;
  opacity: 0.85;
}
</style>
