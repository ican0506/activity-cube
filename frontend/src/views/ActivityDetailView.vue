<template>
  <section v-loading="loading">
    <div v-if="detail" class="activity-detail-page">
      <div class="detail-title-panel panel">
        <div>
          <div class="detail-title-line">
            <h1>{{ activity.title }}</h1>
            <el-tag :type="statusTagType(activity.status)">
              {{ activityDisplayStatusText(activity) }}
            </el-tag>
          </div>
          <div class="detail-title-tags">
            <span class="wheat-badge">{{ activityCampusText(activity) }}</span>
            <span class="wheat-badge">{{ activityModeText(activity) }}</span>
            <span class="wheat-badge">{{ activityCategoryText(activity) }}</span>
          </div>
        </div>
        <div class="toolbar detail-actions">
          <RouterLink v-if="canRegister(activity)" :to="`/activities/${id}/register`">
            <el-button type="primary" :icon="EditPen">立即报名</el-button>
          </RouterLink>
          <el-tooltip v-else :content="registerDisabledReason(activity)" placement="top">
            <span><el-button type="primary" :icon="EditPen" disabled>立即报名</el-button></span>
          </el-tooltip>

          <el-button v-if="activity.checkedIn" :icon="Checked" type="success" disabled>已签到</el-button>
          <RouterLink v-else-if="canOnlineCheckin(activity)" :to="`/activities/${id}/checkin`">
            <el-button :icon="Checked">线上签到</el-button>
          </RouterLink>
          <el-tooltip v-else-if="supportsOnline" :content="checkinDisabledReason(activity)" placement="top">
            <span><el-button :icon="Checked" disabled>线上签到</el-button></span>
          </el-tooltip>
          <RouterLink v-if="!activity.checkedIn && canQrCheckin(activity) && isStudent" to="/scan">
            <el-button :icon="Camera">扫码签到</el-button>
          </RouterLink>
          <el-tooltip v-else-if="supportsQr && isStudent && !activity.checkedIn" content="该活动需现场扫码签到" placement="top">
            <span><el-button :icon="Camera" disabled>扫码签到</el-button></span>
          </el-tooltip>
          <RouterLink v-if="!isStudent" :to="`/admin/activities/${id}/qrcodes`">
            <el-button :icon="Camera">二维码管理</el-button>
          </RouterLink>

          <RouterLink v-if="canFeedback(activity)" :to="`/activities/${id}/feedback`">
            <el-button :icon="ChatDotRound">反馈建议</el-button>
          </RouterLink>
          <el-tooltip v-else :content="feedbackDisabledReason(activity)" placement="top">
            <span><el-button :icon="ChatDotRound" disabled>反馈建议</el-button></span>
          </el-tooltip>

          <el-button @click="openQrcodeDialog">报名二维码</el-button>
        </div>
      </div>

      <div class="info-stack">
        <div class="detail-info-grid">
          <div class="panel detail-info-card">
            <div class="section-title compact">
              <h2>活动信息</h2>
            </div>
            <div class="detail-field-list">
              <div><span>活动地点</span><strong>{{ activityLocationText(activity) }}</strong></div>
              <div><span>活动时间</span><strong>{{ dateRangeText(activity.startTime, activity.endTime) }}</strong></div>
              <div><span>报名人数</span><strong>{{ detail.registrationCount || 0 }}</strong></div>
              <div><span>最大人数</span><strong>{{ activity.maxParticipants || '不限' }}</strong></div>
            </div>
          </div>

          <div class="panel detail-info-card">
            <div class="section-title compact">
              <h2>报名信息</h2>
            </div>
            <div class="detail-field-list">
              <div><span>报名开始时间</span><strong>{{ formatDateMinute(activity.registerStartTime) }}</strong></div>
              <div><span>报名截止时间</span><strong>{{ formatDateMinute(activity.registerEndTime) }}</strong></div>
            </div>
          </div>

          <div class="panel detail-info-card">
            <div class="section-title compact">
              <h2>签到信息</h2>
              <el-tag size="small" :type="checkinTagType">{{ checkinStatusText(activity) }}</el-tag>
            </div>
            <div class="detail-field-list">
              <div><span>签到开始时间</span><strong>{{ formatDateMinute(activity.checkinStartTime || activity.startTime) }}</strong></div>
              <div><span>签到结束时间</span><strong>{{ formatDateMinute(activity.checkinEndTime || activity.endTime) }}</strong></div>
              <div><span>签到方式</span><strong>{{ signMethodText }}</strong></div>
            </div>
          </div>

          <div class="panel detail-info-card">
            <div class="section-title compact">
              <h2>奖励信息</h2>
            </div>
            <div class="detail-field-list">
              <div><span>奖励类型</span><strong>{{ activity.rewardEnabled ? activity.rewardType || '无' : '无奖励' }}</strong></div>
              <div><span>奖励数量</span><strong>{{ rewardAmountText }}</strong></div>
            </div>
          </div>
        </div>

        <div v-if="imageMedia.length" class="panel">
          <div class="section-title">
            <div>
              <h2>校园相册</h2>
            </div>
          </div>
          <div class="media-gallery campus-gallery">
            <el-image
              v-for="item in imageMedia"
              :key="item.id || item.url"
              :src="resolveFileUrl(item.url)"
              :preview-src-list="imagePreviewList"
              fit="cover"
              preview-teleported
            />
          </div>
        </div>

        <div v-if="videoMedia.length" class="panel">
          <div class="section-title">
            <div>
              <h2>活动视频</h2>
            </div>
          </div>
          <div class="media-video-grid campus-gallery">
            <video v-for="item in videoMedia" :key="item.id || item.url" :src="resolveFileUrl(item.url)" controls />
          </div>
        </div>

      </div>

      <el-dialog v-model="qrcodeVisible" title="报名二维码" width="420px" @opened="renderRegisterQr">
        <div class="qr-dialog-content">
          <div class="qr-box">
            <canvas ref="registerQr"></canvas>
          </div>
          <p class="page-subtitle">{{ registerUrl }}</p>
        </div>
      </el-dialog>
    </div>
    <el-empty v-else-if="!loading" class="panel empty-wrap" description="未找到活动详情" />
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { Camera, ChatDotRound, Checked, EditPen } from '@element-plus/icons-vue'
import { getActivity } from '../api/activity'
import { resolveFileUrl } from '../api/file'
import { listActivityMedia } from '../api/media'
import { buildActivityQrLinks } from '../utils/qrLinks'
import { useUserStore } from '../stores/user'
import {
  canOnlineCheckin,
  canQrCheckin,
  canFeedback,
  canRegister,
  checkinDisabledReason,
  checkinStatusText,
  feedbackDisabledReason,
  activityCampusText,
  activityCategoryText,
  activityDisplayStatusText,
  activityLocationText,
  activityModeText,
  formatDateMinute,
  normalizedCheckinMode,
  registerDisabledReason,
  statusTagType
} from '../utils/options'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id
const detail = ref(null)
const mediaList = ref([])
const registerQr = ref(null)
const loading = ref(false)
const qrcodeVisible = ref(false)
const activity = computed(() => detail.value?.activity || {})
const registerUrl = computed(() => buildActivityQrLinks(location.origin, id).registerUrl)
const imageMedia = computed(() => mediaList.value.filter((item) => item.mediaType === 'image'))
const videoMedia = computed(() => mediaList.value.filter((item) => item.mediaType === 'video'))
const imagePreviewList = computed(() => imageMedia.value.map((item) => resolveFileUrl(item.url)))
const isStudent = computed(() => ['student', 'user'].includes(userStore.role))
const signMethodText = computed(() => checkinModeLabel(normalizedCheckinMode(activity.value)))
const supportsOnline = computed(() => ['online', 'both'].includes(normalizedCheckinMode(activity.value)))
const supportsQr = computed(() => ['qr', 'both'].includes(normalizedCheckinMode(activity.value)))
const rewardAmountText = computed(() => {
  const row = activity.value
  if (!row.rewardEnabled) return '-'
  if (row.rewardType === '课外学时') return `${row.rewardHours || 0} 课外学时`
  if (row.rewardType === '积分') return `${row.rewardPoints || 0} 积分`
  return row.rewardDescription || row.rewardType || '-'
})
const checkinTagType = computed(() => {
  const text = checkinStatusText(activity.value)
  if (text === '签到中') return 'success'
  if (text === '签到结束') return 'info'
  return 'warning'
})

function checkinModeLabel(mode) {
  if (mode === 'both') return '线上签到 + 现场扫码签到'
  if (mode === 'qr') return '现场扫码签到'
  return '线上签到'
}

function dateRangeText(start, end) {
  return `${formatDateMinute(start)} 至 ${formatDateMinute(end)}`
}

async function load() {
  loading.value = true
  try {
    const [activityDetail, media] = await Promise.all([
      getActivity(id),
      listActivityMedia(id)
    ])
    detail.value = activityDetail
    mediaList.value = media || []
  } finally {
    loading.value = false
  }
}

async function openQrcodeDialog() {
  qrcodeVisible.value = true
}

async function renderRegisterQr() {
  await nextTick()
  if (registerQr.value) {
    QRCode.toCanvas(registerQr.value, registerUrl.value, { width: 180 })
  }
}

onMounted(load)
</script>

<style scoped>
.detail-title-panel {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 18px;
}

.detail-title-line {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-title-line h1 {
  margin: 0;
  font-size: 30px;
  color: var(--ac-text);
}

.detail-title-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.detail-actions {
  justify-content: flex-end;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-info-card {
  display: grid;
  gap: 14px;
  min-height: 190px;
}

.section-title.compact {
  margin-bottom: 0;
  align-items: center;
}

.section-title.compact h2 {
  margin: 0;
  font-size: 18px;
}

.detail-field-list {
  display: grid;
  gap: 12px;
}

.detail-field-list div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(11, 125, 59, 0.08);
}

.detail-field-list div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.detail-field-list span {
  color: var(--ac-muted);
}

.detail-field-list strong {
  text-align: right;
  color: var(--ac-text);
  font-weight: 700;
}

@media (max-width: 900px) {
  .detail-title-panel {
    display: grid;
  }

  .detail-actions {
    justify-content: flex-start;
  }

  .detail-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
