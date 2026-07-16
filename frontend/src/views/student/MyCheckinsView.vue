<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">我的签到</h1>
        <p class="page-subtitle">查看当前账号已经完成的签到记录。</p>
      </div>
      <RouterLink to="/activities">
        <el-button type="primary">去活动大厅</el-button>
      </RouterLink>
    </div>
    <div class="panel" v-loading="loading">
      <el-table :data="rows" stripe>
        <el-table-column prop="activityId" label="活动ID" width="100" />
        <el-table-column prop="campus" label="校区" />
        <el-table-column prop="checkinTime" label="签到时间" min-width="170" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <RouterLink :to="`/activities/${row.activityId}`">
              <el-button size="small">详情</el-button>
            </RouterLink>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无签到记录" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myCheckins } from '../../api/checkin'

const rows = ref([])
const loading = ref(false)
onMounted(async () => {
  loading.value = true
  try {
    rows.value = await myCheckins()
  } finally {
    loading.value = false
  }
})
</script>
