<template>
  <section>
    <div class="admin-intro">
      <div>
        <h1>我的活动</h1>
        <p>集中管理活动发布、报名名单、签到工具和数据统计。</p>
      </div>
      <RouterLink to="/admin/activities/create">
        <el-button class="hero-button" :icon="Plus">创建活动</el-button>
      </RouterLink>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>活动总数</span><strong>{{ rows.length }}</strong></div>
      <div class="metric"><span>报名中</span><strong>{{ registeringCount }}</strong></div>
      <div class="metric"><span>进行中</span><strong>{{ ongoingCount }}</strong></div>
      <div class="metric"><span>已结束</span><strong>{{ endedCount }}</strong></div>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="title" label="活动名称" min-width="180" />
        <el-table-column prop="campus" label="校区" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column label="操作" width="870" fixed="right">
          <template #default="{ row }">
            <RouterLink :to="`/admin/activities/${row.id}/edit`"><el-button size="small" :icon="Edit">编辑</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/qrcodes`"><el-button size="small" :icon="Tickets">二维码</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/registrations`"><el-button size="small" :icon="User">报名</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/checkins`"><el-button size="small" :icon="Checked">签到</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/lottery`"><el-button size="small" :icon="MagicStick">抽奖</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/tools`"><el-button size="small" :icon="Tools">工具</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/feedbacks`"><el-button size="small" :icon="ChatDotRound">反馈</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/stats`"><el-button size="small" :icon="DataAnalysis">统计</el-button></RouterLink>
            <el-button size="small" type="danger" :icon="Delete" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" class="empty-wrap" description="暂无活动，请先创建活动" />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ChatDotRound, Checked, DataAnalysis, Delete, Edit, MagicStick, Plus, Tickets, Tools, User } from '@element-plus/icons-vue'
import { deleteActivity, listActivities } from '../../api/activity'
import { statusText } from '../../utils/options'

const rows = ref([])
const loading = ref(false)
const registeringCount = computed(() => rows.value.filter((item) => item.status === 'REGISTERING').length)
const ongoingCount = computed(() => rows.value.filter((item) => item.status === 'ONGOING').length)
const endedCount = computed(() => rows.value.filter((item) => item.status === 'ENDED').length)

async function load() {
  loading.value = true
  try {
    rows.value = await listActivities({})
  } finally {
    loading.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该活动？', '删除确认')
  await deleteActivity(id)
  ElMessage.success('已删除')
  load()
}

function statusTagType(status) {
  const map = {
    REGISTERING: 'success',
    ONGOING: 'warning',
    ENDED: 'info',
    CANCELLED: 'danger',
    DRAFT: ''
  }
  return map[status] || ''
}

onMounted(load)
</script>
