package com.example.eco.core.service;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.EsgPurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.cmd.PurchaseMinerProjectPageQry;
import com.example.eco.bean.cmd.PurchaseMinerProjectsCreateCmd;
import com.example.eco.bean.dto.EsgPurchaseMinerProjectDTO;
import com.example.eco.bean.dto.PurchaseMinerProjectDTO;

public interface EsgPurchaseMinerProjectService {

    /**
     * 创建购买矿机项目
     */
    SingleResponse<Void> create(EsgPurchaseMinerProjectsCreateCmd esgPurchaseMinerProjectsCreateCmd);

    /**
     * 分页查询购买矿机项目
     */
    MultiResponse<EsgPurchaseMinerProjectDTO> page(PurchaseMinerProjectPageQry purchaseMinerProjectPageQry);
}
