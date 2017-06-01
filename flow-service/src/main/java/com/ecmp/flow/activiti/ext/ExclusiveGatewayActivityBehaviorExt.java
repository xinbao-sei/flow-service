package com.ecmp.flow.activiti.ext;

import com.ecmp.context.ContextUtil;
import com.ecmp.flow.util.ConditionUtil;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.Condition;
import org.activiti.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/5/24 11:19      谭军(tanjun)                    新建
 * <p/>
 * *************************************************************************************************
 */
public class ExclusiveGatewayActivityBehaviorExt extends ExclusiveGatewayActivityBehavior {

    protected static Logger log =  LoggerFactory.getLogger(ExclusiveGatewayActivityBehaviorExt.class);


    private RepositoryService repositoryService;

    @Override
    protected void leave(ActivityExecution execution) {
        ApplicationContext applicationContext = ContextUtil.getApplicationContext();
        repositoryService = (RepositoryService) applicationContext.getBean("repositoryService");
        String processInstanceId = execution.getProcessInstanceId();
        String processDefId = execution.getProcessDefinitionId();

        List<PvmTransition> outSequenceList = execution.getActivity().getOutgoingTransitions();
        if (outSequenceList != null && outSequenceList.size() > 0) {
            for (PvmTransition pv : outSequenceList) {
                String pvId = pv.getId();
                String conditionText = (String) pv.getProperty("conditionText");
                String name = (String) pv.getProperty("name");
                Condition conditon = (Condition) pv.getProperty("condition");
                if(conditionText != null && conditionText.startsWith("#{")){
                    String conditonFinal = conditionText.substring(conditionText.indexOf("#{")+2, conditionText.lastIndexOf("}"));
                    Map<String, Object> map = execution.getVariables();
                    if(ConditionUtil.groovyTest(conditonFinal, map)){
                        execution.take(pv);
                        return;
                    }
                }
               // System.out.println(conditon);
            //    System.out.println(conditionText);
            }
        }

        // 执行父类的写法，以使其还是可以支持旧式的在跳出线上写条件的做法
        super.leave(execution);

    }

}