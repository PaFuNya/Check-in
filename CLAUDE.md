# 校园寝室自助签到系统 (Campus Check-in AI Assistant)

## 项目概述
基于 LangChain4j + Spring Boot 的校园寝室自助签到 AI 助手。学生通过自然语言对话完成寝室签到、请假报备、考勤查询等操作。

## 技术栈
- Java 17, Spring Boot 3.4.4
- LangChain4j 1.0.1-beta6 (OpenAI-compatible API, DashScope qwen-max)
- MyBatis-Plus 3.5.11
- Spring Data JPA + MySQL 8.x
- Spring WebFlux (SSE streaming)
- Thymeleaf (chat UI)
- Lombok

## 数据库
- MySQL 容器: `1Panel-mysql-RIP6` (WSL Docker)
- 数据库名: `checkin_db`
- 密码: `pafunya@520wuerwu`

### 表结构
1. **chat_history** - 聊天记录 (id, session_id, student_id, role, content)
2. **check_in_record** - 签到记录 (id, student_id, student_name, room_number, dorm_building, status, location_info, check_time)
3. **leave_request** - 请假申请 (id, student_id, student_name, leave_type, start_time, end_time, reason, audit_status, auditor_comment)

## 核心功能模块
1. **AOP 日志拦截** - 拦截用户输入，异步保存聊天记录
2. **意图识别与路由** - AI 分析用户意图，分发到对应处理模块
3. **签到打卡** - 多轮对话采集位置/寝室信息，完成签到
4. **请假报备** - 多轮对话采集请假原因/时间，提交请假申请
5. **规则问答 (RAG)** - 向量化寝室管理条例，回答学生提问
6. **状态查询** - 查询个人签到记录和请假状态

## 参考代码
teacher_reference_src/ 目录下是老师的 lostsystem 项目代码，用于参考架构和实现模式。

## 关键命令
- 构建: `mvn clean compile`
- 运行: `mvn spring-boot:run`
- 测试: `mvn test`
