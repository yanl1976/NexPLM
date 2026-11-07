<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="title">
            <el-button link @click="goBack">← 返回</el-button>
            <span class="code">{{ detail?.code }}</span>
            <span class="name">{{ detail?.name }}</span>
            <el-tag size="small" type="info">{{ detail?.revisionLabel }}</el-tag>
            <StatusDot :status="detail?.lifecycleState" :label="stateLabel(detail?.lifecycleState)" />
            <StatusDot v-if="!detail?.isLatest" tone="warn" label="历史版本" />
          </div>
          <div>
            <el-button @click="openPreview()">预览</el-button>
            <el-button v-if="canWrite" @click="openEdit">编辑属性</el-button>
            <el-button v-if="canWrite && canEdit && !detail?.checkout" type="primary" @click="onCheckout">
              检出
            </el-button>
            <el-button v-if="isMyCheckout" type="success" @click="onCheckin">检入</el-button>
            <el-button v-if="isMyCheckout" @click="onCancelCheckout">取消检出</el-button>
            <el-button v-if="canForceUnlock" type="warning" @click="onForceUnlock">强制解锁</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="detail?.checkout"
        class="checkout-tip"
        type="warning"
        :closable="false"
        show-icon
        :title="`已由 ${detail.checkout.userLabel || detail.checkout.userId} 检出，到期时间 ${formatTime(detail.checkout.expireAt)}`"
      />

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.code" :label="tab.name" :name="tab.code">
          <el-descriptions v-if="tab.tabType === 'PROPS'" :column="2" border>
            <el-descriptions-item label="编码">{{ detail?.code }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detail?.name }}</el-descriptions-item>
            <el-descriptions-item label="文档类型">{{ typeLabel(detail?.docType) }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ sourceLabel(detail?.source) }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail?.revisionLabel }}</el-descriptions-item>
            <el-descriptions-item label="生命周期">{{ stateLabel(detail?.lifecycleState) }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail?.className || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作流">{{ detail?.workflowState || 'NONE' }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ detail?.keyword || '-' }}</el-descriptions-item>
            <el-descriptions-item label="页数">{{ detail?.pageCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="文件数">{{ detail?.fileCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="主文件扩展名">{{ detail?.fileExt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(detail?.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(detail?.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ detail?.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <template v-if="classAttrEntries.length > 0">
            <el-divider content-position="left">分类属性</el-divider>
            <el-descriptions :column="2" border>
              <el-descriptions-item v-for="item in classAttrEntries" :key="item.code" :label="item.label">
                {{ item.value }}
              </el-descriptions-item>
            </el-descriptions>
          </template>

          <template v-else-if="tab.tabType === 'FILES'">
            <div class="toolbar">
              <el-select v-model="uploadRole" class="role-select">
                <el-option v-for="(label, value) in FILE_ROLE_LABEL" :key="value" :label="label" :value="value" />
              </el-select>
              <el-upload :show-file-list="false" :http-request="onUpload" :disabled="!canWrite || !canEdit">
                <el-button type="primary" :disabled="!canWrite || !canEdit">上传文件</el-button>
              </el-upload>
              <span class="hint">同一角色重复上传会生成新版本，旧版本保留可追溯</span>
            </div>

            <el-table :data="detail?.files ?? []" border>
              <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
              <el-table-column label="角色" width="110">
                <template #default="{ row }">{{ FILE_ROLE_LABEL[row.fileRole] ?? row.fileRole }}</template>
              </el-table-column>
              <el-table-column label="大小" width="110">
                <template #default="{ row }">{{ formatSize(row.size) }}</template>
              </el-table-column>
              <el-table-column prop="versionNo" label="版本" width="80" />
              <el-table-column label="上传时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openPreview(row.fileId)">预览</el-button>
                  <el-button link type="primary" @click="onDownload(row)">下载</el-button>
                  <el-button link type="danger" :disabled="!canEdit" @click="onRemoveFile(row)">移除</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无文件，点击「上传文件」添加主文件或附件" />
              </template>
            </el-table>
          </template>

          <visual-preview
            v-else-if="tab.tabType === 'VISUAL'"
            object-type="doc"
            :object-id="docId"
          />

          <el-table v-else-if="tab.tabType === 'VERSIONS'" :data="versions" border @row-click="switchVersion">
            <el-table-column prop="revisionLabel" label="版本" width="100" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
              </template>
            </el-table-column>
            <el-table-column label="是否最新" width="110">
              <template #default="{ row }">
                <StatusDot v-if="row.isLatest" tone="ok" label="最新" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="switchVersion(row)">查看该版本</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-table v-else-if="tab.tabType === 'RELATIONS'" v-loading="subLoading" :data="relations" border>
            <el-table-column prop="relationType" label="关系类型" width="150" />
            <el-table-column label="方向" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.direction === 'OUT' ? 'primary' : 'info'">
                  {{ row.direction === 'OUT' ? '指向' : '被指向' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="code" label="编码" width="160" />
            <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="objectType" label="对象类型" width="130" />
            <el-table-column prop="revisionLabel" label="版本" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.objectType !== 'PdmDoc'" @click="openDoc(row.id)">
                  打开
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无关联对象（引用/被引用关系为空）" />
            </template>
          </el-table>

          <workflow-panel
            v-else-if="tab.tabType === 'WORKFLOW'"
            object-type="PdmDoc"
            :object-id="docId"
            @changed="load"
          />

          <el-table v-else-if="tab.tabType === 'ACL'" v-loading="subLoading" :data="aclRows" border>
            <el-table-column label="被授权者类型" width="130">
              <template #default="{ row }">{{ actorTypeLabel(row.actorType) }}</template>
            </el-table-column>
            <el-table-column label="被授权者" min-width="160">
              <template #default="{ row }">{{ row.actorName || row.actorId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="actions" label="操作" min-width="160" />
            <el-table-column label="效果" width="100">
              <template #default="{ row }">
                <StatusDot
                  :tone="row.effect === 'DENY' ? 'danger' : 'ok'"
                  :label="row.effect === 'DENY' ? '拒绝' : '允许'"
                />
              </template>
            </el-table-column>
            <el-table-column label="来源" width="110">
              <template #default="{ row }">{{ row.source || '-' }}</template>
            </el-table-column>
            <el-table-column label="到期时间" width="170">
              <template #default="{ row }">{{ row.expireAt ? formatTime(row.expireAt) : '长期' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前对象无显式授权，按属主与默认策略判定（属性页可查看属主）" />
            </template>
          </el-table>

          <el-table v-else-if="tab.tabType === 'LOGS'" v-loading="subLoading" :data="logs" border>
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.logType === 'STATE' ? 'warning' : 'info'">
                  {{ row.logType === 'STATE' ? '状态流转' : '操作审计' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="动作" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态变化" width="180">
              <template #default="{ row }">
                <span v-if="row.fromState || row.toState">
                  {{ row.fromState || '-' }} → {{ row.toState || '-' }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作人" width="140">
              <template #default="{ row }">{{ row.operatorLabel || row.operatorId || '-' }}</template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <StatusDot
                  :tone="row.result === 'SUCCESS' ? 'ok' : 'danger'"
                  :label="row.result === 'SUCCESS' ? '成功' : row.result || '-'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
            <template #empty>
              <el-empty description="暂无操作记录" />
            </template>
          </el-table>

          <el-empty v-else :description="`未知页签类型：${tab.tabType}`" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑文档属性" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码">
          <el-input :model-value="detail?.code" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="文档类型" prop="docType">
          <el-select v-model="form.docType" style="width: 100%">
            <el-option v-for="(label, value) in DOC_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-radio-group v-model="form.source">
            <el-radio value="NORMAL">普通文档</el-radio>
            <el-radio value="CAD">CAD 文档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="form.classId"
            :data="classTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="可不选；选择后需填必填属性"
            style="width: 100%"
            @change="onClassChange"
          />
        </el-form-item>
        <el-form-item v-for="attr in classAttrs" :key="attr.code" :label="attr.name" :required="attr.required">
          <el-select
            v-if="attr.valueType === 'ENUM'"
            v-model="form.classAttrs![attr.code]"
            clearable
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option
              v-for="item in enumItems(attr.enumCode)"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
          <el-date-picker
            v-else-if="attr.valueType === 'DATE'"
            v-model="form.classAttrs![attr.code]"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="yyyy-MM-dd"
            style="width: 100%"
          />
          <el-radio-group v-else-if="attr.valueType === 'BOOL'" v-model="form.classAttrs![attr.code]">
            <el-radio value="true">是</el-radio>
            <el-radio value="false">否</el-radio>
          </el-radio-group>
          <el-input
            v-else
            v-model="form.classAttrs![attr.code]"
            :placeholder="attr.inherited ? `继承自 ${attr.ownerClassName}` : '可留空'"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="文档预览" width="1040px" @closed="clearPreviewUrl">
      <div v-if="previewLoading" v-loading="true" class="preview-loading" />
      <!-- 浏览器内可直接渲染的格式（.docx / 电子表格）：不依赖 LibreOffice 转换代理 -->
      <template v-else-if="localEngine && localFile">
        <docx-preview v-if="localEngine === 'DOCX'" :file-id="localFile.fileId" :file-name="localFile.fileName" />
        <sheet-preview v-else :file-id="localFile.fileId" :file-name="localFile.fileName" />
      </template>
      <template v-else>
        <el-alert
          v-if="previewInfo?.status === 'SKIPPED'"
          type="info"
          show-icon
          :closable="false"
          :title="previewInfo?.message ?? '该格式暂不支持在线预览'"
          class="preview-alert"
        />
        <el-alert
          v-else-if="previewInfo?.status === 'FAILED'"
          type="error"
          show-icon
          :closable="false"
          :title="`预览生成失败：${previewInfo?.message ?? '未知原因'}`"
          class="preview-alert"
        />
        <el-alert
          v-else-if="previewInfo?.status === 'NONE'"
          type="info"
          show-icon
          :closable="false"
          title="尚未生成预览，点击「生成预览」开始"
          class="preview-alert"
        />
        <div v-if="previewInfo?.status === 'READY' && previewUrl" class="preview-box">
          <img v-if="isImagePreview" :src="previewUrl" class="preview-image" alt="文档预览" />
          <iframe v-else :src="previewUrl" class="preview-frame" title="文档预览" />
        </div>
      </template>
      <template #footer>
        <el-button v-if="previewInfo?.status === 'READY'" @click="downloadPreview">下载预览</el-button>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button
          v-if="!localEngine"
          type="primary"
          :loading="previewLoading"
          @click="generatePreview(previewTargetFileId ?? undefined)"
        >
          {{ previewInfo?.status === 'READY' ? '重新生成' : '生成预览' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import {
  DOC_SOURCE_LABEL,
  DOC_STATE_LABEL,
  DOC_TYPE_LABEL,
  FILE_ROLE_LABEL,
  docApi,
  docClassApi,
  type DocAclRow,
  type DocClassAttrDef,
  type DocClassNode,
  type DocDetail,
  type DocFile,
  type DocLog,
  type DocPreviewStatus,
  type DocRelation,
  type DocSaveReq,
  type DocVersion,
  type FileRole
} from '@/api/doc'
import { metaApi, type MetaTabDef } from '@/api/system'
import { visualApi, type VisualInfo } from '@/api/visual'
import { workspaceApi } from '@/api/workspace'
import DocxPreview from '@/components/DocxPreview.vue'
import SheetPreview from '@/components/SheetPreview.vue'
import WorkflowPanel from '@/components/WorkflowPanel.vue'
import VisualPreview from '@/components/VisualPreview.vue'
import StatusDot from '@/components/StatusDot.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const route = useRoute()
const router = useRouter()

/** 后端不可用时的页签兜底，与后端 MetaService.DEFAULT_TABS 保持一致。 */
const DEFAULT_TABS: MetaTabDef[] = [
  { code: 'props', name: '属性', tabType: 'PROPS', sort: 10 },
  { code: 'files', name: '文件', tabType: 'FILES', sort: 20 },
  { code: 'versions', name: '历史版本', tabType: 'VERSIONS', sort: 30 },
  { code: 'relations', name: '关联对象', tabType: 'RELATIONS', sort: 40 },
  { code: 'workflow', name: '流程', tabType: 'WORKFLOW', sort: 50 },
  { code: 'acl', name: '权限', tabType: 'ACL', sort: 60 },
  { code: 'logs', name: '日志', tabType: 'LOGS', sort: 70 }
]

const docId = computed(() => String(route.params.id ?? ''))
const detail = ref<DocDetail | null>(null)
const versions = ref<DocVersion[]>([])
const tabs = ref<MetaTabDef[]>(DEFAULT_TABS)
const relations = ref<DocRelation[]>([])
const logs = ref<DocLog[]>([])
const aclRows = ref<DocAclRow[]>([])
const subLoading = ref(false)
const loadedTabs = new Set<string>()
const activeTab = ref('props')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const uploadRole = ref<FileRole>('MAIN')
const formRef = ref<FormInstance>()

const form = reactive<DocSaveReq>({
  name: '',
  docType: 'DESIGN',
  source: 'NORMAL',
  keyword: '',
  description: '',
  classId: null,
  classAttrs: {}
})

const classTree = ref<DocClassNode[]>([])
const classAttrs = ref<DocClassAttrDef[]>([])
const enumCache = ref<Record<string, { code: string; name: string }[]>>({})

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewInfo = ref<DocPreviewStatus | null>(null)
const previewUrl = ref('')
const previewTargetFileId = ref<string | null>(null)
/**
 * 前端可直接渲染的引擎（DOCX / SHEET）。
 * 命中时预览弹窗直接用 docx-preview / SheetJS 渲染原始文件，
 * 不再走 LibreOffice 转换代理（该链路未配置时会返回 SKIPPED，导致格式「暂不支持」）。
 */
const localEngine = ref<'DOCX' | 'SHEET' | ''>('')
const localFile = ref<VisualInfo['file']>(null)

const IMAGE_FORMATS = ['PNG', 'JPG', 'JPEG', 'GIF', 'BMP', 'WEBP']
const isImagePreview = computed(() =>
  IMAGE_FORMATS.includes((previewInfo.value?.format ?? '').toUpperCase())
)

const classAttrEntries = computed(() => {
  const values = detail.value?.classAttrs ?? {}
  return Object.entries(values).map(([code, value]) => ({
    code,
    label: classAttrs.value.find((attr) => attr.code === code)?.name ?? code,
    value: value || '-'
  }))
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  docType: [{ required: true, message: '请选择文档类型', trigger: 'change' }]
}

const canEdit = computed(() => {
  const state = detail.value?.lifecycleState
  return state !== 'Released' && state !== 'Obsolete'
})

const canWrite = computed(() => userStore.can('pdm.doc.write'))

const isMyCheckout = computed(() => {
  const checkout = detail.value?.checkout
  return Boolean(checkout) && String(checkout?.userId) === String(userStore.user?.id)
})

const canForceUnlock = computed(
  () => userStore.can('pdm.doc.release') && Boolean(detail.value?.checkout) && !isMyCheckout.value
)

function formatTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function formatSize(size?: number): string {
  if (!size) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

function typeLabel(value?: string): string {
  return value ? DOC_TYPE_LABEL[value] ?? value : '-'
}

function sourceLabel(value?: string): string {
  return value ? DOC_SOURCE_LABEL[value] ?? value : '-'
}

function stateLabel(value?: string): string {
  return value ? DOC_STATE_LABEL[value] ?? value : '-'
}


async function load(): Promise<void> {
  if (!docId.value) {
    return
  }
  loading.value = true
  try {
    detail.value = await docApi.detail(docId.value)
    versions.value = await docApi.versions(docId.value)
    if (classTree.value.length === 0) {
      classTree.value = await docClassApi.tree().catch(() => [])
    }
    classAttrs.value = detail.value.classId
      ? await docClassApi.attrs(detail.value.classId).catch(() => [])
      : []
    tabs.value = await metaApi.entityTabs('PdmDoc').catch(() => DEFAULT_TABS)
    loadedTabs.clear()
    relations.value = []
    logs.value = []
    aclRows.value = []
    if (!tabs.value.some((tab) => tab.code === activeTab.value)) {
      activeTab.value = tabs.value[0]?.code ?? 'props'
    }
    workspaceApi.touchRecent(docId.value).catch(() => undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
  await loadTabData(activeTab.value)
}

/** 页签数据懒加载：关联对象 / 权限 / 日志各取一次，切换版本后由 load 清空。 */
async function loadTabData(code: string): Promise<void> {
  const tab = tabs.value.find((item) => item.code === code)
  if (!tab || !docId.value || loadedTabs.has(code)) {
    return
  }
  if (tab.tabType !== 'RELATIONS' && tab.tabType !== 'ACL' && tab.tabType !== 'LOGS') {
    return
  }
  subLoading.value = true
  try {
    if (tab.tabType === 'RELATIONS') {
      relations.value = await docApi.relations(docId.value)
    } else if (tab.tabType === 'ACL') {
      aclRows.value = await docApi.acl(docId.value)
    } else {
      logs.value = await docApi.logs(docId.value)
    }
    loadedTabs.add(code)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载页签数据失败')
  } finally {
    subLoading.value = false
  }
}

function onTabChange(name: string): void {
  loadTabData(name)
}

const ACTOR_TYPE_LABEL: Record<string, string> = {
  USER: '用户',
  ROLE: '角色',
  GROUP: '用户组',
  GROUP_ROLE: '组角色',
  CREATOR: '创建人',
  OWNER: '属主',
  ALL: '所有人'
}

function actorTypeLabel(value?: string): string {
  return value ? ACTOR_TYPE_LABEL[value] ?? value : '-'
}

function openDoc(id: string): void {
  router.push({ name: 'doc-detail', params: { id } })
}

function goBack(): void {
  router.push({ name: 'doc-list' })
}

function clearPreviewUrl(): void {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

async function openPreview(fileId?: string): Promise<void> {
  previewVisible.value = true
  previewInfo.value = null
  previewTargetFileId.value = fileId ?? null
  localEngine.value = ''
  localFile.value = null
  clearPreviewUrl()
  previewLoading.value = true
  try {
    // 优先判定能否在浏览器内直接渲染（.docx / 表格）。
    // 这一类文件根本不需要转换代理，也就不会出现「未配置 LibreOffice」的 SKIPPED 提示。
    const visual = await visualApi.doc(docId.value).catch(() => null)
    if (
      visual &&
      !visual.needsConvert &&
      visual.file &&
      (visual.engine === 'DOCX' || visual.engine === 'SHEET')
    ) {
      localEngine.value = visual.engine
      localFile.value = visual.file
      return
    }
    const current = await docApi.preview(docId.value)
    previewInfo.value = current
    if (fileId) {
      if (current.sourceFileId !== fileId || current.status !== 'READY') {
        await generatePreview(fileId)
      } else {
        await loadPreviewBlob()
      }
    } else if (current.status === 'READY') {
      await loadPreviewBlob()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '查询预览状态失败')
  } finally {
    previewLoading.value = false
  }
}

async function generatePreview(fileId?: string): Promise<void> {
  previewLoading.value = true
  try {
    const result = await docApi.generatePreview(docId.value, fileId ?? previewTargetFileId.value ?? undefined)
    previewInfo.value = result
    if (result.status === 'READY') {
      await loadPreviewBlob()
    } else {
      clearPreviewUrl()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成预览失败')
  } finally {
    previewLoading.value = false
  }
}

async function loadPreviewBlob(): Promise<void> {
  const fileId = previewInfo.value?.previewFileId
  if (!fileId) {
    return
  }
  try {
    const blob = await docApi.downloadFileBlob(fileId)
    clearPreviewUrl()
    previewUrl.value = URL.createObjectURL(blob)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载预览内容失败')
  }
}

async function downloadPreview(): Promise<void> {
  const fileId = previewInfo.value?.previewFileId
  if (!fileId) {
    return
  }
  try {
    await docApi.downloadPreview(fileId, previewInfo.value?.fileName ?? 'preview.pdf')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载预览失败')
  }
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.name = detail.value.name
  form.docType = detail.value.docType
  form.source = detail.value.source
  form.keyword = detail.value.keyword ?? ''
  form.description = detail.value.description ?? ''
  form.classId = detail.value.classId ?? null
  form.classAttrs = { ...(detail.value.classAttrs ?? {}) }
  dialogVisible.value = true
}

async function onClassChange(value: string | null): Promise<void> {
  form.classAttrs = {}
  classAttrs.value = []
  if (!value) {
    return
  }
  try {
    const attrs = await docClassApi.attrs(value)
    classAttrs.value = attrs
    const initial: Record<string, string> = {}
    attrs.forEach((attr) => {
      initial[attr.code] = attr.defaultValue ?? ''
    })
    form.classAttrs = initial
    await ensureEnums(attrs.map((attr) => attr.enumCode).filter((code): code is string => Boolean(code)))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载分类属性失败')
  }
}

async function ensureEnums(codes: string[]): Promise<void> {
  for (const code of codes) {
    if (enumCache.value[code]) {
      continue
    }
    try {
      const enumDetail = await metaApi.getEnum(code)
      enumCache.value[code] = enumDetail.items.map((item) => ({ code: item.code, name: item.name }))
    } catch {
      enumCache.value[code] = []
    }
  }
}

function enumItems(code?: string | null): { code: string; name: string }[] {
  return code ? enumCache.value[code] ?? [] : []
}

function cleanClassAttrs(): Record<string, string> {
  const result: Record<string, string> = {}
  Object.entries(form.classAttrs ?? {}).forEach(([key, value]) => {
    const text = value === null || value === undefined ? '' : String(value).trim()
    if (text) {
      result[key] = text
    }
  })
  return result
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
    await docApi.update(docId.value, {
      ...form,
      classId: form.classId || null,
      classAttrs: cleanClassAttrs()
    })
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onUpload(options: UploadRequestOptions): Promise<void> {
  try {
    await docApi.upload(docId.value, options.file as File, uploadRole.value)
    ElMessage.success('上传成功')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  }
}

async function onDownload(file: DocFile): Promise<void> {
  try {
    await docApi.download(file)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  }
}

async function onRemoveFile(file: DocFile): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认移除文件「${file.fileName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await docApi.removeFile(docId.value, file.linkId)
    ElMessage.success('已移除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function onCheckout(): Promise<void> {
  try {
    await ElMessageBox.confirm('检出后其他人只能查看，确认检出？', '提示', { type: 'info' })
  } catch {
    return
  }
  try {
    await docApi.checkout(docId.value)
    ElMessage.success('已检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检出失败')
  }
}

async function onCheckin(): Promise<void> {
  const result = await ElMessageBox.prompt('检入将生成新的小版本，可填写说明（可选）', '检入', {
    inputPlaceholder: '本次修改说明',
    inputValue: '',
    confirmButtonText: '检入',
    cancelButtonText: '取消'
  }).catch(() => null)
  if (!result) {
    return
  }
  try {
    const next = await docApi.checkin(docId.value, { remark: result.value || undefined })
    ElMessage.success(`已检入，新版本 ${next.revisionLabel}`)
    router.replace({ name: 'doc-detail', params: { id: next.id } })
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检入失败')
  }
}

async function onCancelCheckout(): Promise<void> {
  try {
    await ElMessageBox.confirm('取消检出将丢弃本次编辑（不产生新版本），确认？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await docApi.cancelCheckout(docId.value)
    ElMessage.success('已取消检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function onForceUnlock(): Promise<void> {
  try {
    await ElMessageBox.confirm('强制解锁会中断他人编辑并记录审计，确认？', '强制解锁', { type: 'warning' })
  } catch {
    return
  }
  try {
    const holder = await docApi.forceUnlock(docId.value)
    ElMessage.success(`已解锁${holder ? `（原持有者 ${holder}）` : ''}`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '解锁失败')
  }
}

function switchVersion(version: DocVersion): void {
  if (version.id === docId.value) {
    return
  }
  router.replace({ name: 'doc-detail', params: { id: version.id } })
}

watch(docId, load)
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
  gap: 12px;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.code {
  font-weight: 600;
  color: #409eff;
}

.name {
  font-weight: 600;
}

.checkout-tip {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.role-select {
  width: 140px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.preview-alert {
  margin-bottom: 12px;
}

.preview-loading {
  height: 360px;
}

.preview-box {
  height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border: 1px solid #eef1f6;
  overflow: auto;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.placeholder {
  padding: 8px 0;
}
</style>
