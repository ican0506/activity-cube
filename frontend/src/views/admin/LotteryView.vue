<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">随机抽奖</h1>
        <p class="page-subtitle">{{ activity?.title || '活动抽奖管理' }}</p>
      </div>
      <RouterLink to="/admin/activities">
        <el-button>返回活动管理</el-button>
      </RouterLink>
    </div>

    <div class="lottery-layout">
      <div class="panel lottery-control-panel" v-loading="loading">
        <div class="lottery-badge">
          <el-icon><MagicStick /></el-icon>
          <span>校园幸运抽取</span>
        </div>
        <h2>{{ activity?.title || '加载活动中...' }}</h2>
        <p>从报名名单或已签到名单中随机抽取，结果会保存为历史抽奖记录。</p>

        <el-form label-position="top" class="lottery-form">
          <el-form-item label="抽奖来源">
            <el-segmented v-model="form.source" :options="sourceOptions" />
          </el-form-item>
          <el-form-item label="抽取人数">
            <el-input-number v-model="form.count" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="重复中奖">
            <el-switch v-model="form.allowRepeat" active-text="允许" inactive-text="不允许" />
          </el-form-item>
          <el-button class="lottery-start" type="primary" :icon="MagicStick" :loading="drawing" @click="startDraw">
            开始抽奖
          </el-button>
        </el-form>
      </div>

      <div class="panel lottery-stage">
        <div class="stage-screen" :class="{ spinning: drawing }">
          <div class="stage-glow"></div>
          <div class="stage-content">
            <el-icon class="stage-icon"><Trophy /></el-icon>
            <strong>{{ rollingName }}</strong>
            <span>{{ drawing ? '名单滚动中' : '等待开始抽奖' }}</span>
          </div>
        </div>
        <div class="lottery-tip">
          当前来源：{{ sourceText(form.source) }} · {{ form.allowRepeat ? '允许重复中奖' : '排除历史中奖人' }}
        </div>
      </div>
    </div>

    <div class="panel result-panel">
      <div class="section-title">
        <div>
          <h2>本轮抽奖结果</h2>
          <p>中奖同学以卡片形式展示，可继续切换来源进行下一轮抽奖。</p>
        </div>
        <el-tag v-if="currentResults.length" type="success">第 {{ currentRoundNo }} 轮</el-tag>
      </div>
      <div v-if="currentResults.length" class="winner-grid">
        <div v-for="winner in currentResults" :key="winner.id || `${winner.userId}-${winner.roundNo}`" class="winner-card">
          <div class="winner-medal"><el-icon><Medal /></el-icon></div>
          <h3>{{ winner.realName }}</h3>
          <p>{{ winner.studentNo }}</p>
          <div class="winner-meta">
            <span>{{ winner.college || '-' }}</span>
            <span>{{ winner.className || '-' }}</span>
            <span>{{ winner.phone || '-' }}</span>
            <span>{{ winner.campus || '-' }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无本轮抽奖结果" />
    </div>

    <div class="panel history-panel" v-loading="historyLoading">
      <div class="section-title">
        <div>
          <h2>历史抽奖结果</h2>
          <p>按抽奖轮次查看已经保存的中奖名单。</p>
        </div>
        <el-button :icon="Refresh" @click="loadHistory">刷新</el-button>
      </div>
      <el-collapse v-if="historyGroups.length" class="history-collapse">
        <el-collapse-item
          v-for="group in historyGroups"
          :key="group.roundNo"
          :title="`第 ${group.roundNo} 轮 · ${sourceText(group.source)} · ${group.items.length} 人`"
          :name="group.roundNo"
        >
          <div class="history-list">
            <div v-for="item in group.items" :key="item.id" class="history-row">
              <strong>{{ item.realName }}</strong>
              <span>{{ item.studentNo }}</span>
              <span>{{ item.college }}</span>
              <span>{{ item.className }}</span>
              <span>{{ item.phone }}</span>
              <span>{{ item.campus }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-else description="暂无历史抽奖结果" />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Medal, Refresh, Trophy } from '@element-plus/icons-vue'
import { getActivity } from '../../api/activity'
import { drawLottery, listLotteryResults } from '../../api/lottery'

const route = useRoute()
const activity = ref(null)
const loading = ref(false)
const drawing = ref(false)
const historyLoading = ref(false)
const currentResults = ref([])
const historyRows = ref([])
const rollingName = ref('准备抽奖')
let rollingTimer = null

const form = reactive({
  source: 'registration',
  count: 1,
  allowRepeat: false
})

const sourceOptions = [
  { label: '报名名单', value: 'registration' },
  { label: '已签到名单', value: 'checkin' }
]

const currentRoundNo = computed(() => currentResults.value[0]?.roundNo || '-')
const historyGroups = computed(() => {
  const groups = new Map()
  for (const item of historyRows.value) {
    const key = item.roundNo || 0
    if (!groups.has(key)) {
      groups.set(key, { roundNo: key, source: item.source, items: [] })
    }
    groups.get(key).items.push(item)
  }
  return Array.from(groups.values()).sort((a, b) => b.roundNo - a.roundNo)
})

function sourceText(source) {
  return source === 'checkin' ? '已签到名单' : '报名名单'
}

async function startDraw() {
  drawing.value = true
  currentResults.value = []
  startRolling()
  try {
    const [results] = await Promise.all([
      drawLottery(route.params.id, form),
      new Promise((resolve) => setTimeout(resolve, 900))
    ])
    currentResults.value = results
    rollingName.value = results[0]?.realName || '抽奖完成'
    ElMessage.success('抽奖完成')
    await loadHistory()
  } finally {
    stopRolling()
    drawing.value = false
  }
}

function startRolling() {
  const names = ['报名名单滚动中', '候选人洗牌中', '幸运名单生成中', '结果保存中']
  let index = 0
  rollingName.value = names[index]
  rollingTimer = window.setInterval(() => {
    index = (index + 1) % names.length
    rollingName.value = names[index]
  }, 140)
}

function stopRolling() {
  if (rollingTimer) {
    window.clearInterval(rollingTimer)
    rollingTimer = null
  }
}

async function loadActivity() {
  loading.value = true
  try {
    const detail = await getActivity(route.params.id)
    activity.value = detail.activity
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    historyRows.value = await listLotteryResults(route.params.id)
  } finally {
    historyLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadActivity(), loadHistory()])
})

onBeforeUnmount(stopRolling)
</script>
