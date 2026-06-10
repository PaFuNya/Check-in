# 校园签到系统 — 全面优化任务

## 任务概述
对校园寝室自助签到系统进行全方位优化，重点是前端重设计 + 后端安全修复 + API密钥工具类。

## 项目信息
- 项目路径: `/root/bighomework`
- 技术栈: Spring Boot 3.4.4 + Vue 3 + Tailwind CSS 4 + Vite 8 + Pinia + GSAP
- 包名: `org.example`
- 前端路径: `/root/bighomework/frontend/`
- 后端路径: `/root/bighomework/src/main/java/org/example/`
- 构建后端: `cd /root/bighomework && mvn clean compile -DskipTests`
- 构建前端: `cd /root/bighomework/frontend && npm run build`

---

## Phase 1: @architect — 分析现状，输出 SPEC

### 1.1 读取所有源文件，分析以下问题：

#### 前端问题清单
1. **Login.vue** — 登录按钮不显示或不可见（可能是 z-index、opacity、display 问题）
2. **整体设计太丑** — 需要完全重新设计所有页面的视觉风格
3. **交互逻辑问题** — 检查所有页面的用户交互流程是否顺畅

#### 后端安全问题清单
1. **WebConfig.java** 和 **WebMvcConfig.java** 重复配置，且 WebConfig 排除了 `/api/**` 的认证拦截
2. **AuthInterceptor.java** — 未登录时 `return true` 放行（第57行），应该重定向到登录页或返回401
3. **BaiduFaceService.java** — API_KEY 和 SECRET_KEY 硬编码
4. **RagConfig.java** — DashScope API_KEY 硬编码

### 1.2 输出 SPEC 文件到 `/root/bighomework/FULL_OPTIMIZE_SPEC.md`，包含：
- 每个文件的具体修改方案
- 前端重新设计的完整设计方案（配色、布局、组件规范）
- API密钥工具类的接口设计
- 认证拦截器的修复方案

---

## Phase 2: @coder — 严格按 SPEC 实现

### 2.1 后端：API密钥配置化工具类

创建 `org.example.config.ApiKeyConfig.java`:
- 所有外部 API 密钥从 `application.properties` 读取（通过 `${}` 占位符）
- 包括: 百度AI (API_KEY, SECRET_KEY), DashScope (API_KEY), DeepSeek (已有)
- 提供 `@Value` 注入或 `@ConfigurationProperties`
- 修改 BaiduFaceService.java 和 RagConfig.java 使用新的配置

### 2.2 后端：修复认证拦截

**合并 WebConfig 和 WebMvcConfig** — 只保留一个配置类
**修复 AuthInterceptor** — 未登录时对 `/api/**` 请求返回 401 JSON 响应，对页面请求重定向到 /login
**修复 excludePathPatterns** — 只排除登录/登出/静态资源等白名单路径，不再排除所有 `/api/**`

排除路径白名单:
```
/auth/login
/auth/logout
/auth/check
/static/**
/css/**
/js/**
/images/**
/favicon.ico
/error
```

### 2.3 前端：全面重新设计

**设计规范（遵循 ui-ux-pro-max 最佳实践）：**

**配色方案：** 现代教育类应用风格
- 主色: `#2563EB` (蓝色) → 保持一致
- 渐变背景: 柔和蓝紫渐变
- 卡片: glass-morphism 风格（已有基础，需要加强对比度）
- 文字: `#0F172A` (深色标题) + `#475569` (正文) + `#94A3B8` (辅助)
- 暗色模式: 完整支持

**页面清单（每个都要重写 template + style）：**

1. **Login.vue** — 确保登录按钮可见且突出
   - 大号蓝色渐变按钮，有明确的"登录"文字
   - 表单字段间距合理
   - 加载状态清晰
   - 错误提示明显

2. **HomeView.vue** — 首页仪表板
   - 问候语 + 日期
   - 签到状态卡片（大号数字显示）
   - 签到按钮（呼吸动画，突出）
   - 快捷操作网格

3. **CheckInView.vue** — 签到页面
   - 人脸识别区域
   - 位置信息
   - 签到确认流程

4. **ChatView.vue** — AI 对话
   - 消息气泡样式
   - 输入区域
   - 滚动行为

5. **RecordsView.vue** — 签到记录
   - 记录列表
   - 分页
   - 筛选

6. **ProfileView.vue** — 个人中心
   - 用户信息展示
   - 编辑表单

7. **DefaultLayout.vue** — 布局容器
   - 顶部导航栏（桌面+移动）
   - 主内容区

**关键 UI/UX 规则（必须遵守）：**
- 所有可交互元素必须有 `cursor-pointer`
- 触摸目标最小 44x44px
- 过渡动画 150-300ms
- 禁止用 emoji 当图标（使用 SVG）
- 浅色模式文字对比度 4.5:1
- 响应式：375px / 768px / 1024px / 1440px
- glass-card 在浅色模式下背景 `bg-white/80` 不低于 0.8 透明度

### 2.4 前端构建
修改完成后执行 `cd /root/bighomework/frontend && npm run build`，将 dist 输出复制到 Spring Boot 静态资源目录。

---

## Phase 3: @reviewer — 逐条审查

对照 SPEC 逐项检查：
1. API密钥是否全部从 application.properties 读取，无硬编码
2. 认证拦截是否正确（未登录 API 返回 401，页面重定向）
3. WebConfig/WebMvcConfig 是否合并
4. 前端每个页面是否有完整的 template + style
5. 登录按钮是否可见（检查 CSS）
6. 响应式是否正确
7. 暗色模式是否完整
8. 后端编译是否通过 (`mvn clean compile -DskipTests`)
9. 前端构建是否通过 (`npm run build`)

输出审查报告到 `/root/bighomework/FULL_OPTIMIZE_REVIEW.md`

---

## 重要约束
1. **不要删除已有功能** — 只优化，不删减
2. **保持 API 接口兼容** — 前端调用的 `/api/*` 路径不变
3. **application.properties 中的密钥用占位符** — 真实值不写入代码，通过环境变量注入
4. **前端 build 产物必须复制到 Spring Boot 静态目录** — `src/main/resources/static/`
5. **保持 GSAP 动画** — 现有动画保留或增强
6. **中文 UI** — 所有界面文字用中文
