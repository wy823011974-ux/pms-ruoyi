<template>
  <div>
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h1 class="page-title">项目管理</h1>
        <el-select v-model="filter.status" placeholder="全部状态" clearable class="filter-select" @change="load">
          <el-option v-for="s in ['IN_PROGRESS','COMPLETED','ARCHIVED']" :key="s" :label="s==='IN_PROGRESS'?'进行中':s==='COMPLETED'?'已完结':'已归档'" :value="s" />
        </el-select>
        <el-input v-model="filter.keyword" placeholder="搜索" clearable class="filter-input" @keyup.enter="load" />
        <el-checkbox v-model="filter.showDeleted" @change="load">显示已删除</el-checkbox>
      </div>
      <el-button type="primary" size="large" @click="openCreate">
        <el-icon><Plus /></el-icon>新建项目
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="p in projects" :key="p.id">
        <div class="project-card" :class="{ deleted: p.isDeleted }">
          <div class="card-top">
            <div class="card-title-row">
              <span class="card-title">{{ p.name }}</span>
              <el-tag v-if="p.isDeleted" type="danger" size="small">已删除</el-tag>
              <el-tag v-else :type="p.status==='IN_PROGRESS'?'':p.status==='COMPLETED'?'success':'info'" size="small" effect="plain">{{ p.status==='IN_PROGRESS'?'进行中':p.status==='COMPLETED'?'已完结':'已归档' }}</el-tag>
            </div>
            <div class="card-uploads" v-if="!p.isDeleted">
              <el-button size="small" class="upload-btn" @click="$router.push('/files/'+p.id)">
                <el-icon><Upload /></el-icon>文件
              </el-button>
              <el-button size="small" class="upload-btn primary" @click="$router.push('/survey/'+p.id)">
                <el-icon><Upload /></el-icon>数据
              </el-button>
            </div>
          </div>
          <div class="card-meta">
            <code>{{ p.code }}</code>
            <span class="meta-text">{{ p.location }} · {{ p.year }}</span>
          </div>
          <div class="card-actions" v-if="!p.isDeleted">
            <template v-if="p.status==='IN_PROGRESS'">
              <el-button size="small" class="action-btn" @click="changeStatus(p,'COMPLETED')">完结</el-button>
            </template>
            <template v-if="p.status==='COMPLETED'">
              <el-button size="small" class="action-btn" @click="changeStatus(p,'IN_PROGRESS')">重开</el-button>
              <el-button size="small" class="action-btn" @click="changeStatus(p,'ARCHIVED')">归档</el-button>
            </template>
            <template v-if="p.status==='ARCHIVED'">
              <el-button size="small" class="action-btn" @click="changeStatus(p,'IN_PROGRESS')">重开</el-button>
            </template>
            <el-button size="small" class="action-btn" @click="openEdit(p)">编辑</el-button>
            <el-popconfirm title="确定删除项目？" @confirm="doDelete(p)">
              <template #reference><el-button size="small" class="action-btn danger">删除</el-button></template>
            </el-popconfirm>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!projects.length" :description="filter.showDeleted ? '没有已删除的项目' : '暂无项目，点击上方按钮新建'" style="margin-top:60px" />

    <el-dialog :title="editing ? '编辑项目' : '新建项目'" v-model="dialogVisible" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="项目名称"><el-input v-model="form.name" placeholder="如：2024年昌江县体育场地普查" /></el-form-item>
        <el-form-item label="调查年份"><el-input-number v-model="form.year" :min="2020" :max="2030" style="width:100%" /></el-form-item>
        <el-form-item label="调查地区"><el-input v-model="form.location" placeholder="如：海南省海口市" /></el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="form.projectTypeId" placeholder="选择项目类型" style="width:100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="dialog-cancel">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listProjects, createProject, updateProject, deleteProject, updateStatus, listProjectTypes } from '@/api/project'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'

const projects = ref([]); const types = ref([]); const dialogVisible = ref(false); const editing = ref(null)
const filter = reactive({ status: '', keyword: '', showDeleted: false })
const form = reactive({ name: '', year: 2026, location: '海南省', projectTypeId: null })

async function load() {
  const r = await listProjects({ status: filter.status || undefined, keyword: filter.keyword || undefined, deleted: filter.showDeleted || undefined })
  projects.value = r.data?.rows || []
}
async function loadTypes() { try { const r = await listProjectTypes(); types.value = r.data || [] } catch {} }
function openCreate() { editing.value = null; Object.assign(form, { name: '', year: 2026, location: '海南省', projectTypeId: null }); dialogVisible.value = true }
function openEdit(p) { editing.value = p.id; Object.assign(form, { name: p.name, year: p.year, location: p.location, projectTypeId: p.projectTypeId }); dialogVisible.value = true }
async function submit() { if (editing.value) { await updateProject(editing.value, form) } else { await createProject(form) }; ElMessage.success(editing.value ? '已更新' : '已创建'); dialogVisible.value = false; load() }
async function changeStatus(p, status) { await updateStatus(p.id, { status }); ElMessage.success('已更新'); load() }
async function doDelete(p) { try { await deleteProject(p.id); ElMessage.success('已删除'); load() } catch (e) { ElMessage.error(e.response?.data?.msg || '删除失败') } }
onMounted(() => { load(); loadTypes() })
</script>

<style scoped>
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 28px; gap: 16px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.filter-select { width: 120px; }
.filter-input { width: 180px; }

.project-card {
  background: var(--apple-surface); border-radius: 16px; padding: 22px 22px 18px;
  margin-bottom: 20px; box-shadow: var(--apple-shadow);
  transition: box-shadow 200ms ease, transform 200ms ease;
}
.project-card:hover { box-shadow: var(--apple-shadow-modal); transform: translateY(-2px); }
.project-card.deleted { opacity: 0.5; }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.card-title-row { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.card-title { font-size: 16px; font-weight: 600; color: var(--apple-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-uploads { display: flex; gap: 6px; flex-shrink: 0; }
.upload-btn { border: none; background: var(--apple-fill-tertiary); color: var(--apple-text); font-weight: 500; border-radius: 10px; }
.upload-btn:hover { background: var(--apple-fill); }
.upload-btn.primary { background: rgba(0,122,255,0.1); color: var(--apple-blue); }
.upload-btn.primary:hover { background: rgba(0,122,255,0.18); }

.card-meta { margin-bottom: 16px; }
.card-meta code { font-size: 12px; color: var(--apple-text-secondary); background: var(--apple-fill-tertiary); }
.meta-text { font-size: 13px; color: var(--apple-text-secondary); margin-left: 10px; }

.card-actions { display: flex; gap: 6px; flex-wrap: wrap; padding-top: 14px; border-top: 0.5px solid var(--apple-separator); }
.action-btn { border: none; background: var(--apple-fill-tertiary); color: var(--apple-text); border-radius: 10px; font-weight: 500; }
.action-btn:hover { background: var(--apple-fill); }
.action-btn.danger { color: var(--apple-red); }
.action-btn.danger:hover { background: rgba(255,59,48,0.08); }

.dialog-cancel { background: var(--apple-fill-tertiary); border: none; color: var(--apple-text); }
.dialog-cancel:hover { background: var(--apple-fill); }
</style>
