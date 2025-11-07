package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.EsgMinerProjectCreateCmd;
import com.example.eco.bean.cmd.EsgMinerProjectUpdateCmd;
import com.example.eco.bean.cmd.MinerProjectDeleteCmd;
import com.example.eco.bean.cmd.MinerProjectPageQry;
import com.example.eco.bean.dto.EsgMinerProjectDTO;
import com.example.eco.bean.dto.EsgPurchaseMinerProjectStatisticDTO;
import com.example.eco.core.service.EsgMinerProjectService;
import com.example.eco.core.service.EsgPurchaseMinerProjectService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/esg/miner/project")
public class AdminEsgMinerProjectController {


    @Resource
    private EsgMinerProjectService esgMinerProjectService;

    @Resource
    private EsgPurchaseMinerProjectService esgPurchaseMinerProjectService;



    /**
     * 创建矿机项目
     */
    @PostMapping("/create")
    SingleResponse<Void> create(@RequestBody EsgMinerProjectCreateCmd esgMinerProjectCreateCmd){
        return esgMinerProjectService.create(esgMinerProjectCreateCmd);
    }

    /**
     * 更新矿机项目
     */
    @PostMapping("/update")
    SingleResponse<Void> update(@RequestBody EsgMinerProjectUpdateCmd esgMinerProjectUpdateCmd){
        return esgMinerProjectService.update(esgMinerProjectUpdateCmd);
    }


    /**
     * 删除矿机项目
     */
    @PostMapping("/delete")
    SingleResponse<Void> delete(@RequestBody MinerProjectDeleteCmd minerProjectDeleteCmd){
        return esgMinerProjectService.delete(minerProjectDeleteCmd);
    }

    /**
     * 分页查询矿机项目
     */
    @PostMapping("/page")
    MultiResponse<EsgMinerProjectDTO> page(@RequestBody MinerProjectPageQry minerProjectPageQry){
        return esgMinerProjectService.page(minerProjectPageQry);
    }

    /**
     * 统计购买矿机项目
     */
    @GetMapping("/statistic")
    SingleResponse<EsgPurchaseMinerProjectStatisticDTO> statistic(){
        return esgPurchaseMinerProjectService.statistic();
    }
}
