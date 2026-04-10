<template>
  <div class="student-dashboard">
    <h1>Student Dashboard</h1>
    <div class="dashboard-grid">
      <div class="card catalog-card">
        <h3>Learning Catalog</h3>
        <p>Browse and search imported learning materials</p>
        <router-link to="/student/catalog" class="card-link">Browse Catalog</router-link>
      </div>
      <div class="card schedule-card">
        <h3>My Timetable</h3>
        <p>View and edit your weekly schedule with drag-and-drop</p>
        <router-link to="/student/timetable" class="card-link">Edit Timetable</router-link>
      </div>
      <div class="card assessment-card">
        <h3>Assessments</h3>
        <p>Take quizzes and view your results</p>
        <div class="assessment-list">
          <p v-if="quizzes.length === 0" class="empty-note">No assessments available</p>
          <div v-for="quiz in quizzes" :key="quiz.id" class="quiz-item">
            <span>{{ quiz.title }}</span>
            <router-link :to="`/student/assessment/${quiz.id}`" class="take-btn">Take</router-link>
          </div>
        </div>
      </div>
      <div class="card review-card">
        <h3>Wrong Answer Review</h3>
        <p>Review incorrect answers with instructor explanations</p>
        <router-link to="/student/wrong-answers" class="card-link">Review Answers</router-link>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getQuizzes } from '@/api/quiz'
const quizzes = ref<any[]>([])
onMounted(async () => {
  try {
    const page = await getQuizzes(0, 5)
    quizzes.value = page.content || []
  } catch(e) {
    // Quiz list may not be available for students without QUIZ_MANAGE
    quizzes.value = []
  }
})
</script>
<style scoped>
.student-dashboard { padding: 24px; }
.student-dashboard h1 { color: #1a365d; margin-bottom: 24px; }
.dashboard-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.card { background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; }
.card h3 { color: #2d3748; margin-bottom: 8px; }
.card p { color: #718096; font-size: 14px; margin-bottom: 12px; }
.card-link { color: #3182ce; font-weight: 600; text-decoration: none; font-size: 14px; }
.quiz-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.take-btn { color: #3182ce; text-decoration: none; font-weight: 600; font-size: 13px; }
.empty-note { color: #a0aec0; font-size: 13px; }
</style>
