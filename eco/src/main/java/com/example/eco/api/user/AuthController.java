package com.example.eco.api.user;

import com.example.eco.annotation.NoJwtAuth;
import com.example.eco.bean.SingleResponse;
import com.example.eco.bean.cmd.AuthVerifyCmd;
import com.example.eco.bean.cmd.NonceGenerateCmd;
import com.example.eco.bean.dto.AuthDTO;
import com.example.eco.core.service.NonceService;
import com.example.eco.util.JwtUtil;
import com.example.eco.util.Web3SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 认证控制器 - Web3钱包登录
 */
@Slf4j
@RestController
@RequestMapping("/v1/user/auth")
@NoJwtAuth  // 整个认证控制器都不需要JWT验证
public class AuthController {
    
    @Resource
    private NonceService nonceService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private Web3SignatureUtil web3SignatureUtil;
    
    /**
     * 生成nonce - 对应原Node.js的 GET /auth/nonce/:address
     */
    @GetMapping("/nonce/{address}")
    public SingleResponse<AuthDTO> generateNonce(@PathVariable String address) {
        try {
            // 参数验证
            if (!StringUtils.hasText(address)) {
                return SingleResponse.buildFailure("钱包地址不能为空");
            }
            
            // 转换为小写
            address = address.toLowerCase();
            
            // 生成并保存nonce
            String nonce = nonceService.generateAndSaveNonce(address);
            
            // 构建响应
            AuthDTO authDTO = new AuthDTO();
            authDTO.setAddress(address);
            authDTO.setNonce(nonce);
            authDTO.setSuccess(true);
            
            log.info("生成nonce成功: address={}", address);
            return SingleResponse.of(authDTO);
            
        } catch (Exception e) {
            log.error("生成nonce失败: address={}", address, e);
            return SingleResponse.buildFailure("生成nonce失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证签名并登录 - 对应原Node.js的 POST /auth/verify
     */
    @PostMapping("/verify")
    public SingleResponse<AuthDTO> verifySignature(@RequestBody AuthVerifyCmd cmd) {
        try {
            // 参数验证
            if (!StringUtils.hasText(cmd.getAddress()) || !StringUtils.hasText(cmd.getSignature())) {
                return SingleResponse.buildFailure("钱包地址和签名不能为空");
            }
            
            String address = cmd.getAddress().toLowerCase();
            
            // 获取nonce
            String nonce = nonceService.getAndDeleteNonce(address);
            if (nonce == null) {
                return SingleResponse.buildFailure("nonce不存在或已过期，请重新获取");
            }
            
            // 构建登录消息
//            String message = web3SignatureUtil.buildLoginMessage(nonce);
            String message = nonce;
            
            // 验证签名
            boolean isValid = web3SignatureUtil.verifySignature(message, cmd.getSignature(), address);
            
            if (!isValid) {
                log.warn("签名验证失败: address={}", address);
                return SingleResponse.buildFailure("签名验证失败");
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(address);
            
            // 构建响应
            AuthDTO authDTO = new AuthDTO();
            authDTO.setAddress(address);
            authDTO.setToken(token);
            authDTO.setSuccess(true);
            
            log.info("登录成功: address={}", address);
            return SingleResponse.of(authDTO);
            
        } catch (Exception e) {
            log.error("验证签名失败: address={}", cmd.getAddress(), e);
            return SingleResponse.buildFailure("验证签名失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证token有效性
     */
    @PostMapping("/validate")
    public SingleResponse<AuthDTO> validateToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                return SingleResponse.buildFailure("无效的Authorization头");
            }
            
            String token = authorization.substring(7); // 移除"Bearer "前缀
            
            // 验证token
            boolean isValid = jwtUtil.validateToken(token);
            if (!isValid) {
                return SingleResponse.buildFailure("token无效或已过期");
            }
            
            // 获取地址
            String address = jwtUtil.getAddressFromToken(token);
            
            AuthDTO authDTO = new AuthDTO();
            authDTO.setAddress(address);
            authDTO.setSuccess(true);
            
            return SingleResponse.of(authDTO);
            
        } catch (Exception e) {
            log.error("验证token失败", e);
            return SingleResponse.buildFailure("验证token失败: " + e.getMessage());
        }
    }
}
