<template>
  <div class="page">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="对象类型" name="entity">
        <div class="card-header">
          <div class="filters">
            <el-input v-model="keyword" placeholder="编码 / 名称" clearable style="width: 240px" @keyup.enter="loadEntities" />
            <el-button type="primary" @click="loadEntities">查询</el-button>
          </div>
          <el-button type="primary" @click="openCreateEntity">新建对象类型</el-button>
        </div>

        <el-table :data="entities" v-loading="entityLoading" border stripe>
          <el-table-column prop="code" label="类型编码" width="180" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="parentCode" label="父类型" width="140">
            <template #default="{ row }">{{ row.parentCode || '-' }}</template>
          </el-table-column>
          <el-table-column prop="fieldCount" label="字段数" width="90" />
          <el-table-column prop="versionNo" label="版本" width="80" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
                {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditEntity(row.code)">编辑</el-button>
              <el-button link type="success" @click="onPublish(row.code)">发布</el-button>
              <el-button link type="danger" @click="onDeleteEntity(row.code)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="值列表" name="enum">
        <div class="filters">
          <el-select v-model="selectedEnum" placeholder="选择枚举" style="width: 260px" @change="onEnumChange">
            <el-option v-for="item in enums" :key="item.code" :label="`${item.name}（${item.code}）`" :value="item.code" />
          </el-select>
          <el-button type="primary" :disabled="!selectedEnum" @click="addEnumItem">新增项</el-button>
          <el-button type="success" :disabled="!selectedEnum" @click="saveEnumItems">保存</el-button>
        </div>

        <el-table :data="enumItems" border>
          <el-table-column label="编码" width="220">
            <template #default="{ row }">
              <el-input v-model="row.code" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="名称">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="排序" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.sort" :min="0" :max="9999" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ $index }">
              <el-button link type="danger" @click="enumItems.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="entityVisible" :title="entityEditing ? '编辑对象类型' : '新建对象类型'" size="820px">
      <el-form ref="entityFormRef" :model="entityForm" :rules="entityRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型编码" prop="code">
              <el-input v-model="entityForm.code" :disabled="entityEditing" placeholder="如 PdmDoc" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="entityForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父类型">
              <el-select v-model="entityForm.parentCode" clearable style="width: 100%">
                <el-option v-for="item in entities" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生命周期">
              <el-input v-model="entityForm.lifecycleCode" placeholder="如 DEFAULT_LIFECYCLE" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本策略">
              <el-select v-model="entityForm.versionPolicy" style="width: 100%">
                <el-option label="字母版本（A/B/C）" value="LETTER" />
                <el-option label="数字版本（1/2/3）" value="NUMBER" />
                <el-option label="不受版本控制" value="NONE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可发起流程">
              <el-switch v-model="entityForm.workflowAllowed" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="能力位">
              <el-checkbox-group v-model="entityForm.capabilities">
                <el-checkbox value="createPermission">新建需授权</el-checkbox>
                <el-checkbox value="dataIsolation">启用数据隔离</el-checkbox>
                <el-checkbox value="advanceQuery">支持高级查询</el-checkbox>
                <el-checkbox value="ruleCondition">可作为规则条件对象</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">字段定义</el-divider>
        <div class="field-toolbar">
          <el-button size="small" type="primary" @click="addField">新增字段</el-button>
          <span class="hint">值类型见方案 5.1（共 31 种语义类型）</span>
        </div>
        <el-table :data="entityForm.fields" border size="small" max-height="360">
          <el-table-column label="编码" width="150">
            <template #default="{ row }">
              <el-input v-model="row.code" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="名称" width="140">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="值类型" width="200">
            <template #default="{ row }">
              <el-select v-model="row.valueType" size="small" filterable>
                <el-option v-for="type in valueTypes" :key="type" :label="type" :value="type" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="枚举" width="160">
            <template #default="{ row }">
              <el-select v-model="row.enumCode" size="small" clearable filterable>
                <el-option v-for="item in enums" :key="item.code" :label="item.code" :value="item.code" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="70">
            <template #default="{ row }">
              <el-checkbox v-model="row.required" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button link type="danger" @click="entityForm.fields.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>

      <template #footer>
        <el-button @click="entityVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveEntity">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  metaApi,
  type MetaEntityDetail,
  type MetaEntitySummary,
  type MetaEnumDetail,
  type MetaEnumItem,
  type MetaFieldDef
} from '@/api/system'

const activeTab = ref('entity')

const entities = ref<MetaEntitySummary[]>([])
const entityLoading = ref(false)
const keyword = ref('')

const enums = ref<MetaEnumDetail[]>([])
const selectedEnum = ref('')
const enumItems = ref<MetaEnumItem[]>([])

const entityVisible = ref(false)
const entityEditing = ref(false)
const saving = ref(false)
const entityFormRef = ref<FormInstance>()

const valueTypes = [
  'DefaultObjectValue', 'DescriptionValue', 'BoolValue', 'DateTimeValue', 'DateValue',
  'CodeValue', 'StateValue', 'StageValue', 'EnumValue', 'MultiEnumValue', 'EnumRadioValue',
  'LovListValue', 'VersionValue', 'MinorVersionValue', 'MajorMinorVersionValue',
  'SingleUserValue', 'MultiUserValue', 'SingleUserAutoComboxValue', 'SingleGroupValue',
  'CompanyValue', 'TaskPrincipalValue', 'PositionValue', 'SelectPositionValue',
  'ObjectIDValue', 'SubDataModelValue', 'SubDataModelIconValue', 'PseItemNameCodeValue',
  'PseBomViewDataValue', 'FileUploadValue', 'EditButtonValue', 'ThickRuleValue', 'ThinRuleValue'
]

interface EntityForm {
  code: string
  name: string
  parentCode: string | null
  lifecycleCode: string
  versionPolicy: string
  workflowAllowed: boolean
  capabilities: string[]
  fields: MetaFieldDef[]
}

const entityForm = reactive<EntityForm>({
  code: '',
  name: '',
  parentCode: null,
  lifecycleCode: 'DEFAULT_LIFECYCLE',
  versionPolicy: 'LETTER',
  workflowAllowed: true,
  capabilities: ['createPermission', 'dataIsolation', 'advanceQuery'],
  fields: []
})

const entityRules: FormRules = {
  code: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function loadEntities(): Promise<void> {
  entityLoading.value = true
  try {
    entities.value = await metaApi.listEntities(keyword.value || undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    entityLoading.value = false
  }
}

async function loadEnums(): Promise<void> {
  enums.value = await metaApi.listEnums()
}

function openCreateEntity(): void {
  entityEditing.value = false
  Object.assign(entityForm, {
    code: '',
    name: '',
    parentCode: null,
    lifecycleCode: 'DEFAULT_LIFECYCLE',
    versionPolicy: 'LETTER',
    workflowAllowed: true,
    capabilities: ['createPermission', 'dataIsolation', 'advanceQuery'],
    fields: []
  })
  entityVisible.value = true
}

async function openEditEntity(code: string): Promise<void> {
  entityEditing.value = true
  const detail: MetaEntityDetail = await metaApi.getEntity(code)
  Object.assign(entityForm, {
    code: detail.code,
    name: detail.name,
    parentCode: detail.parentCode,
    lifecycleCode: detail.lifecycleCode ?? '',
    versionPolicy: detail.versionPolicy,
    workflowAllowed: detail.workflowAllowed,
    capabilities: [...detail.capabilities],
    fields: detail.fields.map((field) => ({ ...field }))
  })
  entityVisible.value = true
}

function addField(): void {
  entityForm.fields.push({
    code: '',
    name: '',
    valueType: 'DefaultObjectValue',
    enumCode: null,
    required: false
  })
}

async function onSaveEntity(): Promise<void> {
  if (!entityFormRef.value) {
    return
  }
  const valid = await entityFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = { ...entityForm, code: entityForm.code.trim() }
    if (entityEditing.value) {
      await metaApi.updateEntity(payload.code, payload)
    } else {
      await metaApi.createEntity(payload)
    }
    ElMessage.success('已保存')
    entityVisible.value = false
    await loadEntities()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onPublish(code: string): Promise<void> {
  try {
    await metaApi.publish(code)
    ElMessage.success('已发布')
    await loadEntities()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function onDeleteEntity(code: string): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除对象类型「${code}」？仅草稿可删除。`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await metaApi.remove(code)
    ElMessage.success('已删除')
    await loadEntities()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function onEnumChange(code: string): void {
  const target = enums.value.find((item) => item.code === code)
  enumItems.value = target ? target.items.map((item) => ({ ...item })) : []
}

function addEnumItem(): void {
  enumItems.value.push({ code: '', name: '', sort: enumItems.value.length * 10 })
}

async function saveEnumItems(): Promise<void> {
  try {
    await metaApi.saveEnumItems(selectedEnum.value, enumItems.value)
    ElMessage.success('已保存')
    await loadEnums()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

onMounted(async () => {
  await Promise.all([loadEntities(), loadEnums()])
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.field-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.hint {
  color: #909399;
  font-size: 12px;
}
</style>
