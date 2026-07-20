<template>
  <section>
    <div class="page-head">
      <div>
        <h1 class="page-title">签到管理</h1>
        <p class="page-subtitle">生成签到二维码，查看已签到与未签到名单。</p>
      </div>
      <div class="toolbar">
        <el-button @click="copyLink">复制签到链接</el-button>
        <el-button type="success" @click="openManualCheckin">人工补签</el-button>
        <el-button type="primary" @click="download">导出签到名单</el-button>
      </div>
    </div>

    <div class="metric-row">
      <div class="metric metric-accent"><span>已签到</span><strong>{{ checkins.length }}</strong></div>
      <div class="metric"><span>未签到</span><strong>{{ absences.length }}</strong></div>
      <div class="metric"><span>签到率</span><strong>{{ rate }}%</strong></div>
    </div>

    <div class="panel qr-card">
      <div class="qr-box">
        <canvas ref="qr"></canvas>
      </div>
      <div>
        <h3>签到二维码</h3>
        <p class="page-subtitle">{{ checkinUrl }}</p>
      </div>
    </div>

    <div class="checkin-grid" style="margin-top: 16px">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>已签到名单</h2>
            <p>活动时间内完成签到的记录。</p>
          </div>
        </div>
        <el-table :data="displayCheckins" stripe>
          <el-table-column prop="name" label="姓名" min-width="90" />
          <el-table-column prop="studentNo" label="学号" min-width="120" />
          <el-table-column prop="college" label="学院" min-width="130" />
          <el-table-column prop="majorClass" label="班级" min-width="130" />
          <el-table-column prop="campus" label="校区" min-width="110" />
          <el-table-column prop="checkinTime" label="签到时间" min-width="170" />
          <el-table-column label="签到方式" min-width="105">
            <template #default="{ row }">
              <el-tag :type="checkinTypeTag(row.checkinType)" effect="light">
                {{ getCheckinTypeLabel(row.checkinType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" min-width="100">
            <template #default="{ row }">{{ row.operatorName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!checkins.length" description="暂无签到数据" />
      </div>
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>未签到名单</h2>
            <p>已报名但未签到的学生。</p>
          </div>
        </div>
        <el-table :data="absences" stripe>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column prop="campus" label="校区" />
        </el-table>
        <el-empty v-if="!absences.length" description="暂无未签到数据" />
      </div>
    </div>

    <el-dialog v-model="manualDialogVisible" title="人工补签" width="min(92vw, 620px)" class="manual-checkin-dialog" destroy-on-close>
      <p class="dialog-tip">仅可为已经报名但尚未签到的学生补签，请如实填写补签原因。</p>
      <el-input
        v-model="candidateKeyword"
        clearable
        placeholder="按姓名或学号搜索学生"
        class="manual-search"
      />

      <div class="manual-candidate-list">
        <el-radio-group v-if="filteredCandidates.length" v-model="manualForm.userId" class="manual-candidate-options">
          <el-radio v-for="student in filteredCandidates" :key="student.userId" :value="student.userId" border>
            <span class="candidate-name">{{ student.name }}</span>
            <span>{{ student.studentNo }}</span>
            <span>{{ student.college }} · {{ student.majorClass }}</span>
          </el-radio>
        </el-radio-group>
        <el-empty v-else description="暂无可补签学生" :image-size="78" />
      </div>

      <el-form label-position="top" class="manual-checkin-form">
        <el-form-item label="补签原因" required>
          <el-input
            v-model="manualForm.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="例如：手机没电、网络异常，经现场核验后补签"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualSubmitting" @click="submitManualCheckin">确认补签</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { getActivity } from '../../api/activity'
import { listAbsences, listCheckins, manualCheckinActivity } from '../../api/checkin'
import { listRegistrations } from '../../api/registration'
import { checkedRegistrations, exportRosterExcel } from '../../utils/excelExport'
import { buildActivityQrLinks } from '../../utils/qrLinks'
import { enrichCheckins, getCheckinTypeLabel } from '../../utils/checkinDisplay'
import { recordFrontendExport } from '../../api/export'

const route = useRoute()
const qr = ref(null)
const activity = ref(null)
const registrations = ref([])
const checkins = ref([])
const absences = ref([])
const manualDialogVisible = ref(false)
const manualSubmitting = ref(false)
const candidateKeyword = ref('')
const manualForm = reactive({ userId: null, remark: '' })
const checkinUrl = computed(() => buildActivityQrLinks(location.origin, route.params.id, {
  checkinCode: activity.value?.checkinCode
}).checkinUrl)
const rate = computed(() => {
  const total = checkins.value.length + absences.value.length
  return total ? Math.round((checkins.value.length * 10000) / total) / 100 : 0
})
const displayCheckins = computed(() => enrichCheckins(checkins.value, registrations.value))
const filteredCandidates = computed(() => {
  const keyword = candidateKeyword.value.trim().toLowerCase()
  if (!keyword) return absences.value
  return absences.value.filter((student) => [student.name, student.studentNo]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword)))
})

async function load() {
  const [detail, registrationRows, checkinRows, absenceRows] = await Promise.all([
    getActivity(route.params.id),
    listRegistrations(route.params.id),
    listCheckins(route.params.id),
    listAbsences(route.params.id)
  ])
  activity.value = detail.activity
  registrations.value = registrationRows
  checkins.value = checkinRows
  absences.value = absenceRows
  await nextTick()
  if (qr.value) QRCode.toCanvas(qr.value, checkinUrl.value, { width: 180 })
}

async function copyLink() {
  await navigator.clipboard.writeText(checkinUrl.value)
  ElMessage.success('已复制')
}

async function download() {
  exportRosterExcel({
    activityName: activity.value?.title,
    suffix: '签到名单',
    registrations: checkedRegistrations(registrations.value, checkins.value),
    checkins: checkins.value
  })
  await recordFrontendExport(route.params.id, 'checkins')
  ElMessage.success('签到名单已导出')
}

function openManualCheckin() {
  candidateKeyword.value = ''
  manualForm.userId = null
  manualForm.remark = ''
  manualDialogVisible.value = true
}

async function submitManualCheckin() {
  if (!manualForm.userId) {
    ElMessage.warning('请选择需要补签的学生')
    return
  }
  if (!manualForm.remark.trim()) {
    ElMessage.warning('补签原因不能为空')
    return
  }
  manualSubmitting.value = true
  try {
    await manualCheckinActivity(route.params.id, {
      userId: manualForm.userId,
      remark: manualForm.remark.trim()
    })
    ElMessage.success('补签成功')
    manualDialogVisible.value = false
    await load()
  } finally {
    manualSubmitting.value = false
  }
}

function checkinTypeTag(type) {
  return { qr: 'success', online: 'primary', manual: 'warning' }[type] || 'info'
}

onMounted(load)
</script>

<style scoped>
.dialog-tip {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}

.manual-search {
  margin-bottom: 12px;
}

.manual-candidate-list {
  max-height: 260px;
  overflow-y: auto;
  margin-bottom: 14px;
  padding: 4px;
  border: 1px solid #dff0e7;
  border-radius: 12px;
  background: #fbfefc;
}

.manual-candidate-options {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 8px;
}

.manual-candidate-options :deep(.el-radio) {
  display: flex;
  width: 100%;
  height: auto;
  min-height: 58px;
  margin: 0;
  padding: 10px 12px;
  align-items: center;
}

.manual-candidate-options :deep(.el-radio__label) {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 4px 12px;
  padding-left: 8px;
  color: #52616b;
  line-height: 1.45;
}

.candidate-name {
  color: #173c2c;
  font-weight: 700;
}

@media (max-width: 768px) {
  .manual-candidate-options :deep(.el-radio__label) {
    display: grid;
    gap: 2px;
  }
}
</style>
