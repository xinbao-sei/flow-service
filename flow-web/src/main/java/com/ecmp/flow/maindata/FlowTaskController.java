package com.ecmp.flow.maindata;

import com.ecmp.config.util.ApiClient;
import com.ecmp.core.json.JsonUtil;
import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.core.search.SearchUtil;
import com.ecmp.core.vo.OperateStatus;
import com.ecmp.flow.api.IFlowTaskService;
import com.ecmp.flow.entity.FlowTask;
import com.ecmp.vo.OperateResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletRequest;
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
@RequestMapping(value = "/maindata/flowTask")
public class FlowTaskController {

    @RequestMapping(value = "show", method = RequestMethod.GET)
    public String show() {
        return "maindata/FlowTaskView";
    }

    @RequestMapping(value = "find")
    @ResponseBody
    public String find(ServletRequest request) throws JsonProcessingException {
       System.out.println("---------------------------------------------");
        Search search = SearchUtil.genSearch(request);
        IFlowTaskService proxy = ApiClient.createProxy(IFlowTaskService.class);
        PageResult<FlowTask> flowTaskPageResult = proxy.findByPage(search);
        String flowTask =  JsonUtil.serialize(flowTaskPageResult);
        return flowTask;
    }

    @RequestMapping(value = "completeTask")
    @ResponseBody
    public String completeTask(String id)  {
        System.out.println("---------------------------------------------");
        System.out.println(id);
        IFlowTaskService proxy = ApiClient.createProxy(IFlowTaskService.class);
        OperateResult result = proxy.complete(id, null);
        OperateStatus status=new OperateStatus(result.successful(),result.getMessage());
        return JsonUtil.serialize(status);
    }


//    @RequestMapping(value = "delete")
//    @ResponseBody
//    public String delete(String id) throws JsonProcessingException {
//        System.out.println("---------------------------------------------");
//        System.out.println("delete--------------"+id);
//        OperateStatus status = new OperateStatus();
//        ObjectMapper objectMapper = new ObjectMapper();
//        IFlowTaskService proxy = ApiClient.createProxy(IFlowTaskService.class);
//        proxy.delete(id);
//        String delSuccess = objectMapper.writeValueAsString(status);
//        return delSuccess;
//    }
//
//    @RequestMapping(value = "findAllFlowInstanceName")
//    @ResponseBody
//    public String findAllFlowInstanceName() throws JsonProcessingException {
//        System.out.println("---------------------------------------------");
//        OperateStatus status = new OperateStatus();
//        ObjectMapper objectMapper = new ObjectMapper();
//        IFlowInstanceService proxy = ApiClient.createProxy(IFlowInstanceService.class);
//        List<FlowInstance> FlowInstanceList = proxy.findAll();
//        status.setData(FlowInstanceList);
//        String findFlowInstanceName = objectMapper.writeValueAsString(status);
//        return findFlowInstanceName;
//    }
//
//
//    @RequestMapping(value = "update")
//    @ResponseBody
//    public String update(FlowTask flowTask) throws JsonProcessingException {
//        System.out.println("---------------------------------------------");
//        System.out.println(flowTask);
//        OperateStatus status = new OperateStatus();
//        status.setSuccess(true);
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        IFlowTaskService proxy = ApiClient.createProxy(IFlowTaskService.class);
//        proxy.save(flowTask);
//        String updateSuccess = objectMapper.writeValueAsString(status);
//        return updateSuccess;
//    }
}
