<template>
  <section class="login-page">
    <div class="login-card register-card">
      <div class="login-brand">
        <span class="brand-mark">AC</span>
        <div>
          <h1>注册账号</h1>
          <p>注册后自动以学生身份进入系统</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入登录账号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password size="large" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" size="large" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" size="large" />
        </el-form-item>
        <el-form-item label="所在校区">
          <el-select v-model="form.campus" class="full" size="large">
            <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" size="large" />
        </el-form-item>
        <el-form-item label="专业班级">
          <el-input v-model="form.majorClass" size="large" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" size="large" />
        </el-form-item>
        <el-button type="primary" class="full" size="large" :loading="loading" @click="submit">
          注册并登录
        </el-button>
      </el-form>

      <p class="auth-switch">
        已有账号？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { userCampuses } from '../../utils/options'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  studentNo: '',
  campus: '龙子湖校区',
  college: '',
  majorClass: '',
  phone: ''
})

async function submit() {
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功')
    router.push('/activities')
  } finally {
    loading.value = false
  }
}
</script>
