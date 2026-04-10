<template>
  <div class="assessment-view">
    <div class="assessment-view__header">
      <h2 class="assessment-view__title">{{ quizTitle }}</h2>
      <div class="assessment-view__header-right">
        <CountdownTimer
          :timeRemainingSeconds="timeRemaining"
          @expired="handleTimeExpired"
          @tick="onTimerTick"
        />
        <span class="assessment-view__autosave" :class="{ 'assessment-view__autosave--saving': isSaving }">
          {{ isSaving ? 'Saving...' : lastSaved ? `Saved at ${lastSaved}` : '' }}
        </span>
      </div>
    </div>

    <div class="assessment-view__progress">
      <div class="assessment-view__progress-bar">
        <div
          class="assessment-view__progress-fill"
          :style="{ width: `${progressPercent}%` }"
        ></div>
      </div>
      <span class="assessment-view__progress-text">{{ answeredCount }} / {{ questions.length }} answered</span>
    </div>

    <div class="assessment-view__question-area">
      <div v-if="currentQuestion" class="assessment-view__question">
        <div class="assessment-view__question-header">
          <span class="assessment-view__question-number">Question {{ currentIndex + 1 }} of {{ questions.length }}</span>
          <span class="assessment-view__question-points">{{ currentQuestion.points }} pts</span>
        </div>

        <p class="assessment-view__question-text">{{ currentQuestion.questionText }}</p>

        <div v-if="currentQuestion.questionType === 'MULTIPLE_CHOICE'" class="assessment-view__options">
          <label
            v-for="(option, idx) in parsedOptions"
            :key="idx"
            class="assessment-view__option"
            :class="{
              'assessment-view__option--selected': answers[currentQuestion.id]?.selectedOption === option,
              'assessment-view__option--correct': showFeedback && option === currentQuestion.correctAnswer,
              'assessment-view__option--incorrect': showFeedback && answers[currentQuestion.id]?.selectedOption === option && option !== currentQuestion.correctAnswer,
            }"
          >
            <input
              type="radio"
              :name="`q-${currentQuestion.id}`"
              :value="option"
              :checked="answers[currentQuestion.id]?.selectedOption === option"
              @change="selectOption(currentQuestion.id, option)"
              class="assessment-view__radio"
              :disabled="showFeedback"
            />
            <span class="assessment-view__option-label">{{ String.fromCharCode(65 + idx) }}.</span>
            <span class="assessment-view__option-text">{{ option }}</span>
          </label>
        </div>

        <div v-else class="assessment-view__text-answer">
          <textarea
            :value="answers[currentQuestion.id]?.answerText || ''"
            @input="setAnswerText(currentQuestion.id, ($event.target as HTMLTextAreaElement).value)"
            class="assessment-view__textarea"
            rows="6"
            placeholder="Type your answer here..."
            :disabled="showFeedback"
          ></textarea>
        </div>

        <div v-if="showFeedback && feedback" class="assessment-view__feedback">
          <p class="assessment-view__feedback-result" :class="feedback.isCorrect ? 'assessment-view__feedback-result--correct' : 'assessment-view__feedback-result--incorrect'">
            {{ feedback.isCorrect ? 'Correct!' : 'Incorrect' }}
          </p>
          <p v-if="currentQuestion.explanation" class="assessment-view__feedback-explanation">
            {{ currentQuestion.explanation }}
          </p>
        </div>
      </div>
    </div>

    <div class="assessment-view__navigation">
      <button type="button" class="assessment-view__nav-btn" @click="prevQuestion" :disabled="currentIndex === 0">Previous</button>
      <div class="assessment-view__question-dots">
        <button
          v-for="(q, idx) in questions"
          :key="q.id"
          type="button"
          class="assessment-view__dot"
          :class="{
            'assessment-view__dot--active': idx === currentIndex,
            'assessment-view__dot--answered': !!answers[q.id],
          }"
          @click="currentIndex = idx"
        >
          {{ idx + 1 }}
        </button>
      </div>
      <button
        v-if="currentIndex < questions.length - 1"
        type="button"
        class="assessment-view__nav-btn"
        @click="nextQuestion"
      >
        Next
      </button>
      <button
        v-else
        type="button"
        class="assessment-view__submit-btn"
        @click="emit('submit')"
      >
        Submit Assessment
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue'
import type { Question } from '@/types/quiz'
import CountdownTimer from './CountdownTimer.vue'

const props = defineProps<{
  quizTitle: string
  questions: Question[]
  timeRemaining: number
  showFeedback: boolean
  isSaving: boolean
  lastSaved: string
}>()

const emit = defineEmits<{
  submit: []
  autosave: [answers: Record<number, { answerText?: string; selectedOption?: string }>, timeRemainingSeconds: number]
  timeExpired: []
}>()

const currentIndex = ref(0)
const answers = reactive<Record<number, { answerText?: string; selectedOption?: string }>>({})
const liveRemaining = ref(props.timeRemaining)

watch(() => props.timeRemaining, (t) => {
  liveRemaining.value = t
})

function onTimerTick(remaining: number) {
  liveRemaining.value = remaining
}

const currentQuestion = computed(() => props.questions[currentIndex.value] || null)

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.options) return []
  try {
    const parsed = JSON.parse(currentQuestion.value.options)
    return parsed.options || []
  } catch {
    return []
  }
})

const answeredCount = computed(() => Object.keys(answers).length)
const progressPercent = computed(() => props.questions.length > 0 ? (answeredCount.value / props.questions.length) * 100 : 0)

const feedback = computed(() => {
  if (!currentQuestion.value || !props.showFeedback) return null
  const answer = answers[currentQuestion.value.id]
  if (!answer) return null
  const isCorrect = answer.selectedOption === currentQuestion.value.correctAnswer ||
    answer.answerText === currentQuestion.value.correctAnswer
  return { isCorrect }
})

function selectOption(questionId: number, option: string) {
  answers[questionId] = { ...answers[questionId], selectedOption: option }
  triggerAutosave()
}

function setAnswerText(questionId: number, text: string) {
  answers[questionId] = { ...answers[questionId], answerText: text }
  triggerAutosave()
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < props.questions.length - 1) currentIndex.value++
}

let autosaveTimer: ReturnType<typeof setTimeout> | null = null
function triggerAutosave() {
  if (autosaveTimer) clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(() => {
    emit('autosave', { ...answers }, liveRemaining.value)
  }, 15000)
}

function handleTimeExpired() {
  emit('timeExpired')
}
</script>

<style scoped>
.assessment-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.assessment-view__title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e293b;
}

.assessment-view__header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.assessment-view__autosave {
  font-size: 0.75rem;
  color: #94a3b8;
}

.assessment-view__autosave--saving {
  color: #f59e0b;
}

.assessment-view__progress {
  margin-bottom: 24px;
}

.assessment-view__progress-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.assessment-view__progress-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 3px;
  transition: width 0.3s;
}

.assessment-view__progress-text {
  font-size: 0.78rem;
  color: #64748b;
}

.assessment-view__question-area {
  background: #fff;
  border-radius: 10px;
  padding: 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  min-height: 300px;
}

.assessment-view__question-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.assessment-view__question-number {
  font-size: 0.82rem;
  font-weight: 600;
  color: #64748b;
}

.assessment-view__question-points {
  font-size: 0.78rem;
  color: #94a3b8;
}

.assessment-view__question-text {
  font-size: 1.05rem;
  color: #0f172a;
  line-height: 1.6;
  margin-bottom: 24px;
}

.assessment-view__options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.assessment-view__option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.assessment-view__option:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.assessment-view__option--selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.assessment-view__option--correct {
  border-color: #16a34a;
  background: #dcfce7;
}

.assessment-view__option--incorrect {
  border-color: #dc2626;
  background: #fef2f2;
}

.assessment-view__radio {
  accent-color: #3b82f6;
}

.assessment-view__option-label {
  font-weight: 700;
  color: #64748b;
  min-width: 20px;
}

.assessment-view__option-text {
  font-size: 0.9rem;
  color: #334155;
}

.assessment-view__textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.9rem;
  resize: vertical;
  outline: none;
  font-family: inherit;
}

.assessment-view__textarea:focus {
  border-color: #3b82f6;
}

.assessment-view__feedback {
  margin-top: 20px;
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
}

.assessment-view__feedback-result {
  font-weight: 700;
  font-size: 1rem;
  margin-bottom: 6px;
}

.assessment-view__feedback-result--correct {
  color: #16a34a;
}

.assessment-view__feedback-result--incorrect {
  color: #dc2626;
}

.assessment-view__feedback-explanation {
  font-size: 0.88rem;
  color: #475569;
  line-height: 1.5;
}

.assessment-view__navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.assessment-view__nav-btn {
  padding: 8px 20px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  font-size: 0.85rem;
  cursor: pointer;
  color: #374151;
}

.assessment-view__nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.assessment-view__submit-btn {
  padding: 8px 24px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.assessment-view__submit-btn:hover {
  background: #15803d;
}

.assessment-view__question-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.assessment-view__dot {
  width: 28px;
  height: 28px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.72rem;
  cursor: pointer;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.assessment-view__dot--active {
  border-color: #3b82f6;
  color: #3b82f6;
  font-weight: 700;
}

.assessment-view__dot--answered {
  background: #dbeafe;
}
</style>
