<template>
  <article class="activity-card">
    <div>
      <h3>{{ activity.title }}</h3>
      <div class="meta-row">
        <span>{{ activity.campus }}</span>
        <span>{{ activity.location }}</span>
      </div>
    </div>
    <div class="meta-row">
      <span>{{ activity.startTime }}</span>
      <span v-if="activity.maxParticipants">上限 {{ activity.maxParticipants }} 人</span>
    </div>
    <div class="meta-row">
      <el-tag size="small">{{ statusText(activity.status) }}</el-tag>
      <el-tag size="small" type="success" v-if="activity.allowCrossCampus">允许跨校区</el-tag>
      <el-tag size="small" type="warning" v-else>限制校区</el-tag>
    </div>
    <div class="toolbar">
      <RouterLink :to="`/activities/${activity.id}`">
        <el-button type="primary">查看详情</el-button>
      </RouterLink>
      <RouterLink :to="`/activities/${activity.id}/register`">
        <el-button>报名</el-button>
      </RouterLink>
    </div>
  </article>
</template>

<script setup>
import { statusText } from '../utils/options'

defineProps({
  activity: {
    type: Object,
    required: true
  }
})
</script>
