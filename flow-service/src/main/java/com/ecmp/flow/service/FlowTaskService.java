package com.ecmp.flow.service;

import com.ecmp.config.util.ApiClient;
import com.ecmp.config.util.ApiJsonUtils;
import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.core.search.*;
import com.ecmp.core.service.BaseEntityService;
import com.ecmp.enums.UserAuthorityPolicy;
import com.ecmp.flow.api.IFlowTaskService;
import com.ecmp.flow.basic.vo.Employee;
import com.ecmp.flow.basic.vo.Executor;
import com.ecmp.flow.basic.vo.UserEmailAlert;
import com.ecmp.flow.common.util.Constants;
import com.ecmp.flow.constant.EarlyWarningStatus;
import com.ecmp.flow.constant.FlowExecuteStatus;
import com.ecmp.flow.constant.FlowStatus;
import com.ecmp.flow.dao.*;
import com.ecmp.flow.dao.util.PageUrlUtil;
import com.ecmp.flow.dto.*;
import com.ecmp.flow.entity.*;
import com.ecmp.flow.util.*;
import com.ecmp.flow.vo.*;
import com.ecmp.flow.vo.bpmn.Definition;
import com.ecmp.flow.vo.bpmn.UserTask;
import com.ecmp.flow.vo.phone.FlowTaskBatchPhoneVO;
import com.ecmp.flow.vo.phone.FlowTaskPhoneVo;
import com.ecmp.flow.vo.push.PushFlowDefVersionVo;
import com.ecmp.flow.vo.push.PushFlowTaskVo;
import com.ecmp.flow.vo.push.PushInstanceVo;
import com.ecmp.log.util.LogUtil;
import com.ecmp.notify.api.INotifyService;
import com.ecmp.notity.entity.EcmpMessage;
import com.ecmp.notity.entity.NotifyType;
import com.ecmp.util.JsonUtils;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import com.ecmp.vo.ResponseData;
import com.ecmp.vo.SessionUser;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.el.UelExpressionCondition;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.impl.pvm.process.ProcessElementImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.core.GenericType;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：流程任务管理
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/3/23 22:39      谭军(tanjun)               新建
 * <p/>
 * *************************************************************************************************
 */
@Service
public class FlowTaskService extends BaseEntityService<FlowTask> implements IFlowTaskService {

    @Autowired
    private FlowTaskTool flowTaskTool;

    @Autowired
    private FlowTaskDao flowTaskDao;

    @Autowired
    private AppModuleDao appModuleDao;

    protected BaseEntityDao<FlowTask> getDao() {
        return this.flowTaskDao;
    }

    @Autowired
    private FlowInstanceDao flowInstanceDao;

    @Autowired
    private FlowHistoryDao flowHistoryDao;

    @Autowired
    private FlowCommonUtil flowCommonUtil;

    @Autowired
    private FlowExecutorConfigDao flowExecutorConfigDao;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FlowDefinationDao flowDefinationDao;

    @Autowired
    private BusinessModelDao businessModelDao;

    @Autowired
    private FlowDefVersionDao flowDefVersionDao;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FlowSolidifyExecutorDao flowSolidifyExecutorDao;

    @Autowired
    private FlowSolidifyExecutorService flowSolidifyExecutorService;

    @Autowired
    private AppModuleService appModuleService;

    @Autowired
    private FlowTaskPushControlService flowTaskPushControlService;

    @Autowired
    private FlowHistoryService flowHistoryService;

    @Autowired
    private TaskMakeOverPowerService taskMakeOverPowerService;

    @Autowired
    private DefaultFlowBaseService defaultFlowBaseService;

    @Autowired
    private DisagreeReasonService disagreeReasonService;

    @Autowired
    private FlowTaskPushDao flowTaskPushDao;

    @Autowired
    private FlowInstanceService flowInstanceService;


    /**
     * 保存推送信息
     *
     * @param type     推送类型
     * @param status   推送状态
     * @param url      推送url
     * @param taskList 推送的任务集合
     */
    public void savePushAndControlInfo(String type, String status, String url, Boolean success, List<FlowTask> taskList) {
        if (!CollectionUtils.isEmpty(taskList)) {
            try {
                //判断进来的任务是不是属于同一个实例
                String oneFlowInstanceId = taskList.get(0).getFlowInstance().getId();
                String oneTaskId = taskList.get(0).getId();
                FlowTask bean = taskList.stream().filter(a -> (!oneFlowInstanceId.equals(a.getFlowInstance().getId()) && !oneTaskId.equals(a.getId()))).findFirst().orElse(null);
                //taskList属于不同流程实例
                if (bean != null) {
                    for (FlowTask flowtask : taskList) {
                        List<FlowTask> needList = new ArrayList<>();
                        needList.add(flowtask);
                        List<FlowTaskPushControl> list = flowTaskPushControlService.getByInstanceAndNodeAndTypeAndStatus(flowtask.getFlowInstance().getId(), flowtask.getActTaskDefKey(), type, status);
                        if (CollectionUtils.isEmpty(list)) {
                            //新建推送任务父表和任务列表
                            flowTaskPushControlService.saveNewControlInfo(type, status, url, success, needList);
                        } else {
                            //更新推送任务父表和任务列表
                            flowTaskPushControlService.updateOldControlInfo(type, status, url, success, needList, list);
                        }
                    }
                } else { //taskList属于同实例
                    List<FlowTaskPushControl> list = flowTaskPushControlService.getByInstanceAndNodeAndTypeAndStatus(taskList.get(0).getFlowInstance().getId(), taskList.get(0).getActTaskDefKey(), type, status);
                    if (CollectionUtils.isEmpty(list)) {
                        //新建推送任务父表和任务列表
                        flowTaskPushControlService.saveNewControlInfo(type, status, url, success, taskList);
                    } else {
                        //更新推送任务父表和任务列表
                        flowTaskPushControlService.updateOldControlInfo(type, status, url, success, taskList, list);
                    }
                }
            } catch (Exception e) {
                LogUtil.error("保存推送信息失败！", e);
            }
        }
    }


    /**
     * 查看是否需要推送任务信息到basic模块
     *
     * @return true或者false，true为需要推送
     */
    public boolean getBooleanPushTaskToBasic() {
        Boolean pushBasic = false;
        String pushBasicStr = Constants.getFlowPropertiesByKey("FLOW_PUSH_TASK_BASIC");
        if (StringUtils.isNotEmpty(pushBasicStr)) {
            if ("true".equalsIgnoreCase(pushBasicStr.trim())) {
                pushBasic = true;
            }
        }
        return pushBasic;
    }


    /**
     * 为推送的待办添加是否为自动执行的标记
     *
     * @param newList
     */
    public void addTaskAutoStatus(List<FlowTask> newList) {
        Map<String, Object> map = new HashMap<>();
        for (FlowTask flowTask : newList) {
            flowTask.setNewTaskAuto(false);//默认不会自动执行
            List<FlowTask> checkList = new ArrayList<>();
            checkList.add(flowTask);
            String businessId = flowTask.getFlowInstance().getBusinessId();
            List<FlowSolidifyExecutor> solidifylist;
            if (map.get(businessId) == null) {
                solidifylist = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
                map.put(businessId, solidifylist);
            } else {
                solidifylist = (List<FlowSolidifyExecutor>) map.get(businessId);
            }

            if (!CollectionUtils.isEmpty(solidifylist)) {
                //检查固化需要跳过的待办
                ResponseData solidifyResult = flowSolidifyExecutorService.checkAutomaticToDoSolidifyTask(solidifylist, checkList, false);
                if (solidifyResult.successful()) {
                    List<FlowTask> needLsit = (List<FlowTask>) solidifyResult.getData();
                    if (!CollectionUtils.isEmpty(needLsit)) {
                        LogUtil.bizLog("固化记录需要自动执行信息：taskId={},taskName={},executorName={}", flowTask.getId(), flowTask.getTaskName(), flowTask.getExecutorName());
                        flowTask.setNewTaskAuto(true);
                    }
                }
            } else {
                //检查非固化需要跳过的待办
//                ResponseData needTaskResult = this.checkAutomaticToDoTask(checkList);
//                if (needTaskResult.successful()) {
//                    List<FlowTask> needLsit = (List<FlowTask>) needTaskResult.getData();
//                    if (!CollectionUtils.isEmpty(needLsit)) {
//                        LogUtil.bizLog("非固化记录需要自动执行信息：taskId={},taskName={},executorName={}", flowTask.getId(), flowTask.getTaskName(), flowTask.getExecutorName());
//                        flowTask.setNewTaskAuto(true);
//                    }
//                }
            }
        }
    }

    /**
     * @param newList 待办
     * @param oldList 待办转已办
     * @param delList 删除待办
     * @param endTask 归档
     */
    public Boolean pushToBasic(List<FlowTask> newList, List<FlowTask> oldList, List<FlowTask> delList, FlowTask endTask) {
        Boolean boo = false;
        if (!CollectionUtils.isEmpty(delList)) { //删除待办
            boo = pushDelTaskToBasic(delList);
        }
        if (!CollectionUtils.isEmpty(oldList)) { //待办转已办
            boo = pushOldTaskToBasic(oldList);
        }
        if (!CollectionUtils.isEmpty(newList)) {  //新增待办
            boo = pushNewTaskToBasic(newList);
        }
        if (endTask != null) { //归档（终止）
            boo = pushEndTaskToBasic(endTask);
        }
        return boo;
    }


    //去掉defJson和defXml大字段
    public List<PushFlowTaskVo> taskToPushTask(List<FlowTask> taskList) {
        List<PushFlowTaskVo> list = new ArrayList<>();
        for (FlowTask flowTask : taskList) {
            PushFlowTaskVo pushFlowTaskVo = new PushFlowTaskVo();
            BeanUtils.copyProperties(flowTask, pushFlowTaskVo);
            PushInstanceVo pushInstanceVo = new PushInstanceVo();
            BeanUtils.copyProperties(flowTask.getFlowInstance(), pushInstanceVo);
            PushFlowDefVersionVo pushFlowDefVersionVo = new PushFlowDefVersionVo();
            BeanUtils.copyProperties(flowTask.getFlowInstance().getFlowDefVersion(), pushFlowDefVersionVo);
            pushInstanceVo.setFlowDefVersion(pushFlowDefVersionVo);
            pushFlowTaskVo.setFlowInstance(pushInstanceVo);
            list.add(pushFlowTaskVo);
        }
        return list;
    }

    /**
     * 推送待办到basic模块
     *
     * @param taskList 需要推送的待办
     */
    @Override
    public Boolean pushNewTaskToBasic(List<FlowTask> taskList) {
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> idList = new ArrayList<>();
            taskList.forEach(a -> {
                a.setApproveStatus(null);
                idList.add("【id=" + a.getId() + "】");
            });
            this.initFlowTasks(taskList); //添加待办处理地址等
            List<PushFlowTaskVo> pushFlowTaskVoList = taskToPushTask(taskList);
            String url = Constants.getBasicPushNewTaskUrl(); //推送待办接口
            ResponseData responseData = ResponseData.operationFailure("10196");
            try {
                responseData = ApiClient.postViaProxyReturnResult(url, new GenericType<ResponseData>() {
                }, pushFlowTaskVoList);
            } catch (Exception e) {
                LogUtil.error("开始调用【推送待办到basic】接口，接口url={},参数值ID集合:{},推送待办异常：{}", url, JsonUtils.toJson(idList), e.getMessage(), e);
            } finally {
                this.savePushAndControlInfo(Constants.TYPE_BASIC, Constants.STATUS_BASIC_NEW, url, responseData.getSuccess(), taskList);
            }
            return responseData.getSuccess();
        }
        return false;
    }


    /**
     * 推送新的已办到basic模块
     *
     * @param taskList 需要推送的已办（刚执行完成的）
     */
    @Override
    public Boolean pushOldTaskToBasic(List<FlowTask> taskList) {
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> idList = new ArrayList<>();
            taskList.forEach(a -> {
                a.setNewTaskAuto(null);
                idList.add("【id=" + a.getId() + "】");
            });
            List<PushFlowTaskVo> pushFlowTaskVoList = taskToPushTask(taskList);
            String url = Constants.getBasicPushOldTaskUrl(); //推送已办接口
            ResponseData responseData = ResponseData.operationFailure("10196");
            try {
                responseData = ApiClient.postViaProxyReturnResult(url, new GenericType<ResponseData>() {
                }, pushFlowTaskVoList);
            } catch (Exception e) {
                LogUtil.error("开始调用【推送已办到basic】接口，接口url={},参数值ID集合:{},推送已办异常：{}", url, JsonUtils.toJson(idList), e.getMessage(), e);
            } finally {
                this.savePushAndControlInfo(Constants.TYPE_BASIC, Constants.STATUS_BASIC_OLD, url, responseData.getSuccess(), taskList);
            }
            return responseData.getSuccess();
        }
        return false;
    }

    /**
     * 推送需要删除的待办到basic模块
     *
     * @param taskList 需要删除的待办
     */
    @Override
    public Boolean pushDelTaskToBasic(List<FlowTask> taskList) {
        if (!CollectionUtils.isEmpty(taskList)) {
            List<String> idList = new ArrayList<>();
            taskList.forEach(a -> {
                a.getFlowInstance().setFlowTasks(null);
                a.setNewTaskAuto(null);
                a.setApproveStatus(null);
                idList.add("【id=" + a.getId() + "】");
            });
            List<PushFlowTaskVo> pushFlowTaskVoList = taskToPushTask(taskList);
            String url = Constants.getBasicPushDelTaskUrl(); //推送需要删除待办接口
            ResponseData responseData = ResponseData.operationFailure("10196");
            try {
                responseData = ApiClient.postViaProxyReturnResult(url, new GenericType<ResponseData>() {
                }, pushFlowTaskVoList);
            } catch (Exception e) {
                LogUtil.error("开始调用【推送删除待办到basic】接口，接口url={},参数值ID集合:{},推送删除待办异常：{}", url, JsonUtils.toJson(idList), e.getMessage(), e);
            } finally {
                this.savePushAndControlInfo(Constants.TYPE_BASIC, Constants.STATUS_BASIC_DEL, url, responseData.getSuccess(), taskList);
            }
            return responseData.getSuccess();
        }
        return false;
    }

    /**
     * 推送需要归档（终止）的任务到basic模块
     *
     * @param task 需要终止的任务
     */
    @Override
    public Boolean pushEndTaskToBasic(FlowTask task) {
        if (task != null) {
            task.setNewTaskAuto(null);
            task.setApproveStatus(null);
            List<FlowTask> taskList = new ArrayList<>();
            taskList.add(task);
            List<PushFlowTaskVo> pushFlowTaskVoList = taskToPushTask(taskList);
            PushFlowTaskVo pushFlowTaskVo = pushFlowTaskVoList.get(0);
            String url = Constants.getBasicPushEndTaskUrl(); //推送需要归档（终止）的任务到basic模块接口
            ResponseData responseData = ResponseData.operationFailure("10196");
            try {
                responseData = ApiClient.postViaProxyReturnResult(url, new GenericType<ResponseData>() {
                }, pushFlowTaskVo);
            } catch (Exception e) {
                LogUtil.error("开始调用【推送归档任务到basic】接口，接口url={},参数值ID集合:{},推送归档任务异常：{}", url, task.getId(), e.getMessage(), e);

            } finally {
                this.savePushAndControlInfo(Constants.TYPE_BASIC, Constants.STATUS_BASIC_END, url, responseData.getSuccess(), taskList);
            }
            return responseData.getSuccess();
        }
        return false;
    }


    /**
     * 任务签收
     *
     * @param id     任务id
     * @param userId 用户账号
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperateResult claim(String id, String userId) {
        FlowTask flowTask = flowTaskDao.findOne(id);
        String actTaskId = flowTask.getActTaskId();
        this.claimActiviti(actTaskId, userId);
        flowTask.setActClaimTime(new Date());
        flowTask.setTaskStatus(TaskStatus.CLAIM.toString());
        flowTaskDao.save(flowTask);
        //是否推送信息到baisc
        Boolean pushBasic = this.getBooleanPushTaskToBasic();
        //需要异步推送删除待办信息到basic
        if (pushBasic) {
            List<FlowTask> alllist = flowTaskDao.findListByProperty("actTaskId", actTaskId);
            List<FlowTask> needDelList = alllist.stream().filter((a) -> !a.getId().equalsIgnoreCase(flowTask.getId())).collect(Collectors.toList());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pushToBasic(null, null, needDelList, null);
                }
            }).start();
        }
        flowTaskDao.deleteNotClaimTask(actTaskId, id);
        OperateResult result = OperateResult.operationSuccess("10012");
        return result;
    }


    /**
     * 任务签收
     *
     * @param taskId 任务id
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseData claimTaskOfPhone(String taskId) {
        String userId = ContextUtil.getUserId();
        OperateResult result = this.claim(taskId, userId);
        if (!result.successful()) {
            return ResponseData.operationFailure(result.getMessage());
        }
        return ResponseData.operationSuccess();
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperateResultWithData<FlowStatus> complete(FlowTaskCompleteVO flowTaskCompleteVO) throws Exception {
        String taskId = flowTaskCompleteVO.getTaskId();
        Map<String, Object> variables = flowTaskCompleteVO.getVariables();
        Map<String, String> manualSelectedNodes = flowTaskCompleteVO.getManualSelectedNode();
        OperateResultWithData<FlowStatus> result;
        try {
            if (CollectionUtils.isEmpty(manualSelectedNodes)) {//非人工选择任务的情况
                //是否直接返回上一个任务
                Boolean boo = this.whetherOrNotReturnLastNode(taskId);
                if (boo) {
                    result = this.returnLastTask(taskId, flowTaskCompleteVO.getOpinion(), variables);
                } else {
                    result = this.complete(taskId, flowTaskCompleteVO.getOpinion(), variables);
                }
            } else {//人工选择任务的情况
                FlowTask flowTask = flowTaskDao.findOne(taskId);
                if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
                    return OperateResultWithData.operationFailure("10197");
                }
                String taskJsonDef = flowTask.getTaskJsonDef();
                JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                String nodeType = taskJsonDefObj.get("nodeType") + "";//针对审批网关的情况
                String actTaskId = flowTask.getActTaskId();
                // 取得当前任务
                HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(actTaskId).singleResult();
                ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
                if (definition == null) {
                    LogUtil.error(ContextUtil.getMessage("10003"));
                }
                // 取得当前活动定义节点
                ActivityImpl currActivity = ((ProcessDefinitionImpl) definition).findActivity(currTask.getTaskDefinitionKey());
                if ("Approve".equalsIgnoreCase(nodeType)) {//针对审批任务的情况
                    currActivity = (ActivityImpl) currActivity.getOutgoingTransitions().get(0).getDestination();
                    String defaultSequenId = (String) currActivity.getProperty("default");
                    Map<PvmTransition, String> oriPvmTransitionMap = new LinkedHashMap<>();
                    List<PvmTransition> pvmTransitionList = currActivity.getOutgoingTransitions();
                    for (PvmTransition pvmTransition : pvmTransitionList) {
                        String uelText = (String) pvmTransition.getProperty("conditionText");
                        if (pvmTransition.getId().equals(defaultSequenId)) {
                            continue;
                        }
                        for (Map.Entry<String, String> entry : manualSelectedNodes.entrySet()) {
                            String nodeId = entry.getValue();
                            ActivityImpl destinationActivity = ((ProcessDefinitionImpl) definition).findActivity(nodeId);
                            if (destinationActivity != null && FlowTaskTool.checkNextHas(pvmTransition.getDestination(), destinationActivity)) {
                                oriPvmTransitionMap.put(pvmTransition, uelText);
                                String proName = destinationActivity.getId() + "_approveResult";
                                uelText = "${" + proName + " == true}";
                                UelExpressionCondition uel = new UelExpressionCondition(uelText);
                                ((ProcessElementImpl) pvmTransition).setProperty("condition", uel);
                                ((ProcessElementImpl) pvmTransition).setProperty("conditionText", uelText);
                                variables.put(proName, true);
                            }
                        }
                    }
                    variables.put("approveResult", null);
                    //执行任务
                    try {
                        result = complete(taskId, flowTaskCompleteVO.getOpinion(), variables);
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        if (!CollectionUtils.isEmpty(oriPvmTransitionMap)) {
                            for (Map.Entry<PvmTransition, String> entry : oriPvmTransitionMap.entrySet()) {
                                PvmTransition pvmTransition = entry.getKey();
                                String uelText = entry.getValue();
                                UelExpressionCondition uel = new UelExpressionCondition(uelText);
                                ((ProcessElementImpl) pvmTransition).setProperty("condition", uel);
                                ((ProcessElementImpl) pvmTransition).setProperty("conditionText", uelText);
                            }
                        }
                    }
                } else {//针对人工网关的情况
                    ActivityImpl currActivityTemp = (ActivityImpl) currActivity.getOutgoingTransitions().get(0).getDestination();
                    boolean gateWay = FlowTaskTool.ifExclusiveGateway(currActivityTemp);
                    if (gateWay) {
                        currActivity = currActivityTemp;
                    }
                    Map<PvmTransition, String> oriPvmTransitionMap = new LinkedHashMap<>();
                    List<PvmTransition> pvmTransitionList = currActivity.getOutgoingTransitions();
                    for (PvmTransition pvmTransition : pvmTransitionList) {
                        ((ProcessElementImpl) pvmTransition).setProperty("condition", null);
                        ((ProcessElementImpl) pvmTransition).setProperty("conditionText", null);
                        UelExpressionCondition uel = (UelExpressionCondition) pvmTransition.getProperty("condition");
                        String uelText = (String) pvmTransition.getProperty("conditionText");
                        boolean isSet = false;
                        for (Map.Entry<String, String> entry : manualSelectedNodes.entrySet()) {
                            String nodeId = entry.getValue();
                            if (!nodeId.equals(entry.getKey())) {//存在子流程的情况
                                String path = entry.getKey();
                                String[] resultArray = path.split("/");
                                nodeId = resultArray[2];
                            }
                            ActivityImpl destinationActivity = ((ProcessDefinitionImpl) definition).findActivity(nodeId);
                            if (destinationActivity != null && FlowTaskTool.checkNextHas(pvmTransition.getDestination(), destinationActivity)) {
                                oriPvmTransitionMap.put(pvmTransition, uelText);
                                String proName = destinationActivity.getId() + "_approveResult";
                                uelText = "${" + proName + " == true}";
                                uel = new UelExpressionCondition(uelText);
                                ((ProcessElementImpl) pvmTransition).setProperty("condition", uel);
                                ((ProcessElementImpl) pvmTransition).setProperty("conditionText", uelText);
                                variables.put(proName, true);
                                isSet = true;
                                break;
                            }
                        }
                        if (gateWay && !isSet && (uel == null || StringUtils.isEmpty(uelText))) {
                            oriPvmTransitionMap.put(pvmTransition, uelText);
                            uelText = "${0>1}";
                            uel = new UelExpressionCondition(uelText);
                            ((ProcessElementImpl) pvmTransition).setProperty("condition", uel);
                            ((ProcessElementImpl) pvmTransition).setProperty("conditionText", uelText);
                        }
                    }
                    try {
                        //执行任务
                        result = complete(taskId, flowTaskCompleteVO.getOpinion(), variables);
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        if (!CollectionUtils.isEmpty(oriPvmTransitionMap)) {
                            for (Map.Entry<PvmTransition, String> entry : oriPvmTransitionMap.entrySet()) {
                                PvmTransition pvmTransition = entry.getKey();
                                String uelText = entry.getValue();
                                if (StringUtils.isNotEmpty(uelText)) {
                                    UelExpressionCondition uel = new UelExpressionCondition(uelText);
                                    ((ProcessElementImpl) pvmTransition).setProperty("condition", uel);
                                    ((ProcessElementImpl) pvmTransition).setProperty("conditionText", uelText);
                                } else {
                                    ((ProcessElementImpl) pvmTransition).setProperty("condition", null);
                                    ((ProcessElementImpl) pvmTransition).setProperty("conditionText", null);
                                }
                            }
                        }
                    }


                }
            }
        } catch (FlowException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result = OperateResultWithData.operationFailure(e.getMessage());
            LogUtil.error(e.getMessage(), e);
        }
        return result;
    }


    /**
     * 完成任务
     *
     * @param id        任务id
     * @param opinion   审批意见
     * @param variables 参数
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResultWithData<FlowStatus> complete(String id, String opinion, Map<String, Object> variables) throws Exception {

        Boolean setValue = redisTemplate.opsForValue().setIfAbsent("complete_" + id, id);
        if (!setValue) {
            Long remainingTime = redisTemplate.getExpire("complete_" + id, TimeUnit.SECONDS);
            if (remainingTime == -1) {  //说明时间未设置进去
                redisTemplate.expire("complete_" + id, 10 * 60, TimeUnit.SECONDS);
                remainingTime = 600L;
            }
            throw new FlowException(ContextUtil.getMessage("10198", remainingTime));
        }

        try {
            //处理redis检查设置10分钟过期
            redisTemplate.expire("complete_" + id, 10 * 60, TimeUnit.SECONDS);

            FlowTask flowTask = flowTaskDao.findOne(id);
            variables.put("flowCurrentTaskId", id);
            if (flowTask == null) {
                return OperateResultWithData.operationFailure("10033");
            }
            if (flowTask.getTrustState() != null) {
                if (flowTask.getTrustState() == 1) {
                    return OperateResultWithData.operationFailure("10199");
                } else if (flowTask.getTrustState() == 2) {
                    return OperateResultWithData.operationFailure("10200", "提交到下一步");
                }
            }
            if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
                return OperateResultWithData.operationFailure("10197");
            }


            String taskJsonDef = flowTask.getTaskJsonDef();
            JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
            JSONObject normalInfo = taskJsonDefObj.getJSONObject("nodeConfig").getJSONObject("normal");
            String nodeType = taskJsonDefObj.get("nodeType") + "";

            //选择不同意原因逻辑检查
            if ("Approve".equalsIgnoreCase(nodeType) && normalInfo.has("chooseDisagreeReason") && normalInfo.getBoolean("chooseDisagreeReason")) {
                String approved = variables.get("approved") + "";
                if ("true".equalsIgnoreCase(approved)) {
                    variables.put("disagreeReasonCode", null);
                } else {
                    if (variables.get("disagreeReasonCode") == null) {
                        throw new FlowException(ContextUtil.getMessage("10202"));
                    } else {
                        String disagreeReasonCode = (String) variables.get("disagreeReasonCode");
                        if ("common_else".equals(disagreeReasonCode) && StringUtils.isEmpty(opinion)) {
                            throw new FlowException(ContextUtil.getMessage("10203"));
                        }
                    }
                }
            } else {
                variables.put("disagreeReasonCode", null);
            }

            //审批任务，推送判断是同意还是不同意
            if ("Approve".equalsIgnoreCase(nodeType)) {
                String approved = variables.get("approved") + "";
                if ("true".equalsIgnoreCase(approved)) {
                    flowTask.setApproveStatus("agree");
                } else {
                    flowTask.setApproveStatus("disagree");
                }
            }


            String userId = ContextUtil.getUserId();
            if (flowTask.getExecutorId() != null && !flowTask.getExecutorId().equals(userId)) {
                //根据被授权人和待办查询符合的转授权信息（通过查看模式）
                TaskMakeOverPower bean = taskMakeOverPowerService.findPowerByPowerUserAndTask(userId, flowTask);
                if (bean != null) {
                    //转授权情况替换执行人(共同查看模式-授权人处理)
                    flowTask.setExecutorId(ContextUtil.getUserId());
                    flowTask.setExecutorAccount(ContextUtil.getUserAccount());
                    flowTask.setExecutorName(ContextUtil.getUserName());
                    //添加组织机构信息
                    flowTask.setExecutorOrgId(bean.getPowerUserOrgId());
                    flowTask.setExecutorOrgCode(bean.getPowerUserOrgCode());
                    flowTask.setExecutorOrgName(bean.getPowerUserOrgName());
                }
            }
            FlowInstance flowInstance = flowTask.getFlowInstance();
            //如果是转授权转办模式（获取转授权记录信息）
            String OverPowerStr = taskMakeOverPowerService.getOverPowerStrByDepict(flowTask.getDepict());
            flowTask.setDepict(OverPowerStr + opinion);
            Integer reject = null;
            Integer returnS = null;
            Boolean manageSolidifyFlow = false;
            if (variables != null) {
                //驳回
                Object rejectO = variables.get("reject");
                if (rejectO != null) {
                    try {
                        reject = Integer.parseInt(rejectO.toString());
                    } catch (Exception e) {
                    }
                }
                //退回
                Object returnSo = variables.get("return");
                if (returnSo != null) {
                    try {
                        returnS = Integer.parseInt(returnSo.toString());
                    } catch (Exception e) {
                    }
                }

                Object manageSolidifyFlowO = variables.get("manageSolidifyFlow");
                if (manageSolidifyFlowO != null) {
                    manageSolidifyFlow = Boolean.parseBoolean(manageSolidifyFlowO.toString());
                }
            }
            if (reject != null && reject == 1) {
                flowTask.setDepict("【被驳回】" + flowTask.getDepict());
                flowTask.setTaskStatus(TaskStatus.REJECT.toString());
                flowTask.setPriority(1);
            } else if (returnS != null && returnS == 1) {
                flowTask.setDepict("【被退回】" + flowTask.getDepict());
                flowTask.setTaskStatus(TaskStatus.RETURN.toString());
                flowTask.setPriority(1);
            } else {
                flowTask.setTaskStatus(TaskStatus.COMPLETED.toString());
            }
            //匿名用户执行的情况，直接使用当前上下文用户
            if (Constants.ANONYMOUS.equalsIgnoreCase(flowTask.getOwnerId()) && !Constants.ANONYMOUS.equalsIgnoreCase(ContextUtil.getUserId())) {
                flowTask.setOwnerId(ContextUtil.getUserId());
                flowTask.setOwnerAccount(ContextUtil.getUserAccount());
                flowTask.setOwnerName(ContextUtil.getUserName());
                flowTask.setExecutorId(ContextUtil.getUserId());
                flowTask.setExecutorAccount(ContextUtil.getUserAccount());
                flowTask.setExecutorName(ContextUtil.getUserName());
                //添加组织机构信息
                Executor executor = flowCommonUtil.getBasicUserExecutor(ContextUtil.getUserId());
                flowTask.setOwnerOrgId(executor.getOrganizationId());
                flowTask.setOwnerOrgCode(executor.getOrganizationCode());
                flowTask.setOwnerOrgName(executor.getOrganizationName());
                flowTask.setExecutorOrgId(executor.getOrganizationId());
                flowTask.setExecutorOrgCode(executor.getOrganizationCode());
                flowTask.setExecutorOrgName(executor.getOrganizationName());
            }

            if (StringUtils.isNotEmpty(flowTask.getDepict()) && flowTask.getDepict().length() > 2000) {
                throw new FlowException(ContextUtil.getMessage("10204", flowTask.getDepict().length()));
            }
            variables.put("opinion", flowTask.getDepict());
            String actTaskId = flowTask.getActTaskId();

            //获取当前业务实体表单的条件表达式信息，（目前是任务执行时就注入，后期根据条件来优化)
            String businessId = flowInstance.getBusinessId();
            BusinessModel businessModel = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel();
            Map<String, Object> v = ExpressionUtil.getPropertiesValuesMap(businessModel, businessId, true);
            if (!CollectionUtils.isEmpty(v)) {
                if (variables == null) {
                    variables = new HashMap<>();
                }
                variables.putAll(v);
            }

            Boolean counterSignLastTask = false;
            //得到当前执行节点自定义code
            String currentNodeCode = "";
            try {
                currentNodeCode = normalInfo.getString("nodeCode");
            } catch (Exception e) {
            }
            variables.put("currentNodeCode", currentNodeCode);

            // 取得当前任务
            HistoricTaskInstance currTask = historyService
                    .createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId())
                    .singleResult();
            if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务做处理判断
                String executionId = currTask.getExecutionId();
                Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(executionId);
                //完成会签的次数
                Integer completeCounter = (Integer) processVariables.get("nrOfCompletedInstances").getValue();
                if (completeCounter == 0) {//表示会签第一人审批（初始化赞成、不赞成、弃权参数）
                    runtimeService.setVariable(currTask.getProcessInstanceId(), Constants.COUNTER_SIGN_AGREE + currTask.getTaskDefinitionKey(), 0);
                    runtimeService.setVariable(currTask.getProcessInstanceId(), Constants.COUNTER_SIGN_OPPOSITION + currTask.getTaskDefinitionKey(), 0);
                    runtimeService.setVariable(currTask.getProcessInstanceId(), Constants.COUNTER_SIGN_WAIVER + currTask.getTaskDefinitionKey(), 0);
                    processVariables = runtimeService.getVariableInstances(executionId);
                }

                //总循环次数
                Integer instanceOfNumbers = (Integer) processVariables.get("nrOfInstances").getValue();
                //会签决策比
                int counterDecision = 100;
                try {
                    counterDecision = normalInfo.getInt("counterDecision");
                } catch (Exception e) {
                }
                //会签结果是否即时生效
                Boolean immediatelyEnd = false;
                try {
                    immediatelyEnd = normalInfo.getBoolean("immediatelyEnd");
                } catch (Exception e) {
                }

                if (completeCounter + 1 == instanceOfNumbers) {//会签最后一个任务
                    counterSignLastTask = true;
                    //通过票数
                    Integer counterSignAgree = 0;
                    if (processVariables.get(Constants.COUNTER_SIGN_AGREE + currTask.getTaskDefinitionKey()) != null) {
                        counterSignAgree = (Integer) processVariables.get(Constants.COUNTER_SIGN_AGREE + currTask.getTaskDefinitionKey()).getValue();
                    }

                    String approved = variables.get("approved") + "";
                    if ("true".equalsIgnoreCase(approved)) {
                        counterSignAgree++;
                    }
                    ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                            .getDeployedProcessDefinition(currTask
                                    .getProcessDefinitionId());
                    if (definition == null) {
                        LogUtil.error(ContextUtil.getMessage("10003"));
                    }
                    if (counterDecision <= ((counterSignAgree / (instanceOfNumbers + 0.0)) * 100)) {//获取通过节点
                        variables.put("approveResult", true);
                    } else {
                        variables.put("approveResult", false);
                    }
                    //执行任务
                    this.completeActiviti(actTaskId, variables);
                } else if (immediatelyEnd) { //会签结果是否即时生效
                    String approved = variables.get("approved") + "";
                    //并串行会签（false为并行：并行将已生成的待办转已办，串行在最后人审批中记录）
                    Boolean isSequential = normalInfo.getBoolean("isSequential");

                    if ("true".equalsIgnoreCase(approved)) {
                        //通过票数
                        Integer counterSignAgree = 0;
                        if (processVariables.get(Constants.COUNTER_SIGN_AGREE + currTask.getTaskDefinitionKey()) != null) {
                            counterSignAgree = (Integer) processVariables.get(Constants.COUNTER_SIGN_AGREE + currTask.getTaskDefinitionKey()).getValue();
                        }
                        counterSignAgree++;
                        if (counterDecision <= ((counterSignAgree / (instanceOfNumbers + 0.0)) * 100)) {//通过
                            //清除其他会签执行人
                            flowTaskTool.counterSignImmediatelyEnd(flowTask, flowInstance, variables, isSequential, executionId, instanceOfNumbers);
                            variables.put("approveResult", true);
                            counterSignLastTask = true; //即时结束后，当前任务算最后一个节点
                        }
                    } else {
                        //不通过票数
                        Integer counterSignOpposition = 0;
                        if (processVariables.get(Constants.COUNTER_SIGN_OPPOSITION + currTask.getTaskDefinitionKey()) != null && completeCounter != 0) {
                            counterSignOpposition = (Integer) processVariables.get(Constants.COUNTER_SIGN_OPPOSITION + currTask.getTaskDefinitionKey()).getValue();
                        }
                        counterSignOpposition++;
                        if ((100 - counterDecision) < ((counterSignOpposition / (instanceOfNumbers + 0.0)) * 100)) {//不通过
                            //清除其他会签执行人
                            flowTaskTool.counterSignImmediatelyEnd(flowTask, flowInstance, variables, isSequential, executionId, instanceOfNumbers);
                            variables.put("approveResult", false);
                            counterSignLastTask = true; //即时结束后，当前任务算最后一个节点
                        }
                    }
                    this.completeActiviti(actTaskId, variables);
                } else {
                    this.completeActiviti(actTaskId, variables);
                }

            } else if ("Approve".equalsIgnoreCase(nodeType)) {
                String approved = variables.get("approved") + "";
                if ("true".equalsIgnoreCase(approved)) {
                    variables.put("approveResult", true);
                } else {
                    variables.put("approveResult", false);
                }
                this.completeActiviti(actTaskId, variables);
                counterSignLastTask = true;
            } else if ("ParallelTask".equalsIgnoreCase(nodeType) || "SerialTask".equalsIgnoreCase(nodeType)) {
                String executionId = currTask.getExecutionId();
                Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(executionId);
                //完成会签的次数
                Integer completeCounter = (Integer) processVariables.get("nrOfCompletedInstances").getValue();
                //总循环次数
                Integer instanceOfNumbers = (Integer) processVariables.get("nrOfInstances").getValue();
                if (completeCounter + 1 == instanceOfNumbers) {//最后一个任务
                    counterSignLastTask = true;
                }
                this.completeActiviti(actTaskId, variables);
            } else {
                this.completeActiviti(actTaskId, variables);
                counterSignLastTask = true;
            }
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(actTaskId).singleResult(); // 创建历史任务实例查询

            // 取得流程实例
            ProcessInstance instance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(historicTaskInstance.getProcessInstanceId())
                    .singleResult();
            //是否推送信息到baisc
            Boolean pushBasic = this.getBooleanPushTaskToBasic();

            List<FlowTask> oldList = new ArrayList<>();
            List<FlowTask> needDelList = new ArrayList<>();

            if (historicTaskInstance != null) {

                Boolean canCancel = null;
                if (normalInfo.get("allowPreUndo") != null) {
                    canCancel = normalInfo.getBoolean("allowPreUndo");
                }
                FlowHistory flowHistory = flowTaskTool.initFlowHistory(flowTask, historicTaskInstance, canCancel, variables);

                if (manageSolidifyFlow) {  //需要处理流程固化表(添加逻辑执行顺序)
                    flowSolidifyExecutorService.manageSolidifyFlowByBusinessIdAndTaskKey(businessId, flowTask);
                }


                //需要异步推送待办转已办信息到basic、<业务模块>、<配置的url>
                if (pushBasic) {
                    oldList.add(flowTask);
                }
                flowHistoryDao.save(flowHistory);
                //单签任务，清除其他待办;工作池任务新增多执行人模式，也需要清楚其他待办
                if ("SingleSign".equalsIgnoreCase(nodeType) || "PoolTask".equalsIgnoreCase(nodeType)) {
                    //需要异步推送删除待办信息到basic
                    if (pushBasic) {
                        List<FlowTask> alllist = flowTaskDao.findListByProperty("actTaskId", actTaskId);
                        List<FlowTask> DelList = alllist.stream().filter((a) -> !a.getId().equalsIgnoreCase(id)).collect(Collectors.toList());
                        needDelList.addAll(DelList);
                    }
                    flowTaskDao.deleteByActTaskId(actTaskId);
//                    flowTaskDao.deleteNotClaimTask(actTaskId,flowTask.getId());
                } else {
                    flowTaskDao.delete(flowTask);
                }
                //初始化新的任务
                String actTaskDefKey = flowTask.getActTaskDefKey();
                String actProcessDefinitionId = flowTask.getFlowInstance().getFlowDefVersion().getActDefId();
                ProcessDefinitionEntity definition;
                PvmActivity currentNode;
                FlowInstance flowInstanceTemp = flowInstance;
                FlowInstance flowInstanceP = flowInstanceTemp.getParent();
                boolean sonEndButParnetNotEnd = false;
                while (flowInstanceTemp.isEnded() && (flowInstanceP != null)) {//子流程结束，主流程未结束
                    if (!flowInstanceP.isEnded()) {
                        actProcessDefinitionId = flowInstanceP.getFlowDefVersion().getActDefId();
                        definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                                .getDeployedProcessDefinition(actProcessDefinitionId);
                        String superExecutionId = (String) runtimeService.getVariable(flowInstanceP.getActInstanceId(), flowInstanceTemp.getActInstanceId() + "_superExecutionId");
                        HistoricActivityInstance historicActivityInstance = null;
                        HistoricActivityInstanceQuery his = historyService.createHistoricActivityInstanceQuery()
                                .executionId(superExecutionId).activityType("callActivity");
                        if (his != null) {
                            historicActivityInstance = his.singleResult();
                            HistoricActivityInstanceEntity he = (HistoricActivityInstanceEntity) historicActivityInstance;
                            actTaskDefKey = he.getActivityId();
                            currentNode = FlowTaskTool.getActivitNode(definition, actTaskDefKey);
                            callInitTaskBack(currentNode, flowInstanceP, flowHistory, counterSignLastTask, variables);
                        }
                    }
                    sonEndButParnetNotEnd = true;
                    flowInstanceTemp = flowInstanceP;
                    flowInstanceP = flowInstanceTemp.getParent();
                }
                if (!sonEndButParnetNotEnd) {
                    definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                            .getDeployedProcessDefinition(actProcessDefinitionId);
                    currentNode = FlowTaskTool.getActivitNode(definition, actTaskDefKey);
                    if (instance != null && currentNode != null && (!"endEvent".equalsIgnoreCase(currentNode.getProperty("type") + ""))) {
                        callInitTaskBack(currentNode, flowInstance, flowHistory, counterSignLastTask, variables);
                    }
                }
            }
            OperateResultWithData<FlowStatus> result = OperateResultWithData.operationSuccess("10017");
            if (instance == null || instance.isEnded()) {
                result.setData(FlowStatus.COMPLETED);//任务结束
                flowSolidifyExecutorDao.deleteByBusinessId(flowInstance.getBusinessId());//查看是否为固化流程（如果是固化流程删除固化执行人列表）
                //需要异步推送归档信息到basic
                if (pushBasic) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pushToBasic(null, oldList, needDelList, flowTask);
                        }
                    }).start();
                }
            } else {
                if (pushBasic) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pushToBasic(null, oldList, needDelList, null);
                        }
                    }).start();
                }
            }
            return result;

        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw e;
        } finally {
            //处理开始的时候设置的检查参数
            redisTemplate.delete("complete_" + id);
        }
    }


    private void callInitTaskBack(PvmActivity currentNode, FlowInstance flowInstance, FlowHistory flowHistory, boolean counterSignLastTask, Map<String, Object> variables) {
        if (!counterSignLastTask && FlowTaskTool.ifMultiInstance(currentNode)) {
            String sequential = currentNode.getProperty("multiInstance") + "";
            if ("sequential".equalsIgnoreCase(sequential)) {//会签当中串行任务,非最后一个任务
                String key = currentNode.getProperty("key") != null ? currentNode.getProperty("key").toString() : null;
                if (key == null) {
                    key = currentNode.getId();
                }
                flowTaskTool.initTask(flowInstance, flowHistory, key, variables);
                return;
            }
        }
        List<PvmTransition> nextNodes = currentNode.getOutgoingTransitions();
        if (!CollectionUtils.isEmpty(nextNodes)) {
            for (PvmTransition node : nextNodes) {
                PvmActivity nextActivity = node.getDestination();
                if (FlowTaskTool.ifGageway(nextActivity) || "ManualTask".equalsIgnoreCase(nextActivity.getProperty("type") + "")) {
                    callInitTaskBack(nextActivity, flowInstance, flowHistory, counterSignLastTask, variables);
                    continue;
                }
                String key = nextActivity.getProperty("key") != null ? nextActivity.getProperty("key").toString() : null;
                if (key == null) {
                    key = nextActivity.getId();
                }
                if ("serviceTask".equalsIgnoreCase(nextActivity.getProperty("type") + "")) {
                } else if ("CallActivity".equalsIgnoreCase(nextActivity.getProperty("type") + "") && counterSignLastTask) {
                    flowTaskTool.initTask(flowInstance, flowHistory, null, variables);
                } else {
                    flowTaskTool.initTask(flowInstance, flowHistory, key, variables);
                }
            }
        }
    }

    /**
     * 撤回到指定任务节点,加撤销意见
     * 将来弃用，新方法是rollBackToHis
     */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperateResult rollBackTo(String id, String opinion) {
        FlowHistory flowHistory = flowHistoryDao.findOne(id);
        if (flowHistory == null) {
            return OperateResult.operationFailure("10205");
        }
        if (TaskStatus.VIRTUAL.toString().equals(flowHistory.getTaskStatus())) { //虚拟任务
            return OperateResult.operationFailure("10206");
        }
        String flowInstanceId = flowHistory.getFlowInstance().getId();
        OperateResult operateResult = flowTaskTool.taskRollBack(flowHistory, opinion);
        if (operateResult.getSuccess()) {
            new Thread(new Runnable() {//重置固化执行表顺序
                @Override
                public void run() {
                    flowSolidifyExecutorService.setSolidifyExecutorTaskOrder(flowInstanceId);
                }
            }).start();
        }
        return operateResult;
    }


    @Override
    public ResponseData rollBackToHis(RollBackParam rollBackParam) {
        String id = rollBackParam.getId();
        String opinion = rollBackParam.getOpinion();
        if (StringUtils.isEmpty(id)) {
            return ResponseData.operationFailure("10207");
        }
        if (StringUtils.isEmpty(opinion)) {
            return ResponseData.operationFailure("10208");
        }
        return this.rollBackTo(id, opinion);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseData rollBackToOfPhone(String preTaskId, String opinion) {
        OperateResult res = rollBackTo(preTaskId, opinion);
        if (!res.successful()) {
            return ResponseData.operationFailure(res.getMessage());
        }
        return ResponseData.operationSuccess();
    }

    /**
     * 签收任务
     *
     * @param taskId
     * @param userId
     */
    private void claimActiviti(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }

    /**
     * 完成任务
     *
     * @param taskId
     * @param variables
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void completeActiviti(String taskId, Map<String, Object> variables) throws Exception {
        taskService.complete(taskId, variables);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public ResponseData rejectTaskOfPhone(String taskId, String opinion) throws Exception {
        OperateResult result = this.taskReject(taskId, opinion, null);
        if (!result.successful()) {
            return ResponseData.operationFailure(result.getMessage());
        }
        return ResponseData.operationSuccess();

    }

    @Override
    public ResponseData taskRejectToHis(RejectParam rejectParam) throws Exception {
        String opinion = StringUtils.isNotEmpty(rejectParam.getRemark()) ? rejectParam.getRemark() : rejectParam.getOpinion();
        return this.taskReject(rejectParam.getId(), opinion, null);
    }

    /**
     * 任务驳回
     *
     * @param id        任务id
     * @param variables 参数
     * @return 结果
     */
    public OperateResult taskReject(String id, String opinion, Map<String, Object> variables) throws Exception {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return OperateResult.operationFailure("10209");
        }
        if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
            return OperateResult.operationFailure("10210");
        }
        if (flowTask.getTrustState() != null) {
            if (flowTask.getTrustState() == 1) {
                //当前任务已委托出去，等待委托方处理后才能处理！
                return OperateResult.operationFailure("10199");
            } else if (flowTask.getTrustState() == 2) {
                return OperateResult.operationFailure("10200", "进行驳回");
            }
        }
        flowTask.setDepict(opinion);
        try {
            if (flowTask != null && StringUtils.isNotEmpty(flowTask.getPreId())) {
                FlowHistory preFlowTask = flowHistoryDao.findOne(flowTask.getPreId());
                if (preFlowTask == null) {
                    return OperateResult.operationFailure("10211");
                } else {
                    String flowInstanceId = flowTask.getFlowInstance().getId();
                    OperateResult operateResult = this.activitiReject(flowTask, preFlowTask, variables);
                    if (operateResult.successful()) {
                        new Thread(new Runnable() {//重置固化执行表顺序
                            @Override
                            public void run() {
                                flowSolidifyExecutorService.setSolidifyExecutorTaskOrder(flowInstanceId);
                            }
                        }).start();
                    }
                    return operateResult;
                }
            } else {
                return OperateResult.operationFailure("10212");
            }
        } catch (FlowException e) {
            LogUtil.error("驳回失败：{}", e.getMessage(), e);
            return OperateResult.operationFailure("10415", e.getMessage());
        }
    }


    /**
     * 驳回前一个任务
     *
     * @param currentTask 当前任务
     * @param preFlowTask 上一个任务
     * @return 结果
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResult activitiReject(FlowTask currentTask, FlowHistory preFlowTask, Map<String, Object> variables) throws Exception {
        // 取得当前任务
        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(currentTask.getActTaskId()).singleResult();
        // 取得流程实例
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(currTask.getProcessInstanceId()).singleResult();
        if (instance == null) {
            return OperateResult.operationFailure("10213");
        }
        if (variables == null) {
            variables = new HashMap();
        }
        Map variablesProcess = instance.getProcessVariables();
        Map variablesTask = currTask.getTaskLocalVariables();
        if (!CollectionUtils.isEmpty(variablesProcess)) {
            variables.putAll(variablesProcess);
        }
        if (!CollectionUtils.isEmpty(variablesTask)) {
            variables.putAll(variablesTask);
        }

        // 取得流程定义
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
        if (definition == null) {
            return OperateResult.operationFailure("10214");
        }

        // 取得当前任务标节点的活动
        ActivityImpl currentActivity = ((ProcessDefinitionImpl) definition).findActivity(currentTask.getActTaskDefKey());
        // 取得驳回目标节点的活动
        while ("cancel".equalsIgnoreCase(preFlowTask.getTaskStatus()) || "reject".equalsIgnoreCase(preFlowTask.getTaskStatus()) || preFlowTask.getActTaskDefKey().equals(currentTask.getActTaskDefKey())) {//如果前一任务为撤回或者驳回任务，则依次向上迭代
            String preFlowTaskId = preFlowTask.getPreId();
            if (StringUtils.isNotEmpty(preFlowTaskId)) {
                preFlowTask = flowHistoryDao.findOne(preFlowTaskId);
            } else {
                return OperateResult.operationFailure("10215");
            }
        }
        ActivityImpl preActivity = ((ProcessDefinitionImpl) definition).findActivity(preFlowTask.getActTaskDefKey());

        //检查是否满足驳回的条件
        ResponseData checkResponse = FlowTaskTool.checkCanReject(currentActivity, preActivity);
        if (checkResponse.getSuccess()) {
            //取活动，清除活动方向
            List<PvmTransition> oriPvmTransitionList = new ArrayList<>();
            List<PvmTransition> pvmTransitionList = currentActivity
                    .getOutgoingTransitions();
            for (PvmTransition pvmTransition : pvmTransitionList) {
                oriPvmTransitionList.add(pvmTransition);
            }
            pvmTransitionList.clear();
            //建立新方向
            TransitionImpl newTransition = currentActivity
                    .createOutgoingTransition();
            // 取得转向的目标，这里需要指定用需要回退到的任务节点
            newTransition.setDestination(preActivity);

            try {
                //完成任务
                variables.put("reject", 1);
                variables.put("approved", null);
                this.complete(currentTask.getId(), currentTask.getDepict(), variables);
            } catch (Exception e) {
                throw e;
            } finally {
                //恢复方向
                preActivity.getIncomingTransitions().remove(newTransition);
                List<PvmTransition> pvmTList = currentActivity
                        .getOutgoingTransitions();
                pvmTList.clear();
                for (PvmTransition pvmTransition : oriPvmTransitionList) {
                    pvmTransitionList.add(pvmTransition);
                }
                runtimeService.removeVariable(instance.getProcessInstanceId(), "reject");//将状态重置
            }
            return OperateResult.operationSuccess("10216");
        } else {
            return OperateResult.operationFailure(checkResponse.getMessage());
        }
    }


    /**
     * 获取出口节点信息（带初始化流程设计器配置用户）
     *
     * @param flowTask
     * @param approved
     * @return
     * @throws NoSuchMethodException
     */
    public List<NodeInfo> findNexNodesWithUserSet(FlowTask flowTask, String approved, List<String> includeNodeIds) throws NoSuchMethodException {
        List<NodeInfo> nodeInfoList = flowTaskTool.findNextNodesWithCondition(flowTask, approved, includeNodeIds);

        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            String flowDefJson = flowTask.getFlowInstance().getFlowDefVersion().getDefJson();
            JSONObject defObj = JSONObject.fromObject(flowDefJson);
            Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);

            String flowTaskDefJson = flowTask.getTaskJsonDef();
            JSONObject flowTaskDefObj = JSONObject.fromObject(flowTaskDefJson);
            String currentNodeType = flowTaskDefObj.get("nodeType") + "";
            JSONObject normalInfo = flowTaskDefObj.getJSONObject("nodeConfig").getJSONObject("normal");
            Boolean currentSingleTaskAuto = false;
            if (normalInfo != null && normalInfo.has("singleTaskNoChoose") && normalInfo.get("singleTaskNoChoose") != null) {
                currentSingleTaskAuto = normalInfo.getBoolean("singleTaskNoChoose");
            }
            Map<NodeInfo, List<NodeInfo>> nodeInfoSonMap = new LinkedHashMap();
            //是否是固化流程
            boolean solidifyFlow = BooleanUtils.isTrue(flowTask.getFlowInstance().getFlowDefVersion().getSolidifyFlow());
            for (NodeInfo nodeInfo : nodeInfoList) {
                nodeInfo.setCurrentTaskType(currentNodeType);
                nodeInfo.setCurrentSingleTaskAuto(currentSingleTaskAuto);
                if ("CounterSignNotEnd".equalsIgnoreCase(nodeInfo.getType())) {
                    continue;
                } else if ("serviceTask".equalsIgnoreCase(nodeInfo.getType())) {
                    nodeInfo.setUserVarName(nodeInfo.getId() + "_ServiceTask");
                    nodeInfo.setUiType("radiobox");
                    nodeInfo.setFlowTaskType("serviceTask");
                    String userId = ContextUtil.getSessionUser().getUserId();
                    Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                    if (executor != null) {//服务任务封装执行人信息（ID为操作人）
                        executor.setName("系统自动");
                        executor.setCode(Constants.ADMIN);
                        executor.setOrganizationName("系统自动执行的任务");
                        List<Executor> employeeSet = new ArrayList<>();
                        employeeSet.add(executor);
                        nodeInfo.setExecutorSet(employeeSet);
                    }
                } else if ("receiveTask".equalsIgnoreCase(nodeInfo.getType())) {
                    nodeInfo.setUserVarName(nodeInfo.getId() + "_ReceiveTask");
                    nodeInfo.setUiType("radiobox");
                    nodeInfo.setFlowTaskType("receiveTask");
                    String userId = ContextUtil.getSessionUser().getUserId();
                    Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                    if (executor != null) {//接收任务封装执行人信息（ID为操作人）
                        executor.setName("系统触发");
                        executor.setCode(Constants.ADMIN);
                        executor.setOrganizationName("等待系统触发后执行");
                        List<Executor> employeeSet = new ArrayList<>();
                        employeeSet.add(executor);
                        nodeInfo.setExecutorSet(employeeSet);
                    }
                } else if ("callActivity".equalsIgnoreCase(nodeInfo.getType())) {
                    List<NodeInfo> nodeInfoListSons = new ArrayList<>();
                    nodeInfoListSons = flowTaskTool.getCallActivityNodeInfo(flowTask, nodeInfo.getId(), nodeInfoListSons);
                    nodeInfoSonMap.put(nodeInfo, nodeInfoListSons);
                } else {
                    List<Executor> executorSet = nodeInfo.getExecutorSet();
                    if (!CollectionUtils.isEmpty(executorSet)) {
                        continue;
                    }
                    JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(nodeInfo.getId());

                    try {
                        JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject("normal");
                        Boolean allowChooseInstancy = normal.getBoolean("allowChooseInstancy");
                        nodeInfo.setAllowChooseInstancy(allowChooseInstancy);
                    } catch (Exception e) {
                    }

                    JSONObject executor = null;
                    net.sf.json.JSONArray executorList = null;//针对两个条件以上的情况
                    if (currentNode.getJSONObject(Constants.NODE_CONFIG).has(Constants.EXECUTOR)) {
                        try {
                            executor = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject(Constants.EXECUTOR);
                        } catch (Exception e) {
                            try {
                                executorList = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONArray(Constants.EXECUTOR);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            if (!CollectionUtils.isEmpty(executorList) && executorList.size() == 1) {
                                executor = executorList.getJSONObject(0);
                            }
                        }
                    }

                    UserTask userTaskTemp = (UserTask) JSONObject.toBean(currentNode, UserTask.class);
                    if (userTaskTemp.getType() != null && userTaskTemp.getType().contains("EndEvent")) {
                        //非固化流程配置了结束抄送
                        if (solidifyFlow || CollectionUtils.isEmpty(executor)) {
                            nodeInfo.setType("EndEvent");
                            continue;
                        } else {
                            nodeInfo.setType("EndEvent");
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_end");
                            nodeInfo.setUiType("checkbox");
                            //兼容组件逻辑
                            nodeInfo.setFlowTaskType("common");
                            nodeInfo.setAllowChooseInstancy(false);
                        }
                    }
                    if (StringUtils.isEmpty(nodeInfo.getUserVarName())) {
                        if ("Normal".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_Normal");
                        } else if ("SingleSign".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_SingleSign");
                            nodeInfo.setUiType("checkbox");
                        } else if ("Approve".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_Approve");
                            //如果是直接返回上一个节点，通过历史数据获取执行人信息
                            if (flowTask.getJumpBackPrevious() != null && flowTask.getJumpBackPrevious()) {
                                FlowHistory flowHistory = flowHistoryDao.findOne(flowTask.getPreId());
                                String executorId = flowHistory.getExecutorId();
                                Executor exe = flowCommonUtil.getBasicUserExecutor(executorId);
                                List<Executor> employeeSet = new ArrayList<>();
                                employeeSet.add(exe);
                                nodeInfo.setExecutorSet(employeeSet);
                                return nodeInfoList;
                            }
                        } else if ("CounterSign".equalsIgnoreCase(userTaskTemp.getNodeType()) || "ParallelTask".equalsIgnoreCase(userTaskTemp.getNodeType()) || "SerialTask".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_List_CounterSign");
                            nodeInfo.setUiType("checkbox");
                        }
                    }

                    if (!solidifyFlow) {
                        if (!CollectionUtils.isEmpty(executor)) {
                            String userType = (String) executor.get("userType");
                            String ids = (String) executor.get("ids");
                            List<Executor> employees = null;
                            nodeInfo.setUiUserType(userType);
                            if ("StartUser".equalsIgnoreCase(userType)) {//获取流程实例启动者
                                FlowInstance flowInstance = flowTask.getFlowInstance();
                                while (flowInstance.getParent() != null) { //以父流程的启动人为准
                                    flowInstance = flowInstance.getParent();
                                }
                                String startUserId = null;
                                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(flowInstance.getActInstanceId()).singleResult();
                                if (historicProcessInstance == null) {//当第一个任务为服务任务的时候存在为空的情况发生
                                    startUserId = ContextUtil.getUserId();
                                } else {
                                    startUserId = historicProcessInstance.getStartUserId();
                                }
                                //根据用户的id列表获取执行人
                                employees = flowCommonUtil.getBasicUserExecutors(Arrays.asList(startUserId));
                            } else {
                                String selfDefId = (String) executor.get("selfDefId");
                                if (StringUtils.isNotEmpty(ids) || StringUtils.isNotEmpty(selfDefId)) {
                                    if ("SelfDefinition".equalsIgnoreCase(userType)) {//通过业务ID获取自定义用户
                                        FlowExecutorConfig flowExecutorConfig = flowExecutorConfigDao.findOne(selfDefId);
                                        String path = flowExecutorConfig.getUrl();
                                        AppModule appModule = flowExecutorConfig.getBusinessModel().getAppModule();
                                        String appModuleCode = appModule.getApiBaseAddress();
                                        String businessId = flowTask.getFlowInstance().getBusinessId();
                                        String param = flowExecutorConfig.getParam();
                                        FlowInvokeParams flowInvokeParams = new FlowInvokeParams();
                                        flowInvokeParams.setId(businessId);
                                        flowInvokeParams.setJsonParam(param);
                                        String nodeCode = "";
                                        try {
                                            JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject("normal");
                                            nodeCode = normal.getString("nodeCode");
                                            if (StringUtils.isNotEmpty(nodeCode)) {
                                                Map<String, String> map = new HashMap<>();
                                                map.put("nodeCode", nodeCode);
                                                flowInvokeParams.setParams(map);
                                            }
                                        } catch (Exception e) {
                                        }
                                        employees = flowCommonUtil.getExecutorsBySelfDef(appModuleCode, flowExecutorConfig.getName(), path, flowInvokeParams);
                                    } else {
                                        //岗位或者岗位类型（Position、PositionType、AnyOne）、组织机构都改为单据的组织机构
                                        String currentOrgId = this.getOrgIdByFlowTask(flowTask);
                                        employees = flowTaskTool.getExecutors(userType, ids, currentOrgId);
                                    }
                                }
                            }
                            if (!CollectionUtils.isEmpty(employees)) {
                                List<Executor> returnExecutors = flowCommonUtil.setListExecutor(employees);
                                nodeInfo.setExecutorSet(returnExecutors);
                            }
                        } else if (!CollectionUtils.isEmpty(executorList) && executorList.size() > 1) {
                            List<Executor> employees;
                            String selfDefId = null;
                            List<String> orgDimensionCodes = null;//组织维度代码集合
                            List<String> positionIds = null;//岗位代码集合
                            List<String> orgIds = null; //组织机构id集合
                            List<String> positionTypesIds = null;//岗位类别id集合
                            for (Object executorObject : executorList.toArray()) {
                                JSONObject executorTemp = (JSONObject) executorObject;
                                String userType = executorTemp.get("userType") + "";
                                String ids = executorTemp.get("ids") + "";
                                List<String> tempList = null;
                                if (StringUtils.isNotEmpty(ids)) {
                                    String[] idsShuZhu = ids.split(",");
                                    tempList = Arrays.asList(idsShuZhu);
                                }
                                if ("SelfDefinition".equalsIgnoreCase(userType)) {//通过业务ID获取自定义用户
                                    selfDefId = executorTemp.get("selfDefOfOrgAndSelId") + "";
                                } else if ("Position".equalsIgnoreCase(userType)) {
                                    positionIds = tempList;
                                } else if ("OrganizationDimension".equalsIgnoreCase(userType)) {
                                    orgDimensionCodes = tempList;
                                } else if ("PositionType".equalsIgnoreCase(userType)) {
                                    positionTypesIds = tempList;
                                } else if ("Org".equalsIgnoreCase(userType)) {
                                    orgIds = tempList;
                                }
                            }
                            // 取得当前任务
                            String currentOrgId = this.getOrgIdByFlowTask(flowTask);
                            if (StringUtils.isNotEmpty(selfDefId) && !Constants.NULL_S.equalsIgnoreCase(selfDefId)) {
                                FlowExecutorConfig flowExecutorConfig = flowExecutorConfigDao.findOne(selfDefId);
                                String path = flowExecutorConfig.getUrl();
                                AppModule appModule = flowExecutorConfig.getBusinessModel().getAppModule();
                                String appModuleCode = appModule.getApiBaseAddress();
                                String param = flowExecutorConfig.getParam();
                                FlowInvokeParams flowInvokeParams = new FlowInvokeParams();
                                flowInvokeParams.setId(flowTask.getFlowInstance().getBusinessId());

                                flowInvokeParams.setOrgId(currentOrgId);
                                flowInvokeParams.setPositionIds(positionIds);
                                flowInvokeParams.setPositionTypeIds(positionTypesIds);
                                flowInvokeParams.setOrganizationIds(orgIds);
                                flowInvokeParams.setOrgDimensionCodes(orgDimensionCodes);

                                flowInvokeParams.setJsonParam(param);
                                try {
                                    JSONObject normal = currentNode.getJSONObject(Constants.NODE_CONFIG).getJSONObject("normal");
                                    String nodeCode = normal.getString("nodeCode");
                                    if (StringUtils.isNotEmpty(nodeCode)) {
                                        Map<String, String> map = new HashMap<>();
                                        map.put("nodeCode", nodeCode);
                                        flowInvokeParams.setParams(map);
                                    }
                                } catch (Exception e) {
                                }
                                employees = flowCommonUtil.getExecutorsBySelfDef(appModuleCode, flowExecutorConfig.getName(), path, flowInvokeParams);

                            } else {
                                if (positionTypesIds != null && orgIds != null) {
                                    //新增根据（岗位类别+组织机构）获得执行人
                                    employees = flowCommonUtil.getExecutorsByPostCatIdsAndOrgs(positionTypesIds, orgIds);
                                } else {
                                    //通过岗位ids、组织维度ids和组织机构id来获取执行人
                                    employees = flowCommonUtil.getExecutorsByPositionIdsAndorgDimIds(positionIds, orgDimensionCodes, currentOrgId);
                                }
                            }
                            if (!CollectionUtils.isEmpty(employees)) {
                                List<Executor> returnExecutors = flowCommonUtil.setListExecutor(employees);
                                nodeInfo.setExecutorSet(returnExecutors);
                            }
                        }
                    } else {
                        if (!CollectionUtils.isEmpty(executor)) {
                            String userType = (String) executor.get("userType");
                            nodeInfo.setUiUserType(userType);
                        }
                    }
                }
            }

            if (!CollectionUtils.isEmpty(nodeInfoSonMap)) {
                for (Map.Entry<NodeInfo, List<NodeInfo>> entry : nodeInfoSonMap.entrySet()) {
                    nodeInfoList.remove(entry.getKey());
                    nodeInfoList.addAll(entry.getValue());
                }
            }
        }
        return nodeInfoList;
    }


    /**
     * 固化流程获取出口节点信息（带初始化流程设计器配置用户）
     *
     * @param flowTask
     * @param approved
     * @return
     * @throws NoSuchMethodException
     */
    public List<NodeInfo> findNexNodesWithUserSetSolidifyFlow(FlowTask flowTask, String approved, List<String> includeNodeIds) throws NoSuchMethodException {
        List<NodeInfo> nodeInfoList = flowTaskTool.findNextNodesWithCondition(flowTask, approved, includeNodeIds);

        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            String flowDefJson = flowTask.getFlowInstance().getFlowDefVersion().getDefJson();
            JSONObject defObj = JSONObject.fromObject(flowDefJson);
            Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);

            String flowTaskDefJson = flowTask.getTaskJsonDef();
            JSONObject flowTaskDefObj = JSONObject.fromObject(flowTaskDefJson);
            String currentNodeType = flowTaskDefObj.get("nodeType") + "";
            JSONObject normalInfo = flowTaskDefObj.getJSONObject("nodeConfig").getJSONObject("normal");
            Boolean currentSingleTaskAuto = false;
            if (normalInfo != null && normalInfo.has("singleTaskNoChoose") && normalInfo.get("singleTaskNoChoose") != null) {
                currentSingleTaskAuto = normalInfo.getBoolean("singleTaskNoChoose");
            }
            Map<NodeInfo, List<NodeInfo>> nodeInfoSonMap = new LinkedHashMap();
            for (NodeInfo nodeInfo : nodeInfoList) {
                nodeInfo.setCurrentTaskType(currentNodeType);
                nodeInfo.setCurrentSingleTaskAuto(currentSingleTaskAuto);
                if ("CounterSignNotEnd".equalsIgnoreCase(nodeInfo.getType())) {
                    continue;
                } else if ("serviceTask".equalsIgnoreCase(nodeInfo.getType())) {
                    nodeInfo.setUserVarName(nodeInfo.getId() + "_ServiceTask");
                    nodeInfo.setUiType("radiobox");
                    nodeInfo.setFlowTaskType("serviceTask");
                    String userId = ContextUtil.getSessionUser().getUserId();
                    //通过用户id获取用户信息
                    Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                    if (executor != null) {//服务任务默认选择当前操作人
                        List<Executor> employeeSet = new ArrayList<>();
                        employeeSet.add(executor);
                        nodeInfo.setExecutorSet(employeeSet);
                    }
                } else if ("receiveTask".equalsIgnoreCase(nodeInfo.getType())) {
                    nodeInfo.setUserVarName(nodeInfo.getId() + "_ReceiveTask");
                    nodeInfo.setUiType("radiobox");
                    nodeInfo.setFlowTaskType("receiveTask");
                    String userId = ContextUtil.getSessionUser().getUserId();
                    //通过用户id获取用户信息
                    Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                    if (executor != null) {//接收任务默认选择当前操作人
                        List<Executor> employeeSet = new ArrayList<>();
                        employeeSet.add(executor);
                        nodeInfo.setExecutorSet(employeeSet);
                    }
                } else if ("callActivity".equalsIgnoreCase(nodeInfo.getType())) {
                    List<NodeInfo> nodeInfoListSons = new ArrayList<NodeInfo>();
                    nodeInfoListSons = flowTaskTool.getCallActivityNodeInfo(flowTask, nodeInfo.getId(), nodeInfoListSons);
                    nodeInfoSonMap.put(nodeInfo, nodeInfoListSons);
                } else {
                    List<Executor> executorSet = nodeInfo.getExecutorSet();
                    if (!CollectionUtils.isEmpty(executorSet)) {
                        continue;
                    }

                    JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(nodeInfo.getId());
                    nodeInfo.setAllowChooseInstancy(false);

                    UserTask userTaskTemp = (UserTask) JSONObject.toBean(currentNode, UserTask.class);
                    if ("EndEvent".equalsIgnoreCase(userTaskTemp.getType())) {
                        nodeInfo.setType("EndEvent");
                        continue;
                    }
                    if (StringUtils.isEmpty(nodeInfo.getUserVarName())) {
                        if ("Normal".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_Normal");
                        } else if ("SingleSign".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_SingleSign");
                            nodeInfo.setUiType("checkbox");
                        } else if ("Approve".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_Approve");
                        } else if ("CounterSign".equalsIgnoreCase(userTaskTemp.getNodeType()) || "ParallelTask".equalsIgnoreCase(userTaskTemp.getNodeType()) || "SerialTask".equalsIgnoreCase(userTaskTemp.getNodeType())) {
                            nodeInfo.setUserVarName(userTaskTemp.getId() + "_List_CounterSign");
                            nodeInfo.setUiType("checkbox");
                        }
                    }

                    if ("pooltask".equalsIgnoreCase(nodeInfo.getFlowTaskType())) {
                        nodeInfo.setExecutorSet(null);
                    } else if ("serviceTask".equalsIgnoreCase(nodeInfo.getFlowTaskType())) {
                        String userId = ContextUtil.getSessionUser().getUserId();
                        Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                        if (executor != null) {
                            List<Executor> employeeSet = new ArrayList<>();
                            employeeSet.add(executor);
                            nodeInfo.setExecutorSet(employeeSet);
                        }
                    } else {
                        String businessId = flowTask.getFlowInstance().getBusinessId();
                        Search search = new Search();
                        search.addFilter(new SearchFilter("businessId", businessId));
                        search.addFilter(new SearchFilter("actTaskDefKey", nodeInfo.getId()));
                        List<FlowSolidifyExecutor> solidifyExecutorlist = flowSolidifyExecutorDao.findByFilters(search);
                        if (!CollectionUtils.isEmpty(solidifyExecutorlist)) {
                            String userIds = solidifyExecutorlist.get(0).getExecutorIds();
                            String[] idArray = userIds.split(",");
                            List<String> idList = Arrays.asList(idArray);
                            List<Executor> list = flowCommonUtil.getBasicUserExecutors(idList);
                            if (!CollectionUtils.isEmpty(list)) {
                                List<Executor> returnExecutors = flowCommonUtil.setListExecutor(list);
                                nodeInfo.setExecutorSet(returnExecutors);
                            }
                        }
                    }
                }
            }
            if (!CollectionUtils.isEmpty(nodeInfoSonMap)) {
                for (Map.Entry<NodeInfo, List<NodeInfo>> entry : nodeInfoSonMap.entrySet()) {
                    nodeInfoList.remove(entry.getKey());
                    nodeInfoList.addAll(entry.getValue());
                }
            }
        }
        return nodeInfoList;
    }

    public ResponseData<List<NodeInfo>> findNextNodesOfGateway(String id) {
        try {
            List<NodeInfo> list = this.findNextNodes(id);
            return ResponseData.operationSuccessWithData(list);
        } catch (Exception e) {
            LogUtil.error("请求决策信息失败:{}", e.getMessage(), e);
            return ResponseData.operationFailure("10414", e.getMessage());
        }
    }

    public List<NodeInfo> findNextNodes(String id) throws NoSuchMethodException {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return null;
        }
        String businessId = flowTask.getFlowInstance().getBusinessId();
        return this.findNextNodes(id, businessId);
    }

    /**
     * 选择下一步执行的节点信息
     *
     * @param id
     * @return
     * @throws NoSuchMethodException
     */
    public List<NodeInfo> findNextNodes(String id, String businessId) throws NoSuchMethodException {
        return this.findNextNodes(id, businessId, null);
    }


    /**
     * 选择下一步执行的节点信息
     *
     * @param id
     * @return
     * @throws NoSuchMethodException
     */
    public List<NodeInfo> findNextNodes(String id, String businessId, List<String> includeNodeIds) throws NoSuchMethodException {
        FlowTask flowTask = flowTaskDao.findOne(id);
        return flowTaskTool.selectNextAllNodes(flowTask, includeNodeIds);
    }


    public ResponseData<ApprovalHeaderVO> getApprovalHeaderVoOfGateway(String id) {
        try {
            ApprovalHeaderVO approvalHeaderVO = this.getApprovalHeaderVO(id);
            return ResponseData.operationSuccessWithData(approvalHeaderVO);
        } catch (Exception e) {
            LogUtil.error("请求抬头信息失败:{}", e.getMessage(), e);
            return ResponseData.operationFailure("10413", e.getMessage());
        }
    }


    public ApprovalHeaderVO getApprovalHeaderVO(String id) {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return null;
        }
        String preId = flowTask.getPreId();
        FlowHistory preFlowTask = null;
        ApprovalHeaderVO result = new ApprovalHeaderVO();
        result.setBusinessId(flowTask.getFlowInstance().getBusinessId());
        result.setBusinessCode(flowTask.getFlowInstance().getBusinessCode());
        result.setCreateUser(flowTask.getFlowInstance().getCreatorName());
        result.setCreateTime(flowTask.getFlowInstance().getCreatedDate());
        result.setWorkAndAdditionRemark(flowTask.getFlowInstance().getBusinessModelRemark());
        result.setFlowName(flowTask.getFlowInstance().getFlowName());
        result.setBusinessModelName(flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getName());
        result.setFlowInstanceId(flowTask.getFlowInstance().getId());
        result.setLabelReason(flowTask.getLabelReason());

        //移动端配置的地址
        WorkPageUrl workPageUrl = flowTask.getWorkPageUrl();
        if (workPageUrl != null) {
            result.setPhoneUrl(StringUtils.isNotEmpty(workPageUrl.getPhoneUrl()) ? workPageUrl.getPhoneUrl() : "NotConfig");
        }


        FlowDefVersion flowDefVersion = flowTask.getFlowInstance().getFlowDefVersion();
        //判断是否是固化流程
        result.setSolidifyFlow(BooleanUtils.isTrue(flowDefVersion.getSolidifyFlow()));

        String defJson = flowTask.getTaskJsonDef();
        JSONObject defObj = JSONObject.fromObject(defJson);
        JSONObject normalInfo = defObj.getJSONObject("nodeConfig").getJSONObject("normal");
        String nodeType = (String) defObj.get("nodeType");

        result.setOpinionList(null);


        //节点是否配置了【处理后返回我审批】
        if (normalInfo.has("allowJumpBack")) {
            result.setAllowJumpBack(normalInfo.getBoolean("allowJumpBack"));
        } else {
            result.setAllowJumpBack(false);
        }

        //节点是否配置了【允许任意退回】
        if (normalInfo.has("allowReturn")) {
            result.setAllowReturn(normalInfo.getBoolean("allowReturn"));
        } else {
            result.setAllowReturn(false);
        }

        //如果节点配置了默认意见，就在请求头中返回
        if (normalInfo.has("defaultOpinion")) {
            result.setCurrentNodeDefaultOpinion(normalInfo.getString("defaultOpinion"));
        }

        //是否需要选择不同意原因
        if (normalInfo.has("chooseDisagreeReason") && normalInfo.getBoolean("chooseDisagreeReason")) {
            String flowTypeId = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getId();
            List<DisagreeReason> list = disagreeReasonService.getDisReasonListByTypeId(flowTypeId);
            result.setDisagreeReasonList(list);
        }

        //如果配置了抄送/呈报
        if (nodeType.equalsIgnoreCase("ParallelTask")
                && normalInfo.has("carbonCopyOrReport")
                && normalInfo.getBoolean("carbonCopyOrReport")) {
            try {
                //需要单独检查是否后一个节点是结束节点，如果不是，默认不生效
                ResponseData responseData = this.whetherNeedCarbonCopyOrReport(id);
                result.setCarbonCopyOrReport(responseData.successful());
            } catch (Exception e) {
                LogUtil.error("检查是否满足抄送和呈报配置需求报错：", e);
                result.setCarbonCopyOrReport(false);
            }
        }


        if (!StringUtils.isEmpty(preId)) {
            preFlowTask = flowHistoryDao.findOne(flowTask.getPreId());//上一个任务id
        }
        if (preFlowTask == null) {//如果没有上一步任务信息,默认上一步为开始节点
            result.setPrUser(flowTask.getFlowInstance().getCreatorAccount() + "[" + flowTask.getFlowInstance().getCreatorName() + "]");
            result.setPreCreateTime(flowTask.getFlowInstance().getCreatedDate());
            result.setPrOpinion("流程启动");
        } else {
            result.setPrUser(preFlowTask.getExecutorAccount() + "[" + preFlowTask.getExecutorName() + "]");
            result.setPreCreateTime(preFlowTask.getCreatedDate());
            result.setPrOpinion(preFlowTask.getDepict());
        }
        //处理界面用于判断按钮的参数（没有按钮可以不需要）
        result.setTrustState(flowTask.getTrustState());
        result.setCanReject(flowTask.getCanReject());
        result.setTaskJsonDef(flowTask.getTaskJsonDef());
        result.setActClaimTime(flowTask.getActClaimTime());
        result.setCanSuspension(flowTask.getCanSuspension());
        result.setExecutorId(flowTask.getExecutorId());
        result.setFlowInstanceCreatorId(flowTask.getFlowInstance().getCreatorId());

        if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
            result.setCarbonCopyOrReport(true); //虚拟任务默认成抄送呈报，只需要点进去自动访问flowTask/haveReadTaskByTaskId
            result.setPrUser("virtual[虚拟任务]");
            result.setPreCreateTime(flowTask.getCreatedDate());
            if (StringUtils.isNotEmpty(flowTask.getDepict())) {
                result.setPrOpinion(flowTask.getDepict());
            } else {
                result.setPrOpinion("虚拟任务通知");
            }
        }

        return result;
    }


    /**
     * 检查是否满足抄送/呈报要求
     * 因为只有并行任务，要求只有唯一分支，并且唯一分支连接结束节点
     */
    public ResponseData whetherNeedCarbonCopyOrReport(String flowTaskId) throws Exception {
        //可能路径
        List<NodeInfo> list = this.findNextNodes(flowTaskId);
        List<NodeInfo> nodeInfoList = null;
        if (!CollectionUtils.isEmpty(list)) {
            if (list.size() == 1) {
                nodeInfoList = this.findNexNodesWithUserSet(flowTaskId, null, null);
            } else {
                String gateWayName = list.get(0).getGateWayName();
                if (StringUtils.isNotEmpty(gateWayName) && "人工排他网关".equals(gateWayName)) {
                    //人工网关不处理
                    return ResponseData.operationFailure("10217");
                } else {
                    nodeInfoList = this.findNexNodesWithUserSet(flowTaskId, null, null);
                }
            }
        }

        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            if (nodeInfoList.size() == 1 && "EndEvent".equalsIgnoreCase(nodeInfoList.get(0).getType())) {//只存在结束节点
                return ResponseData.operationSuccessWithData("EndEvent");
            } else if (nodeInfoList.size() == 1 && "CounterSignNotEnd".equalsIgnoreCase(nodeInfoList.get(0).getType())) {
                return ResponseData.operationSuccessWithData("CounterSignNotEnd");
            } else {
                return ResponseData.operationFailure("10218");
            }
        } else {
            return ResponseData.operationFailure("10079");
        }
    }


    public List<NodeInfo> findNexNodesWithUserSet(String id) throws NoSuchMethodException {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return null;
        }
        return this.findNexNodesWithUserSet(flowTask, null, null);
    }

    public List<NodeInfo> findNexNodesWithUserSet(FlowTask flowTask) throws NoSuchMethodException {
        if (flowTask == null) {
            return null;
        }
        return this.findNexNodesWithUserSet(flowTask, null, null);
    }

    public List<NodeInfo> findNexNodesWithUserSetSolidifyFlow(FlowTask flowTask) throws NoSuchMethodException {
        if (flowTask == null) {
            return null;
        }
        return this.findNexNodesWithUserSetSolidifyFlow(flowTask, null, null);
    }

    public List<NodeInfo> findNexNodesWithUserSet(String id, String approved, List<String> includeNodeIds) throws NoSuchMethodException {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return null;
        }
        if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟待办不允许获取下一步信息
            return new ArrayList<>();
        }
        List<NodeInfo> result;
        List<NodeInfo> nodeInfoList = this.findNexNodesWithUserSet(flowTask, approved, includeNodeIds);
        result = nodeInfoList;
        FlowInstance parentFlowInstance = flowTask.getFlowInstance().getParent();
        FlowTask flowTaskTempSrc = new FlowTask();
        BeanUtils.copyProperties(flowTask, flowTaskTempSrc);

        while (parentFlowInstance != null && !CollectionUtils.isEmpty(nodeInfoList) && nodeInfoList.size() == 1 && "EndEvent".equalsIgnoreCase(nodeInfoList.get(0).getType())) {//针对子流程结束节点
            FlowTask flowTaskTemp = new FlowTask();
            BeanUtils.copyProperties(flowTaskTempSrc, flowTaskTemp);

            ProcessInstance instanceSon = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(flowTaskTemp.getFlowInstance().getActInstanceId())
                    .singleResult();
            flowTaskTemp.setFlowInstance(parentFlowInstance);
            // 取得流程实例
            String superExecutionId = instanceSon.getSuperExecutionId();
            HistoricActivityInstance historicActivityInstance = null;
            HistoricActivityInstanceQuery his = historyService.createHistoricActivityInstanceQuery()
                    .executionId(superExecutionId).activityType("callActivity").unfinished();
            if (his != null) {
                historicActivityInstance = his.singleResult();
                HistoricActivityInstanceEntity he = (HistoricActivityInstanceEntity) historicActivityInstance;
                flowTaskTemp.setActTaskKey(he.getActivityId());
                flowTaskTemp.setActTaskDefKey(he.getActivityId());
                String flowDefJson = parentFlowInstance.getFlowDefVersion().getDefJson();
                JSONObject defObj = JSONObject.fromObject(flowDefJson);
                Definition definition = (Definition) JSONObject.toBean(defObj, Definition.class);
                JSONObject currentNode = definition.getProcess().getNodes().getJSONObject(he.getActivityId());
                flowTaskTemp.setTaskJsonDef(currentNode.toString());
                result = this.findNexNodesWithUserSet(flowTaskTemp, approved, includeNodeIds);
                flowTaskTempSrc = flowTaskTemp;
            }
            parentFlowInstance = parentFlowInstance.getParent();
            nodeInfoList = result;
        }
        return result;
    }

    /**
     * 根据流程实例Id获取待办
     *
     * @param instanceId
     * @return
     */
    @Override
    public List<FlowTask> findByInstanceId(String instanceId) {
        return flowTaskDao.findByInstanceId(instanceId);
    }

    /**
     * 根据流程实例Id获取待办（不查虚拟待办）
     *
     * @param instanceId
     * @return
     */
    public List<FlowTask> findByInstanceIdNoVirtual(String instanceId) {
        return flowTaskDao.findByInstanceIdNoVirtual(instanceId);
    }


    public ResponseData listFlowTaskHeader() {
        try {
            List<TodoBusinessSummaryVO> list = this.findTaskSumHeader("");
            return ResponseData.operationSuccessWithData(list);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            return ResponseData.operationFailure("10118");
        }
    }

    /**
     * 查询当前用户待办业务单据汇总信息
     *
     * @param appSign 应用标识
     * @return 汇总信息
     */
    public List<TodoBusinessSummaryVO> findTaskSumHeader(String appSign) {
        return this.findCommonTaskSumHeader(false, appSign);
    }


    public ResponseData findTaskSumHeaderOfPhone() {
        List<TodoBusinessSummaryVO> list = this.findTaskSumHeader("");
        if (list == null) {
            list = new ArrayList<>();
        }
        return ResponseData.operationSuccessWithData(list);
    }


    public ResponseData findTaskSumHeaderCanBatchApprovalOfPhone() {
        List<TodoBusinessSummaryVO> result = this.findCommonTaskSumHeader(true, "");
        if (result == null) {
            result = new ArrayList<>();
        }
        return ResponseData.operationSuccessWithData(result);
    }


    public Integer getUserTodoSum(String userId) {
        //根据被授权人ID查看所有满足的转授权设置信息（共同查看模式）
        List<TaskMakeOverPower> list = taskMakeOverPowerService.findPowerByPowerUser(userId);
        int allTodoSum = 0;
        if (!CollectionUtils.isEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                TaskMakeOverPower bean = list.get(i);
                if (StringUtils.isNotEmpty(bean.getFlowTypeId())) { //流程类型
                    int typeSum = flowTaskDao.findTodoSumByExecutorIdAndFlowTypeId(bean.getUserId(), bean.getFlowTypeId());
                    allTodoSum += typeSum;
                }
            }
        }
        int exeListSum = flowTaskDao.findTodoSumByExecutorId(userId);
        allTodoSum += exeListSum;
        return allTodoSum;
    }


    /**
     * 查询当前用户待办业务单据汇总信息,只有批量审批
     *
     * @param appSign 应用标识
     * @return
     */
    public List<TodoBusinessSummaryVO> findCommonTaskSumHeader(Boolean batchApproval, String appSign) {
        List<TodoBusinessSummaryVO> voList = new ArrayList<>();
        String userID = ContextUtil.getUserId();
        List groupResultList = new ArrayList();

        List<TaskMakeOverPower> powerList = taskMakeOverPowerService.findPowerByPowerUser(userID);
        if (!CollectionUtils.isEmpty(powerList)) {
            if (batchApproval == true) {
                for (int i = 0; i < powerList.size(); i++) {
                    TaskMakeOverPower bean = powerList.get(i);
                    if (StringUtils.isNotEmpty(bean.getFlowTypeId())) { //流程类型
                        List typeList = flowTaskDao.findCanBatchApprovalByExecutorAndFlowTypeId(bean.getUserId(), bean.getFlowTypeId());
                        groupResultList.addAll(typeList);
                    }
                }
            } else {
                for (int i = 0; i < powerList.size(); i++) {
                    TaskMakeOverPower bean = powerList.get(i);
                    if (StringUtils.isNotEmpty(bean.getFlowTypeId())) { //流程类型
                        List typeList = flowTaskDao.findGroupByExecutorIdAndAndFlowTypeId(bean.getUserId(), bean.getFlowTypeId());
                        groupResultList.addAll(typeList);
                    }
                }
            }
        }

        if (batchApproval == true) {
            List list = flowTaskDao.findByExecutorIdGroupCanBatchApprovalOfPower(userID);
            groupResultList.addAll(list);
        } else {
            List list = flowTaskDao.findByExecutorIdGroupOfPower(userID);
            groupResultList.addAll(list);
        }

        Map<BusinessModel, Integer> businessModelCountMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(groupResultList)) {
            Iterator it = groupResultList.iterator();
            while (it.hasNext()) {
                Object[] res = (Object[]) it.next();
                int count = ((Number) res[0]).intValue();
                String flowDefinationId = res[1] + "";
                FlowDefination flowDefination = flowDefinationDao.findOne(flowDefinationId);
                if (flowDefination == null) {
                    continue;
                }
                // 获取业务类型
                BusinessModel businessModel = businessModelDao.findOne(flowDefination.getFlowType().getBusinessModel().getId());
                // 限制应用标识
                boolean canAdd = true;
                if (!StringUtils.isBlank(appSign)) {
                    // 判断应用模块代码是否以应用标识开头,不是就不添加
                    if (!businessModel.getAppModule().getCode().startsWith(appSign)) {
                        canAdd = false;
                    }
                }
                if (canAdd) {
                    Integer oldCount = businessModelCountMap.get(businessModel);
                    if (oldCount == null) {
                        oldCount = 0;
                    }
                    businessModelCountMap.put(businessModel, oldCount + count);
                }
            }
        }
        if (!CollectionUtils.isEmpty(businessModelCountMap)) {
            for (Map.Entry<BusinessModel, Integer> map : businessModelCountMap.entrySet()) {
                TodoBusinessSummaryVO todoBusinessSummaryVO = new TodoBusinessSummaryVO();
                todoBusinessSummaryVO.setBusinessModelCode(map.getKey().getClassName());
                todoBusinessSummaryVO.setBusinessModeId(map.getKey().getId());
                todoBusinessSummaryVO.setCount(map.getValue());
                todoBusinessSummaryVO.setAppRank(map.getKey().getAppModule().getRank());
                todoBusinessSummaryVO.setBusinessRank(map.getKey().getRank());
                // 在业务实体名称上不拼接待办数量
                todoBusinessSummaryVO.setBusinessModelName(map.getKey().getName());
                voList.add(todoBusinessSummaryVO);
            }
        }
        //先按照应用模块排序，再按照业务实体排序
        Comparator<TodoBusinessSummaryVO> byAppRank = Comparator.comparing(TodoBusinessSummaryVO::getAppRank);
        Comparator<TodoBusinessSummaryVO> byBusinessRank = Comparator.comparing(TodoBusinessSummaryVO::getBusinessRank);
        voList.sort(byAppRank.thenComparing(byBusinessRank));
        return voList;
    }


    public PageResult<FlowTask> findAllByTenant(String appModuleId, String businessModelId, String flowTypeId, Search searchConfig) {
        if (searchConfig != null) {
            boolean boo = SqlCommonUtil.isValid(searchConfig.getQuickSearchValue());
            if (!boo) {
                return null;
            }
        }
//        SessionUser sessionUser = ContextUtil.getSessionUser();
//        UserAuthorityPolicy authorityPolicy = sessionUser.getAuthorityPolicy();
//        if (!authorityPolicy.equals(UserAuthorityPolicy.TenantAdmin)) {
//            return null;
//        }
        PageResult<FlowTask> pageResult = flowTaskDao.findByPageByTenant(appModuleId, businessModelId, flowTypeId, searchConfig);
        List<FlowTask> result = pageResult.getRows();
        initFlowTasks(result);
        return pageResult;
    }


    public PageResult<FlowTask> findByPageCanBatchApproval(Search searchConfig) {
        return this.findByPageCanBatchApprovalByBusinessModelId(null, searchConfig);
    }

    public PageResult<FlowTaskBatchPhoneVO> findByPageCanBatchApprovalOfMobile(String businessModelId, int page, int rows, String quickValue) {
        Search search = new Search();
        search.addQuickSearchProperty("flowName");
        search.addQuickSearchProperty("taskName");
        search.addQuickSearchProperty("flowInstance.businessCode");
        search.addQuickSearchProperty("flowInstance.businessModelRemark");
        search.addQuickSearchProperty("creatorName");
        search.setQuickSearchValue(quickValue);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(page);
        pageInfo.setRows(rows);
        search.setPageInfo(pageInfo);

        SearchOrder searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.DESC);
        List<SearchOrder> list = new ArrayList<SearchOrder>();
        list.add(searchOrder);
        search.setSortOrders(list);

        PageResult<FlowTask> flowTaskPage = this.findByPageCanBatchApprovalByBusinessModelId(businessModelId, search);
        PageResult<FlowTaskBatchPhoneVO> phoneVoPage = new PageResult<FlowTaskBatchPhoneVO>();
        phoneVoPage.setPage(flowTaskPage.getPage());
        phoneVoPage.setRecords(flowTaskPage.getRecords());
        phoneVoPage.setTotal(flowTaskPage.getTotal());
        if (!CollectionUtils.isEmpty(flowTaskPage.getRows())) {
            List<FlowTask> taskList = flowTaskPage.getRows();
            List<FlowTaskBatchPhoneVO> phoneVoList = new ArrayList<FlowTaskBatchPhoneVO>();
            taskList.forEach(bean -> {
                FlowTaskBatchPhoneVO beanVo = new FlowTaskBatchPhoneVO();
                FlowType flowType = bean.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType();
                beanVo.setFlowInstanceBusinessCode(bean.getFlowInstance().getBusinessCode());
                beanVo.setTaskName(bean.getTaskName());
                beanVo.setFlowTypeName(flowType.getName());
                beanVo.setActClaimTime(bean.getActClaimTime());
                beanVo.setCreatedDate(bean.getCreatedDate());
                beanVo.setCanMobile(bean.getCanMobile());
                beanVo.setTaskId(bean.getId());
                beanVo.setFlowName(bean.getFlowName());
                beanVo.setFlowInstanceCreatorName(bean.getFlowInstance().getCreatorName());
                beanVo.setBusinessModelRemark(bean.getFlowInstance().getBusinessModelRemark());

                String taskJsonDef = bean.getTaskJsonDef();
                JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                String nodeType = taskJsonDefObj.get("nodeType") + "";
                beanVo.setNodeType(nodeType);

                String webBaseAddress = Constants.getConfigValueByWeb(flowType.getBusinessModel().getAppModule().getWebBaseAddress());
                if (StringUtils.isNotEmpty(webBaseAddress)) {
                    String[] tempWebBaseAddress = webBaseAddress.split("/");
                    if (tempWebBaseAddress != null && tempWebBaseAddress.length > 0) {
                        webBaseAddress = tempWebBaseAddress[tempWebBaseAddress.length - 1];
                        webBaseAddress = "/" + webBaseAddress + "/";
                    }
                }
                beanVo.setCompleteTaskUrl(webBaseAddress);
                phoneVoList.add(beanVo);
            });
            phoneVoPage.setRows(phoneVoList);
        } else {
            phoneVoPage.setRows(new ArrayList<FlowTaskBatchPhoneVO>());
        }

        return phoneVoPage;
    }


    public PageResult<FlowTask> findByPageCanBatchApprovalOfPhone(String businessModelId, String property, String direction, int page, int rows, String quickValue) {
        Search search = new Search();
        search.addQuickSearchProperty("flowName");
        search.addQuickSearchProperty("taskName");
        search.addQuickSearchProperty("flowInstance.businessCode");
        search.addQuickSearchProperty("flowInstance.businessModelRemark");
        search.addQuickSearchProperty("creatorName");
        search.setQuickSearchValue(quickValue);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(page);
        pageInfo.setRows(rows);
        search.setPageInfo(pageInfo);

        SearchOrder searchOrder;
        if (StringUtils.isNotEmpty(property) && StringUtils.isNotEmpty(direction)) {
            if (SearchOrder.Direction.ASC.equals(direction)) {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.ASC);
            } else {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.DESC);
            }
        } else {
            searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.DESC);
        }
        List<SearchOrder> list = new ArrayList<SearchOrder>();
        list.add(searchOrder);
        search.setSortOrders(list);

        return this.findByPageCanBatchApprovalByBusinessModelId(businessModelId, search);
    }

    public PageResult<FlowTask> findByPageCanBatchApprovalByBusinessModelId(String businessModelId, Search searchConfig) {
        if (searchConfig != null) {
            boolean boo = SqlCommonUtil.isValid(searchConfig.getQuickSearchValue());
            if (!boo) {
                return null;
            }
        }

        String userId = ContextUtil.getUserId();

        //根据被授权人ID查看所有满足的转授权设置信息（共同查看模式）
        List<TaskMakeOverPower> powerList = taskMakeOverPowerService.findPowerByPowerUser(userId);
        PageResult<FlowTask> flowTaskPageResult = flowTaskDao.findByPageCanBatchApprovalByBusinessModelIdOfPower(businessModelId, userId, powerList, searchConfig);

        //对待办信息进行特殊需求处理
        List<FlowTask> flowTaskList = flowTaskPageResult.getRows();
        for (FlowTask bean : flowTaskList) {
            //实时计算预警状态
            try {
                this.setWarningStatusByTask(bean);
            } catch (Exception e) {
                LogUtil.error("待办设置预警状态失败，taskId=" + bean.getId(), e);
            }
            //共同查看模式前台添加转授权备注
            if (!userId.equals(bean.getExecutorId())) {
                bean.getFlowInstance().setBusinessModelRemark("【" + bean.getExecutorName() + "-转授权】" + bean.getFlowInstance().getBusinessModelRemark());
            }
        }
        return flowTaskPageResult;
    }


    /**
     * 获取可批量审批待办信息(最新移动端专用)
     */
    public FlowTaskPageResultVO<FlowTaskPhoneVo> findByBusinessModelIdWithAllCountOfMobile(String businessModelId, String property, String direction, int page, int rows, String quickValue) {
        Search search = new Search();
        search.addQuickSearchProperty("flowName");
        search.addQuickSearchProperty("taskName");
        search.addQuickSearchProperty("flowInstance.businessCode");
        search.addQuickSearchProperty("flowInstance.businessModelRemark");
        search.addQuickSearchProperty("creatorName");
        search.setQuickSearchValue(quickValue);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(page);
        pageInfo.setRows(rows);
        search.setPageInfo(pageInfo);

        SearchOrder searchOrder;
        if (StringUtils.isNotEmpty(property) && StringUtils.isNotEmpty(direction)) {
            if ("ASC".equalsIgnoreCase(direction)) {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.ASC);
            } else {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.DESC);
            }
        } else {
            searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.DESC);
        }
        List<SearchOrder> list = new ArrayList<SearchOrder>();
        list.add(searchOrder);
        search.setSortOrders(list);

        FlowTaskPageResultVO<FlowTask> flowTaskPage = findByBusinessModelIdWithAllCount(businessModelId, search);
        FlowTaskPageResultVO<FlowTaskPhoneVo> phoneVoPage = new FlowTaskPageResultVO<>();
        phoneVoPage.setAllTotal(flowTaskPage.getAllTotal());
        phoneVoPage.setPage(flowTaskPage.getPage());
        phoneVoPage.setRecords(flowTaskPage.getRecords());
        phoneVoPage.setTotal(flowTaskPage.getTotal());
        if (flowTaskPage.getAllTotal() != 0 && !CollectionUtils.isEmpty(flowTaskPage.getRows())) {
            List<FlowTask> taskList = flowTaskPage.getRows();
            List<FlowTaskPhoneVo> phoneVoList = new ArrayList<>();
            taskList.forEach(bean -> {
                FlowTaskPhoneVo beanVo = new FlowTaskPhoneVo();
                FlowType flowType = bean.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType();
                beanVo.setFlowInstanceBusinessCode(bean.getFlowInstance().getBusinessCode());
                beanVo.setFlowInstanceFlowName(bean.getFlowInstance().getFlowName());
                beanVo.setTaskName(bean.getTaskName());
                beanVo.setFlowTypeId(flowType.getId());
                beanVo.setFlowTypeName(flowType.getName());
                beanVo.setActClaimTime(bean.getActClaimTime());
                beanVo.setCreatedDate(bean.getCreatedDate());
                beanVo.setBusinessModelClassName(flowType.getBusinessModel().getClassName());
                beanVo.setFlowInstanceBusinessId(bean.getFlowInstance().getBusinessId());
                beanVo.setCanReject(bean.getCanReject());
                beanVo.setFlowInstanceId(bean.getFlowInstance().getId());
                beanVo.setCanSuspension(bean.getCanSuspension());
                beanVo.setCanMobile(bean.getCanMobile());
                beanVo.setTaskId(bean.getId());
                beanVo.setTrustState(bean.getTrustState());
                beanVo.setFlowInstanceCreatorName(bean.getFlowInstance().getCreatorName());
                beanVo.setBusinessModelRemark(bean.getFlowInstance().getBusinessModelRemark());

                String taskJsonDef = bean.getTaskJsonDef();
                JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                String nodeType = taskJsonDefObj.get("nodeType") + "";
                beanVo.setNodeType(nodeType);

                String apiBaseAddress = Constants.getConfigValueByApi(flowType.getBusinessModel().getAppModule().getApiBaseAddress());
                beanVo.setApiBaseAddress(apiBaseAddress);
                beanVo.setBusinessDetailServiceUrl(bean.getBusinessDetailServiceUrl());

                String webBaseAddressConfig = flowType.getBusinessModel().getAppModule().getWebBaseAddress();
                String webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                WorkPageUrl workPageUrl = bean.getWorkPageUrl();
                if (workPageUrl != null) {
                    beanVo.setTaskFormUrl(PageUrlUtil.buildUrl(webBaseAddress, workPageUrl.getUrl()));
                    String appModuleId = workPageUrl.getAppModuleId();
                    AppModule appModule = appModuleDao.findOne(appModuleId);
                    if (appModule != null && !appModule.getId().equals(bean.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getId())) {
                        webBaseAddressConfig = appModule.getWebBaseAddress();
                        webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                        beanVo.setTaskFormUrl(PageUrlUtil.buildUrl(webBaseAddress, workPageUrl.getUrl()));
                    }
                }

                beanVo.setCompleteTaskUrl(PageUrlUtil.buildUrl(webBaseAddress, flowType.getBusinessModel().getCompleteTaskServiceUrl()));

                phoneVoList.add(beanVo);
            });
            phoneVoPage.setRows(phoneVoList);
        } else {
            phoneVoPage.setRows(new ArrayList<FlowTaskPhoneVo>());
        }

        return phoneVoPage;
    }

    public FlowTaskPageResultVO<FlowTask> findByBusinessModelIdWithAllCountOfPhone(String businessModelId, String property,
                                                                                   String direction, int page, int rows, String quickValue) {
        Search search = new Search();
        search.addQuickSearchProperty("flowName");
        search.addQuickSearchProperty("taskName");
        search.addQuickSearchProperty("flowInstance.businessCode");
        search.addQuickSearchProperty("flowInstance.businessModelRemark");
        search.addQuickSearchProperty("creatorName");
        search.setQuickSearchValue(quickValue);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(page);
        pageInfo.setRows(rows);
        search.setPageInfo(pageInfo);

        SearchOrder searchOrder;
        if (StringUtils.isNotEmpty(property) && StringUtils.isNotEmpty(direction)) {
            if (SearchOrder.Direction.ASC.equals(direction)) {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.ASC);
            } else {
                searchOrder = new SearchOrder(property, SearchOrder.Direction.DESC);
            }
        } else {
            searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.ASC);
        }
        List<SearchOrder> list = new ArrayList<SearchOrder>();
        list.add(searchOrder);
        search.setSortOrders(list);

        return findByBusinessModelIdWithAllCount(businessModelId, search);
    }

    @Override
    public ResponseData listFlowTaskWithAllCount(Search search, String modelId) {
        if (search != null) {
            boolean boo = SqlCommonUtil.isValid(search.getQuickSearchValue());
            if (!boo) {
                return ResponseData.operationFailure("获取待办失败：未通过sql注入检测！");
            }
        }
        try {
            FlowTaskPageResultVO<FlowTask> resVo = this.findByBusinessModelIdWithAllCount(modelId, search);
            return ResponseData.operationSuccessWithData(resVo);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            return ResponseData.operationFailure("10118");
        }
    }

    @Override
    public ResponseData listFlowTaskByUserId(Search search, String userId) {
        if (StringUtils.isEmpty(userId) || "anonymous".equalsIgnoreCase(userId)) {
            throw new FlowException(ContextUtil.getMessage("10219"));
        }
        if (search != null) {
            boolean boo = SqlCommonUtil.isValid(search.getQuickSearchValue());
            if (!boo) {
                return ResponseData.operationFailure("10220");
            }
        }

        FlowTaskPageResultVO<FlowTask> resultVO = new FlowTaskPageResultVO<>();

        //根据被授权人ID查看所有满足的转授权设置信息（共同查看模式）
        List<TaskMakeOverPower> powerList = taskMakeOverPowerService.findPowerByPowerUser(userId);
        if (search == null) {
            search = new Search();
            PageInfo pageInfo = new PageInfo();
            pageInfo.setPage(1);
            pageInfo.setRows(1000);
            search.setPageInfo(pageInfo);
        }
        PageResult<FlowTask> pageResult = flowTaskDao.findByPageByBusinessModelIdOfPower("", userId, powerList, search);

        //说明添加授权人信息
        List<FlowTask> flowTaskList = pageResult.getRows();

        initFlowTasks(flowTaskList);

        flowTaskList.forEach(a -> {
            if (!userId.equals(a.getExecutorId())) {
                a.getFlowInstance().setBusinessModelRemark("【" + a.getExecutorName() + "-转授权】" + a.getFlowInstance().getBusinessModelRemark());
            }
        });
        resultVO.setRows(flowTaskList);
        resultVO.setRecords(pageResult.getRecords());
        resultVO.setPage(pageResult.getPage());
        resultVO.setTotal(pageResult.getTotal());
        resultVO.setAllTotal(pageResult.getRecords());
        return ResponseData.operationSuccessWithData(resultVO);
    }


    public FlowTaskPageResultVO<FlowTask> findByBusinessModelIdWithAllCount(String businessModelId, Search searchConfig) {
        String userId = ContextUtil.getUserId();
        FlowTaskPageResultVO<FlowTask> resultVO = new FlowTaskPageResultVO<>();
        if (StringUtils.isEmpty(userId) || "anonymous".equalsIgnoreCase(userId)) {
            throw new FlowException(ContextUtil.getMessage("10219"));
        }

        if (searchConfig != null) {
            boolean boo = SqlCommonUtil.isValid(searchConfig.getQuickSearchValue());
            if (!boo) {
                return null;
            }
        }

        //根据被授权人ID查看所有满足的转授权设置信息（共同查看模式）
        List<TaskMakeOverPower> powerList = taskMakeOverPowerService.findPowerByPowerUser(userId);
        PageResult<FlowTask> pageResult = flowTaskDao.findByPageByBusinessModelIdOfPower(businessModelId, userId, powerList, searchConfig);

        //对待办信息进行特殊需求处理
        List<FlowTask> flowTaskList = pageResult.getRows();
        for (FlowTask bean : flowTaskList) {
            //实时计算预警状态
            try {
                this.setWarningStatusByTask(bean);
            } catch (Exception e) {
                LogUtil.error("待办设置预警状态失败，taskId=" + bean.getId(), e);
            }
            //共同查看模式前台添加转授权备注
            if (!userId.equals(bean.getExecutorId())) {
                bean.getFlowInstance().setBusinessModelRemark("【" + bean.getExecutorName() + "-转授权】" + bean.getFlowInstance().getBusinessModelRemark());
            }

            WorkPageUrl workPageUrl = bean.getWorkPageUrl();
            if (workPageUrl != null) {
                bean.setPhoneUrl(StringUtils.isNotEmpty(workPageUrl.getPhoneUrl()) ? workPageUrl.getPhoneUrl() : "NotConfig");
            }

        }
        //完成待办任务的URL设置
        initFlowTasks(flowTaskList);
        resultVO.setRows(flowTaskList);
        resultVO.setRecords(pageResult.getRecords());
        resultVO.setPage(pageResult.getPage());
        resultVO.setTotal(pageResult.getTotal());
        resultVO.setAllTotal(pageResult.getRecords());
        return resultVO;
    }


    /**
     * 为待办设置预警状态
     *
     * @param flowTask
     */
    public void setWarningStatusByTask(FlowTask flowTask) {
        //任务的额定工时（可能带两位小数的小时）
        Double timing = flowTask.getTiming();
        //任务的额定工时（分钟）
        Integer timingMinute;
        //如果未设置额定工时，表示该任务时间不做考核
        if (timing == null || timing < 0.000001) {
            flowTask.setWarningStatus(EarlyWarningStatus.NORMAL.getCode());
            return;
        } else {
            Double timingMin = timing * 60;
            timingMinute = timingMin.intValue();
        }
        //任务到达时间
        Date createDate = flowTask.getCreatedDate();

        //当前时间
        Calendar nowCalendar = Calendar.getInstance();

        //任务到达时间+额定工时
        Calendar createAddTiming = Calendar.getInstance();
        createAddTiming.setTime(createDate);
        createAddTiming.add(Calendar.MINUTE, timingMinute);

        //当前时间>=任务到达时间+额定工时  (超时)
        if (!nowCalendar.before(createAddTiming)) {
            flowTask.setWarningStatus(EarlyWarningStatus.TIMEOUT.getCode());
            return;
        }
        //流程定义中设置的提前预警时间（小时）
        Integer earlyWarningTime = Optional.ofNullable(flowTask).map(FlowTask::getFlowInstance).map(FlowInstance::getFlowDefVersion).map(FlowDefVersion::getEarlyWarningTime).orElse(0);
        //任务到达时间+额定工时-提前预警时间
        createAddTiming.add(Calendar.HOUR, -earlyWarningTime);

        //任务到达时间+额定工时-提前预警时间=<当前时间  (预警)
        if (!nowCalendar.before(createAddTiming)) {
            flowTask.setWarningStatus(EarlyWarningStatus.WARNING.getCode());
        } else {
            //当前时间<任务到达时间+额定工时-提前预警时间 (正常)
            flowTask.setWarningStatus(EarlyWarningStatus.NORMAL.getCode());
        }
    }


    public List<BatchApprovalFlowTaskGroupVO> getBatchApprovalFlowTasks(List<String> taskIdArray) throws NoSuchMethodException {
        List<BatchApprovalFlowTaskGroupVO> result = new ArrayList<>();
        List<FlowTask> flowTaskList = this.findByIds(taskIdArray);
        if (!CollectionUtils.isEmpty(flowTaskList)) {
            for (FlowTask flowTask : flowTaskList) {
                List<NodeInfo> nodeInfoList = this.findNexNodesWithUserSet(flowTask, "true", null);
                BatchApprovalFlowTaskGroupVO batchApprovalFlowTaskGroupVO = new BatchApprovalFlowTaskGroupVO();
                String key = flowTask.getActTaskDefKey() + "@" + flowTask.getFlowInstance().getFlowDefVersion().getVersionCode() + "@" + flowTask.getFlowDefinitionId();
                batchApprovalFlowTaskGroupVO.setKey(key);
                int index = result.indexOf(batchApprovalFlowTaskGroupVO);
                if (index > -1) {
                    batchApprovalFlowTaskGroupVO = result.get(index);
                } else {
                    result.add(batchApprovalFlowTaskGroupVO);
                }
                Map<FlowTask, List<NodeInfo>> flowTaskNextNodesInfoMap = batchApprovalFlowTaskGroupVO.getFlowTaskNextNodesInfo();
                flowTaskNextNodesInfoMap.put(flowTask, nodeInfoList);
            }
        }
        return result;
    }

    public OperateResultWithData<FlowStatus> completeBatchApproval(List<FlowTaskCompleteVO> flowTaskCompleteVOList) throws Exception {
        for (FlowTaskCompleteVO flowTaskCompleteVO : flowTaskCompleteVOList) {
            OperateResultWithData<FlowStatus> tempResult = this.complete(flowTaskCompleteVO);
            if (!tempResult.successful()) {
                throw new FlowException(ContextUtil.getMessage("10221"));
            }
        }
        return OperateResultWithData.operationSuccess("10017");
    }

    public OperateResultWithData getSelectedNodesInfo(String taskId, String approved, String includeNodeIdsStr, Boolean solidifyFlow) throws NoSuchMethodException {
        List<String> includeNodeIds = null;
        if (StringUtils.isNotEmpty(includeNodeIdsStr)) {
            String[] includeNodeIdsStringArray = includeNodeIdsStr.split(",");
            includeNodeIds = Arrays.asList(includeNodeIdsStringArray);
        }
        if (StringUtils.isEmpty(approved)) {
            approved = "true";
        }
        List<NodeInfo> nodeInfoList;

        try {
            nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, includeNodeIds);
        } catch (Exception e) {
            LogUtil.error("获取下一节点信息错误：{}", e.getMessage(), e);
            return OperateResultWithData.operationFailure("10222", e.getMessage());
        }

        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            if (nodeInfoList.size() == 1 && "EndEvent".equalsIgnoreCase(nodeInfoList.get(0).getType())) {//只存在结束节点
                //非固化流程配置了结束抄送
                if (!solidifyFlow && (nodeInfoList.get(0).getExecutorSet() != null || "AnyOne".equals(nodeInfoList.get(0).getUiUserType()))) {
                    return OperateResultWithData.operationSuccessWithData(nodeInfoList);
                }
                return OperateResultWithData.operationSuccessWithData("EndEvent");
            } else if (nodeInfoList.size() == 1 && "CounterSignNotEnd".equalsIgnoreCase(nodeInfoList.get(0).getType())) {
                return OperateResultWithData.operationSuccessWithData("CounterSignNotEnd");
            } else {
                if (BooleanUtils.isTrue(solidifyFlow)) { //表示为固化流程
                    FlowTask flowTask = flowTaskDao.findOne(taskId);
                    //设置固化执行人信息(只是前台展示使用)
                    nodeInfoList = flowSolidifyExecutorService.
                            setNodeExecutorByBusinessId(nodeInfoList, flowTask.getFlowInstance().getBusinessId());
                }
                return OperateResultWithData.operationSuccessWithData(nodeInfoList);
            }
        } else if (nodeInfoList == null) {
            return OperateResultWithData.operationFailure("10033");
        } else {
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟待办
                return OperateResultWithData.operationFailure("10223");
            }
            return OperateResultWithData.operationFailure("10079");
        }
    }


    public List<NodeInfo> findNexNodesWithUserSetCanBatch(String taskIds) throws NoSuchMethodException {
        List<NodeInfo> all = new ArrayList<>();
        List<String> taskIdList = null;
        if (StringUtils.isNotEmpty(taskIds)) {
            String[] includeNodeIdsStringArray = taskIds.split(",");
            taskIdList = Arrays.asList(includeNodeIdsStringArray);
        }
        if (!CollectionUtils.isEmpty(taskIdList)) {
            String approved = "true";
            for (String taskId : taskIdList) {
                List<NodeInfo> nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
                if (!CollectionUtils.isEmpty(nodeInfoList)) {
                    all.addAll(nodeInfoList);
                }
            }
        }
        return all;
    }

    public Map<String, List<NodeGroupInfo>> findNexNodesGroupWithUserSetCanBatch(String taskIds) throws Exception {
        List<String> taskIdList = null;
        if (StringUtils.isNotEmpty(taskIds)) {
            String[] includeNodeIdsStringArray = taskIds.split(",");
            taskIdList = Arrays.asList(includeNodeIdsStringArray);
        }
        Map<String, List<NodeGroupInfo>> tempNodeGroupInfoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(taskIdList)) {
            String approved = "true";
            for (String taskId : taskIdList) {
                List<NodeInfo> nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
                if (!CollectionUtils.isEmpty(nodeInfoList)) {
                    if (nodeInfoList.size() == 1) {
                        NodeInfo nodeInfo = nodeInfoList.get(0);
                        String flowDefVersionId = nodeInfo.getFlowDefVersionId();
                        List<Executor> executorSet = nodeInfo.getExecutorSet();
                        List<NodeGroupInfo> tempNodeGroupInfoList;
                        String mapKey;
                        if (!CollectionUtils.isEmpty(executorSet)) {
                            mapKey = flowDefVersionId + nodeInfo.getId() + executorSet.size() + executorSet.iterator().next().getId();
                            tempNodeGroupInfoList = tempNodeGroupInfoMap.get(mapKey);
                        } else {
                            mapKey = flowDefVersionId + nodeInfo.getId();
                            tempNodeGroupInfoList = tempNodeGroupInfoMap.get(mapKey);
                        }
                        if (CollectionUtils.isEmpty(tempNodeGroupInfoList)) {
                            NodeGroupInfo tempNodeGroupInfo = new NodeGroupInfo();
                            tempNodeGroupInfo.setFlowDefVersionId(flowDefVersionId);
                            tempNodeGroupInfo.setNodeId(nodeInfo.getId());
                            tempNodeGroupInfo.setFlowDefVersionName(nodeInfo.getFlowDefVersionName());
                            BeanUtils.copyProperties(nodeInfo, tempNodeGroupInfo);
                            tempNodeGroupInfo.getIds().add(nodeInfo.getFlowTaskId());
                            tempNodeGroupInfo.setExecutorSet(nodeInfo.getExecutorSet());
                            tempNodeGroupInfoList = new ArrayList<>();
                            tempNodeGroupInfoList.add(tempNodeGroupInfo);
                            tempNodeGroupInfoMap.put(mapKey, tempNodeGroupInfoList);
                        } else {
                            for (NodeGroupInfo tempNodeGroupInfo : tempNodeGroupInfoList) {
                                tempNodeGroupInfo.getIds().add(nodeInfo.getFlowTaskId());
                            }
                        }
                    } else {
                        String mapKey = null;
                        List<NodeGroupInfo> nodes = new ArrayList<>();
                        for (NodeInfo nodeInfo : nodeInfoList) {
                            String flowDefVersionId = nodeInfo.getFlowDefVersionId();
                            if (StringUtils.isEmpty(mapKey)) {
                                mapKey = flowDefVersionId;
                            }
                            List<Executor> executorSet = nodeInfo.getExecutorSet();
                            if (!CollectionUtils.isEmpty(executorSet)) {
                                mapKey += nodeInfo.getId() + executorSet.size() + executorSet.iterator().next().getId();
                            } else {
                                mapKey += nodeInfo.getId();
                            }
                            NodeGroupInfo tempNodeGroupInfo = new NodeGroupInfo();
                            tempNodeGroupInfo.setFlowDefVersionId(flowDefVersionId);
                            tempNodeGroupInfo.setNodeId(nodeInfo.getId());
                            tempNodeGroupInfo.setFlowDefVersionName(nodeInfo.getFlowDefVersionName());
                            BeanUtils.copyProperties(nodeInfo, tempNodeGroupInfo);
                            tempNodeGroupInfo.getIds().add(nodeInfo.getFlowTaskId());
                            tempNodeGroupInfo.setExecutorSet(nodeInfo.getExecutorSet());
                            nodes.add(tempNodeGroupInfo);
                        }
                        List<NodeGroupInfo> tempNodeGroupInfoList = tempNodeGroupInfoMap.get(mapKey);
                        if (CollectionUtils.isEmpty(tempNodeGroupInfoList)) {
                            tempNodeGroupInfoMap.put(mapKey, nodes);
                        } else {
                            for (NodeGroupInfo tempNodeGroupInfo : tempNodeGroupInfoList) {
                                tempNodeGroupInfo.getIds().add(nodeInfoList.get(0).getFlowTaskId());
                            }
                        }
                    }
                }
            }
        }
        return tempNodeGroupInfoMap;
    }

    public ResponseData getSelectedCanBatchNodesInfoOfPhone(String taskIds) throws Exception {
        List<NodeGroupByFlowVersionInfo> nodeInfoList = this.findNexNodesGroupByVersionWithUserSetCanBatch(taskIds);
        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            return ResponseData.operationSuccessWithData(nodeInfoList);
        } else {
            return ResponseData.operationFailure("10224");
        }
    }

    @Override
    public ResponseData<List<NodeGroupByFlowVersionInfo>> getBatchNextNodes(List<String> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return ResponseData.operationFailure("10006");
        }
        String requestStr = "";
        for (String taskId : taskIds) {
            if (StringUtils.isEmpty(requestStr)) {
                requestStr += taskId;
            } else {
                requestStr += "," + taskId;
            }
        }
        try {
            List<NodeGroupByFlowVersionInfo> list = this.findNexNodesGroupByVersionWithUserSetCanBatch(requestStr);
            return ResponseData.operationSuccessWithData(list);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            return ResponseData.operationFailure(e.getMessage());
        }
    }

    public List<NodeGroupByFlowVersionInfo> findNexNodesGroupByVersionWithUserSetCanBatch(String taskIds) throws Exception {
        List<NodeGroupByFlowVersionInfo> all = new ArrayList<>();
        Map<String, List<NodeGroupInfo>> nodeGroupInfoMap = this.findNexNodesGroupWithUserSetCanBatch(taskIds);
        Map<String, NodeGroupByFlowVersionInfo> nodeGroupByFlowVersionInfoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(nodeGroupInfoMap)) {
            int i = 0;
            for (String key : nodeGroupInfoMap.keySet()) {
                List<NodeGroupInfo> nodeGroupInfoList = nodeGroupInfoMap.get(key);
                i++;
                String flowDefVersionId = nodeGroupInfoList.get(0).getFlowDefVersionId();
                NodeGroupByFlowVersionInfo nodeGroupByFlowVersionInfo = new NodeGroupByFlowVersionInfo();
                nodeGroupByFlowVersionInfo.setId(flowDefVersionId + "_" + i);
                nodeGroupByFlowVersionInfo.setName(nodeGroupInfoList.get(0).getFlowDefVersionName());
                FlowDefVersion flowDefVersion = flowDefVersionDao.findOne(flowDefVersionId);
                if (flowDefVersion != null) {
                    Boolean boo = flowDefVersion.getSolidifyFlow() == null ? false : flowDefVersion.getSolidifyFlow();
                    nodeGroupByFlowVersionInfo.setSolidifyFlow(boo);
                }
                if (nodeGroupByFlowVersionInfo.getSolidifyFlow()) {
                    for (NodeGroupInfo nodeGroupInfo : nodeGroupInfoList) {
                        nodeGroupInfo.setExecutorSet(null);
                    }
                }
                nodeGroupByFlowVersionInfo.setNodeGroupInfos(nodeGroupInfoList);
                nodeGroupByFlowVersionInfoMap.put(flowDefVersionId + "_" + i, nodeGroupByFlowVersionInfo);
            }
            all.addAll(nodeGroupByFlowVersionInfoMap.values());
        }
        return all;
    }


    /**
     * 批量处理（逻辑重新整理，只适合于react版本的批量处理）
     *
     * @param flowTaskBatchCompleteWebVOList 任务传输对象
     * @return
     */
    public ResponseData completeTaskBatch(List<FlowTaskBatchCompleteWebVO> flowTaskBatchCompleteWebVOList) {
        if (!CollectionUtils.isEmpty(flowTaskBatchCompleteWebVOList)) {
            int total = 0;//记录处理任务总数
            for (FlowTaskBatchCompleteWebVO flowTaskBatchCompleteWebVO : flowTaskBatchCompleteWebVOList) {
                List<String> taskIdList = flowTaskBatchCompleteWebVO.getTaskIdList();
                if (!CollectionUtils.isEmpty(taskIdList)) {
                    for (String taskId : taskIdList) {
                        CompleteTaskVo completeTaskVo = new CompleteTaskVo();
                        completeTaskVo.setTaskId(taskId);
                        FlowTask flowTask = flowTaskDao.findOne(taskId);
                        if (flowTask != null && flowTask.getFlowInstance() != null) {
                            completeTaskVo.setBusinessId(flowTask.getFlowInstance().getBusinessId());
                        } else {
                            continue;
                        }
                        completeTaskVo.setOpinion("同意(批量)");
                        List<FlowTaskCompleteWebVO> flowTaskCompleteWebVOList = flowTaskBatchCompleteWebVO.getFlowTaskCompleteList();
                        Boolean endEventId = null;
                        if (!CollectionUtils.isEmpty(flowTaskCompleteWebVOList)) {
                            for (FlowTaskCompleteWebVO f : flowTaskCompleteWebVOList) {
                                if (endEventId == null && f.getFlowTaskType() == null && f.getNodeId().indexOf("EndEvent") != -1) {
                                    endEventId = true;
                                }
                                f.setSolidifyFlow(flowTaskBatchCompleteWebVO.getSolidifyFlow());
                            }
                        }
                        completeTaskVo.setTaskList(JsonUtils.toJson(flowTaskCompleteWebVOList));
                        if (endEventId != null) {
                            completeTaskVo.setEndEventId("true");
                            completeTaskVo.setTaskList(null);
                        }
                        completeTaskVo.setManualSelected(false);  //审批和会签都不能连接人工排他网关
                        completeTaskVo.setApproved("true");
                        completeTaskVo.setLoadOverTime(new Long(7777));
                        try {
                            defaultFlowBaseService.completeTask(completeTaskVo);
                            total++;
                        } catch (Exception e) {
                            LogUtil.error(e.getMessage(), e);
                        }
                    }
                }
            }
            if (total > 0) {
                return ResponseData.operationSuccess("10225", total);
            } else {
                return ResponseData.operationFailure("10226");
            }
        } else {
            return ResponseData.operationFailure("10227");
        }
    }


    public ResponseData completeTaskBatchOfPhone(String flowTaskBatchCompleteWebVoStrs) {
        List<FlowTaskBatchCompleteWebVO> flowTaskBatchCompleteWebVOList = null;
        if (StringUtils.isNotEmpty(flowTaskBatchCompleteWebVoStrs)) {
            JSONArray jsonArray = JSONArray.fromObject(flowTaskBatchCompleteWebVoStrs);//把String转换为json
            if (!CollectionUtils.isEmpty(jsonArray)) {
                flowTaskBatchCompleteWebVOList = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    FlowTaskBatchCompleteWebVO flowTaskBatchCompleteWebVO = new FlowTaskBatchCompleteWebVO();
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    JSONArray taskIdListJsonArray = (JSONArray) jsonObject.get("taskIdList");
                    JSONArray flowTaskCompleteListJsonArray = (JSONArray) jsonObject.get("flowTaskCompleteList");
                    List<FlowTaskCompleteWebVO> flowTaskCompleteWebVOList = (List<FlowTaskCompleteWebVO>) JSONArray.toCollection(flowTaskCompleteListJsonArray, FlowTaskCompleteWebVO.class);
                    flowTaskBatchCompleteWebVO.setFlowTaskCompleteList(flowTaskCompleteWebVOList);
                    List<String> taskIdList = (List<String>) JSONArray.toCollection(taskIdListJsonArray, String.class);
                    flowTaskBatchCompleteWebVO.setTaskIdList(taskIdList);
                    flowTaskBatchCompleteWebVOList.add(flowTaskBatchCompleteWebVO);
                }
            }
            return this.completeTaskBatch(flowTaskBatchCompleteWebVOList);
        } else {
            return ResponseData.operationFailure("10228");
        }
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResultWithData<Integer> completeBatch(FlowTaskBatchCompleteVO flowTaskBatchCompleteVO) {
        List<String> taskIdList = flowTaskBatchCompleteVO.getTaskIdList();
        int total = 0;
        OperateResultWithData result;
        if (!CollectionUtils.isEmpty(taskIdList)) {
            for (String taskId : taskIdList) {
                FlowTaskCompleteVO flowTaskCompleteVO = new FlowTaskCompleteVO();
                BeanUtils.copyProperties(flowTaskBatchCompleteVO, flowTaskCompleteVO);
                flowTaskCompleteVO.setTaskId(taskId);
                try {
                    this.complete(flowTaskCompleteVO);
                    total++;
                } catch (Exception e) {
                    LogUtil.error(e.getMessage(), e);
                }
            }
            if (total > 0) {
                result = OperateResultWithData.operationSuccess();
                result.setData(total);
            } else {
                result = OperateResultWithData.operationFailure("10034");
            }
        } else {
            result = OperateResultWithData.operationFailure("10034");
        }
        return result;
    }


    @Override
    public ResponseData taskTurnToEmployee(TaskTurnInfoVo taskTurnInfoVo) {

        if (taskTurnInfoVo == null) {
            return OperateResult.operationFailure("10435");
        }
        String taskId = taskTurnInfoVo.getTaskId();
        if (StringUtils.isEmpty(taskId)) {
            return OperateResult.operationFailure("10436");
        }
        String userId = taskTurnInfoVo.getUserId();
        if (StringUtils.isEmpty(userId)) {
            return OperateResult.operationFailure("10437");
        }
        return taskTurnToDo(taskId, userId, taskTurnInfoVo.getOpinion());
    }


    @Override
    public OperateResult taskTurnToDo(String taskId, String userId) {
        return taskTurnToDo(taskId, userId, "");
    }

    public OperateResult taskTurnToDo(String taskId, String userId, String opinion) {
        if (StringUtils.isEmpty(opinion) || "null".equals(opinion)) {
            opinion = "";
        }
        Boolean setValue = redisTemplate.opsForValue().setIfAbsent("taskTurn_" + taskId, taskId);
        if (!setValue) {
            Long remainingTime = redisTemplate.getExpire("taskTurn_" + taskId, TimeUnit.SECONDS);
            if (remainingTime == -1) {  //说明时间未设置进去
                redisTemplate.expire("taskTurn_" + taskId, 2 * 60, TimeUnit.SECONDS);
                remainingTime = 120L;
            }
            throw new FlowException(ContextUtil.getMessage("10229", remainingTime));
        }

        try {
            //如果设置成功，redis检查设置2分钟过期
            redisTemplate.expire("taskTurn_" + taskId, 2 * 60, TimeUnit.SECONDS);

            SessionUser sessionUser = ContextUtil.getSessionUser();
            OperateResult result;
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (flowTask != null) {
                if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
                    return OperateResult.operationFailure("10230");
                }
                Integer trustState = flowTask.getTrustState();
                if (trustState != null) {
                    if (trustState == 1) {//发起委托的任务
                        return OperateResult.operationFailure("10199");
                    } else if (trustState == 2) { //委托的任务
                        return OperateResult.operationFailure("10200", "进行转办");
                    }
                }

                String defJson = flowTask.getTaskJsonDef();
                JSONObject defObj = JSONObject.fromObject(defJson);
                if (defObj.getString("nodeType").equalsIgnoreCase("PoolTask")) {
                    return OperateResult.operationFailure("10425");
                }
                //单签任务，先进行签收
                if (defObj.getString("nodeType").equalsIgnoreCase("SingleSign")) {
                    OperateResult resultData = this.claim(taskId, ContextUtil.getUserId());
                    if (!resultData.successful()) {
                        return resultData;
                    }
                }
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult(); // 创建历史任务实例查询
                //根据用户的id获取执行人
                Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                if (executor != null) {
                    FlowTask newFlowTask = new FlowTask();
                    BeanUtils.copyProperties(flowTask, newFlowTask);
                    FlowHistory flowHistory = flowTaskTool.initFlowHistory(flowTask, historicTaskInstance, true, null);//转办后先允许撤回
                    //如果是转授权转办模式（获取转授权记录信息）
                    String overPowerStr = taskMakeOverPowerService.getOverPowerStrByDepict(flowHistory.getDepict());
                    flowHistory.setDepict(overPowerStr + "【被转办给：“" + executor.getName() + "”】" + opinion);
                    flowHistory.setFlowExecuteStatus(FlowExecuteStatus.TURNTODO.getCode());//转办
                    newFlowTask.setId(null);
                    //判断待办转授权模式(如果是转办模式，需要返回转授权信息，其余情况返回null)
                    TaskMakeOverPower taskMakeOverPower = taskMakeOverPowerService.getMakeOverPowerByTypeAndUserId(executor.getId(), flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getId());
                    if (taskMakeOverPower != null) {
                        newFlowTask.setExecutorId(taskMakeOverPower.getPowerUserId());
                        newFlowTask.setExecutorAccount(taskMakeOverPower.getPowerUserAccount());
                        newFlowTask.setExecutorName(taskMakeOverPower.getPowerUserName());
                        newFlowTask.setDepict("【由：“" + sessionUser.getUserName() + "”转办】【转授权-" + executor.getName() + "授权】" + (StringUtils.isNotEmpty(flowTask.getDepict()) ? flowTask.getDepict() : ""));
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(taskMakeOverPower.getPowerUserOrgId());
                        newFlowTask.setExecutorOrgCode(taskMakeOverPower.getPowerUserOrgCode());
                        newFlowTask.setExecutorOrgName(taskMakeOverPower.getPowerUserOrgName());
                    } else {
                        newFlowTask.setExecutorId(executor.getId());
                        newFlowTask.setExecutorAccount(executor.getCode());
                        newFlowTask.setExecutorName(executor.getName());
                        newFlowTask.setDepict("【由：“" + sessionUser.getUserName() + "”转办】" + (StringUtils.isNotEmpty(flowTask.getDepict()) ? flowTask.getDepict() : ""));
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(executor.getOrganizationId());
                        newFlowTask.setExecutorOrgCode(executor.getOrganizationCode());
                        newFlowTask.setExecutorOrgName(executor.getOrganizationName());
                    }
                    newFlowTask.setOwnerId(executor.getId());
                    newFlowTask.setOwnerName(executor.getName());
                    newFlowTask.setOwnerAccount(executor.getCode());
                    //添加组织机构信息
                    newFlowTask.setOwnerOrgId(executor.getOrganizationId());
                    newFlowTask.setOwnerOrgCode(executor.getOrganizationCode());
                    newFlowTask.setOwnerOrgName(executor.getOrganizationName());
                    newFlowTask.setTrustState(null);
                    taskService.setAssignee(flowTask.getActTaskId(), executor.getId());

                    if (flowTask.getFlowInstance() != null && flowTask.getFlowInstance().getAllowEmergency()) {
                        newFlowTask.setPriority(3);
                    }

                    // 取得当前任务
                    HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId())
                            .singleResult();
                    String taskJsonDef = newFlowTask.getTaskJsonDef();
                    JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                    String nodeType = taskJsonDefObj.get("nodeType") + "";//会签
                    if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务替换执行人集合
                        String processInstanceId = currTask.getProcessInstanceId();
                        String userListDesc = currTask.getTaskDefinitionKey() + "_List_CounterSign";
                        List<String> userList = (List<String>) runtimeService.getVariableLocal(processInstanceId, userListDesc);
                        Collections.replaceAll(userList, flowTask.getExecutorId(), userId);
                        runtimeService.setVariableLocal(processInstanceId, userListDesc, userList);
                    }

                    flowHistoryDao.save(flowHistory);
                    //是否推送信息到baisc
                    Boolean pushBasic = this.getBooleanPushTaskToBasic();

                    List<FlowTask> needDelList = new ArrayList<>();  //需要删除的待办
                    if (pushBasic) {
                        needDelList.add(flowTask);
                    }
                    flowTaskDao.delete(flowTask);
                    flowTaskDao.save(newFlowTask);
                    List<FlowTask> needAddList = new ArrayList<>(); //需要新增的待办
                    if (pushBasic) {
                        needAddList.add(newFlowTask);
                        if (pushBasic) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    pushToBasic(needAddList, null, needDelList, null);
                                }
                            }).start();
                        }
                    }
                    result = OperateResult.operationSuccess();
                } else {
                    result = OperateResult.operationFailure("10038");//执行人查询结果为空
                }
            } else {
                result = OperateResult.operationFailure("10033");//任务不存在，可能已经被处理
            }
            return result;
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw e;
        } finally {
            //转办开始的时候设置的检查参数
            redisTemplate.delete("taskTurn_" + taskId);
        }
    }


    /**
     * 委托多人
     *
     * @param taskTrustInfoVo 任务委托信息
     * @return
     */
    public ResponseData taskTrustToEmployees(TaskTrustInfoVo taskTrustInfoVo) {
        if (taskTrustInfoVo == null) {
            return ResponseData.operationFailure("10384");
        }
        String taskId = taskTrustInfoVo.getTaskId();
        if (StringUtils.isEmpty(taskId)) {
            return ResponseData.operationFailure("10385");
        }
        List<String> userIds = taskTrustInfoVo.getUserIds();
        if (CollectionUtils.isEmpty(userIds)) {
            return ResponseData.operationFailure("10386");
        }
        String opinion = taskTrustInfoVo.getOpinion();
        if (StringUtils.isEmpty(opinion) || "null".equals(opinion)) {
            opinion = "";
        }


        Boolean setValue = redisTemplate.opsForValue().setIfAbsent("taskTrust_" + taskId, taskId);
        if (!setValue) {
            Long remainingTime = redisTemplate.getExpire("taskTrust_" + taskId, TimeUnit.SECONDS);
            if (remainingTime == -1) {  //说明时间未设置进去
                redisTemplate.expire("taskTrust_" + taskId, 2 * 60, TimeUnit.SECONDS);
                remainingTime = 120L;
            }
            throw new FlowException(ContextUtil.getMessage("10231", remainingTime));
        }

        try {
            //如果设置成功，redis检查设置2分钟过期
            redisTemplate.expire("taskTrust_" + taskId, 2 * 60, TimeUnit.SECONDS);
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (flowTask != null) {
                if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
                    return ResponseData.operationFailure("10201");
                }
                if (flowTask.getTrustState() != null && flowTask.getTrustState() == 1) {
                    return ResponseData.operationFailure("10232");
                }
                String defJson = flowTask.getTaskJsonDef();
                JSONObject defObj = JSONObject.fromObject(defJson);
                //单签任务，先进行签收
                if (defObj.getString("nodeType").equalsIgnoreCase("SingleSign")) {
                    OperateResult resultData = this.claim(taskId, ContextUtil.getUserId());
                    if (!resultData.successful()) {
                        return resultData;
                    }
                }
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult(); // 创建历史任务实例查询
                FlowHistory flowHistory = flowTaskTool.initFlowHistory(flowTask, historicTaskInstance, true, null); //委托后先允许撤回
                //是否推送信息到baisc
                Boolean pushBasic = this.getBooleanPushTaskToBasic();
                List<FlowTask> needDelList = new ArrayList<>(); //需要删除的待办
                List<FlowTask> needAddList = new ArrayList<>(); //需要新增的待办

                String employeeNameStr = "";


                //通过用户ID获取执行人
                List<Executor> executorList = flowCommonUtil.getBasicUserExecutors(userIds);
                if (CollectionUtils.isEmpty(executorList)) {
                    return ResponseData.operationFailure("10038");
                }

                //委托任务不能退回
                String taskJsonDef = flowTask.getTaskJsonDef();
                JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                JSONObject nodeConfig = taskJsonDefObj.getJSONObject("nodeConfig");
                JSONObject normalInfo = nodeConfig.getJSONObject("normal");
                normalInfo.put("allowReturn", false);
                nodeConfig.put("normal", normalInfo);
                taskJsonDefObj.put("nodeConfig", nodeConfig);


                for (Executor executor : executorList) {
                    if (StringUtils.isEmpty(employeeNameStr)) {
                        employeeNameStr += executor.getName();
                    } else {
                        employeeNameStr += "," + executor.getName();
                    }
                    FlowTask newFlowTask = new FlowTask();
                    BeanUtils.copyProperties(flowTask, newFlowTask);
                    newFlowTask.setId(null);
                    //判断待办转授权模式(如果是转办模式，需要返回转授权信息，其余情况返回null)
                    TaskMakeOverPower taskMakeOverPower = taskMakeOverPowerService.getMakeOverPowerByTypeAndUserId(executor.getId(), flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getId());
                    String taskDepict = flowTask.getDepict() == null ? "" : flowTask.getDepict();
                    if (taskMakeOverPower != null) {
                        newFlowTask.setExecutorId(taskMakeOverPower.getPowerUserId());
                        newFlowTask.setExecutorAccount(taskMakeOverPower.getPowerUserAccount());
                        newFlowTask.setExecutorName(taskMakeOverPower.getPowerUserName());
                        newFlowTask.setDepict("【由-" + flowTask.getExecutorName() + "委托】【转授权-" + executor.getName() + "授权】" + taskDepict);
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(taskMakeOverPower.getPowerUserOrgId());
                        newFlowTask.setExecutorOrgCode(taskMakeOverPower.getPowerUserOrgCode());
                        newFlowTask.setExecutorOrgName(taskMakeOverPower.getPowerUserOrgName());
                    } else {
                        newFlowTask.setExecutorId(executor.getId());
                        newFlowTask.setExecutorAccount(executor.getCode());
                        newFlowTask.setExecutorName(executor.getName());
                        newFlowTask.setDepict("【由-" + flowTask.getExecutorName() + "委托】" + taskDepict);
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(executor.getOrganizationId());
                        newFlowTask.setExecutorOrgCode(executor.getOrganizationCode());
                        newFlowTask.setExecutorOrgName(executor.getOrganizationName());
                    }
                    newFlowTask.setOwnerId(executor.getId());
                    newFlowTask.setOwnerAccount(executor.getCode());
                    newFlowTask.setOwnerName(executor.getName());

                    //添加组织机构信息
                    newFlowTask.setOwnerOrgId(executor.getOrganizationId());
                    newFlowTask.setOwnerOrgCode(executor.getOrganizationCode());
                    newFlowTask.setOwnerOrgName(executor.getOrganizationName());
                    newFlowTask.setPreId(flowHistory.getId());
                    newFlowTask.setTrustState(2);
                    newFlowTask.setTrustOwnerTaskId(flowTask.getId());
                    //委托任务去掉退回按钮
                    newFlowTask.setTaskJsonDef(taskJsonDefObj.toString());

                    if (flowTask.getFlowInstance() != null && flowTask.getFlowInstance().getAllowEmergency()) {
                        newFlowTask.setPriority(3);
                    }

                    flowTaskDao.save(newFlowTask);
                    if (pushBasic) {
                        needAddList.add(newFlowTask);
                    }
                }
                //如果是转授权转办模式（获取转授权记录信息）
                String overPowerStr = taskMakeOverPowerService.getOverPowerStrByDepict(flowHistory.getDepict());
                flowHistory.setDepict(overPowerStr + "【委托给：" + employeeNameStr + "】" + opinion);
                flowHistory.setFlowExecuteStatus(FlowExecuteStatus.ENTRUST.getCode());//委托
                flowHistoryDao.save(flowHistory);

                //修改委托任务状态
                flowTask.setTrustState(1);
                flowTaskDao.save(flowTask);
                if (pushBasic) {
                    needDelList.add(flowTask);
                }

                //新增和删除待办
                if (pushBasic) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pushToBasic(needAddList, null, needDelList, null);
                        }
                    }).start();
                }
                return ResponseData.operationSuccess();
            } else {
                return ResponseData.operationFailure("10033");//任务不存在，可能已经被处理
            }
        } catch (Exception e) {
            LogUtil.error("任务委托报错：" + e.getMessage(), e);
            throw e;
        } finally {
            //委托开始的时候设置的检查参数
            redisTemplate.delete("taskTrust_" + taskId);
        }
    }


    public OperateResult taskTrustToDo(String taskId, String userId) throws Exception {

        Boolean setValue = redisTemplate.opsForValue().setIfAbsent("taskTrust_" + taskId, taskId);
        if (!setValue) {
            Long remainingTime = redisTemplate.getExpire("taskTrust_" + taskId, TimeUnit.SECONDS);
            if (remainingTime == -1) {  //说明时间未设置进去
                redisTemplate.expire("taskTrust_" + taskId, 2 * 60, TimeUnit.SECONDS);
                remainingTime = 120L;
            }
            throw new FlowException(ContextUtil.getMessage("10231", remainingTime));
        }

        try {
            //如果设置成功，redis检查设置2分钟过期
            redisTemplate.expire("taskTrust_" + taskId, 2 * 60, TimeUnit.SECONDS);

            OperateResult result;
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (flowTask != null) {

                if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟任务
                    return OperateResult.operationFailure("10201");
                }

                if (flowTask.getTrustState() != null && flowTask.getTrustState() == 1) {
                    return OperateResult.operationFailure("10232");
                }

                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult(); // 创建历史任务实例查询
                //通过用户ID获取执行人
                Executor executor = flowCommonUtil.getBasicUserExecutor(userId);
                if (executor != null) {
                    FlowTask newFlowTask = new FlowTask();
                    BeanUtils.copyProperties(flowTask, newFlowTask);
                    FlowHistory flowHistory = flowTaskTool.initFlowHistory(flowTask, historicTaskInstance, true, null); //委托后先允许撤回
                    //如果是转授权转办模式（获取转授权记录信息）
                    String overPowerStr = taskMakeOverPowerService.getOverPowerStrByDepict(flowHistory.getDepict());
                    flowHistory.setDepict(overPowerStr + "【被委托给：" + executor.getName() + "】");
                    flowHistory.setFlowExecuteStatus(FlowExecuteStatus.ENTRUST.getCode());//委托

                    newFlowTask.setId(null);
                    //判断待办转授权模式(如果是转办模式，需要返回转授权信息，其余情况返回null)
                    TaskMakeOverPower taskMakeOverPower = taskMakeOverPowerService.getMakeOverPowerByTypeAndUserId(executor.getId(), flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getId());
                    String taskDepict = flowTask.getDepict() == null ? "" : flowTask.getDepict();
                    if (taskMakeOverPower != null) {
                        newFlowTask.setExecutorId(taskMakeOverPower.getPowerUserId());
                        newFlowTask.setExecutorAccount(taskMakeOverPower.getPowerUserAccount());
                        newFlowTask.setExecutorName(taskMakeOverPower.getPowerUserName());
                        newFlowTask.setDepict("【由：“" + flowTask.getExecutorName() + "”委托】【转授权-" + executor.getName() + "授权】" + taskDepict);
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(taskMakeOverPower.getPowerUserOrgId());
                        newFlowTask.setExecutorOrgCode(taskMakeOverPower.getPowerUserOrgCode());
                        newFlowTask.setExecutorOrgName(taskMakeOverPower.getPowerUserOrgName());
                    } else {
                        newFlowTask.setExecutorId(executor.getId());
                        newFlowTask.setExecutorAccount(executor.getCode());
                        newFlowTask.setExecutorName(executor.getName());
                        newFlowTask.setDepict("【由：“" + flowTask.getExecutorName() + "”委托】" + taskDepict);
                        //添加组织机构信息
                        newFlowTask.setExecutorOrgId(executor.getOrganizationId());
                        newFlowTask.setExecutorOrgCode(executor.getOrganizationCode());
                        newFlowTask.setExecutorOrgName(executor.getOrganizationName());
                    }

                    newFlowTask.setOwnerId(executor.getId());
                    newFlowTask.setOwnerAccount(executor.getCode());
                    newFlowTask.setOwnerName(executor.getName());
                    //添加组织机构信息
                    newFlowTask.setOwnerOrgId(executor.getOrganizationId());
                    newFlowTask.setOwnerOrgCode(executor.getOrganizationCode());
                    newFlowTask.setOwnerOrgName(executor.getOrganizationName());
                    newFlowTask.setPreId(flowHistory.getId());
                    flowHistoryDao.save(flowHistory);
                    flowTask.setTrustState(1);
                    newFlowTask.setTrustState(2);
                    newFlowTask.setTrustOwnerTaskId(flowTask.getId());


                    //委托任务不能退回
                    String taskJsonDef = newFlowTask.getTaskJsonDef();
                    JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                    JSONObject nodeConfig = taskJsonDefObj.getJSONObject("nodeConfig");
                    JSONObject normalInfo = nodeConfig.getJSONObject("normal");
                    normalInfo.put("allowReturn", false);
                    nodeConfig.put("normal", normalInfo);
                    taskJsonDefObj.put("nodeConfig", nodeConfig);
                    newFlowTask.setTaskJsonDef(taskJsonDefObj.toString());


                    //是否推送信息到baisc
                    Boolean pushBasic = this.getBooleanPushTaskToBasic();
                    List<FlowTask> needDelList = new ArrayList<>(); //需要删除的待办
                    List<FlowTask> needAddList = new ArrayList<>(); //需要新增的待办
                    if (pushBasic) {
                        needDelList.add(flowTask);
                    }
                    flowTaskDao.save(flowTask);
                    flowTaskDao.save(newFlowTask);
                    if (pushBasic) {
                        needAddList.add(newFlowTask);
                    }

                    //新增和删除待办
                    if (pushBasic) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                pushToBasic(needAddList, null, needDelList, null);
                            }
                        }).start();
                    }

                    result = OperateResult.operationSuccess();
                } else {
                    result = OperateResult.operationFailure("10038");//执行人查询结果为空
                }
            } else {
                result = OperateResult.operationFailure("10033");//任务不存在，可能已经被处理
            }
            return result;
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw e;
        } finally {
            //委托开始的时候设置的检查参数
            redisTemplate.delete("taskTrust_" + taskId);
        }
    }


    public List<Executor> getCounterSignExecutorList(String actInstanceId, String taskActKey) throws Exception {
        List<Executor> result = null;
        List<FlowTask> flowTaskList = flowTaskDao.findByActTaskDefKeyAndActInstanceId(taskActKey, actInstanceId);
        if (!CollectionUtils.isEmpty(flowTaskList)) {
            FlowTask flowTaskTemp = flowTaskList.get(0);
            // 取得当前任务
            HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTaskTemp.getActTaskId())
                    .singleResult();
            String taskJsonDef = flowTaskTemp.getTaskJsonDef();
            JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
            String nodeType = taskJsonDefObj.get("nodeType") + "";//会签
            if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务做处理判断
                String processInstanceId = currTask.getProcessInstanceId();
                String userListDesc = currTask.getTaskDefinitionKey() + "_List_CounterSign";
                List<String> userList = (List<String>) runtimeService.getVariableLocal(processInstanceId, userListDesc);
                //根据用户的id列表获取执行人
                result = flowCommonUtil.getBasicUserExecutors(userList);
            } else {
                throw new FlowException(ContextUtil.getMessage("10233"));
            }
        }
        return result;
    }

    public OperateResult taskTrustToReturn(String taskId, String opinion) throws Exception {
        OperateResult result;
        FlowTask flowTask = flowTaskDao.findOne(taskId);
        if (flowTask != null) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult(); // 创建历史任务实例查询
            FlowHistory flowHistory = flowTaskTool.initFlowHistory(flowTask, historicTaskInstance, null, null);
            //如果是转授权转办模式（获取转授权记录信息）
            String overPowerStr = taskMakeOverPowerService.getOverPowerStrByDepict(flowHistory.getDepict());

            //实现了委托多人
            List<FlowTask> flowTaskList = flowTaskDao.findListByProperty("trustOwnerTaskId", flowTask.getTrustOwnerTaskId());
            if (flowTaskList.size() > 1) { //还有其他委托任务
                flowHistory.setDepict(overPowerStr + "【委托任务完成】" + opinion);
                flowHistory.setFlowExecuteStatus(FlowExecuteStatus.SUBMIT.getCode());
                //是否推送信息到basic
                Boolean pushBasic = this.getBooleanPushTaskToBasic();
                if (pushBasic) {
                    List<FlowTask> needDelList = new ArrayList<>();
                    needDelList.add(flowTask);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pushToBasic(null, needDelList, null, null);
                        }
                    }).start();
                }
                flowHistoryDao.save(flowHistory);
                flowTaskDao.delete(flowTask);
                result = OperateResult.operationSuccess();
            } else { //最后的委托任务
                FlowTask oldFlowTask = flowTaskDao.findOne(flowTask.getTrustOwnerTaskId());
                if (oldFlowTask != null) {
                    flowHistory.setDepict(overPowerStr + "【办理完成返回委托方】" + opinion);
                    flowHistory.setFlowExecuteStatus(FlowExecuteStatus.SUBMIT.getCode());
                    //判断待办转授权模式(如果是转办模式，需要返回转授权信息，其余情况返回null)
                    TaskMakeOverPower taskMakeOverPower = taskMakeOverPowerService.getMakeOverPowerByTypeAndUserId(oldFlowTask.getExecutorId(), oldFlowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getId());
                    if (taskMakeOverPower != null) {
                        oldFlowTask.setExecutorId(taskMakeOverPower.getPowerUserId());
                        oldFlowTask.setExecutorAccount(taskMakeOverPower.getPowerUserAccount());
                        oldFlowTask.setExecutorName(taskMakeOverPower.getPowerUserName());
                        //添加组织机构信息
                        oldFlowTask.setExecutorOrgId(taskMakeOverPower.getPowerUserOrgId());
                        oldFlowTask.setExecutorOrgCode(taskMakeOverPower.getPowerUserOrgCode());
                        oldFlowTask.setExecutorOrgName(taskMakeOverPower.getPowerUserOrgName());
                        oldFlowTask.setDepict("【委托完成】【转授权-" + taskMakeOverPower.getUserName() + "授权】" + opinion);
                    } else {
                        oldFlowTask.setDepict("【委托完成】" + opinion);
                    }
                    oldFlowTask.setTrustState(3);  //委托完成
                    oldFlowTask.setPreId(flowHistory.getId());
                    flowHistoryDao.save(flowHistory);
                    //是否推送信息到basic
                    Boolean pushBasic = this.getBooleanPushTaskToBasic();
                    if (pushBasic) {
                        List<FlowTask> needDelList = new ArrayList<>();
                        needDelList.add(flowTask);
                        List<FlowTask> needAddList = new ArrayList<>();
                        needAddList.add(oldFlowTask);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                pushToBasic(needAddList, needDelList, null, null);
                            }
                        }).start();
                    }
                    flowTaskDao.save(oldFlowTask);
                    flowTaskDao.delete(flowTask);
                    result = OperateResult.operationSuccess();
                } else {
                    result = OperateResult.operationFailure("10038");//执行人查询结果为空
                }
            }
        } else {
            result = OperateResult.operationFailure("10033");//任务不存在，可能已经被处理
        }
        return result;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResult counterSignAdd(String actInstanceId, String taskActKey, String userIds) throws Exception {

        String[] userIdArray = null;
        StringBuffer resultDec = new StringBuffer();
        StringBuffer resultDecTrue = new StringBuffer();
        StringBuffer resultDecFalseOne = new StringBuffer();
        StringBuffer resultDecFalseTwo = new StringBuffer();
        StringBuffer resultDecFalseThree = new StringBuffer();
        OperateResult result = null;
        if (StringUtils.isNotEmpty(userIds)) {
            userIdArray = userIds.split(",");
            if (userIdArray != null && userIdArray.length > 0) {
                for (String userId : userIdArray) {
                    //检查用户是否存在
                    Executor executor = null;
                    try {
                        executor = flowCommonUtil.getBasicUserExecutor(userId);
                    } catch (IllegalArgumentException e) {
                        LogUtil.error(e.getMessage(), e);
                    }
                    if (executor == null) {
                        resultDecFalseOne.append("【ID='" + userId + "'】");
                        continue;
                    }
                    List<FlowTask> flowTaskList = flowTaskDao.findByActTaskDefKeyAndActInstanceId(taskActKey, actInstanceId);
                    if (!CollectionUtils.isEmpty(flowTaskList)) {
                        FlowTask flowTaskTemp = flowTaskList.get(0);
                        // 取得当前任务
                        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTaskTemp.getActTaskId())
                                .singleResult();
                        // 取得流程定义
                        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                                .getDeployedProcessDefinition(currTask.getProcessDefinitionId());
                        if (definition == null) {
                            LogUtil.error(ContextUtil.getMessage("10003"));
                            return OperateResult.operationFailure("10003");//流程定义未找到找到");
                        }
                        FlowInstance flowInstance = flowTaskTemp.getFlowInstance();
                        String taskJsonDef = flowTaskTemp.getTaskJsonDef();
                        JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                        String nodeType = taskJsonDefObj.get("nodeType") + "";//会签
                        if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务做处理判断
                            String executionId = currTask.getExecutionId();
                            Execution execution = runtimeService.createExecutionQuery().executionId(executionId).singleResult();
                            ExecutionEntity executionEntity = (ExecutionEntity) execution;
                            String processInstanceId = currTask.getProcessInstanceId();
                            Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(executionId);
                            //总循环次数
                            Integer instanceOfNumbers = (Integer) processVariables.get("nrOfInstances").getValue();
                            runtimeService.setVariable(executionId, "nrOfInstances", (instanceOfNumbers + 1));
                            //判断是否是并行会签
                            Boolean isSequential = taskJsonDefObj.getJSONObject("nodeConfig").getJSONObject("normal").getBoolean("isSequential");
                            if (isSequential == false) {
                                Integer nrOfActiveInstancesNumbers = (Integer) processVariables.get("nrOfActiveInstances").getValue();
                                runtimeService.setVariable(executionId, "nrOfActiveInstances", (nrOfActiveInstancesNumbers + 1));
                            }
                            String userListDesc = currTask.getTaskDefinitionKey() + "_List_CounterSign";
                            List<String> userList = (List<String>) runtimeService.getVariableLocal(processInstanceId, userListDesc);
                            userList = new ArrayList<String>(userList);
                            userList.add(userId);
                            runtimeService.setVariable(processInstanceId, userListDesc, userList);
                            if (isSequential == false) {//并行会签，需要立即初始化执行人
                                taskService.counterSignAddTask(userId, executionEntity, currTask);
                                String preId = flowTaskTemp.getPreId();
                                flowTaskTool.initCounterSignAddTask(flowInstance, currTask.getTaskDefinitionKey(), userId, preId);
                            }
                            resultDecTrue.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                            LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的加签操作执行成功;");
                            continue;
                        } else {
                            resultDecFalseTwo.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                            LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的执行节点为非会签节点，无法加签;");
                            continue;
                        }
                    } else {
                        resultDecFalseThree.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                        LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,任务可能已经执行完，无法加签;");
                        continue;
                    }
                }
            } else {
                return OperateResult.operationFailure("10234");
            }
        } else {
            return OperateResult.operationFailure("10234");
        }

        if (resultDecTrue.length() > 0) {
            resultDecTrue.append(ContextUtil.getMessage("10235"));
        }
        if (resultDecFalseOne.length() > 0) {
            resultDecFalseOne.append(ContextUtil.getMessage("10236"));
        }
        if (resultDecFalseTwo.length() > 0) {
            resultDecFalseTwo.append(ContextUtil.getMessage("10237"));
        }
        if (resultDecFalseThree.length() > 0) {
            resultDecFalseThree.append(ContextUtil.getMessage("10238"));
        }
        resultDec.append(resultDecTrue).append(resultDecFalseOne).append(resultDecFalseTwo).append(resultDecFalseThree);
        if (resultDecTrue.length() > 0) {
            result = OperateResult.operationSuccess(resultDec.toString());
        } else {
            result = OperateResult.operationFailure(resultDec.toString());
        }
        return result;
    }

    /**
     * @param actInstanceId 流程实例id
     * @param taskActKey    节点key
     * @param userIds       被减签的人
     * @return
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResult counterSignDel(String actInstanceId, String taskActKey, String userIds) throws Exception {
        String[] userIdArray = null;
        StringBuffer resultDec = new StringBuffer();
        StringBuffer resultDecTrue = new StringBuffer();
        StringBuffer resultDecFalseOne = new StringBuffer();
        StringBuffer resultDecFalseTwo = new StringBuffer();
        StringBuffer resultDecFalseThree = new StringBuffer();
        StringBuffer resultDecFalseFour = new StringBuffer();
        StringBuffer resultDecFalseFive = new StringBuffer();
        StringBuffer resultDecFalseSix = new StringBuffer();
        StringBuffer resultDecFalseSeven = new StringBuffer();
        StringBuffer resultDecFalseEight = new StringBuffer();
        OperateResult result = null;
        if (StringUtils.isNotEmpty(userIds)) {
            userIdArray = userIds.split(",");
            if (userIdArray != null && userIdArray.length > 0) {
                for (String userId : userIdArray) {
                    //检查用户是否存在
                    Executor executor = null;
                    try {
                        executor = flowCommonUtil.getBasicUserExecutor(userId);
                    } catch (IllegalArgumentException e) {
                        LogUtil.error(e.getMessage(), e);
                    }
                    if (executor == null) {
                        resultDecFalseOne.append("【ID=" + userId + "】");
                        continue;
                    }
                    List<FlowTask> flowTaskList = flowTaskDao.findByActTaskDefKeyAndActInstanceId(taskActKey, actInstanceId);
                    if (!CollectionUtils.isEmpty(flowTaskList)) {
                        FlowTask flowTaskTemp = flowTaskList.get(0);
                        FlowInstance flowInstance = flowTaskTemp.getFlowInstance();
                        // 取得当前任务
                        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTaskTemp.getActTaskId())
                                .singleResult();
                        // 取得流程定义
                        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                                .getDeployedProcessDefinition(currTask.getProcessDefinitionId());
                        if (definition == null) {
                            LogUtil.error(ContextUtil.getMessage("10003"));
                            return OperateResult.operationFailure("10003");//流程定义未找到找到");
                        }
                        String taskJsonDef = flowTaskTemp.getTaskJsonDef();
                        JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                        String nodeType = taskJsonDefObj.get("nodeType") + "";//会签
                        if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务做处理判断
                            String processInstanceId = currTask.getProcessInstanceId();
                            String userListDesc = currTask.getTaskDefinitionKey() + "_List_CounterSign";
                            List<String> userListArray = (List<String>) runtimeService.getVariableLocal(processInstanceId, userListDesc);
                            List<String> userList = new ArrayList<>(userListArray);
                            if (!userList.contains(userId)) {
                                resultDecFalseTwo.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                                LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,在当前任务节点找不到，减签失败;");
                                continue;
                            }
                            String executionId = currTask.getExecutionId();
                            Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(executionId);
                            //总循环次数
                            Integer instanceOfNumbers = (Integer) processVariables.get("nrOfInstances").getValue();
                            //完成会签的次数
                            Integer completeCounter = (Integer) processVariables.get("nrOfCompletedInstances").getValue();
                            if (completeCounter + 1 == instanceOfNumbers) {//最后一个任务
                                resultDecFalseThree.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                                LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,任务已经到达最后一位执行人，减签失败;");
                                continue;
                            }

                            //判断是否是并行会签
                            Boolean isSequential = taskJsonDefObj.getJSONObject("nodeConfig").getJSONObject("normal").getBoolean("isSequential");

                            List<FlowTask> flowTaskListCurrent = flowTaskDao.findByActTaskDefKeyAndActInstanceIdAndExecutorId(taskActKey, actInstanceId, userId);
                            if (isSequential == false) {//并行会签，需要清空对应的执行人任务信息
                                if (!CollectionUtils.isEmpty(flowTaskListCurrent)) {
                                    //是否推送信息到baisc
                                    Boolean pushBasic = this.getBooleanPushTaskToBasic();
                                    runtimeService.setVariable(executionId, "nrOfInstances", (instanceOfNumbers - 1));
                                    if (isSequential == false) {
                                        Integer nrOfActiveInstancesNumbers = (Integer) processVariables.get("nrOfActiveInstances").getValue();
                                        runtimeService.setVariable(executionId, "nrOfActiveInstances", (nrOfActiveInstancesNumbers - 1));
                                    }
                                    userList.remove(userId);
                                    runtimeService.setVariable(processInstanceId, userListDesc, userList);//回写减签后的执行人列表

                                    List<FlowTask> delList = new ArrayList<>();
                                    for (FlowTask flowTask : flowTaskListCurrent) {
                                        if (pushBasic) {
                                            delList.add(flowTask);
                                        }
                                        taskService.deleteRuningTask(flowTask.getActTaskId(), true);
                                        flowTaskDao.delete(flowTask);
                                    }

                                    if (pushBasic) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pushToBasic(null, null, delList, null);
                                            }
                                        }).start();
                                    }

                                } else {
                                    resultDecFalseFour.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                                    LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,当前任务节点已执行，减签失败;");
                                    continue;
                                }
                            } else {//串行会签不允许对当前在线的任务进行直接减签，未来可扩展允许
                                if (!CollectionUtils.isEmpty(flowTaskListCurrent)) {
                                    resultDecFalseFive.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                                    LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,串行会签不允许对当前在线的执行人直接减签操作，减签失败;");
                                    continue;
                                } else {
                                    List<FlowHistory> flowHistoryList = flowHistoryDao.findByActTaskDefKeyAndActInstanceId(taskActKey, actInstanceId);
                                    boolean canDel = true;
                                    if (!CollectionUtils.isEmpty(flowHistoryList)) {
                                        while (flowHistoryList.size() > userList.size()) {
                                            for (int index = 0; index < userList.size(); index++) {
                                                flowHistoryList.remove(index);
                                            }
                                        }
                                        for (FlowHistory flowHistory : flowHistoryList) {
                                            if (userId.equals(flowHistory.getExecutorId())) {
                                                canDel = false;
                                                break;
                                            }
                                        }
                                    }
                                    if (canDel) {
                                        runtimeService.setVariable(executionId, "nrOfInstances", (instanceOfNumbers - 1));
                                        if (isSequential == false) {
                                            Integer nrOfActiveInstancesNumbers = (Integer) processVariables.get("nrOfActiveInstances").getValue();
                                            runtimeService.setVariable(executionId, "nrOfActiveInstances", (nrOfActiveInstancesNumbers - 1));
                                        }
                                        userList.remove(userId);
                                        runtimeService.setVariable(processInstanceId, userListDesc, userList);//回写减签后的执行人列表
                                    } else {
                                        resultDecFalseSix.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                                        LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,发现已经执行，减签操作执行失败;");
                                        continue;
                                    }
                                }
                            }

                            resultDecTrue.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                            LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,的减签操作执行成;");
                        } else {
                            resultDecFalseSeven.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                            LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,执行节点为非会签节点，无法减签;");
                            continue;
                        }
                    } else {
                        resultDecFalseEight.append("【" + executor.getName() + "-" + executor.getCode() + "】");
                        LogUtil.bizLog(executor.getName() + "【" + executor.getCode() + "】,id='" + executor.getId() + "'的用户,任务可能已经执行完，无法减签;");
                        continue;
                    }
                }
            } else {
                return OperateResult.operationFailure("10234");
            }
        }

        if (resultDecTrue.length() > 0) {
            resultDecTrue.append("10239");
        }
        if (resultDecFalseOne.length() > 0) {
            resultDecFalseOne.append("10240");
        }
        if (resultDecFalseTwo.length() > 0) {
            resultDecFalseTwo.append("10241");
        }
        if (resultDecFalseThree.length() > 0) {
            resultDecFalseThree.append("10242");
        }
        if (resultDecFalseFour.length() > 0) {
            resultDecFalseFour.append("10243");
        }
        if (resultDecFalseFive.length() > 0) {
            resultDecFalseFive.append("10244");
        }
        if (resultDecFalseSix.length() > 0) {
            resultDecFalseSix.append("10245");
        }
        if (resultDecFalseSeven.length() > 0) {
            resultDecFalseSeven.append("10246");
        }
        if (resultDecFalseEight.length() > 0) {
            resultDecFalseEight.append("10247");
        }
        resultDec.append(resultDecTrue).append(resultDecFalseOne).append(resultDecFalseTwo)
                .append(resultDecFalseThree).append(resultDecFalseFour).append(resultDecFalseFive)
                .append(resultDecFalseSix).append(resultDecFalseSeven).append(resultDecFalseEight);
        if (resultDecTrue.length() > 0) {
            result = OperateResult.operationSuccess(resultDec.toString());
        } else {
            result = OperateResult.operationFailure(resultDec.toString());
        }
        return result;
    }

    public List<CanAddOrDelNodeInfo> getAllCanAddNodeInfoList() throws Exception {
        List<CanAddOrDelNodeInfo> result = new ArrayList<>();
        List<CanAddOrDelNodeInfo> resultDai = flowTaskDao.findByAllowAddSign(ContextUtil.getUserId());
        List<CanAddOrDelNodeInfo> resultStart = flowTaskDao.findByAllowAddSignStart(ContextUtil.getUserId());
        result.addAll(resultStart);
        result.addAll(resultDai);
        Map<String, CanAddOrDelNodeInfo> tempMap = new HashMap<>();
        for (CanAddOrDelNodeInfo c : result) {
            //设置执行人列表名称
            this.setNameListByCanAddOrDelNodeInfo(c);
            tempMap.put(c.getActInstanceId() + c.getNodeKey(), c);
        }
        result.clear();
        result.addAll(tempMap.values());
        return result;
    }

    public List<CanAddOrDelNodeInfo> getAllCanDelNodeInfoList() throws Exception {
        List<CanAddOrDelNodeInfo> result = new ArrayList<>();
        List<CanAddOrDelNodeInfo> resultDai = flowTaskDao.findByAllowSubtractSign(ContextUtil.getUserId());
        List<CanAddOrDelNodeInfo> resultStart = flowTaskDao.findByAllowSubtractSignStart(ContextUtil.getUserId());
        result.addAll(resultStart);
        result.addAll(resultDai);
        Map<String, CanAddOrDelNodeInfo> tempMap = new HashMap<>();
        for (CanAddOrDelNodeInfo c : result) {
            //设置执行人列表名称
            this.setNameListByCanAddOrDelNodeInfo(c);
            tempMap.put(c.getActInstanceId() + c.getNodeKey(), c);
        }
        result.clear();
        result.addAll(tempMap.values());
        return result;
    }

    public void setNameListByCanAddOrDelNodeInfo(CanAddOrDelNodeInfo nodeInfo) {
        List<Executor> exeList = null;
        try {
            exeList = this.getCounterSignExecutorList(nodeInfo.getActInstanceId(), nodeInfo.getNodeKey());
        } catch (Exception e) {
            LogUtil.error("会签获取执行人列表失败！actInstanceId=" + nodeInfo.getActInstanceId() + ",taskActKey=" + nodeInfo.getNodeKey(), e);
        }
        if (!CollectionUtils.isEmpty(exeList)) {
            List<String> exeNameList = exeList.stream().map(Executor::getName).collect(Collectors.toList());
            nodeInfo.setExecutorNameList(StringUtils.join(exeNameList.toArray(), ","));
        }
    }

    public OperateResult reminding() {
        OperateResult result = null;
        List<FlowTaskExecutorIdAndCount> executorIdAndCountList = flowTaskDao.findAllExecutorIdAndCount();
        if (executorIdAndCountList != null && !executorIdAndCountList.isEmpty()) {
            Map<String, Long> executorIdAndCountMap = executorIdAndCountList.stream().collect(Collectors.toMap(FlowTaskExecutorIdAndCount::getExecutorId, FlowTaskExecutorIdAndCount::getCount));
            //调用basic个人基本信息
            if (!CollectionUtils.isEmpty(executorIdAndCountMap)) {
                Set<String> userIdSet = executorIdAndCountMap.keySet();

                String url = Constants.getUserEmailAlertFindByUserIdsUrl();
                List<UserEmailAlert> userEmailAlertList = ApiClient.postViaProxyReturnResult(url, new GenericType<List<UserEmailAlert>>() {
                }, userIdSet);
                if (!CollectionUtils.isEmpty(userEmailAlertList)) {
                    for (UserEmailAlert userEmailAlert : userEmailAlertList) {
                        Integer jianGeTime = userEmailAlert.getHours();//间隔时间
                        Integer toDoAmount = userEmailAlert.getToDoAmount();//待办数量阀值
                        Date lastSendTime = userEmailAlert.getLastTime();
                        if (jianGeTime != null && jianGeTime > 0) {
                            if (lastSendTime == null) {
                                //直接发送邮件
                                emailSend(userEmailAlert.getUserId());
                                LogUtil.bizLog("催办提醒：" + userEmailAlert.getUserId() + "，最长间隔时间到达，lastSendTime==null。");
                                continue;
                            }
                            double hours = (System.currentTimeMillis() - lastSendTime.getTime()) / (1000 * 60 * 60.0);
                            if (hours >= jianGeTime) {
                                //发送邮件
                                emailSend(userEmailAlert.getUserId());
                                LogUtil.bizLog("催办提醒：" + userEmailAlert.getUserId() + "，最长间隔时间到达。");
                            }
                        }
                        if (toDoAmount >= executorIdAndCountMap.get(userEmailAlert.getUserId())) {
                            if (lastSendTime == null) {
                                //直接发送邮件
                                emailSend(userEmailAlert.getUserId());
                                LogUtil.bizLog("催办提醒：" + userEmailAlert.getUserId() + "，待办数量超过阀值，lastSendTime==null。");
                                continue;
                            }
                            double hours = (System.currentTimeMillis() - lastSendTime.getTime()) / (1000 * 60 * 60.0);
                            if (hours >= 1) {
                                //发送邮件
                                emailSend(userEmailAlert.getUserId());
                                LogUtil.bizLog("催办提醒：" + userEmailAlert.getUserId() + "，待办数量超过阀值。");
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private void emailSend(String userId) {
        String userName;
        List<FlowTaskExecutorIdAndCount> list = flowTaskDao.findAllTaskKeyAndCountByExecutorId(userId);
        if (!CollectionUtils.isEmpty(list)) {
            Map<String, Object> contentTemplateParams = new HashMap<>();
            userName = list.get(0).getExecutorName();
            List<CuiBanEmailTemplate> toDoItems = new ArrayList<>();
            for (FlowTaskExecutorIdAndCount f : list) {
                CuiBanEmailTemplate template = new CuiBanEmailTemplate();
                template.setFlowName(f.getFlowName());
                template.setTaskName(f.getTaskName());
                template.setTaskCount(f.getCount());
                toDoItems.add(template);
            }

            EcmpMessage message = new EcmpMessage();
            String senderId = userId;
            message.setSenderId(senderId);
            List<String> receiverIds = new ArrayList<>();
            receiverIds.add(userId);
            message.setReceiverIds(receiverIds);
            contentTemplateParams.put("userName", userName);
            contentTemplateParams.put("toDoItems", toDoItems);
            message.setContentTemplateParams(contentTemplateParams);
            message.setContentTemplateCode("EMAIL_TEMPLATE_TODO_WARN");//模板代码

            message.setCanToSender(false);
            INotifyService iNotifyService = ContextUtil.getBean(INotifyService.class);
            message.setSubject("催办提醒");
            List<NotifyType> notifyTypes = new ArrayList<NotifyType>();
            notifyTypes.add(NotifyType.EMAIL);
            message.setNotifyTypes(notifyTypes);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    iNotifyService.send(message);
                    String url = Constants.getUsderEmailAlertUpdateLastTimesUrl();
                    OperateResult result = ApiClient.postViaProxyReturnResult(url, new GenericType<OperateResult>() {
                    }, receiverIds);
                    LogUtil.bizLog("催办send email to userId=" + userId + ",userName = " + userName + "，重置时间状态=" + (result != null ? result.getMessage() : "失败"));
                }
            }).start();
        }


    }

    /**
     * 获取指定用户的待办工作数量
     *
     * @param executorId   执行人用户Id
     * @param searchConfig 查询参数
     * @return 待办工作的数量
     */
    @Override
    public int findCountByExecutorId(String executorId, Search searchConfig) {
        if (searchConfig != null) {
            boolean boo = SqlCommonUtil.isValid(searchConfig.getQuickSearchValue());
            if (!boo) {
                return 0;
            }
        }
        Long count = flowTaskDao.findCountByExecutorId(executorId, searchConfig);
        return count.intValue();
    }

    /**
     * 通过Id获取一个待办任务(设置了办理任务URL)
     *
     * @param taskId 待办任务Id
     * @return 待办任务
     */
    @Override
    public FlowTask findTaskById(String taskId) {
        FlowTask flowTask = flowTaskDao.findTaskById(taskId);
        if (Objects.nonNull(flowTask)) {
            initFlowTask(flowTask);
        }
        return flowTask;
    }


    /**
     * 通过业务单据Id获取待办任务（带url）
     *
     * @param businessId 业务单据id
     * @param boo        是否加载url设置
     * @return 待办任务集合
     */
    public ResponseData findTasksByBusinessId(String businessId, Boolean boo) {
        if (StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }
        List<FlowTask> list = new ArrayList<FlowTask>();
        //通过业务单据id查询没有结束并且没有挂起的流程实例
        List<FlowInstance> flowInstanceList = flowInstanceDao.findNoEndByBusinessIdOrder(businessId);
        if (!CollectionUtils.isEmpty(flowInstanceList)) {
            FlowInstance instance = flowInstanceList.get(0);
            //根据流程实例id查询待办
            List<FlowTask> addList = flowTaskDao.findByInstanceId(instance.getId());
            if (boo) {
                //完成待办任务的URL设置
                this.initFlowTasks(addList);
            }
            list.addAll(addList);
        }
        return ResponseData.operationSuccessWithData(list);
    }


    public ResponseData getExecutorsByRequestExecutorsVoAndOrg(List<RequestExecutorsVo> requestExecutorsVos, String businessId, String orgId) {
        if (CollectionUtils.isEmpty(requestExecutorsVos) || StringUtils.isEmpty(businessId) || StringUtils.isEmpty(orgId)) {
            return ResponseData.operationFailure("10006");
        }
        List<Executor> executors = new ArrayList<>();
        if (requestExecutorsVos.size() == 1) { //流程发起人、指定岗位、指定岗位类别、自定义执行人、任意执行人
            String userType = requestExecutorsVos.get(0).getUserType();
            if ("StartUser".equalsIgnoreCase(userType)) { //流程发起人
                String startUserId = ContextUtil.getSessionUser().getUserId();
                executors = flowCommonUtil.getBasicUserExecutors(Arrays.asList(startUserId));
            } else if ("Position".equalsIgnoreCase(userType)) {//指定岗位
                String ids = requestExecutorsVos.get(0).getIds();
                executors = flowTaskTool.getExecutors(userType, ids, orgId);
            } else if ("PositionType".equalsIgnoreCase(userType)) { //指定岗位类别
                String ids = requestExecutorsVos.get(0).getIds();
                executors = flowTaskTool.getExecutors(userType, ids, orgId);
            } else if ("SelfDefinition".equalsIgnoreCase(userType)) { //自定义执行人
                String selfDefId = requestExecutorsVos.get(0).getIds();
                if (StringUtils.isNotEmpty(selfDefId) && !Constants.NULL_S.equalsIgnoreCase(selfDefId)) {
                    FlowExecutorConfig flowExecutorConfig = flowExecutorConfigDao.findOne(selfDefId);
                    String path = flowExecutorConfig.getUrl();
                    AppModule appModule = flowExecutorConfig.getBusinessModel().getAppModule();
                    String appModuleCode = appModule.getApiBaseAddress();
                    String param = flowExecutorConfig.getParam();
                    FlowInvokeParams flowInvokeParams = new FlowInvokeParams();
                    flowInvokeParams.setId(businessId);
                    flowInvokeParams.setOrgId("" + orgId);
                    flowInvokeParams.setJsonParam(param);
                    executors = flowCommonUtil.getExecutorsBySelfDef(appModuleCode, flowExecutorConfig.getName(), path, flowInvokeParams);
                } else {
                    return ResponseData.operationFailure("10248");
                }
            } else if ("AnyOne".equalsIgnoreCase(userType)) { //任意执行人
                return ResponseData.operationSuccess();
            }
        } else if (requestExecutorsVos.size() > 1) { //岗位+组织维度、岗位+组织维度+自定义执行人、岗位类别+组织机构
            String selfDefId = null; //自定义执行人id
            List<String> positionIds = null;//岗位代码集合
            List<String> orgDimensionCodes = null;//组织维度代码集合
            List<String> orgIds = null; //组织机构id集合
            List<String> positionTypesIds = null;//岗位类别id集合
            for (RequestExecutorsVo executorsVo : requestExecutorsVos) {
                String ids = executorsVo.getIds();
                List<String> tempList = null;
                if (StringUtils.isNotEmpty(ids)) {
                    String[] idsShuZhu = ids.split(",");
                    tempList = Arrays.asList(idsShuZhu);
                }
                if ("SelfDefinition".equalsIgnoreCase(executorsVo.getUserType())) {//通过业务ID获取自定义用户
                    selfDefId = ids;
                } else if ("Position".equalsIgnoreCase(executorsVo.getUserType())) {
                    positionIds = tempList;
                } else if ("OrganizationDimension".equalsIgnoreCase(executorsVo.getUserType())) {
                    orgDimensionCodes = tempList;
                } else if ("PositionType".equalsIgnoreCase(executorsVo.getUserType())) {
                    positionTypesIds = tempList;
                } else if ("Org".equalsIgnoreCase(executorsVo.getUserType())) {
                    orgIds = tempList;
                }
            }

            if (StringUtils.isNotEmpty(selfDefId) && !Constants.NULL_S.equalsIgnoreCase(selfDefId)) {
                FlowExecutorConfig flowExecutorConfig = flowExecutorConfigDao.findOne(selfDefId);
                String path = flowExecutorConfig.getUrl();
                AppModule appModule = flowExecutorConfig.getBusinessModel().getAppModule();
                String appModuleCode = appModule.getApiBaseAddress();
                String param = flowExecutorConfig.getParam();
                FlowInvokeParams flowInvokeParams = new FlowInvokeParams();
                flowInvokeParams.setId(businessId);
                flowInvokeParams.setOrgId("" + orgId);
                flowInvokeParams.setOrganizationIds(orgIds);
                flowInvokeParams.setOrgDimensionCodes(orgDimensionCodes);
                flowInvokeParams.setPositionIds(positionIds);
                flowInvokeParams.setPositionTypeIds(positionTypesIds);
                flowInvokeParams.setJsonParam(param);
                executors = flowCommonUtil.getExecutorsBySelfDef(appModuleCode, flowExecutorConfig.getName(), path, flowInvokeParams);
            } else {
                if (positionTypesIds != null && orgIds != null) {
                    //新增根据（岗位类别+组织机构）获得执行人
                    executors = flowCommonUtil.getExecutorsByPostCatIdsAndOrgs(positionTypesIds, orgIds);
                } else {
                    //通过岗位ids、组织维度ids和组织机构id来获取执行人【岗位+组织维度】
                    executors = flowCommonUtil.getExecutorsByPositionIdsAndorgDimIds(positionIds, orgDimensionCodes, orgId);
                }
            }
        }
        List<Executor> returnExecutors = flowCommonUtil.setListExecutor(executors);
        return ResponseData.operationSuccessWithData(returnExecutors);
    }


    @Override
    public ResponseData getExecutorsByExecutorsVos(FindExecutorsVo findExecutorsVo) {
        List<RequestExecutorsVo> requestExecutorsVoList = null;
        String requestExecutorsVos = findExecutorsVo.getRequestExecutorsVos();
        if (StringUtils.isNotEmpty(requestExecutorsVos)) {
            JSONArray jsonArray = JSONArray.fromObject(requestExecutorsVos);//把String转换为json
            requestExecutorsVoList = (List<RequestExecutorsVo>) JSONArray.toCollection(jsonArray, RequestExecutorsVo.class);
        }
        return this.getExecutorsByRequestExecutorsVo(requestExecutorsVoList, findExecutorsVo.getBusinessModelCode(), findExecutorsVo.getBusinessId());
    }

    @Override
    public ResponseData getExecutorsByVoAndInstanceIdVo(FindExecutorsVo findExecutorsVo) {
        List<RequestExecutorsVo> requestExecutorsVoList = null;
        String requestExecutorsVos = findExecutorsVo.getRequestExecutorsVos();
        if (StringUtils.isNotEmpty(requestExecutorsVos)) {
            JSONArray jsonArray = JSONArray.fromObject(requestExecutorsVos);//把String转换为json
            requestExecutorsVoList = (List<RequestExecutorsVo>) JSONArray.toCollection(jsonArray, RequestExecutorsVo.class);
        }
        return this.getExecutorsByVoAndInstanceId(requestExecutorsVoList, findExecutorsVo.getInstanceId());
    }

    @Override
    public ResponseData getExecutorsByVoAndInstanceId(List<RequestExecutorsVo> requestExecutorsVos, String instanceId) {
        if (StringUtils.isEmpty(instanceId)) {
            return ResponseData.operationFailure("10139");
        }
        FlowInstance flowInstance = flowInstanceDao.findOne(instanceId);
        if (flowInstance == null) {
            return ResponseData.operationFailure("10249");
        }
        String businessId = flowInstance.getBusinessId();
        if (StringUtils.isEmpty(flowInstance.getBusinessOrgId())) {
            String businessModelCode = flowInstance.getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getClassName();
            return getExecutorsByRequestExecutorsVo(requestExecutorsVos, businessModelCode, businessId);
        } else {
            if (CollectionUtils.isEmpty(requestExecutorsVos) || StringUtils.isEmpty(businessId)) {
                return ResponseData.operationFailure("10006");
            }
            return this.getExecutorsByRequestExecutorsVoAndOrg(requestExecutorsVos, businessId, flowInstance.getBusinessOrgId());
        }
    }

    @Override
    public ResponseData getExecutorsByRequestExecutorsVo(List<RequestExecutorsVo> requestExecutorsVos, String businessModelCode, String businessId) {
        if (CollectionUtils.isEmpty(requestExecutorsVos) || StringUtils.isEmpty(businessModelCode) || StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }

        String orgId;
        try {
            BusinessModel businessModel = businessModelDao.findByProperty("className", businessModelCode);
            Map<String, Object> businessV = ExpressionUtil.getPropertiesValuesMap(businessModel, businessId, true);
            orgId = (String) businessV.get(Constants.ORG_ID);
            if (StringUtils.isEmpty(orgId)) {
                return ResponseData.operationFailure("10250");
            }
        } catch (Exception e) {
            LogUtil.error("获取业务单据组织机构失败！", e);
            return ResponseData.operationFailure("10251");
        }

        return this.getExecutorsByRequestExecutorsVoAndOrg(requestExecutorsVos, businessId, orgId);
    }


    public String getOrgIdByFlowTask(FlowTask flowTask) {
        String actInstanceId;
        if (flowTask.getFlowInstance() != null) {
            //流程实例里面添加了发起组织机构的字段，新的数据可以直接获取
            if (StringUtils.isNotEmpty(flowTask.getFlowInstance().getBusinessOrgId())) {
                return flowTask.getFlowInstance().getBusinessOrgId();
            } else {
                actInstanceId = flowTask.getFlowInstance().getActInstanceId();
                Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(actInstanceId);
                return processVariables.get("orgId").getValue() + "";
            }
        } else {
            //从回调进来的参数flowTask.getActTaskId()可能为空（服务任务）
            HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult();
            actInstanceId = currTask.getProcessInstanceId();
            Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(actInstanceId);
            return processVariables.get("orgId").getValue() + "";
        }
    }

    @Override
    public ResponseData getTaskFormUrlXiangDuiByTaskId(String taskId) {
        ResponseData res = new ResponseData();
        if (StringUtils.isNotEmpty(taskId)) {
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (flowTask != null) {
                String webBaseAddressConfig = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getWebBaseAddress();
                String webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                if (StringUtils.isNotEmpty(webBaseAddress)) {
                    String[] tempWebBaseAddress = webBaseAddress.split("/");
                    if (tempWebBaseAddress != null && tempWebBaseAddress.length > 0) {
                        webBaseAddress = tempWebBaseAddress[tempWebBaseAddress.length - 1];
                    }
                }
                WorkPageUrl workPageUrl = flowTask.getWorkPageUrl();
                if (workPageUrl != null) {
                    String taskFormUrlXiangDui = "/" + webBaseAddress + "/" + workPageUrl.getUrl();
                    taskFormUrlXiangDui = taskFormUrlXiangDui.replaceAll("\\//", "/");
                    flowTask.setTaskFormUrlXiangDui(taskFormUrlXiangDui); //处理界面是同一模块
                    String appModuleId = workPageUrl.getAppModuleId();
                    AppModule appModule = appModuleService.findOne(appModuleId);
                    if (appModule != null && !appModule.getId().equals(flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getId())) {
                        webBaseAddressConfig = appModule.getWebBaseAddress();
                        webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                        if (StringUtils.isNotEmpty(webBaseAddress)) {
                            String[] tempWebBaseAddress = webBaseAddress.split("/");
                            if (tempWebBaseAddress != null && tempWebBaseAddress.length > 0) {
                                webBaseAddress = tempWebBaseAddress[tempWebBaseAddress.length - 1];
                            }
                        }
                        taskFormUrlXiangDui = "/" + webBaseAddress + "/" + workPageUrl.getUrl();
                        taskFormUrlXiangDui = taskFormUrlXiangDui.replaceAll("\\//", "/");
                        flowTask.setTaskFormUrlXiangDui(taskFormUrlXiangDui);
                    }
                    res.setData(flowTask.getTaskFormUrlXiangDui());
                } else {
                    return ResponseData.operationFailure("10252");
                }
            } else {
                List<FlowHistory> historylist = flowHistoryService.findListByProperty("oldTaskId", taskId);
                if (!CollectionUtils.isEmpty(historylist)) {
                    FlowHistory history = historylist.get(0);
                    String webBaseAddressConfig = history.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getWebBaseAddress();
                    String webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                    if (StringUtils.isNotEmpty(webBaseAddress)) {
                        String[] tempWebBaseAddress = webBaseAddress.split("/");
                        if (tempWebBaseAddress != null && tempWebBaseAddress.length > 0) {
                            webBaseAddress = tempWebBaseAddress[tempWebBaseAddress.length - 1];
                        }
                    }
                    String lookUrl = history.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getLookUrl();
                    String lookUrlXiangDui = "/" + webBaseAddress + "/" + lookUrl;
                    lookUrlXiangDui = lookUrlXiangDui.replaceAll("\\//", "/");
                    res.setData(lookUrlXiangDui);
                } else {
                    return ResponseData.operationFailure("10253");
                }
            }
        } else {
            return ResponseData.operationFailure("10006");
        }
        return res;
    }


    @Override
    public ResponseData<FlowTaskPageResultVO<FlowTask>> queryCurrentUserFlowTaskByPhone(UserFlowTaskQueryParam queryParam) {
        try {
            FlowTaskPageResultVO<FlowTask> resultVO = queryCurrentUserFlowTask(queryParam);
            return ResponseData.operationSuccessWithData(resultVO);
        } catch (Exception e) {
            LogUtil.error("请求待办失败:{}", e.getMessage(), e);
            return ResponseData.operationFailure("10412", e.getMessage());
        }
    }

    /**
     * 查询当前用户的待办工作
     *
     * @param queryParam 查询参数
     * @return 分页查询结果
     */
    @Override
    public FlowTaskPageResultVO<FlowTask> queryCurrentUserFlowTask(UserFlowTaskQueryParam queryParam) {
        Boolean canBatch = queryParam.getCanBatch();
        String modelId = queryParam.getModelId();
        // 可以批量处理的待办查询结果
        if (canBatch) {
            PageResult<FlowTask> pageResult = findByPageCanBatchApprovalByBusinessModelId(modelId, queryParam);
            FlowTaskPageResultVO<FlowTask> resultVO = new FlowTaskPageResultVO<>();
            resultVO.setRows(pageResult.getRows());
            resultVO.setRecords(pageResult.getRecords());
            resultVO.setPage(pageResult.getPage());
            resultVO.setTotal(pageResult.getTotal());
            resultVO.setAllTotal(pageResult.getRecords());
            return resultVO;
        }
        // 一般待办查询结果
        return findByBusinessModelIdWithAllCount(modelId, queryParam);
    }


    /**
     * 完成待办任务的URL设置
     *
     * @param flowTasks 待办任务清单
     * @return 待办任务
     */
    public void initFlowTasks(List<FlowTask> flowTasks) {
        if (CollectionUtils.isEmpty(flowTasks)) {
            return;
        }
        flowTasks.forEach(this::initFlowTask);
    }


    /**
     * 完成待办任务的URL设置
     *
     * @param flowTask 待办任务
     * @return 待办任务
     */
    private void initFlowTask(FlowTask flowTask) {
        String apiBaseAddressConfig = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getApiBaseAddress();
        String apiBaseAddress = Constants.getConfigValueByApi(apiBaseAddressConfig);
        if (StringUtils.isNotEmpty(apiBaseAddress)) {
            flowTask.setApiBaseAddressAbsolute(apiBaseAddress);
            String apiAddress = Constants.getConfigValueByApi(apiBaseAddressConfig);
            flowTask.setApiBaseAddress(apiAddress);
        }
        String webBaseAddressConfig = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getWebBaseAddress();
        String webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
        String webAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);

        if (StringUtils.isNotEmpty(webBaseAddress)) {
            flowTask.setWebBaseAddressAbsolute(webBaseAddress);
            flowTask.setLookWebBaseAddressAbsolute(webBaseAddress);
            flowTask.setWebBaseAddress(webAddress);
            flowTask.setLookWebBaseAddress(webAddress);
        }
        WorkPageUrl workPageUrl = flowTask.getWorkPageUrl();
        String completeTaskServiceUrl = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getCompleteTaskServiceUrl();
        String businessDetailServiceUrl = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessDetailServiceUrl();
        if (StringUtils.isEmpty(completeTaskServiceUrl)) {
            completeTaskServiceUrl = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getCompleteTaskServiceUrl();
        }
        if (StringUtils.isEmpty(businessDetailServiceUrl)) {
            businessDetailServiceUrl = flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getBusinessDetailServiceUrl();
        }
        flowTask.setCompleteTaskServiceUrl(completeTaskServiceUrl);
        flowTask.setBusinessDetailServiceUrl(businessDetailServiceUrl);
        if (workPageUrl != null) {
            flowTask.setTaskFormUrl(PageUrlUtil.buildUrl(webBaseAddress, workPageUrl.getUrl()));
            String taskFormUrlXiangDui = PageUrlUtil.buildUrl(webAddress, workPageUrl.getUrl());
            flowTask.setTaskFormUrlXiangDui(taskFormUrlXiangDui);
            String appModuleId = workPageUrl.getAppModuleId();
            AppModule appModule = appModuleDao.findOne(appModuleId);
            if (appModule != null && !appModule.getId().equals(flowTask.getFlowInstance().getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getAppModule().getId())) {
                webBaseAddressConfig = appModule.getWebBaseAddress();
                webBaseAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                flowTask.setTaskFormUrl(PageUrlUtil.buildUrl(webBaseAddress, workPageUrl.getUrl()));
                if (StringUtils.isNotEmpty(webBaseAddress)) {
                    flowTask.setWebBaseAddressAbsolute(webBaseAddress);
                    webAddress = Constants.getConfigValueByWeb(webBaseAddressConfig);
                    flowTask.setWebBaseAddress(webAddress);
                }
                taskFormUrlXiangDui = PageUrlUtil.buildUrl(webBaseAddress, workPageUrl.getUrl());
                flowTask.setTaskFormUrlXiangDui(taskFormUrlXiangDui);
            }
        }
    }


    public FlowHistory pollingCheckHis(FlowHistory flowHistory, String hisId) {
        flowHistory = flowHistoryService.findOne(hisId);
        return flowHistory;
    }

    /**
     * 检查非固化流程需要跳过的待办
     *
     * @param taskList 全部待办
     * @return 需要自动执行的待办
     */
    public ResponseData<List<FlowTask>> checkAutomaticToDoTask(List<FlowTask> taskList) {
        List<FlowTask> needLsit = new ArrayList<>();//需要自动跳过的任务
        if (!CollectionUtils.isEmpty(taskList)) {
            for (FlowTask flowTask : taskList) {

                FlowInstance flowInstance = flowTask.getFlowInstance();
                if (flowTask.getExecutorId().equals(flowInstance.getCreatorId())) { //执行人是发起人
                    if (flowSolidifyExecutorService.checkPage(flowTask)) {
                        needLsit.add(flowTask);
                        continue;
                    }
                }

                if (StringUtils.isNotEmpty(flowTask.getPreId())) {
                    //上一节点信息
                    FlowHistory flowHistory = flowHistoryService.findOne(flowTask.getPreId());
                    //监控发现存在此时流程历史还没有持久化的情况（轮询获取）
                    if (flowHistory == null) {
                        Calendar startTreadTime = Calendar.getInstance();
                        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                        Runnable runnable = new Runnable() {
                            public void run() {
                                Calendar nowTime = Calendar.getInstance();
                                nowTime.add(Calendar.SECOND, -30);//不能超过30秒
                                if (nowTime.after(startTreadTime)) {
                                    service.shutdown();
                                }
                                FlowHistory flowHistoryTwo = pollingCheckHis(flowHistory, flowTask.getPreId());
                                if (flowHistoryTwo != null) {
                                    service.shutdown();
                                }
                            }
                        };
                        service.scheduleWithFixedDelay(runnable, 5, 10, TimeUnit.SECONDS);
                    }

                    if (flowHistory != null) {
                        //上一步和当前执行人一致
                        if (StringUtils.isNotEmpty(flowHistory.getExecutorId())
                                && StringUtils.isNotEmpty(flowTask.getExecutorId())
                                && flowHistory.getExecutorId().equals(flowTask.getExecutorId())) {
                            String hisJson = flowHistory.getTaskJsonDef();
                            JSONObject hisJsonObj = JSONObject.fromObject(hisJson);
                            String hisNodeType = hisJsonObj.get("nodeType") + "";
                            //上一步如果是审批节点(并且同意)
                            if ("Approve".equalsIgnoreCase(hisNodeType) && "agree".equalsIgnoreCase(flowHistory.getFlowExecuteStatus())) {
                                String taskJsonDef = flowTask.getTaskJsonDef();
                                JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
                                String nodeType = taskJsonDefObj.get("nodeType") + "";
                                //本节点也是审批节点
                                if ("Approve".equalsIgnoreCase(nodeType)) {
                                    if (flowSolidifyExecutorService.checkPage(flowTask)) {
                                        needLsit.add(flowTask);
                                    }
                                }
                            }
                        }
                    } else {
                        LogUtil.error("检查待办是否需要自动执行失败：流程历史信息还未实例化！");
                    }
                }
            }
        }
        return ResponseData.operationSuccessWithData(needLsit);
    }


    /**
     * 需要自动执行人的待办(审批任务)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseData automaticToDoTask(List<FlowTask> taskList) {
        for (int i = 0; i < taskList.size(); i++) {
            FlowTask task = taskList.get(i);
            String taskJsonDef = task.getTaskJsonDef();
            JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
            String nodeType = taskJsonDefObj.get("nodeType") + "";
            String approved = null; //是否同意
            String opinion = "【自动执行】";
            if ("CounterSign".equalsIgnoreCase(nodeType) || "Approve".equalsIgnoreCase(nodeType)) { //跳过处理默认同意
                approved = "true";
                opinion = "同意【自动执行】";
            }
            ResponseData responseData;
            try {
                //模拟请求下一步数据
                responseData = this.simulationGetNodesInfo(task.getId(), approved);
            } catch (Exception e) {
                LogUtil.error("自动执行待办-模拟请求下一步数据报错：" + e.getMessage(), e);
                return ResponseData.operationFailure("10432", e.getMessage());
            }

            //模拟下一不节点信息成功
            if (responseData.getSuccess()) {
                String taskListString;
                String endEventId = null;
                if ("CounterSignNotEnd".equalsIgnoreCase(responseData.getData().toString())) { //会签未结束
                    taskListString = "[]";
                } else if ("EndEvent".equalsIgnoreCase(responseData.getData().toString())) { //结束节点
                    taskListString = "[]";
                    endEventId = "true";
                } else {
                    List<NodeInfo> nodeInfoList = (List<NodeInfo>) responseData.getData();
                    List<FlowTaskCompleteWebVO> flowTaskCompleteList = new ArrayList<>();
                    for (NodeInfo nodeInfo : nodeInfoList) {
                        FlowTaskCompleteWebVO taskWebVO = new FlowTaskCompleteWebVO();
                        taskWebVO.setNodeId(nodeInfo.getId());
                        taskWebVO.setUserVarName(nodeInfo.getUserVarName());
                        taskWebVO.setFlowTaskType(nodeInfo.getFlowTaskType());
                        //节点类型
                        String flowTaskType = nodeInfo.getFlowTaskType() + "";
                        if (flowTaskType.equalsIgnoreCase("pooltask")) {//工作池任务
                            taskWebVO.setUserIds("");
                        } else if (flowTaskType.equalsIgnoreCase("serviceTask")
                                || flowTaskType.equalsIgnoreCase("receiveTask")) { //服务任务 或 接收任务
                            taskWebVO.setUserIds(ContextUtil.getUserId());
                        } else if (flowTaskType.equalsIgnoreCase("singleSign")
                                || flowTaskType.equalsIgnoreCase("CounterSign")
                                || flowTaskType.equalsIgnoreCase("ParallelTask")
                                || flowTaskType.equalsIgnoreCase("SerialTask")) { //单签、会签、并行、串行
                            List<Executor> set = nodeInfo.getExecutorSet();
                            String userIds = "";
                            if (!CollectionUtils.isEmpty(set)) {
                                for (Executor executor : set) {
                                    if (StringUtils.isEmpty(userIds)) {
                                        userIds += executor.getId();
                                    } else {
                                        userIds += "," + executor.getId();
                                    }
                                }
                                taskWebVO.setUserIds(userIds);
                            } else {
                                taskWebVO.setUserIds("");
                            }
                        } else if (flowTaskType.equalsIgnoreCase("common")
                                || flowTaskType.equalsIgnoreCase("approve")) { //普通任务、审批任务
                            List<Executor> set = nodeInfo.getExecutorSet();
                            if (!CollectionUtils.isEmpty(set)) {
                                taskWebVO.setUserIds(set.iterator().next().getId());
                            }
                        }
                        taskWebVO.setSolidifyFlow(false); //固化
                        taskWebVO.setInstancyStatus(false);//加急
                        flowTaskCompleteList.add(taskWebVO);
                    }
                    JSONArray jsonArray = JSONArray.fromObject(flowTaskCompleteList);
                    taskListString = jsonArray.toString();
                }

                try {
                    //默认3秒后执行，防止和前面节点执行时间一样，在历史里面顺序不定
                    TimeUnit.SECONDS.sleep(3);
                    //自动执行待办
                    defaultFlowBaseService.completeTask(task.getId(), task.getFlowInstance().getBusinessId(),
                            opinion, taskListString,
                            endEventId, null, false, approved, null);
                } catch (Exception e) {
                    LogUtil.error("自动执行待办报错：" + e.getMessage(), e);
                    return ResponseData.operationFailure("10379", e.getMessage());
                }
            } else {
                LogUtil.error("自动执行待办-模拟请求下一步数据报错：" + responseData.getMessage());
                return ResponseData.operationFailure("10432", responseData.getMessage());
            }
        }
        return ResponseData.operationSuccess();
    }


    /**
     * 通过业务单据Id获取待办任务（不带url）
     *
     * @param businessId 业务单据id
     * @return 待办任务集合
     */
    public ResponseData findTasksNoUrlByBusinessId(String businessId) {
        if (StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }
        List<FlowTask> list = new ArrayList<>();
        //通过业务单据id查询没有结束并且没有挂起的流程实例
        List<FlowInstance> flowInstanceList = flowInstanceDao.findNoEndByBusinessIdOrder(businessId);
        if (!CollectionUtils.isEmpty(flowInstanceList)) {
            FlowInstance instance = flowInstanceList.get(0);
            //根据流程实例id查询待办
            List<FlowTask> addList = flowTaskDao.findByInstanceId(instance.getId());
            list.addAll(addList);
        }
        return ResponseData.operationSuccessWithData(list);
    }


    /**
     * 模拟请求下一步数据flow-web/flowClient/getSelectedNodesInfo
     *
     * @param taskId
     * @param approved
     * @return
     */
    public ResponseData simulationGetNodesInfo(String taskId, String approved) throws Exception {
        if (StringUtils.isEmpty(approved)) {
            approved = "true";
        }
        //可能路径
        List<NodeInfo> list = this.findNextNodes(taskId);
        List<NodeInfo> nodeInfoList = null;
        if (!CollectionUtils.isEmpty(list)) {
            if (list.size() == 1) {
                nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
            } else {
                String gateWayName = list.get(0).getGateWayName();
                if (StringUtils.isNotEmpty(gateWayName) && "人工排他网关".equals(gateWayName)) {
                    //人工网关不处理
                    return ResponseData.operationFailure("10217");
                } else {
                    nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
                }
            }
        }

        if (!CollectionUtils.isEmpty(nodeInfoList)) {
            if (nodeInfoList.size() == 1 && "EndEvent".equalsIgnoreCase(nodeInfoList.get(0).getType())) {//只存在结束节点
                return ResponseData.operationSuccessWithData("EndEvent");
            } else if (nodeInfoList.size() == 1 && "CounterSignNotEnd".equalsIgnoreCase(nodeInfoList.get(0).getType())) {
                return ResponseData.operationSuccessWithData("CounterSignNotEnd");
            } else {
                return ResponseData.operationSuccessWithData(nodeInfoList);
            }
        } else {
            LogUtil.error("当前规则找不到符合条件的分支！");
            return ResponseData.operationFailure("10079");
        }

    }


    @Override
    public void pushTheUnpushedTaskToBasicAgain() {
        List<FlowTask> list = this.findAll();
        List<FlowTask> needAddList = new ArrayList<>();
        for (FlowTask bean : list) {
            List<FlowTaskPush> flowTaskPushList = flowTaskPushDao.findListByProperty("flowTaskId", bean.getId());
            if (CollectionUtils.isEmpty(flowTaskPushList)) {
                needAddList.add(bean);
            }
        }
        if (!CollectionUtils.isEmpty(needAddList)) {
            LogUtil.bizLog("-------------------查询出未推送的待办总数：" + needAddList.size() + "个！");
        } else {
            LogUtil.bizLog("-------------------所有的待办都推送了！");
        }
        LogUtil.bizLog("------------------开始重新推送这些待办到BASIC模块！");
        this.pushToBasic(needAddList, null, null, null);
        LogUtil.bizLog("-----------------重新推送待办到BASIC模块已经完成！");
    }


    @Override
    public ResponseData haveReadTaskByTaskId(String taskId) {
        if (StringUtils.isNotEmpty(taskId)) {
            FlowTask flowTask = this.findOne(taskId);
            if (flowTask != null) {
                String defJson = flowTask.getTaskJsonDef();
                JSONObject defObj = JSONObject.fromObject(defJson);
                String nodeType = defObj.getString("nodeType");
                JSONObject normalInfo = defObj.getJSONObject("nodeConfig").getJSONObject("normal");
                if (nodeType.equalsIgnoreCase("ParallelTask")
                        && normalInfo.has("carbonCopyOrReport")
                        && normalInfo.getBoolean("carbonCopyOrReport")) {
                    //已阅模拟请求下一步
                    ResponseData responseNodeInfo;
                    try {
                        //需要单独检查是否后一个节点是结束节点，如果不是，默认失败
                        responseNodeInfo = this.whetherNeedCarbonCopyOrReport(flowTask.getId());
                    } catch (Exception e) {
                        LogUtil.error("已阅失败：模拟下一步失败！", e);
                        return ResponseData.operationFailure("10255");
                    }

                    if (responseNodeInfo.successful()) {
                        String endEventId = null;
                        if ("EndEvent".equalsIgnoreCase(responseNodeInfo.getData().toString())) { //结束节点
                            endEventId = "true";
                        }

                        try {
                            //自动执行待办
                            long currentTime = System.currentTimeMillis();
                            ResponseData responseData = defaultFlowBaseService.completeTask(flowTask.getId(),
                                    flowTask.getFlowInstance().getBusinessId(), "【已阅】", "[]",
                                    endEventId, null, false, null, currentTime);

                            if (responseData.successful()) {
                                return ResponseData.operationSuccess("10256");
                            } else {
                                return ResponseData.operationFailure("10257", responseData.getMessage());
                            }
                        } catch (Exception e) {
                            LogUtil.error("已阅失败：自动执行报错！", e);
                            return ResponseData.operationFailure("10258");
                        }
                    } else {
                        return ResponseData.operationFailure("已阅失败：" + responseNodeInfo.getMessage());
                    }
                } else {
                    if (TaskStatus.VIRTUAL.toString().equals(flowTask.getTaskStatus())) { //虚拟待办
                        return this.completeVirtualTask(flowTask);
                    }
                    return ResponseData.operationFailure("10259");
                }
            } else {
                return ResponseData.operationFailure("10260");
            }
        } else {
            return ResponseData.operationFailure("10261");
        }
    }


    /**
     * 处理虚拟待办
     *
     * @param flowTask
     * @return
     */
    public ResponseData completeVirtualTask(FlowTask flowTask) {
        try {
            List<FlowTask> oldList = new ArrayList<>();
            oldList.add(flowTask);
            FlowHistory flowHistory = flowTaskTool.initVirtualFlowHistory(flowTask);
            flowTaskDao.delete(flowTask);
            flowHistoryDao.save(flowHistory);
            //是否推送信息到baisc
            Boolean pushBasic = this.getBooleanPushTaskToBasic();
            if (pushBasic) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pushToBasic(null, oldList, null, null);
                    }
                }).start();
            }
        } catch (Exception e) {
            LogUtil.error("已阅失败：虚拟任务处理失败！", e);
            return ResponseData.operationFailure("10262");
        }
        return ResponseData.operationSuccess("10263");
    }


    /**
     * 节点跳转
     *
     * @param currentTask  当前任务
     * @param targetNodeId 目标节点ID
     * @param variables    提交参数
     * @return 是否跳转成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public ResponseData jumpToTarget(FlowTask currentTask, String targetNodeId, Map<String, Object> variables, String jumpOpinion) throws Exception {
        // 取得当前任务
        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(currentTask.getActTaskId()).singleResult();
        // 取得流程实例
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(currTask.getProcessInstanceId()).singleResult();
        if (instance == null) {
            return OperateResult.operationFailure("10264");
        }
        // 取得流程定义
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
        if (definition == null) {
            return OperateResult.operationFailure("10265");
        }
        // 取得当前任务节点的活动
        ActivityImpl currentActivity = ((ProcessDefinitionImpl) definition).findActivity(currentTask.getActTaskDefKey());
        ActivityImpl targetActivity = ((ProcessDefinitionImpl) definition).findActivity(targetNodeId);
        //取活动，清除活动方向
        List<PvmTransition> oriPvmTransitionList = new ArrayList<>();
        List<PvmTransition> pvmTransitionList = currentActivity.getOutgoingTransitions();
        for (PvmTransition pvmTransition : pvmTransitionList) {
            oriPvmTransitionList.add(pvmTransition);
        }

        pvmTransitionList.clear();
        //建立新方向
        TransitionImpl newTransition = currentActivity.createOutgoingTransition();
        //取得转向的目标
        newTransition.setDestination(targetActivity);

        try {
            //完成任务
            this.complete(currentTask.getId(), jumpOpinion, variables);
        } catch (Exception e) {
            throw e;
        } finally {
            //恢复方向
            targetActivity.getIncomingTransitions().remove(newTransition);
            List<PvmTransition> pvmTList = currentActivity.getOutgoingTransitions();
            pvmTList.clear();
            for (PvmTransition pvmTransition : oriPvmTransitionList) {
                pvmTransitionList.add(pvmTransition);
            }
            //将状态重置
            runtimeService.removeVariable(instance.getProcessInstanceId(), currentTask.getActTaskDefKey() + "currentNodeAfterEvent");
            runtimeService.removeVariable(instance.getProcessInstanceId(), targetNodeId + "targetNodeBeforeEvent");
        }

        return OperateResult.operationSuccess("10266");
    }


    /**
     * 判断是否需要返回上一步
     *
     * @return
     */
    public Boolean whetherOrNotReturnLastNode(String flowTaskId) {
        FlowTask flowTask = flowTaskDao.findOne(flowTaskId);
        if (flowTask != null) {
            return BooleanUtils.isTrue(flowTask.getJumpBackPrevious());
        }
        return false;
    }


    /**
     * 返回上一个任务
     *
     * @param id
     * @param opinion
     * @param variables
     * @return
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResultWithData<FlowStatus> returnLastTask(String id, String opinion, Map<String, Object> variables) throws Exception {
        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask == null) {
            return OperateResultWithData.operationFailure("10009");
        }
        flowTask.setDepict("【自动返回】" + opinion);
        if (StringUtils.isNotEmpty(flowTask.getPreId())) {
            FlowHistory preFlowTask = flowHistoryDao.findOne(flowTask.getPreId());//上一个任务id
            if (preFlowTask == null) {
                return OperateResultWithData.operationFailure("10267");
            } else {
                return this.returnActivitiNode(flowTask, preFlowTask, variables);
            }
        } else {
            return OperateResultWithData.operationFailure("10023");
        }
    }


    /**
     * 返回上一个任务
     *
     * @param currentTask
     * @param preFlowTask
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public OperateResultWithData returnActivitiNode(FlowTask currentTask, FlowHistory preFlowTask, Map<String, Object> variables) throws Exception {
        OperateResultWithData result = OperateResultWithData.operationSuccess("执行成功！");
        // 取得当前任务
        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(currentTask.getActTaskId()).singleResult();
        // 取得流程实例
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(currTask.getProcessInstanceId()).singleResult();
        if (instance == null) {
            return OperateResultWithData.operationFailure("10268");
        }

        // 取得流程定义
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
        if (definition == null) {
            return OperateResultWithData.operationFailure("10269");
        }

        // 取得当前任务标节点的活动
        ActivityImpl currentActivity = definition.findActivity(currentTask.getActTaskDefKey());
        // 取得上一个目标节点的活动
        ActivityImpl preActivity = definition.findActivity(preFlowTask.getActTaskDefKey());

        //取活动，清除活动方向
        List<PvmTransition> oriPvmTransitionList = new ArrayList<>();
        List<PvmTransition> pvmTransitionList = currentActivity.getOutgoingTransitions();
        for (PvmTransition pvmTransition : pvmTransitionList) {
            oriPvmTransitionList.add(pvmTransition);
        }
        pvmTransitionList.clear();
        //建立新方向
        TransitionImpl newTransition = currentActivity.createOutgoingTransition();
        // 取得转向的目标，这里需要指定用需要回退到的任务节点
        newTransition.setDestination(preActivity);

        //完成任务
        this.complete(currentTask.getId(), currentTask.getDepict(), variables);

        //恢复方向
        preActivity.getIncomingTransitions().remove(newTransition);
        List<PvmTransition> pvmTList = currentActivity.getOutgoingTransitions();
        pvmTList.clear();
        for (PvmTransition pvmTransition : oriPvmTransitionList) {
            pvmTransitionList.add(pvmTransition);
        }
        return result;
    }


    @Override
    public ResponseData automatingTaskByBusinessId(String businessId) {
        return automatingTask(businessId, null, null);
    }

    @Override
    public ResponseData automatingTaskByBusinessIdAndCode(String businessId, String nodeCode) {
        return automatingTask(businessId, nodeCode, null);
    }

    @Override
    public ResponseData automatingTaskByParams(AutoTaskParam autoTaskParam) {
        if (autoTaskParam == null) {
            return ResponseData.operationFailure("10006");
        }
        return automatingTask(autoTaskParam.getBusinessId(), autoTaskParam.getNodeCode(), autoTaskParam.getOpinion());
    }

    public ResponseData automatingTask(String businessId, String nodeCode, String opinion) {
        if (StringUtils.isNotEmpty(businessId)) {
            FlowInstance flowInstance = flowInstanceService.findLastInstanceByBusinessId(businessId);
            if (flowInstance != null && !flowInstance.isEnded()) {//只考虑还在流程中的流程实例
                FlowTask autoTask;
                String nodeType;
                List<FlowTask> allList = this.findByInstanceId(flowInstance.getId());
                List<FlowTask> list = new ArrayList<>();
                List<FlowTask> virtualList = new ArrayList<>();
                if (allList != null) {
                    allList.forEach(task -> {
                        if (TaskStatus.VIRTUAL.toString().equals(task.getTaskStatus())) { //虚拟待办
                            virtualList.add(task);
                        } else {
                            list.add(task);
                        }
                    });
                }

                FlowTask virtualTask = virtualList.stream().filter(task -> task.getExecutorId().equals(flowInstance.getCreatorId())).findFirst().orElse(null);

                if (list.size() == 1) {
                    FlowTask flowTask = list.get(0);
                    String defJson = flowTask.getTaskJsonDef();
                    JSONObject defObj = JSONObject.fromObject(defJson);
                    if (StringUtils.isNotEmpty(nodeCode)) {
                        JSONObject normalInfo = defObj.getJSONObject("nodeConfig").getJSONObject("normal");
                        String code = normalInfo.getString("nodeCode");
                        if (!nodeCode.equalsIgnoreCase(code)) {
                            return ResponseData.operationFailure("10411", code);
                        }
                    }
                    nodeType = defObj.getString("nodeType");
                    if ("Approve".equalsIgnoreCase(nodeType) || "Normal".equalsIgnoreCase(nodeType)
                            || "SingleSign".equalsIgnoreCase(nodeType) || "PoolTask".equalsIgnoreCase(nodeType)) {
                        if ("PoolTask".equalsIgnoreCase(nodeType) && "anonymous".equalsIgnoreCase(flowTask.getExecutorId())) { //工作池任务验证执行人是否已经确认
                            return ResponseData.operationFailure("10417");
                        }
                        autoTask = flowTask;
                    } else {
                        return ResponseData.operationFailure("10380");
                    }
                } else {
                    //可以执行单签任务和会签任务(验证执行人)
                    SessionUser sessionUser = ContextUtil.getSessionUser();
                    String userId = sessionUser.getUserId();
                    FlowTask task;
                    task = list.stream().filter(a -> a.getExecutorId().equals(userId)).findFirst().orElse(null);

                    if (task == null) {  //考虑共同查看模式的转授权
                        TaskMakeOverPower newPower = null;
                        //根据被授权人ID查看所有满足的转授权设置信息（共同查看模式）
                        List<TaskMakeOverPower> overPowerList = taskMakeOverPowerService.findPowerByPowerUser(userId);
                        if (!CollectionUtils.isEmpty(overPowerList)) {
                            String flowTypeId = flowInstance.getFlowDefVersion().getFlowDefination().getFlowType().getId();
                            for (int i = 0; i < overPowerList.size(); i++) {
                                TaskMakeOverPower bean = overPowerList.get(i);
                                if (StringUtils.isNotEmpty(bean.getFlowTypeId()) && bean.getFlowTypeId().equals(flowTypeId)) { //流程类型
                                    newPower = bean;
                                }
                            }
                        }
                        if (newPower != null) { //表示有共同查看模式的转授权情况
                            String powerUserId = newPower.getUserId();
                            task = list.stream().filter(a -> a.getExecutorId().equals(powerUserId)).findFirst().orElse(null);
                        }
                    }

                    if (task != null) {
                        String defJson = task.getTaskJsonDef();
                        JSONObject defObj = JSONObject.fromObject(defJson);
                        if (StringUtils.isNotEmpty(nodeCode)) {
                            JSONObject normalInfo = defObj.getJSONObject("nodeConfig").getJSONObject("normal");
                            String code = normalInfo.getString("nodeCode");
                            if (!nodeCode.equalsIgnoreCase(code)) {
                                return ResponseData.operationFailure("10411", code);
                            }
                        }
                        nodeType = defObj.getString("nodeType");
                        if ("SingleSign".equalsIgnoreCase(nodeType) || "CounterSign".equalsIgnoreCase(nodeType)) {
                            autoTask = task;
                        } else {
                            return ResponseData.operationFailure("10380");
                        }
                    } else {
                        return ResponseData.operationFailure("10381");
                    }
                }


                //自动执行这个待办
                String approved;
                if ("Normal".equalsIgnoreCase(nodeType) || "SingleSign".equalsIgnoreCase(nodeType) || "PoolTask".equalsIgnoreCase(nodeType)) {
                    approved = null;
                    if (StringUtils.isEmpty(opinion)) {
                        opinion = "【自动执行】";
                    } else {
                        opinion += "【自动执行】";
                    }
                } else {
                    approved = "true";
                    if (StringUtils.isEmpty(opinion)) {
                        opinion = "同意【自动执行】";
                    } else {
                        opinion += "【自动执行】";
                    }
                }
                //模拟请求下一步，如果需要人为选择的情况，返回false
                ResponseData responseData;
                try {
                    responseData = this.simulationNodesInfo(autoTask.getId(), approved);
                } catch (Exception e) {
                    //模拟下一步失败，将待办设置为紧急
                    autoTask.setPriority(3);
                    this.save(autoTask);
                    return ResponseData.operationFailure("10376");
                }

                if (responseData.getSuccess()) {
                    String taskListString;
                    String endEventId = null;
                    boolean solidi = autoTask.getFlowInstance().getFlowDefVersion().getSolidifyFlow();
                    if ("CounterSignNotEnd".equalsIgnoreCase(responseData.getData().toString())) { //会签未结束
                        taskListString = "[]";
                    } else if ("EndEvent".equalsIgnoreCase(responseData.getData().toString())) { //结束节点
                        taskListString = "[]";
                        endEventId = "true";
                    } else {
                        List<NodeInfo> nodeInfoList = (List<NodeInfo>) responseData.getData();
                        List<FlowTaskCompleteWebVO> flowTaskCompleteList = new ArrayList<>();
                        for (NodeInfo nodeInfo : nodeInfoList) {
                            FlowTaskCompleteWebVO taskWebVO = new FlowTaskCompleteWebVO();
                            taskWebVO.setNodeId(nodeInfo.getId());
                            taskWebVO.setUserVarName(nodeInfo.getUserVarName());
                            taskWebVO.setFlowTaskType(nodeInfo.getFlowTaskType());
                            taskWebVO.setSolidifyFlow(solidi);
                            taskWebVO.setCallActivityPath(null);
                            if (nodeInfo.getExecutorSet() != null && nodeInfo.getExecutorSet().size() == 1) {
                                taskWebVO.setUserIds(nodeInfo.getExecutorSet().iterator().next().getId());
                            } else if ("poolTask".equalsIgnoreCase(nodeInfo.getFlowTaskType())) {
                                taskWebVO.setUserIds(null);
                            } else if ("serviceTask".equalsIgnoreCase(nodeInfo.getFlowTaskType()) || "receiveTask".equalsIgnoreCase(nodeInfo.getFlowTaskType())) {
                                taskWebVO.setUserIds(ContextUtil.getUserId());
                            } else {
                                if (solidi) {
                                    taskWebVO.setUserIds("");
                                } else {
                                    List<Executor> set = nodeInfo.getExecutorSet();
                                    String userIds = "";
                                    for (Executor executor : set) {
                                        if (StringUtils.isEmpty(userIds)) {
                                            userIds += executor.getId();
                                        } else {
                                            userIds += "," + executor.getId();
                                        }
                                    }
                                    taskWebVO.setUserIds(userIds);
                                }
                            }
                            flowTaskCompleteList.add(taskWebVO);
                        }
                        JSONArray jsonArray = JSONArray.fromObject(flowTaskCompleteList);
                        taskListString = jsonArray.toString();
                    }

                    try {
                        //自动执行待办
                        long currentTime = System.currentTimeMillis();
                        ResponseData response = defaultFlowBaseService.completeTask(autoTask.getId(), businessId,
                                opinion, taskListString, endEventId, null, false,
                                approved, currentTime);

                        if (response.successful()) {
                            if (virtualTask != null) { //同时执行发起人的通知
                                haveReadTaskByTaskId(virtualTask.getId());
                            }
                            return ResponseData.operationSuccess("10378");
                        } else {
                            return ResponseData.operationFailure("10379", response.getMessage());
                        }
                    } catch (Exception e) {
                        LogUtil.error("批量上传自动执行待办报错：" + e.getMessage(), e);
                        //处理下一步失败，将待办设置为紧急
                        autoTask.setPriority(3);
                        this.save(autoTask);
                        return ResponseData.operationFailure("10379", e.getMessage());
                    }
                } else {
                    //模拟下一步失败，将待办设置为紧急
                    autoTask.setPriority(3);
                    this.save(autoTask);
                    return responseData;
                }
            } else {
                return ResponseData.operationFailure("10382");
            }
        } else {
            return ResponseData.operationFailure("10383");
        }
    }


    /**
     * 模拟请求下一步数据：参考simulationGetSelectedNodesInfo
     *
     * @param taskId
     * @return
     */
    public ResponseData simulationNodesInfo(String taskId, String approved) {
        try {
            //所有可能路径（人工网关默认选择一条路径）
            List<NodeInfo> list = this.findNextNodes(taskId);
            List<NodeInfo> nodeInfoList = null;
            if (!CollectionUtils.isEmpty(list) && list.size() == 1) {
                nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
            } else if (!CollectionUtils.isEmpty(list)) {
                String gateWayName = list.get(0).getGateWayName();
                if (StringUtils.isNotEmpty(gateWayName) && "人工排他网关".equals(gateWayName)) {
                    List<String> nodeOnle = new ArrayList<>();
                    nodeOnle.add(list.get(0).getId());
                    nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, nodeOnle);
                } else {
                    nodeInfoList = this.findNexNodesWithUserSet(taskId, approved, null);
                }
            }

            if (!CollectionUtils.isEmpty(nodeInfoList)) {
                if (nodeInfoList.size() == 1 && "EndEvent".equalsIgnoreCase(nodeInfoList.get(0).getType())) {//只存在结束节点
                    return ResponseData.operationSuccessWithData("EndEvent");
                } else if (nodeInfoList.size() == 1 && "CounterSignNotEnd".equalsIgnoreCase(nodeInfoList.get(0).getType())) {
                    return ResponseData.operationSuccessWithData("CounterSignNotEnd");
                } else {
                    return ResponseData.operationSuccessWithData(nodeInfoList);
                }
            } else if (nodeInfoList == null) {
                return ResponseData.operationFailure("任务不存在，可能已经被处理！");
            } else {
                return ResponseData.operationFailure("当前表单规则找不到符合条件的分支！");
            }

        } catch (Exception e) {
            LogUtil.error("模拟请求下一步错误[simulationNodesInfo]：" + e.getMessage(), e);
            return ResponseData.operationFailure("模拟请求下一步错误[simulationNodesInfo]：" + e.getMessage());
        }
    }


    @Override
    public ResponseData<List<FlowNodeVO>> getCanReturnNodeInfos(String taskId) {
        if (StringUtils.isNotEmpty(taskId)) {
            FlowTask flowTask = flowTaskDao.findOne(taskId);
            if (flowTask != null) {
                List<FlowNodeVO> returnList = new ArrayList<>();
                List<FlowHistory> historyList = flowHistoryService.findByInstanceId(flowTask.getFlowInstance().getId());
                String defJson = flowTask.getFlowInstance().getFlowDefVersion().getDefJson();
                if (defJson.contains("ParallelGateway") || defJson.contains("InclusiveGateway")) {
                    boolean newFlow = defJson.contains("flowNewPosition");
                    if (newFlow) {
                        JSONObject defObj = JSONObject.fromObject(defJson);
                        JSONObject nodesObj = defObj.getJSONObject("process").getJSONObject("nodes");
                        JSONObject nodeInfo = nodesObj.getJSONObject(flowTask.getActTaskDefKey());
                        String curPosition = nodeInfo.getString("flowNewPosition");
                        for (FlowHistory history : historyList) {
                            String hisJson = history.getTaskJsonDef();
                            JSONObject hisNodeObj = JSONObject.fromObject(hisJson);
                            String hisPosition = hisNodeObj.getString("flowNewPosition");
                            if (curPosition.equals(hisPosition)) {   //同一分支的节点
                                String actTaskDefKey = history.getActTaskDefKey();
                                //历史的人工节点(非本节点)
                                if (actTaskDefKey.contains("UserTask") && !actTaskDefKey.equals(flowTask.getActTaskDefKey())) {
                                    if (CollectionUtils.isEmpty(returnList)) {
                                        returnList.add(new FlowNodeVO(actTaskDefKey, history.getFlowTaskName(), ""));
                                    } else {
                                        FlowNodeVO nodeVO = returnList.stream().filter(node -> node.getId().equals(actTaskDefKey)).findFirst().orElse(null);
                                        if (nodeVO == null) {
                                            returnList.add(new FlowNodeVO(actTaskDefKey, history.getFlowTaskName(), ""));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        //当前流程不支持包含并行网关或包容网关的退回操作，需要重新发布流程定义已检查规则！
                        return ResponseData.operationFailure("10404");
                    }
                } else {
                    if (!CollectionUtils.isEmpty(historyList)) {
                        for (FlowHistory history : historyList) {
                            String actTaskDefKey = history.getActTaskDefKey();
                            //历史的人工节点(非本节点)
                            if (actTaskDefKey.contains("UserTask") && !actTaskDefKey.equals(flowTask.getActTaskDefKey())) {
                                if (CollectionUtils.isEmpty(returnList)) {
                                    returnList.add(new FlowNodeVO(actTaskDefKey, history.getFlowTaskName(), ""));
                                } else {
                                    FlowNodeVO nodeVO = returnList.stream().filter(node -> node.getId().equals(actTaskDefKey)).findFirst().orElse(null);
                                    if (nodeVO == null) {
                                        returnList.add(new FlowNodeVO(actTaskDefKey, history.getFlowTaskName(), ""));
                                    }
                                }
                            }
                        }
                    }
                }
                return ResponseData.operationSuccessWithData(returnList);
            } else {
                //任务不存在，可能已经被处理!
                return ResponseData.operationFailure("10033");
            }
        } else {
            //请求参数不能为空！
            return ResponseData.operationFailure("10006");
        }
    }


    @Override
    public ResponseData returnToSpecifiedNode(ReturnToSpecifiedNode params) {
        if (params == null) {
            //请求参数不能为空！
            return ResponseData.operationFailure("10006");
        }
        String nodeId = params.getNodeId();
        if (StringUtils.isEmpty(nodeId)) {
            //需要退回的节点ID不能为空！
            return ResponseData.operationFailure("10391");
        }
        String taskId = params.getTaskId();
        if (StringUtils.isEmpty(taskId)) {
            //参数任务ID不能为空！
            return ResponseData.operationFailure("10392");
        }
        String opinion = params.getOpinion();
        if (StringUtils.isEmpty(opinion)) {
            //参数退回原因不能为空！
            return ResponseData.operationFailure("10393");
        }
        boolean allowJumpBack = false;
        if (BooleanUtils.isTrue(params.getAllowJumpBack())) {
            allowJumpBack = true;
        }

        try {
            ResponseData responseData = this.returnToNode(taskId, allowJumpBack, opinion, nodeId);
            if (responseData.successful()) {
                String flowInstanceId = (String) responseData.getData();
                new Thread(new Runnable() {//重置固化执行表顺序
                    @Override
                    public void run() {
                        flowSolidifyExecutorService.setSolidifyExecutorTaskOrder(flowInstanceId);
                    }
                }).start();
            }
            return responseData;
        } catch (Exception e) {
            LogUtil.error("退回失败:" + e.getMessage(), e);
            return ResponseData.operationFailure("10396", e.getMessage());
        }
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public ResponseData returnToNode(String taskId, boolean allowJumpBack, String opinion, String nodeId) throws Exception {
        FlowTask flowTask = flowTaskDao.findOne(taskId);

        if (flowTask.getTrustState() != null) {
            if (flowTask.getTrustState() == 1) {
                //当前任务已委托出去，等待委托方处理后才能处理！
                return OperateResultWithData.operationFailure("10199");
            } else if (flowTask.getTrustState() == 2) {
                //退回失败：委托任务不能直接进行退回操作，需要先返回委托方进行处理！
                return OperateResultWithData.operationFailure("10397");
            }
        }

        String taskJsonDef = flowTask.getTaskJsonDef();
        JSONObject taskJsonDefObj = JSONObject.fromObject(taskJsonDef);
        JSONObject normalInfo = taskJsonDefObj.getJSONObject("nodeConfig").getJSONObject("normal");
        String nodeType = taskJsonDefObj.get("nodeType") + "";

        // 取得当前任务
        HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(flowTask.getActTaskId()).singleResult();

        if ("CounterSign".equalsIgnoreCase(nodeType)) {//会签任务做处理判断
            String executionId = currTask.getExecutionId();
            Map<String, VariableInstance> processVariables = runtimeService.getVariableInstances(executionId);
            //总循环次数
            Integer instanceOfNumbers = (Integer) processVariables.get("nrOfInstances").getValue();
            //并串行会签（false为并行：并行将已生成的待办转已办，串行在最后人审批中记录）
            Boolean isSequential = normalInfo.getBoolean("isSequential");
            //清除其他会签执行人
            flowTaskTool.counterSignImmediatelyEnd(flowTask, flowTask.getFlowInstance(), null, isSequential, executionId, instanceOfNumbers);
        }


        // 取得流程实例
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(currTask.getProcessInstanceId()).singleResult();
        if (instance == null) {
            //退回失败：流程实例不存在！
            return OperateResult.operationFailure("10394");
        }
        // 取得流程定义
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(currTask.getProcessDefinitionId());
        if (definition == null) {
            //退回失败：流程定义获取失败！
            return OperateResult.operationFailure("10395");
        }

        // 取得当前任务节点的活动
        ActivityImpl currentActivity = ((ProcessDefinitionImpl) definition).findActivity(flowTask.getActTaskDefKey());
        ActivityImpl targetActivity = ((ProcessDefinitionImpl) definition).findActivity(nodeId);
        //取活动，清除活动方向
        List<PvmTransition> oriPvmTransitionList = new ArrayList<>();
        List<PvmTransition> pvmTransitionList = currentActivity.getOutgoingTransitions();
        for (PvmTransition pvmTransition : pvmTransitionList) {
            oriPvmTransitionList.add(pvmTransition);
        }
        pvmTransitionList.clear();
        //建立新方向
        TransitionImpl newTransition = currentActivity.createOutgoingTransition();
        //取得转向的目标
        newTransition.setDestination(targetActivity);

        Map<String, Object> variables = new HashMap<>();


        try {
            variables.put("return", 1); //退回
            variables.put("allowJumpBack", allowJumpBack); //是否处理后返回我审批
            variables.put("approved", null); //清空审批参数
            //完成任务
            this.complete(flowTask.getId(), opinion, variables);
        } catch (Exception e) {
            throw e;
        } finally {
            //恢复方向
            targetActivity.getIncomingTransitions().remove(newTransition);
            List<PvmTransition> pvmTList = currentActivity.getOutgoingTransitions();
            pvmTList.clear();
            for (PvmTransition pvmTransition : oriPvmTransitionList) {
                pvmTransitionList.add(pvmTransition);
            }
            runtimeService.removeVariable(instance.getProcessInstanceId(), "return");//将状态重置
            runtimeService.removeVariable(instance.getProcessInstanceId(), "allowJumpBack");//将状态重置
        }
        return ResponseData.operationSuccessWithData(flowTask.getFlowInstance().getId());
    }


    @Override
    public ResponseData labelTaskReasonByTaskId(LabelTaskReasonVo params) {
        if (params == null) {
            return ResponseData.operationFailure("10006");
        }
        String id = params.getTaskId();
        if (StringUtils.isEmpty(id)) {
            return ResponseData.operationFailure("10377");
        }
        String reason = params.getLabelReason();
        if (StringUtils.isEmpty(reason)) {
            return ResponseData.operationFailure("10418");
        }

        FlowTask flowTask = flowTaskDao.findOne(id);
        if (flowTask != null) {
            flowTask.setPriority(4);
            flowTask.setLabelReason(reason);
            flowTaskDao.save(flowTask);
            return ResponseData.operationSuccess();
        } else {
            return ResponseData.operationFailure("10419");
        }
    }
}

