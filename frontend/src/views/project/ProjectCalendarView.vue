<template>
  <div class="page">
    <div class="toolbar">
      <el-button link type="primary" @click="back">← 返回项目列表</el-button>
      <span class="prj-title">{{ board?.projectCode }} {{ board?.projectName }}</span>
      <el-button-group>
        <el-button @click="shiftMonth(-1)">上月</el-button>
        <el-button @click="goToday">今天</el-button>
        <el-button @click="shiftMonth(1)">下月</el-button>
      </el-button-group>
      <span class="month-label">{{ monthLabel }}</span>
      <el-button @click="load">刷新</el-button>
      <el-button type="success" plain @click="openTasks">WBS 任务</el-button>
      <span class="hint">拖动任务条到另一天即可平移排期（子任务跟着走，不自动改父任务与依赖）</span>
    </div>

    <div class="weekday-row">
      <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
    </div>

    <div v-loading="loading" class="month-grid">
      <div
        v-for="cell in cells"
        :key="cell.date"
        class="day-cell"
        :class="{ 'out-month': !cell.inMonth, today: cell.isToday, 'drag-over': dragOverDate === cell.date }"
        @dragover.prevent="onDragOver(cell.date)"
        @dragleave="onDragLeave(cell.date)"
        @drop.prevent="onDrop(cell.date)"
      >
        <div class="day-head">
          <span class="day-no">{{ cell.dayNo }}</span>
          <span v-if="cell.tasks.length" class="day-count">{{ cell.tasks.length }}</span>
        </div>
        <div class="day-body">
          <div
            v-for="task in cell.tasks.slice(0, 3)"
            :key="`${cell.date}-${task.id}`"
            class="task-chip"
            :class="[`status-${task.status.toLowerCase()}`, { dragging: dragTask?.id === task.id }]"
            :title="`${task.wbsCode} ${task.name}｜${task.planStart} ~ ${task.planEnd}｜${statusLabel(task.status)}`"
            draggable="true"
            @dragstart="onDragStart(task)"
            @dragend="onDragEnd"
          >
            <span class="chip-wbs">{{ task.wbsCode }}</span>
            <span class="chip-name">{{ task.name }}</span>
          </div>
          <div v-if="cell.tasks.length > 3" class="more">+{{ cell.tasks.length - 3 }} 项</div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !board?.tasks.length" description="本月没有已排期的任务" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi, type CalendarBoard, type TaskCalendarRow } from '@/api/project'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canWrite = computed(() => userStore.can('prj.project.write'))

const projectId = String(route.params.id ?? '')
const loading = ref(false)
const board = ref<CalendarBoard | null>(null)
const month = ref(startOfMonth(new Date()))
const dragTask = ref<TaskCalendarRow | null>(null)
const dragOverDate = ref('')

const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const monthLabel = computed(() => `${month.value.getFullYear()} 年 ${month.value.getMonth() + 1} 月`)

/** 单元格：从当月首日所在周的周一算起，固定 6 周 42 格。 */
const cells = computed(() => {
  const byDate = new Map<string, TaskCalendarRow[]>()
  ;(board.value?.tasks ?? []).forEach((task) => {
    eachDay(task.planStart, task.planEnd, (date) => {
      const list = byDate.get(date) ?? []
      list.push(task)
      byDate.set(date, list)
    })
  })
  const first = startOfWeek(month.value)
  const todayText = formatDate(new Date())
  const result: Array<{
    date: string
    dayNo: number
    inMonth: boolean
    isToday: boolean
    tasks: TaskCalendarRow[]
  }> = []
  for (let index = 0; index < 42; index += 1) {
    const date = addDays(first, index)
    const text = formatDate(date)
    result.push({
      date: text,
      dayNo: date.getDate(),
      inMonth: date.getMonth() === month.value.getMonth(),
      isToday: text === todayText,
      tasks: byDate.get(text) ?? []
    })
  }
  return result
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const from = formatDate(month.value)
    const to = formatDate(new Date(month.value.getFullYear(), month.value.getMonth() + 1, 0))
    board.value = await taskApi.calendar(projectId, from, to)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载日历失败')
  } finally {
    loading.value = false
  }
}

function shiftMonth(delta: number): void {
  month.value = new Date(month.value.getFullYear(), month.value.getMonth() + delta, 1)
  void load()
}

function goToday(): void {
  month.value = startOfMonth(new Date())
  void load()
}

function onDragStart(task: TaskCalendarRow): void {
  if (!canWrite.value) {
    ElMessage.warning('没有项目编辑权限，不能拖拽排期')
    return
  }
  dragTask.value = task
}

function onDragEnd(): void {
  dragTask.value = null
  dragOverDate.value = ''
}

function onDragOver(date: string): void {
  if (dragTask.value) {
    dragOverDate.value = date
  }
}

function onDragLeave(date: string): void {
  if (dragOverDate.value === date) {
    dragOverDate.value = ''
  }
}

async function onDrop(date: string): Promise<void> {
  const task = dragTask.value
  dragOverDate.value = ''
  dragTask.value = null
  if (!task || !canWrite.value) {
    return
  }
  const delta = daysBetween(parseDate(task.planStart), parseDate(date))
  if (delta === 0) {
    return
  }
  try {
    await taskApi.reschedule(
      task.id,
      formatDate(addDays(parseDate(task.planStart), delta)),
      formatDate(addDays(parseDate(task.planEnd), delta))
    )
    ElMessage.success(`「${task.name}」已${delta > 0 ? '后移' : '前移'} ${Math.abs(delta)} 天`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '改期失败')
  }
}

function eachDay(start: string, end: string, visit: (date: string) => void): void {
  const from = parseDate(start)
  const to = parseDate(end)
  // 防御脏数据：跨度超过一年就只画起止两点
  const span = daysBetween(from, to)
  if (span < 0) {
    return
  }
  if (span > 366) {
    visit(start)
    visit(end)
    return
  }
  for (let index = 0; index <= span; index += 1) {
    visit(formatDate(addDays(from, index)))
  }
}

function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    SUBMITTED: '待审核',
    DONE: '已完成',
    BLOCKED: '受阻'
  }
  return labels[status] ?? status
}

function back(): void {
  router.push('/project')
}

function openTasks(): void {
  router.push(`/project/${projectId}/tasks`)
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function startOfWeek(date: Date): Date {
  const copy = new Date(date)
  const day = copy.getDay() === 0 ? 7 : copy.getDay()
  copy.setDate(copy.getDate() - day + 1)
  copy.setHours(0, 0, 0, 0)
  return copy
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

function daysBetween(from: Date, to: Date): number {
  const oneDay = 24 * 60 * 60 * 1000
  return Math.round((to.getTime() - from.getTime()) / oneDay)
}

function parseDate(value: string): Date {
  const [year, monthPart, day] = value.split('-').map(Number)
  return new Date(year, (monthPart ?? 1) - 1, day ?? 1)
}

function formatDate(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>

<style scoped>
.month-label {
  font-weight: 600;
  margin: 0 6px;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
}

.weekday {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 4px 0;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: minmax(110px, auto);
  border-top: 1px solid #ebeef5;
  border-left: 1px solid #ebeef5;
}

.day-cell {
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  padding: 4px 6px;
  min-height: 110px;
}

.day-cell.out-month {
  background: #fafafa;
  color: #c0c4cc;
}

.day-cell.today {
  background: #eff6ff;
  box-shadow: inset 0 0 0 1px #409eff;
}

.day-cell.drag-over {
  background: #f0f9eb;
}

.day-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
}

.day-count {
  color: #909399;
}

.task-chip {
  display: flex;
  gap: 4px;
  align-items: center;
  font-size: 12px;
  line-height: 18px;
  padding: 1px 4px;
  margin-bottom: 3px;
  border-radius: 3px;
  cursor: grab;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.task-chip.dragging {
  opacity: 0.4;
}

.chip-wbs {
  color: #606266;
  flex: none;
}

.chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-not_started {
  background: #f4f4f5;
  color: #606266;
}

.status-in_progress {
  background: #ecf5ff;
  color: #409eff;
}

.status-submitted {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-done {
  background: #f0f9eb;
  color: #67c23a;
}

.status-blocked {
  background: #fef0f0;
  color: #f56c6c;
}

.more {
  color: #909399;
  font-size: 12px;
}
</style>
