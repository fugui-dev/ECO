package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.cmd.PendOrderCompleteCmd;
import com.example.eco.common.PendOrderStatus;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.model.entity.PendOrder;
import com.example.eco.model.mapper.PendOrderMapper;
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

    @Scheduled(cron = "0 0/3 * * * ?")
    public void complete() {

        Long time = System.currentTimeMillis() - (30 * 60 * 1000);

        LambdaQueryWrapper<PendOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PendOrder::getStatus, PendOrderStatus.LOCK.getCode());
        lambdaQueryWrapper.ge(PendOrder::getPlaceOrderTime, time);

        List<PendOrder> pendOrderList = pendOrderMapper.selectList(lambdaQueryWrapper);

        for (PendOrder pendOrder : pendOrderList) {

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
