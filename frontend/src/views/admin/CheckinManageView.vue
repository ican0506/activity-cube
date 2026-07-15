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
import { listAbsences, listCheckins } from '../../api/checkin'
import { downloadExport } from '../../api/export'

const route = useRoute()
const qr = ref(null)
const checkins = ref([])
const absences = ref([])
const checkinUrl = `${location.origin}/activities/${route.params.id}/checkin`
const rate = computed(() => {
  const total = checkins.value.length + absences.value.length
  return total ? Math.round((checkins.value.length * 10000) / total) / 100 : 0
})

async function load() {
  checkins.value = await listCheckins(route.params.id)
  absences.value = await listAbsences(route.params.id)
  await nextTick()
  QRCode.toCanvas(qr.value, checkinUrl, { width: 180 })
}

async function copyLink() {
  await navigator.clipboard.writeText(checkinUrl)
  ElMessage.success('已复制')
}

function download() {
  downloadExport(route.params.id, 'checkins')
}

onMounted(load)
</script>
