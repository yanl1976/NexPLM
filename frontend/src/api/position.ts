import { http } from './http'

/** 工位树节点（对齐旧系统 DEEPPOSITIONLINKVIEW）。 */
export interface PositionNode {
  id: string
  code: string
  name: string
  parentId: string | null
  positionLevel: string
  logisticsUnit: string | null
  capacity: number | null
  shifts: number | null
  enabled: boolean
  remark: string | null
  children: PositionNode[]
}

export interface PositionReq {
  code?: string
  name: string
  parentId?: string | null
  positionLevel?: string
  logisticsUnit?: string
  capacity?: number | null
  shifts?: number | null
  enabled?: boolean
  remark?: string
}

/** 消耗定义（对齐旧系统 CSOBJ）。 */
export interface ConsumptionRow {
  id: string
  code: string
  name: string | null
  positionId: string | null
  positionCode: string | null
  positionName: string | null
  objectId: string | null
  itemCode: string | null
  itemName: string | null
  qty: number
  unit: string | null
  refItem: boolean
  suitability: string | null
  fitProduct: string | null
  allRoute: boolean
  isPrimary: boolean
  remark: string | null
  createdAt: string | null
}

export const positionApi = {
  tree: () => http.get<PositionNode[]>('/capp/positions'),
  create: (body: PositionReq) => http.post<PositionNode>('/capp/positions', body),
  update: (id: string, body: PositionReq) => http.put<PositionNode>(`/capp/positions/${id}`, body),
  remove: (id: string) => http.delete<void>(`/capp/positions/${id}`),

  consumptions: (positionId?: string, keyword?: string) =>
    http.get<ConsumptionRow[]>('/capp/consumptions', { positionId, keyword }),
  createConsumption: (body: Record<string, unknown>) => http.post<string>('/capp/consumptions', body),
  updateConsumption: (id: string, body: Record<string, unknown>) =>
    http.put<void>(`/capp/consumptions/${id}`, body),
  removeConsumption: (id: string) => http.delete<void>(`/capp/consumptions/${id}`)
}
