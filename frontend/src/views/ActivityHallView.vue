<template>
  <section class="activity-hall-page">
    <div class="activity-hall-head ds-card">
      <div>
        <span class="ds-eyebrow">Activity Hall</span>
        <h1>活动大厅</h1>
        <p>优先展示正在报名、即将开始和签到中的校园活动。</p>
      </div>
      <div class="hall-head-actions">
        <el-button :icon="MagicStick" :loading="recommending" @click="loadRecommendations">AI 为我推荐</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新活动</el-button>
      </div>
    </div>

    <div class="hall-overview-row">
      <article v-for="item in hallMetrics" :key="item.label" class="ds-card hall-metric-tile">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <div class="hall-filter-card ds-card">
      <div class="hall-filter-title">
        <div>
          <span class="ds-eyebrow">Filters</span>
          <h2>快速筛选</h2>
        </div>
        <div class="hall-filter-actions">
          <el-button type="primary" :icon="Search" :loading="loading" @click="searchActivities">筛选</el-button>
        </div>
      </div>

      <div class="hall-search-row">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" @clear="searchActivities" @keyup.enter="searchActivities" />
        <el-select v-model="filters.status" placeholder="活动状态" @change="searchActivities">
          <el-option v-for="item in hallStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>

      <div class="hall-filter-group">
        <span>活动范围</span>
        <div class="hall-campus-tabs" aria-label="活动范围筛选">
          <button
            v-for="item in quickCampuses"
            :key="item"
            class="filter-chip"
            :class="{ active: filters.scope === item }"
            type="button"
            @click="setCampus(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="hasRecommended" class="recommend-panel ds-card">
      <div class="hall-section-bar recommendation-title">
        <div>
          <span class="ds-eyebrow ai">AI Recommend</span>
          <h2>为你推荐</h2>
          <p>推荐结果来自真实活动数据，AI 只负责解释推荐理由。</p>
        </div>
        <el-button :icon="Refresh" :loading="recommending" text @click="loadRecommendations">重新生成</el-button>
      </div>
      <div v-if="recommendations.length > 0" class="recommend-grid">
        <article v-for="item in recommendations" :key="item.activityId" class="recommend-card">
          <div class="recommend-score">
            <strong>{{ item.recommendScore }}</strong>
            <span>推荐分</span>
          </div>
          <div class="recommend-content">
            <div class="recommend-card-head">
              <h3>{{ item.title }}</h3>
              <span class="ds-mini-tag">{{ item.activityCategory || '其他' }}</span>
            </div>
            <div class="recommend-meta">
              <span>
                <el-icon><Calendar /></el-icon>
                {{ formatDateTime(item.startTime) }}
              </span>
              <span>
                <el-icon><Location /></el-icon>
                {{ item.location || '线上活动' }}
              </span>
              <span>
                <el-icon><School /></el-icon>
                {{ item.campus || '全校区' }}
              </span>
            </div>
            <p class="recommend-reason">{{ item.aiReason || '该活动与你的校区、参与记录或当前活动状态较匹配。' }}</p>
            <div class="recommend-actions">
              <span class="ds-mini-tag">{{ activityModeText(item) }}</span>
              <span class="ds-mini-tag">报名 {{ item.registrationCount || 0 }} / {{ item.maxParticipants || '不限' }}</span>
              <RouterLink :to="`/activities/${item.activityId}`">
                <el-button type="primary" size="small" :icon="Tickets">查看详情</el-button>
              </RouterLink>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无合适推荐，先浏览活动大厅看看吧。" />
    </div>

    <div class="hall-section-bar">
      <div>
        <span class="ds-eyebrow">Activities</span>
        <h2>活动列表</h2>
      </div>
      <span>{{ pagination.total }} 个活动 · 按发布时间排序</span>
    </div>

    <div v-loading="loading" class="grid activity-grid hall-activity-grid">
      <ActivityCard v-for="activity in visibleActivities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty
      v-if="!loading && visibleActivities.length === 0"
      class="panel empty-wrap hall-empty"
      description="当前筛选条件下暂无活动，换个范围或状态试试。"
    />
    <div v-if="pagination.total > 0" class="hall-pagination ds-card">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Calendar, Location, MagicStick, Refresh, School, Search, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ActivityCard from '../components/ActivityCard.vue'
import { pageActivities } from '../api/activity'
import { listActivityRecommendations } from '../api/recommendation'
import {
  activityModeText,
  studentActivityStatusOptions
} from '../utils/options'

const filters = reactive({ keyword: '', scope: '全部', status: 'ALL' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const activities = ref([])
const recommendations = ref([])
const loading = ref(false)
const recommending = ref(false)
const hasRecommended = ref(false)
const quickCampuses = ['全部', '龙子湖校区', '文化路校区', '许昌校区', '全校区', '线上活动']
const hallStatusOptions = studentActivityStatusOptions.filter((item) => item.value !== 'CHECKIN')
let latestRequestId = 0

const visibleActivities = computed(() => activities.value)

const hallMetrics = computed(() => [
  { label: '正在报名', value: countByStatus('REGISTERING') },
  { label: '签到中', value: activities.value.filter((item) => item.canCheckin && !item.checkedIn).length },
  { label: '进行中', value: countByStatus('ONGOING') },
  { label: '已结束', value: countByStatus('ENDED') }
])

function countByStatus(status) {
  return activities.value.filter((item) => item.status === status).length
}

function setCampus(scope) {
  filters.scope = scope
  searchActivities()
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '时间待定'
}

async function loadRecommendations() {
  recommending.value = true
  hasRecommended.value = true
  try {
    recommendations.value = await listActivityRecommendations()
    if (recommendations.value.length > 0) {
      ElMessage.success('已生成活动推荐')
    }
  } catch (error) {
    recommendations.value = []
  } finally {
    recommending.value = false
  }
}

async function load() {
  return loadActivities()
}

function searchActivities() {
  pagination.pageNum = 1
  loadActivities()
}

function handlePageChange(page) {
  loadActivities({ pageNum: page })
}

function handlePageSizeChange(size) {
  pagination.pageNum = 1
  pagination.pageSize = size
  loadActivities()
}

function buildPageParams() {
  const params = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
  const keyword = filters.keyword.trim()
  if (keyword) {
    params.keyword = keyword
  }
  if (filters.status && filters.status !== 'ALL') {
    params.status = filters.status
  }
  if (filters.scope === '线上活动') {
    params.activityMode = 'online'
  } else if (filters.scope && filters.scope !== '全部') {
    params.campus = filters.scope
  }
  return params
}

async function loadActivities(options = {}) {
  const requestedPage = options.pageNum || pagination.pageNum
  const requestedSize = options.pageSize || pagination.pageSize
  pagination.pageNum = requestedPage
  pagination.pageSize = requestedSize
  const requestId = ++latestRequestId
  loading.value = true
  try {
    const data = await pageActivities(buildPageParams())
    if (requestId !== latestRequestId) {
      return
    }
    activities.value = data?.records ?? []
    pagination.total = Number(data?.total ?? 0)
    pagination.pageNum = Number(data?.page ?? requestedPage)
    pagination.pageSize = Number(data?.size ?? requestedSize)
  } catch (error) {
    if (requestId === latestRequestId) {
      activities.value = []
      pagination.total = 0
      ElMessage.error(error?.message || '活动加载失败，请稍后重试')
    }
  } finally {
    if (requestId === latestRequestId) {
      loading.value = false
    }
  }
}

onMounted(load)
</script>
