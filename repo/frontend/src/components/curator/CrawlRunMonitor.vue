<template>
  <div class="crawl-monitor">
    <div class="crawl-monitor__header">
      <h3 class="crawl-monitor__title">Crawl Runs</h3>
      <button type="button" class="crawl-monitor__start-btn" @click="emit('startRun')">
        Start New Run
      </button>
    </div>

    <LoadingSpinner v-if="loading" message="Loading crawl runs..." />

    <table v-else-if="runs.length > 0" class="crawl-monitor__table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Status</th>
          <th>Pages Crawled</th>
          <th>Items Extracted</th>
          <th>Errors</th>
          <th>Started</th>
          <th>Completed</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="run in runs" :key="run.id">
          <td class="crawl-monitor__id">#{{ run.id }}</td>
          <td>
            <span class="crawl-monitor__status" :class="statusClass(run.status)">
              {{ run.status }}
            </span>
          </td>
          <td>{{ run.pagesCrawled }} / {{ run.totalPages }}</td>
          <td>{{ run.itemsExtracted }}</td>
          <td>
            <span :class="{ 'crawl-monitor__error-count': run.pagesFailed > 0 }">
              {{ run.pagesFailed }}
            </span>
          </td>
          <td class="crawl-monitor__date">{{ formatDate(run.startedAt) }}</td>
          <td class="crawl-monitor__date">{{ run.completedAt ? formatDate(run.completedAt) : '--' }}</td>
          <td>
            <button
              v-if="run.status === 'RUNNING' || run.status === 'PENDING'"
              type="button"
              class="crawl-monitor__cancel-btn"
              @click="emit('cancel', run.id)"
            >
              Cancel
            </button>
            <button
              v-if="run.errorLog"
              type="button"
              class="crawl-monitor__log-btn"
              @click="showErrorLog(run)"
            >
              View Log
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <EmptyState v-else title="No crawl runs" description="Start a new crawl run to begin extraction." />

    <Teleport to="body">
      <div v-if="errorLogVisible" class="crawl-monitor__log-overlay" @click.self="errorLogVisible = false">
        <div class="crawl-monitor__log-modal">
          <header class="crawl-monitor__log-header">
            <h4>Error Log - Run #{{ selectedRun?.id }}</h4>
            <button type="button" @click="errorLogVisible = false">&times;</button>
          </header>
          <pre class="crawl-monitor__log-content">{{ selectedRun?.errorLog }}</pre>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { CrawlRun } from '@/types/crawl'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps<{
  runs: CrawlRun[]
  loading: boolean
}>()

const emit = defineEmits<{
  startRun: []
  cancel: [runId: number]
}>()

const errorLogVisible = ref(false)
const selectedRun = ref<CrawlRun | null>(null)

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString()
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'crawl-monitor__status--pending',
    RUNNING: 'crawl-monitor__status--running',
    COMPLETED: 'crawl-monitor__status--completed',
    FAILED: 'crawl-monitor__status--failed',
    CANCELLED: 'crawl-monitor__status--cancelled',
  }
  return map[status] || ''
}

function showErrorLog(run: CrawlRun) {
  selectedRun.value = run
  errorLogVisible.value = true
}
</script>

<style scoped>
.crawl-monitor__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.crawl-monitor__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.crawl-monitor__start-btn {
  padding: 8px 16px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
}

.crawl-monitor__start-btn:hover {
  background: #15803d;
}

.crawl-monitor__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.crawl-monitor__table th {
  text-align: left;
  padding: 12px 14px;
  background: #f8fafc;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.crawl-monitor__table td {
  padding: 10px 14px;
  font-size: 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.crawl-monitor__id {
  font-weight: 600;
  color: #64748b;
}

.crawl-monitor__status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.crawl-monitor__status--pending { background: #f1f5f9; color: #64748b; }
.crawl-monitor__status--running { background: #dbeafe; color: #2563eb; }
.crawl-monitor__status--completed { background: #dcfce7; color: #16a34a; }
.crawl-monitor__status--failed { background: #fee2e2; color: #dc2626; }
.crawl-monitor__status--cancelled { background: #fef9c3; color: #ca8a04; }

.crawl-monitor__error-count {
  color: #dc2626;
  font-weight: 600;
}

.crawl-monitor__date {
  font-size: 0.78rem;
  color: #64748b;
  white-space: nowrap;
}

.crawl-monitor__cancel-btn {
  padding: 4px 10px;
  border: 1px solid #fca5a5;
  border-radius: 4px;
  background: #fff;
  color: #dc2626;
  font-size: 0.75rem;
  cursor: pointer;
  margin-right: 4px;
}

.crawl-monitor__cancel-btn:hover {
  background: #fef2f2;
}

.crawl-monitor__log-btn {
  padding: 4px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  font-size: 0.75rem;
  cursor: pointer;
}

.crawl-monitor__log-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.crawl-monitor__log-modal {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.crawl-monitor__log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.crawl-monitor__log-header h4 {
  font-size: 1rem;
  font-weight: 600;
}

.crawl-monitor__log-header button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #64748b;
}

.crawl-monitor__log-content {
  padding: 20px;
  font-family: monospace;
  font-size: 0.8rem;
  overflow: auto;
  flex: 1;
  background: #f8fafc;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
