<script setup lang="ts">
/**
 * 材料定额（公式库）。对齐旧系统 Formula：一个公式 = 类型 + 表达式 + 参数表（para_Grid）。
 * 表达式只支持四则运算、括号、参数名与 min/max/round/ceil/floor —— 由后端解析器保证，
 * 前端不做二次计算，试算一律走后端，避免两边算法分叉。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { formulaApi } from '@/api/formula'
import type { Formula, FormulaParam } from '@/api/formula'

const TYPES = [
  { value: 'MATERIAL', label: '材料定额' },
  { value: 'TIME', label: '工时定额' },
  { value: 'OTHER', label: '其他' }
]

const loading = ref(false)
const rows = ref<Formula[]>([])
const keyword = ref('')
const typeFilter = ref('')

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<Formula | null>(null)
const form = ref({
  code: '',
  name: '',
  formulaType: 'MATERIAL',
  expression: '',
  resultUnit: '',
  explain: '',
  enabled: true,
  params: [] as FormulaParam[]
})

const evalVisible = ref(false)
const evalTarget = ref<Formula | null>(null)
const evalValues = ref<Record<string, number>>({})
const evalResult = ref<string | null>(null)
const evalError = ref<string | null>(null)

const isEdit = computed(() => !!editing.value)

async function load() {
  loading.value = true
  try {
    rows.value = await formulaApi.list(keyword.value || undefined, typeFilter.value || undefined)
  } catch (error) {
    ElMessage.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = {
    code: '',
    name: '',
    formulaType: 'MATERIAL',
    expression: '',
    resultUnit: 'kg',
    explain: '',
    enabled: true,
    params: []
  }
  dialogVisible.value = true
}

function openEdit(row: Formula) {
  editing.value = row
  form.value = {
    code: row.code,
    name: row.name,
    formulaType: row.formulaType,
    expression: row.expression,
    resultUnit: row.resultUnit || '',
    explain: row.explain || '',
    enabled: row.enabled,
    params: row.params.map((p) => ({ ...p }))
  }
  dialogVisible.value = true
}

function addParam() {
  form.value.params.push({ name: '', label: '', defaultValue: 0, unit: '' })
}

function removeParam(index: number) {
  form.value.params.splice(index, 1)
}

async function save() {
  if (!form.value.name.trim() || !form.value.expression.trim()) {
    ElMessage.warning('名称与表达式必填')
    return
  }
  saving.value = true
  try {
    const body = {
      code: form.value.code || undefined,
      name: form.value.name,
      formulaType: form.value.formulaType,
      expression: form.value.expression,
      resultUnit: form.value.resultUnit || undefined,
      explain: form.value.explain || undefined,
      enabled: form.value.enabled,
      params: form.value.params.map((p) => ({
        name: p.name,
        label: p.label || undefined,
        defaultValue: p.defaultValue ?? null,
        unit: p.unit || undefined
      }))
    }
    if (isEdit.value && editing.value) {
      await formulaApi.update(editing.value.id, body)
    } else {
      await formulaApi.create(body)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败（表达式语法错误也会在这里提示）')
  } finally {
    saving.value = false
  }
}

async function remove(row: Formula) {
  await ElMessageBox.confirm(`删除公式「${row.code} ${row.name}」？`, '删除', { type: 'warning' })
  await formulaApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

function openEvaluate(row: Formula) {
  evalTarget.value = row
  evalValues.value = {}
  for (const param of row.params) {
    evalValues.value[param.name] = Number(param.defaultValue ?? 0)
  }
  evalResult.value = null
  evalError.value = null
  evalVisible.value = true
}

async function runEvaluate() {
  if (!evalTarget.value) return
  evalResult.value = null
  evalError.value = null
  try {
    const result = await formulaApi.evaluate(evalTarget.value.id, evalValues.value)
    evalResult.value = `${result.display} ${evalTarget.value.resultUnit || ''}`
  } catch (error) {
    evalError.value = (error as Error).message || '试算失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="formula-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="title">材料定额</span>
            <span class="hint">表达式 + 参数；只支持四则运算与 min/max/round/ceil/floor</span>
          </div>
          <div class="filters">
            <el-input v-model="keyword" placeholder="编码/名称" clearable style="width: 180px" @keyup.enter="load" />
            <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width: 140px" @change="load">
              <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-button :icon="Refresh" @click="load">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreate">新建公式</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" size="small" border empty-text="还没有公式">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            {{ TYPES.find((t) => t.value === row.formulaType)?.label || row.formulaType }}
          </template>
        </el-table-column>
        <el-table-column prop="expression" label="表达式" min-width="260">
          <template #default="{ row }">
            <code class="expr">{{ row.expression }}</code>
          </template>
        </el-table-column>
        <el-table-column label="参数" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="p in row.params" :key="p.name" size="small" effect="plain" class="param-tag">
              {{ p.label || p.name }}
            </el-tag>
            <span v-if="!row.params.length" class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="resultUnit" label="单位" width="80" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEvaluate(row)">试算</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公式' : '新建公式'" width="720px">
      <el-form label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="留空自动取号（FM-xxxxxx）" style="width: 220px" />
          <el-select v-model="form.formulaType" style="width: 150px; margin-left: 12px">
            <el-option v-for="t in TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="form.expression" placeholder="如 (gross - net) / net * 100" />
        </el-form-item>
        <el-form-item label="结果单位">
          <el-input v-model="form.resultUnit" style="width: 140px" placeholder="如 kg / %" />
          <el-switch v-model="form.enabled" active-text="启用" style="margin-left: 16px" />
        </el-form-item>
        <el-form-item label="参数">
          <div class="param-editor">
            <div v-for="(param, index) in form.params" :key="index" class="param-row">
              <el-input v-model="param.name" placeholder="参数名（英文）" style="width: 150px" />
              <el-input v-model="param.label" placeholder="显示名" style="width: 150px" />
              <el-input-number v-model="param.defaultValue" placeholder="默认值" style="width: 150px" />
              <el-input v-model="param.unit" placeholder="单位" style="width: 90px" />
              <el-button link type="danger" @click="removeParam(index)">删除</el-button>
            </div>
            <el-button size="small" :icon="Plus" @click="addParam">添加参数</el-button>
          </div>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.explain" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evalVisible" :title="`试算：${evalTarget?.code ?? ''} ${evalTarget?.name ?? ''}`" width="520px">
      <el-form label-width="110px">
        <el-form-item v-for="param in evalTarget?.params || []" :key="param.name" :label="param.label || param.name">
          <el-input-number v-model="evalValues[param.name]" :precision="4" style="width: 200px" />
          <span class="muted" style="margin-left: 8px">{{ param.unit || '' }}</span>
        </el-form-item>
        <el-form-item label="表达式">
          <code class="expr">{{ evalTarget?.expression }}</code>
        </el-form-item>
        <el-form-item v-if="evalResult" label="结果">
          <el-tag type="success" effect="dark">{{ evalResult }}</el-tag>
        </el-form-item>
        <el-form-item v-if="evalError" label="错误">
          <span class="error">{{ evalError }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalVisible = false">关闭</el-button>
        <el-button type="primary" @click="runEvaluate">试算</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.formula-view {
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
  margin-right: 12px;
}

.card-header .hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expr {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--el-color-primary);
}

.param-tag {
  margin-right: 4px;
}

.muted {
  color: var(--plm-text-secondary);
  font-size: 12px;
}

.param-editor {
  width: 100%;
}

.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.error {
  color: var(--el-color-danger);
  font-size: 12px;
}
</style>
