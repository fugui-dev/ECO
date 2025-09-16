package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;
import com.example.eco.core.service.PurchaseMinerProjectService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/purchase/miner/project")
public class PurchaseMinerProjectController {

    @Resource
    private PurchaseMinerProjectService purchaseMinerProjectService;


    /**
     * 分页查询购买矿机项目
     */
    @PostMapping("/page")
    MultiResponse<PurchaseMinerProjectDTO> page(@RequestBody PurchaseMinerProjectPageQry purchaseMinerProjectPageQry){
        return purchaseMinerProjectService.page(purchaseMinerProjectPageQry);
    }
}
