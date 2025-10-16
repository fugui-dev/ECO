package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PendOrderAppealStatus {

    WAIT("WAIT", "待处理"),

    AGREE("AGREE", "同意"),

    REFUSE("REFUSE", "拒绝");

    private String code;

    private String name;

    PendOrderAppealStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PendOrderAppealStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (PendOrderAppealStatus status : PendOrderAppealStatus.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
