<template>
  <section v-loading="loading">
    <div class="hero campus-hero">
      <div>
        <div class="motto-badge">扫码签到 · 有序参与</div>
        <h1>活动签到</h1>
        <p>签到入口仅用于活动现场。后端会校验报名记录、重复签到和签到时间。</p>
      </div>
      <div v-if="activity" class="hero-card">
        <span>{{ statusText(activity.status) }}</span>
        <strong>{{ activity.title }}</strong>
        <p>签到：{{ activity.checkinStartTime || activity.startTime }} 至 {{ activity.checkinEndTime || activity.endTime }}</p>
      </div>
    </div>

    <div class="panel form-narrow agri-card">
      <el-alert
        v-if="activity && !canCheckin(activity)"
        class="qr-tip"
        :title="checkinDisabledReason(activity)"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-if="activity && !isOnlineActivity(activity) && !checkinCode"
        class="qr-tip"
        title="线下活动需扫描现场签到二维码"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-if="checked"
        class="success-note"
        title="签到成功，欢迎参加本次活动。"
        type="success"
        :closable="false"
        show-icon
      />
      <el-result icon="success" title="扫码签到入口" sub-title="确认后将写入签到记录，重复签到会被后端拦截。">
        <template #extra>
          <el-button type="primary" :disabled="checked || (activity && (!canCheckin(activity) || (!isOnlineActivity(activity) && !checkinCode)))" :loading="submitting" @click="submit">确认签到</el-button>
          <RouterLink :to="`/activities/${route.params.id}`">
            <el-button>返回详情</el-button>
          </RouterLink>
        </template>
      </el-result>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { checkinActivity } from '../../api/checkin'
import { canCheckin, checkinDisabledReason, isOnlineActivity, statusText } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)
const checked = ref(false)
const checkinCode = computed(() => String(route.query.code || ''))

async function submit() {
  if (activity.value && !canCheckin(activity.value)) {
    ElMessage.warning(checkinDisabledReason(activity.value))
    return
  }
  submitting.value = true
  try {
    await checkinActivity(route.params.id, checkinCode.value)
    checked.value = true
    ElMessage.success('签到成功，欢迎参加本次活动。')
    window.setTimeout(() => router.push(`/activities/${route.params.id}`), 800)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const detail = await getActivity(route.params.id)
    activity.value = detail.activity
  } finally {
    loading.value = false
  }
})
</script>
