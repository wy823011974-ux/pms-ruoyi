<template>
  <div>
    <h1 class="page-title mb-4">仪表盘</h1>

    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <div class="stat-item">
          <span class="stat-value">{{ stats.totalProjects }}</span>
          <span class="stat-label">项目总数</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item accent">
          <span class="stat-value">{{ stats.activeProjects }}</span>
          <span class="stat-label">进行中</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item secondary">
          <span class="stat-value">{{ stats.totalFiles }}</span>
          <span class="stat-label">文件数</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-item warn">
          <span class="stat-value">{{ stats.totalDataRows }}</span>
          <span class="stat-label">数据行数</span>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="font-semibold">快捷操作</span></template>
          <div class="quick-actions">
            <el-button @click="$router.push('/projects')">
              <el-icon><FolderOpened /></el-icon> 项目管理
            </el-button>
            <el-button type="success" @click="$router.push('/project-types')" v-if="store.isAdmin">
              <el-icon><Setting /></el-icon> 项目类型配置
            </el-button>
            <el-button type="warning" @click="$router.push('/users')" v-if="store.isAdmin">
              <el-icon><User /></el-icon> 用户管理
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="font-semibold">最近项目</span></template>
          <div v-if="recentProjects.length" class="recent-list">
            <div v-for="p in recentProjects" :key="p.id" class="recent-item" @click="$router.push('/survey/' + p.id)">
              <code>{{ p.code }}</code>
              <span class="recent-name">{{ p.name }}</span>
              <el-tag :type="p.status === 'IN_PROGRESS' ? '' : p.status === 'COMPLETED' ? 'success' : 'info'" size="small">
                {{ p.status === 'IN_PROGRESS' ? '进行中' : p.status === 'COMPLETED' ? '已完结' : '已归档' }}
              </el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无项目" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const stats = ref({ totalProjects: 0, activeProjects: 0, totalFiles: 0, totalDataRows: 0 })
const recentProjects = ref([])

onMounted(async () => {
  try { const res = await get('/dashboard/stats'); if (res.code === 200) stats.value = res.data } catch {}
  try { const res = await get('/projects', { pageSize: 5 }); if (res.code === 200) recentProjects.value = res.data?.rows || [] } catch {}
})
</script>

<style scoped>
.stat-item {
  text-align: center; padding: 24px 16px; border-radius: 12px;
  background: #fff; border: 1px solid var(--color-border);
  transition: box-shadow 200ms ease, transform 200ms ease;
}
.stat-item:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-item.accent { background: #ECFDF5; border-color: #A7F3D0; }
.stat-item.secondary { background: #F0FDF4; border-color: #86EFAC; }
.stat-item.warn { background: #FFFBEB; border-color: #FDE68A; }
.stat-value { display: block; font-size: 32px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-item.accent .stat-value { color: #059669; }
.stat-item.secondary .stat-value { color: #16A34A; }
.stat-item.warn .stat-value { color: #D97706; }
.stat-label { display: block; margin-top: 4px; font-size: 13px; color: var(--color-text-secondary); }

.quick-actions { display: flex; flex-direction: column; gap: 8px; }
.quick-actions .el-button { justify-content: flex-start; margin-left: 0; }

.recent-list { display: flex; flex-direction: column; }
.recent-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 0;
  border-bottom: 1px solid var(--color-border); cursor: pointer; transition: background 150ms ease;
}
.recent-item:last-child { border-bottom: none; }
.recent-item:hover { background: var(--color-muted); margin: 0 -20px; padding: 10px 20px; border-radius: 8px; }
.recent-name { flex: 1; font-size: 14px; color: var(--color-text); font-weight: 500; }
</style>
