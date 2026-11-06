<template>
  <div class="page">
    <el-tabs v-model="tab" class="tabs">
      <!-- ------------------------------------------------------ 集成端点 -->
      <el-tab-pane label="集成端点" name="endpoint">
        <div class="toolbar">
          <el-input
            v-model="endpointKeyword"
            placeholder="编码/名称/地址"
            clearable
            style="width: 220px"
            @keyup.enter="loadEndpoints"
          />
          <el-select v-model="endpointEnabled" placeholder="全部状态" clearable style="width: 130px" @change="loadEndpoints">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
          <el-button type="primary" @click="loadEndpoints">查询</el-button>
          <el-button v-if="canConfig" type="primary" plain @click="openEndpointCreate">新建端点</el-button>
          <span class="hint">端点地址必须是 http/https；认证信息只写不读，编辑时不填即保持不变</span>
        </div>
        <el-table v-loading="endpointLoading" :data="endpoints" border stripe>
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="目标系统" width="120" align="center">
            <template #default="{ row }">{{ SYSTEM_CODE_LABEL[row.systemCode] ?? row.systemCode }}</template>
          </el-table-column>
          <el-table-column prop="baseUrl" label="地址" min-width="240" show-overflow-tooltip />
          <el-table-column label="认证" width="150" align="center">
            <template #default="{ row }">
              {{ AUTH_TYPE_LABEL[row.authType] ?? row.authType }}
              <el-tag v-if="row.authConfigured" type="success" size="small" class="tag-gap">已配置</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="超时" width="90" align="center">
            <template #default="{ row }">{{ row.timeoutMs }} ms</template>
          </el-table-column>
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled"
                :disabled="!canConfig"
                @change="(value: boolean) => toggleEndpoint(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="canConfig" label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEndpointEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeEndpoint(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!endpointLoading && !endpoints.length" description="还没有集成端点" :image-size="60" />
        <el-pagination
          v-if="endpointTotal > endpointSize"
          class="pager"
          layout="total, prev, pager, next"
          :total="endpointTotal"
          :page-size="endpointSize"
          :current-page="endpointPage"
          @current-change="(page: number) => { endpointPage = page; loadEndpoints() }"
        />
      </el-tab-pane>

      <!-- ------------------------------------------------------ 字段映射 -->
      <el-tab-pane label="字段映射" name="mapping">
        <div class="toolbar">
          <el-select v-model="mappingEndpointId" placeholder="全部端点" clearable style="width: 200px" @change="loadMappings">
            <el-option v-for="item in endpointOptions" :key="item.id" :label="item.code" :value="item.id" />
          </el-select>
          <el-input
            v-model="mappingObjectCode"
            placeholder="对象编码，如 Material"
            clearable
            style="width: 200px"
            @keyup.enter="loadMappings"
          />
          <el-button type="primary" @click="loadMappings">查询</el-button>
          <el-button v-if="canConfig" type="primary" plain @click="openMappingCreate">新建映射</el-button>
          <span class="hint">规则存库里，改完立即生效，无需重启；required 字段缺失时提交即拒绝</span>
        </div>
        <el-table v-loading="mappingLoading" :data="mappings" border stripe>
          <el-table-column prop="endpointCode" label="端点" width="140" />
          <el-table-column prop="objectCode" label="业务对象" width="150" />
          <el-table-column label="方向" width="90" align="center">
            <template #default="{ row }">{{ row.direction === 'OUT' ? '下发' : '回传' }}</template>
          </el-table-column>
          <el-table-column prop="payloadVersion" label="报文版本" width="100" align="center" />
          <el-table-column label="规则数" width="90" align="center">
            <template #default="{ row }">{{ row.ruleCount }}</template>
          </el-table-column>
          <el-table-column label="启用" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column v-if="canConfig" label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMappingEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeMapping(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!mappingLoading && !mappings.length" description="还没有字段映射" :image-size="60" />
      </el-tab-pane>

      <!-- ------------------------------------------------------ 集成任务 -->
      <el-tab-pane label="集成任务" name="job">
        <div class="toolbar">
          <el-select v-model="jobStatus" placeholder="全部状态" clearable style="width: 140px" @change="loadJobs">
            <el-option v-for="(label, value) in JOB_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <el-input
            v-model="jobBizType"
            placeholder="业务类型，如 Material"
            clearable
            style="width: 190px"
            @keyup.enter="loadJobs"
          />
          <el-button type="primary" @click="loadJobs">查询</el-button>
          <el-button v-if="canRun" type="primary" plain @click="openJobSubmit">提交任务</el-button>
          <el-button v-if="canRun" plain @click="openKingdee">下发本地对象</el-button>
          <span class="hint">同一端点+业务类型+业务键只发一次（幂等）；失败按退避重试，次数用尽进死信需人工重放</span>
        </div>
        <el-table v-loading="jobLoading" :data="jobs" border stripe>
          <el-table-column prop="jobNo" label="任务号" width="130" />
          <el-table-column prop="endpointCode" label="端点" width="130" />
          <el-table-column prop="bizType" label="业务类型" width="120" />
          <el-table-column prop="bizKey" label="业务键" width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="jobStatusType(row.status)" size="small">
                {{ JOB_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="尝试" width="90" align="center">
            <template #default="{ row }">{{ row.attempt }}/{{ row.maxAttempt }}</template>
          </el-table-column>
          <el-table-column label="HTTP" width="80" align="center">
            <template #default="{ row }">{{ row.httpStatus ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="center">
            <template #default="{ row }">{{ row.durationMs }} ms</template>
          </el-table-column>
          <el-table-column label="创建时间" width="150" align="center">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openJobDetail(row.id)">详情</el-button>
              <el-button
                v-if="canRun && (row.status === 'PENDING' || row.status === 'FAILED')"
                link
                type="success"
                @click="dispatchJob(row)"
              >发送</el-button>
              <el-button
                v-if="canRun && (row.status === 'FAILED' || row.status === 'DEAD')"
                link
                type="warning"
                @click="replayJob(row)"
              >重放</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!jobLoading && !jobs.length" description="还没有集成任务" :image-size="60" />
        <el-pagination
          v-if="jobTotal > jobSize"
          class="pager"
          layout="total, prev, pager, next"
          :total="jobTotal"
          :page-size="jobSize"
          :current-page="jobPageNo"
          @current-change="(page: number) => { jobPageNo = page; loadJobs() }"
        />
      </el-tab-pane>
      <!-- ------------------------------------------------------ API 客户端 -->
      <el-tab-pane label="API 客户端" name="client">
        <div class="toolbar">
          <el-input
            v-model="clientKeyword"
            placeholder="编码/名称"
            clearable
            style="width: 200px"
            @keyup.enter="loadClients"
          />
          <el-button type="primary" @click="loadClients">查询</el-button>
          <el-button v-if="canConfig" type="primary" plain @click="openClientCreate">新建客户端</el-button>
          <span class="hint">Key 只在创建/轮换时显示一次；过期或停用立即失效（走库校验，不是 JWT）</span>
        </div>
        <el-table v-loading="clientLoading" :data="clients" border stripe>
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="keyPrefix" label="Key 前缀" width="130" />
          <el-table-column label="作用域" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="scope in row.scopes" :key="scope" size="small" class="tag-gap">
                {{ SCOPE_LABEL[scope] ?? scope }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="限流" width="100" align="center">
            <template #default="{ row }">{{ row.rateLimitPerMin }}/分</template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="到期" width="110" align="center">
            <template #default="{ row }">{{ row.expiresAt || '长期' }}</template>
          </el-table-column>
          <el-table-column label="最近使用" width="150" align="center">
            <template #default="{ row }">{{ formatTime(row.lastUsedAt) }}</template>
          </el-table-column>
          <el-table-column v-if="canConfig" label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openClientEdit(row)">编辑</el-button>
              <el-button link type="warning" @click="rotateClient(row)">轮换</el-button>
              <el-button link type="danger" @click="removeClient(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!clientLoading && !clients.length" description="还没有 API 客户端" :image-size="60" />
      </el-tab-pane>

      <el-tab-pane label="Webhook" name="webhook">
        <div class="toolbar">
          <el-input
            v-model="webhookKeyword"
            placeholder="编码/名称/回调地址"
            clearable
            style="width: 220px"
            @keyup.enter="loadWebhooks"
          />
          <el-button type="primary" @click="loadWebhooks">查询</el-button>
          <el-button v-if="canWebhook" type="primary" plain @click="openWebhookCreate">新建订阅</el-button>
          <span class="hint">订阅文档发布 / 变更生效 / 任务完成；报文用 HMAC-SHA256 签名，失败自动退避重试</span>
        </div>
        <el-table v-loading="webhookLoading" :data="webhooks" border stripe>
          <el-table-column prop="code" label="编码" width="150" />
          <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="url" label="回调地址" min-width="240" show-overflow-tooltip />
          <el-table-column label="事件" min-width="200">
            <template #default="{ row }">
              <el-tag v-for="event in row.events" :key="event" size="small" class="tag-gap">
                {{ WEBHOOK_EVENT_LABEL[event] ?? event }}
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
          <el-table-column label="重试" width="90" align="center">
            <template #default="{ row }">{{ row.maxAttempts }} 次</template>
          </el-table-column>
          <el-table-column v-if="canWebhook" label="操作" width="280" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWebhookEdit(row)">编辑</el-button>
              <el-button link type="success" @click="testWebhook(row)">测试</el-button>
              <el-button link type="warning" @click="openDeliveries(row)">投递</el-button>
              <el-button link type="danger" @click="removeWebhook(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!webhookLoading && !webhooks.length" description="还没有 Webhook 订阅" :image-size="60" />
      </el-tab-pane>
    </el-tabs>

    <!-- 端点编辑 -->
    <el-dialog v-model="endpointDialogVisible" :title="endpointForm.id ? '编辑集成端点' : '新建集成端点'" width="640px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="编码" required>
          <el-input v-model="endpointForm.code" :disabled="!!endpointForm.id" maxlength="64" style="width: 260px" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="endpointForm.name" maxlength="200" style="width: 360px" />
        </el-form-item>
        <el-form-item label="目标系统">
          <el-select v-model="endpointForm.systemCode" style="width: 200px">
            <el-option v-for="(label, value) in SYSTEM_CODE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">超时(ms)</span>
          <el-input-number v-model="endpointForm.timeoutMs" :min="1000" :max="120000" :step="1000" controls-position="right" style="width: 150px" />
        </el-form-item>
        <el-form-item label="端点地址" required>
          <el-input v-model="endpointForm.baseUrl" maxlength="500" placeholder="http://host:port/api/xxx" />
        </el-form-item>
        <el-form-item label="认证方式">
          <el-select v-model="endpointForm.authType" style="width: 200px">
            <el-option v-for="(label, value) in AUTH_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span v-if="endpointForm.id && endpointForm.authConfigured" class="hint label-gap">
            已配置认证信息，留空即保持不变
          </span>
        </el-form-item>
        <el-form-item v-if="endpointForm.authType === 'BASIC'" label="用户名/密码">
          <el-input v-model="endpointForm.username" placeholder="用户名" style="width: 170px" />
          <el-input v-model="endpointForm.password" placeholder="密码" type="password" show-password style="width: 170px; margin-left: 8px" />
        </el-form-item>
        <el-form-item v-else-if="endpointForm.authType === 'BEARER'" label="Token">
          <el-input v-model="endpointForm.token" placeholder="Bearer Token" style="width: 360px" />
        </el-form-item>
        <el-form-item v-else-if="endpointForm.authType === 'HEADER'" label="请求头">
          <el-input v-model="endpointForm.headerName" placeholder="Header 名" style="width: 170px" />
          <el-input v-model="endpointForm.headerValue" placeholder="Header 值" style="width: 170px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="endpointForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="endpointForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="endpointDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="endpointSaving" @click="saveEndpoint">保存</el-button>
      </template>
    </el-dialog>

    <!-- 映射编辑 -->
    <el-dialog v-model="mappingDialogVisible" :title="mappingForm.id ? '编辑字段映射' : '新建字段映射'" width="820px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="端点" required>
          <el-select v-model="mappingForm.endpointId" style="width: 220px">
            <el-option v-for="item in endpointOptions" :key="item.id" :label="`${item.code} ${item.name}`" :value="item.id" />
          </el-select>
          <span class="label-gap">业务对象</span>
          <el-input v-model="mappingForm.objectCode" placeholder="Material / Bom / Doc / Route" style="width: 200px" />
          <span class="label-gap">方向</span>
          <el-select v-model="mappingForm.direction" style="width: 110px">
            <el-option label="下发" value="OUT" />
            <el-option label="回传" value="IN" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则">
          <div class="rules">
            <el-table :data="mappingForm.rules" border size="small">
              <el-table-column label="源字段（点号路径）" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.source" placeholder="code 或 material.name" />
                </template>
              </el-table-column>
              <el-table-column label="目标字段" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.target" placeholder="Number 或 Bill.head.No" />
                </template>
              </el-table-column>
              <el-table-column label="转换" width="130">
                <template #default="{ row }">
                  <el-select v-model="row.transform" clearable placeholder="不转换" style="width: 100%">
                    <el-option v-for="(label, value) in TRANSFORM_LABEL" :key="value" :label="label" :value="value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="70" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.required" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="mappingForm.rules.splice($index, 1)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="rule-footer">
              <el-button link type="primary" @click="addRule">+ 增加规则</el-button>
              <span class="hint">支持 UPPER/LOWER/TRIM/STRING/NUMBER/BOOLEAN 六种转换</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="mappingForm.enabled" />
          <span class="label-gap">备注</span>
          <el-input v-model="mappingForm.remark" maxlength="1000" style="width: 320px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mappingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="mappingSaving" @click="saveMapping">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交任务 -->
    <el-dialog v-model="jobSubmitVisible" title="提交集成任务" width="720px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="端点" required>
          <el-select v-model="jobForm.endpointId" style="width: 220px">
            <el-option v-for="item in endpointOptions" :key="item.id" :label="item.code" :value="item.id" />
          </el-select>
          <span class="label-gap">业务类型</span>
          <el-input v-model="jobForm.bizType" placeholder="Material" style="width: 160px" />
        </el-form-item>
        <el-form-item label="业务键" required>
          <el-input v-model="jobForm.bizKey" placeholder="幂等键，如物料编码" style="width: 320px" />
        </el-form-item>
        <el-form-item label="源报文" required>
          <el-input
            v-model="jobForm.payload"
            type="textarea"
            :rows="10"
            placeholder='{"code":"M-001","name":"支架","qty":12}'
          />
        </el-form-item>
        <el-form-item label="仅预览">
          <el-switch v-model="jobForm.dryRun" />
          <span class="hint label-gap">开启只生成映射后的报文快照，不发送</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jobSubmitVisible = false">取消</el-button>
        <el-button type="primary" :loading="jobSubmitting" @click="submitJob">提交</el-button>
      </template>
    </el-dialog>

    <!-- 下发本地对象 -->
    <el-dialog v-model="kingdeeVisible" title="下发本地对象到金蝶" width="720px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="端点" required>
          <el-select v-model="kingdeeForm.endpointId" style="width: 220px">
            <el-option v-for="item in endpointOptions" :key="item.id" :label="item.code" :value="item.id" />
          </el-select>
          <span class="label-gap">业务类型</span>
          <el-select v-model="kingdeeForm.bizType" style="width: 140px">
            <el-option v-for="(label, value) in KINGDEE_BIZ_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象 ID" required>
          <el-input v-model="kingdeeForm.bizId" placeholder="本地对象 id（零件/BOM 根件/文档/工艺路线）" style="width: 300px" />
          <span v-if="kingdeeForm.bizType === 'Bom'" class="label-gap">视图</span>
          <el-select v-if="kingdeeForm.bizType === 'Bom'" v-model="kingdeeForm.viewType" style="width: 120px">
            <el-option label="设计" value="DESIGN" />
            <el-option label="工艺" value="PROCESS" />
            <el-option label="制造" value="MANUFACTURE" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-button :loading="kingdeePreviewing" @click="previewKingdee">预览源报文</el-button>
          <el-checkbox v-model="kingdeeForm.dryRun" class="label-gap">仅生成快照（不发送）</el-checkbox>
        </el-form-item>
        <el-form-item v-if="kingdeePreviewed" label="源报文">
          <pre class="payload kingdee-payload">{{ prettyJson(kingdeePreviewed.payload) }}</pre>
          <div class="hint">幂等键：{{ kingdeePreviewed.bizKey }}（同一版本重复下发会返回原任务）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kingdeeVisible = false">取消</el-button>
        <el-button type="primary" :loading="kingdeePushing" @click="pushKingdee">下发</el-button>
      </template>
    </el-dialog>

    <!-- API 客户端编辑 -->
    <el-dialog v-model="clientDialogVisible" :title="clientForm.id ? '编辑 API 客户端' : '新建 API 客户端'" width="600px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="编码" required>
          <el-input v-model="clientForm.code" :disabled="!!clientForm.id" maxlength="64" style="width: 240px" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="clientForm.name" maxlength="200" style="width: 340px" />
        </el-form-item>
        <el-form-item label="作用域" required>
          <el-checkbox-group v-model="clientForm.scopes">
            <el-checkbox v-for="(label, value) in SCOPE_LABEL" :key="value" :value="value">
              {{ label }}（{{ value }}）
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="限流(次/分)">
          <el-input-number v-model="clientForm.rateLimitPerMin" :min="1" :max="10000" controls-position="right" style="width: 160px" />
        </el-form-item>
        <el-form-item label="到期日">
          <el-date-picker v-model="clientForm.expiresAt" type="date" value-format="YYYY-MM-DD" placeholder="留空为长期" style="width: 180px" />
          <span class="label-gap">启用</span>
          <el-switch v-model="clientForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="clientForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clientDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="clientSaving" @click="saveClient">保存</el-button>
      </template>
    </el-dialog>

    <!-- API Key 展示（仅一次） -->
    <el-dialog v-model="keyVisible" title="API Key" width="560px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        title="请立即复制保存：服务端只存摘要，关闭后无法再次查看"
        show-icon
      />
      <div class="api-key">{{ newApiKey }}</div>
      <el-button size="small" @click="copyKey">复制 Key</el-button>
    </el-dialog>

    <!-- Webhook 订阅编辑 -->
    <el-dialog v-model="webhookDialogVisible" :title="webhookForm.id ? '编辑 Webhook 订阅' : '新建 Webhook 订阅'" width="680px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="编码" required>
          <el-input v-model="webhookForm.code" :disabled="!!webhookForm.id" maxlength="64" style="width: 240px" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="webhookForm.name" maxlength="200" style="width: 340px" />
        </el-form-item>
        <el-form-item label="回调地址" required>
          <el-input v-model="webhookForm.url" maxlength="500" placeholder="https://example.com/hooks/plm" />
        </el-form-item>
        <el-form-item label="订阅事件" required>
          <el-checkbox-group v-model="webhookForm.events">
            <el-checkbox v-for="(label, value) in WEBHOOK_EVENT_LABEL" :key="value" :value="value">
              {{ label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="webhookForm.maxAttempts" :min="1" :max="10" controls-position="right" style="width: 140px" />
          <span class="label-gap">超时(ms)</span>
          <el-input-number v-model="webhookForm.timeoutMs" :min="1000" :max="30000" :step="500" controls-position="right" style="width: 160px" />
        </el-form-item>
        <el-form-item label="签名密钥">
          <el-input
            v-model="webhookForm.secret"
            maxlength="200"
            show-password
            style="width: 340px"
            :placeholder="webhookForm.id ? '留空则保持原密钥不变' : '留空自动生成'"
          />
          <div class="hint">对端用该密钥对原始报文做 HMAC-SHA256，与请求头 X-NexPLM-Signature 比对</div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="webhookForm.enabled" />
          <span class="label-gap">备注</span>
          <el-input v-model="webhookForm.remark" maxlength="1000" style="width: 300px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="webhookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="webhookSaving" @click="saveWebhook">保存</el-button>
      </template>
    </el-dialog>

    <!-- Webhook 密钥展示（仅一次） -->
    <el-dialog v-model="webhookSecretVisible" title="Webhook 签名密钥" width="560px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        title="请立即复制交给对端：服务端不回传密钥，关闭后只能轮换"
        show-icon
      />
      <div class="api-key">{{ newWebhookSecret }}</div>
      <el-button size="small" @click="copyWebhookSecret">复制密钥</el-button>
    </el-dialog>

    <!-- Webhook 投递记录 -->
    <el-drawer v-model="deliveryDrawerVisible" :title="`投递记录 · ${currentWebhookCode}`" size="780px" destroy-on-close>
      <div class="toolbar">
        <el-select v-model="deliveryStatus" clearable placeholder="全部状态" style="width: 160px" @change="loadDeliveries">
          <el-option v-for="(label, value) in DELIVERY_STATUS_LABEL" :key="value" :label="label" :value="value" />
        </el-select>
        <el-button @click="loadDeliveries">刷新</el-button>
        <span class="hint">失败按退避自动重试，次数用尽进死信；死信可用原报文快照重放</span>
      </div>
      <el-table v-loading="deliveryLoading" :data="deliveries" border size="small">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div>事件键：{{ row.eventKey }}</div>
              <div>下次重试：{{ formatTime(row.nextRetryAt) }}</div>
              <div>错误：{{ row.errorMsg || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="事件" width="120">
          <template #default="{ row }">{{ WEBHOOK_EVENT_LABEL[row.eventType] ?? row.eventType }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="deliveryStatusType(row.status)" size="small">
              {{ DELIVERY_STATUS_LABEL[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="尝试" width="80" align="center">
          <template #default="{ row }">{{ row.attempt }}/{{ row.maxAttempts }}</template>
        </el-table-column>
        <el-table-column label="HTTP" width="70" align="center">
          <template #default="{ row }">{{ row.httpStatus ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ row.durationMs }} ms</template>
        </el-table-column>
        <el-table-column label="时间" width="150" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canWebhook && (row.status === 'FAILED' || row.status === 'DEAD')"
              link
              type="warning"
              @click="replayDelivery(row)"
            >
              重放
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!deliveryLoading && !deliveries.length" description="还没有投递记录" :image-size="60" />
    </el-drawer>

    <!-- 任务详情 -->
    <el-drawer v-model="jobDetailVisible" title="集成任务详情" size="640px" destroy-on-close>
      <template v-if="jobDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务号">{{ jobDetail.jobNo }}</el-descriptions-item>
          <el-descriptions-item label="端点">{{ jobDetail.endpointCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务">{{ jobDetail.bizType }} / {{ jobDetail.bizKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="jobStatusType(jobDetail.status)" size="small">
              {{ JOB_STATUS_LABEL[jobDetail.status] ?? jobDetail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="尝试">{{ jobDetail.attempt }}/{{ jobDetail.maxAttempt }}</el-descriptions-item>
          <el-descriptions-item label="HTTP">{{ jobDetail.httpStatus ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ jobDetail.durationMs }} ms</el-descriptions-item>
          <el-descriptions-item label="下次重试">{{ formatTime(jobDetail.nextRetryAt) }}</el-descriptions-item>
          <el-descriptions-item label="错误" :span="2">{{ jobDetail.errorMsg || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">请求报文（提交时固定的快照）</div>
        <pre class="payload">{{ prettyJson(jobDetail.requestPayload) }}</pre>

        <div class="section-title">对端响应</div>
        <pre class="payload">{{ jobDetail.responseBody || '-' }}</pre>

        <div class="section-title">每次尝试</div>
        <el-table :data="jobDetail.logs" border size="small">
          <el-table-column prop="attempt" label="#" width="50" align="center" />
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="HTTP" width="70" align="center">
            <template #default="{ row }">{{ row.httpStatus ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="耗时" width="90" align="center">
            <template #default="{ row }">{{ row.durationMs ?? '-' }} ms</template>
          </el-table-column>
          <el-table-column prop="errorMsg" label="错误" min-width="160" show-overflow-tooltip />
          <el-table-column label="时间" width="140" align="center">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <div v-if="canRun && (jobDetail.status === 'FAILED' || jobDetail.status === 'DEAD' || jobDetail.status === 'PENDING')" class="detail-actions">
          <el-button v-if="jobDetail.status !== 'PENDING'" type="warning" plain @click="replayJob(jobDetail)">重放</el-button>
          <el-button type="primary" plain @click="dispatchJob(jobDetail)">立即发送</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import {
  AUTH_TYPE_LABEL,
  clientApi,
  DELIVERY_STATUS_LABEL,
  itgApi,
  JOB_STATUS_LABEL,
  KINGDEE_BIZ_LABEL,
  SCOPE_LABEL,
  SYSTEM_CODE_LABEL,
  TRANSFORM_LABEL,
  WEBHOOK_EVENT_LABEL,
  webhookApi,
  type ApiClientRow,
  type DeliveryRow,
  type EndpointDetail,
  type EndpointRow,
  type JobDetail,
  type JobRow,
  type MappingRule,
  type MappingRow,
  type WebhookRow
} from '../../api/itg'

const userStore = useUserStore()
const canConfig = computed(() => userStore.can('itg.config.write'))
const canRun = computed(() => userStore.can('itg.job.write'))

const tab = ref('endpoint')

/* ------------------------------------------------------------ 端点 */

const endpoints = ref<EndpointRow[]>([])
const endpointOptions = ref<EndpointRow[]>([])
const endpointLoading = ref(false)
const endpointKeyword = ref('')
const endpointEnabled = ref<boolean | undefined>()
const endpointTotal = ref(0)
const endpointPage = ref(1)
const endpointSize = 20

const endpointDialogVisible = ref(false)
const endpointSaving = ref(false)
const endpointForm = reactive({
  id: '' as string,
  code: '',
  name: '',
  systemCode: 'KINGDEE',
  baseUrl: '',
  authType: 'NONE',
  authConfigured: false,
  username: '',
  password: '',
  token: '',
  headerName: '',
  headerValue: '',
  timeoutMs: 10000,
  enabled: true,
  remark: ''
})

async function loadEndpoints(): Promise<void> {
  endpointLoading.value = true
  try {
    const result = await itgApi.endpointPage({
      keyword: endpointKeyword.value || undefined,
      enabled: endpointEnabled.value,
      page: endpointPage.value,
      size: endpointSize
    })
    endpoints.value = result.items
    endpointTotal.value = result.total
    if (endpointPage.value === 1) {
      endpointOptions.value = result.items
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载端点失败')
  } finally {
    endpointLoading.value = false
  }
}

function openEndpointCreate(): void {
  Object.assign(endpointForm, {
    id: '',
    code: '',
    name: '',
    systemCode: 'KINGDEE',
    baseUrl: '',
    authType: 'NONE',
    authConfigured: false,
    username: '',
    password: '',
    token: '',
    headerName: '',
    headerValue: '',
    timeoutMs: 10000,
    enabled: true,
    remark: ''
  })
  endpointDialogVisible.value = true
}

async function openEndpointEdit(row: EndpointRow): Promise<void> {
  try {
    const detail: EndpointDetail = await itgApi.endpointDetail(row.id)
    Object.assign(endpointForm, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      systemCode: detail.systemCode,
      baseUrl: detail.baseUrl,
      authType: detail.authType,
      authConfigured: detail.authConfigured,
      username: '',
      password: '',
      token: '',
      headerName: '',
      headerValue: '',
      timeoutMs: detail.timeoutMs,
      enabled: detail.enabled,
      remark: detail.remark ?? ''
    })
    endpointDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载端点失败')
  }
}

/** 只把用户真正填了的认证字段回传；全空表示"保持原认证信息不变"。 */
function buildAuthConfig(): Record<string, unknown> | null {
  if (endpointForm.authType === 'NONE') {
    return {}
  }
  if (endpointForm.authType === 'BASIC') {
    if (!endpointForm.username && !endpointForm.password) {
      return null
    }
    return { username: endpointForm.username, password: endpointForm.password }
  }
  if (endpointForm.authType === 'BEARER') {
    return endpointForm.token ? { token: endpointForm.token } : null
  }
  if (endpointForm.authType === 'HEADER') {
    return endpointForm.headerName
      ? { headerName: endpointForm.headerName, headerValue: endpointForm.headerValue }
      : null
  }
  return null
}

async function saveEndpoint(): Promise<void> {
  if (!endpointForm.code.trim() || !endpointForm.name.trim() || !endpointForm.baseUrl.trim()) {
    ElMessage.warning('编码、名称、端点地址为必填')
    return
  }
  endpointSaving.value = true
  try {
    const payload = {
      code: endpointForm.code.trim(),
      name: endpointForm.name.trim(),
      systemCode: endpointForm.systemCode,
      baseUrl: endpointForm.baseUrl.trim(),
      authType: endpointForm.authType,
      authConfig: buildAuthConfig(),
      timeoutMs: endpointForm.timeoutMs,
      enabled: endpointForm.enabled,
      remark: endpointForm.remark.trim() || null
    }
    if (endpointForm.id) {
      await itgApi.updateEndpoint(endpointForm.id, payload)
    } else {
      await itgApi.createEndpoint(payload)
    }
    ElMessage.success('已保存')
    endpointDialogVisible.value = false
    await loadEndpoints()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存端点失败')
  } finally {
    endpointSaving.value = false
  }
}

async function toggleEndpoint(row: EndpointRow, value: boolean): Promise<void> {
  try {
    await itgApi.updateEndpoint(row.id, {
      code: row.code,
      name: row.name,
      systemCode: row.systemCode,
      baseUrl: row.baseUrl,
      authType: row.authType,
      authConfig: null,
      timeoutMs: row.timeoutMs,
      enabled: value,
      remark: row.remark ?? null
    })
    row.enabled = value
    ElMessage.success(value ? '已启用' : '已停用')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

async function removeEndpoint(row: EndpointRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除端点 ${row.code}？其字段映射会一并停用，历史任务保留可查。`,
      '删除端点',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await itgApi.removeEndpoint(row.id)
    ElMessage.success('已删除')
    await loadEndpoints()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 映射 */

const mappings = ref<MappingRow[]>([])
const mappingLoading = ref(false)
const mappingEndpointId = ref<string | undefined>()
const mappingObjectCode = ref('')
const mappingDialogVisible = ref(false)
const mappingSaving = ref(false)
const mappingForm = reactive({
  id: '' as string,
  endpointId: '' as string,
  objectCode: '',
  direction: 'OUT',
  enabled: true,
  remark: '',
  rules: [] as MappingRule[]
})

async function loadMappings(): Promise<void> {
  mappingLoading.value = true
  try {
    const result = await itgApi.mappingPage({
      endpointId: mappingEndpointId.value,
      objectCode: mappingObjectCode.value || undefined,
      page: 1,
      size: 50
    })
    mappings.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载映射失败')
  } finally {
    mappingLoading.value = false
  }
}

function addRule(): void {
  mappingForm.rules.push({ source: '', target: '', transform: '', required: false })
}

function openMappingCreate(): void {
  Object.assign(mappingForm, {
    id: '',
    endpointId: endpointOptions.value[0]?.id ?? '',
    objectCode: '',
    direction: 'OUT',
    enabled: true,
    remark: '',
    rules: [{ source: 'code', target: 'Number', transform: '', required: true }]
  })
  mappingDialogVisible.value = true
}

async function openMappingEdit(row: MappingRow): Promise<void> {
  try {
    const detail = await itgApi.mappingDetail(row.id)
    Object.assign(mappingForm, {
      id: detail.id,
      endpointId: detail.endpointId,
      objectCode: detail.objectCode,
      direction: detail.direction,
      enabled: detail.enabled,
      remark: detail.remark ?? '',
      rules: detail.rules.map((rule) => ({ ...rule }))
    })
    mappingDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载映射失败')
  }
}

async function saveMapping(): Promise<void> {
  if (!mappingForm.endpointId || !mappingForm.objectCode.trim()) {
    ElMessage.warning('端点和业务对象为必填')
    return
  }
  const rules = mappingForm.rules.filter((rule) => rule.source.trim() && rule.target.trim())
  if (!rules.length) {
    ElMessage.warning('至少需要一条完整规则')
    return
  }
  mappingSaving.value = true
  try {
    const payload = {
      endpointId: mappingForm.endpointId,
      objectCode: mappingForm.objectCode.trim(),
      direction: mappingForm.direction,
      rules: rules.map((rule) => ({
        source: rule.source.trim(),
        target: rule.target.trim(),
        transform: rule.transform || null,
        required: Boolean(rule.required)
      })),
      enabled: mappingForm.enabled,
      remark: mappingForm.remark.trim() || null
    }
    if (mappingForm.id) {
      await itgApi.updateMapping(mappingForm.id, payload)
    } else {
      await itgApi.createMapping(payload)
    }
    ElMessage.success('已保存')
    mappingDialogVisible.value = false
    await loadMappings()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存映射失败')
  } finally {
    mappingSaving.value = false
  }
}

async function removeMapping(row: MappingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除 ${row.endpointCode} / ${row.objectCode} 的映射？`, '删除映射', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await itgApi.removeMapping(row.id)
    ElMessage.success('已删除')
    await loadMappings()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 任务 */

const jobs = ref<JobRow[]>([])
const jobLoading = ref(false)
const jobStatus = ref<string | undefined>()
const jobBizType = ref('')
const jobTotal = ref(0)
const jobPageNo = ref(1)
const jobSize = 20

const jobSubmitVisible = ref(false)
const jobSubmitting = ref(false)
const jobForm = reactive({
  endpointId: '' as string,
  bizType: 'Material',
  bizKey: '',
  payload: '',
  dryRun: false
})

const jobDetailVisible = ref(false)
const jobDetail = ref<JobDetail | null>(null)

async function loadJobs(): Promise<void> {
  jobLoading.value = true
  try {
    const result = await itgApi.jobPage({
      endpointId: undefined,
      bizType: jobBizType.value || undefined,
      status: jobStatus.value,
      page: jobPageNo.value,
      size: jobSize
    })
    jobs.value = result.items
    jobTotal.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载任务失败')
  } finally {
    jobLoading.value = false
  }
}

function openJobSubmit(): void {
  jobForm.endpointId = endpointOptions.value[0]?.id ?? ''
  jobForm.bizType = 'Material'
  jobForm.bizKey = ''
  jobForm.payload = '{\n  "code": "M-001",\n  "name": "支架"\n}'
  jobForm.dryRun = false
  jobSubmitVisible.value = true
}

async function submitJob(): Promise<void> {
  if (!jobForm.endpointId || !jobForm.bizType.trim() || !jobForm.bizKey.trim()) {
    ElMessage.warning('端点、业务类型、业务键为必填')
    return
  }
  let payload: unknown
  try {
    payload = JSON.parse(jobForm.payload)
  } catch {
    ElMessage.error('源报文不是合法 JSON')
    return
  }
  jobSubmitting.value = true
  try {
    const result = await itgApi.submitJob({
      endpointId: jobForm.endpointId,
      bizType: jobForm.bizType.trim(),
      bizKey: jobForm.bizKey.trim(),
      payload,
      dryRun: jobForm.dryRun
    })
    ElMessage.success(result.duplicated ? '该业务键已提交过，返回原任务' : '已提交')
    jobSubmitVisible.value = false
    await loadJobs()
    jobDetail.value = result.job
    jobDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    jobSubmitting.value = false
  }
}

async function dispatchJob(row: { id: string; status: string }): Promise<void> {
  try {
    jobDetail.value = await itgApi.dispatchJob(row.id)
    ElMessage.success('已发送')
    await loadJobs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败')
  }
}

async function replayJob(row: { id: string; status: string }): Promise<void> {
  try {
    jobDetail.value = await itgApi.replayJob(row.id)
    ElMessage.success('已重放')
    await loadJobs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重放失败')
  }
}

async function openJobDetail(id: string): Promise<void> {
  try {
    jobDetail.value = await itgApi.jobDetail(id)
    jobDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载任务失败')
  }
}

/* ------------------------------------------------------------ API 客户端 */

const clients = ref<ApiClientRow[]>([])
const clientLoading = ref(false)
const clientKeyword = ref('')
const clientDialogVisible = ref(false)
const clientSaving = ref(false)
const keyVisible = ref(false)
const newApiKey = ref('')
const clientForm = reactive({
  id: '' as string,
  code: '',
  name: '',
  scopes: ['object.read'] as string[],
  rateLimitPerMin: 60,
  enabled: true,
  expiresAt: '' as string,
  remark: ''
})

async function loadClients(): Promise<void> {
  clientLoading.value = true
  try {
    const result = await clientApi.page({ keyword: clientKeyword.value || undefined, page: 1, size: 50 })
    clients.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载客户端失败')
  } finally {
    clientLoading.value = false
  }
}

function openClientCreate(): void {
  Object.assign(clientForm, {
    id: '',
    code: '',
    name: '',
    scopes: ['object.read'],
    rateLimitPerMin: 60,
    enabled: true,
    expiresAt: '',
    remark: ''
  })
  clientDialogVisible.value = true
}

async function openClientEdit(row: ApiClientRow): Promise<void> {
  try {
    const detail = await clientApi.detail(row.id)
    Object.assign(clientForm, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      scopes: [...detail.scopes],
      rateLimitPerMin: detail.rateLimitPerMin,
      enabled: detail.enabled,
      expiresAt: detail.expiresAt ?? '',
      remark: detail.remark ?? ''
    })
    clientDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载客户端失败')
  }
}

async function saveClient(): Promise<void> {
  if (!clientForm.code.trim() || !clientForm.name.trim()) {
    ElMessage.warning('编码与名称为必填')
    return
  }
  if (!clientForm.scopes.length) {
    ElMessage.warning('至少选择一个作用域')
    return
  }
  clientSaving.value = true
  try {
    const payload = {
      code: clientForm.code.trim(),
      name: clientForm.name.trim(),
      scopes: clientForm.scopes,
      rateLimitPerMin: clientForm.rateLimitPerMin,
      enabled: clientForm.enabled,
      expiresAt: clientForm.expiresAt || null,
      remark: clientForm.remark.trim() || null
    }
    if (clientForm.id) {
      await clientApi.update(clientForm.id, payload)
      ElMessage.success('已保存')
    } else {
      const created = await clientApi.create(payload)
      newApiKey.value = created.apiKey
      keyVisible.value = true
    }
    clientDialogVisible.value = false
    await loadClients()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存客户端失败')
  } finally {
    clientSaving.value = false
  }
}

async function rotateClient(row: ApiClientRow): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `轮换 ${row.code} 的密钥？旧 Key 会立即失效，已接入的外部系统需要用新 Key。`,
      '轮换 API Key',
      { confirmButtonText: '轮换', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const rotated = await clientApi.rotate(row.id)
    newApiKey.value = rotated.apiKey
    keyVisible.value = true
    await loadClients()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '轮换失败')
  }
}

async function removeClient(row: ApiClientRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除客户端 ${row.code}？其 Key 将立即失效。`, '删除客户端', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await clientApi.remove(row.id)
    ElMessage.success('已删除')
    await loadClients()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function copyKey(): Promise<void> {
  try {
    await navigator.clipboard.writeText(newApiKey.value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

/* ------------------------------------------------------------ 金蝶下发 */

const kingdeeVisible = ref(false)
const kingdeePreviewing = ref(false)
const kingdeePushing = ref(false)
const kingdeePreviewed = ref<{ bizType: string; bizKey: string; payload: unknown } | null>(null)
const kingdeeForm = reactive({
  endpointId: '' as string,
  bizType: 'Material',
  bizId: '',
  viewType: 'DESIGN',
  dryRun: false
})

function openKingdee(): void {
  kingdeeForm.endpointId = endpointOptions.value[0]?.id ?? ''
  kingdeeForm.bizType = 'Material'
  kingdeeForm.bizId = ''
  kingdeeForm.viewType = 'DESIGN'
  kingdeeForm.dryRun = false
  kingdeePreviewed.value = null
  kingdeeVisible.value = true
}

async function previewKingdee(): Promise<void> {
  if (!kingdeeForm.bizType || !kingdeeForm.bizId.trim()) {
    ElMessage.warning('请填写对象 ID')
    return
  }
  kingdeePreviewing.value = true
  try {
    kingdeePreviewed.value = await itgApi.kingdeePreview({
      bizType: kingdeeForm.bizType,
      bizId: kingdeeForm.bizId.trim(),
      viewType: kingdeeForm.bizType === 'Bom' ? kingdeeForm.viewType : undefined
    })
  } catch (error) {
    kingdeePreviewed.value = null
    ElMessage.error(error instanceof Error ? error.message : '预览失败')
  } finally {
    kingdeePreviewing.value = false
  }
}

async function pushKingdee(): Promise<void> {
  if (!kingdeeForm.endpointId || !kingdeeForm.bizId.trim()) {
    ElMessage.warning('请选择端点并填写对象 ID')
    return
  }
  kingdeePushing.value = true
  try {
    const result = await itgApi.kingdeePush({
      endpointId: kingdeeForm.endpointId,
      bizType: kingdeeForm.bizType,
      bizId: kingdeeForm.bizId.trim(),
      viewType: kingdeeForm.bizType === 'Bom' ? kingdeeForm.viewType : undefined,
      dryRun: kingdeeForm.dryRun
    })
    ElMessage.success(result.duplicated ? '该版本已下发过，返回原任务' : '已下发')
    kingdeeVisible.value = false
    await loadJobs()
    jobDetail.value = result.job
    jobDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下发失败')
  } finally {
    kingdeePushing.value = false
  }
}

/* ------------------------------------------------------------ Webhook */

const canWebhook = computed(() => userStore.can('itg.webhook.write'))
const webhooks = ref<WebhookRow[]>([])
const webhookLoading = ref(false)
const webhookKeyword = ref('')
const webhookDialogVisible = ref(false)
const webhookSaving = ref(false)
const webhookSecretVisible = ref(false)
const newWebhookSecret = ref('')
const webhookForm = reactive({
  id: '',
  code: '',
  name: '',
  url: '',
  events: ['doc.released'] as string[],
  enabled: true,
  maxAttempts: 5,
  timeoutMs: 5000,
  remark: '',
  secret: ''
})

const deliveryDrawerVisible = ref(false)
const deliveryLoading = ref(false)
const deliveries = ref<DeliveryRow[]>([])
const deliveryStatus = ref('')
const currentWebhookId = ref('')
const currentWebhookCode = ref('')

async function loadWebhooks(): Promise<void> {
  webhookLoading.value = true
  try {
    const result = await webhookApi.page({
      keyword: webhookKeyword.value || undefined,
      page: 1,
      size: 50
    })
    webhooks.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 Webhook 失败')
  } finally {
    webhookLoading.value = false
  }
}

function openWebhookCreate(): void {
  Object.assign(webhookForm, {
    id: '',
    code: '',
    name: '',
    url: '',
    events: ['doc.released'],
    enabled: true,
    maxAttempts: 5,
    timeoutMs: 5000,
    remark: '',
    secret: ''
  })
  webhookDialogVisible.value = true
}

async function openWebhookEdit(row: WebhookRow): Promise<void> {
  try {
    const detail = await webhookApi.detail(row.id)
    Object.assign(webhookForm, {
      id: detail.id,
      code: detail.code,
      name: detail.name,
      url: detail.url,
      events: [...detail.events],
      enabled: detail.enabled,
      maxAttempts: detail.maxAttempts,
      timeoutMs: detail.timeoutMs,
      remark: detail.remark ?? '',
      secret: ''
    })
    webhookDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载订阅失败')
  }
}

async function saveWebhook(): Promise<void> {
  if (!webhookForm.code.trim() || !webhookForm.name.trim() || !webhookForm.url.trim()) {
    ElMessage.warning('编码、名称与回调地址为必填')
    return
  }
  if (!webhookForm.events.length) {
    ElMessage.warning('至少订阅一个事件')
    return
  }
  webhookSaving.value = true
  try {
    const payload = {
      code: webhookForm.code.trim(),
      name: webhookForm.name.trim(),
      url: webhookForm.url.trim(),
      events: webhookForm.events,
      enabled: webhookForm.enabled,
      maxAttempts: webhookForm.maxAttempts,
      timeoutMs: webhookForm.timeoutMs,
      remark: webhookForm.remark.trim() || null,
      secret: webhookForm.secret.trim() || null
    }
    if (webhookForm.id) {
      await webhookApi.update(webhookForm.id, payload)
      ElMessage.success('已保存')
    } else {
      const created = await webhookApi.create(payload)
      newWebhookSecret.value = created.secret
      webhookSecretVisible.value = true
    }
    webhookDialogVisible.value = false
    await loadWebhooks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存订阅失败')
  } finally {
    webhookSaving.value = false
  }
}

async function removeWebhook(row: WebhookRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除订阅 ${row.code}？历史投递记录保留，但不再接收事件。`, '删除订阅', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await webhookApi.remove(row.id)
    ElMessage.success('已删除')
    await loadWebhooks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function testWebhook(row: WebhookRow): Promise<void> {
  try {
    const delivery = await webhookApi.test(row.id)
    if (delivery.status === 'SENT') {
      ElMessage.success(`测试投递已送达（HTTP ${delivery.httpStatus ?? 200}）`)
    } else {
      ElMessage.warning(`测试投递失败：${delivery.errorMsg ?? delivery.status}`)
    }
    await openDeliveries(row)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '测试投递失败')
  }
}

async function openDeliveries(row: WebhookRow): Promise<void> {
  currentWebhookId.value = row.id
  currentWebhookCode.value = row.code
  deliveryStatus.value = ''
  deliveryDrawerVisible.value = true
  await loadDeliveries()
}

async function loadDeliveries(): Promise<void> {
  if (!currentWebhookId.value) {
    return
  }
  deliveryLoading.value = true
  try {
    const result = await webhookApi.deliveryPage(currentWebhookId.value, {
      status: deliveryStatus.value || undefined,
      page: 1,
      size: 100
    })
    deliveries.value = result.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载投递记录失败')
  } finally {
    deliveryLoading.value = false
  }
}

async function replayDelivery(row: DeliveryRow): Promise<void> {
  try {
    const replayed = await webhookApi.replay(row.id)
    if (replayed.status === 'SENT') {
      ElMessage.success('重放成功')
    } else {
      ElMessage.warning(`重放失败：${replayed.errorMsg ?? replayed.status}`)
    }
    await loadDeliveries()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重放失败')
  }
}

async function copyWebhookSecret(): Promise<void> {
  try {
    await navigator.clipboard.writeText(newWebhookSecret.value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

function deliveryStatusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'SENT') {
    return 'success'
  }
  if (value === 'DEAD') {
    return 'danger'
  }
  if (value === 'FAILED') {
    return 'warning'
  }
  return 'info'
}

/* ------------------------------------------------------------ 展示辅助 */

function jobStatusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'DEAD') {
    return 'danger'
  }
  if (value === 'FAILED') {
    return 'warning'
  }
  if (value === 'RUNNING') {
    return 'warning'
  }
  return 'info'
}

function formatTime(value?: string | null): string {
  return value ? value.substring(0, 16).replace('T', ' ') : '-'
}

function prettyJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

onMounted(async () => {
  await loadEndpoints()
  await loadMappings()
  await loadJobs()
  await loadClients()
  await loadWebhooks()
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
  flex-wrap: wrap;
}

.hint {
  margin-left: 4px;
  color: #909399;
  font-size: 12px;
}

.tag-gap {
  margin-left: 6px;
}

.label-gap {
  margin-left: 12px;
  margin-right: 6px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.rules,
.rules .el-table {
  width: 100%;
}

.rule-footer {
  margin-top: 8px;
}

.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #303133;
}

.payload {
  max-height: 220px;
  overflow: auto;
  margin: 0;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

.kingdee-payload {
  width: 100%;
}

.api-key {
  margin: 12px 0;
  padding: 12px;
  background: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  word-break: break-all;
  user-select: all;
}

.expand-box {
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.9;
  color: #5c6b7a;
  word-break: break-all;
}
</style>
