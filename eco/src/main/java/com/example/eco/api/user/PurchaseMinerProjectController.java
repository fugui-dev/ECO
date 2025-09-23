package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectRewardDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectStatisticsDTO;
import com.example.eco.core.service.PurchaseMinerProjectService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/user/purchase/miner/project")
public class PurchaseMinerProjectController {

    @Resource
    private PurchaseMinerProjectService purchaseMinerProjectService;


    /**
     * 首页 -》 我的资产 （购买几台矿机，总算力）
     */
    @PostMapping("/page")
    MultiResponse<PurchaseMinerProjectDTO> page(@RequestBody PurchaseMinerProjectPageQry purchaseMinerProjectPageQry) {
        return purchaseMinerProjectService.page(purchaseMinerProjectPageQry);
    }


    /**
     * 创建购买矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {
        return purchaseMinerProjectService.create(purchaseMinerProjectsCreateCmd);
    }

    /**
     * 首页-》矿机的相关统计数据
     */
    @GetMapping("/statistics")
    SingleResponse<PurchaseMinerProjectStatisticsDTO> statistics(){
        return purchaseMinerProjectService.statistics();
    }


    /**
     * 根据天数查询奖励数据
     */
    @PostMapping("/reward")
    SingleResponse<PurchaseMinerProjectRewardDTO> reward(@RequestBody PurchaseMinerProjectRewardQry purchaseMinerProjectRewardQry){
        return purchaseMinerProjectService.reward(purchaseMinerProjectRewardQry);
    }


}
