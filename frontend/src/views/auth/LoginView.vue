<template>
  <section class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="brand-mark">AC</span>
        <div>
          <h1>活动魔方</h1>
          <p>登录后进入河南农业大学校园活动平台</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号 / 学号 / 工号">
          <el-input v-model="form.username" placeholder="学生请输入学号，负责人/管理员请输入账号或工号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password size="large" />
        </el-form-item>
        <el-button type="primary" class="full" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="quick-login">
        <el-button @click="fill('2321241389')">学生账号</el-button>
        <el-button @click="fill('T2024001')">负责人工号</el-button>
        <el-button @click="fill('admin')">管理员账号</el-button>
      </div>

      <p class="auth-switch">
        还没有账号？
        <RouterLink to="/register">注册学生账号</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { defaultTargetForRole, normalizeLoginTarget } from '../../utils/authSession'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '123456' })

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(normalizeLoginTarget(route.query.redirect, defaultTargetForRole(userStore.role)))
  } finally {
    loading.value = false
  }
}
</script>
