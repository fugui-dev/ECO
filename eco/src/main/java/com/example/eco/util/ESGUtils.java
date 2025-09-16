package com.example.eco.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.example.eco.bean.dto.BscScanAccountTransactionResponseDTO;
import com.example.eco.bean.dto.CurrencyPairDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class ESGUtils {

    private String url = "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=ESG_USDT";

    public BigDecimal getEsgPrice(){

        try {

            String response = HttpUtil.get(url);

            List<CurrencyPairDTO> currencyPairList = JSONUtil.toList(response, CurrencyPairDTO.class);

            if (currencyPairList != null && !currencyPairList.isEmpty()) {
                return new BigDecimal(currencyPairList.get(0).getLast());
            } else {
                log.error("无法获取ESG价格，响应数据为空");
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("请求BSCscan API失败", e);
            return BigDecimal.ZERO;
        }
    }

}
