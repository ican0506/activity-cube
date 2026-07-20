<template>
  <section v-loading="loading">
    <div class="campus-hero stats-campus-hero">
      <div class="hero-copy">
        <span class="motto-badge">农业科技 · 活动复盘</span>
        <h1>{{ activity?.title || '活动数据统计' }}</h1>
        <p>从报名到签到，从参与到反馈，每一步都有迹可循。</p>
      </div>
      <el-button class="hero-button" @click="downloadAbsentees">导出未签到名单</el-button>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>报名人数</span><strong>{{ stats.registrationCount || 0 }}</strong></div>
      <div class="metric metric-accent"><span>签到人数</span><strong>{{ stats.checkinCount || 0 }}</strong></div>
      <div class="metric"><span>未签到人数</span><strong>{{ stats.absenceCount || 0 }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ Number(stats.checkinRate || 0).toFixed(1) }}%</strong></div>
    </div>

    <div class="insight-grid">
      <div class="insight-card">
        <span>校区覆盖</span>
        <strong>{{ campusStats.length }}</strong>
        <p>覆盖三校区与线上活动时，会在图表中集中对比。</p>
      </div>
      <div class="insight-card">
        <span>反馈人数</span>
        <strong>{{ feedbackStats.feedbackCount || 0 }}</strong>
        <p>结合满意度与文字反馈，帮助负责人快速复盘。</p>
      </div>
      <div class="insight-card">
        <span>平均评分</span>
        <strong>{{ Number(feedbackStats.averageScore || 0).toFixed(1) }}</strong>
        <p>评分越高，说明活动体验和现场组织越稳定。</p>
      </div>
      <div class="insight-card">
        <span>报名 → 签到</span>
        <strong>{{ formatRate(stats.registrationToCheckinRate ?? stats.checkinRate) }}</strong>
        <p>衡量报名学生实际到场情况。</p>
      </div>
      <div class="insight-card">
        <span>签到 → 反馈</span>
        <strong>{{ formatRate(stats.checkinToFeedbackRate) }}</strong>
        <p>衡量活动结束后的反馈回收情况。</p>
      </div>
      <div class="insight-card">
        <span>报名 → 反馈</span>
        <strong>{{ formatRate(stats.registrationToFeedbackRate) }}</strong>
        <p>衡量从报名到复盘的完整闭环。</p>
      </div>
    </div>

    <div class="stats-dashboard-grid">
      <div class="panel">
        <div class="page-head">
          <div>
            <h2 class="page-title">校区数据分布</h2>
            <p class="page-subtitle">对比各校区报名和签到情况，快速判断活动覆盖效果。</p>
          </div>
        </div>
        <div v-if="campusStats.length > 0" ref="chart" class="stats-chart"></div>
        <el-empty v-else class="empty-wrap" description="暂无报名数据" />
      </div>

      <div class="ai-summary-card">
        <span class="wheat-badge">AI 活动总结</span>
        <h2>活动总结自动生成</h2>
        <p>{{ aiSummary }}</p>
        <div class="campus-strip">
          <span>报名 {{ stats.registrationCount || 0 }}</span>
          <span>签到 {{ stats.checkinCount || 0 }}</span>
          <span>反馈 {{ feedbackStats.feedbackCount || 0 }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getActivity } from '../../api/activity'
import { listAbsentees } from '../../api/checkin'
import { getFeedbackStats } from '../../api/feedback'
import { getActivityStats } from '../../api/stat'
import { exportRosterExcel } from '../../utils/excelExport'
import { recordFrontendExport } from '../../api/export'

const route = useRoute()
const stats = ref({})
const feedbackStats = ref({ feedbackCount: 0, averageScore: 0 })
const activity = ref(null)
const chart = ref(null)
const loading = ref(false)
let chartInstance = null
const campusStats = computed(() => stats.value.campusStats || [])
const aiSummary = computed(() => {
  const signRate = Number(stats.value.checkinRate || 0).toFixed(1)
  const score = Number(feedbackStats.value.averageScore || 0).toFixed(1)
  if (!stats.value.registrationCount) return '当前活动暂无报名数据，发布后可在这里查看报名、签到与反馈的复盘摘要。'
  return `本次活动报名 ${stats.value.registrationCount || 0} 人，签到率 ${signRate}%，平均满意度 ${score} 分。可结合未签到名单和反馈内容优化下次组织节奏。`
})

function formatRate(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

async function load() {
  loading.value = true
  try {
    const [detail, statData, feedbackData] = await Promise.all([
      getActivity(route.params.id),
      getActivityStats(route.params.id),
      getFeedbackStats(route.params.id).catch(() => ({ feedbackCount: 0, averageScore: 0 }))
    ])
    activity.value = detail.activity
    stats.value = statData
    feedbackStats.value = feedbackData
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
  chartInstance.setOption({
    color: ['#0b7d3b', '#1abc9c'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, textStyle: { color: '#38524a' } },
    grid: { left: 18, right: 18, top: 52, bottom: 20, containLabel: true },
    xAxis: {
      type: 'category',
      data: campusStats.value.map((item) => item.campus),
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
      { name: '报名人数', type: 'bar', barMaxWidth: 34, data: campusStats.value.map((item) => item.registrationCount), itemStyle: { borderRadius: [10, 10, 0, 0] } },
      { name: '签到人数', type: 'bar', barMaxWidth: 34, data: campusStats.value.map((item) => item.checkinCount), itemStyle: { borderRadius: [10, 10, 0, 0] } }
    ]
  })
}

function resizeChart() {
  if (chartInstance) chartInstance.resize()
}

async function downloadAbsentees() {
  const absentees = await listAbsentees(route.params.id)
  exportRosterExcel({
    activityName: activity.value?.title,
    suffix: '未签到名单',
    registrations: absentees
  })
  await recordFrontendExport(route.params.id, 'absences')
  ElMessage.success('未签到名单已导出')
}

onMounted(() => {
  load()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) chartInstance.dispose()
})
</script>
