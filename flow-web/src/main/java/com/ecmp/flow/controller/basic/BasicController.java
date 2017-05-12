package com.ecmp.flow.controller.basic;

import com.ecmp.basic.api.IOrganizationService;
import com.ecmp.basic.api.IPositionCategoryService;
import com.ecmp.basic.api.IPositionService;
import com.ecmp.basic.entity.Organization;
import com.ecmp.basic.entity.Position;
import com.ecmp.basic.entity.PositionCategory;
import com.ecmp.config.util.ApiClient;
import com.ecmp.core.json.JsonUtil;
import com.ecmp.core.vo.OperateStatus;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.OperateResultWithData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
@RequestMapping(value = "/basic")
public class BasicController {


    /**
     * 获取所有的组织机构
     * @return 所有组织机构树
     */
    @ResponseBody
    @RequestMapping("findAllOrgs")
    public List<Organization> findAllOrgs(){
        IOrganizationService proxy = ApiClient.createProxy(IOrganizationService.class);
        List<Organization> allOrgsList = proxy.findAllOrgs();
        return allOrgsList;
    }

    /**
     * 获取所有的岗位类别
     * @return 所有岗位类别清单
     */
    @ResponseBody
    @RequestMapping("findAllPositionCategory")
    public List<PositionCategory> findAllPositionCategory(){
        IPositionCategoryService proxy = ApiClient.createProxy(IPositionCategoryService.class);
        List<PositionCategory> positionCategoryList = proxy.findAll();
        return positionCategoryList;
    }

    /**
     * 获取所有的岗位
     * @return 所有岗位清单
     */
    @ResponseBody
    @RequestMapping("findAllPosition")
    public List<Position> findAllPosition(){
        IPositionService proxy = ApiClient.createProxy(IPositionService.class);
        List<Position> positionList = proxy.findAll();
        return positionList;
    }

}
