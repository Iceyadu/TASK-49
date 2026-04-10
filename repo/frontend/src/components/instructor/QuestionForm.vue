<template>
  <form class="question-form" @submit.prevent="handleSubmit">
    <h3 class="question-form__title">{{ isEdit ? 'Edit Question' : 'Create Question' }}</h3>

    <div class="question-form__field">
      <label class="question-form__label" for="qType">Question Type</label>
      <select id="qType" v-model="form.questionType" class="question-form__select" required>
        <option value="MULTIPLE_CHOICE">Multiple Choice</option>
        <option value="SHORT_ANSWER">Short Answer</option>
        <option value="ESSAY">Essay</option>
        <option value="TRUE_FALSE">True / False</option>
      </select>
    </div>

    <div class="question-form__field">
      <label class="question-form__label">Difficulty Level</label>
      <div class="question-form__difficulty">
        <button
          v-for="n in 5"
          :key="n"
          type="button"
          class="question-form__diff-btn"
          :class="{ 'question-form__diff-btn--active': form.difficultyLevel >= n }"
          @click="form.difficultyLevel = n"
        >
          {{ n }}
        </button>
      </div>
    </div>

    <div class="question-form__field">
      <label class="question-form__label" for="qText">Question Text</label>
      <textarea
        id="qText"
        v-model="form.questionText"
        class="question-form__textarea"
        rows="4"
        required
        placeholder="Enter the question..."
      ></textarea>
    </div>

    <div v-if="form.questionType === 'MULTIPLE_CHOICE'" class="question-form__field">
      <label class="question-form__label">Options</label>
      <div
        v-for="(option, index) in options"
        :key="index"
        class="question-form__option-row"
      >
        <input
          type="radio"
          name="correctOption"
          :value="index"
          v-model="correctOptionIndex"
          class="question-form__option-radio"
        />
        <input
          v-model="options[index]"
          type="text"
          class="question-form__option-input"
          :placeholder="`Option ${String.fromCharCode(65 + index)}`"
          required
        />
        <button
          v-if="options.length > 2"
          type="button"
          class="question-form__option-remove"
          @click="removeOption(index)"
        >
          &times;
        </button>
      </div>
      <button type="button" class="question-form__add-option" @click="addOption">+ Add Option</button>
    </div>

    <div v-if="form.questionType === 'TRUE_FALSE'" class="question-form__field">
      <label class="question-form__label">Correct Answer</label>
      <div class="question-form__tf-options">
        <label class="question-form__tf-label">
          <input type="radio" value="true" v-model="form.correctAnswer" /> True
        </label>
        <label class="question-form__tf-label">
          <input type="radio" value="false" v-model="form.correctAnswer" /> False
        </label>
      </div>
    </div>

    <div v-if="form.questionType === 'SHORT_ANSWER'" class="question-form__field">
      <label class="question-form__label" for="correctAnswer">Correct Answer</label>
      <input
        id="correctAnswer"
        v-model="form.correctAnswer"
        type="text"
        class="question-form__input"
        placeholder="Expected answer..."
      />
    </div>

    <div class="question-form__field">
      <label class="question-form__label" for="explanation">Explanation</label>
      <textarea
        id="explanation"
        v-model="form.explanation"
        class="question-form__textarea"
        rows="3"
        placeholder="Explain the correct answer..."
      ></textarea>
    </div>

    <div class="question-form__field">
      <label class="question-form__label" for="points">Points</label>
      <input
        id="points"
        v-model.number="form.points"
        type="number"
        class="question-form__input question-form__input--short"
        min="1"
        required
      />
    </div>

    <div class="question-form__field">
      <label class="question-form__label">Knowledge Tags</label>
      <div class="question-form__tags-select">
        <label
          v-for="tag in availableTags"
          :key="tag.id"
          class="question-form__tag-option"
          :class="{ 'question-form__tag-option--selected': selectedTagIds.includes(tag.id) }"
        >
          <input
            type="checkbox"
            :value="tag.id"
            v-model="selectedTagIds"
            class="question-form__tag-checkbox"
          />
          {{ tag.name }}
        </label>
      </div>
    </div>

    <div class="question-form__actions">
      <button type="button" class="question-form__btn question-form__btn--cancel" @click="emit('cancel')">Cancel</button>
      <button type="submit" class="question-form__btn question-form__btn--save" :disabled="saving">
        {{ saving ? 'Saving...' : (isEdit ? 'Update' : 'Create') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import type { Question, KnowledgeTag } from '@/types/quiz'

const props = defineProps<{
  question?: Question | null
  availableTags: KnowledgeTag[]
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [data: any]
}>()

const isEdit = computed(() => !!props.question)

const form = reactive({
  questionType: 'MULTIPLE_CHOICE',
  difficultyLevel: 3,
  questionText: '',
  correctAnswer: '',
  explanation: '',
  points: 1,
})

const options = ref<string[]>(['', '', '', ''])
const correctOptionIndex = ref(0)
const selectedTagIds = ref<number[]>([])

watch(() => props.question, (q) => {
  if (q) {
    form.questionType = q.questionType
    form.difficultyLevel = q.difficultyLevel
    form.questionText = q.questionText
    form.correctAnswer = q.correctAnswer
    form.explanation = q.explanation
    form.points = q.points
    selectedTagIds.value = q.knowledgeTags.map(t => t.id)
    if (q.questionType === 'MULTIPLE_CHOICE' && q.options) {
      try {
        const parsed = JSON.parse(q.options)
        options.value = parsed.options || ['', '', '', '']
        correctOptionIndex.value = parsed.correctIndex || 0
      } catch {
        options.value = ['', '', '', '']
      }
    }
  }
}, { immediate: true })

function addOption() {
  options.value.push('')
}

function removeOption(index: number) {
  options.value.splice(index, 1)
  if (correctOptionIndex.value >= options.value.length) {
    correctOptionIndex.value = 0
  }
}

function handleSubmit() {
  const data: any = { ...form, tagIds: selectedTagIds.value }
  if (form.questionType === 'MULTIPLE_CHOICE') {
    data.options = JSON.stringify({ options: options.value, correctIndex: correctOptionIndex.value })
    data.correctAnswer = options.value[correctOptionIndex.value]
  }
  emit('submit', data)
}
</script>

<style scoped>
.question-form {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 640px;
}

.question-form__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.question-form__field {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.question-form__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.question-form__select,
.question-form__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
}

.question-form__select:focus,
.question-form__input:focus {
  border-color: #3b82f6;
}

.question-form__input--short {
  max-width: 120px;
}

.question-form__textarea {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  resize: vertical;
  outline: none;
}

.question-form__textarea:focus {
  border-color: #3b82f6;
}

.question-form__difficulty {
  display: flex;
  gap: 6px;
}

.question-form__diff-btn {
  width: 36px;
  height: 36px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  background: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  color: #64748b;
}

.question-form__diff-btn--active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: #fff;
}

.question-form__option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.question-form__option-radio {
  accent-color: #3b82f6;
}

.question-form__option-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.85rem;
  outline: none;
}

.question-form__option-input:focus {
  border-color: #3b82f6;
}

.question-form__option-remove {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 4px;
}

.question-form__add-option {
  background: none;
  border: 1px dashed #d1d5db;
  border-radius: 4px;
  padding: 6px;
  font-size: 0.82rem;
  color: #3b82f6;
  cursor: pointer;
  text-align: center;
}

.question-form__add-option:hover {
  background: #eff6ff;
}

.question-form__tf-options {
  display: flex;
  gap: 24px;
}

.question-form__tf-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  cursor: pointer;
}

.question-form__tags-select {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.question-form__tag-option {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #d1d5db;
  border-radius: 14px;
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.15s;
}

.question-form__tag-option--selected {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #2563eb;
}

.question-form__tag-checkbox {
  display: none;
}

.question-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}

.question-form__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.question-form__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.question-form__btn--save {
  background: #3b82f6;
  color: #fff;
}

.question-form__btn--save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
