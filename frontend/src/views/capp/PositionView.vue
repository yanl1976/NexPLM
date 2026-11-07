<script setup lang="ts">
/**
 * 工厂结构与消耗定义。对齐旧系统「工艺管理-工位」（PositionManager 的 POSITIONITEM 树）
 * 与「业务管理-消耗定义」（ConsumptionStation 的 CSOBJ）：左侧层级树，右侧当前工位的
 * 属性 + 消耗清单。旧系统是两套页面（工位管理 / 消耗站），这里合并成一屏，因为
 * 消耗定义永远挂在某个工位上，分开看反而要来回切。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { positionApi } from '@/api/position'
import type { ConsumptionRow, PositionNode } from '@/api/position'

const LEVELS = [
  { value: 'PLANT', label: '工厂' },
  { value: 'WORKSHOP', label: '车间' },
  { value: 'LINE', label: '产线' },
  { value: 'STATION', label: '工位' },
  { value: 'WORKCENTER', label: '工作地' }
]

const loading = ref(false)
const tree = ref<PositionNode[]>([])
const currentId = ref<string | null>(null)
const keyword = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const form = ref({
  id: '',
  code: '',
  name: '',
  parentId: '' as string | null,
  positionLevel: 'STATION',
  logisticsUnit: '',
  capacity: null as number | null,
  shifts: null as number | null,
  enabled: true,
  remark: ''
})
const isEdit = computed(() => !!form.value.id)

const consumptions = ref<ConsumptionRow[]>([])
const consDialogVisible = ref(false)
const consForm = ref({
  id: '',
  name: '',
  objectId: '' as string | null,
  itemCode: '',
  itemName: '',
  qty: 1,
  unit: '',
  suitability: '',
  fitProduct: '',
  allRoute: false,
  isPrimary: false,
  remark: ''
})

function findNode(nodes: PositionNode[], id: string): PositionNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    const hit = findNode(node.children || [], id)
    if (hit) return hit
  }
  return null
}

const currentNode = computed(() => (currentId.value ? findNode(tree.value, currentId.value) : null))

async function loadTree() {
  loading.value = true
  try {
    tree.value = await positionApi.tree()
  } catch (error) {
    ElMessage.error((error as Error).message || '加载工位树失败')
  } finally {
    loading.value = false
  }
}

async function loadConsumptions() {
  try {
    consumptions.value = await positionApi.consumptions(currentId.value || undefined, keyword.value || undefined)
  } catch (error) {
    ElMessage.error((error as Error).message || '加载消耗定义失败')
  }
}

function onNodeClick(node: PositionNode) {
  currentId.value = node.id
  void loadConsumptions()
}

function openCreate(parentId?: string | null) {
  form.value = {
    id: '',
    code: '',
    name: '',
    parentId: parentId ?? null,
    positionLevel: parentId ? 'STATION' : 'PLANT',
    logisticsUnit: '',
    capacity: null,
    shifts: null,
    enabled: true,
    remark: ''
  }
  dialogVisible.value = true
}

function openEdit(node: PositionNode) {
  form.value = {
    id: node.id,
    code: node.code,
    name: node.name,
    parentId: node.parentId,
    positionLevel: node.positionLevel,
    logisticsUnit: node.logisticsUnit || '',
    capacity: node.capacity,
    shifts: node.shifts,
    enabled: node.enabled,
    remark: node.remark || ''
  }
  dialogVisible.value = true
}

async function savePosition() {
  if (!form.value.name.trim()) {
    ElMessage.warning('工位名称必填')
    return
  }
  saving.value = true
  try {
    const body = {
      code: form.value.code || undefined,
      name: form.value.name,
      parentId: form.value.parentId || null,
      positionLevel: form.value.positionLevel,
      logisticsUnit: form.value.logisticsUnit || undefined,
      capacity: form.value.capacity,
      shifts: form.value.shifts,
      enabled: form.value.enabled,
      remark: form.value.remark || undefined
    }
    if (isEdit.value) {
      await positionApi.update(form.value.id, body)
    } else {
      await positionApi.create(body)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removePosition(node: PositionNode) {
  await ElMessageBox.confirm(`删除工位「${node.code} ${node.name}」？`, '删除工位', { type: 'warning' })
  try {
    await positionApi.remove(node.id)
    ElMessage.success('已删除')
    if (currentId.value === node.id) currentId.value = null
    await loadTree()
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败')
  }
}

function openConsCreate() {
  consForm.value = {
    id: '',
    name: '',
    objectId: null,
    itemCode: '',
    itemName: '',
    qty: 1,
    unit: '',
    suitability: '',
    fitProduct: '',
    allRoute: false,
    isPrimary: false,
    remark: ''
  }
  consDialogVisible.value = true
}

function openConsEdit(row: ConsumptionRow) {
  consForm.value = {
    id: row.id,
    name: row.name || '',
    objectId: row.objectId,
    itemCode: row.itemCode || '',
    itemName: row.itemName || '',
    qty: row.qty,
    unit: row.unit || '',
    suitability: row.suitability || '',
    fitProduct: row.fitProduct || '',
    allRoute: row.allRoute,
    isPrimary: row.isPrimary,
    remark: row.remark || ''
  }
  consDialogVisible.value = true
}

async function saveConsumption() {
  if (!currentId.value) {
    ElMessage.warning('请先在左侧选择工位')
    return
  }
  try {
    const body = {
      name: consForm.value.name || undefined,
      positionId: currentId.value,
      objectId: consForm.value.objectId || null,
      itemCode: consForm.value.itemCode || undefined,
      itemName: consForm.value.itemName || undefined,
      qty: consForm.value.qty,
      unit: consForm.value.unit || undefined,
      refItem: true,
      suitability: consForm.value.suitability || undefined,
      fitProduct: consForm.value.fitProduct || undefined,
      allRoute: consForm.value.allRoute,
      isPrimary: consForm.value.isPrimary,
      remark: consForm.value.remark || undefined
    }
    if (consForm.value.id) {
      await positionApi.updateConsumption(consForm.value.id, body)
    } else {
      await positionApi.createConsumption(body)
    }
    ElMessage.success('已保存')
    consDialogVisible.value = false
    await loadConsumptions()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  }
}

async function removeConsumption(row: ConsumptionRow) {
  await ElMessageBox.confirm(`删除消耗定义「${row.code}」？`, '删除', { type: 'warning' })
  await positionApi.removeConsumption(row.id)
  ElMessage.success('已删除')
  await loadConsumptions()
}

onMounted(async () => {
  await loadTree()
  if (!currentId.value) {
    await loadConsumptions()
  }
})

function levelLabel(level: string) {
  return LEVELS.find((l) => l.value === level)?.label || level
}
</script>

<template>
  <div class="position-view">
    <el-row :gutter="12">
      <el-col :span="7">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="title">工厂结构</span>
              <div>
                <el-button size="small" :icon="Plus" @click="openCreate(null)">新建工厂</el-button>
                <el-button size="small" :icon="Refresh" @click="loadTree" />
              </div>
            </div>
          </template>
          <el-tree
            v-loading="loading"
            :data="tree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            highlight-current
            @node-click="onNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span class="code">{{ data.code }}</span>
                <span>{{ data.name }}</span>
                <el-tag size="small" type="info" effect="plain">{{ levelLabel(data.positionLevel) }}</el-tag>
                <el-button link type="primary" size="small" @click.stop="openCreate(data.id)">+子级</el-button>
                <el-button link type="primary" size="small" @click.stop="openEdit(data)">改</el-button>
                <el-button link type="danger" size="small" @click.stop="removePosition(data)">删</el-button>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="!loading && tree.length === 0" description="还没有工位，先建一个工厂" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :span="17">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="title">
                消耗定义
                <span v-if="currentNode" class="sub">（{{ currentNode.code }} {{ currentNode.name }}）</span>
              </span>
              <div class="filters">
                <el-input v-model="keyword" placeholder="编码/名称/物料" clearable style="width: 200px" @keyup.enter="loadConsumptions" />
                <el-button size="small" @click="loadConsumptions">查询</el-button>
                <el-button size="small" type="primary" :icon="Plus" @click="openConsCreate">新增消耗</el-button>
              </div>
            </div>
          </template>
          <el-table :data="consumptions" size="small" border empty-text="该范围内暂无消耗定义">
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="itemCode" label="物料编码" width="150" />
            <el-table-column prop="itemName" label="物料名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="qty" label="用量" width="90" align="right" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="suitability" label="适用性" width="120" show-overflow-tooltip />
            <el-table-column prop="fitProduct" label="适配产品" width="120" show-overflow-tooltip />
            <el-table-column label="全部路线" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.allRoute" size="small" type="success">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openConsEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="removeConsumption(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工位' : '新建工位'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="留空自动取号（POS-xxxxxx）" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="层级">
          <el-select v-model="form.positionLevel" style="width: 180px">
            <el-option v-for="l in LEVELS" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单位">
          <el-input v-model="form.logisticsUnit" placeholder="同物流单元共搬运资源" />
        </el-form-item>
        <el-form-item label="产能/班次">
          <el-input-number v-model="form.capacity" :min="0" :precision="2" style="width: 150px" />
          <el-input-number v-model="form.shifts" :min="0" :max="4" style="width: 120px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePosition">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="consDialogVisible" :title="consForm.id ? '编辑消耗定义' : '新增消耗定义'" width="600px">
      <el-form label-width="96px">
        <el-form-item label="名称">
          <el-input v-model="consForm.name" />
        </el-form-item>
        <el-form-item label="物料ID">
          <el-input v-model="consForm.objectId" placeholder="填零部件对象 id（留空则手填编码/名称）" />
        </el-form-item>
        <el-form-item v-if="!consForm.objectId" label="物料编码">
          <el-input v-model="consForm.itemCode" />
        </el-form-item>
        <el-form-item v-if="!consForm.objectId" label="物料名称">
          <el-input v-model="consForm.itemName" />
        </el-form-item>
        <el-form-item label="用量/单位">
          <el-input-number v-model="consForm.qty" :min="0" :precision="4" style="width: 150px" />
          <el-input v-model="consForm.unit" placeholder="如 件/kg" style="width: 120px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="适用性">
          <el-input v-model="consForm.suitability" />
        </el-form-item>
        <el-form-item label="适配产品">
          <el-input v-model="consForm.fitProduct" />
        </el-form-item>
        <el-form-item label="标记">
          <el-checkbox v-model="consForm.allRoute" label="适用全部路线" />
          <el-checkbox v-model="consForm.isPrimary" label="主消耗" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="consForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="consDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConsumption">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.position-view {
  padding: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
}

.card-header .sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--plm-text-secondary);
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.tree-node .code {
  color: var(--plm-text-secondary);
}
</style>
