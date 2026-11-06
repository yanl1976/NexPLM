<template>
  <div class="page">
    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="monthrange"
        value-format="YYYY-MM"
        start-placeholder="起始月"
        end-placeholder="结束月"
        style="width: 260px"
        @change="reload"
      />
      <el-button type="primary" @click="reload">刷新</el-button>
      <span class="hint">关闭率按全量快照计算（不随期间变化）；趋势按月统计登记数与关闭数</span>
    </div>

    <el-row :gutter="12" v-loading="loading">
      <el-col v-for="card in kpis" :key="card.label" :span="4">
        <div class="kpi" :class="card.tone">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value">{{ card.value }}<span v-if="card.unit" class="kpi-unit">{{ card.unit }}</span></div>
          <div class="kpi-sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never" class="block">
          <template #header><span class="card-title">分布</span></template>
          <div v-for="group in distributions" :key="group.title" class="dist">
            <div class="dist-title">{{ group.title }}</div>
            <div v-for="row in group.rows" :key="row.key" class="dist-row">
              <span class="dist-key">{{ row.label }}</span>
              <el-progress
                :percentage="percent(row.count)"
                :stroke-width="10"
                :show-text="false"
                class="dist-bar"
              />
              <span class="dist-count">{{ row.count }}</span>
            </div>
            <div v-if="!group.rows.length" class="sub">暂无数据</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="block">
          <template #header><span class="card-title">责任人负荷 TOP5（按未关闭）</span></template>
          <el-table :data="dashboard?.topOwners ?? []" border size="small">
            <el-table-column label="责任人" min-width="110">
              <template #default="{ row }">{{ row.ownerName || row.ownerId }}</template>
            </el-table-column>
            <el-table-column label="未关闭" width="90" align="center">
              <template #default="{ row }">{{ row.openCount }}</template>
            </el-table-column>
            <el-table-column label="其中超期" width="100" align="center">
              <template #default="{ row }">
                <span :class="{ 'danger-text': row.overdueCount > 0 }">{{ row.overdueCount }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(dashboard?.topOwners ?? []).length" description="没有未关闭的质量问题" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="block">
      <template #header><span class="card-title">月度趋势</span></template>
      <el-table :data="dashboard?.trend ?? []" border size="small">
        <el-table-column prop="month" label="月份" width="110" />
        <el-table-column label="新登记" width="140">
          <template #default="{ row }">
            <div class="trend-cell">
              <span class="trend-num">{{ row.created }}</span>
              <div class="trend-bar created" :style="{ width: barWidth(row.created) }"></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="已关闭" width="140">
          <template #default="{ row }">
            <div class="trend-cell">
              <span class="trend-num">{{ row.closed }}</span>
              <div class="trend-bar closed" :style="{ width: barWidth(row.closed) }"></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="净增" min-width="100">
          <template #default="{ row }">
            <span :class="{ 'danger-text': row.created - row.closed > 0 }">{{ row.created - row.closed }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(dashboard?.trend ?? []).length" description="期间内没有数据" :image-size="50" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  qualityDashboardApi,
  ISSUE_SEVERITY_LABEL,
  ISSUE_SOURCE_LABEL,
  ISSUE_STATUS_LABEL,
  type CountRow,
  type QualityDashboard
} from '@/api/quality'

const loading = ref(false)
const dashboard = ref<QualityDashboard | null>(null)
const range = ref<string[]>(defaultRange())

onMounted(() => {
  void reload()
})

/** 默认近 6 个月（含当月）。 */
function defaultRange(): string[] {
  const end = new Date()
  const start = new Date(end.getFullYear(), end.getMonth() - 5, 1)
  return [monthOf(start), monthOf(end)]
}

function monthOf(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

/** 月份 → 该月最后一天（后端按业务日期闭区间过滤）。 */
function monthEnd(month: string): string {
  const [year, mon] = month.split('-').map(Number)
  const last = new Date(year, mon, 0)
  return `${last.getFullYear()}-${String(last.getMonth() + 1).padStart(2, '0')}-${String(last.getDate()).padStart(2, '0')}`
}

async function reload(): Promise<void> {
  const [start, end] = range.value?.length === 2 ? range.value : defaultRange()
  loading.value = true
  try {
    dashboard.value = await qualityDashboardApi.get({
      from: `${start}-01`,
      to: monthEnd(end)
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载质量看板失败')
  } finally {
    loading.value = false
  }
}

const kpis = computed(() => {
  const data = dashboard.value
  return [
    { label: '问题总数', value: data?.total ?? 0, unit: '', sub: '全量快照', tone: '' },
    { label: '未关闭', value: data?.open ?? 0, unit: '', sub: '含待分派/整改中/待验证', tone: 'warn' },
    { label: '已关闭', value: data?.closed ?? 0, unit: '', sub: '验证通过', tone: 'ok' },
    { label: '关闭率', value: data?.closedRate ?? 0, unit: '%', sub: '已关闭 / 全部', tone: 'ok' },
    { label: '超期未关闭', value: data?.overdue ?? 0, unit: '', sub: '期限早于今天', tone: 'bad' },
    { label: '平均关闭天数', value: data?.avgCloseDays ?? 0, unit: '天', sub: '仅统计已关闭', tone: '' }
  ]
})

const distributions = computed(() => {
  const data = dashboard.value
  return [
    { title: '按状态', rows: buildRows(data?.byStatus ?? [], ISSUE_STATUS_LABEL) },
    { title: '按来源', rows: buildRows(data?.bySource ?? [], ISSUE_SOURCE_LABEL) },
    { title: '按严重度', rows: buildRows(data?.bySeverity ?? [], ISSUE_SEVERITY_LABEL) }
  ]
})

function buildRows(rows: CountRow[], labels: Record<string, string>) {
  return rows.map((row) => ({ key: row.key, label: labels[row.key] ?? row.key, count: row.count }))
}

function percent(count: number): number {
  const total = dashboard.value?.total ?? 0
  if (!total) {
    return 0
  }
  return Math.round((count * 100) / total)
}

/** 趋势条按期间最大值等比缩放（不同量级月份也能看出多少）。 */
function barWidth(count: number): string {
  const values = (dashboard.value?.trend ?? []).flatMap((row) => [row.created, row.closed])
  const max = Math.max(1, ...values)
  return `${Math.max(2, Math.round((count * 100) / max))}%`
}
</script>

<style scoped>
.page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hint {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.kpi {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
  height: 100%;
}

.kpi-label {
  color: #909399;
  font-size: 13px;
}

.kpi-value {
  font-size: 24px;
  font-weight: 600;
  margin: 6px 0 2px;
}

.kpi-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 2px;
  color: #909399;
}

.kpi-sub {
  color: #b1b3b8;
  font-size: 12px;
}

.kpi.warn .kpi-value {
  color: #e6a23c;
}

.kpi.ok .kpi-value {
  color: #67c23a;
}

.kpi.bad .kpi-value {
  color: #f56c6c;
}

.block {
  margin-top: 12px;
}

.card-title {
  font-weight: 600;
}

.dist {
  margin-bottom: 12px;
}

.dist-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.dist-key {
  width: 90px;
  color: #606266;
  font-size: 13px;
}

.dist-bar {
  flex: 1;
}

.dist-count {
  width: 40px;
  text-align: right;
  font-size: 13px;
}

.trend-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-num {
  width: 28px;
  text-align: right;
}

.trend-bar {
  height: 8px;
  border-radius: 4px;
  max-width: 80px;
}

.trend-bar.created {
  background: #409eff;
}

.trend-bar.closed {
  background: #67c23a;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}
</style>
