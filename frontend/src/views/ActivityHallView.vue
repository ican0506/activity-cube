<template>
  <section>
    <div class="hero campus-hero">
      <div>
        <div class="motto-badge">明德自强 求是力行</div>
        <h1>活动魔方</h1>
        <p>让校园活动组织更轻松。服务龙子湖、文化路、许昌三校区，覆盖活动创建、报名收集、扫码签到、抽签分组、反馈统计等全流程。</p>
        <div class="campus-strip">
          <span class="campus-pill">龙子湖校区</span>
          <span class="campus-pill">文化路校区</span>
          <span class="campus-pill">许昌校区</span>
          <span class="campus-pill">知农爱农 · 强农兴农</span>
        </div>
        <div class="hero-actions">
          <el-button class="hero-button" :icon="Refresh" @click="load">刷新活动</el-button>
          <RouterLink to="/scan">
            <el-button class="hero-button" :icon="Camera">扫一扫</el-button>
          </RouterLink>
        </div>
      </div>
      <div class="hero-card">
        <span>当前可浏览活动</span>
        <strong>{{ activities.length }}</strong>
        <p>连接每一次校园参与，让活动信息更清晰。</p>
        <div class="hero-stat-grid">
          <div class="hero-stat"><b>{{ registeringCount }}</b><small>报名中</small></div>
          <div class="hero-stat"><b>{{ ongoingCount }}</b><small>进行中</small></div>
        </div>
      </div>
    </div>

    <div class="filter-panel agri-card">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" style="width: 260px" @keyup.enter="load" />
        <el-select v-model="filters.status" style="width: 170px">
          <el-option v-for="item in statuses" :key="item" :label="statusText(item)" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load">筛选</el-button>
      </div>
      <div class="chip-row">
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
      <p class="page-subtitle">按校区、状态和关键词快速定位活动，适合在校园微服务平台或微信公众号 H5 中浏览。</p>
    </div>

    <div v-loading="loading" class="grid activity-grid">
      <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty
      v-if="!loading && activities.length === 0"
      class="panel empty-wrap"
      description="当前校区暂无活动，去发起一场属于农大学子的校园活动吧。"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Camera, Refresh, Search } from '@element-plus/icons-vue'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { statuses, statusText } from '../utils/options'

const filters = reactive({ keyword: '', campus: '全部', status: '全部' })
const activities = ref([])
const loading = ref(false)
const quickCampuses = ['全部', '全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
const registeringCount = computed(() => activities.value.filter((item) => item.status === 'REGISTERING').length)
const ongoingCount = computed(() => activities.value.filter((item) => item.status === 'ONGOING').length)

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
