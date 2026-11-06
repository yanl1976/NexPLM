//初始化工具栏控件
var screenWidth = document.documentElement.clientWidth;
var screenHeight = document.documentElement.clientHeight;
//设置页面相关事件
function checkBoxChange(key) {
    let checkBox = sview0.sviewFrame.getUIManager().getElement(key);
    let value = checkBox.getValue();
    console.log(value);
}
function selectChange(key) {
    let select = sview0.sviewFrame.getUIManager().getElement(key);
    let value = select.getValue();
    console.log(value);
}





window.onresize = function (e) {
    orient(e);
}

function orient() {
    //var canvas = document.getElementById('gesture_canvas');
    //canvas.style.width = window.sviewFrameDiv.offsetWidth + "px";
    //canvas.style.height = window.sviewFrameDiv.offsetHight + "px";
    //var offset_x = window.offsetWidth - document.getElementById("sview_anima_step_panel_lit").offsetWidth;
    //document.getElementById("sview_anima_step_panel_lit").style.left = offset_x + "px";
    //document.getElementById("sview_anima_step_panel_lit").style.top = "0px";
    paint.reset();
    if (paint.list_map) {
        for (var i = 0; i < paint.list_map.length; i++) {
            var map = paint.list_map[i];
            var x = map.x;
            var y = map.y;
            for (var j = 0; j < x.length; j++) {
                paint.cxt.beginPath();
                if (map['clickDrag'][j] && j) {
                    paint.cxt.moveTo(x[j - 1], y[j - 1]);
                } else {
                    paint.cxt.moveTo(x[j] - 1, y[j]);
                }
                paint.cxt.lineTo(x[j], y[j]);
                paint.cxt.closePath();
                paint.cxt.stroke();
            }
        }
    }
}
//切换按钮
function selectImg(id, parentId) {
    var dirs = document.getElementById(id).parentElement.parentElement.children;
    for (var i = 0; i < dirs.length; i++) {
        let div = dirs[i].children[0];
        div.style.backgroundColor = "";
        // dirs[i].children[0].style.backgroundColor = "";
    }
    SView.Windows.Button.highLightLabel(id);
    var menu = document.getElementById(parentId);
    var btnImg = menu.getElementsByTagName("img")[0];
    var currentImg = document.getElementById(id).getElementsByTagName("img")[0];
    btnImg.src = currentImg.src;
    btnImg.title = currentImg.title;
    menu.getElementsByClassName("SView-show")[0].classList.add("SView-hide");
    menu.getElementsByClassName("SView-show")[0].classList.remove("SView-show");
    if (parentId == "gesture_global") {
        let globalType = 0;
        switch (id) {
            case "gesture3d_freedom"://直线
                globalType = 0;
                break
            case "gesture3d_circle"://圆形
                globalType = 1;
                break;
            case "gesture3d_rect"://矩形
                globalType = 2;
                break;
            case "gesture3d_triangle"://三角形
                globalType = 3;
                break;
        }
        if (paint.list_map.length == 0) {
            if (linetType != globalType) {
                paint.cxt.clearRect(0, 0, screenWidth, screenHeight);
                paint.list_map.length = 0;
            }
            linetType = globalType;
            return;
        }
        var okFun = function () {
            //sview.Create3DGestureNote(paint.list_map, linetType + 1);
            if (linetType != globalType) {
                paint.cxt.clearRect(0, 0, screenWidth, screenHeight);
                paint.list_map.length = 0;
            }
            linetType = globalType;
        };
        var cancleFun = function () {
            if (linetType != globalType) {
                paint.cxt.clearRect(0, 0, screenWidth, screenHeight);
                paint.list_map.length = 0;
            }
            linetType = globalType;
        };
        confirmExit("", SView[SView.UIManager.languageInfo].languageObj.Prompt.SaveCurrentAnnotation, okFun, cancleFun);
    } else if (parentId == "gesture_color_info") {

    }
}
function exitGestureMenu(currentMenu, returnMenu) {
    if (paint.list_map.length == 0) {
        exitBottomMenu(currentMenu, returnMenu);
    } else {
        var okFun = function () {
            //sview.Create3DGestureNote(paint.list_map, linetType + 1);
            exitBottomMenu(currentMenu, returnMenu);
        };
        var cancleFun = function () {
            paint.cxt.clearRect(0, 0, screenWidth, screenHeight);
            paint.list_map.length = 0;
            exitBottomMenu(currentMenu, returnMenu);
        };
        confirmExit("", SView[SView.UIManager.languageInfo].languageObj.Prompt.SaveCurrentAnnotation, okFun, cancleFun);
    }
}
var linetType = 0;  // 0 :直线 1 :矩形 2 :圆3 :三角形
var lineColor = 5;
var lineFont = 1;

var paint = {
    init: function () {
        this.load();
    },
    load: function () {
        this.x = [];//记录鼠标移动时的X坐标
        this.y = [];//记录鼠标移动时的Y坐标
        this.clickDrag = [];
        this.list_map = new Array();
        this.lock = false;//鼠标移动前，判断鼠标是否按下
        this.fontType = [1, 2, 3, 4];//画笔的样式 直线、圆、矩形、三角形
        this.color = ["#000000", "#D81E06", "#1296DB", "#2CB936", "#F4EA2A", "#E98F36", "#8A8A8A", "#A92DC8", "#811204", "#ffc0cb", "#FFFFFF"];
        this.fontWeight = [2, 5, 8];
        this.storageFontType = this.fontType[linetType]; //默认画笔样式为直线;
        this.storageColor = this.color[lineColor];//默认画笔颜色
        this.storageFont = this.fontWeight[lineFont];//默认画笔粗细
        this.$ = function (id) {
            return typeof id == "string" ? document.getElementById(id) : id;
        };
        this.canvas = this.$("gesture_canvas");
        if (this.canvas.getContext) {
        } else {
            CreateRemindWindow(event, "warning", "browserNotSupportCanvas");
            //alert(getLocalizedString(language, "warning", "browserNotSupportCanvas"));
            return;
        }
        //window.addEventListener('touchmove', function (e) {
        //    e.preventDefault();
        //}, { passive: false });
        document.ontouchmove = function () {
            if (e.cancleable) {
                if (!e.defaultPrevented) {
                    e.preventDefault();
                    e.stopPropagation();
                }
            }
        }
        this.reset();
        this.revocation = this.$("sview_undo");//撤销按钮
        this.confim = this.$("button_gesture3D_confirm");
        // $(document).on('touchmove',function(e){
        //    e.preventDefault();
        // })
        //this.cxt=this.canvas.getContext('2d');
        //this.cxt.lineJoin = "round";//context.lineJoin - 指定两条线段的连接方式
        //this.cxt.lineWidth = this.storageFont;//线条的宽度
        //this.cxt.strokeStyle = this.storageColor;//画笔颜色
        //this.revocation=this.$("gesture_undo");//撤销按钮
        this.w = this.canvas.width;//取画布的宽
        this.h = this.canvas.height;//取画布的高
        this.touch = ("createTouch" in document);//判定是否为手持设备
        this.StartEvent = this.touch ? "touchstart" : "mousedown";//支持触摸式使用相应的事件替代
        this.MoveEvent = this.touch ? "touchmove" : "mousemove";
        this.EndEvent = this.touch ? "touchend" : "mouseup";
        this.bind();
    },
    reset: function () {
        if (this.canvas) {
            this.cxt = this.canvas.getContext('2d');
            this.cxt.lineJoin = "round";//context.lineJoin - 指定两条线段的连接方式
            this.cxt.lineWidth = this.storageFont;//线条的宽度
            this.cxt.strokeStyle = this.storageColor;//画笔颜色
        }
    },
    bind: function () {
        var t = this;
        /*鼠标按下事件，记录鼠标位置，并绘制，解锁lock，打开mousemove事件*/
        this.canvas['on' + t.StartEvent] = function (e) {
            e.preventDefault();
            e.stopPropagation();
            t.width = sviewFrameDiv.offsetWidth;
            t.height = sviewFrameDiv.offsetHeight;
            t.w = sviewFrameDiv.offsetWidth;
            t.h = sviewFrameDiv.offsetHeight;
            if (e.cancleable) {
                if (!e.defaultPrevented) {
                    e.preventDefault();
                }
            }
            var touch = t.touch ? e.touches[0] : e;
            //var _x = touch.offsetX != undefined ? touch.offsetX : e.layerX;//鼠标在画布上的x坐标，以画布左上角为起点
            //var _y = touch.offsetY != undefined ? touch.offsetY : e.layerY;//鼠标在画布上的y坐标，以画布左上角为起点
            var _x = touch.offsetX != undefined ? touch.offsetX : (e.layerX ? e.layerX : (touch.clientX ? touch.clientX : touch.pageX));//鼠标在画布上的x坐标，以画布左上角为起点
            var _y = touch.offsetY != undefined ? touch.offsetY : (e.layerY ? e.layerY : (touch.clientY ? touch.clientY : touch.pageY));//鼠标在画布上的y坐标，以画布左上角为起点
            t.movePoint(_x, _y);//记录鼠标位置
            if (t.storageFontType == 1) {
                t.drawPoint();//绘制路线
            }
            t.lock = true;

        };
        /*鼠标移动事件*/
        this.canvas['on' + t.MoveEvent] = function (e) {
            e.preventDefault();
            e.stopPropagation();
            t.width = sviewFrameDiv.offsetWidth;
            t.height = sviewFrameDiv.offsetHeight;
            t.w = sviewFrameDiv.offsetWidth;
            t.h = sviewFrameDiv.offsetHeight;

            if (e.cancleable) {
                if (!e.defaultPrevented) {
                    e.preventDefault();
                }
            }
            var touch = t.touch ? e.touches[0] : e;
            if (t.lock) {     //t.lock为true则执行
                //var _x = touch.offsetX != undefined ? touch.offsetX : e.layerX;//鼠标在画布上的x坐标，以画布左上角为起点
                //var _y = touch.offsetY != undefined ? touch.offsetY : e.layerY;//鼠标在画布上的y坐标，以画布左上角为起点
                var _x = touch.offsetX != undefined ? touch.offsetX : (e.layerX ? e.layerX : (touch.clientX ? touch.clientX : touch.pageX));//鼠标在画布上的x坐标，以画布左上角为起点
                var _y = touch.offsetY != undefined ? touch.offsetY : (e.layerY ? e.layerY : (touch.clientY ? touch.clientY : touch.pageY));//鼠标在画布上的y坐标，以画布左上角为起点
                t.movePoint(_x, _y, true);//记录鼠标位置
                t.drawPoint();//绘制路线
            }

        };
        this.canvas['on' + t.EndEvent] = function (e) {
            e.preventDefault();
            e.stopPropagation();
            t.width = sviewFrameDiv.offsetWidth;
            t.height = sviewFrameDiv.offsetHeight;
            t.w = sviewFrameDiv.offsetWidth;
            t.h = sviewFrameDiv.offsetHeight;

            /*重置数据*/
            var newmap = {};
            newmap['x'] = t.x;
            newmap['y'] = t.y;
            newmap['clickDrag'] = t.clickDrag;
            newmap['color'] = t.cxt.strokeStyle;
            newmap['font'] = t.cxt.lineWidth;
            t.list_map.push(newmap);
            t.lock = false;
            t.x = [];
            t.y = [];
            t.clickDrag = [];

        };
        this.revocation.onclick = function () {  //撤销
            t.redraw();
            console.log(t.list_map);
            if (t.storageFontType == 2 || t.storageFontType == 3 || t.storageFontType == 4) {
                t.list_map = [];
            }
        };
        this.confim.onclick = function () {
            //console.log(t.x + "------" + t.y);
            sview.Create3DGestureNote(t.list_map, t.storageFontType);
            paint.cxt.clearRect(0, 0, screenWidth, screenHeight);
            paint.list_map.length = 0;
        }
        this.changeColor();
    },
    movePoint: function (x, y, dragging) {
        /*将鼠标坐标添加到各自对应的数组里*/
        if (this.x.length > 0) {
            var lastx = this.x[this.x.length - 1];
            var lasty = this.y[this.y.length - 1];
            if ((x * x + y * y) - (lastx * lastx + lasty * lasty) >= 9 || (lastx * lastx + lasty * lasty) - (x * x + y * y) >= 9) {
                this.x.push(x);
                this.y.push(y);
                this.clickDrag.push(y);
            }
        } else {
            this.x.push(x);
            this.y.push(y);
            this.clickDrag.push(y);
        }


    },
    drawPoint: function (x, y, radius) {

        for (var i = 0; i < this.x.length; i++) {  //循环数组
            if (this.storageFontType == 1) {
                //画线

                this.cxt.beginPath(); //context.beginPath() , 准备绘制一条路径
                if (this.clickDrag[i] && i) {  //当是拖动而且i!=0时，从上一个点开始画线。
                    this.cxt.moveTo(this.x[i - 1], this.y[i - 1]);//context.moveTo(x, y) , 新开一个路径，并指定路径的起点
                } else {
                    this.cxt.moveTo(this.x[i] - 1, this.y[i]);
                }
                this.cxt.lineTo(this.x[i], this.y[i]);//context.lineTo(x, y) , 将当前点与指定的点用一条笔直的路径连接起来
                this.cxt.closePath();//context.closePath() , 如果当前路径是打开的则关闭它
                this.cxt.stroke();//context.stroke() , 绘制当前路径
            }
            if (this.storageFontType == 2) {
                //圆形
                this.cxt.clearRect(0, 0, screenWidth, screenHeight);
                this.cxt.beginPath();//开始一个新的绘制路径
                this.cxt.arc(this.x[0], this.y[0], Math.pow(((this.x[i] - this.x[0]) * (this.x[i] - this.x[0]) + (this.y[i] - this.y[0]) * (this.y[i] - this.y[0])), 0.5), 0, 2 * Math.PI, true);
                this.cxt.stroke();
            }
            if (this.storageFontType == 3) {
                //矩形
                this.cxt.clearRect(0, 0, screenWidth, screenHeight);
                this.cxt.strokeRect(this.x[0], this.y[0], this.x[i] - this.x[0], this.y[i] - this.y[0]);
            }
            if (this.storageFontType == 4) {
                //三角形
                this.cxt.clearRect(0, 0, screenWidth, screenHeight);
                this.cxt.beginPath();//开始一个新的绘制路径
                /*
                //直角三角形
                this.cxt.moveTo(this.x[0], this.y[0]);
                this.cxt.lineTo(this.x[0], this.y[i]);//context.lineTo(x, y) , 将当前点与指定的点用一条笔直的路径连接起来
                this.cxt.lineTo(this.x[i], this.y[i]);
                this.cxt.closePath();//context.closePath() , 如果当前路径是打开的则关闭它
                this.cxt.stroke();//context.stroke() , 绘制当前路径*/
                //等腰三角形
                this.cxt.moveTo(this.x[0] + (this.x[i] - this.x[0]) / 2, this.y[0]);
                this.cxt.lineTo(this.x[0], this.y[i]);
                this.cxt.lineTo(this.x[i], this.y[i]);
                this.cxt.closePath();
                this.cxt.stroke();
            }
        }
    },
    redraw: function () {
        /*撤销*/
        var t = this;
        t.cxt.restore();
        t.cxt.clearRect(0, 0, screenWidth, screenHeight);
        if (t.storageFontType == 1) {
            t.list_map.pop();
            for (var j = 0; j < t.list_map.length; j++) {
                var map = t.list_map[j];
                t.cxt.strokeStyle = map['color'];
                t.cxt.lineWidth = map['font'];
                for (var i = 0; i < map['x'].length; i++) {
                    t.cxt.beginPath(); //context.beginPath() , 准备绘制一条路径
                    if (map['clickDrag'][i] && i) {  //当是拖动而且i!=0时，从上一个点开始画线。
                        t.cxt.moveTo(map['x'][i - 1], map['y'][i - 1]);//context.moveTo(x, y) , 新开一个路径，并指定路径的起点
                    } else {
                        t.cxt.moveTo(map['x'][i] - 1, map['y'][i]);
                    }
                    t.cxt.lineTo(map['x'][i], map['y'][i]);//context.lineTo(x, y) , 将当前点与指定的点用一条笔直的路径连接起来
                    t.cxt.closePath();//context.closePath() , 如果当前路径是打开的则关闭它
                    t.cxt.stroke();//context.stroke() , 绘制当前路径		
                }
            }
        } else if (M3D.Parameters.getInstance().isAPPUse) {
            if (t.storageFontType == 2) {
                t.list_map.pop();
                for (var j = 0; j < t.list_map.length; j++) {
                    var map = t.list_map[j];
                    t.cxt.strokeStyle = map['color'];
                    t.cxt.lineWidth = map['font'];
                    //for (var i = 0; i < map['x'].length; i++) {
                    t.cxt.beginPath(); //context.beginPath() , 准备绘制一条路径
                    t.cxt.arc(map.x[0], map.y[0], Math.pow(((map.x[map.x.length - 1] - map.x[0]) * (map.x[map.x.length - 1] - map.x[0]) + (map.y[map.y.length - 1] - map.y[0]) * (map.y[map.y.length - 1] - map.y[0])), 0.5), 0, 2 * Math.PI, true);
                    t.cxt.stroke();
                    //}
                }
            } else if (t.storageFontType == 3) {
                t.list_map.pop();
                for (var j = 0; j < t.list_map.length; j++) {
                    var map = t.list_map[j];
                    t.cxt.strokeStyle = map['color'];
                    t.cxt.lineWidth = map['font'];
                    //for (var i = 0; i < map['x'].length; i++) {
                    t.cxt.strokeRect(map.x[0], map.y[0], map.x[map.x.length - 1] - map.x[0], map.y[map.x.length - 1] - map.y[0]);
                    //}
                }
            } else if (t.storageFontType == 4) {
                t.list_map.pop();
                for (var j = 0; j < t.list_map.length; j++) {
                    var map = t.list_map[j];
                    t.cxt.strokeStyle = map['color'];
                    t.cxt.lineWidth = map['font'];
                    //for (var i = 0; i < map['x'].length; i++) {
                    this.cxt.beginPath();//开始一个新的绘制路径

                    //等腰三角形
                    t.cxt.moveTo(map.x[0] + (map.x[map.x.length - 1] - map.x[0]) / 2, map.y[0]);
                    t.cxt.lineTo(map.x[0], map.y[map.x.length - 1]);
                    t.cxt.lineTo(map.x[map.x.length - 1], map.y[map.x.length - 1]);
                    t.cxt.closePath();
                    t.cxt.stroke();
                    //}
                }
            }
        }
        t.cxt.lineWidth = t.storageFont;
        t.cxt.strokeStyle = t.storageColor;
    },
    preventDefault: function (e) {   //暂时不知道哪里调用
        /*阻止默认*/
        var touch = this.touch ? e.touches[0] : e;
        if (this.touch) {
            touch.preventDefault();
            touch.stopPropagation();
        }
        else { window.event.returnValue = false; }
    },
    changeColor: function () {
        /*为按钮添加事件*/
        var t = this;
        var typeNum = this.$("gestureType").getElementsByTagName("div");
        var colNum = this.$("gesture_color").getElementsByTagName("div");
        //画笔样式
        for (var i = 0; i < typeNum.length; i++) {
            t.cxt.save();
            typeNum[i].index = i;
            typeNum[i].onclick = function () {
                t.storageFontType = t.fontType[this.index];
                selectImg(this.id, "gesture_global");
            }
        }
        //颜色
        for (var i = 0; i < colNum.length; i++) {
            colNum[i].index = i;
            colNum[i].onclick = function () {
                t.cxt.save();
                t.cxt.strokeStyle = t.color[this.index];
                t.storageColor = t.color[this.index];
                t.cxt.strokeStyle = t.storageColor;
                selectImg(this.id, "gesture_color_info");
            }
        }
    }
};
