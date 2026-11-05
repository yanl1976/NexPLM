import { http } from './http'

export interface UserBrief {
  id: string
  username: string
  displayName: string
  companyId: string
  roles: string[]
  permissions: string[]
  mustChangePwd: boolean
}

export interface TokenResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  user: UserBrief
}

export const authApi = {
  login: (username: string, password: string) =>
    http.post<TokenResponse>('/auth/login', { username, password }),
  me: () => http.get<UserBrief>('/auth/me')
}
