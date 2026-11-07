import { http, request } from './http'

export interface OrgNode {
  id: string
  parentId: string | null
  code: string
  name: string
  orgType: string
  sort: number
  status: string
  children: OrgNode[]
}

export interface OrgSaveReq {
  parentId: string | null
  code: string
  name: string
  orgType: string
  sort: number
  status: string
}

export interface UserRow {
  id: string
  username: string
  displayName: string
  email: string | null
  mobile: string | null
  securityLevel: number
  status: string
  mustChangePwd: boolean
  lastLoginAt: string | null
  roleIds: string[]
  roleNames: string[]
}

export interface UserSaveReq {
  username?: string
  password?: string
  displayName: string
  email?: string
  mobile?: string
  securityLevel?: number
  status?: string
  roleIds: string[]
}

export interface RoleRow {
  id: string
  code: string
  name: string
  roleType: string
  dataScope: string
  status: string
  userCount: number
  permissionCount: number
}

export interface PermissionItem {
  code: string
  module: string
  action: string
  name: string
  nameEn: string | null
}

export interface MetaEntitySummary {
  code: string
  name: string
  nameEn: string | null
  parentCode: string | null
  status: string
  versionNo: number
  fieldCount: number
  updatedAt: string
}

export interface MetaFieldDef {
  code: string
  name: string
  nameEn?: string | null
  valueType: string
  dataType?: string | null
  length?: number | null
  required?: boolean
  defaultValue?: string | null
  enumCode?: string | null
  refEntity?: string | null
  searchable?: boolean
  sort?: number | null
  uiJson?: string | null
}

export interface MetaEntityDetail {
  code: string
  name: string
  nameEn: string | null
  parentCode: string | null
  abstractFlag: boolean
  capabilities: string[]
  versionPolicy: string
  iterationPolicy: string
  lifecycleCode: string | null
  workflowAllowed: boolean
  status: string
  versionNo: number
  fields: MetaFieldDef[]
}

export interface MetaEnumItem {
  code: string
  name: string
  sort: number
}

export interface MetaEnumDetail {
  code: string
  name: string
  items: MetaEnumItem[]
}

export const orgApi = {
  tree: () => http.get<OrgNode[]>('/system/orgs'),
  create: (body: OrgSaveReq) => http.post<string>('/system/orgs', body),
  update: (id: string, body: OrgSaveReq) => http.put<void>(`/system/orgs/${id}`, body),
  remove: (id: string) => http.delete<void>(`/system/orgs/${id}`)
}

export const userApi = {
  page: (params: { keyword?: string; status?: string; page: number; size: number }) =>
    http.get<{ items: UserRow[]; total: number; page: number; size: number }>('/system/users', params),
  create: (body: UserSaveReq) => http.post<string>('/system/users', body),
  update: (id: string, body: UserSaveReq) => http.put<void>(`/system/users/${id}`, body),
  resetPassword: (id: string, newPassword: string) =>
    http.post<void>(`/system/users/${id}/reset-password`, { newPassword }),
  remove: (id: string) => http.delete<void>(`/system/users/${id}`)
}

export const roleApi = {
  list: () => http.get<RoleRow[]>('/system/roles'),
  permissions: () => http.get<PermissionItem[]>('/system/roles/permissions'),
  rolePermissions: (id: string) => http.get<string[]>(`/system/roles/${id}/permissions`),
  grant: (id: string, permissionCodes: string[]) =>
    http.put<void>(`/system/roles/${id}/permissions`, { permissionCodes }),
  create: (body: { code: string; name: string; dataScope: string; status: string }) =>
    http.post<string>('/system/roles', body),
  update: (id: string, body: { code: string; name: string; dataScope: string; status: string }) =>
    http.put<void>(`/system/roles/${id}`, body),
  remove: (id: string) => http.delete<void>(`/system/roles/${id}`)
}

export interface LifecycleState {
  code: string
  name: string
  allFlow: boolean
}

export interface LifecycleTransition {
  from: string
  to: string
  action: string
  label: string
}

export interface LifecycleDefinition {
  code: string
  name: string
  initial: string
  states: LifecycleState[]
  transitions: LifecycleTransition[]
}

export const lifecycleApi = {
  list: () => http.get<LifecycleDefinition[]>('/meta/lifecycles'),
  detail: (code: string) => http.get<LifecycleDefinition>(`/meta/lifecycles/${code}`),
  save: (code: string, definitionJson: string) =>
    http.put<void>(`/meta/lifecycles/${code}`, { definitionJson })
}

/** 对象查看器页签定义（元数据驱动）。 */
export interface MetaTabDef {
  code: string
  name: string
  tabType: string
  permissionCode?: string | null
  sort: number
  configJson?: string | null
}

export const metaApi = {
  listEntities: (keyword?: string) => http.get<MetaEntitySummary[]>('/meta/entities', { keyword }),
  getEntity: (code: string) => http.get<MetaEntityDetail>(`/meta/entities/${code}`),
  createEntity: (body: unknown) => http.post<void>('/meta/entities', body),
  updateEntity: (code: string, body: unknown) => http.put<void>(`/meta/entities/${code}`, body),
  publish: (code: string) => http.post<void>(`/meta/entities/${code}/publish`),
  remove: (code: string) => http.delete<void>(`/meta/entities/${code}`),
  entityTabs: (code: string) => http.get<MetaTabDef[]>(`/meta/entities/${code}/tabs`),
  listEnums: () => http.get<MetaEnumDetail[]>('/meta/enums'),
  getEnum: (code: string) => http.get<MetaEnumDetail>(`/meta/enums/${code}`),
  saveEnumItems: (code: string, items: MetaEnumItem[]) =>
    http.put<void>(`/meta/enums/${code}/items`, { items })
}

// ---------- 批次 F-1 在线用户 ----------
export interface SessionRow {
  id: string
  userId: string
  username: string
  displayName: string | null
  deviceType: string
  ip: string | null
  ua: string | null
  loginAt: string
  lastActiveAt: string
  /** 在线秒数（后端 long，保持数字） */
  onlineSeconds: number
  active: boolean
}

export const sessionApi = {
  page: (params: {
    keyword?: string
    deviceType?: string
    onlyActive?: boolean
    page: number
    size: number
  }) => http.get<{ items: SessionRow[]; total: number; page: number; size: number }>('/sys/sessions', params),
  kick: (id: string) => http.post<void>(`/sys/sessions/${id}/kick`),
  /** 后端 kick-all 的 deviceType 是 @RequestParam，必须走查询串而不是请求体。 */
  kickAll: (deviceType?: string) =>
    request<{ kicked: number }>('/sys/sessions/kick-all', { method: 'POST', query: { deviceType } })
}

// ---------- 批次 F-3 系统运行日志 ----------
export interface LoginLogRow {
  id: string
  username: string
  userId: string | null
  displayName: string | null
  result: string
  failReason: string | null
  ip: string | null
  ua: string | null
  createdAt: string
}

export interface AuditLogRow {
  id: string
  traceId: string | null
  userId: string | null
  displayName: string | null
  module: string
  action: string
  objectType: string | null
  objectId: string | null
  result: string
  detail: string | null
  ip: string | null
  ua: string | null
  createdAt: string
}

export interface LogSummary {
  loginSuccess: number
  loginFailed: number
  auditCount: number
  chainEnabled: boolean
  chainChecked: number
  chainBroken: number
  chainMessage: string
}

export const logApi = {
  summary: () => http.get<LogSummary>('/sys/logs/summary'),
  login: (params: {
    username?: string
    result?: string
    from?: string
    to?: string
    page: number
    size: number
  }) => http.get<{ items: LoginLogRow[]; total: number; page: number; size: number }>('/sys/logs/login', params),
  audit: (params: {
    module?: string
    action?: string
    result?: string
    keyword?: string
    userId?: string
    from?: string
    to?: string
    page: number
    size: number
  }) => http.get<{ items: AuditLogRow[]; total: number; page: number; size: number }>('/sys/logs/audit', params)
}

// ---------- 批次 F-2 系统广播 ----------
export interface BroadcastRow {
  id: string
  title: string
  content: string
  level: string
  publishState: string
  publishAt: string | null
  expireAt: string | null
  popup: boolean
  createdBy: string | null
  createdAt: string
  updatedAt: string
  readCount: number
}

export interface BroadcastPending {
  id: string
  title: string
  content: string
  level: string
  publishAt: string | null
}

export interface BroadcastSaveReq {
  title: string
  content: string
  level: string
  publishAt: string | null
  expireAt: string | null
  popup: boolean
}

export const broadcastApi = {
  page: (params: { keyword?: string; state?: string; page: number; size: number }) =>
    http.get<{ items: BroadcastRow[]; total: number; page: number; size: number }>('/sys/broadcasts', params),
  create: (body: BroadcastSaveReq) => http.post<string>('/sys/broadcasts', body),
  update: (id: string, body: BroadcastSaveReq) => http.put<void>(`/sys/broadcasts/${id}`, body),
  publish: (id: string) => http.post<void>(`/sys/broadcasts/${id}/publish`),
  revoke: (id: string) => http.post<void>(`/sys/broadcasts/${id}/revoke`),
  remove: (id: string) => http.delete<void>(`/sys/broadcasts/${id}`),
  /** 收件人侧：登录后待弹窗的未读广播（只要登录即可，不需要管理权限） */
  pending: () => http.get<BroadcastPending[]>('/sys/broadcasts/pending'),
  markRead: (id: string) => http.post<void>(`/sys/broadcasts/${id}/read`)
}

// ---------- 批次 F-4 三员管理 ----------
export interface ThreeRoleMember {
  userId: string
  username: string
  displayName: string
  status: string
  lastLoginAt: string | null
}

export interface ThreeRoleGroup {
  roleCode: string
  roleName: string
  members: ThreeRoleMember[]
}

export interface ThreeRoleViolation {
  userId: string
  username: string
  displayName: string
  roleCodes: string
  roleCount: number
}

export interface ThreeRoleOverview {
  roles: ThreeRoleGroup[]
  violations: ThreeRoleViolation[]
  note: string
}

export const threeRoleApi = {
  overview: () => http.get<ThreeRoleOverview>('/sys/three-roles')
}
