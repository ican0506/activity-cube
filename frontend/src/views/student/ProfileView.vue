<template>
  <section class="profile-page" v-loading="loading">
    <div class="profile-summary-card">
      <div class="profile-identity">
        <el-avatar :size="86" :src="avatarUrl" class="profile-avatar">
          {{ profileInitial }}
        </el-avatar>
        <div>
          <span class="section-eyebrow">个人中心</span>
          <h1>{{ profile.realName || profile.username || '农大学子' }}</h1>
          <p>{{ profile.studentNo || '-' }} · {{ profile.campus || '-' }} · {{ profile.college || '-' }}</p>
          <p class="profile-bio">{{ profile.bio || '暂无个人简介' }}</p>
        </div>
      </div>
      <div class="profile-hero-actions">
        <el-button type="primary" @click="openEditDialog">编辑资料</el-button>
        <RouterLink to="/profile/security">
          <el-button>账号安全</el-button>
        </RouterLink>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      show-icon
      :closable="false"
      class="profile-alert"
      :title="loadError"
    />

    <div class="profile-grid">
      <div class="panel profile-card">
        <div class="section-title compact">
          <div>
            <h2>学生身份</h2>
            <p>学籍信息由系统维护，个人中心仅允许修改头像、手机号和简介。</p>
          </div>
        </div>
        <div class="identity-list">
          <div v-for="item in identityItems" :key="item.label" class="identity-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value || '-' }}</strong>
          </div>
        </div>
      </div>

      <div class="panel quick-card">
        <div class="section-title compact">
          <div>
            <h2>快捷入口</h2>
            <p>常用学生端功能集中在这里。</p>
          </div>
        </div>
        <div class="quick-entry-grid">
          <RouterLink v-for="item in quickEntries" :key="item.title" :to="item.to" class="quick-entry">
            <span>{{ item.title }}</span>
            <small>{{ item.desc }}</small>
          </RouterLink>
        </div>
      </div>
    </div>

    <div class="profile-metrics">
      <div v-for="item in metrics" :key="item.label" class="metric metric-accent">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="profile-grid">
      <div class="panel todo-panel">
        <div class="section-title compact">
          <div>
            <h2>待办提醒</h2>
            <p>即将开始、待签到、待反馈和未读消息会优先展示。</p>
          </div>
        </div>
        <div v-if="todos.length" class="todo-list">
          <button v-for="item in todos" :key="`${item.type}-${item.activityId || item.title}`" class="todo-item" @click="goTodo(item)">
            <div>
              <el-tag :type="todoTagType(item.type)" size="small">{{ todoTypeText(item.type) }}</el-tag>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </div>
            <span>{{ formatTime(item.dueTime) }}</span>
          </button>
        </div>
        <el-empty v-else description="暂无待办事项，新的活动提醒会出现在这里。" />
      </div>

      <div class="panel profile-security-card">
        <div class="section-title compact">
          <div>
            <h2>账号安全</h2>
            <p>建议定期修改密码，不要和其他平台共用密码。</p>
          </div>
        </div>
        <div class="security-box">
          <strong>登录账号</strong>
          <span>{{ profile.username || profile.studentNo || '-' }}</span>
          <RouterLink to="/profile/security">
            <el-button type="primary">进入账号安全</el-button>
          </RouterLink>
        </div>
      </div>
    </div>

    <div id="rewards" class="panel rewards-panel">
      <div class="section-title">
        <div>
          <h2>我的活动成果</h2>
          <p>只有已报名且已签到，并由负责人或管理员发放奖励的活动会展示在这里。</p>
        </div>
      </div>
      <el-table v-if="rewards.length" :data="rewards" stripe class="desktop-table">
        <el-table-column prop="activityTitle" label="活动名称" min-width="180" />
        <el-table-column prop="activityCategory" label="活动类型" width="120" />
        <el-table-column label="活动时间" min-width="190">
          <template #default="{ row }">{{ formatRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="签到状态" width="100">
          <template #default>已签到</template>
        </el-table-column>
        <el-table-column prop="rewardType" label="奖励类型" width="110" />
        <el-table-column label="课外学时" width="110">
          <template #default="{ row }">{{ row.rewardHours || 0 }}</template>
        </el-table-column>
        <el-table-column label="积分" width="90">
          <template #default="{ row }">{{ row.rewardPoints || 0 }}</template>
        </el-table-column>
        <el-table-column prop="issuedTime" label="获得时间" min-width="170" />
        <el-table-column prop="rewardDescription" label="奖励说明" min-width="180" />
      </el-table>
      <div v-if="rewards.length" class="mobile-card-list reward-mobile-list">
        <div v-for="reward in rewards" :key="reward.id" class="reward-card">
          <div>
            <el-tag type="success" size="small">{{ reward.activityCategory || '其他' }}</el-tag>
            <h3>{{ reward.activityTitle }}</h3>
            <p>{{ formatRange(reward.startTime, reward.endTime) }}</p>
          </div>
          <div class="reward-values">
            <span>{{ reward.rewardType || '无' }}</span>
            <strong>{{ reward.rewardHours || 0 }} 学时 · {{ reward.rewardPoints || 0 }} 积分</strong>
            <small>{{ reward.issuedTime || '-' }}</small>
          </div>
        </div>
      </div>
      <el-empty v-if="!rewards.length && !loading" description="暂无活动成果，参加并完成活动后会展示在这里。" />
    </div>

    <el-dialog v-model="editVisible" title="编辑个人资料" width="520px" class="profile-dialog">
      <el-form label-position="top">
        <el-form-item label="头像">
          <div class="avatar-editor">
            <el-avatar :size="72" :src="resolveFileUrl(editForm.avatarUrl)">
              {{ profileInitial }}
            </el-avatar>
            <el-upload
              action="#"
              :show-file-list="false"
              :http-request="uploadAvatar"
              :before-upload="beforeAvatarUpload"
            >
              <el-button :loading="avatarUploading">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="30" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="写一句你的校园活动简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listMyRewards } from '../../api/reward'
import {
  getStudentProfile,
  getStudentProfileSummary,
  getStudentProfileTodos,
  updateStudentProfile,
  uploadStudentAvatar
} from '../../api/studentProfile'
import { resolveFileUrl } from '../../api/file'
import { useUserStore } from '../../stores/user'
import { userRoleText } from '../../utils/options'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loadError = ref('')
const profile = ref({})
const summary = ref({})
const todos = ref([])
const rewards = ref([])
const editVisible = ref(false)
const savingProfile = ref(false)
const avatarUploading = ref(false)

const editForm = reactive({
  avatarUrl: '',
  phone: '',
  bio: ''
})

const avatarUrl = computed(() => resolveFileUrl(profile.value.avatarUrl))
const profileInitial = computed(() => (profile.value.realName || profile.value.username || '学').slice(0, 1))

const identityItems = computed(() => [
  { label: '姓名', value: profile.value.realName },
  { label: '学号', value: profile.value.studentNo },
  { label: '年级', value: profile.value.gradeYear },
  { label: '学院', value: profile.value.college },
  { label: '专业', value: profile.value.majorName },
  { label: '班级', value: profile.value.className || profile.value.majorClass },
  { label: '校区', value: profile.value.campus },
  { label: '手机号', value: profile.value.phone },
  { label: '角色', value: userRoleText(profile.value.role) }
])

const metrics = computed(() => [
  { label: '已报名活动', value: summary.value.registrationCount || 0, hint: '累计提交报名' },
  { label: '已签到活动', value: summary.value.checkinCount || 0, hint: '完成现场或线上签到' },
  { label: '待反馈活动', value: summary.value.pendingFeedbackCount || 0, hint: '活动结束后可提交' },
  { label: '累计课外学时', value: summary.value.rewardHours || 0, hint: '已发放活动奖励' },
  { label: '累计积分', value: summary.value.rewardPoints || 0, hint: '已发放活动积分' },
  { label: '未读消息', value: summary.value.unreadMessageCount || 0, hint: '活动通知和系统消息' }
])

const quickEntries = [
  { title: '我的报名', desc: '查看和取消报名', to: '/my/registrations' },
  { title: '我的签到', desc: '查看签到记录', to: '/my/checkins' },
  { title: '我的反馈', desc: '到已结束活动提交', to: '/activities/ended' },
  { title: '我的消息', desc: '查看活动通知', to: '/messages' },
  { title: '已结束活动', desc: '查看历史活动', to: '/activities/ended' },
  { title: '账号安全', desc: '修改登录密码', to: '/profile/security' }
]

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  try {
    const [profileData, summaryData, todoData, rewardData] = await Promise.all([
      getStudentProfile(),
      getStudentProfileSummary(),
      getStudentProfileTodos(),
      listMyRewards()
    ])
    profile.value = profileData || {}
    summary.value = summaryData || {}
    todos.value = todoData || []
    rewards.value = rewardData || []
  } catch (error) {
    loadError.value = error.message || '个人中心加载失败'
  } finally {
    loading.value = false
  }
}

function openEditDialog() {
  editForm.avatarUrl = profile.value.avatarUrl || ''
  editForm.phone = profile.value.phone || ''
  editForm.bio = profile.value.bio || ''
  editVisible.value = true
}

function beforeAvatarUpload(file) {
  const validType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  if (!validType) {
    ElMessage.error('头像只支持 jpg、jpeg、png、webp 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件不能超过 2MB')
    return false
  }
  return true
}

async function uploadAvatar({ file }) {
  avatarUploading.value = true
  try {
    const data = await uploadStudentAvatar(file)
    editForm.avatarUrl = data.avatarUrl || data.url
    profile.value = { ...profile.value, avatarUrl: editForm.avatarUrl }
    await userStore.refresh()
    ElMessage.success('头像上传成功')
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    profile.value = await updateStudentProfile(editForm)
    await userStore.refresh()
    editVisible.value = false
    ElMessage.success('个人资料已更新')
  } finally {
    savingProfile.value = false
  }
}

function goTodo(item) {
  if (item.targetPath) {
    router.push(item.targetPath)
  }
}

function todoTypeText(type) {
  const map = {
    upcoming: '即将开始',
    checkin: '待签到',
    feedback: '待反馈',
    message: '未读消息',
    registration_closing: '报名截止'
  }
  return map[type] || '待办'
}

function todoTagType(type) {
  const map = {
    checkin: 'success',
    feedback: 'warning',
    message: 'danger',
    registration_closing: 'warning',
    upcoming: 'primary'
  }
  return map[type] || 'info'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatRange(start, end) {
  const left = formatTime(start)
  const right = formatTime(end)
  return left && right ? `${left} - ${right}` : left || right || '-'
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-summary-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 20px;
  border: 1px solid var(--ac-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--ac-shadow);
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.profile-avatar {
  flex: 0 0 auto;
  border: 4px solid rgba(45, 190, 116, 0.16);
  background: linear-gradient(135deg, var(--ac-primary), var(--ac-secondary));
  color: #fff;
  font-size: 30px;
  font-weight: 900;
}

.profile-summary-card h1 {
  margin: 0 0 10px;
  color: var(--ac-ink);
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.16;
}

.profile-summary-card p {
  margin: 0;
  color: var(--ac-muted);
  line-height: 1.7;
}

.profile-bio {
  margin-top: 8px !important;
}

.profile-hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.profile-alert {
  border-radius: 14px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 16px;
  align-items: stretch;
}

.section-title.compact {
  margin-bottom: 12px;
}

.identity-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.identity-item {
  padding: 13px;
  border: 1px solid rgba(22, 160, 133, 0.12);
  border-radius: 14px;
  background: #f7fcf9;
}

.identity-item span,
.security-box span,
.reward-values small {
  display: block;
  color: var(--ac-muted);
  font-size: 13px;
}

.identity-item strong,
.security-box strong {
  display: block;
  margin-top: 6px;
  color: var(--ac-ink);
  overflow-wrap: anywhere;
}

.quick-entry-grid {
  display: grid;
  gap: 10px;
}

.quick-entry {
  display: grid;
  gap: 4px;
  padding: 13px 14px;
  border: 1px solid rgba(22, 160, 133, 0.14);
  border-radius: 14px;
  background: #f7fcf9;
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.quick-entry:hover {
  transform: translateY(-2px);
  border-color: rgba(22, 160, 133, 0.36);
}

.quick-entry span {
  color: var(--ac-primary-strong);
  font-weight: 900;
}

.quick-entry small {
  color: var(--ac-muted);
}

.profile-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
}

.metric small {
  display: block;
  margin-top: 8px;
  color: var(--ac-muted);
}

.todo-list {
  display: grid;
  gap: 10px;
}

.todo-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(22, 160, 133, 0.12);
  border-radius: 14px;
  background: #f7fcf9;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.todo-item:hover {
  border-color: rgba(22, 160, 133, 0.34);
  box-shadow: 0 12px 26px rgba(22, 160, 133, 0.08);
}

.todo-item strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 16px;
}

.todo-item p {
  margin: 0;
  color: var(--ac-muted);
  line-height: 1.6;
}

.todo-item > span {
  flex: 0 0 auto;
  color: var(--ac-muted);
  font-size: 13px;
}

.security-box {
  display: grid;
  gap: 12px;
  min-height: 174px;
  align-content: center;
  padding: 18px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 90% 10%, rgba(217, 164, 65, 0.14), transparent 24%),
    #f7fcf9;
}

.rewards-panel {
  overflow: hidden;
}

.reward-mobile-list {
  display: none;
}

.reward-card {
  display: grid;
  gap: 12px;
  padding: 15px;
  border: 1px solid rgba(22, 160, 133, 0.14);
  border-radius: 14px;
  background: #f7fcf9;
}

.reward-card h3 {
  margin: 8px 0 4px;
  font-size: 18px;
}

.reward-card p {
  margin: 0;
  color: var(--ac-muted);
}

.reward-values {
  display: grid;
  gap: 4px;
}

.reward-values span {
  color: var(--ac-primary-strong);
  font-weight: 900;
}

.reward-values strong {
  color: var(--ac-ink);
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .profile-summary-card,
  .profile-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .profile-hero-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .profile-summary-card {
    border-radius: 16px;
    padding: 18px;
  }

  .profile-identity {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-avatar {
    width: 72px !important;
    height: 72px !important;
  }

  .profile-hero-actions .el-button,
  .security-box .el-button {
    width: 100%;
  }

  .identity-list,
  .profile-metrics {
    grid-template-columns: 1fr;
  }

  .todo-item {
    display: grid;
  }

  .desktop-table {
    display: none;
  }

  .reward-mobile-list {
    display: grid;
    gap: 10px;
  }
}
</style>
