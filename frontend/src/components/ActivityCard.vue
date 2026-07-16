<template>
  <article class="activity-card agri-card">
    <div class="activity-cover">
      <img v-if="activity.coverUrl" :src="resolveFileUrl(activity.coverUrl)" :alt="activity.title" />
      <span class="activity-cover-badge">
        <el-icon><School /></el-icon>
        {{ activity.campus || '校园活动' }}
      </span>
    </div>

    <div class="activity-card-body">
      <div class="activity-card-top">
        <h3>{{ activity.title }}</h3>
        <el-tag size="small" :type="statusTagType(activity.status)">
          {{ statusText(activity.status) }}
        </el-tag>
      </div>

      <p class="activity-summary">{{ activity.description || '活动介绍正在完善，点击查看时间、地点和报名安排。' }}</p>

      <div class="meta-row">
        <span class="meta-item">
          <el-icon><Location /></el-icon>
          {{ activity.location || '地点待定' }}
        </span>
        <span class="meta-item">
          <el-icon><Calendar /></el-icon>
          {{ activity.startTime || '时间待定' }}
        </span>
      </div>

      <div class="meta-row">
        <span class="meta-item">
          <el-icon><UserFilled /></el-icon>
          报名上限 {{ activity.maxParticipants || '不限' }}
        </span>
        <span class="wheat-badge">{{ activityModeText(activity) }}</span>
      </div>

      <div class="activity-actions">
        <RouterLink :to="`/activities/${activity.id}`">
          <el-button type="primary" :icon="Tickets">查看详情</el-button>
        </RouterLink>
        <RouterLink v-if="canRegister(activity)" :to="`/activities/${activity.id}/register`">
          <el-button :icon="Right">立即报名</el-button>
        </RouterLink>
        <el-tooltip v-else :content="registerDisabledReason(activity)" placement="top">
          <span>
            <el-button :icon="Right" disabled>立即报名</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>
  </article>
</template>

<script setup>
import { Calendar, Location, Right, School, Tickets, UserFilled } from '@element-plus/icons-vue'
import { resolveFileUrl } from '../api/file'
import { activityModeText, canRegister, registerDisabledReason, statusTagType, statusText } from '../utils/options'

defineProps({
  activity: {
    type: Object,
    required: true
  }
})
</script>
