<template>
  <section v-loading="loading">
    <div v-if="detail" class="detail-layout">
      <aside class="activity-poster">
        <div class="poster-content">
          <el-tag :type="statusTagType(detail.activity.status)" effect="dark">
            {{ statusText(detail.activity.status) }}
          </el-tag>
          <h1>{{ detail.activity.title }}</h1>
          <p>{{ detail.activity.description || '活动详情正在完善中，欢迎关注报名时间和活动安排。' }}</p>
        </div>
        <div class="poster-foot">
          <div class="meta-item">
            <el-icon><Location /></el-icon>
            {{ detail.activity.campus }} · {{ detail.activity.location || '地点待定' }}
          </div>
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            {{ detail.activity.startTime }}
          </div>
        </div>
      </aside>

      <div class="info-stack">
        <div class="page-head">
          <div>
            <h2 class="page-title">活动信息</h2>
            <p class="page-subtitle">查看活动安排、报名时间和签到入口。</p>
          </div>
          <div class="toolbar">
            <RouterLink :to="`/activities/${id}/register`">
              <el-button type="primary" :icon="EditPen">立即报名</el-button>
            </RouterLink>
            <RouterLink :to="`/activities/${id}/checkin`">
              <el-button :icon="Checked">扫码签到入口</el-button>
            </RouterLink>
            <RouterLink :to="`/activities/${id}/feedback`">
              <el-button :icon="ChatDotRound">活动反馈</el-button>
            </RouterLink>
          </div>
        </div>

        <div class="metric-row">
          <div class="metric metric-accent"><span>报名人数</span><strong>{{ detail.registrationCount }}</strong></div>
          <div class="metric metric-accent"><span>签到人数</span><strong>{{ detail.checkinCount }}</strong></div>
          <div class="metric"><span>人数上限</span><strong>{{ detail.activity.maxParticipants || '不限' }}</strong></div>
        </div>

        <div class="panel">
          <el-descriptions border :column="2">
            <el-descriptions-item label="活动时间">{{ detail.activity.startTime }} 至 {{ detail.activity.endTime }}</el-descriptions-item>
            <el-descriptions-item label="报名时间">{{ detail.activity.registerStartTime }} 至 {{ detail.activity.registerEndTime }}</el-descriptions-item>
            <el-descriptions-item label="活动校区">{{ detail.activity.campus }}</el-descriptions-item>
            <el-descriptions-item label="活动地点">{{ detail.activity.location || '地点待定' }}</el-descriptions-item>
            <el-descriptions-item label="人数上限">{{ detail.activity.maxParticipants || '不限' }}</el-descriptions-item>
            <el-descriptions-item label="跨校区">{{ detail.activity.allowCrossCampus ? '允许' : '不允许' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="panel qr-card">
          <div class="qr-box">
            <canvas ref="registerQr"></canvas>
          </div>
          <div>
            <h3>报名二维码</h3>
            <p class="page-subtitle">{{ registerUrl }}</p>
          </div>
        </div>
      </div>
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
import { buildActivityQrLinks } from '../utils/qrLinks'
import { statusText } from '../utils/options'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const registerQr = ref(null)
const loading = ref(false)
const registerUrl = computed(() => buildActivityQrLinks(location.origin, id).registerUrl)

async function load() {
  loading.value = true
  try {
    detail.value = await getActivity(id)
    await nextTick()
    if (registerQr.value) {
      QRCode.toCanvas(registerQr.value, registerUrl.value, { width: 180 })
    }
  } finally {
    loading.value = false
  }
}

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

onMounted(load)
</script>
