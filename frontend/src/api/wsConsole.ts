import { http } from './http'

/** 工作空间文件夹（扁平；space = MY / PUBLIC / PROJECT）。 */
export interface WsFolder {
  id: string
  parent_id?: string | null
  name: string
  path: string
  space: string
  project_id?: string | null
  project_name?: string | null
}

/** 文件夹里的对象行（对齐蓝本工作空间列表）。 */
export interface WsObject {
  id: string
  object_type: string
  code: string
  name: string
  revision?: number | null
  iteration?: number | null
  lifecycle_state?: string | null
  workflow_state?: string | null
  item_type?: string | null
  using_state?: string | null
  specification?: string | null
  project_code?: string | null
  checkout_user?: string | null
  checked_out_at?: string | null
}

export const wsConsoleApi = {
  tree: (space: string) => http.get<WsFolder[]>('/ws-console/tree', { space }),
  objects: (space: string, folderId: string) =>
    http.get<WsObject[]>('/ws-console/objects', { space, folderId }),
  addObject: (space: string, folderId: string, objectId: string) =>
    http.post<void>(`/ws-console/objects?space=${encodeURIComponent(space)}&folderId=${encodeURIComponent(folderId)}`, {
      objectId
    }),
  checkout: (id: string) => http.post<void>(`/ws-console/objects/${id}/checkout`, {}),
  checkin: (id: string) => http.delete<void>(`/ws-console/objects/${id}/checkout`)
}
