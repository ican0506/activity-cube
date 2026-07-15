<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">我的报名</h1>
        <p class="page-subtitle">查看当前账号提交过的活动报名记录。</p>
      </div>
    </div>
    <div class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="activityId" label="活动ID" width="100" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="campus" label="校区" />
        <el-table-column prop="createdAt" label="报名时间" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <RouterLink :to="`/activities/${row.activityId}`">
              <el-button size="small">详情</el-button>
            </RouterLink>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myRegistrations } from '../../api/registration'

const rows = ref([])
onMounted(async () => {
  rows.value = await myRegistrations()
})
</script>
