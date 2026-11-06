<template>
  <div class="home">
    <el-card shadow="never" class="welcome">
      <div class="welcome-title">欢迎，{{ userStore.displayName }}</div>
      <div class="welcome-sub">
        当前角色：{{ userStore.user?.roles.join('、') || '-' }} · 已授权功能点 {{ userStore.permissions.size }} 项
      </div>
    </el-card>

    <el-row :gutter="16" class="stats">
      <el-col v-for="item in stats" :key="item.label" :span="6">
        <el-card shadow="hover" class="stat">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="phase">
      <template #header>
        <span>实施阶段</span>
        <StatusDot tone="ok" label="P0 已完成" class="phase-tag" />
      </template>
      <el-steps :active="1" align-center finish-status="success">
        <el-step v-for="step in phases" :key="step.title" :title="step.title" :description="step.desc" />
      </el-steps>
    </el-card>

    <el-card shadow="never" class="plan">
      <template #header>下一步（P1 平台内核）</template>
      <ul class="plan-list">
        <li>组织架构：公司 / 部门 / 组 / 用户 / 角色，树形维护与导入</li>
        <li>功能权限与数据隔离：权限点注册、角色授权、组织子树与隔离组过滤</li>
        <li>规则权限：嵌套条件树设计器 + 试算器 + 审计</li>
        <li>元数据引擎：对象类型 / 字段 / 枚举 / 表单定义、发布、版本快照</li>
        <li>元数据驱动表单：31 种值类型控件映射与联动校验</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import StatusDot from '@/components/StatusDot.vue'

const userStore = useUserStore()

const stats = [
  { label: '阶段进度', value: '1 / 11' },
  { label: '领域模块', value: '11' },
  { label: '数据表（基线）', value: '26' },
  { label: '接口（已开放）', value: '3' }
]

const phases = [
  { title: 'P0 基线', desc: '骨架/鉴权/迁移/CI' },
  { title: 'P1 平台内核', desc: '组织/权限/元数据' },
  { title: 'P2 图文档', desc: '文件仓/检入检出' },
  { title: 'P3 零部件 BOM', desc: '多视图/基线' },
  { title: 'P4 流程', desc: '审批/生命周期' },
  { title: 'P5+ 业务域', desc: '变更/工艺/项目…' }
]
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--plm-text-primary);
}

.welcome-sub {
  margin-top: 6px;
  color: var(--plm-text-secondary);
  font-size: 12px;
}

.stat {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--plm-primary);
  line-height: 1.2;
}

.stat-label {
  margin-top: 4px;
  color: var(--plm-text-secondary);
  font-size: 12px;
}

.phase-tag {
  margin-left: 8px;
}

.plan-list {
  margin: 0;
  padding-left: 18px;
  color: var(--plm-text-regular);
  font-size: 12px;
  line-height: 1.9;
}
</style>
