import { http } from './http'

/** 我的检出（列口径对齐旧系统 CHECKOUTOBJECTVIEW：对象类型 / 编码 / 检出时间）。 */
export interface CheckoutRow {
  objectType: string
  objectTypeName: string
  objectId: string
  code: string | null
  name: string | null
  revision: string | null
  checkedOutAt: string | null
  expireAt: string | null
  expired: boolean
}

/** 收藏条目（旧系统 FAVORITELINK：收藏对象 + 分组 + 顺序）。 */
export interface FavoriteRow {
  id: string
  objectType: string
  objectTypeName: string
  objectId: string
  code: string | null
  name: string | null
  lifecycleState: string | null
  folder: string | null
  seqNo: number
  remark: string | null
  createdAt: string | null
}

/** 最近访问（旧系统 AccessRecord）。 */
export interface RecentRow {
  objectType: string
  objectTypeName: string
  objectId: string
  code: string | null
  name: string | null
  accessType: string
  accessedAt: string | null
}

/** 站内消息（复用通知台账的收件箱视角）。 */
export interface MessageRow {
  id: string
  eventType: string
  title: string
  content: string
  status: string
  createdAt: string | null
  readAt: string | null
}

/** 高级查询结果（plm_object ∪ chg_ecr）。 */
export interface SearchRow {
  objectType: string
  objectTypeName: string
  objectId: string
  code: string | null
  name: string | null
  lifecycleState: string | null
  revision: string | null
  updatedAt: string | null
}

export const myApi = {
  checkouts: () => http.get<CheckoutRow[]>('/my/checkouts'),
  cancelCheckout: (objectId: string) => http.post<void>(`/my/checkouts/${objectId}/cancel`, {}),

  favorites: (objectType?: string) =>
    http.get<FavoriteRow[]>('/my/favorites', { objectType }),
  addFavorite: (body: { objectType: string; objectId: string; folder?: string; remark?: string }) =>
    http.post<string>('/my/favorites', body),
  removeFavorite: (id: string) => http.delete<void>(`/my/favorites/${id}`),

  recent: (objectType?: string, limit = 50) =>
    http.get<RecentRow[]>('/my/recent', { objectType, limit }),
  recordAccess: (body: { objectType: string; objectId: string; accessType?: string }) =>
    http.post<void>('/my/recent', body),

  messages: (unreadOnly = false, limit = 50) =>
    http.get<MessageRow[]>('/my/messages', { unreadOnly, limit }),
  unreadCount: () => http.get<{ unread: number }>('/my/messages/unread-count'),
  markRead: (id: string) => http.post<void>(`/my/messages/${id}/read`, {}),
  /** 全部已读：只影响本人收件箱（批次 E-2 收件人化后才有意义）。 */
  markAllRead: () => http.post<{ updated: number }>('/my/messages/read-all', {}),

  search: (params: { keyword?: string; objectType?: string; limit?: number }) =>
    http.get<SearchRow[]>('/my/search', params),
  objectTypes: () => http.get<{ types: string[]; labels: Record<string, string> }>('/my/object-types')
}
