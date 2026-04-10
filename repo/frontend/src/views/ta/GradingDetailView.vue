<template>
  <div class="grading-detail">
    <h1>Grade Submission</h1>
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <SubjectiveGradingView
      v-else-if="gradingState"
      :question-text="gradingState.submissionAnswer?.question?.questionText || '—'"
      :answer-text="gradingState.submissionAnswer?.answerText || '—'"
      :rubric-criteria="gradingState.rubricScores || []"
      :saving="saving"
      @submit="handleGrade"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SubjectiveGradingView from '@/components/ta/SubjectiveGradingView.vue'
import { getGradingState, gradeItem } from '@/api/grading'

const route = useRoute()
const gradingState = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) { error.value = 'Invalid grading ID'; return }
  loading.value = true
  try {
    gradingState.value = await getGradingState(id)
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Failed to load grading state'
  } finally {
    loading.value = false
  }
})

async function handleGrade(payload: { score: number; feedback: string }) {
  const id = Number(route.params.id)
  saving.value = true
  try {
    gradingState.value = await gradeItem(id, payload.score, payload.feedback)
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Failed to save grade'
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.grading-detail { padding: 24px; }
.grading-detail h1 { color: #1a365d; margin-bottom: 24px; }
.loading { color: #718096; }
.error { color: #dc2626; }
</style>
