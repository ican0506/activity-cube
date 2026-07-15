<template>
  <section>
    <div class="hero">
      <div>
        <h1>活动大厅</h1>
        <p>发现社团招新、讲座沙龙、志愿服务和线上活动。按校区和状态快速筛选，找到适合自己的校园活动。</p>
        <div class="hero-actions">
          <el-button class="hero-button" :icon="Search" @click="load">刷新活动</el-button>
        </div>
      </div>
      <div class="hero-card">
        <span>当前可浏览活动</span>
        <strong>{{ activities.length }}</strong>
        <p>覆盖全校区、三大校区和线上活动</p>
      </div>
    </div>

    <div class="filter-panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" style="width: 260px" @keyup.enter="load" />
        <el-select v-model="filters.campus" style="width: 170px">
        <el-option v-for="item in campuses" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="filters.status" style="width: 170px">
        <el-option v-for="item in statuses" :key="item" :label="statusText(item)" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">筛选</el-button>
      </div>
      <p class="page-subtitle">支持活动名称、活动校区和活动状态组合筛选。</p>
    </div>

    <div v-loading="loading" class="grid activity-grid">
      <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty v-if="!loading && activities.length === 0" class="panel empty-wrap" description="暂无活动，换个筛选条件试试" />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { campuses, statuses, statusText } from '../utils/options'

const filters = reactive({ keyword: '', campus: '全部', status: '全部' })
const activities = ref([])
const loading = ref(false)

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
