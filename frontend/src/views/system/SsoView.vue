<template>
  <div class="page">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="编码/名称"
        clearable
        style="width: 220px"
        @keyup.enter="loadProviders"
      />
      <el-button type="primary" @click="loadProviders">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建登录方式</el-button>
      <span class="hint">
        OIDC 走授权码 + PKCE 并校验 id_token 签名；LDAP/AD 用服务账号查人后以本人 DN 校验密码。
        密钥只写不读回，留空表示保持不变。
      </span>
    </div>

    <el-table v-loading="loading" :data="providers" border stripe>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="类型" width="150">
        <template #default="{ row }">
          <el-tag :type="row.type === 'OIDC' ? 'primary' : 'warning'" size="small">
            {{ SSO_TYPE_LABEL[row.type] ?? row.type }}
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
      <el-table-column label="自动建号" width="100" align="center">
        <template #default="{ row }">{{ row.autoProvision ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="defaultRoleCode" label="默认角色" width="120" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="更新时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canWrite" label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeProvider(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !providers.length" description="还没有配置单点登录" :image-size="60" />

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑登录方式' : '新建登录方式'"
      width="720px"
      destroy-on-close
    >
      <el-form label-width="140px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="!!form.id" maxlength="64" style="width: 240px" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" style="width: 340px" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" :disabled="!!form.id" style="width: 220px">
            <el-option label="OIDC 统一身份" value="OIDC" />
            <el-option label="LDAP / AD 域账号" value="LDAP" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="label-gap">自动建号</span>
          <el-switch v-model="form.autoProvision" />
          <span class="label-gap">默认角色</span>
          <el-input v-model="form.defaultRoleCode" placeholder="如 BUSINESS" style="width: 160px" />
        </el-form-item>

        <template v-if="form.type === 'OIDC'">
          <el-divider content-position="left">OIDC 参数</el-divider>
          <el-form-item label="Issuer">
            <el-input v-model="form.config.issuer" placeholder="https://idp.example.com" />
          </el-form-item>
          <el-form-item label="授权端点">
            <el-input v-model="form.config.authorizationEndpoint" placeholder="https://idp.example.com/oauth2/authorize" />
          </el-form-item>
          <el-form-item label="令牌端点">
            <el-input v-model="form.config.tokenEndpoint" placeholder="https://idp.example.com/oauth2/token" />
          </el-form-item>
          <el-form-item label="JWKS 端点">
            <el-input v-model="form.config.jwksEndpoint" placeholder="https://idp.example.com/oauth2/jwks" />
          </el-form-item>
          <el-form-item label="Client ID">
            <el-input v-model="form.config.clientId" style="width: 320px" />
          </el-form-item>
          <el-form-item label="Client Secret">
            <el-input
              v-model="form.config.clientSecret"
              show-password
              style="width: 320px"
              :placeholder="secretHint('clientSecret', '留空自动沿用已保存密钥')"
            />
          </el-form-item>
          <el-form-item label="回调地址">
            <el-input v-model="form.config.redirectUri" placeholder="http://localhost:5173/login" />
          </el-form-item>
          <el-form-item label="Scope">
            <el-input v-model="form.config.scope" placeholder="openid profile email" style="width: 320px" />
          </el-form-item>
          <el-form-item label="用户名声明">
            <el-input v-model="form.config.usernameClaim" placeholder="preferred_username" style="width: 200px" />
            <span class="label-gap">姓名</span>
            <el-input v-model="form.config.displayNameClaim" placeholder="name" style="width: 150px" />
            <span class="label-gap">邮箱</span>
            <el-input v-model="form.config.emailClaim" placeholder="email" style="width: 150px" />
          </el-form-item>
        </template>

        <template v-else>
          <el-divider content-position="left">LDAP / AD 参数</el-divider>
          <el-form-item label="服务地址">
            <el-input v-model="form.config.url" placeholder="ldap://ad.example.com:389" />
          </el-form-item>
          <el-form-item label="Base DN">
            <el-input v-model="form.config.baseDn" placeholder="dc=corp,dc=local" />
          </el-form-item>
          <el-form-item label="Bind DN">
            <el-input v-model="form.config.bindDn" placeholder="cn=svc_plm,ou=service,dc=corp,dc=local" />
          </el-form-item>
          <el-form-item label="Bind 口令">
            <el-input
              v-model="form.config.bindCredential"
              show-password
              :placeholder="secretHint('bindCredential', '留空自动沿用已保存口令')"
            />
          </el-form-item>
          <el-form-item label="用户过滤器">
            <el-input v-model="form.config.userFilter" placeholder="(sAMAccountName={0})" />
          </el-form-item>
          <el-form-item label="直绑 DN 模板">
            <el-input v-model="form.config.userDnPattern" placeholder="可选：uid={0},ou=people,dc=corp,dc=local" />
          </el-form-item>
          <el-form-item label="属性映射">
            <el-input v-model="form.config.attrUsername" placeholder="sAMAccountName" style="width: 180px" />
            <span class="label-gap">姓名</span>
            <el-input v-model="form.config.attrDisplayName" placeholder="cn" style="width: 130px" />
            <span class="label-gap">邮箱</span>
            <el-input v-model="form.config.attrEmail" placeholder="mail" style="width: 130px" />
          </el-form-item>
          <el-form-item label="主体标识属性">
            <el-input v-model="form.config.subjectAttr" placeholder="entryDN / objectGUID" style="width: 220px" />
            <div class="hint">用于把 IdP 中的同一个人稳定映射到同一账号，缺省用 entryDN</div>
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProvider">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ssoProviderApi, SSO_TYPE_LABEL, type SsoProviderRow } from '@/api/sso'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canWrite = userStore.can('sys.sso.write')

const providers = ref<SsoProviderRow[]>([])
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const secretFields = ref<string[]>([])

const form = reactive({
  id: '',
  code: '',
  name: '',
  type: 'OIDC',
  enabled: true,
  autoProvision: true,
  defaultRoleCode: '',
  remark: '',
  config: {} as Record<string, string>
})

async function loadProviders(): Promise<void> {
  loading.value = true
  try {
    const result = await ssoProviderApi.page({
      keyword: keyword.value || undefined,
      page: 1,
      size: 50
    })
    providers.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载登录方式失败')
  } finally {
    loading.value = false
  }
}

function blankConfig(): Record<string, string> {
  return {
    issuer: '',
    authorizationEndpoint: '',
    tokenEndpoint: '',
    jwksEndpoint: '',
    clientId: '',
    clientSecret: '',
    redirectUri: '',
    scope: 'openid profile email',
    usernameClaim: 'preferred_username',
    displayNameClaim: 'name',
    emailClaim: 'email',
    url: '',
    baseDn: '',
    bindDn: '',
    bindCredential: '',
    userFilter: '(sAMAccountName={0})',
    userDnPattern: '',
    attrUsername: 'sAMAccountName',
    attrDisplayName: 'cn',
    attrEmail: 'mail',
    subjectAttr: 'entryDN'
  }
}

function openCreate(): void {
  Object.assign(form, {
    id: '',
    code: '',
    name: '',
    type: 'OIDC',
    enabled: true,
    autoProvision: true,
    defaultRoleCode: '',
    remark: '',
    config: blankConfig()
  })
  secretFields.value = []
  dialogVisible.value = true
}

async function openEdit(row: SsoProviderRow): Promise<void> {
  try {
    const detail = await ssoProviderApi.detail(row.id)
    secretFields.value = detail.secretFields ?? []
    Object.assign(form, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      type: detail.type,
      enabled: detail.enabled,
      autoProvision: detail.autoProvision,
      defaultRoleCode: detail.defaultRoleCode ?? '',
      remark: detail.remark ?? '',
      config: { ...blankConfig(), ...(detail.config as Record<string, string>) }
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载配置失败')
  }
}

function secretHint(field: string, fallback: string): string {
  return secretFields.value.includes(field) ? '已配置，留空保持不变' : fallback
}

async function saveProvider(): Promise<void> {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('编码与名称为必填')
    return
  }
  const config: Record<string, unknown> = {}
  const fields = form.type === 'OIDC'
    ? ['issuer', 'authorizationEndpoint', 'tokenEndpoint', 'jwksEndpoint', 'clientId', 'clientSecret',
      'redirectUri', 'scope', 'usernameClaim', 'displayNameClaim', 'emailClaim']
    : ['url', 'baseDn', 'bindDn', 'bindCredential', 'userFilter', 'userDnPattern', 'attrUsername',
      'attrDisplayName', 'attrEmail', 'subjectAttr']
  for (const field of fields) {
    const value = (form.config[field] ?? '').trim()
    if (value) {
      config[field] = value
    }
  }
  saving.value = true
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      type: form.type,
      enabled: form.enabled,
      autoProvision: form.autoProvision,
      defaultRoleCode: form.defaultRoleCode.trim() || null,
      remark: form.remark.trim() || null,
      config
    }
    if (form.id) {
      await ssoProviderApi.update(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await ssoProviderApi.create(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadProviders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeProvider(row: SsoProviderRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除登录方式 ${row.code}？已绑定的账号会保留，但将无法再用它登录。`, '删除登录方式', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await ssoProviderApi.remove(row.id)
    ElMessage.success('已删除')
    await loadProviders()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function formatTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadProviders)
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
</style>
