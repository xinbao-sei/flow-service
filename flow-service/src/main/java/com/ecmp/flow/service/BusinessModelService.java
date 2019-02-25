package com.ecmp.flow.service;

import com.ecmp.config.util.ApiClient;
import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.core.search.SearchFilter;
import com.ecmp.core.service.BaseEntityService;
import com.ecmp.flow.api.IBusinessModelService;
import com.ecmp.flow.basic.vo.AppModule;
import com.ecmp.flow.dao.BusinessModelDao;
import com.ecmp.flow.entity.BusinessModel;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.GenericType;
import java.sql.SQLException;
import java.util.*;

import static com.ecmp.flow.api.client.util.ExpressionUtil.getAppModule;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/3/23 22:39      谭军(tanjun)               新建
 * <p/>
 * *************************************************************************************************
 */
@Service
public class BusinessModelService extends BaseEntityService<BusinessModel> implements IBusinessModelService{

    private final Logger logger = LoggerFactory.getLogger(BusinessModel.class);

    @Autowired
    private BusinessModelDao businessModelDao;

    protected BaseEntityDao<BusinessModel> getDao(){
        return this.businessModelDao;
    }


    @Override
    public Map<String, String> getPropertiesForConditionPojo(String businessModelCode) throws ClassNotFoundException{
        Map<String, String> result=null;
        BusinessModel businessModel = this.findByClassName(businessModelCode);
        if (businessModel != null) {
            String apiBaseAddressConfig = getAppModule(businessModel).getApiBaseAddress();
            String clientApiBaseUrl =  ContextUtil.getGlobalProperty(apiBaseAddressConfig);
            String clientApiUrl = clientApiBaseUrl + businessModel.getConditonProperties();
            Map<String,Object> params = new HashMap();
            params.put("businessModelCode",businessModelCode);
            params.put("all",false);
            result = ApiClient.getEntityViaProxy(clientApiUrl,new GenericType<Map<String,String> >() {},params);
        }
        return result;
    }

    @Override
    public List<BusinessModel> findByAppModuleId(String appModuleId) {
        return businessModelDao.findByAppModuleId(appModuleId);
    }

    @Override
    public BusinessModel findByClassName(String className) {
        return businessModelDao.findByClassName(className);
    }
    /**
     * 主键删除
     *
     * @param id 主键
     * @return 返回操作结果对象
     */
    public OperateResult delete(String id) {
        OperateResult operateResult = preDelete(id);
        if (Objects.isNull(operateResult) || operateResult.successful()) {
            BusinessModel entity = findOne(id);
            if (entity != null) {
                try {
                    getDao().delete(entity);
                }catch (org.springframework.dao.DataIntegrityViolationException e){
                    e.printStackTrace();
                    SQLException sqlException = (SQLException)e.getCause().getCause();
                    if(sqlException!=null && "23000".equals(sqlException.getSQLState())){
                        return OperateResult.operationFailure("10027");
                    }else {
                        throw  e;
                    }
                }
                // 业务实体删除成功！
                return OperateResult.operationSuccess("10057");
            } else {
                // 业务实体{0}不存在！
                return OperateResult.operationWarning("10058", id);
            }
        }
        clearFlowDefVersion();
        return operateResult;
    }

   public OperateResultWithData<BusinessModel> save(BusinessModel businessModel){
       OperateResultWithData<BusinessModel> resultWithData = null;
       try {
           resultWithData = super.save(businessModel);
       }catch (org.springframework.dao.DataIntegrityViolationException e){
           e.printStackTrace();
           Throwable cause =  e.getCause();
           cause=  cause.getCause();
           SQLException sqlException = (SQLException)cause;
           if(sqlException!=null && sqlException.getSQLState().equals("23000")){
               resultWithData = OperateResultWithData.operationFailure("10037");//类全路径重复，请检查！
           }else{
               resultWithData = OperateResultWithData.operationFailure(e.getMessage());
           }
           logger.error(e.getMessage());
       }
       clearFlowDefVersion();
       return resultWithData;
    }
    private void clearFlowDefVersion(){
        String pattern = "FLowGetLastFlowDefVersion_*";
        if(redisTemplate!=null){
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys!=null&&!keys.isEmpty()){
                redisTemplate.delete(keys);
            }
        }
    }

    public PageResult<BusinessModel> findByPage(Search searchConfig){
        List<AppModule> appModuleList = null;
        List<String > appModuleCodeList = null;
        try {
            String url = com.ecmp.flow.common.util.Constants.getBasicTenantAppModuleUrl();
            appModuleList = ApiClient.getEntityViaProxy(url, new GenericType<List<AppModule>>() {
            }, null);
            if(appModuleList!=null && !appModuleList.isEmpty()){
                appModuleCodeList = new ArrayList<String>();
                for(AppModule appModule:appModuleList){
                    appModuleCodeList.add(appModule.getCode());
                }
            }
            if(appModuleCodeList!=null && !appModuleCodeList.isEmpty()){
                SearchFilter searchFilter =   new SearchFilter("appModule.code", appModuleCodeList, SearchFilter.Operator.IN);
                searchConfig.addFilter(searchFilter);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        PageResult<BusinessModel> result = businessModelDao.findByPage(searchConfig);
        return result;
    }

    public  List<BusinessModel> findAllByAuth(){
        List<BusinessModel> result=null;
        List<AppModule> appModuleList = null;
        List<String > appModuleCodeList = null;
        try {
            String url = com.ecmp.flow.common.util.Constants.getBasicTenantAppModuleUrl();
            appModuleList = ApiClient.getEntityViaProxy(url, new GenericType<List<AppModule>>() {
            }, null);
            if(appModuleList!=null && !appModuleList.isEmpty()){
                appModuleCodeList = new ArrayList<String>();
                for(AppModule appModule:appModuleList){
                    appModuleCodeList.add(appModule.getCode());
                }
            }
            if(appModuleCodeList!=null && !appModuleCodeList.isEmpty()){
                result = businessModelDao.findByAppModuleCodes(appModuleCodeList);
            }

        }catch (Exception e){
            e.printStackTrace();
            result = businessModelDao.findAll();
        }
        return result;
    }
}
