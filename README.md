# 校园寝室自助签到系统

基于 Spring Boot + LangChain4j + Vue 3 的校园寝室自助签到 AI 助手。学生通过自然语言对话完成寝室签到、请假报备、考勤查询等操作。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4.4, Java 17, LangChain4j 1.0.1-beta6, Spring Data JPA, MySQL 8 |
| 前端 | Vue 3 + Vite, Pinia, Vue Router, GSAP |
| AI | DeepSeek API (对话), DashScope qwen-max (RAG embedding), 百度AI (人脸识别) |
| 搜索 | 关键词 n-gram 匹配 + 组合加分算法 (学生手册 RAG) |
| 部署 | Maven, JAR, Cloudflare Tunnel (*.kaminya.top) |

## 功能特性

1. **学号密码登录** - Session + Remember Token 双重认证，30天免登录
2. **人脸注册** - 摄像头拍照 → 百度AI 人脸注册
3. **AI 智能助手** - SSE 流式对话，意图识别路由：
   - 🏠 请假报备（多轮对话采集信息 → 数据库入库）
   - 📊 状态查询（签到记录、请假状态）
   - 📖 规则问答（RAG 检索学生手册，精确到页码）
   - 👤 个人信息查询
4. **聊天记录持久化** - 按用户隔离，支持清空
5. **深色/浅色主题** - 跟随系统或手动切换

## 项目结构

```
src/main/java/org/example/
├── Application.java              # 启动类
├── common/ApiResponse.java       # 统一响应封装
├── config/                       # 配置类
│   ├── ApiKeyConfig.java         # API密钥配置化
│   ├── RagConfig.java            # RAG 向量存储配置
│   ├── RagStartupLoader.java     # 启动时加载手册文本
│   └── WebMvcConfig.java         # MVC + 拦截器配置
├── controller/                   # REST 控制器
│   ├── ApiAuthController.java    # /api/auth/* 认证
│   ├── ApiAiController.java      # /api/ai/* AI对话 + 聊天记录
│   ├── ApiCheckInController.java # /api/checkin/* 签到
│   └── PageController.java       # SPA 路由回退
├── entity/                       # JPA 实体
├── repository/                   # Spring Data Repository
├── service/impl/                 # 业务实现
│   ├── AiChatServiceImpl.java    # AI对话 + RAG搜索核心
│   └── AuthServiceImpl.java      # 认证 + 登出
├── aiservice/                    # LangChain4j AI Service 接口
├── aop/ChatFlowAop.java          # 聊天记录 AOP 切面
├── interceptor/AuthInterceptor.java # 认证拦截器
└── enums/                        # 枚举 (ChatRole, CheckInStatus, AuditStatus)

frontend/src/
├── stores/auth.js                # Pinia 认证状态管理
├── router/index.js               # Vue Router + 认证守卫
├── layouts/DefaultLayout.vue     # 导航栏 + 底部Tab
└── views/
    ├── Login.vue                 # 登录页
    ├── HomeView.vue              # 首页仪表板
    ├── ChatView.vue              # AI对话 (SSE流式)
    ├── CheckInView.vue           # 签到页
    ├── ProfileView.vue           # 个人中心
    ├── RecordsView.vue           # 签到记录
    └── FaceRegisterView.vue      # 人脸注册

data/
├── student_handbook.pdf          # 学生手册原文
├── handbook_tagged.txt           # 手册文本 (带 [p页码] 标记)
└── handbook_text.txt             # 手册纯文本
```

## 快速开始

```bash
# 后端
cd /root/bighomework
mvn clean package -DskipTests
java -jar target/campus-checkin-assistant-1.0-SNAPSHOT.jar --spring.profiles.active=local

# 前端开发
cd frontend
npm install
npm run dev

# 前端构建部署
npm run build
cp -r dist/* ../src/main/resources/static/
mvn package -DskipTests
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `SPRING_DATASOURCE_PASSWORD` | MySQL 密码 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DASHSCOPE_API_KEY` | DashScope (阿里云) API Key |
| `BAIDU_API_KEY` / `BAIDU_SECRET_KEY` | 百度AI 人脸识别 |

## RAG 搜索算法

学生手册问答采用关键词 n-gram 匹配 + 组合加分：

1. 从用户问题中提取关键词（去虚词后 ≥2字的片段）
2. 逐行匹配手册文本，按 `count × len²` 计分
3. **组合加分**：同时命中多个关键词的行获得 `score × matched²` 指数级加成
4. 按分数降序取 Top 5，合并同页内容
5. 将上下文交给 AI 生成正式回答（要求引用页码）

## 许可证

MIT License
