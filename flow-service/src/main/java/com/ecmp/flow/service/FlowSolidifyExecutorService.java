package com.ecmp.flow.service;

import com.ecmp.context.ContextUtil;
import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.core.search.*;
import com.ecmp.core.service.BaseEntityService;
import com.ecmp.enums.UserAuthorityPolicy;
import com.ecmp.flow.api.IFlowSolidifyExecutorService;
import com.ecmp.flow.basic.vo.Executor;
import com.ecmp.flow.dao.FlowSolidifyExecutorDao;
import com.ecmp.flow.entity.FlowInstance;
import com.ecmp.flow.entity.FlowSolidifyExecutor;
import com.ecmp.flow.entity.FlowTask;
import com.ecmp.flow.util.FlowCommonUtil;
import com.ecmp.flow.vo.*;
import com.ecmp.flow.vo.bpmn.Definition;
import com.ecmp.log.util.LogUtil;
import com.ecmp.vo.ResponseData;
import com.ecmp.vo.SessionUser;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class FlowSolidifyExecutorService extends BaseEntityService<FlowSolidifyExecutor> implements IFlowSolidifyExecutorService {

    @Autowired
    private FlowSolidifyExecutorDao flowSolidifyExecutorDao;

    @Autowired
    private FlowTaskService flowTaskService;

    @Autowired
    private DefaultFlowBaseService defaultFlowBaseService;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private FlowCommonUtil flowCommonUtil;

    protected BaseEntityDao<FlowSolidifyExecutor> getDao() {
        return this.flowSolidifyExecutorDao;
    }


    /**
     * 为节点设置已选择的固化执行人信息
     *
     * @param nodeInfoList
     * @param businessId
     * @return
     */
    public List<NodeInfo> setNodeExecutorByBusinessId(List<NodeInfo> nodeInfoList, String businessId) {
        if (StringUtils.isNotEmpty(businessId)) {
            List<FlowSolidifyExecutor> solidifylist = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
            if (!CollectionUtils.isEmpty(solidifylist)) {
                nodeInfoList.forEach(nodeInfo -> {
                    FlowSolidifyExecutor bean = solidifylist.stream().filter(a -> a.getActTaskDefKey().equalsIgnoreCase(nodeInfo.getId())).findFirst().orElse(null);
                    if (bean != null) {
                        String userIds = bean.getExecutorIds();
                        String[] idArray = userIds.split(",");
                        List<String> userList = Arrays.asList(idArray);
                        List<Executor> executorList = flowCommonUtil.getBasicUserExecutors(userList);
                        List<Executor> returnExecutors = flowCommonUtil.setListExecutor(executorList);
                        nodeInfo.setExecutorSet(returnExecutors);
                    }
                });
            }
        }
        return nodeInfoList;
    }


    public ResponseData getExecuteInfoByBusinessId(String businessId) {
        if (StringUtils.isNotEmpty(businessId)) {
            FlowInstance flowInstance = flowInstanceService.findLastInstanceByBusinessId(businessId);
            if (flowInstance == null) {
                return ResponseData.operationFailure("10173");
            }
            List<FlowSolidifyExecutor> solidifylist = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
            if (solidifylist == null || solidifylist.size() == 0) {
                return ResponseData.operationFailure("10174");
            }
            Definition definitionP = flowCommonUtil.flowDefinition(flowInstance.getFlowDefVersion());
            List<NodeAndExecutes> list = new ArrayList<NodeAndExecutes>();
            for (FlowSolidifyExecutor bean : solidifylist) {
                String currNodeId = bean.getActTaskDefKey();
                JSONObject currentNode = definitionP.getProcess().getNodes().getJSONObject(currNodeId);
                //节点信息
                FlowNodeVO flowNodeVO = new FlowNodeVO();
                flowNodeVO.setId(currentNode.getString("id"));
                flowNodeVO.setType(currentNode.getString("type"));
                flowNodeVO.setNodeType(currentNode.getString("nodeType"));
                flowNodeVO.setName(currentNode.getString("name"));
                //执行人信息
                String userIds = bean.getExecutorIds();
                String[] idArray = userIds.split(",");
                List<String> userList = Arrays.asList(idArray);
                List<Executor> executorList = flowCommonUtil.getBasicUserExecutors(userList);
                //节点和执行人汇总
                NodeAndExecutes nodeAndExecutes = new NodeAndExecutes(flowNodeVO, executorList);
                list.add(nodeAndExecutes);
            }
            return ResponseData.operationSuccessWithData(list);
        } else {
            return ResponseData.operationFailure("10006");
        }
    }


    /**
     * 通过VO集合保存固化流程的执行人信息
     *
     * @return
     */
    @Transactional
    public ResponseData saveSolidifyInfoByExecutorVos(FindSolidifyExecutorVO findSolidifyExecutorVO) {
        List<FlowSolidifyExecutorVO> solidifyExecutorVOList = null;
        String executorsVos = findSolidifyExecutorVO.getExecutorsVos();
        if (StringUtils.isNotEmpty(executorsVos)) {
            JSONArray jsonArray = JSONArray.fromObject(executorsVos);//把String 转 换 为 json
            solidifyExecutorVOList = (List<FlowSolidifyExecutorVO>) JSONArray.toCollection(jsonArray, FlowSolidifyExecutorVO.class);
        }
        return this.saveByExecutorVoList(solidifyExecutorVOList, findSolidifyExecutorVO.getBusinessModelCode(), findSolidifyExecutorVO.getBusinessId());
    }


    /**
     * 通过执行人VO集合保存固化流程的执行人信息
     *
     * @param executorVoList    固化执行人VO集合
     * @param businessModelCode
     * @param businessId
     * @return
     */
    @Transactional
    public ResponseData saveByExecutorVoList(List<FlowSolidifyExecutorVO> executorVoList, String businessModelCode, String businessId) {
        if (CollectionUtils.isEmpty(executorVoList) || StringUtils.isEmpty(businessModelCode) || StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }
        //新启动流程时，清除以前的数据
        List<FlowSolidifyExecutor> list = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(bean -> flowSolidifyExecutorDao.delete(bean));
        }

        try {
            executorVoList.forEach(executorVo -> {
                FlowSolidifyExecutor bean = new FlowSolidifyExecutor();
                bean.setBusinessCode(businessModelCode);
                bean.setBusinessId(businessId);
                bean.setActTaskDefKey(executorVo.getActTaskDefKey());
                bean.setNodeType(executorVo.getNodeType());
                bean.setInstancyStatus(executorVo.getInstancyStatus());
                String userIds = executorVo.getExecutorIds();
                String[] idArray = userIds.split(",");
                StringBuilder executorIds = null;
                for (String id : idArray) {
                    if (!id.equals("undefined") && !id.equals("null")) {
                        if (executorIds == null) {
                            executorIds = new StringBuilder();
                            executorIds.append(id);
                        } else {
                            executorIds.append("," + id);
                        }
                    }
                }
                bean.setExecutorIds(executorIds.toString());
                flowSolidifyExecutorDao.save(bean);
            });
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            return ResponseData.operationFailure("10175");
        }
        return ResponseData.operationSuccess();
    }

    /**
     * 给FlowTaskCompleteWebVO设置执行人和紧急状态
     *
     * @param list       FlowTaskCompleteWebVO集合
     * @param businessId 业务表单id
     * @return
     */
    public ResponseData setInstancyAndIdsByTaskList(List<FlowTaskCompleteWebVO> list, String businessId) {
        if (list == null || list.size() == 0 || StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }

        for (FlowTaskCompleteWebVO webvO : list) {
            //工作池任务
            if ("pooltask".equalsIgnoreCase(webvO.getFlowTaskType())) {
                webvO.setInstancyStatus(false);
                webvO.setUserIds(null);
            } else if ("serviceTask".equalsIgnoreCase(webvO.getFlowTaskType())) { //服务任务
                webvO.setInstancyStatus(false);
                webvO.setUserIds(ContextUtil.getUserId());
            } else if ("receiveTask".equalsIgnoreCase(webvO.getFlowTaskType())) { //接收任务
                webvO.setInstancyStatus(false);
                webvO.setUserIds(ContextUtil.getUserId());
            } else {
                Search search = new Search();
                search.addFilter(new SearchFilter("businessId", businessId));
                search.addFilter(new SearchFilter("actTaskDefKey", webvO.getNodeId()));
                List<FlowSolidifyExecutor> solidifyExecutorlist = flowSolidifyExecutorDao.findByFilters(search);
                if (solidifyExecutorlist == null || solidifyExecutorlist.size() == 0) {
                    return ResponseData.operationFailure("10176");
                }
                webvO.setInstancyStatus(solidifyExecutorlist.get(0).getInstancyStatus());
                webvO.setUserIds(solidifyExecutorlist.get(0).getExecutorIds());
            }
        }
        return ResponseData.operationSuccessWithData(list);
    }


    /**
     * 通过BusinessId删除固化流程执行人列表
     */
    @Transactional
    public ResponseData deleteByBusinessId(String businessId) {
        if (StringUtils.isEmpty(businessId)) {
            return ResponseData.operationFailure("10006");
        }
        List<FlowSolidifyExecutor> list = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
        if (list != null) {
            flowSolidifyExecutorDao.deleteByBusinessId(businessId);
        }
        return ResponseData.operationSuccess();
    }


    /**
     * 记录固化逻辑执行顺序
     */
    public void manageSolidifyFlowByBusinessIdAndTaskKey(String businessId, FlowTask task) {
        if (StringUtils.isNotEmpty(businessId) && StringUtils.isNotEmpty(task.getActTaskDefKey())) {
            String taskKey = task.getActTaskDefKey();
            String taskUserId = task.getExecutorId();
            List<FlowSolidifyExecutor> list = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
            if (!CollectionUtils.isEmpty(list)) {
                int maxInt = list.stream().mapToInt(FlowSolidifyExecutor::getTaskOrder).max().getAsInt();
                FlowSolidifyExecutor bean = list.stream().filter(a -> taskKey.equalsIgnoreCase(a.getActTaskDefKey())).findFirst().orElse(null);
                if (bean != null && bean.getExecutorIds().contains(taskUserId)) { //转办和委托（实际执行人不是启动时设置的人的情况）
                    if ("SingleSign".equalsIgnoreCase(bean.getNodeType())) {  //单签任务设置实际执行人
                        bean.setTrueExecutorIds(task.getExecutorId());
                    }
                    bean.setTaskOrder(maxInt + 1);
                    flowSolidifyExecutorDao.save(bean);
                }
            }
        }
    }


    /**
     * 检查页面配置是否允许自动执行
     *
     * @param task
     * @return
     */
    public boolean checkPage(FlowTask task) {
        boolean canMobile = task.getCanMobile() == null ? false : task.getCanMobile();
        if (BooleanUtils.isTrue(canMobile)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 重置固化执行表顺序
     *
     * @param flowInstanceId
     * @return
     */
    @Transactional
    public void setSolidifyExecutorTaskOrder(String flowInstanceId) {
        if (StringUtils.isNotEmpty(flowInstanceId)) {
            FlowInstance flowInstance = flowInstanceService.findOne(flowInstanceId);
            if (flowInstance != null) {
                if (flowInstance.getFlowDefVersion().getSolidifyFlow()) {  //只判断固化流程
                    List<FlowTask> checkList = flowTaskService.findByInstanceId(flowInstance.getId());
                    if (!CollectionUtils.isEmpty(checkList)) {
                        List<FlowSolidifyExecutor> solidifylist = flowSolidifyExecutorDao.findListByProperty("businessId", flowInstance.getBusinessId());
                        if (!CollectionUtils.isEmpty(solidifylist)) {
                            //检查固化需要跳过的待办
                            ResponseData solidifyResult = this.checkAutomaticToDoSolidifyTask(solidifylist, checkList, true);
                            if (!solidifyResult.successful()) {
                                LogUtil.error("重置固化执行表顺序失败：" + solidifyResult.getMessage());
                            }
                        }
                    } else {
                        LogUtil.error("重置固化执行表顺序-查询待办失败！实例ID=" + flowInstanceId);
                    }
                }
            } else {
                LogUtil.error("重置固化执行表顺序-查询流程实例失败！实例ID=" + flowInstanceId);
            }
        } else {
            LogUtil.error("重置固化执行表顺序-参数为空！");
        }
    }


    /**
     * 固化流程中自动执行待办
     *
     * @param businessId
     * @return
     */
    @Transactional
    public ResponseData selfMotionExecuteTask(String businessId) {
        if (StringUtils.isNotEmpty(businessId)) {
            //根据业务id查询待办
            ResponseData responseData = flowTaskService.findTasksNoUrlByBusinessId(businessId);
            if (responseData.getSuccess()) {
                List<FlowTask> checkList = (List<FlowTask>) responseData.getData();
                List<FlowSolidifyExecutor> solidifylist = flowSolidifyExecutorDao.findListByProperty("businessId", businessId);
                if (!CollectionUtils.isEmpty(solidifylist)) {
                    //检查固化需要跳过的待办
                    ResponseData solidifyResult = this.checkAutomaticToDoSolidifyTask(solidifylist, checkList, true);
                    if (solidifyResult.successful()) {
                        List<FlowTask> needLsit = (List<FlowTask>) solidifyResult.getData();
                        if (!CollectionUtils.isEmpty(needLsit)) {
                            //执行需要自动跳过的任务
                            return this.needSelfMotionTaskList(needLsit, solidifylist);
                        } else {
                            return ResponseData.operationFailure("10428");
                        }
                    } else {
                        return ResponseData.operationFailure("10431", solidifyResult.getMessage());
                    }
                } else {
                    //检查非固化需要跳过的待办
                    ResponseData needTaskResult = flowTaskService.checkAutomaticToDoTask(checkList);
                    if (needTaskResult.successful()) {
                        List<FlowTask> needLsit = (List<FlowTask>) needTaskResult.getData();
                        if (!CollectionUtils.isEmpty(needLsit)) {
                            //执行需要自动跳过的任务
                            return flowTaskService.automaticToDoTask(needLsit);
                        } else {
                            return ResponseData.operationFailure("10428");
                        }
                    } else {
                        return ResponseData.operationFailure("10431", needTaskResult.getMessage());
                    }
                }
            } else {
                LogUtil.error("自动执行待办-查询待办失败！");
                return ResponseData.operationFailure("10429");
            }
        } else {
            LogUtil.error("自动执行待办-参数为空！");
            return ResponseData.operationFailure("10430");
        }
    }

    /**
     * 检查固化流程需要跳过的待办
     *
     * @param solidifylist 固化配置信息
     * @param taskList     全部待办信息
     * @param boo          是否要修改数据
     * @return 需要自动执行的待办
     */
    public ResponseData<List<FlowTask>> checkAutomaticToDoSolidifyTask(List<FlowSolidifyExecutor> solidifylist, List<FlowTask> taskList, Boolean boo) {
        List<FlowTask> needLsit = new ArrayList<>();//需要自动跳过的任务
        if (!CollectionUtils.isEmpty(taskList)) {
            for (FlowTask flowTask : taskList) {
                FlowSolidifyExecutor bean = solidifylist.stream().
                        filter(a -> a.getActTaskDefKey().equalsIgnoreCase(flowTask.getActTaskDefKey())).findFirst().orElse(null);
                //说明是可以自动跳过的任务（普通、单签、会签、审批、并行、串行）
                if (bean != null) {
                    if (bean.getTaskOrder() == 0) { //该节点未执行过
                        //检查已经执行过的任务中，待执行人相同的数据
                        List<FlowSolidifyExecutor> executeList = solidifylist.stream().
                                filter(a -> (a.getTaskOrder() > 0 && a.getExecutorIds().contains(flowTask.getExecutorId()))).collect(Collectors.toList());
                        if (!CollectionUtils.isEmpty(executeList)) {
                            //检查相同数据中有多少是单签（单签待办很多，实际执行人只有一个）
                            List<FlowSolidifyExecutor> singleSignList = executeList.stream().
                                    filter(a -> a.getNodeType().equalsIgnoreCase("SingleSign")).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(singleSignList)) {
                                if (executeList.size() != singleSignList.size()) {
                                    //执行过的不全是单签，该任务需要自动跳过
                                    if (this.checkPage(flowTask)) {
                                        needLsit.add(flowTask);
                                    }
                                } else {
                                    //单签里面实际执行人中有没有当前任务的执行人
                                    List<FlowSolidifyExecutor> trueExecuteList = singleSignList.stream().
                                            filter(a -> (a.getTrueExecutorIds() != null && a.getTrueExecutorIds().contains(flowTask.getExecutorId()))).collect(Collectors.toList());
                                    if (!CollectionUtils.isEmpty(trueExecuteList)) {
                                        //单签实际执行人有当前的执行人，该任务需要自动跳过
                                        if (this.checkPage(flowTask)) {
                                            needLsit.add(flowTask);
                                        }
                                    }
                                }
                            } else {
                                //执行过的没有单签，该任务需要自动跳过
                                if (this.checkPage(flowTask)) {
                                    needLsit.add(flowTask);
                                }
                            }
                        } else { //还没有相同执行人的情况，判断执行人是否是流程发起人
                            FlowInstance flowInstance = flowTask.getFlowInstance();
                            if (flowTask.getExecutorId().equals(flowInstance.getCreatorId())) {
                                if (this.checkPage(flowTask)) {
                                    needLsit.add(flowTask);
                                }
                            }
                        }
                    } else {//该节点已经执行过(执行过的节点，回到该节点，无论前后都不自动执行)
                        if (boo) {
                            List<FlowSolidifyExecutor> updateList = solidifylist.stream().
                                    filter(a -> a.getTaskOrder() >= bean.getTaskOrder()).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(updateList)) {
                                updateList.forEach(a -> {
                                    a.setTrueExecutorIds(null);
                                    a.setTaskOrder(0);
                                    flowSolidifyExecutorDao.save(a);
                                });
                                return ResponseData.operationSuccessWithData(needLsit);
                            }
                        }
                    }
                }
            }
        }
        return ResponseData.operationSuccessWithData(needLsit);
    }


    /**
     * 需要自动执行的待办任务
     *
     * @param taskList
     * @param solidifylist
     */
    @Transactional
    public ResponseData needSelfMotionTaskList(List<FlowTask> taskList, List<FlowSolidifyExecutor> solidifylist) {
        if (!CollectionUtils.isEmpty(taskList) && !CollectionUtils.isEmpty(solidifylist)) {
            for (FlowTask task : taskList) {
                FlowSolidifyExecutor bean = solidifylist.stream().filter(a -> a.getActTaskDefKey().equalsIgnoreCase(task.getActTaskDefKey())).findFirst().orElse(null);
                if (bean != null) {
                    String approved = null; //是否同意
                    String opinion = "【自动执行】";
                    if ("CounterSign".equalsIgnoreCase(bean.getNodeType()) || "Approve".equalsIgnoreCase(bean.getNodeType())) { //跳过处理默认同意
                        approved = "true";
                        opinion = "同意【自动执行】";
                    }
                    ResponseData responseData;
                    try {
                        //模拟请求下一步数据
                        responseData = this.simulationGetSelectedNodesInfo(task.getId(), approved);
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
                            nodeInfoList.forEach(nodeInfo -> {
                                FlowTaskCompleteWebVO taskWebVO = new FlowTaskCompleteWebVO();
                                taskWebVO.setNodeId(nodeInfo.getId());
                                taskWebVO.setUserVarName(nodeInfo.getUserVarName());
                                taskWebVO.setFlowTaskType(nodeInfo.getFlowTaskType());
                                taskWebVO.setUserIds("");
                                taskWebVO.setSolidifyFlow(true); //固化
                                flowTaskCompleteList.add(taskWebVO);
                            });
                            JSONArray jsonArray = JSONArray.fromObject(flowTaskCompleteList);
                            taskListString = jsonArray.toString();
                        }


                        try {
                            //默认3秒后执行，防止和前面节点执行时间一样，在历史里面顺序不定
                            TimeUnit.SECONDS.sleep(3);
                            //自动执行待办
                            long currentTime = System.currentTimeMillis();
                            defaultFlowBaseService.completeTask(task.getId(), bean.getBusinessId(),
                                    opinion, taskListString,
                                    endEventId, null, false, approved, currentTime);
                        } catch (Exception e) {
                            LogUtil.error("自动执行待办报错：" + e.getMessage(), e);
                            return ResponseData.operationFailure("10379", e.getMessage());
                        }

                    }else{
                        LogUtil.error("自动执行待办-模拟请求下一步数据报错：" + responseData.getMessage());
                        return ResponseData.operationFailure("10432", responseData.getMessage());
                    }
                }
            }
        }
        return ResponseData.operationSuccess();
    }


    /**
     * 模拟请求下一步数据flow-web/flowClient/getSelectedNodesInfo
     */
    public ResponseData simulationGetSelectedNodesInfo(String taskId, String approved) throws Exception {
        if (StringUtils.isEmpty(approved)) {
            approved = "true";
        }
        //可能路径（人工网关默认选择一条路径）
        List<NodeInfo> list = flowTaskService.findNextNodes(taskId);
        List<NodeInfo> nodeInfoList = null;
        if (!CollectionUtils.isEmpty(list)) {
            if (list.size() == 1) {
                nodeInfoList = flowTaskService.findNexNodesWithUserSet(taskId, approved, null);
            } else {
                String gateWayName = list.get(0).getGateWayName();
                if (StringUtils.isNotEmpty(gateWayName) && "人工排他网关".equals(gateWayName)) {
                    List<String> nodeOnle = new ArrayList<>();
                    nodeOnle.add(list.get(0).getId());
                    nodeInfoList = flowTaskService.findNexNodesWithUserSet(taskId, approved, nodeOnle);
                } else {
                    nodeInfoList = flowTaskService.findNexNodesWithUserSet(taskId, approved, null);
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
        } else if (nodeInfoList == null) {
            return ResponseData.operationFailure("10033");
        } else {
            return ResponseData.operationFailure("10180");
        }
    }


    /**
     * 查询执行人列表中有该用户的固化数据
     *
     * @param userId
     * @return
     */
    public List<FlowSolidifyExecutor> getFlowSolidifyExecutorInfoByUserId(String userId) {

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(1);
        pageInfo.setRows(1000);

        List<SearchFilter> filters = new ArrayList<>();
        SearchFilter filter = new SearchFilter("executorIds", userId, SearchFilter.Operator.LK);
        filters.add(filter);

        List<SearchOrder> sortOrders = new ArrayList<>();
        SearchOrder searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.DESC);
        sortOrders.add(searchOrder);

        Search search = new Search(filters, sortOrders, pageInfo);

        PageResult<FlowSolidifyExecutor> pageResult = flowSolidifyExecutorDao.findByPage(search);

        return pageResult.getRows();
    }


    @Override
    public ResponseData replaceSolidifyExecutorByVo(ReplaceSolidifyExecutorVo replaceSolidifyExecutorVo) {
        if (replaceSolidifyExecutorVo == null) {
            return ResponseData.operationFailure("10006");
        }
        String oldUserId = replaceSolidifyExecutorVo.getOldUserId();
        if (StringUtils.isEmpty(oldUserId)) {
            return ResponseData.operationFailure("10407");
        }
        String newUserId = replaceSolidifyExecutorVo.getNewUserId();
        if (StringUtils.isEmpty(newUserId)) {
            return ResponseData.operationFailure("10408");
        }
        List<String> businessIdList = replaceSolidifyExecutorVo.getBusinessIdList();
        if (CollectionUtils.isEmpty(businessIdList)) {
            return ResponseData.operationFailure("10409");
        }
        List<FlowSolidifyExecutor> list = this.getFlowSolidifyExecutorInfoByUserIdAndBusinessIdList(oldUserId, businessIdList);
        if (CollectionUtils.isEmpty(list)) {
            return ResponseData.operationFailure("10410");
        }
        list.forEach(bean -> {
            bean.setExecutorIds(bean.getExecutorIds().replace(oldUserId, newUserId));
        });
        flowSolidifyExecutorDao.save(list);
        return ResponseData.operationSuccess();
    }


    /**
     * 根据执行人和业务ID查询满足条件的固化配置
     *
     * @param userId
     * @return
     */
    public List<FlowSolidifyExecutor> getFlowSolidifyExecutorInfoByUserIdAndBusinessIdList(String userId, List<String> businessIdList) {

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPage(1);
        pageInfo.setRows(1000);

        List<SearchFilter> filters = new ArrayList<>();
        SearchFilter filter = new SearchFilter("executorIds", userId, SearchFilter.Operator.LK);
        filters.add(filter);
        SearchFilter filter2 = new SearchFilter("businessId", businessIdList, SearchFilter.Operator.IN);
        filters.add(filter2);


        List<SearchOrder> sortOrders = new ArrayList<>();
        SearchOrder searchOrder = new SearchOrder("createdDate", SearchOrder.Direction.DESC);
        sortOrders.add(searchOrder);

        Search search = new Search(filters, sortOrders, pageInfo);

        PageResult<FlowSolidifyExecutor> pageResult = flowSolidifyExecutorDao.findByPage(search);

        return pageResult.getRows();
    }

}
