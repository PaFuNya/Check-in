package org.example.enums;

/**
 * 签到状态枚举
 */
public enum CheckInStatus {
    CHECKED_IN("已签到", "签到成功"),
    PENDING("待签到", "等待签到");

    private final String code;
    private final String label;

    CheckInStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static CheckInStatus fromCode(String code) {
        for (CheckInStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("未知签到状态: " + code);
    }
}
