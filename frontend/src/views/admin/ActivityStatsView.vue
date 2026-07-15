<template>
  <section v-loading="loading">
    <div class="admin-intro">
      <div>
        <h1>活动数据统计</h1>
        <p>追踪报名人数、签到人数、未签到人数和签到率，辅助活动复盘。</p>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>报名人数</span><strong>{{ stats.registrationCount || 0 }}</strong></div>
      <div class="metric metric-accent"><span>签到人数</span><strong>{{ stats.checkinCount || 0 }}</strong></div>
      <div class="metric"><span>未签到人数</span><strong>{{ stats.absenceCount || 0 }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ Number(stats.checkinRate || 0).toFixed(1) }}%</strong></div>
    </div>

    <div class="panel">
      <div class="page-head">
        <div>
          <h2 class="page-title">校区数据分布</h2>
          <p class="page-subtitle">对比各校区报名和签到情况。</p>
        </div>
      </div>
      <div v-if="campusStats.length > 0" ref="chart" class="stats-chart"></div>
      <el-empty v-else class="empty-wrap" description="暂无报名数据" />
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getActivityStats } from '../../api/stat'

const route = useRoute()
const stats = ref({})
const chart = ref(null)
const loading = ref(false)
let chartInstance = null
const campusStats = computed(() => stats.value.campusStats || [])

async function load() {
  loading.value = true
  try {
    stats.value = await getActivityStats(route.params.id)
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chart.value || campusStats.value.length === 0) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chart.value)
  const campuses = campusStats.value.map((item) => item.campus)
  chartInstance.setOption({
    color: ['#16a085', '#2ecc71'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, textStyle: { color: '#38524a' } },
    grid: { left: 18, right: 18, top: 52, bottom: 20, containLabel: true },
    xAxis: {
      type: 'category',
      data: campuses,
      axisLine: { lineStyle: { color: '#dcebe4' } },
      axisLabel: { color: '#667085' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#edf5f0' } },
      axisLabel: { color: '#667085' }
    },
    series: [
      {
        name: '报名人数',
        type: 'bar',
        barMaxWidth: 34,
        data: campusStats.value.map((item) => item.registrationCount)
      },
      {
        name: '签到人数',
        type: 'bar',
        barMaxWidth: 34,
        data: campusStats.value.map((item) => item.checkinCount)
      }
    ]
  })
}

function resizeChart() {
  if (chartInstance) chartInstance.resize()
}

onMounted(load)
window.addEventListener('resize', resizeChart)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) chartInstance.dispose()
})
</script>
