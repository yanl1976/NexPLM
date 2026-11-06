var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @beta
         * @memberof SView.Actions
         * @description Action日志工具类
         * @export
         * @class ActionLogger
         * @extends {M3D.Utility.Logger}
         */
        class ActionLogger extends M3D.Utility.Logger {
            static ACTIONLOGE(moduleName, threadName, message) {
                M3D.Utility.Logger.LOGE(this.SPAND_ACTION, moduleName, threadName, message);
            }
            static ACTIONLOGW(message, moduleName, threadName) {
                M3D.Utility.Logger.LOGW(this.SPAND_ACTION, moduleName, threadName, message);
            }
            static ACTIONLOGI(message, moduleName = "", threadName = "") {
                M3D.Utility.Logger.LOGI(this.SPAND_ACTION, moduleName, threadName, message);
            }
            static ACTIONLOGD(message, moduleName = "", threadName = "") {
                M3D.Utility.Logger.LOGD(this.SPAND_ACTION, moduleName, threadName, message);
            }
        }
        ActionLogger.SPAND_ACTION = 'Action';
        Actions.ActionLogger = ActionLogger;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
/// <reference path = "../../../lib/m3d/M3D.d.ts"/>
/// <reference path="../../../lib/m3d/M3D.Reader.d.ts" />
/// <reference path = "../../../lib/base/S3DView.d.ts"/>
var SView;
(function (SView) {
    /**
     *@namespace SView.Actions
     *@memberof SView
     *@description Action动作命名空间
     *@date 2022-1-20
    */
    let Actions;
    (function (Actions) {
        Actions.SPAND_ACTION_CODE = "1";
        Actions.ACTION_ANIMATION_CODE = "0";
        Actions.ACTION_CAMERA_CODE = "1";
        Actions.ACTION_DRAGGER_CODE = "2";
        Actions.ACTION_EXPLOSIVE_CODE = "3";
        Actions.ACTION_INPUT_CODE = "4";
        Actions.ACTION_RAYPICK_CODE = "5";
        Actions.ACTION_SECTION_CODE = "6";
        Actions.ACTION_TRANSFORM_CODE = "7";
        Actions.ACTION_WALKTHROUGH_CODE = "8";
        /**
         * @memberof SView.Actions
         * @description 基本视图类型
         * @export
         * @enum {number}
         */
        let BaseViewType;
        (function (BaseViewType) {
            BaseViewType[BaseViewType["FRONT"] = 0] = "FRONT";
            BaseViewType[BaseViewType["BACK"] = 1] = "BACK";
            BaseViewType[BaseViewType["LEFT"] = 2] = "LEFT";
            BaseViewType[BaseViewType["RIGHT"] = 3] = "RIGHT";
            BaseViewType[BaseViewType["TOP"] = 4] = "TOP";
            BaseViewType[BaseViewType["BOTTOM"] = 5] = "BOTTOM";
            BaseViewType[BaseViewType["ISOMETRIC"] = 6] = "ISOMETRIC";
            BaseViewType[BaseViewType["DEFAULT"] = 7] = "DEFAULT";
        })(BaseViewType = Actions.BaseViewType || (Actions.BaseViewType = {}));
        /**
         * @memberof SView.Actions
         * @description Action名称
         * @export
         * @enum {number}
         */
        let ActionNames;
        (function (ActionNames) {
            ActionNames["ANNOTATIONACTION"] = "AnnotationAction";
            ActionNames["ANIMATIONACTION"] = "AnimationAction";
            ActionNames["DISTANCEMEASUREACTION"] = "DistanceMeasureAction";
            ActionNames["DIAMETERMEASUREACTION"] = "DiameterMeasureAction";
            ActionNames["ANGLEMEASUREACTION"] = "AngleMeasureAction";
            ActionNames["DRAGGERACTION"] = "DraggerAction";
            ActionNames["EXPLOSIVEACTION"] = "ExplosiveAction";
            ActionNames["MODELVIEWACTION"] = "ModelViewAction";
            ActionNames["SECTIONACTION"] = "SectionAction";
            ActionNames["SHOWMODELVIEWACTION"] = "ShowModelViewAction";
            ActionNames["CREATEMODELVIEWACTION"] = "CreateModelViewAction";
            ActionNames["SETDEFAULTMODELVIEWACTION"] = "SetDefaultModelViewAction";
            ActionNames["RENAMEMODELVIEWACTION"] = "RenameModelViewAction";
            ActionNames["DELETEMODELVIEWACTION"] = "DeleteModelViewAction";
            ActionNames["BASEVIEWACTION"] = "BaseViewAction";
            ActionNames["ANGLEOFZERO"] = "Angle of zero";
        })(ActionNames = Actions.ActionNames || (Actions.ActionNames = {}));
        Actions.GetModelsError = "GetModelsError";
        Actions.MODELVIEWNAMEPREFIX = "视图";
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 添加模型Action
         * @export
         * @class AddFileAction
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class AddFileAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of AddFileAction.
             * @memberof AddFileAction
             */
            constructor() {
                super();
            }
            /**
             * @private
             * @description  send
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof AddFileAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             * @description  post
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof AddFileAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
            /**
             * @public
            * @description 设置当前场景对象
            * @param {M3D.Scene} scene 场景对象
            * @memberof AddFileAction
            */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof AddFileAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 获取添加的模型
             * @return {M3D.Shapes.Model}
             * @memberof AddFileAction
             */
            getAddModel() {
                return this.addModel;
            }
            /**
             * @protected
             * @description 执行Action，若返回Action_PartExecuted，则说明是异步过程，需要监听该Action发送的消息来判断何时执行完成
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof AddFileAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, AddFileAction.name, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (!this.scene) {
                    retState = M3D.Actions.ActionState.Action_Error;
                }
                else {
                    retState = this.addFile();
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @public
             * @description 设置参数
             * @param {M3D.Shapes.Model} parentModel 将要添加到的模型对象
             * @param {string} fileName 文件名称
             * @param {ArrayBuffer} fileArray 文件内容
             * @return {boolean}
             * @memberof AddFileAction
             */
            setParameter(parentModel, fileName, fileArray) {
                this.parentModel = parentModel;
                this.fileName = fileName;
                this.fileArray = fileArray;
                return true;
            }
            /**
             * @private
             * @return {M3D.Actions.ActionState}
             * @memberof AddFileAction
             */
            addFile() {
                let result = M3D.Actions.ActionState.Action_Error;
                if (this.parentModel && this.fileName && this.fileArray) {
                    let scene = this.getScene();
                    const that = this;
                    //已存在reader，直接获取后进行copy
                    //仅加载装配结构时，方便后续加载使用，则不使用已存在的reader
                    if (scene.getLoader().getReaders().has(this.fileName) && !M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.isOnlyLoadAssy)) {
                        result = M3D.Actions.ActionState.Action_PartExecuted;
                        scene.getLoader().getReader(this.fileName).then((reader) => {
                            let topModel = reader.getTopModel(false);
                            if (topModel) {
                                let resourModel = new M3D.Shapes.Model();
                                M3D.Shapes.ShapeHelper.copyAllData(resourModel, topModel);
                                M3D.Shapes.ShapeHelper.addModelToShapes(resourModel, scene, true);
                                M3D.Shapes.ShapeHelper.addModelToVisibles(resourModel, scene, true);
                                that.parentModel.addChild(resourModel);
                                that.addModel = resourModel;
                                //let data = new SView.JsonAssembly(topModel, true, true);
                                //添加到装配结构中
                                //data.checked = true;
                                //tree.addNode(addedFileParentNode, data);
                                result = M3D.Actions.ActionState.Action_Success;
                                let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                that.send(message);
                            }
                        });
                    }
                    //不存在reader
                    else {
                        if (M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.isOnlyLoadAssy)) {
                            //跨浏览器通用去缓存：
                            this.fileName += ((/\?/).test(this.fileName) ? "&" : "?") + (new Date()).getTime();
                        }
                        let that = this;
                        result = M3D.Actions.ActionState.Action_PartExecuted;
                        scene.getLoader().getReader(this.fileName, this.fileArray, "svlx").then((reader) => {
                            reader.setIsParseInfo(false);
                            reader.setFile(this.fileArray, this.fileName).then((reader) => {
                                scene.addFile(that.fileName, reader, that.parentModel).then((res) => {
                                    if (!res) {
                                        result = M3D.Actions.ActionState.Action_Error;
                                        let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                        that.send(message);
                                        Actions.ActionLogger.ACTIONLOGE(AddFileAction.name, that.addFile.name, Actions.GetModelsError);
                                    }
                                    reader.getModels().then((model) => {
                                        if (model) {
                                            [...model.values()].forEach((model) => {
                                                that.getScene().getShapes().addShape(model);
                                                if (model.hasRenderData()) {
                                                    const lods = [...model.getLodDatas().values()];
                                                    scene.getLoader().getLODLoader().subscriberLOData(reader, lods);
                                                }
                                            });
                                            let topModel = reader.getTopModel(false);
                                            let parentModel = reader.getParentModel() || that.getScene().getTopModel();
                                            parentModel.addChild(topModel);
                                            that.addModel = topModel;
                                        }
                                        reader.getProtos().then((res) => {
                                            reader.getMaterials().then((materilaMap) => {
                                                if (!M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.isOnlyLoadAssy)) {
                                                    reader.getMesh().then(() => {
                                                        let reader = scene.getLoader().getReader(that.fileName);
                                                        if (reader) {
                                                            result = M3D.Actions.ActionState.Action_Success;
                                                            let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                                            that.send(message);
                                                        }
                                                    });
                                                }
                                                else {
                                                    M3D.Config.ViewPortParameters.getInstance().currentFPS = M3D.Config.Parameters.getInstance().getNumberParameter(M3D.Config.maxFPS);
                                                    let reader = scene.getLoader().getReader(that.fileName);
                                                    if (reader) {
                                                        result = M3D.Actions.ActionState.Action_Success;
                                                        let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                                        that.send(message);
                                                    }
                                                }
                                            });
                                        });
                                    }).catch((error) => {
                                        result = M3D.Actions.ActionState.Action_Error;
                                        let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                        that.send(message);
                                        Actions.ActionLogger.ACTIONLOGE(AddFileAction.name, that.addFile.name, Actions.GetModelsError);
                                    });
                                }).catch((err) => {
                                    result = M3D.Actions.ActionState.Action_Error;
                                    let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                    that.send(message);
                                    Actions.ActionLogger.ACTIONLOGE(AddFileAction.name, that.addFile.name, err.description);
                                });
                            }).catch((error) => {
                                result = M3D.Actions.ActionState.Action_Error;
                                let message = new SView.AddFileActionMessage(SView.ActionMessageType.AddFile, result.toString(), that.parentModel, that.addModel);
                                that.send(message);
                                Actions.ActionLogger.ACTIONLOGE(AddFileAction.name, that.addFile.name, error.description);
                            });
                        });
                    }
                }
                return result;
            }
            /**
             * @private
             * @description  clear
             * @memberof AddFileAction
             */
            clear() {
                this.parentModel = null;
                this.addModel = null;
                this.fileName = null;
                this.fileArray = null;
            }
        }
        Actions.AddFileAction = AddFileAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 对齐相机Action
         * @export
         * @class AlignmentCameraAction
         * @extends {M3D.Actions.Action}
         */
        class AlignmentCameraAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of AlignmentCameraAction.
             * @memberof AlignmentCameraAction
             */
            constructor() {
                super();
                this.isOpen = false; // 是否对其相机
                this.selectesPoint = new M3D.M3DMath.Vector2(); //选中位置
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof AlignmentCameraAction
             */
            setScene(scene) {
                this.pScene = scene;
            }
            /**
             * @description 设置是否开启Action
             *
             * @param {boolean} open
             * @memberof AlignmentCameraAction
             */
            setOpen(open) {
                this.isOpen = open;
            }
            /**
             * @public
             * @description 设置拾取的点
             * @param {M3D.M3DMath.Vector2} point 二维点坐标
             * @memberof AlignmentCameraAction
             */
            setSelectesPoint(point) {
                this.selectesPoint = point;
            }
            /**
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof AlignmentCameraAction
             */
            onExecute() {
                if (this.isOpen) {
                    //开启对其相机
                    this.alignmentCamera();
                }
                else {
                    //关闭对齐相机
                    this.closeAlignmentCamera();
                }
                return M3D.Actions.ActionState.Action_Error;
            }
            /**
             *
             * @private
             * @memberof AlignmentCameraAction
             */
            alignmentCamera() {
                let srcPnt = this.selectesPoint.Clone();
                let position = new M3D.M3DMath.Vector3();
                //获取鼠标拾取的三维空间点
                this.pScene.getCurrentViewPort().getPickPoint(srcPnt, position, true, true);
                let normal = this.pScene.getCurrentViewPort().getPickNormal(this.selectesPoint);
                if (!normal) {
                    return;
                }
                let direction = normal.Clone();
                //获取照相机的位置
                let camera = this.pScene.getCurrentViewPort().getCamera();
                let zDir = new M3D.M3DMath.Vector3(0, 0, 1.0);
                let tmpRot = new M3D.M3DMath.Quaternion(zDir, direction); //得到面的旋转量
                tmpRot = this.fitRotation(tmpRot);
                //获取相机焦距长度
                let fDistance = camera.getHalfViewSize();
                //计算摄像机相对目标点的位置
                let tmpPosition = position.Add(direction.Multiply(fDistance));
                camera.setPosition(tmpPosition);
                camera.setRotation(tmpRot);
            }
            /**
             * @private
             * @memberof AlignmentCameraAction
             */
            closeAlignmentCamera() {
                this.selectesPoint = new M3D.M3DMath.Vector2();
            }
            /**
             * @private
             * @param {M3D.M3DMath.Quaternion} rotation
             * @return {*}
             * @memberof AlignmentCameraAction
             */
            fitRotation(rotation) {
                let vecVerticalAxis = new M3D.M3DMath.Vector3(0, 1, 0);
                let strVerticalAxis = M3D.Config.Parameters.getInstance().getParameter(M3D.Config.verticalAxis);
                switch (strVerticalAxis) {
                    case 0:
                        vecVerticalAxis = M3D.M3DMath.Vector3.RIGHT();
                        break;
                    case 1:
                        vecVerticalAxis = M3D.M3DMath.Vector3.LEFT();
                        break;
                    case 3:
                        vecVerticalAxis = M3D.M3DMath.Vector3.DOWN();
                        break;
                    case 4:
                        vecVerticalAxis = M3D.M3DMath.Vector3.FORWARD();
                        break;
                    case 5:
                        vecVerticalAxis = M3D.M3DMath.Vector3.BACK();
                        break;
                    case 2:
                    default:
                        vecVerticalAxis = M3D.M3DMath.Vector3.UP();
                        break;
                }
                let matrix = rotation.RotationMatrix();
                let xOldDir = new M3D.M3DMath.Vector3(matrix.data[0], matrix.data[3], matrix.data[6]);
                let zDir = new M3D.M3DMath.Vector3(matrix.data[2], matrix.data[5], matrix.data[8]);
                let xDir = vecVerticalAxis.CrossProduct(zDir);
                //垂直，不用调整
                if (Math.abs(xDir.x) < 0.0001 && Math.abs(xDir.y) < 0.0001 && Math.abs(xDir.z) < 0.0001) {
                    return rotation;
                }
                xDir.Normalize();
                let offsetRotation = new M3D.M3DMath.Quaternion(xDir, xOldDir);
                matrix = matrix.Multiply(new M3D.M3DMath.Quaternion(offsetRotation.GetAngleValue(), new M3D.M3DMath.Vector3(0.0, 0.0, 1.0)).RotationMatrix());
                rotation.FromRotationMatrix(matrix);
                return rotation;
            }
        }
        Actions.AlignmentCameraAction = AlignmentCameraAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @extends {M3D.Actions.Action}
         */
        class AngleMeasureAction extends M3D.Actions.Action {
            /**
             * 构造函数
             * */
            constructor() {
                super();
                this.angle = []; //
                this.measure = null;
                this.measureType = M3D.Shapes.MeasureType.BASE_MEASURE;
                this.objID = 0;
                this.parentModelID1 = 0;
                this.measureType = M3D.Shapes.MeasureType.LINE_LINE_ANGLE;
                this.titleTextColor = M3D.M3DMath.Color.BLACK();
                this.fontSize = M3D.ANNOTATION_TEXT_FONT_SIZE;
                this.fontName = "";
                this.backGroundColor = M3D.M3DMath.Color.BLUE();
                this.resultTextColor = M3D.M3DMath.Color.BLACK();
                this.scene = null;
                this.firstPosition = null;
                this.secondPosition = null;
                this.thirdPosition = null;
                this.fourthPosition = null;
                this.leaderColor = new M3D.M3DMath.Color(241.0 / 255.0, 167.0 / 255.0, 71.0 / 255.0);
            }
            setScene(scene) {
                this.scene = scene;
            }
            /**
            * @description 获取新建的测量对象
            * @returns  测量对象
            */
            getShape() {
                return this.measure;
            }
            /**
             * 设置Action的名称
             * @param nameStr
             */
            setName(nameStr) {
                this.name = nameStr;
            }
            /**
             * 设置第一个选中的shape
             * @param firstID
             */
            setFirstShape(firstID) {
                this.firstShapeID = firstID;
            }
            /**
             * 设置第二个选中的shape
             * @param secondID
             */
            setSecondShape(secondID) {
                this.secondShapeID = secondID;
            }
            /**
             * 设置参数
             * @param objID 测量ID
             * @param type 测量类型
             * @param firstShape
             * @param secondShape
             * @param titleColor 标题颜色
             * @param textColor 文本颜色
             * @param fontName 字体名称
             * @param fontSize 字体大小
             * @param titleFormat 标题格式
             * @param backGroundColor 背景颜色
             * @param measureUnit 测量单位
             * @param parentModelID 模型ID
             */
            setParameter(objID, type, firstShape, firstPosition, secondShape, secondPosition, titleColor, textColor, fontName, fontSize, titleFormat, backGroundColor, measureUnit, parentModelID1, fourthPosition) {
                this.objID = objID;
                this.measureType = type;
                this.firstShape = firstShape;
                this.firstPosition = firstPosition;
                this.secondShape = secondShape;
                this.secondPosition = secondPosition;
                this.titleTextColor = titleColor;
                this.resultTextColor = textColor;
                this.fontName = fontName;
                this.fontSize = fontSize;
                this.titleFormat = titleFormat;
                this.backGroundColor = backGroundColor;
                this.measureUnit = measureUnit;
                this.parentModelID1 = parentModelID1;
                this.fourthPosition = fourthPosition;
                return true;
            }
            /**
             * 执行
             * @returns Action状态
             * */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                retState = this.doMeasure();
                if (retState != M3D.Actions.ActionState.Action_Success)
                    return retState;
                if (this.objID == 0) {
                    if (!this.addToScene())
                        retState = M3D.Actions.ActionState.Action_Error;
                }
                this.scene.getViewPorts().markDirty();
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @description 根据参数创建距离对象
             * @returns Action状态
             * */
            doMeasure() {
                let methodName = this.doMeasure.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.objID == 0)
                    this.measure = new M3D.Shapes.MeasureAngle();
                else {
                    let measures = this.scene.getShapes();
                    this.measure = measures.getShape(this.objID);
                }
                this.angle = [];
                this.setBaseInfo();
                //let bFlag: boolean = false;
                if (this.measureType == M3D.Shapes.MeasureType.LINE_LINE_ANGLE) {
                    //线线角度
                    state = this.lineWithLine();
                }
                else if (this.measureType == M3D.Shapes.MeasureType.LINE_FACE_ANGLE) {
                    //线面角度
                }
                else if (this.measureType == M3D.Shapes.MeasureType.FACE_FACE_ANGLE) {
                    //面面角度
                }
                //if (!bFlag)
                return state;
                //获取引线
                //this.getLeaderPoints(this.firstPosition, this.secondPosition, this.thirdPosition, this.fouthPosition);
                //this.setPoint("firstPoint", this.firstPosition);
                //this.setPoint("secondPoint", this.secondPosition);
                ////2 添加连接线和引出线
                //this.setLine("connectLine", this.titleTextColor, this.firstPosition, this.secondPosition);
                //this.setLine("leaderLine", this.leaderColor, this.thirdPosition, this.fouthPosition);
                //if (state != M3D.Actions.ActionState.Action_Success) {
                //    ActionLogger.ACTIONLOGE(methodName, ActionNames.ANNOTATIONACTION, "CreateMeasure addline failure!");
                //    return state;
                //}
                ////3 文本
                //state = this.setComText(0, this.distances);
                //ActionLogger.ACTIONLOGD(M3D.END, ActionNames.DISTANCEMEASUREACTION, methodName);
                //return state;
            }
            /**
             * @description 线与线之间的角度
             * */
            lineWithLine() {
                let ret = M3D.Actions.ActionState.Action_GetObjectFailure;
                //若有第一个边则设置第一个边
                if (this.firstShape && this.firstPosition) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            firstGeo = firstEdge.getGeometryFeature();
                        }
                    }
                    if (firstGeo
                        && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let firstLineFeatrue = firstGeo;
                        let firstWorldMatrix = firstEdge.getBody().getModel().getWorldTransform().ToMatrix3x4();
                        //M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let firstStartPoint = firstLineFeatrue.getStartPoint(); //!< Line起点
                        let firstEndPoint = firstLineFeatrue.getEndPoint(); //!< Line终点
                        firstStartPoint = firstWorldMatrix.Multiply(firstStartPoint).Clone();
                        firstEndPoint = firstWorldMatrix.Multiply(firstEndPoint).Clone();
                        ret = this.setLine("FirstLine", M3D.M3DMath.Color.RED(), firstStartPoint, firstEndPoint);
                    }
                }
                //若有两个边，则计算测量结果
                //若没有第二个边，则判断是否有测量结果，若有测量结果，则删除
                if (this.firstShape && this.firstPosition
                    && this.secondShape && this.secondPosition
                    && !this.fourthPosition) {
                    //删除第二个线，及其他线
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() === "MeasureImageLeader") {
                            this.measure.removeLine(lines[i]);
                            //return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                    ret = this.setLineToLineAngle();
                }
                if (this.firstShape && this.firstPosition && !this.secondShape && !this.secondPosition) {
                    if (this.measure.getImageList().length > 0) {
                        this.measure.clearImage();
                    }
                    //删除第二个线，及其他线
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() !== "FirstLine") {
                            this.measure.removeLine(lines[i]);
                            //return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                    ret = M3D.Actions.ActionState.Action_Success;
                }
                if (this.fourthPosition) {
                    let imageList = this.measure.getImageList();
                    if (imageList.length > 0) {
                        for (let image of imageList) {
                            image.setPosition(this.fourthPosition);
                        }
                    }
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() === "MeasureImageLeader") {
                            lines[i].setEndPoint(this.fourthPosition);
                            return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                }
                return ret;
                ////若有第一个边则设置第一个边
                //if (this.firstShape && this.secondPosition) {
                //    //this.setLine("firstLine", this.firstShape);
                //}
                ////若有第二个边则设置第二个边
                //if (this.secondShape && this.secondPosition) {
                //    //this.setLine();
                //}
                ////若没有第二个边，则判断是否有测量结果，若有测量结果，则删除
                ////若两个边都有了，则计算角度，生成测量结果
                ////角度
                //if (!(this.firstShape instanceof M3D.Shapes.Edge) && !(this.secondShape instanceof M3D.Shapes.Edge)) {
                //    M3D.Reader.MeasureFactory.createPntToPntDistance(this.measureType, this.firstPosition, this.secondPosition, this.pScene, this.measure,
                //        this.titleTextColor, this.resultTextColor, this.fontName, this.fontSize, this.titleFormat, this.backGroundColor, this.maxDistanceColor,
                //        this.minDistanceColor, this.measurementUnit, this.isShowMaxDistance, this.isShowMinDistance);
                //}
                //return true;
            }
            fromJson(jsonStr) {
                return false;
            }
            //createMeasure(): M3D.Actions.ActionState {
            //    let methodName = M3D.Utility.BrowserHelper.getMethodName();
            //    this.measure = new M3D.Shapes.MeasureAngle();
            //    this.measure.setMeasureType(this.measureType);
            //    let state: M3D.Actions.ActionState = M3D.Actions.ActionState.Action_Success;
            //    if (this.measureType == M3D.Shapes.MeasureType.LINE_LINE_ANGLE) {
            //        if (this.firstShape.getShapeType() != M3D.Shapes.ShapeType.EDGE
            //            || this.secondShape.getShapeType() != M3D.Shapes.ShapeType.EDGE) {
            //            ActionLogger.ACTIONLOGE(methodName, ActionNames.ANGLEMEASUREACTION, "CreateMeasure type not matched!");
            //            return M3D.Actions.ActionState.Action_ObjectTypeNotMatch;
            //        }
            //        this.setLineToLineAngle();
            //    } else if (this.measureType == M3D.Shapes.MeasureType.FACE_FACE_ANGLE) {
            //        if (this.firstShape.getShapeType() != M3D.Shapes.ShapeType.FACE
            //            || this.secondShape.getShapeType() != M3D.Shapes.ShapeType.FACE) {
            //            ActionLogger.ACTIONLOGE(methodName, ActionNames.ANGLEMEASUREACTION, "AngleMeasureAction.CreateMeasure type not matched!");
            //            return M3D.Actions.ActionState.Action_ObjectTypeNotMatch;
            //        }
            //        this.setFaceToFaceAngle();
            //    }
            //    else if (this.measureType == M3D.Shapes.MeasureType.LINE_FACE_ANGLE) {
            //        if (this.firstShape.getShapeType() != M3D.Shapes.ShapeType.EDGE
            //            || this.secondShape.getShapeType() != M3D.Shapes.ShapeType.FACE) {
            //            ActionLogger.ACTIONLOGE(methodName, ActionNames.ANGLEMEASUREACTION, "AngleMeasureAction.CreateMeasure type not matched!");
            //            return M3D.Actions.ActionState.Action_ObjectTypeNotMatch;
            //        }
            //        this.setLineToFaceAngle();
            //    }
            //    return state;
            //}
            /**
             * 设置线线角度
             * */
            setLineToLineAngle() {
                let methodName = this.setLineToLineAngle.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let ret = M3D.Actions.ActionState.Action_GetObjectFailure;
                let firstEdge = this.firstShape;
                let secondEdge = this.secondShape;
                let firstGeo = firstEdge.getGeometryFeature();
                let secondGeo = secondEdge.getGeometryFeature();
                if (!firstGeo) {
                    let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                    if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        firstGeo = firstEdge.getGeometryFeature();
                    }
                }
                if (!secondGeo) {
                    let type = M3D.Shapes.ShapeHelper.getLineType(secondEdge);
                    if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        secondGeo = secondEdge.getGeometryFeature();
                    }
                }
                let gr = M3D.M3DMath.Color.GRAY();
                let wt = M3D.M3DMath.Color.WHITE();
                let blk = M3D.M3DMath.Color.BLACK();
                let grn = M3D.M3DMath.Color.GREEN();
                let rd = M3D.M3DMath.Color.RED();
                let blu = M3D.M3DMath.Color.BLUE();
                if (firstGeo
                    && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE && secondGeo
                    && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                    let firstLineFeatrue = firstGeo;
                    let secondLineFeatrue = secondGeo;
                    let firstWorldMatrix = firstEdge.getBody().getModel().getWorldTransform().ToMatrix3x4();
                    let secondWorldMatrix = secondEdge.getBody().getModel().getWorldTransform().ToMatrix3x4();
                    //几何属性变化
                    //M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                    //M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, secondWorldMatrix);
                    let angle = 0.0;
                    //获取角度接口（目前缺失）
                    let firstStartPoint = firstLineFeatrue.getStartPoint(); //!< Line起点
                    let firstEndPoint = firstLineFeatrue.getEndPoint(); //!< Line终点
                    firstStartPoint = firstWorldMatrix.Multiply(firstStartPoint).Clone();
                    firstEndPoint = firstWorldMatrix.Multiply(firstEndPoint).Clone();
                    let secondStartPoint = secondLineFeatrue.getStartPoint(); //!< Line起点
                    let secondEndPoint = secondLineFeatrue.getEndPoint(); //!< Line起点
                    secondStartPoint = secondWorldMatrix.Multiply(secondStartPoint).Clone();
                    secondEndPoint = secondWorldMatrix.Multiply(secondEndPoint).Clone();
                    angle = firstEndPoint.Sub(firstStartPoint).Angle(secondEndPoint.Sub(secondStartPoint));
                    //将夹角转换到0-π/2
                    if (angle > 90.0) {
                        let temp = new M3D.M3DMath.Vector3();
                        temp = firstEndPoint;
                        firstEndPoint = firstStartPoint;
                        firstStartPoint = temp;
                        angle = 180.0 - angle;
                    }
                    this.angle = [angle];
                    let line1Vector = firstEndPoint.Sub(firstStartPoint);
                    let line2Temp = secondStartPoint.Add(secondEndPoint);
                    let line2Center = line2Temp.Divide(2);
                    //平移线1，使其与线2相交
                    let removeLine1End = secondStartPoint.Add(line1Vector);
                    let screenPoint = this.scene.getCurrentViewPort().worldToScreenPoint(this.secondPosition);
                    //camera射线
                    let cameraRay = this.scene.getCurrentViewPort().getScreenRay(screenPoint.x, screenPoint.y);
                    //构造线
                    //构造投影平面
                    let projPlane = new M3D.M3DMath.Plane(cameraRay.GetDirection(), firstStartPoint);
                    let pntInPlane;
                    if (!this.fourthPosition) {
                        pntInPlane = projPlane.Project(cameraRay.GetOrigin());
                    }
                    else {
                        pntInPlane = this.fourthPosition;
                    }
                    //let line2: M3D.Shapes.Line = new M3D.Shapes.Line(firstStartPoint, firstEndPoint);//测量线1
                    //let lineColor: M3D.M3DMath.Color = M3D.M3DMath.Color.GREEN();
                    //line2.setColor(this.backGroundColor);//暂时使用backGroundColor，应该使用选择颜色
                    this.setLine("FirstLine", this.backGroundColor, firstStartPoint, firstEndPoint);
                    let line3 = new M3D.Shapes.Line(secondStartPoint, secondEndPoint); //测量线2
                    line3.setColor(this.titleTextColor); //暂时使用titleTextColor ，应该使用线颜色
                    this.setLine("SecondLine", this.titleTextColor, secondStartPoint, secondEndPoint);
                    let line4 = new M3D.Shapes.Line(secondStartPoint, removeLine1End); //线1平移线
                    line4.setColor(this.titleTextColor); //暂时使用m_titleTextColor ，应该使用线颜色
                    line4.setName("exLine");
                    this.setLine("exLine1", this.titleTextColor, secondStartPoint, removeLine1End);
                    let line5 = new M3D.Shapes.Line(firstStartPoint, secondStartPoint); //延长线
                    line5.setColor(this.titleTextColor); //暂时使用m_titleTextColor ，应该使用线颜色
                    line5.setName("exLine");
                    this.setLine("exLine2", this.titleTextColor, firstStartPoint, secondStartPoint);
                    let line6 = new M3D.Shapes.Line(firstEndPoint, removeLine1End); //延长线
                    line6.setColor(this.titleTextColor); //暂时使用m_titleTextColor ，应该使用线颜色
                    line6.setName("exLine");
                    this.setLine("exLine3", this.titleTextColor, firstEndPoint, removeLine1End);
                    let linesTemp = [];
                    this.createAngleMark(secondStartPoint, removeLine1End, secondStartPoint, secondEndPoint, angle, linesTemp);
                    let line1;
                    if (linesTemp.length > 0) {
                        for (let i = 0; i < linesTemp.length - 1; i++) {
                            let lineTemp = new M3D.Shapes.Line(linesTemp[i], linesTemp[i + 1]);
                            lineTemp.setColor(this.resultTextColor);
                            this.measure.addLine(lineTemp);
                        }
                        if ((angle > 0 && angle < 90.0) || (angle > 90.0 && angle < 180.0)) {
                            line1 = new M3D.Shapes.Line(linesTemp[Number.parseInt(linesTemp.length / 2 + "")].Add(linesTemp[Number.parseInt(linesTemp.length / 2 + "") - 1]).Divide(2), pntInPlane); //引线
                        }
                        else if (angle - 90 < 0.0000001 && angle - 90 > -0.0000001) {
                            line1 = new M3D.Shapes.Line(linesTemp[1], pntInPlane); //引线
                        }
                        else {
                            line1 = new M3D.Shapes.Line(secondStartPoint, pntInPlane); //引线
                        }
                    }
                    else {
                        line1 = new M3D.Shapes.Line(secondStartPoint, pntInPlane); //引线
                    }
                    line1.setName("MeasureImageLeader");
                    line1.setColor(this.leaderColor);
                    this.measure.addLine(line1);
                    //构造线
                    //缺失
                    //构造投影平面（缺失）
                    //构造显示文本 
                    let textLength;
                    this.setComText(0, this.angle);
                    let unit = "°";
                    //unit = this.getMeasureUnit(/*M3D.Config.Parameters.getInstance().measureUnitStyle*/1, this.distances, unit);
                    let temptext = new Array();
                    for (let i = 0; i < this.angle.length; ++i) {
                        if (this.angle[i] > -0.000001) {
                            let tempText = [];
                            let lengthstr = this.angle[i].toFixed(2).toString();
                            let title = new M3D.Shapes.Texts2D();
                            title.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                            title.setBackground(this.backGroundColor);
                            title.setFontColor(this.titleTextColor);
                            title.setTexts(M3D.Shapes.MeasureDisplayHelper.getTitleText(i, this.measureType));
                            tempText.push(title);
                            let content = new M3D.Shapes.Texts2D();
                            content.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                            content.setBackground(M3D.M3DMath.Color.WHITE());
                            content.setFontColor(this.resultTextColor);
                            content.setTexts(lengthstr + unit + " ");
                            tempText.push(content);
                            temptext.push(tempText);
                        }
                    }
                    M3D.Shapes.MeasureDisplayHelper.createNoteTextsImageMultiline(this.scene, temptext, pntInPlane, this.measure);
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    //根据整个场景包围盒的大小来确定文字的大小
                    ret = M3D.Actions.ActionState.Action_Success;
                }
                return ret;
            }
            /**
             * 设置面面角度
             * */
            //setFaceToFaceAngle() {
            //    let firstFace: M3D.Shapes.Face = new M3D.Shapes.Face();
            //    let secondFace = new M3D.Shapes.Face();
            //    let firstGeo: M3D.Shapes.GeometryFeature = firstFace.getGeometryFeature();
            //    let secondGeo: M3D.Shapes.GeometryFeature = secondFace.getGeometryFeature();
            //    if (!firstGeo) {
            //        // GetFaceType  接口不存在  缺失
            //        /*
            //        GeoFeatureEnum type = (GeoFeatureEnum)ShapeHelper::GetLineType(firstFace->GetSelfPtr());
            //        if (type == GeoFeatureEnum::FEATURE_TYPE_PLANEFACE) {
            //        firstGeo = firstFace->GetSelfPtr()->GetGeometryFeature();
            //        }
            //        */
            //    }
            //    if (!secondGeo) {
            //        // GetFaceType  接口不存在  缺失
            //        /*
            //        GeoFeatureEnum type = (GeoFeatureEnum)ShapeHelper::GetLineType(secondFace->GetSelfPtr());
            //        if (type == GeoFeatureEnum::FEATURE_TYPE_PLANEFACE) {
            //        secondGeo = secondFace->GetSelfPtr()->GetGeometryFeature();
            //        }*/
            //    }
            //    let gr: M3D.M3DMath.Color = M3D.M3DMath.Color.GRAY();
            //    let wt: M3D.M3DMath.Color = M3D.M3DMath.Color.WHITE();
            //    let blk: M3D.M3DMath.Color = M3D.M3DMath.Color.BLACK();
            //    let grn: M3D.M3DMath.Color = M3D.M3DMath.Color.GREEN();
            //    let rd: M3D.M3DMath.Color = M3D.M3DMath.Color.RED();
            //    let blu: M3D.M3DMath.Color = M3D.M3DMath.Color.BLUE();
            //    if (firstGeo
            //        && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE && secondGeo
            //        && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
            //        let firstLineFeatrue: M3D.Shapes.LineFeature = <M3D.Shapes.LineFeature>firstGeo;
            //        let secondLineFeatrue: M3D.Shapes.LineFeature = <M3D.Shapes.LineFeature>secondGeo;
            //        let firstWorldMatrix: M3D.M3DMath.Matrix3x4 = firstFace.getWorldTransform().ToMatrix3x4();
            //        let secondWorldMatrix: M3D.M3DMath.Matrix3x4 = secondFace.getWorldTransform().ToMatrix3x4();
            //        //几何属性变化
            //        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
            //        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, secondWorldMatrix);
            //        let angle: number = 50.2;
            //        //获取角度接口（目前缺失）
            //        //camera射线
            //        /*Ray cameraRay = m_pScene->GetCamera()->GetViewPort().GetScreenRay(
            //        screenPnt.m_x, screenPnt.m_y);*/
            //        //构造线
            //        //缺失
            //        //构造投影平面（缺失）
            //        //构造显示文本
            //        let textLength: number[];
            //        this.setComText(0, this.angle);
            //        //根据整个场景包围盒的大小来确定文字的大小
            //    }
            //}
            /*
             * 设置线面角度
             * */
            //setLineToFaceAngle() {
            //}
            /**
             * @description 添加节点到场景树
             * */
            addToScene() {
                //LOGDEBUG("Action", "DistanceMeasureAction::AddToScene  begin!");
                //添加批注到场景
                let ret = false;
                if (this.measure && this.scene) {
                    this.measure.setVisible(true);
                    let measures = this.scene.getShapes();
                    measures.addShape(this.measure);
                    this.objID = this.measure.getId();
                    this.scene.refresh();
                    //解锁
                    ret = true;
                }
                else {
                    //LOGERRO("Action", "DistanceMeasureAction::AddToScene  annotation is null!");
                }
                //LOGDEBUG("Action", "DistanceMeasureAction::AddToScene  end!");
                return ret;
            }
            setComText(index, angle) {
                let bFlag = false;
                let comtexts = this.measure.getComTexts();
                if (comtexts.length > index) {
                    let comtext = comtexts[index];
                    if (comtext) {
                        let itor = angle[0];
                        let tmp;
                        while (itor != angle[angle.length - 1]) {
                            tmp += itor.toString();
                            if ((itor + 1) != angle[angle.length - 1])
                                tmp += "_";
                        }
                        comtext.setText(0, tmp);
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                let ct = new M3D.Shapes.ComText();
                let itor = angle[0];
                let tmp;
                while (itor != angle[angle.length - 1]) {
                    tmp += itor.toString();
                    if ((itor + 1) != angle[angle.length - 1])
                        tmp += "_";
                }
                let t = new M3D.Shapes.Text();
                ct.addText(t);
                t.setText(tmp);
                this.measure.addComText(ct);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 计算角度弧线
             * @param line1Start 第一条线的起点
             * @param line1End 第一条线的结束点
             * @param line2Start 第二条线的起点
             * @param line2End 第二条线的结束点
             * @param center 弧线的中心点
             * @param radius 弧线的半径
             * @param theta 弧线的角度
             * @param lines 结果弧线
             */
            createAngleMark(line1Start, line1End, line2Start, line2End, theta, lines) {
                if (theta < 0.000001 && theta > -0.000001) {
                    // LOGI("MeasureDisplayHelper::CreateAngleMark ZERO VECTOR");
                    return;
                }
                let line1Length = line1End.Sub(line1Start).Length();
                let line2Length = line2End.Sub(line2Start).Length();
                let num = Math.abs(theta / 10);
                let radius = line1Length > line2Length ? line2Length / 3.0 : line1Length / 3.0;
                let center = line2Start;
                let perTheta = theta / num;
                let line1Direction = (line1End.Sub(line1Start)).Normalized();
                let line2Direction = (line2End.Sub(line2Start)).Normalized();
                let axis = line1Direction.CrossProduct(line2Direction);
                axis = axis.Normalized();
                let cosTheta = line1Direction.DotProduct(line2Direction);
                let startPoint = center.Add(line1Direction.Multiply(radius)); //端点起始点
                lines.push(startPoint);
                for (var i = 0; i < num - 1; i++) {
                    let q = new M3D.M3DMath.Quaternion(perTheta * (i + 1), axis);
                    q.Normalize();
                    let line1DirectionQua = new M3D.M3DMath.Quaternion(0, line1Direction.x, line1Direction.y, line1Direction.z);
                    let tempVectorQuaternion = q.Multiply(line1DirectionQua).Multiply(q.Conjugate());
                    let newVector = new M3D.M3DMath.Vector3(tempVectorQuaternion.x, tempVectorQuaternion.y, tempVectorQuaternion.z);
                    newVector.Normalize();
                    let newPoint = center.Add(newVector.Multiply(radius)).Clone();
                    lines.push(newPoint);
                }
                let endPoint = center.Add(line2Direction.Multiply(radius)).Clone(); //端点结束点
                lines.push(endPoint);
            }
            addLine(firstPoint, secondPoint, thirdPoint, fourthPoint) {
                return M3D.Actions.ActionState.Action_Success;
            }
            setLine(lineName, color, beginPoint, endPoint) {
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    if (lines[i].getName() == lineName) {
                        lines[i].setStartPoint(beginPoint);
                        lines[i].setEndPoint(endPoint);
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                let leaderLine = new M3D.Shapes.Line();
                leaderLine.setColor(color);
                leaderLine.setStartPoint(beginPoint);
                leaderLine.setEndPoint(endPoint);
                leaderLine.setName(lineName);
                this.measure.addLine(leaderLine);
                return M3D.Actions.ActionState.Action_Success;
            }
            getLeaderPoints(firstPoint, secondPoint, beginPoint, endPoint) {
                return true;
            }
            setPointPosition(index, position) {
                //待实现
            }
            toJson() {
                return "";
            }
            /**
             * @description 设置基本参数
             * */
            setBaseInfo() {
                if (this.measure) {
                    this.measure.setMeasureType(this.measureType);
                }
            }
        }
        Actions.AngleMeasureAction = AngleMeasureAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @extends {M3D.Actions.Action}
         */
        class AnimationAction extends M3D.Actions.Action {
            /**
             * 构造函数
             * */
            constructor() {
                super();
            }
            /**
             * 执行
             * @returns Action状态
             * */
            onExecute() {
                return M3D.Actions.ActionState.Action_Error;
            }
        }
        Actions.AnimationAction = AnimationAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @extends {M3D.Actions.Action}
         */
        class AnnotationAction extends M3D.Actions.Action {
            /**
             * 构造函数
             * */
            constructor() {
                super();
                this.objID = 0;
                this.modelPath = "";
                this.parentModelPath = "";
                this.retAnnotation = null;
                this.type = 0;
                this.firstPosition = new M3D.M3DMath.Vector3();
                this.secondPosition = new M3D.M3DMath.Vector3();
                this.text = "";
                this.fontColor = M3D.M3DMath.Color.BLACK();
                this.fontSize = M3D.ANNOTATION_TEXT_FONT_SIZE;
                this.fontName = "";
                this.frameColor = M3D.M3DMath.Color.BLACK();
                this.fillColor = M3D.M3DMath.Color.BLUE();
                this.leaderColor = new M3D.M3DMath.Color(241.0 / 255.0, 167.0 / 255.0, 71.0 / 255.0);
                this.isLeader = true;
                this.isEnvelope = false;
                this.isFixed = false;
                this.isInTop = true;
                this.isStub = false;
                this.pScene = null;
                this.bReaderState = false;
                this.bReImage = true;
                this.isEdit = false;
            }
            /**
             * description设置参数
             * @param  objID  批注ID ，type 类型，firstPosition 第一个点 ，secondPosition 第二个点
             * @param text 文本内容，fontColor 字体颜色，fontName 字体名称，fontSize 字体大小
             * @param frameColor 边框颜色，fillColor 填充颜色，isLeader 是否显示引线,isEnvelope 是否显示外框
             * @param isFixed 是否固定屏幕 ，inTop  是否前端显示，isStub 是否包含折弯,modelID 关联模型ID
             * */
            setParameter(objID, type, firstPosition, secondPosition, text, fontColor, fontName, fontSize, frameColor, fillColor, isLeader, isEnvelope, isFixed, inTop, isStub, modelPath, isEdit) {
                this.objID = objID;
                this.type = type;
                this.firstPosition = firstPosition;
                this.secondPosition = secondPosition;
                this.text = text;
                this.fontColor = fontColor;
                this.fontName = fontName;
                this.fontSize = fontSize;
                this.frameColor = frameColor;
                this.fillColor = fillColor;
                this.isLeader = isLeader;
                this.isEnvelope = isEnvelope;
                this.isFixed = isFixed;
                this.isInTop = inTop;
                this.isStub = isStub;
                this.modelPath = modelPath == "0" ? "" : modelPath;
                this.isEdit = isEdit;
                return true;
            }
            /**
            * @description 设置当前视口
            * @param pScene 摄像机对象
            */
            setScene(pScene) {
                this.pScene = pScene;
            }
            /**
            * @description 获取当前视口
            * @param pScene 摄像机对象
            */
            getScene() {
                return this.pScene;
            }
            /**
            * @description 设置是否为读取状态
            * @param value boolean
            */
            setReaderState(value) {
                this.bReaderState = value;
            }
            /**
            * @description 获取新建的对象
            * @returns 新建的批注对象
            */
            getShape() {
                return this.retAnnotation;
            }
            /**
             * @description 执行
             * @returns Action状态
             * */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Error;
                if (this.objID === 0) {
                    if (this.createAnnotation() && this.addToScene()) {
                        retState = M3D.Actions.ActionState.Action_Success;
                    }
                }
                else {
                    if (this.editorAnnotation()) {
                        retState = M3D.Actions.ActionState.Action_Success;
                    }
                }
                this.pScene.getViewPorts().markDirty();
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
                return retState;
            }
            /**
             * @description 根据参数创建批注
             * */
            createAnnotation() {
                let methodName = this.createAnnotation.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                this.retAnnotation = new M3D.Shapes.Annotation();
                // 设置基本参数
                this.setAnnotationInfo();
                this.setRelatedModel();
                // 设置文本节点
                this.setAnnotationText();
                //this.retAnnotation = M3D.Reader.AnnotationFactory.createAnnotation(this.firstPosition,
                //    this.secondPosition, this.text, this.frameColor, this.fillColor, this.isLeader, this.isEnvelope,
                //    this.isStub, this.isFixed, this.pScene, this.type, null);
                ////this.pScene.getShapes().addShape(this.retAnnotation, true);
                //this.retAnnotation.setVisible(true);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
                return true;
            }
            /**
             * @description 根据参数编辑批注
             * */
            editorAnnotation() {
                let methodName = this.editorAnnotation.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                if (this.objID !== 0 && this.pScene) {
                    let notes = this.pScene.getShapes();
                    if (notes) {
                        //if (this.isEdit) {
                        //    let annotation: M3D.Shapes.Annotation = notes.getShape(this.objID, M3D.Shapes.ShapeType.ANNOTATION_NOTE);
                        //    if (annotation) {
                        //        annotation.setVisible(false);
                        //    }
                        //    this.retAnnotation = new M3D.Shapes.Annotation();
                        //} else {
                        this.retAnnotation = notes.getShape(this.objID, M3D.Shapes.ShapeType.ANNOTATION_NOTE);
                        //}
                        if (this.retAnnotation) {
                            // 设置关联模型
                            this.setRelatedModel();
                            // 设置基本参数
                            this.setAnnotationInfo();
                            if (this.retAnnotation && this.retAnnotation.getParentModel()) {
                                // 设置第一个点
                                const worldTransformInverse = this.retAnnotation.getParentModel().getWorldTransform().Inverse();
                                this.setPointPosition(0, this.isEdit ? this.firstPosition : worldTransformInverse.Multiply(this.firstPosition));
                            }
                            else {
                                // 设置第一个点
                                this.setPointPosition(0, this.firstPosition);
                            }
                            // 设置文本节点
                            this.setAnnotationText();
                            //
                            this.pScene.getShapes().updateShape(this.retAnnotation, true);
                            return true;
                        }
                        else {
                            //待实现
                        }
                    }
                    else {
                        //待实现
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
                return false;
            }
            /**
             * @description 添加节点到场景树
             * */
            addToScene() {
                //添加批注到场景
                let ret = false;
                if (this.retAnnotation && !this.bReaderState && this.pScene) {
                    //加锁
                    let annotations = this.pScene.getShapes();
                    this.retAnnotation.setVisible(false, true);
                    annotations.addShape(this.retAnnotation);
                    this.retAnnotation.setVisible(true, false);
                    //解锁
                    ret = true;
                }
                else {
                    //待实现
                }
                return ret;
            }
            /**
             * @description 设置基本参数
             * */
            setAnnotationInfo() {
                let methodName = this.setAnnotationInfo.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                if (this.retAnnotation) {
                    this.bReImage = this.retAnnotation.getText() != this.text;
                    this.retAnnotation.setText(this.text);
                    this.retAnnotation.setAnnotationType(this.type);
                    this.retAnnotation.setIsFixed(this.isFixed);
                    this.retAnnotation.setFillColor(this.fillColor);
                    this.retAnnotation.setFrameColor(this.frameColor);
                    let inverseMatrix = new M3D.M3DMath.Matrix4();
                    if (this.retAnnotation.getParentModel()) {
                        inverseMatrix = this.retAnnotation.getParentModel().getWorldTransform().Inverse();
                    }
                    if (this.isEdit) {
                        this.retAnnotation.setPosition(this.firstPosition);
                        this.retAnnotation.setCenterPos(this.secondPosition);
                    }
                    else {
                        this.retAnnotation.setPosition(inverseMatrix.Multiply(this.firstPosition));
                        this.retAnnotation.setCenterPos(inverseMatrix.Multiply(this.secondPosition));
                    }
                    this.retAnnotation.setHaveStub(this.isStub);
                    this.retAnnotation.setHaveLeader(this.isLeader);
                    if (this.isLeader) {
                        let lineList = this.retAnnotation.getLineList();
                        if (lineList && lineList.length > 0) {
                            for (let line of lineList) {
                                if (this.isEdit) {
                                    line.setStartPoint(this.firstPosition);
                                    line.setEndPoint(this.secondPosition);
                                }
                                else {
                                    line.setStartPoint(inverseMatrix.Multiply(this.firstPosition));
                                    line.setEndPoint(inverseMatrix.Multiply(this.secondPosition));
                                }
                            }
                        }
                        else {
                            let line1 = null;
                            if (this.isEdit) {
                                line1 = new M3D.Shapes.Line(this.firstPosition, this.secondPosition);
                            }
                            else {
                                line1 = new M3D.Shapes.Line(inverseMatrix.Multiply(this.firstPosition), inverseMatrix.Multiply(this.secondPosition));
                            }
                            line1.setColor(this.leaderColor);
                            line1.setName(M3D.MeasureNames.LINE_LEADER);
                            this.retAnnotation.addLine(line1);
                        }
                    }
                    this.retAnnotation.setHaveEnvelope(this.isEnvelope);
                    this.retAnnotation.setTextFontName(this.fontName);
                    this.retAnnotation.setTextFontColor(this.fontColor);
                    this.retAnnotation.setTextFontSize(this.fontSize);
                    this.retAnnotation.setScene(this.pScene);
                    if (!this.isEdit)
                        this.retAnnotation.setObjectId(this.retAnnotation.getId());
                    //this.retAnnotation.getImageList()[0];
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
            }
            /**
             * @description 设置点位置
             * @param index 点的index position 点的坐标
             * */
            setPointPosition(index, position) {
                let points = this.retAnnotation.getPointList();
                if (points.length > index) {
                    let ptrPoint = points[index];
                    if (ptrPoint) {
                        ptrPoint.setPosition(position);
                    }
                }
            }
            /// 设置批注文字
            setAnnotationText() {
                if (!this.retAnnotation)
                    return;
                if (this.bReImage) {
                    //this.retAnnotation = M3D.Reader.AnnotationFactory.createAnnotation(this.firstPosition,
                    //    this.secondPosition, this.text, this.frameColor, this.fillColor, this.isLeader, this.isEnvelope,
                    //    this.isStub, this.isFixed, this.pScene, this.type, null);
                    ////this.pScene.getShapes().addShape(this.retAnnotation, true);
                    ////this.retAnnotation.setVisible(true);
                    this.retAnnotation = M3D.Reader.AnnotationFactory.createAnnotation(this.pScene, this.retAnnotation);
                }
                else {
                    if (this.retAnnotation && this.retAnnotation.getParentModel()) {
                        // 设置第一个点
                        const worldTransformInverse = this.retAnnotation.getParentModel().getWorldTransform().Inverse();
                        this.retAnnotation.setTextsPos(this.isEdit ? this.secondPosition : worldTransformInverse.Multiply(this.secondPosition));
                    }
                    else {
                        // 设置第一个点
                        this.retAnnotation.setTextsPos(this.secondPosition);
                    }
                }
            }
            /**
             * @description 设置折线位置
             * @param index 点的index position 点的坐标
             * */
            //private setPolyLinePosition(index: number, vecPosition: M3D.M3DMath.Vector3[]) {
            //    let methodName = M3D.Utility.BrowserHelper.getMethodName();
            //    ActionLogger.ACTIONLOGD(M3D.START, ActionNames.ANNOTATIONACTION, methodName);
            //    let polylines = this.retAnnotation.getPolyLineList();
            //    if (polylines.length > index) {
            //        let polyline = polylines[index];
            //        if (polyline) {
            //            polyline.clear();
            //            let position: M3D.M3DMath.Vector3;
            //            for (position of vecPosition) {
            //                polyline.addPoint(position);
            //            }
            //        }
            //    }
            //    ActionLogger.ACTIONLOGD(M3D.END, ActionNames.ANNOTATIONACTION, methodName);
            //}
            /**
             * @description 创建文字
             * @param text 文字 fontSize 文字大小 fontColor 字体颜色 fontName 字体名字
             * */
            //private createComText(text: string, fontSize, fontColor: M3D.M3DMath.Color, fontName: string): M3D.Shapes.ComText {
            //    let methodName = M3D.Utility.BrowserHelper.getMethodName();
            //    ActionLogger.ACTIONLOGD(M3D.START, ActionNames.ANNOTATIONACTION, methodName);
            //    let width, height;
            //    //M3D.Shapes.NoteHelper.getTextRectInfo(text, fontSize, fontName, width, height);
            //    let ct: M3D.Shapes.ComText = new M3D.Shapes.ComText();
            //    let t: M3D.Shapes.Text = new M3D.Shapes.Text();
            //    ct.addText(t);
            //    this.setComText(ct, text, fontSize, fontColor, fontName);
            //    ActionLogger.ACTIONLOGD(M3D.END, ActionNames.ANNOTATIONACTION, methodName);
            //    return ct;
            //}
            /**
             * @description 编辑文字
             * @param text 文字 fontSize 文字大小 fontColor 字体颜色 fontName 字体名字
             * */
            //private setComText(index: number, text: string, fontSize: number, fontColor: M3D.M3DMath.Color, fontName: string);
            //private setComText(comtext: M3D.Shapes.ComText, text: string, fontSize, fontColor: M3D.M3DMath.Color, fontName: string);
            //private setComText(...argv: any[]): any {
            //    if (argv.length == 5) {
            //        let comtexts = this.retAnnotation.getTextList();
            //        if (comtexts.length > argv[0]) {
            //            let comtext = comtexts[argv[0]];
            //            if (comtext) {
            //                this.setComText(comtext, argv[1], argv[2], argv[3], argv[4]);
            //            }
            //        }
            //    }
            //    if (argv.length == 4) {
            //        let comtext: M3D.Shapes.ComText = argv[0];
            //        if (comtext) {
            //            comtext.setFontName(this.fontName);
            //            comtext.setFontSize(this.fontSize);
            //            comtext.setColor(this.fontColor);
            //            comtext.setText(0, this.text);
            //        }
            //    }
            //}
            /**
             * @description 设置关联模型
             * */
            setRelatedModel() {
                let methodName = this.setRelatedModel.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                let ret = false;
                if (this.pScene) {
                    if (this.modelPath && this.modelPath != "") {
                        let parentModel = this.pScene.getShapes().getShapeByPlcPath(this.modelPath);
                        if (parentModel) {
                            this.retAnnotation.setFirstParentPath(parentModel.getPlcPath());
                            this.retAnnotation.setParentModel(parentModel);
                            ret = true;
                        }
                    }
                    else {
                        this.retAnnotation.setFirstParentPath("");
                        this.retAnnotation.setParentModel(null);
                        ret = true;
                    }
                }
                else if (this.parentModelPath && this.parentModelPath !== "") {
                    this.retAnnotation.setFirstParentPath(this.parentModelPath);
                    ret = true;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
                return ret;
            }
            /**
             * @description 批注文件Json读取
             * @param annotationStr 批注内容
             * @param scene 场景
             */
            fromJson(annotationStr) {
                // 若Json数据错误
                if (typeof (annotationStr) != "string") {
                    return null;
                }
                // 若场景对象错误
                if (!(this.pScene instanceof M3D.Scene)) {
                    return null;
                }
                // 判断是否为Json文件
                let isJson;
                let annotationJson;
                if (typeof (annotationStr) == "string") {
                    try {
                        annotationJson = JSON.parse(annotationStr);
                        isJson = true;
                    }
                    catch (error) {
                        isJson = false;
                    }
                }
                // 若内容不是Json格式
                if (!isJson) {
                    return null;
                }
                let type;
                if (annotationJson.type != null && annotationJson.type != undefined) {
                    type = annotationJson.type;
                }
                else {
                    type = -1;
                }
                //// 创建批注
                //let annotation = this.createAnnotation(type, scene);
                //if (!(annotation instanceof M3D.Shapes.Annotation)) {
                //    return null;
                //}
                // 批注ID
                if (typeof (annotationJson.createID) == "number") {
                    this.objID = annotationJson.createID;
                }
                else {
                    // 若批注ID不存在
                    return null;
                }
                // 批注属性
                if (annotationJson.attributes) {
                    let guid = annotationJson.attributes.guid;
                    let author = annotationJson.attributes.author;
                    let createTime = annotationJson.attributes.createTime;
                    // 批注GUID
                    if (typeof (guid) == "string") {
                        this.retAnnotation.setGuid(guid);
                    }
                    // 批注作者
                    if (typeof (author) == "string") {
                        this.retAnnotation.setAuthor(author);
                    }
                    // 批注创建时间
                    if (typeof (createTime) == "string") {
                        this.retAnnotation.setCreateTime(createTime);
                    }
                }
                // 批注文本内容
                if (annotationJson.text != null && annotationJson.text != undefined) {
                    this.retAnnotation.setText(String(annotationJson.text));
                    this.text = String(annotationJson.text);
                }
                else {
                    // 若参数不存在，默认文本内容为空
                    annotationJson.text = "";
                    this.retAnnotation.setText("");
                }
                // 批注leaderLine
                if (typeof (annotationJson.leaderLine) == "boolean") {
                    this.isLeader = annotationJson.leaderLine;
                }
                else {
                    // 若参数不存在，默认设置true
                    annotationJson.leaderLine = true;
                    this.isLeader = annotationJson.leaderLine;
                }
                // 批注envelope
                if (typeof (annotationJson.envelope) == "boolean") {
                    this.isEnvelope = annotationJson.envelope;
                }
                else {
                    // 若参数不存在，默认设置true
                    annotationJson.envelope = true;
                    this.isEnvelope = true;
                }
                // 批注stub
                if (typeof (annotationJson.stub) == "boolean") {
                    this.isStub = annotationJson.stub;
                }
                else {
                    // 若参数不存在，默认设置false
                    annotationJson.stub = false;
                    this.isStub = false;
                }
                // 批注固定
                if (typeof (annotationJson.fixed) == "boolean") {
                    this.isFixed = annotationJson.fixed;
                }
                else {
                    // 若参数不存在，默认设置为false
                    annotationJson.fixed = false;
                    this.isFixed = false;
                }
                // 批注是否显示
                if (typeof (annotationJson.visible) == "boolean") {
                    this.retAnnotation.setVisible(annotationJson.visible);
                }
                else {
                    // 若参数不存在，默认设置为true
                    annotationJson.visible = true;
                    this.retAnnotation.setVisible(true);
                }
                // 批注边框颜色
                let frameColor = annotationJson.frameColor;
                if (frameColor instanceof Array && frameColor.length == 4) {
                    let frameColorType = true;
                    for (let i = 0; i < frameColor.length; i++) {
                        // 判断内容是否符合要求
                        if (typeof (frameColor[i]) != "number" || frameColor[i] < 0 || frameColor[i] > 1) {
                            frameColorType = false;
                            break;
                        }
                    }
                    // 若颜色数值符合要求
                    if (frameColorType) {
                        this.frameColor = new M3D.M3DMath.Color(frameColor[0], frameColor[1], frameColor[2], frameColor[3]);
                    }
                }
                // 批注填充颜色
                let fillColor = annotationJson.fillColor;
                if (fillColor instanceof Array && fillColor.length == 4) {
                    let fillColorType = true;
                    for (let i = 0; i < fillColor.length; i++) {
                        // 判断内容是否复合要求
                        if (typeof (fillColor[i]) != "number" || fillColor[i] < 0 || fillColor[i] > 1) {
                            fillColorType = false;
                            break;
                        }
                    }
                    // 若颜色数值符合要求
                    if (fillColorType) {
                        this.fillColor = new M3D.M3DMath.Color(fillColor[0], fillColor[1], fillColor[2], fillColor[3]);
                    }
                }
                // 批注位置
                let annotationPos = annotationJson.annotationPos;
                if (annotationPos instanceof Array && annotationPos.length == 3) {
                    this.firstPosition = new M3D.M3DMath.Vector3(annotationPos[0], annotationPos[1], annotationPos[2]);
                }
                // 批注中心位置
                let centerPos = annotationJson.centerPos;
                if (centerPos instanceof Array && centerPos.length == 3) {
                    this.firstPosition = new M3D.M3DMath.Vector3(centerPos[0], centerPos[1], centerPos[2]);
                }
                // 如果批注有父级模型，设置父级模型
                if (annotationJson.parentmodel) {
                    this.modelPath = annotationJson.parentmodel;
                }
                // 如果模型固定
                if (this.isFixed) {
                    let broadPos = annotationJson.broadPos;
                    if (broadPos instanceof Array && broadPos.length == 2) {
                        this.retAnnotation.setBroadPos(new M3D.M3DMath.Vector2(broadPos[0], broadPos[1]));
                    }
                }
                else {
                    this.retAnnotation.setIsFixed(false);
                }
                // 如果有样式内容
                if (annotationJson.style) {
                    let style = annotationJson.style;
                    let textColor = style.textColor;
                    let textFontName = style.textFontSize;
                    let textFontSize = style.textFontName;
                    if (textColor instanceof Array && textColor.length == 4) {
                        let textColorType = true;
                        for (let i = 0; i < textColor.length; i++) {
                            // 判断内容是否符合要求
                            if (typeof (textColor[i]) != "number" || textColor[i] < 0 || textColor[i] > 1) {
                                textColorType = false;
                                break;
                            }
                        }
                        // 若颜色数值符合要求
                        if (textColorType) {
                            this.fontColor = new M3D.M3DMath.Color(textColor[0], textColor[1], textColor[2], textColor[3]);
                        }
                    }
                    if (typeof (textFontName) == "string") {
                        this.fontName = textFontName;
                    }
                    if (typeof (textFontSize) == "string") {
                        this.fontSize = Number(textFontSize);
                    }
                }
                return true;
            }
            /**
             * @description 批注文件Json写入
             * @param annotation 批注类
             */
            static toJson(annotation) {
                let annotationJson = "{";
                annotationJson += '"createID":' + annotation.getObjectId() + ",";
                annotationJson += '"type":' + annotation.getAnnotationType() + ",";
                annotationJson += '"text":"' + annotation.getText() + '",';
                annotationJson += '"leaderLine":' + annotation.getHaveLeader() + ",";
                annotationJson += '"envelope":' + annotation.getHaveEnvelope() + ",";
                annotationJson += '"stub":' + annotation.getHaveStub() + ",";
                annotationJson += '"fixed":' + annotation.getIsFixed() + ",";
                annotationJson += '"visible"' + annotation.isVisible() + ",";
                // 如果有父级模型
                if (annotation.getParentModel()) {
                    annotationJson += '"parentmodel":"' + annotation.getParentModel().getPlcPath() + '",';
                }
                // 边框颜色
                annotationJson += '"frameColor":[' +
                    annotation.getFrameColor().r + ',' +
                    annotation.getFrameColor().g + ',' +
                    annotation.getFrameColor().b + ',' +
                    annotation.getFrameColor().a + '],';
                // 填充颜色
                annotationJson += '"fillColor":[' +
                    annotation.getFillColor().r + ',' +
                    annotation.getFillColor().g + ',' +
                    annotation.getFillColor().b + ',' +
                    annotation.getFillColor().a + '],';
                // 批注位置
                annotationJson += '"annotationPos":[' +
                    annotation.getPosition().x + ',' +
                    annotation.getPosition().y + ',' +
                    annotation.getPosition().z + '],';
                // 批注中心位置
                annotationJson += '"centerPos":[' +
                    annotation.getCenterPos().x + ',' +
                    annotation.getCenterPos().y + ',' +
                    annotation.getCenterPos().z + '],';
                // 若批注固定，批注固定位置
                if (annotation.getBroadPos()) {
                    annotationJson += '"broadPos":[' +
                        annotation.getBroadPos().x + ',' +
                        annotation.getBroadPos().y + '],';
                }
                // 批注属性（GUID，批注作者，批注创建时间）
                annotationJson += '"attributes":{' +
                    '"guid":"' + annotation.getGuid() + '",' +
                    '"author":"' + annotation.getAuthor() + '",' +
                    '"createTime":"' + annotation.getCreateTime() + '"},';
                // 文本颜色、文本字体、文本字号
                annotationJson += '"style":{';
                if (annotation.getTextFontColor()) {
                    annotationJson += '"textColor":[' +
                        annotation.getTextFontColor().r + ',' +
                        annotation.getTextFontColor().g + ',' +
                        annotation.getTextFontColor().b + ',' +
                        annotation.getTextFontColor().a + '],';
                }
                if (annotation.getTextFontName()) {
                    annotationJson += '"textFontName":"' + annotation.getTextFontName() + '",';
                }
                if (annotation.getTextFontSize()) {
                    annotationJson += '"textFontSize":' + annotation.getTextFontSize() + ',';
                }
                annotationJson += "}},";
                return annotationJson;
            }
        }
        Actions.AnnotationAction = AnnotationAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        const DEFAULIT_TIMES = 30;
        /**
         * @memberof SView.Actions
         * @description 基本视图方向
         * @export
         * @enum {number}
         */
        let EAxisName;
        (function (EAxisName) {
            EAxisName[EAxisName["X"] = 0] = "X";
            EAxisName[EAxisName["Y"] = 1] = "Y";
            EAxisName[EAxisName["Z"] = 2] = "Z";
            EAxisName[EAxisName["MX"] = 3] = "MX";
            EAxisName[EAxisName["MY"] = 4] = "MY";
            EAxisName[EAxisName["MZ"] = 5] = "MZ";
        })(EAxisName = Actions.EAxisName || (Actions.EAxisName = {}));
        /**
         * @memberof SView.Actions
         * @description 切换基本视图Action
         * @export
         * @class BaseViewAction
         * @extends {M3D.Actions.Action}
         */
        class BaseViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of BaseViewAction.
             * @memberof BaseViewAction
             */
            constructor() {
                super();
                this.viewType = Actions.BaseViewType.FRONT;
                this.timer = new M3D.Utility.Timer();
                this.animationmvMatrix = new M3D.M3DMath.Matrix4();
            }
            /**
             * @public
             * @description 设置基本视图类型
             * @param {BaseViewType} viewType
             * @memberof BaseViewAction
             */
            setViewType(viewType) {
                this.viewType = viewType;
            }
            /**
             * @public
             * @description 获取基本视图类型
             * @return {BaseViewType}
             * @memberof BaseViewAction
             */
            getViewType() {
                return this.viewType;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof BaseViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof BaseViewAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @description 执行
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof BaseViewAction
             */
            onExecute() {
                if (!this.scene || !this.scene.getCurrentViewPort()) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                if (!this.showView(this.viewType)) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 显示View
             * @private
             * @param {BaseViewType} viewType
             * @param {boolean} [allowRotate=true]
             * @param {boolean} [allowTran=false]
             * @param {boolean} [allowScale=false]
             * @param {boolean} [useAni=true]
             * @return {*}  {boolean}
             * @memberof BaseViewAction
             */
            showView(viewType, allowRotate = true, allowTran = false, allowScale = false, useAni = true) {
                this.bAllowRotate = allowRotate;
                this.bAllowScale = allowScale;
                this.bAllowTran = allowTran;
                this.useAni = useAni;
                let ret = this.handleStandardView();
                return ret;
            }
            /**
             * @private
             * @return {*}  {boolean}
             * @memberof BaseViewAction
             */
            handleStandardView() {
                switch (this.viewType) {
                    case Actions.BaseViewType.FRONT:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.FRONT_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.BACK:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.BACK_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.LEFT:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.LEFT_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.RIGHT:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.RIGHT_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.TOP:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.TOP_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.BOTTOM:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.BOTTOM_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.ISOMETRIC:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.ISOMETRIC_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    case Actions.BaseViewType.DEFAULT:
                        return this.createPerspectiveAnimation(M3D.Actions.CameraAction.DEFAULT_MVMATRIX(), this.bAllowRotate, this.bAllowTran, this.bAllowScale);
                    default:
                        return false;
                }
            }
            /**
             * @private
             *@description 获取动画执行状态
             * @return {boolean}
             * @memberof BaseViewAction
             */
            getAnimationState() {
                return this.timer.isStart();
            }
            /**
             * @private
             * @description 结束动画
             * @return {boolean}
             * @memberof BaseViewAction
             */
            finishAnimation() {
                if (this.timer.isStart()) {
                    this.timer.stopTimer();
                }
                this.currentAniFrame = 0;
                this.animationKeepMsecond = DEFAULIT_TIMES;
                return true;
            }
            /**
             * @private
             * @param {M3D.M3DMath.Matrix4} to
             * @param {boolean} [allowRotate=true]
             * @param {boolean} [allowTran=false]
             * @param {boolean} [allowScale=false]
             * @return {boolean}
             * @memberof BaseViewAction
             */
            createPerspectiveAnimation(to, allowRotate = true, allowTran = false, allowScale = false) {
                //正在进行动画
                if (this.getAnimationState()) {
                    //结束当前动画
                    this.finishAnimation();
                }
                let camera = this.scene.getCurrentViewPort().getCamera();
                //开始新动画
                this.currentAniFrame = 0;
                const q = new M3D.M3DMath.Quaternion();
                if (camera.getUpDir().Equals(M3D.M3DMath.Vector3.DOWN())) { //-y
                    this.getAxisTransFormQuaternion(EAxisName.MY, EAxisName.Z, q);
                    const matrix = new M3D.M3DMath.Matrix4(new M3D.M3DMath.Vector3(0, 0, 0), q, 1.0);
                    to = to.Multiply(matrix);
                }
                else if (camera.getUpDir().Equals(M3D.M3DMath.Vector3.LEFT())) { //-x
                    this.getAxisTransFormQuaternion(EAxisName.MX, EAxisName.Y, q);
                    const matrix = new M3D.M3DMath.Matrix4(new M3D.M3DMath.Vector3(0, 0, 0), q, 1.0);
                    to = to.Multiply(matrix);
                }
                else if (camera.getUpDir().Equals(M3D.M3DMath.Vector3.RIGHT())) { // +x
                    this.getAxisTransFormQuaternion(EAxisName.X, EAxisName.Z, q);
                    const matrix = new M3D.M3DMath.Matrix4(new M3D.M3DMath.Vector3(0, 0, 0), q, 1.0);
                    to = to.Multiply(matrix);
                }
                else if (camera.getUpDir().Equals(M3D.M3DMath.Vector3.FORWARD())) { //+z
                    this.getAxisTransFormQuaternion(EAxisName.Z, EAxisName.Y, q);
                    const matrix = new M3D.M3DMath.Matrix4(new M3D.M3DMath.Vector3(0, 0, 0), q, 1.0);
                    to = to.Multiply(matrix);
                }
                else if (camera.getUpDir().Equals(M3D.M3DMath.Vector3.BACK())) { // -z
                    this.getAxisTransFormQuaternion(EAxisName.MZ, EAxisName.X, q);
                    const matrix = new M3D.M3DMath.Matrix4(new M3D.M3DMath.Vector3(0, 0, 0), q, 1.0);
                    to = to.Multiply(matrix);
                }
                this.animationTo = to;
                camera.setRotateCenter(this.getScene().getSceneBox().Center());
                this.animationmvMatrix.SetRotation(camera.getWorldRotation().Inverse().RotationMatrix());
                this.animationmvMatrix.SetTranslation(camera.getPosition());
                this.animationmvMatrix.SetScale(camera.getZoom());
                return this.startAnimation();
            }
            /**
             * @private
             * @return {boolean}
             * @memberof BaseViewAction
             */
            startAnimation() {
                let to = this.animationTo;
                if (this.bAllowRotate) {
                    this.setRotationToCamera(to.Rotation());
                }
                if (this.bAllowScale) {
                    this.applyScaleToSelectedNode(to.Scale().x);
                }
                if (this.bAllowTran) {
                    this.applyMovementToSelectedNode(to.Translation());
                }
                this.bAllowRotate = true;
                this.scene.getCurrentViewPort().getInput().resetInputHandler();
                return true;
            }
            /**
             * @private
             * @param {M3D.M3DMath.Quaternion} rotation
             * @memberof BaseViewAction
             */
            setRotationToCamera(rotation) {
                if (this.scene && this.scene.getTopModel()) {
                    let deltaRotation = rotation.Inverse();
                    let camera = this.scene.getCurrentViewPort().getCamera();
                    let rot = camera.getWorldRotation().Inverse().Multiply(deltaRotation);
                    camera.rotateAroundCenter(rot, 2);
                }
            }
            /**
             * @private
             * @param {number} scale
             * @memberof BaseViewAction
             */
            applyScaleToSelectedNode(scale) {
                if (this.scene && this.scene.getTopModel()) {
                    let camera = this.scene.getCurrentViewPort().getCamera();
                    camera.setZoom(1 / scale);
                }
            }
            /**
             * @private
             * @param {M3D.M3DMath.Vector3} mov
             * @memberof BaseViewAction
             */
            applyMovementToSelectedNode(mov) {
                if (this.scene && this.scene.getTopModel()) {
                    let camera = this.scene.getCurrentViewPort().getCamera();
                    camera.setWorldPosition(mov);
                }
            }
            /**
             * @private
             * @param {EAxisName} up
             * @param {EAxisName} right
             * @param {M3D.M3DMath.Quaternion} q
             * @return {boolean}
             * @memberof BaseViewAction
             */
            getAxisTransFormQuaternion(up, right, q) {
                switch (up) {
                    case EAxisName.X:
                        {
                            if (right === EAxisName.X) {
                                return false;
                            }
                            else if (right === EAxisName.Y) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(0, 1, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.Z) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MX) {
                                return false;
                            }
                            else if (right === EAxisName.MY) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.MZ) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                        }
                        break;
                    case EAxisName.Y:
                        {
                            if (right === EAxisName.X) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.Y) {
                                return false;
                            }
                            else if (right === EAxisName.Z) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.MX) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.MY) {
                                return false;
                            }
                            else if (right === EAxisName.MZ) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                        }
                        break;
                    case EAxisName.Z:
                        {
                            if (right === EAxisName.X) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.Y) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.Z) {
                                return false;
                            }
                            else if (right === EAxisName.MX) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(180, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MY) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MZ) {
                                return false;
                            }
                        }
                        break;
                    case EAxisName.MX:
                        {
                            if (right === EAxisName.X) {
                                return false;
                            }
                            else if (right === EAxisName.Y) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.Z) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(270, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MX) {
                                return false;
                            }
                            else if (right === EAxisName.MY) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(180, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MZ) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                        }
                        break;
                    case EAxisName.MY:
                        {
                            if (right === EAxisName.X) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.Y) {
                                return false;
                            }
                            else if (right === EAxisName.Z) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MX) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(180, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MY) {
                                return false;
                            }
                            else if (right === EAxisName.MZ) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(180, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 1, 0));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                        }
                        break;
                    case EAxisName.MZ:
                        {
                            if (right === EAxisName.X) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                q.copyFrom(q1);
                                return true;
                            }
                            else if (right === EAxisName.Y) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(270, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.Z) {
                                return false;
                            }
                            else if (right === EAxisName.MX) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(180, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MY) {
                                let q1 = new M3D.M3DMath.Quaternion();
                                q1.FromAngleAxis(90, new M3D.M3DMath.Vector3(1, 0, 0));
                                let q2 = new M3D.M3DMath.Quaternion();
                                q2.FromAngleAxis(90, new M3D.M3DMath.Vector3(0, 0, 1));
                                q.copyFrom(q1.Multiply(q2));
                                return true;
                            }
                            else if (right === EAxisName.MZ) {
                                return false;
                            }
                        }
                        break;
                }
            }
        }
        Actions.BaseViewAction = BaseViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建属性Action
         * @export
         * @class CreatePropertyAction
         * @extends {M3D.Actions.Action}
         */
        class CreatePropertyAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of CreatePropertyAction.
             * @memberof CreatePropertyAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof CreatePropertyAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof CreatePropertyAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 获取属性
             * @param {M3D.Shapes.Shape} shape
             * @return {string}
             * @memberof CreatePropertyAction
             */
            getProperty(shape) {
                let str = "";
                let shapeptr = shape;
                let type = shapeptr.getShapeType();
                switch (type) {
                    case M3D.Shapes.ShapeType.POINT:
                    case M3D.Shapes.ShapeType.EDGE:
                    case M3D.Shapes.ShapeType.SECTION_EDGE:
                    case M3D.Shapes.ShapeType.SECTION_PLANE:
                    case M3D.Shapes.ShapeType.FACE:
                        str = this.getGeometryProperty(shape, type);
                        break;
                    case M3D.Shapes.ShapeType.MODEL:
                        str = this.getModelProperty(shape);
                        break;
                    default:
                        break;
                }
                return str;
            }
            /**
             * @public
             * @description 获取模型属性
             * @param {M3D.Shapes.Shape} shape 模型对象
             * @return {string}
             * @memberof CreatePropertyAction
             */
            getModelProperty(shape) {
                let str = "";
                let model = shape;
                if (!model) {
                    let topModel = this.scene.getTopModel();
                    if (topModel) {
                        let subModelArr = topModel.getChildren();
                        if (subModelArr.length > 0) {
                            model = subModelArr[0];
                        }
                    }
                }
                if (model) {
                    model.getModelProperties().then((propertyStr) => {
                        str = propertyStr;
                        return str;
                    });
                }
                return str;
            }
            /**
             * @public
             * @description 获取几何属性
             * @param {M3D.Shapes.Shape} shape Shape对象
             * @param {M3D.Shapes.ShapeType} type 类型，可为M3D.Shapes.ShapeType.FACE、M3D.Shapes.ShapeType.SECTION_PLANE、M3D.Shapes.ShapeType.EDGE、M3D.Shapes.ShapeType.SECTION_EDGE、M3D.Shapes.ShapeType.POINT
             * @return {string}
             * @memberof CreatePropertyAction
             */
            getGeometryProperty(shape, type) {
                let str = "";
                if (type === M3D.Shapes.ShapeType.FACE || type === M3D.Shapes.ShapeType.SECTION_PLANE) {
                    let face = shape;
                    let nameStr = "Name"; //language.GetCurrentTypeValue("ModelPropertiesName");
                    let ID = "ID"; //language.GetCurrentTypeValue("ModelPropertiesID");
                    let Color = "Color"; //language.GetCurrentTypeValue("ModelPropertiesColor");
                    let LOD0PatchNumber = "LOD0PatchCount"; //language.GetCurrentTypeValue("ModelPropertiesLOD0PatchNumber");
                    let LOD1PatchNumber = "LOD1PatchNumber"; //language.GetCurrentTypeValue("ModelPropertiesLOD1PatchNumber");
                    str = this.addProperty(ID, String(face.getId()), str);
                    let renderColor = face.getRenderColor();
                    let colorStr = String(renderColor.r.toFixed(2)) + "," + String(renderColor.g.toFixed(2)) + ","
                        + String(renderColor.b.toFixed(2)) + "," + String(renderColor.a.toFixed(2));
                    str = this.addProperty(Color, colorStr, str);
                    let result = face.getVolumeAndArea();
                    str = this.addProperty(LOD0PatchNumber, String(result[2]), str);
                    let areaKey = "Area"; //language.GetCurrentTypeValue("FacePropertiesArea");
                    let area = result[0];
                    let values = new Array();
                    values.push(area);
                    let areaStr = String(values[0].toFixed(2));
                    str = this.addProperty(areaKey, String(areaStr), str);
                }
                else if (type === M3D.Shapes.ShapeType.EDGE || type === M3D.Shapes.ShapeType.SECTION_EDGE) {
                    let edge = shape;
                    str = this.getLineProperty(edge, str);
                }
                else if (type == M3D.Shapes.ShapeType.POINT) {
                    let point = shape;
                    if (point.getPosition().x != 0 && point.getPosition().y != 0 && point.getPosition().z != 0) {
                        let xStr = "X:" + point.getPosition().x.toFixed(2) + ", " + "Y:" + point.getPosition().y.toFixed(2) + ", " + "Z:" + point.getPosition().z.toFixed(2);
                        str = this.addProperty("Point", xStr, str);
                    }
                }
                return str;
            }
            /**
             * @public
             * @description 获取线属性
             * @param {M3D.Shapes.Edge} edge
             * @param {string} propertyStr 可不传
             * @return {string}
             * @memberof CreatePropertyAction
             */
            getLineProperty(edge, propertyStr) {
                propertyStr = "";
                let length = 0;
                if (edge && (edge.getShapeType() == M3D.Shapes.ShapeType.EDGE ||
                    (M3D.Config.Parameters.getInstance().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE) && edge.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE))) {
                    //得到边界线数据
                    let geo = edge.getGeometryFeature();
                    if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let lineFeatrue = geo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                        let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                        length = (pntStartPoint.Sub(pntEndPoint)).Length();
                        let typestr = "Line";
                        let values = new Array();
                        values.push(length);
                        let distanceStr = String(values[0].toFixed(2));
                        propertyStr = this.addProperty(typestr, distanceStr, propertyStr);
                    }
                    else if (geo != null && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let ellipseAttribute = geo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        M3D.Shapes.ShapeHelper.transform(ellipseAttribute, worldMatrix);
                        let pntStartPoint = ellipseAttribute.getStartPoint(); //!< Line起点
                        let pntEndPoint = ellipseAttribute.getEndPoint(); //!< Line终点
                        let circleCenter = ellipseAttribute.getCenter(); //圆心
                        let radius = ellipseAttribute.getMajorRadius(); //长半轴 当 半径
                        let xVector = new M3D.M3DMath.Vector3(), yVector = new M3D.M3DMath.Vector3(), zVector = new M3D.M3DMath.Vector3();
                        let results = ellipseAttribute.getXYZDir();
                        xVector = results[0];
                        yVector = results[1];
                        zVector = results[2];
                        //求弧长
                        let points = [];
                        let indexCount = 0;
                        if (M3D.Config.Parameters.getInstance().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE) && edge.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE) {
                            points = edge.getSectionLine().getData();
                            indexCount = points.length;
                        }
                        else {
                            let vertexSets = this.scene.getVertexSetManager();
                            let body = edge.getBody();
                            let bodyVertexHeader = vertexSets.getVertexHeader(body.getProto());
                            let edgeVertexHeader = vertexSets.getVertexHeader(edge.getProto());
                            let edgeVertexSet = edgeVertexHeader.vertexSet;
                            let bodyVertexSet = bodyVertexHeader.vertexSet;
                            indexCount = edge.getIndexLength();
                            let indexOffset = edge.getIndexOffset();
                            let meshBuffer = bodyVertexSet.getMeshArray();
                            let indexBuffer = edgeVertexSet.getMeshArray();
                            let meshArray = new Float32Array(meshBuffer, bodyVertexHeader.vertexOffset, bodyVertexHeader.vertexLength);
                            let indexArray = new Uint32Array(indexBuffer, indexOffset, indexCount);
                            for (let index = 0; index < indexCount; index++) {
                                let p1 = new M3D.M3DMath.Vector3();
                                p1.x = meshArray[indexArray[index] * 3 + 0];
                                p1.y = meshArray[indexArray[index] * 3 + 1];
                                p1.z = meshArray[indexArray[index] * 3 + 2];
                                points.push(p1);
                            }
                        }
                        //let edgeData = MeshHelper.getEdgeData(<Edge>firstShape);
                        let minIndex = (indexCount / 2).toFixed(0);
                        let arcLength = 0; //弧长
                        let startVector = pntStartPoint.Sub(circleCenter).Clone();
                        let endVector = pntEndPoint.Sub(circleCenter).Clone();
                        let midVector = worldMatrix.Multiply((points[minIndex])).Sub(circleCenter);
                        let crossVector = startVector.CrossProduct(endVector);
                        let crossVectorMid = startVector.CrossProduct(midVector);
                        let angle = crossVector.DotProduct(crossVectorMid);
                        //let dotValue = crossVector.DotProduct(zVector);
                        let alpha = Math.acos(startVector.DotProduct(endVector.Divide(startVector.Length()
                            * endVector.Length())));
                        if (pntStartPoint.Equals(pntEndPoint)) {
                            //说明是个完成的圆
                            arcLength = 2 * Math.PI * radius;
                        }
                        else if (angle > 0) {
                            //说明与z方向同向
                            arcLength = alpha * radius;
                        }
                        else {
                            //说明方向相反
                            arcLength = (2 * Math.PI - alpha) * radius;
                        }
                        //构造显示文本
                        let radiusStr = String(radius.toFixed(2));
                        let centerStrX = String(circleCenter.x.toFixed(2));
                        let centerStrY = String(circleCenter.y.toFixed(2));
                        let centerStrZ = String(circleCenter.z.toFixed(2));
                        let centerStr = centerStrX + "," + centerStrY + "," + centerStrZ;
                        let arcLengthStr = String(arcLength.toFixed(2));
                        let cstr = "CircleCenter"; //language.GetCurrentTypeValue(
                        let rstr = "CircleRadius"; //language.GetCurrentTypeValue(
                        let arcstr = "CircleArcLength"; //language.GetCurrentTypeValue(
                        //edge.ClearProperties();
                        let values = new Array();
                        values.push(arcLength);
                        arcLengthStr = String(values[0].toFixed(2));
                        propertyStr = this.addProperty(cstr, centerStr, propertyStr);
                        propertyStr = this.addProperty(rstr, radiusStr, propertyStr);
                        propertyStr = this.addProperty(arcstr, arcLengthStr, propertyStr);
                        //let colorStr = String(edge.getColor().r) + "," + String(this.Color.g) + ","
                        //    + String(this.Color.b) + "," + String(this.Color.a);
                        //propertyStr = this.addProperty(Color, colorStr, propertyStr);
                    }
                    else {
                        //无几何属性的
                        if (edge.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE) {
                            let pntPoints = edge.getSectionLine().getData();
                            let dataLength = pntPoints.length;
                            for (let i = 0; i < dataLength - 1; i++) {
                                let point1 = pntPoints[i];
                                let point2 = pntPoints[i + 1];
                                length += (point2.Sub(point1)).Length();
                            }
                        }
                        if (edge.getShapeType() === M3D.Shapes.ShapeType.EDGE) {
                            let vertexSets = this.scene.getVertexSetManager();
                            let body = edge.getBody();
                            let bodyVertexHeader = vertexSets.getVertexHeader(body.getProto());
                            let edgeVertexHeader = vertexSets.getVertexHeader(edge.getProto());
                            let edgeVertexSet = edgeVertexHeader.vertexSet;
                            let bodyVertexSet = bodyVertexHeader.vertexSet;
                            let indexCount = edge.getIndexLength();
                            let indexOffset = edge.getIndexOffset();
                            let meshBuffer = bodyVertexSet.getMeshArray();
                            let indexBuffer = edgeVertexSet.getMeshArray();
                            let meshArray = new Float32Array(meshBuffer, bodyVertexHeader.vertexOffset, bodyVertexHeader.vertexLength);
                            let indexArray = new Uint32Array(indexBuffer, indexOffset, indexCount);
                            let pntPoints = [];
                            for (let index = 0; index < indexCount; index++) {
                                let p1 = new M3D.M3DMath.Vector3();
                                p1.x = meshArray[indexArray[index] * 3 + 0];
                                p1.y = meshArray[indexArray[index] * 3 + 1];
                                p1.z = meshArray[indexArray[index] * 3 + 2];
                                pntPoints.push(p1);
                            }
                            for (let i = 0; i < indexCount - 1; i += 1) {
                                let point1 = pntPoints[i];
                                let point2 = pntPoints[i + 1];
                                length += (point2.Sub(point1)).Length();
                            }
                        }
                        if (length > 0) {
                            let lengthstr = String(length);
                            let typestr = "Line"; //language.GetCurrentTypeValue("LinePropertiesLength");
                            let values = new Array();
                            values.push(length);
                            lengthstr = String(values[0].toFixed(2));
                            propertyStr = this.addProperty(typestr, lengthstr, propertyStr);
                        }
                    }
                    let ID = "ID";
                    propertyStr = this.addProperty(ID, String(edge.getId()), propertyStr);
                }
                else {
                    propertyStr = "";
                }
                return propertyStr;
            }
            /**
             * @public
             * @description 添加属性
             * @param {string} key 属性的key
             * @param {string} value 属性值
             * @param {string} str 属性字符串
             * @return {string}
             * @memberof CreatePropertyAction
             */
            addProperty(key, value, str) {
                if (str.indexOf(";:;" + key + "::") > 0 || str.indexOf(key + "::") === 0) {
                    return;
                }
                if (str.length != 0) {
                    str += ";:;";
                }
                str = str + (key + "::" + value);
                return str;
            }
            /**
             * @private
             * @return {M3D.Actions.ActionState}
             * @memberof CreatePropertyAction
             */
            onExecute() {
                let retState = M3D.Actions.ActionState.Action_Error;
                return retState;
            }
        }
        Actions.CreatePropertyAction = CreatePropertyAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 删除视图Action
         * @export
         * @class DeleteModelViewAction
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class DeleteModelViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of DeleteModelViewAction.
             * @memberof DeleteModelViewAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof DeleteModelViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置参数
             * @param {string} currentModelViewId 视图id
             * @param {string} currentModelViewServerId 在线视图id
             * @return {boolean}
             * @memberof DeleteModelViewAction
             */
            setParameter(currentModelViewId, currentModelViewServerId) {
                if (!currentModelViewId || !currentModelViewServerId || currentModelViewServerId.length <= 0) {
                    return false;
                }
                this.currentModelViewId = currentModelViewId;
                this.currentModelViewServerId = currentModelViewServerId;
                return true;
            }
            /**
             * @protected
             * @return {*}  {M3D.Actions.ActionState}
             * @memberof DeleteModelViewAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                //ActionLogger.ACTIONLOGD(M3D.START, ActionNames.DISTANCEMEASUREACTION, methodName);
                let that = this;
                if (!this.scene) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let retState = M3D.Actions.ActionState.Action_Success;
                let uniqueid = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.uniqueId);
                let fileId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.fileId);
                M3D.Utility.HttpMessageHandler.deleteView(fileId, that.currentModelViewServerId, uniqueid).then((response) => {
                    let jsonObject = JSON.parse(response);
                    let result = jsonObject["return"];
                    if (result.toString() !== M3D.Actions.ActionState.Action_Success.toString()) {
                        retState = M3D.Actions.ActionState.Action_Error;
                    }
                    let message = new SView.ModelViewActionMessage(SView.ActionMessageType.RemoveModelView, retState.toString(), result.toString(), that.currentModelViewServerId, that.currentModelViewId, "");
                    that.send(message);
                }).catch((error) => {
                    //ActionLogger.ACTIONLOGD(M3D.END, ActionNames.DISTANCEMEASUREACTION, methodName);
                });
                //ActionLogger.ACTIONLOGD(M3D.END, ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof DeleteModelViewAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof DeleteModelViewAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.DeleteModelViewAction = DeleteModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @description 弧度测量
         * @extends {M3D.Actions.Action}
         */
        class DiametreMeasureAction extends M3D.Actions.Action {
            // 构造函数
            constructor() {
                super();
                this.objID = 0;
                this.modelID = "";
                this.retMeasure = null;
                this.type = 0;
                this.firstPosition = M3D.M3DMath.Vector3.ZERO();
                this.secondPosition = M3D.M3DMath.Vector3.ZERO();
                this.text = "";
                this.fontColor = M3D.M3DMath.Color.BLACK();
                this.fillColor = M3D.M3DMath.Color.BLUE();
                this.isLeader = true;
                this.isEnvelope = true;
                this.isFixed = false;
                this.isInTop = true;
                this.isStub = false;
                this.scene = null;
                this.readerState = false;
            }
            setParameter(objID, type, firstPosition, secondPosition, text, fontColor, fontName, fontSize, frameColor, fillColor, isLeader, isEnvelope, isFixed, inTop, isStub, parentModelID) {
                this.objID = objID;
                this.type = type;
                this.firstPosition = firstPosition;
                this.secondPosition = secondPosition;
                this.text = text;
                this.fontColor = fontColor;
                this.fontName = fontName;
                this.fontSize = fontSize;
                this.frameColor = frameColor;
                this.fillColor = fillColor;
                this.isLeader = isLeader;
                this.isEnvelope = isEnvelope;
                this.isFixed = isFixed;
                this.isInTop = inTop;
                this.isStub = isStub;
                this.modelID = parentModelID;
            }
            setParentModelPath(plcPath) {
                this.parentModelPath = plcPath;
            }
            SetScene(scene) {
                this.scene = scene;
            }
            /**
             * 设置是否为读取状态
             * @param state
             */
            setReaderState(state) {
                this.readerState = state;
            }
            /**
             * 获取新建的对象
             * @returns  {M3D.Shapes.MeasureDiameter }
             */
            getShape() {
                return this.retMeasure;
            }
            /**
            * 执行
            * @returns Action状态
            * */
            onExecute() {
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * 创建完成测量对象
             */
            completeMeasure() {
                let state = false;
                if (this.nativeMeasure != null) {
                    let shapeId = this.m_Shape;
                    let measureType = this.measureType;
                    let anchorX = this.anchorX;
                    let anchorY = this.anchorY;
                }
                return state;
            }
            /**
             * 创建临时状态
             */
            createTempMeasure() {
                let createState = false;
                let measure = null;
                let nativeMeaureType = this.measureType;
                let shapeId = this.m_Shape;
                return createState;
            }
            getAnchorX() {
                return this.anchorX;
            }
            setAnchorX(anchorX) {
                this.anchorX = anchorX;
            }
            getAnchorY() {
                return this.anchorY;
            }
            setAnchorY(anchorY) {
                this.anchorY = anchorY;
            }
            getMeasureType() {
                return this.measureType;
            }
            setMeasureType(type) {
                this.measureType = type;
            }
            /**
             * 根据参数创建测量
             */
            createDiametre() {
                return false;
            }
            /**
             * 根据参数编辑测量
             */
            editDiametre() {
                return false;
            }
            /**
             * 添加节点到场景树
             */
            addToScene() {
                let ret = false;
                if (this.retMeasure != null && this.readerState && this.scene != null) {
                    let shapeSet = this.scene.getShapes();
                    shapeSet.addShape(this.retMeasure);
                    this.scene.refresh();
                    ret = true;
                }
                else {
                    //TODO
                }
                return ret;
            }
            /**
             * 设置基本参数
             */
            setDiametreInfo() {
                //TODO
            }
            /**
             * 设置点位置
             * @param index 点的index
             * @param position 点的坐标
             */
            setPointPosition(index, position) {
                //TODO
            }
            /**
             * 设置折线位置
             * @param index
             * @param vecPosition 点的坐标
             */
            setPolyLinePosition(index, vecPosition) {
                //TODO
            }
            /**
             * 创建文字
             * @param text 文字
             * @param fontSize 字体大小
             * @param fontColor 字体颜色
             * @param fontName 字体名字
             */
            createComText(text, fontSize, fontColor, fontName) {
                return new M3D.Shapes.ComText();
            }
            setComText(...param) {
                //TODO
            }
            // 设置关联模型
            setRelatedModel() {
                return false;
            }
        }
        Actions.DiametreMeasureAction = DiametreMeasureAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @extends {M3D.Actions.Action}
         */
        class DistanceMeasureAction extends M3D.Actions.Action {
            /**
             * 构造函数
             * */
            constructor() {
                super();
                this.measure = null;
                this.measureType = M3D.Shapes.MeasureType.BASE_MEASURE;
                this.objID = 0;
                this.parentModelID = 0;
                this.measureType = M3D.Shapes.MeasureType.PNT_PNT_DISTANCE;
                this.titleTextColor = M3D.M3DMath.Color.BLACK();
                this.fontSize = M3D.ANNOTATION_TEXT_FONT_SIZE;
                this.fontName = "";
                this.backGroundColor = M3D.M3DMath.Color.BLUE();
                this.isShowMaxDistance = false;
                this.isShowMinDistance = false;
                this.maxDistanceColor = M3D.M3DMath.Color.BLACK();
                this.minDistanceColor = M3D.M3DMath.Color.BLACK();
                this.backGroundColor = M3D.M3DMath.Color.BLACK();
                this.resultTextColor = M3D.M3DMath.Color.BLACK();
                this.pScene = null;
                this.firstPosition = null;
                this.secondPosition = null;
                this.thirdPosition = null;
                this.fourthPosition = null;
                this.markingColor = new M3D.M3DMath.Color(255.0 / 255.0, 117.0 / 255.0, 24.0 / 255.0);
                this.leaderColor = new M3D.M3DMath.Color(241.0 / 255.0, 167.0 / 255.0, 71.0 / 255.0);
                this.squareBoxColor = new M3D.M3DMath.Color(255.0 / 255.0, 194.0 / 255.0, 77.0 / 255.0);
            }
            /**
            * @description 设置第一个选中的Shape
            * @param firstID 第一个选中的Shape ID
            */
            setFirstShape(firstID) {
                this.firstShapeID = firstID;
            }
            /**
            * @description 设置第二个选中的Shape
            * @param firstID 第二个选中的Shape ID
            */
            setSecondShape(secondID) {
                this.secondShapeID = secondID;
            }
            /**
            * @description 设置参数
            * @param  objID  测量ID ，type 类型，titleColor 标题颜色 ，textColor 文本颜色，fontName 字体名称，fontSize 字体大小，titleFormat 标题格式，backGroundColor 背景颜色，maxDistanceColor 最远距离颜色，minDistanceColor 最近距离颜色，measurementUnit 测量单位，isShowMaxDistance 是否展示最远距离，isShowMinDistance 是否展示最远距离 ，parentModelID  模型ID
            */
            setParameter(objID, type, firstShape, firstPosition, secondShape, secondPosition, titleTextColor, resultTextColor, fontName, fontSize, titleFormat, backGroundColor, maxDistanceColor, minDistanceColor, measurementUnit, isShowMaxDistance, isShowMinDistance, model1, model2, fourthPosition) {
                this.objID = objID ? objID : 0;
                this.measureType = type;
                this.titleTextColor = titleTextColor;
                this.fontSize = fontSize;
                this.fontName = fontName;
                this.backGroundColor = backGroundColor;
                this.isShowMaxDistance = isShowMaxDistance;
                this.isShowMinDistance = isShowMinDistance;
                this.maxDistanceColor = maxDistanceColor;
                this.minDistanceColor = minDistanceColor;
                this.resultTextColor = resultTextColor;
                this.firstShape = firstShape;
                this.firstPosition = firstPosition ? firstPosition : this.firstPosition;
                this.secondShape = secondShape;
                this.secondPosition = secondPosition ? secondPosition : this.secondPosition;
                this.fourthPosition = fourthPosition;
                this.modelPath1 = model1;
                this.modelPath2 = model2;
                return true;
            }
            /**
            * @description 设置当前视口
            * @param camera 摄像机对象
            */
            setScene(pScene) {
                this.pScene = pScene;
            }
            /**
            * @description 获取当前视口
            */
            getScene() {
                return this.pScene;
            }
            /**
            * @description 获取新建的测量对象
            * @returns  测量对象
            */
            getShape() {
                return this.measure;
            }
            /**
             * @description 执行
             * @returns Action状态
             * */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                retState = this.doMeasure();
                if (retState != M3D.Actions.ActionState.Action_Success)
                    return retState;
                if (this.objID == 0) {
                    if (!this.addToScene())
                        retState = M3D.Actions.ActionState.Action_Error;
                }
                this.pScene.getViewPorts().markDirty();
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @description 根据参数创建距离对象
             * @returns Action状态
             * */
            doMeasure() {
                let methodName = this.doMeasure.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.objID == 0)
                    this.measure = new M3D.Shapes.MeasureDistance();
                else {
                    let measures = this.pScene.getShapes();
                    this.measure = measures.getShape(this.objID);
                }
                this.distances = [];
                this.setBaseInfo();
                let bFlag = false;
                if (this.measureType == M3D.Shapes.MeasureType.PNT_PNT_DISTANCE) {
                    bFlag = this.pointToPoint();
                }
                else if (this.measureType == M3D.Shapes.MeasureType.PNT_LINE_DISTANCE) {
                    //点线距离
                    bFlag = this.pointToLine();
                }
                else if (this.measureType == M3D.Shapes.MeasureType.PNT_FACE_DISTANCE) {
                    //点到面
                    bFlag = this.pointToFace();
                }
                else if (this.measureType == M3D.Shapes.MeasureType.LINE_LINE_DISTANCE) {
                    //线到线
                    bFlag = this.lineToLine();
                }
                else if (this.measureType == M3D.Shapes.MeasureType.LINE_FACE_DISTANCE) {
                    //线到线
                    bFlag = this.lineToFace();
                }
                if (!bFlag) {
                    return state;
                }
                //获取引线
                if (!this.fourthPosition) {
                    let leaderLineEndPoint;
                    let results = this.getLeaderPoints(this.firstPosition, this.secondPosition);
                    this.thirdPosition = results[0];
                    if (!this.fourthPosition) {
                        leaderLineEndPoint = results[1];
                    }
                    else {
                        leaderLineEndPoint = this.fourthPosition;
                    }
                    this.setPoint("firstPoint", this.firstPosition);
                    this.setPoint("secondPoint", this.secondPosition);
                    // 添加连接线和引出线
                    this.setLine("connectLine", this.titleTextColor, this.firstPosition, this.secondPosition);
                    this.setLine("leaderLine", this.leaderColor, this.thirdPosition, leaderLineEndPoint);
                    if (state != M3D.Actions.ActionState.Action_Success) {
                        return state;
                    }
                    let unit = "";
                    unit = this.getMeasureUnit(/*M3D.Config.Parameters.getInstance().measureUnitStyle*/ 1, this.distances, unit);
                    let temptext = new Array();
                    for (let i = 0; i < this.distances.length; ++i) {
                        if (this.distances[i] > -0.000001) {
                            let tempText = [];
                            let length = this.distances[i];
                            let lengthstr = length.toFixed(2).toString();
                            let title = new M3D.Shapes.Texts2D();
                            title.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                            title.setBackground(this.backGroundColor);
                            title.setFontColor(this.titleTextColor);
                            title.setTexts(M3D.Shapes.MeasureDisplayHelper.getTitleText(i, this.measureType));
                            tempText.push(title);
                            let content = new M3D.Shapes.Texts2D();
                            content.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                            content.setBackground(M3D.M3DMath.Color.WHITE());
                            content.setFontColor(this.resultTextColor);
                            content.setTexts("  ~" + lengthstr + unit + " ");
                            tempText.push(content);
                            temptext.push(tempText);
                            //添加Text文本，用于在线视图保存
                            //解决协同中文本读取
                            let measureAction = new Actions.MeasureAction();
                            measureAction.setComText(i, length);
                        }
                    }
                    M3D.Shapes.MeasureDisplayHelper.createNoteTextsImageMultiline(this.pScene, temptext, leaderLineEndPoint, this.measure);
                }
                else {
                    let imageList = this.measure.getImageList();
                    if (imageList.length > 0) {
                        for (let image of imageList) {
                            image.setPosition(this.fourthPosition);
                        }
                    }
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() === "leaderLine") {
                            lines[i].setEndPoint(this.fourthPosition);
                            return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return state;
            }
            /**
             * @description 点到点的距离
             * */
            pointToPoint() {
                if (!this.secondPosition) {
                    this.setPoint("firstPoint", this.firstPosition);
                    return false;
                }
                //距离
                if (!this.firstPosition.Equals(new M3D.M3DMath.Vector3) && !this.secondPosition.Equals(new M3D.M3DMath.Vector3) && !this.fourthPosition) {
                    this.distances.push(this.firstPosition.Sub(this.secondPosition).Length());
                }
                return true;
            }
            /**
             * @description 点到线的距离
             * */
            pointToLine() {
                let methodName = this.pointToLine.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = this.pScene.getParameters().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if (!this.firstPosition && this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT) {
                    this.firstPosition = this.firstShape.getPosition();
                }
                if (!this.firstPosition)
                    return false;
                if (!this.secondPosition) {
                    this.setPoint("firstPoint", this.firstPosition);
                    return false;
                }
                if (this.secondShape && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE || (isMeasureClipEdge && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE)) {
                    if (this.fourthPosition) {
                        return true;
                    }
                    let edge = this.secondShape;
                    let geo = edge.getGeometryFeature();
                    if (!geo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            geo = edge.getGeometryFeature();
                        }
                    }
                    if (this.secondShape && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE || (isMeasureClipEdge && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE)) {
                        if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            let lineFeatrue = geo.clone();
                            let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                            M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                            // 垂足
                            let inLine;
                            let distance = 0;
                            distance = this.pointLineDistance(this.firstPosition, lineFeatrue, distance, inLine, this.secondPosition);
                            let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                            let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                            this.setLine("measureLine", M3D.M3DMath.Color.RED(), pntStartPoint, pntEndPoint);
                            let startD = this.firstPosition.Sub(pntStartPoint).Length();
                            let endD = this.firstPosition.Sub(pntEndPoint).Length();
                            let minDistance;
                            let maxDistance;
                            let maxPoint = new M3D.M3DMath.Vector3();
                            let minPoint = new M3D.M3DMath.Vector3();
                            if (startD > endD) {
                                maxDistance = startD;
                                minDistance = endD;
                                maxPoint = pntStartPoint;
                                minPoint = pntEndPoint;
                            }
                            else {
                                maxDistance = endD;
                                minDistance = startD;
                                minPoint = pntStartPoint;
                                maxPoint = pntEndPoint;
                            }
                            let isMin = false;
                            let projecPoint = new M3D.M3DMath.Vector3();
                            if (!(this.pntInSegment(this.secondPosition, pntStartPoint, pntEndPoint, projecPoint))) {
                                //最近点
                                isMin = true;
                                this.setLine("extendLine", this.markingColor, projecPoint, this.secondPosition);
                                //标记点
                                this.setPoint("markingPoint", this.secondPosition);
                            }
                            else {
                                this.removetPoint("markingPoint");
                                this.removetLine("extendLine");
                            }
                            isMin = isMin && this.isShowMinDistance;
                            if (isMin) {
                                //最近距离点
                                this.setPoint("minDistancePoint", minPoint);
                                //最近距离线
                                this.setLine("minDistanceLine", this.minDistanceColor, minPoint, this.firstPosition);
                            }
                            else {
                                this.removetPoint("minDistancePoint");
                                this.removetLine("minDistanceLine");
                            }
                            let isMax = isMin && this.isShowMaxDistance;
                            if (isMax) {
                                //最远距离点
                                this.setPoint("maxDistancePoint", maxPoint);
                                //最远距离线
                                this.setLine("maxDistanceLine", this.maxDistanceColor, maxPoint, this.firstPosition);
                            }
                            else {
                                this.removetPoint("maxDistancePoint");
                                this.removetLine("maxDistanceLine");
                            }
                            //距离
                            minDistance = isMin ? minDistance : -1.0;
                            maxDistance = isMax ? maxDistance : -1.0;
                            this.distances.push(distance);
                            this.distances.push(minDistance);
                            this.distances.push(maxDistance);
                        }
                        else if (geo == null || (geo && geo.getFeatureType() != M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE)) {
                            let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                            // 垂足
                            let closestCoordiant = new M3D.M3DMath.Vector3();
                            let startPoint = new M3D.M3DMath.Vector3();
                            let endPoint = new M3D.M3DMath.Vector3();
                            let inLine;
                            let distance;
                            let result = this.pointLineTempDistance(edge, this.firstPosition, distance, closestCoordiant, worldMatrix, startPoint, endPoint);
                            distance = result[0];
                            startPoint = result[1];
                            endPoint = result[2];
                            this.removetLine("markingLine");
                            this.removetPoint("minDistancePoint");
                            this.removetLine("minDistanceLine");
                            this.removetPoint("maxDistancePoint");
                            this.removetLine("maxDistanceLine");
                            this.removetLine("extendLine");
                            this.removetPoint("markingPoint");
                            //距离
                            this.distances.push(distance);
                        }
                    }
                    else {
                        //释放
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return false;
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return true;
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return false;
                }
            }
            /**
             * @description 点到面的距离
             * */
            pointToFace() {
                let methodName = this.pointToFace.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (!this.firstPosition && this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT) {
                    this.firstPosition = this.firstShape.getPosition();
                }
                if (!this.firstPosition)
                    return false;
                if (!this.secondPosition) {
                    this.setPoint("firstPoint", this.firstPosition);
                    return false;
                }
                if (this.secondShape && this.secondPosition && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE) {
                    if (this.fourthPosition) {
                        return true;
                    }
                    let face = this.secondShape;
                    let geo = face.getGeometryFeature();
                    if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        let faceFeatrue = geo.clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                        M3D.Shapes.ShapeHelper.transform(faceFeatrue, worldMatrix1);
                        let distance = 0;
                        let projectPnt = new M3D.M3DMath.Vector3();
                        let result = this.pointFaceDistance(this.secondPosition, faceFeatrue, distance, projectPnt);
                        distance = result[1];
                        projectPnt = result[2];
                        //取视口宽高中小的固定比例
                        let width = 0, height = 0;
                        width = this.pScene.getCurrentViewPort().getRect().Size().x;
                        height = this.pScene.getCurrentViewPort().getRect().Size().y;
                        distance = width > height ? height / 450 : width / 450;
                        let normalVec = faceFeatrue.getNormal();
                        let originVec = faceFeatrue.getOrigin();
                        let direction = projectPnt.Sub(originVec);
                        //平面标记
                        let pointArray = [];
                        pointArray = this.getPointSquareBoxMark(pointArray, this.secondPosition, direction, normalVec, distance);
                        this.setLine("squareBoxMark1", this.squareBoxColor, pointArray[0], pointArray[1]);
                        this.setLine("squareBoxMark2", this.squareBoxColor, pointArray[2], pointArray[3]);
                        this.setLine("squareBoxMark3", this.squareBoxColor, pointArray[0], pointArray[2]);
                        this.setLine("squareBoxMark4", this.squareBoxColor, pointArray[3], pointArray[1]);
                        this.setLine("squareBoxMark1", this.secondPosition, true, true, false);
                        this.setLine("squareBoxMark2", this.secondPosition, true, true, false);
                        this.setLine("squareBoxMark3", this.secondPosition, true, true, false);
                        this.setLine("squareBoxMark4", this.secondPosition, true, true, false);
                        if (this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT || this.firstPosition) {
                            let planeTemp = new M3D.M3DMath.Plane(normalVec, originVec);
                            let pntProject = planeTemp.Project(this.firstPosition);
                            let length = this.firstPosition.Sub(pntProject).Length();
                            //最近距离，最远距离
                            let inverseMat = worldMatrix1.Inverse();
                            let modelVec = inverseMat.Multiply(this.firstPosition);
                            let minDistance, maxDistance;
                            let maxPoint = new M3D.M3DMath.Vector3();
                            let minPoint = new M3D.M3DMath.Vector3();
                            let results = this.pointFaceDistance(modelVec, face, minDistance, maxDistance, minPoint, maxPoint);
                            distance = results[0];
                            maxDistance = results[1];
                            minPoint = worldMatrix1.Multiply(minPoint).Clone();
                            maxPoint = worldMatrix1.Multiply(maxPoint).Clone();
                            this.setLine("concatLine", this.backGroundColor, this.firstPosition, pntProject);
                            this.setLine("extendFaceLine", this.squareBoxColor, this.secondPosition, pntProject);
                            let isMin = !pntProject.Equals(minPoint) && this.isShowMinDistance;
                            let isMax = maxDistance > 0 && this.isShowMaxDistance;
                            if (isMin) {
                                this.setPoint("minPoint", minPoint);
                                this.setLine("minLine", this.minDistanceColor, this.firstPosition, minPoint);
                            }
                            if (isMax) {
                                this.setPoint("maxPoint", maxPoint);
                                this.setLine("maxLine", this.maxDistanceColor, this.firstPosition, maxPoint);
                            }
                            //测量结果单位转换
                            minDistance = isMin ? minDistance : -1.0;
                            maxDistance = isMax ? maxDistance : -1.0;
                            this.secondPosition = pntProject;
                            this.distances.push(length);
                            this.distances.push(minDistance);
                            this.distances.push(maxDistance);
                        }
                    }
                    else if (geo == null || (geo && geo.getFeatureType() != M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE)) {
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                        let inverseMat = worldMatrix1.Inverse();
                        if ((!this.firstShape || this.firstShape.getShapeType() != M3D.Shapes.ShapeType.POINT) && !this.firstPosition) {
                            return false;
                        }
                        let modelVec = inverseMat.Multiply(this.firstPosition);
                        let minDistance, maxDistance;
                        let maxPoint = new M3D.M3DMath.Vector3();
                        let minPoint = new M3D.M3DMath.Vector3();
                        let results = this.pointFaceDistance(modelVec, face, minDistance, maxDistance, minPoint, maxPoint);
                        minDistance = results[0];
                        maxDistance = results[1];
                        minPoint = worldMatrix1.Multiply(minPoint);
                        maxPoint = worldMatrix1.Multiply(maxPoint);
                        let length = minPoint.Sub(this.firstPosition).Length();
                        this.distances.push(length);
                        if (minPoint != null) {
                            this.setPoint("minPoint", minPoint);
                            this.setLine("minLine", this.leaderColor, this.firstPosition, minPoint);
                        }
                        if (maxPoint != null) {
                            this.setPoint("maxPoint", maxPoint);
                            this.setLine("minLine", this.leaderColor, this.firstPosition, maxPoint);
                        }
                        this.secondPosition = minPoint;
                    }
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return false;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return true;
            }
            /**
             * @description 线到线的距离
             * */
            lineToLine() {
                let methodName = this.lineToLine.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if ((this.firstShape && !this.secondShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE) ||
                    this.isSectionEdgeEnable(true, this.firstShape, this.secondShape)) {
                    let edge = this.firstShape;
                    let geo = edge.getGeometryFeature();
                    if (!geo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            geo = edge.getGeometryFeature();
                        }
                    }
                    if (geo && !this.secondShape && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let lineFeatrue = geo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                        let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                        //添加预显线 后续将删除
                        this.setLine("preline", this.markingColor, pntStartPoint, pntEndPoint);
                    }
                    else {
                        return false;
                    }
                }
                else if ((this.firstShape && this.secondShape && this.firstShape != this.secondShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE) || this.isSectionEdgeEnable(false, this.firstShape, this.secondShape)) {
                    let firstEdge = this.firstShape;
                    let secondEdge = this.secondShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    let secondGeo = secondEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let geoType = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        firstGeo = firstEdge.getGeometryFeature();
                    }
                    if (!secondGeo) {
                        let geoType = M3D.Shapes.ShapeHelper.getLineType(secondEdge);
                        secondGeo = secondEdge.getGeometryFeature();
                    }
                    let isNoGeoMeasure = true;
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE && secondGeo != null && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let secondLineFeatrue = secondGeo.clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        let worldMatrix2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, worldMatrix1);
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, worldMatrix2);
                        let distance = 0;
                        let projectPnt1 = new M3D.M3DMath.Vector3();
                        let projectPnt2 = new M3D.M3DMath.Vector3();
                        let parallel = true;
                        let result = this.lineLineDistance(firstLineFeatrue, secondLineFeatrue, distance, projectPnt1, projectPnt2, parallel);
                        distance = result[0];
                        parallel = result[1];
                        this.firstPosition = projectPnt1;
                        this.secondPosition = projectPnt2;
                        let pointStartPoint1 = firstLineFeatrue.getStartPoint();
                        let pointEndPoint1 = firstLineFeatrue.getEndPoint();
                        let pointStartPoint2 = secondLineFeatrue.getStartPoint();
                        let pointEndPoint2 = secondLineFeatrue.getEndPoint();
                        if (parallel) {
                            isNoGeoMeasure = false;
                            //判断得到的垂足是否在两条线段上，只要一条不在，就用四个端点之间的距离来判断。
                            let sdis1, edis1, sdis2, edis2;
                            let in1, in2;
                            in1 = this.pntInSegment(projectPnt1, firstLineFeatrue);
                            in2 = this.pntInSegment(projectPnt2, secondLineFeatrue);
                            let firstLineStartToSecondLineMinDistance = 0.0;
                            let firstStartProjectPnt = new M3D.M3DMath.Vector3();
                            firstLineStartToSecondLineMinDistance = this.pointLineDistance(pointStartPoint2, pointEndPoint2, pointStartPoint1, firstLineStartToSecondLineMinDistance, firstStartProjectPnt);
                            let firstLineEndToSecondLineMinDistance = 0.0;
                            let firstEndProjectPnt = new M3D.M3DMath.Vector3();
                            firstLineEndToSecondLineMinDistance = this.pointLineDistance(pointStartPoint2, pointEndPoint2, pointEndPoint1, firstLineEndToSecondLineMinDistance, firstEndProjectPnt);
                            let secondLineStartToFirstLineMinDistance = 0.0;
                            let secondStartProjectPnt = new M3D.M3DMath.Vector3();
                            secondLineStartToFirstLineMinDistance = this.pointLineDistance(pointStartPoint1, pointEndPoint1, pointStartPoint2, secondLineStartToFirstLineMinDistance, secondStartProjectPnt);
                            let secondLineEndToFirstLineMinDistance = 0.0;
                            let secondEndProjectPnt = new M3D.M3DMath.Vector3();
                            secondLineEndToFirstLineMinDistance = this.pointLineDistance(pointStartPoint1, pointEndPoint1, pointEndPoint2, secondLineEndToFirstLineMinDistance, secondEndProjectPnt);
                            let minDistance = 0;
                            let minPoint1 = new M3D.M3DMath.Vector3(), minPoint2 = new M3D.M3DMath.Vector3();
                            sdis2 = secondLineStartToFirstLineMinDistance;
                            edis2 = secondLineEndToFirstLineMinDistance;
                            sdis1 = firstLineStartToSecondLineMinDistance;
                            edis1 = firstLineEndToSecondLineMinDistance;
                            if (sdis2 > edis2) {
                                minDistance = edis2;
                                minPoint1 = secondEndProjectPnt;
                                minPoint2 = pointEndPoint2;
                            }
                            else {
                                minDistance = sdis2;
                                minPoint1 = secondStartProjectPnt;
                                minPoint2 = pointStartPoint2;
                            }
                            if (minDistance > sdis1) {
                                minDistance = sdis1;
                                minPoint1 = firstStartProjectPnt;
                                minPoint2 = pointStartPoint1;
                            }
                            if (minDistance > edis1) {
                                minDistance = edis1;
                                minPoint1 = pointEndPoint1;
                                minPoint2 = firstEndProjectPnt;
                            }
                            // 计算临时测量结果显示位置
                            //camera射线
                            let cameraRay = this.pScene.getCurrentViewPort().getScreenRay(0, 0);
                            let lineCenter = (projectPnt1.Add(projectPnt2)).Divide(2).Clone();
                            //构造投影平面
                            let projPlane = new M3D.M3DMath.Plane(cameraRay.GetDirection(), lineCenter);
                            let pntInPlane = projPlane.Project(cameraRay.GetOrigin());
                            if (!in1) {
                                let extern1 = new M3D.M3DMath.Vector3();
                                if ((projectPnt1.Sub(pointStartPoint1)).Length() > (projectPnt1.Sub(pointEndPoint1)).Length()) {
                                    extern1 = pointEndPoint1;
                                }
                                else {
                                    extern1 = pointStartPoint1;
                                }
                                this.setLine("extern1", this.markingColor, extern1, projectPnt1);
                            }
                            if (!in2) {
                                let extern2 = new M3D.M3DMath.Vector3();
                                if ((projectPnt2.Sub(pointStartPoint2)).Length()
                                    > (projectPnt2.Sub(pointEndPoint2)).Length()) {
                                    extern2 = pointEndPoint2;
                                }
                                else {
                                    extern2 = pointStartPoint2;
                                }
                                this.setLine("extern2", this.markingColor, extern2, projectPnt2);
                            }
                            let line1Vector = pointStartPoint1.Sub(pointEndPoint1).Divide(50);
                            let line2Vector = pointStartPoint2.Sub(pointEndPoint2).Divide(50);
                            this.setLine("markingLine1", this.markingColor, projectPnt1.Add(line1Vector), projectPnt1.Sub(line1Vector)); //测量线1
                            this.setLine("markingLine2", this.markingColor, projectPnt2.Add(line2Vector), projectPnt2.Sub(line2Vector)); //测量线2
                            let isMin = M3D.M3DMath.Abs(distance - minDistance) > 0.000001 && this.isShowMinDistance;
                            if (isMin) {
                                this.setPoint("minPoint1", minPoint1);
                                this.setPoint("minPoint2", minPoint2);
                                this.setLine("minLine", this.markingColor, minPoint1, minPoint2);
                            }
                            this.distances.push(distance);
                        }
                    }
                    if (isNoGeoMeasure) {
                        let firstLineFeatrue = firstGeo.clone();
                        let secondLineFeatrue = secondGeo.clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        let worldMatrix2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, worldMatrix1);
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, worldMatrix2);
                        let distance = 0;
                        let result = this.lineLineDistance(firstEdge, secondEdge, this.firstPosition, this.secondPosition, distance, worldMatrix1, worldMatrix2);
                        distance = result[0];
                        this.firstPosition = result[1];
                        this.secondPosition = result[2];
                        this.distances.push(distance);
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return true;
            }
            /**
             * @description 线到面的距离
             * */
            lineToFace() {
                let methodName = this.lineToFace.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = this.pScene.getParameters().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if ((this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE) || (isMeasureClipEdge
                    && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE)) {
                    let edge = this.firstShape;
                    let lineGeo = edge.getGeometryFeature();
                    if (!lineGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            lineGeo = edge.getGeometryFeature();
                        }
                    }
                    if (!this.secondShape) {
                        if (lineGeo) {
                            let lineFeatrue = lineGeo.clone();
                            let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                            M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                            let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                            let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                            //添加预显线 后续将删除
                            this.setLine("preline", this.markingColor, pntStartPoint, pntEndPoint);
                        }
                        else {
                            return false;
                        }
                    }
                    else if (this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE && !this.secondPosition.IsZero()) {
                        let face = this.secondShape;
                        let faceGeo = face.getGeometryFeature();
                        let firstFinished = false;
                        let secondFinished = false;
                        let tCoordinate = new M3D.M3DMath.Vector3(), sCoordinate = new M3D.M3DMath.Vector3();
                        if (faceGeo && faceGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                            let faceFeature = faceGeo.clone();
                            let worldMatrixFace = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                            M3D.Shapes.ShapeHelper.transform(faceFeature, worldMatrixFace);
                            if (lineGeo && lineGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                                let lineFeatrue = lineGeo.clone();
                                let worldMatrixLine = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                                M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrixLine);
                                let distance = 0;
                                let projectPnt0 = new M3D.M3DMath.Vector3(), projectPnt1 = new M3D.M3DMath.Vector3();
                                let results = this.lineFaceDistance(lineFeatrue, faceFeature, distance, projectPnt0, projectPnt1);
                                distance = results[1];
                                let bRet = results[0];
                                if (bRet) {
                                    let line1Start = lineFeatrue.getStartPoint();
                                    let line1End = lineFeatrue.getEndPoint();
                                    let normalVec = line1Start.Sub(projectPnt0);
                                    normalVec.Normalize();
                                    this.firstPosition = this.secondPosition.Add(normalVec.Multiply(distance));
                                    let tempdis = 0;
                                    let minPoin = new M3D.M3DMath.Vector3();
                                    let inLine;
                                    this.pointLineDistance(this.firstPosition, lineFeatrue, tempdis, inLine, minPoin);
                                    sCoordinate = minPoin;
                                    let projecPnt = new M3D.M3DMath.Vector3();
                                    if (!this.pntInSegment(minPoin, line1Start, line1End, projecPnt)) {
                                        //延长虚线
                                        this.setLine("dottedLine", this.markingColor, minPoin, projecPnt);
                                    }
                                    let lineVector = (line1Start.Sub(line1End)).Divide(50);
                                    //线垂足横线
                                    this.setLine("markingLine1", this.markingColor, sCoordinate.Add(lineVector), sCoordinate.Sub(lineVector));
                                    //线垂足竖线
                                    this.setLine("markingLine2", this.markingColor, this.firstPosition, sCoordinate);
                                    let result = this.pointFaceDistance(this.secondPosition, faceFeature, distance, projecPnt);
                                    distance = result[1];
                                    let width = 0, height = 0;
                                    width = this.pScene.getCurrentViewPort().getRect().Size().x;
                                    height = this.pScene.getCurrentViewPort().getRect().Size().y;
                                    distance = width > height ? height / 450 : width / 450;
                                    let originVec = faceFeature.getOrigin();
                                    let direction = projecPnt.Sub(originVec);
                                    //平面标记
                                    let pointArray = [];
                                    pointArray = this.getPointSquareBoxMark(pointArray, this.secondPosition, direction, normalVec, distance);
                                    this.setLine("squareBoxMark1", this.squareBoxColor, pointArray[0], pointArray[1]);
                                    this.setLine("squareBoxMark2", this.squareBoxColor, pointArray[2], pointArray[3]);
                                    this.setLine("squareBoxMark3", this.squareBoxColor, pointArray[0], pointArray[2]);
                                    this.setLine("squareBoxMark4", this.squareBoxColor, pointArray[3], pointArray[1]);
                                    this.setLine("squareBoxMark1", this.secondPosition, true, true, false);
                                    this.setLine("squareBoxMark2", this.secondPosition, true, true, false);
                                    this.setLine("squareBoxMark3", this.secondPosition, true, true, false);
                                    this.setLine("squareBoxMark4", this.secondPosition, true, true, false);
                                    secondFinished = true;
                                    this.distances.push(distance);
                                }
                                else {
                                    let distance;
                                    let tCoordinate = new M3D.M3DMath.Vector3(), sCoordinate = new M3D.M3DMath.Vector3();
                                    let result = this.lineFaceDistance(face, edge, distance, this.firstPosition, this.secondPosition, worldMatrixLine, worldMatrixFace);
                                    this.distances.push(distance);
                                }
                            }
                            else {
                                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                                return false;
                            }
                        }
                        else {
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return false;
                        }
                    }
                    else {
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return false;
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return true;
            }
            /**
            * @description 判断剖面线是否可用
            * @param bFirstShape 第一次选择
            * @param firstShape 第一对象
            * @param secondShape 第二对象
            * @return true 成功 false 失败
            */
            isSectionEdgeEnable(bFirstShape, firstShape, secondShape) {
                let methodName = this.isSectionEdgeEnable.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = this.pScene.getParameters().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if (!bFirstShape) {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return (isMeasureClipEdge && firstShape && secondShape
                        && (firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE && secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE ||
                            firstShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE && secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE ||
                            firstShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE && secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE));
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return (isMeasureClipEdge && firstShape &&
                        firstShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE && !secondShape);
                }
            }
            /**
            * @description 选择的面显示矩形
            * @param pointArray point数组
            * @param boxCenter 包围盒中心
            * @param direction 方向
            * @param faceNormal 面的法向量
            */
            getPointSquareBoxMark(pointArray, boxCenter, direction, faceNormal, length) {
                let methodName = this.getPointSquareBoxMark.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                //假设direction表示X轴，faceNormal表示Z轴，则可求出另外一个边方向向量Y
                let x = direction.Normalized();
                let z = faceNormal.Normalized();
                let y = (direction.CrossProduct(faceNormal)).Normalized();
                let p1 = boxCenter.Add((x.Add(y)).Multiply(length));
                pointArray.push(p1);
                let p2 = boxCenter.Add((x.Sub(y)).Multiply(length));
                pointArray.push(p2);
                let p3 = boxCenter.Add((y.Sub(x)).Multiply(length));
                pointArray.push(p3);
                let p4 = boxCenter.Sub((x.Add(y)).Multiply(length));
                pointArray.push(p4);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return pointArray;
            }
            /**
             * @description 添加节点到场景树
             * */
            addToScene() {
                let methodName = this.addToScene.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                //添加批注到场景
                let ret = false;
                if (this.measure && this.pScene) {
                    this.measure.setVisible(true);
                    let measures = this.pScene.getShapes();
                    measures.addShape(this.measure);
                    this.pScene.refresh();
                    //解锁
                    ret = true;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return ret;
            }
            pointLineTempDistance(...restOfName) {
                let methodName = this.pointLineTempDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length === 5) {
                    let lineSet = restOfName[0];
                    let point = restOfName[1];
                    let distance = restOfName[2];
                    let minPoint = restOfName[3];
                    let worldMatrix = restOfName[4];
                    //let xPolyLine: SPolyLine = lineSet.GetRefLine();
                    let vertexSets = this.pScene.getVertexSetManager();
                    let vertexHeader = vertexSets.getRenderVertexHeader(lineSet);
                    let vertexset = vertexHeader.vertexSet;
                    if (vertexset) {
                        let useIndex = vertexHeader.useIndex;
                        if (useIndex) {
                            let indexCount = vertexHeader.indexLength;
                            let offset = vertexHeader.indexOffset;
                            let pointArray = new Float32Array(vertexset.getMeshArray(), vertexHeader.vertexOffset, indexCount);
                            let pEdgeIndexArray = vertexset.getEdgeIndexArray();
                            let startPoint = new M3D.M3DMath.Vector3();
                            startPoint.x = pointArray[pEdgeIndexArray[offset] * 3 + 0];
                            startPoint.y = pointArray[pEdgeIndexArray[offset] * 3 + 1];
                            startPoint.z = pointArray[pEdgeIndexArray[offset] * 3 + 2];
                            distance = (worldMatrix.Multiply(startPoint).Sub(worldMatrix.Multiply(point)).Clone()).Length(); //初始化
                            minPoint.copyFrom(worldMatrix.Multiply(startPoint)); //初始化
                            for (let i = pEdgeIndexArray[0] + offset; i != pEdgeIndexArray[0] + offset + indexCount; i += 2) {
                                let distanceTemp;
                                let minPointTemp = new M3D.M3DMath.Vector3();
                                let p1 = new M3D.M3DMath.Vector3();
                                let p2 = new M3D.M3DMath.Vector3();
                                p1.x = pointArray[pEdgeIndexArray[i] * 3 + 0];
                                p1.y = pointArray[pEdgeIndexArray[i] * 3 + 1];
                                p1.z = pointArray[pEdgeIndexArray[i] * 3 + 2];
                                p2.x = pointArray[pEdgeIndexArray[i + 1] * 3 + 0];
                                p2.y = pointArray[pEdgeIndexArray[i + 1] * 3 + 1];
                                p2.z = pointArray[pEdgeIndexArray[i + 1] * 3 + 2];
                                let point1 = worldMatrix.Multiply(p1);
                                let point2 = worldMatrix.Multiply(p2);
                                distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                                if (distance > distanceTemp) {
                                    distance = distanceTemp;
                                    minPoint.copyFrom(minPointTemp);
                                }
                            }
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return distance;
                }
                else if (restOfName.length === 7) {
                    let lineSet = restOfName[0];
                    let point = restOfName[1];
                    let distance = restOfName[2];
                    let minPoint = restOfName[3];
                    let worldMatrix = restOfName[4];
                    let startPoint = restOfName[5];
                    let endPoint = restOfName[6];
                    let vertexSets = this.pScene.getVertexSetManager();
                    let vertexHeader = vertexSets.getRenderVertexHeader(lineSet);
                    let vertexset = vertexHeader.vertexSet;
                    if (vertexset) {
                        let useIndex = vertexHeader.useIndex;
                        if (useIndex) {
                            let indexCount = vertexHeader.indexLength;
                            let offset = vertexHeader.indexOffset;
                            let pointArray = new Float32Array(vertexset.getMeshArray(), vertexHeader.vertexOffset, indexCount);
                            let pEdgeIndexArray = vertexset.getEdgeIndexArray();
                            let startPoint = new M3D.M3DMath.Vector3();
                            startPoint.x = pointArray[pEdgeIndexArray[offset] * 3 + 0];
                            startPoint.y = pointArray[pEdgeIndexArray[offset] * 3 + 1];
                            startPoint.z = pointArray[pEdgeIndexArray[offset] * 3 + 2];
                            distance = (worldMatrix.Multiply(startPoint).Sub(worldMatrix.Multiply(point)).Clone()).Length(); //初始化
                            minPoint.copyFrom(worldMatrix.Multiply(startPoint)); //初始化
                            for (let i = pEdgeIndexArray[0] + offset; i != pEdgeIndexArray[0] + offset + indexCount; i = i + 2) {
                                let distanceTemp;
                                let minPointTemp = new M3D.M3DMath.Vector3();
                                let p1 = new M3D.M3DMath.Vector3();
                                let p2 = new M3D.M3DMath.Vector3();
                                p1.x = pointArray[pEdgeIndexArray[i] * 3 + 0];
                                p1.y = pointArray[pEdgeIndexArray[i] * 3 + 1];
                                p1.z = pointArray[pEdgeIndexArray[i] * 3 + 2];
                                p2.x = pointArray[pEdgeIndexArray[i + 1] * 3 + 0];
                                p2.y = pointArray[pEdgeIndexArray[i + 1] * 3 + 1];
                                p2.z = pointArray[pEdgeIndexArray[i + 1] * 3 + 2];
                                let point1 = worldMatrix.Multiply(p1);
                                let point2 = worldMatrix.Multiply(p2);
                                distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                                if (distance > distanceTemp) {
                                    distance = distanceTemp;
                                    minPoint.copyFrom(minPointTemp);
                                    startPoint = point1;
                                    endPoint = point2;
                                }
                            }
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [distance, startPoint, endPoint];
                }
            }
            pointLineDistance(...restOfName) {
                let methodName = this.pointLineDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 5) {
                    if (restOfName[1] instanceof M3D.Shapes.LineFeature) {
                        let pntCoordinate = restOfName[0];
                        let lineFeature = restOfName[1];
                        let distance = restOfName[2];
                        let inLine = restOfName[3];
                        let projectPnt = restOfName[4];
                        let ray = new M3D.M3DMath.Ray(lineFeature.getStartPoint(), lineFeature.getDirection());
                        projectPnt.copyFrom(ray.Project(pntCoordinate));
                        inLine = 0;
                        distance = (pntCoordinate.Sub(projectPnt)).Length();
                        return distance;
                    }
                    else if (restOfName[1] instanceof M3D.M3DMath.Vector3) {
                        let point1 = restOfName[0];
                        let point2 = restOfName[1];
                        let point = restOfName[2];
                        let minDistance = restOfName[3];
                        let minPoint = restOfName[4];
                        let projectPoint = new M3D.M3DMath.Vector3();
                        this.pntInSegment(point, point1, point2, projectPoint);
                        minDistance = (point.Sub(projectPoint)).Length();
                        minPoint.copyFrom(projectPoint);
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return minDistance;
                    }
                }
                else if (restOfName.length == 7) {
                    let pntCoordinate = restOfName[0];
                    let ellipseAttribute = restOfName[1];
                    let distance = restOfName[2];
                    let inLine = restOfName[3];
                    let projectPnt = restOfName[4];
                    let worldMatrix = restOfName[5];
                    let lineSet = restOfName[6];
                    inLine = 0; //没用
                    let center = ellipseAttribute.GetCenterPoint();
                    let start = ellipseAttribute.GetStartPoint();
                    let end = ellipseAttribute.GetEndPoint();
                    let dirX = new M3D.M3DMath.Vector3(), dirY = new M3D.M3DMath.Vector3(), dirZ = new M3D.M3DMath.Vector3();
                    ellipseAttribute.GetXYZDir(dirX, dirY, dirZ);
                    let R = ellipseAttribute.GetMajorRadius(); //半径
                    //构造圆弧平面
                    let plane = new M3D.M3DMath.Plane(dirZ, center);
                    //求出投影点
                    let projectP = plane.Project(pntCoordinate);
                    let OP = projectP.Sub(center).Clone();
                    let disTocen = OP.Length(); //投影点到圆心距离
                    if (disTocen > R) {
                        //let xPolyLine: M3D.Shapes.PolyLine = lineSet.GetRefLine();
                        //let pntPoints: M3D.M3DMath.Vector3[] = lineSet.getPoints();
                        //let offset = lineSet.GetDataOffset();
                        //let dataLength = lineSet.GetDataLength();
                        //let flag = false;
                        //	int countIt =1;
                        //for (let i = offset; i < offset + dataLength; i += 2) {
                        //    flag = MeasureCalculateHelper.RaySegmentInsect(center, projectP, pntPoints[i], pntPoints[i + 1]);
                        //    if (flag) {
                        //        projectPnt.copyFrom(center.Add(OP.Multiply(R * (1.0 / disTocen))));
                        //        distance = (pntCoordinate.Sub(projectPnt).Clone()).Length();
                        //    }
                        //}
                        //if (!flag) {
                        //    let CADis = (projectP.Sub(start).Clone()).Length();
                        //    let CBDis = (projectP.Sub(end).Clone()).Length();
                        //    if (CADis >= CBDis) {
                        //        distance = (pntCoordinate.Sub(end).Clone()).Length();
                        //        projectPnt.copyFrom(end);
                        //    }
                        //    else {
                        //        distance = (pntCoordinate.Sub(start).Clone()).Length();
                        //        projectPnt.copyFrom(start);
                        //    }
                        //}
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return distance;
                    }
                    else {
                        //let xPolyLine: SPolyLine = m_lineSet.GetRefLine();
                        //let pntPoints: Vector3[] = xPolyLine.GetPoints();
                        //let offset = m_lineSet.GetDataOffset();
                        //let dataLength = m_lineSet.GetDataLength();
                        ////	int countIt =1;
                        //let flag = false;
                        //for (let i = offset; i < offset + dataLength; i += 2) {
                        //    flag = MeasureCalculateHelper.RaySegmentInsect(center, projectP, pntPoints[i], pntPoints[i + 1]);
                        //    if (flag) {
                        //        projectPnt.copyFrom(center.Add(OP.Multiply(R * (1.0 / disTocen))));
                        //        distance = (pntCoordinate.Sub(projectPnt).Clone()).Length();
                        //    }
                        //}
                        //if (!flag) {
                        //    let CADis = (projectP.Sub(start).Clone()).Length();
                        //    let CBDis = (projectP.Sub(end).Clone()).Length();
                        //    if (CADis >= CBDis) {
                        //        distance = (pntCoordinate.Sub(end).Clone()).Length();
                        //        projectPnt.copyFrom(end);
                        //    }
                        //    else {
                        //        distance = (pntCoordinate.Sub(start).Clone()).Length();
                        //        projectPnt.copyFrom(start);
                        //    }
                        //}
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return distance;
                    }
                }
                else if (restOfName.length == 4) {
                    let lineSet = restOfName[0];
                    let point = restOfName[1];
                    let distance = restOfName[2];
                    let minPoint = restOfName[3];
                    //let xPolyLine: SPolyLine = lineSet.GetRefLine();
                    let pntPoints = lineSet.getPoints();
                    let offset = lineSet.GetDataOffset();
                    let dataLength = lineSet.GetDataLength();
                    let countIt = 1;
                    distance = (pntPoints[offset].Sub(point).Clone()).Length(); //初始化
                    minPoint.copyFrom(pntPoints[offset]); //初始化
                    for (let i = offset; i < offset + dataLength; i += 2) {
                        let distanceTemp;
                        let minPointTemp = new M3D.M3DMath.Vector3();
                        let point1 = pntPoints[i];
                        let point2 = pntPoints[i + 1];
                        distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                        if (distance > distanceTemp) {
                            distance = distanceTemp;
                            minPoint.copyFrom(minPointTemp);
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return distance;
                }
            }
            pointFaceDistance(...restOfName) {
                let methodName = this.pointFaceDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 6) {
                    let pntCoordinate = restOfName[0];
                    let face = restOfName[1];
                    let distance = restOfName[2];
                    let maxDistance = restOfName[3];
                    let projection = restOfName[4];
                    let maxPoint = restOfName[5];
                    let vertexSets = this.pScene.getVertexSetManager();
                    let vertexHeader = vertexSets.getRenderVertexHeader(face);
                    let vertexset = vertexHeader.vertexSet;
                    if (vertexset) {
                        let useIndex = vertexHeader.useIndex;
                        let indexCount = vertexHeader.indexLength;
                        let offset = vertexHeader.indexOffset;
                        let positionArray = new Float32Array(vertexset.getMeshArray(), vertexHeader.vertexOffset, indexCount);
                        let faceIndexArray = vertexset.getEdgeIndexArray();
                        if (useIndex) {
                            if (positionArray.length > 0) {
                                //LOGI("MeasureCalculateHelper::PntFaceDistance INDEX");
                                //let indexArray = pSet.GetIndexArray();
                                let min = 0.0, max = 0.0;
                                //Vector3 minPnt;
                                let tempTriSet;
                                let minIndex, maxIndex;
                                let count = 0;
                                //获得最小距离点索引
                                let startPnt = new M3D.M3DMath.Vector3();
                                startPnt.x = positionArray[faceIndexArray[0] * 3 + 0];
                                startPnt.y = positionArray[faceIndexArray[0] * 3 + 1];
                                startPnt.z = positionArray[faceIndexArray[0] * 3 + 2];
                                projection.copyFrom(startPnt);
                                min = (pntCoordinate.Sub(startPnt).Clone()).Length();
                                distance = min;
                                maxDistance = 0.0;
                                for (let index = (faceIndexArray)[0]; index <= faceIndexArray[0] + indexCount; index += 3) {
                                    let minIndexTemp, maxIndexTemp;
                                    let mintemp, maxtemp;
                                    let p1 = new M3D.M3DMath.Vector3();
                                    let p2 = new M3D.M3DMath.Vector3();
                                    let p3 = new M3D.M3DMath.Vector3();
                                    p1.x = positionArray[faceIndexArray[index] * 3 + 0];
                                    p1.y = positionArray[faceIndexArray[index] * 3 + 1];
                                    p1.z = positionArray[faceIndexArray[index] * 3 + 2];
                                    p2.x = positionArray[faceIndexArray[index + 1] * 3 + 0];
                                    p2.y = positionArray[faceIndexArray[index + 1] * 3 + 1];
                                    p2.z = positionArray[faceIndexArray[index + 1] * 3 + 2];
                                    p3.x = positionArray[faceIndexArray[index + 2] * 3 + 0];
                                    p3.y = positionArray[faceIndexArray[index + 2] * 3 + 1];
                                    p3.z = positionArray[faceIndexArray[index + 2] * 3 + 2];
                                    let d1 = (pntCoordinate.Sub(p1).Clone()).Length();
                                    let d2 = (pntCoordinate.Sub(p2).Clone()).Length();
                                    let d3 = (pntCoordinate.Sub(p3).Clone()).Length();
                                    if (d1 > d2) {
                                        if (d1 > d3) {
                                            maxIndexTemp = index;
                                            maxtemp = d1;
                                        }
                                        else {
                                            maxIndexTemp = index + 2;
                                            maxtemp = d3;
                                        }
                                    }
                                    else if (d2 > d3) {
                                        maxIndexTemp = index + 1;
                                        maxtemp = d2;
                                    }
                                    else {
                                        maxIndexTemp = index + 2;
                                        maxtemp = d3;
                                    }
                                    if (max < maxtemp) {
                                        max = maxtemp;
                                        maxIndex = maxIndexTemp;
                                    }
                                    let plane = new M3D.M3DMath.Plane(p1, p2, p3);
                                    let pntInPlane = plane.Project(pntCoordinate);
                                    let ray = new M3D.M3DMath.Ray(pntCoordinate, (pntInPlane.Sub(pntCoordinate)));
                                    let temPro = M3D.Actions.RayPickHelper.isIntersectRayAndTriangle(p1, p2, p3, ray);
                                    if ((pntCoordinate.Sub(pntInPlane).Clone()).IsZero()) {
                                        distance = 0.0;
                                        projection.copyFrom(pntInPlane);
                                    }
                                    else if (temPro) {
                                        let tempDistance = 0.0;
                                        tempDistance = (pntCoordinate.Sub(temPro).Clone()).Length();
                                        if (distance > tempDistance) {
                                            distance = tempDistance;
                                            projection.copyFrom(temPro);
                                        }
                                    }
                                    else {
                                        let tempDistance;
                                        let tempProjection = new M3D.M3DMath.Vector3();
                                        let pro1 = new M3D.M3DMath.Vector3(), pro2 = new M3D.M3DMath.Vector3(), pro3 = new M3D.M3DMath.Vector3();
                                        this.pntInSegment(pntCoordinate, p1, p2, pro1);
                                        this.pntInSegment(pntCoordinate, p1, p3, pro2);
                                        this.pntInSegment(pntCoordinate, p2, p3, pro3);
                                        let ds1 = (pntCoordinate.Sub(pro1).Clone()).Length();
                                        let ds2 = (pntCoordinate.Sub(pro2).Clone()).Length();
                                        let ds3 = (pntCoordinate.Sub(pro3).Clone()).Length();
                                        if (ds1 < ds2) {
                                            if (ds1 < ds3) {
                                                tempDistance = ds1;
                                                tempProjection = pro1;
                                            }
                                            else {
                                                tempDistance = ds3;
                                                tempProjection = pro3;
                                            }
                                        }
                                        else if (ds2 < ds3) {
                                            tempDistance = ds2;
                                            tempProjection = pro2;
                                        }
                                        else {
                                            tempDistance = ds3;
                                            tempProjection = pro3;
                                        }
                                        if (distance > tempDistance) {
                                            distance = tempDistance;
                                            projection.copyFrom(tempProjection);
                                        }
                                    }
                                }
                                maxDistance = max;
                                maxPoint.x = positionArray[maxIndex * 3 + 0];
                                maxPoint.y = positionArray[maxIndex * 3 + 1];
                                maxPoint.z = positionArray[maxIndex * 3 + 2];
                            }
                            else {
                                let min = 0.0, max = 0.0;
                                let startPnt = new M3D.M3DMath.Vector3();
                                startPnt.x = positionArray[(faceIndexArray)[0] * 3 + 0];
                                startPnt.y = positionArray[(faceIndexArray)[0] * 3 + 1];
                                startPnt.z = positionArray[(faceIndexArray)[0] * 3 + 2];
                                projection.copyFrom(startPnt); //初始化
                                min = (pntCoordinate.Sub(startPnt).Clone()).Length();
                                distance = min; //初始化
                                let mintemp, maxtemp;
                                let minPntTemp = new M3D.M3DMath.Vector3(), maxPntTemp = new M3D.M3DMath.Vector3();
                                let minPnt = new M3D.M3DMath.Vector3(), maxPnt = new M3D.M3DMath.Vector3();
                                let tempTriSet;
                                for (let index = (faceIndexArray)[0]; index <= faceIndexArray[0] + indexCount; index += 3) {
                                    let tempDistance = 0.0;
                                    let p1 = new M3D.M3DMath.Vector3();
                                    let p2 = new M3D.M3DMath.Vector3();
                                    let p3 = new M3D.M3DMath.Vector3();
                                    p1.x = positionArray[faceIndexArray[index] * 3 + 0];
                                    p1.y = positionArray[faceIndexArray[index] * 3 + 1];
                                    p1.z = positionArray[faceIndexArray[index] * 3 + 2];
                                    p2.x = positionArray[faceIndexArray[index + 1] * 3 + 0];
                                    p2.y = positionArray[faceIndexArray[index + 1] * 3 + 1];
                                    p2.z = positionArray[faceIndexArray[index + 1] * 3 + 2];
                                    p3.x = positionArray[faceIndexArray[index + 2] * 3 + 0];
                                    p3.y = positionArray[faceIndexArray[index + 2] * 3 + 1];
                                    p3.z = positionArray[faceIndexArray[index + 2] * 3 + 2];
                                    let d1 = (pntCoordinate.Sub(p1).Clone()).Length();
                                    let d2 = (pntCoordinate.Sub(p2).Clone()).Length();
                                    let d3 = (pntCoordinate.Sub(p3).Clone()).Length();
                                    if (d1 > d2) {
                                        if (d1 > d3) {
                                            maxPntTemp = p1;
                                            maxtemp = d1;
                                        }
                                        else {
                                            maxPntTemp = p3;
                                            maxtemp = d3;
                                        }
                                    }
                                    else if (d2 > d3) {
                                        maxPntTemp = p2;
                                        maxtemp = d2;
                                    }
                                    else {
                                        maxPntTemp = p3;
                                        maxtemp = d3;
                                    }
                                    if (max < maxtemp) {
                                        max = maxtemp;
                                        maxPnt.copyFrom(maxPntTemp);
                                    }
                                    let plane = new M3D.M3DMath.Plane(p1, p2, p3);
                                    let pntInPlane = plane.Project(pntCoordinate);
                                    let ray = new M3D.M3DMath.Ray(pntCoordinate, (pntInPlane.Sub(pntCoordinate).Clone()));
                                    let temPro = M3D.Actions.RayPickHelper.isIntersectRayAndTriangle(p1, p2, p3, ray);
                                    if ((pntCoordinate.Sub(pntInPlane)).IsZero()) {
                                        distance = 0.0;
                                        projection.copyFrom(pntInPlane);
                                    }
                                    else if (temPro) {
                                        tempDistance = (pntCoordinate.Sub(temPro).Clone()).Length();
                                        if (distance > tempDistance) {
                                            distance = tempDistance;
                                            projection.copyFrom(temPro);
                                        }
                                    }
                                    else {
                                        let tempDistance;
                                        let tempProjection = new M3D.M3DMath.Vector3();
                                        let pro1 = new M3D.M3DMath.Vector3(), pro2 = new M3D.M3DMath.Vector3(), pro3 = new M3D.M3DMath.Vector3();
                                        this.pntInSegment(pntCoordinate, p1, p2, pro1);
                                        this.pntInSegment(pntCoordinate, p1, p3, pro2);
                                        this.pntInSegment(pntCoordinate, p2, p3, pro3);
                                        let ds1 = (pntCoordinate.Sub(pro1).Clone()).Length();
                                        let ds2 = (pntCoordinate.Sub(pro2).Clone()).Length();
                                        let ds3 = (pntCoordinate.Sub(pro3).Clone()).Length();
                                        if (ds1 < ds2) {
                                            if (ds1 < ds3) {
                                                tempDistance = ds1;
                                                tempProjection = pro1;
                                            }
                                            else {
                                                tempDistance = ds3;
                                                tempProjection = pro3;
                                            }
                                        }
                                        else if (ds2 < ds3) {
                                            tempDistance = ds2;
                                            tempProjection = pro2;
                                        }
                                        else {
                                            tempDistance = ds3;
                                            tempProjection = pro3;
                                        }
                                        if (distance > tempDistance) {
                                            distance = tempDistance;
                                            projection.copyFrom(tempProjection);
                                        }
                                    }
                                }
                                maxDistance = max;
                                maxPoint.copyFrom(maxPnt);
                            }
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [distance, maxDistance];
                }
                else {
                    let pntCoordinate = restOfName[0];
                    let planeFaceFeatrue = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt = restOfName[3];
                    let ret = true;
                    let plane = new M3D.M3DMath.Plane(planeFaceFeatrue.getNormal(), planeFaceFeatrue.getOrigin());
                    projectPnt.copyFrom(plane.Project(pntCoordinate));
                    distance = projectPnt.Sub(pntCoordinate).Length();
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [ret, distance, projectPnt];
                }
            }
            lineLineDistance(...restOfName) {
                let methodName = this.lineLineDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 6) {
                    let result = [];
                    let lineFeature0 = restOfName[0];
                    let lineFeature1 = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let parallel = restOfName[5];
                    let alpha0 = lineFeature0.getEndPoint().Sub(lineFeature0.getStartPoint()).Normalized();
                    let alpha1 = lineFeature1.getEndPoint().Sub(lineFeature1.getStartPoint()).Normalized();
                    if (alpha0.CrossProduct(alpha1).Length() >= -0.001
                        && alpha0.CrossProduct(alpha1).Length() <= 0.001) {
                        //两直线平行
                        //转化到点到直线的距离
                        parallel = true;
                        let projectPnt = new M3D.M3DMath.Vector3();
                        let ray = new M3D.M3DMath.Ray(lineFeature0.getStartPoint(), lineFeature0.getDirection());
                        //		 Vector3 offset =lineAttribute1.GetStartPoint() -  ray.GetOrigin();
                        //		 Vector3 m_direction = lineAttribute1.GetDirection();
                        //		 Vector3 dot = offset.DotProduct(m_direction) * m_direction;
                        //		 LOGE("offset %s ",offset.Tostring().c_str());
                        //		 LOGE("GetDirection %s ",lineAttribute0.GetDirection().Tostring().c_str());
                        projectPnt.copyFrom(ray.Project(lineFeature1.getStartPoint()));
                        distance = lineFeature1.getStartPoint().Sub(projectPnt).Length();
                        let l1 = lineFeature0.getStartPoint().Sub(projectPnt).Length();
                        let l2 = lineFeature0.getEndPoint().Sub(projectPnt).Length();
                        let delta = lineFeature0.getLength() - (l1 + l2);
                        projectPnt0.copyFrom(projectPnt);
                        projectPnt1.copyFrom(lineFeature1.getStartPoint());
                    }
                    else {
                        parallel = false;
                        //空间异面直线
                        let Xa, Ya, Za, Xb, Yb, Zb, Xc, Yc, Zc, Xd, Yd, Zd;
                        Xa = Number(lineFeature0.getStartPoint().x);
                        Ya = Number(lineFeature0.getStartPoint().y);
                        Za = Number(lineFeature0.getStartPoint().z);
                        Xb = Number(lineFeature0.getEndPoint().x);
                        Yb = Number(lineFeature0.getEndPoint().y);
                        Zb = Number(lineFeature0.getEndPoint().z);
                        Xc = Number(lineFeature1.getStartPoint().x);
                        Yc = Number(lineFeature1.getStartPoint().y);
                        Zc = Number(lineFeature1.getStartPoint().z);
                        Xd = Number(lineFeature1.getEndPoint().x);
                        Yd = Number(lineFeature1.getEndPoint().y);
                        Zd = Number(lineFeature1.getEndPoint().z);
                        let H, I, J, K, L, M;
                        H = Xb - Xa;
                        I = Yb - Ya;
                        J = Zb - Za;
                        K = Xd - Xc;
                        L = Yd - Yc;
                        M = Zd - Zc;
                        let N, O, P, Q;
                        N = H * I * L - I * I * K - J * J * K + H * J * M;
                        O = H * H * L - H * I * K - I * J * M + J * J * L;
                        P = H * J * K - H * H * M - I * I * M + I * J * L;
                        Q = -Xa * N + Ya * O - Za * P;
                        let k = (O * Yc - N * Xc - P * Zc - Q) / (N * K - O * L + P * M);
                        let P1 = new M3D.M3DMath.Vector3(), P2 = new M3D.M3DMath.Vector3();
                        P1.x = K * k + Xc;
                        P1.y = L * k + Yc;
                        P1.z = M * k + Zc;
                        let projectPntT;
                        let rayT = new M3D.M3DMath.Ray(lineFeature0.getStartPoint(), lineFeature0.getDirection());
                        projectPntT = rayT.Project(P1);
                        projectPnt1.copyFrom(P1);
                        projectPnt0.copyFrom(projectPntT);
                        distance = projectPntT.Sub(P1).Length();
                    }
                    result.push(distance);
                    result.push(parallel);
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return result;
                }
                else if (restOfName.length == 7) {
                    if (restOfName[0] instanceof M3D.Shapes.EllipseFeature) {
                        let ellipseFeature0 = restOfName[0];
                        let ellipseFeature1 = restOfName[1];
                        let distance0 = restOfName[2];
                        let distance1 = restOfName[3];
                        let projectPnt0 = restOfName[4];
                        let projectPnt1 = restOfName[5];
                        let types = restOfName[6];
                        let center0 = new M3D.M3DMath.Vector3(), center1 = new M3D.M3DMath.Vector3(); //圆心
                        let radius0, radius1; //半径
                        let start0 = new M3D.M3DMath.Vector3(), start1 = new M3D.M3DMath.Vector3(); //起点
                        let dirX0 = new M3D.M3DMath.Vector3(), dirY0 = new M3D.M3DMath.Vector3(), dirZ0 = new M3D.M3DMath.Vector3(), dirX1 = new M3D.M3DMath.Vector3(), dirY1 = new M3D.M3DMath.Vector3(), dirZ1 = new M3D.M3DMath.Vector3();
                        let dirResult0 = ellipseFeature0.getXYZDir();
                        dirX0 = dirResult0[0];
                        dirY0 = dirResult0[1];
                        dirZ0 = dirResult0[2];
                        let dirResult1 = ellipseFeature1.getXYZDir();
                        dirX1 = dirResult1[0];
                        dirY1 = dirResult1[1];
                        dirZ1 = dirResult1[2];
                        center0 = ellipseFeature0.getCenter();
                        center1 = ellipseFeature1.getCenter();
                        radius0 = ellipseFeature0.getMajorRadius();
                        radius1 = ellipseFeature1.getMajorRadius();
                        start0 = ellipseFeature0.getStartPoint();
                        start1 = ellipseFeature1.getStartPoint();
                        let parall = dirZ0.CrossProduct(dirZ1);
                        if (parall.IsZero()) {
                            //所在平面平行
                            if (center0.Sub(center1).IsZero()) {
                                //两点相等 同心圆弧
                                distance0 = 0.0;
                                distance1 = M3D.M3DMath.Abs(radius0 - radius1);
                                projectPnt0.copyFrom(start0);
                                projectPnt1.copyFrom(center1.Add((start0.Sub(center0)).Multiply(radius1).Divide(start0.Sub(center0).Length())));
                                //projectPnt1 = start1;
                                types = 2;
                            }
                            else {
                                //非共面，求轴心，和 圆心距
                                let ray = new M3D.M3DMath.Ray(center0, dirZ0);
                                let projectPnt = ray.Project(center1);
                                distance0 = projectPnt.Sub(center1).Length();
                                distance1 = center0.Sub(center1).Length();
                                projectPnt0.copyFrom(projectPnt);
                                projectPnt1.copyFrom(center1);
                                types = 1;
                            }
                        }
                        else {
                            //所在平面不平行
                            types = 0;
                            distance0 = -1;
                            distance1 = center0.Sub(center1).Length();
                            projectPnt0.copyFrom(center0);
                            projectPnt1.copyFrom(center1);
                        }
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return [distance0, distance1, types];
                    }
                    else if (restOfName[0] instanceof M3D.Shapes.Edge) {
                        let lineSet1 = restOfName[0];
                        let lineSet2 = restOfName[1];
                        let lineSet1MinPoint = restOfName[2];
                        let lineSet2MinPoint = restOfName[3];
                        let distance = restOfName[4];
                        let worldMatrix1 = restOfName[5];
                        let worldMatrix2 = restOfName[6];
                        let vertexSets = this.pScene.getVertexSetManager();
                        let vertexHeader = vertexSets.getRenderVertexHeader(lineSet1);
                        let vertexset = vertexHeader.vertexSet;
                        let dataLength1 = vertexHeader.indexLength;
                        let offset1 = vertexHeader.indexOffset;
                        let pointArray = new Float32Array(vertexset.getMeshArray(), vertexHeader.vertexOffset, dataLength1);
                        //let result1 = MeshHelper.getEdgeData(lineSet1);
                        //let pntPoints: M3D.M3DMath.Vector3[] = result1[0];
                        //let offset1 = result1[1];
                        //let dataLength1 = result1[2];
                        let vertexHeader1 = vertexSets.getRenderVertexHeader(lineSet1);
                        let vertexset1 = vertexHeader.vertexSet;
                        let dataLength2 = vertexHeader.indexLength;
                        let offset2 = vertexHeader.indexOffset;
                        let pointArray1 = new Float32Array(vertexset.getMeshArray(), vertexHeader.vertexOffset, dataLength2);
                        //let result2 = MeshHelper.getEdgeData(lineSet2);
                        //let pntPoints2: M3D.M3DMath.Vector3[] = result2[0];
                        //let offset2 = result2[1];
                        //let dataLength2 = result2[2];
                        let distanceTemp = Number.MAX_VALUE;
                        //for (let i = 0; i < dataLength1 - 1; i++) {
                        //    let point1 = worldMatrix1.Multiply(pntPoints[i]);
                        //    let point2 = worldMatrix1.Multiply(pntPoints[i + 1]);
                        //    let alpha0 = point1.Sub(point2).Normalized();
                        //    for (let j = offset2; j < offset2 + dataLength2 - 1; j++) {
                        //        let line2Point1 = worldMatrix2.Multiply(pntPoints2[j]);
                        //        let line2Point2 = worldMatrix2.Multiply(pntPoints2[j + 1]);
                        //        let alpha1 = line2Point1.Sub(line2Point2).Normalized();
                        //        let results = MeasureCalculateHelper.SqrDistanceDouble(point1, point2, line2Point1, line2Point2, 0, 0);
                        //        let s = results[1];
                        //        let t = results[2];
                        //        let P1 = (point2.Sub(point1)).Multiply(s).Add(point1);
                        //        let P2 = (line2Point2.Sub(line2Point1)).Multiply(t).Add(line2Point1);
                        //        distance = P2.Sub(P1).Length();
                        //        console.log(distance);
                        //        if (distanceTemp >= distance) {
                        //            distanceTemp = distance;
                        //            lineSet1MinPoint = P1;
                        //            lineSet2MinPoint = P2;
                        //        }
                        //    }
                        //}
                        distance = distanceTemp;
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return [distance, lineSet1MinPoint, lineSet2MinPoint];
                    }
                }
            }
            lineFaceDistance(...restOfName) {
                let methodName = this.lineFaceDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length === 5) {
                    let lineFeature = restOfName[0];
                    let planeFaceFeature = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let ret = true;
                    let lineDirection = lineFeature.GetEndPoint().Sub(lineFeature.GetStartPoint());
                    let faceNormal = planeFaceFeature.GetNormal();
                    let projectPnt;
                    if (lineDirection.DotProduct(faceNormal) >= -0.001
                        && lineDirection.DotProduct(faceNormal) <= 0.001) {
                        distance = this.pointFaceDistance(lineFeature.GetStartPoint(), planeFaceFeature, distance, projectPnt0)[1];
                        distance = this.pointFaceDistance(lineFeature.GetEndPoint(), planeFaceFeature, distance, projectPnt1)[1];
                    }
                    else {
                        ret = false;
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [ret, distance];
                }
                else if (restOfName.length === 7) {
                    let face = restOfName[0];
                    let edge = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let worldMatrix1 = restOfName[5];
                    let worldMatrix2 = restOfName[6];
                    //let result1 = MeshHelper.getEdgeData(edge);
                    //let pntPoints: M3D.M3DMath.Vector3[] = result1[0];
                    //let offset = result1[1];
                    //let dataLength = result1[2];
                    //let result2 = MeshHelper.getFaceData(face);
                    //let positionArray: M3D.M3DMath.Vector3[] = result2[0];
                    //let facedataOffect = result2[1];
                    //let facedataLength = result2[2];
                    //let triangle = new TriangleHelper();
                    //let minTriangle = new TriangleHelper();
                    //let minLine = new Line3D();
                    //let distanceTemp = -1;
                    //let result;
                    //let pt1: M3D.M3DMath.Vector3, pt2: M3D.M3DMath.Vector3;
                    //for (let i = 0 + offset; i < offset + dataLength - 1; i++) {
                    //    let point1 = worldMatrix1.Multiply(pntPoints[i]);
                    //    let point2 = worldMatrix1.Multiply(pntPoints[i + 1]);
                    //    for (let j = 0; j < facedataOffect + facedataLength; j = j + 3) {
                    //        let p1 = worldMatrix2.Multiply(positionArray[j]);
                    //        let p2 = worldMatrix2.Multiply(positionArray[j + 1]);
                    //        let p3 = worldMatrix2.Multiply(positionArray[j + 2]);
                    //        triangle.SetVtx(p1, p2, p3);
                    //        result = triangle.GetDistance(point1, point2, pt1, pt2);
                    //        let distance1 = result[0];
                    //        pt1 = result[1];
                    //        pt2 = result[2];
                    //        if (distanceTemp == -1 || distanceTemp > distance1) {
                    //            distanceTemp = distance1;
                    //            projectPnt0 = pt1;
                    //            projectPnt1 = pt2;
                    //            //result = true;
                    //        }
                    //    }
                    //}
                    //distance = distanceTemp;
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [distance, projectPnt0, projectPnt1];
                }
            }
            pntInSegment(...restOfName) {
                let methodName = this.pntInSegment.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 4) {
                    if (restOfName[0] instanceof M3D.M3DMath.Vector3 && restOfName[1] instanceof M3D.M3DMath.Vector3 && restOfName[2] instanceof M3D.M3DMath.Vector3 && restOfName[3] instanceof M3D.M3DMath.Vector3) {
                        let pntCoordinate = restOfName[0];
                        let point1 = restOfName[1];
                        let point2 = restOfName[2];
                        let projecPoint = restOfName[3];
                        let startPointDistance;
                        let endPointDistance;
                        let direction = point2.Sub(point1);
                        let ray = new M3D.M3DMath.Ray(point1, direction);
                        let projection = ray.Project(pntCoordinate);
                        startPointDistance = (projection.Sub(point1)).Length();
                        endPointDistance = (projection.Sub(point2)).Length();
                        if ((startPointDistance + endPointDistance) > direction.Length()) {
                            if (startPointDistance > endPointDistance) {
                                projecPoint.copyFrom(point2);
                            }
                            else {
                                projecPoint.copyFrom(point1);
                            }
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return false;
                        }
                        else {
                            projecPoint.copyFrom(projection);
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return true;
                        }
                    }
                }
                else {
                    let pntCoordinate = restOfName[0];
                    let lineFeature = restOfName[1];
                    let startPntDistance, endPntDistance;
                    let start = lineFeature.getStartPoint();
                    let end = lineFeature.getEndPoint();
                    startPntDistance = pntCoordinate.Sub(start).Length();
                    endPntDistance = pntCoordinate.Sub(end).Length();
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    if ((startPntDistance + endPntDistance) > (start.Sub(end)).Length())
                        return false;
                    else
                        return true;
                }
            }
            setLine(...args) {
                let methodName = this.setLine.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (args && args.length == 4) {
                    let lineName = args[0];
                    let color = args[1];
                    let beginPoint = args[2];
                    let endPoint = args[3];
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() == lineName) {
                            lines[i].setStartPoint(beginPoint);
                            lines[i].setEndPoint(endPoint);
                            return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                    let leaderLine = new M3D.Shapes.Line();
                    leaderLine.setColor(color);
                    leaderLine.setStartPoint(beginPoint);
                    leaderLine.setEndPoint(endPoint);
                    leaderLine.setName(lineName);
                    this.measure.addLine(leaderLine);
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return M3D.Actions.ActionState.Action_Success;
                }
                else {
                    let lineName = args[0];
                    let position = args[1];
                    let allowTran = args[2];
                    let allowRotate = args[3];
                    let allowScale = args[4];
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        if (lines[i].getName() == lineName) {
                            lines[i].createBillBoard(position, allowTran, allowRotate, allowScale);
                            return M3D.Actions.ActionState.Action_Success;
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return M3D.Actions.ActionState.Action_Success;
                }
            }
            /**
             * @description 删除线
             * @param lineName 线的名称
             * */
            removetLine(lineName) {
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    if (lines[i].getName() == lineName) {
                        this.measure.removeLine(lines[i]);
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 设置点位置
             * @param index 点的index，position 点的坐标
             * */
            setPoint(name, position) {
                let methodName = this.setPoint.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let bFlag = false;
                let points = this.measure.getPointList();
                for (let i = 0; i < points.length; i++) {
                    if (points[i].getName() == name) {
                        bFlag = true;
                        points[i].setPosition(position);
                        if (points[i] instanceof M3D.Shapes.ImagePoint) {
                            points[i].getImage().setPosition(position);
                        }
                    }
                }
                if (!bFlag) {
                    let point = new M3D.Shapes.ImagePoint();
                    point.setName(name);
                    point.setPosition(position);
                    point.setScene(this.pScene);
                    this.measure.addPoint(point);
                    let pntImage = new M3D.Shapes.Image(position, new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //pntImage.setPosition(position);
                    //let pointSize = M3D.Utility.ShapeHelper.getCommonSize(this.pScene.getViewPorts().getViewport(0), new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //point.getImage().setSize(pointSize);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
            }
            /**
             * @description 删除点
             * @param pointName 点的名称
             * */
            removetPoint(pointName) {
                let points = this.measure.getPointList();
                for (let i = 0; i < points.length; i++) {
                    if (points[i].getName() == pointName) {
                        this.measure.removePoint(points[i]);
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 获取引线点（起止点）
             * @param firstPoint，secondPoint ，beginPoint 始点 ，endPoint 终点
             * */
            getLeaderPoints(firstPoint, secondPoint) {
                let methodName = this.getLeaderPoints.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let startPoint = (firstPoint.Add(secondPoint)).Divide(2);
                let lineLength = (firstPoint.Sub(secondPoint)).Length();
                let directionVector = secondPoint.Sub(firstPoint);
                let cameraRay = this.pScene.getCurrentViewPort().getCamera().getScreenRay(0, 0);
                let zDirection = cameraRay.GetDirection();
                directionVector = zDirection.CrossProduct(directionVector);
                directionVector.Normalize();
                let thirdCoord = firstPoint.Add(directionVector.Multiply(lineLength).Multiply(0.2));
                let fourCoord = secondPoint.Add(directionVector.Multiply(lineLength).Multiply(0.2));
                let endPoint = (thirdCoord.Add(fourCoord)).Divide(2);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return [startPoint, endPoint];
            }
            /**
             * @description 设置基本参数
             * */
            setBaseInfo() {
                if (this.measure) {
                    this.measure.setMeasureType(this.measureType);
                }
            }
            /**
             * @description 设置测量结果单位，一开始是按照mm来算
             * @param unitFlag 单位标志
             * @param value 数值
             * @param unit 单位
             * */
            getMeasureUnit(unitFlag, values, unit) {
                let methodName = this.getMeasureUnit.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                switch (unitFlag) {
                    case 0:
                        unit = "";
                        break;
                    case 1:
                        {
                            unit = "mm"; //毫米
                        }
                        break;
                    case 2:
                        {
                            unit = "cm"; //厘米
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] / 10;
                            }
                        }
                        break;
                    case 3:
                        {
                            unit = "m"; //米
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] / 1000;
                            }
                        }
                        break;
                    case 4:
                        {
                            unit = "in"; //英寸
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] * 0.0393701;
                            }
                        }
                        break;
                    case 5:
                        {
                            unit = "ft"; //英尺
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] * 0.0032808;
                            }
                        }
                        break;
                    default:
                        break;
                }
                unit = " " + unit;
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return unit;
            }
        }
        Actions.DistanceMeasureAction = DistanceMeasureAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @extends {M3D.Actions.TransformAction}
         */
        class DraggerAction extends M3D.Actions.TransformAction {
            /**
             * 构造函数
             * */
            constructor() {
                super();
            }
            /**
             * 执行
             * @returns Action状态
             * */
            onExecute() {
                return M3D.Actions.ActionState.Action_Error;
            }
        }
        Actions.DraggerAction = DraggerAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @beta
         * @memberof SView.Actions
         * @description 模型状态缓存类
         * @export
         * @class NodeStateCache
         */
        class NodeStateCache {
            constructor(...args) {
                this.sceneNode = null;
                this.matrixCache = new M3D.M3DMath.Matrix4();
                this.boxCache = new M3D.M3DMath.BoundingBox();
                if (args.length === 3) {
                    this.sceneNode = args[0];
                    this.matrixCache = args[1];
                    this.boxCache = args[2];
                }
            }
            copyFrom(nodeState) {
                this.sceneNode = nodeState.sceneNode;
                this.matrixCache = nodeState.matrixCache;
                this.boxCache = nodeState.boxCache;
            }
        }
        Actions.NodeStateCache = NodeStateCache;
        /**
         * @memberof SView.Actions
         * @description 爆炸类型枚举
         * @export
         * @enum {number}
         */
        let ExplosiveStyle;
        (function (ExplosiveStyle) {
            ExplosiveStyle[ExplosiveStyle["NOEXPLOSIVE"] = -1] = "NOEXPLOSIVE";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER"] = 0] = "AWAYFROMCENTER";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_X"] = 1] = "AWAYFROMCENTER_X";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_Y"] = 2] = "AWAYFROMCENTER_Y";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_Z"] = 3] = "AWAYFROMCENTER_Z";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_X_LEFT"] = 4] = "AWAYFROMCENTER_X_LEFT";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_Y_FRONT"] = 5] = "AWAYFROMCENTER_Y_FRONT";
            ExplosiveStyle[ExplosiveStyle["AWAYFROMCENTER_Z_BOTTOM"] = 6] = "AWAYFROMCENTER_Z_BOTTOM";
            ExplosiveStyle[ExplosiveStyle["TOBALLSURFACE"] = 9] = "TOBALLSURFACE";
        })(ExplosiveStyle = Actions.ExplosiveStyle || (Actions.ExplosiveStyle = {}));
        /**
         * @memberof SView.Actions
         * @description 爆炸Action
         * @export
         * @class ExplosiveAction
         * @extends {M3D.Actions.Action}
         */
        class ExplosiveAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of ExplosiveAction.
             * @memberof ExplosiveAction
             */
            constructor() {
                super();
                this.direction = 0; // 爆炸方向
                this.level = 0; //爆炸级别
                this.percent = 0; //爆炸百分比
                this.explosionModels = []; //爆炸模型
                this.allNodeMatrixsCacheMap = new Map();
                this.setLevel(ExplosiveAction.MAX_ASSEMBLY_LEVLE);
                this.reset();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof ExplosiveAction
             */
            setScene(scene) {
                this.scene = scene;
                let sceneBox = this.scene.getBoundingBox().Clone();
                let maxPos = sceneBox.max.Clone();
                let minPos = sceneBox.min.Clone();
                //场景包围盒中心
                this.topNodeCenter = (maxPos.Add(minPos)).Multiply(0.5);
                //XY面中心点
                this.topNodeBottom = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, maxPos.y, minPos.z))).Multiply(0.5);
                //YZ面中心点
                this.topNodeFront = (minPos.Add(new M3D.M3DMath.Vector3(minPos.x, maxPos.y, maxPos.z))).Multiply(0.5);
                //XZ面中心点
                this.topNodeLeft = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, minPos.y, maxPos.z))).Multiply(0.5);
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof ExplosiveAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @beta
             * @description 获取爆炸等级
             * @return {number}
             * @memberof ExplosiveAction
             */
            getLevel() {
                return this.level;
            }
            setLevel(value) {
                this.level = value;
            }
            /**
             * @public
             * @description 设置爆炸方向
             * @param {number} type
             * @memberof ExplosiveAction
             */
            setDrection(type) {
                this.direction = type;
            }
            /**
             * @public
             * @description 获取爆炸方向
             * @return {number}
             * @memberof ExplosiveAction
             */
            getDrection() {
                return this.direction;
            }
            /**
             * @public
             * @description 获取爆炸进度
             * @return {number}
             * @memberof ExplosiveAction
             */
            getPercent() {
                return this.percent;
            }
            /**
             * @beta
             * @return {any[]}
             * @memberof ExplosiveAction
             */
            getExplosionModels() {
                return this.explosionModels;
            }
            /**
             * @description 执行爆炸
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof ExplosiveAction
             */
            onExecute() {
                // let sceneBox = this.scene.getBoundingBox().Clone();
                // let maxPos = sceneBox.max.Clone();
                // let minPos = sceneBox.min.Clone();
                // //场景包围盒中心
                // this.topNodeCenter = (maxPos.Add(minPos)).Multiply(0.5);
                // //XY面中心点
                // this.topNodeBottom = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, maxPos.y, minPos.z))).Multiply(0.5);
                // //YZ面中心点
                // this.topNodeFront = (minPos.Add(new M3D.M3DMath.Vector3(minPos.x, maxPos.y, maxPos.z))).Multiply(0.5);
                // //XZ面中心点
                // this.topNodeLeft = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, minPos.y, maxPos.z))).Multiply(0.5);
                if (this.explosionModels.length > 0) {
                    for (let explosionModel of this.explosionModels) {
                        let childModels = explosionModel.getChildren();
                        for (let model of childModels) {
                            this.cacheMatrixState(model);
                        }
                    }
                }
                else {
                    let topModel = this.scene.getTopModel();
                    let selectChildModels = topModel.getChildren();
                    for (let model of selectChildModels) {
                        this.cacheMatrixState(model);
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @public
             * @description 设置进度
             * @param {number} style 类型
             * @param {number} [percent=100.0] 进度
             * @param {boolean} [useAnimation=true] 是否使用动画
             * @memberof ExplosiveAction
             */
            setPercent(style, percent = 100.0, useAnimation = true) {
                if (style != this.direction || percent != this.percent) {
                    this.direction = style;
                    this.percent = percent;
                    if (this.explosionModels.length > 0) {
                        for (let explosionModel of this.explosionModels) {
                            let childModels = explosionModel.getChildren();
                            for (let model of childModels) {
                                this.procressAwayFromCenter(model);
                            }
                        }
                    }
                    else {
                        let topModel = this.scene.getTopModel();
                        let selectchildModels = topModel.getChildren();
                        for (let model of selectchildModels) {
                            this.procressAwayFromCenter(model);
                        }
                    }
                }
            }
            /**
             * @private
             * @param {M3D.Shapes.Model} model
             * @return {*}
             * @memberof ExplosiveAction
             */
            procressAwayFromCenter(model) {
                if (!model) {
                    return;
                }
                let currentNodeCanExplosive = true;
                if (currentNodeCanExplosive && model.hasDrawData()) {
                    let nodeState = new NodeStateCache();
                    let hasMatrix = this.getMatrix(model, nodeState);
                    if (hasMatrix) {
                        let nodestate = nodeState;
                        let moveVector;
                        let referencePosition;
                        let matrixCache = nodestate.matrixCache.Clone();
                        let shapeNodeBox = nodestate.boxCache.Clone();
                        let modelPosition = shapeNodeBox.Center().Clone();
                        let tDisPos = modelPosition.Clone();
                        if (this.direction === ExplosiveStyle.AWAYFROMCENTER) {
                            referencePosition = this.topNodeCenter.Clone();
                        }
                        else if (this.direction === ExplosiveStyle.AWAYFROMCENTER_X) {
                            //X direction
                            referencePosition = this.topNodeFront.Clone();
                            referencePosition.y = tDisPos.y;
                            referencePosition.z = tDisPos.z;
                        }
                        else if (this.direction === ExplosiveStyle.AWAYFROMCENTER_Y) {
                            //Y direction
                            referencePosition = this.topNodeLeft.Clone();
                            referencePosition.x = tDisPos.x;
                            referencePosition.z = tDisPos.z;
                        }
                        else if (this.direction === ExplosiveStyle.AWAYFROMCENTER_Z) {
                            //Z direction
                            referencePosition = this.topNodeBottom.Clone();
                            referencePosition.x = tDisPos.x;
                            referencePosition.y = tDisPos.y;
                        }
                        //世界坐标系下的移动距离
                        moveVector = (modelPosition.Sub(referencePosition)).Multiply(this.percent);
                        let parentModel = model.getParentModel();
                        //当前模型父矩阵
                        let parentMat;
                        if (parentModel) {
                            parentMat = parentModel.getWorldTransform().Clone();
                        }
                        matrixCache.data[3] += moveVector.x;
                        matrixCache.data[7] += moveVector.y;
                        matrixCache.data[11] += moveVector.z;
                        // 将父矩阵转为相对于父模型的配置矩阵
                        let plcMat = parentMat.Inverse().Multiply(matrixCache);
                        model.setPlaceMatrix(plcMat);
                        model.markDirty();
                    }
                }
                let childModels = model.getChildren();
                if (childModels != null && childModels.length > 0) {
                    for (let childModel of childModels) {
                        this.procressAwayFromCenter(childModel);
                    }
                }
                this.getScene().refresh();
            }
            /**
             * @private
             * @param {M3D.Shapes.Model} model
             * @memberof ExplosiveAction
             */
            cacheMatrixState(model) {
                if (this.level >= ExplosiveAction.MAX_ASSEMBLY_LEVLE) {
                    if (model && model.hasDrawData()) {
                        let modelMatrix = model.getWorldTransform().Clone();
                        let box = model.getWorldBoundingBox().Clone();
                        let nodeState = new NodeStateCache(model, modelMatrix, box);
                        this.addMatrix(model, nodeState);
                    }
                }
                let childModels = model.getChildren();
                for (let cModel of childModels) {
                    this.cacheMatrixState(cModel);
                }
            }
            /**
             * @private
             * @param {M3D.Shapes.Model} model
             * @param {NodeStateCache} nodeState
             * @memberof ExplosiveAction
             */
            addMatrix(model, nodeState) {
                this.allNodeMatrixsCacheMap.set(model, nodeState);
            }
            /**
             * @private
             * @param {M3D.Shapes.Model} model
             * @param {NodeStateCache} nodeStateCache
             * @return {*}
             * @memberof ExplosiveAction
             */
            getMatrix(model, nodeStateCache) {
                let hasMatrix = false;
                if (!model) {
                    return hasMatrix;
                }
                let nodeMatrixsCache = this.allNodeMatrixsCacheMap.get(model);
                if (nodeMatrixsCache) {
                    nodeStateCache.copyFrom(nodeMatrixsCache);
                    hasMatrix = true;
                }
                return hasMatrix;
            }
            /**
             * @public
             * @description 重置爆炸状态
             * @memberof ExplosiveAction
             */
            reset() {
                this.direction = ExplosiveStyle.NOEXPLOSIVE;
                this.percent = 0;
                this.allNodeMatrixsCacheMap.clear();
                this.explosionModels = [];
            }
            /**
             * @private
             * @param {M3D.Shapes.Model[]} explosionModels
             * @memberof ExplosiveAction
             */
            setExplosiveModel(explosionModels) {
                if (explosionModels.length > 0) {
                    this.explosionModels = explosionModels;
                }
                else {
                    //获取所有选中模型
                    let selectShapes = this.scene.getSelector().getAll();
                    if (selectShapes.length > 0) {
                        let selectModels = [];
                        for (let shape of selectShapes) {
                            if (shape != null &&
                                shape instanceof M3D.Shapes.Model) {
                                let model = shape;
                                selectModels.push(model);
                            }
                        }
                        this.explosionModels = selectModels;
                    }
                    else {
                        this.explosionModels = [];
                        let scene = this.getScene();
                        if (scene != null) {
                            this.explosionModels.push(this.getScene().getTopModel());
                        }
                    }
                }
            }
        }
        ExplosiveAction.MAX_ASSEMBLY_LEVLE = 32; // 最大的可爆炸装配级别
        Actions.ExplosiveAction = ExplosiveAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @beta
         * @memberof SView.Actions
         * @description 装配状态缓存类
         * @export
         * @class BomNodeStateCache
         */
        class BomNodeStateCache {
            constructor(...args) {
                this.sceneNode = null;
                this.worldTransform = new M3D.M3DMath.Matrix4();
                this.boxCache = new M3D.M3DMath.BoundingBox();
                this.matrixCache = new M3D.M3DMath.Matrix4();
                if (args.length === 4) {
                    this.sceneNode = args[0];
                    this.matrixCache = args[1];
                    this.worldTransform = args[2];
                    this.boxCache = args[3];
                }
            }
            copyFrom(nodeState) {
                this.sceneNode = nodeState.sceneNode;
                this.worldTransform = nodeState.worldTransform;
                this.boxCache = nodeState.boxCache;
                this.matrixCache = nodeState.matrixCache;
            }
        }
        Actions.BomNodeStateCache = BomNodeStateCache;
        /**
         * @beta
         * @memberof SView.Actions
         * @description 分级爆炸类
         * @export
         * @class ExplosiveBomAction
         * @extends {M3D.Actions.Action}
         */
        class ExplosiveBomAction extends M3D.Actions.Action {
            constructor() {
                super();
                // 初始爆炸模型
                this.explosionModels = [];
                // 分级爆炸模型map
                this.explosionLevelModelsMap = new Map();
                this.allNodeMatrixsCacheMap = new Map();
                this.scene = null;
                this.percent = 0;
                this.direction = 0;
                this.explosionModels = [];
            }
            setScene(scene) {
                this.scene = scene;
                let sceneBox = this.scene.getBoundingBox().Clone();
                let maxPos = sceneBox.max.Clone();
                let minPos = sceneBox.min.Clone();
                //场景包围盒中心
                this.topNodeCenter = (maxPos.Add(minPos)).Multiply(0.5);
                //XY面中心点
                this.topNodeBottom = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, maxPos.y, minPos.z))).Multiply(0.5);
                //YZ面中心点
                this.topNodeFront = (minPos.Add(new M3D.M3DMath.Vector3(minPos.x, maxPos.y, maxPos.z))).Multiply(0.5);
                //XZ面中心点
                this.topNodeLeft = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, minPos.y, maxPos.z))).Multiply(0.5);
            }
            getScene() {
                return this.scene;
            }
            setLevel(level) {
                this.level = level;
            }
            getLevel() {
                return this.level;
            }
            setPercent(percent) {
                this.percent = percent;
            }
            getPercent() {
                return this.percent;
            }
            setDirection(diretion) {
                this.direction = diretion;
            }
            getDirection() {
                return this.direction;
            }
            onExecute() {
                for (let [level, subArr] of this.explosionLevelModelsMap) {
                    if (subArr.length <= 1)
                        continue;
                    for (let levelModel of subArr) {
                        this.explosionModel(levelModel, level);
                    }
                }
                this.getScene().refresh();
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 爆炸指定模型
             * @param model
             * @param level
             * @private
             */
            explosionModel(model, level) {
                if (model == null)
                    return;
                let nodeCacheState = new BomNodeStateCache();
                let hasMatrix = this.getMatrix(model, nodeCacheState);
                if (hasMatrix) {
                    // 复位
                    model.setPlaceMatrix(nodeCacheState.matrixCache);
                    model.markDirty();
                    let parentModel = nodeCacheState.sceneNode.getParentModel();
                    if (!parentModel || level > this.getLevel())
                        return;
                    let parentStatus = new BomNodeStateCache();
                    let parentStatusHas = this.getMatrix(parentModel, parentStatus);
                    if (parentStatusHas) {
                        let nodeCenter = nodeCacheState.boxCache.Center().Clone();
                        let parentCenter = parentModel ? parentStatus.boxCache.Center().Clone() : this.topNodeCenter.Clone();
                        let mov = this.getModelDirection(nodeCenter, parentCenter).MultiplyED(this.percent);
                        let modelWMat = model.getWorldTransform().Clone();
                        let pareWMat = parentModel ? parentModel.getWorldTransform().Clone() : new M3D.M3DMath.Matrix4();
                        let tempmat = new M3D.M3DMath.Matrix4();
                        tempmat.MultiTranslate(mov);
                        let plcMat = pareWMat.Inverse().Multiply(tempmat.Multiply(modelWMat));
                        model.setPlaceMatrix(plcMat);
                    }
                    model.markDirty();
                }
            }
            /**
             * @public
             * @description 重置分级爆炸
             * @memberof ExplosiveBomAction
             */
            reset() {
                // this.allNodeMatrixsCacheMap.clear();
                this.explosionModels.length = 0;
                this.explosionLevelModelsMap.clear();
            }
            /**
             * @description 获取方向
             * @param nodeCenter
             * @param parentCenter
             * @private
             */
            getModelDirection(nodeCenter, parentCenter) {
                let direction = new M3D.M3DMath.Vector3();
                switch (this.getDirection()) {
                    case SView.Actions.ExplosiveStyle.AWAYFROMCENTER:
                        direction = new M3D.M3DMath.Vector3(nodeCenter.Sub(parentCenter));
                        break;
                    case SView.Actions.ExplosiveStyle.AWAYFROMCENTER_X:
                        direction.x = nodeCenter.x = parentCenter.x;
                        break;
                    case SView.Actions.ExplosiveStyle.AWAYFROMCENTER_Y:
                        direction.y = nodeCenter.y - parentCenter.y;
                        break;
                    case SView.Actions.ExplosiveStyle.AWAYFROMCENTER_Z:
                        direction.z = nodeCenter.z - parentCenter.z;
                        break;
                }
                return direction;
            }
            /**
             * @description 设置爆炸模型数据
             * @param models
             */
            setExplosionModels(models) {
                for (let i = 0; i < models.length; i++) {
                    let model = models[i];
                    let isExist = this.explosionModels.indexOf(model);
                    if (isExist == -1) {
                        this.explosionModels.push(model);
                        this.addExplosiveModel(model, 0);
                    }
                }
            }
            /**
             * @description 添加爆炸模型
             * @param model
             * @param level
             * @private
             */
            addExplosiveModel(model, level) {
                if (model == null)
                    return;
                let arr = [];
                let levelArr = this.explosionLevelModelsMap.get(level);
                if (levelArr != null) {
                    if (levelArr.indexOf(model) >= 0) {
                        return;
                    }
                    levelArr.push(model);
                    arr = levelArr;
                }
                else {
                    arr.push(model);
                }
                // ios 9.1 的逻辑先注释
                // if (level == 0) {
                //     let sceneBox = model.getBoundingBox().Clone();
                //     if (sceneBox == M3D.M3DMath.BoundingBox.NO_BOX()) {
                //
                //     }
                //     let maxPos = sceneBox.max.Clone();
                //     let minPos = sceneBox.min.Clone();
                //     //场景包围盒中心
                //     this.topNodeCenter = (maxPos.Add(minPos)).Multiply(0.5);
                //     //XY面中心点
                //     this.topNodeBottom = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, maxPos.y, minPos.z))).Multiply(0.5);
                //     //YZ面中心点
                //     this.topNodeFront = (minPos.Add(new M3D.M3DMath.Vector3(minPos.x, maxPos.y, maxPos.z))).Multiply(0.5);
                //     //XZ面中心点
                //     this.topNodeLeft = (minPos.Add(new M3D.M3DMath.Vector3(maxPos.x, minPos.y, maxPos.z))).Multiply(0.5);
                //
                // }
                this.cacheMatrixState(model);
                this.explosionLevelModelsMap.set(level, arr);
                let children = model.getChildren();
                for (let child of children) {
                    this.addExplosiveModel(child, level + 1);
                }
            }
            /**
             * @description 记录模型初始位置信息
             * @param model
             * @param recursion 是否递归当前模型的子模型
             */
            cacheMatrixState(model, recursion = false) {
                if (model != null /*&& model.hasDrawData()*/) {
                    let plcMat = model.getPlaceMatrix().Clone();
                    let transform = model.getWorldTransform().Clone();
                    let box = model.getTotalWorldBoundingBox().Clone();
                    let nodeState = new BomNodeStateCache(model, plcMat, transform, box); //new NodeStateCache(model, modelMatrix, box);
                    this.addMatrix(model, nodeState);
                }
                if (recursion) {
                    //缓存子件状态
                    let childModels = model.getChildren();
                    if (childModels.length > 0) {
                        for (let childModel of childModels) {
                            this.cacheMatrixState(childModel);
                        }
                    }
                }
            }
            /**
             * @description 添加模型对应的缓存状态
             * @param model
             * @param nodeState
             * @private
             */
            addMatrix(model, nodeState) {
                this.allNodeMatrixsCacheMap.set(model, nodeState);
            }
            /**
             * @description 获取模型对应的缓存状态
             * @param model
             * @param nodeStateCache
             * @private
             */
            getMatrix(model, nodeStateCache) {
                let hasMatrix = false;
                if (!model) {
                    return hasMatrix;
                }
                let nodeMatrixsCache = this.allNodeMatrixsCacheMap.get(model);
                if (nodeMatrixsCache) {
                    nodeStateCache.copyFrom(nodeMatrixsCache);
                    hasMatrix = true;
                }
                return hasMatrix;
            }
        }
        Actions.ExplosiveBomAction = ExplosiveBomAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 设置模型颜色
         * @export
         * @class ModelSetColorAction
         * @extends {M3D.Actions.Action}
         */
        class ModelSetColorAction extends M3D.Actions.Action {
            /**
             * @public
             * @description 设置颜色
             * @param {M3D.M3DMath.Color} color
             * @memberof ModelSetColorAction
             */
            setColor(color) {
                if (color == null)
                    return;
                this.color = color;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof ModelSetColorAction
             */
            setScene(scene) {
                if (scene == null)
                    return;
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof ModelSetColorAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 获取颜色
             * @return {M3D.M3DMath.Color}
             * @memberof ModelSetColorAction
             */
            getColor() {
                return this.color;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {*}  {M3D.Actions.ActionState}
             * @memberof ModelSetColorAction
             */
            onExecute() {
                if (this.scene == null ||
                    this.color == null) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let shapes = this.scene.getSelector().getAll();
                if (shapes == null ||
                    shapes.length <= 0) {
                    // shapes = this.scene.getTopModel().getChildren();
                    return M3D.Actions.ActionState.Action_GetObjectFailure;
                }
                for (let shape of shapes) {
                    if (shape instanceof M3D.Shapes.Model) {
                        let model = shape;
                        model.setColor(this.color);
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.ModelSetColorAction = ModelSetColorAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @deprecated
         * @description 视图Action
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class ModelViewAction extends M3D.Actions.Action {
            constructor() {
                super();
                this.isUpDataCamera = false; //标识是否更新Camera
                this.isUpDataModel = true; //标识是否更新模型的矩阵
                this.isDefaultView = false; //是否是默认视图
                this.isUpDataCamera = true;
                this.isUpDataModel = true;
                this.isDefaultView = false;
            }
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @description 设置Action的名称
             * @param nameStr
             */
            setName(nameStr) {
                this.name = nameStr;
            }
            /**
             * @description 设置视图快照截图Url
             * @param nameStr
             */
            setSnapshotUrl(snapshotUrl) {
                this.snapshotUrl = snapshotUrl;
            }
            /**
             * @description 设置是否更新相机
             * @param updata
             */
            setUpdateCamera(updata) {
                this.isUpDataCamera = updata;
            }
            /**
             * @description 设置是否更新模型矩阵
             * @param updata
             */
            setUpdateModel(updata) {
                this.isUpDataCamera = updata;
            }
            /**
             * @description 设置是否为默认视图
             * @param isDefault
             */
            setIsDefaultView(isDefault) {
                this.isDefaultView = isDefault;
            }
            /**
             * @description 设置视图的类型
             * @param nameStr
             */
            setViewType(type) {
                this.type = type;
            }
            onExecute() {
                if (!this.scene) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                this.modelView = new M3D.Shapes.ModelView();
                this.modelView.setViewType(this.type);
                this.modelView.setUpDataCamera(this.isUpDataCamera);
                this.modelView.setUpDataModel(this.isUpDataModel);
                //this.updateViewByCurrentScene();//更新的视图内容
                //this.addModelView();
                //this.addModelViewList();
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 服务器创建一个新的视图
             * @returns 视图
             * */
            static create() {
                let nowTime = M3D.Utility.TimeHelper.getCurrentTime();
                let currentModelView = new M3D.Shapes.ModelView();
                currentModelView.setName(nowTime);
                currentModelView.setViewType(M3D.Shapes.ViewTypeEnum.UserView); //自定义视图
                currentModelView.setUpDataCamera(true); //标识是否更新Camera
                currentModelView.setUpDataModel(true); //标识是否更新模型的矩阵
                return currentModelView;
            }
            /**
             *
             * @description 将新添加的视图添加进视图列表Shapes
             * @param modelView
             */
            addModelViewList(modelView) {
                let exist = false;
                let temp_id = 0;
                let modelViews = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SHAPE_MODELVIEW);
                if (modelViews.length) {
                    for (let i = 0; i < modelViews.length; i++) {
                        if (modelViews[i].getObjectId() === modelView.getObjectId()) {
                            Actions.ActionLogger.LOGI(ModelViewAction.name, "", "", "Exist View Id is " + modelView.getObjectId());
                            exist = true;
                            temp_id = i;
                            break;
                        }
                    }
                }
                if (exist) {
                    Actions.ActionLogger.LOGI(ModelViewAction.name, "", "", "Exist View Id is " + modelView.getObjectId());
                    modelViews[temp_id] = this.modelView;
                }
                else {
                    modelViews.push(modelView);
                    this.scene.getShapes().addShape(modelView);
                }
            }
            ///**
            // * 获取视图相机及模型位置等数据
            // * 
            // * */
            //updateViewByCurrentScene(): boolean {
            //    let ret = false;
            //    if (this.modelView.getUpDataCameraState()) {
            //        //使用当前相机状态填充视图相机数据
            //        let pCurCamera = this.scene.getCurrentViewPort().getCamera();
            //        let newViewCamera: M3D.Camera = new M3D.Camera();
            //        let position: M3D.M3DMath.Vector3 = pCurCamera.getPosition();
            //        newViewCamera.setPosition(position);
            //        newViewCamera.setRotate(pCurCamera.getRotationQuaternion());
            //        newViewCamera.setZoom(pCurCamera.getZoom());
            //        let orthoSize = pCurCamera.getOrthoSize();
            //        newViewCamera.setOrthoSize(orthoSize);
            //        newViewCamera.setOrthographic(pCurCamera.isOrthographic());
            //        newViewCamera.setAspectRatio(pCurCamera.getAspectRatio());
            //        newViewCamera.setFov(pCurCamera.getFov());
            //        newViewCamera.setFarClip(pCurCamera.getFarClip());
            //        newViewCamera.setNearClip(pCurCamera.getNearClip());
            //        //存在相机信息，则用字符串形式存储，后期使用时候解析
            //        this.modelView.setCamera(newViewCamera);
            //        let newViewCameraJson = M3D.Reader.ModelViewFactory.toJSONByCamera(newViewCamera);
            //        let modelViewJsonObj = JSON.parse(newViewCameraJson);
            //        this.modelView.setCameraStr(modelViewJsonObj);
            //        this.modelView.setUpDataCamera(true);
            //    }
            //    //生成原始视图数据记录
            //    //if (this.modelView.getName().indexOf("OriginalView") != -1 && this.modelView.getViewType() == M3D.Shapes.ViewTypeEnum.OrignalView) {
            //    //    let insAttMap: Map<number, M3D.Shapes.InstanceAttribute>;
            //    //    let allSubModels: M3D.Shapes.Model[] = <M3D.Shapes.Model[]>this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODEL);
            //    //    if (!allSubModels) {
            //    //        //数据错误 return
            //    //        return;
            //    //    }
            //    //    for (let curModel of allSubModels) {
            //    //        let ia = new M3D.Shapes.InstanceAttribute();
            //    //        ia.id = curModel.getId();
            //    //        ia.materialId = -1;
            //    //        ia.visible = curModel.isVisible();
            //    //        ia.path = M3D.Utility.PathHelper.M3DPathToSVLPath(curModel.getPlcPath());
            //    //        if (curModel.getChildren().length <= 0) {
            //    //            let pTmpColor;
            //    //            if (curModel.hasInstanceMaterial()) {
            //    //                let curModeMaterial = curModel.getMaterial();
            //    //                if (!curModeMaterial) {
            //    //                    continue;
            //    //                }
            //    //                switch (curModeMaterial.getMaterialType()) {
            //    //                    case M3D.Render.MaterialType.MaterialType_Phong:
            //    //                        {
            //    //                            pTmpColor = (<M3D.Render.Material>curModeMaterial).getDiffuse();
            //    //                        }
            //    //                        break;
            //    //                    case M3D.Render.MaterialType.MaterialType_Pbr:
            //    //                        {
            //    //                            pTmpColor = (<M3D.Render.PbrMaterial>curModeMaterial).getAlbedoColor();
            //    //                        }
            //    //                        break;
            //    //                    case M3D.Render.MaterialType.MaterialType_MatCap:
            //    //                        {
            //    //                            pTmpColor = (<M3D.Render.MatCapMaterial>curModeMaterial).getDiffuse();
            //    //                        }
            //    //                        break;
            //    //                    default:
            //    //                        break;
            //    //                }
            //    //                if (curModeMaterial.getMaterialType() == M3D.Render.MaterialType.MaterialType_Shader) {
            //    //                    ia.hasColor = false;
            //    //                    ia.insColor = M3D.M3DMath.Color.GRAY();
            //    //                } else {
            //    //                    ia.hasColor = true;
            //    //                    ia.insColor = pTmpColor;
            //    //                }
            //    //            }
            //    //            else {
            //    //                //TODO:
            //    //                if (!M3D.Resources.isModelHaveMultyMaterial(curModel)) {
            //    //                    if (curModel.getMaterial() && curModel.getMaterial().getMaterialType() === M3D.Render.MaterialType.MaterialType_Shader) {
            //    //                        ia.hasColor = false;
            //    //                        ia.insColor = M3D.M3DMath.Color.GRAY();
            //    //                    }
            //    //                    else {
            //    //                        let pColor = curModel.getColor();
            //    //                        ia.hasColor = true;
            //    //                        ia.insColor = pColor;
            //    //                    }
            //    //                }
            //    //                else {
            //    //                    ia.hasColor = false;
            //    //                    ia.insColor = M3D.M3DMath.Color.GRAY();
            //    //                }
            //    //            }
            //    //        }
            //    //        else {
            //    //            ia.hasColor = false;
            //    //        }
            //    //        let matrixTran = curModel.getPlaceMatrix();
            //    //        ia.placeMatrix = matrixTran;
            //    //        ia.hasMatrix = true;
            //    //        insAttMap.set(ia.id, ia);
            //    //    }
            //    //    //InstanceAttribute对象及字符串存两份
            //    //    this.modelView.setInstanceAttributeMap(insAttMap);
            //    //    let insAttMapJson = M3D.Reader.ModelViewFactory.toJSONByInsAttributes(insAttMap);
            //    //    let insAttMapJsonObj = JSON.parse(insAttMapJson);
            //    //    this.modelView.setInstanceAttributeStr(insAttMapJsonObj);
            //    //}else if (true) {
            //        if (this.modelView.getUpDataModelState()) {
            //            let allSubModels: M3D.Shapes.Model[] = <M3D.Shapes.Model[]>this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODEL);
            //            if (!allSubModels) {
            //                //数据错误 return
            //                return;
            //            }
            //            let insAttMap: Map<number, M3D.Shapes.InstanceAttribute> = new Map<number, M3D.Shapes.InstanceAttribute>();
            //            let plcPath;
            //            for (let curModel of allSubModels) {
            //                //ia临时变量用于记录当前模型状态与原始视图是否一致，如果不一致
            //                let instanceflag = false;
            //                let ia: M3D.Shapes.InstanceAttribute = new M3D.Shapes.InstanceAttribute();
            //                ia.hasVisble = false;
            //                ia.hasColor = false;
            //                ia.hasMatrix = false;
            //                ia.id = curModel.getInstanceId();
            //                //ia.id = curModel.getObjectId();
            //                ia.insColor = M3D.M3DMath.Color.GRAY();
            //                ia.materialId = -1;
            //                let protoShape = curModel.getProto();
            //                if (curModel.isOriginalVisible() != curModel.isVisible()) {
            //                    instanceflag = true;
            //                    ia.hasVisble = true;
            //                    ia.visible = curModel.isVisible();
            //                } else {
            //                    //ia.hasVisble = curModel.o
            //                    ia.visible = curModel.isOriginalVisible();
            //                }
            //                if (curModel.getChildren().length <= 0) {
            //                    let pColor = curModel.getColor();
            //                    let oriColor = curModel.getOriginalColor();
            //                    let curModeMaterial = curModel.getMaterial();
            //                    let pTmpColor;
            //                    if (curModel.hasInstanceMaterial()) {
            //                        let curModeMaterial = curModel.getMaterial();
            //                        if (!curModeMaterial) {
            //                            continue;
            //                        }
            //                        switch (curModeMaterial.getMaterialType()) {
            //                            case M3D.Render.MaterialType.MaterialType_Phong:
            //                                {
            //                                    pTmpColor = (<M3D.Render.Material>curModeMaterial).getDiffuse();
            //                                }
            //                                break;
            //                            case M3D.Render.MaterialType.MaterialType_Pbr:
            //                                {
            //                                    pTmpColor = (<M3D.Render.PbrMaterial>curModeMaterial).getAlbedoColor();
            //                                }
            //                                break;
            //                            case M3D.Render.MaterialType.MaterialType_MatCap:
            //                                {
            //                                    pTmpColor = (<M3D.Render.MatCapMaterial>curModeMaterial).getDiffuse();
            //                                }
            //                                break;
            //                            default:
            //                                break;
            //                        }
            //                        if (pTmpColor != oriColor) {
            //                            instanceflag = true;
            //                            ia.hasColor = true;
            //                            ia.insColor = pTmpColor;
            //                        }
            //                    }
            //                }
            //                ia.path = curModel.getPlcPath();
            //                let totalBoundingBox = curModel.getTotalBoundingBox();
            //                //let precision = totalBoundingBox.Length() * 0.01;
            //                let matrixTran = curModel.getPlaceMatrix();
            //                let oriMatrixTran = curModel.getOriginalMatrix();
            //                if (!matrixTran.Equals(oriMatrixTran) &&
            //                    !matrixTran.Translation().Equals(oriMatrixTran.Translation())) {
            //                    instanceflag = true;
            //                    ia.hasMatrix = true;
            //                }
            //                ia.placeMatrix = matrixTran;
            //                //if (instanceflag) {
            //                    insAttMap.set(ia.id, ia);
            //                //}
            //            }
            //            this.modelView.setInstanceAttributeMap(insAttMap);
            //            let insAttMapJson = M3D.Reader.ModelViewFactory.toJSONByInsAttributes(insAttMap);
            //            let insAttMapJsonObj = JSON.parse(insAttMapJson);
            //            this.modelView.setInstanceAttributeStr(insAttMapJsonObj);
            //        }
            //        //if (this.usingTouchHandlerType === M3D.TouchHandlerType.HANDLER_WALKTHROUGH) {
            //        //    this.modelView.setProperty("isWalkThrough", "true");
            //        //    this.modelView.setProperty("speed", this.getTouchHandler().firstPersionSpeed / 8 + "");
            //        //} else {
            //        //    this.modelView.setProperty("isWalkThrough", "false");
            //        //}
            //        //DOTO 暂时写死 等观察模式写完加上
            //        this.modelView.setProperty("observeMode", "0");
            //        //this.modelView.setProperty("observeMode", this.commonTouchHandler.freeViewMode ? "0" : (this.commonTouchHandler.oribitMode ? "1" : "2"));
            //        let verticalAxis = M3D.Config.Parameters.getInstance().getNumberParameter(M3D.Config.verticalAxis);
            //        if (!verticalAxis) {
            //            verticalAxis = 0;
            //        }
            //        this.modelView.setProperty("upDirection", "0 " + verticalAxis.toString() + " 0");
            //    //}
            //    this.getSectionPlaneIdsRelationModelView();
            //    this.getPMIIdsRelationModelView();
            //    //this.getNoteIdsRelationModelView();
            //    this.getAnnotationIdsRelationModelView();
            //    this.getModelBoardIdsRelationModelView();
            //    return ret;
            //}
            updateViewExtentInfo() {
                //PMI
                let pmiIDs = [];
                let pmis = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.PMI);
                if (pmis.length > 0) {
                    for (let pmi of pmis) {
                        if (pmi && pmi.isVisible()) {
                            pmiIDs.push(pmi.getObjectId());
                        }
                    }
                }
                this.modelView.setPMIIds(pmiIDs);
                //文本批注
                this.modelView.clearNoteDataList();
                let noteIds = [];
                let annotations = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.ANNOTATION_NOTE);
                if (annotations.length > 0) {
                    for (let annotation of annotations) {
                        if (annotation && annotation.isVisible()) {
                            noteIds.push(annotation.getObjectId());
                        }
                    }
                }
                //测量批注
                let measures = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MEASURE_BASE);
                if (measures.length > 0) {
                    for (let measure of measures) {
                        if (measure && measure.isVisible()) {
                            noteIds.push(measure.getObjectId());
                        }
                    }
                }
                this.modelView.setNoteIds(noteIds);
                //ScreenUI
                //看板
                let modelBoardIds = [];
                let modelBoards = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODELBOARD);
                if (measures.length > 0) {
                    for (let modelBoard of modelBoards) {
                        if (modelBoard && modelBoard.isVisible()) {
                            modelBoardIds.push(modelBoard.getObjectId());
                        }
                    }
                }
                // this.modelView.setModelBoardIds(modelBoardIds);
                return true;
            }
            /**
             * @private
             *
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof ModelViewAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             *
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof ModelViewAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.ModelViewAction = ModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 移动类型
         * @export
         * @enum {number}
         */
        let MoveStyle;
        (function (MoveStyle) {
            // 平移
            MoveStyle[MoveStyle["MoveStyleTranslation"] = 0] = "MoveStyleTranslation";
            // 旋转
            MoveStyle[MoveStyle["MoveStyleRotate"] = 1] = "MoveStyleRotate";
            // 缩放
            MoveStyle[MoveStyle["MoveStyleScale"] = 2] = "MoveStyleScale";
        })(MoveStyle = Actions.MoveStyle || (Actions.MoveStyle = {}));
        /**
         * @memberof SView.Actions
         * @description 移动方向
         * @export
         * @enum {number}
         */
        let MoveDirection;
        (function (MoveDirection) {
            // 平面
            MoveDirection[MoveDirection["MoveDirectionPlane"] = 0] = "MoveDirectionPlane";
            MoveDirection[MoveDirection["MoveDirectionX"] = 1] = "MoveDirectionX";
            MoveDirection[MoveDirection["MoveDirectionY"] = 2] = "MoveDirectionY";
            MoveDirection[MoveDirection["MoveDirectionZ"] = 3] = "MoveDirectionZ";
        })(MoveDirection = Actions.MoveDirection || (Actions.MoveDirection = {}));
        /**
         * @memberof SView.Actions
         * @description 自由移动Action
         * @export
         * @class MoveTypeAction
         * @extends {M3D.Actions.Action}
         */
        class MoveTypeAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of MoveTypeAction.
             * @memberof MoveTypeAction
             */
            constructor() {
                super();
                this.scale = 0; // 屏幕倍率
                this.scale = 1;
                this.moveStyle = MoveStyle.MoveStyleTranslation;
                this.moveDirection = MoveDirection.MoveDirectionPlane;
            }
            /**
             * @public
             * @description 获取移动类型
             * @type {SView.Actions.MoveStyle}
             * @memberof MoveTypeAction
             */
            get moveStyle() {
                return this._moveStyle;
            }
            /**
             * @public
             * @description 设置移动类型
             * @memberof MoveTypeAction
             */
            set moveStyle(value) {
                this._moveStyle = value;
            }
            /**
             * @public
             * @description 获取移动方向
             * @type {SView.Actions.MoveDirection}
             * @memberof MoveTypeAction
             */
            get moveDirection() {
                return this._moveDirection;
            }
            /**
             * @public
             * @description 设置移动方向
             * @memberof MoveTypeAction
             */
            set moveDirection(value) {
                this._moveDirection = value;
            }
            /**
             * @public
             * @description 获取场景对象
             * @type {M3D.Scene}
             * @memberof MoveTypeAction
             */
            get scene() {
                return this._scene;
            }
            /**
             * @public
             * @description 设置场景对象
             * @memberof MoveTypeAction
             */
            set scene(value) {
                this._scene = value;
            }
            /**
             * @beta
             * @description 执行Action
             * @return {M3D.Actions.ActionState}
             * @memberof MoveTypeAction
             */
            execute() {
                return this.onExecute();
            }
            executeAction(...args) {
                if (args.length != 2)
                    return;
                let style = args[0];
                let value = args[1];
                if (value == undefined)
                    return;
                let shapes = this.scene.getSelector().getAll();
                let models = [];
                for (let shape of shapes) {
                    if (shape instanceof M3D.Shapes.Model) {
                        models.push(shape);
                    }
                }
                switch (style) {
                    case MoveStyle.MoveStyleTranslation:
                        this.executeModelTranslation(models, value);
                        break;
                    case MoveStyle.MoveStyleRotate:
                        this.executeModeRotate(models, value);
                        break;
                    case MoveStyle.MoveStyleScale:
                        this.executeScale(models, value);
                }
                this.scene.refresh();
            }
            /**
             * @beta
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MoveTypeAction
             */
            onExecute() {
                return M3D.Actions.ActionState.Action_Success;
            }
            ;
            /**
             * @public
             * @description 设置外部事件输入类型
             * @param {M3D.Inputs.InputHandlerType} type
             * @memberof MoveTypeAction
             */
            setHandlerType(type) {
                if (this.scene != null) {
                    let allViewport = this.scene.getViewPorts().getAllViewport();
                    for (let element of allViewport) {
                        let viewprot = Array.from(element.values())[0];
                        viewprot.getInput().inputHandlerType = type;
                    }
                }
            }
            /**
             * @description 执行选中模型平移
             * @private
             * @param {M3D.Shapes.Model[]} models
             * @param {M3D.M3DMath.Vector3} move
             * @memberof MoveTypeAction
             */
            executeModelTranslation(models, move) {
                if (move.IsZero())
                    return;
                for (let model of models) {
                    M3D.Shapes.ShapeHelper.translateModel(model, move);
                    this.updateModelDraw(model);
                }
            }
            /**
             * @description 执行模型旋转
             * @private
             * @param {M3D.Shapes.Model[]} models
             * @param {M3D.M3DMath.Quaternion} quat
             * @memberof MoveTypeAction
             */
            executeModeRotate(models, quat) {
                if (quat.IsZero())
                    return;
                for (let model of models) {
                    M3D.Shapes.ShapeHelper.rotateModel(model, quat);
                    this.updateModelDraw(model);
                }
            }
            /**
             * @description 执行缩放
             * @private
             * @param {M3D.Shapes.Model[]} models
             * @param {M3D.M3DMath.Vector3} scale
             * @memberof MoveTypeAction
             */
            executeScale(models, scale) {
                if (scale.IsZero())
                    return;
                for (let model of models) {
                    M3D.Shapes.ShapeHelper.scaleModel(model, scale);
                    this.updateModelDraw(model);
                }
            }
            /**
             * @description 标记模型并清除包围盒
             * @private
             * @param {M3D.Shapes.Model} model
             * @memberof MoveTypeAction
             */
            updateModelDraw(model) {
                model.getWorldBoundingBox().Clear();
                model.markDirty();
            }
        }
        Actions.MoveTypeAction = MoveTypeAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 打开模型Action
         * @export
         * @class OpenFileAction
         * @extends {M3D.Actions.Action}
         */
        class OpenFileAction extends M3D.Actions.Action {
            /**
             * @public
             * @description 获取文件名称
             * @return {string}
             * @memberof OpenFileAction
             */
            getFileName() {
                return this.fileName;
            }
            /**
             * @public
             * @description 设置文件名
             * @param {string} value
             * @memberof OpenFileAction
             */
            setFileName(value) {
                this.fileName = value;
            }
            /**
             * @public
             * @description 设置S3DView对象
             * @param {S3DView} value
             * @memberof OpenFileAction
             */
            setS3DView(value) {
                this.view = value;
            }
            /**
             * @protected
            * @description 执行
            * @returns {M3D.Actions.ActionState}
            * @memberof OpenFileAction
            * */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.AddFileAction.name, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (!this.view) {
                    retState = M3D.Actions.ActionState.Action_Error;
                }
                else {
                    this.clearData();
                    retState = this.view.open(this.fileName);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @public
             * @description 清空数据
             * @memberof OpenFileAction
             */
            clearData() {
                //清空选择集数据
                this.view.getShapeSetManager().clear();
                this.view.getAnimationPlayer().clearData();
                this.view.getAnimationPlayer().close();
                M3D.Animation.SimulationAnimationManager.getInstance().deleteAllSimAni();
                M3D.Animation.SimulationAnimationManager.getInstance().deleteAllChildSAManager();
                //清空M3D
                this.view.getScene().clear();
            }
        }
        Actions.OpenFileAction = OpenFileAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 手绘批注类型
         * @export
         * @enum {number}
         */
        let DrawType;
        (function (DrawType) {
            // 线 0（手绘）
            DrawType[DrawType["Line"] = 0] = "Line";
            // 圆 1
            DrawType[DrawType["Circle"] = 1] = "Circle";
            // 矩形 2
            DrawType[DrawType["Rectangle"] = 2] = "Rectangle";
            // 三角形 3
            DrawType[DrawType["Triangle"] = 3] = "Triangle";
            // 线 100 （画笔 ）
            DrawType[DrawType["Lines"] = 100] = "Lines";
        })(DrawType = Actions.DrawType || (Actions.DrawType = {}));
        /**
         * @memberof SView.Actions
         * @description 执行手绘Action
         * @export
         * @class PaintBrushAction
         * @extends {M3D.Actions.Action}
         */
        class PaintBrushAction extends M3D.Actions.Action {
            constructor(...args) {
                super();
                // 绘制类型
                this.types = [];
                // 绘制颜色
                this.colors = [];
                this.points = [];
                this.gesturesNote = null;
                if (args.length == 1) {
                    this.scene = args[0];
                }
                if (args.length == 5) {
                    this.types = args[0];
                    this.colors = args[1];
                    this.points = args[2];
                    this.scene = args[3];
                }
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof PaintBrushAction
             */
            onExecute() {
                if (this.types.length <= 0 ||
                    this.colors.length <= 0 ||
                    this.points.length <= 0) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let note = this.createThreeDGesturesNote(this.points, this.colors);
                if (note == null) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                note.setVisible(false, true); //设置手绘原始显隐状态为隐藏
                this.scene.getShapes().addShape(note);
                note.setVisible(true, false); //设置手绘当前状态为显示
                this.gesturesNote = note;
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @public
             * @description 设置绘制类型
             * @param {SView.Actions.DrawType[]} types
             * @memberof PaintBrushAction
             */
            setTypes(types) {
                this.types = types;
            }
            /**
             * @public
             * @description 设置绘制颜色
             * @param {M3D.M3DMath.Color[]} colors
             * @memberof PaintBrushAction
             */
            setColors(colors) {
                this.colors = colors;
            }
            /**
             * @public
             * @description 设置绘制的点集
             * @param {M3D.M3DMath.Vector2[][]} points
             * @memberof PaintBrushAction
             */
            setPoints(points) {
                this.points = points;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene 场景对象
             * @memberof PaintBrushAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置参数
             * @param {DrawType[]} drawTypes 绘制类型数组
             * @param {M3D.M3DMath.Color[]} colors 绘制颜色数组
             * @param {M3D.M3DMath.Vector2[][]} points 绘制点集
             * @memberof PaintBrushAction
             */
            setParameter(drawTypes, colors, points) {
                this.types = drawTypes;
                this.colors = colors;
                this.points = points;
            }
            /**
             * @description 创建底层手绘批注对象
             * @private
             * @param {M3D.M3DMath.Vector2[][]} pointArrayList
             * @param {M3D.M3DMath.Color[]} colorList
             * @return {M3D.Shapes.ThreeDGesturesNote}
             * @memberof PaintBrushAction
             */
            createThreeDGesturesNote(pointArrayList, colorList) {
                let gesturesNote = new M3D.Shapes.ThreeDGesturesNote();
                ;
                let allViewport = this.scene.getViewPorts().getAllViewport();
                let viewport = Array.from(allViewport[0].values())[0];
                let parameter = M3D.Config.Parameters.getInstance();
                //手势批注是否吸附在模型上
                let isThreeDNoteAdsorption = parameter.getParameter(M3D.Config.isThreeDNoteAdsorption);
                let camera = viewport.getCamera();
                let plan = new M3D.M3DMath.Plane(camera.getDirection(), camera.getPosition());
                for (let i = 0; i < pointArrayList.length; i++) {
                    let pointArray = pointArrayList[i];
                    let line = new M3D.Shapes.PolyLine();
                    let color = colorList[i];
                    line.setColor(color);
                    line.setLineWidth(1);
                    let nearPosition;
                    let tempDistance = 0;
                    let pointCount = pointArray.length;
                    if (pointCount < 2)
                        continue;
                    let pointInfo = [];
                    for (let j = 0; j < pointCount; j++) {
                        let insectPoint1 = new M3D.M3DMath.Vector3();
                        let tempScreenPnt = pointArray[j];
                        let intersect1 = viewport.getPickPoint(tempScreenPnt, insectPoint1, false);
                        if (isThreeDNoteAdsorption) {
                            pointInfo.push([intersect1, insectPoint1]);
                        }
                        else {
                            if (!intersect1)
                                continue;
                            let projectPnt = plan.Project(insectPoint1);
                            let distance = projectPnt.Sub(insectPoint1).Length();
                            if (j == 0) {
                                tempDistance = distance;
                            }
                            if (distance <= tempDistance) {
                                tempDistance = distance;
                                nearPosition = insectPoint1;
                            }
                        }
                    }
                    if (isThreeDNoteAdsorption) {
                        for (let j = 0; j < pointInfo.length; j++) {
                            let info = pointInfo[j];
                            let intersect1 = info[0];
                            if (intersect1) {
                                line.addPoint(info[1]);
                            }
                        }
                    }
                    else {
                        for (let j = 0; j < pointCount; j++) {
                            let temp = M3D.M3DMath.Vector2.ZERO();
                            let screenPnt = pointArray[j];
                            let insectPoint1 = new M3D.M3DMath.Vector3();
                            const rect = camera.getViewport().getRect();
                            if (!rect.Equals(M3D.M3DMath.Rect.ZERO())) {
                                temp = new M3D.M3DMath.Vector2((screenPnt.x - rect.min.x) / rect.Size().x, (screenPnt.y - rect.min.y) / rect.Size().y);
                            }
                            insectPoint1 = camera.screenToWorldPoint(temp, nearPosition);
                            line.addPoint(insectPoint1);
                        }
                    }
                    gesturesNote.addPolyLine(line);
                    gesturesNote.setOriginalProjectColors(color);
                    gesturesNote.setOriginalProjectWidths(1);
                    gesturesNote.setFrontShow(true);
                }
                return gesturesNote;
            }
        }
        Actions.PaintBrushAction = PaintBrushAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 自由配色Action
         * @export
         * @class RandomColorAction
         * @extends {M3D.Actions.Action}
         */
        class RandomColorAction extends M3D.Actions.Action {
            /**
             * @public
             * @description 设置执行自由配色的Model，与setScene二选一即可
             * @param {M3D.Shapes.Model} model
             * @memberof RandomColorAction
             */
            setModel(model) {
                this.model = model;
            }
            /**
             * @public
             * @description 设置场景对象，与setModel二选一即可
             * @param {M3D.Scene} scene
             * @memberof RandomColorAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @description 执行自由配色
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof RandomColorAction
             */
            onExecute() {
                if (this.model == null &&
                    this.scene == null) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                if (this.model == null) {
                    this.model = this.scene.getTopModel().getChildren()[0];
                    if (this.model == null) {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                this.setModelRandomColor(this.model);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 自由配色
             * @private
             * @param {M3D.Shapes.Model} model
             * @memberof RandomColorAction
             */
            setModelRandomColor(model) {
                if (model == null)
                    return;
                let children = model.getChildren();
                if (children == null || children.length == 0) {
                    model.setColor(this.randomColor());
                    return;
                }
                for (let child of children) {
                    this.setModelRandomColor(child);
                }
            }
            /**
             * @description 生成随机颜色
             * @private
             * @return {M3D.M3DMath.Color}
             * @memberof RandomColorAction
             */
            randomColor() {
                return new M3D.M3DMath.Color(Math.random(), Math.random(), Math.random());
            }
        }
        Actions.RandomColorAction = RandomColorAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 重命名视图Action
         * @export
         * @class RenameModelViewAction
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class RenameModelViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of RenameModelViewAction.
             * @memberof RenameModelViewAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof RenameModelViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置参数
             * @param {string} currentModelViewId 视图id
             * @param {string} currentModelViewServerId 在线视图id
             * @param {string} currentModelViewName 新的视图名称
             * @return {boolean}
             * @memberof RenameModelViewAction
             */
            setParameter(currentModelViewId, currentModelViewServerId, currentModelViewName) {
                if (!currentModelViewId || !currentModelViewServerId || currentModelViewServerId.length <= 0) {
                    return false;
                }
                this.currentModelViewId = currentModelViewId;
                this.currentModelViewServerId = currentModelViewServerId;
                this.currentModelViewName = currentModelViewName;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof RenameModelViewAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                //ActionLogger.ACTIONLOGD(M3D.START, ActionNames.DISTANCEMEASUREACTION, methodName);
                let that = this;
                if (!this.scene) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let retState = M3D.Actions.ActionState.Action_Success;
                let uniqueid = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.uniqueId);
                M3D.Utility.HttpMessageHandler.updateView(uniqueid, this.currentModelViewServerId, this.currentModelViewName, "", "0").then((response) => {
                    let jsonObject = JSON.parse(response);
                    let result = jsonObject["return"];
                    if (result !== M3D.Actions.ActionState.Action_Success) {
                        retState = M3D.Actions.ActionState.Action_Error;
                    }
                    let message = new SView.ModelViewActionMessage(SView.ActionMessageType.RenameModelView, retState.toString(), result.toString(), that.currentModelViewServerId, that.currentModelViewId, this.currentModelViewName);
                    that.send(message);
                }).catch((error) => {
                    //ActionLogger.ACTIONLOGD(M3D.END, ActionNames.DISTANCEMEASUREACTION, methodName);
                });
                //ActionLogger.ACTIONLOGD(M3D.END, ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof RenameModelViewAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof RenameModelViewAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.RenameModelViewAction = RenameModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 漫游Action
         * @export
         * @class RoamAction
         * @extends {M3D.Actions.Action}
         */
        class RoamAction extends M3D.Actions.Action {
            constructor(...args) {
                super();
                if (args.length == 1) {
                    if (args[0] instanceof M3D.Scene) {
                        let scene = args[0];
                        this.setScene(scene);
                        this.setViewport(scene.getCurrentViewPort());
                    }
                }
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof RoamAction
             */
            onExecute() {
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof RoamAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof RoamAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 设置视口对象
             * @param {M3D.Viewport} viewport
             * @memberof RoamAction
             */
            setViewport(viewport) {
                if (viewport != null) {
                    this.viewport = viewport;
                    //TODO: 看着好像有问题，和windows端不太一样
                    this.walkthroughInputHandler = this.viewport.getInput().getWorkingInputHandler();
                }
            }
            /**
             * @public
             * @description 获取视口对象
             * @return {M3D.Viewport}
             * @memberof RoamAction
             */
            getViewport() {
                return this.viewport;
            }
            /**
             * @public
             * @description 设置漫游模式下相机角度
             * @param {number} fov
             * @memberof RoamAction
             */
            setCameraFov(fov) {
                let camera = this.getCamera();
                if (camera != null) {
                    camera.setFov(fov);
                }
                if (this.viewport != null) {
                    this.viewport.getParameter().setNumberParameter(M3D.Config.fieldSize, fov);
                }
            }
            /**
             * @public
             * @description 获取漫游模式下相机角度
             * @return {number}
             * @memberof RoamAction
             */
            getCameraFov() {
                let fFov = 1.0;
                let camera = this.getCamera();
                if (camera != null) {
                    fFov = camera.getFov();
                }
                return fFov;
            }
            /**
             * @public
             * @description 设置向上方向
             * @param {M3D.M3DMath.Vector3} upDir
             * @memberof RoamAction
             */
            setUpDirection(upDir) {
                if (this.walkthroughInputHandler != null) {
                    //this.walkthroughInputHandler.setUpDirection(upDir);
                    this.getViewport().setUpDirection(upDir);
                }
            }
            /**
             * @public
             * @description 获取向上方向
             * @return {M3D.M3DMath.Vector3}
             * @memberof RoamAction
             */
            getUpDirection() {
                if (this.walkthroughInputHandler != null) {
                    return this.walkthroughInputHandler.getUpDirection();
                }
            }
            /**
             * @public
             * @description 前后移动
             * @param {number} distance
             * @memberof RoamAction
             */
            moveStraight(distance) {
                if (this.walkthroughInputHandler != null) {
                    this.walkthroughInputHandler.moveStraight(distance);
                }
            }
            /**
             * @public
             * @description 左右移动
             * @param {number} distance
             * @memberof RoamAction
             */
            moveSideways(distance) {
                if (this.walkthroughInputHandler != null) {
                    this.walkthroughInputHandler.MoveSideways(distance);
                }
            }
            /**
             * @public
             * @description 左右旋转
             * @param {number} degree
             * @memberof RoamAction
             */
            rotateSideways(degree) {
                if (this.walkthroughInputHandler != null) {
                    this.walkthroughInputHandler.RotateSideways(degree);
                }
            }
            /**
             * @public
             * @description 上下移动
             * @param {number} distance
             * @memberof RoamAction
             */
            moveUpAndDown(distance) {
                if (this.walkthroughInputHandler != null) {
                    this.walkthroughInputHandler.MoveUpAndDown(distance);
                }
            }
            /**
             * @public
             * @description 虚拟摇杆移动
             * @param {number} strSpeed
             * @param {number} sidSpeed
             * @memberof RoamAction
             */
            joystickMove(strSpeed, sidSpeed) {
                if (this.walkthroughInputHandler != null) {
                    this.walkthroughInputHandler.virtualKeyMove(strSpeed, sidSpeed);
                }
            }
            /**
             * @public
             * @description 获取相机对象
             * @return {M3D.Camera}
             * @memberof RoamAction
             */
            getCamera() {
                if (this.viewport != null) {
                    return this.viewport.getCamera();
                }
            }
        }
        Actions.RoamAction = RoamAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 选中模型Action
         * @export
         * @class SelectModelAction
         * @extends {M3D.Actions.Action}
         */
        class SelectModelAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of SelectModelAction.
             * @memberof SelectModelAction
             */
            constructor() {
                super();
                this.models = [];
            }
            /**
             * @public
             * @description 设置参数
             * @param {M3D.Scene} scene 场景对象
             * @param {(M3D.Shapes.Model | M3D.Shapes.Model[])} models 要选中的模型或模型数组
             * @param {boolean} selected 是否选中
             * @return {boolean}
             * @memberof SelectModelAction
             */
            setParameter(scene, models, selected) {
                if (!scene || !models) {
                    return false;
                }
                this.scene = scene;
                if (models instanceof M3D.Shapes.Model) {
                    this.models.push(models);
                }
                else {
                    this.models = models;
                }
                this.selected = selected;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof SelectModelAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                let shapeSet = this.scene.getShapes();
                let selector = this.scene.getSelector();
                if (shapeSet && selector) {
                    if (this.selected) {
                        let multiSelected = this.scene.getCurrentViewPort().getParameter().getBoolParameter(M3D.Config.isMultipleSelected);
                        if (!multiSelected) {
                            selector.clear();
                        }
                        selector.add(this.models);
                    }
                    else {
                        selector.remove(this.models);
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof SelectModelAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof SelectModelAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @beta
             * @memberof SelectModelAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof SelectModelAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.SelectModelAction = SelectModelAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 设置模型显隐Action
         * @export
         * @class ShowModelAction
         * @extends {M3D.Actions.Action}
         */
        class ShowModelAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of ShowModelAction.
             * @memberof ShowModelAction
             */
            constructor() {
                super();
                this.models = [];
            }
            /**
             * @public
             * @description 设置参数
             * @param {M3D.Scene} scene 场景对象
             * @param {(M3D.Shapes.Model | M3D.Shapes.Model[])} models 模型对象或者模型对象数组
             * @param {boolean} visible 是否显示
             * @return {boolean}
             * @memberof ShowModelAction
             */
            setParameter(scene, models, visible) {
                if (!scene || !models) {
                    return false;
                }
                this.scene = scene;
                if (models instanceof M3D.Shapes.Model) {
                    this.models.push(models);
                }
                else {
                    this.models = models;
                }
                this.visible = visible;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof ShowModelAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                this.arrModels = [];
                this.getArrModels(this.models);
                for (let model of this.models) {
                    if (!this.visible) {
                        model.setSelected(this.visible); // 取消选中
                    }
                }
                this.scene.setModelIsVisible(this.arrModels, this.visible);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @private
             * @description 获取模型下所有子件模型
             * @param {any} models
             * @memberof ShowModelAction
             */
            getArrModels(models) {
                for (let model of models) {
                    this.arrModels.push(model);
                    if (model.getChildren().length > 0) {
                        this.getArrModels(model.getChildren());
                    }
                }
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof ShowModelAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof ShowModelAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @beta
             * @memberof ShowModelAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof ShowModelAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.ShowModelAction = ShowModelAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 切换视图Action
         * @export
         * @class ShowModelViewAction
         * @extends {M3D.Actions.Action}
        */
        class ShowModelViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of ShowModelViewAction.
             * @memberof ShowModelViewAction
             */
            constructor() {
                super();
                // 数据差值个数
                this.interpolatorCount = 1000;
                // 模型位置差值数据
                /*
                 *  {"SVLPATH":[[位置差值数据],[旋转角度差值数据]]}
                 */
                this.updateModelMap = {};
                this.useAnimation = true; //是否使用动画播放
                this.isShowCamera = true;
                this.cameraAnimationTimer = 500; //相机切换动画时间
                this.modelExplosiveAnimationTimer = 500; //模型位置设置动画时间
                this.timerKeepMsecond = 30; //动画任务之间调用时间差
                this.animationTimer = null;
                this.midRotation = [];
                this.midScale = [];
                this.midTran = [];
                this.midFov = [];
                this.useAnimation = true;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof ShowModelViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置视图id
             * @param {number} id
             * @memberof ShowModelViewAction
             */
            setModelViewID(id) {
                this.modelViewId = id;
            }
            /**
             * @beta
             * @description 是否是首次加载
             * @param {boolean} loadFlag
             * @memberof ShowModelViewAction
             */
            setLoadFlag(loadFlag) {
                this.loadFlag = loadFlag;
                if (loadFlag) {
                    this.useAnimation = !loadFlag;
                }
                else {
                    this.useAnimation = true;
                }
            }
            /**
             * @public
             * @description 设置视图的objId
             * @param {number} id
             * @memberof ShowModelViewAction
             */
            setModelViewObjID(id) {
                this.modelViewObjId = id;
            }
            /**
             * @public
             * @description 设置视图对象
             * @param {M3D.Shapes.ModelView} modelView
             * @memberof ShowModelViewAction
             */
            setModelView(modelView) {
                this.modelView = modelView;
            }
            /**
             * @public
             * @description 设置是否使用动画
             * @param {boolean} useAnimation
             * @memberof ShowModelViewAction
             */
            setUseAnimation(useAnimation) {
                this.useAnimation = useAnimation;
            }
            /**
             * @protected
             * @description 执行Action
             * @return {M3D.Actions.ActionState}
             * @memberof ShowModelViewAction
             */
            onExecute() {
                let that = this;
                let modelViewList = that.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SHAPE_MODELVIEW);
                this.modelViews = new M3D.Reader.ModelViews();
                for (let i = 0; i < modelViewList.length; i++) {
                    let viewCurr = modelViewList[i];
                    if (this.modelViewObjId) { //存在objid 则优先obj匹配
                        let objId = viewCurr.getObjectId().toString();
                        if (that.modelViewObjId.toString() === objId) {
                            that.modelView = viewCurr;
                            break;
                        }
                    }
                    else {
                        let viewId = viewCurr.getId().toString();
                        if (that.modelViewId.toString() === viewId) {
                            that.modelView = viewCurr;
                            break;
                        }
                    }
                }
                if (!this.modelView) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                //视图内容未解析处理
                if (this.modelView.getProperty("requestInfo") === "false") {
                    this.scene.getLoader().getModelViewContent(this.modelView).then((ret) => {
                        if (ret) {
                            this.update();
                        }
                    });
                }
                else {
                    this.update();
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 根据视图更新视口
             * @private
             * @memberof ShowModelViewAction
             */
            update() {
                //this.retAnnotation = notes.getShape(this.objID, M3D.Shapes.ShapeType.ANNOTATION_NOTE)
                let upDataCameraState = this.modelView.getUpDataCameraState(); //记录原始视图更新相机状态
                if (this.useAnimation && !this.loadFlag) {
                    if (upDataCameraState) {
                        this.updateCameraStateUseAnimation();
                    }
                    if (this.modelView.getUpDataModelState()) {
                        this.updateModelStateAccordOrignalView();
                    }
                    if (!this.updateCameraState) {
                        if (Object.getOwnPropertyNames(this.updateModelMap).length > 0) {
                            this.startAnimation(this.interPolatorModel, this);
                        }
                        else {
                            let message = new SView.ShowModelViewActionMessage(SView.ActionMessageType.ShowModelView, M3D.Actions.ActionState.Action_Success.toString(), this.modelView);
                            this.send(message);
                        }
                    }
                }
                else {
                    if (this.loadFlag) { //如果是模型首次加载，则不更新相机
                        //this.modelView.setUpDataCamera(false);
                    }
                    else if (upDataCameraState) { //如果不是模型首次加载，则判断视图中是否更新视图状态
                        this.updateCameraState();
                    }
                    //先显示看板、批注、测量数据后再更新模型位置，否则会出现数据不跟随模型移动更新问题
                    if (this.modelView.getUpDataModelState()) {
                        this.updateModelStateAccordOrignalView();
                    }
                }
                //设置视图中的剖面为启用
                //let sectionPlaneList: SectionPlane[] = curRootModel.GetSectionPlaneList() ? curRootModel.GetSectionPlaneList() : [];
                //if (sectionPlaneList != undefined) {
                //    if (pView.GetProperty("SectionPlanes")) {
                //        let sections = pView.GetProperty("SectionPlanes");
                //        let hasSection = false;
                //        let isReverse = false;
                //        let selectedModelPath: [] = [];
                //        for (let i = 0; i < sections.length; i++) {
                //            let sectionObj = sections[i];
                //            let sectionPlane = SectionPlane.fromJson(sectionObj);
                //            isReverse = sectionPlane.GetReverseClip();
                //            hasSection = true;
                //            //指定剖
                //            selectedModelPath = sectionPlane.GetSelectedModelPath();
                //            this.GetModel().AddSectionPlane(sectionPlane);
                //            this.sceneManager.GetSection().AddPlane(sectionPlane);
                //        }
                //        if (selectedModelPath.length > 0) {
                //            this.GetSectionManager().SetSelectedModels(selectedModelPath);
                //        } else {
                //            this.GetSectionManager().UseSelected(false);
                //        }
                //        if (hasSection) {
                //            this.GetSceneManager().GetSection().SetIsReverseClipping(isReverse);
                //        }
                //    } else {
                //        let clipPlaneIdList: number[] = pView.GetSectionPlaneIDList();
                //        if (clipPlaneIdList.length > 0) {
                //            let hasSection = false;
                //            let isReverse = false;
                //            let selectedModelPath: [] = [];
                //            for (let i = 0; i < sectionPlaneList.length; i++) {
                //                let curPlane: SectionPlane = sectionPlaneList[i];
                //                for (let j = 0; j < clipPlaneIdList.length; j++) {
                //                    if (clipPlaneIdList[j] == curPlane.GetID()) {
                //                        //是否反向信息
                //                        isReverse = curPlane.GetReverseClip();
                //                        //指定剖
                //                        selectedModelPath = curPlane.GetSelectedModelPath();
                //                        curPlane.SetEnable(true);
                //                        this.sceneManager.GetSection().AddPlane(curPlane);
                //                        hasSection = true;
                //                        break;
                //                    }
                //                }
                //            }
                //            if (hasSection) {
                //                this.GetSceneManager().GetSection().SetIsReverseClipping(isReverse);
                //            }
                //            //指定剖
                //            if (selectedModelPath.length > 0) {
                //                this.GetSectionManager().SetSelectedModels(selectedModelPath);
                //            } else {
                //                this.GetSectionManager().UseSelected(false);
                //            }
                //        }
                //    }
                //}
                //显示关联的文本批注
                let annotations = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.ANNOTATION_NOTE);
                if (annotations.length > 0) {
                    let noteIDList = this.modelView.getNoteList();
                    for (let annotation of annotations) {
                        let target = annotation;
                        if (noteIDList.length > 0) {
                            //如果在当前视图的批注列表中找到该批注则显示，否则则隐藏
                            if ((noteIDList.indexOf(target.getObjectId()) != -1) || (noteIDList.indexOf(target.getObjectId()) != -1)) {
                                target.setVisible(true);
                            }
                            else {
                                target.setVisible(false);
                            }
                        }
                        else {
                            target.setVisible(false);
                        }
                    }
                }
                //显示关联的测量
                let measures = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MEASURE_BASE);
                if (measures.length > 0) {
                    let measureIDList = this.modelView.getMeasures();
                    for (let measure of measures) {
                        let target = measure;
                        if (measureIDList.length > 0) {
                            //如果在当前视图的批注列表中找到该批注则显示，否则则隐藏
                            if ((measureIDList.indexOf(target.getObjectId()) != -1) || (measureIDList.indexOf(target.getObjectId()) != -1)) {
                                target.setVisible(true);
                            }
                            else {
                                target.setVisible(false);
                            }
                        }
                        else {
                            target.setVisible(false);
                        }
                    }
                }
                //显示关联的手势批注
                let threeGestureNotes = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.THREED_GESTURE_NOTE);
                if (threeGestureNotes.length > 0) {
                    let measureIDList = this.modelView.getMeasures();
                    for (let note of threeGestureNotes) {
                        let target = note;
                        if (measureIDList.length > 0) {
                            //如果在当前视图的批注列表中找到该批注则显示，否则则隐藏
                            if ((measureIDList.indexOf(target.getObjectId()) != -1) || (measureIDList.indexOf(target.getId()) != -1)) {
                                target.setVisible(true);
                            }
                            else {
                                target.setVisible(false);
                            }
                        }
                        else {
                            target.setVisible(false);
                        }
                    }
                }
                //看板
                let pModeleBoardGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODELBOARD);
                if (pModeleBoardGroup) {
                    let iModelBoardCount = pModeleBoardGroup.length;
                    //隐藏所有看板
                    for (let i = 0; i < iModelBoardCount; i++) {
                        let pShapeNode = pModeleBoardGroup[i];
                        if (pShapeNode)
                            pShapeNode.setVisible(false);
                    }
                    let iModelBoardsIDCount = 0;
                    let modelBoardsIDList = this.modelView.getModelBoardsIDList();
                    iModelBoardsIDCount = modelBoardsIDList.length;
                    for (let i = 0; i < iModelBoardsIDCount; i++) {
                        let iModelBoardID = modelBoardsIDList[i];
                        for (let j = 0; j < pModeleBoardGroup.length; j++) {
                            let pShapeNode = (pModeleBoardGroup[j]);
                            if (!pShapeNode)
                                continue;
                            if (pShapeNode && ((iModelBoardID == pShapeNode.getObjectId()) /* || (iModelBoardID == pShapeNode.getId())*/)) {
                                pShapeNode.setVisible(true);
                                break;
                            }
                        }
                    }
                }
                //先显示看板、批注、测量数据后再更新模型位置，否则会出现数据不跟随模型移动更新问题
                //if (this.modelView.getUpDataModelState()) {
                //    this.updateModelStateAccordOrignalView();
                //}
                //显示关联剖切面
                let sectionPlanes = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SECTION_PLANE);
                let currentPlaneIdList = this.modelView.getSectionPlaneIDList();
                if (sectionPlanes.length > 0) {
                    let hasSection = false;
                    let isReverse = false;
                    let selectedModelPath = [];
                    for (let sectionPlane of sectionPlanes) {
                        let target = sectionPlane;
                        //激活视图中关联的剖切
                        if (currentPlaneIdList.length > 0) {
                            if (currentPlaneIdList.indexOf(sectionPlane.getObjectId()) != -1) {
                                target.setEnable(true);
                                target.setVisible(true);
                                if (typeof target.getOriShowCappingPlane() === "boolean") {
                                    target.setShowCappingPlane(target.getOriShowCappingPlane());
                                }
                                if (typeof target.getOriShowClipPlane() === "boolean") {
                                    target.setShowClipPlane(target.getOriShowClipPlane());
                                }
                                //是否反向信息
                                //isReverse = target.getReverseClip();
                                //hasSection = true;
                                //指定剖
                                //selectedModelPath = target.getSelectedModelPath();
                            }
                            else {
                                target.setEnable(false);
                            }
                        }
                        else {
                            target.setEnable(false);
                        }
                    }
                }
                let sections = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SECTION);
                if (sections.length > 0) {
                    for (let section of sections) {
                        let planes = section.getPlaneList();
                        for (let sectionPlane of planes) {
                            let target = sectionPlane;
                            //激活视图中关联的剖切
                            if (currentPlaneIdList.length > 0) {
                                if (currentPlaneIdList.indexOf(sectionPlane.getObjectId()) != -1) {
                                    target.setEnable(true);
                                    target.setVisible(true);
                                    if (typeof target.getOriShowCappingPlane() === "boolean") {
                                        target.setShowCappingPlane(target.getOriShowCappingPlane());
                                    }
                                    if (typeof target.getOriShowClipPlane() === "boolean") {
                                        target.setShowClipPlane(target.getOriShowClipPlane());
                                    }
                                    //target.setShowCappingPlane(target.getOriShowCappingPlane());
                                    //target.setShowClipPlane(target.getOriShowClipPlane());
                                    //是否反向信息
                                    //isReverse = target.getReverseClip();
                                    //hasSection = true;
                                    //指定剖
                                    //selectedModelPath = target.getSelectedModelPath();
                                }
                                else {
                                    target.setEnable(false);
                                }
                            }
                            else {
                                target.setEnable(false);
                            }
                        }
                    }
                }
                //显示关联PMI，隐藏所有,显示相关
                let pmimaps = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.PMI);
                if (pmimaps !== null && pmimaps !== undefined && pmimaps.length > 0) {
                    // 如果视图名为“DEFAULT”则显示所有PMI
                    if (this.modelView.getViewType() === 0) {
                        for (let pmi of pmimaps) {
                            if (pmi) {
                                pmi.setVisible(true);
                            }
                        }
                    }
                    else {
                        for (let pmi of pmimaps) {
                            if (pmi) {
                                pmi.setVisible(false);
                            }
                        }
                        //显示关联的PMI
                        for (let i = 0; i < this.modelView.getPMIList().length; i++) {
                            let pmiID = this.modelView.getPMIList()[i];
                            for (let pmi of pmimaps) {
                                if (pmi && (pmi.getPmiId() === Number(pmiID))) {
                                    pmi.setVisible(true);
                                }
                            }
                        }
                    }
                }
                //模型切换完成，重新设置视图原始切换相机状态
                //this.modelView.setUpDataCamera(upDataCameraState);
                if (!this.useAnimation) {
                    let message = new SView.ShowModelViewActionMessage(SView.ActionMessageType.ShowModelView, M3D.Actions.ActionState.Action_Success.toString(), this.modelView);
                    this.send(message);
                }
            }
            /**
             * @private
             * @description 更新相机信息
             * @memberof ShowModelViewAction
             */
            updateCameraState() {
                if (this.modelView === null) {
                    return;
                }
                let topModel = this.scene.getTopModel();
                let subModels = topModel.getChildren();
                if (subModels.length === 0) {
                    return;
                }
                //先获的相机字符串信息
                let camera = this.modelView.getCamera();
                if (!camera || camera === null || camera === undefined) { //不存在Camera 就解析
                    this.parseCamera();
                    camera = this.modelView.getCamera();
                }
                if (!camera) {
                    return;
                }
                let viewCamera = camera;
                let currentCamera = this.scene.getCurrentViewPort().getCamera();
                let newCamPos = viewCamera.getPosition();
                let newCamRotaion = viewCamera.getRotationQuaternion();
                let pModel = subModels.find(v => {
                    v.getProtoId() === this.modelView.getProtoId();
                });
                //子件视图的摄像机需要调整为总装配中的场景信息
                if (pModel && pModel != topModel) {
                    let modelWorldMatrix = topModel.getWorldTransform();
                    newCamPos = modelWorldMatrix.Multiply(newCamPos);
                    newCamRotaion = new M3D.M3DMath.Quaternion(modelWorldMatrix.ToMatrix3().Multiply(newCamRotaion.RotationMatrix()));
                }
                currentCamera.setOrthographic(camera.isOrthographic());
                currentCamera.setPosition(newCamPos);
                currentCamera.setWorldRotation(newCamRotaion);
                if (currentCamera.isOrthographic() && currentCamera.getOrthoSize() > 0 && camera.getOrthoSize()) {
                    currentCamera.setZoom(currentCamera.getOrthoSize() / camera.getOrthoSize() * viewCamera.getZoom());
                    //子件视图中如果场景大小为100，说明是默认值。要根据模型包围盒大小设置的摄像机缩放信息
                    if (pModel && pModel != topModel && viewCamera.getOrthoSize() == 100) {
                        let fModelScreenScale = 1.0;
                        let modelBox = topModel.getTotalWorldBoundingBox();
                        let iRootModelSize = modelBox.Length();
                        let worldTotalBoundingBox = pModel.getTotalWorldBoundingBox();
                        let iCurModelSize = worldTotalBoundingBox.Length();
                        if (iRootModelSize > 0 && iCurModelSize > 0) {
                            fModelScreenScale = iRootModelSize / iCurModelSize;
                            //自适应大小时，位置不能偏移，因此，要使模型包围中心显示在屏幕中心
                            let viewModelBox = pModel.getTotalWorldBoundingBox();
                            let viewMat = newCamRotaion.RotationMatrix();
                            let direction = new M3D.M3DMath.Vector3(viewMat.data[2], viewMat.data[5], viewMat.data[8]);
                            let newCamPos = viewModelBox.Center().Add(direction.Multiply(this.scene.getBoundingBox().Length()).Multiply(0.8));
                            currentCamera.setPosition(newCamPos);
                        }
                        currentCamera.setZoom(fModelScreenScale * viewCamera.getZoom());
                    }
                }
                else {
                    let defaultZoom = currentCamera.getDefaultZoom();
                    currentCamera.setZoom(defaultZoom);
                }
                //如果是平行投影，进行显示矫正
                if (viewCamera.isOrthographic()) {
                    let sceneBox = this.scene.getBoundingBox();
                    let center = sceneBox.Center();
                    let cameraPos = currentCamera.getPosition();
                    //如果摄像机在模型内部，则进行调整
                    if (sceneBox.IsInside(cameraPos) == M3D.M3DMath.Intersection.INSIDE) {
                        let viewMat = viewCamera.getRotationQuaternion().RotationMatrix();
                        let direction = new M3D.M3DMath.Vector3(viewMat.data[2], viewMat.data[5], viewMat.data[8]);
                        let newPos = cameraPos.Add(direction.Multiply(sceneBox.Length()).Multiply(0.8));
                        currentCamera.setWorldPosition(newPos);
                    }
                    else {
                        //如果修改的零件的位置或显示隐藏状态，此调整方法正确，不进行调整
                        if (!this.modelView.getUpDataModelState()) {
                            //如果超出视景体，进行调整，支持老版本视图
                            let viewMat = viewCamera.getRotationQuaternion().RotationMatrix();
                            let direction = new M3D.M3DMath.Vector3(viewMat.data[2], viewMat.data[5], viewMat.data[8]);
                            let centerPlane = new M3D.M3DMath.Plane(direction, center);
                            let targetPnt = centerPlane.Project(newCamPos);
                            let fCurInitWind = currentCamera.getOrthoSize();
                            let fHalfViewSize = viewCamera.getOrthoSize() * 0.5;
                            if (viewCamera.getZoom() < 1.0) {
                                fHalfViewSize = fHalfViewSize / viewCamera.getZoom();
                            }
                            let fScreenWidthHeightRat = fCurInitWind[0] / fCurInitWind[1];
                            if (fScreenWidthHeightRat >= 1.0) {
                                fHalfViewSize = fHalfViewSize * fScreenWidthHeightRat;
                            }
                            else {
                                fHalfViewSize = fHalfViewSize / fScreenWidthHeightRat;
                            }
                            let viewMin = new M3D.M3DMath.Vector3(center.x - fHalfViewSize, center.y - fHalfViewSize, center.z - fHalfViewSize);
                            let viewMax = new M3D.M3DMath.Vector3(center.x + fHalfViewSize, center.y + fHalfViewSize, center.z + fHalfViewSize);
                            let viewBox = new M3D.M3DMath.BoundingBox(viewMin, viewMax);
                            if (viewBox.IsInside(targetPnt) != M3D.M3DMath.Intersection.INSIDE) {
                                newCamPos = sceneBox.Center().Add(direction.Multiply(sceneBox.Length()).Multiply(0.8));
                                currentCamera.setWorldPosition(newCamPos);
                            }
                        }
                    }
                }
            }
            /**
             * @beta
             * @description 使用动画更新相机
             * @memberof ShowModelViewAction
             */
            updateCameraStateUseAnimation() {
                let camera = this.modelView.getCamera();
                if (!camera) {
                    this.parseCamera();
                }
                camera = this.modelView.getCamera();
                if (camera) {
                    this.interpolatCamera(camera);
                }
                this.startAnimation(this.interpolatorCamera, this);
            }
            /**
             * @private
             * @description 更新模型状态
             * @return {*}
             * @memberof ShowModelViewAction
             */
            updateModelStateAccordOrignalView() {
                let isModelAnni = this.useAnimation; //是否更新动画  DOTO先设置为false
                let strTrochoidName = "ModelViewTrochoid";
                let allSubModels = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODEL);
                if (!allSubModels) {
                    //数据错误 return
                    return;
                }
                let insAttrMap = this.modelView.getInstanceAttributeMap();
                if (insAttrMap.size <= 1) { //不存在，则新生成                    
                    let instanceAttributeStrNew = this.modelView.getInstanceAttributeStr();
                    if (instanceAttributeStrNew) {
                        let instanceAttributeDataNew = JSON.parse(JSON.stringify(instanceAttributeStrNew));
                        if (instanceAttributeDataNew) {
                            this.modelView = this.parseInstance(instanceAttributeDataNew, this.modelView);
                            insAttrMap = this.modelView.getInstanceAttributeMap();
                        }
                    }
                }
                for (let i = 0; i < allSubModels.length; i++) {
                    let curModel = allSubModels[i];
                    if (curModel) {
                        let newIns = new M3D.Shapes.InstanceAttribute();
                        newIns.path = curModel.getPlcPath();
                        let curInsAtt = insAttrMap.get(newIns.path);
                        if (curInsAtt) {
                            if (curInsAtt.visible !== undefined && curInsAtt !== null) {
                                newIns.visible = curInsAtt.visible;
                            }
                            else {
                                newIns.visible = curModel.isVisible(true);
                            }
                            if (curInsAtt.hasColor) {
                                newIns.hasColor = true;
                                newIns.insColor = curInsAtt.insColor;
                            }
                            else {
                                if (curModel.getOriginalColor()) {
                                    newIns.hasColor = true;
                                    newIns.insColor = curModel.getOriginalColor();
                                }
                                else {
                                    newIns.hasColor = false;
                                }
                            }
                            if (curInsAtt.placeMatrix && !curInsAtt.placeMatrix.Equals(M3D.M3DMath.Matrix4.IDENTITY())) {
                                newIns.placeMatrix = curInsAtt.placeMatrix;
                            }
                            else {
                                newIns.placeMatrix = curModel.getOriginalMatrix();
                            }
                        }
                        else {
                            newIns.visible = curModel.isVisible(true);
                            if (curModel.getOriginalColor()) {
                                newIns.hasColor = true;
                                newIns.insColor = curModel.getOriginalColor();
                            }
                            else {
                                newIns.hasColor = false;
                            }
                            newIns.placeMatrix = curModel.getOriginalMatrix();
                        }
                        //显隐变化
                        curModel.setVisible(newIns.visible);
                        //视图颜色变化
                        if (newIns.hasColor) {
                            if (newIns.insColor && newIns.insColor.r >= 0 && newIns.insColor.g >= 0 && newIns.insColor.b >= 0) {
                                let tmpColor = newIns.insColor;
                                let fAlpha = newIns.insColor.a;
                                let pCurColor = curModel.getColor();
                                if (pCurColor && curModel.getColor().equals(tmpColor) && curModel.getAlpha() === fAlpha) {
                                    //continue;
                                }
                                else {
                                    curModel.setColor(tmpColor, false);
                                }
                            }
                            else if (newIns.insColor.a >= 0) {
                                let tempColor = newIns.insColor;
                                curModel.setColor(tempColor, false);
                                //curModel.UpdataRenderMaterial();
                            }
                            else {
                                if (curModel.getParentModel() && curModel.getParentModel().hasInstanceMaterial()) {
                                    curModel.setMaterial(curModel.getParentModel().getMaterial(), false);
                                }
                                else {
                                    curModel.setMaterial(null, false);
                                }
                            }
                        }
                        else {
                            if (curModel.getParentModel() && curModel.getParentModel().hasInstanceMaterial()) {
                                curModel.setMaterial(curModel.getParentModel().getMaterial(), false);
                            }
                            else {
                                curModel.setMaterial(null, false);
                            }
                        }
                        //判断矩阵信息是否正确，若全部为0则不设置
                        if (newIns.placeMatrix && !newIns.placeMatrix.Equals(M3D.M3DMath.Matrix4.ZERO())) {
                            // 判断模型当前矩阵信息和视图内矩阵信息是否相同
                            if (!newIns.placeMatrix.Equals(curModel.getPlaceMatrix())) {
                                //console.log(i);
                                // 模型位置更新是否需要动画
                                if (isModelAnni) {
                                    // 差值计算
                                    let m_position = new M3D.M3DMath.Vector3();
                                    let m_rotation = new M3D.M3DMath.Quaternion();
                                    let m_scale = new M3D.M3DMath.Vector3();
                                    // 获取模型当前矩阵信息
                                    curModel.getPlaceMatrix().Decompose(m_position, m_rotation, m_scale);
                                    let newPosition = new M3D.M3DMath.Vector3();
                                    let newRotation = new M3D.M3DMath.Quaternion();
                                    let newScale = new M3D.M3DMath.Vector3();
                                    // 获取新的矩阵信息
                                    newIns.placeMatrix.Decompose(newPosition, newRotation, newScale);
                                    let tran = [];
                                    let rotate = [];
                                    let scale = [];
                                    // 计算差值 DOTO
                                    M3D.Utility.MathVector.VecSlerp(m_position, newPosition, this.interpolatorCount, tran);
                                    M3D.Utility.MathVector.RotationSlerp(m_rotation, newRotation, this.interpolatorCount, rotate);
                                    M3D.Utility.MathVector.VecSlerp(m_scale, newScale, this.interpolatorCount, scale);
                                    // 存储差值
                                    this.updateModelMap[newIns.path] = [tran, rotate, scale];
                                    //获取视图动画轨迹的起始点和终点
                                    let newLineEndPosition = new M3D.M3DMath.Vector3();
                                    let oldLineStartPosition = new M3D.M3DMath.Vector3();
                                    if (curModel.getParentModel() && curModel.getParentModel().getParentModel()) {
                                        let worldTrasform = curModel.getParentModel().getWorldTransform();
                                        newLineEndPosition = worldTrasform.Multiply(newPosition);
                                        oldLineStartPosition = worldTrasform.Multiply(m_position);
                                    }
                                    else {
                                        newLineEndPosition = newPosition;
                                        oldLineStartPosition = m_position;
                                    }
                                    if (M3D.Config.Parameters.getInstance().isShowAniTrochoid) {
                                        M3D.Animation.AnimationHelper.addTrochoid([oldLineStartPosition, newLineEndPosition], strTrochoidName, this.scene);
                                    }
                                }
                                else {
                                    //直接设置新的矩阵
                                    curModel.setPlaceMatrix(newIns.placeMatrix);
                                }
                            }
                        }
                    }
                    else {
                        //M3D.Logger.M3DLOGW(M3D.Logger.MOUDLE_Default, M3D.Logger.THREAD_Default, "don`t find model");
                    }
                }
            }
            /**
             * @private
             * @description 解析实例数据
             * @param {{}} instanceAttributeData
             * @param {M3D.Shapes.ModelView} modelView
             * @return {M3D.Shapes.ModelView}
             * @memberof ShowModelViewAction
             */
            parseInstance(instanceAttributeData, modelView) {
                if (instanceAttributeData.visible) { //判断是否是instances还是insAttributes
                    let instances = instanceAttributeData;
                    let visible = instances.visible;
                    if (visible) {
                        for (let i = 0; i < visible.length; i++) {
                            let path = M3D.Utility.PathHelper.M3D_PATH + visible[i];
                            path = M3D.Utility.PathHelper.SVLPathToM3D(path);
                            let shape = this.scene.getShapes().getModelBySVLPath(path);
                            if (shape) {
                                let instanceAttribute = new M3D.Shapes.InstanceAttribute();
                                instanceAttribute.path = path;
                                instanceAttribute.visible = true;
                                instanceAttribute.hasVisble = true;
                                modelView.getInstanceAttributeMap().set(path, instanceAttribute);
                            }
                        }
                    }
                    let unvisible = instances.unvisible;
                    if (unvisible) {
                        for (let i = 0; i < unvisible.length; i++) {
                            let path = M3D.Utility.PathHelper.M3D_PATH + unvisible[i];
                            path = M3D.Utility.PathHelper.SVLPathToM3D(path);
                            let shape = this.scene.getShapes().getModelBySVLPath(path);
                            if (shape) {
                                let instanceAttribute = new M3D.Shapes.InstanceAttribute();
                                instanceAttribute.path = path;
                                instanceAttribute.visible = false;
                                instanceAttribute.hasVisble = true;
                                modelView.getInstanceAttributeMap().set(path, instanceAttribute);
                            }
                        }
                    }
                    let instanceColor = instances.color;
                    if (instanceColor) {
                        let path = instanceColor.path;
                        let colors = instanceColor.colors;
                        if (path && colors && path.length * 4 === colors.length) {
                            for (let i = 0; i < path.length; i++) {
                                let instancePath = M3D.Utility.PathHelper.M3D_PATH + path[i];
                                instancePath = M3D.Utility.PathHelper.SVLPathToM3D(instancePath);
                                let shape = this.scene.getShapes().getModelBySVLPath(instancePath);
                                //let shape: M3D.Shapes.Model = <M3D.Shapes.Model>view.GetShapeBySVLPath(instancePath);
                                if (shape) {
                                    let instanceAttribute = modelView.getInstanceAttributeMap().get(instancePath);
                                    if (!instanceAttribute) {
                                        instanceAttribute = new M3D.Shapes.InstanceAttribute();
                                        //this.modelView.getInstanceAttributeMap().set(shape.getInstanceId(), instanceAttribute);
                                        modelView.getInstanceAttributeMap().set(instancePath, instanceAttribute);
                                        instanceAttribute.path = instancePath;
                                    }
                                    let color = new M3D.M3DMath.Color(colors[i * 4], colors[i * 4 + 1], colors[i * 4 + 2], colors[i * 4 + 3]);
                                    instanceAttribute.insColor = color;
                                    instanceAttribute.hasColor = true;
                                }
                            }
                        }
                    }
                    let instanceMatrix = instances.matrix;
                    if (instanceMatrix) {
                        let path = instanceMatrix.path;
                        let matrices = instanceMatrix.matrices;
                        if (path && matrices && path.length * 16 === matrices.length) {
                            for (let i = 0; i < path.length; i++) {
                                let instancePath = M3D.Utility.PathHelper.M3D_PATH + path[i];
                                instancePath = M3D.Utility.PathHelper.SVLPathToM3D(instancePath);
                                let shape = this.scene.getShapes().getModelBySVLPath(instancePath);
                                //let shape: M3D.Shapes.Model = <M3D.Shapes.Model>view.GetShapeBySVLPath(instancePath);
                                if (shape) {
                                    let instanceAttribute = modelView.getInstanceAttributeMap().get(instancePath);
                                    if (!instanceAttribute) {
                                        instanceAttribute = new M3D.Shapes.InstanceAttribute();
                                        //this.modelView.getInstanceAttributeMap().set(shape.getInstanceId(), instanceAttribute);
                                        modelView.getInstanceAttributeMap().set(instancePath, instanceAttribute);
                                        instanceAttribute.path = instancePath;
                                    }
                                    let matrix = new M3D.M3DMath.Matrix4(matrices[i * 16], matrices[i * 16 + 1], matrices[i * 16 + 2], matrices[i * 16 + 3], matrices[i * 16 + 4], matrices[i * 16 + 5], matrices[i * 16 + 6], matrices[i * 16 + 7], matrices[i * 16 + 8], matrices[i * 16 + 9], matrices[i * 16 + 10], matrices[i * 16 + 11], matrices[i * 16 + 12], matrices[i * 16 + 13], matrices[i * 16 + 14], matrices[i * 16 + 15]);
                                    instanceAttribute.placeMatrix = matrix;
                                    instanceAttribute.hasMatrix = true;
                                }
                            }
                        }
                    }
                    //this.modelView.setUpDataModel(true);
                }
                else {
                    for (let insAttKey in instanceAttributeData) {
                        let insAttInfo = instanceAttributeData[insAttKey];
                        let matixStr = insAttInfo.matix;
                        let insAtt = new M3D.Shapes.InstanceAttribute();
                        insAtt.id = insAttInfo.plcPath;
                        let visiable = insAttInfo.visible;
                        insAtt.visible = visiable === 0 ? true : false;
                        insAtt.hasVisble = true;
                        insAtt.path = M3D.Utility.PathHelper.SVLPathDecToHex(insAttKey);
                        if (matixStr) {
                            let matrix;
                            let strSeparator = " ";
                            let strAry = matixStr.split(strSeparator);
                            if (strAry.length == 16) {
                                matrix = new M3D.M3DMath.Matrix4();
                            }
                            else if (strAry.length == 12) {
                                matrix = new M3D.M3DMath.Matrix3x4();
                            }
                            matrix.fromString(matixStr);
                            if (matrix.Equals(M3D.M3DMath.Matrix4.ZERO().Clone())) {
                                continue;
                            }
                            insAtt.placeMatrix = matrix;
                            insAtt.hasMatrix = true;
                        }
                        let colorStr = insAttInfo.color;
                        if (colorStr) {
                            let colorArray = colorStr.split(" ");
                            insAtt.hasColor = true;
                            let color = new M3D.M3DMath.Color(Number(colorArray[0]), Number(colorArray[1]), Number(colorArray[2]), Number(colorArray[3]));
                            insAtt.insColor = color.Clone();
                        }
                        //this.modelView.getInstanceAttributeMap().set(Number(insAttInfo.plcPath), insAtt);
                        modelView.getInstanceAttributeMap().set(insAtt.path, insAtt);
                    }
                }
                return modelView;
            }
            parseCamera() {
                let camera = null;
                let cameraStr = this.modelView.getCameraStr();
                if (cameraStr) {
                    let cameraData = JSON.parse(JSON.stringify(cameraStr));
                    //let camera;
                    if (cameraData) {
                        camera = new M3D.Camera();
                        let pos;
                        let target;
                        let up;
                        camera.setOrthographic(cameraData.projectType == "1" ? true : false); // 投影类型
                        if (cameraData.angle) { //相机视角
                            let angle = cameraData.angle;
                            camera.setFov(angle);
                        }
                        if (cameraData.origin) {
                            pos = new M3D.M3DMath.Vector3(cameraData.origin.split(" ")[0], cameraData.origin.split(" ")[1], cameraData.origin.split(" ")[2]);
                        }
                        if (cameraData.targetVector) {
                            target = new M3D.M3DMath.Vector3(cameraData.targetVector.split(" ")[0], cameraData.targetVector.split(" ")[1], cameraData.targetVector.split(" ")[2]);
                        }
                        if (cameraData.upVector) {
                            up = new M3D.M3DMath.Vector3(cameraData.upVector.split(" ")[0], cameraData.upVector.split(" ")[1], cameraData.upVector.split(" ")[2]);
                        }
                        if (cameraData.matix) { //矩阵
                            let matix = new M3D.M3DMath.Matrix4();
                            matix.fromString(cameraData.matix);
                            let rotation = new M3D.M3DMath.Quaternion();
                            let matrix3 = new M3D.M3DMath.Matrix3(matix.data[0], matix.data[4], matix.data[8], matix.data[1], matix.data[5], matix.data[9], matix.data[2], matix.data[6], matix.data[10]);
                            rotation.FromRotationMatrix(matrix3);
                            camera.setPosition(new M3D.M3DMath.Vector3(matix.data[12], matix.data[13], matix.data[14]));
                            camera.setRotation(rotation);
                        }
                        else {
                            let rotation = new M3D.M3DMath.Quaternion();
                            if ((target.x != -1.0 || target.y != -1.0 || target.z != -1.0) &&
                                (up.x != -1.0 || up.y != -1.0 || up.z != -1.0)) {
                                //构造出旋转,参考李坤的算法,貌似和已有的通过direct和up构造的方法不一致.
                                let x = target.CrossProduct(up);
                                let y = up;
                                let z = target.NagativeED().Clone();
                                rotation.FromAxes(x, y, z);
                            }
                            camera.setPosition(pos);
                            camera.setRotation(rotation);
                        }
                        let aspectRatio = 1.0; //宽高比
                        if (cameraData.aspectRatio) {
                            aspectRatio = cameraData.aspectRatio;
                        }
                        camera.setAspectRatio(aspectRatio);
                        if (cameraData.nearDistance) {
                            camera.setNearClip(cameraData.nearDistance);
                        }
                        if (cameraData.farDistance) { //近点、远点
                            camera.setFarClip(cameraData.farDistance);
                        }
                        if (cameraData.focalDistance) { //
                            //camera.TryGetValue(<number>cameraData.focalDistance);
                        }
                        if (cameraData.height) { //高度
                            if (camera.isOrthographic()) {
                                camera.setOrthoSize(new M3D.M3DMath.Vector2(cameraData.height * aspectRatio, cameraData.height));
                            }
                        }
                        if (cameraData.zoom) {
                            camera.setZoom(cameraData.zoom);
                        }
                        this.modelView.setCamera(camera);
                    }
                }
            }
            startAnimation(task, data) {
                data.startTime = new Date().getTime();
                if (!data.animationTimer) {
                    data.animationTimer = new M3D.Utility.Timer();
                }
                data.animationTimer.setTimer(task, data, this.timerKeepMsecond);
                data.animationTimer.startTimer();
            }
            stopModelViewAnimation() {
                if (!this.animationTimer) {
                    return;
                }
                if (this.animationTimer.isStart()) {
                    this.animationTimer.stopTimer();
                }
                this.midRotation = [];
                this.midScale = [];
                this.midTran = [];
                this.midFov = [];
            }
            /**
             * @private
             * @description 计算相机动画插值数据
             * @param camera
             */
            interpolatCamera(camera) {
                let currentCamera = this.scene.getCurrentViewPort().getCamera();
                let cameraNewPosition = camera.getPosition();
                let zoom = 1;
                if (currentCamera.isOrthographic() && currentCamera.getOrthoSize() > 0 && camera.getOrthoSize() > 0) {
                    zoom = currentCamera.getOrthoSize() / camera.getOrthoSize() * camera.getZoom();
                }
                else {
                    zoom = currentCamera.getDefaultZoom();
                }
                M3D.Utility.MathVector.VecSlerp(currentCamera.getPosition(), cameraNewPosition, this.interpolatorCount, this.midTran);
                M3D.Utility.MathVector.RotationSlerp(currentCamera.getRotation(), camera.getRotation(), this.interpolatorCount, this.midRotation);
                M3D.Utility.MathVector.FloatSlerp(currentCamera.getZoom(), zoom, this.interpolatorCount, this.midScale);
            }
            /**
             *
             * @param view
             */
            interpolatorCamera(view) {
                //if (view.animationTimer.isStart()) {
                //    view.stopModelViewAnimation();
                //    return;
                //}
                let currentCamera = view.scene.getCurrentViewPort().getCamera();
                let currentTime = new Date().getTime();
                let currentFrame = Math.round((view.interpolatorCount * (currentTime - view.startTime)) / view.cameraAnimationTimer);
                if (currentTime - view.startTime > view.cameraAnimationTimer) {
                    currentFrame = view.interpolatorCount - 1;
                }
                let rotation = view.midRotation[currentFrame];
                if (rotation) {
                    currentCamera.setRotation(rotation);
                }
                let scale = view.midScale[currentFrame];
                if (scale) {
                    currentCamera.setZoom(scale);
                }
                let fov = view.midFov[currentFrame];
                if (fov) {
                    currentCamera.setFov(fov);
                }
                let tran = view.midTran[currentFrame];
                if (tran) {
                    currentCamera.setPosition(tran);
                    let sceneBox = view.scene.getBoundingBox();
                    //如果是平行投影，进行显示矫正
                    if (currentCamera.isOrthographic()) {
                        let cameraPos = currentCamera.getPosition();
                        //如果摄像机在模型内部，则进行调整
                        if (sceneBox.IsInside(cameraPos) == M3D.M3DMath.Intersection.INSIDE) {
                            let viewMat = currentCamera.getRotationQuaternion().RotationMatrix();
                            let direction = new M3D.M3DMath.Vector3(viewMat.data[2], viewMat.data[5], viewMat.data[8]);
                            let newPos = cameraPos.Add(direction.Multiply(sceneBox.Length()).Multiply(0.8));
                            currentCamera.setWorldPosition(newPos);
                        }
                    }
                }
                if (currentFrame >= view.interpolatorCount - 1) {
                    view.stopModelViewAnimation();
                    if (Object.getOwnPropertyNames(view.updateModelMap).length > 0) {
                        view.startAnimation(view.interPolatorModel, view);
                    }
                    else {
                        let message = new SView.ShowModelViewActionMessage(SView.ActionMessageType.ShowModelView, M3D.Actions.ActionState.Action_Success.toString(), view.modelView);
                        view.send(message);
                    }
                }
            }
            interPolatorModel(that) {
                //if (that.animationTimer.isStart()) {
                //    that.stopModelViewAnimation();
                //    return;
                //}
                let currentTime = new Date().getTime();
                let currentFrame = Math.round((that.interpolatorCount * (currentTime - that.startTime)) / that.modelExplosiveAnimationTimer);
                if (currentTime - that.startTime > that.modelExplosiveAnimationTimer || currentFrame === that.interpolatorCount) {
                    currentFrame = that.interpolatorCount - 1;
                }
                for (let modelSVLPath in that.updateModelMap) {
                    let path = M3D.Utility.PathHelper.SVLPathToM3D(modelSVLPath);
                    let model = that.scene.getShapes().getModelBySVLPath(path);
                    //let model: Model = <Model>that.GetShapeBySVLPath(modelSVLPath);
                    if (model) {
                        let tran = that.updateModelMap[modelSVLPath][0];
                        let rotate = that.updateModelMap[modelSVLPath][1];
                        let scale = that.updateModelMap[modelSVLPath][2];
                        if (tran[currentFrame] && rotate[currentFrame] && scale[currentFrame]) {
                            let newMatrix = new M3D.M3DMath.Matrix4(tran[currentFrame], rotate[currentFrame], scale[currentFrame]);
                            model.setPlaceMatrix(newMatrix);
                        }
                    }
                }
                if (currentFrame >= that.interpolatorCount - 1) {
                    // 清空数据
                    that.updateModelMap = {};
                    //view.GetSceneManager().GetSceneBoxNote().GetBoundingBox().defined = false;
                    //动画播放完毕移除轨迹线
                    M3D.Animation.AnimationHelper.removeTrochoid("ModelViewTrochoid", that.scene);
                    that.stopModelViewAnimation();
                    let message = new SView.ShowModelViewActionMessage(SView.ActionMessageType.ShowModelView, M3D.Actions.ActionState.Action_Success.toString(), that.modelView);
                    that.send(message);
                }
            }
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.ShowModelViewAction = ShowModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 设置透明度Action
         * @export
         * @class TransparencyAction
         * @extends {M3D.Actions.Action}
         */
        class TransparencyAction extends M3D.Actions.Action {
            constructor() {
                super(...arguments);
                /**
                 * 透明度 有效范围 0~1
                 * @private
                 */
                this.transparency = 1;
                this.models = [];
            }
            /**
             * @public
             * @description 设置透明度
             * @param {number} transparency
             * @memberof TransparencyAction
             */
            setTransparency(transparency) {
                if (transparency <= 0) {
                    transparency = 0;
                }
                else if (transparency >= 1) {
                    transparency = 1;
                }
                this.transparency = transparency;
            }
            setModels(...args) {
                if (args == null)
                    return;
                let temp = args[0];
                this.models = [];
                if (temp instanceof Array) {
                    this.models = temp;
                    return;
                }
                if (temp instanceof M3D.Shapes.Shape) {
                    this.models.push(temp);
                    return;
                }
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof TransparencyAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof TransparencyAction
             */
            onExecute() {
                if (this.models == null &&
                    this.models.length <= 0) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                this.prepareData();
                this.executeTransparency();
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 准备数据
             * @private
             * @memberof TransparencyAction
             */
            prepareData() {
                if (this.models != null &&
                    this.models.length > 0) {
                    return;
                }
                if (this.scene == null) {
                    return;
                }
                let shapes = this.scene.getSelector().getAll();
                this.models = [];
                if (shapes == null || shapes.length == 0) {
                    this.models = this.scene.getTopModel().getChildren();
                }
                else {
                    this.models = shapes;
                }
            }
            /**
             * @description 执行模型透明
             * @private
             * @memberof TransparencyAction
             */
            executeTransparency() {
                if (this.models == null ||
                    this.models.length <= 0) {
                    return;
                }
                for (let model of this.models) {
                    model.setAlpha(this.transparency);
                }
            }
        }
        Actions.TransparencyAction = TransparencyAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 更新批注测量位置Action
         * @export
         * @class UpdatePositionAction
         * @extends {M3D.Actions.Action}
         */
        class UpdatePositionAction extends M3D.Actions.Action {
            constructor(...args) {
                super();
                this.positionX = 0;
                this.positionY = 0;
                if (args.length == 1) {
                    if (args[0] instanceof M3D.Scene) {
                        let scene = args[0];
                        this.setScene(scene);
                    }
                }
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof UpdatePositionAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof UpdatePositionAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 设置参数
             * @param {(M3D.Shapes.Annotation | M3D.Shapes.Measure)} moveShape 需要移动的批注或测量对象
             * @param {number} x 需要移动的x位置
             * @param {number} y 需要移动的y位置
             * @return {boolean}
             * @memberof UpdatePositionAction
             */
            setParameter(moveShape, x, y) {
                this.moveShape = moveShape;
                this.positionX = x;
                this.positionY = y;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof UpdatePositionAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.AddFileAction.name, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (!this.scene) {
                    retState = M3D.Actions.ActionState.Action_Error;
                }
                else {
                    retState = this.updatePosition(this.moveShape);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @description 更新位置
             * @private
             * @param {(M3D.Shapes.Annotation | M3D.Shapes.Measure)} moveShape
             * @return {M3D.Actions.ActionState}
             * @memberof UpdatePositionAction
             */
            updatePosition(moveShape) {
                let camera = this.scene.getCurrentViewPort().getCamera();
                let rect = this.scene.getCurrentViewPort().getRect();
                let viewWidth = rect.Size().x;
                let viewHeight = rect.Size().y;
                let screenPos = new M3D.M3DMath.Vector2(this.positionX / viewWidth, this.positionY / viewHeight);
                if (moveShape) {
                    let depthPosition = this.getLinePoint();
                    if (depthPosition) {
                        let position = camera.screenToWorldPoint(screenPos, depthPosition);
                        if (moveShape instanceof M3D.Shapes.Annotation && moveShape.getParentModel()) {
                            const worldTransformInverse = moveShape.getParentModel().getWorldTransform().Inverse();
                            position = worldTransformInverse.Multiply(position);
                        }
                        moveShape.setImagePosition(position);
                        if (moveShape instanceof M3D.Shapes.Annotation) {
                            moveShape.setCenterPos(position);
                        }
                        else {
                            moveShape.setPosition(position);
                        }
                        this.scene.refresh();
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_Error;
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 获取引线的起点
             * @protected
             * @return {M3D.M3DMath.Vector3}
             * @memberof UpdatePositionAction
             */
            getLinePoint() {
                let lineList = this.moveShape.getLineList();
                let position = null;
                if (lineList && lineList.length > 0) {
                    for (let line of lineList) {
                        if (!line) {
                            continue;
                        }
                        if (line.getName() === M3D.MeasureNames.LINE_LEADER) {
                            position = line.getStartPoint();
                            break;
                        }
                    }
                }
                return position;
            }
        }
        Actions.UpdatePositionAction = UpdatePositionAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 热点动作类型
         * @export
         * @enum {number}
         */
        let HotSpotActionType;
        (function (HotSpotActionType) {
            HotSpotActionType[HotSpotActionType["OPENFILE"] = 0] = "OPENFILE";
            HotSpotActionType[HotSpotActionType["OPENWEB"] = 1] = "OPENWEB";
            HotSpotActionType[HotSpotActionType["OPENFTP"] = 2] = "OPENFTP";
            HotSpotActionType[HotSpotActionType["OPENVIEW"] = 3] = "OPENVIEW";
            HotSpotActionType[HotSpotActionType["SHOWNOTE"] = 4] = "SHOWNOTE";
            HotSpotActionType[HotSpotActionType["PLAYANIMATION"] = 5] = "PLAYANIMATION";
            HotSpotActionType[HotSpotActionType["HIGHLIGHTPARTS"] = 6] = "HIGHLIGHTPARTS";
            HotSpotActionType[HotSpotActionType["SHOWDESCRIPTION"] = 7] = "SHOWDESCRIPTION";
            HotSpotActionType[HotSpotActionType["OPENAUDIO"] = 8] = "OPENAUDIO";
        })(HotSpotActionType = Actions.HotSpotActionType || (Actions.HotSpotActionType = {}));
        ;
        /**
         * @memberof SView.Actions
         * @description 热点动作基类
         * @export
         * @class HotSpotAction
         * @extends {M3D.Actions.Action}
         */
        class HotSpotAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of HotSpotAction.
             * @param {M3D.Scene} scene 场景对象
             * @param {HotSpotActionType} type 热点类型
             * @memberof HotSpotAction
             */
            constructor(scene, type) {
                super();
                this.scene = scene;
                this.type = type;
            }
            /**
             * @description 设置场景对象
             * @protected
             * @param {M3D.Scene} scene 场景对象
             * @memberof HotSpotAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @description 获取场景对象
             * @protected
             * @return {M3D.Scene}
             * @memberof HotSpotAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @description 执行action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof HotSpotAction
             */
            onExecute() {
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.HotSpotAction = HotSpotAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
/// <reference path = "./HotSpotAction.ts"/>
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 执行动画热点动作
         * @export
         * @class AnimationHotSpotAction
         * @extends {HotSpotAction}
         */
        class AnimationHotSpotAction extends Actions.HotSpotAction {
            /**
             * @constructor
             * Creates an instance of AnimationHotSpotAction.
             * @param {S3DView} view S3DView对象
             * @param {number} processID 动画过程ID
             * @param {number} stepID 动画步骤ID
             * @memberof AnimationHotSpotAction
             */
            constructor(view, processID, stepID) {
                super(view.getScene(), Actions.HotSpotActionType.PLAYANIMATION);
                this.simulationAnimationManager = null;
                this.viewer = view;
                this.processID = processID;
                this.stepID = stepID;
                if (this.stepID == -1) {
                    this.stepID = null;
                }
                if (!this.simulationAnimationManager) {
                    this.simulationAnimationManager = M3D.Animation.SimulationAnimationManager.getInstance();
                    this.simulationAnimationManager.setScene(this.getScene());
                }
            }
            /**
             * @public
             * @description 设置动画过程的ID
             * @param {number} id 动画过程ID
             * @memberof AnimationHotSpotAction
             */
            setProcessID(id) {
                this.processID = id;
            }
            /**
             * @public
             * @description 设置动画步骤的ID
             * @param {number} id 动画步骤的ID
             * @memberof AnimationHotSpotAction
             */
            setStepID(id) {
                this.stepID = id;
            }
            /**
             * @public
             * @description 获取动画过程ID
             * @return {number}
             * @memberof AnimationHotSpotAction
             */
            getProcessID() {
                return this.processID;
            }
            /**
             * @public
             * @description 获取动画步骤的ID
             * @return {number}
             * @memberof AnimationHotSpotAction
             */
            getStepID() {
                return this.stepID;
            }
            /**
             * @protected
             * @description 开始执行Action
             * @return {M3D.Actions.ActionState}
             * @memberof AnimationHotSpotAction
             */
            onExecute() {
                //是否有动画
                if (!this.simulationAnimationManager.hasAnimations()) {
                    //判断是否已加载文件
                    this.viewer.getAnimationPlayer().getAnimation().then(() => {
                        this.viewer.getAnimationPlayer().setLoop(false);
                        this.viewer.getAnimationPlayer().open();
                        this.viewer.getAnimationPlayer().playAnimation(this.processID, this.stepID);
                    }).catch(() => {
                        //
                        return M3D.Actions.ActionState.Action_Error;
                    });
                }
                else {
                    //有动画,执行;
                    this.viewer.getAnimationPlayer().setLoop(false);
                    this.viewer.getAnimationPlayer().open();
                    this.viewer.getAnimationPlayer().playAnimation(this.processID, this.stepID);
                }
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.AnimationHotSpotAction = AnimationHotSpotAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 热点切换场景动作
         * @export
         * @class AreaHotSpotAction
         * @extends {HotSpotAction}
         */
        class AreaHotSpotAction extends Actions.HotSpotAction {
            /**
             * @constructor
             * Creates an instance of AreaHotSpotAction.
             * @param {S3DView} view S3DView对象
             * @param {number} areaID 场景Area对象ID
             * @memberof AreaHotSpotAction
             */
            constructor(view, areaID) {
                super(view.getScene(), Actions.HotSpotActionType.OPENFILE);
                this.areaID = -1; //场景ID
                this.viewer = view;
                this.areaID = areaID;
            }
            /**
             * @public
             * @description 设置场景的ID
             * @param {number} id
             * @memberof AreaHotSpotAction
             */
            setAreaID(id) {
                this.areaID = id;
            }
            /**
             * @public
             * @description 获取场景的ID
             * @return {number}
             * @memberof AreaHotSpotAction
             */
            getAreaID() {
                return this.areaID;
            }
            /**
             * @description 执行action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof AreaHotSpotAction
             */
            onExecute() {
                let ret = M3D.Actions.ActionState.Action_Success;
                if (this.areaID != -1) {
                    let targeArea = this.getScene().getShapes().getShape(this.areaID);
                    if (targeArea) {
                        if (targeArea.getType() === M3D.Shapes.AreaType.AreaType_OpenFile) {
                            let filePath = targeArea.getFilePath();
                            let openFileAction = new Actions.OpenFileAction();
                            openFileAction.setS3DView(this.viewer);
                            let result = M3D.Utility.FileHelper.findFile(filePath, this.viewer.getScene());
                            let type = result[0]; // 0 查找到文件
                            let retFilePath = result[1];
                            if (retFilePath && type !== -1) {
                                openFileAction.setFileName(retFilePath);
                                openFileAction.execute();
                            }
                            else {
                                //发出没有文件的消息
                                ret = M3D.Actions.ActionState.Action_Error;
                            }
                        }
                        else if (targeArea.getType() === M3D.Shapes.AreaType.AreaType_ChangeView) {
                            let viewID = targeArea.getModelViewID();
                            if (this.showModelViewAction == null) {
                                this.showModelViewAction = new Actions.ShowModelViewAction();
                                //设置视口
                                this.showModelViewAction.setScene(this.getScene());
                            }
                            //设置视口
                            this.showModelViewAction.setScene(this.getScene());
                            this.showModelViewAction.setModelViewObjID(viewID);
                            this.showModelViewAction.execute();
                        }
                    }
                }
                return ret;
            }
        }
        Actions.AreaHotSpotAction = AreaHotSpotAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 切换视图热点动作
         * @export
         * @class ViewHotSpotAction
         * @extends {HotSpotAction}
         */
        class ViewHotSpotAction extends Actions.HotSpotAction {
            /**
             * @constructor
             * Creates an instance of ViewHotSpotAction.
             * @param {M3D.Scene} scene 场景对象
             * @param {number} viewID 视图id
             * @memberof ViewHotSpotAction
             */
            constructor(scene, viewID) {
                super(scene, Actions.HotSpotActionType.OPENVIEW);
                this.viewID = viewID;
            }
            /**
             * @public
             * @description 设置视图的ID
             * @param {number} id
             * @memberof ViewHotSpotAction
             */
            setViewID(id) {
                this.viewID = id;
            }
            /**
             * @public
             * @description 获取视图的ID
             * @return {number}
             * @memberof ViewHotSpotAction
             */
            getViewID() {
                return this.viewID;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof ViewHotSpotAction
             */
            onExecute() {
                if (this.showModelViewAction == null) {
                    this.showModelViewAction = new Actions.ShowModelViewAction();
                    //设置视口
                    this.showModelViewAction.setScene(this.getScene());
                }
                this.showModelViewAction.setModelViewObjID(this.viewID);
                this.showModelViewAction.execute();
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.ViewHotSpotAction = ViewHotSpotAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 声音热点动作
         * @export
         * @class VoiceHotSpotAction
         * @extends {HotSpotAction}
         */
        class VoiceHotSpotAction extends Actions.HotSpotAction {
            /**
             * @constructor
             * Creates an instance of VoiceHotSpotAction.
             * @param {M3D.Scene} scene 场景对象
             * @param {string} fileName 声音文件路径
             * @memberof VoiceHotSpotAction
             */
            constructor(scene, fileName) {
                super(scene, Actions.HotSpotActionType.OPENAUDIO);
                this.fileName = fileName;
            }
            /**
             * @description 设置声音文件路径
             * @protected
             * @param {string} voiceFileName 声音文件路径
             * @memberof VoiceHotSpotAction
             */
            setFileName(voiceFileName) {
                this.fileName = voiceFileName;
            }
            /**
             * @description 获取声音文件路径
             * @protected
             * @return {string}
             * @memberof VoiceHotSpotAction
             */
            getFileName() {
                return this.fileName;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof VoiceHotSpotAction
             */
            onExecute() {
                let ret = M3D.Actions.ActionState.Action_Error;
                if (this.fileName) {
                    //加载动画res
                    this.getScene().getLoader().getHotspotRes().then((resData) => {
                        if (resData) {
                            for (let [key, value] of resData) {
                                let name = key; //'hotspotres/M800000ECIX742GiJ4.mp3'
                                let nameEnd = name.lastIndexOf('.');
                                if (name.indexOf(this.fileName) != -1) {
                                    let imageSuffix = name.substring(nameEnd + 1, name.length);
                                    if (imageSuffix.toLowerCase() === "mp3" || imageSuffix.toLowerCase() === "wma" || imageSuffix.toLowerCase() === "wav") {
                                        let resource = this.parseMp3(name, value);
                                        if (resource) {
                                            M3D.Animation.SoundPlayer.instance().play(resource.getBlobPath(), true, 0);
                                            ret = M3D.Actions.ActionState.Action_Success;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }).catch((error) => {
                        //
                    });
                }
                return ret;
            }
            parseMp3(name, file) {
                if (!file) {
                    return;
                }
                let blob = new Blob([file], { type: "application/octet-binary" });
                let url = URL.createObjectURL(blob);
                let resource = this.getScene().getResources().getOrCreateResource(name, M3D.Assets.ResourceType.AUDIO);
                if (resource) {
                    resource.setBlobPath(url);
                }
                return resource;
            }
        }
        Actions.VoiceHotSpotAction = VoiceHotSpotAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 测量Action基类
         * @export
         * @class MeasureAction
         * @extends {M3D.Actions.Action}
         */
        class MeasureAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of MeasureAction.
             * @memberof MeasureAction
             */
            constructor() {
                super();
                this.measure = null;
                this.measureType = M3D.Shapes.MeasureType.BASE_MEASURE;
                this.objID = 0;
                this.parentModelID = 0;
                this.measureType = M3D.Shapes.MeasureType.BASE_MEASURE;
                this.thicknessType = M3D.Config.ViewPortParameters.getInstance().getNumberParameter(M3D.Config.thicknessDirectionType);
                this.titleTextColor = M3D.M3DMath.Color.BLACK();
                this.fontSize = M3D.ANNOTATION_TEXT_FONT_SIZE;
                this.fontName = "";
                this.titleBackgroundColor = new M3D.M3DMath.Color(75.0 / 255.0, 238.0 / 255.0, 253.0 / 255.0);
                this.resultBackgroundColor = M3D.M3DMath.Color.WHITE();
                this.maxDistanceColor = M3D.M3DMath.Color.BLACK();
                this.minDistanceColor = M3D.M3DMath.Color.BLACK();
                this.resultTextColor = M3D.M3DMath.Color.BLACK();
                this.scene = null;
                this.isShowMaxDistance = false;
                this.isShowMinDistance = false;
                this.firstPosition = null;
                this.secondPosition = null;
                this.thirdPosition = null;
                this.fourthPosition = null;
                this.distanceLineStartPosition = null;
                this.distanceLineEndPosition = null;
                this.leaderStartPosition = null;
                this.leaderEndPosition = null;
                this.markingColor = new M3D.M3DMath.Color(255.0 / 255.0, 117.0 / 255.0, 24.0 / 255.0);
                this.leaderColor = new M3D.M3DMath.Color(241.0 / 255.0, 167.0 / 255.0, 71.0 / 255.0);
                this.extendLineColor = new M3D.M3DMath.Color(63.0 / 255.0, 104.0 / 255.0, 188.0 / 255.0);
                //this.squareBoxColor = M3D.M3DMath.Color.YELLOW();
                this.squareBoxColor = new M3D.M3DMath.Color(255.0 / 255.0, 117.0 / 255.0, 24.0 / 255.0);
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof MeasureAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 获取场景对象
             * @return {M3D.Scene}
             * @memberof MeasureAction
             */
            getScene() {
                return this.scene;
            }
            /**
             * @public
             * @description 获取创建的测量
             * @return {M3D.Shapes.Shape}
             * @memberof MeasureAction
             */
            getShape() {
                return this.measure;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                retState = this.doMeasure();
                if (retState === M3D.Actions.ActionState.Action_Error)
                    return retState;
                if (this.objID === 0 && (retState === M3D.Actions.ActionState.Action_Success
                    || retState === M3D.Actions.ActionState.Action_PartExecuted)) {
                    if (!this.addToScene())
                        retState = M3D.Actions.ActionState.Action_Error;
                }
                this.scene.getViewPorts().markDirty();
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @description 创建测量
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            doMeasure() {
                Actions.ActionLogger.ACTIONLOGD("CreateAngleAction::CreateMeasure begin!");
                let state = M3D.Actions.ActionState.Action_GetObjectFailure;
                return state;
            }
            /**
             * @beta
             * @param {string} jsonString
             * @return {*}
             * @memberof MeasureAction
             */
            fromJson(jsonString) {
                return false;
            }
            /**
             * @beta
             * @return {*}
             * @memberof MeasureAction
             */
            toJson() {
                return null;
            }
            /**
             * @description 根据参数创建测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            setShapeElement() {
                Actions.ActionLogger.ACTIONLOGD("CreateAngleAction::CreateMeasure begin!");
                let state = M3D.Actions.ActionState.Action_Success;
                return state;
            }
            /**
             * @public
             * @description 清空测量中图片
             * @memberof MeasureAction
             */
            removeImage() {
                this.measure.clearImage();
            }
            /**
             * @protected
             * @description 设置线
             * @param lineName ，beginPoint 始点 ，endPoint 终点
             * */
            setLine(lineName, color, beginPoint, endPoint, lineType = M3D.Shapes.CurvePatternMode.SolidLine) {
                let methodName = this.setLine.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    if (lines[i].getName() === lineName) {
                        lines[i].setStartPoint(beginPoint);
                        lines[i].setEndPoint(endPoint);
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                let leaderLine = new M3D.Shapes.Line();
                leaderLine.setColor(color);
                leaderLine.setLineType(lineType);
                leaderLine.setStartPoint(beginPoint);
                leaderLine.setEndPoint(endPoint);
                leaderLine.setName(lineName);
                leaderLine.setScene(this.scene);
                this.measure.addLine(leaderLine);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
             * @description 删除线
             * @param lineName 线的名称
             * */
            removeLine(lineName) {
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    if (lines[i].getName() === lineName) {
                        this.measure.removeLine(lines[i]);
                        this.scene.refresh();
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            clearLines() {
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    this.measure.removeLine(lines[i]);
                }
                this.scene.refresh();
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            clearMeasure() {
                if (this.measure) {
                    let lines = this.measure.getLineList();
                    for (let i = 0; i < lines.length; i++) {
                        this.measure.removeLine(lines[i]);
                        i--;
                    }
                    let points = this.measure.getPointList();
                    for (let i = 0; i < points.length; i++) {
                        this.measure.removePoint(points[i]);
                        i--;
                    }
                    //点点测量，有第二个点时不需要清除图片
                    if (this.measureType === M3D.Shapes.MeasureType.PNT_PNT_DISTANCE &&
                        this.secondPosition) {
                        //不需要任何操作
                    }
                    else {
                        this.measure.clearImage();
                    }
                    this.scene.refresh();
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
             * @description 设置点位置
             * @param index 点的index，position 点的坐标
             * */
            setPoint(name, position, size) {
                let methodName = this.setPoint.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let bFlag = false;
                let points = this.measure.getPointList();
                for (let i = 0; i < points.length; i++) {
                    if (points[i].getName() === name) {
                        bFlag = true;
                        points[i].setPosition(position);
                        if (points[i] instanceof M3D.Shapes.ImagePoint) {
                            points[i].getImage().setPosition(position);
                        }
                    }
                }
                if (!bFlag) {
                    let point = new M3D.Shapes.ImagePoint();
                    point.setName(name);
                    point.setPosition(position);
                    point.setScene(this.scene);
                    if (name === M3D.MeasureNames.MEASURE_POINT_CENTER) {
                        point.setPointType(7);
                        point.setSize(1.8);
                    }
                    if (size) {
                        point.setSize(size);
                    }
                    this.measure.addPoint(point);
                    //let pntImage = new M3D.Shapes.Image(position, new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //pntImage.setPosition(position);
                    //let pointSize = M3D.Utility.ShapeHelper.getCommonSize(this.scene.getViewPorts().getViewport(0), new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //point.getImage().setSize(pointSize);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
            }
            /**
             * @protected
             * @description 删除点
             * @param pointName 点的名称
             * */
            removePoint(pointName) {
                let points = this.measure.getPointList();
                for (let i = 0; i < points.length; i++) {
                    if (points[i].getName() === pointName) {
                        this.measure.removePoint(points[i]);
                        this.scene.refresh();
                    }
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @description 设置线的显示属性
             * */
            /**
             * @public
             * @description 设置线的属性
             * @param {string} lineName 线的名称
             * @param {M3D.M3DMath.Vector3} position 点
             * @param {boolean} allowTran 是否跟随平移
             * @param {boolean} allowRotate 是否跟随旋转
             * @param {boolean} allowScale 是否跟随缩放
             * @return {M3D.Actions.ActionState}
             * @memberof MeasureAction
             */
            setLineProperty(lineName, position, allowTran, allowRotate, allowScale) {
                let methodName = this.setLineProperty.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let lines = this.measure.getLineList();
                for (let i = 0; i < lines.length; i++) {
                    if (lines[i].getName() === lineName) {
                        lines[i].createBillBoard(position, allowTran, allowRotate, allowScale);
                        // 只有线的标识线不允许缩放时，设置缩放范围
                        if (!allowScale && (lineName === M3D.MeasureNames.MEASURE_LINE_MARK_1 || lineName === M3D.MeasureNames.MEASURE_LINE_MARK_2)) {
                            lines[i].getBillBoard().setScaleLimits(1.0, Number.MAX_VALUE);
                        }
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
             * @description 设置基本参数
             * */
            setBaseInfo() {
                if (this.measure) {
                    this.measure.setMeasureType(this.measureType);
                }
            }
            /**
             * @protected
             * @description 添加节点到场景树
             * */
            addToScene() {
                let methodName = this.addToScene.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                //添加批注到场景
                let ret = false;
                if (this.measure && this.scene) {
                    //this.measure.setVisible(true);
                    let measures = this.scene.getShapes();
                    this.measure.setVisible(false, true);
                    measures.addShape(this.measure);
                    this.measure.setVisible(true, false);
                    this.scene.refresh();
                    //解锁
                    ret = true;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return ret;
            }
            /**
             * @public
             * @description 编辑指定文本
             * @param index 文本的索引
             * @param angle 测量结果文本
             */
            setComText(index, angle) {
                //let bFlag = false;
                let comtexts = this.measure.getComTexts();
                if (comtexts.length > index) {
                    let comtext = comtexts[index];
                    if (comtext) {
                        let tmp = angle.toString();
                        comtext.setText(0, tmp);
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                let ct = new M3D.Shapes.ComText();
                let tmp = angle.toString();
                let t = new M3D.Shapes.Text();
                ct.addText(t);
                t.setText(tmp);
                this.measure.addComText(ct);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @protected
            * @description 选择的面显示矩形
            * @param pointArray point数组
            * @param boxCenter 包围盒中心
            * @param direction 方向
            * @param faceNormal 面的法向量
            */
            getPointSquareBoxMark(pointArray, boxCenter, direction, faceNormal, length) {
                let methodName = this.getPointSquareBoxMark.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                //假设direction表示X轴，faceNormal表示Z轴，则可求出另外一个边方向向量Y
                let x = direction.Normalized();
                //let z: M3D.M3DMath.Vector3 = faceNormal.Normalized();
                let y = (direction.CrossProduct(faceNormal)).Normalized();
                let p1 = boxCenter.Add((x.Add(y)).Multiply(length));
                pointArray.push(p1);
                let p2 = boxCenter.Add((x.Sub(y)).Multiply(length));
                pointArray.push(p2);
                let p3 = boxCenter.Add((y.Sub(x)).Multiply(length));
                pointArray.push(p3);
                let p4 = boxCenter.Sub((x.Add(y)).Multiply(length));
                pointArray.push(p4);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return pointArray;
            }
            /**
             * @protected
            * @description 判断剖面线是否可用
            * @param bFirstShape 第一次选择
            * @param firstShape 第一对象
            * @param secondShape 第二对象
            * @return true 成功 false 失败
            */
            isSectionEdgeEnable(bFirstShape, firstShape, secondShape) {
                let methodName = this.isSectionEdgeEnable.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if (!bFirstShape) {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return (isMeasureClipEdge && firstShape && secondShape
                        && (firstShape.getShapeType() === M3D.Shapes.ShapeType.EDGE && secondShape.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE ||
                            firstShape.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE && secondShape.getShapeType() === M3D.Shapes.ShapeType.EDGE ||
                            firstShape.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE && secondShape.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE));
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return (isMeasureClipEdge && firstShape &&
                        firstShape.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE && !secondShape);
                }
            }
            pntInSegment(...restOfName) {
                let methodName = this.pntInSegment.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length === 4) {
                    if (restOfName[0] instanceof M3D.M3DMath.Vector3 && restOfName[1] instanceof M3D.M3DMath.Vector3 && restOfName[2] instanceof M3D.M3DMath.Vector3 && restOfName[3] instanceof M3D.M3DMath.Vector3) {
                        let pntCoordinate = restOfName[0];
                        let point1 = restOfName[1];
                        let point2 = restOfName[2];
                        let projecPoint = restOfName[3];
                        let startPointDistance;
                        let endPointDistance;
                        let direction = point2.Sub(point1).Clone();
                        let ray = new M3D.M3DMath.Ray(point1, direction);
                        let projection = ray.Project(pntCoordinate);
                        startPointDistance = (projection.Sub(point1)).Length();
                        endPointDistance = (projection.Sub(point2)).Length();
                        if (!M3D.M3DMath.Equals((startPointDistance + endPointDistance), direction.Length())) {
                            if (startPointDistance > endPointDistance) {
                                projecPoint.copyFrom(point2);
                            }
                            else {
                                projecPoint.copyFrom(point1);
                            }
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return false;
                        }
                        else {
                            projecPoint.copyFrom(projection);
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return true;
                        }
                    }
                }
                else {
                    let pntCoordinate = restOfName[0];
                    let lineFeature = restOfName[1];
                    let startPntDistance, endPntDistance;
                    let start = lineFeature.getStartPoint();
                    let end = lineFeature.getEndPoint();
                    startPntDistance = pntCoordinate.Sub(start).Length();
                    endPntDistance = pntCoordinate.Sub(end).Length();
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    if ((startPntDistance + endPntDistance) > (start.Sub(end)).Length())
                        return false;
                    else
                        return true;
                }
            }
            lineLineDistance(...restOfName) {
                let methodName = this.lineLineDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length === 6) {
                    let result = [];
                    let lineFeature0 = restOfName[0];
                    let lineFeature1 = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let parallel = restOfName[5];
                    let alpha0 = lineFeature0.getEndPoint().Sub(lineFeature0.getStartPoint()).Normalized();
                    let alpha1 = lineFeature1.getEndPoint().Sub(lineFeature1.getStartPoint()).Normalized();
                    if (alpha0.CrossProduct(alpha1).Length() >= -0.001
                        && alpha0.CrossProduct(alpha1).Length() <= 0.001) {
                        //两直线平行
                        //转化到点到直线的距离
                        parallel = true;
                        let projectPnt = new M3D.M3DMath.Vector3();
                        let ray = new M3D.M3DMath.Ray(lineFeature0.getStartPoint(), lineFeature0.getDirection());
                        //		 Vector3 offset =lineAttribute1.GetStartPoint() -  ray.GetOrigin();
                        //		 Vector3 m_direction = lineAttribute1.GetDirection();
                        //		 Vector3 dot = offset.DotProduct(m_direction) * m_direction;
                        //		 LOGE("offset %s ",offset.Tostring().c_str());
                        //		 LOGE("GetDirection %s ",lineAttribute0.GetDirection().Tostring().c_str());
                        projectPnt.copyFrom(ray.Project(lineFeature1.getStartPoint()));
                        distance = lineFeature1.getStartPoint().Sub(projectPnt).Length();
                        let l1 = lineFeature0.getStartPoint().Sub(projectPnt).Length();
                        let l2 = lineFeature0.getEndPoint().Sub(projectPnt).Length();
                        let delta = lineFeature0.getLength() - (l1 + l2);
                        projectPnt0.copyFrom(projectPnt);
                        projectPnt1.copyFrom(lineFeature1.getStartPoint());
                    }
                    else {
                        parallel = false;
                        //空间异面直线
                        let Xa, Ya, Za, Xb, Yb, Zb, Xc, Yc, Zc, Xd, Yd, Zd;
                        Xa = Number(lineFeature0.getStartPoint().x);
                        Ya = Number(lineFeature0.getStartPoint().y);
                        Za = Number(lineFeature0.getStartPoint().z);
                        Xb = Number(lineFeature0.getEndPoint().x);
                        Yb = Number(lineFeature0.getEndPoint().y);
                        Zb = Number(lineFeature0.getEndPoint().z);
                        Xc = Number(lineFeature1.getStartPoint().x);
                        Yc = Number(lineFeature1.getStartPoint().y);
                        Zc = Number(lineFeature1.getStartPoint().z);
                        Xd = Number(lineFeature1.getEndPoint().x);
                        Yd = Number(lineFeature1.getEndPoint().y);
                        Zd = Number(lineFeature1.getEndPoint().z);
                        let H, I, J, K, L, M;
                        H = Xb - Xa;
                        I = Yb - Ya;
                        J = Zb - Za;
                        K = Xd - Xc;
                        L = Yd - Yc;
                        M = Zd - Zc;
                        let N, O, P, Q;
                        N = H * I * L - I * I * K - J * J * K + H * J * M;
                        O = H * H * L - H * I * K - I * J * M + J * J * L;
                        P = H * J * K - H * H * M - I * I * M + I * J * L;
                        Q = -Xa * N + Ya * O - Za * P;
                        let k = (O * Yc - N * Xc - P * Zc - Q) / (N * K - O * L + P * M);
                        let P1 = new M3D.M3DMath.Vector3(), P2 = new M3D.M3DMath.Vector3();
                        P1.x = K * k + Xc;
                        P1.y = L * k + Yc;
                        P1.z = M * k + Zc;
                        let projectPntT;
                        let rayT = new M3D.M3DMath.Ray(lineFeature0.getStartPoint(), lineFeature0.getDirection());
                        projectPntT = rayT.Project(P1);
                        projectPnt1.copyFrom(P1);
                        projectPnt0.copyFrom(projectPntT);
                        distance = projectPntT.Sub(P1).Length();
                    }
                    result.push(distance);
                    result.push(parallel);
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return result;
                }
                else if (restOfName.length === 7) {
                    if (restOfName[0] instanceof M3D.Shapes.EllipseFeature) {
                        let ellipseFeature0 = restOfName[0];
                        let ellipseFeature1 = restOfName[1];
                        let distance0 = restOfName[2];
                        let distance1 = restOfName[3];
                        let projectPnt0 = restOfName[4];
                        let projectPnt1 = restOfName[5];
                        let types = restOfName[6];
                        let center0 = new M3D.M3DMath.Vector3(), center1 = new M3D.M3DMath.Vector3(); //圆心
                        let radius0, radius1; //半径
                        let start0 = new M3D.M3DMath.Vector3(), start1 = new M3D.M3DMath.Vector3(); //起点
                        let dirX0 = new M3D.M3DMath.Vector3(), dirY0 = new M3D.M3DMath.Vector3(), dirZ0 = new M3D.M3DMath.Vector3(), dirX1 = new M3D.M3DMath.Vector3(), dirY1 = new M3D.M3DMath.Vector3(), dirZ1 = new M3D.M3DMath.Vector3();
                        let dirResult0 = ellipseFeature0.getXYZDir();
                        dirX0 = dirResult0[0];
                        dirY0 = dirResult0[1];
                        dirZ0 = dirResult0[2];
                        let dirResult1 = ellipseFeature1.getXYZDir();
                        dirX1 = dirResult1[0];
                        dirY1 = dirResult1[1];
                        dirZ1 = dirResult1[2];
                        center0 = ellipseFeature0.getCenter();
                        center1 = ellipseFeature1.getCenter();
                        radius0 = ellipseFeature0.getMajorRadius();
                        radius1 = ellipseFeature1.getMajorRadius();
                        start0 = ellipseFeature0.getStartPoint();
                        start1 = ellipseFeature1.getStartPoint();
                        let parall = dirZ0.CrossProduct(dirZ1);
                        if (parall.IsZero()) {
                            //所在平面平行
                            if (center0.Sub(center1).IsZero()) {
                                //两点相等 同心圆弧
                                distance0 = 0.0;
                                distance1 = M3D.M3DMath.Abs(radius0 - radius1);
                                projectPnt0.copyFrom(start0);
                                projectPnt1.copyFrom(center1.Add((start0.Sub(center0)).Multiply(radius1).Divide(start0.Sub(center0).Length())));
                                //projectPnt1 = start1;
                                types = 2;
                            }
                            else {
                                //非共面，求轴心，和 圆心距
                                let ray = new M3D.M3DMath.Ray(center0, dirZ0);
                                let projectPnt = ray.Project(center1);
                                distance0 = projectPnt.Sub(center1).Length();
                                distance1 = center0.Sub(center1).Length();
                                projectPnt0.copyFrom(projectPnt);
                                projectPnt1.copyFrom(center1);
                                types = 1;
                            }
                        }
                        else {
                            //所在平面不平行
                            types = 0;
                            distance0 = -1;
                            distance1 = center0.Sub(center1).Length();
                            projectPnt0.copyFrom(center0);
                            projectPnt1.copyFrom(center1);
                        }
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return [distance0, distance1, types];
                    }
                    else if (restOfName[0] instanceof M3D.Shapes.Edge) {
                        let bRet = false;
                        let lineSet1 = restOfName[0];
                        let lineSet2 = restOfName[1];
                        let lineSet1MinPoint = restOfName[2];
                        let lineSet2MinPoint = restOfName[3];
                        let distance = restOfName[4];
                        let worldMatrix1 = restOfName[5];
                        let worldMatrix2 = restOfName[6];
                        let distanceTemp = Number.MAX_VALUE;
                        let vertexSets = this.scene.getVertexSetManager();
                        if (lineSet1 instanceof M3D.Shapes.SectionEdge && lineSet2 instanceof M3D.Shapes.SectionEdge) {
                            let pntPoints = lineSet1.getSectionLine().getData();
                            let dataLength1 = pntPoints.length;
                            let pntPoints2 = lineSet2.getSectionLine().getData();
                            let dataLength2 = pntPoints2.length;
                            for (let i = 0; i < dataLength1 - 1; i++) {
                                let point1 = pntPoints[i];
                                let point2 = pntPoints[i + 1];
                                for (let j = 0; j < dataLength2 - 1; j++) {
                                    let line2Point1 = pntPoints2[j];
                                    let line2Point2 = pntPoints2[j + 1];
                                    let alpha1 = line2Point1.Sub(line2Point2).Normalized();
                                    let results = M3D.Algorithm.TriangleData.sqrDistance(point1, point2, line2Point1, line2Point2, 0, 0);
                                    let s = results[1];
                                    let t = results[2];
                                    let P1 = (point2.Sub(point1)).Multiply(s).Add(point1);
                                    let P2 = (line2Point2.Sub(line2Point1)).Multiply(t).Add(line2Point1);
                                    distance = P2.Sub(P1).Length();
                                    console.log(distance);
                                    if (distanceTemp >= distance) {
                                        distanceTemp = distance;
                                        lineSet1MinPoint = P1;
                                        lineSet2MinPoint = P2;
                                    }
                                }
                            }
                        }
                        else if (lineSet1 instanceof M3D.Shapes.SectionEdge || lineSet2 instanceof M3D.Shapes.SectionEdge) {
                            let sectionEdge = null;
                            let normalEdge = null;
                            if (lineSet1 instanceof M3D.Shapes.SectionEdge) {
                                sectionEdge = lineSet1;
                                normalEdge = lineSet2;
                            }
                            else {
                                sectionEdge = lineSet2;
                                normalEdge = lineSet1;
                            }
                            let body = normalEdge.getBody();
                            let bodyVertexHeader = vertexSets.getVertexHeader(body.getProto());
                            let edgeVertexHeader = vertexSets.getVertexHeader(normalEdge.getProto());
                            let edgeVertexSet = edgeVertexHeader.vertexSet;
                            let bodyVertexSet = bodyVertexHeader.vertexSet;
                            if (edgeVertexSet) {
                                let useIndex = edgeVertexHeader.useIndex;
                                if (!useIndex)
                                    return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            else {
                                return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            let indexCount = normalEdge.getIndexLength();
                            let indexOffset = normalEdge.getIndexOffset();
                            let meshBuffer = bodyVertexSet.getMeshArray();
                            let indexBuffer = edgeVertexSet.getMeshArray();
                            let pointArray = new Float32Array(meshBuffer, bodyVertexHeader.vertexOffset, bodyVertexHeader.vertexLength);
                            let pEdgeIndexArray = new Uint32Array(indexBuffer, indexOffset, indexCount);
                            let pntPoints = sectionEdge.getSectionLine().getData();
                            let dataLength = pntPoints.length;
                            if (lineSet1 instanceof M3D.Shapes.SectionEdge) {
                                for (let i = 0; i < dataLength - 1; i++) {
                                    let point1 = pntPoints[i];
                                    let point2 = pntPoints[i + 1];
                                    for (let j = 0; j < indexCount; j += 2) {
                                        let p1 = new M3D.M3DMath.Vector3();
                                        let p2 = new M3D.M3DMath.Vector3();
                                        p1.x = pointArray[pEdgeIndexArray[j] * 3 + 0];
                                        p1.y = pointArray[pEdgeIndexArray[j] * 3 + 1];
                                        p1.z = pointArray[pEdgeIndexArray[j] * 3 + 2];
                                        p2.x = pointArray[pEdgeIndexArray[j + 1] * 3 + 0];
                                        p2.y = pointArray[pEdgeIndexArray[j + 1] * 3 + 1];
                                        p2.z = pointArray[pEdgeIndexArray[j + 1] * 3 + 2];
                                        let line2Point1 = worldMatrix2.Multiply(p1);
                                        let line2Point2 = worldMatrix2.Multiply(p2);
                                        let alpha1 = (line2Point1.Sub(line2Point2)).Normalized();
                                        let s = 0;
                                        let t = 0;
                                        let results = M3D.Algorithm.TriangleData.sqrDistance(point1, point2, line2Point1, line2Point2, s, t);
                                        distance = results[0];
                                        s = results[1];
                                        t = results[2];
                                        let pnt1 = point1.Add(point2.Sub(point1).Multiply(s));
                                        let pnt2 = line2Point1.Add((line2Point2.Sub(line2Point1)).Multiply(t));
                                        distance = pnt2.Sub(pnt1).Length();
                                        if (distanceTemp >= distance) {
                                            distanceTemp = distance;
                                            lineSet1MinPoint = pnt1;
                                            lineSet2MinPoint = pnt2;
                                        }
                                    }
                                }
                            }
                            else {
                                for (let j = 0; j < indexCount; j += 2) {
                                    let p1 = new M3D.M3DMath.Vector3();
                                    let p2 = new M3D.M3DMath.Vector3();
                                    p1.x = pointArray[pEdgeIndexArray[j] * 3 + 0];
                                    p1.y = pointArray[pEdgeIndexArray[j] * 3 + 1];
                                    p1.z = pointArray[pEdgeIndexArray[j] * 3 + 2];
                                    p2.x = pointArray[pEdgeIndexArray[j + 1] * 3 + 0];
                                    p2.y = pointArray[pEdgeIndexArray[j + 1] * 3 + 1];
                                    p2.z = pointArray[pEdgeIndexArray[j + 1] * 3 + 2];
                                    let point1 = worldMatrix1.Multiply(p1);
                                    let point2 = worldMatrix1.Multiply(p2);
                                    for (let i = 0; i < dataLength - 1; i++) {
                                        let line2Point1 = pntPoints[i];
                                        let line2Point2 = pntPoints[i + 1];
                                        let s = 0;
                                        let t = 0;
                                        let results = M3D.Algorithm.TriangleData.sqrDistance(point1, point2, line2Point1, line2Point2, s, t);
                                        distance = results[0];
                                        s = results[1];
                                        t = results[2];
                                        let pnt1 = point1.Add(point2.Sub(point1).Multiply(s));
                                        let pnt2 = line2Point1.Add((line2Point2.Sub(line2Point1)).Multiply(t));
                                        distance = pnt2.Sub(pnt1).Length();
                                        if (distanceTemp >= distance) {
                                            distanceTemp = distance;
                                            lineSet1MinPoint = pnt1;
                                            lineSet2MinPoint = pnt2;
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            let body1 = lineSet1.getBody();
                            let body1VertexHeader = vertexSets.getVertexHeader(body1.getProto());
                            let edge1VertexHeader = vertexSets.getVertexHeader(lineSet1.getProto());
                            let edge1VertexSet = edge1VertexHeader.vertexSet;
                            let body1VertexSet = body1VertexHeader.vertexSet;
                            if (edge1VertexSet) {
                                let useIndex = edge1VertexHeader.useIndex;
                                if (!useIndex)
                                    return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            else {
                                return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            let body2 = lineSet2.getBody();
                            let body2VertexHeader = vertexSets.getVertexHeader(body2.getProto());
                            let edge2VertexHeader = vertexSets.getVertexHeader(lineSet2.getProto());
                            let edge2VertexSet = edge2VertexHeader.vertexSet;
                            let body2VertexSet = body2VertexHeader.vertexSet;
                            if (edge2VertexSet) {
                                let useIndex = edge2VertexHeader.useIndex;
                                if (!useIndex)
                                    return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            else {
                                return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                            }
                            let indexCount1 = lineSet1.getIndexLength();
                            let indexOffset1 = lineSet1.getIndexOffset();
                            let meshBuffer1 = body1VertexSet.getMeshArray();
                            let indexBuffer1 = edge1VertexSet.getMeshArray();
                            let pointArray1 = new Float32Array(meshBuffer1, body1VertexHeader.vertexOffset, body1VertexHeader.vertexLength);
                            let pEdgeIndexArray1 = new Uint32Array(indexBuffer1, indexOffset1, indexCount1);
                            let indexCount2 = lineSet2.getIndexLength();
                            let indexOffset2 = lineSet2.getIndexOffset();
                            let meshBuffer2 = body2VertexSet.getMeshArray();
                            let indexBuffer2 = edge2VertexSet.getMeshArray();
                            let pointArray2 = new Float32Array(meshBuffer2, body2VertexHeader.vertexOffset, body2VertexHeader.vertexLength);
                            let pEdgeIndexArray2 = new Uint32Array(indexBuffer2, indexOffset2, indexCount2);
                            //let dataLength = edge1VertexHeader.indexLength;
                            //let offset: number = edge1VertexHeader.indexOffset;
                            //let pointArray = new Float32Array(edge1Vertexset.getMeshArray(), edge1VertexHeader.vertexOffset, edge1VertexHeader.vertexLength);
                            //let pEdgeIndexArray: ArrayBuffer | number[] = edge1Vertexset.getEdgeIndexArray();
                            //let dataLength2 = edge2VertexHeader.indexLength;
                            //let offset2: number = edge2VertexHeader.indexOffset;
                            //let pointArray2 = new Float32Array(edge2Vertexset.getMeshArray(), edge2VertexHeader.vertexOffset, edge2VertexHeader.vertexLength);
                            //let pEdgeIndexArray2: ArrayBuffer | number[] = edge2Vertexset.getEdgeIndexArray();
                            for (let index = 0; index < indexCount1; index += 2) {
                                //for (let i = pEdgeIndexArray[0] + offset; i != pEdgeIndexArray[0] + offset + dataLength; i = i + 2) {
                                let p1 = new M3D.M3DMath.Vector3();
                                let p2 = new M3D.M3DMath.Vector3();
                                p1.x = pointArray1[pEdgeIndexArray1[index] * 3 + 0];
                                p1.y = pointArray1[pEdgeIndexArray1[index] * 3 + 1];
                                p1.z = pointArray1[pEdgeIndexArray1[index] * 3 + 2];
                                p2.x = pointArray1[pEdgeIndexArray1[index + 1] * 3 + 0];
                                p2.y = pointArray1[pEdgeIndexArray1[index + 1] * 3 + 1];
                                p2.z = pointArray1[pEdgeIndexArray1[index + 1] * 3 + 2];
                                let point1 = worldMatrix1.Multiply(p1);
                                let point2 = worldMatrix1.Multiply(p2);
                                let alpha0 = (point1.Sub(point2)).Normalized();
                                for (let j = 0; j < indexCount2; j += 2) {
                                    //for (let j = pEdgeIndexArray2[0] + offset2; j != pEdgeIndexArray2[0] + offset2 + dataLength2; j = j + 2) {
                                    let p1 = new M3D.M3DMath.Vector3();
                                    let p2 = new M3D.M3DMath.Vector3();
                                    p1.x = pointArray2[pEdgeIndexArray2[j] * 3 + 0];
                                    p1.y = pointArray2[pEdgeIndexArray2[j] * 3 + 1];
                                    p1.z = pointArray2[pEdgeIndexArray2[j] * 3 + 2];
                                    p2.x = pointArray2[pEdgeIndexArray2[j + 1] * 3 + 0];
                                    p2.y = pointArray2[pEdgeIndexArray2[j + 1] * 3 + 1];
                                    p2.z = pointArray2[pEdgeIndexArray2[j + 1] * 3 + 2];
                                    let line2Point1 = worldMatrix2.Multiply(p1);
                                    let line2Point2 = worldMatrix2.Multiply(p2);
                                    let alpha1 = (line2Point1.Sub(line2Point2)).Normalized();
                                    let s = 0;
                                    let t = 0;
                                    let results = M3D.Algorithm.TriangleData.sqrDistance(point1, point2, line2Point1, line2Point2, s, t);
                                    distance = results[0];
                                    s = results[1];
                                    t = results[2];
                                    let pnt1 = point1.Add(point2.Sub(point1).Multiply(s));
                                    let pnt2 = line2Point1.Add((line2Point2.Sub(line2Point1)).Multiply(t));
                                    distance = pnt2.Sub(pnt1).Length();
                                    if (distanceTemp >= distance) {
                                        distanceTemp = distance;
                                        lineSet1MinPoint = pnt1;
                                        lineSet2MinPoint = pnt2;
                                    }
                                }
                            }
                        }
                        distance = distanceTemp;
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return [distance, lineSet1MinPoint, lineSet2MinPoint, bRet];
                    }
                }
            }
            /**
             * @protected
             * @description 获取引线点（起止点）
             * @param firstPoint，secondPoint ，beginPoint 始点 ，endPoint 终点
             * */
            getLeaderPoints(firstPoint, secondPoint) {
                let methodName = this.getLeaderPoints.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let startPoint = (firstPoint.Add(secondPoint)).Divide(M3D.MeasureNames.VALUE_2);
                let lineLength = (firstPoint.Sub(secondPoint)).Length();
                let directionVector = secondPoint.Sub(firstPoint);
                let cameraRay = this.scene.getCurrentViewPort().getCamera().getScreenRay(M3D.MeasureNames.VALUE_0, M3D.MeasureNames.VALUE_0);
                let zDirection = cameraRay.GetDirection();
                directionVector = zDirection.CrossProduct(directionVector);
                directionVector.Normalize();
                let thirdCoord = firstPoint.Add(directionVector.Multiply(lineLength).Multiply(M3D.MeasureNames.VALUE_0DOT2));
                let fourCoord = secondPoint.Add(directionVector.Multiply(lineLength).Multiply(M3D.MeasureNames.VALUE_0DOT2));
                let endPoint = (thirdCoord.Add(fourCoord)).Divide(M3D.MeasureNames.VALUE_2);
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return [startPoint, endPoint];
            }
            /**
             * @public
             * @description 获取面标记线长
             * @return {number}
             * @memberof MeasureAction
             */
            getSquareBoxLineLength() {
                let width = 0, height = 0;
                width = this.scene.getCurrentViewPort().getRect().Size().x;
                height = this.scene.getCurrentViewPort().getRect().Size().y;
                let distance = 0;
                distance = width > height ? (height / M3D.MeasureNames.SCREENSQUAREBOXSCALE)
                    : (width / M3D.MeasureNames.SCREENSQUAREBOXSCALE);
                //let tmpScreenPoint1: M3D.M3DMath.Vector2 = new M3D.M3DMath.Vector2(0, 0),
                //    tmpScreenPoint2: M3D.M3DMath.Vector2 = new M3D.M3DMath.Vector2(0, distance);
                //let tmpWorldPoint1: M3D.M3DMath.Vector3 = this.scene.getCurrentViewPort().getCamera().screenToWorldPoint(tmpScreenPoint1, new M3D.M3DMath.Vector3(0, 0, 0));
                //let tmpWorldPoint2: M3D.M3DMath.Vector3 = this.scene.getCurrentViewPort().getCamera().screenToWorldPoint(tmpScreenPoint2, new M3D.M3DMath.Vector3(0, 0, 0));
                //distance = (tmpWorldPoint1.Sub(tmpWorldPoint2)).Length();
                let size = new M3D.M3DMath.Vector2(0.4, 0.4);
                let commonSize = M3D.Utility.ShapeHelper.getCommonSize(this.scene.getCurrentViewPort(), size);
                distance = commonSize.Length();
                return distance;
            }
            /**
             * @protected
             * @description 设置测量结果单位，一开始是按照mm来算
             * @param unitFlag 单位标志
             * @param value 数值
             * @param unit 单位
             * */
            getMeasureUnit(unitFlag, values, unit) {
                let methodName = this.getMeasureUnit.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                switch (unitFlag) {
                    case 0:
                        unit = "";
                        break;
                    case 1:
                        {
                            unit = "mm"; //毫米
                        }
                        break;
                    case 2:
                        {
                            unit = "cm"; //厘米
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] / 10;
                            }
                        }
                        break;
                    case 3:
                        {
                            unit = "m"; //米
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] / 1000;
                            }
                        }
                        break;
                    case 4:
                        {
                            unit = "in"; //英寸
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] * 0.0393701;
                            }
                        }
                        break;
                    case 5:
                        {
                            unit = "ft"; //英尺
                            for (let i = 0; i < values.length; i++) {
                                values[i] = values[i] * 0.0032808;
                            }
                        }
                        break;
                    default:
                        break;
                }
                unit = " " + unit;
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return unit;
            }
        }
        Actions.MeasureAction = MeasureAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
///<reference path="../MeasureAction.ts"/>
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建角度测量动作
         * @export
         * @class CreateAngleAction
         * @extends {MeasureAction}
         */
        class CreateAngleAction extends Actions.MeasureAction {
            /**
             * @constructor
             * Creates an instance of CreateAngleAction.
             * @memberof CreateAngleAction
             */
            constructor() {
                super();
                this.measureType = M3D.Shapes.MeasureType.LINE_LINE_ANGLE;
            }
            /**
             * @public
             * @description 设置参数
             * @param {number} objID 测量ID
             * @param {M3D.Shapes.MeasureType} type 类型
             * @param {M3D.Shapes.Shape} firstShape 第一个对象
             * @param {M3D.M3DMath.Vector3} firstPosition 选择第一个shape生成的点
             * @param {M3D.Shapes.Shape} secondShape 第二个对象
             * @param {M3D.M3DMath.Vector3} secondPosition 选择第二个shape生成的点
             * @param {M3D.M3DMath.Color} titleColor 标题文本颜色
             * @param {M3D.M3DMath.Color} textColor 结果文本颜色
             * @param {string} fontName 字体
             * @param {number} fontSize 字体大小
             * @param {string} titleFormat 标题样式
             * @param {M3D.M3DMath.Color} titleBackgroundColor 标题背景颜色
             * @param {M3D.M3DMath.Color} resultBackgroundColor 结果背景颜色
             * @param {string} measurementUnit 测量单位
             * @param {string} modelPath1 关联对象path
             * @param {string} modelPath2 关联对象path
             * @param {M3D.M3DMath.Color} fourthPosition 标签所在点
             */
            setParameter(objID, type, firstShape, firstPosition, secondShape, secondPosition, titleColor, textColor, fontName, fontSize, titleFormat, titleBackgroundColor, resultBackgroundColor, measurementUnit, modelPath1, modelPath2, fourthPosition) {
                this.objID = objID;
                this.measureType = type;
                this.firstShape = firstShape;
                this.firstPosition = firstPosition;
                this.secondShape = secondShape;
                this.secondPosition = secondPosition;
                this.titleTextColor = titleColor;
                this.resultTextColor = textColor;
                this.fontName = fontName;
                this.fontSize = fontSize;
                this.titleFormat = titleFormat;
                this.titleBackgroundColor = titleBackgroundColor;
                this.resultBackgroundColor = resultBackgroundColor;
                this.measurementUnit = measurementUnit;
                this.modelPath1 = modelPath1;
                this.modelPath2 = modelPath2;
                this.fourthPosition = fourthPosition;
                return true;
            }
            /**
             * @description 创建测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateAngleAction
             */
            doMeasure() {
                Actions.ActionLogger.ACTIONLOGD("CreateAngleAction::CreateMeasure begin!");
                let state = M3D.Actions.ActionState.Action_Success;
                this.clearMeasure();
                //必须两条线或者面都拾取到之后，再创建对象，再计算距离
                if ((!this.firstPosition && !this.secondPosition)
                    || (this.firstPosition.IsZero() && this.secondPosition.IsZero())) {
                    return state;
                }
                if (this.objID == 0) {
                    this.measure = new M3D.Shapes.MeasureAngle();
                    this.measure.setScene(this.scene);
                }
                else {
                    this.measure = this.scene.getShapes().getShape(this.objID, M3D.Shapes.ShapeType.MEASURE_BASE);
                }
                this.setBaseInfo();
                let bFlag = false;
                state = this.setShapeElement();
                if (state != M3D.Actions.ActionState.Action_Success) {
                    return state;
                }
                if (this.angleLine1Start && this.angleLine1End
                    && this.angleLine2Start && this.angleLine2End
                    && this.lineAngle) {
                    let linesTemp = [];
                    this.createAngleMark(this.angleLine1Start, this.angleLine1End, this.angleLine2Start, this.angleLine2End, this.lineAngle, linesTemp);
                    if (!this.fourthPosition) {
                        this.fourthPosition = (linesTemp[Number.parseInt(linesTemp.length / 2 + "")].Sub(this.angleLine1Start)).Add(linesTemp[Number.parseInt(linesTemp.length / 2 + "")]);
                    }
                    if (linesTemp.length > 0) {
                        for (let i = 0; i < linesTemp.length - 1; i++) {
                            this.setLine(i + "", this.titleBackgroundColor, linesTemp[i], linesTemp[i + 1]);
                        }
                        if ((this.angle > 0 && this.angle < 90.0) || (this.angle > 90.0 && this.angle < 180.0)) {
                            this.setLine(M3D.MeasureNames.LINE_LEADER, this.leaderColor, linesTemp[Number.parseInt(linesTemp.length / 2 + "")].
                                Add(linesTemp[Number.parseInt(linesTemp.length / 2 + "") - 1]).Divide(2), this.fourthPosition);
                        }
                        else if (this.angle - 90 < 0.0000001 || this.angle - 90 > -0.0000001) {
                            //this.setLine(M3D.MeasureNames.MEASURE_LINE_LEADER, this.leaderColor, linesTemp[1], this.fourthPosition);
                            this.setLine(M3D.MeasureNames.LINE_LEADER, this.leaderColor, linesTemp[Number.parseInt(linesTemp.length / 2 + "")].
                                Add(linesTemp[Number.parseInt(linesTemp.length / 2 + "") - 1]).Divide(2), this.fourthPosition);
                        }
                    }
                    //构造投影平面（缺失）
                    //构造显示文本 
                    this.setComText(0, this.angle);
                    let unit = "°";
                    let temptext = new Array();
                    //DOTO
                    if (this.angle > -0.000001) {
                        let tempText = [];
                        let lengthstr = this.angle.toFixed(2).toString();
                        let title = new M3D.Shapes.Texts2D();
                        title.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                        title.setBackground(this.titleBackgroundColor);
                        title.setFontColor(this.titleTextColor);
                        title.setTexts(M3D.Shapes.MeasureDisplayHelper.getTitleText(0, this.measureType));
                        tempText.push(title);
                        let content = new M3D.Shapes.Texts2D();
                        content.setSize(this.fontSize); // ANNOTATION_TEXT_FONT_SIZE;//
                        content.setBackground(M3D.M3DMath.Color.WHITE());
                        content.setFontColor(this.resultTextColor);
                        content.setTexts(lengthstr + unit + " ");
                        tempText.push(content);
                        temptext.push(tempText);
                        //添加Text文本，用于在线视图保存
                        //解决协同中文本读取
                        this.setComText(0, this.angle);
                    }
                    if (temptext.length > 0) { //如果存在点位置，则增加Position
                        this.measure.setPosition(this.fourthPosition);
                    }
                    M3D.Shapes.MeasureDisplayHelper.createNoteTextsImageMultiline(this.scene, temptext, this.fourthPosition, this.measure);
                    this.angleLine1Start = null;
                    this.angleLine1End = null;
                    this.angleLine2Start = null;
                    this.angleLine2End = null;
                    this.lineAngle = 0;
                }
                return state;
            }
            /**
             * @description 创建角度标识线
             * @param line1Start
             * @param line1End
             * @param line2Start
             * @param line2End
             */
            /**
             * @description 创建角度标识线，弧线
             * @protected
             * @param {M3D.M3DMath.Vector3} line1Start 第一条线的起点
             * @param {M3D.M3DMath.Vector3} line1End 第一条线的结束点
             * @param {M3D.M3DMath.Vector3} line2Start 第二条线的起点
             * @param {M3D.M3DMath.Vector3} line2End 第二条线的结束点
             * @param {number} theta 线之间的角度
             * @param {M3D.M3DMath.Vector3[]} lines 线上的所有点
             * @param {number} [fAngleTol=10] 多少个点
             * @memberof CreateAngleAction
             */
            createAngleMark(line1Start, line1End, line2Start, line2End, theta, lines, fAngleTol = 10) {
                Actions.ActionLogger.ACTIONLOGI("CreateAngleAction::createAngleMark begin!");
                if (theta < 0.000001 && theta > -0.000001) {
                    Actions.ActionLogger.ACTIONLOGI("CreateAngleAction::createAngleMark ZERO VECTOR!");
                    return;
                }
                let num = 0;
                if (theta <= (fAngleTol + 1) * 2) {
                    num = 2;
                }
                else {
                    num = M3D.M3DMath.Abs(theta / (fAngleTol - 0.001));
                }
                let line1Length = (line1End.Sub(line1Start)).Length();
                let line2Length = (line2End.Sub(line2Start)).Length();
                let radius = line1Length > line2Length ? line2Length / 3 : line1Length / 3;
                let center = line2Start;
                let perTheta = theta / num;
                let line1Direction = line1End.Sub(line1Start).Normalized();
                let line2Direction = line2End.Sub(line2Start).Normalized();
                let axis = line1Direction.CrossProduct(line2Direction);
                axis = axis.Normalized();
                let cosTheta = line1Direction.DotProduct(line2Direction);
                let startPoint = center.Add(line1Direction.Multiply(radius)); //端点起始点
                lines.push(startPoint);
                for (let i = 0; i < num - 1; i++) {
                    let q = new M3D.M3DMath.Quaternion(perTheta * (i + 1), axis);
                    q.Normalize();
                    let line1DirectionQua = new M3D.M3DMath.Quaternion(0, line1Direction.x, line1Direction.y, line1Direction.z);
                    let tempVectorQuaternion = q.Multiply(line1DirectionQua).Multiply(q.Conjugate());
                    let newVector = new M3D.M3DMath.Vector3(tempVectorQuaternion.x, tempVectorQuaternion.y, tempVectorQuaternion.z);
                    newVector.Normalize();
                    let newPoint = center.Add(newVector.Multiply(radius));
                    lines.push(newPoint);
                }
                let endPoint = center.Add(line2Direction.Multiply(radius)); //端点结束点
                lines.push(endPoint);
                //if (M3D.M3DMath.Abs(theta - 90) < 0.0000001) {
                //    let rightPoint = startPoint.Add(line2Direction.Multiply(radius));//直角
                //    lines.push(rightPoint);
                //}else if (theta < 90 || (theta > 90 && theta < 180)) {
                //    for (let i = 0; i < 4; i++) {
                //        let q = new M3D.M3DMath.Quaternion(perTheta * (i + 1), axis);
                //        q.Normalize();
                //        let line1DirectionQua = new M3D.M3DMath.Quaternion(0, line1Direction.x,
                //            line1Direction.y, line1Direction.z);
                //        let tempVectorQuaternion = q.Multiply(line1DirectionQua).Multiply(q.Conjugate());
                //        let newVector = new M3D.M3DMath.Vector3(tempVectorQuaternion.x,
                //            tempVectorQuaternion.y, tempVectorQuaternion.z);
                //        newVector.Normalize();
                //        let newPoint = center.Add(newVector.Multiply(radius));
                //        lines.push(newPoint);
                //    }
                //}
                //if (theta >= 0) {
                //    let endPoint = center.Add(line2Direction.Multiply(radius));//端点结束点
                //    lines.push(endPoint);
                //} else {
                //    let endPoint = center.Sub(line2Direction.Multiply(radius));//端点结束点
                //    lines.push(endPoint);
                //}
                Actions.ActionLogger.ACTIONLOGI("CreateAngleAction::createAngleMark end!");
            }
        }
        Actions.CreateAngleAction = CreateAngleAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建面面角度测量动作
         * @export
         * @class CreateFaceFaceAngleAction
         * @extends {CreateAngleAction}
         */
        class CreateFaceFaceAngleAction extends Actions.CreateAngleAction {
            /**
             * @constructor
             * Creates an instance of CreateFaceFaceAngleAction.
             * @memberof CreateFaceFaceAngleAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建角度对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateFaceFaceAngleAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                let firstFaceFeatrue = null;
                //let firstFinished: boolean = false;
                if (this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.FACE) {
                    let firstFace = this.firstShape;
                    let firstGeo = firstFace.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getFaceType(firstFace);
                        //if (type === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstFace.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        firstFaceFeatrue = firstGeo.clone();
                        let firstWorldMatrix = firstFace.getBody().getModel().getWorldTransform().ToMatrix3x4();
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstFaceFeatrue, firstWorldMatrix);
                        let firstNormal = firstFaceFeatrue.getNormal();
                        //构造第一个面标识四边形
                        let firstProjectPnt = this.firstPosition.Clone();
                        let firstBoundingBox = firstFace.getBoundingBox();
                        //let firstDistance = firstBoundingBox.max.Sub(firstBoundingBox.min).Length() / 20;
                        //let firstDistance = firstBoundingBox.getMinLength() / 10;
                        let firstDistance = this.getSquareBoxLineLength();
                        let firstOriginVec = firstFaceFeatrue.getOrigin();
                        let firstFaceDirection = firstProjectPnt.Sub(firstOriginVec);
                        let pointArray = [];
                        this.getPointSquareBoxMark(pointArray, firstProjectPnt, firstFaceDirection, firstNormal, firstDistance);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.squareBoxColor, pointArray[0].Sub(firstProjectPnt), pointArray[1].Sub(firstProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.squareBoxColor, pointArray[2].Sub(firstProjectPnt), pointArray[3].Sub(firstProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.squareBoxColor, pointArray[0].Sub(firstProjectPnt), pointArray[2].Sub(firstProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.squareBoxColor, pointArray[3].Sub(firstProjectPnt), pointArray[1].Sub(firstProjectPnt));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, firstProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, firstProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, firstProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, firstProjectPnt, true, true, false);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    if (!this.secondShape) {
                        return state;
                    }
                    //firstFinished = true;
                }
                if (this.firstShape && this.secondShape && this.firstShape !== this.secondShape
                    && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.FACE
                    && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE) {
                    let firstFace = this.firstShape;
                    let firstGeo = firstFace.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getFaceType(firstFace);
                        //if (type === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstFace.getGeometryFeature();
                        //}
                    }
                    let secondFace = this.secondShape;
                    let secondGeo = secondFace.getGeometryFeature();
                    if (!secondGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getFaceType(secondFace);
                        //if (type === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        //    secondGeo = secondFace.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE
                        && secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        firstFaceFeatrue = firstGeo.clone();
                        let firstWorldMatrix = firstFace.getBody().getModel().getWorldTransform().ToMatrix3x4();
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstFaceFeatrue, firstWorldMatrix);
                        let secondFaceFeatrue = secondGeo.clone();
                        let secondWorldMatrix = secondFace.getBody().getModel().getWorldTransform().ToMatrix3x4();
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(secondFaceFeatrue, secondWorldMatrix);
                        let firstBox = firstFace.getBoundingBox().Transformed(firstWorldMatrix);
                        let secondBox = secondFace.getBoundingBox().Transformed(secondWorldMatrix);
                        firstBox.Merge(secondBox);
                        let boxLength = firstBox.Length() * 1.5;
                        let angle = 0.0;
                        //获取角度接口
                        let firstNormal = firstFaceFeatrue.getNormal();
                        let secondNormal = secondFaceFeatrue.getNormal();
                        angle = firstNormal.Angle(secondNormal);
                        if (M3D.M3DMath.Abs(angle - 180) < 0.001 || M3D.M3DMath.Abs(angle) < 0.001) {
                            Actions.ActionLogger.ACTIONLOGW(Actions.ActionNames.ANGLEOFZERO, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        //构造第二个面标识四边形
                        let secondProjectPnt = this.secondPosition.Clone();
                        let secondBoundingBox = secondFace.getBoundingBox();
                        //let secondDistance = secondBoundingBox.max.Sub(secondBoundingBox.min).Length() / 20;
                        //let secondDistance = secondBoundingBox.getMinLength() / 10;
                        let secondDistance = this.getSquareBoxLineLength();
                        let secondOriginVec = secondFaceFeatrue.getOrigin();
                        let secondFaceDirection = secondProjectPnt.Sub(secondOriginVec);
                        let pointArray = [];
                        this.getPointSquareBoxMark(pointArray, secondProjectPnt, secondFaceDirection, secondNormal, secondDistance);
                        let centerPnt = new M3D.M3DMath.Vector3(); //交点
                        if (M3D.M3DMath.Abs(angle - 90) < 0.001) {
                            //创建示意线
                            let face1Plane = new M3D.M3DMath.Plane(firstFaceFeatrue.getNormal(), this.firstPosition);
                            let footPnt = face1Plane.Project(this.secondPosition);
                            this.setLine(M3D.MeasureNames.MEASURE_ANGLE_LINE2, this.markingColor, footPnt, this.secondPosition);
                            this.setLine(M3D.MeasureNames.MEASURE_ANGLE_LINE1, this.markingColor, footPnt, this.firstPosition);
                            centerPnt = footPnt;
                        }
                        else {
                            //创建示意线
                            let face1Plane = new M3D.M3DMath.Plane(firstFaceFeatrue.getNormal(), this.firstPosition);
                            let face2Plane = new M3D.M3DMath.Plane(secondFaceFeatrue.getNormal(), this.secondPosition);
                            let firstPnt = face1Plane.Project(this.secondPosition);
                            let secondRay = new M3D.M3DMath.Ray(this.secondPosition, secondFaceFeatrue.getNormal());
                            let secondPnt = new M3D.M3DMath.Vector3();
                            secondPnt = face1Plane.GetInsectPnt1(secondRay, secondPnt);
                            if (secondPnt.Equals(M3D.M3DMath.Vector3.ZERO())) {
                                secondRay.Define(this.secondPosition, secondFaceFeatrue.getNormal().Nagative());
                                secondPnt = face1Plane.GetInsectPnt1(secondRay, secondPnt);
                                if (secondPnt.Equals(M3D.M3DMath.Vector3.ZERO())) {
                                    Actions.ActionLogger.ACTIONLOGW(Actions.ActionNames.ANGLEOFZERO, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                                    return M3D.Actions.ActionState.Action_Error;
                                }
                            }
                            let firstRay = new M3D.M3DMath.Ray(secondPnt, firstPnt.Sub(secondPnt));
                            secondPnt = new M3D.M3DMath.Vector3();
                            secondPnt = face2Plane.GetInsectPnt1(firstRay, secondPnt);
                            if (secondPnt.Equals(M3D.M3DMath.Vector3.ZERO())) {
                                firstRay.Define(secondPnt, secondPnt.Sub(firstPnt));
                                secondPnt = face2Plane.GetInsectPnt1(firstRay, secondPnt);
                                if (secondPnt.Equals(M3D.M3DMath.Vector3.ZERO())) {
                                    Actions.ActionLogger.ACTIONLOGW(Actions.ActionNames.ANGLEOFZERO, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                                    return M3D.Actions.ActionState.Action_Error;
                                }
                            }
                            let lineLength1 = this.firstPosition.Sub(secondPnt).Length();
                            let lineLength2 = this.secondPosition.Sub(secondPnt).Length();
                            let lineLength = lineLength1 > lineLength2 ? lineLength2 : lineLength1;
                            if (lineLength > boxLength) {
                                let pt1, pt2;
                                let line1Direction = secondPnt.Sub(this.secondPosition).Normalized();
                                let line2Direction = secondPnt.Sub(this.firstPosition).Normalized();
                                if (lineLength1 > lineLength2) {
                                    pt1 = this.firstPosition.Add(line1Direction.Multiply(boxLength));
                                    let n = line1Direction;
                                    let t = (pt1.DotProduct(n) - this.secondPosition.DotProduct(n)) / (line2Direction.DotProduct(n));
                                    pt2 = this.secondPosition.Add(line1Direction.Multiply(t));
                                }
                                else {
                                    pt1 = this.secondPosition.Add(line2Direction.Multiply(boxLength));
                                    let n = line2Direction;
                                    let t = (pt1.DotProduct(n) - this.firstPosition.DotProduct(n)) / (line1Direction.DotProduct(n));
                                    pt2 = this.firstPosition.Add(line1Direction.Multiply(t));
                                }
                                secondPnt = pt1.Add(pt2).Divide(2);
                            }
                            this.setLine(M3D.MeasureNames.MEASURE_ANGLE_LINE2, this.markingColor, this.secondPosition, secondPnt);
                            this.setLine(M3D.MeasureNames.MEASURE_ANGLE_LINE1, this.markingColor, this.firstPosition, secondPnt);
                            centerPnt = secondPnt;
                        }
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5, this.squareBoxColor, pointArray[0].Sub(secondProjectPnt), pointArray[1].Sub(secondProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6, this.squareBoxColor, pointArray[2].Sub(secondProjectPnt), pointArray[3].Sub(secondProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7, this.squareBoxColor, pointArray[0].Sub(secondProjectPnt), pointArray[2].Sub(secondProjectPnt));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8, this.squareBoxColor, pointArray[3].Sub(secondProjectPnt), pointArray[1].Sub(secondProjectPnt));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5, secondProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6, secondProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7, secondProjectPnt, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8, secondProjectPnt, true, true, false);
                        //if (!this.fourthPosition) {
                        //    this.fourthPosition = centerPnt;
                        //}
                        this.angleLine1Start = centerPnt;
                        this.angleLine1End = this.firstPosition;
                        this.angleLine2Start = centerPnt;
                        this.angleLine2End = this.secondPosition;
                        this.lineAngle = this.secondPosition.Sub(centerPnt).Angle(this.firstPosition.Sub(centerPnt));
                        if (this.lineAngle > 90 && angle < 90) {
                            angle = 180 - angle;
                        }
                        else if (this.lineAngle < 90 && angle > 90) {
                            angle = 180 - angle;
                        }
                        this.angle = angle;
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return state;
            }
            /**
             * @description 通过json转为measure
             * @beta
             * @param jsonString
             */
            fromJson(jsonString) {
                return false;
            }
            /**
             * @description 将测量转为json
             * @beta
             * */
            toJson() {
                return null;
            }
        }
        Actions.CreateFaceFaceAngleAction = CreateFaceFaceAngleAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建线面角度测量Action
         * @export
         * @class CreateLineFaceAngleAction
         * @extends {CreateAngleAction}
         */
        class CreateLineFaceAngleAction extends Actions.CreateAngleAction {
            /**
             * @constructor
             * Creates an instance of CreateLineFaceAngleAction.
             * @memberof CreateLineFaceAngleAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建角度测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateLineFaceAngleAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                let firstLineFeatrue = null;
                //let firstFinished: boolean = false;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE ||
                    this.isSectionEdgeEnable(true, this.firstShape, null))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let pntStartPoint = firstLineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = firstLineFeatrue.getEndPoint(); //!< Line终点
                        //this.setLine(M3D.MeasureNames.MEASURE_LINE_PRE, this.markingColor, pntStartPoint, pntEndPoint);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    if (!this.secondShape) {
                        return state;
                    }
                    //firstFinished = true;
                }
                if (this.firstShape && this.secondShape
                    && ((this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE ||
                        this.isSectionEdgeEnable(true, this.firstShape, null))
                        && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE)) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    firstLineFeatrue = firstGeo.clone();
                    let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                    //几何属性变化
                    M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                    let secondFace = this.secondShape;
                    let secondGeo = secondFace.getGeometryFeature();
                    if (!secondGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getFaceType(secondFace);
                        //if (type === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        //    secondGeo = secondFace.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE
                        && secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        let secondFaceFeatrue = secondGeo.clone();
                        let secondWorldMatrix = secondFace.getBody().getModel().getWorldTransform().ToMatrix3x4();
                        M3D.Shapes.ShapeHelper.transform(secondFaceFeatrue, secondWorldMatrix);
                        let box = secondFace.getBoundingBox();
                        //let distance = box.max.Sub(box.min).Length() / 20;
                        //let distance = box.getMinLength() / 10;
                        let distance = this.getSquareBoxLineLength();
                        let projectPnt = this.secondPosition.Clone();
                        let normalVec = secondFaceFeatrue.getNormal();
                        let originVec = secondFaceFeatrue.getOrigin();
                        let plane = new M3D.M3DMath.Plane(normalVec, originVec);
                        projectPnt = plane.Project(projectPnt);
                        let direction = projectPnt.Sub(originVec);
                        let pointArray = [];
                        this.getPointSquareBoxMark(pointArray, projectPnt, direction, normalVec, distance);
                        let angle = 0.0;
                        //获取角度
                        let normal = secondFaceFeatrue.getNormal();
                        let lineDirection = firstLineFeatrue.getDirection();
                        let sinTheta = M3D.M3DMath.Abs(normal.DotProduct(lineDirection) /
                            (normal.Length() * lineDirection.Length()));
                        angle = M3D.M3DMath.Asin(sinTheta);
                        if (M3D.M3DMath.Abs(angle - 180) < 0.001 || M3D.M3DMath.Abs(angle) < 0.001) {
                            Actions.ActionLogger.ACTIONLOGW(Actions.ActionNames.ANGLEOFZERO, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        this.angle = angle;
                        let facePlane = new M3D.M3DMath.Plane(normal, originVec);
                        let line1Start = firstLineFeatrue.getStartPoint();
                        let line1End = firstLineFeatrue.getEndPoint();
                        let centerPnt = (line1Start.Add(line1End)).Divide(2);
                        let lineRay = new M3D.M3DMath.Ray(line1Start, line1Start.Sub(line1End));
                        let insectPnt = new M3D.M3DMath.Vector3(); //交点
                        let projectPnt1 = new M3D.M3DMath.Vector3();
                        if (!facePlane.GetInsectPnt(lineRay, insectPnt)) {
                            lineRay.Define(line1Start, line1End.Sub(line1Start));
                            if (!facePlane.GetInsectPnt(lineRay, insectPnt)) {
                                Actions.ActionLogger.ACTIONLOGW(Actions.ActionNames.ANGLEOFZERO, Actions.ActionNames.ANGLEMEASUREACTION, methodName);
                                return M3D.Actions.ActionState.Action_Error;
                            }
                        }
                        insectPnt = facePlane.GetInsectPnt1(lineRay, insectPnt);
                        if (!(this.pntInSegment(insectPnt, line1Start, line1End, projectPnt1))) {
                            //let line = new Line3D(projectPnt, insectPnt);
                            //line.setName("dottedLine");
                            //line.setColor(MeasureFactory.ExLineColor().Clone());
                            //measure.AddLine(line);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, projectPnt1, insectPnt, M3D.Shapes.CurvePatternMode.DashedLine);
                        }
                        //if (!this.fourthPosition) {
                        //    this.fourthPosition = insectPnt;
                        //}
                        //计算两条延长线
                        let lineDirection1 = line1Start.Sub(line1End).Divide(50);
                        let dis1 = line1End.Sub(projectPnt1).Length();
                        let dis2 = line1Start.Sub(projectPnt1).Length();
                        if (dis1 >= dis2) {
                            lineDirection1 = line1End.Sub(line1Start).Divide(50);
                        }
                        //设置面标识线框
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.markingColor, pointArray[2].Sub(this.secondPosition), pointArray[3].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[2].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.markingColor, pointArray[3].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.secondPosition, true, true, false);
                        //计算两条示意线
                        let line1EndPosition = lineDirection1.Multiply(distance / 2).Add(projectPnt1);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, projectPnt1.Sub(projectPnt1), line1EndPosition.Sub(projectPnt1));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, projectPnt1, true, true, false);
                        this.setLine(M3D.MeasureNames.MEASURE_ANGLE_LINE2, this.markingColor, insectPnt, this.secondPosition);
                        //设置交点
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_PROJECT, insectPnt);
                        this.angleLine1Start = insectPnt;
                        this.angleLine1End = line1EndPosition;
                        this.angleLine2Start = insectPnt;
                        this.angleLine2End = this.secondPosition;
                        this.lineAngle = line1EndPosition.Sub(projectPnt1).Angle(this.secondPosition.Sub(insectPnt));
                        //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return state;
            }
            /**
             * @beta
             * @description 通过json转为measure
             * @param {string} jsonString
             * @return {M3D.Shapes.Measure}
             * @memberof CreateLineFaceAngleAction
             */
            fromJson(jsonString) {
                return false;
            }
            /**
             * @beta
             * @description 将测量转为json
             * */
            toJson() {
                return null;
            }
        }
        Actions.CreateLineFaceAngleAction = CreateLineFaceAngleAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建线线角度测量Action
         * @export
         * @class CreateLineLineAngleAction
         * @extends {CreateAngleAction}
         */
        class CreateLineLineAngleAction extends Actions.CreateAngleAction {
            /**
             * @constructor
             * Creates an instance of CreateLineLineAngleAction.
             * @memberof CreateLineLineAngleAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建角度测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateLineLineAngleAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                let firstLineFeatrue = null;
                //let firstFinished: boolean = false;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE ||
                    this.isSectionEdgeEnable(true, this.firstShape, this.secondShape))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let pntStartPoint = firstLineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = firstLineFeatrue.getEndPoint(); //!< Line终点
                        //this.setLine(M3D.MeasureNames.MEASURE_LINE_PRE, this.markingColor, pntStartPoint, pntEndPoint);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    if (!this.secondShape) {
                        return state;
                    }
                }
                if (this.firstShape && this.secondShape && this.firstShape !== this.secondShape
                    && ((this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                        && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE)
                        || this.isSectionEdgeEnable(false, this.firstShape, this.secondShape))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    let secondEdge = this.secondShape;
                    let secondGeo = secondEdge.getGeometryFeature();
                    if (!secondGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //暂时没有重新生成GEO的方法
                        //let type = M3D.Shapes.ShapeHelper.getLineType(secondEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    secondGeo = secondEdge.getGeometryFeature().clone();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE
                        && secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let secondLineFeatrue = secondGeo.clone();
                        let secondWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, secondWorldMatrix);
                        let angle = 0.0;
                        //获取角度接口（目前缺失）
                        let firstStartPoint = firstLineFeatrue.getStartPoint(); //!< Line起点
                        let firstEndPoint = firstLineFeatrue.getEndPoint(); //!< Line终点
                        let line1Center = firstStartPoint.Add(firstEndPoint).Divide(2);
                        let secondStartPoint = secondLineFeatrue.getStartPoint(); //!< Line起点
                        let secondEndPoint = secondLineFeatrue.getEndPoint(); //!< Line起点
                        let line2Center = secondStartPoint.Add(secondEndPoint).Divide(2);
                        let distance = 0;
                        let projectPnt1 = new M3D.M3DMath.Vector3(); //交点
                        let projectPnt2 = new M3D.M3DMath.Vector3();
                        let parallel = true; //是否平行
                        let result = this.lineLineDistance(firstLineFeatrue, secondLineFeatrue, distance, projectPnt1, projectPnt2, parallel);
                        distance = result[0];
                        parallel = result[1];
                        if (parallel || distance > 0.001) {
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        let line1Direction = firstEndPoint.Sub(firstStartPoint);
                        let line2Direction = secondEndPoint.Sub(secondStartPoint);
                        //if (!this.fourthPosition) {
                        //    this.fourthPosition = projectPnt1;
                        //}
                        let projecPnt1 = new M3D.M3DMath.Vector3();
                        let projecPnt2 = new M3D.M3DMath.Vector3();
                        // 计算并创建两条延长线,若延长中较长的线大于两条线包围盒的1.5倍，则对线进行切割
                        let box1 = new M3D.M3DMath.BoundingBox(new M3D.M3DMath.Vector3(Math.min(firstStartPoint.x, firstEndPoint.x), Math.min(firstStartPoint.y, firstEndPoint.y), Math.min(firstStartPoint.z, firstEndPoint.z)), new M3D.M3DMath.Vector3(Math.max(firstStartPoint.x, firstEndPoint.x), Math.max(firstStartPoint.y, firstEndPoint.y), Math.max(firstStartPoint.z, firstEndPoint.z)));
                        let box2 = new M3D.M3DMath.BoundingBox(new M3D.M3DMath.Vector3(Math.min(secondStartPoint.x, secondEndPoint.x), Math.min(secondStartPoint.y, secondEndPoint.y), Math.min(secondStartPoint.z, secondEndPoint.z)), new M3D.M3DMath.Vector3(Math.max(secondStartPoint.x, secondEndPoint.x), Math.max(secondStartPoint.y, secondEndPoint.y), Math.max(secondStartPoint.z, secondEndPoint.z)));
                        box1.Merge(box2);
                        let boxLength = box1.Length() * 1.5;
                        let tmp1 = false;
                        if (!(this.pntInSegment(projectPnt1, firstStartPoint, firstEndPoint, projecPnt1))) {
                            tmp1 = true;
                        }
                        let tmp2 = false;
                        if (!(this.pntInSegment(projectPnt1, secondStartPoint, secondEndPoint, projecPnt2))) {
                            tmp2 = true;
                        }
                        let projectPnt1Origen = projectPnt1;
                        if (tmp1) {
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, projecPnt1, projectPnt1, M3D.Shapes.CurvePatternMode.DashedLine);
                        }
                        if (tmp2) {
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND2, this.extendLineColor, projecPnt2, projectPnt1, M3D.Shapes.CurvePatternMode.DashedLine);
                        }
                        //if (tmp1 && tmp2) {
                        //    let lineLength1 = projecPnt1.Sub(projectPnt1).LengthSquared();
                        //    let lineLength2 = projecPnt2.Sub(projectPnt1).LengthSquared();
                        //    let tmp1Direction = projecPnt1.Sub(projectPnt1).Normalized();
                        //    let tmp2Direction = projecPnt2.Sub(projectPnt1).Normalized();
                        //    let lineLength = lineLength1 > lineLength2 ? lineLength2 : lineLength1;
                        //    if (lineLength > boxLength * boxLength) {
                        //        let pt1: M3D.M3DMath.Vector3, pt2: M3D.M3DMath.Vector3;
                        //        if (lineLength1 > lineLength2) {
                        //            pt1 = projecPnt1.Add(tmp1Direction.Multiply(boxLength));
                        //            let n: M3D.M3DMath.Vector3 = tmp1Direction;
                        //            let t = (pt1.DotProduct(n) - projecPnt2.DotProduct(n)) / (tmp2Direction.DotProduct(n));
                        //            pt2 = projecPnt2.Add(tmp2Direction.Multiply(t));
                        //        } else {
                        //            pt1 = projecPnt2.Add(tmp2Direction.Multiply(boxLength));
                        //            let n = tmp2Direction;
                        //            let t = (pt1.DotProduct(n) - projecPnt1.DotProduct(n)) / (tmp1Direction.DotProduct(n));
                        //            pt2 = projecPnt1.Add(tmp1Direction.Multiply(t));
                        //        }
                        //        projectPnt1 = pt1.Add(pt2).Divide(2);
                        //    }
                        //    this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.markingColor, projecPnt1, projectPnt1);
                        //    this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND2, this.markingColor, projecPnt2, projectPnt1);
                        //}
                        let direction = (firstStartPoint.Sub(firstEndPoint)).Divide(5);
                        let dis1 = (firstEndPoint.Sub(projecPnt1)).Length();
                        let dis2 = (firstStartPoint.Sub(projecPnt1)).Length();
                        let dis3 = (projecPnt1.Sub(projectPnt1Origen)).Length();
                        if (dis1 >= dis2) {
                            direction = (firstEndPoint.Sub(firstStartPoint)).Divide(5);
                        }
                        let direction1 = (secondStartPoint.Sub(secondEndPoint)).Divide(5);
                        let dis11 = (secondEndPoint.Sub(projecPnt2)).Length();
                        let dis22 = (secondStartPoint.Sub(projecPnt2)).Length();
                        let dis33 = (projecPnt2.Sub(projectPnt1Origen)).Length();
                        if (dis11 >= dis22) {
                            direction1 = (secondEndPoint.Sub(secondStartPoint)).Divide(5);
                        }
                        let tmpResult = 1;
                        let length1 = (direction.Multiply(5)).Length();
                        let length2 = (direction1.Multiply(5)).Length();
                        tmpResult = length1 / length2;
                        if (tmpResult > 1) {
                            direction = direction.Divide(tmpResult);
                        }
                        else {
                            direction1 = direction1.Multiply(tmpResult);
                        }
                        // 计算两条示意线
                        let markingLine1Start = projecPnt1;
                        let markingLine1End = direction.NormalizeED().Multiply(this.getSquareBoxLineLength() * 3).Add(projecPnt1);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, markingLine1Start.Sub(markingLine1Start), markingLine1End.Sub(markingLine1Start));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, markingLine1Start, true, true, false);
                        // 计算两条示意线
                        let markingLine2Start = projecPnt2;
                        let markingLine2End = direction1.NormalizeED().Multiply(this.getSquareBoxLineLength() * 3).Add(projecPnt2);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_2, this.markingColor, markingLine2Start.Sub(markingLine2Start), markingLine2End.Sub(markingLine2Start));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_2, markingLine2Start, true, true, false);
                        if (markingLine1Start.Equals(projectPnt1Origen)) {
                            line1Direction = markingLine1End.Sub(projectPnt1Origen);
                        }
                        else if (markingLine1End.Equals(projectPnt1Origen)) {
                            line1Direction = markingLine1Start.Sub(projectPnt1Origen);
                        }
                        else {
                            line1Direction = markingLine1End.Sub(projectPnt1Origen);
                        }
                        if (markingLine2Start.Equals(projectPnt1Origen)) {
                            line2Direction = markingLine2End.Sub(projectPnt1Origen);
                        }
                        else if (markingLine2End.Equals(projectPnt1Origen)) {
                            line2Direction = markingLine2Start.Sub(projectPnt1Origen);
                        }
                        else {
                            line2Direction = markingLine2End.Sub(projectPnt1Origen);
                        }
                        line1Direction.Normalize();
                        line2Direction.Normalize();
                        let angleLine = line1Direction.Angle(line2Direction);
                        length1 = (projectPnt1.Sub(markingLine1End)).Length();
                        length2 = (projectPnt1.Sub(markingLine2End)).Length();
                        let linesTemp = [];
                        let tmpLineDir1 = projectPnt1.Sub(line1Center);
                        let tmpLineDir2 = projectPnt1.Sub(line2Center);
                        let angleTmpLine = tmpLineDir1.Angle(tmpLineDir2);
                        this.angleLine1Start = projectPnt1;
                        this.angleLine1End = markingLine1End;
                        this.angleLine2Start = projectPnt1;
                        this.angleLine2End = markingLine2End;
                        this.lineAngle = angleTmpLine;
                        this.angle = angleTmpLine;
                        //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
            /**
             * @beta
             * @description 通过json转为measure
             * @param {string} jsonString
             * @return {M3D.Shapes.Measure}
             * @memberof CreateLineLineAngleAction
             */
            fromJson(jsonString) {
                return false;
            }
            /**
             * @beta
             * @description 将测量转为json
             * @return {M3D.Shapes.MeasureAngle}
             * @memberof CreateLineLineAngleAction
             */
            toJson() {
                return null;
            }
        }
        Actions.CreateLineLineAngleAction = CreateLineLineAngleAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建圆测量基类
         * @export
         * @class CreateCircleAction
         * @extends {MeasureAction}
         */
        class CreateCircleAction extends Actions.MeasureAction {
            /**
             * @constructor
             * Creates an instance of CreateCircleAction.
             * @memberof CreateCircleAction
             */
            constructor() {
                super();
                this.measure = null;
                this.measureType = M3D.Shapes.MeasureType.BASE_MEASURE;
                this.objID = 0;
                this.parentModelID = 0;
                this.measureType = M3D.Shapes.MeasureType.DIAMETRE;
                //this.titleTextColor = M3D.M3DMath.Color.YELLOW().Clone();
                this.fontSize = M3D.ANNOTATION_TEXT_FONT_SIZE;
                this.fontName = "";
                //this.titleBackgroundColor = M3D.M3DMath.Color.YELLOW().Clone();
                //this.resultBackgroundColor = M3D.M3DMath.Color.YELLOW().Clone();
                //this.resultTextColor = M3D.M3DMath.Color.YELLOW().Clone();
                this.scene = null;
                this.firstPosition = M3D.M3DMath.Vector3.ZERO().Clone();
                this.secondPosition = M3D.M3DMath.Vector3.ZERO().Clone();
                this.thirdPosition = M3D.M3DMath.Vector3.ZERO().Clone();
                this.fourthPosition = M3D.M3DMath.Vector3.ZERO().Clone();
                //this.markingColor = M3D.M3DMath.Color.RED();
                //this.leaderColor = M3D.M3DMath.Color.BLACK();
                //this.squareBoxColor = M3D.M3DMath.Color.YELLOW().Clone();
            }
            /**
             * @public
             * @description 设置参数
             * @param objID 测量ID
             * @param type 类型
             * @param firstShape 第一个对象
             * @param firstPosition 选择第一个shape生成的点
             * @param secondShape 第二个对象
             * @param secondPosition 选择第二个shape生成的点
             * @param thirdShape 第三个对象
             * @param thirdPosition 选择第三个shape生成的点
             * @param titleColor 标题文本颜色
             * @param textColor 结果文本颜色
             * @param fontName 字体
             * @param fontSize 字体大小
             * @param titleFormat 标题样式
             * @param titleBackgroundColor 标题背景颜色
             * @param resultBackgroundColor 结果背景颜色
             * @param measurementUnit 测量单位
             * @param modelPath1 关联对象path
             * @param modelPath2 关联对象path
             * @param modelPath3 关联对象path
             * @param fourthPosition 标签所在点
             */
            setParameter(objID, type, firstShape, firstPosition, secondShape, secondPosition, thirdShape, thirdPosition, titleColor, textColor, fontName, fontSize, titleFormat, titleBackgroundColor, resultBackgroundColor, measurementUnit, modelPath1, modelPath2, modelPath3, fourthPosition) {
                this.objID = objID;
                this.measureType = type;
                this.firstShape = firstShape;
                this.firstPosition = firstPosition;
                this.secondShape = secondShape;
                this.secondPosition = secondPosition;
                this.thirdShape = thirdShape;
                this.thirdPosition = thirdPosition;
                this.titleTextColor = titleColor;
                this.resultTextColor = textColor;
                this.fontName = fontName;
                this.fontSize = fontSize;
                this.titleFormat = titleFormat;
                this.titleBackgroundColor = titleBackgroundColor;
                this.resultBackgroundColor = resultBackgroundColor;
                this.measurementUnit = measurementUnit;
                this.modelPath1 = modelPath1;
                this.modelPath2 = modelPath2;
                this.modelPath3 = modelPath3;
                this.fourthPosition = fourthPosition;
                return true;
            }
            /**
             * @description 添加点
             * @protected
             * @param {string} name
             * @param {M3D.M3DMath.Vector3} position
             * @param {number} [size]
             * @memberof CreateCircleAction
             */
            setPoint(name, position, size) {
                let methodName = this.setPoint.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let bFlag = false;
                let points = this.measure.getPointList();
                for (let i = 0; i < points.length; i++) {
                    if (points[i].getName() == name) {
                        bFlag = true;
                        points[i].setPosition(position);
                        if (points[i] instanceof M3D.Shapes.ImagePoint) {
                            points[i].getImage().setPosition(position);
                        }
                        break;
                    }
                }
                if (!bFlag) {
                    let point = new M3D.Shapes.ImagePoint();
                    point.setName(name);
                    point.setPosition(position);
                    point.setScene(this.scene);
                    if (name === M3D.MeasureNames.MEASURE_POINT_CENTER || name === M3D.MeasureNames.MEASURE_POINT_CENTER2) {
                        point.setPointType(M3D.Shapes.ImagePointType.ImagePath7);
                    }
                    else {
                        point.setPointType(M3D.Shapes.ImagePointType.ImagePath3);
                    }
                    if (size) {
                        point.setSize(size);
                    }
                    this.measure.addPoint(point);
                    let pntImage = new M3D.Shapes.Image(position, new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //pntImage.setPosition(position);
                    //let pointSize = M3D.Utility.ShapeHelper.getCommonSize(this.scene.getViewPorts().getViewport(0), new M3D.M3DMath.Vector2(point.getSize(), point.getSize()));
                    //point.getImage().setSize(pointSize);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
            }
            /**
             * @protected
             * @description 执行测量
             * @returns {M3D.Actions.ActionState}
             * */
            doMeasure() {
                Actions.ActionLogger.ACTIONLOGD("CreateAngleAction::CreateMeasure begin!");
                let state = M3D.Actions.ActionState.Action_Success;
                this.clearMeasure();
                ////必须两条线或者面都拾取到之后，再创建对象，再计算距离
                //if ((!this.firstPosition && !this.secondPosition)
                //    || (this.firstPosition.IsZero() && this.secondPosition.IsZero())) {
                //    return state;
                //}
                if (this.objID == 0) {
                    this.measure = new M3D.Shapes.MeasureDiameter();
                    this.measure.setScene(this.scene);
                }
                else {
                    this.measure = this.scene.getShapes().getShape(this.objID, M3D.Shapes.ShapeType.MEASURE_BASE);
                }
                this.setBaseInfo();
                let bFlag = false;
                state = this.setShapeElement();
                if (state != M3D.Actions.ActionState.Action_Success) {
                    return state;
                }
                if (this.measureType === M3D.Shapes.MeasureType.DIAMETRE
                    || this.measureType === M3D.Shapes.MeasureType.RADIUS
                    || this.measureType === M3D.Shapes.MeasureType.CENTER_CENTER_DISTANCE
                    || this.measureType === M3D.Shapes.MeasureType.SHAFT_SHAFT_DISTANCE) {
                    //获取引线
                    //if (!this.fourthPosition) {
                    if (this.distanceLineStartPosition && this.distanceLineEndPosition) {
                        let results = this.getLeaderPoints(this.distanceLineStartPosition, this.distanceLineEndPosition);
                        this.leaderStartPosition = results[0];
                        this.leaderEndPosition = results[1];
                        //if (this.measureType === M3D.Shapes.MeasureType.RADIUS) {
                        //    this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.distanceLineStartPosition);
                        //} else {
                        //    this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.distanceLineStartPosition);
                        //    this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_END, this.distanceLineEndPosition);
                        //}
                        // 添加连接线和引出线
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_DISTANCE, this.titleBackgroundColor, this.distanceLineStartPosition, this.distanceLineEndPosition);
                        if (this.leaderStartPosition && this.leaderEndPosition) {
                            if (this.measureType === M3D.Shapes.MeasureType.RADIUS || this.measureType === M3D.Shapes.MeasureType.DIAMETRE) {
                                this.setLine(M3D.MeasureNames.LINE_LEADER, this.leaderColor, this.distanceLineStartPosition, this.leaderEndPosition);
                            }
                            else {
                                this.setLine(M3D.MeasureNames.LINE_LEADER, this.leaderColor, this.leaderStartPosition, this.leaderEndPosition);
                            }
                        }
                        if (this.distance > -0.000001) {
                            //添加Text文本，用于在线视图保存
                            //解决协同中文本读取
                            this.setComText(0, this.distance);
                        }
                        this.distances = [this.distance];
                        let unit = "";
                        unit = this.getMeasureUnit(Number(this.measurementUnit), this.distances, unit);
                        //let temptext: Array<Array<M3D.Shapes.Texts2D>> = new Array();
                        let temptext = [];
                        //DOTO
                        if (this.distances[0] > -0.000001) {
                            let tempText = [];
                            let lengthstr = this.distances[0].toFixed(2).toString();
                            let title = new M3D.Shapes.Texts2D();
                            title.setSize(this.fontSize);
                            title.setBackground(this.titleBackgroundColor);
                            title.setFontColor(this.titleTextColor);
                            title.setTexts(M3D.Shapes.MeasureDisplayHelper.getTitleText(0, this.measureType));
                            tempText.push(title);
                            let content = new M3D.Shapes.Texts2D();
                            content.setSize(this.fontSize);
                            content.setBackground(this.resultBackgroundColor);
                            content.setFontColor(this.resultTextColor);
                            content.setTexts(M3D.MeasureNames.MEASURE_APPROXIMATE_SYMBOL + lengthstr + unit + " ");
                            tempText.push(content);
                            temptext.push(tempText);
                        }
                        if (temptext.length > 0) { //如果存在点位置，则增加Position
                            this.measure.setPosition(this.leaderEndPosition);
                        }
                        M3D.Shapes.MeasureDisplayHelper.createNoteTextsImageMultiline(this.scene, temptext, this.leaderEndPosition, this.measure);
                        this.distanceLineStartPosition = null;
                        this.distanceLineEndPosition = null;
                        this.distance = 0;
                    }
                }
                return state;
            }
        }
        Actions.CreateCircleAction = CreateCircleAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
///<reference path="./CreateCircleAction.ts"/>
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 圆弧创建圆心Action
         * @export
         * @class CreateArcCircleCenterAction
         * @extends {CreateCircleAction}
         */
        class CreateArcCircleCenterAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateArcCircleCenterAction.
             * @memberof CreateArcCircleCenterAction
             */
            constructor() {
                super();
                this.mCircularSize = -1;
            }
            /**
             * @public
             * @description 设置圆心点大小
             * @param {number} size
             * @memberof CreateArcCircleCenterAction
             */
            setCircularSize(size) {
                this.mCircularSize = size;
            }
            /**
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateArcCircleCenterAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DIAMETERMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(true, this.firstShape, this.secondShape))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            firstGeo = firstEdge.getGeometryFeature();
                        }
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let lineAttribute = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(lineAttribute, firstWorldMatrix);
                        let center = lineAttribute.getCenter();
                        //设置中心点
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, center);
                        let points = this.measure.getPointList();
                        let point = points.find((p) => {
                            return p && p.getName() && p.getName() == M3D.MeasureNames.MEASURE_POINT_CENTER;
                        });
                        if (point && this.mCircularSize > 0) {
                            point.setSize(this.mCircularSize);
                        }
                    }
                    else {
                        state = M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    state = M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
        }
        Actions.CreateArcCircleCenterAction = CreateArcCircleCenterAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建圆心点测量Action
         * @export
         * @class CreateCenterCenterAction
         * @extends {CreateCircleAction}
         */
        class CreateCenterCenterAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateCenterCenterAction.
             * @memberof CreateCenterCenterAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateCenterCenterAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                let center1 = null;
                let firstFinished = false;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(true, this.firstShape, null))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        center1 = firstLineFeatrue.getCenter();
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, center1, 1.8);
                        firstFinished = true;
                        this.distanceLineStartPosition = center1;
                        state = M3D.Actions.ActionState.Action_PartExecuted;
                    }
                    else {
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_CENTER);
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    if (!this.secondShape) {
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                else {
                    this.removePoint(M3D.MeasureNames.MEASURE_POINT_CENTER);
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                if (firstFinished && this.secondShape && this.firstShape !== this.secondShape && (this.secondShape.getShapeType() === M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(false, this.firstShape, this.secondShape))) {
                    let secondEdge = this.secondShape;
                    let secondGeo = secondEdge.getGeometryFeature();
                    if (!secondGeo) {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let secondLineFeatrue = secondGeo.clone();
                        let secondWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, secondWorldMatrix);
                        let center2 = secondLineFeatrue.getCenter();
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER2, center2, 1.8);
                        firstFinished = true;
                        let distance = center1.Sub(center2).Length();
                        this.distance = distance;
                        this.distanceLineEndPosition = center2;
                        state = M3D.Actions.ActionState.Action_Success;
                    }
                    else {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
        }
        Actions.CreateCenterCenterAction = CreateCenterCenterAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建直径测量Action
         * @export
         * @class CreateDiametreAction
         * @extends {CreateCircleAction}
         */
        class CreateDiametreAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateDiametreAction.
             * @memberof CreateDiametreAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateDiametreAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(true, this.firstShape, this.secondShape))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            firstGeo = firstEdge.getGeometryFeature();
                        }
                    }
                    if (firstGeo && !this.secondShape && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let center = firstLineFeatrue.getCenter();
                        let majorRadius = firstLineFeatrue.getMajorRadius();
                        let minorRadius = firstLineFeatrue.getMinorRadius();
                        if (majorRadius != minorRadius) {
                            return M3D.Actions.ActionState.Action_ObjectTypeNotMatch;
                        }
                        let start = firstLineFeatrue.getStartPoint();
                        let end = firstLineFeatrue.getEndPoint();
                        //设置中心点
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, center, 1.8);
                        let pntInPlane = start;
                        //let direction = pntInPlane.Sub(center);
                        //direction.Normalize();
                        //let textPositon = direction.Multiply(10).Add(pntInPlane);
                        let lineStartPoint = center.Sub(pntInPlane).Add(center);
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, lineStartPoint);
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_END, pntInPlane);
                        this.distanceLineStartPosition = lineStartPoint;
                        this.distanceLineEndPosition = pntInPlane;
                        this.distance = majorRadius * 2;
                    }
                    else {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    state = M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
        }
        Actions.CreateDiametreAction = CreateDiametreAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建半径测量Action
         * @export
         * @class CreateRadiusAction
         * @extends {CreateCircleAction}
         */
        class CreateRadiusAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateRadiusAction.
             * @memberof CreateRadiusAction
             */
            constructor() {
                super();
            }
            /**
             * @protected
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateRadiusAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(true, this.firstShape, this.secondShape))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            firstGeo = firstEdge.getGeometryFeature();
                        }
                    }
                    if (firstGeo && !this.secondShape && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        let center = firstLineFeatrue.getCenter();
                        let majorRadius = firstLineFeatrue.getMajorRadius();
                        let minorRadius = firstLineFeatrue.getMinorRadius();
                        if (majorRadius != minorRadius) {
                            return M3D.Actions.ActionState.Action_ObjectTypeNotMatch;
                        }
                        let start = firstLineFeatrue.getStartPoint();
                        let end = firstLineFeatrue.getEndPoint();
                        //设置中心点
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, center, 1.8);
                        let pntInPlane = start;
                        //let direction = pntInPlane.Sub(center);
                        //direction.Normalize();
                        //let textPositon = direction.Multiply(10).Add(pntInPlane);
                        let lineStartPoint = center;
                        //this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, lineStartPoint);
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_END, pntInPlane);
                        this.distanceLineStartPosition = pntInPlane;
                        this.distanceLineEndPosition = lineStartPoint;
                        this.distance = majorRadius;
                    }
                    else {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
        }
        Actions.CreateRadiusAction = CreateRadiusAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建轴距测量Action
         * @export
         * @class CreateShaftShaftAction
         * @extends {CreateCircleAction}
         */
        class CreateShaftShaftAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateShaftShaftAction.
             * @memberof CreateShaftShaftAction
             */
            constructor() {
                super();
            }
            /**
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateShaftShaftAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                let firstCenter = null;
                let firstVector = null;
                let firstRadius = -1;
                let firstFinished = false;
                if (this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(true, this.firstShape, null))) {
                    let firstEdge = this.firstShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    if (!firstGeo) {
                        return M3D.Actions.ActionState.Action_Error;
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let firstWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, firstWorldMatrix);
                        firstCenter = firstLineFeatrue.getCenter();
                        firstRadius = firstLineFeatrue.getMajorRadius();
                        firstVector = this.getCircularVector(firstEdge);
                        if (!firstVector) {
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, firstVector.Multiply(this.getSquareBoxLineLength() * 1.5), firstVector.Multiply(this.getSquareBoxLineLength() * -1.5));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, firstCenter, true, true, false);
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, firstCenter, 1.8);
                        firstFinished = true;
                        this.distanceLineStartPosition = firstCenter;
                        state = M3D.Actions.ActionState.Action_PartExecuted;
                    }
                    else {
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_CENTER);
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    if (!this.secondShape) {
                        return M3D.Actions.ActionState.Action_Success;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                if (firstFinished && this.secondShape && this.firstShape !== this.secondShape && (this.secondShape.getShapeType() === M3D.Shapes.ShapeType.EDGE
                    || this.isSectionEdgeEnable(false, this.firstShape, this.secondShape))) {
                    let secondEdge = this.secondShape;
                    let secondGeo = secondEdge.getGeometryFeature();
                    if (!secondGeo) {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                        //let type = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        //if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        //    firstGeo = firstEdge.getGeometryFeature();
                        //}
                    }
                    if (secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                        let secondLineFeatrue = secondGeo.clone();
                        let secondWorldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        //几何属性变化
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, secondWorldMatrix);
                        let secondCenter = secondLineFeatrue.getCenter();
                        let secondRadius = secondLineFeatrue.getMajorRadius();
                        let secondVector = this.getCircularVector(secondEdge);
                        if (!secondVector) {
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        if (!(Math.abs(firstVector.Angle(secondVector) - 180) < 0.001 || Math.abs(firstVector.Angle(secondVector)) < 0.001)) {
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_2, this.markingColor, secondVector.Multiply(this.getSquareBoxLineLength() * 1.5), secondVector.Multiply(this.getSquareBoxLineLength() * -1.5));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_2, secondCenter, true, true, false);
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER2, secondCenter, 1.8);
                        let ray = new M3D.M3DMath.Ray(firstCenter, firstVector);
                        let projecPnt = ray.Project(secondCenter);
                        if (!projecPnt.Equals(firstCenter)) {
                            this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, projecPnt);
                            this.setLine(M3D.MeasureNames.DOTTEND_LINE, this.markingColor, firstCenter, projecPnt, M3D.Shapes.CurvePatternMode.DashedLine);
                        }
                        let distance = projecPnt.Sub(secondCenter).Length();
                        this.distance = distance;
                        this.distanceLineStartPosition = secondCenter;
                        this.distanceLineEndPosition = projecPnt;
                        state = M3D.Actions.ActionState.Action_Success;
                    }
                    else {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
            /**
             * @description 获取圆弧坐标
             * @private
             * @param {M3D.Shapes.Edge} shape
             * @return {M3D.M3DMath.Vector3}
             * @memberof CreateShaftShaftAction
             */
            getCircularVector(shape) {
                let edge = shape;
                let lineGeo = edge.getGeometryFeature();
                let vector = null;
                if (lineGeo && lineGeo.getFeatureType() === M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_ELLIPSE) {
                    let lineAttribute = lineGeo.clone();
                    let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                    M3D.Shapes.ShapeHelper.transform(lineAttribute, worldMatrix);
                    let center = lineAttribute.getCenter();
                    let start = lineAttribute.getStartPoint();
                    let end = lineAttribute.getEndPoint();
                    let startDirection = start.Sub(center).Normalized();
                    let endDirection = end.Sub(center).Normalized();
                    vector = startDirection.CrossProduct(endDirection).Normalized();
                    let angle = startDirection.Angle(endDirection);
                    if (edge instanceof M3D.Shapes.SectionEdge) {
                        let positionArray = edge.getSectionLine().getData();
                        if (Math.abs(angle - 180) < 0.1) {
                            let dataLength = Math.round(positionArray.length / 2);
                            let tmpPoint = positionArray[dataLength];
                            tmpPoint = worldMatrix.Multiply(tmpPoint);
                            let direction = center.Sub(tmpPoint).Normalized();
                            vector = startDirection.CrossProduct(direction).Normalized();
                        }
                        else if (Math.abs(angle) < 0.001) {
                            let dataLength = Math.round(positionArray.length / 3);
                            let tmpPoint = positionArray[dataLength];
                            tmpPoint = worldMatrix.Multiply(tmpPoint);
                            let direction = center.Sub(tmpPoint).Normalized();
                            vector = startDirection.CrossProduct(direction).Normalized();
                        }
                        if (vector.IsZero()) {
                            let proPlane;
                            let pt1, pt2;
                            pt1 = positionArray[0];
                            pt2 = positionArray[1];
                            proPlane = new M3D.M3DMath.Plane(pt1, pt2, center);
                            vector = proPlane.normal;
                        }
                    }
                    else if (edge instanceof M3D.Shapes.Edge) {
                        let vertexData = M3D.VertexHelper.getVertexData(this.scene, edge.getBody());
                        let indexData = M3D.VertexHelper.getIndexData(this.scene, edge);
                        if (Math.abs(angle - 180) < 0.1) {
                            let dataLength = Math.round(indexData.length / 3 / 2);
                            let tmpPoint = new M3D.M3DMath.Vector3(vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3], vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3 + 1], vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3 + 2]);
                            tmpPoint = worldMatrix.Multiply(tmpPoint);
                            let direction = center.Sub(tmpPoint).Normalized();
                            vector = startDirection.CrossProduct(direction).Normalized();
                        }
                        else if (Math.abs(angle) < 0.001) {
                            let dataLength = Math.round(indexData.length / 3 / 3);
                            let tmpPoint = new M3D.M3DMath.Vector3(vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3], vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3 + 1], vertexData.buffer[indexData.buffer[(indexData.offset + dataLength)] * 3 + 2]);
                            tmpPoint = worldMatrix.Multiply(tmpPoint);
                            let direction = center.Sub(tmpPoint).Normalized();
                            vector = startDirection.CrossProduct(direction).Normalized();
                        }
                        if (vector.IsZero()) {
                            let proPlane;
                            let pt1, pt2;
                            pt1 = new M3D.M3DMath.Vector3(vertexData.buffer[indexData.buffer[indexData.offset] * 3], vertexData.buffer[indexData.buffer[indexData.offset] * 3 + 1], vertexData.buffer[indexData.buffer[indexData.offset] * 3 + 2]);
                            pt2 = new M3D.M3DMath.Vector3(vertexData.buffer[indexData.buffer[indexData.offset] * 3 + 3], vertexData.buffer[indexData.buffer[indexData.offset] * 3 + 4], vertexData.buffer[indexData.buffer[indexData.offset] * 3 + 5]);
                            proPlane = new M3D.M3DMath.Plane(pt1, pt2, center);
                            vector = proPlane.normal;
                        }
                    }
                }
                return vector;
            }
        }
        Actions.CreateShaftShaftAction = CreateShaftShaftAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
///<reference path="./CreateCircleAction.ts"/>
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 根据三点创建圆心测量Action
         * @export
         * @class CreateThreePointsCircleCenterAction
         * @extends {CreateCircleAction}
         */
        class CreateThreePointsCircleCenterAction extends Actions.CreateCircleAction {
            /**
             * @constructor
             * Creates an instance of CreateThreePointsCircleCenterAction.
             * @memberof CreateThreePointsCircleCenterAction
             */
            constructor() {
                super();
                this.mCircularSize = -1;
            }
            /**
             * @public
             * @description 设置圆心点大小
             * @param {number} size
             */
            setCircularSize(size) {
                this.mCircularSize = size;
            }
            /**
             * @description 根据参数创建圆测量对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateThreePointsCircleCenterAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.firstPosition) {
                    this.setPoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_1, this.firstPosition);
                }
                else {
                    console.log("");
                }
                if (this.secondPosition) {
                    this.setPoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_2, this.secondPosition);
                }
                else {
                    console.log("");
                }
                if (this.thirdPosition) {
                    this.setPoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_3, this.thirdPosition);
                    if (this.firstPosition
                        && !this.firstPosition.IsZero()
                        && this.secondPosition
                        && !this.secondPosition.IsZero()
                        && this.thirdPosition
                        && !this.thirdPosition.IsZero()) {
                        let res = M3D.Utility.CalculateHelper.isTriangle(this.firstPosition, this.secondPosition, this.thirdPosition);
                        if (res) {
                            let radius = 0;
                            let mPoint = new M3D.M3DMath.Vector3();
                            res = M3D.Utility.CalculateHelper.calculateCircleCenter(this.firstPosition, this.secondPosition, this.thirdPosition, mPoint, radius);
                            //if (res) {
                            //设置中心点
                            this.setPoint(M3D.MeasureNames.MEASURE_POINT_CENTER, mPoint);
                            //}
                            let points = this.measure.getPointList();
                            let point = points.find((p) => {
                                return p && p.getName() && p.getName() == M3D.MeasureNames.MEASURE_POINT_CENTER;
                            });
                            if (point && this.mCircularSize > 0) {
                                point.setSize(this.mCircularSize);
                            }
                        }
                        else {
                            state = M3D.Actions.ActionState.Action_Error;
                        }
                    }
                    else {
                        state = M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                else {
                    state = M3D.Actions.ActionState.Action_PartExecuted;
                }
                return state;
            }
            /**
             * @public
             * @description 删除临时辅助点(3个)
             * */
            removeTmpPoint() {
                this.removePoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_1);
                this.removePoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_2);
                this.removePoint(M3D.MeasureNames.MEASURE_POINT_CIRCLE_3);
            }
        }
        Actions.CreateThreePointsCircleCenterAction = CreateThreePointsCircleCenterAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 距离测量动作
         * @export
         * @class CreateDistanceAction
         * @extends {MeasureAction}
         */
        class CreateDistanceAction extends Actions.MeasureAction {
            /**
             * @constructor
             * Creates an instance of CreateDistanceAction.
             * @memberof CreateDistanceAction
             */
            constructor() {
                super();
            }
            /**
            * @description 设置参数
            * @param  objID   ，type ，titleColor  ，textColor ，fontName ，fontSize 字体大小，titleFormat 标题格式，backGroundColor 背景颜色，maxDistanceColor 最远距离颜色，minDistanceColor 最近距离颜色，measurementUnit 测量单位，isShowMaxDistance 是否展示最远距离，isShowMinDistance 是否展示最远距离 ，parentModelID  模型ID
            */
            /**
             * @public
             * @description 设置参数
             * @param {number} objID 测量ID
             * @param {number} type 类型
             * @param {M3D.Shapes.Shape} firstShape 第一个Shape对象
             * @param {M3D.M3DMath.Vector3} firstPosition 拾取的第一个位置
             * @param {M3D.Shapes.Shape} secondShape 第二个Shape对象
             * @param {M3D.M3DMath.Vector3} secondPosition 拾取的第二个位置
             * @param {M3D.M3DMath.Color} titleTextColor 标题字体颜色
             * @param {M3D.M3DMath.Color} resultTextColor 结果字体颜色
             * @param {string} fontName 字体类型
             * @param {number} fontSize 字体大小
             * @param {string} titleFormat 标题类型
             * @param {M3D.M3DMath.Color} titleBackgroundColor 标题背景颜色
             * @param {M3D.M3DMath.Color} resultBackgroundColor 结果背景颜色
             * @param {M3D.M3DMath.Color} maxDistanceColor 最大距离颜色
             * @param {M3D.M3DMath.Color} minDistanceColor 最近距离颜色
             * @param {string} measurementUnit 测量结果单位
             * @param {boolean} isShowMaxDistance 是否显示最大距离
             * @param {boolean} isShowMinDistance 是否显示最近距离
             * @param {string} model1 关联模型装配路径1
             * @param {string} model2 关联模型装配路径2
             * @param {M3D.M3DMath.Vector3} normalDirection 拾取点的方向
             * @param {M3D.M3DMath.Vector3} thirdPosition 第三个点
             * @return {boolean}
             * @memberof CreateDistanceAction
             */
            setParameter(objID, type, firstShape, firstPosition, secondShape, secondPosition, titleTextColor, resultTextColor, fontName, fontSize, titleFormat, titleBackgroundColor, resultBackgroundColor, maxDistanceColor, minDistanceColor, measurementUnit, isShowMaxDistance, isShowMinDistance, model1, model2, normalDirection, thirdPosition) {
                this.objID = objID ? objID : 0;
                this.measureType = type;
                this.titleTextColor = titleTextColor;
                this.fontSize = fontSize;
                this.fontName = fontName;
                this.titleBackgroundColor = titleBackgroundColor;
                this.resultBackgroundColor = resultBackgroundColor;
                this.isShowMaxDistance = isShowMaxDistance;
                this.isShowMinDistance = isShowMinDistance;
                this.maxDistanceColor = maxDistanceColor;
                this.minDistanceColor = minDistanceColor;
                this.resultTextColor = resultTextColor;
                this.firstShape = firstShape;
                this.firstPosition = firstPosition;
                this.secondShape = secondShape;
                this.secondPosition = secondPosition;
                this.imagePosition = thirdPosition;
                this.normalDirection = normalDirection;
                this.modelPath1 = model1;
                this.modelPath2 = model2;
                this.measurementUnit = measurementUnit;
                return true;
            }
            /**
             * @description 根据参数创建距离对象
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateDistanceAction
             */
            doMeasure() {
                let methodName = this.doMeasure.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let state = M3D.Actions.ActionState.Action_Success;
                if (this.objID == 0)
                    this.measure = new M3D.Shapes.MeasureDistance();
                else {
                    let measures = this.scene.getShapes();
                    this.measure = measures.getShape(this.objID);
                    if (this.imagePosition && !this.imagePosition.IsZero()) {
                        let imageList = this.measure.getImageList();
                        if (imageList.length > 0) {
                            for (let image of imageList) {
                                image.setPosition(this.fourthPosition);
                            }
                        }
                        let lines = this.measure.getLineList();
                        for (let i = 0; i < lines.length; i++) {
                            if (lines[i].getName() === "leaderLine") {
                                lines[i].setEndPoint(this.fourthPosition);
                                return M3D.Actions.ActionState.Action_Success;
                            }
                        }
                    }
                }
                this.distances = [];
                this.setBaseInfo();
                this.clearMeasure();
                this.setRelatedModel();
                state = this.setShapeElement();
                if (state != M3D.Actions.ActionState.Action_Success) {
                    return state;
                }
                //获取引线
                //if (!this.fourthPosition) {
                if (this.distanceLineStartPosition && this.distanceLineEndPosition) {
                    let results = this.getLeaderPoints(this.distanceLineStartPosition, this.distanceLineEndPosition);
                    this.leaderStartPosition = results[0];
                    this.leaderEndPosition = results[1];
                    if (this.measure && this.measure.getFirstParentModel()
                        && this.measure.getMeasureType() === M3D.Shapes.MeasureType.PNT_PNT_DISTANCE) {
                        // 设置第一个点
                        const worldTransformInverse = this.measure.getFirstParentModel().getWorldTransform().Inverse();
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, worldTransformInverse.Multiply(this.distanceLineStartPosition));
                        this.measure.setFirstPosition(worldTransformInverse.Multiply(this.distanceLineStartPosition));
                    }
                    else {
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.distanceLineStartPosition);
                    }
                    //this.setPoint(M3D.ActionNames.MEASURE_POINT_DISTANCELINE_START, this.distanceLineStartPosition);
                    if (this.measure && this.measure.getSecondParentModel()
                        && this.measure.getMeasureType() === M3D.Shapes.MeasureType.PNT_PNT_DISTANCE) {
                        // 设置第一个点
                        const worldTransformInverse = this.measure.getSecondParentModel().getWorldTransform().Inverse();
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_END, worldTransformInverse.Multiply(this.distanceLineEndPosition));
                        this.measure.setSecondPosition(worldTransformInverse.Multiply(this.distanceLineEndPosition));
                    }
                    else {
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_END, this.distanceLineEndPosition);
                    }
                    //this.setPoint(M3D.ActionNames.MEASURE_POINT_DISTANCELINE_END, this.distanceLineEndPosition);
                    // 添加连接线和引出线
                    this.setLine(M3D.MeasureNames.MEASURE_LINE_DISTANCE, this.titleBackgroundColor, this.distanceLineStartPosition, this.distanceLineEndPosition);
                    this.setLine(M3D.MeasureNames.LINE_LEADER, this.leaderColor, this.leaderStartPosition, this.leaderEndPosition);
                    if (state != M3D.Actions.ActionState.Action_Success) {
                        return state;
                    }
                    for (let i = 0; i < this.distances.length; i++) {
                        //添加Text文本，用于在线视图保存
                        //解决协同中文本读取
                        this.setComText(i, this.distances[i]);
                    }
                    let unit = "";
                    unit = this.getMeasureUnit(Number(this.measurementUnit), this.distances, unit);
                    let temptext = new Array();
                    //DOTO
                    for (let i = 0; i < this.distances.length; ++i) {
                        if (this.distances[i] > -0.000001) {
                            let tempText = [];
                            let length = this.distances[i];
                            let lengthstr = length.toFixed(2).toString();
                            let title = new M3D.Shapes.Texts2D();
                            title.setSize(this.fontSize);
                            let titleBackGround = i === 0 ? this.titleBackgroundColor : (i === 1 ? this.minDistanceColor : this.maxDistanceColor);
                            title.setBackground(titleBackGround);
                            title.setFontColor(this.titleTextColor);
                            title.setTexts(M3D.Shapes.MeasureDisplayHelper.getTitleText(i, this.measureType));
                            tempText.push(title);
                            let content = new M3D.Shapes.Texts2D();
                            content.setSize(this.fontSize);
                            content.setBackground(this.resultBackgroundColor);
                            content.setFontColor(this.resultTextColor);
                            content.setTexts(M3D.MeasureNames.MEASURE_APPROXIMATE_SYMBOL + lengthstr + unit + " ");
                            tempText.push(content);
                            temptext.push(tempText);
                            //添加Text文本，用于在线视图保存
                            //解决协同中文本读取
                            //this.setComText(i, length);
                        }
                    }
                    if (temptext.length > 0) {
                        this.measure.setPosition(this.leaderEndPosition);
                        M3D.Shapes.MeasureDisplayHelper.createNoteTextsImageMultiline(this.scene, temptext, this.leaderEndPosition, this.measure);
                    }
                    //} else {
                    //    let imageList = this.measure.getImageList();
                    //    if (imageList.length > 0) {
                    //        for (let image of imageList) {
                    //            image.setPosition(this.fourthPosition);
                    //        }
                    //    }
                    //    let lines = this.measure.getLineList();
                    //    for (let i = 0; i < lines.length; i++) {
                    //        if (lines[i].getName() === "leaderLine") {
                    //            lines[i].setEndPoint(this.fourthPosition);
                    //            return M3D.Actions.ActionState.Action_Success;
                    //        }
                    //    }
                    //}
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return state;
            }
            pointLineDistance(...restOfName) {
                let methodName = this.pointLineDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 5) {
                    if (restOfName[1] instanceof M3D.Shapes.LineFeature) {
                        let pntCoordinate = restOfName[0];
                        let lineFeature = restOfName[1];
                        let distance = restOfName[2];
                        let inLine = restOfName[3];
                        let projectPnt = restOfName[4];
                        let ray = new M3D.M3DMath.Ray(lineFeature.getStartPoint(), lineFeature.getDirection());
                        projectPnt.copyFrom(ray.Project(pntCoordinate));
                        inLine = 0;
                        distance = (pntCoordinate.Sub(projectPnt)).Length();
                        return distance;
                    }
                    else if (restOfName[1] instanceof M3D.M3DMath.Vector3) {
                        let point1 = restOfName[0];
                        let point2 = restOfName[1];
                        let point = restOfName[2];
                        let minDistance = restOfName[3];
                        let minPoint = restOfName[4];
                        let projectPoint = new M3D.M3DMath.Vector3();
                        this.pntInSegment(point, point1, point2, projectPoint);
                        minDistance = (point.Sub(projectPoint)).Length();
                        minPoint.copyFrom(projectPoint);
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return minDistance;
                    }
                }
                else if (restOfName.length == 7) {
                    if (restOfName[0] instanceof M3D.M3DMath.Vector3) {
                        let pntCoordinate = restOfName[0];
                        let ellipseAttribute = restOfName[1];
                        let distance = restOfName[2];
                        let inLine = restOfName[3];
                        let projectPnt = restOfName[4];
                        let worldMatrix = restOfName[5];
                        let lineSet = restOfName[6];
                        inLine = 0; //没用
                        let center = ellipseAttribute.GetCenterPoint();
                        let start = ellipseAttribute.GetStartPoint();
                        let end = ellipseAttribute.GetEndPoint();
                        let dirX = new M3D.M3DMath.Vector3(), dirY = new M3D.M3DMath.Vector3(), dirZ = new M3D.M3DMath.Vector3();
                        ellipseAttribute.GetXYZDir(dirX, dirY, dirZ);
                        let R = ellipseAttribute.GetMajorRadius(); //半径
                        //构造圆弧平面
                        let plane = new M3D.M3DMath.Plane(dirZ, center);
                        //求出投影点
                        let projectP = plane.Project(pntCoordinate);
                        let OP = projectP.Sub(center).Clone();
                        let disTocen = OP.Length(); //投影点到圆心距离
                        if (disTocen > R) {
                            //let xPolyLine: M3D.Shapes.PolyLine = lineSet.GetRefLine();
                            //let pntPoints: M3D.M3DMath.Vector3[] = lineSet.getPoints();
                            //let offset = lineSet.GetDataOffset();
                            //let dataLength = lineSet.GetDataLength();
                            //let flag = false;
                            //	int countIt =1;
                            //for (let i = offset; i < offset + dataLength; i += 2) {
                            //    flag = MeasureCalculateHelper.RaySegmentInsect(center, projectP, pntPoints[i], pntPoints[i + 1]);
                            //    if (flag) {
                            //        projectPnt.copyFrom(center.Add(OP.Multiply(R * (1.0 / disTocen))));
                            //        distance = (pntCoordinate.Sub(projectPnt).Clone()).Length();
                            //    }
                            //}
                            //if (!flag) {
                            //    let CADis = (projectP.Sub(start).Clone()).Length();
                            //    let CBDis = (projectP.Sub(end).Clone()).Length();
                            //    if (CADis >= CBDis) {
                            //        distance = (pntCoordinate.Sub(end).Clone()).Length();
                            //        projectPnt.copyFrom(end);
                            //    }
                            //    else {
                            //        distance = (pntCoordinate.Sub(start).Clone()).Length();
                            //        projectPnt.copyFrom(start);
                            //    }
                            //}
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return distance;
                        }
                        else {
                            //let xPolyLine: SPolyLine = m_lineSet.GetRefLine();
                            //let pntPoints: Vector3[] = xPolyLine.GetPoints();
                            //let offset = m_lineSet.GetDataOffset();
                            //let dataLength = m_lineSet.GetDataLength();
                            ////	int countIt =1;
                            //let flag = false;
                            //for (let i = offset; i < offset + dataLength; i += 2) {
                            //    flag = MeasureCalculateHelper.RaySegmentInsect(center, projectP, pntPoints[i], pntPoints[i + 1]);
                            //    if (flag) {
                            //        projectPnt.copyFrom(center.Add(OP.Multiply(R * (1.0 / disTocen))));
                            //        distance = (pntCoordinate.Sub(projectPnt).Clone()).Length();
                            //    }
                            //}
                            //if (!flag) {
                            //    let CADis = (projectP.Sub(start).Clone()).Length();
                            //    let CBDis = (projectP.Sub(end).Clone()).Length();
                            //    if (CADis >= CBDis) {
                            //        distance = (pntCoordinate.Sub(end).Clone()).Length();
                            //        projectPnt.copyFrom(end);
                            //    }
                            //    else {
                            //        distance = (pntCoordinate.Sub(start).Clone()).Length();
                            //        projectPnt.copyFrom(start);
                            //    }
                            //}
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return distance;
                        }
                    }
                    else {
                        let lineSet = restOfName[0];
                        let point = restOfName[1];
                        let distance = restOfName[2];
                        let minPoint = restOfName[3];
                        let worldMatrix = restOfName[4];
                        let lineStart = restOfName[5];
                        let lineEnd = restOfName[6];
                        let vertexSets = this.scene.getVertexSetManager();
                        if (lineSet instanceof M3D.Shapes.SectionEdge) {
                            let pntPoints = lineSet.getSectionLine().getData();
                            let dataLength = pntPoints.length;
                            let offset = lineSet.getSectionLine().getVertexOffset();
                            distance = (pntPoints[offset].Sub(point).Clone()).Length(); //初始化
                            minPoint.copyFrom(pntPoints[offset]); //初始化
                            for (let i = 0; i < dataLength - 1; i++) {
                                let point1 = pntPoints[i];
                                let point2 = pntPoints[i + 1];
                                let distanceTemp;
                                let minPointTemp = new M3D.M3DMath.Vector3();
                                distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                                if (distance > distanceTemp) {
                                    distance = distanceTemp;
                                    minPoint.copyFrom(minPointTemp);
                                    lineStart = point1;
                                    lineEnd = point2;
                                }
                            }
                        }
                        else {
                            let body = lineSet.getBody();
                            let bodyVertexHeader = vertexSets.getVertexHeader(body.getProto());
                            let edgeVertexHeader = vertexSets.getVertexHeader(lineSet.getProto());
                            let edgeVertexSet = edgeVertexHeader.vertexSet;
                            let bodyVertexSet = bodyVertexHeader.vertexSet;
                            if (edgeVertexSet && bodyVertexSet) {
                                //使用索引
                                let indexCount = lineSet.getIndexLength();
                                let indexOffset = lineSet.getIndexOffset();
                                let meshBuffer = bodyVertexSet.getMeshArray();
                                let indexBuffer = edgeVertexSet.getMeshArray();
                                let meshArray = new Float32Array(meshBuffer, bodyVertexHeader.vertexOffset, bodyVertexHeader.vertexLength);
                                let indexArray = new Uint32Array(indexBuffer, indexOffset, indexCount);
                                let startPoint = new M3D.M3DMath.Vector3();
                                startPoint.x = meshArray[indexArray[0] * 3 + 0];
                                startPoint.y = meshArray[indexArray[0] * 3 + 1];
                                startPoint.z = meshArray[indexArray[0] * 3 + 2];
                                startPoint = worldMatrix.Multiply(startPoint);
                                distance = (worldMatrix.Multiply(startPoint).Sub(worldMatrix.Multiply(point)).Clone()).Length(); //初始化
                                minPoint.copyFrom(startPoint); //初始化
                                if (indexArray) {
                                    for (let index = 0; index < indexCount; index += 2) {
                                        let distanceTemp;
                                        let minPointTemp = new M3D.M3DMath.Vector3();
                                        let p1 = new M3D.M3DMath.Vector3();
                                        let p2 = new M3D.M3DMath.Vector3();
                                        p1.x = meshArray[indexArray[index] * 3 + 0];
                                        p1.y = meshArray[indexArray[index] * 3 + 1];
                                        p1.z = meshArray[indexArray[index] * 3 + 2];
                                        p2.x = meshArray[indexArray[index + 1] * 3 + 0];
                                        p2.y = meshArray[indexArray[index + 1] * 3 + 1];
                                        p2.z = meshArray[indexArray[index + 1] * 3 + 2];
                                        let point1 = worldMatrix.Multiply(p1);
                                        let point2 = worldMatrix.Multiply(p2);
                                        distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                                        if (distance > distanceTemp) {
                                            distance = distanceTemp;
                                            minPoint.copyFrom(minPointTemp);
                                            lineStart = point1;
                                            lineEnd = point2;
                                        }
                                    }
                                }
                            }
                        }
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return [distance, lineStart, lineEnd];
                    }
                }
                else if (restOfName.length == 4) {
                    let lineSet = restOfName[0];
                    let point = restOfName[1];
                    let distance = restOfName[2];
                    let minPoint = restOfName[3];
                    //let xPolyLine: SPolyLine = lineSet.GetRefLine();
                    let pntPoints = lineSet.getPoints();
                    let offset = lineSet.GetDataOffset();
                    let dataLength = lineSet.GetDataLength();
                    let countIt = 1;
                    distance = (pntPoints[offset].Sub(point).Clone()).Length(); //初始化
                    minPoint.copyFrom(pntPoints[offset]); //初始化
                    for (let i = offset; i < offset + dataLength; i += 2) {
                        let distanceTemp;
                        let minPointTemp = new M3D.M3DMath.Vector3();
                        let point1 = pntPoints[i];
                        let point2 = pntPoints[i + 1];
                        distanceTemp = this.pointLineDistance(point1, point2, point, distanceTemp, minPointTemp);
                        if (distance > distanceTemp) {
                            distance = distanceTemp;
                            minPoint.copyFrom(minPointTemp);
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return distance;
                }
            }
            pointFaceDistance(...restOfName) {
                let methodName = this.pointFaceDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length == 6) {
                    let pntCoordinate = restOfName[0];
                    let face = restOfName[1];
                    let minDistance = restOfName[2];
                    let maxDistance = restOfName[3];
                    let minPoint = restOfName[4];
                    let maxPoint = restOfName[5];
                    let vertexSets = this.scene.getVertexSetManager();
                    let body = face.getBody();
                    let bodyVertexHeader = vertexSets.getVertexHeader(body.getProto());
                    let faceVertexHeader = vertexSets.getVertexHeader(face.getProto());
                    let faceVertexSet = faceVertexHeader.vertexSet;
                    let bodyVertexSet = bodyVertexHeader.vertexSet;
                    if (faceVertexSet && bodyVertexSet) {
                        let useIndex = faceVertexHeader.useIndex;
                        let indexCount = faceVertexHeader.indexLength;
                        let indexOffset = faceVertexHeader.indexOffset;
                        let meshBuffer = bodyVertexSet.getMeshArray();
                        let indexBuffer = faceVertexSet.getMeshArray();
                        let positionArray = new Float32Array(meshBuffer, bodyVertexHeader.vertexOffset, bodyVertexHeader.vertexLength);
                        let indexArray = new Uint32Array(indexBuffer, indexOffset, indexCount);
                        if (useIndex) {
                            //let indexArray = pSet.GetIndexArray();
                            let min = 0.0, max = 0.0;
                            //Vector3 minPnt;
                            let tempTriSet;
                            let minIndex, maxIndex;
                            let count = 0;
                            //获得最小距离点索引
                            let startPnt = new M3D.M3DMath.Vector3();
                            startPnt.x = positionArray[indexArray[0] * 3 + 0];
                            startPnt.y = positionArray[indexArray[0] * 3 + 1];
                            startPnt.z = positionArray[indexArray[0] * 3 + 2];
                            minPoint.copyFrom(startPnt);
                            min = (pntCoordinate.Sub(startPnt).Clone()).Length();
                            minDistance = min;
                            maxDistance = 0.0;
                            for (let index = 0; index < indexCount; index += 3) {
                                let minIndexTemp, maxIndexTemp;
                                let mintemp, maxtemp;
                                let p1 = new M3D.M3DMath.Vector3();
                                let p2 = new M3D.M3DMath.Vector3();
                                let p3 = new M3D.M3DMath.Vector3();
                                p1.x = positionArray[indexArray[index] * 3 + 0];
                                p1.y = positionArray[indexArray[index] * 3 + 1];
                                p1.z = positionArray[indexArray[index] * 3 + 2];
                                p2.x = positionArray[indexArray[index + 1] * 3 + 0];
                                p2.y = positionArray[indexArray[index + 1] * 3 + 1];
                                p2.z = positionArray[indexArray[index + 1] * 3 + 2];
                                p3.x = positionArray[indexArray[index + 2] * 3 + 0];
                                p3.y = positionArray[indexArray[index + 2] * 3 + 1];
                                p3.z = positionArray[indexArray[index + 2] * 3 + 2];
                                let d1 = (pntCoordinate.Sub(p1).Clone()).Length();
                                let d2 = (pntCoordinate.Sub(p2).Clone()).Length();
                                let d3 = (pntCoordinate.Sub(p3).Clone()).Length();
                                if (d1 > d2) {
                                    if (d1 > d3) {
                                        maxIndexTemp = indexArray[index];
                                        maxtemp = d1;
                                    }
                                    else {
                                        maxIndexTemp = (indexArray[index + 2]);
                                        maxtemp = d3;
                                    }
                                }
                                else if (d2 > d3) {
                                    maxIndexTemp = (indexArray[index + 1]);
                                    maxtemp = d2;
                                }
                                else {
                                    maxIndexTemp = (indexArray[index + 2]);
                                    maxtemp = d3;
                                }
                                if (max < maxtemp) {
                                    max = maxtemp;
                                    maxIndex = maxIndexTemp;
                                }
                                let plane = new M3D.M3DMath.Plane(p1, p2, p3);
                                let pntInPlane = plane.Project(pntCoordinate);
                                let ray = new M3D.M3DMath.Ray(pntCoordinate, (pntInPlane.Sub(pntCoordinate)));
                                let temPro = M3D.Actions.RayPickHelper.isIntersectRayAndTriangle(p1, p2, p3, ray);
                                if ((pntCoordinate.Sub(pntInPlane).Clone()).IsZero()) {
                                    minDistance = 0.0;
                                    minPoint.copyFrom(pntInPlane);
                                }
                                else if (temPro) {
                                    let tempDistance = 0.0;
                                    tempDistance = (pntCoordinate.Sub(temPro).Clone()).Length();
                                    if (minDistance > tempDistance) {
                                        minDistance = tempDistance;
                                        minPoint.copyFrom(temPro);
                                    }
                                }
                                else {
                                    let tempDistance;
                                    let tempProjection = new M3D.M3DMath.Vector3();
                                    let pro1 = new M3D.M3DMath.Vector3(), pro2 = new M3D.M3DMath.Vector3(), pro3 = new M3D.M3DMath.Vector3();
                                    this.pntInSegment(pntCoordinate, p1, p2, pro1);
                                    this.pntInSegment(pntCoordinate, p1, p3, pro2);
                                    this.pntInSegment(pntCoordinate, p2, p3, pro3);
                                    let ds1 = (pntCoordinate.Sub(pro1).Clone()).Length();
                                    let ds2 = (pntCoordinate.Sub(pro2).Clone()).Length();
                                    let ds3 = (pntCoordinate.Sub(pro3).Clone()).Length();
                                    if (ds1 < ds2) {
                                        if (ds1 < ds3) {
                                            tempDistance = ds1;
                                            tempProjection = pro1;
                                        }
                                        else {
                                            tempDistance = ds3;
                                            tempProjection = pro3;
                                        }
                                    }
                                    else if (ds2 < ds3) {
                                        tempDistance = ds2;
                                        tempProjection = pro2;
                                    }
                                    else {
                                        tempDistance = ds3;
                                        tempProjection = pro3;
                                    }
                                    if (minDistance > tempDistance) {
                                        minDistance = tempDistance;
                                        minPoint.copyFrom(tempProjection);
                                    }
                                }
                            }
                            maxDistance = max;
                            maxPoint.x = positionArray[maxIndex * 3 + 0];
                            maxPoint.y = positionArray[maxIndex * 3 + 1];
                            maxPoint.z = positionArray[maxIndex * 3 + 2];
                        }
                        else {
                            let min = 0.0, max = 0.0;
                            let startPnt = new M3D.M3DMath.Vector3();
                            startPnt.x = positionArray[indexArray[0] * 3 + 0];
                            startPnt.y = positionArray[indexArray[0] * 3 + 1];
                            startPnt.z = positionArray[indexArray[0] * 3 + 2];
                            minPoint.copyFrom(startPnt); //初始化
                            min = (pntCoordinate.Sub(startPnt).Clone()).Length();
                            minDistance = min; //初始化
                            let mintemp, maxtemp;
                            let minPntTemp = new M3D.M3DMath.Vector3(), maxPntTemp = new M3D.M3DMath.Vector3();
                            let minPnt = new M3D.M3DMath.Vector3(), maxPnt = new M3D.M3DMath.Vector3();
                            let tempTriSet;
                            for (let index = (indexArray)[0]; index <= indexArray[0] + indexCount; index += 3) {
                                let tempDistance = 0.0;
                                let p1 = new M3D.M3DMath.Vector3();
                                let p2 = new M3D.M3DMath.Vector3();
                                let p3 = new M3D.M3DMath.Vector3();
                                p1.x = positionArray[indexArray[index] * 3 + 0];
                                p1.y = positionArray[indexArray[index] * 3 + 1];
                                p1.z = positionArray[indexArray[index] * 3 + 2];
                                p2.x = positionArray[indexArray[index + 1] * 3 + 0];
                                p2.y = positionArray[indexArray[index + 1] * 3 + 1];
                                p2.z = positionArray[indexArray[index + 1] * 3 + 2];
                                p3.x = positionArray[indexArray[index + 2] * 3 + 0];
                                p3.y = positionArray[indexArray[index + 2] * 3 + 1];
                                p3.z = positionArray[indexArray[index + 2] * 3 + 2];
                                let d1 = (pntCoordinate.Sub(p1).Clone()).Length();
                                let d2 = (pntCoordinate.Sub(p2).Clone()).Length();
                                let d3 = (pntCoordinate.Sub(p3).Clone()).Length();
                                if (d1 > d2) {
                                    if (d1 > d3) {
                                        maxPntTemp = p1;
                                        maxtemp = d1;
                                    }
                                    else {
                                        maxPntTemp = p3;
                                        maxtemp = d3;
                                    }
                                }
                                else if (d2 > d3) {
                                    maxPntTemp = p2;
                                    maxtemp = d2;
                                }
                                else {
                                    maxPntTemp = p3;
                                    maxtemp = d3;
                                }
                                if (max < maxtemp) {
                                    max = maxtemp;
                                    maxPnt.copyFrom(maxPntTemp);
                                }
                                let plane = new M3D.M3DMath.Plane(p1, p2, p3);
                                let pntInPlane = plane.Project(pntCoordinate);
                                let ray = new M3D.M3DMath.Ray(pntCoordinate, (pntInPlane.Sub(pntCoordinate).Clone()));
                                let temPro = M3D.Actions.RayPickHelper.isIntersectRayAndTriangle(p1, p2, p3, ray);
                                if ((pntCoordinate.Sub(pntInPlane)).IsZero()) {
                                    minDistance = 0.0;
                                    minPoint.copyFrom(pntInPlane);
                                }
                                else if (temPro) {
                                    tempDistance = (pntCoordinate.Sub(temPro).Clone()).Length();
                                    if (minDistance > tempDistance) {
                                        minDistance = tempDistance;
                                        minPoint.copyFrom(temPro);
                                    }
                                }
                                else {
                                    let tempDistance;
                                    let tempProjection = new M3D.M3DMath.Vector3();
                                    let pro1 = new M3D.M3DMath.Vector3(), pro2 = new M3D.M3DMath.Vector3(), pro3 = new M3D.M3DMath.Vector3();
                                    this.pntInSegment(pntCoordinate, p1, p2, pro1);
                                    this.pntInSegment(pntCoordinate, p1, p3, pro2);
                                    this.pntInSegment(pntCoordinate, p2, p3, pro3);
                                    let ds1 = (pntCoordinate.Sub(pro1).Clone()).Length();
                                    let ds2 = (pntCoordinate.Sub(pro2).Clone()).Length();
                                    let ds3 = (pntCoordinate.Sub(pro3).Clone()).Length();
                                    if (ds1 < ds2) {
                                        if (ds1 < ds3) {
                                            tempDistance = ds1;
                                            tempProjection = pro1;
                                        }
                                        else {
                                            tempDistance = ds3;
                                            tempProjection = pro3;
                                        }
                                    }
                                    else if (ds2 < ds3) {
                                        tempDistance = ds2;
                                        tempProjection = pro2;
                                    }
                                    else {
                                        tempDistance = ds3;
                                        tempProjection = pro3;
                                    }
                                    if (minDistance > tempDistance) {
                                        minDistance = tempDistance;
                                        minPoint.copyFrom(tempProjection);
                                    }
                                }
                            }
                            maxDistance = max;
                            maxPoint.copyFrom(maxPnt);
                        }
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [minDistance, maxDistance];
                }
                else {
                    let pntCoordinate = restOfName[0];
                    let planeFaceFeatrue = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt = restOfName[3];
                    let ret = true;
                    let plane = new M3D.M3DMath.Plane(planeFaceFeatrue.getNormal(), planeFaceFeatrue.getOrigin());
                    projectPnt.copyFrom(plane.Project(pntCoordinate));
                    distance = projectPnt.Sub(pntCoordinate).Length();
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [ret, distance, projectPnt];
                }
            }
            /**
             * @description 设置关联模型
             * @private
             * @return {boolean}
             * @memberof CreateDistanceAction
             */
            setRelatedModel() {
                let methodName = this.setRelatedModel.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.ANNOTATIONACTION, methodName);
                let ret = false;
                if (this.scene) {
                    if (this.modelPath1 && this.modelPath1 != "") {
                        let parentModel = this.scene.getShapes().getShapeByPlcPath(this.modelPath1);
                        if (parentModel) {
                            this.measure.setFirstParentPath(parentModel.getPlcPath());
                            this.measure.setFirstParentModel(parentModel);
                            ret = true;
                        }
                    }
                    else {
                        this.measure.setFirstParentPath("");
                        this.measure.setFirstParentModel(null);
                        ret = true;
                    }
                    if (this.modelPath2 && this.modelPath2 != "") {
                        let parentModel = this.scene.getShapes().getShapeByPlcPath(this.modelPath2);
                        if (parentModel) {
                            this.measure.setSecondParentPath(parentModel.getPlcPath());
                            this.measure.setSecondParentModel(parentModel);
                            ret = true;
                        }
                    }
                    else {
                        this.measure.setSecondParentPath("");
                        this.measure.setSecondParentModel(null);
                        ret = true;
                    }
                }
                else {
                    if (this.modelPath1 && this.modelPath1 !== "") {
                        this.measure.setFirstParentPath(this.modelPath1);
                    }
                    if (this.modelPath2 && this.modelPath2 !== "") {
                        this.measure.setSecondParentPath(this.modelPath2);
                    }
                    ret = true;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.ANNOTATIONACTION, methodName);
                return ret;
            }
        }
        Actions.CreateDistanceAction = CreateDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建面面距离测量Action
         * @export
         * @class CreateFaceFaceDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreateFaceFaceDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreateFaceFaceDistanceAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isFirstPlane = false; // 第一个面是否是平面
                let isSecondPane = false; //第二个面是否是平面
                //let width: number = 0, height: number = 0;
                //width = this.scene.getCurrentViewPort().getRect().Size().x;
                //height = this.scene.getCurrentViewPort().getRect().Size().y;
                let firstFinished = false, secondFinished = false;
                let face1Feature = new M3D.Shapes.PlaneFaceFeature();
                let face2Feature = new M3D.Shapes.PlaneFaceFeature();
                if (this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.FACE && this.firstPosition) {
                    let face1 = this.firstShape;
                    let srcFace1Geo = face1.getGeometryFeature();
                    if (srcFace1Geo && srcFace1Geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        isFirstPlane = true;
                        face1Feature = srcFace1Geo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face1);
                        M3D.Shapes.ShapeHelper.transform(face1Feature, worldMatrix);
                        let distance = 0;
                        let projectPnt = new M3D.M3DMath.Vector3();
                        let result = this.pointFaceDistance(this.firstPosition, face1Feature, distance, projectPnt);
                        distance = result[1];
                        //distance = width > height ? height / M3D.MeasureNames.SCREENSQUAREBOXSCALE : width / M3D.MeasureNames.SCREENSQUAREBOXSCALE;
                        //let faceBoundingBox = face1.getBoundingBox();
                        //distance = faceBoundingBox.getMinLength() / 10;
                        distance = this.getSquareBoxLineLength();
                        let normalVec = face1Feature.getNormal();
                        let originVec = face1Feature.getOrigin();
                        let direction = projectPnt.Sub(originVec);
                        //this.setPoint(M3D.MeasureNames.MEASURE_POINT_FACE1, this.firstPosition);
                        this.distanceLineStartPosition = this.firstPosition;
                        //平面标记
                        let pointArray = [];
                        pointArray = this.getPointSquareBoxMark(pointArray, this.firstPosition, direction, normalVec, distance);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.markingColor, pointArray[0].Sub(this.firstPosition), pointArray[1].Sub(this.firstPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.markingColor, pointArray[2].Sub(this.firstPosition), pointArray[3].Sub(this.firstPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.markingColor, pointArray[0].Sub(this.firstPosition), pointArray[2].Sub(this.firstPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.markingColor, pointArray[3].Sub(this.firstPosition), pointArray[1].Sub(this.firstPosition));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.firstPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.firstPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.firstPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.firstPosition, true, true, false);
                        firstFinished = true;
                    }
                    else if (srcFace1Geo) {
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                        //this.setPoint(M3D.MeasureNames.MEASURE_POINT_FACE1, this.firstPosition);
                        firstFinished = false;
                    }
                }
                if (firstFinished && this.secondShape && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE && this.secondPosition) {
                    let face2 = this.secondShape;
                    let srcFace2Geo = face2.getGeometryFeature();
                    if (this.firstPosition != this.secondPosition && srcFace2Geo && srcFace2Geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        face2Feature = srcFace2Geo.clone();
                        let worldMatrixFace2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face2);
                        M3D.Shapes.ShapeHelper.transform(face2Feature, worldMatrixFace2);
                        let distance = 0;
                        let projectPnt = new M3D.M3DMath.Vector3(), projectPnt1 = new M3D.M3DMath.Vector3();
                        let results = this.pointFaceDistance(this.secondPosition, face2Feature, distance, projectPnt);
                        distance = results[1];
                        //distance = width > height ? height / M3D.MeasureNames.SCREENSQUAREBOXSCALE : width / M3D.MeasureNames.SCREENSQUAREBOXSCALE;
                        //let faceBoundingBox = face2.getBoundingBox();
                        //distance = faceBoundingBox.getMinLength() / 10;
                        distance = this.getSquareBoxLineLength();
                        let normalVec = face2Feature.getNormal();
                        let originVec = face2Feature.getOrigin();
                        let direction = projectPnt.Sub(originVec);
                        //平面标记
                        let pointArray = [];
                        pointArray = this.getPointSquareBoxMark(pointArray, this.secondPosition, direction, normalVec, distance);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6, this.markingColor, pointArray[2].Sub(this.secondPosition), pointArray[3].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[2].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8, this.markingColor, pointArray[3].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8, this.secondPosition, true, true, false);
                        secondFinished = true;
                    }
                    else {
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8);
                        //this.setPoint(M3D.MeasureNames.MEASURE_POINT_FACE2, this.secondPosition);
                    }
                }
                let isNoGeoMeasure = true;
                if (firstFinished && secondFinished) {
                    let distance = 0;
                    let projectPnt0 = new M3D.M3DMath.Vector3();
                    let projectPnt1 = new M3D.M3DMath.Vector3();
                    distance = this.faceFaceDistance(face1Feature, face2Feature, distance, projectPnt0, projectPnt1);
                    if (distance >= 0) {
                        isNoGeoMeasure = false;
                        let originVec = face1Feature.getOrigin();
                        let direction = projectPnt0.Sub(originVec);
                        direction.Normalize();
                        let pnt1 = this.firstPosition.Add(direction.Multiply(distance));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_CONCAT, this.markingColor, pnt1, this.secondPosition);
                        this.distanceLineEndPosition = pnt1;
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_PROJECT, pnt1);
                        this.distances.push(distance);
                    }
                    else {
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_CONCAT);
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_PROJECT);
                    }
                }
                else {
                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_CONCAT);
                    this.removePoint(M3D.MeasureNames.MEASURE_POINT_PROJECT);
                }
                if (isNoGeoMeasure) {
                    if (this.firstShape && this.secondShape && this.firstShape !== this.secondShape
                        && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.FACE
                        && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE) {
                        let face1 = this.firstShape;
                        let face2 = this.secondShape;
                        let worldMatrixFace = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face1);
                        let worldMatrixFace2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face2);
                        let distance = 0;
                        let fCoordiante = new M3D.M3DMath.Vector3();
                        let sCoordiante = new M3D.M3DMath.Vector3();
                        let results = this.faceFaceDistance(face1, face2, distance, fCoordiante, sCoordiante, worldMatrixFace, worldMatrixFace2);
                        if (results[0]) {
                            distance = results[1];
                            this.distanceLineStartPosition = fCoordiante;
                            this.distanceLineEndPosition = sCoordiante;
                        }
                        this.distances.push(distance);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX5);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX6);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX7);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX8);
                        //this.removePoint(M3D.MeasureNames.MEASURE_POINT_FACE1);
                    }
                    else {
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            faceFaceDistance(...restOfName) {
                if (restOfName.length === 5) {
                    let planeFaceFeature0 = restOfName[0];
                    let planeFaceFeature1 = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let origin0 = planeFaceFeature0.getOrigin();
                    let origin1 = planeFaceFeature1.getOrigin();
                    let normal0 = planeFaceFeature0.getNormal();
                    let normal1 = planeFaceFeature1.getNormal();
                    let cross = normal0.CrossProduct(normal1);
                    if (cross.IsZero()) {
                        let plan0 = new M3D.M3DMath.Plane(normal0, origin0);
                        let plan1 = new M3D.M3DMath.Plane(normal1, origin1);
                        projectPnt0.copyFrom(plan1.Project(origin0));
                        projectPnt1.copyFrom(plan0.Project(origin1));
                        distance = (projectPnt0.Sub(origin0)).Length();
                    }
                    else {
                        distance = -1; //表示无效测量对象
                    }
                    return distance;
                }
                else if (restOfName.length === 7) {
                    let firstShape = restOfName[0];
                    let secondShape = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let worldMatrix1 = restOfName[5];
                    let worldMatrix2 = restOfName[6];
                    let bRet = true;
                    if (!firstShape || !secondShape) {
                        bRet = false;
                        return [bRet, distance];
                    }
                    let vertexSets = this.scene.getVertexSetManager();
                    let body1 = firstShape.getBody();
                    let body1VertexHeader = vertexSets.getVertexHeader(body1.getProto());
                    let face1VertexHeader = vertexSets.getVertexHeader(firstShape.getProto());
                    let face1Vertexset = face1VertexHeader.vertexSet;
                    let body1VertexSet = body1VertexHeader.vertexSet;
                    if (!face1Vertexset || !body1VertexSet) {
                        bRet = false;
                        return [bRet, distance];
                    }
                    let body2 = secondShape.getBody();
                    let body2VertexHeader = vertexSets.getVertexHeader(body2.getProto());
                    let face2VertexHeader = vertexSets.getVertexHeader(secondShape.getProto());
                    let face2Vertexset = face2VertexHeader.vertexSet;
                    let body2VertexSet = body2VertexHeader.vertexSet;
                    if (!face2Vertexset || !body2VertexSet) {
                        bRet = false;
                        return [bRet, distance];
                    }
                    let face1dataOffect = face1VertexHeader.indexOffset;
                    let face1dataLength = face1VertexHeader.indexLength;
                    let face1PositionArray = new Float32Array(body1VertexSet.getMeshArray(), body1VertexHeader.vertexOffset, body1VertexHeader.vertexLength);
                    let face2dataOffect = face2VertexHeader.indexOffset;
                    let face2dataLength = face2VertexHeader.indexLength;
                    let face2PositionArray = new Float32Array(body2VertexSet.getMeshArray(), body2VertexHeader.vertexOffset, body2VertexHeader.vertexLength);
                    let tempTriangle1 = new M3D.Algorithm.TriangleData();
                    let tempTriangle2 = new M3D.Algorithm.TriangleData();
                    let minTriangle1 = new M3D.Algorithm.TriangleData();
                    let minTriangle2 = new M3D.Algorithm.TriangleData();
                    let distanceTemp = -1.0;
                    let first = true;
                    let pt1 = new M3D.M3DMath.Vector3();
                    let pt2 = new M3D.M3DMath.Vector3();
                    if (face1VertexHeader.useIndex) {
                        let indexArray1 = new Uint32Array(face1Vertexset.getMeshArray(), face1dataOffect, face1dataLength);
                        for (let it = 0; it < face1dataLength; it += 3) {
                            //for (let it = (indexArray1)[0]; it <= indexArray1[0] + face1IndexCount; it += 3) {
                            let p1 = new M3D.M3DMath.Vector3();
                            let p2 = new M3D.M3DMath.Vector3();
                            let p3 = new M3D.M3DMath.Vector3();
                            p1.x = face1PositionArray[indexArray1[it] * 3 + 0];
                            p1.y = face1PositionArray[indexArray1[it] * 3 + 1];
                            p1.z = face1PositionArray[indexArray1[it] * 3 + 2];
                            p2.x = face1PositionArray[indexArray1[it + 1] * 3 + 0];
                            p2.y = face1PositionArray[indexArray1[it + 1] * 3 + 1];
                            p2.z = face1PositionArray[indexArray1[it + 1] * 3 + 2];
                            p3.x = face1PositionArray[indexArray1[it + 2] * 3 + 0];
                            p3.y = face1PositionArray[indexArray1[it + 2] * 3 + 1];
                            p3.z = face1PositionArray[indexArray1[it + 2] * 3 + 2];
                            p1 = worldMatrix1.Multiply(p1);
                            p2 = worldMatrix1.Multiply(p2);
                            p3 = worldMatrix1.Multiply(p3);
                            tempTriangle1.setVtx(p1, p2, p3);
                            if (face2VertexHeader.useIndex) {
                                let indexArray2 = new Uint32Array(face2Vertexset.getMeshArray(), face2dataOffect, face2dataLength);
                                for (let it2 = 0; it2 < face2dataLength; it2 += 3) {
                                    //for (let it2 = (indexArray2)[0]; it2 <= indexArray2[0] + face2IndexCount; it2 += 3) {
                                    let x1 = new M3D.M3DMath.Vector3();
                                    let x2 = new M3D.M3DMath.Vector3();
                                    let x3 = new M3D.M3DMath.Vector3();
                                    x1.x = face2PositionArray[indexArray2[it2] * 3 + 0];
                                    x1.y = face2PositionArray[indexArray2[it2] * 3 + 1];
                                    x1.z = face2PositionArray[indexArray2[it2] * 3 + 2];
                                    x2.x = face2PositionArray[indexArray2[it2 + 1] * 3 + 0];
                                    x2.y = face2PositionArray[indexArray2[it2 + 1] * 3 + 1];
                                    x2.z = face2PositionArray[indexArray2[it2 + 1] * 3 + 2];
                                    x3.x = face2PositionArray[indexArray2[it2 + 2] * 3 + 0];
                                    x3.y = face2PositionArray[indexArray2[it2 + 2] * 3 + 1];
                                    x3.z = face2PositionArray[indexArray2[it2 + 2] * 3 + 2];
                                    x1 = worldMatrix2.Multiply(x1);
                                    x2 = worldMatrix2.Multiply(x2);
                                    x3 = worldMatrix2.Multiply(x3);
                                    tempTriangle2.setVtx(x1, x2, x3);
                                    if (!first) {
                                        //let box1: M3D.M3DMath.BoundingBox = tempTriangle1.updataBoundingBox();
                                        let box1 = this.getTriangle1Box(p1, p2, p3);
                                        //let box2: M3D.M3DMath.BoundingBox = tempTriangle2.updataBoundingBox();
                                        let box2 = this.getTriangle1Box(x1, x2, x3);
                                        if (box1.min.x > box2.max.x) {
                                            if (box1.min.x - box2.max.x > distanceTemp)
                                                continue;
                                        }
                                        if (box1.max.x < box2.min.x) {
                                            if (box2.min.x - box1.max.x > distanceTemp)
                                                continue;
                                        }
                                        if (box1.min.y > box2.max.y) {
                                            if (box1.min.y - box2.max.y > distanceTemp)
                                                continue;
                                        }
                                        if (box1.max.y < box2.min.y) {
                                            if (box2.min.y - box1.max.y > distanceTemp)
                                                continue;
                                        }
                                        if (box1.min.z > box2.max.z) {
                                            if (box1.min.z - box2.max.z > distanceTemp)
                                                continue;
                                        }
                                        if (box1.max.z < box2.min.z) {
                                            if (box2.min.z - box1.max.z > distanceTemp)
                                                continue;
                                        }
                                    }
                                    let tempDistance = tempTriangle1.getDistance(tempTriangle2, pt1, pt2);
                                    if (distanceTemp == -1.0 || distanceTemp > tempDistance) {
                                        distanceTemp = tempDistance;
                                        //                        minTriangle1->SetVtx(p1, p2, p3);
                                        //                        minTriangle2->SetVtx(x1, x2, x3);
                                        projectPnt0.copyFrom(pt1);
                                        projectPnt1.copyFrom(pt2);
                                        first = false;
                                    }
                                }
                            }
                            else {
                                //不使用面索引
                                //TODO
                            }
                        }
                        distance = distanceTemp;
                    }
                    else {
                        //不使用面索引
                        //TODO
                    }
                    tempTriangle1 = null;
                    tempTriangle2 = null;
                    minTriangle1 = null;
                    minTriangle2 = null;
                    //    for (vector<Vector3>::iterator it = positionArray1.begin() + dataOffect1;
                    //            it != positionArray1.begin() + dataOffect1 + dataLength1 ; it=it+3)
                    //    {
                    //
                    //    }
                    return [bRet, distance];
                }
            }
            /**
             * @description 获取三角面片包围盒
             * @private
             * @param {M3D.M3DMath.Vector3} pnt1 三角面片第一个点
             * @param {M3D.M3DMath.Vector3} pnt2 三角面片第二个点
             * @param {M3D.M3DMath.Vector3} pnt3 三角面片第三个点
             * @return {M3D.M3DMath.BoundingBox}
             * @memberof CreateFaceFaceDistanceAction
             */
            getTriangle1Box(pnt1, pnt2, pnt3) {
                let boundingBox = new M3D.M3DMath.BoundingBox();
                boundingBox.min.x = (pnt1.x >= pnt2.x) ? (pnt2.x >= pnt3.x ? pnt3.x : pnt2.x) : (pnt1.x >= pnt3.x ? pnt3.x : pnt1.x);
                boundingBox.min.y = (pnt1.y >= pnt2.y) ? (pnt2.y >= pnt3.y ? pnt3.y : pnt2.y) : (pnt1.y >= pnt3.y ? pnt3.y : pnt1.y);
                boundingBox.min.z = (pnt1.z >= pnt2.z) ? (pnt2.z >= pnt3.z ? pnt3.z : pnt2.z) : (pnt1.z >= pnt3.z ? pnt3.z : pnt1.z);
                boundingBox.max.x = (pnt1.x >= pnt2.x) ? (pnt1.x >= pnt3.x ? pnt1.x : pnt3.x) : (pnt2.x >= pnt3.x ? pnt2.x : pnt3.x);
                boundingBox.max.y = (pnt1.y >= pnt2.y) ? (pnt1.y >= pnt3.y ? pnt1.y : pnt3.y) : (pnt2.y >= pnt3.y ? pnt2.y : pnt3.y);
                boundingBox.max.z = (pnt1.z >= pnt2.z) ? (pnt1.z >= pnt3.z ? pnt1.z : pnt3.z) : (pnt2.z >= pnt3.z ? pnt2.z : pnt3.z);
                return boundingBox;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             * @memberof CreateFaceFaceDistanceAction
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {string}
             * @memberof CreateFaceFaceDistanceAction
             */
            toJson() {
                return "";
            }
        }
        Actions.CreateFaceFaceDistanceAction = CreateFaceFaceDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建线面距离测量Action
         * @export
         * @class CreateLineFaceDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreateLineFaceDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreateLineFaceDistanceAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if ((this.firstShape && (this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE || (isMeasureClipEdge
                    && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE)))) {
                    let edge = this.firstShape;
                    let lineGeo = edge.getGeometryFeature();
                    if (!lineGeo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            lineGeo = edge.getGeometryFeature();
                        }
                    }
                    //if (!this.secondShape) {
                    if (lineGeo && lineGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let lineFeatrue = lineGeo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                        let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                        //添加预显线 后续将删除
                        //this.setLine(M3D.MeasureNames.MEASURE_LINE_PRE, this.markingColor, pntStartPoint, pntEndPoint);
                    }
                    //else {
                    //    //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                    //    return M3D.Actions.ActionState.Action_PartExecuted;
                    //}
                    /*} else*/
                    if (this.secondShape && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE
                        && !this.secondPosition.IsZero()) {
                        let face = this.secondShape;
                        let faceGeo = face.getGeometryFeature();
                        let tCoordinate = new M3D.M3DMath.Vector3(), sCoordinate = new M3D.M3DMath.Vector3();
                        let worldMatrixFace = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                        let worldMatrixLine = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        if (lineGeo && faceGeo && faceGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                            let faceFeature = faceGeo.clone();
                            M3D.Shapes.ShapeHelper.transform(faceFeature, worldMatrixFace);
                            let lineFeatrue = lineGeo.clone();
                            M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrixLine);
                            if (lineGeo && lineGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                                let distance = 0;
                                let projectPnt0 = new M3D.M3DMath.Vector3(), projectPnt1 = new M3D.M3DMath.Vector3();
                                let results = this.lineFaceDistance(lineFeatrue, faceFeature, distance, projectPnt0, projectPnt1);
                                distance = results[1];
                                let bRet = results[0];
                                if (bRet) {
                                    let line1Start = lineFeatrue.getStartPoint();
                                    let line1End = lineFeatrue.getEndPoint();
                                    let normalVec = line1Start.Sub(projectPnt0);
                                    normalVec.Normalize();
                                    this.distanceLineStartPosition = this.secondPosition.Add(normalVec.Multiply(distance));
                                    let tempdis = 0;
                                    let minPoin = new M3D.M3DMath.Vector3();
                                    let inLine;
                                    tempdis = this.pointLineDistance(this.distanceLineStartPosition, lineFeatrue, tempdis, inLine, minPoin);
                                    sCoordinate = minPoin;
                                    let projecPnt = new M3D.M3DMath.Vector3();
                                    if (!this.pntInSegment(minPoin, line1Start, line1End, projecPnt)) {
                                        //延长虚线
                                        this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, minPoin, projecPnt.Clone(), M3D.Shapes.CurvePatternMode.DashedLine);
                                    }
                                    else {
                                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                                    }
                                    let tmpDistance = 0;
                                    let result = this.pointFaceDistance(this.secondPosition, faceFeature, distance, projecPnt);
                                    tmpDistance = result[1];
                                    this.distanceLineEndPosition = this.secondPosition;
                                    //let faceBoundingBox = face.getBoundingBox();
                                    //tmpDistance = faceBoundingBox.getMinLength() / 10;
                                    tmpDistance = this.getSquareBoxLineLength();
                                    let lineVector = (line1Start.Sub(line1End)).Normalized().Multiply(tmpDistance * 3);
                                    //线垂足横线
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, lineVector, lineVector.Multiply(-1));
                                    this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, sCoordinate, true, true, false);
                                    //线垂足竖线
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_FACE_LINE, this.markingColor, this.distanceLineStartPosition, sCoordinate);
                                    let originVec = faceFeature.getOrigin();
                                    let direction = projecPnt.Sub(originVec);
                                    //平面标记
                                    let pointArray = [];
                                    pointArray = this.getPointSquareBoxMark(pointArray, this.secondPosition, direction, normalVec, tmpDistance);
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.markingColor, pointArray[2].Sub(this.secondPosition), pointArray[3].Sub(this.secondPosition));
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[2].Sub(this.secondPosition));
                                    this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.markingColor, pointArray[3].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                                    this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.secondPosition, true, true, false);
                                    this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.secondPosition, true, true, false);
                                    this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.secondPosition, true, true, false);
                                    this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.secondPosition, true, true, false);
                                    this.distances.push(distance);
                                }
                                else {
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_1);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_2);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                                    this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                                    let distance = 0;
                                    this.distanceLineStartPosition = this.distanceLineStartPosition ? this.distanceLineStartPosition : new M3D.M3DMath.Vector3();
                                    this.distanceLineEndPosition = this.distanceLineEndPosition ? this.distanceLineEndPosition : new M3D.M3DMath.Vector3();
                                    let result = this.lineFaceDistance(face, edge, distance, this.distanceLineStartPosition, this.distanceLineEndPosition, worldMatrixLine, worldMatrixFace);
                                    distance = result[1];
                                    this.distances.push(distance);
                                }
                                //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                            }
                            else {
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_1);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_2);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                                let distance = 0;
                                this.distanceLineStartPosition = this.distanceLineStartPosition ? this.distanceLineStartPosition : new M3D.M3DMath.Vector3();
                                this.distanceLineEndPosition = this.distanceLineEndPosition ? this.distanceLineEndPosition : new M3D.M3DMath.Vector3();
                                let result = this.lineFaceDistance(face, edge, distance, this.distanceLineStartPosition, this.distanceLineEndPosition, worldMatrixLine, worldMatrixFace);
                                distance = result[1];
                                this.distances.push(distance);
                            }
                        }
                        else {
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_1);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_2);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                            let distance = 0;
                            this.distanceLineStartPosition = this.distanceLineStartPosition ? this.distanceLineStartPosition : new M3D.M3DMath.Vector3();
                            this.distanceLineEndPosition = this.distanceLineEndPosition ? this.distanceLineEndPosition : new M3D.M3DMath.Vector3();
                            let result = this.lineFaceDistance(face, edge, distance, this.distanceLineStartPosition, this.distanceLineEndPosition, worldMatrixLine, worldMatrixFace);
                            distance = result[1];
                            this.distances.push(distance);
                        }
                    }
                    else {
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return M3D.Actions.ActionState.Action_PartExecuted;
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            lineFaceDistance(...restOfName) {
                let methodName = this.lineFaceDistance.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (restOfName.length === 5) {
                    let lineFeature = restOfName[0];
                    let planeFaceFeature = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let ret = true;
                    let lineDirection = lineFeature.getEndPoint().Sub(lineFeature.getStartPoint());
                    let faceNormal = planeFaceFeature.getNormal();
                    let projectPnt;
                    if (lineDirection.DotProduct(faceNormal) >= -0.001
                        && lineDirection.DotProduct(faceNormal) <= 0.001) {
                        distance = this.pointFaceDistance(lineFeature.getStartPoint(), planeFaceFeature, distance, projectPnt0)[1];
                        distance = this.pointFaceDistance(lineFeature.getEndPoint(), planeFaceFeature, distance, projectPnt1)[1];
                    }
                    else {
                        ret = false;
                    }
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return [ret, distance];
                }
                else if (restOfName.length === 7) {
                    let face = restOfName[0];
                    let edge = restOfName[1];
                    let distance = restOfName[2];
                    let projectPnt0 = restOfName[3];
                    let projectPnt1 = restOfName[4];
                    let worldMatrix1 = restOfName[5];
                    let worldMatrix2 = restOfName[6];
                    let bRet = true;
                    let vertexSets = this.scene.getVertexSetManager();
                    let body1 = face.getBody();
                    let body1VertexHeader = vertexSets.getVertexHeader(body1.getProto());
                    let faceVertexHeader = vertexSets.getVertexHeader(face.getProto());
                    let faceVertexset = faceVertexHeader.vertexSet;
                    let body1VertexSet = body1VertexHeader.vertexSet;
                    if (!faceVertexset || !body1VertexSet) {
                        bRet = false;
                        return [bRet, distance];
                    }
                    else {
                        let useIndex = faceVertexHeader.useIndex;
                        if (!useIndex) {
                            bRet = false;
                            return [bRet, distance];
                        }
                    }
                    let facedataOffect = faceVertexHeader.indexOffset;
                    let facedataLength = faceVertexHeader.indexLength;
                    let facePositionArray = new Float32Array(body1VertexSet.getMeshArray(), body1VertexHeader.vertexOffset, body1VertexHeader.vertexLength);
                    if (edge.getShapeType() === M3D.Shapes.ShapeType.EDGE) {
                        //线
                        let body2 = edge.getBody();
                        let body2VertexHeader = vertexSets.getVertexHeader(body2.getProto());
                        let edgeVertexHeader = vertexSets.getVertexHeader(edge.getProto());
                        let edgeVertexset = edgeVertexHeader.vertexSet;
                        let body2VertexSet = body2VertexHeader.vertexSet;
                        if (!edgeVertexset || !body2VertexSet) {
                            bRet = false;
                            return [bRet, distance];
                        }
                        let edgedataOffect = edgeVertexHeader.indexOffset;
                        let edgedataLength = edgeVertexHeader.indexLength;
                        let meshBuffer = body2VertexSet.getMeshArray();
                        let indexBuffer = edgeVertexset.getMeshArray();
                        let pEdgeIndexArray = new Uint32Array(indexBuffer, edgedataOffect, edgedataLength);
                        let edgePositionArray = new Float32Array(meshBuffer, body2VertexHeader.vertexOffset, body2VertexHeader.vertexLength);
                        let tempTriangle = new M3D.Algorithm.TriangleData();
                        let minTriangle = new M3D.Algorithm.TriangleData();
                        let minLine = new M3D.Shapes.Line();
                        let distanceTemp = -1.0;
                        let result = true;
                        let pt1 = new M3D.M3DMath.Vector3();
                        let pt2 = new M3D.M3DMath.Vector3();
                        for (let it = 0; it < edgedataLength; it += 2) {
                            let p1 = new M3D.M3DMath.Vector3();
                            let p2 = new M3D.M3DMath.Vector3();
                            p1.x = edgePositionArray[pEdgeIndexArray[it] * 3 + 0];
                            p1.y = edgePositionArray[pEdgeIndexArray[it] * 3 + 1];
                            p1.z = edgePositionArray[pEdgeIndexArray[it] * 3 + 2];
                            p2.x = edgePositionArray[pEdgeIndexArray[it + 1] * 3 + 0];
                            p2.y = edgePositionArray[pEdgeIndexArray[it + 1] * 3 + 1];
                            p2.z = edgePositionArray[pEdgeIndexArray[it + 1] * 3 + 2];
                            p1 = worldMatrix1.Multiply(p1);
                            p2 = worldMatrix1.Multiply(p2);
                            let indexArray = new Uint32Array(faceVertexset.getMeshArray(), facedataOffect, facedataLength);
                            for (let it2 = 0; it2 < facedataLength; it2 += 3) {
                                //for (let it2 = (indexArray2)[0]; it2 <= indexArray2[0] + face2IndexCount; it2 += 3) {
                                let x1 = new M3D.M3DMath.Vector3();
                                let x2 = new M3D.M3DMath.Vector3();
                                let x3 = new M3D.M3DMath.Vector3();
                                x1.x = facePositionArray[indexArray[it2] * 3 + 0];
                                x1.y = facePositionArray[indexArray[it2] * 3 + 1];
                                x1.z = facePositionArray[indexArray[it2] * 3 + 2];
                                x2.x = facePositionArray[indexArray[it2 + 1] * 3 + 0];
                                x2.y = facePositionArray[indexArray[it2 + 1] * 3 + 1];
                                x2.z = facePositionArray[indexArray[it2 + 1] * 3 + 2];
                                x3.x = facePositionArray[indexArray[it2 + 2] * 3 + 0];
                                x3.y = facePositionArray[indexArray[it2 + 2] * 3 + 1];
                                x3.z = facePositionArray[indexArray[it2 + 2] * 3 + 2];
                                x1 = worldMatrix2.Multiply(x1);
                                x2 = worldMatrix2.Multiply(x2);
                                x3 = worldMatrix2.Multiply(x3);
                                tempTriangle.setVtx(x1, x2, x3);
                                let distance = tempTriangle.getDistance(p1, p2, pt1, pt2);
                                if (distanceTemp == -1.0 || distanceTemp > distance) {
                                    distanceTemp = distance;
                                    projectPnt0.copyFrom(pt1);
                                    projectPnt1.copyFrom(pt2);
                                    result = true;
                                }
                            }
                            distance = distanceTemp;
                            tempTriangle = null;
                            minTriangle = null;
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return [bRet, distance];
                        }
                    }
                    else if (edge.getShapeType() === M3D.Shapes.ShapeType.SECTION_EDGE) {
                        //线
                        let tempTriangle = new M3D.Algorithm.TriangleData();
                        let minTriangle = new M3D.Algorithm.TriangleData();
                        let minLine = new M3D.Shapes.Line();
                        let distanceTemp = -1.0;
                        let result = true;
                        let pt1 = new M3D.M3DMath.Vector3();
                        let pt2 = new M3D.M3DMath.Vector3();
                        let pntPoints = edge.getSectionLine().getData();
                        let dataLength = pntPoints.length;
                        for (let i = 0; i < dataLength - 1; i++) {
                            let p1 = pntPoints[i];
                            let p2 = pntPoints[i + 1];
                            let indexArray = new Uint32Array(faceVertexset.getMeshArray(), facedataOffect, facedataLength);
                            for (let it2 = 0; it2 < facedataLength; it2 += 3) {
                                //for (let it2 = (indexArray2)[0]; it2 <= indexArray2[0] + face2IndexCount; it2 += 3) {
                                let x1 = new M3D.M3DMath.Vector3();
                                let x2 = new M3D.M3DMath.Vector3();
                                let x3 = new M3D.M3DMath.Vector3();
                                x1.x = facePositionArray[indexArray[it2] * 3 + 0];
                                x1.y = facePositionArray[indexArray[it2] * 3 + 1];
                                x1.z = facePositionArray[indexArray[it2] * 3 + 2];
                                x2.x = facePositionArray[indexArray[it2 + 1] * 3 + 0];
                                x2.y = facePositionArray[indexArray[it2 + 1] * 3 + 1];
                                x2.z = facePositionArray[indexArray[it2 + 1] * 3 + 2];
                                x3.x = facePositionArray[indexArray[it2 + 2] * 3 + 0];
                                x3.y = facePositionArray[indexArray[it2 + 2] * 3 + 1];
                                x3.z = facePositionArray[indexArray[it2 + 2] * 3 + 2];
                                x1 = worldMatrix2.Multiply(x1);
                                x2 = worldMatrix2.Multiply(x2);
                                x3 = worldMatrix2.Multiply(x3);
                                tempTriangle.setVtx(x1, x2, x3);
                                let distance = tempTriangle.getDistance(p1, p2, pt1, pt2);
                                if (distanceTemp == -1.0 || distanceTemp > distance) {
                                    distanceTemp = distance;
                                    projectPnt0.copyFrom(pt1);
                                    projectPnt1.copyFrom(pt2);
                                    result = true;
                                }
                            }
                            distance = distanceTemp;
                            tempTriangle = null;
                            minTriangle = null;
                            Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                            return [bRet, distance];
                        }
                    }
                }
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             * @memberof CreateLineFaceDistanceAction
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             * @memberof CreateLineFaceDistanceAction
             */
            toJson() {
                return "";
            }
        }
        Actions.CreateLineFaceDistanceAction = CreateLineFaceDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建线线测量Action
         * @export
         * @class CreateLineLineDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreateLineLineDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreateLineLineDistanceAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if ((this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE)
                    || this.isSectionEdgeEnable(true, this.firstShape, this.secondShape)) {
                    let edge = this.firstShape;
                    let geo = edge.getGeometryFeature();
                    if (!geo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            geo = edge.getGeometryFeature();
                        }
                    }
                    if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let lineFeatrue = geo.clone();
                        let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                        M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                        let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                        let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                        //添加预显线 后续将删除
                        //this.setLine(M3D.MeasureNames.MEASURE_LINE_PRE, this.markingColor, pntStartPoint, pntEndPoint);
                    }
                    //else {
                    //    //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                    //    return M3D.Actions.ActionState.Action_PartExecuted;
                    //}
                }
                if ((this.firstShape && this.secondShape && this.firstShape != this.secondShape
                    && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE)
                    || this.isSectionEdgeEnable(false, this.firstShape, this.secondShape)) {
                    let firstEdge = this.firstShape;
                    let secondEdge = this.secondShape;
                    let firstGeo = firstEdge.getGeometryFeature();
                    let secondGeo = secondEdge.getGeometryFeature();
                    if (!firstGeo) {
                        let geoType = M3D.Shapes.ShapeHelper.getLineType(firstEdge);
                        firstGeo = firstEdge.getGeometryFeature();
                    }
                    if (!secondGeo) {
                        let geoType = M3D.Shapes.ShapeHelper.getLineType(secondEdge);
                        secondGeo = secondEdge.getGeometryFeature();
                    }
                    let isNoGeoMeasure = true;
                    if (firstGeo && firstGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE
                        && secondGeo && secondGeo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                        let firstLineFeatrue = firstGeo.clone();
                        let secondLineFeatrue = secondGeo.clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        let worldMatrix2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, worldMatrix1);
                        M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, worldMatrix2);
                        let distance = 0;
                        let projectPnt1 = new M3D.M3DMath.Vector3();
                        let projectPnt2 = new M3D.M3DMath.Vector3();
                        let parallel = true;
                        let result = this.lineLineDistance(firstLineFeatrue, secondLineFeatrue, distance, projectPnt1, projectPnt2, parallel);
                        distance = result[0];
                        parallel = result[1];
                        this.distanceLineStartPosition = projectPnt1;
                        this.distanceLineEndPosition = projectPnt2;
                        let pointStartPoint1 = firstLineFeatrue.getStartPoint();
                        let pointEndPoint1 = firstLineFeatrue.getEndPoint();
                        let pointStartPoint2 = secondLineFeatrue.getStartPoint();
                        let pointEndPoint2 = secondLineFeatrue.getEndPoint();
                        if (parallel) {
                            isNoGeoMeasure = false;
                            //判断得到的垂足是否在两条线段上，只要一条不在，就用四个端点之间的距离来判断。
                            let sdis1, edis1, sdis2, edis2;
                            let in1, in2;
                            in1 = this.pntInSegment(projectPnt1, firstLineFeatrue);
                            in2 = this.pntInSegment(projectPnt2, secondLineFeatrue);
                            let firstLineStartToSecondLineMinDistance = 0.0;
                            let firstStartProjectPnt = new M3D.M3DMath.Vector3();
                            firstLineStartToSecondLineMinDistance = this.pointLineDistance(pointStartPoint2, pointEndPoint2, pointStartPoint1, firstLineStartToSecondLineMinDistance, firstStartProjectPnt);
                            let firstLineEndToSecondLineMinDistance = 0.0;
                            let firstEndProjectPnt = new M3D.M3DMath.Vector3();
                            firstLineEndToSecondLineMinDistance = this.pointLineDistance(pointStartPoint2, pointEndPoint2, pointEndPoint1, firstLineEndToSecondLineMinDistance, firstEndProjectPnt);
                            let secondLineStartToFirstLineMinDistance = 0.0;
                            let secondStartProjectPnt = new M3D.M3DMath.Vector3();
                            secondLineStartToFirstLineMinDistance = this.pointLineDistance(pointStartPoint1, pointEndPoint1, pointStartPoint2, secondLineStartToFirstLineMinDistance, secondStartProjectPnt);
                            let secondLineEndToFirstLineMinDistance = 0.0;
                            let secondEndProjectPnt = new M3D.M3DMath.Vector3();
                            secondLineEndToFirstLineMinDistance = this.pointLineDistance(pointStartPoint1, pointEndPoint1, pointEndPoint2, secondLineEndToFirstLineMinDistance, secondEndProjectPnt);
                            let minDistance = 0;
                            let minPoint1 = new M3D.M3DMath.Vector3(), minPoint2 = new M3D.M3DMath.Vector3();
                            sdis2 = secondLineStartToFirstLineMinDistance;
                            edis2 = secondLineEndToFirstLineMinDistance;
                            sdis1 = firstLineStartToSecondLineMinDistance;
                            edis1 = firstLineEndToSecondLineMinDistance;
                            if (sdis2 > edis2) {
                                minDistance = edis2;
                                minPoint1 = secondEndProjectPnt;
                                minPoint2 = pointEndPoint2;
                            }
                            else {
                                minDistance = sdis2;
                                minPoint1 = secondStartProjectPnt;
                                minPoint2 = pointStartPoint2;
                            }
                            if (minDistance > sdis1) {
                                minDistance = sdis1;
                                minPoint1 = firstStartProjectPnt;
                                minPoint2 = pointStartPoint1;
                            }
                            if (minDistance > edis1) {
                                minDistance = edis1;
                                minPoint1 = pointEndPoint1;
                                minPoint2 = firstEndProjectPnt;
                            }
                            // 计算临时测量结果显示位置
                            //camera射线
                            let cameraRay = this.scene.getCurrentViewPort().getScreenRay(0, 0);
                            let lineCenter = (projectPnt1.Add(projectPnt2)).Divide(2).Clone();
                            //构造投影平面
                            let projPlane = new M3D.M3DMath.Plane(cameraRay.GetDirection(), lineCenter);
                            let pntInPlane = projPlane.Project(cameraRay.GetOrigin());
                            if (!in1) {
                                let extern1 = new M3D.M3DMath.Vector3();
                                if ((projectPnt1.Sub(pointStartPoint1)).Length() > (projectPnt1.Sub(pointEndPoint1)).Length()) {
                                    extern1 = pointEndPoint1;
                                }
                                else {
                                    extern1 = pointStartPoint1;
                                }
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, extern1, projectPnt1, M3D.Shapes.CurvePatternMode.DashedLine);
                            }
                            else {
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                            }
                            if (!in2) {
                                let extern2 = new M3D.M3DMath.Vector3();
                                if ((projectPnt2.Sub(pointStartPoint2)).Length()
                                    > (projectPnt2.Sub(pointEndPoint2)).Length()) {
                                    extern2 = pointEndPoint2;
                                }
                                else {
                                    extern2 = pointStartPoint2;
                                }
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND2, this.extendLineColor, extern2, projectPnt2, M3D.Shapes.CurvePatternMode.DashedLine);
                            }
                            else {
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND2);
                            }
                            let line1Vector = pointStartPoint1.Sub(pointEndPoint1).Normalized().Multiply(this.getSquareBoxLineLength() * 3);
                            let line2Vector = pointStartPoint2.Sub(pointEndPoint2).Normalized().Multiply(this.getSquareBoxLineLength() * 3);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, line1Vector, line1Vector.Multiply(-1)); //测量线1
                            this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, projectPnt1, true, true, false);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_2, this.markingColor, line2Vector, line2Vector.Multiply(-1)); //测量线2
                            this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_2, projectPnt2, true, true, false);
                            this.distances.push(distance);
                            let isMin = M3D.M3DMath.Abs(distance - minDistance) > 0.000001 && this.isShowMinDistance;
                            if (isMin) {
                                this.distances.push(minDistance);
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MIN_1, minPoint1);
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MIN_2, minPoint2);
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_MIN, this.minDistanceColor, minPoint1, minPoint2);
                            }
                            //this.removeLine(M3D.MeasureNames.MEASURE_LINE_PRE);
                        }
                    }
                    if (isNoGeoMeasure) {
                        //let firstLineFeatrue: M3D.Shapes.LineFeature = (<M3D.Shapes.LineFeature>firstGeo).clone();
                        //let secondLineFeatrue: M3D.Shapes.LineFeature = (<M3D.Shapes.LineFeature>secondGeo).clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(firstEdge);
                        let worldMatrix2 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(secondEdge);
                        //M3D.Shapes.ShapeHelper.transform(firstLineFeatrue, worldMatrix1);
                        //M3D.Shapes.ShapeHelper.transform(secondLineFeatrue, worldMatrix2);
                        let distance = 0;
                        let result = this.lineLineDistance(firstEdge, secondEdge, this.firstPosition, this.secondPosition, distance, worldMatrix1, worldMatrix2);
                        distance = result[0];
                        this.distanceLineStartPosition = result[1];
                        this.distanceLineEndPosition = result[2];
                        this.distances.push(distance);
                    }
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             */
            toJson() {
                return "";
            }
        }
        Actions.CreateLineLineDistanceAction = CreateLineLineDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 点面距离测量Action
         * @export
         * @class CreatePointFaceDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreatePointFaceDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreatePointFaceDistanceAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                if (!this.firstPosition && this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT) {
                    this.firstPosition = this.firstShape.getPosition();
                }
                if (!this.firstPosition)
                    return M3D.Actions.ActionState.Action_PartExecuted;
                //if (!this.secondPosition) {
                this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.firstPosition);
                this.distanceLineStartPosition = this.firstPosition;
                //}
                if (this.secondShape && this.secondPosition && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.FACE) {
                    let face = this.secondShape;
                    let geo = face.getGeometryFeature();
                    if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE) {
                        let faceFeatrue = geo.clone();
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                        let faceBoundingBox = face.getBoundingBox();
                        M3D.Shapes.ShapeHelper.transform(faceFeatrue, worldMatrix1);
                        let distance = 0;
                        let projectPnt = new M3D.M3DMath.Vector3();
                        let result = this.pointFaceDistance(this.secondPosition, faceFeatrue, distance, projectPnt);
                        distance = result[1];
                        projectPnt = result[2];
                        //取视口宽高中小的固定比例
                        //let width: number = 0, height: number = 0;
                        //width = this.scene.getCurrentViewPort().getRect().Size().x;
                        //height = this.scene.getCurrentViewPort().getRect().Size().y;
                        //distance = width > height ? height / 200 : width / 200;
                        //distance = faceBoundingBox.getMinLength() / 10;
                        distance = this.getSquareBoxLineLength();
                        let normalVec = faceFeatrue.getNormal();
                        let originVec = faceFeatrue.getOrigin();
                        let direction = projectPnt.Sub(originVec);
                        //平面标记
                        let pointArray = [];
                        pointArray = this.getPointSquareBoxMark(pointArray, this.secondPosition, direction, normalVec, distance);
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.markingColor, pointArray[2].Sub(this.secondPosition), pointArray[3].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.markingColor, pointArray[0].Sub(this.secondPosition), pointArray[2].Sub(this.secondPosition));
                        this.setLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.markingColor, pointArray[3].Sub(this.secondPosition), pointArray[1].Sub(this.secondPosition));
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3, this.secondPosition, true, true, false);
                        this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4, this.secondPosition, true, true, false);
                        if (this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT || this.firstPosition) {
                            let planeTemp = new M3D.M3DMath.Plane(normalVec, originVec);
                            let pntProject = planeTemp.Project(this.firstPosition);
                            let length = this.firstPosition.Sub(pntProject).Length();
                            //最近距离，最远距离
                            let inverseMat = worldMatrix1.Inverse();
                            let modelVec = inverseMat.Multiply(this.firstPosition);
                            let minDistance, maxDistance;
                            let maxPoint = new M3D.M3DMath.Vector3();
                            let minPoint = new M3D.M3DMath.Vector3();
                            let results = this.pointFaceDistance(modelVec, face, minDistance, maxDistance, minPoint, maxPoint);
                            minDistance = results[0];
                            maxDistance = results[1];
                            minPoint = worldMatrix1.Multiply(minPoint).Clone();
                            maxPoint = worldMatrix1.Multiply(maxPoint).Clone();
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_CONCAT, this.markingColor, this.firstPosition, pntProject, M3D.Shapes.CurvePatternMode.DashedLine);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, this.secondPosition, pntProject, M3D.Shapes.CurvePatternMode.DashedLine);
                            let isMin = !pntProject.Equals(minPoint) && this.isShowMinDistance;
                            let isMax = minDistance > 0 && this.isShowMaxDistance;
                            if (isMin) {
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MIN, minPoint);
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_MIN, this.minDistanceColor, this.firstPosition, minPoint);
                            }
                            if (isMax) {
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MAX, maxPoint);
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_MAX, this.maxDistanceColor, this.firstPosition, maxPoint);
                            }
                            //测量结果单位转换
                            minDistance = isMin ? minDistance : -1.0;
                            maxDistance = isMax ? maxDistance : -1.0;
                            this.distanceLineEndPosition = pntProject;
                            this.distances.push(length);
                            this.distances.push(minDistance);
                            this.distances.push(maxDistance);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START);
                        }
                    }
                    else if (geo == null || (geo && geo.getFeatureType() != M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_PLANEFACE)) {
                        let worldMatrix1 = M3D.Shapes.ShapeHelper.getShapeWorldTransform(face);
                        let inverseMat = worldMatrix1.Inverse();
                        if ((!this.firstShape || this.firstShape.getShapeType() != M3D.Shapes.ShapeType.POINT) && !this.firstPosition) {
                            return M3D.Actions.ActionState.Action_Error;
                        }
                        let modelVec = inverseMat.Multiply(this.firstPosition);
                        let minDistance, maxDistance;
                        let maxPoint = new M3D.M3DMath.Vector3();
                        let minPoint = new M3D.M3DMath.Vector3();
                        let results = this.pointFaceDistance(modelVec, face, minDistance, maxDistance, minPoint, maxPoint);
                        minDistance = results[0];
                        maxDistance = results[1];
                        minPoint = worldMatrix1.Multiply(minPoint);
                        let length = minPoint.Sub(this.firstPosition).Length();
                        this.distances.push(length);
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_MIN);
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_MAX);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_MIN);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_MAX);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_CONCAT);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX1);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX2);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX3);
                        this.removeLine(M3D.MeasureNames.MEASURE_LINE_SQUAREBOX4);
                        //if (minPoint != null) {
                        //    this.setPoint(M3D.MeasureNames.MEASURE_POINT_MIN, minPoint);
                        //    this.setLine(M3D.MeasureNames.MEASURE_LINE_MIN, this.leaderColor, this.firstPosition, minPoint);
                        //}
                        //if (maxPoint != null) {
                        //    this.setPoint(M3D.MeasureNames.MEASURE_POINT_MAX, maxPoint);
                        //    this.setLine(M3D.MeasureNames.MEASURE_LINE_MAX, this.leaderColor, this.firstPosition, maxPoint);
                        //}
                        this.distanceLineEndPosition = minPoint;
                        this.removePoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START);
                    }
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             */
            toJson() {
                return "";
            }
        }
        Actions.CreatePointFaceDistanceAction = CreatePointFaceDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 点线距离测量Action
         * @export
         * @class CreatePointLineDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreatePointLineDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreatePointLineDistanceAction
             */
            setShapeElement() {
                let methodName = this.setShapeElement.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let isMeasureClipEdge = false;
                isMeasureClipEdge = M3D.Config.ViewPortParameters.getInstance().getBoolParameter(M3D.Config.ConfigKeys.ITEM_IS_MEASURE_CLIP_EDGE);
                if (!this.firstPosition && this.firstShape && this.firstShape.getShapeType() == M3D.Shapes.ShapeType.POINT) {
                    this.firstPosition = this.firstShape.getPosition();
                }
                if (!this.firstPosition)
                    return M3D.Actions.ActionState.Action_PartExecuted;
                //if (!this.secondPosition) {
                this.distanceLineStartPosition = this.firstPosition;
                this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.firstPosition);
                //}
                if (!this.secondPosition) {
                    this.secondPosition = new M3D.M3DMath.Vector3();
                }
                if (this.secondShape && (this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                    || (isMeasureClipEdge && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE))) {
                    let edge = this.secondShape;
                    let geo = edge.getGeometryFeature();
                    if (!geo) {
                        let type = M3D.Shapes.ShapeHelper.getLineType(edge);
                        if (type == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            geo = edge.getGeometryFeature();
                        }
                    }
                    if (this.secondShape && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.EDGE
                        || (isMeasureClipEdge && this.secondShape.getShapeType() == M3D.Shapes.ShapeType.SECTION_EDGE)) {
                        if (geo && geo.getFeatureType() == M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE) {
                            let lineFeatrue = geo.clone();
                            let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                            M3D.Shapes.ShapeHelper.transform(lineFeatrue, worldMatrix);
                            // 垂足
                            let inLine;
                            let distance = 0;
                            distance = this.pointLineDistance(this.firstPosition, lineFeatrue, distance, inLine, this.secondPosition);
                            this.distanceLineEndPosition = this.secondPosition;
                            let pntStartPoint = lineFeatrue.getStartPoint(); //!< Line起点
                            let pntEndPoint = lineFeatrue.getEndPoint(); //!< Line终点
                            //this.setLine("measureLine", M3D.M3DMath.Color.RED(), pntStartPoint, pntEndPoint);
                            let startD = this.firstPosition.Sub(pntStartPoint).Length();
                            let endD = this.firstPosition.Sub(pntEndPoint).Length();
                            let minDistance;
                            let maxDistance;
                            let maxPoint = new M3D.M3DMath.Vector3();
                            let minPoint = new M3D.M3DMath.Vector3();
                            if (startD > endD) {
                                maxDistance = startD;
                                minDistance = endD;
                                maxPoint = pntStartPoint;
                                minPoint = pntEndPoint;
                            }
                            else {
                                maxDistance = endD;
                                minDistance = startD;
                                minPoint = pntStartPoint;
                                maxPoint = pntEndPoint;
                            }
                            let isMin = false;
                            let projecPoint = new M3D.M3DMath.Vector3();
                            if (!(this.pntInSegment(this.secondPosition, pntStartPoint, pntEndPoint, projecPoint))) {
                                //最近点
                                isMin = true;
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_EXTEND, this.extendLineColor, projecPoint, this.secondPosition, M3D.Shapes.CurvePatternMode.DashedLine);
                                //标记点
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_EXTEND, this.secondPosition);
                            }
                            else {
                                this.removePoint(M3D.MeasureNames.MEASURE_POINT_EXTEND);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                            }
                            isMin = isMin && this.isShowMinDistance;
                            if (isMin) {
                                //最近距离点
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MIN, minPoint);
                                //最近距离线
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_MIN, this.minDistanceColor, minPoint, this.firstPosition);
                            }
                            else {
                                this.removePoint(M3D.MeasureNames.MEASURE_POINT_MIN);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_MIN);
                            }
                            let isMax = this.isShowMaxDistance;
                            if (isMax) {
                                //最远距离点
                                this.setPoint(M3D.MeasureNames.MEASURE_POINT_MAX, maxPoint);
                                //最远距离线
                                this.setLine(M3D.MeasureNames.MEASURE_LINE_MAX, this.maxDistanceColor, maxPoint, this.firstPosition);
                            }
                            else {
                                this.removePoint(M3D.MeasureNames.MEASURE_POINT_MAX);
                                this.removeLine(M3D.MeasureNames.MEASURE_LINE_MAX);
                            }
                            //添加线标识
                            let lineLength = (pntEndPoint.Sub(pntStartPoint)).Normalized().Multiply(this.getSquareBoxLineLength() * 3);
                            let lineStart = this.secondPosition.Add(lineLength);
                            let lineEnd = this.secondPosition.Sub(lineLength);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, lineLength, lineLength.Multiply(-1));
                            this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.secondPosition, true, true, false);
                            //距离
                            minDistance = isMin ? minDistance : -1.0;
                            maxDistance = isMax ? maxDistance : -1.0;
                            this.distances.push(distance);
                            this.distances.push(minDistance);
                            this.distances.push(maxDistance);
                            //this.removeLine(M3D.MeasureNames.MEASURE_LINE_MARK_1);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START);
                        }
                        else if (geo == null || (geo && geo.getFeatureType() != M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_LINE)) {
                            let worldMatrix = M3D.Shapes.ShapeHelper.getShapeWorldTransform(edge);
                            // 垂足
                            let closestCoordiant = new M3D.M3DMath.Vector3();
                            let startPoint = new M3D.M3DMath.Vector3();
                            let endPoint = new M3D.M3DMath.Vector3();
                            let inLine;
                            let distance;
                            let result = this.pointLineDistance(edge, this.firstPosition, distance, closestCoordiant, worldMatrix, startPoint, endPoint);
                            distance = result[0];
                            startPoint = result[1];
                            endPoint = result[2];
                            let dir = (endPoint.Sub(startPoint)).Normalized().Multiply(this.getSquareBoxLineLength() * 3);
                            let _startPoint = closestCoordiant.Sub(dir);
                            let _endPoint = closestCoordiant.Add(dir);
                            this.setLine(M3D.MeasureNames.MEASURE_LINE_MARK_1, this.markingColor, dir, dir.Multiply(-1));
                            this.setLineProperty(M3D.MeasureNames.MEASURE_LINE_MARK_1, closestCoordiant, true, true, false);
                            this.distanceLineEndPosition = closestCoordiant;
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_EXTEND);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_MIN);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_MIN);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_MAX);
                            this.removeLine(M3D.MeasureNames.MEASURE_LINE_MAX);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_EXTEND);
                            //距离
                            this.distances.push(distance);
                            this.removePoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START);
                        }
                    }
                    else {
                        //释放
                        Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                        return M3D.Actions.ActionState.Action_Error;
                    }
                }
                else {
                    Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             */
            toJson() {
                return "";
            }
        }
        Actions.CreatePointLineDistanceAction = CreatePointLineDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建点点距离测量Action
         * @export
         * @class CreatePointPointDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreatePointPointDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreatePointPointDistanceAction
             */
            setShapeElement() {
                if (this.firstPosition) {
                    this.distanceLineStartPosition = this.firstPosition;
                    if (this.measure.getMeasureType() === M3D.Shapes.MeasureType.PNT_PNT_DISTANCE && this.measure.getFirstParentModel()) {
                        // 设置第一个点
                        const worldTransformInverse = this.measure.getFirstParentModel().getWorldTransform().Inverse();
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, worldTransformInverse.Multiply(this.distanceLineStartPosition), 1);
                    }
                    else {
                        this.setPoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START, this.distanceLineStartPosition, 1);
                    }
                    //return M3D.Actions.ActionState.Action_PartExecuted;
                }
                if (this.secondPosition) {
                    //距离
                    this.distanceLineEndPosition = this.secondPosition;
                    if (!this.distanceLineStartPosition.Equals(new M3D.M3DMath.Vector3) && !this.distanceLineEndPosition.Equals(new M3D.M3DMath.Vector3)) {
                        this.distances.push(this.firstPosition.Sub(this.secondPosition).Length());
                    }
                    this.removePoint(M3D.MeasureNames.MEASURE_POINT_DISTANCELINE_START);
                }
                else {
                    return M3D.Actions.ActionState.Action_PartExecuted;
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             * @memberof CreatePointPointDistanceAction
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             * @memberof CreatePointPointDistanceAction
             */
            toJson() {
                return "";
            }
        }
        Actions.CreatePointPointDistanceAction = CreatePointPointDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 厚度测量Action
         * @export
         * @class CreateThicknessDistanceAction
         * @extends {CreateDistanceAction}
         */
        class CreateThicknessDistanceAction extends Actions.CreateDistanceAction {
            /**
             * @public
             * @description 根据传入的参数计算并设置测量数据
             * @return {M3D.Actions.ActionState}
             * @memberof CreateThicknessDistanceAction
             */
            setShapeElement() {
                if (!this.modelPath1 || this.modelPath1 === "")
                    return M3D.Actions.ActionState.Action_Error;
                let shape = this.scene.getShapes().getShapeByPlcPath(this.modelPath1);
                if (shape && shape.getShapeType() === M3D.Shapes.ShapeType.MODEL) {
                    this.distanceLineStartPosition = this.firstPosition;
                    // 根据方向和点 构建射线
                    let ray = new M3D.M3DMath.Ray(this.firstPosition, this.normalDirection);
                    let rayPickAction = new M3D.Actions.RaypickAction(this.scene.getCurrentViewPort());
                    rayPickAction.setPickShapeType(M3D.Shapes.ShapeType.ONLY_MODEL);
                    //rayPickAction.setPickGeoType(M3D.Shapes.GeoFeatureEnum.FEATURE_TYPE_UNKNOWN);
                    rayPickAction.setRay(ray);
                    M3D.Actions.RaypickFunction.rayPick(rayPickAction, shape);
                    //m_pScene->DoAction(rayPickAction->GetSelfPtr());
                    // 获取射线与模型的所有相交点
                    let points = rayPickAction.getAllPickPoint(shape);
                    let pointSize = points.length;
                    if (pointSize < 2) {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    let minDistance = 0;
                    let maxDistance = 0;
                    let pointPos = 0;
                    let maxpointPos = 0;
                    for (let i = 0; i < pointSize; i++) {
                        let currentPoint = points[i];
                        if (currentPoint.Equals(this.firstPosition)) {
                            continue;
                        }
                        let dir = currentPoint.Sub(this.firstPosition);
                        if (!dir.Divide(dir.Length()).Equal(this.normalDirection)) {
                            continue;
                        }
                        let distance = this.firstPosition.Sub(currentPoint).Length();
                        if (minDistance === 0 || distance <= minDistance) {
                            minDistance = distance;
                            pointPos = i;
                        }
                        if (maxDistance <= distance) {
                            maxDistance = distance;
                            maxpointPos = i;
                        }
                    }
                    if (this.thicknessType === 1) {
                        pointPos = maxpointPos;
                        minDistance = maxDistance;
                    }
                    if (minDistance === M3D.MeasureNames.VALUE_0DOT0) {
                        return M3D.Actions.ActionState.Action_Error;
                    }
                    this.secondPosition = points[pointPos];
                    this.distanceLineEndPosition = this.secondPosition;
                    if (!this.firstPosition.Equals(new M3D.M3DMath.Vector3()) && !this.secondPosition.Equals(new M3D.M3DMath.Vector3()))
                        this.distances.push(this.firstPosition.Sub(this.secondPosition).Length());
                }
                return M3D.Actions.ActionState.Action_Success;
            }
            /**
             * @beta
             * @param {*} sonString
             * @return {*}
             * @memberof CreateThicknessDistanceAction
             */
            fromJson(sonString) {
                return true;
            }
            /**
             * @beta
             * @return {*}
             * @memberof CreateThicknessDistanceAction
             */
            toJson() {
                return "";
            }
        }
        Actions.CreateThicknessDistanceAction = CreateThicknessDistanceAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SVie.Actions
         * @description 创建视图Action
         * @export
         * @class CreateModelViewAction
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class CreateModelViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of CreateModelViewAction.
             * @memberof CreateModelViewAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof CreateModelViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置参数
             * @param {string} modelViewName 视图名称
             * @param {string} snapshotUrl 截图地址
             * @return {boolean}
             * @memberof CreateModelViewAction
             */
            setParameter(modelViewName, snapshotUrl) {
                this.currentModelViewSnapshotUrl = snapshotUrl;
                this.currentModelViewName = modelViewName;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof CreateModelViewAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.CREATEMODELVIEWACTION, methodName);
                let that = this;
                if (!this.scene) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let retState = M3D.Actions.ActionState.Action_Success;
                this.addModelView();
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.CREATEMODELVIEWACTION, methodName);
                return retState;
            }
            /**
             * @public
             * @description 添加视图
             * @memberof CreateModelViewAction
             */
            addModelView() {
                let that = this;
                let ret = "0";
                let retState = M3D.Actions.ActionState.Action_Success;
                let result = {};
                result["ret"] = ret;
                let fileId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.fileId);
                let uniqueId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.uniqueId);
                let creator = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.usernickname);
                let creatorId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.userid);
                let nowDate = new Date();
                let nowTime = new Intl.DateTimeFormat('en-CA', { "dateStyle": 'short' }).format(nowDate) + " " + nowDate.toTimeString().slice(0, 8);
                let currentModelView = new M3D.Shapes.ModelView();
                currentModelView.setName(this.currentModelViewName);
                currentModelView.setViewType(M3D.Shapes.ViewTypeEnum.UserView); //视图类型为在线视图
                currentModelView.setSvlType(M3D.Shapes.StkViewUsageEnum.VIEW_USAGE_SV_USER_VIEW);
                currentModelView.setProperty("createTime", nowTime);
                currentModelView.setProperty("creator", creator);
                currentModelView.setProperty("creatorId", creatorId);
                currentModelView.setProperty("uniqueId", uniqueId);
                currentModelView.setProperty("fileId", fileId);
                let topModel = this.scene.getTopModel();
                let modelId = 0;
                //需要获得顶级模型childen中模型的第一个
                if (topModel) {
                    let childenModelArray = topModel.getChildren();
                    if (childenModelArray.length > 0) {
                        let child = childenModelArray[0];
                        modelId = child.getInstanceId();
                    }
                    else {
                        modelId = topModel.getId();
                    }
                }
                currentModelView.setProperty("modelId", modelId + "");
                currentModelView.setProperty("image", this.currentModelViewSnapshotUrl);
                currentModelView.setUpDataCamera(true);
                currentModelView.setUpDataModel(true);
                //设置视图的上级视图id，暂时存为-1代表原始视图，后期可优化为真正的上级视图id
                currentModelView.setProperty("preview", "-1");
                this.modelView = currentModelView;
                //更新视图当前数据
                this.updateViewByCurrentScene();
                //判断是否是没有图片的原始视图
                let isResetImage = true;
                let serverViewUrl = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.serverViewUrl);
                if (serverViewUrl && fileId && fileId != "" && serverViewUrl != "") {
                    this.addServerModelView().then((response) => {
                        ret = response;
                        result["ret"] = ret;
                        if (ret.toString() !== M3D.Actions.ActionState.Action_Success.toString()) {
                            retState = M3D.Actions.ActionState.Action_Error;
                        }
                        else {
                            this.addModelViewList(this.modelView);
                            this.modelView.setProperty("currentView", "true");
                            result["view"] = this.modelView;
                        }
                        let message = new SView.AddModelViewActionMessage(SView.ActionMessageType.AddModelView, retState.toString(), ret.toString(), this.modelView);
                        that.send(message);
                    });
                }
                else { //直接保存一个本地视图，刷新后消息
                    this.addModelViewList(this.modelView);
                    this.modelView.setProperty("currentView", "true");
                    let message = new SView.AddModelViewActionMessage(SView.ActionMessageType.AddModelView, retState.toString(), ret.toString(), this.modelView);
                    that.send(message);
                }
            }
            /**
             * @public
             * @description 将视图添加至视图列表
             * @param {M3D.Shapes.ModelView} modelView
             * @memberof CreateModelViewAction
             */
            addModelViewList(modelView) {
                let that = this;
                let exist = false;
                let temp_id = 0;
                let modelViews = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SHAPE_MODELVIEW);
                if (modelViews.length) {
                    for (let i = 0; i < modelViews.length; i++) {
                        if (modelViews[i].getObjectId() === modelView.getObjectId()) {
                            Actions.ActionLogger.LOGI(that.addModelViewList.name, "", "", "Exist View Id is " + modelView.getObjectId());
                            exist = true;
                            temp_id = i;
                            break;
                        }
                    }
                }
                if (exist) {
                    Actions.ActionLogger.LOGI(that.addModelViewList.name, "", "", "Exist View Id is " + modelView.getObjectId());
                    modelViews[temp_id] = this.modelView;
                }
                else {
                    modelViews.push(modelView);
                    this.scene.getShapes().addShape(modelView);
                }
            }
            /**
             * @public
             * @description 添加在线视图
             * @param {boolean} [isResetImage=true]
             * @param {M3D.Shapes.ModelView} modelView 视图对象，若不传，则使用Action中的modelView
             * @return {Promise<string>} 结果为‘0’时添加成功
             * @memberof CreateModelViewAction
             */
            addServerModelView(isResetImage = true, modelView) {
                //保存数据库视图
                let ret = "0";
                if (modelView) {
                    this.modelView = modelView;
                }
                let image = this.modelView.getProperty("image");
                let blob = this.modelView.getImageFile();
                if (isResetImage) {
                    var array = [];
                    if (image) {
                        let binary = atob(image.split(',')[1]);
                        for (var i = 0; i < binary.length; i++) {
                            array.push(binary.charCodeAt(i));
                        }
                    }
                    blob = new Blob([new Uint8Array(array)], { type: 'image/png' });
                }
                if (blob === undefined) {
                    blob = new Blob();
                }
                let id = this.modelView.getProperty("fileId");
                let image1 = blob, image2 = blob;
                let type = "0";
                let content = M3D.Reader.ModelViewFactory.toJson(this.modelView);
                //formData.append("content", modelView.toJson(this));
                let uniqueid = this.modelView.getProperty("uniqueId");
                if (!uniqueid) {
                    uniqueid = "";
                }
                let creator = this.modelView.getProperty("creator") + "";
                let name = this.modelView.getName();
                let modelid = this.modelView.getProperty("modelId") + "";
                let modelviewid = this.modelView.getObjectId() + "";
                let isactivated = this.modelView.getIsInitView() ? "1" : "0";
                let blob2 = new Blob([M3D.Reader.ModelViewFactory.toServerJson(this.modelView, this.scene)]);
                let contentfile = blob2;
                //添加看板图片
                let modelBoardsIds = this.modelView.getModelBoardsIDList();
                let pModelBoardsGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODELBOARD);
                let boardName;
                let boardimage;
                if (pModelBoardsGroup && modelBoardsIds && modelBoardsIds.length > 0) {
                    for (let i = 0; i < modelBoardsIds.length; i++) {
                        let iModelBoardID = modelBoardsIds[i];
                        for (let j = 0; j < pModelBoardsGroup.length; j++) {
                            let pNode = (pModelBoardsGroup[j]);
                            if (!pNode)
                                continue;
                            if (pNode && iModelBoardID == pNode.getObjectId()) {
                                let imageList = pNode.getImageList();
                                if (imageList.length > 0) {
                                    let image = imageList[0]; //看板只有一张图片
                                    let fileName = image.getName();
                                    if (fileName) { //存在图片则上传，不存在则跳过
                                        let suffix = fileName.substring(fileName.lastIndexOf("."), fileName.length);
                                        let Arraybuffer = image.getImagePath();
                                        boardimage = new Blob([Arraybuffer], { type: "application/octet-binary" });
                                        boardName = pNode.getObjectId().toString() + suffix;
                                        //formData.append("boardfiles", boardimage, pNode.getId().toString() + suffix);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                return new Promise((resolve, rejects) => {
                    M3D.Utility.HttpMessageHandler.saveView(id, uniqueid, content, creator, name, modelid, modelviewid, isactivated, contentfile, image1, image2, boardimage, boardName).then((response) => {
                        let jsonObject = JSON.parse(response);
                        let ret = jsonObject["return"];
                        //回调 DOTO
                        if (ret == '0') {
                            let viewid = jsonObject["id"];
                            this.modelView.setProperty("serverId", viewid);
                            this.modelView.setProperty("currentView", "true");
                        }
                        resolve(ret);
                    });
                });
            }
            /**
             * @public
             * @description 根据当前场景更新视图
             * @return {boolean}
             * @memberof CreateModelViewAction
             */
            updateViewByCurrentScene() {
                let ret = false;
                if (this.modelView.getUpDataCameraState()) {
                    //使用当前相机状态填充视图相机数据
                    let pCurCamera = this.scene.getCurrentViewPort().getCamera();
                    let newViewCamera = new M3D.Camera();
                    let position = pCurCamera.getPosition();
                    newViewCamera.setPosition(position);
                    newViewCamera.setRotate(pCurCamera.getRotationQuaternion());
                    newViewCamera.setZoom(pCurCamera.getZoom());
                    let orthoSize = pCurCamera.getOrthoSize();
                    newViewCamera.setOrthoSize(orthoSize);
                    newViewCamera.setOrthographic(pCurCamera.isOrthographic());
                    newViewCamera.setAspectRatio(pCurCamera.getAspectRatio());
                    newViewCamera.setFov(pCurCamera.getFov());
                    newViewCamera.setFarClip(pCurCamera.getFarClip());
                    newViewCamera.setNearClip(pCurCamera.getNearClip());
                    //存在相机信息，则用字符串形式存储，后期使用时候解析
                    this.modelView.setCamera(newViewCamera);
                    let newViewCameraJson = M3D.Reader.ModelViewFactory.toJSONByCamera(newViewCamera);
                    let modelViewJsonObj = JSON.parse(newViewCameraJson);
                    this.modelView.setCameraStr(modelViewJsonObj);
                    this.modelView.setUpDataCamera(true);
                }
                if (this.modelView.getUpDataModelState()) {
                    let allSubModels = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODEL);
                    if (!allSubModels) {
                        //数据错误 return
                        return;
                    }
                    let insAttMap = new Map();
                    let plcPath;
                    for (let curModel of allSubModels) {
                        //ia临时变量用于记录当前模型状态与原始视图是否一致，如果不一致
                        let instanceflag = false;
                        let ia = new M3D.Shapes.InstanceAttribute();
                        ia.hasVisble = false;
                        ia.hasColor = false;
                        ia.hasMatrix = false;
                        ia.id = curModel.getInstanceId();
                        //ia.id = curModel.getObjectId();
                        ia.insColor = M3D.M3DMath.Color.GRAY();
                        ia.materialId = -1;
                        let protoShape = curModel.getProto();
                        if (curModel.isOriginalVisible() != curModel.isVisible()) {
                            instanceflag = true;
                            ia.hasVisble = true;
                            ia.visible = curModel.isVisible();
                        }
                        else {
                            //ia.hasVisble = curModel.o
                            ia.visible = curModel.isOriginalVisible();
                        }
                        if (curModel.getChildren().length <= 0) {
                            let pColor = curModel.getColor();
                            let oriColor = curModel.getOriginalColor();
                            let curModeMaterial = curModel.getMaterial();
                            let pTmpColor;
                            if (curModel.hasInstanceMaterial()) {
                                let curModeMaterial = curModel.getMaterial();
                                if (!curModeMaterial) {
                                    continue;
                                }
                                switch (curModeMaterial.getMaterialType()) {
                                    case M3D.Render.MaterialType.MaterialType_Phong:
                                        {
                                            pTmpColor = curModeMaterial.getDiffuse();
                                        }
                                        break;
                                    case M3D.Render.MaterialType.MaterialType_Pbr:
                                        {
                                            pTmpColor = curModeMaterial.getAlbedoColor();
                                        }
                                        break;
                                    case M3D.Render.MaterialType.MaterialType_MatCap:
                                        {
                                            pTmpColor = curModeMaterial.getDiffuse();
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                if (pTmpColor != oriColor) {
                                    instanceflag = true;
                                    ia.hasColor = true;
                                    ia.insColor = pTmpColor;
                                }
                            }
                        }
                        ia.path = curModel.getPlcPath();
                        let totalBoundingBox = curModel.getTotalBoundingBox();
                        //let precision = totalBoundingBox.Length() * 0.01;
                        let matrixTran = curModel.getPlaceMatrix();
                        let oriMatrixTran = curModel.getOriginalMatrix();
                        if (!matrixTran.Equals(oriMatrixTran) &&
                            !matrixTran.Translation().Equals(oriMatrixTran.Translation())) {
                            instanceflag = true;
                            ia.hasMatrix = true;
                            ia.placeMatrix = matrixTran;
                        }
                        insAttMap.set(ia.path, ia);
                    }
                    this.modelView.setInstanceAttributeMap(insAttMap);
                    let insAttMapJson = M3D.Reader.ModelViewFactory.toJSONByInsAttributes(insAttMap);
                    let insAttMapJsonObj = JSON.parse(insAttMapJson);
                    this.modelView.setInstanceAttributeStr(insAttMapJsonObj);
                }
                //DOTO 暂时写死 等观察模式写完加上
                this.modelView.setProperty("observeMode", "0");
                //this.modelView.setProperty("observeMode", this.commonTouchHandler.freeViewMode ? "0" : (this.commonTouchHandler.oribitMode ? "1" : "2"));
                let verticalAxis = M3D.Config.Parameters.getInstance().getNumberParameter(M3D.Config.verticalAxis);
                if (!verticalAxis) {
                    verticalAxis = 0;
                }
                this.modelView.setProperty("upDirection", "0 " + verticalAxis.toString() + " 0");
                //}
                this.getSectionPlaneIdsRelationModelView();
                this.getPMIIdsRelationModelView();
                this.getAnnotationIdsRelationModelView();
                this.getModelBoardIdsRelationModelView();
                return ret;
            }
            /**
             * @beta
             * @memberof CreateModelViewAction
             */
            getSectionPlaneIdsRelationModelView() {
                let pPlaneList = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SECTION_PLANE);
                let sectionList = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.SECTION);
                let showPlanList = [];
                this.modelView.clearSectionPlaneId();
                if (pPlaneList) {
                    let topModel = this.scene.getTopModel();
                    for (let i = 0; i < pPlaneList.length; i++) {
                        let pPlane = pPlaneList[i];
                        if (!pPlane) {
                            continue;
                        }
                        if (pPlane.getEnable()) {
                            let newPlane = new M3D.Shapes.SectionPlane();
                            let id = newPlane.getId();
                            let box = new M3D.M3DMath.BoundingBox();
                            newPlane.setDrawPlane(box);
                            newPlane.copy(pPlane);
                            newPlane.setId(id);
                            newPlane.setObjectId(id);
                            newPlane.updateDrawData(); //重新绘制数据
                            this.modelView.addSectionPlaneId(newPlane.getObjectId());
                            this.scene.getShapes().addShape(newPlane);
                            showPlanList.push(newPlane);
                            pPlane.setEnable(false);
                        }
                    }
                }
                if (sectionList && sectionList.length > 0) {
                    for (let section of sectionList) {
                        let sectionPlanes = section.getPlaneList();
                        for (let plane of sectionPlanes) {
                            if (plane.getEnable()) {
                                //新建SectionPlane 解决剖面切换显示问题，保存视图时新建section，防止剖面冲突
                                let newPlane = new M3D.Shapes.SectionPlane();
                                let id = newPlane.getId();
                                let box = new M3D.M3DMath.BoundingBox();
                                newPlane.setDrawPlane(box);
                                newPlane.copy(plane);
                                newPlane.setId(id);
                                newPlane.setObjectId(id);
                                newPlane.updateDrawData(); //重新绘制数据
                                this.modelView.addSectionPlaneId(newPlane.getObjectId());
                                this.scene.getShapes().addShape(newPlane);
                                showPlanList.push(newPlane);
                                plane.setEnable(false);
                            }
                        }
                    }
                }
                if (showPlanList.length > 0) {
                    //剖切
                    let sectionJson = '[ ';
                    for (let p of showPlanList) {
                        let sectionObj = M3D.Reader.SectionFactory.toJsonSectionPlane(p);
                        sectionJson += sectionObj + ',';
                    }
                    sectionJson = sectionJson.substring(0, sectionJson.length - 1);
                    sectionJson += ']';
                    this.modelView.setProperty("SectionPlanes", JSON.parse(sectionJson));
                }
            }
            /**
             * @beta
             * @description 获得视图关联的PMIID
             * @memberof CreateModelViewAction
             */
            getPMIIdsRelationModelView() {
                //设置与视图关联的PMI
                let vecPMIIds = [];
                let pmis = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.PMI);
                if (pmis) {
                    for (let pmi of pmis) {
                        if (!pmi) {
                            continue;
                        }
                        if (pmi.isVisible()) {
                            vecPMIIds.push(pmi.getPmiId());
                        }
                    }
                }
                this.modelView.setPMIIds(vecPMIIds);
            }
            /**
             * @beta
             * @description 获得视图关联的AnnotationID
             * @memberof CreateModelViewAction
             */
            getAnnotationIdsRelationModelView() {
                //文本批注
                let vecNoteIds = [];
                let pAnnotationGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.ANNOTATION_NOTE);
                if (pAnnotationGroup && pAnnotationGroup.length > 0) {
                    for (let node of pAnnotationGroup) {
                        if (node && node.isVisible()) {
                            vecNoteIds.push(node.getObjectId());
                        }
                    }
                }
                //测量批注
                let pMeasureGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MEASURE_BASE);
                if (pMeasureGroup && pMeasureGroup.length > 0) {
                    for (let measure of pMeasureGroup) {
                        let node = measure;
                        if (node && node.isVisible()) {
                            vecNoteIds.push(node.getObjectId());
                        }
                    }
                }
                //手势批注
                let noteGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.THREED_GESTURE_NOTE);
                if (noteGroup.length > 0) {
                    let noteCount = noteGroup.length;
                    for (let i = 0; i < noteCount; i++) {
                        let pNote = noteGroup[i];
                        if (pNote.isVisible()) {
                            //在此处填充View中使用的TextNote ID。
                            vecNoteIds.push(pNote.getId());
                        }
                    }
                }
                this.modelView.setNoteIds(vecNoteIds);
            }
            /**
             * @beta
             * @description 获得视图关联的看板ID
             * @memberof CreateModelViewAction
             */
            getModelBoardIdsRelationModelView() {
                //添加看板关联id列表
                let pModelBoardGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODELBOARD);
                if (pModelBoardGroup && pModelBoardGroup.length > 0) {
                    for (let j = 0; j < pModelBoardGroup.length; j++) {
                        let pShapeNode = (pModelBoardGroup[j]);
                        if (!pShapeNode)
                            continue;
                        if (pShapeNode && pShapeNode.isVisible()) {
                            this.modelView.addModelboardsId(pShapeNode.getObjectId());
                        }
                    }
                }
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof CreateModelViewAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof CreateModelViewAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.CreateModelViewAction = CreateModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 设置默认视图Action
         * @export
         * @class SetDefaultModelViewAction
         * @extends {M3D.Actions.Action}
         * @implements {M3D.Utility.Publisher}
         */
        class SetDefaultModelViewAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of SetDefaultModelViewAction.
             * @memberof SetDefaultModelViewAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof SetDefaultModelViewAction
             */
            setScene(scene) {
                this.scene = scene;
            }
            /**
             * @public
             * @description 设置Action执行所需参数
             * @param {string} currentModelViewId 视图ID
             * @param {string} currentModelViewServerId 视图对应的在线视图ID
             * @param {boolean} isDefault 是否是默认视图
             * @return {boolean}
             * @memberof SetDefaultModelViewAction
             */
            setParameter(currentModelViewId, currentModelViewServerId, isDefault) {
                if (!currentModelViewId) {
                    return false;
                }
                this.currentModelViewId = currentModelViewId;
                this.currentModelViewServerId = currentModelViewServerId;
                this.isDefault = isDefault;
                return true;
            }
            /**
             * @protected
             * @description 执行action
             * @return {M3D.Actions.ActionState}
             * @memberof SetDefaultModelViewAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                //ActionLogger.ACTIONLOGD(M3D.START, ActionNames.DISTANCEMEASUREACTION, methodName);
                let that = this;
                if (!that.scene) {
                    return M3D.Actions.ActionState.Action_Error;
                }
                let retState = M3D.Actions.ActionState.Action_Success;
                let fileId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.fileId);
                let uniqueId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.uniqueId);
                let creator = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.usernickname);
                let creatorId = M3D.Config.Parameters.getInstance().getStringParameter(M3D.Config.userid);
                if (!this.currentModelViewServerId || this.currentModelViewServerId === "undefined" || this.currentModelViewServerId === "") { //本地视图-->将本地视图存为在线视图
                    let view = this.getModelViewByModelViewId(Number(this.currentModelViewId), that.scene);
                    let nowDate = new Date();
                    let nowTime = nowDate.toLocaleDateString() + " " + nowDate.toTimeString().slice(0, 8); //System.DateTime.Now.ToString("yyyy-MM-dd HH-mm-ss");          
                    let image = view.getProperty("imageBlob");
                    view.setProperty("createTime", nowTime);
                    view.setProperty("creator", creator);
                    view.setProperty("creatorId", creatorId);
                    view.setProperty("uniqueId", uniqueId);
                    view.setProperty("fileId", fileId);
                    let topModel = that.scene.getTopModel();
                    let modelId = 0;
                    //需要获得顶级模型childen中模型的第一个
                    if (topModel) {
                        let childenModelArray = topModel.getChildren();
                        if (childenModelArray.length > 0) {
                            let child = childenModelArray[0];
                            modelId = child.getInstanceId();
                        }
                        else {
                            modelId = topModel.getId();
                        }
                    }
                    view.setProperty("modelId", modelId + "");
                    view.setProperty("preview", "-1");
                    if (image) {
                        view.setProperty("image", image);
                    }
                    else {
                        view.setProperty("image", null);
                    }
                    view.setIsInitView(this.isDefault);
                    this.addServerModelView(false, view).then((ret) => {
                        let result = JSON.parse(ret);
                        if (Number(result) !== M3D.Actions.ActionState.Action_Success) {
                            retState = M3D.Actions.ActionState.Action_Error;
                        }
                        else {
                            if (!that.isDefault) {
                                that.scene.getFileInfo().setCamera(null);
                            }
                        }
                        let message = new SView.ModelViewActionMessage(SView.ActionMessageType.SetDefaultModelView, retState.toString(), result.toString(), that.currentModelViewServerId, that.currentModelViewId, "", that.isDefault);
                        that.send(message);
                    });
                }
                else { //在线视图，走服务器
                    M3D.Utility.HttpMessageHandler.updateView(uniqueId, this.currentModelViewServerId, "", (this.isDefault ? 1 + "" : 0 + ""), "1").then((response) => {
                        let jsonObject = JSON.parse(response);
                        let result = jsonObject["return"];
                        if (Number(result) !== M3D.Actions.ActionState.Action_Success) {
                            retState = M3D.Actions.ActionState.Action_Error;
                        }
                        else {
                            if (!that.isDefault) {
                                that.scene.getFileInfo().setCamera(null);
                            }
                        }
                        let message = new SView.ModelViewActionMessage(SView.ActionMessageType.SetDefaultModelView, retState.toString(), result.toString(), that.currentModelViewServerId, that.currentModelViewId, "", that.isDefault);
                        that.send(message);
                    });
                    //M3D.Logger.M3DLOGD(M3D.Logger.MOUDLE_Default, M3D.Logger.THREAD_Default, "Set defaultView");
                }
                return retState;
            }
            /**
             * @private
             * @description 通过视图id获取视图对象
             * @param {number} modelViewId 视图id
             * @param {M3D.Scene} scene 场景对象
             * @return {M3D.Shapes.ModelView}
             * @memberof SetDefaultModelViewAction
             */
            getModelViewByModelViewId(modelViewId, scene) {
                let modelViews = scene.getShapes().getShapes(M3D.Shapes.ShapeType.SHAPE_MODELVIEW);
                if (modelViews.length > 0) {
                    for (let i = 0; i < modelViews.length; i++) {
                        if (modelViews[i].getId() === modelViewId) {
                            let modelView = modelViews[i];
                            return modelView;
                        }
                    }
                }
            }
            /**
             * @private
             * @description 添加在线视图
             * @param {boolean} [isResetImage=false] 是否重置图片
             * @param {M3D.Shapes.ModelView} 视图对象
             * @return {Promise<string>}
             * @memberof SetDefaultModelViewAction
             */
            addServerModelView(isResetImage = false, modelView) {
                let that = this;
                //保存数据库视图
                if (modelView) {
                    this.modelView = modelView;
                }
                let image = this.modelView.getProperty("image");
                let blob = this.modelView.getImageFile();
                if (isResetImage) {
                    var array = [];
                    if (image) {
                        let binary = atob(image.split(',')[1]);
                        for (var i = 0; i < binary.length; i++) {
                            array.push(binary.charCodeAt(i));
                        }
                    }
                    blob = new Blob([new Uint8Array(array)], { type: 'image/png' });
                }
                if (blob === undefined) {
                    blob = new Blob();
                }
                let id = this.modelView.getProperty("fileId");
                let image1 = blob, image2 = blob;
                let type = "0";
                let content = M3D.Reader.ModelViewFactory.toJson(this.modelView);
                //formData.append("content", modelView.toJson(this));
                let uniqueid = this.modelView.getProperty("uniqueId");
                if (!uniqueid) {
                    uniqueid = "";
                }
                let creator = this.modelView.getProperty("creator") + "";
                let name = this.modelView.getName();
                let modelid = this.modelView.getProperty("modelId") + "";
                let modelviewid = this.modelView.getObjectId() + "";
                let isactivated = this.modelView.getIsInitView() ? "1" : "0";
                let blob2 = new Blob([M3D.Reader.ModelViewFactory.toServerJson(this.modelView, this.scene)]);
                let contentfile = blob2;
                //添加看板图片
                let modelBoardsIds = this.modelView.getModelBoardsIDList();
                let pModelBoardsGroup = this.scene.getShapes().getShapes(M3D.Shapes.ShapeType.MODELBOARD);
                let boardName;
                let boardimage;
                if (pModelBoardsGroup && modelBoardsIds && modelBoardsIds.length > 0) {
                    for (let i = 0; i < modelBoardsIds.length; i++) {
                        let iModelBoardID = modelBoardsIds[i];
                        for (let j = 0; j < pModelBoardsGroup.length; j++) {
                            let pNode = (pModelBoardsGroup[j]);
                            if (!pNode)
                                continue;
                            if (pNode && iModelBoardID == pNode.getObjectId()) {
                                let imageList = pNode.getImageList();
                                if (imageList.length > 0) {
                                    let image = imageList[0]; //看板只有一张图片
                                    let fileName = image.getName();
                                    if (fileName) { //存在图片则上传，不存在则跳过
                                        let suffix = fileName.substring(fileName.lastIndexOf("."), fileName.length);
                                        let Arraybuffer = image.getImagePath();
                                        boardimage = new Blob([Arraybuffer], { type: "application/octet-binary" });
                                        boardName = pNode.getObjectId().toString() + suffix;
                                        //formData.append("boardfiles", boardimage, pNode.getId().toString() + suffix);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                return new Promise((resolve, rejects) => {
                    M3D.Utility.HttpMessageHandler.saveView(id, uniqueid, content, creator, name, modelid, modelviewid, isactivated, contentfile, image1, image2, boardimage, boardName).then((response) => {
                        let jsonObject = JSON.parse(response);
                        let ret = jsonObject["return"];
                        //回调 DOTO
                        if (ret == '0') {
                            let viewid = jsonObject["id"];
                            that.currentModelViewServerId = viewid;
                            //that.modelView.setProperty("serverId", viewid);
                            //that.modelView.setProperty("currentView", "true");
                        }
                        resolve(ret);
                    });
                });
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof SetDefaultModelViewAction
             */
            send(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().sendMessage(this, msg);
            }
            /**
             * @private
             * @param {M3D.Utility.M3DMessage} msg
             * @memberof SetDefaultModelViewAction
             */
            post(msg) {
                M3D.Utility.M3DMessageCenter.getCenter().postMessage(this, msg);
            }
        }
        Actions.SetDefaultModelViewAction = SetDefaultModelViewAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 剖切方向枚举
         * @export
         * @enum {number}
         */
        let SectionDirectionType;
        (function (SectionDirectionType) {
            SectionDirectionType[SectionDirectionType["SECTION_XY"] = 0] = "SECTION_XY";
            SectionDirectionType[SectionDirectionType["SECTION_YZ"] = 1] = "SECTION_YZ";
            SectionDirectionType[SectionDirectionType["SECTION_ZX"] = 2] = "SECTION_ZX";
        })(SectionDirectionType = Actions.SectionDirectionType || (Actions.SectionDirectionType = {}));
        ;
        /**
         * @memberof SView.Actions
         * @description 多面剖切Action
         * @export
         * @class MultiSectionAction
         * @extends {M3D.Actions.Action}
         */
        class MultiSectionAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of MultiSectionAction.
             * @memberof MultiSectionAction
             */
            constructor() {
                super();
                this.selectedShapeList = [];
                this.section_XY = new Actions.SectionAction(false);
                this.section_YZ = new Actions.SectionAction(true);
                this.section_ZX = new Actions.SectionAction(false);
                this.isVisible_XY = false;
                this.isVisible_YZ = true;
                this.isVisible_ZX = false;
                this.percent_XY = 0.5;
                this.percent_YZ = 0.5;
                this.percent_ZX = 0.5;
                this.isReverse_XY = false;
                this.isReverse_YZ = false;
                this.isReverse_ZX = false;
                this.isShowClipPlane = false;
                this.isShowCappingPlane = false;
                this.isAllReverse = false;
                this.sectionPlane_XY = null;
                this.sectionPlane_YZ = null;
                this.sectionPlane_ZX = null;
                this.selectedShapeList = [];
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} pScene
             * @memberof MultiSectionAction
             */
            setScene(pScene) {
                if (pScene != null &&
                    this.section_XY != null &&
                    this.section_YZ != null &&
                    this.section_ZX != null) {
                    this.scene = pScene;
                    this.section_XY.setScene(pScene);
                    this.section_YZ.setScene(pScene);
                    this.section_ZX.setScene(pScene);
                }
            }
            addSelectedShape(...args) {
                if (args.length === 1) {
                    let shape = args[0];
                    if (this.selectedShapeList.indexOf(shape) === -1) {
                        this.selectedShapeList.push(shape);
                    }
                }
                if (args.length === 2) {
                    let sectionAction = args[0];
                    let shape = args[1];
                    if (sectionAction) {
                        sectionAction.addSelectedShape(shape);
                    }
                }
            }
            /**
             * @public
             * @description 添加指定剖对象
             * @param {M3D.Shapes.Shape[]} shapes M3D.Shapes.Shape数组，目前仅支持模型对象
             * @memberof MultiSectionAction
             */
            addSelectedShapes(shapes) {
                for (let shape of shapes) {
                    this.addSelectedShape(shape);
                }
            }
            removeSelectedShape(...args) {
                if (args.length === 1) {
                    let shape = args[0];
                    if (this.selectedShapeList.indexOf(shape) != -1) {
                        this.selectedShapeList.splice(this.selectedShapeList.indexOf(shape), 1);
                    }
                }
                if (args.length === 2) {
                    let sectionAction = args[0];
                    let shape = args[1];
                    if (sectionAction) {
                        sectionAction.removeSelectedShape(shape);
                    }
                }
            }
            /**
             * @public
             * @description 删除指定剖对象
             * @param {M3D.Shapes.Shape[]} shapes M3D.Shapes.Shape数组，目前仅支持模型对象
             * @memberof MultiSectionAction
             */
            removeSelectedShapes(shapes) {
                for (let shape of shapes) {
                    this.removeSelectedShape(shape);
                }
            }
            /**
             * @public
             * @description 获取指定剖模型
             * @return {M3D.Shapes.Shape[]}
             * @memberof MultiSectionAction
             */
            getSelectedShapes() {
                return this.selectedShapeList;
            }
            setSectionVisible(...args) {
                if (args.length === 1) {
                    let visible = args[0];
                    this.isVisible_XY = visible;
                    this.isVisible_YZ = visible;
                    this.isVisible_ZX = visible;
                }
                else if (args.length === 2) {
                    let type = args[0];
                    let visible = args[1];
                    switch (type) {
                        case SectionDirectionType.SECTION_XY:
                            this.isVisible_XY = visible;
                            break;
                        case SectionDirectionType.SECTION_YZ:
                            this.isVisible_YZ = visible;
                            break;
                        case SectionDirectionType.SECTION_ZX:
                            this.isVisible_ZX = visible;
                            break;
                        default:
                            break;
                    }
                }
            }
            /**
             * @public
             * @description 设置反向剖切
             * @param {SectionDirectionType} type 剖切面类型
             * @param {boolean} reverse 是否反向
             * @memberof MultiSectionAction
             */
            setSectionReverse(type, reverse) {
                switch (type) {
                    case SectionDirectionType.SECTION_XY:
                        this.isReverse_XY = reverse;
                        break;
                    case SectionDirectionType.SECTION_YZ:
                        this.isReverse_YZ = reverse;
                        break;
                    case SectionDirectionType.SECTION_ZX:
                        this.isReverse_ZX = reverse;
                        break;
                    default:
                        break;
                }
            }
            /**
             * @public
             * @description 设置剖切进度
             * @param {SectionDirectionType} type 剖切面类型
             * @param {number} percent 进度，0-1
             * @return {*}
             * @memberof MultiSectionAction
             */
            setSectionPercent(type, percent) {
                //比例的正确性验证
                if (percent > 1 || percent < 0) {
                    return;
                }
                switch (type) {
                    case SectionDirectionType.SECTION_XY:
                        this.percent_XY = percent;
                        break;
                    case SectionDirectionType.SECTION_YZ:
                        this.percent_YZ = percent;
                        break;
                    case SectionDirectionType.SECTION_ZX:
                        this.percent_ZX = percent;
                        break;
                    default:
                        break;
                }
            }
            /**
             * @public
             * @description 设置是否显示盖面
             * @param {boolean} isShowCappingPlanes
             * @memberof MultiSectionAction
             */
            setShowCappingPlane(isShowCappingPlanes) {
                this.isShowCappingPlane = isShowCappingPlanes;
            }
            /**
             * @public
             * @description 设置是否显示截面
             * @param {boolean} isShowClipPlanes
             * @memberof MultiSectionAction
             */
            setShowClipPlane(isShowClipPlanes) {
                this.isShowClipPlane = isShowClipPlanes;
            }
            /**
             * @public
             * @description 设置是否反向
             * @param {boolean} isReverseClips
             * @memberof MultiSectionAction
             */
            setReverseClip(isReverseClips) {
                this.isAllReverse = isReverseClips;
            }
            /**
             * @public
             * @description 清空指定剖模型
             * @memberof MultiSectionAction
             */
            clearSelectedShapes() {
                if (this.section_XY) {
                    this.section_XY.clearSelectedShapes();
                }
                if (this.section_YZ) {
                    this.section_YZ.clearSelectedShapes();
                }
                if (this.section_ZX) {
                    this.section_ZX.clearSelectedShapes();
                }
                this.selectedShapeList = [];
            }
            /**
             * @description 执行剖切Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof MultiSectionAction
             */
            onExecute() {
                //TODO 先不使用全部反向，各个剖面使用自己对应的反向值控制
                if (this.isVisible_XY) {
                    let box = this.scene.getBoundingBox(true);
                    if (Math.abs(this.percent_XY - 1.0) < 0.01)
                        this.percent_XY = 1.01;
                    if (Math.abs(this.percent_XY) < 0.01)
                        this.percent_XY = -0.01;
                    let zlen = box.GetZLength();
                    let position = box.min.z + zlen * this.percent_XY;
                    let z = 1;
                    if (this.isReverse_XY) {
                        z = -1;
                    }
                    else {
                        position = -position;
                    }
                    if (this.sectionPlane_XY == null) {
                        let directonXY = new M3D.M3DMath.Vector3(0, 0, z);
                        this.section_XY.setDirection(directonXY);
                        let positionXY = new M3D.M3DMath.Vector3(0, 0, box.Center().z);
                        this.section_XY.setPosition(positionXY);
                        this.section_XY.execute();
                        let id = this.section_XY.getSection().getId();
                        let ssection = this.scene.getShapes()
                            .getShape(id, M3D.Shapes.ShapeType.SECTION);
                        this.sectionPlane_XY = ssection.getPlaneList()[0];
                    }
                    if (this.sectionPlane_XY != null) {
                        this.section_XY.clearSelectedShapes();
                        for (let shape of this.selectedShapeList) {
                            this.addSelectedShape(this.section_XY, shape);
                        }
                        this.sectionPlane_XY.setEnable(true);
                        this.sectionPlane_XY.setProgress(this.percent_XY);
                        this.sectionPlane_XY.setPlaneParam(0, 0, z, position);
                        this.sectionPlane_XY.setShowClipPlane(this.isShowClipPlane);
                        this.sectionPlane_XY.setReverseClip(false);
                        this.sectionPlane_XY.setShowCappingPlane(this.isShowCappingPlane);
                    }
                }
                else {
                    if (this.sectionPlane_XY != null) {
                        this.sectionPlane_XY.setEnable(false);
                    }
                }
                if (this.isVisible_YZ) {
                    let box = this.scene.getBoundingBox(true);
                    if (Math.abs(this.percent_YZ - 1.0) < 0.01)
                        this.percent_YZ = 1.01;
                    if (Math.abs(this.percent_YZ) < 0.01)
                        this.percent_YZ = -0.01;
                    let xlen = box.GetXLength();
                    let position = box.min.x + xlen * this.percent_YZ;
                    let x = 1;
                    if (this.isReverse_YZ) {
                        x = -1;
                    }
                    else {
                        position = -position;
                    }
                    if (this.sectionPlane_YZ == null) {
                        let directonYZ = new M3D.M3DMath.Vector3(x, 0, 0);
                        this.section_YZ.setDirection(directonYZ);
                        let positionYZ = new M3D.M3DMath.Vector3(box.Center().x, 0, 0);
                        this.section_YZ.setPosition(positionYZ);
                        this.section_YZ.execute();
                        let id = this.section_YZ.getSection().getId();
                        let ssection = this.scene.getShapes()
                            .getShape(id, M3D.Shapes.ShapeType.SECTION);
                        this.sectionPlane_YZ = ssection.getPlaneList()[0];
                    }
                    if (this.sectionPlane_YZ != null) {
                        this.section_YZ.clearSelectedShapes();
                        for (let shape of this.selectedShapeList) {
                            this.addSelectedShape(this.section_YZ, shape);
                        }
                        this.sectionPlane_YZ.setEnable(true);
                        this.sectionPlane_YZ.setProgress(this.percent_YZ);
                        this.sectionPlane_YZ.setPlaneParam(x, 0, 0, position);
                        this.sectionPlane_YZ.setShowClipPlane(this.isShowClipPlane);
                        this.sectionPlane_YZ.setReverseClip(false);
                        this.sectionPlane_YZ.setShowCappingPlane(this.isShowCappingPlane);
                    }
                }
                else {
                    if (this.sectionPlane_YZ != null) {
                        this.sectionPlane_YZ.setEnable(false);
                    }
                }
                if (this.isVisible_ZX) {
                    let box = this.scene.getBoundingBox(true);
                    if (Math.abs(this.percent_ZX - 1.0) < 0.01)
                        this.percent_ZX = 1.01;
                    if (Math.abs(this.percent_ZX) < 0.01)
                        this.percent_ZX = -0.01;
                    let ylen = box.GetYLength();
                    let position = box.min.y + ylen * this.percent_ZX;
                    let y = 1;
                    if (this.isReverse_ZX) {
                        y = -1;
                    }
                    else {
                        position = -position;
                    }
                    if (this.sectionPlane_ZX == null) {
                        let directonZX = new M3D.M3DMath.Vector3(0, y, 0);
                        this.section_ZX.setDirection(directonZX);
                        let positionZX = new M3D.M3DMath.Vector3(0, box.Center().y, 0);
                        this.section_ZX.setPosition(positionZX);
                        this.section_ZX.execute();
                        let id = this.section_ZX.getSection().getId();
                        let ssection = this.scene.getShapes()
                            .getShape(id, M3D.Shapes.ShapeType.SECTION);
                        this.sectionPlane_ZX = ssection.getPlaneList()[0];
                    }
                    if (this.sectionPlane_ZX != null) {
                        this.section_ZX.clearSelectedShapes();
                        for (let shape of this.selectedShapeList) {
                            this.addSelectedShape(this.section_ZX, shape);
                        }
                        this.sectionPlane_ZX.setEnable(true);
                        this.sectionPlane_ZX.setProgress(this.percent_ZX);
                        this.sectionPlane_ZX.setPlaneParam(0, y, 0, position);
                        this.sectionPlane_ZX.setShowClipPlane(this.isShowClipPlane);
                        this.sectionPlane_ZX.setReverseClip(false);
                        this.sectionPlane_ZX.setShowCappingPlane(this.isShowCappingPlane);
                    }
                }
                else {
                    if (this.sectionPlane_ZX != null) {
                        this.sectionPlane_ZX.setEnable(false);
                    }
                }
                this.scene.getCurrentViewPort().markDirty();
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.MultiSectionAction = MultiSectionAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 剖切Action
         * @export
         * @class SectionAction
         * @extends {M3D.Actions.Action}
         */
        class SectionAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of SectionAction.
             * @param {boolean} isHide
             * @memberof SectionAction
             */
            constructor(isHide) {
                super();
                this.pCurSectionPlane = null;
                this.isHideOtherSection = isHide;
            }
            /**
             * @public
             * @description 设置场景对象
             * @param {M3D.Scene} scene
             * @memberof SectionAction
             */
            setScene(scene) {
                this.pScene = scene;
            }
            /**
             * @public
             * @description 设置剖切方向
             * @param {M3D.M3DMath.Vector3} direction 方向
             * @memberof SectionAction
             */
            setDirection(direction) {
                this.direction = direction;
            }
            /**
             * @public
             * @description 设置剖切位置
             * @param {M3D.M3DMath.Vector3} position
             * @memberof SectionAction
             */
            setPosition(position) {
                this.position = position;
            }
            /**
             * @public
             * @description 设置剖切对象
             * @param {M3D.Shapes.Section} section
             * @memberof SectionAction
             */
            setSection(section) {
                this.sectionM = section;
            }
            /**
             * @public
             * @description 获取剖切对象
             * @return {M3D.Shapes.Section}
             * @memberof SectionAction
             */
            getSection() {
                if (this.sectionM) {
                    return this.sectionM;
                }
                return null;
            }
            /**
             * @public
             * @description 添加指定剖切的模型对象
             * @param {M3D.Shapes.Shape} shape
             * @memberof SectionAction
             */
            addSelectedShape(shape) {
                if (this.sectionM) {
                    this.sectionM.addSelectedShape(shape);
                }
            }
            /**
             * @public
             * @description 删除指定剖模型对象
             * @param {M3D.Shapes.Shape} shape
             * @return {boolean}
             * @memberof SectionAction
             */
            removeSelectedShape(shape) {
                let isRemoveSuccess = false;
                if (this.sectionM) {
                    isRemoveSuccess = this.sectionM.removeSelectedShape(shape);
                }
                return isRemoveSuccess;
            }
            /**
             * @public
             * @description 获取指定剖切对象
             * @return {M3D.Shapes.Shape[]}
             * @memberof SectionAction
             */
            getSelectedShapes() {
                let ret;
                if (this.sectionM) {
                    ret = this.sectionM.getSelectedShapes();
                }
                return ret;
            }
            /**
             * @public
             * @description 清空指定剖模型对象
             * @memberof SectionAction
             */
            clearSelectedShapes() {
                if (this.sectionM) {
                    this.sectionM.clearSelectedShapes();
                }
            }
            /**
             * @public
             * @description 设置是否显隐
             * @param {boolean} visible
             * @memberof SectionAction
             */
            setVisible(visible) {
                if (this.sectionM) {
                    this.sectionM.setVisible(visible);
                }
            }
            /**
             * @protected
             * @description 执行剖切Action
             * @return {M3D.Actions.ActionState}
             * @memberof SectionAction
             */
            onExecute() {
                this.sectionM = new M3D.Shapes.Section();
                let secPlane = new M3D.Shapes.SectionPlane(this.position, this.direction);
                this.sectionM.addNewPlane(secPlane);
                if (this.pScene) {
                    this.pScene.getShapes().addShape(this.sectionM, true);
                    secPlane.setScene(this.pScene);
                }
                return M3D.Actions.ActionState.Action_Success;
            }
        }
        Actions.SectionAction = SectionAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 选择集Action基类
         * @export
         * @class ShapeSetAction
         * @extends {M3D.Actions.Action}
         */
        class ShapeSetAction extends M3D.Actions.Action {
            /**
             * @constructor
             * Creates an instance of ShapeSetAction.
             * @memberof ShapeSetAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置S3DView对象
             * @param {S3DView} viewer
             * @memberof ShapeSetAction
             */
            setViewer(viewer) {
                this.viewer = viewer;
            }
            /**
             * @public
             * @description 获取S3DView对象
             * @return {S3DView}
             * @memberof ShapeSetAction
             */
            getViewer() {
                return this.viewer;
            }
            /**
             * @public
             * @description 获取选择集对象
             * @return {ShapeSet}
             * @memberof ShapeSetAction
             */
            getShapeSet() {
                return this.shapeSet;
            }
            /**
             * @beta
             * @description 设置参数
             * @param {...any[]} para
             * @return {boolean}
             * @memberof ShapeSetAction
             */
            setParameter(...para) {
                return true;
            }
        }
        Actions.ShapeSetAction = ShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
/// <reference path = "ShapeSetAction.ts"/>
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 添加至选择集Action
         * @export
         * @class AddToShapeSetAction
         * @extends {ShapeSetAction}
         */
        class AddToShapeSetAction extends Actions.ShapeSetAction {
            /**
             * @constructor
             * Creates an instance of AddToShapeSetAction.
             * @memberof AddToShapeSetAction
             */
            constructor() {
                super();
                this.shapes = [];
            }
            /**
             * @public
             * @description 设置参数
             * @param {ShapeSet} shapeSet 选择集对象
             * @param {M3D.Shapes.Shape[]} shapes Shape数组，目前仅支持模型对象
             * @return {boolean}
             * @memberof AddToShapeSetAction
             */
            setParameter(shapeSet, shapes) {
                if (!shapeSet) {
                    return false;
                }
                this.shapeSet = shapeSet;
                this.shapes = shapes;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof AddToShapeSetAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (this.shapeSet) {
                    this.shapeSet.addShapes(this.shapes);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @beta
             * @memberof AddToShapeSetAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof AddToShapeSetAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.AddToShapeSetAction = AddToShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 创建选择集Action
         * @export
         * @class CreateShapeSetAction
         * @extends {ShapeSetAction}
         */
        class CreateShapeSetAction extends Actions.ShapeSetAction {
            /**
             * @constructor
             * Creates an instance of CreateShapeSetAction.
             * @memberof CreateShapeSetAction
             */
            constructor() {
                super();
                this.shapes = [];
            }
            /**
             * @public
             * @description 设置参数
             * @param {M3D.Shapes.Shape[]} shapes Shape数组
             * @param {string} name 选择集名称
             * @return {boolean}
             * @memberof CreateShapeSetAction
             */
            setParameter(shapes, name) {
                if (shapes) {
                    this.shapes = shapes;
                }
                if (name) {
                    this.name = name;
                }
                return true;
            }
            /**
             * @protected
             * @description 执行Action
             * @return {M3D.Actions.ActionState}
             * @memberof CreateShapeSetAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                let shapeSetManager = this.getViewer().getShapeSetManager();
                let shapeSet = shapeSetManager.createShapeSet(this.shapes, this.name);
                if (shapeSet) {
                    //if (this.name) {
                    //    shapeSet.setName(this.name);
                    //}
                    this.shapeSet = shapeSet;
                }
                else {
                    retState = M3D.Actions.ActionState.Action_Error;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @public
             * @description 获取选择集
             * @return {ShapeSet}
             * @memberof CreateShapeSetAction
             */
            getShapeSet() {
                return this.shapeSet;
            }
            /**
             * @beta
             * @memberof CreateShapeSetAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof CreateShapeSetAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.CreateShapeSetAction = CreateShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 删除选择集Action
         * @export
         * @class DeleteShapeSetAction
         * @extends {ShapeSetAction}
         */
        class DeleteShapeSetAction extends Actions.ShapeSetAction {
            /**
             * @constructor
             * Creates an instance of DeleteShapeSetAction.
             * @memberof DeleteShapeSetAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置参数
             * @param {(ShapeSet | string)} shapeSet 选择集名称或选择集对象
             * @return {boolean}
             * @memberof DeleteShapeSetAction
             */
            setParameter(shapeSet) {
                if (shapeSet instanceof SView.ShapeSet) {
                    this.shapeSet = shapeSet;
                }
                else if (typeof shapeSet === "string") {
                    this.name = shapeSet;
                }
                else {
                    return false;
                }
                return true;
            }
            /**
             * @description z执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof DeleteShapeSetAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                let shapeSetManager = this.getViewer().getShapeSetManager();
                if (this.shapeSet) {
                    shapeSetManager.removeShapeSet(this.shapeSet);
                }
                if (this.name) {
                    let shapeSet = shapeSetManager.getShapeSet(this.name);
                    if (shapeSet) {
                        shapeSetManager.removeShapeSet(shapeSet);
                    }
                    else {
                        retState = M3D.Actions.ActionState.Action_Error;
                    }
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @beta
             * @memberof DeleteShapeSetAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof DeleteShapeSetAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.DeleteShapeSetAction = DeleteShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 从选择集中删除模型
         * @export
         * @class RemoveFromShapeSetAction
         * @extends {ShapeSetAction}
         */
        class RemoveFromShapeSetAction extends Actions.ShapeSetAction {
            /**
             * @constructor
             * Creates an instance of RemoveFromShapeSetAction.
             * @memberof RemoveFromShapeSetAction
             */
            constructor() {
                super();
                this.shapes = [];
            }
            /**
             * @public
             * @description 设置参数
             * @param {ShapeSet} shapeSet 选择集对象
             * @param {M3D.Shapes.Shape[]} shapes 模型数组
             * @return {boolean}
             * @memberof RemoveFromShapeSetAction
             */
            setParameter(shapeSet, shapes) {
                if (!shapeSet || !shapes || shapes.length <= 0) {
                    return false;
                }
                this.shapeSet = shapeSet;
                this.shapes = shapes;
                return true;
            }
            /**
             * @description 执行Action
             * @protected
             * @return {M3D.Actions.ActionState}
             * @memberof RemoveFromShapeSetAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (this.shapeSet) {
                    this.shapeSet.removeShapes(this.shapes);
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @beta
             * @memberof RemoveFromShapeSetAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             * @param {string} jsonStr
             * @memberof RemoveFromShapeSetAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.RemoveFromShapeSetAction = RemoveFromShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Actions;
    (function (Actions) {
        /**
         * @memberof SView.Actions
         * @description 重命名选择集
         * @export
         * @class RenameShapeSetAction
         * @extends {ShapeSetAction}
         */
        class RenameShapeSetAction extends Actions.ShapeSetAction {
            /**
             * @constructor
             * Creates an instance of RenameShapeSetAction.
             * @memberof RenameShapeSetAction
             */
            constructor() {
                super();
            }
            /**
             * @public
             * @description 设置参数
             * @param {ShapeSet} shapeSet 选择集对象
             * @param {string} name 名称
             * @return {boolean}
             * @memberof RenameShapeSetAction
             */
            setParameter(shapeSet, name) {
                if (shapeSet instanceof SView.ShapeSet) {
                    this.shapeSet = shapeSet;
                    if (name) {
                        this.name = name;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
                return true;
            }
            /**
             * @protected
             * @description 执行Action
             * @return {M3D.Actions.ActionState}
             * @memberof RenameShapeSetAction
             */
            onExecute() {
                let methodName = this.onExecute.name;
                Actions.ActionLogger.ACTIONLOGD(M3D.START, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                let retState = M3D.Actions.ActionState.Action_Success;
                if (this.shapeSet && this.name) {
                    this.shapeSet.setName(this.name);
                }
                else {
                    retState = M3D.Actions.ActionState.Action_Error;
                }
                Actions.ActionLogger.ACTIONLOGD(M3D.END, Actions.ActionNames.DISTANCEMEASUREACTION, methodName);
                return retState;
            }
            /**
             * @beta
             *
             * @memberof RenameShapeSetAction
             */
            toJson() {
                //TODO
            }
            /**
             * @beta
             *
             * @param {string} jsonStr
             * @memberof RenameShapeSetAction
             */
            fromJson(jsonStr) {
                //TODO
            }
        }
        Actions.RenameShapeSetAction = RenameShapeSetAction;
    })(Actions = SView.Actions || (SView.Actions = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    /**
     * @memberof SView
     * @description Action消息类型
     * @export
     * @enum {number}
     */
    let ActionMessageType;
    (function (ActionMessageType) {
        ActionMessageType["AddFile"] = "addFile";
        ActionMessageType["RemoveModelView"] = "removeModelView";
        ActionMessageType["RenameModelView"] = "renameModelView";
        ActionMessageType["SetDefaultModelView"] = "setDefaultModelView";
        ActionMessageType["AddModelView"] = "addModelView";
        ActionMessageType["ShowModelView"] = "showModelView";
    })(ActionMessageType = SView.ActionMessageType || (SView.ActionMessageType = {}));
    /**
     * @memberof SView
     * @description Action消息状态
     * @export
     * @enum {number}
     */
    let ActionMessageState;
    (function (ActionMessageState) {
        ActionMessageState["DeleteSuccess"] = "0";
        ActionMessageState["OperationFailed"] = "-1";
        ActionMessageState["NoParameterError"] = "1";
        ActionMessageState["ParameterError"] = "2";
        ActionMessageState["WithoutPermissionRemove"] = "1007";
    })(ActionMessageState = SView.ActionMessageState || (SView.ActionMessageState = {}));
    /**
     * @memberof SView
     * @description Action消息对象
     * @export
     * @class ActionMessage
     * @extends {M3D.Utility.M3DMessage}
     */
    class ActionMessage extends M3D.Utility.M3DMessage {
        /**
         * @constructor
         * Creates an instance of ActionMessage.
         * @param {string} commandName 命令名称
         * @param {string} state 状态
         * @memberof ActionMessage
         */
        constructor(commandName, state) {
            super(commandName, state);
        }
    }
    SView.ActionMessage = ActionMessage;
    /**
     * @memberof SView
     * @description 添加文件Action消息
     * @export
     * @class AddFileActionMessage
     * @extends {M3D.Utility.M3DMessage}
     */
    class AddFileActionMessage extends M3D.Utility.M3DMessage {
        /**
         * @constructor
         * Creates an instance of AddFileActionMessage.
         * @param {string} commandName 命令名称
         * @param {string} state 状态
         * @param {M3D.Shapes.Model} parentModel 父级模型
         * @param {M3D.Shapes.Model} model 添加的模型
         * @memberof AddFileActionMessage
         */
        constructor(commandName, state, parentModel, model) {
            super(commandName, state);
            this.parentModel = parentModel;
            this.model = model;
        }
    }
    SView.AddFileActionMessage = AddFileActionMessage;
    /**
     * @memberof SView
     * @description 视图Action相关消息
     * @export
     * @class ModelViewActionMessage
     * @extends {M3D.Utility.M3DMessage}
     */
    class ModelViewActionMessage extends M3D.Utility.M3DMessage {
        /**
         * @constructor
         * Creates an instance of ModelViewActionMessage.
         * @param {string} commandName 命令名称
         * @param {string} state 状态
         * @param {string} resultState 服务器端的数据状态
         * @param {string} modelViewServerId 在线视图ID
         * @param {string} modelViewId 视图ID
         * @param {string} modelViewName 视图名称
         * @param {boolean} [isDefault] 是否是默认视图
         * @memberof ModelViewActionMessage
         */
        constructor(commandName, state, resultState, modelViewServerId, modelViewId, modelViewName, isDefault) {
            super(commandName, state);
            this.modelViewServerId = modelViewServerId;
            this.modelViewId = modelViewId;
            this.resultState = resultState;
            this.modelViewName = modelViewName;
            this.isDefault = isDefault;
        }
    }
    SView.ModelViewActionMessage = ModelViewActionMessage;
    /**
     * @memberof SView
     * @description 添加视图Action
     * @export
     * @class AddModelViewActionMessage
     * @extends {M3D.Utility.M3DMessage}
     */
    class AddModelViewActionMessage extends M3D.Utility.M3DMessage {
        /**
         * @constructor
         * Creates an instance of AddModelViewActionMessage.
         * @param {string} commandName 命令名称
         * @param {string} state 状态
         * @param {string} resultState 服务器端的数据状态
         * @param {M3D.Shapes.ModelView} modelView 添加成功的视图
         * @memberof AddModelViewActionMessage
         */
        constructor(commandName, state, resultState, modelView) {
            super(commandName, state);
            this.modelView = modelView;
            this.resultState = resultState;
        }
    }
    SView.AddModelViewActionMessage = AddModelViewActionMessage;
    /**
     * @memberof SView
     * @description 切换视图Action
     * @export
     * @class AddModelViewActionMessage
     * @extends {M3D.Utility.M3DMessage}
     */
    class ShowModelViewActionMessage extends M3D.Utility.M3DMessage {
        /**
         * @constructor
         * Creates an instance of AddModelViewActionMessage.
         * @param {string} commandName 命令名称
         * @param {string} state 状态
         * @param {string} resultState 服务器端的数据状态
         * @param {M3D.Shapes.ModelView} modelView 添加成功的视图
         * @memberof AddModelViewActionMessage
         */
        constructor(commandName, state, modelView) {
            super(commandName, state);
            this.modelView = modelView;
        }
    }
    SView.ShowModelViewActionMessage = ShowModelViewActionMessage;
})(SView || (SView = {}));
//# sourceMappingURL=SView.Action.js.map