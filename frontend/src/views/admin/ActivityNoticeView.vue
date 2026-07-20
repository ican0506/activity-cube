<template>
  <section>
    <div class="lite-page-head">
      <div>
        <span class="section-eyebrow">活动通知</span>
        <h1>{{ activity?.title || '通知发布' }}</h1>
        <p>向报名学生发送活动安排、签到提醒或反馈提醒。</p>
      </div>
    </div>

    <div class="notice-admin-grid">
      <div class="panel notice-form-panel">
        <div class="section-title">
          <div>
            <h2>发布新通知</h2>
            <p>发布成功后会按通知对象生成学生消息。</p>
          </div>
        </div>
        <el-form :model="form" label-position="top">
          <el-form-item label="通知标题"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
          <el-form-item label="通知类型">
            <el-select v-model="form.noticeType" class="full">
              <el-option label="活动通知" value="activity" />
              <el-option label="签到提醒" value="checkin_reminder" />
              <el-option label="反馈提醒" value="feedback_reminder" />
            </el-select>
          </el-form-item>
          <el-form-item label="通知对象">
            <el-select v-model="form.targetType" class="full">
              <el-option label="全部报名学生" value="all_registered" />
              <el-option label="已签到学生" value="checked_in" />
              <el-option label="未签到学生" value="not_checked_in" />
            </el-select>
          </el-form-item>
          <el-form-item label="通知内容">
            <el-input v-model="form.content" type="textarea" :rows="7" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-button type="primary" class="full" :loading="saving" @click="submit">发布通知</el-button>
        </el-form>
      </div>

      <div class="panel" v-loading="loading">
        <div class="section-title">
          <div>
            <h2>已发布通知</h2>
            <p>查看该活动历史通知记录。</p>
          </div>
        </div>
        <div class="notice-history">
          <div v-for="item in notices" :key="item.id" class="notice-history-item">
            <div class="message-card-head">
              <h3>{{ item.title }}</h3>
              <el-tag>{{ noticeTypeText(item.noticeType) }}</el-tag>
            </div>
            <p>{{ item.content }}</p>
            <div class="meta-row">
              <span>{{ targetTypeText(item.targetType) }}</span>
              <span>{{ item.senderName || '系统' }}</span>
              <span>{{ item.createdAt || item.createTime }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && notices.length === 0" description="暂无已发布通知" />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivity } from '../../api/activity'
import { listActivityNotices, publishActivityNotice } from '../../api/notice'

const route = useRoute()
const activityId = route.params.id
const activity = ref(null)
const notices = ref([])
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  title: '',
  content: '',
  noticeType: 'activity',
  targetType: 'all_registered'
})

async function load() {
  loading.value = true
  try {
    const detail = await getActivity(activityId)
    activity.value = detail.activity
    notices.value = await listActivityNotices(activityId)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写通知标题和内容')
    return
  }
  saving.value = true
  try {
    await publishActivityNotice(activityId, form)
    ElMessage.success('通知发布成功。')
    form.title = ''
    form.content = ''
    await load()
  } finally {
    saving.value = false
  }
}

function noticeTypeText(type) {
  return {
    activity: '活动通知',
    checkin_reminder: '签到提醒',
    feedback_reminder: '反馈提醒'
  }[type] || '活动通知'
}

function targetTypeText(type) {
  return {
    all_registered: '全部报名学生',
    checked_in: '已签到学生',
    not_checked_in: '未签到学生'
  }[type] || '全部报名学生'
}

onMounted(load)
</script>

<style scoped>
.notice-admin-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.85fr) minmax(0, 1.15fr);
  gap: 16px;
}

.notice-history {
  display: grid;
  gap: 12px;
}

.notice-history-item {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 14px;
  background: #f7fcf9;
  border: 1px solid rgba(11, 125, 59, 0.12);
}

.notice-history-item h3,
.notice-history-item p {
  margin: 0;
}

.notice-history-item p {
  color: var(--ac-muted);
  line-height: 1.7;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .notice-admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
