package com.ecmp.flow.api;

import com.ecmp.flow.entity.BusinessModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * *************************************************************************************************
 * <p/>
 * 实现功能：业务实体服务API接口定义
 * <p>
 * ------------------------------------------------------------------------------------------------
 * 版本          变更时间             变更人                     变更原因
 * ------------------------------------------------------------------------------------------------
 * 1.0.00      2017/3/26 10:39      谭军(tanjun)               新建
 * <p/>
 * *************************************************************************************************
 */
@Path("businessModel")
@Api(value = "IBusinessModelService 工作流项目API服务")
public interface IBusinessModelService {
    /**
     * 测试WEB服务方法
     * @return hello
     */
    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "测试方法",notes = "测试 hello")
    String hello();

    /**
     * 获取所有实体
     * @return 实体清单
     */
    @GET
    @Path("getAll")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取所有实体",notes = "测试 获取所有实体")
    List<BusinessModel> findAll();

    /**
     * 保存一个实体
     * @param entity 实体
     * @return 保存后的实体
     */
    @POST
    @Path("save")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    BusinessModel save(BusinessModel entity);
}
