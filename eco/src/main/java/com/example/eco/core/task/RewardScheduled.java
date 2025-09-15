package com.example.eco.core.task;

import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.core.cmd.RewardConstructor;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.model.entity.PurchaseMinerProject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@Transactional
public class RewardScheduled {

    @Resource
    private RewardConstructor rewardConstructor;

    @Scheduled(cron = "0 0 2 * * ?")
    @SneakyThrows
    public void reward(){
        log.info("reward 开始执行");

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd = new PurchaseMinerProjectRewardCmd();
        purchaseMinerProjectRewardCmd.setDayTime(dayTime);

        rewardConstructor.reward(purchaseMinerProjectRewardCmd);

        log.info("reward 执行结束");
    }
}
