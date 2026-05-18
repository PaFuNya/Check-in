package org.example.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.enitity.ChatHistoryEntity;
import org.example.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Aspect
public class ChatFlowAop {

    //用户消息
    private String userRole = "0";
    //ai消息
    private String aiRole="1";

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Pointcut("@annotation(org.example.aop.ChatFlow)")
    public void pointcut(){

    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sessionId = (String) args[0];
        String message = (String) args[1];
        log.info("-----sessionId:{}---message:{}---",sessionId,message);
        saveChatHistory(sessionId,message,userRole);
        Object result = joinPoint.proceed();
        log.info("-----sessionId:{}---aiMessage:{}---",sessionId,result);
        saveChatHistory(sessionId,result.toString(),aiRole);
        return result;
    }

    private void saveChatHistory(String sessionId, String message, String userRole) {
        ChatHistoryEntity entity = new ChatHistoryEntity();
        entity.setSessionId(sessionId);
        entity.setRole(userRole);
        entity.setContent(message);
        chatHistoryRepository.save(entity);
    }
}
