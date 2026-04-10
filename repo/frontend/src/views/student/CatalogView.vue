<template>
  <div class="catalog-view">
    <h1>Learning Catalog</h1>
    <CatalogBrowser
      :items="items"
      :loading="loading"
      :total-items="totalItems"
      :current-page="currentPage"
      :page-size="pageSize"
      @update:current-page="onPageChange"
      @update:page-size="onPageSizeChange"
      @search="onSearch"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CatalogBrowser from '@/components/student/CatalogBrowser.vue'
import { searchCatalog } from '@/api/catalog'
import type { ContentRecord } from '@/types/content'
import type { CatalogSearchParams } from '@/types/catalog'

const items = ref<ContentRecord[]>([])
const loading = ref(false)
const totalItems = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const lastSearch = ref<CatalogSearchParams>({})

async function fetchPage() {
  loading.value = true
  try {
    const res = await searchCatalog({
      ...lastSearch.value,
      page: currentPage.value - 1,
      size: pageSize.value,
    })
    items.value = res.content
    totalItems.value = res.totalElements
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  currentPage.value = page
  fetchPage()
}

function onPageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchPage()
}

function onSearch(params: Record<string, unknown>) {
  lastSearch.value = params as CatalogSearchParams
  currentPage.value = 1
  fetchPage()
}

onMounted(fetchPage)
</script>
<style scoped>
.catalog-view { padding: 24px; }
.catalog-view h1 { color: #1a365d; margin-bottom: 24px; }
</style>
