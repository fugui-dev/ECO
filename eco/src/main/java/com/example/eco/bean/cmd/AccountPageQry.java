package com.example.eco.bean.cmd;

import com.example.eco.bean.PageQuery;
import lombok.Data;

@Data
public class AccountPageQry extends PageQuery {

    private String walletAddress;

    private String type;
}
