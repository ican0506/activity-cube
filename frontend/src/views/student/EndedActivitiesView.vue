<template>
  <section>
    <div class="campus-hero">
      <div>
        <span class="motto-badge">历史活动</span>
        <h1>已结束活动</h1>
        <p>查看已经结束的校园活动。已报名的活动可以继续进入详情提交反馈，已签到且发放奖励后会同步到个人中心。</p>
      </div>
      <RouterLink to="/profile">
        <el-button class="hero-button">查看我的活动成果</el-button>
      </RouterLink>
    </div>

    <div v-loading="loading" class="grid activity-grid">
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
