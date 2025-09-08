package com.example.eco.common;

import lombok.Getter;

@Getter
public enum RecommendStatus {

    NORMAL("NORMAL", "正常挖矿"),

    STOP("STOP", "已达2倍挖矿限制，暂停挖矿，以及奖励");

    private String code;

    private String name;

    RecommendStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RecommendStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (RecommendStatus statsu : RecommendStatus.values()) {
            if (code.equals(statsu.code)) {
                return statsu;
            }
        }
        return null;
    }
}
