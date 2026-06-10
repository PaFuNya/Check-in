package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "api")
public class ApiKeyConfig {

    private Baidu baidu = new Baidu();
    private Dashscope dashscope = new Dashscope();

    @Getter
    @Setter
    public static class Baidu {
        private String apiKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Dashscope {
        private String apiKey;
    }
}
