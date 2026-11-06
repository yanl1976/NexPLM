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
            <el-tag size="small" :type="stateTag(detail?.lifecycleState)">{{ stateLabel(detail?.lifecycleState) }}</el-tag>
            <el-tag v-if="detail" size="small" :type="detail.sourceType === 'BUY' ? 'warning' : 'success'">
              {{ ITEM_SOURCE_LABEL[detail.sourceType] ?? detail.sourceType }}
            </el-tag>
          </div>
          <div>
            <el-button v-if="canWrite" @click="openEdit">编辑属性</el-button>
          </div>
        </div>
      </template>

      <el-descriptions v-if="detail" :column="2" border>
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
        <el-descriptions-item label="生命周期">{{ stateLabel(detail.lifecycleState) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="BOM" name="bom">
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
        </el-tab-pane>

        <el-tab-pane label="BOM 展开" name="bom-summary">
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
        </el-tab-pane>

        <el-tab-pane label="BOM 汇总" name="bom-rollup">
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
        </el-tab-pane>

        <el-tab-pane label="被引用" name="whereused">
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
        </el-tab-pane>

        <el-tab-pane label="基线" name="baselines">
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
        </el-tab-pane>
      </el-tabs>
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

      <el-table v-loading="diffLoading" :data="diffResult?.rows ?? []" border>
        <el-table-column label="变化" width="110">
          <template #default="{ row }">
            <el-tag :type="changeTag(row.changeType)" size="small">
              {{ BASELINE_CHANGE_LABEL[row.changeType] ?? row.changeType }}
            </el-tag>
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

    <el-dialog v-model="dialogVisible" title="编辑零部件" width="640px">
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
  ITEM_SOURCE_LABEL,
  ITEM_TYPE_LABEL,
  alternateApi,
  baselineApi,
  bomApi,
  bomExcelApi,
  itemApi,
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
  type ItemDetail,
  type ItemSaveReq,
  type ItemSummary,
  type WhereUsedRow
} from '@/api/item'
import { DOC_STATE_LABEL } from '@/api/doc'
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
const activeTab = ref('bom')

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

const form = reactive<ItemSaveReq>({
  name: '',
  itemType: 'PART',
  sourceType: 'MAKE',
  specification: '',
  material: '',
  unit: 'EA',
  weight: null,
  stdCode: '',
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function stateLabel(state?: string | null): string {
  return state ? DOC_STATE_LABEL[state] ?? state : '-'
}

function stateTag(state?: string | null): 'success' | 'info' | 'warning' | 'danger' {
  switch (state) {
    case 'Released':
      return 'success'
    case 'InWorkflow':
      return 'warning'
    case 'Obsolete':
      return 'danger'
    default:
      return 'info'
  }
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
  dialogVisible.value = true
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
    detail.value = await itemApi.update(itemId.value, { ...form })
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

function changeTag(type: string): 'success' | 'danger' | 'warning' | 'info' {
  if (type === 'ADDED') {
    return 'success'
  }
  if (type === 'REMOVED') {
    return 'danger'
  }
  if (type === 'QTY_CHANGED') {
    return 'warning'
  }
  return 'info'
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

onMounted(async () => {
  await load()
  await loadBom()
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
</style>
