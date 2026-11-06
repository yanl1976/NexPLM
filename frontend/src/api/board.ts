import { http } from './http'

/** 组件类型：metric 合计卡 / bar 横向条 / table 明细表。 */
export const BOARD_COMPONENT_LABEL: Record<string, string> = {
  metric: '合计卡',
  bar: '横向条形',
  table: '明细表'
}

export interface BoardComponent {
  id: string
  type: string
  title: string
  dataset: string
  top: number
  span: number
  order: number
}

export interface BoardRow {
  id: string
  code: string
  name: string
  description?: string | null
  shared: boolean
  componentCount: number
  updatedAt?: string | null
}

export interface BoardDetail {
  id: string
  code: string
  name: string
  description?: string | null
  shared: boolean
  components: BoardComponent[]
  createdAt?: string | null
  updatedAt?: string | null
}

export interface BoardSaveReq {
  code: string
  name: string
  description?: string | null
  shared?: boolean
  components: Array<{
    id?: string
    type: string
    title?: string
    dataset: string
    top?: number
    span?: number
  }>
}

export interface DatasetMeta {
  code: string
  label: string
  domain: string
}

export interface ComponentData {
  componentId: string
  type: string
  title: string
  dataset: string
  domain?: string | null
  total: number
  rows: Array<{ key: string; count: number }>
}

export interface BoardData {
  boardId: string
  boardName: string
  components: ComponentData[]
}

export const boardApi = {
  datasets(): Promise<DatasetMeta[]> {
    return http.get<DatasetMeta[]>('/ana/boards/datasets')
  },
  page(params: {
    keyword?: string
    page?: number
    size?: number
  }): Promise<{ items: BoardRow[]; total: number; page: number; size: number }> {
    return http.get('/ana/boards', params)
  },
  detail(id: string): Promise<BoardDetail> {
    return http.get<BoardDetail>(`/ana/boards/${id}`)
  },
  create(req: BoardSaveReq): Promise<BoardDetail> {
    return http.post<BoardDetail>('/ana/boards', req)
  },
  update(id: string, req: BoardSaveReq): Promise<BoardDetail> {
    return http.put<BoardDetail>(`/ana/boards/${id}`, req)
  },
  remove(id: string): Promise<void> {
    return http.delete<void>(`/ana/boards/${id}`)
  },
  data(id: string): Promise<BoardData> {
    return http.get<BoardData>(`/ana/boards/${id}/data`)
  }
}
