var tree;
var option;
var modelViewList;//全局的视图列表对象（包括模型视图及在线视图）
var viewItemArray = [];
var commandsManager;//command 管理器
var showViewListCommand;
var createViewCommand;//创建默认视图Command(创建、修改)
var deleteViewCommand;//删除默认视图Command
var setDefViewCommand;//设置默认视图Command
var changeViewCommand;//获取视图列表及视图切换Command
var renameViewCommand;//重命名视图
let viewData = null;
var treeConfig = {
    isCopy: false,
    isCut: false,
    node: ""
};
var searchModelList = [];
var searchModelIndex = -1;
var searchModelText = "";
//视图搜索
var hasserachedviewid = [];
var presearchtex = "";
/* ------------------ 打开装配树 ------------------- */
function showAssemblyTree(elementId) {
    let uiManager = sview0.sviewFrame.getUIManager();
    let treeMenu = uiManager.getElement(AssemblyDialog.TreeDialog.id);
    let shapesetDialog = uiManager.getElement("shapeset-dialog");
    let attributeInfo = uiManager.getElement(SView.Commands.AttributeDialog.Dialog.id);
    SView.Windows.Button.highLightLabel(elementId);
    if (treeMenu === null) {
        var base = sview0.sviewFrame.getViewer().scene;
        var topModel = base.getTopModel();
        if (!topModel) {
            return;
        }


        AssemblyDialog.TreeDialog.content_1.subComponents[0].Tree.data = [];
        AssemblyDialog.TreeDialog.content_1.subComponents[0].Tree.data[0] =
            new SView.JsonAssembly(topModel, true, true);
        uiManager.load(AssemblyDialog);
        document.getElementById("searchContentInput").addEventListener("keypress", function () {
            if (event.keyCode == "13") {
                var searchStr = document.getElementById("searchContentInput").value;
                if (searchModelText !== searchStr) {
                    searchModelIndex = -1;
                    searchModelList = [];
                    searchModelText = searchStr;
                }
                checkLength('searchContentInput', 'assemblyTreeDialog');
                document.getElementById("searchContentInput").focus();
            }
        })
        var data = AssemblyDialog.TreeDialog.content_1.subComponents[0].Tree.data;
        data[0].checked = true;
        option = {
            data: data,
            checkbox: true,
            loadAll: false,
            elem: '.SView-tree-content',
            fold: function () { },
            unfold: function () { },
            format: function (format) {
                format.id = 'id';
                format.data = 'all';
                format.name = 'name';
                format.title = 'all';
                format.childs = 'children';
                f = format;
            },
            loadText: '正在加载数据中，请稍后...',
            loadCallBack: function (tree) {
                tree.nodeUnfold(data[0].id);
            },
            toolClick: function (type, node) {
                let nodeInfo = node;
            },
            //多选框点击
            checkClick: function (event, node) {
                let nodeInfo = node;
                nodeCheckChange(node);
            },
            folder_click: function (node) {
                let nodeInfo = node;
            },
            //选中
            gm_click: function (node) {
                tree.setNodeSelect(node.id, true, true);
                //let model = sview0.sviewFrame.getViewer().getScene().getShapes().getShape(node.id);
                //if (model && model.isVisible()) {
                nodeSelect(node);
                //}
            },
            gm_contextmenu: function (node) {
                //tree.nodeUnfold(node.id);
                tree.setNodeSelect(node.id, true, true);
                //nodeSelect(node);
                addedFileParentNode = node;
                if (!node.install)
                    tree.setNodeSelect(node.id, true, true);
                nodeRightClick(node, "treeRightClickMenu");
            },
            template: function (data) {

            },
        };
        tree = new Tree(option);
        tree.init();
        document.getElementById(AssemblyDialog.TreeDialog.id).oncontextmenu = function (e) {
            e = e || window.event;
            event.cancelBubble = true;
            if (e.preventDefault) {
                e.preventDefault()
            } else {
                window.event.returnValue = false //兼容IE6、7、8
            }
            return false;
        }
        //添加对Frame层的消息订阅
        M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.sview0.sviewFrame);
        M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.sview0.sviewFrame);
        treeMenu = uiManager.getElement(AssemblyDialog.TreeDialog.id);
        if (attributeInfo && attributeInfo.isShow()) {
            attributeInfo.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = (treeMenu.eleLabel.offsetWidth + 20) + "px";
        }
    } else {
        if (treeMenu.isShow()) {
            treeMenu.hide();
            //取消对Frame层的消息订阅
            //M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.sview0.sviewFrame);
        } else {
            treeMenu.show();
            tree.reload();
            if (attributeInfo && attributeInfo.isShow()) {
                attributeInfo.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = (treeMenu.eleLabel.offsetWidth + 20) + "px";
            }
        }
    }
}

//模型拾取事件回调(更新装配树)
function update(msg) {
    let treeMenu = sview0.sviewFrame.getUIManager().getElement(AssemblyDialog.TreeDialog.id);
    //if ((!treeMenu || !treeMenu.isShow())) {
    //    //取消对Frame层的消息订阅
    //    M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.sview0.sviewFrame);
    //    return;
    //}
    if (msg instanceof M3D.Utility.VisiableMessage) {
        let state = msg.state;
        let shapes = msg.shapes;
        if (state === M3D.Utility.OperaState.HotOpenFile) {
            var topModel = sview0.sviewFrame.getViewer().getScene().getTopModel();
            let data = [new SView.JsonAssembly(topModel, true, true)];
            data[0].checked = true;
            tree.reload(data);
        }
        if (state === M3D.Utility.OperaState.VisibleAdd) {
            if (!shapes || shapes.length === 0 || !tree) {
                return;
            }
            for (let [key, val] of shapes.entries()) {
                if (key === 4) {
                    tree.setChecked(val, true);
                    //for (let i in val) {
                    //    let shape = val[i];
                    //    if (shape && shape instanceof M3D.Shapes.Model) {
                    //        var node = tree.getNodeById(shape.getId(), false);
                    //        if (node && node.checkedInfo == 3) {
                    //            tree.setChecked(node.id, true);
                    //        }
                    //    }
                    //}
                }
            }
        }
        if (state === M3D.Utility.OperaState.Remove) {
            if (!shapes || shapes.length === 0 || !tree) {
                return;
            }
            for (let [key, val] of shapes.entries()) {
                if (key === 4) {
                    tree.setChecked(val, false);
                    //for (let i in val) {
                    //    let shape = val[i];
                    //    if (shape && shape instanceof M3D.Shapes.Model) {
                    //        let node = tree.getNodeById(shape.getId(), false);
                    //        if (node && node.checkedInfo == 2) {
                    //            tree.setChecked(node.id, false);
                    //        }
                    //    }
                    //}
                }
            }
        }
    }
    else if (msg instanceof M3D.Utility.SelectorMessage) {
        let state = msg.state;
        let shapes = msg.shapes;
        if (state === M3D.Utility.OperaState.Clear) {
            if (tree) {
                tree.setAllNodeUnSelect();
            }
        }
        else if (state === M3D.Utility.OperaState.Remove) {
            if (!shapes || shapes.length === 0 || !tree) {
                return;
            }
            let shape = shapes[shapes.length - 1];
            if (shape && shape instanceof M3D.Shapes.Model) {
                let node = tree.getNodeById(shape.getId());
                if (!node && shape.getParentModel()) {
                    openNodes(shape);
                }
                tree.setNodeSelect(shape.getId(), false, false);
            }
        } else if (state === M3D.Utility.OperaState.Add) {

            if (!shapes || shapes.length === 0 || !tree) {
                return;
            }
            let isMultiSelect = M3D.Config.Parameters.getInstance().getBoolParameter(M3D.Config.isMultipleSelected);
            let shape = shapes[shapes.length - 1];
            if (shape && shape instanceof M3D.Shapes.Model) {
                let node = tree.getNodeById(shape.getId());
                if (!node && shape.getParentModel()) {
                    openNodes(shape);
                }
                tree.setNodeSelect(shape.getId(), true, !isMultiSelect);
            }
        }

    }
    else if (msg instanceof SView.Commands.AddFileCommandMessage) {
        if (msg.state === SView.Commands.CommandMessageState.AddNodeToTree) {
            let parentModel = msg.parentModel;
            let topModel = msg.model;
            let command = msg.command;
            parentNode = tree.getNodeById(parentModel.getId());
            let data = new SView.JsonAssembly(topModel, true, true);
            data.checked = true;
            tree.addNode(parentNode, data);
            M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, command);
        }
    }
}
/**
 * 展开未加载的节点
 * @param {any} nodes
 */
function openNodes(shape) {
    let nodeId = [];
    updateSelect(shape, shape);
    function updateSelect(model, shape) {
        let parentModel = model.getParentModel();
        if (parentModel) {
            let nodeParent = tree.getNodeById(parentModel.getId());
            if (nodeParent) {
                nodeId.unshift(parentModel.getId());
                tree.nodeAllUnfold(nodeId);
                return;
            } else {
                nodeId.unshift(parentModel.getId());
                updateSelect(parentModel, shape);
            }
        }
    }
}
/**
 * 展开多个节点nodes
 * @param {any} nodes
 */
function expandNodes(nodes) {
    if (tree && nodes && nodes.length > 0) {
        for (let i = 0; i < nodes.length; i++) {
            let node = nodes[i];
            if (node) {
                tree.expandNode(node);
            }
        }
    }
}

function closeAssemblyDialog(elementId, btnId = null) {
    // 删除相应标签
    sview0.sviewFrame.getUIManager().hideLabel(elementId);
    // 对应按钮取消高亮
    if (btnId !== null) {
        SView.Windows.Button.highLightLabel(btnId);
    }
    //取消对Frame层的消息订阅
    //M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.sview0.sviewFrame);

}


//装配树和视图列表切换
function changeTreeTabs(elementId, dataClass) {
    let tabs = document.getElementById(elementId);
    // 标签按钮
    let btns = tabs.getElementsByTagName("div");
    // 标签内容
    let tabContent = document.getElementById(dataClass);
    // 按钮高亮
    for (var i = 0; i < btns.length; i++) {
        let btnDataClass = btns[i].dataset.class;
        if (btnDataClass === dataClass) {
            tabContent.style.display = "";
            tabContent.dataset.active = "true";
            btns[i].style.backgroundColor = "rgba(44,135,255, .5)";
            //如果是视图，则加载视图列表
            if (dataClass === "SView-viewList") {
                sview0.sviewFrame.getUIManager().hideLabel(treeSerchInput.WithBtnInput.id);
                sview0.sviewFrame.getUIManager().showLabel(viewSerchInput.WithBtnInput.id);
                //取消对Frame层的消息订阅
                /* M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.sview0.sviewFrame);*/
                ////获取视图列表模块
                //let sviewViewPanel = sview0.sviewFrame.getUIManager().getElement("sview_view_panel");
                ////如果存在则不再重新加载视图
                //if (sviewViewPanel.getAllSubComponents().childElementCount === 0) {
                //获取视图列表
                let array = new Map();
                array.set("fullClassName", "SView.Commands.ShowViewListCommand");//命令对应的类的相关信息
                let inputParas = new SView.CommandParameters();
                let inputPara = new SView.CommandParameter()
                inputPara.name = "loadFlag";
                inputPara.value = false;
                inputParas.add(inputPara);
                array.set("commandParameters", inputParas);
                getCommandsManager().execute("ShowViewList", array);
                //}
            } else {
                sview0.sviewFrame.getUIManager().hideLabel(viewSerchInput.WithBtnInput.id);
                sview0.sviewFrame.getUIManager().showLabel(treeSerchInput.WithBtnInput.id);
                tree.reload();
                //添加对Frame层的消息订阅
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.sview0.sviewFrame);
            }
        } else {
            btns[i].style.backgroundColor = "";
            let hiddenContent = document.getElementById(btnDataClass);
            hiddenContent.dataset.active = "false";
            hiddenContent.style.display = "none";
        }
    }
}
/* ------------------ 打开视图 ------------------- */
//获取Command管理类
function getCommandsManager() {
    if (commandsManager) {
        return commandsManager;
    } else {
        commandsManager = sview0.sviewFrame.getCommandsManager();
        return commandsManager;
    }
}





//发起创建视图弹窗
function createView() {
    //创建视图Command
    let array = new Map();
    array.set("fullClassName", "SView.Commands.CreateViewCommand");//命令对应的类的相关信息
    getCommandsManager().execute("CreateView", array);
}





var addedFileParentNode;
/* ------------------ 装配树右键相关方法 ------------------- */
//添加装配
var isOver = true;

function addToShapeSet() {
    if (!addedFileParentNode) {
        return;
    }
    var scene = sview0.sviewFrame.getViewer().getScene();
    var model = scene.getTopModel();
    var modelID = addedFileParentNode.id;
    if (model.id != modelID) {
        model = scene.getShapes().getShape(modelID);
    }
    if (model && (model instanceof M3D.Shapes.Model) && model.isVisible()) {
        scene.getSelector().add(model);
    }
    let array = new Map();
    array.set("fullClassName", "SView.Commands.ShapeSetCommand");
    sview0.sviewFrame.getCommandsManager().execute("ShapeSetCommand", array);
}

/**
 * 插入模型
 * */
function addAsemble() {
    if (!isOver) {
        showResultDialog('loading', '', SView[SView.UIManager.languageInfo].languageObj.Assembly.Assembly_Wait);
        return;
    }
    if (!addedFileParentNode) {
        return;
    }
    var scene = sview0.sviewFrame.getViewer().getScene();
    var shape = scene.getTopModel();
    var modelID = addedFileParentNode.id;
    if (shape.id != modelID) {
        shape = scene.getShapes().getShape(modelID);
    }
    if (!shape || !(shape instanceof M3D.Shapes.Model)) {
        return;
    }
    //let shape = sview0.sviewFrame.getViewer().getScene().getShapes().getShape(addedFileParentNode.id);
    //if (!shape || !(shape instanceof M3D.Shapes.Model)) {
    //    return;
    //}
    if (shape.getChildren().length === 0) {
        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.AssemblyCanAddModel;
        sview0.sviewFrame.getUIManager().load(MessageDialog);
        let that = this;
        setTimeout(function () {
            sview0.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
        }, 3000);
        return;
    }
    //组合命令相关参数
    let array = new Map();
    array.set("fullClassName", "SView.Commands.AddFileCommand");
    let inputParas = new SView.CommandParameters();
    let inputPara = new SView.CommandParameter()
    inputPara.name = "modelId";
    inputPara.value = modelID;
    inputParas.add(inputPara);
    array.set("commandParameters", inputParas);
    //let addFileCommand = sview0.sviewFrame.getCommandsManager().execute("AddFileCommand", array);
    let addFileCommand = sview0.sviewFrame.getCommandsManager().getCommand("AddFileCommand", array);

    sview0.sviewFrame.getCommandsManager().execute(addFileCommand);
    M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, addFileCommand);
    //asmFileInput.click();
}
//卸载和加载
function loadOrUnload(type) {
    if (!addedFileParentNode)
        return;
    var id = addedFileParentNode.id;
    if (addedFileParentNode && addedFileParentNode.install) {
        if (type == "uninstall")
            tree.setInstall(id, false);
    } else {
        if (type == "install")
            tree.setInstall(id, true);
    }
    let models = [];
    models.push(id);

    let cmdParaMap = new Map();
    cmdParaMap.set("fullClassName", "SView.Commands.LoadLodCommand");

    let inputParas = new SView.CommandParameters();
    let modelPara = new SView.CommandParameter();
    modelPara.name = "Models";
    modelPara.value = models;
    modelPara.type = SView.CommandParameterType.Array;
    modelPara.ioType = SView.IOParameterType.INPUT_PARAM;
    inputParas.push(modelPara);
    let loadPara = new SView.CommandParameter();
    loadPara.name = "Loaded";
    loadPara.value = type == "install" ? true : false;
    loadPara.type = SView.CommandParameterType.BOOL;
    loadPara.ioType = SView.IOParameterType.INPUT_PARAM;
    inputParas.push(loadPara);
    cmdParaMap.set(SView.CommonStrings.commandParameters, inputParas);

    sview0.sviewFrame.getCommandsManager().execute("LoadLod", cmdParaMap);
    sview0.sviewFrame.getUIManager().hideLabel("treeRightClickMenu");
}
//删除
function confirmRemoveAsemble() {
    var selectedNoedes = tree.getSelected();
    DeleteDialog.Dialog.content = "确认删除" + selectedNoedes.name + "吗？";
    DeleteDialog.Dialog.buttons[0].Button.onClick = "removeAsemble('confirm-delete')";
    sview0.sviewFrame.getUIManager().load(DeleteDialog);
}
function removeAsemble(elementId) {
    var selectedNoedes = tree.getSelected();
    selectedNoedes.remove();
    sview0.sviewFrame.getUIManager().removeLabel(elementId);
}

//node-Check事件
function nodeCheckChange(node) {
    if (!tree || !node) {
        return;
    }

    let modelID = node.id;
    let scene = sview0.sviewFrame.getViewer().getScene();
    if (!scene) {
        return;
    }
    let changeModel;
    //case top model
    let topModel = scene.getTopModel();
    if (topModel && modelID === topModel.getId()) {
        changeModel = topModel.getChildren();
    } else {
        let model = scene.getShapes().getShape(modelID);
        if (!model) {
            return;
        }
        changeModel = model;
    }

    let showAction = new SView.Actions.ShowModelAction();
    showAction.setParameter(scene, changeModel, node.checkedInfo == 3 ? false : true);
    showAction.execute();
    scene.refresh();
}

//node选中事件
function nodeSelect(node) {
    if (!tree || !node) {
        return;
    }
    let selected = tree.isNodeSelected(node);
    let modelID = node.id;
    if (modelID < 0) {
        return;
    }
    let scene = sview0.sviewFrame.getViewer().getScene();
    var model = scene.getTopModel();

    if (model.id != modelID) {
        model = scene.getShapes().getShape(modelID);
        if (!model || model.isSelected() === selected || !model.isVisible()) {
            return;
        }
    }

    let selectedAction = new SView.Actions.SelectModelAction();
    selectedAction.setParameter(scene, model, selected);
    selectedAction.execute();
    scene.refresh();
}

var treeRightMenu;
// //oncontextmenu上下文菜单事件，右键菜单
// //自定义一个浏览器右键菜单，单击右键是显示它
function offsetTree(ele) {
    var obj = { t: 0, l: 0 };
    obj.l = ele.offsetLeft;
    obj.t = ele.offsetTop;
    return obj
}
function nodeRightClick(node, rightClickMenuId) {
    let ele = document.querySelector('.tree-node-name-active[tree-id="' + node.id + '"]');
    treeRightMenu = document.getElementById(rightClickMenuId);
    //显示我们自己定义的右键菜单
    //1.找到菜单
    //提取到函数外面作为全局变量

    //兼容Event对象
    let e = window.event;
    event.cancelBubble = true;
    if (e.preventDefault) {
        e.preventDefault()
    } else {
        window.event.returnValue = false //兼容IE6、7、8
    }
    //鼠标坐标
    var offsetObj = offsetTree(ele);
    let top = offsetObj.t;
    let left = offsetObj.l;
    //3.显示右键菜单
    sview0.sviewFrame.getUIManager().showLabel("treeRightClickMenu");
    let installBtn = sview0.sviewFrame.getUIManager().getElement("treeLoad");
    let unInstallBtn = sview0.sviewFrame.getUIManager().getElement("treeUninstall");
    let addTreeBtn = sview0.sviewFrame.getUIManager().getElement("treeAdd");
    let addShapesetBtn = sview0.sviewFrame.getUIManager().getElement("tree_add_to_shapeset");
    if (node.install) {
        installBtn.setIsDisable(true);
        unInstallBtn.setIsDisable(false);
        addShapesetBtn.setIsDisable(false);
        addTreeBtn.setIsDisable(false);
    } else {
        unInstallBtn.setIsDisable(true);
        installBtn.setIsDisable(false);
        addShapesetBtn.setIsDisable(true);
        addTreeBtn.setIsDisable(true);
    }
    var scene = sview0.sviewFrame.getViewer().getScene();
    if (scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Jewel)
        || scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Diamond)
        || scene.getMaterialManager().hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Metal)
        || scene.getMaterialManager().hasSpecificTypeMaterial(102)) {
        addShapesetBtn.setIsDisable(true);
        addTreeBtn.setIsDisable(true);
    }
    if ((left + treeRightMenu.offsetWidth) > (document.getElementById("SView-assemblyTree").offsetWidth + document.getElementById("SView-assemblyTree").scrollLeft)) {
        left = document.getElementById("SView-assemblyTree").offsetWidth - treeRightMenu.offsetWidth - 20;
    }
    treeRightMenu.style.left = left + "px";
    if ((offsetObj.t + treeRightMenu.offsetHeight + 30) > document.getElementById("SView-assemblyTree").offsetHeight) {
        top = offsetObj.t - treeRightMenu.offsetHeight - 20;
    }
    treeRightMenu.style.top = top + 20 + "px";
    //阻止默认的右键菜单显示
    return false;
};

////不需要-隐藏右键菜单
//document.onclick = function () {

//}
function updateNodes(node) {
    let nodeId = [];
    updateSelect(node, node);
    function updateSelect(model, shape) {
        let parentModel = model.parent;
        if (parentModel.name) {
            let nodeParent = tree.getNodeById(parentModel.id);
            if (nodeParent) {
                nodeId.unshift(parentModel.id);
                tree.nodeAllUnfold(nodeId);
                return;
            } else {
                nodeId.unshift(parentModel.id);
                updateSelect(parentModel, shape);
            }
        }
    }
}
/* ------------------ 检查输入框内容长度 ------------------- */
function checkLength(elementId, id) {
    var element = document.getElementById(elementId);
    var fatherElement = element.parentNode;
    var warning = fatherElement.getElementsByClassName("SView-warningPopover")[0];
    if (warning && element.value === "") {
        warning.style.display = "";
        setTimeout(function () {
            warning.style.display = "none";
        }, 3000)
    } else {
        //warning = fatherElement.getElementsByTagName("p")[0];
        if (element.value.length === 0) {
            warning.style.display = "";
        } else {
            warning.style.display = "none";
            if (searchModelText !== element.value) {
                searchModelIndex = -1;
                searchModelList = [];
                searchModelText = element.value;
            }
            if (id === AssemblyDialog.TreeDialog.id) {
                let selector = sview0.sviewFrame.getViewer().scene.getSelector();
                if (searchModelIndex < 0) {
                    searchModelList = tree.getNodeByName(element.value) ? tree.getNodeByName(element.value) : [];
                    searchModelIndex = 0;
                    if (searchModelList[searchModelIndex]) {
                        selector.clear();
                        let node = tree.getNodeById(searchModelList[searchModelIndex].id);
                        let shape = sview0.sviewFrame.getViewer().scene.getShapes().getShape(searchModelList[searchModelIndex].id);
                        if (!node && shape.getParentModel()) {
                            openNodes(shape);
                        }
                        tree.setNodeSelect(searchModelList[searchModelIndex].id, true, true);
                        nodeSelect(node);
                    } else {
                        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.SearchOver;
                        sview0.sviewFrame.getUIManager().load(MessageDialog);
                        setTimeout(function () {
                            sview0.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                        }, 3000);
                    }
                } else {
                    if (searchModelIndex < searchModelList.length - 1) {
                        searchModelIndex++;
                    } else {
                        searchModelIndex = 0;
                    }
                    if (searchModelList[searchModelIndex]) {
                        selector.clear();
                        let node = tree.getNodeById(searchModelList[searchModelIndex].id);
                        let shape = sview0.sviewFrame.getViewer().scene.getShapes().getShape(searchModelList[searchModelIndex].id);
                        if (!node && shape.getParentModel()) {
                            openNodes(shape);
                        }
                        tree.setNodeSelect(searchModelList[searchModelIndex].id, true, true);
                        nodeSelect(node);
                    } else {
                        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.SearchOver;
                        sview0.sviewFrame.getUIManager().load(MessageDialog);
                        setTimeout(function () {
                            sview0.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                        }, 3000);
                    }
                }


            }
        }
    }
    if (elementId === "meetingNickName") {
        nickName = element.value;
    }
}
//视图搜索
function searchView(inputId, listId) {
    var searchtex = document.getElementById(inputId).value.toLowerCase();
    if (searchtex != '' && searchtex.length > 0) {
        var viewitems = document.getElementById(listId).querySelectorAll(".SView-pull-left-item");
        if (presearchtex != searchtex) {
            hasserachedviewid = [];
            presearchtex = searchtex;
            if (document.getElementsByClassName("SView-pull-left-item-active").length > 0) {
                let activeViewItem = sview0.sviewFrame.getUIManager().getElement(document.getElementsByClassName("SView-pull-left-item-active")[0].getAttribute("view-id"));
                activeViewItem.setIsSelected(false);
            }
        }

        for (var i = 0; i < viewitems.length; i++) {
            var vitem = viewitems[i];
            var vid = vitem.getAttribute("view-id");
            var vname = vitem.querySelector(".SView-view-name").querySelector("span").innerHTML;
            vname = transferredMeaning(vname);
            var vcreator = vitem.querySelector(".SView-view_creator") ? vitem.querySelector(".SView-view_creator").innerHTML : null;
            var vtime = vitem.querySelector(".SView-view_time") ? vitem.querySelector(".SView-view_time").innerHTML : null;
            var vmname = vitem.querySelector(".SView-view_modelName") ? vitem.querySelector(".SView-view_modelName").innerHTML : null;
            if ((vname != null && vname.toString().toLowerCase().indexOf(searchtex) > -1) ||
                (vcreator != null && vcreator.toString().toLowerCase().indexOf(searchtex) > -1) ||
                (vtime != null && vtime.toString().toLowerCase().indexOf(searchtex) > -1) ||
                (vmname != null && vmname.toString().toLowerCase().indexOf(searchtex) > -1)) {
                if (hasserachedviewid.indexOf(vid) > -1) {
                    if (i == (viewitems.length - 1)) {
                        hasserachedviewid = [];
                        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.SearchOver;
                        sview0.sviewFrame.getUIManager().load(MessageDialog);
                        setTimeout(function () {
                            sview0.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                        }, 3000);
                    }
                    continue;
                } else {
                    if (document.getElementsByClassName("SView-pull-left-item-active").length > 0) {
                        let activeViewItem = sview0.sviewFrame.getUIManager().getElement(document.getElementsByClassName("SView-pull-left-item-active")[0].getAttribute("view-id"));
                        activeViewItem.setIsSelected(false);
                    }
                    let viewItem = sview0.sviewFrame.getUIManager().getElement(vid)
                    viewItem.setIsSelected(true);
                    document.getElementById("sview_view_panel").scrollTo(0, document.getElementById(vid).offsetTop - 60);
                    hasserachedviewid.push(vid);
                    break;
                }

            } else {
                if (i == (viewitems.length - 1)) {
                    hasserachedviewid = [];
                    MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.SearchOver;
                    sview0.sviewFrame.getUIManager().load(MessageDialog);
                    setTimeout(function () {
                        sview0.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                    }, 3000);
                }
            }
        }
    } else {
        hasserachedviewid = [];
        presearchtex = "";
        if (document.getElementsByClassName("SView-pull-left-item-active").length > 0) {
            let activeViewItem = sview0.sviewFrame.getUIManager().getElement(document.getElementsByClassName("SView-pull-left-item-active")[0].getAttribute("view-id"));
            activeViewItem.setIsSelected(false);
        }
    }
}


function transferredMeaning(str) {
    str = str.replaceAll("&amp;", "&");
    str = str.replaceAll("&lt;", "<");
    str = str.replaceAll("&gt;", ">");
    str = str.replaceAll("&quot;", "\"");
    str = str.replaceAll("&times;", "×");
    str = str.replaceAll("&divide;", "÷");
    return str;

}
