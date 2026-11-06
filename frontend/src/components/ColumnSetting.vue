<script setup lang="ts">
import { computed } from 'vue'
import type { ColumnDef } from './columnSetting'

const props = defineProps<{
  /** 全部可选列 */
  columns: ColumnDef[]
  /** 当前显示的列 key（有序） */
  modelValue: string[]
}>()

const emit = defineEmits<{ 'update:modelValue': [string[]] }>()

const visibleKeys = computed(() => props.modelValue)

function toggle(key: string, checked: boolean): void {
  const keys = props.columns.map((column) => column.key).filter((columnKey) => {
    if (columnKey === key) {
      return checked
    }
    return visibleKeys.value.includes(columnKey)
  })
  emit('update:modelValue', keys)
}

function reset(): void {
  emit('update:modelValue', props.columns.map((column) => column.key))
}

function toggleAll(checked: boolean): void {
  emit(
    'update:modelValue',
    checked ? props.columns.map((column) => column.key) : props.columns.filter((c) => c.fixed).map((c) => c.key)
  )
}
</script>

<template>
  <el-popover placement="bottom-end" :width="240" trigger="click">
    <template #reference>
      <el-button size="small">列设置</el-button>
    </template>
    <div class="column-setting">
      <div class="head">
        <span>显示列</span>
        <div>
          <el-button link type="primary" size="small" @click="toggleAll(true)">全选</el-button>
          <el-button link size="small" @click="reset">重置</el-button>
        </div>
      </div>
      <el-checkbox
        v-for="column in columns"
        :key="column.key"
        :model-value="visibleKeys.includes(column.key)"
        :disabled="column.fixed"
        class="item"
        @change="(checked: boolean) => toggle(column.key, checked)"
      >
        {{ column.label }}
      </el-checkbox>
    </div>
  </el-popover>
</template>

<style scoped>
.column-setting {
  max-height: 320px;
  overflow-y: auto;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--plm-border, #dde3ea);
  font-size: 12px;
  color: var(--plm-text-secondary, #5b6778);
}

.item {
  display: flex;
  height: 24px;
  margin-right: 0;
}
</style>
