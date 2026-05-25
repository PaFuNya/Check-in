你是实现者。修复以下 BLOCKER 级别安全问题。每个修复后运行 mvn clean compile 验证。

=== 修复 1: AuthInterceptor.java ===
当前: 始终返回 true，从不拦截未登录请求
修复: 
- 对于页面请求(非API)，未登录时 redirect 到 /login
- 对于 API 请求(/ai/**, /checkin/**)，未登录时返回 401 JSON
- 排除路径: /auth/login, /auth/logout, /login, /css/**, /js/**, /images/**, /static/**, /favicon.ico
- 用 request.getRequestURI() 判断是页面还是API

=== 修复 2: CheckInController.java ===
当前: studentId 从 @RequestBody 读取，可被篡改
修复:
- 从 HttpSession 中获取 studentId: session.getAttribute("studentId")
- 忽略请求体中的 studentId
- 如果 session 中没有 studentId，返回 401

=== 修复 3: AuthServiceImpl.java ===
当前: remember token 明文存DB，Cookie 缺 Secure/SameSite
修复:
- token 存入 DB 前用 SHA-256 哈希
- Cookie 设置: setSecure(true), setSameSite("Lax")
- 如果 Java Servlet 不支持 setSameSite，用 response.setHeader("Set-Cookie", ...) 方式

=== 修复 4: checkin.html ===
当前: innerHTML 拼接 server 数据导致 XSS，studentId 从客户端 JS 变量读取
修复:
- showResult() 中用 textContent 替代 innerHTML 设置文本内容
- 签到请求不发送 studentId（由后端从 session 获取）
- init 时仍然从 /auth/check 获取 studentId 用于显示，但不发送给 /checkin/verify

先读每个文件的当前代码，然后修复。修复后 mvn clean compile。
