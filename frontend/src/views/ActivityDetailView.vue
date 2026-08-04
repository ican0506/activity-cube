<template>
  <section v-loading="loading">
    <div v-if="detail" class="activity-detail-page enterprise-detail">
      <div class="detail-hero ds-card">
        <div class="detail-hero-main">
          <div class="ds-eyebrow">Activity Detail</div>
          <div class="detail-title-line">
            <h1>{{ activity.title }}</h1>
            <el-tag :type="statusTagType(activity.status)">
              {{ activityDisplayStatusText(activity) }}
            </el-tag>
          </div>
          <div class="detail-top-meta">
            <span><School />{{ activityCampusText(activity) }}</span>
            <span><Connection />{{ activityModeText(activity) }}</span>
            <span><CollectionTag />{{ activityCategoryText(activity) }}</span>
            <span><Location />{{ activityLocationText(activity) }}</span>
          </div>
        </div>

        <div class="detail-action-panel">
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

          <RouterLink v-if="canFeedback(activity)" :to="`/activities/${id}/feedback`">
            <el-button :icon="ChatDotRound">反馈建议</el-button>
          </RouterLink>
          <el-tooltip v-else :content="feedbackDisabledReason(activity)" placement="top">
            <span><el-button :icon="ChatDotRound" disabled>反馈建议</el-button></span>
          </el-tooltip>

          <RouterLink v-if="isManager" :to="`/admin/activities/${id}/edit`">
            <el-button :icon="Edit">编辑活动</el-button>
          </RouterLink>
        </div>
      </div>

      <div class="detail-layout-v2">
        <main class="detail-main-column">
          <div class="detail-lifecycle ds-card">
            <div class="detail-section-head">
              <div>
                <div class="ds-eyebrow">Lifecycle</div>
                <h2>活动生命周期</h2>
              </div>
              <span>从活动创建到 AI 复盘的完整运营链路</span>
            </div>
            <div class="lifecycle-grid">
              <div
                v-for="step in lifecycleSteps"
                :key="step.key"
                class="lifecycle-card"
                :class="{ active: step.active, done: step.done }"
              >
                <div class="lifecycle-icon">
                  <component :is="step.icon" />
                </div>
                <div>
                  <strong>{{ step.title }}</strong>
                  <p>{{ step.desc }}</p>
                  <RouterLink v-if="step.to" :to="step.to">
                    <el-button link type="primary">{{ step.action }}</el-button>
                  </RouterLink>
                  <el-button v-else link disabled>{{ step.action }}</el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="detail-info-grid-v2">
            <div class="detail-info-card-v2 ds-card">
              <div class="detail-card-title">
                <Calendar />
                <h3>活动信息</h3>
              </div>
              <div class="detail-field-list">
                <div><span>活动时间</span><strong>{{ dateRangeText(activity.startTime, activity.endTime) }}</strong></div>
                <div><span>活动地点</span><strong>{{ activityLocationText(activity) }}</strong></div>
                <div><span>报名人数</span><strong>{{ registrationCountText }}</strong></div>
                <div><span>允许跨校区</span><strong>{{ activity.allowCrossCampus ? '允许' : '不允许' }}</strong></div>
              </div>
            </div>

            <div class="detail-info-card-v2 ds-card">
              <div class="detail-card-title">
                <Tickets />
                <h3>报名信息</h3>
              </div>
              <div class="detail-field-list">
                <div><span>报名开始</span><strong>{{ formatDateMinute(activity.registerStartTime) }}</strong></div>
                <div><span>报名截止</span><strong>{{ formatDateMinute(activity.registerEndTime) }}</strong></div>
                <div><span>报名状态</span><strong>{{ activityDisplayStatusText(activity) }}</strong></div>
              </div>
            </div>

            <div class="detail-info-card-v2 ds-card">
              <div class="detail-card-title">
                <Checked />
                <h3>签到信息</h3>
                <el-tag size="small" :type="checkinTagType">{{ checkinStatusText(activity) }}</el-tag>
              </div>
              <div class="detail-field-list">
                <div><span>签到开始</span><strong>{{ formatDateMinute(activity.checkinStartTime || activity.startTime) }}</strong></div>
                <div><span>签到结束</span><strong>{{ formatDateMinute(activity.checkinEndTime || activity.endTime) }}</strong></div>
                <div><span>签到方式</span><strong>{{ signMethodText }}</strong></div>
              </div>
            </div>

            <div class="detail-info-card-v2 ds-card">
              <div class="detail-card-title">
                <Medal />
                <h3>奖励信息</h3>
              </div>
              <div class="detail-field-list">
                <div><span>奖励类型</span><strong>{{ activity.rewardEnabled ? activity.rewardType || '无' : '无奖励' }}</strong></div>
                <div><span>奖励数量</span><strong>{{ rewardAmountText }}</strong></div>
                <div><span>奖励说明</span><strong>{{ activity.rewardDescription || '-' }}</strong></div>
              </div>
            </div>
          </div>

          <div class="detail-description ds-card">
            <div class="detail-section-head">
              <div>
                <div class="ds-eyebrow">Brief</div>
                <h2>活动介绍</h2>
              </div>
            </div>
            <p>{{ activity.description || '暂无活动介绍。' }}</p>
          </div>

          <div v-if="imageMedia.length || videoMedia.length" class="detail-media ds-card">
            <div class="detail-section-head">
              <div>
                <div class="ds-eyebrow">Media</div>
                <h2>活动素材</h2>
              </div>
            </div>
            <div v-if="imageMedia.length" class="media-gallery campus-gallery">
              <el-image
                v-for="item in imageMedia"
                :key="item.id || item.url"
                :src="resolveFileUrl(item.url)"
                :preview-src-list="imagePreviewList"
                fit="cover"
                preview-teleported
              />
            </div>
            <div v-if="videoMedia.length" class="media-video-grid campus-gallery">
              <video v-for="item in videoMedia" :key="item.id || item.url" :src="resolveFileUrl(item.url)" controls />
            </div>
          </div>
        </main>

        <aside class="detail-side-column">
          <div class="detail-cover-card ds-card">
            <img v-if="activity.coverUrl" :src="resolveFileUrl(activity.coverUrl)" alt="活动封面" />
            <div v-else class="detail-cover-fallback">
              <span>{{ activityCategoryText(activity) }}</span>
            </div>
          </div>

          <div class="detail-side-card ds-card">
            <div class="detail-section-head compact">
              <div>
                <div class="ds-eyebrow">Actions</div>
                <h2>快捷操作</h2>
              </div>
            </div>
            <div class="detail-side-actions">
              <el-button @click="openQrcodeDialog">报名二维码</el-button>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/qrcodes`">
                <el-button>二维码管理</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/registrations`">
                <el-button>报名名单</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/checkins`">
                <el-button>签到名单</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/notices`">
                <el-button>活动通知</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/tools`">
                <el-button>抽签分组</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/feedbacks`">
                <el-button>反馈统计</el-button>
              </RouterLink>
              <RouterLink v-if="isManager" :to="`/admin/activities/${id}/stats`">
                <el-button type="primary">统计与 AI 复盘</el-button>
              </RouterLink>
            </div>
          </div>

          <div class="detail-side-card ds-card">
            <div class="detail-section-head compact">
              <div>
                <div class="ds-eyebrow ai">AI Review</div>
                <h2>AI 复盘</h2>
              </div>
            </div>
            <p class="detail-side-text">活动结束后，负责人可在统计页基于报名、签到和反馈数据生成复盘报告。</p>
            <RouterLink v-if="isManager" :to="`/admin/activities/${id}/stats`">
              <el-button type="primary" plain>进入统计页</el-button>
            </RouterLink>
          </div>
        </aside>
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
import {
  Calendar,
  Camera,
  ChatDotRound,
  Checked,
  CollectionTag,
  Connection,
  DataAnalysis,
  Edit,
  EditPen,
  Location,
  Medal,
  School,
  Tickets
} from '@element-plus/icons-vue'
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
const isManager = computed(() => ['organizer', 'admin'].includes(userStore.role))
const signMethodText = computed(() => checkinModeLabel(normalizedCheckinMode(activity.value)))
const supportsOnline = computed(() => ['online', 'both'].includes(normalizedCheckinMode(activity.value)))
const supportsQr = computed(() => ['qr', 'both'].includes(normalizedCheckinMode(activity.value)))
const registrationCountText = computed(() => {
  const current = detail.value?.registrationCount || activity.value.registrationCount || 0
  return activity.value.maxParticipants ? `${current}/${activity.value.maxParticipants}` : `${current}/不限`
})
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
const lifecycleSteps = computed(() => {
  const row = activity.value
  const status = row.status
  return [
    {
      key: 'create',
      title: '创建',
      desc: isManager.value ? '维护活动基础信息和时间安排' : '查看活动基本信息',
      icon: Edit,
      done: Boolean(row.id),
      active: ['DRAFT', 'PENDING_REVIEW', 'REJECTED'].includes(status),
      to: isManager.value ? `/admin/activities/${id}/edit` : '',
      action: isManager.value ? '编辑活动' : '已创建'
    },
    {
      key: 'register',
      title: '报名',
      desc: `当前报名 ${registrationCountText.value}`,
      icon: Tickets,
      done: Boolean(row.registered) || Number(detail.value?.registrationCount || row.registrationCount || 0) > 0,
      active: status === 'REGISTERING',
      to: canRegister(row) ? `/activities/${id}/register` : '',
      action: row.registered ? '已报名' : '去报名'
    },
    {
      key: 'checkin',
      title: '签到',
      desc: `${checkinStatusText(row)} · ${signMethodText.value}`,
      icon: Checked,
      done: Boolean(row.checkedIn),
      active: checkinStatusText(row) === '签到中',
      to: canOnlineCheckin(row) ? `/activities/${id}/checkin` : '',
      action: row.checkedIn ? '已签到' : '去签到'
    },
    {
      key: 'feedback',
      title: '反馈',
      desc: '提交活动建议、问题反馈或活动评价',
      icon: ChatDotRound,
      done: Boolean(row.feedbackSubmitted),
      active: canFeedback(row),
      to: canFeedback(row) ? `/activities/${id}/feedback` : '',
      action: row.feedbackSubmitted ? '已反馈' : '反馈建议'
    },
    {
      key: 'stats',
      title: '统计',
      desc: '查看报名、签到、未签到和反馈数据',
      icon: DataAnalysis,
      done: ['ONGOING', 'ENDED'].includes(status),
      active: isManager.value,
      to: isManager.value ? `/admin/activities/${id}/stats` : '',
      action: isManager.value ? '查看统计' : '管理端查看'
    },
    {
      key: 'ai',
      title: 'AI 复盘',
      desc: status === 'ENDED' ? '活动结束后生成复盘报告' : '活动结束后开放复盘',
      icon: CollectionTag,
      done: false,
      active: isManager.value && status === 'ENDED',
      to: isManager.value ? `/admin/activities/${id}/stats` : '',
      action: '生成复盘'
    }
  ]
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
.enterprise-detail {
  gap: 18px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 24px;
}

.detail-hero-main {
  min-width: 0;
}

.detail-title-line {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.detail-title-line h1 {
  margin: 0;
  color: var(--ac-text);
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.16;
  letter-spacing: 0;
}

.detail-top-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 14px;
  color: var(--ac-muted);
  font-size: 14px;
}

.detail-top-meta span,
.detail-card-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.detail-top-meta svg,
.detail-card-title svg {
  width: 16px;
  height: 16px;
}

.detail-action-panel {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  max-width: 520px;
}

.detail-layout-v2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.detail-main-column,
.detail-side-column {
  display: grid;
  gap: 18px;
}

.detail-side-column {
  position: sticky;
  top: 96px;
}

.detail-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.detail-section-head.compact {
  margin-bottom: 14px;
}

.detail-section-head h2,
.detail-card-title h3 {
  margin: 6px 0 0;
  color: var(--ac-text);
  font-size: 18px;
}

.detail-section-head span,
.detail-side-text {
  margin: 0;
  color: var(--ac-muted);
  font-size: 13px;
}

.lifecycle-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.lifecycle-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--ac-border);
  border-radius: 16px;
  background: #fff;
}

.lifecycle-card.active {
  border-color: rgba(11, 125, 59, 0.28);
  background: #f3faf6;
}

.lifecycle-card.done .lifecycle-icon {
  color: #fff;
  background: var(--ac-primary);
}

.lifecycle-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 14px;
  color: var(--ac-primary-strong);
  background: rgba(11, 125, 59, 0.1);
}

.lifecycle-icon svg {
  width: 18px;
  height: 18px;
}

.lifecycle-card strong {
  color: var(--ac-text);
}

.lifecycle-card p {
  min-height: 38px;
  margin: 5px 0 4px;
  color: var(--ac-muted);
  font-size: 13px;
  line-height: 1.45;
}

.detail-info-grid-v2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-info-card-v2 {
  display: grid;
  gap: 14px;
  min-height: 190px;
}

.detail-card-title {
  color: var(--ac-primary-strong);
}

.detail-card-title h3 {
  color: var(--ac-text);
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

.detail-description p {
  margin: 0;
  color: var(--ac-muted);
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-cover-card {
  overflow: hidden;
  padding: 0;
}

.detail-cover-card img,
.detail-cover-fallback {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.detail-cover-fallback {
  display: grid;
  place-items: center;
  color: var(--ac-primary-strong);
  background:
    linear-gradient(135deg, rgba(11, 125, 59, 0.12), rgba(26, 188, 156, 0.1)),
    #fff;
}

.detail-cover-fallback span {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(11, 125, 59, 0.16);
  background: rgba(255, 255, 255, 0.8);
  font-weight: 800;
}

.detail-side-actions {
  display: grid;
  gap: 10px;
}

.detail-side-actions :deep(.el-button),
.detail-side-actions a {
  width: 100%;
}

.detail-side-actions :deep(.el-button) {
  justify-content: center;
}

.detail-media {
  display: grid;
  gap: 14px;
}

@media (max-width: 1000px) {
  .detail-hero,
  .detail-layout-v2 {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    display: grid;
  }

  .detail-action-panel {
    justify-content: flex-start;
    max-width: none;
  }

  .detail-side-column {
    position: static;
  }
}

@media (max-width: 760px) {
  .detail-hero {
    padding: 18px;
  }

  .lifecycle-grid,
  .detail-info-grid-v2 {
    grid-template-columns: 1fr;
  }

  .detail-action-panel :deep(.el-button),
  .detail-action-panel a {
    width: 100%;
  }

  .detail-field-list div {
    display: grid;
    gap: 4px;
  }

  .detail-field-list strong {
    text-align: left;
  }
}
</style>
