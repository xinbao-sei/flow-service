package com.ecmp.flow.entity;

import com.ecmp.annotation.Remark;
import com.ecmp.flow.constant.ConditionAnnotaion;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.vo.conditon.DefaultBusinessModelCondition;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.lang.reflect.InvocationTargetException;


/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * 工作流程抽象业务实体定义
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/05/26 10:20      谭军(tanjun)                新建
 * <p/>
 * *************************************************************************************************
 */
@MappedSuperclass
@Deprecated
public abstract class AbstractBusinessModel extends com.ecmp.core.entity.BaseAuditableEntity implements IBusinessFlowEntity{

    @Transient
    protected static final Log logger = LogFactory
            .getLog(AbstractBusinessModel.class);
    /**
     * 业务名称
     */
    @Column(name = "name",length = 80,nullable = false)
    private  String name;

    /**
     * 当前流程状态
     */
    @Column(name = "flow_status",length = 10,nullable = false)
    private FlowStatus flowStatus=FlowStatus.INIT;

    /**
     * 租户代码
     */
    @Column(name = "tenant_code",length = 10)
    private String tenantCode;

    /**
     * 组织机构代码
     */
    @Column(name = "org_code",length = 20)
    private String orgCode;

    /**
     * 组织机构Id
     */
    @Column(name = "org_id",length = 36)
    private String orgId;

    /**
     * 组织机构名称
     */
    @Column(name = "org_name",length = 80)
    private String orgName;

    /**
     * 组织机构层级路径
     */
    @Column(name = "org_path",length = 500)
    private String orgPath;

    /**
     * 优先级别
     */
    @Column(name = "priority")
    private int priority;

    /**
     * 工作说明
     */
    @Column(name = "work_caption",length = 1000,nullable = false)
    private  String workCaption;

    /**
     * 业务单号
     */
    @Column(name = "business_code",length = 2000,nullable = false)
    private  String businessCode;



    /**
     * 获取条件实体
     * @return
     */
   public IConditionPojo getConditionPojo() {
        IConditionPojo conditionPojo = new DefaultBusinessModelCondition();
       if(this.getId()!=null){
           try {
               BeanUtils.copyProperties(conditionPojo,this);
               conditionPojo.customLogic();
           } catch (IllegalAccessException e) {
               e.printStackTrace();
               logger.error(e.getMessage(),e);
               conditionPojo = null;
           } catch (InvocationTargetException e) {
               e.printStackTrace();
               logger.error(e.getMessage(),e);
               conditionPojo = null;
           }
       }
        return conditionPojo;
    }



    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public FlowStatus getFlowStatus() {
        return flowStatus;
    }

    @Override
    public void setFlowStatus(FlowStatus flowStatus) {
        this.flowStatus = flowStatus;
    }


    @Override
    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    @Override
    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @Override
    public String getOrgName() {
        return orgName;
    }

    @Override
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }


    @Override
    public void setOrgPath(String orgPath) {
        this.orgPath = orgPath;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }


    @Override
    public void setWorkCaption(String workCaption) {
        this.workCaption = workCaption;
    }


    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }



    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    @Override
    @ConditionAnnotaion(name="组织机构id",rank = -10000,canSee=false)
    public String getOrgId() {
        return this.orgId;
    }

    @Override
    @ConditionAnnotaion(name="组织机构代码",rank = -10000,canSee=false)
    public String getOrgCode() {
        return orgCode;
    }

    @Override
    @ConditionAnnotaion(name="组织机构路径",rank = -10000,canSee=false)
    public String getOrgPath() {
        return this.orgPath;
    }

    @Override
    @ConditionAnnotaion(name="租户代码",rank = -10000,canSee=false)
    public String getTenantCode() {
        return this.tenantCode;
    }

    @Override
    @ConditionAnnotaion(name="工作说明",rank = -10000,canSee=false)
    @Remark(value = "工作说明",rank = 1)
    public String getWorkCaption(){return this.workCaption;}

    @Override
    @ConditionAnnotaion(name="业务名称",rank = -10000,canSee=false)
    @Remark("业务名称")
    public String getName() {
        return name;
    }

    @ConditionAnnotaion(name="业务单号",rank = -10000,canSee=false)
    @Remark("业务单号")
    public String getBusinessCode() {
        return businessCode;
    }


    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", this.getId())
                .append("name", this.name)
                .append("orgPath", this.orgPath)
                .append("orgId", this.orgId)
                .append("orgName", this.orgName)
                .append("flowStatus", this.flowStatus)
                .append("priority", this.priority)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
