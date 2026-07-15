<template>
  <section v-loading="loading">
    <div class="admin-intro">
      <div>
        <h1>二维码管理</h1>
        <p>生成活动报名码和签到码，用于海报、现场展板或班级群转发。</p>
      </div>
      <RouterLink :to="`/admin/activities/${id}/checkins`">
        <el-button class="hero-button" :icon="Checked">查看签到名单</el-button>
      </RouterLink>
    </div>

    <div v-if="activity" class="panel qr-summary">
      <div>
        <h2>{{ activity.title }}</h2>
        <p class="page-subtitle">{{ activity.campus }} · {{ activity.location || '地点待定' }}</p>
      </div>
      <div class="meta-row">
        <el-tag :type="statusTagType(activity.status)">{{ statusText(activity.status) }}</el-tag>
        <span>报名：{{ activity.registerStartTime }} 至 {{ activity.registerEndTime }}</span>
        <span>活动：{{ activity.startTime }} 至 {{ activity.endTime }}</span>
      </div>
    </div>

    <div class="qr-manage-grid">
      <div class="panel qr-manage-card">
        <div class="qr-card-head">
          <div>
            <h3>报名二维码</h3>
            <p class="page-subtitle">学生扫码后进入活动报名页，提交报名信息。</p>
          </div>
          <el-tag type="success">报名</el-tag>
        </div>
        <div class="qr-canvas-wrap">
          <canvas ref="registerQr"></canvas>
        </div>
        <p class="qr-link">{{ links.registerUrl }}</p>
        <div class="toolbar">
          <el-button type="primary" :icon="CopyDocument" @click="copy(links.registerUrl)">复制报名链接</el-button>
          <el-button :icon="View" @click="open(links.registerUrl)">打开报名页</el-button>
        </div>
      </div>

      <div class="panel qr-manage-card">
        <div class="qr-card-head">
          <div>
            <h3>签到二维码</h3>
            <p class="page-subtitle">活动开始后学生扫码进入签到页，确认后写入签到记录。</p>
          </div>
          <el-tag type="warning">签到</el-tag>
        </div>
        <div class="qr-canvas-wrap">
          <canvas ref="checkinQr"></canvas>
        </div>
        <p class="qr-link">{{ links.checkinUrl }}</p>
        <div class="toolbar">
          <el-button type="primary" :icon="CopyDocument" @click="copy(links.checkinUrl)">复制签到链接</el-button>
          <el-button :icon="View" @click="open(links.checkinUrl)">打开签到页</el-button>
        </div>
      </div>
    </div>

    <el-alert
      class="qr-tip"
      title="使用说明"
      type="success"
      :closable="false"
      show-icon
      description="报名二维码适合提前发布；签到二维码建议只在活动开始时展示。签到仍会走后端规则校验，未报名、重复签到、非活动时间都会被拦截。"
    />
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Checked, CopyDocument, View } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { getActivity } from '../../api/activity'
import { buildActivityQrLinks } from '../../utils/qrLinks'
import { statusText } from '../../utils/options'

const route = useRoute()
const id = route.params.id
const loading = ref(false)
const activity = ref(null)
const registerQr = ref(null)
const checkinQr = ref(null)
const links = computed(() => buildActivityQrLinks(location.origin, id))

async function load() {
  loading.value = true
  try {
    const detail = await getActivity(id)
    activity.value = detail.activity
    await nextTick()
    await renderQrs()
  } finally {
    loading.value = false
  }
}

async function renderQrs() {
  if (registerQr.value) {
    await QRCode.toCanvas(registerQr.value, links.value.registerUrl, { width: 220, margin: 2 })
  }
  if (checkinQr.value) {
    await QRCode.toCanvas(checkinQr.value, links.value.checkinUrl, { width: 220, margin: 2 })
  }
}

async function copy(url) {
  await navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制')
}

function open(url) {
  window.open(url, '_blank', 'noopener,noreferrer')
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
