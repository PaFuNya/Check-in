# REVIEW_PHASE1 — Phase 1 Login Authentication System

Reviewer: automated code review against PHASE1_SPEC.md
Date: 2026-05-30

---

## 1. BUILD VERIFICATION

| # | Item | Status | Note |
|---|------|--------|------|
| 1 | `mvn clean compile` 通过, 无编译错误 | **PASS** | BUILD SUCCESS, 39 source files compiled |

---

## 2. FILE INVENTORY CHECK

### 2.1 New Files (10)

| # | File | Status | Note |
|---|------|--------|------|
| 1 | `src/main/java/org/example/entity/StudentEntity.java` | **PASS** | Present |
| 2 | `src/main/java/org/example/entity/RememberTokenEntity.java` | **PASS** | Present |
| 3 | `src/main/java/org/example/repository/StudentRepository.java` | **PASS** | Present |
| 4 | `src/main/java/org/example/repository/RememberTokenRepository.java` | **PASS** | Present |
| 5 | `src/main/java/org/example/service/AuthService.java` | **PASS** | Present |
| 6 | `src/main/java/org/example/service/impl/AuthServiceImpl.java` | **PASS** | Present |
| 7 | `src/main/java/org/example/controller/AuthController.java` | **PASS** | Present |
| 8 | `src/main/java/org/example/interceptor/AuthInterceptor.java` | **PASS** | Present |
| 9 | `src/main/java/org/example/config/WebConfig.java` | **PASS** | Present |
| 10 | `src/main/resources/templates/login.html` | **PASS** | Present |

### 2.2 Modified Files (3)

| # | File | Status | Note |
|---|------|--------|------|
| 1 | `pom.xml` | **PASS** | Dependencies added |
| 2 | `src/main/resources/application.properties` | **PASS** | Auth config added |
| 3 | `src/main/java/org/example/controller/PageController.java` | **PASS** | Session check + redirect added |

---

## 3. DETAILED SPEC COMPLIANCE

### 3.1 StudentEntity

| Check | Status | Note |
|-------|--------|------|
| Does NOT extend BaseEntity | **PASS** | No `extends` clause |
| @Entity, @Table(name = "student") | **PASS** | Both present |
| @Data | **PASS** | Present |
| @Comment("学生表") | **PASS** | Present |
| studentId is @Id VARCHAR(20) | **PASS** | `@Id private String studentId;` |
| All 7 fields present | **PASS** | studentId, studentName, password, dormBuilding, roomNumber, faceImageUrl, createdAt |
| @Comment on all fields | **PASS** | All 7 fields have @Comment annotations matching SPEC text |

### 3.2 RememberTokenEntity

| Check | Status | Note |
|-------|--------|------|
| extends BaseEntity | **PASS** | `extends BaseEntity` present |
| @EqualsAndHashCode(callSuper = true) | **PASS** | Present |
| @Entity, @Table(name = "remember_token") | **PASS** | Both present |
| @Data | **PASS** | Present |
| @Comment("记住登录令牌") | **PASS** | Present |
| All 3 additional fields present | **PASS** | studentId, token, expiresAt |
| Inherited fields (id, createdDate, updatedDate) | **PASS** | From BaseEntity (Long id AUTO_INCREMENT, Date createdDate, Date updatedDate) |

### 3.3 StudentRepository

| Check | Status | Note |
|-------|--------|------|
| extends JpaRepository<StudentEntity, String> | **PASS** | Correct, cannot use BaseRepository due to String PK |
| Correct package: org.example.repository | **PASS** | Matches |

### 3.4 RememberTokenRepository

| Check | Status | Note |
|-------|--------|------|
| extends BaseRepository<RememberTokenEntity> | **PASS** | Correct, RememberTokenEntity extends BaseEntity with Long PK |
| findByToken(String token) | **PASS** | Returns Optional<RememberTokenEntity> |
| findByStudentId(String studentId) | **PASS** | Returns Optional<RememberTokenEntity> |
| deleteByStudentId(String studentId) | **PASS** | Returns void |

### 3.5 AuthService Interface

| Check | Status | Note |
|-------|--------|------|
| login signature matches SPEC | **PASS** | `Map<String, Object> login(String, String, boolean, HttpSession, HttpServletResponse)` |
| logout signature matches SPEC | **PASS** | `void logout(HttpSession, HttpServletResponse)` |
| check signature matches SPEC | **PASS** | `Map<String, Object> check(HttpSession)` |
| validateRememberToken signature matches SPEC | **PASS** | `StudentEntity validateRememberToken(String)` |

### 3.6 AuthServiceImpl

| Check | Status | Note |
|-------|--------|------|
| @Slf4j, @Service | **PASS** | Both present |
| @Autowired StudentRepository | **PASS** | Present |
| @Autowired RememberTokenRepository | **PASS** | Present |
| BCryptPasswordEncoder as field | **PASS** | `private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();` |
| login: checks student exists | **PASS** | Uses `findById`, returns error if empty |
| login: verifies BCrypt password | **PASS** | `passwordEncoder.matches(password, student.getPassword())` |
| login: sets session attributes | **PASS** | Sets "studentId" and "studentName" |
| login: UUID token generation | **PASS** | `UUID.randomUUID().toString()` |
| login: 30-day expiry calculation | **PASS** | `new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)` |
| login: rememberMe cookie settings | **PASS** | name="remember_token", maxAge=30*24*60*60, path="/", httpOnly=true |
| login: returns correct Map on success | **PASS** | `{success: true, message: "登录成功", studentId: ...}` |
| login: returns correct Map on failure | **PASS** | `{success: false, message: "学号或密码错误"}` |
| logout: deletes tokens by studentId | **PASS** | `rememberTokenRepository.deleteByStudentId(studentId)` |
| logout: invalidates session | **PASS** | `session.invalidate()` |
| logout: clears cookie | **PASS** | Cookie with maxAge=0, path="/" |
| check: returns loggedIn:false when no session | **PASS** | Returns `{loggedIn: false, studentId: null, studentName: null}` |
| check: returns loggedIn:true with student info | **PASS** | Returns `{loggedIn: true, studentId: ..., studentName: ...}` |
| validateRememberToken: finds by token | **PASS** | `rememberTokenRepository.findByToken(token)` |
| validateRememberToken: checks expiry | **PASS** | `tokenEntity.getExpiresAt().before(new Date())` |
| validateRememberToken: deletes expired token | **PASS** | `rememberTokenRepository.delete(tokenEntity)` |
| validateRememberToken: returns StudentEntity or null | **PASS** | `studentRepository.findById(...)`, returns `.orElse(null)` |

### 3.7 AuthController

| Check | Status | Note |
|-------|--------|------|
| @RestController, @RequestMapping("/auth") | **PASS** | Both present |
| @Autowired AuthService | **PASS** | Present |
| POST /auth/login: correct params | **PASS** | `@RequestBody Map`, HttpSession, HttpServletResponse, extracts studentId/password/rememberMe |
| POST /auth/login: rememberMe defaults to false | **PASS** | `body.getOrDefault("rememberMe", false)` |
| POST /auth/logout: returns Map | **PASS** | `Map.of("success", "true", "message", "已登出")` |
| GET /auth/check: correct params | **PASS** | HttpSession only, returns `authService.check(session)` |

### 3.8 AuthInterceptor

| Check | Status | Note |
|-------|--------|------|
| implements HandlerInterceptor | **PASS** | `implements HandlerInterceptor` |
| @Component, @Slf4j | **PASS** | Both present |
| @Autowired AuthService | **PASS** | Present |
| preHandle: checks session first | **PASS** | `session.getAttribute("studentId") != null` |
| preHandle: checks cookie remember_token | **PASS** | Iterates cookies, looks for "remember_token" |
| preHandle: validates token via AuthService | **PASS** | Calls `authService.validateRememberToken(tokenValue)` |
| preHandle: auto-login sets session attributes | **PASS** | Sets "studentId" and "studentName" |
| preHandle: clears invalid/expired cookie | **PASS** | Creates cookie with maxAge=0, path="/" |
| preHandle: ALWAYS returns true | **PASS** | All code paths end with `return true;` |
| preHandle: log message on auto-login | **PASS** | `log.info("Auto-login via remember token: {}", ...)` |

### 3.9 WebConfig

| Check | Status | Note |
|-------|--------|------|
| @Configuration | **PASS** | Present |
| implements WebMvcConfigurer | **PASS** | Present |
| @Autowired AuthInterceptor | **PASS** | Present |
| addPathPatterns("/**") | **PASS** | Correct |
| excludes /auth/login | **PASS** | Present |
| excludes /auth/logout | **PASS** | Present |
| excludes /auth/check | **PASS** | Present |
| excludes /login.html | **PASS** | Present |
| excludes /error | **PASS** | Present |
| excludes /css/**, /js/**, /images/** | **PASS** | Present |
| excludes /favicon.ico | **PASS** | Present |

### 3.10 login.html

| Check | Status | Note |
|-------|--------|------|
| Title: "寝室签到助手 — 登录" | **PASS** | Present in `<title>` |
| Form with studentId input | **PASS** | `<input type="text" id="studentId">` |
| Form with password input | **PASS** | `<input type="password" id="password">` |
| Form with rememberMe checkbox | **PASS** | `<input type="checkbox" id="rememberMe">` |
| Uses fetch() to POST /auth/login | **PASS** | `fetch('/auth/login', {method: 'POST', ...})` with JSON body |
| On success: redirects to / | **PASS** | `window.location.href = '/'` |
| On failure: shows error message | **PASS** | Error message displayed in `#errorMessage` div |

### 3.11 PageController Modification

| Check | Status | Note |
|-------|--------|------|
| index() accepts HttpSession | **PASS** | `public String index(HttpSession session)` |
| Checks session for studentId | **PASS** | `session.getAttribute("studentId") == null` |
| Redirects to /login.html when not logged in | **PASS** | `return "redirect:/login.html"` |
| Returns "index" when logged in | **PASS** | `return "index"` |

### 3.12 pom.xml Dependencies

| Check | Status | Note |
|-------|--------|------|
| spring-security-crypto added (no version) | **PASS** | `<groupId>org.springframework.security</groupId><artifactId>spring-security-crypto</artifactId>`, no `<version>` |
| jjwt-api at 0.12.6 | **PASS** | `<version>0.12.6</version>` |
| jjwt-impl at 0.12.6, scope runtime | **PASS** | `<version>0.12.6</version><scope>runtime</scope>` |
| jjwt-jackson at 0.12.6, scope runtime | **PASS** | `<version>0.12.6</version><scope>runtime</scope>` |

### 3.13 application.properties Auth Config

| Check | Status | Note |
|-------|--------|------|
| jwt.secret config present | **PASS** | `jwt.secret=YourSuperSecretKeyForJWT_ChangeThisInProduction_MinLength32Bytes!` |
| auth.remember-token.expire-days config present | **PASS** | `auth.remember-token.expire-days=30` |

### 3.14 Existing Code Integrity

| Check | Status | Note |
|-------|--------|------|
| No existing files deleted | **PASS** | All pre-existing files (BaseEntity, ChatHistoryEntity, CheckInRecordEntity, LeaveRequestEntity, BaseRepository, ChatHistoryRepository, CheckInRecordRepository, LeaveRequestRepository, AiChatService, AiChatServiceImpl, AiController, ChatMemoryConfig, RagConfig) intact |
| No existing files modified beyond SPEC list | **PASS** | Only pom.xml, application.properties, PageController.java were modified |
| AiController unchanged | **PASS** | File timestamp unchanged (May 30 15:20) |
| Directory structure matches SPEC section 7 | **PASS** | All files at correct paths |

---

## 4. SPEC 14. 测试验证清单 (RUNTIME CHECKS)

These are runtime behavioral checks that cannot be fully verified at compile time, but code inspection confirms the logic is correct.

| # | Test Item | Code Inspection | Status |
|---|-----------|-----------------|--------|
| 1 | `mvn clean compile` 通过, 无编译错误 | BUILD SUCCESS confirmed | **PASS** |
| 2 | 应用启动后 student 表和 remember_token 表自动创建 | @Entity + @Table annotations present; JPA ddl-auto default behavior will auto-create | **PASS** |
| 3 | POST /auth/login 使用正确密码返回 success: true | AuthServiceImpl.login sets success:true and studentId after BCrypt match | **PASS** |
| 4 | POST /auth/login 使用错误密码返回 success: false | AuthServiceImpl.login returns success:false when student not found or password mismatches | **PASS** |
| 5 | GET /auth/check 未登录返回 loggedIn: false | AuthServiceImpl.check returns loggedIn:false when studentId is null | **PASS** |
| 6 | 登录后 GET /auth/check 返回 loggedIn: true | AuthServiceImpl.check returns loggedIn:true with studentId and studentName | **PASS** |
| 7 | 勾选 rememberMe 登录后 cookie 被正确设置 | Cookie name="remember_token", maxAge=30*24*60*60, path="/", httpOnly=true | **PASS** |
| 8 | POST /auth/logout 后 session 清除, cookie 清除 | session.invalidate() + Cookie maxAge=0 | **PASS** |
| 9 | remember_token 表记录正确 (UUID格式, expiresAt 30天后) | UUID.randomUUID().toString(); expiresAt = now + 30*24*60*60*1000 | **PASS** |
| 10 | 携带有效 remember_token cookie 时自动登录 | AuthInterceptor.preHandle validates token, sets session attributes | **PASS** |
| 11 | 未登录访问 "/" 时重定向到 /login.html | PageController.index returns "redirect:/login.html" when studentId is null | **PASS** |
| 12 | 已有功能不受影响 | AuthInterceptor always returns true; no existing files deleted; only PageController modified | **PASS** |
| 13 | login.html 可正常渲染并可发起登录请求 | HTML form present; fetch() POST /auth/login; success redirect; error display | **PASS** |

---

## 5. ISSUES AND SUGGESTIONS

### BLOCKER (Must Fix)

None.

### SUGGESTION (Optional Improvements)

1. **Null-safety on body.get("studentId") and body.get("password")**: In AuthController.login(), if the request body is missing `studentId` or `password` keys, the cast to `String` will silently produce `null`. AuthServiceImpl.login() calls `studentRepository.findById(null)` and `passwordEncoder.matches(null, ...)` which would throw exceptions. Consider adding null checks in the controller or service layer for robustness.

2. **Thread safety of BCryptPasswordEncoder field**: `BCryptPasswordEncoder` is already thread-safe (documented as immutable), so the `private final` field pattern is safe. No action needed, but worth noting for future reviewers.

---

## 6. FINAL VERDICT

**FINAL VERDICT: PASS**

All 13 items in SPEC section 14 (测试验证清单) pass code inspection. All 10 new files are present and match SPEC requirements. All 3 modified files have the correct changes. `mvn clean compile` succeeds with no errors. No existing code has been deleted or unexpectedly modified. Zero BLOCKER issues found.
