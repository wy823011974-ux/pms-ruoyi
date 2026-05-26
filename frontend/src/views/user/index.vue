<template>
  <div>
    <div class="flex justify-between mb-4">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" @click="openCreate">+ 新建用户</el-button>
    </div>

    <el-table :data="users" border stripe v-loading="loading">
      <el-table-column label="#" width="50" type="index" />
      <el-table-column prop="displayName" label="姓名" min-width="90" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="department" label="部门" min-width="120" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.role==='super_admin'" type="danger" size="small">超级管理员</el-tag>
          <el-select v-else v-model="row.role" size="small" @change="(v) => changeRole(row, v)" style="width:85px">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{row}"><el-tag :type="row.status?'success':'danger'" size="small">{{ row.status?'启用':'待审批' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role !== 'super_admin'">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" v-if="row.status" type="warning" @click="toggleStatus(row, false)">禁用</el-button>
            <el-button size="small" v-else type="success" @click="toggleStatus(row, true)">审批</el-button>
            <el-popconfirm title="确定重置密码?" @confirm="doReset(row)"><template #reference><el-button size="small" type="warning">重置密码</el-button></template></el-popconfirm>
            <el-popconfirm title="确定删除?" @confirm="doDelete(row)"><template #reference><el-button size="small" type="danger">删除</el-button></template></el-popconfirm>
          </template>
          <span v-else class="text-gray-400 text-xs">配置文件管理</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog :title="editing?'编辑用户':'新建用户'" v-model="dialogVisible" width="500px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" :disabled="editing && isSelf" placeholder="11位手机号" /></el-form-item>
        <el-form-item label="姓名" prop="displayName"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="部门" prop="department"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editing"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="角色" prop="role" v-if="!editing">
          <el-select v-model="form.role"><el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog title="重置密码" v-model="resetVisible" width="400px">
      <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
      <template #footer><el-button @click="resetVisible=false">取消</el-button><el-button type="primary" @click="submitReset">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listUsers, createUser, updateUser, deleteUser, resetPassword } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const resetVisible = ref(false)
const editing = ref(null)
const newPassword = ref('')
const resetTarget = ref(null)
const formRef = ref(null)
const roles = [{value:'admin',label:'管理员'},{value:'manager',label:'负责人'},{value:'member',label:'成员'},{value:'customer',label:'客户'}]

const isSelf = ref(false)
const form = reactive({ phone:'', displayName:'', department:'', email:'', password:'', role:'member' })
const rules = { phone:[{required:true,pattern:/^1[3-9]\d{9}$/,message:'正确手机号'}], displayName:[{required:true,message:'必填'}], department:[{required:true,message:'必填'}], email:[{required:true,type:'email'}] }

async function loadUsers() { loading.value=true; try {const r=await listUsers();users.value=r.data.rows||[]} catch{} finally{loading.value=false} }

function openCreate() { editing.value=null; isSelf.value=false; resetForm(); dialogVisible.value=true }
function openEdit(u) { editing.value=u.id; isSelf.value=store.userInfo?.id===u.id; Object.assign(form,{phone:u.phone,displayName:u.displayName,department:u.department,email:u.email,role:u.role}); dialogVisible.value=true }
function resetForm() { Object.assign(form,{phone:'',displayName:'',department:'',email:'',password:'',role:'member'}); formRef.value?.resetFields() }

async function submit() {
  await formRef.value.validate()
  if(editing.value){await updateUser(editing.value,{displayName:form.displayName,department:form.department,email:form.email,phone:form.phone,role:form.role})}
  else{await createUser(form)}
  ElMessage.success('保存成功'); dialogVisible.value=false; loadUsers()
}

async function changeRole(u, role) { await updateUser(u.id, {role}); ElMessage.success('角色已更新') }
async function toggleStatus(u, status) { await updateUser(u.id, {status: status?1:0}); ElMessage.success(status?'已启用':'已禁用'); loadUsers() }
function doReset(u) { resetTarget.value=u; newPassword.value=''; resetVisible.value=true }
async function submitReset() { await resetPassword(resetTarget.value.id, {newPassword:newPassword.value}); ElMessage.success('已重置'); resetVisible.value=false }
async function doDelete(u) { await deleteUser(u.id); ElMessage.success('已删除'); loadUsers() }

onMounted(loadUsers)
</script>
