<template>
  <div>
    <div class="page-toolbar">
      <h1 class="page-title">项目类型配置</h1>
      <el-button type="primary" size="large" @click="openCreateType">
        <el-icon><Plus /></el-icon>新建类型
      </el-button>
    </div>

    <el-row :gutter="20" v-if="types.length">
      <el-col :span="8" v-for="pt in types" :key="pt.id">
        <div class="type-card">
          <div class="type-card-header">
            <div class="type-card-title">{{ pt.name }}</div>
            <code class="type-code">{{ pt.code }}</code>
          </div>
          <div v-if="pt.description" class="type-card-desc">{{ pt.description }}</div>
          <div class="type-card-actions">
            <el-button size="small" class="type-btn" @click="openConfig(pt)">配置文件类型</el-button>
            <el-button size="small" class="type-btn" @click="handleExportSchema(pt)">导出 Schema</el-button>
            <el-button size="small" class="type-btn" @click="openImportSchema(pt)">导入 Schema</el-button>
            <el-popconfirm title="确定删除?" @confirm="doDeleteType(pt)">
              <template #reference><el-button size="small" class="type-btn danger">删除</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!types.length" description="暂无项目类型" style="margin-top:60px" />

    <el-dialog title="新建项目类型" v-model="typeDialog" width="440px">
      <el-form :model="typeForm" label-position="top">
        <el-form-item label="类型名称"><el-input v-model="typeForm.name" placeholder="如：体育锻炼达标测验" /></el-form-item>
        <el-form-item label="类型编码"><el-input v-model="typeForm.code" placeholder="如：ti_yu_duan_lian" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="typeForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="typeDialog = false" class="dialog-cancel">取消</el-button><el-button type="primary" @click="submitType">保存</el-button></template>
    </el-dialog>

    <el-dialog title="导入 Schema" v-model="importDialog" width="560px">
      <p style="margin-bottom:12px;color:var(--apple-text-secondary);font-size:14px">粘贴从旧系统或其他环境导出的 Schema JSON</p>
      <el-input v-model="importJson" type="textarea" :rows="14" placeholder='{"schemas": [...]}' />
      <template #footer><el-button @click="importDialog = false" class="dialog-cancel">取消</el-button><el-button type="primary" @click="submitImport">导入</el-button></template>
    </el-dialog>

    <el-drawer v-model="ftcVisible" title="文件类型配置" size="700px" direction="rtl">
      <div class="drawer-toolbar">
        <span class="drawer-subtitle">{{ currentType?.name }} · {{ ftcList.length }} 个文件类型</span>
        <el-button type="primary" size="small" @click="openCreateFtc"><el-icon><Plus /></el-icon>新增</el-button>
      </div>
      <div class="ftc-list" v-if="ftcList.length">
        <div v-for="ft in ftcList" :key="ft.id" class="ftc-item">
          <div class="ftc-info">
            <div class="ftc-name">{{ ft.name }}</div>
            <code class="ftc-code">{{ ft.code }}</code>
            <div class="ftc-meta">
              <span>Sheet: {{ ft.sheetName || '第一个' }}</span>
              <span>跳过 {{ ft.skipRows }} 行</span>
              <el-tag v-if="ft.hasFields" size="small" type="success" effect="plain">字段校验</el-tag>
            </div>
          </div>
          <div class="ftc-actions">
            <el-button size="small" class="type-btn" @click="openFields(ft)">字段</el-button>
            <el-button size="small" class="type-btn" @click="openEditFtc(ft)">编辑</el-button>
            <el-popconfirm title="删除?" @confirm="doDeleteFtc(ft)">
              <template #reference><el-button size="small" class="type-btn danger">删除</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文件类型" />

      <el-drawer v-model="fieldVisible" title="字段配置" size="520px" direction="rtl" :append-to-body="true">
        <div class="drawer-toolbar">
          <span class="drawer-subtitle">{{ currentFtc?.name }} · {{ fieldList.length }} 个字段</span>
          <el-button type="primary" size="small" @click="openCreateField"><el-icon><Plus /></el-icon>新增字段</el-button>
        </div>
        <div class="field-list" v-if="fieldList.length">
          <div v-for="fd in fieldList" :key="fd.id" class="field-item">
            <div class="field-info">
              <div class="field-name">{{ fd.fieldLabel }}</div>
              <code class="field-key">{{ fd.fieldKey }}</code>
              <div class="field-meta">
                <el-tag size="small" effect="plain">{{ fd.fieldType }}</el-tag>
                <span v-if="fd.isRequired" class="required-mark">必填</span>
              </div>
            </div>
            <div class="field-actions">
              <el-button size="small" class="type-btn" @click="openEditField(fd)">编辑</el-button>
              <el-popconfirm title="删除?" @confirm="doDeleteField(fd)">
                <template #reference><el-button size="small" class="type-btn danger">删除</el-button></template>
              </el-popconfirm>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无字段" />
      </el-drawer>
    </el-drawer>

    <el-dialog :title="ftcEditing != null ? '编辑文件类型' : '新增文件类型'" v-model="ftcDialog" width="440px">
      <el-form :model="ftcForm" label-position="top">
        <el-form-item label="名称"><el-input v-model="ftcForm.name" placeholder="如：单项成绩表" /></el-form-item>
        <el-form-item label="Sheet 名称"><el-input v-model="ftcForm.sheetName" placeholder="留空=第一个Sheet" /></el-form-item>
        <el-form-item label="跳过行数"><el-input-number v-model="ftcForm.skipRows" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="字段校验"><el-switch v-model="ftcForm.hasFields" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="ftcForm.sortOrder" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="ftcDialog = false" class="dialog-cancel">取消</el-button><el-button type="primary" @click="submitFtc">保存</el-button></template>
    </el-dialog>

    <el-dialog :title="fieldEditing != null ? '编辑字段' : '新增字段'" v-model="fieldDialog" width="460px">
      <el-form :model="fieldForm" label-position="top">
        <el-form-item label="字段名称"><el-input v-model="fieldForm.fieldLabel" placeholder="如：姓名" /></el-form-item>
        <el-form-item label="字段标识"><el-input v-model="fieldForm.fieldKey" placeholder="如：xing_ming" /></el-form-item>
        <el-form-item label="字段类型">
          <el-select v-model="fieldForm.fieldType" style="width:100%">
            <el-option v-for="t in ['text','number','date','select']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="必填"><el-switch v-model="fieldForm.isRequired" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="启用"><el-switch v-model="fieldForm.isActive" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="fieldForm.sortOrder" :min="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="fieldDialog = false" class="dialog-cancel">取消</el-button><el-button type="primary" @click="submitField">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listProjectTypes, createProjectType, deleteProjectType, listFileTypes, createFileType, deleteFileType, listFields, createField, updateField, deleteField, exportSchema, importSchema } from '@/api/project'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const types = ref([]); const typeDialog = ref(false)
const typeForm = reactive({ name:'', code:'', description:'' })
const importDialog = ref(false); const importTypeId = ref(null); const importJson = ref('')
const ftcVisible = ref(false); const currentType = ref(null); const ftcList = ref([])
const ftcDialog = ref(false); const ftcEditing = ref(null)
const ftcForm = reactive({ name:'', sheetName:'', skipRows:0, hasFields:false, sortOrder:0 })
const fieldVisible = ref(false); const currentFtc = ref(null); const fieldList = ref([])
const fieldDialog = ref(false); const fieldEditing = ref(null)
const fieldForm = reactive({ fieldLabel:'', fieldKey:'', fieldType:'text', isRequired:false, sortOrder:0, isActive:true })

async function loadTypes() { try { const r = await listProjectTypes(); types.value = r.data || [] } catch {} }
function openCreateType() { Object.assign(typeForm, { name:'', code:'', description:'' }); typeDialog.value = true }
async function submitType() { await createProjectType({ ...typeForm }); ElMessage.success('已创建'); typeDialog.value = false; loadTypes() }
async function doDeleteType(pt) { await deleteProjectType(pt.id); ElMessage.success('已删除'); loadTypes() }

async function handleExportSchema(pt) { try { const r = await exportSchema(pt.id); const blob = new Blob([JSON.stringify(r.data, null, 2)], { type:'application/json' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `${pt.code}_schema.json`; a.click(); URL.revokeObjectURL(url); ElMessage.success('已导出') } catch { ElMessage.error('导出失败') } }
function openImportSchema(pt) { importTypeId.value = pt.id; importJson.value = ''; importDialog.value = true }
async function submitImport() { try { const data = JSON.parse(importJson.value); const r = await importSchema(importTypeId.value, data); ElMessage.success(`导入成功：${r.data?.fileTypesCreated||0} 新建，${r.data?.fileTypesUpdated||0} 更新，${r.data?.fieldsCreated||0} 字段`); importDialog.value = false } catch (e) { ElMessage.error('导入失败：JSON 格式不正确') } }

async function openConfig(pt) { currentType.value = pt; ftcVisible.value = true; await loadFtcList() }
async function loadFtcList() { try { const r = await listFileTypes(currentType.value.id); ftcList.value = r.data || [] } catch {} }
function openCreateFtc() { ftcEditing.value = null; Object.assign(ftcForm, { name:'', sheetName:'', skipRows:0, hasFields:false, sortOrder:0 }); ftcDialog.value = true }
function openEditFtc(f) { ftcEditing.value = f.id; Object.assign(ftcForm, { name:f.name, sheetName:f.sheetName, skipRows:f.skipRows, hasFields:!!f.hasFields, sortOrder:f.sortOrder||0 }); ftcDialog.value = true }
async function submitFtc() { await createFileType(currentType.value.id, { ...ftcForm }); ElMessage.success('已保存'); ftcDialog.value = false; loadFtcList() }
async function doDeleteFtc(f) { await deleteFileType(currentType.value.id, f.id); ElMessage.success('已删除'); loadFtcList() }

async function openFields(ftc) { currentFtc.value = ftc; fieldVisible.value = true; await loadFieldList() }
async function loadFieldList() { try { const r = await listFields(currentType.value.id, currentFtc.value.id); fieldList.value = r.data || [] } catch {} }
function openCreateField() { fieldEditing.value = null; Object.assign(fieldForm, { fieldLabel:'', fieldKey:'', fieldType:'text', isRequired:false, sortOrder:0, isActive:true }); fieldDialog.value = true }
function openEditField(f) { fieldEditing.value = f.id; Object.assign(fieldForm, { fieldLabel:f.fieldLabel, fieldKey:f.fieldKey, fieldType:f.fieldType, isRequired:!!f.isRequired, sortOrder:f.sortOrder||0, isActive:!!f.isActive }); fieldDialog.value = true }
async function submitField() { const data = { ...fieldForm, isRequired:fieldForm.isRequired?1:0, isActive:fieldForm.isActive?1:0 }; if (fieldEditing.value != null) { await updateField(currentType.value.id, currentFtc.value.id, fieldEditing.value, data) } else { await createField(currentType.value.id, currentFtc.value.id, data) }; ElMessage.success('已保存'); fieldDialog.value = false; loadFieldList() }
async function doDeleteField(f) { await deleteField(currentType.value.id, currentFtc.value.id, f.id); ElMessage.success('已删除'); loadFieldList() }
onMounted(loadTypes)
</script>

<style scoped>
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 28px; }

.type-card {
  background: var(--apple-surface); border-radius: 16px; padding: 22px; margin-bottom: 20px;
  box-shadow: var(--apple-shadow); transition: box-shadow 200ms ease, transform 200ms ease;
}
.type-card:hover { box-shadow: var(--apple-shadow-modal); transform: translateY(-2px); }
.type-card-header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 8px; }
.type-card-title { font-size: 17px; font-weight: 600; color: var(--apple-text); }
.type-code { font-size: 12px; color: var(--apple-text-tertiary); background: var(--apple-fill-tertiary); padding: 2px 8px; border-radius: 6px; }
.type-card-desc { font-size: 14px; color: var(--apple-text-secondary); margin-bottom: 16px; }
.type-card-actions { display: flex; gap: 6px; flex-wrap: wrap; padding-top: 14px; border-top: 0.5px solid var(--apple-separator); }
.type-btn { border: none; background: var(--apple-fill-tertiary); color: var(--apple-text); border-radius: 10px; font-weight: 500; }
.type-btn:hover { background: var(--apple-fill); }
.type-btn.danger { color: var(--apple-red); }
.type-btn.danger:hover { background: rgba(255,59,48,0.08); }

.drawer-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.drawer-subtitle { font-size: 14px; color: var(--apple-text-secondary); }

.ftc-item, .field-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 16px; margin-bottom: 8px; background: var(--apple-bg); border-radius: 12px;
}
.ftc-name, .field-name { font-size: 15px; font-weight: 600; color: var(--apple-text); }
.ftc-code, .field-key { font-size: 12px; color: var(--apple-text-tertiary); margin: 2px 0 6px; display: block; }
.ftc-meta, .field-meta { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--apple-text-secondary); }
.required-mark { color: var(--apple-red); font-size: 13px; font-weight: 500; }
.ftc-actions, .field-actions { display: flex; gap: 4px; flex-shrink: 0; }

.dialog-cancel { background: var(--apple-fill-tertiary); border: none; color: var(--apple-text); }
.dialog-cancel:hover { background: var(--apple-fill); }
</style>
