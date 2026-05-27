<template>
  <div>
    <h1 class="page-title">仪表盘</h1>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><FolderOpened /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.totalProjects }}</div>
          <div class="stat-label">项目总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><Loading /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.activeProjects }}</div>
          <div class="stat-label">进行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Files /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.totalFiles }}</div>
          <div class="stat-label">文件数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><Document /></el-icon></div>
        <div>
          <div class="stat-value">{{ stats.totalDataRows }}</div>
          <div class="stat-label">数据行数</div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="panel">
        <div class="panel-header">快捷操作</div>
        <div class="quick-actions">
          <div class="action-item" @click="$router.push('/projects')">
            <div class="action-icon"><el-icon :size="20"><FolderOpened /></el-icon></div>
            <div class="action-text">
              <div class="action-title">项目管理</div>
              <div class="action-desc">创建和查看项目</div>
            </div>
            <el-icon class="action-chevron"><ArrowRight /></el-icon>
          </div>
          <div class="action-item" v-if="store.isAdmin" @click="$router.push('/project-types')">
            <div class="action-icon"><el-icon :size="20"><Setting /></el-icon></div>
            <div class="action-text">
              <div class="action-title">项目类型配置</div>
              <div class="action-desc">管理分类和模板</div>
            </div>
            <el-icon class="action-chevron"><ArrowRight /></el-icon>
          </div>
          <div class="action-item" v-if="store.isAdmin" @click="$router.push('/users')">
            <div class="action-icon"><el-icon :size="20"><User /></el-icon></div>
            <div class="action-text">
              <div class="action-title">用户管理</div>
              <div class="action-desc">账号和权限</div>
            </div>
            <el-icon class="action-chevron"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">最近项目</div>
        <div v-if="recentProjects.length" class="recent-list">
          <div v-for="p in recentProjects" :key="p.id" class="recent-item" @click="$router.push('/survey/' + p.id)">
            <div class="recent-left">
              <code class="recent-code">{{ p.code }}</code>
              <span class="recent-name">{{ p.name }}</span>
            </div>
            <el-tag :type="p.status === 'IN_PROGRESS' ? '' : p.status === 'COMPLETED' ? 'success' : 'info'" size="small" effect="plain">
              {{ p.status === 'IN_PROGRESS' ? '进行中' : p.status === 'COMPLETED' ? '已完结' : '已归档' }}
            </el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无项目" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'
import { useUserStore } from '@/stores/user'
import { ArrowRight } from '@element-plus/icons-vue'

const store = useUserStore()
const stats = ref({ totalProjects: 0, activeProjects: 0, totalFiles: 0, totalDataRows: 0 })
const recentProjects = ref([])

onMounted(async () => {
  try { const res = await get('/dashboard/stats'); if (res.code === 200) stats.value = res.data } catch {}
  try { const res = await get('/projects', { pageSize: 5 }); if (res.code === 200) recentProjects.value = res.data?.rows || [] } catch {}
})
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 24px 0; }
.stat-card {
  background: var(--apple-surface); border-radius: 16px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: var(--apple-shadow); transition: box-shadow 200ms ease, transform 200ms ease;
}
.stat-card:hover { box-shadow: var(--apple-shadow-modal); transform: translateY(-2px); }
.stat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-icon.blue { background: rgba(0,122,255,0.1); color: var(--apple-blue); }
.stat-icon.green { background: rgba(52,199,89,0.1); color: var(--apple-green); }
.stat-icon.orange { background: rgba(255,149,0,0.1); color: var(--apple-orange); }
.stat-icon.purple { background: rgba(175,82,222,0.1); color: var(--apple-purple); }
.stat-value { font-size: 28px; font-weight: 700; color: var(--apple-text); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--apple-text-secondary); margin-top: 2px; }

.dashboard-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel { background: var(--apple-surface); border-radius: 16px; overflow: hidden; box-shadow: var(--apple-shadow); }
.panel-header { font-size: 17px; font-weight: 600; padding: 18px 20px 14px; border-bottom: 0.5px solid var(--apple-separator); color: var(--apple-text); }

.quick-actions { padding: 4px 0; }
.action-item {
  display: flex; align-items: center; gap: 14px; padding: 14px 20px;
  cursor: pointer; transition: background 150ms ease;
}
.action-item:hover { background: var(--apple-fill-quaternary); }
.action-icon { width: 36px; height: 36px; border-radius: 10px; background: var(--apple-fill-tertiary); display: flex; align-items: center; justify-content: center; color: var(--apple-blue); }
.action-title { font-size: 15px; font-weight: 500; color: var(--apple-text); }
.action-desc { font-size: 13px; color: var(--apple-text-secondary); margin-top: 1px; }
.action-chevron { color: var(--apple-text-tertiary); font-size: 14px; }
.action-text { flex: 1; }

.recent-list { padding: 4px 0; }
.recent-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; cursor: pointer; transition: background 150ms ease;
}
.recent-item:hover { background: var(--apple-fill-quaternary); }
.recent-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.recent-code {
  font-family: var(--apple-font-mono); font-size: 12px; color: var(--apple-text-secondary);
  background: var(--apple-fill-tertiary); padding: 2px 8px; border-radius: 6px;
}
.recent-name { font-size: 15px; font-weight: 500; color: var(--apple-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
