<template>
  <div class="page">
    <div class="cards">
      <el-card shadow="never" class="card">
        <div class="card-label">今日登录成功</div>
        <div class="card-value">{{ summary?.loginSuccess ?? '-' }}</div>
      </el-card>
      <el-card shadow="never" class="card">
        <div class="card-label">今日登录失败</div>
        <div class="card-value danger">{{ summary?.loginFailed ?? '-' }}</div>
      </el-card>
      <el-card shadow="never" class="card">
        <div class="card-label">今日操作日志</div>
        <div class="card-value">{{ summary?.auditCount ?? '-' }}</div>
      </el-card>
      <el-card shadow="never" class="card wide">
        <div class="card-label">审计哈希链</div>
        <div class="card-value small" :class="summary?.chainEnabled ? 'ok' : 'warn'">
          {{ summary?.chainMessage ?? '-' }}
        </div>
      </el-card>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="登录日志" name="login">
        <div class="toolbar">
          <el-input
            v-model="loginQuery.username"
            placeholder="账号"
            clearable
            style="width: 180px"
            @keyup.enter="reloadLogin"
          />
          <el-select v-model="loginQuery.result" placeholder="全部结果" clearable style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-date-picker
            v-model="loginRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
          <el-button type="primary" @click="reloadLogin">查询</el-button>
        </div>

        <el-table v-loading="loadingLogin" :data="loginRows" border stripe>
          <el-table-column label="时间" width="150" align="center">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="username" label="账号" width="150" />
          <el-table-column label="姓名" width="140">
            <template #default="{ row }">{{ row.displayName ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
                {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.failReason ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="IP" width="150">
            <template #default="{ row }">{{ row.ip ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="客户端" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.ua ?? '-' }}</template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loadingLogin && !loginRows.length" description="没有登录记录" :image-size="60" />

        <div v-if="loginTotal > 0" class="pager">
          <el-pagination
            v-model:current-page="loginQuery.page"
            v-model:page-size="loginQuery.size"
            :total="loginTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadLogin"
            @size-change="reloadLogin"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="audit">
        <div class="toolbar">
          <el-input
            v-model="auditQuery.keyword"
            placeholder="模块 / 动作 / 对象类型"
            clearable
            style="width: 200px"
            @keyup.enter="reloadAudit"
          />
          <el-select v-model="auditQuery.result" placeholder="全部结果" clearable style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
          <el-date-picker
            v-model="auditRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
          <el-button type="primary" @click="reloadAudit">查询</el-button>
          <span class="hint">只查模块/动作/对象类型，不对 4000 字的明细做全表模糊匹配。</span>
        </div>

        <el-table v-loading="loadingAudit" :data="auditRows" border stripe>
          <el-table-column label="时间" width="150" align="center">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作人" width="140">
            <template #default="{ row }">{{ row.displayName ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="110" />
          <el-table-column prop="action" label="动作" min-width="180" show-overflow-tooltip />
          <el-table-column label="对象" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.objectType ? `${row.objectType}${row.objectId ? '#' + row.objectId : ''}` : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
                {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="IP" width="140">
            <template #default="{ row }">{{ row.ip ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="明细" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.detail ?? '-' }}</template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loadingAudit && !auditRows.length" description="没有操作记录" :image-size="60" />

        <div v-if="auditTotal > 0" class="pager">
          <el-pagination
            v-model:current-page="auditQuery.page"
            v-model:page-size="auditQuery.size"
            :total="auditTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadAudit"
            @size-change="reloadAudit"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { logApi, type AuditLogRow, type LoginLogRow, type LogSummary } from '@/api/system'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canRead = userStore.can('sys.log.read')

const tab = ref<'login' | 'audit'>('login')
const summary = ref<LogSummary | null>(null)

const loginRows = ref<LoginLogRow[]>([])
const loginTotal = ref(0)
const loadingLogin = ref(false)
const loginQuery = ref({ username: '', result: '', page: 1, size: 20 })
const loginRange = ref<[string, string] | null>(null)

const auditRows = ref<AuditLogRow[]>([])
const auditTotal = ref(0)
const loadingAudit = ref(false)
const auditQuery = ref({ keyword: '', result: '', page: 1, size: 20 })
const auditRange = ref<[string, string] | null>(null)

async function loadSummary(): Promise<void> {
  if (!canRead) {
    return
  }
  try {
    summary.value = await logApi.summary()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function loadLogin(): Promise<void> {
  if (!canRead) {
    return
  }
  loadingLogin.value = true
  try {
    const result = await logApi.login({
      username: loginQuery.value.username || undefined,
      result: loginQuery.value.result || undefined,
      from: loginRange.value?.[0],
      to: loginRange.value?.[1],
      page: loginQuery.value.page,
      size: loginQuery.value.size
    })
    loginRows.value = result.items
    loginTotal.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loadingLogin.value = false
  }
}

async function loadAudit(): Promise<void> {
  if (!canRead) {
    return
  }
  loadingAudit.value = true
  try {
    const result = await logApi.audit({
      keyword: auditQuery.value.keyword || undefined,
      result: auditQuery.value.result || undefined,
      from: auditRange.value?.[0],
      to: auditRange.value?.[1],
      page: auditQuery.value.page,
      size: auditQuery.value.size
    })
    auditRows.value = result.items
    auditTotal.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loadingAudit.value = false
  }
}

function reloadLogin(): void {
  loginQuery.value.page = 1
  void loadLogin()
}

function reloadAudit(): void {
  auditQuery.value.page = 1
  void loadAudit()
}

/** 首个 Tab 的数据按需加载：审计日志表数据量大，不跟着首屏一起查。 */
function onTabChange(name: string | number): void {
  if (name === 'audit' && !auditRows.value.length) {
    void loadAudit()
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
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

void loadSummary()
void loadLogin()
</script>

<style scoped>
.cards {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.card {
  flex: 1;
  min-width: 140px;
}

.card.wide {
  flex: 2;
}

.card-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.card-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 600;
}

.card-value.small {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.card-value.danger {
  color: var(--el-color-danger);
}

.card-value.ok {
  color: var(--el-color-success);
}

.card-value.warn {
  color: var(--el-color-warning);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
