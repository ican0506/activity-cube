<template>
  <article class="activity-card">
    <div class="activity-card-top">
      <div>
        <div class="activity-campus">{{ activity.campus }}</div>
        <h3>{{ activity.title }}</h3>
      </div>
      <el-tag size="small" :type="statusTagType(activity.status)">
        {{ statusText(activity.status) }}
      </el-tag>
    </div>
    <div class="meta-row">
      <span class="meta-item">
        <el-icon><Location /></el-icon>
        {{ activity.location || '地点待定' }}
      </span>
      <span class="meta-item">
        <el-icon><Calendar /></el-icon>
        {{ activity.startTime }}
      </span>
    </div>
    <div class="meta-row">
      <span class="meta-item">
        <el-icon><UserFilled /></el-icon>
        人数上限 {{ activity.maxParticipants || '不限' }}
      </span>
      <el-tag size="small" type="success" v-if="activity.allowCrossCampus">允许跨校区</el-tag>
      <el-tag size="small" type="warning" v-else>限制校区</el-tag>
    </div>
    <div class="activity-actions">
      <RouterLink :to="`/activities/${activity.id}`">
        <el-button type="primary" :icon="Tickets">查看详情</el-button>
      </RouterLink>
      <RouterLink :to="`/activities/${activity.id}/register`">
        <el-button :icon="Right">报名</el-button>
      </RouterLink>
    </div>
  </article>
</template>

<script setup>
import { Calendar, Location, Right, Tickets, UserFilled } from '@element-plus/icons-vue'
import { statusText } from '../utils/options'

defineProps({
  activity: {
    type: Object,
    required: true
  }
})

function statusTagType(status) {
  const map = {
    REGISTERING: 'success',
    ONGOING: 'warning',
    ENDED: 'info',
    CANCELLED: 'danger',
    DRAFT: ''
  }
  return map[status] || ''
}
</script>
