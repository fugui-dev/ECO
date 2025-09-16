package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.ChargeOrderCreateCmd;
import com.example.eco.bean.cmd.ChargeOrderPageQry;
import com.example.eco.bean.dto.ChargeOrderDTO;
import com.example.eco.core.service.ChargeOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/charge/order")
public class ChargeOrderController {

    @Resource
    private ChargeOrderService chargeOrderService;


    /**
     * 分页查询充值订单
     */
    @PostMapping("/page")
    MultiResponse<ChargeOrderDTO> page(@RequestBody ChargeOrderPageQry chargeOrderPageQry){
        return chargeOrderService.page(chargeOrderPageQry);
    }


    /**
     * 创建充值订单
     *
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody ChargeOrderCreateCmd chargeOrderCreateCmd){
        return chargeOrderService.create(chargeOrderCreateCmd);
    }
}
