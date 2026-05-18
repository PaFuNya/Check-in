你是 team lead。按 @architect → @coder → @reviewer 顺序执行。

=== 当前问题 ===
校园寝室自助签到系统需要增加登录功能、人脸识别签到、GPS定位验证。
当前项目是一个 Spring Boot 3.4.4 + LangChain4j + Thymeleaf 的项目。

=== Phase 1: 后端登录认证系统 ===

需求：
1. 新建 StudentEntity — 学生表 (student_id 主键, student_name, password, dorm_building, room_number, face_image_url, created_at)
2. 新建 RememberTokenEntity — 记住登录令牌表 (id, student_id, token, expires_at)
3. 新建 StudentRepository — JPA Repository
4. 新建 RememberTokenRepository — JPA Repository  
5. 新建 AuthService — 登录验证、生成记住token、校验token
6. 新建 AuthController — POST /auth/login, POST /auth/logout, GET /auth/check
7. 新建 AuthInterceptor — 拦截器，检查登录状态（从 session 或 cookie 中的 remember-token 读取）
8. 修改 PageController — 根路由 "/" 先检查登录状态，未登录跳转登录页
9. 新建 login.html — 登录页面（前端在 Phase 3 做，Phase 1 只需后端 API 和简单页面占位）
10. 在 pom.xml 中添加 jjwt 依赖（用于生成 token）
11. 在 application.properties 中添加 JWT secret 配置

数据库变更：
- 新增 student 表：student_id VARCHAR(20) PK, student_name VARCHAR(50), password VARCHAR(100), dorm_building VARCHAR(50), room_number VARCHAR(20), face_image_url VARCHAR(500), created_at DATETIME
- 新增 remember_token 表：id BIGINT AUTO_INCREMENT PK, student_id VARCHAR(20), token VARCHAR(255), expires_at DATETIME

API 设计：
- POST /auth/login — 请求体 {studentId, password, rememberMe: boolean}，返回 {success, message, studentId}
- POST /auth/logout — 清除 session 和 remember-token
- GET /auth/check — 检查当前登录状态，返回 {loggedIn, studentId, studentName}

Remember-Me 逻辑：
- 用户勾选 rememberMe 时，生成 UUID token 存入 remember_token 表，有效期 30 天
- token 写入 HTTP cookie（名称 remember_token，HttpOnly，Path=/）
- AuthInterceptor 先检查 session，session 无则检查 cookie 中的 token
- token 有效则自动登录并刷新 session

=== 你的任务 ===
第1步: @architect — 读代码、分析现有结构、输出 PHASE1_SPEC.md
第2步: @coder — 严格按 SPEC 实现
第3步: @reviewer — 对照 SPEC 逐条检查，输出 REVIEW_PHASE1.md
第4步: 最终汇报 PASS/FAIL

CRITICAL:
- 按顺序执行，不跳步，不越界
- architect 不写实现代码，coder 不质疑方案，reviewer 不改代码
- 每次 mvn clean compile 必须通过
- 保持现有功能不受影响
- Student entity 的 studentId 和现有 CheckInRecordEntity 的 studentId 一致
- 密码用 BCrypt 加密存储
- 使用 Spring 的 HandlerInterceptor 实现拦截，不用 Spring Security
