var baseUrl = "";
var ui = 'icon';
var test = false;
var option;
var isInstall = 'install';
var notNull = function () {
    return arguments[0] != undefined && arguments[0] != null
}
var getJsonLength = function (json) {
    if (!json) return 0;
    var length = 0;
    for (var i in json) {
        length++;
    }
    return length;
}


var addClass = function (elem, className) {
    if (!notNull(elem)) {
        return;
    }
    var oldClassName = elem.className;
    if (!oldClassName) {
        elem.className = className;
        return;
    }
    var olds = oldClassName.split(" ");
    for (var i in olds) {
        if (olds[i] == className) {
            return;
        }
    }
    elem.className += " " + className;
}

var removeClass = function (elem, className) {
    if (!notNull(elem)) {
        return;
    }
    var oldClassName = elem.className;
    if (!oldClassName) {
        return;
    }
    var newClassName = '';
    var olds = oldClassName.split(" ");
    for (var i in olds) {
        if (olds[i] == className) {
            continue;
        }
        newClassName += olds[i] + " ";
    }
    newClassName = newClassName.substring(0, newClassName.length - 1);
    elem.className = newClassName;
}

var isFunction = function (func) {
    return notNull(func) && typeof (func) == 'function';
}

var exist = function (array, target) {
    for (var i in array) {
        if (array[i] == target) {
            return true;
        }
    }
    return false;
}

//克隆一个对象，可选择深度，point设置为1则只克隆当前的，设置为2则多加一层子层，为-1则复制全部
var clone = function (src, point) {
    if (src == null) {
        return null;
    }
    if (point == 0) {
        return null;
    }
    var target = {};
    if (src instanceof Array) {
        target = [];
    }
    for (var i in src) {
        if (typeof (src[i]) == 'object') {
            target[i] = clone(src[i], point - 1);
        }
        // else if(i=="children"&&src[i] instanceof Array){
        // 	target[i]=JSON.stringify(src[i]);
        // }
        else {
            target[i] = src[i];
        }
    }

    return target;
}

var object;

function Tree(option1) {
    option = clone(option1, -1);
    this.data = option.data || [],
        this.visibleData = [],
        this.allVisibleData = [],
        this.isAsync = !notNull(option.data),
        this.mainNode = document.createElement("div"),
        this.parent = document.querySelector(option.elem),
        this.allId = [],
        this.point = 0,
        this.format = {
            name: 'name',
            childs: 'childs',
            unfold: 'unfold',
            type: 'type',
            title: 'title',
            id: "id",
            hide: 'hide'
        },
        this.allData = {},
        this.isDbClick,
        this.timeOut = 0;
    if (!notNull(option.loadAll)) {
        option.loadAll = true;
    }

    object = this;

    if (notNull(option.format) && typeof (option.format) == 'function') {
        option.format(this.format);
    }

    this.incidents = ['click', 'dblclick', 'mouseleave', 'mouseenter', 'contextmenu'];
    this.allIncident = this.incidents;
    this.mainNode.className = "tree-main";
    this.load();
}

Tree.prototype = {
    Icon: {
        dir_fold: {
            src: baseUrl + "images/" + ui + "/icon_asm.png",
            width: 16,
            height: 16
        },
        dir_unfold: {
            src: baseUrl + "images/" + ui + "/icon_asm_unload.png",
            width: 16,
            height: 16
        },
        option: {
            src: baseUrl + "images/" + ui + "/icon_part.png",
            width: 16,
            height: 16
        },
        option_unfold: {
            src: baseUrl + "images/" + ui + "/icon_part_unload.png",
            width: 16,
            height: 16
        },
        icon_fold: {
            src: baseUrl + "images/" + ui + "/open.png",
            width: 16,
            height: 16
        },
        icon_unfold: {
            src: baseUrl + "images/" + ui + "/close.png",
            width: 16,
            height: 16
        }
    },
    mainNode: null,
    div_Load: null,
    incidents: null,
    allIncident: null,
    allId: null,
    data: null,
    allData: null,
    format: null,
    parent: null,
    isDbClick: null,
    point: null,
    timeOut: null,
    tools: null,
    foldTools: null,
    isAsync: null,
    start: 0,
    end: 0,
    Node: function (elem, isParent) {
        this.elem = elem;
        this.data = object.getDataById(elem);
        this.isParent = isParent || this.data[object.format.type] || object.hasChild(elem);
        this.id = this.data[object.format.id];
        this.name = this.data[object.format.name];
        this.title = this.data[object.format.title] || this.name;
        var checked, parent, childs, next, prev;
        this.childs = function () {
            if (!notNull(childs)) {
                childs = [];
                var elems = this.elem.nextElementSibling.childNodes;
                for (var i in elems) {
                    var li = elems[i];
                    if (li instanceof HTMLLIElement) {
                        var elem = li.firstElementChild;
                        childs[childs.length] = new object.Node(elem.getAttribute('tree-id'));
                    }
                }
            }
            return childs;
        }
        this.checked = function () {
            if (!notNull(checked)) {
                checked = object.checked(this);
            }
            return checked;
        }
        this.parent = function () {
            if (notNull(parent)) {
                return parent;
            }
            if (this.elem.parentNode.parentNode.parentNode.className.indexOf('tree-main') == -1) {
                parent = new object.Node(this.elem.parentNode.parentNode.previousElementSibling.getAttribute('tree-id'), true);
                return parent;
            } else {
                return null;
            }
        }

        this.next = function () {
            if (!notNull(next)) {
                var nextLi = this.elem.parentNode.nextElementSibling;
                if (notNull(nextLi)) {
                    next = new object.Node(nextLi.firstElementChild.getAttribute('tree-id'));
                } else {
                    return null;
                }
            }
            return next;
        }

        this.prev = function () {
            if (!notNull(prev)) {
                var prevLi = this.elem.parentNode.previousElementSibling;
                if (notNull(prevLi)) {
                    prev = new object.Node(prevLi.firstElementChild.getAttribute('tree-id'));
                } else {
                    return null;
                }
            }
            return prev;
        }

        this.hasChild = function () {
            if (!this.isParent) {
                return false;
            }
            return object.hasChild(this.elem);
        }

        this.foldAll = function () {
            object.foldAll(this);
        }

        this.addEventListener = function (type, func) {
            object.addEventListener(this, type, func);
        }

        this.check = function () {
            if (option.checkbox) {
                object.checkClick(this);
            }
        }

        this.addNode = function (data) {
            object.addNode(this, data);
        }

        this.remove = function () {
            object.remove(this);
        }

        this.unFold = function () {
            object.nodeUnfold(this, false);
        }

        this.fold = function () {
            object.nodeFold(this, false);
        }

    },
    ajax: function (option) {
        var request = new XMLHttpRequest();
        request.open(option.method, option.url, true);
        request.setRequestHeader("Access-Control-Allow-Origin", "*");
        request.setRequestHeader("Access-Control-Allow-Credentials", true);
        try {
            request.send(option.data || "");
        } catch (e) {
            if (isFunction(option.error)) {
                option.error(e);
            }
        }
        request.onreadystatechange = function () {
            if (request.status == 200 && request.readyState == 4) {
                if (isFunction(option.callback)) {
                    if (option.responseType == 'json') {
                        if (notNull(request.responseText) && request.responseText[0] == '{' || request
                            .responseText[0] == '[') {
                            option.callback(JSON.parse(request.responseText));
                        } else {
                            option.callback(request.responseText);
                        }
                    } else {
                        option.callback(request.responseText);
                    }
                }
            } else {
                if (request.status != 200) {
                    if (isFunction(option.error)) {
                        option.error(request.status);
                    }
                }
            }
        }
    },
    load: function () {
        //this.loadInfo();
        if (!notNull(this.data) || this.data.length == 0 && notNull(option.async)) {
            var newAjax = option.async;
            newAjax.callback = function (data) {
                object.data = data;
                if (JSON.stringify(object.data).substr(0, 1) == '{') {
                    var array = [];
                    array[0] = object.data;
                    object.data = array;
                }
                object.init("init");
            }
            newAjax.error = function (e) {
                console.log(e);
                this.div_Load.firstElementChild.innerText = option.loadText || "加载失败！";
            }
            newAjax.responseType = 'json';
            this.ajax(newAjax);
        }
    },
    createElement: function (elem, data, unfold) {
        if (data && data.length > 0) {
            var ul = this.createUl();
            for (var i in data) {
                var node = data[i];
                if (isFunction(option.template)) {
                    option.template(node);
                }
                var li;
                if (!notNull(node[this.format.childs]) || node[this.format.childs].length == 0) {
                    li = this.createNode(node);
                    li.style.marginLeft = (node.level * 18 + 14) + "px";
                } else {
                    li = this.createFolder(node);
                    li.style.marginLeft = (node.level * 18) + "px";
                }
                if (node[this.format.unfold]) {
                    unfold = true;
                }
                if (node[this.format.hide]) {
                    addClass(li, 'tree-hide');
                }
                ul.appendChild(li);
            }
            elem.innerHTML = "";
            elem.appendChild(ul);
            //如果展开，即将前面的也展开
            if (unfold) {
                addClass(ul, 'tree-fold')
            }
        }
    },
    flattenTree: function () {
        let arr = [];
        var flatten = function (
            list,
            childKey = "children",
            level = 1,
            parent = null,
            defaultExpand = false
        ) {
            list.forEach(item => {
                item.level = level;
                if (item.expand === undefined) {
                    item.expand = defaultExpand;
                }
                if (item.visible === undefined) {
                    item.visible = true;
                }
                if (!parent.visible || !parent.expand) {
                    item.visible = false;
                }
                item.parent = parent;
                arr.push(item);
                let childNode = item[childKey];
                if (childNode && childNode.length > 0) {
                    flatten(
                        childNode,
                        childKey,
                        level + 1,
                        item,
                        defaultExpand
                    )
                }
            });
        };
        flatten(this.data, "children", 1, {
            level: 0,
            visible: true,
            expand: true,
            children: this.data
        });
        return arr;
    },
    updateVisibleData: function (scrollTop = 0) {
        this.start = Math.floor(scrollTop / 29);
        this.end = this.start + Math.ceil((document.getElementsByClassName("SView-tree")[0].offsetHeight) / 29);
        this.allData = this.flattenTree();
        const allVisibleData = (this.allData || []).filter(
            item => item.visible
        );
        this.allVisibleData = allVisibleData;
        this.visibleData = allVisibleData.slice(this.start, this.end);
        let offset = this.start * 29;
        document.getElementsByClassName("SView-tree-content")[0].style.transform = "translateY(" + offset + "px)";
        document.getElementsByClassName("SView-tree-scroll")[0].style.height = (allVisibleData.length * 29) + "px";
        this.createElement(this.mainNode, this.visibleData, true);
    },
    scrollFun: function () {
        let that = this;
        document.getElementById("SView-assemblyTree").onscroll = function (e) {
            const scrollTop = e.target.scrollTop
            that.updateVisibleData(scrollTop)
        }
    },
    init: function (arg) {
        if (this.isAsync && !notNull(arg) && arg != 'init') {
            return;
        }
        this.parent.appendChild(this.mainNode);
        this.updateVisibleData();
        this.scrollFun();
        //this.div_Load.parentNode.removeChild(this.div_Load);
        let unSelecked = (this.allData || []).filter(
            item => item.checkedInfo == 3
        );
        if (unSelecked.length > 0) {
            let lastParentId = "";
            for (var node of unSelecked) {
                if (node.parent && node.parent.id && lastParentId != node.parent.id) {
                    this.setTopSelected(node.parent.id);
                    lastParentId = node.parent.id;
                }
            }
        }
        var nodes = document.getElementsByClassName('tree-fold');
        if (nodes.length > 0) {
            for (var i in nodes) {
                this.loadUnFold(nodes[i]);
            }
        }

        if (notNull(option.loadCallBack) && typeof (option.loadCallBack) == 'function') {
            option.loadCallBack(object);
        }
        this.mainNode.style.minWidth = this.parent.scrollWidth + "px";
    },
    addNode: function (node, datas) {
        var flatten = function (list, type) {
            list.forEach(item => {
                if (node.id == item.id) {
                    item.children.push(datas);
                    item.children.forEach(node => {
                        node.visible = true;
                    })
                }
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
        let unSelecked = (this.allData || []).filter(
            item => item.checkedInfo == 3
        );
        if (unSelecked.length > 0) {
            for (var node of unSelecked) {
                if (node.parent && node.parent.id) {
                    this.setTopSelected(node.parent.id);
                    break
                }
            }
        }
        this.updateVisibleData(scrollTop);
    },
    hide: function () {
        var hides = document.querySelectorAll('.tree-show.tree-hide');
        for (var i in hides) {
            if (hides[i] instanceof HTMLElement) {
                removeClass(hides[i], 'tree-show');
            }
        }
    },
    show: function () {
        var hides = document.querySelectorAll('.tree-hide');
        for (var i in hides) {
            if (hides[i] instanceof HTMLElement) {
                addClass(hides[i], 'tree-show');
            }
        }
    },
    loadUnFold: function (node) {
        var elem = node.elem;
        if (elem instanceof HTMLElement) {
            if (elem.parentNode.className.indexOf('tree-main') == -1) {
                if (elem.className.indexOf('tree-ul') != -1) {
                    //addClass(elem, 'tree-fold');
                    this.nodeUnfold(elem.previousElementSibling.getAttribute('tree-id'), false);
                }
                this.loadUnFold(elem.parentNode);
            }
        }
    },
    remove: function (node) {
        if (!notNull(node)) {
            this.mainNode.removeChild(this.mainNode.firstElementChild);
        } else {
            if (node instanceof object.Node) {
                var parent = node.elem.parentNode;
                parent.parentNode.removeChild(parent);
            }
        }
    },
    reload: function (data) {
        //if (!notNull(newOption) || !notNull(newOption.elem)) {
        //    return;
        //}
        //if (notNull(newOption)) {
        //    option = newOption;
        //}
        //this.mainNode.removeChild(this.mainNode.firstElementChild);
        //this.allData = {};
        //this.allId = [];
        //this.point = 0;
        //this.data = option.data || [];
        //this.init();
        if (data)
            this.data = data;
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
    },
    getChecked: function () {

        if (!option.checkbox) {
            return [];
        }
        var spans = document.getElementsByClassName('tree-checkbox-background-all');
        var ids = new Set();
        if (option.loadAll) {
            ids = [];
        }
        if (spans.length > 0) {
            for (var i in spans) {
                var span = spans[i];
                if (span instanceof HTMLSpanElement) {
                    var div = span.parentNode.parentNode;
                    if (option.loadAll) {
                        ids[ids.length] = new object.Node(div.parentNode.parentNode.getAttribute('tree-id'));
                    } else {
                        ids.add(div.getAttribute('tree-check-id'));
                    }
                    var data = div.parentNode.parentNode.parentNode.getAttribute('after-data');
                    if (notNull(data) && !option.loadAll) {
                        data = JSON.parse(data);
                        this.getNotLoadId(data[this.format.childs], ids);
                    }
                }
            }
        }
        return ids;
    },
    getSelected: function () {
        var spans = document.getElementsByClassName('tree-node-name-active');
        var id;
        if (spans.length == 0) {
            id = null;
        }
        if (spans.length > 0) {
            id = this.getNodeById(spans[0].getAttribute('tree-id'));
        }
        return id;
    },
    checked: function (node) {
        if (option.checkbox) {
            var background = node.elem.firstElementChild.firstElementChild.className;
            if (background == 'tree-fold-icon') {
                background = node.elem.firstElementChild.firstElementChild.nextElementSibling.firstElementChild
                    .firstElementChild.className;
            }
            if (background.indexOf('tree-checkbox-background-all') != -1) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    },
    checkClick: function (elem, andParent, andChild) {
        var checkbox = elem;
        if (checkbox.className.indexOf("tree-fold-icon") != -1) {
            checkbox = checkbox.nextElementSibling;
        }
        this.checkboxSelect(checkbox);
    },
    createCheckBox: function (id, node) {
        var div = document.createElement("div");
        addClass(div, 'tree-checkbox');
        var border = document.createElement("span");
        addClass(border, 'tree-checkbox-border');
        var background = document.createElement('span');
        if (node.checkedInfo == 1) {
            addClass(background, 'tree-checkbox-background tree-checkbox-background-notall');
        }
        if (node.checkedInfo == 2) {
            addClass(background, 'tree-checkbox-background tree-checkbox-background-all');
        }
        if (node.checkedInfo == 3) {
            addClass(background, 'tree-checkbox-background');
        }
        border.appendChild(background);
        div.appendChild(border);
        div.setAttribute('tree-check-id', id);
        div.onclick = function () {
            object.checkboxClick(this);
            if (isFunction(option.checkClick)) {
                var ele = this.parentNode.parentNode;
                option.checkClick(event, object.getNodeById(ele.getAttribute('tree-id')));
            }
        }
        return div;
    },
    createSubject: function (node, iconOption) {
        var Newdiv = document.createElement("div");
        Newdiv.className = "tree-subject";
        var div = document.createElement("div");
        var nodeDiv = document.createElement("div");
        if (node.activeNode) {
            nodeDiv.className = "tree-node-name tree-node-name-active";
        } else {
            nodeDiv.className = "tree-node-name";
        }
        var span = document.createElement("span");
        var input = document.createElement("input");
        input.setAttribute("type", "text");
        var newNode = clone(node, 1);
        // delete newNode[this.format.childs];

        if (this.format.data == 'all') {
            div.setAttribute("tree-data", JSON.stringify(newNode));
        } else {
            //判断是否有data属性需要赋值
            if (notNull(node[this.format.data])) {
                Newdiv.setAttribute("tree-data", node[this.format.data]);
            }
        }
        var tree_id;
        //判断是否含有id属性
        if (notNull(node[this.format.id])) {
            node[this.format.id] = parseInt(node[this.format.id]);
            tree_id = node[this.format.id];
        }
        nodeDiv.setAttribute('tree-id', tree_id);
        Newdiv.setAttribute('tree-id', tree_id);
        if (option.checkbox) {
            var check = this.createCheckBox(tree_id, node);
            div.appendChild(check);
        }

        if (iconOption && getJsonLength(iconOption) > 1) {
            var img = this.createIcon(iconOption);
            nodeDiv.appendChild(img);
        }
        span.className = "tree-title";
        if (!notNull(node[this.format.name])) {
            node[this.format.name] = "节点";
        }
        span.innerHTML = node[this.format.name];
        input.value = node[this.format.name];
        var that = this;
        input.onblur = function () {
            var nodeItem = that.getSelected();
            let ele = document.querySelector('.tree-node-name-active[tree-id="' + nodeItem.id + '"]');
            var value = ele.querySelector("input").value;
            if (value != "") {
                ele.querySelector(".tree-title").innerHTML = value;
                ele.querySelector(".tree-title").style.display = "inline-block";
                ele.querySelector("input").style.display = "none";
            }
        }
        if (notNull(node[this.format.title])) {
            div.title = node[this.format.title];
        } else {
            div.title = node[this.format.name];
        }
        var suffix = 'gm_';
        for (var j in this.allIncident) {
            var type = this.allIncident[j];
            var arg = type;
            if (type == 'dblclick') {
                arg = 'dbclick';
            }
            if (notNull(option[suffix + arg]) && typeof (option[suffix + arg]) == 'function') {
                this.addEventListener(nodeDiv, type, option[suffix + arg]);
            }
        }
        this.addData(tree_id, newNode);
        nodeDiv.appendChild(span);
        nodeDiv.appendChild(input);
        div.appendChild(nodeDiv);
        Newdiv.appendChild(div);
        return Newdiv;
    },
    createUl: function () {
        var ul = document.createElement("ul");
        ul.className = "tree-ul";
        return ul;
    },
    createNode: function (node) {
        var li = document.createElement("li");
        li.className = "tree-li tree-node";
        var subject;
        if (node.install) {
            subject = this.createSubject(node, this.Icon.option);
        } else {
            subject = this.createSubject(node, this.Icon.option_unfold);
        }


        var suffix = 'node_';
        for (var i in this.incidents) {
            var type = this.incidents[i];
            var arg = type;
            if (type == 'dblclick') {
                arg = 'dbclick';
            }
            if (notNull(option[suffix + arg]) && typeof (option[suffix + arg]) == 'function') {
                this.addEventListener(subject, type, option[suffix + arg]);
            }
        }
        li.appendChild(subject);
        return li;
    },
    addEventListener: function (node, type, func) {
        var elem = node;
        if (node instanceof object.Node) {
            elem = node.elem;
        }

        function event() {
            if (isFunction(func)) {
                if (type == 'click') {
                    object.isDbClick = false;
                    setTimeout(function () {
                        if (object.isDbClick) {
                            return;
                        }
                        func(object.getNodeById(elem.getAttribute('tree-id')), object);
                    }, object.timeOut);
                    return;
                } else if (type == 'dbclick') {
                    object.isDbClick = true;
                }
                func(object.getNodeById(elem.getAttribute('tree-id')), object);
            }
        }

        if (document.addEventListener) {
            elem.addEventListener(type, event);
        } else if (document.attachEvent) {
            elem.attachEvent('on' + type, event);
        }
    },
    createFolder: function (node, isUnfold) {
        var li = document.createElement("li");
        li.className = "tree-li";
        let iconSrc;
        if (node.install) {
            iconSrc = this.Icon.dir_fold;
        } else {
            iconSrc = this.Icon.dir_unfold;
        }
        var subject;
        var icon;
        if (node.expand) {
            icon = this.createIcon(this.Icon.icon_fold);
            icon.setAttribute('fold', true);
            subject = this.createSubject(node, iconSrc);
            if (isUnfold) {
                subject.firstElementChild.src = iconSrc.src;
                icon.src = this.Icon.icon_unfold.src;
            }
        } else {
            icon = this.createIcon(this.Icon.icon_unfold);
            icon.setAttribute('fold', false);
            subject = this.createSubject(node, iconSrc);
        }
        addClass(icon, 'tree-fold-icon');
        removeClass(icon, 'tree-title-icon');

        function click(elem) {
            if (elem.getAttribute("fold") == 'true') {
                object.nodeFold(elem.parentNode.parentNode.getAttribute('tree-id'), true);
            } else {
                object.nodeUnfold(elem.parentNode.parentNode.getAttribute('tree-id'), true);
            }
        }

        if (document.addEventListener) {
            icon.addEventListener('click', function () {
                object.isDbClick = false;
                var elem = this;
                setTimeout(function () {
                    click(elem);
                }, object.timeOut);
            });
        } else if (subject.attachEvent) {
            icon.attachEvent('onclick', function () {
                object.isDbClick = false;
                var elem = this;
                setTimeout(function () {
                    click(elem);
                }, object.timeOut);
            });
        }
        subject.firstElementChild.insertBefore(icon, subject.firstElementChild.firstElementChild);

        function dbclick() {
            object.isDbClick = true;
            if (this.firstElementChild.getAttribute('fold') == 'true') {
                object.nodeFold(this.parentNode.getAttribute('tree-id'), true);
            } else {
                object.nodeUnfold(this.parentNode.getAttribute('tree-id'), true);
            }
            if (notNull(option.folder_dbclick) && typeof (option.folder_dbclick) == 'function') {
                //option.folder_dbclick(new object.Node(this.parentNode.getAttribute('tree-id')), object);
            }
        }

        if (document.addEventListener) {
            subject.firstElementChild.addEventListener('dblclick', dbclick);
        } else if (document.attachEvent) {
            subject.firstElementChild.attachEvent('ondblclick', dbclick);
        }
        var suffix = 'folder_';
        for (var i in this.incidents) {
            var type = this.incidents[i];
            if (type == 'dblclick') {
                continue;
            }
            if (notNull(option[suffix + type]) && typeof (option[suffix + type]) == 'function') {
                this.addEventListener(subject, type, option[suffix + type]);
            }
        }
        li.appendChild(subject);
        return li;
    },
    foldAll: function (node) {
        var elem = node;
        if (node instanceof object.Node) {
            elem = node.elem;
        }
        if (notNull(elem) && elem instanceof HTMLElement) {
            this.nodeUnfold(elem.getAttribute('tree-id'), false);
            var ul = elem.nextElementSibling;
            if (notNull(ul)) {
                var lis = ul.childNodes;
                for (var i in lis) {
                    var li = lis.item(i);
                    if (li instanceof HTMLElement) {
                        this.nodeUnfold(li.firstElementChild.getAttribute('tree-id'), false);
                        if (notNull(li.firstElementChild.nextElementSibling)) {
                            this.foldAll(li.firstElementChild);
                        }
                    }
                }
            }
        }
    },
    getNodeById: function (id, type = true) {
        if (type) {
            var elem = (this.allData || []).filter(
                item => item.id == id && item.visible
            );
        } else {
            var elem = (this.allData || []).filter(
                item => item.id == id
            );
        }
        if (elem.length > 0) {
            return elem[0];
        }
        return null;
    },
    getNodeByName: function (name) {
        var elem = (this.allData || []).filter(
            item => item.name.toLowerCase().indexOf(name.toLowerCase()) != -1
        );
        if (elem.length > 0) {
            return elem;
        }
        return null;
    },
    getNodeDataById: function (id) {
        let nodeData = this.visibleData.filter(
            item => item.id == id
        );
        return nodeData;
    },
    hasChild: function (elem) {
        if (elem instanceof HTMLElement) {
            return notNull(elem.nextElementSibling);
        } else if (elem instanceof object.Node) {
            return notNull(elem.elem.nextElementSibling);
        } else {
            return false;
        }
    },
    recursionVisible: function (id, status) {
        var flatten = function (list) {
            list.forEach(item => {
                if (id == item.id) {
                    item.expand = status;
                    item.children.forEach(node => {
                        node.visible = status;
                        if (node.children && !status) {
                            this.recursionVisible(node.children, status);
                        }
                    })
                }
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
    },
    nodeFold: function (elem, call, unfold) {
        let elemId = null;
        if (elem instanceof object.Node) {
            elemId = elem.id;
        } else {
            elemId = elem;
        }
        var flatten = function (list, type) {
            list.forEach(item => {
                if (elemId == item.id) {
                    item.expand = false;
                    item.children.forEach(node => {
                        node.visible = false;
                        if (node.children && node.children.length > 0) {
                            node.expand = false;
                            flatten(node.children, true);
                        }
                    })
                } else if (type) {
                    item.expand = false;
                    item.children.forEach(node => {
                        node.visible = false;
                        if (node.children && node.children.length > 0) {
                            node.expand = false;
                            flatten(node.children, true);
                        }
                    })
                }
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
    },
    nodeUnfold: function (elem, call) {
        let elemId = null;
        if (elem instanceof object.Node) {
            elemId = elem.id;
        } else {
            elemId = elem;
        }
        this.recursionVisible(elemId, true);
        if (call) {
            if (isFunction(option.fold)) {
                /* option.fold(new object.Node(elemId), object);*/
            }
        }
    },
    nodeAllUnfold: function (nodeIds) {
        if (nodeIds && nodeIds.length > 0) {
            for (let i = 0; i < nodeIds.length; i++) {
                this.nodeUnfold(nodeIds[i]);
            }
        }
    },
    addData: function (key, data) {

    },
    getDataById: function (id) {
        if (notNull(this.allData)) {
            return this.allData[id];
        }
        return null;
    },
    createIcon: function (iconOption) {
        var img = document.createElement("img");
        if (notNull(iconOption.width) && notNull(iconOption.height)) {
            img.width = iconOption.width;
            img.height = iconOption.height;
        } else {
            img.width = 16;
            img.height = 16;
        }
        if (iconOption.src) {
            img.src = iconOption.src;
        }
        img.className = "tree-title-icon";
        return img;
    },
    checkboxClick: function (checkbox) {
        this.checkboxSelect(checkbox);
    },
    checkboxSelect: function (elem, type = false) {
        let checked = false;
        let elemId = null;
        if (!type) {
            var background = elem.firstElementChild.firstElementChild.className;
            if (background.indexOf('tree-checkbox-background-all') != -1 ||
                background.indexOf('tree-checkbox-background-notall') != -1) {
                checked = false;
            } else {
                checked = true;
            }
            elemId = elem.getAttribute("tree-check-id");
        } else {
            checked = type == 2 ? true : false;
            elemId = elem;
        }
        var flatten = function (list, type) {
            let state = checked ? 2 : 3;
            list.forEach(item => {
                if (elemId == item.id) {
                    if (item.checkedInfo != state) {
                        if (document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]')) {
                            document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = checked ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background";
                        }
                        item.checkedInfo = state;
                        if (item.parent && item.parent.id) {
                            topSet(item.parent);
                        }
                    }
                    item.children.forEach(node => {
                        if (node.checkedInfo != state) {
                            node.checkedInfo = checked ? 2 : 3;
                            if (document.querySelector('.tree-checkbox[tree-check-id="' + node.id + '"]')) {
                                document.querySelector('.tree-checkbox[tree-check-id="' + node.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = checked ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background";
                            }
                        }
                        if (node.children && node.children.length > 0) {
                            flatten(node.children, true);
                        }
                    })
                } else if (type) {
                    if (item.checkedInfo != state) {
                        item.checkedInfo = checked ? 2 : 3;
                        if (document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]')) {
                            document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = checked ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background";
                        }
                    }
                    item.children.forEach(node => {
                        if (node.checkedInfo != state) {
                            node.checkedInfo = checked ? 2 : 3;
                            if (document.querySelector('.tree-checkbox[tree-check-id="' + node.id + '"]')) {
                                document.querySelector('.tree-checkbox[tree-check-id="' + node.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = checked ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background";
                            }
                        }
                        if (node.children && node.children.length > 0) {
                            flatten(node.children, true);
                        }
                    })
                }
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        var topSet = function (parent) {
            let checked = 0;
            let noAllChecked = 0;
            let unchecked = 0;
            let className = "";
            parent.children.forEach(node => {
                if (node.checkedInfo == 2) {
                    checked++;
                }
                if (node.checkedInfo == 1) {
                    noAllChecked++;
                }
                if (node.checkedInfo == 3) {
                    unchecked++;
                }
            })
            if (noAllChecked > 0) {
                parent.checkedInfo = 1;
                className = "tree-checkbox-background tree-checkbox-background-notall"
            } else {
                if (checked > 0 && unchecked > 0) {
                    parent.checkedInfo = 1;
                    className = "tree-checkbox-background tree-checkbox-background-notall"
                }
                if (checked == 0 && unchecked > 0) {
                    let model = sview0.sviewFrame.viewer.getScene().getShapes().getShape(parent.id);
                    if (model && model.isVisible()) {
                        parent.checkedInfo = 1;
                        className = "tree-checkbox-background tree-checkbox-background-notall"
                    } else {
                        parent.checkedInfo = 3;
                        className = "tree-checkbox-background"
                    }
                }
                if (checked > 0 && unchecked == 0) {
                    parent.checkedInfo = 2;
                    className = "tree-checkbox-background tree-checkbox-background-all"
                }
            }
            if (document.querySelector('.tree-checkbox[tree-check-id="' + parent.id + '"]')) {
                document.querySelector('.tree-checkbox[tree-check-id="' + parent.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = className;
            }
            if (parent.parent && parent.parent.id) {
                topSet(parent.parent);
            } else {
                return
            }
        };
        flatten(this.data);
        //let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        //this.updateVisibleData(scrollTop);
    },
    setChecked: function (models, val) {
        let elemId = null;
        let isUpdate = false;
        var flatten = function (list) {
            let state = val ? 2 : 3;
            list.forEach(item => {
                if (elemId == item.id) {
                    if (item.checkedInfo != state) {
                        isUpdate = true;
                        let flag = true;
                        if (item.children && item.children.length > 0) {
                            item.children.forEach(child => {
                                if (child.checkedInfo == 1 || child.checkedInfo == 2) {
                                    flag = false;
                                    return
                                }

                            })
                        }
                        if (state === 3 && !flag) {
                            item.checkedInfo = 1;
                            if (document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]')) {
                                document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = val ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background tree-checkbox-background-notall";
                            }
                        } else {
                            if (document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]')) {
                                document.querySelector('.tree-checkbox[tree-check-id="' + item.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = val ? "tree-checkbox-background tree-checkbox-background-all" : "tree-checkbox-background";
                            }
                            item.checkedInfo = state;
                        }
                        if (item.parent && item.parent.id) {
                            topSet(item.parent);
                        }
                    }
                    return;
                } else {
                    let childNode = item.children;
                    if (childNode && childNode.length > 0) {
                        flatten(childNode)
                    }
                }
            });
        }
        var topSet = function (parent) {
            let checked = 0;
            let noAllChecked = 0;
            let unchecked = 0;
            let className = "";
            parent.children.forEach(node => {
                if (node.checkedInfo == 2) {
                    checked++;
                }
                if (node.checkedInfo == 1) {
                    noAllChecked++;
                }
                if (node.checkedInfo == 3) {
                    unchecked++;
                }
            })
            if (noAllChecked > 0) {
                parent.checkedInfo = 1;
                className = "tree-checkbox-background tree-checkbox-background-notall"
            } else {
                let model = sview0.sviewFrame.viewer.getScene().getShapes().getShape(parent.id);
                if (checked > 0 && unchecked > 0) {
                    parent.checkedInfo = 1;
                    className = "tree-checkbox-background tree-checkbox-background-notall"
                }
                if (checked == 0 && unchecked > 0) {
                    if (model && model.isVisible()) {
                        parent.checkedInfo = 1;
                        className = "tree-checkbox-background tree-checkbox-background-notall"
                    } else {
                        parent.checkedInfo = 3;
                        className = "tree-checkbox-background"
                    }

                }
                if (checked > 0 && unchecked == 0) {
                    if ((model && model.isVisible()) || !model) {
                        parent.checkedInfo = 2;
                        className = "tree-checkbox-background tree-checkbox-background-all"
                    } else {
                        parent.checkedInfo = 1;
                        className = "tree-checkbox-background tree-checkbox-background-notall"
                    }
                }
            }
            if (document.querySelector('.tree-checkbox[tree-check-id="' + parent.id + '"]')) {
                document.querySelector('.tree-checkbox[tree-check-id="' + parent.id + '"]').querySelector(".tree-checkbox-border").querySelector("span").className = className;
            }
            if (parent.parent && parent.parent.id) {
                topSet(parent.parent);
            } else {
                return
            }
        };
        for (let i in models) {
            let shape = models[i];
            if (shape && shape instanceof M3D.Shapes.Model) {
                var node = tree.getNodeById(shape.getId(), false);
                if (node && (node.checkedInfo == 3 || node.checkedInfo == 1) && val) {
                    elemId = node.id;
                    flatten(this.data);
                }
                if (node && (node.checkedInfo == 2 || node.checkedInfo == 1) && !val) {
                    elemId = node.id;
                    flatten(this.data);
                }
            }
        }
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        //if (isUpdate)
        //    this.updateVisibleData(scrollTop);
    },
    setAllNodeUnSelect: function () {
        var flatten = function (list, type) {
            list.forEach(item => {
                item.activeNode = false;
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
    },
    //向上设置是否选中
    setTopSelected(parentId) {
        let data = this.data;
        var topSet = function (id, list) {
            let checked = 0;
            let noAllChecked = 0;
            let unchecked = 0;
            list.forEach(item => {
                if (id == item.id) {
                    item.children.forEach(node => {
                        if (node.checkedInfo == 2) {
                            checked++;
                        }
                        if (node.checkedInfo == 1) {
                            noAllChecked++;
                        }
                        if (node.checkedInfo == 3) {
                            unchecked++;
                        }
                    })
                    if (noAllChecked > 0) {
                        item.checkedInfo = 1;
                    } else {
                        if (checked > 0 && unchecked > 0)
                            item.checkedInfo = 1;
                        if (checked == 0 && unchecked > 0) {
                            let model = sview0.sviewFrame.viewer.getScene().getShapes().getShape(item.id);
                            if (model && model.isVisible()) {
                                item.checkedInfo = 1;
                            } else {
                                item.checkedInfo = 3;
                            }
                        }

                        if (checked > 0 && unchecked == 0)
                            item.checkedInfo = 2;
                    }
                    if (item.parent && item.parent.id) {
                        topSet(item.parent.id, data);
                    } else {
                        return
                    }
                } else {
                    let childNode = item.children;
                    if (childNode && childNode.length > 0) {
                        topSet(id, childNode)
                    }
                }
            });
        };
        topSet(parentId, data);
    },
    /**
     * 展开节点
     * @param {any} node
     */
    expandNode: function (node) {
        if (!node || !tree) {
            return;
        }

        if (node.isParent) {
            //装配,展开;
            node.unFold();
        }
        while (true) {
            let parent = node.parent();
            if (!parent) {
                break;
            }
            parent.unFold();
            node = parent;
        }
    },
    /**
     * 设置多个node节点选中状态
     * @param {any} node 要选中的节点
     * @param {any} selected 选中状态
     */
    setNodeMultipleSelect: function (node, selected) {
        for (let i = 0; i < node.length; i++) {
            this.setNodeSelect(node[i], selected, false);
        }
    },
    /**
     * 获取node的select状态
     * @param {any} node  节点
     */
    isNodeSelected: function (node) {
        if (!node || !node.id)
            return false;
        var elem = (this.allData || []).filter(
            item => item.id == node.id && item.activeNode
        );
        if (elem.length > 0) {
            return true;
        } else {
            return false;
        }
    },

    /**
     * 设置node选中状态
     * @param {any} elemId 要选中的节点id
     * @param {any} selected 选中状态
     * @param {any} clearPre 是否清掉上一个;
     */
    setNodeSelect: function (elemId, selected, clearPre) {
        var flatten = function (list, type) {
            list.forEach(item => {
                if (elemId == item.id) {
                    item.activeNode = selected;
                    if (clearPre) {
                        item.children.forEach(node => {
                            node.activeNode = false;
                            if (node.children && node.children.length > 0) {
                                flatten(node.children, clearPre);
                            }
                        })
                    }
                } else if (type) {
                    item.activeNode = false;
                    item.children.forEach(node => {
                        node.activeNode = false;
                        if (node.children && node.children.length > 0) {
                            flatten(node.children, clearPre);
                        }
                    })
                }
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode, clearPre);
                }
            });
        };
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
        let selectNode = (this.visibleData || []).filter(
            item => item.id == elemId
        );
        if (selectNode.length == 0) {
            let postion = 0;
            for (var i = 0; i < this.allVisibleData.length; i++) {
                if (this.allVisibleData[i].id == elemId) {
                    postion = i;
                    break;
                }
            }
            let scroll = postion * 29;
            document.getElementById("SView-assemblyTree").scrollTo(0, scroll);
        }
    },
    getNotLoadId: function (data, arr) {
        if (!notNull(data) || option.loadAll) {
            return;
        }
        if (typeof data == 'string') {
            data = JSON.parse(data);
        }
        if (JSON.stringify(data).substr(0, 1) == '{') {
            var array = [];
            array[0] = data;
            data = array;
        }
        for (var i in data) {
            var d = data[i];
            var childs = d[object.format.childs];
            arr.add(d[object.format.id]);
            if (notNull(childs) && childs.length > 0) {
                this.getNotLoadId(childs, arr);
            }
        }

    },
    hasChildArray: function (data) {
        return notNull(data[object.format.childs]) && data[object.format.childs].length > 0;
    },
    loadInfo: function () {
        this.div_Load = document.createElement('div');
        this.div_Load.className = 'tree-load';
        this.div_Load.style.width = object.parent.style.width;
        var p = document.createElement('p');
        p.style.width = object.parent.style.width;
        p.style.height = (object.parent.style.fontSize || 20) + "px";
        p.innerHTML = option.loadText || '正在加载数据中，请稍后...';
        this.div_Load.appendChild(p)
        object.parent.appendChild(this.div_Load);
    },
    setInstall: function (elemId, val) {
        let that = this;
        var flatten = function (list) {
            list.forEach(item => {
                if (elemId == item.id) {
                    item.install = val;
                    let childNode = item.children;
                    if (childNode && childNode.length > 0) {
                        that.setInstallFun(childNode, val);
                    }
                } else {
                    let childNode = item.children;
                    if (childNode && childNode.length > 0) {
                        flatten(childNode)
                    }
                }
            });
        }
        flatten(this.data);
        let scrollTop = document.getElementById("SView-assemblyTree").scrollTop;
        this.updateVisibleData(scrollTop);
    },
    setInstallFun: function (list, val) {
        var flatten = function (list) {
            list.forEach(item => {
                item.install = val;
                let childNode = item.children;
                if (childNode && childNode.length > 0) {
                    flatten(childNode)
                }
            });
        };
        flatten(list);
    },
}
