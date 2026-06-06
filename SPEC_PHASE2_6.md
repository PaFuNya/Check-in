# SPEC: 校园寝室签到系统 Phase 2–6 规格文档

> 生成时间: 2026-06-07  
> 基于 `/root/bighomework` 现有代码逆向生成

---

## 目录

1. [Phase 2: Redis 集成规格](#phase-2-redis-集成规格)
2. [Phase 3: Vue 前端项目架构规格](#phase-3-vue-前端项目架构规格)
3. [Phase 4: 各页面功能规格](#phase-4-各页面功能规格)
4. [Phase 5: 部署规格](#phase-5-部署规格)
5. [Phase 6: 后端 API 接口完整规格](#phase-6-后端-api-接口完整规格)
6. [附录: 文件清单与依赖关系](#附录-文件清单与依赖关系)

---

## Phase 2: Redis 集成规格

### 2.1 容器配置

| 项目 | 规格 |
|------|------|
| Docker 容器名 | `1Panel-redis-checkin` |
| 宿主端口 | 默认 6379 |
| 网络模式 | WSL Docker 本地桥接 |

### 2.2 Maven 依赖 (pom.xml)

```xml
<!-- Redis 核心 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Session 存入 Redis -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

### 2.3 application.properties 配置项

```properties
# Redis 连接
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379

# Session 存储方式 = Redis
spring.session.store-type=redis
# Session 超时: 30 分钟
server.servlet.session.timeout=1800s
```

### 2.4 签到防重逻辑 (ApiCheckInController)

| 项目 | 规格 |
|------|------|
| 注入 | `@Autowired StringRedisTemplate redisTemplate` |
| Key 格式 | `checkin:today:{studentId}:{yyyyMMdd}` |
| Key 示例 | `checkin:today:2023001:20260607` |
| 检查时机 | `POST /api/checkin/verify` 方法入口处 |
| 检查命令 | `redisTemplate.hasKey(todayKey)` → 已存在则返回 `400 今日已签到，请勿重复签到` |
| 写入时机 | 签到记录保存到 MySQL **之后** |
| 写入命令 | `redisTemplate.opsForValue().set(todayKey, "1", Duration.ofHours(23))` |
| TTL | **23 小时**（非 24h，确保次日可正常签到） |

**防重流程图:**

```
POST /api/checkin/verify
  │
  ├─ Redis hasKey("checkin:today:{id}:{date}")?
  │    └─ YES → 400 "今日已签到，请勿重复签到"
  │
  ├─ 人脸验证 (faceRecognitionService.verifyFace)
  ├─ GPS 验证 (locationVerificationService.verifyLocation)
  │
  ├─ 两者都通过?
  │    ├─ YES → 保存 CheckInRecord → Redis SET key, TTL=23h → 200 OK
  │    └─ NO  → 400 错误信息
```

---

## Phase 3: Vue 前端项目架构规格

### 3.1 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API, `<script setup>`) | ^3.5.34 |
| 构建工具 | Vite | ^8.0.12 |
| CSS 工具 | Tailwind CSS v4 (Vite 插件模式) | ^4.3.0 |
| 路由 | Vue Router 4 (HTML5 History) | ^4.6.4 |
| 状态管理 | Pinia | ^3.0.4 |
| HTTP 客户端 | Axios | ^1.17.0 |
| 动画库 | GSAP | ^3.15.0 |
| Vite 插件 | `@vitejs/plugin-vue` ^6.0.6, `@tailwindcss/vite` ^4.3.0 |

### 3.2 Vite 配置 (`frontend/vite.config.js`)

```js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
```

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 路径别名 | `@` → `./src` | 允许 `import X from '@/stores/auth'` |
| Tailwind | Vite 插件模式 | 非 PostCSS 模式，`main.css` 中 `@import "tailwindcss"` 即可 |
| 开发端口 | 默认 5173 | 未显式配置，使用 Vite 默认 |

### 3.3 入口文件 (`frontend/src/main.js`)

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './assets/main.css'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())  // 状态管理
app.use(router)         // 路由
app.mount('#app')
```

**插件加载顺序:** Pinia → Router（Router 的 `beforeEach` 中需调用 Pinia store）

### 3.4 App.vue

```vue
<script setup>
import { useAuthStore } from '@/stores/auth'
</script>
<template>
  <router-view />
</template>
```

仅作为根挂载点，不做任何布局。

### 3.5 路由规格 (`frontend/src/router/index.js`)

| 路由路径 | name | 组件 | meta | 说明 |
|----------|------|------|------|------|
| `/login` | `Login` | `@/views/Login.vue` | `{ requiresGuest: true }` | 登录页，已登录自动跳转首页 |
| `/` | (布局容器) | `@/layouts/DefaultLayout.vue` | `{ requiresAuth: true }` | 需要登录 |
| ↳ `` | `Home` | `@/views/HomeView.vue` | — | 首页 |
| ↳ `checkin` | `CheckIn` | `@/views/CheckInView.vue` | — | 签到页 |
| ↳ `records` | `Records` | `@/views/RecordsView.vue` | — | 签到记录页 |
| ↳ `chat` | `Chat` | `@/views/ChatView.vue` | — | AI 对话页 |
| `/:pathMatch(.*)*` | — | — | redirect `/` | 兜底路由 |

**路由守卫逻辑 (`beforeEach`):**

1. 首次加载时，若 `authStore.authChecked === false`，调用 `await authStore.checkAuth()`
2. 若目标路由 `requiresAuth === true` 且 `isLoggedIn === false` → 重定向 `/login`
3. 若目标路由 `requiresGuest === true` 且 `isLoggedIn === true` → 重定向 `/`

**加载模式:** 所有组件均使用 `() => import(...)` 懒加载。

### 3.6 状态管理 (`frontend/src/stores/auth.js`)

**Store name:** `auth`

#### State

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `user` | `Object \| null` | `null` | 用户信息 |
| `isLoggedIn` | `boolean` | `false` | 是否已登录 |
| `authChecked` | `boolean` | `false` | 是否已完成首次鉴权检查 |
| `loading` | `boolean` | `false` | 请求进行中 |
| `error` | `string \| null` | `null` | 错误信息 |

#### Getters

| getter | 返回值 | 说明 |
|--------|--------|------|
| `studentId` | `user.studentId \|\| null` | 学号 |
| `studentName` | `user.studentName \|\| null` | 姓名 |
| `displayName` | `user.studentName \|\| user.studentId \|\| '未登录'` | 显示名称 |

#### Actions

| action | 参数 | API 调用 | 说明 |
|--------|------|----------|------|
| `login(studentId, password, rememberMe)` | 3 个 string/bool | `POST /api/auth/login` | 登录，成功设 `user + isLoggedIn` |
| `logout()` | 无 | `POST /api/auth/logout` | 登出，清空本地状态（忽略 API 错误） |
| `checkAuth()` | 无 | `GET /api/auth/check` | 首次加载检查 session，始终设 `authChecked=true` |
| `fetchProfile()` | 无 | `GET /api/auth/profile` | 获取完整个人资料 |
| `clearError()` | 无 | — | 清空 error |

### 3.7 CSS 架构 (`frontend/src/assets/main.css`)

```css
@import "tailwindcss";
```

仅引入 Tailwind CSS v4 基础层。组件样式使用 `<style scoped>` 中的原生 CSS + Tailwind class 混合方案。

### 3.8 CORS 配置 (后端 `CorsConfig.java`)

| 配置项 | 值 |
|--------|-----|
| 匹配路径 | `/api/**` |
| 允许来源 | `http://localhost:5173`, `https://check.kaminya.top` |
| 允许方法 | GET, POST, PUT, DELETE, OPTIONS |
| 允许头 | `*` |
| 允许凭证 | `true` |
| 预检缓存 | 3600s |

### 3.9 统一响应格式 (`ApiResponse.java`)

```java
@Data
public class ApiResponse<T> {
    private int code;       // 200=成功, 400/401/404/500=错误
    private String message; // "success" 或错误描述
    private T data;         // 响应数据，可为 null
}
```

**静态工厂方法:**

| 方法 | 签名 | 说明 |
|------|------|------|
| `ok(data)` | `<T> ApiResponse<T> ok(T data)` | code=200, message="success" |
| `ok()` | `<T> ApiResponse<T> ok()` | code=200, data=null |
| `error(code, message)` | `<T> ApiResponse<T> error(int code, String message)` | 无 data |

---

## Phase 4: 各页面功能规格

### 4.1 Login.vue（玻璃拟态登录页）

**路径:** `/login`  
**路由:** `name: 'Login'`, `meta: { requiresGuest: true }`

#### 数据绑定

| ref | 类型 | 说明 |
|-----|------|------|
| `studentId` | `string` | 学号输入 |
| `password` | `string` | 密码输入 |
| `cardRef` | `ref(null)` | GSAP 动画目标 (卡片 DOM) |
| `formRef` | `ref(null)` | GSAP 动画目标 (表单 DOM) |

#### 交互流程

1. 用户输入学号+密码 → 点击"登 录"按钮
2. 校验: 若任一为空 → 设置 `authStore.error`
3. 调用 `authStore.login(studentId, password, false)` → 成功则 `router.push({ name: 'Home' })`
4. 按钮显示 loading spinner（CSS animation）

#### GSAP 动画

| 时序 | 目标 | 属性 | duration |
|------|------|------|----------|
| tl[0] | `.login-card` | `y:60→0, opacity:0→1` | 0.8s |
| tl[1] | `.form-group, .error-message, button[type="submit"]` | `y:30→0, opacity:0→1, stagger:0.1` | 0.5s, overlap 0.3s |

#### CSS 特性

- 深色渐变背景: `linear-gradient(135deg, #0f0c29, #302b63, #24243e)`
- 3 个浮动光晕 (`.glow-1/2/3`): `filter: blur(80px)` + CSS `@keyframes float` 8s 循环
- 卡片: Glassmorphism `backdrop-filter: blur(20px)`, `border-radius: 20px`
- 输入框: 半透明背景, 图标前缀（👤/🔒）, focus 蓝色边框+发光
- 按钮: 渐变背景 `#667eea→#764ba2`, hover 上浮 2px + 阴影增强

### 4.2 DefaultLayout.vue（布局框架）

**路径:** `/` (父路由)  
**包裹所有需登录的子路由**

#### 功能模块

| 模块 | 实现 | 说明 |
|------|------|------|
| 顶部导航栏 | `.navbar-glass` | 固定定位 `fixed top-0`, Glassmorphism 效果 |
| 导航链接 | 4 个 `router-link` | 首页/签到/记录/AI助手, 带 emoji 图标 |
| 实时时钟 | `setInterval(updateClock, 1000)` | `zh-CN` 格式 HH:mm:ss |
| 暗色主题 | `isDark` toggle | `document.documentElement.classList.toggle('dark')`, localStorage 持久化 |
| 用户名显示 | `authStore.displayName` | 导航栏右侧 |
| 退出按钮 | `authStore.logout()` + `router.push('/login')` | 导航栏右侧 |
| 移动端导航 | `md:hidden` 响应式 | 仅显示 emoji 图标 |
| 内容区 | `<router-view />` | `pt-20` 为 fixed 导航栏留空 |

#### 导航项定义

```js
const navItems = [
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/checkin', label: '签到', icon: '✅' },
  { path: '/records', label: '记录', icon: '📋' },
  { path: '/chat', label: 'AI助手', icon: '🤖' },
]
```

#### CSS 变量 (暗色主题)

| 场景 | 亮色 | 暗色 |
|------|------|------|
| 布局背景 | `linear-gradient(135deg, #e0e7ff, #f0e6ff, #dbeafe)` | `linear-gradient(135deg, #0f172a, #1e1b4b, #0f172a)` |
| 导航栏背景 | `rgba(255,255,255,0.6)` | `rgba(15,23,42,0.7)` |
| 导航链接颜色 | `#475569` | `#cbd5e1` |
| 激活链接 | indigo 系 | indigo 浅色系 |

### 4.3 HomeView.vue（首页仪表板）

**路径:** `/`  
**路由:** `name: 'Home'`

#### 数据与 API

| 数据 | API | 说明 |
|------|-----|------|
| `checkedIn` (ref) | `GET /api/checkin/status` → `data.checkedIn` | 今日是否已签到 |
| `checkInCount` (ref) | 同上 → `data.count` | 今日签到次数 |
| `statusLoading` (ref) | — | 加载状态 |
| `greeting` (computed) | — | 根据小时: 夜深了/早上好/上午好/中午好/下午好/晚上好 |
| `today` (computed) | — | `YYYY年M月D日 星期X` |

#### UI 区块

1. **问候区**: `greeting + displayName`, 日期
2. **签到状态卡**: 状态徽章(已签到绿/未签到红/加载灰), 次数, 提示文案
3. **圆形签到按钮**: 120px 圆形, GSAP 呼吸动画 (scale 1.0↔1.08, boxShadow 脉动, 2.8s 周期, `repeat:-1, yoyo:true`)
4. **快捷操作网格**: 2×2 grid, 4 张卡片(签到打卡/签到记录/AI助手/个人信息), hover 上浮+边框高亮

#### GSAP 动画

```js
breatheTl = gsap.timeline({ repeat: -1, yoyo: true })
  .to(btnRef, { scale: 1.08, boxShadow: '0 0 28px rgba(37,99,235,0.35)', duration: 1.4, ease: 'sine.inOut' })
  .to(btnRef, { scale: 1.0,  boxShadow: '0 0 8px rgba(37,99,235,0.1)',   duration: 1.4, ease: 'sine.inOut' })
```

组件卸载时 `breatheTl.kill()` 清理。

### 4.4 CheckInView.vue（签到页）— 占位

**路径:** `/checkin`  
**路由:** `name: 'CheckIn'`

**当前状态:** 占位页面，显示"签到功能开发中..."

**布局:** 返回按钮 + 标题 + 空内容卡片

> ⚠️ **待实现:** 需集成人脸摄像头采集 + GPS 定位 + 调用 `POST /api/checkin/verify`

### 4.5 RecordsView.vue（签到记录页）— 占位

**路径:** `/records`  
**路由:** `name: 'Records'`

**当前状态:** 占位页面，显示"签到记录功能开发中..."

> ⚠️ **待实现:** 需调用 `GET /api/checkin/records?page=0&size=10` 分页展示

### 4.6 ChatView.vue（AI 助手页）— 占位

**路径:** `/chat`  
**路由:** `name: 'Chat'`

**当前状态:** 占位页面，显示"AI 聊天功能开发中..."

> ⚠️ **待实现:** 需集成 `EventSource` 消费 `GET /api/ai/chat-stream?message=xxx` SSE 流

### 4.7 LoginView.vue（简洁登录页 — 备用/历史）

**路径:** 未被路由引用（路由指向 `Login.vue`）  
**说明:** 保留的简洁风格登录页，无 GSAP 动画，有"记住我"复选框。当前未使用。

---

## Phase 5: 部署规格

### 5.1 deploy.sh 脚本

```bash
#!/bin/bash
set -e

echo "[1/5] Checking Redis..."
docker ps | grep 1Panel-redis-checkin || docker start 1Panel-redis-checkin

echo "[2/5] Building frontend..."
cd frontend && npm run build && cd ..

echo "[3/5] Building backend..."
mvn clean package -DskipTests -q

echo "[4/5] Restarting backend..."
pkill -f campus-checkin || true; sleep 2
nohup java -jar target/campus-checkin-assistant-1.0-SNAPSHOT.jar \
  --server.port=8088 > /tmp/check.log 2>&1 &

echo "[5/5] Done! PID: $!"
```

### 5.2 构建流程

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1. Redis | `docker start 1Panel-redis-checkin` | 确保 Redis 容器运行 |
| 2. 前端构建 | `cd frontend && npm run build` | Vite 生产构建 → `frontend/dist/` |
| 3. 后端构建 | `mvn clean package -DskipTests -q` | 打包为 JAR |
| 4. 重启后端 | `pkill -f campus-checkin && java -jar target/...jar` | 停旧启新, 端口 8088 |

### 5.3 环境变量

| 变量 | 用途 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | AI 模型 API Key | `changeme` |
| `SPRING_DATASOURCE_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/checkin_db?...` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 用户名 | `changeme` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 | `changeme` |
| `JWT_SECRET` | JWT 签名密钥 (Phase 3 预留) | `changeme-please-set-in-env-or-application-local` |

### 5.4 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Spring Boot 后端 | 8088 | `--server.port=8088` |
| Vite 开发服务器 | 5173 | 默认 |
| Redis | 6379 | Docker 容器 |
| MySQL | 3306 | Docker 容器 `1Panel-mysql-RIP6` |

### 5.5 生产域名

| 用途 | URL |
|------|-----|
| 前端 (HTTPS) | `https://check.kaminya.top` |
| 后端 API | `https://check.kaminya.top:8088` |

---

## Phase 6: 后端 API 接口完整规格

### 6.1 认证模块 (`/api/auth`)

**Controller:** `ApiAuthController`  
**注入依赖:** `AuthService`

#### `POST /api/auth/login`

| 项 | 规格 |
|----|------|
| Content-Type | `application/json` |
| 请求体 | `{ "studentId": "string", "password": "string", "rememberMe": false }` |
| 校验 | studentId/password 不能为空 → 400 |
| 业务逻辑 | 调用 `authService.login(studentId, password, rememberMe, session, response)` |
| 成功响应 | `200 { code:200, data: { studentId, studentName, className, avatarUrl } }` |
| 失败响应 | `401 { code:401, message: "学号或密码错误" }` |
| Session 写入 | `studentId`, `studentName`, `className` |

#### `POST /api/auth/logout`

| 项 | 规格 |
|----|------|
| 请求体 | 无 |
| 逻辑 | 调用 `authService.logout(session, response)` |
| 响应 | `200 { code:200 }` |

#### `GET /api/auth/check`

| 项 | 规格 |
|----|------|
| 用途 | 前端首次加载时检查 session 是否有效 |
| 响应 | `200 { code:200, data: { loggedIn: bool, studentId, studentName, className, phoneNumber, avatarUrl } }` |
| 特点 | 未登录时返回 `loggedIn: false`，**不返回 401** |

#### `GET /api/auth/profile`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 (session studentId) |
| 响应 | `200 { code:200, data: { ...完整学生信息 } }` |

#### `PUT /api/auth/profile`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 请求体 | `{ "className": "string", "phoneNumber": "string", "avatarUrl": "string" }` |
| 逻辑 | 调用 `authService.updateProfile()`，同时更新 session 属性 |
| 响应 | 成功 `200 { code:200 }`，失败 `500` |

### 6.2 签到模块 (`/api/checkin`)

**Controller:** `ApiCheckInController`  
**注入依赖:** `FaceRecognitionService`, `LocationVerificationService`, `CheckInRecordRepository`, `StudentRepository`, `StringRedisTemplate`

#### `GET /api/checkin/status`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 逻辑 | 查询今日零点之后的签到记录 |
| 响应 | `200 { code:200, data: { checkedIn: bool, count: int } }` |

#### `POST /api/checkin/verify`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 请求体 | `{ "faceImageData": "base64 string", "latitude": double, "longitude": double }` |
| 校验 | faceImageData 不能为空, latitude/longitude 不能为空 |
| Redis 防重 | Key `checkin:today:{id}:{date}`, 存在则 400 |
| 人脸验证 | `faceRecognitionService.verifyFace(studentId, faceImageData)` |
| GPS 验证 | `locationVerificationService.verifyLocation(studentId, latitude, longitude)` |
| 成功响应 | `200 { code:200, data: { faceVerified: true, locationVerified: true, checkedIn: true } }` |
| 失败响应 | `400 { code:400, message: "签到失败：人脸识别未通过; GPS不在范围内" }` |
| 副作用 | 写入 `check_in_record` 表 + Redis key TTL=23h |

#### `GET /api/checkin/records`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 参数 | `page` (默认 0), `size` (默认 10) |
| 排序 | `checkTime DESC` |
| 响应 | `200 { code:200, data: { content: [...], totalPages, totalElements, currentPage, size } }` |

### 6.3 AI 对话模块 (`/api/ai`)

**Controller:** `ApiAiController`  
**注入依赖:** `AiChatService`, `CheckInRecordRepository`

#### `GET /api/ai/chat-stream`

| 项 | 规格 |
|----|------|
| Content-Type | `text/event-stream;charset=utf-8` |
| 参数 | `message` (string), `userId` (默认 "111") |
| Session 读取 | `studentId`, `studentName`, `className` |
| 逻辑 | 同步调用 `aiChatService.chatStream()` 获取完整回复，拆分为逐字符 Flux |
| 延迟 | 每字符间隔 20ms |
| SSE 格式 | `data: {char}\n\n` ... `data: [DONE]\n\n` |
| 前端消费 | `EventSource` 或手动 `fetch` + ReadableStream |

#### `GET /api/ai/records`

| 项 | 规格 |
|----|------|
| 参数 | `studentId` |
| 响应 | `200 { code:200, data: [...CheckInRecordEntity] }` |
| 用途 | 供 AI 工具函数查询签到记录（非分页） |

### 6.4 人脸模块 (`/api/face`)

**Controller:** `ApiFaceController`  
**注入依赖:** `FaceRecognitionService`, `StudentRepository`, `BaiduFaceService`

#### `POST /api/face/register`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 请求体 | `{ "imageData": "base64 string" }` |
| 逻辑 | `faceRecognitionService.registerFace(studentId, imageData)` |
| 成功 | `200 { code:200, data: { registered: true } }` |
| 失败 | `500 { code:500, message: "人脸注册失败，请确保图片中有清晰人脸" }` |

#### `POST /api/face/detect`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 多帧流程 | 请求体含 `frames: [base64, ...]` |
| 步骤 1 | `baiduFaceService.verifyLivenessMultiFrame(frames)` 活体检测 |
| 步骤 2 | `baiduFaceService.searchFace(frames[0])` 人脸搜索 |
| 步骤 3 | 验证 `searchResult.userId === session.studentId` |
| 成功 | `200 { faceVerified: true, confidence: 0.95, livenessPassed: true }` |
| 单帧回退 | 请求体含 `imageData: base64`，直接 `verifyFace()` |

#### `GET /api/face/status`

| 项 | 规格 |
|----|------|
| 鉴权 | 需登录 |
| 逻辑 | 检查 `student.faceImageUrl === "registered"` |
| 响应 | `200 { code:200, data: { registered: bool } }` |

### 6.5 统一错误码

| HTTP Status | code | 场景 |
|-------------|------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 参数错误 / 业务校验失败 / 签到防重 |
| 401 | 401 | 未登录 (session 无 studentId) |
| 404 | 404 | 资源不存在 (学生不存在等) |
| 500 | 500 | 服务端异常 |

---

## 附录: 文件清单与依赖关系

### 后端 Java 文件 (src/main/java/org/example/)

| 文件 | 包 | 职责 |
|------|-----|------|
| `controller/ApiAuthController.java` | controller | 登录/登出/鉴权/个人资料 API |
| `controller/ApiCheckInController.java` | controller | 签到状态/验证/记录 API (含 Redis 防重) |
| `controller/ApiAiController.java` | controller | SSE 流式 AI 对话 API |
| `controller/ApiFaceController.java` | controller | 人脸注册/检测/状态 API |
| `common/ApiResponse.java` | common | 统一响应包装 `@Data` |
| `config/CorsConfig.java` | config | CORS 跨域配置 |
| `config/WebConfig.java` | config | 拦截器注册 (AuthInterceptor) |
| `service/AuthService.java` | service | 认证服务接口 |
| `service/impl/AuthServiceImpl.java` | service.impl | 认证服务实现 |
| `service/FaceRecognitionService.java` | service | 人脸识别接口 |
| `service/impl/DemoFaceRecognitionServiceImpl.java` | service.impl | Demo 人脸识别实现 |
| `service/BaiduFaceService.java` | service | 百度云人脸服务 (活体检测+搜索) |
| `service/LocationVerificationService.java` | service | GPS 定位验证接口 |
| `service/impl/LocationVerificationServiceImpl.java` | service.impl | GPS 验证实现 |
| `service/AiChatService.java` | service | AI 对话服务接口 |
| `service/impl/AiChatServiceImpl.java` | service.impl | AI 对话实现 |
| `entity/StudentEntity.java` | entity | 学生实体 |
| `entity/CheckInRecordEntity.java` | entity | 签到记录实体 |
| `entity/LeaveRequestEntity.java` | entity | 请假申请实体 |
| `entity/ChatHistoryEntity.java` | entity | 聊天历史实体 |
| `entity/RememberTokenEntity.java` | entity | 记住我 Token 实体 |
| `entity/BaseEntity.java` | entity | 基础实体 (id, 创建时间等) |
| `repository/StudentRepository.java` | repository | 学生 JPA Repository |
| `repository/CheckInRecordRepository.java` | repository | 签到记录 Repository |
| `repository/LeaveRequestRepository.java` | repository | 请假 Repository |
| `repository/ChatHistoryRepository.java` | repository | 聊天历史 Repository |
| `repository/RememberTokenRepository.java` | repository | Remember Token Repository |

### 前端 Vue 文件 (frontend/src/)

| 文件 | 职责 | 被引用 |
|------|------|--------|
| `main.js` | 应用入口, 挂载 Pinia + Router | index.html |
| `App.vue` | 根组件, `<router-view />` | main.js |
| `router/index.js` | 路由定义 + 守卫 | main.js |
| `stores/auth.js` | Pinia auth store | 多处 |
| `layouts/DefaultLayout.vue` | 导航栏布局 | 路由 `/` |
| `views/Login.vue` | 玻璃拟态登录页 (GSAP) | 路由 `/login` |
| `views/LoginView.vue` | 简洁登录页 (未使用) | 无 |
| `views/HomeView.vue` | 首页仪表板 (GSAP 呼吸动画) | 路由 `/` 子路由 |
| `views/CheckInView.vue` | 签到页 (占位) | 路由 `/checkin` |
| `views/RecordsView.vue` | 签到记录页 (占位) | 路由 `/records` |
| `views/ChatView.vue` | AI 助手页 (占位) | 路由 `/chat` |
| `components/HelloWorld.vue` | 脚手架示例 (未使用) | 无 |
| `assets/main.css` | Tailwind CSS v4 入口 | main.js |

### Maven 依赖汇总

| 依赖 | 版本 | Phase |
|------|------|-------|
| spring-boot-starter-web | 3.4.4 | 基础 |
| spring-boot-starter-data-jpa | 3.4.4 | 基础 |
| spring-boot-starter-webflux | 3.4.4 | SSE 支持 |
| spring-boot-starter-thymeleaf | 3.4.4 | 基础 (遗留) |
| spring-boot-starter-data-redis | 3.4.4 | Phase 2 |
| spring-session-data-redis | 3.4.4 | Phase 2 |
| spring-security-crypto | 3.4.4 | BCrypt 密码 |
| mysql-connector-j | managed | MySQL |
| mybatis-plus-spring-boot3-starter | 3.5.11 | MyBatis |
| langchain4j-open-ai-spring-boot-starter | 1.0.1-beta6 | AI |
| langchain4j-spring-boot-starter | 1.0.1-beta6 | AI |
| langchain4j-reactor | 1.0.1-beta6 | AI |
| hutool-all | 5.8.16 | 工具库 |
| jjwt-api / impl / jackson | 0.12.6 | JWT (Phase 3 预留) |

### 前端依赖汇总 (package.json)

| 依赖 | 版本 | 类型 | 用途 |
|------|------|------|------|
| vue | ^3.5.34 | runtime | 框架 |
| vue-router | ^4.6.4 | runtime | 路由 |
| pinia | ^3.0.4 | runtime | 状态管理 |
| axios | ^1.17.0 | runtime | HTTP 请求 |
| gsap | ^3.15.0 | runtime | 动画 |
| vite | ^8.0.12 | dev | 构建工具 |
| @vitejs/plugin-vue | ^6.0.6 | dev | Vue SFC 编译 |
| tailwindcss | ^4.3.0 | dev | CSS 工具 |
| @tailwindcss/vite | ^4.3.0 | dev | Tailwind Vite 插件 |

---

## 完成度检查表

| 模块 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 登录认证 | ✅ `ApiAuthController` | ✅ `Login.vue` + `auth.js` | **已完成** |
| Session 管理 | ✅ Redis Session | ✅ `checkAuth()` | **已完成** |
| 签到防重 | ✅ Redis TTL | — | **已完成** |
| 签到验证 API | ✅ `ApiCheckInController.verify` | ⚠️ `CheckInView.vue` 占位 | 后端完成, 前端待实现 |
| 签到记录 API | ✅ `ApiCheckInController.records` | ⚠️ `RecordsView.vue` 占位 | 后端完成, 前端待实现 |
| AI 对话 API | ✅ `ApiAiController.chatStream` | ⚠️ `ChatView.vue` 占位 | 后端完成, 前端待实现 |
| 人脸 API | ✅ `ApiFaceController` | — | 后端完成, 前端待集成 |
| 首页仪表板 | — | ✅ `HomeView.vue` | **已完成** |
| 导航布局 | — | ✅ `DefaultLayout.vue` | **已完成** |
| 暗色主题 | — | ✅ 默认布局 toggle | **已完成** |
| 部署脚本 | ✅ `deploy.sh` | ✅ 前端构建集成 | **已完成** |
