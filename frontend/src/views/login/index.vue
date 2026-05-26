<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">项目管理系统 v2.0</h2>
      <el-form v-if="!showRegister" ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
        <el-form-item prop="phone">
          <el-input v-model="loginForm.phone" placeholder="手机号" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <div class="text-center text-sm text-gray-500">
          还没有账号？<el-link type="primary" @click="showRegister = true">注册</el-link>
        </div>
      </el-form>

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister">
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号 *" size="large" />
        </el-form-item>
        <el-form-item prop="displayName">
          <el-input v-model="registerForm.displayName" placeholder="姓名 *" size="large" />
        </el-form-item>
        <el-form-item prop="department">
          <el-input v-model="registerForm.department" placeholder="部门/单位 *" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱 *" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码 (8位+字母+数字)" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="large" :loading="loading" class="login-btn" @click="handleRegister">注 册</el-button>
        </el-form-item>
        <div class="text-center text-sm text-gray-500">
          已有账号？<el-link type="primary" @click="showRegister = false">返回登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

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
  catch {} finally { loading.value = false }
}

async function handleRegister() {
  loading.value = true
  try {
    await register({ phone: registerForm.phone, password: registerForm.password, email: registerForm.email, displayName: registerForm.displayName, department: registerForm.department })
    ElMessage.success('注册成功，等待管理员审批')
    showRegister.value = false
  } catch {} finally { loading.value = false }
}
</script>

<style scoped>
.login-container {
  display: flex; align-items: center; justify-content: center; min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card { width: 420px; padding: 20px 30px; }
.login-title { text-align: center; margin-bottom: 24px; font-size: 22px; color: #303133; }
.login-btn { width: 100%; }
</style>
