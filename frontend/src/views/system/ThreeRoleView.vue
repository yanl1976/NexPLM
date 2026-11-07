<template>
  <div class="page">
    <div class="toolbar">
      <el-button @click="load">刷新</el-button>
      <span class="hint">
        三员互斥：同一账号最多持有「系统管理员 / 安全保密管理员 / 安全审计员」其中一个，
        保存用户角色时由后端强制校验（历史违规会列在下方，需人工改）。
      </span>
    </div>

    <el-alert
      v-if="overview"
      :title="overview.note"
      :type="overview.violations.length ? 'warning' : 'success'"
      :closable="false"
      show-icon
      class="notice"
    />

    <div v-loading="loading" class="groups">
      <el-card v-for="group in overview?.roles ?? []" :key="group.roleCode" shadow="never" class="group">
        <template #header>
          <div class="group-header">
            <span class="group-name">{{ group.roleName }}</span>
            <el-tag size="small" type="info">{{ group.roleCode }}</el-tag>
            <span class="group-count">{{ group.members.length }} 人</span>
          </div>
        </template>
        <el-table :data="group.members" border size="small" empty-text="尚未分配">
          <el-table-column prop="username" label="账号" width="150" />
          <el-table-column prop="displayName" label="姓名" min-width="120" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusDot :tone="row.status === 'ACTIVE' ? 'ok' : 'muted'" :label="row.status === 'ACTIVE' ? '正常' : row.status" />
            </template>
          </el-table-column>
          <el-table-column label="最后登录" width="150" align="center">
            <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <template v-if="overview && overview.violations.length">
      <div class="section-title">互斥违规账号（同一账号持有多个三员角色）</div>
      <el-table :data="overview.violations" border stripe>
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="displayName" label="姓名" width="140" />
        <el-table-column prop="roleCodes" label="同时持有的三员角色" min-width="220" />
        <el-table-column label="角色数" width="100" align="center" prop="roleCount" />
      </el-table>
    </template>

    <el-empty
      v-if="!loading && !overview"
      description="当前账号没有「三员配置查看」权限（sys.three-role.read）"
      :image-size="60"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { threeRoleApi, type ThreeRoleOverview } from '@/api/system'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canRead = userStore.can('sys.three-role.read')

const overview = ref<ThreeRoleOverview | null>(null)
const loading = ref(false)

async function load(): Promise<void> {
  if (!canRead) {
    return
  }
  loading.value = true
  try {
    overview.value = await threeRoleApi.overview()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

function formatTime(value: string | null): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

void load()
</script>

<style scoped>
.groups {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.group {
  flex: 1;
  min-width: 360px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-name {
  font-weight: 600;
}

.group-count {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.notice {
  margin-bottom: 12px;
}

.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
