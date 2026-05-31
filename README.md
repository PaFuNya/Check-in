# 校园寝室自助签到系统

基于 Spring Boot + LangChain4j + Thymeleaf 的校园寝室自助签到系统。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.4.4, Java 17, LangChain4j, JPA/Hibernate, MySQL |
| 前端 | Thymeleaf, CSS (Glassmorphism), 原生 JavaScript |
| AI | DeepSeek API (对话), 百度AI (人脸识别+活体检测) |
| 地图 | 百度地图 API (GPS定位) |
| 部署 | Maven, nohup JAR, 1Panel 反向代理 |

## 功能特性

1. **学号密码登录** - 记住我30天免登录
2. **GPS定位签到** - 校内500米范围内验证
3. **人脸识别+活体检测** - 录视频5秒 + 抽帧 + 百度detect API
4. **AI智能助手** - 意图识别：请假报备/状态查询/规则问答/个人信息
5. **个人信息设置** - 班级/手机号/头像上传
6. **三模式主题切换** - 浅色/深色/跟随系统
7. **签到记录查询**

## 项目结构

```
src/main/java/org/example/
├── Application.java          # 启动类
├── config/                   # 配置 (WebMvc, ChatMemory, Rag)
├── controller/               # 控制器 (Auth, Ai, CheckIn, Face, Page)
├── entity/                   # 实体 (Student, CheckInRecord, LeaveRequest, ChatHistory)
├── repository/               # JPA Repository
├── service/                  # 服务层 (Auth, AiChat, FaceRecognition, Location)
├── service/impl/             # 服务实现
├── aiservice/                # LangChain4j AI Service 接口
├── aioutput/                 # AI 输出 DTO
├── aop/                      # AOP 切面 (ChatFlow)
├── tools/                    # AI Tools (ChatHistory, CheckInRecord, LeaveRequest)
├── interceptor/              # 拦截器 (AuthInterceptor)
└── vo/                       # 视图对象

src/main/resources/
├── templates/                # Thymeleaf 模板 (login, index, checkin)
├── getIntention.txt          # AI意图识别提示词
├── checkin.txt               # 签到提示词
├── leaveRequest.txt          # 请假提示词
└── application.properties    # 配置文件
```

## 快速启动

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/campus-checkin-assistant-1.0-SNAPSHOT.jar --server.port=8088

# 访问
http://localhost:8088/login
```

## 测试账号

| 学号 | 密码 | 姓名 |
|------|------|------|
| 2024001 | 123456 | 张三 |
| 2024002 | 123456 | 李四 |

## 架构设计

采用 LangChain4j AiService 架构，与老师参考项目对齐：

- 单次AI调用完成意图识别 + 回复生成
- @SystemMessage 加载提示词模板
- 模板变量注入用户信息 (studentId, studentName)
- 5类意图：请假报备、状态查询、规则问答、个人信息、其他
- @Tool 注解实现 AI 调用后端服务

### 与老师参考项目的对应关系

| 老师参考 (失物招领) | 本项目 (寝室签到) |
|----------------------|-------------------|
| 失物登记 | 签到登记 |
| 失物查询 | 签到记录查询 |
| 拾获登记 | 请假报备 |
| 意图识别(4类) | 意图识别(5类) |
| 用户ID设置 | 学号登录+个人信息 |

## 页面展示

### 登录页
支持学号密码登录，记住我30天免登录，三模式主题切换。

### 主页
圆形签到按钮(呼吸光晕动画)，AI助手对话，签到记录查询，个人信息设置(头像上传)。

### 签到页
GPS自动定位验证，人脸识别(录视频5秒+活体检测)，签到结果反馈。

## 开发团队

WhiteEmpties - 浙江国际海运职业技术学院 大数据技术专业
