<template>
  <div class="submissions-review">
    <h1>Submissions Review</h1>
    <p class="subtitle">Review student submissions and grading progress</p>
    <GradingOverview :submissions="submissions" :loading="loading" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import GradingOverview from '@/components/instructor/GradingOverview.vue'
import { getGradingQueue } from '@/api/grading'

const submissions = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const page = await getGradingQueue(0, 50)
    submissions.value = page.content || []
  } catch (e) {
    submissions.value = []
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.submissions-review { padding: 24px; }
.submissions-review h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; font-size: 14px; }
</style>
