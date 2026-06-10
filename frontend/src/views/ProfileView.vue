<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import gsap from 'gsap'

const router = useRouter()
const profile = ref({})
const loading = ref(true)
const error = ref('')
const editing = ref(false)
const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref(false)
const showFacePrompt = ref(false)

const form = reactive({ studentName: '', className: '', phoneNumber: '', dormBuilding: '', roomNumber: '' })

async function fetchProfile() {
  loading.value = true; error.value = ''
  try {
    const { data } = await axios.get('/api/auth/profile')
    if (data.code === 200) {
      profile.value = data.data
      // 人脸未注册提示
      if (!data.data.faceRegistered && !data.data.faceImageUrl) {
        showFacePrompt.value = true
      }
    }
    else error.value = data.message
  } catch { error.value = '网络错误' } finally { loading.value = false }
}

function startEdit() {
  form.studentName = profile.value.studentName || ''
  form.className = profile.value.className || ''
  form.phoneNumber = profile.value.phoneNumber || ''
  form.dormBuilding = profile.value.dormBuilding || ''
  form.roomNumber = profile.value.roomNumber || ''
  editing.value = true; saveError.value = ''; saveSuccess.value = false
}

function cancelEdit() { editing.value = false; saveError.value = '' }

async function saveProfile() {
  saving.value = true; saveError.value = ''; saveSuccess.value = false
  try {
    const { data } = await axios.put('/api/auth/profile', {
      studentName: form.studentName,
      className: form.className,
      phoneNumber: form.phoneNumber,
      dormBuilding: form.dormBuilding,
      roomNumber: form.roomNumber
    })
    if (data.code === 200) {
      Object.assign(profile.value, form)
      editing.value = false; saveSuccess.value = true
      setTimeout(() => { saveSuccess.value = false }, 2500)
    } else { saveError.value = data.message }
  } catch { saveError.value = '网络错误' } finally { saving.value = false }
}

function goFaceRegister() {
  showFacePrompt.value = false
  router.push('/face-register')
}

onMounted(() => {
  fetchProfile()
  gsap.from('.profile-card', { opacity: 0, y: 20, duration: 0.5, ease: 'power3.out' })
})
</script>

<template>
  <div class="profile-page">
    <!-- 人脸注册提示弹窗 -->
    <div v-if="showFacePrompt" class="face-prompt-overlay" @click.self="showFacePrompt = false">
      <div class="face-prompt-card glass-card">
        <div class="face-prompt-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#2563EB" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
          </svg>
        </div>
        <h3 class="face-prompt-title">请先注册人脸</h3>
        <p class="face-prompt-desc">您尚未注册人脸数据，注册后才能使用人脸签到功能。</p>
        <div class="face-prompt-actions">
          <button class="btn-primary cursor-pointer" @click="goFaceRegister">去注册</button>
          <button class="btn-ghost cursor-pointer" @click="showFacePrompt = false">稍后再说</button>
        </div>
      </div>
    </div>

    <div class="profile-card glass-card">
      <!-- Header -->
      <div class="card-header">
        <div class="card-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <h2 class="card-title">个人信息</h2>
      </div>

      <!-- Avatar -->
      <div class="avatar-area">
        <div class="avatar-circle">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <span class="avatar-name">{{ profile.studentName || profile.studentId || '--' }}</span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="state-box">
        <div class="spinner"></div><span>加载中...</span>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="state-box">
        <span class="state-error">{{ error }}</span>
        <button class="btn-outline cursor-pointer" @click="fetchProfile">重试</button>
      </div>

      <!-- Profile -->
      <div v-else class="profile-body">
        <!-- 学号（只读） -->
        <div class="field-group">
          <label class="field-label">学号</label>
          <div class="field-value">{{ profile.studentId }}</div>
        </div>

        <!-- 姓名（可编辑） -->
        <div class="field-group">
          <label class="field-label">姓名</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.studentName" class="edit-input" placeholder="请输入姓名" />
          </div>
          <div v-else class="field-value">{{ profile.studentName || '未设置' }}</div>
        </div>

        <!-- 宿舍楼栋（可编辑） -->
        <div class="field-group">
          <label class="field-label">宿舍楼栋</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.dormBuilding" class="edit-input" placeholder="如：1号楼" />
          </div>
          <div v-else class="field-value">{{ profile.dormBuilding || '未设置' }}</div>
        </div>

        <!-- 寝室号（可编辑） -->
        <div class="field-group">
          <label class="field-label">寝室号</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.roomNumber" class="edit-input" placeholder="如：301" />
          </div>
          <div v-else class="field-value">{{ profile.roomNumber || '未设置' }}</div>
        </div>

        <!-- 人脸注册状态 -->
        <div class="field-group">
          <label class="field-label">人脸注册</label>
          <div class="field-value">
            <span v-if="profile.faceRegistered || profile.faceImageUrl === 'registered'" class="face-badge face-done">已注册</span>
            <span v-else class="face-badge face-pending cursor-pointer" @click="router.push('/face-register')">未注册</span>
          </div>
        </div>

        <!-- 班级（可编辑） -->
        <div class="field-group">
          <label class="field-label">班级</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.className" class="edit-input" placeholder="请输入班级" />
          </div>
          <div v-else class="field-value">{{ profile.className || '未设置' }}</div>
        </div>

        <!-- 手机号（可编辑） -->
        <div class="field-group">
          <label class="field-label">手机号</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.phoneNumber" class="edit-input" placeholder="请输入手机号" />
          </div>
          <div v-else class="field-value">{{ profile.phoneNumber || '未设置' }}</div>
        </div>

        <!-- Actions -->
        <div class="action-row">
          <button v-if="!editing" class="btn-primary cursor-pointer" @click="startEdit">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            编辑
          </button>
          <template v-else>
            <button class="btn-success cursor-pointer" :disabled="saving" @click="saveProfile">
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button class="btn-ghost cursor-pointer" @click="cancelEdit">取消</button>
          </template>
        </div>

        <!-- Messages -->
        <p v-if="saveError" class="msg-error">{{ saveError }}</p>
        <p v-if="saveSuccess" class="msg-success">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          保存成功
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { display: flex; justify-content: center; padding: 0; }
.profile-card { width: 100%; max-width: 480px; padding: 32px 28px; }

.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.card-icon { width: 44px; height: 44px; border-radius: 12px; background: var(--color-primary-light); color: var(--color-primary); display: flex; align-items: center; justify-content: center; }
.card-title { font-size: 1.25rem; font-weight: 700; color: var(--color-text); margin: 0; }

/* Avatar */
.avatar-area { display: flex; flex-direction: column; align-items: center; gap: 12px; margin-bottom: 28px; }
.avatar-circle { width: 80px; height: 80px; border-radius: 50%; background: var(--color-primary-light); color: var(--color-primary); display: flex; align-items: center; justify-content: center; }
.avatar-name { font-size: 1rem; font-weight: 700; color: var(--color-text); }

/* States */
.state-box { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 32px 0; color: var(--color-text-muted); }
.state-error { color: var(--color-error); font-size: 0.875rem; }
.spinner { width: 24px; height: 24px; border: 3px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Fields */
.profile-body { display: flex; flex-direction: column; }
.field-group { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #F1F5F9; }
.dark .field-group { border-bottom-color: rgba(255, 255, 255, 0.04); }
.field-group:last-of-type { border-bottom: none; }
.field-label { font-size: 0.8125rem; color: var(--color-text-muted); font-weight: 500; flex-shrink: 0; width: 70px; }
.field-value { font-size: 0.9375rem; color: var(--color-text); font-weight: 500; text-align: right; }
.field-edit { flex: 1; text-align: right; }
.edit-input { width: 100%; max-width: 240px; padding: 8px 12px; border: 1.5px solid var(--color-border); border-radius: 8px; font-size: 0.9375rem; background: #F8FAFC; color: var(--color-text); outline: none; text-align: right; font-family: inherit; transition: border-color 0.2s; min-height: 40px; }
.dark .edit-input { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.1); }
.edit-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }

/* Face badge */
.face-badge { display: inline-flex; align-items: center; padding: 3px 12px; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }
.face-done { background: var(--color-success-light); color: var(--color-success); }
.face-pending { background: var(--color-error-light); color: var(--color-error); }

/* Actions */
.action-row { display: flex; gap: 10px; margin-top: 24px; justify-content: center; }
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border: none; border-radius: 10px; font-size: 0.875rem; font-weight: 600; min-height: 44px; font-family: inherit; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25); }
.btn-primary:hover { transform: translateY(-1px); }
.btn-success { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: linear-gradient(135deg, #059669, #10B981); color: white; border: none; border-radius: 10px; font-size: 0.875rem; font-weight: 600; min-height: 44px; font-family: inherit; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(5, 150, 105, 0.25); }
.btn-success:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-success:hover:not(:disabled) { transform: translateY(-1px); }
.btn-ghost { padding: 10px 24px; background: #F1F5F9; color: #64748B; border: 1px solid #E2E8F0; border-radius: 10px; font-size: 0.875rem; font-weight: 500; min-height: 44px; font-family: inherit; cursor: pointer; transition: all 0.2s; }
.dark .btn-ghost { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: #94A3B8; }
.btn-ghost:hover { background: #E2E8F0; }
.btn-outline { padding: 8px 20px; background: transparent; color: #2563EB; border: 1px solid #2563EB; border-radius: 8px; font-size: 0.8125rem; cursor: pointer; min-height: 40px; font-family: inherit; transition: all 0.2s; }

/* Messages */
.msg-error { display: flex; align-items: center; justify-content: center; gap: 6px; color: var(--color-error); font-size: 0.8125rem; margin-top: 12px; }
.msg-success { display: flex; align-items: center; justify-content: center; gap: 6px; color: var(--color-success); font-size: 0.8125rem; margin-top: 12px; }

/* Face prompt overlay */
.face-prompt-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 24px; }
.face-prompt-card { max-width: 360px; width: 100%; padding: 36px 28px; text-align: center; }
.face-prompt-icon { margin-bottom: 16px; }
.face-prompt-title { font-size: 1.25rem; font-weight: 700; color: var(--color-text); margin: 0 0 8px; }
.face-prompt-desc { font-size: 0.875rem; color: var(--color-text-muted); margin: 0 0 24px; line-height: 1.6; }
.face-prompt-actions { display: flex; flex-direction: column; gap: 10px; }
</style>
