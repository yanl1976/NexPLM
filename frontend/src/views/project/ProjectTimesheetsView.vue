<template>
  <div class="page">
    <div class="toolbar">
      <el-button link type="primary" @click="back">← 返回项目列表</el-button>
      <span class="prj-title">{{ summary?.projectCode }} {{ summary?.projectName }}</span>
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="false"
        style="width: 260px"
        @change="loadAll"
      />
      <el-button @click="loadAll">刷新</el-button>
      <el-button type="success" plain @click="openTasks">WBS 任务</el-button>
      <span class="hint">实际工时只认「已通过」，待审批单列；负荷 = 已审批工时 / (周数 × 40h)</span>
    </div>

    <div class="stat-row">
      <el-card shadow="never" class="stat">
        <div class="stat-label">已通过工时</div>
        <div class="stat-value success">{{ summary?.approvedHours ?? 0 }} h</div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">待审批工时</div>
        <div class="stat-value" :class="{ warning: (summary?.pendingHours ?? 0) > 0 }">
          {{ summary?.pendingHours ?? 0 }} h
        </div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">已驳回工时</div>
        <div class="stat-value" :class="{ danger: (summary?.rejectedHours ?? 0) > 0 }">
          {{ summary?.rejectedHours ?? 0 }} h
        </div>
      </el-card>
      <el-card shadow="never" class="stat">
        <div class="stat-label">已通过 / 待审批（条）</div>
        <div class="stat-value">{{ summary?.approvedCount ?? 0 }} / {{ summary?.pendingCount ?? 0 }}</div>
      </el-card>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="工时明细" name="detail">
        <div class="toolbar inner">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="loadPage">
            <el-option v-for="(label, value) in TIMESHEET_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button v-if="canApprove" type="primary" :disabled="!selected.length" @click="onBatchApprove">
            批量通过（{{ selected.length }}）
          </el-button>
          <span class="hint">选中多条待审批记录可一次通过；不能审批自己填报的工时</span>
        </div>

        <el-table v-loading="loading" :data="rows" border stripe @selection-change="onSelectionChange">
          <el-table-column v-if="canApprove" type="selection" width="46" :selectable="isSelectable" />
          <el-table-column prop="workDate" label="日期" width="115" />
          <el-table-column prop="userName" label="填报人" width="120" />
          <el-table-column label="任务" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ row.taskWbsCode }} {{ row.taskName }}</template>
          </el-table-column>
          <el-table-column prop="hours" label="工时(h)" width="90" align="right" />
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">{{ TIMESHEET_WORK_TYPE_LABEL[row.workType] ?? row.workType }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ TIMESHEET_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column label="审批" width="160">
            <template #default="{ row }">
              <span v-if="row.status === 'APPROVED'">{{ row.approverName }} {{ shortDate(row.approvedAt) }}</span>
              <span v-else-if="row.status === 'REJECTED'" class="danger-text">{{ row.rejectReason }}</span>
              <span v-else class="sub">-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="canApprove" label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'SUBMITTED'">
                <el-button link type="success" @click="onApprove(row)">通过</el-button>
                <el-button link type="danger" @click="onReject(row)">打回</el-button>
              </template>
              <span v-else class="sub">-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !rows.length" description="该期间没有工时记录" :image-size="60" />
        <el-pagination
          v-if="total > size"
          class="pager"
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </el-tab-pane>

      <el-tab-pane label="按人汇总" name="user">
        <el-table :data="summary?.byUser ?? []" border stripe>
          <el-table-column prop="userName" label="人员" min-width="140" />
          <el-table-column prop="approvedHours" label="已通过(h)" width="130" align="right" />
          <el-table-column prop="pendingHours" label="待审批(h)" width="130" align="right" />
          <el-table-column label="合计(h)" width="120" align="right">
            <template #default="{ row }">{{ round(row.approvedHours + row.pendingHours) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="按任务汇总" name="task">
        <el-table :data="summary?.byTask ?? []" border stripe>
          <el-table-column prop="wbsCode" label="WBS" width="100" />
          <el-table-column prop="taskName" label="任务名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="approvedHours" label="已通过(h)" width="130" align="right" />
          <el-table-column prop="pendingHours" label="待审批(h)" width="130" align="right" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="资源负荷" name="load">
        <el-table :data="load?.rows ?? []" border stripe>
          <el-table-column prop="userName" label="人员" min-width="140" />
          <el-table-column prop="approvedHours" label="已通过工时(h)" width="150" align="right" />
          <el-table-column label="负荷率（容量 {{ load?.capacityHours ?? 0 }}h）" min-width="240">
            <template #default="{ row }">
              <el-progress
                :percentage="loadRate(row.approvedHours)"
                :status="loadRate(row.approvedHours) > 100 ? 'exception' : undefined"
                :stroke-width="12"
              />
            </template>
          </el-table-column>
          <el-table-column label="按周分布" min-width="260">
            <template #default="{ row }">
              <span v-for="week in row.weeks" :key="week.weekStart" class="week-chip">
                {{ week.weekStart }}：{{ week.approvedHours }}h
              </span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(load?.rows.length ?? 0)" description="该期间没有已审批工时" :image-size="60" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  timesheetApi,
  TIMESHEET_STATUS_LABEL,
  TIMESHEET_WORK_TYPE_LABEL,
  type ResourceLoad,
  type TimesheetRow,
  type TimesheetStatus,
  type TimesheetSummary
} from '@/api/project'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canApprove = computed(() => userStore.can('prj.project.write'))

const projectId = String(route.params.id ?? '')
const tab = ref<'detail' | 'user' | 'task' | 'load'>('detail')
const loading = ref(false)
const rows = ref<TimesheetRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusFilter = ref('')
const selected = ref<TimesheetRow[]>([])
const summary = ref<TimesheetSummary | null>(null)
const load = ref<ResourceLoad | null>(null)
const range = ref<[string, string]>([monthStart(), today()])

onMounted(loadAll)

async function loadAll(): Promise<void> {
  await Promise.all([loadPage(), loadSummary(), loadResource()])
}

async function loadPage(): Promise<void> {
  loading.value = true
  try {
    const result = await timesheetApi.page({
      projectId,
      status: statusFilter.value || undefined,
      from: range.value?.[0],
      to: range.value?.[1],
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工时列表失败')
  } finally {
    loading.value = false
  }
}

async function loadSummary(): Promise<void> {
  try {
    summary.value = await timesheetApi.summary(projectId, range.value?.[0], range.value?.[1])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载工时汇总失败')
  }
}

async function loadResource(): Promise<void> {
  try {
    load.value = await timesheetApi.resourceLoad(projectId, range.value?.[0], range.value?.[1])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资源负荷失败')
  }
}

function onPageChange(value: number): void {
  page.value = value
  void loadPage()
}

function onSelectionChange(list: TimesheetRow[]): void {
  selected.value = list
}

function isSelectable(row: TimesheetRow): boolean {
  return row.status === 'SUBMITTED'
}

async function onApprove(row: TimesheetRow): Promise<void> {
  try {
    await timesheetApi.approve(row.id)
    ElMessage.success('已通过')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审批失败')
  }
}

async function onReject(row: TimesheetRow): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('打回原因（必填）', `打回 ${row.userName} 的工时`, {
      inputValidator: (value: string) => (value && value.trim() ? true : '请填写打回原因'),
      confirmButtonText: '打回',
      cancelButtonText: '取消'
    })
    reason = (result as { value?: string }).value?.trim() ?? ''
  } catch {
    return
  }
  try {
    await timesheetApi.reject(row.id, reason)
    ElMessage.success('已打回')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打回失败')
  }
}

async function onBatchApprove(): Promise<void> {
  const ids = selected.value.map((item) => item.id)
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认通过选中的 ${ids.length} 条工时？整批原子，任一失败全部回滚`, '批量通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const result = await timesheetApi.batchApprove(ids)
    ElMessage.success(`已通过 ${result.approved} 条`)
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量通过失败')
  }
}

function loadRate(hours: number): number {
  const capacity = load.value?.capacityHours ?? 0
  if (!capacity) {
    return 0
  }
  return Math.round((hours / capacity) * 100)
}

function statusTagType(value: TimesheetStatus): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'APPROVED') {
    return 'success'
  }
  if (value === 'SUBMITTED') {
    return 'warning'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

function back(): void {
  router.push('/project')
}

function openTasks(): void {
  router.push(`/project/${projectId}/tasks`)
}

function round(value: number): number {
  return Math.round(value * 100) / 100
}

function shortDate(value?: string | null): string {
  return value ? value.slice(0, 10) : ''
}

function today(): string {
  return formatDate(new Date())
}

function monthStart(): string {
  const now = new Date()
  return formatDate(new Date(now.getFullYear(), now.getMonth(), 1))
}

function formatDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>

<style scoped>
.stat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.stat {
  flex: 1;
}

.stat-label {
  color: #909399;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 600;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.toolbar.inner {
  margin-bottom: 8px;
}

.danger-text {
  color: #f56c6c;
}

.week-chip {
  display: inline-block;
  margin-right: 10px;
  color: #606266;
  font-size: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
