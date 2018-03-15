package com.ecmp.flow.listener;

import com.ecmp.context.ContextUtil;
import com.ecmp.flow.common.util.Constants;
import com.ecmp.flow.dao.FlowDefVersionDao;
import com.ecmp.flow.dao.FlowHistoryDao;
import com.ecmp.flow.dao.FlowInstanceDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.entity.*;
import com.ecmp.flow.service.FlowDefinationService;
import com.ecmp.flow.util.FlowListenerTool;
import com.ecmp.flow.util.TaskStatus;
import com.ecmp.flow.vo.NodeInfo;
import com.ecmp.flow.vo.bpmn.Definition;
import net.sf.json.JSONObject;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/8/14 9:40      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
//@Component(value="poolTaskAfterListener")
public class PoolTaskAfterListener implements org.activiti.engine.delegate.JavaDelegate {

    private final Logger logger = LoggerFactory.getLogger(PoolTaskAfterListener.class);

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    FlowHistoryDao  flowHistoryDao;

    @Autowired
    private FlowInstanceDao flowInstanceDao;

    @Autowired
    FlowDefinationService flowDefinationService;

    @Autowired
    private FlowListenerTool flowListenerTool;

    @Override
    public void execute(DelegateExecution delegateTask) throws Exception {
        String eventName = delegateTask.getEventName();
        String deleteReason = ((ExecutionEntity) delegateTask).getDeleteReason();
        if(Constants.END.equalsIgnoreCase(eventName) && StringUtils.isNotEmpty(deleteReason)){
            return;
        }
        String actProcessInstanceId = delegateTask.getProcessInstanceId();
        ExecutionEntity taskEntity = (ExecutionEntity) delegateTask;
        String actTaskDefKey = delegateTask.getCurrentActivityId();
        String actProcessDefinitionId = delegateTask.getProcessDefinitionId();
        FlowDefVersion flowDefVersion = flowDefVersionDao.findByActDefId(actProcessDefinitionId);
        String flowDefJson = flowDefVersion.getDefJson();
        JSONObject defObj = JSONObject.fromObject(flowDefJson);
        Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);
        JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(actTaskDefKey);
        JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject(Constants.NORMAL);
        if (normal != null) {
            String flowTaskName = (String) normal.get(Constants.NAME);
            FlowTask flowTask = null;
            FlowHistory flowHistory = new FlowHistory();
            List<FlowTask> flowTaskList = flowTaskDao.findByActTaskDefKeyAndActInstanceId(actTaskDefKey,actProcessInstanceId);
            if(flowTaskList!=null && !flowTaskList.isEmpty()){
                flowTask = flowTaskList.get(0);
                flowTaskDao.delete(flowTask);
            }
            if(flowTask!=null){
                BeanUtils.copyProperties(flowTask,flowHistory);
                flowHistory.setId(null);
                flowHistory.setActStartTime(flowTask.getActDueDate());
            }else{
                flowTask = new FlowTask();
                flowHistory.setTaskJsonDef(currentNode.toString());
                flowHistory.setFlowName(definition.getProcess().getName());
                flowHistory.setFlowTaskName(flowTaskName);
                FlowInstance flowInstance = flowInstanceDao.findByActInstanceId(actProcessInstanceId);
                flowHistory.setFlowInstance(flowInstance);
                String ownerName = flowDefVersion.getFlowDefination().getFlowType().getBusinessModel().getName();
                AppModule appModule= flowDefVersion.getFlowDefination().getFlowType().getBusinessModel().getAppModule();
                if(appModule!=null && StringUtils.isNotEmpty(appModule.getName())){
                    ownerName = appModule.getName();
                }
                flowHistory.setOwnerAccount(Constants.ADMIN);
                flowHistory.setOwnerName(ownerName);
                flowHistory.setExecutorAccount(Constants.ADMIN);
                flowHistory.setExecutorId("");
                flowHistory.setExecutorName(ownerName);
                flowHistory.setCandidateAccount("");
                flowHistory.setActStartTime(new Date());
                flowHistory.setActHistoryId(null);
                flowHistory.setActTaskDefKey(actTaskDefKey);
                flowHistory.setPreId(null);

                BeanUtils.copyProperties(flowHistory,flowTask);
                flowTask.setTaskStatus(TaskStatus.INIT.toString());
            }
            flowHistory.setFlowName(definition.getProcess().getName());
            flowHistory.setFlowTaskName(flowTaskName);
            flowHistory.setDepict(ContextUtil.getMessage("10046"));//接收任务【执行完成】
            flowHistory.setTaskStatus(TaskStatus.COMPLETED.toString());
            flowHistory.setActEndTime(new Date());
            flowHistory.setFlowDefId(flowDefVersion.getFlowDefination().getId());

            if(flowHistory.getActDurationInMillis() == null){
                Long actDurationInMillis = flowHistory.getActEndTime().getTime()-flowHistory.getActStartTime().getTime();
                flowHistory.setActDurationInMillis(actDurationInMillis);
            }
            flowHistoryDao.save(flowHistory);

                //选择下一步执行人，默认选择第一个，会签、串、并行选择全部
            List<NodeInfo> results = flowListenerTool.nextNodeInfoList(flowTask,delegateTask);
                //初始化节点执行人
            List<NodeInfo> nextNodes = flowListenerTool.initNodeUsers(results,delegateTask,actTaskDefKey);
                //初始化下一步任务信息
            flowListenerTool.initNextAllTask(nextNodes, taskEntity, flowHistory );
        }
    }
}
