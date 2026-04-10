<template>
  <div class="forbidden-page">
    <div class="forbidden-content">
      <h1>403</h1>
      <h2>Access Denied</h2>
      <p>You do not have permission to access this page.</p>
      <router-link to="/" class="back-btn">Return to Dashboard</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.forbidden-page { min-height: 80vh; display: flex; align-items: center; justify-content: center; }
.forbidden-content { text-align: center; }
.forbidden-content h1 { font-size: 72px; color: #e53e3e; margin-bottom: 0; }
.forbidden-content h2 { font-size: 24px; color: #2d3748; margin-bottom: 12px; }
.forbidden-content p { color: #718096; margin-bottom: 24px; }
.back-btn { display: inline-block; padding: 10px 24px; background: #3182ce; color: white; border-radius: 8px; text-decoration: none; }
</style>
