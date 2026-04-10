<template>
  <div class="wrong-answers">
    <h3 class="wrong-answers__title">Wrong Answer Review</h3>

    <LoadingSpinner v-if="loading" message="Loading wrong answers..." />

    <EmptyState
      v-else-if="items.length === 0"
      title="No wrong answers"
      description="Great job! You have no wrong answers to review."
    />

    <div v-else class="wrong-answers__list">
      <article
        v-for="(item, idx) in items"
        :key="idx"
        class="wrong-answers__item"
      >
        <div class="wrong-answers__item-header">
          <span class="wrong-answers__item-number">Question {{ idx + 1 }}</span>
          <span class="wrong-answers__item-quiz">{{ item.quizTitle }}</span>
        </div>

        <p class="wrong-answers__question-text">{{ item.questionText }}</p>

        <div class="wrong-answers__answers">
          <div class="wrong-answers__answer wrong-answers__answer--student">
            <span class="wrong-answers__answer-label">Your Answer</span>
            <span class="wrong-answers__answer-text">{{ item.studentAnswer || '(No answer)' }}</span>
          </div>
          <div class="wrong-answers__answer wrong-answers__answer--correct">
            <span class="wrong-answers__answer-label">Correct Answer</span>
            <span class="wrong-answers__answer-text">{{ item.correctAnswer }}</span>
          </div>
        </div>

        <div v-if="item.explanation" class="wrong-answers__explanation">
          <span class="wrong-answers__explanation-label">Explanation</span>
          <p class="wrong-answers__explanation-text">{{ item.explanation }}</p>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

export interface WrongAnswerItem {
  questionText: string
  studentAnswer: string
  correctAnswer: string
  explanation: string
  quizTitle: string
}

defineProps<{
  items: WrongAnswerItem[]
  loading: boolean
}>()
</script>

<style scoped>
.wrong-answers__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.wrong-answers__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wrong-answers__item {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  padding: 20px;
}

.wrong-answers__item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.wrong-answers__item-number {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
}

.wrong-answers__item-quiz {
  font-size: 0.72rem;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 10px;
}

.wrong-answers__question-text {
  font-size: 0.95rem;
  color: #0f172a;
  line-height: 1.5;
  margin-bottom: 16px;
}

.wrong-answers__answers {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.wrong-answers__answer {
  padding: 10px 14px;
  border-radius: 6px;
}

.wrong-answers__answer--student {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.wrong-answers__answer--correct {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.wrong-answers__answer-label {
  display: block;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 4px;
}

.wrong-answers__answer--student .wrong-answers__answer-label {
  color: #dc2626;
}

.wrong-answers__answer--correct .wrong-answers__answer-label {
  color: #16a34a;
}

.wrong-answers__answer-text {
  font-size: 0.88rem;
  color: #334155;
}

.wrong-answers__explanation {
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 6px;
}

.wrong-answers__explanation-label {
  display: block;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  color: #64748b;
  margin-bottom: 4px;
}

.wrong-answers__explanation-text {
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.5;
}
</style>
