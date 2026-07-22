<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动反馈</span>
        <h1>反馈建议</h1>
        <p>{{ activity?.title || '提交活动建议、问题反馈或满意度评价。' }}</p>
      </div>
      <div class="lite-summary-card">
        <span>反馈类型</span>
        <strong>{{ currentFeedbackTypeText }}</strong>
        <p>支持匿名提交</p>
      </div>
    </div>

    <section class="panel form-narrow agri-card" v-loading="loading">
      <div class="page-head">
        <div>
          <h2 class="page-title">填写反馈</h2>
          <p class="page-subtitle">活动建议和问题反馈可提前提交；活动评价在签到后或活动结束后提交。</p>
        </div>
        <RouterLink :to="`/activities/${route.params.id}`">
          <el-button>返回详情</el-button>
        </RouterLink>
      </div>

      <el-alert
        v-if="activity && !canFeedback(activity)"
        class="qr-tip"
        type="warning"
        :closable="false"
        show-icon
        :title="feedbackDisabledReason(activity)"
        description="活动发布后即可反馈建议和问题，活动取消后不再接收反馈。"
      />

      <el-form :model="form" label-position="top">
        <el-form-item label="反馈类型">
          <el-radio-group v-model="form.feedbackType">
            <el-radio-button v-for="item in availableFeedbackTypes" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="form.feedbackType === 'suggestion'"
          class="qr-tip"
          type="info"
          :closable="false"
          title="可以提前反馈你对活动时间、地点、内容安排的建议。"
        />
        <el-alert
          v-if="form.feedbackType === 'issue'"
          class="qr-tip"
          type="warning"
          :closable="false"
          title="遇到报名、地点、通知等问题可以在这里反馈。"
        />
        <el-alert
          v-if="form.feedbackType === 'evaluation'"
          class="qr-tip"
          type="success"
          :closable="false"
          title="活动结束后可以评价本次活动体验。"
        />

        <el-form-item v-if="form.feedbackType === 'evaluation'" label="满意度评分">
          <div class="feedback-rate">
            <el-rate v-model="form.score" :max="5" />
            <span>{{ form.score }} 分</span>
          </div>
        </el-form-item>
        <el-form-item :label="contentLabel">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            :placeholder="contentPlaceholder"
          />
        </el-form-item>
        <el-form-item v-if="form.feedbackType === 'evaluation'" label="改进建议">
          <el-input
            v-model="form.suggestion"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="例如时间安排、场地、通知、互动形式等建议"
          />
        </el-form-item>

        <el-form-item label="图片/视频附件">
          <div class="upload-block full">
            <el-upload
              v-model:file-list="attachmentFileList"
              action="#"
              multiple
              accept=".jpg,.jpeg,.png,.gif,.webp,.mp4,.webm,.mov"
              :limit="9"
              :disabled="activity && !canFeedback(activity)"
              :auto-upload="true"
              :http-request="uploadAttachment"
              :before-upload="beforeAttachmentUpload"
              :on-remove="removeAttachment"
              :on-exceed="attachmentExceed"
            >
              <el-button type="primary">上传附件</el-button>
            </el-upload>
            <p class="upload-tip">最多上传 9 个附件。图片最大 5MB，视频最大 100MB。</p>
            <div v-if="attachments.length" class="media-preview-grid campus-gallery">
              <div v-for="item in attachments" :key="item.url" class="media-preview-card">
                <el-image v-if="item.mediaType === 'image'" :src="resolveFileUrl(item.url)" fit="cover" />
                <video v-else :src="resolveFileUrl(item.url)" controls />
                <span>{{ item.originalName || item.fileName }}</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="是否匿名">
          <el-switch v-model="form.anonymous" active-text="匿名提交" inactive-text="实名提交" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="activity && !canFeedback(activity)" :loading="submitting" @click="submit">提交反馈</el-button>
        </el-form-item>
      </el-form>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { resolveFileUrl, uploadFile } from '../../api/file'
import { submitFeedback } from '../../api/feedback'
import { saveFeedbackMedia } from '../../api/media'
import { canFeedback, feedbackDisabledReason, feedbackTypeText } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)
const attachments = ref([])
const attachmentFileList = ref([])
const form = reactive({
  feedbackType: 'suggestion',
  score: 5,
  content: '',
  suggestion: '',
  anonymous: false
})

const evaluationAvailable = computed(() => activity.value?.checkedIn || activity.value?.status === 'ENDED')
const availableFeedbackTypes = computed(() => {
  const types = [
    { label: '活动建议', value: 'suggestion' },
    { label: '问题反馈', value: 'issue' }
  ]
  if (evaluationAvailable.value) {
    types.push({ label: '活动评价', value: 'evaluation' })
  }
  return types
})
const currentFeedbackTypeText = computed(() => feedbackTypeText(form.feedbackType))
const contentLabel = computed(() => {
  if (form.feedbackType === 'issue') return '问题描述'
  if (form.feedbackType === 'evaluation') return '活动体验'
  return '活动建议'
})
const contentPlaceholder = computed(() => {
  if (form.feedbackType === 'issue') return '请描述报名、地点、通知或活动安排中遇到的问题'
  if (form.feedbackType === 'evaluation') return '可以写下活动流程、现场体验、组织服务等感受'
  return '可以写下你对活动时间、地点、内容安排的建议'
})

watch(evaluationAvailable, (available) => {
  if (!available && form.feedbackType === 'evaluation') {
    form.feedbackType = 'suggestion'
  }
})

async function submit() {
  if (activity.value && !canFeedback(activity.value)) {
    ElMessage.warning(feedbackDisabledReason(activity.value))
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('反馈内容不能为空')
    return
  }
  if (form.feedbackType === 'evaluation' && (!form.score || form.score < 1 || form.score > 5)) {
    ElMessage.warning('满意度评分必须在 1 到 5 之间')
    return
  }
  submitting.value = true
  try {
    const feedback = await submitFeedback(route.params.id, form)
    if (attachments.value.length) {
      await saveFeedbackMedia(feedback.id, attachments.value)
    }
    ElMessage.success('反馈提交成功，感谢你的参与。')
    router.push(`/activities/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}

async function uploadAttachment(options) {
  try {
    const result = await uploadFile(options.file, (event) => {
      const percent = event.total ? Math.round((event.loaded * 100) / event.total) : 0
      options.onProgress({ percent })
    })
    attachments.value.push({
      mediaType: result.fileType,
      url: result.url,
      fileName: result.fileName,
      originalName: result.originalName,
      size: result.size
    })
    options.onSuccess(result)
    ElMessage.success('附件上传成功')
  } catch (error) {
    options.onError(error)
  }
}

function removeAttachment(file) {
  const url = file.response?.url || file.url
  attachments.value = attachments.value.filter((item) => resolveFileUrl(item.url) !== url && item.url !== url)
}

function attachmentExceed() {
  ElMessage.warning('最多上传 9 个附件')
}

function beforeAttachmentUpload(file) {
  const extension = String(file.name || '').split('.').pop().toLowerCase()
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

onMounted(async () => {
  loading.value = true
  try {
    const detail = await getActivity(route.params.id)
    activity.value = detail.activity
  } finally {
    loading.value = false
  }
})
</script>
