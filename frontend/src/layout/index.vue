<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-icon">P</span>
        <span class="logo-text">项目管理系统</span>
      </div>
      <el-menu
        :default-active="route.path" router
        background-color="transparent"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><FolderOpened /></el-icon><span>项目管理</span>
        </el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/project-types">
          <el-icon><Setting /></el-icon><span>项目类型配置</span>
        </el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-area">
      <el-header class="header">
        <div class="header-left">
          <span class="user-name">{{ store.userInfo?.displayName || store.userInfo?.username || '未登录' }}</span>
          <el-tag v-if="store.userInfo?.role" size="small" :type="roleType" effect="plain">{{ roleLabel }}</el-tag>
        </div>
        <el-dropdown @command="handleCommand" trigger="click">
          <span class="user-menu-trigger">
            <el-icon><UserFilled /></el-icon>
            <span>{{ store.userInfo?.username }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
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
import { changePassword } from '@/api/auth'
import { ElMessageBox, ElMessage } from 'element-plus'

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
    ElMessageBox.prompt('请输入新密码', '修改密码', {
      inputType: 'password', confirmButtonText: '确认', cancelButtonText: '取消',
    }).then(async ({ value }) => {
      if (!value || value.length < 8) { ElMessage.warning('密码至少8位'); return }
      ElMessageBox.prompt('请输入旧密码', '验证身份', { inputType: 'password' }).then(async ({ value: oldPwd }) => {
        try {
          await changePassword({ oldPassword: oldPwd, newPassword: value })
          ElMessage.success('密码修改成功，请重新登录')
          store.logout(); router.push('/login')
        } catch (e) { ElMessage.error('修改失败') }
      }).catch(() => {})
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }

/* 侧边栏 — 绿色系 */
.sidebar {
  background: linear-gradient(180deg, #065F46 0%, #047857 100%);
  overflow-y: auto; border-right: 1px solid rgba(255,255,255,0.06);
}
.logo { display: flex; align-items: center; gap: 10px; height: 56px; padding: 0 20px; border-bottom: 1px solid rgba(255,255,255,0.08); }
.logo-icon {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.15); color: #fff; border-radius: 8px; font-weight: 700; font-size: 16px;
}
.logo-text { color: #fff; font-size: 15px; font-weight: 600; letter-spacing: 0.5px; }
.el-menu { border-right: none !important; padding: 8px 0; }
.el-menu-item { margin: 2px 8px; border-radius: 8px; height: 40px; line-height: 40px; font-size: 14px; transition: all 150ms ease; }
.el-menu-item:hover { background: rgba(255,255,255,0.08) !important; }
.el-menu-item.is-active { background: rgba(255,255,255,0.15) !important; color: #fff !important; font-weight: 500; }

/* 顶栏 */
.main-area { background: var(--color-bg); }
.header {
  background: #fff; height: 56px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; border-bottom: 1px solid var(--color-border); box-shadow: var(--shadow-sm);
}
.header-left { display: flex; align-items: center; gap: 10px; }
.user-name { font-size: 14px; color: var(--color-text); font-weight: 500; }
.user-menu-trigger {
  display: flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 8px;
  cursor: pointer; font-size: 13px; color: var(--color-text-secondary); transition: background 150ms ease;
}
.user-menu-trigger:hover { background: var(--color-muted); }
</style>
