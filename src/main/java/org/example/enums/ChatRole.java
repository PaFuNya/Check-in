package org.example.enums;

/**
 * 聊天角色枚举
 */
public enum ChatRole {
    USER("0", "用户"),
    AI("1", "AI助手");

    private final String code;
    private final String label;

    ChatRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static ChatRole fromCode(String code) {
        for (ChatRole role : values()) {
            if (role.code.equals(code)) return role;
        }
        throw new IllegalArgumentException("未知聊天角色: " + code);
    }
}
