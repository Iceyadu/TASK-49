<template>
  <div class="content-preview">
    <header class="content-preview__header">
      <h3 class="content-preview__title">Content Preview</h3>
      <div class="content-preview__header-actions">
        <span
          class="content-preview__status"
          :class="content?.isPublished ? 'content-preview__status--published' : 'content-preview__status--draft'"
        >
          {{ content?.isPublished ? 'Published' : 'Draft' }}
        </span>
        <button
          v-if="content && !content.isPublished"
          type="button"
          class="content-preview__publish-btn"
          @click="emit('publish', content.id)"
        >
          Publish
        </button>
      </div>
    </header>

    <div v-if="!content" class="content-preview__empty">
      <EmptyState title="No content selected" description="Select a content record to preview." />
    </div>

    <div v-else class="content-preview__body">
      <section class="content-preview__section">
        <h2 class="content-preview__content-title">{{ content.title }}</h2>
        <p class="content-preview__description">{{ content.description }}</p>
      </section>

      <section class="content-preview__section">
        <h4 class="content-preview__section-heading">Body</h4>
        <div class="content-preview__body-text">{{ content.bodyText }}</div>
      </section>

      <section class="content-preview__section">
        <h4 class="content-preview__section-heading">Metadata</h4>
        <dl class="content-preview__meta-list">
          <div class="content-preview__meta-item">
            <dt>Content Type</dt>
            <dd>{{ content.contentType }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Language</dt>
            <dd>{{ content.detectedLanguage }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Timestamp</dt>
            <dd>{{ content.standardizedTimestamp }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Timezone</dt>
            <dd>{{ content.timezoneId }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Address</dt>
            <dd>{{ content.normalizedAddress || '--' }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Price</dt>
            <dd>{{ content.price != null ? `$${content.price.toFixed(2)}` : '--' }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Popularity Score</dt>
            <dd>{{ content.popularityScore }}</dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Source URL</dt>
            <dd><a :href="content.sourceUrl" target="_blank" rel="noopener">{{ content.sourceUrl }}</a></dd>
          </div>
          <div class="content-preview__meta-item">
            <dt>Availability</dt>
            <dd>{{ content.availabilityStart }} -- {{ content.availabilityEnd }}</dd>
          </div>
        </dl>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ContentRecord } from '@/types/content'
import EmptyState from '@/components/common/EmptyState.vue'

const props = defineProps<{
  content: ContentRecord | null
}>()

const emit = defineEmits<{
  publish: [id: number]
}>()
</script>

<style scoped>
.content-preview {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.content-preview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.content-preview__title {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.content-preview__header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.content-preview__status {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}

.content-preview__status--published {
  background: #dcfce7;
  color: #16a34a;
}

.content-preview__status--draft {
  background: #fef9c3;
  color: #ca8a04;
}

.content-preview__publish-btn {
  padding: 6px 16px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
}

.content-preview__publish-btn:hover {
  background: #15803d;
}

.content-preview__empty {
  padding: 24px;
}

.content-preview__body {
  padding: 24px;
}

.content-preview__section {
  margin-bottom: 24px;
}

.content-preview__content-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.content-preview__description {
  font-size: 0.95rem;
  color: #475569;
  line-height: 1.5;
}

.content-preview__section-heading {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 6px;
}

.content-preview__body-text {
  font-size: 0.9rem;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.content-preview__meta-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.content-preview__meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.content-preview__meta-item dt {
  font-size: 0.72rem;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.content-preview__meta-item dd {
  font-size: 0.85rem;
  color: #334155;
  margin: 0;
}

.content-preview__meta-item a {
  color: #3b82f6;
  text-decoration: none;
  word-break: break-all;
}

.content-preview__meta-item a:hover {
  text-decoration: underline;
}
</style>
