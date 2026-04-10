import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()

  const isAuthenticated = computed(() => authStore.isAuthenticated)
  const user = computed(() => authStore.user)
  const roles = computed(() => authStore.roles)
  const isAdmin = computed(() => authStore.isAdmin)
  const isCurator = computed(() => authStore.isCurator)
  const isInstructor = computed(() => authStore.isInstructor)
  const isTA = computed(() => authStore.isTA)
  const isStudent = computed(() => authStore.isStudent)

  async function login(username: string, password: string) {
    await authStore.login(username, password)
    router.push({ name: 'dashboard' })
  }

  function logout() {
    authStore.logout()
    router.push({ name: 'login' })
  }

  return {
    isAuthenticated,
    user,
    roles,
    isAdmin,
    isCurator,
    isInstructor,
    isTA,
    isStudent,
    login,
    logout
  }
}
