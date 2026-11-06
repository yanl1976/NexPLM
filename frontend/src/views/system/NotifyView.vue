<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="编码/名称"
        clearable
        style="width: 220px"
        @keyup.enter="loadChannels"
      />
      <el-button type="primary" @click="loadChannels">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建通道</el-button>
      <span class="hint">
        邮件走 SMTP，通道里留空的字段回落 application.yml 的 NEXPLM_SMTP_* 占位符；两处都没配会明确报错，不会静默丢弃。
      </span>
    </div>

    <el-table v-loading="loading" :data="channels" border stripe>
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="170">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ NOTIFY_TYPE_LABEL[row.type] ?? row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="事件" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="event in row.events" :key="event" size="small" class="tag-gap">
            {{ eventLabel(event) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试上限" width="90" align="center">
        <template #default="{ row }">{{ row.maxAttempts }} 次</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="更新时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" @click="testChannel(row)">测试</el-button>
          <el-button link type="warning" @click="openMessages(row)">记录</el-button>
          <el-button link type="danger" @click="removeChannel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!loading && !channels.length"
      :description="canRead ? '还没有配置通知通道' : '当前账号没有「通知通道查看」权限（sys.notify.read）'"
      :image-size="60"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑通知通道' : '新建通知通道'"
      width="700px"
      destroy-on-close
    >
      <el-form label-width="120px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="!!form.id" maxlength="64" style="width: 240px" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" style="width: 320px" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" :disabled="!!form.id" style="width: 220px">
            <el-option v-for="type in channelTypes" :key="type" :label="NOTIFY_TYPE_LABEL[type] ?? type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="订阅事件" required>
          <el-checkbox-group v-model="form.events">
            <el-checkbox v-for="(label, value) in eventLabels" :key="value" :value="value">
              {{ label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="label-gap">重试上限</span>
          <el-input-number v-model="form.maxAttempts" :min="1" :max="10" controls-position="right" style="width: 120px" />
        </el-form-item>

        <template v-if="form.type === 'EMAIL'">
          <el-divider content-position="left">SMTP 参数</el-divider>
          <el-form-item label="收件人" required>
            <el-input v-model="form.config.to" placeholder="ops@example.com，多个用逗号分隔" />
          </el-form-item>
          <el-form-item label="SMTP 主机">
            <el-input v-model="form.config.host" :placeholder="placeholderOf('host', 'smtp.example.com（留空用 NEXPLM_SMTP_HOST）')" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input v-model="form.config.port" :placeholder="placeholderOf('port', '465（留空用 NEXPLM_SMTP_PORT）')" style="width: 200px" />
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="form.config.username" :placeholder="placeholderOf('username', '留空用 NEXPLM_SMTP_USERNAME')" style="width: 320px" />
          </el-form-item>
          <el-form-item label="口令">
            <el-input
              v-model="form.config.password"
              show-password
              style="width: 320px"
              :placeholder="secretHint('password', '留空用 NEXPLM_SMTP_PASSWORD')"
            />
          </el-form-item>
          <el-form-item label="发件人">
            <el-input v-model="form.config.from" :placeholder="placeholderOf('from', 'notify@example.com（留空用 NEXPLM_SMTP_FROM）')" />
          </el-form-item>
          <el-form-item label="发件人名称">
            <el-input v-model="form.config.fromName" placeholder="NexPLM" style="width: 200px" />
          </el-form-item>
          <el-form-item label="加密方式">
            <span class="label-gap">SSL</span>
            <el-switch v-model="form.config.ssl" />
            <span class="label-gap">STARTTLS</span>
            <el-switch v-model="form.config.starttls" />
            <div class="hint">465 端口用 SSL；587 端口用 STARTTLS（二者择一）</div>
          </el-form-item>
        </template>

        <template v-else-if="form.type === 'WECOM'">
          <el-divider content-position="left">企业微信群机器人</el-divider>
          <el-form-item label="Webhook" required>
            <el-input v-model="form.config.webhook" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." />
            <div class="hint">URL 里的 key 即凭据，详情接口不会回传</div>
          </el-form-item>
        </template>

        <template v-else>
          <el-divider content-position="left">钉钉自定义机器人</el-divider>
          <el-form-item label="Webhook" required>
            <el-input v-model="form.config.webhook" placeholder="https://oapi.dingtalk.com/robot/send?access_token=..." />
          </el-form-item>
          <el-form-item label="加签密钥">
            <el-input
              v-model="form.config.secret"
              show-password
              :placeholder="secretHint('secret', '可选：安全设置里选「加签」时填写')"
            />
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveChannel">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="messageDrawerVisible" :title="`发送记录 · ${currentChannelCode}`" size="820px" destroy-on-close>
      <div class="toolbar">
        <el-select v-model="messageStatus" clearable placeholder="全部状态" style="width: 160px" @change="loadMessages">
          <el-option v-for="(label, value) in NOTIFY_STATUS_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button @click="loadMessages">刷新</el-button>
        <span class="hint">失败按退避自动重试，次数用尽进死信；死信用原文案快照重发</span>
      </div>
      <el-table v-loading="messageLoading" :data="messages" border size="small">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div>标题：{{ row.title }}</div>
              <div>事件键：{{ row.eventKey }}</div>
              <div>下次重试：{{ formatTime(row.nextRetryAt) }}</div>
              <div>错误：{{ row.errorMsg || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="事件" width="120">
          <template #default="{ row }">{{ eventLabel(row.eventType) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">
              {{ NOTIFY_STATUS_LABEL[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="尝试" width="80" align="center">
          <template #default="{ row }">{{ row.attempt }}/{{ row.maxAttempts }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canWrite && (row.status === 'FAILED' || row.status === 'DEAD')"
              link
              type="warning"
              @click="resend(row)"
            >
              重发
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!messageLoading && !messages.length" description="还没有发送记录" :image-size="60" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  NOTIFY_EVENT_LABEL,
  NOTIFY_STATUS_LABEL,
  NOTIFY_TYPE_LABEL,
  notifyApi,
  type NotifyChannelRow,
  type NotifyMessageRow
} from '@/api/notify'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = userStore.can('sys.notify.write')
const canRead = userStore.can('sys.notify.read')

const channels = ref<NotifyChannelRow[]>([])
const channelTypes = ref<string[]>(['EMAIL', 'WECOM', 'DINGTALK'])
// 事件字典以后端 EVENT_LABELS 为准，前端常量只作接口不可用时的兜底
const eventLabels = ref<Record<string, string>>({ ...NOTIFY_EVENT_LABEL })
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const secretFields = ref<string[]>([])

const form = reactive({
  id: '',
  code: '',
  name: '',
  type: 'EMAIL',
  enabled: true,
  maxAttempts: 3,
  events: ['doc.released'] as string[],
  remark: '',
  config: {} as Record<string, string | boolean>
})

const messageDrawerVisible = ref(false)
const messageLoading = ref(false)
const messages = ref<NotifyMessageRow[]>([])
const messageStatus = ref('')
const currentChannelId = ref('')
const currentChannelCode = ref('')

async function loadChannels(): Promise<void> {
  loading.value = true
  try {
    const result = await notifyApi.page({
      keyword: keyword.value || undefined,
      page: 1,
      size: 50
    })
    channels.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载通知通道失败')
  } finally {
    loading.value = false
  }
}

function blankConfig(): Record<string, string | boolean> {
  return {
    to: '',
    host: '',
    port: '',
    username: '',
    password: '',
    from: '',
    fromName: 'NexPLM',
    ssl: true,
    starttls: false,
    webhook: '',
    secret: ''
  }
}

function openCreate(): void {
  Object.assign(form, {
    id: '',
    code: '',
    name: '',
    type: channelTypes.value[0] ?? 'EMAIL',
    enabled: true,
    maxAttempts: 3,
    events: ['doc.released'],
    remark: '',
    config: blankConfig()
  })
  secretFields.value = []
  dialogVisible.value = true
}

async function openEdit(row: NotifyChannelRow): Promise<void> {
  try {
    const detail = await notifyApi.detail(row.id)
    secretFields.value = detail.secretFields ?? []
    Object.assign(form, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      type: detail.type,
      enabled: detail.enabled,
      maxAttempts: detail.maxAttempts,
      events: [...detail.events],
      remark: detail.remark ?? '',
      config: { ...blankConfig(), ...(detail.config as Record<string, string | boolean>) }
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载通道失败')
  }
}

function secretHint(field: string, fallback: string): string {
  return secretFields.value.includes(field) ? '已配置，留空保持不变' : fallback
}

function placeholderOf(field: string, fallback: string): string {
  return secretFields.value.includes(field) ? '已配置，留空保持不变' : fallback
}

async function saveChannel(): Promise<void> {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('编码与名称为必填')
    return
  }
  if (!form.events.length) {
    ElMessage.warning('至少订阅一个事件')
    return
  }
  const config: Record<string, unknown> = {}
  if (form.type === 'EMAIL') {
    for (const field of ['to', 'host', 'port', 'username', 'password', 'from', 'fromName']) {
      const value = String(form.config[field] ?? '').trim()
      if (value) {
        config[field] = value
      }
    }
    config.ssl = Boolean(form.config.ssl)
    config.starttls = Boolean(form.config.starttls)
  } else if (form.type === 'WECOM') {
    const webhook = String(form.config.webhook ?? '').trim()
    if (webhook) {
      config.webhook = webhook
    }
  } else {
    const webhook = String(form.config.webhook ?? '').trim()
    const secret = String(form.config.secret ?? '').trim()
    if (webhook) {
      config.webhook = webhook
    }
    if (secret) {
      config.secret = secret
    }
  }

  saving.value = true
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      type: form.type,
      enabled: form.enabled,
      maxAttempts: form.maxAttempts,
      events: form.events,
      remark: form.remark.trim() || null,
      config
    }
    if (form.id) {
      await notifyApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await notifyApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadChannels()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function testChannel(row: NotifyChannelRow): Promise<void> {
  try {
    const message = await notifyApi.test(row.id)
    if (message.status === 'SENT') {
      ElMessage.success('测试消息已发送')
    } else {
      ElMessage.warning(`测试发送失败：${message.errorMsg ?? message.status}`)
    }
    await openMessages(row)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '测试发送失败')
  }
}

async function removeChannel(row: NotifyChannelRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除通道 ${row.code}？历史发送记录保留，但不再接收事件。`, '删除通知通道', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await notifyApi.remove(row.id)
    ElMessage.success('已删除')
    await loadChannels()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function openMessages(row: NotifyChannelRow): Promise<void> {
  currentChannelId.value = row.id
  currentChannelCode.value = row.code
  messageStatus.value = ''
  messageDrawerVisible.value = true
  await loadMessages()
}

async function loadMessages(): Promise<void> {
  if (!currentChannelId.value) {
    return
  }
  messageLoading.value = true
  try {
    const result = await notifyApi.messagePage(currentChannelId.value, {
      status: messageStatus.value || undefined,
      page: 1,
      size: 100
    })
    messages.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载发送记录失败')
  } finally {
    messageLoading.value = false
  }
}

async function resend(row: NotifyMessageRow): Promise<void> {
  try {
    const message = await notifyApi.resend(row.id)
    if (message.status === 'SENT') {
      ElMessage.success('重发成功')
    } else {
      ElMessage.warning(`重发失败：${message.errorMsg ?? message.status}`)
    }
    await loadMessages()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重发失败')
  }
}

function eventLabel(event: string): string {
  return eventLabels.value[event] ?? event
}

function typeTag(type: string): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 'EMAIL') {
    return 'primary'
  }
  return type === 'WECOM' ? 'success' : 'warning'
}

function statusTag(status: string): 'success' | 'info' | 'warning' | 'danger' {
  if (status === 'SENT') {
    return 'success'
  }
  if (status === 'DEAD') {
    return 'danger'
  }
  if (status === 'FAILED') {
    return 'warning'
  }
  return 'info'
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  if (!canRead) {
    return
  }
  try {
    const types = await notifyApi.types()
    if (types.length) {
      channelTypes.value = types
    }
  } catch {
    // 接口不可用时用默认三类，不影响本地渲染
  }
  try {
    const events = await notifyApi.events()
    if (Object.keys(events).length) {
      eventLabels.value = events
    }
  } catch {
    // 同上：保留前端兜底字典
  }
  await loadChannels()
})
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8a94a6;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
  font-size: 13px;
}

.tag-gap {
  margin: 0 4px 2px 0;
}

.expand-box {
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.9;
  color: #5c6b7a;
  word-break: break-all;
}
</style>
