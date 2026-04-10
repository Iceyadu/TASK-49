<template>
  <div class="crawl-sources">
    <div class="page-header"><h1>Crawl Sources</h1><button @click="showForm = true" class="btn-primary">Add Source</button></div>
    <div v-if="loading" class="loading">Loading sources...</div>
    <div v-else-if="sources.length === 0" class="empty">No crawl sources configured yet.</div>
    <div v-else class="sources-list">
      <div v-for="source in sources" :key="source.id" class="source-card">
        <div class="source-header"><h3>{{ source.name }}</h3><span :class="source.enabled ? 'status-active' : 'status-disabled'">{{ source.enabled ? 'Active' : 'Disabled' }}</span></div>
        <p class="url">{{ source.baseUrl }}</p>
        <p class="desc">{{ source.description }}</p>
        <div class="meta"><span>Rate limit: {{ source.rateLimitPerMinute }}/min</span><span v-if="source.requiresAuth">Requires auth</span></div>
        <div class="actions"><button @click="editSource(source)">Edit</button><button @click="deleteSource(source.id)">Delete</button></div>
      </div>
    </div>
    <CrawlSourceForm
      v-if="showForm"
      :source="editingSource"
      :saving="saving"
      @cancel="showForm = false; editingSource = null"
      @submit="submitSource"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CrawlSourceForm from '@/components/curator/CrawlSourceForm.vue'
import { getSources, deleteSource as apiDelete, createSource as apiCreate, updateSource as apiUpdate } from '@/api/crawl'

const sources = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingSource = ref<any>(null)
const saving = ref(false)

async function loadSources() {
  loading.value = true
  try { sources.value = await getSources() } catch (e) { console.error(e) }
  finally { loading.value = false; showForm.value = false }
}
function editSource(source: any) { editingSource.value = source; showForm.value = true }
async function deleteSource(id: number) { if (confirm('Delete this source?')) { await apiDelete(id); loadSources() } }
async function submitSource(data: any) {
  saving.value = true
  try {
    if (editingSource.value?.id) await apiUpdate(editingSource.value.id, data)
    else await apiCreate(data)
    editingSource.value = null
    showForm.value = false
    await loadSources()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}
onMounted(loadSources)
</script>

<style scoped>
.crawl-sources { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { color: #1a365d; }
.btn-primary { padding: 10px 20px; background: #3182ce; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
.sources-list { display: grid; gap: 16px; }
.source-card { background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; }
.source-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.source-header h3 { color: #2d3748; }
.status-active { color: #38a169; font-size: 12px; font-weight: 600; }
.status-disabled { color: #e53e3e; font-size: 12px; font-weight: 600; }
.url { color: #3182ce; font-size: 13px; font-family: monospace; margin-bottom: 4px; word-break: break-all; }
.desc { color: #718096; font-size: 14px; margin-bottom: 8px; }
.meta { display: flex; gap: 16px; font-size: 12px; color: #a0aec0; margin-bottom: 12px; }
.actions { display: flex; gap: 8px; }
.actions button { padding: 6px 14px; border: 1px solid #e2e8f0; background: white; border-radius: 4px; cursor: pointer; font-size: 13px; }
.loading, .empty { text-align: center; color: #718096; padding: 40px; }
</style>
