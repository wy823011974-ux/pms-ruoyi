<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">文件管理</h1>
      <el-button @click="$router.push('/projects')">返回</el-button>
    </div>

    <el-card class="mb-4">
      <div class="flex gap-2 items-center flex-wrap">
        <el-select v-model="uploadFtcId" placeholder="选择文件类型" size="small" style="width:200px">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name+' (跳过'+ft.skipRows+'行)'" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="small" style="width:80px" placeholder="跳过行" />
        <el-checkbox v-model="uploadAsNew">新增上传</el-checkbox>
        <label class="el-button el-button--primary el-button--small"><input type="file" @change="handleUpload" accept=".xlsx,.xls,.docx,.doc,.pdf,.ppt,.pptx,.txt,.csv" hidden /> 选择文件</label>
      </div>
      <div v-if="uploadFtcId && files.some(f=>f.fileTypeConfigId===uploadFtcId)" class="mt-2 text-xs text-amber-600 bg-amber-50 p-2 rounded">当前文件类型已有文件，再次上传默认覆盖。勾选"新增上传"可保留旧文件。</div>
    </el-card>

    <el-table :data="files" border size="small">
      <el-table-column prop="filename" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="100"><template #default="{row}"><el-tag size="small">{{row.fileType}}</el-tag></template></el-table-column>
      <el-table-column prop="version" label="版本" width="70"><template #default="{row}"><span>v{{row.version}}</span></template></el-table-column>
      <el-table-column label="大小" width="80"><template #default="{row}">{{ (row.fileSize/1024/1024).toFixed(1) }}MB</template></el-table-column>
      <el-table-column label="上传时间" width="160"><template #default="{row}"><span class="text-xs">{{row.createdAt}}</span></template></el-table-column>
      <el-table-column label="操作" width="120"><template #default="{row}"><el-button size="small" @click="loadVersions(row)">版本</el-button><el-popconfirm title="删除?" @confirm="doDelete(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm></template></el-table-column>
    </el-table>
    <el-empty v-if="!files.length" description="暂无文件" />

    <!-- Version history dialog -->
    <el-dialog title="版本历史" v-model="versDialog" width="500px">
      <el-timeline>
        <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createdAt" placement="top">
          v{{ v.version }} · {{ (v.fileSize/1024/1024).toFixed(1) }}MB · 跳过{{ v.skipRows }}行
          <el-tag v-if="v.isOverwrite" size="small" type="warning">覆盖</el-tag>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes } from '@/api/project'
import request, { get, del } from '@/api/request'
import { ElMessage } from 'element-plus'

const route = useRoute(); const pid = ref(Number(route.params.projectId))
const fileTypes = ref([]); const uploadFtcId = ref(null)
const uploadSkipRows = ref(0); const uploadAsNew = ref(false)
const files = ref([]); const versDialog = ref(false); const versions = ref([])

async function loadFtc() {
  try{const r=await listProjects();const projs=r.data?.rows||[];const p=projs.find(x=>x.id===pid.value)
  if(p){const r2=await listFileTypes(p.projectTypeId);fileTypes.value=(r2.data||[]).filter(f=>!f.hasFields)}}
  catch{}
}
async function loadFiles() { try{const r=await get(`/projects/${pid.value}/files`);files.value=r.data||[]}catch{} }

async function handleUpload(e) {
  if(!uploadFtcId.value||!e.target.files?.length) return
  const fd = new FormData(); fd.append('file', e.target.files[0]); fd.append('file_type_config_id', uploadFtcId.value)
  fd.append('skip_rows', uploadSkipRows.value||0); fd.append('overwrite', uploadAsNew.value?'false':'true')
  await request.post(`/projects/${pid.value}/upload`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  ElMessage.success('上传成功'); e.target.value=''; loadFiles()
}

async function loadVersions(f) { const r=await get(`/files/${f.id}/versions`); versions.value=r.data||[]; versDialog.value=true }

async function doDelete(f) { await del(`/files/${f.id}`); ElMessage.success('已删除'); loadFiles() }

onMounted(() => { loadFtc(); loadFiles() })
</script>
