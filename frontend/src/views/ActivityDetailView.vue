<template>
  <section v-loading="loading">
    <div v-if="detail" class="activity-detail-page">
      <div class="lite-page-head detail-title-head">
        <div>
          <div class="detail-title-line">
            <h1>{{ detail.activity.title }}</h1>
            <el-tag :type="statusTagType(detail.activity.status)">
              {{ statusText(detail.activity.status) }}
            </el-tag>
          </div>
          <div class="detail-top-meta">
            <span>
              <el-icon><Calendar /></el-icon>
              {{ detail.activity.startTime || '时间待定' }}
            </span>
            <span>
              <el-icon><Location /></el-icon>
              {{ detail.activity.campus }} · {{ detail.activity.location || '地点待定' }}
            </span>
          </div>
        </div>
        <div class="toolbar detail-actions">
          <RouterLink v-if="canRegister(detail.activity)" :to="`/activities/${id}/register`">
            <el-button type="primary" :icon="EditPen">立即报名</el-button>
          </RouterLink>
          <el-tooltip v-else :content="registerDisabledReason(detail.activity)" placement="top">
            <span><el-button type="primary" :icon="EditPen" disabled>立即报名</el-button></span>
          </el-tooltip>

          <RouterLink v-if="isOnlineActivity(detail.activity) && canCheckin(detail.activity)" :to="`/activities/${id}/checkin`">
            <el-button :icon="Checked">去签到</el-button>
          </RouterLink>
          <el-tooltip v-else-if="isOnlineActivity(detail.activity)" :content="checkinDisabledReason(detail.activity)" placement="top">
            <span><el-button :icon="Checked" disabled>去签到</el-button></span>
          </el-tooltip>
          <span v-else class="offline-checkin-tip">线下扫码签到</span>

          <RouterLink v-if="canFeedback(detail.activity)" :to="`/activities/${id}/feedback`">
            <el-button :icon="ChatDotRound">活动反馈</el-button>
          </RouterLink>
          <el-tooltip v-else :content="feedbackDisabledReason(detail.activity)" placement="top">
            <span><el-button :icon="ChatDotRound" disabled>活动反馈</el-button></span>
          </el-tooltip>

          <el-button @click="openQrcodeDialog">报名二维码</el-button>
        </div>
      </div>

      <div class="info-stack">
        <div class="metric-row">
          <div class="metric metric-accent"><span>报名人数</span><strong>{{ detail.registrationCount }}</strong></div>
          <div class="metric metric-accent"><span>签到人数</span><strong>{{ detail.checkinCount }}</strong></div>
          <div class="metric"><span>人数上限</span><strong>{{ detail.activity.maxParticipants || '不限' }}</strong></div>
        </div>

        <div class="panel agri-card">
          <div class="section-title">
            <div>
              <h2>活动安排</h2>
            </div>
            <span class="wheat-badge">活动安排</span>
          </div>
          <div class="info-list">
            <div class="info-card"><span>报名时间</span><strong>{{ detail.activity.registerStartTime }} 至 {{ detail.activity.registerEndTime }}</strong></div>
            <div class="info-card"><span>签到时间</span><strong>{{ detail.activity.checkinStartTime || detail.activity.startTime }} 至 {{ detail.activity.checkinEndTime || detail.activity.endTime }}</strong></div>
            <div class="info-card"><span>活动时间</span><strong>{{ detail.activity.startTime }} 至 {{ detail.activity.endTime }}</strong></div>
            <div class="info-card"><span>活动校区</span><strong>{{ detail.activity.campus }}</strong></div>
            <div class="info-card"><span>活动形式</span><strong>{{ activityModeText(detail.activity) }}</strong></div>
            <div class="info-card"><span>活动类型</span><strong>{{ activityCategoryText(detail.activity) }}</strong></div>
            <div class="info-card"><span>活动奖励</span><strong>{{ rewardSummary(detail.activity) }}</strong></div>
            <div class="info-card"><span>活动地点</span><strong>{{ detail.activity.location || '地点待定' }}</strong></div>
            <div class="info-card"><span>跨校区报名</span><strong>{{ detail.activity.allowCrossCampus ? '允许' : '不允许' }}</strong></div>
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
import { Calendar, ChatDotRound, Checked, EditPen, Location } from '@element-plus/icons-vue'
import { getActivity } from '../api/activity'
import { resolveFileUrl } from '../api/file'
import { listActivityMedia } from '../api/media'
import { buildActivityQrLinks } from '../utils/qrLinks'
import {
  canCheckin,
  canFeedback,
  canRegister,
  checkinDisabledReason,
  feedbackDisabledReason,
  activityCategoryText,
  activityModeText,
  isOnlineActivity,
  registerDisabledReason,
  rewardSummary,
  statusTagType,
  statusText
} from '../utils/options'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const mediaList = ref([])
const registerQr = ref(null)
const loading = ref(false)
const qrcodeVisible = ref(false)
const registerUrl = computed(() => buildActivityQrLinks(location.origin, id).registerUrl)
const imageMedia = computed(() => mediaList.value.filter((item) => item.mediaType === 'image'))
const videoMedia = computed(() => mediaList.value.filter((item) => item.mediaType === 'video'))
const imagePreviewList = computed(() => imageMedia.value.map((item) => resolveFileUrl(item.url)))

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
