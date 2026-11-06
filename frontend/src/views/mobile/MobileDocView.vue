<template>
  <div class="m-page">
    <el-input
      v-model="keyword"
      placeholder="编码 / 名称"
      clearable
      @keyup.enter="load"
    >
      <template #append>
        <el-button @click="load">搜索</el-button>
      </template>
    </el-input>

    <div v-loading="loading" class="card-list">
      <div v-for="doc in docs" :key="doc.id" class="doc-card" @click="openDetail(doc)">
        <div class="doc-head">
          <span class="doc-code">{{ doc.code }}</span>
          <el-tag size="small" :type="doc.lifecycleState === 'Released' ? 'success' : 'info'">
            {{ DOC_STATE_LABEL[doc.lifecycleState] ?? doc.lifecycleState }}
          </el-tag>
        </div>
        <div class="doc-name">{{ doc.name }}</div>
        <div class="doc-foot">
          <span>{{ doc.revisionLabel || `v${doc.revision}.${doc.iteration}` }}</span>
          <span>{{ DOC_TYPE_LABEL[doc.docType] ?? doc.docType }}</span>
          <span>文件 {{ doc.fileCount }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && !docs.length" description="没有匹配的文档" :image-size="60" />
    </div>

    <div v-if="docs.length < total" class="load-more">
      <el-button link type="primary" :loading="loading" @click="loadMore">加载更多</el-button>
    </div>

    <el-drawer
      v-model="detailVisible"
      direction="btt"
      size="85%"
      :title="detail ? `${detail.code}` : '文档详情'"
      destroy-on-close
    >
      <div v-if="detailLoading" v-loading="true" class="detail-loading" />
      <div v-else-if="detail" class="detail-body">
        <div class="detail-name">{{ detail.name }}</div>
        <div class="detail-grid">
          <span>类型</span><span>{{ DOC_TYPE_LABEL[detail.docType] ?? detail.docType }}</span>
          <span>状态</span><span>{{ DOC_STATE_LABEL[detail.lifecycleState] ?? detail.lifecycleState }}</span>
          <span>版本</span><span>{{ detail.revisionLabel || `v${detail.revision}.${detail.iteration}` }}</span>
          <span>分类</span><span>{{ detail.className || '-' }}</span>
          <span>更新时间</span><span>{{ formatTime(detail.updatedAt) }}</span>
        </div>

        <div class="files-title">文件（{{ detail.files?.length ?? 0 }}）</div>
        <div v-for="file in detail.files" :key="file.linkId" class="file-row">
          <div class="file-main">
            <span class="file-name">{{ file.fileName }}</span>
            <span class="file-meta">
              {{ FILE_ROLE_LABEL[file.fileRole] ?? file.fileRole }} · {{ formatSize(file.size) }}
            </span>
          </div>
          <el-button size="small" type="primary" plain @click="download(file)">下载</el-button>
        </div>
        <el-empty v-if="!detail.files?.length" description="没有文件" :image-size="50" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DOC_STATE_LABEL,
  DOC_TYPE_LABEL,
  FILE_ROLE_LABEL,
  docApi,
  type DocDetail,
  type DocFile,
  type DocSummary
} from '@/api/doc'

const PAGE_SIZE = 20

const keyword = ref('')
const docs = ref<DocSummary[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<DocDetail | null>(null)

async function load(): Promise<void> {
  page.value = 1
  docs.value = []
  await fetchPage()
}

async function loadMore(): Promise<void> {
  page.value += 1
  await fetchPage()
}

async function fetchPage(): Promise<void> {
  loading.value = true
  try {
    const result = await docApi.list({
      page: page.value,
      size: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined
    })
    docs.value = page.value === 1 ? result.items : [...docs.value, ...result.items]
    total.value = result.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载文档失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(row: DocSummary): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await docApi.detail(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载文档详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function download(file: DocFile): Promise<void> {
  try {
    await docApi.download(file)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  }
}

function formatTime(value?: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatSize(size?: number | null): string {
  if (!size) {
    return '0 B'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<style scoped>
.m-page {
  padding-bottom: 8px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.doc-card {
  background: #fff;
  border: 1px solid #e5e8ef;
  border-radius: 8px;
  padding: 10px 12px;
}

.doc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.doc-code {
  font-size: 13px;
  font-weight: 600;
  color: #1f2d3d;
}

.doc-name {
  margin-top: 6px;
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.doc-foot {
  display: flex;
  gap: 10px;
  margin-top: 6px;
  font-size: 11px;
  color: #9aa4b2;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.detail-loading {
  height: 160px;
}

.detail-body {
  padding-bottom: 8px;
}

.detail-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 10px;
  word-break: break-all;
}

.detail-grid {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 6px 8px;
  font-size: 13px;
  color: #5c6b7a;
}

.detail-grid span:nth-child(odd) {
  color: #9aa4b2;
}

.files-title {
  margin: 14px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f3f8;
}

.file-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #9aa4b2;
}
</style>
