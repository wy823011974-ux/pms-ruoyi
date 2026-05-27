<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">项目类型配置</h1>
      <el-button type="primary" @click="openCreateType">+ 新建类型</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="8" v-for="pt in types" :key="pt.id">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-semibold">{{ pt.name }}</span>
              <code class="text-xs text-gray-400">{{ pt.code }}</code>
            </div>
          </template>
          <div v-if="pt.description" class="text-xs text-gray-500 mb-3">{{ pt.description }}</div>
          <div class="flex gap-1 flex-wrap">
            <el-button size="small" @click="openConfig(pt)">文件类型配置</el-button>
            <el-button size="small" @click="handleExportSchema(pt)">导出Schema</el-button>
            <el-button size="small" @click="openImportSchema(pt)">导入Schema</el-button>
            <el-popconfirm title="删除项目类型及其下所有配置?" @confirm="doDeleteType(pt)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!types.length" description="暂无项目类型，请点击上方按钮创建" />

    <!-- 创建/编辑类型弹窗 -->
    <el-dialog :title="typeEditing ? '编辑类型' : '新建类型'" v-model="typeDialog" width="450px">
      <el-form :model="typeForm" label-width="60px">
        <el-form-item label="名称"><el-input v-model="typeForm.name" placeholder="如：体育锻炼达标测验" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="typeForm.code" placeholder="拼音，如：ti_yu_duan_lian" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="typeForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入Schema弹窗 -->
    <el-dialog title="导入 Schema JSON" v-model="importDialog" width="600px">
      <p class="text-sm text-gray-500 mb-3">粘贴从旧系统或其他环境导出的 Schema JSON</p>
      <el-input v-model="importJson" type="textarea" :rows="15" placeholder='{"schemas": [...]}' />
      <template #footer>
        <el-button @click="importDialog = false">取消</el-button>
        <el-button type="primary" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- 文件类型配置抽屉 -->
    <el-drawer v-model="ftcVisible" title="文件类型配置" size="750px" direction="rtl">
      <div class="px-4">
        <div class="flex justify-between mb-3">
          <span class="text-sm text-gray-500">项目类型：{{ currentType?.name }} · 共 {{ ftcList.length }} 个文件类型</span>
          <el-button size="small" type="primary" @click="openCreateFtc">+ 新增</el-button>
        </div>
        <el-table :data="ftcList" border size="small">
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column prop="code" label="编码" width="120"><template #default="{ row }"><code class="text-xs">{{ row.code }}</code></template></el-table-column>
          <el-table-column label="Sheet" width="130"><template #default="{ row }"><span class="text-xs text-purple-600">{{ row.sheetName || '(第一个Sheet)' }}</span></template></el-table-column>
          <el-table-column label="跳过行" width="75"><template #default="{ row }"><el-tag size="small">{{ row.skipRows }}行</el-tag></template></el-table-column>
          <el-table-column label="字段校验" width="80"><template #default="{ row }"><el-tag v-if="row.hasFields" size="small" type="success">已启用</el-tag><span v-else class="text-gray-300">-</span></template></el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openFields(row)">字段</el-button>
              <el-button size="small" @click="openEditFtc(row)">编辑</el-button>
              <el-popconfirm title="删除?" @confirm="doDeleteFtc(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!ftcList.length" description="暂无文件类型配置" />

        <!-- 字段定义子抽屉 -->
        <el-drawer v-model="fieldVisible" title="字段配置" size="550px" direction="rtl" :append-to-body="true">
          <div class="px-4">
            <div class="flex justify-between mb-3">
              <span class="text-sm text-gray-500">文件类型：{{ currentFtc?.name }} · {{ fieldList.length }} 个字段</span>
              <el-button size="small" type="primary" @click="openCreateField">+ 新增字段</el-button>
            </div>
            <el-table :data="fieldList" border size="small">
              <el-table-column prop="fieldLabel" label="名称" min-width="100" />
              <el-table-column prop="fieldKey" label="标识" width="100"><template #default="{ row }"><code class="text-xs">{{ row.fieldKey }}</code></template></el-table-column>
              <el-table-column prop="fieldType" label="类型" width="60" />
              <el-table-column label="必填" width="55"><template #default="{ row }"><span v-if="row.isRequired" class="text-red-500 text-xs">必填</span><span v-else class="text-gray-300 text-xs">-</span></template></el-table-column>
              <el-table-column label="操作" width="110" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="openEditField(row)">编辑</el-button>
                  <el-popconfirm title="删除?" @confirm="doDeleteField(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!fieldList.length" description="暂无字段定义" />
          </div>
        </el-drawer>
      </div>
    </el-drawer>

    <!-- 文件类型弹窗 -->
    <el-dialog :title="ftcEditing != null ? '编辑文件类型' : '新增文件类型'" v-model="ftcDialog" width="450px">
      <el-form :model="ftcForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="ftcForm.name" placeholder="如：单项成绩表" /></el-form-item>
        <el-form-item label="Sheet名称"><el-input v-model="ftcForm.sheetName" placeholder="留空=第一个Sheet" /></el-form-item>
        <el-form-item label="跳过行数"><el-input-number v-model="ftcForm.skipRows" :min="0" /></el-form-item>
        <el-form-item label="字段校验"><el-switch v-model="ftcForm.hasFields" active-text="启用" inactive-text="关闭" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="ftcForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="ftcDialog = false">取消</el-button><el-button type="primary" @click="submitFtc">保存</el-button></template>
    </el-dialog>

    <!-- 字段弹窗 -->
    <el-dialog :title="fieldEditing != null ? '编辑字段' : '新增字段'" v-model="fieldDialog" width="500px">
      <el-form :model="fieldForm" label-width="80px">
        <el-form-item label="字段名称"><el-input v-model="fieldForm.fieldLabel" placeholder="如：姓名" /></el-form-item>
        <el-form-item label="字段标识"><el-input v-model="fieldForm.fieldKey" placeholder="如：xing_ming" /></el-form-item>
        <el-form-item label="字段类型">
          <el-select v-model="fieldForm.fieldType"><el-option v-for="t in ['text','number','date','select']" :key="t" :label="t" :value="t" /></el-select>
        </el-form-item>
        <el-form-item label="是否必填"><el-switch v-model="fieldForm.isRequired" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="fieldForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="fieldForm.isActive" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="fieldDialog = false">取消</el-button><el-button type="primary" @click="submitField">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  listProjectTypes, createProjectType, deleteProjectType,
  listFileTypes, createFileType, deleteFileType,
  listFields, createField, updateField, deleteField,
  exportSchema, importSchema
} from '@/api/project'
import { ElMessage } from 'element-plus'

// ===== 项目类型 =====
const types = ref([])
const typeDialog = ref(false)
const typeEditing = ref(null)
const typeForm = reactive({ name: '', code: '', description: '' })

// ===== Schema 导入导出 =====
const importDialog = ref(false)
const importTypeId = ref(null)
const importJson = ref('')

// ===== 文件类型 =====
const ftcVisible = ref(false)
const currentType = ref(null)
const ftcList = ref([])
const ftcDialog = ref(false)
const ftcEditing = ref(null)
const ftcForm = reactive({ name: '', sheetName: '', skipRows: 0, hasFields: false, sortOrder: 0 })

// ===== 字段 =====
const fieldVisible = ref(false)
const currentFtc = ref(null)
const fieldList = ref([])
const fieldDialog = ref(false)
const fieldEditing = ref(null)
const fieldForm = reactive({ fieldLabel: '', fieldKey: '', fieldType: 'text', isRequired: false, sortOrder: 0, isActive: true })

// ===== 类型操作 =====
async function loadTypes() { try { const r = await listProjectTypes(); types.value = r.data || [] } catch { /* */ } }

function openCreateType() { typeEditing.value = null; Object.assign(typeForm, { name: '', code: '', description: '' }); typeDialog.value = true }

async function submitType() {
  try {
    if (typeEditing.value) { /* TODO: update type */ }
    else { await createProjectType({ ...typeForm }) }
    ElMessage.success('已保存'); typeDialog.value = false; loadTypes()
  } catch { ElMessage.error('保存失败') }
}

async function doDeleteType(pt) { try { await deleteProjectType(pt.id); ElMessage.success('已删除'); loadTypes() } catch { ElMessage.error('删除失败') } }

// ===== Schema 操作 =====
async function handleExportSchema(pt) {
  try {
    const r = await exportSchema(pt.id)
    const blob = new Blob([JSON.stringify(r.data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = `${pt.code}_schema.json`; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Schema 已导出')
  } catch { ElMessage.error('导出失败') }
}

function openImportSchema(pt) { importTypeId.value = pt.id; importJson.value = ''; importDialog.value = true }

async function submitImport() {
  try {
    const data = JSON.parse(importJson.value)
    const r = await importSchema(importTypeId.value, data)
    ElMessage.success(r.data?.message || `导入成功：${r.data?.fileTypesCreated || 0} 个文件类型，${r.data?.fieldsCreated || 0} 个字段`)
    importDialog.value = false
  } catch (e) { ElMessage.error('导入失败：JSON 格式不正确') }
}

// ===== 文件类型操作 =====
async function openConfig(pt) { currentType.value = pt; ftcVisible.value = true; await loadFtcList() }
async function loadFtcList() { try { const r = await listFileTypes(currentType.value.id); ftcList.value = r.data || [] } catch { /* */ } }

function openCreateFtc() { ftcEditing.value = null; Object.assign(ftcForm, { name: '', sheetName: '', skipRows: 0, hasFields: false, sortOrder: 0 }); ftcDialog.value = true }
function openEditFtc(f) { ftcEditing.value = f.id; Object.assign(ftcForm, { name: f.name, sheetName: f.sheetName, skipRows: f.skipRows, hasFields: !!f.hasFields, sortOrder: f.sortOrder || 0 }); ftcDialog.value = true }

async function submitFtc() {
  try {
    if (ftcEditing.value != null) {
      await createFileType(currentType.value.id, { ...ftcForm, id: ftcEditing.value })
    } else {
      await createFileType(currentType.value.id, { ...ftcForm })
    }
    ElMessage.success('已保存'); ftcDialog.value = false; loadFtcList()
  } catch { ElMessage.error('保存失败') }
}

async function doDeleteFtc(f) { try { await deleteFileType(currentType.value.id, f.id); ElMessage.success('已删除'); loadFtcList() } catch { ElMessage.error('删除失败') } }

// ===== 字段操作 =====
async function openFields(ftc) { currentFtc.value = ftc; fieldVisible.value = true; await loadFieldList() }
async function loadFieldList() { try { const r = await listFields(currentType.value.id, currentFtc.value.id); fieldList.value = r.data || [] } catch { /* */ } }

function openCreateField() { fieldEditing.value = null; Object.assign(fieldForm, { fieldLabel: '', fieldKey: '', fieldType: 'text', isRequired: false, sortOrder: 0, isActive: true }); fieldDialog.value = true }
function openEditField(f) {
  fieldEditing.value = f.id
  Object.assign(fieldForm, { fieldLabel: f.fieldLabel, fieldKey: f.fieldKey, fieldType: f.fieldType, isRequired: !!f.isRequired, sortOrder: f.sortOrder || 0, isActive: !!f.isActive })
  fieldDialog.value = true
}

async function submitField() {
  try {
    const data = { ...fieldForm, isRequired: fieldForm.isRequired ? 1 : 0, isActive: fieldForm.isActive ? 1 : 0 }
    if (fieldEditing.value != null) {
      await updateField(currentType.value.id, currentFtc.value.id, fieldEditing.value, data)
    } else {
      await createField(currentType.value.id, currentFtc.value.id, data)
    }
    ElMessage.success('已保存'); fieldDialog.value = false; loadFieldList()
  } catch { ElMessage.error('保存失败') }
}

async function doDeleteField(f) { try { await deleteField(currentType.value.id, currentFtc.value.id, f.id); ElMessage.success('已删除'); loadFieldList() } catch { ElMessage.error('删除失败') } }

onMounted(loadTypes)
</script>
