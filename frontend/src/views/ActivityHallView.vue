<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">活动大厅</h1>
        <p class="page-subtitle">按关键词、校区和状态筛选公开活动。</p>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索活动名称" style="width: 240px" @keyup.enter="load" />
      <el-select v-model="filters.campus" style="width: 160px">
        <el-option v-for="item in campuses" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.status" style="width: 160px">
        <el-option v-for="item in statuses" :key="item" :label="statusText(item)" :value="item" />
      </el-select>
      <el-button type="primary" @click="load">筛选</el-button>
    </div>

    <div class="grid activity-grid">
      <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import ActivityCard from '../components/ActivityCard.vue'
import { listActivities } from '../api/activity'
import { campuses, statuses, statusText } from '../utils/options'

const filters = reactive({ keyword: '', campus: '全部', status: '全部' })
const activities = ref([])

async function load() {
  activities.value = await listActivities(filters)
}

onMounted(load)
</script>
