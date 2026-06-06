<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="checkin-page">
    <header class="page-header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>签到打卡</h1>
    </header>
    <div class="content">
      <p>签到功能开发中...</p>
    </div>
  </div>
</template>

<style scoped>
.checkin-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.page-header h1 {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a1a);
  margin: 0;
}
.back-btn {
  padding: 6px 12px;
  border: 1px solid var(--border, #E5E5E3);
  border-radius: var(--radius-sm, 6px);
  background: var(--bg-surface, #fff);
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  font-size: 0.85rem;
}
.content {
  background: var(--bg-surface, #fff);
  border-radius: var(--radius-md, 10px);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.04));
  padding: 40px;
  text-align: center;
  color: var(--text-secondary, #6b7280);
}
</style>
