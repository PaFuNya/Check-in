# 任务：实现校园寝室自助签到系统

## 你的角色
你是 @coder（实现者），严格按照下面的 SPEC 实现代码。不要质疑方案。

## 重要：先读参考代码
在开始之前，必须先阅读 teacher_reference_src/ 目录下的所有 Java 源文件，理解老师的架构模式：
- `teacher_reference_src/main/java/org/example/` 下的所有文件
- `teacher_reference_src/main/resources/` 下的所有 .txt 提示词文件
- `teacher_reference_src/main/resources/application.properties`

## 要实现的文件清单

### 1. Entity 实体类 (src/main/java/org/example/entity/)
基于 checkin_db 数据库表创建 JPA 实体：

```
BaseEntity.java          - 基础字段 (id, createdDate, updatedDate)
ChatHistoryEntity.java   - chat_history 表
CheckInRecordEntity.java - check_in_record 表  
LeaveRequestEntity.java  - leave_request 表
```

### 2. Repository (src/main/java/org/example/repository/)
```
BaseRepository.java         - CrudRepository<T, Long>
ChatHistoryRepository.java  - findBySessionId, custom queries
CheckInRecordRepository.java - findByStudentId, findByCheckTimeBetween
LeaveRequestRepository.java - findByStudentId, findByAuditStatus
```

### 3. AI Output DTOs (src/main/java/org/example/aioutput/)
```
IntentionOutput.java      - 意图输出 (intention: int, output: String)
CheckInOutput.java        - 签到输出 (studentId, studentName, roomNumber, dormBuilding, locationInfo, completed: boolean, output: String)
LeaveRequestOutput.java   - 请假输出 (studentId, studentName, leaveType, startTime, endTime, reason, completed: boolean, output: String)
```

### 4. AI Service 接口 (src/main/java/org/example/aiservice/)
```
AiAssistant.java                    - 意图识别 (@UserMessage with getIntention.txt)
CheckInAssistant.java               - 签到多轮对话 (@SystemMessage with checkin.txt)
LeaveRequestAssistant.java          - 请假多轮对话 (@SystemMessage with leaveRequest.txt)
QueryCheckInAssistant.java          - 流式查询签到记录 (@SystemMessage with queryCheckIn.txt)
```

### 5. Tools 工具类 (src/main/java/org/example/tools/)
```
ChatHistoryTools.java       - @Tool getChatHistory(sessionId) - 从DB查历史
CheckInRecordTools.java     - @Tool queryCheckInByStudentId(studentId) - 查签到记录
LeaveRequestTools.java      - @Tool queryLeaveByStudentId(studentId) - 查请假记录
```

### 6. AOP (src/main/java/org/example/aop/)
```
ChatFlow.java    - 自定义注解
ChatFlowAop.java - 切面：拦截 @ChatFlow 方法，保存 user+AI 消息到 chat_history
```

### 7. Service 业务层 (src/main/java/org/example/service/)
```
AiChatService.java           - 接口
impl/AiChatServiceImpl.java  - 核心编排：意图识别 → 路由 → 对应AI处理 → 返回结果
```

### 8. Controller (src/main/java/org/example/controller/)
```
AiController.java - REST 端点:
  GET /ai/chat-stream?userId=X&message=Y  → SSE 流式返回
  GET /ai/chat-history?sessionId=X        → 聊天历史
  GET /ai/check-in-records?studentId=X    → 签到记录
  GET /ai/leave-requests?studentId=X      → 请假记录
```

### 9. Config (src/main/java/org/example/config/)
```
ChatMemoryConfig.java  - InMemoryChatMemoryStore (20 messages window)
RagConfig.java         - EmbeddingModel + InMemoryEmbeddingStore (for RAG)
```

### 10. 提示词文件 (src/main/resources/)
```
getIntention.txt      - 意图分类：1=签到打卡, 2=请假报备, 3=查询签到/请假状态, 4=规则问答, 5=其他
checkin.txt           - 签到登记：收集学号、姓名、寝室号、楼栋、位置信息
leaveRequest.txt      - 请假报备：收集学号、姓名、请假类型、起止时间、原因
queryCheckIn.txt      - 查询签到：调用工具查询签到记录和请假状态
```

### 11. application.properties
参考老师项目的配置，修改：
- 数据库连接: `jdbc:mysql://localhost:3306/checkin_db`
- 密码: `pafunya@520wuerwu`
- LLM: DashScope qwen-max (保持老师的配置)
- 嵌入模型: text-embedding-v3

### 12. Application.java
Spring Boot 启动类，包名 `org.example`

### 13. Main.java
简单测试入口（可选）

## 实现顺序
1. 先创建所有目录结构
2. 实现 Entity → Repository
3. 实现 AI Output DTOs
4. 实现 Config (ChatMemory, RAG)
5. 实现 AOP
6. 实现 Tools
7. 实现 AI Service 接口
8. 实现提示词文件
9. 实现业务 Service
10. 实现 Controller
11. 更新 application.properties
12. 创建 Application.java
13. 编译验证: `mvn clean compile`

## 关键设计原则
- 参考 teacher_reference_src/ 的代码风格和架构
- 使用 LangChain4j 的 @UserMessage, @SystemMessage, @V 注解
- 意图识别输出严格 JSON: `{"intention": 1, "output": "..."}`
- 签到/请假输出也严格 JSON
- AOP 用 @Around 环绕通知保存聊天记录
- Controller 使用 Flux<String> 实现 SSE 流式返回

## 编译验证
每完成一个模块后，运行 `mvn clean compile` 确保无编译错误。
