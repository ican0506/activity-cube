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
        <el-tag size="small" :type="studentActivityStatusTagType(activity)">
          {{ studentActivityStatusText(activity) }}
        </el-tag>
      </div>

      <div class="activity-fact-list">
        <span>
          <el-icon><Calendar /></el-icon>
          {{ activity.startTime || '时间待定' }}
        </span>
        <span>
          <el-icon><Location /></el-icon>
          {{ activityLocationText(activity) }}
        </span>
        <span>
          <el-icon><School /></el-icon>
          {{ activityCampusText(activity) }}
        </span>
      </div>

      <div class="activity-card-tags">
        <span class="wheat-badge">{{ activityModeText(activity) }}</span>
        <span class="wheat-badge">{{ activityCategoryText(activity) }}</span>
      </div>
      <p v-if="activity.rewardEnabled" class="page-subtitle">活动奖励：{{ rewardSummary(activity) }}</p>

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
import { Calendar, Location, Right, School, Tickets } from '@element-plus/icons-vue'
import { resolveFileUrl } from '../api/file'
import {
  activityCampusText,
  activityCategoryText,
  activityLocationText,
  activityModeText,
  canRegister,
  registerDisabledReason,
  rewardSummary,
  studentActivityStatusTagType,
  studentActivityStatusText
} from '../utils/options'

defineProps({
  activity: {
    type: Object,
    required: true
  }
})
</script>
