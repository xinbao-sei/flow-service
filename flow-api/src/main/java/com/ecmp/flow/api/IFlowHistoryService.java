package com.ecmp.flow.api;

import com.ecmp.core.search.PageResult;
import com.ecmp.core.search.Search;
import com.ecmp.flow.api.common.api.IBaseService;
import com.ecmp.flow.entity.FlowHistory;
import com.ecmp.vo.OperateResultWithData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * *************************************************************************************************
 * <p>
 * 实现功能：流程历史服务API接口定义
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
@Path("flowHistory")
@Api(value = "IFlowHistoryService 流程历史服务API接口")
public interface IFlowHistoryService extends IBaseService<FlowHistory, String> {

    /**
     * 保存一个实体
     * @param flowHistory 实体
     * @return 保存后的实体
     */
    @POST
    @Path("save")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "保存实体",notes = "测试 保存实体")
    OperateResultWithData<FlowHistory> save(FlowHistory flowHistory);

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
    PageResult<FlowHistory> findByPage(Search searchConfig);

    /**
     * 根据流程实例ID查询流程历史
     * @param instanceId 实例id
     * @return 执行历史
     */
    @POST
    @Path("findByInstanceId")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "查询流程历史", notes = "查询流程历史")
    List<FlowHistory> findByInstanceId(String instanceId);

    /**
     * 获取分页数据
     *
     * @return 实体清单
     */
    @POST
    @Path("findByPageAndUser")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取分页数据", notes = "测试 获取分页数据")
    PageResult<FlowHistory> findByPageAndUser(Search searchConfig);

    /**
     * 获取待办汇总信息
     * @param businessModelId 业务实体id
     * @param searchConfig 查询条件
     * @return 待办汇总信息
     */
    @POST
    @Path("findByBusinessModelId")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取待办汇总信息",notes = "测试")
    public PageResult<FlowHistory> findByBusinessModelId(@QueryParam("businessModelId") String businessModelId, Search searchConfig);

    /**
     * 获取待办汇总信息
     * @param businessModelCode 业务实体代码
     * @param searchConfig 查询条件
     * @return 待办汇总信息
     */
    @POST
    @Path("findByBusinessModelCode")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取待办汇总信息",notes = "测试")
    public PageResult<FlowHistory> findByBusinessModelCode(@QueryParam("businessModelCode") String businessModelCode, Search searchConfig);

}
