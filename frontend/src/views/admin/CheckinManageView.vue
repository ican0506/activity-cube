<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">签到管理</h1>
        <p class="page-subtitle">生成签到二维码，查看已签到与未签到名单。</p>
      </div>
      <div class="toolbar">
        <el-button @click="copyLink">复制签到链接</el-button>
        <el-button type="primary" @click="download">导出签到名单</el-button>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric"><span>已签到</span><strong>{{ checkins.length }}</strong></div>
      <div class="metric"><span>未签到</span><strong>{{ absences.length }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ rate }}%</strong></div>
    </div>

    <div class="panel qr-box">
      <canvas ref="qr"></canvas>
      <span class="page-subtitle">{{ checkinUrl }}</span>
    </div>

    <div class="grid" style="grid-template-columns: 1fr 1fr; margin-top: 16px">
      <div class="panel">
        <h3>已签到名单</h3>
        <el-table :data="checkins" stripe>
          <el-table-column prop="userId" label="用户ID" />
          <el-table-column prop="campus" label="校区" />
          <el-table-column prop="checkinTime" label="签到时间" />
        </el-table>
      </div>
      <div class="panel">
        <h3>未签到名单</h3>
        <el-table :data="absences" stripe>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column prop="campus" label="校区" />
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { getActivity } from '../../api/activity'
import { listAbsences, listCheckins } from '../../api/checkin'
import { listRegistrations } from '../../api/registration'
import { checkedRegistrations, exportRosterExcel } from '../../utils/excelExport'
import { buildActivityQrLinks } from '../../utils/qrLinks'

const route = useRoute()
const qr = ref(null)
const activity = ref(null)
const registrations = ref([])
const checkins = ref([])
const absences = ref([])
const checkinUrl = buildActivityQrLinks(location.origin, route.params.id).checkinUrl
const rate = computed(() => {
  const total = checkins.value.length + absences.value.length
  return total ? Math.round((checkins.value.length * 10000) / total) / 100 : 0
})

async function load() {
  const [detail, registrationRows, checkinRows, absenceRows] = await Promise.all([
    getActivity(route.params.id),
    listRegistrations(route.params.id),
    listCheckins(route.params.id),
    listAbsences(route.params.id)
  ])
  activity.value = detail.activity
  registrations.value = registrationRows
  checkins.value = checkinRows
  absences.value = absenceRows
  await nextTick()
  QRCode.toCanvas(qr.value, checkinUrl, { width: 180 })
}

async function copyLink() {
  await navigator.clipboard.writeText(checkinUrl)
  ElMessage.success('已复制')
}

function download() {
  exportRosterExcel({
    activityName: activity.value?.title,
    suffix: '签到名单',
    registrations: checkedRegistrations(registrations.value, checkins.value),
    checkins: checkins.value
  })
  ElMessage.success('签到名单已导出')
}

onMounted(load)
</script>
