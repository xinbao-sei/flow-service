package com.ecmp.flow.service;

import com.ecmp.config.util.ApiClient;
import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.core.entity.ITenant;
import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.core.search.SearchFilter;
import com.ecmp.core.service.BaseEntityService;
import com.ecmp.core.service.BaseService;
import com.ecmp.core.service.Validation;
import com.ecmp.flow.api.IAppModuleService;
import com.ecmp.flow.dao.AppModuleDao;
import com.ecmp.flow.entity.AppModule;
import com.ecmp.flow.util.FlowCommonUtil;
import com.ecmp.vo.OperateResultWithData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.GenericType;
import java.util.*;


/**
 * <p>
 * *************************************************************************************************
 * </p><p>
 * 实现功能：应用模块管理-工作流内部
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
@Service
public class AppModuleService extends BaseEntityService<AppModule> implements IAppModuleService {

    @Autowired
    private AppModuleDao appModuleDao;
    @Autowired
    private FlowCommonUtil flowCommonUtil;

    @Override
    protected BaseEntityDao<AppModule> getDao() {
        return appModuleDao;
    }

    private final Logger logger = LoggerFactory.getLogger(BaseService.class);


    /**
     * 数据保存操作
     */
    @SuppressWarnings("unchecked")
    public OperateResultWithData<AppModule> save(AppModule entity) {
        Validation.notNull(entity, "持久化对象不能为空");
        OperateResultWithData<AppModule> operateResultWithData;
        boolean isNew = isNew(entity);
        if (isNew) {
            // 创建前设置租户代码
            if (entity instanceof ITenant) {
                ITenant tenantEntity = (ITenant) entity;
                if (StringUtils.isBlank(tenantEntity.getTenantCode())) {
                    tenantEntity.setTenantCode(ContextUtil.getTenantCode());
                }
            }
            operateResultWithData = preInsert(entity);
        } else {
            operateResultWithData = preUpdate(entity);
        }
        if (Objects.isNull(operateResultWithData) || operateResultWithData.successful()) {
            AppModule saveEntity = getDao().save(entity);
            if (logger.isDebugEnabled()) {
                logger.debug("Saved entity id is {}", entity.getId());
            }
            // 应用模块保存成功！
            operateResultWithData = OperateResultWithData.operationSuccessWithData(saveEntity, "10055");
        }
        clearFlowDefVersion();
        return operateResultWithData;
    }

    private void clearFlowDefVersion() {
        String pattern = "FLowGetLastFlowDefVersion_*";
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    public PageResult<AppModule> findByPage(Search searchConfig) {
        List<com.ecmp.flow.basic.vo.AppModule> appModuleList = null;
        List<String> appModuleCodeList = null;
        appModuleList = flowCommonUtil.getBasicTenantAppModule();
        if (appModuleList != null && !appModuleList.isEmpty()) {
            appModuleCodeList = new ArrayList<String>();
            for (com.ecmp.flow.basic.vo.AppModule appModule : appModuleList) {
                appModuleCodeList.add(appModule.getCode());
            }
        }
        if (appModuleCodeList != null && !appModuleCodeList.isEmpty()) {
            SearchFilter searchFilter = new SearchFilter("code", appModuleCodeList, SearchFilter.Operator.IN);
            searchConfig.addFilter(searchFilter);
        }
        PageResult<AppModule> result = appModuleDao.findByPage(searchConfig);
        return result;
    }

    public List<AppModule> findAllByAuth() {
        List<AppModule> result = null;
        List<com.ecmp.flow.basic.vo.AppModule> appModuleList = null;
        List<String> appModuleCodeList = null;
        try {
            appModuleList =  flowCommonUtil.getBasicTenantAppModule();
            if (appModuleList != null && !appModuleList.isEmpty()) {
                appModuleCodeList = new ArrayList<String>();
                for (com.ecmp.flow.basic.vo.AppModule appModule : appModuleList) {
                    appModuleCodeList.add(appModule.getCode());
                }
            }
            if (appModuleCodeList != null && !appModuleCodeList.isEmpty()) {
                result = appModuleDao.findByCodes(appModuleCodeList);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = appModuleDao.findAll();
        }
        return result;
    }

}
