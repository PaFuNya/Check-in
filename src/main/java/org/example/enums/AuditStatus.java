package org.example.enums;

/**
 * 请假审核状态枚举
 */
public enum AuditStatus {
    PENDING("待审核", "等待审核"),
    APPROVED("已通过", "审核通过"),
    REJECTED("已驳回", "审核驳回");

    private final String code;
    private final String label;

    AuditStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static AuditStatus fromCode(String code) {
        for (AuditStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("未知审核状态: " + code);
    }
}
