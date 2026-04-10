<template>
  <aside class="sidebar">
    <div class="sidebar__brand">
      <h2 class="sidebar__logo">ScholarOps</h2>
    </div>
    <nav class="sidebar__nav">
      <ul class="sidebar__menu">
        <li class="sidebar__item">
          <router-link to="/" class="sidebar__link" exact-active-class="sidebar__link--active">
            <span class="sidebar__icon">&#9679;</span>
            Dashboard
          </router-link>
        </li>

        <template v-if="authStore.isAdmin">
          <li class="sidebar__section-title">Administration</li>
          <li class="sidebar__item">
            <router-link to="/admin/users" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#9775;</span>
              User Management
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/admin/roles" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#9881;</span>
              Role Management
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/admin/audit" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128220;</span>
              Audit History
            </router-link>
          </li>
        </template>

        <template v-if="authStore.isCurator">
          <li class="sidebar__section-title">Content Curation</li>
          <li class="sidebar__item">
            <router-link to="/curator/sources" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#127760;</span>
              Crawl Sources
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/curator/rules" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128221;</span>
              Crawl Rules
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/curator/runs" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#9654;</span>
              Crawl Runs
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/curator/content" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128196;</span>
              Content Review
            </router-link>
          </li>
        </template>

        <template v-if="authStore.isInstructor">
          <li class="sidebar__section-title">Instruction</li>
          <li class="sidebar__item">
            <router-link to="/instructor/question-banks" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128218;</span>
              Question Banks
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/instructor/quizzes" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128203;</span>
              Quiz Management
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/instructor/submissions" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128202;</span>
              Submissions
            </router-link>
          </li>
        </template>

        <template v-if="authStore.isStudent">
          <li class="sidebar__section-title">Student</li>
          <li class="sidebar__item">
            <router-link to="/student/dashboard" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#127968;</span>
              My Dashboard
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/student/catalog" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128269;</span>
              Catalog
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/student/timetable" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#128197;</span>
              Timetable
            </router-link>
          </li>
          <li class="sidebar__item">
            <router-link to="/student/wrong-answers" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#10060;</span>
              Wrong Answers
            </router-link>
          </li>
        </template>

        <template v-if="authStore.isTA || authStore.isInstructor">
          <li class="sidebar__section-title">Grading</li>
          <li class="sidebar__item">
            <router-link to="/ta/grading" class="sidebar__link" active-class="sidebar__link--active">
              <span class="sidebar__icon">&#9998;</span>
              Grading Queue
            </router-link>
          </li>
        </template>
      </ul>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.sidebar {
  background: #1e293b;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

.sidebar__brand {
  padding: 20px 16px;
  border-bottom: 1px solid #334155;
}

.sidebar__logo {
  font-size: 1.25rem;
  font-weight: 700;
  color: #38bdf8;
  letter-spacing: 0.5px;
}

.sidebar__nav {
  flex: 1;
  padding: 8px 0;
}

.sidebar__menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar__section-title {
  padding: 16px 16px 6px;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #64748b;
  font-weight: 600;
}

.sidebar__item {
  margin: 1px 0;
}

.sidebar__link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: #cbd5e1;
  text-decoration: none;
  font-size: 0.875rem;
  border-left: 3px solid transparent;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.sidebar__link:hover {
  background: #334155;
  color: #f1f5f9;
}

.sidebar__link--active {
  background: #334155;
  color: #38bdf8;
  border-left-color: #38bdf8;
}

.sidebar__icon {
  font-size: 1rem;
  width: 20px;
  text-align: center;
}
</style>
