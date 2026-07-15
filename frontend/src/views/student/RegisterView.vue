<template>
  <section class="panel form-narrow">
    <div class="page-head">
      <div>
        <h1 class="page-title">活动报名</h1>
        <p class="page-subtitle">提交后同一活动不能重复报名。</p>
      </div>
    </div>
    <el-form :model="form" label-width="96px">
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
      <el-form-item label="学院"><el-input v-model="form.college" /></el-form-item>
      <el-form-item label="专业班级"><el-input v-model="form.majorClass" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="所在校区">
        <el-select v-model="form.campus">
          <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">提交报名</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerActivity } from '../../api/registration'
import { useUserStore } from '../../stores/user'
import { userCampuses } from '../../utils/options'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const user = userStore.userInfo || {}
const form = reactive({
  name: user.realName || '',
  studentNo: user.studentNo || '',
  college: user.college || '',
  majorClass: user.majorClass || '',
  phone: user.phone || '',
  campus: user.campus || '龙子湖校区',
  remark: ''
})

async function submit() {
  await registerActivity(route.params.id, form)
  ElMessage.success('报名成功')
  router.push(`/activities/${route.params.id}`)
}
</script>
