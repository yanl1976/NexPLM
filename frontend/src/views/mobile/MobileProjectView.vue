<template>
  <div class="m-page">
    <div class="m-toolbar">
      <span class="hint">我是经理或成员的项目（{{ rows.length }}）</span>
      <el-button size="small" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="card-list">
      <div v-for="p in rows" :key="p.id" class="task-card" @click="openDetail(p)">
        <div class="task-head">
          <span class="task-name">{{ p.name }}</span>
          <span class="badge">{{ PROJECT_STATUS_LABEL[p.status] ?? p.status }}</span>
        </div>
        <div class="task-object">{{ p.code }} · {{ PROJECT_TYPE_LABEL[p.type] ?? p.type }}</div>
        <el-progress :percentage="p.progress" :stroke-width="8" :show-text="false" />
        <div class="task-meta">
          <span>{{ p.planStart || '-' }} ~ {{ p.planEnd || '-' }}</span>
          <span>我的任务 {{ p.myTaskCount }}（待办 {{ p.myOpenTaskCount }}）</span>
        </div>
        <div class="task-tags">
          <el-tag v-if="p.manager" size="small" type="warning">项目经理</el-tag>
          <el-tag v-if="p.member" size="small">成员</el-tag>
          <span class="manager">{{ p.managerName || '-' }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !rows.length" description="暂无参与的项目" :image-size="60" />
    </div>

    <el-drawer v-model="detailVisible" direction="btt" size="62%" :title="detail?.name ?? '项目'">
      <div v-if="detail" class="detail">
        <div class="detail-row"><span>编码</span><b>{{ detail.code }}</b></div>
        <div class="detail-row">
          <span>状态</span>
          <b>{{ PROJECT_STATUS_LABEL[detail.status] ?? detail.status }}</b>
        </div>
        <div class="detail-row">
          <span>类型</span>
          <b>{{ PROJECT_TYPE_LABEL[detail.type] ?? detail.type }}</b>
        </div>
        <div class="detail-row"><span>EPS</span><b>{{ detail.epsName || '-' }}</b></div>
        <div class="detail-row"><span>项目经理</span><b>{{ detail.managerName || '-' }}</b></div>
        <div class="detail-row">
          <span>计划</span>
          <b>{{ detail.planStart || '-' }} ~ {{ detail.planEnd || '-' }}</b>
        </div>
        <div class="detail-row"><span>进度</span><b>{{ detail.progress }}%</b></div>
        <div class="detail-row"><span>成员</span><b>{{ detail.members?.length ?? 0 }} 人</b></div>
      </div>
      <el-empty v-else-if="!detail" description="加载中" :image-size="48" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goTasks">我的任务</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端「我的项目」（B11 补页）。
 *
 * <p>数据直接复用项目门户的 `GET /prj/portal/my-projects`（同时带"我的任务数/待办数"），
 * 不在后端另造一个"我参与的"查询（R2）；点开只读详情，动作统一去「我的任务」页做。</p>
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  portalApi,
  projectApi,
  PROJECT_STATUS_LABEL,
  PROJECT_TYPE_LABEL,
  type PortalProjectCard,
  type ProjectDetail
} from '@/api/project'

const router = useRouter()
const loading = ref(false)
const rows = ref<PortalProjectCard[]>([])
const detailVisible = ref(false)
const detail = ref<ProjectDetail | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await portalApi.myProjects()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function openDetail(card: PortalProjectCard): Promise<void> {
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await projectApi.detail(card.id)
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

function goTasks(): void {
  detailVisible.value = false
  router.push('/m/task')
}

onMounted(load)
</script>

<style scoped>
.m-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.m-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hint {
  font-size: 12px;
  color: #8a94a6;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.task-card {
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgb(31 45 61 / 8%);
  cursor: pointer;
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.badge {
  padding: 1px 6px;
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  border-radius: 8px;
}

.task-object {
  margin: 4px 0 6px;
  font-size: 12px;
  color: #5c6b80;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 11px;
  color: #8a94a6;
}

.task-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}

.manager {
  margin-left: auto;
  font-size: 11px;
  color: #8a94a6;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #5c6b80;
}

.detail-row b {
  color: #1f2d3d;
  font-weight: 500;
}
</style>
