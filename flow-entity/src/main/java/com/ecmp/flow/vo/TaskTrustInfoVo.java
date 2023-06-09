package com.ecmp.flow.vo;


import java.io.Serializable;
import java.util.List;

public class TaskTrustInfoVo implements Serializable {


    /**
     * 被委托的任务ID
     */
    private String taskId;


    /**
     * 选择的用户ID集合
     */
    private List<String>  userIds;


    /**
     * 委托时输入的意见
     */
    private String opinion;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
}
