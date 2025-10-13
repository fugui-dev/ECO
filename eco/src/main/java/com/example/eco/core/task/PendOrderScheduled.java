package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.cmd.ChargeOrderUpdateCmd;
import com.example.eco.bean.cmd.PendOrderCompleteCmd;
import com.example.eco.common.ChargeOrderStatus;
import com.example.eco.common.PendOrderStatus;
import com.example.eco.core.service.ChargeOrderService;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.model.entity.ChargeOrder;
import com.example.eco.model.entity.PendOrder;
import com.example.eco.model.mapper.ChargeOrderMapper;
import com.example.eco.model.mapper.PendOrderMapper;
import com.example.eco.util.TransactionVerificationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
@Transactional
public class PendOrderScheduled {

    @Resource
    private PendOrderMapper pendOrderMapper;
    @Resource
    private PendOrderService pendOrderService;
    @Resource
    private ChargeOrderMapper chargeOrderMapper;
    @Resource
    private ChargeOrderService chargeOrderService;
    @Resource
    private TransactionVerificationUtil transactionVerificationUtil;

//    @Scheduled(cron = "0 0/3 * * * ?")
    public void complete() {

        Long time = System.currentTimeMillis() - (30 * 60 * 1000);

        LambdaQueryWrapper<PendOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PendOrder::getStatus, PendOrderStatus.APPLY.getCode());
        lambdaQueryWrapper.le(PendOrder::getPlaceOrderTime, time);

        List<PendOrder> pendOrderList = pendOrderMapper.selectList(lambdaQueryWrapper);

        for (PendOrder pendOrder : pendOrderList) {

            log.info("锁单时间:{},过期30分钟:{}",pendOrder.getPlaceOrderTime(),time);

            PendOrderCompleteCmd pendOrderCompleteCmd = new PendOrderCompleteCmd();
            pendOrderCompleteCmd.setOrder(pendOrder.getOrder());
            pendOrderCompleteCmd.setWalletAddress(pendOrder.getWalletAddress());

            try {
                pendOrderService.completePendOrder(pendOrderCompleteCmd);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
