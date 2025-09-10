package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;

public interface PurchaseMinerProjectService {

    /**
     * 创建购买矿机项目
     */
    SingleResponse<Void> create(PurchaseMinerProjectsCreateCmd purchaseMinerProjectsCreateCmd);

    /**
     * 分页查询购买矿机项目
     */
    MultiResponse<PurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry);

}
