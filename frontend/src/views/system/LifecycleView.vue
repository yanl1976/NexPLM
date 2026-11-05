<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>生命周期设计器</span>
          <div>
            <el-button :disabled="!current" @click="addState">新增状态</el-button>
            <el-button :disabled="!current" @click="addTransition">新增迁移</el-button>
            <el-button type="primary" :loading="saving" :disabled="!current" @click="onSave">保存定义</el-button>
          </div>
        </div>
      </template>

      <div class="body">
        <div class="side">
          <div class="side-title">生命周期</div>
          <el-menu :default-active="current?.code" class="life-menu" @select="onSelect">
            <el-menu-item v-for="item in lifecycles" :key="item.code" :index="item.code">
              <div class="menu-item">
                <span>{{ item.name }}</span>
                <el-tag size="small" type="info">{{ item.states.length }} 状态</el-tag>
              </div>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="content" v-if="current">
          <el-descriptions :column="3" border size="small" class="meta">
            <el-descriptions-item label="编码">{{ current.code }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
            <el-descriptions-item label="初始状态">
              <el-select v-model="initialState" size="small" style="width: 140px">
                <el-option v-for="state in states" :key="state.code" :label="state.name" :value="state.code" />
              </el-select>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">状态</el-divider>
          <el-table :data="states" border size="small">
            <el-table-column label="状态编码" width="180">
              <template #default="{ row }"><el-input v-model="row.code" size="small" /></template>
            </el-table-column>
            <el-table-column label="状态名称">
              <template #default="{ row }"><el-input v-model="row.name" size="small" /></template>
            </el-table-column>
            <el-table-column label="允许发起任意流程" width="170">
              <template #default="{ row }"><el-switch v-model="row.allFlow" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button link type="danger" @click="states.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">状态迁移</el-divider>
          <el-table :data="transitions" border size="small">
            <el-table-column label="起点" width="170">
              <template #default="{ row }">
                <el-select v-model="row.from" size="small" filterable>
                  <el-option v-for="state in states" :key="state.code" :label="state.code" :value="state.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="终点" width="170">
              <template #default="{ row }">
                <el-select v-model="row.to" size="small" filterable>
                  <el-option v-for="state in states" :key="state.code" :label="state.code" :value="state.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作码" width="160">
              <template #default="{ row }"><el-input v-model="row.action" size="small" placeholder="如 submit" /></template>
            </el-table-column>
            <el-table-column label="显示名">
              <template #default="{ row }"><el-input v-model="row.label" size="small" placeholder="如 提交审批" /></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button link type="danger" @click="transitions.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-alert class="tip" type="info" :closable="false"
                    title="状态字段只允许通过状态机迁移写入；保存时后端会校验状态唯一、初始态存在、迁移端点合法。" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { lifecycleApi, type LifecycleDefinition, type LifecycleState, type LifecycleTransition } from '@/api/system'

const lifecycles = ref<LifecycleDefinition[]>([])
const current = ref<LifecycleDefinition | null>(null)
const states = ref<LifecycleState[]>([])
const transitions = ref<LifecycleTransition[]>([])
const initialState = ref('')
const saving = ref(false)

async function load(): Promise<void> {
  lifecycles.value = await lifecycleApi.list()
  if (!current.value && lifecycles.value.length > 0) {
    await onSelect(lifecycles.value[0].code)
  }
}

async function onSelect(code: string): Promise<void> {
  const detail = await lifecycleApi.detail(code)
  current.value = detail
  states.value = detail.states.map((item) => ({ ...item }))
  transitions.value = detail.transitions.map((item) => ({ ...item }))
  initialState.value = detail.initial
}

function addState(): void {
  states.value.push({ code: '', name: '', allFlow: false })
}

function addTransition(): void {
  const first = states.value[0]?.code ?? ''
  transitions.value.push({ from: first, to: first, action: '', label: '' })
}

async function onSave(): Promise<void> {
  if (!current.value) {
    return
  }
  if (states.value.some((item) => !item.code.trim() || !item.name.trim())) {
    ElMessage.warning('状态的编码与名称不能为空')
    return
  }
  const definition = {
    code: current.value.code,
    name: current.value.name,
    initial: initialState.value,
    states: states.value,
    transitions: transitions.value
  }
  saving.value = true
  try {
    await lifecycleApi.save(current.value.code, JSON.stringify(definition))
    ElMessage.success('已保存')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.body {
  display: flex;
  gap: 16px;
}

.side {
  width: 240px;
  border-right: 1px solid #eef1f6;
  padding-right: 12px;
}

.side-title {
  font-weight: 600;
  color: #24435f;
  margin-bottom: 8px;
}

.life-menu {
  border-right: none;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content {
  flex: 1;
  min-width: 0;
}

.meta {
  margin-bottom: 8px;
}

.tip {
  margin-top: 12px;
}
</style>
