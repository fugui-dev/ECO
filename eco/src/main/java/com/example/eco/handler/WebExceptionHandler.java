package com.example.eco.handler;

import com.example.eco.bean.Response;
import com.example.eco.bean.SingleResponse;
import com.example.eco.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Response businessException(BusinessException exception) {
        log.warn("Business Error: {}", exception.getMessage());
        
        SingleResponse response = new SingleResponse();
        response.setCode(400);
        response.setErrMessage(exception.getMessage());
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response customerException(Exception exception) {
        log.error("System Error", exception);

        SingleResponse response = new SingleResponse();
        
        // 检查是否是业务异常（包含具体错误信息）
        if (exception.getMessage() != null && 
            (exception.getMessage().contains("支付失败") || 
             exception.getMessage().contains("余额不足") ||
             exception.getMessage().contains("账户不存在"))) {
            response.setCode(400);
            response.setErrMessage(exception.getMessage());
        } else {
            response.setCode(500);
            response.setErrMessage("System Error, Please Try Again Later");
        }
        
        return response;
    }
}
