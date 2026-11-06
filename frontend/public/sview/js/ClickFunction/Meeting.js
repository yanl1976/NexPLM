// 进入创建/加入/会议记录界面
function enterMeetingSubMenu(className) {
    var menu = document.getElementsByClassName(className)[0];
    var hiddenMenu = document.getElementsByClassName("SView-meeting-mainMenu")[0];
    menu.style.visibility = "visible";
    hiddenMenu.style.visibility = "hidden";
}

// 返回会议主界面
function returnMeetingMainMenu(className) {
    var subMenu = document.getElementsByClassName(className)[0];
    var mainMenu = document.getElementsByClassName("SView-meeting-mainMenu")[0];
    subMenu.style.visibility = "hidden";
    mainMenu.style.visibility = "visible";
}


// 输入框内容判断
function checkInputContent(className) {
    var menu = document.getElementsByClassName(className)[0];
    var inputMenu = menu.getElementsByTagName("input")[0];
    var content = inputMenu.value.replace(/^\s*|\s*$/g,"");
    var warning = menu.getElementsByTagName("p")[0];
    if (content.length == 0) {
        // warning.style.visibility = "visible";
        warning.style.display = "block";
    } else {
        // warning.style.visibility = "hidden";
        warning.style.display = "none";
    }
}