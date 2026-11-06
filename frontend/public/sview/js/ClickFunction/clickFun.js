/* ------------------ 提示信息框 ------------------- */
function showResultDialog(type, title, content) {
  SuccessInfo.ResultDialog.icon.type = type;
  SuccessInfo.ResultDialog.titleContent = title;
  SuccessInfo.ResultDialog.detailContent = content;
  let uiManager = sview0.sviewFrame.getUIManager();
  var resultDialog = document.getElementById(SuccessInfo.ResultDialog.id);
  let resultElement;
  if (resultDialog == null) {
    uiManager.load(SuccessInfo);
    resultElement = uiManager.getElement(SuccessInfo.ResultDialog.id);
  } else {
    resultElement = uiManager.getElement(SuccessInfo.ResultDialog.id);
    resultElement.setTitle(title);
    resultElement.setDetail(content);
    resultElement.setImgsrc("./images/info/" + type + ".png");
    resultElement.show();
  }
  setTimeout(function () {
    resultElement.hide();
  }, 5000);
}
/* ------------------ 关闭弹窗 ------------------- */
function closeDialog(elementId, btnId = null) {
  //rightClickMenu(true);
  // 删除相应标签
  sview0.sviewFrame.getUIManager().removeLabel(elementId);
  // 隐藏相应标签
  //sview0.sviewFrame.getUIManager().hideLabel(elementId);
  // 对应按钮取消高亮
  if (btnId != null) {
    SView.Windows.Button.highLightLabel(btnId);
  }
  var meetingDialog = document.getElementById(MeetingDialog.MeetingDialog.id);
  if (meetingDialog) {
    meetingDialog.style.display = "block";
  }
}

/* ------------------ 确认删除弹出框 ------------------- */
function confirmDelete(elementId) {
  let uiManager = sview0.sviewFrame.getUIManager();
  let selector = sview0.sviewFrame.getViewer().getScene().getSelector();
  let shape = selector.get();
  let type = 0;
  if (elementId == "deleteAnnotate") {
    type = 1;
  } else if (elementId == "deleteMeasure") {
    type = 2;
  }
  if (elementId == "deleteAnnotate" || elementId == "deleteMeasure") {
    if (shape != null) {
      if (shape instanceof M3D.Shapes.Annotation) {
        DeleteDialog.Dialog.content =
          SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSelectedAnnotation;
      }
      if (shape instanceof M3D.Shapes.Measure) {
        DeleteDialog.Dialog.content =
          SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSelectedMeasure;
      }
      DeleteDialog.Dialog.buttons[0].Button.onClick = "deleteSign('confirm-delete')";
      if (shape instanceof M3D.Shapes.Model) {
        if (type === 1) {
          MessageDialog.Message.content =
            SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneAnnotation;
        } else if (type === 2) {
          MessageDialog.Message.content =
            SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneMeasure;
        }
        // 创建提示信息弹出框
        uiManager.load(MessageDialog);
        setTimeout(function () {
          uiManager.removeLabel(MessageDialog.Message.id);
        }, 3000);
        return;
      }
    } else {
      if (type === 1) {
        MessageDialog.Message.content =
          SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneAnnotation;
      } else if (type === 2) {
        MessageDialog.Message.content =
          SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneMeasure;
      }
      // 创建提示信息弹出框
      uiManager.load(MessageDialog);
      setTimeout(function () {
        uiManager.removeLabel(MessageDialog.Message.id);
      }, 3000);
      return;
    }
  }
  //if (elementId == 'deleteMeasure') {
  //    //不进行类型判断，只判断有没有选中对象
  //    if (shape != null) {
  //        if (shape instanceof M3D.Shapes.Annotation) {
  //            DeleteDialog.Dialog.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSelectedAnnotation;
  //        }
  //        if (shape instanceof M3D.Shapes.Measure) {
  //            DeleteDialog.Dialog.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.DeleteSelectedMeasure;
  //        }
  //        DeleteDialog.Dialog.buttons[0].Button.onClick = "deleteSign('confirm-delete')";
  //        if (shape instanceof M3D.Shapes.Model) {
  //            MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneMeasure;
  //            // 创建提示信息弹出框
  //            uiManager.load(MessageDialog);
  //            setTimeout(function () {
  //                uiManager.removeLabel(MessageDialog.Message.id);
  //            }, 3000);
  //            return;
  //        }
  //    } else {
  //        MessageDialog.Message.content = SView[SView.UIManager.languageInfo].languageObj.Prompt.PleaseSelectOneMeasure;
  //        // 创建提示信息弹出框
  //        uiManager.load(MessageDialog);
  //        setTimeout(function () {
  //            uiManager.removeLabel(MessageDialog.Message.id);
  //        }, 3000);
  //        return;
  //    }
  //}
  //if (elementId == 'clearAnnotates') {
  //    DeleteDialog.Dialog.content = SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle.ClearAnnotationPrompt;
  //}
  //if (elementId == 'clearMeasurements') {
  //    DeleteDialog.Dialog.content = SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle.ClearMeasurePrompt;
  //}
  //if (elementId == 'clearSectionPlanes') {
  //    DeleteDialog.Dialog.content = SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle.ClearSectionPrompt;
  //}
  //DeleteDialog.Dialog.buttons[0].onClick = "deleteSign('confirm-delete')";
  sview0.sviewFrame.getUIManager().load(DeleteDialog);
}
/* ------------------ 播放动画前，提示保存视图 ------------------- */
function confirmPlayAnimation(commandType) {
  //let okFun = function () {
  //    commandClickFun('', commandType);
  //}
  //let cancleFun = function () {
  //    sview0.sviewFrame.getUIManager().removeLabel(ExitDialog.Dialog.id);
  //}
  //confirmExit("", SView[SView.UIManager.languageInfo].languageObj.Prompt.SaveCurrentAnnotation, okFun, cancleFun);
  ExitDialog.Dialog.content =
    SView[SView.UIManager.languageInfo].languageObj.Prompt.PlayCameraPreTip;
  ExitDialog.Dialog.buttons[0].Button.onClick = "commandClickFun('', '" + commandType + "')";
  sview0.sviewFrame.getUIManager().load(ExitDialog);
}

/* ------------------ 确认退出弹出框 ------------------- */
function confirmExit(exitDialog, content, okFun, cancleFun) {
  let showDialog = window.extMethods?.confirmExitShowDialog?.(exitDialog);
  if (showDialog === false) {
    okFun ? okFun() : (0, eval("exitBottomMenu(" + exitDialog + ")"));
    return;
  }
  if (exitDialog && !content) {
    ExitDialog.Dialog.content =
      SView[SView.UIManager.languageInfo].languageObj.Prompt.ExitCurrentOperation;
    ExitDialog.Dialog.buttons[0].Button.onClick = "exitBottomMenu(" + exitDialog + ")";
  }
  if (content && okFun && cancleFun) {
    ExitDialog.Dialog.content = content;
    ExitDialog.Dialog.buttons[0].Button.onClick = okFun;
    ExitDialog.Dialog.buttons[1].Button.onClick = cancleFun;
  }
  sview0.sviewFrame.getUIManager().load(ExitDialog);
}
/* ------------------ 折叠面板关闭打开 ------------------- */
//function collapseClose(e) {
//    // console.log(e);
//    if (e.getElementsByClassName("SView-icon-open").length > 0) {
//        e.getElementsByClassName("SView-icon-open")[0].setAttribute("class", "SView-icon-close");
//        e.nextElementSibling.style.display = "none";
//    } else {
//        e.getElementsByClassName("SView-icon-close")[0].setAttribute("class", "SView-icon-open");
//        e.nextElementSibling.style.display = "block";
//    }
//}

/* ------------------ 切换标签页 ------------------- */
function changeTabs(elementId, dataClass) {
  let tabs = sview0.sviewFrame.getUIManager().getElement(elementId);
  tabs.setActiveTab(dataClass);
}
/* ------------------ 检查输入框内容长度 ------------------- */
function checkLength(elementId, id) {
  var element = document.getElementById(elementId);
  var fatherElement = element.parentNode;
  var warning = fatherElement.getElementsByClassName("SView-warningPopover")[0];
  if (warning != null && element.value == "") {
    warning.style.display = "";
    setTimeout(function () {
      warning.style.display = "none";
    }, 3000);
  } else {
    warning = fatherElement.getElementsByTagName("p")[0];
    if (element.value.length == 0) {
      warning.style.display = "";
    } else {
      warning.style.display = "none";
      if (id == AssemblyDialog.TreeDialog.id) {
        var node = tree.getNodeByName(element.value);
        tree.setNodeSelect(node);
      }
    }
  }
  if (elementId == "meetingNickName") {
    nickName = element.value;
  }
}

// //自定义一个浏览器右键菜单，单击右键是显示它
function offset(ele) {
  var obj = { t: 0, l: 0 };
  obj.l = ele.offsetLeft;
  obj.t = ele.offsetTop;

  while (ele.offsetParent) {
    //寻找父级，当寻找到 body 往上的时候为 null ，所以循环停止
    ele = ele.offsetParent; //初始值
    obj.l += ele.offsetLeft;
    obj.t += ele.offsetTop;
  }

  return obj;
}
//自定义一个浏览器右键菜单，单击右键是显示它
function showRightClickMenu(e) {
  var rm;
  //显示我们自己定义的右键菜单
  let viewPort = sview0.sviewFrame.getViewer().getTouchViewport(e);
  let selector = viewPort.getScene().getSelector();
  let shapeList = selector.getAll();
  if (shapeList && shapeList.length > 0) {
    if (
      viewPort
        .getScene()
        .getMaterialManager()
        .hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Jewel) ||
      viewPort
        .getScene()
        .getMaterialManager()
        .hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Diamond) ||
      viewPort
        .getScene()
        .getMaterialManager()
        .hasSpecificTypeMaterial(M3D.Render.MaterialType.MaterialType_Metal) ||
      viewPort.getScene().getMaterialManager().hasSpecificTypeMaterial(102)
    ) {
      rm = document.getElementById(RightClickSelectRingMenu.RightClickMenu.id);
    } else {
      if (shapeList[0] instanceof M3D.Shapes.Annotation) {
        rm = document.getElementById(RightClickSelectAnnotateMenu.RightClickMenu.id);
      } else if (
        shapeList[0] instanceof M3D.Shapes.Measure ||
        shapeList[0] instanceof M3D.Shapes.ThreeDGesturesNote
      ) {
        rm = document.getElementById(RightClickSelectMeasureMenu.RightClickMenu.id);
      } else if (shapeList[0] instanceof M3D.Shapes.Model) {
        rm = document.getElementById(RightClickSelectMenu.RightClickMenu.id);
      } else {
        //TODO  nihy 2023-3-3 其他不弹出右键，如PMI、看板、热点等，后续有新需求再适配处理
      }
    }
  } else {
    rm = document.getElementById(RightClickMenu.RightClickMenu.id);
  }
  if (rm) {
    //1.找到菜单
    //兼容Event对象
    e = e || window.event;
    e.stopPropagation();
    //2.设置菜单的位置等于鼠标的坐标
    //判断：如果鼠标的位置+菜单的宽度>网页的宽度，那么就改为右边定位
    //鼠标坐标
    var mx = e.clientX || e.changedTouches[0].clientX;
    var my = e.clientY || e.changedTouches[0].clientY;
    rm.style.display = "block";
    //菜单宽度
    var rmWidth = rm.offsetWidth;
    var rmHeight = rm.offsetHeight;
    //网页的宽度(高度用同样的方法解决)
    var pageWidth = document.documentElement.clientWidth;
    var pageHeight = document.documentElement.clientHeight;
    //console.log(pageWidth);
    if (mx + rmWidth < pageWidth) {
      rm.style.left = mx + "px";
    } else {
      rm.style.left = mx - rmWidth + "px";
    }
    if (my + rmHeight < pageHeight) {
      rm.style.top = my + "px";
    } else {
      rm.style.top = my - rmHeight + "px";
    }
  }
  //3.显示右键菜单
  //点击其他位置隐藏
  //document.documentElement.onclick = function (e) {
  //    if (rm && rm.style) {
  //        rm.style.display = "none";
  //    }
  //}
  //阻止默认的右键菜单显示
  return false;
}
//oncontextmenu上下文菜单事件，右键菜单
function rightClickMenu(type) {
  let startTime, endTime;
  let pageX = 0;
  let pageY = 0;
  let touchstart = function (event) {
    startTime = new Date();
    pageX = event.changedTouches[0].clientX;
    pageY = event.changedTouches[0].clientY;
  };
  let touchend = function (event) {
    endTime = new Date();
    if (
      endTime - startTime > 700 &&
      event.changedTouches[0].clientX == pageX &&
      event.changedTouches[0].clientY == pageY
    ) {
      showRightClickMenu(event);
    }
  };
  if (type) {
    var system = {};
    var p = navigator.platform;
    system.win = p.indexOf("Win") == 0;
    system.mac = p.indexOf("Mac") == 0;
    system.x11 = p == "X11" || p.indexOf("Linux") == 0;
    if (system.win || system.mac || system.xll) {
      document.documentElement.oncontextmenu = function (e) {
        showRightClickMenu(e);
      };
    } else {
      document.addEventListener("touchstart", touchstart, false);
      document.addEventListener("touchend", touchend, false);
    }
  } else {
    document.removeEventListener("touchstart", touchstart, false);
    document.removeEventListener("touchend", touchend, false);
    document.documentElement.oncontextmenu = function (e) {
      return false;
    };
  }
}

//右键菜单相关方法
function showSettingColor() {
  rightClickMenu(false);
  var colorDialog = document.getElementById(ColorDialog.Dialog.id);
  let uiManager = sview0.sviewFrame.getUIManager();
  if (colorDialog == null) {
    colorDialog = uiManager.load(ColorDialog);
    //Colorpicker.create({
    //    el: "color-picker", //元素id
    //    color: "#000fff", //默认颜色
    //    change: function (elem, hex) {
    //        //选中颜色发生改变时事件
    //        // elem.style.backgroundColor = hex;
    //    }
    //})
  } else {
    uiManager.removeLabel(ColorDialog.Dialog.id);
    colorDialog = null;
  }
}
//会议相关
function showMeetingDialog(id, dialog) {
  rightClickMenu(false);
  document.getElementById(id).style.display = "none";
  let uiManager = sview0.sviewFrame.getUIManager();
  uiManager.load(dialog);
}

//获取设置页面的初始值
function getSettingValue(key, type = "number") {
  var value = null;
  switch (type) {
    case "number":
      value = M3D.Config.Parameters.getInstance().getNumberParameter(key);
      break;
    case "vector2":
      value = M3D.Config.Parameters.getInstance().getVector2Parameter(key);
      break;
    case "color":
      value = M3D.Config.Parameters.getInstance().getColorParameter(key);
      break;
    case "string":
      value = M3D.Config.Parameters.getInstance().getStringParameter(key);
      break;
    case "boolean":
      value = M3D.Config.Parameters.getInstance().getBoolParameter(key);
      break;
  }
  return value;
}
function scrollToFun(dataClass, id) {
  let uiManager = sview0.sviewFrame.getUIManager();
  if (uiManager.getElement(id)) {
    uiManager.getElement(id).setActiveTab(dataClass);
    let item = document.getElementById(dataClass);
    let wrapper = item.parentNode;
    if (item.offsetTop <= wrapper.offsetTop) {
      wrapper.scrollTo(0, 0);
    } else {
      wrapper.scrollTo(0, item.offsetTop);
    }
  } else {
    return;
  }
}
