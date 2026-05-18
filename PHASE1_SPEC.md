# PHASE1_SPEC: 后端登录认证系统

## 1. 概述

为校园寝室自助签到系统添加完整的后端登录认证系统，包括学生实体、记住登录令牌、登录/登出 API、拦截器自动登录，以及登录页面占位。不引入 Spring Security，使用 Spring HandlerInterceptor 实现。

---

## 2. 文件清单

### 2.1 新建文件 (8 个 Java 文件 + 1 个 HTML 文件)

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `src/main/java/org/example/entity/StudentEntity.java` | 学生实体 (不继承 BaseEntity) |
| 2 | `src/main/java/org/example/entity/RememberTokenEntity.java` | 记住登录令牌实体 (继承 BaseEntity) |
| 3 | `src/main/java/org/example/repository/StudentRepository.java` | 学生数据仓库 |
| 4 | `src/main/java/org/example/repository/RememberTokenRepository.java` | 令牌数据仓库 |
| 5 | `src/main/java/org/example/service/AuthService.java` | 认证服务接口 |
| 6 | `src/main/java/org/example/service/impl/AuthServiceImpl.java` | 认证服务实现 |
| 7 | `src/main/java/org/example/controller/AuthController.java` | 认证 REST 控制器 |
| 8 | `src/main/java/org/example/interceptor/AuthInterceptor.java` | 登录拦截器 |
| 9 | `src/main/java/org/example/config/WebConfig.java` | WebMvc 配置 (注册拦截器) |
| 10 | `src/main/resources/templates/login.html` | 登录页面占位 |

### 2.2 修改文件 (3 个)

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `pom.xml` | 添加 jjwt 依赖, 添加 spring-security-crypto 依赖 |
| 2 | `src/main/resources/application.properties` | 添加 JWT secret 配置 |
| 3 | `src/main/java/org/example/controller/PageController.java` | 修改 "/" 路由增加登录检查 |

---

## 3. 数据库变更 (DDL)

### 3.1 新建 student 表

JPA 会自动建表 (ddl-auto 默认行为), 对应 Entity 如下:

```sql
CREATE TABLE student (
    student_id       VARCHAR(20)  NOT NULL COMMENT '学生id',
    student_name     VARCHAR(50)  DEFAULT NULL COMMENT '学生姓名',
    password         VARCHAR(100) DEFAULT NULL COMMENT '密码(BCrypt加密)',
    dorm_building    VARCHAR(50)  DEFAULT NULL COMMENT '宿舍楼栋',
    room_number      VARCHAR(20)  DEFAULT NULL COMMENT '寝室号',
    face_image_url   VARCHAR(500) DEFAULT NULL COMMENT '人脸图片URL',
    created_at       DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';
```

### 3.2 新建 remember_token 表

JPA 会自动建表, 对应 Entity 如下:

```sql
CREATE TABLE remember_token (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    student_id    VARCHAR(20)  DEFAULT NULL COMMENT '学生id',
    token         VARCHAR(255) DEFAULT NULL COMMENT '令牌(UUID字符串)',
    expires_at    DATETIME     DEFAULT NULL COMMENT '过期时间',
    created_date  DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_date  DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记住登录令牌表';
```

---

## 4. 各文件详细规格

### 4.1 StudentEntity

- **包名**: `org.example.entity`
- **类名**: `StudentEntity`
- **注解**: `@Entity`, `@Table(name = "student")`, `@Data`, `@Comment("学生表")`
- **注意**: **不继承 BaseEntity**，使用 `studentId` 作为业务主键

**字段清单**:

| 字段名 | Java类型 | 数据库列名 | 注解 | 说明 |
|-------|---------|-----------|------|------|
| studentId | String | student_id | `@Id`, `@Comment("学生id")` | 业务主键 VARCHAR(20) |
| studentName | String | student_name | `@Comment("学生姓名")` | VARCHAR(50) |
| password | String | password | `@Comment("密码(BCrypt加密)")` | VARCHAR(100) |
| dormBuilding | String | dorm_building | `@Comment("宿舍楼栋")` | VARCHAR(50) |
| roomNumber | String | room_number | `@Comment("寝室号")` | VARCHAR(20) |
| faceImageUrl | String | face_image_url | `@Comment("人脸图片URL")` | VARCHAR(500) |
| createdAt | Date | created_at | `@Comment("创建时间")` | DATETIME |

**代码模板**:
```java
package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Table(name = "student")
@Entity
@Data
@Comment("学生表")
public class StudentEntity {

    @Id
    @Comment("学生id")
    private String studentId;

    @Comment("学生姓名")
    private String studentName;

    @Comment("密码(BCrypt加密)")
    private String password;

    @Comment("宿舍楼栋")
    private String dormBuilding;

    @Comment("寝室号")
    private String roomNumber;

    @Comment("人脸图片URL")
    private String faceImageUrl;

    @Comment("创建时间")
    private Date createdAt;
}
```

### 4.2 RememberTokenEntity

- **包名**: `org.example.entity`
- **类名**: `RememberTokenEntity`
- **注解**: `@Entity`, `@Table(name = "remember_token")`, `@Data`, `@EqualsAndHashCode(callSuper = true)`, `@Comment("记住登录令牌")`
- **继承**: `BaseEntity` (自动获得 `id`, `createdDate`, `updatedDate`)

**新增字段 (除继承字段外)**:

| 字段名 | Java类型 | 数据库列名 | 注解 | 说明 |
|-------|---------|-----------|------|------|
| studentId | String | student_id | `@Comment("学生id")` | VARCHAR(20) |
| token | String | token | `@Comment("令牌")` | VARCHAR(255), UUID 字符串 |
| expiresAt | Date | expires_at | `@Comment("过期时间")` | DATETIME |

**代码模板**:
```java
package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Table(name = "remember_token")
@Entity
@Data
@Comment("记住登录令牌")
public class RememberTokenEntity extends BaseEntity {

    @Comment("学生id")
    private String studentId;

    @Comment("令牌")
    private String token;

    @Comment("过期时间")
    private Date expiresAt;
}
```

### 4.3 StudentRepository

- **包名**: `org.example.repository`
- **类名**: `StudentRepository`
- **继承**: `JpaRepository<StudentEntity, String>` (主键类型为 String, 不是 Long, 因此不能继承 BaseRepository<T>)
- **注意**: 不继承 BaseRepository, 因为 BaseRepository 绑定的是 `CrudRepository<T, Long>`

**方法签名**:
```java
package org.example.repository;

import org.example.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentEntity, String> {
    // 不需要额外方法, studentId 即主键, 使用内置 findById 即可
}
```

### 4.4 RememberTokenRepository

- **包名**: `org.example.repository`
- **类名**: `RememberTokenRepository`
- **继承**: 可直接继承 `JpaRepository<RememberTokenEntity, Long>`
- **注意**: 不继承 BaseRepository 也可以; 若要遵循项目惯例, 可继承 BaseRepository<RememberTokenEntity>, 因为 RememberTokenEntity extends BaseEntity (主键为 Long)

**推荐方案**: 继承 BaseRepository, 保持 project style 一致:

```java
package org.example.repository;

import org.example.entity.RememberTokenEntity;

import java.util.Optional;

public interface RememberTokenRepository extends BaseRepository<RememberTokenEntity> {

    // 根据 token 字符串查找
    Optional<RememberTokenEntity> findByToken(String token);

    // 根据 studentId 查找
    Optional<RememberTokenEntity> findByStudentId(String studentId);

    // 根据 studentId 删除所有 token (登出时清除)
    void deleteByStudentId(String studentId);
}
```

### 4.5 AuthService (接口)

- **包名**: `org.example.service`
- **类名**: `AuthService`

**方法签名**:
```java
package org.example.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.entity.StudentEntity;

import java.util.Map;

public interface AuthService {

    /**
     * 登录验证
     * @param studentId 学号
     * @param password 密码(明文)
     * @param rememberMe 是否记住登录
     * @param session HTTP Session
     * @param response HTTP Response (用于设置 cookie)
     * @return Map {success: boolean, message: String, studentId: String}
     */
    Map<String, Object> login(String studentId, String password, boolean rememberMe,
                               HttpSession session, HttpServletResponse response);

    /**
     * 登出: 清除 session 和 remember-token cookie, 删除 DB 中的 token
     * @param session HTTP Session
     * @param response HTTP Response
     */
    void logout(HttpSession session, HttpServletResponse response);

    /**
     * 检查登录状态 (供 GET /auth/check 调用)
     * @param session HTTP Session
     * @return Map {loggedIn: boolean, studentId: String, studentName: String}
     */
    Map<String, Object> check(HttpSession session);

    /**
     * 根据 remember_token cookie 值验证并获取学生信息 (供拦截器调用)
     * @param token UUID 字符串
     * @return StudentEntity 或 null
     */
    StudentEntity validateRememberToken(String token);
}
```

### 4.6 AuthServiceImpl

- **包名**: `org.example.service.impl`
- **类名**: `AuthServiceImpl`
- **注解**: `@Slf4j`, `@Service`

**依赖注入**:
- `@Autowired StudentRepository studentRepository`
- `@Autowired RememberTokenRepository rememberTokenRepository`

**核心逻辑**:

1. **login 方法**:
   - 从 `studentRepository.findById(studentId)` 查学生
   - 若不存在: 返回 `{success: false, message: "学号或密码错误"}`
   - 使用 `BCryptPasswordEncoder` 验证密码
   - 若密码不匹配: 返回 `{success: false, message: "学号或密码错误"}`
   - 若成功: `session.setAttribute("studentId", studentId)`; `session.setAttribute("studentName", studentName)`
   - 若 `rememberMe == true`:
     - 生成 `String token = UUID.randomUUID().toString()`
     - 创建 `RememberTokenEntity`:
       - `studentId`: 学生ID
       - `token`: UUID 字符串
       - `expiresAt`: `new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)` (30天)
     - `rememberTokenRepository.save(entity)`
     - 设置 HttpOnly Cookie:
       - Cookie name: `remember_token`
       - value: UUID 字符串
       - maxAge: 30 * 24 * 60 * 60 秒
       - path: `/`
       - httpOnly: true
       - 通过 `response.addCookie(cookie)` 写入
   - 返回 `{success: true, message: "登录成功", studentId: studentId}`

2. **logout 方法**:
   - 从 session 获取 `studentId`
   - 若存在: `rememberTokenRepository.deleteByStudentId(studentId)`
   - `session.invalidate()`
   - 清除 cookie: 创建同名 cookie, setMaxAge(0), Path("/"), addCookie

3. **check 方法**:
   - 从 session 获取 `studentId` 和 `studentName`
   - 若 studentId 不存在: 返回 `{loggedIn: false, studentId: null, studentName: null}`
   - 若存在: 返回 `{loggedIn: true, studentId: "xxx", studentName: "xxx"}`

4. **validateRememberToken 方法** (供拦截器用):
   - `rememberTokenRepository.findByToken(token)` 查找
   - 若不存在或已过期 (`entity.getExpiresAt().before(new Date())`): 删除该 token, 返回 null
   - 若有效: 从 `studentRepository.findById(entity.getStudentId())` 查学生, 返回 `StudentEntity`

**BCrypt 密码编码器**:
- 使用 `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
- 在 AuthServiceImpl 中创建实例: `private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();`
- 登录时: `passwordEncoder.matches(rawPassword, student.getPassword())`

### 4.7 AuthController

- **包名**: `org.example.controller`
- **类名**: `AuthController`
- **注解**: `@RestController`, `@RequestMapping("/auth")`

**依赖注入**:
- `@Autowired AuthService authService`

**端点**:

#### POST /auth/login
```java
@PostMapping("/login")
public Map<String, Object> login(@RequestBody Map<String, Object> body,
                                  HttpSession session,
                                  HttpServletResponse response) {
    String studentId = (String) body.get("studentId");
    String password = (String) body.get("password");
    Boolean rememberMe = (Boolean) body.getOrDefault("rememberMe", false);
    return authService.login(studentId, password, rememberMe, session, response);
}
```

- **请求体** (JSON): `{"studentId": "2024001", "password": "123456", "rememberMe": true}`
- **响应体** (JSON): `{"success": true, "message": "登录成功", "studentId": "2024001"}`

#### POST /auth/logout
```java
@PostMapping("/logout")
public Map<String, String> logout(HttpSession session, HttpServletResponse response) {
    authService.logout(session, response);
    return Map.of("success", "true", "message", "已登出");
}
```

- **响应体** (JSON): `{"success": "true", "message": "已登出"}`

#### GET /auth/check
```java
@GetMapping("/check")
public Map<String, Object> check(HttpSession session) {
    return authService.check(session);
}
```

- **响应体** (JSON): `{"loggedIn": true, "studentId": "2024001", "studentName": "张三"}`

### 4.8 AuthInterceptor

- **包名**: `org.example.interceptor`
- **类名**: `AuthInterceptor`
- **实现**: `org.springframework.web.servlet.HandlerInterceptor`
- **注解**: `@Slf4j`

**依赖注入**:
- `@Autowired AuthService authService`

**核心逻辑** (`preHandle` 方法):

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    HttpSession session = request.getSession();

    // 1. 检查 session 是否已登录
    if (session.getAttribute("studentId") != null) {
        return true;  // 已登录, 放行
    }

    // 2. session 无登录态, 检查 cookie 中的 remember_token
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("remember_token".equals(cookie.getName())) {
                String tokenValue = cookie.getValue();
                if (tokenValue != null && !tokenValue.isBlank()) {
                    // 3. 验证 token
                    StudentEntity student = authService.validateRememberToken(tokenValue);
                    if (student != null) {
                        // 4. 自动登录: 刷新 session
                        session.setAttribute("studentId", student.getStudentId());
                        session.setAttribute("studentName", student.getStudentName());
                        log.info("Auto-login via remember token: {}", student.getStudentId());
                        return true;
                    } else {
                        // token 无效或过期, 清除 cookie
                        Cookie clearCookie = new Cookie("remember_token", "");
                        clearCookie.setPath("/");
                        clearCookie.setMaxAge(0);
                        response.addCookie(clearCookie);
                    }
                }
            }
        }
    }

    // 5. 未登录, 但放行请求 (由 Controller 决定是否重定向)
    // 注意: 这里不直接返回 false 或 sendRedirect, 只负责自动登录逻辑
    return true;
}
```

**关键设计点**:
- `preHandle` **始终返回 true** (放行所有请求)
- 拦截器的唯一职责是: 检查 cookie 中的 remember_token, 如果有效则自动登录 (写入 session)
- 访问控制由 PageController (对页面) 和未来的接口权限控制负责
- 这样不会破坏现有的 `/ai/*` 等无需登录即可使用的 API

### 4.9 WebConfig

- **包名**: `org.example.config`
- **类名**: `WebConfig`
- **注解**: `@Configuration`
- **实现**: `org.springframework.web.servlet.config.annotation.WebMvcConfigurer`

```java
package org.example.config;

import org.example.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/logout",
                        "/auth/check",
                        "/login.html",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                );
    }
}
```

**排除路径说明** (这些路径不需要自动登录检查):
- `/auth/login`, `/auth/logout`, `/auth/check` — 认证接口本身
- `/login.html` — 登录页面
- `/error` — Spring Boot 错误页面
- `/css/**`, `/js/**`, `/images/**`, `/favicon.ico` — 静态资源

### 4.10 login.html

- **路径**: `src/main/resources/templates/login.html`

**内容**: 一个极简的 HTML 登录页面，拥有完整功能的表单，提供 Phase 1 可用、Phase 3 美化的基础。

页面应包含:
- 标题: "寝室签到助手 — 登录"
- 表单字段: 学号 `<input type="text">`、密码 `<input type="password">`、记住我 `<input type="checkbox">`
- 登录按钮: 点击后 POST `/auth/login`
- 使用原生 JavaScript `fetch` 发送 JSON 请求
- 成功: 跳转到 `/` (首页)
- 失败: 显示错误消息
- 密码使用 BCrypt 存储 (后端处理, 前端透明)

**页面不需要华丽样式** (Phase 3 再美化), 但需要功能完整可用。可参考现有 `index.html` 中的 `.modal` 样式片段作为基础。

### 4.11 PageController 修改

- **文件**: `src/main/java/org/example/controller/PageController.java`

**改动内容**:

修改 `index()` 方法，增加 session 检查:

```java
@GetMapping("/")
public String index(HttpSession session) {
    // 检查登录状态 (AuthInterceptor 可能已完成自动登录)
    if (session.getAttribute("studentId") == null) {
        return "redirect:/login.html";
    }
    return "index";
}
```

- 注入 `HttpSession` 参数
- 若 session 中无 `studentId`, 302 重定向到 `/login.html`
- 若已登录, 正常返回 `index` 视图

---

## 5. pom.xml 变更

在 `<dependencies>` 内 (与现有依赖平级) 添加以下 4 个依赖:

### 5.1 spring-security-crypto (BCrypt 加密)

```xml
<!-- BCrypt 密码加密 (仅 crypto 模块, 不引入完整 Spring Security) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```
- 说明: Spring Boot Starter Parent 已管理版本, 无需指定 version
- 提供 `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`

### 5.2 jjwt 三件套 (Phase 3 JWT 使用, Phase 1 仅引入依赖)

```xml
<!-- JWT (Phase 3 使用, Phase 1 先行引入) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## 6. application.properties 变更

在文件末尾追加:

```properties
# ==================== Auth ====================
# JWT 密钥 (Phase 3 使用, Phase 1 预留)
jwt.secret=YourSuperSecretKeyForJWT_ChangeThisInProduction_MinLength32Bytes!
# Remember-Me Token 有效期 (天)
auth.remember-token.expire-days=30
```

---

## 7. 目录结构 (Phase 1 完成后)

```
src/main/java/org/example/
├── Application.java
├── aioutput/
├── aiservice/
├── aop/
├── bean/
├── config/
│   ├── ChatMemoryConfig.java
│   ├── RagConfig.java
│   └── WebConfig.java              ← NEW
├── controller/
│   ├── AiController.java
│   ├── AuthController.java         ← NEW
│   └── PageController.java         ← MODIFIED
├── entity/
│   ├── BaseEntity.java
│   ├── ChatHistoryEntity.java
│   ├── CheckInRecordEntity.java
│   ├── LeaveRequestEntity.java
│   ├── RememberTokenEntity.java    ← NEW
│   └── StudentEntity.java          ← NEW
├── interceptor/
│   └── AuthInterceptor.java        ← NEW
├── repository/
│   ├── BaseRepository.java
│   ├── ChatHistoryRepository.java
│   ├── CheckInRecordRepository.java
│   ├── LeaveRequestRepository.java
│   ├── RememberTokenRepository.java ← NEW
│   └── StudentRepository.java       ← NEW
├── service/
│   ├── AiChatService.java
│   ├── AuthService.java            ← NEW
│   └── impl/
│       ├── AiChatServiceImpl.java
│       └── AuthServiceImpl.java    ← NEW
├── tools/
├── util/
└── vo/
```

---

## 8. Session 约定

| Session Attribute | 类型 | 说明 |
|-------------------|------|------|
| studentId | String | 当前登录学生的学号 |
| studentName | String | 当前登录学生的姓名 |

---

## 9. Cookie 约定

| Cookie Name | Path | HttpOnly | Secure | MaxAge | 说明 |
|------------|------|----------|--------|--------|------|
| remember_token | / | true | false (开发环境) | 30天 | 存储 UUID 令牌, 用于自动登录 |

---

## 10. 密码编码约定

- **算法**: BCrypt (strength = 10, BCryptPasswordEncoder 默认值)
- **编码工具**: `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
- **存储格式**: `$2a$10$...` (60 字符的 BCrypt hash)
- **数据库字段**: `student.password VARCHAR(100)`
- **校验**: `passwordEncoder.matches(rawPassword, encodedPassword)`

---

## 11. Remember-Me 令牌生命周期

1. **创建**: 用户勾选 rememberMe 登录成功 → 生成 `UUID.randomUUID().toString()` → 存入 `remember_token` 表 → 写入 cookie
2. **使用**: AuthInterceptor 检测到 session 无登录态 + cookie 中有 `remember_token` → 查 DB 验证 → 有效则自动登录并刷新 session
3. **过期**: 30 天 (从 `expiresAt` 字段判断), `expiresAt` 在创建时设为 `now + 30天`
4. **失效**: 登出时删除 DB 记录 + 清除 cookie
5. **安全**: 未实现 token 轮换 (Phase 3 可考虑), 目前一个用户可能有多个 token (允许多设备)

---

## 12. 与现有系统的兼容性检查

| 系统功能 | 影响评估 | 说明 |
|---------|---------|------|
| AiController /ai/* | 无影响 | 拦截器仅做自动登录, 不限制访问 |
| PageController "/" | 修改 | 增加登录检查, 未登录重定向到 /login.html |
| CheckInRecordEntity.studentId | 兼容 | StudentEntity.studentId 同样为 VARCHAR(20) |
| BaseEntity 体系 | 兼容 | StudentEntity 不使用 BaseEntity, RememberTokenEntity 使用 |
| JPA Auditing | 兼容 | RememberTokenEntity extends BaseEntity 自动获得审计字段 |
| MyBatis-Plus | 无影响 | 新 Entity 仅使用 JPA |
| index.html (SPA) | 无影响 | 原有 SPA 仍可访问, 但访问前需先登录 (通过 PageController 检查) |
| LangChain4j | 无影响 | 未触及 AI 相关代码 |

---

## 13. 实现注意事项

1. **StudentEntity 不继承 BaseEntity**: 因为主键是 `studentId VARCHAR(20)` 而非 `Long id`. 必须单独使用 `@Id` 注解在 `studentId` 字段上.

2. **RememberTokenEntity 继承 BaseEntity**: 因为主键是 `Long id AUTO_INCREMENT`, 与 BaseEntity 完全匹配.

3. **StudentRepository 不能继承 BaseRepository**: 因为 `BaseRepository<T extends BaseEntity>` 绑定了 `CrudRepository<T, Long>`, 而 StudentEntity 的主键类型是 `String`. 应直接继承 `JpaRepository<StudentEntity, String>`.

4. **RememberTokenRepository 可以继承 BaseRepository**: 因为 `RememberTokenEntity extends BaseEntity`, 主键是 `Long`.

5. **拦截器始终返回 true**: `preHandle` 不应该阻断请求, 仅负责自动登录逻辑. 访问控制由各 Controller 自行处理.

6. **BCryptPasswordEncoder 无需 @Bean**: 直接在 AuthServiceImpl 中 new 一个实例即可, 避免引入不必要的全局配置.

7. **mvn clean compile**: 每次修改代码后必须执行 `mvn clean compile` 验证编译通过, 确保所有依赖和语法正确.

8. **现有功能不受影响**: 所有变更均为增量添加, 不删除任何现有代码, 不修改现有 API 签名 (仅修改 PageController.index 增加 session 检查).

---

## 14. 测试验证清单 (Phase 1 完成后由 Reviewer 检查)

- [ ] `mvn clean compile` 通过, 无编译错误
- [ ] 应用启动后 student 表和 remember_token 表自动创建
- [ ] POST /auth/login 使用正确密码返回 `success: true`
- [ ] POST /auth/login 使用错误密码返回 `success: false`
- [ ] GET /auth/check 未登录返回 `loggedIn: false`
- [ ] 登录后 GET /auth/check 返回 `loggedIn: true` 及正确的 studentId/studentName
- [ ] 勾选 rememberMe 登录后, cookie `remember_token` 被正确设置
- [ ] POST /auth/logout 后 session 清除, cookie 清除
- [ ] remember_token 表中记录正确 (token 为UUID格式, expiresAt 为30天后)
- [ ] 携带有效 remember_token cookie 访问 "/" 时自动登录
- [ ] 未登录访问 "/" 时重定向到 /login.html
- [ ] 已有功能 (AI 对话、签到等) 不受影响
- [ ] login.html 可正常渲染并可发起登录请求
