import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, me, register as registerApi } from '../api/auth'
import { clearAuthSession, getStoredToken, getStoredUser, persistAuthSession } from '../utils/authSession'

export const useUserStore = defineStore('user', () => {
  const token = ref(getStoredToken(localStorage))
  const userInfo = ref(getStoredUser(localStorage))

  const isLogin = computed(() => Boolean(token.value))
  const role = computed(() => userInfo.value?.role || '')
  const campus = computed(() => userInfo.value?.campus || '')
  const canManage = computed(() => ['organizer', 'admin'].includes(role.value))
  const canAdmin = computed(() => role.value === 'admin')

  function persist(nextToken, nextUser) {
    token.value = nextToken
    userInfo.value = nextUser
    persistAuthSession(localStorage, nextToken, nextUser)
  }

  async function login(payload) {
    const data = await loginApi(payload)
    persist(data.token, data.user)
    return data
  }

  async function register(payload) {
    const data = await registerApi(payload)
    persist(data.token, data.user)
    return data
  }

  async function refresh() {
    if (!token.value) return null
    const user = await me()
    userInfo.value = user
    persistAuthSession(localStorage, token.value, user)
    return user
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    clearAuthSession(localStorage)
  }

  return { token, userInfo, isLogin, role, campus, canManage, canAdmin, login, register, refresh, logout }
})
