<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1>404</h1>
      <h2>Page Not Found</h2>
      <p>The page you are looking for does not exist.</p>
      <router-link to="/" class="back-btn">Return to Dashboard</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found-page { min-height: 80vh; display: flex; align-items: center; justify-content: center; }
.not-found-content { text-align: center; }
.not-found-content h1 { font-size: 72px; color: #718096; margin-bottom: 0; }
.not-found-content h2 { font-size: 24px; color: #2d3748; margin-bottom: 12px; }
.not-found-content p { color: #718096; margin-bottom: 24px; }
.back-btn { display: inline-block; padding: 10px 24px; background: #3182ce; color: white; border-radius: 8px; text-decoration: none; }
</style>
