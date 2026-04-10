<template>
  <div class="grading-queue">
    <div class="grading-queue__header">
      <h3 class="grading-queue__title">Grading Queue</h3>
      <SearchFilterBar
        v-model:searchQuery="searchQuery"
        placeholder="Search submissions..."
        :filters="queueFilters"
        v-model:filterValues="filterValues"
      />
    </div>

    <LoadingSpinner v-if="loading" message="Loading grading queue..." />

    <table v-else-if="filteredItems.length > 0" class="grading-queue__table">
      <thead>
        <tr>
          <th>Question</th>
          <th>Student</th>
          <th>Status</th>
          <th>Assigned To</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in filteredItems" :key="item.id">
          <td class="grading-queue__question">
            <span class="grading-queue__question-text">{{ truncate(item.questionText, 80) }}</span>
          </td>
          <td>{{ item.studentName || `Student #${item.studentId}` }}</td>
          <td>
            <span class="grading-queue__status" :class="statusClass(item.status)">
              {{ item.status }}
            </span>
          </td>
          <td>{{ item.assignedToName || 'Unassigned' }}</td>
          <td class="grading-queue__actions">
            <button
              v-if="item.status === 'PENDING' || item.status === 'SUBMITTED'"
              type="button"
              class="grading-queue__btn grading-queue__btn--claim"
              @click="emit('claim', item.id)"
            >
              Claim
            </button>
            <button
              type="button"
              class="grading-queue__btn grading-queue__btn--grade"
              @click="emit('grade', item.id)"
            >
              Grade
            </button>
            <button
              v-if="item.status === 'IN_PROGRESS'"
              type="button"
              class="grading-queue__btn grading-queue__btn--release"
              @click="emit('release', item.id)"
            >
              Release
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <EmptyState v-else title="Queue is empty" description="No submissions are awaiting grading." />

    <PaginationBar
      v-if="filteredItems.length > 0"
      v-model:currentPage="currentPage"
      v-model:pageSize="pageSize"
      :totalItems="totalItems"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SearchFilterBar from '@/components/common/SearchFilterBar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

export interface GradingQueueItem {
  id: number
  questionText: string
  studentId: number
  studentName?: string
  status: string
  assignedToId?: number
  assignedToName?: string
}

const props = defineProps<{
  items: GradingQueueItem[]
  loading: boolean
  totalItems: number
}>()

const emit = defineEmits<{
  claim: [id: number]
  grade: [id: number]
  release: [id: number]
}>()

const searchQuery = ref('')
const filterValues = ref<Record<string, string>>({})
const currentPage = ref(1)
const pageSize = ref(25)

const queueFilters = [
  {
    key: 'status',
    label: 'Status',
    options: [
      { value: 'PENDING', label: 'Pending' },
      { value: 'SUBMITTED', label: 'Submitted' },
      { value: 'IN_PROGRESS', label: 'In Progress' },
      { value: 'GRADED', label: 'Graded' },
    ],
  },
]

const filteredItems = computed(() => {
  let result = props.items
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(
      i => i.questionText.toLowerCase().includes(q) ||
           (i.studentName || '').toLowerCase().includes(q)
    )
  }
  if (filterValues.value.status) {
    result = result.filter(i => i.status === filterValues.value.status)
  }
  return result
})

function statusClass(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'grading-queue__status--pending',
    SUBMITTED: 'grading-queue__status--submitted',
    IN_PROGRESS: 'grading-queue__status--in-progress',
    GRADED: 'grading-queue__status--graded',
  }
  return map[status] || ''
}

function truncate(text: string, maxLen: number): string {
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}
</script>

<style scoped>
.grading-queue__header {
  margin-bottom: 16px;
}

.grading-queue__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.grading-queue__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.grading-queue__table th {
  text-align: left;
  padding: 12px 14px;
  background: #f8fafc;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.grading-queue__table td {
  padding: 10px 14px;
  font-size: 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.grading-queue__question-text {
  font-size: 0.82rem;
  color: #475569;
}

.grading-queue__status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.grading-queue__status--pending { background: #f1f5f9; color: #64748b; }
.grading-queue__status--submitted { background: #fef9c3; color: #ca8a04; }
.grading-queue__status--in-progress { background: #dbeafe; color: #2563eb; }
.grading-queue__status--graded { background: #dcfce7; color: #16a34a; }

.grading-queue__actions {
  display: flex;
  gap: 6px;
}

.grading-queue__btn {
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.75rem;
  cursor: pointer;
}

.grading-queue__btn--claim {
  color: #2563eb;
  border-color: #93c5fd;
}

.grading-queue__btn--claim:hover {
  background: #eff6ff;
}

.grading-queue__btn--grade {
  color: #16a34a;
  border-color: #86efac;
}

.grading-queue__btn--grade:hover {
  background: #f0fdf4;
}

.grading-queue__btn--release {
  color: #ca8a04;
  border-color: #fde68a;
}

.grading-queue__btn--release:hover {
  background: #fefce8;
}
</style>
