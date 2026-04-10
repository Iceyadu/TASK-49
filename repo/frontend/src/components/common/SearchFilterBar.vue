<template>
  <div class="search-filter-bar">
    <div class="search-filter-bar__search">
      <svg class="search-filter-bar__search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        type="text"
        class="search-filter-bar__input"
        :placeholder="placeholder"
        :value="searchQuery"
        @input="onSearchInput"
      />
      <button
        v-if="searchQuery"
        type="button"
        class="search-filter-bar__clear"
        @click="clearSearch"
      >
        &times;
      </button>
    </div>

    <div v-if="filters.length > 0" class="search-filter-bar__filters">
      <div v-for="filter in filters" :key="filter.key" class="search-filter-bar__filter">
        <label class="search-filter-bar__filter-label">{{ filter.label }}</label>
        <select
          class="search-filter-bar__filter-select"
          :value="filterValues[filter.key] || ''"
          @change="onFilterChange(filter.key, $event)"
        >
          <option value="">All</option>
          <option v-for="opt in filter.options" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>
    </div>

    <slot name="actions" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

export interface FilterOption {
  value: string
  label: string
}

export interface FilterConfig {
  key: string
  label: string
  options: FilterOption[]
}

const props = withDefaults(defineProps<{
  searchQuery?: string
  placeholder?: string
  filters?: FilterConfig[]
  filterValues?: Record<string, string>
}>(), {
  searchQuery: '',
  placeholder: 'Search...',
  filters: () => [],
  filterValues: () => ({}),
})

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  'update:filterValues': [values: Record<string, string>]
}>()

function onSearchInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:searchQuery', target.value)
}

function clearSearch() {
  emit('update:searchQuery', '')
}

function onFilterChange(key: string, event: Event) {
  const target = event.target as HTMLSelectElement
  const newValues = { ...props.filterValues, [key]: target.value }
  emit('update:filterValues', newValues)
}
</script>

<style scoped>
.search-filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 12px 0;
}

.search-filter-bar__search {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.search-filter-bar__search-icon {
  position: absolute;
  left: 10px;
  color: #94a3b8;
  pointer-events: none;
}

.search-filter-bar__input {
  width: 100%;
  padding: 8px 32px 8px 34px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.15s;
}

.search-filter-bar__input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-filter-bar__clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #94a3b8;
  cursor: pointer;
  padding: 0 4px;
}

.search-filter-bar__clear:hover {
  color: #475569;
}

.search-filter-bar__filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-filter-bar__filter {
  display: flex;
  align-items: center;
  gap: 6px;
}

.search-filter-bar__filter-label {
  font-size: 0.8rem;
  color: #64748b;
  white-space: nowrap;
}

.search-filter-bar__filter-select {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.85rem;
  background: #fff;
  outline: none;
}

.search-filter-bar__filter-select:focus {
  border-color: #3b82f6;
}
</style>
