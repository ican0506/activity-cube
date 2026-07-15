<template>
  <section v-if="detail">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ detail.activity.title }}</h1>
        <p class="page-subtitle">{{ detail.activity.campus }} · {{ detail.activity.location }}</p>
      </div>
      <div class="toolbar">
        <RouterLink :to="`/activities/${id}/register`">
          <el-button type="primary">立即报名</el-button>
        </RouterLink>
        <RouterLink :to="`/activities/${id}/checkin`">
          <el-button>扫码签到入口</el-button>
        </RouterLink>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric"><span>报名人数</span><strong>{{ detail.registrationCount }}</strong></div>
      <div class="metric"><span>签到人数</span><strong>{{ detail.checkinCount }}</strong></div>
      <div class="metric"><span>活动状态</span><strong>{{ statusText(detail.activity.status) }}</strong></div>
    </div>

    <div class="panel grid">
      <p>{{ detail.activity.description }}</p>
      <el-descriptions border :column="2">
        <el-descriptions-item label="活动时间">{{ detail.activity.startTime }} 至 {{ detail.activity.endTime }}</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ detail.activity.registerStartTime }} 至 {{ detail.activity.registerEndTime }}</el-descriptions-item>
        <el-descriptions-item label="人数上限">{{ detail.activity.maxParticipants || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="跨校区">{{ detail.activity.allowCrossCampus ? '允许' : '不允许' }}</el-descriptions-item>
      </el-descriptions>
      <div class="qr-box">
        <canvas ref="registerQr"></canvas>
        <span class="page-subtitle">报名二维码：{{ registerUrl }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import { getActivity } from '../api/activity'
import { statusText } from '../utils/options'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const registerQr = ref(null)
const registerUrl = computed(() => `${location.origin}/activities/${id}/register`)

async function load() {
  detail.value = await getActivity(id)
  await nextTick()
  QRCode.toCanvas(registerQr.value, registerUrl.value, { width: 180 })
}

onMounted(load)
</script>
