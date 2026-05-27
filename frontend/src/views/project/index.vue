<template>
  <div>
    <div class="flex justify-between mb-4">
      <div class="flex gap-2">
        <h1 class="page-title">项目管理</h1>
        <el-select v-model="filter.status" placeholder="状态" clearable size="small" style="width:120px" @change="load">
          <el-option v-for="s in ['IN_PROGRESS','COMPLETED','ARCHIVED']" :key="s" :label="s==='IN_PROGRESS'?'进行中':s==='COMPLETED'?'已完结':'已归档'" :value="s" />
        </el-select>
        <el-input v-model="filter.keyword" placeholder="搜索" clearable size="small" style="width:180px" @keyup.enter="load" />
      </div>
      <el-button type="primary" @click="openCreate">+ 新建项目</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="8" v-for="p in projects" :key="p.id">
        <el-card shadow="hover" class="mb-4">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-semibold">{{ p.name }}</span>
              <el-tag :type="p.status==='IN_PROGRESS'?'':p.status==='COMPLETED'?'success':'info'" size="small">{{ p.status==='IN_PROGRESS'?'进行中':p.status==='COMPLETED'?'已完结':'已归档' }}</el-tag>
            </div>
          </template>
          <div class="text-sm text-gray-500 space-y-1">
            <div><code class="text-blue-600 bg-blue-50 px-1 rounded">{{ p.code }}</code></div>
            <div>{{ p.location }} · {{ p.year }}</div>
            <div class="text-xs text-gray-400">{{ p.createTime }}</div>
          </div>
          <div class="mt-3 flex gap-1 flex-wrap">
            <el-button v-if="p.status==='IN_PROGRESS'" size="small" @click="changeStatus(p,'COMPLETED')">完结</el-button>
            <el-button v-if="p.status==='COMPLETED'" size="small" @click="changeStatus(p,'IN_PROGRESS')">重开</el-button>
            <el-button v-if="p.status==='COMPLETED'" size="small" @click="changeStatus(p,'ARCHIVED')">归档</el-button>
            <el-button size="small" @click="$router.push('/files/'+p.id)">文件</el-button>
            <el-button size="small" type="success" @click="$router.push('/survey/'+p.id)">数据</el-button>
            <el-button size="small" @click="openEdit(p)">编辑</el-button>
            <el-popconfirm title="软删除?" @confirm="doDelete(p)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!projects.length" description="暂无项目" />

    <el-dialog :title="editing ? '编辑项目' : '新建项目'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="项目名称" /></el-form-item>
        <el-form-item label="年份"><el-input-number v-model="form.year" :min="2020" :max="2030" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.location" placeholder="如：海南省海口市" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.projectTypeId" placeholder="选择项目类型">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-alert v-if="!editing" type="info" :closable="false" show-icon class="mt-2">
          <template #title><span class="text-xs">项目编码将自动生成（格式：HBXM-类型-年份-序号）</span></template>
        </el-alert>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listProjects, createProject, updateProject, deleteProject, updateStatus, listProjectTypes } from '@/api/project'
import { ElMessage } from 'element-plus'

const projects = ref([]); const types = ref([]); const dialogVisible = ref(false); const editing = ref(null)
const filter = reactive({ status:'', keyword:'' })
const form = reactive({ name:'', year:2026, location:'海南省', projectTypeId:null })

async function load() { const r = await listProjects({status:filter.status||undefined, keyword:filter.keyword||undefined}); projects.value = r.data?.rows||[] }
async function loadTypes() { try{const r=await listProjectTypes(); types.value=r.data||[]}catch{} }
function openCreate() { editing.value=null; Object.assign(form,{name:'',year:2026,location:'海南省',projectTypeId:null}); dialogVisible.value=true }
function openEdit(p) { editing.value=p.id; Object.assign(form,{name:p.name,year:p.year,location:p.location,projectTypeId:p.projectTypeId}); dialogVisible.value=true }
async function submit() {
  if(editing.value){await updateProject(editing.value,form)}else{await createProject(form)}
  ElMessage.success(editing.value ? '已更新' : '已创建，项目编码已自动生成'); dialogVisible.value=false; load()
}
async function changeStatus(p, status) { await updateStatus(p.id, {status}); ElMessage.success('已更新'); load() }
async function doDelete(p) { await deleteProject(p.id); ElMessage.success('已删除'); load() }
onMounted(() => { load(); loadTypes() })
</script>
