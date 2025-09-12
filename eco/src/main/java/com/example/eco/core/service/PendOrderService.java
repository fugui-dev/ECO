package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PendOrderDTO;

public interface PendOrderService {

    /**
     * 创建挂单
     *
     */
    SingleResponse<Void> createPendOrder(PendOrderCreateCmd pendOrderCreateCmd);

    /**
     * 删除挂单
     *
     */
    SingleResponse<Void> deletePendOrder(PendOrderDeleteCmd pendOrderDeleteCmd);

    /**
     * 锁定挂单
     *
     */
    SingleResponse<Void> lockPendOrder(PendOrderLockCmd pendOrderLockCmd);


    /**
     * 取消挂单
     *
     */
    SingleResponse<Void> cancelPendOrder(PendOrderCancelCmd pendOrderCancelCmd);

    /**
     * 完成挂单
     *
     */
    SingleResponse<Void> completePendOrder(PendOrderCompleteCmd pendOrderCompleteCmd);

    /**
     * 上传支付凭证
     *
     */
    SingleResponse<Void> uploadVoucher(PendOrderUploadVoucherCmd pendOrderUploadVoucherCmd);


    /**
     * 分页查询挂单
     */
    MultiResponse<PendOrderDTO> page(PendOrderPageQry pendOrderPageQry);
}
