package com.ecmp.flow.listener;

import com.ecmp.config.util.ApiClient;
import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.jpa.BaseDao;
import com.ecmp.flow.api.common.api.IFlowCommonConditionService;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.dao.FlowDefVersionDao;
import com.ecmp.flow.dao.FlowDefinationDao;
import com.ecmp.flow.dao.FlowInstanceDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.entity.AppModule;
import com.ecmp.flow.entity.BusinessModel;
import com.ecmp.flow.entity.FlowDefVersion;
import com.ecmp.flow.entity.FlowInstance;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.GenericType;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * *************************************************************************************************
 * <p/>
 * 实现功能： 启动完成监听器
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/5/24 13:40      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
@Component(value="endEventCompleteListener")
public class EndEventCompleteListener implements ExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(EndEventCompleteListener.class);
	public EndEventCompleteListener(){
	}
    private static final long serialVersionUID = 1L;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    private FlowDefinationDao flowDefinationDao;

    @Autowired
    private FlowInstanceDao flowInstanceDao;

    @Transactional( propagation= Propagation.REQUIRED)
    public void notify(DelegateExecution delegateTask) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException{
        ExecutionEntity taskEntity = (ExecutionEntity) delegateTask;
        Map<String,Object> variables = delegateTask.getVariables();
        ProcessInstance processInstance  = taskEntity.getProcessInstance();
        FlowInstance  flowInstance = flowInstanceDao.findByActInstanceId(processInstance.getId());
        if(flowInstance==null){
             throw new RuntimeException("流程实例不存在！");
        }else {
            if (processInstance.isEnded()) {//针对启动时只有服务任务这种情况（即启动就结束）
                flowInstance.setEnded(true);
                flowInstance.setEndDate(new Date());
                flowInstanceDao.save(flowInstance);

                //回写状态
                FlowInstance flowInstanceP = flowInstance.getParent();
                    BusinessModel businessModel = flowInstance.getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel();

                AppModule appModule = businessModel.getAppModule();
                Map<String, Object> params = new HashMap<String,Object>();;
                params.put("businessModelCode",businessModel.getClassName());
                params.put("id",flowInstance.getBusinessId());
                params.put("status",FlowStatus.COMPLETED);
                String url = appModule.getApiBaseAddress()+"/"+businessModel.getConditonStatusRest();
                Boolean result = ApiClient.postViaProxyReturnResult(url,new GenericType<Boolean>() {}, params);
                if(!result){
                    throw new RuntimeException("调用重置表单流程结束状态失败");
                }
                if(flowInstanceP!=null){
                    ExecutionEntity parent = taskEntity.getSuperExecution();
                    if(parent != null){
                        ProcessInstance   parentProcessInstance = parent.getProcessInstance();
                        Map<String,Object> variablesParent = runtimeService.getVariables(parent.getId());
                        String superExecutionId = processInstance.getSuperExecutionId();
                        variablesParent.putAll(variables);
                        variablesParent.put(processInstance.getId()+"_superExecutionId",superExecutionId);
                        runtimeService.setVariables(parentProcessInstance.getId(),variablesParent);
                    }
                }
//                flowTaskDao.deleteByFlowInstanceId(flowInstance.getId());//针对终止结束时，删除所有待办
                try {
                    String businessId = delegateTask.getProcessBusinessKey();
                    this.callEndService(businessId, flowInstance.getFlowDefVersion());
                }catch (Exception e){
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private void callEndService( String businessKey,FlowDefVersion flowDefVersion){
        if(flowDefVersion!=null && StringUtils.isNotEmpty(businessKey)){
            String endCallServiceUrl = flowDefVersion.getEndCallServiceUrl();
            if(StringUtils.isNotEmpty(endCallServiceUrl)){
                String baseUrl= flowDefVersion.getFlowDefination().getFlowType().getBusinessModel().getAppModule().getApiBaseAddress();
                String endCallServiceUrlPath = baseUrl+endCallServiceUrl;
                Map<String, Object> params = new HashMap<>();
                params.put("id",businessKey);
                new Thread(new Runnable() {//模拟异步
                    @Override
                    public void run() {
                        ApiClient.getEntityViaProxy(endCallServiceUrlPath,new GenericType<Boolean>() {},params);
                    }
                }).start();
            }
        }
    }
}
