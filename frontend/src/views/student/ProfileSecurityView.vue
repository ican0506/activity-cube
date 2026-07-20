<template>
  <section class="security-page">
    <div class="security-hero campus-hero">
      <div>
        <span class="motto-badge">账号安全</span>
        <h1>修改登录密码</h1>
        <p>修改成功后会清除当前登录状态，请使用新密码重新登录。</p>
      </div>
      <RouterLink to="/profile">
        <el-button class="hero-button">返回个人中心</el-button>
      </RouterLink>
    </div>

    <div class="panel security-panel">
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="不少于 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-button type="primary" class="full" :loading="submitting" @click="submit">确认修改</el-button>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeStudentPassword } from '../../api/studentProfile'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function submit() {
  if (!form.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!form.newPassword || form.newPassword.length < 6) {
    ElMessage.warning('新密码不能少于 6 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await changeStudentPassword(form)
    await ElMessageBox.alert('密码修改成功，请重新登录。', '账号安全', { confirmButtonText: '去登录' })
    userStore.logout()
    router.push('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.security-page {
  display: grid;
  gap: 18px;
}

.security-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  min-height: 210px;
  padding: clamp(22px, 4vw, 38px);
  border-radius: 24px;
  color: #fff;
}

.security-hero h1 {
  margin: 0 0 10px;
  font-size: clamp(32px, 5vw, 52px);
  line-height: 1.06;
}

.security-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
}

.security-panel {
  width: min(560px, 100%);
}

@media (max-width: 560px) {
  .security-hero {
    display: grid;
    border-radius: 16px;
  }

  .security-hero .el-button {
    width: 100%;
  }
}
</style>
