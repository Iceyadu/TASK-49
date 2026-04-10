<template>
  <div class="dashboard">
    <h1>Welcome, {{ authStore.user?.fullName || 'User' }}</h1>
    <div class="role-badges">
      <span v-for="role in authStore.roles" :key="role" class="badge">{{ role.replace('_', ' ') }}</span>
    </div>
    <div class="dashboard-grid">
      <div v-if="authStore.isAdmin" class="card">
        <h3>User Management</h3>
        <p>Manage users, assign roles, and review audit history</p>
        <router-link to="/admin/users" class="card-link">Manage Users</router-link>
      </div>
      <div v-if="authStore.isAdmin" class="card">
        <h3>Audit History</h3>
        <p>Review permission changes and system activity</p>
        <router-link to="/admin/audit" class="card-link">View Audit Logs</router-link>
      </div>
      <div v-if="authStore.isCurator" class="card">
        <h3>Crawl Sources</h3>
        <p>Configure and manage content intake sources</p>
        <router-link to="/curator/sources" class="card-link">Manage Sources</router-link>
      </div>
      <div v-if="authStore.isCurator" class="card">
        <h3>Content Review</h3>
        <p>Review and publish standardized content</p>
        <router-link to="/curator/content" class="card-link">Review Content</router-link>
      </div>
      <div v-if="authStore.isInstructor" class="card">
        <h3>Question Banks</h3>
        <p>Create questions and assemble quizzes</p>
        <router-link to="/instructor/question-banks" class="card-link">Manage Banks</router-link>
      </div>
      <div v-if="authStore.isInstructor" class="card">
        <h3>Quiz Management</h3>
        <p>Assemble, schedule, and publish quizzes</p>
        <router-link to="/instructor/quizzes" class="card-link">Manage Quizzes</router-link>
      </div>
      <div v-if="authStore.isStudent" class="card">
        <h3>Learning Catalog</h3>
        <p>Browse imported learning materials</p>
        <router-link to="/student/catalog" class="card-link">Browse Catalog</router-link>
      </div>
      <div v-if="authStore.isStudent" class="card">
        <h3>My Timetable</h3>
        <p>Manage your weekly schedule</p>
        <router-link to="/student/timetable" class="card-link">Edit Timetable</router-link>
      </div>
      <div v-if="authStore.isTA || authStore.isInstructor" class="card">
        <h3>Grading Queue</h3>
        <p>Grade pending subjective submissions</p>
        <router-link to="/ta/grading" class="card-link">View Queue</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()
</script>

<style scoped>
.dashboard { padding: 24px; }
.dashboard h1 { margin-bottom: 8px; color: #1a365d; }
.role-badges { margin-bottom: 24px; }
.badge { display: inline-block; background: #ebf4ff; color: #3182ce; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; margin-right: 8px; text-transform: capitalize; }
.dashboard-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.card { background: white; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
.card h3 { color: #2d3748; margin-bottom: 8px; }
.card p { color: #718096; font-size: 14px; margin-bottom: 16px; }
.card-link { color: #3182ce; text-decoration: none; font-weight: 600; font-size: 14px; }
.card-link:hover { text-decoration: underline; }
</style>
