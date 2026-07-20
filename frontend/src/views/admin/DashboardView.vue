<template>
  <section>
    <div class="admin-intro">
      <div>
        <h1>数据总览</h1>
        <p>活动负责人查看平台运行概览，快速进入活动创建和管理。</p>
      </div>
      <RouterLink to="/admin/activities/create">
        <el-button type="primary">创建活动</el-button>
      </RouterLink>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>活动总数</span><strong>{{ dashboard.activityCount || 0 }}</strong></div>
      <div class="metric"><span>当前模块</span><strong>MVP</strong></div>
      <div class="metric"><span>数据库</span><strong>activity_cube</strong></div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>平台状态</h2>
          <p>{{ dashboard.message || '后端服务已连接，开始创建和管理校园活动。' }}</p>
        </div>
      </div>
      <div class="button-row">
        <RouterLink to="/activities"><el-button type="primary">查看活动大厅</el-button></RouterLink>
        <RouterLink to="/admin/activities"><el-button>进入活动管理</el-button></RouterLink>
      </div>
    </div>
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
