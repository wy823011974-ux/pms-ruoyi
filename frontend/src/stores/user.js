import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('pms_token') || '')
  const userInfo = ref(null)

  const isAdmin = computed(() => userInfo.value && (userInfo.value.role === 'super_admin' || userInfo.value.role === 'admin'))

  async function doLogin(account, password) {
    const res = await login({ account, password })
    token.value = res.data.access_token
    localStorage.setItem('pms_token', res.data.access_token)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
    } catch {
      // 仅网络/服务端临时故障，不清除登录态
      // 真正的401已由请求拦截器统一处理
    }
  }

  // 刷新页面时恢复登录状态
  async function restoreSession() {
    if (token.value && !userInfo.value) {
      await fetchUserInfo()
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('pms_token')
  }

  return { token, userInfo, isAdmin, doLogin, fetchUserInfo, restoreSession, logout }
})
