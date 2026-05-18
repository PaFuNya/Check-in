package org.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("❌ 对象转JSON失败: {}", e.getMessage());
            return "{}";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON转对象失败: {}", e.getMessage());
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz, T defaultValue) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON转对象失败，使用默认值: {}", e.getMessage());
            return defaultValue;
        }
    }
}
