<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">调查数据管理</h1>
      <div class="flex gap-2">
        <el-button v-if="uploadFtcId" type="warning" @click="handleClear">清空数据</el-button>
        <el-button type="success" @click="handleExport" :disabled="!rows.length">导出 Excel</el-button>
        <el-button @click="$router.push('/projects')">返回项目</el-button>
      </div>
    </div>

    <!-- 上传区域 -->
    <el-card class="mb-4">
      <div class="flex gap-2 items-center flex-wrap">
        <el-select v-model="uploadFtcId" placeholder="选择数据类型" size="small" style="width:200px" @change="onFtcChange">
          <el-option v-for="ft in fileTypes" :key="ft.id" :label="ft.name + ' (跳过' + ft.skipRows + '行)'" :value="ft.id" />
        </el-select>
        <el-input-number v-model="uploadSkipRows" :min="0" size="small" style="width:80px" title="跳过行数" />
        <el-checkbox v-model="uploadAsNew">新增上传（保留旧数据）</el-checkbox>
        <label class="el-button el-button--primary el-button--small" style="cursor:pointer">
          <input type="file" @change="handleUpload" accept=".xlsx,.xls" hidden /> 上传数据
        </label>
      </div>
      <div v-if="uploadFtcId && total > 0" class="mt-2 text-xs text-amber-600 bg-amber-50 p-2 rounded">
        当前类型已有 {{ total }} 条数据，再次上传默认覆盖。勾选"新增上传"可保留旧数据。
      </div>
      <div class="mt-1 text-xs text-gray-400">支持 .xlsx / .xls，最大 50MB，上传自动校验并脱敏入库</div>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="rows" border size="small" v-loading="loading" max-height="500">
        <el-table-column prop="projectCode" label="项目编号" width="160" />
        <el-table-column prop="projectYear" label="年份" width="70" />
        <el-table-column prop="projectLocation" label="地区" width="120" />
        <el-table-column v-for="key in dataKeys" :key="key" :prop="'rowData.' + key" :label="key" min-width="100" show-overflow-tooltip />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }"><span class="text-xs">{{ row.createdAt }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除?" @confirm="doDelete(row)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length && !loading" description="暂无数据，请先上传" />
      <div class="flex justify-between mt-3 text-sm text-gray-500" v-if="total > 0">
        <span>共 {{ total }} 条数据</span>
        <el-pagination v-model:currentPage="page" :page-size="50" :total="total" layout="prev, pager, next" @current-change="loadData" small />
      </div>
    </el-card>

    <!-- 上传历史 -->
    <el-card class="mt-4" v-if="histories.length">
      <template #header>上传历史</template>
      <el-table :data="histories" border size="small">
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }"><span class="text-xs">{{ row.createdAt?.substring(0, 16) }}</span></template>
        </el-table-column>
        <el-table-column prop="originalFilename" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="结果" width="180">
          <template #default="{ row }">
            <span :class="row.status === 'success' ? 'text-green-600' : 'text-red-600'">
              {{ row.successRows }}/{{ row.totalRows }} 行 — {{ row.status === 'success' ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listProjects, listFileTypes } from '@/api/project'
import { uploadData, listSurveyData, deleteSurveyData, clearData, history, exportSurveyData } from '@/api/survey'
import { ElMessage } from 'element-plus'

const route = useRoute()
const pid = ref(Number(route.params.projectId))
const fileTypes = ref([])
const uploadFtcId = ref(null)
const uploadSkipRows = ref(0)
const uploadAsNew = ref(false)
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const histories = ref([])

const dataKeys = computed(() => {
  if (!rows.value.length) return []
  const keys = new Set()
  rows.value.forEach(r => {
    if (r.rowData) Object.keys(r.rowData).forEach(k => keys.add(k))
  })
  return [...keys]
})

async function loadFtc() {
  try {
    const r = await listProjects()
    const projs = r.data?.rows || []
    const p = projs.find(x => x.id === pid.value)
    if (p) {
      const r2 = await listFileTypes(p.projectTypeId)
      fileTypes.value = (r2.data || []).filter(f => f.hasFields)
    }
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const r = await listSurveyData(pid.value, page.value, 50, uploadFtcId.value || undefined)
    rows.value = r.data?.rows || []
    total.value = r.data?.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadHistory() {
  try {
    const r = await history(pid.value, uploadFtcId.value || undefined)
    histories.value = r.data || []
  } catch { /* ignore */ }
}

function onFtcChange(val) {
  const ft = fileTypes.value.find(f => f.id === val)
  if (ft) uploadSkipRows.value = ft.skipRows || 0
  if (!val) { rows.value = []; total.value = 0; histories.value = []; return }
  page.value = 1
  loadData()
  loadHistory()
}

async function handleUpload(e) {
  if (!uploadFtcId.value || !e.target.files?.length) return
  try {
    const r = await uploadData(pid.value, e.target, uploadFtcId.value, uploadSkipRows.value, !uploadAsNew.value)
    ElMessage.success(r.data?.message || '上传成功')
    e.target.value = ''
    loadData()
    loadHistory()
  } catch { ElMessage.error('上传失败，请检查文件格式') }
}

async function handleClear() {
  if (!confirm('确定清空当前类型全部数据？此操作不可恢复！')) return
  try {
    await clearData(pid.value, uploadFtcId.value)
    ElMessage.success('已清空')
    rows.value = []
    total.value = 0
    loadHistory()
  } catch { ElMessage.error('清空失败') }
}

async function handleExport() {
  if (!rows.value.length) { ElMessage.warning('没有数据可导出'); return }
  try {
    const blob = await exportSurveyData(pid.value, uploadFtcId.value || undefined)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `survey_data_${pid.value}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') }
}

async function doDelete(row) {
  try {
    await deleteSurveyData(pid.value, row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { ElMessage.error('删除失败') }
}

onMounted(() => { loadFtc() })
</script>
