package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PurchaseMinerProjectRewardType {

    STATIC("STATIC", "静态奖励"),

    DYNAMIC("DYNAMIC", "动态奖励");

    private String code;

    private String name;

    PurchaseMinerProjectRewardType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PurchaseMinerProjectRewardType of(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseMinerProjectRewardType type : PurchaseMinerProjectRewardType.values()) {
            if (code.equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
