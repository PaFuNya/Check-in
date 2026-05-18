package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON工具类
 * 替代DashScope的JsonUtils
 */
@Slf4j
@Component
public class JsonUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("❌ 对象转JSON失败: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON转对象失败: {}", e.getMessage());
            throw new RuntimeException("JSON解析失败", e);
        }
    }
    
    /**
     * JSON字符串转对象（带默认值）
     */
    public static <T> T fromJson(String json, Class<T> clazz, T defaultValue) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON转对象失败，使用默认值: {}", e.getMessage());
            return defaultValue;
        }
    }
}

