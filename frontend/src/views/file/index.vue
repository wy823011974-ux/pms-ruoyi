<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">文件管理</h1>
      <el-button @click="$router.push('/projects')">返回项目</el-button>
    </div>

    <!-- 上传区域 -->
    <el-card class="mb-4">
      <div class="flex gap-2 items-center flex-wrap">
        <el-select v-model="uploadFtcId" placeholder="选择文件类型" size="small" style="width:220px">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="small" style="width:80px" placeholder="跳过行" />
        <el-checkbox v-model="uploadAsNew">新增上传（保留旧版本）</el-checkbox>
        <label class="el-button el-button--primary el-button--small" style="cursor:pointer">
          <input type="file" @change="handleUpload" accept=".xlsx,.xls,.docx,.doc,.pdf,.ppt,.pptx,.txt,.csv" hidden /> 选择文件
        </label>
      </div>
      <div v-if="uploadFtcId && files.some(f => f.fileType === fileTypes.find(ft => ft.id === uploadFtcId)?.code)" class="mt-2 text-xs text-amber-600 bg-amber-50 p-2 rounded">
        当前文件类型已有文件，再次上传默认覆盖。勾选"新增上传"可保留旧文件作为新版本。
      </div>
      <div class="mt-1 text-xs text-gray-400">支持 .xlsx / .xls / .docx / .pdf 等格式，最大 50MB</div>
    </el-card>

    <!-- 文件列表 -->
    <el-table :data="files" border size="small" v-loading="loading">
      <el-table-column prop="originalName" label="原始文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="fileType" label="文件类型" width="120">
        <template #default="{ row }"><el-tag size="small">{{ row.fileType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70">
        <template #default="{ row }"><span>v{{ row.version }}</span></template>
      </el-table-column>
      <el-table-column label="大小" width="90">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="上传时间" width="160">
        <template #default="{ row }"><span class="text-xs">{{ row.createdAt }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleDownload(row)">下载</el-button>
          <el-button size="small" @click="loadVersions(row)">版本</el-button>
          <el-popconfirm title="确定删除所有版本?" @confirm="doDelete(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!files.length && !loading" description="暂无文件，请先上传" />

    <!-- 版本历史弹窗 -->
    <el-dialog title="版本历史" v-model="versDialog" width="550px">
      <el-timeline v-if="versions.length">
        <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createdAt" placement="top">
          <strong>v{{ v.version }}</strong> · {{ formatSize(v.fileSize) }} · 跳过 {{ v.skipRows }} 行
          <el-tag v-if="v.isOverwrite" size="small" type="warning" class="ml-1">覆盖</el-tag>
          <el-tag v-else size="small" type="info" class="ml-1">新增</el-tag>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无版本记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes } from '@/api/project'
import request, { get, del } from '@/api/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const pid = ref(Number(route.params.projectId))
const fileTypes = ref([])
const uploadFtcId = ref(null)
const uploadSkipRows = ref(0)
const uploadAsNew = ref(false)
const files = ref([])
const loading = ref(false)
const versDialog = ref(false)
const versions = ref([])

async function loadFtc() {
  try {
    const r = await listProjects()
    const projs = r.data?.rows || []
    const p = projs.find(x => x.id === pid.value)
    if (p) {
      const r2 = await listFileTypes(p.projectTypeId)
      fileTypes.value = (r2.data || [])
    }
  } catch { /* ignore */ }
}

async function loadFiles() {
  loading.value = true
  try {
    const r = await get(`/projects/${pid.value}/files`)
    files.value = r.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

async function handleUpload(e) {
  if (!uploadFtcId.value || !e.target.files?.length) return
  const fd = new FormData()
  fd.append('file', e.target.files[0])
  fd.append('file_type_config_id', uploadFtcId.value)
  fd.append('skip_rows', uploadSkipRows.value || 0)
  fd.append('overwrite', uploadAsNew.value ? 'false' : 'true')
  try {
    await request.post(`/projects/${pid.value}/upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    e.target.value = ''
    loadFiles()
  } catch { ElMessage.error('上传失败') }
}

function handleDownload(row) {
  window.open(`/api/files/${row.id}/download`, '_blank')
}

async function loadVersions(f) {
  try {
    const r = await get(`/files/${f.id}/versions`)
    versions.value = r.data || []
    versDialog.value = true
  } catch { ElMessage.error('获取版本历史失败') }
}

async function doDelete(f) {
  try {
    await del(`/files/${f.id}`)
    ElMessage.success('已删除')
    loadFiles()
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => { loadFtc(); loadFiles() })
</script>
