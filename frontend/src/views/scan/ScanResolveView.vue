<template>
  <section v-loading="loading">
    <div class="panel scan-resolve-card agri-card">
      <div v-if="errorMessage" class="scan-state">
        <el-result icon="warning" title="扫码结果无法使用" :sub-title="errorMessage">
          <template #extra>
            <RouterLink to="/activities"><el-button type="primary">返回活动大厅</el-button></RouterLink>
            <RouterLink to="/scan"><el-button>重新扫码</el-button></RouterLink>
          </template>
        </el-result>
      </div>
      <div v-else class="scan-state">
        <el-result icon="success" title="正在识别二维码" sub-title="正在检查活动状态，并为你跳转到对应页面。" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivity } from '../../api/activity'
import { useUserStore } from '../../stores/user'
import { buildLoginRedirect } from '../../utils/authSession'
import { canCheckin, canQrCheckin, canRegister, checkinDisabledReason, registerDisabledReason } from '../../utils/options'
import { resolveScanAction } from '../../utils/scanResolve'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const errorMessage = ref('')

async function resolve() {
  const action = resolveScanAction(route.query)
  if (action.error) {
    errorMessage.value = action.error
    return
  }
  if (!userStore.isLogin) {
    router.replace(buildLoginRedirect(route.path, route.fullPath.slice(route.path.length)))
    return
  }
  loading.value = true
  try {
    const detail = await getActivity(route.query.activityId)
    const activity = detail.activity
    if (route.query.type === 'register') {
      if (!canRegister(activity)) {
        errorMessage.value = registerDisabledReason(activity)
        return
      }
      router.replace(action.route)
      return
    }

    if (!route.query.code) {
      errorMessage.value = '签到二维码缺少活动信息，请重新扫码或输入完整签到码。'
      return
    }
    if (activity.checkedIn) {
      errorMessage.value = '你已完成签到，请勿重复签到'
      return
    }
    if (!activity.registered) {
      errorMessage.value = '你尚未报名，不能签到'
      return
    }
    if (!canQrCheckin(activity)) {
      errorMessage.value = canCheckin(activity) ? '该活动不支持现场扫码签到' : checkinDisabledReason(activity)
      return
    }
    if (!canCheckin(activity)) {
      errorMessage.value = checkinDisabledReason(activity)
      return
    }
    router.replace(action.route)
  } catch (error) {
    errorMessage.value = error?.message || '活动不存在'
  } finally {
    loading.value = false
  }
}

onMounted(resolve)
</script>
