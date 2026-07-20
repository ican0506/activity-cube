<template>
  <section>
    <div class="campus-hero admin-campus-hero">
      <div class="hero-copy">
        <span class="motto-badge">系统通知</span>
        <h1>发布系统通知</h1>
        <p>面向全部学生发送平台公告、活动提醒或重要说明。</p>
      </div>
    </div>

    <div class="panel system-notice-panel">
      <div class="section-title">
        <div>
          <h2>通知内容</h2>
          <p>当前版本先支持发送给全部学生。</p>
        </div>
      </div>
      <el-form :model="form" label-position="top">
        <el-form-item label="通知标题"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="通知对象">
          <el-select v-model="form.targetType" class="full" disabled>
            <el-option label="全部学生" value="all_students" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">发布系统通知</el-button>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { publishSystemNotice } from '../../api/notice'

const saving = ref(false)
const form = reactive({
  title: '',
  content: '',
  targetType: 'all_students'
})

async function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写通知标题和内容')
    return
  }
  saving.value = true
  try {
    await publishSystemNotice(form)
    ElMessage.success('通知发布成功。')
    form.title = ''
    form.content = ''
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.system-notice-panel {
  max-width: 760px;
}
</style>
