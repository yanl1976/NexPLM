<template>
  <div class="page">
    <div class="toolbar">
      <span class="prj-title">项目门户</span>
      <el-button @click="loadAll">刷新</el-button>
      <el-button type="primary" plain @click="goList">项目列表</el-button>
      <span class="hint">三视图回答三个问题：我要做什么 / 我的项目快不快 / 公司整体怎么样</span>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="我的项目" name="mine">
        <el-empty v-if="!loading && !mine.length" description="你还没有参与任何项目" :image-size="60" />
        <div v-else class="card-grid">
          <el-card v-for="item in mine" :key="item.id" shadow="hover" class="project-card">
            <div class="card-head">
              <span class="card-code">{{ item.code }}</span>
              <el-tag :type="statusType(item.status)" size="small">
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
            <div class="card-name" :title="item.name">{{ item.name }}</div>
            <el-progress :percentage="item.progress" :stroke-width="10" />
            <div class="card-meta">
              <span>{{ item.planStart || '-' }} ~ {{ item.planEnd || '-' }}</span>
            </div>
            <div class="card-meta">
              <span>我的任务：待办 {{ item.myOpenTaskCount }} / 共 {{ item.myTaskCount }}</span>
            </div>
            <div class="card-foot">
              <span class="sub">经理：{{ item.managerName || '-' }}</span>
              <el-button link type="primary" @click="openTasks(item.id)">任务看板</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我管理的项目" name="managed">
        <el-table v-loading="loading" :data="managed" border stripe>
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="进度" width="160">
            <template #default="{ row }">
              <el-progress :percentage="row.progress" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="时间进度" width="100" align="right">
            <template #default="{ row }">
              <span>{{ row.timeProgress === null || row.timeProgress === undefined ? '-' : row.timeProgress + '%' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="偏差" width="90" align="right">
            <template #default="{ row }">
              <span v-if="row.deviation === null || row.deviation === undefined" class="sub">-</span>
              <span v-else :class="deviationClass(row.deviation)">{{ row.deviation > 0 ? '+' : '' }}{{ row.deviation }}</span>
            </template>
          </el-table-column>
          <el-table-column label="任务" width="110" align="center">
            <template #default="{ row }">{{ row.doneTaskCount }} / {{ row.taskCount }}</template>
          </el-table-column>
          <el-table-column label="逾期任务" width="100" align="center">
            <template #default="{ row }">
              <span :class="{ 'danger-text': row.overdueTaskCount > 0 }">{{ row.overdueTaskCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="待审工时" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.pendingTimesheetCount > 0"
                link
                type="warning"
                @click="openTimesheets(row.id)"
              >{{ row.pendingTimesheetCount }} 条</el-button>
              <span v-else class="sub">0</span>
            </template>
          </el-table-column>
          <el-table-column prop="memberCount" label="成员" width="80" align="center" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTasks(row.id)">任务</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !managed.length" description="你还没有管理的项目" :image-size="60" />
      </el-tab-pane>

      <el-tab-pane label="公司组合" name="portfolio">
        <div class="stat-row">
          <el-card shadow="never" class="stat">
            <div class="stat-label">项目总数</div>
            <div class="stat-value">{{ portfolio?.totalProjects ?? 0 }}</div>
          </el-card>
          <el-card shadow="never" class="stat">
            <div class="stat-label">进行中 / 已结项</div>
            <div class="stat-value">{{ portfolio?.activeProjects ?? 0 }} / {{ portfolio?.closedProjects ?? 0 }}</div>
          </el-card>
          <el-card shadow="never" class="stat">
            <div class="stat-label">已到期未结项</div>
            <div class="stat-value" :class="{ danger: (portfolio?.overdueProjects ?? 0) > 0 }">
              {{ portfolio?.overdueProjects ?? 0 }}
            </div>
          </el-card>
          <el-card shadow="never" class="stat">
            <div class="stat-label">平均进度</div>
            <div class="stat-value">{{ portfolio?.avgProgress ?? 0 }}%</div>
          </el-card>
        </div>

        <div class="section-title">状态分布</div>
        <div class="status-row">
          <el-tag v-for="item in portfolio?.byStatus ?? []" :key="item.status" :type="statusType(item.status)" size="large">
            {{ statusLabel(item.status) }}：{{ item.count }}
          </el-tag>
          <span v-if="!(portfolio?.byStatus ?? []).length" class="sub">暂无数据</span>
        </div>

        <div class="section-title">风险清单（落后最多 / 有逾期任务，最多 10 个）</div>
        <el-table :data="portfolio?.risks ?? []" border stripe>
          <el-table-column prop="code" label="编码" width="120" />
          <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="progress" label="进度" width="90" align="right" />
          <el-table-column label="时间进度" width="100" align="right">
            <template #default="{ row }">
              {{ row.timeProgress === null || row.timeProgress === undefined ? '-' : row.timeProgress + '%' }}
            </template>
          </el-table-column>
          <el-table-column label="偏差" width="90" align="right">
            <template #default="{ row }">
              <span v-if="row.deviation === null || row.deviation === undefined" class="sub">-</span>
              <span v-else :class="deviationClass(row.deviation)">{{ row.deviation > 0 ? '+' : '' }}{{ row.deviation }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="overdueTaskCount" label="逾期任务" width="100" align="center" />
          <el-table-column prop="planEnd" label="计划结束" width="120" />
        </el-table>
        <el-empty v-if="!(portfolio?.risks ?? []).length" description="没有落后或逾期的项目" :image-size="60" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  portalApi,
  PROJECT_STATUS_LABEL,
  type ManagedProjectRow,
  type PortalProjectCard,
  type PortfolioOverview,
  type ProjectStatus
} from '@/api/project'

const router = useRouter()
const loading = ref(false)
const tab = ref<'mine' | 'managed' | 'portfolio'>('mine')
const mine = ref<PortalProjectCard[]>([])
const managed = ref<ManagedProjectRow[]>([])
const portfolio = ref<PortfolioOverview | null>(null)

onMounted(loadAll)

async function loadAll(): Promise<void> {
  loading.value = true
  try {
    const [mineResult, managedResult, portfolioResult] = await Promise.all([
      portalApi.myProjects(),
      portalApi.managedProjects(),
      portalApi.portfolio()
    ])
    mine.value = mineResult
    managed.value = managedResult
    portfolio.value = portfolioResult
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载项目门户失败')
  } finally {
    loading.value = false
  }
}

function statusLabel(value: string): string {
  return PROJECT_STATUS_LABEL[value as ProjectStatus] ?? value
}

function statusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'ACTIVE') {
    return 'success'
  }
  if (value === 'PAUSED') {
    return 'warning'
  }
  if (value === 'CLOSED') {
    return 'info'
  }
  return 'info'
}

function deviationClass(value: number): string {
  if (value <= -20) {
    return 'danger-text'
  }
  if (value < 0) {
    return 'warning-text'
  }
  return 'success-text'
}

function openTasks(projectId: string): void {
  router.push(`/project/${projectId}/tasks`)
}

function openTimesheets(projectId: string): void {
  router.push(`/project/${projectId}/timesheets`)
}

function goList(): void {
  router.push('/project')
}
</script>

<style scoped>
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.project-card {
  min-height: 170px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-code {
  color: #909399;
  font-size: 12px;
}

.card-name {
  font-weight: 600;
  margin: 6px 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  color: #606266;
  font-size: 12px;
  margin-top: 6px;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

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

.stat-value.danger {
  color: #f56c6c;
}

.section-title {
  font-weight: 600;
  margin: 12px 0 8px;
}

.status-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.warning-text {
  color: #e6a23c;
  font-weight: 600;
}

.success-text {
  color: #67c23a;
  font-weight: 600;
}
</style>
