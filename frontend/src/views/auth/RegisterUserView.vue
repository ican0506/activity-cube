<template>
  <section class="login-page">
    <div class="login-card register-card">
      <div class="login-brand">
        <span class="brand-mark">
          <img :src="schoolEmblem" alt="河南农业大学校徽" />
        </span>
        <div>
          <h1 class="login-school-name">河南农业大学</h1>
          <p>注册学生账号，进入校园活动平台</p>
        </div>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" size="large" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" placeholder="请输入10位学号，例如 2321241389" size="large" @blur="handleStudentNoChange" />
          <p v-if="studentNoHint" class="field-hint">{{ studentNoHint }}</p>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请设置登录密码" size="large" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" size="large" />
        </el-form-item>
        <el-form-item label="所在校区">
          <el-select v-model="form.campus" class="full" size="large">
            <el-option v-for="item in userCampuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" size="large" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.majorName" placeholder="识别成功后自动填充，也可以手动填写" size="large" />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="form.className" placeholder="例如 软件工程2301" size="large" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" size="large" />
        </el-form-item>
        <el-button type="primary" class="full" size="large" :loading="loading" @click="submit">
          注册并登录
        </el-button>
      </el-form>

      <p class="auth-switch">
        已有账号？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { userCampuses } from '../../utils/options'
import { parseStudentNo } from '../../utils/studentNo'
import schoolEmblem from '../../assets/school-emblem-clean.png'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({
  password: '',
  confirmPassword: '',
  realName: '',
  studentNo: '',
  gradeYear: '',
  majorCode: '',
  majorName: '',
  campus: '龙子湖校区',
  college: '',
  className: '',
  phone: ''
})

const studentNoParsed = computed(() => parseStudentNo(form.studentNo))
const studentNoHint = computed(() => {
  if (!form.studentNo) return ''
  const parsed = studentNoParsed.value
  if (!parsed.valid) return parsed.message
  return `${parsed.gradeYear} · 专业编码 ${parsed.majorCode}${parsed.majorName ? ` · ${parsed.majorName}` : ' · 专业待手动选择'} · 序号 ${parsed.sequenceNo}`
})

watch(() => form.studentNo, handleStudentNoChange)

function handleStudentNoChange() {
  const parsed = parseStudentNo(form.studentNo)
  if (!parsed.valid) return
  form.gradeYear = parsed.gradeYear
  form.majorCode = parsed.majorCode
  if (parsed.majorName) {
    form.majorName = parsed.majorName
  }
}

async function submit() {
  const requiredFields = ['realName', 'studentNo', 'password', 'confirmPassword', 'campus', 'college', 'majorName', 'className', 'phone']
  if (requiredFields.some((field) => !String(form[field] || '').trim())) {
    ElMessage.warning('请完整填写学生注册信息')
    return
  }
  if (!studentNoParsed.value.valid) {
    ElMessage.warning(studentNoParsed.value.message)
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    handleStudentNoChange()
    const { confirmPassword, ...payload } = form
    await userStore.register(payload)
    ElMessage.success('注册成功，已使用学生账号登录')
    router.push('/activities')
  } finally {
    loading.value = false
  }
}
</script>
