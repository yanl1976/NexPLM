<template>
  <div class="page">
    <div class="toolbar">
      <el-select
        v-model="currentId"
        placeholder="选择看板"
        clearable
        style="width: 240px"
        @change="onSelect"
      >
        <el-option v-for="row in boards" :key="row.id" :label="`${row.name}（${row.code}）`" :value="row.id" />
      </el-select>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建看板</el-button>
      <template v-if="board && canWrite">
        <el-button v-if="!editing" @click="startEdit">编辑布局</el-button>
        <template v-else>
          <el-button type="primary" :loading="saving" @click="saveBoard">保存布局</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </template>
        <el-button link type="danger" @click="removeBoard">删除看板</el-button>
      </template>
      <span class="hint">
        数据集是服务端白名单（不支持自定义 SQL）；组件可拖动排序，宽度按 3 列栅格
      </span>
    </div>

    <el-alert
      v-if="!canRead"
      type="warning"
      :closable="false"
      show-icon
      title="当前账号没有「自定义看板查看」权限（ana.board.read）"
    />

    <el-empty
      v-else-if="!loading && !board"
      :description="canWrite ? '还没有自定义看板，点「新建看板」开始' : '还没有自定义看板'"
      :image-size="70"
    />

    <template v-else-if="board">
      <div class="board-head">
        <div>
          <span class="board-name">{{ board.name }}</span>
          <el-tag v-if="board.shared" size="small" type="success" class="tag-gap">公司共享</el-tag>
          <el-tag size="small" type="info" class="tag-gap">{{ board.code }}</el-tag>
        </div>
        <span class="hint">{{ board.description || '暂无描述' }}</span>
      </div>

      <div v-loading="loadingData" class="board-grid">
        <div
          v-for="(item, index) in renderComponents"
          :key="item.id"
          class="board-cell"
          :style="{ gridColumn: `span ${item.span}` }"
        >
          <div
            class="cell-head"
            :draggable="editing"
            @dragstart="onDragStart(index)"
            @dragover.prevent
            @drop="onDrop(index)"
          >
            <span class="cell-title">
              <span v-if="editing" class="drag-handle" title="拖动排序">⋮⋮</span>
              {{ item.title }}
            </span>
            <span class="cell-meta">{{ datasetLabel(item.dataset) }} · {{ BOARD_COMPONENT_LABEL[item.type] }}</span>
          </div>

          <template v-if="editing">
            <div class="cell-form">
              <el-select v-model="item.type" size="small" style="width: 110px">
                <el-option
                  v-for="(label, value) in BOARD_COMPONENT_LABEL"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
              <el-select v-model="item.dataset" size="small" style="width: 190px" filterable @change="onDatasetChange(item)">
                <el-option-group v-for="group in datasetGroups" :key="group.domain" :label="group.label">
                  <el-option
                    v-for="option in group.options"
                    :key="option.code"
                    :label="option.label"
                    :value="option.code"
                  />
                </el-option-group>
              </el-select>
              <el-input v-model="item.title" size="small" style="width: 150px" placeholder="标题" />
              <el-select v-model="item.span" size="small" style="width: 90px">
                <el-option label="1 列" :value="1" />
                <el-option label="2 列" :value="2" />
                <el-option label="3 列" :value="3" />
              </el-select>
              <el-input-number v-model="item.top" size="small" :min="1" :max="100" style="width: 110px" />
              <div class="cell-actions">
                <el-button link :disabled="index === 0" @click="move(index, -1)">上移</el-button>
                <el-button link :disabled="index === formComponents.length - 1" @click="move(index, 1)">下移</el-button>
                <el-button link type="danger" @click="removeComponent(index)">删除</el-button>
              </div>
            </div>
          </template>

          <template v-else>
            <div v-if="item.type === 'metric'" class="metric-body">
              <span class="metric-value">{{ formatNumber(dataOf(item.id)?.total ?? 0) }}</span>
              <span class="metric-unit">合计</span>
            </div>
            <div v-else-if="item.type === 'bar'" class="bar-body">
              <div v-for="row in rowsOf(item.id)" :key="row.key" class="bar-row">
                <span class="bar-label">{{ keyLabel(row.key) }}</span>
                <span class="bar-track">
                  <span class="bar-fill" :style="{ width: barWidth(row.count, item.id) }" />
                </span>
                <span class="bar-count">{{ row.count }}</span>
              </div>
              <el-empty v-if="!rowsOf(item.id).length" description="暂无数据" :image-size="40" />
            </div>
            <el-table v-else :data="rowsOf(item.id)" size="small" border>
              <el-table-column label="分组" min-width="140">
                <template #default="{ row }">{{ keyLabel(row.key) }}</template>
              </el-table-column>
              <el-table-column prop="count" label="数量" width="90" align="right" />
            </el-table>
          </template>
        </div>
      </div>

      <el-button v-if="editing" class="add-btn" plain @click="addComponent">+ 添加组件</el-button>
    </template>

    <el-dialog v-model="createVisible" title="新建看板" width="520px">
      <el-form label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="createForm.code" maxlength="64" placeholder="BOARD-001" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="公司共享">
          <el-switch v-model="createForm.shared" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createBoard">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  BOARD_COMPONENT_LABEL,
  boardApi,
  type BoardComponent,
  type BoardDetail,
  type BoardRow,
  type ComponentData,
  type DatasetMeta
} from '@/api/board'
import { DASHBOARD_DOMAIN_LABEL, DASHBOARD_STATUS_LABEL } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canRead = userStore.can('ana.board.read')
const canWrite = userStore.can('ana.board.write')

const boards = ref<BoardRow[]>([])
const currentId = ref('')
const board = ref<BoardDetail | null>(null)
const formComponents = ref<BoardComponent[]>([])
const datasets = ref<DatasetMeta[]>([])
const dataMap = ref<Record<string, ComponentData>>({})
const loading = ref(false)
const loadingData = ref(false)
const saving = ref(false)
const editing = ref(false)

const createVisible = ref(false)
const createForm = reactive({ code: '', name: '', description: '', shared: false })

const renderComponents = computed<BoardComponent[]>(() =>
  editing.value ? formComponents.value : board.value?.components ?? []
)

/** 数据集按域分组，下拉更短；域顺序沿用后端返回顺序。 */
const datasetGroups = computed(() => {
  const groups: Array<{ domain: string; label: string; options: DatasetMeta[] }> = []
  for (const meta of datasets.value) {
    let group = groups.find((item) => item.domain === meta.domain)
    if (!group) {
      group = { domain: meta.domain, label: DASHBOARD_DOMAIN_LABEL[meta.domain] ?? meta.domain, options: [] }
      groups.push(group)
    }
    group.options.push(meta)
  }
  return groups
})

async function loadBoards(): Promise<void> {
  if (!canRead) {
    return
  }
  loading.value = true
  try {
    const result = await boardApi.page({ page: 1, size: 100 })
    boards.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载看板列表失败')
  } finally {
    loading.value = false
  }
}

async function onSelect(): Promise<void> {
  editing.value = false
  if (!currentId.value) {
    board.value = null
    dataMap.value = {}
    return
  }
  await loadBoard(currentId.value)
  await loadData(currentId.value)
}

async function loadBoard(id: string): Promise<void> {
  try {
    board.value = await boardApi.detail(id)
    formComponents.value = board.value.components.map((item) => ({ ...item }))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载看板失败')
  }
}

async function loadData(id: string): Promise<void> {
  loadingData.value = true
  try {
    const data = await boardApi.data(id)
    const map: Record<string, ComponentData> = {}
    for (const item of data.components) {
      map[item.componentId] = item
    }
    dataMap.value = map
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载看板数据失败')
  } finally {
    loadingData.value = false
  }
}

function startEdit(): void {
  if (!board.value) {
    return
  }
  formComponents.value = board.value.components.map((item) => ({ ...item }))
  editing.value = true
}

function cancelEdit(): void {
  editing.value = false
  if (board.value) {
    formComponents.value = board.value.components.map((item) => ({ ...item }))
  }
}

async function saveBoard(): Promise<void> {
  if (!board.value) {
    return
  }
  if (!formComponents.value.length) {
    ElMessage.warning('看板至少需要一个组件')
    return
  }
  saving.value = true
  try {
    const saved = await boardApi.update(board.value.id, {
      code: board.value.code,
      name: board.value.name,
      description: board.value.description ?? null,
      shared: board.value.shared,
      components: formComponents.value.map((item) => ({
        id: item.id,
        type: item.type,
        title: item.title,
        dataset: item.dataset,
        top: item.top,
        span: item.span
      }))
    })
    board.value = saved
    formComponents.value = saved.components.map((item) => ({ ...item }))
    editing.value = false
    ElMessage.success('布局已保存')
    await loadData(saved.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function openCreate(): void {
  Object.assign(createForm, { code: '', name: '', description: '', shared: false })
  createVisible.value = true
}

async function createBoard(): Promise<void> {
  if (!createForm.code.trim() || !createForm.name.trim()) {
    ElMessage.warning('编码与名称为必填')
    return
  }
  saving.value = true
  try {
    // 新看板先给一个默认组件：后端要求至少一个，也省得用户面对空白
    const created = await boardApi.create({
      code: createForm.code.trim(),
      name: createForm.name.trim(),
      description: createForm.description.trim() || null,
      shared: createForm.shared,
      components: [
        {
          type: 'bar',
          dataset: 'doc.byState',
          title: '文档按状态',
          span: 2,
          top: 10
        }
      ]
    })
    createVisible.value = false
    ElMessage.success('看板已创建')
    await loadBoards()
    currentId.value = created.id
    await loadBoard(created.id)
    await loadData(created.id)
    editing.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    saving.value = false
  }
}

async function removeBoard(): Promise<void> {
  if (!board.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`删除看板「${board.value.name}」？`, '删除看板', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await boardApi.remove(board.value.id)
    ElMessage.success('已删除')
    board.value = null
    currentId.value = ''
    dataMap.value = {}
    await loadBoards()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function addComponent(): void {
  const first = datasets.value[0]
  formComponents.value.push({
    id: '',
    type: 'bar',
    title: first?.label ?? '新组件',
    dataset: first?.code ?? '',
    top: 10,
    span: 1,
    order: formComponents.value.length
  })
}

function removeComponent(index: number): void {
  formComponents.value.splice(index, 1)
}

function move(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= formComponents.value.length) {
    return
  }
  const items = formComponents.value
  const [moved] = items.splice(index, 1)
  items.splice(target, 0, moved)
}

let dragIndex = -1

function onDragStart(index: number): void {
  dragIndex = index
}

function onDrop(index: number): void {
  if (dragIndex < 0 || dragIndex === index) {
    return
  }
  const items = formComponents.value
  const [moved] = items.splice(dragIndex, 1)
  items.splice(index, 0, moved)
  dragIndex = -1
}

function onDatasetChange(item: BoardComponent): void {
  const meta = datasets.value.find((option) => option.code === item.dataset)
  if (meta) {
    item.title = meta.label
  }
}

function dataOf(componentId: string): ComponentData | undefined {
  return dataMap.value[componentId]
}

function rowsOf(componentId: string): Array<{ key: string; count: number }> {
  return dataMap.value[componentId]?.rows ?? []
}

/** 条形宽度按该组件内最大值归一，不跨组件比较。 */
function barWidth(count: number, componentId: string): string {
  const rows = rowsOf(componentId)
  const max = rows.reduce((acc, row) => Math.max(acc, row.count), 0)
  return max ? `${Math.round((count / max) * 100)}%` : '0%'
}

function datasetLabel(code: string): string {
  return datasets.value.find((item) => item.code === code)?.label ?? code
}

function keyLabel(key: string): string {
  if (!key) {
    return '未设置'
  }
  return DASHBOARD_STATUS_LABEL[key] ?? key
}

function formatNumber(value?: number | null): string {
  return (value ?? 0).toLocaleString()
}

onMounted(async () => {
  if (!canRead) {
    return
  }
  try {
    datasets.value = await boardApi.datasets()
  } catch {
    // 字典不可用时仍可查看已有看板（组件自带标题）
  }
  await loadBoards()
  if (boards.value.length) {
    currentId.value = boards.value[0].id
    await loadBoard(currentId.value)
    await loadData(currentId.value)
  }
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8a94a6;
}

.board-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
}

.board-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.tag-gap {
  margin-left: 6px;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.board-cell {
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 6px;
  padding: 10px 12px;
  min-width: 0;
}

.cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: default;
}

.cell-head[draggable='true'] {
  cursor: move;
}

.cell-title {
  font-weight: 600;
  color: #303133;
}

.drag-handle {
  margin-right: 6px;
  color: #b1b8c4;
  letter-spacing: -2px;
}

.cell-meta {
  font-size: 12px;
  color: #8a94a6;
}

.cell-form {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.cell-actions {
  width: 100%;
  display: flex;
  gap: 4px;
  justify-content: flex-end;
}

.metric-body {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 14px 0;
}

.metric-value {
  font-size: 30px;
  font-weight: 700;
  color: #1f2d3d;
}

.metric-unit {
  font-size: 12px;
  color: #8a94a6;
}

.bar-body {
  margin-top: 10px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
}

.bar-label {
  width: 96px;
  flex: none;
  color: #5c6b7a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  flex: 1;
  height: 10px;
  background: #f0f3f8;
  border-radius: 5px;
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  background: #409eff;
  border-radius: 5px;
}

.bar-count {
  width: 40px;
  flex: none;
  text-align: right;
  font-weight: 600;
  color: #303133;
}

.add-btn {
  margin-top: 12px;
}
</style>
