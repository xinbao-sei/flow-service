package com.ecmp.flow.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * 业务实体模型Entity定义
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/3/21 10:20      谭军(tanjun)                新建
 * <p/>
 * *************************************************************************************************
 */
@Entity(name = "business_model")
@DynamicInsert
@DynamicUpdate
public class BusinessModel extends com.ecmp.core.entity.BaseAuditableEntity {

    /**
     * 乐观锁-版本
     */
    //@Version
    @Column(name = "version")
    private Integer version = 0;

    /**
     * 名称
     */
    @Column(length = 80, nullable = false)
    private String name;

    /**
     * 类全路径
     */
    @Column(length = 255, nullable = false, unique = true, name = "class_name")
    private String className;


    /**
     * 转换对象，全路径
     */
    @Column(length = 255, name = "conditon_bean")
    private String conditonBean;

    /**
     * 数据访问对象,spring管理的bean名称
     */
    @Column(length = 80, name = "dao_bean")
    private String daoBean;


    /**
     * 描述
     */
    @Column(length = 250)
    private String depict;

//    /**
//     * 所属应用模块
//     */
//    @ManyToOne()
//    @JoinColumn(name = "app_module_id")
//    private AppModule appModule;

    /**
     * 关联的应用模块ID
     */
    @Column(length = 36,name = "app_module_id")
    private String appModuleId;

    /**
     * 关联的应用模块Code
     */
    @Column(length = 20,name = "app_module_code")
    private String appModuleCode;

    /**
     * 查看单据的URL
     */
    @Column(length = 6000,name = "look_url")
    @Lob
    private String lookUrl;

    /**
     * 拥有的流程类型
     */
    @Transient
//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "businessModel")
    private Set<FlowType> flowTypes = new HashSet<FlowType>();


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDepict() {
        return depict;
    }

    public void setDepict(String depict) {
        this.depict = depict;
    }

    public String getAppModuleId() {
        return appModuleId;
    }

    public void setAppModuleId(String appModuleId) {
        this.appModuleId = appModuleId;
    }

    public Set<FlowType> getFlowTypes() {
        return flowTypes;
    }

    public void setFlowTypes(Set<FlowType> flowTypes) {
        this.flowTypes = flowTypes;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDaoBean() {
        return daoBean;
    }

    public void setDaoBean(String daoBean) {
        this.daoBean = daoBean;
    }

    public String getAppModuleCode() {
        return appModuleCode;
    }

    public void setAppModuleCode(String appModuleCode) {
        this.appModuleCode = appModuleCode;
    }

    public String getLookUrl() {
        return lookUrl;
    }

    public void setLookUrl(String lookUrl) {
        this.lookUrl = lookUrl;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }


    public String getConditonBean() {
        return conditonBean;
    }

    public void setConditonBean(String conditonBean) {
        this.conditonBean = conditonBean;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", this.getId())
                .append("name", name)
                .append("className", className)
                .append("conditonBean", conditonBean)
                .append("daoBean", daoBean)
                .append("depict", depict)
                .append("appModuleId", appModuleId)
                .append("appModuleCode", appModuleCode)
                .append("flowTypes", flowTypes)
                .append("lookUrl", lookUrl)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
