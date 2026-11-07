<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="账号 / 姓名"
        clearable
        style="width: 200px"
        @keyup.enter="reload"
      />
      <el-select v-model="deviceType" placeholder="全部设备" clearable style="width: 140px">
        <el-option label="PC" value="PC" />
        <el-option label="移动端" value="MOBILE" />
      </el-select>
      <el-checkbox v-model="onlyActive" @change="reload">只看在线</el-checkbox>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="danger" plain @click="kickAll()">全部下线</el-button>
      <el-button v-if="canWrite" type="warning" plain @click="kickAll('MOBILE')">移动端全部下线</el-button>
      <span class="hint">
        踢下线 = 把会话置失效，对方已签发的令牌立刻 401（不再等令牌自然过期）；「最近操作」5 分钟节流更新。
      </span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="username" label="账号" width="150" />
      <el-table-column label="姓名" width="140">
        <template #default="{ row }">{{ row.displayName ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="设备类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.deviceType === 'MOBILE' ? 'warning' : 'info'">
            {{ row.deviceType === 'MOBILE' ? '移动端' : row.deviceType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="登录 IP" width="150">
        <template #default="{ row }">{{ row.ip ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="登录时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.loginAt) }}</template>
      </el-table-column>
      <el-table-column label="最近操作" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.lastActiveAt) }}</template>
      </el-table-column>
      <el-table-column label="在线时长" width="120" align="center">
        <template #default="{ row }">{{ formatDuration(row.onlineSeconds) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusDot :tone="row.active ? 'ok' : 'muted'" :label="row.active ? '在线' : '已下线'" />
        </template>
      </el-table-column>
      <el-table-column label="客户端" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.ua ?? '-' }}</template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.active" link type="danger" @click="kick(row)">踢下线</el-button>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!loading && !rows.length"
      :description="canRead ? '当前没有符合条件的会话记录' : '当前账号没有「在线用户查看」权限（sys.session.read）'"
      :image-size="60"
    />

    <div v-if="total > 0" class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="reload"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sessionApi, type SessionRow } from '@/api/system'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canRead = userStore.can('sys.session.read')
const canWrite = userStore.can('sys.session.write')

const rows = ref<SessionRow[]>([])
const loading = ref(false)
const keyword = ref('')
const deviceType = ref('')
const onlyActive = ref(true)
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load(): Promise<void> {
  if (!canRead) {
    return
  }
  loading.value = true
  try {
    const result = await sessionApi.page({
      keyword: keyword.value || undefined,
      deviceType: deviceType.value || undefined,
      onlyActive: onlyActive.value,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

function reload(): void {
  page.value = 1
  void load()
}

async function kick(row: SessionRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定让「${row.displayName ?? row.username}」的这次登录（${row.deviceType} / ${row.ip ?? '未知 IP'}）立即下线？`,
      '强制下线',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await sessionApi.kick(row.id)
    ElMessage.success('已下线，该令牌立即失效')
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function kickAll(device?: string): Promise<void> {
  try {
    await ElMessageBox.confirm(
      device === 'MOBILE' ? '确定让所有移动端会话下线？' : '确定让所有在线会话下线？',
      '全部下线',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const result = await sessionApi.kickAll(device)
    ElMessage.success(`已下线 ${result.kicked} 个会话`)
    void load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

/** 后端给 ISO 字符串，这里只显示到分钟，避免表格里挤满毫秒。 */
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

function formatDuration(seconds: number): string {
  if (!seconds || seconds < 0) {
    return '-'
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours} 小时 ${minutes} 分`
  }
  return `${minutes} 分钟`
}

void load()
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.muted {
  color: var(--el-text-color-placeholder);
}
</style>
