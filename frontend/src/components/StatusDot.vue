<template>
  <span class="status-dot" :title="status ?? undefined">
    <span class="dot" :class="tone" />
    <span class="text">{{ text }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 状态展示：色点 + 文字（参考 SIPM 的「绿色圆点 + 正常」与红黄绿灯）。
 *
 * 为什么不直接用 el-tag：列表里一列状态全是彩色块时，颜色会盖过信息本身，
 * 也没法表达"正常 / 进行 / 延误 / 超期"的紧急度差异。色点体积小、语义靠颜色 + 文字双通道。
 */
type Tone = 'ok' | 'running' | 'warn' | 'danger' | 'muted'

const props = defineProps<{
  status?: string | null
  /** 覆盖显示文案；不传则按内置字典翻译，未命中显示原值 */
  label?: string
  /** 直接指定色调（用于不属于状态字典的枚举，如供应方式）；优先于 status 推导 */
  tone?: Tone
}>()

const LABELS: Record<string, string> = {
  // 通用生命周期
  Draft: '草稿',
  InWorkflow: '审批中',
  Released: '已发布',
  Obsolete: '已废弃',
  DRAFT: '草稿',
  IN_WORKFLOW: '审批中',
  RELEASED: '已发布',
  OBSOLETE: '已废弃',
  ARCHIVED: '已归档',
  // 变更
  SUBMITTED: '已提交',
  REVIEWING: '评审中',
  APPROVED: '已批准',
  NOTICED: '已通知',
  EXECUTING: '执行中',
  CLOSED: '已关闭',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
  // 任务 / 质量
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  BLOCKED: '阻塞',
  OPEN: '待处理',
  ASSIGNED: '已指派',
  VERIFYING: '验证中',
  // 状态灯（SIPM 用语）
  NORMAL: '正常',
  DELAYED: '延误',
  OVERDUE: '超期'
}

const TONE_BY_STATUS: Record<string, Tone> = {
  Released: 'ok',
  RELEASED: 'ok',
  CLOSED: 'ok',
  DONE: 'ok',
  APPROVED: 'ok',
  NORMAL: 'ok',
  InWorkflow: 'running',
  IN_PROGRESS: 'running',
  EXECUTING: 'running',
  REVIEWING: 'running',
  SUBMITTED: 'running',
  ASSIGNED: 'running',
  VERIFYING: 'running',
  DELAYED: 'warn',
  NOTICED: 'warn',
  OVERDUE: 'danger',
  BLOCKED: 'danger',
  REJECTED: 'danger',
  CANCELLED: 'danger',
  Draft: 'muted',
  DRAFT: 'muted',
  NOT_STARTED: 'muted',
  Obsolete: 'muted',
  OBSOLETE: 'muted',
  ARCHIVED: 'muted',
  OPEN: 'danger'
}

const text = computed(() => {
  if (props.label) {
    return props.label
  }
  const value = props.status ?? ''
  return LABELS[value] ?? (value || '-')
})

const tone = computed<Tone>(() => props.tone ?? TONE_BY_STATUS[props.status ?? ''] ?? 'muted')
</script>

<style scoped>
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
}

.dot.ok {
  background: var(--plm-ok, #2e9e5b);
}

.dot.running {
  background: var(--plm-running, #1f7ac0);
}

.dot.warn {
  background: var(--plm-warn, #e6a23c);
}

.dot.danger {
  background: var(--plm-danger, #d64541);
}

.dot.muted {
  background: var(--plm-muted, #8a94a6);
}

.text {
  color: var(--plm-text-regular, #4a5a6a);
}
</style>
