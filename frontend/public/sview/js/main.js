"use strict";
/* global 
  $
  M3D 
  SView 
  RightMenu 
  RightClickMenu 
  RightClickSelectMenu 
  RightClickSelectMeasureMenu 
  RightClickSelectAnnotateMenu 
  RightClickSelectRingMenu 
  rightClickMenu 
  Main
  initRightMenu
  initRightClickMenu
  HoteamUI
*/
window.onload = function () {
  const initiator = "All",
    noteTextEnum = {
      AnnotationData: ["setting", "AnnotationData"],
      note: ["BottomMenuTitle", "_note"],
      node_0: ["NoteMenuTitle", "node_text"], // 文本
      node_1: ["NoteMenuTitle", "note_component"], // 名称
      node_2: ["NoteMenuTitle", "note_order"], // 序号
      line_0: ["GestureMenuTitle", "freedom"], // 线
      line_1: ["GestureMenuTitle", "circle"], // 圆
      line_2: ["GestureMenuTitle", "rect"], // 矩形
      line_3: ["GestureMenuTitle", "triangle"], // 三角形
    },
    errorTextEnum = {
      SequenceRepeat: ["TextNote", "SequenceRepeat"],
      FormatError: ["TextNote", "FormatError"],
    },
    tipsEnum = {
      saveSuccess: ["Prompt", "SaveSuccess"],
      saveError: ["Prompt", "SaveError"],
      prohibitEdit: ["Prompt", "ProhibitEdit"],
    },
    // 蓝色背景对应颜色，sview 如果改动会影响到设置窗口中的选中状态
    bgTopColorStr = "#40a0ff",
    bgBottomColorStr = "#e1f5ff";

  window.extMethods = {
    removeAnnotationCheck(shapes, type) {
      let result = true;
      // 1 为批注的情况
      if (type == 1) {
        result = removeAnnotateCheck(shapes);
      }

      return result;
    },
    editAnnotationCheck(annotation) {
      let result = removeAnnotateCheck([annotation], 1);

      if (result === false) {
        sviewCreateTips(...tipsEnum.prohibitEdit);
      }

      return result;
    },
    addedGestureAnnotation(lineType) {
      sviewUpdateGestureAnnotation(lineType);
    },
    // enterEditAnnotate() {
    //   selectedMulti = true;
    //   sviewUpdateSelectByNode();
    // },
    // exitEditAnnotate() {
    //   sviewUpdateSelectByNode();
    //   selectedMulti = false;
    // },
    updatedAnnotation(annotation) {
      addAnnotateNode(annotation);
    },
    removeAnnotation(annotations) {
      removeAnnotateNode(annotations);
    },
    openFinished,
    editAnnotationValidate,
    confirmExitShowDialog(exitDialog) {
      return !(exitDialog === "AnnotateMenu");
    },
    restoreSettings(instance, commands) {
      sviewRestoreSettings(instance, commands);
    },
  };

  let _window = window.opener || window.parent,
    sview0,
    ztree,
    AppSets,
    params = {},
    taskUIDObj,
    ctrlData,
    nodeMap = new Map(),
    selectedMulti = false,
    maxOrderNum = 0;

  init();

  // 初始化
  function init() {
    let search = window.location.search.substring(1),
      ctrlid;

    params = getParams(search);
    ({ ctrlid } = params);

    if (!ctrlid) {
      return;
    } else {
      params = JSON.parse(sessionStorage.getItem("Hoteam_" + ctrlid + "_Params") || "{}");
    }

    // 处理跨域的情况，通过 _window 获取参数报错
    try {
      ctrlData = _window?.hoteamctrl?.[ctrlid];
    } catch (err) {
      ctrlData = null;
      _window = null;
    }

    if (ctrlData) {
      AppSets = window.AppSets = _window.AppSets;
      sessionStorage.setItem("AppSets", JSON.stringify(AppSets));
    } else {
      window.AppSets = JSON.parse(sessionStorage.getItem("AppSets") || null);
    }

    sviewInit();
    bindEvent();
  }

  function openFinished() {
    // 添加定时器延时加载，直接调用会出现批注不显示的问题
    setTimeout(() => {
      initAnnotate();
    }, 200);
  }

  // 初始化批注
  function initAnnotate() {
    const { mark, annotateUrl } = params;

    if (!mark || !annotateUrl) {
      return;
    }

    let annotateData = [];
    annotateUrl.forEach((url) => {
      $.ajax({
        url: url,
        type: "GET",
        //dataType: "json",
        async: false,
        success: function (result) {
          let annotations;

          if (typeof result === "string") {
            result = JSON.parse(result);
          }

          annotations = result.annotations || [];
          annotateData = annotateData.concat(annotations);
        },
        error: function (e) {
          console.log(e);
        },
      });
    });
    createAnnotateContent();
    // todo
    // propertyListDrag("userAnnotate")

    annotateData = processAnnotateData(annotateData);
    initTree(annotateData);
    updateMaxOrderNum();
    bindAnnotateEvent();
  }

  // 修改SView按钮全局数据的方式隐藏批注按钮
  function hideAnnotateMenu() {
    const rightMenu = window.RightMenu?.RightMenu?.subComponents || [],
      buttonMenu = window.MainBottomMenu?.BottomMenu?.subComponents || [],
      annotateMenu = rightMenu.find((i) => i?.Button?.id === "annotateMenu"),
      annotateBtn = buttonMenu.find((i) => i?.Button?.id === "annotate");

    if (annotateMenu) {
      annotateMenu.Button.isShow = false;
    }

    if (annotateBtn) {
      annotateBtn.Button.isShow = false;
    }

    // $("#annotateMenu").remove();
    // $("#annotate").remove();
  }

  function createAnnotateContent() {
    let html = `
      <div class="annotate-tree-dialog annotateTree hidden">
        <div class="SView-dialog-title dialog-title">
          <span class="SView-dialog-title-name">${sviewGetText(
            ...noteTextEnum.AnnotationData
          )}</span>
          <div class="SView-dialog-title-closeBtn SView-button-iconBtn annotate-tree-close">
            <img src="./images/meeting/MeetingExist.png">
          </div>
        </div>
        <div class="SView-dialog-content" style="border: none;">
          <ul id="userAnnotateContent"  class="ztree" style="overflow-x:auto;resize:none;height:100%;max-height: 280px;"></ul>
        </div>
      </div>
    `;

    $(document.body).append(html);
  }

  // 处理批注数据，创建显示批注
  function processAnnotateData(data = []) {
    let result = [],
      map = new Map(),
      { User } = params,
      userNodeData;

    if (data.length === 0) {
      return result;
    }

    data.forEach((node) => {
      // const { attributes, text, type, noteType = "node" } = node,
      //   { author, guid, taskUID } = attributes;
      const { user, guid, note, noteType = "node", taskUID, text, type } = node;

      let rootNode = map.get(user),
        nodeData,
        children,
        visible;

      nodeMap.set(guid, node);

      if (!rootNode) {
        rootNode = {
          name: user,
          check: false,
          children: [],
        };
        map.set(user, rootNode);
      }

      children = rootNode.children;
      visible = checkVisible(taskUID, user);

      nodeData = {
        guid,
        name: getNodeText(node), // `[${type}]-${text}`
        user,
        type,
        noteType,
        checked: visible,
      };

      children.push(nodeData);

      // 序号类型
      if (noteType == "node" && type == 2) {
        maxOrderNum = Math.max(maxOrderNum, text - 0);
      }

      if (visible) {
        rootNode.checked = true;
        rootNode.open = true;
        sviewCreateAnnotation(node);
      }
    });

    userNodeData = map.get(User);

    // 当前用户节点调整为第一个
    if (userNodeData) {
      map.delete(User);
      result = [userNodeData, ...map.values()];
    } else {
      result = [...map.values()];
    }

    return result;
  }

  // 校验批注是否显示
  function checkVisible(taskUID, user) {
    let { ShowMarkSet: showMarkSet, User } = params,
      visible = false;

    if (!taskUIDObj && showMarkSet) {
      taskUIDObj = {};
      showMarkSet = showMarkSet.split(";");
      showMarkSet.forEach((i) => {
        let nowSet = i.split("|");
        taskUIDObj[nowSet[0]] = nowSet[1];
      });
    }

    if (initiator === User) {
      visible = true;
    } else if (taskUIDObj?.[taskUID] == user || taskUIDObj?.[taskUID] == initiator) {
      visible = true;
    }

    return visible;
  }

  // 初始化树
  function initTree(data) {
    const $content = $("#userAnnotateContent");

    ztree = $.fn.zTree.init(
      $content,
      {
        view: {
          showIcon: false,
        },
        check: {
          enable: true,
          chkStyle: "checkbox",
          selectedMulti: false,
          chkboxType: {
            Y: "ps",
            N: "ps",
          },
        },
        callback: {
          onCheck: function (event, treeId, treeNode) {
            let { checked, isParent, children = [] } = treeNode,
              nodes = isParent ? children : [treeNode];

            nodes.forEach((tNode) => {
              let { guid } = tNode,
                node = nodeMap.get(guid);

              if (!node) {
                return;
              }

              if (checked) {
                sviewCreateAnnotation(node);
              } else {
                sviewRemoveAnnotation(node);
              }
            });

            sviewUpdateSelectByNode();
          },
          onClick: function (event, treeId, treeNode) {
            let selected = ztree.getSelectedNodes();

            if (!selectedMulti && selected.length > 1) {
              ztree.cancelSelectedNode();
              ztree.selectNode(treeNode, false);
            }

            sviewUpdateSelectByNode();
          },
        },
      },
      data
    );
  }

  // 获取节点文本
  function getNodeText(note) {
    let { text = "", type, noteType } = note,
      prefix,
      lang = noteTextEnum[`${noteType}_${type}`] || noteTextEnum["note"];

    // language 全局变量
    prefix = sviewGetText(...lang) || "note";

    text = `[${prefix}]${text ? ` ${text}` : ""}`;

    return text;
  }

  // 添加批注节点
  function addAnnotateNode(annotation, noteType = "node", sType) {
    let { guid, textString: text, annotationType } = annotation,
      { TaskUID: taskUID, User: user } = params,
      tNode = ztree.getNodeByParam("guid", guid),
      rootNode,
      rootNodes,
      node = nodeMap.get(guid),
      nodeData,
      note = sviewGetAnnotationJson(annotation, noteType),
      type = sType ?? annotationType;

    if (node) {
      node = $.extend(node, {
        note,
        text,
        taskUID,
      });

      $.extend(tNode, {
        name: getNodeText(node),
      });
      ztree.updateNode(tNode);
      return;
    }

    // 处理序号类型重复问题
    if (noteType == "node" && type == 2) {
      let _node;

      if (text <= maxOrderNum) {
        maxOrderNum = maxOrderNum + 1;

        note = JSON.stringify({
          ...JSON.parse(note),
          text: maxOrderNum + "",
        });

        _node = {
          guid,
          note,
          noteType: "node",
        };

        sviewRemoveAnnotation(_node);

        annotation = sviewCreateAnnotation(_node);
        text = annotation.textString;
      }
    }

    annotation.author = user;
    rootNode = ztree.getNodeByParam("name", user);

    node = {
      note,
      guid,
      taskUID,
      user,
      text,
      type,
      noteType,
    };

    nodeData = {
      checked: true,
      guid,
      user: user,
      type,
      noteType,
      name: getNodeText(node), // `[${type}]-${text}`
    };
    if (rootNode) {
      ztree.addNodes(rootNode, [nodeData]);
      ztree.expandNode(rootNode, true);
      ztree.checkNode(rootNode, true);
    } else {
      ztree.addNodes(null, {
        name: user,
        checked: true,
        open: true,
        children: [nodeData],
      });

      rootNode = ztree.getNodeByParam("name", user);
      rootNodes = ztree.getNodes();

      if (rootNodes.length > 1) {
        ztree.moveNode(rootNodes[0], rootNode, "prev");
      }
    }

    nodeMap.set(guid, node);
  }

  // 删除批注节点
  function removeAnnotateNode(annotationArr = []) {
    annotationArr.forEach((annotation) => {
      let { guid } = annotation,
        tNode = ztree.getNodeByParam("guid", guid),
        node = nodeMap.get(guid),
        noteType,
        type;

      if (!node) {
        return;
      }

      ({ noteType, type } = node);

      nodeMap.delete(guid);

      if (noteType == "node" && type == 2) {
        updateMaxOrderNum();
      }

      ztree.removeNode(tNode);
    });
  }

  // 删除批注校验
  function removeAnnotateCheck(annotationArr = []) {
    let result = true,
      { User: user } = params;

    for (let i = 0; i < annotationArr.length; i++) {
      const note = annotationArr[i],
        { guid } = note,
        node = nodeMap.get(guid);

      if (!node) {
        return false;
      }

      if (user != node.user) {
        result = false;
        break;
      }
    }

    if (!result) {
      setTimeout(() => {
        updateSelectedNode();
      });
    }

    return result;
  }

  function updateMaxOrderNum() {
    let num = 0;

    nodeMap.forEach((node) => {
      let { noteType, type, text } = node;

      if (noteType == "node" && type == 2) {
        num = Math.max(num, text - 0);
      }
    });

    maxOrderNum = num;
    // 更新 SView序号类型当前最大值
    M3D.Config.Parameters.getInstance().sequenceAnnotationMaxValue = maxOrderNum;
  }

  // 编辑批注校验
  function editAnnotateCheck(annotationArr) {}

  function editAnnotationValidate(newValue, note) {
    const { guid, annotationType } = note;

    if (annotationType == 2) {
      let regexRes = /^[1-9][0-9]?$/.test(newValue);

      if (!regexRes) {
        return sviewGetText(...errorTextEnum.FormatError);
      }

      let tNode = ztree.getNodesByFilter((node) => {
        const { guid: _guid, type } = node;

        if (type != 2 || _guid == guid) {
          return false;
        }

        let { text } = nodeMap.get(_guid);

        return text === newValue;
      });

      if (tNode.length > 0) {
        return sviewGetText(...errorTextEnum.SequenceRepeat);
      }
    }

    return;
  }

  // 更新批注节点选中状态
  function updateSelectedNode() {
    let selected = sviewGetSelected();

    ztree.cancelSelectedNode();

    selected.forEach((annotation) => {
      let { guid } = annotation,
        tNode;
      annotation = selected[0];

      if (!guid) {
        return;
      }

      tNode = ztree.getNodeByParam("guid", guid);

      if (tNode) {
        ztree.selectNode(tNode, selectedMulti);
      }
    });
  }

  // 绑定相关事件
  function bindEvent() {
    sviewBindEvent();
    bindDocumentEvents();
  }
//20230414 刘仁董 绑定mouseup触发_window的mouseup事件，解决弹窗移动时进入iframe无法取消移动
  function bindDocumentEvents(){
    $(document).on("mouseup", function () {
      if(_window){
        _window.$(_window.document).trigger("mouseup");
      }
    });
  }
  function bindAnnotateEvent() {
    let $body = $(document.body),
      { TaskUID: taskUID, User: user } = params;
    $body
      .on("click", "#saveAnnotate", function () {
        sviewResetAnnotateMenu();

        let nodes = sviewGetAllAnnotation(),
          cups = sviewGetAllGestureAnnotation(),
          saveArr = [];

        for (let i = 0, len = nodes.length; i < len; i++) {
          let node = nodes[i],
            { guid, textString: text, annotationType: type } = node,
            note,
            nodeData;
          if (node.author == null || node.author === "") {
            node.author = user;
          }

          note = sviewGetAnnotationJson(node);
          nodeData = nodeMap.get(guid);

          if (nodeData) {
            nodeData.note = note;
            nodeData.text = text;
            nodeData.type = type;
          } else {
            nodeMap.set(guid, {
              note,
              guid,
              taskUID,
              user,
              text,
              type,
              noteType: "node",
            });
          }
        }

        for (let i = 0, len = cups.length; i < len; i++) {
          let cup = cups[i],
            { guid } = cup,
            note,
            nodeData;

          if (cup.author == null || cup.author === "") {
            cup.author = user;
          }

          note = sviewGetAnnotationJson(cup, "line");
          nodeData = nodeMap.get(guid);

          if (nodeData) {
            nodeData.note = note;
          } else {
            nodeMap.set(guid, {
              note,
              guid,
              taskUID,
              user,
              noteType: "line",
            });
          }
        }

        nodeMap.forEach((value) => {
          if (value.user == user) {
            saveArr.push(value);
          }
        });

        saveAnnotate(
          JSON.stringify({
            annotations: saveArr,
          })
        );
      })
      .on("click", "#annotateMenu", function () {
        SView.Windows.Button.highLightLabel("annotateMenu");
        $(".annotateTree").toggleClass("hidden");
      })
      .on("click", "#annotateCloseButton", function () {
        $("#userAnnotate").addClass("hide");
        $("#annotateMenu").removeClass("bg-yellow");
      })
      .on("click", "#annotateMenu2", function () {
        SView.Windows.Button.highLightLabel("annotateMenu");
        $(".annotateTree").toggleClass("hidden");
        sviewResetAnnotateMenu();
      })
      .on("click", "canvas:not(#gesture_canvas)", function () {
        updateSelectedNode();
      })
      .on("click", "#notebar", function () {
        updateSelectedNode();
      })
      .on("click", ".annotate-tree-close", function () {
        SView.Windows.Button.highLightLabel("annotateMenu");
        $(".annotateTree").addClass("hidden");
      });

    bindDragEvent(".annotateTree");
  }

  /**
   * @description 设置弹窗拖动
   * @param 弹窗id id
   */
  function bindDragEvent(tag) {
    let setObj;
    let fixedX = 0;
    let fixedY = 0;
    let $content = $(tag);
    if ($content.length > 0) {
      setObj = $content.find(".dialog-title")[0];
    }
    if (!setObj) {
      return;
    }
    let obj = $content[0];
    let downFun = function (event) {
      event.preventDefault();
      setObj.setCapture && setObj.setCapture(); // IE8 及以下 强制捕获下一次单击事件
      fixedX = event.pageX - obj.offsetLeft;
      fixedY = event.pageY - obj.offsetTop;
      dragDiv(obj, event, fixedX, fixedY);
    };
    let upFun = function (event) {
      event.preventDefault();
      setObj.releaseCapture && setObj.releaseCapture(); // IE8 及以下 解除强制捕获单击事件
    };
    let touchStartFun = function (event) {
      event.preventDefault();
      setObj.setCapture && setObj.setCapture(); // IE8 及以下 强制捕获下一次单击事件
      fixedX = event.touches[0].pageX - obj.offsetLeft;
      fixedY = event.touches[0].pageY - obj.offsetTop;
      dragDiv(obj, event, fixedX, fixedY);
    };
    let touchEndFun = function (event) {
      event.preventDefault();
      setObj.releaseCapture && setObj.releaseCapture(); // IE8 及以下 解除强制捕获单击事件
    };
    //setObj.onmousedown = downFun;
    //setObj.onmouseup = upFun;
    setObj.addEventListener("mousedown", downFun, false);
    setObj.addEventListener("mouseup", upFun, false);
    setObj.addEventListener("touchstart", touchStartFun, false);
    setObj.addEventListener("touchend", touchEndFun, false);
  }

  function dragDiv(obj, e, fixedX, fixedY) {
    let _move = true;
    document.addEventListener(
      "mousemove",
      function (event) {
        if (_move) {
          let w =
            window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
          let h =
            window.innerHeight ||
            document.documentElement.clientHeight ||
            document.body.clientHeight;
          let x = event.clientX ? event.clientX : event.pageX;
          let y = event.clientY ? event.clientY : event.pageY;
          let left = x - fixedX;
          let top = y - fixedY;
          if (left >= w - obj.offsetWidth) {
            left = w - obj.offsetWidth - 2;
          }
          if (top >= h - obj.offsetHeight) {
            top = h - obj.offsetHeight - 2;
          }
          if (left < 0) {
            left = 0;
          }
          if (top < 0) {
            top = 0;
          }
          obj.style.left = left + "px"; // 元素在页面中的坐标 = 鼠标在页面中的坐标 - 元素在页面中的坐标
          obj.style.top = top + "px";
          obj.style.transform = "none";
        }
      },
      false
    );
    document.addEventListener(
      "mouseup",
      function () {
        _move = false;
      },
      false
    );
    document.addEventListener(
      "touchmove",
      function (event) {
        if (_move) {
          let touches = event.touches;
          let w =
            window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
          let h =
            window.innerHeight ||
            document.documentElement.clientHeight ||
            document.body.clientHeight;
          let x = touches[0].clientX ? touches[0].clientX : touches[0].pageX;
          let y = touches[0].clientY ? touches[0].clientY : touches[0].pageY;
          let left = x - fixedX;
          let top = y - fixedY;
          if (left >= w - obj.offsetWidth) {
            left = w - obj.offsetWidth - 2;
          }
          if (top >= h - obj.offsetHeight) {
            top = h - obj.offsetHeight - 2;
          }
          if (left < 0) {
            left = 0;
          }
          if (top < 0) {
            top = 0;
          }
          obj.style.left = left + "px"; // 元素在页面中的坐标 = 鼠标在页面中的坐标 - 元素在页面中的坐标
          obj.style.top = top + "px";
          obj.style.transform = "none";
        }
      },
      false
    );
    document.addEventListener(
      "touchend",
      function () {
        _move = false;
      },
      false
    );
  }

  // 保存批注
  function saveAnnotate(annotateData = "") {
    const { annotateUrl, AnnotateUrlRelink, currentCommentIndex = 0 } = params,
      _annotateUrl = annotateUrl[currentCommentIndex] ?? "";

    if (ctrlData) {
      const opt = ctrlData.initOpt,
        ctrl = opt.o;

      try {
        ctrl?.SaveAnnotate?.(annotateData, _annotateUrl);
      } catch (err) {
        _saveAnnotate(annotateData, _annotateUrl, AnnotateUrlRelink);
      }
    } else {
      _saveAnnotate(annotateData, _annotateUrl, AnnotateUrlRelink);
    }
  }

  /*  
  保存批注方法，如果平台相关逻辑调整可能会失效
  注：若出现问题优先与 base\ctrls\commonfileview\commonfileview.js 中的 SaveAnnotate 对比差异
*/
  function _saveAnnotate(content, annotateUrl, AnnotateUrlRelink) {
    if (!AnnotateUrlRelink) {
      return;
    }
    let itoken = annotateUrl.indexOf("?token=") + 7;
    let token = annotateUrl.substring(itoken);
    let downloadInfo = AnnotateUrlRelink[token];
    let path = downloadInfo.File.Path;
    let volumn = downloadInfo.Vault.Volumn;
    let isSSL = location.protocol.toLowerCase() === "https:";

    //改为从config传入serverHost
    // let serverHost = downloadInfo.Vault.IP + ":" + downloadInfo.Vault.Port;
    let serverHost = params.serverHost;

    if (typeof content != "string") {
      content = JSON.stringify(content);
    }

    let saveAnnotateWs = new HoteamUI.Common.WsConnect({
      action: "UPLOAD_COMMENT",
      type: "fs",
      webSocketURI: (isSSL ? "wss://" : "ws://") + serverHost,
      maxReconnectAttempts: -1,
      onconfirm: function (data) {
        //连通后收到确认消息，立即发送参数
        let toSend = {
          Volumn: volumn,
          Path: path,
          Replace: "true",
          Append: "true",
          OServerAddress: downloadInfo.Vault.IP,
          OServerPort: downloadInfo.Vault.Port,
          DataLength: content.length,
        };
        this.sendData(toSend);
      },
      onmessage: function (data) {
        let dataObj = null;
        try {
          dataObj = HoteamUI.Common.WsMessageUtils.parseMsgToObject(data);
        } catch (ex) {
          console.log(ex);
        }
        //当进行批量请求时，返回token以逗号分隔，此处只进行单个请求，故无需额外处理
        if (dataObj && dataObj["SEND"] == "CONTENT") {
          this.sendData({
            Data: content,
          });
        }
        saveAnnotateWs.ws && saveAnnotateWs.ws.close();

        sviewCreateTips(...tipsEnum.saveSuccess);
      },
      onsocketerror: function (error) {
        console.log(error);
        sviewCreateTips(...tipsEnum.saveError);
      },
    });
  }

  // 获取文件相关信息
  function getParams(query) {
    let parts = query.split("&"),
      params = Object.create(null);

    for (let i = 0, ii = parts.length; i < ii; ++i) {
      let param = parts[i].split("="),
        key = param[0].toLowerCase(),
        value;
      if (param.length > 2) {
        value = param.slice(1, param.length).join("=");
      } else {
        value = param.length > 1 ? param[1] : null;
      }
      params[decodeURIComponent(key)] = decodeURIComponent(value);
    }

    return params;
  }

  // 初始化Sview
  function sviewInit() {
    let { fileUrl: path, mark, annotateUrl } = params,
      {
        language,
        backgroundTopColor,
        backgroundBottomColor,
        rotateCenter,
        isSpecifyRotation,
        licencePath,
        aniToolFileUrl,
        isUseModelBackground,
      } = M3D.Config || {},
      instance = M3D.Config.Parameters.getInstance(),
      cookieHelper = M3D.Utility.CookieHelper;

    window.language =
      cookieHelper.getCookie(language) === "1"
        ? SView.UIManager.languageConstants.EN
        : SView.UIManager.languageConstants.CN;

    //初始化多语言
    initView();
    //初始化页面
    sview0 = window.sview0 = new Main("sview_1");

    if (!mark || !annotateUrl) {
      hideAnnotateMenu();
    }

    let uiManager = sview0.sviewFrame.getUIManager();
    uiManager.load(RightMenu);
    uiManager.load(RightClickMenu);
    uiManager.load(RightClickSelectMenu);
    uiManager.load(RightClickSelectMeasureMenu);
    uiManager.load(RightClickSelectAnnotateMenu);
    uiManager.load(RightClickSelectRingMenu);
    rightClickMenu(availableRightMenuClick);
    let configManager = sview0.sviewFrame.getConfigManager();

    let basePath = window.location.href.slice(0, window.location.href.lastIndexOf("/") + 1);

    // let cookie = document.cookie;

    // let cookieArray = cookie.split(";");
    // let isArray;
    // let isSvlx;
    // for (let i = 0; i < cookieArray.length; i++) {
    //   let keyValue = cookieArray[i].split("=");
    //   keyValue[0] = keyValue[0].replace(/\s*/g, "");
    //   if (keyValue[0] == "path") {
    //     path = keyValue[1];
    //   }
    //   if (keyValue[0] == "isArray") {
    //     isArray = keyValue[1];
    //   }
    //   if (keyValue[0] == "isSvlx") {
    //     isSvlx = keyValue[1];
    //   }
    // }

    configManager.setCommandConfigPath(basePath + `config/commandParameters.json`);
    instance.setParameter(licencePath, basePath + "lic");
    instance.setParameter(aniToolFileUrl, basePath);

    // 关闭指定旋转
    if (!cookieHelper.getCookie(isSpecifyRotation)) {
      instance.setBoolParameter(isSpecifyRotation, false);
    }

    // 关闭显示中心
    if (!cookieHelper.getCookie(rotateCenter)) {
      instance.setBoolParameter(rotateCenter, 0);
    }

    if (
      !cookieHelper.getCookie(backgroundTopColor) ||
      !cookieHelper.getCookie(backgroundBottomColor)
    ) {
      // 修改背景
      let bg = sview0.getViewer().getScene().getBackground(),
        topColor = new M3D.M3DMath.Color(),
        bottomColor = new M3D.M3DMath.Color();

      topColor.hexToRgb(bgTopColorStr);
      bottomColor.hexToRgb(bgBottomColorStr);

      cookieHelper.addCookie(backgroundTopColor, topColor.toString());
      cookieHelper.addCookie(backgroundBottomColor, bottomColor.toString());

      bg.setType(M3D.Shapes.BackgroundType.ColorBackgournd);
      bg.setUseColor(true);
      bg.setTopColor(topColor);
      bg.setBottomColor(bottomColor);
    }

    // 禁止使用模型自带背景
    if (!cookieHelper.getCookie(isUseModelBackground)) {
      cookieHelper.addCookie(isUseModelBackground, false);
      instance.setBoolParameter(isUseModelBackground, false);
    }

    configManager.loadCommandConfigs().then((ret) => {
      let commandsManager = sview0.sviewFrame.getCommandsManager();
      let openCommand = null;
      if (ret) {
        openCommand = commandsManager.getCommand(SView.Commands.CommandNames.OPENSAMPLEFILE);
      } else {
        //没有配置文件的情况
        let array = new Map();
        array.set("fullClassName", "SView.Commands.OpenCommand");
        openCommand = commandsManager.getCommand(SView.Commands.CommandNames.OPENSAMPLEFILE, array);
      }
      // if (isArray == "false") {
      openCommand.path = path;
      // }else{
      // 	let paths = path.split(",");
      // 	for(let i=0;i<paths.length;i++){
      // 		openCommand.addPath(paths[i]);
      // 	}
      // }
      commandsManager.execute(SView.Commands.CommandNames.OPENSAMPLEFILE);
    });
  }

  // 更改默认设置为外部修改后的
  function sviewRestoreSettings(instance, commands = {}) {
    let M3DInstance = M3D.Config.Parameters.getInstance(),
      { rotateCenter, isSpecifyRotation, backgroundTopColor, backgroundBottomColor } =
        M3D.Config || {},
      {
        SystemCommonBackgroundColorItem = {
          SettingItem: {
            subComponents: [],
          },
        },
        SystemCommonIsUseModelBackgroundItem,
        SystemCommonIsSpecifyRotation,
        isShowRotateCenter,
      } = commands,
      { id: bgContentId, subComponents } = SystemCommonBackgroundColorItem.SettingItem,
      modelBgElem = instance.uiManager.getElement(
        SystemCommonIsUseModelBackgroundItem?.SettingItem?.id
      ),
      rotateCenterElem = instance.uiManager.getElement(isShowRotateCenter?.SettingItem?.id),
      specifyRotationElem = instance.uiManager.getElement(
        SystemCommonIsSpecifyRotation?.SettingItem?.id
      );

    if (modelBgElem) {
      modelBgElem.setValue(false);
      instance.onChange(modelBgElem, true);
    }
    if (rotateCenterElem) {
      rotateCenterElem.setValue(false);
      instance.onChange(rotateCenterElem, true);
      M3DInstance.setBoolParameter(rotateCenter, 0);
    }
    if (specifyRotationElem) {
      specifyRotationElem.setValue(false);
      instance.onChange(rotateCenterElem, true);
      M3DInstance.setBoolParameter(isSpecifyRotation, false);
    }

    subComponents.forEach((item) => {
      let { Button } = item,
        highLight =
          Button[backgroundTopColor] == bgTopColorStr &&
          Button[backgroundBottomColor] == bgBottomColorStr,
        btn = instance.uiManager.getElement(Button.id);

      if (btn) {
        btn.setHighLight(highLight);
      }
    });
  }

  // 处理更新批注后显示问题
  function sviewRefresh() {
    // sview0.sviewFrame.getViewer().getScene().refresh();
    sview0.getViewer().getScene().refresh();
  }

  function sviewCreateTips(str1, str2) {
    let manager = sview0.sviewFrame.getUIManager();
    let data = {
      Message: { classes: ["SView-message"], id: "HtSViewTips", content: "" },
    };

    data.Message.content = sviewGetText(str1, str2);
    manager.load(data);
    setTimeout(function () {
      manager.removeLabel(data.Message.id);
    }, 3000);
  }

  // 获取文本信息
  function sviewGetText(str1, str2) {
    return SView[SView.UIManager.languageInfo].languageObj[str1][str2];
  }

  // 创建批注
  function sviewCreateAnnotation(node) {
    const { noteType = "node", note, guid } = node;
    let annotation = sviewGetAnnotation(guid, noteType),
      shapes;

    if (annotation) {
      return annotation;
    }

    shapes = sviewGetShapes();

    if (noteType === "line") {
      annotation = M3D.Reader.PaintBrushFactory.threeGestureNoteFromJson(
        note,
        sview0.getViewer().getScene()
      );
    } else if (noteType === "node") {
      annotation = M3D.Reader.AnnotationFactory.fromJson(note, sview0.getViewer().getScene());
    }

    // 批注默认隐藏，需手动显示
    if (annotation) {
      // annotationId = annotation.getId();
      shapes.addShape(annotation, true);
      sview0.getViewer().getScene().getVisiables().add(annotation);
    }

    // sviewRefresh();

    return annotation;
  }

  // 删除批注
  function sviewRemoveAnnotation(node) {
    let { noteType, guid } = node,
      annotation = sviewGetAnnotation(guid, noteType),
      shapes;

    if (!annotation) {
      return;
    }

    shapes = sviewGetShapes();

    shapes.removeShape(annotation.getId(), true);

    // sviewRefresh();
  }

  // 获取 sAnnotate
  function sviewGetAnnotation(guid, noteType = "node") {
    let annotations, annotation;

    if (noteType == "line") {
      annotations = sviewGetAllGestureAnnotation();
    } else {
      annotations = sviewGetAllAnnotation();
    }

    for (let i = 0, len = annotations.length; i < len; i++) {
      const node = annotations[i],
        { guid: _guid } = node;

      if (guid === _guid) {
        annotation = node;
        break;
      }
    }

    return annotation;
  }

  // 获取批注对应 json
  function sviewGetAnnotationJson(annotation, noteType = "node") {
    let note = "";
    if (!annotation) {
      return note;
    }

    if (noteType == "node") {
      note = M3D.Reader.AnnotationFactory.toJson(annotation);
      // sview 转换json有问题，最后会加个 ','
      if (note.endsWith(",")) {
        note = note.substring(0, note.length - 1);
      }
    } else if (noteType == "line") {
      note = M3D.Reader.PaintBrushFactory.threeGestureNoteToJson(annotation);
      // // 手绘批注 visible 默认 false， 调整为false
      // note = JSON.stringify(JSON.parse(note), {
      //   visible: true,
      // });
    }

    return note;
  }

  // 获取所有批注
  function sviewGetAllAnnotation() {
    return sview0
      .getViewer()
      .getScene()
      .getShapes()
      .getShapes(M3D.Shapes.ShapeType.ANNOTATION_NOTE);
  }

  // 获取所有手绘批注
  function sviewGetAllGestureAnnotation() {
    return sview0
      .getViewer()
      .getScene()
      .getShapes()
      .getShapes(M3D.Shapes.ShapeType.THREED_GESTURE_NOTE);
  }

  // 根据guid选中批注
  function sviewSelectByGuid(guid, clear = true) {
    let node = nodeMap.get(guid) || {},
      { noteType } = node,
      annotation = sviewGetAnnotation(guid, noteType),
      selector = sviewGetSelector();

    clear && sviewClearSelected();

    if (!annotation) {
      // sviewRefresh();
      return;
    }

    selector.add(annotation);

    // sviewRefresh();
  }

  function sviewGetSelector() {
    return sview0.sviewFrame.getViewer().getScene().getSelector();
  }

  // 取消选中批注按钮
  function sviewResetAnnotateMenu() {
    let aMenu = window.AnnotateMenu?.BottomMenu || {},
      { subComponents } = aMenu,
      uIManage;

    if (!subComponents) {
      return;
    }

    uIManage = sview0.sviewFrame.getUIManager();

    for (let i = 0; i < subComponents.length; i++) {
      const { id } = subComponents[i].Button,
        element = uIManage.getElement(id) || { btnHighlight: false },
        // 选中按钮 btnHighlight 未更新，暂不用，使用样式判断
        { btnHighlight, eleLabel } = element;

      if (eleLabel?.style?.backgroundColor) {
        let commands = sview0.sviewFrame.getCommandsManager();
        SView.Windows.Button.highLightLabel(id);
        commands.close?.(commands.activeCommandQueue.front());
        break;
      }
    }
  }

  // 根据选中节点更新批注选中状态
  function sviewUpdateSelectByNode() {
    let nodes = ztree.getSelectedNodes(),
      clear = true;

    if (nodes.length == 0) {
      sviewClearSelected();
      return;
    }

    nodes.forEach((tNode) => {
      let { guid, isParent } = tNode;
      if (isParent) {
        ztree.cancelSelectedNode(tNode);
      }
      sviewSelectByGuid(guid, clear);
      clear && (clear = false);
    });
  }

  // 更新手绘批注
  function sviewUpdateGestureAnnotation(lineType) {
    let cups = sviewGetAllGestureAnnotation(),
      addCup,
      guid;

    for (let i = 0; i < cups.length; i++) {
      let cup = cups[i],
        node;

      ({ guid } = cup);
      node = nodeMap.get(guid);

      if (!node) {
        addCup = cup;
        break;
      }
    }

    if (!addCup) {
      return;
    }

    addAnnotateNode(addCup, "line", lineType);
  }

  function sviewGetShapes() {
    return sview0.getViewer().getScene().getShapes();
  }

  function sviewGetSelected() {
    return sview0.sviewFrame.getViewer().getScene().getSelector().getAll() || [];
  }

  function sviewClearSelected() {
    let selector = sviewGetSelector();
    selector.clear();
  }

  function sviewBindEvent() {
    //点击其他位置隐藏
    document.documentElement.onclick = function (e) {
      sview0.sviewFrame.getUIManager().hideLabel("treeRightClickMenu");
      //右键菜单隐藏
      let rm1;
      let rm2;
      let rm3;
      let rm4;
      let rm5;
      rm1 = document.getElementById(RightClickSelectMenu.RightClickMenu.id);
      rm2 = document.getElementById(RightClickMenu.RightClickMenu.id);
      rm3 = document.getElementById(RightClickSelectAnnotateMenu.RightClickMenu.id);
      rm4 = document.getElementById(RightClickSelectMeasureMenu.RightClickMenu.id);
      rm5 = document.getElementById(RightClickSelectRingMenu.RightClickMenu.id);
      if (rm1 && rm1.style) {
        rm1.style.display = "none";
      }
      if (rm2 && rm2.style) {
        rm2.style.display = "none";
      }
      if (rm3 && rm3.style) {
        rm3.style.display = "none";
      }
      if (rm4 && rm4.style) {
        rm4.style.display = "none";
      }
      if (rm5 && rm5.style) {
        rm5.style.display = "none";
      }
    };
  }
};
//右键菜单是否可用
let availableRightMenuClick = true;
/**
 * 修改右键菜单是否可用
 * @param {any} enable
 */
function changeRightMenuClickable(enable) {
  availableRightMenuClick = enable;
  rightClickMenu(availableRightMenuClick);
}

//初始化各个控件库
function initView() {
  initRightMenu();
  initRightClickMenu();
  window.sviewFrameDiv = document.getElementById("sview_1");
}
