<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">流程定义</span>
          <div>
            <el-button @click="back">返回流程中心</el-button>
            <el-button type="primary" @click="openDesigner()">新建流程</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="同名流程标识重复保存即生成新版本：已在跑的实例继续走旧版本，新发起的实例走最新版本。"
        class="tip"
      />

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="key" label="流程标识" width="200" show-overflow-tooltip />
        <el-table-column label="名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="版本" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="部署时间" width="180">
          <template #default="{ row }">{{ formatTime(row.deploymentTime) }}</template>
        </el-table-column>
        <el-table-column prop="resourceName" label="资源文件" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
            <el-button link @click="viewXml(row)">XML</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无流程定义，点击「新建流程」用设计器创建" />
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="designerVisible"
      :title="editingId ? '编辑流程定义' : '新建流程定义'"
      fullscreen
      destroy-on-close
      @closed="destroyModeler"
    >
      <div class="designer-bar">
        <span class="label">流程名称</span>
        <el-input v-model="form.name" placeholder="如 文档通用审批" style="width: 220px" />
        <span class="label">流程标识</span>
        <el-input
          v-model="form.key"
          placeholder="BPMN process id，如 nexplmApproval"
          :disabled="Boolean(editingId)"
          style="width: 280px"
        />
        <span v-if="editingId" class="hint">当前 v{{ editingVersion }}，保存后生成 v{{ editingVersion + 1 }}</span>
        <span v-else class="hint">标识相同即视为同一流程的升级</span>
      </div>
      <div ref="canvasRef" class="canvas"></div>
      <template #footer>
        <el-button @click="designerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存并部署</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="xmlVisible" title="流程 XML" width="860px">
      <el-input :model-value="xmlText" type="textarea" :rows="24" readonly />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { workflowApi, type DefinitionRow } from '@/api/workflow'

/** 新建流程的空白模板：开始 → 审批 → 结束，用户在设计器里改。 */
const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1" targetNamespace="http://nexplm.com/bpmn">
  <bpmn:process id="process_new" name="新流程" isExecutable="true">
    <bpmn:startEvent id="startEvent_1" name="开始" />
    <bpmn:userTask id="review" name="审批" flowable:assignee="\${approver}" />
    <bpmn:endEvent id="endEvent_1" name="结束" />
    <bpmn:sequenceFlow id="Flow_1" sourceRef="startEvent_1" targetRef="review" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="review" targetRef="endEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_new">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="startEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1_di" bpmnElement="review">
        <dc:Bounds x="280" y="138" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="endEvent_1">
        <dc:Bounds x="440" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="216" y="178" />
        <di:waypoint x="280" y="178" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="380" y="178" />
        <di:waypoint x="440" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

const router = useRouter()

const rows = ref<DefinitionRow[]>([])
const loading = ref(false)
const saving = ref(false)

const designerVisible = ref(false)
const editingId = ref<string | null>(null)
const editingVersion = ref(0)
const canvasRef = ref<HTMLElement | null>(null)

const xmlVisible = ref(false)
const xmlText = ref('')

const form = reactive({ name: '', key: '' })

let modeler: InstanceType<typeof BpmnModeler> | null = null

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load(): Promise<void> {
  loading.value = true
  try {
    rows.value = await workflowApi.definitions()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载流程定义失败')
  } finally {
    loading.value = false
  }
}

function createModeler(): void {
  if (!canvasRef.value) {
    return
  }
  modeler = new BpmnModeler({ container: canvasRef.value })
}

function destroyModeler(): void {
  modeler?.destroy()
  modeler = null
}

function fitViewport(): void {
  const canvas = (modeler as unknown as { get(name: string): { zoom(v: string): void } } | null)?.get('canvas')
  canvas?.zoom('fit-viewport')
}

async function openDesigner(row?: DefinitionRow): Promise<void> {
  editingId.value = row?.id ?? null
  editingVersion.value = row?.version ?? 0
  form.name = row?.name ?? ''
  form.key = row?.key ?? ''
  designerVisible.value = true
  await nextTick()
  createModeler()
  if (!modeler) {
    return
  }
  try {
    const xml = row ? (await workflowApi.definitionXml(row.id)).xml : EMPTY_BPMN
    await modeler.importXML(xml)
    fitViewport()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载流程图失败')
  }
}

/** 用 DOM 设置 process 的 id/name，避免正则替换 XML 带来的误伤。 */
function patchProcessMeta(xml: string, key: string, name: string): string {
  const doc = new DOMParser().parseFromString(xml, 'application/xml')
  const process = doc.getElementsByTagNameNS('*', 'process')[0]
  if (!process) {
    return xml
  }
  if (key) {
    process.setAttribute('id', key)
  }
  if (name) {
    process.setAttribute('name', name)
  }
  return new XMLSerializer().serializeToString(doc)
}

async function onSave(): Promise<void> {
  if (!modeler) {
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请填写流程名称')
    return
  }
  if (!editingId.value && !form.key.trim()) {
    ElMessage.warning('请填写流程标识（BPMN process id）')
    return
  }
  saving.value = true
  try {
    const { xml } = await modeler.saveXML({ format: true })
    if (!xml) {
      ElMessage.error('导出流程 XML 失败')
      return
    }
    const deployed = await workflowApi.deployDefinition({
      name: form.name.trim(),
      bpmnXml: patchProcessMeta(xml, form.key.trim(), form.name.trim())
    })
    ElMessage.success(`已部署 ${deployed.key} v${deployed.version}`)
    designerVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '部署失败')
  } finally {
    saving.value = false
  }
}

async function viewXml(row: DefinitionRow): Promise<void> {
  try {
    const detail = await workflowApi.definitionXml(row.id)
    xmlText.value = detail.xml
    xmlVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '读取 XML 失败')
  }
}

function back(): void {
  router.push({ name: 'workflow-center' })
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.tip {
  margin-bottom: 12px;
}

.designer-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e8ef;
  margin-bottom: 8px;
}

.label {
  color: #606266;
  font-size: 13px;
}

.hint {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.canvas {
  height: calc(100vh - 210px);
  border: 1px solid #e5e8ef;
}
</style>
