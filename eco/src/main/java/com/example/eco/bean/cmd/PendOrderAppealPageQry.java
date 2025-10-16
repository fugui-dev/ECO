package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class PendOrderAppealPageQry extends PageQuery {

    private String status;


    private String walletAddress;


    private String order;
}
