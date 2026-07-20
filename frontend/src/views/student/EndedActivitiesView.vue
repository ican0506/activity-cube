<template>
  <section class="ended-page">
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">历史活动</span>
        <h1>已结束活动</h1>
        <p>查看历史活动、反馈入口和已获得的活动成果。</p>
      </div>
      <RouterLink to="/profile">
        <el-button type="primary">查看我的活动成果</el-button>
      </RouterLink>
    </div>

    <div class="lite-section-bar">
      <div>
        <h2>历史活动列表</h2>
        <p>活动结束后，可从详情页提交反馈；奖励发放后会同步到个人中心。</p>
      </div>
      <span>{{ activities.length }} 个活动</span>
    </div>

    <div v-loading="loading" class="grid activity-grid hall-activity-grid">
      <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
    </div>
    <el-empty
      v-if="!loading && activities.length === 0"
      class="panel empty-wrap"
      description="暂无已结束活动，历史活动会展示在这里。"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ActivityCard from '../../components/ActivityCard.vue'
import { listActivities } from '../../api/activity'

const activities = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    activities.value = await listActivities({ status: 'ENDED' })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
