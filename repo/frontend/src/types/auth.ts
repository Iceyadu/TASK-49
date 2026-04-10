export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
  roles: string[]
  permissions: string[]
}

export interface UserInfo {
  id: number
  username: string
  email: string
  fullName: string
}
