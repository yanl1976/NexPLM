var fileListJson = [];
var filePathList = [];
function fileChange(that) {
    var files = that.files;
    var filesJson = '[';
    for (var i = 0; i < files.length; i++) {
        let name = files[i].name;
        let path = files[i].webkitRelativePath;
        if (/\.svlx$/ig.test(name)) {
            filesJson += '{"name":"' + name + '","path":"' + path + '"},';
        }
    }
    filesJson = filesJson.slice(0, -1) + "]";
    createTable(filesJson);
}

function requestFloder(path){
    var xhr = new XMLHttpRequest();
    xhr.open('GET', path, false);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = () => {
        if (xhr.readyState === 4 && xhr.status === 200) {
            parseHTML(xhr.response,path);
            createTable(fileListJson);
        } else {

        }
    }
    try {

        xhr.send(null);
    } catch (error) {
        console.log(error.code + "\n" + error.message);
    }
}

function parseHTML(html,path){
    var htmlStrList = html.split("<body>");
    var bodyStr = htmlStrList[1].split("</body>")[0];
    
    var el = document.createElement("div");
    el.innerHTML = bodyStr;
    var tbody = el.getElementsByTagName("tbody")[0];
    var aList = tbody.getElementsByTagName("a");
    for(var i=0;i<aList.length;i++){
        var fileJSON = {};
        var a = aList[i];
        var name = a.parentNode.nextElementSibling.nextElementSibling.getAttribute("name") ;
        
        fileJSON["path"] =path+a.innerHTML;
        fileJSON["size"] = a.parentNode.nextElementSibling.nextElementSibling.innerHTML;
        fileJSON["time"] = a.parentNode.nextElementSibling.innerHTML;
        if(name>0){
            fileJSON["name"] = a.innerHTML;


            fileJSON["isFile"] = true;
            //获取最后一个.的位置
            var index= a.innerHTML.lastIndexOf(".");
            //获取后缀
            var ext = a.innerHTML.substr(index+1);
            if(ext==="svlx"){
                fileJSON["suffix"] = "svlx";
            }else if( ext==="zip"){
                fileJSON["suffix"] = "zip";
            }else{
                fileJSON["suffix"] = "null";
            }
        }else{
            fileJSON["name"] = a.innerHTML.substring(0,a.innerHTML.length-1);
           fileJSON["isFile"] = false;
        }




        fileListJson.push(fileJSON);
    }
}



function createTable(jsonStr) {
    var obj = jsonStr;
    var tableObject = document.getElementById('dllList');
    for (var i in obj) {
        if (obj[i].path == "undefined") {
            obj[i].path = obj[i].name
        }
        var fileHTML = "";
        if(obj[i].isFile){
            if(obj[i].suffix==="svlx"){
                fileHTML = '<tr><td><input type="checkbox" name="filename" value="'+obj[i].path+'"/></td>'
                    +'<td onclick="openFile('+i+')"><a href="javascript:void(0);" class="fileA"><div class="fileImg"></div><div class="fileName">'+obj[i].name+'</div></a></td>'
                    +'<td>'+obj[i].time+'</td>'+'<td>文件</td><td>'+obj[i].size+'</td>';
            }else if(obj[i].suffix==="zip"){
                fileHTML = '<tr><td><input type="checkbox" name="filename" value="'+obj[i].path+'"/></td>'
                    +'<td ><a href="javascript:void(0);" class="fileA"><div class="zipImg"></div><div class="fileName">'+obj[i].name+'</div></a></td>'
                    +'<td>'+obj[i].time+'</td>'+'<td>文件</td><td>'+obj[i].size+'</td>';
            }else{
                fileHTML = '<tr><td><input type="checkbox" name="filename" value="" onclick="return false;"  disabled="disabled"/></td>'
                    +'<td ><a href="javascript:void(0);" class="fileA"><div class="unknowImg"></div><div class="fileName">'+obj[i].name+'</div></a></td>'
                    +'<td>'+obj[i].time+'</td>'+'<td>文件</td><td>'+obj[i].size+'</td>';
            }
            
        }else{
            fileHTML = '<tr><td><input type="checkbox" name="filename" value="" onclick="return false;"  disabled="disabled"/></td>'
                    +'<td onclick="openFile('+i+')"><a href="javascript:void(0);" class="fileA"><div class="folderImg"></div><div class="fileName">'+obj[i].name+'</div></a></td>'
                    +'<td>'+obj[i].time+'</td>'+'<td>文件夹</td><td></td>';
        }
        $("#fileTable").append(fileHTML);
        // $("#fileList").append('<input id="rotateswitch" type="checkbox" value="' + obj[i].path + '" name="filename" />' + obj[i].path+'</br>');
    }
}

function openFile(index){
    var path = fileListJson[index].path;
    if(fileListJson[index].isFile){
        if(fileListJson[index].suffix==="svlx"){
            document.cookie = "isSvlx=" +  true;
        }else{
            document.cookie = "isSvlx=" +  false;
        }
        document.cookie = "path=" +  path;
        document.cookie = "isArray=" + false;
        window.open("./demo.html");
    }else{
        var pathA = '<span>&gt;<a href="javascript:void(0);" onclick="goToFloder('+filePathList.length+')">'+fileListJson[index].name+'</a></span>';
        $("#path").append(pathA);
        $("#fileTable").html("");
        filePathList.push(fileListJson[index]);
        fileListJson = [];
        requestFloder(path);
    }
    
}

function goToFloder(index){
    if(filePathList.length>index){
        filePathList.splice(index+1,filePathList.length-index);
    }
    var path = filePathList[index].path;
    $("#fileTable").html("");
    fileListJson = [];
    requestFloder(path);
    updatePath();
}

function toHome(){
    $("#fileTable").html("");
    var pathA = $("#path span");
    fileListJson = [];
    for(var i=2;i<pathA.length;i++){
        pathA[i].remove();
    }
    // var pathA = '<span>&gt;<a href="javascript:void(0);" onclick="toHome()">models</a></span>';
    //     $("#path").append(pathA);
        filePathList = [];
        filePathList.push({"path":url+"models/","name":"models"});
    requestFloder(url+"models/");
}

function updatePath(){
    var pathA = $("#path span");
    for(var i=2;i<pathA.length;i++){
        pathA[i].remove();
    }
    for(var i=1;i<filePathList.length;i++){
        var pathA = '<span>&gt;<a href="javascript:void(0);" onclick="goToFloder('+i+')">'+filePathList[i].name+'</a></span>';
        $("#path").append(pathA);
    }
}

function backPreLevel(){
    $("#fileTable").html("");
    fileListJson = [];
    if(filePathList.length>1){
        filePathList.splice(filePathList.length-1,1);
    }
    var path = filePathList[filePathList.length-1].path;
    requestFloder(path);
    updatePath();
}



function openFiles() {
    var name = [];
    var models = $('input[name="filename"]:checked');
    if(models.length>1){
        var hasZip = false;
        $('input[name="filename"]:checked').each(function () {
            //获取最后一个.的位置
            var index= $(this).val().lastIndexOf(".");
            //获取后缀
            var ext = $(this).val().substr(index+1);
            if(ext==="svlx"){
                name.push($(this).val());
            }else{
                hasZip = true;
            }
            
        });
        if(hasZip){
            return;
        }
    }else if(models.length<1){
        return;
    }else{
        //获取最后一个.的位置
        var index= models[0].value.lastIndexOf(".");
        //获取后缀
        var ext = models[0].value.substr(index+1);
        if(ext==="svlx"){
            document.cookie = "isSvlx=" +  true;
        }else{
            document.cookie = "isSvlx=" +  false;
        }
        name.push(models[0].value);
    }
    if (name.length==0) {
        return;
    }
    try {
        if (name.length == 1) {
            document.cookie = "path=" +  name[0];
            document.cookie = "isArray=" + false;
            
            window.open("./demo.html");
        } else if (name.length > 1) {
            var filePath = [];
            for (var i = 0; i < name.length; i++) {
                filePath.push( name[i]);
            }
			document.cookie = "fileId=" +  "";
            document.cookie = "path=" + filePath;
            document.cookie = "isArray=" + true;
            window.open("./demo.html");
        }
    } catch (e) {
        alert("无法正常打开，请检查原始模型是否正确或联系客服人员！");
        $("#jdzw").css("width", "100%");
        $("#jdz").html("<span style='color:red;'>无法正常打开，请检查原始模型是否正确或联系客服人员！</span>");
    }
}

