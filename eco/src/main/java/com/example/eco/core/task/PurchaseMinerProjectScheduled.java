package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.TotalComputingPowerCmd;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.model.entity.PurchaseMinerProject;
import com.example.eco.model.entity.PurchaseMinerProjectReward;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.PurchaseMinerProjectMapper;
import com.example.eco.model.mapper.PurchaseMinerProjectRewardMapper;
import com.example.eco.model.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@Transactional
public class PurchaseMinerProjectScheduled {


    @Resource
    private PurchaseMinerProjectMapper purchaseMinerProjectMapper;
    @Resource
    private RecommendStatisticsLogService recommendStatisticsLogService;
    @Resource
    private PurchaseMinerProjectRewardMapper purchaseMinerProjectRewardMapper;
    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Scheduled(cron = "0 0/3 * * * ?")
    public void accelerateExpire() {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
        lambdaQueryWrapper.ge(PurchaseMinerProject::getAccelerateExpireTime, System.currentTimeMillis());
        lambdaQueryWrapper.isNotNull(PurchaseMinerProject::getAccelerateExpireTime);

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);

        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {

            BigDecimal computingPower = new BigDecimal(purchaseMinerProject.getActualComputingPower())
                    .subtract(new BigDecimal(purchaseMinerProject.getComputingPower()));

            purchaseMinerProject.setActualComputingPower(purchaseMinerProject.getComputingPower());
            purchaseMinerProject.setUpdateTime(System.currentTimeMillis());

            TotalComputingPowerCmd totalComputingPowerCmd = new TotalComputingPowerCmd();
            totalComputingPowerCmd.setWalletAddress(purchaseMinerProject.getWalletAddress());
            totalComputingPowerCmd.setComputingPower(computingPower.toString());

            try {

                recommendStatisticsLogService.statistics(totalComputingPowerCmd);

                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Scheduled(cron = "0 0/3 * * * ?")
    public void stop() {

        LambdaQueryWrapper<SystemConfig> systemConfigLambdaQueryWrapper = new LambdaQueryWrapper<>();
        systemConfigLambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ECO_PRICE.getCode());

        SystemConfig systemConfig = systemConfigMapper.selectOne(systemConfigLambdaQueryWrapper);
        if (Objects.isNull(systemConfig)) {
            log.info("未设置ECO价格");
            return;
        }

        BigDecimal price = new BigDecimal(systemConfig.getValue());

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);
        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {

            LambdaQueryWrapper<PurchaseMinerProjectReward> rewardLambdaQueryWrapper = new LambdaQueryWrapper<>();
            rewardLambdaQueryWrapper.eq(PurchaseMinerProjectReward::getPurchaseMinerProjectId, purchaseMinerProject.getId());

            List<PurchaseMinerProjectReward> rewardList = purchaseMinerProjectRewardMapper.selectList(rewardLambdaQueryWrapper);

            BigDecimal totalReward = rewardList.stream()
                    .map(PurchaseMinerProjectReward::getReward)
                    .map(BigDecimal::new)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            if (totalReward.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal totalPrice = totalReward.multiply(price);

            if (totalPrice.compareTo(new BigDecimal(purchaseMinerProject.getPrice())) >= 0) {

                purchaseMinerProject.setStatus(PurchaseMinerProjectStatus.STOP.getCode());
                purchaseMinerProject.setUpdateTime(System.currentTimeMillis());

                TotalComputingPowerCmd totalComputingPowerCmd = new TotalComputingPowerCmd();
                totalComputingPowerCmd.setWalletAddress(purchaseMinerProject.getWalletAddress());
                totalComputingPowerCmd.setComputingPower(new BigDecimal(purchaseMinerProject.getComputingPower()).negate().toString());

                recommendStatisticsLogService.statistics(totalComputingPowerCmd);

                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
            }
        }
    }
}
