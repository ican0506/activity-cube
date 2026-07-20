<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动反馈</span>
        <h1>活动反馈</h1>
        <p>{{ activity?.title || '分享你的活动体验。' }}</p>
      </div>
      <div class="lite-summary-card">
        <span>满意度评分</span>
        <strong>{{ form.score }} 分</strong>
        <p>支持匿名提交</p>
      </div>
    </div>

    <section class="panel form-narrow agri-card" v-loading="loading">
      <div class="page-head">
        <div>
          <h2 class="page-title">填写反馈</h2>
          <p class="page-subtitle">记录真实体验、改进建议，也可以上传图片或视频附件。</p>
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
        description="活动结束后再回来提交体验和建议，反馈会帮助负责人复盘活动效果。"
      />

      <el-form :model="form" label-position="top">
        <el-form-item label="满意度评分">
          <div class="feedback-rate">
            <el-rate v-model="form.score" :max="5" />
            <span>{{ form.score }} 分</span>
          </div>
        </el-form-item>
        <el-form-item label="活动体验">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="可以写下活动流程、现场体验、组织服务等感受"
          />
        </el-form-item>
        <el-form-item label="改进建议">
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
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { resolveFileUrl, uploadFile } from '../../api/file'
import { submitFeedback } from '../../api/feedback'
import { saveFeedbackMedia } from '../../api/media'
import { canFeedback, feedbackDisabledReason } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const activity = ref(null)
const loading = ref(false)
const submitting = ref(false)
const attachments = ref([])
const attachmentFileList = ref([])
const form = reactive({
  score: 5,
  content: '',
  suggestion: '',
  anonymous: false
})

async function submit() {
  if (activity.value && !canFeedback(activity.value)) {
    ElMessage.warning(feedbackDisabledReason(activity.value))
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
