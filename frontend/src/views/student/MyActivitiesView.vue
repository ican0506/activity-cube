<template>
  <section class="my-activities-page">
    <div class="page-head compact-head">
      <div>
        <h1 class="page-title">我的活动</h1>
        <p class="page-subtitle">集中查看报名、签到、待反馈和已结束活动。</p>
      </div>
      <RouterLink to="/activities">
        <el-button type="primary">去活动大厅</el-button>
      </RouterLink>
    </div>

    <div class="panel my-activities-panel" v-loading="loading">
      <el-tabs v-model="activeTab" class="student-activity-tabs">
        <el-tab-pane label="我的报名" name="registrations">
          <el-table v-if="registrations.length" :data="registrations" stripe class="desktop-table">
            <el-table-column label="活动名称" min-width="190">
              <template #default="{ row }">{{ row.activityTitle || `活动 #${row.activityId}` }}</template>
            </el-table-column>
            <el-table-column prop="campus" label="校区" width="120" />
            <el-table-column prop="createdAt" label="报名时间" min-width="170" />
            <el-table-column label="操作" width="230">
              <template #default="{ row }">
                <RouterLink :to="`/activities/${row.activityId}`">
                  <el-button size="small">详情</el-button>
                </RouterLink>
                <el-tooltip v-if="!row.canCancel" :content="row.cancelReason || '当前不能取消报名'">
                  <span class="inline-action-wrap">
                    <el-button size="small" type="danger" plain disabled>取消报名</el-button>
                  </span>
                </el-tooltip>
                <el-button v-else size="small" type="danger" plain @click="cancel(row)">取消报名</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="registrations.length" class="mobile-card-list">
            <div v-for="row in registrations" :key="`reg-${row.id || row.activityId}`" class="simple-record-card">
              <div>
                <h3>{{ row.activityTitle || `活动 #${row.activityId}` }}</h3>
                <p>{{ row.campus || '校区待定' }} · {{ row.createdAt || '-' }}</p>
              </div>
              <div class="card-actions">
                <RouterLink :to="`/activities/${row.activityId}`">
                  <el-button size="small">详情</el-button>
                </RouterLink>
                <el-button v-if="row.canCancel" size="small" type="danger" plain @click="cancel(row)">取消报名</el-button>
                <el-tag v-else type="info">{{ row.cancelReason || '不可取消' }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-if="!loading && registrations.length === 0" description="暂无报名记录，去活动大厅看看吧。" />
        </el-tab-pane>

        <el-tab-pane label="我的签到" name="checkins">
          <el-table v-if="checkins.length" :data="checkins" stripe class="desktop-table">
            <el-table-column label="活动名称" min-width="190">
              <template #default="{ row }">{{ row.activityTitle || `活动 #${row.activityId}` }}</template>
            </el-table-column>
            <el-table-column prop="campus" label="校区" width="120" />
            <el-table-column label="签到方式" width="120">
              <template #default="{ row }">{{ checkinTypeText(row.checkinType) }}</template>
            </el-table-column>
            <el-table-column prop="checkinTime" label="签到时间" min-width="170" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <RouterLink :to="`/activities/${row.activityId}`">
                  <el-button size="small">详情</el-button>
                </RouterLink>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="checkins.length" class="mobile-card-list">
            <div v-for="row in checkins" :key="`checkin-${row.id || row.activityId}`" class="simple-record-card">
              <div>
                <h3>{{ row.activityTitle || `活动 #${row.activityId}` }}</h3>
                <p>{{ checkinTypeText(row.checkinType) }} · {{ row.checkinTime || '-' }}</p>
              </div>
              <RouterLink :to="`/activities/${row.activityId}`">
                <el-button size="small">详情</el-button>
              </RouterLink>
            </div>
          </div>
          <el-empty v-if="!loading && checkins.length === 0" description="暂无签到记录。" />
        </el-tab-pane>

        <el-tab-pane label="待反馈" name="feedback">
          <div v-if="pendingFeedbacks.length" class="todo-activity-list">
            <div v-for="item in pendingFeedbacks" :key="`feedback-${item.activityId || item.title}`" class="todo-activity-card">
              <div>
                <el-tag type="warning" size="small">待反馈</el-tag>
                <h3>{{ item.activityTitle || item.title || '待反馈活动' }}</h3>
                <p>{{ item.description || '活动已结束，欢迎提交活动反馈。' }}</p>
              </div>
              <RouterLink :to="item.targetPath || `/activities/${item.activityId}/feedback`">
                <el-button type="primary">去反馈</el-button>
              </RouterLink>
            </div>
          </div>
          <el-empty v-if="!loading && pendingFeedbacks.length === 0" description="暂无待反馈活动。" />
        </el-tab-pane>

        <el-tab-pane label="已结束" name="ended">
          <el-table v-if="endedActivities.length" :data="endedActivities" stripe class="desktop-table">
            <el-table-column prop="title" label="活动名称" min-width="190" />
            <el-table-column prop="activityCategory" label="活动类型" width="120" />
            <el-table-column label="是否签到" width="100">
              <template #default="{ row }">
                <el-tag :type="hasCheckedIn(row.id) ? 'success' : 'info'">{{ hasCheckedIn(row.id) ? '已签到' : '未签到' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否反馈" width="100">
              <template #default="{ row }">
                <el-tag :type="needsFeedback(row.id) ? 'warning' : 'success'">{{ needsFeedback(row.id) ? '待反馈' : '已反馈' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否获得奖励" width="130">
              <template #default="{ row }">
                <el-tag :type="rewardByActivityId(row.id) ? 'success' : 'info'">{{ rewardByActivityId(row.id) ? '已获得' : '未获得' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
                <RouterLink :to="`/activities/${row.id}`">
                  <el-button size="small">详情</el-button>
                </RouterLink>
                <RouterLink v-if="needsFeedback(row.id)" :to="`/activities/${row.id}/feedback`">
                  <el-button size="small" type="primary">反馈</el-button>
                </RouterLink>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="endedActivities.length" class="mobile-card-list">
            <div v-for="row in endedActivities" :key="`ended-${row.id}`" class="simple-record-card">
              <div>
                <h3>{{ row.title }}</h3>
                <p>{{ row.activityCategory || '其他' }} · {{ row.startTime || '时间待定' }}</p>
                <div class="status-row">
                  <el-tag :type="hasCheckedIn(row.id) ? 'success' : 'info'">{{ hasCheckedIn(row.id) ? '已签到' : '未签到' }}</el-tag>
                  <el-tag :type="needsFeedback(row.id) ? 'warning' : 'success'">{{ needsFeedback(row.id) ? '待反馈' : '已反馈' }}</el-tag>
                  <el-tag :type="rewardByActivityId(row.id) ? 'success' : 'info'">{{ rewardByActivityId(row.id) ? '已获奖励' : '未获奖励' }}</el-tag>
                </div>
              </div>
              <div class="card-actions">
                <RouterLink :to="`/activities/${row.id}`">
                  <el-button size="small">详情</el-button>
                </RouterLink>
                <RouterLink v-if="needsFeedback(row.id)" :to="`/activities/${row.id}/feedback`">
                  <el-button size="small" type="primary">反馈</el-button>
                </RouterLink>
              </div>
            </div>
          </div>
          <el-empty v-if="!loading && endedActivities.length === 0" description="暂无已结束活动。" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listActivities } from '../../api/activity'
import { myCheckins } from '../../api/checkin'
import { cancelMyRegistration, myRegistrations } from '../../api/registration'
import { listMyRewards } from '../../api/reward'
import { getStudentProfileTodos } from '../../api/studentProfile'

const activeTab = ref('registrations')
const loading = ref(false)
const registrations = ref([])
const checkins = ref([])
const todos = ref([])
const rewards = ref([])
const endedSource = ref([])

const pendingFeedbacks = computed(() => todos.value.filter((item) => item.type === 'feedback'))
const registeredActivityIds = computed(() => new Set(registrations.value.map((item) => Number(item.activityId)).filter(Boolean)))
const checkedActivityIds = computed(() => new Set(checkins.value.map((item) => Number(item.activityId)).filter(Boolean)))
const rewardActivityIds = computed(() => new Set(rewards.value.map((item) => Number(item.activityId)).filter(Boolean)))
const feedbackActivityIds = computed(() => new Set(pendingFeedbacks.value.map((item) => Number(item.activityId)).filter(Boolean)))

const endedActivities = computed(() => {
  return endedSource.value.filter((activity) => {
    const id = Number(activity.id)
    return registeredActivityIds.value.has(id)
      || checkedActivityIds.value.has(id)
      || rewardActivityIds.value.has(id)
  })
})

async function load() {
  loading.value = true
  try {
    const [registrationData, checkinData, todoData, rewardData, endedData] = await Promise.all([
      myRegistrations(),
      myCheckins(),
      getStudentProfileTodos().catch(() => []),
      listMyRewards().catch(() => []),
      listActivities({ status: 'ENDED' }).catch(() => [])
    ])
    registrations.value = registrationData || []
    checkins.value = checkinData || []
    todos.value = todoData || []
    rewards.value = rewardData || []
    endedSource.value = endedData || []
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认取消“${row.activityTitle || '该活动'}”的报名吗？取消后需要重新报名。`, '取消报名确认', {
    confirmButtonText: '确认取消',
    cancelButtonText: '暂不取消',
    type: 'warning'
  })
  await cancelMyRegistration(row.activityId)
  ElMessage.success('报名已取消')
  await load()
}

function checkinTypeText(type) {
  const map = {
    online: '线上签到',
    qr: '扫码签到',
    manual: '人工补签'
  }
  return map[type] || '扫码签到'
}

function hasCheckedIn(activityId) {
  return checkedActivityIds.value.has(Number(activityId))
}

function needsFeedback(activityId) {
  return feedbackActivityIds.value.has(Number(activityId))
}

function rewardByActivityId(activityId) {
  return rewards.value.find((item) => Number(item.activityId) === Number(activityId))
}

onMounted(load)
</script>

<style scoped>
.my-activities-page {
  display: grid;
  gap: 18px;
}

.compact-head {
  align-items: center;
}

.my-activities-panel {
  overflow: hidden;
}

.student-activity-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.student-activity-tabs :deep(.el-tabs__item) {
  font-weight: 800;
}

.inline-action-wrap {
  display: inline-block;
  margin-left: 8px;
}

.mobile-card-list {
  display: none;
}

.simple-record-card,
.todo-activity-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 15px;
  border: 1px solid rgba(22, 160, 133, 0.12);
  border-radius: 14px;
  background: #f7fcf9;
}

.simple-record-card h3,
.todo-activity-card h3 {
  margin: 0 0 6px;
  color: var(--ac-ink);
  font-size: 17px;
}

.simple-record-card p,
.todo-activity-card p {
  margin: 0;
  color: var(--ac-muted);
  line-height: 1.6;
}

.todo-activity-list {
  display: grid;
  gap: 12px;
}

.todo-activity-card h3 {
  margin-top: 8px;
}

.card-actions,
.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 720px) {
  .compact-head {
    display: grid;
    gap: 12px;
  }

  .compact-head .el-button {
    width: 100%;
  }

  .desktop-table {
    display: none;
  }

  .mobile-card-list {
    display: grid;
    gap: 10px;
  }

  .simple-record-card,
  .todo-activity-card {
    display: grid;
    align-items: stretch;
  }

  .card-actions .el-button,
  .todo-activity-card .el-button {
    width: 100%;
  }
}
</style>
