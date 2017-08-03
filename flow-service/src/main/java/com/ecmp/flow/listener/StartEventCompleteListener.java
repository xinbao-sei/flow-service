package com.ecmp.flow.listener;

import com.ecmp.flow.dao.FlowDefVersionDao;
import com.ecmp.flow.dao.FlowDefinationDao;
import com.ecmp.flow.dao.FlowInstanceDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.entity.FlowDefVersion;
import com.ecmp.flow.entity.FlowInstance;
import com.ecmp.flow.util.ServiceCallUtil;
import com.ecmp.flow.vo.bpmn.Definition;
import net.sf.json.JSONObject;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
@Component(value="startEventCompleteListener")
public class StartEventCompleteListener implements ExecutionListener {

    private final Logger logger = LoggerFactory.getLogger(StartEventCompleteListener.class);
	public StartEventCompleteListener(){
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
    public void notify(DelegateExecution delegateTask) {
        ExecutionEntity taskEntity = (ExecutionEntity) delegateTask;
        Map<String,Object> variables = delegateTask.getVariables();
        ProcessInstance processInstance  = taskEntity.getProcessInstance();
        FlowInstance  flowInstance = flowInstanceDao.findByActInstanceId(processInstance.getId());
        if(flowInstance==null){
            flowInstance = new FlowInstance();
            flowInstance.setBusinessId(processInstance.getBusinessKey());
            String workCaption = variables.get("workCaption")+"";//工作说明
            flowInstance.setBusinessModelRemark(workCaption);
            String businessCode = variables.get("businessCode")+"";//工作说明
            flowInstance.setBusinessCode(businessCode);
            String businessName = variables.get("name")+"";//业务单据名称
            flowInstance.setBusinessName(businessName);
            String flowDefVersionId= (String) variables.get("flowDefVersionId");
            FlowDefVersion flowDefVersion = flowDefVersionDao.findOne(flowDefVersionId);
            if(flowDefVersion==null){
                throw new RuntimeException("流程版本找不到！");
            }
            flowInstance.setFlowDefVersion(flowDefVersion);
            flowInstance.setStartDate(new Date());
            flowInstance.setFlowName(flowDefVersion.getName());
            flowInstance.setActInstanceId(processInstance.getId());
            flowInstanceDao.save(flowInstance);
        }
    }
}