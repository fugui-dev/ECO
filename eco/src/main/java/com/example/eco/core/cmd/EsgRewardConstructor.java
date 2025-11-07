package com.example.eco.core.cmd;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AccountEsgStaticNumberCmd;
import com.example.eco.bean.cmd.AccountStaticNumberCmd;
import com.example.eco.bean.cmd.EsgPurchaseMinerProjectRewardCmd;
import com.example.eco.common.PurchaseMinerProjectStatus;
import com.example.eco.core.service.AccountService;
import com.example.eco.core.service.EsgAccountService;
import com.example.eco.model.entity.EsgMinerProject;
import com.example.eco.model.entity.EsgPurchaseMinerProject;
import com.example.eco.model.entity.EsgPurchaseMinerProjectReward;
import com.example.eco.model.mapper.EsgMinerProjectMapper;
import com.example.eco.model.mapper.EsgPurchaseMinerProjectMapper;
import com.example.eco.model.mapper.EsgPurchaseMinerProjectRewardMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class EsgRewardConstructor {

    @Resource
    private EsgPurchaseMinerProjectMapper esgPurchaseMinerProjectMapper;

    @Resource
    private EsgPurchaseMinerProjectRewardMapper esgPurchaseMinerProjectRewardMapper;

    @Resource
    private EsgMinerProjectMapper esgMinerProjectMapper;

    @Resource
    private AccountService accountService;

    @Resource
    private EsgAccountService esgAccountService;



    public SingleResponse reward(EsgPurchaseMinerProjectRewardCmd esgPurchaseMinerProjectRewardCmd){

        Long endTime = LocalDate.parse(esgPurchaseMinerProjectRewardCmd.getDayTime()).plusDays(1).atStartOfDay()
                .atZone(ZoneId.systemDefault())  // 明确使用系统时区
                .toInstant()
                .toEpochMilli();

        LambdaQueryWrapper<EsgPurchaseMinerProject> purchaseMinerProjectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        purchaseMinerProjectLambdaQueryWrapper.eq(EsgPurchaseMinerProject::getStatus, PurchaseMinerProjectStatus.SUCCESS.getCode());
        purchaseMinerProjectLambdaQueryWrapper.le(EsgPurchaseMinerProject::getCreateTime, endTime);

        List<EsgPurchaseMinerProject> esgPurchaseMinerProjects = esgPurchaseMinerProjectMapper.selectList(purchaseMinerProjectLambdaQueryWrapper);

        for (EsgPurchaseMinerProject esgPurchaseMinerProject : esgPurchaseMinerProjects){

            BigDecimal totalReward = new BigDecimal(esgPurchaseMinerProject.getReward());

            BigDecimal computingPower = new BigDecimal(esgPurchaseMinerProject.getComputingPower());

            if (totalReward.compareTo(computingPower) >= 0){
                log.info("用户{}的矿机项目{}已发放完毕，跳过此次发放", esgPurchaseMinerProject.getWalletAddress(), esgPurchaseMinerProject.getId());

                esgPurchaseMinerProject.setStatus(PurchaseMinerProjectStatus.STOP.getCode());

                esgPurchaseMinerProjectMapper.updateById(esgPurchaseMinerProject);

                continue;
            }

            String order = "EST" + System.currentTimeMillis();

            EsgMinerProject esgMinerProject = esgMinerProjectMapper.selectById(esgPurchaseMinerProject.getMinerProjectId());

            if(Objects.isNull(esgMinerProject)){
                log.info("用户{}的矿机项目{}对应的矿机产品不存在，跳过此次发放", esgPurchaseMinerProject.getWalletAddress(), esgPurchaseMinerProject.getId());
                continue;
            }

            if (esgMinerProject.getRate() == null || esgMinerProject.getRate().isEmpty()){
                log.info("用户{}的矿机项目{}对应的矿机产品的发放比例不存在，跳过此次发放", esgPurchaseMinerProject.getWalletAddress(), esgPurchaseMinerProject.getId());
                continue;
            }

            BigDecimal rate = new BigDecimal(esgMinerProject.getRate());

            BigDecimal dailyReward = computingPower.multiply(rate);

            EsgPurchaseMinerProjectReward esgPurchaseMinerProjectReward = new EsgPurchaseMinerProjectReward();
            esgPurchaseMinerProjectReward.setEsgPurchaseMinerProjectId(esgPurchaseMinerProject.getId());
            esgPurchaseMinerProjectReward.setWalletAddress(esgPurchaseMinerProject.getWalletAddress());
            esgPurchaseMinerProjectReward.setReward(dailyReward.toPlainString());
            esgPurchaseMinerProjectReward.setComputingPower(esgPurchaseMinerProject.getComputingPower());
            esgPurchaseMinerProjectReward.setRate(esgMinerProject.getRate());
            esgPurchaseMinerProjectReward.setDayTime(esgPurchaseMinerProjectRewardCmd.getDayTime());
            esgPurchaseMinerProjectReward.setOrder(order);
            esgPurchaseMinerProjectReward.setCreateTime(System.currentTimeMillis());
            esgPurchaseMinerProjectRewardMapper.insert(esgPurchaseMinerProjectReward);


            totalReward = totalReward.add(dailyReward);

            if (totalReward.compareTo(computingPower) >= 0){
                esgPurchaseMinerProject.setStatus(PurchaseMinerProjectStatus.STOP.getCode());
            }

            esgPurchaseMinerProject.setYesterdayReward(dailyReward.toString());
            esgPurchaseMinerProject.setReward(totalReward.toString());
            esgPurchaseMinerProjectMapper.updateById(esgPurchaseMinerProject);

            try {
                AccountEsgStaticNumberCmd accountEsgStaticNumberCmd = new AccountEsgStaticNumberCmd();
                accountEsgStaticNumberCmd.setWalletAddress(esgPurchaseMinerProject.getWalletAddress());
                accountEsgStaticNumberCmd.setNumber(dailyReward.toString());
                accountEsgStaticNumberCmd.setOrder(order);

                SingleResponse<Void> response = accountService.addEsgRewardNumber(accountEsgStaticNumberCmd);
                if (!response.isSuccess()) {
                    log.info("用户{}发放ESG静态奖励{}失败，调用账户服务失败", esgPurchaseMinerProject.getWalletAddress(), dailyReward);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            try {
                AccountStaticNumberCmd accountStaticNumberCmd = new AccountStaticNumberCmd();
                accountStaticNumberCmd.setWalletAddress(esgPurchaseMinerProject.getWalletAddress());
                accountStaticNumberCmd.setNumber(dailyReward.toString());
                accountStaticNumberCmd.setOrder(order);

                SingleResponse<Void> response = esgAccountService.addStaticNumber(accountStaticNumberCmd);
                if (!response.isSuccess()) {
                    log.info("用户{}发放ESG静态奖励{}失败，调用账户服务失败", esgPurchaseMinerProject.getWalletAddress(), dailyReward);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            log.info("用户{}的矿机项目{}发放奖励{}成功", esgPurchaseMinerProject.getWalletAddress(), esgPurchaseMinerProject.getId(), dailyReward.toPlainString());
        }

        return SingleResponse.buildSuccess();
    }
}
