package org.example.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import dev.langchain4j.agent.tool.Tool;


import java.lang.reflect.Method;
import java.util.Map;

/**
 * 工具调试配置类
 * 用于检查所有@Tool注解的注册情况
 */
@Slf4j
@Configuration
public class ToolsDebugConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void debugToolsRegistration() {
        log.info("🔧 ========== 开始检查@Tool注解注册情况 ==========");
        
        // 获取所有Spring Bean
        Map<String, Object> allBeans = applicationContext.getBeansOfType(Object.class);
        
        int toolCount = 0;
        for (Map.Entry<String, Object> entry : allBeans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            // 检查每个Bean的方法
            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                Tool toolAnnotation = AnnotationUtils.findAnnotation(method, Tool.class);
                if (toolAnnotation != null) {
                    toolCount++;
                    log.info("🔧 [TOOL-FOUND] Bean: {} | 方法: {} | 工具名: \"{}\" | 描述: \"{}\"", 
                        beanName, method.getName(), toolAnnotation.name(), toolAnnotation.value());
                    
                    // 打印参数信息
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length > 0) {
                        StringBuilder params = new StringBuilder();
                        for (int i = 0; i < paramTypes.length; i++) {
                            if (i > 0) params.append(", ");
                            params.append(paramTypes[i].getSimpleName());
                        }
                        log.info("🔧 [TOOL-PARAMS] 参数类型: [{}]", params.toString());
                    }
                }
            }
        }
        
        log.info("🔧 ========== 总共找到 {} 个@Tool注解 ==========", toolCount);
        
        // 检查特定的工具Bean
        checkSpecificToolBean("chatHistoryTools");
        checkSpecificToolBean("lostRegisterTools");
        checkSpecificToolBean("registerFindLostPropertyTools");
        checkSpecificToolBean("queryLostPropertyTools");
    }
    
    private void checkSpecificToolBean(String beanName) {
        try {
            Object bean = applicationContext.getBean(beanName);
            log.info("✅ [TOOL-BEAN] Bean \"{}\" 已成功注册: {}", beanName, bean.getClass().getName());
        } catch (Exception e) {
            log.error("❌ [TOOL-BEAN] Bean \"{}\" 注册失败: {}", beanName, e.getMessage());
        }
    }
}

