/**
 * 统一 HTTP 客户端：注入令牌、统一解包 Result、401 自动刷新一次。
 */
export interface ApiResult<T> {
  code: string
  message: string
  data: T
  traceId: string
}

export class ApiError extends Error {
  code: string
  traceId: string

  constructor(code: string, message: string, traceId = '') {
    super(message)
    this.code = code
    this.traceId = traceId
  }
}

const ACCESS_KEY = 'nexplm.access'
const REFRESH_KEY = 'nexplm.refresh'
const BASE = '/api/v1'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

let refreshing: Promise<boolean> | null = null

async function doRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (!refreshToken) {
    return false
  }
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await fetch(`${BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
        if (!res.ok) {
          return false
        }
        const body = (await res.json()) as ApiResult<{ accessToken: string; refreshToken: string }>
        setTokens(body.data.accessToken, body.data.refreshToken)
        return true
      } catch {
        return false
      } finally {
        setTimeout(() => {
          refreshing = null
        }, 0)
      }
    })()
  }
  return refreshing
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Record<string, string | number | boolean | undefined>
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = `${BASE}${path}`
  if (!query) {
    return url
  }
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, String(value))
    }
  })
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  let body: BodyInit | undefined
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(options.body)
  }
  return fetch(buildUrl(path, options.query), {
    method: options.method ?? 'GET',
    headers,
    body
  })
}

async function unwrap<T>(response: Response): Promise<T> {
  let payload: ApiResult<T> | null = null
  try {
    payload = (await response.json()) as ApiResult<T>
  } catch {
    payload = null
  }
  if (!response.ok || !payload || payload.code !== 'OK') {
    const code = payload?.code ?? `HTTP.${response.status}`
    const message = payload?.message ?? `请求失败（${response.status}）`
    throw new ApiError(code, message, payload?.traceId)
  }
  return payload.data
}

/** 上传文件（multipart）。同一 content 接口用于单文件与分片场景。 */
export async function uploadFile<T>(
  path: string,
  file: File,
  query?: RequestOptions['query'],
  fieldName = 'file'
): Promise<T> {
  const form = new FormData()
  form.append(fieldName, file)
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  let response = await fetch(buildUrl(path, query), { method: 'POST', headers, body: form })
  if (response.status === 401 && (await doRefresh())) {
    const retryHeaders = { ...headers }
    const newToken = getAccessToken()
    if (newToken) {
      retryHeaders.Authorization = `Bearer ${newToken}`
    }
    response = await fetch(buildUrl(path, query), { method: 'POST', headers: retryHeaders, body: form })
  }
  return unwrap<T>(response)
}

/** 下载二进制内容（带令牌），返回 blob 与文件名。 */
export async function downloadFile(
  path: string,
  query?: RequestOptions['query']
): Promise<{ blob: Blob; fileName: string }> {
  const headers: Record<string, string> = {}
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(buildUrl(path, query), { headers })
  if (!response.ok) {
    throw new ApiError(`HTTP.${response.status}`, `下载失败（${response.status}）`)
  }
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition) ?? /filename="?([^";]+)"?/i.exec(disposition)
  const fileName = match ? decodeURIComponent(match[1]) : 'download'
  return { blob: await response.blob(), fileName }
}

/** 以 POST 提交条件并下载二进制（如批量打包下载）。 */
export async function downloadFileByPost(
  path: string,
  body: unknown
): Promise<{ blob: Blob; fileName: string }> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  let response = await fetch(`${BASE}${path}`, { method: 'POST', headers, body: JSON.stringify(body) })
  if (response.status === 401 && (await doRefresh())) {
    const newToken = getAccessToken()
    if (newToken) {
      headers.Authorization = `Bearer ${newToken}`
    }
    response = await fetch(`${BASE}${path}`, { method: 'POST', headers, body: JSON.stringify(body) })
  }
  if (!response.ok) {
    let message = `下载失败（${response.status}）`
    try {
      const payload = (await response.json()) as ApiResult<unknown>
      message = payload?.message ?? message
    } catch {
      // 二进制错误体忽略，使用默认提示
    }
    throw new ApiError(`HTTP.${response.status}`, message)
  }
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition) ?? /filename="?([^";]+)"?/i.exec(disposition)
  const fileName = match ? decodeURIComponent(match[1]) : 'download'
  return { blob: await response.blob(), fileName }
}

/** 触发浏览器保存。 */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await send(path, options)
  if (response.status === 401 && (await doRefresh())) {
    response = await send(path, options)
  }
  return unwrap<T>(response)
}

export const http = {
  get: <T>(path: string, query?: RequestOptions['query']) => request<T>(path, { query }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' })
}
