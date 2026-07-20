<template>
  <section class="qr-page">
    <div class="admin-intro">
      <div>
        <h1>二维码管理</h1>
        <p>生成活动报名码和签到码，用于海报、现场展板或班级群转发。</p>
      </div>
      <RouterLink v-if="activity?.id" :to="`/admin/activities/${activity?.id}/checkins`">
        <el-button class="hero-button" :icon="Checked">查看签到名单</el-button>
      </RouterLink>
    </div>

    <div v-if="pageState === 'loading'" class="panel qr-state-panel agri-card">
      <el-skeleton :rows="6" animated />
      <p>活动信息加载中...</p>
    </div>

    <el-result
      v-else-if="pageState === 'forbidden'"
      icon="warning"
      title="你没有权限查看该活动二维码"
      sub-title="管理员可以查看全部活动，负责人只能查看自己创建的活动。"
    >
      <template #extra><RouterLink to="/activities"><el-button type="primary">返回活动大厅</el-button></RouterLink></template>
    </el-result>

    <el-result
      v-else-if="pageState === 'empty'"
      icon="info"
      title="活动不存在或加载失败"
      sub-title="请检查活动链接是否正确，或返回活动管理页面重新选择。"
    >
      <template #extra><RouterLink to="/admin/activities"><el-button type="primary">返回活动管理</el-button></RouterLink></template>
    </el-result>

    <el-result
      v-else-if="pageState === 'error'"
      icon="error"
      title="活动加载失败"
      :sub-title="errorMessage || '请检查网络后重试。'"
    >
      <template #extra><el-button type="primary" @click="load">重新加载</el-button></template>
    </el-result>

    <template v-else-if="activity">
      <div class="panel qr-summary">
        <div>
          <h2>{{ activity?.title || activity?.name || '未命名活动' }}</h2>
          <p class="page-subtitle">{{ activity?.campus || '校区待定' }} · {{ activity?.location || '地点待定' }}</p>
        </div>
        <div class="meta-row">
          <el-tag :type="statusTagType(activity?.status)">{{ statusText(activity?.status) }}</el-tag>
          <span>报名：{{ activity?.registerStartTime || '待设置' }} 至 {{ activity?.registerEndTime || '待设置' }}</span>
          <span>活动：{{ activity?.startTime || '待设置' }} 至 {{ activity?.endTime || '待设置' }}</span>
        </div>
      </div>

      <div class="qr-manage-grid">
        <div class="panel qr-manage-card">
          <div class="qr-card-head">
            <div>
              <h3>报名二维码</h3>
              <p class="page-subtitle">学生扫码后进入活动报名页，提交报名信息。</p>
            </div>
            <el-tag type="success">报名</el-tag>
          </div>
          <div class="qr-canvas-wrap"><canvas ref="registerQr"></canvas></div>
          <p class="qr-link">{{ links.registerUrl }}</p>
          <div class="toolbar">
            <el-button type="primary" :icon="CopyDocument" @click="copy(links.registerUrl, '报名链接已复制')">复制报名链接</el-button>
            <el-button :icon="View" @click="open(links.registerUrl)">打开报名页</el-button>
          </div>
        </div>

        <div class="panel qr-manage-card">
          <div class="qr-card-head">
            <div>
              <h3>签到二维码</h3>
              <p class="page-subtitle">线下活动签到码会携带现场校验 code，建议只在活动现场展示。</p>
            </div>
            <el-tag type="warning">签到</el-tag>
          </div>

          <template v-if="hasCheckinCode">
            <div class="qr-canvas-wrap"><canvas ref="checkinQr"></canvas></div>
            <p class="qr-link">{{ links.checkinUrl }}</p>
            <div class="checkin-code-box">
              <div>
                <span>现场签到码</span>
                <code>{{ activity?.checkinCode }}</code>
              </div>
              <el-button :icon="CopyDocument" @click="copy(activity?.checkinCode, '现场签到码已复制')">复制签到码</el-button>
            </div>
            <div class="toolbar">
              <el-button type="primary" :icon="CopyDocument" @click="copy(links.checkinUrl, '签到链接已复制')">复制签到链接</el-button>
              <el-button :icon="View" @click="open(links.checkinUrl)">打开签到页</el-button>
            </div>
          </template>

          <div v-else class="qr-no-code">
            <el-empty description="签到码未生成" :image-size="92" />
            <el-button type="primary" :loading="generatingCode" @click="generateCode">生成签到码</el-button>
          </div>
        </div>
      </div>

      <el-alert
        class="qr-tip"
        title="使用说明"
        type="success"
        :closable="false"
        show-icon
        description="报名二维码适合提前发布；线下活动签到二维码必须携带签到码，建议只在活动开始时展示。如果学生手机无法启动摄像头，可在扫一扫页面输入现场签到码完成签到。签到仍会走后端规则校验，未报名、重复签到、非活动时间都会被拦截。"
      />
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Checked, CopyDocument, View } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { generateActivityCheckinCode, getActivityQrCode } from '../../api/activity'
import { buildActivityQrLinks } from '../../utils/qrLinks'
import { statusTagType, statusText } from '../../utils/options'

const route = useRoute()
const id = route.params.id
const pageState = ref('loading')
const errorMessage = ref('')
const generatingCode = ref(false)
const activity = ref(null)
const registerQr = ref(null)
const checkinQr = ref(null)

const hasCheckinCode = computed(() => Boolean(activity.value?.checkinCode))
const links = computed(() => buildActivityQrLinks(location.origin, activity.value?.id, {
  checkinCode: activity.value?.checkinCode
}))

async function load() {
  pageState.value = 'loading'
  errorMessage.value = ''
  activity.value = null
  try {
    const result = await getActivityQrCode(id)
    activity.value = normalizeActivity(result)
    if (!activity.value?.id) {
      pageState.value = 'empty'
      return
    }
    pageState.value = 'success'
    await nextTick()
    await renderQrs()
  } catch (error) {
    const message = error?.message || ''
    if (message.includes('活动不存在')) {
      pageState.value = 'empty'
    } else if (message.includes('权限') || message.includes('只能管理')) {
      pageState.value = 'forbidden'
    } else {
      pageState.value = 'error'
      errorMessage.value = message
    }
  }
}

function normalizeActivity(value) {
  if (!value) return null
  return {
    ...value,
    title: value.title || value.name,
    checkinCode: value.checkinCode || value.checkin_code || ''
  }
}

async function renderQrs() {
  if (registerQr.value && links.value.registerUrl) {
    await QRCode.toCanvas(registerQr.value, links.value.registerUrl, { width: 220, margin: 2 })
  }
  if (checkinQr.value && links.value.checkinUrl) {
    await QRCode.toCanvas(checkinQr.value, links.value.checkinUrl, { width: 220, margin: 2 })
  }
}

async function generateCode() {
  if (!activity.value?.id) return
  generatingCode.value = true
  try {
    const result = await generateActivityCheckinCode(activity.value.id)
    activity.value = { ...activity.value, checkinCode: result.checkinCode || '' }
    if (!activity.value.checkinCode) {
      ElMessage.error('签到码生成失败，请重试')
      return
    }
    ElMessage.success('签到码已生成')
    await nextTick()
    await renderQrs()
  } finally {
    generatingCode.value = false
  }
}

async function copy(value, message) {
  if (!value) {
    ElMessage.warning('内容尚未生成')
    return
  }
  try {
    if (navigator.clipboard?.writeText && window.isSecureContext) {
      await navigator.clipboard.writeText(value)
    } else {
      copyWithTextArea(value)
    }
    ElMessage.success(message)
  } catch {
    ElMessage.error('复制失败，请长按链接或签到码手动复制')
  }
}

function copyWithTextArea(value) {
  const input = document.createElement('textarea')
  input.value = value
  input.setAttribute('readonly', '')
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(input)
  if (!copied) throw new Error('copy failed')
}

function open(url) {
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(load)
</script>
