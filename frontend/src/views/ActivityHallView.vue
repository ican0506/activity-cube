<template>
  <section class="activity-hall-page">
    <div class="activity-hall-head">
      <div>
        <h1>活动大厅</h1>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新活动</el-button>
    </div>

    <div class="hall-filter-card">
      <div class="hall-filter-title">
        <h2>筛选活动</h2>
        <div class="hall-filter-actions">
          <el-button :icon="MagicStick" :loading="recommending" @click="loadRecommendations">为我推荐</el-button>
          <el-button type="primary" :icon="Search" :loading="loading" @click="load">筛选</el-button>
        </div>
      </div>
      <div class="hall-search-row">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" @keyup.enter="load" />
        <el-select v-model="filters.status">
          <el-option v-for="item in studentActivityStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
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

    <div v-if="hasRecommended" class="recommend-panel agri-card">
      <div class="hall-section-bar recommendation-title">
        <div>
          <h2>为你推荐</h2>
          <p>根据你的校区、专业和历史参与记录生成。</p>
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
              <span class="wheat-badge">{{ item.activityCategory || '其他' }}</span>
            </div>
            <div class="recommend-meta">
              <span>
                <el-icon><Calendar /></el-icon>
                {{ item.startTime || '时间待定' }}
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
            <p class="recommend-reason">{{ item.aiReason }}</p>
            <div class="recommend-actions">
              <span class="wheat-badge">{{ activityModeText(item) }}</span>
              <span class="wheat-badge">报名 {{ item.registrationCount || 0 }} / {{ item.maxParticipants || '不限' }}</span>
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
      <h2>活动列表</h2>
      <span>{{ visibleActivities.length }} 个活动</span>
    </div>

    <div v-loading="loading" class="grid activity-grid hall-activity-grid">
      <ActivityCard v-for="activity in visibleActivities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty
      v-if="!loading && visibleActivities.length === 0"
      class="panel empty-wrap hall-empty"
      description="当前校区暂无活动，去发起一场属于农大学子的校园活动吧。"
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
import { activityModeText, activityScopeMatches, isStudentVisibleActivity, studentActivityStatus, studentActivityStatusOptions } from '../utils/options'

const filters = reactive({ keyword: '', scope: '全部', status: 'ALL' })
const activities = ref([])
const recommendations = ref([])
const loading = ref(false)
const recommending = ref(false)
const hasRecommended = ref(false)
const quickCampuses = ['全部', '龙子湖校区', '文化路校区', '许昌校区', '全校区', '线上活动']
const visibleActivities = computed(() => {
  const rows = activities.value
    .filter(isStudentVisibleActivity)
    .filter((item) => activityScopeMatches(item, filters.scope))
  if (filters.status === 'ALL') return rows
  return rows.filter((item) => studentActivityStatus(item) === filters.status)
})

function setCampus(scope) {
  filters.scope = scope
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
    activities.value = await listActivities({
      keyword: filters.keyword
    })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
