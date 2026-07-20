<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">反馈统计</span>
        <h1>{{ activity?.title || '反馈统计' }}</h1>
        <p>查看评分分布、文字建议和图片视频附件。</p>
      </div>
      <RouterLink to="/admin/activities">
        <el-button>返回活动管理</el-button>
      </RouterLink>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>反馈人数</span><strong>{{ stats.feedbackCount || 0 }}</strong></div>
      <div class="metric"><span>平均评分</span><strong>{{ averageScoreText }}</strong></div>
      <div class="metric"><span>5 分人数</span><strong>{{ distribution[5] || 0 }}</strong></div>
      <div class="metric"><span>低分反馈</span><strong>{{ lowScoreCount }}</strong></div>
    </div>

    <div class="feedback-stats-grid">
      <div class="panel" v-loading="loading">
        <div class="section-title">
          <div>
            <h2>评分分布</h2>
            <p>按 5 分到 1 分展示满意度人数，快速判断活动体验。</p>
          </div>
        </div>
        <div v-show="rows.length" ref="chartRef" class="feedback-chart"></div>
        <el-empty v-if="!loading && rows.length === 0" description="暂无反馈数据" />
      </div>

      <div class="panel voice-wall" v-loading="loading">
        <div class="section-title">
          <div>
            <h2>文字反馈</h2>
            <p>匿名反馈显示为“匿名同学”，附件仍可正常查看。</p>
          </div>
        </div>
        <div v-if="rows.length" class="feedback-list">
          <div v-for="item in rows" :key="item.id" class="feedback-item">
            <div class="feedback-item-head">
              <strong>{{ displayName(item) }}</strong>
              <el-rate :model-value="item.score" disabled />
            </div>
            <p v-if="item.content"><span>活动体验：</span>{{ item.content }}</p>
            <p v-if="item.suggestion"><span>改进建议：</span>{{ item.suggestion }}</p>
            <MediaPreview :items="item.mediaList || []" />
            <small>{{ item.createdAt }}</small>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="暂无文字反馈" />
      </div>
    </div>

    <div class="panel feedback-table-panel" v-loading="loading">
      <div class="section-title">
        <div>
          <h2>反馈明细</h2>
          <p>用于快速检索每条反馈记录和附件内容。</p>
        </div>
      </div>
      <el-table :data="rows" stripe>
        <el-table-column label="姓名" width="130">
          <template #default="{ row }">{{ displayName(row) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="100" />
        <el-table-column prop="content" label="活动体验" min-width="220" />
        <el-table-column prop="suggestion" label="改进建议" min-width="220" />
        <el-table-column label="附件" min-width="220">
          <template #default="{ row }">
            <MediaPreview :items="row.mediaList || []" compact />
          </template>
        </el-table-column>
        <el-table-column prop="anonymous" label="匿名" width="100">
          <template #default="{ row }">
            <el-tag :type="row.anonymous ? 'info' : 'success'">{{ row.anonymous ? '匿名' : '实名' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无反馈数据" />
    </div>
  </section>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElImage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { resolveFileUrl } from '../../api/file'
import { getFeedbackStats, listFeedbacks } from '../../api/feedback'
import { listFeedbackMedia } from '../../api/media'

const MediaPreview = defineComponent({
  props: {
    items: { type: Array, default: () => [] },
    compact: { type: Boolean, default: false }
  },
  setup(props) {
    return () => {
      if (!props.items.length) {
        return h('span', { class: 'upload-tip' }, '无附件')
      }
      const images = props.items.filter((item) => item.mediaType === 'image').map((item) => resolveFileUrl(item.url))
      return h('div', { class: props.compact ? 'feedback-media compact' : 'feedback-media' }, props.items.map((item) => {
        if (item.mediaType === 'image') {
          return h(ElImage, {
            key: item.id || item.url,
            src: resolveFileUrl(item.url),
            fit: 'cover',
            previewSrcList: images,
            previewTeleported: true
          })
        }
        return h('video', {
          key: item.id || item.url,
          src: resolveFileUrl(item.url),
          controls: true
        })
      }))
    }
  }
})

const route = useRoute()
const rows = ref([])
const activity = ref(null)
const stats = ref({ feedbackCount: 0, averageScore: 0, scoreDistribution: {} })
const loading = ref(false)
const chartRef = ref(null)
let chart = null

const distribution = computed(() => stats.value.scoreDistribution || {})
const averageScoreText = computed(() => Number(stats.value.averageScore || 0).toFixed(1))
const lowScoreCount = computed(() => (distribution.value[1] || 0) + (distribution.value[2] || 0))

function displayName(item) {
  return item.anonymous ? '匿名同学' : (item.realName || '农大同学')
}

async function load() {
  loading.value = true
  try {
    const [detail, feedbackRows, feedbackStats] = await Promise.all([
      getActivity(route.params.id),
      listFeedbacks(route.params.id),
      getFeedbackStats(route.params.id)
    ])
    const mediaMap = await loadFeedbackMedia(feedbackRows)
    activity.value = detail.activity
    rows.value = feedbackRows.map((row) => ({ ...row, mediaList: mediaMap[row.id] || [] }))
    stats.value = feedbackStats
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

async function loadFeedbackMedia(feedbackRows) {
  const pairs = await Promise.all(feedbackRows.map(async (row) => [row.id, await listFeedbackMedia(row.id)]))
  return Object.fromEntries(pairs)
}

function renderChart() {
  if (!chartRef.value || rows.value.length === 0) return
  chart = chart || echarts.init(chartRef.value)
  chart.setOption({
    color: ['#0b7d3b'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 26, bottom: 28 },
    xAxis: {
      type: 'category',
      data: ['5 分', '4 分', '3 分', '2 分', '1 分'],
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
        type: 'bar',
        data: [5, 4, 3, 2, 1].map((score) => distribution.value[score] || 0),
        barWidth: 34,
        itemStyle: { borderRadius: [10, 10, 0, 0] }
      }
    ]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resizeChart)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>
