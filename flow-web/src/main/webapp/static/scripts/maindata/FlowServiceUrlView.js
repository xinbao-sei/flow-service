/**
 * 显示页面
 */
EUI.FlowServiceUrlView = EUI.extend(EUI.CustomUI, {
    businessModelId: "",
    businessModelName: "",
    initComponent: function () {
        EUI.Container({
            renderTo: this.renderTo,
            layout: "border",
            border: false,
            padding: 8,
            itemspace: 0,
            items: [this.initTbar(), this.initGrid()]
        });
        this.addEvents();
    },
    initTbar: function () {
        var g = this;
        return {
            xtype: "ToolBar",
            region: "north",
            height: 40,
            padding: 0,
            isOverFlow: false,
            border: false,
            items: [{
                // xtype: "ComboBox",
                // title: "<span style='font-weight: bold'>" + g.lang.businessEntityText + "</span>",
                // id: "coboId",
                // async: false,
                // colon: false,
                // labelWidth: 70,
                // store: {
                //     url: _ctxPath + "/flowType/listAllBusinessModel"
                // },
                // reader: {
                //     name: "name",
                //     filed: ["id"]
                // },
                // afterLoad: function (data) {
                //     if (!data) {
                //         return;
                //     }
                //     var cobo = EUI.getCmp("coboId");
                //     cobo.setValue(data[0].name);
                //     g.businessModelId = data[0].id;
                //     g.businessModelName = data[0].name;
                //     var gridPanel = EUI.getCmp("gridPanel").setGridParams({
                //         url: _ctxPath + "/flowServiceUrl/listServiceUrl",
                //         loadonce: false,
                //         datatype: "json",
                //         postData: {
                //             "Q_EQ_businessModel.id": data[0].id
                //         }
                //     }, true)
                // },
                // afterSelect: function (data) {
                //     g.businessModel = data.data.id;
                //     g.businessModelName = data.data.name;
                //     EUI.getCmp("gridPanel").setPostParams({
                //             "Q_EQ_businessModel.id": data.data.id
                //         }
                //     ).trigger("reloadGrid");
                // }
                xtype: "ComboGrid",
                title: "<span style='font-weight: bold'>" + g.lang.businessEntityText + "</span>",
                name: "bussinessModelName",
                id: "coboId",
                //async: false,
                colon: false,
                field: ["id"],
                listWidth: 400,
                labelWidth: 85,
                editable:true,
                value:g.lang.totalText,
                showSearch:true,
                onSearch:function(value){
                    this.grid.localSearch(value);
                },
                gridCfg: {
                    url: _ctxPath + "/flowType/listAllBusinessModel",
                    loadonce:true,
                    // loadComplete: function (data) {
                    //     console.log(data.rows[0]);
                    //     if (typeof(data.rows[0]) == "undefined") {
                    //         return;
                    //     }
                    //     var cobo = EUI.getCmp("coboId");
                    //     cobo.setValue(data.rows[0].name);
                    //     g.businessModel = data.rows[0].id;
                    //     g.businessModelName = data.rows[0].name;
                    //     var gridPanel = EUI.getCmp("gridPanel").setGridParams({
                    //         url: _ctxPath + "/flowType/listFlowType",
                    //         loadonce: false,
                    //         datatype: "json",
                    //         postData: {
                    //             "Q_EQ_businessModel.id": data.rows[0].id
                    //         }
                    //     }, true)
                    // },
                    colModel: [{
                        name: "id",
                        index: "id",
                        hidden: true
                    }, {
                        label: this.lang.nameText,
                        name: "name",
                        index: "name"
                    }]
                },
                reader: {
                    name: "name",
                    filed: ["id"]
                },
                afterSelect: function (data) {
                    console.log(data);
                    g.businessModel = data.data.id;
                    g.businessModelName = data.data.name;
                    EUI.getCmp("gridPanel").setPostParams({
                        "Q_EQ_businessModel.id": data.data.id
                    },true);
                },
                afterClear:function(){
                    var cobo = EUI.getCmp("coboId");
                    cobo.setValue(g.lang.totalText);
                    EUI.getCmp("gridPanel").setPostParams({
                        "Q_EQ_businessModel.id": null
                    },true);
                }
            }, {
                xtype: "Button",
                title: this.lang.addResourceText,
                selected: true,
                handler: function () {
                    g.addFlowServiceUrl();
                }
            }, '->', {
                xtype: "SearchBox",
                displayText: g.lang.searchByNameMsgText,
                onSearch: function (value) {
                    console.log(value);
                    if (!value) {
                        EUI.getCmp("gridPanel").setPostParams({
                                Q_LK_name: ""
                            }
                        ).trigger("reloadGrid");
                    }
                    EUI.getCmp("gridPanel").setPostParams({
                            Q_LK_name: value
                        }
                    ).trigger("reloadGrid");
                }
            }]
        };
    },
    initGrid: function () {
        var g = this;
        return {
            xtype: "GridPanel",
            region: "center",
            id: "gridPanel",
            style: {
                "border-reduis": "3px"
            },
            gridCfg: {
                //loadonce: true,
                url: _ctxPath + "/flowServiceUrl/listServiceUrl",
                colModel: [{
                    label: this.lang.operateText,
                    name: "operate",
                    index: "operate",
                    width: "50%",
                    align: "center",
                    formatter: function (cellvalue, options, rowObject) {
                        var strVar = "<div class='condetail_operate'>"
                            + "<div class='condetail_update' title='"+g.lang.editText+"'></div>"
                            + "<div class='condetail_delete' title='"+g.lang.deleteText+"'></div></div>";
                        return strVar;
                    }
                }, {
                    name: "id",
                    index: "id",
                    hidden: true
                }, {
                    label: this.lang.codeText,
                    name: "code",
                    index: "code"
                }, {
                    label: this.lang.nameText,
                    name: "name",
                    index: "name"
                }, {
                    label: "URL",
                    name: "url",
                    index: "url"
                }, {
                    label: this.lang.depictText,
                    name: "depict",
                    index: "depict"
                },{
                    label: "businessModelId",
                    name: "businessModel.id",
                    index: "businessModel.id",
                    hidden: true
                }, {
                    label: g.lang.businessEntityModelText,
                    name: "businessModel.name",
                    index: "businessModel.name"
                }],
                ondbClick: function () {
                    var rowData = EUI.getCmp("gridPanel").getSelectRow();
                    g.getValues(rowData.id);
                }
            }
        };
    },
    addEvents: function () {
        var g = this;
        $(".condetail_update").live("click", function () {
            var data = EUI.getCmp("gridPanel").getSelectRow();
            console.log(data);
            g.updateFlowServiceUrl(data);
        });
        $(".condetail_delete").live("click", function () {
            var rowData = EUI.getCmp("gridPanel").getSelectRow();
            console.log(rowData);
            g.deleteFlowServiceUrl(rowData)
        });
    },
    deleteFlowServiceUrl:function (rowData) {
      var g = this;
        var infoBox = EUI.MessageBox({
            title: g.lang.tiShiText,
            msg: g.lang.ifDelMsgText,
            buttons: [{
                title: g.lang.sureText,
                selected: true,
                handler: function () {
                    infoBox.remove();
                    var myMask = EUI.LoadMask({
                        msg: g.lang.nowDelMsgText
                    });
                    EUI.Store({
                        url: _ctxPath + "/flowServiceUrl/delete",
                        params: {
                            id: rowData.id
                        },
                        success: function (result) {
                            myMask.hide();
                            EUI.ProcessStatus(result);
                            if (result.success) {
                                EUI.getCmp("gridPanel").grid.trigger("reloadGrid");
                            }
                        },
                        failure: function (result) {
                            myMask.hide();
                            EUI.ProcessStatus(result);
                        }
                    });
                }
            }, {
                title: g.lang.cancelText,
                handler: function () {
                    infoBox.remove();
                }
            }]
        });
    },
    updateFlowServiceUrl: function (data) {
        var g = this;
        console.log(data);
        win = EUI.Window({
            title: g.lang.updateFlowServiceUrlText,
            height: 250,
            padding: 15,
            items: [{
                xtype: "FormPanel",
                id: "updateFlowServiceUrl",
                padding: 0,
                items: [{
                    xtype: "TextField",
                    title: "ID",
                    labelWidth: 90,
                    allowBlank: false,
                    name: "id",
                    width: 220,
                    hidden: true
                }, {
                    xtype: "ComboGrid",
                    title:  g.lang.businessEntityText,
                    name: "businessModel.name",
                    field: ["businessModel.id"],
                    listWidth: 400,
                    labelWidth: 90,
                    width: 220,
                    allowBlank: false,
                    showSearch:true,
                    onSearch:function(value){
                        this.grid.localSearch(value);
                    },
                    gridCfg: {
                        url: _ctxPath + "/flowType/listAllBusinessModel",
                        loadonce:true,
                        colModel: [{
                            name: "id",
                            index: "id",
                            hidden: true
                        }, {
                            label: this.lang.nameText,
                            name: "name",
                            index: "name"
                        }]
                    },
                    reader: {
                        name: "name",
                        field: ["id"]
                    }
                },{
                    xtype: "TextField",
                    title: g.lang.codeText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "code",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: g.lang.nameText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "name",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: "URL",
                    labelWidth: 90,
                    allowBlank: false,
                    name: "url",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: g.lang.depictText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "depict",
                    width: 220
                }]
            }],
            buttons: [{
                title: g.lang.saveText,
                selected: true,
                handler: function () {
                    var form = EUI.getCmp("updateFlowServiceUrl");
                    if (!form.isValid()) {
                        return;
                    }
                    var data = form.getFormValue();
                    console.log(data);
                    g.saveFlowServiceUrl(data);
                }
            }, {
                title: g.lang.cancelText,
                handler: function () {
                    win.remove();
                }
            }]
        });
        EUI.getCmp("updateFlowServiceUrl").loadData(data);
    },
    addFlowServiceUrl: function () {
        var g = this;
        win = EUI.Window({
            title: g.lang.addNewFlowServiceUrlText,
            height: 250,
            padding: 15,
            items: [{
                xtype: "FormPanel",
                id: "addFlowServiceUrl",
                padding: 0,
                items: [{
                    xtype: "ComboGrid",
                    title:  g.lang.businessEntityText,
                    name: "businessModelName",
                    field: ["businessModel.id"],
                    listWidth: 400,
                    labelWidth: 90,
                    width: 220,
                    allowBlank: false,
                    showSearch:true,
                    onSearch:function(value){
                        this.grid.localSearch(value);
                    },
                    gridCfg: {
                        url: _ctxPath + "/flowType/listAllBusinessModel",
                        loadonce:true,
                        colModel: [{
                            name: "id",
                            index: "id",
                            hidden: true
                        }, {
                            label: this.lang.nameText,
                            name: "name",
                            index: "name"
                        }]
                    },
                    reader: {
                        name: "name",
                        field: ["id"]
                    }
                },{
                    xtype: "TextField",
                    title: g.lang.codeText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "code",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: g.lang.nameText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "name",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: "URL",
                    labelWidth: 90,
                    allowBlank: false,
                    name: "url",
                    width: 220
                }, {
                    xtype: "TextField",
                    title: g.lang.depictText,
                    labelWidth: 90,
                    allowBlank: false,
                    name: "depict",
                    width: 220
                }]
            }],
            buttons: [{
                title: g.lang.saveText,
                selected: true,
                handler: function () {
                    var form = EUI.getCmp("addFlowServiceUrl");
                    if (!form.isValid()) {
                        return;
                    }
                    var data = form.getFormValue();
                    console.log(data);
                    g.saveFlowServiceUrl(data);
                }
            }, {
                title: g.lang.cancelText,
                handler: function () {
                    win.remove();
                }
            }]
        });
    },
    saveFlowServiceUrl: function (data) {
        var g = this;
        console.log(data);
        var myMask = EUI.LoadMask({
            msg: g.lang.nowSaveMsgText
        });
        EUI.Store({
            url: _ctxPath + "/flowServiceUrl/save",
            params: data,
            success: function (result) {
                myMask.hide();
                EUI.ProcessStatus(result);
                if (result.success) {
                    EUI.getCmp("gridPanel").grid.trigger("reloadGrid");
                    win.close();
                }
            },
            failure: function (result) {
                myMask.hide();
                EUI.ProcessStatus(result);
            }
        });
    }
});