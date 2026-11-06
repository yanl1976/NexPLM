// 按钮高亮 
function btnHighLight(btn) {
    if (btn.style.backgroundColor == "") {
        btn.style.backgroundColor = "#bae7ff";
        // btn.style.opacity = '.5';
    } else {
        btn.style.backgroundColor = ""
    }
}
/*------------------ 复位 -------------------*/
function excuteReset(elementId) {
    //SView.Windows.Button.highLightLabel(elementId);

    let roamService = SView.ServiceManager.getInstance().getService(SView.Services.ServiceType.RoamService, SView.Services);
    if (roamService.state == SView.ServiceState.Started) {
        //如果已经开启漫游，则禁止复位
        let uiManager = sview0.sviewFrame.getUIManager();
        MessageDialog.Message.content = SView[language].languageObj.Prompt.WalkThroughRestore;
        uiManager.load(MessageDialog);
        setTimeout(function () {
            uiManager.removeLabel(MessageDialog.Message.id);
        }, 3000);
        return;
    }
    commandClickFun('', 'restore');
    //commandClickFun('', SView[language].languageObj.RightMenuTitle.RestoreView_li);
    sview0.sviewFrame.getViewer().getScene().refresh();
}

/* ------------------ 打开视图菜单 ------------------- */
function showViewMenu(elementId) {
    this.event.stopPropagation();
    this.event.preventDefault();
    let uiManager = sview0.sviewFrame.getUIManager();
    let scene = sview0.sviewFrame.getViewer().getScene();
    let viewMenu = uiManager.getElement(ViewMenu.ViewMenu.id);
    if (viewMenu == null || !viewMenu.isShow()) {
        if (viewMenu) {
            uiManager.showLabel(ViewMenu.ViewMenu.id);
        } else {
            // 创建视图菜单
            viewMenu = uiManager.load(ViewMenu);
            //设置透视投影高亮状态
            let btn = uiManager.getElement("perspectiveView");
            if (btn) {
                let proValue = scene.getCurrentViewPort().getCamera().isOrthographic();
                btn.setHighLight(!proValue);
            }
            //设置漫游的高亮状态
            let walkingBtn = uiManager.getElement("walking");
            if (walkingBtn) {
                let proValue = M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.isOpenRoaming);
                walkingBtn.setHighLight(proValue);
            }
            var dirs = document.getElementById("solid").parentElement.parentElement.children;
            for (var i = 0; i < dirs.length; i++) {
                dirs[i].children[0].style.backgroundColor = "";
            }
            let drawMode = scene.getDrawMode();
            switch (drawMode) {
                case SView.Commands.DrawMode.NORMAL:
                    SView.Windows.Button.highLightLabel("solid");
                    break;
                case SView.Commands.DrawMode.TRANSPARENT:
                    SView.Windows.Button.highLightLabel('transparent');
                    break;
                case SView.Commands.DrawMode.TECHNICAL:
                    SView.Windows.Button.highLightLabel('wire');
                    break;
                default:
                    SView.Windows.Button.highLightLabel("solid");
            }
        }
        SView.Windows.Button.highLightLabel(elementId, true);
        if (scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Jewel)
            || scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Diamond)
            || scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Metal)
            || scene.getMaterialManager().hasSpecificTypeMaterial(102)) {
            uiManager.hideLabel("walking");
            uiManager.hideLabel("perspectiveView");
            uiManager.hideLabel("wire");
            uiManager.hideLabel("transparent");
            uiManager.showLabel("notUseButton");
        }
    } else {
        // 删除视图菜单
        uiManager.hideLabel(ViewMenu.ViewMenu.id);
        SView.Windows.Button.highLightLabel(elementId, false);
    }
}
//视图菜单点击
function selectRenderMode(id, type) {
    this.event.stopPropagation();
    this.event.preventDefault();
    var dirs = document.getElementById(id).parentElement.parentElement.children;
    for (var i = 0; i < dirs.length; i++) {
        dirs[i].children[0].style.backgroundColor = "";
    }
    SView.Windows.Button.highLightLabel(id);
    setPerspective(id);
}
let proType = true;
//基本视图点击事件
function viewDirection(type) {
    this.event.stopPropagation();
    this.event.preventDefault();
    let viewType = 0;
    if (type === "fromTop") {
        viewType = 5;
    } else if (type === "fromAxis") {
        viewType = 6;
    } else if (type === "fromLeft") {
        viewType = 3;
    } else if (type === "fromFront") {
        viewType = 0;
    } else if (type === "fromRight") {
        viewType = 2;
    } else if (type === "fromBack") {
        viewType = 1;
    } else if (type === "fromBottom") {
        viewType = 4;
    } else if (type == "perspectiveView") {
        //SView.Windows.Button.highLightLabel(type);

        let uiManager = sview0.sviewFrame.getUIManager();
        let btn = uiManager.getElement(type);
        if (btn) {
            let proValue = sview0.sviewFrame.getViewer().getScene().getCurrentViewPort().getCamera().isOrthographic();
            btn.setHighLight(proValue);
        }


        /* let paras = new SView.CommandParameters();
         paras.add(SView.Commands.ProjectionCommand.type, SView.CommandParameterType.BOOL, proType);
         proType = !proType;*/
        var commandsManager = sview0.sviewFrame.getCommandsManager();
        commandsManager.execute(/*SView.Commands.CommandNames.BASEVIEWCOMMAND*/"Projection"/*, paras*/);
        return;
    }


    let paras = new SView.CommandParameters();
    paras.add(SView.Commands.BaseViewCommand.baseViewType, SView.CommandParameterType.INT, viewType);
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    commandsManager.execute(/*SView.Commands.CommandNames.BASEVIEWCOMMAND*/"BaseView", paras);
}

//漫游模式
function selectWalkingMode(id) {
    this.event.stopPropagation();
    this.event.preventDefault();

    let commandManager = sview0.sviewFrame.getCommandsManager();
    let array = new Map();
    //组合命令相关参数
    array.set("fullClassName", "SView.Commands.RoamCommand");
    commandManager.execute("RoamCommand", array)

    //let uiManager = sview0.sviewFrame.getUIManager();
    //let btn = uiManager.getElement("perspectiveView");
    //let walkingbtn = uiManager.getElement(id);
    //let roamService = SView.ServiceManager.getRoamService(sview0.sviewFrame);
    //walkingbtn.setHighLight(roamService.state == SView.ServiceState.Started);
    //M3D.Config.Parameters.getInstance().setBoolParameter(M3D.Config.isOpenRoaming, roamService.state == SView.ServiceState.Started);  
    ////开启摇杆
    //SView.Commands.CommandConfig.getInstance().setBoolParameter(SView.Commands.CommandConfig.isJoySticks, roamService.state == SView.ServiceState.Started);
    //isShowJoyStick(roamService.state == SView.ServiceState.Started);


    //let proValue = sview0.sviewFrame.getViewer().getScene().getCurrentViewPort().getCamera().isOrthographic();
    //btn.setHighLight(!proValue);
    sview0.sviewFrame.getViewer().getScene().refresh();
}
/**
 * 是否显示虚拟摇杆
 * @param isShow
 */
//function isShowJoyStick(isShow) {
//    let uiManager = sview0.sviewFrame.getUIManager();
//    let joyStickVirtualRockerItem = uiManager.getElement(JoystickVirtualRocker.VirtualRocker.id);
//    let joyStickUpAndDownItem = uiManager.getElement(JoystickUpAndDown.UpAndDown.id);
//    if (isShow) {
//        //同时打开摇杆
//        if (!joyStickVirtualRockerItem) {
//            uiManager.load(JSON.parse(JSON.stringify(JoystickVirtualRocker)));

//        } else {
//            uiManager.showLabel(JoystickVirtualRocker.VirtualRocker.id);
//        }
//        if (!joyStickUpAndDownItem) {
//            uiManager.load(JSON.parse(JSON.stringify(JoystickUpAndDown)));

//        } else {
//            uiManager.showLabel(JoystickUpAndDown.UpAndDown.id);
//        }
//        this.setJoyStickEvent(true);
//    } else {
//        uiManager.hideLabel(JoystickVirtualRocker.VirtualRocker.id);
//        uiManager.hideLabel(JoystickUpAndDown.UpAndDown.id);
//        this.setJoyStickEvent(false);
//    }

//}
/**
    * 给虚拟摇杆添加事件
    * */
//function setJoyStickEvent(isSet) {
//    let uiManager = sview0.sviewFrame.getUIManager();
//    let joyStickVirtualRockerItem = uiManager.getElement(JoystickVirtualRocker.VirtualRocker.id);
//    let joyStickUpAndDownItem = uiManager.getElement(JoystickUpAndDown.UpAndDown.id);
//    let ser = SView.ServiceManager.getRoamService(sview0.sviewFrame);
//    let upAndDownMouseMove = function (isUp, distance) {
//        if (ser) {
//            //ser.getRoamAction().moveUpAndDown(isUp ? -distance : distance);
//            if (!ser.getIsUpAndDown()) {
//                ser.setIsUpAndDown(true);
//            }
//            ser.getUpOrDownMap().isUp = isUp;
//            ser.getUpOrDownMap().distance = distance;
//        }
//    }
//    let upAndDownMouseUp = function (isUp, distance) {
//        if (ser) {
//            if (ser.getIsUpAndDown()) {
//                ser.setIsUpAndDown(false);
//            }
//        }

//    }
//    let virtualRockerMouseMove = function (toSend) {
//        if (ser) {
//            if (!ser.getIsVirtualRocker()) {
//                ser.setIsVirtualRocker(true);
//            }

//            let theta = Number(toSend.angle.radian);
//            let dis = Number(toSend.distance);

//            ser.getVirtualRockerMap().strSpeed = dis * Math.sin(theta) / 80.0;
//            ser.getVirtualRockerMap().sidSpeed = dis * Math.cos(theta) / 80.0;

//        }
//    }
//    let virtualRockerMouseUp = function () {
//        if (ser) {
//            if (ser.getIsVirtualRocker()) {
//                ser.setIsVirtualRocker(false);
//            }
//        }

//    }
//    if (isSet) {
//        joyStickUpAndDownItem.setOnMove(upAndDownMouseMove);
//        joyStickUpAndDownItem.setOnMouseup(upAndDownMouseUp);
//        joyStickVirtualRockerItem.setOnMove(virtualRockerMouseMove);
//        joyStickVirtualRockerItem.setOnMouseup(virtualRockerMouseUp);
//        if (ser) {
//            ser.statrTimer();
//        }

//    } else {
//        if (ser) {
//            ser.stopTimer();
//        }
//        if (joyStickUpAndDownItem) {
//            joyStickUpAndDownItem.setOnMove(null);
//            joyStickUpAndDownItem.setOnMouseup(null);
//        }
//        if (joyStickVirtualRockerItem) {
//            joyStickVirtualRockerItem.setOnMove(null);
//            joyStickVirtualRockerItem.setOnMouseup(null);
//        }
//    }
//}
//设置视图角度
function setPerspective(type) {
    this.event.stopPropagation();
    this.event.preventDefault();
    let viewType = 0;
    if (type === "solid") {
        viewType = 0;
    } else if (type === "transparent") {
        viewType = 1;
    } else if (type === "solidWire") {
        viewType = 2;
    } else if (type === "wire") {
        viewType = 3;
    }
    let array = new Map();
    array.set("fullClassName", "SView.Commands.DrawModeCommand");

    let paras = new SView.CommandParameters();
    paras.add(SView.Commands.DrawModeCommand.drawModeType, SView.CommandParameterType.INT, viewType);
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    commandsManager.execute(/*SView.Commands.CommandNames.DRAWMODECOMMAND*/"DrawMode", paras);
}


/* ------------------ 打开属性菜单 ------------------- */
function showAttributeInfo(elementId) {
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    commandsManager.execute(/*SView.Commands.CommandNames.DRAWMODECOMMAND*/"ShowProperty");

    SView.Windows.Button.highLightLabel(elementId);
}
function fillModelPropertyDialog(propertyString) {
    if (!(propertyString.length > 0)) {
        return;
    }

    var maxLength = 0;
    var PropertyList = [];
    var GeometricList = [];
    var FeaturesList = [];
    let json = JSON.parse(propertyString);
    for (let key in json) {
        let value = json[key];
        //分组显示
        if (key === "name"
        ) {
            PropertyList.push({
                label: key,
                value: value
            });
        } else if (key === "Volume"
            || key === "Area"
            || key === "Point"
            || key === "Line"
            || key === "CircleCenter"
            || key === "CircleRadius"
            || key === "CircleArcLength"
        ) {
            GeometricList.push({
                label: key,
                value: value
            });
        } else if (key === "ModleViewCount"
            || key === "Color"
            || key === "PMICount"
            || key === "SubmodelsCount"
            || key === "LOD0PatchCount"
            || key === "LOD1PatchNumber"
            || key === "InstanceCount"
            || key === "ID") {
            FeaturesList.push({
                label: key,
                value: value
            });
        } else {
            if (key === "PlaceID"
                || key === "protoID") {
                FeaturesList.push({
                    label: key,
                    value: value
                });
            } else {
                PropertyList.push({
                    label: key,
                    value: value
                });
            }
        }
    }


    AttrProperty.Collapse.detailInfo = PropertyList;
    GeometryProperty.Collapse.detailInfo = GeometricList;
    GeometryProperty.Collapse.detailInfo = GeometricList;

}
/* ------------------ 打开协同会议 ------------------- */
function showMeetingMenu(elementId) {

    //var meetingDialog = document.getElementById(MeetingDialog.MeetingDialog.id);
    //SView.Windows.Button.highLightLabel(elementId);
    //let uiManager = sview0.sviewFrame.getUIManager();
    //if (meetingDialog == null) {
    //    Synergy.Main.instance().init("http://10.1.202.42", sview0.sviewFrame)
    //        .then(function (data) {
    //            console.log("RightMenu.js---会议初始化成功");
    //            meetingDialog = uiManager.load(MeetingDialog);
    //            bindDragEvent(MeetingDialog.MeetingDialog.id);
    //        }).catch(function (err) {

    //        });

    //} else {
    //    uiManager.removeLabel(MeetingDialog.MeetingDialog.id);
    //    meetingDialog = null;
    //}
    //SView.Windows.Button.highLightLabel(elementId);
    var fileid = "42678f717d89483abc86ed7128ae01de";//测试服务器歼10飞机
    Synergy.Main.instance().init("http://10.1.202.42", fileid, sview0.sviewFrame)
        .then(function (data) {
            console.log("RightMenu.js---会议初始化成功");
            //meetingDialog = uiManager.load(MeetingDialog);
            //bindDragEvent(MeetingDialog.MeetingDialog.id);
        }).catch(function (err) {

        });

}
/* ------------------ 打开聊天界面 ------------------- */
function showChatMenu(elementId) {

    //SView.Windows.Button.highLightLabel(elementId);
    Synergy.Main.instance().openOrCloseChatDialog()
        .then(function (data) {
            //可添加回车键事件
            console.log("-----------打开聊天界面--------------");
        }).catch(function (err) {
            //可取消添加回车键事件
            console.error("-----------关闭聊天界面--------------");
        });
}
/* ------------------ 打开会议控制主界面 ------------------- */
function showMeetingControlMenu(elementId) {

    //SView.Windows.Button.highLightLabel(elementId);
    Synergy.Main.instance().openOrCloseMeetingControlDialog()
        .then(function (data) {
            console.log("-----------打开会议控制主界面--------------");
        }).catch(function (err) {
            console.error("-----------关闭会议控制主界面--------------");
        });
}
/* ------------------ 打开底部菜单栏 ------------------- */
var currBottomMenu;  // 当前底部菜单
function showBottomMenu(elementId) {
    var mainBottomMenu = document.getElementById(MainBottomMenu.BottomMenu.id);
    SView.Windows.Button.highLightLabel(elementId);
    if (mainBottomMenu == null) {
        // 创建底部菜单
        mainBottomMenu = sview0.sviewFrame.getUIManager().load(MainBottomMenu);
        currBottomMenu = "mainBottomMenu"
    } else {
        // 删除底部菜单
        sview0.sviewFrame.getUIManager().removeLabel(currBottomMenu);
        mainBottomMenu = null;
        currBottomMenu = null;
    }
}
