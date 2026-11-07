import { metaApi, type MetaFieldDef } from '@/api/system'

/**
 * 零部件 ERP 客制字段（批次 G·方案 A）的唯一前端实现。
 *
 * <p>字段集合 = PdmItem 元数据 − 标准键 − 设计属性键，与后端
 * `ItemService.STANDARD_EXT_KEYS / DESIGN_ATTR_KEYS` 的排除算法一致；
 * 值落 `plm_object.ext_json`，与分类属性（`classAttrs` 嵌套键）互不覆盖。</p>
 *
 * <p>提交语义（T-G-02 决策）：
 * 新建 = 提交用户填写的全部值（布尔仅显式选择时提交）；
 * 编辑 = 只提交改动过的键，未改动不提交＝后端保留既有值，文本清空提交空串＝清空。</p>
 *
 * <p>取证依据：`docs/06` EXP-20261107-22（前端入口设计）、EXP-20261107-21（后端落点）。</p>
 */

/** 由专用字段维护的标准键，不允许经 `ext` 覆盖。 */
export const STANDARD_EXT_KEYS = new Set([
  'itemType',
  'sourceType',
  'unit',
  'specification',
  'material',
  'weight',
  'stdCode',
  'remark'
])

/** 设计属性键：落 `item_design_attr`，不属于客制字段。 */
export const DESIGN_ATTR_KEYS = new Set([
  'provider',
  'dwgNo',
  'productCategory',
  'productModel',
  'productSpec',
  'materialType',
  'materialType2',
  'surfaceTreatment',
  'isTooling',
  'weightUnit',
  'montage',
  'colorItem'
])

/** 元数据接口不可用时的兜底（与 Flyway V70 注册的 12 个字段一致）。 */
export const ITEM_CUSTOM_FIELD_FALLBACK: MetaFieldDef[] = [
  { code: 'FErpClsID', name: '物料属性', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 210 },
  { code: 'FCategoryID', name: '存货类别', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 220 },
  { code: 'FPlanningStrategy', name: '计划策略', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 230 },
  { code: 'FIsProduce', name: '允许生产', valueType: 'BoolValue', dataType: 'BOOLEAN', sort: 240 },
  { code: 'FIsPurchase', name: '允许采购', valueType: 'BoolValue', dataType: 'BOOLEAN', sort: 250 },
  { code: 'FIsInventory', name: '允许库存', valueType: 'BoolValue', dataType: 'BOOLEAN', sort: 260 },
  { code: 'FIsSubContract', name: '允许委外', valueType: 'BoolValue', dataType: 'BOOLEAN', sort: 270 },
  { code: 'FIsSale', name: '允许销售', valueType: 'BoolValue', dataType: 'BOOLEAN', sort: 280 },
  { code: 'F_ZXCV_ZYCS', name: '主要参数', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 290 },
  { code: 'F_zxcv_SCCS', name: '生产厂名', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 300 },
  { code: 'F_PJQF_XMDA', name: '项目代号', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 310 },
  { code: 'F_zxcv_JSCS', name: '技术参数', valueType: 'DefaultObjectValue', dataType: 'VARCHAR', sort: 320 }
]

/** 客制字段：元数据减去标准键与设计属性键，按 sort 排序。 */
export function buildCustomFields(metaFields: MetaFieldDef[]): MetaFieldDef[] {
  const source = metaFields.length ? metaFields : ITEM_CUSTOM_FIELD_FALLBACK
  return source
    .filter((field) => !STANDARD_EXT_KEYS.has(field.code) && !DESIGN_ATTR_KEYS.has(field.code))
    .slice()
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
}

/** 拉取 PdmItem 元数据并返回客制字段；接口异常时回退到兜底清单。 */
export async function loadItemCustomFields(): Promise<MetaFieldDef[]> {
  try {
    const entity = await metaApi.getEntity('PdmItem')
    return buildCustomFields(entity.fields ?? [])
  } catch {
    return buildCustomFields([])
  }
}

export function isBoolField(field: MetaFieldDef): boolean {
  return field.valueType === 'BoolValue' || field.dataType === 'BOOLEAN'
}

function toBool(raw: unknown): boolean | null {
  if (raw === true || raw === 'true' || raw === 1 || raw === '1') {
    return true
  }
  if (raw === false || raw === 'false' || raw === 0 || raw === '0') {
    return false
  }
  return null
}

/** 只读展示：未填显示 -，布尔转是/否。 */
export function extValueText(ext: Record<string, unknown> | null | undefined, field: MetaFieldDef): string {
  const raw = ext?.[field.code]
  if (raw === null || raw === undefined || raw === '') {
    return '-'
  }
  const boolValue = toBool(raw)
  if (boolValue !== null) {
    return boolValue ? '是' : '否'
  }
  return String(raw)
}

/** 回填表单：文本进 text 记录，布尔进三态记录（`''` = 未设置）。 */
export function pickExtValues(
  fields: MetaFieldDef[],
  ext: Record<string, unknown> | null | undefined
): { text: Record<string, string>; bool: Record<string, boolean | ''> } {
  const text: Record<string, string> = {}
  const bool: Record<string, boolean | ''> = {}
  fields.forEach((field) => {
    const raw = ext?.[field.code]
    if (isBoolField(field)) {
      bool[field.code] = toBool(raw) ?? ''
    } else {
      text[field.code] = raw === null || raw === undefined ? '' : String(raw)
    }
  })
  return { text, bool }
}

/**
 * 编辑：只提交相对原始值发生变化的键。
 * 未改动不提交（后端保留既有值）；文本清空提交空串（清空该键）；布尔选"未设置"且原先有值则提交空串。
 */
export function buildErpPatch(
  fields: MetaFieldDef[],
  text: Record<string, string>,
  bool: Record<string, boolean | ''>,
  original: Record<string, unknown>
): Record<string, unknown> {
  const patch: Record<string, unknown> = {}
  fields.forEach((field) => {
    const origin = original[field.code]
    if (isBoolField(field)) {
      const next = bool[field.code] ?? ''
      if (next === '') {
        if (origin !== null && origin !== undefined && origin !== '') {
          patch[field.code] = ''
        }
        return
      }
      if (toBool(origin) !== next) {
        patch[field.code] = next
      }
    } else {
      const next = (text[field.code] ?? '').trim()
      const originText = origin === null || origin === undefined ? '' : String(origin).trim()
      if (next !== originText) {
        patch[field.code] = next
      }
    }
  })
  return patch
}

/**
 * 新建：提交用户填写的全部值。
 * 布尔"未设置"不提交（缺键 = 由金蝶按默认带出，不能静默写成 false）；文本空值不提交。
 */
export function buildErpValues(
  fields: MetaFieldDef[],
  text: Record<string, string>,
  bool: Record<string, boolean | ''>
): Record<string, unknown> {
  const values: Record<string, unknown> = {}
  fields.forEach((field) => {
    if (isBoolField(field)) {
      const next = bool[field.code]
      if (next === true || next === false) {
        values[field.code] = next
      }
      return
    }
    const next = (text[field.code] ?? '').trim()
    if (next !== '') {
      values[field.code] = next
    }
  })
  return values
}
