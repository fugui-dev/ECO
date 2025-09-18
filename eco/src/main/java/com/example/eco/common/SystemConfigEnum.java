package com.example.eco.common;

import lombok.Getter;

@Getter
public enum SystemConfigEnum {

    ECO_PRICE("ECO_PRICE", "ECO价格"),

    ECO_TOTAL_NUMBER("ECO_TOTAL_NUMBER", "ECO总发行量"),

    DAILY_TOTAL_REWARD("DAILY_TOTAL_REWARD", "每天挖矿总数数量"),

    DAILY_TOTAL_REWARD_LIMIT("DAILY_TOTAL_REWARD_LIMIT", "每天挖矿总数数量上限"),

    DAILY_TOTAL_REWARD_REDUCE_RATE("DAILY_TOTAL_REWARD_REDUCE_RATE", "每天挖矿总数数量降低到比例"),

    STATIC_REWARD_RATE("STATIC_REWARD_RATE", "静态奖励占比"),

    STATIC_NEW_MINER_RATE("STATIC_NEW_MINER_RATE", "新增算力静态奖励增加倍数"),

    DYNAMIC_REWARD_RATE("DYNAMIC_REWARD_RATE", "动态奖励占比"),

    DYNAMIC_REWARD_RECOMMEND_RATE("DYNAMIC_REWARD_RECOMMEND_RATE", "动态奖励推荐奖励占比"),

    DYNAMIC_REWARD_BASE_RATE("DYNAMIC_REWARD_BASE_RATE", "动态奖励基础奖励占比"),

    DYNAMIC_REWARD_NEW_RATE("DYNAMIC_REWARD_NEW_RATE", "动态奖励新增奖励占比");

    private String code;

    private String name;

    SystemConfigEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SystemConfigEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (SystemConfigEnum config : SystemConfigEnum.values()) {
            if (code.equals(config.code)) {
                return config;
            }
        }
        return null;
    }
}
