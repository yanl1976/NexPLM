<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? '编辑零部件' : '新建零部件'"
    width="900px"
    @update:model-value="close"
  >
    <el-form :model="form" label-width="90px">
      <div class="form-section">基本属性</div>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="编码">
            <el-input :model-value="displayCode" readonly class="code-readonly" :placeholder="editingId ? '' : '选择分类后自动生成'" />
            <div v-if="!editingId && codePreview" class="code-preview">
              将生成：<span class="code-preview-value">{{ codePreview.code }}</span>
              <el-tag size="small" effect="plain" :type="codePreview.source === 'DRAWING' ? 'warning' : 'success'">
                {{ codePreview.source === 'DRAWING' ? '按图号' : '按分类' }}
              </el-tag>
              <span class="code-preview-src">{{ codePreview.ruleName }}（预览不占号）</span>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="名称" required>
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="类型">
            <el-select v-model="form.itemType" style="width: 100%">
              <el-option v-for="(label, value) in ITEM_TYPE_LABEL" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类">
            <el-cascader
              v-model="form.classId"
              :options="classTree"
              :props="{ value: 'id', label: 'name', children: 'children', emitPath: false }"
              clearable
              filterable
              placeholder="只能选最末级（小类）"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="图号">
            <el-input v-model="form.designAttr.dwgNo" placeholder="填图号可自动生成编号" :disabled="Boolean(editingId)" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="项目" required>
            <el-select v-model="form.projectId" filterable clearable style="width: 100%">
              <el-option
                v-for="project in projects"
                :key="project.id"
                :label="`${project.code} ${project.name}`"
                :value="String(project.id)"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="位置">
            <el-input v-model="form.location" disabled placeholder="绑定项目后自动生成" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="供应方式">
            <el-radio-group v-model="form.sourceType">
              <el-radio-button value="MAKE">自制</el-radio-button>
              <el-radio-button value="BUY">外购</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格型号">
            <el-input v-model="form.specification" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="材料">
            <el-input v-model="form.material" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计量单位">
            <el-input v-model="form.unit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单件重量">
            <el-input-number v-model="form.weight" :min="0" :precision="3" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标准号">
            <el-input v-model="form.stdCode" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-section">设计属性</div>
      <el-row :gutter="12">
        <el-col v-for="field in DESIGN_FIELDS" :key="field.key" :span="12">
          <el-form-item :label="field.label">
            <el-switch v-if="field.bool" v-model="form.designAttr[field.key]" />
            <el-input v-else v-model="form.designAttr[field.key]" />
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="customFields.length">
        <div class="form-section">扩展属性（ERP）</div>
        <el-row :gutter="12">
          <el-col v-for="field in customFields" :key="field.code" :span="12">
            <el-form-item :label="field.name || field.code">
              <el-select v-if="isBoolField(field)" v-model="extBoolForm[field.code]" clearable style="width: 100%">
                <el-option label="是" :value="true" />
                <el-option label="否" :value="false" />
              </el-select>
              <el-input v-else v-model="extTextForm[field.code]" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="close(false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  itemApi,
  itemClassApi,
  ITEM_TYPE_LABEL,
  type CodePreview,
  type ItemClassNode,
  type ItemDesignAttr,
  type ItemSaveReq
} from '@/api/item'
import { type MetaFieldDef } from '@/api/system'
import {
  buildErpPatch,
  buildErpValues,
  isBoolField,
  loadItemCustomFields,
  pickExtValues
} from '@/utils/itemErpFields'

const props = defineProps<{
  modelValue: boolean
  editingId?: string | null
  presetProjectId?: string | null
  presetClassId?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  saved: [string]
}>()

interface SelectableClass extends ItemClassNode {
  selectDisabled: boolean
}

const EXCLUDED_ROOTS = ['80', 'CH']

const DESIGN_FIELDS: Array<{ key: keyof ItemDesignAttr; label: string; bool?: boolean }> = [
  { key: 'provider', label: '供应商' },
  { key: 'dwgNo', label: '图号' },
  { key: 'productCategory', label: '产品类别' },
  { key: 'productModel', label: '产品型号' },
  { key: 'productSpec', label: '产品规格' },
  { key: 'materialType', label: '材料类型' },
  { key: 'materialType2', label: '材料类型2' },
  { key: 'surfaceTreatment', label: '表面处理' },
  { key: 'isTooling', label: '是否工装', bool: true },
  { key: 'weightUnit', label: '重量单位' },
  { key: 'montage', label: '装配' },
  { key: 'colorItem', label: '颜色件', bool: true }
]

function emptyDesignAttr(): ItemDesignAttr {
  return {
    provider: '',
    dwgNo: '',
    productCategory: '',
    productModel: '',
    productSpec: '',
    materialType: '',
    materialType2: '',
    surfaceTreatment: '',
    isTooling: false,
    weightUnit: '',
    montage: '',
    colorItem: false
  }
}

const form = reactive<ItemSaveReq & { designAttr: ItemDesignAttr }>({
  code: '',
  name: '',
  itemType: 'PART',
  sourceType: 'MAKE',
  specification: '',
  material: '',
  unit: 'EA',
  weight: null,
  stdCode: '',
  remark: '',
  classId: null,
  location: '',
  projectId: null,
  designAttr: emptyDesignAttr()
})

const classTree = ref<SelectableClass[]>([])
const projects = ref<Array<{ id: string; code: string; name: string }>>([])
const codePreview = ref<CodePreview | null>(null)
const customFields = ref<MetaFieldDef[]>([])
const extTextForm = reactive<Record<string, string>>({})
const extBoolForm = reactive<Record<string, boolean | ''>>({})
const extOriginal = ref<Record<string, unknown>>({})
const saving = ref(false)

const displayCode = computed(() =>
  props.editingId ? form.code ?? '' : codePreview.value?.code ?? ''
)

function toSelectable(nodes: ItemClassNode[]): SelectableClass[] {
  return nodes
    .filter((node) => !EXCLUDED_ROOTS.includes(node.code))
    .map((node) => ({
      ...node,
      selectDisabled: (node.children?.length ?? 0) > 0,
      children: toSelectable(node.children ?? [])
    }))
}

async function loadBasics(): Promise<void> {
  const [tree, projectList, fields] = await Promise.all([
    itemClassApi.tree(),
    itemApi.projectOptions(),
    loadItemCustomFields()
  ])
  classTree.value = toSelectable(tree)
  projects.value = projectList
  customFields.value = fields
}

async function refreshPreview(): Promise<void> {
  codePreview.value = null
  if (props.editingId) {
    return
  }
  try {
    codePreview.value = await itemApi.previewCode({
      classId: form.classId || undefined,
      itemType: form.itemType,
      dwgNo: form.designAttr.dwgNo?.trim() || undefined
    })
  } catch {
    codePreview.value = null
  }
}

let previewTimer: ReturnType<typeof setTimeout> | null = null
watch(
  () => [form.classId, form.itemType, form.designAttr.dwgNo],
  () => {
    if (previewTimer) {
      clearTimeout(previewTimer)
    }
    previewTimer = setTimeout(refreshPreview, 250)
  }
)

watch(
  () => form.projectId,
  (id) => {
    if (props.editingId || !id) {
      return
    }
    const hit = projects.value.find((p) => String(p.id) === String(id))
    if (hit) {
      form.location = `个人空间/${hit.code}/物料`
    }
  }
)

function resetExtForms(source: Record<string, unknown>): void {
  Object.keys(extTextForm).forEach((key) => delete extTextForm[key])
  Object.keys(extBoolForm).forEach((key) => delete extBoolForm[key])
  const values = pickExtValues(customFields.value, source)
  Object.entries(values.text).forEach(([key, value]) => (extTextForm[key] = value))
  Object.entries(values.bool).forEach(([key, value]) => (extBoolForm[key] = value))
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return
    }
    await loadBasics()
    if (props.editingId) {
      const detail = await itemApi.detail(props.editingId)
      form.code = detail.code ?? ''
      form.name = detail.name ?? ''
      form.itemType = detail.itemType ?? 'PART'
      form.sourceType = 'MAKE'
      form.specification = detail.specification ?? ''
      form.material = detail.material ?? ''
      form.unit = detail.unit ?? 'EA'
      form.stdCode = detail.stdCode ?? ''
      form.remark = detail.remark ?? ''
      form.location = (detail as unknown as { location?: string }).location ?? ''
      form.projectId = (detail as unknown as { projectId?: string }).projectId ?? null
      form.classId = null
      form.designAttr = { ...emptyDesignAttr(), ...(detail.designAttr ?? {}) }
      extOriginal.value = { ...(detail.ext ?? {}) }
      resetExtForms(extOriginal.value)
    } else {
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
      form.classId = props.presetClassId ?? null
      form.location = ''
      form.projectId = props.presetProjectId ?? null
      form.designAttr = emptyDesignAttr()
      extOriginal.value = {}
      resetExtForms({})
      if (form.projectId) {
        const hit = projects.value.find((p) => String(p.id) === String(form.projectId))
        if (hit) {
          form.location = `个人空间/${hit.code}/物料`
        }
      }
      refreshPreview()
    }
  }
)

function close(value = false): void {
  emit('update:modelValue', value)
}

async function submit(): Promise<void> {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  if (!form.projectId) {
    ElMessage.warning('请选择项目')
    return
  }
  saving.value = true
  try {
    const payload: ItemSaveReq = {
      ...form,
      code: undefined,
      ext: props.editingId
        ? buildErpPatch(customFields.value, extTextForm, extBoolForm, extOriginal.value)
        : buildErpValues(customFields.value, extTextForm, extBoolForm)
    }
    const result = props.editingId
      ? await itemApi.update(props.editingId, payload)
      : await itemApi.create(payload)
    ElMessage.success('已保存')
    emit('saved', String(result.id))
    close(false)
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadBasics)
</script>

<style scoped>
.form-section {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 4px;
  margin: 8px 0 12px;
}

.code-readonly :deep(.el-input__wrapper) {
  background-color: var(--el-fill-color-dark, #e6e8eb);
}

.code-preview {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.code-preview-value {
  font-family: var(--el-font-family-mono, monospace);
  color: var(--el-color-primary);
  font-weight: 600;
}

.code-preview-src {
  margin-left: 6px;
  color: var(--el-text-color-placeholder);
}
</style>
