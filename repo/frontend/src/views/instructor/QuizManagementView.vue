<template>
  <div class="quiz-management">
    <div class="page-header">
      <h1>Quiz Management</h1>
      <button type="button" @click="showAssembler = true" class="btn-primary">Assemble Quiz</button>
    </div>
    <div v-if="loading" class="loading">Loading quizzes...</div>
    <table v-else-if="quizzes.length > 0" class="data-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Questions</th>
          <th>Time Limit</th>
          <th>Attempts</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="q in quizzes" :key="q.id">
          <td>{{ q.title }}</td>
          <td>{{ q.totalQuestions }}</td>
          <td>{{ q.timeLimitMinutes ? q.timeLimitMinutes + ' min' : 'None' }}</td>
          <td>{{ q.maxAttempts }}</td>
          <td>
            <span :class="q.isPublished ? 'published' : 'draft'">{{ q.isPublished ? 'Published' : 'Draft' }}</span>
          </td>
          <td>
            <router-link :to="`/instructor/quizzes/${q.id}`" class="action-link">View</router-link>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">No quizzes created yet.</div>
    <div v-if="showAssembler" class="assembler-panel">
      <QuizAssembler :assembling="assembling" @assemble="onAssemble" />
      <button type="button" class="assembler-dismiss" @click="showAssembler = false">Close</button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import QuizAssembler from '@/components/instructor/QuizAssembler.vue'
import { assembleQuiz, getQuizzes } from '@/api/quiz'
import type { QuizPaper, QuizRule } from '@/types/quiz'

const quizzes = ref<QuizPaper[]>([])
const loading = ref(false)
const showAssembler = ref(false)
const assembling = ref(false)

async function loadQuizzes() {
  loading.value = true
  try {
    const page = await getQuizzes(0, 20)
    quizzes.value = page.content
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function onAssemble(data: { totalQuestions: number; rules: QuizRule[]; questionBankId?: number; title?: string }) {
  assembling.value = true
  try {
    await assembleQuiz({
      title: data.title || 'Assembled quiz',
      questionBankId: data.questionBankId || 1,
      totalQuestions: data.totalQuestions,
      rules: data.rules,
    })
    showAssembler.value = false
    await loadQuizzes()
  } catch (e) {
    console.error(e)
  } finally {
    assembling.value = false
  }
}

onMounted(loadQuizzes)
</script>
<style scoped>
.quiz-management { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { color: #1a365d; }
.btn-primary { padding: 10px 20px; background: #3182ce; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
.data-table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.data-table th { background: #f7fafc; padding: 12px 16px; text-align: left; font-size: 13px; color: #4a5568; border-bottom: 1px solid #e2e8f0; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.published { color: #38a169; font-weight: 600; }
.draft { color: #d69e2e; font-weight: 600; }
.action-link { color: #3182ce; text-decoration: none; font-weight: 500; }
.loading, .empty { text-align: center; color: #718096; padding: 40px; }
.assembler-panel { margin-top: 24px; position: relative; padding-top: 16px; border-top: 1px solid #e2e8f0; }
.assembler-dismiss { margin-top: 12px; padding: 8px 16px; background: white; border: 1px solid #cbd5e1; border-radius: 6px; cursor: pointer; }
</style>
