<template>
  <section>
    <div class="campus-hero admin-campus-hero">
      <div class="hero-copy">
        <span class="motto-badge">管理员专属</span>
        <h1>用户管理</h1>
        <p>统一查看学生、活动负责人和管理员账号，按角色、校区和关键词快速定位用户。</p>
      </div>
      <el-button class="hero-button" :icon="Plus" @click="openCreateOrganizer">创建负责人账号</el-button>
    </div>

    <div class="metric-row campus-overview">
      <div class="metric metric-accent"><span>当前页用户</span><strong>{{ rows.length }}</strong></div>
      <div class="metric"><span>总用户数</span><strong>{{ pagination.total }}</strong></div>
      <div class="metric"><span>角色分类</span><strong>{{ filter.role ? userRoleText(filter.role) : '全部' }}</strong></div>
      <div class="metric"><span>校区筛选</span><strong>{{ filter.campus || '全部' }}</strong></div>
    </div>

    <div class="panel filter-panel">
      <div class="toolbar">
        <el-input v-model="filter.keyword" clearable placeholder="搜索账号、姓名、学号、工号、手机号" />
        <el-select v-model="filter.role" placeholder="角色">
          <el-option v-for="item in userRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filter.campus" placeholder="校区">
          <el-option label="全部" value="" />
          <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="filter.status" placeholder="状态">
          <el-option v-for="item in userStatusOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">筛选</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <div class="panel admin-table-panel" v-loading="loading">
      <div class="section-title">
        <div>
          <h2>用户列表</h2>
          <p>管理员可以维护负责人账号、账号状态和基础资料。公开注册入口只会创建学生账号。</p>
        </div>
      </div>

      <el-table class="desktop-table" :data="rows" stripe>
        <el-table-column prop="id" label="用户ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="studentNo" label="学号" min-width="130">
          <template #default="{ row }">{{ row.studentNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="workNo" label="工号" min-width="120">
          <template #default="{ row }">{{ row.workNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="gradeYear" label="年级" min-width="90">
          <template #default="{ row }">{{ row.gradeYear || '-' }}</template>
        </el-table-column>
        <el-table-column prop="majorName" label="专业" min-width="150">
          <template #default="{ row }">{{ row.majorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="campus" label="校区" min-width="120" />
        <el-table-column prop="college" label="学院/部门" min-width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="userRoleTagType(row.role)">{{ userRoleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag :type="userStatusTagType(row.status)">{{ userStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="270" fixed="right">
          <template #default="{ row }">
            <div class="table-actions action-cluster">
              <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" :type="row.status === 0 ? 'success' : 'warning'" @click="toggleStatus(row)">
                {{ row.status === 0 ? '启用' : '禁用' }}
              </el-button>
              <el-button size="small" @click="openResetPassword(row)">重置密码</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="mobile-card-list">
        <div v-for="row in rows" :key="row.id" class="panel manage-card agri-card">
          <div class="manage-card-head">
            <div>
              <span class="wheat-badge">{{ row.campus || '未填写校区' }}</span>
              <h3>{{ row.realName || row.username }}</h3>
              <p class="page-subtitle">{{ row.username }} · {{ row.studentNo || row.workNo || '无学号/工号' }}</p>
            </div>
            <el-tag :type="userRoleTagType(row.role)">{{ userRoleText(row.role) }}</el-tag>
          </div>
          <div class="meta-row">
            <span>{{ row.phone || '未填写手机号' }}</span>
            <el-tag :type="userStatusTagType(row.status)">{{ userStatusText(row.status) }}</el-tag>
          </div>
          <div class="button-row">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="toggleStatus(row)">{{ row.status === 0 ? '启用' : '禁用' }}</el-button>
            <el-button size="small" @click="openResetPassword(row)">重置密码</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && rows.length === 0" class="empty-wrap" description="暂无用户数据" />

      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :total="pagination.total"
          :page-size="pagination.size"
          :current-page="pagination.page"
          :page-sizes="[10, 20, 50]"
          @current-change="changePage"
          @size-change="changeSize"
        />
      </div>
    </div>

    <el-dialog v-model="organizerDialogVisible" title="创建负责人账号" width="520px">
      <el-form :model="organizerForm" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="organizerForm.realName" /></el-form-item>
        <el-form-item label="工号"><el-input v-model="organizerForm.workNo" placeholder="例如 T2024001，创建后作为登录账号" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="organizerForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="organizerForm.phone" /></el-form-item>
        <el-form-item label="校区">
          <el-select v-model="organizerForm.campus">
            <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院/部门"><el-input v-model="organizerForm.college" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="organizerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitOrganizer">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑用户" width="520px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="学号"><el-input v-model="editForm.studentNo" /></el-form-item>
        <el-form-item label="工号"><el-input v-model="editForm.workNo" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="editForm.majorName" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="editForm.className" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
        <el-form-item label="校区">
          <el-select v-model="editForm.campus">
            <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院/部门"><el-input v-model="editForm.college" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" :disabled="editForm.role === 'admin'">
            <el-option label="学生" value="student" />
            <el-option label="活动负责人" value="organizer" />
            <el-option v-if="editForm.role === 'admin'" label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
      <el-form label-width="90px">
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Search } from '@element-plus/icons-vue'
import {
  createOrganizer,
  listAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus
} from '../../api/adminUser'
import {
  userCampuses,
  userRoleOptions,
  userRoleTagType,
  userRoleText,
  userStatusOptions,
  userStatusTagType,
  userStatusText
} from '../../utils/options'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const filter = reactive({
  keyword: '',
  role: '',
  campus: '',
  status: ''
})
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const organizerDialogVisible = ref(false)
const editDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const currentUserId = ref(null)
const newPassword = ref('')
const organizerForm = reactive(emptyOrganizerForm())
const editForm = reactive(emptyEditForm())

async function load() {
  loading.value = true
  try {
    const data = await listAdminUsers({
      keyword: filter.keyword || undefined,
      role: filter.role || undefined,
      campus: filter.campus || undefined,
      status: filter.status === '' ? undefined : filter.status,
      page: pagination.page,
      size: pagination.size
    })
    rows.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.page || pagination.page
    pagination.size = data.size || pagination.size
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.page = 1
  load()
}

function resetFilter() {
  filter.keyword = ''
  filter.role = ''
  filter.campus = ''
  filter.status = ''
  search()
}

function changePage(page) {
  pagination.page = page
  load()
}

function changeSize(size) {
  pagination.size = size
  pagination.page = 1
  load()
}

function openCreateOrganizer() {
  Object.assign(organizerForm, emptyOrganizerForm())
  organizerDialogVisible.value = true
}

async function submitOrganizer() {
  if (!organizerForm.realName || !organizerForm.workNo || !organizerForm.password || !organizerForm.campus) {
    ElMessage.warning('请填写负责人姓名、工号、初始密码和校区')
    return
  }
  saving.value = true
  try {
    await createOrganizer(organizerForm)
    ElMessage.success('负责人账号创建成功')
    organizerDialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openEdit(row) {
  currentUserId.value = row.id
  Object.assign(editForm, {
    realName: row.realName || '',
    studentNo: row.studentNo || '',
    workNo: row.workNo || '',
    gradeYear: row.gradeYear || '',
    majorCode: row.majorCode || '',
    majorName: row.majorName || '',
    className: row.className || '',
    phone: row.phone || '',
    campus: row.campus || '龙子湖校区',
    college: row.college || '',
    role: row.role === 'user' ? 'student' : row.role
  })
  editDialogVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    await updateAdminUser(currentUserId.value, editForm)
    ElMessage.success('用户信息已保存')
    editDialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 0 ? 1 : 0
  await ElMessageBox.confirm(`确认${nextStatus === 1 ? '启用' : '禁用'}该账号？`, '账号状态')
  await updateAdminUserStatus(row.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? '账号已启用' : '账号已禁用')
  load()
}

function openResetPassword(row) {
  currentUserId.value = row.id
  newPassword.value = ''
  passwordDialogVisible.value = true
}

async function submitResetPassword() {
  saving.value = true
  try {
    await resetAdminUserPassword(currentUserId.value, newPassword.value)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

function emptyOrganizerForm() {
  return {
    password: '',
    realName: '',
    workNo: '',
    phone: '',
    campus: '龙子湖校区',
    college: ''
  }
}

function emptyEditForm() {
  return {
    realName: '',
    studentNo: '',
    workNo: '',
    gradeYear: '',
    majorCode: '',
    majorName: '',
    className: '',
    phone: '',
    campus: '龙子湖校区',
    college: '',
    role: 'student'
  }
}

onMounted(load)
</script>
