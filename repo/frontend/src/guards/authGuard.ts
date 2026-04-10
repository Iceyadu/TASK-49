import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function authGuard(
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
): void {
  const authStore = useAuthStore()

  // Allow public routes
  if (to.meta.public) {
    return next()
  }

  // Redirect unauthenticated users to login
  if (!authStore.isAuthenticated) {
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  // Check role requirements
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => authStore.hasRole(role))
    if (!hasRequiredRole) {
      return next({ name: 'forbidden' })
    }
  }

  // Check permission requirements (user must have every listed permission)
  const requiredPermissions = to.meta.permissions as string[] | undefined
  if (requiredPermissions && requiredPermissions.length > 0) {
    const hasRequiredPermission = requiredPermissions.every(perm => authStore.hasPermission(perm))
    if (!hasRequiredPermission) {
      return next({ name: 'forbidden' })
    }
  }

  next()
}

export function guestGuard(
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
): void {
  const authStore = useAuthStore()

  if (authStore.isAuthenticated) {
    return next({ name: 'dashboard' })
  }

  next()
}
