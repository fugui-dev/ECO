package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectRewardDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectStatisticsDTO;

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


}
