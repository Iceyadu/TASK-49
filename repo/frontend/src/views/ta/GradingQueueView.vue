<template>
  <div class="grading-queue-view">
    <h1>Grading Queue</h1>
    <p class="subtitle">Subjective items awaiting rubric-based scoring</p>
    <GradingQueue :items="items" :loading="loading" :total-items="totalItems" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import GradingQueue from '@/components/ta/GradingQueue.vue'
import { getGradingQueue } from '@/api/grading'

const items = ref<any[]>([])
const loading = ref(false)
const totalItems = ref(0)

onMounted(async () => {
  loading.value = true
  try {
    const page = await getGradingQueue(0, 20, 'PENDING')
    items.value = page.content || []
    totalItems.value = page.totalElements || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.grading-queue-view { padding: 24px; }
.grading-queue-view h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; font-size: 14px; }
</style>
