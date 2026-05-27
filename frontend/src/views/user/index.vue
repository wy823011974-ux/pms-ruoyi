<template>
  <div>
    <div class="flex justify-between items-center mb-4">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新建用户
      </el-button>
    </div>

    <el-table :data="users" border v-loading="loading" class="user-table">
      <el-table-column prop="displayName" label="姓名" min-width="90" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="department" label="部门" min-width="100" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'super_admin'" type="danger" size="small">超级管理员</el-tag>
          <el-select v-else v-model="row.role" size="small" @change="(v) => changeRole(row, v)" style="width:85px">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '待审批' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }"><span class="text-xs text-gray-500">{{ row.createTime?.substring(0, 10) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role !== 'super_admin'">
            <el-dropdown trigger="click" @command="(cmd) => handleUserAction(cmd, row)">
              <el-button size="small" class="action-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon> 编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="toggle">
                    <el-icon><SwitchButton /></el-icon> {{ row.status ? '禁用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="reset" divided>
                    <el-icon><Lock /></el-icon> 重置密码
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <span class="text-red-500"><el-icon><Delete /></el-icon> 删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <span v-else class="text-gray-300 text-xs">受保护</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!users.length && !loading" description="暂无用户，点击上方按钮新建" />

    <!-- 新建/编辑弹窗 -->
    <el-dialog :title="editing ? '编辑用户' : '新建用户'" v-model="dialogVisible" width="480px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" :label-position="'right'">
        <el-form-item label="手机号" prop="phone" required>
          <el-input v-model="form.phone" :disabled="!!editing && isSelf" placeholder="11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName" required>
          <el-input v-model="form.displayName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="department" required>
          <el-input v-model="form.department" placeholder="所属部门或单位" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email" required>
          <el-input v-model="form.email" placeholder="email@example.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editing" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少8位" />
        </el-form-item>
        <el-form-item label="角色" prop="role" v-if="!editing">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog title="重置密码" v-model="resetVisible" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码，至少8位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listUsers, createUser, updateUser, deleteUser, resetPassword } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled, Edit, SwitchButton, Lock, Delete } from '@element-plus/icons-vue'

const store = useUserStore()
const users = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const resetVisible = ref(false)
const editing = ref(null)
const newPassword = ref('')
const resetTarget = ref(null)
const formRef = ref(null)
const roles = [
  { value: 'admin', label: '管理员' },
  { value: 'manager', label: '负责人' },
  { value: 'member', label: '成员' },
  { value: 'customer', label: '客户' }
]

const isSelf = ref(false)
const form = reactive({ phone: '', displayName: '', department: '', email: '', password: '', role: 'member' })
const rules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请输入部门', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  password: [{ required: true, min: 8, message: '密码至少8位', trigger: 'blur' }]
}

async function loadUsers() {
  loading.value = true
  try { const r = await listUsers(); users.value = r.data?.rows || [] } catch { /* */ }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null; isSelf.value = false; resetForm(); dialogVisible.value = true
}
function openEdit(u) {
  editing.value = u.id; isSelf.value = store.userInfo?.id === u.id
  Object.assign(form, { phone: u.phone, displayName: u.displayName, department: u.department, email: u.email, role: u.role })
  dialogVisible.value = true
}
function resetForm() {
  Object.assign(form, { phone: '', displayName: '', department: '', email: '', password: '', role: 'member' })
  formRef.value?.resetFields()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editing.value) {
      await updateUser(editing.value, { displayName: form.displayName, department: form.department, email: form.email, phone: form.phone, role: form.role })
    } else {
      await createUser(form)
    }
    ElMessage.success('保存成功'); dialogVisible.value = false; loadUsers()
  } catch { /* validation or API error */ }
  finally { submitting.value = false }
}

async function changeRole(u, role) { await updateUser(u.id, { role }); ElMessage.success('角色已更新') }

async function handleUserAction(cmd, row) {
  switch (cmd) {
    case 'edit': openEdit(row); break
    case 'toggle': toggleStatus(row, !row.status); break
    case 'reset': resetTarget.value = row; newPassword.value = ''; resetVisible.value = true; break
    case 'delete':
      try {
        await ElMessageBox.confirm(`确定删除用户「${row.displayName}」？此操作不可恢复。`, '删除确认', {
          type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消'
        })
        await deleteUser(row.id); ElMessage.success('已删除'); loadUsers()
      } catch { /* 取消 */ }
      break
  }
}

async function toggleStatus(u, status) {
  await updateUser(u.id, { status: status ? 1 : 0 })
  ElMessage.success(status ? '已启用' : '已禁用'); loadUsers()
}

async function submitReset() {
  if (!newPassword.value || newPassword.value.length < 8) { ElMessage.warning('密码至少8位'); return }
  try {
    await resetPassword(resetTarget.value.id, { newPassword: newPassword.value })
    ElMessage.success('密码已重置'); resetVisible.value = false
  } catch { ElMessage.error('重置失败') }
}

onMounted(loadUsers)
</script>

<style scoped>
.user-table { font-size: 13px; }
.action-btn { border: none; color: var(--color-text-muted); padding: 4px 6px; }
.action-btn:hover { color: var(--color-primary); background: var(--color-primary-light); }
</style>
