<template>
  <div
    class="countdown-timer"
    :class="{
      'countdown-timer--warning': isWarning,
      'countdown-timer--critical': isCritical,
    }"
    role="timer"
    :aria-label="`Time remaining: ${displayTime}`"
  >
    <svg class="countdown-timer__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
    <span class="countdown-timer__display">{{ displayTime }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'

const props = defineProps<{
  timeRemainingSeconds: number
}>()

const emit = defineEmits<{
  expired: []
  tick: [remaining: number]
}>()

const remaining = ref(props.timeRemainingSeconds)
let intervalId: ReturnType<typeof setInterval> | null = null

watch(() => props.timeRemainingSeconds, (val) => {
  remaining.value = val
}, { immediate: true })

function startTimer() {
  stopTimer()
  intervalId = setInterval(() => {
    if (remaining.value <= 0) {
      stopTimer()
      emit('expired')
      return
    }
    remaining.value--
    emit('tick', remaining.value)
  }, 1000)
}

function stopTimer() {
  if (intervalId) {
    clearInterval(intervalId)
    intervalId = null
  }
}

startTimer()

onUnmounted(() => {
  stopTimer()
})

const displayTime = computed(() => {
  const total = Math.max(0, remaining.value)
  const minutes = Math.floor(total / 60)
  const seconds = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const isWarning = computed(() => remaining.value <= 300 && remaining.value > 60)
const isCritical = computed(() => remaining.value <= 60)
</script>

<style scoped>
.countdown-timer {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #334155;
  font-weight: 600;
  font-size: 1rem;
  font-variant-numeric: tabular-nums;
  transition: background 0.3s, color 0.3s;
}

.countdown-timer--warning {
  background: #fef9c3;
  color: #b45309;
}

.countdown-timer--critical {
  background: #fee2e2;
  color: #dc2626;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.countdown-timer__icon {
  flex-shrink: 0;
}

.countdown-timer__display {
  letter-spacing: 1px;
}
</style>
