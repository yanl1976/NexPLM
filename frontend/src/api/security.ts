import { http } from './http'

export interface RuleRow {
  id: string
  code: string
  name: string
  objectType: string
  exprJson: string
  actions: string
  priority: number
  effect: string
  enabled: boolean
  remark: string | null
  updatedAt: string | null
}

export interface RuleSaveReq {
  code: string
  name: string
  objectType: string
  exprJson: string
  actions: string
  priority: number
  effect: string
  enabled: boolean
  remark?: string
}

export interface RuleTestResp {
  matched: boolean
  matchedRules: string[]
}

export interface AclRow {
  id: string
  objectId: string
  actorType: string
  actorId: string | null
  actorName: string | null
  actions: string
  effect: string
  expireAt: string | null
  source: string
}

export interface AclGrantReq {
  objectId: string
  actorType: string
  actorId?: string | null
  actions: string
  effect: string
  expireAt?: string | null
}

export const securityApi = {
  listRules: (objectType?: string) => http.get<RuleRow[]>('/security/rules', { objectType }),
  createRule: (body: RuleSaveReq) => http.post<string>('/security/rules', body),
  updateRule: (code: string, body: RuleSaveReq) => http.put<void>(`/security/rules/${code}`, body),
  removeRule: (id: string) => http.delete<void>(`/security/rules/${id}`),
  testRule: (body: { ruleId?: string; exprJson?: string; currentUserId?: string; attributes?: Record<string, unknown> }) =>
    http.post<RuleTestResp>('/security/rules/test', body),
  listAcl: (objectId: string) => http.get<AclRow[]>('/security/acl', { objectId }),
  grant: (body: AclGrantReq) => http.post<void>('/security/acl', body),
  revoke: (id: string) => http.delete<void>(`/security/acl/${id}`)
}
