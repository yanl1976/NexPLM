<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>组织架构</span>
          <div>
            <el-button type="primary" @click="openCreate(null)">新增根组织</el-button>
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="body">
        <div class="tree-panel">
          <el-tree
            :data="tree"
            node-key="id"
            :props="{ children: 'children', label: 'name' }"
            default-expand-all
            highlight-current
            @node-click="onSelect"
          >
            <template #default="{ data }">
              <span class="node">
                <span>{{ data.name }}</span>
                <el-tag size="small" type="info" class="node-tag">{{ data.code }}</el-tag>
                <span class="node-actions">
                  <el-button link type="primary" size="small" @click.stop="openCreate(data)">新增下级</el-button>
                  <el-button link type="primary" size="small" @click.stop="openEdit(data)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="onDelete(data)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="tree.length === 0" description="暂无组织，请先新增" />
        </div>

        <div class="detail-panel">
          <el-descriptions v-if="selected" :column="1" border title="节点信息">
            <el-descriptions-item label="名称">{{ selected.name }}</el-descriptions-item>
            <el-descriptions-item label="编码">{{ selected.code }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ selected.orgType }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selected.sort }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ selected.status }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="选择左侧节点查看详情" />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑组织' : '新增组织'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级组织">
          <el-input :model-value="parentLabel" disabled />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="如 DEPT-001" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如 研发部" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.orgType" style="width: 100%">
            <el-option label="公司" value="COMPANY" />
            <el-option label="工厂" value="FACTORY" />
            <el-option label="部门" value="DEPT" />
            <el-option label="组" value="GROUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { orgApi, type OrgNode, type OrgSaveReq } from '@/api/system'

const tree = ref<OrgNode[]>([])
const selected = ref<OrgNode | null>(null)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const parentName = ref<string>('无（作为根组织）')

const form = reactive<OrgSaveReq>({
  parentId: null,
  code: '',
  name: '',
  orgType: 'DEPT',
  sort: 0,
  status: 'ACTIVE'
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入组织名称', trigger: 'blur' }]
}

const parentLabel = computed(() => parentName.value)

async function load(): Promise<void> {
  tree.value = await orgApi.tree()
}

function onSelect(node: OrgNode): void {
  selected.value = node
}

function openCreate(parent: OrgNode | null): void {
  editingId.value = null
  form.parentId = parent?.id ?? null
  parentName.value = parent ? `${parent.name}（${parent.code}）` : '无（作为根组织）'
  form.code = ''
  form.name = ''
  form.orgType = 'DEPT'
  form.sort = 0
  form.status = 'ACTIVE'
  dialogVisible.value = true
}

function openEdit(node: OrgNode): void {
  editingId.value = node.id
  form.parentId = node.parentId
  parentName.value = node.parentId ? '（保持不变）' : '无（根组织）'
  form.code = node.code
  form.name = node.name
  form.orgType = node.orgType
  form.sort = node.sort
  form.status = node.status
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
      await orgApi.update(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await orgApi.create({ ...form })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(node: OrgNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除组织「${node.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await orgApi.remove(node.id)
    ElMessage.success('已删除')
    if (selected.value?.id === node.id) {
      selected.value = null
    }
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
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

.body {
  display: flex;
  gap: 16px;
}

.tree-panel {
  flex: 1;
  min-height: 420px;
  border-right: 1px solid #eef1f6;
  padding-right: 16px;
}

.detail-panel {
  width: 380px;
}

.node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-tag {
  transform: scale(0.85);
}

.node-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.2s;
}

.node:hover .node-actions {
  opacity: 1;
}
</style>
