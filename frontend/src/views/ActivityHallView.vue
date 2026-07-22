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
        <el-button type="primary" :icon="Search" :loading="loading" @click="load">筛选</el-button>
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
import { Refresh, Search } from '@element-plus/icons-vue'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { activityScopeMatches, isStudentVisibleActivity, studentActivityStatus, studentActivityStatusOptions } from '../utils/options'

const filters = reactive({ keyword: '', scope: '全部', status: 'ALL' })
const activities = ref([])
const loading = ref(false)
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
