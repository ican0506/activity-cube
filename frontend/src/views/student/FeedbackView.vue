<template>
  <section class="panel form-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">活动反馈</h1>
        <p class="page-subtitle">反馈属于后续扩展功能，本页提供基础提交闭环。</p>
      </div>
    </div>
    <el-form :model="form" label-width="80px">
      <el-form-item label="评分">
        <el-rate v-model="form.rating" />
      </el-form-item>
      <el-form-item label="建议">
        <el-input v-model="form.content" type="textarea" :rows="5" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">提交反馈</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitFeedback } from '../../api/feedback'

const route = useRoute()
const router = useRouter()
const form = reactive({ rating: 5, content: '' })

async function submit() {
  await submitFeedback(route.params.id, form)
  ElMessage.success('反馈已提交')
  router.push(`/activities/${route.params.id}`)
}
</script>
