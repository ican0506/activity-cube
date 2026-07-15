import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, me, register as registerApi } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('activity_cube_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('activity_cube_user') || 'null'))

  const isLogin = computed(() => Boolean(token.value))
  const role = computed(() => userInfo.value?.role || '')
  const campus = computed(() => userInfo.value?.campus || '')
  const canManage = computed(() => ['organizer', 'admin'].includes(role.value))

  function persist(nextToken, nextUser) {
    token.value = nextToken
    userInfo.value = nextUser
    localStorage.setItem('activity_cube_token', nextToken)
    localStorage.setItem('activity_cube_user', JSON.stringify(nextUser))
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
    localStorage.setItem('activity_cube_user', JSON.stringify(user))
    return user
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('activity_cube_token')
    localStorage.removeItem('activity_cube_user')
  }

  return { token, userInfo, isLogin, role, campus, canManage, login, register, refresh, logout }
})
