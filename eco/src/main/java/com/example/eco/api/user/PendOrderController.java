package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.MultiResponse;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.*;
import com.example.eco.bean.dto.PendOrderAppealDTO;
import com.example.eco.bean.dto.PendOrderDTO;
import com.example.eco.core.service.PendOrderService;
import com.example.eco.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/v1/user/pend/order")
public class PendOrderController {

    @Resource
    private PendOrderService pendOrderService;

    /**
     * 分页查询挂单
     */
    @PostMapping("/page")
    MultiResponse<PendOrderDTO> page(@RequestBody PendOrderPageQry pendOrderPageQry){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }
        
        // 设置钱包地址到查询条件中
        pendOrderPageQry.setWalletAddress(walletAddress);
        log.info("分页查询挂单: walletAddress={}", walletAddress);
        
        return pendOrderService.page(pendOrderPageQry);
    }

    /**
     * 创建挂单
     *
     */
    @PostMapping("/create")
    SingleResponse<Void> createPendOrder(@RequestBody PendOrderCreateCmd pendOrderCreateCmd){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }
        
        // 设置钱包地址
        pendOrderCreateCmd.setWalletAddress(walletAddress);
        log.info("创建挂单: walletAddress={}, type={}, number={}", 
                walletAddress, pendOrderCreateCmd.getType(), pendOrderCreateCmd.getNumber());
        
        return pendOrderService.createPendOrder(pendOrderCreateCmd);
    }

    /**
     * 删除挂单
     *
     */
    @PostMapping("/delete")
    SingleResponse<Void> deletePendOrder(@RequestBody PendOrderDeleteCmd pendOrderDeleteCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        pendOrderDeleteCmd.setWalletAddress(walletAddress);

        return pendOrderService.deletePendOrder(pendOrderDeleteCmd);
    }

    /**
     * 锁定挂单
     *
     */
    @PostMapping("/lock")
    SingleResponse<Void> lockPendOrder(@RequestBody PendOrderLockCmd pendOrderLockCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        pendOrderLockCmd.setWalletAddress(walletAddress);

        return pendOrderService.lockPendOrder(pendOrderLockCmd);
    }


    /**
     * 取消挂单
     *
     */
    @PostMapping("/cancel")
    SingleResponse<Void> cancelPendOrder(@RequestBody PendOrderCancelCmd pendOrderCancelCmd){
        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        pendOrderCancelCmd.setWalletAddress(walletAddress);

        return pendOrderService.cancelPendOrder(pendOrderCancelCmd);
    }

    /**
     * 完成挂单
     *
     */
    @PostMapping("/complete")
    SingleResponse<Void> completePendOrder(@RequestBody PendOrderCompleteCmd pendOrderCompleteCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        pendOrderCompleteCmd.setWalletAddress(walletAddress);

        return pendOrderService.completePendOrder(pendOrderCompleteCmd);
    }

    /**
     * 上传支付凭证
     *
     */
    @PostMapping("/upload/voucher")
    SingleResponse<Void> uploadVoucher(@RequestBody PendOrderUploadVoucherCmd pendOrderUploadVoucherCmd){
        return pendOrderService.uploadVoucher(pendOrderUploadVoucherCmd);
    }


    /**
     * 创建申诉
     */
    @PostMapping("/appeal/create")
    SingleResponse<Void> appealCreate(@RequestBody PendOrderAppealCreateCmd pendOrderAppealCreateCmd){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return SingleResponse.buildFailure("用户未登录");
        }

        pendOrderAppealCreateCmd.setWalletAddress(walletAddress);

        return pendOrderService.appealCreate(pendOrderAppealCreateCmd);
    }



    /**
     * 分页查询申诉
     */
    @PostMapping("/appeal/page")
    MultiResponse<PendOrderAppealDTO> appealPage(@RequestBody PendOrderAppealPageQry pendOrderAppealPageQry){

        // 从JWT token中获取钱包地址
        String walletAddress = UserContextUtil.getCurrentWalletAddress();
        if (walletAddress == null) {
            log.warn("获取当前用户钱包地址失败");
            return MultiResponse.buildFailure("400","用户未登录");
        }

        // 设置钱包地址到查询条件中
        pendOrderAppealPageQry.setWalletAddress(walletAddress);
        log.info("分页查询挂单: walletAddress={}", walletAddress);

        return pendOrderService.appealPage(pendOrderAppealPageQry);
    }

}
