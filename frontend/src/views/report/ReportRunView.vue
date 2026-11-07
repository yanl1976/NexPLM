<script setup lang="ts">
/**
 * 运行报表（复刻批次 D）：参数表单 → 结果表格 → 导出 xlsx / PDF。
 *
 * 参数与列全部来自后端模板/数据集定义（`GET /ana/reports/{id}`），前端不硬编码任何字段，
 * 所以新建一张模板不需要改前端代码。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Printer, RefreshLeft } from '@element-plus/icons-vue'
import { saveBlob } from '@/api/http'
import {
  reportApi,
  type ReportColumn,
  type ReportParamMeta,
  type ReportRunResult,
  type ReportTemplateRow
} from '@/api/report'

const route = useRoute()
const router = useRouter()
const templateId = computed(() => String(route.params.id ?? ''))

const loading = ref(false)
const exporting = ref('')
const template = ref<ReportTemplateRow | null>(null)
const result = ref<ReportRunResult | null>(null)
const paramValues = reactive<Record<string, string>>({})
const page = ref(1)
const size = ref(50)

const params = computed<ReportParamMeta[]>(() => template.value?.params ?? [])
const columns = computed<ReportColumn[]>(() => result.value?.columns ?? [])
const total = computed(() => result.value?.total ?? 0)

onMounted(async () => {
  if (!templateId.value) {
    ElMessage.error('缺少报表 id')
    return
  }
  await loadTemplate()
  await run(1)
})

async function loadTemplate() {
  try {
    template.value = await reportApi.detail(templateId.value)
    params.value.forEach((param) => {
      if (!(param.key in paramValues)) {
        paramValues[param.key] = ''
      }
    })
  } catch (error) {
    ElMessage.error((error as Error).message || '加载报表失败')
  }
}

function currentParams(): Record<string, string> {
  const values: Record<string, string> = {}
  Object.keys(paramValues).forEach((key) => {
    const value = (paramValues[key] ?? '').trim()
    if (value) {
      values[key] = value
    }
  })
  return values
}

async function run(targetPage = 1) {
  if (!template.value) {
    return
  }
  page.value = targetPage
  loading.value = true
  try {
    result.value = await reportApi.run(templateId.value, {
      params: currentParams(),
      page: page.value,
      size: size.value
    })
  } catch (error) {
    ElMessage.error((error as Error).message || '运行报表失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.keys(paramValues).forEach((key) => {
    paramValues[key] = ''
  })
  run(1)
}

async function exportFile(kind: 'xlsx' | 'pdf') {
  if (!template.value) {
    return
  }
  exporting.value = kind
  try {
    const { blob, fileName } =
      kind === 'xlsx'
        ? await reportApi.exportXlsx(templateId.value, currentParams())
        : await reportApi.exportPdf(templateId.value, currentParams())
    saveBlob(blob, fileName)
    ElMessage.success(`已导出 ${kind.toUpperCase()}`)
  } catch (error) {
    ElMessage.error((error as Error).message || `导出 ${kind.toUpperCase()} 失败`)
  } finally {
    exporting.value = ''
  }
}

function back() {
  router.push({ name: 'report-templates' })
}

/** 结果列格式化：布尔转是/否，时间去掉 T 与毫秒，数字保留最多 4 位小数。 */
function display(column: ReportColumn, value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (column.type === 'BOOL') {
    return value === true || String(value).toLowerCase() === 'true' ? '是' : '否'
  }
  if (column.type === 'NUMBER' && typeof value === 'number') {
    return value.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
  }
  if (column.type === 'DATE' || column.type === 'DATETIME') {
    const text = String(value).replace('T', ' ')
    return column.type === 'DATE' ? text.slice(0, 10) : text.slice(0, 19)
  }
  return String(value)
}

function alignOf(column: ReportColumn): 'right' | 'left' | 'center' {
  if (column.type === 'NUMBER') {
    return 'right'
  }
  return column.type === 'BOOL' ? 'center' : 'left'
}
</script>

<template>
  <div class="run-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="head-left">
            <el-button link :icon="RefreshLeft" @click="back">返回报表管理</el-button>
            <span class="title">{{ template?.name || '运行报表' }}</span>
            <el-tag v-if="template" size="small" type="info">{{ template.code }}</el-tag>
            <el-tag v-if="template" size="small">{{ template.datasetLabel }}</el-tag>
          </div>
          <div class="actions">
            <el-button :loading="exporting === 'xlsx'" :icon="Download" @click="exportFile('xlsx')">
              导出 Excel
            </el-button>
            <el-button :loading="exporting === 'pdf'" :icon="Printer" @click="exportFile('pdf')">
              导出 PDF
            </el-button>
          </div>
        </div>
      </template>

      <el-form v-if="params.length" :inline="true" class="param-form" @submit.prevent>
        <el-form-item v-for="param in params" :key="param.key" :label="param.label">
          <el-select
            v-if="param.type === 'SELECT'"
            v-model="paramValues[param.key]"
            clearable
            placeholder="全部"
            style="width: 170px"
          >
            <el-option
              v-for="option in param.options ?? []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-date-picker
            v-else-if="param.type === 'DATE'"
            v-model="paramValues[param.key]"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 170px"
          />
          <el-input
            v-else
            v-model="paramValues[param.key]"
            clearable
            placeholder="不填为全部"
            style="width: 190px"
            @keyup.enter="run(1)"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="run(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-else
        type="info"
        :closable="false"
        show-icon
        title="这张报表没有配置筛选参数，直接展示全部数据"
        class="no-param"
      />

      <div class="summary">
        <span>共 {{ total }} 条</span>
        <span v-if="template?.description" class="muted">{{ template.description }}</span>
      </div>

      <el-table v-loading="loading" :data="result?.rows ?? []" border size="small" empty-text="没有符合条件的数据">
        <el-table-column
          v-for="column in columns"
          :key="column.key"
          :label="column.label"
          :min-width="Math.max(90, column.label.length * 16)"
          :align="alignOf(column)"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ display(column, row[column.key]) }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[50, 100, 200, 500]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="run"
          @size-change="run(1)"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.run-page {
  padding: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.head-left .title {
  font-size: 15px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.param-form {
  margin-bottom: 4px;
}

.no-param {
  margin-bottom: 8px;
}

.summary {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 6px 0 10px;
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
