<template>
  <div class="crawl-runs">
    <h1>Crawl Runs</h1>
    <p class="subtitle">Monitor and manage crawl job executions</p>

    <div class="filters">
      <label>
        Source
        <select v-model.number="selectedSourceId" @change="onSourceChange">
          <option :value="0">All sources</option>
          <option v-for="source in sources" :key="source.id" :value="source.id">{{ source.name }}</option>
        </select>
      </label>
      <label>
        Start-run rule
        <select v-model.number="selectedRuleId">
          <option :value="0">Select rule</option>
          <option v-for="rule in rules" :key="rule.id" :value="rule.id">
            v{{ rule.versionNumber }} {{ rule.isActive ? '(active)' : '' }}
          </option>
        </select>
      </label>
    </div>

    <CrawlRunMonitor
      :runs="runs"
      :loading="loading"
      @start-run="onStartRun"
      @cancel="onCancelRun"
    />
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CrawlRunMonitor from '@/components/curator/CrawlRunMonitor.vue'
import { cancelRun, getRules, getRuns, getSources, startRun } from '@/api/crawl'
import type { CrawlRun, CrawlRuleVersion, CrawlSource } from '@/types/crawl'

const runs = ref<CrawlRun[]>([])
const sources = ref<CrawlSource[]>([])
const rules = ref<CrawlRuleVersion[]>([])
const loading = ref(false)
const selectedSourceId = ref(0)
const selectedRuleId = ref(0)

async function loadSources() {
  sources.value = await getSources()
}

async function loadRulesForSelectedSource() {
  if (!selectedSourceId.value) {
    rules.value = []
    selectedRuleId.value = 0
    return
  }

  rules.value = await getRules(selectedSourceId.value)
  const active = rules.value.find(r => r.isActive)
  selectedRuleId.value = active?.id ?? rules.value[0]?.id ?? 0
}

async function loadRuns() {
  loading.value = true
  try {
    const response = await getRuns(selectedSourceId.value || undefined)
    runs.value = response.content ?? []
  } finally {
    loading.value = false
  }
}

async function onSourceChange() {
  await loadRulesForSelectedSource()
  await loadRuns()
}

async function onStartRun() {
  if (!selectedSourceId.value || !selectedRuleId.value) return
  await startRun(selectedSourceId.value, selectedRuleId.value)
  await loadRuns()
}

async function onCancelRun(runId: number) {
  await cancelRun(runId)
  await loadRuns()
}

onMounted(async () => {
  await loadSources()
  await loadRuns()
})
</script>
<style scoped>
.crawl-runs { padding: 24px; }
.crawl-runs h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; }
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.filters label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #475569;
}
.filters select {
  min-width: 220px;
  padding: 8px;
}
</style>
