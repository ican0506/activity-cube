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
      <div class="metric metric-accent"><span>已签到</span><strong>{{ checkins.length }}</strong></div>
      <div class="metric"><span>未签到</span><strong>{{ absences.length }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ rate }}%</strong></div>
    </div>

    <div class="panel qr-card">
      <div class="qr-box">
        <canvas ref="qr"></canvas>
      </div>
      <div>
        <h3>签到二维码</h3>
        <p class="page-subtitle">{{ checkinUrl }}</p>
      </div>
    </div>

    <div class="checkin-grid" style="margin-top: 16px">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>已签到名单</h2>
            <p>活动时间内完成签到的记录。</p>
          </div>
        </div>
        <el-table :data="checkins" stripe>
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="campus" label="校区" />
          <el-table-column prop="checkinTime" label="签到时间" min-width="170" />
        </el-table>
        <el-empty v-if="!checkins.length" description="暂无签到数据" />
      </div>
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>未签到名单</h2>
            <p>已报名但未签到的学生。</p>
          </div>
        </div>
        <el-table :data="absences" stripe>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column prop="campus" label="校区" />
        </el-table>
        <el-empty v-if="!absences.length" description="暂无未签到数据" />
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
const checkinUrl = computed(() => buildActivityQrLinks(location.origin, route.params.id, {
  checkinCode: activity.value?.checkinCode
}).checkinUrl)
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
  if (qr.value) QRCode.toCanvas(qr.value, checkinUrl.value, { width: 180 })
}

async function copyLink() {
  await navigator.clipboard.writeText(checkinUrl.value)
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
