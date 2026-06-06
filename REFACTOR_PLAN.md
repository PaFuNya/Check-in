# 校园寝室签到系统 — 重构实施方案

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** 将 Thymeleaf 模板渲染的签到系统重构为 Vue 3 SPA + Spring Boot JSON API + Redis 缓存的前后端分离架构。

**Architecture:** 前端 Vue 3 + Vite + Tailwind CSS + GSAP 动画，通过 Axios 调用后端 `/api/*` JSON API，SSE 流式输出 AI 对话。后端 Spring Boot 提供 RESTful API，Redis 管理 Session 和缓存。Nginx (1Panel) 反代前端静态资源 + 后端 API。

**Tech Stack:** Vue 3, Vite, Vue Router 4, Pinia, Axios, Tailwind CSS, GSAP 3, Spring Boot 3.4.4, Spring Session Data Redis, MySQL 8.4, LangChain4j

---

## 阶段 1：后端 API 层重构

> **目标：** 将 Thymeleaf Controller 改造为 JSON API，新增统一响应格式和 CORS 配置。
> **Agent 流程：** architect(出SPEC) → engineer(实现) → qa(验证)

### Task 1.1: 创建统一响应封装类

**Objective:** 定义标准 API 响应格式，所有接口统一使用。

**Files:**
- Create: `src/main/java/org/example/common/ApiResponse.java`

**Step 1: 创建 ApiResponse 类**

```java
package org.example.common;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200; r.message = "success"; r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code; r.message = message;
        return r;
    }

    // getters/setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

**Step 2: 验证编译通过**

Run: `cd /root/bighomework && source ~/.cargo/env && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/org/example/common/ApiResponse.java
git commit -m "feat: add unified API response wrapper"
```

---

### Task 1.2: 配置 CORS 跨域

**Objective:** 允许 Vue 前端 (localhost:5173) 和生产域名访问后端 API。

**Files:**
- Create: `src/main/java/org/example/config/CorsConfig.java`

**Step 1: 创建 CORS 配置**

```java
package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:5173", "https://check.kaminya.top")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

**Step 2: 验证编译**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/org/example/config/CorsConfig.java
git commit -m "feat: add CORS config for Vue frontend"
```

---

### Task 1.3: 认证 API 改造

**Objective:** 将 AuthController 的登录/登出/检查接口改为 `/api/auth/*` JSON API。

**Files:**
- Create: `src/main/java/org/example/controller/ApiAuthController.java`

**Step 1: 创建 ApiAuthController**

```java
package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.StudentEntity;
import org.example.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        String studentId = body.get("studentId");
        String password = body.get("password");
        if (studentId == null || password == null) {
            return ApiResponse.error(400, "学号和密码不能为空");
        }
        Optional<StudentEntity> opt = studentRepository.findById(studentId);
        if (opt.isEmpty() || !opt.get().getPassword().equals(password)) {
            return ApiResponse.error(401, "学号或密码错误");
        }
        StudentEntity s = opt.get();
        session.setAttribute("studentId", studentId);
        session.setAttribute("studentName", s.getStudentName());
        return ApiResponse.ok(Map.of("studentId", studentId, "studentName", s.getStudentName()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok();
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        return ApiResponse.ok(Map.of(
            "loggedIn", true,
            "studentId", sid,
            "studentName", session.getAttribute("studentName")
        ));
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> getProfile(HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        Optional<StudentEntity> opt = studentRepository.findById(sid);
        if (opt.isEmpty()) return ApiResponse.error(404, "学生不存在");
        StudentEntity s = opt.get();
        return ApiResponse.ok(Map.of(
            "studentId", s.getStudentId(),
            "studentName", s.getStudentName() != null ? s.getStudentName() : "",
            "className", s.getClassName() != null ? s.getClassName() : "",
            "phoneNumber", s.getPhoneNumber() != null ? s.getPhoneNumber() : "",
            "avatarUrl", s.getAvatarUrl() != null ? s.getAvatarUrl() : ""
        ));
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        Optional<StudentEntity> opt = studentRepository.findById(sid);
        if (opt.isEmpty()) return ApiResponse.error(404, "学生不存在");
        StudentEntity s = opt.get();
        if (body.containsKey("className")) s.setClassName(body.get("className"));
        if (body.containsKey("phoneNumber")) s.setPhoneNumber(body.get("phoneNumber"));
        studentRepository.save(s);
        return ApiResponse.ok();
    }
}
```

**Step 2: 编译验证**

Run: `mvn compile -q`

**Step 3: Commit**

```bash
git add src/main/java/org/example/controller/ApiAuthController.java
git commit -m "feat: add REST auth API (/api/auth/*)"
```

---

### Task 1.4: 签到 API 改造

**Objective:** 将签到验证和记录查询改为 `/api/checkin/*` JSON API。

**Files:**
- Create: `src/main/java/org/example/controller/ApiCheckInController.java`

**Step 1: 创建 ApiCheckInController**

```java
package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.CheckInRecordEntity;
import org.example.entity.StudentEntity;
import org.example.repository.CheckInRecordRepository;
import org.example.repository.StudentRepository;
import org.example.service.FaceRecognitionService;
import org.example.service.LocationVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/checkin")
public class ApiCheckInController {

    @Autowired private FaceRecognitionService faceRecognitionService;
    @Autowired private LocationVerificationService locationVerificationService;
    @Autowired private CheckInRecordRepository checkInRecordRepository;
    @Autowired private StudentRepository studentRepository;

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> todayStatus(HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        // 查询今日是否已签到
        List<CheckInRecordEntity> records = checkInRecordRepository.findByStudentIdAndCheckTimeAfter(
            sid, getTodayStart());
        boolean checkedIn = !records.isEmpty();
        return ApiResponse.ok(Map.of("checkedIn", checkedIn, "count", records.size()));
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestBody Map<String, Object> body, HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");

        String faceImageData = (String) body.get("faceImageData");
        Number latNum = (Number) body.get("latitude");
        Number lngNum = (Number) body.get("longitude");
        if (faceImageData == null || latNum == null || lngNum == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        boolean faceOk = faceRecognitionService.verifyFace(sid, faceImageData);
        boolean locOk = locationVerificationService.verifyLocation(sid, latNum.doubleValue(), lngNum.doubleValue());

        if (faceOk && locOk) {
            Optional<StudentEntity> opt = studentRepository.findById(sid);
            if (opt.isEmpty()) return ApiResponse.error(404, "学生不存在");
            StudentEntity s = opt.get();
            CheckInRecordEntity record = new CheckInRecordEntity();
            record.setStudentId(sid);
            record.setStudentName(s.getStudentName());
            record.setRoomNumber(s.getRoomNumber());
            record.setDormBuilding(s.getDormBuilding());
            record.setStatus("已签到");
            record.setLocationInfo(latNum + "," + lngNum);
            record.setCheckTime(new Date());
            checkInRecordRepository.save(record);
            return ApiResponse.ok(Map.of("faceVerified", true, "locationVerified", true));
        }
        return ApiResponse.error(400, "签到失败: " + (!faceOk ? "人脸识别未通过; " : "") + (!locOk ? "GPS不在范围内" : ""));
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> records(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        Page<CheckInRecordEntity> p = checkInRecordRepository.findByStudentId(
            sid, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkTime")));
        return ApiResponse.ok(Map.of("content", p.getContent(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements()));
    }

    private Date getTodayStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
```

**Step 2: 检查 CheckInRecordRepository 是否有分页查询方法，如没有则添加**

```java
// CheckInRecordRepository.java 中添加:
Page<CheckInRecordEntity> findByStudentId(String studentId, Pageable pageable);
List<CheckInRecordEntity> findByStudentIdAndCheckTimeAfter(String studentId, Date after);
```

**Step 3: 编译验证 + Commit**

```bash
mvn compile -q
git add -A && git commit -m "feat: add REST checkin API (/api/checkin/*)"
```

---

### Task 1.5: AI 对话 SSE 流式 API

**Objective:** 将 AI 对话改为真正的 SSE 流式输出。

**Files:**
- Create: `src/main/java/org/example/controller/ApiAiController.java`

**Step 1: 创建 SSE 流式控制器**

```java
package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.entity.CheckInRecordEntity;
import org.example.repository.CheckInRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class ApiAiController {

    @Autowired private org.example.service.AiChatService aiChatService;
    @Autowired private CheckInRecordRepository checkInRecordRepository;

    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message, @RequestParam String userId) {
        return aiChatService.streamChat(message, userId)
            .map(chunk -> "data: " + chunk + "\n\n")
            .concatWithValues("data: [DONE]\n\n");
    }

    @GetMapping("/records")
    public ApiResponse<List<CheckInRecordEntity>> records(@RequestParam String studentId) {
        List<CheckInRecordEntity> records = checkInRecordRepository.findByStudentId(studentId, Sort.by(Sort.Direction.DESC, "checkTime"));
        return ApiResponse.ok(records);
    }
}
```

**Step 2: 确认 AiChatService 有 streamChat 方法（返回 Flux<String>），如没有需实现**

**Step 3: 编译 + Commit**

```bash
mvn compile -q
git add -A && git commit -m "feat: add SSE streaming AI chat API (/api/ai/*)"
```

---

### Task 1.6: 人脸 API 改造

**Objective:** 将人脸注册和检测改为 `/api/face/*` JSON API。

**Files:**
- Create: `src/main/java/org/example/controller/ApiFaceController.java`

**Step 1: 创建 ApiFaceController**

```java
package org.example.controller;

import jakarta.servlet.http.HttpSession;
import org.example.common.ApiResponse;
import org.example.service.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/face")
public class ApiFaceController {

    @Autowired private FaceRecognitionService faceRecognitionService;

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody Map<String, String> body, HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        String imageData = body.get("imageData");
        if (imageData == null) return ApiResponse.error(400, "缺少图片数据");
        boolean ok = faceRecognitionService.registerFace(sid, imageData);
        return ok ? ApiResponse.ok() : ApiResponse.error(500, "人脸注册失败");
    }

    @PostMapping("/detect")
    public ApiResponse<Map<String, Object>> detect(@RequestBody Map<String, Object> body, HttpSession session) {
        String sid = (String) session.getAttribute("studentId");
        if (sid == null) return ApiResponse.error(401, "未登录");
        @SuppressWarnings("unchecked")
        java.util.List<String> frames = (java.util.List<String>) body.get("frames");
        if (frames == null || frames.isEmpty()) return ApiResponse.error(400, "缺少视频帧");
        boolean ok = faceRecognitionService.verifyFace(sid, frames.get(0));
        return ok ? ApiResponse.ok(Map.of("success", true)) : ApiResponse.error(400, "人脸识别未通过");
    }
}
```

**Step 2: 编译 + Commit**

```bash
mvn compile -q
git add -A && git commit -m "feat: add REST face API (/api/face/*)"
```

---

## 阶段 2：Redis 集成

> **目标：** 引入 Redis 实现 Session 持久化、签到防重、AI 对话缓存。
> **Agent 流程：** architect(出SPEC) → engineer(实现) → qa(验证)

### Task 2.1: 添加 Redis 依赖

**Objective:** 在 pom.xml 中添加 Spring Data Redis 和 Spring Session Data Redis 依赖。

**Files:**
- Modify: `pom.xml`

**Step 1: 添加依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

**Step 2: 添加 Redis 配置到 application.properties**

```properties
# Redis
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.session.store-type=redis
spring.session.timeout=1800
```

**Step 3: 编译 + Commit**

```bash
mvn compile -q
git add -A && git commit -m "feat: add Redis + Spring Session dependencies"
```

---

### Task 2.2: Docker 部署 Redis

**Objective:** 在 WSL 中通过 Docker 运行 Redis 容器。

**Step 1: 启动 Redis 容器**

```bash
docker run -d --name 1Panel-redis-checkin \
  -p 6379:6379 \
  -v /opt/1panel/apps/redis-checkin/data:/data \
  --restart unless-stopped \
  redis:7-alpine
```

**Step 2: 验证连接**

```bash
docker exec 1Panel-redis-checkin redis-cli ping
# Expected: PONG
```

---

### Task 2.3: 签到防重逻辑

**Objective:** 使用 Redis 记录今日已签到学生，避免重复签到查询数据库。

**Files:**
- Modify: `src/main/java/org/example/controller/ApiCheckInController.java`

**Step 1: 注入 RedisTemplate，在签到方法中添加防重检查**

```java
@Autowired private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

// 在 verify 方法开头添加:
String todayKey = "checkin:today:" + sid + ":" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
if (Boolean.TRUE.equals(redisTemplate.hasKey(todayKey))) {
    return ApiResponse.error(400, "今日已签到，请勿重复签到");
}
// 签到成功后:
redisTemplate.opsForValue().set(todayKey, "1", java.time.Duration.ofHours(23));
```

**Step 2: 编译 + Commit**

```bash
mvn compile -q
git add -A && git commit -m "feat: add Redis-based duplicate check-in prevention"
```

---

## 阶段 3：Vue 3 前端项目搭建

> **目标：** 搭建 Vue 3 + Vite 项目骨架，配置 Tailwind CSS、GSAP、Vue Router、Pinia。
> **参考 skill：** ui-ux-pro-max (glassmorphism 风格), gsap-core (动画)
> **Agent 流程：** architect(出SPEC) → engineer(实现)

### Task 3.1: 初始化 Vue 项目

**Objective:** 使用 Vite 创建 Vue 3 项目，安装依赖。

**Step 1: 创建项目**

```bash
cd /root/bighomework
npm create vite@latest frontend -- --template vue
cd frontend
npm install
npm install vue-router@4 pinia axios
npm install -D tailwindcss @tailwindcss/vite
```

**Step 2: 配置 Tailwind CSS**

`frontend/vite.config.js`:
```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8088'
    }
  }
})
```

`frontend/src/assets/main.css`:
```css
@import "tailwindcss";

:root {
  --bg-canvas: #F7F6F3;
  --bg-surface: #FFFFFF;
  --text-primary: #1a1a1a;
  --text-secondary: #6b7280;
  --accent: #2563EB;
  --success: #059669;
  --error: #DC2626;
  --glass-bg: rgba(255,255,255,0.72);
  --glass-border: 1px solid rgba(255,255,255,0.5);
  --glass-shadow: 0 8px 32px rgba(0,0,0,0.08);
}

[data-theme="dark"] {
  --bg-canvas: #0f0f0f;
  --bg-surface: #1a1a1a;
  --text-primary: #f0f0f0;
  --text-secondary: #9ca3af;
  --accent: #3b82f6;
  --success: #10b981;
  --error: #ef4444;
  --glass-bg: rgba(30,30,30,0.72);
  --glass-border: 1px solid rgba(255,255,255,0.08);
  --glass-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
```

**Step 3: 验证项目启动**

```bash
npm run dev
# 访问 http://localhost:5173 看到 Vite 默认页
```

**Step 4: Commit**

```bash
git add frontend/
git commit -m "feat: init Vue 3 + Vite + Tailwind CSS project"
```

---

### Task 3.2: 配置 Vue Router 和 Pinia

**Objective:** 设置路由和状态管理。

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`
- Modify: `frontend/src/main.js`

**Step 1: 创建路由**

```js
// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('../layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'checkin', name: 'CheckIn', component: () => import('../views/CheckIn.vue') },
      { path: 'records', name: 'Records', component: () => import('../views/Records.vue') },
      { path: 'chat', name: 'Chat', component: () => import('../views/Chat.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.meta.guest) return next()
  if (to.meta.requiresAuth) {
    try {
      const resp = await fetch('/api/auth/check')
      const data = await resp.json()
      if (data.code === 200) return next()
    } catch {}
    return next('/login')
  }
  next()
})

export default router
```

**Step 2: 创建 auth store**

```js
// frontend/src/stores/auth.js
import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const studentId = ref(null)
  const studentName = ref(null)
  const loggedIn = ref(false)

  async function checkAuth() {
    try {
      const { data } = await axios.get('/api/auth/check')
      if (data.code === 200) {
        studentId.value = data.data.studentId
        studentName.value = data.data.studentName
        loggedIn.value = true
      }
    } catch {
      loggedIn.value = false
    }
  }

  async function login(id, password) {
    const { data } = await axios.post('/api/auth/login', { studentId: id, password })
    if (data.code === 200) {
      studentId.value = data.data.studentId
      studentName.value = data.data.studentName
      loggedIn.value = true
    }
    return data
  }

  async function logout() {
    await axios.post('/api/auth/logout')
    loggedIn.value = false
    studentId.value = null
    studentName.value = null
  }

  return { studentId, studentName, loggedIn, checkAuth, login, logout }
})
```

**Step 3: 更新 main.js**

```js
// frontend/src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './assets/main.css'

createApp(App).use(createPinia()).use(router).mount('#app')
```

**Step 4: Commit**

```bash
git add frontend/src/
git commit -m "feat: add Vue Router + Pinia auth store"
```

---

### Task 3.3: 创建 DefaultLayout 布局

**Objective:** 实现毛玻璃导航栏 + 主体布局，参考 ui-ux-pro-max 的 glassmorphism 风格。

**Files:**
- Create: `frontend/src/layouts/DefaultLayout.vue`

**Step 1: 创建布局组件**

```vue
<template>
  <div class="h-screen flex flex-col" :class="theme">
    <!-- Nav -->
    <nav class="flex items-center justify-between px-6 py-3 backdrop-blur-xl border-b"
         style="background: var(--glass-bg); border-color: var(--glass-border);">
      <span class="text-lg font-bold" style="color: var(--text-primary)">寝室签到助手</span>
      <div class="flex items-center gap-3">
        <span class="text-sm" style="color: var(--text-secondary)">{{ clock }}</span>
        <span class="text-sm cursor-pointer" style="color: var(--text-secondary)"
              @click="$router.push('/')">{{ auth.studentName }}</span>
        <button @click="cycleTheme"
                class="w-9 h-9 rounded-lg border flex items-center justify-center backdrop-blur-xl transition-all hover:-translate-y-0.5"
                style="background: var(--glass-bg); border-color: var(--glass-border);">
          {{ themeIcon }}
        </button>
        <button @click="handleLogout"
                class="px-3 py-1.5 rounded-lg border text-sm transition-all hover:-translate-y-0.5"
                style="border-color: var(--glass-border); color: var(--error);">
          退出
        </button>
      </div>
    </nav>
    <!-- Main -->
    <main class="flex-1 overflow-hidden">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const themes = ['light', 'dark', 'system']
const themeIdx = ref(0)
const theme = computed(() => themes[themeIdx.value] === 'dark' ? 'dark' : '')
const themeIcon = computed(() => ['☀️', '🌙', '🖥️'][themeIdx.value])
const clock = ref('')

let timer
onMounted(() => {
  auth.checkAuth()
  const update = () => { clock.value = new Date().toLocaleTimeString('zh-CN', { hour12: false }) }
  update(); timer = setInterval(update, 1000)
})
onUnmounted(() => clearInterval(timer))

function cycleTheme() { themeIdx.value = (themeIdx.value + 1) % themes.length }
async function handleLogout() { await auth.logout(); router.push('/login') }
</script>
```

**Step 2: Commit**

```bash
git add frontend/src/layouts/
git commit -m "feat: add glassmorphism DefaultLayout with theme toggle"
```

---

## 阶段 4：前端页面实现

> **目标：** 逐页面实现 Vue 前端，保持毛玻璃风格，GSAP 动画增强。
> **参考 skill：** ui-ux-pro-max, gsap-core, gsap-scrolltrigger
> **Agent 流程：** engineer(实现) → qa(验证)

### Task 4.1: Login.vue 登录页

**Objective:** 实现登录页面，毛玻璃卡片风格，带主题切换。

**Files:**
- Create: `frontend/src/views/Login.vue`

**关键设计点 (ui-ux-pro-max):**
- 44px 最小触摸目标
- 4.5:1 对比度
- 毛玻璃卡片 (backdrop-blur)
- GSAP fadeInUp 入场动画
- 错误信息就近显示

**Step 1: 创建 Login.vue**

```vue
<template>
  <div class="min-h-screen flex items-center justify-center relative overflow-hidden"
       style="background: var(--bg-canvas)">
    <!-- Background orbs -->
    <div class="absolute -top-20 -left-10 w-[500px] h-[500px] rounded-full pointer-events-none"
         style="background: radial-gradient(circle, rgba(37,99,235,0.15), transparent 70%); animation: orbFloat 8s ease-in-out infinite"/>
    <div class="absolute -bottom-20 -right-10 w-[400px] h-[400px] rounded-full pointer-events-none"
         style="background: radial-gradient(circle, rgba(16,185,129,0.1), transparent 70%); animation: orbFloat 10s ease-in-out infinite reverse"/>

    <!-- Login Card -->
    <div ref="card" class="backdrop-blur-xl rounded-2xl p-10 w-[420px] max-w-[90%] shadow-xl"
         style="background: var(--glass-bg); border: var(--glass-border); box-shadow: var(--glass-shadow)">
      <h1 class="text-xl font-bold text-center mb-1" style="color: var(--text-primary)">寝室签到助手</h1>
      <p class="text-center text-sm mb-8" style="color: var(--text-secondary)">校园寝室自助签到系统</p>

      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label class="block text-sm mb-1.5 font-medium" style="color: var(--text-secondary)">学号</label>
          <input v-model="studentId" type="text" placeholder="请输入学号"
                 class="w-full px-3.5 py-2.5 rounded-xl border text-sm outline-none transition-all focus:scale-[1.01]"
                 style="background: var(--bg-surface); border-color: var(--glass-border); color: var(--text-primary)"
                 required />
        </div>
        <div class="mb-5">
          <label class="block text-sm mb-1.5 font-medium" style="color: var(--text-secondary)">密码</label>
          <input v-model="password" type="password" placeholder="请输入密码"
                 class="w-full px-3.5 py-2.5 rounded-xl border text-sm outline-none transition-all focus:scale-[1.01]"
                 style="background: var(--bg-surface); border-color: var(--glass-border); color: var(--text-primary)"
                 required />
        </div>
        <button type="submit" :disabled="loading"
                class="w-full py-3 rounded-xl text-white text-sm font-medium transition-all hover:-translate-y-0.5 disabled:opacity-50"
                style="background: var(--accent)">
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <p v-if="error" class="mt-4 text-center text-sm" style="color: var(--error)">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import gsap from 'gsap'

const auth = useAuthStore()
const router = useRouter()
const card = ref(null)
const studentId = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (card.value) gsap.from(card.value, { opacity: 0, y: 20, duration: 0.5, ease: 'power2.out' })
})

async function handleLogin() {
  loading.value = true; error.value = ''
  try {
    const data = await auth.login(studentId.value, password.value)
    if (data.code === 200) router.push('/')
    else error.value = data.message
  } catch { error.value = '网络错误' }
  finally { loading.value = false }
}
</script>

<style scoped>
@keyframes orbFloat { 0%,100% { transform: translate(0,0) scale(1) } 50% { transform: translate(30px,-20px) scale(1.05) } }
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/views/Login.vue
git commit -m "feat: implement Login page with glassmorphism + GSAP"
```

---

### Task 4.2: Home.vue 首页

**Objective:** 实现首页 — 签到状态卡片 + 圆形签到按钮 + 快捷操作 + AI 入口。

**Files:**
- Create: `frontend/src/views/Home.vue`

**Step 1: 创建 Home.vue**

```vue
<template>
  <div class="h-full flex flex-col overflow-auto" style="background: var(--bg-canvas)">
    <!-- Hero -->
    <div class="flex flex-col items-center py-9 px-6">
      <h2 ref="greeting" class="text-xl font-bold mb-1" style="color: var(--text-primary)">
        {{ greetText }}，{{ auth.studentName }}！
      </h2>
      <p class="text-sm mb-6" style="color: var(--text-secondary)">欢迎使用寝室自助签到系统</p>

      <!-- Checkin circle button with GSAP breathe animation -->
      <button ref="checkinBtn" @click="$router.push('/checkin')"
              class="w-[130px] h-[130px] rounded-full text-white flex flex-col items-center justify-center gap-1.5 transition-transform hover:scale-105 active:scale-95"
              style="background: linear-gradient(135deg, var(--accent), #1d4ed8)">
        <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
        </svg>
        <span class="text-sm font-semibold">查寝签到</span>
        <span class="text-xs opacity-80">点击开始签到</span>
      </button>

      <div class="flex gap-3 mt-5">
        <button @click="$router.push('/records')"
                class="px-5 py-2 rounded-full border text-sm backdrop-blur-xl transition-all hover:-translate-y-0.5"
                style="border-color: var(--glass-border); background: var(--glass-bg); color: var(--text-primary)">
          签到记录
        </button>
        <button @click="$router.push('/chat')"
                class="px-5 py-2 rounded-full border text-sm backdrop-blur-xl transition-all hover:-translate-y-0.5"
                style="border-color: var(--glass-border); background: var(--glass-bg); color: var(--text-primary)">
          咨询问题
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import gsap from 'gsap'

const auth = useAuthStore()
const greeting = ref(null)
const checkinBtn = ref(null)

const greetText = computed(() => {
  const h = new Date().getHours()
  return h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
})

onMounted(() => {
  if (greeting.value) gsap.from(greeting.value, { opacity: 0, y: 20, duration: 0.5, ease: 'power2.out' })
  if (checkinBtn.value) {
    gsap.from(checkinBtn.value, { opacity: 0, scale: 0.9, duration: 0.5, delay: 0.1, ease: 'back.out(1.7)' })
    // Breathe glow animation
    gsap.to(checkinBtn.value, {
      boxShadow: '0 8px 32px rgba(37,99,235,0.3), 0 0 0 20px rgba(37,99,235,0)',
      duration: 1.25, repeat: -1, yoyo: true, ease: 'power1.inOut'
    })
  }
})
</script>
```

**Step 2: Commit**

```bash
git add frontend/src/views/Home.vue
git commit -m "feat: implement Home page with GSAP breathe animation"
```

---

### Task 4.3: CheckIn.vue 签到页

**Objective:** 实现签到页 — GPS 定位 + 人脸拍照，流程精简为 3 步。

**优化点：**
- GPS 和人脸并行验证（不互相等待）
- 人脸改为拍照 1 帧（不录 5 秒视频）
- GPS 失败有重试按钮
- GSAP 状态切换动画

**Files:**
- Create: `frontend/src/views/CheckIn.vue`

**Step 1: 创建 CheckIn.vue**（核心签到页面，GPS + 人脸并行）

**Step 2: Commit**

```bash
git add frontend/src/views/CheckIn.vue
git commit -m "feat: implement CheckIn page with parallel GPS + face verify"
```

---

### Task 4.4: Records.vue 签到记录

**Objective:** 独立签到记录页面，分页查询，GSAP 列表动画。

**Files:**
- Create: `frontend/src/views/Records.vue`

**Step 1: 创建 Records.vue**（分页 + GSAP stagger 入场动画）

**Step 2: Commit**

```bash
git add frontend/src/views/Records.vue
git commit -m "feat: implement Records page with pagination + GSAP stagger"
```

---

### Task 4.5: Chat.vue AI 对话

**Objective:** 实现 SSE 流式 AI 对话，逐字输出，GSAP 打字效果。

**Files:**
- Create: `frontend/src/views/Chat.vue`

**关键实现：**
```js
// SSE 流式读取
function streamChat(message) {
  const es = new EventSource(`/api/ai/chat-stream?message=${encodeURIComponent(message)}&userId=${auth.studentId}`)
  es.onmessage = (e) => {
    if (e.data === '[DONE]') { es.close(); return }
    botText.value += e.data  // 逐字追加
  }
  es.onerror = () => es.close()
}
```

**Step 1: 创建 Chat.vue**

**Step 2: Commit**

```bash
git add frontend/src/views/Chat.vue
git commit -m "feat: implement Chat page with SSE streaming + GSAP"
```

---

## 阶段 5：构建与部署

> **目标：** 前端构建、Nginx 配置、部署脚本更新。
> **Agent 流程：** director(部署) → qa(验证)

### Task 5.1: 前端构建配置

**Objective:** 配置 Vite 构建输出到 Spring Boot 静态资源目录或独立 dist 目录。

**Step 1: 修改 vite.config.js 的 build 输出**

```js
export default defineConfig({
  // ...
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
```

**Step 2: 构建**

```bash
cd frontend && npm run build
```

---

### Task 5.2: Nginx 配置

**Objective:** 配置 1Panel Nginx 反代 — 前端静态资源 + 后端 API。

**Step 1: 更新 1Panel 网站 Nginx 配置**

```nginx
location / {
    root /root/bighomework/frontend/dist;
    try_files $uri $uri/ /index.html;
}

location /api/ {
    proxy_pass http://127.0.0.1:8088;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /api/ai/chat-stream {
    proxy_pass http://127.0.0.1:8088;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 300s;
}
```

---

### Task 5.3: 更新部署脚本

**Objective:** 更新 deploy.sh 支持前端构建 + 后端打包 + Redis 检查。

**Step 1: 更新 deploy.sh**

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

---

## 阶段 6：测试与验证

> **参考 skill：** webapp-testing (Playwright 测试)
> **Agent 流程：** qa(测试) → engineer(修复)

### Task 6.1: 端到端测试清单

| # | 测试项 | 步骤 | 预期 |
|---|--------|------|------|
| 1 | 登录 | 输入学号密码，点登录 | 跳转首页，显示用户名 |
| 2 | 签到状态 | 打开首页 | 显示今日签到状态 |
| 3 | GPS 定位 | 打开签到页 | 自动获取位置，显示距离 |
| 4 | 人脸识别 | 开启摄像头，拍照 | 识别通过/失败有明确提示 |
| 5 | 签到 | GPS+人脸都通过后点签到 | 显示签到成功 |
| 6 | 防重复 | 同一天再签一次 | 提示已签到 |
| 7 | AI 对话 | 发送消息 | SSE 逐字输出 |
| 8 | 签到记录 | 点签到记录 | 分页显示，GSAP 动画 |
| 9 | 主题切换 | 点主题按钮 | light/dark/system 切换 |
| 10 | Session 持久化 | 重启后端 | 用户仍在线 |

---

## Agent 协作流程总结

```
PM        → 需求确认、验收标准
Architect → 阅读现有代码，输出每个阶段的 SPEC 文件
Engineer  → 严格按 SPEC 写代码，TDD 流程
QA        → 对照 SPEC 逐条验证 + Playwright 测试
Director  → Docker Redis 部署、Nginx 配置、Gitee 同步
```

### 每个 Task 的执行命令 (subagent-driven-development)

```bash
# 1. Architect 出 SPEC
delegate_task(goal="阅读 /root/bighomework 现有代码，输出 SPEC_PHASE1_API.md")

# 2. Engineer 实现 (每个 Task 一个 subagent)
delegate_task(goal="实现 Task 1.1: 创建 ApiResponse.java", context="[完整 SPEC]", toolsets=['terminal','file'])

# 3. QA 审查 (两阶段: spec compliance → code quality)
delegate_task(goal="审查 Task 1.1 实现是否符合 SPEC", toolsets=['file'])
delegate_task(goal="审查 Task 1.1 代码质量", toolsets=['file'])

# 4. 只有两阶段都 PASS 才进入下一个 Task
```

---

## 里程碑

| M | 阶段 | 产出 | 验收 |
|---|------|------|------|
| M1 | 1+2 | 后端 API + Redis | curl 测试所有 API 返回 200 |
| M2 | 3+4 | Vue 前端 | 浏览器访问 localhost:5173 可用 |
| M3 | 5 | 部署 | check.kaminya.top 访问正常 |
| M4 | 6 | 测试通过 | 10 项测试全部 PASS |
