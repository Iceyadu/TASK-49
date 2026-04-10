<template>
  <form class="rubric-form" @submit.prevent="handleSubmit">
    <h4 class="rubric-form__title">Rubric Scoring</h4>

    <div class="rubric-form__criteria">
      <div
        v-for="(criterion, idx) in criteria"
        :key="idx"
        class="rubric-form__criterion"
      >
        <div class="rubric-form__criterion-header">
          <span class="rubric-form__criterion-name">{{ criterion.criterionName }}</span>
          <span class="rubric-form__criterion-max">Max: {{ criterion.maxScore }}</span>
        </div>

        <div class="rubric-form__criterion-inputs">
          <div class="rubric-form__score-field">
            <label class="rubric-form__label">Score</label>
            <input
              v-model.number="criterion.awardedScore"
              type="number"
              class="rubric-form__score-input"
              :min="0"
              :max="criterion.maxScore"
              required
            />
          </div>
          <div class="rubric-form__comment-field">
            <label class="rubric-form__label">Comment <span class="rubric-form__required">*</span></label>
            <textarea
              v-model="criterion.comment"
              class="rubric-form__comment-input"
              rows="2"
              required
              placeholder="Provide feedback for this criterion..."
            ></textarea>
          </div>
        </div>

        <p v-if="validationErrors[idx]" class="rubric-form__error">{{ validationErrors[idx] }}</p>
      </div>
    </div>

    <div class="rubric-form__total">
      <span class="rubric-form__total-label">Total Score:</span>
      <span class="rubric-form__total-value">{{ totalAwarded }} / {{ totalMax }}</span>
    </div>

    <div class="rubric-form__actions">
      <button type="button" class="rubric-form__btn rubric-form__btn--cancel" @click="emit('cancel')">Cancel</button>
      <button type="submit" class="rubric-form__btn rubric-form__btn--submit" :disabled="!isValid || saving">
        {{ saving ? 'Saving...' : 'Submit Scores' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { RubricScore } from '@/types/grading'

const props = defineProps<{
  initialCriteria: RubricScore[]
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [scores: RubricScore[]]
}>()

const criteria = ref<RubricScore[]>([])

watch(() => props.initialCriteria, (val) => {
  criteria.value = val.map(c => ({ ...c }))
}, { immediate: true, deep: true })

const totalAwarded = computed(() => criteria.value.reduce((s, c) => s + (c.awardedScore || 0), 0))
const totalMax = computed(() => criteria.value.reduce((s, c) => s + c.maxScore, 0))

const validationErrors = computed(() => {
  return criteria.value.map((c, idx) => {
    if (c.awardedScore < 0) return 'Score cannot be negative.'
    if (c.awardedScore > c.maxScore) return `Score exceeds maximum of ${c.maxScore}.`
    if (!c.comment || !c.comment.trim()) return 'Comment is required.'
    return ''
  })
})

const isValid = computed(() => validationErrors.value.every(e => e === ''))

function handleSubmit() {
  if (!isValid.value) return
  emit('submit', criteria.value.map(c => ({ ...c })))
}
</script>

<style scoped>
.rubric-form {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.rubric-form__title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}

.rubric-form__criteria {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.rubric-form__criterion {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fafafa;
}

.rubric-form__criterion-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.rubric-form__criterion-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e293b;
}

.rubric-form__criterion-max {
  font-size: 0.78rem;
  color: #64748b;
}

.rubric-form__criterion-inputs {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.rubric-form__score-field {
  flex-shrink: 0;
  width: 80px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rubric-form__comment-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rubric-form__label {
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
}

.rubric-form__required {
  color: #dc2626;
}

.rubric-form__score-input {
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.875rem;
  text-align: center;
  outline: none;
}

.rubric-form__score-input:focus {
  border-color: #3b82f6;
}

.rubric-form__comment-input {
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.82rem;
  resize: vertical;
  outline: none;
}

.rubric-form__comment-input:focus {
  border-color: #3b82f6;
}

.rubric-form__error {
  color: #dc2626;
  font-size: 0.75rem;
  margin-top: 6px;
}

.rubric-form__total {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 6px;
  margin-bottom: 16px;
}

.rubric-form__total-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.rubric-form__total-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
}

.rubric-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.rubric-form__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.rubric-form__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.rubric-form__btn--submit {
  background: #16a34a;
  color: #fff;
}

.rubric-form__btn--submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
