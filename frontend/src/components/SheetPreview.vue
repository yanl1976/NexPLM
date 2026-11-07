<template>
  <div class="sheet-wrap">
    <div v-if="loading" class="sheet-mask">正在解析表格…</div>
    <el-empty v-else-if="error" :description="error">
      <el-button type="primary" @click="onDownload">下载文件</el-button>
    </el-empty>
    <el-tabs v-else-if="sheets.length" v-model="active" type="border-card" class="sheet-tabs">
      <el-tab-pane v-for="sheet in sheets" :key="sheet.name" :name="sheet.name">
        <template #label>
          <span>{{ sheet.name }}</span>
          <span class="sheet-count">{{ sheet.totalRows }} 行</span>
        </template>
        <div v-if="sheet.rows.length === 0" class="sheet-empty">该工作表无数据</div>
        <template v-else>
          <el-alert
            v-if="sheet.truncated"
            type="info"
            :closable="false"
            show-icon
            class="sheet-tip"
            :title="`仅显示前 ${MAX_ROWS} 行（共 ${sheet.totalRows} 行），完整数据请下载原文件`"
          />
          <el-table :data="sheet.rows" height="560" border size="small" class="sheet-table">
            <el-table-column
              v-for="col in sheet.columns"
              :key="col.prop"
              :prop="col.prop"
              :label="col.label"
              :width="col.width"
              show-overflow-tooltip
            />
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else :description="'该表格没有可显示的工作表'">
      <el-button type="primary" @click="onDownload">下载文件</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
/**
 * 电子表格（.xlsx/.xlsm/.xls）在线预览。
 *
 * 走纯前端解析：SheetJS 把工作簿读成二维数组，再用 el-table 分 sheet 渲染。
 * 不经过转换代理，因而无需 Windows 转换节点与商业授权。
 *
 * 技术要点：
 * - 依赖用动态 import 拆包（SheetJS 体积较大，不进首屏）；
 * - 取首行作表头，表头为空/重复时回落到列序号，避免 el-table 的 prop 冲突；
 * - 只渲染前 MAX_ROWS 行、前 MAX_COLS 列，防止超大宽表把主线程与内存打满；
 * - cellDates 让日期单元格读成 Date 对象，展示前统一格式化，避免出现 45000 这类序列号。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { downloadFile, saveBlob } from '../api/http'

const props = defineProps<{
  fileId: string
  fileName?: string
}>()

const MAX_ROWS = 1000
const MAX_COLS = 80
const MAX_SIZE = 30 * 1024 * 1024

interface ColumnDef {
  prop: string
  label: string
  width: number
}

interface SheetData {
  name: string
  columns: ColumnDef[]
  rows: Record<string, string>[]
  totalRows: number
  truncated: boolean
}

const sheets = ref<SheetData[]>([])
const active = ref('')
const loading = ref(true)
const error = ref('')

/** 中文按 2 个字符宽度估算，用于列宽自适应。 */
function displayWidth(value: string): number {
  let width = 0
  for (const ch of value) {
    width += ch.charCodeAt(0) > 255 ? 2 : 1
  }
  return width
}

function formatCell(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }
  if (value instanceof Date) {
    const pad = (n: number): string => String(n).padStart(2, '0')
    const date = `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
    const time = `${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
    return time === '00:00:00' ? date : `${date} ${time}`
  }
  return String(value)
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  sheets.value = []
  try {
    const { blob } = await downloadFile(`/files/${props.fileId}/content`)
    if (blob.size > MAX_SIZE) {
      throw new Error(`文件 ${(blob.size / 1024 / 1024).toFixed(1)}MB，超过在线预览上限 30MB，请下载后查看`)
    }
    const buffer = await blob.arrayBuffer()
    const { read, utils } = await import('xlsx')
    const workbook = read(buffer, { type: 'array', cellDates: true })

    const parsed: SheetData[] = []
    for (const name of workbook.SheetNames) {
      const worksheet = workbook.Sheets[name]
      if (!worksheet) {
        continue
      }
      // raw:true 必须保留：raw:false 会让日期单元格输出成 Excel 的区域化短日期串（如 11/7/26），
      // 既无法统一成 yyyy-MM-dd，也会随浏览器区域设置漂移。保持原始值由 formatCell 统一格式化。
      const matrix = utils.sheet_to_json<unknown[]>(worksheet, {
        header: 1,
        raw: true,
        defval: '',
        blankrows: false
      })
      if (matrix.length === 0) {
        parsed.push({ name, columns: [], rows: [], totalRows: 0, truncated: false })
        continue
      }

      const totalRows = Math.max(matrix.length - 1, 0)
      const headerRow = (matrix[0] ?? []).map((cell, index) => {
        const label = formatCell(cell).trim()
        return label === '' ? `列${index + 1}` : label
      })
      const colCount = Math.min(Math.max(headerRow.length, 1), MAX_COLS)

      // 表头去重：同名表头会导致 el-table 的 prop 冲突
      const seen = new Map<string, number>()
      const columns: ColumnDef[] = []
      for (let index = 0; index < colCount; index += 1) {
        const base = headerRow[index] ?? `列${index + 1}`
        const hit = seen.get(base) ?? 0
        seen.set(base, hit + 1)
        columns.push({
          prop: `c${index}`,
          label: hit === 0 ? base : `${base}(${hit + 1})`,
          width: 120
        })
      }

      const bodyRows = matrix.slice(1, MAX_ROWS + 1)
      const rows = bodyRows.map((line) => {
        const record: Record<string, string> = {}
        for (let index = 0; index < colCount; index += 1) {
          record[`c${index}`] = formatCell((line ?? [])[index])
        }
        return record
      })

      // 列宽按表头与样本单元格的最大显示宽度估算，限制在 80~320 之间
      const sample = bodyRows.slice(0, 50)
      for (let index = 0; index < colCount; index += 1) {
        let maxWidth = displayWidth(columns[index].label)
        for (const line of sample) {
          const width = displayWidth(formatCell((line ?? [])[index]))
          if (width > maxWidth) {
            maxWidth = width
          }
        }
        columns[index].width = Math.min(Math.max(maxWidth * 8 + 24, 80), 320)
      }

      parsed.push({
        name,
        columns,
        rows,
        totalRows,
        truncated: totalRows > MAX_ROWS
      })
    }

    sheets.value = parsed
    active.value = parsed.find((sheet) => sheet.rows.length > 0)?.name ?? parsed[0]?.name ?? ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '表格解析失败，请下载后本地查看'
  } finally {
    loading.value = false
  }
}

async function onDownload(): Promise<void> {
  try {
    const { blob, fileName } = await downloadFile(`/files/${props.fileId}/content`)
    saveBlob(blob, fileName || props.fileName || 'workbook.xlsx')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载失败'
  }
}

onMounted(load)

onBeforeUnmount(() => {
  sheets.value = []
})
</script>

<style scoped>
.sheet-wrap {
  width: 100%;
  min-height: 520px;
}

.sheet-tabs {
  background: #fff;
}

.sheet-count {
  margin-left: 6px;
  color: #909399;
  font-size: 11px;
}

.sheet-tip {
  margin-bottom: 8px;
}

.sheet-empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.sheet-mask {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 520px;
  color: #606266;
  background: #f2f5f9;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
