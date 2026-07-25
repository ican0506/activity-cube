<template>
  <section v-loading="loading">
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">负责人工作台</span>
        <h1>消息反馈</h1>
        <p>查看学生提交的活动建议、问题反馈和活动评价。</p>
      </div>
      <div class="feedback-summary">
        <span>反馈总数</span>
        <strong>{{ filteredFeedbacks.length }}</strong>
      </div>
    </div>

    <div class="panel filter-panel">
      <div class="pill-tabs">
        <button
          v-for="item in feedbackFilters"
          :key="item.value"
          type="button"
          :class="{ active: activeType === item.value }"
          @click="changeType(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div class="panel">
      <el-table v-if="filteredFeedbacks.length" :data="filteredFeedbacks" class="soft-table">
        <el-table-column prop="activityName" label="活动名称" min-width="180" />
        <el-table-column prop="studentName" label="学生姓名" width="130" />
        <el-table-column label="反馈类型" width="120">
          <template #default="{ row }">
            <el-tag :type="feedbackTypeTag(row.feedbackType)">
              {{ feedbackTypeText(row.feedbackType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="评分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.score">{{ row.score }} 分</span>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-else
        class="empty-wrap"
        description="暂无反馈，学生提交的活动建议、问题反馈和活动评价会显示在这里。"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listManagerFeedbacks } from '../../api/feedback'
import { feedbackTypeText } from '../../utils/options'

const feedbackFilters = [
  { label: '全部', value: 'all' },
  { label: '活动建议', value: 'suggestion' },
  { label: '问题反馈', value: 'problem' },
  { label: '活动评价', value: 'evaluation' }
]

const loading = ref(false)
const activeType = ref('all')
const feedbacks = ref([])

const filteredFeedbacks = computed(() => {
  if (activeType.value === 'all') return feedbacks.value
  return feedbacks.value.filter((item) => item.feedbackType === activeType.value)
})

function feedbackTypeTag(type) {
  if (type === 'problem') return 'warning'
  if (type === 'evaluation') return 'success'
  return 'primary'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadFeedbacks() {
  loading.value = true
  try {
    const params = activeType.value === 'all' ? {} : { type: activeType.value }
    feedbacks.value = await listManagerFeedbacks(params)
  } finally {
    loading.value = false
  }
}

function changeType(type) {
  activeType.value = type
  loadFeedbacks()
}

onMounted(loadFeedbacks)
</script>

<style scoped>
.feedback-summary {
  min-width: 140px;
  padding: 18px 22px;
  border: 1px solid #dceee7;
  border-radius: 18px;
  background: #f7fcf9;
  color: #5f716b;
}

.feedback-summary strong {
  display: block;
  margin-top: 8px;
  color: #0b7d3b;
  font-size: 34px;
  line-height: 1;
}

.filter-panel {
  margin-bottom: 18px;
}

.pill-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.pill-tabs button {
  border: 1px solid #dceee7;
  border-radius: 999px;
  background: #fff;
  color: #38524a;
  cursor: pointer;
  font-weight: 700;
  padding: 10px 18px;
  transition: all 0.2s ease;
}

.pill-tabs button:hover,
.pill-tabs button.active {
  border-color: #0b7d3b;
  background: #e9f7f0;
  color: #0b7d3b;
}

.muted-text {
  color: #98a2b3;
}

@media (max-width: 768px) {
  .feedback-summary {
    width: 100%;
  }
}
</style>
