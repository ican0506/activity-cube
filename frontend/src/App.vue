<template>
  <el-config-provider>
    <RouterView v-if="isAuthPage" />
    <div v-else class="app-shell">
      <header class="topbar">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">AC</span>
          <span class="brand-copy">
            <strong>活动魔方</strong>
            <small>校园活动轻工具平台</small>
          </span>
        </RouterLink>

        <nav class="nav">
          <RouterLink to="/activities">活动大厅</RouterLink>
          <RouterLink v-if="userStore.isLogin" to="/my/registrations">我的报名</RouterLink>
          <RouterLink v-if="userStore.isLogin" to="/my/checkins">我的签到</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/dashboard">数据总览</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/activities">活动管理</RouterLink>
          <RouterLink v-if="userStore.canAdmin" to="/admin/users">用户管理</RouterLink>
        </nav>

        <div class="user-box">
          <template v-if="userStore.isLogin">
            <div class="user-profile">
              <div class="user-name">{{ userStore.userInfo.realName || userStore.userInfo.username }}</div>
              <div class="user-meta">{{ userRoleText(userStore.userInfo.role) }} · {{ userStore.userInfo.campus }}</div>
            </div>
            <el-button size="small" plain @click="logout">退出</el-button>
          </template>
          <RouterLink v-else to="/login">
            <el-button type="primary" class="full">登录</el-button>
          </RouterLink>
        </div>
      </header>

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
import { userRoleText } from './utils/options'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
