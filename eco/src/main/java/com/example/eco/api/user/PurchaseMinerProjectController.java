package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.*;
import com.example.eco.common.PurchaseMinerType;
import com.example.eco.core.service.PurchaseMinerProjectService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
@Slf4j
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

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }

        purchaseMinerProjectPageQry.setWalletAddress(walletAddress);

        return purchaseMinerProjectService.page(purchaseMinerProjectPageQry);
    }


    /**
     * 创建购买矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd) {

        if (purchaseMinerProjectsCreateCmd.getType().equals(PurchaseMinerType.AIRDROP.getCode())){
            return SingleResponse.buildFailure("支付类型错误");
        }

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        purchaseMinerProjectsCreateCmd.setWalletAddress(walletAddress);

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



    /**
     * 检查昨日奖励服务费
     */
    @PostMapping("/check/reward/service")
    SingleResponse<RewardServiceResultDTO> checkRewardService(@RequestBody RewardServiceQry rewardServiceQry){
        return purchaseMinerProjectService.checkRewardService(rewardServiceQry);
    }

    /**
     * 获取支付方式
     */
    @PostMapping("/buy/way/list")
    MultiResponse<PurchaseMinerBuyWayDTO> purchaseMinerBuyWayList(@RequestBody PurchaseMinerBuyWayQry purchaseMinerBuyWayQry){
        purchaseMinerBuyWayQry.setStatus(1);
        return purchaseMinerProjectService.purchaseMinerBuyWayList(purchaseMinerBuyWayQry);
    }

}
