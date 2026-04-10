<template>
  <div class="extraction-tester">
    <h3 class="extraction-tester__title">Extraction Tester</h3>

    <div class="extraction-tester__input-row">
      <input
        v-model="sampleUrl"
        type="url"
        class="extraction-tester__url-input"
        placeholder="Enter a sample URL to test extraction..."
      />
      <button
        type="button"
        class="extraction-tester__run-btn"
        :disabled="!sampleUrl || running"
        @click="handleRunTest"
      >
        {{ running ? 'Running...' : 'Run Test' }}
      </button>
    </div>

    <div v-if="running" class="extraction-tester__loading">
      <LoadingSpinner message="Extracting content from URL..." />
    </div>

    <div v-else-if="error" class="extraction-tester__error">
      <ErrorDisplay :message="error" :onRetry="handleRunTest" />
    </div>

    <div v-else-if="results" class="extraction-tester__results">
      <h4 class="extraction-tester__results-title">Extraction Results</h4>

      <div class="extraction-tester__result-stats">
        <span class="extraction-tester__stat">
          Fields extracted: <strong>{{ results.fieldsExtracted }}</strong>
        </span>
        <span class="extraction-tester__stat">
          Time: <strong>{{ results.durationMs }}ms</strong>
        </span>
      </div>

      <div class="extraction-tester__result-panel">
        <table class="extraction-tester__result-table">
          <thead>
            <tr>
              <th>Field</th>
              <th>Value</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="field in results.fields" :key="field.name">
              <td class="extraction-tester__field-name">{{ field.name }}</td>
              <td class="extraction-tester__field-value">{{ field.value || '(empty)' }}</td>
              <td>
                <span
                  class="extraction-tester__field-status"
                  :class="field.matched ? 'extraction-tester__field-status--ok' : 'extraction-tester__field-status--miss'"
                >
                  {{ field.matched ? 'Matched' : 'No match' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <details class="extraction-tester__raw">
        <summary>Raw Response</summary>
        <pre class="extraction-tester__raw-content">{{ JSON.stringify(results.raw, null, 2) }}</pre>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorDisplay from '@/components/common/ErrorDisplay.vue'

interface TestField {
  name: string
  value: string
  matched: boolean
}

interface TestResults {
  fieldsExtracted: number
  durationMs: number
  fields: TestField[]
  raw: any
}

const emit = defineEmits<{
  test: [url: string]
}>()

const sampleUrl = ref('')
const running = ref(false)
const error = ref('')
const results = ref<TestResults | null>(null)

defineExpose({ setResults, setError, setRunning })

function handleRunTest() {
  if (!sampleUrl.value) return
  error.value = ''
  results.value = null
  running.value = true
  emit('test', sampleUrl.value)
}

function setResults(data: TestResults) {
  results.value = data
  running.value = false
}

function setError(msg: string) {
  error.value = msg
  running.value = false
}

function setRunning(val: boolean) {
  running.value = val
}
</script>

<style scoped>
.extraction-tester {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.extraction-tester__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
}

.extraction-tester__input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.extraction-tester__url-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
}

.extraction-tester__url-input:focus {
  border-color: #3b82f6;
}

.extraction-tester__run-btn {
  padding: 8px 20px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.extraction-tester__run-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.extraction-tester__results-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.extraction-tester__result-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.extraction-tester__stat {
  font-size: 0.85rem;
  color: #64748b;
}

.extraction-tester__result-table {
  width: 100%;
  border-collapse: collapse;
}

.extraction-tester__result-table th {
  text-align: left;
  padding: 8px 12px;
  background: #f8fafc;
  font-size: 0.75rem;
  text-transform: uppercase;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.extraction-tester__result-table td {
  padding: 8px 12px;
  font-size: 0.85rem;
  border-bottom: 1px solid #f1f5f9;
}

.extraction-tester__field-name {
  font-weight: 600;
  color: #334155;
}

.extraction-tester__field-value {
  color: #475569;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.extraction-tester__field-status {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.extraction-tester__field-status--ok {
  background: #dcfce7;
  color: #16a34a;
}

.extraction-tester__field-status--miss {
  background: #fee2e2;
  color: #dc2626;
}

.extraction-tester__raw {
  margin-top: 16px;
  cursor: pointer;
}

.extraction-tester__raw summary {
  font-size: 0.85rem;
  color: #3b82f6;
  font-weight: 500;
}

.extraction-tester__raw-content {
  margin-top: 8px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 0.78rem;
  overflow-x: auto;
  font-family: monospace;
  max-height: 300px;
  overflow-y: auto;
}
</style>
