<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">管理端</span>
        <h1>活动管理</h1>
        <p>管理活动发布、二维码、名单、统计和工具入口。</p>
      </div>
      <div class="button-row">
        <RouterLink v-if="userStore.canAdmin" to="/admin/activity-reviews"><el-button>待审核活动</el-button></RouterLink>
        <RouterLink to="/admin/activities/create"><el-button type="primary" :icon="Plus">创建活动</el-button></RouterLink>
      </div>
    </div>

    <div class="metric-row campus-overview">
      <div class="metric metric-accent"><span>活动总数</span><strong>{{ managedRows.length }}</strong></div>
      <div class="metric"><span>报名总数</span><strong>{{ overview.registrationTotal }}</strong></div>
      <div class="metric"><span>签到总数</span><strong>{{ overview.checkinTotal }}</strong></div>
      <div class="metric"><span>反馈数量</span><strong>{{ overview.feedbackTotal }}</strong></div>
    </div>

    <div class="panel admin-table-panel" v-loading="loading">
      <div class="section-title">
        <div>
          <h2>活动列表</h2>
          <p>保持发布、名单、数据和工具入口清晰可达，适合日常活动负责人快速操作。</p>
        </div>
      </div>

      <el-table class="desktop-table" :data="managedRows" stripe>
        <el-table-column prop="title" label="活动名称" min-width="220" />
        <el-table-column prop="campus" label="校区" width="130">
          <template #default="{ row }">
            <span class="wheat-badge">{{ row.campus || '全校区' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewStatus" label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.reviewStatus || row.status)">{{ statusText(row.reviewStatus || row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="160">
          <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column label="常用操作" width="260">
          <template #default="{ row }">
            <div class="table-actions action-cluster">
              <RouterLink :to="`/admin/activities/${row.id}/edit`">
                <el-button size="small" :icon="Edit">编辑</el-button>
              </RouterLink>
              <RouterLink :to="`/admin/activities/${row.id}/qrcodes`">
                <el-button size="small" type="primary" :icon="Tickets">二维码</el-button>
              </RouterLink>
              <RouterLink :to="`/admin/activities/${row.id}/stats`">
                <el-button size="small">统计</el-button>
              </RouterLink>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="名单与工具" width="280" fixed="right">
          <template #default="{ row }">
            <div class="table-actions action-cluster">
              <el-dropdown trigger="click" @command="(command) => handleCommand(command, row)">
                <el-button size="small" :icon="MoreFilled">更多入口</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="registrations">报名名单</el-dropdown-item>
                    <el-dropdown-item command="checkins">签到名单</el-dropdown-item>
                    <el-dropdown-item command="notices">活动通知</el-dropdown-item>
                    <el-dropdown-item command="lottery">随机抽奖</el-dropdown-item>
                    <el-dropdown-item command="tools">抽签分组</el-dropdown-item>
                    <el-dropdown-item command="rewards">发放奖励</el-dropdown-item>
                    <el-dropdown-item command="feedbacks">反馈统计</el-dropdown-item>
                    <el-dropdown-item divided command="delete">删除活动</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="mobile-card-list">
        <div v-for="row in managedRows" :key="row.id" class="panel manage-card agri-card">
          <div class="manage-card-head">
            <div>
              <span class="wheat-badge">{{ row.campus || '全校区' }}</span>
              <h3>{{ row.title }}</h3>
              <p class="page-subtitle">{{ row.startTime || '时间待定' }}</p>
              <p v-if="row.rejectReason" class="page-subtitle">驳回原因：{{ row.rejectReason }}</p>
            </div>
            <el-tag :type="statusTagType(row.reviewStatus || row.status)">{{ statusText(row.reviewStatus || row.status) }}</el-tag>
          </div>
          <div class="button-row">
            <RouterLink :to="`/admin/activities/${row.id}/edit`"><el-button size="small">编辑</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/qrcodes`"><el-button size="small" type="primary">二维码</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/registrations`"><el-button size="small">报名</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/checkins`"><el-button size="small">签到</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/notices`"><el-button size="small">通知</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/lottery`"><el-button size="small">抽奖</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/tools`"><el-button size="small">分组</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/feedbacks`"><el-button size="small">反馈</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/stats`"><el-button size="small">统计</el-button></RouterLink>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && managedRows.length === 0" class="empty-wrap" description="暂无可管理活动，请先创建活动" />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, MoreFilled, Plus, Tickets } from '@element-plus/icons-vue'
import { deleteActivity, listActivities } from '../../api/activity'
import { listCheckins } from '../../api/checkin'
import { listFeedbacks } from '../../api/feedback'
import { listRegistrations } from '../../api/registration'
import { issueActivityRewards } from '../../api/reward'
import { useUserStore } from '../../stores/user'
import { statusTagType, statusText } from '../../utils/options'

const router = useRouter()
const userStore = useUserStore()
const rows = ref([])
const loading = ref(false)
const overview = reactive({
  registrationTotal: 0,
  checkinTotal: 0,
  feedbackTotal: 0
})

const managedRows = computed(() => {
  if (userStore.role === 'admin') return rows.value
  return rows.value.filter((item) => item.creatorId === userStore.userInfo?.id)
})
const activityIds = computed(() => managedRows.value.map((item) => item.id).filter(Boolean))

async function load() {
  loading.value = true
  try {
    rows.value = await listActivities({})
    await loadOverview()
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  overview.registrationTotal = 0
  overview.checkinTotal = 0
  overview.feedbackTotal = 0
  if (activityIds.value.length === 0) return

  const [registrations, checkins, feedbacks] = await Promise.all([
    sumListCount(listRegistrations),
    sumListCount(listCheckins),
    sumListCount(listFeedbacks)
  ])
  overview.registrationTotal = registrations
  overview.checkinTotal = checkins
  overview.feedbackTotal = feedbacks
}

async function sumListCount(fetcher) {
  const results = await Promise.allSettled(activityIds.value.map((id) => fetcher(id)))
  return results.reduce((total, item) => total + (item.status === 'fulfilled' ? item.value.length : 0), 0)
}

async function handleCommand(command, row) {
  if (command === 'delete') {
    await remove(row.id)
    return
  }
  if (command === 'rewards') {
    await issueRewards(row)
    return
  }
  router.push(`/admin/activities/${row.id}/${command}`)
}

async function issueRewards(row) {
  await ElMessageBox.confirm('确认给该活动已签到学生发放奖励？未签到学生不会获得奖励。', '发放奖励')
  const issued = await issueActivityRewards(row.id)
  ElMessage.success(`已发放 ${issued.length} 条活动奖励`)
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该活动？', '删除确认')
  await deleteActivity(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
