<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">我的报名</h1>
        <p class="page-subtitle">查看当前账号提交过的活动报名记录。</p>
      </div>
      <RouterLink to="/activities">
        <el-button type="primary">去活动大厅</el-button>
      </RouterLink>
    </div>
    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="activityTitle" label="活动名称" min-width="180">
          <template #default="{ row }">{{ row.activityTitle || `活动 #${row.activityId}` }}</template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="campus" label="校区" />
        <el-table-column prop="createdAt" label="报名时间" min-width="170" />
        <el-table-column label="操作" min-width="260">
          <template #default="{ row }">
            <RouterLink :to="`/activities/${row.activityId}`">
              <el-button size="small">详情</el-button>
            </RouterLink>
            <el-tooltip v-if="!row.canCancel" :content="row.cancelReason || '当前不能取消报名'">
              <span class="cancel-action-wrap">
                <el-button size="small" type="danger" plain disabled>取消报名</el-button>
              </span>
            </el-tooltip>
            <el-button v-else size="small" type="danger" plain @click="cancel(row)">取消报名</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无报名记录" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelMyRegistration, myRegistrations } from '../../api/registration'

const rows = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    rows.value = await myRegistrations()
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  await ElMessageBox.confirm(`确认取消“${row.activityTitle || '该活动'}”的报名吗？取消后需重新报名。`, '取消报名确认', {
    confirmButtonText: '确认取消',
    cancelButtonText: '暂不取消',
    type: 'warning'
  })
  await cancelMyRegistration(row.activityId)
  ElMessage.success('报名已取消')
  await load()
}

onMounted(load)
</script>

<style scoped>
.cancel-action-wrap {
  display: inline-block;
  margin-left: 8px;
}
</style>
