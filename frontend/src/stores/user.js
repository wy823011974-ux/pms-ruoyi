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
    } catch { logout() }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('pms_token')
  }

  return { token, userInfo, isAdmin, doLogin, fetchUserInfo, logout }
})
