<template>
  <div class="forbidden-state">
    <div class="forbidden-state__icon">
      <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="1.5">
        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
        <path d="M7 11V7a5 5 0 0110 0v4" />
      </svg>
    </div>
    <h2 class="forbidden-state__code">403</h2>
    <h3 class="forbidden-state__title">Access Forbidden</h3>
    <p class="forbidden-state__description">
      You do not have permission to access this resource. Please contact your administrator if you believe this is an error.
    </p>
    <router-link to="/" class="forbidden-state__home-link">Return to Dashboard</router-link>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.forbidden-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  min-height: 60vh;
}

.forbidden-state__icon {
  margin-bottom: 16px;
  opacity: 0.8;
}

.forbidden-state__code {
  font-size: 3rem;
  font-weight: 800;
  color: #ef4444;
  margin-bottom: 4px;
}

.forbidden-state__title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 12px;
}

.forbidden-state__description {
  font-size: 0.9rem;
  color: #64748b;
  max-width: 400px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.forbidden-state__home-link {
  padding: 10px 24px;
  background: #3b82f6;
  color: #fff;
  border-radius: 6px;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.15s;
}

.forbidden-state__home-link:hover {
  background: #2563eb;
}
</style>
