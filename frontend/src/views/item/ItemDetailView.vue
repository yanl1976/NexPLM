<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="head-left">
            <el-button link @click="goBack">← 返回</el-button>
            <span class="code">{{ detail?.code }}</span>
            <span class="name">{{ detail?.name }}</span>
            <el-tag size="small" type="info">{{ detail?.revisionLabel }}</el-tag>
            <el-tag size="small" :type="stateTag(detail?.lifecycleState)">{{ stateLabel(detail?.lifecycleState) }}</el-tag>
            <el-tag v-if="detail" size="small" :type="detail.sourceType === 'BUY' ? 'warning' : 'success'">
              {{ ITEM_SOURCE_LABEL[detail.sourceType] ?? detail.sourceType }}
            </el-tag>
          </div>
          <div>
            <el-button v-if="canWrite" @click="openEdit">编辑属性</el-button>
          </div>
        </div>
      </template>

      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ ITEM_TYPE_LABEL[detail.itemType] ?? detail.itemType }}
        </el-descriptions-item>
        <el-descriptions-item label="供应方式">
          {{ ITEM_SOURCE_LABEL[detail.sourceType] ?? detail.sourceType }}
        </el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ detail.specification || '-' }}</el-descriptions-item>
        <el-descriptions-item label="材料">{{ detail.material || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ detail.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单件重量">{{ detail.weight ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="标准号">{{ detail.stdCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.revisionLabel }}</el-descriptions-item>
        <el-descriptions-item label="生命周期">{{ stateLabel(detail.lifecycleState) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="BOM" name="bom">
          <div class="bom-toolbar">
            <el-radio-group v-model="bomView" @change="loadBom">
              <el-radio-button v-for="(label, value) in BOM_VIEW_LABEL" :key="value" :value="value">
                {{ label }}
              </el-radio-button>
            </el-radio-group>
            <el-button type="primary" :disabled="!canWrite" @click="openAddBom">添加子件</el-button>
            <el-button @click="checkBom">结构校验</el-button>
            <span class="hint">共 {{ bomRows.length }} 个顶层子件，展开可查看下级</span>
          </div>

          <el-alert
            v-if="bomIssues.length > 0"
            type="warning"
            :closable="false"
            show-icon
            class="bom-issues"
          >
            <template #title>结构校验发现 {{ bomIssues.length }} 个问题</template>
            <div v-for="(issue, index) in bomIssues" :key="index" class="issue-line">
              [{{ BOM_ISSUE_LABEL[issue.type] ?? issue.type }}] {{ issue.message }}
            </div>
          </el-alert>

          <el-table
            v-loading="bomLoading"
            :data="bomRows"
            row-key="lineId"
            :tree-props="{ children: 'children' }"
            border
            default-expand-all
          >
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
            <el-table-column prop="qty" label="用量" width="100" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column label="损耗率" width="90">
              <template #default="{ row }">{{ row.lossRate ?? 0 }}%</template>
            </el-table-column>
            <el-table-column prop="refDesignator" label="位号" width="120" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="!canWrite" @click="openEditBom(row)">编辑</el-button>
                <el-button link type="danger" :disabled="!canWrite" @click="onDeleteBom(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无子件，点击「添加子件」建立 BOM" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="被引用" name="whereused">
          <el-table v-loading="whereUsedLoading" :data="whereUsedRows" border>
            <el-table-column prop="parentCode" label="父件编码" width="150" />
            <el-table-column prop="parentName" label="父件名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="视图" width="100">
              <template #default="{ row }">{{ BOM_VIEW_LABEL[row.viewType] ?? row.viewType }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="用量" width="100" />
            <el-table-column prop="lineNo" label="序号" width="90" />
            <template #empty>
              <el-empty description="暂无父件引用该零部件" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog
      v-model="bomDialogVisible"
      :title="bomEditingLineId ? '编辑 BOM 行' : '添加子件'"
      width="560px"
    >
      <el-form :model="bomForm" label-width="90px">
        <el-form-item v-if="!bomEditingLineId" label="子件" required>
          <el-select
            v-model="bomForm.childId"
            filterable
            remote
            :remote-method="searchChildren"
            :loading="childLoading"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in childOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用量" required>
          <el-input-number v-model="bomForm.qty" :min="0.000001" :precision="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="bomForm.unit" placeholder="留空取子件单位" />
        </el-form-item>
        <el-form-item label="损耗率(%)">
          <el-input-number v-model="bomForm.lossRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="位号">
          <el-input v-model="bomForm.refDesignator" placeholder="如 P1,P2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bomForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bomSaving" @click="onSaveBom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="编辑零部件" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
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
          <el-input v-model="form.specification" />
        </el-form-item>
        <el-form-item label="材料">
          <el-input v-model="form.material" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="单件重量">
          <el-input-number v-model="form.weight" :min="0" :precision="4" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标准号">
          <el-input v-model="form.stdCode" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  BOM_ISSUE_LABEL,
  BOM_VIEW_LABEL,
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  bomApi,
  itemApi,
  type BomIssue,
  type BomLineReq,
  type BomNode,
  type ItemDetail,
  type ItemSaveReq,
  type ItemSummary,
  type WhereUsedRow
} from '@/api/item'
import { DOC_STATE_LABEL } from '@/api/doc'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const itemId = computed(() => String(route.params.id))
const detail = ref<ItemDetail | null>(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('bom')

const bomView = ref('DESIGN')
const bomRows = ref<BomNode[]>([])
const bomLoading = ref(false)
const bomIssues = ref<BomIssue[]>([])
const whereUsedRows = ref<WhereUsedRow[]>([])
const whereUsedLoading = ref(false)
const bomDialogVisible = ref(false)
const bomEditingLineId = ref<string | null>(null)
const bomSaving = ref(false)
const childOptions = ref<ItemSummary[]>([])
const childLoading = ref(false)
const bomForm = reactive<BomLineReq>({
  childId: '',
  qty: 1,
  unit: '',
  lossRate: 0,
  refDesignator: '',
  remark: ''
})

const canWrite = computed(() => userStore.can('pse.item.write'))

const form = reactive<ItemSaveReq>({
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

function stateTag(state?: string | null): 'success' | 'info' | 'warning' | 'danger' {
  switch (state) {
    case 'Released':
      return 'success'
    case 'InWorkflow':
      return 'warning'
    case 'Obsolete':
      return 'danger'
    default:
      return 'info'
  }
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    detail.value = await itemApi.detail(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载零部件失败')
  } finally {
    loading.value = false
  }
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.name = detail.value.name
  form.itemType = detail.value.itemType
  form.sourceType = detail.value.sourceType
  form.specification = detail.value.specification ?? ''
  form.material = detail.value.material ?? ''
  form.unit = detail.value.unit
  form.weight = detail.value.weight ?? null
  form.stdCode = detail.value.stdCode ?? ''
  form.remark = detail.value.remark ?? ''
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
    detail.value = await itemApi.update(itemId.value, { ...form })
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function loadBom(): Promise<void> {
  bomLoading.value = true
  try {
    bomRows.value = await bomApi.tree(itemId.value, bomView.value, 6)
    bomIssues.value = []
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 BOM 失败')
  } finally {
    bomLoading.value = false
  }
}

async function loadWhereUsed(): Promise<void> {
  whereUsedLoading.value = true
  try {
    whereUsedRows.value = await bomApi.whereUsed(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载被引用失败')
  } finally {
    whereUsedLoading.value = false
  }
}

function onTabChange(name: string): void {
  if (name === 'bom') {
    loadBom()
  } else if (name === 'whereused') {
    loadWhereUsed()
  }
}

async function searchChildren(keyword: string): Promise<void> {
  childLoading.value = true
  try {
    const page = await itemApi.list({ page: 1, size: 20, keyword: keyword || undefined })
    childOptions.value = page.items.filter((item) => item.id !== itemId.value)
  } catch {
    childOptions.value = []
  } finally {
    childLoading.value = false
  }
}

function openAddBom(): void {
  bomEditingLineId.value = null
  bomForm.childId = ''
  bomForm.qty = 1
  bomForm.unit = ''
  bomForm.lossRate = 0
  bomForm.refDesignator = ''
  bomForm.remark = ''
  childOptions.value = []
  bomDialogVisible.value = true
}

function openEditBom(row: BomNode): void {
  bomEditingLineId.value = row.lineId
  bomForm.childId = row.childId
  bomForm.qty = Number(row.qty)
  bomForm.unit = row.unit ?? ''
  bomForm.lossRate = row.lossRate ?? 0
  bomForm.refDesignator = row.refDesignator ?? ''
  bomForm.remark = row.remark ?? ''
  bomDialogVisible.value = true
}

async function onSaveBom(): Promise<void> {
  if (!bomEditingLineId.value && !bomForm.childId) {
    ElMessage.warning('请选择子件')
    return
  }
  if (!bomForm.qty || Number(bomForm.qty) <= 0) {
    ElMessage.warning('用量必须大于 0')
    return
  }
  bomSaving.value = true
  try {
    if (bomEditingLineId.value) {
      await bomApi.update(itemId.value, bomEditingLineId.value, { ...bomForm })
    } else {
      await bomApi.add(itemId.value, bomView.value, { ...bomForm })
    }
    ElMessage.success('已保存')
    bomDialogVisible.value = false
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存 BOM 失败')
  } finally {
    bomSaving.value = false
  }
}

async function onDeleteBom(row: BomNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认从 BOM 中移除「${row.code} ${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await bomApi.remove(itemId.value, row.lineId)
    ElMessage.success('已移除')
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function checkBom(): Promise<void> {
  try {
    const result = await bomApi.validate(itemId.value, bomView.value)
    bomIssues.value = result.issues
    if (result.issues.length === 0) {
      ElMessage.success('结构校验通过：无用量/单位/孤儿件/循环引用问题')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结构校验失败')
  }
}

function goBack(): void {
  router.push({ name: 'item-list' })
}

onMounted(async () => {
  await load()
  await loadBom()
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

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.code {
  color: #909399;
}

.name {
  font-weight: 600;
}

.bom-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.bom-issues {
  margin-bottom: 12px;
}

.issue-line {
  font-size: 12px;
  line-height: 1.6;
}
</style>
