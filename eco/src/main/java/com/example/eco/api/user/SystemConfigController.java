package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.SystemConfigLogPageQry;
import com.example.eco.bean.cmd.SystemConfigUpdateCmd;
import com.example.eco.bean.dto.SystemConfigDTO;
import com.example.eco.bean.dto.SystemConfigLogDTO;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.core.service.SystemConfigService;
import com.example.eco.util.ESGUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/user/system/config")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private ESGUtils esgUtils;


    /**
     * 获取系统修改记录
     */
    @PostMapping("/log/list")
    MultiResponse<SystemConfigLogDTO> list(@RequestBody SystemConfigLogPageQry systemConfigLogPageQry){
        systemConfigLogPageQry.setName(SystemConfigEnum.ECO_PRICE.getCode());
        return systemConfigService.list(systemConfigLogPageQry);
    }

    /**
     * 获取ECO价格
     */
    @GetMapping("/eco/price")
    SingleResponse<SystemConfigDTO> getEcoPrice(){
        return systemConfigService.getEcoPrice();
    }


    /**
     * 获取ECO价格
     */
    @GetMapping("/esg/price")
    SingleResponse<SystemConfigDTO> getEsgPrice(){
        BigDecimal esgPrice = esgUtils.getEsgPrice();

        SystemConfigDTO systemConfigDTO = new SystemConfigDTO();
        systemConfigDTO.setName("ESG_PRICE");
        systemConfigDTO.setValue(esgPrice.toString());
        return SingleResponse.of(systemConfigDTO);
    }
}
