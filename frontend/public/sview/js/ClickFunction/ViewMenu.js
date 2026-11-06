// 渲染模式选择
function selectRenderMode(className) {
    var renderMode = document.getElementsByClassName(className)[0];
    var modes = document.getElementsByClassName("renderMode")[0];
    var allmodes = modes.getElementsByTagName("li")
    for (var i = 0; i < allmodes.length; i++) {
        allmodes[i].style.backgroundColor = "";
    }
    btnHighLight(renderMode);
    if (className == "solid") {
        console.log("实体模式")
        /* ------------------ 实体模式 ------------------ */
    } else if (className == "solidWire") {
        console.log("实体线框模式")
        /* ------------------ 实体线框模式 ------------------ */
    } else if (className == "wire") {
        console.log("插图模式")
        /* ------------------ 插图模式 ------------------ */
    } else if (className == "transparent") {
        console.log("半透明模式")
        /* ------------------ 半透明模式 ------------------ */
    }
}

// 视图方向
function viewDirection(className) {
    if (className == "fromTop") {
        console.log("俯视图")
        /* ------------------ 俯视图 ------------------ */
    } else if (className == "fromAxis") {
        console.log("轴视图")
        /* ------------------ 轴视图 ------------------ */
    } else if (className == "fromLeft") {
        console.log("左视图")
        /* ------------------ 左视图 ------------------ */
    } else if (className == "fromFront") {
        console.log("正视图")
        /* ------------------ 正视图 ------------------ */
    } else if (className == "fromRight") {
        console.log("右视图")
        /* ------------------ 右视图 ------------------ */
    } else if (className == "fromBack") {
        console.log("后视图")
        /* ------------------ 后视图 ------------------ */
    } else if (className == "fromBottom") {
        console.log("仰视图")
        /* ------------------ 仰视图 ------------------ */
    } else if (className == "perspectiveView") {
        console.log("透视投影")
        /* ------------------ 透视投影 ------------------ */
    }
}

// 漫游模式
function selectWalkingMode(className) {
    this.event.stopPropagation();
    this.event.preventDefault();
    var walkingMode = document.getElementsByClassName(className)[0];
    btnHighLight(walkingMode);
    if (walkingMode.style.backgroundColor != "") {
        console.log("漫游模式");
        /* ------------------ 漫游模式 ------------------ */
    }
}

// 设置菜单
function showSettingMenu(className) {
    var button = document.getElementsByClassName(className)[0];
    btnHighLight(button);
    if (button.style.backgroundColor != "") {
        console.log("设置弹窗弹出");
        /* ------------------ 设置弹窗 ------------------ */
    }
}