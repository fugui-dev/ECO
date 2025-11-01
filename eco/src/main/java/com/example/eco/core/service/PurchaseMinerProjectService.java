package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.*;

public interface PurchaseMinerProjectService {

    /**
     * 创建购买矿机项目
     */
    SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd);

    /**
     * 分页查询购买矿机项目
     */
    MultiResponse<PurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry);

    /**
     * 首页-》矿机的相关统计数据
     */
    SingleResponse<PurchaseMinerProjectStatisticsDTO> statistics();


    /**
     * 根据天数查询奖励数据
     */
    SingleResponse<PurchaseMinerProjectRewardDTO> reward(PurchaseMinerProjectRewardQry purchaseMinerProjectRewardQry);


    /**
     * 检查昨日奖励服务费
     */
    SingleResponse<RewardServiceResultDTO> checkRewardService(RewardServiceQry rewardServiceQry);

    /**
     * 获取支付方式
     */
    MultiResponse<PurchaseMinerBuyWayDTO> purchaseMinerBuyWayList(PurchaseMinerBuyWayQry purchaseMinerBuyWayQry);


    /**
     * 创建或更新支付方式
     */
    SingleResponse<Void> createPurchaseMinerBuyWay(PurchaseMinerBuyWayCreateCmd purchaseMinerBuyWayCreateCmd);


    /**
     * 算力统计
     */
    SingleResponse<ComputingPowerStatisticDTO> computingPowerStatistic(ComputingPowerStatisticQry computingPowerStatisticQry);

    /**
     * 查询伞下使用ESG-ECO方式购买不同等级矿机的数量
     */
    MultiResponse<MinerLevelStatisticsDTO> getSubordinateMinerStatistics(SubordinateMinerStatisticsQry qry);

}
