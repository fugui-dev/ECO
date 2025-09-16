package com.example.eco.api.admin;

import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.cmd.PendOrderPageQry;
import com.example.eco.bean.dto.PendOrderDTO;
import com.example.eco.core.service.PendOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/v1/admin/pend/order")
public class AdminPendOrderController {

    @Resource
    private PendOrderService pendOrderService;

    /**
     * 分页查询挂单
     */
    @PostMapping("/page")
    MultiResponse<PendOrderDTO> page(@RequestBody PendOrderPageQry pendOrderPageQry){
        return pendOrderService.page(pendOrderPageQry);
    }


}
