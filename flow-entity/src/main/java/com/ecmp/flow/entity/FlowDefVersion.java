package com.ecmp.flow.entity;

import com.ecmp.core.entity.ITenant;
import com.ecmp.flow.constant.FlowDefinationStatus;
import com.ecmp.flow.vo.SolidifyStartExecutorVo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.*;


/**
 * 流程版本
 */
@Entity
@Table(name = "flow_def_version", uniqueConstraints = @UniqueConstraint(columnNames = "def_key"))
public class FlowDefVersion extends com.ecmp.core.entity.BaseAuditableEntity implements Cloneable, ITenant {


    /**
     * 乐观锁-版本
     */
    @Column(name = "version")
    private Integer version = 0;

    /**
     * 所属流程定义
     */
    @ManyToOne()
    @JoinColumn(name = "flow_defination_id")
    private FlowDefination flowDefination;


    /**
     * 定义ID
     */

    @Column(name = "act_def_id")
    private String actDefId;

    /**
     * 定义KEY
     */
    @Column(name = "def_key")
    private String defKey;

    /**
     * 名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 部署ID
     */
    @Column(name = "act_deploy_id")
    private String actDeployId;

    /**
     * 启动条件UEL
     */
    @Column(name = "start_uel")
    private String startUel;

    /**
     * 版本号
     */
    @Column(name = "version_code")
    private Integer versionCode;

    /**
     * 优先级
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * 流程JSON文本
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "def_json", columnDefinition = "CLOB")
    private String defJson;


    /**
     * 租户代码
     */
    @Column(name = "tenant_code")
    private String tenantCode;


    /**
     * 最终定义XML
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "def_xml", columnDefinition = "CLOB")
    private String defXml;

    /**
     * 描述
     */
    @Column(name = "depict")
    private String depict;

    /**
     * 当前流程版本状态
     */
    @Column(name = "flow_defination_status")
    private FlowDefinationStatus flowDefinationStatus;

    /**
     * 拥有的流程实例
     */
    @Transient
    private Set<FlowInstance> flowInstances = new HashSet<FlowInstance>(0);

    /**
     * 固化流程初始化时用的单个执行人列表
     */
    @Transient
    private Map<String, SolidifyStartExecutorVo> solidifyExecutorOfOnly;

    /**
     * 是否允许做为子流程来进行引用
     */
    @Column(name = "sub_process")
    private Boolean subProcess;

    /**
     * 是否为固化流程
     */
    @Column(name = "solidify_flow")
    private Boolean solidifyFlow;


    /**
     * 流程额定工时（小时）
     */
    @Column(name = "timing")
    private Integer timing;


    /**
     * 任务提前预警时间（小时）
     */
    @Column(name = "early_warning_time")
    private Integer earlyWarningTime;


    /**
     * 启动时调用检查服务ID
     */
    @Column(name = "start_check_service_url")
    private String startCheckServiceUrlId;

    /**
     * 启动时调用检查服务名称
     */
    @Column(name = "start_check_service_name")
    private String startCheckServiceUrlName;

    /**
     * 启动时调用检查服务是否异步
     */
    @Column(name = "start_check_service_aync")
    private Boolean startCheckServiceAync;


    /**
     * 启动完成时调用服务id
     */
    @Column(name = "start_after_service_id")
    private String afterStartServiceId;

    /**
     * 启动完成时调用服务名称
     */
    @Column(name = "start_after_service_name")
    private String afterStartServiceName;

    /**
     * 启动完成时调用服务是否异步
     */
    @Column(name = "start_after_service_aync")
    private Boolean afterStartServiceAync;

    /**
     * 流程结束前检查，调用服务ID
     */
    @Column(name = "end_before_call_service_url")
    private String endBeforeCallServiceUrlId;

    /**
     * 流程结束时，调用服务名称
     */
    @Column(name = "end_before_call_service_name")
    private String endBeforeCallServiceUrlName;


    /**
     * 流程结束前检查服务是否异步
     */
    @Column(name = "end_before_call_service_aync")
    private Boolean endBeforeCallServiceAync;

    /**
     * 流程结束时，调用服务，异步
     */
    @Column(name = "end_call_service_url")
    private String endCallServiceUrlId;

    /**
     * 流程结束时，调用服务名称，异步
     */
    @Column(name = "end_call_service_name")
    private String endCallServiceUrlName;


    /**
     * 流程结束调用服务是否异步
     */
    @Column(name = "end_call_service_aync")
    private Boolean endCallServiceAync;


    /**
     * default constructor
     */
    public FlowDefVersion() {
    }

    /**
     * minimal constructor
     */
    public FlowDefVersion(String defKey, String name) {
        this.defKey = defKey;
        this.name = name;

    }

    /**
     * full constructor
     */
    public FlowDefVersion(FlowDefination flowDefination, String defKey,
                          String name, String actDeployId, String startUel,
                          Integer versionCode, Integer priority, String defJson,
                          String defXml, String depict, Set<FlowInstance> flowInstances) {
        this.flowDefination = flowDefination;
        this.defKey = defKey;
        this.name = name;
        this.actDeployId = actDeployId;
        this.startUel = startUel;
        this.versionCode = versionCode;
        this.priority = priority;
        this.defJson = defJson;
        this.defXml = defXml;
        this.depict = depict;
        this.flowInstances = flowInstances;
    }


    public Integer getTiming() {
        return timing;
    }

    public void setTiming(Integer timing) {
        this.timing = timing;
    }

    public Integer getEarlyWarningTime() {
        return earlyWarningTime;
    }

    public void setEarlyWarningTime(Integer earlyWarningTime) {
        this.earlyWarningTime = earlyWarningTime;
    }

    public FlowDefination getFlowDefination() {
        return this.flowDefination;
    }

    public void setFlowDefination(FlowDefination flowDefination) {
        this.flowDefination = flowDefination;
    }

    public String getActDefId() {
        return actDefId;
    }

    public void setActDefId(String actDefId) {
        this.actDefId = actDefId;
    }

    public String getDefKey() {
        return this.defKey;
    }

    public void setDefKey(String defKey) {
        this.defKey = defKey;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActDeployId() {
        return this.actDeployId;
    }

    public void setActDeployId(String actDeployId) {
        this.actDeployId = actDeployId;
    }

    public String getStartUel() {
        return this.startUel;
    }

    public void setStartUel(String startUel) {
        this.startUel = startUel;
    }

    public Integer getVersionCode() {
        return this.versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDefJson() {
        return this.defJson;
    }

    public void setDefJson(String defJson) {
        this.defJson = defJson;
    }

    public String getDefXml() {
        return this.defXml;
    }

    public void setDefXml(String defXml) {
        this.defXml = defXml;
    }

    public String getDepict() {
        return this.depict;
    }

    public void setDepict(String depict) {
        this.depict = depict;
    }

    public Set<FlowInstance> getFlowInstances() {
        return this.flowInstances;
    }

    public void setFlowInstances(Set<FlowInstance> flowInstances) {
        this.flowInstances = flowInstances;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public FlowDefinationStatus getFlowDefinationStatus() {
        return flowDefinationStatus;
    }

    public void setFlowDefinationStatus(FlowDefinationStatus flowDefinationStatus) {
        this.flowDefinationStatus = flowDefinationStatus;
    }

    public Boolean getSubProcess() {
        return subProcess;
    }

    public void setSubProcess(Boolean subProcess) {
        this.subProcess = subProcess;
    }


    public Boolean getSolidifyFlow() {
        return solidifyFlow;
    }

    public void setSolidifyFlow(Boolean solidifyFlow) {
        this.solidifyFlow = solidifyFlow;
    }

    public String getStartCheckServiceUrlId() {
        return startCheckServiceUrlId;
    }

    public void setStartCheckServiceUrlId(String startCheckServiceUrlId) {
        this.startCheckServiceUrlId = startCheckServiceUrlId;
    }

    public String getEndCallServiceUrlId() {
        return endCallServiceUrlId;
    }

    public void setEndCallServiceUrlId(String endCallServiceUrlId) {
        this.endCallServiceUrlId = endCallServiceUrlId;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this)
                .append("id", this.getId())
                .append("flowDefination", flowDefination)
                .append("defKey", defKey)
                .append("name", name)
                .append("actDeployId", actDeployId)
                .append("startUel", startUel)
                .append("versionCode", versionCode)
                .append("priority", priority)
                .append("defJson", defJson)
                .append("startCheckServiceUrl", startCheckServiceUrlId)
                .append("startCheckServiceUrlName", startCheckServiceUrlName)
                .append("startCheckServiceAync", startCheckServiceAync)
                .append("afterStartServiceId", afterStartServiceId)
                .append("afterStartServiceName", afterStartServiceName)
                .append("afterStartServiceAync", afterStartServiceAync)
                .append("endBeforeCallServiceUrlId", endBeforeCallServiceUrlId)
                .append("endBeforeCallServiceUrlName", endBeforeCallServiceUrlName)
                .append("endBeforeCallServiceAync", endBeforeCallServiceAync)
                .append("endCallServiceUrl", endCallServiceUrlId)
                .append("endCallServiceUrlName", endCallServiceUrlName)
                .append("endCallServiceAync", endCallServiceAync)
                .append("defXml", defXml)
                .append("depict", depict)
                .append("flowInstances", flowInstances)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FlowDefVersion o = null;
        // Object中的clone()识别出你要复制的是哪一个对象。
        o = (FlowDefVersion) super.clone();
        return o;
    }

    public String getStartCheckServiceUrlName() {
        return startCheckServiceUrlName;
    }

    public void setStartCheckServiceUrlName(String startCheckServiceUrlName) {
        this.startCheckServiceUrlName = startCheckServiceUrlName;
    }

    public String getEndCallServiceUrlName() {
        return endCallServiceUrlName;
    }

    public void setEndCallServiceUrlName(String endCallServiceUrlName) {
        this.endCallServiceUrlName = endCallServiceUrlName;
    }

    public String getAfterStartServiceId() {
        return afterStartServiceId;
    }

    public void setAfterStartServiceId(String afterStartServiceId) {
        this.afterStartServiceId = afterStartServiceId;
    }

    public String getAfterStartServiceName() {
        return afterStartServiceName;
    }

    public void setAfterStartServiceName(String afterStartServiceName) {
        this.afterStartServiceName = afterStartServiceName;
    }

    public Boolean getAfterStartServiceAync() {
        return afterStartServiceAync;
    }

    public void setAfterStartServiceAync(Boolean afterStartServiceAync) {
        this.afterStartServiceAync = afterStartServiceAync;
    }

    public String getEndBeforeCallServiceUrlId() {
        return endBeforeCallServiceUrlId;
    }

    public void setEndBeforeCallServiceUrlId(String endBeforeCallServiceUrlId) {
        this.endBeforeCallServiceUrlId = endBeforeCallServiceUrlId;
    }

    public String getEndBeforeCallServiceUrlName() {
        return endBeforeCallServiceUrlName;
    }

    public void setEndBeforeCallServiceUrlName(String endBeforeCallServiceUrlName) {
        this.endBeforeCallServiceUrlName = endBeforeCallServiceUrlName;
    }

    public Map<String, SolidifyStartExecutorVo> getSolidifyExecutorOfOnly() {
        return solidifyExecutorOfOnly;
    }

    public void setSolidifyExecutorOfOnly(Map<String, SolidifyStartExecutorVo> solidifyExecutorOfOnly) {
        this.solidifyExecutorOfOnly = solidifyExecutorOfOnly;
    }

    public Boolean getStartCheckServiceAync() {
        return startCheckServiceAync;
    }

    public void setStartCheckServiceAync(Boolean startCheckServiceAync) {
        this.startCheckServiceAync = startCheckServiceAync;
    }

    public Boolean getEndBeforeCallServiceAync() {
        return endBeforeCallServiceAync;
    }

    public void setEndBeforeCallServiceAync(Boolean endBeforeCallServiceAync) {
        this.endBeforeCallServiceAync = endBeforeCallServiceAync;
    }

    public Boolean getEndCallServiceAync() {
        return endCallServiceAync;
    }

    public void setEndCallServiceAync(Boolean endCallServiceAync) {
        this.endCallServiceAync = endCallServiceAync;
    }

    @Override
    public String getTenantCode() {
        return tenantCode;
    }

    @Override
    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    @Override
    @JsonIgnore(false)
    public String getCreatorId() {
        return super.getCreatorId();
    }

    @Override
    @JsonIgnore(false)
    public String getCreatorAccount() {
        return super.getCreatorAccount();
    }

    @Override
    @JsonIgnore(false)
    public String getCreatorName() {
        return super.getCreatorName();
    }

    @Override
    @JsonIgnore(false)
    public Date getCreatedDate() {
        return super.getCreatedDate();
    }

    @Override
    @JsonIgnore(false)
    public String getLastEditorId() {
        return super.getLastEditorId();
    }

    @Override
    @JsonIgnore(false)
    public String getLastEditorAccount() {
        return super.getLastEditorAccount();
    }

    @Override
    @JsonIgnore(false)
    public String getLastEditorName() {
        return super.getLastEditorName();
    }

    @Override
    @JsonIgnore(false)
    public Date getLastEditedDate() {
        return super.getLastEditedDate();
    }
}