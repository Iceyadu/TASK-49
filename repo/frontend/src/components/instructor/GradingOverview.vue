<template>
  <div class="grading-overview">
    <h3 class="grading-overview__title">Grading Overview</h3>

    <div class="grading-overview__stats">
      <div class="grading-overview__stat-card">
        <span class="grading-overview__stat-value">{{ stats.totalSubmissions }}</span>
        <span class="grading-overview__stat-label">Total Submissions</span>
      </div>
      <div class="grading-overview__stat-card grading-overview__stat-card--graded">
        <span class="grading-overview__stat-value">{{ stats.graded }}</span>
        <span class="grading-overview__stat-label">Graded</span>
      </div>
      <div class="grading-overview__stat-card grading-overview__stat-card--pending">
        <span class="grading-overview__stat-value">{{ stats.pending }}</span>
        <span class="grading-overview__stat-label">Pending</span>
      </div>
      <div class="grading-overview__stat-card grading-overview__stat-card--in-progress">
        <span class="grading-overview__stat-value">{{ stats.inProgress }}</span>
        <span class="grading-overview__stat-label">In Progress</span>
      </div>
    </div>

    <LoadingSpinner v-if="loading" message="Loading submissions..." />

    <table v-else-if="submissions.length > 0" class="grading-overview__table">
      <thead>
        <tr>
          <th>Student</th>
          <th>Attempt</th>
          <th>Submitted At</th>
          <th>Score</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="sub in submissions" :key="sub.id">
          <td>Student #{{ sub.studentId }}</td>
          <td>{{ sub.attemptNumber }}</td>
          <td class="grading-overview__date">{{ formatDate(sub.submittedAt) }}</td>
          <td>
            <template v-if="sub.totalScore != null">
              {{ sub.totalScore }} / {{ sub.maxScore }}
              <span class="grading-overview__pct">({{ sub.percentage }}%)</span>
            </template>
            <template v-else>--</template>
          </td>
          <td>
            <span class="grading-overview__status" :class="statusClass(sub.status)">
              {{ sub.status }}
            </span>
          </td>
          <td>
            <button type="button" class="grading-overview__action-btn" @click="emit('viewSubmission', sub.id)">
              View
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <EmptyState v-else title="No submissions yet" description="Submissions will appear here once students submit their work." />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Submission } from '@/types/submission'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps<{
  submissions: Submission[]
  loading: boolean
}>()

const emit = defineEmits<{
  viewSubmission: [id: number]
}>()

const stats = computed(() => {
  const total = props.submissions.length
  const graded = props.submissions.filter(s => s.status === 'GRADED').length
  const pending = props.submissions.filter(s => s.status === 'SUBMITTED').length
  const inProgress = props.submissions.filter(s => s.status === 'GRADING').length
  return { totalSubmissions: total, graded, pending, inProgress }
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleString()
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    SUBMITTED: 'grading-overview__status--submitted',
    GRADING: 'grading-overview__status--grading',
    GRADED: 'grading-overview__status--graded',
    IN_PROGRESS: 'grading-overview__status--grading',
  }
  return map[status] || ''
}
</script>

<style scoped>
.grading-overview__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.grading-overview__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.grading-overview__stat-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.grading-overview__stat-card--graded {
  border-left: 4px solid #16a34a;
}

.grading-overview__stat-card--pending {
  border-left: 4px solid #f59e0b;
}

.grading-overview__stat-card--in-progress {
  border-left: 4px solid #3b82f6;
}

.grading-overview__stat-value {
  display: block;
  font-size: 1.8rem;
  font-weight: 700;
  color: #1e293b;
}

.grading-overview__stat-label {
  font-size: 0.78rem;
  color: #64748b;
}

.grading-overview__table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.grading-overview__table th {
  text-align: left;
  padding: 12px 14px;
  background: #f8fafc;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}

.grading-overview__table td {
  padding: 10px 14px;
  font-size: 0.85rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.grading-overview__date {
  font-size: 0.8rem;
  color: #64748b;
}

.grading-overview__pct {
  font-size: 0.78rem;
  color: #64748b;
}

.grading-overview__status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.grading-overview__status--submitted { background: #fef9c3; color: #ca8a04; }
.grading-overview__status--grading { background: #dbeafe; color: #2563eb; }
.grading-overview__status--graded { background: #dcfce7; color: #16a34a; }

.grading-overview__action-btn {
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.78rem;
  cursor: pointer;
  color: #3b82f6;
}

.grading-overview__action-btn:hover {
  background: #eff6ff;
}
</style>
