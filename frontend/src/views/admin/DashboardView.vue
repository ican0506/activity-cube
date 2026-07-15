<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">后台首页</h1>
        <p class="page-subtitle">活动负责人查看 MVP 数据概览。</p>
      </div>
      <RouterLink to="/admin/activities/create">
        <el-button type="primary">创建活动</el-button>
      </RouterLink>
    </div>
    <div class="metric-row">
      <div class="metric"><span>活动总数</span><strong>{{ dashboard.activityCount || 0 }}</strong></div>
      <div class="metric"><span>当前模块</span><strong>MVP</strong></div>
      <div class="metric"><span>数据库</span><strong>activity_cube</strong></div>
    </div>
    <div class="panel">{{ dashboard.message }}</div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDashboard } from '../../api/stat'

const dashboard = ref({})
onMounted(async () => {
  dashboard.value = await getDashboard()
})
</script>
