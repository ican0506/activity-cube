<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">我的活动</h1>
        <p class="page-subtitle">管理活动、查看报名签到、进入工具和统计。</p>
      </div>
      <RouterLink to="/admin/activities/create">
        <el-button type="primary">创建活动</el-button>
      </RouterLink>
    </div>
    <div class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="title" label="活动名称" min-width="180" />
        <el-table-column prop="campus" label="校区" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">{{ statusText(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column label="操作" width="520">
          <template #default="{ row }">
            <RouterLink :to="`/admin/activities/${row.id}/edit`"><el-button size="small">编辑</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/registrations`"><el-button size="small">报名</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/checkins`"><el-button size="small">签到</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/tools`"><el-button size="small">工具</el-button></RouterLink>
            <RouterLink :to="`/admin/activities/${row.id}/stats`"><el-button size="small">统计</el-button></RouterLink>
            <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { deleteActivity, listActivities } from '../../api/activity'
import { statusText } from '../../utils/options'

const rows = ref([])

async function load() {
  rows.value = await listActivities({})
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该活动？', '删除确认')
  await deleteActivity(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
