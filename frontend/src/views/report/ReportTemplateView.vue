<script setup lang="ts">
/**
 * 报表管理（复刻批次 D）。
 *
 * 模板 = 数据集（服务端白名单）+ 参数勾选，不含 SQL：因此这里的新建/编辑只是
 * 「选哪张表、给哪些筛选条件起名字」。5 张内置报表全局只读，可自建同数据集的新模板。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, VideoPlay } from '@element-plus/icons-vue'
import {
  REPORT_CATEGORY_LABEL,
  reportApi,
  type ReportDataset,
  type ReportParamMeta,
  type ReportTemplateRow
} from '@/api/report'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const canWrite = computed(() => userStore.can('ana.report.write'))

const loading = ref(false)
const rows = ref<ReportTemplateRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const datasetFilter = ref('')
const datasets = ref<ReportDataset[]>([])

const datasetOptions = computed(() =>
  datasets.value.map((item) => ({
    value: item.key,
    label: `${item.label}（${item.key}）`
  }))
)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({
  code: '',
  name: '',
  dataset: '',
  category: 'OTHER',
  description: '',
  sortNo: 100,
  paramKeys: [] as string[]
})

/** 当前数据集可选的参数（类型与选项由后端字典给出，前端只做勾选）。 */
const paramCandidates = computed<ReportParamMeta[]>(
  () => datasets.value.find((item) => item.key === form.dataset)?.params ?? []
)

onMounted(async () => {
  await loadDatasets()
  await load()
})

async function loadDatasets() {
  try {
    datasets.value = await reportApi.datasets()
  } catch (error) {
    ElMessage.error((error as Error).message || '加载数据集失败')
  }
}

async function load() {
  loading.value = true
  try {
    const result = await reportApi.page({
      keyword: keyword.value || undefined,
      dataset: datasetFilter.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error((error as Error).message || '加载报表模板失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  if (!datasets.value.length) {
    ElMessage.warning('数据集字典为空，请先确认后端已启用报表模块')
    return
  }
  editingId.value = null
  Object.assign(form, {
    code: '',
    name: '',
    dataset: datasets.value[0].key,
    category: 'OTHER',
    description: '',
    sortNo: 100,
    paramKeys: datasets.value[0].params.map((item) => item.key)
  })
  dialogVisible.value = true
}

async function openEdit(row: ReportTemplateRow) {
  if (row.builtIn) {
    ElMessage.warning('内置报表只读，可「另建」一张同数据集模板后修改')
    return
  }
  editingId.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    dataset: row.dataset,
    category: row.category || 'OTHER',
    description: row.description || '',
    sortNo: row.sortNo,
    paramKeys: row.params.map((item) => item.key)
  })
  dialogVisible.value = true
}

/** 换数据集时清掉旧参数勾选，避免提交一个该数据集不存在的键（后端会 400）。 */
function onDatasetChange() {
  form.paramKeys = paramCandidates.value.map((item) => item.key)
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写报表名称')
    return
  }
  if (!form.dataset) {
    ElMessage.warning('请选择数据集')
    return
  }
  saving.value = true
  try {
    const body = {
      code: form.code.trim() || undefined,
      name: form.name.trim(),
      dataset: form.dataset,
      category: form.category,
      description: form.description.trim() || undefined,
      sortNo: form.sortNo,
      params: paramCandidates.value
        .filter((item) => form.paramKeys.includes(item.key))
        .map((item) => ({ key: item.key, label: item.label }))
    }
    if (editingId.value) {
      await reportApi.update(editingId.value, body)
      ElMessage.success('已保存')
    } else {
      const created = await reportApi.create(body)
      ElMessage.success(`已创建（编码 ${created.code}）`)
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: ReportTemplateRow) {
  if (row.builtIn) {
    ElMessage.warning('内置报表不可删除')
    return
  }
  await ElMessageBox.confirm(`删除报表「${row.name}」？`, '删除', { type: 'warning' })
  try {
    await reportApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '删除失败')
  }
}

function run(row: ReportTemplateRow) {
  router.push({ name: 'report-run', params: { id: row.id } })
}

function categoryLabel(value: string) {
  return REPORT_CATEGORY_LABEL[value] || value || '-'
}
</script>

<template>
  <div class="report-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="title">报表管理</span>
            <span class="hint">
              模板只引用服务端白名单数据集（不支持裸 SQL），内置 {{ datasets.length }} 个数据集
            </span>
          </div>
          <div class="filters">
            <el-input
              v-model="keyword"
              placeholder="编码 / 名称"
              clearable
              style="width: 180px"
              @keyup.enter="search"
            />
            <el-select
              v-model="datasetFilter"
              placeholder="全部数据集"
              clearable
              style="width: 200px"
              @change="search"
            >
              <el-option
                v-for="item in datasetOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button :icon="Refresh" @click="load">刷新</el-button>
            <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新建报表</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border size="small" empty-text="暂无报表">
        <el-table-column prop="code" label="编码" width="190" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="datasetLabel" label="数据集" width="150" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="参数" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="!row.params?.length" class="muted">无参数</span>
            <span v-else>{{ row.params.map((item: ReportParamMeta) => item.label).join(' / ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.builtIn ? 'info' : 'success'">
              {{ row.builtIn ? '内置' : '自建' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="80" align="center" />
        <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="VideoPlay" @click="run(row)">运行</el-button>
            <el-button v-if="!row.builtIn" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!row.builtIn" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑报表' : '新建报表'"
      width="680px"
    >
      <el-form label-width="96px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" placeholder="如：已发布文档台账" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" placeholder="留空自动取号（RPT-000001）" />
        </el-form-item>
        <el-form-item label="数据集" required>
          <el-select v-model="form.dataset" filterable style="width: 100%" @change="onDatasetChange">
            <el-option
              v-for="item in datasetOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 180px">
            <el-option
              v-for="(label, value) in REPORT_CATEGORY_LABEL"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
          <span class="form-gap">排序</span>
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" :controls="false" style="width: 110px" />
        </el-form-item>
        <el-form-item label="参数">
          <div v-if="!paramCandidates.length" class="muted">该数据集没有可配置参数</div>
          <el-checkbox-group v-else v-model="form.paramKeys" class="param-group">
            <el-checkbox v-for="item in paramCandidates" :key="item.key" :value="item.key">
              {{ item.label }}
              <span class="muted">（{{ item.type }}）</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="口径提示：报表不另立口径——文档只取最新版本、工时只统计已审批、工艺只统计本公司未删除数据。"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-page {
  padding: 4px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
  margin-right: 12px;
}

.hint {
  font-size: 12px;
  color: var(--plm-text-secondary);
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.muted {
  color: var(--plm-text-secondary);
  font-size: 12px;
}

.form-gap {
  margin: 0 8px 0 16px;
  color: var(--plm-text-secondary);
}

.param-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}
</style>
