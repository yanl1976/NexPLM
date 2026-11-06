<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'list' ? '供应商评级方案' : form.id ? `编辑方案 ${form.code}` : '新建评级方案'"
    width="1000px"
    destroy-on-close
    @closed="onClosed"
  >
    <!-- 方案列表 -->
    <template v-if="mode === 'list'">
      <div class="bar">
        <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建方案</el-button>
        <span class="hint">
          维度可配、权重合计必须 100；评级按"得分 / 满分 × 权重"加权，改方案不影响历史评级（已存快照）
        </span>
      </div>
      <el-table v-loading="loading" :data="rows" border size="small">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="方案名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="适用类别" width="110" align="center">
          <template #default="{ row }">
            {{ row.category ? (SUPPLIER_CATEGORY_LABEL[row.category] ?? row.category) : '通用' }}
          </template>
        </el-table-column>
        <el-table-column prop="dimCount" label="维度" width="80" align="center" />
        <el-table-column label="权重合计" width="100" align="right">
          <template #default="{ row }">{{ row.weightSum }}</template>
        </el-table-column>
        <el-table-column label="暂停档" width="90" align="center">
          <template #default="{ row }">{{ row.suspendGrade ? `${row.suspendGrade} 级` : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ SCHEME_STATUS_LABEL[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-button link type="danger" @click="onRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" description="还没有评级方案" :image-size="50" />
    </template>

    <!-- 方案编辑 -->
    <template v-else>
      <el-form label-width="96px">
        <el-form-item label="方案名称" required>
          <el-input v-model="form.name" maxlength="200" style="width: 360px" />
          <span class="label-gap">适用类别</span>
          <el-select v-model="form.category" clearable placeholder="通用" style="width: 140px">
            <el-option
              v-for="(label, value) in SUPPLIER_CATEGORY_LABEL"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
          <span class="label-gap">状态</span>
          <el-select v-model="form.status" style="width: 110px">
            <el-option
              v-for="(label, value) in SCHEME_STATUS_LABEL"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="等级规则">
          <div class="bands">
            <div v-for="(band, index) in form.gradeRule" :key="index" class="band-row">
              <el-input v-model="band.grade" maxlength="8" placeholder="等级" style="width: 90px" />
              <span class="label-gap">总分 ≥</span>
              <el-input-number v-model="band.min" :min="0" :max="100" :precision="2" style="width: 120px" />
              <el-button link type="danger" @click="removeBand(index)">移除</el-button>
            </div>
            <el-button link type="primary" @click="addBand">+ 增加等级档</el-button>
            <span class="hint">数组按最低分从高到低判定，需覆盖最差档（通常最低分 0）</span>
          </div>
        </el-form-item>
        <el-form-item label="暂停档">
          <el-select v-model="form.suspendGrade" clearable placeholder="不联动" style="width: 140px">
            <el-option v-for="band in gradeOptions" :key="band" :label="`${band} 级`" :value="band" />
          </el-select>
          <span class="hint">确认评级时若低于等于该档，供应商状态自动置为暂停（不自动恢复）</span>
        </el-form-item>
        <el-form-item label="维度">
          <div class="dims">
            <el-table :data="form.dims" border size="small">
              <el-table-column label="编码" width="130">
                <template #default="{ row }">
                  <el-input v-model="row.code" maxlength="32" placeholder="自动 D1/D2" />
                </template>
              </el-table-column>
              <el-table-column label="维度名称" min-width="140">
                <template #default="{ row }">
                  <el-input v-model="row.name" maxlength="64" />
                </template>
              </el-table-column>
              <el-table-column label="权重" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.weight" :min="0.01" :max="100" :precision="2" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="满分" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.maxScore" :min="1" :max="1000" :precision="2" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="启用" width="80" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeDim($index)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="dim-footer">
              <el-button link type="primary" @click="addDim">+ 增加维度</el-button>
              <span :class="['weight-sum', weightSum === 100 ? 'ok' : 'bad']">
                权重合计 {{ weightSum }} / 100
              </span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <template v-if="mode === 'edit'">
        <el-button @click="mode = 'list'">返回列表</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存方案</el-button>
      </template>
      <el-button v-else @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ratingApi,
  SCHEME_STATUS_LABEL,
  SUPPLIER_CATEGORY_LABEL,
  type GradeBand,
  type RatingSchemeRow
} from '@/api/supplier'

const props = defineProps<{ modelValue: boolean; canWrite: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

interface DimDraft {
  code: string
  name: string
  weight: number
  maxScore: number
  enabled: boolean
}

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const mode = ref<'list' | 'edit'>('list')
const loading = ref(false)
const saving = ref(false)
const rows = ref<RatingSchemeRow[]>([])

const form = reactive({
  id: '',
  code: '',
  name: '',
  category: '',
  status: 'ACTIVE',
  suspendGrade: '',
  remark: '',
  gradeRule: [] as GradeBand[],
  dims: [] as DimDraft[]
})

const weightSum = computed(() =>
  form.dims.reduce((sum, dim) => sum + Number(dim.weight || 0), 0)
)
const gradeOptions = computed(() =>
  form.gradeRule.map((band) => band.grade.trim().toUpperCase()).filter((grade) => grade)
)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      mode.value = 'list'
      void load()
    }
  },
  { immediate: true }
)

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await ratingApi.schemes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载评级方案失败')
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.category = ''
  form.status = 'ACTIVE'
  form.suspendGrade = ''
  form.remark = ''
  form.gradeRule = [
    { grade: 'A', min: 90 },
    { grade: 'B', min: 75 },
    { grade: 'C', min: 60 },
    { grade: 'D', min: 0 }
  ]
  form.dims = [
    { code: 'QUALITY', name: '质量', weight: 40, maxScore: 100, enabled: true },
    { code: 'DELIVERY', name: '交付', weight: 30, maxScore: 100, enabled: true },
    { code: 'PRICE', name: '价格', weight: 20, maxScore: 100, enabled: true },
    { code: 'SERVICE', name: '服务', weight: 10, maxScore: 100, enabled: true }
  ]
  mode.value = 'edit'
}

async function openEdit(id: string): Promise<void> {
  try {
    const detail = await ratingApi.scheme(id)
    form.id = detail.id
    form.code = detail.code
    form.name = detail.name
    form.category = detail.category ?? ''
    form.status = detail.status
    form.suspendGrade = detail.suspendGrade ?? ''
    form.remark = detail.remark ?? ''
    form.gradeRule = detail.gradeRule.map((band) => ({ grade: band.grade, min: band.min }))
    form.dims = detail.dims.map((dim) => ({
      code: dim.code,
      name: dim.name,
      weight: dim.weight,
      maxScore: dim.maxScore,
      enabled: dim.enabled
    }))
    mode.value = 'edit'
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载方案失败')
  }
}

function addBand(): void {
  form.gradeRule.push({ grade: '', min: 0 })
}

function removeBand(index: number): void {
  form.gradeRule.splice(index, 1)
}

function addDim(): void {
  form.dims.push({ code: '', name: '', weight: 0, maxScore: 100, enabled: true })
}

function removeDim(index: number): void {
  form.dims.splice(index, 1)
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写方案名称')
    return
  }
  const bands = form.gradeRule.filter((band) => band.grade.trim())
  if (!bands.length) {
    ElMessage.warning('至少需要一个等级档')
    return
  }
  if (weightSum.value !== 100) {
    ElMessage.warning(`维度权重合计必须为 100，当前为 ${weightSum.value}`)
    return
  }
  if (!form.dims.length || form.dims.some((dim) => !dim.name.trim())) {
    ElMessage.warning('维度名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      category: form.category || null,
      gradeRule: bands.map((band) => ({
        grade: band.grade.trim().toUpperCase(),
        min: band.min
      })),
      suspendGrade: form.suspendGrade || null,
      status: form.status,
      remark: form.remark.trim() || null,
      dims: form.dims.map((dim) => ({
        code: dim.code.trim() || null,
        name: dim.name.trim(),
        weight: dim.weight,
        maxScore: dim.maxScore,
        enabled: dim.enabled
      }))
    }
    if (form.id) {
      await ratingApi.updateScheme(form.id, payload)
    } else {
      await ratingApi.createScheme(payload)
    }
    ElMessage.success('已保存')
    mode.value = 'list'
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存方案失败')
  } finally {
    saving.value = false
  }
}

async function onRemove(row: RatingSchemeRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除方案 ${row.code}？已有评级记录的方案会被拒绝（可改为停用）。`,
      '删除评级方案',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await ratingApi.removeScheme(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function onClosed(): void {
  mode.value = 'list'
}
</script>

<style scoped>
.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}

.bands,
.dims {
  width: 100%;
}

.band-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.dim-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.weight-sum {
  font-size: 13px;
}

.weight-sum.ok {
  color: #67c23a;
}

.weight-sum.bad {
  color: #f56c6c;
}
</style>
