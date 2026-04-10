<template>
  <div class="quiz-assembler">
    <h3 class="quiz-assembler__title">Quiz Assembly Rules</h3>

    <form class="quiz-assembler__form" @submit.prevent="handleAssemble">
      <div class="quiz-assembler__field">
        <label class="quiz-assembler__label" for="totalQuestions">Total Questions</label>
        <input
          id="totalQuestions"
          v-model.number="form.totalQuestions"
          type="number"
          class="quiz-assembler__input"
          min="1"
          max="200"
          required
        />
      </div>

      <fieldset class="quiz-assembler__fieldset">
        <legend class="quiz-assembler__legend">Difficulty Constraints</legend>

        <div
          v-for="constraint in form.constraints"
          :key="constraint.difficultyLevel"
          class="quiz-assembler__constraint-row"
        >
          <span class="quiz-assembler__constraint-label">
            Difficulty {{ constraint.difficultyLevel }}
            <span class="quiz-assembler__constraint-stars">
              <span v-for="n in constraint.difficultyLevel" :key="n" class="quiz-assembler__star-filled">&#9733;</span>
            </span>
          </span>
          <div class="quiz-assembler__constraint-inputs">
            <label class="quiz-assembler__mini-label">
              Min:
              <input
                v-model.number="constraint.minCount"
                type="number"
                class="quiz-assembler__mini-input"
                min="0"
              />
            </label>
            <label class="quiz-assembler__mini-label">
              Max:
              <input
                v-model.number="constraint.maxCount"
                type="number"
                class="quiz-assembler__mini-input"
                min="0"
              />
            </label>
          </div>
        </div>
      </fieldset>

      <div class="quiz-assembler__summary">
        <p class="quiz-assembler__summary-text">
          Minimum required: <strong>{{ totalMin }}</strong> |
          Maximum allowed: <strong>{{ totalMax }}</strong> |
          Target: <strong>{{ form.totalQuestions }}</strong>
        </p>
        <p v-if="validationError" class="quiz-assembler__validation-error">{{ validationError }}</p>
      </div>

      <div class="quiz-assembler__actions">
        <button
          type="button"
          class="quiz-assembler__btn quiz-assembler__btn--preview"
          @click="emit('preview', buildRules())"
        >
          Preview Selection
        </button>
        <button
          type="submit"
          class="quiz-assembler__btn quiz-assembler__btn--assemble"
          :disabled="!!validationError || assembling"
        >
          {{ assembling ? 'Assembling...' : 'Assemble Quiz' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue'
import type { QuizRule } from '@/types/quiz'

const props = defineProps<{
  assembling: boolean
}>()

const emit = defineEmits<{
  preview: [rules: QuizRule[]]
  assemble: [data: { totalQuestions: number; rules: QuizRule[] }]
}>()

const form = reactive({
  totalQuestions: 20,
  constraints: [
    { difficultyLevel: 1, minCount: 0, maxCount: 0 },
    { difficultyLevel: 2, minCount: 2, maxCount: 5 },
    { difficultyLevel: 3, minCount: 6, maxCount: 10 },
    { difficultyLevel: 4, minCount: 3, maxCount: 6 },
    { difficultyLevel: 5, minCount: 2, maxCount: 4 },
  ],
})

const totalMin = computed(() => form.constraints.reduce((s, c) => s + (c.minCount || 0), 0))
const totalMax = computed(() => form.constraints.reduce((s, c) => s + (c.maxCount || 0), 0))

const validationError = computed(() => {
  if (totalMin.value > form.totalQuestions) {
    return `Minimum constraints (${totalMin.value}) exceed total questions (${form.totalQuestions}).`
  }
  if (totalMax.value < form.totalQuestions) {
    return `Maximum constraints (${totalMax.value}) are less than total questions (${form.totalQuestions}).`
  }
  return ''
})

function buildRules(): QuizRule[] {
  return form.constraints
    .filter(c => c.minCount > 0 || c.maxCount > 0)
    .map(c => ({
      ruleType: 'DIFFICULTY',
      difficultyLevel: c.difficultyLevel,
      minCount: c.minCount,
      maxCount: c.maxCount,
    }))
}

function handleAssemble() {
  if (validationError.value) return
  emit('assemble', { totalQuestions: form.totalQuestions, rules: buildRules() })
}
</script>

<style scoped>
.quiz-assembler {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 560px;
}

.quiz-assembler__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.quiz-assembler__field {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quiz-assembler__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.quiz-assembler__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  max-width: 140px;
  outline: none;
}

.quiz-assembler__input:focus {
  border-color: #3b82f6;
}

.quiz-assembler__fieldset {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.quiz-assembler__legend {
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
  padding: 0 6px;
}

.quiz-assembler__constraint-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.quiz-assembler__constraint-row:last-child {
  border-bottom: none;
}

.quiz-assembler__constraint-label {
  font-size: 0.85rem;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}

.quiz-assembler__constraint-stars {
  font-size: 0.8rem;
}

.quiz-assembler__star-filled {
  color: #f59e0b;
}

.quiz-assembler__constraint-inputs {
  display: flex;
  gap: 12px;
}

.quiz-assembler__mini-label {
  font-size: 0.78rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.quiz-assembler__mini-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.82rem;
  outline: none;
}

.quiz-assembler__summary {
  margin-bottom: 16px;
}

.quiz-assembler__summary-text {
  font-size: 0.85rem;
  color: #475569;
}

.quiz-assembler__validation-error {
  font-size: 0.82rem;
  color: #dc2626;
  margin-top: 6px;
}

.quiz-assembler__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.quiz-assembler__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.quiz-assembler__btn--preview {
  background: #e2e8f0;
  color: #475569;
}

.quiz-assembler__btn--assemble {
  background: #3b82f6;
  color: #fff;
}

.quiz-assembler__btn--assemble:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
