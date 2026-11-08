<template>
  <div class="m-layout">
    <header class="m-header">
      <span class="m-title">{{ pageTitle }}</span>
      <div class="m-actions">
        <el-button link type="primary" @click="goDesktop">电脑版</el-button>
        <el-button link type="primary" @click="onLogout">退出</el-button>
      </div>
    </header>

    <main class="m-main">
      <router-view />
    </main>

    <nav class="m-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="m-tab"
        :class="{ active: isActive(tab.path) }"
        @click="router.push(tab.path)"
      >
        <el-icon :size="20"><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera, Document, List, Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => (route.meta.title as string | undefined) ?? 'NexPLM 移动端')

/** 按权限出 tab：没权限的入口不显示，点进去也只会 403。 */
const tabs = computed(() =>
  [
    { path: '/m/todo', label: '待办', icon: List, visible: userStore.can('wf.task.read') },
    { path: '/m/task', label: '任务', icon: Tickets, visible: userStore.can('prj.project.read') },
    { path: '/m/doc', label: '文档', icon: Document, visible: userStore.can('pdm.doc.read') },
    { path: '/m/scan', label: '扫码', icon: Camera, visible: true }
  ].filter((tab) => tab.visible)
)

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function goDesktop(): void {
  router.push('/')
}

function onLogout(): void {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}
</script>

<style scoped>
.m-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f4f6fa;
}

.m-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: #1f2d3d;
  color: #fff;
}

.m-title {
  font-size: 15px;
  font-weight: 600;
}

.m-actions :deep(.el-button) {
  color: #c8d3e0;
}

.m-main {
  flex: 1;
  padding: 10px;
  /* 给底部固定 tabbar 留出空间，避免最后一屏内容被遮住 */
  padding-bottom: 70px;
}

.m-tabs {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e5e8ef;
}

.m-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 11px;
  color: #8a94a6;
  cursor: pointer;
}

.m-tab.active {
  color: #409eff;
}
</style>
