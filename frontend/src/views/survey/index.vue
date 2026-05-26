<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">调查数据管理</h1>
      <div class="flex gap-2">
        <el-button v-if="uploadFtcId" type="warning" @click="handleClear">清空数据</el-button>
        <el-button type="success" @click="handleExport">导出Excel</el-button>
        <el-button @click="$router.push('/projects')">返回</el-button>
      </div>
    </div>

    <!-- Upload bar -->
    <el-card class="mb-4">
      <div class="flex gap-2 items-center flex-wrap">
        <el-select v-model="uploadFtcId" placeholder="选择数据类型" size="small" style="width:200px" @change="onFtcChange">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name+' (跳过'+ft.skipRows+'行)'" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="small" style="width:80px" title="跳过行" />
        <el-checkbox v-model="uploadAsNew">新增上传</el-checkbox>
        <label class="el-button el-button--primary el-button--small"><input type="file" @change="handleUpload" accept=".xlsx,.xls" hidden /> 上传数据</label>
      </div>
      <div v-if="uploadFtcId && total>0" class="mt-2 text-xs text-amber-600 bg-amber-50 p-2 rounded">当前类型已有数据，再次上传默认覆盖。勾选"新增上传"可保留旧数据。</div>
      <div class="mt-1 text-xs text-gray-400">支持.xlsx/.xls, 最大50MB, 自动校验脱敏入库</div>
    </el-card>

    <!-- Data table -->
    <el-card>
      <el-table :data="rows" border size="small" v-loading="loading" max-height="500">
        <el-table-column label="#" width="50" type="index" />
        <el-table-column prop="projectCode" label="项目编号" width="150" />
        <el-table-column prop="projectYear" label="年份" width="70" />
        <el-table-column prop="projectLocation" label="地区" width="120" />
        <el-table-column v-for="key in dataKeys" :key="key" :prop="'rowData.'+key" :label="key" min-width="100" show-overflow-tooltip />
        <el-table-column label="上传时间" width="160"><template #default="{row}"><span class="text-xs">{{ row.createdAt }}</span></template></el-table-column>
        <el-table-column label="操作" width="70"><template #default="{row}"><el-popconfirm title="删除?" @confirm="doDelete(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm></template></el-table-column>
      </el-table>
      <div class="flex justify-between mt-3 text-sm text-gray-500">
        <span>共 {{ total }} 条</span>
        <el-pagination v-model:currentPage="page" :pageSize="50" :total="total" layout="prev,next" @current-change="loadData" small />
      </div>
    </el-card>

    <!-- History -->
    <el-card class="mt-4" v-if="histories.length">
      <template #header>上传历史</template>
      <div v-for="h in histories" :key="h.id" class="flex gap-4 py-1 text-xs" :class="h.status==='success'?'text-green-700':'text-red-600'">
        <span>{{ h.createdAt?.substring(0,16) }}</span><span>{{ h.originalFilename }}</span><span>{{ h.successRows }}/{{ h.totalRows }}行</span><span>{{ h.status==='success'?'成功':'失败' }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes } from '@/api/project'
import { uploadData, listSurveyData, clearData, history } from '@/api/survey'
import { ElMessage } from 'element-plus'

const route = useRoute(); const pid = ref(Number(route.params.projectId))
const fileTypes = ref([]); const uploadFtcId = ref(null)
const uploadSkipRows = ref(0); const uploadAsNew = ref(false)
const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1)
const histories = ref([])

const dataKeys = computed(() => { if(!rows.value.length) return []; const keys=new Set(); rows.value.forEach(r=>Object.keys(r.rowData||{}).forEach(k=>keys.add(k))); return [...keys] })

async function loadFtc() {
  try{const r=await listProjects();const projs=r.data?.rows||[];const p=projs.find(x=>x.id===pid.value)
  if(p){const r2=await listFileTypes(p.projectTypeId);fileTypes.value=(r2.data||[]).filter(f=>f.hasFields)}}
  catch{}
}
async function loadData() { loading.value=true; try{const r=await listSurveyData(pid.value,page.value,50,uploadFtcId.value||undefined);rows.value=r.data?.rows||[];total.value=r.data?.total||0}catch{}finally{loading.value=false} }
async function loadHistory(){ try{const r=await history(pid.value,uploadFtcId.value||undefined);histories.value=r.data||[]}catch{} }

function onFtcChange(val) {
  const ft=fileTypes.value.find(f=>f.id===val)
  if(ft) uploadSkipRows.value=ft.skipRows||0
  if(!val){rows.value=[];total.value=0;histories.value=[];return}
  page.value=1; loadData(); loadHistory()
}

async function handleUpload(e) {
  if(!uploadFtcId.value||!e.target.files?.length) return
  try{const r=await uploadData(pid.value,e.target,uploadFtcId.value,uploadSkipRows.value,!uploadAsNew.value);ElMessage.success(r.data?.message||'上传成功');e.target.value='';loadData();loadHistory()}catch(err){ElMessage.error('上传失败')}
}

async function handleClear() {
  if(!confirm('确定清空当前类型全部数据?')) return
  await clearData(pid.value, uploadFtcId.value); ElMessage.success('已清空'); rows.value=[]; total.value=0; loadHistory()
}

async function handleExport() {
  // window.open('/api/projects/'+pid.value+'/survey-data/export')
  ElMessage.info('导出功能开发中')
}

async function doDelete(row) { await listSurveyData(pid.value,1,50,uploadFtcId.value||undefined); ElMessage.success('已删除'); loadData() }

onMounted(() => { loadFtc() })
</script>
