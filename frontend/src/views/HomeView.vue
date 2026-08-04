<template>
  <section class="dashboard-page" v-loading="loading">
    <div class="dashboard-hero ds-card">
      <div class="dashboard-hero-main">
        <span class="ds-eyebrow">{{ roleLabel }} Dashboard</span>
        <h1>{{ greetingTitle }}</h1>
        <p>{{ greetingSubtitle }}</p>
      </div>
      <div class="dashboard-hero-side">
        <span>AI Actions</span>
        <strong>{{ aiActions.length }}</strong>
        <p>{{ aiActionHint }}</p>
      </div>
    </div>

    <div class="dashboard-metrics">
      <article v-for="item in metrics" :key="item.label" class="ds-card metric-tile">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.hint }}</p>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="ds-card dashboard-section dashboard-task-card">
        <div class="dashboard-section-head">
          <div>
            <span class="ds-eyebrow">Today</span>
            <h2>{{ taskTitle }}</h2>
          </div>
          <RouterLink :to="primaryTaskPath">
            <el-button text>查看全部</el-button>
          </RouterLink>
        </div>
        <div v-if="tasks.length" class="task-list">
          <RouterLink v-for="task in tasks" :key="task.title" :to="task.to" class="task-item">
            <span class="task-dot" :class="task.tone"></span>
            <div>
              <strong>{{ task.title }}</strong>
              <p>{{ task.description }}</p>
            </div>
            <span class="task-action">{{ task.action }}</span>
          </RouterLink>
        </div>
        <el-empty v-else description="当前没有待处理事项" />
      </section>

      <section class="ds-card dashboard-section ai-action-panel">
        <div class="dashboard-section-head">
          <div>
            <span class="ds-eyebrow ai">AI</span>
            <h2>AI 工作建议</h2>
          </div>
        </div>
        <div class="ai-action-list">
          <RouterLink v-for="item in aiActions" :key="item.title" :to="item.to" class="ai-action-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </RouterLink>
        </div>
      </section>
    </div>

    <div class="dashboard-grid lower">
      <section class="ds-card dashboard-section">
        <div class="dashboard-section-head">
          <div>
            <span class="ds-eyebrow">Activities</span>
            <h2>{{ activitySectionTitle }}</h2>
          </div>
          <RouterLink :to="activityListPath">
            <el-button text>进入列表</el-button>
          </RouterLink>
        </div>
        <div v-if="focusActivities.length" class="dashboard-activity-list">
          <RouterLink
            v-for="activity in focusActivities"
            :key="activity.id"
            class="dashboard-activity-row"
            :to="activityDetailPath(activity)"
          >
            <div>
              <strong>{{ activity.title }}</strong>
              <p>{{ formatActivityMeta(activity) }}</p>
            </div>
            <el-tag :type="statusTagType(activity.status)" round>
              {{ activityDisplayStatusText(activity) }}
            </el-tag>
          </RouterLink>
        </div>
        <el-empty v-else description="暂无活动数据" />
      </section>

      <section class="ds-card dashboard-section notice-panel">
        <div class="dashboard-section-head">
          <div>
            <span class="ds-eyebrow">Messages</span>
            <h2>最新通知</h2>
          </div>
        </div>
        <div class="notice-summary">
          <strong>{{ unreadCount }}</strong>
          <span>未读消息</span>
          <p>{{ noticeHint }}</p>
        </div>
        <RouterLink :to="messagePath">
          <el-button type="primary" plain>查看消息</el-button>
        </RouterLink>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Calendar, DataAnalysis, MagicStick, Message, Monitor, User } from '@element-plus/icons-vue'
import { listActivities } from '../api/activity'
import { getUnreadMessageCount } from '../api/message'
import { useUserStore } from '../stores/user'
import { activityDisplayStatusText, statusTagType } from '../utils/options'

const userStore = useUserStore()
const loading = ref(false)
const activities = ref([])
const unreadCount = ref(0)

const role = computed(() => userStore.role || 'student')
const isStudent = computed(() => ['student', 'user', ''].includes(role.value))
const isAdmin = computed(() => role.value === 'admin')
const displayName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '同学')
const roleLabel = computed(() => {
  if (isAdmin.value) return 'Admin'
  if (role.value === 'organizer') return 'Organizer'
  return 'Student'
})

const greetingTitle = computed(() => {
  if (isAdmin.value) return `欢迎回来，${displayName.value}`
  if (role.value === 'organizer') return `今天的活动运营，从这里开始`
  return `发现活动，记录成长足迹`
})

const greetingSubtitle = computed(() => {
  if (isAdmin.value) return '查看平台运行状态、活动审核和用户治理事项。'
  if (role.value === 'organizer') return '集中处理活动创建、报名签到、反馈复盘和 AI 运营建议。'
  return '快速查看今日可参与活动、AI 推荐、待签到和未读通知。'
})

const registeringActivities = computed(() => activities.value.filter((item) => item.status === 'REGISTERING'))
const ongoingActivities = computed(() => activities.value.filter((item) => item.status === 'ONGOING'))
const endedActivities = computed(() => activities.value.filter((item) => item.status === 'ENDED'))
const pendingReviewActivities = computed(() => activities.value.filter((item) => item.status === 'PENDING_REVIEW'))
const todayActivities = computed(() => activities.value.filter((item) => isToday(item.startTime)))
const checkinActivities = computed(() => activities.value.filter((item) => item.canCheckin && !item.checkedIn))

const metrics = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '待审核活动', value: pendingReviewActivities.value.length, hint: '需要管理员处理的活动申请' },
      { label: '活动总数', value: activities.value.length, hint: '当前平台活动池规模' },
      { label: '今日活动', value: todayActivities.value.length, hint: '今天开始或进行的活动' },
      { label: '未读消息', value: unreadCount.value, hint: '系统通知和平台消息' }
    ]
  }
  if (role.value === 'organizer') {
    return [
      { label: '进行中活动', value: ongoingActivities.value.length, hint: '需要持续关注现场进展' },
      { label: '今日活动', value: todayActivities.value.length, hint: '今天需要运营的活动' },
      { label: '可生成复盘', value: endedActivities.value.length, hint: '已结束活动可进入 AI 复盘' },
      { label: '未读消息', value: unreadCount.value, hint: '反馈和系统提醒' }
    ]
  }
  return [
    { label: '正在报名', value: registeringActivities.value.length, hint: '当前可直接报名的活动' },
    { label: '今日活动', value: todayActivities.value.length, hint: '今天可以关注的校园活动' },
    { label: '待签到', value: checkinActivities.value.length, hint: '进入签到时间的活动' },
    { label: '未读消息', value: unreadCount.value, hint: '活动通知和提醒' }
  ]
})

const tasks = computed(() => {
  if (isAdmin.value) {
    return [
      task('处理活动审核', `${pendingReviewActivities.value.length} 个活动等待审核`, '/admin/activity-reviews', '去审核', 'warning'),
      task('查看平台活动', `当前共 ${activities.value.length} 个活动`, '/admin/activities', '查看', 'success'),
      task('管理用户账号', '维护学生、负责人和管理员权限', '/admin/users', '管理', 'neutral')
    ]
  }
  if (role.value === 'organizer') {
    return [
      task('创建或完善活动', '使用 AI 生成文案，减少活动发布准备时间', '/admin/activities/create', '创建', 'success'),
      task('查看消息反馈', '集中处理学生提交的建议、问题和评价', '/admin/feedbacks', '处理', 'warning'),
      task('生成活动复盘', `${endedActivities.value.length} 个已结束活动可复盘`, '/admin/activities', '查看', 'neutral')
    ]
  }
  return [
    task('浏览可报名活动', `${registeringActivities.value.length} 个活动正在报名`, '/activities', '去看看', 'success'),
    task('处理待签到', `${checkinActivities.value.length} 个活动进入签到时间`, '/my-activities', '去签到', 'warning'),
    task('查看我的活动', '报名、签到、待反馈集中管理', '/my-activities', '进入', 'neutral')
  ]
})

const aiActions = computed(() => {
  if (isAdmin.value) {
    return [
      ai('平台数据洞察', '从数据中心查看活动治理趋势。', '/admin/dashboard', DataAnalysis),
      ai('审核风险提示', '优先处理待审核和异常活动。', '/admin/activity-reviews', Monitor),
      ai('系统通知管理', '向学生或负责人发送平台通知。', '/admin/notices/system', Message)
    ]
  }
  if (role.value === 'organizer') {
    return [
      ai('AI 生成活动文案', '创建活动时生成标题、简介和报名须知。', '/admin/activities/create', MagicStick),
      ai('AI 活动复盘', '活动结束后基于数据和反馈生成复盘。', '/admin/activities', DataAnalysis),
      ai('反馈聚合分析', '查看学生建议、问题反馈和活动评价。', '/admin/feedbacks', Message)
    ]
  }
  return [
    ai('AI 为我推荐', '根据校区、专业和参与记录推荐活动。', '/activities', MagicStick),
    ai('今日可参加', '优先查看报名中和今天开始的活动。', '/activities', Calendar),
    ai('成长记录', '查看报名、签到和活动成果。', '/profile', User)
  ]
})

const aiActionHint = computed(() => {
  if (role.value === 'organizer') return '活动创建、运营和复盘都可以用 AI 加速。'
  if (isAdmin.value) return 'AI 辅助发现治理线索，人工确认后再执行。'
  return '推荐只来自真实活动数据，不生成不存在的活动。'
})

const taskTitle = computed(() => {
  if (isAdmin.value) return '今日治理任务'
  if (role.value === 'organizer') return '今日运营任务'
  return '我的今日待办'
})
const primaryTaskPath = computed(() => isStudent.value ? '/my-activities' : '/admin/activities')
const activityListPath = computed(() => isStudent.value ? '/activities' : '/admin/activities')
const messagePath = computed(() => isStudent.value ? '/messages' : (isAdmin.value ? '/admin/notices/system' : '/admin/feedbacks'))
const activitySectionTitle = computed(() => isStudent.value ? '今日活动' : '近期活动运营状态')
const noticeHint = computed(() => isStudent.value ? '报名、签到、反馈和奖励通知会出现在这里。' : '运营反馈和系统消息会出现在这里。')

const focusActivities = computed(() => {
  const rows = todayActivities.value.length ? todayActivities.value : activities.value
  return rows.slice().sort(activitySort).slice(0, 5)
})

function task(title, description, to, action, tone) {
  return { title, description, to, action, tone }
}

function ai(title, description, to, icon) {
  return { title, description, to, icon }
}

function activitySort(a, b) {
  return statusWeight(a.status) - statusWeight(b.status)
    || new Date(a.startTime || 0) - new Date(b.startTime || 0)
}

function statusWeight(status) {
  const map = { REGISTERING: 1, WAITING_START: 2, ONGOING: 3, PENDING_REVIEW: 4, ENDED: 5, CANCELLED: 6 }
  return map[status] || 9
}

function isToday(value) {
  if (!value) return false
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return false
  const now = new Date()
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

function formatActivityMeta(activity) {
  const time = activity.startTime ? activity.startTime.replace('T', ' ').slice(0, 16) : '时间待定'
  const place = activity.activityMode === 'online' ? '线上活动' : (activity.location || activity.campus || '地点待定')
  return `${time} · ${place}`
}

function activityDetailPath(activity) {
  return isStudent.value ? `/activities/${activity.id}` : `/admin/activities/${activity.id}/stats`
}

async function loadDashboard() {
  loading.value = true
  try {
    const [activityRows, unread] = await Promise.all([
      listActivities().catch(() => []),
      getUnreadMessageCount().catch(() => 0)
    ])
    activities.value = Array.isArray(activityRows) ? activityRows : []
    unreadCount.value = Number(unread || 0)
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>
