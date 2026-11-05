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
          v-model="query.keyword"
          placeholder="编码或名称"
          clearable
          class="filter-input"
          @keyup.enter="search"
        />
        <el-select v-model="query.docType" placeholder="文档类型" clearable class="filter-select">
          <el-option v-for="(label, value) in DOC_TYPE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.lifecycleState" placeholder="状态" clearable class="filter-select">
          <el-option v-for="(label, value) in DOC_STATE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border @row-click="openDetail">
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeLabel(row.docType) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  DOC_SOURCE_LABEL,
  DOC_STATE_LABEL,
  DOC_TYPE_LABEL,
  docApi,
  type DocSaveReq,
  type DocSummary
} from '@/api/doc'

const router = useRouter()

const rows = ref<DocSummary[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  docType: '',
  lifecycleState: ''
})

const form = reactive<DocSaveReq>({
  code: '',
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
      keyword: query.keyword || undefined,
      docType: query.docType || undefined,
      lifecycleState: query.lifecycleState || undefined
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

function openCreate(): void {
  form.code = ''
  form.name = ''
  form.docType = 'DESIGN'
  form.source = 'NORMAL'
  form.keyword = ''
  form.description = ''
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
    const created = await docApi.create({ ...form })
    ElMessage.success(`已创建 ${created.code}`)
    dialogVisible.value = false
    router.push({ name: 'doc-detail', params: { id: created.id } })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    saving.value = false
  }
}

function openDetail(row: DocSummary): void {
  router.push({ name: 'doc-detail', params: { id: row.id } })
}

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

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
