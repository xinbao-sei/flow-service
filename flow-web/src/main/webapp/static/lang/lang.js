var common_lang = {
    operateText: "操作",
    tiShiText: "提示",
    ifDelMsgText: "确定删除吗？",
    sureText: "确定",
    nowDelMsgText: "正在删除,请稍后....",
    cancelText: "取消",
    saveText: "保存",
    nowSaveMsgText: "正在保存，请稍候...",
    codeText: "代码",
    nameText: "名称",
    depictText: "描述",
    searchNameText: "请输入名称进行搜索",
    InputSearchNameText: "请输入搜索名称",

    addText: "新增",
    hintText: "提示",
    paramsText: "参数为空!",
    addHintMessageText: "您确定要切换操作吗？未保存的数据可能会丢失!",
    okText: "确定",
    operateText: "操作",
    cancelText: "取消",
    saveText: "保存",
    modifyText: "修改",
    appendText: "添加",
    saveMaskMessageText: "正在保存，请稍候...",
    deleteText: "删除",
    deleteHintMessageText: "您确定要删除吗？",
    deleteMaskMessageText: "正在删除，请稍候...",
    queryMaskMessageText: "正在努力获取数据，请稍候...",
};

if (EUI.BusinessModelView) {
    EUI.apply(EUI.BusinessModelView.prototype.lang, {
        modelText: "应用模块",
        addResourceText: "新增",
        classPathText: "类全路径",
        conditonBeanText: "转换对象",
        belongToAppModuleText: "所属应用模块",
        updateBusinessModelText: "修改业务模型",
        conditonBeanText: "转换对象",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputClassPathMsgText: "请输入类全路径",
        inputConditonBeanText: "请输入转换对象",
        inputDepictMsgText: "请输入描述",
        inputWorkPageText: "请输入工作界面",
        inputConditonBeanText: "请输入转换对象",
        searchNameText: "请输入名称进行搜索",
        chooseBelongToAppModuleText: "请选择所属应用模块",
        addNewBusinessModelText: "新增业务模型",
        urlViewAddressText: "URL地址",
        conditionPropertyText: "条件属性",
        propertyText: "属性",
        workPageSetText: "工作界面配置",
        typeText: "类型",
        fieldNameText: "字段名",
        noteText: "注解",
        chooseAppModelText: "请选择应用模块",
        appModelIdText: "应用模块ID",
        serviceUrlText: "服务地址管理",
        addServiceUrlText: "新增服务地址",
        businessModelIdText: "业务实体ID",
        updateServiceUrlText: "修改服务地址管理"
    }, common_lang);
}

if (EUI.FlowServiceUrlView) {
    EUI.apply(EUI.FlowServiceUrlView.prototype.lang, {
        addResourceText: "新增",
        updateFlowServiceUrlText: "修改服务地址管理",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputUrlMsgText: "请输入URL",
        inputDepictMsgText: "请输入描述",
        addNewFlowServiceUrlText: "新增服务地址管理"
    }, common_lang);
}

if (EUI.CustomExecutorView) {
    EUI.apply(EUI.CustomExecutorView.prototype.lang, {
        addResourceText: "新增",
        updateFlowServiceUrlText: "修改服务地址管理",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputUrlMsgText: "请输入URL",
        inputDepictMsgText: "请输入描述",
        addNewFlowServiceUrlText: "新增服务地址管理"
    }, common_lang);
}

if (EUI.FlowTypeView) {
    EUI.apply(EUI.FlowTypeView.prototype.lang, {
        addResourceText: "新增",
        belongToBusinessModelText: "所属业务实体模型",
        updateFlowTypeText: "修改流程类型",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputDepictMsgText: "请输入描述",
        chooseBelongToBusinessModelText: "请选择所属业务实体模型",
        belongToBusinessText: "所属业务实体",
        addNewFlowTypeText: "新增流程类型"
    }, common_lang);
}

if (EUI.FlowInstanceView) {
    EUI.apply(EUI.FlowInstanceView.prototype.lang, {
        addResourceText: "新增",
        belongToBusinessModelText: "所属业务实体模型",
        updateFlowTypeText: "修改流程类型",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputDepictMsgText: "请输入描述",
        chooseBelongToBusinessModelText: "请选择所属业务实体模型",
        belongToBusinessText: "所属业务实体",
        addNewFlowTypeText: "新增流程类型"
    }, common_lang);
}

if (EUI.FlowDefinationView) {
    EUI.apply(EUI.FlowDefinationView.prototype.lang, {
        addResourceText: "新增",
        belongToBusinessModelText: "所属业务实体模型",
        updateFlowTypeText: "修改流程类型",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputDepictMsgText: "请输入描述",
        chooseBelongToBusinessModelText: "请选择所属业务实体模型",
        belongToBusinessText: "所属业务实体",
        addNewFlowTypeText: "新增流程类型",
    }, common_lang);
}

if (EUI.WorkPageUrlView) {
    EUI.apply(EUI.WorkPageUrlView.prototype.lang, {
        modelText: "应用模块",
        inputModelText: "请选择应用模块",
        modelValueText: "全部模块",
        addBtnText: "新增",
        urlViewAddressText: "URL地址",
        appModelIdText: "应用模块ID",
        updateWorkPageUrlText: "修改工作页面",
        inputCodeMsgText: "请输入代码",
        inputNameMsgText: "请输入名称",
        inputUrlViewAddressMsgText: "请输入URL界面地址",
        inputDepictMsgText: "请输入描述",
        addNewWorkPageUrlText: "新增工作页面"
    }, common_lang);
}

if (EUI.FlowDefinationView) {
    EUI.apply(EUI.FlowDefinationView.prototype.lang, {
        moveText: "移动",
        codeText: "代码",
        nameText: "名称",
        frozenText: '是否冻结',
        rankText: '排序',
        refreshTest: "刷新",
        modifyRootText: "禁止修改根节点！",
        addHintMessageText: "请选择一个组织结构节点!",
        createNodeText: "创建节点",
        updateRootText: "禁止修改根节点!",
        moveHintMessageText: "请选择您要移动的节点！",
        rootText: "根节点",
        queryMaskMessageText: "正在努力获取数据，请稍候...",
        closeText: "关闭",
        searchDisplayText: "请输入名称查询",
        processMaskMessageText: "正在处理，请稍候...",
        operateHintMessage: "请选择一条要操作的行项目!",
        processMaskMessageText: "正在处理，请稍候..."
    }, common_lang);
}
;
if (EUI.WorkFlowView) {
    EUI.apply(EUI.WorkFlowView.prototype.lang, {
        eventTitleText: "事件",
        taskTitleText: "任务",
        gatewayTitleText: "网关",
        noConnectLineText: "{0}节点没有进行连线",
        startEventText: "开始",
        endEventText: "结束",
        normalTaskText: "普通任务",
        singleSignTaskText: "单签任务",
        counterSignTaskText: "会签任务",
        userTaskText: "审批任务",
        serviceTaskText: "服务任务",
        scriptTaskText: "脚本任务",
        emailTaskText: "邮件任务",
        manualTaskText: "手工任务",
        exclusiveGatewayText: "系统排他网关",
        manualExclusiveGatewayText: "人工排他网关",
        parallelGatewayText: "并行网关",
        inclusiveGatewayText: "包容网关",
        eventGatewayText: "事件网关",
        saveText: "保存",
        resetText: "清空",
        deployText: "发布"
    }, common_lang);
}

if (EUI.LookWorkFlowView) {
    EUI.apply(EUI.LookWorkFlowView.prototype.lang, {
        eventTitleText: "事件",
        taskTitleText: "任务",
        gatewayTitleText: "网关",
        noConnectLineText: "{0}节点没有进行连线",
        startEventText: "开始",
        endEventText: "结束",
        normalTaskText: "普通任务",
        singleSignTaskText: "单签任务",
        counterSignTaskText: "会签任务",
        userTaskText: "审批任务",
        serviceTaskText: "服务任务",
        scriptTaskText: "脚本任务",
        emailTaskText: "邮件任务",
        manualTaskText: "手工任务",
        exclusiveGatewayText: "系统排他网关",
        manualExclusiveGatewayText: "人工排他网关",
        parallelGatewayText: "并行网关",
        inclusiveGatewayText: "包容网关",
        eventGatewayText: "事件网关",
        saveText: "保存",
        resetText: "清空",
        deployText: "发布"
    }, common_lang);
}

