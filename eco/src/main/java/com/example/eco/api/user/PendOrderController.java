package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PendOrderDTO;
import com.example.eco.core.service.PendOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/pend/order")
public class PendOrderController {

    @Resource
    private PendOrderService pendOrderService;

    /**
     * 分页查询挂单
     */
    @PostMapping("/page")
    MultiResponse<PendOrderDTO> page(@RequestBody PendOrderPageQry pendOrderPageQry){
        return pendOrderService.page(pendOrderPageQry);
    }

    /**
     * 创建挂单
     *
     */
    @PostMapping("/create")
    SingleResponse<Void> createPendOrder(@RequestBody PendOrderCreateCmd pendOrderCreateCmd){
        return pendOrderService.createPendOrder(pendOrderCreateCmd);
    }

    /**
     * 删除挂单
     *
     */
    @PostMapping("/delete")
    SingleResponse<Void> deletePendOrder(@RequestBody PendOrderDeleteCmd pendOrderDeleteCmd){
        return pendOrderService.deletePendOrder(pendOrderDeleteCmd);
    }

    /**
     * 锁定挂单
     *
     */
    @PostMapping("/lock")
    SingleResponse<Void> lockPendOrder(@RequestBody PendOrderLockCmd pendOrderLockCmd){
        return pendOrderService.lockPendOrder(pendOrderLockCmd);
    }


    /**
     * 取消挂单
     *
     */
    @PostMapping("/cancel")
    SingleResponse<Void> cancelPendOrder(@RequestBody PendOrderCancelCmd pendOrderCancelCmd){
        return pendOrderService.cancelPendOrder(pendOrderCancelCmd);
    }

    /**
     * 完成挂单
     *
     */
    @PostMapping("/complete")
    SingleResponse<Void> completePendOrder(@RequestBody PendOrderCompleteCmd pendOrderCompleteCmd){
        return pendOrderService.completePendOrder(pendOrderCompleteCmd);
    }

    /**
     * 上传支付凭证
     *
     */
    @PostMapping("/upload/voucher")
    SingleResponse<Void> uploadVoucher(@RequestBody PendOrderUploadVoucherCmd pendOrderUploadVoucherCmd){
        return pendOrderService.uploadVoucher(pendOrderUploadVoucherCmd);
    }



}
