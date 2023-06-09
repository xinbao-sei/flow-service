package com.ecmp.flow.dao;

import com.ecmp.flow.entity.AppModule;
import com.ecmp.core.dao.BaseEntityDao;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * *************************************************************************************************
 * </p><p>
 * 实现功能：应用模块
 * </p><p>
 * ------------------------------------------------------------------------------------------------
 * </p><p>
 * 版本          变更时间             变更人                     变更原因
 * </p><p>
 * ------------------------------------------------------------------------------------------------
 * </p><p>
 * 1.0.00      2017/09/06 11:39      谭军(tanjun)                新建
 * </p><p>
 * *************************************************************************************************
 * </p>
 */
@Repository
public interface AppModuleDao extends BaseEntityDao<AppModule>,CustomAppModuleDao{
}
