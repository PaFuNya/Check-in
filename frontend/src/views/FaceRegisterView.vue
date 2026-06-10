<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const authStore = useAuthStore()

const videoRef = ref(null)
const canvasRef = ref(null)
const stream = ref(null)
const step = ref('camera') // camera | preview | uploading | success | error
const capturedImage = ref(null)
const errorMsg = ref('')
const facingMode = 'user'

async function startCamera() {
  try {
    stream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode, width: { ideal: 640 }, height: { ideal: 480 } },
      audio: false
    })
    if (videoRef.value) {
      videoRef.value.srcObject = stream.value
    }
    step.value = 'camera'
  } catch (e) {
    errorMsg.value = '无法访问摄像头，请检查浏览器权限设置'
    step.value = 'error'
  }
}

function capture() {
  const video = videoRef.value
  const canvas = canvasRef.value
  if (!video || !canvas) return

  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0)
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
  step.value = 'preview'
  stopCamera()
}

function retake() {
  capturedImage.value = null
  startCamera()
}

function stopCamera() {
  if (stream.value) {
    stream.value.getTracks().forEach(t => t.stop())
    stream.value = null
  }
}

async function submitFace() {
  if (!capturedImage.value) return
  step.value = 'uploading'
  errorMsg.value = ''

  try {
    const { data } = await axios.post('/api/face/register', {
      imageData: capturedImage.value
    })
    if (data.code === 200 && data.data?.registered) {
      step.value = 'success'
      gsap.from('.success-card', { scale: 0.8, opacity: 0, duration: 0.5, ease: 'back.out(1.7)' })
      // 2秒后跳转首页
      setTimeout(() => { router.push('/') }, 2000)
    } else {
      errorMsg.value = data.message || '注册失败，请确保照片中有清晰人脸'
      step.value = 'error'
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '网络错误，请重试'
    step.value = 'error'
  }
}

onMounted(() => {
  startCamera()
  gsap.from('.register-card', { y: 40, opacity: 0, duration: 0.6, ease: 'power3.out' })
})

onUnmounted(() => { stopCamera() })
</script>

<template>
  <div class="register-page">
    <div class="register-card glass-card">
      <!-- Header -->
      <div class="card-header">
        <button class="back-btn cursor-pointer" @click="stopCamera(); router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <h2 class="card-title">人脸注册</h2>
      </div>

      <!-- Step 1: Camera -->
      <div v-if="step === 'camera'" class="camera-area">
        <div class="camera-frame">
          <video ref="videoRef" autoplay playsinline muted class="camera-video"></video>
          <div class="face-guide">
            <svg viewBox="0 0 200 200" class="guide-oval">
              <ellipse cx="100" cy="100" rx="70" ry="90" fill="none" stroke="rgba(37,99,235,0.6)" stroke-width="2" stroke-dasharray="8 4"/>
            </svg>
          </div>
          <p class="camera-hint">请将面部对准框内</p>
        </div>
        <button class="capture-btn cursor-pointer" @click="capture">
          <div class="capture-ring">
            <div class="capture-dot"></div>
          </div>
        </button>
      </div>

      <!-- Step 2: Preview -->
      <div v-else-if="step === 'preview'" class="preview-area">
        <img :src="capturedImage" class="preview-image" alt="captured face" />
        <div class="preview-actions">
          <button class="btn-primary cursor-pointer" @click="submitFace">确认注册</button>
          <button class="btn-ghost cursor-pointer" @click="retake">重新拍照</button>
        </div>
      </div>

      <!-- Step 3: Uploading -->
      <div v-else-if="step === 'uploading'" class="state-box">
        <div class="spinner"></div>
        <p>正在注册人脸数据...</p>
      </div>

      <!-- Step 4: Success -->
      <div v-else-if="step === 'success'" class="state-box success-card">
        <div class="success-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <h3 class="success-title">注册成功！</h3>
        <p class="success-desc">人脸数据已录入，现在可以使用人脸签到了。</p>
      </div>

      <!-- Step 5: Error -->
      <div v-else-if="step === 'error'" class="state-box">
        <div class="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <p class="error-text">{{ errorMsg }}</p>
        <button class="btn-primary cursor-pointer" @click="retake">重试</button>
      </div>

      <!-- Hidden canvas -->
      <canvas ref="canvasRef" style="display:none"></canvas>
    </div>
  </div>
</template>

<style scoped>
.register-page { display: flex; justify-content: center; padding: 0; }
.register-card { width: 100%; max-width: 420px; padding: 24px 20px; }

.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.back-btn { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border-radius: 10px; border: none; background: rgba(241,245,249,0.8); color: #64748B; transition: all 0.2s; }
.back-btn:hover { background: #E2E8F0; }
:root.dark .back-btn { background: rgba(30,41,59,0.6); color: #94A3B8; }
.card-title { font-size: 1.125rem; font-weight: 700; color: var(--color-text); margin: 0; }

/* Camera */
.camera-area { display: flex; flex-direction: column; align-items: center; gap: 24px; }
.camera-frame { position: relative; width: 100%; aspect-ratio: 3/4; border-radius: 16px; overflow: hidden; background: #0F172A; }
.camera-video { width: 100%; height: 100%; object-fit: cover; }
.face-guide { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; pointer-events: none; }
.guide-oval { width: 60%; height: 70%; }
.camera-hint { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); color: rgba(255,255,255,0.8); font-size: 0.8125rem; background: rgba(0,0,0,0.4); padding: 6px 16px; border-radius: 20px; }

.capture-btn { width: 72px; height: 72px; border-radius: 50%; border: none; background: transparent; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.capture-ring { width: 68px; height: 68px; border-radius: 50%; border: 4px solid #2563EB; display: flex; align-items: center; justify-content: center; transition: transform 0.15s; }
.capture-btn:hover .capture-ring { transform: scale(1.05); }
.capture-dot { width: 52px; height: 52px; border-radius: 50%; background: linear-gradient(135deg, #2563EB, #3B82F6); }

/* Preview */
.preview-area { display: flex; flex-direction: column; align-items: center; gap: 20px; }
.preview-image { width: 100%; max-width: 320px; border-radius: 16px; }
.preview-actions { display: flex; gap: 12px; width: 100%; }
.preview-actions .btn-primary, .preview-actions .btn-ghost { flex: 1; }

/* States */
.state-box { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 48px 0; text-align: center; color: var(--color-text-muted); }
.spinner { width: 32px; height: 32px; border: 3px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.success-icon { margin-bottom: 8px; }
.success-title { font-size: 1.25rem; font-weight: 700; color: #059669; margin: 0; }
.success-desc { font-size: 0.875rem; color: var(--color-text-muted); margin: 0; }
.error-icon { margin-bottom: 8px; }
.error-text { font-size: 0.875rem; color: #DC2626; }

/* Buttons */
.btn-primary { display: inline-flex; align-items: center; justify-content: center; gap: 6px; padding: 12px 24px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border: none; border-radius: 12px; font-size: 0.9375rem; font-weight: 600; min-height: 48px; font-family: inherit; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37,99,235,0.25); }
.btn-primary:hover { transform: translateY(-1px); }
.btn-ghost { padding: 12px 24px; background: #F1F5F9; color: #64748B; border: 1px solid #E2E8F0; border-radius: 12px; font-size: 0.9375rem; font-weight: 500; min-height: 48px; font-family: inherit; cursor: pointer; transition: all 0.2s; }
.dark .btn-ghost { background: rgba(30,41,59,0.5); border-color: rgba(255,255,255,0.08); color: #94A3B8; }
.btn-ghost:hover { background: #E2E8F0; }
</style>
