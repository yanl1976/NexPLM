var SView;
(function (SView) {
    /**
    *@memberof SView
    *@extends M3D.Utility.Logger
    *@description     Frame模块日志类
    *@author	YW
    *@date		2022-3-23
    *@version	1.0
    */
    class FrameLogger extends M3D.Utility.Logger {
        /**
         * @description 输出Error日志
         * @param {string} moduleName 模块名称
         * @param {string} threadName 线程名称
         * @param {string} message 描述
         */
        static FRAMELOGE(moduleName, threadName, message) {
            M3D.Utility.Logger.LOGE(this.SPAND_FRAME, moduleName, threadName, message);
        }
        /**
         * @description 输出Warning日志
         * @param moduleName 模块名称
         * @param threadName 线程名称
         * @param message 描述
         */
        static FRAMELOGW(message, moduleName, threadName) {
            M3D.Utility.Logger.LOGW(this.SPAND_FRAME, moduleName, threadName, message);
        }
        /**
         * @description 输出Info日志
         * @param moduleName 模块名称
         * @param threadName 线程名称
         * @param message 描述
         */
        static FRAMELOGI(message, moduleName = "", threadName = "") {
            M3D.Utility.Logger.LOGI(this.SPAND_FRAME, moduleName, threadName, message);
        }
        /**
        * @description 输出Debug日志
        * @param moduleName 模块名称
        * @param threadName 线程名称
        * @param message 描述
        */
        static FRAMELOGD(message, moduleName = "", threadName = "") {
            M3D.Utility.Logger.LOGD(this.SPAND_FRAME, moduleName, threadName, message);
        }
    }
    FrameLogger.SPAND_FRAME = 'Frame';
    SView.FrameLogger = FrameLogger;
})(SView || (SView = {}));
var SView;
(function (SView) {
    SView.NULLSTRING = "";
    SView.ID = "id";
    SView.COMMANDMODULENAME = "Command";
    SView.ONINTERRUPTEXCUTING = "OnInterrupt executing command name ";
    SView.ONINTERRUPTEXCUTED = "OnInterrupt executed command name ";
    SView.LOADPARASFROMOBJECTEXECUTING = "LoadParasFromObject executing command name ";
    SView.PARAMETEREXIST = "The parameter does not exist";
    SView.PARAMETEROVERIDEFAIL = "The original parameter is not allowed to be overwritten ";
    SView.PARAMETEROVERIDESUCCESS = "The original parameter was rewritten successfully";
    SView.ROOT = "root";
    SView.MODEULES = "modules";
    SView.NAME = "name";
    SView.TYPE = "type";
    SView.VALUE = "value";
    SView.ISOVERRIDE = "isOverride";
    SView.ASSEMBLY = "assembly";
    SView.COMMANDS = "commands";
    SView.FULLCLASSNAME = "fullClassname";
    SView.PRIORITY = "priority";
    SView.INTERRUPTILBE = "interruptible";
    SView.VIEWNAME = "viewName";
    SView.EXECUTEDHANDLEMODE = "executedHandleMode";
    SView.ISPREVIEW = "isPreView";
    SView.INPUTPARAMATERS = "inputParameters";
    SView.OUTPUTPARAMATERS = "outputParameters";
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @description 语言类别
     * @enum {number}
     */
    let LanguageType;
    (function (LanguageType) {
        LanguageType[LanguageType["Chinese"] = 0] = "Chinese";
        LanguageType[LanguageType["English"] = 1] = "English";
        LanguageType[LanguageType["Japanese"] = 2] = "Japanese";
    })(LanguageType = SView.LanguageType || (SView.LanguageType = {}));
})(SView || (SView = {}));
/// <reference path="../../../lib/m3d/M3D.d.ts" />
/// <reference path="../../../lib/base/S3DView.d.ts" />
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description SViewFrame入口类，包含SViewFrame层的初始化等方法
     * @author YW
     * @date 2022-1-17
     * @version	1.0
     * @implements {M3D.Utility.Subscriber}
     * @implements {M3D.Utility.Publisher}
     */
    class FrameMain {
        /**
         * 构造函数
         * */
        constructor(labelId) {
            //SView入口
            this.viewer = null;
            //commands管理器
            this.commandManager = null;
            //布局管理器
            this.uiManager = null;
            //配置
            this.configManager = null;
            this.commandEvent = [];
            this.initSViewFrame(labelId);
        }
        /**
         * @private
         * @description 初始化SViewFrame
         * @param {string} labelId
         */
        initSViewFrame(labelId) {
            if (!this.viewer) {
                SView.FrameLogger.FRAMELOGD(this.initSViewFrame.name + M3D.START);
                this.viewer = createSViewObject(SView, "S3DView");
                this.configManager = SView.FrameConfigure.getInstance();
                //this.sviewBase = new Viewer();
                this.commandManager = createSViewObject(SView, "CommandManager", this);
                //this.commandManager = new CommandManager(this);
                this.uiManager = createSViewObject(SView, "UIManager", labelId);
                //this.uiManager = new UIManager(labelId);
                this.initCommandTouchListener();
                //this.createCanvas(labelId);
                //添加对base层的消息订阅
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.viewer);
                //添加对CommandManager的消息订阅
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.commandManager);
                SView.FrameLogger.FRAMELOGD(this.initSViewFrame.name + M3D.END);
                // 绑定resize监听事件
                this.resizeListener();
                this.initFrameData();
            }
        }
        /**
         * @public
         * @description 窗口绑定resize侦听事件
         */
        resizeListener() {
            // resize事件绑定在window上
            window.addEventListener("resize", () => {
                let canvas = this.getCanvas();
                if (canvas !== null) {
                    canvas.width = canvas.offsetWidth * window.devicePixelRatio;
                    canvas.height = canvas.offsetHeight * window.devicePixelRatio;
                    let scene = this.viewer.getScene();
                    if (scene !== null) {
                        let viewport = scene.getViewPorts().getViewport(0);
                        if (viewport != null) {
                            viewport.resizeRect(canvas.offsetLeft, canvas.offsetWidth, canvas.offsetTop, canvas.offsetHeight);
                            console.log("window" + M3D.Utility.TimeHelper.getCurrentTime());
                        }
                        else {
                            SView.FrameLogger.FRAMELOGW("Viewport is undefined");
                        }
                    }
                    else {
                        SView.FrameLogger.FRAMELOGW("Scene is undefined");
                    }
                }
                else {
                    SView.FrameLogger.FRAMELOGW("Canvas is undefined");
                }
            });
        }
        /**
         *
         * @description 初始化数据
         * */
        initFrameData() {
            this.commandManager.initData();
        }
        /**
         * @description 获取commands管理器
         * */
        getCommandsManager() {
            return this.commandManager;
        }
        /**
         * @description 创建渲染Canvas
         * @param labelId
         */
        createCanvas(labelId) {
            SView.FrameLogger.FRAMELOGD(this.createCanvas.name + M3D.START);
            let canvas = this.viewer.createCanvas();
            if (!canvas) {
                return false;
            }
            //let canvas = this.viewer.getCanvas();
            let label = document.getElementById(labelId);
            label.appendChild(canvas);
            canvas.style.width = "100%";
            canvas.style.height = "100%";
            canvas.style.position = "absolute";
            canvas.style.zIndex = "-2";
            let v = this.viewer.getScene().getViewPorts().getViewport(0);
            v.setRect(new M3D.M3DMath.Rect(0, 0, canvas.clientWidth, canvas.clientHeight));
            SView.FrameLogger.FRAMELOGD(this.createCanvas.name + M3D.END);
            return true;
        }
        /**
         * @description 获取渲染Canvas元素
         * */
        getCanvas() {
            return this.viewer.getCanvas();
        }
        /**
         * @private
         * @description 初始化鼠标事件监听
         * */
        initCommandTouchListener() {
            SView.FrameLogger.FRAMELOGD(this.initCommandTouchListener.name + M3D.START);
            let touchListener = new SView.CommandTouchListener(this);
            this.viewer.getEventListenersManager().addEventListener(touchListener);
            SView.FrameLogger.FRAMELOGD(this.initCommandTouchListener.name + M3D.END);
        }
        /**
         * @public
         * @description 添加鼠标或键盘事件监听
         * @param { SView.EventListener} listener
         */
        addEventListener(listener) {
            if (!listener) {
                SView.FrameLogger.FRAMELOGW("EventListener is undefined");
                return false;
            }
            SView.FrameLogger.FRAMELOGI(this.addEventListener.name + M3D.START);
            let eventListenerManager = this.viewer.getEventListenersManager();
            if (!eventListenerManager) {
                SView.FrameLogger.FRAMELOGW("EventListenerManager is undefined");
                return false;
            }
            eventListenerManager.addEventListener(listener);
            SView.FrameLogger.FRAMELOGI(this.addEventListener.name + M3D.END);
            return true;
        }
        /**
         * @public
         * @description 移除鼠标或键盘事件监听
         * @param { SView.EventListener}  listener
         */
        removeEventListener(listener) {
            if (!listener) {
                SView.FrameLogger.FRAMELOGW("EventListener is undefined");
                return false;
            }
            SView.FrameLogger.FRAMELOGD(this.removeEventListener.name + M3D.START);
            let eventListenerManager = this.viewer.getEventListenersManager();
            if (!eventListenerManager) {
                SView.FrameLogger.FRAMELOGW("EventListenerManager is undefined");
                return false;
            }
            eventListenerManager.removeEventListener(listener);
            SView.FrameLogger.FRAMELOGD(this.removeEventListener.name + M3D.END);
            return true;
        }
        /**
         * @public
         * @description 获取sviewBase入口
         * */
        getViewer() {
            return this.viewer;
        }
        /**
         * @description 从视口中选择点
         * @param x
         * @param y
         * @param isFeature 是否使用特征点
         */
        selectPoint(isFeature, event) {
            //调用base层封装的接口，选择点，返回点的ID
            SView.FrameLogger.FRAMELOGD(this.selectPoint.name + M3D.START);
            let pointId = 0;
            pointId = this.viewer.selectPoint(isFeature, event);
            SView.FrameLogger.FRAMELOGD(this.selectPoint.name + M3D.END);
            return pointId;
        }
        /**
         * @public
         * @description 通过id获取Shape
         * @param id
         */
        getShape(id, shapeType) {
            SView.FrameLogger.FRAMELOGD(this.getShape.name + M3D.START);
            let shape = null;
            if (typeof id === "number") {
                //调用base层接口，通过id获取相应的shape
                let scene = this.viewer.getScene();
                shape = scene.getShapes().getShape(id);
            }
            else {
                SView.FrameLogger.FRAMELOGW("typeof id is not number");
            }
            SView.FrameLogger.FRAMELOGD(this.getShape.name + M3D.END);
            return shape;
        }
        /**
         * @public
         * @description  通过id删除shape
         * @param id
         */
        removeShape(id, shapeType) {
            SView.FrameLogger.FRAMELOGD(this.removeShape.name + M3D.START);
            let result = false;
            if (typeof id === "number") {
                //调用base层接口，通过id删除相应的shape
                let scene = this.viewer.getScene();
                result = scene.getShapes().removeShape(id);
            }
            else {
                SView.FrameLogger.FRAMELOGW("typeof id is not number");
            }
            SView.FrameLogger.FRAMELOGD(this.removeShape.name + M3D.END);
            return result;
        }
        /**
         * @public
         * @description 获取UIManager
         * */
        getUIManager(path) {
            if (path) {
                this.uiManager.setRootPath(path);
            }
            return this.uiManager;
        }
        /**
         * @description 命令阶段
         * */
        fireCommandProcess(command, process) {
            this.fireEvent(command, process, null);
        }
        /**
         * @private
         * @description 发送状态事件
         * @param command
         * @param msg
         * @param args
         */
        fireEvent(command, msg, args) {
            if (this.commandEvent && this.commandEvent.length > 0) {
                for (let fun of this.commandEvent) {
                    fun(this.getCommandsManager(), command, msg, args);
                }
            }
        }
        /**
         * @public
         * @description 绑定命令执行进度事件
         * @param eventHandler
         */
        addEventHandler(eventHandler) {
            this.commandEvent.push(eventHandler);
        }
        /**
         * @public
         * @description 解绑命令执行进度事件
         * @param eventHandler
         */
        removeEventHandler(eventHandler) {
            let index = this.commandEvent.indexOf(eventHandler);
            if (index >= 0) {
                this.commandEvent.splice(index, 1);
            }
        }
        /**
         * @description 获取配置管理器
         * @returns {FrameConfigure}
         */
        getConfigManager() {
            return this.configManager;
        }
        open(...pars) {
            this.viewer.open(...pars);
        }
        /**
         * @public
         * @description 取消操作
         */
        cancle() {
            this.viewer.cancle();
        }
        /**
         * @private
         * @param {M3D.Utility.M3DMessage} msg
         */
        send(msg) {
            M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
        }
        /**
         * @private
         * @param {M3D.Utility.M3DMessage} msg
         */
        post(msg) {
            M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
        }
        /**
         * @private
         * @param {M3D.Utility.M3DMessage} msg
         */
        update(msg) {
            if (msg.handler) {
                msg.handler(`我来着${FrameMain.name}`);
            }
            this.send(msg);
        }
    }
    SView.FrameMain = FrameMain;
    /**
     * @memberof SView
     * @description 创建SView命名空间中的对象
     * @param module 命名空间
     * @param className 类名
     * @param paras 构造方法中的参数
     */
    function createSViewObject(module, className, ...paras) {
        try {
            SView.FrameLogger.FRAMELOGD(createSViewObject.name + ` ${className}` + M3D.START);
            let t = null;
            if (module[className]) {
                t = new module[className](...paras);
            }
            else {
                SView.FrameLogger.FRAMELOGW(`${module}[${className}] is not constructor`);
            }
            SView.FrameLogger.FRAMELOGD(createSViewObject.name + ` ${className}` + M3D.END);
            return t;
        }
        catch (error) {
            SView.FrameLogger.FRAMELOGW(createSViewObject.name, "MAIN", error);
        }
    }
    SView.createSViewObject = createSViewObject;
    function createObject(className, ...paras) {
        try {
            SView.FrameLogger.FRAMELOGD(createObject.name + ` ${className}` + M3D.START);
            let t = null;
            if (className.constructor) {
                t = new className(...paras);
                return t;
            }
            else {
                SView.FrameLogger.FRAMELOGW(className + "is not constructor");
            }
            SView.FrameLogger.FRAMELOGD(createObject.name + ` ${className}` + M3D.END);
            return t;
        }
        catch (error) {
            SView.FrameLogger.FRAMELOGW(createObject.name, "MAIN", error);
            //console.error(error);
        }
    }
    SView.createObject = createObject;
    //export function getModelViewList() {
    //    let methodName = M3D.Utility.BrowserHelper.getMethodName();
    //    var scene: M3D.Scene = this.viewer.getScene();
    //    if (scene !== null) {
    //        let modelViewList = scene.getLoader().getModelViews();
    //        if (modelViewList != null) {
    //            FrameLogger.FRAMELOGI(SViewFrame.name, methodName, "");
    //            viewport.resizeRect(canvas.offsetLeft, canvas.offsetWidth, canvas.offsetTop, canvas.offsetHeight);
    //            console.log("window" + M3D.Utility.TimeHelper.getCurrentTime())
    //        } else {
    //            FrameLogger.FRAMELOGW("Viewport is undefined");
    //        }
    //    } else {
    //        FrameLogger.FRAMELOGW("Scene is undefined");
    //    }
    //    return modelView;
    //}
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 选择器监听类
     * @beta
     * @hidden
     * @author nihy
     * @date 03/04/2023
     * @extends {M3D.Utility.Logger}
     */
    class SelectorListener extends M3D.Utility.Logger {
        initData() {
            //
        }
    }
    SView.SelectorListener = SelectorListener;
})(SView || (SView = {}));
/// <reference path="../../../../lib/frame/SViewControls.d.ts" />
/// <reference path="../FrameLogger.ts" />
/// <reference path="../SViewFrame.ts" />
var SView;
(function (SView) {
    /**
     *@memberof SView
     *@description    UI动态生成：入口
     *@author	ZYC
     *@date		2022-1-26
     *@version	1.0
     *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
     */
    class UIManager {
        /**
         * @constructor
         * @description  所有控件集合
         * @param {string} labelId 标签id
         */
        constructor(labelId) {
            // 所有控件集合
            /**
             * @description  所有控件集合
             */
            this.allElements = new Map();
            // 父级标签id
            this.fatherLabelId = labelId;
            UIManager.languageInfo = SView.FrameConfigure.getInstance().
                getNumberParameter(M3D.Config.language) === 1 ? UIManager.languageConstants.EN : UIManager.languageConstants.CN; //0中文 ，1英文
        }
        /**
         * @public
         * @description 读取界面控件信息
         * @param {*} elementInfo  elementsInfo 控件信息
         * @param {*} [fatherLabelId=null]  父级标签id 可选参数
         */
        load(elementInfo, fatherLabelId = null) {
            SView.FrameLogger.FRAMELOGD(this.load.name + M3D.START);
            let tagName = Object.keys(elementInfo)[0];
            // 判断元素是否已生成
            let element = document.getElementById(elementInfo[tagName].id);
            if (element != null) {
                SView.FrameLogger.FRAMELOGD("id is repetition");
                this.removeLabel(elementInfo[tagName].id);
            }
            let newElement = SView.createSViewObject(SView.Windows, tagName);
            this.allElements.set(elementInfo[tagName].id, newElement);
            newElement.create(elementInfo);
            let fatherElement;
            if (this.fatherLabelId) {
                fatherElement = document.getElementById(this.fatherLabelId);
            }
            else {
                fatherElement = document.getElementsByTagName("body")[0];
            }
            if (fatherLabelId && document.getElementById(fatherLabelId)) {
                fatherElement = document.getElementById(fatherLabelId);
            }
            fatherElement.appendChild(newElement.eleLabel);
            SView.FrameLogger.FRAMELOGD(this.load.name + M3D.END);
        }
        /**
         * @public
         * @description 删除标签
         * @param {string} labelId 标签id
         */
        removeLabel(labelId) {
            SView.FrameLogger.FRAMELOGD(this.removeLabel.name + M3D.START);
            let remove = document.getElementById(labelId);
            if (remove) {
                let parentElement = remove.parentElement;
                if (parentElement.tagName == "LI") {
                    SView.FrameLogger.FRAMELOGD("parentNode is li");
                    parentElement.parentNode.removeChild(parentElement);
                }
                else {
                    remove.parentNode.removeChild(remove);
                }
                // 深度优先遍历：找到id所在位置
                //let find = function (elements: Map<string, any>, id: string) {
                //    for (let value of elements.values()) {
                //        if (value.allSubComponents && value.allSubComponents.size != 0) {
                //            if (value.allSubComponents.has(id)) {
                //                // 清空
                //                value.allSubComponents.set(labelId, null);
                //                // 删除
                //                value.allSubComponents.delete(labelId);
                //            } else {
                //                find(value.allSubComponents, id);
                //            }
                //        }
                //    }
                //}
                //if (this.allElements.has(labelId)) {
                //    // 清空
                //    this.allElements.set(labelId, null);
                //    // 删除
                //    this.allElements.delete(labelId);
                //} else {
                //    find(this.allElements, labelId);
                //}
                let elementMap = this.find(labelId, this.allElements);
                if (elementMap) {
                    elementMap.set(labelId, null);
                    elementMap.delete(labelId);
                }
                SView.FrameLogger.FRAMELOGD(this.removeLabel.name + M3D.START);
            }
        }
        /**
         * @public
         * @description 根据id查找标签
         * @param {string} id 标签id
         * @param {Map<string, any>} elements 元素Map 可选参数
         */
        find(id, elements) {
            SView.FrameLogger.FRAMELOGD(this.find.name + M3D.START);
            if (!elements) {
                elements = this.allElements;
            }
            if (elements.get(id)) {
                SView.FrameLogger.FRAMELOGD(this.find.name + M3D.END);
                return elements;
            }
            for (let value of elements.values()) {
                if (value && value.allSubComponents && value.allSubComponents.size != 0) {
                    if (value.allSubComponents.has(id) == true) {
                        SView.FrameLogger.FRAMELOGD(this.find.name + M3D.END);
                        return value.allSubComponents;
                    }
                    else {
                        if (this.find(id, value.allSubComponents)) {
                            return this.find(id, value.allSubComponents);
                        }
                    }
                }
            }
            SView.FrameLogger.FRAMELOGD("element is not defined");
            return null;
        }
        /**
         * @public
         * @description 删除某类标签
         * @param {string} labelClass 标签类名
         */
        removeLabelByClass(labelClass) {
            SView.FrameLogger.FRAMELOGD(this.removeLabelByClass.name + M3D.START);
            let remove = document.getElementsByClassName(labelClass);
            for (let i = 0; i < remove.length; i++) {
                remove[i].parentNode.removeChild(remove[i]);
            }
            SView.FrameLogger.FRAMELOGD(this.removeLabelByClass.name + M3D.END);
        }
        /**
         * @public
         * @description 隐藏标签
         * @param {string} labelId 标签id
         */
        hideLabel(labelId) {
            SView.FrameLogger.FRAMELOGD(this.hideLabel.name + M3D.START);
            let element = this.getElement(labelId);
            if (element) {
                element.hide();
            }
            else {
                SView.FrameLogger.FRAMELOGD("label is not defined");
            }
            //let hide = document.getElementById(labelId);
            //if (!hide) {
            //    FrameLogger.FRAMELOGD("label is not defined");
            //}
            //hide.style.display = "none";
            SView.FrameLogger.FRAMELOGD(this.hideLabel.name + M3D.END);
        }
        /**
         * @public
         * @description 显示标签
         * @param {string} labelId 标签id
         */
        showLabel(labelId) {
            SView.FrameLogger.FRAMELOGD(this.showLabel.name + M3D.START);
            let element = this.getElement(labelId);
            if (element) {
                element.show();
            }
            else {
                SView.FrameLogger.FRAMELOGD("label is not defined");
            }
            //let show = document.getElementById(labelId);
            //if (!show) {
            //    FrameLogger.FRAMELOGD("label is not defined");
            //}
            //show.style.display = "";
            SView.FrameLogger.FRAMELOGD(this.showLabel.name + M3D.END);
        }
        /**
         * @public
         * @description 新增子控件标签
         * @param {*} id 标签信息
         * @param {array} newLabelInfo 插入子控件信息（数组）
         * @param {number} position 子控件插入位置
         */
        insertSubComponents(id, newLabelInfo, position) {
            SView.FrameLogger.FRAMELOGD(this.insertSubComponents.name + M3D.START);
            let ele = this.allElements.get(id);
            if (ele) {
                //let keyName = ele.constructor.name;
                ele.addSubComponents(newLabelInfo, position);
            }
            SView.FrameLogger.FRAMELOGD(this.insertSubComponents.name + M3D.END);
        }
        /**
         * @public
         * @description 获取所有控件集合
         * @returns {Map<string, any>} 所有控件的集合
         */
        getAllElements() {
            return this.allElements;
        }
        /**
         * @public
         * @description 获取某个控件
         * @param {string} id 控件的id
         * @returns 单个控件
         */
        getElement(id, elements) {
            if (!elements) {
                elements = this.allElements;
            }
            let elementsMap = this.find(id, elements);
            if (elementsMap && elementsMap.get(id)) {
                return elementsMap.get(id);
            }
            else {
                return null;
            }
        }
        /**
         * @public
         * @description 设置基础路径
         * @param {string} path 路径
         */
        setRootPath(path) {
            if (path) {
                SView.Windows.Label.setRootPath(path);
            }
        }
    }
    UIManager.languageConstants = {
        CN: "language_cn",
        EN: "language_en",
    };
    //国际化信息
    UIManager.languageInfo = M3D.Utility.CookieHelper.
        getCookie(M3D.Config.language) === "1" ? UIManager.languageConstants.EN : UIManager.languageConstants.CN;
    SView.UIManager = UIManager;
})(SView || (SView = {}));
var SView;
(function (SView) {
    class language_cn {
    }
    language_cn.languageObj = {
        "common": {
            "close": "关闭",
            "subMit": "提交",
            "confirm": "确定",
            "exit": "退出"
        },
        "warning": {
            "browserNotSupportCanvas": "您的浏览器不支持 canvas 标签",
            "browserNotSupportCanvasChangeBrowser": "您的浏览器不支持HTML5 canvas，请换一个浏览器。"
        },
        "about": {
            "about_title": "关于",
            "about_licence_title": "许可",
            "about_license_endtime": "不限期",
            "about_license_modules": "模块:",
            "about_license_creator": "制作工具:",
            "about_license_createtime": "制作时间:",
            "about_company": "HOTEAMSOFT",
            "about_license_expire": "&nbsp到期",
            "about_html_version": "HTML版本:",
            "about_m3d_version": "M3D版本:"
        },
        "Alert": {
            "Alert_Prompt": "提示",
            "Alert_Context": "需要开启漫游模式!",
            "Alert_Reload": "请刷新页面",
            "Alert_OpenMergeFace": "开启该配置项需要刷新页面后生效，同时部分测量功能无法使用",
            "Alert_CloseMergeFace": "关闭该配置项会导致性能下降，同时需要刷新页面后生效",
            "Alert_MergeFace": "请注意，使用该测量功能需要关闭合并面配置项并刷新页面后生效",
            "Alert_MeasureError": "测量对象无效",
            "Alert_SHotPotNameRepeat": "热点名称已被使用",
            "ALert_SHotPotNotName": "请输入热点名称"
        },
        "setting": {
            "Conventional": "常规",
            "Show": "显示",
            "Performance": "性能",
            "Render": "渲染",
            "Function": "功能",
            "Load": "加载",
            "Background": "背景",
            "Other": "其他",
            "Help": "帮助",
            "License": "许可",
            "About": "关于",
            "Measure": "测量",
            "Annotation": "批注",
            "Debug": "调试",
            "ShowDebugMenu": "显示调试菜单",
            "General": "通用",
            "Axis": "坐标轴",
            "DomSetting": "文档",
            "SysSetting": "系统",
            "WalkThrough": "漫游",
            "Speed": "速度",
            "WalkThrough_Career_perspective": "视野大小",
            "WalkThrough_JoyStick": "虚拟按键",
            "WalkThrough_MouseOperation": "漫游时鼠标操作",
            "VersionRecords": "版本说明",
            "BGColorDialog_title": "背景",
            "BGWaterMark_title": "水印文字",
            "Lable_User_Name": "用户名",
            "Label_Password": "密码",
            "Label_Login_button": "登录",
            "Tip_User_Name": "手机号或者电子邮箱地址",
            "setting": "设置",
            "continue_rotation": "持续旋转",
            "show_fps": "显示FPS",
            "merge_face": "面合并",
            "show_shotspot": "显示热点",
            "show_annotation": "显示批注",
            "show_rotation_center": "显示旋转中心",
            "default_show_mode": "默认显示场景",
            "show_shaft_side": "轴侧视图",
            "show_model_view": "默认视图",
            "show_model_properties": "模型属性",
            "play_animation": "播放动画",
            "WaterMark": "显示水印",
            "show_pmi": "显示PMI",
            "show_submodelpmi": "显示子件PMI",
            "show_submodelview": "显示子件视图",
            "Catia_mode": "Catia模式",
            "High_performance_mode": "高性能模式",
            "Use_LOD": "使用LOD",
            "LOD_Selection": "LOD选择方式",
            "Auto_Calculation": "自动计算",
            "Fixed_Level": "固定级别",
            "Not_More_Than_Level": "不高于级别",
            "LOD_Level": "LOD级别",
            "Small_strip_pattem": "小件剔除模式",
            "Small_strip_pattem_Model": "模型",
            "Small_strip_pattem_Screen": "屏幕",
            "small_strip_size": "小件剔除百分比",
            "Unit_of_measure": "单位",
            "Unit_of_none": "无",
            "Unit_of_mm": "毫米",
            "Unit_of_cm": "厘米",
            "Unit_of_m": "米",
            "Unit_of_Inch": "英寸",
            "Unit_of_foot": "英尺",
            "language_Type": "语言",
            "language_cn": "简体中文",
            "language_en": "English",
            "selectType": "选中效果",
            "selectedColor": "选中颜色",
            "selectType_side": "轮廓",
            "selectType_color": "颜色",
            "selectType_high": "高亮",
            "selectType_highlight": "突出显示",
            "selectType_XRay": "Xray",
            "showTrilateralEdge": "显示网格",
            "mulSelect": "多选",
            "useModelBackground": "使用模型自带背景",
            "showBox": "显示包围盒",
            "showBoxNote": "显示包围盒尺寸",
            "Show_PMI": "显示PMI",
            "edgeSelector": "设置边界样式",
            "DefaultUpward": "向上方向",
            "DefaultObservation": "观察模式",
            "FreeObservation": "自由观察",
            "OribitObservation": "动态观察",
            "ConstraintObservation": "约束观察",
            "useCache": "使用缓存",
            "clearCache": "清除缓存",
            "restoreSetting": "恢复默认设置",
            "ShowAxis": "显示坐标轴",
            "LoadOnDemand": "按需加载扩展数据(视图、动画等)",
            "RequestOnDemand": "按流模式加载",
            "OrignalView": "使用默认视图",
            "LoadEdge": "显示边线",
            "MouseWheelOperationFlip": "反转鼠标滚动方向",
            "MergeModel": "使用合批渲染",
            "OnlyBom": "打开时仅加载装配结构",
            "CurrentFPS": "最大渲染帧率",
            "GenetateTexCood": "打开模型时生成纹理坐标",
            "GradingAssembltTree": "分级加载装配树",
            "CheckErrorPoint": "剔除mesh数据中异常顶点",
            "ShadowMapEnabled": "显示阴影",
            "useSSAO": "环境光遮蔽",
            "useGround": "地面",
            "useGroundGrid": "网格",
            "isOpenMirror": "镜像",
            "isOpenShadow": "阴影",
            "groundSize": "地面尺寸",
            "ReferenceFileLoadedOnline": "分散模型在线加载",
            "PrerenderProcessing": "渲染预处理",
            "RenderClassification": "设置渲染分类方式",
            "RenderByInstance": "按实例渲染",
            "RenderNoClassification": "不分类",
            "RenderByMaterial": "按材质渲染",
            "UseWorkerRequestFile": "多线程下载",
            "UseWorkerParse": "多线程加载",
            "ProgressiveRender": "渐进式显示",
            "displayOptimization": "运动状态下实时计算",
            "InstanceRender": "实例化渲染",
            "Configuration": "配置方案",
            "PriorityLoadBigModel": "优先加载大模型",
            "ProgressiveDisplay": "边加载边显示",
            "RecomputeBoundingBox": "是否重新计算模型包围盒",
            "LoadBySettingArea": "按设定区域加载",
            "HTTPCompressionMode": "HTTP传输压缩方式",
            "NoCompressionMode": "不压缩",
            "GZipCompressionMode": "GZIP压缩",
            "ZipCompressionMode": "ZIP压缩",
            "VisualBodyRemoval": "视锥体剔除",
            "MultiInstanceRender": "多实例渲染",
            "BlockOut": "遮挡剔除",
            "PreRenderedDataCache": "预渲染数据缓存",
            "MSAA": "多重采样抗锯齿",
            "MipMap": "mipmap",
            "ModelPredisplay": "模型预显示",
            "PreView": "预显示",
            "DoubleLight": "双面光照",
            "UseCullFace": "背面剔除",
            "IsShowEdgeLine": "显示边界线",
            "IsShowLinePoint": "显示边线顶点",
            "UseVBO": "使用VBO",
            "TechnicalView_DepthThreshold": "插图模式-深度系数",
            "IsRender": "是否渲染",
            "AnnotationData": "批注数据",
            "AnnotationFontFamily": "批注字体",
            "AnnotationFontSize": "批注字体大小",
            "AnnotationTextColor": "批注文本颜色",
            "AnnotationFrameColor": "批注外框颜色",
            "AnnotationFillColor": "批注填充颜色",
            "MeasureTitleType": "标题类型",
            "MeasureTitleTag": "符号",
            "MeasureTitleCh": "中文",
            "MeasureTitleEn": "英文",
            "MeasureTitleMaxFill": "最远距离标题背景颜色",
            "MeasureTitleMinFill": "最近距离标题背景颜色",
            "MeasureTitleVerFill": "标题背景颜色",
            "IsShowMaxDis": "显示最远距离",
            "IsShowMinDis": "显示最近距离",
            "MeasureFontSize": "字体大小",
            "MeasureFontFamily": "字体",
            "MeasureTitleColor": "标题文本颜色",
            "MeasureResultColor": "结果文本颜色",
            "Section": "剖切",
            "ShowCutLines": "显示轮廓线",
            "ClipEdgeColor": "剖面线颜色",
            "ClipPlaneColor": "剖面颜色",
            "ClipCapColor": "盖面颜色",
            "Orignal": "默认",
            "PerformancePriority": "性能优先",
            "DisplayPriority": "显示优先",
            "Import": "导入",
            "Export": "导出",
            "Rotation": "指定旋转",
            "SelectedCenter": "选中居中",
            "InputMaxFPSNumber": "请输入[0,40]区间内的整数",
            "MaxFPSCountRange": "取值区间为[0,40]",
        },
        "Assembly": {
            "Assembly_add": "添加",
            "Assembly_load": "加载",
            "Assembly_unload": "卸载",
            "Assembly_add_file_error": "请选择正确格式的文件！",
            "Assembly_delete": "删除",
            "ConfirmDelete": "确定删除",
            "TopNote_CannotDel": "顶级装配不能删除！",
            "TopAssemblyCannotCopy": "顶级装配不能复制",
            "TopAssemblyCannotCut": "顶级装配不能剪切",
            "CopyCutFirst": "请先复制/剪切!",
            "CelectOtherAssembly": "请选择其他装配",
            "SelectAssembly": "请选择含有子节点的装配!",
            "Assembly_copy": "复制",
            "Assembly_cut": "剪切",
            "Assembly_paste": "粘贴",
            "Assembly_rename": "重命名",
            "Assembly_Wait": "请稍等！",
            "ModelView_Name": "视图",
            "Click_OpenFile": "点击打开模型"
        },
        "ModelView": {
            "ModelView_Add_Success": "视图添加成功"
        },
        "Configure": {
            "Configure_Title": "配置",
            "Configure_Speed": "速度",
            "Configure_Direction": "方向",
            "Configure_Rotation_scheme": "转动方案：",
            "Configure_Rotation_scheme_Explosive": "同时爆炸",
            "Configure_Rotation_scheme_Rotation": "曲线旋转"
        },
        "Animation": {
            "Animation_Loop": "循环播放",
            "Animation_Speed": "播放速度",
            "Animation_Auto_WalkThrough": "自动漫游"
        },
        "WalkThrough": {
            "WalkThrough_Mode": "漫游模式",
            "WalkThrough_Speed": "漫游速度",
            "WalkThrough_Updirection": "向上方向",
            "WalkThrough_Career_perspective": "视野大小",
            "WalkThrough_JoyStick": "虚拟按键"
        },
        "ClearMemory": {
            "Clear_Memory": "默认设置",
            "Clear_File_Memory": "清除文件缓存",
            "Clear_File_Memory_Success": "成功清除文件缓存",
            "Clear_Memory_Success": "成功恢复默认,部分配置项刷新后生效"
        },
        "ImportConfig": {
            "Import_Config_Success": "导入配置成功",
            "Import_Config_Success_Refresh": "请刷新页面",
            "Import_Config_Fail": "配置文件无效"
        },
        "FeedBack": {
            "FeedBack_Title": "用户反馈",
            "FeedBack_QuestionDetail": "问题描述",
            "FeedBack_Function": "相关功能",
            "FeedBack_Contact": "联系方式"
        },
        "waterMark": {
            "InputWaterMarkContent": "请输入水印文字"
        },
        "TextNote": {
            "CreateAnnotation": "创建批注",
            "TextNote_Title": "批注",
            "InputNumber1": "请输入",
            "InputNumber2": "的正整数",
            "SequenceRepeat": "序号重复",
            "InputContent": "请输入内容",
            "InputTextNoteContent": "请输入批注内容",
            "SizeOverrun": `字数超出限制`,
            "NumberAnnotationTextValue": "1",
            "SequenceAnnotationCountRange": "取值区间为[1,9007199254740991]",
            "SequenceAnnotationMaxCount": "序号批注最大值"
        },
        "SHotSpot": {
            "SHotSpot_Title": "热点",
            "Name": "名称",
            "Description": "描述",
            "Type": "热点类型",
            "ChangeView": "切换视图",
            "PlayAnimation": "播放动画",
            "ModelView": "视图",
            "Animation": "动画",
            "AnimationProcess": "动画过程",
            "AnimationStep": "动画步骤"
        },
        "Edit": {
            "TextNote_Edit": "编辑批注",
            "TextNoteMulEdit": "只支持编辑一个批注对象"
        },
        "RightMenu": {
            "RightMenu_Hide": "隐藏",
            "RightMenu_Single_display": "单独显示",
            "RightMenu_Centered_display": "居中",
            "RightMenu_Turn_continue_rotation": "开启持续旋转",
            "RightMenu_Close_continue_rotation": "关闭持续旋转",
            "RightMenu_Set_Backgroud_Color": "设置背景颜色",
            "RightMenu_Turn_JoyStick": "开启虚拟摇杆",
            "RightMenu_Close_JoyStick": "关闭虚拟摇杆",
            "RightMenu_Explicit_exchange": "显隐交换",
            "RightMenu_Show_all": "全部显示",
            "RightMenu_Move": "移动..",
            "RightMenu_Setting": "设置",
            "RightMenu_About": "关于",
            "RightMenu_Licence": "许可信息",
            "RightMenu_SetColor": "设置颜色",
            "RightMenu_ResetSelect": "选择复位",
            "RightMenu_CreateMateral": "创建材质",
            "RightMenu_Version_Record": "版本说明",
            "RightMenu_ShapeSet": "添加到选择集"
        },
        "VersionRecord": {
            "Record_Title": "版本说明",
            "v10_0": "1、优化模型加载和浏览性能;<br />2、添加测量结果预显功能;<br />3、添加选择集功能。",
            "v9_1": "1、添加地面、地面阴影、地面网格、地面镜像功能;<br />2、添加Xray选中效果;<br />3、添加剖切轮廓线及其测量功能;<br />4、优化批注测量显示样式;<br />5、修复遗留缺陷。",
            "v9_0": "1、添加实例化渲染功能;<br />2、添加多线程下载功能;<br />3、添加多线程加载功能;<br />4、添加渐进式显示功能;<br />5、添加一键配置方案;<br />6、添加运动状态下实时计算功能;<br />7、添加边线的按需加载功能;<br />8、添加环境光遮蔽功能;<br/>9、修复遗留缺陷。",
            "v8_2": "1、增强了产品稳定性;<br />2、增加日志输出功能;<br />3、实现子件视图、子件PMI的显示和数据管理功能;<br />4、增加指定剖功能;<br />5、增加突出显示选中效果;<br />6、增加厚度测量功能;<br />7、增加创建圆心功能，以及完善圆心相关测量功能;<br />8、增加无几何属性对象的相关测量功能。",
            "v8_1": "1、增加H5动画音频播放功能;<br />2、增加默认视图功能;<br />3、增加模型看板功能;<br />4、增加分级爆炸功能;<br />5、增加批注、看板等与模型同步高亮显隐功能;<br />6、修复遗留缺陷。",
            "v8_0": "1、增加H5协同会议功能;<br />2、H5性能优化;<br />3、增加仅加载装配结构功能;<br />4、增加按流模式加载功能;<br />5、增加插图模式功能;<br />6、增加按需加载扩展属性功能;<br />7、修复遗留缺陷。",
            "v7_2": "1、添加批注、点点测量随动效果;<br />2、添加场景信息读取及服务器端保存;<br />3、添加模型看板效果读取;<br />4、添加热点场景读取功能;<br />5、优化在线视图功能;<br />6、添加多面剖功能;<br />7、添加分散文件浏览。<br />8、修复已知缺陷。<br />",
            "v7_1": "1、优化界面效果;<br />2、修改测量显示样式及预选效果;<br />3、添加子件PMI显示功能;<br />4、修改漫游效果及添加上下虚拟按键;<br />5、添加在线视图保存;<br />6、改进配置项显示，增加系统和文档的分类;<br />7、修复已知缺陷。<br />",
            "v7_0": "1、优化打开速度及显示性能;<br />2、添加LOD加载;<br />3、添加按需加载;<br />4、添加路径动画;<br />5、添加缓存机制;<br />6、优化界面效果;<br />7、修复已知缺陷。<br />",
            "v6_2": "1、统一产品接口;<br />2、优化模型打开速度;<br />3、添加工具动画;<br />4、优化界面及提示信息;<br />5、修复已知缺陷。<br />",
            "v6_1": "1、修改许可认证方式;<br />2、完善漫游体验;<br />3、添加自定义天空盒显示功能;<br />4、添加子动画的读取播放;<br />5、添加面积和体积属性及接口;<br />6、修复bom加载缓慢缺陷。<br />",
            "v6_0": "1、修复灯光读取缺陷;<br />2、添加动画轨迹线;<br />3、添加热点功能;<br />4、添加中心线角度测量;<br />5、添加设置背景图片接口;<br />6、修复已知缺陷。<br />",
            "v5_0": "1、珠宝模式的添加;<br />2、半透明缺陷修改;<br />3、选择样式的添加;<br />4、修复缺少材质时模型读取出错问题;<br />5、修复某些模型颜色显示错误;<br />6、修改面选择样式，与PC端和移动端保持一致。<br />"
        },
        "RightMenuTitle": {
            "RestoreView_li": "复位",
            "ViewList": "视图",
            "assembly": "装配",
            "mainMenu": "菜单",
            "walking": "漫游模式",
            "FullScreen": "全屏",
            "Apply": "申请",
            "ExitFullScreen": "退出全屏",
            "modelproperties": "模型属性",
            "synergyMeeting": "协同会议",
            "TextMessage": "文本聊天",
            "OtherOperations": "其他操作",
            "AlignmentCamera": "对齐相机"
        },
        "ViewMenuTitle": {
            "solidMode": "实体模式",
            "solidWireMode": "实体线框模式",
            "illustrationMode": "插图模式",
            "transparentMode": "半透明模式",
            "triangularGrid": "三角网格",
            "topView": "俯视图",
            "axisView": "轴视图",
            "leftView": "左视图",
            "frontView": "正视图",
            "rightView": "右视图",
            "behindView": "后视图",
            "bottomView": "仰视图",
            "perspective": "透视投影",
            "walkThrough": "漫游模式",
            "setting": "设置",
        },
        "BottomMenuTitle": {
            "_note": "批注",
            "_scaleL": "手绘",
            "animation": "动画",
            "measure": "测量",
            "li_explosive": "爆炸",
            "lipq": "剖切",
            "snapshot": "快照",
            "clear": "清空"
        },
        "SectionMenuTitle": {
            "section_bar_main": "剖切方向",
            "section_yzsection": "yz面剖切",
            "section_xysection": "xy面剖切",
            "section_xzsection": "zx面剖切",
            "section_reversal": "剖切反向",
            "section_cappingPlane": "盖面显隐",
            "section_clipEdge": "剖面轮廓线",
            "section_plane": "剖切面显隐",
            "section_addselected": "添加剖切对象",
            "section_clearselected": "清空剖切对象",
            "section_selectedSet": "剖切对象",
        },
        "ExplosiveMenuTitle": {
            "section_bar_main": "爆炸方向",
            "explosive_center": "中心爆炸",
            "explosive_by_x": "x轴线性爆炸",
            "explosive_by_y": "y轴线性爆炸",
            "level_none": "无",
            "exit": "退出",
            "confirm": "确定",
            "cancel": "取消",
            "sure_exit": "确认退出吗？",
            "hint": "提示"
        },
        "NoteMenuTitle": {
            "node_text": "文本批注",
            "note_order": "序号",
            "note_component": "模型名称",
            "note_shotspot": "热点",
            "node_edit": "编辑",
            "node_del": "删除"
        },
        "GestureMenuTitle": {
            "gesture_bar_main": "当前类型",
            "freedom": "线",
            "circle": "圆",
            "rect": "矩形",
            "triangle": "三角形",
            "gesture_bar_second_main": "颜色",
            "gesture_black": "黑色",
            "gesture_red": "红色",
            "gesture_blue": "蓝色",
            "gesture_green": "绿色",
            "gesture_yellow": "黄色",
            "gesture_orange": "橙色",
            "gesture_gray": "灰色",
            "gesture_purple": "紫色",
            "gesture_brown": "棕色",
            "gesture_pink": "粉色",
            "gesture_white": "白色",
            "gesture_bar_third_main": "粗细",
            "gesture_thin_line": "细",
            "gesture_middle_line": "中",
            "gesture_strong_line": "粗",
            "gesture_undo": "撤销"
        },
        "DraggerMenuTitle": {
            "dragger_main": "移动模式",
            "trans": "平移",
            "rot": "旋转",
            "sca": "缩放",
            "dragger_face": "沿平面",
            "yzsection": "沿X轴",
            "zxsection": "沿Y轴",
            "xysection": "沿Z轴",
            "Cyzsection": "绕X轴",
            "Czxsection": "绕Y轴",
            "Cxysection": "绕Z轴",
            "dragger_Centertranslation": "绕中心",
            "dragger_planetranslation": "沿整体",
            "exit": "退出"
        },
        "ClearMenuTitle": {
            "ClearAnnotation": "清空批注对象",
            "ClearMeasure": "清空测量对象",
            "ClearSection": "清空剖切面",
            "ClearAnnotationPrompt": "批注对象删除后,将不可恢复,确定要删除所有批注对象吗?",
            "ClearMeasurePrompt": "测量对象删除后,将不可恢复,确定要删除所有测量对象吗?",
            "ClearSectionPrompt": "剖切面删除后,将不可恢复,确定要删除所有剖切面吗?"
        },
        "AnimationMenuTitle": {
            "start": "开始",
            "pause": "暂停",
            "forward": "前进",
            "backward": "后退",
            "more": "更多",
            "": "更多",
            "exit": "退出"
        },
        "MeasureMenuTitle": {
            "measure_Type_distance": "距离测量",
            "measure_Type_Angle": "角度测量",
            "measure_Type_Circle": "圆相关测量",
            "measure_Type_Property": "属性测量",
            "measure_Distance_PntToPnt": "点点测量",
            "measure_Distance_PntToLine": "点线测量",
            "measure_Distance_PntToFace": "点面测量",
            "measure_Distance_LineToLine": "线线测量",
            "measure_Distance_LineToFace": "线面测量",
            "measure_Distance_FaceToFace": "面面测量",
            "measure_Distance_Thickness": "厚度",
            "measure_Angle_LineToLine": "线线角度",
            "measure_Angle_LineToFace": "线面角度",
            "measure_Angle_FaceToFace": "面面角度",
            "measure_Circle_Center_Arc": "弧线创建圆心",
            "measure_Circle_Center_Point": "三点创建圆心",
            "measure_Diameter": "直径",
            "measure_Radius": "半径",
            "measure_Distance_ShaftToShaft": "轴距",
            "measure_Distance_CenterToCenter": "圆心距",
            "measure_Property_Point": "点",
            "measure_Property_Line": "线",
            "measure_Property_Face": "面",
            "measure_Property_Model": "模型",
            "measure_Box_Note": "包围盒尺寸",
            "measure_del": "删除",
            "backward": "后退"
        },
        "ViewList": {
            "showEdgeLine": "实体线框模式",
            "notshowEdgeLine": "实体模式",
            "onlyshowEdgeLine": "线框模式",
            "IsTechnicalView": "插图模式",
            "isNoteVisibleWithModel": "与模型同步显隐(批注、看板等)",
            "isNoteHighLightWithModel": "与模型同步高亮(批注、看板等)",
            "IsShowTransparent": "半透明模式",
            "top": "俯视图",
            "isometric": "轴视图",
            "left": "左视图",
            "front": "正视图",
            "right": "右视图",
            "back": "后视图",
            "bottom": "仰视图",
            "perspective": "透视投影",
            "showTrilateralEdge": "显示网格",
            "mulSelect": "多选",
            "showBox": "显示包围盒",
            "observeMode": "观察模式",
            "FreeObserve": "自由观察",
            "OribitObserve": "动态观察",
            "ConstraintObserve": "约束观察"
        },
        "Prompt": {
            "isOpening": "正在打开...",
            "Prompt": "提示",
            "Downloading": "正在下载...",
            "WatingLoading": "正在加载...",
            "PleaseWating": "请稍候...",
            "FileNoExist": "文件不存在",
            "FileFormatError": "svlx文件格式错误",
            "PleaseOpenFile": "请先打开模型",
            "WalkThroughRestore": "复位功能会对漫游效果产生影响,不能同时使用",
            "PleaseSelectOneAssembly": "请先选择一个装配",
            "InputNumber": "请输入正整数",
            "TextNoteMulEdit": "暂不支持多个对象的同时编辑",
            "AnnotationNotEmoji": "不支持表情符号",
            "AnnotationDonotEdit": "当前批注对象无法进行编辑",
            "PleaseSelectOneAnnotation": "请先选择一个批注对象",
            "PleaseSelectOneMeasure": "请先选择一个测量对象",
            "Edit": "点击引线移动批注位置，点击文本框修改批注文本内容",
            "DeviceDoesNotThisFunction": "当前设备不支持此功能",
            "CreateSuccess": "创建成功",
            "CreateError": "创建失败",
            "AxisIntersect": "弧所在轴线相交",
            "ParallelOrNoteSamePlane": "选择对象平行或不在同一平面",
            "SurfaceCannotMeasure": "仅支持平面测量",
            "CurveCannotMeasure": "无法进行弧线测量",
            "NotFindCurveCenter": "未找到弧线圆心",
            "PointsAreOnSmaeLine": "点在同一直线上，请重新选择",
            "SameShapeCannotMeasure": "同一对象无法测量",
            "NotGeoAttribute": "测量对象无几何属性",
            "ShapeIntersect": "选择对象相交",
            "ModelNameError": "名称只能包含汉字、字母、数字、下划线和点",
            "DuplicateViewName": "已存在相同名称的视图，请重新命名",
            "PleaseSelectAnnotationPoint": "请选择批注位置",
            "PleaseSelectAnnotationPosition": "请选择批注显示位置",
            "PleaseSelectMeasurePosition": "请选择测量显示位置",
            "PleaseSelectFirstMeasurePoint": "请选择第一个测量点",
            "PleaseSelectSecondMeasurePoint": "请选择第二个测量点",
            "PleaseSelectFirstMeasureLine": "请选择第一条测量线",
            "PleaseSelectSecondMeasureLine": "请选择第二条测量线",
            "PleaseSelectFirstMeasureFace": "请选择第一个测量面",
            "PleaseSelectSecondMeasureFace": "请选择第二个测量面",
            "PleaseSelectFirstPoint": "请选择第一个点",
            "PleaseSelectSecondPoint": "请选择第二个点",
            "PleaseSelectThirdPoint": "请选择第三个点",
            "PleaseSelectFirstStraightLine": "请选择第一条直线",
            "PleaseSelectFirstLine": "请选择第一条线",
            "PleaseSelectSecondStraightLine": "请选择第二条直线",
            "PleaseSelectSecondLine": "请选择第二条线",
            "PleaseSelectFirstPlane": "请选择第一个平面",
            "PleaseSelectFirstFace": "请选择第一个面",
            "PleaseSelectSecondPlane": "请选择第二个平面",
            "PleaseSelectSecondFace": "请选择第二个面",
            "PleaseSelectFirstCurve": "请选择第一个弧",
            "PleaseSelectSecondCurve": "请选择第二个弧",
            "PleaseSelectOnePoint": "请选择点",
            "ClickAnyWhere": "点击任意位置查看结果",
            "PleaseSelectOneLine": "请选择一条直线",
            "PleaseSelectOneStrLine": "请选择线",
            "PleaseSelectOneMeasureLine": "请选择一条测量线",
            "PleaseSelectOneMeasureFace": "请选择一个测量面",
            "PleaseSelectOnePlane": "请选择一个平面",
            "PleaseSelectOneFace": "请选择面",
            "PleaseSelectFace": "请选择面",
            "PleaseSelectOneCurve": "请选择弧",
            "PleaseSelectOneModel": "请选择模型",
            "NameNotNull": "名称不能为空",
            "PleaseSelectDeleteView": "请选择要删除的视图",
            "DeleteSuccess": "删除成功",
            "ProhibitDeletion": "禁止删除",
            "ProhibitEdit": "禁止编辑",
            "DeleteModelViewSuccess": "删除在线视图成功",
            "DeleteError": "删除失败",
            "RemoveSuccess": "移除成功",
            "RemoveError": "移除失败",
            "AddSuccess": "添加成功",
            "AddError": "添加失败",
            "ViewCannotDelete": "该视图不允许删除",
            "PleaseSelectAssembly": "请先选择装配件",
            "AssemblyCanAddModel": "装配件才能添加模型",
            "PleaseSelectModel": "请先选择模型",
            "CannotAddPeerAssembly": "不能添加同级装配件",
            "SelectBin": "请同时选择JSvl相应的bin文件",
            "SuccessfulOperation": "操作成功",
            "OperationFailed": "操作失败",
            "DocSettingSaveFailed": "文档配置保存失败",
            "TopAssemblyCannotCopy": "顶级装配件不能复制",
            "TopAssemblyCannotCut": "顶级装配件不能剪切",
            "AssemblyOrModelCannotNull": "装配件或模型不得为空",
            "PeerAssemblyCannotPaste": "同级装配件不能粘贴",
            "ClipboardIsEmpty": "剪贴板为空,无法粘贴",
            "CircularReference": "存在循环引用,无法粘贴",
            "TopAssemblyCannotDelete": "顶级装配件不能删除",
            "NetworkError": "网络异常",
            "DeleteSelectedView": "是否要删除本视图?",
            "DeleteCache": "缓存数据清除后将不可恢复,是否删除?",
            "RestoreDefaultSetting": "恢复默认设置后,将还原所有配置,是否恢复?",
            "SureDelete": "是否要删除",
            "ResetName": "重命名",
            "ViewName": "视图名称",
            "DeleteSelectedAnnotation": "是否删除选中的批注?",
            "DeleteSelectedMeasure": "是否删除选中的测量?",
            "DeleteSelectedObject": "确定删除选中对象?",
            "SureFreeColor": "确定给装配重新配置颜色吗?",
            "ExitCurrentOperation": "退出当前操作前，请确认数据是否已保存到视图中?",
            "PlayCameraPreTip": "播放动画前，请确认数据是否已保存到视图中?",
            "SelectThicknessDriection": "测量方向",
            "VerticalSurface": "垂直于表面",
            "VerticalScreen": "垂直于屏幕",
            "SaveCurrentAnnotation": "是否保留当前批注?",
            "NeedOpenWalkThrough": "需要开启漫游模式!",
            "NeedReload": "请刷新页面",
            "OpenMergeFace": "需要刷新页面后生效",
            "CloseMergeFace": "需要刷新页面后生效",
            "MeasureMergeFace": "请关闭合并面并刷新页面",
            "SHotPotNameRepeat": "热点名称已被使用",
            "SHotPotNotName": "请输入热点名称",
            "SecretKeyError": "密钥文件请求错误",
            "FileSecret": "该文件为加密文件，请配置密钥信息",
            "OpenError": "打开失败",
            "Annotation": "批注",
            "Confim": "确定",
            "Cancle": "取消",
            "Delete": "删除",
            "Restore": "恢复",
            "Exit": "退出",
            "Yes": "是",
            "No": "否",
            "HasApplyOperateAuth": "已向主持人申请操作权限",
            "HasApplyAudioAndVideoAuth": "已向主持人申请音视频权限",
            "QuitGroup": "是否要退出会议?",
            "ApplyAuth": "是否向主持人申请操作权限?",
            "ApplyAudioAndVideo": "是否向主持人申请音视频权限?",
            "DefaultView": "默认视图",
            "ApplySetDefaultView": "是否设置为默认视图",
            "ApplySetUndefaultView": "是否取消默认视图",
            "PlaeaseOpenServerView": "请开启在线视图功能",
            "LocalViewDonotSet": "本地视图无法设置为默认视图",
            "SearchOver": "查询结束",
            "WalkThrough_Move": "请关闭漫游模式后再进行模型移动操作",
            "PleaseSelectShapeSet": "请选择或创建选择集",
            "PleaseSelectModelOnly": "暂不支持更多类型的数据添加到选择集的功能",
            "PleaseSelectModelOnlySection": "暂不支持更多类型的数据添加到剖切对象的功能",
            "DeleteShapeSet": "是否删除该选择集",
            "RemoveFromShapeSet": "是否从选择集中删除",
            "DeleteSectionShape": "是否删除选中模型",
            "ClearSectionShapes": "是否清空指定剖切模型?",
            "ClearSuccess": "已全部清空",
            "ClearError": "清空失败",
            "NoAnimationFile": "无动画文件",
            "SaveSuccess": "保存成功",
            "SaveError": "保存失败",
        },
        "OpenErorr": {
            "SelectBin": "请同时选择JSvl相应的bin文件！"
        },
        "Material": {
            "CreateMaterial": "创建材质",
            "MaterialType": "材质类型",
            "MaterialName": "名称",
            "DifusseColor": "漫反射光",
            "SpecularColor": "镜面反射光",
            "Shininess": "光亮度",
            "Transparency": "透明度",
            "AmbientColor": "环境光",
            "EmissiveColor": "辐射光",
            "UvRotate": "纹理坐标旋转度",
            "UvScale": "纹理坐标缩放",
            "UvTranslate": "纹理坐标翻转",
            "DifusseTexture": "漫反射纹理",
            "SpecularTexture": "镜面反射纹理",
            "EmissiveTexture": "辐射纹理",
            "NormalTexture": "常规纹理",
            "NormalMapScale": "常规纹理缩放",
            "MetalnessFactor": "金属光泽",
            "RoughnessFactor": "粗糙度",
            "UseClearCoat": "是否清除覆盖",
            "ClearCoat": "清除覆盖",
            "ClearCoatRoughness": "清除粗糙覆盖",
            "MetalnessRoughnessTexture": "粗糙金属纹理",
            "MatcapTexture": "材质捕获纹理",
            "Image": "图片"
        },
        "License": {
            "Licence_Error": "提示",
            "ErrorCode": "错误码:",
            "GetLicenseFailed": "许可错误:获取许可信息失败",
            "LicenseInfoError": "许可错误:许可信息错误",
            "ServerRequestFailed": "许可错误:服务器请求失败",
            "LicenseTypeFailed": "许可错误:许可类型错误",
            "LicenseCheckFailed": "许可错误:许可认证失败",
            "UnavailablePoints": "许可错误:无可用点数",
            "LicenseNotStarted": "许可错误:许可时间未开始",
            "LicenseExpired": "许可错误:许可已过期",
            "NoUseAccess": "许可错误:无使用权限",
            "BrowserMismatch": "许可错误:浏览器不匹配",
            "UserMismatch": "许可错误:用户不匹配",
            "LisenseDisabled": "许可错误:许可已禁止使用",
            "DeniedAccess": "许可错误:您已被禁止访问",
            "ProductNameError": "许可错误:产品名称不一致",
            "ProductVersionError": "许可错误:产品版本不一致"
        },
        "Property": {
            "ID": "ID",
            "Property": "属性",
            "Geometric": "几何",
            "Features": "造型",
            "name": "名称",
            "Color": "颜色",
            "SubmodelsCount": "子模型数量",
            "InstanceCount": "实例数量",
            "InstanceID": "实例ID",
            "ModleViewCount": "视图数量",
            "PMICount": "PMI数量",
            "FaceCount": "面数量",
            "LOD0PatchCount": "LOD0面片数量",
            "Volume": "体积",
            "Area": "面积",
            "Point": "点属性",
            "Line": "线长度",
            "CircleCenter": "圆心",
            "CircleRadius": "半径",
            "CircleArcLength": "弧长"
        },
        "ShapeSet": {
            "ShapeSet": "选择集",
            "SelectedShapeSet": "现有选择集",
            "Add": "添加",
            "Delete": "删除",
            "InputShapeSetName": "请输入选择集名称",
            "CreateShapeSet": "创建选择集",
            "Search": "搜索",
        },
        "SweetAlert": {
            "Confirm": "确定",
            "Cancel": "取消",
            "EnterViewName": "请输入视图名",
            "SaveError": "保存失败",
            "Rename": "重命名",
            "Remove": "删除",
            "ViewCreating": "视图创建中，请稍后...",
            "ViewRename": "视图名称",
            "DefaultView": "默认视图",
            "InputViewName": "请输入视图名称",
            "ParameterError": "参数错误",
            "NoParameterError": "参数不得为空",
            "WithoutPermissionEdit": "无权限编辑他人视图",
            "ViewRenameError": "视图重命名失败",
            "SetDefaultViewError": "设置默认视图失败，请联系管理员",
            "ViewRenameSuccess": "视图重命名成功",
            "ViewCreateError": "视图创建失败",
            "ConfimRemoveView": "确定删除视图",
            "What": "?",
            "WithoutPermissionRemove": "无权限删除他人视图",
            "TimAuthorizationFailure": "授权失败",
            "TimOperateFailure": "操作失败",
            "TimListGroupFailure": "获取会议列表失败",
            "TimLoginFailure": "会议初始化失败",
            "TimNoNickError": "昵称不能为空",
            "TimNoGroupIDError": "会议号不能为空",
            "TimCreateGroupFailure": "创建会议失败",
            "TimSelectGroupToJoin": "请选择需要加入的会议",
            "TimJoinGroupFailure": "加入会议失败",
            "TimDismissGroupFailure": "解散会议失败",
            "TimQuitGroupFailure": "退出会议失败",
            "TimCopyGroupInfo": "会议信息成功复制到剪贴板",
            "MsgSizeOverrun": "字数超出限制,请分条发送",
            "SizeOverrun": "字数超出限制"
        },
        "SynergyGroup": {
            "GroupSetting": "会议设置",
            "NickName": "昵称",
            "NickNamePlaceHolder": "请输入昵称",
            "Save": "保存",
            "Reset": "重置",
            "CreateGroup": "创建会议",
            "Theme": "主题",
            "ThemePlaceHolder": "请输入主题",
            "Join": "加入会议",
            "Cancel": "取消",
            "GroupList": "会议列表",
            "GroupID": "会议号",
            "GroupIDPlaceHolder": "请输入会议号",
            "Host": "主持人",
            "CreateTime": "创建时间",
            "ExitGroup": "退出会议",
            "Invite": "邀请参会",
            "Attendees": "参会人",
            "Attachment": "会议附件",
            "SelectFile": "选择文件",
            "Info": "会议信息",
            "MuteAllMembers": "全员禁言",
            "UnmuteAllMembers": "解除禁言",
            "BanAllMembers": "禁止全员操作",
            "AllowAllMembers": "允许全员操作",
            "ApplyMembers": "申请人员",
            "OpenAudio": "开启音频",
            "CloseAudio": "关闭音频",
            "OpenVideo": "开启视频",
            "CloseVideo": "关闭视频",
            "AllowOperate": "允许操作",
            "BanOperate": "禁止操作",
            "NoSendBlank": "不能发送空白信息",
            "ChatContentPlaceHolder": "请输入聊天内容",
            "Send": "发送",
            "Agree": "同意",
            "Ageeed": "已同意",
            "WhoseMeeting": "的会议",
            "DoesnotSupportAudioAndVideo": "当前浏览器不支持音视频功能,建议使用最新版本的 Chrome 浏览器",
            "DeviceIsUnavailable": "摄像头/麦克风设备无法使用",
            "DeviceIsUnavailableAccessed": "暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试",
            "UnauthorizedDeviceAccedd": "未授权摄像头/麦克风访问,无法进行音视频通话",
            "NoDeviceDetected": "未检测到摄像头/麦克风设备",
            "Initialize": "初始化...",
            "MeetingWasUnexist": "会议不存在或已过期",
            "HostEndTheMeeting": "主持人结束会议",
            "ApplyOperation": "申请操作权限",
            "ApplyAudioAndVideoOperation": "申请音视频权限",
            "CloseAudioAndVideoOperation": "关闭音视频权限",
            "OpenAudioAndVideoOperation": "开启音视频权限",
            "EnabledYourAudioAndVideoOperation": "主持人已开启您的音视频权限",
            "ClosedYourAudioAndVideoOperation": "主持人已关闭您的音视频权限",
            "ServerIsConnected": "服务器连接成功",
            "ConnectedToTheNetwork": "已接入网络",
            "TheCurrentNetworkIsUnstable": "当前网络不稳定",
            "TheCurrentNetworkIsUnavailable": "当前网络不可用",
            "EnabledYourOperationPermissions": "主持人已开启您的操作权限",
            "ClosedYourOperationPermissions": "主持人已关闭您的操作权限",
            "CreationFailed": "创建失败",
            "WaitingForTheAdministratorToAgree": "等待管理员同意",
            "AlreadyInTheGroup": "已经在群中",
            "AddGroupSuccess": "加群成功",
            "Me": "我",
            "I": "(我)",
            "GroupOwner": "(群主)",
            "PleaseWait": "请稍后...",
            "NoPassword": "密码:无",
            "UnableToGetAudioAndVideoData": "无法获取音视频数据,请检查音视频设备"
        },
        "MeetingDirection": {
            "OpenFile": "打开文件",
            "SelectShape": "选择模型",
            "ShowModel": "显示模型",
            "HideModel": "隐藏模型",
            "SetModelColor": "设置模型颜色",
            "RestoreModel": "复位模型",
            "ShowHideExchange": "显隐切换",
            "UnSelectShape": "取消选择模型",
            "ClearAllSelect": "清空选择模型",
            "MoveModel": "移动模型",
            "RestoreView": "复位",
            "SetView": "切换视图",
            "CreateView": "创建视图",
            "DeleteView": "删除视图",
            "ChangeModelView": "切换视图",
            "ShowWalkthrough": "开启漫游",
            "CloseWalkthrough": "关闭漫游",
            "SettingWalkThrough": "更新漫游状态",
            "SettingWalkthroughParameter": "设置漫游参数",
            "SetRederMode": "渲染模式设置",
            "Play": "播放",
            "CloseAnimation": "关闭动画",
            "PauseAnimation": "暂停动画",
            "SetUseScene": "设置是否使用场景",
            "SetLoop": "设置是否循环播放",
            "SetPlaySpeed": "设置播放速度",
            "SetPlayPercent": "设置动画播放百分比",
            "AddGestureNote": "添加手势批注",
            "AddTextNote": "添加文本批注",
            "UpdateTextNote": "更新文本批注",
            "DeleteTextNote": "删除文本批注",
            "SelectAnnotation": "选择批注",
            "UnSelectAnnotation": "取消选择批注",
            "SelectMeasure": "选择测量",
            "UnselectMeasure": "取消选择测量",
            "AddSequenceNote": "添加序号批注",
            "AddMeasure": "添加测量",
            "DeleteMeasure": "删除测量",
            "DeleteGesture": "删除手势批注",
            "UpdateMeasurePosition": "更新测量位置",
            "ClearAnnotation": "清空批注对象",
            "ClearMeasure": "清空测量对象",
            "ShowExplosion": "显示爆炸",
            "ExitExplosion": "退出爆炸",
            "CloseSection": "清空剖切",
            "Section": "剖切"
        },
        "LogList": {
            "Log": "日志",
            "IsOpen": "启用日志记录",
            "LogPath": "输出路径",
            "LogLevel": "输出等级",
            "Clear": "清除",
            "AllClear": "一键清除",
            "Error": "Error",
            "Warning": "Warning",
            "Info": "Info",
            "Debug": "Debug"
        },
        "": {}
    };
    SView.language_cn = language_cn;
})(SView || (SView = {}));
var SView;
(function (SView) {
    class language_en {
    }
    language_en.languageObj = {
        "common": {
            "close": "Close",
            "subMit": "Submit",
            "confirm": "OK",
            "exit": "Exit"
        },
        "warning": {
            "browserNotSupportCanvas": "Your browser does not support the canvas tag",
            "browserNotSupportCanvasChangeBrowser": "Your browser does not support HTML5 canvas. Please change your browser."
        },
        "about": {
            "about_title": "About",
            "about_licence_title": "License",
            "about_license_endtime": "No Limit",
            "about_license_modules": "Modules:",
            "about_license_creator": "Creator:",
            "about_license_createtime": "Create Time:",
            "about_company": "HOTEAMSOFT",
            "about_license_expire": "&nbspExpire",
            "about_html_version": "HTML Version:",
            "about_m3d_version": "M3D Version:"
        },
        "Alert": {
            "Alert_Prompt": "Prompt",
            "Alert_Context": "Need to open roaming mode!",
            "Alert_Reload": "Please reload the page",
            "Alert_MergeFace": "Note that using this measurement feature requires that the merge face configuration item be closed and the page refreshed to take effect",
            "Alert_OpenMergeFace": "Starting this configuration item takes effect after the page is refreshed, and some measurement functions are unavailable",
            "Alert_CloseMergeFace": "Closing this configuration item causes performance degradation and requires a page refresh to take effect",
            "Alert_MeasureError": "Invalid measurement object",
            "Alert_SHotPotNameRepeat": "The HotSpot name has been used",
            "ALert_SHotPotNotName": "Please enter the HotSpot name"
        },
        "setting": {
            "Conventional": "Conventional",
            "Show": "Show",
            "Performance": "Performance",
            "Render": "Render",
            "Function": "Function",
            "Load": "Load",
            "Background": "Background",
            "Other": "Other",
            "Help": "Help",
            "License": "Lincense",
            "About": "About",
            "Measure": "Measure",
            "Annotation": "Annotation",
            "Debug": "Debug",
            "ShowDebugMenu": "Show Debug Menu",
            "General": "General",
            "Axis": "Axis",
            "DomSetting": "Doc",
            "SysSetting": "System",
            "WalkThrough": "WalkThrough",
            "Speed": "Speed",
            "WalkThrough_Career_perspective": "View size",
            "WalkThrough_JoyStick": "JoyStick",
            "WalkThrough_MouseOperation": "Mouse operation",
            "VersionRecords": "Version Records",
            "BGColorDialog_title": "Set Backgroud Color",
            "BGWaterMark_title": "Set Background WaterMark Text",
            "Lable_User_Name": "UserName",
            "Label_Password": "Password",
            "Label_Login_button": "Login",
            "Tip_User_Name": "Mobile phone number or e-mail address",
            "setting": "Setting",
            "continue_rotation": "Continuous rotation",
            "show_fps": "Show FPS",
            "merge_face": "Merge face",
            "show_shotspot": "Show HotSpot",
            "show_annotation": "Show Annotation",
            "show_rotation_center": "Show Rotation Cneter",
            "default_show_mode": "Default Display Mode",
            "show_shaft_side": "Show Axial Profile",
            "show_model_view": "Show Default ModelView",
            "show_model_properties": "Show Model Properties",
            "play_animation": "Play Animation",
            "WaterMark": "WaterMark",
            "show_pmi": "Show PMI",
            "show_submodelpmi": "Show Components PMI",
            "show_submodelview": "Show Components View",
            "Catia_mode": "Catia Mode",
            "High_performance_mode": "High performance mode",
            "Use_LOD": "Use LOD",
            "LOD_Selection": "LOD Choose Way",
            "Auto_Calculation": "Automatic Calculation",
            "Fixed_Level": "Fixed Level",
            "Not_More_Than_Level": "Not More Than Level",
            "LOD_Level": "LOD Level",
            "Small_strip_pattem": "Small strip rejection",
            "Small_strip_pattem_Model": "Model",
            "Small_strip_pattem_Screen": "Screen",
            "small_strip_size": "Small strip Percent",
            "Unit_of_measure": "Unit",
            "Unit_of_none": "None",
            "Unit_of_mm": "MM",
            "Unit_of_cm": "CM",
            "Unit_of_m": "M",
            "Unit_of_Inch": "Inch",
            "Unit_of_foot": "Foot",
            "language_Type": "Language",
            "language_cn": "简体中文",
            "language_en": "English",
            "selectType": "Selected effect",
            "selectedColor": "Selected Color",
            "selectType_side": "Outline",
            "selectType_color": "Color",
            "selectType_high": "Highlight",
            "selectType_highlight": "Prominent Show",
            "selectType_XRay": "Xray",
            "showTrilateralEdge": "Show Grid",
            "mulSelect": "MultiSelect",
            "useModelBackground": "Use Model Background",
            "showBox": "Show Box",
            "showBoxNote": "Show Bounding Box Note",
            "Show_PMI": "Show PMI",
            "edgeSelector": "Edge style",
            "DefaultUpward": "Default Upward Direction",
            "DefaultObservation": "Default Observation Mode",
            "FreeObservation": "Free Observation",
            "OribitObservation": "Dynamic Observation",
            "ConstraintObservation": "Constraint Observation",
            "useCache": "Use Cache",
            "clearCache": "Clear Cache",
            "restoreSetting": "Restore",
            "ShowAxis": "Show coordinate axis",
            "LoadOnDemand": "On demand loading",
            "RequestOnDemand": "Load in stream mode",
            "OrignalView": "Use the default view",
            "LoadEdge": "Show Edge",
            "MouseWheelOperationFlip": "Reverse Mouse Scrolling",
            "MergeModel": "Use Merge Model Rend",
            "OnlyBom": "Only load Bom",
            "CurrentFPS": "Maximum FPS",
            "GenetateTexCood": "Texture coordinates are generated when the model is opened",
            "GradingAssembltTree": "Step loading assembly tree",
            "CheckErrorPoint": "Abnormal vertices in mesh data are eliminated",
            "ShadowMapEnabled": "Show Shadow",
            "useSSAO": "Use SSAO",
            "useGround": "Ground",
            "useGroundGrid": "Ground Grid",
            "isOpenMirror": "Ground Mirror",
            "isOpenShadow": "Ground Shadow",
            "groundSize": "Ground Size",
            "ReferenceFileLoadedOnline": "Reference file loaded online",
            "PrerenderProcessing": "Prerender",
            "RenderClassification": "RenderClassification",
            "RenderByInstance": "Render by instance",
            "RenderNoClassification": "RenderNoClassification",
            "RenderByMaterial": "Render by material",
            "UseWorkerRequestFile": "Multithreaded download",
            "UseWorkerParse": "Multithreaded parsing",
            "ProgressiveRender": "Progressive Render",
            "InstanceRender": "Instance Render",
            "Configuration": "Configuration",
            "displayOptimization": "Real-time calculation in motion state",
            "PriorityLoadBigModel": "Loading large models first",
            "ProgressiveDisplay": "Display while loading",
            "RecomputeBoundingBox": "Recompute BoundingBox",
            "LoadBySettingArea": "Load by area which setted",
            "HTTPCompressionMode": "HTTPCompressionMode",
            "NoCompressionMode": "NoCompression",
            "GZipCompressionMode": "GZipCompression",
            "ZipCompressionMode": "ZipCompression",
            "VisualBodyRemoval": "VisualBodyRemoval",
            "MultiInstanceRender": "MultiInstanceRender",
            "BlockOut": "BlockOut",
            "PreRenderedDataCache": "PreRendered data cache",
            "MSAA": "MSAA",
            "MipMap": "mipmap",
            "ModelPredisplay": "Model predisplay",
            "PreView": "PreView",
            "DoubleLight": "Double light",
            "UseCullFace": "Use Cull Face",
            "IsShowEdgeLine": "Show edge line",
            "IsShowLinePoint": "Show edge line point",
            "UseVBO": "UseVBO",
            "TechnicalView_DepthThreshold": "echnicalView_DepthThreshold",
            "IsRender": "IsRender",
            "AnnotationData": "Annotation Data",
            "AnnotationFontFamily": "Font Family",
            "AnnotationFontSize": "Font Size",
            "AnnotationTextColor": "Text Color",
            "AnnotationFrameColor": "Frame Color",
            "AnnotationFillColor": "Fill Color",
            "MeasureTitleType": "Title Type",
            "MeasureTitleTag": "Symbol",
            "MeasureTitleCh": "中文",
            "MeasureTitleEn": "English",
            "MeasureTitleMaxFill": "Max Distance Title Fill Color",
            "MeasureTitleMinFill": "Min Distance Title Fill Color",
            "MeasureTitleVerFill": "Title Fill Color",
            "IsShowMaxDis": "Show Max Distance",
            "IsShowMinDis": "Show Min Distance",
            "MeasureFontSize": "Font Size",
            "MeasureFontFamily": "Font Family",
            "MeasureTitleColor": "Title Text Color",
            "MeasureResultColor": "Result Text Color",
            "Section": "Section",
            "ShowCutLines": "Show Cut Lines",
            "ClipEdgeColor": "Section Edge Color",
            "ClipPlaneColor": "Clip Plane Color",
            "ClipCapColor": "Clip Cap Color",
            "Orignal": "Orignal",
            "PerformancePriority": "Performance priority",
            "DisplayPriority": "Display priority",
            "Import": "Import",
            "Export": "Export",
            "Rotation": "Rotation",
            "SelectedCenter": "Selected Center",
            "InputMaxFPSNumber": "Please enter an integer in the range [0,40]",
            "MaxFPSCountRange": "The value range is [0,40]",
        },
        "Assembly": {
            "Assembly_add": "Add",
            "Assembly_load": "Load",
            "Assembly_unload": "Unload",
            "Assembly_add_file_error": "Please select the file in the correct format",
            "Assembly_delete": "Delete",
            "ConfirmDelete": "Confirm Delete",
            "TopNote_CannotDel": "Top assembly can't delete",
            "TopAssemblyCannotCopy": "Top assembly can't copy",
            "TopAssemblyCannotCut": "Top assembly can't cut",
            "CopyCutFirst": "Please copy or cut first",
            "CelectOtherAssembly": "Please select other assembly",
            "SelectAssembly": "Please select the assembly that contains the child nodes",
            "Assembly_copy": "Copy",
            "Assembly_cut": "Cut",
            "Assembly_paste": "Paste",
            "Assembly_rename": "Rename",
            "Assembly_Wait": "Please wait a moment!",
            "ModelView_Name": "ModelView",
            "Click_OpenFile": "Click Open Model"
        },
        "ModelView": {
            "ModelView_Add_Success": "ModelView added successfully"
        },
        "Configure": {
            "Configure_Title": "Configure",
            "Configure_Speed": "Speed",
            "Configure_Direction": "Direction",
            "Configure_Rotation_scheme": "Rotation scheme：",
            "Configure_Rotation_scheme_Explosive": "Simultaneous explosion",
            "Configure_Rotation_scheme_Rotation": "Curve rotation"
        },
        "Animation": {
            "Animation_Loop": "Loop Playback",
            "Animation_Speed": "Speed",
            "Animation_Auto_WalkThrough": "Auto roaming"
        },
        "WalkThrough": {
            "WalkThrough_Mode": "Roaming Mode",
            "WalkThrough_Speed": "Speed",
            "WalkThrough_Updirection": "Direction",
            "WalkThrough_Career_perspective": "View size",
            "WalkThrough_JoyStick": "JoyStick"
        },
        "ClearMemory": {
            "Clear_Memory": "Clear default",
            "Clear_File_Memory": "Clear file cache",
            "Clear_File_Memory_Success": "Clear file cache successfully",
            "Clear_Memory_Success": "Clear default successfully,Some configuration need to be refreshed to take effect"
        },
        "ImportConfig": {
            "Import_Config_Success": "Successfully imported configuration",
            "Import_Config_Success_Refresh": "Please refresh the page",
            "Import_Config_Fail": "The configuration file is invalid"
        },
        "FeedBack": {
            "FeedBack_Title": "FeedBack",
            "FeedBack_QuestionDetail": "Problem description",
            "FeedBack_Function": "Related function",
            "FeedBack_Contact": "Contact information"
        },
        "waterMark": {
            "InputWaterMarkContent": "Please enter watermark text"
        },
        "TextNote": {
            "CreateAnnotation": "Create Annotation",
            "TextNote_Title": "Text Annotation",
            "InputNumber1": "Please enter an integer within",
            "InputNumber2": "",
            "InputNumber": "Please input positive integer",
            "SequenceRepeat": "Sequence Repeat",
            "InputContent": "Please input content",
            "InputTextNoteContent": "Please input text annotation",
            "SizeOverrun": "Words out of limit",
            "NumberAnnotationTextValue": "1",
            "SequenceAnnotationCountRange": "Count Range [1,9007199254740991]",
            "SequenceAnnotationMaxCount": "Sequence Annotation Max Count"
        },
        "SHotSpot": {
            "SHotSpot_Title": "HotSpot",
            "Name": "Name",
            "Description": "Description",
            "Type": "SHotSpotType",
            "ChangeView": "ChangeModelView",
            "PlayAnimation": "PlayAnimation",
            "ModelView": "Model View",
            "Animation": "Animation",
            "AnimationProcess": "Animation Process",
            "AnimationStep": "Animation Step"
        },
        "Edit": {
            "TextNote_Edit": "Annotation editor",
            "TextNoteMulEdit": "Support edit one annotation only"
        },
        "RightMenu": {
            "RightMenu_Hide": "Hide",
            "RightMenu_Single_display": "Single display",
            "RightMenu_Centered_display": "Centered",
            "RightMenu_Turn_continue_rotation": "Turn continue rotation",
            "RightMenu_Close_continue_rotation": "Close continue rotation",
            "RightMenu_Set_Backgroud_Color": "Set Backgroud Color",
            "RightMenu_Turn_JoyStick": "Turn JoyStick",
            "RightMenu_Close_JoyStick": "Close JoyStick",
            "RightMenu_Explicit_exchange": "Explicit exchange",
            "RightMenu_Show_all": "Show all",
            "RightMenu_Move": "Move",
            "RightMenu_Setting": "Setting",
            "RightMenu_About": "About",
            "RightMenu_Licence": "License Information",
            "RightMenu_SetColor": "Set Color",
            "RightMenu_ResetSelect": "SelectedRestore",
            "RightMenu_CreateMateral": "Create Materal",
            "RightMenu_Version_Record": "Version Records",
            "RightMenu_ShapeSet": "Add To ShapeSet"
        },
        "VersionRecord": {
            "Record_Title": "Version Notes",
            "v10_0": "1、Optimize model loading and browsing performance;<br />2、Added the measurement result predisplay function;<br />3、Add select set functionality.",
            "v9_1": "1、Add ground、ground shadow、ground grid、ground mirror functionality;<br />2、Add the Xray selection effect;<br />3、Add SectionEdge and its measure function;<br />4、Optimized display style for annotations and measures;<br />5、Fix known defects.",
            "v9_0": "1、Add instantiation rendering;<br />2、Add multithreaded download functionality;<br />3、Add multithreaded loading capabilities;<br />4、Add progressive display function;<br />5、Add a one-click configuration scheme;<br />6、Add real-time calculation function in motion state;<br />7、Added edge loading on demand feature;<br />8、Add ambient shading;<br/>9、Fix known defects.",
            "v8_2": "1、Enhanced product stability;<br />2、Increase log output function;<br />3、Realize component view, component PMI display and data management functions;<br />4、Add specified section function;<br />5、Increase highlight selection effect;<br />6、Add thickness measurement function;<br />7、Add the function of creating the center of the circle, and improve the function of measuring the center of the circle;<br />8、Add related measurement functions for objects without geometric attributes.",
            "v8_1": "1、Add H5 animation audio playback function;<br />2、Add the default view function;<br />3、Increase model Board function;<br />4、Increase the grading explosion function;<br />5、Add annotations, modelboard, etc. to synchronize with the model to highlight and hide functions;<br />6、Fix known defects.",
            "v8_0": "1、Add coordination meeting module;<br />2、Improve performance;<br />3、Add the function of only load Bom;<br />4、Add the function of load in stream mode;<br />5、Add the illustration mode;<br />6、Add the function of on demand loading;<br />7、Fix known defects.<br />",
            "v7_2": "1、Add annotations and measure follow-up effects;<br />2、Add scene information reading and server-side storage;<br />3、Add model border effect reading;<br />4、Add hot scene reading function;<br />5、Optimize online view function;<br />6、Add multi-faceted section function;<br />7、Add scattered file browsing;<br />8、Fix known defects.<br />",
            "v7_1": "1、Optimize the interface effect;<br />2、Modify measurement display style and preselected effect;<br />3、Add component PMI display function;<br />4、Modify the roaming effect and add up and down virtual buttons;<br />5、Add online view save;<br />6、Improve the display of configuration items and increase the classification of systems and documents;<br />7、Fix known defects.<br />",
            "v7_0": "1、Optimize opening speed and display performance;<br />2、Add LOD loading;<br />3、Add on-demand loading;<br />4、Add path animation;<br />5、Add caching mechanism;<br />6、Optimize the interface effect;<br />7、Fix known defects.<br />",
            "v6_2": "1、Unified product interface;<br />2、Optimize model opening speed;<br />3、Add tool animation;<br />4、Optimize the interface and prompt information;<br />5、Fix known defects.<br />",
            "v6_1": "1、Modify license authentication method;<br />2、Improve roaming experience;<br />3、Add custom skybox display;<br />4、Add sub-animation to read and play;<br />5、Add area and volume attributes and interfaces;<br />6、Fix bug of slow bom loading.<br />",
            "v6_0": "1、Fix light reading defects;<br />2、Adding animated track lines;<br />3、Add hotspot feature;<br />4、Add centerline angle measurement;<br />5、Add setting background image interface;<br />6、Fix known defects.<br />",
            "v5_0": "1、Added Jewelry Mode;<br />2、Translucent defect modification;<br />3、Selection style added;<br />4、Fixed model reading error when missing materials;<br />5、Fix some model color display errors;<br />6、Modify face selection style to be consistent with PC and mobile.<br />"
        },
        "RightMenuTitle": {
            "RestoreView_li": "Restore",
            "ViewList": "View",
            "assembly": "Assembly",
            "mainMenu": "Menu",
            "walking": "Walking",
            "FullScreen": "Full screen",
            "Apply": "Apply",
            "ExitFullScreen": "Exit full screen",
            "modelproperties": "Properties ",
            "synergyMeeting": "Meeting",
            "TextMessage": "Chat",
            "OtherOperations": "Other Operations",
            "AlignmentCamera": "Alignment Camera"
        },
        "ViewMenuTitle": {
            "solidMode": "Solid Mode",
            "solidWireMode": "Solid Wire Mode",
            "illustrationMode": "Illustration Mode",
            "transparentMode": "Transparent Mode",
            "triangularGrid": "Triangular Grid",
            "topView": "Top View",
            "axisView": "Axis View",
            "leftView": "Left View",
            "frontView": "Front View",
            "rightView": "Right View",
            "behindView": "Behind View",
            "bottomView": "Bottom View",
            "perspective": "Perspective",
            "walkThrough": "Walk Through",
            "setting": "Setting",
        },
        "BottomMenuTitle": {
            "_note": "Annotation",
            "_scaleL": "Hand Note",
            "animation": "Animation",
            "measure": "Measure",
            "li_explosive": "Explosive",
            "lipq": "Section",
            "snapshot": "Snapshot",
            "clear": "Clear"
        },
        "SectionMenuTitle": {
            "section_bar_main": "Section Direction",
            "section_yzsection": "YZ Section",
            "section_xysection": "XY Section",
            "section_xzsection": "ZX Section",
            "section_reversal": "Reverse Section",
            "section_cappingPlane": "Cover Surface",
            "section_clipEdge": "Section Edge",
            "section_plane": "Section Surface",
            "section_addselected": "Add Selected",
            "section_clearselected": "Clear Selected",
            "section_selectedSet": "Section Shapes",
        },
        "ExplosiveMenuTitle": {
            "section_bar_main": "Explosive Direction",
            "explosive_center": "Center",
            "explosive_by_x": "Explosive By X",
            "explosive_by_y": "Explosive By Y",
            "level_none": "None",
            "exit": "Exit",
            "confirm": "Confirm",
            "cancel": "Cancel",
            "sure_exit": "Are you sure you want to quit?",
            "hint": "Hint"
        },
        "NoteMenuTitle": {
            "node_text": "Text Annotation",
            "note_order": "Order Annotation",
            "note_component": "Model Name",
            "note_shotspot": "HotSpot",
            "node_edit": "Annotation Edit",
            "node_del": "Annotation Delete"
        },
        "GestureMenuTitle": {
            "gesture_bar_main": "Current Mode",
            "freedom": "Freedom",
            "circle": "Circle",
            "rect": "Rect",
            "triangle": "Triangle",
            "gesture_bar_second_main": "Color",
            "gesture_black": "Black",
            "gesture_red": "Red",
            "gesture_blue": "Blue",
            "gesture_green": "Green",
            "gesture_yellow": "Yellow",
            "gesture_orange": "Orange",
            "gesture_gray": "Gray",
            "gesture_purple": "Purple",
            "gesture_brown": "Brown",
            "gesture_pink": "Pink",
            "gesture_white": "White",
            "gesture_bar_third_main": "Thickness",
            "gesture_thin_line": "Thin",
            "gesture_middle_line": "Middle",
            "gesture_strong_line": "Strong",
            "gesture_undo": "Revoke"
        },
        "DraggerMenuTitle": {
            "dragger_main": "Dragger Mode",
            "trans": "Trans",
            "rot": "Rot",
            "sca": "Sca",
            "dragger_face": "Along Plane",
            "yzsection": "Along X",
            "zxsection": "Along Y",
            "xysection": "Along Z",
            "Cyzsection": "Winding X",
            "Czxsection": "Winding Y",
            "Cxysection": "Winding Z",
            "dragger_Centertranslation": "Winding Center",
            "dragger_planetranslation": "Along The Whole",
            "exit": "Exit"
        },
        "ClearMenuTitle": {
            "ClearAnnotation": "Empty annotation objects",
            "ClearMeasure": "Empty measurement objects",
            "ClearSection": "Clear Section",
            "ClearAnnotationPrompt": "After the annotation objects are deletd,they will not be recovered.Are you sure you want to delete all annotation objects?",
            "ClearMeasurePrompt": "After the measurement objects are deleted,they will not be recovered.Are you sure you want to delete all the measurement objects?",
            "ClearSectionPrompt": "After the section planes are deleted,they will not be recovered.Are you sure you want to delete all the section planes?"
        },
        "AnimationMenuTitle": {
            "start": "Start",
            "pause": "Pause",
            "forward": "Forward",
            "backward": "Backward",
            "more": "More",
            "exit": "Exit"
        },
        "MeasureMenuTitle": {
            "measure_Type_distance": "Distance",
            "measure_Type_Angle": "Angle",
            "measure_Type_Circle": "Circle",
            "measure_Type_Property": "Property",
            "measure_Distance_PntToPnt": "Point to Point",
            "measure_Distance_PntToLine": "Point to Line",
            "measure_Distance_PntToFace": "Point to Face",
            "measure_Distance_LineToLine": "Line to Line",
            "measure_Distance_LineToFace": "Line to Face",
            "measure_Distance_FaceToFace": "Face to Face",
            "measure_Distance_Thickness": "Thickness",
            "measure_Angle_LineToLine": "Line with Line",
            "measure_Angle_LineToFace": "Line with Face",
            "measure_Angle_FaceToFace": "Face with Face",
            "measure_Circle_Center_Arc": "Create center by Curve",
            "measure_Circle_Center_Point": "Create center by three Points",
            "measure_Diameter": "Diameter",
            "measure_Radius": "Radius",
            "measure_Distance_ShaftToShaft": "Shaft to Shaft",
            "measure_Distance_CenterToCenter": "Center to Center",
            "measure_Property_Point": "Point",
            "measure_Property_Line": "Line",
            "measure_Property_Face": "Face",
            "measure_Property_Model": "Model",
            "measure_Box_Note": "Bounding Box Note",
            "measure_del": "Delete",
            "backward": "Backward"
        },
        "ViewList": {
            "showEdgeLine": "EdgeLine and Mode",
            "notshowEdgeLine": "Mode with not Show EdgeLine",
            "onlyshowEdgeLine": "Only show EdgeLine",
            "IsTechnicalView": "Technical",
            "isNoteVisibleWithModel": "Visible with Model(Annotation,Board)",
            "isNoteHighLightWithModel": "High light with Model(Annotation,Board)",
            "IsShowTransparent": "Transparent",
            "top": "Top",
            "isometric": "Isometric",
            "left": "Left",
            "front": "Front",
            "right": "Right",
            "back": "Back",
            "bottom": "Bottom",
            "perspective": "Perspective",
            "showTrilateralEdge": "Show Grid",
            "mulSelect": "MultiSelect",
            "showBox": "Show Box",
            "observeMode": "Observate Mode",
            "FreeObserve": "FreeObservation",
            "OribitObserve": "DynamicObservation",
            "ConstraintObserve": "ConstraintObservation"
        },
        "Prompt": {
            "isOpening": "Opening...",
            "Prompt": "Prompt",
            "Downloading": "Downloading...",
            "WatingLoading": "Loading...",
            "PleaseWating": "Wating...",
            "FileNoExist": "File Not Exist",
            "FileFormatError": "SVLX File Format Error",
            "PleaseOpenFile": "Please Open File",
            "WalkThroughRestore": "Reset will affect the roaming effect and cannot be used at the same time",
            "PleaseSelectOneAssembly": "Please select an assembly first",
            "InputNumber": "Please input positive integer",
            "TextNoteMulEdit": "Currently does not support simultaneous editing of multiple objects",
            "AnnotationNotEmoji": "Emoji not supported",
            "AnnotationDonotEdit": "The current annotation object cannot be edited",
            "PleaseSelectOneAnnotation": "Please select an annotation object first",
            "PleaseSelectOneMeasure": "Please select a measurement object first",
            "DeviceDoesNotThisFunction": "The current device does not support this function",
            "CreateSuccess": "Created successfully",
            "CreateError": "Creation failed",
            "AxisIntersect": "The axis where the arc intersects",
            "ParallelOrNoteSamePlane": "Select objects to be parallel or not in the same plane",
            "SurfaceCannotMeasure": "Only supports plane measurement",
            "CurveCannotMeasure": "Cannot perform arc measurement",
            "NotFindCurveCenter": "Center of arc not found",
            "SameShapeCannotMeasure": "The same object cannot be measured",
            "PointsAreOnSmaeLine": "The points are on the same line.Please reselect",
            "NotGeoAttribute": "The measuring object has no geometric properties",
            "ShapeIntersect": "Select objects to intersect",
            "ModelNameError": "The name can only contain Chinese characters, letters, numbers, underscores and dots",
            "DuplicateViewName": "A view with the same name already exists, please rename it",
            "PleaseSelectAnnotationPoint": "Please select annotation point",
            "PleaseSelectAnnotationPosition": "Please select annotation position",
            "PleaseSelectFirstMeasurePoint": "Please select the first measuring point",
            "PleaseSelectSecondMeasurePoint": "Please select the second measuring point",
            "PleaseSelectFirstMeasureLine": "Please select the first measuring line",
            "PleaseSelectSecondMeasureLine": "Please select the second measuring line",
            "PleaseSelectFirstMeasureFace": "Please select the first measuring face",
            "PleaseSelectSecondMeasureFace": "Please select the second measuring face",
            "PleaseSelectOneMeasureLine": "Please select a measuring line",
            "PleaseSelectOneMeasureFace": "Please select a measuring face",
            "PleaseSelectFirstPoint": "Please select the first point",
            "PleaseSelectSecondPoint": "Please select the second point",
            "PleaseSelectThirdPoint": "Please select the third point",
            "PleaseSelectFirstStraightLine": "Please select the first straight line",
            "PleaseSelectFirstLine": "Please select the first line",
            "PleaseSelectSecondStraightLine": "Please select the second straight line",
            "PleaseSelectSecondLine": "Please select the second line",
            "PleaseSelectFirstPlane": "Please select the first plane",
            "PleaseSelectFirstFace": "Please select the first face",
            "PleaseSelectSecondPlane": "Please select the second plane",
            "PleaseSelectSecondFace": "Please select the second face",
            "PleaseSelectFirstCurve": "Please select the first arc",
            "PleaseSelectSecondCurve": "Please select the second arc",
            "PleaseSelectOnePoint": "Please select a point",
            "ClickAnyWhere": "Click anywhere to see the results",
            "PleaseSelectOneLine": "Please select a straight line",
            "PleaseSelectOneStrLine": "Please select the line",
            "PleaseSelectOnePlane": "Please select a plane",
            "PleaseSelectOneFace": "Please select face",
            "PleaseSelectFace": "Please select the face",
            "PleaseSelectOneCurve": "Please select arc",
            "PleaseSelectOneModel": "Please select a model",
            "NameNotNull": "Name is required",
            "PleaseSelectDeleteView": "Please select a view to delete",
            "DeleteSuccess": "Successfully deleted",
            "ProhibitDeletion": "Prohibit deletion",
            "ProhibitEdit": "Prohibit editing",
            "DeleteModelViewSuccess": "Successfully deleted modelView",
            "DeleteError": "Failed to delete",
            "RemoveSuccess": "Successfully removed",
            "RemoveError": "Failed to remov",
            "AddSuccess": "Successfully added",
            "AddError": "Failed to add",
            "ViewCannotDelete": "This view does not allow deletion",
            "PleaseSelectAssembly": "Please select fittings first",
            "AssemblyCanAddModel": "Add models to add models",
            "PleaseSelectModel": "Please select a model first",
            "CannotAddPeerAssembly": "Cannot add same-level assembly parts",
            "SelectBin": "Please also select the corresponding bin file of JSvl",
            "SuccessfulOperation": "Successful operation",
            "OperationFailed": "Operation failed",
            "DocSettingSaveFailed": "Failed to save the document configuration",
            "TopAssemblyCannotCopy": "Top assembly cannot be copied",
            "TopAssemblyCannotCut": "Top assembly cannot be cut",
            "AssemblyOrModelCannotNull": "Assembly or model must not be empty",
            "PeerAssemblyCannotPaste": "The same level assembly can not be pasted",
            "ClipboardIsEmpty": "Clipboard is empty and cannot be pasted",
            "CircularReference": "There is a circular reference and cannot be pasted",
            "TopAssemblyCannotDelete": "The top assembly cannot be deleted",
            "NetworkError": "Network anomaly",
            "DeleteSelectedView": "Do you want to delete the selected view?",
            "DeleteCache": "After the cache data is cleared, it cannot be restored, whether to delete it?",
            "RestoreDefaultSetting": "After restoring the default settings, all configurations will be restored, whether to restore?",
            "SureDelete": "Do you want to delete",
            "ResetName": "Rename",
            "ViewName": "View name",
            "DeleteSelectedAnnotation": "Whether to delete the selected comment?",
            "DeleteSelectedMeasure": "Whether to delete the selected measurement?",
            "DeleteSelectedObject": "Remove selected Object?",
            "SureFreeColor": "Are you sure to reconfigure the colors for the assembly?",
            "ExitCurrentOperation": "Whether to quit the current operation?",
            "PlayCameraPreTip": "Verify that the data is saved to the view?",
            "SelectThicknessDriection": "Direction",
            "VerticalSurface": "Perpendicular to the surface",
            "VerticalScreen": "Perpendicular to the screen",
            "SaveCurrentAnnotation": "Whether to keep the current annotation?",
            "NeedOpenWalkThrough": "Need to enable roaming mode!",
            "NeedReload": "Please refresh the page",
            "OpenMergeFace": "Need to refresh the page to take effect",
            "CloseMergeFace": "Need to refresh the page to take effect",
            "MeasureMergeFace": "Please close the merged face and refresh the page",
            "SHotPotNameRepeat": "Hotspot name has been used",
            "SHotPotNotName": "Please enter the hotspot name",
            "SecretKeyError": "Key file request error",
            "FileSecret": "This file is an encrypted file, please configure the key information",
            "OpenError": "Open failed",
            "Annotation": "Annotation",
            "Confim": "Confim",
            "Delete": "Delete",
            "Restore": "Restore",
            "Exit": "Exit",
            "Cancle": "Cancel",
            "Yes": "Yes",
            "No": "No",
            "HasApplyOperateAuth": "The authorization has been apply",
            "HasApplyAudioAndVideoAuth": "The authorization has been apply",
            "QuitGroup": "Determine to quit meeting?",
            "ApplyAuth": "Do you apply to the host for operation permission?",
            "ApplyAudioAndVideo": "Do you apply to the host for audio and video permission?",
            "DefaultView": "DefaultView",
            "ApplySetDefaultView": "Whether to set the default view",
            "ApplySetUndefaultView": "Whether to cancel the default view",
            "PleaseOpenServerView": "Please enable online view",
            "LocalViewDonotSet": "The local view cannot be set as the default view",
            "SearchOver": "Search Over",
            "WalkThrough_Move": "Please turn off WalkThrough mode before moving the model",
            "PleaseSelectShapeSet": "Please select or create a ShapeSet",
            "PleaseSelectModelOnly": "Only model data is supported for addition to the ShapeSet",
            "PleaseSelectModelOnlySection": "Only model data is supported for addition to the sectioning objects ",
            "DeleteShapeSet": "Whether to delete the ShapeSet?",
            "RemoveFromShapeSet": "Whether to remove from the ShapeSet?",
            "DeleteSectionShape": "Whether to delete the sectional shape(s)?",
            "ClearSectionShapes": "Whether to empty sectional shapes?",
            "ClearSuccess": "Successfully",
            "ClearError": "failed",
            "NoAnimationFile": "No animation file",
            "SaveSuccess": "Save Success",
            "SaveError": "Save Error",
        },
        "OpenErorr": {
            "SelectBin": "Please also selece the corresponding bin file for JSvl!"
        },
        "Material": {
            "CreateMaterial": "CreateMaterial",
            "MaterialType": "MaterialType",
            "MaterialName": "MaterialName",
            "DifusseColor": "DifusseColor",
            "SpecularColor": "SpecularColor",
            "Shininess": "Shininess",
            "Transparency": "Transparency",
            "AmbientColor": "AmbientColor",
            "EmissiveColor": "EmissiveColor",
            "UvRotate": "TextureCoordinateRotate",
            "UvScale": "TextureCoordinateScale",
            "UvTranslate": "TextureCoordinateTranslate",
            "DifusseTexture": "DifusseTexture",
            "SpecularTexture": "SpecularTexture",
            "EmissiveTexture": "EmissiveTexture",
            "NormalTexture": "NormalTexture",
            "NormalMapScale": "NormalTextureScale",
            "MetalnessFactor": "MetalnessFactor",
            "RoughnessFactor": "RoughnessFactor",
            "UseClearCoat": "UseClearCoat",
            "ClearCoat": "ClearCoat",
            "ClearCoatRoughness": "ClearCoatRoughness",
            "MetalnessRoughnessTexture": "MetalnessRoughnessTexture",
            "MatcapTexture": "MatcapTexture",
            "Image": "Image"
        },
        "License": {
            "Licence_Error": "Prompt",
            "ErrorCode": "Error Code:",
            "GetLIcenseFailed": "LicenseError:Failed to obtain license information",
            "LicenseInfoError": "LicenseError:License Information error",
            "ServerRequestFailed": "LicenseError:Server request failed",
            "LicenseTypeFailed": "LicenseError:License Type Failed",
            "LicenseCheckFailed": "LicenseError:License failure",
            "UnavailablePoints": "LicenseError:Unavailable points",
            "LicenseNotStarted": "LicenseError:License time not started",
            "LicenseExpired": "LicenseError:License expired",
            "NoUseAccess": "LicenseError:You have no access",
            "BrowserMismatch": "LicenseError:Browser mismatch",
            "UserMismatch": "LicenseError:User mismatch",
            "LisenseDisabled": "LicenseError:The license has been disable",
            "DeniedAccess": "LicenseError:You have been denied access",
            "ProductNameError": "LicenseError:Inconsistent product name",
            "ProductVersionError": "LicenseError:Inconsistent product version"
        },
        "Property": {
            "ID": "ID",
            "name": "Name",
            "Property": "Property",
            "Geometric": "Geometry",
            "Features": "Modeling",
            "Color": "Color",
            "SubmodelsCount": "SubModelCount",
            "InstanceCount": "InstanceCount",
            "InstanceID": "InstanceID",
            "ModleViewCount": "ModleViewCount",
            "PMICount": "PMICount",
            "FaceCount": "FaceCount",
            "LOD0PatchCount": "LOD0Count",
            "Volume": "Volume",
            "Area": "Area",
            "Point": "PointCoordinates",
            "Line": "LineLength",
            "CircleCenter": "CircleCenter",
            "CircleRadius": "Radius",
            "CircleArcLength": "ArcLength"
        },
        "ShapeSet": {
            "ShapeSet": "ShapeSet",
            "SelectedShapeSet": "Existing ShapeSet",
            "Add": "Add",
            "Delete": "Delete",
            "InputShapeSetName": "Please enter the shapeset name",
            "CreateShapeSet": "Create Shapeset",
            "Search": "Search",
        },
        "SweetAlert": {
            "Confirm": "Confirm",
            "Cancel": "Cancel",
            "EnterViewName": "Please enter a view name",
            "SaveError": "SaveError",
            "Rename": "Rename",
            "Remove": "Remove",
            "ViewCreating": "View creation, please wait...",
            "ViewRename": "View Rename",
            "DefaultView": "DefaultView",
            "InputViewName": "Enter a view name",
            "ParameterError": "Parameter error",
            "NoParameterError": "The parameter must not be null",
            "WithoutPermissionEdit": "Edit someone else's view without permission",
            "ViewRenameError": "View rename failed",
            "SetDefaultViewError": "Failed to set the default view. Please contact the administrator",
            "ViewRenameSuccess": "View renamed successfully",
            "ViewCreateError": "View creation failed",
            "ConfimRemoveView": "Are you sure to delete the view",
            "What": "?",
            "WithoutPermissionRemove": "Delete someone else's view without permission",
            "TimAuthorizationFailure": "Authorization failure",
            "TimOperateFailure": "Operate failure",
            "TimListGroupFailure": "List meeting failure",
            "TimLoginFailure": "Initialize meeting failure",
            "TimNoNickError": "Nickname cannot be empty",
            "TimNoGroupIDError": "Group ID cannot be empty",
            "TimCreateGroupFailure": "Create meeting failure",
            "TimSelectGroupToJoin": "Please select the meeting you want to join",
            "TimJoinGroupFailure": "Join meeting failure",
            "TimDismissGroupFailure": "Dismiss meeting failure",
            "TimQuitGroupFailure": "Quit meeting failure",
            "TimCopyGroupInfo": "The meeting information was successfully copied to the clipboard",
            "MsgSizeOverrun": "If the number of words exceeds the limit,please send it in separate columns",
            "SizeOverrun": "Words out of limit"
        },
        "SynergyGroup": {
            "GroupSetting": "Meeting Set",
            "NickName": "NickName",
            "NickNamePlaceHolder": "Enter NickName",
            "Save": "Save",
            "Reset": "Reset",
            "CreateGroup": "Create Meeting",
            "Theme": "Theme",
            "ThemePlaceHolder": "Enter Theme",
            "Join": "Join Meeting",
            "Cancel": "Cancel",
            "GroupList": "GroupList",
            "GroupID": "GroupID",
            "GroupIDPlaceHolder": "Enter GroupID",
            "Host": "Host",
            "CreateTime": "CreateTime",
            "ExitGroup": "Exit Meeting",
            "Invite": "Invite",
            "Attendees": "Attendees",
            "Attachment": "Attachment",
            "SelectFile": "Select File",
            "Info": "Info",
            "MuteAllMembers": "Total Silence",
            "UnmuteAllMembers": "Lift silence",
            "BanAllMembers": "Prohibited All",
            "AllowAllMembers": "Allow All",
            "ApplyMembers": "Applicants",
            "OpenAudio": "Open Audio",
            "CloseAudio": "Close Audio",
            "OpenVideo": "Open Video",
            "CloseVideo": "Close Video",
            "AllowOperate": "Allow Operate",
            "BanOperate": "Ban Operate",
            "NoSendBlank": "Cannot send blank messages",
            "ChatContentPlaceHolder": "Enter Messages",
            "Send": "Send",
            "Agree": "Agree",
            "Ageeed": "Ageeed",
            "WhoseMeeting": "'meeting",
            "DoesnotSupportAudioAndVideo": "The current browser does not support audio and video functions. You are advised to use the latest Chrome browser",
            "DeviceIsUnavailable": "The camera/microphone device is unavailable",
            "DeviceIsUnavailableAccessed": "The camera/microphone cannot be accessed for the time being. Please ensure that no other applications are currently requesting access to the camera/microphone and try again",
            "UnauthorizedDeviceAccedd": "Unauthorized camera/microphone access, audio and video calls cannot be performed",
            "NoDeviceDetected": "No camera/microphone device detected",
            "Initialize": "Initialize...",
            "MeetingWasUnexist": "Meeting was over or didn't exist",
            "HostEndTheMeeting": "Host End The Meeting",
            "ApplyOperation": "Apply Operation",
            "ApplyAudioAndVideoOperation": "Apply Audio And Video Operation",
            "CloseAudioAndVideoOperation": "Close Audio And Video Operation",
            "OpenAudioAndVideoOperation": "Open Audio And Video Operation",
            "EnabledYourAudioAndVideoOperation": "The Host has enabled your Audio and Video operation",
            "ClosedYourAudioAndVideoOperation": "The Host has closed your Audio and Video operation",
            "ServerIsConnected": "Server is connected succeddfully",
            "ConnectedToTheNetwork": "Connected to the network",
            "TheCurrentNetworkIsUnstable": "The current network is unstable",
            "TheCurrentNetworkIsUnavailable": "The current network is unavailable",
            "EnabledYourOperationPermissions": "The Host has enabled your operation permissions",
            "ClosedYourOperationPermissions": "The Host has closed your operation permissions",
            "CreationFailed": "Creation failed",
            "WaitingForTheAdministratorToAgree": "Waiting for the administrator to agree",
            "AlreadyInTheGroup": "Already in the group",
            "AddGroupSuccess": "Add group success",
            "Me": "Me",
            "I": "(Me)",
            "GroupOwner": "(GroupOwner)",
            "PleaseWait": "Please Wait...",
            "NoPassword": "Password:None",
            "UnableToGetAudioAndVideoData": "Unable to get audio and video data, please check audio and video equipment"
        },
        "MeetingDirection": {
            "OpenFile": "Open File",
            "SelectShape": "Select Shape",
            "HideModel": "Hide Model",
            "ShowModel": "Show Model",
            "SetModelColor": "Set Model Color",
            "RestoreModel": "Restore Shape",
            "ShowHideExchange": "Show Hide Exchange",
            "UnSelectShape": "Unselect Model",
            "ClearAllSelect": "Cancle all select",
            "MoveModel": "Transform Model",
            "RestoreView": "Restore View",
            "SetView": "Change View",
            "CreateView": "Create ModelView",
            "DeleteView": "Delete ModelView",
            "ChangeModelView": "Change ModelView",
            "ShowWalkthrough": "Open Walkthrough",
            "CloseWalkthrough": "Close Walkthrough",
            "SettingWalkThrough": "Update Walkthrough State",
            "SettingWalkthroughParameter": "Setting Walkthrough Parameters",
            "SetRederMode": "Set Render Mode",
            "Play": "Play",
            "CloseAnimation": "Close Animation",
            "PauseAnimation": "Pause Animation",
            "SetUseScene": "Set Use Scene",
            "SetLoop": "Set Loop",
            "SetPlaySpeed": "Set Play Speed",
            "SetPlayPercent": "Set Play Percent",
            "AddGestureNote": "Add Gesture Note",
            "AddTextNote": "Add Text Note",
            "UpdateTextNote": "Update Text Note",
            "DeleteTextNote": "Delete Text Note",
            "SelectAnnotation": "Select Annotation",
            "UnSelectAnnotation": "Unselect Annotation",
            "SelectMeasure": "Select Measure",
            "UnselectMeasure": "Unselect Measure",
            "AddSequenceNote": "Add Sequence Note",
            "AddMeasure": "Add Measure",
            "DeleteMeasure": "Delete Measure",
            "DeleteGesture": "Delete Gesture Note",
            "UpdateMeasurePosition": "Update Measure Position",
            "ClearAnnotation": "Empty annotation objects",
            "ClearMeasure": "Empty measurement objects",
            "ShowExplosion": "Show Explosion",
            "ExitExplosion": "Exit Explosion",
            "CloseSection": "Close Section",
            "Section": "Section"
        },
        "LogList": {
            "Log": "Log",
            "IsOpen": "Enable logging",
            "LogPath": "Path",
            "LogLevel": "Level",
            "Clear": "Clear",
            "AllClear": "One-click clear",
            "Error": "Error",
            "Warning": "Warning",
            "Info": "Info",
            "Debug": "Debug"
        }
    };
    SView.language_en = language_en;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @LastEditors nihy
     * @LastEditTime 2023-03-31 15:11:47
     * @FilePath \SView_WebGL2\Frame\SViewFrame\src\command\CommandConfig.ts
     * @description 命令配置类 一个配置对应一个parameters,parameters对应多个parameter
     * @author nihy
     * @date 2022-08-12 14:43:21
     */
    class CommandConfig {
        /**
         * @constructor
         * Creates an instance of CommandConfig.
         * @param {string} fullClassName
         * @memberof CommandConfig
         */
        constructor(fullClassName) {
            //优先级
            this.priority = SView.CommandPriority.Priority_7;
            //是否可中断
            this.isInterruptible = true;
            //是否需要显示对话框
            this.showDialog = false;
            this.executedHandleMode = -1;
            this.isPreView = 1;
            this.setFullClassName(fullClassName);
        }
        /**
         * @public
         * @description: 设置命令对应的全类名
         * @param {string} value
         */
        setFullClassName(value) {
            this.fullClassName = value;
            this.fullNameSpace = value.lastIndexOf(".") > -1 ? value.slice(0, value.lastIndexOf(".")) : value;
            this.className = value.lastIndexOf(".") > -1 ? value.slice(value.lastIndexOf(".") + 1) : value;
        }
        /**
         * @public
         * @description: 获取命令对应的全类名
         * @return {string} 命令对应的全类名，包括命名空间
         */
        getFullClassName() {
            return this.fullClassName;
        }
        /**
         * @public
         * @description 获取命令对应的全命名空间
         * @returns  {string}
         */
        getFullNameSpace() {
            return this.fullNameSpace;
        }
        /**
         * @public
         * @description 获取命令对应的类名
         * @returns {string}
         * @memberof CommandConfig
         */
        getClassName() {
            return this.className;
        }
        /**
         * @public
         * @description: 设置View名称
         * @param {string} value
         */
        setViewName(value) {
            if (!M3D.Utility.DecideHelper.isEmpty(value)) {
                this.viewName = value;
                this.viewNameSpace = value.lastIndexOf(".") > -1 ? value.slice(0, value.lastIndexOf(".")) : value;
                this.viewClassName = value.lastIndexOf(".") > -1 ? value.slice(value.lastIndexOf(".") + 1) : value;
            }
            else {
                //TODO
            }
        }
        /**
         * @public
         * @description: 获取View名称
         * @return {string} view的名称，包括命名空间
         */
        getViewName() {
            return this.viewName;
        }
        /**
         * @public
         * @description: 获取View名称
         * @return {string} view的名称，包括命名空间
         */
        getViewNameSpace() {
            return this.viewNameSpace;
        }
        /**
         * @public
         * @description: 获取View名称
         * @return {string} view的名称，包括命名空间
         */
        getViewClassName() {
            return this.viewClassName;
        }
        /**
         * @public
         * @description 获取命令名称
         * @param {string} value
         */
        setName(value) {
            this.name = value;
        }
        /**
         * @public
         * @description 获取命令名称
         * @returns {string}
         */
        getName() {
            return this.name;
        }
        /**
         * @public
         * @description:  设置命令的优先级
         * @param {number} value
         */
        setPriority(value) {
            this.priority = value;
        }
        /**
         * @public
         * @description: 获取命令的优先级
         * @return {number} 命令级别
         */
        getPriority() {
            return this.priority;
        }
        /**
         * @public
         * @description: 设置是否可被中断
         * @param {boolean} value
         */
        setIsInterruptible(value) {
            this.isInterruptible = value;
        }
        /**
         * @public
         * @description: 获取是否可被中断
         * @return {boolean} true :可中断 false:不可中断
         */
        getIsInterruptible() {
            return this.isInterruptible;
        }
        /**
         * @public
         * @description 设置是否显示命令对话框
         * @param value true :显示 false:不显示
         */
        setShowDialog(value) {
            this.showDialog = value;
        }
        /**
         * @public
         * @description: 是否显示对话框
         * @return {boolean} true :显示 false:不显示
         */
        getShowDialog() {
            return this.showDialog;
        }
        /**
         * @description  获取执行模式
         * @returns {number} 执行模式
         */
        getExecutedHandleMode() {
            return this.executedHandleMode;
        }
        /**
         * @description 设置执行模型
         * @param {number} value
         */
        setExecutedHandleMode(value) {
            this.executedHandleMode = value;
        }
        /**
         * @public
         * @description 获取命令是否预显
         * @returns  {number} 0不预显，1预显
         */
        getIsPreView() {
            return this.isPreView;
        }
        /**
         * @public
        * @description: 设置命令是否预显
        * @param {number} value 0不预显，1预显
        */
        setIsPreView(value) {
            this.isPreView = value;
        }
        /**
         * @public
        * @description 设置命令对应的参数列表
        * @param {CommandParameters} value SCommandParameters类型对象
        */
        setInputParameters(value) {
            this.inputParameters = value;
        }
        /**
         * @public
         * @description: 获取参数列表
         * @return {CommandParameters} 返回SCommandParameters类型对象
         */
        getInputParameters() {
            if (this.inputParameters) {
                return this.inputParameters;
            }
            else {
                SView.FrameLogger.FRAMELOGW("Command's inputParameters is null");
            }
        }
        /**
         * @public
        * @description: 设置命令对应的参数列表
        * @param {CommandParameters} value SCommandParameters类型对象
        */
        setOutputParameters(value) {
            this.outputParameters = value;
        }
        /**
         * @public
         * @description: 获取参数列表
         * @return {CommandParameters} 返回SCommandParameters类型对象
         */
        getOutputParameters() {
            if (this.outputParameters) {
                return this.outputParameters;
            }
            else {
                SView.FrameLogger.FRAMELOGW("Command's outputParamaters is null");
            }
        }
    }
    SView.CommandConfig = CommandConfig;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     *  @memberof SView
     * @description 命令配置列表
     * @author nihy
     * @date 2022-08-12 17:30:16
     */
    class CommandConfigList {
        constructor() {
            // 黑名单
            this.blackList = [];
            // 白名单
            this.whiteList = [];
            //黑名单之外的命令列表
            this.exceptBlackList = [];
            //白名单之外
            this.exceptWhiteList = [];
        }
        /**
         * @public
         * @description: 设置命令名称
         * @param {string} value 命令名称
         */
        set name(value) {
            this._name = value;
        }
        /**
         * @public
         * @description: 获取命令名称
         * @return {string}
         */
        get name() {
            return this._name;
        }
        /**
         * @public
         * @description: 设置为大部分命令在白名单中
         * @param {boolean} value
         */
        set isMostInWhiteList(value) {
            this._isMostInWhiteList = value;
        }
        /**
         * @public
         * @description: 是否大部分命令在白名单中
         * @return {boolean}
         */
        get isMostInWhiteList() {
            return this._isMostInWhiteList;
        }
        /**
         * @public
        * @description:  设置为大部分命令在黑名单中
        * @param {boolean} value
        */
        set isMostInBlackList(value) {
            this._isMostInBlackList = value;
        }
        /**
         * @public
         * @description:  是否大部分命令在黑名单中
         * @return {boolean}
         */
        get isMostInBlackList() {
            return this._isMostInBlackList;
        }
    }
    SView.CommandConfigList = CommandConfigList;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @enum
    * @memberof SView
    * @description 命令事件
    * @author nihy
    * @date 2022-08-12 17:30:16
    */
    let CommandEvent;
    (function (CommandEvent) {
        CommandEvent[CommandEvent["Started"] = 0] = "Started";
        CommandEvent[CommandEvent["Executing"] = 1] = "Executing";
        CommandEvent[CommandEvent["Execute"] = 2] = "Execute";
        CommandEvent[CommandEvent["Executed"] = 3] = "Executed";
        CommandEvent[CommandEvent["Cancelling"] = 4] = "Cancelling";
        CommandEvent[CommandEvent["Cancelled"] = 5] = "Cancelled";
        CommandEvent[CommandEvent["Closing"] = 6] = "Closing";
        CommandEvent[CommandEvent["Closed"] = 7] = "Closed";
        CommandEvent[CommandEvent["WaitingInput"] = 8] = "WaitingInput";
        CommandEvent[CommandEvent["Interrupted"] = 9] = "Interrupted";
        CommandEvent[CommandEvent["Exception"] = 10] = "Exception";
        CommandEvent[CommandEvent["Navigation"] = 100] = "Navigation";
        CommandEvent[CommandEvent["Progress"] = 101] = "Progress";
        CommandEvent[CommandEvent["Waiting"] = 102] = "Waiting";
        CommandEvent[CommandEvent["Panel"] = 103] = "Panel";
        CommandEvent[CommandEvent["UserSelectionWindow_AbortCurOrContinue"] = 104] = "UserSelectionWindow_AbortCurOrContinue";
        CommandEvent[CommandEvent["UserSelectionWindow_AbortCurOrWaitingNew"] = 105] = "UserSelectionWindow_AbortCurOrWaitingNew"; //用户选择窗口 中止当前的命令启动新命令或者新命令放入等待队列，执行完成后再运行
    })(CommandEvent = SView.CommandEvent || (SView.CommandEvent = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @description 命令事件参数
    * @author nihy
    * @date 2022-08-12 17:30:16
    */
    class SCommandEventArgs {
        SCommandEventArgs(args) {
            this.args = args;
        }
    }
    SView.SCommandEventArgs = SCommandEventArgs;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @hidden
    * @beta
    * @description 命令异常
    * @author nihy
    * @date 2022-08-12 17:30:16
    */
    class CommandException extends M3D.Utility.Exception {
    }
    SView.CommandException = CommandException;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @hidden
    * @beta
    * @description 命令异常类型
    * @author nihy
    * @date 2022-08-12 17:30:16
    */
    class CommandExceptionType {
    }
    /************************************************ 命令管理异常 ***********************************************/
    /// <summary>
    /// 执行成功
    /// </summary>
    CommandExceptionType.failure = -1;
    /// <summary>
    /// 执行成功
    /// </summary>
    CommandExceptionType.success = 0;
    /// <summary>
    /// 找不到命令
    /// </summary>                    
    CommandExceptionType.notFoundCommand = 1;
    /// <summary>
    /// 命令没有定义
    /// </summary>          
    CommandExceptionType.commandNotDefined = 2;
    /// <summary>
    /// 命令对话框没有定义
    /// </summary>          
    CommandExceptionType.commandDialogNotDefined = 3;
    /// <summary>
    /// 命令为空
    /// </summary>          
    CommandExceptionType.commandNull = 4;
    /// <summary>
    /// DLL丢失
    /// </summary>         
    CommandExceptionType.dllMissing = 5;
    /// <summary>
    /// 获取命令类型失败
    /// </summary>         
    CommandExceptionType.getCommandTypeFailed = 6;
    /// <summary>
    /// 获取对话框类型失败
    /// </summary>         
    CommandExceptionType.getCommandDialogTypeFailed = 7;
    /// <summary>
    /// 创建命令失败
    /// </summary>         
    CommandExceptionType.createCommandFailed = 8;
    /// <summary>
    /// 参数不正确
    /// </summary>         
    CommandExceptionType.parameterIsIncorrect = 9;
    /// <summary>
    /// 没有找到指定参数
    /// </summary>     
    CommandExceptionType.notFoundParameter = 10;
    /// <summary>
    /// 丢失权限
    /// </summary>     
    CommandExceptionType.missingPermission = 11;
    /// <summary>
    /// 等待运行
    /// </summary>     
    CommandExceptionType.isWaiting = 12;
    /// <summary>
    /// 模式冲突
    /// </summary>     
    CommandExceptionType.modeConflict = 13;
    /// <summary>
    /// 在运行列表
    /// </summary>     
    CommandExceptionType.inRunningList = 14;
    /// <summary>
    /// 不可中断
    /// </summary>     
    CommandExceptionType.noninterruptible = 15;
    /// <summary>
    /// 黑名单
    /// </summary>     
    CommandExceptionType.blackList = 16;
    /// <summary>
    /// 不在运行列表
    /// </summary>     
    CommandExceptionType.notInRunningList = 17;
    /// <summary>
    /// 执行失败
    /// </summary>     
    CommandExceptionType.executedFailed = 18;
    /// <summary>
    /// 预览失败
    /// </summary>     
    CommandExceptionType.previewFailed = 19;
    /// <summary>
    /// 应用失败
    /// </summary>     
    CommandExceptionType.applyFailed = 20;
    /// <summary>
    /// 配置为空
    /// </summary>     
    CommandExceptionType.configIsNull = 21;
    /// <summary>
    /// View为空
    /// </summary>     
    CommandExceptionType.viewIsNull = 22;
    /// <summary>
    /// 命令参数不足
    /// </summary>
    CommandExceptionType.parametersNotEnough = 23;
    /// <summary>
    /// 构造函数错误
    /// </summary>
    CommandExceptionType.missingConstructor = 24;
    /************************************************ 命令管理异常 ***********************************************/
    /************************************************ 具体异常 ***********************************************/
    /// <summary>
    /// 对象为空
    /// </summary>
    CommandExceptionType.objectNull = 200;
    /// <summary>
    /// 内存不足
    /// </summary>
    CommandExceptionType.memoryNotEnough = 201;
    /// <summary>
    /// 参数错误
    /// </summary>     
    CommandExceptionType.parameterError = 202;
    /// <summary>
    /// 未找到对话框
    /// </summary>       
    CommandExceptionType.dialogNotFound = 203;
    CommandExceptionType.other = 1000;
    SView.CommandExceptionType = CommandExceptionType;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @enum
     * @Author  nihy
     * @Date  2022-08-16 11:01:33
     * @LastEditors  nihy
     * @LastEditTime  2022-08-17 15:04:32
     * @FilePath  \SView_WebGL2\Frame\SViewFrame\src\command\CommandExecutePermission.ts
     * @Description  命令是否可运行
     *
     */
    let CommandExecutePermission;
    (function (CommandExecutePermission) {
        CommandExecutePermission[CommandExecutePermission["canExecute"] = 0] = "canExecute";
        CommandExecutePermission[CommandExecutePermission["commandNull"] = 1] = "commandNull";
        CommandExecutePermission[CommandExecutePermission["noLicense"] = 2] = "noLicense";
        CommandExecutePermission[CommandExecutePermission["modeConflict"] = 3] = "modeConflict";
        CommandExecutePermission[CommandExecutePermission["animationPlayConflict"] = 4] = "animationPlayConflict";
        CommandExecutePermission[CommandExecutePermission["animationRecConflict"] = 5] = "animationRecConflict";
        CommandExecutePermission[CommandExecutePermission["browseModeConflict"] = 6] = "browseModeConflict";
        CommandExecutePermission[CommandExecutePermission["motionCollisionConflict"] = 7] = "motionCollisionConflict";
        CommandExecutePermission[CommandExecutePermission["roamConflict"] = 8] = "roamConflict";
        CommandExecutePermission[CommandExecutePermission["isRunning"] = 9] = "isRunning";
        CommandExecutePermission[CommandExecutePermission["inRunningList"] = 10] = "inRunningList";
        CommandExecutePermission[CommandExecutePermission["inWaitingQueue"] = 11] = "inWaitingQueue";
        CommandExecutePermission[CommandExecutePermission["userAborted"] = 12] = "userAborted";
        CommandExecutePermission[CommandExecutePermission["waitingOperation"] = 13] = "waitingOperation";
        CommandExecutePermission[CommandExecutePermission["noninterruptible"] = 14] = "noninterruptible";
        CommandExecutePermission[CommandExecutePermission["blackList_PermissionDenied"] = 15] = "blackList_PermissionDenied";
        CommandExecutePermission[CommandExecutePermission["interruptedOperation"] = 16] = "interruptedOperation";
        CommandExecutePermission[CommandExecutePermission["replacedOperation"] = 17] = "replacedOperation";
        CommandExecutePermission[CommandExecutePermission["parallelOperation"] = 18] = "parallelOperation";
        CommandExecutePermission[CommandExecutePermission["userChoicingAbortCurOrContinue"] = 19] = "userChoicingAbortCurOrContinue";
        CommandExecutePermission[CommandExecutePermission["userChoicingAbortCurOrWaitingNew"] = 20] = "userChoicingAbortCurOrWaitingNew"; //用户选择运行 中止当前的命令启动新命令或者新命令放入等待队列，执行完成后再运行
    })(CommandExecutePermission = SView.CommandExecutePermission || (SView.CommandExecutePermission = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @enum {number}
     * @description 命令执行返回值
     * @author nihy
     * @date 2022-08-16 11:09:39
     */
    let CommandExecuteResult;
    (function (CommandExecuteResult) {
        CommandExecuteResult[CommandExecuteResult["Failure"] = -1] = "Failure";
        CommandExecuteResult[CommandExecuteResult["Success"] = 0] = "Success";
        CommandExecuteResult[CommandExecuteResult["NotFoundCommand"] = 1] = "NotFoundCommand";
        CommandExecuteResult[CommandExecuteResult["CommandNotDefined"] = 2] = "CommandNotDefined";
        CommandExecuteResult[CommandExecuteResult["CommandDialogNotDefined"] = 3] = "CommandDialogNotDefined";
        CommandExecuteResult[CommandExecuteResult["CommandNull"] = 4] = "CommandNull";
        CommandExecuteResult[CommandExecuteResult["DLLMissing"] = 5] = "DLLMissing";
        CommandExecuteResult[CommandExecuteResult["GetCommandTypeFailed"] = 6] = "GetCommandTypeFailed";
        CommandExecuteResult[CommandExecuteResult["GetCommandDialogTypeFailed"] = 7] = "GetCommandDialogTypeFailed";
        CommandExecuteResult[CommandExecuteResult["CreateCommandFailed"] = 8] = "CreateCommandFailed";
        CommandExecuteResult[CommandExecuteResult["ParameterIsIncorrect"] = 9] = "ParameterIsIncorrect";
        CommandExecuteResult[CommandExecuteResult["NotFoundParameter"] = 10] = "NotFoundParameter";
        CommandExecuteResult[CommandExecuteResult["MissingPermission"] = 11] = "MissingPermission";
        CommandExecuteResult[CommandExecuteResult["IsWaiting"] = 12] = "IsWaiting";
        CommandExecuteResult[CommandExecuteResult["ModeConflict"] = 13] = "ModeConflict";
        CommandExecuteResult[CommandExecuteResult["InRunningList"] = 14] = "InRunningList";
        CommandExecuteResult[CommandExecuteResult["Noninterruptible"] = 15] = "Noninterruptible";
        CommandExecuteResult[CommandExecuteResult["BlackList"] = 16] = "BlackList";
        CommandExecuteResult[CommandExecuteResult["NotInRunningList"] = 17] = "NotInRunningList";
        CommandExecuteResult[CommandExecuteResult["ExecutedFailed"] = 18] = "ExecutedFailed";
        CommandExecuteResult[CommandExecuteResult["PreviewFailed"] = 19] = "PreviewFailed";
        CommandExecuteResult[CommandExecuteResult["ApplyFailed"] = 20] = "ApplyFailed";
        CommandExecuteResult[CommandExecuteResult["ConfigIsNull"] = 21] = "ConfigIsNull";
        CommandExecuteResult[CommandExecuteResult["ViewIsNull"] = 22] = "ViewIsNull";
        CommandExecuteResult[CommandExecuteResult["ParametersNotEnough"] = 23] = "ParametersNotEnough";
        CommandExecuteResult[CommandExecuteResult["NotContainView"] = 24] = "NotContainView";
        CommandExecuteResult[CommandExecuteResult["MissingConstructor"] = 25] = "MissingConstructor";
        /************************************************ 命令管理异常 ***********************************************/
        CommandExecuteResult[CommandExecuteResult["LowPriority"] = 100] = "LowPriority";
        CommandExecuteResult[CommandExecuteResult["Waiting"] = 101] = "Waiting";
        CommandExecuteResult[CommandExecuteResult["UserChoicing_AbortCurOrContinue"] = 102] = "UserChoicing_AbortCurOrContinue";
        CommandExecuteResult[CommandExecuteResult["UserChoicing_AbortCurOrWaitingNew"] = 103] = "UserChoicing_AbortCurOrWaitingNew";
        CommandExecuteResult[CommandExecuteResult["Close"] = 104] = "Close";
        CommandExecuteResult[CommandExecuteResult["Interrupted"] = 105] = "Interrupted";
        /************************************************ 具体异常 ***********************************************/
        CommandExecuteResult[CommandExecuteResult["ObjectNull"] = 200] = "ObjectNull";
        CommandExecuteResult[CommandExecuteResult["MemoryNotEnough"] = 201] = "MemoryNotEnough";
        CommandExecuteResult[CommandExecuteResult["ParameterError"] = 202] = "ParameterError";
        CommandExecuteResult[CommandExecuteResult["DialogNotFound"] = 203] = "DialogNotFound";
        CommandExecuteResult[CommandExecuteResult["CommandNameIsNull"] = 204] = "CommandNameIsNull";
        CommandExecuteResult[CommandExecuteResult["Other"] = 1000] = "Other";
    })(CommandExecuteResult = SView.CommandExecuteResult || (SView.CommandExecuteResult = {}));
    /**
     * @memberof SView
     * @enum {number}
     * @description 命令执行返回值提示
     * @author nihy
     * @date 2022-08-16 11:09:39
     */
    let CommandExecuteResultTips;
    (function (CommandExecuteResultTips) {
        CommandExecuteResultTips[CommandExecuteResultTips["Failure"] = -1] = "Failure";
        CommandExecuteResultTips[CommandExecuteResultTips["uccess"] = 0] = "uccess";
        CommandExecuteResultTips[CommandExecuteResultTips["NotFoundCommand"] = 1] = "NotFoundCommand";
        CommandExecuteResultTips[CommandExecuteResultTips["CommandNotDefined"] = 2] = "CommandNotDefined";
        CommandExecuteResultTips[CommandExecuteResultTips["CommandDialogNotDefined"] = 3] = "CommandDialogNotDefined";
        CommandExecuteResultTips[CommandExecuteResultTips["CommandNull"] = 4] = "CommandNull";
        CommandExecuteResultTips[CommandExecuteResultTips["DLLMissing"] = 5] = "DLLMissing";
        CommandExecuteResultTips[CommandExecuteResultTips["GetCommandTypeFailed"] = 6] = "GetCommandTypeFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["GetCommandDialogTypeFailed"] = 7] = "GetCommandDialogTypeFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["CreateCommandFailed"] = 8] = "CreateCommandFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["ParameterIsIncorrect"] = 9] = "ParameterIsIncorrect";
        CommandExecuteResultTips[CommandExecuteResultTips["NotFoundParameter"] = 10] = "NotFoundParameter";
        CommandExecuteResultTips[CommandExecuteResultTips["MissingPermission"] = 11] = "MissingPermission";
        CommandExecuteResultTips[CommandExecuteResultTips["IsWaiting"] = 12] = "IsWaiting";
        CommandExecuteResultTips[CommandExecuteResultTips["ModeConflict"] = 13] = "ModeConflict";
        CommandExecuteResultTips[CommandExecuteResultTips["InRunningList"] = 14] = "InRunningList";
        CommandExecuteResultTips[CommandExecuteResultTips["Noninterruptible"] = 15] = "Noninterruptible";
        CommandExecuteResultTips[CommandExecuteResultTips["BlackList"] = 16] = "BlackList";
        CommandExecuteResultTips[CommandExecuteResultTips["NotInRunningList"] = 17] = "NotInRunningList";
        CommandExecuteResultTips[CommandExecuteResultTips["ExecutedFailed"] = 18] = "ExecutedFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["PreviewFailed"] = 19] = "PreviewFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["ApplyFailed"] = 20] = "ApplyFailed";
        CommandExecuteResultTips[CommandExecuteResultTips["ConfigIsNull"] = 21] = "ConfigIsNull";
        CommandExecuteResultTips[CommandExecuteResultTips["ViewIsNull"] = 22] = "ViewIsNull";
        CommandExecuteResultTips[CommandExecuteResultTips["ParametersNotEnough"] = 23] = "ParametersNotEnough";
        CommandExecuteResultTips[CommandExecuteResultTips["NotContainView"] = 24] = "NotContainView";
        CommandExecuteResultTips[CommandExecuteResultTips["MissingConstructor"] = 25] = "MissingConstructor";
        /************************************************ 命令管理异常 ***********************************************/
        CommandExecuteResultTips[CommandExecuteResultTips["LowPriority"] = 100] = "LowPriority";
        CommandExecuteResultTips[CommandExecuteResultTips["Waiting"] = 101] = "Waiting";
        CommandExecuteResultTips[CommandExecuteResultTips["UserChoicing_AbortCurOrContinue"] = 102] = "UserChoicing_AbortCurOrContinue";
        CommandExecuteResultTips[CommandExecuteResultTips["UserChoicing_AbortCurOrWaitingNew"] = 103] = "UserChoicing_AbortCurOrWaitingNew";
        CommandExecuteResultTips[CommandExecuteResultTips["Close"] = 104] = "Close";
        CommandExecuteResultTips[CommandExecuteResultTips["Interrupted"] = 105] = "Interrupted";
        /************************************************ 具体异常 ***********************************************/
        CommandExecuteResultTips[CommandExecuteResultTips["ObjectNull"] = 200] = "ObjectNull";
        CommandExecuteResultTips[CommandExecuteResultTips["MemoryNotEnough"] = 201] = "MemoryNotEnough";
        CommandExecuteResultTips[CommandExecuteResultTips["ParameterError"] = 202] = "ParameterError";
        CommandExecuteResultTips[CommandExecuteResultTips["DialogNotFound"] = 203] = "DialogNotFound";
        CommandExecuteResultTips[CommandExecuteResultTips["CommandNameIsNull"] = 204] = "CommandNameIsNull";
        CommandExecuteResultTips[CommandExecuteResultTips["Other"] = 1000] = "Other";
    })(CommandExecuteResultTips = SView.CommandExecuteResultTips || (SView.CommandExecuteResultTips = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 命令工厂类，负责命令的创建、注册及UI等相关处理
     * @author nihy
     * @date 2022-08-16 16:48:06
     */
    class CommandFactory {
        constructor() {
            //命令配置项列表
            this.command2ConfigMap = new Map();
            //模式对应的配置列表
            this.mode2ConfigMap = new Map();
            //命令和所在模块的集合 key：命令名称 value：所在模块
            this.cmd2ModuleMap = new Map();
            //注册的命令 Map<命令名，类>
            this.registerCommandMap = new Map();
            //注册的命令View Map Map<命令名，类>
            this.registerViewMap = new Map();
            //已创建命令的Map Map<命令名，命令>
            this.createdCommandMap = new Map();
            //已创建命令View的Map Map<命令名，命令>
            this.createdViewMap = new Map();
        }
        /**
         * @instance
         * @description 获取命令工厂类的单例
         * @returns {CommandFactory}
         */
        static getInstance() {
            if (this.factory == null)
                this.factory = new CommandFactory();
            return this.factory;
        }
        /**
         * @public
         * @description 注册命令并添加到命令集合
         * @param {string} commandName 带全命名空间的命令名
         * @param {SCommand} command 命令
         * @return {boolean} 是否注册成功
         */
        registerCommand(commandName, command) {
            if (!M3D.Utility.DecideHelper.isEmpty(commandName)) {
                if (this.registerCommandMap.get(commandName) && this.registerCommandMap.get(commandName).type == command.type) {
                    return false;
                }
                this.registerCommandMap.set(commandName, command);
            }
            else {
                return false;
            }
            return true;
        }
        /**
         * @public
         * @description 注册命令对话框
         * @param {string} dialogName
         * @param {SCommandView} view
         * @returns {boolean} 是否注册成功
         */
        registerView(dialogName, view) {
            if (!M3D.Utility.DecideHelper.isEmpty(dialogName)) {
                if (this.registerViewMap.get(dialogName)) {
                    return false;
                }
                this.registerViewMap.set(dialogName, view);
            }
            else {
                return false;
            }
            return true;
        }
        /**
         * @public
         * @description 创建命令
         * @param {CommandConfig}config 命令配置
         * @param {FrameMain} frame SViewFrame 对象
         * @return {SCommand} 返回创建的command
         */
        createCommand(config, frame) {
            if (config === null) {
                return null;
            }
            let command = null;
            let name = config.getFullClassName();
            if (this.registerCommandMap.get(name)) {
                command = this.registerCommandMap.get(name);
            }
            if (command != null) {
                command.name = config.getName();
                command.frame = frame;
                this.addCommand(command);
            }
            else {
                //输出报错
                SView.FrameLogger.FRAMELOGW("Command create failed");
            }
            return command;
        }
        /**
         * @public
         * @description 创建命令对应的UI
         * @param {CommandConfig} config
         * @returns {SCommandView}
         */
        createView(config) {
            if (config === null) {
                return null;
            }
            let dialog = null;
            if (this.registerViewMap.get(config.getViewName())) {
                dialog = this.registerViewMap.get(config.getViewName());
            }
            if (dialog != null) {
                this.addView(config, dialog);
            }
            else {
                //  输出报错
                SView.FrameLogger.FRAMELOGW("Command view not found");
            }
            return dialog;
        }
        /**
         * @public
         * @description 添加命令对应配置列表
         * @param {string} cmdName 命令名
         * @param {CommandConfigList} list 配置列表
         */
        addCommandConfig(cmdName, list) {
            this.command2ConfigMap.set(cmdName, list);
        }
        /**
         * @public
         * @description 添加命令对应模式配置
         * @param {string} modelName
         * @param {CommandConfigList} list 配置列表
         */
        addModeConfig(modelName, list) {
            this.mode2ConfigMap.set(modelName, list);
        }
        /**
         * @public
         * @description  添加命令对应模块
         * @param  {string} cmdName 命令名
         * @param {SModule} module 命令所属模块
         */
        addModule(cmdName, module) {
            this.cmd2ModuleMap.set(cmdName, module);
        }
        /**
         * @public
         * @description  添加命令
         * @return {void}
         */
        addCommand(command) {
            this.createdCommandMap.set(command.name, command);
        }
        /**
         * @public
         * @description  添加命令对话框
         * @return {void}
         */
        addView(config, dialog) {
            this.createdViewMap.set(config.getViewName(), dialog);
        }
        /**
         *  @public
         * @description  获取命令配置
         * @param {string} commandName
         * @return {CommandConfig} 命令配置
         */
        getCommandConfig(commandName) {
            let commandConfig = null;
            if (!this.cmd2ModuleMap.has(commandName))
                return commandConfig;
            let module = this.cmd2ModuleMap.get(commandName);
            if (module != null) {
                commandConfig = module.getCommandConfig(commandName);
            }
            return commandConfig;
        }
        /**
         * @public
         * @description 获取命名所在模块
         * @param {string} commandName 命令名称
         * @returns {SModule} 命名所在模块
         */
        getModule(commandName) {
            if (!this.cmd2ModuleMap.get(commandName)) {
                return null;
            }
            return this.cmd2ModuleMap.get(commandName);
        }
        /**
         * @public
         * @description 查找对应的命令
         * @param {FrameMain} frame SViewFrame对象
         * @param {string} cmdName 命令名称
         * @param {any} paras 命令的配置项 参数可选
         */
        getCommand(frame, cmdName, paras) {
            let type = null;
            let fullClassName = "";
            if (paras) {
                if (typeof paras === "string") {
                    //TODO
                }
                else if (paras instanceof SView.CommandParameters) {
                    let commandParas = paras;
                    if (commandParas.contains("type")) {
                        type = commandParas.get("type").value;
                    }
                }
                else if (paras instanceof Map) {
                    let commandParasMap = paras;
                    type = commandParasMap.get(SView.CommonStrings.commandType);
                    fullClassName = commandParasMap.get(SView.CommonStrings.fullClassName);
                }
            }
            //1.有指定类型且类型一致则为找到
            //2.没有指定，找到名称对应的为找到
            if ((type != null && this.createdCommandMap.get(cmdName) && this.createdCommandMap.get(cmdName).type == type) ||
                (type == null && this.createdCommandMap.get(cmdName))) {
                return this.createdCommandMap.get(cmdName);
            }
            else {
                //3.配置文件中没有找到当前命令对应的配置，使用默认参数自行组织创建
                let module = this.getModule(cmdName);
                let commandConfig = null;
                if (module === null) {
                    //创建默认的模块存放命令相关配置
                    let module = new SView.SModule();
                    if (M3D.Utility.DecideHelper.isEmpty(fullClassName) == false) {
                        commandConfig = new SView.CommandConfig(fullClassName);
                        commandConfig.setName(cmdName);
                        commandConfig.setViewName(fullClassName + "View");
                        module.addCommandConfig(commandConfig);
                        CommandFactory.getInstance().addModule(commandConfig.getName(), module);
                    }
                    else {
                        SView.CommandException.get("", cmdName + " fullClassName is null");
                    }
                }
                else {
                    commandConfig = module.getCommandConfig(cmdName);
                }
                //得到了命令配置
                if (commandConfig != null) {
                    //创建命令对象
                    let className = commandConfig.getClassName();
                    let fullNameSpace = commandConfig.getFullNameSpace();
                    let windowObj = this.getWindowObj(fullNameSpace);
                    let command = SView.createSViewObject(windowObj, className, cmdName);
                    if (command == null) {
                        //获取类型失败
                        SView.CommandException.get("", cmdName + "creation failed");
                        return null;
                    }
                    //存放命令相关参数
                    if (type != null) {
                        command.type = type;
                    }
                    this.registerCommand(commandConfig.getFullClassName(), command);
                    return this.createCommand(commandConfig, frame);
                }
                return null;
            }
        }
        /**
         * @private
         * @description 获取命令类创建命令
         * @param {string} fullClassName
         * @returns  {SCommand}
         */
        getWindowObj(fullClassName) {
            if (M3D.Utility.DecideHelper.isEmpty(fullClassName)) {
                return null;
            }
            let nameArray = fullClassName.split(".");
            let ret = window[nameArray[0]];
            for (var i = 1; i < nameArray.length; i++) {
                ret = ret[nameArray[i]];
            }
            return ret;
        }
        /**
         * @public
         * @description 获取命令对象的View
         * @param {CommandConfig} config
         * @param {SCommandView} view
         * @returns {Array}
         */
        getView(config, view) {
            if (config === null) {
                view = null;
                return [SView.CommandExecuteResult.ConfigIsNull, view];
            }
            if (config.getViewName() === null) {
                view = null;
                return [SView.CommandExecuteResult.Failure, view];
            }
            if (this.createdViewMap.get(config.getViewName())) {
                view = this.createdViewMap.get(config.getViewName());
                return [SView.CommandExecuteResult.Success, view];
            }
            else {
                //已经读取过 不再重复提示
                if (this.registerViewMap.get(config.getViewName()) && this.registerViewMap.get(config.getViewName()) === null) {
                    view = null;
                    return [SView.CommandExecuteResult.Failure, view];
                }
                if (config.getViewName() === null || config.getViewName() === undefined || config.getViewName() === "") {
                    //命令对话框没有定义
                    view = null;
                    return [SView.CommandExecuteResult.CommandDialogNotDefined, view];
                }
                //TODO
                let module = this.getModule(config.getName());
                if (module === null) {
                    //命令没有定义
                    //view = null;
                    //return [CommandExecuteResult.CommandNotDefined, view];
                }
                let pageType = SView.createSViewObject(this.getWindowObj(config.getViewNameSpace()), config.getViewClassName());
                if (pageType == null) {
                    //暂时将null的情况放入 防止多次弹框提示
                    this.registerViewMap.set(config.getViewName(), pageType);
                    //获取类型失败
                    view = null;
                    return [SView.CommandExecuteResult.GetCommandDialogTypeFailed, view];
                }
                this.registerViewMap.set(config.getViewName(), pageType);
                view = this.createView(config);
            }
            return [SView.CommandExecuteResult.Success, view];
            ;
        }
        /**
         *  @public
        * @beta
         * @description: 判断两个命令是否为黑名单关系
         * @param {string} firstCmdName 第一个命令名
         * @param {string} secondCmdName 第二个命令名
         * @return {boolean}
         */
        isBlackListRelationship(firstCmdName, secondCmdName) {
            return false;
        }
        /**
         * @public
         * @beta
         * @description: 判断两个命令是否为白名单关系
         * @param {string} firstCmdName 第一个命令名
         * @param {string} secondCmdName 第二个命令名
         * @return {boolean}
         */
        isWriteListRelationship(firstCmdName, secondCmdName) {
            return false;
        }
        /**
         * @public
         * @description: 查找命令是否是黑名单除外的命令
         *               非必要不使用接口，可能适用于及特殊的大量命令情形
         * @param {string} cmdName 命令名
         * @param {string} configName 配置名
         * @return {boolean} true 是黑名单除外命令 ;false在黑名单中
         */
        findCommandBlackWithoutConfig(cmdName, configName) {
            return this.command2ConfigMap.get(cmdName).exceptBlackList.indexOf(configName) != -1 ? true : false;
        }
        /**
         * @public
         * @description:  查找命令是否是黑名单中的命令
         * @param {string} cmdName 命令名
         * @param {string} configName 配置名
         * @return {boolean} true 在黑名单中 ;false不在黑名单中
         */
        findCommandBlackConfig(cmdName, configName) {
            return this.command2ConfigMap.get(cmdName).blackList.indexOf(configName) != -1 ? true : false;
        }
        /**
         * @public
        * @description: 查找命令是否是白名单除外的命令
        *               非必要不使用接口，可能适用于及特殊的大量命令情形
        * @param {string} cmdName 命令名
        * @param {string} configName 配置名
        * @return {boolean} true 是黑名单除外命令 ;false在黑名单中
        */
        findCommandWriteWithoutConfig(cmdName, configName) {
            return this.command2ConfigMap.get(cmdName).exceptWhiteList.indexOf(configName) != -1 ? true : false;
        }
        /**
         * @public
         * @description:  查找命令是否是白名单中的命令
         * @param {string} cmdName 命令名
         * @param {string} configName 配置名
         * @return {boolean} true 在黑名单中 ;false不在黑名单中
         */
        findCommandWriteConfig(cmdName, configName) {
            return this.command2ConfigMap.get(cmdName).whiteList.indexOf(configName) != -1 ? true : false;
        }
        /**
         * @description 查找是否有模式黑名单配置
         * @param {string} modeName
         * @param {string} configName
         * @returns {boolean}
         */
        findModeBlackConfig(modeName, configName) {
            if (!this.mode2ConfigMap.get(modeName))
                return false;
            return this.mode2ConfigMap[modeName].blackList.indexOf(configName) != -1 ? true : false;
        }
        /**
         * @beta
         * @description 查找是否有模式黑名单配置
         */
        findModeWhiteConfig() {
            //
        }
    }
    SView.CommandFactory = CommandFactory;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description Command监听事件，在执行回执的过程中能够实现一些额外的操作
     * @author YW
     * @date 2022-2-8
     * @version	1.0
     * @extends {M3D.Utility.Listener}
     */
    class CommandListener extends M3D.Utility.Listener {
        constructor() {
            super(...arguments);
            /**
             * @description 开始监听 onStartListener
             * */
            this.onStart = function () { };
            /**
             * @description 状态改变监听 onStateChangedListener
             * @params state
             * */
            this.onStateChanged = function (state) { };
            /**
             *
             * @description 结束监听 onEndListener
             * */
            this.onEnd = function () { };
            /**
             * @description 监听错误 onErrorListener
             * @params error
             * */
            this.onError = function (error) { };
            /**
             * @description 进度监听 onProgressListener
             * @params progress
             * */
            this.onProgress = function (progress) { };
            /**
             * @description 监听成功 onSuccessListener
             * @params {SCommand} command
             * */
            this.onSuccess = function (command) { };
        }
    }
    SView.CommandListener = CommandListener;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 命令管理类
     * @author YW
     * @date  2022-08-03 19:54:37
     * @implements {M3D.Utility.Publisher}
     */
    class CommandManager {
        /**
         * @constructor
         * @description 构造函数
         * */
        constructor(frame) {
            //Command工厂类
            this.cmdFactory = null;
            // TODO 需要保证插入顺序
            //所有已创建的Command key: string(command 的name), command: SCommand
            //private commandQueue = new M3D.Utility.Queue<SCommand>();
            //正在运行的命令栈
            this.runningCommandStack = new M3D.Utility.Stack();
            //正在执行的Command的队列
            this.activeCommandQueue = new M3D.Utility.Queue();
            //等待执行的Command的队列
            this.waitingCommandQueue = new M3D.Utility.Queue();
            //历史Command
            this.historyCommandList = [];
            //正在执行的命令
            this.executingCommand = null;
            this.frame = frame;
            this.cmdFactory = SView.CommandFactory.getInstance();
            this.historyCommandList = [];
        }
        /**
         * @public
         * @description 初始化数据
         * @beta
         */
        initData() {
            this.registerCommandEventHandler();
            // TODO 注册命令事件
        }
        /**
         * @private
         * @description 注册命令事件
         */
        registerCommandEventHandler() {
            this.frame.addEventHandler(this.commandEventHandle);
        }
        /**
         * @private
         * @description 取消注册命令事件
         */
        unregisterCommandEventHandler() {
            this.frame.removeEventHandler(this.commandEventHandle);
        }
        /**
         * @private
         * @description 接受命令状态事件 并处理
         * @param {CommandManager} commandManager 命令管理对象
         * @param {SCommand} sCommand 命令
         * @param {commandEventArgs} commandEventArgs 命令事件
         * @param {commandEventArgs} commandEventArgs 命令事件参数
         */
        commandEventHandle(commandManager, sCommand, commandEvent, commandEventArgs) {
            //命令后续处理：关闭 循环编辑
            //结束执行 在事件中执行 
            if (commandEvent == SView.CommandEvent.Started) {
                commandManager.doAfterStarted(sCommand);
            }
            else if (commandEvent == SView.CommandEvent.Executing) {
                //
            }
            else if (commandEvent == SView.CommandEvent.Executed) {
                commandManager.doAfterExecuted(sCommand);
            }
            else if (commandEvent == SView.CommandEvent.Cancelling) {
                //TODO
            }
            else if (commandEvent == SView.CommandEvent.Cancelled) {
                commandManager.doAfterCancelled(sCommand);
            }
            else if (commandEvent == SView.CommandEvent.Closed) {
                commandManager.doAfterClosed(sCommand);
            }
            else if (commandEvent == SView.CommandEvent.Interrupted) {
                commandManager.doAfterInterrupted(sCommand);
            }
            else if (commandEvent == SView.CommandEvent.Exception) {
                let exception = commandEventArgs.args;
                //CommandException.Throw(exception);
            }
            else if (commandEvent == SView.CommandEvent.WaitingInput) {
                //可以执行其他等待命令
                commandManager.doAfterIdle(sCommand);
            }
        }
        /**
         * @private
         * @description 命令开始之后执行
         * @param {SCommand}  cmd  命令
         */
        doAfterStarted(cmd) {
            if (cmd.getIsDisplayView()) {
                //SApplication.Instance.ShowPanel(cmd, true);
            }
            if (!this.activeCommandQueue.contains(cmd))
                this.activeCommandQueue.put(cmd);
        }
        /**
         * @private
         * @description 取消命令后执行的操作
         * @param {SCommand}  cmd  命令
         */
        doAfterCancelled(cmd) {
            /*为了解决相邻嵌套、等待运行套命令都有对话框的问题，将面板隐藏的代码移植到这里 判断相邻的命令是否有对话框*/
            //if (cmd == null)
            //CommandException.Throw(CommandExceptionType.parameterIsIncorrect, cmd.getName() + CommonStrings.parameterError);
            if (cmd.getExecutedHandleMode() == SView.ExecutedHandleMode.NextExecute) { //再次执行
                this.runningCommandStack.remove(cmd);
                this.activeCommandQueue.remove(cmd);
                this.clear(cmd);
                this.execute(cmd);
            }
            else if (cmd.getExecutedHandleMode() == SView.ExecutedHandleMode.Finish) { //结束执行
                this.doAfterClosed(cmd);
            }
        }
        /**
         * @private
         * @description 命令中断后执行
         * @param  {SCommand}  cmd  命令
         */
        doAfterInterrupted(cmd) {
            //if (cmd == null)
            //CommandException.Throw(CommandExceptionType.parameterIsIncorrect, cmd.getName() + CommonStrings.parameterError);
            //执行下一个命令
            let nextCommand = this.waitingCommandQueue.take();
            if (nextCommand != null) {
                if (nextCommand instanceof SView.SCommand && !nextCommand.getIsDisplayView())
                    //SApplication.Instance.ShowPanel(nextCommand as SCommand, false);
                    this.execute(nextCommand);
            }
            else if ((nextCommand = this.executingCommand) == null || (nextCommand instanceof SView.SCommand && !nextCommand.getIsDisplayView())) {
                //SApplication.Instance.ShowPanel(nextCommand as SCommand, false);
            }
        }
        /**
         * @private
         * @description 命令关闭后执行的操作
         * @return  {SCommand}  cmd  命令
         */
        doAfterClosed(cmd) {
            /*为了解决相邻嵌套、等待运行套命令都有对话框的问题，将面板隐藏的代码移植到这里*/
            //if (cmd == null)
            //CommandException.Throw(CommandExceptionType.parameterIsIncorrect, cmd.getName() + CommonStrings.parameterError);
            this.runningCommandStack.remove(cmd);
            this.activeCommandQueue.remove(cmd);
            //先从运行队列取命令 恢复运行
            let activeCommand = this.runningCommandStack.pop();
            if (activeCommand != null) {
                if (activeCommand instanceof SView.SCommand && !activeCommand.getIsDisplayView())
                    //SApplication.Instance.ShowPanel(null, false);
                    if (!this.activeCommandQueue.contains(activeCommand)) {
                        activeCommand.resume();
                        this.activeCommandQueue.put(activeCommand);
                    }
                return;
            }
            //执行下一个命令
            let nextCommand = this.waitingCommandQueue.take();
            if (nextCommand != null) {
                if (nextCommand instanceof SView.SCommand && !nextCommand.getIsDisplayView())
                    //SApplication.Instance.ShowPanel(nextCommand as SCommand, false);
                    this.execute(nextCommand);
            }
            else if ((nextCommand = this.executingCommand) == null || (nextCommand instanceof SView.SCommand && !nextCommand.getIsDisplayView())) {
                //SApplication.Instance.ShowPanel(nextCommand as SCommand, false);
            }
        }
        /**
         * @private
         * @description 完成一次命令后执行
         * @return  {SCommand}  cmd  命令
         */
        doAfterExecuted(sCommand) {
            //加入history
            //if (sCommand is IUndo)
            //{
            //    //加入history
            //    let paras: CommandParameters = sCommand.getInputParas();
            //    this.historyCommandList.push(sCommand.getName(), paras, sCommand.getDescription());
            //}
            if (sCommand.getExecutedHandleMode() == SView.ExecutedHandleMode.NextExecute) {
                //再次执行
                this.runningCommandStack.remove(sCommand);
                this.activeCommandQueue.remove(sCommand);
                this.clear(sCommand);
                this.execute(sCommand);
            }
            else if (sCommand.getExecutedHandleMode() == SView.ExecutedHandleMode.Finish) {
                //结束执行
                this.close(sCommand);
            }
            else if (sCommand.getExecutedHandleMode() == SView.ExecutedHandleMode.StartEdit) {
                //进入编辑状态
            }
        }
        /**
         * @private
         * @beta
         * @description 空闲处理
         * @param  {SCommand}  cmd  命令
         */
        doAfterIdle(cmd) {
            //TODO
        }
        /**
         * @public
         * @description 获取Command工厂类
         * @return {CommandFactory}
         */
        getFactory() {
            if (this.cmdFactory === null) {
                this.cmdFactory = SView.CommandFactory.getInstance();
            }
            return this.cmdFactory;
        }
        /**
         * @public
         * @description 设置模块
         * @param {Object} module
         */
        setModule(module) {
            this.module = module;
        }
        /**
         * @private
         * @description 返回是否可执行的状态值
         * @param {SCommand} cmd
         * @return {CommandExecutePermission} 命令执行许可值
         */
        canExecute(cmd) {
            //命令为空
            if (cmd === null) {
                return SView.CommandExecutePermission.commandNull;
            }
            let executePermission = SView.CommandExecutePermission.canExecute;
            // 许可判断 是否有权限
            //if (!SServiceManager.getLicenseService().canExecute()) {
            //    return CommandExecutePermission.noLicense;
            //}
            //模式判断 是否允许
            if (SView.CommandExecutePermission.canExecute != (executePermission = this.checkMode(cmd))) {
                return executePermission;
            }
            //运行队列是否包含该命令
            if (this.activeCommandQueue.contains(cmd)) {
                return SView.CommandExecutePermission.inRunningList;
            }
            // 等待队列是否存在
            if (this.waitingCommandQueue.contains(cmd)) {
                return SView.CommandExecutePermission.inWaitingQueue;
            }
            if ((executePermission = this.checkPriority(cmd)) != SView.CommandExecutePermission.canExecute) {
                return executePermission;
            }
            return executePermission;
        }
        /**
         * @private
         * @description 检测命令的优先级
         * @param {SCommand} cmd
         * @return {CommandExecutePermission} 命令执行许可值
         */
        checkPriority(cmd) {
            //启动命令优先级低于当前正在执行的命令 如果为不可中断 或者添加了黑名单 则不可执行
            let executePermission = SView.CommandExecutePermission.canExecute;
            if (this.activeCommandQueue.size() === 0 || this.activeCommandQueue.front() === null) {
                return executePermission;
            }
            let fristCommand = this.activeCommandQueue.front();
            if (cmd.priority > fristCommand.priority) {
                //启动命令优先级低于当前运行命令
                if (fristCommand.isInterruptible === false) {
                    return SView.CommandExecutePermission.noninterruptible;
                }
                else if (this.cmdFactory.isBlackListRelationship(fristCommand.name, cmd.name)) {
                    return SView.CommandExecutePermission.blackList_PermissionDenied;
                }
                else {
                    return SView.CommandExecutePermission.waitingOperation;
                }
            }
            else if (cmd.priority === fristCommand.priority) {
                //启动命令优先级等于当前运行命令
                if (fristCommand.isInterruptible === false) {
                    return SView.CommandExecutePermission.noninterruptible;
                }
                else if (this.cmdFactory.isWriteListRelationship(fristCommand.name, cmd.name)) {
                    if (fristCommand.getIsCanAbort()) {
                        return SView.CommandExecutePermission.interruptedOperation;
                    }
                    else {
                        return SView.CommandExecutePermission.userChoicingAbortCurOrWaitingNew;
                    }
                }
                else {
                    if (fristCommand.existValidParameters()) {
                        return SView.CommandExecutePermission.userChoicingAbortCurOrContinue;
                    }
                    else {
                        return SView.CommandExecutePermission.replacedOperation;
                    }
                }
            }
            else {
                //启动命令优先级高于当前运行命令
                if (this.cmdFactory.isBlackListRelationship(fristCommand.name, cmd.name)) {
                    return SView.CommandExecutePermission.replacedOperation;
                }
                else if (this.cmdFactory.isWriteListRelationship(fristCommand.name, cmd.name)) {
                    return SView.CommandExecutePermission.parallelOperation;
                }
                else {
                    //判断当前命令是否可以中断
                    if (fristCommand.isCanAbort) {
                        return SView.CommandExecutePermission.interruptedOperation;
                    }
                    else {
                        return SView.CommandExecutePermission.userChoicingAbortCurOrWaitingNew;
                    }
                }
            }
        }
        /**
         * @private
         * @description 检查模式
         * @param {SCommand} cmd
         * @returns  {CommandExecutePermission} 命令执行许可值
         */
        checkMode(cmd) {
            let executePermission = SView.CommandExecutePermission.canExecute;
            let serviceManager = SView.ServiceManager.getInstance();
            serviceManager.setFrame(this.frame);
            for (const service of serviceManager.getServices().values()) {
                if (service.canExecute(cmd) === false) {
                    executePermission = SView.CommandExecutePermission.modeConflict;
                }
            }
            return executePermission;
        }
        /**
         * @private
        * @description 将命令添加到运行列表
        * @param {SCommand} cmd
        * @returns {boolean} 是否添加成功
        */
        addToRunningCommandQueue(cmd) {
            if (!this.runningCommandStack.contains(cmd)) {
                this.runningCommandStack.push(cmd);
            }
            return false;
        }
        /**
         * @private
         * @description 将命令添加到等待列表
         * @param {SCommand} cmd
         */
        addToWaitingCommandQueue(cmd) {
            if (!this.waitingCommandQueue.contains(cmd)) {
                this.waitingCommandQueue.put(cmd);
            }
            return false;
        }
        execute(...args) {
            SView.FrameLogger.FRAMELOGD(this.execute.name + M3D.START);
            let result = SView.CommandExecuteResult.Failure;
            if (args.length === 1) {
                if (typeof args[0] === "string") {
                    return this.executeCommand(this.getCommand(args[0]));
                }
                else if (args[0] instanceof SView.SCommand) {
                    return this.executeCommand(args[0]);
                }
            }
            else if (args.length === 2) {
                if (typeof args[1] === "string") {
                    return this.executeCommand(this.getCommand(args[0], args[1]));
                }
                else if (args[1] instanceof SView.CommandParameters) {
                    return this.executeCommand(this.getCommand(args[0], args[1]));
                }
                else if (args[1] instanceof Map) {
                    return this.executeCommand(this.getCommand(args[0], args[1]));
                }
            }
            else if (args.length === 3) {
                return this.executeCommand(this.getCommand(args[0], args[1], args[2]));
            }
            return result;
        }
        /**
         * @private
         * @description 执行命令的具体逻辑
         * @param {SCommand} cmd  命令对象
         * @returns {CommandExecuteResult}
         */
        executeCommand(cmd) {
            let result = SView.CommandExecuteResult.Failure;
            //执行命令操作
            if (cmd === null) {
                return SView.CommandExecuteResult.Other;
            }
            if ((this.runningCommandStack.contains(cmd) && this.runningCommandStack.pop().type == cmd.type) || this.waitingCommandQueue.contains(cmd)) {
                this.close(cmd);
                return SView.CommandExecuteResult.Close;
            }
            //判断是否可执行
            let canExecute = this.canExecute(cmd);
            if (canExecute === SView.CommandExecutePermission.commandNull) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.noLicense) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.modeConflict) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.inRunningList) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.inWaitingQueue) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.noninterruptible) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.blackList_PermissionDenied) {
                //TODO
            }
            else if (canExecute === SView.CommandExecutePermission.waitingOperation) {
                //放入等待队列
                if (this.addToWaitingCommandQueue(cmd)) {
                    //TODO
                }
                result = SView.CommandExecuteResult.Waiting;
                return result;
            }
            else if (canExecute === SView.CommandExecutePermission.interruptedOperation) {
                //中断执行 将当前运行命令中断，先运行启动的命令
                this.activeCommandQueue.front().interrupt();
                this.activeCommandQueue.take();
                if (this.addWaitingCommand(cmd)) {
                    //TODO
                }
                result = SView.CommandExecuteResult.Interrupted;
                return result;
            }
            else if (canExecute === SView.CommandExecutePermission.replacedOperation) {
                //替换运行
                this.executingCommand = cmd;
                this.close(this.activeCommandQueue.front());
            }
            else if (canExecute === SView.CommandExecutePermission.parallelOperation) {
                //同时运行 并行
                //暂时无需任何操作
            }
            else if (canExecute == SView.CommandExecutePermission.userChoicingAbortCurOrContinue) {
                //TODO 需要弹出让用户选择的对话框
                return SView.CommandExecuteResult.UserChoicing_AbortCurOrContinue;
            }
            else if (canExecute == SView.CommandExecutePermission.userChoicingAbortCurOrWaitingNew) {
                //TODO 需要弹出让用户选择的对话框
                return SView.CommandExecuteResult.UserChoicing_AbortCurOrWaitingNew;
            }
            //2 执行   队列管理 异常处理
            //如果有未完成命令，处理上一个命令 根据优先级判断 当前优先级高于其他命令则执行 否则等待
            //放入队列
            if (this.addToRunningCommandQueue(cmd)) {
                //TODO
            }
            result = cmd.execute(true);
            this.executingCommand = null;
            return result;
        }
        /**
         * @public
         * @beta
         * @description 命令回退
         * */
        undo() {
            SView.FrameLogger.FRAMELOGD(this.undo.name + M3D.START);
            let currentCommands = this.getCurrentCommands();
            let index = currentCommands.size() - 1;
            let result = false;
            for (index; index >= 0; index--) {
                let currentCommand = currentCommands[index];
                if (currentCommand.undo()) {
                    result = true;
                }
            }
            if (!result) {
                //历史命令中进行回退，暂不实现
            }
            SView.FrameLogger.FRAMELOGD(this.undo.name + M3D.END);
            return result;
        }
        /**
         * @public
         * @description 关闭指定命令
         * @param {SCommand} cmd  命令对象
         */
        close(cmd) {
            //运行队列包含
            if (this.runningCommandStack.contains(cmd)) {
                cmd.close();
                this.runningCommandStack.remove(cmd);
            }
            if (this.waitingCommandQueue.contains(cmd)) {
                this.waitingCommandQueue.remove(cmd);
            }
            if (this.activeCommandQueue.contains(cmd)) {
                this.activeCommandQueue.remove(cmd);
            }
            // cmd.close();
        }
        /**
         * @public
         * @description 关闭所有命令
         */
        closeAll() {
            //先把运行列表的命令关闭并清空
            let cmd = null;
            while ((cmd = this.runningCommandStack.pop()) != null) {
                this.close(cmd);
            }
        }
        /**
         *  @public
         * @description 清空指定命令
         * @param {SCommand} cmd  命令对象
         */
        clear(cmd) {
            //判断命令在运行列表中，则需要停止该命令
            if (cmd.state === SView.CommandState.Execute) {
                cmd.close();
            }
            cmd.clear();
        }
        /**
         * @public
         * @description 清空所有命令
         */
        clearAll() {
            //清空运行栈
            let cmd = null;
            while ((cmd = this.runningCommandStack.pop()) != null) {
                this.close(cmd);
            }
            //清空执行列表
            this.activeCommandQueue.clear();
            //清空等待列表
            this.waitingCommandQueue.clear();
        }
        getCommand(key, ...args) {
            SView.FrameLogger.FRAMELOGD(this.getCommand.name + M3D.START);
            let command = null;
            try {
                //查找或创建命令
                if (args.length > 0) {
                    //根据信息去创建或查找命令
                    command = this.cmdFactory.getCommand(this.frame, key, args[0]);
                }
                else {
                    command = this.cmdFactory.getCommand(this.frame, key);
                }
                if (command === null) {
                    return command;
                }
                //更新命令配置参数
                if (args.length > 0) {
                    let paras = null;
                    //判断第一个可变参数的类型
                    if (args[0] instanceof Map) {
                        //取命令配置
                        if (args[0].has(SView.CommonStrings.commandParameters)) {
                            //取出命令配置信息
                            let parasValue = args[0].get(SView.CommonStrings.commandParameters);
                            //值为commandPara对象
                            if (parasValue instanceof SView.CommandParameters) {
                                paras = parasValue;
                            }
                            else 
                            //值为string类型的信息
                            if (typeof parasValue === "string") {
                                // paras = this.parseParameters(parasJson);
                            }
                        }
                    }
                    else if (args[0] instanceof SView.CommandParameters) {
                        paras = args[0];
                    }
                    else if (typeof args[0] === "string") {
                        // paras = this.parseParameters(parasJson);
                    }
                    this.upDataCommandParamaters(command, paras);
                }
                //更新描述
                if (args.length === 2) {
                    let description = args[1];
                    //将描述赋给当前命令
                    command.description = description;
                }
            }
            catch (error) {
                SView.FrameLogger.FRAMELOGW(error, this.getCommand.name);
            }
            SView.FrameLogger.FRAMELOGD(this.getCommand.name + M3D.END);
            return command;
        }
        /**
         * @private
         * @description 更新命令参数
         * @param {SCommand} command 命令
         * @param {CommandParameters} paras CommandParameters对象的配置信息
         */
        upDataCommandParamaters(command, paras) {
            if (!paras) {
                return;
            }
            //更新参数
            for (let tag of paras) {
                //如果原有参数列表中有该参数，则更新值，没有则先添加参数对象
                if (command.getInputParas().contains(tag.name)) {
                    command.getInputParas().update(tag.name, tag.value);
                }
                else {
                    command.getInputParas().add(tag);
                }
            }
        }
        /**
         * @public
         * @description 获取指定的等待命令
         * @param {SCommand} cmd 命令
         */
        addWaitingCommand(cmd) {
            if (!this.waitingCommandQueue.contains(cmd)) {
                this.waitingCommandQueue.put(cmd);
                return true;
            }
            return false;
        }
        /**
         * @public
         * @description 获取指定的运行命令
         * @param {string} cmdName 命令名称
         * @returns {SCommand} 命令对象
         */
        getRunningCommand(cmdName) {
            if (!this.runningCommandStack) {
                return null;
            }
            for (let cmd of this.runningCommandStack.value()) {
                if (cmd.name.match(cmdName) != null) {
                    return cmd;
                }
            }
        }
        /**
         * @public
         * @description 获取所有正在运行的命令
         * @returns  {M3D.Utility.Stack<SCommand>}
         * @memberof CommandManager
         */
        getRunningCommands() {
            //FrameLogger.FRAMELOGD(
            //    M3D.Utility.BrowserHelper.getMethodName() + M3D.START
            //);
            //try {
            //    if (!this.runningCommandStack) {
            //        this.runningCommandStack = new M3D.Utility.Stack();
            //        //return null;
            //    }
            //} catch (error) {
            //    FrameLogger.FRAMELOGW(error, M3D.Utility.BrowserHelper.getMethodName());
            //}
            //FrameLogger.FRAMELOGD(
            //    M3D.Utility.BrowserHelper.getMethodName() + M3D.END
            //);
            return this.runningCommandStack;
        }
        /**
         * @public
         * @description  获取指定的等待队列中的命令
         * @param {string} cmdName 命令名称
         * @returns {SCommand}
         */
        getWaitingCommandQueue(cmdName) {
            if (!this.waitingCommandQueue) {
                return null;
            }
            for (let cmd of this.waitingCommandQueue.value()) {
                if (cmd.name.match(cmdName) != null) {
                    return cmd;
                }
            }
        }
        /**
        * @public
        * @description  获取所有等待的命令
        * @returns  {M3D.Utility.Queue<SCommand>}
        */
        getWaitingCommandQueues() {
            return this.waitingCommandQueue;
        }
        /**
         * @public
         * @description  获取已激活命令队列中的指定命令
         * @param {string} cmdName 命令名称
         */
        getActiveCommandQueue(cmdName) {
            if (!this.activeCommandQueue) {
                return null;
            }
            for (let cmd of this.activeCommandQueue.value()) {
                if (cmd.name.match(cmdName) != null) {
                    return cmd;
                }
            }
        }
        /**
         * @public
         * @description 获取正在执行的命令
         * @returns {M3D.Utility.Queue<SCommand>}
         */
        getCurrentCommands() {
            SView.FrameLogger.FRAMELOGD(this.getCurrentCommands.name + M3D.START);
            try {
                if (!this.activeCommandQueue) {
                    this.activeCommandQueue = new M3D.Utility.Queue();
                }
            }
            catch (error) {
                SView.FrameLogger.FRAMELOGW(error, this.getCurrentCommands.name);
                //console.error(error);
            }
            SView.FrameLogger.FRAMELOGD(this.getCurrentCommands.name + M3D.END);
            return this.activeCommandQueue;
        }
        /**
         * @public
         * @description 获取已执行的历史命令
         * */
        getHistoryCommands() {
            SView.FrameLogger.FRAMELOGD(this.getCurrentCommands.name + M3D.START);
            try {
                if (!this.historyCommandList) {
                    this.historyCommandList = new Array();
                }
            }
            catch (error) {
                SView.FrameLogger.FRAMELOGW(error, this.getCurrentCommands.name);
            }
            SView.FrameLogger.FRAMELOGD(this.getCurrentCommands.name + M3D.END);
            return this.historyCommandList;
        }
        /**
         * @public
         * @description 发送命令退出消息
         * @param {SCommand} cmd 命令对象
         */
        sendExitMassage(cmd) {
            let viewMassage = new SView.CommandViewMessage(cmd.getName(), SView.CommandMessageState.ExitView);
            this.send(viewMassage);
        }
        /**
         * @private
         * @description  发送即时消息
         * @param {M3D.Utility.M3DMessage} msg
         */
        send(msg) {
            // 
            M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
        }
        /**
         * @private
         * @beta
         * @description  发送即时消息
         * @param {M3D.Utility.M3DMessage} msg
         */
        post(msg) {
            //
        }
    }
    SView.CommandManager = CommandManager;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description  命令的相关参数对象
     * @author wz
     * @date  2022-08-15 16:12:00
     * @implements {M3D.Utility.Publisher}
     */
    class CommandParameter {
        constructor(...args) {
            //参数是否可被重写
            this.isOverride = true;
            if (args.length === 4) {
                this.name = args[0];
                this.type = args[1];
                this.ioType = args[2];
                this.value = args[3];
            }
            if (args.length === 5) {
                this.name = args[0];
                this.type = args[1];
                this.ioType = args[2];
                this.value = args[3];
                this.isOverride = args[4];
            }
        }
        /**
         * @public
         * @description 根据配置对象生成配置JSON字符串
         * @returns  {string}
         */
        toString() {
            return this.value.toString();
        }
        /**
         * @public
         * @description 根据配置内容字符串生成配置对象
         * @param {string} value 配置内容字符
         * @returns {CommandParameter} 配置对象
         */
        fromString(value) {
            let isJson = M3D.Utility.DecideHelper.isJson(value);
            if (!isJson) {
                return;
            }
            let ret = new CommandParameter();
            return ret;
        }
        /**
         * @public
         * @description 判断Value是否有效
         * @returns  {boolean}
         */
        hasValue() {
            if (this.value == null)
                return false;
            if (typeof (this.value) == "string" && this.value == "")
                return false;
            return true;
        }
    }
    SView.CommandParameter = CommandParameter;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 命令配置参数类型
     * @enum {number}
     */
    let CommandParameterType;
    (function (CommandParameterType) {
        CommandParameterType[CommandParameterType["UNKNOW"] = 0] = "UNKNOW";
        CommandParameterType[CommandParameterType["INT"] = 1] = "INT";
        CommandParameterType[CommandParameterType["LONG"] = 2] = "LONG";
        CommandParameterType[CommandParameterType["FLOAT"] = 3] = "FLOAT";
        CommandParameterType[CommandParameterType["DOUBLE"] = 4] = "DOUBLE";
        CommandParameterType[CommandParameterType["STRING"] = 5] = "STRING";
        CommandParameterType[CommandParameterType["BOOL"] = 6] = "BOOL";
        CommandParameterType[CommandParameterType["SMODEL"] = 7] = "SMODEL";
        CommandParameterType[CommandParameterType["COLOR"] = 8] = "COLOR";
        CommandParameterType[CommandParameterType["VECTOR3"] = 9] = "VECTOR3";
        CommandParameterType[CommandParameterType["VECTOR2"] = 10] = "VECTOR2";
        CommandParameterType[CommandParameterType["ARRAY"] = 11] = "ARRAY";
        CommandParameterType[CommandParameterType["OBJECT"] = 12] = "OBJECT";
    })(CommandParameterType = SView.CommandParameterType || (SView.CommandParameterType = {}));
    /**
     * @memberof SView
     * @description 命令输入输出配置参数类型
     * @enum {number}
     */
    let IOParameterType;
    (function (IOParameterType) {
        IOParameterType[IOParameterType["UNKNOW"] = 0] = "UNKNOW";
        IOParameterType[IOParameterType["INPUT_PARAM"] = 1] = "INPUT_PARAM";
        IOParameterType[IOParameterType["OUTPUT_PARAM"] = 2] = "OUTPUT_PARAM";
        IOParameterType[IOParameterType["INPUT_OUTPUT_PARAM"] = 3] = "INPUT_OUTPUT_PARAM"; //输出输出参数
    })(IOParameterType = SView.IOParameterType || (SView.IOParameterType = {}));
    /**
     * @memberof SView
     * @description 命令参数基类
     * @author nihy
     * @date 2022-08-15 16:20:23
     * @extends {Array<CommandParameter>}
     */
    class CommandParameters extends Array {
        // 配置项的深度拷贝 防止命令配置中的参数被覆盖
        clone() {
            let paramters = new CommandParameters();
            for (let parm of this) {
                let newParm = new SView.CommandParameter(parm.name, parm.type, parm.ioType, parm.value, parm.isOverride);
                paramters.push(newParm);
            }
            return paramters;
        }
        /**
         * @public
         * @description 获取指定名称的命令配置项
         * @param {string} strName
         * @returns {CommandParameter}
         */
        get(strName) {
            let ret = null;
            for (let i = 0; i < this.length; i++) {
                if (this[i].name == strName) {
                    ret = this[i];
                    break;
                }
            }
            return ret;
        }
        /**
         * @public
         * @description 是否包含此命令参数
         * @param {string} strName
         * @returns {boolean}
         */
        contains(strName) {
            let bRet = false;
            for (let i = 0; i < this.length; i++) {
                if (this[i].name == strName) {
                    bRet = true;
                    break;
                }
            }
            return bRet;
        }
        add(...args) {
            SView.FrameLogger.FRAMELOGD(this.add.name + M3D.START);
            let para = null;
            let name;
            if (args.length === 1) {
                //只传递了CommandParameter 对象
                para = args[0];
                name = para.name;
            }
            else if (args.length === 2) {
                if (typeof args[1] === "boolean") {
                    //传递的CommandParameter 对象，且指定了是否可被重写
                    para = args[0];
                    name = para.name;
                }
                else {
                    //只传递了配置参数的名称和类型
                    para = new SView.CommandParameter();
                    name = args[1];
                    para.name = name;
                }
            }
            else {
                para = new SView.CommandParameter();
                name = args[0];
                para.name = name;
                para.type = args[1];
                if (args.length === 3) {
                    para.value = args[2];
                }
                if (args.length === 4) {
                    para.value = args[2];
                    para.ioType = args[3];
                }
                if (args.length === 5) {
                    para.value = args[2];
                    para.ioType = args[3];
                    para.isOverride = args[4];
                }
            }
            let contain = this.contains(name);
            //不存在直接放入
            if (!contain) {
                this.push(para);
                SView.FrameLogger.FRAMELOGD(SView.PARAMETEREXIST, SView.COMMANDMODULENAME, SView.NULLSTRING);
            }
            //存在
            else {
                let originalPara = this.get(name);
                if (!originalPara.isOverride) {
                    //不允许被重写
                    SView.FrameLogger.FRAMELOGD(SView.PARAMETEROVERIDEFAIL, SView.COMMANDMODULENAME, SView.NULLSTRING);
                    return false;
                }
                else {
                    //允许被重写
                    originalPara.type = para.type;
                    originalPara.ioType = para.ioType;
                    originalPara.value = para.value;
                    originalPara.isOverride = para.isOverride;
                    SView.FrameLogger.FRAMELOGD(SView.PARAMETEROVERIDESUCCESS, SView.COMMANDMODULENAME, SView.NULLSTRING);
                }
            }
            SView.FrameLogger.FRAMELOGD(this.add.name + M3D.END);
            return true;
        }
        /**
         *  @public
         * @description 移除指定参数
         * @param {string} paraName 参数名称
         */
        remove(paraName) {
            if (!this.contains(paraName)) {
                return;
            }
            let targePara = null;
            for (let para of this) {
                if (para.name.match(paraName) != null) {
                    targePara = para;
                    break;
                }
            }
            if (targePara != null) {
                this.splice(this.indexOf(targePara), 1);
            }
        }
        /**
         * @public
         * @description 更新参数值
         * @param {string} name  参数名称
         * @param {*} value 参数内容
         * @returns  {boolean} 是否更新成功
         */
        update(name, value) {
            let bRet = false;
            let para = this.get(name);
            const patt = /^\d+\.\d+$/;
            if (para == null)
                return bRet;
            if (para.type === CommandParameterType.BOOL && (typeof (value) != "boolean") ||
                para.type === CommandParameterType.COLOR && !(value instanceof M3D.M3DMath.Color) ||
                para.type === CommandParameterType.ARRAY && !(value instanceof Array) ||
                (para.type === CommandParameterType.INT
                    || para.type === CommandParameterType.FLOAT
                    || para.type === CommandParameterType.LONG
                    || para.type === CommandParameterType.DOUBLE) && (typeof (value) != "number") ||
                para.type === CommandParameterType.STRING && (typeof (value) != "string") ||
                para.type === CommandParameterType.VECTOR2 && !(value instanceof M3D.M3DMath.Color) ||
                para.type === CommandParameterType.VECTOR3 && !(value instanceof M3D.M3DMath.Color) ||
                para.type === CommandParameterType.SMODEL && !(value instanceof M3D.M3DMath.Color)) {
                return bRet;
            }
            para.value = value;
            return true;
        }
    }
    SView.CommandParameters = CommandParameters;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
       * @memberof SView
       * @enum {number}
       * @description 命令优先级
       * @author nihy
       * @date 2022-08-16 11:09:39
       */
    let CommandPriority;
    (function (CommandPriority) {
        CommandPriority[CommandPriority["Priority_0"] = 0] = "Priority_0";
        CommandPriority[CommandPriority["Priority_1"] = 1] = "Priority_1";
        CommandPriority[CommandPriority["Priority_3"] = 3] = "Priority_3";
        CommandPriority[CommandPriority["Priority_5"] = 5] = "Priority_5";
        CommandPriority[CommandPriority["Priority_7"] = 7] = "Priority_7";
        CommandPriority[CommandPriority["Priority_9"] = 9] = "Priority_9";
    })(CommandPriority = SView.CommandPriority || (SView.CommandPriority = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 命令现场  中断和恢复时使用
     * @author wz
     * @date 2022-08-22 17:41:36
     */
    class CommandScene {
        constructor() {
            // 当前命令执行状态
            this.commandState = SView.CommandState.Initializing;
            // 现场数据
            this.commandData = null;
        }
        /**
         * @public
         * @param {object} value
         * @description 设置状态
         */
        set state(value) {
            this.commandState = value;
        }
        /**
         * @public
         * @description 获取状态
         * @returns {CommandState}
         */
        get state() {
            return this.commandState;
        }
        /**
         * @public
         * @param {object} value
         * @description 设置命令内容
         */
        set data(value) {
            this.commandData = value;
        }
        /**
         * @public
         * @description  获取命令内容
         * @returns {object}
         */
        get data() {
            return this.commandData;
        }
    }
    SView.CommandScene = CommandScene;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
       * @memberof SView
       * @enum {number}
       * @description 命令状态
       * @author nihy
       * @date 2022-08-10 08:41:26
       */
    let CommandState;
    (function (CommandState) {
        CommandState[CommandState["Wait"] = 0] = "Wait";
        CommandState[CommandState["Initializing"] = 1] = "Initializing";
        CommandState[CommandState["ViewInitializing"] = 2] = "ViewInitializing";
        CommandState[CommandState["PreExecute"] = 3] = "PreExecute";
        CommandState[CommandState["Execute"] = 4] = "Execute";
        CommandState[CommandState["Executed"] = 5] = "Executed";
        CommandState[CommandState["Cancelling"] = 6] = "Cancelling";
        CommandState[CommandState["Cancelled"] = 7] = "Cancelled";
        CommandState[CommandState["Closing"] = 8] = "Closing";
        CommandState[CommandState["Closed"] = 9] = "Closed";
        CommandState[CommandState["WaitingInput"] = 10] = "WaitingInput";
        CommandState[CommandState["Interrupted"] = 11] = "Interrupted";
        CommandState[CommandState["Resumed"] = 12] = "Resumed";
        CommandState[CommandState["Preview"] = 13] = "Preview";
        CommandState[CommandState["Apply"] = 14] = "Apply"; //应用
    })(CommandState = SView.CommandState || (SView.CommandState = {}));
})(SView || (SView = {}));
/// <reference path="../FrameLogger.ts" />
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description Command中EVent监听，
     * @author YW
     * @date 2022-1-20
     * @extends {SView.EventListener}
     */
    class CommandTouchListener extends SView.EventListener {
        /**
         * @public
         * @description 构造函数
         * @param {FrameMain} sviewFrame
         */
        constructor(sviewFrame) {
            super();
            this.sviewFrame = sviewFrame;
            this.mouseDownListener = this.onTouchHandle;
            this.mouseMoveListener = this.onTouchHandle;
            this.mouseWheelListener = this.onTouchHandle;
            this.mouseUpListener = this.onTouchHandle;
            this.keyDownListener = this.onTouchHandle;
            this.keyUpListener = this.onTouchHandle;
        }
        ///**
        // * @description 鼠标或触摸按下事件
        // * @param event
        // */
        //private onMouseDown (event: MotionEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        ///**
        // * @description 鼠标或触摸移动事件
        // * @param event
        // */
        //private onMouseMove(event: MotionEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        ///**
        // * @description 鼠标滚轮事件
        // * @param event
        // */
        //private onMouseWheel(event: MotionEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        ///**
        // * @description 鼠标或触摸抬起事件
        // * @param event
        // */
        //private onMouseUp(event: MotionEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        ///**
        // * @description 键盘按下事件
        // * @param event
        // */
        //private onKeyDown(event: KeyEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        ///**
        // * @description 键盘抬起事件
        // * @param event
        // */
        //private onKeyUp(event: KeyEvent): boolean {
        //    return this.sviewFrame.getCommandsManager().onTouchHandle(event);
        //}
        /**
         * @private
         * @description 鼠标键盘操作事件
         * @param event 鼠标事件|触摸事件|关键事件
         */
        onTouchHandle(event) {
            SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.START);
            let result = true;
            try {
                let currentCommands = this.sviewFrame.getCommandsManager().getCurrentCommands().value();
                //判断是否有正在执行的Command
                if (currentCommands) {
                    //遍历正在执行的Command
                    for (let i = 0; i < currentCommands.length; i++) {
                        let command = currentCommands[i];
                        //执行Command的鼠标操作事件，并判断是否阻断后续传递
                        if (command.view != null) {
                            if (command.view.onMouseEvent && (event instanceof M3D.Inputs.MouseEventArgs)) {
                                if (command.frame != null) {
                                    let sender = command.frame.getViewer().getTouchViewport(event).getInput();
                                    result = command.view.onMouseEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                    // result = false;
                                    //if (!command.touchHandle(event)) {
                                    //    result = false;
                                    //}else {
                                    //    FrameLogger.FRAMELOGD(command.name + " is not implement interface touchHandle");
                                    //}
                                }
                            }
                            if (command.view.onKeyEvent && event instanceof M3D.Inputs.KeyEventArgs) {
                                if (command.frame != null) {
                                    let sender = command.frame.getViewer().getTouchViewport(event).getInput();
                                    result = command.view.onKeyEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                            if (command.view.onTouchEvent && event instanceof M3D.Inputs.TouchEventArgs) {
                                if (command.frame != null) {
                                    let sender = command.frame.getViewer().getTouchViewport(event).getInput();
                                    result = command.view.onTouchEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    SView.FrameLogger.FRAMELOGW("Commands has not excuting");
                }
            }
            catch (error) {
                SView.FrameLogger.FRAMELOGW(error, this.onTouchHandle.name);
            }
            SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.END);
            return result;
        }
    }
    SView.CommandTouchListener = CommandTouchListener;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     *  @memberof SView
     * @description 消息类型
     * @enum {number}
     */
    let CommandMessageType;
    (function (CommandMessageType) {
        CommandMessageType["CommandView"] = "view";
    })(CommandMessageType = SView.CommandMessageType || (SView.CommandMessageType = {}));
    /**
     *  @memberof SView
     * @description 消息状态
     * @enum {number}
     */
    let CommandMessageState;
    (function (CommandMessageState) {
        CommandMessageState["ExitView"] = "exit";
    })(CommandMessageState = SView.CommandMessageState || (SView.CommandMessageState = {}));
    /**
     *  @memberof SView
     * @description CommandViewMessage
     * @author nihy
     * @date 31/03/2023
     * @extends {M3D.Utility.M3DMessage}
     */
    class CommandViewMessage extends M3D.Utility.M3DMessage {
        constructor(commandName, state) {
            super(commandName, state);
        }
    }
    SView.CommandViewMessage = CommandViewMessage;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @hidden
    * @description 命令状态
    * @author nihy
    * @date 2022-08-10 08:41:26
    */
    class CommonStrings {
    }
    CommonStrings.serviceAddress = "http://service.sv3d.cn";
    CommonStrings.nullString = "";
    CommonStrings.space = " ";
    CommonStrings.ID = "ID";
    CommonStrings.SViewFrame = "SViewFrame";
    CommonStrings.end = "End";
    CommonStrings.begin = "Begin";
    CommonStrings.false = "False";
    CommonStrings.true = "True";
    CommonStrings.regularScript = "楷书";
    CommonStrings.commandBegin = ":command begin";
    CommonStrings.commandObjectNull = "command object null";
    CommonStrings.missingLicense = ":missing license";
    CommonStrings.conflictWithMode = ":conflicts with mode";
    CommonStrings.conflictWithAnimationPlayMode = ":conflicts with animation play mode";
    CommonStrings.conflictWithAnimationRecordMode = ":conflicts with animation record mode";
    CommonStrings.conflictWithMotionCollisionMode = ":conflicts with motion collision mode";
    CommonStrings.conflictWithBrowseMode = ":conflicts with browse mode";
    CommonStrings.conflictWithRoamMode = ":conflicts with roam mode";
    CommonStrings.inRunningList = ":in running list";
    CommonStrings.inWaitingQueue = ":in waiting queue";
    CommonStrings.noninterruptible = ":noninterruptible";
    CommonStrings.inBlackList = ":in black list, permission denied";
    CommonStrings.addCommand = "add command:";
    CommonStrings.commad = "command ";
    CommonStrings.toList = " to list";
    CommonStrings.priorityIsLow = " priority is low";
    CommonStrings.beforeExecute = ":before execute";
    CommonStrings.afterExecute = ":after execute";
    CommonStrings.commandEnd = ":command end";
    CommonStrings.colon = ":";
    CommonStrings.comma = ",";
    CommonStrings.getCommand = "get command:";
    CommonStrings.successfully = " successfully";
    CommonStrings.parameterError = ":parameter error";
    CommonStrings.commandClosed = ":command closed";
    CommonStrings.stopCommand = "stop command: ";
    CommonStrings.notInRunningList = "not in running list";
    CommonStrings.stopAllCommands = "stop all commands";
    CommonStrings.clearCommand = "clear command";
    CommonStrings.addCommandIntoWaitingQueue = "add command into waiting queue:";
    CommonStrings.addCommandIntoExecutingQueue = "add command into executing queue:";
    CommonStrings.executeBegin = " execute begin";
    CommonStrings.executeEnd = " execute end";
    CommonStrings.doExecuteBegin = "SCommand DoExecute begin,cmd name ";
    CommonStrings.doPreviewBegin = "SCommand DoPreview begin,cmd name ";
    CommonStrings.doApplyBegin = "SCommand DoApply begin,cmd name ";
    CommonStrings.asynDoExecuteBegin = "SAsynCommand DoExecute begin,cmd name ";
    CommonStrings.doExecuteEnd = "SCommand DoExecute end,cmd name ";
    CommonStrings.doPreviewEnd = "SCommand DoPreview end,cmd name ";
    CommonStrings.doApplyEnd = "SCommand DoApply end,cmd name ";
    CommonStrings.asynDoExecuteEnd = "SAsynCommand DoExecute end,cmd name ";
    CommonStrings.asynBeforeWait = "SAsynCommand DoAction  before wait,cmd name ";
    CommonStrings.asynAfterWait = "SAsynCommand DoAction  after wait,cmd name ";
    CommonStrings.previewExecuting = "Preview executing command name ";
    CommonStrings.onInitializeBegin = "OnInitialize begin,command name ";
    CommonStrings.onInitializeIntoEditState = "OnInitialize into edit state,command name ";
    CommonStrings.onExecuteExecuting = "OnExecute executing command name ";
    CommonStrings.onPreviewExecuting = "OnPreview executing command name";
    CommonStrings.onApplyExecuting = "OnApply executing command name";
    CommonStrings.onExecutingExcuting = "OnExecuting executing command name ";
    CommonStrings.onExecutedExcuting = "OnExecuted executing command name ";
    CommonStrings.onInterruptExcuting = "OnInterrupt executing command name ";
    CommonStrings.onInterruptExcuted = "OnInterrupt executed command name ";
    CommonStrings.onResumeExecuting = "OnResume executing command name ";
    CommonStrings.onStartEditExecuting = "OnStartEdit executing command name ";
    CommonStrings.onEndEditExecuting = "OnEndEdit executing command name ";
    CommonStrings.onCloseExecuting = "OnClose executing command name ";
    CommonStrings.loadParasFromObjectExecuting = "LoadParasFromObject executing command name ";
    CommonStrings.closeBegin = "Close begin command name ";
    CommonStrings.closeEnd = "Close end command name ";
    CommonStrings.cancelBegin = "Cancel begin command name ";
    CommonStrings.cancelEnd = "Cancel end command name ";
    CommonStrings.queueNotFoundCmdName = "queue not found cmd name: ";
    CommonStrings.stackNotFoundCmdName = "stack not found cmd name: ";
    CommonStrings.getConfigFailed = "get config failed";
    CommonStrings.getModuleFailed = "get module failed";
    CommonStrings.getAssemblyOfModuleFailed = "get assembly of module failed!";
    CommonStrings.getTypeOfModuleFailed = "get type of module failed!";
    CommonStrings.instantiationCommandFailed = "instantiation command failed!";
    CommonStrings.getCommandConfigFailed = "get command config failed!";
    CommonStrings.licenseCheckLibDll = "LicenseCheckLib.dll";
    CommonStrings.SViewLicenseService = "SView.LicenseService";
    CommonStrings.commandNotDefined = "command not defined!";
    CommonStrings.commandNotFoundConfig = "command not found config!";
    CommonStrings.createCommandFailed = "command created failed!";
    CommonStrings.commandDialogNotDefined = "command dialog not defined!";
    CommonStrings.getTypeOfCommandDialogFailed = "get type of command dialog failed!";
    CommonStrings.getTypeOfCommandFailed = "get type of command failed!";
    CommonStrings.onExecuteSuccessfully = "OnExecute successfully,cmd name ";
    CommonStrings.onPreviewSuccessfully = "OnPreview successfully,cmd name ";
    CommonStrings.onApplySuccessfully = "OnApply successfully,cmd name ";
    CommonStrings.onExecuteFailed = "OnExecute failed,cmd name ";
    CommonStrings.onApplyFailed = "OnApply failed,cmd name  ";
    CommonStrings.onPreviewFailed = "OnPreview failed,cmd name  ";
    CommonStrings.commandConfigNull = "Config null";
    CommonStrings.configureReader = "ConfigureReader";
    CommonStrings.shortcutReader = "SShortcutReader";
    CommonStrings.configureWriter = "ConfigureWriter";
    CommonStrings.shortcutWriter = "ShortcutWriter";
    CommonStrings.fromFileBegin = "FromFile begin";
    CommonStrings.fromFileEnd = "FromFile end";
    CommonStrings.convertException = "Convert error";
    CommonStrings.savedFailed = "Configure file save failed";
    CommonStrings.colorConfigureFormatError = "Color configuration item format error";
    CommonStrings.fillCommandParas = "FillCommandParas";
    CommonStrings.fillControlParas = "FillControlParas";
    CommonStrings.mouseEvent = "MouseEvent";
    CommonStrings.leftButton = "LeftButton";
    CommonStrings.rightButton = "RightButton";
    CommonStrings.middleButton = "MiddleButton";
    CommonStrings.commandViewNotDefined = "";
    CommonStrings.commandExecutedFailure = "command executed failure!";
    CommonStrings.notFoundCommand = "command not founded!";
    CommonStrings.commandNull = "command is null!";
    CommonStrings.getCommandTypeFailed = "command type getted failure!";
    CommonStrings.commandParameterIsIncorrect = "command paramters is incorrect!";
    CommonStrings.commandNotFoundParameter = "command not found parameter!";
    CommonStrings.commandMissingPermission = "command losed permission!";
    CommonStrings.commandIsWaiting = "command is waiting!";
    CommonStrings.commandModeConflict = "command mode conflict!";
    CommonStrings.commandIsRunning = "command is running!";
    CommonStrings.commandNoninterruptible = "command uninterruptible!";
    CommonStrings.commandBlackList = "command is in black list,permission denied!";
    CommonStrings.commandConfigIsNull = "command configure is null!";
    CommonStrings.commandViewIsNull = "command view is null!";
    CommonStrings.commandParametersNotEnough = "command parameters not enough!";
    CommonStrings.commandMissingConstructor = "command missing Constructor!";
    CommonStrings.leftBracket = "[";
    CommonStrings.rightBracket = "]";
    CommonStrings.fullClassName = "fullClassName";
    CommonStrings.commandParameters = "commandParameters";
    CommonStrings.commandType = "commandType";
    SView.CommonStrings = CommonStrings;
})(SView || (SView = {}));
var SView;
(function (SView) {
    // 执行完，是结束命令，还是继续执行 还是进入编辑
    /**
     *  @memberof SView
     * @description  命令执行模式
     * @author wz
     * @date 2022-08-22 15:09:39
     * @export
     * @enum {number}
     */
    let ExecutedHandleMode;
    (function (ExecutedHandleMode) {
        ExecutedHandleMode[ExecutedHandleMode["Finish"] = 0] = "Finish";
        ExecutedHandleMode[ExecutedHandleMode["NextExecute"] = 1] = "NextExecute";
        ExecutedHandleMode[ExecutedHandleMode["StartEdit"] = 2] = "StartEdit"; //进入编辑
    })(ExecutedHandleMode = SView.ExecutedHandleMode || (SView.ExecutedHandleMode = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 鼠标点击次数
     * @author nihy
     * @date 2022-08-18 09:33:04
     * @enum {number} 鼠标点击次数
     */
    let SMouseClickCount;
    (function (SMouseClickCount) {
        SMouseClickCount[SMouseClickCount["MouseClickCount_Init"] = -1] = "MouseClickCount_Init";
        SMouseClickCount[SMouseClickCount["MouseClickCount_0"] = 0] = "MouseClickCount_0";
        SMouseClickCount[SMouseClickCount["MouseClickCount_1"] = 1] = "MouseClickCount_1";
        SMouseClickCount[SMouseClickCount["MouseClickCount_2"] = 2] = "MouseClickCount_2";
    })(SMouseClickCount = SView.SMouseClickCount || (SView.SMouseClickCount = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description Command的基类
     * @author nihy
     * @date 2022-08-10 08:41:26
     * @implements {ICommand}
     */
    class SCommand {
        constructor(...args) {
            this.type = 0; //命令类型
            this.priority = SView.CommandPriority.Priority_7; //命令优先级 ，默认为7级
            this.description = ""; //命令描述
            // 命令的View 用于处理鼠标事件 弹出UI界面 
            this.view = null;
            this.isShowLoadingDialog = false;
            this.isDisplayView = false;
            // 是否可以中止 在命令执行中断时使用
            this.isCanAbort = false;
            this.isParametersComplete = true;
            //命令执行模式 0:结束命令 1:继续执行,2:进入编辑状态
            this.executedHandleMode = SView.ExecutedHandleMode.Finish;
            //命令是否预显 0:不预显 1:预显
            this.isPreView = 1;
            //是否可中断
            this.isInterruptible = true;
            this.outputParas = new SView.CommandParameters();
            // 命令场景数据 用于命令中断和恢复 只在类内使用
            this.commandScene = null;
            // this.view = new SCommandView();
            this.state = SView.CommandState.Initializing;
            this.description = "";
            if (args.length === 1) {
                this.name = args[0];
                this.commandScene = new SView.CommandScene();
                let result = this.getConfigData();
                if (result != SView.CommandExecuteResult.Success) {
                    /*CommandException.Throw(result, "");*/
                }
            }
            else if (args.length === 2) {
                this.frame = args[0];
                this.priority = SView.CommandPriority.Priority_7;
            }
            else if (args.length === 4) {
                this.frame = args[0];
                this.type = args[2];
                this.priority = args[3];
            }
        }
        /**
         * @protected
         * @description 初始化参数
         * @returns {number}
         */
        onInitialize() {
            this.state = SView.CommandState.Initializing;
            let ret = SView.CommandExecuteResult.Failure;
            ret = this.getConfigData();
            if (ret !== SView.CommandExecuteResult.Success) {
                this.close();
                return ret;
            }
            //判断对话框是否正常添加
            let containView = this.isContainView();
            if (containView === SView.CommandExecuteResult.ConfigIsNull) {
                this.onClose();
                return containView;
            }
            else if (containView === 1 && !this.view) {
                this.onClose();
                return SView.CommandExecuteResult.ViewIsNull;
            }
            //编辑状态下执行
            if (this.getInputParas().contains(SView.ID) && this.inputParas.get(SView.ID).value) {
                this.loadParasFromObject();
            }
            else {
                //不能在此处清空参数 可以在子类调用
                //this.resetParameters();
            }
            //TODO
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @private
         * @description 命令是否包含相关的UI界面
         * @returns {number}
         */
        isContainView() {
            let config = SView.CommandFactory.getInstance().getCommandConfig(this.name);
            if (!config) {
                return SView.CommandExecuteResult.ConfigIsNull;
            }
            if (M3D.Utility.DecideHelper.isEmpty(config.getViewName())) {
                return 0;
            }
            else {
                return 1;
            }
        }
        /**
         * @protected
         * @beta
         * @description 通过配置对象加载配置参数
         */
        loadParasFromObject() {
            SView.FrameLogger.FRAMELOGD(SView.LOADPARASFROMOBJECTEXECUTING + this.getName());
        }
        /**
         * @public
         * @description 设置scene
         * @param {M3D.Scene} value
         */
        setScene(value) {
            this.scene = value;
        }
        /**
         * @public
         * @description 获取scene
         * @returns {M3D.Scene}
         */
        getScene() {
            if (this.scene == null) {
                if (this.frame != null) {
                    this.scene = this.frame.getViewer().getScene();
                }
            }
            return this.scene;
        }
        /**
         * @public
         * @description 获取view
         * @returns {SCommandView}
         */
        getView() {
            var _a;
            if (M3D.Utility.DecideHelper.isEmpty(this.getName())) {
                return null;
            }
            if (!this.view) {
                let config = (_a = SView.CommandFactory.getInstance()) === null || _a === void 0 ? void 0 : _a.getCommandConfig(this.getName());
                if (config) {
                    SView.CommandFactory.getInstance().getView(config, this.view);
                    if (this.view) {
                        this.view.command = this;
                    }
                }
            }
            return this.view;
        }
        /**
         * @public
         * @description  设置view
         * @param {SCommandView} value
         */
        setView(value) {
            this.view = value;
        }
        /**
         * @public
         * @description 获取命令名称
         * @returns {string}
         */
        getName() {
            return this.name;
        }
        /**
         * @public
         * @description  设置命令名称
         * @param {string} value
         */
        setName(value) {
            this.name = value;
        }
        /**
         * @public
         * @description 设置输入参数
         * @param {CommandParameters} value
         */
        setInputParas(value) {
            this.inputParas = value;
        }
        /**
         * @public
         * @description 获取参数
         * @returns  {CommandParameters}
         */
        getInputParas() {
            if (this.inputParas == null) {
                let config = SView.CommandFactory.getInstance().getCommandConfig(this.name);
                //初始化参数
                if (config && config.getInputParameters()) {
                    this.inputParas = config.getInputParameters().clone();
                }
                else {
                    this.inputParas = new SView.CommandParameters();
                }
            }
            return this.inputParas;
        }
        /**
         * @public
         * @description 设置输出参数
         * @param {CommandParameters} value
         */
        setOutputParas(value) {
            this.outputParas = value;
        }
        /**
         * @public
         * @description 获取输出参数
         * @returns  {CommandParameters}
         */
        getOutputParas() {
            if (this.outputParas == null) {
                let config = SView.CommandFactory.getInstance().getCommandConfig(this.name);
                //初始化参数
                if (config == null) {
                    this.outputParas = new SView.CommandParameters();
                }
                else {
                    this.outputParas = config.getOutputParameters().clone();
                }
            }
            return this.outputParas;
        }
        /**
         * @public
         * @description 获取命令描述
         * @returns {string}
         */
        getDescription() {
            // TODO 是不是可以直接从命令里获取 ，而不是使用设计器类似的服务里再去数据
            return this.description;
        }
        /**
         * @public
         * @description 设置命令优先级
         * @param {CommandPriority} value
         */
        setPriority(value) {
            this.priority = value;
        }
        /**
         * @public
         * @description 获取命令优先级
         * @returns {CommandPriority}
         */
        getPriority() {
            if (this.priority === SView.CommandPriority.Priority_0) {
                let config = SView.CommandFactory.getInstance().getCommandConfig(this.name);
                this.priority = config.getPriority();
            }
            return this.priority;
        }
        /**
         * @public
         * @description 是否显示加载对话框
         * @returns {boolean}
         */
        getIsShowLoadingDialog() {
            return this.isShowLoadingDialog;
        }
        /**
         * @public
         * @description 设置是否显示加载对话框
         * @param {boolean} value
         */
        setIsShowLoadingDialog(value) {
            this.isShowLoadingDialog = value;
        }
        /**
         * @public
         * @description 是否显示View
         * @returns {boolean}
         */
        getIsDisplayView() {
            return this.isDisplayView;
        }
        /**
         * @public
         * @description 设置是否显示View
         * @param {boolean} value
         */
        setIsDisplayView(value) {
            this.isDisplayView = value;
        }
        /**
         * @public
         * @description 是否可以中止
         * @returns {boolean}
         */
        getIsCanAbort() {
            return this.isCanAbort;
        }
        /**
         * @public
         * @description 设置是否可以中止
         * @param {boolean} value
         */
        setIsCanAbort(value) {
            this.isCanAbort = value;
        }
        /**
         * @public
         * @description 获取命令状态
         * @returns {CommandState}
         */
        getState() {
            return this.state;
        }
        /**
         * @public
         * @description 设置命令状态
         * @param {CommandState} state
         */
        setState(state) {
            this.state = state;
        }
        /**
         * @public
         * @description  判断参数是否完全，仅判断参数是否有空缺,可重写方法，实现自己的判定规则
         * @returns {boolean}
         */
        getIsParametersComplete() {
            let bRet = true;
            //重写该接口 自己判断参数是否完全
            let para = new SView.CommandParameter();
            for (para of this.getInputParas()) {
                if ((!para.value || para.value === null || para.value === "") && para.value != 0) {
                    bRet = false;
                    break;
                }
            }
            return bRet;
        }
        /**
         * @public
         * @description 参数是否完整
         * @param {*} value
         */
        setIsParametersComplete(value) {
            this.isParametersComplete = value;
        }
        /**
         * @public
         * @description 获取命令是否预显
         * @returns {number}
         */
        getIsPreView() {
            return this.isPreView;
        }
        /**
         * @public
        * @description: 设置命令是否预显
        * @param value ：0不预显，1预显
        */
        setIsPreView(value) {
            this.isPreView = value;
        }
        /**
         * @public
         * @description 获取执行模式
         * @returns {*}
         */
        getExecutedHandleMode() {
            return this.executedHandleMode;
        }
        /**
         * @public
         * @description 设置执行模式
         * @param {*} value
         */
        setExecutedHandleMode(value) {
            this.executedHandleMode = value;
        }
        /**
         * @public
         * @description 获取是否可被中断
         * @returns {boolean}
         */
        getIsInterruptible() {
            return this.isInterruptible;
        }
        /**
         * @public
         * @description 设置是否可被中断
         * @param {boolean} value
         */
        setIsInterruptible(value) {
            this.isInterruptible = value;
        }
        /**
         * @public
         * @description 是否存在有效参数
         * @returns {boolean}
         */
        existValidParameters() {
            return false;
        }
        /**
         * @public
         * @description 处理触摸事件
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs | M3D.Inputs.KeyEventArgs)} event
         * @returns {*}
         */
        touchHandle(event) {
            return false;
        }
        /**
         * @public
         * @description 获取配置中的命令信息
         * @returns  {number}
         */
        getConfigData() {
            let result = SView.CommandExecuteResult.Failure;
            if (this.name === null) {
                return SView.CommandExecuteResult.CommandNameIsNull;
            }
            let config = SView.CommandFactory.getInstance().getCommandConfig(this.name);
            if (config != null) {
                this.isInterruptible = config.getIsInterruptible();
                this.executedHandleMode = config.getExecutedHandleMode();
                //this.isPreView = config.getIsPreView();
                if (this.view === null) {
                    let tmpView = null;
                    let ret = SView.CommandFactory.getInstance().getView(config, tmpView);
                    result = ret[0];
                    this.view = ret[1];
                    if (this.view != null) {
                        this.view.command = this;
                    }
                    else {
                        return SView.CommandExecuteResult.Success;
                    }
                }
                else {
                    result = SView.CommandExecuteResult.Success;
                }
            }
            else {
                result = SView.CommandExecuteResult.ConfigIsNull;
            }
            return result;
        }
        /**
         * @protected
         * @description  应用
         * @returns  {number}
         */
        onApply() {
            this.state = SView.CommandState.Apply;
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 应用
         * @returns  {number}
         */
        apply() {
            let result = SView.CommandExecuteResult.Failure;
            if (this.isShowLoadingDialog) {
                //TODO
            }
            result = this.onApply();
            if (result != SView.CommandExecuteResult.Success) {
                //  info = SServiceManager.ResourceService.GetString("ApplyFailed");
                //SViewApp.ThrowCommandException(this, SCommandExceptionType.ApplyFailed, info);
                //SServiceManager.LogService.LogError(SCommonStrings.SViewCore, SCommonStrings.NULLSTRING, SCommonStrings.NULLSTRING, SCommonStrings.OnApplyFailed + Name);
            }
            else {
                // SServiceManager.LogService.LogDebug(SCommonStrings.SViewCore, SCommonStrings.NULLSTRING, SCommonStrings.NULLSTRING, SCommonStrings.OnApplySuccessfully + Name);
            }
            //  SServiceManager.LogService.LogDebug(SCommonStrings.SViewCore, SCommonStrings.NULLSTRING, SCommonStrings.NULLSTRING, SCommonStrings.DoApplyEnd + Name);
            return result;
        }
        /**
         * @protected
         * @description 执行命令逻辑
         * @returns  {number}
         */
        doExecute() {
            let retState = SView.CommandExecuteResult.Failure;
            if (this.state === SView.CommandState.Initializing || this.state === SView.CommandState.ViewInitializing
                || this.state === SView.CommandState.WaitingInput) {
                this.state = SView.CommandState.PreExecute;
                retState = this.onPreExecute();
                //if (this.state == CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
                if (retState != SView.CommandExecuteResult.Success) {
                    this.close();
                    return retState;
                }
                if (this.isShowLoadingDialog)
                    // SViewApp.ShowCommandLoadingDialog(this, true);
                    //子类添加进度逻辑
                    this.state = SView.CommandState.Execute;
                retState = this.onExecute();
                //if (this.state === CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
                if (this.isShowLoadingDialog)
                    //SViewApp.ShowCommandLoadingDialog(this, false);
                    if (retState === SView.CommandExecuteResult.Close
                    /*|| this.state === CommandState.Closed*/ ) {
                        return retState;
                    }
                    else if (retState != SView.CommandExecuteResult.Success) {
                        //SServiceManager.LogService.LogError(SCommonStrings.SViewCore, SCommonStrings.NULLSTRING, SCommonStrings.NULLSTRING, SCommonStrings.OnExecuteFailed + Name);
                        this.onExecuted();
                        return retState;
                    }
                //只有执行OnExecute成功才执行后续流程逻辑
                this.state = SView.CommandState.Executed;
                this.onExecuted();
                //if (this.state === CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
            }
            else if (this.state == SView.CommandState.PreExecute) {
                if (this.isShowLoadingDialog)
                    // SViewApp.ShowCommandLoadingDialog(this, true);
                    //子类添加进度逻辑
                    this.state = SView.CommandState.Execute;
                retState = this.onExecute();
                //if (this.state === CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
                if (this.isShowLoadingDialog)
                    // SViewApp.ShowCommandLoadingDialog(this, false);
                    if (retState == SView.CommandExecuteResult.Close
                    /* || this.state === CommandState.Closed*/ ) {
                        return retState;
                    }
                    else if (retState != SView.CommandExecuteResult.Success) {
                        //  SServiceManager.LogService.LogError(SCommonStrings.SViewCore, SCommonStrings.NULLSTRING, SCommonStrings.NULLSTRING, SCommonStrings.OnExecuteFailed + Name);
                        this.onExecuted();
                        return retState;
                    }
                //只有执行OnExecute成功才执行后续流程逻辑
                this.state = SView.CommandState.Executed;
                this.onExecuted();
                //if (this.state === CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
            }
            else if (this.state === SView.CommandState.Execute) {
                //只有执行OnExecute成功才执行后续流程逻辑
                this.state = SView.CommandState.Executed;
                this.onExecuted();
                //if (this.state === CommandState.Interrupted) {
                //    retState = CommandExecuteResult.Interrupted;
                //    return retState;
                //}
            }
            //TODO
            return retState;
        }
        /**
         * @public
         * @description 执行
         * @param {boolean} [executeAll=false]
         * @returns  {number}
         */
        execute(executeAll = false) {
            let retState = SView.CommandExecuteResult.Failure;
            if (this.state == SView.CommandState.WaitingInput || !executeAll) {
                if (!this.getIsParametersComplete()) {
                    return SView.CommandExecuteResult.ParametersNotEnough;
                }
                retState = this.doExecute();
                return retState;
            }
            this.setState(SView.CommandState.Initializing);
            //触发命令开始事件
            this.frame.fireCommandProcess(this, SView.CommandEvent.Started);
            //初始化状态
            retState = this.onInitialize();
            if (this.state === SView.CommandState.Interrupted) {
                retState = SView.CommandExecuteResult.Interrupted;
                //触发命令中断事件
                this.frame.fireCommandProcess(this, SView.CommandEvent.Interrupted);
                /*SViewApp.FireCommandProcess(this, SCommandEvent.Interrupted);*/
                return retState;
            }
            else if (retState != SView.CommandExecuteResult.Success) {
                this.close();
                return retState;
            }
            //View初始化
            if (this.view != null) {
                this.view.command = this;
                retState = this.view.init();
            }
            //if (this.state == CommandState.Interrupted) {
            //    retState = CommandExecuteResult.Interrupted;
            //    return retState;
            //}
            if (retState != SView.CommandExecuteResult.Success) {
                this.close();
                return retState;
            }
            //打开了对话框 需要等待对话框响应
            if (this.isWaiting()) {
                //等待参数输入
                this.frame.fireCommandProcess(this, SView.CommandEvent.WaitingInput);
                this.setState(SView.CommandState.WaitingInput);
                retState = SView.CommandExecuteResult.Waiting;
            }
            else {
                if (!this.getIsParametersComplete()) {
                    this.state = SView.CommandState.WaitingInput; //需要搜集参数
                    this.close();
                    return SView.CommandExecuteResult.ParametersNotEnough;
                }
                retState = this.doExecute();
            }
            return retState;
        }
        /**
         * @protected
         * @description 执行业务逻辑前代码
         * @returns {number}
         */
        onPreExecute() {
            this.state = SView.CommandState.PreExecute;
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @protected
         * @description 执行命令业务逻辑
         * @returns {number}
         */
        onExecute() {
            this.state = SView.CommandState.Execute;
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 业务逻辑执行完毕后
         * @returns {number}
         */
        onExecuted() {
            this.setState(SView.CommandState.Executed);
            this.frame.fireCommandProcess(this, SView.CommandEvent.Executed);
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @protected
         * @description  中断命令前执行 \r\n 对于一般命令 都是执行一个action，中断前需要完成action；对于异步执行的命令或者流程性操作 需要用户处理中断和恢复接口
         * @returns  {number}
         */
        onInterrupt() {
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 中断当前命令
         * @returns {CommandExecuteResult}
         */
        interrupt() {
            let retState = SView.CommandExecuteResult.Success;
            this.commandScene.state = this.state;
            this.state = SView.CommandState.Interrupted;
            SView.FrameLogger.FRAMELOGD(SView.ONINTERRUPTEXCUTING + this.name, SView.COMMANDMODULENAME, SView.NULLSTRING);
            if (this.view) {
                this.view.interrupt();
            }
            //执行中断操作
            this.onInterrupt();
            //删除鼠标事件
            if (this.commandScene.state === SView.CommandState.Initializing) {
                if (this.view && this.view instanceof SView.SCommandView) {
                    this.view.removeCommandTouchHandle();
                }
            }
            switch (this.commandScene.state) {
                case SView.CommandState.Initializing:
                    {
                        //TODO
                    }
                    break;
                case SView.CommandState.WaitingInput:
                    {
                        if (this.isShowView()) {
                            //this.frame.ShowPanel(this, false);
                        }
                    }
                    break;
                case SView.CommandState.PreExecute:
                case SView.CommandState.Execute:
                case SView.CommandState.Executed:
                    {
                        //TODO
                    }
                    break;
            }
            SView.FrameLogger.FRAMELOGD(SView.ONINTERRUPTEXCUTED + this.name, SView.COMMANDMODULENAME, SView.NULLSTRING);
            return retState;
        }
        /**
         * @protected
         * @description 结束中断后执行
         * @returns {number}
         */
        onResume() {
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 恢复命名
         * @returns {number}
         */
        resume() {
            let retState = SView.CommandExecuteResult.Success;
            this.setState(SView.CommandState.Resumed);
            this.onResume();
            if (this.commandScene.state === SView.CommandState.Initializing ||
                this.commandScene.state === SView.CommandState.ViewInitializing ||
                this.commandScene.state === SView.CommandState.WaitingInput) {
                //添加鼠标事件
                let view = this.getView();
                if (view && view instanceof SView.SCommandView && view.useEvent) {
                    view.addCommandTouchHandle();
                }
            }
            if (this.commandScene.state === SView.CommandState.Initializing) {
                //View初始化
                if (this.getView()) {
                    retState = this.getView().init();
                }
                //打开了对话框 需要等待对话框相应
                if (this.isShowView()) {
                    //等待参数输入
                    this.frame.fireCommandProcess(this, SView.CommandEvent.WaitingInput);
                    this.setState(SView.CommandState.WaitingInput);
                    retState = SView.CommandExecuteResult.Waiting;
                }
                else {
                    retState = this.doExecute();
                }
            }
            else if (this.commandScene.state === SView.CommandState.ViewInitializing) {
                //打开了对话框 需要等待对话框响应
                if (this.isShowView()) {
                    //等待参数输入
                    this.frame.fireCommandProcess(this, SView.CommandEvent.WaitingInput);
                    this.setState(SView.CommandState.WaitingInput);
                    retState = SView.CommandExecuteResult.Waiting;
                }
                else {
                    retState = this.doExecute();
                }
            }
            else if (this.commandScene.state === SView.CommandState.WaitingInput) {
                if (this.isShowView()) {
                    //含有对话框的命令 恢复后 显示界面
                    //this.frame.showPanel(this, true);
                    //State = SCommandState.WaitingInput;
                }
            }
            else if (this.commandScene.state === SView.CommandState.PreExecute ||
                this.commandScene.state == SView.CommandState.Execute ||
                this.commandScene.state == SView.CommandState.Executed) {
                retState = this.doExecute();
            }
            return retState;
        }
        /**
         * @protected
         * @description 取消任务
         */
        onCancel() {
            this.onClose();
        }
        /**
         * @public
         * @description 取消
         * @memberof SCommand
         */
        cancel() {
            //
            this.frame.fireCommandProcess(this, SView.CommandEvent.Cancelling);
            this.setState(SView.CommandState.Cancelling);
            this.onCancel();
            this.setState(SView.CommandState.Cancelled);
            this.frame.fireCommandProcess(this, SView.CommandEvent.Cancelled);
        }
        /**
         * @protected
         * @description  结束命令执行
         * @returns {number}
         */
        onClose() {
            var _a;
            //view可能不存在
            (_a = this.view) === null || _a === void 0 ? void 0 : _a.close();
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 关闭
         */
        close() {
            //当前执行命令中都不包含View
            this.frame.fireCommandProcess(this, SView.CommandEvent.Closing);
            this.state = SView.CommandState.Closing;
            this.onClose();
            this.state = SView.CommandState.Closed;
            this.frame.fireCommandProcess(this, SView.CommandEvent.Closed);
        }
        /**
         * @public
         * @beta
         * @description 回退
         * @returns {boolean}
         */
        undo() {
            return false;
        }
        // 
        /**
         * @protected
         * @description 命令具体逻辑的操作
         * @returns  {number}
         */
        onProcess() {
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 执行预览
         * @returns {number}
         */
        doPreview() {
            //TODO
            return this.onProcess();
        }
        /**
         * @protected
         * @description 预览命令业务逻辑
         * @returns  {number}
         */
        onPreview() {
            this.state = SView.CommandState.Preview;
            SView.FrameLogger.LOGD("", "", "", this.onPreview.name + this.name);
            //TODO日志如何打印
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description  预览
         * @returns {number}
         */
        preview() {
            //调用执行动作
            return this.onProcess();
        }
        /**
         * @public
         * @protected
         * @description 清空预览
         */
        onClearPreview() {
            //TODO
        }
        /**
         * @protected
         * @description 清空数据
         * @returns {number}
         */
        onClear() {
            var _a;
            (_a = this.view) === null || _a === void 0 ? void 0 : _a.close();
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description 清空
         * @returns {number}
         */
        clear() {
            return this.onClear();
        }
        /**
         * @public
         * @description 释放
         * @returns {boolean}
         */
        release() {
            return true;
        }
        /**
         * @private
         * @description 判断需要等待
         * @returns {boolean}
         */
        isWaiting() {
            if (this.getView() == null)
                return false;
            let paras = this.getInputParas();
            //根据参数的完整性、是否是命令行运行、是否是编辑返回true或false
            if (!paras.contains(SView.CommonStrings.ID) || paras.get(SView.CommonStrings.ID).value == null || paras.get(SView.CommonStrings.ID).value == SView.CommonStrings.nullString) {
                //非编辑状态 根据参数是否齐全 判定是否显示面板
                if (this.getIsParametersComplete())
                    return false;
                else
                    return true;
            }
            else {
                //编辑状态下 需要判定ShowView状态
                if (this.getIsDisplayView()) {
                    return true;
                }
                else {
                    if (this.getIsParametersComplete())
                        return false;
                    else
                        return true;
                }
            }
        }
        /**
         * @public
         * @description 判断是否需要显示面板
         * @returns  {boolean}
         */
        isShowView() {
            return this.isWaiting() && (this.view ? true : false);
        }
    }
    SView.SCommand = SCommand;
})(SView || (SView = {}));
/// <reference path = "./SCommand.ts"/>
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 异步命令
     * @author nihy
     * @hidden
     * @date 2022-08-24 16:47:39
     * @extends {SCommand}
     */
    class SAsynCommand extends SView.SCommand {
        constructor(name) {
            super(name);
        }
        /**
         * @return {number}
         */
        onInitialize() {
            return super.onInitialize();
        }
        /**
         * @return {number}
         */
        onPreExecute() {
            return super.onPreExecute();
        }
        /**
         * @return {number}
         */
        onExecute() {
            return super.onExecute();
        }
        /**
         * @return {number}
         */
        onExecuted() {
            // TODO  关闭对话框
            return super.onExecuted();
        }
        /**
         * @return {number}
         */
        onClose() {
            super.onClose();
            if (this.state === SView.CommandState.Execute) {
            }
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @return {number}
         */
        doExecute() {
            return super.doExecute();
        }
        /**
         * @return {*}
         */
        doAction() {
        }
        /**
         * @return {*}
         */
        AsynExecute() {
        }
    }
    SView.SAsynCommand = SAsynCommand;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 鼠标事件处理及命令对话框操作
     * @author nihy
     * @date 2022-08-10 08:41:26
     * @implements {ICommandView}
     */
    class SCommandView {
        constructor() {
            this.clickflag = false;
            //鼠标事件是否已经绑定
            this.eventBinding = false;
            //是否使用鼠标事件
            this.useEvent = false;
            //是否是左键并移动
            this.isMouseLeftMove = false;
            //
        }
        /**
         * @public
         * @description 是否使用鼠标事件
         * @returns {boolean}
         */
        getUseEvent() {
            return this.useEvent;
        }
        /**
         * @public
         * @description 设置是否使用鼠标事件
         * @param {boolean} value
         */
        setUseEvent(value) {
            this.useEvent = value;
        }
        /**
         * @public
         * @description  获取命令
         * @returns {SCommand}
         */
        getCommand() {
            return this.command;
        }
        /**
         * @public
         * @description 设置命令
         * @param {SCommand} value
         */
        setCommand(value) {
            this.command = value;
        }
        /**
         * @public
         * @description 获取当前配置
         * @returns {*}
         */
        getCurrentParameter() {
            return this.currentParameter;
        }
        /**
         * @public
         * @description 设置当前配置
         * @param {*} value
         */
        setCurrentParameter(value) {
            this.currentParameter = value;
        }
        /**
         * @public
         * @description 初始化View
         * @returns {number}
         */
        init() {
            //
            let result = SView.CommandExecuteResult.Failure;
            result = this.onInitialize();
            if (this.useEvent)
                this.addCommandTouchHandle();
            return result;
        }
        /**
         * @protected
         * @description 初始化
         * @returns  {number}
         */
        onInitialize() {
            this.command.state = SView.CommandState.ViewInitializing;
            return SView.CommandExecuteResult.Success;
        }
        /**
         * @public
         * @description  鼠标事件监听
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标事件
         * @returns {boolean}
         */
        onMouseEvent(sender, e) {
            let result = true;
            if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_LEFT) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                    result = this.onMouseLeftButtonDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                    result = this.onMouseLeftButtonUp(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseLeftButtonMove(sender, e);
                }
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_RIGHT) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                    result = this.onMouseRightButtonDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                    result = this.onMouseRightButtonUp(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseRightButtonMove(sender, e);
                }
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_MIDDLE) {
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_NONE) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseNoneButtonMove(sender, e);
                }
            }
            if (result !== false) {
                result = true;
            }
            return result;
        }
        /**
         *  @public
         * @description 是否为单击
         * @param {M3D.Inputs.TouchEventArgs} e 触摸事件
         * @returns {boolean}
         */
        isSingleClick(e) {
            let ret = false;
            switch (e.inputEventType) {
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN:
                    if (e.touchCount == 1) {
                        this.clickflag = true;
                    }
                    break;
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE:
                    //let limitDistance: number = SParameters.GetLimitDistance();
                    //if (Math.abs(e.getX() - this._previousX) < limitDistance || Math.abs(e.getY() - this._previousY) < limitDistance) {
                    //} else {
                    //    this._clickflag = false;
                    //}
                    break;
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP:
                    if (this.clickflag) {
                        ret = true;
                    }
                    this.clickflag = false;
                    break;
            }
            return ret;
        }
        /**
         *  @public
         * @description 触摸事件监听
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.TouchEventArgs} e  触摸事件
         * @returns  {boolean}
         */
        onTouchEvent(sender, e) {
            let result = true;
            if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN) {
                if (e.touchCount == 1)
                    result = this.onMouseLeftButtonDown(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP) {
                result = this.onMouseLeftButtonUp(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE) {
                if (e.touchCount == 1)
                    result = this.onMouseLeftButtonMove(sender, e);
            }
            if (!this.isSingleClick(e)) {
                return true;
            }
            if (result !== false) {
                result = true;
            }
            return result;
        }
        /**
         * @protected
         * @description 鼠标左键按下事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e  鼠标或触摸事件
         * @returns {boolean}
         */
        onMouseLeftButtonDown(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标左键抬起事件
         * @param {M3D.Inputs.Input} sender  input对象
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e 鼠标或触摸事件
         * @returns {boolean}
         */
        onMouseLeftButtonUp(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标左键移动事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e  鼠标或触摸事件
         * @returns {boolean}
         */
        onMouseLeftButtonMove(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标右键按下事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
         * @returns  {boolean}
         */
        onMouseRightButtonDown(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标右键抬起事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
         * @returns  {boolean}
         */
        onMouseRightButtonUp(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标右键移动事件
         * @param {M3D.Inputs.Input} sender  input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
         * @returns  {boolean}
         */
        onMouseRightButtonMove(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 不按下鼠标键 鼠标移动
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标事件
         * @returns  {boolean}
         */
        onMouseNoneButtonMove(sender, e) {
            return true;
        }
        /**
         *  @public
         * @description  键盘事件处理
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
         * @returns {boolean}
         */
        onKeyEvent(sender, e) {
            let result = true;
            if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_DOWN) {
                result = this.onKeyDown(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_UP) {
                result = this.onKeyUp(sender, e);
            }
            return result;
        }
        /**
         * @protected
         * @description 键盘按下事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
         * @returns  {boolean}
         */
        onKeyDown(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 键盘抬起事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
         * @returns  {boolean}
         */
        onKeyUp(sender, e) {
            return true;
        }
        /**
         *  @public
         * @description 添加触摸事件
         */
        addCommandTouchHandle() {
            if (this.eventBinding == false) {
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.SMouseEvent += OnMouseEvent;
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.STouchEvent += OnTouchEvent;
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.SKeyEvent += OnKeyEvent;
                this.eventBinding = true;
            }
        }
        /**
         *  @public
         * @description 移除触摸事件
         */
        removeCommandTouchHandle() {
            if (this.eventBinding == true) {
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.SMouseEvent -= OnMouseEvent;
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.STouchEvent -= OnTouchEvent;
                //SApplication.Instance.CurrentDocument.S3DViewControl.Viewports.SKeyEvent -= OnKeyEvent;
                this.eventBinding = false;
            }
        }
        /**
         *  @public
         * @description 确认
         * */
        ok() {
            //
            this.command.execute();
        }
        /**
         *  @public
         * @description 取消
         * */
        cancel() {
            //
            this.command.cancel();
        }
        /**
         *  @public
        * @description 关闭Command
        * */
        close() {
            //
            this.onClose();
            if (this.useEvent)
                this.removeCommandTouchHandle();
        }
        /**
         * @protected
         * @description  关闭的处理
         */
        onClose() {
        }
        /**
         *  @public
         * @beta
         * @description 核实？覆盖？
         * @returns {boolean}
         */
        vertify() {
            return true;
        }
        /**
         *  @public
         * @description 引用
         */
        apply() {
            this.command.apply();
        }
        /**
         *  @public
         * @description 预显
         * @beta
         */
        preview() {
            //
        }
        /**
         *  @public
         * @description 中断
         * @beta
         */
        interrupt() {
            //
        }
        /**
         *  @public
         * @description 恢复
         * @beta
         */
        resume() {
            //
        }
    }
    SView.SCommandView = SCommandView;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 模块
     * @author nihy
     * @date 31/03/2023
     */
    class SModule {
        constructor() {
            this.id = "-1"; //模块ID
            this.name = "Default"; //模块名
            this.assembly = "-1"; //所属板块
            this.commandConfigs = []; //命令配置列表
        }
        /**
         * @public
         * @description 设置模块ID
         * @returns  {string}
         */
        getId() {
            return this.id;
        }
        /**
         * @public
         * @description 获取模块ID
         * @param {string} value
         */
        setId(value) {
            this.id = value;
        }
        /**
         * @public
         * @description 获取模块名
         * @returns   {string}
         */
        getName() {
            return this.name;
        }
        /**
         * @public
         * @description 设置模块名
         * @param {string} value
         */
        setName(value) {
            this.name = value;
        }
        /**
         * @public
         * @description  获取所属模块
         * @returns {string}
         */
        getAssembly() {
            return this.assembly;
        }
        /**
         * @public
         * @description 设置所属模块
         * @param {string} value
         */
        setAssembly(value) {
            this.assembly = value;
        }
        /**
         * @public
         * @description 获取命令配置
         * @returns {*}
         */
        getCommandConfigs() {
            return this.commandConfigs;
        }
        /**
         * @public
         * @description 设置命令配置
         * @param  value
         */
        setCommandConfigs(value) {
            this.commandConfigs = value;
        }
        /**
         * @public
         * @description 添加命令配置
         * @param {CommandConfig} conf 配置对象
         * @returns {boolean}
         */
        addCommandConfig(conf) {
            if (conf != null && this.commandConfigs.indexOf(conf) < 0) {
                this.commandConfigs.push(conf);
                return true;
            }
            return false;
        }
        /**
         * @public
         * @description 获取命令配置
         * @param {string} cmdName 命令名称
         * @returns {CommandConfig}
         */
        getCommandConfig(cmdName) {
            let ret = null;
            for (let i = 0; i < this.commandConfigs.length; i++) {
                if (this.commandConfigs[i].getName() === cmdName) {
                    ret = this.commandConfigs[i];
                    break;
                }
            }
            return ret;
        }
    }
    SView.SModule = SModule;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description   命令配置读取类
     * @author nihy
     * @date 2022-08-22 16:32:58
     */
    class CommandConfigureReader {
        constructor(path) {
            this.filePath = "";
            this.filePath = path;
        }
        getFilePath() {
            return this.filePath;
        }
        setFilePath(value) {
            this.filePath = value;
        }
        /**
         * @beta
         * @description 读取命令配置文件
         */
        load() {
            //  this.readCommandConfig();
        }
        /**
         * @public
         * @description 读取并解析命令配置文件内容 ，支持指定模块、指定模块指定命令的数据获取
         * @param {string} jsonValue  配置json
         * @param {string} [moduleName]  模块名 可选参数
         * @param {string} [cmdName]  命令名  可选参数
         * @returns {boolean}
         */
        readCommandConfig(jsonValue, moduleName, cmdName) {
            let value = JSON.parse(jsonValue);
            let ret = false;
            if (value[SView.ROOT][SView.MODEULES]) {
                var modulesValue = value[SView.ROOT][SView.MODEULES];
                if (modulesValue) {
                    for (let moduleValue of modulesValue) {
                        let module = new SView.SModule();
                        module.setId(moduleValue[SView.ID]);
                        module.setName(moduleValue[SView.NAME]);
                        if (moduleName != undefined) {
                            //没有匹配上
                            if (module.getName().match(moduleName) === null) {
                                continue;
                            }
                            else {
                                ret = true;
                            }
                        }
                        if (moduleValue[SView.ASSEMBLY]) {
                            module.setAssembly(moduleValue[SView.ASSEMBLY]);
                        }
                        if (moduleValue[SView.COMMANDS]) {
                            var commandArray = moduleValue[SView.COMMANDS];
                            if (commandArray) {
                                for (let commandConfValue of commandArray) {
                                    var name = commandConfValue[SView.NAME];
                                    if (cmdName != undefined) {
                                        //没有匹配上
                                        if (name.match(cmdName) === null) {
                                            continue;
                                        }
                                        else {
                                            ret = true;
                                        }
                                    }
                                    var fullClassName = commandConfValue[SView.FULLCLASSNAME];
                                    if (M3D.Utility.DecideHelper.isEmpty(name) ||
                                        M3D.Utility.DecideHelper.isEmpty(fullClassName)) {
                                        continue;
                                    }
                                    let config = new SView.CommandConfig(fullClassName);
                                    config.setName(name);
                                    if (commandConfValue[SView.PRIORITY]) {
                                        config.setPriority(commandConfValue[SView.PRIORITY]);
                                    }
                                    if (commandConfValue[SView.INTERRUPTILBE]) {
                                        config.setPriority(commandConfValue[SView.INTERRUPTILBE]);
                                    }
                                    if (commandConfValue[SView.VIEWNAME]) {
                                        config.setViewName(commandConfValue[SView.VIEWNAME]);
                                        config.setShowDialog(true);
                                    }
                                    if (commandConfValue[SView.EXECUTEDHANDLEMODE]) {
                                        config.setExecutedHandleMode(commandConfValue[SView.EXECUTEDHANDLEMODE]);
                                    }
                                    if (commandConfValue[SView.ISPREVIEW]) {
                                        config.setIsPreView(commandConfValue[SView.ISPREVIEW]);
                                    }
                                    if (commandConfValue[SView.INPUTPARAMATERS]) {
                                        config.setInputParameters(this.parseCommandParas(SView.IOParameterType.INPUT_PARAM, commandConfValue[SView.INPUTPARAMATERS]));
                                    }
                                    if (commandConfValue[SView.OUTPUTPARAMATERS]) {
                                        config.setOutputParameters(this.parseCommandParas(SView.IOParameterType.OUTPUT_PARAM, commandConfValue[SView.OUTPUTPARAMATERS]));
                                    }
                                    module.addCommandConfig(config);
                                    SView.CommandFactory.getInstance().addModule(config.getName(), module);
                                }
                                ret = true;
                            }
                        }
                    }
                }
            }
            return ret;
        }
        /**
         * @private
         * @description 解析命令配置
         * @param {IOParameterType} ioType
         * @param {*} parasValue
         * @returns {CommandParameters}
         */
        parseCommandParas(ioType, parasValue) {
            let retParas = new SView.CommandParameters();
            if (parasValue) {
                for (let paraValue of parasValue) {
                    var name = paraValue[SView.NAME];
                    var type = paraValue[SView.TYPE];
                    if (M3D.Utility.DecideHelper.isEmpty(SView.NAME) ||
                        M3D.Utility.DecideHelper.isEmpty(SView.TYPE)) {
                        continue;
                    }
                    let para = new SView.CommandParameter();
                    para.name = name;
                    para.type = type;
                    para.ioType = ioType;
                    if (para.type == SView.CommandParameterType.COLOR) {
                        let color = new M3D.M3DMath.Color();
                        if (paraValue[SView.VALUE].length == 4) {
                            color.setColor(Number(paraValue[SView.VALUE][1]), Number(paraValue[SView.VALUE][2]), Number(paraValue[SView.VALUE][3]), Number(paraValue[SView.VALUE][0]));
                        }
                        para.value = color;
                    }
                    if (paraValue[SView.VALUE]) {
                        para.value = paraValue[SView.VALUE];
                    }
                    //该参数是否允许被重写，默认可被重写
                    if (paraValue[SView.ISOVERRIDE]) {
                        para.isOverride = paraValue[SView.ISOVERRIDE];
                    }
                    retParas.add(para);
                }
            }
            return retParas;
        }
        /**
         * @private
         * @description 读取命令外部配置
         * @beta
         * @returns {Map<string, object>}
         */
        readCommandExtConfig() {
            return null;
        }
    }
    SView.CommandConfigureReader = CommandConfigureReader;
})(SView || (SView = {}));
/**
 * @memberof SView
 * @hidden
 * @description   命令配置读取类
 * @author nihy
 * @date 2022-08-22 16:32:58
 */
var SView;
(function (SView) {
    class SConfigureWriter {
    }
    SView.SConfigureWriter = SConfigureWriter;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description  文档配置
     * @author nihy
     * @date 2022-08-22 16:32:58
     */
    class DocumentConfigure extends M3D.Config.Parameters {
        //加载模型自带配置文件
        load() {
            //
        }
        /**
         * @description 设置配置参数值
         * @beta
         * @param key
         * @param value
         */
        setParameter(key, value, all) {
            //
            return false;
        }
        /**
         * @description 获取配置项
         * @param {string} key
         */
        getParameter(key) {
            //
        }
    }
    DocumentConfigure.observationMode = "observationMode"; //观察模式1 自由观察 2 动态观察 3 约束观察
    DocumentConfigure.verticalAxis = "verticalAxis"; //上方向   0：X+；1：X-；2：Y+；3：Y-；4：Z+；5：Z-
    DocumentConfigure.selectedEffect = "selectedEffect"; //拾取模型高亮显示模式
    DocumentConfigure.isUseCullFace = "isUseCullFace"; //是否开启背面剔除
    DocumentConfigure.isOpenRoaming = "isOpenRoaming"; //开启漫游
    DocumentConfigure.roamingSpeed = "roamingSpeed"; //漫游速度
    DocumentConfigure.fieldSize = "fieldSize"; //视野大小
    DocumentConfigure.virtualKey = "virtualKey"; //虚拟按键
    SView.DocumentConfigure = DocumentConfigure;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description  Frame配置
     * @author	YW
     * @date		2022-1-20
     */
    class FrameConfigure extends M3D.Config.SParameters {
        /**
         * @constructor
         * @description 创建一个Frame配置的单例
         */
        constructor() {
            super();
            this.FontSize = 15.0;
            this.FPS = 60;
            this.BlueSky = "BlueSky";
            this.measureUnit = "毫米";
            this.commandConfigPath = ""; // 命令配置路径
            this.commandConfReader = null;
            //配置文件读取器（包括模型文件中的配置信息及系统的配置信息）
            this.sysConfigPath = ""; // 系统配置路径()
            this.docConfigPath = ""; // 系统配置路径()
            this.frameConfReader = null;
            this.commonConfigKey = [
                //M3D.Config.observationMode, M3D.Config.verticalAxis,
                M3D.Config.selectedEffect, M3D.Config.selectedColor,
                M3D.Config.isUseCullFace, M3D.Config.isOpenRoaming, M3D.Config.roamingSpeed, M3D.Config.fieldSize,
                "isJoySticks", "isMouseswitch"
            ];
            this.parentPara = SView.BaseConfigure.getInstance();
            this.parentPara.add(this);
        }
        /**
         * @description 获取Frame配置的单例
         * @static
         * @returns {FrameConfigure}
         */
        static getInstance() {
            if (!this.instance) {
                this.instance = new FrameConfigure();
            }
            return this.instance;
        }
        /**
         * @public
         * @description 获取命令配置路径
         * @returns {string}
         */
        getCommandConfigPath() {
            return this.commandConfigPath;
        }
        /**
         *  @public
         * @description 设置命令配置路径
         * @param {string} path
         */
        setCommandConfigPath(path) {
            if (!M3D.Utility.DecideHelper.isEmpty(path)) {
                this.commandConfigPath = path;
            }
        }
        /**
         * @public
         * @description  获取文档配置路径
         * @returns {string}
         */
        getDocConfigPath() {
            return this.docConfigPath;
        }
        /**
         * @public
         * @description  设置文档配置路径
         * @param {string} path
         */
        setDocConfigPath(path) {
            if (!M3D.Utility.DecideHelper.isEmpty(path)) {
                this.docConfigPath = path;
            }
        }
        /**
         * @public
         * @description   获取系统配置路径
         * @returns {string}
         */
        getSysConfigPath() {
            return this.sysConfigPath;
        }
        /**
         *  @public
         * @description  设置系统配置路径
         * @param {string} path
         */
        setSysConfigPath(path) {
            if (!M3D.Utility.DecideHelper.isEmpty(path)) {
                this.sysConfigPath = path;
            }
        }
        /**
         *  @public
         * @description 设置读取到的配置参数
         * @param {Map<string, any>} paras
         */
        initConfig(paras) {
            if (paras.size > 0) {
                for (let key of paras.keys()) {
                    if (String(paras.get(key)).length > 0) {
                        //this.setParameter(key, paras.get(key));
                        if (this.commonConfigKey.indexOf(String(paras.get(key))) < 0) {
                            //this.setCookieParameter(key, paras.get(key));
                            M3D.Config.Parameters.getInstance().setCookieParameter(key, paras.get(key));
                        }
                        if (key == M3D.Config.verticalAxis ||
                            key == M3D.Config.observationMode) {
                            //如果是向上方向或观察模式，则往viewportpara里更新一下
                            M3D.Config.ViewPortParameters.getInstance().setParameter(key, paras.get(key));
                        }
                    }
                }
                M3D.Utility.Logger.init();
            }
        }
        /**
         *  @public
         * @description 设置读取到的在线的配置文件配置参数
         * @param {Map<string, any>} paras 配置项Map
         * @param {boolean} isSystem   是否为系统配置
         */
        initOnlineConfig(paras, isSystem) {
            if (paras.size > 0) {
                for (let key of paras.keys()) {
                    if (String(paras.get(key)).length > 0) {
                        if (isSystem) {
                            //this.setParameterByCookie(key, paras.get(key));
                            M3D.Config.Parameters.getInstance().setParameterByCookie(key, paras.get(key));
                        }
                        else {
                            //this.setParameter(key, paras.get(key));
                            if (key == M3D.Config.verticalAxis ||
                                key == M3D.Config.observationMode) {
                                M3D.Config.SceneParameters.getInstance().setParameter(key, paras.get(key), false);
                            }
                            else {
                                M3D.Config.SceneParameters.getInstance().setParameter(key, paras.get(key));
                            }
                        }
                        if (key == M3D.Config.verticalAxis ||
                            key == M3D.Config.observationMode) {
                            //如果是向上方向或观察模式，则往viewportpara里更新一下
                            M3D.Config.ViewPortParameters.getInstance().setParameter(key, paras.get(key));
                        }
                    }
                }
                M3D.Utility.Logger.init();
            }
        }
        /**
         *  @public
         * @description 设置默认参数
         */
        setDefault() {
            //父级的配置文件的默认操作
            if (this.parentPara) {
                this.parentPara.setDefault();
            }
        }
        /**
         *  @public
         * @description 加载配置
         * @param {string} path 配置路径
         * @param {boolean} [isSystem=true] 是否为系统配置 ，可选参数 默认为系统配置
         * @returns {Promise<boolean>}
         */
        load(path, isSystem = true) {
            if (!this.frameConfReader) {
                this.frameConfReader = new SView.FrameConfigureReader();
            }
            //请求文件
            return new Promise((resolve, reject) => {
                this.requestConfigFile(path).then((ret) => {
                    if (typeof ret === "string") {
                        if (!M3D.Utility.DecideHelper.isEmpty(ret) && M3D.Utility.DecideHelper.isJson(ret)) {
                            //解析数据
                            let retParas = new Map();
                            if (isSystem) {
                                resolve(this.frameConfReader.parse(ret, retParas));
                            }
                            else {
                                resolve(this.frameConfReader.parse(ret, retParas));
                            }
                            if (retParas.size > 0) {
                                if (M3D.Config.ConfigFileParameters.check(retParas, true)) {
                                    this.initOnlineConfig(retParas, isSystem);
                                    resolve(true);
                                }
                                else {
                                    resolve(false);
                                }
                            }
                            else {
                                resolve(false);
                            }
                        }
                        else {
                            resolve(false);
                        }
                    }
                    else {
                        resolve(ret);
                    }
                });
            });
        }
        /**
         *  @public
         * @description 加载整个配置命令文件信息
         * @param {string} moduleName 不为空时,仅解析该模块相关的命令配置信息
         * @param {string} cmdName 不为空时,moduleName也必须不为空，仅解析该模块下的指定的命令配置信息
         */
        loadCommandConfigs(moduleName, cmdName) {
            if (this.commandConfReader === null) {
                this.commandConfReader = new SView.CommandConfigureReader();
            }
            //请求文件
            return new Promise((resolve, reject) => {
                this.requestConfigFile(this.getCommandConfigPath()).then((ret) => {
                    if (typeof ret === "string") {
                        if (!M3D.Utility.DecideHelper.isEmpty(ret)) {
                            //解析数据
                            resolve(this.commandConfReader.readCommandConfig(ret, moduleName, cmdName));
                        }
                        else {
                            resolve(false);
                        }
                    }
                    else {
                        resolve(ret);
                    }
                }).catch(() => {
                    reject(false);
                });
            });
        }
        /**
         *  @public
         * @description 向服务器请求配置文件
         * @param {string} path
         * @returns {(Promise<string | boolean>)}
         */
        requestConfigFile(path) {
            return new Promise((resolve, reject) => {
                let requestParameters = M3D.Utility.ServerHelper.getRequestParameter(M3D.Utility.REQUESTRESPONSETYPE.JSON, null);
                if (path) {
                    M3D.Utility.HttpMessageHandler.get(path, false, requestParameters).then((response) => {
                        if (!response || response.length <= 0) {
                            reject(false);
                        }
                        resolve(response);
                    }).catch((error) => {
                        //获取文件错误
                        SView.FrameLogger.FRAMELOGW(error);
                        reject(false);
                    });
                }
                else {
                    resolve(false);
                }
            });
        }
    }
    SView.FrameConfigure = FrameConfigure;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @description  Frame配置文件读取类
    * @author	YW
    * @date		2022-1-20
    */
    class FrameConfigureReader {
        /**
         * @description 解析配置文件
         * @param {string} jsonStr 配置内容
         * @param {Map<string, any>} retMap 配置项数组
         * @returns {boolean} 是否解析成功
         */
        parse(jsonStr, retMap) {
            try {
                let jsonValue = JSON.parse(jsonStr);
                if (jsonValue) {
                    let configureValues = jsonValue[FrameConfigureReader.configureItemsKey];
                    if (configureValues) {
                        for (let configureValue of configureValues) {
                            //循环读取到所有的配置项
                            retMap.set(configureValue[SView.NAME], configureValue[SView.VALUE]);
                        }
                    }
                }
                else {
                    return false;
                }
            }
            catch (error) {
                SView.FrameLogger.FRAMELOGW(error);
                return false;
            }
            return true;
        }
    }
    FrameConfigureReader.configureItemsKey = "configureItems";
    SView.FrameConfigureReader = FrameConfigureReader;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
    * @memberof SView
    * @description  Frame配置项目
    * @author	YW
    * @date		2022-1-20
    */
    class FrameConfigureItem {
        constructor() {
            /**
             * @description  指向H5根目录的路径
             */
            this.sviewBaseUrl = "";
            /**
             * @description  指向API的路径，用于分散存储查找子模型文件、按流模式请求数据、在线视图在线文档配置的服务器路径
             */
            this.sviewAPIUrl = "";
            /**
             * @description  国际化 0为中文 1为英文 默认中文
             */
            this.internationalLanguage = 0; //
            //设置界面配置项
            /**
                * @description  是否开启调试分组
                */
            this.debugCheck = false;
            /**
                * @description  配置方案
                */
            this.configuration = 0;
            /**
                * @description  视图在线管理服务器路径
                */
            this.serverViewUrl = true;
            /**
                * @description  是否使用视图在线管理
                */
            this.isUseServerView = true;
            /**
                * @description  是否使用在线文档设置
                */
            this.isUseServerInfo = true;
            //日志相关
            /**
                * @description  是否开启日志
                */
            this.isOpenLog = true;
            /**
                * @description  日志级别
                */
            this.logLevel = 1; //0：E 1：W  2：I 3：D
        }
    }
    SView.FrameConfigureItem = FrameConfigureItem;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @description 服务状态
     * @enum {number}
     */
    let ServiceState;
    (function (ServiceState) {
        ServiceState[ServiceState["Stoped"] = 1] = "Stoped";
        ServiceState[ServiceState["Started"] = 2] = "Started";
        ServiceState[ServiceState["Paused"] = 3] = "Paused";
    })(ServiceState = SView.ServiceState || (SView.ServiceState = {}));
    /**
     * @memberof SView
     * @description  服务基类 提供服务共用的属性及接口
     * @author nihy
     * @date 2022-08-17 08:59:28
     * @implements {M3D.Utility.Subscriber}
     * @implements {M3D.Utility.Publisher}
     */
    class Service {
        constructor(...arg) {
            this.frame = null;
            //当前服务的状态
            this.state = ServiceState.Stoped;
            this.clickflag = false;
            if (arg.length === 2) {
                this.type = arg[0];
                this.frame = arg[1];
            }
        }
        /**
         * @public
        * @description 设置SViewFrame对象
        * @param {FrameMain} view SViewFrame对象
        */
        setFrame(view) {
            return this.frame = view;
        }
        /**
         * @public
        * @description 获取SViewFrame对象
        * @return {FrameMain} SViewFrame对象
        */
        getFrame() {
            return this.frame;
        }
        /**
         * @public
         * @description 设置命令服务类型
         * @return {string} value 服务类型
         */
        setType(value) {
            this.type = value;
        }
        /**
         * @public
         * @description 获取命令服务类型
         * @return {string} 服务类型
         */
        getType() {
            return this.type;
        }
        /**
         *  @public
         * @description 获取服务状态
         * @return {number}
         */
        getState() {
            return this.state;
        }
        /**
         *  @public
         * @description 设置服务状态
         * @param {SServiceState} value
         */
        setState(value) {
            this.state = value;
        }
        /**
         *  @public
         * @description 启动服务
         * @return {void}
         */
        start() {
            this.state = ServiceState.Started;
        }
        /**
         *  @public
         * @description 停止服务
         * @return {void}
         */
        stop() {
            this.state = ServiceState.Stoped;
        }
        /**
         * @public
         * @description 是否自动停止服务
         * @param {SCommand} cmd 命令对象
         * @return {boolean}
         */
        isAutoStopService(cmd) {
            return false;
        }
        /**
         *  @public
         * @beta
         * @description 判断当前服务状态下，创建是否可以运行
         * @param {SCommand} cmd  命令对象
         * @return {boolean}
         */
        canExecute(cmd) {
            let bRet = true;
            if (this.state == ServiceState.Started) {
                //
            }
            return bRet;
        }
        /**
         *  @public
        * @description 鼠标事件监听
        * @param {M3D.Inputs.Input} sender 事件
        * @param {M3D.Inputs.MouseEventArgs} e 事件
        */
        onMouseEvent(sender, e) {
            let result = true;
            if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_LEFT) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                    result = this.onMouseLeftButtonDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                    result = this.onMouseLeftButtonUp(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                    //
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseLeftButtonMove(sender, e);
                }
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_RIGHT) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                    result = this.onMouseRightButtonDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                    result = this.onMouseRightButtonUp(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                    //
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseRightButtonMove(sender, e);
                }
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_MIDDLE) {
                //
            }
            else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_NONE) {
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                    result = this.onMouseNoneButtonMove(sender, e);
                }
            }
            if (result !== false) {
                result = true;
            }
            return result;
        }
        /**
         * @protected
         * @description 鼠标左键按下事件
         * @param {M3D.Inputs.Input} sender
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e
         * @returns {boolean}
         */
        onMouseLeftButtonDown(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标左键抬起事件
         * @param {M3D.Inputs.Input} sender  input对象
         * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e 鼠标或触摸事件
         * @returns {boolean}
         */
        onMouseLeftButtonUp(sender, e) {
            return true;
        }
        /**
        * @protected
        * @description 鼠标左键移动事件
        * @param {M3D.Inputs.Input} sender input对象
        * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e  鼠标或触摸事件
        * @returns {boolean}
        */
        onMouseLeftButtonMove(sender, e) {
            return true;
        }
        /**
          * @protected
          * @description 鼠标右键按下事件
          * @param {M3D.Inputs.Input} sender input对象
          * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
          * @returns  {boolean}
          */
        onMouseRightButtonDown(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 鼠标右键抬起事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
         * @returns   {boolean}
         */
        onMouseRightButtonUp(sender, e) {
            return true;
        }
        /**
          * @protected
          * @description 鼠标右键移动事件
          * @param {M3D.Inputs.Input} sender  input对象
          * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
          * @returns   {boolean}
          */
        onMouseRightButtonMove(sender, e) {
            return true;
        }
        /**
         * @protected
         * @description 不按下鼠标键 鼠标移动
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.MouseEventArgs} e 鼠标事件
         * @returns  {boolean}
         */
        onMouseNoneButtonMove(sender, e) {
            return true;
        }
        // 触屏事件处理
        onTouchEvent(sender, e) {
            let result = true;
            if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN) {
                if (e.touchCount == 1)
                    result = this.onMouseLeftButtonDown(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP) {
                result = this.onMouseLeftButtonUp(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE) {
                if (e.touchCount == 1)
                    result = this.onMouseLeftButtonMove(sender, e);
            }
            if (!this.isSingleClick(e)) {
                return true;
            }
            if (result !== false) {
                result = true;
            }
            return result;
        }
        /**
         *  @public
         * @description 是否为单击
         * @param {M3D.Inputs.TouchEventArgs} e 触摸事件
         * @returns {boolean}
         */
        isSingleClick(e) {
            let ret = false;
            switch (e.inputEventType) {
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN:
                    if (e.touchCount == 1) {
                        this.clickflag = true;
                    }
                    break;
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE:
                    //let limitDistance: number = SParameters.GetLimitDistance();
                    //if (Math.abs(e.getX() - this._previousX) < limitDistance || Math.abs(e.getY() - this._previousY) < limitDistance) {
                    //} else {
                    //    this._clickflag = false;
                    //}
                    break;
                case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP:
                    if (this.clickflag) {
                        ret = true;
                    }
                    this.clickflag = false;
                    break;
            }
            return ret;
        }
        /**
         *  @public
         * @description   键盘事件
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
         * @returns {boolean}
         */
        onKeyEvent(sender, e) {
            let result = true;
            if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_DOWN) {
                result = this.onKeyDown(sender, e);
            }
            else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_UP) {
                result = this.onKeyUp(sender, e);
            }
            return result;
        }
        /**
         * @protected
         * @description  键盘按下
         * @param {M3D.Inputs.Input} sender input对象
         * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
         * @returns  {boolean}
         */
        onKeyDown(sender, e) {
            return true;
        }
        /**
           * @protected
           * @description 按键抬起
           * @param {M3D.Inputs.Input} sender input对象
           * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
           * @returns  {boolean}
           */
        onKeyUp(sender, e) {
            return true;
        }
        /**
         * @private
         * @description 发送及时消息
         * @param {M3D.Utility.M3DMessage} msg
         */
        send(msg) {
            //send消息，监听立即执行
        }
        /**
         * @private
         * @description 发送普通消息
         * @param {M3D.Utility.M3DMessage} msg
         */
        post(msg) {
            //post消息，监听根据需要再获取执行
        }
        /**
         * @private
         * @description 根据收到的消息进行相关的逻辑处理
         * @param {M3D.Utility.M3DMessage} msg
         */
        update(msg) {
            //接收到消息
        }
    }
    SView.Service = Service;
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description 服务管理类
     * @author nihy
     * @date 2022-08-19
     */
    class ServiceManager {
        constructor() {
            //命令服务列表<string:服务类型，Service服务>
            this.services = new Map();
        }
        /**
         * @description 获取服务管理类实例
         * @static
         * @returns {ServiceManager}
         */
        static getInstance() {
            if (!this.instance) {
                this.instance = new ServiceManager();
            }
            return this.instance;
        }
        /**
         * @public
         * @description 设置Frame对象
         * @param {FrameMain} frame
         */
        setFrame(frame) {
            this.frame = frame;
        }
        /**
         * @public
         * @description 获取Frame对象
         * @returns {FrameMain}
         */
        getFrame() {
            return this.frame;
        }
        /**
         * @public
         * @description 设置服务列表
         * @param {Map<string, Service>} value
         */
        setServices(value) {
            this.services = value;
        }
        /**
         * @public
         * @description 获取整个服务列表
         * @return {Map} Map<string, Service>
         */
        getServices() {
            return this.services;
        }
        /**
         * @public
         * @description 添加服务到服务列表中
         * @param {Service} service
         */
        add(service) {
            if (!this.services.has(service.getType())) {
                this.services.set(service.getType(), service);
            }
        }
        /**
         * @public
         * @description 根据服务类型获取服务，如果服务不存在，会自动创建服务并返回新建的服务
         * @param {string} type  服务类型
         * @param {any} module  所属模块，所在命名空间
         * @return {Service}
         */
        getService(type, module) {
            let ret;
            if (M3D.Utility.DecideHelper.isEmpty(type)) {
                return ret;
            }
            if (this.services.has(type)) {
                ret = this.services.get(type);
            }
            else {
                if (!module) {
                    ret = new SView.Service(type, this.getFrame());
                }
                else {
                    let newService = SView.createSViewObject(module, type, type, this.getFrame());
                    if (newService) {
                        ret = newService;
                    }
                }
                if (ret) {
                    this.services.set(ret.getType(), ret);
                }
            }
            return ret;
        }
    }
    SView.ServiceManager = ServiceManager;
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        /**
         *@memberof SView
         *@description   Command中EVent监听，
         *@author	YW
         *@date		2022-1-20
         *@version	1.0
        */
        class ServiceTouchListener extends SView.EventListener {
            /**
             * @param sviewFrame  sviewFrame主入口
             */
            /**
             * @constructor
             * @description 构造函数
             * @param {Service} service
             */
            constructor(service) {
                super();
                this.clickflag = false;
                this.service = service;
                this.mouseDownListener = this.onTouchHandle;
                this.mouseMoveListener = this.onTouchHandle;
                this.mouseWheelListener = this.onTouchHandle;
                this.mouseUpListener = this.onTouchHandle;
                this.keyDownListener = this.onTouchHandle;
                this.keyUpListener = this.onTouchHandle;
            }
            /**
             * @description 触摸操作事件
             * @param event 鼠标事件|触摸事件|关键事件
             */
            onTouchHandle(event) {
                return true;
            }
            /**
             * @public
             * @description  鼠标事件监听
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.MouseEventArgs} e 鼠标事件
             * @returns {boolean}
             */
            onMouseEvent(sender, e) {
                let result = true;
                if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_LEFT) {
                    if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                        result = this.onMouseLeftButtonDown(sender, e);
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                        result = this.onMouseLeftButtonUp(sender, e);
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                        //
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                        result = this.onMouseLeftButtonMove(sender, e);
                    }
                }
                else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_RIGHT) {
                    if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_DOWN) {
                        result = this.onMouseRightButtonDown(sender, e);
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_UP) {
                        result = this.onMouseRightButtonUp(sender, e);
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_WHEEL) {
                        //
                    }
                    else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                        result = this.onMouseRightButtonMove(sender, e);
                    }
                }
                else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_MIDDLE) {
                    //
                }
                else if (e.mouseButton == M3D.Inputs.MouseButton.MOUSE_BUTTON_NONE) {
                    if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_MOUSE_MOVE) {
                        result = this.onMouseNoneButtonMove(sender, e);
                    }
                }
                if (result !== false) {
                    result = true;
                }
                return result;
            }
            /**
            * @protected
            * @description 鼠标左键按下事件
            * @param {M3D.Inputs.Input} sender input对象
            * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e  鼠标或触摸事件
            * @returns {boolean}
            */
            onMouseLeftButtonDown(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 鼠标左键抬起事件
             * @param {M3D.Inputs.Input} sender  input对象
             * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e 鼠标或触摸事件
             * @returns {boolean}
             */
            onMouseLeftButtonUp(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 鼠标左键移动事件
             * @param {M3D.Inputs.Input} sender input对象
             * @param {(M3D.Inputs.MouseEventArgs | M3D.Inputs.TouchEventArgs)} e  鼠标或触摸事件
             * @returns {boolean}
             */
            onMouseLeftButtonMove(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 鼠标右键按下事件
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
             * @returns  {boolean}
             */
            onMouseRightButtonDown(sender, e) {
                return true;
            }
            /**
              * @protected
              * @description 鼠标右键抬起事件
              * @param {M3D.Inputs.Input} sender input对象
              * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
              * @returns  {boolean}
              */
            onMouseRightButtonUp(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 鼠标右键移动事件
             * @param {M3D.Inputs.Input} sender  input对象
             * @param {M3D.Inputs.MouseEventArgs} e 鼠标或触摸事件
             * @returns {boolean}
             */
            onMouseRightButtonMove(sender, e) {
                return true;
            }
            /**
              * @protected
              * @description 不按下鼠标键 鼠标移动
              * @param {M3D.Inputs.Input} sender input对象
              * @param {M3D.Inputs.MouseEventArgs} e 鼠标事件
              * @returns  {boolean}
              */
            onMouseNoneButtonMove(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 事件处理
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.TouchEventArgs} e 鼠标事件
             * @returns {boolean}
             */
            onTouchEvent(sender, e) {
                let result = true;
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN) {
                    if (e.touchCount == 1)
                        result = this.onMouseLeftButtonDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP) {
                    result = this.onMouseLeftButtonUp(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE) {
                    if (e.touchCount == 1)
                        result = this.onMouseLeftButtonMove(sender, e);
                }
                if (!this.isSingleClick(e)) {
                    return true;
                }
                if (result !== false) {
                    result = true;
                }
                return result;
            }
            /**
            *  @public
            * @description 是否为单击
            * @param {M3D.Inputs.TouchEventArgs} e 触摸事件
            * @returns {boolean}
            */
            isSingleClick(e) {
                let ret = false;
                switch (e.inputEventType) {
                    case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_DOWN:
                        if (e.touchCount == 1) {
                            this.clickflag = true;
                        }
                        break;
                    case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_MOVE:
                        //let limitDistance: number = SParameters.GetLimitDistance();
                        //if (Math.abs(e.getX() - this._previousX) < limitDistance || Math.abs(e.getY() - this._previousY) < limitDistance) {
                        //} else {
                        //    this._clickflag = false;
                        //}
                        break;
                    case M3D.Inputs.InputEventType.INPUTEVENT_TOUCH_UP:
                        if (this.clickflag) {
                            ret = true;
                        }
                        this.clickflag = false;
                        break;
                }
                return ret;
            }
            /**
             *  @public
             * @description  键盘事件处理
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
             * @returns {boolean}
             */
            onKeyEvent(sender, e) {
                let result = true;
                if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_DOWN) {
                    result = this.onKeyDown(sender, e);
                }
                else if (e.inputEventType == M3D.Inputs.InputEventType.INPUTEVENT_KEY_UP) {
                    result = this.onKeyUp(sender, e);
                }
                return result;
            }
            /**
             * @protected
             * @description 键盘按下事件
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
             * @returns  {boolean}
             */
            onKeyDown(sender, e) {
                return true;
            }
            /**
             * @protected
             * @description 键盘抬起事件
             * @param {M3D.Inputs.Input} sender input对象
             * @param {M3D.Inputs.KeyEventArgs} e 键盘事件
             * @returns  {boolean}
             */
            onKeyUp(sender, e) {
                return true;
            }
        }
        Services.ServiceTouchListener = ServiceTouchListener;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
//# sourceMappingURL=SViewFrame.js.map