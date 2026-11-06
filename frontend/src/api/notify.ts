import { http } from './http'

export const NOTIFY_TYPE_LABEL: Record<string, string> = {
  EMAIL: '邮件（SMTP）',
  WECOM: '企业微信群机器人',
  DINGTALK: '钉钉自定义机器人'
}

export const NOTIFY_EVENT_LABEL: Record<string, string> = {
  'doc.released': '文档发布',
  'change.effective': '变更生效',
  'task.completed': '任务完成'
}

export const NOTIFY_STATUS_LABEL: Record<string, string> = {
  PENDING: '待发送',
  SENDING: '发送中',
  SENT: '已发送',
  FAILED: '失败待重试',
  DEAD: '死信'
}

export interface NotifyChannelRow {
  id: string
  code: string
  name: string
  type: string
  enabled: boolean
  maxAttempts: number
  events: string[]
  remark?: string | null
  updatedAt?: string | null
}

export interface NotifyChannelDetail extends NotifyChannelRow {
  config: Record<string, unknown>
  secretFields: string[]
  createdAt?: string | null
}

export interface NotifyChannelSaveReq {
  code: string
  name: string
  type: string
  enabled?: boolean
  maxAttempts?: number
  events: string[]
  remark?: string | null
  config: Record<string, unknown>
}

export interface NotifyMessageRow {
  id: string
  channelId: string
  channelCode?: string | null
  eventType: string
  eventKey: string
  title: string
  status: string
  attempt: number
  maxAttempts: number
  errorMsg?: string | null
  nextRetryAt?: string | null
  createdAt?: string | null
  finishedAt?: string | null
}

export interface NotifyMessageDetail extends NotifyMessageRow {
  content: string
  response?: string | null
  startedAt?: string | null
}

export const notifyApi = {
  types(): Promise<string[]> {
    return http.get('/sys/notify-channels/types')
  },
  events(): Promise<Record<string, string>> {
    return http.get('/sys/notify-channels/events')
  },
  page(params: {
    keyword?: string
    page?: number
    size?: number
  }): Promise<{ items: NotifyChannelRow[]; total: number; page: number; size: number }> {
    return http.get('/sys/notify-channels', params)
  },
  detail(id: string): Promise<NotifyChannelDetail> {
    return http.get<NotifyChannelDetail>(`/sys/notify-channels/${id}`)
  },
  create(req: NotifyChannelSaveReq): Promise<NotifyChannelDetail> {
    return http.post<NotifyChannelDetail>('/sys/notify-channels', req)
  },
  update(id: string, req: NotifyChannelSaveReq): Promise<NotifyChannelDetail> {
    return http.put<NotifyChannelDetail>(`/sys/notify-channels/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/sys/notify-channels/${id}`)
  },
  test(id: string): Promise<NotifyMessageDetail> {
    return http.post<NotifyMessageDetail>(`/sys/notify-channels/${id}/test`)
  },
  messagePage(
    id: string,
    params: { status?: string; page?: number; size?: number }
  ): Promise<{ items: NotifyMessageRow[]; total: number; page: number; size: number }> {
    return http.get(`/sys/notify-channels/${id}/messages`, params)
  },
  message(id: string): Promise<NotifyMessageDetail> {
    return http.get<NotifyMessageDetail>(`/sys/notify-channels/messages/${id}`)
  },
  resend(id: string): Promise<NotifyMessageDetail> {
    return http.post<NotifyMessageDetail>(`/sys/notify-channels/messages/${id}/resend`)
  }
}
