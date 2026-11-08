<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="head-left">
            <el-button link @click="goBack">← 返回</el-button>
            <span class="code">{{ detail?.code }}</span>
            <span class="name">{{ detail?.name }}</span>
            <el-tag size="small" type="info">{{ detail?.revisionLabel }}</el-tag>
            <StatusDot :status="detail?.lifecycleState" :label="stateLabel(detail?.lifecycleState)" />
            <StatusDot :tone="phaseTone(detail?.phaseState)" :label="`阶段：${phaseLabel(detail?.phaseState)}`" />
            <StatusDot :tone="usingTone(detail?.usingState)" :label="`使用：${usingLabel(detail?.usingState)}`" />
            <StatusDot
              :tone="workflowTone(detail?.workflowState)"
              :label="`流程：${workflowLabel(detail?.workflowState)}`"
            />
            <StatusDot
              v-if="detail"
              :tone="detail.sourceType === 'BUY' ? 'warn' : 'ok'"
              :label="ITEM_SOURCE_LABEL[detail.sourceType] ?? detail.sourceType"
            />
            <el-tag v-if="detail?.checkout" size="small" :type="detail.checkout.mine ? 'warning' : 'danger'">
              {{ detail.checkout.mine ? '我已检出' : `${detail.checkout.userName || '他人'} 已检出` }}
            </el-tag>
          </div>
          <div class="head-actions">
            <el-button v-if="canWrite && !detail?.checkout" @click="onCheckout">检出</el-button>
            <el-button v-if="canWrite && detail?.checkout?.mine" @click="onCheckin">检入</el-button>
            <el-button v-if="canWrite && detail?.checkout?.mine" @click="onCancelCheckout">撤销检出</el-button>
            <el-button v-if="canWrite && detail?.lifecycleState === 'Released'" @click="onRevise">修订</el-button>
            <el-dropdown v-if="canWrite" @command="onUsingStateChange">
              <el-button>使用状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="(label, value) in ITEM_USING_STATE_LABEL" :key="value" :command="value">
                    {{ label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-if="canWrite" @click="openEdit">编辑属性</el-button>
          </div>
        </div>
      </template>

    </el-card>

    <el-card shadow="never">
      <ObjectDetailTabs
        v-model="activeTab"
        entity-code="PdmItem"
        :extras="itemExtraTabs"
        :fallback="ITEM_TAB_FALLBACK"
        :lazy-codes="['visual']"
        @change="onTabChange"
      >
        <template #props>
          <div v-if="detail" class="props-layout">
            <div class="props-thumb">
              <el-empty description="暂无缩略图" :image-size="56" />
            </div>
            <div class="props-body">
              <div class="prop-title">基本属性</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
                <el-descriptions-item label="类型">
                  {{ ITEM_TYPE_LABEL[detail.itemType] ?? detail.itemType }}
                </el-descriptions-item>
                <el-descriptions-item label="供应方式">
                  {{ ITEM_SOURCE_LABEL[detail.sourceType] ?? detail.sourceType }}
                </el-descriptions-item>
                <el-descriptions-item label="规格型号">{{ detail.specification || '-' }}</el-descriptions-item>
                <el-descriptions-item label="材料">{{ detail.material || '-' }}</el-descriptions-item>
                <el-descriptions-item label="计量单位">{{ detail.unit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="单件重量">{{ detail.weight ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="标准号">{{ detail.stdCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="版本">{{ detail.revisionLabel }}</el-descriptions-item>
                <el-descriptions-item label="发布状态">{{ stateLabel(detail.lifecycleState) }}</el-descriptions-item>
                <el-descriptions-item label="生命周期">{{ phaseLabel(detail.phaseState) }}</el-descriptions-item>
                <el-descriptions-item label="使用状态">{{ usingLabel(detail.usingState) }}</el-descriptions-item>
                <el-descriptions-item label="流程状态">{{ workflowLabel(detail.workflowState) }}</el-descriptions-item>
                <el-descriptions-item label="发布时间">{{ formatTime(detail.releasedAt) }}</el-descriptions-item>
                <el-descriptions-item label="创建人">{{ detail.createdByName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="修改人">{{ detail.updatedByName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="修改时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
              </el-descriptions>

              <div class="prop-title">设计属性</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="供应商">{{ detail.designAttr?.provider || '-' }}</el-descriptions-item>
                <el-descriptions-item label="图号">{{ detail.designAttr?.dwgNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="产品类别">
                  {{ detail.designAttr?.productCategory || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="产品型号">{{ detail.designAttr?.productModel || '-' }}</el-descriptions-item>
                <el-descriptions-item label="产品规格">{{ detail.designAttr?.productSpec || '-' }}</el-descriptions-item>
                <el-descriptions-item label="毛坯种类">
                  {{ detail.designAttr?.materialType || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="毛坯种类2">
                  {{ detail.designAttr?.materialType2 || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="表面处理">
                  {{ detail.designAttr?.surfaceTreatment || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="是否工装">
                  {{ detail.designAttr?.isTooling ? '是' : '否' }}
                </el-descriptions-item>
                <el-descriptions-item label="重量单位">{{ detail.designAttr?.weightUnit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="所属装配">{{ detail.designAttr?.montage || '-' }}</el-descriptions-item>
                <el-descriptions-item label="是否颜色件">
                  {{ detail.designAttr?.colorItem ? '是' : '否' }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="prop-title">分类</div>
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="所属分类">
                  {{ classify.className || '未归类' }}
                </el-descriptions-item>
                <el-descriptions-item label="分类路径">{{ classify.classPath || '-' }}</el-descriptions-item>
                <el-descriptions-item label="操作">
                  <el-button v-if="canWrite" link type="primary" size="small" @click="openClassify">
                    归类
                  </el-button>
                  <span v-else class="prop-hint">无编辑权限</span>
                </el-descriptions-item>
                <el-descriptions-item v-for="def in classify.attrDefs" :key="def.code" :label="def.name">
                  {{ classify.attrs[def.code] || '-' }}
                </el-descriptions-item>
              </el-descriptions>

              <template v-if="customFields.length">
                <div class="prop-title">ERP 扩展属性</div>
                <el-descriptions :column="3" border size="small">
                  <el-descriptions-item v-for="field in customFields" :key="field.code">
                    <template #label>
                      <span :title="`ERP 列：${field.code}`">{{ field.name }}</span>
                    </template>
                    {{ extValueText(field) }}
                  </el-descriptions-item>
                </el-descriptions>
                <div class="prop-hint">以上字段直接对应金蝶 ERP 物料列（编码见悬停提示），随物料下发报文提交</div>
              </template>

              <div v-if="detail.checkout" class="checkout-line">
                检出信息：{{ detail.checkout.userName || detail.checkout.userId }} 于
                {{ formatTime(detail.checkout.checkedOutAt) }} 检出
                <span v-if="detail.checkout.expireAt">（锁到期 {{ formatTime(detail.checkout.expireAt) }}）</span>
              </div>
            </div>
          </div>
        </template>

        <template #views>
          <div class="bom-toolbar">
            <el-select v-model="newViewType" placeholder="视图类型" style="width: 150px">
              <el-option
                v-for="(label, value) in ITEM_VIEW_TYPE_LABEL"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
            <el-input v-model="newViewDesc" placeholder="视图描述（可空）" style="width: 220px" />
            <el-select
              v-model="newViewCopyFrom"
              placeholder="复制来源视图（可空）"
              clearable
              style="width: 200px"
            >
              <el-option
                v-for="row in views"
                :key="row.id"
                :label="`${row.viewTypeLabel}${row.description ? ' / ' + row.description : ''}`"
                :value="row.id"
                :disabled="row.viewType === newViewType"
              />
            </el-select>
            <el-button type="primary" :disabled="!canWrite" @click="onCreateView">新建视图</el-button>
            <el-button @click="loadViews">刷新</el-button>
            <span class="hint">BOM 行按视图类型归属；选择来源视图可把该视图的 BOM 行复制到新视图</span>
          </div>
          <el-table v-loading="viewLoading" :data="views" border>
            <el-table-column prop="viewTypeLabel" label="视图类型" width="130" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column label="默认" width="80" align="center">
              <template #default="{ row }">
                <StatusDot v-if="row.isDefault" tone="ok" label="默认" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="发布状态" width="110">
              <template #default="{ row }">
                <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
              </template>
            </el-table-column>
            <el-table-column prop="bomLineCount" label="BOM 行数" width="100" align="center" />
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="switchView(row.viewType)">看 BOM</el-button>
                <el-button
                  v-if="canWrite && !row.isDefault"
                  link
                  type="danger"
                  @click="onRemoveView(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无视图" />
            </template>
          </el-table>
        </template>

        <template #docs>
          <div class="bom-toolbar">
            <el-button @click="loadDocs">刷新</el-button>
            <span class="hint">零件通过对象关系关联的图文档（任一方向）</span>
          </div>
          <el-table v-loading="docLoading" :data="docs" border>
            <el-table-column prop="code" label="文档编码" width="160" />
            <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="docType" label="文档类型" width="110" />
            <el-table-column prop="relationType" label="关系" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
              </template>
            </el-table-column>
            <el-table-column prop="revisionLabel" label="版本" width="80" />
            <el-table-column label="更新时间" width="160">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDoc(row.id)">打开</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无关联的设计文档" />
            </template>
          </el-table>
        </template>

        <template #visual>
          <visual-preview object-type="item" :object-id="itemId" />
        </template>

        <template #bom>
          <div class="bom-toolbar">
            <el-radio-group v-model="bomView" @change="loadBom">
              <el-radio-button v-for="(label, value) in BOM_VIEW_LABEL" :key="value" :value="value">
                {{ label }}
              </el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="bomEffectAt"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="有效性时点（空=全部）"
              clearable
              style="width: 190px"
              @change="loadBom"
            />
            <el-button type="primary" :disabled="!canWrite" @click="openAddBom">添加子件</el-button>
            <el-button :disabled="!canWrite || selectedBomRows.length === 0" @click="openBatchQty">
              批量改用量
            </el-button>
            <el-button :disabled="!canWrite || selectedBomRows.length === 0" @click="openBatchReplace">
              批量替换
            </el-button>
            <el-button @click="checkBom">结构校验</el-button>
            <el-button @click="onExportBom">导出 Excel</el-button>
            <el-button :disabled="!canWrite" @click="openImportDialog">导入 Excel</el-button>
            <span class="hint">
              共 {{ bomRows.length }} 个顶层子件，展开可查看下级；已勾选 {{ selectedBomRows.length }} 行
            </span>
          </div>

          <el-alert
            v-if="bomIssues.length > 0"
            type="warning"
            :closable="false"
            show-icon
            class="bom-issues"
          >
            <template #title>结构校验发现 {{ bomIssues.length }} 个问题</template>
            <div v-for="(issue, index) in bomIssues" :key="index" class="issue-line">
              [{{ BOM_ISSUE_LABEL[issue.type] ?? issue.type }}] {{ issue.message }}
            </div>
          </el-alert>

          <el-table
            ref="bomTableRef"
            v-loading="bomLoading"
            :data="bomRows"
            row-key="lineId"
            :tree-props="{ children: 'children' }"
            border
            default-expand-all
            @selection-change="onBomSelectionChange"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
            <el-table-column prop="qty" label="用量" width="100" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column label="损耗率" width="90">
              <template #default="{ row }">{{ row.lossRate ?? 0 }}%</template>
            </el-table-column>
            <el-table-column label="有效期" width="190">
              <template #default="{ row }">{{ effectLabel(row) }}</template>
            </el-table-column>
            <el-table-column prop="refDesignator" label="位号" width="120" />
            <el-table-column label="操作" width="330" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="!canWrite" @click="openEditBom(row)">编辑</el-button>
                <el-button link :disabled="!canWrite" @click="onMoveBom(row, -1)">上移</el-button>
                <el-button link :disabled="!canWrite" @click="onMoveBom(row, 1)">下移</el-button>
                <el-button link type="info" @click="openAlternates(row)">备选件</el-button>
                <el-button link type="danger" :disabled="!canWrite" @click="onDeleteBom(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无子件，点击「添加子件」建立 BOM" />
            </template>
          </el-table>
        </template>

        <template #bom-summary>
          <div class="bom-toolbar">
            <el-radio-group v-model="bomView" @change="loadBomSummary">
              <el-radio-button v-for="(label, value) in BOM_VIEW_LABEL" :key="value" :value="value">
                {{ label }}
              </el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="bomEffectAt"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="有效性时点（空=全部）"
              clearable
              style="width: 190px"
              @change="loadBomSummary"
            />
            <span class="hint">
              共 {{ bomSummaryRows.length }} 行；用量按层级累乘，有效用量已计入损耗率
            </span>
          </div>

          <el-table v-loading="bomSummaryLoading" :data="bomSummaryRows" border>
            <el-table-column label="层级" width="90">
              <template #default="{ row }">
                <span :style="{ paddingLeft: `${Math.max(0, row.level - 1) * 12}px` }">L{{ row.level }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="路径" width="110" />
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
            <el-table-column prop="qty" label="累计用量" width="110" />
            <el-table-column label="损耗率" width="90">
              <template #default="{ row }">{{ row.lossRate ?? 0 }}%</template>
            </el-table-column>
            <el-table-column prop="effectiveQty" label="有效用量" width="110" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="lineNo" label="序号" width="90" />
            <template #empty>
              <el-empty description="该视图下暂无 BOM 结构" />
            </template>
          </el-table>
        </template>

        <template #bom-rollup>
          <div class="bom-toolbar">
            <el-radio-group v-model="bomView" @change="loadBomRollup">
              <el-radio-button v-for="(label, value) in BOM_VIEW_LABEL" :key="value" :value="value">
                {{ label }}
              </el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="bomEffectAt"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="有效性时点（空=全部）"
              clearable
              style="width: 190px"
              @change="loadBomRollup"
            />
            <el-switch v-model="bomLeafOnly" active-text="仅叶子件" @change="loadBomRollup" />
            <span class="hint">共 {{ bomRollupRows.length }} 种子件；同一子件多处出现已合并累计</span>
          </div>

          <el-table v-loading="bomRollupLoading" :data="bomRollupRows" border>
            <el-table-column prop="code" label="编码" width="140" />
            <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ ITEM_TYPE_LABEL[row.itemType] ?? row.itemType ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
            <el-table-column prop="totalQty" label="累计用量" width="110" />
            <el-table-column prop="totalEffectiveQty" label="累计有效用量" width="130" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="occurrences" label="出现次数" width="100" />
            <template #empty>
              <el-empty description="该视图下暂无 BOM 结构" />
            </template>
          </el-table>
        </template>

        <template #whereused>
          <el-table v-loading="whereUsedLoading" :data="whereUsedRows" border>
            <el-table-column prop="parentCode" label="父件编码" width="150" />
            <el-table-column prop="parentName" label="父件名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="视图" width="100">
              <template #default="{ row }">{{ BOM_VIEW_LABEL[row.viewType] ?? row.viewType }}</template>
            </el-table-column>
            <el-table-column prop="qty" label="用量" width="100" />
            <el-table-column prop="lineNo" label="序号" width="90" />
            <template #empty>
              <el-empty description="暂无父件引用该零部件" />
            </template>
          </el-table>
        </template>

        <template #baselines>
          <div class="bom-toolbar">
            <el-input v-model="baselineForm.code" placeholder="基线编码（必填）" style="width: 170px" />
            <el-input v-model="baselineForm.name" placeholder="基线名称" style="width: 180px" />
            <el-button
              type="primary"
              :disabled="!canWrite || !baselineForm.code"
              @click="onCreateBaseline"
            >
              冻结当前结构
            </el-button>
            <el-button @click="loadBaselines">刷新</el-button>
            <span class="hint">
              在当前视图（{{ BOM_VIEW_LABEL[bomView] }}）下冻结，之后可对比结构变化
            </span>
          </div>

          <el-table v-loading="baselineLoading" :data="baselineRows" border>
            <el-table-column prop="code" label="编码" width="150" />
            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="视图" width="90">
              <template #default="{ row }">{{ BOM_VIEW_LABEL[row.viewType] ?? row.viewType }}</template>
            </el-table-column>
            <el-table-column prop="lineCount" label="行数" width="80" />
            <el-table-column label="冻结时间" width="170">
              <template #default="{ row }">{{ formatTime(row.frozenAt) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="onDiffBaseline(row)">对比</el-button>
                <el-button link type="danger" :disabled="!canWrite" @click="onDeleteBaseline(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无基线，点击「冻结当前结构」创建" />
            </template>
          </el-table>
        </template>

        <template #versions>
          <div class="bom-toolbar">
            <el-button @click="loadVersions">刷新</el-button>
            <el-button :disabled="versionCompareRows.length !== 2" @click="openVersionCompare">
              比较选中版本
            </el-button>
            <span class="hint">
              大版本（修订）与小版本（检入）在旧系统分别对应 A / A.1 展示；勾选两个版本可逐字段比较
            </span>
          </div>
          <el-table
            v-loading="versionLoading"
            :data="versions"
            border
            @selection-change="onVersionSelectionChange"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column prop="revisionLabel" label="版本" width="100" />
            <el-table-column label="发布状态" width="120">
              <template #default="{ row }">
                <StatusDot :status="row.lifecycleState" :label="stateLabel(row.lifecycleState)" />
              </template>
            </el-table-column>
            <el-table-column label="当前" width="90" align="center">
              <template #default="{ row }">
                <StatusDot v-if="row.isLatest" tone="ok" label="最新" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdByName" label="创建人" width="110" />
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="发布时间" width="170">
              <template #default="{ row }">{{ formatTime(row.releasedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.id !== itemId" link type="primary" @click="openVersion(row.id)">打开</el-button>
                <span v-else class="hint">当前</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无历史版本" />
            </template>
          </el-table>
        </template>

        <template #workflow>
          <workflow-panel object-type="PdmItem" :object-id="itemId" @changed="load" />
        </template>

        <template #acl>
          <div class="bom-toolbar">
            <el-button @click="loadAcl">刷新</el-button>
            <span class="hint">实例权限只读；授予与撤销在「系统管理 → 安全策略」或对象授权入口完成</span>
          </div>
          <el-table v-loading="subLoading" :data="aclRows" border>
            <el-table-column label="被授权者类型" width="140">
              <template #default="{ row }">{{ aclTypeLabel(row.actorType) }}</template>
            </el-table-column>
            <el-table-column label="被授权者" min-width="180">
              <template #default="{ row }">{{ row.actorName || row.actorId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="actions" label="操作" min-width="180" />
            <el-table-column label="效果" width="100">
              <template #default="{ row }">
                <StatusDot
                  :tone="row.effect === 'DENY' ? 'danger' : 'ok'"
                  :label="row.effect === 'DENY' ? '拒绝' : '允许'"
                />
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无实例权限，按角色或数据作用域继承" />
            </template>
          </el-table>
        </template>

        <template #logs>
          <div class="bom-toolbar">
            <el-button @click="loadLogs">刷新</el-button>
            <span class="hint">状态流转与审计事件合并按时间倒序</span>
          </div>
          <el-table v-loading="subLoading" :data="logs" border>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.logType === 'STATE' ? '状态' : '审计' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="动作" width="140" />
            <el-table-column label="状态变化" width="180">
              <template #default="{ row }">
                <span v-if="row.fromState || row.toState">{{ row.fromState || '-' }} → {{ row.toState || '-' }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <StatusDot
                  :tone="row.result === 'SUCCESS' ? 'ok' : 'danger'"
                  :label="row.result === 'SUCCESS' ? '成功' : row.result || '-'"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作人" width="110">
              <template #default="{ row }">{{ row.operatorLabel || row.operatorId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无日志" />
            </template>
          </el-table>
        </template>
      </ObjectDetailTabs>
    </el-card>

    <el-dialog
      v-model="bomDialogVisible"
      :title="bomEditingLineId ? '编辑 BOM 行' : '添加子件'"
      width="560px"
    >
      <el-form :model="bomForm" label-width="90px">
        <el-form-item v-if="!bomEditingLineId" label="子件" required>
          <el-select
            v-model="bomForm.childId"
            filterable
            remote
            :remote-method="searchChildren"
            :loading="childLoading"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in childOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用量" required>
          <el-input-number v-model="bomForm.qty" :min="0.000001" :precision="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="bomForm.unit" placeholder="留空取子件单位" />
        </el-form-item>
        <el-form-item label="损耗率(%)">
          <el-input-number v-model="bomForm.lossRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期">
          <div class="effect-range">
            <el-date-picker
              v-model="bomForm.effectFrom"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="起始（空=不限）"
              clearable
            />
            <span class="hint">至</span>
            <el-date-picker
              v-model="bomForm.effectTo"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="截止（空=不限）"
              clearable
            />
          </div>
        </el-form-item>
        <el-form-item label="位号">
          <el-input v-model="bomForm.refDesignator" placeholder="如 P1,P2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bomForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bomSaving" @click="onSaveBom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchQtyDialogVisible" title="批量修改用量" width="520px">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>已勾选 {{ selectedBomRows.length }} 行，只有勾选的字段会被修改</template>
      </el-alert>
      <el-form label-width="110px" class="batch-form">
        <el-form-item label="用量">
          <el-checkbox v-model="batchQtyForm.useQty">修改</el-checkbox>
          <el-input-number
            v-model="batchQtyForm.qty"
            :min="0.000001"
            :precision="3"
            :disabled="!batchQtyForm.useQty"
            class="batch-input"
          />
        </el-form-item>
        <el-form-item label="损耗率(%)">
          <el-checkbox v-model="batchQtyForm.useLossRate">修改</el-checkbox>
          <el-input-number
            v-model="batchQtyForm.lossRate"
            :min="0"
            :max="100"
            :precision="2"
            :disabled="!batchQtyForm.useLossRate"
            class="batch-input"
          />
        </el-form-item>
        <el-form-item label="单位">
          <el-checkbox v-model="batchQtyForm.useUnit">修改</el-checkbox>
          <el-input
            v-model="batchQtyForm.unit"
            placeholder="留空则不改"
            :disabled="!batchQtyForm.useUnit"
            class="batch-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchQtyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchQtySaving" @click="onSaveBatchQty">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchReplaceDialogVisible" title="批量替换子件" width="520px">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>
          把当前 BOM 树内选定的子件全部替换为目标子件；重复子件、会成环等行会被跳过并给出原因
        </template>
      </el-alert>
      <el-form label-width="110px" class="batch-form">
        <el-form-item label="被替换子件" required>
          <el-select v-model="batchReplaceForm.fromChildId" placeholder="选择已勾选行中的子件" style="width: 100%">
            <el-option
              v-for="option in replaceSourceOptions"
              :key="option.childId"
              :label="option.label"
              :value="option.childId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="替换为" required>
          <el-select
            v-model="batchReplaceForm.toChildId"
            filterable
            remote
            :remote-method="searchReplaceTarget"
            :loading="replaceTargetLoading"
            placeholder="输入编码或名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in replaceTargetOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchReplaceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchReplaceSaving" @click="onSaveBatchReplace">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="BOM 增量导入" width="880px">
      <div class="bom-toolbar">
        <el-upload
          :auto-upload="false"
          :limit="1"
          :file-list="importFileList"
          accept=".xlsx,.xls"
          :on-change="onImportFileChange"
          :on-remove="onImportFileRemove"
        >
          <el-button>选择 Excel</el-button>
        </el-upload>
        <span class="hint">表头需包含：层级号、编码、数量；可先「导出 Excel」取得模板</span>
      </div>

      <el-alert
        v-if="importPreview && importPreview.errors.length > 0"
        type="error"
        :closable="false"
        show-icon
        class="bom-issues"
      >
        <template #title>发现 {{ importPreview.errors.length }} 个问题，修正后才能导入</template>
        <div v-for="(err, index) in importPreview.errors" :key="index" class="issue-line">{{ err }}</div>
      </el-alert>

      <div v-if="importPreview" class="bom-toolbar">
        <el-tag type="success">新增 {{ importPreview.createCount }}</el-tag>
        <el-tag type="warning">更新 {{ importPreview.updateCount }}</el-tag>
        <el-tag type="info">不变 {{ importPreview.unchangedCount }}</el-tag>
        <el-tag v-if="importPreview.invalid > 0" type="danger">错误 {{ importPreview.invalid }}</el-tag>
        <span class="hint">导入为增量合并，不会删除系统中已有的 BOM 行</span>
      </div>

      <el-table v-loading="importLoading" :data="importPreview?.rows ?? []" border max-height="360">
        <el-table-column prop="rowNum" label="行号" width="70" />
        <el-table-column prop="levelCode" label="层级号" width="100" />
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="qty" label="数量" width="90" />
        <el-table-column label="损耗率" width="90">
          <template #default="{ row }">{{ row.lossRate ?? 0 }}%</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="动作" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="importActionTag(row.action)">
              {{ BOM_IMPORT_ACTION_LABEL[row.action] ?? row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="请选择 Excel 文件进行预演" />
        </template>
      </el-table>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!importPreview || importPreview.invalid > 0 || !importFile"
          :loading="importSubmitting"
          @click="onConfirmImport"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="diffDialogVisible"
      :title="`基线对比：${diffResult?.baselineCode ?? ''}`"
      width="880px"
    >
      <div class="bom-toolbar">
        <el-tag type="success">新增 {{ diffResult?.added ?? 0 }}</el-tag>
        <el-tag type="danger">删除 {{ diffResult?.removed ?? 0 }}</el-tag>
        <el-tag type="warning">变更 {{ diffResult?.changed ?? 0 }}</el-tag>
        <span class="hint">按层级路径匹配，展示基线冻结后发生的结构变化</span>
      </div>

      <el-table
        v-loading="diffLoading"
        :data="diffResult?.rows ?? []"
        border
        :row-class-name="diffRowClass"
      >
        <el-table-column label="变化" width="110">
          <template #default="{ row }">
            <StatusDot
              :tone="changeTone(row.changeType)"
              :label="BASELINE_CHANGE_LABEL[row.changeType] ?? row.changeType"
            />
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" width="110" />
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="基线用量" width="110">
          <template #default="{ row }">{{ row.baselineQty ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="当前用量" width="110">
          <template #default="{ row }">{{ row.currentQty ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="基线损耗率" width="120">
          <template #default="{ row }">{{ row.baselineLossRate ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="当前损耗率" width="120">
          <template #default="{ row }">{{ row.currentLossRate ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <template #empty>
          <el-empty description="结构与基线一致，无变化" />
        </template>
      </el-table>

      <template #footer>
        <el-button @click="diffDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="altDialogVisible"
      :title="`备选件：${altItemLabel}`"
      width="780px"
    >
      <div class="bom-toolbar">
        <el-select
          v-model="altForm.alternateId"
          filterable
          remote
          :remote-method="searchChildren"
          :loading="childLoading"
          placeholder="搜索备选零部件（编码/名称）"
          style="width: 260px"
        >
          <el-option
            v-for="item in childOptions"
            :key="item.id"
            :label="`${item.code} ${item.name}`"
            :value="item.id"
          />
        </el-select>
        <el-checkbox v-model="altForm.isGlobal">全局</el-checkbox>
        <el-checkbox v-model="altForm.isTwoWay">双向</el-checkbox>
        <el-input v-model="altForm.priorityLevel" placeholder="优先级" style="width: 100px" />
        <el-button
          type="primary"
          :disabled="!canWrite || !altForm.alternateId"
          :loading="altSaving"
          @click="onAddAlternate"
        >
          添加
        </el-button>
        <span class="hint">替代关系不参与 BOM 展开与汇总，仅用于替换参考</span>
      </div>

      <el-table v-loading="altLoading" :data="altRows" border>
        <el-table-column prop="code" label="备选件编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格型号" min-width="140" show-overflow-tooltip />
        <el-table-column label="视图" width="100">
          <template #default="{ row }">
            {{ row.isGlobal ? '全局' : (BOM_VIEW_LABEL[row.viewType] ?? row.viewType) }}
          </template>
        </el-table-column>
        <el-table-column label="双向" width="80">
          <template #default="{ row }">{{ row.isTwoWay ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="priorityLevel" label="优先级" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="!canWrite" @click="onDeleteAlternate(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无备选件" />
        </template>
      </el-table>

      <template #footer>
        <el-button @click="altDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="classifyDialog" title="零部件归类" width="560px">
      <el-form label-width="110px">
        <el-form-item label="所属分类">
          <el-tree-select
            v-model="classifyForm.classId"
            :data="classTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择分类（清空后保存 = 取消归类）"
            style="width: 100%"
            @change="onClassPicked"
          />
        </el-form-item>
        <el-form-item
          v-for="def in classifyDefs"
          :key="def.code"
          :label="def.name"
          :required="def.required"
        >
          <el-select
            v-if="def.valueType === 'BOOL'"
            v-model="classifyAttrs[def.code]"
            clearable
            style="width: 100%"
          >
            <el-option label="是" value="true" />
            <el-option label="否" value="false" />
          </el-select>
          <el-date-picker
            v-else-if="def.valueType === 'DATE'"
            v-model="classifyAttrs[def.code]"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
          <el-input v-else v-model="classifyAttrs[def.code]" />
        </el-form-item>
      </el-form>
      <div v-if="classifyForm.classId && !classifyDefs.length" class="prop-hint">该分类未定义属性</div>
      <template #footer>
        <el-button @click="classifyDialog = false">取消</el-button>
        <el-button type="primary" :loading="classifySaving" @click="onSaveClassify">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="编辑零部件" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="itemType">
          <el-select v-model="form.itemType" style="width: 100%">
            <el-option v-for="(label, value) in ITEM_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应方式">
          <el-radio-group v-model="form.sourceType">
            <el-radio value="MAKE">自制</el-radio>
            <el-radio value="BUY">外购</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specification" />
        </el-form-item>
        <el-form-item label="材料">
          <el-input v-model="form.material" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="单件重量">
          <el-input-number v-model="form.weight" :min="0" :precision="4" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标准号">
          <el-input v-model="form.stdCode" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <div class="form-section">设计属性</div>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-input v-model="form.designAttr!.provider" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图号">
              <el-input v-model="form.designAttr!.dwgNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品类别">
              <el-input v-model="form.designAttr!.productCategory" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品型号">
              <el-input v-model="form.designAttr!.productModel" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品规格">
              <el-input v-model="form.designAttr!.productSpec" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="毛坯种类">
              <el-input v-model="form.designAttr!.materialType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="毛坯种类2">
              <el-input v-model="form.designAttr!.materialType2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表面处理">
              <el-input v-model="form.designAttr!.surfaceTreatment" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否工装">
              <el-switch v-model="form.designAttr!.isTooling" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否颜色件">
              <el-switch v-model="form.designAttr!.colorItem" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重量单位">
              <el-input v-model="form.designAttr!.weightUnit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属装配">
              <el-input v-model="form.designAttr!.montage" />
            </el-form-item>
          </el-col>
        </el-row>
        <template v-if="customFields.length">
          <div class="form-section">ERP 扩展属性</div>
          <el-row :gutter="12">
            <el-col v-for="field in customFields" :key="field.code" :span="12">
              <el-form-item :label="field.name">
                <el-select
                  v-if="isBoolField(field)"
                  v-model="extBoolForm[field.code]"
                  style="width: 100%"
                  :title="`ERP 列：${field.code}`"
                >
                  <el-option label="未设置" value="" />
                  <el-option label="是" :value="true" />
                  <el-option label="否" :value="false" />
                </el-select>
                <el-input
                  v-else
                  v-model="extTextForm[field.code]"
                  :title="`ERP 列：${field.code}`"
                  :placeholder="`ERP 列 ${field.code}`"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="compareVisible" title="版本比较" width="920px">
      <div v-loading="compareLoading">
        <div v-if="compare" class="compare-head">
          <span>基准版本 <b>{{ compare.baseLabel }}</b></span>
          <span class="compare-arrow">→</span>
          <span>目标版本 <b>{{ compare.targetLabel }}</b></span>
          <el-tag v-if="compare.changedCount > 0" type="warning" size="small">
            {{ compare.changedCount }} / {{ compare.totalCount }} 个字段有差异
          </el-tag>
          <el-tag v-else type="success" size="small">两个版本业务字段一致</el-tag>
        </div>
        <el-table
          :data="compare?.rows ?? []"
          border
          size="small"
          row-key="field"
          max-height="460"
          :row-class-name="compareRowClass"
          :span-method="compareSpanMethod"
        >
          <el-table-column prop="group" label="分组" width="100" />
          <el-table-column prop="label" label="字段" width="140" />
          <el-table-column label="基准值" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ compareValue(row, row.baseValue) }}</template>
          </el-table-column>
          <el-table-column label="目标值" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ compareValue(row, row.targetValue) }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.changed" size="small" type="warning">有差异</el-tag>
              <span v-else class="hint">一致</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="没有可比字段" />
          </template>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="compareVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type TableInstance,
  type UploadFile
} from 'element-plus'
import {
  BASELINE_CHANGE_LABEL,
  BOM_IMPORT_ACTION_LABEL,
  BOM_ISSUE_LABEL,
  BOM_VIEW_LABEL,
  ITEM_PHASE_LABEL,
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  ITEM_USING_STATE_LABEL,
  ITEM_VIEW_TYPE_LABEL,
  ITEM_WORKFLOW_LABEL,
  alternateApi,
  baselineApi,
  bomApi,
  bomExcelApi,
  itemApi,
  itemClassApi,
  type BomAlternateReq,
  type BomAlternateRow,
  type BomBatchResult,
  type BomBaselineDiff,
  type BomBaselineReq,
  type BomBaselineRow,
  type BomExcelPreview,
  type BomIssue,
  type BomLineReq,
  type BomNode,
  type BomRollupRow,
  type BomSummaryRow,
  type ItemAclRow,
  type ItemDesignAttr,
  type ItemDetail,
  type ItemDocRow,
  type ItemLogRow,
  type ItemSaveReq,
  type ItemSummary,
  type ItemVersionCompare,
  type ItemVersionDiff,
  type ItemVersionRow,
  type ItemViewRow,
  type ItemClassAttrDef,
  type ItemClassNode,
  type ItemClassifyInfo,
  type WhereUsedRow
} from '@/api/item'
import { DOC_STATE_LABEL } from '@/api/doc'
import { type MetaFieldDef } from '@/api/system'
import {
  buildErpPatch,
  extValueText as extValueTextOf,
  isBoolField,
  loadItemCustomFields,
  pickExtValues
} from '@/utils/itemErpFields'
import WorkflowPanel from '@/components/WorkflowPanel.vue'
import VisualPreview from '@/components/VisualPreview.vue'
import StatusDot from '@/components/StatusDot.vue'
import ObjectDetailTabs from '@/components/ObjectDetailTabs.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const itemId = computed(() => String(route.params.id))
const detail = ref<ItemDetail | null>(null)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
// 默认落在「属性」页签（旧系统对象查看器物件的首屏）；支持 ?tab=acl 直达
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'props')

/** 元数据取不到时的兜底页签（与 V53 的 meta_tab 配置一致）。 */
const ITEM_TAB_FALLBACK = [
  { code: 'props', name: '属性' },
  { code: 'views', name: '视图' },
  { code: 'docs', name: '设计文档' },
  { code: 'visual', name: '可视化' },
  { code: 'bom', name: 'BOM' },
  { code: 'whereused', name: '被引用' },
  { code: 'versions', name: '历史' },
  { code: 'workflow', name: '流程' },
  { code: 'acl', name: '权限' },
  { code: 'logs', name: '日志' }
]

/** 页面独有页签：BOM 展开/汇总紧跟 BOM，基线跟随被引用。 */
const itemExtraTabs = [
  { code: 'bom-summary', name: 'BOM 展开', after: 'bom' },
  { code: 'bom-rollup', name: 'BOM 汇总', after: 'bom-summary' },
  { code: 'baselines', name: '基线', after: 'whereused' }
]

/** ERP 客制字段（字段集合、回填与提交语义的唯一实现在 utils/itemErpFields，T-G-02）。 */
const customFields = ref<MetaFieldDef[]>([])

/** 文本类客制字段的编辑态值。 */
const extTextForm = reactive<Record<string, string>>({})
/** 布尔类客制字段的编辑态值：'' = 未设置（不写 ext_json），true/false = 明确取值。 */
const extBoolForm = reactive<Record<string, boolean | ''>>({})
/** 打开编辑时的 ext_json 快照：保存时只提交改动过的键，未改动的交给后端保留。 */
const extOriginal = ref<Record<string, unknown>>({})

/** 属性 / 视图 / 设计文档 / 历史 / 权限 / 日志页签数据。 */
const views = ref<ItemViewRow[]>([])
const viewLoading = ref(false)
const newViewType = ref('DESIGN')
const newViewDesc = ref('')
const docs = ref<ItemDocRow[]>([])
const docLoading = ref(false)
const versions = ref<ItemVersionRow[]>([])
const versionLoading = ref(false)
const versionCompareRows = ref<ItemVersionRow[]>([])
const compareVisible = ref(false)
const compareLoading = ref(false)
const compare = ref<ItemVersionCompare | null>(null)
const newViewCopyFrom = ref('')
const logs = ref<ItemLogRow[]>([])
const aclRows = ref<ItemAclRow[]>([])
const subLoading = ref(false)

const bomView = ref('DESIGN')
/** 有效性时点：为空表示不按有效性筛选（对齐旧系统不传 VALIDITYQUERYDATA 的语义）。 */
const bomEffectAt = ref<string | null>(null)
/** 汇总只统计叶子件（对应旧系统元件汇总口径）。 */
const bomLeafOnly = ref(false)
const bomRows = ref<BomNode[]>([])
const bomTableRef = ref<TableInstance>()
const bomLoading = ref(false)
const bomIssues = ref<BomIssue[]>([])
const bomSummaryRows = ref<BomSummaryRow[]>([])
const bomSummaryLoading = ref(false)
const bomRollupRows = ref<BomRollupRow[]>([])
const bomRollupLoading = ref(false)
const whereUsedRows = ref<WhereUsedRow[]>([])
const whereUsedLoading = ref(false)
const bomDialogVisible = ref(false)
const bomEditingLineId = ref<string | null>(null)
const bomSaving = ref(false)
const childOptions = ref<ItemSummary[]>([])
const childLoading = ref(false)
const bomForm = reactive<BomLineReq>({
  childId: '',
  qty: 1,
  unit: '',
  lossRate: 0,
  effectFrom: null,
  effectTo: null,
  refDesignator: '',
  remark: ''
})

/** BOM 行勾选结果：批量改用量/批量替换的作用范围。 */
const selectedBomRows = ref<BomNode[]>([])
const batchQtyDialogVisible = ref(false)
const batchQtySaving = ref(false)
const batchQtyForm = reactive({
  useQty: false,
  qty: 1,
  useLossRate: false,
  lossRate: 0,
  useUnit: false,
  unit: ''
})
const batchReplaceDialogVisible = ref(false)
const batchReplaceSaving = ref(false)
const batchReplaceForm = reactive({ fromChildId: '', toChildId: '' })
const replaceTargetOptions = ref<ItemSummary[]>([])
const replaceTargetLoading = ref(false)
/** 批量替换的候选被替换件：取当前勾选行里的子件去重。 */
const replaceSourceOptions = computed(() => {
  const map = new Map<string, { childId: string; label: string }>()
  selectedBomRows.value.forEach((row) => {
    if (!map.has(row.childId)) {
      map.set(row.childId, { childId: row.childId, label: `${row.code} ${row.name}` })
    }
  })
  return [...map.values()]
})

const altDialogVisible = ref(false)
const altItemId = ref('')
const altItemLabel = ref('')
const altRows = ref<BomAlternateRow[]>([])
const altLoading = ref(false)
const altSaving = ref(false)
const altForm = reactive<BomAlternateReq>({
  alternateId: '',
  isGlobal: false,
  isTwoWay: false,
  priorityLevel: ''
})

const importDialogVisible = ref(false)
const importFile = ref<File | null>(null)
const importFileList = ref<UploadFile[]>([])
const importPreview = ref<BomExcelPreview | null>(null)
const importLoading = ref(false)
const importSubmitting = ref(false)

const baselineRows = ref<BomBaselineRow[]>([])
const baselineLoading = ref(false)
const baselineForm = reactive<BomBaselineReq>({ code: '', name: '' })
const diffDialogVisible = ref(false)
const diffLoading = ref(false)
const diffResult = ref<BomBaselineDiff | null>(null)

const canWrite = computed(() => userStore.can('pse.item.write'))

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

const form = reactive<ItemSaveReq>({
  name: '',
  itemType: 'PART',
  sourceType: 'MAKE',
  specification: '',
  material: '',
  unit: 'EA',
  weight: null,
  stdCode: '',
  remark: '',
  designAttr: emptyDesignAttr()
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function stateLabel(state?: string | null): string {
  return state ? DOC_STATE_LABEL[state] ?? state : '-'
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

function aclTypeLabel(actorType?: string | null): string {
  if (actorType === 'USER') {
    return '用户'
  }
  if (actorType === 'ROLE') {
    return '角色'
  }
  if (actorType === 'ORG') {
    return '组织'
  }
  if (actorType === 'GROUP') {
    return '用户组'
  }
  return actorType || '-'
}


function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

/** 有效性区间展示：两侧都为空即「长期有效」。 */
function effectLabel(row: { effectFrom?: string | null; effectTo?: string | null }): string {
  if (!row.effectFrom && !row.effectTo) {
    return '长期有效'
  }
  return `${row.effectFrom || '不限'} ~ ${row.effectTo || '不限'}`
}

async function load(): Promise<void> {
  loading.value = true
  try {
    detail.value = await itemApi.detail(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载零部件失败')
  } finally {
    loading.value = false
  }
}

/** 拉 PdmItem 元数据并算出客制字段（失败时回退到 V70 兜底清单）。 */
async function loadMetaFields(): Promise<void> {
  customFields.value = await loadItemCustomFields()
}

/** 属性页签里的只读展示：未填显示 -，布尔值转是/否。 */
function extValueText(field: MetaFieldDef): string {
  return extValueTextOf(detail.value?.ext, field)
}

/** 清空并重填 reactive 记录，避免上一张单子的残留键串到本次编辑。 */
function replaceTextRecord(target: Record<string, string>, source: Record<string, string>): void {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

function replaceBoolRecord(
  target: Record<string, boolean | ''>,
  source: Record<string, boolean | ''>
): void {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.name = detail.value.name
  form.itemType = detail.value.itemType
  form.sourceType = detail.value.sourceType
  form.specification = detail.value.specification ?? ''
  form.material = detail.value.material ?? ''
  form.unit = detail.value.unit
  form.weight = detail.value.weight ?? null
  form.stdCode = detail.value.stdCode ?? ''
  form.remark = detail.value.remark ?? ''
  form.designAttr = { ...emptyDesignAttr(), ...(detail.value.designAttr ?? {}) }

  const ext = detail.value.ext ?? {}
  extOriginal.value = { ...ext }
  const values = pickExtValues(customFields.value, ext)
  replaceTextRecord(extTextForm, values.text)
  replaceBoolRecord(extBoolForm, values.bool)

  dialogVisible.value = true
}

/** 编辑语义：只提交改动过的键（未改动=后端保留，文本清空=清空）。 */
function buildExtPatch(): Record<string, unknown> {
  return buildErpPatch(customFields.value, extTextForm, extBoolForm, extOriginal.value)
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
    detail.value = await itemApi.update(itemId.value, { ...form, ext: buildExtPatch() })
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function loadBom(): Promise<void> {
  bomLoading.value = true
  try {
    bomRows.value = await bomApi.tree(itemId.value, bomView.value, 6, bomEffectAt.value ?? undefined)
    bomIssues.value = []
    selectedBomRows.value = []
    bomTableRef.value?.clearSelection()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 BOM 失败')
  } finally {
    bomLoading.value = false
  }
}

async function loadBomSummary(): Promise<void> {
  bomSummaryLoading.value = true
  try {
    bomSummaryRows.value = await bomApi.summary(itemId.value, bomView.value, 10, bomEffectAt.value ?? undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 BOM 展开失败')
  } finally {
    bomSummaryLoading.value = false
  }
}

async function loadBomRollup(): Promise<void> {
  bomRollupLoading.value = true
  try {
    bomRollupRows.value = await bomApi.rollup(
      itemId.value,
      bomView.value,
      10,
      bomEffectAt.value ?? undefined,
      bomLeafOnly.value
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载 BOM 汇总失败')
  } finally {
    bomRollupLoading.value = false
  }
}

async function onExportBom(): Promise<void> {
  try {
    await bomExcelApi.export(itemId.value, {
      viewType: bomView.value,
      effectAt: bomEffectAt.value ?? undefined
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

function openImportDialog(): void {
  importFile.value = null
  importFileList.value = []
  importPreview.value = null
  importDialogVisible.value = true
}

async function onImportFileChange(file: UploadFile): Promise<void> {
  const raw = file.raw ?? null
  importFile.value = raw
  importFileList.value = raw ? [file] : []
  importPreview.value = null
  if (!raw) {
    return
  }
  importLoading.value = true
  try {
    importPreview.value = await bomExcelApi.preview(itemId.value, raw, bomView.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '解析 Excel 失败')
  } finally {
    importLoading.value = false
  }
}

function onImportFileRemove(): void {
  importFile.value = null
  importFileList.value = []
  importPreview.value = null
}

async function onConfirmImport(): Promise<void> {
  if (!importFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  if ((importPreview.value?.invalid ?? 0) > 0) {
    ElMessage.warning('存在校验问题，请修正文件后重新选择')
    return
  }
  importSubmitting.value = true
  try {
    const result = await bomExcelApi.import(itemId.value, importFile.value, bomView.value)
    ElMessage.success(`导入完成：新增 ${result.created}，更新 ${result.updated}，未变 ${result.skipped}`)
    importDialogVisible.value = false
    importFile.value = null
    importFileList.value = []
    importPreview.value = null
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    importSubmitting.value = false
  }
}

function importActionTag(action: string): 'success' | 'warning' | 'info' {
  if (action === 'NEW') {
    return 'success'
  }
  if (action === 'UPDATE') {
    return 'warning'
  }
  return 'info'
}

async function loadBaselines(): Promise<void> {
  baselineLoading.value = true
  try {
    baselineRows.value = await baselineApi.list(itemId.value, bomView.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载基线失败')
  } finally {
    baselineLoading.value = false
  }
}

async function onCreateBaseline(): Promise<void> {
  if (!baselineForm.code.trim()) {
    ElMessage.warning('请输入基线编码')
    return
  }
  try {
    await baselineApi.create(itemId.value, { ...baselineForm, viewType: bomView.value })
    ElMessage.success('已冻结当前结构')
    baselineForm.code = ''
    baselineForm.name = ''
    await loadBaselines()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建基线失败')
  }
}

async function onDiffBaseline(row: BomBaselineRow): Promise<void> {
  diffDialogVisible.value = true
  diffLoading.value = true
  diffResult.value = null
  try {
    diffResult.value = await baselineApi.diff(itemId.value, row.id, {
      effectAt: bomEffectAt.value ?? undefined
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '对比失败')
  } finally {
    diffLoading.value = false
  }
}

async function onDeleteBaseline(row: BomBaselineRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除基线「${row.code}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await baselineApi.remove(itemId.value, row.id)
    ElMessage.success('已删除')
    await loadBaselines()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除基线失败')
  }
}

function changeTone(type: string): 'ok' | 'danger' | 'warn' | 'muted' {
  if (type === 'ADDED') {
    return 'ok'
  }
  if (type === 'REMOVED') {
    return 'danger'
  }
  if (type === 'QTY_CHANGED') {
    return 'warn'
  }
  return 'muted'
}

/** 差异行底色（SIPM p35：新增浅绿 / 删除浅红 / 变更浅黄）。 */
function diffRowClass({ row }: { row: { changeType?: string } }): string {
  if (row.changeType === 'ADDED') {
    return 'diff-row-added'
  }
  if (row.changeType === 'REMOVED') {
    return 'diff-row-removed'
  }
  if (row.changeType === 'QTY_CHANGED') {
    return 'diff-row-changed'
  }
  return ''
}

async function loadWhereUsed(): Promise<void> {
  whereUsedLoading.value = true
  try {
    whereUsedRows.value = await bomApi.whereUsed(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载被引用失败')
  } finally {
    whereUsedLoading.value = false
  }
}

// ------------------------------------------------------------ 视图 / 文档 / 历史 / 权限 / 日志

async function loadViews(): Promise<void> {
  viewLoading.value = true
  try {
    views.value = await itemApi.views(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载视图失败')
  } finally {
    viewLoading.value = false
  }
}

async function onCreateView(): Promise<void> {
  try {
    await itemApi.createView(itemId.value, {
      viewType: newViewType.value,
      description: newViewDesc.value || undefined,
      copyFromViewId: newViewCopyFrom.value || undefined
    })
    newViewDesc.value = ''
    newViewCopyFrom.value = ''
    ElMessage.success('视图已创建')
    await loadViews()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '新建视图失败')
  }
}

async function onRemoveView(row: ItemViewRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除视图「${row.viewTypeLabel}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await itemApi.removeView(itemId.value, row.id)
    ElMessage.success('视图已删除')
    await loadViews()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除视图失败')
  }
}

function switchView(viewType: string): void {
  bomView.value = viewType
  activeTab.value = 'bom'
  loadBom()
}

async function loadDocs(): Promise<void> {
  docLoading.value = true
  try {
    docs.value = await itemApi.docs(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载设计文档失败')
  } finally {
    docLoading.value = false
  }
}

function openDoc(id: string): void {
  router.push(`/docs/${id}`)
}

async function loadVersions(): Promise<void> {
  versionLoading.value = true
  try {
    versions.value = await itemApi.versions(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载版本历史失败')
  } finally {
    versionLoading.value = false
  }
}

function openVersion(id: string): void {
  router.push({ name: 'item-detail', params: { id }, query: { tab: 'versions' } })
}

// ------------------------------------------------------------ 版本比较（B4）

function onVersionSelectionChange(rows: ItemVersionRow[]): void {
  versionCompareRows.value = rows
  if (rows.length > 2) {
    ElMessage.warning('最多勾选两个版本')
  }
}

/** 基准 = 较旧版本，目标 = 较新版本，差异方向符合"这版改了什么"的阅读习惯。 */
async function openVersionCompare(): Promise<void> {
  if (versionCompareRows.value.length !== 2) {
    ElMessage.warning('请勾选两个版本')
    return
  }
  const [base, target] = [...versionCompareRows.value].sort(
    (a, b) => a.revision - b.revision || a.iteration - b.iteration
  )
  compare.value = null
  compareVisible.value = true
  compareLoading.value = true
  try {
    compare.value = await itemApi.compareVersions(itemId.value, base.id, target.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '版本比较失败')
    compareVisible.value = false
  } finally {
    compareLoading.value = false
  }
}

function compareRowClass({ row }: { row: ItemVersionDiff }): string {
  return row.changed ? 'diff-row' : ''
}

/** 分组列按连续相同分组纵向合并，避免每行都重复「基本属性」。 */
function compareSpanMethod({
  row,
  rowIndex,
  columnIndex
}: {
  row: ItemVersionDiff
  rowIndex: number
  columnIndex: number
}): [number, number] | undefined {
  if (columnIndex !== 0) {
    return undefined
  }
  const rows = compare.value?.rows ?? []
  if (rowIndex > 0 && rows[rowIndex - 1]?.group === row.group) {
    return [0, 0]
  }
  let span = 1
  while (report(rows, rowIndex + span) === row.group) {
    span++
  }
  return [span, 1]
}

function report(rows: ItemVersionDiff[], index: number): string | undefined {
  return rows[index]?.group
}

/** 码值字段翻成中文再比较展示，避免出现「MAKE vs BUY」这种业务看不懂的 diff。 */
function compareValue(row: ItemVersionDiff, value?: string | null): string {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  switch (row.field) {
    case 'itemType':
      return ITEM_TYPE_LABEL[value] ?? value
    case 'sourceType':
      return ITEM_SOURCE_LABEL[value] ?? value
    case 'lifecycleState':
      return stateLabel(value)
    case 'phaseState':
      return phaseLabel(value)
    case 'usingState':
      return usingLabel(value)
    case 'workflowState':
      return workflowLabel(value)
    default:
      return value
  }
}

async function loadAcl(): Promise<void> {
  subLoading.value = true
  try {
    aclRows.value = await itemApi.acl(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载权限失败')
  } finally {
    subLoading.value = false
  }
}

async function loadLogs(): Promise<void> {
  subLoading.value = true
  try {
    logs.value = await itemApi.logs(itemId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载日志失败')
  } finally {
    subLoading.value = false
  }
}

// ------------------------------------------------------------ 检出 / 检入 / 修订 / 使用状态

async function onCheckout(): Promise<void> {
  try {
    detail.value = await itemApi.checkout(itemId.value)
    ElMessage.success('已检出，可编辑')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检出失败')
  }
}

async function onCheckin(): Promise<void> {
  try {
    const created = await itemApi.checkin(itemId.value)
    ElMessage.success(`已检入，新版本 ${created.revisionLabel}`)
    router.push({ name: 'item-detail', params: { id: created.id }, query: { tab: 'versions' } })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检入失败')
  }
}

async function onCancelCheckout(): Promise<void> {
  try {
    await itemApi.cancelCheckout(itemId.value)
    ElMessage.success('已撤销检出')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤销检出失败')
  }
}

async function onRevise(): Promise<void> {
  try {
    await ElMessageBox.confirm('修订会生成新的大版本（回到草稿），旧版本保留可追溯。继续？', '修订', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const created = await itemApi.revise(itemId.value)
    ElMessage.success(`已修订，新版本 ${created.revisionLabel}`)
    router.push({ name: 'item-detail', params: { id: created.id }, query: { tab: 'versions' } })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修订失败')
  }
}

async function onUsingStateChange(usingState: string | number | object): Promise<void> {
  try {
    detail.value = await itemApi.changeUsingState(itemId.value, String(usingState))
    ElMessage.success('使用状态已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新使用状态失败')
  }
}

function onTabChange(name: string): void {
  if (name === 'bom') {
    loadBom()
  } else if (name === 'bom-summary') {
    loadBomSummary()
  } else if (name === 'bom-rollup') {
    loadBomRollup()
  } else if (name === 'baselines') {
    loadBaselines()
  } else if (name === 'whereused') {
    loadWhereUsed()
  } else if (name === 'views') {
    loadViews()
  } else if (name === 'docs') {
    loadDocs()
  } else if (name === 'versions') {
    loadVersions()
  } else if (name === 'acl') {
    loadAcl()
  } else if (name === 'logs') {
    loadLogs()
  }
}

async function searchChildren(keyword: string): Promise<void> {
  childLoading.value = true
  try {
    const page = await itemApi.list({ page: 1, size: 20, keyword: keyword || undefined })
    childOptions.value = page.items.filter((item) => item.id !== itemId.value)
  } catch {
    childOptions.value = []
  } finally {
    childLoading.value = false
  }
}

function openAddBom(): void {
  bomEditingLineId.value = null
  bomForm.childId = ''
  bomForm.qty = 1
  bomForm.unit = ''
  bomForm.lossRate = 0
  bomForm.effectFrom = null
  bomForm.effectTo = null
  bomForm.refDesignator = ''
  bomForm.remark = ''
  childOptions.value = []
  bomDialogVisible.value = true
}

function openEditBom(row: BomNode): void {
  bomEditingLineId.value = row.lineId
  bomForm.childId = row.childId
  bomForm.qty = Number(row.qty)
  bomForm.unit = row.unit ?? ''
  bomForm.lossRate = row.lossRate ?? 0
  bomForm.effectFrom = row.effectFrom ?? null
  bomForm.effectTo = row.effectTo ?? null
  bomForm.refDesignator = row.refDesignator ?? ''
  bomForm.remark = row.remark ?? ''
  bomDialogVisible.value = true
}

async function onSaveBom(): Promise<void> {
  if (!bomEditingLineId.value && !bomForm.childId) {
    ElMessage.warning('请选择子件')
    return
  }
  if (!bomForm.qty || Number(bomForm.qty) <= 0) {
    ElMessage.warning('用量必须大于 0')
    return
  }
  bomSaving.value = true
  try {
    if (bomEditingLineId.value) {
      await bomApi.update(itemId.value, bomEditingLineId.value, { ...bomForm })
    } else {
      await bomApi.add(itemId.value, bomView.value, { ...bomForm })
    }
    ElMessage.success('已保存')
    bomDialogVisible.value = false
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存 BOM 失败')
  } finally {
    bomSaving.value = false
  }
}

async function onDeleteBom(row: BomNode): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认从 BOM 中移除「${row.code} ${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await bomApi.remove(itemId.value, row.lineId)
    ElMessage.success('已移除')
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

function onBomSelectionChange(rows: BomNode[]): void {
  selectedBomRows.value = rows
}

function openBatchQty(): void {
  if (selectedBomRows.value.length === 0) {
    ElMessage.warning('请先勾选要修改的 BOM 行')
    return
  }
  batchQtyForm.useQty = false
  batchQtyForm.qty = 1
  batchQtyForm.useLossRate = false
  batchQtyForm.lossRate = 0
  batchQtyForm.useUnit = false
  batchQtyForm.unit = ''
  batchQtyDialogVisible.value = true
}

async function onSaveBatchQty(): Promise<void> {
  if (!batchQtyForm.useQty && !batchQtyForm.useLossRate && !batchQtyForm.useUnit) {
    ElMessage.warning('请至少勾选一个要修改的字段')
    return
  }
  if (batchQtyForm.useQty && (!batchQtyForm.qty || Number(batchQtyForm.qty) <= 0)) {
    ElMessage.warning('用量必须大于 0')
    return
  }
  batchQtySaving.value = true
  try {
    const result = await bomApi.batchUpdateQty(itemId.value, {
      lineIds: selectedBomRows.value.map((row) => row.lineId),
      qty: batchQtyForm.useQty ? batchQtyForm.qty : null,
      lossRate: batchQtyForm.useLossRate ? batchQtyForm.lossRate : null,
      unit: batchQtyForm.useUnit ? batchQtyForm.unit.trim() || null : null
    })
    batchQtyDialogVisible.value = false
    await loadBom()
    reportBatchResult('批量修改用量', result)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量修改失败')
  } finally {
    batchQtySaving.value = false
  }
}

function openBatchReplace(): void {
  if (selectedBomRows.value.length === 0) {
    ElMessage.warning('请先勾选要替换的 BOM 行')
    return
  }
  const sources = replaceSourceOptions.value
  batchReplaceForm.fromChildId = sources.length === 1 ? sources[0].childId : ''
  batchReplaceForm.toChildId = ''
  replaceTargetOptions.value = []
  batchReplaceDialogVisible.value = true
}

async function searchReplaceTarget(keyword: string): Promise<void> {
  replaceTargetLoading.value = true
  try {
    const page = await itemApi.list({ page: 1, size: 20, keyword: keyword || undefined })
    replaceTargetOptions.value = page.items.filter((item) => item.id !== itemId.value)
  } catch {
    replaceTargetOptions.value = []
  } finally {
    replaceTargetLoading.value = false
  }
}

async function onSaveBatchReplace(): Promise<void> {
  if (!batchReplaceForm.fromChildId) {
    ElMessage.warning('请选择被替换的子件')
    return
  }
  if (!batchReplaceForm.toChildId) {
    ElMessage.warning('请选择替换目标的子件')
    return
  }
  if (batchReplaceForm.fromChildId === batchReplaceForm.toChildId) {
    ElMessage.warning('替换目标不能与被替换件相同')
    return
  }
  batchReplaceSaving.value = true
  try {
    const result = await bomApi.batchReplace(itemId.value, {
      fromChildId: batchReplaceForm.fromChildId,
      toChildId: batchReplaceForm.toChildId,
      viewType: bomView.value
    })
    batchReplaceDialogVisible.value = false
    await loadBom()
    reportBatchResult('批量替换子件', result)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量替换失败')
  } finally {
    batchReplaceSaving.value = false
  }
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (ch) => {
    const map: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
    return map[ch] ?? ch
  })
}

/** 批量结果统一提示：成功/跳过计数 + 逐行原因（最多 10 条）。 */
function reportBatchResult(title: string, result: BomBatchResult): void {
  if (result.updated === 0 && result.skipped === 0) {
    ElMessage.info('没有需要处理的行')
    return
  }
  const lines = [`成功 ${result.updated} 行，跳过 ${result.skipped} 行`]
  result.messages.slice(0, 10).forEach((message) => lines.push(`· ${escapeHtml(message)}`))
  if (result.messages.length > 10) {
    lines.push(`… 其余 ${result.messages.length - 10} 条已省略`)
  }
  ElMessageBox.alert(lines.map((line) => `<div>${line}</div>`).join(''), title, {
    type: result.skipped > 0 ? 'warning' : 'success',
    dangerouslyUseHTMLString: true
  })
}

/** 定位某行在同级中的兄弟列表（树形结构按 lineNo 排序）。 */
function siblingsOf(target: BomNode): BomNode[] {
  const walk = (nodes: BomNode[]): BomNode[] | null => {
    if (nodes.some((node) => node.lineId === target.lineId)) {
      return nodes
    }
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        const found = walk(node.children)
        if (found) {
          return found
        }
      }
    }
    return null
  }
  return walk(bomRows.value) ?? []
}

/** 上移/下移：与相邻兄弟交换 lineNo（不换父件，故只传 newLineNo）。 */
async function onMoveBom(row: BomNode, delta: number): Promise<void> {
  const siblings = siblingsOf(row).slice().sort((a, b) => a.lineNo - b.lineNo)
  const index = siblings.findIndex((node) => node.lineId === row.lineId)
  const neighbour = siblings[index + delta]
  if (index < 0 || !neighbour) {
    ElMessage.info(delta < 0 ? '已是同级第一行' : '已是同级最后一行')
    return
  }
  try {
    await bomApi.move(itemId.value, row.lineId, { newLineNo: neighbour.lineNo, viewType: bomView.value })
    await bomApi.move(itemId.value, neighbour.lineId, { newLineNo: row.lineNo, viewType: bomView.value })
    await loadBom()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '调整序号失败')
  }
}

async function openAlternates(row: BomNode): Promise<void> {
  altItemId.value = row.childId
  altItemLabel.value = `${row.code} ${row.name}`
  altForm.alternateId = ''
  altForm.isGlobal = false
  altForm.isTwoWay = false
  altForm.priorityLevel = ''
  childOptions.value = []
  altDialogVisible.value = true
  await loadAlternates()
}

async function loadAlternates(): Promise<void> {
  altLoading.value = true
  try {
    altRows.value = await alternateApi.list(altItemId.value, bomView.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载备选件失败')
  } finally {
    altLoading.value = false
  }
}

async function onAddAlternate(): Promise<void> {
  if (!altForm.alternateId) {
    ElMessage.warning('请选择备选件')
    return
  }
  altSaving.value = true
  try {
    await alternateApi.add(altItemId.value, { ...altForm, viewType: bomView.value })
    ElMessage.success('已添加备选件')
    altForm.alternateId = ''
    altForm.priorityLevel = ''
    await loadAlternates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加备选件失败')
  } finally {
    altSaving.value = false
  }
}

async function onDeleteAlternate(row: BomAlternateRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认移除备选件「${row.code} ${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await alternateApi.remove(altItemId.value, row.id)
    ElMessage.success('已移除')
    await loadAlternates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function checkBom(): Promise<void> {
  try {
    const result = await bomApi.validate(itemId.value, bomView.value)
    bomIssues.value = result.issues
    if (result.issues.length === 0) {
      ElMessage.success('结构校验通过：无用量/单位/孤儿件/循环引用问题')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结构校验失败')
  }
}

function goBack(): void {
  router.push({ name: 'item-list' })
}

// ------------------------------------------------------------ 分类（批次 H·T-H-01）

const classify = ref<ItemClassifyInfo>({ attrs: {}, attrDefs: [] })
const classTree = ref<ItemClassNode[]>([])
const classifyDialog = ref(false)
const classifySaving = ref(false)
const classifyDefs = ref<ItemClassAttrDef[]>([])
const classifyForm = reactive<{ classId: string | null }>({ classId: null })
const classifyAttrs = reactive<Record<string, string>>({})

/** 分类读取失败不应影响详情主体（兼容未执行 V71 的库） */
async function loadClassify(): Promise<void> {
  try {
    classify.value = await itemClassApi.info(itemId.value)
  } catch {
    classify.value = { attrs: {}, attrDefs: [] }
  }
}

function resetClassifyAttrs(source?: Record<string, string>): void {
  Object.keys(classifyAttrs).forEach((key) => delete classifyAttrs[key])
  Object.entries(source ?? {}).forEach(([key, value]) => {
    classifyAttrs[key] = value ?? ''
  })
}

async function openClassify(): Promise<void> {
  if (!classTree.value.length) {
    try {
      classTree.value = await itemClassApi.tree()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '加载分类树失败')
      return
    }
  }
  classifyForm.classId = classify.value.classId ?? null
  classifyDefs.value = classify.value.attrDefs ?? []
  resetClassifyAttrs(classify.value.attrs)
  classifyDialog.value = true

  // 未归类时按编码自动匹配分类（对齐蓝本 Classify.js:117-127）；命中只做预选，用户仍可改
  if (!classifyForm.classId) {
    try {
      const suggestion = await itemClassApi.suggestion(itemId.value)
      if (suggestion.matched && suggestion.classId) {
        classifyForm.classId = suggestion.classId
        await onClassPicked(suggestion.classId)
        ElMessage.info(suggestion.reason ?? '已按编码自动匹配分类')
      }
    } catch {
      // 自动匹配是增强项，失败不影响手动归类
    }
  }
}

/** 切换分类时重新取生效属性（含继承），已填值保留 */
async function onClassPicked(classId: string | null): Promise<void> {
  if (!classId) {
    classifyDefs.value = []
    return
  }
  try {
    classifyDefs.value = await itemClassApi.attrs(classId)
  } catch (error) {
    classifyDefs.value = []
    ElMessage.error(error instanceof Error ? error.message : '加载分类属性失败')
  }
}

async function onSaveClassify(): Promise<void> {
  const attrs: Record<string, string> = {}
  Object.entries(classifyAttrs).forEach(([code, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      attrs[code] = value
    }
  })
  classifySaving.value = true
  try {
    classify.value = await itemClassApi.assign(itemId.value, classifyForm.classId, attrs)
    ElMessage.success(classifyForm.classId ? '已归类' : '已取消归类')
    classifyDialog.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '归类失败')
  } finally {
    classifySaving.value = false
  }
}

async function reloadAll(): Promise<void> {
  await load()
  await loadClassify()
  if (activeTab.value === 'bom') {
    await loadBom()
  } else if (activeTab.value === 'views') {
    await loadViews()
  } else if (activeTab.value === 'versions') {
    await loadVersions()
  } else if (activeTab.value === 'docs') {
    await loadDocs()
  } else if (activeTab.value === 'acl') {
    await loadAcl()
  } else if (activeTab.value === 'logs') {
    await loadLogs()
  }
}

// 打开历史版本时路由 id 变化，需要重新拉取该版本详情与对应页签数据
watch(itemId, () => {
  reloadAll()
})

onMounted(() => {
  // 元数据字段集合一次取够，切换版本（路由 id 变化）无需重取
  loadMetaFields()
  reloadAll()
})
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
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.code {
  color: #909399;
}

.name {
  font-weight: 600;
}

.bom-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.compare-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.compare-arrow {
  color: #909399;
}

:deep(.diff-row) {
  background: #fdf6ec;
}

:deep(.diff-row td) {
  color: #b88230;
}

.bom-issues {
  margin-bottom: 12px;
}

.effect-range {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.issue-line {
  font-size: 12px;
  line-height: 1.6;
}

.batch-form {
  margin-top: 12px;
}

.batch-input {
  width: 180px;
  margin-left: 8px;
}

/* 基线对比差异行：新增浅绿 / 删除浅红 / 变更浅黄（SIPM p35 的目视化差异） */
:deep(.diff-row-added) td.el-table__cell {
  background: #f1faf4;
}

:deep(.diff-row-removed) td.el-table__cell {
  background: #fdf2f1;
}

:deep(.diff-row-changed) td.el-table__cell {
  background: var(--plm-highlight, #fff8d6);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.head-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* 属性页签：左缩略图 + 右属性表（旧系统 ItemInfoPage 的版式） */
.props-layout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.props-thumb {
  display: flex;
  flex: 0 0 180px;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  border: 1px solid var(--plm-border, #dde3ea);
  background: #fafbfc;
}

.props-body {
  flex: 1;
  min-width: 0;
}

.prop-title {
  margin: 10px 0 8px;
  padding-left: 8px;
  border-left: 3px solid var(--plm-primary, #1f5c8b);
  font-weight: 600;
  font-size: 13px;
}

.prop-title:first-child {
  margin-top: 0;
}

.prop-hint {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.checkout-line {
  margin-top: 10px;
  color: var(--plm-warn, #e6a23c);
  font-size: 12px;
}

.form-section {
  margin: 6px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--plm-primary, #1f5c8b);
  font-weight: 600;
  font-size: 13px;
}
</style>
