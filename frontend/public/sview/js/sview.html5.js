//import { Frame } from '../js/sview.frame';
//import { Command } from '../js/sview.command';
//function SView() {
//    this.sviewframe = null;

//import { FrameMain } from "./sview.frame";

//    this.initSView = function (labelId) {
//        this.sviewframe = createSViewObject("FrameMain");
//        this.createViewer(labelId);
//        return this.sviewFrame;
//    }
//    this.createViewer = function (labelId) {
//        this.sviewFrame.createViewer(labelId);

//    }

//    this.getViewer = function () {
//        return this.sviewFrame.getViewer();
//    }

//    this.initParameters = function () {

//    }

//    this.createSelectorListener = function (onSelect, unSelect, onClear) {

//    }

//    this.createAnnotationCommandListener = function (onStart, onEnd, onError, onSuccess, onStateChanged) {
//        let annotationCommandListener = new Command.AnnotationCommandListener();
//        annotationCommandListener.onStart = onStart;
//        annotationCommandListener.onStateChanged = onStateChanged;
//        annotationCommandListener.onEnd = onEnd;
//        annotationCommandListener.onError = onError;
//        annotationCommandListener.onSuccess = onSuccess;
//    }

//    this.createMeasureCommandListener = function (onStart, onEnd, onError, onSuccess, onStateChanged) {

//    }
//}
var sview0 = null;
class Main {
    sviewFrame = null;

    constructor(labelId) {
        this.initSView(labelId);

    }

    initSView(labelId) {
        this.sviewFrame = SView.createSViewObject(SView, "FrameMain", labelId);
        //this.createViewer(labelId);
        this.sviewFrame.createCanvas(labelId);
        this.sviewFrame.getViewer().initEventListener();
        //添加对Frame层的消息订阅
        M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.sviewFrame);

        return this.sviewFrame;
    }

    createViewer(labelId) {
        this.sviewFrame.createCanvas(labelId);
    }

    getViewer() {
        return this.sviewFrame.getViewer();
    }

    initParameters() {

    }

    createSelectorListener(onSelect, unSelect, onClear) {

    }

    createAnnotationCommandListener(onStart, onEnd, onError, onSuccess, onStateChanged) {
        let annotationCommandListener = new SView.Commands.AnnotationCommandListener();
        annotationCommandListener.onStart = onStart;
        annotationCommandListener.onStateChanged = onStateChanged;
        annotationCommandListener.onEnd = onEnd;
        annotationCommandListener.onError = onError;
        annotationCommandListener.onSuccess = onSuccess;
    }

    createMeasureCommandListener(onStart, onEnd, onError, onSuccess, onStateChanged) {


    }


    update(msg) {
        // console.error(`我是[sview]，收到了新消息,内容为： ${msg.state}`);
        switch (msg.type) {
            //修改右键菜单是否可用(msg.state= "true")
            case "ChangeRightMenuClick":
                {
                    let availableRightMenuClick = msg.state && msg.state === "true";
                    changeRightMenuClickable(availableRightMenuClick);
                    
                }
                break;
            case "MoveTypeCommand":
                {
                    let state = msg.state;
                    if (state) {
                        if (state === "onInitialize") {
                            changeRightMenuClickable(false);
                        } else if (state === "onClose") {
                            changeRightMenuClickable(true);
                        }
                    }
                }
                break;
            case "PlayAnimationCommand":
                if (msg.state === "exit") {
                    // 显示上一级菜单
                    this.sviewFrame.getUIManager().showLabel(MainBottomMenu.BottomMenu.id);
                    // 显示右侧菜单
                    if (hiddenRightMenu.indexOf(MainBottomMenu.BottomMenu.id) == -1) {
                        sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
                    }
                    //sview0.sviewFrame.getViewer().getScene().restoreView();
                    changeRightMenuClickable(true);
                } else if (msg.state === SView.Commands.PlayAnimationCommand.HideBottomLeftMenu) {
                    //隐藏是否已经保存视图的提示框
                    //this.sviewFrame.getUIManager().removeLabel(ExitDialog.Dialog.id);
                    // 隐藏当前底部菜单
                    this.sviewFrame.getUIManager().hideLabel(MainBottomMenu.BottomMenu.id);
                    // 隐藏右侧菜单
                    hideRightMenus();
                    //屏蔽右键菜单
                    changeRightMenuClickable(false);

                } else if (msg.state === SView.Commands.PlayAnimationCommand.LoadAnimationDataError) {
                    //隐藏是否已经保存视图的提示框
                    //this.sviewFrame.getUIManager().removeLabel(ExitDialog.Dialog.id);
                    MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.NoAnimationFile;
                    this.sviewFrame.getUIManager().load(MessageDialog);
                    let that = this;
                    setTimeout(function () {
                        that.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                    }, 3000);
                    // 显示上一级菜单
                    this.sviewFrame.getUIManager().showLabel(MainBottomMenu.BottomMenu.id);
                    // 显示右侧菜单
                    if (hiddenRightMenu.indexOf(MainBottomMenu.BottomMenu.id) == -1) {
                        sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
                    }
                    //sview0.sviewFrame.getViewer().getScene().restoreView();
                    changeRightMenuClickable(true);
                }
                break;
            case "CreateClipPlaneCommandView":
                //if (msg.state == "show") {
                //    let uiManager = sview0.sviewFrame.getUIManager();
                //    let sectionDialog = uiManager.getElement("section-shapeset-dialog");
                //    let shapesetDialog = uiManager.getElement("shapeset-dialog");
                //    if (shapesetDialog) {
                //        if (shapesetDialog.isShow()) {
                //            if (shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetLeft < shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth) {
                //                sectionDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "calc(50% - " + shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth / 2 + "px)";
                //            } else {
                //                sectionDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "0px";
                //            }
                //        } else {
                //            /*sectionDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "calc(50% - " + shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth / 2 + "px)";*/
                //        }
                //    }
                //}
                break;
            case "ShapeSetCommandView":
                //if (msg.state == "show") {
                //    let uiManager = sview0.sviewFrame.getUIManager();
                //    let sectionDialog = uiManager.getElement("section-shapeset-dialog");
                //    let shapesetDialog = uiManager.getElement("shapeset-dialog");
                //    if (sectionDialog) {
                //        if (sectionDialog.isShow()) {
                //            if (sectionDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetLeft < sectionDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth) {
                //                shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "calc(50% - " + shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth / 2 + "px)";
                //            } else {
                //                shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "0px";
                //            }
                //            //shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "0px";
                //        } else {
                //            /*shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "calc(50% - " + shapesetDialog.eleLabel.getElementsByClassName("SView-dialog")[0].offsetWidth / 2 + "px)";*/
                //        }
                //    }
                //}
                break;
            case "PropertyCommandView":
                if (msg.state == "show") {
                    let uiManager = sview0.sviewFrame.getUIManager();
                    let treeMenu = uiManager.getElement(AssemblyDialog.TreeDialog.id);
                    let attributeInfo = uiManager.getElement(SView.Commands.AttributeDialog.Dialog.id);
                    if (treeMenu && treeMenu.isShow()) {
                        attributeInfo.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = (treeMenu.eleLabel.offsetWidth + 20) + "px";
                    } else {
                        attributeInfo.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "10px";
                    }
                } else if (msg.state == "exit")
                    SView.Windows.Button.highLightLabel("attributeInfo", false);
                break;
            case /*SView.Commands.CommandNames.DRAWMODECOMMAND*/"DrawMode":
                {
                    MessageDialog.Message.content = msg.info;
                    this.sviewFrame.getUIManager().load(MessageDialog);
                    let that = this;
                    setTimeout(function () {
                        that.sviewFrame.getUIManager().removeLabel(MessageDialog.Message.id);
                    }, 3000);
                }
               
                break;
            case "ClipPlaneCommand":
                // 显示上一级菜单
                this.sviewFrame.getUIManager().showLabel(MainBottomMenu.BottomMenu.id);
                // 显示右侧菜单
                if (hiddenRightMenu.indexOf(MainBottomMenu.BottomMenu.id) == -1) {
                    sview0.sviewFrame.getUIManager().showLabel(RightMenu.RightMenu.id);
                }
                break;


        }

    }

}