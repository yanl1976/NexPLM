<template>
  <div class="page">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="规则权限" name="rule">
        <div class="card-header">
          <div class="filters">
            <el-input v-model="objectTypeFilter" placeholder="对象类型（如 PdmDoc）" clearable style="width: 220px" />
            <el-button type="primary" @click="loadRules">查询</el-button>
          </div>
          <div>
            <el-button @click="openTest">规则试算</el-button>
            <el-button type="primary" @click="openCreateRule">新建规则</el-button>
          </div>
        </div>

        <el-table :data="rules" v-loading="ruleLoading" border stripe>
          <el-table-column prop="code" label="规则编码" width="180" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="objectType" label="对象类型" width="130" />
          <el-table-column prop="actions" label="授予操作" width="160" />
          <el-table-column label="效果" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.effect === 'DENY' ? 'danger' : 'success'">
                {{ row.effect === 'DENY' ? '拒绝' : '允许' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="90" />
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditRule(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="对象授权" name="acl">
        <div class="filters">
          <el-input v-model="aclObjectId" placeholder="对象 ID" style="width: 220px" />
          <el-button type="primary" @click="loadAcl">查询授权</el-button>
          <el-button @click="aclDialogVisible = true">新增授权</el-button>
        </div>

        <el-table :data="aclRows" border stripe>
          <el-table-column prop="actorType" label="被授权者类型" width="150" />
          <el-table-column label="被授权者" min-width="160">
            <template #default="{ row }">{{ row.actorName || row.actorId || '全体用户' }}</template>
          </el-table-column>
          <el-table-column prop="actions" label="操作" width="200" />
          <el-table-column label="效果" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.effect === 'DENY' ? 'danger' : 'success'">
                {{ row.effect === 'DENY' ? '拒绝' : '允许' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="110" />
          <el-table-column label="到期时间" width="180">
            <template #default="{ row }">{{ row.expireAt ? row.expireAt.replace('T', ' ').substring(0, 19) : '永久' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" @click="onRevoke(row)">撤销</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="ruleVisible" :title="ruleEditing ? '编辑规则' : '新建规则'" size="620px">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleRules" label-width="100px">
        <el-form-item label="规则编码" prop="code">
          <el-input v-model="ruleForm.code" :disabled="ruleEditing" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="对象类型" prop="objectType">
          <el-input v-model="ruleForm.objectType" placeholder="如 PdmDoc" />
        </el-form-item>
        <el-form-item label="授予操作" prop="actions">
          <el-select v-model="actionList" multiple style="width: 100%">
            <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
          </el-select>
        </el-form-item>
        <el-form-item label="效果">
          <el-radio-group v-model="ruleForm.effect">
            <el-radio value="ALLOW">允许</el-radio>
            <el-radio value="DENY">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
        <el-form-item label="条件表达式" prop="exprJson">
          <el-input v-model="ruleForm.exprJson" type="textarea" :rows="10" spellcheck="false" />
          <div class="hint">
            支持 AND / OR / NOT 嵌套，运算符 EQ NE IN NOT_IN GT GE LT LE CONTAINS STARTS_WITH IS_NULL NOT_NULL；
            属性可用 ext_json.xxx 路径，内置变量 ${currentUserId} / ${currentCompanyId}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveRule">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="testVisible" title="规则试算" width="620px">
      <el-form label-width="110px">
        <el-form-item label="条件表达式">
          <el-input v-model="testExpr" type="textarea" :rows="6" spellcheck="false" />
        </el-form-item>
        <el-form-item label="对象属性(JSON)">
          <el-input v-model="testAttributes" type="textarea" :rows="6" spellcheck="false" />
        </el-form-item>
      </el-form>
      <el-alert v-if="testResult !== null" :type="testResult ? 'success' : 'info'" :closable="false"
                :title="testResult ? '条件命中：用户可获授权' : '条件未命中'" />
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" @click="onTest">试算</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aclDialogVisible" title="新增授权" width="520px">
      <el-form :model="aclForm" label-width="110px">
        <el-form-item label="对象 ID">
          <el-input v-model="aclForm.objectId" />
        </el-form-item>
        <el-form-item label="被授权者类型">
          <el-select v-model="aclForm.actorType" style="width: 100%">
            <el-option label="用户" value="USER" />
            <el-option label="角色" value="ROLE" />
            <el-option label="组" value="GROUP" />
            <el-option label="全体用户" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="aclForm.actorType !== 'ALL'" label="被授权者 ID">
          <el-input v-model="aclForm.actorId" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="aclActions" multiple style="width: 100%">
            <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
          </el-select>
        </el-form-item>
        <el-form-item label="效果">
          <el-radio-group v-model="aclForm.effect">
            <el-radio value="ALLOW">允许</el-radio>
            <el-radio value="DENY">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aclDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onGrant">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { securityApi, type AclRow, type RuleRow, type RuleSaveReq } from '@/api/security'

const activeTab = ref('rule')
const actionOptions = ['READ', 'WRITE', 'DELETE', 'GRANT', 'CHECKOUT', 'RELEASE', 'APPROVE']

const rules = ref<RuleRow[]>([])
const ruleLoading = ref(false)
const objectTypeFilter = ref('')

const ruleVisible = ref(false)
const ruleEditing = ref(false)
const saving = ref(false)
const ruleFormRef = ref<FormInstance>()
const actionList = ref<string[]>([])

const defaultExpr = '{\n  "op": "AND",\n  "children": [\n    {"op": "EQ", "field": "ownerId", "value": "${currentUserId}"}\n  ]\n}'

const ruleForm = reactive<RuleSaveReq>({
  code: '',
  name: '',
  objectType: 'PdmDoc',
  exprJson: defaultExpr,
  actions: 'READ',
  priority: 100,
  effect: 'ALLOW',
  enabled: true,
  remark: ''
})

const ruleRules: FormRules = {
  code: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  objectType: [{ required: true, message: '请输入对象类型', trigger: 'blur' }],
  exprJson: [{ required: true, message: '请输入条件表达式', trigger: 'blur' }]
}

const testVisible = ref(false)
const testExpr = ref(defaultExpr)
const testAttributes = ref('{"ownerId": "1", "lifecycleState": "Released"}')
const testResult = ref<boolean | null>(null)

const aclObjectId = ref('')
const aclRows = ref<AclRow[]>([])
const aclDialogVisible = ref(false)
const aclActions = ref<string[]>(['READ'])
const aclForm = reactive({ objectId: '', actorType: 'USER', actorId: '', effect: 'ALLOW' })

const actionsText = computed(() => actionList.value.join(','))

async function loadRules(): Promise<void> {
  ruleLoading.value = true
  try {
    rules.value = await securityApi.listRules(objectTypeFilter.value || undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    ruleLoading.value = false
  }
}

function openCreateRule(): void {
  ruleEditing.value = false
  Object.assign(ruleForm, {
    code: '',
    name: '',
    objectType: 'PdmDoc',
    exprJson: defaultExpr,
    actions: 'READ',
    priority: 100,
    effect: 'ALLOW',
    enabled: true,
    remark: ''
  })
  actionList.value = ['READ']
  ruleVisible.value = true
}

function openEditRule(row: RuleRow): void {
  ruleEditing.value = true
  Object.assign(ruleForm, {
    code: row.code,
    name: row.name,
    objectType: row.objectType,
    exprJson: row.exprJson,
    actions: row.actions,
    priority: row.priority,
    effect: row.effect,
    enabled: row.enabled,
    remark: row.remark ?? ''
  })
  actionList.value = row.actions.split(',').map((item) => item.trim()).filter(Boolean)
  ruleVisible.value = true
}

async function onSaveRule(): Promise<void> {
  if (!ruleFormRef.value) {
    return
  }
  const valid = await ruleFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (actionList.value.length === 0) {
    ElMessage.warning('至少选择一个操作')
    return
  }
  saving.value = true
  try {
    const payload = { ...ruleForm, actions: actionsText.value }
    if (ruleEditing.value) {
      await securityApi.updateRule(payload.code, payload)
    } else {
      await securityApi.createRule(payload)
    }
    ElMessage.success('已保存')
    ruleVisible.value = false
    await loadRules()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDeleteRule(row: RuleRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await securityApi.removeRule(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function openTest(): void {
  testResult.value = null
  testVisible.value = true
}

async function onTest(): Promise<void> {
  try {
    let attributes: Record<string, unknown> = {}
    if (testAttributes.value.trim()) {
      attributes = JSON.parse(testAttributes.value) as Record<string, unknown>
    }
    const result = await securityApi.testRule({ exprJson: testExpr.value, attributes })
    testResult.value = result.matched
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试算失败（检查 JSON 格式）')
  }
}

async function loadAcl(): Promise<void> {
  if (!aclObjectId.value) {
    ElMessage.warning('请输入对象 ID')
    return
  }
  try {
    aclRows.value = await securityApi.listAcl(aclObjectId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  }
}

async function onGrant(): Promise<void> {
  if (aclActions.value.length === 0) {
    ElMessage.warning('至少选择一个操作')
    return
  }
  try {
    await securityApi.grant({
      objectId: aclForm.objectId,
      actorType: aclForm.actorType,
      actorId: aclForm.actorType === 'ALL' ? null : aclForm.actorId,
      actions: aclActions.value.join(','),
      effect: aclForm.effect
    })
    ElMessage.success('已授权')
    aclDialogVisible.value = false
    aclObjectId.value = aclForm.objectId
    await loadAcl()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '授权失败')
  }
}

async function onRevoke(row: AclRow): Promise<void> {
  try {
    await ElMessageBox.confirm('确认撤销该授权？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await securityApi.revoke(row.id)
    ElMessage.success('已撤销')
    await loadAcl()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤销失败')
  }
}

onMounted(loadRules)
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

.hint {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
