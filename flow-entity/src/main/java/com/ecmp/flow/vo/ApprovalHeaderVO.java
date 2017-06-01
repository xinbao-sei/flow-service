package com.ecmp.flow.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/5/25 20:38      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
public class ApprovalHeaderVO implements Serializable{
    /**
     * 业务单号
     */
    private String businessId;
    /**
     * 创单人
     */
    private String createUser;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 上一步执行人
     */
    private String prUser;
    /**
     * 上一步审批意见
     */
    private String prOpinion;

    /**
     * 上一步执行时间
     */
    private Date preCreateTime;


    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getPrUser() {
        return prUser;
    }

    public void setPrUser(String prUser) {
        this.prUser = prUser;
    }

    public String getPrOpinion() {
        return prOpinion;
    }

    public void setPrOpinion(String prOpinion) {
        this.prOpinion = prOpinion;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getPreCreateTime() {
        return preCreateTime;
    }

    public void setPreCreateTime(Date preCreateTime) {
        this.preCreateTime = preCreateTime;
    }
}