<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">委托设置</span>
          <div>
            <el-button @click="back">返回流程中心</el-button>
            <el-button type="primary" @click="openCreate">新建委托</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="生效区间内，委托人名下的待办会出现在受托人的待办列表并可代办理，审批意见自动标注「代 XXX 办理」。"
        class="tip"
      />

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="委托人" width="150">
          <template #default="{ row }">{{ row.ownerName ?? row.ownerId }}</template>
        </el-table-column>
        <el-table-column label="受托人" width="150">
          <template #default="{ row }">{{ row.delegateName ?? row.delegateId }}</template>
        </el-table-column>
        <el-table-column label="生效开始" width="180">
          <template #default="{ row }">{{ formatTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="生效结束" width="180">
          <template #default="{ row }">{{ formatTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column label="限定流程" min-width="160">
          <template #default="{ row }">
            <span v-if="row.processKey">{{ row.processKey }}</span>
            <el-tag v-else size="small" type="info">全部流程</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusDot :tone="row.enabled ? 'ok' : 'muted'" :label="row.enabled ? '启用' : '停用'" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="onRemove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无委托规则" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建委托" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="受托人" prop="delegateId">
          <el-select
            v-model="form.delegateId"
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
        <el-form-item label="生效区间" prop="range">
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ssZ"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="限定流程">
          <el-input v-model="form.processKey" placeholder="留空表示全部流程" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { workflowApi, type ActorUserRow, type DelegateRow } from '@/api/workflow'
import StatusDot from '@/components/StatusDot.vue'

const router = useRouter()

const rows = ref<DelegateRow[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const userLoading = ref(false)
const users = ref<ActorUserRow[]>([])
const formRef = ref<FormInstance>()

const form = reactive<{
  delegateId: string
  range: [string, string] | null
  processKey: string
  remark: string
}>({
  delegateId: '',
  range: null,
  processKey: '',
  remark: ''
})

const rules: FormRules = {
  delegateId: [{ required: true, message: '请选择受托人', trigger: 'change' }],
  range: [{ required: true, message: '请选择生效区间', trigger: 'change' }]
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await workflowApi.delegates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载委托失败')
  } finally {
    loading.value = false
  }
}

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    users.value = await workflowApi.actorUsers(keyword)
  } catch {
    users.value = []
  } finally {
    userLoading.value = false
  }
}

function openCreate(): void {
  form.delegateId = ''
  form.range = null
  form.processKey = ''
  form.remark = ''
  dialogVisible.value = true
  searchUsers('')
}

async function onSave(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !form.range) {
    return
  }
  saving.value = true
  try {
    await workflowApi.createDelegate({
      delegateId: form.delegateId,
      startAt: form.range[0],
      endAt: form.range[1],
      processKey: form.processKey || null,
      remark: form.remark || null
    })
    ElMessage.success('已创建委托')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建委托失败')
  } finally {
    saving.value = false
  }
}

async function onRemove(row: DelegateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除「${row.ownerName ?? row.ownerId} → ${row.delegateName ?? row.delegateId}」的委托？`,
      '删除委托',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await workflowApi.removeDelegate(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
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

.tip {
  margin-bottom: 12px;
}
</style>
