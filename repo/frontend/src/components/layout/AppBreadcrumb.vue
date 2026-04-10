<template>
  <nav class="breadcrumb" aria-label="Breadcrumb">
    <ol class="breadcrumb__list">
      <li
        v-for="(crumb, index) in crumbs"
        :key="crumb.path"
        class="breadcrumb__item"
      >
        <router-link
          v-if="index < crumbs.length - 1"
          :to="crumb.path"
          class="breadcrumb__link"
        >
          {{ crumb.label }}
        </router-link>
        <span v-else class="breadcrumb__current" aria-current="page">
          {{ crumb.label }}
        </span>
        <span v-if="index < crumbs.length - 1" class="breadcrumb__separator">/</span>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

interface Crumb {
  label: string
  path: string
}

const route = useRoute()

const crumbs = computed<Crumb[]>(() => {
  const segments = route.path.split('/').filter(Boolean)
  const result: Crumb[] = [{ label: 'Home', path: '/' }]
  let accumulated = ''
  for (const segment of segments) {
    accumulated += `/${segment}`
    const label = segment
      .replace(/-/g, ' ')
      .replace(/\b\w/g, c => c.toUpperCase())
    result.push({ label, path: accumulated })
  }
  return result
})
</script>

<style scoped>
.breadcrumb__list {
  display: flex;
  align-items: center;
  list-style: none;
  padding: 0;
  margin: 0;
  font-size: 0.85rem;
}

.breadcrumb__item {
  display: flex;
  align-items: center;
}

.breadcrumb__link {
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.15s;
}

.breadcrumb__link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.breadcrumb__current {
  color: #64748b;
  font-weight: 500;
}

.breadcrumb__separator {
  margin: 0 8px;
  color: #94a3b8;
}
</style>
