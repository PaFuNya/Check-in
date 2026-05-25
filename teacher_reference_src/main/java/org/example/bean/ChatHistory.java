package org.example.bean;

import lombok.Data;
import org.springframework.boot.convert.DataSizeUnit;

@Data
public class ChatHistory {
    /**
     * 聊天角色
     */
    private String role;

    /**
     * 会话内容
     */
    private String content;
}
