package com.ecmp.flow.api;

import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.flow.api.common.api.IBaseService;
import com.ecmp.flow.entity.BusinessSelfDefEmployee;
import com.ecmp.vo.OperateResultWithData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * *************************************************************************************************
 * <p>
 * 实现功能：业务实体自定义执行人配置管理服务API接口定义
 * </p>
 * <p>
 * ------------------------------------------------------------------------------------------------
 * </p>
 * <p>
 * 版本          变更时间             变更人                     变更原因
 * </p>
 * <p>
 * ------------------------------------------------------------------------------------------------
 * </p>
 * <p>
 * 1.0.00      2017/3/31 11:39      谭军(tanjun)                新建
 * </p>
 * *************************************************************************************************
 */
@Path("businessSelfDefEmployee")
@Api(value = "IBusinessSelfDefEmployeeService 业务实体自定义执行人配置管理服务API接口")
public interface IBusinessSelfDefEmployeeService extends IBaseService<BusinessSelfDefEmployee, String> {

    /**
     * 保存一个实体
     * @param businessSelfDefEmployee 实体
     * @return 保存后的实体
     */
    @POST
    @Path("save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "保存实体",notes = "测试 保存实体")
    OperateResultWithData<BusinessSelfDefEmployee> save(BusinessSelfDefEmployee businessSelfDefEmployee);

    /**
     * 获取分页数据
     *
     * @return 实体清单
     */
    @POST
    @Path("findByPage")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取分页数据", notes = "测试 获取分页数据")
    PageResult<BusinessSelfDefEmployee> findByPage(Search searchConfig);


    /**
     * 通过业务实体ID获取定义的企业员工
     * @param businessModelId 业务模块ID
     * @return 企业员工
     */
    @GET
    @Path("findByBusinessModelId/{businessModelId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取分页数据", notes = "测试 获取分页数据")
    List<BusinessSelfDefEmployee > findByBusinessModelId(@PathParam("businessModelId")String businessModelId);

    /**
     * 保存设置的执行人
     * @param businessModelId 业务模块id
     * @param selectedCustomExecutorIds 选择的执行人id
     */
    @POST
    @Path("saveCustomExecutor/{businessModelId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "保存设置的自由执行人",notes = "测试")
    public void saveCustomExecutor(@PathParam("businessModelId")String businessModelId,String selectedCustomExecutorIds);

}
