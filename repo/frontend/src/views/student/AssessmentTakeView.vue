<template>
  <div class="assessment-take">
    <div v-if="!started" class="start-panel">
      <h1>{{ quiz?.title || 'Assessment' }}</h1>
      <div v-if="quiz" class="quiz-info">
        <p>Questions: {{ quiz.totalQuestions }}</p>
        <p v-if="quiz.timeLimitMinutes">Time Limit: {{ quiz.timeLimitMinutes }} minutes</p>
        <p>Max Attempts: {{ quiz.maxAttempts }}</p>
      </div>
      <button type="button" @click="startAssessment" class="btn-primary" :disabled="starting">
        {{ starting ? 'Starting...' : 'Start Assessment' }}
      </button>
    </div>
    <AssessmentView
      v-else-if="quiz && submission"
      :quiz-title="quiz.title"
      :questions="quiz.questions ?? []"
      :time-remaining="assessmentTimeSeconds"
      :show-feedback="false"
      :is-saving="isSaving"
      :last-saved="lastSaved"
      @submit="handleSubmit"
      @autosave="handleAutosave"
      @time-expired="handleSubmit"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssessmentView from '@/components/student/AssessmentView.vue'
import { getQuiz } from '@/api/quiz'
import { autosave, startSubmission, submitSubmission } from '@/api/submissions'
import type { QuizPaper } from '@/types/quiz'
import type { Submission } from '@/types/submission'

const route = useRoute()
const router = useRouter()
const quiz = ref<QuizPaper | null>(null)
const submission = ref<Submission | null>(null)
const started = ref(false)
const starting = ref(false)
const isSaving = ref(false)
const lastSaved = ref('')

const assessmentTimeSeconds = computed(() => {
  if (!submission.value) return 0
  if (submission.value.timeRemainingSeconds != null && submission.value.timeRemainingSeconds > 0) {
    return submission.value.timeRemainingSeconds
  }
  const mins = quiz.value?.timeLimitMinutes
  return mins != null && mins > 0 ? mins * 60 : 3600
})

onMounted(async () => {
  try {
    quiz.value = await getQuiz(Number(route.params.id))
  } catch (e) {
    console.error(e)
  }
})

async function startAssessment() {
  starting.value = true
  try {
    submission.value = await startSubmission(Number(route.params.id))
    started.value = true
  } catch (e: unknown) {
    const err = e as { response?: { data?: { error?: { message?: string } } } }
    alert(err.response?.data?.error?.message || 'Failed to start')
  } finally {
    starting.value = false
  }
}

async function handleAutosave(
  answerMap: Record<number, { answerText?: string; selectedOption?: string }>,
  timeRemainingSeconds: number
) {
  if (!submission.value) return
  isSaving.value = true
  try {
    await autosave(submission.value.id, {
      answers: Object.entries(answerMap).map(([qid, a]) => ({
        questionId: Number(qid),
        answerText: a.answerText,
        selectedOption: a.selectedOption,
      })),
      timeRemainingSeconds,
    })
    lastSaved.value = new Date().toLocaleTimeString()
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}

async function handleSubmit() {
  if (!submission.value) return
  try {
    await submitSubmission(submission.value.id)
    router.push('/student/dashboard')
  } catch (e) {
    console.error(e)
    alert('Failed to submit')
  }
}
</script>
<style scoped>
.assessment-take { padding: 24px; }
.start-panel { max-width: 500px; margin: 60px auto; text-align: center; background: white; border-radius: 12px; padding: 48px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.start-panel h1 { color: #1a365d; margin-bottom: 16px; }
.quiz-info { margin-bottom: 24px; }
.quiz-info p { color: #718096; margin-bottom: 4px; }
.btn-primary { padding: 14px 32px; background: #3182ce; color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
