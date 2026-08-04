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
          <el-button type="primary" :icon="Search" :loading="loading" @click="load">筛选</el-button>
        </div>
      </div>

      <div class="hall-search-row">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称、地点或校区" @keyup.enter="load" />
        <el-select v-model="filters.status" placeholder="活动状态">
          <el-option v-for="item in studentActivityStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.category" placeholder="活动分类">
          <el-option label="全部分类" value="ALL" />
          <el-option v-for="item in activityCategories" :key="item" :label="item" :value="item" />
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
      <span>{{ visibleActivities.length }} 个活动 · 按报名中、即将开始、进行中、已结束排序</span>
    </div>

    <div v-loading="loading" class="grid activity-grid hall-activity-grid">
      <ActivityCard v-for="activity in visibleActivities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty
      v-if="!loading && visibleActivities.length === 0"
      class="panel empty-wrap hall-empty"
      description="当前筛选条件下暂无活动，换个范围或状态试试。"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Calendar, Location, MagicStick, Refresh, School, Search, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { listActivityRecommendations } from '../api/recommendation'
import {
  activityCategories,
  activityModeText,
  activityScopeMatches,
  isStudentVisibleActivity,
  studentActivityStatus,
  studentActivityStatusOptions
} from '../utils/options'

const filters = reactive({ keyword: '', scope: '全部', status: 'ALL', category: 'ALL' })
const activities = ref([])
const recommendations = ref([])
const loading = ref(false)
const recommending = ref(false)
const hasRecommended = ref(false)
const quickCampuses = ['全部', '龙子湖校区', '文化路校区', '许昌校区', '全校区', '线上活动']

const visibleActivities = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return activities.value
    .filter(isStudentVisibleActivity)
    .filter((item) => activityScopeMatches(item, filters.scope))
    .filter((item) => filters.status === 'ALL' || studentActivityStatus(item) === filters.status)
    .filter((item) => filters.category === 'ALL' || item.activityCategory === filters.category)
    .filter((item) => {
      if (!keyword) return true
      return [item.title, item.location, item.campus, item.activityCategory]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(keyword))
    })
    .slice()
    .sort(activitySort)
})

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
}

function activitySort(a, b) {
  return statusWeight(a) - statusWeight(b)
    || dateWeight(a.startTime) - dateWeight(b.startTime)
}

function statusWeight(activity) {
  if (activity.status === 'REGISTERING') return 1
  if (activity.status === 'WAITING_START' || activity.status === 'NOT_STARTED') return 2
  if (activity.canCheckin && !activity.checkedIn) return 3
  if (activity.status === 'ONGOING') return 4
  if (activity.status === 'ENDED') return 5
  return 9
}

function dateWeight(value) {
  const date = value ? new Date(value) : null
  return date && !Number.isNaN(date.getTime()) ? date.getTime() : Number.MAX_SAFE_INTEGER
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
  loading.value = true
  try {
    activities.value = await listActivities()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
