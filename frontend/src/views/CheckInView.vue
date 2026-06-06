<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

// ---- Step management (0=gps, 1=face, 2=verify/result) ----
const currentStep = ref(0)
const stepRefs = ref([])

// ---- GPS ----
const gps = reactive({ latitude: null, longitude: null, accuracy: null })
const gpsLoading = ref(false)
const gpsError = ref('')
const gpsDone = ref(false)

// ---- Face ----
const videoRef = ref(null)
const canvasRef = ref(null)
const faceImageData = ref('')
const faceLoading = ref(false)
const faceError = ref('')
const faceDone = ref(false)
const cameraReady = ref(false)
let stream = null

// ---- Verify ----
const verifyLoading = ref(false)
const verifyResult = ref(null) // { faceVerified, locationVerified, checkedIn }
const verifyError = ref('')

// ---- GSAP transition ----
function animateStep(fromIdx, toIdx) {
  const allCards = document.querySelectorAll('.step-card')
  const fromEl = allCards[fromIdx]
  const toEl = allCards[toIdx]
  if (!fromEl || !toEl) return

  const tl = gsap.timeline()

  // Fade out current step
  tl.to(fromEl, {
    x: -40,
    opacity: 0,
    duration: 0.35,
    ease: 'power2.in',
    onComplete: () => {
      currentStep.value = toIdx
    },
  })

  // Fade in next step
  tl.fromTo(
    toEl,
    { x: 40, opacity: 0 },
    { x: 0, opacity: 1, duration: 0.4, ease: 'power2.out' },
    '-=0.05'
  )
}

function goToStep(step) {
  const prev = currentStep.value
  if (prev === step) return
  nextTick(() => animateStep(prev, step))
}

// ---- Step 1: GPS ----
function requestGPS() {
  if (!navigator.geolocation) {
    gpsError.value = '浏览器不支持定位功能'
    return
  }
  gpsLoading.value = true
  gpsError.value = ''

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      gps.latitude = pos.coords.latitude
      gps.longitude = pos.coords.longitude
      gps.accuracy = pos.coords.accuracy
      gpsDone.value = true
      gpsLoading.value = false

      // Success pulse animation
      nextTick(() => {
        const badge = document.querySelector('.gps-success')
        if (badge) {
          gsap.fromTo(badge, { scale: 0.6, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.4, ease: 'back.out(2)' })
        }
      })

      // Auto-advance to face step after a brief pause
      setTimeout(() => goToStep(1), 800)
    },
    (err) => {
      gpsLoading.value = false
      if (err.code === 1) gpsError.value = '定位权限被拒绝，请在浏览器设置中允许定位'
      else if (err.code === 2) gpsError.value = '无法获取位置信息，请检查 GPS'
      else if (err.code === 3) gpsError.value = '定位超时，请重试'
      else gpsError.value = '定位失败，请重试'
    },
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 60000 }
  )
}

// ---- Step 2: Face Camera ----
async function startCamera() {
  faceError.value = ''
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } },
      audio: false,
    })
    await nextTick()
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play()
      cameraReady.value = true
    }
  } catch (err) {
    if (err.name === 'NotAllowedError') faceError.value = '摄像头权限被拒绝，请在浏览器设置中允许'
    else if (err.name === 'NotFoundError') faceError.value = '未检测到摄像头设备'
    else faceError.value = '无法打开摄像头：' + err.message
  }
}

function stopCamera() {
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    stream = null
  }
  cameraReady.value = false
}

function captureFace() {
  if (!videoRef.value || !canvasRef.value) return
  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0)
  faceImageData.value = canvas.toDataURL('image/jpeg', 0.85)
  faceDone.value = true
  stopCamera()

  // Show captured preview with animation
  nextTick(() => {
    const preview = document.querySelector('.face-preview')
    if (preview) {
      gsap.fromTo(preview, { scale: 0.8, opacity: 0, rotateY: 15 }, { scale: 1, opacity: 1, rotateY: 0, duration: 0.5, ease: 'back.out(1.5)' })
    }
  })

  // Auto-advance to verify step
  setTimeout(() => goToStep(2), 800)
}

function retakeFace() {
  faceImageData.value = ''
  faceDone.value = false
  startCamera()
}

// ---- Step 3: Verify & Submit ----
async function submitVerify() {
  verifyLoading.value = true
  verifyError.value = ''
  verifyResult.value = null

  try {
    const res = await axios.post('/api/checkin/verify', {
      faceImageData: faceImageData.value,
      latitude: gps.latitude,
      longitude: gps.longitude,
    })

    if (res.data.code === 200) {
      verifyResult.value = res.data.data
      // Success animation
      nextTick(() => {
        const resultEl = document.querySelector('.result-panel')
        if (resultEl) {
          gsap.fromTo(resultEl, { y: 30, opacity: 0 }, { y: 0, opacity: 1, duration: 0.6, ease: 'power3.out' })
        }
      })
    } else {
      verifyError.value = res.data.message || '签到失败'
      verifyResult.value = res.data.data || null
    }
  } catch (err) {
    if (err.response?.data) {
      verifyError.value = err.response.data.message || '签到失败'
      verifyResult.value = err.response.data.data || null
    } else {
      verifyError.value = '网络错误，请检查后端服务'
    }
  } finally {
    verifyLoading.value = false
  }
}

// ---- Lifecycle ----
onMounted(async () => {
  // Entrance animation
  const card = document.querySelector('.checkin-card')
  if (card) {
    gsap.fromTo(card, { y: 50, opacity: 0 }, { y: 0, opacity: 1, duration: 0.7, ease: 'power3.out' })
  }

  // Check today's status first
  try {
    const res = await axios.get('/api/checkin/status')
    if (res.data.code === 200 && res.data.data?.checkedIn) {
      // Already checked in — show result immediately
      verifyResult.value = { faceVerified: true, locationVerified: true, checkedIn: true }
      currentStep.value = 2
    }
  } catch {
    // silent — proceed to check-in flow
  }

  // Start GPS immediately on mount
  requestGPS()
})

onUnmounted(() => {
  stopCamera()
})

// ---- Helpers ----
function formatCoord(val) {
  return val != null ? val.toFixed(6) : '--'
}

function goHome() {
  router.push('/')
}

function restart() {
  verifyResult.value = null
  verifyError.value = ''
  faceImageData.value = ''
  faceDone.value = false
  gpsDone.value = false
  gps.latitude = null
  gps.longitude = null
  gps.accuracy = null
  gpsError.value = ''
  currentStep.value = 0
  nextTick(() => requestGPS())
}
</script>

<template>
  <div class="checkin-page">
    <!-- Background glow orbs -->
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>

    <!-- Top bar -->
    <div class="top-bar">
      <button class="back-btn" @click="goHome">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        返回
      </button>
      <h1 class="page-title">签到打卡</h1>
      <div style="width: 64px"></div>
    </div>

    <!-- Step indicator -->
    <div class="steps-indicator">
      <div class="step-dot" :class="{ active: currentStep === 0, done: gpsDone }">
        <span class="dot-num">1</span>
        <span class="dot-label">GPS 定位</span>
      </div>
      <div class="step-line" :class="{ done: gpsDone }"></div>
      <div class="step-dot" :class="{ active: currentStep === 1, done: faceDone }">
        <span class="dot-num">2</span>
        <span class="dot-label">人脸拍照</span>
      </div>
      <div class="step-line" :class="{ done: faceDone }"></div>
      <div class="step-dot" :class="{ active: currentStep === 2, done: verifyResult?.checkedIn }">
        <span class="dot-num">3</span>
        <span class="dot-label">签到结果</span>
      </div>
    </div>

    <!-- Main card -->
    <div class="checkin-card">
      <!-- Step 1: GPS -->
      <div v-show="currentStep === 0" class="step-card">
        <div class="step-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
        </div>
        <h2 class="step-title">GPS 定位</h2>
        <p class="step-desc">正在获取您的位置信息，请确保已开启定位权限</p>

        <!-- GPS status -->
        <div v-if="gpsLoading" class="status-row">
          <div class="spinner"></div>
          <span>正在定位中...</span>
        </div>

        <div v-else-if="gpsError" class="error-box">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          <span>{{ gpsError }}</span>
          <button class="retry-btn" @click="requestGPS">重试</button>
        </div>

        <div v-else-if="gpsDone" class="gps-success">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <span class="success-text">定位成功</span>
          <div class="coord-info">
            <span>纬度：{{ formatCoord(gps.latitude) }}</span>
            <span>经度：{{ formatCoord(gps.longitude) }}</span>
            <span v-if="gps.accuracy">精度：{{ gps.accuracy.toFixed(0) }}m</span>
          </div>
          <button class="next-btn" @click="goToStep(1)">下一步：人脸拍照 →</button>
        </div>

        <div v-else class="status-row">
          <div class="spinner"></div>
          <span>准备定位...</span>
        </div>
      </div>

      <!-- Step 2: Face -->
      <div v-show="currentStep === 1" class="step-card">
        <div class="step-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <h2 class="step-title">人脸拍照</h2>
        <p class="step-desc">请正对摄像头，确保面部清晰可见</p>

        <!-- Camera view -->
        <div v-if="!faceDone" class="camera-container">
          <video
            v-show="cameraReady"
            ref="videoRef"
            class="camera-video"
            autoplay
            playsinline
            muted
          ></video>
          <canvas ref="canvasRef" style="display: none"></canvas>

          <div v-if="!cameraReady && !faceError" class="camera-placeholder">
            <div class="spinner"></div>
            <span>正在打开摄像头...</span>
            <button class="start-camera-btn" @click="startCamera">手动开启摄像头</button>
          </div>

          <div v-if="faceError" class="error-box">
            <span>{{ faceError }}</span>
            <button class="retry-btn" @click="startCamera">重试</button>
          </div>
        </div>

        <!-- Captured preview -->
        <div v-if="faceDone" class="face-preview">
          <img :src="faceImageData" alt="captured face" />
          <div class="preview-actions">
            <button class="retake-btn" @click="retakeFace">重新拍照</button>
            <button class="next-btn" @click="goToStep(2)">下一步：提交签到 →</button>
          </div>
        </div>

        <!-- Capture button -->
        <button
          v-if="cameraReady && !faceDone"
          class="capture-btn"
          @click="captureFace"
        >
          <span class="capture-ring">
            <span class="capture-inner"></span>
          </span>
        </button>

        <!-- Manual skip to verify (if face is done) -->
        <button v-if="faceDone" class="next-btn" @click="goToStep(2)" style="margin-top: 0">
          下一步：提交签到 →
        </button>
      </div>

      <!-- Step 3: Verify Result -->
      <div v-show="currentStep === 2" class="step-card">
        <!-- Submitting -->
        <div v-if="verifyLoading" class="verifying-state">
          <div class="spinner large"></div>
          <h2 class="step-title">正在提交签到...</h2>
          <p class="step-desc">正在验证人脸和位置信息</p>
        </div>

        <!-- Not yet submitted — show summary + confirm -->
        <div v-else-if="!verifyResult && !verifyError" class="verify-summary">
          <div class="step-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <h2 class="step-title">确认签到</h2>
          <div class="summary-grid">
            <div class="summary-item">
              <span class="summary-label">📍 位置</span>
              <span class="summary-value">{{ formatCoord(gps.latitude) }}, {{ formatCoord(gps.longitude) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">📷 人脸</span>
              <span class="summary-value">{{ faceDone ? '已拍照' : '未拍照' }}</span>
            </div>
          </div>
          <div class="summary-actions">
            <button class="back-step-btn" @click="goToStep(1)">返回修改</button>
            <button class="submit-btn" @click="submitVerify">确认签到</button>
          </div>
        </div>

        <!-- Error with partial result -->
        <div v-else-if="verifyError" class="result-panel result-fail">
          <div class="result-icon fail">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
          <h2 class="result-title">签到失败</h2>
          <p class="result-message">{{ verifyError }}</p>
          <div v-if="verifyResult" class="result-details">
            <div class="detail-row">
              <span>人脸识别</span>
              <span :class="verifyResult.faceVerified ? 'pass' : 'fail'">{{ verifyResult.faceVerified ? '✓ 通过' : '✗ 未通过' }}</span>
            </div>
            <div class="detail-row">
              <span>位置验证</span>
              <span :class="verifyResult.locationVerified ? 'pass' : 'fail'">{{ verifyResult.locationVerified ? '✓ 通过' : '✗ 未通过' }}</span>
            </div>
          </div>
          <button class="retry-submit-btn" @click="restart">重新签到</button>
        </div>

        <!-- Success -->
        <div v-else-if="verifyResult?.checkedIn" class="result-panel result-success">
          <div class="result-icon success">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <h2 class="result-title">签到成功！</h2>
          <p class="result-message">您已完成本次寝室签到</p>
          <div class="result-details">
            <div class="detail-row">
              <span>人脸识别</span>
              <span class="pass">✓ 通过</span>
            </div>
            <div class="detail-row">
              <span>位置验证</span>
              <span class="pass">✓ 通过</span>
            </div>
          </div>
          <button class="home-btn" @click="goHome">返回首页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkin-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  position: relative;
  overflow: hidden;
  padding: 0 0 40px;
}

/* ---- Background glows ---- */
.glow {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  pointer-events: none;
  animation: float 10s ease-in-out infinite;
}
.glow-1 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #667eea, #764ba2);
  top: -80px;
  left: -80px;
}
.glow-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #4facfe, #00f2fe);
  bottom: -60px;
  right: -60px;
  animation-delay: -4s;
}
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(25px, -25px) scale(1.05); }
  50% { transform: translate(-15px, 15px) scale(0.95); }
  75% { transform: translate(10px, 10px) scale(1.02); }
}

/* ---- Top bar ---- */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  position: relative;
  z-index: 2;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 8px 14px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: all 0.2s;
}
.back-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.page-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* ---- Steps indicator ---- */
.steps-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 8px 20px 20px;
  position: relative;
  z-index: 2;
}
.step-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
}
.dot-num {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.5);
  transition: all 0.3s;
}
.step-dot.active .dot-num {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 0 16px rgba(102, 126, 234, 0.4);
}
.step-dot.done .dot-num {
  background: #059669;
  border-color: transparent;
  color: #fff;
}
.dot-label {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 500;
}
.step-dot.active .dot-label {
  color: rgba(255, 255, 255, 0.9);
}
.step-dot.done .dot-label {
  color: rgba(255, 255, 255, 0.7);
}
.step-line {
  width: 60px;
  height: 2px;
  background: rgba(255, 255, 255, 0.15);
  margin: 0 8px 24px;
  border-radius: 1px;
  transition: background 0.3s;
}
.step-line.done {
  background: #059669;
}

/* ---- Main card ---- */
.checkin-card {
  max-width: 480px;
  margin: 0 auto;
  padding: 0 16px;
  position: relative;
  z-index: 2;
}

/* ---- Glassmorphism step card ---- */
.step-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  text-align: center;
  color: #fff;
}

.step-icon {
  margin-bottom: 16px;
  color: rgba(255, 255, 255, 0.6);
}
.step-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}
.step-desc {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 24px;
  line-height: 1.5;
}

/* ---- Status row ---- */
.status-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.9rem;
  padding: 20px 0;
}

/* ---- Spinner ---- */
.spinner {
  width: 22px;
  height: 22px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
.spinner.large {
  width: 44px;
  height: 44px;
  border-width: 4px;
  margin-bottom: 16px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- Error box ---- */
.error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: rgba(255, 107, 107, 0.1);
  border: 1px solid rgba(255, 107, 107, 0.2);
  border-radius: 14px;
  color: #ff6b6b;
  font-size: 0.875rem;
  line-height: 1.5;
}
.error-box svg {
  flex-shrink: 0;
}

/* ---- GPS success ---- */
.gps-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #059669;
}
.gps-success svg {
  filter: drop-shadow(0 0 12px rgba(5, 150, 105, 0.4));
}
.success-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: #fff;
}
.coord-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.55);
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
  padding: 12px 20px;
}

/* ---- Buttons ---- */
.next-btn, .retry-btn, .submit-btn, .home-btn, .retake-btn, .back-step-btn, .retry-submit-btn, .start-camera-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}
.next-btn, .submit-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  margin-top: 20px;
}
.next-btn:hover, .submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
}
.retry-btn {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 8px 16px;
  font-size: 0.8rem;
  margin-top: 4px;
}
.retry-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
.retake-btn, .back-step-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.8);
}
.retake-btn:hover, .back-step-btn:hover {
  background: rgba(255, 255, 255, 0.18);
}
.home-btn {
  background: linear-gradient(135deg, #059669, #10b981);
  color: #fff;
  box-shadow: 0 4px 15px rgba(5, 150, 105, 0.3);
  margin-top: 20px;
}
.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.45);
}
.retry-submit-btn {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
  margin-top: 20px;
}
.retry-submit-btn:hover {
  transform: translateY(-2px);
}
.start-camera-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.8rem;
  padding: 8px 16px;
  margin-top: 8px;
}

/* ---- Camera ---- */
.camera-container {
  position: relative;
  width: 100%;
  max-width: 360px;
  margin: 0 auto 20px;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.3);
  aspect-ratio: 4 / 3;
}
.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transform: scaleX(-1); /* mirror */
}
.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
  padding: 20px;
}

/* ---- Capture button ---- */
.capture-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  margin: 0 auto;
  padding: 0;
}
.capture-ring {
  width: 68px;
  height: 68px;
  border: 4px solid rgba(255, 255, 255, 0.7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.capture-btn:hover .capture-ring {
  border-color: #fff;
  transform: scale(1.05);
}
.capture-inner {
  width: 52px;
  height: 52px;
  background: #fff;
  border-radius: 50%;
  transition: all 0.15s;
}
.capture-btn:active .capture-inner {
  transform: scale(0.85);
  background: rgba(255, 255, 255, 0.8);
}

/* ---- Face preview ---- */
.face-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.face-preview img {
  width: 100%;
  max-width: 320px;
  border-radius: 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}
.preview-actions {
  display: flex;
  gap: 12px;
}

/* ---- Verify summary ---- */
.verify-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.summary-grid {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 20px 0;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.summary-label {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.6);
}
.summary-value {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}
.summary-actions {
  display: flex;
  gap: 12px;
}

/* ---- Verifying state ---- */
.verifying-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

/* ---- Result panels ---- */
.result-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-icon {
  margin-bottom: 20px;
}
.result-icon.success {
  color: #059669;
  filter: drop-shadow(0 0 20px rgba(5, 150, 105, 0.4));
}
.result-icon.fail {
  color: #ef4444;
  filter: drop-shadow(0 0 20px rgba(239, 68, 68, 0.3));
}
.result-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
}
.result-message {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 24px;
}
.result-details {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.7);
}
.detail-row .pass {
  color: #059669;
  font-weight: 600;
}
.detail-row .fail {
  color: #ef4444;
  font-weight: 600;
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .step-card {
    padding: 32px 20px;
  }
  .step-line {
    width: 36px;
  }
  .checkin-card {
    padding: 0 12px;
  }
  .dot-label {
    font-size: 0.625rem;
  }
}
</style>
