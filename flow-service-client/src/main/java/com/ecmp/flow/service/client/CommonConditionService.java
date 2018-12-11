package com.ecmp.flow.service.client;

import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.jpa.BaseDao;
import com.ecmp.core.entity.BaseEntity;
import com.ecmp.flow.api.client.util.ExpressionUtil;
import com.ecmp.flow.clientapi.ICommonConditionService;
import com.ecmp.flow.common.util.BusinessUtil;
import com.ecmp.flow.constant.BusinessEntityAnnotaion;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.entity.FlowTask;
import com.ecmp.flow.entity.IBusinessFlowEntity;
import com.ecmp.flow.entity.IConditionPojo;
import com.ecmp.vo.ResponseData;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：通用客户端条件表达式服务
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/07/20 13:22      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
public class CommonConditionService implements ICommonConditionService {

//    private IBusinessModelService businessModelService;

    public CommonConditionService() {
    }

    private Map<String, String> getPropertiesForConditionPojo(String conditonPojoClassName,Boolean all) throws ClassNotFoundException {
        return ExpressionUtil.getProperties(conditonPojoClassName,all);
    }


    private Map<String, Object> getPropertiesAndValues(String conditonPojoClassName,Boolean all) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        return ExpressionUtil.getPropertiesAndValues(conditonPojoClassName,all);
    }


    private Map<String, Object> getConditonPojoMap(String conditonPojoClassName, String daoBeanName, String id,Boolean all) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        Class conditonPojoClass = Class.forName(conditonPojoClassName);
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        BaseDao appModuleDao = (BaseDao) applicationContext.getBean(daoBeanName);
        IConditionPojo conditionPojo = (IConditionPojo) conditonPojoClass.newInstance();
        BaseEntity content = (BaseEntity) appModuleDao.findOne(id);
        BeanUtils.copyProperties(conditionPojo, content);
        if (conditionPojo != null) {
            return new ExpressionUtil<IConditionPojo>().getPropertiesAndValues(conditionPojo,all);
        } else {
            return null;
        }
    }


    @Override
    public Map<String, String> properties(String businessModelCode,Boolean all) throws ClassNotFoundException {
        String conditonPojoClassName = null;
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
//        if (businessModel != null) {
//            conditonPojoClassName = getConditionBeanName(businessModelCode);
//        }
        conditonPojoClassName = getConditionBeanName(businessModelCode);
        return this.getPropertiesForConditionPojo(conditonPojoClassName,all);
    }
    public Map<String, String> propertiesAll(String businessModelCode) throws ClassNotFoundException {
        String conditonPojoClassName = null;
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
//        if (businessModel != null) {
//            conditonPojoClassName = getConditionBeanName(businessModelCode);
//        }
        conditonPojoClassName = getConditionBeanName(businessModelCode);
        return this.getPropertiesForConditionPojo(conditonPojoClassName,true);
    }

    @Override
    public Map<String, Object> initPropertiesAndValues(String businessModelCode) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        String conditonPojoClassName = null;
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
//        if (businessModel != null) {
//            conditonPojoClassName = getConditionBeanName(businessModelCode);
//        }
        conditonPojoClassName = getConditionBeanName(businessModelCode);
        return this.getPropertiesAndValues(conditonPojoClassName,true);
    }

    @Override
    public Map<String, Object> propertiesAndValues(String businessModelCode, String id,Boolean all) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {

        String conditonPojoClassName = null;
        String daoBeanName = null;
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
//        if (businessModel != null) {
//            conditonPojoClassName = getConditionBeanName(businessModelCode);
//            daoBeanName = getDaoBeanName(businessModelCode);
//        }
        conditonPojoClassName = getConditionBeanName(businessModelCode);
        daoBeanName = getDaoBeanName(businessModelCode);
        return this.getConditonPojoMap(conditonPojoClassName, daoBeanName, id,all);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public Boolean resetState(String businessModelCode, String id, FlowStatus status) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
        String daoBeanName = null;
//        if (businessModel != null) {
//            daoBeanName = getDaoBeanName(businessModelCode);
//        }
        daoBeanName = getDaoBeanName(businessModelCode);
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        BaseDao appModuleDao = (BaseDao) applicationContext.getBean(daoBeanName);
        IBusinessFlowEntity content = (IBusinessFlowEntity) appModuleDao.findOne(id);
        if(status==FlowStatus.INIT){//针对流程强制终止时，表单已经被删除的情况
            if(content!=null){
                content.setFlowStatus(status);
                appModuleDao.save(content);
            }
        }else{
          if(content==null){
             throw new RuntimeException("business.id do not exist, can not start or complete the process!");
           }
            content.setFlowStatus(status);
            appModuleDao.save(content);
        }
        return true;
    }

    public Map<String,Object> businessPropertiesAndValues(String businessModelCode,String id) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,NoSuchMethodException{
//        businessModelService = ApiClient.createProxy(IBusinessModelService.class);
//        BusinessModel businessModel = businessModelService.findByClassName(businessModelCode);
        String daoBeanName = null;
//        if (businessModel != null) {
//            daoBeanName = getDaoBeanName(businessModelCode);
//        }
        daoBeanName = getDaoBeanName(businessModelCode);
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        BaseDao appModuleDao = (BaseDao) applicationContext.getBean(daoBeanName);
        IBusinessFlowEntity content = (IBusinessFlowEntity) appModuleDao.findOne(id);
        return   BusinessUtil.getPropertiesAndValues(content,null);
    }
    private String getDaoBeanName(String className)throws ClassNotFoundException {
        BusinessEntityAnnotaion businessEntityAnnotaion = this.getBusinessEntityAnnotaion(className);
         return   businessEntityAnnotaion.daoBean();
    }
    private String getConditionBeanName(String className)throws ClassNotFoundException {
        BusinessEntityAnnotaion businessEntityAnnotaion = this.getBusinessEntityAnnotaion(className);
        return   businessEntityAnnotaion.conditionBean();
    }
    private BusinessEntityAnnotaion getBusinessEntityAnnotaion(String className)throws ClassNotFoundException {
        if (StringUtils.isNotEmpty(className)) {
            Class sourceClass = Class.forName(className);
            BusinessEntityAnnotaion businessEntityAnnotaion = (BusinessEntityAnnotaion) sourceClass.getAnnotation(BusinessEntityAnnotaion.class);
            return  businessEntityAnnotaion;
        }else {
            throw new RuntimeException("className is null!");
        }
    }

    public ResponseData pushTasksToDo(List<FlowTask> list){
        ResponseData responseData =new ResponseData();
        responseData.setSuccess(true);
        responseData.setMessage("推送成功！");
        return responseData;
    }


}
