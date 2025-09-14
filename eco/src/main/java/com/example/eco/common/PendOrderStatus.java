package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PendOrderStatus {

    WAIT("WAIT", "待出售"),

    LOCK("LOCK", "已锁定"),

    CANCEL("CANCEL", "已取消"),

    APPLY("APPLY", "申请放款"),

    DELETE("DELETE", "已删除"),

    COMPLETE("COMPLETE", "已完成");

    private String code;

    private String name;

    PendOrderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PendOrderStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (PendOrderStatus status : PendOrderStatus.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
