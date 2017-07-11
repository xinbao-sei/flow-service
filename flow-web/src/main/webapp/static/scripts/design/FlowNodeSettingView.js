/**
 * Created by fly on 2017/4/18.
 */
EUI.FlowNodeSettingView = EUI.extend(EUI.CustomUI, {
    title: null,
    data: null,
    nowNotifyTab: null,
    nodeType: null,
    afterConfirm: null,
    businessModelId: null,
    flowTypeId: null,
    notifyBeforePositionData: null,
    notifyAfterPositionData: null,
    initComponent: function () {
        this.window = EUI.Window({
            width: 550,
            height: 420,
            padding: 15,
            buttons: this.getButtons(),
            afterRender: function () {
                this.dom.find(".ux-window-content").css("border-radius", "6px");
            },
            items: [{
                xtype: "TabPanel",
                isOverFlow: false,
                defaultConfig: {
                    iframe: false,
                    closable: false
                },
                items: [this.getNormalTab(), this.getExcutorTab(), this.getEventTab(),
                    this.getNotifyTab()]
            }]
        });
        this.initNotify();
        if (this.data && !Object.isEmpty(this.data)) {
            this.loadData();
        }
        this.addEvent();
    },
    addEvent: function () {
        var g = this;
        $(".condetail-delete").live("click", function () {
            var data = EUI.getCmp("userType").getValue();
            var userType = data.userType;
            var id = $(this).attr("id");
            var grid;
            if (userType == "Position") {
                grid = EUI.getCmp("positionGrid");
            } else if (userType == "PositionType") {
                grid = EUI.getCmp("positionTypeGrid");
            } else if (userType == "SelfDefinition") {
                grid = EUI.getCmp("selfDefGrid");
            }
            grid.deleteRow(id);
        });

        $(".west-navbar").live("click", function () {
            if ($(this).hasClass("select-navbar")) {
                return;
            }
            $(this).addClass("select-navbar").siblings().removeClass("select-navbar");
            var index = $(this).index();
            $(".notify-center").hide();
            var selecter = ".notify-center:eq(" + index + ")";
            $(selecter).show();
            if (index == 0) {
                g.nowNotifyTab = EUI.getCmp("notify-before");
            } else {
                g.nowNotifyTab = EUI.getCmp("notify-after");
            }
        });

        $(".notify-user-item").live("click", function () {
            if ($(this).hasClass("select")) {
                return;
            }
            $(this).addClass("select").siblings().removeClass("select");
            EUI.getCmp(g.nowNotifyTab.items[0]).hide();
            EUI.getCmp(g.nowNotifyTab.items[1]).hide();
            EUI.getCmp(g.nowNotifyTab.items[2]).hide();
            var index = $(this).index();
            switch (index) {
                case 0:
                    EUI.getCmp(g.nowNotifyTab.items[0]).show();
                    break;
                case 1:
                    EUI.getCmp(g.nowNotifyTab.items[1]).show();
                    break;
                case 2:
                    EUI.getCmp(g.nowNotifyTab.items[2]).show();
                    break;
                default:
                    break;
            }
        });
    },
    getButtons: function () {
        var g = this;
        return [{
            title: "保存配置",
            iconCss: "ecmp-common-save",
            selected: true,
            handler: function () {
                var normalForm = EUI.getCmp("normal");
                if (!normalForm.isValid()) {
                    EUI.ProcessStatus({
                        success: false,
                        msg: "请将常规项配置完整"
                    });
                    return;
                }
                if (!g.checkExcutor()) {
                    EUI.ProcessStatus({
                        success: false,
                        msg: "请将执行人项配置完整"
                    });
                    return;
                }
                var executorForm = EUI.getCmp("excutor");
                var eventForm = EUI.getCmp("event");
                var normalData = normalForm.getFormValue();
                var eventData = eventForm.getFormValue();
                g.afterConfirm && g.afterConfirm.call(this, {
                    normal: normalData,
                    executor: g.getExcutorData(),
                    event: eventData,
                    notify: g.getNotifyData()
                });
                g.remove();
                g.window.close();
            }
        }, {
            title: "取消",
            iconCss: "ecmp-common-delete",
            handler: function () {
                g.remove();
                g.window.close();
            }
        }];
    },
    getNormalTab: function () {
        var items = [{
            title: "节点名称",
            labelWidth: 100,
            allowBlank: false,
            name: "name",
            value: this.title
        }, {
            xtype: "NumberField",
            title: "额定工时",
            allowNegative: false,
            name: "executeTime",
            width: 262,
            labelWidth: 100,
            unit: "分钟"
        }, {
            xtype: "ComboBox",
            title: "工作界面",
            labelWidth: 100,
            allowBlank: false,
            name: "workPageName",
            field: ["workPageUrl"],
            async: false,
            store: {
                url: _ctxPath + "/design/listAllWorkPage",
                params: {
                    businessModelId: this.businessModelId
                }
            },
            reader: {
                name: "name",
                field: ["url"]
            }
        }];
        if (this.nodeType == "CounterSign") {
            items.push({
                xtype: "NumberField",
                title: "会签决策",
                labelWidth: 100,
                width: 283,
                unit: "%",
                minValue: 1,
                maxValue: 100.1,
                minValueText: "最低通过比例为1%",
                maxValueText: "最高通过比例为100%",
                displayText: "请输入会签通过的百分比1%—100%",
                allowNegative: false,
                allowBlank: false,
                name: "counterDecision"
            })
        }else {
            items = items.concat([{
                xtype: "CheckBox",
                title: "允许流程发起人终止",
                name: "allowTerminate"
            }, {
                xtype: "CheckBox",
                title: "允许撤回",
                name: "allowPreUndo"
            }, {
                xtype: "CheckBox",
                title: "允许驳回",
                name: "allowReject"
            }]);
        }
        return {
            title: "常规",
            xtype: "FormPanel",
            id: "normal",
            padding: 10,
            defaultConfig: {
                width: 300,
                xtype: "TextField",
                labelWidth: 150,
                colon: false
            },
            style: {
                padding: "10px 30px"
            },
            items: items
        };
    },
    getExcutorTab: function () {
        var g = this;
        return {
            xtype: "FormPanel",
            title: "执行人",
            height: 375,
            width: 535,
            id: "excutor",
            itemspace: 0,
            items: [{
                xtype: "Container",
                height: 65,
                width: 532,
                padding: 0,
                border: false,
                items: [this.initUserTypeGroup()]
            }, {
                xtype: "Container",
                width: 532,
                height: 290,
                padding: 0,
                id: "gridBox",
                hidden: true,
                defaultConfig: {
                    border: true,
                    height: 240,
                    width: 520,
                },
                items: [{
                    xtype: "ToolBar",
                    region: "north",
                    height: 40,
                    padding: 0,
                    border: false,
                    items: [{
                        xtype: "Button",
                        title: "选择岗位",
                        iconCss: "ecmp-common-choose",
                        id: "chooseBtn",
                        handler: function () {
                            var userType = EUI.getCmp("userType").getValue().userType;
                            if (userType == "Position") {
                                g.showSelectPositionWindow();
                            } else if (userType == "PositionType") {
                                g.showSelectPositionTypeWindow();
                            }
                        }
                    }]
                }, this.getPositionGrid(), this.getPositionTypeGrid(), this.getSelfDef()]
            }]
        };
    },
    initUserTypeGroup: function () {
        var g = this;
        return {
            xtype: "RadioBoxGroup",
            title: "执行人类型",
            labelWidth: 100,
            name: "userType",
            id: "userType",
            defaultConfig: {
                labelWidth: 100
            },
            items: [{
                title: "流程发起人",
                name: "StartUser",
                checked: true,
                onChecked: function (value) {
                    g.showChooseUserGrid(this.name);
                }
            }, {
                title: "指定岗位",
                name: "Position",
                onChecked: function (value) {
                    g.showChooseUserGrid(this.name);
                }
            }, {
                title: "指定岗位类别",
                name: "PositionType",
                onChecked: function (value) {
                    g.showChooseUserGrid(this.name);
                }
            }, {
                title: "自定义执行人",
                name: "SelfDefinition",
                onChecked: function (value) {
                    g.showChooseUserGrid(this.name);
                }
            }, {
                title: "任意执行人",
                name: "AnyOne",
                onChecked: function (value) {
                    g.showChooseUserGrid(this.name);
                }
            }]
        };
    },
    showChooseUserGrid: function (userType, data) {
        if (userType == "StartUser") {
            var grid = EUI.getCmp("gridBox");
            grid && grid.hide();
        }
        else if (userType == "Position") {
            EUI.getCmp("chooseBtn").show();
            EUI.getCmp("gridBox").show();
            EUI.getCmp("positionGrid").show();
            EUI.getCmp("positionTypeGrid").hide();
            EUI.getCmp("selfDef").hide();
            EUI.getCmp("chooseBtn").setTitle("选择岗位");
            if (data && data.rowdata) {
                EUI.getCmp("positionGrid").setDataInGrid(data.rowdata);
            }
        }
        else if (userType == "PositionType") {
            EUI.getCmp("gridBox").show();
            EUI.getCmp("chooseBtn").show();
            EUI.getCmp("positionGrid").hide();
            EUI.getCmp("positionTypeGrid").show();
            EUI.getCmp("selfDef").hide();
            EUI.getCmp("chooseBtn").setTitle("选择岗位类别");
            if (data && data.rowdata) {
                EUI.getCmp("positionTypeGrid").setDataInGrid(data.rowdata);
            }
        } else if (userType == "SelfDefinition") {
            EUI.getCmp("gridBox").show();
            EUI.getCmp("chooseBtn").hide();
            EUI.getCmp("positionGrid").hide();
            EUI.getCmp("positionTypeGrid").hide();
            EUI.getCmp("selfDef").show();
            EUI.getCmp("selfDef").loadData(data);
        } else if (userType == "AnyOne") {
            EUI.getCmp("gridBox").hide();
        }
    },
    getEventTab: function () {
        return {
            xtype: "FormPanel",
            title: "事件",
            id: "event",
            padding: 20,
            items: [{
                xtype: "ComboBox",
                name: "beforeExcuteService",
                field: ["beforeExcuteServiceId"],
                title: "任务执行前",
                colon: false,
                labelWidth: 100,
                width: 220,
                store: {
                    url: _ctxPath + "/design/listAllServiceUrl",
                    params: {
                        "busModelId": this.businessModelId
                    }
                },
                reader: {
                    name: "name",
                    field: ["id"]
                }
            }, {
                xtype: "ComboBox",
                name: "afterExcuteService",
                field: ["afterExcuteServiceId"],
                title: "任务执行后",
                colon: false,
                labelWidth: 100,
                width: 220,
                store: {
                    url: _ctxPath + "/design/listAllServiceUrl",
                    params: {
                        "busModelId": this.businessModelId
                    }
                },
                reader: {
                    name: "name",
                    field: ["id"]
                }
            }]
        };
    },
    getNotifyTab: function () {
        return {
            title: "通知",
            padding: 10,
            style: {
                "position": "relative"
            },
            defaultConfig: {
                width: 300,
                xtype: "TextField",
                colon: false
            },
            html: '<div class="notify-west">' +
            '<div class="west-navbar select-navbar">任务达到时</div>' +
            '<div class="west-navbar">任务执行后</div>' +
            '</div>' +
            '<div class="notify-center">' +
            '<div class="notify-user">' +
            '<div class="notify-user-item select">通知执行人</div>' +
            '<div class="notify-user-item">通知发起人</div>' +
            '<div class="notify-user-item">通知岗位</div>' +
            '</div>' +
            '<div id="notify-before"></div>' +
            '</div>' +
            '<div class="notify-center" style="display: none;">' +
            '<div class="notify-user">' +
            '<div class="notify-user-item select">通知执行人</div>' +
            '<div class="notify-user-item">通知发起人</div>' +
            '<div class="notify-user-item">通知岗位</div>' +
            '</div>' +
            '<div id="notify-after"></div>' +
            '</div>'
        };
    },
    initNotify: function () {
        this.nowNotifyTab = EUI.Container({
            width: 445,
            height: 330,
            padding: 12,
            renderTo: "notify-before",
            itemspace: 0,
            defaultConfig: {
                iframe: false,
                xtype: "FormPanel",
                width: 425,
                height: 305,
                padding: 0,
                itemspace: 10
            },
            items: [{
                items: this.getNotifyItem()
            }, {
                hidden: true,
                items: this.getNotifyItem()
            }, {
                hidden: true,
                //  items: this.getNotifyItem()
                items: this.getNotifyChoosePositionItem("notifyBefore")
            }]
        });
        var nextTab = EUI.Container({
            width: 445,
            height: 330,
            padding: 12,
            itemspace: 0,
            renderTo: "notify-after",
            defaultConfig: {
                iframe: false,
                xtype: "FormPanel",
                width: 425,
                height: 305,
                padding: 0,
                itemspace: 10
            },
            items: [{
                items: this.getNotifyItem()
            }, {
                hidden: true,
                items: this.getNotifyItem()
            }, {
                hidden: true,
                // items: this.getNotifyItem()
                items: this.getNotifyChoosePositionItem("notifyAfter")
            }]
        });
    },
    getNotifyChoosePositionItem: function (notifyType) {
        var g = this;
        if (notifyType == "notifyBefore") {
            if (!this.notifyBeforePositionData) {
                var choosePositionNum = 0
            } else {
                var choosePositionNum = this.notifyBeforePositionData.length
            }
        }
        if (notifyType == "notifyAfter") {
            if (!this.notifyAfterPositionData) {
                var choosePositionNum = 0
            } else {
                var choosePositionNum = this.notifyAfterPositionData.length
            }
        }
        return [{
            xtype: "CheckBoxGroup",
            title: "通知方式",
            labelWidth: 80,
            name: "type",
            defaultConfig: {
                labelWidth: 40
            },
            items: [{
                title: "邮件",
                name: "EMAIL"
            }, {
                title: "短信",
                name: "SMS"
            }, {
                title: "APP",
                name: "APP"
            }]
        }, {
            xtype: "Button",
            id: notifyType + "ChoosePositionBtn",
            width: 85,
            height: 25,
            title: "选择岗位(" + '<a class=' + notifyType + 'notifyChoosePositionNum>' + choosePositionNum + '</a>)',
            style: {
                "margin-left": "305px",
                "position": "absolute",
                "top": "67px"
            },
            handler: function () {
                var nowChooseBtnId = $(this).attr("id");
                var notifyChoosePositionGridData = null;
                if (nowChooseBtnId.indexOf("notifyBefore") == 0) {
                    notifyChoosePositionGridData = g.notifyBeforePositionData
                }
                if (nowChooseBtnId.indexOf("notifyAfter") == 0) {
                    notifyChoosePositionGridData = g.notifyAfterPositionData
                }
                g.showNotifySelectPositionWindow(function (data) {
                    if (nowChooseBtnId.indexOf("notifyBefore") == 0) {
                        g.notifyBeforePositionData = data;
                        $(".notifyBeforenotifyChoosePositionNum").html(data.length);
                    }
                    if (nowChooseBtnId.indexOf("notifyAfter") == 0) {
                        g.notifyAfterPositionData = data;
                        $(".notifyAfternotifyChoosePositionNum").html(data.length);
                    }
                    g.notifySelectPositionWin.close()
                }, notifyChoosePositionGridData);
            }
        }, {
            xtype: "TextArea",
            width: 320,
            height: 210,
            labelWidth: 80,
            title: "通知备注",
            name: "content"
        }];
    },
    getNotifyItem: function () {
        return [{
            xtype: "CheckBoxGroup",
            title: "通知方式",
            labelWidth: 80,
            name: "type",
            defaultConfig: {
                labelWidth: 40
            },
            items: [{
                title: "邮件",
                name: "EMAIL"
            }, {
                title: "短信",
                name: "SMS"
            }, {
                title: "APP",
                name: "APP"
            }]
        }, {
            xtype: "TextArea",
            width: 320,
            height: 220,
            labelWidth: 80,
            title: "通知备注",
            name: "content"
        }];
    },
    getNotifyData: function () {
        var data = {};
        var notifyTab1 = EUI.getCmp("notify-before");
        var notifyTab2 = EUI.getCmp("notify-after");
        var beforePosition = EUI.getCmp(notifyTab1.items[2]).getFormValue();
        var afterPosition = EUI.getCmp(notifyTab2.items[2]).getFormValue();
        beforePosition.positionData = this.notifyBeforePositionData || [];
        beforePosition.positionIds = this.getNotifyChoosePositionIds(this.notifyBeforePositionData);
        afterPosition.positionData = this.notifyAfterPositionData || [];
        afterPosition.positionIds = this.getNotifyChoosePositionIds(this.notifyAfterPositionData);
        data.before = {
            notifyExecutor: EUI.getCmp(notifyTab1.items[0]).getFormValue(),
            notifyStarter: EUI.getCmp(notifyTab1.items[1]).getFormValue(),
            notifyPosition: beforePosition
        };
        data.after = {
            notifyExecutor: EUI.getCmp(notifyTab2.items[0]).getFormValue(),
            notifyStarter: EUI.getCmp(notifyTab2.items[1]).getFormValue(),
            notifyPosition: afterPosition
        };
        return data;
    },
    getNotifyChoosePositionIds: function (data) {
        var notifyChoosePositionIds = [];
        if (data) {
            for (var i = 0; i < data.length; i++) {
                notifyChoosePositionIds.push(data[i].id)
            }
        }
        return notifyChoosePositionIds;
    },
    getPositionGrid: function () {
        var colModel = [{
            label: this.lang.operateText,
            name: "id",
            index: "id",
            width: 60,
            align: "center",
            formatter: function (cellvalue, options, rowObject) {
                // return "<div class='condetail-operate'>" +
                //     "<div class='condetail-delete' title='删除' id='" + cellvalue + "'></div></div>";
                return "<div class='ecmp-common-delete condetail-delete' title='删除' id='" + cellvalue + "'></div>";
            }
        }];
        colModel = colModel.concat(this.positionGridColModel());
        return {
            xtype: "GridPanel",
            id: "positionGrid",
            gridCfg: {
                loadonce: true,
                hasPager: false,
                // url: _ctxPath + "",
                colModel: colModel
            }
        };
    },
    positionGridColModel: function () {
        return [{
            name: "id",
            index: "id",
            hidden: true
        }, {
            label: this.lang.codeText,
            name: "code",
            index: "code",
            width: 100
        }, {
            label: this.lang.nameText,
            name: "name",
            index: "name",
            width: 150

        }, {
            label: this.lang.organizationText,
            name: "organization.name",
            index: "organization.name",
            width: 150

        }];
    },
    positionTypeGridColModel: function () {
        return [{
            name: "id",
            index: "id",
            hidden: true
        }, {
            label: this.lang.codeText,
            name: "code",
            index: "code",
            width: 100
        }, {
            label: this.lang.nameText,
            name: "name",
            index: "name",
            width: 150
        }];
    },
    getPositionTypeGrid: function () {
        var colModel = [{
            label: this.lang.operateText,
            name: "id",
            index: "id",
            width: 60,
            align: "center",
            formatter: function (cellvalue, options, rowObject) {
                return "<div class='ecmp-common-delete condetail-delete' title='删除' id='" + cellvalue + "'></div>";
            }
        }];
        colModel = colModel.concat(this.positionTypeGridColModel());
        return {
            xtype: "GridPanel",
            hidden: true,
            id: "positionTypeGrid",
            gridCfg: {
                loadonce: true,
                hasPager: false,
                colModel: colModel
            }
        };
    },
    getSelfDefGridColModel: function () {
        return [{
            name: "id",
            index: "id",
            hidden: true
        }, {
            label: "员工编号",
            name: "code",
            index: "code",
            width: 100
        }, {
            label: this.lang.nameText,
            name: "userName",
            index: "userName",
            width: 150
        }];
    },
    getSelfDef: function () {
        return {
            xtype: "ComboBox",
            id: "selfDef",
            name: "name",
            title: "自定义执行人类型",
            labelWidth: 140,
            height: 18,
            allowBlank: false,
            width: 340,
            hidden: true,
            field: ["selfDefUserUrl"],
            store: {
                url: _ctxPath + "/flowExecutorConfig/listCombo",
                params: {
                    "Q_EQ_businessModel.id": this.businessModelId
                }
            }
        };
    },
    showSelectPositionWindow: function () {
        var g = this;
        var win = EUI.Window({
            title: "选择岗位",
            padding: 0,
            width: 1000,
            height: 350,
            buttons: [{
                title: "确定",
                iconCss: "ecmp-common-ok",
                selected: true,
                handler: function () {
                    var cmp = EUI.getCmp("positionGrid");
                    var selectRow = EUI.getCmp("selPositionGrid").getGridData();
                    cmp.reset();
                    cmp.setDataInGrid(selectRow, false);
                    win.close();
                }
            }, {
                title: "取消",
                iconCss: "ecmp-common-delete",
                handler: function () {
                    win.close();
                }
            }],
            items: [{
                xtype: "Container",
                layout: "auto",
                border: false,
                padding: 0,
                itemspace: 1,
                items: [{
                    xtype: "Container",
                    region: "west",
                    layout: "border",
                    border: false,
                    padding: 0,
                    width: 470,
                    itemspace: 0,
                    isOverFlow: false,
                    items: [this.initTitle("已选择"), {
                        xtype: "GridPanel",
                        id: "selPositionGrid",
                        region: "center",
                        gridCfg: {
                            datatype: "local",
                            loadonce: true,
                            multiselect: true,
                            sortname: 'code',
                            colModel: this.positionGridColModel(),
                            ondblClickRow: function (rowid) {
                                var cmp = EUI.getCmp("selPositionGrid");
                                var row = cmp.grid.jqGrid('getRowData', rowid);
                                if (!row) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                g.deleteRowData([row], cmp);
                            }
                        }
                    }]
                }, g.getCenterIcon("position"), {
                    xtype: "Container",
                    layout: "border",
                    border: false,
                    padding: 0,
                    itemspace: 0,
                    width: 470,
                    region: "east",
                    items: [{
                        xtype: "ToolBar",
                        region: "north",
                        height: 35,
                        padding: 2,
                        border: false,
                        isOverFlow: false,
                        items: [this.initTitle("所有岗位"), "->", {
                            xtype: "SearchBox",
                            id: "searchBox_positionGrid",
                            width: 200,
                            //searchDisplayText:请输入代码、名称或组织机构查询
                            displayText: "请输入代码、名称或组织机构查询",
                            onSearch: function (v) {
                                EUI.getCmp("allPositionGrid").setPostParams({
                                    Quick_value: v,
                                }, true);
                            },
                            afterClear: function () {
                                EUI.getCmp("allPositionGrid").setPostParams({
                                    Quick_value: null,
                                }, true);
                            }
                        }]
                    }, {
                        xtype: "GridPanel",
                        id: "allPositionGrid",
                        region: "center",
                        searchConfig: {
                            searchCols: ["code", "name"]
                        },
                        gridCfg: {
                            hasPager: true,
                            multiselect: true,
                            loadonce: false,
                            sortname: 'code',
                            url: _ctxPath + "/design/listPos",
                            colModel: this.positionGridColModel(),
                            ondblClickRow: function (rowid) {
                                var selectRow = EUI.getCmp("allPositionGrid").grid.jqGrid('getRowData', rowid);
                                if (!selectRow) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                EUI.getCmp("selPositionGrid").addRowData([selectRow], true);
                            }
                        }
                    }]
                }]
            }]
        });
        var data = EUI.getCmp("positionGrid").getGridData();
        EUI.getCmp("selPositionGrid").reset();
        EUI.getCmp("selPositionGrid").setDataInGrid(data, false);
        this.addPositionEvent();
    },
    addPositionEvent: function () {
        var g = this;
        $("#position-left").bind("click", function (e) {
            var cmp = EUI.getCmp("selPositionGrid");
            var selectRow = EUI.getCmp("allPositionGrid").getSelectRow();
            if (selectRow.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            cmp.addRowData(selectRow, true);
        });
        $("#position-right").bind("click", function (e) {
            var cmp = EUI.getCmp("selPositionGrid");
            var row = cmp.getSelectRow();
            if (row.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            g.deleteRowData(row, cmp);
        });
    },
    deleteRowData: function (data, cmp) {
        var g = this;
        for (var i = 0; i < data.length; i++) {
            cmp.deleteRow(data[i].id);
        }
        cmp.setDataInGrid([].concat(cmp.getGridData()), false);
    },
    showSelectPositionTypeWindow: function () {
        var g = this;
        var win = EUI.Window({
            title: "选择岗位类别",
            padding: 0,
            width: 1000,
            height: 350,
            buttons: [{
                title: "确定",
                iconCss: "ecmp-common-ok",
                selected: true,
                handler: function () {
                    var cmp = EUI.getCmp("positionTypeGrid");
                    var selectRow = EUI.getCmp("selPositionTypeGrid").getGridData();
                    cmp.reset();
                    cmp.setDataInGrid(selectRow, false);
                    win.close();
                }
            }, {
                title: "取消",
                iconCss: "ecmp-common-delete",
                handler: function () {
                    win.close();
                }
            }],
            items: [{
                xtype: "Container",
                layout: "auto",
                border: false,
                padding: 0,
                itemspace: 1,
                items: [{
                    xtype: "Container",
                    region: "west",
                    layout: "border",
                    border: false,
                    padding: 0,
                    width: 470,
                    itemspace: 0,
                    isOverFlow: false,
                    items: [this.initTitle("已选择"), {
                        xtype: "GridPanel",
                        id: "selPositionTypeGrid",
                        region: "center",
                        gridCfg: {
                            datatype: "local",
                            loadonce: true,
                            hasPager: false,
                            multiselect: true,
                            sortname: 'code',
                            colModel: this.positionTypeGridColModel(),
                            ondblClickRow: function (rowid) {
                                var cmp = EUI.getCmp("selPositionTypeGrid");
                                var row = cmp.grid.jqGrid('getRowData', rowid);
                                if (!row) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                g.deleteRowData([row], cmp);
                            }
                        }
                    }]
                }, this.getCenterIcon("positionType"), {
                    xtype: "Container",
                    layout: "border",
                    border: false,
                    padding: 0,
                    itemspace: 0,
                    width: 470,
                    region: "east",
                    items: [{
                        xtype: "ToolBar",
                        region: "north",
                        height: 35,
                        padding: 2,
                        border: false,
                        isOverFlow: false,
                        items: [this.initTitle("所有岗位类别"), "->", {
                            xtype: "SearchBox",
                            id: "searchBox_positionTypeGrid",
                            width: 200,
                            //searchDisplayText:请输入代码或名称查询
                            displayText: "请输入代码或名称查询",
                            onSearch: function (v) {
                                EUI.getCmp("allPositionTypeGrid").localSearch(v);
                            },
                            afterClear: function () {
                                EUI.getCmp("allPositionTypeGrid").restore();
                            }
                        }]
                    }, {
                        xtype: "GridPanel",
                        region: "center",
                        id: "allPositionTypeGrid",
                        gridCfg: {
                            multiselect: true,
                            hasPager: false,
                            sortname: 'code',
                            datatype: "local",
                            loadonce: true,
                            url: _ctxPath + "/design/listPosType",
                            colModel: this.positionTypeGridColModel(),
                            ondblClickRow: function (rowid) {
                                var selectRow = EUI.getCmp("allPositionTypeGrid").grid.jqGrid('getRowData', rowid);
                                if (!selectRow) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                EUI.getCmp("selPositionTypeGrid").addRowData([selectRow], true);
                            }
                        }
                    }]
                }]
            }]
        });
        var data = EUI.getCmp("positionTypeGrid").getGridData();
        EUI.getCmp("selPositionTypeGrid").reset();
        EUI.getCmp("selPositionTypeGrid").setDataInGrid(data, false);
        this.addPositionTypeEvent();
    },
    getCenterIcon: function (id) {
        var g = this;
        return {
            xtype: "Container",
            region: "center",
            width: 60,
            border: false,
            isOverFlow: false,
            html: "<div class='ecmp-common-moveright arrow-right' id=" + id + "-right></div>" +
            "<div class='ecmp-common-leftmove arrow-left' id=" + id + "-left></div>"
        }
    },
    addPositionTypeEvent: function () {
        var g = this;
        $("#positionType-left").bind("click", function (e) {
            var cmp = EUI.getCmp("selPositionTypeGrid");
            var selectRow = EUI.getCmp("allPositionTypeGrid").getSelectRow();
            if (selectRow.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            cmp.addRowData(selectRow, true);
        });
        $("#positionType-right").bind("click", function (e) {
            var cmp = EUI.getCmp("selPositionTypeGrid");
            var row = cmp.getSelectRow();
            if (row.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            g.deleteRowData(row, cmp);
        });
    },
    checkExcutor: function () {
        var userType = EUI.getCmp("userType").getValue().userType;
        var data;
        if (userType == "Position") {
            data = EUI.getCmp("positionGrid").getGridData();
        } else if (userType == "PositionType") {
            data = EUI.getCmp("positionTypeGrid").getGridData();
        } else if (userType == "SelfDefinition") {
            data = EUI.getCmp("selfDefGrid").getGridData();
        } else {
            return true;
        }
        if (!data || data.length == 0) {
            return false;
        }
        return true;
    },
    getExcutorData: function () {
        var data = EUI.getCmp("userType").getValue();
        var userType = data.userType;
        if (userType == "Position") {
            rowdata = EUI.getCmp("positionGrid").getGridData();
            data.ids = this.getSelectIds(rowdata);
            data.rowdata = rowdata;
        } else if (userType == "PositionType") {
            rowdata = EUI.getCmp("positionTypeGrid").getGridData();
            data.ids = this.getSelectIds(rowdata);
            data.rowdata = rowdata;
        } else if (userType == "SelfDefinition") {
            rowdata = EUI.getCmp("selfDefGrid").getGridData();
            data.ids = this.getSelectIds(rowdata);
            data.rowdata = rowdata;
        }
        return data;
    },
    getSelectIds: function (data) {
        var ids = "";
        for (var i = 0; i < data.length; i++) {
            if (i > 0) {
                ids += ",";
            }
            ids += data[i].id;
        }
        return ids;
    },
    loadData: function () {
        var g = this;
        var normalForm = EUI.getCmp("normal");
        var executorForm = EUI.getCmp("excutor");
        var eventForm = EUI.getCmp("event");
        var notifyForm = EUI.getCmp("notify");
        if (!this.data) {
            return;
        }
        //加载常规配置
        normalForm.loadData(this.data.normal);

        //加载执行人配置
        var userType = this.data.executor.userType;
        var userTypeCmp = EUI.getCmp("userType");
        userTypeCmp.setValue(userType);
        this.showChooseUserGrid(userType, this.data.executor);

        //加载事件配置
        eventForm.loadData(this.data.event);

        //加载通知配置
        if (!this.data.notify) {
            return;
        }
        var notifyBefore = EUI.getCmp("notify-before");
        var notifyAfter = EUI.getCmp("notify-after");
        this.loadNotifyData(notifyBefore, this.data.notify.before);
        this.loadNotifyData(notifyAfter, this.data.notify.after);

        this.loadNotifyChoosePositonData(this.data);
        this.notifyBeforePositionData = this.data.notify.before.notifyPosition.positionData;
        this.notifyAfterPositionData = this.data.notify.after.notifyPosition.positionData;
    },
    loadNotifyData: function (tab, data) {
        EUI.getCmp(tab.items[0]).loadData(data.notifyExecutor);
        EUI.getCmp(tab.items[1]).loadData(data.notifyStarter);
        EUI.getCmp(tab.items[2]).loadData(data.notifyPosition);
    },
    loadNotifyChoosePositonData: function (data) {
        if (!data.notify.before.notifyPosition.positionData) {
            $(".notifyBeforenotifyChoosePositionNum").html(0);
        } else {
            $(".notifyBeforenotifyChoosePositionNum").html(data.notify.before.notifyPosition.positionData.length);
        }
        if (!data.notify.after.notifyPosition.positionData) {
            $(".notifyAfternotifyChoosePositionNum").html(0);
        } else {
            $(".notifyAfternotifyChoosePositionNum").html(data.notify.after.notifyPosition.positionData.length);
        }
    },
    initTitle: function (title) {
        return {
            xtype: "Container",
            region: "north",
            border: false,
            height: 35,
            width: 110,
            isOverFlow: false,
            html: "<div style='font-size:15px;overflow:hidden;'>" + title + "</div>"
        }
    },
    message: function (msg) {
        var g = this;
        var message = EUI.MessageBox({
            border: true,
            title: "提示",
            showClose: true,
            msg: msg,
            buttons: [{
                title: "确定",
                iconCss: "ecmp-common-ok",
                handler: function () {
                    message.remove();
                }
            }]
        });
    },
    remove: function () {
        EUI.getCmp("notify-before").remove();
        EUI.getCmp("notify-after").remove();
        $(".condetail-delete").die();
        $(".west-navbar").die();
        $(".notify-user-item").die();
    },
    showNotifySelectPositionWindow: function (callback, notifyChoosePositionGridData) {
        var g = this;
        g.notifySelectPositionWin = EUI.Window({
            title: "选择岗位",
            padding: 0,
            width: 1000,
            height: 350,
            buttons: [{
                title: "确定",
                iconCss: "ecmp-common-ok",
                selected: true,
                handler: function () {
                    var selectRow = EUI.getCmp("notifyChoosePositionGrid").getGridData();
                    callback && callback.call(this, selectRow);
                }
            }, {
                title: "取消",
                iconCss: "ecmp-common-delete",
                handler: function () {
                    g.notifySelectPositionWin.close();
                }
            }],
            items: [{
                xtype: "Container",
                layout: "auto",
                border: false,
                padding: 0,
                itemspace: 1,
                items: [{
                    xtype: "Container",
                    region: "west",
                    layout: "border",
                    border: false,
                    padding: 0,
                    width: 470,
                    itemspace: 0,
                    isOverFlow: false,
                    items: [this.initTitle("已选择"), {
                        xtype: "GridPanel",
                        id: "notifyChoosePositionGrid",
                        region: "center",
                        gridCfg: {

                            datatype: "local",
                            loadonce: true,
                            multiselect: true,
                            sortname: 'code',
                            colModel: this.positionGridColModel(),
                            ondblClickRow: function (rowid) {
                                var cmp = EUI.getCmp("notifyChoosePositionGrid");
                                var row = cmp.grid.jqGrid('getRowData', rowid);
                                if (!row) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                g.deleteRowData([row], cmp);
                            }
                        }
                    }]
                }, g.getCenterIcon("notifyPosition"), {
                    xtype: "Container",
                    layout: "border",
                    border: false,
                    padding: 0,
                    itemspace: 0,
                    width: 470,
                    region: "east",
                    items: [{
                        xtype: "ToolBar",
                        region: "north",
                        height: 35,
                        padding: 2,
                        border: false,
                        isOverFlow: false,
                        items: [this.initTitle("所有岗位"), "->", {
                            xtype: "SearchBox",
                            id: "searchBox_positionGrid",
                            width: 200,
                            //searchDisplayText:请输入代码、名称或组织机构查询
                            displayText: "请输入代码、名称或组织机构查询",
                            onSearch: function (v) {
                                EUI.getCmp("notifyAllPositionGrid").setPostParams({
                                    Quick_value: v
                                }, true);
                            },
                            afterClear: function () {
                                EUI.getCmp("notifyAllPositionGrid").setPostParams({
                                    Quick_value: null
                                }, true);
                            }
                        }]
                    }, {
                        xtype: "GridPanel",
                        id: "notifyAllPositionGrid",
                        region: "center",
                        searchConfig: {
                            searchCols: ["code", "name"]
                        },
                        gridCfg: {
                            hasPager: true,
                            multiselect: true,
                            loadonce: false,
                            sortname: 'code',
                            url: _ctxPath + "/design/listPos",
                            colModel: this.positionGridColModel(),
                            ondblClickRow: function (rowid) {
                                var selectRow = EUI.getCmp("notifyAllPositionGrid").grid.jqGrid('getRowData', rowid);
                                if (!selectRow) {
                                    g.message("请选择一条要操作的行项目!");
                                    return false;
                                }
                                EUI.getCmp("notifyChoosePositionGrid").addRowData([selectRow], true);
                            }
                        }
                    }]
                }]
            }]
        });
        EUI.getCmp("notifyChoosePositionGrid").reset();
        if (!notifyChoosePositionGridData) {
            EUI.getCmp("notifyChoosePositionGrid").setDataInGrid([], false);
        } else {
            EUI.getCmp("notifyChoosePositionGrid").setDataInGrid(notifyChoosePositionGridData, false);
        }
        this.notifyAddPositionEvent();
    },
    notifyAddPositionEvent: function (notifyType) {
        var g = this;
        $("#notifyPosition-left").bind("click", function (e) {
            var cmp = EUI.getCmp("notifyChoosePositionGrid");
            var selectRow = EUI.getCmp("notifyAllPositionGrid").getSelectRow();
            if (selectRow.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            cmp.addRowData(selectRow, true);
        });
        $("#notifyPosition-right").bind("click", function (e) {
            var cmp = EUI.getCmp("notifyChoosePositionGrid");
            var row = cmp.getSelectRow();
            if (row.length == 0) {
                g.message("请选择一条要操作的行项目!");
                return false;
            }
            g.deleteRowData(row, cmp);
        });
    }
});