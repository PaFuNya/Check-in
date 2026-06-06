<script setup>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import gsap from 'gsap'

const profile = ref({})
const loading = ref(true)
const error = ref('')
const editing = ref(false)
const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref(false)

const form = reactive({ className: '', phoneNumber: '' })

async function fetchProfile() {
  loading.value = true; error.value = ''
  try {
    const { data } = await axios.get('/api/auth/profile')
    if (data.code === 200) profile.value = data.data
    else error.value = data.message
  } catch { error.value = '网络错误' } finally { loading.value = false }
}

function startEdit() {
  form.className = profile.value.className || ''
  form.phoneNumber = profile.value.phoneNumber || ''
  editing.value = true; saveError.value = ''; saveSuccess.value = false
}

function cancelEdit() { editing.value = false; saveError.value = '' }

async function saveProfile() {
  saving.value = true; saveError.value = ''; saveSuccess.value = false
  try {
    const { data } = await axios.put('/api/auth/profile', { className: form.className, phoneNumber: form.phoneNumber })
    if (data.code === 200) {
      profile.value.className = form.className; profile.value.phoneNumber = form.phoneNumber
      editing.value = false; saveSuccess.value = true
      setTimeout(() => { saveSuccess.value = false }, 2500)
    } else { saveError.value = data.message }
  } catch { saveError.value = '网络错误' } finally { saving.value = false }
}

onMounted(() => {
  fetchProfile()
  gsap.from('.profile-card', { opacity: 0, y: 20, duration: 0.5, ease: 'power3.out' })
})
</script>

<template>
  <div class="profile-page">
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
        <!-- Read-only fields -->
        <div class="field-group">
          <label class="field-label">学号</label>
          <div class="field-value">{{ profile.studentId }}</div>
        </div>
        <div class="field-group">
          <label class="field-label">姓名</label>
          <div class="field-value">{{ profile.studentName || '未设置' }}</div>
        </div>

        <!-- Editable: class -->
        <div class="field-group">
          <label class="field-label">班级</label>
          <div v-if="editing" class="field-edit">
            <input v-model="form.className" class="edit-input" placeholder="请输入班级" />
          </div>
          <div v-else class="field-value">{{ profile.className || '未设置' }}</div>
        </div>

        <!-- Editable: phone -->
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

.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; }
.card-icon { width: 44px; height: 44px; border-radius: 12px; background: #EFF6FF; color: #2563EB; display: flex; align-items: center; justify-content: center; }
:root.dark .card-icon { background: rgba(37, 99, 235, 0.12); }
.card-title { font-size: 1.25rem; font-weight: 700; color: #1E293B; margin: 0; }
:root.dark .card-title { color: #F1F5F9; }

/* States */
.state-box { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 32px 0; color: #64748B; }
.state-error { color: #DC2626; font-size: 0.875rem; }
.spinner { width: 24px; height: 24px; border: 3px solid #E2E8F0; border-top-color: #2563EB; border-radius: 50%; animation: spin 0.7s linear infinite; }
:root.dark .spinner { border-color: rgba(255, 255, 255, 0.1); border-top-color: #60A5FA; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Fields */
.profile-body { display: flex; flex-direction: column; gap: 4px; }
.field-group { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; border-bottom: 1px solid #F1F5F9; }
:root.dark .field-group { border-bottom-color: rgba(255, 255, 255, 0.04); }
.field-group:last-of-type { border-bottom: none; }
.field-label { font-size: 0.8125rem; color: #64748B; font-weight: 500; flex-shrink: 0; width: 60px; }
.field-value { font-size: 0.9375rem; color: #1E293B; font-weight: 500; text-align: right; }
:root.dark .field-value { color: #E2E8F0; }
.field-edit { flex: 1; text-align: right; }
.edit-input { width: 100%; max-width: 240px; padding: 8px 12px; border: 1.5px solid #E2E8F0; border-radius: 8px; font-size: 0.9375rem; background: #F8FAFC; color: #1E293B; outline: none; text-align: right; font-family: inherit; transition: border-color 0.2s; min-height: 40px; }
:root.dark .edit-input { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.1); color: #E2E8F0; }
.edit-input:focus { border-color: #2563EB; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }

/* Actions */
.action-row { display: flex; gap: 10px; margin-top: 24px; justify-content: center; }
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: linear-gradient(135deg, #2563EB, #3B82F6); color: white; border: none; border-radius: 10px; font-size: 0.875rem; font-weight: 600; min-height: 44px; font-family: inherit; transition: all 0.2s; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25); }
.btn-primary:hover { transform: translateY(-1px); }
.btn-success { display: inline-flex; align-items: center; gap: 6px; padding: 10px 24px; background: linear-gradient(135deg, #059669, #10B981); color: white; border: none; border-radius: 10px; font-size: 0.875rem; font-weight: 600; min-height: 44px; font-family: inherit; transition: all 0.2s; box-shadow: 0 4px 12px rgba(5, 150, 105, 0.25); }
.btn-success:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-success:hover:not(:disabled) { transform: translateY(-1px); }
.btn-ghost { padding: 10px 24px; background: #F1F5F9; color: #64748B; border: 1px solid #E2E8F0; border-radius: 10px; font-size: 0.875rem; font-weight: 500; min-height: 44px; font-family: inherit; transition: all 0.2s; }
:root.dark .btn-ghost { background: rgba(30, 41, 59, 0.5); border-color: rgba(255, 255, 255, 0.08); color: #94A3B8; }
.btn-ghost:hover { background: #E2E8F0; }
.btn-outline { padding: 8px 20px; background: transparent; color: #2563EB; border: 1px solid #2563EB; border-radius: 8px; font-size: 0.8125rem; cursor: pointer; min-height: 40px; font-family: inherit; transition: all 0.2s; }
.btn-outline:hover { background: rgba(37, 99, 235, 0.08); }

/* Messages */
.msg-error { display: flex; align-items: center; justify-content: center; gap: 6px; color: #DC2626; font-size: 0.8125rem; margin-top: 12px; }
.msg-success { display: flex; align-items: center; justify-content: center; gap: 6px; color: #059669; font-size: 0.8125rem; margin-top: 12px; }
</style>
