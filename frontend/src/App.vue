<template>
  <el-config-provider>
    <RouterView v-if="isAuthPage" />
    <div v-else class="app-shell">
      <header class="topbar">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">
            <img :src="schoolEmblem" alt="河南农业大学校徽" />
          </span>
          <span class="brand-copy">
            <strong class="brand-school-name">河南农业大学</strong>
            <small>校园活动轻工具平台</small>
          </span>
        </RouterLink>

        <nav class="nav">
          <template v-for="item in navItems" :key="item.label">
            <el-badge v-if="item.badge === 'messages'" class="nav-badge" :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <RouterLink :to="item.to">{{ item.label }}</RouterLink>
            </el-badge>
            <RouterLink v-else :to="item.to">{{ item.label }}</RouterLink>
          </template>
        </nav>

        <div class="user-box">
          <template v-if="userStore.isLogin">
            <el-tooltip v-if="showScanShortcut" content="扫一扫" placement="bottom">
              <el-button
                class="scan-shortcut-button"
                :icon="Camera"
                circle
                aria-label="扫一扫"
                @click="goScan"
              />
            </el-tooltip>
            <el-dropdown trigger="click" class="user-dropdown" @command="handleUserCommand">
              <button class="user-menu-trigger" type="button">
                <el-avatar :size="34" :src="currentAvatarUrl" class="top-avatar">
                  {{ userInitial }}
                </el-avatar>
                <span class="user-menu-name">{{ displayName }}</span>
                <span class="user-menu-arrow">⌄</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <template v-if="isStudent">
                    <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="/profile/security">账号安全</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </template>
                  <template v-else>
                    <el-dropdown-item :command="defaultManageTarget">进入工作台</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/user'
import { navItemsForRole, shouldShowScanShortcut } from './utils/navigation'
import { getUnreadMessageCount } from './api/message'
import { resolveFileUrl } from './api/file'
import { defaultTargetForRole } from './utils/authSession'
import { Camera } from '@element-plus/icons-vue'
import schoolEmblem from './assets/school-emblem-clean.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')
const unreadCount = ref(0)
const navItems = computed(() => {
  return userStore.isLogin ? navItemsForRole(userStore.role) : [{ label: '活动大厅', to: '/activities' }]
})
const isStudent = computed(() => ['student', 'user'].includes(userStore.role))
const showScanShortcut = computed(() => shouldShowScanShortcut(userStore.role))
const displayName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '用户')
const userInitial = computed(() => displayName.value.slice(0, 1))
const currentAvatarUrl = computed(() => resolveFileUrl(userStore.userInfo?.avatarUrl))
const defaultManageTarget = computed(() => defaultTargetForRole(userStore.role))

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

function handleUserCommand(command) {
  if (command === 'logout') {
    logout()
    return
  }
  if (command) {
    router.push(command)
  }
}

function goScan() {
  router.push('/scan')
}

watch(() => route.fullPath, loadUnreadCount)
watch(() => userStore.isLogin, loadUnreadCount)
onMounted(() => {
  loadUnreadCount()
  window.addEventListener('activity-cube:messages-updated', loadUnreadCount)
})
onBeforeUnmount(() => {
  window.removeEventListener('activity-cube:messages-updated', loadUnreadCount)
})
</script>
