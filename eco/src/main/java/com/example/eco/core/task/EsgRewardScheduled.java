package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountEsgStaticNumberCmd;
import com.example.eco.bean.cmd.AccountStaticNumberCmd;
import com.example.eco.bean.cmd.EsgPurchaseMinerProjectRewardCmd;
import com.example.eco.common.AccountType;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.core.cmd.EsgRewardConstructor;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.model.entity.EsgMinerProject;
import com.example.eco.model.entity.EsgPurchaseMinerProject;
import com.example.eco.model.entity.EsgPurchaseMinerProjectReward;
import com.example.eco.model.entity.PurchaseMinerProject;
import com.example.eco.model.mapper.EsgMinerProjectMapper;
import com.example.eco.model.mapper.EsgPurchaseMinerProjectMapper;
import com.example.eco.model.mapper.EsgPurchaseMinerProjectRewardMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@Transactional
public class EsgRewardScheduled {

    @Resource
    private EsgRewardConstructor esgRewardConstructor;

    @Scheduled(cron = "0 0 1 * * ?")
    @SneakyThrows
    public void reward(){
        log.info("=== 定时ESG奖励发放任务开始执行 ===");

        String dayTime = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("【定时任务】发放日期: {}", dayTime);

        try {

            EsgPurchaseMinerProjectRewardCmd esgPurchaseMinerProjectRewardCmd = new EsgPurchaseMinerProjectRewardCmd();
            esgPurchaseMinerProjectRewardCmd.setDayTime(dayTime);

            SingleResponse<?> singleResponse = esgRewardConstructor.reward(esgPurchaseMinerProjectRewardCmd);
            if (singleResponse.isSuccess()) {
                log.info("【定时任务】奖励发放成功，日期: {}", dayTime);
            } else {
                log.error("【定时任务】奖励发放失败，日期: {}, 错误: {}", dayTime, singleResponse.getErrMessage());
            }
        }catch (Exception e) {
            log.error("【定时任务】奖励发放异常，日期: {}", dayTime, e);
        }

        log.info("=== 定时ESG奖励发放任务执行结束 ===");
    }
}
