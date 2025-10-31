package com.example.eco.api.user;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.EsgPurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.dto.EsgPurchaseMinerProjectDTO;
import com.example.eco.core.service.EsgPurchaseMinerProjectService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/esg/purchase/miner/project")
public class EsgPurchaseMinerProjectController {

    @Resource
    private EsgPurchaseMinerProjectService esgPurchaseMinerProjectService;


    /**
     * 创建购买矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody EsgPurchaseMinerProjectsCreateCmd esgPurchaseMinerProjectsCreateCmd){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }
        esgPurchaseMinerProjectsCreateCmd.setWalletAddress(walletAddress);
        return esgPurchaseMinerProjectService.create(esgPurchaseMinerProjectsCreateCmd);
    }

    /**
     * 分页查询购买矿机项目
     */
    @PostMapping("/page")
    MultiResponse<EsgPurchaseMinerProjectDTO> page(@RequestBody PurchaseMinerProjectPageQry purchaseMinerProjectPageQry){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }
        purchaseMinerProjectPageQry.setWalletAddress(walletAddress);
        return esgPurchaseMinerProjectService.page(purchaseMinerProjectPageQry);
    }
}
