<template>
  <div class="profile-page">
    <div class="profile-card">
      <h2 class="card-title">个人信息</h2>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <button @click="fetchProfile" class="btn-retry">重试</button>
      </div>

      <div v-else class="profile-info">
        <div class="info-row">
          <span class="info-label">学号</span>
          <span class="info-value">{{ profile.studentId }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">姓名</span>
          <span class="info-value">{{ profile.studentName || '未设置' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">班级</span>
          <div class="info-edit">
            <input v-if="editing" v-model="form.className" class="edit-input" placeholder="请输入班级" />
            <span v-else class="info-value">{{ profile.className || '未设置' }}</span>
          </div>
        </div>
        <div class="info-row">
          <span class="info-label">手机号</span>
          <div class="info-edit">
            <input v-if="editing" v-model="form.phoneNumber" class="edit-input" placeholder="请输入手机号" />
            <span v-else class="info-value">{{ profile.phoneNumber || '未设置' }}</span>
          </div>
        </div>

        <div class="action-row">
          <button v-if="!editing" @click="startEdit" class="btn-edit">编辑</button>
          <template v-else>
            <button @click="saveProfile" class="btn-save" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
            <button @click="cancelEdit" class="btn-cancel">取消</button>
          </template>
        </div>

        <p v-if="saveError" class="save-error">{{ saveError }}</p>
        <p v-if="saveSuccess" class="save-success">保存成功！</p>
      </div>
    </div>
  </div>
</template>

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

const form = reactive({
  className: '',
  phoneNumber: ''
})

async function fetchProfile() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await axios.get('/api/auth/profile')
    if (data.code === 200) {
      profile.value = data.data
    } else {
      error.value = data.message
    }
  } catch {
    error.value = '网络错误'
  } finally {
    loading.value = false
  }
}

function startEdit() {
  form.className = profile.value.className || ''
  form.phoneNumber = profile.value.phoneNumber || ''
  editing.value = true
  saveError.value = ''
  saveSuccess.value = false
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

async function saveProfile() {
  saving.value = true
  saveError.value = ''
  saveSuccess.value = false
  try {
    const { data } = await axios.put('/api/auth/profile', {
      className: form.className,
      phoneNumber: form.phoneNumber
    })
    if (data.code === 200) {
      profile.value.className = form.className
      profile.value.phoneNumber = form.phoneNumber
      editing.value = false
      saveSuccess.value = true
      setTimeout(() => { saveSuccess.value = false }, 2000)
    } else {
      saveError.value = data.message
    }
  } catch {
    saveError.value = '网络错误'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchProfile()
  gsap.from('.profile-card', { opacity: 0, y: 20, duration: 0.5, ease: 'power2.out' })
})
</script>

<style scoped>
.profile-page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
  min-height: 100%;
}

.profile-card {
  width: 100%;
  max-width: 480px;
  padding: 2rem;
  border-radius: 1rem;
  backdrop-filter: blur(20px) saturate(180%);
  background: var(--glass-bg, rgba(255,255,255,0.72));
  border: var(--glass-border, 1px solid rgba(255,255,255,0.5));
  box-shadow: var(--glass-shadow, 0 8px 32px rgba(0,0,0,0.08));
}

.card-title {
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: var(--text-primary);
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem 0;
  color: var(--text-secondary);
}

.spinner {
  width: 24px; height: 24px;
  border: 3px solid var(--glass-border, rgba(0,0,0,0.1));
  border-top-color: var(--accent, #2563eb);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg) } }

.btn-retry {
  padding: 0.5rem 1.5rem;
  border-radius: 0.5rem;
  background: var(--accent, #2563eb);
  color: white;
  font-size: 0.875rem;
  cursor: pointer;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}

.info-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
  flex-shrink: 0;
  width: 60px;
}

.info-value {
  font-size: 0.875rem;
  color: var(--text-primary);
}

.edit-input {
  width: 100%;
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--glass-border, rgba(0,0,0,0.15));
  border-radius: 0.375rem;
  font-size: 0.875rem;
  background: var(--bg-surface, #fff);
  color: var(--text-primary);
  outline: none;
}

.info-edit { flex: 1; text-align: right; }

.action-row {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
  justify-content: center;
}

.btn-edit, .btn-save, .btn-cancel {
  padding: 0.5rem 2rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-edit { background: var(--accent, #2563eb); color: white; }
.btn-save { background: var(--success, #059669); color: white; }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-cancel { background: transparent; border: 1px solid var(--glass-border); color: var(--text-secondary); }

.save-error { color: var(--error, #dc2626); text-align: center; margin-top: 0.75rem; font-size: 0.875rem; }
.save-success { color: var(--success, #059669); text-align: center; margin-top: 0.75rem; font-size: 0.875rem; }
</style>
