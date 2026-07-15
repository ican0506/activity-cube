<template>
  <section class="panel form-narrow" v-loading="loading">
    <div class="page-head">
      <div>
        <h1 class="page-title">活动反馈</h1>
        <p class="page-subtitle">{{ activity?.title || '分享你的活动体验，帮助活动持续优化。' }}</p>
      </div>
    </div>

    <el-form :model="form" label-position="top">
      <el-form-item label="满意度评分">
        <div class="feedback-rate">
          <el-rate v-model="form.score" :max="5" />
          <span>{{ form.score }} 分</span>
        </div>
      </el-form-item>
      <el-form-item label="活动体验">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="可以写下活动流程、现场体验、组织服务等感受"
        />
      </el-form-item>
      <el-form-item label="改进建议">
        <el-input
          v-model="form.suggestion"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="例如时间安排、场地、通知、互动形式等建议"
        />
      </el-form-item>
      <el-form-item label="是否匿名">
        <el-switch v-model="form.anonymous" active-text="匿名提交" inactive-text="实名提交" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交反馈</el-button>
        <RouterLink :to="`/activities/${route.params.id}`">
          <el-button>返回详情</el-button>
        </RouterLink>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { submitFeedback } from '../../api/feedback'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)
const form = reactive({
  score: 5,
  content: '',
  suggestion: '',
  anonymous: false
})

async function submit() {
  submitting.value = true
  try {
    await submitFeedback(route.params.id, form)
    ElMessage.success('反馈提交成功，感谢你的参与。')
    router.push(`/activities/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const detail = await getActivity(route.params.id)
    activity.value = detail.activity
  } finally {
    loading.value = false
  }
})
</script>
