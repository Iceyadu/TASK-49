import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ROLE_PERMISSIONS } from '@/utils/permissions'

export function usePermission() {
  const authStore = useAuthStore()

  function hasRole(role: string): boolean {
    return authStore.hasRole(role)
  }

  function hasPermission(permission: string): boolean {
    return authStore.hasPermission(permission)
  }

  function hasAnyRole(roles: string[]): boolean {
    return roles.some(role => authStore.hasRole(role))
  }

  function hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(perm => authStore.hasPermission(perm))
  }

  function canAccess(route: string): boolean {
    const roleRouteMap: Record<string, string[]> = {
      ADMINISTRATOR: ['admin'],
      CONTENT_CURATOR: ['curator'],
      INSTRUCTOR: ['instructor', 'ta'],
      TEACHING_ASSISTANT: ['ta'],
      STUDENT: ['student']
    }

    for (const role of authStore.roles) {
      const allowedPrefixes = roleRouteMap[role] || []
      if (allowedPrefixes.some(prefix => route.startsWith(prefix) || route.startsWith(`/${prefix}`))) {
        return true
      }
    }

    return false
  }

  const effectivePermissions = computed(() => {
    const perms = new Set<string>(authStore.permissions)
    for (const role of authStore.roles) {
      const rolePerms = ROLE_PERMISSIONS[role] || []
      rolePerms.forEach(p => perms.add(p))
    }
    return Array.from(perms)
  })

  return {
    hasRole,
    hasPermission,
    hasAnyRole,
    hasAllPermissions,
    canAccess,
    effectivePermissions
  }
}
