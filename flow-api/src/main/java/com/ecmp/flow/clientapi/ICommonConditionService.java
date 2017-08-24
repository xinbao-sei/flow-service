package com.ecmp.flow.clientapi;

import com.ecmp.flow.constant.FlowStatus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * *************************************************************************************************
 * <p>
 * 实现功能：业务系统条件通用服务API接口定义
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
public interface ICommonConditionService {

    /**
     * 获取条件POJO属性说明
     * @param conditonPojoClassName 条件POJO类名
     * @return  POJO属性说明Map
     * @throws ClassNotFoundException 类找不到异常
     */
    @GET
    @Path("properties/{conditonPojoClassName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取条件POJO属性说明",notes = "测试")
    public Map<String,String> getPropertiesForConditionPojo(@PathParam("conditonPojoClassName") String conditonPojoClassName) throws ClassNotFoundException;

    /**
     * 获取条件POJO属性说明及初始化值
     * @param conditonPojoClassName 条件实体类名称
     * @return  获取条件POJO属性说明及初始化值Map
     * @throws ClassNotFoundException 类找不到异常
     * @throws InvocationTargetException 目标类解析异常
     * @throws InstantiationException 实例异常
     * @throws IllegalAccessException 访问异常
     * @throws NoSuchMethodException 没有方法异常
     */
    @GET
    @Path("propertiesAndValues/{conditonPojoClassName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取条件POJO属性及初始化值说明",notes = "测试")
    public Map<String,Object> getPropertiesAndValues(@PathParam("conditonPojoClassName") String conditonPojoClassName) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException;


    /**
     * 通过Id获取条件POJO的值
     * @param conditonPojoClassName 条件pojo类名
     * @param daoBeanName  对应的dao层BeanName
     * @param id  业务ID
     * @return POJO的值
     * @throws ClassNotFoundException 类找不到异常
     * @throws InvocationTargetException 目标类解析异常
     * @throws InstantiationException 实例异常
     * @throws IllegalAccessException 访问异常
     * @throws NoSuchMethodException 没有方法异常
     */
    @GET
    @Path("conditonPojoMap/{conditonPojoClassName}/{daoBeanName}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "通过业务ID获取条件POJO的值",notes = "测试")
    public Map<String,Object> getConditonPojoMap(@PathParam("conditonPojoClassName") String conditonPojoClassName,@PathParam("daoBeanName") String daoBeanName,@PathParam("id") String id) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException;


    /**
     * 获取条件POJO属性说明
     * @param businessModelId 业务实体ID
     * @return  POJO属性说明Map
     * @throws ClassNotFoundException 类找不到异常
     */
    @GET
    @Path("propertiesByBusinessModelId/{businessModelId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "通过业务实体ID获取条件POJO属性说明",notes = "测试")
    public Map<String, String> getPropertiesForConditionPojoByBusinessModelId(@PathParam("businessModelId")String businessModelId) throws ClassNotFoundException;


    /**
     * 获取条件POJO属性说明
     * @param businessModelId 业务实体ID
     * @return  POJO属性说明Map
     * @throws ClassNotFoundException 类找不到异常
     * @throws InvocationTargetException 目标类解析异常
     * @throws InstantiationException 实例异常
     * @throws IllegalAccessException 访问异常
     * @throws NoSuchMethodException 没有方法异常
     */
    @GET
    @Path("propertiesAndValuesByBusinessModelId/{businessModelId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "通过业务实体ID获取条件POJO属性及初始化值说明",notes = "测试")
    public Map<String, Object> getPropertiesAndValuesByBusinessModelId(@PathParam("businessModelId")String businessModelId) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException;


    /**
     * 获取条件POJO属性说明
     * @param businessModelId 业务实体ID
     * @param id 单据id
     * @return  POJO属性说明Map
     * @throws ClassNotFoundException 类找不到异常
     * @throws InvocationTargetException 目标类解析异常
     * @throws InstantiationException 实例异常
     * @throws IllegalAccessException 访问异常
     * @throws NoSuchMethodException 没有方法异常
     */
    @GET
    @Path("conditonPojoMapByBusinessModelId/{businessModelId}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "通过业务实体ID,业务ID获取条件POJO的值",notes = "测试")
    public Map<String,Object> getConditonPojoMapByBusinessModelId(@PathParam("businessModelId")String businessModelId,@PathParam("id")String id) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException;



    /**
     * 重置单据状态
     * @param businessModelId   业务实体id
     * @param id   单据id
     * @param status   状态
     * @return 返回结果
     * @throws ClassNotFoundException 类找不到异常
     * @throws InvocationTargetException 目标类解析异常
     * @throws InstantiationException 实例异常
     * @throws IllegalAccessException 访问异常
     * @throws NoSuchMethodException 没有方法异常
     */
    @GET
    @Path("resetState/{businessModelId}/{id}/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取条件POJO属性及初始化值说明",notes = "测试")
    public Boolean resetState(@PathParam("businessModelId")String businessModelId,@PathParam("id")String id,
                              @PathParam("status") FlowStatus status) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException;
}
