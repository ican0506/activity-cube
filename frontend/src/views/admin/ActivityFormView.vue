<template>
  <section class="panel form-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑活动' : '创建活动' }}</h1>
        <p class="page-subtitle">创建活动时必须选择校区，并配置报名时间。</p>
      </div>
    </div>
    <el-form :model="form" label-width="130px">
      <el-form-item label="活动名称"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="活动介绍"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="活动校区">
        <el-select v-model="form.campus">
          <el-option v-for="item in campusesForForm" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="活动地点"><el-input v-model="form.location" /></el-form-item>
      <el-form-item label="活动开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="活动结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="报名开始"><el-date-picker v-model="form.registerStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="报名结束"><el-date-picker v-model="form.registerEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="最大报名人数"><el-input-number v-model="form.maxParticipants" :min="1" /></el-form-item>
      <el-form-item label="允许跨校区"><el-switch v-model="form.allowCrossCampus" /></el-form-item>
      <el-form-item label="活动状态">
        <el-select v-model="form.status">
          <el-option v-for="item in statusForForm" :key="item" :label="statusText(item)" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createActivity, getActivity, updateActivity } from '../../api/activity'
import { statusText } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.id))
const campusesForForm = ['全校区', '龙子湖校区', '文化路校区', '许昌校区', '线上']
const statusForForm = ['DRAFT', 'REGISTERING', 'ONGOING', 'ENDED', 'CANCELLED']
const form = reactive({
  title: '',
  description: '',
  campus: '龙子湖校区',
  location: '',
  startTime: '2026-07-20T14:00:00',
  endTime: '2026-07-20T16:00:00',
  registerStartTime: '2026-07-15T08:00:00',
  registerEndTime: '2026-07-20T12:00:00',
  maxParticipants: 120,
  allowCrossCampus: true,
  status: 'REGISTERING'
})

onMounted(async () => {
  if (isEdit.value) {
    const detail = await getActivity(route.params.id)
    Object.assign(form, detail.activity)
  }
})

async function submit() {
  if (isEdit.value) {
    await updateActivity(route.params.id, form)
  } else {
    await createActivity(form)
  }
  ElMessage.success('保存成功')
  router.push('/admin/activities')
}
</script>
