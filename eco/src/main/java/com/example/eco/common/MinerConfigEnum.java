package com.example.eco.common;

import lombok.Getter;

@Getter
public enum MinerConfigEnum {

    WITHDRAW_SERVICE("WITHDRAW_SERVICE", "提币消耗ESG服务费"),

    MINER_ADD_NUMBER_REQUIREMENT("MINER_ADD_NUMBER_REQUIREMENT", "新增矿机挖矿数量要求"),

    MINER_ADD_NUMBER("MINER_ADD_NUMBER", "新增矿机挖矿数量"),

    MINER_REQUIREMENT("MINER_REQUIREMENT", "矿机挖矿算力要求");

    private String code;

    private String name;

    MinerConfigEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MinerConfigEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (MinerConfigEnum config : MinerConfigEnum.values()) {
            if (code.equals(config.code)) {
                return config;
            }
        }
        return null;
    }
}
