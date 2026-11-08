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
  me: () => http.get<UserBrief>('/auth/me'),
  /** 修改自己的密码；后端校验强度与历史不可重用，成功后清除 mustChangePwd。 */
  changePassword: (oldPassword: string, newPassword: string) =>
    http.post<void>('/auth/change-password', { oldPassword, newPassword })
}
