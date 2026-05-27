<template>
  <el-container class="layout-container">
    <el-aside width="260px" class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">P</div>
        <div>
          <div class="logo-title">项目管理系统</div>
          <div class="logo-subtitle">PMS v3.0</div>
        </div>
      </div>

      <div class="nav-section">
        <div class="nav-label">主要</div>
        <el-menu :default-active="route.path" router class="sidebar-menu">
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon><span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/projects">
            <el-icon><FolderOpened /></el-icon><span>项目管理</span>
          </el-menu-item>
        </el-menu>

        <div class="nav-label" v-if="store.isAdmin">管理</div>
        <el-menu v-if="store.isAdmin" :default-active="route.path" router class="sidebar-menu">
          <el-menu-item index="/project-types">
            <el-icon><Setting /></el-icon><span>项目类型配置</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>

    <el-container class="main-area">
      <el-header class="header">
        <div class="header-left">
          <span class="user-name">{{ store.userInfo?.displayName || '未登录' }}</span>
          <span class="user-role">{{ roleLabel }}</span>
        </div>
        <el-dropdown @command="handleCommand" trigger="click">
          <span class="user-menu-trigger">
            <div class="avatar">{{ (store.userInfo?.displayName || '?')[0] }}</div>
            <span class="username-text">{{ store.userInfo?.username }}</span>
            <el-icon class="chevron"><ArrowDown /></el-icon>
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

const route = useRoute(); const router = useRouter(); const store = useUserStore()
const roleLabels = { super_admin: '超级管理员', admin: '管理员', manager: '负责人', member: '成员', customer: '客户' }
const roleLabel = computed(() => roleLabels[store.userInfo?.role] || '')

function handleCommand(cmd) {
  if (cmd === 'logout') { store.logout(); router.push('/login') }
  else if (cmd === 'password') {
    ElMessageBox.prompt('请输入新密码', '修改密码', { inputType: 'password', confirmButtonText: '确认' })
      .then(async ({ value }) => {
        if (!value || value.length < 8) { ElMessage.warning('密码至少8位'); return }
        ElMessageBox.prompt('请输入旧密码', '验证身份', { inputType: 'password' })
          .then(async ({ value: oldPwd }) => {
            try { await changePassword({ oldPassword: oldPwd, newPassword: value }); ElMessage.success('密码修改成功'); store.logout(); router.push('/login') }
            catch { ElMessage.error('修改失败') }
          }).catch(() => {})
      }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }

/* 侧边栏 — Apple 毛玻璃 */
.sidebar {
  background: rgba(28,28,30,0.92);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-right: 0.5px solid rgba(255,255,255,0.08);
  display: flex; flex-direction: column; overflow-y: auto;
}
.logo-area { display: flex; align-items: center; gap: 14px; padding: 24px 20px 20px; }
.logo-icon {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: var(--apple-blue); color: #fff; border-radius: 12px; font-weight: 700; font-size: 20px;
}
.logo-title { color: #fff; font-size: 16px; font-weight: 600; line-height: 1.3; }
.logo-subtitle { color: rgba(255,255,255,0.45); font-size: 11px; font-weight: 400; }

.nav-section { flex: 1; padding: 0 12px; }
.nav-label { color: rgba(255,255,255,0.35); font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; padding: 20px 12px 6px; }

.sidebar-menu { border: none !important; background: transparent !important; }
.sidebar-menu .el-menu-item {
  margin: 1px 0; border-radius: 10px; height: 40px; line-height: 40px; font-size: 15px;
  color: rgba(255,255,255,0.7); transition: all 150ms ease;
  padding-left: 12px !important;
}
.sidebar-menu .el-menu-item:hover { background: rgba(255,255,255,0.06); color: #fff; }
.sidebar-menu .el-menu-item.is-active {
  background: rgba(0,122,255,0.25); color: #fff; font-weight: 500;
  position: relative;
}
.sidebar-menu .el-menu-item.is-active::before {
  content: ''; position: absolute; left: 0; top: 8px; bottom: 8px; width: 3px;
  background: var(--apple-blue); border-radius: 2px;
}

/* 主区域 */
.main-area { background: var(--apple-bg); }

/* 顶栏 */
.header {
  height: 52px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; background: rgba(255,255,255,0.72);
  backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px);
  border-bottom: 0.5px solid var(--apple-separator);
}
.header-left { display: flex; align-items: baseline; gap: 10px; }
.user-name { font-size: 15px; font-weight: 600; color: var(--apple-text); }
.user-role { font-size: 12px; color: var(--apple-text-secondary); }

.user-menu-trigger {
  display: flex; align-items: center; gap: 8px; padding: 4px 12px 4px 4px;
  border-radius: 20px; cursor: pointer; transition: background 150ms ease;
}
.user-menu-trigger:hover { background: var(--apple-fill-tertiary); }
.avatar {
  width: 28px; height: 28px; border-radius: 50%; background: var(--apple-fill);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; color: var(--apple-text);
}
.username-text { font-size: 14px; font-weight: 500; color: var(--apple-text); }
.chevron { font-size: 10px; color: var(--apple-text-secondary); }
</style>
