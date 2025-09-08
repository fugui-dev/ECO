package com.example.eco.bean;

/**
 * page query
 * <p/>
 * Created by Danny.Lee on 2017/11/1.
 */
public class PageQuery {

    private int pageNum = 1;
    private int pageSize = 10;
    private boolean needTotalCount = true;


    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isNeedTotalCount() {
        return needTotalCount;
    }

    public void setNeedTotalCount(boolean needTotalCount) {
        this.needTotalCount = needTotalCount;
    }


}
