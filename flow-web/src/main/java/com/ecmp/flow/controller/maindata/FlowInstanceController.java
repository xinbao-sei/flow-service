package com.ecmp.flow.controller.maindata;

import com.ecmp.annotation.IgnoreCheckAuth;
import com.ecmp.config.util.ApiClient;
import com.ecmp.context.ContextUtil;
import com.ecmp.core.json.JsonUtil;
import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.core.search.SearchFilter;
import com.ecmp.core.search.SearchUtil;
import com.ecmp.core.vo.OperateStatus;
import com.ecmp.flow.api.*;
import com.ecmp.flow.entity.*;
import com.ecmp.flow.vo.MyBillVO;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletRequest;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * *************************************************************************************************
 * <br>
 * 实现功能：
 * <br>
 * ------------------------------------------------------------------------------------------------
 * <br>
 * 版本          变更时间             变更人                     变更原因
 * <br>
 * ------------------------------------------------------------------------------------------------
 * <br>
 * 1.0.00      2017/4/26 9:32      詹耀(xxxlimit)                    新建
 * <br>
 * *************************************************************************************************<br>
 */
@Controller
@RequestMapping(value = "/flowInstance")
@IgnoreCheckAuth
public class FlowInstanceController {

    @RequestMapping(value = "show", method = RequestMethod.GET)
    public String show() {
        return "maindata/FlowInstanceView";
    }

    /**
     * 查询流程实例
     * @param request
     * @return 流程实例分页清单
     * @throws JsonProcessingException
     * @throws ParseException
     */
    @RequestMapping(value = "listFlowInstance")
    @ResponseBody
    public String listFlowInstance(ServletRequest request) throws JsonProcessingException, ParseException {
        Search search = SearchUtil.genSearch(request);
        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
        PageResult<FlowInstance> flowInstancePageResult = proxy.findByPage(search);
        return JsonUtil.serialize(flowInstancePageResult,JsonUtil.DATE_TIME);
    }

    /**
     * 根据id删除流程实例
     * @param id
     * @return 操作结果
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public String delete(String id) {
        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
        OperateResult result = proxy.delete(id);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        return JsonUtil.serialize(operateStatus);
    }

    /**
     * 查询流程历史
     * @param request
     * @return  流程历史分页清单
     * @throws JsonProcessingException
     * @throws ParseException
     */
    @RequestMapping(value = "listFlowHistory")
    @ResponseBody
    public String listFlowHistory(ServletRequest request) throws JsonProcessingException, ParseException {
        Search search = SearchUtil.genSearch(request);
        IFlowHistoryService proxy = ApiClient.createProxy(IFlowHistoryService.class);
        PageResult<FlowHistory> flowHistoryPageResult = proxy.findByPage(search);
        return JsonUtil.serialize(flowHistoryPageResult,JsonUtil.DATE_TIME);
    }

    /**
     * 查询流程定义版本
     * @return 操作结果
     */
    @RequestMapping(value = "listAllFlowDefVersion")
    @ResponseBody
    public String listAllFlowDefVersion() {
        IFlowDefVersionService proxy = ApiClient.createProxy(IFlowDefVersionService.class);
        List<FlowDefVersion> flowDefVersions = proxy.findAll();
        OperateStatus operateStatus = new OperateStatus(true, OperateStatus.COMMON_SUCCESS_MSG, flowDefVersions);
        return JsonUtil.serialize(operateStatus);
    }


    /**
     * 获取我的单据（已办/待办）
     * @return
     */
    @RequestMapping(value = "getMyBills")
    @ResponseBody
    public String getMyBills(ServletRequest request)  throws JsonProcessingException, ParseException{
        String creatorId = ContextUtil.getUserId();
        Search search = SearchUtil.genSearch(request);
        SearchFilter searchFilterCreatorId = new SearchFilter("creatorId",creatorId, SearchFilter.Operator.EQ);
        search.addFilter(searchFilterCreatorId);
        //根据业务单据名称、业务单据号、业务工作说明快速查询
        search.addQuickSearchProperty("businessName");
        search.addQuickSearchProperty("businessCode");
        search.addQuickSearchProperty("businessModelRemark");
        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
        PageResult<FlowInstance> flowInstancePageResult = proxy.findByPage(search);
        List<FlowInstance>  flowInstanceList = flowInstancePageResult.getRows();
        PageResult<MyBillVO> results  = new PageResult<MyBillVO>();
        ArrayList<MyBillVO> data=new ArrayList<MyBillVO>();
        if(flowInstanceList!=null && !flowInstanceList.isEmpty()){
            List<String> flowInstanceIds = new ArrayList<String>();
            for(FlowInstance f:flowInstanceList){
                FlowInstance parent = f.getParent();
                if(parent!=null){
                    flowInstancePageResult.setRecords( flowInstancePageResult.getRecords()-1);
                    //flowInstancePageResult.setTotal( flowInstancePageResult.getRecords()-1);
                    //flowInstancePageResult.setPage(flowInstancePageResult.getPage()-1);
                    continue;
                }
                flowInstanceIds.add(f.getId());
                MyBillVO  myBillVO = new MyBillVO();
                myBillVO.setBusinessCode(f.getBusinessCode());
                myBillVO.setBusinessId(f.getBusinessId());
                myBillVO.setBusinessModelRemark(f.getBusinessModelRemark());
                myBillVO.setBusinessName(f.getBusinessName());
                myBillVO.setCreatedDate(f.getCreatedDate());
                myBillVO.setCreatorAccount(f.getCreatorAccount());
                myBillVO.setCreatorName(f.getCreatorName());
                myBillVO.setCreatorId(f.getCreatorId());
                myBillVO.setFlowName(f.getFlowName());
                myBillVO.setLookUrl(f.getFlowDefVersion().getFlowDefination().getFlowType().getBusinessModel().getLookUrl());
                myBillVO.setEndDate(f.getEndDate());
                myBillVO.setFlowInstanceId(f.getId());
//                Boolean canEnd = proxy.checkCanEnd(f.getId());
//                myBillVO.setCanManuallyEnd(canEnd);
                data.add(myBillVO);
            }

            List<Boolean> canEnds = proxy.checkIdsCanEnd(flowInstanceIds);
            if(canEnds!=null && !canEnds.isEmpty()){
                for(int i=0;i<canEnds.size();i++){
                    data.get(i).setCanManuallyEnd(canEnds.get(i));
                }
            }
        }
        results.setRows(data);
        results.setRecords(flowInstancePageResult.getRecords());
        results.setPage(flowInstancePageResult.getPage());
        results.setTotal(flowInstancePageResult.getTotal());
        return JsonUtil.serialize(results,JsonUtil.DATE_TIME);
    }

    /**
     * 根据id终止流程实例,用于待办任务上直接终止流程实例
     * @param id
     * @return 操作结果
     */
    @RequestMapping(value = "endFlowInstance")
    @ResponseBody
    public String endFlowInstance(String id) {
        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
        OperateResult result = proxy.end(id);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        return JsonUtil.serialize(operateStatus);
    }
    /**
     * 根据单据业务id终止流程实例，用于我的待办单据
     * @param businessId
     * @return 操作结果
     */
    @RequestMapping(value = "endFlowInstanceByBusinessId")
    @ResponseBody
    public String endFlowInstanceByBusinessId(String businessId) {
        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
        OperateResult result = proxy.endByBusinessId(businessId);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        return JsonUtil.serialize(operateStatus);
    }

    /**
     * 根据业务实体id查询流程类型
     * @param businessModelId
     * @return
     * @throws JsonProcessingException
     * @throws ParseException
     */
    @RequestMapping(value = "listFlowTypeByBusinessModelId")
    @ResponseBody
    public String listFlowTypeByBusinessModelId(String businessModelId) throws JsonProcessingException, ParseException {
        IFlowTypeService proxy = ApiClient.createProxy(IFlowTypeService.class);
        List<FlowType> flowTypeList = proxy.findByBusinessModelId(businessModelId);
        OperateStatus operateStatus = new OperateStatus(true, OperateStatus.COMMON_SUCCESS_MSG, flowTypeList);
        return JsonUtil.serialize(operateStatus);
    }
}
