<template>
  <form class="quiz-schedule" @submit.prevent="handleSubmit">
    <h3 class="quiz-schedule__title">Quiz Schedule Settings</h3>

    <div class="quiz-schedule__row">
      <div class="quiz-schedule__field">
        <label class="quiz-schedule__label" for="releaseStart">Release Start</label>
        <input
          id="releaseStart"
          v-model="form.releaseStart"
          type="datetime-local"
          class="quiz-schedule__input"
          required
        />
      </div>
      <div class="quiz-schedule__field">
        <label class="quiz-schedule__label" for="releaseEnd">Release End</label>
        <input
          id="releaseEnd"
          v-model="form.releaseEnd"
          type="datetime-local"
          class="quiz-schedule__input"
          required
        />
      </div>
    </div>

    <div class="quiz-schedule__row">
      <div class="quiz-schedule__field">
        <label class="quiz-schedule__label" for="timeLimit">Time Limit (minutes)</label>
        <input
          id="timeLimit"
          v-model.number="form.timeLimitMinutes"
          type="number"
          class="quiz-schedule__input"
          min="1"
          max="600"
          required
        />
      </div>
      <div class="quiz-schedule__field">
        <label class="quiz-schedule__label" for="maxAttempts">Max Attempts</label>
        <input
          id="maxAttempts"
          v-model.number="form.maxAttempts"
          type="number"
          class="quiz-schedule__input"
          min="1"
          max="10"
          required
        />
      </div>
    </div>

    <div class="quiz-schedule__info">
      <p v-if="form.releaseStart && form.releaseEnd" class="quiz-schedule__duration">
        Window duration: <strong>{{ windowDuration }}</strong>
      </p>
    </div>

    <p v-if="validationError" class="quiz-schedule__error">{{ validationError }}</p>

    <div class="quiz-schedule__actions">
      <button type="button" class="quiz-schedule__btn quiz-schedule__btn--cancel" @click="emit('cancel')">Cancel</button>
      <button type="submit" class="quiz-schedule__btn quiz-schedule__btn--save" :disabled="!!validationError || saving">
        {{ saving ? 'Saving...' : 'Save Schedule' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'

const props = defineProps<{
  initialData?: {
    releaseStart?: string
    releaseEnd?: string
    timeLimitMinutes?: number
    maxAttempts?: number
  } | null
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [data: { releaseStart: string; releaseEnd: string; timeLimitMinutes: number; maxAttempts: number }]
}>()

const form = reactive({
  releaseStart: '',
  releaseEnd: '',
  timeLimitMinutes: 60,
  maxAttempts: 1,
})

watch(() => props.initialData, (data) => {
  if (data) {
    form.releaseStart = data.releaseStart?.slice(0, 16) || ''
    form.releaseEnd = data.releaseEnd?.slice(0, 16) || ''
    form.timeLimitMinutes = data.timeLimitMinutes || 60
    form.maxAttempts = data.maxAttempts || 1
  }
}, { immediate: true })

const validationError = computed(() => {
  if (form.releaseStart && form.releaseEnd) {
    if (new Date(form.releaseEnd) <= new Date(form.releaseStart)) {
      return 'Release end must be after release start.'
    }
  }
  return ''
})

const windowDuration = computed(() => {
  if (!form.releaseStart || !form.releaseEnd) return '--'
  const diff = new Date(form.releaseEnd).getTime() - new Date(form.releaseStart).getTime()
  if (diff <= 0) return 'Invalid'
  const hours = Math.floor(diff / 3600000)
  const mins = Math.floor((diff % 3600000) / 60000)
  return `${hours}h ${mins}m`
})

function handleSubmit() {
  if (validationError.value) return
  emit('submit', { ...form })
}
</script>

<style scoped>
.quiz-schedule {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 520px;
}

.quiz-schedule__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.quiz-schedule__row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.quiz-schedule__field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quiz-schedule__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.quiz-schedule__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
}

.quiz-schedule__input:focus {
  border-color: #3b82f6;
}

.quiz-schedule__info {
  margin-bottom: 12px;
}

.quiz-schedule__duration {
  font-size: 0.85rem;
  color: #475569;
}

.quiz-schedule__error {
  font-size: 0.82rem;
  color: #dc2626;
  margin-bottom: 12px;
}

.quiz-schedule__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.quiz-schedule__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.quiz-schedule__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.quiz-schedule__btn--save {
  background: #3b82f6;
  color: #fff;
}

.quiz-schedule__btn--save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
