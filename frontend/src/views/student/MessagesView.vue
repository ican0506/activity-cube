<template>
  <section>
    <div class="campus-hero messages-hero">
      <div class="hero-copy">
        <span class="motto-badge">消息中心</span>
        <h1>我的通知</h1>
        <p>活动报名、签到提醒、反馈提醒和系统通知都会出现在这里。</p>
      </div>
      <div class="message-hero-count">
        <span>未读消息</span>
        <strong>{{ unreadCount }}</strong>
      </div>
    </div>

    <div class="panel message-toolbar">
      <div class="chip-row">
        <button
          v-for="item in typeOptions"
          :key="item.value"
          class="filter-chip"
          :class="{ active: filters.type === item.value }"
          @click="changeType(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="button-row">
        <el-select v-model="filters.readStatus" clearable placeholder="阅读状态" style="width: 140px" @change="search">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-button type="primary" plain @click="readAll">一键全部已读</el-button>
      </div>
    </div>

    <div class="message-list" v-loading="loading">
      <button v-for="item in messages" :key="item.id" class="message-card panel" @click="openMessage(item)">
        <span v-if="item.readStatus === 0" class="unread-dot"></span>
        <div class="message-card-main">
          <div class="message-card-head">
            <h3>{{ item.title }}</h3>
            <el-tag :type="noticeTagType(item.noticeType)">{{ noticeTypeText(item.noticeType) }}</el-tag>
          </div>
          <p>{{ item.summary || item.content }}</p>
          <div class="meta-row">
            <span>{{ item.activityTitle || '系统通知' }}</span>
            <span>{{ item.createdAt || '时间待定' }}</span>
            <span>{{ item.readStatus === 1 ? '已读' : '未读' }}</span>
          </div>
        </div>
      </button>
      <el-empty v-if="!loading && messages.length === 0" class="empty-wrap" description="暂无消息，新的活动通知会出现在这里。" />
    </div>

    <div class="pagination-row">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="pagination.total"
        :page-size="pagination.size"
        :current-page="pagination.page"
        @current-change="changePage"
      />
    </div>

    <el-dialog v-model="detailVisible" title="通知详情" width="560px">
      <div v-if="currentMessage" class="message-detail">
        <div class="message-detail-head">
          <el-tag :type="noticeTagType(currentMessage.noticeType)">{{ noticeTypeText(currentMessage.noticeType) }}</el-tag>
          <span>{{ currentMessage.createdAt }}</span>
        </div>
        <h2>{{ currentMessage.title }}</h2>
        <p class="message-detail-activity">{{ currentMessage.activityTitle || '系统通知' }}</p>
        <p class="message-detail-content">{{ currentMessage.content }}</p>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUnreadMessageCount, listMessages, markAllMessagesRead, markMessageRead } from '../../api/message'

const typeOptions = [
  { label: '全部', value: 'all' },
  { label: '活动通知', value: 'activity' },
  { label: '签到提醒', value: 'checkin_reminder' },
  { label: '反馈提醒', value: 'feedback_reminder' },
  { label: '系统通知', value: 'system' }
]

const filters = reactive({
  type: 'all',
  readStatus: ''
})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const messages = ref([])
const unreadCount = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const currentMessage = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await listMessages({
      type: filters.type === 'all' ? undefined : filters.type,
      readStatus: filters.readStatus === '' ? undefined : filters.readStatus,
      page: pagination.page,
      size: pagination.size
    })
    messages.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.page || pagination.page
    pagination.size = data.size || pagination.size
    unreadCount.value = await getUnreadMessageCount()
  } finally {
    loading.value = false
  }
}

function changeType(type) {
  filters.type = type
  search()
}

function search() {
  pagination.page = 1
  load()
}

function changePage(page) {
  pagination.page = page
  load()
}

async function openMessage(item) {
  currentMessage.value = item
  detailVisible.value = true
  if (item.readStatus === 0) {
    await markMessageRead(item.id)
    item.readStatus = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

async function readAll() {
  await markAllMessagesRead()
  ElMessage.success('已全部标记为已读')
  load()
}

function noticeTypeText(type) {
  return typeOptions.find((item) => item.value === type)?.label || '活动通知'
}

function noticeTagType(type) {
  if (type === 'system') return 'warning'
  if (type === 'checkin_reminder') return 'success'
  if (type === 'feedback_reminder') return 'info'
  return 'primary'
}

onMounted(load)
</script>

<style scoped>
.messages-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 210px;
  margin-bottom: 18px;
  padding: clamp(22px, 4vw, 38px);
  border-radius: 24px;
  color: #ffffff;
  box-shadow: 0 20px 60px rgba(11, 125, 59, 0.18);
}

.message-hero-count {
  min-width: 150px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.24);
}

.message-hero-count span,
.message-hero-count strong {
  display: block;
}

.message-hero-count strong {
  margin-top: 8px;
  font-size: 34px;
}

.message-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.message-list {
  display: grid;
  gap: 12px;
}

.message-card {
  position: relative;
  width: 100%;
  display: block;
  text-align: left;
  border-color: rgba(11, 125, 59, 0.12);
  cursor: pointer;
}

.message-card-main {
  display: grid;
  gap: 8px;
}

.message-card-head,
.message-detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.message-card h3,
.message-detail h2 {
  margin: 0;
}

.message-card p {
  margin: 0;
  color: var(--ac-muted);
  line-height: 1.7;
}

.unread-dot {
  position: absolute;
  left: 10px;
  top: 18px;
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--ac-secondary);
}

.message-card .message-card-main {
  padding-left: 12px;
}

.message-detail {
  display: grid;
  gap: 12px;
}

.message-detail-head,
.message-detail-activity {
  color: var(--ac-muted);
}

.message-detail-content {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.85;
  color: #38524a;
}

@media (max-width: 768px) {
  .messages-hero,
  .message-toolbar {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
