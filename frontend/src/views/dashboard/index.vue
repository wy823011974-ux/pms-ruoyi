<template>
  <div>
    <h1 class="page-title mb-4">仪表盘</h1>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="项目总数" :value="stats.totalProjects">
            <template #prefix><el-icon :size="20"><FolderOpened /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="进行中项目" :value="stats.activeProjects">
            <template #prefix><el-icon :size="20" color="#409eff"><Loading /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="文件总数" :value="stats.totalFiles">
            <template #prefix><el-icon :size="20" color="#67c23a"><Files /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="数据总行数" :value="stats.totalDataRows">
            <template #prefix><el-icon :size="20" color="#e6a23c"><Document /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="font-semibold">快捷操作</span></template>
          <div class="flex flex-col gap-3">
            <el-button type="primary" @click="$router.push('/projects')" style="width:100%">
              <el-icon><FolderOpened /></el-icon> 项目管理
            </el-button>
            <el-button type="success" @click="$router.push('/project-types')" style="width:100%" v-if="store.isAdmin">
              <el-icon><Setting /></el-icon> 项目类型配置
            </el-button>
            <el-button type="warning" @click="$router.push('/users')" style="width:100%" v-if="store.isAdmin">
              <el-icon><User /></el-icon> 用户管理
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="font-semibold">系统信息</span></template>
          <div class="text-sm text-gray-600 space-y-2">
            <div class="flex justify-between"><span>系统版本</span><span class="font-mono">v2.0.0</span></div>
            <div class="flex justify-between"><span>技术栈</span><span>Spring Boot 3.2 + Vue 3</span></div>
            <div class="flex justify-between"><span>数据库</span><span>MySQL 8.0 + Redis 7</span></div>
            <div class="flex justify-between"><span>当前用户</span><span>{{ store.userInfo?.displayName || '-' }}</span></div>
            <el-divider />
            <div class="text-xs text-gray-400">项目管理系统 · 海南海拔市场调查集团</div>
          </div>
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

onMounted(async () => {
  try {
    const res = await get('/dashboard/stats')
    if (res.code === 200) stats.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-card :deep(.el-statistic__head) { font-size: 13px; }
</style>
