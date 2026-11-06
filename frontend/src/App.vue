<template>
  <!-- 全局 size="small"：把 Element Plus 默认控件（按钮/输入/表格/分页）压到紧凑档，
       与 styles/design-tokens.css 的 12px 基准配套；移动端由媒体查询回退到可触控尺寸。 -->
  <el-config-provider size="small">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
</script>
