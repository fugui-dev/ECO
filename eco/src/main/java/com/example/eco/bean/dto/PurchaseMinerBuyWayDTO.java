package com.example.eco.bean.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class PurchaseMinerBuyWayDTO {

    /**
     * key名称
     */
    private String name;

    /**
     * key值
     */
    private String value;


    private Integer status;
}
