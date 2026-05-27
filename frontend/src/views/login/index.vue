<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-icon">P</div>
        <h2>项目管理系统</h2>
        <p class="login-subtitle">海南海拔市场调查集团</p>
      </div>

      <el-form v-if="!showRegister" ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin" class="login-form">
        <el-form-item prop="phone">
          <el-input v-model="loginForm.phone" placeholder="手机号" size="large" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister" class="login-form">
        <el-form-item prop="phone"><el-input v-model="registerForm.phone" placeholder="手机号" size="large" /></el-form-item>
        <el-form-item prop="displayName"><el-input v-model="registerForm.displayName" placeholder="姓名" size="large" /></el-form-item>
        <el-form-item prop="department"><el-input v-model="registerForm.department" placeholder="部门/单位" size="large" /></el-form-item>
        <el-form-item prop="email"><el-input v-model="registerForm.email" placeholder="邮箱" size="large" /></el-form-item>
        <el-form-item prop="password"><el-input v-model="registerForm.password" type="password" placeholder="密码（至少8位）" size="large" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <template v-if="!showRegister">
          还没有账号？<el-link type="primary" :underline="false" @click="showRegister = true">注册</el-link>
        </template>
        <template v-else>
          已有账号？<el-link type="primary" :underline="false" @click="showRegister = false">返回登录</el-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Phone, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const store = useUserStore()
const showRegister = ref(false)
const loading = ref(false)

const loginForm = reactive({ phone: '', password: '' })
const loginRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({ phone: '', password: '', email: '', displayName: '', department: '' })
const registerRules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门/单位', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  password: [{ required: true, min: 8, message: '密码至少8位', trigger: 'blur' }]
}

async function handleLogin() {
  loading.value = true
  try { await store.doLogin(loginForm.phone, loginForm.password); router.push('/dashboard') }
  catch (e) {
    // 错误已由请求拦截器统一显示，此处仅处理未预期的异常
    if (e && e.message && e.message !== '请求失败') ElMessage.error(e.message)
  }
  finally { loading.value = false }
}

async function handleRegister() {
  loading.value = true
  try {
    await register({ phone: registerForm.phone, password: registerForm.password, email: registerForm.email, displayName: registerForm.displayName, department: registerForm.department })
    ElMessage.success('注册成功，等待管理员审批')
    showRegister.value = false
  } catch (e) {
    if (e && e.message && e.message !== '请求失败') ElMessage.error(e.message)
  }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  display: flex; align-items: center; justify-content: center; min-height: 100vh;
  background: linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%);
}
.login-card {
  width: 400px; padding: 40px 36px; background: #fff;
  border-radius: 16px; box-shadow: 0 4px 24px rgba(5, 150, 105, 0.12);
}
.login-header { text-align: center; margin-bottom: 32px; }
.login-icon {
  width: 48px; height: 48px; margin: 0 auto 16px; display: flex; align-items: center; justify-content: center;
  background: #ECFDF5; color: #059669; border-radius: 12px; font-size: 22px; font-weight: 700;
}
.login-header h2 { font-size: 22px; font-weight: 600; color: #0F172A; margin: 0 0 4px; letter-spacing: 0.5px; }
.login-subtitle { font-size: 13px; color: #64748B; margin: 0; }
.login-form { margin-top: 8px; }
.login-btn { width: 100%; height: 44px; font-size: 15px; letter-spacing: 2px; }
.login-footer { text-align: center; margin-top: 16px; font-size: 13px; color: #64748B; }
</style>
