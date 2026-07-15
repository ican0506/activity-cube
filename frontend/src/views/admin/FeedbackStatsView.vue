<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">反馈统计</h1>
        <p class="page-subtitle">查看活动反馈列表。</p>
      </div>
    </div>
    <div class="panel">
      <el-table :data="rows" stripe>
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="rating" label="评分" width="120" />
        <el-table-column prop="content" label="反馈内容" />
        <el-table-column prop="createdAt" label="提交时间" width="180" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { listFeedbacks } from '../../api/feedback'

const route = useRoute()
const rows = ref([])
onMounted(async () => {
  rows.value = await listFeedbacks(route.params.id)
})
</script>
