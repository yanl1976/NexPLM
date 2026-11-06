import { http } from './http'
import type { TokenResponse } from './auth'

export const SSO_TYPE_LABEL: Record<string, string> = {
  OIDC: 'OIDC 统一身份',
  LDAP: 'LDAP / AD 域账号'
}

/** 登录页可见的登录方式（只有编码/名称/类型）。 */
export interface PublicSsoProvider {
  code: string
  name: string
  type: string
}

export interface SsoProviderRow {
  id: string
  code: string
  name: string
  type: string
  enabled: boolean
  autoProvision: boolean
  defaultRoleCode?: string | null
  remark?: string | null
  updatedAt?: string | null
}

export interface SsoProviderDetail extends SsoProviderRow {
  config: Record<string, unknown>
  /** 已配置的密钥字段名（值不回传） */
  secretFields: string[]
  createdAt?: string | null
}

export interface SsoProviderSaveReq {
  code: string
  name: string
  type: string
  enabled?: boolean
  autoProvision?: boolean
  defaultRoleCode?: string | null
  remark?: string | null
  config: Record<string, unknown>
}

export const ssoAuthApi = {
  providers(): Promise<PublicSsoProvider[]> {
    return http.get('/auth/sso/providers')
  },
  authorize(code: string): Promise<{ url: string; state: string }> {
    return http.get(`/auth/sso/${code}/authorize`)
  },
  callback(code: string, authCode: string, state: string): Promise<TokenResponse> {
    return http.post<TokenResponse>(`/auth/sso/${code}/callback`, { authCode, state })
  },
  ldapLogin(code: string, username: string, password: string): Promise<TokenResponse> {
    return http.post<TokenResponse>(`/auth/sso/${code}/ldap-login`, { username, password })
  }
}

export const ssoProviderApi = {
  page(params: {
    keyword?: string
    page?: number
    size?: number
  }): Promise<{ items: SsoProviderRow[]; total: number; page: number; size: number }> {
    return http.get('/sys/sso-providers', params)
  },
  detail(id: string): Promise<SsoProviderDetail> {
    return http.get<SsoProviderDetail>(`/sys/sso-providers/${id}`)
  },
  create(req: SsoProviderSaveReq): Promise<SsoProviderDetail> {
    return http.post<SsoProviderDetail>('/sys/sso-providers', req)
  },
  update(id: string, req: SsoProviderSaveReq): Promise<SsoProviderDetail> {
    return http.put<SsoProviderDetail>(`/sys/sso-providers/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sys/sso-providers/${id}`)
  }
}
