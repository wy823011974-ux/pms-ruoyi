<template>
  <div>
    <div class="flex justify-between mb-4"><h1 class="page-title">项目类型配置</h1><el-button type="primary" @click="openCreateType">+ 新建类型</el-button></div>

    <el-row :gutter="16">
      <el-col :span="8" v-for="pt in types" :key="pt.id">
        <el-card>
          <template #header><div class="flex justify-between items-center"><span class="font-semibold">{{ pt.name }}</span><code class="text-xs text-gray-400">{{ pt.code }}</code></div></template>
          <div v-if="pt.description" class="text-xs text-gray-500 mb-2">{{ pt.description }}</div>
          <div class="flex gap-1">
            <el-button size="small" @click="openConfig(pt)">文件类型配置</el-button>
            <el-popconfirm title="删除?" @confirm="doDeleteType(pt)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="新建类型" v-model="typeDialog" width="400px">
      <el-form :model="typeForm" label-width="60px">
        <el-form-item label="名称"><el-input v-model="typeForm.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="typeForm.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="typeForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="typeDialog=false">取消</el-button><el-button type="primary" @click="submitType">保存</el-button></template>
    </el-dialog>

    <!-- File Type Config Drawer -->
    <el-drawer v-model="ftcVisible" title="文件类型配置" size="700px" direction="rtl">
      <div class="px-4">
        <div class="flex justify-between mb-3"><span class="text-sm text-gray-500">共 {{ ftcList.length }} 个文件类型</span><el-button size="small" type="primary" @click="openCreateFtc">+ 新增</el-button></div>
        <el-table :data="ftcList" border size="small">
          <el-table-column label="#" width="40" type="index" />
          <el-table-column prop="name" label="名称" />
          <el-table-column label="Sheet" width="130"><template #default="{row}"><span class="text-xs text-purple-600">{{ row.sheetName||'(第一个)' }}</span></template></el-table-column>
          <el-table-column label="跳过行" width="70"><template #default="{row}"><el-tag size="small">{{ row.skipRows }}行</el-tag></template></el-table-column>
          <el-table-column label="字段" width="70"><template #default="{row}"><el-tag v-if="row.hasFields" size="small" type="success" @click="openFields(row)" class="cursor-pointer">字段</el-tag><span v-else class="text-gray-300">-</span></template></el-table-column>
          <el-table-column label="操作" width="120"><template #default="{row}"><el-button size="small" @click="openEditFtc(row)">编辑</el-button><el-popconfirm title="删除?" @confirm="doDeleteFtc(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm></template></el-table-column>
        </el-table>

        <!-- Field Sub-Drawer -->
        <el-drawer v-model="fieldVisible" title="字段配置" size="500px" direction="rtl" :append-to-body="true">
          <div class="px-4">
            <el-button size="small" type="primary" @click="openCreateField">+ 新增字段</el-button>
            <el-table :data="fieldList" border size="small" class="mt-3">
              <el-table-column label="#" width="40" type="index" />
              <el-table-column prop="fieldLabel" label="名称" min-width="100" />
              <el-table-column prop="fieldType" label="类型" width="60" />
              <el-table-column label="必填" width="50"><template #default="{row}"><span v-if="row.isRequired" class="text-red-500">必填</span></template></el-table-column>
              <el-table-column label="操作" width="100"><template #default="{row}"><el-button size="small" @click="openEditField(row)">编辑</el-button><el-popconfirm title="删除?" @confirm="doDeleteField(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm></template></el-table-column>
            </el-table>
          </div>
        </el-drawer>
      </div>
    </el-drawer>

    <!-- FTC Dialog -->
    <el-dialog :title="'文件类型'" v-model="ftcDialog" width="450px">
      <el-form :model="ftcForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="ftcForm.name" /></el-form-item>
        <el-form-item label="Sheet名"><el-input v-model="ftcForm.sheetName" placeholder="留空=第一个Sheet" /></el-form-item>
        <el-form-item label="跳过行"><el-input-number v-model="ftcForm.skipRows" :min="0" /></el-form-item>
        <el-form-item label="字段校验"><el-switch v-model="ftcForm.hasFields" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="ftcDialog=false">取消</el-button><el-button type="primary" @click="submitFtc">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listProjectTypes, createProjectType, deleteProjectType, listFileTypes, createFileType, deleteFileType, listFields, createField, updateField, deleteField } from '@/api/project'
import { ElMessage } from 'element-plus'

const types = ref([]); const typeDialog = ref(false)
const typeForm = reactive({ name:'', code:'', description:'' })

const ftcVisible = ref(false); const currentType = ref(null)
const ftcList = ref([]); const ftcDialog = ref(false); const ftcEditing = ref(null)
const ftcForm = reactive({ name:'', sheetName:'', skipRows:0, hasFields:false })

const fieldVisible = ref(false); const currentFtc = ref(null)
const fieldList = ref([])

async function loadTypes() { try{const r=await listProjectTypes();types.value=r.data||[]}catch{} }
function openCreateType() { typeDialog.value=true }
async function submitType() { await createProjectType(typeForm); ElMessage.success('已创建'); typeDialog.value=false; loadTypes() }
async function doDeleteType(pt) { await deleteProjectType(pt.id); ElMessage.success('已删除'); loadTypes() }

async function openConfig(pt) { currentType.value=pt; ftcVisible.value=true; await loadFtcList() }
async function loadFtcList() { try{const r=await listFileTypes(currentType.value.id);ftcList.value=r.data||[]}catch{} }
function openCreateFtc() { ftcEditing.value=null; Object.assign(ftcForm,{name:'',sheetName:'',skipRows:0,hasFields:false}); ftcDialog.value=true }
function openEditFtc(f) { ftcEditing.value=f.id; Object.assign(ftcForm,{name:f.name,sheetName:f.sheetName,skipRows:f.skipRows,hasFields:f.hasFields}); ftcDialog.value=true }
async function submitFtc() {
  if(ftcEditing.value){await createFileType(currentType.value.id,ftcForm)}else{await createFileType(currentType.value.id,ftcForm)}
  ElMessage.success('已保存'); ftcDialog.value=false; loadFtcList()
}
async function doDeleteFtc(f) { await deleteFileType(currentType.value.id, f.id); ElMessage.success('已删除'); loadFtcList() }

async function openFields(ftc) { currentFtc.value=ftc; fieldVisible.value=true; await loadFieldList() }
async function loadFieldList() { try{const r=await listFields(currentType.value.id,currentFtc.value.id);fieldList.value=r.data||[]}catch{} }
function openCreateField() { /* Simple dialog to add field */ }
function openEditField(f) {}
async function doDeleteField(f) { await deleteField(currentType.value.id, currentFtc.value.id, f.id); ElMessage.success('已删除'); loadFieldList() }

onMounted(loadTypes)
</script>
