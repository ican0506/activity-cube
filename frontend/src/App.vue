<template>
  <el-config-provider>
    <RouterView v-if="isLoginPage" />
    <div v-else class="app-shell">
      <aside class="sidebar">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">AC</span>
          <span>
            <strong>活动魔方</strong>
            <small>校园活动轻工具</small>
          </span>
        </RouterLink>
        <nav class="nav">
          <RouterLink to="/activities">活动大厅</RouterLink>
          <RouterLink v-if="userStore.isLogin" to="/my/registrations">我的报名</RouterLink>
          <RouterLink v-if="userStore.isLogin" to="/my/checkins">我的签到</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/dashboard">后台首页</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/activities">我的活动</RouterLink>
        </nav>
        <div class="user-box">
          <template v-if="userStore.isLogin">
            <div class="user-name">{{ userStore.userInfo.realName }}</div>
            <div class="user-meta">{{ userStore.userInfo.role }} · {{ userStore.userInfo.campus }}</div>
            <el-button size="small" @click="logout">退出登录</el-button>
          </template>
          <RouterLink v-else to="/login">
            <el-button type="primary" class="full">登录</el-button>
          </RouterLink>
        </div>
      </aside>
      <main class="main-panel">
        <RouterView />
      </main>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoginPage = computed(() => route.path === '/login')

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
