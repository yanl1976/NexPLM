/// <reference path="../../../lib/frame/SViewFrame.d.ts" />
/// <reference path="../../../lib/frame/SView.Action.d.ts" />
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        let ServiceMessageType;
        (function (ServiceMessageType) {
            ServiceMessageType["RoamServcie"] = "RoamServcie";
        })(ServiceMessageType = Services.ServiceMessageType || (Services.ServiceMessageType = {}));
        let ServiceMessageState;
        (function (ServiceMessageState) {
            ServiceMessageState["Start"] = "start";
            ServiceMessageState["Stop"] = "stop";
        })(ServiceMessageState = Services.ServiceMessageState || (Services.ServiceMessageState = {}));
        /**
         *@memberof SView.Services 从属于哪个命名空间
         *@extends {M3D.Utility.M3DMessage} 继承自哪个类（非必须）
         * @description 描述 服务消息
         *@author        YW
         *@date                2022-1-20
        */
        class ServiceMessage extends M3D.Utility.M3DMessage {
            constructor(serviceName, state, service, para) {
                super(serviceName, state, para);
                this.service = null;
                this.service = service;
            }
        }
        Services.ServiceMessage = ServiceMessage;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        let ServiceType;
        (function (ServiceType) {
            ServiceType["AnimationPlayService"] = "AnimationPlayService";
            ServiceType["BrowseModeService"] = "BrowseModeService";
            ServiceType["LicenseService"] = "LicenseService";
            ServiceType["LogService"] = "LogService";
            ServiceType["OpenService"] = "OpenService";
            ServiceType["ResourceService"] = "ResourceService";
            ServiceType["RoamService"] = "RoamService";
            ServiceType["BaseService"] = "SService";
            ServiceType["HotSpotService"] = "HotSpotService";
            ServiceType["UpdatePositionService"] = "UpdatePositionService";
            ServiceType["BoundingBoxNoteService"] = "BoundingBoxNoteService";
            ServiceType["ProjectionService"] = "ProjectionService";
        })(ServiceType = Services.ServiceType || (Services.ServiceType = {}));
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        /**
         * 设置--开启包围盒尺寸功能
         *
         * */
        class BoundingBoxNoteService extends SView.Service {
            //private viewer: S3DView = null;
            constructor(type, frame) {
                super(type, frame);
                this.scene = null;
                //this.viewer = frame.getViewer();
                this.scene = frame.getViewer().getScene();
                SView.ServiceManager.getInstance().add(this);
            }
            /**
             * 开启服务，增加监听
             * */
            start() {
                //设置环境
                super.start();
                //添加事件监听
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.scene);
                M3D.Shapes.BoundingBoxDisplayHelper.createSceneBoxNote(this.scene);
            }
            /**
            * 停止服务
            * */
            stop() {
                super.stop();
                //移除事件监听
                M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.scene);
                M3D.Shapes.BoundingBoxDisplayHelper.clearBoundingBox(this.scene);
            }
            /**
             *
             * 处理接收到的消息
             * @param msg
             */
            update(msg) {
                //接收到消息
                if (msg && msg instanceof M3D.Utility.SceneMessage) {
                    switch (msg.state) {
                        case M3D.Utility.OperaState.Update:
                            {
                                //创建包围盒尺寸
                                M3D.Shapes.BoundingBoxDisplayHelper.createSceneBoxNote(this.scene);
                            }
                            break;
                    }
                }
            }
        }
        Services.BoundingBoxNoteService = BoundingBoxNoteService;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        /**
         * @Author: nihy
         * @Date: 2023-1-17 14:35:57
         * @LastEditors: nihy
         * @LastEditTime: 2023-1-17 14:36:02
         * @FilePath: \SView_WebGL2\Frame\SViewService\src\services\HopSpotService.ts
         * @Description:
         */
        class HotSpotService extends SView.Service {
            constructor(type, frame) {
                super(type, frame);
                this.scene = null;
                this.viewer = null;
                this.hotSpot = null;
                this.viewer = frame.getViewer();
                this.scene = frame.getViewer().getScene();
                SView.ServiceManager.getInstance().add(this);
            }
            getHotSpot() {
                return this.hotSpot;
            }
            setHotSpot(value) {
                this.hotSpot = value;
            }
            /**
             * 开启服务，增加监听
             * */
            start() {
                //设置环境
                super.start();
                //添加事件监听
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.scene.getSelector());
            }
            /**
             * 停止服务
             * */
            stop() {
                super.stop();
                //移除事件监听
                //添加事件监听
                M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.scene.getSelector());
            }
            /**
             * 更新热点操作
             * @param spot
             */
            updateHotSpot(spot) {
                let spotAction = null;
                if (spot instanceof M3D.Shapes.ViewHotSpot) {
                    let viewSpot = spot;
                    spotAction = new SView.Actions.ViewHotSpotAction(this.scene, viewSpot.getModelViewID());
                }
                else if (spot instanceof M3D.Shapes.VoiceHotSpot) {
                    let voiceSpot = spot;
                    spotAction = new SView.Actions.VoiceHotSpotAction(this.scene, voiceSpot.getVoiceFileName());
                }
                else if (spot instanceof M3D.Shapes.AnimationHotSpot) {
                    let animationSpot = spot;
                    spotAction = new SView.Actions.AnimationHotSpotAction(this.viewer, animationSpot.getAnimationProcessID(), animationSpot.getAnimationStepID());
                }
                else if (spot instanceof M3D.Shapes.AreaHotSpot) {
                    let areaSpot = spot;
                    spotAction = new SView.Actions.AreaHotSpotAction(this.viewer, areaSpot.getAreaId());
                }
                if (spotAction != null) {
                    this.stopPreHotSpot();
                    let ret = spotAction.execute();
                    if (ret === M3D.Actions.ActionState.Action_Success) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
            stopPreHotSpot() {
                this.viewer.getAnimationPlayer().stopPlayer();
                M3D.Animation.SoundPlayer.instance().pause();
            }
            /**
             *
             * 处理接收到的消息
             * @param msg
             */
            update(msg) {
                //接收到消息
                if (msg && msg instanceof M3D.Utility.SelectorMessage) {
                    switch (msg.state) {
                        case M3D.Utility.OperaState.Add:
                            {
                                //添加事件
                                let selectedHotspot = null;
                                let shapes = msg.shapes;
                                //遍历获取最后被选中的model;
                                for (let i = shapes.length - 1; i < shapes.length; i++) {
                                    let shape = shapes[i];
                                    if (shape && shape instanceof M3D.Shapes.HotSpot) {
                                        selectedHotspot = shape;
                                    }
                                }
                                let hotSpotView = Services.HotSpotView.getInstance(this.getFrame());
                                if (selectedHotspot) {
                                    //执行热点操作
                                    let ret = this.updateHotSpot(selectedHotspot);
                                    if (ret) {
                                        hotSpotView.showHotSpotInfoDialog(selectedHotspot);
                                    }
                                    else {
                                        //提示异常信息
                                        Services.MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.OperationFailed;
                                        let hotSpotView = Services.HotSpotView.getInstance(this.getFrame());
                                        hotSpotView.getUiManager().load(Services.MessageDialog);
                                        setTimeout(function () {
                                            hotSpotView.getUiManager().removeLabel(Services.MessageDialog.Message.id);
                                        }, 3000);
                                    }
                                }
                                else {
                                    hotSpotView.hideHotSpotInfoDialog();
                                }
                            }
                            break;
                        case M3D.Utility.OperaState.Clear:
                            Services.HotSpotView.getInstance(this.getFrame()).hideHotSpotInfoDialog();
                            break;
                    }
                }
            }
        }
        Services.HotSpotService = HotSpotService;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        class HotSpotView {
            static getInstance(frameMain) {
                if (!this.hotSpotView) {
                    this.hotSpotView = new HotSpotView(frameMain);
                }
                return this.hotSpotView;
            }
            constructor(frameMain) {
                this.uiManager = frameMain.getUIManager();
            }
            getUiManager() {
                return this.uiManager;
            }
            setUiManager(value) {
                this.uiManager = value;
            }
            /**
            * 显示并根据热点内容显示不同的热点详情数据
            * @param hotSpot 热点对象
            * */
            showHotSpotInfoDialog(hotSpot) {
                this.hotspotInfoDialogId = Services.HotSpotInfoDialog.Dialog.id;
                let uiMap = this.uiManager.find(this.hotspotInfoDialogId);
                if (uiMap) {
                    this.hotspotInfoDialog = uiMap.get(this.hotspotInfoDialogId);
                }
                else {
                    this.hotspotInfoDialog = null;
                }
                if (this.hotspotInfoDialog) {
                    this.uiManager.showLabel(this.hotspotInfoDialogId);
                }
                else {
                    this.uiManager.load(SView.Services.HotSpotInfoDialog);
                    this.hotspotInfoDialog = this.uiManager.getElement(this.hotspotInfoDialogId);
                    //如果动画步骤描述面板正在显示，则将热点描述面板置为左上角
                    //SView - animationList
                    let animationList = this.uiManager.getElement("SView-animationList");
                    if (animationList) {
                        //aaa
                        let animationListWidth = animationList.eleLabel.offsetWidth;
                        this.hotspotInfoDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = String(Number(animationListWidth) + 30) + "px";
                    }
                    else {
                        //如果显示fps
                        if (M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.isShowBrowseFrames)) {
                            this.hotspotInfoDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.top = "60px";
                        }
                        this.hotspotInfoDialog.eleLabel.getElementsByClassName("SView-dialog")[0].style.left = "30px";
                    }
                }
                this.hotspotInfoDialog.setTitle(hotSpot.getName());
                this.hotspotInfoDialog.setContent(hotSpot.getDescription());
                if (hotSpot instanceof M3D.Shapes.AnimationHotSpot) {
                    let loopCBox = this.uiManager.getElement("play_loop_checkbox");
                    //动画界面未打开时，获取不到该元素
                    if (loopCBox) {
                        loopCBox.setValue(false);
                    }
                }
            }
            /**
             * 隐藏热点详情数据显示View
             * */
            hideHotSpotInfoDialog() {
                if (this.hotspotInfoDialog) {
                    this.uiManager.hideLabel(this.hotspotInfoDialogId);
                }
            }
        }
        Services.HotSpotView = HotSpotView;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        Services.HotSpotInfoDialog = {
            "Dialog": {
                "classes": [
                    "hotspot-dialog"
                ],
                "id": "hotspot-dialog",
                "closeBtnIsShow": true,
                "buttonsIsShow": false,
                "showBack": false,
                "onClick": "closeDialog('confirm-exit')",
                "isMove": true,
                "title": SView[SView.UIManager.languageInfo].languageObj.SHotSpot.SHotSpot_Title,
                "content": "动画内容"
            }
        };
        Services.MessageDialog = {
            "Message": {
                "classes": [
                    "SView-message",
                ],
                "id": "basicMessage",
                "content": ""
            }
        };
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
/**
 * 透视投影service
 * */
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        class ProjectionService extends SView.Service {
            constructor(type, frame) {
                super(type, frame);
                this.scene = null;
                this.viewer = null;
                this.viewer = frame.getViewer();
                this.scene = frame.getViewer().getScene();
                SView.ServiceManager.getInstance().add(this);
            }
            /**
            * 开启服务，增加监听
            * */
            start() {
                //设置环境
                super.start();
                //添加事件监听
                M3D.Utility.M3DMessageCenter.getCenter().subscribe(this, this.scene.getCurrentViewPort());
            }
            /**
            * 停止服务
            * */
            stop() {
                super.stop();
                //移除事件监听
                M3D.Utility.M3DMessageCenter.getCenter().unSubscribe(this, this.scene.getCurrentViewPort());
            }
            /**
            *
            * 处理接收到的消息
            * @param msg
            */
            update(msg) {
                //接收到消息
                if (msg && msg instanceof M3D.Utility.CameraMessage) {
                    if (msg.type == M3D.Utility.MessageType.Projection) {
                        switch (msg.state) {
                            case M3D.Utility.OperaState.Open: //开启透视投影
                                {
                                    this.isShowBtn(true);
                                }
                                break;
                            case M3D.Utility.OperaState.Close: //关闭透视投影
                                {
                                    this.isShowBtn(false);
                                }
                                break;
                        }
                    }
                }
            }
            /**
             * 修改透视投影按钮选中状态
             * @param isShow
             */
            isShowBtn(isShow) {
                let uiManager = this.getFrame().getUIManager();
                let btn = uiManager.getElement(ProjectionService.perspectiveViewBtn);
                if (btn) {
                    btn.setHighLight(isShow);
                }
            }
        }
        ProjectionService.perspectiveViewBtn = "perspectiveView"; //透视投影按钮
        Services.ProjectionService = ProjectionService;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        class RoamService extends SView.Service {
            constructor(type, frame) {
                super(type, frame);
                this.scene = null;
                this.roamAction = null;
                this.lastTicks = 0;
                this.oldInputHandlerType = M3D.Inputs.InputHandlerType.INPUT_HANDLER_COMMON;
                this.oldOrthographic = true;
                //private speed = 1.0;
                //虚拟摇杆相关
                this.joyStickLastTicks = 0;
                this.isUpAndDown = false; //右边上下摇杆是否按下
                this.upOrDownMap = { isUp: false, distance: 0 };
                this.isVirtualRocker = false; //左边前后左右按键是否按下
                this.virtualRockerMap = { strSpeed: 0, sidSpeed: 0 };
                this.timer = null;
                this.isMouseswitch = true; //是否开启漫游时鼠标操作
                this.keyMap = {}; //按键映射表
                this.scene = frame.getViewer().getScene();
                this.roamAction = new SView.Actions.RoamAction(this.scene);
                this.timer = new M3D.Utility.Timer();
                this.timer.setTimer(this.joyStick_tick, this, 10);
            }
            getType() {
                return Services.ServiceType.RoamService;
            }
            setOldOrthographic(oldOrthographic) {
                this.oldOrthographic = oldOrthographic;
            }
            getIsUpAndDown() {
                return this.isUpAndDown;
            }
            setIsUpAndDown(isUpAndDown) {
                this.isUpAndDown = isUpAndDown;
            }
            getUpOrDownMap() {
                return this.upOrDownMap;
            }
            getIsVirtualRocker() {
                return this.isVirtualRocker;
            }
            setIsVirtualRocker(isVirtualRocker) {
                this.isVirtualRocker = isVirtualRocker;
            }
            getVirtualRockerMap() {
                return this.virtualRockerMap;
            }
            //public setSpeed(speed: number) {
            //    this.speed = speed;
            //}
            //public setFirstPersionSpeed(firstPersionSpeed: number) {
            //    this.setSpeed(firstPersionSpeed);
            //    if (this.roamAction != null) {
            //        this.roamAction.setSpeed(firstPersionSpeed);
            //    }
            //}
            //public getSpeed(): number {
            //    return this.speed;
            //}
            setIsMouseswitch(isMouseswitch) {
                this.isMouseswitch = isMouseswitch;
            }
            /**
             * 设置向上方向
             * @param dir
             */
            setUpDirection(dir) {
                if (this.roamAction != null) {
                    this.roamAction.setUpDirection(dir);
                }
            }
            /**
             * 获取向上方向
             */
            getUpDirection() {
                if (this.roamAction != null) {
                    return this.roamAction.getUpDirection();
                }
            }
            /**
             * 设置投影中的fov角度值
             * @param fov
             */
            setCameraFov(fov) {
                if (this.roamAction != null) {
                    this.roamAction.setCameraFov(fov);
                }
            }
            /**
             * 获取投影中的fov角度值
             */
            getCameraFov() {
                if (this.roamAction != null) {
                    return this.roamAction.getCameraFov();
                }
            }
            getRoamAction() {
                return this.roamAction;
            }
            joyStick_tick(data) {
                let ser = data;
                //if (ser.joyStickLastTicks == 0) {
                //    ser.joyStickLastTicks = new Date().getTime();
                //}
                //else {
                //    let timeElapsed: number = new Date().getTime() - ser.joyStickLastTicks;
                //    if (timeElapsed > 20)//20毫秒刷新一次
                //    {
                //        ser.joyStickLastTicks = new Date().getTime();
                //        ser.handleJoyStickWalkThrough();
                //    }
                //}
                ser.handleJoyStickWalkThrough();
            }
            dispatcherTimer_Tick() {
                //let ser = this;
                let that = this;
                if (that.lastTicks == 0) {
                    that.lastTicks = new Date().getTime();
                    that.handleWalkThrough(200);
                }
                else {
                    let timeElapsed = new Date().getTime() - that.lastTicks;
                    if (timeElapsed > 20) //20毫秒刷新一次
                     {
                        that.lastTicks = new Date().getTime();
                        that.handleWalkThrough(timeElapsed);
                    }
                }
            }
            /**
             * @description: 判断当前服务状态下，创建是否可以运行
             * @param {SCommand} cmd
             * @return {boolean}
             */
            canExecute(cmd) {
                var _a;
                let bRet = true;
                if (this.getState() === SView.ServiceState.Started) {
                    bRet = !((_a = SView.CommandFactory.getInstance()) === null || _a === void 0 ? void 0 : _a.findModeBlackConfig("Roam", cmd.name));
                }
                return bRet;
            }
            /**
             * @description: 开始漫游服务
             */
            start() {
                //设置环境
                super.start();
                //if (!this.timer.isStart()) {
                //    this.timer.startTimer();
                //}
                //this.oldOrthographic = this.scene.getCurrentViewPort().getCamera().isOrthographic();
                this.scene.getCurrentViewPort().getInput().setInputHandlerType(M3D.Inputs.InputHandlerType.INPUT_HANDLER_WALKTHROUGH);
                this.scene.getCurrentViewPort().getCamera().setOrthographic(false); //Windows端没有此行设置，H5中单独添加这一行
                this.roamAction.setScene(this.scene);
                this.roamAction.setViewport(this.scene.getCurrentViewPort()); //重新设置一遍，因为之前初始化的时候相机参数还未更新
                //this.roamAction.setSpeed(this.speed);
                let message = new Services.ServiceMessage(Services.ServiceMessageType.RoamServcie, Services.ServiceMessageState.Start, this);
                this.send(message);
                //添加事件监听
                let roamListener = Services.RoamServiceTouchListener.getInstance(this);
                this.getFrame().getViewer().getEventListenersManager().addEventListener(roamListener);
            }
            statrTimer() {
                if (!this.timer.isStart()) {
                    this.timer.startTimer();
                }
            }
            stopTimer() {
                if (this.timer.isStart()) {
                    this.timer.stopTimer();
                }
            }
            /**
             * @description: 停止漫游服务
             */
            stop() {
                super.stop();
                this.stopTimer();
                this.scene.getCurrentViewPort().getCamera().setOrthographic(this.oldOrthographic);
                this.scene.getCurrentViewPort().getInput().setInputHandlerType(M3D.Inputs.InputHandlerType.INPUT_HANDLER_COMMON);
                let message = new Services.ServiceMessage(Services.ServiceMessageType.RoamServcie, Services.ServiceMessageState.Stop, this);
                this.send(message);
                //移除事件监听
                let roamListener = Services.RoamServiceTouchListener.getInstance(this);
                this.getFrame().getViewer().getEventListenersManager().removeEventListener(roamListener);
            }
            /**
             * 处理操纵杆漫游逻辑
             * */
            handleJoyStickWalkThrough() {
                let that = this;
                let topModel = that.scene.getTopModel();
                if (topModel == null) {
                    return;
                }
                let curSpeed = M3D.Config.ViewPortParameters.getInstance().getNumberParameter(M3D.Config.roamingSpeed);
                //按下上下建
                if (that.getIsUpAndDown()) {
                    let isUp = Boolean(that.getUpOrDownMap().isUp);
                    let dis = Number(that.getUpOrDownMap().distance);
                    if (dis <= 1) {
                        //dis *= that.getSpeed();
                        dis *= curSpeed;
                    }
                    that.roamAction.moveUpAndDown(isUp ? dis : -dis);
                }
                //按下前后左右建
                if (that.getIsVirtualRocker()) {
                    let strSpeed = Number(that.getVirtualRockerMap().strSpeed);
                    let sidSpeed = Number(that.getVirtualRockerMap().sidSpeed);
                    //console.log("-----strSpeed----=" + strSpeed + "------sidSpeed------="+sidSpeed);
                    //that.roamAction.joystickMove(strSpeed, sidSpeed);
                    if (strSpeed <= 1) {
                        //strSpeed *= that.getSpeed();
                        strSpeed *= curSpeed;
                    }
                    if (sidSpeed <= 1) {
                        //sidSpeed *= that.getSpeed();
                        sidSpeed *= curSpeed;
                    }
                    //左右移动
                    //that.roamAction.moveSideways(-sidSpeed);
                    //前后
                    that.roamAction.moveStraight(strSpeed);
                    //左右旋转
                    that.roamAction.rotateSideways(sidSpeed);
                }
            }
            /**
             * 处理键盘操作逻辑
             * @param timeElapsed
             */
            handleWalkThrough(timeElapsed) {
                //解决漫游模式下，当前活动控制为输入框、程序处于未活动状态时，ASDWQE按键按下时，仍会改变视角的问题
                //if (this.frame.Current != null) {
                //    if (Keyboard.FocusedElement == null || Keyboard.FocusedElement is TextBox)
                //    {
                //        return;
                //    }
                //}
                let that = this;
                let topModel = that.scene.getTopModel();
                if (topModel == null) {
                    return;
                }
                let curSpeed = M3D.Config.ViewPortParameters.getInstance().getNumberParameter(M3D.Config.roamingSpeed);
                //漫游控制前进左右
                let sceneBox = that.scene.getBoundingBox();
                let unitDistance = sceneBox.Length() / 200.0; //n=200：n秒走完包围盒长度。
                //let distance: number = that.speed * unitDistance * timeElapsed / 1000;
                let distance = curSpeed * unitDistance * timeElapsed / 1000;
                if (that.keyMap[87]) //w
                 {
                    if (that.getIsVirtualRocker()) {
                        return;
                    }
                    that.roamAction.moveStraight(distance);
                }
                if (that.keyMap[83]) //s
                 {
                    if (that.getIsVirtualRocker()) {
                        return;
                    }
                    that.roamAction.moveStraight(-distance);
                }
                if (that.keyMap[65]) //a
                 {
                    if (that.getIsVirtualRocker()) {
                        return;
                    }
                    that.roamAction.moveSideways(distance);
                }
                if (that.keyMap[68]) //d
                 {
                    if (that.getIsVirtualRocker()) {
                        return;
                    }
                    that.roamAction.moveSideways(-distance);
                }
                if (that.keyMap[81]) //q
                 {
                    if (that.getIsUpAndDown()) {
                        return;
                    }
                    that.roamAction.moveUpAndDown(-distance);
                }
                if (that.keyMap[69]) //e
                 {
                    if (that.getIsUpAndDown()) {
                        return;
                    }
                    that.roamAction.moveUpAndDown(distance);
                }
            }
            onKeyDown(sender, e) {
                let that = this;
                that.keyMap[e.key] = e.keyEvent.type == 'keydown';
                that.dispatcherTimer_Tick();
                //this.handleWalkThrough(20000);
                return true;
            }
            onKeyUp(sender, e) {
                let that = this;
                that.keyMap[e.key] = e.keyEvent.type == 'keydown'; //不要改成keyup，就是要false
                that.lastTicks = 0; //置为0，否则再次keydown时，导致首次计算的timeElapsed过大，导致模型偏移出屏幕
                return true;
            }
            onMouseEvent(sender, e) {
                let that = this;
                //let isSwitch = M3D.Config.ViewPortParameters.getInstance().getBoolParameter(CommandConfig.isMouseswitch);
                if (that.isMouseswitch) {
                    return super.onMouseEvent(sender, e);
                }
                else {
                    return false;
                }
            }
            send(msg) {
                //send消息，监听立即执行
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            post(msg) {
                //post消息，监听根据需要再获取执行
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
            update(msg) {
                //接收到消息
            }
        }
        Services.RoamService = RoamService;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        /**
         *@file
         *@brief   Command中EVent监听，
         *@author	YW
         *@date		2022-1-20
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
        */
        class RoamServiceTouchListener extends SView.Services.ServiceTouchListener {
            static getInstance(service) {
                if (!this.instance) {
                    this.instance = new RoamServiceTouchListener(service);
                }
                return this.instance;
            }
            /**
             * @description 构造函数
             * @param sviewFrame  sviewFrame主入口
             */
            constructor(service) {
                super(service);
            }
            /**
             * @description 鼠标键盘操作事件
             * @param event 鼠标事件|触摸事件|关键事件
             */
            onTouchHandle(event) {
                SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.START);
                let result = true;
                try {
                    //    let services = SView.ServiceManager.getInstance().getServices();
                    //    if (services.size > 0) {
                    //        for (let key of services.keys()) {
                    //            let service = services.get(key);
                    //            if (service.getState() == ServiceState.Started) {
                    //                if (event instanceof M3D.Inputs.MouseEventArgs) {
                    //                    if (service.getS3DView() != null) {
                    //                        let sender: M3D.Inputs.Input = service.getS3DView().getTouchViewport(event).getInput();
                    //                        result = service.onMouseEvent(sender, event);
                    //                        if (result !== false) {
                    //                            result = true;
                    //                        }
                    //                    }
                    //                }
                    //                if (event instanceof M3D.Inputs.KeyEventArgs) {
                    //                    if (service.getS3DView() != null) {
                    //                        let sender: M3D.Inputs.Input = service.getS3DView().getTouchViewport(event).getInput();
                    //                        result = service.onKeyEvent(sender, event);
                    //                        if (result !== false) {
                    //                            result = true;
                    //                        }
                    //                    }
                    //                }
                    //            }
                    //        }
                    //    } else {
                    //        FrameLogger.FRAMELOGD("Services has not excuting");
                    //    }
                    if (this.service) {
                        if (this.service.getState() == SView.ServiceState.Started) {
                            if (event instanceof M3D.Inputs.MouseEventArgs) {
                                if (this.service.getFrame().getViewer() != null) {
                                    let sender = this.service.getFrame().getViewer().getTouchViewport(event).getInput();
                                    result = this.service.onMouseEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                            if (event instanceof M3D.Inputs.KeyEventArgs) {
                                if (this.service.getFrame().getViewer() != null) {
                                    let sender = this.service.getFrame().getViewer().getTouchViewport(event).getInput();
                                    result = this.service.onKeyEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                        }
                    }
                }
                catch (error) {
                    SView.FrameLogger.FRAMELOGW(error, this.onTouchHandle.name);
                }
                SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.END);
                return result;
            }
        }
        Services.RoamServiceTouchListener = RoamServiceTouchListener;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        class UpdatePositionService extends SView.Service {
            constructor(type, frame) {
                super(type, frame);
                this.scene = null;
                this.updatePositionAction = null;
                this.canUpdatePosition = false;
                this.moveShape = null;
                this.currentViewPort = null;
                this.isMouseLeftMove = false;
                this.scene = frame.getViewer().getScene();
                this.currentViewPort = this.scene.getCurrentViewPort();
                this.updatePositionAction = new SView.Actions.UpdatePositionAction(this.scene);
            }
            getType() {
                return Services.ServiceType.UpdatePositionService;
            }
            getUpdatePositionAction() {
                return this.updatePositionAction;
            }
            /**
             * @description: 判断当前服务状态下，创建是否可以运行
             * @param {SCommand} cmd
             * @return {boolean}
             */
            canExecute(cmd) {
                var _a;
                let bRet = true;
                if (this.getState() === SView.ServiceState.Started) {
                    bRet = !((_a = SView.CommandFactory.getInstance()) === null || _a === void 0 ? void 0 : _a.findModeBlackConfig("Roam", cmd.name));
                }
                return bRet;
            }
            /**
             * @description: 开始漫游服务
             */
            start() {
                //设置环境
                super.start();
                this.updatePositionAction.setScene(this.scene);
                //添加事件监听
                let updatePositionListener = Services.UpdatePositionServiceListener.getInstance(this);
                this.getFrame().getViewer().getEventListenersManager().addEventListener(updatePositionListener);
            }
            /**
             * @description: 停止漫游服务
             */
            stop() {
                super.stop();
                //移除事件监听
                let updatePositionListener = Services.UpdatePositionServiceListener.getInstance(this);
                this.getFrame().getViewer().getEventListenersManager().removeEventListener(updatePositionListener);
            }
            // 鼠标左键按下事件
            onMouseLeftButtonDown(sender, e) {
                this.downPoint = new M3D.M3DMath.Vector2(e.position[0], e.position[1]);
                let shape = null;
                shape = this.currentViewPort.getPickShape(new M3D.M3DMath.Vector2(e.position[0], e.position[1]));
                if (shape && (shape instanceof M3D.Shapes.Annotation || shape instanceof M3D.Shapes.Measure)) {
                    this.moveShape = shape;
                    this.canUpdatePosition = true;
                }
                return true;
            }
            // 鼠标左键抬起事件
            onMouseLeftButtonUp(sender, e) {
                let result = true;
                if (this.canUpdatePosition && this.isMouseLeftMove) {
                    result = false;
                }
                this.canUpdatePosition = false;
                this.isMouseLeftMove = false;
                this.moveShape = null;
                return result;
            }
            // 鼠标左键移动事件
            onMouseLeftButtonMove(sender, e) {
                let curPoint = new M3D.M3DMath.Vector2(e.position[0], e.position[1]);
                let length = (this.downPoint.Sub(curPoint)).Length();
                if (length > 0.0000001) {
                    this.isMouseLeftMove = true;
                }
                if (this.canUpdatePosition && this.isMouseLeftMove) {
                    this.getUpdatePositionAction().setParameter(this.moveShape, e.position[0], e.position[1]);
                    let actionResult = this.getUpdatePositionAction().execute();
                    if (actionResult === M3D.Actions.ActionState.Action_Success) {
                        return false;
                    }
                }
                return true;
            }
            send(msg) {
                //send消息，监听立即执行
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            post(msg) {
                //post消息，监听根据需要再获取执行
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
            update(msg) {
                //接收到消息
            }
        }
        Services.UpdatePositionService = UpdatePositionService;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Services;
    (function (Services) {
        /**
         *@file
         *@brief   Command中Event监听，
         *@author	YW
         *@date		2022-1-20
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
        */
        class UpdatePositionServiceListener extends SView.Services.ServiceTouchListener {
            static getInstance(service) {
                if (!this.instance) {
                    this.instance = new UpdatePositionServiceListener(service);
                }
                return this.instance;
            }
            /**
             * @description 构造函数
             * @param sviewFrame  sviewFrame主入口
             */
            constructor(service) {
                super(service);
            }
            /**
             * @description 鼠标键盘操作事件
             * @param event 鼠标事件|触摸事件|关键事件
             */
            onTouchHandle(event) {
                SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.START);
                let result = true;
                try {
                    if (this.service) {
                        if (this.service.getState() == SView.ServiceState.Started) {
                            if (event instanceof M3D.Inputs.MouseEventArgs) {
                                if (this.service.getFrame().getViewer() != null) {
                                    let sender = this.service.getFrame().getViewer().getTouchViewport(event).getInput();
                                    result = this.service.onMouseEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                            if (event instanceof M3D.Inputs.TouchEventArgs) {
                                if (this.service.getFrame().getViewer() != null) {
                                    let sender = this.service.getFrame().getViewer().getTouchViewport(event).getInput();
                                    result = this.service.onTouchEvent(sender, event);
                                    if (result !== false) {
                                        result = true;
                                    }
                                }
                            }
                        }
                    }
                }
                catch (error) {
                    SView.FrameLogger.FRAMELOGW(error, this.onTouchHandle.name);
                }
                SView.FrameLogger.FRAMELOGD(this.onTouchHandle.name + M3D.END);
                return result;
            }
        }
        Services.UpdatePositionServiceListener = UpdatePositionServiceListener;
    })(Services = SView.Services || (SView.Services = {}));
})(SView || (SView = {}));
//# sourceMappingURL=SView.Service.js.map