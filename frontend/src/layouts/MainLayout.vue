<template>
  <el-container class="layout">
    <!-- 顶部品牌条：深蓝渐变 + 全局快捷入口（SIPM p29 的做法） -->
    <el-header class="topbar" height="44px">
      <el-button class="fold-btn" link @click="toggleCollapse">
        <el-icon>
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
      </el-button>
      <div class="brand">
        <span class="brand-name">NexPLM</span>
        <span class="brand-sub">元数据驱动 · API 优先 · 可私有化部署</span>
      </div>

      <div class="quick">
        <el-button link @click="goTodo">
          待办
          <span v-if="todoCount > 0" class="badge">{{ todoCount }}</span>
        </el-button>
        <el-button link @click="goOverdue">
          逾期任务
          <span v-if="overdueCount > 0" class="badge danger">{{ overdueCount }}</span>
        </el-button>
      </div>

      <div class="header-right">
        <el-button link @click="goMobile">移动版</el-button>
        <span class="user">{{ userStore.displayName }}</span>
        <el-button link @click="onLogout">退出</el-button>
      </div>
    </el-header>

    <el-container class="body">
      <el-aside :width="asideWidth" class="aside">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="menu"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <template #title>概览</template>
          </el-menu-item>
          <el-sub-menu index="/my">
            <template #title>
              <el-icon><Star /></el-icon>
              <span>个人工作区</span>
            </template>
            <el-menu-item index="/my/checkouts">我的检出</el-menu-item>
            <el-menu-item index="/my/favorites">收藏夹</el-menu-item>
            <el-menu-item index="/my/recent">最近访问</el-menu-item>
            <el-menu-item index="/my/search">高级查询</el-menu-item>
            <el-menu-item index="/my/messages">站内消息</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-if="canReadDashboard" index="/report/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>统计看板</template>
          </el-menu-item>
          <el-menu-item v-if="canReadBoard" index="/report/boards">
            <el-icon><Grid /></el-icon>
            <template #title>自定义看板</template>
          </el-menu-item>
          <el-sub-menu index="/system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/org">组织架构</el-menu-item>
            <el-menu-item index="/system/user">用户管理</el-menu-item>
            <el-menu-item index="/system/role">角色与授权</el-menu-item>
            <el-menu-item index="/system/meta">元数据建模</el-menu-item>
            <el-menu-item index="/system/security">安全策略</el-menu-item>
            <el-menu-item index="/system/sso">单点登录</el-menu-item>
            <el-menu-item index="/system/lifecycle">生命周期</el-menu-item>
            <el-menu-item index="/system/coding">编码管理</el-menu-item>
            <el-menu-item v-if="canReadNotify" index="/system/notify">通知通道</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/doc-center">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>图文档</span>
            </template>
            <el-menu-item index="/doc">文档库</el-menu-item>
            <el-menu-item index="/doc/class">文档分类</el-menu-item>
            <el-menu-item index="/workspace">工作空间</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/pse">
            <template #title>
              <el-icon><Share /></el-icon>
              <span>零部件/BOM</span>
            </template>
            <el-menu-item index="/item">零部件</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/wf">
            <template #title>
              <el-icon><Checked /></el-icon>
              <span>流程审批</span>
            </template>
            <el-menu-item index="/workflow">流程中心</el-menu-item>
            <el-menu-item index="/workflow/delegate">委托设置</el-menu-item>
            <el-menu-item v-if="canManageWorkflow" index="/workflow/definitions">流程定义</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/change">
            <el-icon><Refresh /></el-icon>
            <template #title>变更管理</template>
          </el-menu-item>
          <el-sub-menu index="/capp">
            <template #title>
              <el-icon><Operation /></el-icon>
              <span>工艺管理</span>
            </template>
            <el-menu-item index="/capp/routing">工艺路线</el-menu-item>
            <el-menu-item index="/capp/resource">工艺资源</el-menu-item>
            <el-menu-item index="/capp/card">工艺卡片</el-menu-item>
            <el-menu-item index="/capp/time">工时定额</el-menu-item>
            <el-menu-item index="/capp/volume">工艺成册</el-menu-item>
            <el-menu-item index="/capp/quality">质量规划</el-menu-item>
            <el-menu-item index="/capp/dispatch">ERP下发（预留）</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/project">
            <template #title>
              <el-icon><Calendar /></el-icon>
              <span>项目管理</span>
            </template>
            <el-menu-item index="/project">项目列表</el-menu-item>
            <el-menu-item index="/project/portal">项目门户</el-menu-item>
            <el-menu-item index="/project/my-tasks">我的任务</el-menu-item>
            <el-menu-item index="/project/my-timesheets">我的工时</el-menu-item>
            <el-menu-item index="/project/templates">项目模板</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/quality">
            <template #title>
              <el-icon><Checked /></el-icon>
              <span>质量与体系</span>
            </template>
            <el-menu-item index="/quality/issues">质量问题</el-menu-item>
            <el-menu-item index="/quality/templates">8D 模板</el-menu-item>
            <el-menu-item index="/quality/dashboard">质量看板</el-menu-item>
            <el-menu-item index="/quality/sys-docs">体系文件</el-menu-item>
            <el-menu-item index="/quality/sys-doc-templates">体系文件模板</el-menu-item>
            <el-menu-item index="/quality/audits">体系审核</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/sup">
            <template #title>
              <el-icon><OfficeBuilding /></el-icon>
              <span>供应商</span>
            </template>
            <el-menu-item index="/sup/suppliers">供应商主数据</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/itg">
            <template #title>
              <el-icon><Connection /></el-icon>
              <span>集成中心</span>
            </template>
            <el-menu-item index="/itg">端点 / 映射 / 任务</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Calendar,
  Checked,
  Clock,
  Connection,
  DataAnalysis,
  Document,
  Expand,
  Fold,
  Grid,
  HomeFilled,
  OfficeBuilding,
  Operation,
  Refresh,
  Search,
  Setting,
  Share,
  Star
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { workflowApi } from '@/api/workflow'
import { taskApi } from '@/api/project'

const COLLAPSE_KEY = 'nexplm.sider.collapsed'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
const asideWidth = computed(() => (collapsed.value ? '48px' : '200px'))

/** 顶栏「待办 / 逾期」计数：接口不可用时静默留空，不阻断布局。 */
const todoCount = ref(0)
const overdueCount = ref(0)

const canManageWorkflow = computed(() => userStore.can('wf.process.manage'))
const canReadNotify = computed(() => userStore.can('sys.notify.read'))
const canReadDashboard = computed(() => userStore.can('ana.dashboard.read'))
const canReadBoard = computed(() => userStore.can('ana.board.read'))

const activeMenu = computed(() => {
  if (route.path === '/doc/class') {
    return '/doc/class'
  }
  if (route.path.startsWith('/doc')) {
    return '/doc'
  }
  if (route.path.startsWith('/item')) {
    return '/item'
  }
  if (route.path === '/workflow/delegate') {
    return '/workflow/delegate'
  }
  if (route.path === '/workflow/definitions') {
    return '/workflow/definitions'
  }
  if (route.path.startsWith('/workflow')) {
    return '/workflow'
  }
  return route.path
})

function toggleCollapse(): void {
  collapsed.value = !collapsed.value
}

watch(collapsed, (value) => {
  localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
})

function goTodo(): void {
  router.push('/workflow')
}

function goOverdue(): void {
  router.push('/project/my-tasks')
}

function goMobile(): void {
  router.push('/m/todo')
}

function onLogout(): void {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}

/** 逾期口径：计划完成日已过且任务未结束（与「我的任务」列表一致，不猜后端未返回的状态）。 */
function countOverdue(rows: { planEnd?: string | null; status: string }[]): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return rows.filter((row) => {
    if (!row.planEnd || row.status === 'DONE' || row.status === 'CANCELLED') {
      return false
    }
    const end = new Date(row.planEnd)
    return !Number.isNaN(end.getTime()) && end < today
  }).length
}

onMounted(async () => {
  try {
    const page = await workflowApi.todo(1, 1)
    todoCount.value = page.total ?? 0
  } catch {
    todoCount.value = 0
  }
  try {
    const rows = await taskApi.myTasks()
    overdueCount.value = countOverdue(rows)
  } catch {
    overdueCount.value = 0
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ---------------- 顶部品牌条 ---------------- */
.topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  background: linear-gradient(90deg, var(--plm-topbar-from), var(--plm-topbar-to));
  color: #fff;
}

.fold-btn {
  color: rgba(255, 255, 255, 0.82);
  font-size: 16px;
}

.fold-btn:hover {
  color: #fff;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
  white-space: nowrap;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.quick {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: 16px;
}

.quick :deep(.el-button) {
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  height: 24px;
  padding: 0 8px;
  border-radius: var(--plm-radius);
}

.quick :deep(.el-button:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
}

.badge {
  display: inline-block;
  min-width: 16px;
  margin-left: 4px;
  padding: 0 4px;
  font-size: 11px;
  line-height: 15px;
  text-align: center;
  color: #fff;
  background: var(--plm-running);
  border-radius: 8px;
}

.badge.danger {
  background: var(--plm-danger);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  white-space: nowrap;
}

.header-right :deep(.el-button) {
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  height: 24px;
  padding: 0 6px;
}

.header-right :deep(.el-button:hover) {
  color: #fff;
}

.user {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

/* ---------------- 侧栏 ---------------- */
.body {
  overflow: hidden;
}

.aside {
  background: var(--plm-sider-bg);
  border-right: 1px solid var(--plm-border);
  overflow-x: hidden;
  overflow-y: auto;
}

.menu {
  border-right: none;
  background: transparent;
  font-size: var(--plm-font-base);
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--plm-text-regular);
  --el-menu-active-color: var(--plm-primary);
  --el-menu-hover-bg-color: var(--plm-primary-light);
  --el-menu-item-height: 32px;
  --el-menu-sub-item-height: 28px;
  --el-menu-base-level-padding: 12px;
  --el-menu-icon-width: 18px;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 32px;
  line-height: 32px;
  font-size: 12px;
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--plm-primary-light);
  box-shadow: inset 3px 0 0 var(--plm-primary);
  font-weight: 600;
}

.menu :deep(.el-sub-menu .el-menu-item) {
  height: 28px;
  line-height: 28px;
  min-width: auto;
}

/* ---------------- 主内容区 ---------------- */
.main {
  padding: 12px;
  background: var(--plm-bg-page);
  overflow: auto;
}

/*
 * 小屏收起侧栏：桌面端这些页面（宽表格、双栏表单）在手机上本来就挤不下，
 * 与其做半套响应式不如把入口交给移动版（/m，独立 layout + 底部 tabbar）。
 */
@media (max-width: 768px) {
  .aside {
    display: none;
  }

  .brand-sub,
  .fold-btn {
    display: none;
  }

  .quick {
    margin-left: 8px;
  }

  .main {
    padding: 8px;
  }
}
</style>
