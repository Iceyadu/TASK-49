<template>
  <div class="qb-editor">
    <div class="qb-editor__toolbar">
      <SearchFilterBar
        v-model:searchQuery="searchQuery"
        placeholder="Search questions..."
        :filters="questionFilters"
        v-model:filterValues="filterValues"
      >
        <template #actions>
          <button type="button" class="qb-editor__add-btn" @click="emit('addQuestion')">
            + Add Question
          </button>
        </template>
      </SearchFilterBar>
    </div>

    <LoadingSpinner v-if="loading" message="Loading questions..." />

    <div v-else-if="filteredQuestions.length === 0">
      <EmptyState title="No questions found" description="Add your first question to get started." />
    </div>

    <ul v-else class="qb-editor__list">
      <li
        v-for="question in filteredQuestions"
        :key="question.id"
        class="qb-editor__item"
      >
        <div class="qb-editor__item-main">
          <div class="qb-editor__item-header">
            <span class="qb-editor__item-type">{{ question.questionType }}</span>
            <span class="qb-editor__item-difficulty">
              <span
                v-for="n in 5"
                :key="n"
                class="qb-editor__star"
                :class="{ 'qb-editor__star--filled': n <= question.difficultyLevel }"
              >&#9733;</span>
            </span>
            <span class="qb-editor__item-points">{{ question.points }} pts</span>
          </div>
          <p class="qb-editor__item-text">{{ question.questionText }}</p>
          <div class="qb-editor__item-tags">
            <span v-for="tag in question.knowledgeTags" :key="tag.id" class="qb-editor__tag">
              {{ tag.name }}
            </span>
          </div>
        </div>
        <div class="qb-editor__item-actions">
          <button type="button" class="qb-editor__action-btn" @click="emit('editQuestion', question)">Edit</button>
          <button type="button" class="qb-editor__action-btn qb-editor__action-btn--danger" @click="emit('deleteQuestion', question.id)">Delete</button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Question } from '@/types/quiz'
import SearchFilterBar from '@/components/common/SearchFilterBar.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps<{
  questions: Question[]
  loading: boolean
}>()

const emit = defineEmits<{
  addQuestion: []
  editQuestion: [question: Question]
  deleteQuestion: [id: number]
}>()

const searchQuery = ref('')
const filterValues = ref<Record<string, string>>({})

const questionFilters = [
  {
    key: 'difficulty',
    label: 'Difficulty',
    options: [
      { value: '1', label: '1 - Easy' },
      { value: '2', label: '2' },
      { value: '3', label: '3 - Medium' },
      { value: '4', label: '4' },
      { value: '5', label: '5 - Hard' },
    ],
  },
  {
    key: 'type',
    label: 'Type',
    options: [
      { value: 'MULTIPLE_CHOICE', label: 'Multiple Choice' },
      { value: 'SHORT_ANSWER', label: 'Short Answer' },
      { value: 'ESSAY', label: 'Essay' },
      { value: 'TRUE_FALSE', label: 'True/False' },
    ],
  },
]

const filteredQuestions = computed(() => {
  let result = props.questions
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(qn =>
      qn.questionText.toLowerCase().includes(q) ||
      qn.knowledgeTags.some(t => t.name.toLowerCase().includes(q))
    )
  }
  if (filterValues.value.difficulty) {
    result = result.filter(qn => qn.difficultyLevel === Number(filterValues.value.difficulty))
  }
  if (filterValues.value.type) {
    result = result.filter(qn => qn.questionType === filterValues.value.type)
  }
  return result
})
</script>

<style scoped>
.qb-editor__toolbar {
  margin-bottom: 16px;
}

.qb-editor__add-btn {
  padding: 8px 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.qb-editor__add-btn:hover {
  background: #2563eb;
}

.qb-editor__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qb-editor__item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 18px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.qb-editor__item-main {
  flex: 1;
  min-width: 0;
}

.qb-editor__item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.qb-editor__item-type {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 8px;
  border-radius: 10px;
}

.qb-editor__item-difficulty {
  font-size: 0.85rem;
}

.qb-editor__star {
  color: #d1d5db;
}

.qb-editor__star--filled {
  color: #f59e0b;
}

.qb-editor__item-points {
  font-size: 0.78rem;
  color: #64748b;
}

.qb-editor__item-text {
  font-size: 0.88rem;
  color: #334155;
  line-height: 1.4;
  margin-bottom: 8px;
}

.qb-editor__item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.qb-editor__tag {
  font-size: 0.68rem;
  background: #f1f5f9;
  color: #475569;
  padding: 2px 8px;
  border-radius: 10px;
}

.qb-editor__item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}

.qb-editor__action-btn {
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  font-size: 0.78rem;
  cursor: pointer;
  color: #374151;
}

.qb-editor__action-btn:hover {
  background: #f1f5f9;
}

.qb-editor__action-btn--danger {
  color: #dc2626;
  border-color: #fca5a5;
}

.qb-editor__action-btn--danger:hover {
  background: #fef2f2;
}
</style>
