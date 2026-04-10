<template>
  <div class="app-layout">
    <AppSidebar class="app-layout__sidebar" />
    <div class="app-layout__main">
      <AppHeader class="app-layout__header" />
      <div class="app-layout__content">
        <AppBreadcrumb class="app-layout__breadcrumb" />
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import AppBreadcrumb from './AppBreadcrumb.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.app-layout__sidebar {
  width: 240px;
  flex-shrink: 0;
}

.app-layout__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.app-layout__header {
  flex-shrink: 0;
}

.app-layout__content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.app-layout__breadcrumb {
  margin-bottom: 16px;
}
</style>
