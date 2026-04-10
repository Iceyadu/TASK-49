<template>
  <div class="quiz-detail">
    <div v-if="loading" class="loading">Loading quiz...</div>
    <template v-else-if="quiz">
      <div class="page-header">
        <div>
          <h1>{{ quiz.title }}</h1>
          <p class="desc">{{ quiz.description }}</p>
        </div>
        <div class="actions">
          <button v-if="!quiz.isPublished" type="button" @click="onPublish" class="btn-primary">Publish</button>
          <button type="button" @click="showSchedule = true" class="btn-secondary">Schedule</button>
        </div>
      </div>
      <div class="stats-row">
        <div class="stat"><label>Questions</label><span>{{ quiz.totalQuestions }}</span></div>
        <div class="stat"><label>Total Points</label><span>{{ quiz.totalPoints }}</span></div>
        <div class="stat"><label>Time Limit</label><span>{{ quiz.timeLimitMinutes ? quiz.timeLimitMinutes + ' min' : 'None' }}</span></div>
        <div class="stat"><label>Max Attempts</label><span>{{ quiz.maxAttempts }}</span></div>
        <div class="stat">
          <label>Status</label>
          <span :class="quiz.isPublished ? 'published' : 'draft'">{{ quiz.isPublished ? 'Published' : 'Draft' }}</span>
        </div>
      </div>
      <h2>Questions</h2>
      <div class="questions-list">
        <div v-for="(q, idx) in quiz.questions" :key="q.id" class="question-item">
          <span class="q-num">{{ idx + 1 }}</span>
          <div class="q-content">
            <p>{{ q.questionText }}</p>
            <div class="q-meta">
              <span>Type: {{ q.questionType }}</span>
              <span>Difficulty: {{ q.difficultyLevel }}/5</span>
              <span>Points: {{ q.points }}</span>
            </div>
          </div>
        </div>
      </div>
      <QuizScheduleForm
        v-if="showSchedule"
        :initial-data="scheduleInitial"
        :saving="scheduleSaving"
        @cancel="showSchedule = false"
        @submit="onScheduleSubmit"
      />
    </template>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import QuizScheduleForm from '@/components/instructor/QuizScheduleForm.vue'
import { getQuiz, publishQuiz as apiPublish, scheduleQuiz } from '@/api/quiz'
import type { QuizPaper } from '@/types/quiz'

const route = useRoute()
const quiz = ref<QuizPaper | null>(null)
const loading = ref(false)
const showSchedule = ref(false)
const scheduleSaving = ref(false)

const scheduleInitial = computed(() => {
  if (!quiz.value) return null
  return {
    releaseStart: quiz.value.releaseStart,
    releaseEnd: quiz.value.releaseEnd,
    timeLimitMinutes: quiz.value.timeLimitMinutes,
    maxAttempts: quiz.value.maxAttempts,
  }
})

async function loadQuiz() {
  loading.value = true
  try {
    quiz.value = await getQuiz(Number(route.params.id))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function onPublish() {
  if (!quiz.value) return
  try {
    await apiPublish(quiz.value.id)
    await loadQuiz()
  } catch (e) {
    alert('Failed to publish')
  }
}

async function onScheduleSubmit(data: { releaseStart: string; releaseEnd: string; timeLimitMinutes: number; maxAttempts: number }) {
  if (!quiz.value) return
  scheduleSaving.value = true
  try {
    await scheduleQuiz(quiz.value.id, data.releaseStart, data.releaseEnd)
    showSchedule.value = false
    await loadQuiz()
  } catch (e) {
    console.error(e)
  } finally {
    scheduleSaving.value = false
  }
}

onMounted(loadQuiz)
</script>
<style scoped>
.quiz-detail { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-header h1 { color: #1a365d; margin-bottom: 4px; }
.desc { color: #718096; }
.actions { display: flex; gap: 8px; }
.btn-primary { padding: 10px 20px; background: #3182ce; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
.btn-secondary { padding: 10px 20px; background: white; color: #3182ce; border: 1px solid #3182ce; border-radius: 6px; cursor: pointer; font-weight: 600; }
.stats-row { display: flex; gap: 24px; margin-bottom: 32px; flex-wrap: wrap; }
.stat { background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px 24px; text-align: center; }
.stat label { display: block; font-size: 12px; color: #a0aec0; margin-bottom: 4px; }
.stat span { font-size: 20px; font-weight: 700; color: #2d3748; }
.published { color: #38a169 !important; }
.draft { color: #d69e2e !important; }
h2 { color: #2d3748; margin-bottom: 16px; }
.questions-list { display: flex; flex-direction: column; gap: 12px; }
.question-item { display: flex; gap: 16px; background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; }
.q-num { background: #ebf4ff; color: #3182ce; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.q-content p { color: #2d3748; margin-bottom: 8px; }
.q-meta { display: flex; gap: 16px; font-size: 12px; color: #a0aec0; }
.loading { text-align: center; color: #718096; padding: 40px; }
</style>
