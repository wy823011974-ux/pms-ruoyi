<template>
  <div>
    <h1 class="page-title">仪表盘</h1>
    <el-row :gutter="20">
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="项目总数" :value="stats.totalProjects" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="进行中" :value="stats.activeProjects" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="文件数" :value="stats.totalFiles" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><el-statistic title="数据行数" :value="stats.totalDataRows" /></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'

const stats = ref({ totalProjects: 0, activeProjects: 0, totalFiles: 0, totalDataRows: 0 })

onMounted(async () => {
  try { const res = await get('/dashboard/stats'); if (res.code === 200) stats.value = res.data } catch {}
})
</script>
