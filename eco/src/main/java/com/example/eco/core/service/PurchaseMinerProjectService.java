package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectRewardDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectStatisticsDTO;
import com.example.eco.bean.dto.RewardServiceResultDTO;

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


}
