/**
 * 流程设计界面
 */
EUI.LookWorkFlowView = EUI.extend(EUI.CustomUI, {
    renderTo: null,
    count: 0,
    id: null,
    instanceId: null,
    versionCode: null,
    instance: null,
    connectInfo: {},
    uelInfo: {},
    businessModelId: null,//业务实体ID
    viewFlowDefByVersionId: false,//根据流程定义版本id查看流程定义
    initComponent: function () {
        var g = this;
        EUI.Container({
            renderTo: this.renderTo,
            layout: "border",
            defaultConfig: {
                border: true,
                borderCss: "flow-border"
            },
            items: [{
                xtype: "ToolBar",
                region: "north",
                border: false,
                isOverFlow: false,
                height: 40,
                padding: 3,
                items: this.getTopItems()
            }, {
                region: "center",
                id: "center",
                html: this.getCenterHtml()
            }]
        });
        //设置面板背景表格
        EUI.getCmp("center").dom.addClass("flow-grid");
        this.addEvents();
        this.initJSPlumb();
        if (this.id) {
            this.loadData();
        }
    },
    getTopItems: function () {
        var g = this;
        return [{
            xtype: "FormPanel",
            width: 850,
            isOverFlow: false,
            height: 40,
            padding: 0,
            layout: "auto",
            id: "formPanel",
            border: false,
            itemspace: 5,
            defaultConfig: {
                labelWidth: 80,
                readonly: true,
                xtype: "TextField"
            },
            items: [{
                name: "flowTypeName",
                title: "流程类型",
                width: 200
            }, {
                name: "id",
                width: 120,
                labelWidth: 85,
                title: "流程代码"
            }, {
                xtype: "TextField",
                title: "流程名称",
                labelWidth: 85,
                width: 250,
                name: "name"
            }]
        }, {
            xtype: "Button",
            title: "启动条件",
            selected: true,
            iconCss: "ecmp-common-configuration",
            id: "setStartUel",
            hidden: true,
            handler: function () {
                var scope = this;
                new EUI.LookUELSettingView({
                    title: "流程启动条件",
                    data: g.startUEL,
                    businessModelId: g.businessModelId,
                    afterConfirm: function (data) {
                        scope.startUEL = data;
                    }
                });
            }
        }];
    },
    getCenterHtml: function () {
        return "<div class='flow-content'></div>";
    },
    addEvents: function () {
        var g = this;
        $(".node-choosed").live({
            "dblclick": function () {
                var dom = $(this);
                var type = dom.attr("type");
                if (type == "StartEvent" || type.indexOf("EndEvent") != -1) {
                    return;
                }
                var input = dom.find(".node-title");
                if (type.endsWith("Gateway")) {
                    g.showSimpleNodeConfig(input.text());
                } else {
                    new EUI.LookFlowNodeSettingView({
                        businessModelId: g.businessModelId,
                        data: dom.data(),
                        nodeType: dom.attr("nodeType")
                    });
                }
            }
        });
    }
    ,
    initJSPlumb: function () {
        var g = this;
        this.instance = jsPlumb.getInstance({
            Endpoint: "Blank",
            ConnectionOverlays: [["Arrow", {
                location: 1,
                visible: true,
                length: 14,
                id: "ARROW"
            }], ["Label", {
                location: 0.2,
                id: "label",
                visible: false,
                label: null,
                cssClass: "flow-line-note"
            }]],
            Container: "body"
        });

        this.instance.registerConnectionType("basic", {
            anchor: "Continuous",
            connector: ["Flowchart", {
                stub: [5, 5],
                cornerRadius: 5
            }]
        });
        // 双击连线弹出UEL配置界面
        this.instance.bind("dblclick", function (connection) {
            var ueldata = g.uelInfo[connection.sourceId + "," + connection.targetId];
            if (!ueldata) {
                EUI.ProcessStatus({
                    success: true,
                    msg: "当前连线未配置UEL表达式"
                });
                return;
            }
            if (ueldata.isDefault) {
                return;
            }
            var type = $("#" + connection.sourceId).attr("bustype");
            if (type == "ManualExclusiveGateway") {
                g.showSimpleNodeConfig(ueldata.name);
                return;
            }
            new EUI.LookUELSettingView({
                title: "表达式配置",
                data: g.uelInfo[connection.sourceId + "," + connection.targetId],
                businessModelId: g.businessModelId
            });
        });
        // 连接事件
        this.instance.bind("connection", function (connection, originalEvent) {
            g.connectInfo[connection.sourceId + "," + connection.targetId] = true;
            var uel = g.uelInfo[connection.sourceId + "," + connection.targetId];
            if (uel) {
                var overlay = connection.connection.getOverlay("label");
                overlay.setLabel(uel.name);
                overlay.show();
            }
        });
    }
    ,
    initNode: function (el) {
        this.instance.makeSource(el, {
            filter: ".node-dot",
            anchor: "Continuous",
            connector: ["Flowchart", {
                stub: [5, 5],
                cornerRadius: 5,
            }],
            connectorStyle: {
                stroke: "#61B7CF",
                strokeWidth: 2,
                joinstyle: "round",
                outlineStroke: "white",
                outlineWidth: 2
            },
            connectorHoverStyle: {
                strokeWidth: 3,
                stroke: "#216477",
                outlineWidth: 5,
                outlineStroke: "white"
            },
            connectionType: "basic"
        });
    }
    ,
    doConect: function (sourceId, targetId) {
        this.instance.connect({
            source: sourceId,
            target: targetId,
            type: "basic"
        });
    }
    ,
    loadData: function (data) {
        var g = this;
        var url = _ctxPath + "/design/getLookInfo";
        var postData = {
            id: this.id,
            instanceId: this.instanceId,
            versionCode: this.versionCode
        };
        if (this.viewFlowDefByVersionId) {
            url = _ctxPath + "/design/getEntityByVersionId";
            postData = {
                flowDefVersionId: this.id
            }
        }
        var mask = EUI.LoadMask({
            msg: "正在获取数据，请稍候..."
        });
        EUI.Store({
            url: url,
            params: postData,
            success: function (status) {
                mask.hide();
                g.showDesign(status.data);
            },
            failure: function (status) {
                mask.hide();
                EUI.ProcessStatus(status);
            }
        });
    },
    showDesign: function (defData) {
        if (this.viewFlowDefByVersionId) {
            var data = JSON.parse(defData.defJson);
        } else {
            var data = JSON.parse(defData.def.defJson);
            var currentNodes = defData.currentNodes ? defData.currentNodes.join(",") : "";
        }
        this.businessModelId = data.businessModelId;
        this.loadHead(data);
        var html = "";
        for (var id in data.process.nodes) {
            var node = data.process.nodes[id];
            var type = node.type;
            if (type == "StartEvent") {
                html += this.showStartNode(id, node);
            } else if (type.indexOf("EndEvent") != -1) {
                html += this.showEndNode(id, node);
            } else if (type.indexOf("Task") != -1) {
                html += this.showTaskNode(id, node, currentNodes);
            } else if (type.indexOf("Gateway") != -1) {
                html += this.showGatewayNode(id, node, currentNodes);
            }
            var tmps = id.split("_");
            var count = parseInt(tmps[1]);
            this.count = this.count > count ? this.count : count;
        }
        $(".flow-content").append(html);
        var doms = $(".node-choosed");
        for (var i = 0; i < doms.length; i++) {
            this.initNode(doms[i]);
            var item = $(doms[i]);
            var id = item.attr("id");
            item.data(data.process.nodes[id]);
        }
        for (var id in data.process.nodes) {
            var node = data.process.nodes[id];
            for (var index in node.target) {
                var target = node.target[index];
                if (target.uel) {
                    this.uelInfo[id + "," + target.targetId] = target.uel;
                }
                this.doConect(id, target.targetId);
            }
        }
    },
    loadHead: function (data) {
        var headData = {
            name: data.process.name,
            id: data.process.id,
            flowTypeId: data.flowTypeId,
            flowTypeName: data.flowTypeName
        };
        this.startUEL = data.process.startUEL;
        EUI.getCmp("formPanel").loadData(headData);
        if (this.startUEL) {
            EUI.getCmp("setStartUel").show();
        }
    },
    showStartNode: function (id, node) {
        return "<div tabindex=0 type='StartEvent' id='"
            + id
            + "' class='flow-event-box flow-node node-choosed'  style='cursor: pointer; left: "
            + node.x
            + "px; top: "
            + node.y
            + "px; opacity: 1;'>"
            + "<div class='flow-event-iconbox'><div class='flow-event-start'></div></div>"
            + "<div class='node-title'>" + this.lang.startEventText + "</div>"
            + "</div>";
    }
    ,
    showEndNode: function (id, node) {
        var css = "flow-event-end";
        if (node.type == "TerminateEndEvent") {
            css = "flow-event-terminateend";
        }
        return "<div tabindex=0 type='" + node.type + "' id='"
            + id
            + "' class='flow-event-box flow-node node-choosed' style='cursor: pointer; left: "
            + node.x
            + "px; top: "
            + node.y
            + "px; opacity: 1;'>"
            + "<div class='flow-event-iconbox'><div class='" + css + "'></div></div>"
            + "<div class='node-title'>" + node.name + "</div>	</div>";
    }
    ,
    showTaskNode: function (id, node, currentNodes) {
        var nodeCss = "flow-task flow-node node-choosed";
        if (!this.viewFlowDefByVersionId) {
            if (currentNodes.indexOf(id) != -1) {
                nodeCss += " currentNode";
            }
        }
        var css = node.css;
        if (!css) {
            switch (node.nodeType) {
                case "Normal":
                    css = "usertask";
                    break;
                case "SingleSign":
                    css = "singletask";
                    break;
                case "CounterSign":
                    css = "countertask";
                    node.nodeConfig.normal.isSequential = node.nodeConfig.normal.isSequential.toString();
                    if (node.nodeConfig.normal.isSequential == "true") {
                        css = "countertask serial-countertask";
                    } else {
                        css = "countertask parallel-countertask";
                    }
                    break;
                case "Approve":
                    css = "approvetask";
                    break;
                case "ParallelTask":
                    css = "paralleltask";
                    break;
                case "SerialTask":
                    css = "serialtask";
                    break;
            }
        }
        return "<div tabindex=0 id='" + id
            + "' class='" + nodeCss + "' type='"
            + node.type + "' nodeType='" + node.nodeType + "' style='cursor: pointer; left: "
            + node.x + "px; top: " + node.y + "px; opacity: 1;'>"
            + "<div class='" + css + "'></div>"
            + "<div class='node-title'>" + node.name + "</div>"
            + "</div>";
    }
    ,
    showGatewayNode: function (id, node, currentNodes) {
        var nodeCss = "flow-event-box flow-node node-choosed";
        if (!this.viewFlowDefByVersionId) {
            if (currentNodes.indexOf(id) != -1) {
                nodeCss += " currentNode";
            }
        }
        var css = node.type.toLowerCase();
        if (node.busType == "ManualExclusiveGateway") {
            css = "manualExclusivegateway";
        }
        return "<div tabindex=0 id='" + id
            + "' class='" + nodeCss + "' bustype='" + node.busType + "' type='"
            + node.type + "' style='cursor: pointer; left: "
            + node.x + "px; top: " + node.y + "px; opacity: 1;'>"
            + "<div class='flow-gateway-iconbox'>"
            + "<div class='" + css + "'></div></div>"
            + "<div class='node-title'>" + node.name + "</div>"
            + "</div>";
    },
    showSimpleNodeConfig: function (title) {
        var win = EUI.Window({
            height: 33,
            padding: 30,
            title: "节点配置",
            items: [{
                xtype: "TextField",
                title: "节点名称",
                labelWidth: 100,
                readonly: true,
                width: 220,
                id: "nodeName",
                name: "name",
                value: title
            }]
        });
    }
})
;