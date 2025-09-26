package com.example.eco.core.task;

import com.example.eco.core.service.ChargeOrderService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Component
@Transactional
public class ChargeOrderScheduled {


    @Resource
    private ChargeOrderService chargeOrderService;


    @Scheduled(cron = "0 0/3 * * * ?")
    @SneakyThrows
    public void checkChargeOrder() {

        log.info("checkChargeOrder 开始执行");

        try {
            chargeOrderService.checkChargeOrder();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        log.info("checkChargeOrder 执行结束");
    }
}
