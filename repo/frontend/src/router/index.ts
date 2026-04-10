import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from '@/guards/authGuard'
import { PERMISSIONS } from '@/utils/permissions'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/forbidden', name: 'forbidden', component: () => import('@/views/ForbiddenView.vue') },
    {
      path: '/',
      component: () => import('@/components/layout/AppLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        // Admin routes
        { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/UserManagementView.vue'), meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.USER_MANAGE] } },
        { path: 'admin/roles', name: 'admin-roles', component: () => import('@/views/admin/RoleManagementView.vue'), meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.ROLE_ASSIGN] } },
        { path: 'admin/audit', name: 'admin-audit', component: () => import('@/views/admin/AuditHistoryView.vue'), meta: { roles: ['ADMINISTRATOR'], permissions: [PERMISSIONS.AUDIT_VIEW] } },
        // Curator routes
        { path: 'curator/sources', name: 'curator-sources', component: () => import('@/views/curator/CrawlSourcesView.vue'), meta: { roles: ['CONTENT_CURATOR'], permissions: [PERMISSIONS.CRAWL_SOURCE_MANAGE] } },
        { path: 'curator/rules', name: 'curator-rules', component: () => import('@/views/curator/CrawlRulesView.vue'), meta: { roles: ['CONTENT_CURATOR'], permissions: [PERMISSIONS.CRAWL_RULE_MANAGE] } },
        { path: 'curator/runs', name: 'curator-runs', component: () => import('@/views/curator/CrawlRunsView.vue'), meta: { roles: ['CONTENT_CURATOR'], permissions: [PERMISSIONS.CRAWL_RUN_MANAGE] } },
        { path: 'curator/content', name: 'curator-content', component: () => import('@/views/curator/ContentReviewView.vue'), meta: { roles: ['CONTENT_CURATOR'], permissions: [PERMISSIONS.CONTENT_VIEW] } },
        // Instructor routes
        { path: 'instructor/question-banks', name: 'instructor-banks', component: () => import('@/views/instructor/QuestionBanksView.vue'), meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUESTION_BANK_MANAGE] } },
        { path: 'instructor/quizzes', name: 'instructor-quizzes', component: () => import('@/views/instructor/QuizManagementView.vue'), meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUIZ_MANAGE] } },
        { path: 'instructor/quizzes/:id', name: 'instructor-quiz-detail', component: () => import('@/views/instructor/QuizDetailView.vue'), meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.QUIZ_MANAGE] } },
        { path: 'instructor/submissions', name: 'instructor-submissions', component: () => import('@/views/instructor/SubmissionsReviewView.vue'), meta: { roles: ['INSTRUCTOR'], permissions: [PERMISSIONS.SUBMISSION_VIEW_ALL] } },
        // Student routes
        { path: 'student/dashboard', name: 'student-dashboard', component: () => import('@/views/student/StudentDashboardView.vue'), meta: { roles: ['STUDENT'] } },
        { path: 'student/catalog', name: 'student-catalog', component: () => import('@/views/student/CatalogView.vue'), meta: { roles: ['STUDENT'], permissions: [PERMISSIONS.CONTENT_VIEW] } },
        { path: 'student/timetable', name: 'student-timetable', component: () => import('@/views/student/TimetableView.vue'), meta: { roles: ['STUDENT'], permissions: [PERMISSIONS.SCHEDULE_MANAGE_OWN] } },
        { path: 'student/assessment/:id', name: 'student-assessment', component: () => import('@/views/student/AssessmentTakeView.vue'), meta: { roles: ['STUDENT'], permissions: [PERMISSIONS.QUIZ_TAKE] } },
        { path: 'student/wrong-answers', name: 'student-wrong-answers', component: () => import('@/views/student/WrongAnswerReviewView.vue'), meta: { roles: ['STUDENT'], permissions: [PERMISSIONS.WRONG_ANSWER_VIEW_OWN] } },
        // TA routes
        { path: 'ta/grading', name: 'ta-grading', component: () => import('@/views/ta/GradingQueueView.vue'), meta: { roles: ['TEACHING_ASSISTANT', 'INSTRUCTOR'], permissions: [PERMISSIONS.GRADING_VIEW] } },
        { path: 'ta/grading/:id', name: 'ta-grading-detail', component: () => import('@/views/ta/GradingDetailView.vue'), meta: { roles: ['TEACHING_ASSISTANT', 'INSTRUCTOR'], permissions: [PERMISSIONS.GRADING_VIEW] } },
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') }
  ]
})

router.beforeEach(authGuard)

export default router
