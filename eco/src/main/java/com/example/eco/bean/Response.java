package com.example.eco.bean;

/**
 * Response to caller
 *
 * @author fulan.zjf 2017年10月21日 下午8:53:17
 */
public class Response {

    private static final long serialVersionUID = 1L;

    private boolean isSuccess;

    private Integer code;

    private String errMessage;


    public static Response buildFailure(String errCode, String errMessage, Integer rstCode) {
        Response response = new Response();
        response.setSuccess(false);
        response.setCode(-1);
        response.setErrMessage(errMessage);
        return response;
    }

    public static Response buildFailure(String errCode, String errMessage) {
        Response response = new Response();
        response.setSuccess(false);
        response.setCode(-1);
        response.setErrMessage(errMessage);
        return response;
    }

    public static Response buildSuccess() {
        Response response = new Response();
        response.setSuccess(true);
        response.setCode(1);
        return response;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    @Override
    public String toString() {
        return "Response [isSuccess=" + isSuccess + ", code=" + code + ", errMessage=" + errMessage + "]";
    }

}
