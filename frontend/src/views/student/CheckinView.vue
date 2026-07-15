<template>
  <section class="panel form-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">活动签到</h1>
        <p class="page-subtitle">已报名学生可以在这里确认签到。</p>
      </div>
    </div>
    <el-result icon="success" title="扫码签到入口" sub-title="确认后将写入签到记录，重复签到会被后端拦截。">
      <template #extra>
        <el-button type="primary" @click="submit">确认签到</el-button>
      </template>
    </el-result>
  </section>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { checkinActivity } from '../../api/checkin'

const route = useRoute()
const router = useRouter()

async function submit() {
  await checkinActivity(route.params.id)
  ElMessage.success('签到成功')
  router.push(`/activities/${route.params.id}`)
}
</script>
