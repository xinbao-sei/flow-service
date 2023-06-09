package com.ecmp.flow.vo.push;

import com.ecmp.flow.entity.WorkPageUrl;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

public class PushFlowTaskVo implements Serializable {

    protected String id;
    /**
     * 创建者
     */
    protected String creatorId;

    protected String creatorAccount;

    protected String creatorName;

    /**
     * 创建时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date createdDate;

    /**
     * 最后修改者
     */
    protected String lastEditorId;

    protected String lastEditorAccount;

    protected String lastEditorName;

    /**
     * 最后修改时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date lastEditedDate;


    /**
     * 移动端url
     */
    private String phoneUrl;

    /**
     * 推送的已办审批状态（审批任务：agree/disagree）
     */
    private String approveStatus;

    /**
     * 推送的待办是否为自动处理
     */
    private Boolean newTaskAuto;

    /**
     * 表单相对路径
     */
    private String taskFormUrlXiangDui;

    /**
     * web基地址（跟随节点配置的工作界面模块）
     */
    private String webBaseAddress;


    /**
     * web基地址绝对路径（跟随节点配置的工作界面模块）
     */
    private String webBaseAddressAbsolute;


    /**
     * web基地址
     */
    private String lookWebBaseAddress;


    /**
     * web基地址绝对路径
     */
    private String lookWebBaseAddressAbsolute;

    /**
     * api基地址
     */
    private String apiBaseAddress;

    /**
     * api基地址绝对路径
     */
    private String apiBaseAddressAbsolute;


    /**
     * 提交任务地址（react不在使用）
     */
    private String completeTaskServiceUrl;

    /**
     * 表单明细地址
     */
    private String businessDetailServiceUrl;

    /**
     * 乐观锁- 版本
     */
    private Integer version = 0;

    /**
     * 所属流程实例
     */
    private PushInstanceVo flowInstance;

    /**
     * 名称
     */
    private String flowName;

    /**
     * 任务名
     */
    private String taskName;

    /**
     * 任务定义KEY
     */
    private String actTaskDefKey;

    /**
     * 任务表单URL
     */
    private String taskFormUrl;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 代理状态
     */
    private String proxyStatus;


    /**
     * 所属流程实例
     */
    private WorkPageUrl workPageUrl;


    /**
     * 流程定义ID
     */
    private String flowDefinitionId;

    /**
     * 关联的实际流程引擎任务ID
     */
    private String actTaskId;

    /**
     * 执行人名称
     */
    private String executorName;

    /**
     * 执行人账号
     */
    private String executorAccount;


    /**
     * 候选人账号
     */
    private String candidateAccount;

    /**
     * 执行时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date executeDate;


    /**
     * 描述
     */
    private String depict;


    /**
     * activtiti对应任务类型,如assinge、candidate
     */
    private String actType;

    /**
     * 流程任务引擎实际的任务签收时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date actClaimTime;


    /**
     * 优先级：0是普通    1是驳回    2是撤回    3是加急
     */
    private int priority;

    /**
     * 任务所属人账号（拥有人）
     */
    private String ownerAccount;

    /**
     * 任务所属人名称（拥有人）
     */
    private String ownerName;


    /**
     * 流程引擎的实际触发时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date actDueDate;


    /**
     * 流程引擎的实际任务定义KEY
     */
    private String actTaskKey;

    /**
     * 记录上一个流程历史任务的id
     */
    private String preId;


    /**
     * 是否允许驳回
     */
    private Boolean canReject;

    /**
     * 是否允许流程中止（退出流程）
     */
    private Boolean canSuspension;

    /**
     * 任务定义JSON
     */
    private String taskJsonDef;


    /**
     * 额定工时（分钟）
     */
    private Integer executeTime;


    /**
     * 执行人ID
     */
    private String executorId;


    /**
     * 候选人ID
     */
    private String candidateId;


    /**
     * 候选人ID
     */
    private String ownerId;


    /**
     * 能否批量审批
     */
    private Boolean canBatchApproval;

    /**
     * 移动端能否
     */
    private Boolean canMobile;

    /**
     * 1：发起委托的任务，2：被委托的任务，3：委托完成
     * 0：转办的任务（现取消，转办后任然可以转办）
     */
    private Integer trustState;

    /**
     * 被委托的任务id
     */
    private String trustOwnerTaskId;

    /**
     * 允许上一节点加签
     */
    private Boolean allowAddSign;


    /**
     * 允许上一节点减签
     */
    private Boolean allowSubtractSign;


    /**
     * 允许本节点加签
     */
    private Boolean allowCurrentAddSign;


    /**
     * 执行后返回上一节点
     */
    private Boolean jumpBackPrevious;


    /**
     * 租户代码
     */
    private String tenantCode;

    /**
     * 执行人组织机构ID
     */
    private String executorOrgId;

    /**
     * 执行人组织机构code
     */
    private String executorOrgCode;

    /**
     * 执行人组织机构名称
     */
    private String executorOrgName;

    /**
     * 拥有者组织机构ID
     */
    private String ownerOrgId;

    /**
     * 拥有者组织机构code
     */
    private String ownerOrgCode;

    /**
     * 拥有者组织机构名称
     */
    private String ownerOrgName;


    /**
     * 任务额定工时（小时）
     */
    private Double timing;


    /**
     * 预警状态
     */
    private String warningStatus;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorAccount() {
        return creatorAccount;
    }

    public void setCreatorAccount(String creatorAccount) {
        this.creatorAccount = creatorAccount;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastEditorId() {
        return lastEditorId;
    }

    public void setLastEditorId(String lastEditorId) {
        this.lastEditorId = lastEditorId;
    }

    public String getLastEditorAccount() {
        return lastEditorAccount;
    }

    public void setLastEditorAccount(String lastEditorAccount) {
        this.lastEditorAccount = lastEditorAccount;
    }

    public String getLastEditorName() {
        return lastEditorName;
    }

    public void setLastEditorName(String lastEditorName) {
        this.lastEditorName = lastEditorName;
    }

    public Date getLastEditedDate() {
        return lastEditedDate;
    }

    public void setLastEditedDate(Date lastEditedDate) {
        this.lastEditedDate = lastEditedDate;
    }

    public String getPhoneUrl() {
        return phoneUrl;
    }

    public void setPhoneUrl(String phoneUrl) {
        this.phoneUrl = phoneUrl;
    }

    public String getApproveStatus() {
        return approveStatus;
    }

    public void setApproveStatus(String approveStatus) {
        this.approveStatus = approveStatus;
    }

    public Boolean getNewTaskAuto() {
        return newTaskAuto;
    }

    public void setNewTaskAuto(Boolean newTaskAuto) {
        this.newTaskAuto = newTaskAuto;
    }

    public String getTaskFormUrlXiangDui() {
        return taskFormUrlXiangDui;
    }

    public void setTaskFormUrlXiangDui(String taskFormUrlXiangDui) {
        this.taskFormUrlXiangDui = taskFormUrlXiangDui;
    }

    public String getWebBaseAddress() {
        return webBaseAddress;
    }

    public void setWebBaseAddress(String webBaseAddress) {
        this.webBaseAddress = webBaseAddress;
    }

    public String getWebBaseAddressAbsolute() {
        return webBaseAddressAbsolute;
    }

    public void setWebBaseAddressAbsolute(String webBaseAddressAbsolute) {
        this.webBaseAddressAbsolute = webBaseAddressAbsolute;
    }

    public String getLookWebBaseAddress() {
        return lookWebBaseAddress;
    }

    public void setLookWebBaseAddress(String lookWebBaseAddress) {
        this.lookWebBaseAddress = lookWebBaseAddress;
    }

    public String getLookWebBaseAddressAbsolute() {
        return lookWebBaseAddressAbsolute;
    }

    public void setLookWebBaseAddressAbsolute(String lookWebBaseAddressAbsolute) {
        this.lookWebBaseAddressAbsolute = lookWebBaseAddressAbsolute;
    }

    public String getApiBaseAddress() {
        return apiBaseAddress;
    }

    public void setApiBaseAddress(String apiBaseAddress) {
        this.apiBaseAddress = apiBaseAddress;
    }

    public String getApiBaseAddressAbsolute() {
        return apiBaseAddressAbsolute;
    }

    public void setApiBaseAddressAbsolute(String apiBaseAddressAbsolute) {
        this.apiBaseAddressAbsolute = apiBaseAddressAbsolute;
    }

    public String getCompleteTaskServiceUrl() {
        return completeTaskServiceUrl;
    }

    public void setCompleteTaskServiceUrl(String completeTaskServiceUrl) {
        this.completeTaskServiceUrl = completeTaskServiceUrl;
    }

    public String getBusinessDetailServiceUrl() {
        return businessDetailServiceUrl;
    }

    public void setBusinessDetailServiceUrl(String businessDetailServiceUrl) {
        this.businessDetailServiceUrl = businessDetailServiceUrl;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public PushInstanceVo getFlowInstance() {
        return flowInstance;
    }

    public void setFlowInstance(PushInstanceVo  flowInstance) {
        this.flowInstance = flowInstance;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getActTaskDefKey() {
        return actTaskDefKey;
    }

    public void setActTaskDefKey(String actTaskDefKey) {
        this.actTaskDefKey = actTaskDefKey;
    }

    public String getTaskFormUrl() {
        return taskFormUrl;
    }

    public void setTaskFormUrl(String taskFormUrl) {
        this.taskFormUrl = taskFormUrl;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getProxyStatus() {
        return proxyStatus;
    }

    public void setProxyStatus(String proxyStatus) {
        this.proxyStatus = proxyStatus;
    }

    public WorkPageUrl getWorkPageUrl() {
        return workPageUrl;
    }

    public void setWorkPageUrl(WorkPageUrl workPageUrl) {
        this.workPageUrl = workPageUrl;
    }

    public String getFlowDefinitionId() {
        return flowDefinitionId;
    }

    public void setFlowDefinitionId(String flowDefinitionId) {
        this.flowDefinitionId = flowDefinitionId;
    }

    public String getActTaskId() {
        return actTaskId;
    }

    public void setActTaskId(String actTaskId) {
        this.actTaskId = actTaskId;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getExecutorAccount() {
        return executorAccount;
    }

    public void setExecutorAccount(String executorAccount) {
        this.executorAccount = executorAccount;
    }

    public String getCandidateAccount() {
        return candidateAccount;
    }

    public void setCandidateAccount(String candidateAccount) {
        this.candidateAccount = candidateAccount;
    }

    public Date getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(Date executeDate) {
        this.executeDate = executeDate;
    }

    public String getDepict() {
        return depict;
    }

    public void setDepict(String depict) {
        this.depict = depict;
    }

    public String getActType() {
        return actType;
    }

    public void setActType(String actType) {
        this.actType = actType;
    }

    public Date getActClaimTime() {
        return actClaimTime;
    }

    public void setActClaimTime(Date actClaimTime) {
        this.actClaimTime = actClaimTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getOwnerAccount() {
        return ownerAccount;
    }

    public void setOwnerAccount(String ownerAccount) {
        this.ownerAccount = ownerAccount;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Date getActDueDate() {
        return actDueDate;
    }

    public void setActDueDate(Date actDueDate) {
        this.actDueDate = actDueDate;
    }

    public String getActTaskKey() {
        return actTaskKey;
    }

    public void setActTaskKey(String actTaskKey) {
        this.actTaskKey = actTaskKey;
    }

    public String getPreId() {
        return preId;
    }

    public void setPreId(String preId) {
        this.preId = preId;
    }

    public Boolean getCanReject() {
        return canReject;
    }

    public void setCanReject(Boolean canReject) {
        this.canReject = canReject;
    }

    public Boolean getCanSuspension() {
        return canSuspension;
    }

    public void setCanSuspension(Boolean canSuspension) {
        this.canSuspension = canSuspension;
    }

    public String getTaskJsonDef() {
        return taskJsonDef;
    }

    public void setTaskJsonDef(String taskJsonDef) {
        this.taskJsonDef = taskJsonDef;
    }

    public Integer getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Integer executeTime) {
        this.executeTime = executeTime;
    }

    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Boolean getCanBatchApproval() {
        return canBatchApproval;
    }

    public void setCanBatchApproval(Boolean canBatchApproval) {
        this.canBatchApproval = canBatchApproval;
    }

    public Boolean getCanMobile() {
        return canMobile;
    }

    public void setCanMobile(Boolean canMobile) {
        this.canMobile = canMobile;
    }

    public Integer getTrustState() {
        return trustState;
    }

    public void setTrustState(Integer trustState) {
        this.trustState = trustState;
    }

    public String getTrustOwnerTaskId() {
        return trustOwnerTaskId;
    }

    public void setTrustOwnerTaskId(String trustOwnerTaskId) {
        this.trustOwnerTaskId = trustOwnerTaskId;
    }

    public Boolean getAllowAddSign() {
        return allowAddSign;
    }

    public void setAllowAddSign(Boolean allowAddSign) {
        this.allowAddSign = allowAddSign;
    }

    public Boolean getAllowSubtractSign() {
        return allowSubtractSign;
    }

    public void setAllowSubtractSign(Boolean allowSubtractSign) {
        this.allowSubtractSign = allowSubtractSign;
    }

    public Boolean getAllowCurrentAddSign() {
        return allowCurrentAddSign;
    }

    public void setAllowCurrentAddSign(Boolean allowCurrentAddSign) {
        this.allowCurrentAddSign = allowCurrentAddSign;
    }

    public Boolean getJumpBackPrevious() {
        return jumpBackPrevious;
    }

    public void setJumpBackPrevious(Boolean jumpBackPrevious) {
        this.jumpBackPrevious = jumpBackPrevious;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getExecutorOrgId() {
        return executorOrgId;
    }

    public void setExecutorOrgId(String executorOrgId) {
        this.executorOrgId = executorOrgId;
    }

    public String getExecutorOrgCode() {
        return executorOrgCode;
    }

    public void setExecutorOrgCode(String executorOrgCode) {
        this.executorOrgCode = executorOrgCode;
    }

    public String getExecutorOrgName() {
        return executorOrgName;
    }

    public void setExecutorOrgName(String executorOrgName) {
        this.executorOrgName = executorOrgName;
    }

    public String getOwnerOrgId() {
        return ownerOrgId;
    }

    public void setOwnerOrgId(String ownerOrgId) {
        this.ownerOrgId = ownerOrgId;
    }

    public String getOwnerOrgCode() {
        return ownerOrgCode;
    }

    public void setOwnerOrgCode(String ownerOrgCode) {
        this.ownerOrgCode = ownerOrgCode;
    }

    public String getOwnerOrgName() {
        return ownerOrgName;
    }

    public void setOwnerOrgName(String ownerOrgName) {
        this.ownerOrgName = ownerOrgName;
    }

    public Double getTiming() {
        return timing;
    }

    public void setTiming(Double timing) {
        this.timing = timing;
    }

    public String getWarningStatus() {
        return warningStatus;
    }

    public void setWarningStatus(String warningStatus) {
        this.warningStatus = warningStatus;
    }
}
