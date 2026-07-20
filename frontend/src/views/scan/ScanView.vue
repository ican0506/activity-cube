<template>
  <section class="scan-page">
    <div class="hero campus-hero scan-hero">
      <div>
        <div class="motto-badge">校园活动 · 快速参与</div>
        <h1>扫一扫</h1>
        <p>扫描报名二维码或签到二维码</p>
        <p class="scan-hero-note">请将二维码放入扫描框内，识别成功后将自动跳转。</p>
      </div>
      <div class="hero-card scan-hero-card">
        <span>扫码类型</span>
        <strong>报名 / 签到</strong>
        <p>{{ showFallback ? '摄像头不可用时，可使用下方备用输入方式继续。' : '进入页面后会自动打开摄像头开始识别。' }}</p>
      </div>
    </div>

    <div class="scan-layout">
      <div class="panel scan-camera-card agri-card">
        <div class="section-title scan-section-title">
          <div>
            <h2>摄像头扫码</h2>
            <p>授权浏览器使用摄像头后，将二维码放入取景框。</p>
          </div>
          <el-tag :type="cameraRunning ? 'success' : 'info'">{{ cameraRunning ? '识别中' : '未开启' }}</el-tag>
        </div>

        <div class="scan-video-wrap">
          <video ref="videoRef" muted playsinline></video>
          <div v-if="!cameraRunning" class="scan-placeholder">{{ cameraPlaceholder }}</div>
        </div>

        <div class="button-row scan-camera-actions">
          <el-button type="primary" :loading="startingCamera" :disabled="cameraRunning || startingCamera" @click="startCamera">启动扫码</el-button>
          <el-button :disabled="!cameraRunning" @click="stopCamera">停止扫码</el-button>
        </div>

        <el-alert
          v-if="cameraMessage"
          class="qr-tip"
          :title="cameraMessage"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>

      <div v-if="showFallback" ref="fallbackRef" class="panel scan-manual-card agri-card">
        <div class="section-title scan-section-title">
          <div>
            <h2>备用方式</h2>
            <p>摄像头无法启动时，仍可通过签到码或二维码链接进入活动。</p>
          </div>
        </div>

        <div class="scan-manual-form">
          <div class="scan-manual-block">
            <label for="checkin-code">输入签到码</label>
            <div class="scan-input-row">
              <el-input id="checkin-code" v-model="checkinCode" placeholder="请输入现场签到码" @keyup.enter="submitCheckinCode" />
              <el-button type="primary" :loading="resolvingCheckinCode" @click="submitCheckinCode">确认签到码</el-button>
            </div>
            <p>系统会自动根据签到码找到对应活动，再进入签到确认页。</p>
          </div>

          <div class="scan-manual-block">
            <label for="qr-link">粘贴二维码链接</label>
            <div class="scan-input-row">
              <el-input id="qr-link" v-model="qrLink" placeholder="请粘贴二维码链接" @keyup.enter="openQrLink" />
              <el-button type="primary" plain @click="openQrLink">打开链接</el-button>
            </div>
            <p>支持完整链接或以 <code>/scan/resolve</code> 开头的二维码路径。</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resolveCheckinCode } from '../../api/scan'
import { buildScanTarget } from '../../utils/scanResolve'

const router = useRouter()
const videoRef = ref(null)
const fallbackRef = ref(null)
const checkinCode = ref('')
const qrLink = ref('')
const cameraMessage = ref('')
const cameraRunning = ref(false)
const startingCamera = ref(false)
const showFallback = ref(false)
const resolvingCheckinCode = ref(false)
let stream = null
let scanTimer = null

const cameraPlaceholder = computed(() => {
  if (startingCamera.value) return '正在启动摄像头'
  if (cameraMessage.value) return '请使用下方备用方式继续'
  return '正在准备摄像头'
})

async function startCamera() {
  if (cameraRunning.value || startingCamera.value) return
  cameraMessage.value = ''
  showFallback.value = false

  if (!window.isSecureContext) {
    showCameraUnavailable()
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    showCameraUnavailable()
    return
  }
  if (!('BarcodeDetector' in window)) {
    showCameraUnavailable()
    return
  }

  startingCamera.value = true
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' } },
      audio: false
    })
    videoRef.value.srcObject = stream
    await videoRef.value.play()
    cameraRunning.value = true
    scanLoop()
  } catch {
    stopCamera()
    showCameraUnavailable()
  } finally {
    startingCamera.value = false
  }
}

function showCameraUnavailable() {
  cameraMessage.value = '当前环境无法使用摄像头，已为你切换到备用方式。'
  showFallback.value = true
  nextTick(() => {
    fallbackRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function scanLoop() {
  if (scanTimer) {
    window.clearInterval(scanTimer)
    scanTimer = null
  }
  const Detector = window.BarcodeDetector
  const detector = new Detector({ formats: ['qr_code'] })
  scanTimer = window.setInterval(async () => {
    if (!videoRef.value || videoRef.value.readyState < 2) return
    try {
      const codes = await detector.detect(videoRef.value)
      const rawValue = codes[0]?.rawValue
      if (rawValue) {
        stopCamera()
        navigate(rawValue, '未识别到有效的活动二维码')
      }
    } catch {
      // A failed frame should not stop the camera; the next frame can still be decoded.
    }
  }, 600)
}

function stopCamera() {
  if (scanTimer) {
    window.clearInterval(scanTimer)
    scanTimer = null
  }
  if (stream) {
    stream.getTracks().forEach((track) => track.stop())
    stream = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  cameraRunning.value = false
}

async function submitCheckinCode() {
  if (!checkinCode.value.trim()) {
    ElMessage.warning('请输入现场签到码')
    return
  }
  resolvingCheckinCode.value = true
  try {
    const result = await resolveCheckinCode(checkinCode.value.trim())
    router.push(buildScanTarget(result.checkinCode || checkinCode.value, { activityId: result.activityId }))
  } catch {
    // The request interceptor displays the backend's friendly error message.
  } finally {
    resolvingCheckinCode.value = false
  }
}

function openQrLink() {
  navigate(qrLink.value, '二维码链接格式不正确')
}

function navigate(value, invalidMessage) {
  const target = buildScanTarget(value)
  if (!target) {
    ElMessage.warning(invalidMessage)
    return
  }
  router.push(target)
}

onMounted(startCamera)
onBeforeUnmount(stopCamera)
</script>
