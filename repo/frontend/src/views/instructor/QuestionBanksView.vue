<template>
  <div class="question-banks">
    <div class="page-header">
      <h1>Question Banks</h1>
      <button type="button" @click="showCreate = true" class="btn-primary">Create Bank</button>
    </div>
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else class="banks-list">
      <div v-for="bank in banks" :key="bank.id" class="bank-card">
        <h3>{{ bank.name }}</h3>
        <p>{{ bank.description }}</p>
        <div class="meta">
          <span>Subject: {{ bank.subject || 'General' }}</span>
          <span>Questions: {{ bank.questions?.length || 0 }}</span>
        </div>
        <router-link to="/instructor/question-banks" class="card-link">Manage Questions</router-link>
      </div>
    </div>
    <div v-if="showCreate" class="editor-overlay">
      <div class="editor-panel">
        <button type="button" class="editor-close" @click="showCreate = false">Close</button>
        <QuestionBankEditor
          :questions="[]"
          :loading="false"
          @add-question="() => {}"
          @edit-question="() => {}"
          @delete-question="() => {}"
        />
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import QuestionBankEditor from '@/components/instructor/QuestionBankEditor.vue'
import { getQuestionBanks } from '@/api/quiz'
import type { QuestionBank } from '@/types/quiz'

const banks = ref<QuestionBank[]>([])
const loading = ref(false)
const showCreate = ref(false)

async function loadBanks() {
  loading.value = true
  try {
    const page = await getQuestionBanks(0, 20)
    banks.value = page.content
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadBanks)
</script>
<style scoped>
.question-banks { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { color: #1a365d; }
.btn-primary { padding: 10px 20px; background: #3182ce; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
.banks-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.bank-card { background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; }
.bank-card h3 { color: #2d3748; margin-bottom: 6px; }
.bank-card p { color: #718096; font-size: 14px; margin-bottom: 10px; }
.meta { display: flex; gap: 16px; font-size: 12px; color: #a0aec0; margin-bottom: 12px; }
.card-link { color: #3182ce; font-size: 14px; font-weight: 600; text-decoration: none; }
.loading { text-align: center; color: #718096; padding: 40px; }
.editor-overlay { margin-top: 24px; }
.editor-panel { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; position: relative; }
.editor-close { position: absolute; top: 12px; right: 12px; padding: 6px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 6px; cursor: pointer; font-size: 13px; }
</style>
