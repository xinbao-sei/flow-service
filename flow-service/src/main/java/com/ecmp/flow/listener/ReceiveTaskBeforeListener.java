package com.ecmp.flow.listener;

import com.ecmp.context.ContextUtil;
import com.ecmp.flow.common.util.Constants;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.dao.FlowDefVersionDao;
import com.ecmp.flow.dao.FlowHistoryDao;
import com.ecmp.flow.dao.FlowInstanceDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.entity.*;
import com.ecmp.flow.service.FlowTaskService;
import com.ecmp.flow.util.*;
import com.ecmp.flow.vo.FlowOperateResult;
import com.ecmp.flow.vo.NodeInfo;
import com.ecmp.flow.vo.bpmn.Definition;
import com.ecmp.log.util.LogUtil;
import com.ecmp.util.JsonUtils;
import net.sf.json.JSONObject;
import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
public class ReceiveTaskBeforeListener implements org.activiti.engine.delegate.JavaDelegate {

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    private FlowHistoryDao flowHistoryDao;

    @Autowired
    private FlowInstanceDao flowInstanceDao;

    @Autowired
    private FlowTaskTool flowTaskTool;

    @Override
    public void execute(DelegateExecution delegateTask) throws Exception {

        try {
            Date now = new Date();
            String actTaskDefKey = delegateTask.getCurrentActivityId();
            String actProcessDefinitionId = delegateTask.getProcessDefinitionId();
            String businessId = delegateTask.getProcessBusinessKey();
            FlowDefVersion flowDefVersion = flowDefVersionDao.findByActDefId(actProcessDefinitionId);
            String flowDefJson = flowDefVersion.getDefJson();
            JSONObject defObj = JSONObject.fromObject(flowDefJson);
            Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);
            JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(actTaskDefKey);
            JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject(Constants.NORMAL);
            if (normal != null) {
                String serviceTaskId = (String) normal.get(Constants.SERVICE_TASK_ID);
                String serviceTaskName = (String) normal.get(Constants.SERVICE_TASK);
                if (!StringUtils.isEmpty(serviceTaskId)) {
                    Map<String, Object> tempV = delegateTask.getVariables();
                    tempV.put(Constants.RECEIVE_TASK_ACT_DEF_ID, actTaskDefKey);
                    String flowTaskName = (String) normal.get(Constants.NAME);
                    FlowTask flowTask = new FlowTask();
                    flowTask.setTaskJsonDef(currentNode.toString());
                    flowTask.setFlowName(definition.getProcess().getName());
                    flowTask.setDepict(ContextUtil.getMessage("10045"));//"接收任务【等待执行】"
                    flowTask.setTaskName(flowTaskName);
                    flowTask.setFlowDefinitionId(flowDefVersion.getFlowDefination().getId());
                    String actProcessInstanceId = delegateTask.getProcessInstanceId();
                    FlowInstance flowInstance = flowInstanceDao.findByActInstanceId(actProcessInstanceId);
                    flowTask.setFlowInstance(flowInstance);


                    flowTask.setOwnerName("系统触发");
                    flowTask.setExecutorName("系统触发");
                    flowTask.setOwnerAccount(Constants.ADMIN);
                    flowTask.setExecutorAccount(Constants.ADMIN);
                    flowTask.setExecutorId("");
                    flowTask.setOwnerId("");
                    flowTask.setCandidateAccount("");

                    flowTask.setActDueDate(now);
                    flowTask.setActTaskDefKey(actTaskDefKey);
                    flowTask.setPreId(null);
                    flowTask.setTaskStatus(TaskStatus.INIT.toString());

                    //接收任务添加配置是否可以发起人终止
                    Boolean canSuspension = normal.get("allowTerminate") != null ? (Boolean) normal.get("allowTerminate") : null;
                    flowTask.setCanSuspension(canSuspension);


                    //选择下一步可能的执行子流程路径
                    List<NodeInfo> nodeInfoList = flowTaskTool.findNextNodesWithCondition(flowTask, null, null);
                    List<String> paths = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(nodeInfoList)) {
                        for (NodeInfo nodeInfo : nodeInfoList) {
                            if (StringUtils.isNotEmpty(nodeInfo.getCallActivityPath())) {
                                paths.add(nodeInfo.getCallActivityPath());
                            }
                        }
                    }
                    if (!CollectionUtils.isEmpty(paths)) {
                        tempV.put(Constants.CALL_ACTIVITY_SON_PATHS, paths);//提供给调用服务，子流程的绝对路径，用于存入单据id
                    }
                    String param = JsonUtils.toJson(tempV);
                    FlowOperateResult flowOperateResult = null;
                    String callMessage ;
                    try {
                        flowOperateResult = ServiceCallUtil.callService(serviceTaskId, serviceTaskName, flowTaskName, businessId, param);
                        callMessage = flowOperateResult.getMessage();
                    } catch (Exception e) {
                        callMessage = e.getMessage();
                    }
                    if (flowOperateResult == null || !flowOperateResult.isSuccess()) {
                        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
                        FlowTaskService flowTaskService = (FlowTaskService) applicationContext.getBean("flowTaskService");
                        List<FlowTask> flowTaskList = flowTaskService.findByInstanceIdNoVirtual(flowInstance.getId());
                        List<FlowHistory> flowHistoryList = flowHistoryDao.findByInstanceIdNoVirtual(flowInstance.getId());

                        if ( CollectionUtils.isEmpty(flowTaskList) && CollectionUtils.isEmpty(flowHistoryList)) { //如果是开始节点，手动回滚
                            BusinessModel businessModel = flowInstance.getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel();
                            //轮询修改状态为：初始化
                            ExpressionUtil.pollingResetState(businessModel, flowInstance.getBusinessId(), FlowStatus.INIT);
                        }
                        throw new FlowException(callMessage);//抛出异常
                    }

                    flowTaskDao.save(flowTask);
                } else {
                    throw new FlowException(ContextUtil.getMessage("10362"));
                }
            }

        } catch (Exception e) {
            if (e.getClass() != FlowException.class) {
                LogUtil.error(e.getMessage(), e);
            }
            throw e;
        }


    }
}
