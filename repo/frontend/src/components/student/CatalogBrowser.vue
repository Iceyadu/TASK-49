<template>
  <div class="catalog-browser">
    <div class="catalog-browser__controls">
      <div class="catalog-browser__search">
        <input
          v-model="searchQuery"
          type="text"
          class="catalog-browser__search-input"
          placeholder="Search the catalog..."
          @input="onSearch"
        />
      </div>

      <div class="catalog-browser__filters">
        <div class="catalog-browser__keywords">
          <span
            v-for="keyword in activeKeywords"
            :key="keyword"
            class="catalog-browser__keyword-chip"
          >
            {{ keyword }}
            <button type="button" class="catalog-browser__chip-remove" @click="removeKeyword(keyword)">&times;</button>
          </span>
          <input
            v-model="keywordInput"
            type="text"
            class="catalog-browser__keyword-input"
            placeholder="Add keyword filter..."
            @keydown.enter.prevent="addKeyword"
          />
        </div>

        <div class="catalog-browser__price-range">
          <label class="catalog-browser__filter-label">Price Range</label>
          <div class="catalog-browser__price-inputs">
            <input
              v-model.number="priceMin"
              type="number"
              class="catalog-browser__price-input"
              placeholder="Min"
              min="0"
            />
            <span class="catalog-browser__price-separator">--</span>
            <input
              v-model.number="priceMax"
              type="number"
              class="catalog-browser__price-input"
              placeholder="Max"
              min="0"
            />
          </div>
        </div>

        <div class="catalog-browser__date-filters">
          <label class="catalog-browser__filter-label">Availability</label>
          <div class="catalog-browser__date-inputs">
            <input v-model="dateFrom" type="date" class="catalog-browser__date-input" />
            <span class="catalog-browser__price-separator">to</span>
            <input v-model="dateTo" type="date" class="catalog-browser__date-input" />
          </div>
        </div>

        <div class="catalog-browser__sort">
          <label class="catalog-browser__filter-label">Sort By</label>
          <select v-model="sortBy" class="catalog-browser__sort-select">
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="popularity">Most Popular</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
          </select>
        </div>
      </div>
    </div>

    <LoadingSpinner v-if="loading" message="Loading catalog..." />

    <div v-else-if="items.length === 0">
      <EmptyState title="No items found" description="Try adjusting your search or filters." />
    </div>

    <div v-else class="catalog-browser__grid">
      <article
        v-for="item in items"
        :key="item.id"
        class="catalog-browser__card"
        @click="emit('select', item)"
      >
        <div class="catalog-browser__card-header">
          <span class="catalog-browser__card-type">{{ item.contentType }}</span>
          <span v-if="item.price != null && item.price > 0" class="catalog-browser__card-price">${{ item.price.toFixed(2) }}</span>
          <span v-else class="catalog-browser__card-price catalog-browser__card-price--free">Free</span>
        </div>
        <h4 class="catalog-browser__card-title">{{ item.title }}</h4>
        <p class="catalog-browser__card-desc">{{ item.description }}</p>
        <div class="catalog-browser__card-availability">
          <span v-if="item.availabilityStart">Available: {{ formatDate(item.availabilityStart) }}</span>
          <span v-if="item.availabilityEnd"> - {{ formatDate(item.availabilityEnd) }}</span>
        </div>
        <div class="catalog-browser__card-footer">
          <span class="catalog-browser__card-lang">{{ item.detectedLanguage }}</span>
          <span class="catalog-browser__card-popularity">Pop: {{ item.popularityScore }}</span>
        </div>
      </article>
    </div>

    <PaginationBar
      v-if="items.length > 0"
      :current-page="currentPage"
      :page-size="pageSize"
      :total-items="totalItems"
      @update:current-page="onPageChange"
      @update:page-size="onPageSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ContentRecord } from '@/types/content'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const props = defineProps<{
  items: ContentRecord[]
  loading: boolean
  totalItems: number
  currentPage: number
  pageSize: number
}>()

const emit = defineEmits<{
  select: [item: ContentRecord]
  search: [params: Record<string, unknown>]
  'update:currentPage': [page: number]
  'update:pageSize': [size: number]
}>()

const searchQuery = ref('')
const keywordInput = ref('')
const activeKeywords = ref<string[]>([])
const priceMin = ref<number | null>(null)
const priceMax = ref<number | null>(null)
const dateFrom = ref('')
const dateTo = ref('')
const sortBy = ref('newest')

function addKeyword() {
  const kw = keywordInput.value.trim()
  if (kw && !activeKeywords.value.includes(kw)) {
    activeKeywords.value.push(kw)
    keywordInput.value = ''
    emitSearch()
  }
}

function removeKeyword(keyword: string) {
  activeKeywords.value = activeKeywords.value.filter(k => k !== keyword)
  emitSearch()
}

function onSearch() {
  emitSearch()
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

function onPageChange(page: number) {
  emit('update:currentPage', page)
}

function onPageSizeChange(size: number) {
  emit('update:pageSize', size)
}

function emitSearch() {
  emit('search', {
    query: searchQuery.value,
    keywords: activeKeywords.value,
    priceMin: priceMin.value,
    priceMax: priceMax.value,
    dateFrom: dateFrom.value,
    dateTo: dateTo.value,
    sortBy: sortBy.value,
  })
}
</script>

<style scoped>
.catalog-browser__controls {
  margin-bottom: 24px;
}

.catalog-browser__search {
  margin-bottom: 12px;
}

.catalog-browser__search-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
}

.catalog-browser__search-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.catalog-browser__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.catalog-browser__keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  flex: 1;
  min-width: 200px;
}

.catalog-browser__keyword-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 14px;
  font-size: 0.78rem;
  font-weight: 500;
}

.catalog-browser__chip-remove {
  background: none;
  border: none;
  color: #3730a3;
  font-size: 1rem;
  cursor: pointer;
  padding: 0 2px;
}

.catalog-browser__keyword-input {
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.82rem;
  outline: none;
  width: 160px;
}

.catalog-browser__filter-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.catalog-browser__price-inputs,
.catalog-browser__date-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.catalog-browser__price-input,
.catalog-browser__date-input {
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.82rem;
  width: 90px;
  outline: none;
}

.catalog-browser__price-separator {
  color: #94a3b8;
  font-size: 0.82rem;
}

.catalog-browser__sort-select {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.82rem;
  background: #fff;
  outline: none;
}

.catalog-browser__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.catalog-browser__card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 18px;
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;
}

.catalog-browser__card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #93c5fd;
}

.catalog-browser__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.catalog-browser__card-type {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  background: #f1f5f9;
  color: #475569;
  padding: 2px 8px;
  border-radius: 10px;
}

.catalog-browser__card-price {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e293b;
}

.catalog-browser__card-price--free {
  color: #16a34a;
}

.catalog-browser__card-title {
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
  line-height: 1.3;
}

.catalog-browser__card-desc {
  font-size: 0.82rem;
  color: #64748b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.catalog-browser__card-availability {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 12px;
}

.catalog-browser__card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: #94a3b8;
}
</style>
