<template>
  <el-select
    v-model="inner"
    filterable
    remote
    clearable
    reserve-keyword
    :remote-method="onSearch"
    :loading="loading"
    :placeholder="placeholder"
    style="width: 100%"
    @change="onChange"
  >
    <el-option
      v-for="row in options"
      :key="`${row.objectType}:${row.id}`"
      :label="`${row.code} ${row.name}`"
      :value="row.id"
    >
      <span class="picker-code">{{ row.code }}</span>
      <span class="picker-name">{{ row.name }}</span>
      <el-tag size="small" type="info">{{ OBJECT_TYPE_LABEL[row.objectType] ?? row.objectType }}</el-tag>
    </el-option>
    <template #empty>
      <div class="picker-empty">
        {{ keyword ? '没有匹配的对象' : '输入编码或名称检索' }}
      </div>
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { lookupApi, OBJECT_TYPE_LABEL, type LookupResult } from '@/api/lookup'

/**
 * 通用对象选择器（B10）：编码/名称模糊检索，替代各页面「手输对象 ID」。
 *
 * <p>为什么做成组件：变更单、替代件、关联对象等 6 处以上都在各自写
 * remote `<el-select>` + 各自 remote-method，契约不统一；抽成一个组件后
 * 数据源只有 `/objects/search` 一处。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 选中的对象 ID（v-model）。 */
    modelValue?: string | null
    /** 固定对象类型（如 PdmItem）；留空表示跨类型检索。 */
    objectType?: string
    placeholder?: string
    limit?: number
  }>(),
  { modelValue: '', objectType: '', placeholder: '编码 / 名称检索', limit: 20 }
)

const emit = defineEmits<{
  'update:modelValue': [string]
  /** 选中对象（清空时为 null），供调用方同步 objectType 等附带字段。 */
  select: [LookupResult | null]
}>()

const inner = ref<string>(props.modelValue ?? '')
const options = ref<LookupResult[]>([])
const loading = ref(false)
const keyword = ref('')

watch(
  () => props.modelValue,
  (value) => {
    inner.value = value ?? ''
  }
)

async function onSearch(text: string): Promise<void> {
  keyword.value = (text ?? '').trim()
  if (!keyword.value) {
    options.value = []
    return
  }
  loading.value = true
  try {
    options.value = await lookupApi.search(keyword.value, props.objectType || undefined, props.limit)
  } finally {
    loading.value = false
  }
}

function onChange(value: string): void {
  emit('update:modelValue', value ?? '')
  emit('select', options.value.find((row) => row.id === value) ?? null)
}
</script>

<style scoped>
.picker-code {
  margin-right: 8px;
  color: var(--el-text-color-regular);
}
.picker-name {
  margin-right: 8px;
}
.picker-empty {
  padding: 10px 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
