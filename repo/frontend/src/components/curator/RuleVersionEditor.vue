<template>
  <div class="rule-editor">
    <div class="rule-editor__main">
      <h3 class="rule-editor__title">Rule Editor</h3>

      <div class="rule-editor__field">
        <label class="rule-editor__label" for="extractionMethod">Extraction Method</label>
        <select id="extractionMethod" v-model="form.extractionMethod" class="rule-editor__select">
          <option value="XPATH">XPath</option>
          <option value="CSS_SELECTOR">CSS Selector</option>
          <option value="REGEX">Regular Expression</option>
          <option value="JSONPATH">JSONPath</option>
        </select>
      </div>

      <div class="rule-editor__field">
        <label class="rule-editor__label" for="ruleDefinition">Rule Definition</label>
        <textarea
          id="ruleDefinition"
          v-model="form.ruleDefinition"
          class="rule-editor__code-area"
          rows="8"
          placeholder="Enter your extraction rule..."
          spellcheck="false"
        ></textarea>
      </div>

      <div class="rule-editor__field">
        <label class="rule-editor__label" for="fieldMappings">Field Mappings (JSON)</label>
        <textarea
          id="fieldMappings"
          v-model="form.fieldMappings"
          class="rule-editor__code-area"
          rows="6"
          placeholder='{"title": ".headline", "body": ".content"}'
          spellcheck="false"
        ></textarea>
      </div>

      <div class="rule-editor__field">
        <label class="rule-editor__label" for="notes">Version Notes</label>
        <input
          id="notes"
          v-model="form.notes"
          type="text"
          class="rule-editor__input"
          placeholder="Describe changes in this version..."
        />
      </div>

      <div class="rule-editor__actions">
        <button type="button" class="rule-editor__btn rule-editor__btn--secondary" @click="emit('cancel')">Cancel</button>
        <button type="button" class="rule-editor__btn rule-editor__btn--primary" @click="handleSave" :disabled="saving">
          {{ saving ? 'Saving...' : 'Save Version' }}
        </button>
      </div>
    </div>

    <aside class="rule-editor__sidebar">
      <h4 class="rule-editor__sidebar-title">Version History</h4>
      <div v-if="versions.length === 0" class="rule-editor__no-versions">No versions yet.</div>
      <ul class="rule-editor__version-list">
        <li
          v-for="version in versions"
          :key="version.id"
          class="rule-editor__version-item"
          :class="{ 'rule-editor__version-item--active': version.isActive }"
        >
          <div class="rule-editor__version-header">
            <span class="rule-editor__version-number">v{{ version.versionNumber }}</span>
            <span v-if="version.isActive" class="rule-editor__version-badge">Active</span>
          </div>
          <p class="rule-editor__version-notes">{{ version.notes || 'No notes' }}</p>
          <p class="rule-editor__version-date">{{ formatDate(version.createdAt) }}</p>
          <div class="rule-editor__version-actions">
            <button
              type="button"
              class="rule-editor__version-btn"
              @click="emit('revert', version.id)"
            >
              Revert to this version
            </button>
          </div>
        </li>
      </ul>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CrawlRuleVersion } from '@/types/crawl'

const props = defineProps<{
  versions: CrawlRuleVersion[]
  currentVersion?: CrawlRuleVersion | null
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  save: [data: { extractionMethod: string; ruleDefinition: string; fieldMappings: string; notes: string }]
  revert: [versionId: number]
}>()

const form = reactive({
  extractionMethod: 'CSS_SELECTOR',
  ruleDefinition: '',
  fieldMappings: '',
  notes: '',
})

watch(() => props.currentVersion, (v) => {
  if (v) {
    form.extractionMethod = v.extractionMethod
    form.ruleDefinition = v.ruleDefinition
    form.fieldMappings = v.fieldMappings
    form.notes = ''
  }
}, { immediate: true })

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString()
}

function handleSave() {
  emit('save', { ...form })
}
</script>

<style scoped>
.rule-editor {
  display: flex;
  gap: 24px;
}

.rule-editor__main {
  flex: 1;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.rule-editor__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.rule-editor__field {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rule-editor__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.rule-editor__select,
.rule-editor__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
}

.rule-editor__select:focus,
.rule-editor__input:focus {
  border-color: #3b82f6;
}

.rule-editor__code-area {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.82rem;
  resize: vertical;
  outline: none;
  background: #f8fafc;
  line-height: 1.6;
}

.rule-editor__code-area:focus {
  border-color: #3b82f6;
  background: #fff;
}

.rule-editor__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.rule-editor__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.rule-editor__btn--secondary {
  background: #e2e8f0;
  color: #475569;
}

.rule-editor__btn--primary {
  background: #3b82f6;
  color: #fff;
}

.rule-editor__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.rule-editor__sidebar {
  width: 280px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-height: 600px;
  overflow-y: auto;
}

.rule-editor__sidebar-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.rule-editor__no-versions {
  font-size: 0.85rem;
  color: #94a3b8;
}

.rule-editor__version-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-editor__version-item {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.rule-editor__version-item--active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.rule-editor__version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.rule-editor__version-number {
  font-weight: 700;
  font-size: 0.85rem;
  color: #1e293b;
}

.rule-editor__version-badge {
  font-size: 0.65rem;
  background: #3b82f6;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.rule-editor__version-notes {
  font-size: 0.78rem;
  color: #64748b;
  margin-bottom: 4px;
}

.rule-editor__version-date {
  font-size: 0.72rem;
  color: #94a3b8;
}

.rule-editor__version-actions {
  margin-top: 6px;
}

.rule-editor__version-btn {
  padding: 3px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.72rem;
  cursor: pointer;
  color: #3b82f6;
}

.rule-editor__version-btn:hover {
  background: #eff6ff;
}
</style>
