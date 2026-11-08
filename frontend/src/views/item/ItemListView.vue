<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">零部件管理</span>
          <div class="toolbar">
            <el-button v-if="canWrite" type="primary" @click="openCreate">新建</el-button>
            <el-button v-if="canWrite" @click="openBatchCreate">批量新建</el-button>
            <el-button v-if="canWrite" :disabled="selection.length === 0" @click="openBatchUpdate">
              批量改属性
            </el-button>
            <el-button v-if="canWrite" :disabled="selection.length === 0" @click="openTransferOwner">
              转交所有权
            </el-button>
            <el-button v-if="canWrite" :disabled="selection.length !== 1" @click="openEdit()">编辑</el-button>
            <el-button v-if="canWrite" :disabled="selection.length !== 1" @click="onCheckout">检出</el-button>
            <el-button v-if="canWrite" :disabled="selection.length !== 1" @click="onCheckin">检入</el-button>
            <el-button v-if="canWrite" :disabled="selection.length !== 1" @click="onCancelCheckout">撤销检出</el-button>
            <el-button v-if="canDelete" :disabled="selection.length === 0" @click="onBatchDelete">删除</el-button>
            <el-tooltip content="在详情页「历史」页签选择两个版本逐字段比较" placement="bottom">
              <span>
                <el-button :disabled="selection.length !== 1" @click="onCompare">比较</el-button>
              </span>
            </el-tooltip>
            <el-button :disabled="selection.length !== 1" @click="openTab('acl')">授权</el-button>
            <el-tooltip content="发起流程在对象查看器「流程」页签中进行" placement="bottom">
              <el-button disabled>发起流程</el-button>
            </el-tooltip>
            <el-button v-if="canWrite" @click="openImport">导入</el-button>
            <el-button @click="onExport">导出</el-button>
            <ColumnSetting v-model="visibleColumns" :columns="COLUMN_DEFS" />
            <el-button @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="编号 / 名称 / 规格"
          clearable
          class="filter-input"
          @keyup.enter="search"
        />
        <el-select v-model="query.itemType" placeholder="类型" clearable class="filter-select">
          <el-option v-for="(label, value) in ITEM_TYPE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.usingState" placeholder="使用状态" clearable class="filter-select">
          <el-option
            v-for="(label, value) in ITEM_USING_STATE_LABEL"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <el-select v-model="query.sourceType" placeholder="供应方式" clearable class="filter-select">
          <el-option v-for="(label, value) in ITEM_SOURCE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="advanced = !advanced">
          {{ advanced ? '收起' : '高级查询' }}
        </el-button>
      </div>

      <div v-show="advanced" class="filters filters-advanced">
        <el-select v-model="query.phaseState" placeholder="生命周期阶段" clearable class="filter-select">
          <el-option v-for="(label, value) in ITEM_PHASE_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-select v-model="query.workflowState" placeholder="流程状态" clearable class="filter-select">
          <el-option
            v-for="(label, value) in ITEM_WORKFLOW_LABEL"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <el-input v-model="query.material" placeholder="材料" clearable class="filter-select" />
        <el-input v-model="query.specification" placeholder="规格" clearable class="filter-select" />
        <el-date-picker
          v-model="createdRange"
          type="daterange"
          unlink-panels
          start-placeholder="创建起"
          end-placeholder="创建止"
          value-format="YYYY-MM-DD"
          class="filter-range"
        />
        <el-date-picker
          v-model="releasedRange"
          type="daterange"
          unlink-panels
          start-placeholder="发布起"
          end-placeholder="发布止"
          value-format="YYYY-MM-DD"
          class="filter-range"
        />
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        border
        row-key="id"
        @row-click="openDetail"
        @selection-change="onSelectionChange"
        @header-dragend="onHeaderDragend"
      >
        <el-table-column type="selection" width="42" column-key="selection" />
        <el-table-column v-if="show('code')" column-key="code" prop="code" label="编号" :width="width('code', 150)" />
        <el-table-column
          v-if="show('name')"
          column-key="name"
          prop="name"
          label="名称"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column v-if="show('itemType')" column-key="itemType" label="类型" width="100">
          <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType }}</template>
        </el-table-column>
        <el-table-column
          v-if="show('specification')"
          column-key="specification"
          prop="specification"
          label="规格型号"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column v-if="show('material')" column-key="material" prop="material" label="材料" width="100" />
        <el-table-column v-if="show('sourceType')" column-key="sourceType" label="供应方式" width="90">
          <template #default="{ row }">
            <StatusDot
              :tone="row.sourceType === 'BUY' ? 'warn' : 'ok'"
              :label="ITEM_SOURCE_LABEL[row.sourceType] ?? row.sourceType ?? '-'"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="show('revisionLabel')" column-key="revisionLabel" prop="revisionLabel" label="版本" width="70" />
        <el-table-column v-if="show('lifecycleState')" column-key="lifecycleState" label="发布状态" width="100">
          <template #default="{ row }">
            <StatusDot :status="row.lifecycleState" :label="ITEM_LIFECYCLE_LABEL[row.lifecycleState] ?? row.lifecycleState" />
          </template>
        </el-table-column>
        <el-table-column v-if="show('phaseState')" column-key="phaseState" label="生命周期" width="90">
          <template #default="{ row }">
            <StatusDot :tone="phaseTone(row.phaseState)" :label="phaseLabel(row.phaseState)" />
          </template>
        </el-table-column>
        <el-table-column v-if="show('usingState')" column-key="usingState" label="使用状态" width="90">
          <template #default="{ row }">
            <StatusDot :tone="usingTone(row.usingState)" :label="usingLabel(row.usingState)" />
          </template>
        </el-table-column>
        <el-table-column v-if="show('workflowState')" column-key="workflowState" label="流程状态" width="90">
          <template #default="{ row }">
            <StatusDot :tone="workflowTone(row.workflowState)" :label="workflowLabel(row.workflowState)" />
          </template>
        </el-table-column>
        <el-table-column v-if="show('releasedAt')" column-key="releasedAt" label="发布时间" width="150">
          <template #default="{ row }">{{ formatTime(row.releasedAt) }}</template>
        </el-table-column>
        <el-table-column v-if="show('createdByName')" column-key="createdByName" prop="createdByName" label="创建人" width="100" />
        <el-table-column v-if="show('createdAt')" column-key="createdAt" label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column
          v-if="show('updatedByName')"
          column-key="updatedByName"
          prop="updatedByName"
          label="修改人"
          width="100"
        />
        <el-table-column v-if="show('updatedAt')" column-key="updatedAt" label="修改时间" width="150">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">打开</el-button>
            <el-button v-if="canWrite" link type="primary" @click.stop="openEdit(row)">编辑</el-button>
            <el-button v-if="canDelete" link type="danger" @click.stop="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无零部件，点击「新建」开始" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
        @size-change="search"
      />
    </el-card>

    <ItemFormDialog
      v-model="dialogVisible"
      :editing-id="editingId ? String(editingId) : null"
      @saved="onItemFormSaved"
    />

    <el-dialog v-model="classChangeVisible" title="变更分类" width="520px">
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-tree-select
            v-model="classChangeId"
            :data="classTree"
            node-key="id"
            :props="{ label: 'name', children: 'children', disabled: 'selectDisabled' }"
            check-strictly
            filterable
            placeholder="只能选最末级（小类）"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classChangeVisible = false">取消</el-button>
        <el-button type="primary" :loading="classChanging" @click="submitClassChange">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入：预演 → 确认 -->
    <el-dialog v-model="importVisible" title="导入零部件（增量合并）" width="640px">
      <div class="import-row">
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="onPickFile">
          <el-button type="primary" plain>选择文件</el-button>
        </el-upload>
        <span v-if="importFile" class="hint">{{ importFile.name }}</span>
      </div>
      <el-alert
        v-if="preview"
        :type="preview.valid ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="import-alert"
      >
        共 {{ preview.total }} 行：新建 {{ preview.createCount }} 行、更新 {{ preview.updateCount }} 行
        <span v-if="preview.errors.length">，{{ preview.errors.length }} 条提示</span>
      </el-alert>
      <ul v-if="preview && preview.errors.length" class="import-errors">
        <li v-for="(error, index) in preview.errors.slice(0, 10)" :key="index">{{ error }}</li>
        <li v-if="preview.errors.length > 10">… 其余 {{ preview.errors.length - 10 }} 条略</li>
      </ul>
      <div v-if="importResult" class="import-result">
        导入完成：新建 {{ importResult.created }}、更新 {{ importResult.updated }}、跳过
        {{ importResult.skipped }}
        <ul v-if="importResult.messages.length">
          <li v-for="(message, index) in importResult.messages.slice(0, 10)" :key="index">{{ message }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button :disabled="!importFile" @click="onPreview">预演</el-button>
        <el-button type="primary" :disabled="!importFile" :loading="importing" @click="onImport">确认导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchCreateVisible" title="批量新建零部件" width="900px">
      <el-alert type="info" :closable="false" show-icon class="dialog-alert">
        名称与类型必填，编码留空自动取号；逐行独立处理，某行失败不影响其它行。
      </el-alert>
      <el-table :data="batchCreateRows" border size="small" max-height="400">
        <el-table-column label="名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.name" placeholder="必填" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-select v-model="row.itemType">
              <el-option
                v-for="(label, value) in ITEM_TYPE_LABEL"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="规格型号" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.specification" />
          </template>
        </el-table-column>
        <el-table-column label="材料" width="120">
          <template #default="{ row }">
            <el-input v-model="row.material" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="90">
          <template #default="{ row }">
            <el-input v-model="row.unit" />
          </template>
        </el-table-column>
        <el-table-column label="供应方式" width="110">
          <template #default="{ row }">
            <el-select v-model="row.sourceType">
              <el-option
                v-for="(label, value) in ITEM_SOURCE_LABEL"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeBatchCreateRow($index)">移除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有行，点下方「添加一行」" />
        </template>
      </el-table>
      <div class="dialog-toolbar">
        <el-button @click="addBatchCreateRow">添加一行</el-button>
        <span class="hint">共 {{ batchCreateRows.length }} 行，已填名称 {{ batchCreateFilled }} 行</span>
      </div>
      <div v-if="batchCreateMsg" class="dialog-result">{{ batchCreateMsg }}</div>
      <template #footer>
        <el-button @click="batchCreateVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="batchCreating"
          :disabled="batchCreateFilled === 0"
          @click="onBatchCreate"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchUpdateVisible" title="批量改属性" width="720px">
      <el-alert type="info" :closable="false" show-icon class="dialog-alert">
        只填要改的字段，<b>留空表示保持原值</b>；将应用到已勾选的 {{ selection.length }} 个零部件。已发布对象需先修订，他人检出的为只读。
      </el-alert>
      <el-form label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="名称">
              <el-input v-model="batchPatch.name" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="batchPatch.itemType" placeholder="留空不修改" clearable>
                <el-option
                  v-for="(label, value) in ITEM_TYPE_LABEL"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="batchPatch.specification" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料">
              <el-input v-model="batchPatch.material" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位">
              <el-input v-model="batchPatch.unit" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单件重量">
              <el-input v-model="batchPatch.weight" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应方式">
              <el-select v-model="batchPatch.sourceType" placeholder="留空不修改" clearable>
                <el-option
                  v-for="(label, value) in ITEM_SOURCE_LABEL"
                  :key="value"
                  :label="label"
                  :value="value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准号">
              <el-input v-model="batchPatch.stdCode" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="batchPatch.remark" type="textarea" :rows="2" placeholder="留空不修改" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div v-if="batchUpdateMsg" class="dialog-result">{{ batchUpdateMsg }}</div>
      <template #footer>
        <el-button @click="batchUpdateVisible = false">关闭</el-button>
        <el-button type="primary" :loading="batchUpdating" @click="onBatchUpdate">应用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转交所有权" width="520px">
      <el-alert type="warning" :closable="false" show-icon class="dialog-alert">
        把已勾选 {{ selection.length }} 个零部件的属主改为接手人；仅同公司内可转交，发布状态与版本不变。
      </el-alert>
      <el-select
        v-model="transferUserId"
        filterable
        remote
        :remote-method="searchTransferUsers"
        :loading="transferUserLoading"
        placeholder="输入姓名或工号检索接手人"
        style="width: 100%"
      >
        <el-option
          v-for="user in transferUsers"
          :key="user.id"
          :label="`${user.displayName}（${user.username}）`"
          :value="user.id"
        />
      </el-select>
      <div v-if="transferMsg" class="dialog-result">{{ transferMsg }}</div>
      <template #footer>
        <el-button @click="transferVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="transferring"
          :disabled="!transferUserId"
          @click="onTransferOwner"
        >
          确认转交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import {
  ITEM_LIFECYCLE_LABEL,
  ITEM_PHASE_LABEL,
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  ITEM_USING_STATE_LABEL,
  ITEM_WORKFLOW_LABEL,
  itemApi,
  itemClassApi,
  type CodePreview,
  type ItemBatchPatch,
  type ItemClassifyInfo,
  type ItemClassNode,
  type ItemDesignAttr,
  type ItemSaveReq,
  type ItemSummary
} from '@/api/item'
import { workflowApi, type ActorUserRow } from '@/api/workflow'
import { type MetaFieldDef } from '@/api/system'
import {
  buildErpPatch,
  buildErpValues,
  isBoolField,
  loadItemCustomFields,
  pickExtValues
} from '@/utils/itemErpFields'
import { useUserStore } from '@/stores/user'
import StatusDot from '@/components/StatusDot.vue'
import ColumnSetting from '@/components/ColumnSetting.vue'
import ItemFormDialog from '@/components/business/ItemFormDialog.vue'
import type { ColumnDef } from '@/components/columnSetting'

const router = useRouter()
const userStore = useUserStore()
const canWrite = userStore.can('pse.item.write')
const canDelete = userStore.can('pse.item.delete')

/** 列定义：对齐旧系统 PSEITEM 主列表 11 列 + 新系统扩展 5 列。 */
const COLUMN_DEFS: ColumnDef[] = [
  { key: 'code', label: '编号', fixed: true },
  { key: 'name', label: '名称', fixed: true },
  { key: 'itemType', label: '类型' },
  { key: 'specification', label: '规格型号' },
  { key: 'material', label: '材料' },
  { key: 'sourceType', label: '供应方式' },
  { key: 'revisionLabel', label: '版本' },
  { key: 'lifecycleState', label: '发布状态' },
  { key: 'phaseState', label: '生命周期' },
  { key: 'usingState', label: '使用状态' },
  { key: 'workflowState', label: '流程状态' },
  { key: 'releasedAt', label: '发布时间' },
  { key: 'createdByName', label: '创建人' },
  { key: 'createdAt', label: '创建时间' },
  { key: 'updatedByName', label: '修改人' },
  { key: 'updatedAt', label: '修改时间' }
]

const rows = ref<ItemSummary[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const selection = ref<ItemSummary[]>([])
const advanced = ref(false)
const createdRange = ref<string[]>([])
const releasedRange = ref<string[]>([])

const importVisible = ref(false)
const importFile = ref<File | null>(null)
const preview = ref<{ total: number; createCount: number; updateCount: number; errors: string[]; valid: boolean } | null>(
  null
)
const importResult = ref<{ total: number; created: number; updated: number; skipped: number; messages: string[] } | null>(
  null
)
const importing = ref(false)

// ------------------------------------------------------------ 批量新建 / 批量改属性 / 转交所有权（B5）

interface BatchCreateRow {
  name: string
  itemType: string
  specification: string
  material: string
  unit: string
  sourceType: string
}

const batchCreateVisible = ref(false)
const batchCreating = ref(false)
const batchCreateRows = ref<BatchCreateRow[]>([])
const batchCreateMsg = ref('')
const batchUpdateVisible = ref(false)
const batchUpdating = ref(false)
const batchUpdateMsg = ref('')
const batchPatch = reactive({
  name: '',
  itemType: '',
  specification: '',
  material: '',
  unit: '',
  weight: '',
  sourceType: '',
  stdCode: '',
  remark: ''
})
const transferVisible = ref(false)
const transferring = ref(false)
const transferUserId = ref('')
const transferUsers = ref<ActorUserRow[]>([])
const transferUserLoading = ref(false)
const transferMsg = ref('')

const batchCreateFilled = computed(
  () => batchCreateRows.value.filter((row) => row.name.trim().length > 0).length
)

function emptyBatchRow(): BatchCreateRow {
  return { name: '', itemType: 'PART', specification: '', material: '', unit: 'EA', sourceType: 'MAKE' }
}

const COLUMN_STORAGE_KEY = 'plm.item.columns'
const WIDTH_STORAGE_KEY = 'plm.item.colWidths'
const DEFAULT_COLUMNS = COLUMN_DEFS.map((column) => column.key)

const visibleColumns = ref<string[]>(loadColumns())
const colWidths = ref<Record<string, number>>(loadWidths())

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  itemType: '',
  sourceType: '',
  usingState: '',
  phaseState: '',
  workflowState: '',
  material: '',
  specification: ''
})

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

// 设计属性在弹窗里始终有对象（避免模板里到处判空）
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

// ---------------- 分类 → 编码联动（T-CORE-12 / T-CORE-13） ----------------
// 成品库（80）与产号（CH）不属于物料分类，选择器里剔除
const EXCLUDED_CLASS_ROOTS = ['80', 'CH']

interface SelectableClassNode extends ItemClassNode {
  /** 有子节点 = 大类/中类，不允许选（只能选最末级小类） */
  selectDisabled: boolean
}

function toSelectableClassTree(nodes: ItemClassNode[]): SelectableClassNode[] {
  return nodes
    .filter((node) => !EXCLUDED_CLASS_ROOTS.includes(node.code))
    .map((node) => ({
      ...node,
      selectDisabled: (node.children?.length ?? 0) > 0,
      children: toSelectableClassTree(node.children ?? [])
    }))
}

const classTree = ref<SelectableClassNode[]>([])
const projectOptions = ref<Array<{ id: string; code: string; name: string }>>([])

async function loadProjectOptions(): Promise<void> {
  try {
    projectOptions.value = await itemApi.projectOptions()
  } catch {
    projectOptions.value = []
  }
}

// 绑定项目后，位置按「个人空间/{项目编码}/物料」自动生成
watch(
  () => form.projectId,
  (id) => {
    const hit = projectOptions.value.find((item) => String(item.id) === String(id))
    if (hit) {
      form.location = `个人空间/${hit.code}/物料`
    }
  }
)
const codePreview = ref<CodePreview | null>(null)
const previewLoading = ref(false)
let previewTimer: ReturnType<typeof setTimeout> | null = null

/** 新建时编码框只读展示预览值；编辑时展示已有编码。 */
const displayCode = computed(() =>
  editingId.value ? form.code ?? '' : codePreview.value?.code ?? ''
)

async function loadClassTree(): Promise<void> {
  try {
    classTree.value = toSelectableClassTree(await itemClassApi.tree())
  } catch {
    classTree.value = []
  }
}

/** 按当前分类/类型预览将生成的编码（不消耗流水）。 */
async function refreshCodePreview(): Promise<void> {
  codePreview.value = null
  if (editingId.value) {
    return
  }
  previewLoading.value = true
  try {
    codePreview.value = await itemApi.previewCode({
      classId: form.classId || undefined,
      itemType: form.itemType,
      dwgNo: form.designAttr.dwgNo?.trim() || undefined
    })
  } catch {
    codePreview.value = null
  } finally {
    previewLoading.value = false
  }
}

// ---------------- 编辑时的分类展示与变更 ----------------
const editClassify = ref<ItemClassifyInfo | null>(null)
const classChangeVisible = ref(false)
const classChangeId = ref<string | null>(null)
const classChanging = ref(false)

async function loadEditClassify(id: string): Promise<void> {
  try {
    editClassify.value = await itemClassApi.info(id)
  } catch {
    editClassify.value = null
  }
}

function openClassChange(): void {
  classChangeId.value = editClassify.value?.classId ?? null
  classChangeVisible.value = true
}

async function submitClassChange(): Promise<void> {
  if (!editingId.value) {
    return
  }
  if (!classChangeId.value) {
    ElMessage.warning('请选择最末级分类')
    return
  }
  classChanging.value = true
  try {
    await itemClassApi.assign(editingId.value, classChangeId.value, {})
    ElMessage.success('已变更分类')
    classChangeVisible.value = false
    await loadEditClassify(editingId.value)
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message || '变更分类失败')
  } finally {
    classChanging.value = false
  }
}

function schedulePreview(): void {
  if (previewTimer) {
    clearTimeout(previewTimer)
  }
  previewTimer = setTimeout(() => {
    refreshCodePreview()
  }, 250)
}

watch(() => [form.classId, form.itemType, form.designAttr.dwgNo], schedulePreview)

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// ------------------------------------------------------------ ERP 客制字段（批次 G·方案 A / T-G-02）

/** 客制字段集合（元数据 − 标准键 − 设计属性键），实现与详情页共用 utils/itemErpFields。 */
const customFields = ref<MetaFieldDef[]>([])
const extTextForm = reactive<Record<string, string>>({})
/** 布尔三态：'' = 未设置（新建不提交 / 编辑提交空串清空）。 */
const extBoolForm = reactive<Record<string, boolean | ''>>({})
/** 编辑时的原始 ext_json 快照，用于"只提交改动过的键"。 */
const extOriginal = ref<Record<string, unknown>>({})

async function loadCustomFields(): Promise<void> {
  customFields.value = await loadItemCustomFields()
}

function resetExtForms(source?: Record<string, unknown>): void {
  const values = pickExtValues(customFields.value, source)
  Object.keys(extTextForm).forEach((key) => delete extTextForm[key])
  Object.keys(extBoolForm).forEach((key) => delete extBoolForm[key])
  Object.assign(extTextForm, values.text)
  Object.assign(extBoolForm, values.bool)
}

function loadColumns(): string[] {
  try {
    const raw = localStorage.getItem(COLUMN_STORAGE_KEY)
    if (!raw) {
      return [...DEFAULT_COLUMNS]
    }
    const parsed = JSON.parse(raw) as string[]
    const valid = parsed.filter((key) => DEFAULT_COLUMNS.includes(key))
    return valid.length ? valid : [...DEFAULT_COLUMNS]
  } catch {
    return [...DEFAULT_COLUMNS]
  }
}

function loadWidths(): Record<string, number> {
  try {
    const raw = localStorage.getItem(WIDTH_STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Record<string, number>) : {}
  } catch {
    return {}
  }
}

function show(key: string): boolean {
  return visibleColumns.value.includes(key)
}

function width(key: string, fallback: number): number {
  return colWidths.value[key] ?? fallback
}

function onHeaderDragend(newWidth: number, _oldWidth: number, column: { columnKey?: string }): void {
  const key = column?.columnKey
  if (!key) {
    return
  }
  colWidths.value = { ...colWidths.value, [key]: Math.round(newWidth) }
  localStorage.setItem(WIDTH_STORAGE_KEY, JSON.stringify(colWidths.value))
}

function phaseLabel(value?: string | null): string {
  return value ? ITEM_PHASE_LABEL[value] ?? value : '未定义'
}

function phaseTone(value?: string | null): 'warn' | 'running' | 'ok' | 'muted' {
  if (value === 'S') {
    return 'warn'
  }
  if (value === 'A') {
    return 'running'
  }
  if (value === 'B') {
    return 'ok'
  }
  return 'muted'
}

function usingLabel(value?: string | null): string {
  return ITEM_USING_STATE_LABEL[value ?? ''] ?? (value || '在用')
}

function usingTone(value?: string | null): 'ok' | 'warn' | 'muted' {
  if (value === 'StopUsing') {
    return 'warn'
  }
  if (value === 'GiveUp') {
    return 'muted'
  }
  return 'ok'
}

function workflowLabel(value?: string | null): string {
  return ITEM_WORKFLOW_LABEL[value ?? ''] ?? (value || '新建')
}

function workflowTone(value?: string | null): 'ok' | 'warn' | 'danger' | 'running' | 'muted' {
  if (value === 'Approved' || value === 'Published') {
    return 'ok'
  }
  if (value === 'Rejected') {
    return 'danger'
  }
  if (value === 'Running') {
    return 'running'
  }
  return 'muted'
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await itemApi.list({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      itemType: query.itemType || undefined,
      sourceType: query.sourceType || undefined,
      usingState: query.usingState || undefined,
      phaseState: query.phaseState || undefined,
      workflowState: query.workflowState || undefined,
      material: query.material || undefined,
      specification: query.specification || undefined,
      createdFrom: createdRange.value?.[0] || undefined,
      createdTo: createdRange.value?.[1] || undefined,
      releasedFrom: releasedRange.value?.[0] || undefined,
      releasedTo: releasedRange.value?.[1] || undefined
    })
    rows.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function search(): void {
  query.page = 1
  load()
}

function resetQuery(): void {
  query.keyword = ''
  query.itemType = ''
  query.sourceType = ''
  query.usingState = ''
  query.phaseState = ''
  query.workflowState = ''
  query.material = ''
  query.specification = ''
  createdRange.value = []
  releasedRange.value = []
  search()
}

function onSelectionChange(items: ItemSummary[]): void {
  selection.value = items
}

function openCreate(): void {
  editingId.value = null
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
  form.classId = null
  form.location = ''
  form.projectId = null
  editClassify.value = null
  form.designAttr = emptyDesignAttr()
  // 新建：客制字段清空（布尔一律「未设置」，不预置 false）
  extOriginal.value = {}
  resetExtForms()
  dialogVisible.value = true
}

async function openEdit(row?: ItemSummary): Promise<void> {
  const target = row ?? selection.value[0]
  if (!target) {
    return
  }
  try {
    const detail = await itemApi.detail(target.id)
    editingId.value = detail.id
    form.code = detail.code
    form.name = detail.name
    form.itemType = detail.itemType
    form.sourceType = detail.sourceType ?? 'MAKE'
    form.specification = detail.specification ?? ''
    form.material = detail.material ?? ''
    form.unit = detail.unit ?? 'EA'
    form.weight = detail.weight ?? null
    form.stdCode = detail.stdCode ?? ''
    form.remark = detail.remark ?? ''
    // ItemDetail 已返回 location（V85）；类型定义稍后同步，这里用结构化断言拿到值
    form.location = (detail as unknown as { location?: string | null }).location ?? ''
    form.designAttr = { ...emptyDesignAttr(), ...(detail.designAttr ?? {}) }
    extOriginal.value = { ...(detail.ext ?? {}) }
    resetExtForms(detail.ext ?? {})
    // 分类只读展示：变更走独立入口（PUT /items/{id}/classify）
    await loadEditClassify(detail.id)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

async function onSave(keepOpen = false): Promise<void> {
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
      // 编辑：只提交改动过的客制字段，未改动由后端保留
      const ext = buildErpPatch(customFields.value, extTextForm, extBoolForm, extOriginal.value)
      await itemApi.update(editingId.value, { ...form, ext })
      ElMessage.success('已保存')
    } else {
      // 新建：提交用户填写的全部值（布尔仅在显式选择时提交）
      const ext = buildErpValues(customFields.value, extTextForm, extBoolForm)
      // 图号生成的编号要随请求提交（后端按人工编码校验后采用），否则留空由分类取号
      const drawing = form.designAttr.dwgNo?.trim()
      const code = drawing && codePreview.value ? codePreview.value.code : undefined
      await itemApi.create({ ...form, code, ext })
      ElMessage.success('已创建')
    }
    if (keepOpen) {
      // 连续录入：保留类型/分类/单位，清空名称与图号后重新预览
      form.name = ''
      form.designAttr.dwgNo = ''
      codePreview.value = null
      schedulePreview()
    } else {
      dialogVisible.value = false
    }
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onItemFormSaved(): Promise<void> {
  await load()
}

async function onDelete(row: ItemSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除零部件「${row.code} ${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await itemApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function onBatchDelete(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 个零部件？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    const result = await itemApi.batchDelete(selection.value.map((row) => row.id))
    ElMessage.success(`删除完成：成功 ${result.done}，跳过 ${result.skipped}`)
    if (result.messages.length) {
      ElMessage.warning(result.messages.slice(0, 3).join('；'))
    }
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量删除失败')
  }
}

/** 版本比较入口：跳详情页「历史」页签，由那里勾选两个版本（列表只展示最新版，无法两两比较）。 */
function onCompare(): void {
  const target = selection.value[0]
  if (!target) {
    return
  }
  router.push({ name: 'item-detail', params: { id: target.id }, query: { tab: 'versions' } })
}

// ------------------------------------------------------------ 批量新建

function openBatchCreate(): void {
  batchCreateRows.value = [emptyBatchRow(), emptyBatchRow(), emptyBatchRow()]
  batchCreateMsg.value = ''
  batchCreateVisible.value = true
}

function addBatchCreateRow(): void {
  batchCreateRows.value.push(emptyBatchRow())
}

function removeBatchCreateRow(index: number): void {
  batchCreateRows.value.splice(index, 1)
}

async function onBatchCreate(): Promise<void> {
  const items: ItemSaveReq[] = batchCreateRows.value
    .filter((row) => row.name.trim().length > 0)
    .map((row) => ({
      name: row.name.trim(),
      itemType: row.itemType,
      specification: row.specification.trim() || undefined,
      material: row.material.trim() || undefined,
      unit: row.unit.trim() || undefined,
      sourceType: row.sourceType
    }))
  if (!items.length) {
    ElMessage.warning('请至少填写一行名称')
    return
  }
  batchCreating.value = true
  try {
    const result = await itemApi.batchCreate(items)
    batchCreateMsg.value = `提交 ${result.total} 行：成功 ${result.done}，跳过 ${result.skipped}${
      result.messages.length ? '；' + result.messages.slice(0, 5).join('；') : ''
    }`
    if (result.done > 0) {
      await load()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量新建失败')
  } finally {
    batchCreating.value = false
  }
}

// ------------------------------------------------------------ 批量改属性

function openBatchUpdate(): void {
  Object.assign(batchPatch, {
    name: '',
    itemType: '',
    specification: '',
    material: '',
    unit: '',
    weight: '',
    sourceType: '',
    stdCode: '',
    remark: ''
  })
  batchUpdateMsg.value = ''
  batchUpdateVisible.value = true
}

async function onBatchUpdate(): Promise<void> {
  const patch: ItemBatchPatch = {}
  if (batchPatch.name.trim()) {
    patch.name = batchPatch.name.trim()
  }
  if (batchPatch.itemType) {
    patch.itemType = batchPatch.itemType
  }
  if (batchPatch.specification.trim()) {
    patch.specification = batchPatch.specification.trim()
  }
  if (batchPatch.material.trim()) {
    patch.material = batchPatch.material.trim()
  }
  if (batchPatch.unit.trim()) {
    patch.unit = batchPatch.unit.trim()
  }
  if (batchPatch.sourceType) {
    patch.sourceType = batchPatch.sourceType
  }
  if (batchPatch.stdCode.trim()) {
    patch.stdCode = batchPatch.stdCode.trim()
  }
  if (batchPatch.remark.trim()) {
    patch.remark = batchPatch.remark.trim()
  }
  const weight = Number(batchPatch.weight)
  if (batchPatch.weight.trim() && Number.isFinite(weight)) {
    patch.weight = weight
  }
  if (!Object.keys(patch).length) {
    ElMessage.warning('请至少填写一个要修改的字段')
    return
  }
  batchUpdating.value = true
  try {
    const result = await itemApi.batchUpdate(
      selection.value.map((row) => row.id),
      patch
    )
    batchUpdateMsg.value = `应用 ${result.total} 个：成功 ${result.done}，跳过 ${result.skipped}${
      result.messages.length ? '；' + result.messages.slice(0, 5).join('；') : ''
    }`
    if (result.done > 0) {
      await load()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量改属性失败')
  } finally {
    batchUpdating.value = false
  }
}

// ------------------------------------------------------------ 转交所有权

function openTransferOwner(): void {
  transferUserId.value = ''
  transferMsg.value = ''
  transferVisible.value = true
  transferUsers.value = []
  if (selection.value.length) {
    searchTransferUsers('')
  }
}

async function searchTransferUsers(keyword: string): Promise<void> {
  transferUserLoading.value = true
  try {
    transferUsers.value = await workflowApi.actorUsers(keyword, 20)
  } catch {
    transferUsers.value = []
  } finally {
    transferUserLoading.value = false
  }
}

async function onTransferOwner(): Promise<void> {
  if (!transferUserId.value) {
    return
  }
  transferring.value = true
  try {
    const result = await itemApi.transferOwner(
      selection.value.map((row) => row.id),
      transferUserId.value
    )
    transferMsg.value = `转交 ${result.total} 个：成功 ${result.done}，跳过 ${result.skipped}${
      result.messages.length ? '；' + result.messages.slice(0, 5).join('；') : ''
    }`
    if (result.done > 0) {
      await load()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '转交所有权失败')
  } finally {
    transferring.value = false
  }
}

async function onCheckout(): Promise<void> {
  const target = selection.value[0]
  if (!target) {
    return
  }
  try {
    await itemApi.checkout(target.id)
    ElMessage.success('已检出，可编辑')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检出失败')
  }
}

async function onCheckin(): Promise<void> {
  const target = selection.value[0]
  if (!target) {
    return
  }
  try {
    const detail = await itemApi.checkin(target.id)
    ElMessage.success(`已检入，新版本 ${detail.revisionLabel}`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检入失败')
  }
}

async function onCancelCheckout(): Promise<void> {
  const target = selection.value[0]
  if (!target) {
    return
  }
  try {
    await itemApi.cancelCheckout(target.id)
    ElMessage.success('已撤销检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤销检出失败')
  }
}

async function onExport(): Promise<void> {
  try {
    await itemApi.exportFile({
      keyword: query.keyword || undefined,
      itemType: query.itemType || undefined,
      sourceType: query.sourceType || undefined,
      usingState: query.usingState || undefined,
      phaseState: query.phaseState || undefined,
      workflowState: query.workflowState || undefined
    })
    ElMessage.success('已导出')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

function openImport(): void {
  importFile.value = null
  preview.value = null
  importResult.value = null
  importVisible.value = true
}

function onPickFile(file: UploadFile): void {
  importFile.value = file.raw ?? null
  preview.value = null
  importResult.value = null
}

async function downloadTemplate(): Promise<void> {
  try {
    await itemApi.downloadTemplate()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载模板失败')
  }
}

async function onPreview(): Promise<void> {
  if (!importFile.value) {
    return
  }
  try {
    preview.value = await itemApi.previewImport(importFile.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预演失败')
  }
}

async function onImport(): Promise<void> {
  if (!importFile.value) {
    return
  }
  importing.value = true
  try {
    importResult.value = await itemApi.importItems(importFile.value)
    ElMessage.success(
      `导入完成：新建 ${importResult.value.created}、更新 ${importResult.value.updated}、跳过 ${importResult.value.skipped}`
    )
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    importing.value = false
  }
}

function openDetail(row: ItemSummary): void {
  router.push({ name: 'item-detail', params: { id: row.id } })
}

function openTab(tab: string): void {
  const target = selection.value[0]
  if (!target) {
    return
  }
  router.push({ name: 'item-detail', params: { id: target.id }, query: { tab } })
}

onMounted(() => {
  load()
  // 客制字段集合一次取够（与详情页共用同一实现）
  loadCustomFields()
  // 分类树：新建时选分类即可实时预览编码
  loadClassTree()
  loadProjectOptions()
  applyQueryAction()
})

const route = useRoute()

/** 供工作空间等页面复用：/item?create=1&projectId=xxx 或 /item?edit=id */
async function applyQueryAction(): Promise<void> {
  const create = route.query.create === '1'
  const editId = typeof route.query.edit === 'string' ? route.query.edit : ''
  const projectId = typeof route.query.projectId === 'string' ? route.query.projectId : ''
  if (!create && !editId) {
    return
  }
  await router.replace({ path: '/item' })
  if (create) {
    openCreate()
    if (projectId) {
      form.projectId = projectId
    }
    return
  }
  await openEdit({ id: editId } as ItemSummary)
}
</script>

<style scoped>
/* 只读编码框：加深灰底 + 禁止光标，明确"不可编辑" */
.code-readonly :deep(.el-input__wrapper) {
  background-color: var(--el-fill-color-dark, #e6e8eb);
  box-shadow: 0 0 0 1px var(--el-border-color-darker, #c7cbd1) inset;
}

.code-readonly :deep(.el-input__inner) {
  color: var(--el-text-color-regular, #303133);
  font-family: var(--el-font-family-mono, monospace);
  cursor: not-allowed;
}

/* 编码预览：手工编码用警示色，自动取号用主色高亮 */
.code-preview {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.code-preview.is-manual {
  color: var(--el-color-warning);
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

.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-weight: 600;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.toolbar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.filters-advanced {
  padding-top: 8px;
  border-top: 1px dashed var(--plm-border, #dde3ea);
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 140px;
}

.filter-range {
  width: 260px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.form-section {
  margin: 4px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--plm-primary, #1f5c8b);
  font-weight: 600;
  font-size: 13px;
}

.form-hint {
  margin: -4px 0 8px 100px;
  color: #909399;
  font-size: 12px;
}

.import-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-alert {
  margin-bottom: 12px;
}

.dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.dialog-result {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}

.import-alert {
  margin-top: 10px;
}

.import-errors,
.import-result ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--plm-danger, #d64541);
  font-size: 12px;
}

.import-result {
  margin-top: 10px;
  font-size: 12px;
}

.hint {
  color: var(--plm-text-secondary, #5b6778);
  font-size: 12px;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
