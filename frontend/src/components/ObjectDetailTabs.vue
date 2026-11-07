<template>
  <el-tabs :model-value="modelValue" @update:model-value="onChange">
    <el-tab-pane
      v-for="tab in renderedTabs"
      :key="tab.code"
      :label="tab.name"
      :name="tab.code"
      :lazy="lazyCodes.includes(tab.code)"
    >
      <slot :name="tab.code" />
    </el-tab-pane>
  </el-tabs>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, useSlots, watch } from 'vue'
import { metaApi, type MetaTabDef } from '@/api/system'

/**
 * 对象查看器页签容器（B6）：页签顺序/名称/懒加载由元数据 `meta_tab` 驱动，内容由调用方用插槽提供。
 *
 * <p>为什么这么拆：零部件、图文档、变更单三个详情页原来各自硬编码一套
 * `<el-tabs><el-tab-pane>`，页签增删要改三处，且与元数据里配的页签完全脱节。
 * 现在容器只负责「有哪些页签、顺序、懒加载」，业务内容仍在各页面，改动面最小。</p>
 *
 * <p>元数据里配了但页面没提供插槽的页签自动隐藏（未实现的页签不显示空壳）；
 * 页面独有的页签（如 BOM 展开/汇总、基线）通过 {@code extras} 指定插入位置。</p>
 */
const props = withDefaults(
  defineProps<{
    /** 当前页签（v-model）。 */
    modelValue: string
    /** 元数据实体编码，如 PdmItem / PdmDoc。 */
    entityCode: string
    /** 页面独有页签：{ code, name, after }，after 为插入到哪个页签之后。 */
    extras?: { code: string; name: string; after?: string }[]
    /** 元数据取不到时的兜底页签（保持页面可用，不因一个接口失败整页空白）。 */
    fallback?: { code: string; name: string }[]
    /** 需要懒渲染的页签 code。 */
    lazyCodes?: string[]
  }>(),
  { extras: () => [], fallback: () => [], lazyCodes: () => [] }
)

const emit = defineEmits<{
  'update:modelValue': [string]
  /** 页签切换（页签 code），替代原生 @tab-change 便于页面按 code 懒加载数据。 */
  change: [string]
}>()

const slots = useSlots()
const metaTabs = ref<MetaTabDef[]>([])

onMounted(loadTabs)
watch(() => props.entityCode, loadTabs)

async function loadTabs(): Promise<void> {
  metaTabs.value = []
  try {
    metaTabs.value = await metaApi.entityTabs(props.entityCode)
  } catch {
    // 元数据接口失败时走 fallback，详情页不该因为页签配置挂掉
    metaTabs.value = []
  }
}

const renderedTabs = computed<{ code: string; name: string }[]>(() => {
  const base = metaTabs.value.length
    ? metaTabs.value
        .slice()
        .sort((a, b) => a.sort - b.sort)
        .map((tab) => ({ code: tab.code, name: tab.name }))
    : props.fallback.map((tab) => ({ code: tab.code, name: tab.name }))
  // 没有对应插槽的页签不渲染：元数据可以先配、页面后实现
  const tabs = base.filter((tab) => Boolean(slots[tab.code]))
  for (const extra of props.extras) {
    if (!slots[extra.code] || tabs.some((tab) => tab.code === extra.code)) {
      continue
    }
    const index = extra.after ? tabs.findIndex((tab) => tab.code === extra.after) : -1
    if (index >= 0) {
      tabs.splice(index + 1, 0, { code: extra.code, name: extra.name })
    } else {
      tabs.push({ code: extra.code, name: extra.name })
    }
  }
  return tabs
})

function onChange(value: string | number): void {
  const code = String(value)
  emit('update:modelValue', code)
  emit('change', code)
}
</script>
