<template>
  <article class="activity-card agri-card">
    <div class="activity-cover">
      <img v-if="activity.coverUrl" :src="resolveFileUrl(activity.coverUrl)" :alt="activity.title" />
      <div v-else class="activity-cover-fallback">
        <span>{{ activityCategoryText(activity) }}</span>
      </div>
      <span class="activity-cover-badge">
        <el-icon><School /></el-icon>
        {{ activityCampusText(activity) }}
      </span>
    </div>

    <div class="activity-card-body">
      <div class="activity-card-top">
        <h3>{{ activity.title }}</h3>
        <el-tag size="small" :type="studentActivityStatusTagType(activity)" round>
          {{ studentActivityStatusText(activity) }}
        </el-tag>
      </div>

      <div class="activity-fact-list compact">
        <span>
          <el-icon><Calendar /></el-icon>
          {{ formatDateTime(activity.startTime) }}
        </span>
        <span>
          <el-icon><Location /></el-icon>
          {{ activityLocationText(activity) }}
        </span>
      </div>

      <div class="activity-card-tags">
        <span class="ds-mini-tag">{{ activityModeText(activity) }}</span>
        <span class="ds-mini-tag">{{ activityCategoryText(activity) }}</span>
        <span class="ds-mini-tag">报名 {{ registrationCountText(activity) }}</span>
        <span class="ds-mini-tag">{{ activity.allowCrossCampus ? '允许跨校区' : '限本校区' }}</span>
      </div>

      <p v-if="activity.rewardEnabled" class="activity-reward-line">
        {{ rewardSummary(activity) }}
      </p>

      <div class="activity-actions">
        <RouterLink :to="`/activities/${activity.id}`">
          <el-button :icon="Tickets">详情</el-button>
        </RouterLink>
        <RouterLink v-if="action.to !== 'none'" :to="actionTarget">
          <el-button :type="action.type" :icon="Right" :disabled="action.disabled">{{ action.label }}</el-button>
        </RouterLink>
        <el-tooltip v-else :content="action.label" placement="top">
          <span>
            <el-button :type="action.type" :icon="Right" disabled>{{ action.label }}</el-button>
          </span>
        </el-tooltip>
      </div>
    </div>
  </article>
</template>

<script setup>
import { Calendar, Location, Right, School, Tickets } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { resolveFileUrl } from '../api/file'
import {
  activityCampusText,
  activityCategoryText,
  activityLocationText,
  activityModeText,
  registrationCountText,
  rewardSummary,
  studentActivityAction,
  studentActivityStatusTagType,
  studentActivityStatusText
} from '../utils/options'

const props = defineProps({
  activity: {
    type: Object,
    required: true
  }
})

const action = computed(() => studentActivityAction(props.activity))
const actionTarget = computed(() => {
  const id = props.activity.id
  const map = {
    register: `/activities/${id}/register`,
    myActivities: '/my-activities',
    checkin: `/activities/${id}/checkin`,
    scan: '/scan',
    feedback: `/activities/${id}/feedback`,
    detail: `/activities/${id}`
  }
  return map[action.value.to] || `/activities/${id}`
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '时间待定'
}
</script>
