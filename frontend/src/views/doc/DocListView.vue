<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>图文档</span>
          <div>
            <el-button type="primary" @click="openCreate">新建文档</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="query.q"
          placeholder="全文检索：编码/名称/关键词/描述"
          clearable
          class="filter-input"
          @keyup.enter="search"
        />
        <el-button @click="openAdvanced">高级查询</el-button>
        <el-select v-model="query.docType" placeholder="文档类型" clearable class="filter-select">
          <el-option v-for="(label, value) in DOC_TYPE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.lifecycleState" placeholder="状态" clearable class="filter-select">
          <el-option v-for="(label, value) in DOC_STATE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-tree-select
          v-model="query.classId"
          :data="classTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          placeholder="分类（含子分类）"
          class="filter-tree"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button :disabled="selectedIds.length === 0" :loading="downloading" @click="batchDownload">
          批量下载{{ selectedIds.length ? `(${selectedIds.length})` : '' }}
        </el-button>
        <el-button :disabled="selectedIds.length === 0" @click="openGrant">批量授权</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        border
        @row-click="openDetail"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column label="名称" min-width="240">
          <template #default="{ row }">
            <div>{{ row.name }}</div>
            <div v-if="row.highlight" class="highlight" v-html="safeHighlight(row.highlight)" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeLabel(row.docType) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
        </el-table-column>
        <el-table-column label="分类" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column prop="revisionLabel" label="版本" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="stateTag(row.lifecycleState)" size="small">{{ stateLabel(row.lifecycleState) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileCount" label="文件数" width="80" />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">打开</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文档，点击「新建文档」开始" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <el-dialog v-model="grantVisible" title="批量授权" width="540px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="`将对选中的 ${selectedIds.length} 个文档下发同一条授权`"
        class="grant-tip"
      />
      <el-form label-width="100px">
        <el-form-item label="被授权者类型">
          <el-select v-model="grantForm.actorType" style="width: 100%">
            <el-option v-for="item in ACL_ACTOR_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needActorId" label="被授权者 ID">
          <el-input v-model="grantForm.actorId" placeholder="用户 / 角色 / 用户组的 ID" />
        </el-form-item>
        <el-form-item label="操作">
          <el-checkbox-group v-model="grantForm.actions">
            <el-checkbox v-for="item in ACL_ACTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="效果">
          <el-radio-group v-model="grantForm.effect">
            <el-radio value="ALLOW">允许</el-radio>
            <el-radio value="DENY">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="granting" @click="submitGrant">确定授权</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="advancedVisible" title="高级查询" size="620px">
      <el-form label-width="90px">
        <el-form-item label="全文检索">
          <el-input v-model="query.q" placeholder="编码/名称/关键词/描述" clearable />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="query.docType" clearable placeholder="全部" style="width: 100%">
            <el-option v-for="(label, value) in DOC_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.lifecycleState" clearable placeholder="全部" style="width: 100%">
            <el-option v-for="(label, value) in DOC_STATE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="query.classId"
            :data="classTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="含子分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runAdvanced">查询</el-button>
          <el-button @click="resetQuery">清空条件</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">保存为常用查询</el-divider>
      <div class="save-bar">
        <el-input v-model="saveForm.name" placeholder="查询名称，如「我的装配图」" class="save-name" />
        <el-switch v-model="saveForm.shared" active-text="共享给他人" />
        <el-button type="primary" :loading="saving" @click="saveQuery">保存</el-button>
      </div>

      <el-divider content-position="left">已保存的查询</el-divider>
      <el-table :data="savedQueries" border size="small">
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="创建人" width="110" />
        <el-table-column label="共享" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.shared" size="small" type="success">共享</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="applyQuery(row)">应用</el-button>
            <el-button link type="danger" :disabled="!row.mine" @click="removeQuery(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无保存的查询条件" />
        </template>
      </el-table>
    </el-drawer>

    <el-dialog v-model="dialogVisible" title="新建文档" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="留空自动取号（DOC-000001）" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 主传动装配图" />
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
        <el-form-item label="分类">
          <el-tree-select
            v-model="form.classId"
            :data="classTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="可不选；选择后需填必填属性"
            style="width: 100%"
            @change="onClassChange"
          />
        </el-form-item>
        <el-form-item v-for="attr in classAttrs" :key="attr.code" :label="attr.name" :required="attr.required">
          <el-select
            v-if="attr.valueType === 'ENUM'"
            v-model="form.classAttrs![attr.code]"
            clearable
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option
              v-for="item in enumItems(attr.enumCode)"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
          <el-date-picker
            v-else-if="attr.valueType === 'DATE'"
            v-model="form.classAttrs![attr.code]"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="yyyy-MM-dd"
            style="width: 100%"
          />
          <el-radio-group v-else-if="attr.valueType === 'BOOL'" v-model="form.classAttrs![attr.code]">
            <el-radio value="true">是</el-radio>
            <el-radio value="false">否</el-radio>
          </el-radio-group>
          <el-input
            v-else
            v-model="form.classAttrs![attr.code]"
            :placeholder="attr.inherited ? `继承自 ${attr.ownerClassName}` : '可留空'"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" placeholder="便于检索，可留空" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ACL_ACTIONS,
  ACL_ACTOR_TYPES,
  DOC_SOURCE_LABEL,
  DOC_STATE_LABEL,
  DOC_TYPE_LABEL,
  docApi,
  docBatchApi,
  docClassApi,
  docQueryApi,
  type DocClassAttrDef,
  type DocClassNode,
  type DocSavedQuery,
  type DocSaveReq,
  type DocSummary
} from '@/api/doc'
import { metaApi } from '@/api/system'

const router = useRouter()

const rows = ref<DocSummary[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const classTree = ref<DocClassNode[]>([])
const classAttrs = ref<DocClassAttrDef[]>([])
const enumCache = ref<Record<string, { code: string; name: string }[]>>({})

const query = reactive({
  page: 1,
  size: 20,
  q: '',
  docType: '',
  lifecycleState: '',
  classId: '' as string
})

const advancedVisible = ref(false)
const savedQueries = ref<DocSavedQuery[]>([])
const saveForm = reactive({ name: '', shared: false })

const selectedIds = ref<string[]>([])
const downloading = ref(false)
const grantVisible = ref(false)
const granting = ref(false)
const grantForm = reactive({
  actorType: 'USER',
  actorId: '',
  actions: ['READ'] as string[],
  effect: 'ALLOW'
})

const needActorId = computed(() => !['ALL', 'CREATOR', 'OWNER'].includes(grantForm.actorType))

const form = reactive<DocSaveReq>({
  code: '',
  name: '',
  docType: 'DESIGN',
  source: 'NORMAL',
  keyword: '',
  description: '',
  classId: null,
  classAttrs: {}
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  docType: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

function formatTime(value?: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function typeLabel(value: string): string {
  return DOC_TYPE_LABEL[value] ?? value
}

function sourceLabel(value: string): string {
  return DOC_SOURCE_LABEL[value] ?? value
}

function stateLabel(value: string): string {
  return DOC_STATE_LABEL[value] ?? value
}

function stateTag(state: string): 'success' | 'info' | 'warning' | 'danger' {
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
  loading.value = true
  try {
    const page = await docApi.list({
      page: query.page,
      size: query.size,
      q: query.q || undefined,
      docType: query.docType || undefined,
      lifecycleState: query.lifecycleState || undefined,
      classId: query.classId || undefined
    })
    rows.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  query.page = 1
  load()
}

/** 只放行后端插入的 mark 标记，其余 HTML 一律转义，避免检索词注入。 */
function safeHighlight(text?: string | null): string {
  if (!text) {
    return ''
  }
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/&lt;mark&gt;/g, '<mark>')
    .replace(/&lt;\/mark&gt;/g, '</mark>')
}

async function openAdvanced(): Promise<void> {
  advancedVisible.value = true
  await loadSavedQueries()
}

async function loadSavedQueries(): Promise<void> {
  try {
    savedQueries.value = await docQueryApi.list()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载保存的查询失败')
  }
}

function runAdvanced(): void {
  search()
  advancedVisible.value = false
}

function resetQuery(): void {
  query.q = ''
  query.docType = ''
  query.lifecycleState = ''
  query.classId = ''
  query.page = 1
  load()
}

function currentCondition(): Record<string, string> {
  const condition: Record<string, string> = {}
  if (query.q) {
    condition.q = query.q
  }
  if (query.docType) {
    condition.docType = query.docType
  }
  if (query.lifecycleState) {
    condition.lifecycleState = query.lifecycleState
  }
  if (query.classId) {
    condition.classId = query.classId
  }
  return condition
}

async function saveQuery(): Promise<void> {
  if (!saveForm.name.trim()) {
    ElMessage.warning('请输入查询名称')
    return
  }
  const condition = currentCondition()
  if (Object.keys(condition).length === 0) {
    ElMessage.warning('当前没有查询条件可保存')
    return
  }
  saving.value = true
  try {
    await docQueryApi.create({
      name: saveForm.name.trim(),
      conditionJson: JSON.stringify(condition),
      shared: saveForm.shared
    })
    ElMessage.success('已保存')
    saveForm.name = ''
    await loadSavedQueries()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function applyQuery(row: DocSavedQuery): void {
  try {
    const condition = JSON.parse(row.conditionJson) as Record<string, string>
    query.q = condition.q ?? ''
    query.docType = condition.docType ?? ''
    query.lifecycleState = condition.lifecycleState ?? ''
    query.classId = condition.classId ?? ''
    query.page = 1
    load()
    advancedVisible.value = false
    ElMessage.success(`已应用「${row.name}」`)
  } catch {
    ElMessage.error('查询条件格式不合法')
  }
}

async function removeQuery(row: DocSavedQuery): Promise<void> {
  try {
    await docQueryApi.remove(row.id)
    ElMessage.success('已删除')
    await loadSavedQueries()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function loadClassTree(): Promise<void> {
  try {
    classTree.value = await docClassApi.tree()
  } catch {
    classTree.value = []
  }
}

async function ensureEnums(codes: string[]): Promise<void> {
  for (const code of codes) {
    if (enumCache.value[code]) {
      continue
    }
    try {
      const detail = await metaApi.getEnum(code)
      enumCache.value[code] = detail.items.map((item) => ({ code: item.code, name: item.name }))
    } catch {
      enumCache.value[code] = []
    }
  }
}

function enumItems(code?: string | null): { code: string; name: string }[] {
  return code ? enumCache.value[code] ?? [] : []
}

async function onClassChange(value: string | null): Promise<void> {
  form.classAttrs = {}
  classAttrs.value = []
  if (!value) {
    return
  }
  try {
    const attrs = await docClassApi.attrs(value)
    classAttrs.value = attrs
    const initial: Record<string, string> = {}
    attrs.forEach((attr) => {
      initial[attr.code] = attr.defaultValue ?? ''
    })
    form.classAttrs = initial
    await ensureEnums(attrs.map((attr) => attr.enumCode).filter((code): code is string => Boolean(code)))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类属性失败')
  }
}

function cleanClassAttrs(): Record<string, string> {
  const result: Record<string, string> = {}
  Object.entries(form.classAttrs ?? {}).forEach(([key, value]) => {
    const text = value === null || value === undefined ? '' : String(value).trim()
    if (text) {
      result[key] = text
    }
  })
  return result
}

function openCreate(): void {
  form.code = ''
  form.name = ''
  form.docType = 'DESIGN'
  form.source = 'NORMAL'
  form.keyword = ''
  form.description = ''
  form.classId = null
  form.classAttrs = {}
  classAttrs.value = []
  dialogVisible.value = true
}

async function onCreate(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const created = await docApi.create({
      ...form,
      classId: form.classId || null,
      classAttrs: cleanClassAttrs()
    })
    ElMessage.success(`已创建 ${created.code}`)
    dialogVisible.value = false
    router.push({ name: 'doc-detail', params: { id: created.id } })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    saving.value = false
  }
}

function openDetail(row: DocSummary, column?: { type?: string }): void {
  if (column?.type === 'selection') {
    return
  }
  router.push({ name: 'doc-detail', params: { id: row.id } })
}

function onSelectionChange(selection: DocSummary[]): void {
  selectedIds.value = selection.map((row) => row.id)
}

async function batchDownload(): Promise<void> {
  if (selectedIds.value.length === 0) {
    return
  }
  downloading.value = true
  try {
    await docBatchApi.download(selectedIds.value)
    ElMessage.success(`已打包下载 ${selectedIds.value.length} 个文档`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量下载失败')
  } finally {
    downloading.value = false
  }
}

function openGrant(): void {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选文档')
    return
  }
  grantForm.actorType = 'USER'
  grantForm.actorId = ''
  grantForm.actions = ['READ']
  grantForm.effect = 'ALLOW'
  grantVisible.value = true
}

async function submitGrant(): Promise<void> {
  if (grantForm.actions.length === 0) {
    ElMessage.warning('请选择授权操作')
    return
  }
  if (needActorId.value && !grantForm.actorId.trim()) {
    ElMessage.warning('请填写被授权者 ID')
    return
  }
  granting.value = true
  try {
    const results = await docBatchApi.grant({
      ids: selectedIds.value,
      actorType: grantForm.actorType,
      actorId: needActorId.value ? grantForm.actorId.trim() : null,
      actions: grantForm.actions.join(','),
      effect: grantForm.effect
    })
    const failed = results.filter((item) => !item.ok)
    if (failed.length === 0) {
      ElMessage.success(`已为 ${results.length} 个文档授权`)
    } else {
      ElMessage.warning(
        `${results.length - failed.length} 个成功，${failed.length} 个失败：${failed[0]?.message ?? ''}`
      )
    }
    grantVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量授权失败')
  } finally {
    granting.value = false
  }
}

onMounted(() => {
  load()
  loadClassTree()
})
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
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 160px;
}

.filter-tree {
  width: 200px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.highlight {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.highlight :deep(mark) {
  background: #fff3c4;
  color: #d46b08;
  padding: 0 2px;
}

.save-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-name {
  width: 240px;
}

.grant-tip {
  margin-bottom: 12px;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
