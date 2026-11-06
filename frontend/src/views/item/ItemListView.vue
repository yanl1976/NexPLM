<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">零部件</span>
          <el-button type="primary" @click="openCreate">新建零部件</el-button>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称 / 规格"
          clearable
          class="filter-input"
          @keyup.enter="search"
        />
        <el-select v-model="query.itemType" placeholder="类型" clearable class="filter-select">
          <el-option v-for="(label, value) in ITEM_TYPE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.sourceType" placeholder="供应方式" clearable class="filter-select">
          <el-option v-for="(label, value) in ITEM_SOURCE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border @row-click="openDetail">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType }}</template>
        </el-table-column>
        <el-table-column prop="specification" label="规格型号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="material" label="材料" width="110" />
        <el-table-column label="供应方式" width="100">
          <template #default="{ row }">
            <StatusDot
              :tone="row.sourceType === 'BUY' ? 'warn' : 'ok'"
              :label="ITEM_SOURCE_LABEL[row.sourceType] ?? row.sourceType"
            />
          </template>
        </el-table-column>
        <el-table-column prop="revisionLabel" label="版本" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">打开</el-button>
            <el-button link type="danger" @click.stop="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无零部件，点击「新建零部件」开始" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑零部件' : '新建零部件'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="留空自动取号 PN-000001" :disabled="Boolean(editingId)" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="itemType">
          <el-select v-model="form.itemType" style="width: 100%">
            <el-option v-for="(label, value) in ITEM_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应方式">
          <el-radio-group v-model="form.sourceType">
            <el-radio value="MAKE">自制</el-radio>
            <el-radio value="BUY">外购</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specification" placeholder="如 M3 Z=24 / Φ50×120" />
        </el-form-item>
        <el-form-item label="材料">
          <el-input v-model="form.material" placeholder="如 40Cr" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" placeholder="EA / PCS / KG" />
        </el-form-item>
        <el-form-item label="单件重量">
          <el-input-number v-model="form.weight" :min="0" :precision="4" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标准号">
          <el-input v-model="form.stdCode" placeholder="标准件填写，如 GB/T 5783" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import {
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  itemApi,
  type ItemSaveReq,
  type ItemSummary
} from '@/api/item'
import { DOC_STATE_LABEL } from '@/api/doc'
import StatusDot from '@/components/StatusDot.vue'

const router = useRouter()

const rows = ref<ItemSummary[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  itemType: '',
  sourceType: ''
})

const form = reactive<ItemSaveReq>({
  code: '',
  name: '',
  itemType: 'PART',
  sourceType: 'MAKE',
  specification: '',
  material: '',
  unit: 'EA',
  weight: null,
  stdCode: '',
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function stateLabel(state?: string | null): string {
  return state ? DOC_STATE_LABEL[state] ?? state : '-'
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await itemApi.list({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      itemType: query.itemType || undefined,
      sourceType: query.sourceType || undefined
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
  editingId.value = null
  form.code = ''
  form.name = ''
  form.itemType = 'PART'
  form.sourceType = 'MAKE'
  form.specification = ''
  form.material = ''
  form.unit = 'EA'
  form.weight = null
  form.stdCode = ''
  form.remark = ''
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
    if (editingId.value) {
      await itemApi.update(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await itemApi.create({ ...form })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(row: ItemSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除零部件「${row.code} ${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await itemApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function openDetail(row: ItemSummary): void {
  router.push({ name: 'item-detail', params: { id: row.id } })
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

.title {
  font-weight: 600;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-input {
  width: 260px;
}

.filter-select {
  width: 150px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
