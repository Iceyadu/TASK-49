<template>
  <div class="crawl-rules">
    <h1>Crawl Rules</h1>
    <p class="subtitle">Manage extraction rules for each source profile</p>

    <div class="source-picker">
      <label>
        Source
        <select v-model.number="selectedSourceId" @change="loadRules">
          <option :value="0">Select source</option>
          <option v-for="source in sources" :key="source.id" :value="source.id">{{ source.name }}</option>
        </select>
      </label>
    </div>

    <RuleVersionEditor
      :versions="versions"
      :current-version="currentVersion"
      :saving="saving"
      @save="onSave"
      @revert="onRevert"
      @cancel="onCancel"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import RuleVersionEditor from '@/components/curator/RuleVersionEditor.vue'
import { createRule, getRules, getSources, revertRule } from '@/api/crawl'
import type { CrawlRuleVersion, CrawlSource } from '@/types/crawl'

const sources = ref<CrawlSource[]>([])
const selectedSourceId = ref(0)
const versions = ref<CrawlRuleVersion[]>([])
const saving = ref(false)

const currentVersion = computed(() => versions.value.find(v => v.isActive) ?? versions.value[0] ?? null)

async function loadSources() {
  sources.value = await getSources()
}

async function loadRules() {
  if (!selectedSourceId.value) {
    versions.value = []
    return
  }
  versions.value = await getRules(selectedSourceId.value)
}

async function onSave(data: { extractionMethod: string; ruleDefinition: string; fieldMappings: string; notes: string }) {
  if (!selectedSourceId.value) return
  saving.value = true
  try {
    await createRule(selectedSourceId.value, {
      extractionMethod: data.extractionMethod,
      ruleDefinition: safeJsonParse(data.ruleDefinition),
      fieldMappings: safeJsonParse(data.fieldMappings),
      notes: data.notes,
    })
    await loadRules()
  } finally {
    saving.value = false
  }
}

async function onRevert(targetVersionId: number) {
  const ruleId = currentVersion.value?.id ?? targetVersionId
  await revertRule(ruleId, targetVersionId)
  await loadRules()
}

function onCancel() {
  // No local draft cache yet; keeping event wired for UX contract completeness.
}

function safeJsonParse(value: string): Record<string, unknown> {
  if (!value || !value.trim()) return {}
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

onMounted(async () => {
  await loadSources()
})
</script>
<style scoped>
.crawl-rules { padding: 24px; }
.crawl-rules h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; }
.source-picker {
  margin-bottom: 16px;
}
.source-picker label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #475569;
}
.source-picker select {
  min-width: 260px;
  padding: 8px;
}
</style>
