package com.example.eco.common;

import lombok.Getter;

@Getter
public enum PurchaseMinerProjectDynamicRewardType {

    RECOMMEND("RECOMMEND", "推荐奖励"),

    BASE("BASE", "小区基础奖励"),

    NEW("NEW", "小区新增奖励");

    private String code;

    private String name;

    PurchaseMinerProjectDynamicRewardType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static PurchaseMinerProjectDynamicRewardType of(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseMinerProjectDynamicRewardType type : PurchaseMinerProjectDynamicRewardType.values()) {
            if (code.equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
