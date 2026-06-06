# 阶段1：后端 API 层重构 — 详细 SPEC

> **目标：** 将现有 Thymeleaf Controller 改造为面向 Vue 3 SPA 的 `/api/*` JSON RESTful API，统一响应格式，配置 CORS 跨域，更新拦截器和错误处理。
> **原则：** 原有 Controller 不删除（保持 Thymeleaf 向后兼容），新增 `Api*Controller` 并行提供 JSON API。

---

## 1. 总体架构变更概览

| 维度 | 现状 | 目标 |
|------|------|------|
| 响应格式 | `Map<String, Object>` 混合 `success/message/data` | `ApiResponse<T>` 统一 `{code, message, data}` |
| 路径前缀 | `/auth/*`, `/ai/*`, `/checkin/*`, `/face/*` | `/api/auth/*`, `/api/ai/*`, `/api/checkin/*`, `/api/face/*` |
| CORS | 无配置 | 允许 `localhost:5173` + 生产域名 |
| 拦截器 | 仅拦截非 `/api` 路径 | 同时拦截 `/api/**`，返回 JSON 401 |
| 分页查询 | `findByStudentId(studentId)` 返回 `List` | 新增 `Page` 重载支持分页 |
| AI 对话 | `Flux.just(同步字符串)` 伪流式 | 真正 SSE 流式输出 |

---

## 2. 需要创建的新类

### 2.1 `ApiResponse<T>` — 统一响应封装

**文件：** `src/main/java/org/example/common/ApiResponse.java`

```java
package org.example.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;       // 200=成功, 4xx=客户端错误, 5xx=服务端错误
    private String message; // 人类可读消息
    private T data;         // 业务数据(可为null)

    // --- 静态工厂方法 ---

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

**序列化输出示例：**
```json
{"code": 200, "message": "success", "data": {"studentId": "2021001", "studentName": "张三"}}
{"code": 401, "message": "未登录", "data": null}
{"code": 400, "message": "参数不完整", "data": null}
```

---

### 2.2 `CorsConfig` — CORS 跨域配置

**文件：** `src/main/java/org/example/config/CorsConfig.java`

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
                    .allowedOrigins(
                        "http://localhost:5173",       // Vite dev server
                        "https://check.kaminya.top"    // 生产域名
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)            // 允许携带 Cookie/Session
                    .maxAge(3600);                     // preflight 缓存1小时
            }
        };
    }
}
```

**要点：**
- 仅对 `/api/**` 生效，不影响原有 Thymeleaf 路由
- `allowCredentials(true)` 配合 Session-based 认证
- 需在生产环境 Nginx 也配置 CORS 头作为后备

---

### 2.3 `ApiAuthController` — 认证 API

**文件：** `src/main/java/org/example/controller/ApiAuthController.java`

**路径前缀：** `/api/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/auth/login` | 学号密码登录 | 否 |
| `POST` | `/api/auth/logout` | 登出 | 是 |
| `GET` | `/api/auth/check` | 检查登录状态 | 否* |
| `GET` | `/api/auth/profile` | 获取个人信息 | 是 |
| `PUT` | `/api/auth/profile` | 更新个人信息 | 是 |

> *`/api/auth/check` 未登录时返回 `{loggedIn: false}` 而非 401

#### 2.3.1 `POST /api/auth/login`

**请求体：**
```json
{
  "studentId": "2021001",     // 必填，学号
  "password": "123456",       // 必填，密码(明文，后端BCrypt验证)
  "rememberMe": true          // 可选，默认false
}
```

**验证规则：**
- `studentId` 不能为空/null
- `password` 不能为空/null
- 验证失败返回 `400`
- 学号不存在或密码错误返回 `401`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": "2021001",
    "studentName": "张三",
    "className": "计算机2101",
    "avatarUrl": ""
  }
}
```

**失败响应 (400/401)：**
```json
{"code": 400, "message": "学号和密码不能为空", "data": null}
{"code": 401, "message": "学号或密码错误", "data": null}
```

**副作用：**
- 写入 `HttpSession`：`studentId`, `studentName`, `className`, `phoneNumber`, `avatarUrl`
- 若 `rememberMe=true`：创建 `RememberTokenEntity`，设置 `remember_token` Cookie

**实现要点：**
- 复用现有 `AuthService` 的 `login()` 方法，但 Controller 层需将其 `Map` 返回值转换为 `ApiResponse` 格式
- 或新建 `ApiAuthService` / 直接在 Controller 中包装调用

#### 2.3.2 `POST /api/auth/logout`

**请求体：** 无

**响应 (200)：**
```json
{"code": 200, "message": "success", "data": null}
```

**副作用：**
- 清除 `RememberTokenEntity`
- `session.invalidate()`
- 清除 `remember_token` Cookie

#### 2.3.3 `GET /api/auth/check`

**请求体：** 无

**响应 — 已登录 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "loggedIn": true,
    "studentId": "2021001",
    "studentName": "张三",
    "className": "计算机2101",
    "phoneNumber": "13800138000",
    "avatarUrl": ""
  }
}
```

**响应 — 未登录 (200，非401)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "loggedIn": false,
    "studentId": null,
    "studentName": null,
    "className": null,
    "phoneNumber": null,
    "avatarUrl": null
  }
}
```

#### 2.3.4 `GET /api/auth/profile`

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": "2021001",
    "studentName": "张三",
    "className": "计算机2101",
    "phoneNumber": "13800138000",
    "avatarUrl": "",
    "dormBuilding": "3号楼",
    "roomNumber": "301"
  }
}
```

**错误 (401/404)：**
```json
{"code": 401, "message": "未登录", "data": null}
{"code": 404, "message": "学生不存在", "data": null}
```

#### 2.3.5 `PUT /api/auth/profile`

**请求体：**
```json
{
  "className": "计算机2102",    // 可选
  "phoneNumber": "13900139000", // 可选
  "avatarUrl": "https://..."   // 可选
}
```

**验证规则：**
- 必须已登录 (401)
- 仅更新请求体中提供的字段

**响应 (200)：**
```json
{"code": 200, "message": "success", "data": null}
```

---

### 2.4 `ApiCheckInController` — 签到 API

**文件：** `src/main/java/org/example/controller/ApiCheckInController.java`

**路径前缀：** `/api/checkin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `GET` | `/api/checkin/status` | 查询今日签到状态 | 是 |
| `POST` | `/api/checkin/verify` | 提交签到验证(人脸+GPS) | 是 |
| `GET` | `/api/checkin/records` | 分页查询签到记录 | 是 |

#### 2.4.1 `GET /api/checkin/status`

**查询今日是否已签到。**

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "checkedIn": true,
    "count": 1
  }
}
```

**实现要点：**
- 查询 `CheckInRecordRepository.findByStudentIdAndCheckTimeAfter(studentId, todayStart)`
- `todayStart` 为当天 00:00:00
- **需在 `CheckInRecordRepository` 中新增方法：**
  ```java
  List<CheckInRecordEntity> findByStudentIdAndCheckTimeAfter(String studentId, Date after);
  ```

#### 2.4.2 `POST /api/checkin/verify`

**提交人脸识别 + GPS定位进行签到。**

**请求体：**
```json
{
  "faceImageData": "data:image/jpeg;base64,/9j/4AAQ...",  // 必填，Base64人脸图片
  "latitude": 29.991316,                                     // 必填，纬度
  "longitude": 122.179503                                    // 必填，经度
}
```

**验证规则：**
- 必须已登录 (从 Session 获取 studentId，不信任请求体)
- `faceImageData` 不能为空
- `latitude` / `longitude` 不能为空
- 人脸识别通过 `FaceRecognitionService.verifyFace()`
- GPS 定位通过 `LocationVerificationService.verifyLocation()`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "faceVerified": true,
    "locationVerified": true,
    "checkedIn": true
  }
}
```

**失败响应 (400)：**
```json
{
  "code": 400,
  "message": "签到失败：人脸识别未通过; GPS不在范围内",
  "data": {
    "faceVerified": false,
    "locationVerified": false,
    "checkedIn": false
  }
}
```

**副作用：** 签到成功时创建 `CheckInRecordEntity` 记录

#### 2.4.3 `GET /api/checkin/records`

**分页查询当前用户的签到记录。**

**查询参数：**
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 0 | 页码(从0开始) |
| `size` | int | 10 | 每页条数 |

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "studentId": "2021001",
        "studentName": "张三",
        "roomNumber": "301",
        "dormBuilding": "3号楼",
        "status": "已签到",
        "locationInfo": "29.991316,122.179503",
        "checkTime": "2026-06-06T08:30:00.000+08:00",
        "createdDate": "2026-06-06T08:30:00.000+08:00"
      }
    ],
    "totalPages": 3,
    "totalElements": 25,
    "currentPage": 0,
    "size": 10
  }
}
```

**实现要点：**
- **需在 `CheckInRecordRepository` 中新增分页方法：**
  ```java
  Page<CheckInRecordEntity> findByStudentId(String studentId, Pageable pageable);
  ```
- 按 `checkTime` 降序排列

---

### 2.5 `ApiAiController` — AI 对话 API (SSE 流式)

**文件：** `src/main/java/org/example/controller/ApiAiController.java`

**路径前缀：** `/api/ai`

| 方法 | 路径 | 说明 | 认证 | Content-Type |
|------|------|------|------|-------------|
| `GET` | `/api/ai/chat-stream` | SSE 流式 AI 对话 | 是 | `text/event-stream` |
| `GET` | `/api/ai/chat-history` | 分页查询聊天历史 | 是 | `application/json` |
| `POST` | `/api/ai/clear-chat-history/{userId}` | 清除聊天历史 | 是 | `application/json` |
| `GET` | `/api/ai/check-in-records` | 查询签到记录(AI用) | 是 | `application/json` |
| `GET` | `/api/ai/leave-requests` | 查询请假记录(AI用) | 是 | `application/json` |

#### 2.5.1 `GET /api/ai/chat-stream` — SSE 流式对话

**查询参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | String | 是 | 用户消息内容 |
| `userId` | String | 否 | 用户ID，默认从 Session 获取 |

**响应：** `Content-Type: text/event-stream; charset=utf-8`

```
data: 你好
data: ，
data: 我是
data: AI
data: 签到
data: 助手
data: 。
data: [DONE]
```

**实现要点：**
- 当前 `AiChatService.chatStream()` 返回同步 `String`，不是真流式
- 需改造为返回 `Flux<String>` 真正流式输出，或保持同步但前端自行逐字展示
- **方案A（推荐）：** 保持现有 `chatStream()` 返回 `String`，Controller 层将完整字符串按字符/分句拆分为 `Flux<String>`，配合 `@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)`
- **方案B：** 重构 `AiChatService` 使用 `StreamingChatModel` 返回真正的 `Flux<String>`
- 前端通过 `EventSource` API 接收

**SSE 事件格式约定：**
- 每个 chunk：`data: <内容>\n\n`
- 结束标记：`data: [DONE]\n\n`
- 错误事件：`data: {"error": true, "message": "..."}\n\n`

#### 2.5.2 `GET /api/ai/chat-history`

**查询参数：**
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `userId` | String | 从Session获取 | 用户ID |
| `page` | int | 0 | 页码 |
| `size` | int | 20 | 每页条数 |
| `sort` | String | `id,desc` | 排序方式 |

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {"role": "0", "content": "我要请假", "createdDate": "2026-06-06 08:00:00"},
      {"role": "1", "content": "好的，请告诉我请假类型...", "createdDate": "2026-06-06 08:00:01"}
    ],
    "totalPages": 5,
    "totalElements": 100,
    "currentPage": 0,
    "size": 20
  }
}
```

**实现：** 复用现有 `AiChatService.queryChatHistory()`

#### 2.5.3 `POST /api/ai/clear-chat-history/{userId}`

**路径参数：** `userId` — 用户ID

**响应 (200)：**
```json
{"code": 200, "message": "success", "data": null}
```

**实现：** 复用现有 `AiChatService.clearChatHistory()`

#### 2.5.4 `GET /api/ai/check-in-records`

**查询参数：** `studentId` (必填)

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "studentId": "2021001",
      "studentName": "张三",
      "roomNumber": "301",
      "dormBuilding": "3号楼",
      "status": "已签到",
      "locationInfo": "29.991316,122.179503",
      "checkTime": "2026-06-06T08:30:00.000+08:00"
    }
  ]
}
```

#### 2.5.5 `GET /api/ai/leave-requests`

**查询参数：** `studentId` (必填)

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "studentId": "2021001",
      "studentName": "张三",
      "leaveType": "病假",
      "startTime": "2026-06-06T08:00:00.000+08:00",
      "endTime": "2026-06-07T18:00:00.000+08:00",
      "reason": "发烧需要休息",
      "auditStatus": "待审核",
      "auditorComment": ""
    }
  ]
}
```

---

### 2.6 `ApiFaceController` — 人脸 API

**文件：** `src/main/java/org/example/controller/ApiFaceController.java`

**路径前缀：** `/api/face`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| `POST` | `/api/face/register` | 人脸注册 | 是 |
| `POST` | `/api/face/detect` | 人脸检测(活体+搜索) | 是 |
| `GET` | `/api/face/status` | 查询人脸注册状态 | 是 |

#### 2.6.1 `POST /api/face/register`

**请求体：**
```json
{
  "imageData": "data:image/jpeg;base64,/9j/4AAQ..."   // 必填，Base64人脸图片
}
```

**验证规则：**
- 必须已登录
- `imageData` 不能为空
- 通过百度 AI 人脸检测 → 注册到百度人脸库 → 更新 `StudentEntity.faceImageUrl = "registered"`

**成功响应 (200)：**
```json
{"code": 200, "message": "success", "data": {"registered": true}}
```

**失败响应 (400/500)：**
```json
{"code": 400, "message": "缺少图片数据", "data": null}
{"code": 500, "message": "人脸注册失败，请确保图片中有清晰人脸", "data": {"registered": false}}
```

#### 2.6.2 `POST /api/face/detect`

**请求体：** 多帧活体检测模式
```json
{
  "frames": [
    "data:image/jpeg;base64,/9j/4AAQ...",  // 第1帧
    "data:image/jpeg;base64,/9j/4AAQ...",  // 第2帧
    "data:image/jpeg;base64,/9j/4AAQ..."   // 第3帧(建议5帧)
  ]
}
```

**请求体：** 单帧回退模式
```json
{
  "imageData": "data:image/jpeg;base64,/9j/4AAQ..."
}
```

**验证流程：**
1. 从 Session 获取 studentId
2. 若提供 `frames`(列表)：
   - 第一步：多帧活体检测 `baiduFaceService.verifyLivenessMultiFrame(frames)`
   - 第二步：用第一帧做人脸搜索 `baiduFaceService.searchFace(frames[0])`
   - 第三步：验证搜索结果 userId 与当前 studentId 一致
3. 若仅提供 `imageData`(单帧回退)：
   - 调用 `faceRecognitionService.verifyFace(studentId, imageData)`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "faceVerified": true,
    "confidence": 0.95,
    "livenessPassed": true
  }
}
```

**失败响应 (400)：**
```json
{
  "code": 400,
  "message": "活体检测未通过，请确保是本人在镜头前（非照片）",
  "data": {
    "faceVerified": false,
    "confidence": 0.0,
    "livenessPassed": false
  }
}
```

#### 2.6.3 `GET /api/face/status`

**查询当前用户是否已注册人脸。**

**响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "registered": true
  }
}
```

**实现：** 查询 `StudentEntity.faceImageUrl == "registered"`

---

## 3. 需要修改的现有类

### 3.1 `CheckInRecordRepository` — 新增分页和时间查询方法

**文件：** `src/main/java/org/example/repository/CheckInRecordRepository.java`

**新增方法：**
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 分页查询签到记录
Page<CheckInRecordEntity> findByStudentId(String studentId, Pageable pageable);

// 查询指定时间之后的签到记录(用于今日签到状态查询)
List<CheckInRecordEntity> findByStudentIdAndCheckTimeAfter(String studentId, Date after);
```

### 3.2 `LeaveRequestRepository` — 新增分页查询方法（可选）

**文件：** `src/main/java/org/example/repository/LeaveRequestRepository.java`

**新增方法：**
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

Page<LeaveRequestEntity> findByStudentId(String studentId, Pageable pageable);
```

### 3.3 `WebMvcConfig` / `WebConfig` — 拦截器路径更新

**文件：** `src/main/java/org/example/config/WebMvcConfig.java` 和 `src/main/java/org/example/config/WebConfig.java`

**变更：** 在 `excludePathPatterns` 中新增 `/api/auth/login` 和 `/api/auth/check`

```java
registry.addInterceptor(authInterceptor)
    .addPathPatterns("/**")
    .excludePathPatterns(
        "/auth/login", "/auth/logout",
        "/api/auth/login", "/api/auth/check",  // 新增
        "/login", "/login.html",
        "/error",
        "/css/**", "/js/**", "/images/**", "/static/**",
        "/favicon.ico"
    );
```

### 3.4 `AuthInterceptor` — 更新 API 识别逻辑

**文件：** `src/main/java/org/example/interceptor/AuthInterceptor.java`

**变更：**
1. `isExcluded()` 方法新增 `/api/auth/login` 和 `/api/auth/check`
2. `isApiRequest()` 方法新增 `/api/` 前缀判断
3. 未登录 API 响应改为 `ApiResponse` 格式

```java
private boolean isExcluded(String uri) {
    return uri.equals("/auth/login") || uri.equals("/auth/logout") || uri.equals("/login")
        || uri.equals("/api/auth/login") || uri.equals("/api/auth/check")   // 新增
        || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
        || uri.startsWith("/static/") || uri.equals("/favicon.ico");
}

private boolean isApiRequest(String uri) {
    return uri.startsWith("/api/")                                              // 新增：覆盖所有 /api 路径
        || uri.startsWith("/ai/") || uri.startsWith("/checkin/") || uri.startsWith("/face/")
        || uri.equals("/auth/check");
}
```

**未登录 API 响应改为：**
```java
response.setStatus(401);
response.setContentType("application/json;charset=UTF-8");
response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
```

### 3.5 `AiChatService` 接口 — 可选新增流式方法

**文件：** `src/main/java/org/example/service/AiChatService.java`

**可选新增：**
```java
import reactor.core.publisher.Flux;

// 真正的 SSE 流式对话（如需实现方案B）
Flux<String> streamChat(String userId, String studentName, String className, String message);
```

> 如果选择方案A（同步返回后拆分），则无需修改此接口。

---

## 4. 错误处理策略

### 4.1 全局错误码定义

| Code | 含义 | 使用场景 |
|------|------|----------|
| 200 | 成功 | 所有成功响应 |
| 400 | 请求参数错误 | 缺少必填参数、参数格式错误 |
| 401 | 未认证 | 未登录、Session过期 |
| 403 | 无权限 | 访问他人数据 |
| 404 | 资源不存在 | 学生不存在、记录不存在 |
| 500 | 服务端错误 | 人脸服务异常、数据库异常 |

### 4.2 全局异常处理器（建议新增）

**文件：** `src/main/java/org/example/common/GlobalExceptionHandler.java`

```java
package org.example.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "org.example.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("服务异常", e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

### 4.3 各 Controller 内错误处理原则

1. **认证检查统一前置：** 每个需认证的接口第一步检查 `session.getAttribute("studentId")`，为 null 时返回 `ApiResponse.error(401, "未登录")`
2. **参数校验即时返回：** 缺少必填参数时立即返回 `ApiResponse.error(400, "...")`，不做后续处理
3. **业务异常包装：** Service 层抛出的异常在 Controller 层捕获并转为 `ApiResponse.error()`
4. **不暴露内部细节：** 500 错误消息不包含堆栈信息、SQL 语句等敏感内容

---

## 5. 数据格式约定

### 5.1 日期时间格式

- **请求体中的日期：** `yyyy-MM-dd HH:mm` (如 `"2026-06-06 08:00"`)
- **响应中的日期：** ISO 8601 格式 `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` (如 `"2026-06-06T08:00:00.000+08:00"`)
- **聊天历史日期：** `yyyy-MM-dd HH:mm:ss` (如 `"2026-06-06 08:00:00"`)

> 使用 `@JsonFormat(pattern = "...", timezone = "GMT+8")` 注解控制序列化

### 5.2 Base64 图片数据格式

- 支持带 `data:image/xxx;base64,` 前缀的完整 Data URI
- 也支持纯 Base64 字符串（后端自动去除前缀）
- 最大建议尺寸：2MB（Base64 约 2.7MB）

### 5.3 分页参数约定

- `page`：从 0 开始
- `size`：默认 10，最大 100
- `sort`：格式 `field,direction`（如 `checkTime,desc`）

### 5.4 Session 存储字段

| Key | 类型 | 写入时机 |
|-----|------|----------|
| `studentId` | String | 登录成功 |
| `studentName` | String | 登录成功 |
| `className` | String | 登录成功 |
| `phoneNumber` | String | 登录成功 |
| `avatarUrl` | String | 登录成功 |

---

## 6. 文件清单汇总

### 6.1 新建文件

| 文件路径 | 说明 |
|----------|------|
| `src/main/java/org/example/common/ApiResponse.java` | 统一响应封装类 |
| `src/main/java/org/example/common/GlobalExceptionHandler.java` | 全局异常处理器 |
| `src/main/java/org/example/config/CorsConfig.java` | CORS 跨域配置 |
| `src/main/java/org/example/controller/ApiAuthController.java` | 认证 API |
| `src/main/java/org/example/controller/ApiCheckInController.java` | 签到 API |
| `src/main/java/org/example/controller/ApiAiController.java` | AI 对话 API |
| `src/main/java/org/example/controller/ApiFaceController.java` | 人脸 API |

### 6.2 修改文件

| 文件路径 | 变更内容 |
|----------|----------|
| `src/main/java/org/example/repository/CheckInRecordRepository.java` | 新增 `Page<T> findByStudentId(String, Pageable)` 和 `findByStudentIdAndCheckTimeAfter()` |
| `src/main/java/org/example/repository/LeaveRequestRepository.java` | 可选新增 `Page<T> findByStudentId(String, Pageable)` |
| `src/main/java/org/example/config/WebMvcConfig.java` | 拦截器排除 `/api/auth/login`, `/api/auth/check` |
| `src/main/java/org/example/config/WebConfig.java` | 同上 |
| `src/main/java/org/example/interceptor/AuthInterceptor.java` | 更新 `isExcluded()`/`isApiRequest()`，API 401 响应改为 JSON |

### 6.3 不变文件

| 文件路径 | 说明 |
|----------|------|
| `src/main/java/org/example/controller/AuthController.java` | 保留，Thymeleaf 向后兼容 |
| `src/main/java/org/example/controller/CheckInController.java` | 保留 |
| `src/main/java/org/example/controller/AiController.java` | 保留 |
| `src/main/java/org/example/controller/FaceRegisterController.java` | 保留 |
| `src/main/java/org/example/controller/PageController.java` | 保留 |
| `src/main/java/org/example/service/*.java` | 不变，复用现有 Service |
| `src/main/java/org/example/entity/*.java` | 不变 |
| `src/main/java/org/example/aiservice/*.java` | 不变 |
| `src/main/java/org/example/tools/*.java` | 不变 |
| `src/main/java/org/example/aop/*.java` | 不变 |

---

## 7. 实施顺序建议

| 步骤 | Task | 依赖 | 预计耗时 |
|------|------|------|----------|
| 1 | 1.1 创建 `ApiResponse` | 无 | 5min |
| 2 | 1.2 创建 `CorsConfig` | 无 | 5min |
| 3 | 1.3 创建 `ApiAuthController` | ApiResponse | 15min |
| 4 | 1.4 创建 `ApiCheckInController` + 修改 Repository | ApiResponse | 15min |
| 5 | 1.5 创建 `ApiAiController` | ApiResponse | 10min |
| 6 | 1.6 创建 `ApiFaceController` | ApiResponse | 10min |
| 7 | 修改 `AuthInterceptor` + `WebMvcConfig` | ApiAuthController | 10min |
| 8 | 创建 `GlobalExceptionHandler` | ApiResponse | 5min |
| 9 | 编译验证 + 冒烟测试 | 全部 | 15min |

**总计约 90 分钟。**

---

## 8. 验收标准

1. ✅ `mvn clean compile` 编译通过
2. ✅ 所有 `/api/*` 端点返回 `{"code": 200/4xx/5xx, "message": "...", "data": ...}` 格式
3. ✅ CORS 头正确：`Access-Control-Allow-Origin: http://localhost:5173`
4. ✅ 未登录访问 `/api/checkin/status` 返回 `{"code": 401, "message": "未登录"}`
5. ✅ `GET /api/auth/check` 未登录返回 `{loggedIn: false}` (非 401)
6. ✅ `POST /api/auth/login` 成功后 Session 包含 studentId
7. ✅ `GET /api/checkin/records?page=0&size=10` 返回分页数据
8. ✅ `GET /api/ai/chat-stream?message=你好&userId=111` 返回 SSE 流
9. ✅ 原有 Thymeleaf 页面功能不受影响
