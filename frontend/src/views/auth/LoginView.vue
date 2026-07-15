<template>
  <section class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="brand-mark">AC</span>
        <div>
          <h1>活动魔方</h1>
          <p>登录后进入校园活动组织平台</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="student001 / organizer001 / admin" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password size="large" />
        </el-form-item>
        <el-button type="primary" class="full" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="quick-login">
        <el-button @click="fill('student001')">学生账号</el-button>
        <el-button @click="fill('organizer001')">负责人账号</el-button>
        <el-button @click="fill('admin')">管理员账号</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'organizer001', password: '123456' })

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/activities')
  } finally {
    loading.value = false
  }
}
</script>
