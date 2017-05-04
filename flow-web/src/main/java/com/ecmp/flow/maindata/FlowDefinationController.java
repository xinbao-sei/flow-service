package com.ecmp.flow.maindata;

import com.ecmp.basic.entity.Organization;
import com.ecmp.config.util.ApiClient;
import com.ecmp.core.json.JsonUtil;
import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.core.search.SearchUtil;
import com.ecmp.core.vo.OperateStatus;
import com.ecmp.flow.api.IBusinessModelService;
import com.ecmp.flow.api.IFlowDefVersionService;
import com.ecmp.flow.api.IFlowDefinationService;
import com.ecmp.flow.api.IFlowTypeService;
import com.ecmp.flow.basic.JsonTree;
import com.ecmp.flow.entity.BusinessModel;
import com.ecmp.flow.entity.FlowDefVersion;
import com.ecmp.flow.entity.FlowDefination;
import com.ecmp.flow.entity.FlowType;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ecmp.basic.api.IOrganizationService;
import javax.servlet.ServletRequest;
import javax.ws.rs.QueryParam;
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
@RequestMapping(value = "/maindata/flowDefination")
public class FlowDefinationController {

    @RequestMapping(value = "show", method = RequestMethod.GET)
    public String show() {
        return "maindata/FlowDefinationView";
    }


    /**
     * 通过租户代码获取所有组织机构
     *
     * @return 组织机构清单
     */
    @ResponseBody
    @RequestMapping("findOrgTreeByTenantCode")
    public JsonTree findOrgTreeByTenantCode(@QueryParam("tenantCode") String tenantCode) {
        IOrganizationService proxy = ApiClient.createProxy(IOrganizationService.class);
        Organization result = proxy.findOrgTreeByTenantCode(tenantCode);
        return new JsonTree(result, true);
    }

    @RequestMapping(value = "find")
    @ResponseBody
    public String find(ServletRequest request) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        Search search = SearchUtil.genSearch(request);
        IFlowDefinationService proxy = ApiClient.createProxy(IFlowDefinationService.class);
        PageResult<FlowDefination> flowDefinationPageResult = proxy.findByPage(search);
        String flowDefination = JsonUtil.serialize(flowDefinationPageResult);
        return flowDefination;
    }


    @RequestMapping(value = "delete")
    @ResponseBody
    public String delete(String id) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println("delete--------------" + id);
        IFlowDefinationService proxy = ApiClient.createProxy(IFlowDefinationService.class);
        OperateResult result = proxy.delete(id);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        String delSuccess = JsonUtil.serialize(operateStatus);
        return delSuccess;
    }

    @RequestMapping(value = "findAllFlowType")
    @ResponseBody
    public String findAllFlowType() throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        IFlowTypeService proxy = ApiClient.createProxy(IFlowTypeService.class);
        List<FlowType> flowTypeList = proxy.findAll();
        OperateStatus operateStatus = new OperateStatus(true, "ok", flowTypeList);
        String findFlowTypeName = JsonUtil.serialize(operateStatus);
        return findFlowTypeName;
    }

    @RequestMapping(value = "update")
    @ResponseBody
    public String update(FlowDefination flowDefination) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println(flowDefination);

        IFlowDefinationService proxy = ApiClient.createProxy(IFlowDefinationService.class);
        OperateResultWithData<FlowDefination> result = proxy.save(flowDefination);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage(),result.getData());
        String updateSuccess = JsonUtil.serialize(operateStatus);
        return updateSuccess;
    }

    @RequestMapping(value = "findDefVersion")
    @ResponseBody
    public String findDefVersion(ServletRequest request) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
      //  System.out.println(id);
        Search search = SearchUtil.genSearch(request);
        IFlowDefVersionService proxy = ApiClient.createProxy(IFlowDefVersionService.class);
        PageResult<FlowDefVersion> flowDefVersionPageResult = proxy.findByPage(search);
        String flowDefinationVersion = JsonUtil.serialize(flowDefVersionPageResult);
        return flowDefinationVersion;
    }

    @RequestMapping(value = "updateDefVersion")
    @ResponseBody
    public String updateDefVersion(FlowDefVersion flowDefVersion) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println(flowDefVersion);

        IFlowDefVersionService proxy = ApiClient.createProxy(IFlowDefVersionService.class);
        OperateResultWithData<FlowDefVersion> result = proxy.save(flowDefVersion);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage(),result.getData());
        String updateSuccess = JsonUtil.serialize(operateStatus);
        return updateSuccess;
    }


    @RequestMapping(value = "deleteDefVieson")
    @ResponseBody
    public String deleteDefVieson(String id) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println("delete--------------" + id);
        IFlowDefVersionService proxy = ApiClient.createProxy(IFlowDefVersionService.class);
        OperateResult result = proxy.delete(id);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        String delSuccess = JsonUtil.serialize(operateStatus);
        return delSuccess;
    }

}
