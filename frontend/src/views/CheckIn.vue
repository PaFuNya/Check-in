<template>
  <div class="checkin-page fade-in">
    <h1 class="page-title">📍 签到</h1>

    <!-- Step indicator -->
    <div class="steps-bar">
      <div v-for="s in 3" :key="s" class="step-dot" :class="{ active: step >= s, done: step > s }">
        {{ s }}
      </div>
      <div class="step-line"><div class="step-fill" :style="{ width: ((step - 1) / 2 * 100) + '%' }"></div></div>
    </div>

    <!-- Step 1: GPS -->
    <div v-if="step === 1" class="glass step-card fade-in">
      <h2>① 获取位置</h2>
      <p class="step-desc">需要获取您的GPS定位信息</p>
      <button class="btn-primary" @click="getGPS" :disabled="gpsLoading">
        {{ gpsLoading ? '定位中...' : '获取位置' }}
      </button>
      <p v-if="gpsError" class="error-msg">⚠️ {{ gpsError }}</p>
      <p v-if="latitude" class="success-msg">✅ 经度: {{ latitude.toFixed(6) }}, 纬度: {{ longitude.toFixed(6) }}</p>
    </div>

    <!-- Step 2: Face -->
    <div v-if="step === 2" class="glass step-card fade-in">
      <h2>② 人脸拍照</h2>
      <p class="step-desc">请正对摄像头，确保面部清晰</p>
      <div class="video-wrapper" v-show="!captured">
        <video ref="videoRef" autoplay playsinline muted></video>
        <div class="face-guide"></div>
      </div>
      <canvas ref="canvasRef" v-show="captured" class="captured-img"></canvas>
      <div class="step-actions">
        <button v-if="!captured" class="btn-primary" @click="captureFace">📸 拍照</button>
        <button v-else class="btn-primary" @click="retakeFace">🔄 重拍</button>
      </div>
      <p v-if="faceError" class="error-msg">⚠️ {{ faceError }}</p>
    </div>

    <!-- Step 3: Verify -->
    <div v-if="step === 3" class="glass step-card fade-in">
      <h2>③ 提交签到</h2>
      <p class="step-desc">信息已就绪，请提交验证</p>
      <button class="btn-primary" @click="submitCheckin" :disabled="verifying">
        {{ verifying ? '验证中...' : '提交签到' }}
      </button>
      <p v-if="verifyError" class="error-msg">⚠️ {{ verifyError }}</p>
      <button v-if="verifyError" class="btn-secondary" @click="retryAll">🔄 重新签到</button>
    </div>

    <!-- Success -->
    <div v-if="step === 4" class="glass step-card fade-in success-card">
      <div class="success-icon">🎉</div>
      <h2>签到成功！</h2>
      <button class="btn-primary" @click="$router.push('/')">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import axios from 'axios'
import gsap from 'gsap'

const step = ref(1)
const latitude = ref(null)
const longitude = ref(null)
const gpsLoading = ref(false)
const gpsError = ref('')

const videoRef = ref(null)
const canvasRef = ref(null)
const captured = ref(false)
const faceImageData = ref('')
const faceError = ref('')

const verifying = ref(false)
const verifyError = ref('')

let stream = null

function getGPS() {
  gpsLoading.value = true; gpsError.value = ''
  if (!navigator.geolocation) { gpsError.value = '浏览器不支持定位'; gpsLoading.value = false; return }
  navigator.geolocation.getCurrentPosition(
    pos => {
      latitude.value = pos.coords.latitude; longitude.value = pos.coords.longitude
      gpsLoading.value = false
      gsap.to({}, { duration: 0.5, onComplete: () => { step.value = 2; startCamera() } })
    },
    err => { gpsError.value = '定位失败: ' + err.message; gpsLoading.value = false },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

async function startCamera() {
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: 640, height: 480 } })
    if (videoRef.value) videoRef.value.srcObject = stream
  } catch { faceError.value = '无法访问摄像头' }
}

function captureFace() {
  const video = videoRef.value; const canvas = canvasRef.value
  if (!video || !canvas) return
  canvas.width = video.videoWidth || 640; canvas.height = video.videoHeight || 480
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height)
  faceImageData.value = canvas.toDataURL('image/jpeg', 0.8)
  captured.value = true
  stopCamera()
}

function retakeFace() {
  captured.value = false; faceImageData.value = ''; faceError.value = ''
  startCamera()
}

function stopCamera() {
  if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null }
}

async function submitCheckin() {
  verifying.value = true; verifyError.value = ''
  try {
    const { data } = await axios.post('/api/checkin/verify', {
      faceImageData: faceImageData.value,
      latitude: latitude.value,
      longitude: longitude.value
    })
    if (data.code === 200 && data.data.checkedIn) {
      step.value = 4
      gsap.from('.success-card', { scale: 0.5, opacity: 0, duration: 0.6, ease: 'back.out(2)' })
    } else {
      verifyError.value = data.message || '签到验证失败'
    }
  } catch (e) { verifyError.value = e.response?.data?.message || '请求失败' }
  finally { verifying.value = false }
}

function retryAll() {
  step.value = 1; latitude.value = null; longitude.value = null
  captured.value = false; faceImageData.value = ''; gpsError.value = ''; faceError.value = ''; verifyError.value = ''
}

onUnmounted(() => stopCamera())
</script>

<style scoped>
.checkin-page { display: flex; flex-direction: column; gap: 1.5rem; }
.page-title { font-size: 1.3rem; font-weight: 700; }

.steps-bar { display: flex; align-items: center; justify-content: center; gap: 0; position: relative; padding: 0.5rem 0; }
.step-dot {
  width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; font-weight: 700; z-index: 2;
  background: var(--glass-bg); color: var(--text-secondary); border: 2px solid var(--glass-border);
  transition: all 0.3s;
}
.step-dot.active { background: var(--accent); color: #fff; border-color: var(--accent); }
.step-dot.done { background: var(--success); color: #fff; border-color: var(--success); }
.step-line {
  position: absolute; left: 25%; right: 25%; height: 2px; background: var(--glass-border); top: 50%; transform: translateY(-50%);
}
.step-fill { height: 100%; background: var(--accent); transition: width 0.5s; border-radius: 1px; }

.step-card { padding: 1.5rem; display: flex; flex-direction: column; align-items: center; gap: 1rem; text-align: center; }
.step-card h2 { font-size: 1.1rem; font-weight: 600; }
.step-desc { color: var(--text-secondary); font-size: 0.85rem; }

.video-wrapper { position: relative; width: 100%; max-width: 320px; border-radius: 1rem; overflow: hidden; }
.video-wrapper video { width: 100%; display: block; border-radius: 1rem; }
.face-guide {
  position: absolute; top: 15%; left: 20%; width: 60%; height: 70%;
  border: 2px dashed rgba(255,255,255,0.5); border-radius: 50%; pointer-events: none;
}
.captured-img { width: 100%; max-width: 320px; border-radius: 1rem; }

.step-actions { display: flex; gap: 0.75rem; }
.btn-secondary {
  background: var(--glass-bg); color: var(--text-primary); border: 1px solid var(--glass-border);
  border-radius: 0.75rem; padding: 0.75rem 1.5rem; cursor: pointer; min-height: 44px;
}

.success-card { text-align: center; }
.success-icon { font-size: 3rem; }
.error-msg { color: var(--error); font-size: 0.85rem; }
.success-msg { color: var(--success); font-size: 0.85rem; }
</style>
