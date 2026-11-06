// 需要隐藏右侧菜单的底部菜单ID
var hiddenRightMenu = ["move_menu", "sectione_menu", "explosive_menu", "annotate_menu", "measure_distance_menu", "animation_menu", "gesture_menu", "measure_distance_menu", "measure_angle_menu", "measure_diameter_menu", "measure_attributes_menu"];
/* ------------------ 进入下一级底部菜单 ------------------- */
function enterBottomMenu(enterMenu, currentMenu = MainBottomMenu) {
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    // 关闭弹出框
    //if (currentMenu.BottomMenu.id != "moveMenu") {
    //    sview0.sviewFrame.getUIManager().removeLabel("confirm-exit");
    //}
    //退出当前模块下的命令,根据命令名称去队列里查找如果有则删除，
    if (currentMenu.BottomMenu.subComponents) {
        for (let value of currentMenu.BottomMenu.subComponents) {
            if (value.Button) {
                let cmdName = value.Button.commandName;
                if (cmdName) {
                    let cmd = commandsManager.getRunningCommand(cmdName);
                    if (cmd === null) {
                        cmd = commandsManager.getWaitingCommandQueue(cmdName);
                    }
                    if (cmd === null) {
                        cmd = commandsManager.getActiveCommandQueue(cmdName);
                    }
                    if (cmd) {
                        commandsManager.close(cmd);
                    }
                }
            }
        }
    }

    if (enterMenu.BottomMenu.id == "annotate_menu") {
        changeRightMenuClickable(false);
        //sview0.sviewFrame.getUIManager().load(GestureMenu);
    }
    if (enterMenu.BottomMenu.id == "measure_distance_menu") {
        changeRightMenuClickable(false);
    }
    if (enterMenu.BottomMenu.id == "gesture_menu") {
        document.getElementById("gesture_menu").style.display = "block";
        var sviewFrameDiv = document.getElementById("sview_1");
        var canvas = document.getElementById('' +
            '' +
            'gesture_canvas');
        canvas.width = sviewFrameDiv.offsetWidth;
        canvas.height = sviewFrameDiv.offsetHeight;
        paint.init();
        document.getElementById('gesture_canvas').style.display = "block";
    } else {
        // 创建下一级菜单
        sview0.sviewFrame.getUIManager().load(enterMenu);
        if (enterMenu.BottomMenu.id == "measure_distance_menu") {
            sview0.sviewFrame.getUIManager().load(AlignmentCameraBtn);
        }
    }
    currBottomMenu = enterMenu.BottomMenu.id;
    // 隐藏当前底部菜单
    sview0.sviewFrame.getUIManager().hideLabel(currentMenu.BottomMenu.id);

    // 隐藏右侧菜单
    if (hiddenRightMenu.indexOf(enterMenu.BottomMenu.id) != -1) {
        //sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
        hideRightMenus();
    }
}
/* ------------------ 返回上一级底部菜单 ------------------- */
function exitBottomMenu(currentMenu, returnMenu = MainBottomMenu) {
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    // 关闭弹出框
    if (sview0.sviewFrame.getUIManager().getElement("confirm-exit")) {
        sview0.sviewFrame.getUIManager().removeLabel("confirm-exit");
    }
    //退出当前模块下的命令,根据命令名称去队列里查找如果有则删除，
    if (currentMenu.BottomMenu.subComponents) {
        for (let value of currentMenu.BottomMenu.subComponents) {
            if (value.Button) {
                let cmdName = value.Button.commandName;
                if (cmdName) {
                    var cmd = getCommand(cmdName);
                    if (cmd) {
                        commandsManager.close(cmd);
                    }
                }
            }
        }
    }


    currBottomMenu = returnMenu.BottomMenu.id;
    if (currentMenu.BottomMenu.id == "gesture_menu") {
        document.getElementById("gesture_menu").style.display = "none";
    } else {
        if (currentMenu.BottomMenu.id == "annotate_menu") {
            changeRightMenuClickable(true);
        } else if (currentMenu.BottomMenu.id == "measure_distance_menu"
            || currentMenu.BottomMenu.id == "measure_angle_menu"
            || currentMenu.BottomMenu.id == "measure_diameter_menu"
            || currentMenu.BottomMenu.id == "measure_attributes_menu") {
            changeRightMenuClickable(true);

            //退出关联命令
            var cmd = getCommand("AlignmentCamera");
            if (cmd) {
                commandsManager.close(cmd);
            }
            //先完成命令的关闭操作再移除
            sview0.sviewFrame.getUIManager().removeLabel(AlignmentCameraBtn.BottomMenu.id);
        }
        // 删除当前菜单
        sview0.sviewFrame.getUIManager().removeLabel(currentMenu.BottomMenu.id);
    }
    // 显示上一级菜单
    sview0.sviewFrame.getUIManager().showLabel(returnMenu.BottomMenu.id);
    // 显示右侧菜单
    if (hiddenRightMenu.indexOf(currentMenu.BottomMenu.id) != -1 && hiddenRightMenu.indexOf(returnMenu.BottomMenu.id) == -1) {
        sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
    }
}

function getCommand(cmdName) {
    var commandsManager = sview0.sviewFrame.getCommandsManager();
    let cmd = commandsManager.getRunningCommand(cmdName);
    if (cmd === null) {
        cmd = commandsManager.getWaitingCommandQueue(cmdName);
    }
    if (cmd === null) {
        cmd = commandsManager.getActiveCommandQueue(cmdName);
    }
    return cmd;
}
// 打开气泡选择器
function showPopoverSelect(id) {
    this.event.stopPropagation();
    this.event.preventDefault();
    let uiManager = sview0.sviewFrame.getUIManager();
    var dirs = uiManager.getElement(id);
    if (dirs && id != dirs.eleId) {
        dirs.classList.remove("SView-show");
        dirs.classList.add("SView-hide");
    }
    if (dirs.eleIsShow) {
        dirs.hide();
    } else {
        dirs.show();
    }
    return false;
}
// 隐藏气泡选择器
function hidePopoverSelect(id) {
    let uiManager = sview0.sviewFrame.getUIManager();
    var dirs = uiManager.getElement(id);
    if (dirs && id != dirs.eleId) {
        dirs.hide();
    }
}
/**
 * 隐藏右侧菜单(及子菜单)
 * */
function hideRightMenus() {
    // 隐藏右侧菜单
    sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);

    //隐藏属性窗体
    let propertyDialog = sview0.sviewFrame.getUIManager().getElement(SView.Commands.AttributeDialog.Dialog.id);
    if (propertyDialog && propertyDialog.isShow()) {
        sview0.sviewFrame.getUIManager().hideLabel(SView.Commands.AttributeDialog.Dialog.id);
        //取消高亮
        SView.Windows.Button.highLightLabel("attributeInfo", false);
    }

    //隐藏装配窗体
    let treeMenuDialog = sview0.sviewFrame.getUIManager().getElement(AssemblyDialog.TreeDialog.id);
    if (treeMenuDialog && treeMenuDialog.isShow()) {
        sview0.sviewFrame.getUIManager().hideLabel(AssemblyDialog.TreeDialog.id);
        SView.Windows.Button.highLightLabel("assemblyTree", false);
    }

    //隐藏基本视图菜单;
    let viewMenu = sview0.sviewFrame.getUIManager().getElement(ViewMenu.ViewMenu.id);
    if (viewMenu && viewMenu.isShow()) {
        sview0.sviewFrame.getUIManager().hideLabel(ViewMenu.ViewMenu.id);
        SView.Windows.Button.highLightLabel("viewMenuBtn", false);
    }


}
/* -------------------------------- 批注菜单 --------------------------------- */
//公共命令方法
function commandClickFun(id, commandType, tag, type, isEdit) {
    if (!isEdit && (commandType == "AnnotationNumber"
        || commandType == "Annotation")) {
        sview0.sviewFrame.getViewer().getScene().getSelector().clear();
    }
    //对齐相机关闭测量按钮高亮
    if (id == "alignmentCamera") {
        let uiManager = sview0.sviewFrame.getUIManager();
        if (uiManager.getElement("measure_attributes_menu") && uiManager.getElement("measure_attributes_menu").isShow()) {
            var dirs = document.getElementById("measure_attributes_menu").children[0].children;
            for (var i = 0; i < dirs.length; i++) {
                dirs[i].children[0].style.backgroundColor = "";
            }
        }
        if (uiManager.getElement("measure_diameter_menu") && uiManager.getElement("measure_diameter_menu").isShow()) {
            var dirs = document.getElementById("measure_diameter_menu").children[0].children;
            for (var i = 0; i < dirs.length; i++) {
                dirs[i].children[0].style.backgroundColor = "";
            }
        }
        if (uiManager.getElement("measure_angle_menu") && uiManager.getElement("measure_angle_menu").isShow()) {
            var dirs = document.getElementById("measure_angle_menu").children[0].children;
            for (var i = 0; i < dirs.length; i++) {
                dirs[i].children[0].style.backgroundColor = "";
            }
        }
        if (uiManager.getElement("measure_distance_menu") && uiManager.getElement("measure_distance_menu").isShow()) {
            var dirs = document.getElementById("measure_distance_menu").children[0].children;
            for (var i = 0; i < dirs.length; i++) {
                dirs[i].children[0].style.backgroundColor = "";
            }
        }
    }
    //changeRightMenuClickable(false);
    //查找对应的按钮将其进行高亮
    if (id && id != "") {
        var dirs = document.getElementById(id).parentElement.parentElement.children;
        for (var i = 0; i < dirs.length; i++) {
            if (dirs[i].children[0].id != id)
                dirs[i].children[0].style.backgroundColor = "";
        }
        if (id != "gesture" && id != "deleteDistanceMeasure" && id != "deleteAngleMeasure" && id != "deleteDiametreMeasure" && id != "deleteAnnotate" && id != "editButton")
            SView.Windows.Button.highLightLabel(id);
    }
    // 需要隐藏底部菜单和右侧菜单的命令
    let hideMenuCommands = ["ClipPlaneCommand", "SelectMove", "ExplosionCommand", "PaintBrush"];
    if (hideMenuCommands.indexOf(commandType) !== -1) {
        // 隐藏当前底部菜单
        sview0.sviewFrame.getUIManager().hideLabel(MainBottomMenu.BottomMenu.id);
        // 隐藏右侧菜单
        //sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
        hideRightMenus();
    }
    // if (/*commandType == "PlayAnimationCommand" ||*/ commandType == "ClipPlaneCommand") {
    //     // 隐藏当前底部菜单
    //     sview0.sviewFrame.getUIManager().hideLabel(MainBottomMenu.BottomMenu.id);
    //     // 隐藏右侧菜单
    //     sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
    // }
    let uiManager = sview0.sviewFrame.getUIManager();
    let commandsManager = sview0.sviewFrame.getCommandsManager();

    //组合命令相关参数
    let array = new Map();
    if (tag) {
        //指明命令对应的命名空间和类名 ：SView.Commands.MeasureAngleCommand
        array.set("fullClassName", tag);
    }
    if (type) {
        //指明命令对应的类型 ：点点、点线等
        array.set("commandType", type);
    }
    if (isEdit) {
        let inputParas = new SView.CommandParameters();
        let inputPara = new SView.CommandParameter()
        inputPara.name = "isEdit";
        inputPara.value = isEdit;
        inputParas.add(inputPara);
        array.set("commandParameters", inputParas);
    }
    let ret;
    if (array.size === 0) {
        ret = commandsManager.execute(commandType);
    } else {
        ret = commandsManager.execute(commandType, array);
    }

    //if (ret === 0 && commandType != "PlayAnimationCommand" && commandType != "Option") {

    //    MessageDialog.Message.content = commandType + " success";
    //    uiManager.load(MessageDialog);
    //    setTimeout(function () {
    //        uiManager.removeLabel(MessageDialog.Message.id);
    //    }, 3000);
    //}
}

/* ------------------ 编辑批注 ------------------- */
function eidtAnnotate() {
    let uiManager = sview0.sviewFrame.getUIManager();
    let annotation = null;
    //let shape = sview0.sviewFrame.getViewer().getScene().getSelector().get();
    //if (shape != null && shape instanceof M3D.Shapes.Annotation) {
    //    annotation = shape;
    //}
    let shapes = sview0.sviewFrame.getViewer().getScene().getSelector().getAll();
    let hasMulitAnnotation = false; //是否已有选中批注对象
    //判断是否选中多个批注对象
    for (let shape of shapes) {
        if (shape && shape instanceof M3D.Shapes.Annotation) {
            if (annotation === null) {
                annotation = shape;
                break;
            }
            //else {
            //    hasMulitAnnotation = true;
            //    break;
            //}
        }
    }
    if (annotation) {
        //提示多个批注
        //if (hasMulitAnnotation) {
        //    MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.TextNoteMulEdit;
        //    // 创建提示信息弹出框
        //    uiManager.load(MessageDialog);
        //    setTimeout(function () {
        //        uiManager.removeLabel(MessageDialog.Message.id);
        //    }, 3000);
        //    return;
        //}
        if (annotation.getAnnotationType() == M3D.Shapes.AnnotationType.ORDER_ANNOTATION) {
            commandClickFun('', 'AnnotationNumber', null, null, true);
            //MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.AnnotationDonotEdit;
            //// 创建提示信息弹出框
            //uiManager.load(MessageDialog);
            //setTimeout(function () {
            //    uiManager.removeLabel(MessageDialog.Message.id);
            //}, 3000);
        }
        if (annotation.getAnnotationType() == M3D.Shapes.AnnotationType.TEXT_ANNOTATION) {
            isEdit = true;
            commandClickFun('', 'Annotation', null, null, true);
        }
        if (annotation.getAnnotationType() == M3D.Shapes.AnnotationType.COMPONENTNAME_ANNOTATION) {
            MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.AnnotationDonotEdit;
            // 创建提示信息弹出框
            uiManager.load(MessageDialog);
            setTimeout(function () {
                uiManager.removeLabel(MessageDialog.Message.id);
            }, 3000);
        }
    } else {
        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneAnnotation;
        // 创建提示信息弹出框
        uiManager.load(MessageDialog);
        setTimeout(function () {
            uiManager.removeLabel(MessageDialog.Message.id);
        }, 3000);
    }
}
/* ------------------ 确认删除批注 ------------------- */
//function deleteAnnotate(elementId) {
//    let shapes = sview0.sviewFrame.getViewer().getScene().getSelector().getAll();
//    let isSuccess = false;
//    for (let shape of shapes) {
//        if (shape && shape instanceof M3D.Shapes.Annotation) {
//            let ret = sview0.sviewFrame.getViewer().getScene().getShapes().removeShape(shape.getId(), true);
//            if (ret) {
//                sview0.sviewFrame.getViewer().getScene().getSelector().remove(shape);
//                isSuccess = true;
//            }
//        }
//    }
//    sview0.sviewFrame.getViewer().getScene().refresh();
//    let uiManager = sview0.sviewFrame.getUIManager();
//    //SuccessInfo.ResultDialog.detailContent = "选中批注已成功删除";
//    uiManager.removeLabel(elementId);
//    if (isSuccess) {
//        SuccessInfo.ResultDialog.titleContent = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSuccess;
//        uiManager.load(SuccessInfo);
//        setTimeout(function () {
//            uiManager.removeLabel(SuccessInfo.ResultDialog.id);
//        }, 3000);
//    } else {
//        FailInfo.ResultDialog.titleContent = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteError;
//        uiManager.load(FailInfo);
//        setTimeout(function () {
//            uiManager.removeLabel(FailInfo.ResultDialog.id);
//        }, 3000);
//    }
//}

// 删除测量对象
function deleteSign(elementId) {
    let shapes = sview0.sviewFrame.getViewer().getScene().getSelector().getAll();
    let isSuccess = false;
    for (let shape of shapes) {
        if (shape && (shape instanceof M3D.Shapes.Measure || shape instanceof M3D.Shapes.Annotation)) {
            let ret = sview0.sviewFrame.getViewer().getScene().getShapes().removeShape(shape.getId(), true);
            if (ret) {
                sview0.sviewFrame.getViewer().getScene().getSelector().remove(shape);
                isSuccess = true;
            }
        }
    }
    sview0.sviewFrame.getViewer().getScene().refresh();
    let uiManager = sview0.sviewFrame.getUIManager();
    uiManager.removeLabel(elementId);
    if (isSuccess) {
        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSuccess;
        uiManager.load(MessageDialog);
        setTimeout(function () {
            uiManager.removeLabel(MessageDialog.Message.id);
        }, 3000);
        //SuccessInfo.ResultDialog.titleContent = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSuccess;
        //uiManager.load(SuccessInfo);
        //setTimeout(function () {
        //    uiManager.removeLabel(SuccessInfo.ResultDialog.id);
        //}, 3000);
    } else {
        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteError;
        uiManager.load(MessageDialog);
        setTimeout(function () {
            uiManager.removeLabel(MessageDialog.Message.id);
        }, 3000);
        //FailInfo.ResultDialog.titleContent = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteError;
        //uiManager.load(FailInfo);
        //setTimeout(function () {
        //    uiManager.removeLabel(FailInfo.ResultDialog.id);
        //}, 3000);
    }
}

/* -------------------------------- 刨切菜单 --------------------------------- */
//进入剖切命令
function enterClipBottomMenu(enterMenu, commandType) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let ret;
    ret = commandsManager.execute(commandType);
    if (ret === 0) {
        // 创建下一级菜单
        sview0.sviewFrame.getUIManager().load(enterMenu);
        currBottomMenu = enterMenu.BottomMenu.id;
        // 隐藏当前底部菜单
        sview0.sviewFrame.getUIManager().hideLabel(MainBottomMenu.BottomMenu.id);
        // 隐藏右侧菜单
        if (hiddenRightMenu.indexOf(enterMenu.BottomMenu.id) != -1) {
            sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
        }
    }

}
function exitClipBottomMenu(currentMenu, returnMenu = MainBottomMenu) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let cmdName = "ClipPlaneCommand";
    let cmd = commandsManager.getRunningCommand(cmdName);
    if (cmd === null) {
        cmd = commandsManager.getWaitingCommandQueue(cmdName);
    }
    if (cmd === null) {
        cmd = commandsManager.getActiveCommandQueue(cmdName);
    }
    if (cmd) {
        commandsManager.close(cmd);
    }
    // 删除当前菜单
    sview0.sviewFrame.getUIManager().removeLabel(currentMenu.BottomMenu.id);
    // 显示上一级菜单
    sview0.sviewFrame.getUIManager().showLabel(returnMenu.BottomMenu.id);
    // 显示右侧菜单
    if (hiddenRightMenu.indexOf(currentMenu.BottomMenu.id) != -1 && hiddenRightMenu.indexOf(returnMenu.BottomMenu.id) == -1) {
        sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
    }
}
/**
 * 根据剖切界面的按钮状态设置剖切参数
 * @param {any} id
 * @param {any} key
 */
function sectionClickFun(id, key) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ClipPlaneCommand");
    if (!command) {
        return;
    }
    let commandView = command.getView();
    let highLightBtn = document.getElementById(id);
    if (highLightBtn.style.backgroundColor == "") {
        SView.Windows.Button.highLightLabel(id);
        commandView.updateSection(key, true);
    } else {
        highLightBtn.style.backgroundColor = "";
        commandView.updateSection(key, false);
    }
}
function sectionSelectedShapesBtClickFun(id, key) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ClipPlaneCommand");
    if (!command) {
        return;
    }
    let commandView = command.getView();
    let shapeSize = command.sectionAction.selectedShapeList.length;
    if (key === "add") {
        //let highLightBtn = document.getElementById(id);

        //let components = highLightBtn.subComponents;
        //for (let value of components) {
        //    if (value.Badge) {
        //        let content = value.Badge.content;
        //        content = shapeSize;
        //    }
        //}
        commandView.addSelectedShapes();
    } else if (key === "clear") {
        commandView.clearSelectedShapes();
    }
    command.updateSection();
}
// 滑块颜色填充
function sliderClipChange(id, key, className) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ClipPlaneCommand");
    if (!command) {
        return;
    }
    let commandView = command.getView();
    var slider = document.getElementById(id);
    var min = slider.min;
    var max = slider.max;
    var value = Number(slider.value);
    slider.style.backgroundSize = value * 100 / (max - min) + "% 100%";
    value = parseFloat(slider.value) / 100;
    commandView.updateSection(key, value);

    //跟他关联的组件的处理
    if (className) {
        var sliders = document.getElementsByClassName(className);
        for (i = 0; i < sliders.length; i++) {
            var slider1 = sliders[i].getElementsByTagName("input")[0];
            var min = slider.min;
            var max = slider.max;
            var value = Number(slider.value);
            slider1.style.backgroundSize = value * 100 / (max - min) + "% 100%";
            slider1.value = slider.value;
        }
    }

}
/* -------------------------------- 爆炸菜单 --------------------------------- */
var levelValue = -1;
//进入爆炸命令
function enterExplosiveBottomMenu(enterMenu, commandType) {
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let ret;
    ret = commandsManager.execute(commandType);
    if (ret === 0) {
        levelValue = -1;
        // 创建下一级菜单
        sview0.sviewFrame.getUIManager().load(enterMenu);
        currBottomMenu = enterMenu.BottomMenu.id;
        // 隐藏当前底部菜单
        sview0.sviewFrame.getUIManager().hideLabel(MainBottomMenu.BottomMenu.id);
        // 隐藏右侧菜单
        if (hiddenRightMenu.indexOf(enterMenu.BottomMenu.id) != -1) {
            sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
        }
    }

}
//爆炸按钮参数
function explosiveClickFun(id, value) {
    var dirs = document.getElementById(id).parentElement.parentElement.children;
    for (var i = 0; i < dirs.length; i++) {
        if (dirs[i].children[0].id != id)
            dirs[i].children[0].style.backgroundColor = "";
    }
    let highLightBtn = document.getElementById(id);
    if (highLightBtn.style.backgroundColor == "") {
        highLightBtn.style.backgroundColor = "rgba(44,135,255, .5)";
        //command.updateSection(key, value);
    }
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ExplosionCommand");
    let commandView = command.getView();
    // 设置爆炸模式
    // commandView.setExplosionMode(value);
}
//爆炸滑块
function sliderExplosiveChange(id, key) {
    // 滑块
    var slider = document.getElementById(id);
    var min = slider.min;
    var max = slider.max;
    var sliderValue = Number(slider.value);

    // 爆炸模式
    var modeView = document.getElementById("explosive_global");
    var mode = 0;

    // 爆炸级别
    var levelView = document.getElementById("explosive_select");
    var level = -1;


    slider.style.backgroundSize = sliderValue * 100 / (max - min) + "% 100%";
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ExplosionCommand");
    let commandView = command.getView();
    // commandView.updateExplosion(mode, level, sliderValue);
}

//爆炸下拉框
function explosiveSelectClickFun(id) {
    levelValue = Number(document.getElementById(id).value);
    // let commandsManager = sview0.sviewFrame.getCommandsManager();
    // if (levelValue == 0 || levelValue == -1) {
    //     //commandsManager.execute("ExplosionCommand");
    // } else {
    //     //commandsManager.execute("ExplosionLevelCommand");
    // }
    let commandsManager = sview0.sviewFrame.getCommandsManager();
    let command = commandsManager.getCommand("ExplosionCommand");
    let commandView = command.getView();
    commandView.setExplosionLevel(levelValue);
}