package com.example.eco.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.eco.bean.dto.BscScanAccountTransactionResponseDTO;
import com.example.eco.bean.dto.CurrencyPairDTO;
import com.example.eco.common.SystemConfigEnum;
import com.example.eco.model.entity.SystemConfig;
import com.example.eco.model.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ESGUtils {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    private String url = "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=ESG_USDT";

    public BigDecimal getEsgPrice(){

        try {

            LambdaQueryWrapper<SystemConfig> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(SystemConfig::getName, SystemConfigEnum.ESG_PRICE.getCode());

            SystemConfig systemConfig = systemConfigMapper.selectOne(lambdaQueryWrapper);

            if (Objects.nonNull(systemConfig)){
                return new BigDecimal(systemConfig.getValue());
            }else {

            String response = HttpUtil.get(url);

            List<CurrencyPairDTO> currencyPairList = JSONUtil.toList(response, CurrencyPairDTO.class);

            if (currencyPairList != null && !currencyPairList.isEmpty()) {
                return new BigDecimal(currencyPairList.get(0).getLast());
            } else {
                log.error("无法获取ESG价格，响应数据为空");
                return BigDecimal.ZERO;
            }
            }

        } catch (Exception e) {
            log.error("请求BSCscan API失败", e);
            return BigDecimal.ZERO;
        }
    }

}
