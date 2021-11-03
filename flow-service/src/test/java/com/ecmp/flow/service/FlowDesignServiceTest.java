package com.ecmp.flow.service;

import com.ecmp.flow.vo.SaveEntityVo;
import com.ecmp.util.JsonUtils;
import com.ecmp.vo.ResponseData;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FlowDesignServiceTest extends BaseContextTestCase {

    @Autowired
    private FlowDesignService service;

    @Test
    public void save() {
        String json = "{\"flowTypeId\":\"B2FC0C5F-5E87-11EA-AEE3-0242C0A8460D\",\"orgId\":\"877035BF-A40C-11E7-A8B9-02420B99179E\",\"orgCode\":\"10607\",\"id\":\"31719D16-5EA7-11EA-B07D-0242C0A8460D\",\"versionCode\":-1,\"priority\":0,\"businessModelId\":\"B0E334A1-5E86-11EA-AEE3-0242C0A8460D\",\"subProcess\":false,\"solidifyFlow\":false,\"timing\":48,\"earlyWarningTime\":24,\"process\":{\"name\":\"流程自测\",\"id\":\"ak00002\",\"flowDefVersionId\":\"5AAE8FE9-3C81-11EC-8A89-0242C0A84611\",\"isExecutable\":true,\"nodes\":{\"EndEvent_4\":{\"type\":\"EndEvent\",\"x\":985,\"y\":111,\"id\":\"EndEvent_4\",\"target\":[],\"name\":\"结束\",\"nodeConfig\":{}},\"UserTask_77\":{\"type\":\"UserTask\",\"x\":576,\"y\":1,\"id\":\"UserTask_77\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"ParallelGateway_108\",\"uel\":\"\"}],\"name\":\"普通任务3\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":true,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"普通任务3\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_88\":{\"type\":\"UserTask\",\"x\":439,\"y\":243,\"id\":\"UserTask_88\",\"nodeType\":\"Approve\",\"target\":[{\"targetId\":\"UserTask_110\",\"uel\":{\"agree\":true,\"groovyUel\":\"${approveResult == true}\",\"logicUel\":\"\",\"name\":\"同意\"}},{\"targetId\":\"UserTask_120\",\"uel\":{\"groovyUel\":\"\",\"isDefault\":true,\"logicUel\":\"\",\"name\":\"不同意\"}}],\"name\":\"审批任务2\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowEntrust\":true,\"allowJumpBack\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":true,\"allowTerminate\":true,\"allowTransfer\":true,\"chooseDisagreeReason\":false,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"审批任务2\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"condition\":\"ALL\",\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"condition\":\"ALL\",\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"condition\":\"ALL\",\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_99\":{\"type\":\"UserTask\",\"x\":307,\"y\":0,\"id\":\"UserTask_99\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"UserTask_104\",\"uel\":\"\"}],\"name\":\"普通任务1\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":true,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"普通任务1\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_101\":{\"type\":\"UserTask\",\"x\":73,\"y\":370,\"id\":\"UserTask_101\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"UserTask_102\",\"uel\":\"\"}],\"name\":\"修改节点\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"修改节点\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_102\":{\"type\":\"UserTask\",\"x\":89,\"y\":88,\"id\":\"UserTask_102\",\"nodeType\":\"Approve\",\"target\":[{\"targetId\":\"UserTask_101\",\"uel\":{\"groovyUel\":\"\",\"isDefault\":true,\"logicUel\":\"\",\"name\":\"不同意\"}},{\"targetId\":\"ParallelGateway_103\",\"uel\":{\"agree\":true,\"groovyUel\":\"${approveResult == true}\",\"logicUel\":\"\",\"name\":\"同意\"}}],\"name\":\"审批任务\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowEntrust\":true,\"allowJumpBack\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"chooseDisagreeReason\":false,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"审批任务\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"ParallelGateway_103\":{\"type\":\"ParallelGateway\",\"x\":238,\"y\":90,\"id\":\"ParallelGateway_103\",\"target\":[{\"targetId\":\"UserTask_99\",\"uel\":\"\"},{\"targetId\":\"UserTask_105\",\"uel\":\"\"}],\"name\":\"并行网关\",\"nodeConfig\":{},\"busType\":\"ParallelGateway\"},\"UserTask_104\":{\"type\":\"UserTask\",\"x\":446,\"y\":1,\"id\":\"UserTask_104\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"UserTask_77\",\"uel\":\"\"}],\"name\":\"普通任务2\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"普通任务2\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_105\":{\"type\":\"UserTask\",\"x\":312,\"y\":191,\"id\":\"UserTask_105\",\"nodeType\":\"Approve\",\"target\":[{\"targetId\":\"UserTask_88\",\"uel\":{\"agree\":true,\"groovyUel\":\"${approveResult == true}\",\"logicUel\":\"\",\"name\":\"同意\"}},{\"targetId\":\"UserTask_120\",\"uel\":{\"groovyUel\":\"\",\"isDefault\":true,\"logicUel\":\"\",\"name\":\"不同意\"}}],\"name\":\"审批任务1\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowEntrust\":true,\"allowJumpBack\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"chooseDisagreeReason\":false,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"审批任务1\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"StartEvent_106\":{\"type\":\"StartEvent\",\"x\":0,\"y\":70,\"id\":\"StartEvent_106\",\"target\":[{\"targetId\":\"UserTask_102\",\"uel\":\"\"}],\"name\":\"开始\",\"nodeConfig\":{}},\"ParallelGateway_108\":{\"type\":\"ParallelGateway\",\"x\":649,\"y\":127,\"id\":\"ParallelGateway_108\",\"target\":[{\"targetId\":\"UserTask_109\",\"uel\":\"\"}],\"name\":\"并行网关\",\"nodeConfig\":{},\"busType\":\"ParallelGateway\"},\"UserTask_109\":{\"type\":\"UserTask\",\"x\":759,\"y\":118,\"id\":\"UserTask_109\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"EndEvent_4\",\"uel\":\"\"}],\"name\":\"普通任务\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"普通任务\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_110\":{\"type\":\"UserTask\",\"x\":604,\"y\":238,\"id\":\"UserTask_110\",\"nodeType\":\"SingleSign\",\"target\":[{\"targetId\":\"ParallelGateway_108\",\"uel\":\"\"}],\"name\":\"单签任务\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowTerminate\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"单签任务\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}},\"UserTask_120\":{\"type\":\"UserTask\",\"x\":364,\"y\":402,\"id\":\"UserTask_120\",\"nodeType\":\"Normal\",\"target\":[{\"targetId\":\"UserTask_105\",\"uel\":\"\"}],\"name\":\"分支修改\",\"nodeConfig\":{\"normal\":{\"allowChooseInstancy\":false,\"allowPreUndo\":true,\"allowReject\":true,\"allowReturn\":false,\"allowTerminate\":true,\"allowTransfer\":true,\"defaultOpinion\":\"\",\"executeDay\":0,\"executeHour\":0,\"executeMinute\":0,\"id\":\"89B880A6-5E86-11EA-AEE3-0242C0A8460D\",\"mustCommit\":true,\"name\":\"分支修改\",\"nodeCode\":\"\",\"singleTaskNoChoose\":false,\"workPageName\":\"查看页面suid（不跳）\"},\"executor\":[{\"userType\":\"StartUser\"}],\"event\":{\"afterAsync\":false,\"afterExcuteService\":\"\",\"afterExcuteServiceId\":\"\",\"beforeAsync\":true,\"beforeExcuteService\":\"\",\"beforeExcuteServiceId\":\"\"},\"notify\":{\"after\":{\"notifyExecutor\":\"\",\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}},\"before\":{\"notifyExecutor\":{\"content\":\"\",\"type\":[]},\"notifyPosition\":{\"content\":\"\",\"positionData\":[],\"positionIds\":[],\"type\":[]},\"notifySelfDefinition\":{\"content\":\"\",\"name\":\"\",\"notifySelfDefId\":\"\",\"type\":[]},\"notifyStarter\":{\"content\":\"\",\"type\":[]}}}}}},\"beforeStartServiceName\":\"\",\"beforeStartServiceId\":\"\",\"beforeStartServiceAync\":false,\"afterStartServiceName\":\"\",\"afterStartServiceId\":\"\",\"afterStartServiceAync\":false,\"beforeEndServiceName\":\"\",\"beforeEndServiceId\":\"\",\"beforeEndServiceAync\":false,\"afterEndServiceName\":\"\",\"afterEndServiceId\":\"\",\"afterEndServiceAync\":false,\"logicUel\":\"\",\"startUEL\":{\"code\":\"\",\"isDefault\":false,\"logicUel\":\"\",\"groovyUel\":\"\"}}}";
        SaveEntityVo vo = new SaveEntityVo();
        vo.setDef(json);
        vo.setDeploy(true);
        try {
            ResponseData res = service.save(vo);
            System.out.println(JsonUtils.toJson(res));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    @Test
    public void getEntity() {
        String id = "98307F87-5150-11EA-BBE4-0242C0A84421";
        Integer versionCode = -1;
        String businessModelCode = "com.ecmp.flow.entity.DefaultBusinessModel";
        String businessId = "2AA0E332-5A03-11EA-A372-0242C0A84421";
        ResponseData res = service.getEntity(id, versionCode, businessModelCode, businessId);
        System.out.println(JsonUtils.toJson(res));
    }


}
