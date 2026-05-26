<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">项目管理系统</div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/projects"><el-icon><FolderOpened /></el-icon><span>项目管理</span></el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/project-types"><el-icon><Setting /></el-icon><span>项目类型配置</span></el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="flex items-center gap-2">
          <span>{{ store.userInfo?.displayName || store.userInfo?.username || '未登录' }}</span>
          <el-tag v-if="store.userInfo?.role" size="small" :type="roleType">{{ roleLabel }}</el-tag>
        </div>
        <el-dropdown @command="handleCommand">
          <el-button type="default" size="small">
            <el-icon><UserFilled /></el-icon> {{ store.userInfo?.username }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const roleLabels = { super_admin: '超级管理员', admin: '管理员', manager: '负责人', member: '成员', customer: '客户' }
const roleTypes = { super_admin: 'danger', admin: 'danger', manager: 'warning', member: 'info', customer: 'success' }
const roleLabel = computed(() => roleLabels[store.userInfo?.role] || store.userInfo?.role)
const roleType = computed(() => roleTypes[store.userInfo?.role] || 'info')

function handleCommand(cmd) {
  if (cmd === 'logout') { store.logout(); router.push('/login') }
  else if (cmd === 'password') {
    ElMessageBox.prompt('请输入新密码', '修改密码', { inputType: 'password' }).then(({ value }) => {
      // TODO: call API
    })
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar { background-color: #304156; overflow-y: auto; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.1); }
.header { background: #fff; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.el-menu { border-right: none; }
</style>
