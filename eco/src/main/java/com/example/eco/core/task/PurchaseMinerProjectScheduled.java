package com.example.eco.core.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.TotalComputingPowerCmd;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.RecommendStatisticsLogService;
import com.example.eco.core.service.impl.ComputingPowerServiceImplV2;
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
    private ComputingPowerServiceImplV2 computingPowerServiceV2;


    @Scheduled(cron = "0 0/3 * * * ?")
    public void accelerateExpire() {

        LambdaQueryWrapper<PurchaseMinerProject> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(PurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
        lambdaQueryWrapper.le(PurchaseMinerProject::getAccelerateExpireTime, System.currentTimeMillis());
        lambdaQueryWrapper.isNotNull(PurchaseMinerProject::getAccelerateExpireTime);

        List<PurchaseMinerProject> purchaseMinerProjectList = purchaseMinerProjectMapper.selectList(lambdaQueryWrapper);

        for (PurchaseMinerProject purchaseMinerProject : purchaseMinerProjectList) {

            purchaseMinerProject.setActualComputingPower(purchaseMinerProject.getComputingPower());
            purchaseMinerProject.setUpdateTime(System.currentTimeMillis());

            // 矿机加速到期，清除用户算力缓存，让下次查询时重新计算
            computingPowerServiceV2.invalidateUserCache(purchaseMinerProject.getWalletAddress());

            try {

                purchaseMinerProjectMapper.updateById(purchaseMinerProject);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
