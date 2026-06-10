# FULL_OPTIMIZE_SPEC.md — 校园寝室签到系统全面优化规范

> Generated: 2026-06-07
> Architect analysis based on reading ALL source files

---

## 目录
1. [Backend Changes](#1-backend-changes)
2. [Frontend Changes](#2-frontend-changes)
3. [API Key Config](#3-api-key-config)
4. [Auth Interceptor Fix](#4-auth-interceptor-fix)
5. [Frontend Design System](#5-frontend-design-system)
6. [Per-Page Redesign Spec](#6-per-page-redesign-spec)

---

## 1. Backend Changes

### 1.1 DELETE: `/root/bighomework/src/main/java/org/example/config/WebConfig.java`
**Action: DELETE this file entirely.**

**Issue:**
- `WebConfig.java` and `WebMvcConfig.java` are duplicate `WebMvcConfigurer` implementations both registering `AuthInterceptor`
- `WebConfig` excludes `/api/**` from the interceptor — meaning ALL API routes bypass authentication
- `WebMvcConfig` does NOT exclude `/api/**` — creating inconsistent behavior when both beans are loaded

**Resolution:** Delete `WebConfig.java`. All interceptor config will live in `WebMvcConfig.java` (see Section 4).

---

### 1.2 MODIFY: `/root/bighomework/src/main/java/org/example/config/WebMvcConfig.java`
**Full replacement code — see Section 4.**

---

### 1.3 MODIFY: `/root/bighomework/src/main/java/org/example/interceptor/AuthInterceptor.java`
**Issues found:**
1. Line 57: `return true;` — unauthenticated requests pass through without being stopped. The comment says "由 Controller 决定是否重定向", but this defeats the purpose of an interceptor. The controller's manual session checks are defensive but create security holes if any controller forgets to check.
2. The interceptor currently has no way to distinguish API requests (`/api/**`) from page requests — it treats all unauthenticated traffic the same.

**Required changes:**

1. Add constructor injection for `ApiKeyConfig` (or use `@Value`) to hold api prefix pattern.

2. On `preHandle()` when user is NOT authenticated:
   - If the request URI starts with `/api/` → return **HTTP 401 JSON** response and `false` (block the request):
     ```java
     response.setStatus(401);
     response.setContentType("application/json;charset=UTF-8");
     response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
     return false;
     ```
   - Otherwise (page request) → redirect to `/login`:
     ```java
     response.sendRedirect("/login");
     return false;
     ```

3. **Complete replacement of `preHandle()` method** (preserve the remember_token auto-login logic, only change the else/failure path):

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    HttpSession session = request.getSession();

    // 1. Check session
    if (session.getAttribute("studentId") != null) {
        return true;
    }

    // 2. Check remember_token cookie
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("remember_token".equals(cookie.getName())) {
                String tokenValue = cookie.getValue();
                if (tokenValue != null && !tokenValue.isBlank()) {
                    StudentEntity student = authService.validateRememberToken(tokenValue);
                    if (student != null) {
                        session.setAttribute("studentId", student.getStudentId());
                        session.setAttribute("studentName", student.getStudentName());
                        session.setAttribute("dormBuilding", student.getDormBuilding());
                        session.setAttribute("roomNumber", student.getRoomNumber());
                        session.setAttribute("faceRegistered", "registered".equals(student.getFaceImageUrl()));
                        log.info("Auto-login via remember token: {}", student.getStudentId());
                        return true;
                    } else {
                        Cookie clearCookie = new Cookie("remember_token", "");
                        clearCookie.setPath("/");
                        clearCookie.setMaxAge(0);
                        response.addCookie(clearCookie);
                    }
                }
            }
        }
    }

    // 3. NOT authenticated — decide based on request type
    String requestURI = request.getRequestURI();
    if (requestURI.startsWith("/api/")) {
        // API request: return 401 JSON
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
    } else {
        // Page request: redirect to login
        response.sendRedirect("/login");
    }
    return false;
}
```

**Note:** The `preHandle` method signature must add `throws Exception` because `response.getWriter().write()` throws `IOException` and `response.sendRedirect()` throws `IOException`.

---

### 1.4 MODIFY: `/root/bighomework/src/main/java/org/example/service/BaiduFaceService.java`
**Issue:** Lines 27-28 hardcode real Baidu Cloud API credentials:
```java
private static final String API_KEY = "pBlZfNVlhwgMAikW0WmgktM9";
private static final String SECRET_KEY = "bZhL8Y8SuVpvuEpswES2sclkExF48iiT";
```

**Required changes:**

1. Remove the `private static final String API_KEY` and `SECRET_KEY` fields.
2. Add constructor injection for `ApiKeyConfig`:
```java
private final ApiKeyConfig apiKeyConfig;

public BaiduFaceService(ApiKeyConfig apiKeyConfig) {
    this.apiKeyConfig = apiKeyConfig;
}
```
3. Replace all usages of `API_KEY` → `apiKeyConfig.getBaiduApiKey()`
4. Replace all usages of `SECRET_KEY` → `apiKeyConfig.getBaiduSecretKey()`

**Lines to change:**
- Delete lines 27-28 (the static final fields)
- Line 52: `API_KEY` → `apiKeyConfig.getBaiduApiKey()`, `SECRET_KEY` → `apiKeyConfig.getBaiduSecretKey()`

---

### 1.5 MODIFY: `/root/bighomework/src/main/java/org/example/config/RagConfig.java`
**Issue:** Line 19 hardcodes a real DashScope API key:
```java
private static final String API_KEY = "sk-33687bfd11ce4da6899e38acccc82f14";
```

**Required changes:**

1. Remove the `private static final String API_KEY` field (line 19).
2. Add constructor injection for `ApiKeyConfig`:
```java
private final ApiKeyConfig apiKeyConfig;

public RagConfig(ApiKeyConfig apiKeyConfig) {
    this.apiKeyConfig = apiKeyConfig;
}
```
3. In `embeddingModel()` bean method, replace `API_KEY` → `apiKeyConfig.getDashscopeApiKey()` (line 31).

---

### 1.6 MODIFY: `/root/bighomework/src/main/java/org/example/service/impl/AuthServiceImpl.java`
**Issues found:**
1. **`login()` method (lines 57-58):** Only sets `studentId` and `studentName` in session. Missing: `dormBuilding`, `roomNumber`, `faceRegistered` flag, `className`, `phoneNumber`, `avatarUrl`. The `ApiAuthController.login()` expects `className` and `avatarUrl` in the result but the service never puts them.

2. **`check()` method (lines 100-115):** Only returns `loggedIn`, `studentId`, `studentName`. Missing: `className`, `phoneNumber`, `avatarUrl`. The `ApiAuthController.check()` (line 79-80) accesses `result.get("className")`, `result.get("phoneNumber")`, `result.get("avatarUrl")` from the map, but they are never set.

3. **`updateProfile()` method (lines 150-166):** ACCEPTS `className` and `phoneNumber` parameters but ONLY saves `avatarUrl` via `student.setFaceImageUrl(avatarUrl)`. The `className` and `phoneNumber` are completely ignored. This means the Profile page's edit feature silently does nothing for class/phone fields.

4. **`getProfile()` method (lines 137-148):** Returns `studentId`, `studentName`, `dormBuilding`, `roomNumber`, `faceImageUrl` — but does NOT return `className` or `phoneNumber`. The StudentEntity does not seem to have `className` or `phoneNumber` fields (confirmed: StudentEntity has only `studentId`, `studentName`, `password`, `dormBuilding`, `roomNumber`, `faceImageUrl`, `createdAt`). This is a data model issue — `className` and `phoneNumber` are UI concepts without DB backing.

**Required changes:**

#### In `login()`:
Add all available student fields to both session and result map:
```java
// After session.setAttribute("studentName", student.getStudentName()); — add:
session.setAttribute("dormBuilding", student.getDormBuilding());
session.setAttribute("roomNumber", student.getRoomNumber());
session.setAttribute("faceRegistered", "registered".equals(student.getFaceImageUrl()));

// In result map, add:
result.put("dormBuilding", student.getDormBuilding());
result.put("roomNumber", student.getRoomNumber());
result.put("faceRegistered", "registered".equals(student.getFaceImageUrl()));
```

#### In `check()`:
Add all session attributes to the result map:
```java
result.put("dormBuilding", session.getAttribute("dormBuilding"));
result.put("roomNumber", session.getAttribute("roomNumber"));
result.put("faceRegistered", session.getAttribute("faceRegistered"));
```

#### In `getProfile()`:
Return all fields available in StudentEntity (current fields are sufficient for what the DB has). Note: `className` and `phoneNumber` are NOT in StudentEntity so they must either be NULL or the StudentEntity must be extended.

#### In `updateProfile()`:
Since `className` and `phoneNumber` don't exist in StudentEntity, two options:
- **Option A (recommended):** Add `className` and `phoneNumber` columns to the StudentEntity (requires DB migration)
- **Option B (workaround):** Only update `faceImageUrl` (avatar), document that className/phoneNumber require a DB schema change

For this SPEC, **Option A** is specified: add fields to StudentEntity and DB.

---

### 1.7 MODIFY: `/root/bighomework/src/main/java/org/example/entity/StudentEntity.java`
**Issue:** Missing `className` and `phoneNumber` fields that the frontend Profile page expects.

**Required changes:**
Add two new fields:
```java
@Comment("班级")
private String className;

@Comment("手机号")
private String phoneNumber;
```

**Note:** This requires a DB schema migration. JPA's `ddl-auto=update` will add the columns automatically, but review the production DB first.

---

### 1.8 MODIFY: `/root/bighomework/src/main/java/org/example/controller/ApiAuthController.java`
**Issue:** The `login()` method (lines 51-52) reads `className` and `avatarUrl` from the result map, but `AuthServiceImpl.login()` never puts `className` in the result. After fixing AuthServiceImpl, these reads will work.

**No changes needed** — the controller is correct; the fix is in AuthServiceImpl.

---

### 1.9 MODIFY: `/root/bighomework/src/main/resources/application.properties`
**Issue:** No properties exist for Baidu Face API and DashScope API keys. These keys are currently hardcoded in BaiduFaceService.java and RagConfig.java.

**Required changes:**
Add the following properties at the end of the file:
```properties
# ==================== External API Keys ====================
# Baidu AI Face Recognition
api.baidu.api-key=${BAIDU_API_KEY:changeme}
api.baidu.secret-key=${BAIDU_SECRET_KEY:changeme}

# DashScope (Alibaba Cloud) — for RAG embedding
api.dashscope.api-key=${DASHSCOPE_API_KEY:changeme}
```

---

### 1.10 MODIFY: `/root/bighomework/src/main/resources/application-local.properties`
**Issue:** Missing local overrides for Baidu and DashScope API keys.

**Required changes:**
Add:
```properties
# Baidu AI Face
api.baidu.api-key=pBlZfNVlhwgMAikW0WmgktM9
api.baidu.secret-key=bZhL8Y8SuVpvuEpswES2sclkExF48iiT

# DashScope
api.dashscope.api-key=sk-33687bfd11ce4da6899e38acccc82f14
```

---

## 2. Frontend Changes

### 2.1 `/root/bighomework/frontend/src/views/Login.vue`
**Issue analysis:**
- The login button IS visible (white text on blue gradient, adequate contrast). No CSS hide bug found.
- However, UX issues exist: the loading state text layout is confusing (spinner + "登录中..." but "登录" is hidden via v-else, fine for most cases; though if `authStore.loading` toggles rapidly, both might briefly vanish)
- The `handleLogin` function does not clear the error when retrying
- No Enter key handling for password field to submit form
- Form fields lack required attribute
- No focus outline for accessibility on some elements

**Specific issues to fix:**
1. **BUTTON TEXT BUG (line 128-132):** The current code shows:
```html
<button type="submit" class="login-btn" :disabled="authStore.loading">
  <span v-if="authStore.loading" class="spinner"></span>
  <span v-else>登录</span>
  <span v-if="authStore.loading">登录中...</span>
</button>
```
This means when loading: spinner + "登录中..." (OK), when not loading: "登录" (OK). But visually the button could be wider during loading. Convert to single conditional:
```html
<button type="submit" class="login-btn" :disabled="authStore.loading">
  <template v-if="authStore.loading">
    <span class="spinner"></span>
    <span>登录中...</span>
  </template>
  <span v-else>登录</span>
</button>
```

2. **Add `@keydown.enter`** on password input to submit form.

3. **Clear error on new attempt:** In `handleLogin()`, add `authStore.clearError()` at the start.

4. **Add `autofocus`** to studentId input.

**No major visual redesign needed** — this page already follows the design system well. Only fix the button text issue and add the UX improvements.

---

### 2.2 `/root/bighomework/frontend/src/views/HomeView.vue`
**Issues found:**
1. **Silent error handling (line 23):** `catch { /* silent */ }` — if the status API fails, the user sees "加载中" forever or incorrect status. Must show an error state or retry button.
2. **The check-in button breathing animation:** Continuous GSAP tl with `repeat: -1, yoyo: true` — this is good UX.
3. **Quick action cards:** The last section has `margin-top: auto` + `margin-bottom: 0` to push it to the bottom. This is in the `.home-section:last-child` rule but only applies because quick actions is the last section. This is intentional based on commit `d8bb189`.

**Required changes:**
1. Add error state handling: if `fetchStatus()` fails, show a warning icon + "签到状态获取失败" + retry button.
2. Ensure the check-in button's breathing animation is killed on unmount (already done with `onUnmounted`).
3. Add `cursor-pointer` to the check-in button (already has `cursor-pointer` in class list).
4. Add skeleton loading state for status card (instead of inline "--" text).

**Current design is acceptable** — the page already follows the glass-card design system. Only add error handling and loading states.

---

### 2.3 `/root/bighomework/frontend/src/views/CheckInView.vue`
**Issues found:**
1. **No issues with step flow** — the GPS → Face → Verify flow works logically.
2. **Camera stream not mirrored for user preview:** The video has `transform: scaleX(-1)` (line 312) which is correct for selfie mirroring. But the captured image will NOT be mirrored (canvas draws the raw video frame). This could cause face recognition mismatch if user is accustomed to mirrored preview. **This is a design choice** — document it but no change needed.
3. **GPS timeout is 15s** — adequate.
4. **No cancel button** during the process to go back home.

**Required changes:**
1. Add a "cancel/back" button to each step's title area to allow returning to home.
2. Add `aria-label` attributes to the capture button.
3. The step indicator should be clickable to navigate back to previous steps (only for completed steps).

---

### 2.4 `/root/bighomework/frontend/src/views/ChatView.vue`
**Issues found:**
1. **Typewriter effect + SSE streaming dual rendering:** The `typewriterEffect` function (line 51-70) and the SSE `onmessage` handler (line 102-113) both try to control the AI message content. The `onmessage` updates `msg.content` (raw accumulated text), then calls `typewriterEffect` to animate it. When the typewriter is behind, the `onmessage` content may already be updated. This means:
   - On SSE `[DONE]`, `finishStreaming()` kills the typewriter timeline and directly sets `innerHTML` (line 130)
   - There's a race: the typewriter might be animating while `onmessage` accumulates more text — the `formattedContent` in the bubble element may flash between partial typewriter text and the latest accumulated content.

   **Fix:** Remove the typewriter effect during streaming — just append each SSE character directly to the DOM. Use typewriter effect ONLY for loading historical messages. Or: use `requestAnimationFrame` batching to throttle DOM updates during SSE streaming.

2. **`formatMarkdown()` is called with `v-html` directly on user content** (line 226: `<div class="bubble-text">{{ msg.content }}</div>` for user messages is safe — it uses `{{ }}` which is text interpolation. But AI messages use `v-html` (line 225). The content goes through `formatMarkdown()` which does HTML escaping for `&`, `<`, `>` before converting markdown, so XSS is mitigated.)

3. **Chat history loading:** The code calls `/api/ai/chat-history?page=0&size=50&sort=id,asc` but this API endpoint exists on `AiController` at `/ai/chat-history`, not `/api/ai/chat-history`. This will 404. The correct endpoint should be `/ai/chat-history` but since the frontend is behind an Nginx proxy or the API path is different...

   Actually, let me check: `ApiAiController` is at `/api/ai` and has no `/chat-history` endpoint. `AiController` is at `/ai` and has `/chat-history`. BUT looking at `ChatView.vue` line 27: it calls `/api/ai/chat-history` — this path does NOT exist! The history endpoint is at `/ai/chat-history` (on `AiController`). This is a BUG.

   **Fix:** Change the URL to `/ai/chat-history` (but note: this goes through the old non-auth controller...). Or more properly, add a `/chat-history` endpoint to `ApiAiController`.

4. **Clear history:** Line 45 calls `/api/ai/clear-chat-history/${userId}` — same issue. The endpoint is at `/ai/clear-chat-history/{userId}` in `AiController`, not under `/api/ai/`.

**Required changes:**
1. Add `/chat-history` endpoint to `ApiAiController`.
2. Add `/clear-chat-history/{userId}` endpoint to `ApiAiController`.
3. Fix the typewriter/SSE race by simplifying: during live streaming, directly update DOM; use typewriter only for history load and initial AI response animation.
4. Ensure the user input textarea autoresize works correctly on mobile.

---

### 2.5 `/root/bighomework/frontend/src/views/RecordsView.vue`
**Issues found:**
1. **Status label mismatch (lines 50-58):** The `statusLabel()` function checks if status string contains "success", "verified", or "正常". But the backend saves status as `"已签到"` (line 139 in ApiCheckInController: `record.setStatus("已签到")`). The function will NOT match "已签到" against any of those strings. All records will show "异常" even for successful check-ins.

   **Fix:** Update `statusLabel()` and `isSuccess()` to handle the actual status values: `"已签到"`, `"正常"`.

2. **No back navigation** on the page itself (only a back button in header).

3. **Pagination total pages could be 0** — handle the edge case where there are no records but pagination shows "0/0".

**Required changes:**
1. Fix status label logic:
```javascript
function statusLabel(s) {
  const v = String(s || '')
  if (v === '已签到') return '已签到'
  if (v === '正常') return '正常'
  return '异常'
}
function isSuccess(s) {
  const v = String(s || '')
  return v === '已签到' || v === '正常'
}
```
2. Hide pagination when there are no pages.
3. The design is adequate — no major visual changes needed.

---

### 2.6 `/root/bighomework/frontend/src/views/ProfileView.vue`
**Issues found:**
1. **Missing dorm info display:** The backend `getProfile()` returns `dormBuilding`, `roomNumber`, `faceImageUrl` — but the frontend only shows `studentId`, `studentName`, `className`, `phoneNumber`. Dorm information should be displayed.

2. **Edit form only has className and phoneNumber** — cannot edit dorm info (by design, these are read-only).

3. **On successful save, `form.className` and `form.phoneNumber` are written to `profile.value`** (line 38-39), but since the backend doesn't actually save `className`/`phoneNumber` (see AuthServiceImpl bug), the save appears to work but data is lost on reload.

**Required changes:**
1. Add read-only display of `dormBuilding` and `roomNumber` to the profile body.
2. Add read-only display of `faceRegistered` status (whether face is registered).
3. After fixing AuthServiceImpl to save className/phoneNumber (see Section 1.6 + 1.7), the edit will actually persist.

---

### 2.7 `/root/bighomework/frontend/src/layouts/DefaultLayout.vue`
**Issues found:**
1. **The layout is good** — fixed navbar with glass effect, desktop+mobile responsive nav, clock display, theme toggle, logout.
2. **Mobile nav appears at top (under desktop nav)** — the nav-mobile section is INSIDE the nav-bar, which is fixed. This means on mobile, the nav-bar height increases to include both desktop and mobile nav. The `padding-top` adjusts accordingly (120px on mobile). This works but creates a tall navbar.
3. **No active user avatar/name** in the navbar — user identity is only shown in the mobile greeting.

**Required changes:**
1. Add student name display in the desktop navbar (between nav links and action buttons).
2. Ensure the mobile bottom-nav has a solid background (it currently uses the glass nav-bar parent but needs explicit background in case of transparent parent issues on some browsers).
3. The clock auto-updates every second — ensure the interval is cleaned up on unmount (already done with `onUnmounted` at line 74-76).

---

### 2.8 `/root/bighomework/frontend/src/router/index.js`
**Issues found:**
1. **Route guard (line 56-74):** The `beforeEach` guard checks auth state. If `requiresAuth && !authStore.isLoggedIn`, it redirects to `/login`. This is correct.
2. **However**, after the AuthInterceptor fix (Section 4), the backend will also enforce auth. If a page request hits the backend without session and the interceptor redirects to `/login` (which is a Spring redirect, not a Vue route change), there could be confusion. The frontend guard should handle it before the backend sees the request.

**No changes needed** — the frontend guard is already correct.

---

### 2.9 `/root/bighomework/frontend/src/stores/auth.js`
**Issues found:**
1. **`checkAuth()` calls `/api/auth/check`** — this endpoint will NOT be excluded from the auth interceptor (see Section 4). But `/api/auth/check` MUST be accessible without auth (it's how the frontend checks login status). This needs to be added to the interceptor's exclude list.

**Required change:**
Add `"/api/auth/check"` to the interceptor's exclude list (see Section 4).

---

### 2.10 `/root/bighomework/frontend/src/assets/main.css`
**Issues found:**
1. Tailwind CSS import but the project uses scoped CSS in Vue components, not Tailwind utility classes. The Tailwind import might not be necessary but is not harmful.
2. The design tokens are defined but only used in a few global classes (`.glass`, `.glass-card`).

**No changes needed** — the CSS is functioning as a design token source.

---

## 3. API Key Config

### NEW FILE: `/root/bighomework/src/main/java/org/example/config/ApiKeyConfig.java`

```java
package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * External API key configuration.
 * All keys are loaded from application.properties via spring boot configuration binding.
 * DO NOT hardcode real keys here — use environment variables in production.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "api")
public class ApiKeyConfig {

    /** Baidu AI Face Recognition API Key */
    private Baidu baidu = new Baidu();

    /** DashScope (Alibaba Cloud) RAG embedding API Key */
    private Dashscope dashscope = new Dashscope();

    @Getter
    @Setter
    public static class Baidu {
        private String apiKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Dashscope {
        private String apiKey;
    }
}
```

**Usage in BaiduFaceService.java:**
```java
// Replace direct field references:
// OLD: API_KEY → NEW: apiKeyConfig.getBaidu().getApiKey()
// OLD: SECRET_KEY → NEW: apiKeyConfig.getBaidu().getSecretKey()
```

**Usage in RagConfig.java:**
```java
// Replace direct field reference:
// OLD: API_KEY → NEW: apiKeyConfig.getDashscope().getApiKey()
```

**Corresponding application.properties entries:**
```properties
# ==================== External API Keys ====================
api.baidu.api-key=${BAIDU_API_KEY:changeme}
api.baidu.secret-key=${BAIDU_SECRET_KEY:changeme}
api.dashscope.api-key=${DASHSCOPE_API_KEY:changeme}
```

**Add to application-local.properties (DO NOT COMMIT real values):**
```properties
api.baidu.api-key=your_baidu_api_key_here
api.baidu.secret-key=your_baidu_secret_key_here
api.dashscope.api-key=your_dashscope_api_key_here
```

---

## 4. Auth Interceptor Fix

### 4.1 DELETE `/root/bighomework/src/main/java/org/example/config/WebConfig.java`
Remove the entire file.

### 4.2 REWRITE `/root/bighomework/src/main/java/org/example/config/WebMvcConfig.java`
Complete replacement:

```java
package org.example.config;

import org.example.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // Auth endpoints (must be accessible without login)
                        "/auth/login",
                        "/auth/logout",
                        "/auth/check",
                        "/api/auth/login",
                        "/api/auth/check",
                        // Login page (redirect target)
                        "/login",
                        "/login.html",
                        // Static resources
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        // Spring Boot error page
                        "/error",
                        // SPA index.html (served by PageController)
                        "/index.html"
                );
    }
}
```

### 4.3 REWRITE `/root/bighomework/src/main/java/org/example/interceptor/AuthInterceptor.java`
Complete replacement:

```java
package org.example.interceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.StudentEntity;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession();

        // 1. Check session
        if (session.getAttribute("studentId") != null) {
            return true;
        }

        // 2. Check remember_token cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("remember_token".equals(cookie.getName())) {
                    String tokenValue = cookie.getValue();
                    if (tokenValue != null && !tokenValue.isBlank()) {
                        StudentEntity student = authService.validateRememberToken(tokenValue);
                        if (student != null) {
                            session.setAttribute("studentId", student.getStudentId());
                            session.setAttribute("studentName", student.getStudentName());
                            session.setAttribute("dormBuilding", student.getDormBuilding());
                            session.setAttribute("roomNumber", student.getRoomNumber());
                            log.info("Auto-login via remember token: {}", student.getStudentId());
                            return true;
                        } else {
                            Cookie clearCookie = new Cookie("remember_token", "");
                            clearCookie.setPath("/");
                            clearCookie.setMaxAge(0);
                            response.addCookie(clearCookie);
                        }
                    }
                }
            }
        }

        // 3. Not authenticated — determine response by request type
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/")) {
            // API request: return 401 JSON, do NOT redirect
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        // Page request: redirect to login page
        response.sendRedirect("/login");
        return false;
    }
}
```

### 4.4 Exclude path rationale

| Path | Reason |
|------|--------|
| `/auth/login` | Thymeleaf login page form POST |
| `/auth/logout` | Thymeleaf logout |
| `/auth/check` | Thymeleaf session check |
| `/api/auth/login` | REST login API |
| `/api/auth/check` | REST session check (frontend auth guard) |
| `/login`, `/login.html` | Login page itself |
| `/static/**`, `/css/**`, `/js/**`, `/images/**` | Static assets |
| `/favicon.ico` | Browser favicon |
| `/error` | Spring Boot error page |
| `/index.html` | SPA entry point |

**Not excluded (now INTERCEPTED):**
- `/api/checkin/**` — requires auth, returns 401 if not logged in
- `/api/face/**` — requires auth, returns 401 if not logged in
- `/api/ai/**` — requires auth, returns 401 if not logged in
- `/api/auth/profile` — requires auth, returns 401 if not logged in
- `/api/auth/logout` — requires auth (must be logged in to log out; frontend sends `POST /api/auth/logout` only when logged in)
- `/checkin`, `/records`, `/chat`, `/profile` — page routes, will redirect to `/login`

---

## 5. Frontend Design System

### 5.1 Color Palette

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `--color-primary` | `#2563EB` | `#3B82F6` | Primary buttons, links, active states |
| `--color-primary-hover` | `#1D4ED8` | `#2563EB` | Button hover |
| `--color-primary-light` | `#EFF6FF` | `rgba(37,99,235,0.15)` | Light backgrounds, badges |
| `--color-secondary` | `#3B82F6` | `#60A5FA` | Gradients, secondary accents |
| `--color-success` | `#059669` | `#10B981` | Success states |
| `--color-success-light` | `#ECFDF5` | `rgba(5,150,105,0.15)` | Success backgrounds |
| `--color-error` | `#DC2626` | `#EF4444` | Error states |
| `--color-error-light` | `#FEF2F2` | `rgba(220,38,38,0.15)` | Error backgrounds |
| `--color-warning` | `#D97706` | `#F59E0B` | Warning states |
| `--color-bg` | `#F8FAFC` | `#0F172A` | Page background |
| `--color-surface` | `#FFFFFF` | `rgba(30,41,59,0.7)` | Card surfaces |
| `--color-text` | `#1E293B` | `#E2E8F0` | Primary text |
| `--color-text-muted` | `#475569` | `#94A3B8` | Secondary text |
| `--color-text-light` | `#94A3B8` | `#64748B` | Tertiary text |
| `--color-border` | `#E2E8F0` | `rgba(255,255,255,0.08)` | Borders, dividers |

**Gradient Backgrounds:**
- Page: `linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 40%, #F0FDFA 100%)`
- Dark page: `linear-gradient(135deg, #0F172A 0%, #1E1B4B 50%, #0F172A 100%)`
- Primary button: `linear-gradient(135deg, #2563EB, #3B82F6)`
- Success button: `linear-gradient(135deg, #059669, #10B981)`

### 5.2 Typography

| Level | Size | Weight | Line Height | Letter Spacing | Usage |
|-------|------|--------|-------------|----------------|-------|
| h1 | `1.5rem` (24px) | 800 | 1.3 | `-0.02em` | Page titles |
| h2 | `1.25rem` (20px) | 700 | 1.4 | `-0.01em` | Section titles |
| h3 | `1rem` (16px) | 700 | 1.4 | `0` | Card titles |
| body | `0.9375rem` (15px) | 400 | 1.6 | `0` | Body text |
| body-sm | `0.875rem` (14px) | 400 | 1.6 | `0` | Descriptions |
| caption | `0.8125rem` (13px) | 500 | 1.5 | `0` | Labels, hints |
| small | `0.75rem` (12px) | 500 | 1.5 | `0` | Tags, badges |
| tiny | `0.6875rem` (11px) | 500 | 1.4 | `0` | Mobile nav labels |

**Font family:** `'Inter', ui-sans-serif, system-ui, -apple-system, sans-serif`

### 5.3 Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | `4px` | Icon gaps, tight padding |
| `sm` | `8px` | Gap between related elements |
| `md` | `12px` | Card internal gap |
| `lg` | `16px` | Section gap, form field gap |
| `xl` | `20px` | Section margin |
| `2xl` | `24px` | Page padding, large gap |
| `3xl` | `32px` | Card padding, header margin |

### 5.4 Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `rounded-sm` | `4px` | Checkbox, small elements |
| `rounded-md` | `8px` | Inputs, small buttons |
| `rounded-lg` | `10px` | Buttons, action buttons |
| `rounded-xl` | `12px` | Form inputs, nav items |
| `rounded-2xl` | `16px` | Cards, modals |
| `rounded-full` | `9999px` | Badges, chips, avatar, circular buttons |

### 5.5 Shadows

| Token | Value | Usage |
|-------|-------|-------|
| `shadow-sm` | `0 1px 3px rgba(0,0,0,0.06)` | Subtle lift |
| `shadow-md` | `0 4px 12px rgba(37,99,235,0.3)` | Primary button |
| `shadow-lg` | `0 4px 24px rgba(0,0,0,0.06)` | Cards |
| `shadow-glow` | `0 0 32px rgba(37,99,235,0.35)` | Animated check-in button |

### 5.6 Component Specifications

#### Button: Primary
```css
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 24px;
  min-height: 44px;
  background: linear-gradient(135deg, #2563EB, #3B82F6);
  color: #FFFFFF;
  border: none;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
}
.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```

#### Button: Ghost
```css
.btn-ghost {
  padding: 10px 24px;
  min-height: 44px;
  background: #F1F5F9;
  color: #64748B;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover {
  background: #E2E8F0;
}
```

#### Button: Success
```css
.btn-success {
  /* Same as btn-primary but */
  background: linear-gradient(135deg, #059669, #10B981);
  box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
}
```

#### Button: Warning
```css
.btn-warning {
  /* Same as btn-primary but */
  background: linear-gradient(135deg, #D97706, #F59E0B);
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.3);
}
```

#### Card: Glass
```css
.glass-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
}
.dark .glass-card {
  background: rgba(30, 41, 59, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.25);
}
```

#### Input
```css
.field-input {
  width: 100%;
  padding: 12px 14px 12px 44px;  /* 44px left for icon */
  min-height: 44px;
  border: 1.5px solid #E2E8F0;
  border-radius: 12px;
  font-size: 0.9375rem;
  font-family: inherit;
  background: #F8FAFC;
  color: #1E293B;
  transition: all 0.2s;
  outline: none;
}
.field-input:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
  background: #FFFFFF;
}
.dark .field-input {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
  color: #E2E8F0;
}
.dark .field-input:focus {
  background: rgba(30, 41, 59, 0.8);
}
```

### 5.7 Animation Rules

| Element | Property | Duration | Easing |
|---------|----------|----------|--------|
| Page entrance | opacity, translateY | 500-600ms | `power3.out` |
| Card/list entrance | opacity, translateY | 350-450ms | `power2.out` or `back.out(1.3)` |
| Button hover | transform, box-shadow | 200ms | `ease` (default) |
| Theme toggle | background, color | 300ms | `ease` (default) |
| Step transitions | all | 300ms | `ease` (default) |
| Check-in breathing | scale, box-shadow | 1500ms (repeat) | `sine.inOut` |
| Spinner | rotate | 600-700ms | `linear` (infinite) |
| Typing cursor | opacity | 800ms | `step-end` (infinite) |

**GSAP usage:** Use `gsap.from()` or `gsap.timeline()` for entrance animations. Kill all timelines in `onUnmounted()`.

### 5.8 Dark Mode
- Toggle via `document.documentElement.classList.add/remove('dark')`
- Persist preference in `localStorage.setItem('theme', ...)`
- Use `:root.dark` selector prefix for all dark mode overrides
- Text contrast >= 4.5:1 in both modes (light text on dark: `#E2E8F0` on `#0F172A` = 13.4:1 OK)

### 5.9 Responsive Breakpoints

| Breakpoint | Target | Layout Changes |
|------------|--------|----------------|
| `375px` | Small phone | Single column, reduced padding, smaller buttons |
| `480px` | Phone | Actions grid 1 column, login card narrower |
| `640px` | Large phone | Nav clock visible |
| `768px` | Tablet | Desktop nav shows, mobile nav hides |
| `1024px` | Desktop | Max-width container |
| `1440px` | Large desktop | Extra padding |

### 5.10 Hard Rules (MUST be followed)

1. **Cursor:** Every interactive element MUST have `cursor: pointer` or `cursor-pointer` class.
2. **Touch target:** Minimum 44x44px for all interactive elements (buttons, links, inputs, checkboxes via label).
3. **Icons:** Use inline SVG only. NO emoji characters as icons. SVG stroke-width 1.5-2.5.
4. **Contrast:** Light mode text-on-background contrast ratio >= 4.5:1. Dark mode >= 3:1 for large text, >= 4.5:1 for body text.
5. **Focus:** All interactive elements must have a visible focus indicator (use `:focus-visible`).
6. **Disabled:** Disabled buttons must have `opacity: 0.6; cursor: not-allowed;`.
7. **Font:** All components inherit font-family from body. No hardcoded font-family in components.
8. **Reduced motion:** Honor `@media (prefers-reduced-motion: reduce)`.

---

## 6. Per-Page Redesign Spec

### 6.1 Login.vue

#### Layout Wireframe
```
+------------------------------------------+
|          [Orb 1]              [Orb 2]     |
|               [Orb 3]                     |
|                                          |
|         +--------------------+           |
|         |   [Logo Icon 64px] |           |
|         |   校园寝室签到      |           |
|         |   使用学号和密码...  |           |
|         |                    |           |
|         |   学号 [icon]      |           |
|         |   [_____________]  |           |
|         |                    |           |
|         |   密码 [icon]      |           |
|         |   [_____________]  |           |
|         |                    |           |
|         |   [] 记住我        |           |
|         |   [   登录   ]     |  <-- 48px tall, blue gradient
|         |                    |           |
|         +--------------------+           |
|         max-width: 420px                 |
+------------------------------------------+
```

#### Component Hierarchy
- `.login-page` (full viewport, gradient bg)
  - `.orb.orb-1` (decorative)
  - `.orb.orb-2` (decorative)
  - `.orb.orb-3` (decorative)
  - `.login-card.glass-card` (max-width 420px, padding 40px 36px, border-radius 20px)
    - `.login-header`
      - `.login-logo` (64x64, rounded-16, blue gradient bg, white SVG checkmark)
      - `h1.login-title` (24px, 800 weight, #1E293B)
      - `p.login-subtitle` (14px, #64748B)
    - `form.login-form` (flex column, gap 16px)
      - `.form-field` (flex column, gap 6px)
        - `label.field-label` (13px, 600 weight, #334155)
        - `.input-wrapper` (relative)
          - `span.input-icon` (absolute left 14px, #94A3B8)
          - `input.field-input` (padding 12px 14px 12px 44px, min-height 44px)
      - `.form-field` (password, same structure + @keydown.enter)
      - `.form-meta` (flex row)
        - `label.form-check` (flex, gap 8px, min-height 44px, cursor-pointer)
          - `input.check-input` (hidden, styled sibling)
          - `span.check-box` (18x18, border 1.5px, rounded 4px)
          - `span.check-label` (13px, #64748B)
      - `div.error-msg` (if error, flex row, red bg, red text)
      - `button.login-btn` (full width, 48px min-height, blue gradient, white text, 16px font)

#### Colors
- Page bg: `linear-gradient(135deg, #EFF6FF 0%, #E0E7FF 50%, #F0FDFA 100%)`
- Card: `rgba(255,255,255,0.8)` with blur
- Button: `linear-gradient(135deg, #2563EB, #3B82F6)`
- Orbs: `#818CF8` (purple), `#38BDF8` (cyan), `#34D399` (green) — blur 80px, opacity 0.4

#### Key CSS Rules
- `.login-btn`: min-height 48px, border-radius 12px, font-weight 600, cursor-pointer, transition all 0.2s
- `.login-btn:hover`: translateY(-1px), shadow expands
- `.login-btn:disabled`: opacity 0.6, cursor not-allowed
- `.field-input:focus`: border-color #2563EB, box-shadow 0 0 0 3px rgba(37,99,235,0.15)
- Mobile <480px: card padding 32px 24px

#### Animations
- GSAP timeline on mount: card y:60 → 0 (800ms), form fields stagger (450ms each)
- Orbs: floating animation 10s infinite ease-in-out

---

### 6.2 HomeView.vue

#### Layout Wireframe
```
+------------------------------------------+
| 早上好，张三                              |
|  2026年6月7日 星期日                      |
|                                          |
| +--------------------------------------+ |
| | 签到状态               [已签到/未签到] | |
| |                                      | |
| |   3          次     [clock] 今日签到完成| |
| +--------------------------------------+ |
|                                          |
|              +----------+                |
|              |          |                |
|              |  (check) |  <-- 128px circle
|              |   签到   |      breathing animation
|              +----------+                |
|           点击进行寝室签到                 |
|                                          |
| 快捷操作                                  |
| +----------------+ +------------------+  |
| | [icon]         | | [icon]           |  |
| | 签到打卡        | | 签到记录          |  |
| | 人脸+GPS定位    | | 查看历史签到       |  |
| +----------------+ +------------------+  |
| +----------------+ +------------------+  |
| | [icon] AI助手  | | [icon]           |  |
| | 智能问答与帮助  | | 个人信息          |  |
| +----------------+ +------------------+  |
+------------------------------------------+
  max-width: 640px, margin: 0 auto
```

#### Component Hierarchy
- `.home-page` (max-width 640px, flex column)
  - `section.greeting-section`
    - `h1.greeting-title` (24px, 800 weight)
    - `p.greeting-date` (13px, #64748B)
  - `section.status-card.glass-card` (padding 20px 24px, border-radius 16px)
    - `.status-top` (flex row, space-between)
      - `span.status-label` (13px, #64748B)
      - `span.status-badge` (pill, 12px font, 600 weight)
    - `.status-body` (flex row, space-between, baseline)
      - `.status-count` (flex, gap 4px)
        - `span.count-num` (36px, 800 weight, #1E293B)
        - `span.count-unit` (13px, #94A3B8)
      - `.status-hint` (flex, gap 6px, 13px, #94A3B8)
  - `section.checkin-action` (text-align center)
    - `button.checkin-btn` (128x128 circle, blue gradient, GSAP breathing)
      - SVG checkmark (36x36)
      - `span.checkin-text` (16px, 700 weight)
    - `p.checkin-hint` (13px, #94A3B8)
  - `section.home-section`
    - `h2.section-title` (16px, 700 weight)
    - `.actions-grid` (grid 2 columns, gap 12px)
      - `button.action-card.glass-card` (padding 20px 16px, flex column)
        - `.action-icon` (40x40, rounded 10px, bg #EFF6FF)
        - `span.action-name` (15px, 600 weight, #1E293B)
        - `span.action-desc` (12px, #94A3B8)

#### Colors
- Count number: `#1E293B` (dark) / `#F1F5F9` (light mode)
- Badge success: bg `#ECFDF5`, text `#059669`
- Badge error: bg `#FEF2F2`, text `#DC2626`
- Check-in button done: `linear-gradient(135deg, #059669, #10B981)`
- Action icon: bg `#EFF6FF`, color `#2563EB`; AI icon: bg `#ECFDF5`, color `#059669`

#### Responsive
- Mobile <480px: actions grid 1 column, check-in button 108x108

---

### 6.3 CheckInView.vue

#### Layout Wireframe (Step 1: GPS)
```
+------------------------------------------+
|      (1) -- GPS -- (2) -- 人脸 -- (3)     |
|                                          |
| +--------------------------------------+ |
| |            [map pin SVG 44px]        | |
| |            GPS 定位                  | |
| |        正在获取您的位置信息            | |
| |                                      | |
| |     [定位中...] 或 [定位成功]         | |
| |     纬度: 29.991316                  | |
| |     经度: 122.179503                 | |
| |     精度: 15m                        | |
| |                                      | |
| |     [下一步：人脸拍照]                | |
| +--------------------------------------+ |
+------------------------------------------+
```

#### Layout Wireframe (Step 2: Face)
```
+------------------------------------------+
|      (1) -- GPS -- (2) -- 人脸 -- (3)     |
|                                          |
| +--------------------------------------+ |
| |         [person SVG 44px]            | |
| |         人脸拍照                      | |
| |    请正对摄像头，确保面部清晰可见       | |
| |                                      | |
| |     +--------------------------+     | |
| |     |                          |     | |
| |     |    [camera video]        |     | |
| |     |    4:3 aspect, rounded   |     | |
| |     |                          |     | |
| |     +--------------------------+     | |
| |                                      | |
| |           (    )  <-- capture btn    | |
| |           ( ** )                     | |
| |                                      | |
| +--------------------------------------+ |
+------------------------------------------+
```

#### Layout Wireframe (Step 3: Result)
```
+------------------------------------------+
| +--------------------------------------+ |
| |        (success)  or  (fail)         | |
| |                                      | |
| |        签到成功！ / 签到失败           | |
| |        您已完成本次寝室签到            | |
| |                                      | |
| |    人脸识别:   通过 / 未通过          | |
| |    位置验证:   通过 / 未通过          | |
| |                                      | |
| |    [返回首页]      [重新签到]         | |
| +--------------------------------------+ |
+------------------------------------------+
```

#### Key Elements
- Steps indicator: 3 circles (36x36), connected by lines (48px x 2px)
- Active step: blue gradient bg, glow shadow
- Done step: green bg (#059669), checkmark
- Camera: max-width 340px, 4:3 aspect, rounded 16px, bg #1E293B, video scaleX(-1)
- Capture button: 72x72px outer, 64px ring (4px border), 48px dot (blue bg)
- Face preview image: max-width 300px, rounded 16px, border 2px solid #E2E8F0
- Result icons: 52x52 SVG, green/red drop-shadow glow

#### Colors
- Step card: glass-card, padding 32px 28px, text-align center
- Success result: #059669 with glow
- Failure result: #DC2626 with glow
- Capture ring: border #E2E8F0 → #2563EB on hover
- Capture dot: bg #2563EB

---

### 6.4 ChatView.vue

#### Layout Wireframe
```
+------------------------------------------+
| +--------------------------------------+ |
| | [<] AI 签到助手    [online dot] [x]  | |  <- header 54px
| +--------------------------------------+ |
| |                                      | |
| |  (if empty: welcome screen)          | |
| |  [AI icon 72px]                      | |
| |  欢迎使用 AI 签到助手                  | |
| |  我可以帮你完成寝室签到...             | |
| |                                      | |
| |  [今天签到了吗？] [我要请假] [条例]   | |
| |                                      | |
| |  (messages)                          | |
| |                      [user bubble]   | |
| |  [AI avatar] [AI bubble text]        | |
| |                      [user bubble]   | |
| |                                      | |
| +--------------------------------------+ |
| | [_________________________] [send]   | |  <- input area 68px
| +--------------------------------------+ |
+------------------------------------------+
  max-width: 720px, height: calc(100vh - 140px)
```

#### Chat Bubble Styles
- AI bubble: bg white, border 1px #E2E8F0, border-radius 16px (bottom-left 4px), padding 12px 16px
- User bubble: bg linear-gradient(135deg, #2563EB, #3B82F6), color white, border-radius 16px (bottom-right 4px), padding 12px 16px
- Avatar: 32x32, rounded 10px, flex-shrink 0
- Message row: flex, max-width 85%, gap 10px
- User row: align-self flex-end, flex-direction row-reverse
- AI row: align-self flex-start

#### Input Area
- Textarea: flex 1, padding 10px 14px, border-radius 12px, min-height 44px, max-height 120px, autoresize
- Send button: 44x44, border-radius 12px, blue gradient bg, white send icon
- Stop button (streaming): 44x44, red bg (#DC2626), white square icon

#### Quick Prompts
- Chips: padding 8px 16px, border 1px #E2E8F0, border-radius 9999px, bg white, font-size 13px
- Hover: border-color #2563EB, color #2563EB, bg #EFF6FF

---

### 6.5 RecordsView.vue

#### Layout Wireframe
```
+------------------------------------------+
| [< 返回]    签到记录        共 12 条      |
|                                          |
| +--------------------------------------+ |
| | [check] 2026-06-07 08:30:25         | |
| |         5栋 520室                   | |
| |         29.991, 122.179    [已签到]  | |
| +--------------------------------------+ |
| +--------------------------------------+ |
| | [check] 2026-06-06 08:15:00         | |
| |         5栋 520室                   | |
| |         29.991, 122.179    [已签到]  | |
| +--------------------------------------+ |
| ...                                      |
| +--------------------------------------+ |
| |  [上一页]       1/2       [下一页]    | |
| +--------------------------------------+ |
+------------------------------------------+
  max-width: 720px
```

#### Record Item
- Padding: 16px 20px
- Icon: 38x38, rounded 10px, bg #ECFDF5 (success) / #FEF2F2 (error)
- Time: 14px, 600 weight, #1E293B
- Location: 12px, #94A3B8, flex row with SVG icon
- Status tag: pill, 12px, 600 weight, bg #ECFDF5 color #059669

#### Empty State
```
+--------------------------------------+
|          [document SVG 48px]         |
|          暂无签到记录                  |
|      完成签到后，记录将在此显示         |
|          [去签到]                     |
+--------------------------------------+
```

#### Loading/Error States
- Loading: centered spinner + "加载中..."
- Error: red text + retry button

---

### 6.6 ProfileView.vue

#### Layout Wireframe
```
+------------------------------------------+
| +--------------------------------------+ |
| | [person icon 44px]   个人信息         | |
| |                                      | |
| | 学号          2021001234             | |
| | 姓名          张三                    | |
| | 宿舍楼栋      5栋                     | |
| | 寝室号        520                    | |
| | 班级          计算机21-1 [editable]   | |
| | 手机号        138xxxx [editable]      | |
| | 人脸注册      已注册                  | |
| |                                      | |
| |       [编辑] / [保存] [取消]          | |
| |         保存成功!                     | |
| +--------------------------------------+ |
+------------------------------------------+
  max-width: 480px, centered
```

#### Field Display
- Each field: flex row, justify-content space-between, padding 14px 0, border-bottom 1px solid #F1F5F9
- Label: 13px, #64748B, 500 weight, width 60px
- Value: 15px, #1E293B, 500 weight, text-align right
- Edit input: text-align right, border-radius 8px, border 1.5px #E2E8F0, padding 8px 12px, max-width 240px
- Edit input focus: border-color #2563EB, box-shadow 0 0 0 3px rgba(37,99,235,0.1)

---

### 6.7 DefaultLayout.vue

#### Layout Wireframe (Desktop)
```
+==========================================+
| [icon] 寝室签到  [首页][签到][记录][AI]  |  <- 60px navbar, fixed
|                  张三  [clock] [theme][x] |
+==========================================+
|                                          |
|          <router-view />                 |
|          (page content)                  |
|                                          |
+==========================================+
```

#### Layout Wireframe (Mobile)
```
+==========================================+
| [icon] 寝室签到          [theme] [logout] |  <- 60px, fixed
|                                          |
|   [首页]   [签到]   [记录]   [AI助手]    |  <- 44px, fixed below
+==========================================+
|                                          |
|          <router-view />                 |
|                                          |
+==========================================+
  desktop pt: 84px, mobile pt: 120px
```

#### Navbar Details
- Height: 60px (desktop row) + 44px (mobile row when visible)
- Background: `rgba(255,255,255,0.8)` with blur (glass)
- Border-bottom: 1px solid rgba(255,255,255,0.4)
- Max-width: 1024px, centered
- Container padding: 0 20px

#### Desktop Nav Links
- Padding: 8px 12px, border-radius 10px, min-height 44px
- Default: color #64748B, font 14px, 500 weight
- Hover: bg rgba(37,99,235,0.08), color #2563EB
- Active: bg rgba(37,99,235,0.1), color #2563EB, 600 weight

#### Mobile Nav Links
- Flex column, padding 6px 12px, border-radius 10px, min-width 56px
- Default: color #94A3B8, font 11px
- Active: color #2563EB, bg rgba(37,99,235,0.1)

#### Action Buttons
- Theme/logout: 40x40, border-radius 10px, bg rgba(241,245,249,0.8)
- Hover: bg rgba(226,232,240,0.9)
- Logout hover: bg rgba(220,38,38,0.1), color #DC2626
- Clock: monospace font, 12px, color #94A3B8, visible >= 640px

#### Main Content
- Padding-top: 84px (desktop) / 120px (mobile)
- Padding-bottom: 32px
- Padding-x: 16px
- Max-width: 1024px, centered

---

## Summary of All File Changes

| # | File | Action | Reason |
|---|------|--------|--------|
| 1 | `config/WebConfig.java` | **DELETE** | Duplicate config, excluded /api/** from auth |
| 2 | `config/WebMvcConfig.java` | **REWRITE** | Merge both configs, proper exclude list |
| 3 | `interceptor/AuthInterceptor.java` | **REWRITE** | Return 401/redirect instead of `return true` |
| 4 | `config/ApiKeyConfig.java` | **NEW** | Centralized API key config from properties |
| 5 | `config/RagConfig.java` | **MODIFY** | Use ApiKeyConfig instead of hardcoded key |
| 6 | `service/BaiduFaceService.java` | **MODIFY** | Use ApiKeyConfig instead of hardcoded keys |
| 7 | `service/impl/AuthServiceImpl.java` | **MODIFY** | Fix session fields, check() return, updateProfile() |
| 8 | `entity/StudentEntity.java` | **MODIFY** | Add className, phoneNumber fields |
| 9 | `application.properties` | **MODIFY** | Add api.baidu.* and api.dashscope.* properties |
| 10 | `application-local.properties` | **MODIFY** | Add local API key values |
| 11 | `controller/ApiAiController.java` | **MODIFY** | Add /chat-history and /clear-chat-history endpoints |
| 12 | `frontend/src/views/Login.vue` | **MODIFY** | Fix button v-if/v-else, add keyboard submit, autofocus |
| 13 | `frontend/src/views/HomeView.vue` | **MODIFY** | Add error handling for failed status check |
| 14 | `frontend/src/views/CheckInView.vue` | **MODIFY** | Add cancel button, clickable steps |
| 15 | `frontend/src/views/ChatView.vue` | **MODIFY** | Fix API paths to use /api/ai/*, simplify streaming |
| 16 | `frontend/src/views/RecordsView.vue` | **MODIFY** | Fix status label to match "已签到", hide pagination when empty |
| 17 | `frontend/src/views/ProfileView.vue` | **MODIFY** | Add dorm/room/face fields, fix edit persistence |
| 18 | `frontend/src/layouts/DefaultLayout.vue` | **MODIFY** | Add student name to navbar, mobile nav bg |
