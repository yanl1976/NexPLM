import { http } from './http'

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

export const metaApi = {
  listEntities: (keyword?: string) => http.get<MetaEntitySummary[]>('/meta/entities', { keyword }),
  getEntity: (code: string) => http.get<MetaEntityDetail>(`/meta/entities/${code}`),
  createEntity: (body: unknown) => http.post<void>('/meta/entities', body),
  updateEntity: (code: string, body: unknown) => http.put<void>(`/meta/entities/${code}`, body),
  publish: (code: string) => http.post<void>(`/meta/entities/${code}/publish`),
  remove: (code: string) => http.delete<void>(`/meta/entities/${code}`),
  listEnums: () => http.get<MetaEnumDetail[]>('/meta/enums'),
  getEnum: (code: string) => http.get<MetaEnumDetail>(`/meta/enums/${code}`),
  saveEnumItems: (code: string, items: MetaEnumItem[]) =>
    http.put<void>(`/meta/enums/${code}/items`, { items })
}
