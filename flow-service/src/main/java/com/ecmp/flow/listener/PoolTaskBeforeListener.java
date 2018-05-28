package com.ecmp.flow.listener;

import com.ecmp.config.util.ApiClient;
import com.ecmp.context.ContextUtil;
import com.ecmp.flow.common.util.Constants;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.dao.FlowDefVersionDao;
import com.ecmp.flow.dao.FlowHistoryDao;
import com.ecmp.flow.dao.FlowInstanceDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.entity.*;
import com.ecmp.flow.service.FlowTaskService;
import com.ecmp.flow.util.FlowException;
import com.ecmp.flow.util.ServiceCallUtil;
import com.ecmp.flow.util.TaskStatus;
import com.ecmp.flow.vo.FlowOperateResult;
import com.ecmp.flow.vo.NodeInfo;
import com.ecmp.flow.vo.bpmn.Definition;
import com.ecmp.util.JsonUtils;
import net.sf.json.JSONObject;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.core.GenericType;
import java.util.*;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：接收任务触发前监听事件
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/8/14 9:40      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
//@Component(value="poolTaskBeforeListener")
public class PoolTaskBeforeListener implements org.activiti.engine.delegate.JavaDelegate {

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    FlowHistoryDao  flowHistoryDao;

    @Autowired
    private FlowInstanceDao flowInstanceDao;

    @Autowired
    private RuntimeService runtimeService;

    private final Logger logger = LoggerFactory.getLogger(PoolTaskBeforeListener.class);

    @Override
    public void execute(DelegateExecution delegateTask) throws Exception {
            Date now = new Date();
            String actTaskDefKey = delegateTask.getCurrentActivityId();
            String actProcessDefinitionId = delegateTask.getProcessDefinitionId();
            String businessId =delegateTask.getProcessBusinessKey();
            FlowDefVersion flowDefVersion = flowDefVersionDao.findByActDefId(actProcessDefinitionId);
            String flowDefJson = flowDefVersion.getDefJson();
            JSONObject defObj = JSONObject.fromObject(flowDefJson);
            Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);
            JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(actTaskDefKey);
            JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject(Constants.NORMAL);

            if (normal != null) {
                String serviceTaskId = (String) normal.get(Constants.SERVICE_TASK_ID);
                String poolTaskCode = (String) normal.get(Constants.POOL_TASK_CODE);
                if (!StringUtils.isEmpty(serviceTaskId)) {
                    Map<String,Object> tempV = delegateTask.getVariables();
                    tempV.put(Constants.POOL_TASK_ACT_DEF_ID,actTaskDefKey);
                    tempV.put(Constants.POOL_TASK_CODE,poolTaskCode);
                    String flowTaskName = (String) normal.get(Constants.NAME);
                    FlowTask flowTask = new FlowTask();
                    flowTask.setTaskJsonDef(currentNode.toString());
                    flowTask.setFlowName(definition.getProcess().getName());
                    flowTask.setDepict( ContextUtil.getMessage("10052"));//"用户池任务【等待执行】"
                    flowTask.setTaskName(flowTaskName);
                    flowTask.setFlowDefinitionId(flowDefVersion.getFlowDefination().getId());
                    String actProcessInstanceId = delegateTask.getProcessInstanceId();
                    FlowInstance flowInstance = flowInstanceDao.findByActInstanceId(actProcessInstanceId);
                    flowTask.setFlowInstance(flowInstance);
                    String ownerName = flowDefVersion.getFlowDefination().getFlowType().getBusinessModel().getName();
                    AppModule appModule = flowDefVersion.getFlowDefination().getFlowType().getBusinessModel().getAppModule();
                    if(appModule!=null && StringUtils.isNotEmpty(appModule.getName())){
                        ownerName = appModule.getName();
                    }
                    flowTask.setOwnerAccount(Constants.ANONYMOUS);
                    flowTask.setOwnerName(ownerName);
                    flowTask.setExecutorAccount(Constants.ANONYMOUS);
                    flowTask.setExecutorId("");
                    flowTask.setExecutorName(ownerName);
                    flowTask.setCandidateAccount("");
                    flowTask.setActDueDate(now);

                    flowTask.setActTaskDefKey(actTaskDefKey);
                    flowTask.setPreId(null);
                    flowTask.setTaskStatus(TaskStatus.COMPLETED.toString());
                    flowTask.setTaskStatus(TaskStatus.INIT.toString());

                    //选择下一步可能的执行子流程路径
                    ApplicationContext applicationContext = ContextUtil.getApplicationContext();
                    FlowTaskService flowTaskService = (FlowTaskService)applicationContext.getBean("flowTaskService");
                    List<NodeInfo> nodeInfoList = flowTaskService.findNexNodesWithUserSet(flowTask);
                    List<String> paths = new ArrayList<String>();
                    if(nodeInfoList!=null && !nodeInfoList.isEmpty()){
                         for(NodeInfo nodeInfo :nodeInfoList){
                             if(StringUtils.isNotEmpty(nodeInfo.getCallActivityPath())){
                                 paths.add(nodeInfo.getCallActivityPath());
                             }
                         }
                    }
                    if(!paths.isEmpty()){
                        tempV.put(Constants.CALL_ACTIVITY_SON_PATHS,paths);//提供给调用服务，子流程的绝对路径，用于存入单据id
                    }
                    String param = JsonUtils.toJson(tempV);
                    FlowOperateResult flowOperateResult = null;
                    String callMessage = null;
                    try{
                        flowOperateResult =  ServiceCallUtil.callService(serviceTaskId, businessId, param);
                        if(flowOperateResult!=null && flowOperateResult.isSuccess() && StringUtils.isNotEmpty(flowOperateResult.getUserId())){
                            runtimeService.setVariable(delegateTask.getProcessInstanceId(),Constants.POOL_TASK_CALLBACK_USER_ID+actTaskDefKey, flowOperateResult.getUserId());
                        }
                        callMessage = flowOperateResult!=null? flowOperateResult.getMessage():"";
                    }catch (Exception e){
                          logger.error(e.getMessage());
                        flowOperateResult=null;
                        callMessage = e.getMessage();
                    }

                    if((flowOperateResult==null || !flowOperateResult.isSuccess())){
//                        ExecutionEntity taskEntity = (ExecutionEntity) delegateTask;
//                        TransitionImpl transition = taskEntity.getTransition();
//                        String sourceType =transition.getSource().getProperties().get("type")+"";
                        List<FlowTask> flowTaskList = flowTaskService.findByInstanceId(flowInstance.getId());
                        List<FlowHistory> flowHistoryList = flowHistoryDao.findByInstanceId(flowInstance.getId());

                        if(flowTaskList.isEmpty()&&flowHistoryList.isEmpty()){ //如果是开始节点，手动回滚
//                        if("startEvent".equalsIgnoreCase(sourceType)){ //如果是开始节点，手动回滚
                            new Thread(){
                                public void run(){
                                    BusinessModel businessModel = flowInstance.getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel();
                                    Map<String, Object> params = new HashMap<String,Object>();;
                                    params.put(Constants.BUSINESS_MODEL_CODE,businessModel.getClassName());
                                    params.put(Constants.ID,flowInstance.getBusinessId());
                                    params.put(Constants.STATUS, FlowStatus.INIT);
                                    String apiBaseAddressConfig = appModule.getApiBaseAddress();
                                    String baseUrl =  ContextUtil.getGlobalProperty(apiBaseAddressConfig);
                                    String url = baseUrl+"/"+businessModel.getConditonStatusRest();
                                    Boolean result = false;
                                    int index = 5;
                                    while (!result && index>0){
                                        try {
                                            Thread.sleep(1000*(6-index));
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            result =  ApiClient.postViaProxyReturnResult(url,new GenericType<Boolean>() {}, params);
                                        }catch (Exception e){
                                            logger.error(e.getMessage());
                                        }
                                        index--;
                                    }
//                                String actInstanceId = flowInstance.getActInstanceId();
//                                runtimeService.deleteProcessInstance(actInstanceId, null);
//                                flowInstanceDao.delete(flowInstance);
                                }
                            }.start();
                        }
                        throw new FlowException(callMessage);//抛出异常
                    }
//                   flowTaskDao.save(flowTask);

                }else{
                    String message = ContextUtil.getMessage("10044");
                    throw new FlowException(message);//服务地址为空！
                }
            }
    }
}