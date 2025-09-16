package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.ChargeOrderPageQry;
import com.example.eco.bean.dto.ChargeOrderDTO;
import com.example.eco.core.service.ChargeOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/charge/order")
public class AdminChargeOrderController {

    @Resource
    private ChargeOrderService chargeOrderService;


    /**
     * 分页查询充值订单
     */
    @PostMapping("/page")
    MultiResponse<ChargeOrderDTO> page(@RequestBody ChargeOrderPageQry chargeOrderPageQry){
        return chargeOrderService.page(chargeOrderPageQry);
    }
}
