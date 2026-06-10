package org.example.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.entity.ChatHistoryEntity;
import org.example.enums.ChatRole;
import org.example.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Aspect
public class ChatFlowAop {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Pointcut("@annotation(org.example.aop.ChatFlow)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sessionId = (String) args[0];
        // chatStream(userId, studentName, className, message) — message 是 args[3]
        String message = (String) args[3];
        log.info("-----sessionId:{}---message:{}---", sessionId, message);

        // 异步保存用户消息，不阻塞AI响应
        saveChatHistoryAsync(sessionId, message, ChatRole.USER.getCode());

        Object result = joinPoint.proceed();
        log.info("-----sessionId:{}---aiMessage:{}---", sessionId, result);

        // 异步保存AI回复
        saveChatHistoryAsync(sessionId, result.toString(), ChatRole.AI.getCode());
        return result;
    }

    @Async
    public void saveChatHistoryAsync(String sessionId, String message, String role) {
        try {
            ChatHistoryEntity entity = new ChatHistoryEntity();
            entity.setSessionId(sessionId);
            entity.setStudentId(sessionId);
            entity.setRole(role);
            entity.setContent(message);
            chatHistoryRepository.save(entity);
        } catch (Exception e) {
            log.error("异步保存聊天记录失败: sessionId={}, role={}", sessionId, role, e);
        }
    }
}
