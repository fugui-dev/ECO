package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.ChargeOrderCreateCmd;
import com.example.eco.bean.cmd.ChargeOrderPageQry;
import com.example.eco.bean.cmd.ChargeOrderUpdateCmd;
import com.example.eco.bean.dto.ChargeOrderDTO;

public interface EsgChargeOrderService {

    /**
     * 创建充值订单
     *
     */
    SingleResponse<Void> create(ChargeOrderCreateCmd chargeOrderCreateCmd);

    /**
     * 更新充值订单
     *
     */
    SingleResponse<Void> update(ChargeOrderUpdateCmd chargeOrderUpdateCmd);

    /**
     * 分页查询充值订单
     */
    MultiResponse<ChargeOrderDTO> page(ChargeOrderPageQry chargeOrderPageQry);

    /**
     * 定时检查充值订单状态
     */
    SingleResponse<Void> checkChargeOrder();


    /**
     * 定时处理充值失败订单
     */
    SingleResponse<Void> dealwithFailChargeOrder();
}
