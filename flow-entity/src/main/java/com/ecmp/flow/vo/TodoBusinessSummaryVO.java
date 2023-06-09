package com.ecmp.flow.vo;

import java.io.Serializable;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能： 待办汇总VO对象
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/6/14 13:59      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
public class TodoBusinessSummaryVO implements Serializable {
    /**
     * 业务实体类型id
     */
    private String businessModeId;

    /**
     * 业务实体类型代码(类全路径)
     */
    private String businessModelCode;

    /**
     * 业务单据数据
     */
    private int count;

    /**
     * 业务实体类型名称
     */
    private String businessModelName;


    /**
     * 应用模块排序
     */
    private int appRank;

    /**
     * 业务实体排序
     */
    private int businessRank;



    public String getBusinessModeId() {
        return businessModeId;
    }

    public void setBusinessModeId(String businessModeId) {
        this.businessModeId = businessModeId;
    }

    public String getBusinessModelCode() {
        return businessModelCode;
    }

    public void setBusinessModelCode(String businessModelCode) {
        this.businessModelCode = businessModelCode;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getBusinessModelName() {
        return businessModelName;
    }

    public void setBusinessModelName(String businessModelName) {
        this.businessModelName = businessModelName;
    }

    public int getAppRank() {
        return appRank;
    }

    public void setAppRank(int appRank) {
        this.appRank = appRank;
    }

    public int getBusinessRank() {
        return businessRank;
    }

    public void setBusinessRank(int businessRank) {
        this.businessRank = businessRank;
    }
}
