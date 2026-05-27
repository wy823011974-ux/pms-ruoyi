<template>
  <div>
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h1 class="page-title">文件管理</h1>
        <el-button @click="$router.push('/projects')" class="back-btn">返回项目</el-button>
      </div>
    </div>

    <div class="upload-section">
      <div class="upload-controls">
        <el-select v-model="uploadFtcId" placeholder="选择文件类型" size="large" class="upload-select">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="large" class="upload-skip" placeholder="跳过行" />
        <el-checkbox v-model="uploadAsNew">新增上传</el-checkbox>
        <label class="upload-file-btn">
          <input type="file" @change="handleUpload" accept=".xlsx,.xls,.docx,.doc,.pdf,.ppt,.pptx,.txt,.csv" hidden />
          <el-icon><Upload /></el-icon>选择文件
        </label>
      </div>
    </div>

    <div class="table-wrap">
      <el-table :data="files" size="large" v-loading="loading" class="apple-table">
        <el-table-column prop="originalName" label="文件名" min-width="240" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="120">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.fileType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80">
          <template #default="{ row }"><span class="version-text">v{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }"><span class="meta-text">{{ formatSize(row.fileSize) }}</span></template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }"><span class="time-text">{{ row.createdAt }}</span></template>
        </el-table-column>
        <el-table-column label="" width="120" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" class="action-btn" @click="handleDownload(row)">下载</el-button>
              <el-button size="small" class="action-btn" @click="loadVersions(row)">版本</el-button>
              <el-popconfirm title="确定删除?" @confirm="doDelete(row)">
                <template #reference><el-button size="small" class="action-btn danger">删除</el-button></template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!files.length && !loading" description="暂无文件" style="padding:40px 0" />
    </div>

    <el-dialog title="版本历史" v-model="versDialog" width="480px">
      <div v-if="versions.length" class="version-list">
        <div v-for="v in versions" :key="v.id" class="version-item">
          <div class="version-info">
            <span class="version-label">v{{ v.version }}</span>
            <span class="version-meta">{{ formatSize(v.fileSize) }} · 跳过 {{ v.skipRows }} 行</span>
          </div>
          <div class="version-right">
            <el-tag v-if="v.isOverwrite" size="small" type="warning" effect="plain">覆盖</el-tag>
            <el-tag v-else size="small" effect="plain">新增</el-tag>
            <span class="time-text">{{ v.createdAt }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无版本记录" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes } from '@/api/project'
import request, { get, del } from '@/api/request'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

const route = useRoute(); const pid = ref(Number(route.params.projectId))
const fileTypes = ref([]); const uploadFtcId = ref(null); const uploadSkipRows = ref(0); const uploadAsNew = ref(false)
const files = ref([]); const loading = ref(false); const versDialog = ref(false); const versions = ref([])

async function loadFtc() { try { const r = await listProjects(); const projs = r.data?.rows || []; const p = projs.find(x => x.id === pid.value); if (p) { const r2 = await listFileTypes(p.projectTypeId); fileTypes.value = (r2.data || []) } } catch {} }
async function loadFiles() { loading.value = true; try { const r = await get(`/projects/${pid.value}/files`); files.value = r.data || [] } catch {} finally { loading.value = false } }
function formatSize(bytes) { if (!bytes) return '0 B'; if (bytes < 1024) return bytes + ' B'; if (bytes < 1048576) return (bytes/1024).toFixed(1) + ' KB'; return (bytes/1048576).toFixed(1) + ' MB' }
async function handleUpload(e) { if (!uploadFtcId.value || !e.target.files?.length) return; const fd = new FormData(); fd.append('file', e.target.files[0]); fd.append('file_type_config_id', uploadFtcId.value); fd.append('skip_rows', uploadSkipRows.value || 0); fd.append('overwrite', uploadAsNew.value ? 'false' : 'true'); try { await request.post(`/projects/${pid.value}/upload`, fd, { headers: { 'Content-Type': 'multipart/form-data' } }); ElMessage.success('上传成功'); e.target.value = ''; loadFiles() } catch { ElMessage.error('上传失败') } }
function handleDownload(row) { window.open(`/api/files/${row.id}/download`, '_blank') }
async function loadVersions(f) { try { const r = await get(`/files/${f.id}/versions`); versions.value = r.data || []; versDialog.value = true } catch { ElMessage.error('获取失败') } }
async function doDelete(f) { try { await del(`/files/${f.id}`); ElMessage.success('已删除'); loadFiles() } catch { ElMessage.error('删除失败') } }
onMounted(() => { loadFtc(); loadFiles() })
</script>

<style scoped>
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.back-btn { background: var(--apple-fill-tertiary); border: none; color: var(--apple-text); }

.upload-section { background: var(--apple-surface); border-radius: 16px; padding: 20px 24px; margin-bottom: 20px; box-shadow: var(--apple-shadow); }
.upload-controls { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.upload-select { width: 220px; }
.upload-skip { width: 90px; }
.upload-file-btn {
  display: inline-flex; align-items: center; gap: 6px; padding: 10px 18px;
  background: var(--apple-blue); color: #fff; border-radius: 12px; font-size: 15px; font-weight: 500;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,122,255,0.3); transition: all 200ms ease;
}
.upload-file-btn:hover { transform: translateY(-1px); }

.table-wrap { background: var(--apple-surface); border-radius: 16px; overflow: hidden; box-shadow: var(--apple-shadow); }
.row-actions { display: flex; gap: 4px; }
.action-btn { border: none; background: var(--apple-fill-tertiary); color: var(--apple-text); border-radius: 10px; font-weight: 500; }
.action-btn:hover { background: var(--apple-fill); }
.action-btn.danger { color: var(--apple-red); }
.action-btn.danger:hover { background: rgba(255,59,48,0.08); }
.version-text, .meta-text { font-size: 14px; color: var(--apple-text-secondary); }
.time-text { font-size: 13px; color: var(--apple-text-tertiary); }

.version-list { display: flex; flex-direction: column; }
.version-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 0.5px solid var(--apple-separator); }
.version-item:last-child { border-bottom: none; }
.version-label { font-weight: 600; font-size: 15px; color: var(--apple-text); }
.version-meta { font-size: 13px; color: var(--apple-text-secondary); margin-left: 10px; }
.version-right { display: flex; align-items: center; gap: 8px; }
</style>
