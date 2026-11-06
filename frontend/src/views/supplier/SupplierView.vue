<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/名称/简称" clearable style="width: 200px" @keyup.enter="reload" />
      <el-select v-model="category" placeholder="全部类别" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in SUPPLIER_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option v-for="(label, value) in SUPPLIER_STATUS_LABEL" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <el-button v-if="canWrite" type="primary" plain @click="openCreate">新建供应商</el-button>
      <el-button plain @click="schemeVisible = true">评级方案</el-button>
      <span class="hint">一个零件可挂多个供应商（多源），但首选只能一个；有在用零件关系的供应商不能删除</span>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe @row-dblclick="(row: SupplierRow) => openDetail(row.id)">
      <el-table-column prop="code" label="编码" width="130" />
      <el-table-column prop="name" label="供应商名称" min-width="220" show-overflow-tooltip />
      <el-table-column label="类别" width="110" align="center">
        <template #default="{ row }">{{ SUPPLIER_CATEGORY_LABEL[row.category] ?? row.category }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ SUPPLIER_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contact" label="联系人" width="100">
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="归属组织" width="140">
        <template #default="{ row }">{{ row.orgName || '-' }}</template>
      </el-table-column>
      <el-table-column label="零件关系" width="120" align="center">
        <template #default="{ row }">{{ row.partCount }}（首选 {{ row.preferredCount }}）</template>
      </el-table-column>
      <el-table-column label="评级" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.grade" :type="gradeType(row.grade)" size="small">{{ row.grade }} 级</el-tag>
          <span v-else class="muted">未评级</span>
          <span v-if="row.ratingScore != null" class="score">{{ row.ratingScore }} 分</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="没有符合条件的供应商" :image-size="60" />
    <el-pagination
      v-if="total > size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <!-- 新建/编辑供应商 -->
    <el-dialog v-model="formVisible" :title="form.id ? `编辑 ${form.code}` : '新建供应商'" width="680px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="form.shortName" maxlength="100" style="width: 240px" />
          <span class="label-gap">归属组织</span>
          <el-tree-select
            v-model="form.orgId"
            :data="orgTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" style="width: 150px">
            <el-option v-for="(label, value) in SUPPLIER_CATEGORY_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">状态</span>
          <el-select v-model="form.status" style="width: 130px">
            <el-option v-for="(label, value) in SUPPLIER_STATUS_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" maxlength="64" style="width: 200px" />
          <span class="label-gap">电话</span>
          <el-input v-model="form.phone" maxlength="64" style="width: 200px" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="500" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNo" maxlength="64" style="width: 260px" />
          <span class="label-gap">币种</span>
          <el-input v-model="form.currency" maxlength="8" style="width: 100px" />
        </el-form-item>
        <el-form-item label="账期">
          <el-input v-model="form.paymentTerms" maxlength="100" placeholder="月结 60 天 / 预付 30%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detail ? `${detail.code} ${detail.name}` : '供应商'" size="860px">
      <div v-if="detail" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">
              {{ SUPPLIER_STATUS_LABEL[detail.status] ?? detail.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类别">
            {{ SUPPLIER_CATEGORY_LABEL[detail.category] ?? detail.category }}
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.contact || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="归属组织">{{ detail.orgName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="税号">{{ detail.taxNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="币种/账期">
            {{ detail.currency }} / {{ detail.paymentTerms || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="评级">
            <template v-if="detail.grade">
              <el-tag :type="gradeType(detail.grade)" size="small">{{ detail.grade }} 级</el-tag>
              <span class="score">{{ detail.ratingScore ?? '-' }} 分</span>
            </template>
            <span v-else class="muted">未评级</span>
          </el-descriptions-item>
          <el-descriptions-item label="评级时间">{{ detail.ratedAt ? detail.ratedAt.substring(0, 10) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-bar">
          <template v-if="canWrite">
            <el-button size="small" @click="openEdit">编辑供应商</el-button>
            <el-button size="small" type="primary" plain @click="openPartDialog(null)">新增零件关系</el-button>
            <el-button size="small" type="danger" plain @click="onRemove">删除供应商</el-button>
          </template>
        </div>

        <div class="section-title">零件关系（{{ detail.partCount }} 条，首选 {{ detail.preferredCount }}）</div>
        <el-table :data="detail.parts" border size="small">
          <el-table-column prop="itemCode" label="零件编码" width="140" />
          <el-table-column prop="itemName" label="零件名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="供货类型" width="100" align="center">
            <template #default="{ row }">{{ SUPPLY_TYPE_LABEL[row.supplyType] ?? row.supplyType }}</template>
          </el-table-column>
          <el-table-column label="首选" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.preferred" type="success" size="small">首选</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="交期(天)" width="90" align="center">
            <template #default="{ row }">{{ row.leadTimeDays ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="MOQ" width="90" align="right">
            <template #default="{ row }">{{ row.moq ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="参考单价" width="110" align="right">
            <template #default="{ row }">{{ row.price ?? '-' }} {{ row.currency }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '在用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canWrite" label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPartDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="onRemovePart(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.parts.length" description="还没有关联零件" :image-size="50" />

        <div class="section-title">评级记录（{{ ratings.length }}）</div>
        <div class="action-bar">
          <el-button v-if="canWrite" size="small" type="primary" plain @click="openRatingDialog">
            发起评级
          </el-button>
          <span class="hint">确认后等级与分数回写供应商；低于等于方案暂停档时供应商状态自动置为暂停</span>
        </div>
        <el-table :data="ratings" border size="small">
          <el-table-column prop="period" label="周期" width="100" />
          <el-table-column label="方案" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.schemeName || row.schemeCode }}</template>
          </el-table-column>
          <el-table-column label="总分" width="90" align="right">
            <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="等级" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.grade" :type="gradeType(row.grade)" size="small">{{ row.grade }}</el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'info'" size="small">
                {{ RATING_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRatingDetail(row.id)">明细</el-button>
              <template v-if="canWrite && row.status === 'DRAFT'">
                <el-button link type="success" @click="onConfirmRating(row)">确认</el-button>
                <el-button link type="danger" @click="onRemoveRating(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!ratings.length" description="还没有评级记录" :image-size="50" />

        <div class="section-title">准入评审（{{ admissions.length }}）</div>
        <div class="action-bar">
          <el-button v-if="canWrite" size="small" type="primary" plain @click="openAdmissionDialog">
            发起准入评审
          </el-button>
          <span class="hint">只有准入评审通过才会把供应商置为合格；评级再高也不会自动准入</span>
        </div>
        <el-table :data="admissions" border size="small">
          <el-table-column prop="code" label="评审单号" width="130" />
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }">{{ ADMISSION_TYPE_LABEL[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column label="计划日期" width="110" align="center">
            <template #default="{ row }">{{ row.plannedDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="检查项" width="120" align="center">
            <template #default="{ row }">
              {{ row.passCount }}/{{ row.itemCount }} 通过<template v-if="row.failCount">
                ，<span class="fail-count">{{ row.failCount }} 项不通过</span>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="admissionStatusType(row.status)" size="small">
                {{ ADMISSION_STATUS_LABEL[row.status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAdmissionDetail(row.id)">明细</el-button>
              <template v-if="canWrite && row.status === 'DRAFT'">
                <el-button link type="success" @click="onSubmitAdmission(row)">提交</el-button>
                <el-button link type="danger" @click="onRemoveAdmission(row)">删除</el-button>
              </template>
              <template v-if="canWrite && row.status === 'SUBMITTED'">
                <el-button link type="success" @click="onApproveAdmission(row)">通过</el-button>
                <el-button link type="danger" @click="onRejectAdmission(row)">驳回</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!admissions.length" description="还没有准入评审" :image-size="50" />
      </div>
    </el-drawer>

    <!-- 零件关系 -->
    <el-dialog v-model="partVisible" :title="partForm.id ? '编辑零件关系' : '新增零件关系'" width="600px">
      <el-form label-width="100px">
        <el-form-item label="零件" required>
          <el-select
            v-model="partForm.objectId"
            filterable
            remote
            clearable
            :remote-method="searchItems"
            :loading="itemLoading"
            placeholder="输入编码/名称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="item in itemOptions"
              :key="item.id"
              :label="`${item.code} ${item.name}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供货类型">
          <el-select v-model="partForm.supplyType" style="width: 150px">
            <el-option v-for="(label, value) in SUPPLY_TYPE_LABEL" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="label-gap">首选</span>
          <el-switch v-model="partForm.preferred" />
          <span class="label-gap">状态</span>
          <el-switch v-model="partForm.active" active-text="在用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="交期(天)">
          <el-input-number v-model="partForm.leadTimeDays" :min="0" :max="3650" style="width: 140px" />
          <span class="label-gap">MOQ</span>
          <el-input-number v-model="partForm.moq" :min="0" :precision="0" style="width: 140px" />
        </el-form-item>
        <el-form-item label="参考单价">
          <el-input-number v-model="partForm.price" :min="0" :precision="4" style="width: 160px" />
          <span class="label-gap">币种</span>
          <el-input v-model="partForm.currency" maxlength="8" style="width: 100px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="partForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="partVisible = false">取消</el-button>
        <el-button type="primary" :loading="partSaving" @click="onSavePart">保存</el-button>
      </template>
    </el-dialog>

    <!-- 评级方案（维度可配） -->
    <RatingSchemeDialog v-model="schemeVisible" :can-write="canWrite" />

    <!-- 发起评级 -->
    <el-dialog v-model="ratingVisible" title="发起评级" width="720px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="评级方案" required>
          <el-select
            v-model="ratingForm.schemeId"
            placeholder="选择启用的方案"
            style="width: 320px"
            @change="onSchemeChange"
          >
            <el-option
              v-for="scheme in schemeOptions"
              :key="scheme.id"
              :label="`${scheme.code} ${scheme.name}`"
              :value="scheme.id"
            />
          </el-select>
          <span class="label-gap">周期</span>
          <el-input v-model="ratingForm.period" placeholder="2026-Q4" style="width: 130px" />
        </el-form-item>
        <el-form-item label="维度打分">
          <el-table :data="ratingForm.items" border size="small">
            <el-table-column prop="dimName" label="维度" min-width="140" />
            <el-table-column label="权重" width="80" align="right">
              <template #default="{ row }">{{ row.weight }}</template>
            </el-table-column>
            <el-table-column label="得分" width="160">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.score"
                  :min="0"
                  :max="row.maxScore"
                  :precision="2"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="满分" width="80" align="right">
              <template #default="{ row }">{{ row.maxScore }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.comment" maxlength="500" placeholder="扣分原因（可选）" />
              </template>
            </el-table-column>
          </el-table>
          <div class="rating-tip">
            总分 = Σ(得分 / 满分 × 权重)；当前预估
            <b>{{ previewScore }}</b> 分，等级 <b>{{ previewGrade || '-' }}</b>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ratingForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ratingVisible = false">取消</el-button>
        <el-button type="primary" :loading="ratingSaving" @click="onSaveRating">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 评级明细 -->
    <el-dialog v-model="ratingDetailVisible" title="评级明细" width="680px" destroy-on-close>
      <el-descriptions v-if="ratingDetail" :column="3" border size="small">
        <el-descriptions-item label="周期">{{ ratingDetail.period }}</el-descriptions-item>
        <el-descriptions-item label="方案">{{ ratingDetail.schemeName }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ ratingDetail.totalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="等级">{{ ratingDetail.grade || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ RATING_STATUS_LABEL[ratingDetail.status] ?? ratingDetail.status }}
        </el-descriptions-item>
        <el-descriptions-item label="确认时间">
          {{ ratingDetail.confirmedAt ? ratingDetail.confirmedAt.substring(0, 16).replace('T', ' ') : '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table v-if="ratingDetail" :data="ratingDetail.items" border size="small" class="rating-items">
        <el-table-column prop="dimName" label="维度" width="120" />
        <el-table-column prop="weight" label="权重" width="80" align="right" />
        <el-table-column prop="score" label="得分" width="80" align="right" />
        <el-table-column prop="maxScore" label="满分" width="80" align="right" />
        <el-table-column prop="comment" label="说明" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 发起准入评审 -->
    <el-dialog v-model="admissionVisible" title="发起准入评审" width="820px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="评审类型">
          <el-select v-model="admissionForm.type" style="width: 150px">
            <el-option
              v-for="(label, value) in ADMISSION_TYPE_LABEL"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
          <span class="label-gap">计划日期</span>
          <el-date-picker
            v-model="admissionForm.plannedDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 160px"
          />
          <span class="label-gap">评审组长</span>
          <el-input v-model="admissionForm.leader" maxlength="64" style="width: 140px" />
        </el-form-item>
        <el-form-item label="检查项">
          <div class="dims">
            <el-table :data="admissionForm.items" border size="small">
              <el-table-column label="检查项" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.checkItem" maxlength="500" />
                </template>
              </el-table-column>
              <el-table-column label="结果" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.result" style="width: 100%">
                    <el-option
                      v-for="(label, value) in ADMISSION_RESULT_LABEL"
                      :key="value"
                      :label="label"
                      :value="value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="证据/说明" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.evidence" maxlength="500" placeholder="证书编号/样品单号等" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeAdmissionItem($index)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="dim-footer">
              <el-button link type="primary" @click="addAdmissionItem">+ 增加检查项</el-button>
              <span class="hint">通过评审要求：无「不通过」项，且至少一项「通过」</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="admissionForm.remark" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="admissionSaving" @click="onSaveAdmission">
          保存草稿
        </el-button>
      </template>
    </el-dialog>

    <!-- 准入评审明细 -->
    <el-dialog v-model="admissionDetailVisible" title="准入评审明细" width="760px" destroy-on-close>
      <el-descriptions v-if="admissionDetail" :column="3" border size="small">
        <el-descriptions-item label="评审单号">{{ admissionDetail.code }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          {{ ADMISSION_TYPE_LABEL[admissionDetail.type] ?? admissionDetail.type }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="admissionStatusType(admissionDetail.status)" size="small">
            {{ ADMISSION_STATUS_LABEL[admissionDetail.status] ?? admissionDetail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ admissionDetail.plannedDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际日期">{{ admissionDetail.actualDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评审组长">{{ admissionDetail.leader || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结论" :span="3">
          {{ admissionDetail.conclusion || admissionDetail.rejectReason || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table v-if="admissionDetail" :data="admissionDetail.items" border size="small" class="rating-items">
        <el-table-column prop="seq" label="#" width="50" align="center" />
        <el-table-column prop="checkItem" label="检查项" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="admissionResultType(row.result)" size="small">
              {{ ADMISSION_RESULT_LABEL[row.result] ?? row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evidence" label="证据/说明" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  admissionApi,
  ADMISSION_RESULT_LABEL,
  ADMISSION_STATUS_LABEL,
  ADMISSION_TYPE_LABEL,
  ratingApi,
  RATING_STATUS_LABEL,
  supplierApi,
  SUPPLIER_CATEGORY_LABEL,
  SUPPLIER_STATUS_LABEL,
  SUPPLY_TYPE_LABEL,
  type AdmissionDetail,
  type AdmissionRow,
  type GradeBand,
  type RatingSchemeDetail,
  type RatingSchemeRow,
  type SupplierDetail,
  type SupplierPartRow,
  type SupplierRatingDetail,
  type SupplierRatingRow,
  type SupplierRow
} from '@/api/supplier'
import { itemApi, type ItemSummary } from '@/api/item'
import { orgApi, type OrgNode } from '@/api/system'
import { useUserStore } from '@/stores/user'
import RatingSchemeDialog from './RatingSchemeDialog.vue'

const userStore = useUserStore()
const canWrite = computed(() => userStore.can('sup.supplier.write'))

const loading = ref(false)
const saving = ref(false)
const rows = ref<SupplierRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const category = ref('')
const status = ref('')
const orgTree = ref<OrgNode[]>([])

const formVisible = ref(false)
const form = reactive({
  id: '',
  code: '',
  name: '',
  shortName: '',
  category: 'MATERIAL',
  status: 'PENDING',
  contact: '',
  phone: '',
  email: '',
  address: '',
  taxNo: '',
  currency: 'CNY',
  paymentTerms: '',
  orgId: null as string | null,
  remark: ''
})

const detailVisible = ref(false)
const detail = ref<SupplierDetail | null>(null)

const partVisible = ref(false)
const partSaving = ref(false)
const itemLoading = ref(false)
const itemOptions = ref<ItemSummary[]>([])
const partForm = reactive({
  id: '',
  objectId: '',
  supplyType: 'BUY',
  preferred: false,
  active: true,
  leadTimeDays: 0 as number | null,
  moq: 0 as number | null,
  price: 0 as number | null,
  currency: 'CNY',
  remark: ''
})

/* ------------------------------------------------------------ 评级（P8-5-1） */

interface RatingItemDraft {
  dimId: string
  dimName: string
  weight: number
  maxScore: number
  score: number
  comment: string
}

const schemeVisible = ref(false)
const ratings = ref<SupplierRatingRow[]>([])
const schemeOptions = ref<RatingSchemeRow[]>([])
const ratingScheme = ref<RatingSchemeDetail | null>(null)
const ratingVisible = ref(false)
const ratingSaving = ref(false)
const ratingDetailVisible = ref(false)
const ratingDetail = ref<SupplierRatingDetail | null>(null)
const ratingForm = reactive({
  schemeId: '',
  period: defaultPeriod(),
  remark: '',
  items: [] as RatingItemDraft[]
})

const previewScore = computed(() => {
  const total = ratingForm.items.reduce((sum, item) => {
    if (!item.maxScore) {
      return sum
    }
    return sum + (Number(item.score || 0) / item.maxScore) * item.weight
  }, 0)
  return Math.round(total * 100) / 100
})

const previewGrade = computed(() => {
  const bands: GradeBand[] = ratingScheme.value?.gradeRule ?? []
  const hit = bands.find((band) => previewScore.value >= band.min)
  return hit ? hit.grade : bands.length ? bands[bands.length - 1].grade : ''
})

/* ------------------------------------------------------------ 准入（P8-5-2） */

interface AdmissionItemDraft {
  checkItem: string
  result: string
  evidence: string
}

const DEFAULT_ADMISSION_ITEMS = [
  '营业执照与经营范围',
  '质量体系证书（ISO9001/IATF16949）',
  '样品承认与试产验证',
  '现场审核（过程能力/设备/人员）',
  '产能与交付能力评估',
  '财务与信用状况'
]

const admissions = ref<AdmissionRow[]>([])
const admissionVisible = ref(false)
const admissionSaving = ref(false)
const admissionDetailVisible = ref(false)
const admissionDetail = ref<AdmissionDetail | null>(null)
const admissionForm = reactive({
  type: 'INITIAL',
  plannedDate: '',
  leader: '',
  remark: '',
  items: [] as AdmissionItemDraft[]
})

onMounted(async () => {
  await loadOrgs()
  await reload()
})

async function loadOrgs(): Promise<void> {
  try {
    orgTree.value = await orgApi.tree()
  } catch {
    orgTree.value = []
  }
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    const result = await supplierApi.page({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      status: status.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载供应商失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(value: number): void {
  page.value = value
  void reload()
}

/* ------------------------------------------------------------ 供应商 */

function openCreate(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.shortName = ''
  form.category = 'MATERIAL'
  form.status = 'PENDING'
  form.contact = ''
  form.phone = ''
  form.email = ''
  form.address = ''
  form.taxNo = ''
  form.currency = 'CNY'
  form.paymentTerms = ''
  form.orgId = null
  form.remark = ''
  formVisible.value = true
}

function openEdit(): void {
  if (!detail.value) {
    return
  }
  form.id = detail.value.id
  form.code = detail.value.code
  form.name = detail.value.name
  form.shortName = detail.value.shortName ?? ''
  form.category = detail.value.category
  form.status = detail.value.status
  form.contact = detail.value.contact ?? ''
  form.phone = detail.value.phone ?? ''
  form.email = detail.value.email ?? ''
  form.address = detail.value.address ?? ''
  form.taxNo = detail.value.taxNo ?? ''
  form.currency = detail.value.currency
  form.paymentTerms = detail.value.paymentTerms ?? ''
  form.orgId = detail.value.orgId ?? null
  form.remark = detail.value.remark ?? ''
  formVisible.value = true
}

async function onSave(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      shortName: form.shortName.trim() || null,
      category: form.category,
      status: form.status,
      contact: form.contact.trim() || null,
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      address: form.address.trim() || null,
      taxNo: form.taxNo.trim() || null,
      currency: form.currency.trim() || 'CNY',
      paymentTerms: form.paymentTerms.trim() || null,
      orgId: form.orgId,
      remark: form.remark.trim() || null
    }
    const saved = form.id
      ? await supplierApi.update(form.id, payload)
      : await supplierApi.create(payload)
    ElMessage.success('已保存')
    formVisible.value = false
    await reload()
    if (form.id) {
      detail.value = saved
    } else {
      await openDetail(saved.id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function openDetail(id: string): Promise<void> {
  try {
    detail.value = await supplierApi.detail(id)
    detailVisible.value = true
    await loadRatings(id)
    await loadAdmissions(id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

async function loadRatings(supplierId: string): Promise<void> {
  try {
    ratings.value = await ratingApi.bySupplier(supplierId)
  } catch {
    ratings.value = []
  }
}

async function loadAdmissions(supplierId: string): Promise<void> {
  try {
    admissions.value = await admissionApi.bySupplier(supplierId)
  } catch {
    admissions.value = []
  }
}

async function onRemove(): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除供应商 ${detail.value.code}？有在用零件关系时会被拒绝。`,
      '删除供应商',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await supplierApi.remove(detail.value.id)
    ElMessage.success('已删除')
    detailVisible.value = false
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 零件关系 */

function openPartDialog(row: SupplierPartRow | null): void {
  partForm.id = row?.id ?? ''
  partForm.objectId = row?.objectId ?? ''
  partForm.supplyType = row?.supplyType ?? 'BUY'
  partForm.preferred = row?.preferred ?? false
  partForm.active = (row?.status ?? 'ACTIVE') === 'ACTIVE'
  partForm.leadTimeDays = row?.leadTimeDays ?? 0
  partForm.moq = row?.moq ?? 0
  partForm.price = row?.price ?? 0
  partForm.currency = row?.currency ?? 'CNY'
  partForm.remark = row?.remark ?? ''
  itemOptions.value = row
    ? [
        {
          id: row.objectId,
          code: row.itemCode ?? '',
          name: row.itemName ?? ''
        } as ItemSummary
      ]
    : []
  partVisible.value = true
}

async function searchItems(keywordText: string): Promise<void> {
  if (!keywordText) {
    itemOptions.value = []
    return
  }
  itemLoading.value = true
  try {
    const result = await itemApi.list({ keyword: keywordText, page: 1, size: 20 })
    itemOptions.value = result.items
  } catch {
    itemOptions.value = []
  } finally {
    itemLoading.value = false
  }
}

async function onSavePart(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!partForm.objectId) {
    ElMessage.warning('请选择零件')
    return
  }
  partSaving.value = true
  try {
    const payload = {
      objectType: 'PdmItem',
      objectId: partForm.objectId,
      supplyType: partForm.supplyType,
      preferred: partForm.preferred,
      leadTimeDays: partForm.leadTimeDays,
      moq: partForm.moq,
      price: partForm.price,
      currency: partForm.currency.trim() || 'CNY',
      status: partForm.active ? 'ACTIVE' : 'INACTIVE',
      remark: partForm.remark.trim() || null
    }
    detail.value = partForm.id
      ? await supplierApi.updatePart(partForm.id, payload)
      : await supplierApi.addPart(detail.value.id, payload)
    partVisible.value = false
    ElMessage.success('已保存')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存零件关系失败')
  } finally {
    partSaving.value = false
  }
}

async function onRemovePart(row: SupplierPartRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除与零件 ${row.itemCode} 的关系？`, '删除关系', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await supplierApi.removePart(row.id)
    detail.value = await supplierApi.detail(detail.value.id)
    ElMessage.success('已删除')
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ------------------------------------------------------------ 评级操作 */

function defaultPeriod(): string {
  const now = new Date()
  return `${now.getFullYear()}-Q${Math.floor(now.getMonth() / 3) + 1}`
}

async function openRatingDialog(): Promise<void> {
  try {
    schemeOptions.value = await ratingApi.schemes({ status: 'ACTIVE' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载评级方案失败')
    return
  }
  if (!schemeOptions.value.length) {
    ElMessage.warning('还没有启用的评级方案，请先在「评级方案」里创建')
    return
  }
  ratingForm.schemeId = schemeOptions.value[0].id
  ratingForm.period = defaultPeriod()
  ratingForm.remark = ''
  await onSchemeChange(ratingForm.schemeId)
  ratingVisible.value = true
}

async function onSchemeChange(schemeId: string): Promise<void> {
  if (!schemeId) {
    ratingForm.items = []
    ratingScheme.value = null
    return
  }
  try {
    const scheme = await ratingApi.scheme(schemeId)
    ratingScheme.value = scheme
    ratingForm.items = scheme.dims
      .filter((dim) => dim.enabled)
      .map((dim) => ({
        dimId: dim.id,
        dimName: dim.name,
        weight: dim.weight,
        maxScore: dim.maxScore,
        score: dim.maxScore,
        comment: ''
      }))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载方案失败')
  }
}

async function onSaveRating(): Promise<void> {
  if (!detail.value) {
    return
  }
  if (!ratingForm.schemeId || !ratingForm.period.trim()) {
    ElMessage.warning('请选择方案并填写周期')
    return
  }
  if (!ratingForm.items.length) {
    ElMessage.warning('该方案没有启用维度，无法评级')
    return
  }
  ratingSaving.value = true
  try {
    await ratingApi.create({
      supplierId: detail.value.id,
      schemeId: ratingForm.schemeId,
      period: ratingForm.period.trim(),
      remark: ratingForm.remark.trim() || null,
      items: ratingForm.items.map((item) => ({
        dimId: item.dimId,
        score: item.score,
        comment: item.comment.trim() || null
      }))
    })
    ElMessage.success('评级草稿已保存，确认后才会回写供应商等级')
    ratingVisible.value = false
    await loadRatings(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存评级失败')
  } finally {
    ratingSaving.value = false
  }
}

async function onConfirmRating(row: SupplierRatingRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认 ${row.period} 评级？确认后等级与分数回写供应商，且不可再修改。`,
      '确认评级',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await ratingApi.confirm(row.id)
    ElMessage.success('评级已确认')
    detail.value = await supplierApi.detail(detail.value.id)
    await loadRatings(detail.value.id)
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认评级失败')
  }
}

async function onRemoveRating(row: SupplierRatingRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除 ${row.period} 的评级草稿？`, '删除评级', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await ratingApi.remove(row.id)
    ElMessage.success('已删除')
    await loadRatings(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function openRatingDetail(id: string): Promise<void> {
  try {
    ratingDetail.value = await ratingApi.detail(id)
    ratingDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载评级明细失败')
  }
}

/* ------------------------------------------------------------ 准入操作 */

function openAdmissionDialog(): void {
  admissionForm.type = 'INITIAL'
  admissionForm.plannedDate = ''
  admissionForm.leader = ''
  admissionForm.remark = ''
  admissionForm.items = DEFAULT_ADMISSION_ITEMS.map((item) => ({
    checkItem: item,
    result: 'NA',
    evidence: ''
  }))
  admissionVisible.value = true
}

function addAdmissionItem(): void {
  admissionForm.items.push({ checkItem: '', result: 'NA', evidence: '' })
}

function removeAdmissionItem(index: number): void {
  admissionForm.items.splice(index, 1)
}

async function onSaveAdmission(): Promise<void> {
  if (!detail.value) {
    return
  }
  const items = admissionForm.items.filter((item) => item.checkItem.trim())
  if (!items.length) {
    ElMessage.warning('至少需要一项检查项')
    return
  }
  admissionSaving.value = true
  try {
    await admissionApi.create({
      supplierId: detail.value.id,
      type: admissionForm.type,
      plannedDate: admissionForm.plannedDate || null,
      leader: admissionForm.leader.trim() || null,
      remark: admissionForm.remark.trim() || null,
      items: items.map((item) => ({
        checkItem: item.checkItem.trim(),
        result: item.result,
        evidence: item.evidence.trim() || null
      }))
    })
    ElMessage.success('准入评审草稿已保存')
    admissionVisible.value = false
    await loadAdmissions(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存准入评审失败')
  } finally {
    admissionSaving.value = false
  }
}

async function onSubmitAdmission(row: AdmissionRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await admissionApi.submit(row.id)
    ElMessage.success('已提交，检查项已冻结')
    await loadAdmissions(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function onApproveAdmission(row: AdmissionRow): Promise<void> {
  if (!detail.value) {
    return
  }
  let conclusion = ''
  try {
    const result = await ElMessageBox.prompt(
      '通过后供应商将置为「合格」，请填写评审结论（可留空使用默认结论）',
      '通过准入评审',
      { confirmButtonText: '通过', cancelButtonText: '取消', inputPlaceholder: '资料齐全，无重大不符合' }
    )
    conclusion = result.value ?? ''
  } catch {
    return
  }
  try {
    await admissionApi.approve(row.id, conclusion.trim() || undefined)
    ElMessage.success('准入已通过，供应商已转合格')
    detail.value = await supplierApi.detail(detail.value.id)
    await loadAdmissions(detail.value.id)
    await reload()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通过失败')
  }
}

async function onRejectAdmission(row: AdmissionRow): Promise<void> {
  if (!detail.value) {
    return
  }
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请填写驳回原因', '驳回准入评审', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：体系证书过期，需换证后重评',
      inputValidator: (value: string) => (value && value.trim() ? true : '驳回原因必填')
    })
    reason = result.value ?? ''
  } catch {
    return
  }
  try {
    await admissionApi.reject(row.id, reason.trim())
    ElMessage.success('已驳回')
    await loadAdmissions(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '驳回失败')
  }
}

async function onRemoveAdmission(row: AdmissionRow): Promise<void> {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除准入评审草稿 ${row.code}？`, '删除评审', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await admissionApi.remove(row.id)
    ElMessage.success('已删除')
    await loadAdmissions(detail.value.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function openAdmissionDetail(id: string): Promise<void> {
  try {
    admissionDetail.value = await admissionApi.detail(id)
    admissionDetailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载准入明细失败')
  }
}

/* ------------------------------------------------------------ 展示辅助 */

function gradeType(grade: string): 'success' | 'primary' | 'info' | 'warning' | 'danger' {
  if (grade === 'A') {
    return 'success'
  }
  if (grade === 'B') {
    return 'primary'
  }
  if (grade === 'C') {
    return 'warning'
  }
  if (grade === 'D') {
    return 'danger'
  }
  return 'info'
}

function admissionStatusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'APPROVED') {
    return 'success'
  }
  if (value === 'SUBMITTED') {
    return 'warning'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

function admissionResultType(value: string): 'success' | 'info' | 'danger' {
  if (value === 'PASS') {
    return 'success'
  }
  if (value === 'FAIL') {
    return 'danger'
  }
  return 'info'
}


function statusType(value: string): 'success' | 'info' | 'warning' | 'danger' {
  if (value === 'QUALIFIED') {
    return 'success'
  }
  if (value === 'PENDING') {
    return 'warning'
  }
  if (value === 'DISQUALIFIED') {
    return 'danger'
  }
  return 'info'
}
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
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.action-bar {
  margin: 12px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.section-title {
  font-weight: 600;
  margin: 12px 0 8px;
  padding-left: 6px;
  border-left: 3px solid #409eff;
}

.muted {
  color: #909399;
}

.score {
  margin-left: 6px;
  color: #606266;
  font-size: 12px;
}

.rating-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.rating-items {
  margin-top: 12px;
}

.fail-count {
  color: #f56c6c;
}

.label-gap {
  margin: 0 8px;
  color: #606266;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
