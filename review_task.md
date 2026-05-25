你是代码审查者。审查这个 Spring Boot 校园签到项目的代码质量。

请审查以下文件（只读，不改代码）：
1. src/main/java/org/example/controller/AuthController.java
2. src/main/java/org/example/controller/CheckInController.java
3. src/main/java/org/example/service/impl/AuthServiceImpl.java
4. src/main/java/org/example/service/impl/DemoFaceRecognitionServiceImpl.java
5. src/main/java/org/example/service/impl/LocationVerificationServiceImpl.java
6. src/main/java/org/example/interceptor/AuthInterceptor.java
7. src/main/resources/templates/checkin.html
8. src/main/resources/templates/login.html

审查维度：
- 安全性（SQL注入、XSS、密码处理、CSRF）
- 错误处理（异常捕获、参数校验）
- 代码规范（命名、注释、设计模式）
- 前端质量（HTML语义化、JS错误处理、用户体验）

输出格式：对每个文件给出 BLOCKER / SUGGESTION / OK 评级，最后给出总评 PASS/FAIL。
