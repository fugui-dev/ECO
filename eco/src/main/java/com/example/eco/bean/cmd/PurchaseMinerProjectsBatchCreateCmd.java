package com.example.eco.bean.cmd;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseMinerProjectsBatchCreateCmd {

    private List<PurchaseMinerProjectsCreateCmd> purchaseMinerProjectsCreateCmdList;
}
