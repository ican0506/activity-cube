<template>
  <section class="panel form-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
        <p class="page-subtitle">配置活动基础信息、报名时间、签到时间、封面图、宣传图片和视频素材。</p>
      </div>
      <RouterLink to="/admin/activities">
        <el-button>返回活动管理</el-button>
      </RouterLink>
    </div>

    <el-alert
      class="qr-tip"
      title="活动审核与状态"
      type="success"
      :closable="false"
      show-icon
      description="新建活动先保存为草稿，再提交管理员审核。审核通过后才会在学生端展示，发布后的报名、进行和结束状态仍由系统按时间自动计算。"
    />

    <el-alert
      v-if="reviewStatus === 'REJECTED'"
      class="qr-tip"
      title="活动审核未通过"
      type="error"
      :closable="false"
      show-icon
      :description="rejectReason || '请根据审核意见修改后重新提交。'"
    />

    <el-form :model="form" label-width="130px">
      <el-form-item label="活动名称"><el-input v-model="form.title" placeholder="例如：校园志愿服务招募" /></el-form-item>
      <el-form-item label="活动介绍"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>

      <el-form-item label="活动封面">
        <div class="upload-block">
          <el-upload
            v-model:file-list="coverFileList"
            action="#"
            list-type="picture-card"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            :limit="1"
            :auto-upload="true"
            :http-request="uploadCover"
            :before-upload="beforeImageUpload"
            :on-remove="removeCover"
            :on-exceed="coverExceed"
          >
            <span>上传封面</span>
          </el-upload>
          <p class="upload-tip">只能上传 1 张图片，支持 jpg、jpeg、png、gif、webp，最大 5MB。</p>
        </div>
      </el-form-item>

      <el-form-item label="活动校区">
        <el-select v-model="form.campus">
          <el-option v-for="item in activityCampuses" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动形式">
        <el-radio-group v-model="form.activityMode">
          <el-radio-button v-for="item in activityModes" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <p class="upload-tip">线上活动允许学生在详情页直接签到；线下活动需现场扫码签到。</p>
      </el-form-item>
      <el-form-item label="活动地点"><el-input v-model="form.location" /></el-form-item>
      <el-form-item label="活动开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="活动结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="报名开始"><el-date-picker v-model="form.registerStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="报名结束"><el-date-picker v-model="form.registerEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="签到开始"><el-date-picker v-model="form.checkinStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="签到结束"><el-date-picker v-model="form.checkinEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="最大报名人数"><el-input-number v-model="form.maxParticipants" :min="1" /></el-form-item>
      <el-form-item label="允许跨校区"><el-switch v-model="form.allowCrossCampus" /></el-form-item>

      <el-form-item label="当前展示状态" v-if="isEdit">
        <el-tag :type="statusTagType(displayStatus)">{{ statusText(displayStatus) }}</el-tag>
        <el-tag class="review-status-tag" :type="statusTagType(reviewStatus)">{{ statusText(reviewStatus) }}</el-tag>
      </el-form-item>

      <el-form-item label="图片/视频">
        <div class="upload-block full">
          <el-upload
            v-model:file-list="mediaFileList"
            action="#"
            multiple
            accept=".jpg,.jpeg,.png,.gif,.webp,.mp4,.webm,.mov"
            :auto-upload="true"
            :http-request="uploadActivityMedia"
            :before-upload="beforeMediaUpload"
            :on-remove="removeActivityMedia"
          >
            <el-button type="primary">上传宣传素材</el-button>
          </el-upload>
          <p class="upload-tip">图片最大 5MB，视频最大 100MB。图片展示在活动图片区，视频展示在活动视频区。</p>
          <div v-if="mediaItems.length" class="media-preview-grid">
            <div v-for="item in mediaItems" :key="item.url" class="media-preview-card">
              <el-image v-if="item.mediaType === 'image'" :src="resolveFileUrl(item.url)" fit="cover" />
              <video v-else :src="resolveFileUrl(item.url)" controls />
              <span>{{ item.originalName || item.fileName }}</span>
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item>
        <div class="button-row">
          <el-button v-if="canEditDraft" :loading="saving" @click="saveDraft">{{ isEdit ? '保存修改' : '保存为草稿' }}</el-button>
          <el-button v-if="canSubmitReview" type="primary" :loading="saving" @click="submitReview">提交审核</el-button>
          <el-button v-if="isEdit && reviewStatus === 'PUBLISHED'" :loading="saving" @click="savePublishedChanges">保存修改</el-button>
          <el-button v-if="isEdit && reviewStatus !== 'CANCELLED'" type="danger" plain :loading="saving" @click="cancelCurrentActivity">取消活动</el-button>
        </div>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cancelActivity, createActivity, getActivity, submitActivityReview, updateActivity } from '../../api/activity'
import { resolveFileUrl, uploadFile } from '../../api/file'
import { listActivityMedia, saveActivityMedia } from '../../api/media'
import { activityCampuses, activityModes, statusTagType, statusText } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.id))
const saving = ref(false)
const coverFileList = ref([])
const mediaFileList = ref([])
const mediaItems = ref([])
const displayStatus = ref('DRAFT')
const reviewStatus = ref('DRAFT')
const rejectReason = ref('')
const canEditDraft = computed(() => !isEdit.value || ['DRAFT', 'REJECTED'].includes(reviewStatus.value))
const canSubmitReview = computed(() => !isEdit.value || ['DRAFT', 'REJECTED'].includes(reviewStatus.value))
const form = reactive({
  title: '',
  description: '',
  coverUrl: '',
  activityMode: 'offline',
  campus: '龙子湖校区',
  location: '',
  startTime: '2026-07-20T14:00:00',
  endTime: '2026-07-20T16:00:00',
  registerStartTime: '2026-07-15T08:00:00',
  registerEndTime: '2026-07-20T12:00:00',
  checkinStartTime: '2026-07-20T13:45:00',
  checkinEndTime: '2026-07-20T16:15:00',
  maxParticipants: 120,
  allowCrossCampus: true,
  status: 'DRAFT'
})

onMounted(async () => {
  if (isEdit.value) {
    const detail = await getActivity(route.params.id)
    Object.assign(form, detail.activity)
    form.activityMode = detail.activity.activityMode || 'offline'
    displayStatus.value = detail.activity.status
    reviewStatus.value = detail.activity.reviewStatus || detail.activity.status
    rejectReason.value = detail.activity.rejectReason || ''
    form.status = reviewStatus.value
    if (!form.checkinStartTime) form.checkinStartTime = form.startTime
    if (!form.checkinEndTime) form.checkinEndTime = form.endTime
    if (form.coverUrl) {
      coverFileList.value = [{ name: '活动封面', url: resolveFileUrl(form.coverUrl), status: 'success' }]
    }
    mediaItems.value = await listActivityMedia(route.params.id)
    mediaFileList.value = mediaItems.value.map(toUploadFile)
  }
})

async function persistActivity() {
  form.status = reviewStatus.value || 'DRAFT'
  return isEdit.value
    ? updateActivity(route.params.id, form)
    : createActivity(form)
}

async function saveDraft() {
  saving.value = true
  try {
    const activity = await persistActivity()
    await saveActivityMedia(activity.id, mediaItems.value.map((item, index) => ({
      ...item,
      sortOrder: index + 1
    })))
    ElMessage.success(isEdit.value ? '活动修改已保存' : '草稿已保存')
    router.push('/admin/activities')
  } finally {
    saving.value = false
  }
}

async function submitReview() {
  saving.value = true
  try {
    const activity = await persistActivity()
    await saveActivityMedia(activity.id, mediaItems.value.map((item, index) => ({ ...item, sortOrder: index + 1 })))
    await submitActivityReview(activity.id)
    ElMessage.success('活动已提交审核，请等待管理员处理')
    router.push('/admin/activities')
  } finally {
    saving.value = false
  }
}

async function savePublishedChanges() {
  saving.value = true
  try {
    const activity = await persistActivity()
    await saveActivityMedia(activity.id, mediaItems.value.map((item, index) => ({ ...item, sortOrder: index + 1 })))
    ElMessage.success('活动修改已保存')
    router.push('/admin/activities')
  } finally {
    saving.value = false
  }
}

async function cancelCurrentActivity() {
  saving.value = true
  try {
    await cancelActivity(route.params.id)
    ElMessage.success('活动已取消')
    router.push('/admin/activities')
  } finally {
    saving.value = false
  }
}

async function uploadCover(options) {
  try {
    const result = await uploadFile(options.file, progress(options))
    form.coverUrl = result.url
    coverFileList.value = [toUploadFile(result)]
    options.onSuccess(result)
    ElMessage.success('封面上传成功')
  } catch (error) {
    options.onError(error)
  }
}

async function uploadActivityMedia(options) {
  try {
    const result = await uploadFile(options.file, progress(options))
    const usageType = result.fileType === 'video' ? 'video' : 'gallery'
    mediaItems.value.push({
      mediaType: result.fileType,
      usageType,
      url: result.url,
      fileName: result.fileName,
      originalName: result.originalName,
      size: result.size,
      sortOrder: mediaItems.value.length + 1
    })
    options.onSuccess(result)
    ElMessage.success('素材上传成功')
  } catch (error) {
    options.onError(error)
  }
}

function progress(options) {
  return (event) => {
    const percent = event.total ? Math.round((event.loaded * 100) / event.total) : 0
    options.onProgress({ percent })
  }
}

function removeCover() {
  form.coverUrl = ''
  coverFileList.value = []
}

function removeActivityMedia(file) {
  const url = file.response?.url || file.url
  mediaItems.value = mediaItems.value.filter((item) => resolveFileUrl(item.url) !== url && item.url !== url)
}

function coverExceed() {
  ElMessage.warning('活动封面只能上传 1 张，请先删除旧封面')
}

function beforeImageUpload(file) {
  return validateFile(file, ['jpg', 'jpeg', 'png', 'gif', 'webp'], 5, '图片')
}

function beforeMediaUpload(file) {
  const extension = getExtension(file.name)
  const isImage = ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension)
  const isVideo = ['mp4', 'webm', 'mov'].includes(extension)
  if (!isImage && !isVideo) {
    ElMessage.error('仅支持 jpg、jpeg、png、gif、webp、mp4、webm、mov 文件')
    return false
  }
  const maxMb = isVideo ? 100 : 5
  if (file.size > maxMb * 1024 * 1024) {
    ElMessage.error(`${isVideo ? '视频' : '图片'}大小不能超过 ${maxMb}MB`)
    return false
  }
  return true
}

function validateFile(file, extensions, maxMb, label) {
  if (!extensions.includes(getExtension(file.name))) {
    ElMessage.error(`${label}类型不支持`)
    return false
  }
  if (file.size > maxMb * 1024 * 1024) {
    ElMessage.error(`${label}大小不能超过 ${maxMb}MB`)
    return false
  }
  return true
}

function getExtension(name) {
  return String(name || '').split('.').pop().toLowerCase()
}

function toUploadFile(item) {
  return {
    name: item.originalName || item.fileName || '媒体文件',
    url: resolveFileUrl(item.url),
    status: 'success',
    response: item
  }
}
</script>

<style scoped>
.review-status-tag {
  margin-left: 8px;
}
</style>
