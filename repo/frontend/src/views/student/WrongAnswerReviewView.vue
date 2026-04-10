<template>
  <div class="wrong-answer-view">
    <h1>Wrong Answer Review</h1>
    <p class="subtitle">Review your incorrect answers with instructor explanations</p>
    <WrongAnswerReview :items="items" :loading="loading" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import WrongAnswerReview from '@/components/student/WrongAnswerReview.vue'
import apiClient from '@/api/client'

const items = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/api/wrong-answers')
    items.value = data.data || []
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.wrong-answer-view { padding: 24px; }
.wrong-answer-view h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; font-size: 14px; }
</style>
