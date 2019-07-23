package com.ecmp.flow.api;


import com.ecmp.flow.api.common.api.IBaseService;
import com.ecmp.flow.entity.FlowTaskPushControl;
import com.ecmp.vo.ResponseData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("flowTaskPushControl")
@Api(value = "IFlowTaskPushControlService 流程推送控制任务服务API接口")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface IFlowTaskPushControlService extends IBaseService<FlowTaskPushControl, String> {


    /**
     * 通过推送控制表Id重新推送当前任务
     *
     * @param pushControlId 推送控制表ID
     * @return
     */
    @GET
    @Path("pushAgainByControlId")
    @ApiOperation(value = "新推送当前任务", notes = "通过推送控制表Id重新推送当前任务")
    ResponseData pushAgainByControlId(@QueryParam("pushControlId") String pushControlId);

}
