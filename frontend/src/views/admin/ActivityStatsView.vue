<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">活动数据统计</h1>
        <p class="page-subtitle">查看报名、签到、未签到、签到率和校区统计。</p>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric"><span>报名人数</span><strong>{{ stats.registrationCount || 0 }}</strong></div>
      <div class="metric"><span>签到人数</span><strong>{{ stats.checkinCount || 0 }}</strong></div>
      <div class="metric"><span>未签到人数</span><strong>{{ stats.absenceCount || 0 }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ Number(stats.checkinRate || 0).toFixed(1) }}%</strong></div>
    </div>

    <div class="panel">
      <div ref="chart" style="height: 320px"></div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getActivityStats } from '../../api/stat'

const route = useRoute()
const stats = ref({})
const chart = ref(null)

async function load() {
  stats.value = await getActivityStats(route.params.id)
  await nextTick()
  const instance = echarts.init(chart.value)
  const campuses = (stats.value.campusStats || []).map((item) => item.campus)
  instance.setOption({
    tooltip: {},
    legend: {},
    xAxis: { type: 'category', data: campuses },
    yAxis: { type: 'value' },
    series: [
      { name: '报名人数', type: 'bar', data: (stats.value.campusStats || []).map((item) => item.registrationCount) },
      { name: '签到人数', type: 'bar', data: (stats.value.campusStats || []).map((item) => item.checkinCount) }
    ]
  })
}

onMounted(load)
</script>
