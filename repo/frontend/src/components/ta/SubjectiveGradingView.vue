<template>
  <div class="subjective-grading">
    <div class="subjective-grading__layout">
      <section class="subjective-grading__answer-panel">
        <h4 class="subjective-grading__panel-title">Student Answer</h4>
        <div class="subjective-grading__question">
          <p class="subjective-grading__question-label">Question:</p>
          <p class="subjective-grading__question-text">{{ questionText }}</p>
        </div>
        <div class="subjective-grading__student-answer">
          <p class="subjective-grading__answer-label">Answer:</p>
          <div class="subjective-grading__answer-content">{{ answerText }}</div>
        </div>
      </section>

      <section class="subjective-grading__grading-panel">
        <RubricScoringForm
          :initialCriteria="rubricCriteria"
          :saving="saving"
          @submit="handleRubricSubmit"
          @cancel="emit('cancel')"
        />

        <div class="subjective-grading__feedback-section">
          <h4 class="subjective-grading__panel-title">Overall Feedback</h4>
          <textarea
            v-model="feedbackText"
            class="subjective-grading__feedback-textarea"
            rows="4"
            placeholder="Provide overall feedback to the student..."
          ></textarea>
        </div>

        <div class="subjective-grading__submit-section">
          <button
            type="button"
            class="subjective-grading__submit-btn"
            :disabled="!feedbackText.trim() || saving"
            @click="handleSubmitGrade"
          >
            {{ saving ? 'Submitting...' : 'Submit Grade' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { RubricScore } from '@/types/grading'
import RubricScoringForm from './RubricScoringForm.vue'

const props = defineProps<{
  questionText: string
  answerText: string
  rubricCriteria: RubricScore[]
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submitGrade: [data: { rubricScores: RubricScore[]; feedback: string }]
}>()

const feedbackText = ref('')
let latestRubricScores: RubricScore[] = []

function handleRubricSubmit(scores: RubricScore[]) {
  latestRubricScores = scores
}

function handleSubmitGrade() {
  if (!feedbackText.value.trim()) return
  emit('submitGrade', {
    rubricScores: latestRubricScores,
    feedback: feedbackText.value,
  })
}
</script>

<style scoped>
.subjective-grading__layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

@media (max-width: 900px) {
  .subjective-grading__layout {
    grid-template-columns: 1fr;
  }
}

.subjective-grading__answer-panel {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.subjective-grading__panel-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}

.subjective-grading__question {
  margin-bottom: 20px;
}

.subjective-grading__question-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 4px;
}

.subjective-grading__question-text {
  font-size: 0.95rem;
  color: #0f172a;
  line-height: 1.5;
}

.subjective-grading__answer-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #64748b;
  margin-bottom: 4px;
}

.subjective-grading__answer-content {
  font-size: 0.9rem;
  color: #334155;
  line-height: 1.6;
  padding: 14px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  white-space: pre-wrap;
  min-height: 150px;
}

.subjective-grading__grading-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.subjective-grading__feedback-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.subjective-grading__feedback-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  resize: vertical;
  outline: none;
  font-family: inherit;
}

.subjective-grading__feedback-textarea:focus {
  border-color: #3b82f6;
}

.subjective-grading__submit-section {
  display: flex;
  justify-content: flex-end;
}

.subjective-grading__submit-btn {
  padding: 10px 28px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.subjective-grading__submit-btn:hover {
  background: #15803d;
}

.subjective-grading__submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
