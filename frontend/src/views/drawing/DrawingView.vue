<template>
  <div class="drawing-view">
    <header class="dv-header">
      <div class="dv-title">
        <span>图纸管理</span>
        <span class="dv-sub">非标自制件 / 通用自制件 ↔ 图纸（Specification）</span>
      </div>
      <div class="dv-header-right">
        <el-tag size="small" effect="plain" type="info">独立模块</el-tag>
      </div>
    </header>

    <div class="dv-body">
      <DrawingPanel :keyword="keyword" :item-type="itemType">
        <template #filters>
          <el-input
            v-model="keyword"
            size="small"
            clearable
            placeholder="图号 / 物料编码 / 名称"
            style="width: 240px"
          />
          <el-select v-model="itemType" size="small" clearable placeholder="物料类型" style="width: 150px">
            <el-option v-for="item in DRAWING_ITEM_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </DrawingPanel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DrawingPanel from '@/components/business/DrawingPanel.vue'
import { DRAWING_ITEM_TYPES } from '@/api/drawing'

const keyword = ref('')
const itemType = ref('')
</script>

<style scoped>
.drawing-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  padding: 12px;
  gap: 8px;
  background: #f2f5f9;
}

.dv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 12px;
  background: linear-gradient(180deg, #f8fbff, #e8f0fa);
  border: 1px solid #cfd9e6;
}

.dv-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1f4e94;
}

.dv-sub {
  font-size: 12px;
  font-weight: 400;
  color: #6b7a90;
}

.dv-body {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid #cfd9e6;
}
</style>
