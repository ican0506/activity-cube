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
          <RouterLink v-if="userStore.isLogin" to="/scan">扫一扫</RouterLink>
          <el-badge v-if="userStore.isLogin" class="nav-badge" :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <RouterLink to="/messages">消息中心</RouterLink>
          </el-badge>
          <RouterLink v-if="userStore.isLogin" to="/my/registrations">我的报名</RouterLink>
          <RouterLink v-if="userStore.isLogin" to="/my/checkins">我的签到</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/dashboard">数据总览</RouterLink>
          <RouterLink v-if="userStore.canManage" to="/admin/activities">活动管理</RouterLink>
          <RouterLink v-if="userStore.canAdmin" to="/admin/activity-reviews">活动审核</RouterLink>
          <RouterLink v-if="userStore.canAdmin" to="/admin/notices/system">系统通知</RouterLink>
          <RouterLink v-if="userStore.canAdmin" to="/admin/users">用户管理</RouterLink>
          <RouterLink v-if="userStore.canAdmin" to="/admin/operation-logs">操作日志</RouterLink>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { userRoleText } from './utils/options'
import { getUnreadMessageCount } from './api/message'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const unreadCount = ref(0)

async function loadUnreadCount() {
  if (!userStore.isLogin) {
    unreadCount.value = 0
    return
  }
  try {
    unreadCount.value = await getUnreadMessageCount()
  } catch {
    unreadCount.value = 0
  }
}

function logout() {
  userStore.logout()
  unreadCount.value = 0
  router.push('/login')
}

watch(() => route.fullPath, loadUnreadCount)
watch(() => userStore.isLogin, loadUnreadCount)
onMounted(loadUnreadCount)
</script>
