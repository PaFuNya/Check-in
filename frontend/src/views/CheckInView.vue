<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const currentStep = ref(0)

// GPS
const gps = reactive({ latitude: null, longitude: null, accuracy: null })
const gpsLoading = ref(false)
const gpsError = ref('')
const gpsDone = ref(false)

// Face
const videoRef = ref(null)
const canvasRef = ref(null)
const faceImageData = ref('')
const faceLoading = ref(false)
const faceError = ref('')
const faceDone = ref(false)
const cameraReady = ref(false)
let stream = null

// Verify
const verifyLoading = ref(false)
const verifyResult = ref(null)
const verifyError = ref('')

function formatCoord(val) { return val != null ? val.toFixed(6) : '--' }

function requestGPS() {
  if (!navigator.geolocation) { gpsError.value = '浏览器不支持定位功能'; return }
  gpsLoading.value = true; gpsError.value = ''
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      gps.latitude = pos.coords.latitude; gps.longitude = pos.coords.longitude
      gps.accuracy = pos.coords.accuracy; gpsDone.value = true; gpsLoading.value = false
      nextTick(() => {
        const el = document.querySelector('.gps-success-badge')
        if (el) gsap.fromTo(el, { scale: 0.6, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.4, ease: 'back.out(2)' })
      })
      setTimeout(() => { currentStep.value = 1 }, 800)
    },
    (err) => {
      gpsLoading.value = false
      if (err.code === 1) gpsError.value = '定位权限被拒绝，请在浏览器设置中允许定位'
      else if (err.code === 2) gpsError.value = '无法获取位置信息'
      else if (err.code === 3) gpsError.value = '定位超时，请重试'
      else gpsError.value = '定位失败，请重试'
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 60000 }
  )
}

async function startCamera() {
  faceError.value = ''
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }, audio: false })
    await nextTick()
    if (videoRef.value) { videoRef.value.srcObject = stream; await videoRef.value.play(); cameraReady.value = true }
  } catch (err) {
    if (err.name === 'NotAllowedError') faceError.value = '摄像头权限被拒绝'
    else if (err.name === 'NotFoundError') faceError.value = '未检测到摄像头'
    else faceError.value = '无法打开摄像头：' + err.message
  }
}

function stopCamera() {
  if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null }
  cameraReady.value = false
}

function captureFace() {
  if (!videoRef.value || !canvasRef.value) return
  const c = canvasRef.value; c.width = videoRef.value.videoWidth; c.height = videoRef.value.videoHeight
  c.getContext('2d').drawImage(videoRef.value, 0, 0)
  faceImageData.value = c.toDataURL('image/jpeg', 0.85)
  faceDone.value = true; stopCamera()
  nextTick(() => {
    const el = document.querySelector('.face-preview')
    if (el) gsap.fromTo(el, { scale: 0.85, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.4, ease: 'back.out(1.5)' })
  })
  setTimeout(() => { currentStep.value = 2 }, 800)
}

function retakeFace() { faceImageData.value = ''; faceDone.value = false; startCamera() }

async function submitVerify() {
  verifyLoading.value = true; verifyError.value = ''; verifyResult.value = null
  try {
    const res = await axios.post('/api/checkin/verify', { faceImageData: faceImageData.value, latitude: gps.latitude, longitude: gps.longitude })
    if (res.data.code === 200) {
      verifyResult.value = res.data.data
      nextTick(() => { const el = document.querySelector('.result-panel'); if (el) gsap.fromTo(el, { y: 20, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, ease: 'power3.out' }) })
    } else { verifyError.value = res.data.message || '签到失败'; verifyResult.value = res.data.data || null }
  } catch (err) {
    verifyError.value = err.response?.data?.message || '网络错误'
    verifyResult.value = err.response?.data?.data || null
  } finally { verifyLoading.value = false }
}

function restart() {
  verifyResult.value = null; verifyError.value = ''; faceImageData.value = ''
  faceDone.value = false; gpsDone.value = false; gps.latitude = null; gps.longitude = null
  gps.accuracy = null; gpsError.value = ''; currentStep.value = 0
  nextTick(() => requestGPS())
}

function goToStep(step) {
  if (step === 0 && gpsDone) { currentStep.value = 0; return }
  if (step === 1 && faceDone) { currentStep.value = 1; return }
}

onMounted(async () => {
  gsap.from('.checkin-wrapper', { y: 40, opacity: 0, duration: 0.6, ease: 'power3.out' })
  try {
    const res = await axios.get('/api/checkin/status')
    if (res.data.code === 200 && res.data.data?.checkedIn) {
      verifyResult.value = { faceVerified: true, locationVerified: true, checkedIn: true }
      currentStep.value = 2; return
    }
  } catch { /* proceed */ }
  requestGPS()
})

onUnmounted(() => { stopCamera() })
</script>

<template>
  <div class="checkin-wrapper">
    <!-- Steps indicator -->
    <div class="steps-bar">
      <div class="step-item" :class="{ active: currentStep === 0, done: gpsDone }" @click="goToStep(0)">
        <span class="step-num">
          <svg v-if="gpsDone" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          <span v-else>1</span>
        </span>
        <span class="step-label">GPS</span>
      </div>
      <div class="step-line" :class="{ done: gpsDone }"></div>
      <div class="step-item" :class="{ active: currentStep === 1, done: faceDone }" @click="goToStep(1)">
        <span class="step-num">
          <svg v-if="faceDone" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          <span v-else>2</span>
        </span>
        <span class="step-label">人脸</span>
      </div>
      <div class="step-line" :class="{ done: faceDone }"></div>
      <div class="step-item" :class="{ active: currentStep === 2, done: verifyResult?.checkedIn }">
        <span class="step-num">
          <svg v-if="verifyResult?.checkedIn" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          <span v-else>3</span>
        </span>
        <span class="step-label">签到</span>
      </div>
    </div>

    <!-- Step 1: GPS -->
    <div v-if="currentStep === 0" class="step-card glass-card">
      <div class="step-back-row">
        <button class="back-link" @click="router.push('/')">返回</button>
      </div>
      <div class="step-icon">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z" /><circle cx="12" cy="10" r="3" />
        </svg>
      </div>
      <h2 class="step-title">GPS 定位</h2>
      <p class="step-desc">正在获取您的位置信息</p>

      <div v-if="gpsLoading" class="loading-row">
        <div class="spinner"></div><span>定位中...</span>
      </div>
      <div v-else-if="gpsError" class="error-box">
        <span>{{ gpsError }}</span>
        <button class="btn-outline cursor-pointer" @click="requestGPS">重试</button>
      </div>
      <div v-else-if="gpsDone" class="gps-success-badge">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span class="gps-text">定位成功</span>
        <div class="coord-box">
          <span>纬度: {{ formatCoord(gps.latitude) }}</span>
          <span>经度: {{ formatCoord(gps.longitude) }}</span>
          <span v-if="gps.accuracy">精度: {{ gps.accuracy.toFixed(0) }}m</span>
        </div>
        <button class="btn-primary cursor-pointer" @click="currentStep = 1">下一步：人脸拍照</button>
      </div>
      <div v-else class="loading-row"><div class="spinner"></div><span>准备定位...</span></div>
    </div>

    <!-- Step 2: Face -->
    <div v-if="currentStep === 1" class="step-card glass-card">
      <div class="step-back-row">
        <button class="back-link" @click="currentStep = 0">返回上一步</button>
      </div>
      <div class="step-icon">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
        </svg>
      </div>
      <h2 class="step-title">人脸拍照</h2>
      <p class="step-desc">请正对摄像头，确保面部清晰可见</p>

      <div v-if="!faceDone" class="camera-box">
        <video v-show="cameraReady" ref="videoRef" class="camera-video" autoplay playsinline muted></video>
        <canvas ref="canvasRef" style="display:none"></canvas>
        <div v-if="!cameraReady && !faceError" class="camera-placeholder">
          <div class="spinner"></div><span>打开摄像头中...</span>
          <button class="btn-outline cursor-pointer" @click="startCamera">手动开启</button>
        </div>
        <div v-if="faceError" class="error-box"><span>{{ faceError }}</span><button class="btn-outline cursor-pointer" @click="startCamera">重试</button></div>
      </div>

      <div v-if="faceDone" class="face-preview">
        <img :src="faceImageData" alt="已拍照" class="face-img" />
        <div class="preview-actions">
          <button class="btn-ghost cursor-pointer" @click="retakeFace">重新拍照</button>
          <button class="btn-primary cursor-pointer" @click="currentStep = 2">下一步</button>
        </div>
      </div>

      <button v-if="cameraReady && !faceDone" class="capture-btn cursor-pointer" @click="captureFace" aria-label="拍照">
        <span class="capture-ring"><span class="capture-dot"></span></span>
      </button>
    </div>

    <!-- Step 3: Result -->
    <div v-if="currentStep === 2" class="step-card glass-card">
      <div v-if="verifyLoading" class="loading-state">
        <div class="spinner large"></div><h2 class="step-title">提交中...</h2><p class="step-desc">正在验证人脸和位置</p>
      </div>

      <div v-else-if="!verifyResult && !verifyError" class="verify-summary">
        <div class="step-back-row">
          <button class="back-link" @click="currentStep = 1">返回修改</button>
        </div>
        <div class="step-icon">
          <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <h2 class="step-title">确认签到</h2>
        <div class="summary-box">
          <div class="summary-row">
            <span class="summary-label">位置</span>
            <span class="summary-value">{{ formatCoord(gps.latitude) }}, {{ formatCoord(gps.longitude) }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">人脸</span>
            <span class="summary-value">{{ faceDone ? '已拍照' : '未拍照' }}</span>
          </div>
        </div>
        <div class="summary-actions">
          <button class="btn-ghost cursor-pointer" @click="currentStep = 1">返回修改</button>
          <button class="btn-primary cursor-pointer" @click="submitVerify">确认签到</button>
        </div>
      </div>

      <div v-else-if="verifyError" class="result-panel">
        <div class="result-icon-fail">
          <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <h2 class="step-title">签到失败</h2>
        <p class="step-desc">{{ verifyError }}</p>
        <div v-if="verifyResult" class="detail-box">
          <div class="detail-row"><span>人脸识别</span><span :class="verifyResult.faceVerified ? 'text-success' : 'text-error'">{{ verifyResult.faceVerified ? '通过' : '未通过' }}</span></div>
          <div class="detail-row"><span>位置验证</span><span :class="verifyResult.locationVerified ? 'text-success' : 'text-error'">{{ verifyResult.locationVerified ? '通过' : '未通过' }}</span></div>
        </div>
        <button class="btn-warning cursor-pointer" @click="restart">重新签到</button>
      </div>

      <div v-else-if="verifyResult?.checkedIn" class="result-panel">
        <div class="result-icon-success">
          <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <h2 class="step-title" style="color: #059669">签到成功</h2>
        <p class="step-desc">您已完成本次寝室签到</p>
        <div class="detail-box">
          <div class="detail-row"><span>人脸识别</span><span class="text-success">通过</span></div>
          <div class="detail-row"><span>位置验证</span><span class="text-success">通过</span></div>
        </div>
        <button class="btn-success cursor-pointer" @click="router.push('/')">返回首页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkin-wrapper { max-width: 480px; margin: 0 auto; }

/* Steps */
.steps-bar { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 24px; padding: 0 20px; }
.step-item { display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: default; }
.step-item.done { cursor: pointer; }
.step-num { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.8125rem; font-weight: 700; background: #F1F5F9; border: 2px solid #E2E8F0; color: #94A3B8; transition: all 0.3s; }
.dark .step-num { background: rgba(30, 41, 59, 0.6); border-color: rgba(255, 255, 255, 0.1); }
.step-item.active .step-num { background: linear-gradient(135deg, #2563EB, #3B82F6); border-color: transparent; color: white; box-shadow: 0 0 16px rgba(37, 99, 235, 0.3); }
.step-item.done .step-num { background: #059669; border-color: transparent; color: white; }
.step-label { font-size: 0.6875rem; color: #94A3B8; font-weight: 500; }
.step-item.active .step-label { color: #2563EB; }
.dark .step-item.active .step-label { color: #60A5FA; }
.step-item.done .step-label { color: #059669; }
.step-line { width: 48px; height: 2px; background: #E2E8F0; margin: 0 8px 22px; border-radius: 1px; }
.dark .step-line { background: rgba(255, 255, 255, 0.1); }
.step-line.done { background: #059669; }

/* Card */
.step-card { padding: 32px 28px; text-align: center; }
.step-back-row { text-align: left; margin-bottom: 8px; }
.back-link { background: none; border: none; color: var(--color-primary); font-size: 0.8125rem; cursor: pointer; font-family: inherit; padding: 0; }
.back-link:hover { text-decoration: underline; }
.step-icon { margin-bottom: 12px; }
.step-title { font-size: 1.25rem; font-weight: 700; color: var(--color-text); margin: 0 0 6px; }
.step-desc { font-size: 0.875rem; color: var(--color-text-light); margin: 0 0 20px; }

/* Loading */
.loading-row, .loading-state { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 20px 0; color: var(--color-text-muted); }
.spinner { width: 24px; height: 24px; border: 3px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
.dark .spinner { border-color: rgba(255, 255, 255, 0.1); border-top-color: #60A5FA; }
.spinner.large { width: 40px; height: 40px; border-width: 4px; margin-bottom: 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Error */
.error-box { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 16px; background: var(--color-error-light); border: 1px solid #FECACA; border-radius: 12px; color: var(--color-error); font-size: 0.875rem; }
.dark .error-box { border-color: rgba(220, 38, 38, 0.2); }

/* GPS Success */
.gps-success-badge { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.gps-text { font-size: 1rem; font-weight: 700; color: #059669; }
.coord-box { display: flex; flex-direction: column; gap: 3px; font-size: 0.8125rem; color: var(--color-text-muted); background: #F8FAFC; border-radius: 10px; padding: 12px 20px; }
.dark .coord-box { background: rgba(30, 41, 59, 0.5); color: var(--color-text-muted); }

/* Camera */
.camera-box { position: relative; width: 100%; max-width: 340px; margin: 0 auto 16px; border-radius: 16px; overflow: hidden; background: #1E293B; aspect-ratio: 4/3; }
.camera-video { width: 100%; height: 100%; object-fit: cover; display: block; transform: scaleX(-1); }
.camera-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 10px; color: #94A3B8; font-size: 0.875rem; }

/* Capture */
.capture-btn { display: flex; align-items: center; justify-content: center; width: 72px; height: 72px; border-radius: 50%; border: none; background: transparent; margin: 0 auto; }
.capture-ring { width: 64px; height: 64px; border: 4px solid #E2E8F0; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.capture-btn:hover .capture-ring { border-color: #2563EB; transform: scale(1.05); }
.capture-dot { width: 48px; height: 48px; background: #2563EB; border-radius: 50%; transition: all 0.15s; }
.capture-btn:active .capture-dot { transform: scale(0.85); }

/* Face preview */
.face-preview { display: flex; flex-direction: column; align-items: center; gap: 16px; }
.face-img { width: 100%; max-width: 300px; border-radius: 16px; border: 2px solid #E2E8F0; }
.dark .face-img { border-color: rgba(255, 255, 255, 0.1); }
.preview-actions { display: flex; gap: 12px; }

/* Summary */
.verify-summary { display: flex; flex-direction: column; align-items: center; }
.summary-box { width: 100%; display: flex; flex-direction: column; gap: 10px; margin: 16px 0; }
.summary-row { display: flex; justify-content: space-between; padding: 12px 16px; background: #F8FAFC; border-radius: 10px; font-size: 0.875rem; }
.dark .summary-row { background: rgba(30, 41, 59, 0.5); }
.summary-label { color: var(--color-text-muted); }
.summary-value { color: var(--color-text); font-weight: 500; }
.summary-actions { display: flex; gap: 12px; margin-top: 8px; }

/* Result */
.result-panel { display: flex; flex-direction: column; align-items: center; }
.result-icon-success { margin-bottom: 16px; filter: drop-shadow(0 0 16px rgba(5, 150, 105, 0.3)); }
.result-icon-fail { margin-bottom: 16px; filter: drop-shadow(0 0 16px rgba(220, 38, 38, 0.3)); }
.detail-box { width: 100%; display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; }
.detail-row { display: flex; justify-content: space-between; padding: 12px 16px; background: #F8FAFC; border-radius: 10px; font-size: 0.875rem; color: var(--color-text-muted); }
.dark .detail-row { background: rgba(30, 41, 59, 0.5); }
.text-success { color: #059669; font-weight: 600; }
.text-error { color: #DC2626; font-weight: 600; }

/* Buttons */
.btn-primary { padding: 10px 24px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border: none; border-radius: 10px; font-size: 0.9rem; font-weight: 600; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3); min-height: 44px; font-family: inherit; cursor: pointer; }
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4); }
.btn-ghost { padding: 10px 24px; background: #F1F5F9; color: #64748B; border: 1px solid #E2E8F0; border-radius: 10px; font-size: 0.9rem; font-weight: 500; transition: all 0.2s; min-height: 44px; font-family: inherit; cursor: pointer; }
.dark .btn-ghost { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.1); color: #94A3B8; }
.btn-ghost:hover { background: #E2E8F0; }
.btn-outline { padding: 8px 20px; background: transparent; color: #2563EB; border: 1px solid #2563EB; border-radius: 8px; font-size: 0.8125rem; font-weight: 500; cursor: pointer; transition: all 0.2s; min-height: 40px; font-family: inherit; }
.btn-outline:hover { background: rgba(37, 99, 235, 0.08); }
.btn-success { padding: 10px 24px; background: linear-gradient(135deg, #059669, #10B981); color: white; border: none; border-radius: 10px; font-size: 0.9rem; font-weight: 600; box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3); min-height: 44px; font-family: inherit; cursor: pointer; transition: all 0.2s; }
.btn-success:hover { transform: translateY(-1px); }
.btn-warning { padding: 10px 24px; background: linear-gradient(135deg, #D97706, #F59E0B); color: white; border: none; border-radius: 10px; font-size: 0.9rem; font-weight: 600; box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3); min-height: 44px; font-family: inherit; cursor: pointer; transition: all 0.2s; }
.btn-warning:hover { transform: translateY(-1px); }

@media (max-width: 480px) { .step-card { padding: 24px 18px; } .step-line { width: 32px; } }
</style>
