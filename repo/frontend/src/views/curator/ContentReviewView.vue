<template>
  <div class="content-review">
    <h1>Content Review</h1>
    <p class="subtitle">Review standardized content and publish to catalog</p>

    <div class="content-layout">
      <aside class="content-list">
        <h3>Records</h3>
        <button type="button" class="refresh-btn" @click="loadContent">Refresh</button>
        <ul>
          <li
            v-for="item in contentItems"
            :key="item.id"
            :class="{ active: item.id === selectedContentId }"
            @click="selectContent(item.id)"
          >
            <strong>{{ item.title || `Record #${item.id}` }}</strong>
            <span>{{ item.isPublished ? 'Published' : 'Draft' }}</span>
          </li>
        </ul>
      </aside>

      <section class="preview-pane">
        <ContentPreview :content="selectedContent" @publish="onPublish" />
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ContentPreview from '@/components/curator/ContentPreview.vue'
import { getContent, getContentById, publishContent } from '@/api/content'
import type { ContentRecord } from '@/types/content'

const contentItems = ref<ContentRecord[]>([])
const selectedContentId = ref<number | null>(null)
const selectedContent = ref<ContentRecord | null>(null)

async function loadContent() {
  const response = await getContent(0, 50)
  contentItems.value = response.content ?? []

  if (contentItems.value.length > 0 && !selectedContentId.value) {
    await selectContent(contentItems.value[0].id)
  }
}

async function selectContent(id: number) {
  selectedContentId.value = id
  selectedContent.value = await getContentById(id)
}

async function onPublish(id: number) {
  await publishContent(id)
  await selectContent(id)
  await loadContent()
}

onMounted(loadContent)
</script>
<style scoped>
.content-review { padding: 24px; }
.content-review h1 { color: #1a365d; margin-bottom: 4px; }
.subtitle { color: #718096; margin-bottom: 24px; }
.content-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
}
.content-list {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.content-list h3 {
  margin: 0 0 8px 0;
}
.refresh-btn {
  margin-bottom: 10px;
}
.content-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.content-list li {
  cursor: pointer;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.content-list li.active {
  border-color: #3b82f6;
  background: #eff6ff;
}
.content-list span {
  color: #64748b;
  font-size: 0.82rem;
}
</style>
