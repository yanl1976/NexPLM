function newOnclickFun() {
    alert("你点击我了");
}
function baiscBtnOnClick() {
    alert("我是原始点击事件");
}
var fatherElement = document.createElement("div");
fatherElement.setAttribute("id", "testSview_0");
var body = document.getElementsByTagName("body")[0];
body.appendChild(fatherElement);
/* ------------------ 关闭弹窗 ------------------- */
function closeDialog(elementId, btnId = null) {
    // 删除相应标签
    sview0.sviewFrame.getUIManager().removeLabel(elementId);
    // 隐藏相应标签
    // sview0.sviewFrame.getUIManager().hideLabel(elementId);
    // 对应按钮取消高亮
    if (btnId != null) {
        SView.Button.highLightLabel(btnId);
    }
}

/* ------------------ 打开底部菜单栏 ------------------- */
var currBottomMenu;  // 当前底部菜单
function showBottomMenu(elementId) {
    var mainBottomMenu = document.getElementById(MainBottomMenu.BottomMenu.id);
    Controls.Button.highLightLabel(elementId);
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


// 需要隐藏右侧菜单的底部菜单ID
var hiddenRightMenu = ["moveMenu", "annotateMenu", "animationMenu", "measureMenu"];
/* ------------------ 进入下一级底部菜单 ------------------- */
function enterBottomMenu(enterMenu, currentMenu = MainBottomMenu) {
    // 创建下一级菜单
    sview0.sviewFrame.getUIManager().load(enterMenu);
    currBottomMenu = enterMenu.BottomMenu.id;
    // 隐藏当前底部菜单
    sview0.sviewFrame.getUIManager().hideLabel(currentMenu.BottomMenu.id);
    // 隐藏右侧菜单
    if (hiddenRightMenu.indexOf(enterMenu.BottomMenu.id) != -1) {
        sview0.sviewFrame.getUIManager().hideLabel(RightMenu.RightMenu.id);
    }
}

/* ------------------ 确认删除弹出框 ------------------- */
function confirmDelete(elementId) {
    if (elementId == 'deleteAnnotate') {
        DeleteDialog.Dialog.content = "确认删除选中批注吗？";
        DeleteDialog.Dialog.buttons[0].onClick = "deleteAnnotate('confirm-delete')";
    }
    sview0.sviewFrame.getUIManager().load(DeleteDialog);
}

/* ------------------ 确认退出弹出框 ------------------- */
function confirmExit(exitDialog) {
    if (exitDialog) {
        ExitDialog.Dialog.content = "退出当前操作前，请确认数据是否已保存到视图中?";
        ExitDialog.Dialog.buttons[0].Button.onClick = "exitBottomMenu(" + exitDialog + ")";
    }
    sview0.sviewFrame.getUIManager().load(ExitDialog);
}

/* ------------------ 返回上一级底部菜单 ------------------- */
function exitBottomMenu(currentMenu, returnMenu = MainBottomMenu) {
    // 关闭弹出框
    sview0.sviewFrame.getUIManager().removeLabel("confirm-exit");
    currBottomMenu = returnMenu.BottomMenu.id;
    // 删除当前菜单
    sview0.sviewFrame.getUIManager().removeLabel(currentMenu.BottomMenu.id);
    // 显示上一级菜单
    sview0.sviewFrame.getUIManager().showLabel(returnMenu.BottomMenu.id);
    // 显示右侧菜单
    if (hiddenRightMenu.indexOf(currentMenu.BottomMenu.id) != -1) {
        sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
    }
}
// 打开气泡选择器
function showPopoverSelect(id) {
    if (document.getElementById(id).style && document.getElementById(id).style.display == 'block') {
        document.getElementById(id).style.display = "none";
    } else {
        document.getElementById(id).style.display = "block";
    }
}

/* ------------------ 折叠面板关闭打开 ------------------- */
function collapseClose(e) {
    // console.log(e);
    if (e.getElementsByClassName("SView-icon-open").length > 0) {
        e.getElementsByClassName("SView-icon-open")[0].setAttribute("class", "SView-icon-close");
        e.nextElementSibling.style.display = "none";
    } else {
        e.getElementsByClassName("SView-icon-close")[0].setAttribute("class", "SView-icon-open");
        e.nextElementSibling.style.display = "block";
    }
}

/* ------------------ 切换标签页 ------------------- */
function changeTabs(elementId, dataClass) {
    let tabs = document.getElementById(elementId);
    let tabElement = tabs.getElementsByClassName("SView-tabs")[0].children;
    for (let i = 0; i < tabElement.length; i++) {
        let tabContent = document.getElementById(tabElement[i].dataset.class);
        // 更改选中标签样式
        if (tabElement[i].dataset.class != dataClass) {
            tabElement[i].dataset.active = "false";
            tabElement[i].setAttribute("class", "SView-tab");
            // 关闭该标签内容
            tabContent.style.display = "none";
        } else {
            tabElement[i].dataset.active = "true";
            tabElement[i].setAttribute("class", "SView-tab SView-tab-active");
            // 显示该标签
            tabContent.style.display = "";
        }
    }
}

/* ------------------ 打开属性菜单 ------------------- */
function showAttributeInfo(elementId) {
    var attributeMenu = document.getElementById(AttributeDialog.AttributeDialog.id);
    SView.Button.highLightLabel(elementId);
    if (attributeMenu == null) {
        // 创建属性菜单
        attributeMenu = uiLayout.load(AttributeDialog);
        bindDragEvent(AttributeDialog.AttributeDialog.id);
    } else {
        // 删除属性菜单
        uiLayout.removeLabel(AttributeDialog.AttributeDialog.id);
        attributeMenu = null;
    }
}
/* ------------------ 打开装配树 ------------------- */
function showAssemblyTree(elementId) {
    var treeMenu = document.getElementById(AssemblyDialog.TreeDialog.id);
    let uiManager = sview0.sviewFrame.getUIManager();
    Controls.Button.highLightLabel(elementId);
    if (treeMenu == null) {
        var base = sview0.sviewFrame.getViewer().scene;
        var topModel = base.getTopModel("http://10.0.43.166:8082/SView/SView/models/3d_pmi.svlx");
        AssemblyDialog.TreeDialog.content_1.subComponents[0].Tree.data = [];
        AssemblyDialog.TreeDialog.content_1.subComponents[0].Tree.data[0] = new S3DView.JsonAssembly(topModel, true, true);
        uiManager.load(AssemblyDialog);
    } else {
        uiManager.removeLabel(AssemblyDialog.TreeDialog.id);
        treeMenu = null;
    }
}

/* ------------------ 确认删除批注 ------------------- */
function deleteAnnotate(elementId) {
    SuccessInfo.ResultDialog.title.content = "删除成功";
    SuccessInfo.ResultDialog.detail.content = "选中批注已成功删除";
    uiLayout.removeLabel(elementId);
    uiLayout.load(SuccessInfo);
    setTimeout(function () {
        uiLayout.removeLabel(SuccessInfo.ResultDialog.id);
    }, 3000)
}

/* ------------------ 编辑批注 ------------------- */
function eidtAnnotate() {
    MessageDialog.Message.content = "请先选择一个批注对象"
    // 创建提示信息弹出框
    uiLayout.load(MessageDialog);
    setTimeout(function () {
        uiLayout.removeLabel(MessageDialog.Message.id);
    }, 3000)
}

// 为元素 绑定拖动事件
function bindDragEvent(id) {
    var obj = document.getElementById(id);
    obj.onmousedown = function (e) {
        e = e || window.event;

        obj.setCapture && obj.setCapture();    // IE8 及以下 强制捕获下一次单击事件

        obj.fixedX = e.clientX - (obj.getBoundingClientRect().left - document.documentElement.clientLeft);
        obj.fixedY = e.clientY - (obj.getBoundingClientRect().top - document.documentElement.clientTop);

        document.onmousemove = function (e) {
            e = e || window.event;

            var x = e.clientX + (document.documentElement.scrollLeft || window.pageXOffset || document.body.scrollLeft);
            var y = e.clientY + (document.documentElement.scrollTop || window.pageYOffset || document.body.scrollTop);

            obj.style.left = x - obj.fixedX + "px";    // 元素在页面中的坐标 = 鼠标在页面中的坐标 - 元素在页面中的坐标
            obj.style.top = y - obj.fixedY + "px";
        };

        document.onmouseup = function () {
            document.onmousemove = null;    // 解除 鼠标移动div跟随 事件
            document.onmouseup = null;    // 解除鼠标松开事件
            obj.releaseCapture && obj.releaseCapture();    // IE8 及以下 解除强制捕获单击事件
        };

        return false;
    };
}

//公共命令方法
function commandClickFun(id, commandType) {
    Controls.Button.highLightLabel(id);
    let uiManager = sview0.sviewFrame.getUIManager();
    //let command = new [commandType]();
    //commandsManager.add(commandType, command);
    var message = document.getElementById(MessageDialog.Message.id);
    MessageDialog.Message.content = "请选择第一个点";
    if (message == null) {
        uiManager.load(MessageDialog);
    } else {
        Controls.Message.show();
    }
}
//文本批注命令
function selectAnnotateMode(id) {
    //var textAnnotationButton = document.getElementById(id);
    SView.Button.highLightLabel(id);

    let commandsManager = sview0.sviewFrame.getCommandsManager();
    commandsManager.setModule(SView);
    let command = commandsManager.get(SView.Commands.TEXTANNOTATION);
    //let command = new AnnotationTextCommand();
    //commandsManager.add(SView.Commands.TEXTANNOTATION,command);





    let listener = SView.createSViewObject(SView, "AnnotationCommandListener");
    listener.onStart = function () {
        console.log("开始执行批注命令");
    }
    listener.onStateChanged = function (state) {
        console.log("批注当前在步骤" + state);
        if (state === 0) {
            BtnMessageDialog.Message.content = "请选择第一个点";
            sview0.sviewFrame.getUIManager().load(BtnMessageDialog);
        }
        if (state === 1) {
            BtnMessageDialog.Message.content = "请选择第二个点";
            sview0.sviewFrame.getUIManager().load(BtnMessageDialog);
        }
        if (state === 2) {
            sview0.sviewFrame.getUIManager().load(Dialog);
            sview0.sviewFrame.getUIManager().removeLabel(BtnMessageDialog.Message.id);
        }
    }
    listener.onEnd = function () {
        console.log("批注命令执行结束");
    }
    listener.onError = function (error) {
        console.error(error);
    }
    listener.onSuccess = function (command) {
        let annotation = command.getAnnotation();
    }
    command.addListener(listener);
    commandsManager.excute(SView.Commands.TEXTANNOTATION);

    //sview0.sviewFrame.getCommandsManager().exitAll();
    //sview0.sviewFrame.getCommandsManager().setModule(SView);
    //sview0.sviewFrame.getCommandsManager().excute(SView.Commands.TEXTANNOTATION);
}

/* ------------------ 关闭弹窗 ------------------- */
function closeAnnotationDialog(elementId, btnId = null) {
    // 删除相应标签
    sview0.sviewFrame.getUIManager().removeLabel(elementId);
    // 隐藏相应标签
    // sview0.sviewFrame.getUIManager().hideLabel(elementId);
    // 对应按钮取消高亮
    if (btnId != null) {
        SView.Button.highLightLabel(btnId);
    }
    let command = sview0.sviewFrame.getCommandsManager().get(SView.Commands.TEXTANNOTATION);
    command.undo(1);
    //
}

function createAnnotation(elementId, btnId = null) {
    let value = document.getElementById(btnId).value;
    // 删除相应标签
    sview0.sviewFrame.getUIManager().removeLabel(elementId);
    // 隐藏相应标签
    // sview0.sviewFrame.getUIManager().hideLabel(elementId);
    // 对应按钮取消高亮
    if (btnId != null) {
        SView.Button.highLightLabel(btnId);
    }
    let command = sview0.sviewFrame.getCommandsManager().get(SView.Commands.TEXTANNOTATION);
    command.textAnnotationStep(null);
}

/* ------------------ 检查输入框内容长度 ------------------- */
function checkLength(elementId) {
    var element = document.getElementById(elementId);
    var fatherElement = element.parentNode;
    var warning = fatherElement.getElementsByClassName("SView-warningPopover")[0];
    if (warning != null && element.value == "") {
        warning.style.display = "";
        setTimeout(function () {
            warning.style.display = "none";
        }, 3000)
    } else {
        warning = fatherElement.getElementsByTagName("p")[0];
        if (element.value.length == 0) {
            warning.style.display = "";
        } else {
            warning.style.display = "none";
        }
    }
    if (elementId == "meetingNickName") {
        nickName = element.value;
    }
}
/* ------------------ 装配树相关方法 ------------------- */
// 树形控件重命名
function nodeRename() {
    var nodeId = document.getElementsByClassName("tree-node-active")[0].getAttribute("node-id");
    document.getElementById("SView-node-name-" + nodeId).querySelector(".SView-tree-name").style.display = "none";
    document.getElementById("SView-node-input-" + nodeId).style.display = "inline-block";
    document.getElementById("SView-node-input-" + nodeId).focus();
}
function saveNodeName(id) {
    document.getElementById("SView-node-name-" + id).querySelector(".SView-tree-name").style.display = "inline-block";
    document.getElementById("SView-node-input-" + id).style.display = "none";
    console.log(document.getElementById("SView-node-input-" + id).value);
}
// 展开
function dropClick(a) {
    a.className = a.className.indexOf("tree-close") > -1 ? "tree-open" : "tree-close";
    var b = a.parentNode.parentNode.nextElementSibling;
    b.className =
        b.className.indexOf("SView-tree-show") > -1
            ? "SView-tree-subItem"
            : "SView-tree-subItem SView-tree-show";
}
// 选中
function nodeClick(a, type) {
    var e = document.getElementsByClassName("SView-tree")[0].querySelectorAll(".node-name");
    for (var f = 0; f < e.length; f++) {
        e[f].className = "node-name";
    }
    if (!type) {
        a.className = "node-name tree-node-active";
    }
    let shapeId = a.getAttribute("node-id");
    if (shapeId > 0) {
        let shape = sview0.sviewFrame.getViewer().getScene().getShapeByID(Number(shapeId));
        sview0.sviewFrame.getViewer().getScene().getSelector().add(shape);
    }
}
// 多选框选中
function checkboxClick(a) {
    var d,
        e,
        f,
        g,
        h,
        i;
    if (a.className.indexOf("icon__round--false") > -1 || a.className.indexOf("icon__round--part") > -1) {
        //勾选
        d = a.parentNode.parentNode.nextElementSibling;
        e = a.parentNode.parentNode.nextElementSibling.querySelectorAll(".SView-tree-item");
        if (e.length > 0) {
            for (f = 0; f < e.length; f++) {
                g = e[f];
                h = g.querySelector(".icon__round");
                h.className = "icon__round icon__round--true";
            }
        }
        a.className = "icon__round icon__round--true";
        setChecked(true, a.parentNode.parentNode);
        let shapeId = Number(a.parentNode.nextElementSibling.getAttribute("node-id"));
        if (shapeId > 0) {
            let shape = sview0.sviewFrame.getViewer().getScene().getShapeByID(shapeId);
            sview0.sviewFrame.getViewer().getScene().getVisiables().add(shape);
        }
    } else if (a.className.indexOf("icon__round--true") > -1) {
        //取消勾选
        d = a.parentNode.parentNode.nextElementSibling;
        e = a.parentNode.parentNode.nextElementSibling.querySelectorAll(".SView-tree-item");
        if (e.length > 0) {
            for (f = 0; f < e.length; f++) {
                g = e[f];
                h = g.querySelector(".icon__round");
                h.className = "icon__round icon__round--false";
            }
        }
        a.className = "icon__round icon__round--false";
        setChecked(false, a.parentNode.parentNode);
        let shapeId = Number(a.parentNode.nextElementSibling.getAttribute("node-id"));
        if (shapeId > 0) {
            sview0.sviewFrame.getViewer().getScene().getVisiables().remove(shapeId);
        }
    }
}
// 关联选中状态
function setChecked(type, a) {
    var parentNode = a.parentNode.parentNode;
    var sibling = parentNode.querySelectorAll(".icon__round");
    if (type) {
        var flag = true;
        for (var i = 0; i < sibling.length; i++) {
            if (sibling[i].className.indexOf("icon__round--false") > -1) {
                flag = false;
                break;
            }
        }
        if (parentNode.previousElementSibling) {
            if (flag) {
                parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--true";
            } else {
                parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--part";
            }
        }
    } else {
        var flag = true;
        for (var i = 0; i < sibling.length; i++) {
            if (sibling[i].className.indexOf("icon__round--true") > -1) {
                flag = false;
                break;
            }
        }
        if (parentNode.previousElementSibling) {
            if (flag) {
                parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--false";
            } else {
                parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--part";
            }
        }
    }
    if (Number(a.getAttribute("num")) > 2) {
        setChecked(type, parentNode.previousElementSibling)
    }
}
// //自定义一个浏览器右键菜单，单击右键是显示它
function offset(ele) {
    var obj = { t: 0, l: 0 };
    obj.l = ele.offsetLeft;
    obj.t = ele.offsetTop;

    while (ele.offsetParent) {//寻找父级，当寻找到 body 往上的时候为 null ，所以循环停止
        ele = ele.offsetParent; //初始值
        obj.l += ele.offsetLeft
        obj.t += ele.offsetTop
    }

    return obj
}
document.oncontextmenu = function (ev) {
    return false;
}
// //oncontextmenu上下文菜单事件，右键菜单
function nodeRightClick(e, rightClickMenuId) {
    nodeClick(e);
    var treeRightMenu = document.getElementById(rightClickMenuId.id);
    //显示我们自己定义的右键菜单
    //1.找到菜单
    //提取到函数外面作为全局变量

    //兼容Event对象
    e = e || window.event;

    //鼠标坐标
    var offsetObj = offset(e);
    treeRightMenu.style.left = (offsetObj.l + 40) + "px";
    treeRightMenu.style.top = (offsetObj.t + 20) + "px";
    //3.显示右键菜单
    treeRightMenu.style.display = "block";
    //阻止默认的右键菜单显示
    return false;
};

//不需要积隐藏右键菜单
document.documentElement.onclick = function () {
    if (document.getElementById("treeRightClickMenu") && document.getElementById("treeRightClickMenu").style) {
        document.getElementById("treeRightClickMenu").style.display = "none";
    }
}