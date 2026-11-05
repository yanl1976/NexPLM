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
      <el-tabs v-model="activeTab">
        <el-tab-pane label="BOM" name="bom">
          <el-empty description="BOM 树在 P3 第二阶段提供（父子件、三视图、汇总与基线）" />
        </el-tab-pane>
        <el-tab-pane label="被引用" name="whereused">
          <el-empty description="被引用关系将随 BOM 一并提供" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

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
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  itemApi,
  type ItemDetail,
  type ItemSaveReq
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

function goBack(): void {
  router.push({ name: 'item-list' })
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
</style>
