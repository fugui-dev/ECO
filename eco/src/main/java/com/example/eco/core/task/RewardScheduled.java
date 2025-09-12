package com.example.eco.core.task;

import com.example.eco.model.entity.PurchaseMinerProject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Component
@Transactional
public class RewardScheduled {

    @Resource
    private PurchaseMinerProject purchaseMinerProject;

    @Scheduled(cron = "0 0 2 * * ?")
    @SneakyThrows
    public void reward(){
        log.info("reward 开始执行");


        log.info("reward 执行结束");
    }
}
