<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title">
            <el-button link @click="goBack">← 返回</el-button>
            <span class="code">{{ detail?.code }}</span>
            <span class="name">{{ detail?.name }}</span>
            <el-tag size="small" type="info">{{ detail?.revisionLabel }}</el-tag>
            <el-tag size="small" :type="stateTag(detail?.lifecycleState)">{{ stateLabel(detail?.lifecycleState) }}</el-tag>
            <el-tag v-if="!detail?.isLatest" size="small" type="warning">历史版本</el-tag>
          </div>
          <div>
            <el-button v-if="canWrite" @click="openEdit">编辑属性</el-button>
            <el-button v-if="canWrite && canEdit && !detail?.checkout" type="primary" @click="onCheckout">
              检出
            </el-button>
            <el-button v-if="isMyCheckout" type="success" @click="onCheckin">检入</el-button>
            <el-button v-if="isMyCheckout" @click="onCancelCheckout">取消检出</el-button>
            <el-button v-if="canForceUnlock" type="warning" @click="onForceUnlock">强制解锁</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="detail?.checkout"
        class="checkout-tip"
        type="warning"
        :closable="false"
        show-icon
        :title="`已由 ${detail.checkout.userLabel || detail.checkout.userId} 检出，到期时间 ${formatTime(detail.checkout.expireAt)}`"
      />

      <el-tabs v-model="activeTab">
        <el-tab-pane label="属性" name="props">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="编码">{{ detail?.code }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detail?.name }}</el-descriptions-item>
            <el-descriptions-item label="文档类型">{{ typeLabel(detail?.docType) }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ sourceLabel(detail?.source) }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail?.revisionLabel }}</el-descriptions-item>
            <el-descriptions-item label="生命周期">{{ stateLabel(detail?.lifecycleState) }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ detail?.keyword || '-' }}</el-descriptions-item>
            <el-descriptions-item label="页数">{{ detail?.pageCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="文件数">{{ detail?.fileCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="主文件扩展名">{{ detail?.fileExt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(detail?.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(detail?.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ detail?.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="文件" name="files">
          <div class="toolbar">
            <el-select v-model="uploadRole" class="role-select">
              <el-option v-for="(label, value) in FILE_ROLE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
            <el-upload :show-file-list="false" :http-request="onUpload" :disabled="!canWrite || !canEdit">
              <el-button type="primary" :disabled="!canWrite || !canEdit">上传文件</el-button>
            </el-upload>
            <span class="hint">同一角色重复上传会生成新版本，旧版本保留可追溯</span>
          </div>

          <el-table :data="detail?.files ?? []" border>
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column label="角色" width="110">
              <template #default="{ row }">{{ FILE_ROLE_LABEL[row.fileRole] ?? row.fileRole }}</template>
            </el-table-column>
            <el-table-column label="大小" width="110">
              <template #default="{ row }">{{ formatSize(row.size) }}</template>
            </el-table-column>
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column label="上传时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="onDownload(row)">下载</el-button>
                <el-button link type="danger" :disabled="!canEdit" @click="onRemoveFile(row)">移除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无文件，点击「上传文件」添加主文件或附件" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="版本历史" name="versions">
          <el-table :data="versions" border @row-click="switchVersion">
            <el-table-column prop="revisionLabel" label="版本" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="stateTag(row.lifecycleState)">{{ stateLabel(row.lifecycleState) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否最新" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.isLatest" size="small" type="success">最新</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="switchVersion(row)">查看该版本</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑文档属性" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码">
          <el-input :model-value="detail?.code" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="文档类型" prop="docType">
          <el-select v-model="form.docType" style="width: 100%">
            <el-option v-for="(label, value) in DOC_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-radio-group v-model="form.source">
            <el-radio value="NORMAL">普通文档</el-radio>
            <el-radio value="CAD">CAD 文档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import {
  DOC_SOURCE_LABEL,
  DOC_STATE_LABEL,
  DOC_TYPE_LABEL,
  FILE_ROLE_LABEL,
  docApi,
  type DocDetail,
  type DocFile,
  type DocSaveReq,
  type DocVersion,
  type FileRole
} from '@/api/doc'
import { workspaceApi } from '@/api/workspace'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const route = useRoute()
const router = useRouter()

const docId = computed(() => String(route.params.id ?? ''))
const detail = ref<DocDetail | null>(null)
const versions = ref<DocVersion[]>([])
const activeTab = ref('props')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const uploadRole = ref<FileRole>('MAIN')
const formRef = ref<FormInstance>()

const form = reactive<DocSaveReq>({
  name: '',
  docType: 'DESIGN',
  source: 'NORMAL',
  keyword: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  docType: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

const canEdit = computed(() => {
  const state = detail.value?.lifecycleState
  return state !== 'Released' && state !== 'Obsolete'
})

const canWrite = computed(() => userStore.can('pdm.doc.write'))

const isMyCheckout = computed(() => {
  const checkout = detail.value?.checkout
  return Boolean(checkout) && String(checkout?.userId) === String(userStore.user?.id)
})

const canForceUnlock = computed(
  () => userStore.can('pdm.doc.release') && Boolean(detail.value?.checkout) && !isMyCheckout.value
)

function formatTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function formatSize(size?: number): string {
  if (!size) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function typeLabel(value?: string): string {
  return value ? DOC_TYPE_LABEL[value] ?? value : '-'
}

function sourceLabel(value?: string): string {
  return value ? DOC_SOURCE_LABEL[value] ?? value : '-'
}

function stateLabel(value?: string): string {
  return value ? DOC_STATE_LABEL[value] ?? value : '-'
}

function stateTag(state?: string): 'success' | 'info' | 'warning' | 'danger' {
  if (state === 'Released') {
    return 'success'
  }
  if (state === 'InWorkflow') {
    return 'warning'
  }
  if (state === 'Obsolete') {
    return 'danger'
  }
  return 'info'
}

async function load(): Promise<void> {
  if (!docId.value) {
    return
  }
  loading.value = true
  try {
    detail.value = await docApi.detail(docId.value)
    versions.value = await docApi.versions(docId.value)
    workspaceApi.touchRecent(docId.value).catch(() => undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function goBack(): void {
  router.push({ name: 'doc-list' })
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.name = detail.value.name
  form.docType = detail.value.docType
  form.source = detail.value.source
  form.keyword = detail.value.keyword ?? ''
  form.description = detail.value.description ?? ''
  dialogVisible.value = true
}

async function onSave(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await docApi.update(docId.value, { ...form })
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onUpload(options: UploadRequestOptions): Promise<void> {
  try {
    await docApi.upload(docId.value, options.file as File, uploadRole.value)
    ElMessage.success('上传成功')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  }
}

async function onDownload(file: DocFile): Promise<void> {
  try {
    await docApi.download(file)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  }
}

async function onRemoveFile(file: DocFile): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认移除文件「${file.fileName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await docApi.removeFile(docId.value, file.linkId)
    ElMessage.success('已移除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function onCheckout(): Promise<void> {
  try {
    await ElMessageBox.confirm('检出后其他人只能查看，确认检出？', '提示', { type: 'info' })
  } catch {
    return
  }
  try {
    await docApi.checkout(docId.value)
    ElMessage.success('已检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检出失败')
  }
}

async function onCheckin(): Promise<void> {
  const result = await ElMessageBox.prompt('检入将生成新的小版本，可填写说明（可选）', '检入', {
    inputPlaceholder: '本次修改说明',
    inputValue: '',
    confirmButtonText: '检入',
    cancelButtonText: '取消'
  }).catch(() => null)
  if (!result) {
    return
  }
  try {
    const next = await docApi.checkin(docId.value, { remark: result.value || undefined })
    ElMessage.success(`已检入，新版本 ${next.revisionLabel}`)
    router.replace({ name: 'doc-detail', params: { id: next.id } })
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检入失败')
  }
}

async function onCancelCheckout(): Promise<void> {
  try {
    await ElMessageBox.confirm('取消检出将丢弃本次编辑（不产生新版本），确认？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await docApi.cancelCheckout(docId.value)
    ElMessage.success('已取消检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function onForceUnlock(): Promise<void> {
  try {
    await ElMessageBox.confirm('强制解锁会中断他人编辑并记录审计，确认？', '强制解锁', { type: 'warning' })
  } catch {
    return
  }
  try {
    const holder = await docApi.forceUnlock(docId.value)
    ElMessage.success(`已解锁${holder ? `（原持有者 ${holder}）` : ''}`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '解锁失败')
  }
}

function switchVersion(version: DocVersion): void {
  if (version.id === docId.value) {
    return
  }
  router.replace({ name: 'doc-detail', params: { id: version.id } })
}

watch(docId, load)
onMounted(load)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.code {
  font-weight: 600;
  color: #409eff;
}

.name {
  font-weight: 600;
}

.checkout-tip {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.role-select {
  width: 140px;
}

.hint {
  color: #909399;
  font-size: 12px;
}
</style>
