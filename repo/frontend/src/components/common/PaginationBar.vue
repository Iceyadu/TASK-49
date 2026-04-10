<template>
  <div class="pagination-bar">
    <div class="pagination-bar__size">
      <label class="pagination-bar__size-label" for="page-size">Rows per page:</label>
      <select
        id="page-size"
        class="pagination-bar__size-select"
        :value="pageSize"
        @change="onPageSizeChange"
      >
        <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
      </select>
    </div>

    <div class="pagination-bar__info">
      {{ startItem }}--{{ endItem }} of {{ totalItems }}
    </div>

    <div class="pagination-bar__controls">
      <button
        type="button"
        class="pagination-bar__btn"
        :disabled="currentPage <= 1"
        @click="goToPage(currentPage - 1)"
      >
        Prev
      </button>

      <button
        v-for="page in visiblePages"
        :key="page"
        type="button"
        class="pagination-bar__btn pagination-bar__page-btn"
        :class="{ 'pagination-bar__page-btn--active': page === currentPage }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>

      <button
        type="button"
        class="pagination-bar__btn"
        :disabled="currentPage >= totalPages"
        @click="goToPage(currentPage + 1)"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  currentPage: number
  pageSize: number
  totalItems: number
  pageSizeOptions?: number[]
}>(), {
  pageSizeOptions: () => [10, 25, 50, 100],
})

const emit = defineEmits<{
  'update:currentPage': [page: number]
  'update:pageSize': [size: number]
}>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.totalItems / props.pageSize)))

const startItem = computed(() => props.totalItems === 0 ? 0 : (props.currentPage - 1) * props.pageSize + 1)
const endItem = computed(() => Math.min(props.currentPage * props.pageSize, props.totalItems))

const visiblePages = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const current = props.currentPage
  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    emit('update:currentPage', page)
  }
}

function onPageSizeChange(event: Event) {
  const target = event.target as HTMLSelectElement
  emit('update:pageSize', Number(target.value))
  emit('update:currentPage', 1)
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  gap: 16px;
  flex-wrap: wrap;
}

.pagination-bar__size {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-bar__size-label {
  font-size: 0.85rem;
  color: #64748b;
}

.pagination-bar__size-select {
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.85rem;
  background: #fff;
}

.pagination-bar__info {
  font-size: 0.85rem;
  color: #64748b;
}

.pagination-bar__controls {
  display: flex;
  gap: 4px;
}

.pagination-bar__btn {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.8rem;
  cursor: pointer;
  color: #374151;
  transition: background 0.15s, border-color 0.15s;
}

.pagination-bar__btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.pagination-bar__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-bar__page-btn--active {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}

.pagination-bar__page-btn--active:hover {
  background: #2563eb;
}
</style>
