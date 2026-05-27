<template>
  <div>
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h1 class="page-title">调查数据</h1>
        <el-button @click="$router.push('/projects')" class="back-btn">返回项目</el-button>
      </div>
      <div class="toolbar-right">
        <el-button v-if="uploadFtcId" @click="handleClear" class="action-outline danger">清空数据</el-button>
        <el-button type="primary" @click="handleExport" :disabled="!rows.length">
          <el-icon><Download /></el-icon>导出 Excel
        </el-button>
      </div>
    </div>

    <div class="upload-section">
      <div class="upload-controls">
        <el-select v-model="uploadFtcId" placeholder="选择数据类型" size="large" class="upload-select" @change="onFtcChange">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="large" class="upload-skip" title="跳过行数" />
        <el-checkbox v-model="uploadAsNew">新增上传</el-checkbox>
        <label class="upload-file-btn">
          <input type="file" @change="handleUpload" accept=".xlsx,.xls" hidden />
          <el-icon><Upload /></el-icon>选择文件
        </label>
      </div>
      <div v-if="uploadFtcId && total > 0" class="upload-hint">当前类型已有 {{ total }} 条数据，再次上传将覆盖。勾选"新增上传"可保留旧数据。</div>
      <div class="upload-desc">支持 .xlsx / .xls，最大 50MB，自动校验并脱敏入库</div>
    </div>

    <div class="table-wrap">
      <el-table :data="rows" size="large" v-loading="loading" max-height="520" class="apple-table">
        <el-table-column prop="projectCode" label="项目编号" width="170" />
        <el-table-column prop="projectYear" label="年份" width="70" />
        <el-table-column prop="projectLocation" label="地区" width="130" />
        <el-table-column v-for="key in dataKeys" :key="key" :prop="'rowData.' + key" :label="key" min-width="110" show-overflow-tooltip />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }"><span class="time-text">{{ row.createdAt }}</span></template>
        </el-table-column>
        <el-table-column label="" width="50" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除?" @confirm="doDelete(row)">
              <template #reference><el-button size="small" class="more-btn"><el-icon><Delete /></el-icon></el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length && !loading" description="暂无数据，请选择文件类型并上传" style="padding:40px 0" />
      <div class="table-footer" v-if="total > 0">
        <span class="total-text">共 {{ total }} 条数据</span>
        <el-pagination v-model:currentPage="page" :page-size="50" :total="total" layout="prev, pager, next" @current-change="loadData" small />
      </div>
    </div>

    <div class="history-section" v-if="histories.length">
      <div class="section-title">上传历史</div>
      <div class="table-wrap">
        <el-table :data="histories" size="large" class="apple-table">
          <el-table-column prop="createdAt" label="时间" width="170">
            <template #default="{ row }"><span class="time-text">{{ row.createdAt?.substring(0, 16) }}</span></template>
          </el-table-column>
          <el-table-column prop="originalFilename" label="文件名" min-width="250" show-overflow-tooltip />
          <el-table-column label="结果" width="200">
            <template #default="{ row }">
              <span :class="row.status === 'success' ? 'success-text' : 'error-text'">
                {{ row.successRows }}/{{ row.totalRows }} 行 — {{ row.status === 'success' ? '成功' : '失败' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes, listFields } from '@/api/project'
import { uploadData, listSurveyData, deleteSurveyData, clearData, history, exportSurveyData } from '@/api/survey'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, Delete } from '@element-plus/icons-vue'

const route = useRoute(); const pid = ref(Number(route.params.projectId))
const fileTypes = ref([]); const fieldOrder = ref([]); const uploadFtcId = ref(null)
const uploadSkipRows = ref(0); const uploadAsNew = ref(false)
const rows = ref([]); const total = ref(0); const loading = ref(false); const page = ref(1); const histories = ref([])

const dataKeys = computed(() => {
  if (!rows.value.length) return []
  const keys = new Set(); rows.value.forEach(r => { if (r.rowData) Object.keys(r.rowData).forEach(k => keys.add(k)) })
  if (fieldOrder.value.length) { const ordered = fieldOrder.value.filter(k => keys.has(k)); for (const k of keys) { if (!ordered.includes(k)) ordered.push(k) }; return ordered }
  return [...keys]
})

async function loadFtc() { try { const r = await listProjects(); const projs = r.data?.rows || []; const p = projs.find(x => x.id === pid.value); if (p) { const r2 = await listFileTypes(p.projectTypeId); fileTypes.value = (r2.data || []).filter(f => f.hasFields) } } catch {} }
async function loadFields(ftcId) { if (!ftcId) { fieldOrder.value = []; return }; try { const r = await listProjects(); const projs = r.data?.rows || []; const p = projs.find(x => x.id === pid.value); if (!p) return; const fields = await listFields(p.projectTypeId, ftcId); fieldOrder.value = (fields.data || []).filter(f => f.isActive).map(f => f.fieldLabel) } catch { fieldOrder.value = [] } }
async function loadData() { loading.value = true; try { const r = await listSurveyData(pid.value, page.value, 50, uploadFtcId.value || undefined); rows.value = r.data?.rows || []; total.value = r.data?.total || 0 } catch {} finally { loading.value = false } }
async function loadHistory() { try { const r = await history(pid.value, uploadFtcId.value || undefined); histories.value = r.data || [] } catch {} }
function onFtcChange(val) { const ft = fileTypes.value.find(f => f.id === val); if (ft) uploadSkipRows.value = ft.skipRows || 0; if (!val) { rows.value = []; total.value = 0; histories.value = []; fieldOrder.value = []; return }; page.value = 1; loadFields(val); loadData(); loadHistory() }

async function handleUpload(e) { if (!uploadFtcId.value || !e.target.files?.length) return; try { const r = await uploadData(pid.value, e.target, uploadFtcId.value, uploadSkipRows.value, !uploadAsNew.value); ElMessage.success(r.data?.message || '上传成功'); e.target.value = ''; loadData(); loadHistory() } catch (err) { ElMessage.error(err?.response?.data?.msg || err?.message || '上传失败') } }
async function handleClear() { try { await ElMessageBox.confirm('确定清空当前类型全部数据？', '警告', { type:'warning', confirmButtonText:'清空' }); await clearData(pid.value, uploadFtcId.value); ElMessage.success('已清空'); rows.value = []; total.value = 0; loadHistory() } catch {} }
async function handleExport() { if (!rows.value.length) { ElMessage.warning('没有数据可导出'); return }; try { const blob = await exportSurveyData(pid.value, uploadFtcId.value || undefined); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `survey_data_${pid.value}_${new Date().toISOString().slice(0,10)}.xlsx`; a.click(); URL.revokeObjectURL(url); ElMessage.success('导出成功') } catch { ElMessage.error('导出失败') } }
async function doDelete(row) { try { await deleteSurveyData(pid.value, row.id); ElMessage.success('已删除'); loadData() } catch { ElMessage.error('删除失败') } }
onMounted(loadFtc)
</script>

<style scoped>
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 12px; }
.back-btn { background: var(--apple-fill-tertiary); border: none; color: var(--apple-text); }
.back-btn:hover { background: var(--apple-fill); }
.action-outline.danger { background: transparent; color: var(--apple-red); }

.upload-section { background: var(--apple-surface); border-radius: 16px; padding: 20px 24px; margin-bottom: 20px; box-shadow: var(--apple-shadow); }
.upload-controls { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.upload-select { width: 220px; }
.upload-skip { width: 80px; }
.upload-file-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 10px 18px; background: var(--apple-blue); color: #fff;
  border-radius: 12px; font-size: 15px; font-weight: 500; cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,122,255,0.3); transition: all 200ms ease;
}
.upload-file-btn:hover { background: var(--apple-blue-hover); transform: translateY(-1px); }
.upload-hint { margin-top: 12px; padding: 10px 14px; border-radius: 10px; background: #FFF9F0; color: var(--apple-orange); font-size: 13px; }
.upload-desc { margin-top: 8px; font-size: 12px; color: var(--apple-text-tertiary); }

.table-wrap { background: var(--apple-surface); border-radius: 16px; overflow: hidden; box-shadow: var(--apple-shadow); }
.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; border-top: 0.5px solid var(--apple-separator); }
.total-text { font-size: 14px; color: var(--apple-text-secondary); }

.history-section { margin-top: 28px; }
.section-title { font-size: 17px; font-weight: 600; color: var(--apple-text); margin-bottom: 12px; }
.success-text { color: var(--apple-green); font-size: 13px; }
.error-text { color: var(--apple-red); font-size: 13px; }
.time-text { font-size: 13px; color: var(--apple-text-secondary); }
.more-btn { border: none; color: var(--apple-text-tertiary); background: transparent; }
.more-btn:hover { color: var(--apple-red); background: rgba(255,59,48,0.06); }
</style>
