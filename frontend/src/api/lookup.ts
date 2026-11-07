import { http } from './http'

/** 扫码/手输编码后命中的对象（最小字段集）。 */
export interface LookupResult {
  id: string
  objectType: string
  code: string
  name: string
  revision?: number | null
  iteration?: number | null
  lifecycleState: string
  updatedAt?: string | null
}

/** 对象类型 → 可跳转的详情页（没有详情页的类型返回 null，由页面提示去 PC 端）。 */
export function detailRouteOf(objectType: string, id: string): string | null {
  if (objectType === 'PdmItem') {
    return `/item/${id}`
  }
  if (objectType === 'PdmDoc') {
    return `/doc/${id}`
  }
  return null
}

export const OBJECT_TYPE_LABEL: Record<string, string> = {
  PdmItem: '零部件',
  PdmDoc: '图文档',
  ChgEcr: '变更单',
  PdmBom: 'BOM'
}

export const lookupApi = {
  byCode(code: string): Promise<LookupResult[]> {
    return http.get<LookupResult[]>('/objects/lookup', { code })
  },
  /** 通用对象选择器数据源：编码/名称模糊检索，objectType 留空=跨类型。 */
  search(keyword?: string, objectType?: string, limit = 20): Promise<LookupResult[]> {
    return http.get<LookupResult[]>('/objects/search', { keyword, objectType, limit })
  }
}
