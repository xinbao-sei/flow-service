package com.ecmp.flow.listener;

import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.flow.dao.*;
import com.ecmp.flow.entity.FlowDefVersion;
import com.ecmp.flow.entity.FlowTask;
import com.ecmp.flow.service.FlowDefinationService;
import com.ecmp.flow.util.ServiceCallUtil;
import com.ecmp.flow.vo.FlowOpreateResult;
import com.ecmp.flow.vo.NodeInfo;
import com.ecmp.flow.vo.bpmn.Definition;
import com.ecmp.flow.vo.bpmn.UserTask;
import com.ecmp.util.JsonUtils;
import net.sf.json.JSONObject;
import org.activiti.engine.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * *************************************************************************************************
 * <p/>
 * 实现功能： 通用用户任务完成监听器
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/5/24 13:40      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
@Component(value="commonUserTaskCompleteListener")
public class CommonUserTaskCompleteListener implements ExecutionListener {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    private FlowDefinationDao flowDefinationDao;


    private final Logger logger = LoggerFactory.getLogger(CommonUserTaskCompleteListener.class);

	public CommonUserTaskCompleteListener(){
	}
    private static final long serialVersionUID = 1L;

    public void notify(DelegateExecution delegateTask) {
        try {
            String deleteReason = ((ExecutionEntity) delegateTask).getDeleteReason();
            if(StringUtils.isNotEmpty(deleteReason)){
                return;
            }
            String actTaskDefKey = delegateTask.getCurrentActivityId();
            String actProcessDefinitionId = delegateTask.getProcessDefinitionId();
            String businessId =delegateTask.getProcessBusinessKey();
            FlowDefVersion flowDefVersion = flowDefVersionDao.findByActDefId(actProcessDefinitionId);
            String flowDefJson = flowDefVersion.getDefJson();
            JSONObject defObj = JSONObject.fromObject(flowDefJson);
            Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);
//        net.sf.json.JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(currentTaskId);
            net.sf.json.JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(actTaskDefKey);
            //        net.sf.json.JSONObject executor = currentNode.getJSONObject("nodeConfig").getJSONObject("executor");
            net.sf.json.JSONObject event = currentNode.getJSONObject("nodeConfig").getJSONObject("event");
//        UserTask userTaskTemp = (UserTask) JSONObject.toBean(currentNode, UserTask.class);
            if (event != null) {
                String afterExcuteServiceId = (String) event.get("afterExcuteServiceId");
                if (!StringUtils.isEmpty(afterExcuteServiceId)) {
                    Map<String,Object> tempV = delegateTask.getVariables();
                    String param = JsonUtils.toJson(tempV);
                    Object result = ServiceCallUtil.callService(afterExcuteServiceId, businessId, param);
                    try {
                        FlowOpreateResult flowOpreateResult = (FlowOpreateResult) result;
                        if(true!=flowOpreateResult.isSuccess()){
                            throw new RuntimeException("执行逻辑失败，"+flowOpreateResult.getMessage());
                        }
                    }catch (Exception e){
                        logger.error(e.getMessage());
                    }
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            throw e;
        }
    }
}
