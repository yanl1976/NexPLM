<template>
  <div class="portal">
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card" :class="{ warn: card.warn }">
        <div class="stat-icon"><el-icon><component :is="card.icon" /></el-icon></div>
        <div class="stat-body">
          <span class="stat-count">{{ card.count }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <div class="panel-grid">
      <el-card shadow="never" class="panel">
        <template #header>
          <span>未读事件（{{ messages.length }}）</span>
          <el-button link type="primary" @click="go('/my/messages')">更多</el-button>
        </template>
        <el-table :data="messages" size="small" height="220">
          <el-table-column prop="title" label="主题" min-width="200" />
          <el-table-column prop="createdAt" label="接收时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <template #empty><span class="hint">暂无未读事件</span></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>我的任务</span>
          <el-button link type="primary" @click="go('/wf/todo')">更多</el-button>
        </template>
        <el-table :data="tasks" size="small" height="220">
          <el-table-column prop="title" label="任务" min-width="180" />
          <el-table-column prop="state" label="状态" width="90" />
          <el-table-column prop="createdAt" label="接收时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <template #empty><span class="hint">暂无待办任务</span></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>我参与的项目（{{ projects.length }}）</span>
          <el-button link type="primary" @click="go('/prj/portal')">更多</el-button>
        </template>
        <el-table :data="projects" size="small" height="220">
          <el-table-column prop="name" label="任务名称" min-width="180" />
          <el-table-column prop="code" label="编号" width="130" />
          <el-table-column prop="status" label="状态" width="90" />
          <template #empty><span class="hint">暂无参与的项目</span></template>
        </el-table>
      </el-card>
    </div>

    <div class="panel-grid">
      <el-card shadow="never" class="panel">
        <template #header>
          <span>我的检出（{{ checkouts.length }}）</span>
          <el-button link type="primary" @click="go('/my/checkouts')">更多</el-button>
        </template>
        <el-table :data="checkouts" size="small" height="220">
          <el-table-column prop="code" label="编号" width="150" />
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column prop="checkedOutAt" label="检出时间" width="170">
            <template #default="{ row }">{{ fmt(row.checkedOutAt) }}</template>
          </el-table-column>
          <template #empty><span class="hint">暂无检出对象</span></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>最近访问（{{ recent.length }}）</span>
          <el-button link type="primary" @click="go('/my/recent')">更多</el-button>
        </template>
        <el-table :data="recent" size="small" height="220">
          <el-table-column prop="code" label="编号" width="150" />
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column prop="accessedAt" label="访问时间" width="170">
            <template #default="{ row }">{{ fmt(row.accessedAt) }}</template>
          </el-table-column>
          <template #empty><span class="hint">暂无访问记录</span></template>
        </el-table>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <span>收藏夹（{{ favorites.length }}）</span>
          <el-button link type="primary" @click="go('/my/favorites')">更多</el-button>
        </template>
        <el-table :data="favorites" size="small" height="220">
          <el-table-column prop="code" label="编号" width="150" />
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column prop="createdAt" label="收藏时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <template #empty><span class="hint">暂无收藏</span></template>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Check, Collection, Document, FolderOpened, Star, Tickets, Timer } from '@element-plus/icons-vue'
import { myApi } from '@/api/my'
import { workflowApi } from '@/api/workflow'
import { portalApi } from '@/api/project'

const router = useRouter()
/* eslint-disable @typescript-eslint/no-explicit-any */
const messages = ref<any[]>([])
const tasks = ref<any[]>([])
const projects = ref<any[]>([])
const checkouts = ref<any[]>([])
const recent = ref<any[]>([])
const favorites = ref<any[]>([])
const counts = ref({
  todo: 0,
  unread: 0,
  checkouts: 0,
  projects: 0
})

const statCards = computed(() => [
  { label: '待审批流程', count: counts.value.todo, icon: Tickets, warn: false },
  { label: '发起的流程', count: 0, icon: Document, warn: false },
  { label: '托管流程', count: 0, icon: FolderOpened, warn: false },
  { label: '转交流程', count: 0, icon: Timer, warn: false },
  { label: '待编撰日志', count: 0, icon: Document, warn: true },
  { label: '待审阅日志', count: 0, icon: Check, warn: true },
  { label: '检出对象', count: counts.value.checkouts, icon: FolderOpened, warn: false },
  { label: '变更申请', count: 0, icon: Document, warn: false },
  { label: '变更通知', count: counts.value.unread, icon: Bell, warn: false },
  { label: '收到的任务', count: counts.value.todo, icon: Tickets, warn: true },
  { label: '参与的项目', count: counts.value.projects, icon: Collection, warn: true },
  { label: '收藏对象', count: favorites.value.length, icon: Star, warn: false }
])

function fmt(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function go(path: string): void {
  router.push(path)
}

function rows(result: PromiseSettledResult<any>, key?: string): any[] {
  if (result.status !== 'fulfilled') {
    return []
  }
  const value = result.value
  if (Array.isArray(value)) {
    return value
  }
  const list = key ? value?.[key] : value?.items ?? value?.data
  return Array.isArray(list) ? list : []
}

onMounted(async () => {
  const [todo, msgs, chk, rec, fav, prj, unread] = await Promise.allSettled([
    workflowApi.todo(1, 10),
    myApi.messages(false, 10),
    myApi.checkouts(),
    myApi.recent(undefined, 10),
    myApi.favorites(),
    portalApi.myProjects(),
    myApi.unreadCount()
  ])
  tasks.value = rows(todo, 'items')
  messages.value = rows(msgs)
  checkouts.value = rows(chk)
  recent.value = rows(rec)
  favorites.value = rows(fav)
  projects.value = rows(prj)
  counts.value = {
    todo: (todo.status === 'fulfilled' ? Number(todo.value?.total ?? tasks.value.length) : 0),
    unread: unread.status === 'fulfilled' ? Number(unread.value?.unread ?? 0) : 0,
    checkouts: checkouts.value.length,
    projects: projects.value.length
  }
})
</script>

<style scoped>
.portal {
  padding: 12px;
  min-width: 900px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 10px;
  background: var(--el-bg-color);
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  flex: none;
}

.stat-card.warn .stat-icon {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-count {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.1;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.panel :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
}

.panel :deep(.el-card__body) {
  padding: 0 8px 8px;
}

.hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
