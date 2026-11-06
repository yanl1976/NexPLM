<template>
  <div class="page">
    <el-row :gutter="12">
      <el-col :span="5">
        <el-card shadow="never" class="tree-card">
          <div class="tree-head">
            <span>资源分类</span>
            <div v-if="canWrite">
              <el-button link type="primary" @click="openClassDialog(null)">新增根分类</el-button>
              <el-button link type="primary" :disabled="!currentClass" @click="openClassDialog(currentClass)">
                加子类
              </el-button>
            </div>
          </div>
          <el-tree
            :data="treeData"
            node-key="id"
            :expand-on-click-node="false"
            :props="{ label: 'name', children: 'children' }"
            highlight-current
            @node-click="onClassClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.name }}</span>
                <span class="tree-count">{{ data.resourceCount }}</span>
              </span>
            </template>
          </el-tree>
          <div class="tree-foot">
            <el-button link @click="clearClassFilter">查看全部资源</el-button>
            <template v-if="currentClass && canWrite">
              <el-button link type="primary" @click="openEditClass">编辑</el-button>
              <el-button link type="danger" @click="onDeleteClass">删除</el-button>
            </template>
          </div>
        </el-card>
      </el-col>

      <el-col :span="19">
        <div class="toolbar">
          <span class="scope">{{ currentClass ? currentClass.name : '全部资源' }}</span>
          <el-input
            v-model="keyword"
            placeholder="编码 / 名称 / 规格"
            clearable
            style="width: 220px"
            @keyup.enter="reload"
            @clear="reload"
          />
          <el-select v-model="status" placeholder="状态" clearable style="width: 120px" @change="reload">
            <el-option label="在用" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button v-if="canWrite" type="primary" plain :disabled="!currentClass" @click="openResourceDialog()">
            新增资源
          </el-button>

          <el-select
            v-model="savedQueryId"
            placeholder="已保存的查询"
            clearable
            style="width: 180px; margin-left: auto"
            @change="onRunSavedQuery"
          >
            <el-option v-for="item in savedQueries" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button v-if="canWrite" @click="onSaveQuery">保存当前查询</el-button>
          <el-button v-if="canWrite && savedQueryId" type="danger" plain @click="onDeleteQuery">删除查询</el-button>
        </div>

        <div class="attr-filter">
          <span class="label">动态属性过滤</span>
          <div v-for="(item, index) in attrFilter" :key="index" class="attr-row">
            <el-input v-model="item.key" placeholder="属性名" style="width: 120px" />
            <el-input v-model="item.value" placeholder="属性值" style="width: 120px" />
            <el-button link type="danger" @click="attrFilter.splice(index, 1)">移除</el-button>
          </div>
          <el-button link type="primary" @click="attrFilter.push({ key: '', value: '' })">添加属性</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" border stripe>
          <el-table-column prop="code" label="编码" width="130" />
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="className" label="分类" width="120" />
          <el-table-column prop="spec" label="规格" min-width="140" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="70" />
          <el-table-column label="动态属性" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="(value, key) in row.attrs" :key="key" size="small" class="attr-tag">
                {{ key }}：{{ value }}
              </el-tag>
              <span v-if="!Object.keys(row.attrs || {}).length" class="sub">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '在用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="引用" width="80" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openReferences(row)">{{ row.referenceCount }}</el-button>
            </template>
          </el-table-column>
          <el-table-column v-if="canWrite" label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openResourceDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteResource(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          class="pager"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </el-col>
    </el-row>

    <!-- 分类编辑 -->
    <el-dialog v-model="classDialogVisible" :title="classForm.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="上级分类">
          <el-input :model-value="classForm.parentName || '（根分类）'" readonly />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="classForm.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="classForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="classForm.sortNo" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="classForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="classSaving" @click="onSaveClass">保存</el-button>
      </template>
    </el-dialog>

    <!-- 资源编辑 -->
    <el-dialog v-model="resourceDialogVisible" :title="resourceForm.id ? '编辑资源' : '新增资源'" width="720px">
      <el-form label-width="90px">
        <el-form-item label="所属分类" required>
          <el-select v-model="resourceForm.classId" filterable style="width: 100%">
            <el-option v-for="item in flatClasses" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="resourceForm.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="resourceForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="resourceForm.spec" maxlength="500" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="resourceForm.unit" maxlength="32" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="resourceForm.status">
            <el-radio label="ACTIVE">在用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="动态属性">
          <div class="attr-editor">
            <div v-for="(item, index) in resourceForm.attrs" :key="index" class="attr-row">
              <el-input v-model="item.key" placeholder="属性名（如 品牌）" style="width: 160px" />
              <el-input v-model="item.value" placeholder="属性值（如 山特）" style="width: 200px" />
              <el-button link type="danger" @click="resourceForm.attrs.splice(index, 1)">移除</el-button>
            </div>
            <el-button link type="primary" @click="resourceForm.attrs.push({ key: '', value: '' })">
              添加属性
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resourceForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resourceSaving" @click="onSaveResource">保存</el-button>
      </template>
    </el-dialog>

    <!-- 引用统计 -->
    <el-dialog v-model="referenceVisible" title="资源引用统计" width="720px">
      <el-table :data="references" border size="small">
        <el-table-column prop="routingCode" label="路线编码" width="130" />
        <el-table-column prop="routingName" label="路线名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="objectCode" label="零件" width="140" />
        <el-table-column prop="seqNo" label="工序号" width="80" align="center" />
        <el-table-column prop="stepName" label="工序名称" min-width="120" />
      </el-table>
      <el-empty v-if="!references.length" description="暂无工序引用该资源" />
      <template #footer>
        <el-button @click="referenceVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  resourceApi,
  type CappResource,
  type ResourceClass,
  type ResourceQuery,
  type ResourceReference
} from '@/api/capp'
import { useUserStore } from '@/stores/user'

interface TreeNode extends ResourceClass {
  children: TreeNode[]
}

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('capp.resource.write'))

const classes = ref<ResourceClass[]>([])
const currentClass = ref<ResourceClass | null>(null)
const rows = ref<CappResource[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const keyword = ref('')
const status = ref('')
const attrFilter = ref<{ key: string; value: string }[]>([])

const savedQueries = ref<ResourceQuery[]>([])
const savedQueryId = ref<string>('')

const classDialogVisible = ref(false)
const classSaving = ref(false)
const classForm = reactive<{
  id?: string
  code: string
  name: string
  parentId?: string | null
  parentName?: string
  sortNo: number
  remark: string
}>({ code: '', name: '', parentId: null, parentName: '', sortNo: 0, remark: '' })

const resourceDialogVisible = ref(false)
const resourceSaving = ref(false)
const resourceForm = reactive<{
  id?: string
  classId: string
  code: string
  name: string
  spec: string
  unit: string
  status: 'ACTIVE' | 'DISABLED'
  remark: string
  attrs: { key: string; value: string }[]
}>({ classId: '', code: '', name: '', spec: '', unit: '', status: 'ACTIVE', remark: '', attrs: [] })

const referenceVisible = ref(false)
const references = ref<ResourceReference[]>([])

const treeData = computed<TreeNode[]>(() => {
  const map = new Map<string, TreeNode>()
  classes.value.forEach((item) => map.set(item.id, { ...item, children: [] }))
  const roots: TreeNode[] = []
  classes.value.forEach((item) => {
    const node = map.get(item.id)!
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

/** 扁平分类（带缩进前缀），用于资源编辑里的分类下拉。 */
const flatClasses = computed(() => {
  const result: (ResourceClass & { name: string })[] = []
  const walk = (nodes: TreeNode[], depth: number): void => {
    nodes.forEach((node) => {
      result.push({ ...node, name: `${'　'.repeat(depth)}${node.name}` })
      walk(node.children, depth + 1)
    })
  }
  walk(treeData.value, 0)
  return result
})

onMounted(async () => {
  await loadClasses()
  await load()
  await loadQueries()
})

async function loadClasses(): Promise<void> {
  try {
    classes.value = await resourceApi.classes()
    if (currentClass.value) {
      currentClass.value = classes.value.find((item) => item.id === currentClass.value?.id) ?? null
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资源分类失败')
  }
}

async function loadQueries(): Promise<void> {
  try {
    savedQueries.value = await resourceApi.queries()
  } catch {
    savedQueries.value = []
  }
}

function buildAttrs(): Record<string, string> {
  const attrs: Record<string, string> = {}
  attrFilter.value.forEach((item) => {
    if (item.key.trim()) {
      attrs[item.key.trim()] = item.value.trim()
    }
  })
  return attrs
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const attrs = buildAttrs()
    const result = await resourceApi.list({
      page: page.value,
      size: size.value,
      classId: currentClass.value?.id,
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      attrs: Object.keys(attrs).length ? JSON.stringify(attrs) : undefined
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载资源失败')
  } finally {
    loading.value = false
  }
}

function reload(): void {
  page.value = 1
  load()
}

function onClassClick(data: TreeNode): void {
  currentClass.value = classes.value.find((item) => item.id === data.id) ?? null
  reload()
}

function clearClassFilter(): void {
  currentClass.value = null
  reload()
}

// ---- 分类维护

function openClassDialog(parent: ResourceClass | null): void {
  Object.assign(classForm, {
    id: undefined,
    code: '',
    name: '',
    parentId: parent ? parent.id : null,
    parentName: parent ? parent.name : '',
    sortNo: 0,
    remark: ''
  })
  classDialogVisible.value = true
}

function openEditClass(): void {
  if (!currentClass.value) {
    return
  }
  Object.assign(classForm, {
    id: currentClass.value.id,
    code: currentClass.value.code,
    name: currentClass.value.name,
    parentId: currentClass.value.parentId ?? null,
    parentName: currentClass.value.parentId
      ? classes.value.find((item) => item.id === currentClass.value?.parentId)?.name ?? ''
      : '',
    sortNo: currentClass.value.sortNo,
    remark: currentClass.value.remark ?? ''
  })
  classDialogVisible.value = true
}

async function onSaveClass(): Promise<void> {
  if (!classForm.code.trim() || !classForm.name.trim()) {
    ElMessage.warning('请填写分类编码与名称')
    return
  }
  classSaving.value = true
  try {
    const payload = {
      code: classForm.code.trim(),
      name: classForm.name.trim(),
      parentId: classForm.parentId ?? null,
      sortNo: classForm.sortNo,
      remark: classForm.remark || null
    }
    if (classForm.id) {
      await resourceApi.updateClass(classForm.id, payload)
    } else {
      await resourceApi.createClass(payload)
    }
    ElMessage.success('已保存')
    classDialogVisible.value = false
    await loadClasses()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    classSaving.value = false
  }
}

async function onDeleteClass(): Promise<void> {
  if (!currentClass.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除分类「${currentClass.value.name}」？`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await resourceApi.removeClass(currentClass.value.id)
    ElMessage.success('已删除')
    currentClass.value = null
    await loadClasses()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ---- 资源维护

function openResourceDialog(row?: CappResource): void {
  resourceForm.attrs = []
  if (row) {
    Object.assign(resourceForm, {
      id: row.id,
      classId: row.classId,
      code: row.code,
      name: row.name,
      spec: row.spec ?? '',
      unit: row.unit ?? '',
      status: row.status,
      remark: row.remark ?? ''
    })
    resourceForm.attrs = Object.entries(row.attrs ?? {}).map(([key, value]) => ({ key, value }))
  } else {
    Object.assign(resourceForm, {
      id: undefined,
      classId: currentClass.value?.id ?? '',
      code: '',
      name: '',
      spec: '',
      unit: '',
      status: 'ACTIVE',
      remark: ''
    })
  }
  resourceDialogVisible.value = true
}

async function onSaveResource(): Promise<void> {
  if (!resourceForm.classId) {
    ElMessage.warning('请选择所属分类')
    return
  }
  if (!resourceForm.code.trim() || !resourceForm.name.trim()) {
    ElMessage.warning('请填写资源编码与名称')
    return
  }
  const attrs: Record<string, string> = {}
  resourceForm.attrs.forEach((item) => {
    if (item.key.trim()) {
      attrs[item.key.trim()] = item.value
    }
  })
  resourceSaving.value = true
  try {
    const payload = {
      classId: resourceForm.classId,
      code: resourceForm.code.trim(),
      name: resourceForm.name.trim(),
      spec: resourceForm.spec || null,
      unit: resourceForm.unit || null,
      status: resourceForm.status,
      attrs,
      remark: resourceForm.remark || null
    }
    if (resourceForm.id) {
      await resourceApi.update(resourceForm.id, payload)
    } else {
      await resourceApi.create(payload)
    }
    ElMessage.success('已保存')
    resourceDialogVisible.value = false
    await loadClasses()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    resourceSaving.value = false
  }
}

async function onDeleteResource(row: CappResource): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除资源「${row.code} ${row.name}」？`, '删除资源', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await resourceApi.remove(row.id)
    ElMessage.success('已删除')
    await loadClasses()
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function openReferences(row: CappResource): Promise<void> {
  try {
    references.value = await resourceApi.references(row.id)
    referenceVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载引用失败')
  }
}

// ---- 保存的查询条件

async function onSaveQuery(): Promise<void> {
  let name = ''
  try {
    const result = await ElMessageBox.prompt('给当前查询条件起个名字（同名会覆盖）', '保存查询条件', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: `${currentClass.value?.name ?? '全部资源'}查询`
    })
    name = result.value ?? ''
  } catch {
    return
  }
  if (!name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  try {
    await resourceApi.saveQuery({
      name: name.trim(),
      classId: currentClass.value?.id ?? null,
      keyword: keyword.value || null,
      attrs: buildAttrs()
    })
    ElMessage.success('已保存查询条件')
    await loadQueries()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function onRunSavedQuery(id: string): Promise<void> {
  if (!id) {
    return
  }
  const query = savedQueries.value.find((item) => item.id === id)
  if (!query) {
    return
  }
  currentClass.value = classes.value.find((item) => item.id === query.classId) ?? null
  keyword.value = query.keyword ?? ''
  attrFilter.value = Object.entries(query.attrs ?? {}).map(([key, value]) => ({ key, value }))
  page.value = 1
  await load()
}

async function onDeleteQuery(): Promise<void> {
  if (!savedQueryId.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认删除该查询条件？', '删除查询条件', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await resourceApi.removeQuery(savedQueryId.value)
    savedQueryId.value = ''
    ElMessage.success('已删除')
    await loadQueries()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.tree-card {
  min-height: 520px;
}

.tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
}

.tree-node {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  padding-right: 6px;
}

.tree-count {
  color: #909399;
  font-size: 12px;
}

.tree-foot {
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.scope {
  font-weight: 600;
}

.attr-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 10px;
}

.attr-filter .label {
  color: #909399;
  font-size: 12px;
}

.attr-row {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-right: 8px;
}

.attr-editor {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.attr-tag {
  margin: 0 4px 4px 0;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.sub {
  color: #909399;
  font-size: 12px;
}
</style>
