<template>
  <section class="activity-hall-page">
    <div class="activity-hall-head">
      <div>
        <span class="section-eyebrow">校园活动</span>
        <h1>活动大厅</h1>
        <p>发现校园活动，记录成长足迹。按校区、活动类型和状态快速找到适合自己的活动。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新活动</el-button>
    </div>

    <div class="hall-overview">
      <div class="hall-welcome-card">
        <div class="motto-badge light">明德自强 求是力行</div>
        <h2>活动魔方</h2>
        <p>服务龙子湖、文化路、许昌三校区，把报名、签到、反馈和活动成果沉淀到同一个轻量平台。</p>
      </div>
      <div class="hall-stat-list">
        <div class="hall-stat-card">
          <span>当前可参与</span>
          <strong>{{ visibleActivities.length }}</strong>
        </div>
        <div class="hall-stat-card">
          <span>报名中</span>
          <strong>{{ registeringCount }}</strong>
        </div>
        <div class="hall-stat-card">
          <span>进行中</span>
          <strong>{{ ongoingCount }}</strong>
        </div>
      </div>
    </div>

    <div class="hall-filter-card">
      <div class="hall-filter-title">
        <div>
          <h2>查找活动</h2>
          <p>支持活动名称、校区和状态组合筛选。</p>
        </div>
        <el-button type="primary" :icon="Search" :loading="loading" @click="load">筛选</el-button>
      </div>
      <div class="hall-search-row">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" @keyup.enter="load" />
        <el-select v-model="filters.status">
          <el-option v-for="item in statuses" :key="item" :label="statusText(item)" :value="item" />
        </el-select>
      </div>
      <div class="hall-campus-tabs" aria-label="校区筛选">
        <button
          v-for="item in quickCampuses"
          :key="item"
          class="filter-chip"
          :class="{ active: filters.campus === item }"
          type="button"
          @click="setCampus(item)"
        >
          {{ item }}
        </button>
      </div>
      <div class="hall-category-row" aria-label="活动类型">
        <span v-for="item in activityCategories" :key="item" class="category-chip">{{ item }}</span>
      </div>
    </div>

    <div class="hall-section-bar">
      <div>
        <h2>推荐活动</h2>
        <p>优先展示当前可浏览的校园活动。</p>
      </div>
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
import { Refresh, Search } from '@element-plus/icons-vue'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { activityCategories, statuses, statusText } from '../utils/options'

const filters = reactive({ keyword: '', campus: '全部', status: '全部' })
const activities = ref([])
const loading = ref(false)
const quickCampuses = ['全部', '全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
const registeringCount = computed(() => activities.value.filter((item) => item.status === 'REGISTERING').length)
const ongoingCount = computed(() => activities.value.filter((item) => item.status === 'ONGOING').length)
const visibleActivities = computed(() => {
  if (filters.status === 'ENDED') return activities.value
  return activities.value.filter((item) => item.status !== 'ENDED')
})

function setCampus(campus) {
  filters.campus = campus
  load()
}

async function load() {
  loading.value = true
  try {
    activities.value = await listActivities(filters)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
