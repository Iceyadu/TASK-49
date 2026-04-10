export interface User {
  id: number
  username: string
  email: string
  fullName: string
  enabled: boolean
  accountLocked: boolean
  roles: Role[]
  createdAt: string
}

export interface Role {
  id: number
  name: string
  description: string
}

export interface Permission {
  id: number
  code: string
  description: string
  category: string
}

export interface UserCreateRequest {
  username: string
  email: string
  password: string
  fullName: string
}

export interface AdminPasswordResetRequest {
  newPassword: string
  workstationId: string
  reason?: string
}
