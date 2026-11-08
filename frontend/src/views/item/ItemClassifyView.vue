<template>
  <div class="classify-page">
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-head">
          <span class="title">零部件分类</span>
          <el-button size="small" type="primary" plain @click="openCreate(null)">新建根分类</el-button>
        </div>
      </template>
      <el-tree
        :data="tree"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        @node-click="onNodeClick"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="node-label">
              {{ data.name }}
              <span class="node-count">{{ data.docCount }}</span>
            </span>
            <span class="node-actions">
              <el-button link size="small" @click.stop="openCreate(data)">新增子类</el-button>
              <el-button link size="small" @click.stop="openAttr(data)">属性</el-button>
              <el-button link size="small" @click.stop="openEdit(data)">编辑</el-button>
              <el-button link size="small" type="danger" @click.stop="onDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
      <div v-if="!tree.length" class="empty-tip">暂无分类，点「新建根分类」开始维护</div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-head">
          <span class="title">
            {{ currentClass ? `分类：${currentClass.name}` : '全部分类' }}
            <span class="path">{{ currentClass?.path ?? '' }}</span>
          </span>
          <div class="head-actions">
            <el-button size="small" :disabled="!currentClass" @click="openAssign">归类零件</el-button>
            <el-button size="small" @click="onReset">重置条件</el-button>
            <el-button size="small" type="primary" :loading="loading" @click="onSearch">查询</el-button>
          </div>
        </div>
      </template>

      <!-- 检索条件：对齐蓝本 ClassifyUse（分类树 + 名称/编码 + 属性条件行 + 含子分类） -->
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称 / 规格"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        />
        <el-checkbox v-model="query.includeChildren">含子分类</el-checkbox>
        <template v-for="def in filterDefs" :key="def.code">
          <el-select
            v-if="def.valueType === 'ENUM' && def.enumCode"
            v-model="attrFilters[def.code]"
            :placeholder="def.name"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in enumOptions(def.enumCode)"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
          <el-select
            v-else-if="def.valueType === 'BOOL'"
            v-model="attrFilters[def.code]"
            :placeholder="def.name"
            clearable
            style="width: 120px"
          >
            <el-option label="是" value="true" />
            <el-option label="否" value="false" />
          </el-select>
          <el-input
            v-else
            v-model="attrFilters[def.code]"
            :placeholder="def.name"
            clearable
            style="width: 150px"
            @keyup.enter="onSearch"
          />
        </template>
      </div>

      <el-table :data="rows" border size="small" v-loading="loading" @row-dblclick="openItem">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格型号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="itemType" label="类型" width="110" />
        <el-table-column prop="revisionLabel" label="版本" width="90" />
        <el-table-column prop="lifecycleState" label="生命周期" width="110" />
        <el-table-column prop="usingState" label="使用状态" width="110" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click="openItem(row)">详情</el-button>
            <el-button link size="small" type="danger" @click="onUnassign(row)">取消归类</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="loadRows"
        @size-change="onSearch"
      />
    </el-card>

    <!-- 分类维护 -->
    <el-dialog v-model="classDialog" :title="editId ? '编辑分类' : '新建分类'" width="520px">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="90px">
        <el-form-item label="上级分类">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="classForm.code" placeholder="字母开头，如 MECH-STD" />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="classForm.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="classForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="classForm.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联编码">
          <el-select
            v-model="classForm.codeRuleCode"
            clearable
            placeholder="按编码自动归类的规则（可空）"
            style="width: 100%"
          >
            <el-option
              v-for="rule in codeRules"
              :key="rule.code"
              :label="`${rule.name}（${rule.code}）`"
              :value="rule.code"
            />
          </el-select>
          <div class="attr-hint">零件编码匹配到该规则时，归类弹窗会自动带出本分类</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="classForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类属性 -->
    <el-dialog v-model="attrDialog" :title="`分类属性：${attrClass?.name ?? ''}`" width="820px">
      <div class="attr-hint">
        属性值类型对齐蓝本（文本/数值/日期/枚举/布尔）；勾选「可继承」后子分类可见可填。
        <span v-if="inheritedAttrs.length">继承自上级的属性只读展示（{{ inheritedAttrs.length }} 个）。</span>
      </div>
      <el-table :data="attrRows" border size="small">
        <el-table-column label="属性编码" width="150">
          <template #default="{ row }">
            <el-input v-model="row.code" :disabled="!row.local" placeholder="字母开头" />
          </template>
        </el-table-column>
        <el-table-column label="属性名称" width="150">
          <template #default="{ row }">
            <el-input v-model="row.name" :disabled="!row.local" />
          </template>
        </el-table-column>
        <el-table-column label="值类型" width="140">
          <template #default="{ row }">
            <el-select v-model="row.valueType" :disabled="!row.local" style="width: 100%">
              <el-option label="文本" value="STRING" />
              <el-option label="数值" value="NUMBER" />
              <el-option label="日期" value="DATE" />
              <el-option label="枚举" value="ENUM" />
              <el-option label="布尔" value="BOOL" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="枚举编码" width="150">
          <template #default="{ row }">
            <el-input v-model="row.enumCode" :disabled="!row.local || row.valueType !== 'ENUM'" />
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.required" :disabled="!row.local" />
          </template>
        </el-table-column>
        <el-table-column label="可继承" width="80" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.inheritable" :disabled="!row.local" />
          </template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag v-if="!row.local" size="small" type="info">继承</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index, row }">
            <el-button v-if="row.local" link size="small" type="danger" @click="removeAttrRow($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" plain class="attr-add" @click="addAttrRow">新增属性</el-button>
      <template #footer>
        <el-button @click="attrDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveAttrs">保存</el-button>
      </template>
    </el-dialog>

    <!-- 归类零件 -->
    <el-dialog v-model="assignDialog" :title="`归类到：${currentClass?.name ?? ''}`" width="620px">
      <el-form label-width="90px">
        <el-form-item label="选择零件" required>
          <el-select
            v-model="assignItemId"
            filterable
            remote
            clearable
            :remote-method="searchItems"
            :loading="itemSearching"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in itemOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-for="def in assignDefs" :key="def.code" :label="def.name" :required="def.required">
          <el-select
            v-if="def.valueType === 'ENUM' && def.enumCode"
            v-model="assignAttrs[def.code]"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in enumOptions(def.enumCode)"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
          <el-select v-else-if="def.valueType === 'BOOL'" v-model="assignAttrs[def.code]" clearable style="width: 100%">
            <el-option label="是" value="true" />
            <el-option label="否" value="false" />
          </el-select>
          <el-date-picker
            v-else-if="def.valueType === 'DATE'"
            v-model="assignAttrs[def.code]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <el-input v-else v-model="assignAttrs[def.code]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onAssign">确定归类</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { codingApi, type CodeRule } from '@/api/coding'
import {
  itemApi,
  itemClassApi,
  type ItemClassAttrDef,
  type ItemClassNode,
  type ItemClassSaveReq,
  type ItemSummary
} from '@/api/item'
import { metaApi } from '@/api/system'

const router = useRouter()

const tree = ref<ItemClassNode[]>([])
const currentClass = ref<ItemClassNode | null>(null)

const rows = ref<ItemSummary[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
/** 编码规则（只列 PdmItem 类型，供分类的「关联编码」选择）。 */
const codeRules = ref<CodeRule[]>([])

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  includeChildren: true
})
const attrFilters = reactive<Record<string, string>>({})

/** 当前分类的生效属性（自身 + 祖先可继承），用于检索条件下拉与归类表单。 */
const effectiveAttrs = ref<ItemClassAttrDef[]>([])

/** 检索条件下的属性项（只列可填写的生效属性，限制 6 个避免条件行过长）。 */
const filterDefs = computed(() => effectiveAttrs.value.slice(0, 6))
const assignDefs = computed(() => effectiveAttrs.value)

/** 枚举缓存：enumCode → items（蓝本按属性+分类覆盖枚举的简化处理，见 GAP）。 */
const enumCache = reactive<Record<string, { code: string; name: string }[]>>({})

function enumOptions(enumCode?: string | null): { code: string; name: string }[] {
  if (!enumCode) {
    return []
  }
  if (!enumCache[enumCode]) {
    enumCache[enumCode] = []
    metaApi
      .getEnum(enumCode)
      .then((detail) => {
        enumCache[enumCode] = detail.items.map((item) => ({ code: item.code, name: item.name }))
      })
      .catch(() => {
        enumCache[enumCode] = []
      })
  }
  return enumCache[enumCode]
}

async function loadTree(): Promise<void> {
  try {
    tree.value = await itemClassApi.tree()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类树失败')
  }
}

async function loadAttrs(classId?: string | null): Promise<void> {
  if (!classId) {
    effectiveAttrs.value = []
    Object.keys(attrFilters).forEach((key) => delete attrFilters[key])
    return
  }
  try {
    effectiveAttrs.value = await itemClassApi.attrs(classId)
    Object.keys(attrFilters).forEach((key) => {
      if (!effectiveAttrs.value.some((def) => def.code === key)) {
        delete attrFilters[key]
      }
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类属性失败')
  }
}

async function loadRows(): Promise<void> {
  loading.value = true
  try {
    const attrs: Record<string, string> = {}
    Object.entries(attrFilters).forEach(([code, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        attrs[code] = value
      }
    })
    const page = await itemApi.list({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      classId: currentClass.value?.id,
      includeChildren: currentClass.value ? query.includeChildren : undefined,
      classAttrs: attrs
    })
    rows.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载零件失败')
  } finally {
    loading.value = false
  }
}

function onNodeClick(node: ItemClassNode): void {
  currentClass.value = node
  query.page = 1
  loadAttrs(node.id)
  loadRows()
}

function onSearch(): void {
  query.page = 1
  loadRows()
}

function onReset(): void {
  query.keyword = ''
  query.includeChildren = true
  Object.keys(attrFilters).forEach((key) => delete attrFilters[key])
  query.page = 1
  loadRows()
}

// ------------------------------------------------------------ 分类维护

const classDialog = ref(false)
const classFormRef = ref<FormInstance>()
const editId = ref<string | null>(null)
const parentNode = ref<ItemClassNode | null>(null)
const classForm = reactive<ItemClassSaveReq>({
  code: '',
  name: '',
  parentId: null,
  sort: 0,
  status: 'ACTIVE',
  remark: '',
  codeRuleCode: ''
})

/** 关联编码候选：只取 PdmItem 类型的规则（后端还会校验对象类型与唯一性）。 */
async function loadCodeRules(): Promise<void> {
  try {
    const rules = await codingApi.rules()
    codeRules.value = rules.filter((rule) => !rule.objectType || rule.objectType === 'PdmItem')
  } catch {
    codeRules.value = []
  }
}
const classRules: FormRules = {
  code: [
    { required: true, message: '分类编码不能为空', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_-]{0,63}$/, message: '字母开头，仅含字母数字下划线或短横线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
}

const parentLabel = computed(() => {
  if (parentNode.value) {
    return `${parentNode.value.name}（${parentNode.value.path}）`
  }
  return '（根分类）'
})

function openCreate(parent: ItemClassNode | null): void {
  editId.value = null
  parentNode.value = parent
  Object.assign(classForm, {
    code: '',
    name: '',
    parentId: parent?.id ?? null,
    sort: 0,
    status: 'ACTIVE',
    remark: '',
    codeRuleCode: ''
  })
  classDialog.value = true
}

function openEdit(node: ItemClassNode): void {
  editId.value = node.id
  parentNode.value = null
  Object.assign(classForm, {
    code: node.code,
    name: node.name,
    parentId: node.parentId ?? null,
    sort: node.sort ?? 0,
    status: node.status ?? 'ACTIVE',
    remark: node.remark ?? '',
    codeRuleCode: node.codeRuleCode ?? ''
  })
  classDialog.value = true
}

async function onSaveClass(): Promise<void> {
  const valid = await classFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (editId.value) {
      await itemClassApi.update(editId.value, { ...classForm })
    } else {
      await itemClassApi.create({ ...classForm })
    }
    ElMessage.success('已保存')
    classDialog.value = false
    await loadTree()
    await loadRows()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(node: ItemClassNode): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除分类「${node.name}」？分类下有零件或子分类时会被拒绝。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await itemClassApi.remove(node.id)
    ElMessage.success('已删除')
    if (currentClass.value?.id === node.id) {
      currentClass.value = null
      effectiveAttrs.value = []
    }
    await loadTree()
    await loadRows()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ------------------------------------------------------------ 分类属性

interface AttrRow {
  local: boolean
  code: string
  name: string
  valueType: string
  enumCode?: string | null
  required: boolean
  inheritable: boolean
  sort: number
}

const attrDialog = ref(false)
const attrClass = ref<ItemClassNode | null>(null)
const attrRows = ref<AttrRow[]>([])
const inheritedAttrs = computed(() => attrRows.value.filter((row) => !row.local))

async function openAttr(node: ItemClassNode): Promise<void> {
  attrClass.value = node
  try {
    const defs = await itemClassApi.attrs(node.id)
    attrRows.value = defs.map((def) => ({
      local: def.ownerClassId === node.id,
      code: def.code,
      name: def.name,
      valueType: def.valueType,
      enumCode: def.enumCode ?? '',
      required: Boolean(def.required),
      inheritable: Boolean(def.inheritable),
      sort: def.sort ?? 0
    }))
    attrDialog.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载属性失败')
  }
}

function addAttrRow(): void {
  attrRows.value.push({
    local: true,
    code: '',
    name: '',
    valueType: 'STRING',
    enumCode: '',
    required: false,
    inheritable: true,
    sort: attrRows.value.length * 10
  })
}

function removeAttrRow(index: number): void {
  attrRows.value.splice(index, 1)
}

async function onSaveAttrs(): Promise<void> {
  if (!attrClass.value) {
    return
  }
  // 继承属性只读，提交时只发本分类自身的属性（否则会把继承属性复制成本地属性）
  const items = attrRows.value
    .filter((row) => row.local)
    .map((row, index) => ({
      code: row.code.trim(),
      name: row.name.trim(),
      valueType: row.valueType,
      enumCode: row.valueType === 'ENUM' ? row.enumCode?.trim() || null : null,
      required: row.required,
      inheritable: row.inheritable,
      sort: (index + 1) * 10
    }))
  if (items.some((item) => !item.code || !item.name)) {
    ElMessage.warning('属性编码与名称不能为空')
    return
  }
  saving.value = true
  try {
    await itemClassApi.saveAttrs(attrClass.value.id, items)
    ElMessage.success('属性已保存')
    attrDialog.value = false
    await loadAttrs(currentClass.value?.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存属性失败')
  } finally {
    saving.value = false
  }
}

// ------------------------------------------------------------ 归类

const assignDialog = ref(false)
const assignItemId = ref<string | null>(null)
const itemOptions = ref<ItemSummary[]>([])
const itemSearching = ref(false)
const assignAttrs = reactive<Record<string, string>>({})

function openAssign(): void {
  if (!currentClass.value) {
    ElMessage.warning('请先选择分类')
    return
  }
  assignItemId.value = null
  Object.keys(assignAttrs).forEach((key) => delete assignAttrs[key])
  itemOptions.value = []
  assignDialog.value = true
}

async function searchItems(keyword: string): Promise<void> {
  if (!keyword) {
    itemOptions.value = []
    return
  }
  itemSearching.value = true
  try {
    const page = await itemApi.list({ page: 1, size: 20, keyword })
    itemOptions.value = page.items
  } catch {
    itemOptions.value = []
  } finally {
    itemSearching.value = false
  }
}

async function onAssign(): Promise<void> {
  if (!assignItemId.value || !currentClass.value) {
    ElMessage.warning('请选择零件')
    return
  }
  const attrs: Record<string, string> = {}
  Object.entries(assignAttrs).forEach(([code, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      attrs[code] = value
    }
  })
  saving.value = true
  try {
    await itemClassApi.assign(assignItemId.value, currentClass.value.id, attrs)
    ElMessage.success('已归类')
    assignDialog.value = false
    await loadTree()
    await loadRows()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '归类失败')
  } finally {
    saving.value = false
  }
}

async function onUnassign(row: ItemSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`取消「${row.code} ${row.name}」的分类？分类属性值会一并清空。`, '取消归类', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await itemClassApi.assign(row.id, null, {})
    ElMessage.success('已取消归类')
    await loadTree()
    await loadRows()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

function openItem(row: ItemSummary): void {
  router.push(`/item/${row.id}`)
}

onMounted(async () => {
  await Promise.all([loadTree(), loadRows(), loadCodeRules()])
})
</script>

<style scoped>
.classify-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.tree-card {
  width: 340px;
  flex: 0 0 340px;
}

.list-card {
  flex: 1;
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.title {
  font-weight: 600;
}

.path {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 6px;
}

.node-count {
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 8px;
  background: #f0f2f5;
  color: #909399;
  font-size: 11px;
}

.node-actions {
  display: none;
}

.tree-node:hover .node-actions {
  display: inline-flex;
  gap: 2px;
}

.empty-tip {
  padding: 12px 0;
  color: #909399;
  font-size: 12px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

.pager {
  margin-top: 10px;
  justify-content: flex-end;
}

.attr-hint {
  margin-bottom: 8px;
  color: #909399;
  font-size: 12px;
}

.attr-add {
  margin-top: 8px;
}
</style>
