<template>
  <div class="wf-panel">
    <div class="toolbar">
      <el-button type="primary" :disabled="!canStart" @click="openStart">发起审批</el-button>
      <span class="hint">
        提交后对象状态自动流转：发起 → 审批中；会签全票通过 → 已发布；撤回 → 回到草稿
      </span>
    </div>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="processKey" label="流程" width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前节点" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.currentActivities.length ? row.currentActivities.join(' / ') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="发起时间" width="170">
        <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">{{ formatTime(row.endedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openInstance(row.processInstanceId)">查看</el-button>
          <el-button
            v-if="row.status === 'RUNNING' && row.startedBy === currentUserId"
            link
            type="danger"
            @click="onWithdraw(row)"
          >
            撤回
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="该对象尚无审批流程，点击「发起审批」提交" />
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" title="发起审批" width="560px">
      <el-form label-width="90px">
        <el-form-item label="初审人" required>
          <el-select
            v-model="form.approverId"
            filterable
            remote
            reserve-keyword
            placeholder="输入姓名或账号搜索"
            :remote-method="searchUsers"
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.displayName}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会签人" required>
          <el-select
            v-model="form.approverIds"
            multiple
            filterable
            remote
            reserve-keyword
            collapse-tags
            collapse-tags-tooltip
            placeholder="可多选，需全部通过流程才归档"
            :remote-method="searchUsers"
            :loading="userLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.displayName}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onStart">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workflowApi, type ActorUserRow, type InstanceRow } from '@/api/workflow'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ objectType: string; objectId: string }>()
const emit = defineEmits<{ (e: 'changed'): void }>()

const router = useRouter()
const userStore = useUserStore()

const rows = ref<InstanceRow[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const userLoading = ref(false)
const users = ref<ActorUserRow[]>([])

const form = reactive<{ approverId: string; approverIds: string[]; remark: string }>({
  approverId: '',
  approverIds: [],
  remark: ''
})

const canStart = computed(() => userStore.can('wf.process.start'))
const currentUserId = computed(() => userStore.user?.id ?? '')

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

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load(): Promise<void> {
  if (!props.objectId) {
    return
  }
  loading.value = true
  try {
    rows.value = await workflowApi.objectInstances(props.objectType, props.objectId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载流程失败')
  } finally {
    loading.value = false
  }
}

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    const list = await workflowApi.actorUsers(keyword)
    const me = userStore.user
    if (me && !list.some((user) => user.id === me.id)) {
      list.unshift({ id: me.id, username: me.username, displayName: me.displayName })
    }
    users.value = list
  } catch {
    users.value = []
  } finally {
    userLoading.value = false
  }
}

function openStart(): void {
  form.approverId = userStore.user?.id ?? ''
  form.approverIds = []
  form.remark = ''
  dialogVisible.value = true
  searchUsers('')
}

async function onStart(): Promise<void> {
  if (!form.approverId) {
    ElMessage.warning('请选择初审人')
    return
  }
  if (form.approverIds.length === 0) {
    ElMessage.warning('请至少选择一名会签人')
    return
  }
  submitting.value = true
  try {
    await workflowApi.start({
      objectType: props.objectType,
      objectId: props.objectId,
      approverId: form.approverId,
      approverIds: form.approverIds,
      remark: form.remark || undefined
    })
    ElMessage.success('已发起审批')
    dialogVisible.value = false
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发起审批失败')
  } finally {
    submitting.value = false
  }
}

async function onWithdraw(row: InstanceRow): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('撤回后流程终止、对象回到草稿，请填写原因', '撤回流程', {
      inputType: 'textarea',
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消'
    })
    reason = result.value ?? ''
  } catch {
    return
  }
  try {
    await workflowApi.withdraw(row.processInstanceId, reason || undefined)
    ElMessage.success('已撤回')
    await load()
    emit('changed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

function openInstance(processInstanceId: string): void {
  router.push({ name: 'workflow-instance', params: { id: processInstanceId } })
}

watch(() => [props.objectType, props.objectId], load)
onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.hint {
  color: #909399;
  font-size: 12px;
}
</style>
