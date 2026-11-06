<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">NexPLM</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item v-if="canReadDashboard" index="/report/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计看板</span>
        </el-menu-item>
        <el-menu-item v-if="canReadBoard" index="/report/boards">
          <el-icon><Grid /></el-icon>
          <span>自定义看板</span>
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
          <span>变更管理</span>
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

    <el-container>
      <el-header class="header">
        <div class="header-left">元数据驱动 · API 优先 · 可私有化部署</div>
        <div class="header-right">
          <span class="user">{{ userStore.displayName }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Checked,
  Connection,
  DataAnalysis,
  Document,
  Grid,
  HomeFilled,
  OfficeBuilding,
  Operation,
  Refresh,
  Setting,
  Share
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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

function onLogout(): void {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #1f2d3d;
  color: #fff;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #c8d3e0;
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #2b4a6f;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e5e8ef;
}

.header-left {
  color: #6b7a90;
  font-size: 13px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  color: #303133;
  font-weight: 500;
}

.main {
  background: #f4f6fa;
}
</style>
