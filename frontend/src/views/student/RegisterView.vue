<template>
  <section>
    <div class="hero">
      <div>
        <h1>活动报名</h1>
        <p>确认个人信息后提交报名。同一活动只能报名一次，请确保手机号和校区信息准确。</p>
      </div>
      <div v-if="activity" class="hero-card">
        <span>{{ activity.campus }}</span>
        <strong>{{ activity.title }}</strong>
        <p>{{ activity.location || '地点待定' }}</p>
      </div>
    </div>

    <div class="panel form-narrow" v-loading="loading">
      <div class="page-head">
        <div>
          <h2 class="page-title">报名信息</h2>
          <p class="page-subtitle">系统已自动带入登录用户资料，可按实际情况补充修改。</p>
        </div>
      </div>
      <el-form :model="form" label-width="96px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
        <el-form-item label="专业班级"><el-input v-model="form.majorClass" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="所在校区">
          <el-select v-model="form.campus" class="full">
            <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="4" placeholder="可填写社团、特殊说明或联系偏好" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交报名</el-button>
          <RouterLink :to="`/activities/${route.params.id}`">
            <el-button>返回详情</el-button>
          </RouterLink>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { registerActivity } from '../../api/registration'
import { useUserStore } from '../../stores/user'
import { userCampuses } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = userStore.userInfo || {}
const form = reactive({
  name: user.realName || '',
  studentNo: user.studentNo || '',
  college: user.college || '',
  majorClass: user.majorClass || '',
  phone: user.phone || '',
  campus: user.campus || '龙子湖校区',
  remark: ''
})
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)

async function loadActivity() {
  loading.value = true
  try {
    const detail = await getActivity(route.params.id)
    activity.value = detail.activity
  } finally {
    loading.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    await registerActivity(route.params.id, form)
    ElMessage.success('报名成功')
    router.push(`/activities/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(loadActivity)
</script>
