<script setup lang="ts">
/**
 * 编码管理。对齐旧系统「业务管理-编码管理」（InforCenter/Coding）：
 * 旧系统 13 个页面维护的其实是同一件事——**编码由哪些段拼成、流水走到几、什么时候归零**。
 * 这里收敛成一张规则表 + 两个对话框（编辑段定义 / 重置跳号）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Delete } from '@element-plus/icons-vue'
import { codingApi } from '@/api/coding'
import type { CodeIssueRow, CodeRule, CodeSegment } from '@/api/coding'

const loading = ref(false)
const rules = ref<CodeRule[]>([])

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<CodeRule | null>(null)
const form = ref<{
  name: string
  resetPolicy: string
  enabled: boolean
  remark: string
  segments: CodeSegment[]
}>({ name: '', resetPolicy: 'NEVER', enabled: true, remark: '', segments: [] })

const RESET_POLICIES = [
  { value: 'NEVER', label: '不归零' },
  { value: 'YEAR', label: '按年归零' },
  { value: 'MONTH', label: '按月归零' },
  { value: 'DAY', label: '按日归零' }
]

const SEGMENT_TYPES = [
  { value: 'TEXT', label: '固定文本' },
  { value: 'DATE', label: '日期' },
  { value: 'SEQ', label: '流水码' }
]

const DATE_FORMATS = [
  { value: 'yyyy', label: '年（yyyy）' },
  { value: 'yyyyMM', label: '年月（yyyyMM）' },
  { value: 'yyyyMMdd', label: '年月日（yyyyMMdd）' }
]

const RADIX = [
  { value: 'DECIMAL', label: '0-9' },
  { value: 'ALNUM', label: '0-9A-Z' }
]

/** 段定义拼成可读形式：固定文本/日期/流水码(位数,进制)。 */
function describe(segments: CodeSegment[]) {
  return segments
    .map((s) => {
      if (s.type === 'TEXT') return s.value || '∅'
      if (s.type === 'DATE') return `{${s.format || 'yyyyMM'}}`
      if (s.type === 'SEQ') return `{流水${s.width ?? 6}位·${s.radix === 'ALNUM' ? '36进制' : '10进制'}}`
      return s.type
    })
    .join(' + ')
}

const seqCount = computed(() => form.value.segments.filter((s) => s.type === 'SEQ').length)

function openEdit(row: CodeRule) {
  editing.value = row
  form.value = {
    name: row.name,
    resetPolicy: row.resetPolicy,
    enabled: row.enabled,
    remark: row.remark || '',
    segments: row.segments.map((s) => ({ ...s }))
  }
  dialogVisible.value = true
}

function addSegment() {
  form.value.segments.push({ type: 'TEXT', value: '' })
}

function removeSegment(index: number) {
  form.value.segments.splice(index, 1)
}

function onTypeChange(segment: CodeSegment) {
  if (segment.type === 'TEXT') {
    segment.value = segment.value || ''
    segment.format = null
    segment.width = null
    segment.radix = null
  } else if (segment.type === 'DATE') {
    segment.format = segment.format || 'yyyyMM'
    segment.value = null
    segment.width = null
    segment.radix = null
  } else if (segment.type === 'SEQ') {
    segment.width = segment.width ?? 6
    segment.radix = segment.radix || 'DECIMAL'
    segment.value = null
    segment.format = null
  }
}

async function save() {
  if (!editing.value) return
  if (seqCount.value !== 1) {
    ElMessage.warning('编码规则必须且只能有一个流水段')
    return
  }
  saving.value = true
  try {
    await codingApi.update(editing.value.code, {
      name: form.value.name,
      resetPolicy: form.value.resetPolicy,
      enabled: form.value.enabled,
      remark: form.value.remark,
      segments: form.value.segments.map((s) => ({
        type: s.type,
        value: s.value ?? null,
        format: s.format ?? null,
        width: s.width ?? null,
        radix: s.radix ?? null
      }))
    })
    ElMessage.success('已保存，规则对下次取号立即生效')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function reset(row: CodeRule) {
  const { value } = await ElMessageBox.prompt(
    `把「${row.code}」的流水重置到（当前 ${row.currentNo}）。跳号属审计事项，请填写原因。`,
    '重置 / 跳号',
    {
      inputPlaceholder: '目标流水号，如 0',
      inputValidator: (val) => /^\d+$/.test(val || '') || '请输入非负整数',
      inputValue: '0'
    }
  )
  const { value: reason } = await ElMessageBox.prompt('重置原因（会写入审计）', '重置原因', {
    inputPlaceholder: '如：年度切换 / 历史数据导入',
    inputValidator: (val) => (val || '').trim().length > 0 || '原因必填'
  })
  await codingApi.reset(row.code, { toNo: Number(value), reason })
  ElMessage.success('已重置')
  await load()
}

async function load() {
  loading.value = true
  try {
    rules.value = await codingApi.rules()
  } catch (error) {
    ElMessage.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

/** 后端返回 ISO 8601（带时区）；只保留到分钟，避免窄列换行。 */
function formatTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

// ---------------- 发放流水（docs/15 T-CORE-02 的 UI 收口） ----------------
const issueVisible = ref(false)
const issueLoading = ref(false)
const issueRule = ref<CodeRule | null>(null)
const issueRows = ref<CodeIssueRow[]>([])
const issueTotal = ref(0)
const issuePage = ref(1)
const issueSize = ref(20)
const issueKeyword = ref('')

async function loadIssues(): Promise<void> {
  if (!issueRule.value) {
    return
  }
  issueLoading.value = true
  try {
    const result = await codingApi.issues({
      ruleCode: issueRule.value.code,
      code: issueKeyword.value.trim() || undefined,
      page: issuePage.value,
      size: issueSize.value
    })
    issueRows.value = result.items
    issueTotal.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message || '加载流水失败')
  } finally {
    issueLoading.value = false
  }
}

function openIssues(row: CodeRule): void {
  issueRule.value = row
  issueKeyword.value = ''
  issuePage.value = 1
  issueVisible.value = true
  loadIssues()
}

function onIssueSearch(): void {
  issuePage.value = 1
  loadIssues()
}

function onIssuePageChange(page: number): void {
  issuePage.value = page
  loadIssues()
}

onMounted(load)
</script>

<template>
  <div class="coding-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="title">编码管理</span>
            <span class="hint">编码 = 固定文本 + 日期 + 流水码，改动对下次取号立即生效</span>
          </div>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rules" size="small" border row-key="id">
        <el-table-column prop="code" label="规则编码" width="170" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="objectType" label="对象类型" width="140" />
        <el-table-column label="段定义" min-width="260">
          <template #default="{ row }">
            <span class="segments">{{ describe(row.segments) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归零策略" width="110">
          <template #default="{ row }">
            {{ RESET_POLICIES.find((p) => p.value === row.resetPolicy)?.label || row.resetPolicy }}
          </template>
        </el-table-column>
        <el-table-column prop="currentNo" label="当前流水" width="100" align="right" />
        <el-table-column label="下一个编码" width="180">
          <template #default="{ row }">
            <el-tag type="success" effect="plain" size="small">{{ row.preview }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="reset(row)">重置</el-button>
            <el-button link type="info" @click="openIssues(row)">流水</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发放流水：蓝本 CODERES 的等价物，回答"这个码是什么时候发给谁的" -->
    <el-drawer
      v-model="issueVisible"
      :title="`发放流水：${issueRule?.name ?? ''}（${issueRule?.code ?? ''}）`"
      size="620px"
    >
      <div class="issue-toolbar">
        <el-input
          v-model="issueKeyword"
          placeholder="按编码搜索"
          clearable
          style="width: 220px"
          @keyup.enter="onIssueSearch"
          @clear="onIssueSearch"
        />
        <el-button type="primary" plain @click="onIssueSearch">查询</el-button>
        <span class="issue-hint">共 {{ issueTotal }} 条</span>
      </div>
      <el-table v-loading="issueLoading" :data="issueRows" size="small" border>
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="objectType" label="对象类型" width="130">
          <template #default="{ row }">{{ row.objectType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="issuedByName" label="操作人" width="110">
          <template #default="{ row }">{{ row.issuedByName || '-' }}</template>
        </el-table-column>
        <el-table-column label="发放时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.issuedAt) }}</template>
        </el-table-column>
        <template #empty>
          <span class="issue-hint">该规则还没有发放记录</span>
        </template>
      </el-table>
      <div class="issue-pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="issueTotal"
          :page-size="issueSize"
          :current-page="issuePage"
          @current-change="onIssuePageChange"
        />
      </div>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="`编辑编码规则：${editing?.code ?? ''}`" width="780px">
      <el-form label-width="96px">
        <el-form-item label="名称">
          <el-input v-model="form.name" style="width: 260px" />
        </el-form-item>
        <el-form-item label="归零策略">
          <el-select v-model="form.resetPolicy" style="width: 180px">
            <el-option v-for="p in RESET_POLICIES" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
          <el-switch v-model="form.enabled" active-text="启用" style="margin-left: 16px" />
        </el-form-item>
        <el-form-item label="段定义">
          <div class="segments-editor">
            <div v-for="(seg, index) in form.segments" :key="index" class="segment-row">
              <el-tag type="info" size="small">{{ index + 1 }}</el-tag>
              <el-select v-model="seg.type" style="width: 120px" @change="onTypeChange(seg)">
                <el-option v-for="t in SEGMENT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
              <el-input
                v-if="seg.type === 'TEXT'"
                v-model="seg.value"
                placeholder="固定文本，如 RT"
                style="width: 180px"
              />
              <el-select v-if="seg.type === 'DATE'" v-model="seg.format" style="width: 180px">
                <el-option v-for="f in DATE_FORMATS" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
              <template v-if="seg.type === 'SEQ'">
                <el-input-number v-model="seg.width" :min="1" :max="12" controls-position="right" style="width: 130px" />
                <el-select v-model="seg.radix" style="width: 130px">
                  <el-option v-for="r in RADIX" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
              </template>
              <el-button link type="danger" :icon="Delete" @click="removeSegment(index)">删除</el-button>
            </div>
            <div class="segment-actions">
              <el-button :icon="Plus" size="small" @click="addSegment">添加段</el-button>
              <span class="preview-hint" :class="{ warn: seqCount !== 1 }">
                必须且只能有一个流水段（当前 {{ seqCount }} 个）
              </span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.coding-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px;
}

/* 发放流水抽屉：搜索行 + 分页行 */
.issue-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.issue-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.issue-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
  margin-right: 12px;
}

.card-header .hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.segments {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.segments-editor {
  width: 100%;
}

.segment-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.segment-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.preview-hint.warn {
  color: var(--el-color-danger);
}
</style>
