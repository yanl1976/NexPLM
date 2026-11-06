<template>
  <div class="page">
    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="期间开始"
        end-placeholder="期间结束"
        style="width: 260px"
        @change="load"
      />
      <el-button type="primary" @click="load">刷新</el-button>
      <span class="hint">
        总量与完成率取全量快照，期间只影响「期间新增」与趋势；未命中中文映射的状态按原值显示
      </span>
    </div>

    <el-alert
      v-if="!canRead"
      type="warning"
      :closable="false"
      show-icon
      title="当前账号没有「统计看板查看」权限（ana.dashboard.read）"
    />

    <template v-else>
      <div v-loading="loading" class="card-grid">
        <div v-for="card in cards" :key="card.domain" class="metric-card">
          <div class="card-head">
            <span class="card-title">{{ card.label }}</span>
            <el-button link type="primary" @click="openDrill(card.domain)">明细</el-button>
          </div>
          <div class="card-total">
            {{ formatNumber(card.total) }}
            <span class="unit">总数</span>
          </div>
          <div class="card-numbers">
            <span>未完成 <b>{{ formatNumber(card.active) }}</b></span>
            <span>已完成 <b>{{ formatNumber(card.done) }}</b></span>
            <span :class="{ danger: card.overdue > 0 }">超期 <b>{{ formatNumber(card.overdue) }}</b></span>
          </div>
          <el-progress :percentage="card.doneRate" :stroke-width="6" :show-text="false" />
          <div class="card-foot">
            <span>完成率 {{ card.doneRate }}%</span>
            <span>期间新增 {{ formatNumber(card.addedInPeriod) }}</span>
          </div>
          <div class="card-status">
            <el-tag
              v-for="row in card.byStatus"
              :key="row.key"
              size="small"
              class="status-tag"
              @click="openDrill(card.domain, row.key)"
            >
              {{ statusLabel(row.key) }} {{ row.count }}
            </el-tag>
            <span v-if="!card.byStatus.length" class="empty-hint">暂无数据</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !cards.length" description="没有可展示的统计数据" :image-size="60" />

      <el-card shadow="never" class="trend-card">
        <template #header>
          <span>新增趋势（{{ overview?.from }} ~ {{ overview?.to }}，逐月补齐）</span>
        </template>
        <table class="heat">
          <thead>
            <tr>
              <th>月份</th>
              <th v-for="domain in domains" :key="domain">{{ domainLabel(domain) }}</th>
              <th>合计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in trend" :key="row.month">
              <td class="month">{{ row.month }}</td>
              <td v-for="domain in domains" :key="domain" :style="{ background: heatColor(row.domains[domain], columnMax[domain] ?? 0) }">
                {{ row.domains[domain] ?? 0 }}
              </td>
              <td class="total">{{ row.total }}</td>
            </tr>
          </tbody>
        </table>
        <el-empty v-if="!trend.length" description="没有趋势数据" :image-size="60" />
      </el-card>
    </template>

    <el-drawer v-model="drillVisible" :title="drillTitle" size="780px" destroy-on-close>
      <div class="toolbar">
        <el-select v-model="drillStatus" clearable placeholder="全部状态" style="width: 180px" @change="searchDrill">
          <el-option
            v-for="row in drillStatusOptions"
            :key="row.key"
            :label="`${statusLabel(row.key)}（${row.count}）`"
            :value="row.key"
          />
        </el-select>
        <el-button @click="searchDrill">查询</el-button>
        <span class="hint">状态分布来自当前看板快照，实际明细以查询结果为准</span>
      </div>
      <el-table v-loading="drillLoading" :data="drillRows" border size="small">
        <el-table-column prop="code" label="编码" width="150" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="责任人" width="120">
          <template #default="{ row }">{{ row.owner || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150" align="center">
          <template #default="{ row }">{{ formatTime(row.time) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!drillLoading && !drillRows.length" description="没有匹配的明细" :image-size="60" />
      <el-pagination
        v-model:current-page="drillPage"
        class="pager"
        layout="total, prev, pager, next"
        :page-size="drillSize"
        :total="drillTotal"
        @current-change="loadDrill"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DASHBOARD_DOMAIN_LABEL,
  DASHBOARD_STATUS_LABEL,
  dashboardApi,
  type DashboardDomainCard,
  type DashboardDrillRow,
  type DashboardOverview,
  type DashboardTrendRow
} from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canRead = userStore.can('ana.dashboard.read')

const loading = ref(false)
const range = ref<[string, string] | null>(null)
const overview = ref<DashboardOverview | null>(null)
const cards = ref<DashboardDomainCard[]>([])
const domains = ref<string[]>([])
const trend = ref<DashboardTrendRow[]>([])

const drillVisible = ref(false)
const drillLoading = ref(false)
const drillDomain = ref('')
const drillStatus = ref('')
const drillRows = ref<DashboardDrillRow[]>([])
const drillTotal = ref(0)
const drillPage = ref(1)
const drillSize = 20

const drillTitle = computed(
  () => `${domainLabel(drillDomain.value)}明细${drillStatus.value ? ' · ' + statusLabel(drillStatus.value) : ''}`
)

/** 每列最大值：色块深浅按域内相对大小，不跨域比较（各域量级差几个数量级）。 */
const columnMax = computed(() => {
  const max: Record<string, number> = {}
  for (const domain of domains.value) {
    max[domain] = trend.value.reduce((acc, row) => Math.max(acc, row.domains[domain] ?? 0), 0)
  }
  return max
})

/** 下钻状态选项：取当前卡片快照里的状态分布（无数据时为空，仍可手工清空筛选）。 */
const drillStatusOptions = computed(() => {
  const card = cards.value.find((item) => item.domain === drillDomain.value)
  return card?.byStatus ?? []
})

async function load(): Promise<void> {
  if (!canRead) {
    return
  }
  loading.value = true
  try {
    const data = await dashboardApi.overview({
      from: range.value?.[0],
      to: range.value?.[1]
    })
    overview.value = data
    cards.value = data.cards
    domains.value = data.domains
    trend.value = data.trend
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载统计看板失败')
  } finally {
    loading.value = false
  }
}

function openDrill(domain: string, status = ''): void {
  drillDomain.value = domain
  drillStatus.value = status
  drillPage.value = 1
  drillVisible.value = true
  void loadDrill()
}

function searchDrill(): void {
  drillPage.value = 1
  void loadDrill()
}

async function loadDrill(): Promise<void> {
  if (!drillDomain.value) {
    return
  }
  drillLoading.value = true
  try {
    const result = await dashboardApi.items(drillDomain.value, {
      status: drillStatus.value || undefined,
      page: drillPage.value,
      size: drillSize
    })
    drillRows.value = result.items
    drillTotal.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载明细失败')
  } finally {
    drillLoading.value = false
  }
}

function domainLabel(domain: string): string {
  if (!domain) {
    return ''
  }
  return cards.value.find((item) => item.domain === domain)?.label ?? DASHBOARD_DOMAIN_LABEL[domain] ?? domain
}

function statusLabel(status: string): string {
  if (!status) {
    return '-'
  }
  return DASHBOARD_STATUS_LABEL[status] ?? status
}

function formatNumber(value?: number | null): string {
  return (value ?? 0).toLocaleString()
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

/** 0 → 透明，越大越深；单列最大值给最深一档，避免全 0 时满屏色块。 */
function heatColor(value: number | undefined, max: number): string {
  if (!value || !max) {
    return 'transparent'
  }
  const ratio = Math.min(1, value / max)
  return `rgba(64, 158, 255, ${(0.10 + ratio * 0.55).toFixed(2)})`
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8a94a6;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.metric-card {
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 6px;
  padding: 12px 14px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.card-total {
  margin: 6px 0;
  font-size: 26px;
  font-weight: 700;
  color: #1f2d3d;
}

.unit {
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #8a94a6;
}

.card-numbers {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #5c6b7a;
}

.card-numbers b {
  color: #303133;
}

.danger b,
.danger {
  color: #f56c6c;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: #8a94a6;
}

.card-status {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-height: 24px;
}

.status-tag {
  cursor: pointer;
}

.empty-hint {
  font-size: 12px;
  color: #b1b8c4;
}

.trend-card {
  margin-top: 12px;
}

.heat {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.heat th,
.heat td {
  border: 1px solid #e5e8ef;
  padding: 6px 8px;
  text-align: center;
}

.heat th {
  background: #f5f7fa;
  color: #5c6b7a;
  font-weight: 600;
}

.heat .month {
  font-weight: 600;
  color: #303133;
}

.heat .total {
  font-weight: 600;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
