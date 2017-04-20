package com.ecmp.flow.maindata;

import com.ecmp.config.util.ApiClient;
import com.ecmp.core.json.JsonUtil;
import com.ecmp.core.vo.OperateStatus;
import com.ecmp.flow.api.IAppModuleService;
import com.ecmp.flow.api.IBusinessModelService;
import com.ecmp.flow.api.IWorkPageUrlService;
import com.ecmp.flow.entity.AppModule;
import com.ecmp.flow.entity.BusinessModel;
import com.ecmp.flow.entity.WorkPageUrl;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：
 * 业务实体模型控制器
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/3/30 16:10      陈飞(Vision.Mac)            新建
 * <p/>
 * *************************************************************************************************
 */
@Controller
@RequestMapping(value = "/maindata/workPageUrl")
public class WorkPageUrlController {

    @RequestMapping()
    public String showWorkPageUrl() {
        return "maindata/WorkPageUrlView";
    }

    @RequestMapping(value = "find")
    @ResponseBody
    public Object find() throws JsonProcessingException {
        IWorkPageUrlService proxy = ApiClient.createProxy(IWorkPageUrlService.class);
        List<WorkPageUrl> workPageUrlList = proxy.findAll();
        for (int i = 0; i < workPageUrlList.size(); i++) {
            System.out.println(workPageUrlList.get(i));
        }
        String workPageUrl = JsonUtil.serialize(workPageUrlList);
        return workPageUrl;
    }

    @RequestMapping(value = "delete")
    @ResponseBody
    public String delete(String id) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println(id);
        IWorkPageUrlService proxy = ApiClient.createProxy(IWorkPageUrlService.class);
        OperateResult result = proxy.delete(id);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        String delSuccess = JsonUtil.serialize(operateStatus);
        return delSuccess;
    }

    @RequestMapping(value = "findAllAppModuleName")
    @ResponseBody
    public String findAllAppModuleName() throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        IAppModuleService proxy = ApiClient.createProxy(IAppModuleService.class);
        List<AppModule> appModuleList = proxy.findAll();

        OperateStatus operateStatus = new OperateStatus(true, "ok", appModuleList);
        operateStatus.setData(appModuleList);
        String findAppModuleName = JsonUtil.serialize(operateStatus);
        System.out.println(findAppModuleName);
        return findAppModuleName;
    }

    @RequestMapping(value = "update")
    @ResponseBody
    public String update(WorkPageUrl workPageUrl) throws JsonProcessingException {
        System.out.println("---------------------------------------------");
        System.out.println(workPageUrl);
        IWorkPageUrlService proxy = ApiClient.createProxy(IWorkPageUrlService.class);
        OperateResultWithData<WorkPageUrl> result = proxy.save(workPageUrl);
        OperateStatus operateStatus = new OperateStatus(result.successful(), result.getMessage());
        String updateSuccess = JsonUtil.serialize(operateStatus);
        return updateSuccess;
    }
}
