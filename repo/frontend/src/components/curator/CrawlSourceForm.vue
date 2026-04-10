<template>
  <form class="crawl-source-form" @submit.prevent="handleSubmit">
    <h3 class="crawl-source-form__title">{{ isEdit ? 'Edit Source Profile' : 'New Source Profile' }}</h3>

    <div class="crawl-source-form__field">
      <label class="crawl-source-form__label" for="sourceName">Source Name</label>
      <input
        id="sourceName"
        v-model="form.name"
        type="text"
        class="crawl-source-form__input"
        placeholder="e.g. University Portal"
        required
      />
    </div>

    <div class="crawl-source-form__field">
      <label class="crawl-source-form__label" for="baseUrl">Base URL</label>
      <input
        id="baseUrl"
        v-model="form.baseUrl"
        type="url"
        class="crawl-source-form__input"
        placeholder="https://example.com"
        required
      />
    </div>

    <div class="crawl-source-form__field">
      <label class="crawl-source-form__label" for="description">Description</label>
      <textarea
        id="description"
        v-model="form.description"
        class="crawl-source-form__textarea"
        rows="3"
        placeholder="Describe what this source provides..."
      ></textarea>
    </div>

    <div class="crawl-source-form__field">
      <label class="crawl-source-form__label" for="rateLimit">Rate Limit (requests/min)</label>
      <input
        id="rateLimit"
        v-model.number="form.rateLimitPerMinute"
        type="number"
        class="crawl-source-form__input"
        min="1"
        max="600"
        required
      />
    </div>

    <div class="crawl-source-form__field crawl-source-form__field--row">
      <label class="crawl-source-form__toggle-label">
        <input
          type="checkbox"
          v-model="form.requiresAuth"
          class="crawl-source-form__checkbox"
        />
        Requires Authentication
      </label>
    </div>

    <template v-if="form.requiresAuth">
      <div class="crawl-source-form__field">
        <label class="crawl-source-form__label" for="authUsername">Auth Username</label>
        <input
          id="authUsername"
          v-model="authCredentials.username"
          type="text"
          class="crawl-source-form__input"
          placeholder="Username or API key name"
        />
      </div>

      <div class="crawl-source-form__field">
        <label class="crawl-source-form__label" for="authPassword">Auth Password / Token</label>
        <div class="crawl-source-form__masked-input">
          <input
            id="authPassword"
            v-model="authCredentials.password"
            :type="showPassword ? 'text' : 'password'"
            class="crawl-source-form__input"
            placeholder="Enter credentials"
          />
          <button
            type="button"
            class="crawl-source-form__toggle-pwd"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? 'Hide' : 'Show' }}
          </button>
        </div>
      </div>
    </template>

    <div class="crawl-source-form__actions">
      <button type="button" class="crawl-source-form__btn crawl-source-form__btn--cancel" @click="emit('cancel')">Cancel</button>
      <button type="submit" class="crawl-source-form__btn crawl-source-form__btn--save" :disabled="saving">
        {{ saving ? 'Saving...' : (isEdit ? 'Update Source' : 'Create Source') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { CrawlSource } from '@/types/crawl'

const props = defineProps<{
  source?: CrawlSource | null
  saving: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [data: {
    name: string
    baseUrl: string
    description: string
    rateLimitPerMinute: number
    requiresAuth: boolean
    authUsername?: string
    authPassword?: string
  }]
}>()

const isEdit = ref(false)
const showPassword = ref(false)

const form = reactive({
  name: '',
  baseUrl: '',
  description: '',
  rateLimitPerMinute: 60,
  requiresAuth: false,
})

const authCredentials = reactive({
  username: '',
  password: '',
})

watch(() => props.source, (src) => {
  if (src) {
    isEdit.value = true
    form.name = src.name
    form.baseUrl = src.baseUrl
    form.description = src.description
    form.rateLimitPerMinute = src.rateLimitPerMinute
    form.requiresAuth = src.requiresAuth
  } else {
    isEdit.value = false
    form.name = ''
    form.baseUrl = ''
    form.description = ''
    form.rateLimitPerMinute = 60
    form.requiresAuth = false
  }
}, { immediate: true })

function handleSubmit() {
  emit('submit', {
    ...form,
    authUsername: form.requiresAuth ? authCredentials.username : undefined,
    authPassword: form.requiresAuth ? authCredentials.password : undefined,
  })
}
</script>

<style scoped>
.crawl-source-form {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  max-width: 560px;
}

.crawl-source-form__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.crawl-source-form__field {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.crawl-source-form__field--row {
  flex-direction: row;
  align-items: center;
}

.crawl-source-form__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #475569;
}

.crawl-source-form__input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
  width: 100%;
}

.crawl-source-form__input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.crawl-source-form__textarea {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  resize: vertical;
  outline: none;
}

.crawl-source-form__textarea:focus {
  border-color: #3b82f6;
}

.crawl-source-form__toggle-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  color: #334155;
  cursor: pointer;
}

.crawl-source-form__checkbox {
  accent-color: #3b82f6;
}

.crawl-source-form__masked-input {
  position: relative;
  display: flex;
  align-items: center;
}

.crawl-source-form__toggle-pwd {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: #3b82f6;
  font-size: 0.78rem;
  cursor: pointer;
  font-weight: 500;
}

.crawl-source-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
}

.crawl-source-form__btn {
  padding: 8px 18px;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}

.crawl-source-form__btn--cancel {
  background: #e2e8f0;
  color: #475569;
}

.crawl-source-form__btn--save {
  background: #3b82f6;
  color: #fff;
}

.crawl-source-form__btn--save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
