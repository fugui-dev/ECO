package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.*;
import com.example.eco.common.PurchaseMinerType;
import com.example.eco.core.cmd.RewardConstructor;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.util.UserContextUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin/purchase/miner/project")
public class AdminPurchaseMinerProjectController {

    @Resource
    private PurchaseMinerProjectService purchaseMinerProjectService;
    @Resource
    private RewardConstructor rewardConstructor;


    /**
     * 分页查询购买矿机项目
     */
    @PostMapping("/page")
    MultiResponse<PurchaseMinerProjectDTO> page(@RequestBody PurchaseMinerProjectPageQry purchaseMinerProjectPageQry){
        return purchaseMinerProjectService.page(purchaseMinerProjectPageQry);
    }

    @PostMapping("/reward")
    SingleResponse<Void> reward(@RequestBody PurchaseMinerProjectRewardCmd purchaseMinerProjectRewardCmd){
        rewardConstructor.reward(purchaseMinerProjectRewardCmd);
        return SingleResponse.buildSuccess();
    }

    /**
     * 创建购买矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        purchaseMinerProjectsCreateCmd.setType(PurchaseMinerType.AIRDROP.getCode());
        return purchaseMinerProjectService.create(purchaseMinerProjectsCreateCmd);
    }

    /**
     * 批量创建购买矿机项目
     */
    @PostMapping("/batch/create")
    SingleResponse<PurchaseMinerProjectsBatchCreateResultDTO> batchCreate(@RequestBody PurchaseMinerProjectsBatchCreateCmd purchaseMinerProjectsBatchCreateCmd) {

        List<PurchaseMinerProjectsCreateCmd> purchaseMinerProjectsCreateCmdList = purchaseMinerProjectsBatchCreateCmd.getPurchaseMinerProjectsCreateCmdList();

        if (purchaseMinerProjectsCreateCmdList == null || purchaseMinerProjectsCreateCmdList.isEmpty()) {
            return SingleResponse.buildFailure("钱包地址列表不能为空");
        }

        PurchaseMinerProjectsBatchCreateResultDTO resultDTO = new PurchaseMinerProjectsBatchCreateResultDTO();

        Map<String, String> failureDetails = new HashMap<>();

        Integer successCount = 0;

        Integer failureCount = 0;

        for (PurchaseMinerProjectsCreateCmd cmd : purchaseMinerProjectsCreateCmdList){

            cmd.setType(PurchaseMinerType.AIRDROP.getCode());
            SingleResponse<Void> response = purchaseMinerProjectService.create(cmd);

            if (!response.isSuccess()){

                failureCount++;
                failureDetails.put(cmd.getWalletAddress(), response.getErrMessage());

            } else {
                successCount++;
            }
        }

        resultDTO.setSuccessCount(successCount);
        resultDTO.setFailureCount(failureCount);
        resultDTO.setFailureDetails(failureDetails);

        return SingleResponse.of(resultDTO);
    }


    /**
     * 获取支付方式
     */
    @PostMapping("/buy/way/list")
    MultiResponse<PurchaseMinerBuyWayDTO> purchaseMinerBuyWayList(@RequestBody PurchaseMinerBuyWayQry purchaseMinerBuyWayQry){

        return purchaseMinerProjectService.purchaseMinerBuyWayList(purchaseMinerBuyWayQry);
    }


    /**
     * 创建或更新支付方式
     */
    @PostMapping("/buy/way/edit")
    SingleResponse<Void> createPurchaseMinerBuyWay(@RequestBody PurchaseMinerBuyWayCreateCmd purchaseMinerBuyWayCreateCmd){
        return purchaseMinerProjectService.createPurchaseMinerBuyWay(purchaseMinerBuyWayCreateCmd);
    }

    /**
     * 算力统计
     */
    @PostMapping("/computing/power/statistic")
    SingleResponse<ComputingPowerStatisticDTO> computingPowerStatistic(@RequestBody ComputingPowerStatisticQry computingPowerStatisticQry){
        String dayTime = computingPowerStatisticQry.getDayTime();
        if (dayTime == null || dayTime.trim().isEmpty()) {
            dayTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        computingPowerStatisticQry.setDayTime(dayTime);
        return purchaseMinerProjectService.computingPowerStatistic(computingPowerStatisticQry);
    }


    /**
     * 查询伞下使用ESG-ECO方式购买不同等级矿机的数量
     */
    @PostMapping("/subordinate/miner/statistics")
    MultiResponse<MinerLevelStatisticsDTO> getSubordinateMinerStatistics(@RequestBody SubordinateMinerStatisticsQry qry) {

        return purchaseMinerProjectService.getSubordinateMinerStatistics(qry);
    }


    /**
     * 购买矿机项目奖励统计
     */
    @PostMapping("/reward/statistic")
    SingleResponse<PurchaseMinerProjectRewardStatisticDTO> getPurchaseMinerProjectRewardStatistic(@RequestBody PurchaseMinerProjectRewardStatisticQry qry){

        return purchaseMinerProjectService.getPurchaseMinerProjectRewardStatistic(qry);
    }
}
