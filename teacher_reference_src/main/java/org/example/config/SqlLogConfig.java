package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.concurrent.Executor;

/**
 * SQL日志配置
 * 用于增强SQL参数显示
 */
@Slf4j
@Configuration
public class SqlLogConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * 获取Hibernate Session用于调试
     */
    public Session getHibernateSession() {
        return entityManager.unwrap(Session.class);
    }
    
    /**
     * 打印SQL执行上下文信息
     */
    public void logSqlContext(String operation, Object... params) {
        log.info("🔍 [SQL-CONTEXT] 操作: {}", operation);
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                log.info("🔍 [SQL-PARAM-{}] 值: '{}' (类型: {})", 
                    i + 1, 
                    params[i], 
                    params[i] != null ? params[i].getClass().getSimpleName() : "null");
            }
        }
        
        try {
            Session session = getHibernateSession();
            SessionImplementor sessionImpl = (SessionImplementor) session;
            log.info("🔍 [SQL-SESSION] Session状态: open={}, connected={}", 
                session.isOpen(), 
                sessionImpl.getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected());
        } catch (Exception e) {
            log.debug("🔍 [SQL-SESSION] 无法获取Session信息: {}", e.getMessage());
        }
    }
}

