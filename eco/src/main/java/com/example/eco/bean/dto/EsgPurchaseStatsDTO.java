package com.example.eco.bean.dto;

import lombok.Data;

@Data
public class EsgPurchaseStatsDTO {

    private boolean rushMode;     // 是否开启抢购模式

    private long dailyLimit;      // 每日限制（-1表示无限制）

    private long todayCount;      // 今日已售

    private long remaining;       // 剩余数量（-1表示无限制）

    private boolean available;    // 是否可用
}
