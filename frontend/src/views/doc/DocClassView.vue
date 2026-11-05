<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left">
            <span class="title">文档分类</span>
            <span class="hint">分类属性支持继承：子分类可见父分类「可继承」属性，文档保存时按生效属性校验必填</span>
          </div>
          <div>
            <el-button type="primary" @click="openCreate(null)">新建根分类</el-button>
            <el-button @click="loadTree">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="body">
        <div class="tree-panel" v-loading="treeLoading">
          <el-tree
            :data="tree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            highlight-current
            :current-node-key="selected?.id"
            @node-click="onSelect"
          >
            <template #default="{ data }">
              <span class="node">
                <el-icon><Folder /></el-icon>
                <span>{{ data.name }}</span>
                <span class="node-code">{{ data.code }}</span>
                <span class="node-count">{{ data.docCount }} 篇</span>
                <span class="node-actions">
                  <el-button link type="primary" size="small" @click.stop="openCreate(data)">新建下级</el-button>
                  <el-button link type="primary" size="small" @click.stop="openEdit(data)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="onDelete(data)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="tree.length === 0" description="暂无分类，点击「新建根分类」开始" />
        </div>

        <div class="attr-panel">
          <template v-if="selected">
            <div class="panel-title">
              <span>{{ selected.name }}</span>
              <el-tag size="small" type="info">{{ selected.path }}</el-tag>
              <el-tag v-if="selected.status !== 'ACTIVE'" size="small" type="danger">已停用</el-tag>
            </div>

            <div class="toolbar">
              <el-button type="primary" size="small" @click="addAttr">新增属性</el-button>
              <el-button size="small" :loading="saving" @click="saveAttrs">保存属性</el-button>
              <span class="hint">属性编码保存后不建议修改；未出现在列表中的属性会被删除</span>
            </div>

            <el-table :data="editable" border size="small">
              <el-table-column label="属性编码" width="160">
                <template #default="{ row }">
                  <el-input v-model="row.code" placeholder="如 sheetSize" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="属性名称" width="150">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="如 图幅" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="值类型" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.valueType" size="small">
                    <el-option
                      v-for="(label, value) in DOC_CLASS_ATTR_TYPE_LABEL"
                      :key="value"
                      :label="label"
                      :value="value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="枚举" width="160">
                <template #default="{ row }">
                  <el-select
                    v-model="row.enumCode"
                    size="small"
                    clearable
                    placeholder="仅枚举类型"
                    :disabled="row.valueType !== 'ENUM'"
                  >
                    <el-option v-for="item in enums" :key="item.code" :label="item.name" :value="item.code" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.required" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="可继承" width="90">
                <template #default="{ row }">
                  <el-switch v-model="row.inheritable" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="100">
                <template #default="{ row }">
                  <el-input-number v-model="row.sort" size="small" :min="0" :step="10" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="removeAttr($index)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="该分类暂无自身属性" />
              </template>
            </el-table>

            <el-divider content-position="left">继承属性（只读，来自祖先分类）</el-divider>
            <el-table :data="inheritedAttrs" border size="small">
              <el-table-column prop="code" label="属性编码" width="160" />
              <el-table-column prop="name" label="属性名称" width="150" />
              <el-table-column label="值类型" width="100">
                <template #default="{ row }">{{ DOC_CLASS_ATTR_TYPE_LABEL[row.valueType] ?? row.valueType }}</template>
              </el-table-column>
              <el-table-column label="必填" width="80">
                <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column prop="ownerClassName" label="来源分类" min-width="140" />
              <template #empty>
                <el-empty description="无继承属性" />
              </template>
            </el-table>
          </template>
          <el-empty v-else description="请选择左侧分类查看或维护属性" />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-width="90px">
        <el-form-item label="上级分类">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="classForm.code" placeholder="如 MECH-ASSY（字母开头，可含 - 与 _）" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="classForm.name" placeholder="如 装配图" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="classForm.sort" :min="0" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="classForm.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="classForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveClass">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder } from '@element-plus/icons-vue'
import {
  DOC_CLASS_ATTR_TYPE_LABEL,
  docClassApi,
  type DocClassAttrDef,
  type DocClassAttrItem,
  type DocClassNode
} from '@/api/doc'
import { metaApi, type MetaEnumDetail } from '@/api/system'

const tree = ref<DocClassNode[]>([])
const treeLoading = ref(false)
const selected = ref<DocClassNode | null>(null)
const effective = ref<DocClassAttrDef[]>([])
const editable = ref<DocClassAttrItem[]>([])
const enums = ref<MetaEnumDetail[]>([])
const saving = ref(false)
const dirty = ref(false)

const inheritedAttrs = computed(() => effective.value.filter((attr) => attr.inherited))

const dialogVisible = ref(false)
const dialogTitle = ref('新建分类')
const editingId = ref<string | null>(null)
const parentForCreate = ref<DocClassNode | null>(null)
const classForm = reactive({
  code: '',
  name: '',
  sort: 0,
  status: 'ACTIVE',
  remark: ''
})

const parentLabel = computed(() => {
  if (editingId.value) {
    return selected.value?.parentId ? findNode(tree.value, selected.value.parentId)?.name ?? '-' : '根分类'
  }
  return parentForCreate.value ? parentForCreate.value.name : '根分类'
})

function findNode(nodes: DocClassNode[], id: string): DocClassNode | null {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    const found = findNode(node.children, id)
    if (found) {
      return found
    }
  }
  return null
}

async function loadTree(): Promise<void> {
  treeLoading.value = true
  try {
    tree.value = await docClassApi.tree()
    if (selected.value) {
      const still = findNode(tree.value, selected.value.id)
      selected.value = still
      if (still) {
        await loadAttrs(still)
      } else {
        effective.value = []
        editable.value = []
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类失败')
  } finally {
    treeLoading.value = false
  }
}

async function onSelect(node: DocClassNode): Promise<void> {
  selected.value = node
  await loadAttrs(node)
}

async function loadAttrs(node: DocClassNode): Promise<void> {
  try {
    effective.value = await docClassApi.attrs(node.id)
    editable.value = effective.value
      .filter((attr) => !attr.inherited)
      .map((attr) => ({
        code: attr.code,
        name: attr.name,
        valueType: attr.valueType,
        enumCode: attr.enumCode ?? '',
        required: attr.required,
        inheritable: attr.inheritable,
        defaultValue: attr.defaultValue ?? '',
        sort: attr.sort
      }))
    dirty.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类属性失败')
  }
}

function openCreate(parent: DocClassNode | null): void {
  editingId.value = null
  parentForCreate.value = parent
  classForm.code = ''
  classForm.name = ''
  classForm.sort = 0
  classForm.status = 'ACTIVE'
  classForm.remark = ''
  dialogTitle.value = parent ? `在「${parent.name}」下新建分类` : '新建根分类'
  dialogVisible.value = true
}

function openEdit(node: DocClassNode): void {
  editingId.value = node.id
  parentForCreate.value = null
  classForm.code = node.code
  classForm.name = node.name
  classForm.sort = node.sort
  classForm.status = node.status
  classForm.remark = node.remark ?? ''
  dialogTitle.value = `编辑分类：${node.name}`
  dialogVisible.value = true
}

async function onSaveClass(): Promise<void> {
  if (!classForm.code.trim() || !classForm.name.trim()) {
    ElMessage.warning('请填写分类编码与名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await docClassApi.update(editingId.value, {
        code: classForm.code.trim(),
        name: classForm.name.trim(),
        sort: classForm.sort,
        status: classForm.status,
        remark: classForm.remark || null
      })
      ElMessage.success('已保存')
    } else {
      await docClassApi.create({
        code: classForm.code.trim(),
        name: classForm.name.trim(),
        parentId: parentForCreate.value?.id ?? null,
        sort: classForm.sort,
        status: classForm.status,
        remark: classForm.remark || null
      })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(node: DocClassNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除分类「${node.name}」？需先无子分类且无文档引用`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await docClassApi.remove(node.id)
    ElMessage.success('已删除')
    if (selected.value?.id === node.id) {
      selected.value = null
      effective.value = []
      editable.value = []
    }
    await loadTree()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function addAttr(): void {
  editable.value.push({
    code: '',
    name: '',
    valueType: 'STRING',
    enumCode: '',
    required: false,
    inheritable: true,
    defaultValue: '',
    sort: (editable.value.length + 1) * 10
  })
  dirty.value = true
}

function removeAttr(index: number): void {
  editable.value.splice(index, 1)
  dirty.value = true
}

async function saveAttrs(): Promise<void> {
  if (!selected.value) {
    return
  }
  saving.value = true
  try {
    effective.value = await docClassApi.saveAttrs(selected.value.id, editable.value)
    ElMessage.success('属性已保存')
    await loadAttrs(selected.value)
    await loadTree()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存属性失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTree()
  try {
    enums.value = await metaApi.listEnums()
  } catch {
    enums.value = []
  }
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
  gap: 12px;
}

.left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-weight: 600;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.body {
  display: flex;
  gap: 16px;
}

.tree-panel {
  width: 380px;
  min-height: 460px;
  border-right: 1px solid #eef1f6;
  padding-right: 12px;
}

.attr-panel {
  flex: 1;
  min-width: 0;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}

.node-code {
  color: #909399;
  font-size: 12px;
}

.node-count {
  color: #409eff;
  font-size: 12px;
}

.node-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.node:hover .node-actions {
  opacity: 1;
}
</style>
