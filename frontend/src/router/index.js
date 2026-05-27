import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
      { path: 'projects', name: 'Projects', component: () => import('@/views/project/index.vue'), meta: { title: '项目管理', icon: 'FolderOpened' } },
      { path: 'users', name: 'Users', component: () => import('@/views/user/index.vue'), meta: { title: '用户管理', icon: 'User', roles: ['super_admin','admin'] } },
      { path: 'project-types', name: 'ProjectTypes', component: () => import('@/views/projectType/index.vue'), meta: { title: '项目类型配置', icon: 'Setting', roles: ['super_admin','admin'] } },
      { path: 'survey/:projectId', name: 'SurveyData', component: () => import('@/views/survey/index.vue'), meta: { title: '调查数据', icon: 'Document' } },
      { path: 'files/:projectId', name: 'ProjectFiles', component: () => import('@/views/file/index.vue'), meta: { title: '文件管理', icon: 'Files' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - PMS' : '项目管理系统'
  const store = useUserStore()

  // 刷新页面时恢复登录状态（token存在但userInfo丢失）
  if (store.token && !store.userInfo) {
    await store.restoreSession()
  }

  if (to.name !== 'Login' && !store.token) {
    next({ name: 'Login' })
  } else if (to.name === 'Login' && store.token) {
    next({ path: '/dashboard' })
  } else if (to.meta.roles && store.userInfo && !to.meta.roles.includes(store.userInfo.role)) {
    // 角色不足，跳回仪表盘
    next({ path: '/dashboard' })
  } else {
    next()
  }
})

export default router
