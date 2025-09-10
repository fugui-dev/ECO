package com.example.eco.core.service;

import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PendOrderCreateCmd;

public interface PendOrderService {

    /**
     * 创建挂单
     *
     */
    SingleResponse<Void> createPendOrder(PendOrderCreateCmd pendOrderCreateCmd);
}
