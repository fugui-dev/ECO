package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectRewardCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectsBatchCreateCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectsBatchCreateResultDTO;
import com.example.eco.common.PurchaseMinerType;
import com.example.eco.core.cmd.RewardConstructor;
import com.example.eco.core.service.PurchaseMinerProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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
}
