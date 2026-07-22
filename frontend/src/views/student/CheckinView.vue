<template>
  <section v-loading="loading">
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动签到</span>
        <h1>活动签到</h1>
        <p>确认活动信息后再完成签到。</p>
      </div>
      <div v-if="activity" class="lite-summary-card">
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
        v-if="activity && requiresQrCode && !checkinCode"
        class="qr-tip"
        title="该活动需现场扫码签到"
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
      <div v-if="activity" class="info-list scan-checkin-info">
        <div class="info-card"><span>活动名称</span><strong>{{ activity.title }}</strong></div>
        <div class="info-card"><span>校区</span><strong>{{ activity.campus }}</strong></div>
        <div class="info-card"><span>地点</span><strong>{{ activity.location || '地点待定' }}</strong></div>
        <div class="info-card"><span>签到时间</span><strong>{{ activity.checkinStartTime || activity.startTime }} 至 {{ activity.checkinEndTime || activity.endTime }}</strong></div>
      </div>
      <el-result icon="success" title="签到确认" sub-title="确认无误后点击按钮，系统才会写入签到记录。">
        <template #extra>
          <el-button type="primary" :disabled="checked || submitDisabled" :loading="submitting" @click="submit">确认签到</el-button>
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
import { canCheckin, checkinDisabledReason, normalizedCheckinMode, statusText } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)
const checked = ref(false)
const checkinCode = computed(() => String(route.query.code || ''))
const requiresQrCode = computed(() => normalizedCheckinMode(activity.value) === 'qr')
const submitDisabled = computed(() => {
  if (!activity.value) return true
  if (!canCheckin(activity.value)) return true
  return requiresQrCode.value && !checkinCode.value
})

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
