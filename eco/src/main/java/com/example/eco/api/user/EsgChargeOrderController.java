package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.ChargeOrderPageQry;
import com.example.eco.bean.dto.ChargeOrderDTO;
import com.example.eco.core.service.EsgChargeOrderService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/esg/charge/order")
public class EsgChargeOrderController {

    @Resource
    private EsgChargeOrderService esgChargeOrderService;


    /**
     * 分页查询充值订单
     */
    @RequestMapping("/page")
    MultiResponse<ChargeOrderDTO> page(@RequestBody ChargeOrderPageQry chargeOrderPageQry){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }

        // 设置钱包地址到查询条件中
        chargeOrderPageQry.setWalletAddress(walletAddress);

        return esgChargeOrderService.page(chargeOrderPageQry);
    }

}
