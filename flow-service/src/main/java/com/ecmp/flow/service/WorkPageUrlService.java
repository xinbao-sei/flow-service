package com.ecmp.flow.service;

import com.ecmp.core.dao.BaseEntityDao;
import com.ecmp.core.service.BaseEntityService;
import com.ecmp.flow.api.IWorkPageUrlService;
import com.ecmp.flow.dao.BusinessWorkPageUrlDao;
import com.ecmp.flow.dao.FlowTaskDao;
import com.ecmp.flow.dao.WorkPageUrlDao;
import com.ecmp.flow.entity.BusinessWorkPageUrl;
import com.ecmp.flow.entity.FlowTask;
import com.ecmp.flow.entity.WorkPageUrl;
import com.ecmp.vo.OperateResult;
import com.ecmp.vo.ResponseData;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Service
public class WorkPageUrlService extends BaseEntityService<WorkPageUrl> implements IWorkPageUrlService {

    @Autowired
    private WorkPageUrlDao workPageUrlDao;

    @Autowired
    private BusinessWorkPageUrlDao businessWorkPageUrlDao;

    @Autowired
    private FlowTaskDao flowTaskDao;


    protected BaseEntityDao<WorkPageUrl> getDao() {
        return this.workPageUrlDao;
    }


    @Override
    public List<WorkPageUrl> findByAppModuleId(String appModuleId) {
        return workPageUrlDao.findByAppModuleId(appModuleId);
    }

    @Override
    public List<WorkPageUrl> findSelectEdByAppModuleId(String appModuleId, String businessModelId) {
        return workPageUrlDao.findSelectEd(appModuleId, businessModelId);
    }

    @Override
    public List<WorkPageUrl> findNotSelectEdByAppModuleId(String appModuleId, String businessModelId) {
        return workPageUrlDao.findNotSelectEd(appModuleId, businessModelId);
    }

    @Override
    public ResponseData listAllNotSelectEdByAppModuleId(String appModuleId, String businessModelId) {
        if (StringUtils.isNotEmpty(appModuleId) && StringUtils.isNotEmpty(businessModelId)) {
            List<WorkPageUrl> list = this.findNotSelectEdByAppModuleId(appModuleId, businessModelId);
            return ResponseData.operationSuccessWithData(list);
        } else {
            return ResponseData.operationFailure("10006");
        }
    }

    @Override
    public List<WorkPageUrl> findByFlowTypeId(String flowTypeId) {
        return workPageUrlDao.findByFlowTypeId(flowTypeId);
    }


    public List<WorkPageUrl> findSelectEdByBusinessModelId(String businessModelId) {
        return workPageUrlDao.findSelectEdByBusinessModelId(businessModelId);
    }

    @Override
    public ResponseData listAllSelectEdByAppModuleId(String businessModelId) {
        if (StringUtils.isNotEmpty(businessModelId)) {
            List<WorkPageUrl> list = this.findSelectEdByBusinessModelId(businessModelId);
            return ResponseData.operationSuccessWithData(list);
        } else {
            return ResponseData.operationFailure("10006");
        }
    }

    /**
     * 数据删除操作
     * (检查工作界面是不是已经分配，已经分配的不能进行删除，给出提供)
     *
     * @param id 待操作数据
     */
    @Override
    public OperateResult delete(String id) {
        List<BusinessWorkPageUrl> list = businessWorkPageUrlDao.findListByProperty("workPageUrlId", id);
        if (!CollectionUtils.isEmpty(list)) {
            return OperateResult.operationFailure("10276");
        }
        List<FlowTask> taskList = flowTaskDao.findListByProperty("workPageUrl.id",id);
        if (!CollectionUtils.isEmpty(taskList)) {
            return OperateResult.operationFailure("10441");
        }
        return super.delete(id);
    }
}
