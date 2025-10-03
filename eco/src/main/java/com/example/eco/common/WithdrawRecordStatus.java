package com.example.eco.common;

import lombok.Getter;

@Getter
public enum WithdrawRecordStatus {

    PENDING_REVIEW("PENDING_REVIEW", "待审核"),

    CANCELED("CANCELED", "已取消"),

    AGREE("AGREE", "审核通过"),

    IN_PROGRESS("IN_PROGRESS","审核中"),

    REJECT("REJECT", "审核拒绝");

    private String code;

    private String name;

    WithdrawRecordStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static WithdrawRecordStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (WithdrawRecordStatus status : WithdrawRecordStatus.values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        return null;
    }
}
