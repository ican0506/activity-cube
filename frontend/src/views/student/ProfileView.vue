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

    <div class="profile-grid profile-basic-grid">
      <div class="panel profile-card">
        <div class="section-title compact">
          <div>
            <h2>学生身份</h2>
          </div>
        </div>
        <div class="identity-list">
          <div v-for="item in identityItems" :key="item.label" class="identity-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value || '-' }}</strong>
          </div>
        </div>
      </div>

      <div class="panel profile-security-card">
        <div class="section-title compact">
          <div>
            <h2>账号安全</h2>
          </div>
        </div>
        <div class="security-box">
          <strong>登录账号</strong>
          <span>{{ profile.username || profile.studentNo || '-' }}</span>
          <div class="security-actions">
            <RouterLink to="/profile/security">
              <el-button type="primary">修改密码</el-button>
            </RouterLink>
            <el-button plain @click="logout">退出登录</el-button>
          </div>
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

    <div id="rewards" class="panel rewards-panel">
      <div class="section-title">
        <div>
          <h2>我的活动成果</h2>
          <p>完成签到并由负责人发放后，活动成果会展示在这里。</p>
        </div>
        <el-button v-if="rewards.length > 3" text type="primary" @click="showAllRewards = !showAllRewards">
          {{ showAllRewards ? '收起' : '查看全部成果' }}
        </el-button>
      </div>
      <div v-if="rewards.length" class="reward-list">
        <div v-for="reward in displayedRewards" :key="reward.id" class="reward-card">
          <div>
            <el-tag type="success" size="small">{{ reward.activityCategory || '其他' }}</el-tag>
            <h3>{{ reward.activityTitle }}</h3>
            <p>{{ reward.issuedTime || formatRange(reward.startTime, reward.endTime) || '-' }}</p>
          </div>
          <div class="reward-values">
            <span>{{ reward.rewardType || '活动奖励' }}</span>
            <strong>{{ rewardValueText(reward) }}</strong>
            <small>{{ reward.rewardDescription || '已发放' }}</small>
          </div>
        </div>
      </div>
      <el-empty v-if="!rewards.length && !loading" description="暂无活动成果，完成签到并发放奖励后会展示在这里。" />
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
const rewards = ref([])
const editVisible = ref(false)
const savingProfile = ref(false)
const avatarUploading = ref(false)
const showAllRewards = ref(false)

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

const displayedRewards = computed(() => showAllRewards.value ? rewards.value : rewards.value.slice(0, 3))

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  try {
    const [profileData, summaryData, rewardData] = await Promise.all([
      getStudentProfile(),
      getStudentProfileSummary(),
      listMyRewards()
    ])
    profile.value = profileData || {}
    summary.value = summaryData || {}
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

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function rewardValueText(reward) {
  const parts = []
  if (Number(reward.rewardHours || 0) > 0) parts.push(`+${reward.rewardHours} 课外学时`)
  if (Number(reward.rewardPoints || 0) > 0) parts.push(`+${reward.rewardPoints} 积分`)
  return parts.length ? parts.join(' · ') : reward.rewardType || '已获得'
}

function logout() {
  userStore.logout()
  router.push('/login')
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

.security-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.rewards-panel {
  overflow: hidden;
}

.reward-list {
  display: grid;
  gap: 10px;
}

.reward-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

  .reward-card {
    display: grid;
  }

  .security-actions,
  .security-actions a {
    display: grid;
    width: 100%;
  }
}
</style>
