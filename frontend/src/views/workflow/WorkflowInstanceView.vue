<template>
  <div class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">流程详情</span>
          <div>
            <el-button @click="back">返回流程中心</el-button>
            <el-button type="primary" @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="流程定义">{{ detail.processKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTag(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="业务对象">
            {{ objectText(detail.objectCode, detail.objectName) }}
          </el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ detail.objectType }}</el-descriptions-item>
          <el-descriptions-item label="发起人">
            {{ detail.startedByName ?? detail.startedBy }}
          </el-descriptions-item>
          <el-descriptions-item label="发起时间">{{ formatTime(detail.startedAt) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatTime(detail.endedAt) }}</el-descriptions-item>
          <el-descriptions-item label="流程实例">{{ detail.processInstanceId }}</el-descriptions-item>
        </el-descriptions>

        <div class="section">
          <div class="section-title">当前待办任务</div>
          <el-table :data="detail.activeTasks" border>
            <el-table-column prop="name" label="审批节点" min-width="160" />
            <el-table-column label="办理人" width="180">
              <template #default="{ row }">{{ row.assignee ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="到达时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="流程已无待办任务" />
            </template>
          </el-table>
        </div>

        <div class="section">
          <div class="section-title">审批意见</div>
          <el-timeline v-if="detail.comments.length">
            <el-timeline-item
              v-for="(item, index) in detail.comments"
              :key="index"
              :timestamp="formatTime(item.createdAt)"
              :type="commentType(item.action)"
              placement="top"
            >
              <div class="comment-head">
                <el-tag size="small" :type="commentType(item.action)">{{ actionLabel(item.action) }}</el-tag>
                <span class="actor">{{ item.userName ?? item.userId ?? '系统' }}</span>
              </div>
              <div v-if="item.comment" class="comment-body">{{ item.comment }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批意见" />
        </div>

        <div class="section">
          <div class="section-title">电子签名</div>
          <el-table :data="signatures" border>
            <el-table-column label="签名人" width="150">
              <template #default="{ row }">{{ row.signerName ?? row.signerId }}</template>
            </el-table-column>
            <el-table-column prop="meaning" label="签名含义" width="130" />
            <el-table-column prop="revisionLabel" label="版本" width="100" />
            <el-table-column label="签名时间" width="180">
              <template #default="{ row }">{{ formatTime(row.signTime) }}</template>
            </el-table-column>
            <el-table-column label="文件摘要" min-width="180">
              <template #default="{ row }">{{ shortHash(row.fileSha256) }}</template>
            </el-table-column>
            <el-table-column label="核验" width="130">
              <template #default="{ row }">
                <el-tag size="small" :type="verifyTag(row.verifyResult)">
                  {{ verifyLabel(row.verifyResult) }}
                </el-tag>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无签名记录" />
            </template>
          </el-table>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workflowApi, type InstanceDetail, type SignatureRow } from '@/api/workflow'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<InstanceDetail | null>(null)
const signatures = ref<SignatureRow[]>([])

function processInstanceId(): string {
  return String(route.params.id ?? '')
}

function objectText(code?: string | null, name?: string | null): string {
  if (!code && !name) {
    return '-'
  }
  return `${code ?? ''} ${name ?? ''}`.trim()
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function shortHash(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return `${value.slice(0, 12)}…`
}

function statusLabel(status: string): string {
  switch (status) {
    case 'RUNNING':
      return '审批中'
    case 'COMPLETED':
      return '已完成'
    case 'TERMINATED':
      return '已终止'
    default:
      return status
  }
}

function statusTag(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'RUNNING':
      return 'warning'
    case 'TERMINATED':
      return 'danger'
    default:
      return 'info'
  }
}

function actionLabel(action: string): string {
  switch (action) {
    case 'START':
      return '发起'
    case 'APPROVE':
      return '通过'
    case 'REJECT':
      return '驳回'
    case 'WITHDRAW':
      return '撤回'
    case 'ADD_SIGNER':
      return '加签'
    case 'REMIND':
      return '催办'
    default:
      return action
  }
}

function commentType(action: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (action) {
    case 'APPROVE':
      return 'success'
    case 'REJECT':
      return 'danger'
    case 'WITHDRAW':
      return 'warning'
    case 'REMIND':
      return 'warning'
    case 'START':
      return 'primary'
    default:
      return 'info'
  }
}

function verifyLabel(result: string): string {
  switch (result) {
    case 'VALID':
      return '文件一致'
    case 'CHANGED':
      return '文件已变更'
    default:
      return '无法核验'
  }
}

function verifyTag(result: string): 'success' | 'danger' | 'info' {
  switch (result) {
    case 'VALID':
      return 'success'
    case 'CHANGED':
      return 'danger'
    default:
      return 'info'
  }
}

async function load(): Promise<void> {
  const id = processInstanceId()
  if (!id) {
    ElMessage.error('缺少流程实例 id')
    return
  }
  loading.value = true
  try {
    detail.value = await workflowApi.instance(id)
    signatures.value = await workflowApi
      .signatures(detail.value.objectType, detail.value.objectId)
      .catch(() => [])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载流程详情失败')
  } finally {
    loading.value = false
  }
}

function back(): void {
  router.push({ name: 'workflow-center' })
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.section {
  margin-top: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #2b4a6f;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.actor {
  color: #303133;
  font-weight: 500;
}

.comment-body {
  margin-top: 6px;
  color: #606266;
  white-space: pre-wrap;
}
</style>
