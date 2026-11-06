var Synergy;
(function (Synergy) {
    Synergy.SYNERGY_APPID = 1400377499;
    //export const SYNERGY_EXPIRETIME = 604800;
    //export const SYNERGY_SECRETKEY = "77ba29e9125dd4cd78892aa5f745be8adfa6d735500ce69914d2ee5c47028af0";
    Synergy.SYNERGY_COOKIE_EXPIRETIME = 30; //过期时间30天
    //用户个人资料，用于存储cookie的key
    Synergy.SYNERGY_MY_PROFILE_USERID = "timmyuseid"; //用户 id
    Synergy.SYNERGY_MY_PROFILE_NICK = "timmynickname"; //用户昵称
    Synergy.SYNERGY_MY_PROFILE_ROLE = "timmyrole"; //当前会议我的角色
    //export const SYNERGY_MY_PROFILE_AUDIO_AUTH = "timmyaudioauth";//当前会议我的音频权限
    //export const SYNERGY_MY_PROFILE_VIDEO_AUTH = "timmyvideoauth";//当前会议我的视频权限
    Synergy.SYNERGY_MY_PROFILE_VAG_AUTH = "timmyvagauth"; //当前会议我的音视频权限
    //当前会议，用于存储cookie的key
    Synergy.SYNERGY_CUR_GROUP_GROUPID = "timcurgroupid"; //当前会议id
    Synergy.SYNERGY_CUR_GROUP_NAME = "timcurgroupname"; //当前会议名称
    Synergy.SYNERGY_CUR_GROUP_OWNERID = "timcurgroupownerid"; //当前会议群主用户id
    Synergy.SYNERGY_CUR_GROUP_OWNERNICK = "timcurgroupownernick"; //当前会议群主昵称
    Synergy.SYNERGY_CUR_GROUP_ATTENDEES = "timcurgroupattendees"; //当前会议参会者列表字符串
    //权限相关
    Synergy.SYNERGY_MEMBER_ROLE_OWNER = "owner"; //群主，群创建者
    Synergy.SYNERGY_MEMBER_ROLE_ADMIN = "admin"; //管理员
    Synergy.SYNERGY_MEMBER_ROLE_MEMBER = "member"; //普通成员
    //命令相关
    Synergy.SYNERGY_COMMAND_APPLY_ROLE = "apply_permission_from_host"; //向主持人申请操作权限
    Synergy.SYNERGY_COMMAND_APPLY_VAG = "apply_video_and_audio_permission_from_host"; //向主持人申请音视频权限
    Synergy.SYNERGY_COMMAND_CLOSE_FORBIDDEN = "host_close_forbidden_mode"; //主持人关闭禁言
    Synergy.SYNERGY_COMMAND_OPEN_FORBIDDEN = "host_open_forbidden_mode"; //主持人开启禁言
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class Main {
        constructor() {
            this.serverurl = "";
            this.fileid = "";
            this.myprofile = null;
            this.timGroup = null;
            this.sviewFrame = null;
            this.myprofile = new Synergy.MyProfile();
            this.timGroup = new Synergy.TimGroup();
        }
        static instance() {
            if (Synergy.Main.synergymain == null) {
                Synergy.Main.synergymain = new Synergy.Main();
            }
            return Synergy.Main.synergymain;
        }
        getMyProfile() {
            return this.myprofile;
        }
        getTimGroup() {
            return this.timGroup;
        }
        getSviewFrame() {
            return this.sviewFrame;
        }
        /**
         * 会议模块初始化
         * @param serverurl
         */
        init(serverurl, fileid, sviewFrame) {
            this.serverurl = serverurl;
            this.fileid = fileid;
            this.sviewFrame = sviewFrame;
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let meetingDialog = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
                if (meetingDialog == null || !meetingDialog.isShow()) {
                    Synergy.AlertUtill.showLoadingDialog();
                    let timuserid = that.myprofile.getUserID();
                    let promise = Synergy.SynergyTim.instance().timLogin(that.serverurl, timuserid);
                    promise.then(function (result) {
                        let retdata = eval(result);
                        if (retdata.code == '0') {
                            let userid = retdata.userid;
                            that.myprofile.setUserID(userid);
                            //临时加的，用于测试
                            //that.myprofile.setVagAuth(true);
                            //打开会议面板
                            Synergy.UiManager.openMeetingDialog();
                            Synergy.AlertUtill.hideLoadingDialog();
                            resolve('');
                        }
                        else {
                            Synergy.AlertUtill.hideLoadingDialog();
                            Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimAuthorizationFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                            reject(result);
                        }
                    }).catch(function (result) {
                        let retdata = eval(result);
                        Synergy.AlertUtill.hideLoadingDialog();
                        Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimAuthorizationFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                        reject(result);
                        //Synergy.AlertUtill.hideLoadingDialog();
                    });
                }
                else {
                    //关闭会议面板
                    Synergy.UiManager.closeMeetingDialog();
                    meetingDialog = null;
                    resolve('');
                }
            });
            return p;
        }
        /**
         * 打开或关闭聊天界面
         * */
        openOrCloseChatDialog() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let chatDialog = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.ChatDialog.Dialog.id);
                if (!chatDialog.isShow()) {
                    Synergy.UiManager.openChatDialog();
                    resolve('');
                }
                else {
                    //关闭聊天面板
                    Synergy.UiManager.closeChatDialog();
                    chatDialog = null;
                    reject('');
                }
            });
            return p;
        }
        /**
         * 打开或关闭会议控制主界面
         * */
        openOrCloseMeetingControlDialog() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let meetingControlDialog = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlDialog.Dialog.id);
                if (!meetingControlDialog.isShow()) {
                    Synergy.UiManager.openMeetingControlDialog();
                    resolve('');
                }
                else {
                    //关闭会议控制主界面
                    Synergy.UiManager.closeMeetingControlDialog();
                    meetingControlDialog = null;
                    reject('');
                }
            });
            return p;
        }
        /**
         * 创建会议
         * */
        createGroup() {
            let timUserId = this.myprofile.getUserID();
            //let timNickName = this.myprofile.getNickName();
            //let theme = timNickName + "的会议";
            //let nickname = timNickName;
            let theme = this.getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingThemeInput.Input.id).getValue();
            let nickname = this.getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingNicknameInput.Input.id).getValue();
            if (timUserId == '' || timUserId.length == 0) {
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimLoginFailure, Synergy.Dialog.AlertDialogType.WARNING);
                return false;
            }
            if (theme == '' || theme.length == 0 ||
                nickname == '' || nickname.length == 0) {
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.NoParameterError, Synergy.Dialog.AlertDialogType.WARNING);
                return false;
            }
            if (theme.length > 50) {
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.SizeOverrun, Synergy.Dialog.AlertDialogType.WARNING);
                return false;
            }
            if (nickname.length > 50) {
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.SizeOverrun, Synergy.Dialog.AlertDialogType.WARNING);
                return false;
            }
            let notification = ""; //群公告
            let that = this;
            if (this.fileid != "" || this.fileid.length > 0) {
                let fileurl = this.serverurl + "/8.0/file/downloadsource?fileid=" + this.fileid;
                notification = Synergy.SynergyMessages.fileOpenOrCloseFile("Open", fileurl, this.fileid, "打开模型");
            }
            Synergy.AlertUtill.showLoadingDialog();
            this.updateMyNick(nickname).then(function (data) {
                Synergy.SynergyTim.instance().createGroup(that.serverurl, theme, timUserId, nickname, notification)
                    .then(function (resultdata) {
                    let retdata = eval(resultdata);
                    if (retdata.code == '0') {
                        let curGroupId = retdata.groupid;
                        that.timGroup.setGroupID(curGroupId, true);
                        that.timGroup.setOwnerID(timUserId);
                        that.timGroup.setOwnerNick(nickname);
                        //console.log("--------------创建会议成功-----------------");
                        Synergy.UiManager.initGroupDialogs(false, false);
                        //hideGroupLoader();
                        Synergy.AlertUtill.hideLoadingDialog();
                    }
                    else {
                        //swal(getLocalizedString(language, "SweetAlert", "TimCreateGroupFailure"), result.msg, "error", getLocalizedString(language, "SweetAlert", "Confirm"), getLocalizedString(language, "SweetAlert", "Cancel"));
                        //hideGroupLoader();
                        Synergy.AlertUtill.hideLoadingDialog();
                        Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimCreateGroupFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                    }
                });
            });
        }
        /**
         * 解散群组
         * */
        dismissGroup() {
            Synergy.AlertUtill.confirm(SView[SView.UIManager.languageInfo].languageObj.Prompt.Prompt, SView[SView.UIManager.languageInfo].languageObj.Prompt.QuitGroup, "Synergy.Main.instance().dismissGroupConfirm()");
        }
        dismissGroupConfirm() {
            let ownerID = this.timGroup.getOwnerID();
            let timUserId = this.myprofile.getUserID();
            if (Number(ownerID) == Number(timUserId)) {
                //执行解散会议
                Synergy.SynergyTim.instance().dismissGroup(this.timGroup.getGroupID()).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("Synergy.Main解散会议-----");
                        //initGroup(true, false);
                        Synergy.UiManager.initGroupDialogs(true, false);
                        Synergy.AlertUtill.toast(Synergy.Main.instance().getMyProfile().getNickName() + ":" +
                            SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ExitGroup);
                    }
                    else {
                        Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimDismissGroupFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    //console.log("Synergy.Main解散会议失败-----" + retdata);
                    Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimDismissGroupFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                });
            }
            else {
                //执行退出会议
                Synergy.SynergyTim.instance().quitGroup(this.timGroup.getGroupID()).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("Synergy.Main退出会议成功-----");
                        //initGroup(true, false);
                        Synergy.UiManager.initGroupDialogs(true, false);
                        Synergy.AlertUtill.toast(Synergy.Main.instance().getMyProfile().getNickName() + ":" +
                            SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ExitGroup);
                    }
                    else {
                        //console.error("Synergy.Main退出会议失败-----" + retdata);
                        Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimDismissGroupFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    //console.error("Synergy.Main退出会议失败-----" + retdata);
                    Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimDismissGroupFailure, retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                });
            }
        }
        /**
         * 更新我的昵称，类似微信名
         * @param nick
         */
        updateMyNick(nick) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                if (nick == '' || nick.length == 0) {
                    nick = that.myprofile.getNickName();
                }
                Synergy.SynergyTim.instance().updateMyProfile(nick).then(function (data) {
                    that.myprofile.setNickName(nick);
                    resolve(data);
                }).catch(function (imError) {
                    reject(imError);
                });
            });
            return p;
        }
        /**
         * 加入会议
         * */
        joinGroup() {
            let that = this;
            let selTypeID = this.getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingTabs.Tabs.id).getActiveTab();
            //let selTypeID = Synergy.Dialog.JoinMeetingTabsMeetingList.dataClass;
            let groupID = "";
            let nickName = that.myprofile.getNickName();
            if (selTypeID == Synergy.Dialog.JoinMeetingTabsMeetingList.dataClass) {
                //会议列表中点击加入会议
                let selectGroup = that.getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingTabsMeetingListTable.Table.id).getActiveTr();
                if (selectGroup == null) {
                    //console.error("--------请选择需要加入的会议---------");
                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimSelectGroupToJoin, Synergy.Dialog.AlertDialogType.INFO);
                    return false;
                }
                groupID = selectGroup.childNodes[1].innerText;
            }
            else {
                //会议号中点击加入会议
                let groupid_text = that.getSviewFrame().getUIManager().getElement(Synergy.Dialog
                    .JoinMeetingTabsMeetingIdFormMeetingIdInput.Input.id).getValue();
                let nickname_text = that.getSviewFrame().getUIManager().getElement(Synergy.Dialog
                    .JoinMeetingTabsMeetingIdFormNickNameInput.Input.id).getValue();
                groupID = groupid_text;
                nickName = nickname_text;
            }
            if (groupID == '' || groupID.length == 0) {
                //console.error("--------会议id不能为空---------");
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimNoGroupIDError, Synergy.Dialog.AlertDialogType.INFO);
                return false;
            }
            else if (nickName == '' || nickName.length == 0) {
                //console.error("--------会议昵称不能为空---------");
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimNoNickError, Synergy.Dialog.AlertDialogType.INFO);
                return false;
            }
            Synergy.AlertUtill.showLoadingDialog();
            that.updateMyNick(nickName).then(function (data) {
                Synergy.SynergyTim.instance().joinGroup(groupID, "")
                    .then(function (resultdata) {
                    let retdata = eval(resultdata);
                    if (retdata.code == '0') {
                        that.timGroup.setGroupID(groupID, true);
                        if (retdata.isingroup) {
                            //之前已经在群中
                            //initGroup(false, true);
                            //console.log("--------------加入会议成功：之前已经在群组中-----------------");
                            Synergy.UiManager.initGroupDialogs(false, true);
                        }
                        else {
                            //initGroup(false, false);
                            //console.log("--------------加入会议成功：之前未在群组中-----------------");
                            Synergy.UiManager.initGroupDialogs(false, false);
                        }
                        Synergy.AlertUtill.hideLoadingDialog();
                    }
                    else {
                        //console.log("--------------加入会议失败-----------------");
                        Synergy.AlertUtill.hideLoadingDialog();
                        Synergy.AlertUtill.alert(SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimJoinGroupFailure, SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.MeetingWasUnexist, Synergy.Dialog.AlertDialogType.ERROR);
                    }
                });
            });
        }
        /**
         * 初始化加会时的会议列表
         * */
        initGroupList() {
            let that = this;
            Synergy.AlertUtill.showLoadingDialog();
            Synergy.SynergyTim.instance().listGroup(that.serverurl, 0, 100, "").then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    let groups = eval(retdata.groups);
                    for (let i = 0; i < groups.length; i++) {
                        let groupitem = groups[i];
                        let itemarray = [[groupitem.name, groupitem.id, groupitem.host, groupitem.createtime]];
                        Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingTabsMeetingListTable.Table.id)
                            .addRow(itemarray, i + 1);
                    }
                    Synergy.AlertUtill.hideLoadingDialog();
                    //console.log("----------会议列表初始化成功------------");
                }
                else {
                    //console.error("Synergy.Main初始化会议列表失败-----" + retdata);
                    Synergy.AlertUtill.hideLoadingDialog();
                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.TimListGroupFailure, Synergy.Dialog.AlertDialogType.ERROR);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                //console.error("Synergy.Main初始化会议列表失败-----" + retdata);
                Synergy.AlertUtill.hideLoadingDialog();
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.TimListGroupFailure, Synergy.Dialog.AlertDialogType.ERROR);
            });
        }
        /**
         * 保存会议设置内容
         * */
        saveGroupSetting() {
            let timNickName = this.getSviewFrame().getUIManager().getElement(Synergy.Dialog
                .SettingMeetingDialogFormNicknameInput.Input.id).getValue();
            if (!timNickName) {
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimNoNickError, Synergy.Dialog.AlertDialogType.INFO);
                //console.error("-----------------------昵称为空，保存设置失败-------------------");
                return;
            }
            this.updateMyNick(timNickName).then(function (data) {
                Synergy.UiManager.closeSettingMeetingDialog();
                //console.log("-----------------------保存设置成功-------------------");
            }).catch(function (error) {
                //console.error("-----------------------更新群昵称失败，保存设置失败-------------------");
            });
        }
        /**
         * 发送文本聊天内容
         * */
        sendChatMessage() {
            let that = this;
            //let chatcontent = document.getElementById("chat_msg_input").getAttribute("value");
            let chatcontent = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.ChatDialogInput.Input.id).getValue();
            if (chatcontent == '' || chatcontent.trim().length == 0) {
                console.error("Synergy.Main聊天内容不得为空-----"); //todo
                return;
            }
            let message = Synergy.SynergyTim.instance().createTextMessage(that.timGroup.getGroupID(), chatcontent, TIM.TYPES.CONV_GROUP);
            Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    //添加文本聊天框聊天记录
                    if (chatcontent.indexOf("\r\n") > 0) {
                        chatcontent = chatcontent.replace(/\r\n/g, "<br/>");
                    }
                    if (chatcontent.indexOf("\n") > 0) {
                        chatcontent = chatcontent.replace(/\n/g, "<br/>");
                    }
                    let sendtime = Synergy.Utill.formatDate(message.time);
                    let rightChatItem = JSON.parse(JSON.stringify(Synergy.Dialog.RightChatItem));
                    //let rightChatItem = Synergy.Dialog.RightChatItem;
                    rightChatItem.Item.text = chatcontent;
                    rightChatItem.Item.time = sendtime;
                    //let RightChatItem = {
                    //    Item: {
                    //        itemType: "rightChatItem",
                    //        name: "",
                    //        text: chatcontent,
                    //        imgsrc: "images/meeting/sview_conference_chart_me.png",
                    //        time: sendtime
                    //    }
                    //}
                    that.sviewFrame.getUIManager().getElement(Synergy.Dialog.ChatList.List.id).addSubComponents([rightChatItem]);
                    that.sviewFrame.getUIManager().getElement(Synergy.Dialog.ChatDialogInput.Input.id).setValue("");
                    /**
                     * 滚动到聊天框底端
                     * **/
                    let domWrapper = document.getElementById(Synergy.Dialog.ChatList.List.id);
                    const currentScroll = domWrapper.scrollTop; // 已经被卷掉的高度
                    const clientHeight = domWrapper.offsetHeight; // 容器高度
                    const scrollHeight = domWrapper.scrollHeight; // 内容总高度
                    if (scrollHeight - 10 > currentScroll + clientHeight) {
                        domWrapper.scrollTo(0, currentScroll + (scrollHeight - currentScroll - clientHeight));
                    }
                    //console.log("----------发送文本聊天内容成功------" + chatcontent);
                }
                else if (retdata.code == '80002') {
                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.MsgSizeOverrun, Synergy.Dialog.AlertDialogType.ERROR);
                    //console.error("Synergy.Main发送文本聊天内容失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                //console.error("Synergy.Main发送文本聊天内容失败-----" + retdata);
            });
        }
        /***
        * 点击上传
        * */
        addFile() {
            document.getElementById(Synergy.Dialog.FileInput.Input.id).click();
        }
        /***
         * 发送会议附件（图片）
         * */
        sendAttachment() {
            let that = this;
            let groupid = that.timGroup.getGroupID();
            let message = Synergy.SynergyTim.instance().createImageMessage(groupid, document.getElementById(Synergy.Dialog.FileInput.Input.id));
            Synergy.AlertUtill.showLoadingDialog();
            Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    //添加附件发送记录
                    let meetingControlAttachmentItem = Synergy.Dialog.MeetingControlAttachmentItem;
                    //let sendusernick = message.nick;
                    let sendtime = message.time;
                    //let fromUserId = message.from;
                    let imgurl = message.payload.imageInfoArray[0].url;
                    meetingControlAttachmentItem.Item.id = message.ID;
                    meetingControlAttachmentItem.Item.name = Synergy.Dialog.AttachmentItem.myName;
                    meetingControlAttachmentItem.Item.time = Synergy.Utill.formatDate(sendtime);
                    meetingControlAttachmentItem.Item.imgsrc = imgurl;
                    meetingControlAttachmentItem.Item.onClick = "Synergy.Main.instance().showImgDialog('" + message.payload.imageInfoArray[1].url + "')";
                    that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlAttachmentList.List.id).addSubComponents([meetingControlAttachmentItem]);
                    that.sviewFrame.getUIManager().getElement(Synergy.Dialog.FileInput.Input.id).setValue("");
                    Synergy.AlertUtill.hideLoadingDialog();
                    /**
                     * 滚动到附件框底端
                     * **/
                    let domWrapper = document.getElementById(Synergy.Dialog.MeetingControlAttachmentList.List.id).parentNode;
                    const currentScroll = domWrapper.scrollTop; // 已经被卷掉的高度
                    const clientHeight = domWrapper.offsetHeight; // 容器高度
                    const scrollHeight = domWrapper.scrollHeight; // 内容总高度
                    if (scrollHeight - 10 > currentScroll + clientHeight) {
                        domWrapper.scrollTo(0, currentScroll + (scrollHeight - currentScroll - clientHeight));
                    }
                    //console.log("----------发送会议附件（图片）成功------");
                }
                else {
                    Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                    //console.error("Synergy.Main发送会议附件（图片）失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                //console.error("Synergy.Main发送会议附件（图片）失败-----" + retdata);
            });
        }
        /**
         * 点击查看大图
         *
         */
        showImgDialog(url) {
            event.stopPropagation(); //阻止冒泡
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingControlDialog.Dialog.id);
            Synergy.Dialog.PreviewAttachmentDialog.Dialog.contentImg = url;
            Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.PreviewAttachmentDialog);
            let dialog = document.getElementById(Synergy.Dialog.PreviewAttachmentDialog.Dialog.id);
            let hideFun = function () {
                Synergy.Main.instance().closePreviewAttachmentDialog();
                document.body.removeEventListener("click", hideFun);
            };
            document.body.addEventListener('click', hideFun);
            dialog.addEventListener('click', function (event) {
                event.stopPropagation(); //阻止冒泡
            });
        }
        /**
         * 关闭大图
         *
         */
        closePreviewAttachmentDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.PreviewAttachmentDialog.Dialog.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlDialog.Dialog.id);
        }
        /**
         * 发送协同指令
         * @param text 定义的协同指令
         */
        sendSynergyCommand(text) {
            let that = this;
            let message = Synergy.SynergyTim.instance().createTextMessage(that.timGroup.getGroupID(), text, TIM.TYPES.CONV_GROUP);
            Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    console.log("----------发送协同指令成功------" + text);
                }
                else {
                    console.error("Synergy.Main发送协同指令失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                console.error("Synergy.Main发送协同指令失败-----" + retdata);
            });
        }
        /*******************************参会人tab相关*********************************/
        /**
         *  全员禁言(禁止音视频)
         * */
        forbidAllMembersVideoAndAudio() {
            let that = this;
            //let group_members = [];
            let group_members = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).allSubComponents;
            let myuserid = this.getMyProfile().getUserID();
            let myrole = this.getMyProfile().getRole();
            for (let value of group_members.values()) {
                let memberid = value.eleLabel.getAttribute("data-userId");
                //禁言
                that.forbidMemberVideoAndAudio(memberid);
                //if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //    //自己不用发送通知
                //} else {
                //    //禁言
                //    that.forbidMemberVideoAndAudio(memberid);
                //}
            }
            /**
             * 更新按钮显隐，隐藏全员禁言按钮，显示取消禁言按钮
             * **/
            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.MeetingControlMuteAllMembersBtn.Button.id);
            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.MeetingControlUnmuteAllMembersBtn.Button.id);
        }
        /**
         * 禁言
         * */
        forbidMemberVideoAndAudio(memberid) {
            let that = this;
            let myuserid = that.getMyProfile().getUserID();
            let myrole = that.getMyProfile().getRole();
            if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //如果群主禁止自己，则直接return
                return;
            }
            else {
                let message = Synergy.SynergyTim.instance().createTextMessage(memberid, Synergy.SYNERGY_COMMAND_OPEN_FORBIDDEN, TIM.TYPES.CONV_C2C);
                Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("----------发送禁言指令成功------" + memberid);
                        let attendee = that.timGroup.getAttendee(memberid);
                        attendee.vagauth = false;
                        that.timGroup.saveAttendee(attendee);
                        /**
                         * 更新参会者及申请列表里的记录状态
                         * **/
                        //显示申请者开启音视频权限按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyItem.applyItemUnMuteVagBtnPre + memberid);
                        //隐藏申请者关闭音视频权限按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyItem.applyItemMuteVagBtnPre + memberid);
                        //显示参会者开启音视频权限按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnmuteVagBtnPre + memberid);
                        //隐藏参会者关闭音视频权限按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteVagBtnPre + memberid);
                    }
                    else {
                        console.error("发送禁言指令失败-----" + memberid);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    console.error("发送禁言指令失败-----" + memberid);
                });
            }
        }
        /**
        *  解除禁言(允许音视频)
        * */
        allowAllMembersVideoAndAudio() {
            let that = this;
            let group_members = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).allSubComponents;
            let myuserid = this.getMyProfile().getUserID();
            let myrole = this.getMyProfile().getRole();
            for (let value of group_members.values()) {
                let memberid = value.eleLabel.getAttribute("data-userId");
                //解除禁言
                that.allowMemberVideoAndAudio(memberid);
                //if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //    //自己不用发送通知
                //} else {
                //    //解除禁言
                //    that.allowMemberVideoAndAudio(memberid);
                //}
            }
            /**
             * 更新按钮显隐，显示全员禁言按钮，隐藏取消禁言按钮
             * **/
            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.MeetingControlMuteAllMembersBtn.Button.id);
            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.MeetingControlUnmuteAllMembersBtn.Button.id);
        }
        /**
         * 解除禁言(允许音视频)
         * */
        allowMemberVideoAndAudio(memberid, isParseApplyTip) {
            let that = this;
            let myuserid = that.getMyProfile().getUserID();
            let myrole = that.getMyProfile().getRole();
            if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //如果群主禁止自己，则直接return
                return;
            }
            else {
                let message = Synergy.SynergyTim.instance().createTextMessage(memberid, Synergy.SYNERGY_COMMAND_CLOSE_FORBIDDEN, TIM.TYPES.CONV_C2C);
                Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("----------发送解除禁言指令成功------" + memberid);
                        /**
                       * 处理左下角权限弹窗界面效果
                       * **/
                        if (isParseApplyTip) {
                            //隐藏同意按钮，显示已同意按钮
                            that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyTipItemConstant.vagAgreeBtnIdPre + memberid);
                            that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyTipItemConstant.vagAgreedBtnIdPre + memberid);
                            //移除此条目
                            that.getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.ApplyTipItemConstant.vagIdPre + memberid);
                            if (that.getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id).size() == 0) {
                                Synergy.UiManager.closeApplyTipList();
                            }
                        }
                        let attendee = that.timGroup.getAttendee(memberid);
                        attendee.vagauth = true;
                        that.timGroup.saveAttendee(attendee);
                        /**
                         * 更新参会者及申请列表里的记录状态
                         * **/
                        //隐藏申请者开启音视频权限按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyItem.applyItemUnMuteVagBtnPre + memberid);
                        //显示申请者关闭音视频权限按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyItem.applyItemMuteVagBtnPre + memberid);
                        //隐藏参会者开启音视频权限按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.UnmuteVagBtnPre + memberid);
                        //显示参会者关闭音视频权限按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.MuteVagBtnPre + memberid);
                    }
                    else {
                        console.error("发送解除禁言指令失败-----" + memberid);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    console.error("发送解除禁言指令失败-----" + memberid);
                });
            }
        }
        /**
         * 禁止全员操作(取消管理员权限)
         * */
        forbidAllMembers3DOperate() {
            let that = this;
            let group_members = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).allSubComponents;
            let myuserid = this.getMyProfile().getUserID();
            let myrole = this.getMyProfile().getRole();
            for (let value of group_members.values()) {
                let memberid = value.eleLabel.getAttribute("data-userId");
                //取消管理员权限
                that.forbidMember3DOperate(memberid);
                //if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //    //自己不用发送通知
                //} else {
                //    //取消管理员权限
                //    that.forbidMember3DOperate(memberid);
                //}
            }
            /**
             * 更新按钮显隐，显示允许全员操作按钮，隐藏禁止全员操作按钮
             * **/
            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.MeetingControlAllowAllMembersBtn.Button.id);
            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.MeetingControlBanAllMembersBtn.Button.id);
        }
        /**
         * 禁止操作,取消管理员权限
         * @param memberid
         */
        forbidMember3DOperate(memberid) {
            let that = this;
            let myuserid = that.getMyProfile().getUserID();
            let myrole = that.getMyProfile().getRole();
            if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //如果是群主自己禁止自己
                //参会者隐藏禁止操作按钮
                that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.BanOperateBtnPre + memberid);
                //参会者显示禁止操作按钮
                that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + memberid);
                that.getMyProfile().setAllowOperate(false);
                //Synergy.UiManager.hideAllRightMenuButton();
                //Synergy.UiManager.showNoOperateAuthRightMenuButton(true);
                Synergy.UiManager.initAdminRoleInterface(true);
                return;
            }
            else {
                Synergy.SynergyTim.instance().setGroupMemberRole(that.getTimGroup().getGroupID(), memberid, TIM.TYPES.GRP_MBR_ROLE_MEMBER).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("----------通知取消管理员权限（禁止操作）成功------" + memberid);
                        /**
                         * 更新参会者及申请列表里的记录状态
                         * **/
                        //隐藏申请者禁止操作按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyItem.applyItemBanOperateBtnPre + memberid);
                        //显示申请者允许操作按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyItem.applyItemAllowOperateBtnPre + memberid);
                        //隐藏参会者禁止操作按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.BanOperateBtnPre + memberid);
                        //显示参会者允许操作按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + memberid);
                    }
                    else {
                        console.error("通知取消管理员权限（禁止操作）失败-----" + memberid);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    console.error("通知取消管理员权限（禁止操作）失败-----" + memberid);
                });
            }
        }
        /**
         * 允许全员操作(添加管理员角色)
         * */
        allowAllMembers3DOperate() {
            let that = this;
            let group_members = that.sviewFrame.getUIManager().getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).allSubComponents;
            let myuserid = this.getMyProfile().getUserID();
            let myrole = this.getMyProfile().getRole();
            for (let value of group_members.values()) {
                let memberid = value.eleLabel.getAttribute("data-userId");
                //添加管理员权限
                that.allowMember3DOperate(memberid);
                //if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //    //自己不用发送通知
                //} else {
                //    //添加管理员权限
                //    that.allowMember3DOperate(memberid);
                //}
            }
            /**
             * 更新按钮显隐，显示禁止全员操作，隐藏允许全员操作
             * **/
            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.MeetingControlAllowAllMembersBtn.Button.id);
            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.MeetingControlBanAllMembersBtn.Button.id);
        }
        /**
         * 允许操作(添加管理员角色)
         * @param memberid
         * @param isParseApplyTip 是否处理的左下角权限申请提示信息
         */
        allowMember3DOperate(memberid, isParseApplyTip) {
            let that = this;
            let myuserid = that.getMyProfile().getUserID();
            let myrole = that.getMyProfile().getRole();
            if (myrole == Synergy.SYNERGY_MEMBER_ROLE_OWNER && myuserid == memberid) {
                //如果是群主自己允许自己
                //参会者显示禁止操作按钮
                that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.BanOperateBtnPre + memberid);
                //参会者隐藏允许操作按钮
                that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + memberid);
                that.getMyProfile().setAllowOperate(true);
                //Synergy.UiManager.hideAllRightMenuButton();
                //Synergy.UiManager.showHasOperateAuthRightMenuButton();
                Synergy.UiManager.initAdminRoleInterface(true);
                return;
            }
            else {
                Synergy.SynergyTim.instance().setGroupMemberRole(that.getTimGroup().getGroupID(), memberid, TIM.TYPES.GRP_MBR_ROLE_ADMIN).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        //console.log("----------通知添加管理员权限（允许操作）成功------" + memberid);
                        /**
                         * 处理左下角权限弹窗界面效果
                         * **/
                        if (isParseApplyTip) {
                            //隐藏同意按钮，显示已同意按钮
                            that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyTipItemConstant.operateAgreeBtnIdPre + memberid);
                            that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyTipItemConstant.operateAgreedlBtnIdPre + memberid);
                            //移除此条目
                            that.getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.ApplyTipItemConstant.operateIdPre + memberid);
                            if (that.getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id).size() == 0) {
                                Synergy.UiManager.closeApplyTipList();
                            }
                        }
                        /**
                         * 更新参会者及申请列表里的记录状态
                         * **/
                        //隐藏申请者允许操作按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyItem.applyItemAllowOperateBtnPre + memberid);
                        //显示申请者禁止操作按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyItem.applyItemBanOperateBtnPre + memberid);
                        //显示参会者禁止操作按钮
                        that.getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.BanOperateBtnPre + memberid);
                        //隐藏参会者允许操作按钮
                        that.getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + memberid);
                    }
                    else {
                        console.error("通知添加管理员权限（允许操作）失败-----" + memberid);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    console.error("通知添加管理员权限（允许操作）失败-----" + memberid);
                });
            }
        }
        /**
         * 申请音视频权限
         * */
        applyVagAuth() {
            Synergy.AlertUtill.confirm(SView[SView.UIManager.languageInfo].languageObj.Prompt.Prompt, SView[SView.UIManager.languageInfo].languageObj.Prompt.ApplyAudioAndVideo, "Synergy.Main.instance().applyVagAuthConfirm()");
        }
        applyVagAuthConfirm() {
            let that = this;
            let ownerId = that.timGroup.getOwnerID();
            let message = Synergy.SynergyTim.instance().createTextMessage(ownerId, Synergy.SYNERGY_COMMAND_APPLY_VAG, TIM.TYPES.CONV_C2C);
            Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.Prompt.HasApplyAudioAndVideoAuth);
                    //console.log("----------向群主申请音视频权限成功------");
                }
                else {
                    //console.error("向群主申请音视频权限失败-----" + retdata);
                    Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                //console.error("向群主申请音视频权限失败-----" + retdata);
                Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
            });
        }
        /**
         * 申请操作权限
         * */
        applyOperateAuth() {
            Synergy.AlertUtill.confirm(SView[SView.UIManager.languageInfo].languageObj.Prompt.Prompt, SView[SView.UIManager.languageInfo].languageObj.Prompt.ApplyAuth, "Synergy.Main.instance().applyOperateAuthConfirm()");
        }
        applyOperateAuthConfirm() {
            let that = this;
            let ownerId = that.timGroup.getOwnerID();
            let message = Synergy.SynergyTim.instance().createTextMessage(ownerId, Synergy.SYNERGY_COMMAND_APPLY_ROLE, TIM.TYPES.CONV_C2C);
            Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.Prompt.HasApplyOperateAuth);
                    //console.log("----------向群主申请操作权限成功------");
                }
                else {
                    Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                    //console.error("向群主申请操作权限失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                //console.error("向群主申请音视频权限失败-----" + retdata);
            });
        }
        /******************************************以下是音视频相关操作方法************************************************** */
        /**
         *  打开本地的音频
         * */
        openLocalAudio() {
            let that = this;
            Synergy.SynergyTRTC.instance().checkSystemRequirements().then(function (isSupport) {
                if (isSupport) {
                    if (that.myprofile.getVagAuth()) {
                        //判断是否有音频设备
                        Synergy.SynergyTRTC.instance().unmuteLocalAudio().then(function () {
                            let myuserid = that.myprofile.getUserID();
                            /**
                             * 处理界面显隐
                             * **/
                            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemOpenAudioBtn.Button.id);
                            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.LocalVideoItemCloseAudioBtn.Button.id);
                            //显示关闭音频按钮
                            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.AttendeeItem.MuteAudioBtnPre + myuserid);
                            //隐藏打开音频按钮
                            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + myuserid);
                            //console.log("------------打开本地的音频成功-------------------");
                        }).catch(function (errorname) {
                            switch (errorname) {
                                case 'NotFoundError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NoDeviceDetected, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                case 'NotAllowedError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.UnauthorizedDeviceAccedd, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                case 'NotReadableError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DeviceIsUnavailableAccessed, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                default:
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DeviceIsUnavailable, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                            }
                        });
                    }
                    else {
                        that.applyVagAuth();
                        //console.error("-------没有音视频权限，提示是否申请权限---------");
                    }
                }
                else {
                    //createTipWindow("", "当前浏览器不支持音视频功能,建议使用最新版本的 Chrome 浏览器");
                    //console.error("-------当前浏览器不支持音视频功能,建议使用最新版本的 Chrome 浏览器---------");
                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DoesnotSupportAudioAndVideo, Synergy.Dialog.AlertDialogType.WARNING);
                }
            });
        }
        /**
         * 关闭本地音频
         * */
        closeLocalAudio() {
            Synergy.SynergyTRTC.instance().muteLocalAudio();
            let myuserid = this.myprofile.getUserID();
            /**
             * 处理界面
             * **/
            this.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseAudioBtn.Button.id);
            this.sviewFrame.getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenAudioBtn.Button.id);
            //显示打开音频按钮
            this.sviewFrame.getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + myuserid);
            //隐藏关闭音频按钮
            this.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteAudioBtnPre + myuserid);
            //console.log("------------关闭本地音频-------------------");
        }
        /**
         * 打开本地视频
         * */
        openLocalVidio() {
            let that = this;
            Synergy.SynergyTRTC.instance().checkSystemRequirements().then(function (isSupport) {
                if (isSupport) {
                    if (that.myprofile.getVagAuth()) {
                        //判断是否有音频设备
                        Synergy.SynergyTRTC.instance().unmuteLocalVideo().then(function () {
                            let myuserid = that.myprofile.getUserID();
                            /**
                             * 处理界面显隐
                             * **/
                            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemOpenVideoBtn.Button.id);
                            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.LocalVideoItemCloseVideoBtn.Button.id);
                            document.getElementById(Synergy.Dialog.LocalVideoItem.VideoItem.id + Synergy.Dialog.VideoItem.maskSuffix).style.display = "none";
                            //显示关闭视频按钮
                            that.sviewFrame.getUIManager().showLabel(Synergy.Dialog.AttendeeItem.MuteVideoBtnPre + myuserid);
                            //隐藏打开视频按钮
                            that.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + myuserid);
                            //console.log("------------打开本地视频成功-------------------");
                        }).catch(function (errorname) {
                            switch (errorname) {
                                case 'NotFoundError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NoDeviceDetected, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                case 'NotAllowedError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.UnauthorizedDeviceAccedd, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                case 'NotReadableError':
                                    // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                                    //console.error("暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试");
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DeviceIsUnavailableAccessed, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                                default:
                                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DeviceIsUnavailable, Synergy.Dialog.AlertDialogType.WARNING);
                                    break;
                            }
                        });
                    }
                    else {
                        that.applyVagAuth();
                        //console.error("-------没有音视频权限，提示是否申请权限---------");
                    }
                }
                else {
                    //createTipWindow("", "当前浏览器不支持音视频功能,建议使用最新版本的 Chrome 浏览器");
                    //console.error("-------当前浏览器不支持音视频功能,建议使用最新版本的 Chrome 浏览器---------");
                    Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.DoesnotSupportAudioAndVideo, Synergy.Dialog.AlertDialogType.WARNING);
                }
            });
        }
        /**
         * 关闭本地视频
         * */
        closeLocalVidio() {
            let myuserid = this.myprofile.getUserID();
            Synergy.SynergyTRTC.instance().muteLocalVideo();
            /**
             * 处理界面
             * **/
            this.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseVideoBtn.Button.id);
            this.sviewFrame.getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenVideoBtn.Button.id);
            //this.sviewFrame.getUIManager().showLabel(Synergy.Dialog.LocalVideoItem.VideoItem.id + Synergy.Dialog.VideoItemMask.suffix);
            document.getElementById(Synergy.Dialog.LocalVideoItem.VideoItem.id + Synergy.Dialog.VideoItem.maskSuffix).style.display = "block";
            //显示打开视频按钮
            this.sviewFrame.getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + myuserid);
            //隐藏关闭视频按钮
            this.sviewFrame.getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteVideoBtnPre + myuserid);
            //console.log("------------关闭本地视频-------------------");
        }
    }
    Main.synergymain = null;
    Synergy.Main = Main;
})(Synergy || (Synergy = {}));
/// <reference path="../../../../lib/M3D.d.ts" />
var Synergy;
(function (Synergy) {
    class MyProfile {
        constructor() {
            this.allowOperate = false;
        }
        /**
         * 设置userid
         * @param userid
         */
        setUserID(userid) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_MY_PROFILE_USERID, userid, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取userid
         * @param serverurl
         */
        getUserID() {
            let userid = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_USERID);
            if (!userid) {
                userid = '';
            }
            return userid;
        }
        /**
         * 设置昵称
         * @param nickname
         */
        setNickName(nickname) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_MY_PROFILE_NICK, nickname, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取昵称
         * */
        getNickName() {
            let nickname = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_NICK);
            if (!nickname) {
                let timUserId = this.getUserID();
                nickname = 'user_' + timUserId;
            }
            return nickname;
        }
        /**
         * 设置操作权限
         * @param nickname
         * @param isallowoperate
         */
        setRole(role, isallowoperate) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_MY_PROFILE_ROLE, role, Synergy.SYNERGY_COOKIE_EXPIRETIME);
            this.setAllowOperate(isallowoperate);
        }
        setAllowOperate(isallowoperate) {
            //M3D.Utility.CookieHelper.addCookie("isAllowOperate", isallowoperate, Synergy.SYNERGY_COOKIE_EXPIRETIME);
            this.allowOperate = isallowoperate;
        }
        isAllowOperate() {
            //return M3D.Utility.CookieHelper.getCookie("isAllowOperate") == "true";
            return this.allowOperate;
        }
        /**
         * 获取操作权限
         * */
        getRole() {
            let role = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_ROLE);
            if (!role) {
                role = '';
            }
            return role;
        }
        /**
         * 设置音视频权限
         * @param auth
         */
        setVagAuth(auth) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_MY_PROFILE_VAG_AUTH, auth, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取音视频权限
         * */
        getVagAuth() {
            return M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_VAG_AUTH) == "true";
        }
        /**
         * 重置
         * */
        reSet() {
            this.setRole("", true);
            this.setVagAuth(true);
        }
    }
    Synergy.MyProfile = MyProfile;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    let Dialog;
    (function (Dialog) {
        Dialog.RightMenuBtns = {
            resetButton: {
                id: "resetButton"
            },
            groupConferenceButton: {
                id: "groupConferenceButton",
                badgeId: "groupConferenceButtonBadge"
            },
            groupConferenceCharButton: {
                id: "groupConferenceCharButton",
                badgeId: "groupConferenceCharButtonBadge"
            },
            meetingApplyButton: {
                id: "meetingApplyButton"
            },
            viewMenuBtn: {
                id: "viewMenuBtn"
            },
            assemblyTreeButton: {
                id: "assemblyTree"
            },
            attributeInfoButton: {
                id: "attributeInfo"
            },
            meetingMainMenuButton: {
                id: "meetingMainMenu"
            },
            moreMenuButton: {
                id: "moreMenu"
            }
        };
        Dialog.BottomMenu = {
            BottomMenu: {
                id: "mainBottomMenu"
            }
        };
        Dialog.AssemblyDialog = {
            TreeDialog: {
                id: "assemblyTreeDialog"
            }
        };
        Dialog.AttributeDialog = {
            AttributeDialog: {
                id: "attributeInfoDialog"
            }
        };
        /**
         * 会议界面
         * */
        Dialog.MeetingDialog = {
            "MeetingDialog": {
                "id": "meetingDialog",
                "classes": [
                    "SView-meeting"
                ],
                "titleBtns": [
                    {
                        "Button": {
                            "classes": [
                                "SView-button-iconBtn",
                                "meetingSet"
                            ],
                            "onClick": "Synergy.UiManager.openSettingMeetingDialog()",
                            "imgsrc": "images/meeting/MeetingSet.png"
                        }
                    },
                    {
                        "Button": {
                            "classes": [
                                "SView-button-iconBtn",
                                "meetingExist",
                            ],
                            "onClick": "Synergy.UiManager.closeMeetingDialog()",
                            "imgsrc": "images/meeting/MeetingExist.png"
                        }
                    }
                ],
                "images": [
                    {
                        "classes": [
                            "SView-meeting-create"
                        ],
                        "imgsrc": "images/meeting/meeting_create.png",
                        "onClick": "Synergy.UiManager.showCreatMettingDialog()"
                    },
                    {
                        "classes": [
                            "SView-meeting-join"
                        ],
                        "imgsrc": "images/meeting/meeting_join.png",
                        "onClick": "Synergy.UiManager.showJoinMeetingDialog()"
                    },
                    {
                        "classes": [
                            "SView-meeting-record"
                        ],
                        "imgsrc": "images/meeting/meeting_minutes.png",
                        // "onClick": "showMeetingRecord()"
                    }
                ]
            }
        };
        /**************************创建会议界面***********************************/
        Dialog.MeetingThemeInput = {
            Input: {
                id: "creatMeetingThemeInput",
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ThemePlaceHolder, //请输入主题
                //onInput: "checkLength('creatMeetingTheme')"
            }
        };
        Dialog.MeetingTheme = {
            WithBothInput: {
                id: "creatMeetingTheme",
                //warning: "主题不能为空！",
                title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Theme,
                subComponents: [
                    Dialog.MeetingThemeInput
                ]
            },
        };
        Dialog.MeetingNicknameInput = {
            Input: {
                id: "creatMeetingNickNameInput",
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickNamePlaceHolder,
                onInput: "checkLength('creatMeetingNickName')"
            }
        };
        Dialog.MeetingNickname = {
            WithBothInput: {
                id: "creatMeetingNickName",
                title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickName,
                //warning: "昵称不能为空！",
                subComponents: [
                    Dialog.MeetingNicknameInput
                ]
            }
        };
        //创建会议表单
        Dialog.CreateMeetingForm = {
            Form: {
                id: "creatMeetingForm",
                classes: [
                    "SView-form",
                    "SView-form-middle"
                ],
                subComponents: [
                    Dialog.MeetingTheme,
                    Dialog.MeetingNickname
                ]
            }
        };
        /**
         * 创建会议dialog
         * */
        Dialog.CreateMeetingDialog = {
            "Dialog": {
                "id": "create_meeting_dialog",
                "closeBtnOnClick": "Synergy.UiManager.closeCreatMettingDialog()",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CreateGroup,
                "subComponents": [
                    Dialog.CreateMeetingForm
                ],
                "buttons": [{
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "confirmExit",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CreateGroup,
                            "onClick": "Synergy.Main.instance().createGroup()"
                        }
                    }]
            }
        };
        /**************************加入会议界面***********************************/
        Dialog.JoinMeetingTabsMeetingListTable = {
            Table: {
                id: "join_meeting_tabs_meetinglist_table",
                classes: [
                    "SView-table", // 若不写，默认类名为"SView-table"
                ],
                cellTitle: [
                    SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Theme,
                    SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupID,
                    SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Host,
                    SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CreateTime //创建时间
                ],
                cellContent: [
                //["LYA-AL00 的会议", "65558424", "LYA-AL00", "2022-03-23 15:48:56"],
                //["Administrator的会议", "34739797", "DE", "2021-08-02 16:24:05"],
                ] // 表格内容
            }
        };
        Dialog.JoinMeetingTabsMeetingList = {
            dataActive: "true",
            title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupList,
            dataClass: "join_meeting_tabs_meetinglist_subcomponents",
            subComponentsId: "join_meeting_tabs_meetinglist_subcomponents",
            subComponents: [
                Dialog.JoinMeetingTabsMeetingListTable
            ]
        };
        Dialog.JoinMeetingTabsMeetingIdFormMeetingIdInput = {
            Input: {
                id: "join_meeting_tabs_meetingid_form_meetingid_input",
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupIDPlaceHolder, //请输入会议号
                //onInput: "checkLength('creatMeetingInput')"
            }
        };
        Dialog.JoinMeetingTabsMeetingIdFormMeetingId = {
            WithBothInput: {
                id: "join_meeting_tabs_meetingid_form_meetingid",
                //warning: "会议号不能为空！",
                title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupID,
                subComponents: [
                    Dialog.JoinMeetingTabsMeetingIdFormMeetingIdInput
                ]
            }
        };
        Dialog.JoinMeetingTabsMeetingIdFormNickNameInput = {
            Input: {
                id: "join_meeting_tabs_meetingid_form_nickname_input",
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickNamePlaceHolder, //请输入昵称
                //onInput: "checkLength('creatMeetingNickName')"
            }
        };
        Dialog.JoinMeetingTabsMeetingIdFormNickName = {
            WithBothInput: {
                id: "join_meeting_tabs_meetingid_form_nickname",
                title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickName,
                //warning: "昵称不能为空！",
                subComponents: [
                    Dialog.JoinMeetingTabsMeetingIdFormNickNameInput
                ]
            }
        };
        Dialog.JoinMeetingTabsMeetingIdForm = {
            Form: {
                id: "join_meeting_tabs_meetingid_form",
                classes: [
                    "SView-form",
                    "SView-form-middle"
                ],
                subComponents: [
                    Dialog.JoinMeetingTabsMeetingIdFormMeetingId,
                    Dialog.JoinMeetingTabsMeetingIdFormNickName
                ]
            }
        };
        Dialog.JoinMeetingTabsMeetingId = {
            title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupID,
            dataClass: "join_meeting_tabs_meetingid_subcomponents",
            subComponentsId: "join_meeting_tabs_meetingid_subcomponents",
            subComponents: [
                Dialog.JoinMeetingTabsMeetingIdForm
            ]
        };
        Dialog.JoinMeetingTabs = {
            Tabs: {
                id: "join_meeting_tabs",
                tabContent: [
                    Dialog.JoinMeetingTabsMeetingList,
                    Dialog.JoinMeetingTabsMeetingId
                ] // 标签页内容
            }
        };
        //加入会议
        Dialog.JoinMeetingDialog = {
            "Dialog": {
                "id": "join_meeting_dialog",
                "hideTitle": true,
                "closeBtnOnClick": "Synergy.UiManager.closeJoinMeetingDialog()",
                "subComponents": [
                    Dialog.JoinMeetingTabs
                ],
                "buttons": [{
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "confirmExit",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Join,
                            "onClick": "Synergy.Main.instance().joinGroup()"
                        }
                    }, {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-default"
                            ],
                            "id": "confirmExit",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Cancel,
                            //"onClick": "closeDialog('join_meeting_dialog')",
                            "onClick": "Synergy.UiManager.closeJoinMeetingDialog()",
                        }
                    }]
            }
        };
        /**************************会议设置界面***********************************/
        Dialog.SettingMeetingDialogFormNicknameInput = {
            Input: {
                id: "setting_meeting_dialog_form_nickname_input",
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickNamePlaceHolder, //请输入昵称
                //onInput: "checkLength('setMeetingNickName')"
            }
        };
        Dialog.SettingMeetingDialogFormNickname = {
            WithBothInput: {
                id: "setting_meeting_dialog_form_nickname",
                title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NickName,
                //warning: "昵称不能为空！",
                subComponents: [
                    Dialog.SettingMeetingDialogFormNicknameInput
                ]
            }
        };
        Dialog.SettingMeetingDialogForm = {
            Form: {
                id: "setting_meeting_dialog_form",
                classes: [
                    "SView-form",
                    "SView-form-middle"
                ],
                subComponents: [
                    Dialog.SettingMeetingDialogFormNickname
                ]
            }
        };
        //会议设置
        Dialog.SettingMeetingDialog = {
            "Dialog": {
                "id": "setting_meeting_dialog",
                "closeBtnOnClick": "Synergy.UiManager.closeSettingMeetingDialog()",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupSetting,
                "subComponents": [
                    Dialog.SettingMeetingDialogForm
                ],
                "buttons": [{
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "confirmExit",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Save,
                            "onClick": "Synergy.Main.instance().saveGroupSetting()"
                        }
                    }, {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "reset_setting",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Reset,
                            "onClick": "Synergy.UiManager.reSetSettingMeetingDialog()"
                        }
                    }]
            }
        };
        /**************************左上角音视频界面***********************************/
        Dialog.LocalVideoItemOpenAudioBtn = {
            Button: {
                "type": "iconButton",
                "classes": [
                    "SView-button-iconBtn",
                    "SView-video-button-1"
                ],
                "imgsrc": "images/meeting/sview_conference_menu_audio_off.png",
                "id": "local_stream_open_audio",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenAudio,
                "isShow": true,
                "onClick": "Synergy.Main.instance().openLocalAudio()",
            }
        };
        Dialog.LocalVideoItemCloseAudioBtn = {
            Button: {
                "type": "iconButton",
                "classes": [
                    "SView-button-iconBtn",
                    "SView-video-button-1"
                ],
                "imgsrc": "images/meeting/sview_conference_menu_audio_on.png",
                "id": "local_stream_close_audio",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseAudio,
                "isShow": false,
                "onClick": "Synergy.Main.instance().closeLocalAudio()",
            }
        };
        Dialog.LocalVideoItemOpenVideoBtn = {
            Button: {
                "type": "iconButton",
                "classes": [
                    "SView-button-iconBtn",
                    "SView-video-button-2"
                ],
                "imgsrc": "images/meeting/sview_conference_menu_video_off.png",
                "id": "local_stream_open_video",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenVideo,
                "isShow": true,
                "onClick": "Synergy.Main.instance().openLocalVidio()",
            }
        };
        Dialog.LocalVideoItemCloseVideoBtn = {
            Button: {
                "type": "iconButton",
                "classes": [
                    "SView-button-iconBtn",
                    "SView-video-button-2"
                ],
                "imgsrc": "images/meeting/sview_conference_menu_video_on.png",
                "id": "local_stream_close_video",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseVideo,
                "isShow": false,
                "onClick": "Synergy.Main.instance().closeLocalVidio()",
            }
        };
        Dialog.LocalVideoItem = {
            VideoItem: {
                id: "local_stream",
                name: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Me,
                classes: [
                    "SView-video-item",
                    "local_stream"
                ],
                subComponents: [
                    Dialog.LocalVideoItemOpenAudioBtn,
                    Dialog.LocalVideoItemCloseAudioBtn,
                    Dialog.LocalVideoItemOpenVideoBtn,
                    Dialog.LocalVideoItemCloseVideoBtn
                ]
            }
        };
        Dialog.RemoteVideoItem = {
            VideoItem: {
                id: "remote_stream_",
                isShow: false,
                name: "",
                classes: [
                    "SView-video-item",
                    "remote_stream" //移除远端视频框时需要用到此class
                ]
            }
        };
        Dialog.VideoItem = {
            maskSuffix: "_mask",
            remoteVideoIdPre: "remote_stream_",
            remoteVideoClass: "remote_stream"
        };
        //视频窗口
        Dialog.VideoDialog = {
            VideoDialog: {
                id: "meetingVideoDialog",
                subComponents: [
                    Dialog.LocalVideoItem
                ]
            }
        };
        /**************************文本聊天界面***********************************/
        //聊天窗口Item
        Dialog.LeftChatItem = {
            Item: {
                itemType: "leftChatItem",
                name: "",
                text: "",
                imgsrc: "images/meeting/sview_conference_chart_other.png",
                time: ""
            }
        };
        //聊天窗口Item
        Dialog.RightChatItem = {
            Item: {
                itemType: "rightChatItem",
                name: "",
                text: "",
                imgsrc: "images/meeting/sview_conference_chart_me.png",
                time: ""
            }
        };
        //聊天列表
        Dialog.ChatList = {
            List: {
                id: "chat_list",
                classes: [
                    "SView-chat-list"
                ],
                subComponents: [
                    Dialog.LeftChatItem,
                    Dialog.RightChatItem
                ] // 标签页内容
            }
        };
        //聊天内容输入框
        Dialog.ChatDialogInput = {
            Input: {
                id: "chat_dialog_input",
                classes: [
                    "SView-complexInput",
                ],
                value: "",
                placeHolder: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ChatContentPlaceHolder,
                autoComplete: "off"
            }
        };
        //聊天窗口
        Dialog.ChatDialog = {
            "Dialog": {
                "id": "chat_dialog",
                "closeBtnOnClick": "Synergy.UiManager.closeChatDialog()",
                "title": "",
                "subComponents": [
                    Dialog.ChatList
                ],
                isShow: false,
                buttons: [
                    Dialog.ChatDialogInput,
                    {
                        InputButton: {
                            id: "send_chat_btn",
                            onClick: "Synergy.Main.instance().sendChatMessage()",
                            imgsrc: "",
                            btnTitle: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Send, //发送
                            //warning: "内容不能为空!"
                        }
                    }
                ]
            }
        };
        /**************************会议主界面，参会者界面顶部控制按钮***********************************/
        Dialog.MeetingControlMuteAllMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/forbidden_open.png",
                "id": "mute_all_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.MuteAllMembers,
                "onClick": "Synergy.Main.instance().forbidAllMembersVideoAndAudio()"
            }
        };
        Dialog.MeetingControlUnmuteAllMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/forbidden_close.png",
                "id": "unmute_all_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.UnmuteAllMembers,
                "onClick": "Synergy.Main.instance().allowAllMembersVideoAndAudio()"
            }
        };
        Dialog.MeetingControlAllowAllMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/meeting_role_presentor.png",
                "id": "allow_all_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.AllowAllMembers,
                "onClick": "Synergy.Main.instance().allowAllMembers3DOperate()",
            }
        };
        Dialog.MeetingControlBanAllMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/meeting_role_common.png",
                "id": "ban_all_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.BanAllMembers,
                "onClick": "Synergy.Main.instance().forbidAllMembers3DOperate()",
            }
        };
        Dialog.MeetingControlShowApplyMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/show_apply.png",
                "id": "show_apply_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ApplyMembers,
                "onClick": "Synergy.UiManager.openMeetingApplysDialog()",
            }
        };
        Dialog.MeetingControlInviteMembersBtn = {
            Button: {
                "type": "iconTextButton_top",
                "isShow": false,
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/add_attender.png",
                "id": "invite_members_btn",
                "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Invite,
                "onClick": "Synergy.Utill.copyToClipBoard()",
            }
        };
        Dialog.MeetingControlAttendeeButtonList = {
            List: {
                id: "meeting_control_attendee_button_list",
                classes: [
                    "SView-attendee-button"
                ],
                subComponents: [
                    {
                        Item: {
                            itemType: "labelItem",
                            id: "fieldSizeItem",
                            subComponents: [
                                Dialog.MeetingControlMuteAllMembersBtn,
                                Dialog.MeetingControlUnmuteAllMembersBtn,
                                Dialog.MeetingControlAllowAllMembersBtn,
                                Dialog.MeetingControlBanAllMembersBtn,
                                Dialog.MeetingControlShowApplyMembersBtn,
                                Dialog.MeetingControlInviteMembersBtn
                            ]
                        }
                    }
                ] // 标签页内容
            }
        };
        /**************************会议主界面，参会者列表***********************************/
        Dialog.AttendeeItemApplyVagBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/vga_apply.png",
                "id": "attendee_apply_vag_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ApplyAudioAndVideoOperation,
                "onClick": "",
            }
        };
        Dialog.AttendeeItemMuteVideoBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_video_selected.png",
                "id": "attendee_mute_video_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseVideo,
                "onClick": "Synergy.Main.instance().closeLocalVidio()",
            }
        };
        Dialog.AttendeeItemUnMuteVideoBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_video_common.png",
                "id": "attendee_unmute_video_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenVideo,
                "onClick": "Synergy.Main.instance().openLocalVidio()",
            }
        };
        Dialog.AttendeeItemMuteAudioBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_audio_selected.png",
                "id": "attendee_mute_audio_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseAudio,
                "onClick": "Synergy.Main.instance().closeLocalAudio()",
            }
        };
        Dialog.AttendeeItemUnMuteAudioBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_audio_common.png",
                "id": "attendee_unmute_audio_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenAudio,
                "onClick": "Synergy.Main.instance().openLocalAudio()",
            }
        };
        Dialog.AttendeeItemMuteVagBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/forbidden_close.png",
                "id": "attendee_mute_vag_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseAudioAndVideoOperation,
                "onClick": "",
            }
        };
        Dialog.AttendeeItemUnmuteVagBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/forbidden_open.png",
                "id": "attendee_unmute_vag_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenAudioAndVideoOperation,
                "onClick": "",
            }
        };
        Dialog.AttendeeItemAllowOperateBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_role_common.png",
                "id": "attendee_allow_operate_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.AllowOperate,
                "onClick": "",
            }
        };
        Dialog.AttendeeItemBanOperateBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_role_presentor.png",
                "id": "attendee_ban_operate_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.BanOperate,
                "onClick": "",
            }
        };
        Dialog.MeetingControlAttendeeItem = {
            Item: {
                memuserid: '',
                itemType: "labelItem",
                text: "",
                id: "",
                subComponents: [
                    Dialog.AttendeeItemApplyVagBtn,
                    Dialog.AttendeeItemMuteVideoBtn,
                    Dialog.AttendeeItemUnMuteVideoBtn,
                    Dialog.AttendeeItemMuteAudioBtn,
                    Dialog.AttendeeItemUnMuteAudioBtn,
                    Dialog.AttendeeItemMuteVagBtn,
                    Dialog.AttendeeItemUnmuteVagBtn,
                    Dialog.AttendeeItemAllowOperateBtn,
                    Dialog.AttendeeItemBanOperateBtn
                ]
            }
        };
        Dialog.AttendeeItem = {
            host: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupOwner,
            me: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.BanOperate,
            idPre: "attendee_",
            ApplyVagBtnPre: "attendee_apply_vag_btn_",
            MuteVideoBtnPre: "attendee_mute_video_btn_",
            UnMuteVideoBtnPre: "attendee_unmute_video_btn_",
            MuteAudioBtnPre: "attendee_mute_audio_btn_",
            UnMuteAudioBtnPre: "attendee_unmute_audio_btn_",
            MuteVagBtnPre: "attendee_mute_vag_btn_",
            UnmuteVagBtnPre: "attendee_unmute_vag_btn_",
            AllowOperateBtnPre: "attendee_allow_operate_btn_",
            BanOperateBtnPre: "attendee_ban_operate_btn_"
        };
        //参会者列表
        Dialog.MeetingControlAttendeeList = {
            List: {
                id: "meeting_control_attendee_list",
                classes: [
                    "SView-attendee-content"
                ],
                subComponents: [
                //MeetingControlAttendeeItem
                ]
            }
        };
        /**************************会议主界面，会议附件***********************************/
        Dialog.FileInput = {
            Input: {
                id: "file_input",
                isShow: false,
                accept: "image/*",
                inputType: "file",
                onChange: "Synergy.Main.instance().sendAttachment()"
            }
        };
        /**
         * 预览附件dialog
         * */
        Dialog.PreviewAttachmentDialog = {
            "Dialog": {
                "id": "preview_attachment_dialog",
                "closeBtnOnClick": "",
                "hideTitle": true,
                "otherHide": true,
                "closeBtnIsShow": false,
                "contentImg": "",
            }
        };
        Dialog.MeetingControlAttachmentItem = {
            Item: {
                id: "meeting_control_attachment_item",
                itemType: "imgItem",
                name: "",
                time: "",
                imgsrc: "",
                bigImgSrc: "",
                onClick: ""
            }
        };
        Dialog.AttachmentItem = {
            myName: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Me, //我
        };
        Dialog.MeetingControlAttachmentList = {
            List: {
                id: "meeting_control_attachment_list",
                classes: [
                    "SView-imgItem-List"
                ],
                subComponents: [
                //MeetingControlAttachmentItem,
                // MeetingControlAttachmentItem,
                ] // 标签页内容
            }
        };
        /**************************会议主界面，会议信息***********************************/
        Dialog.MeetingControlInfoFormThemeInput = {
            Input: {
                id: "meeting_control_info_form_theme_input",
                readOnly: true,
                value: "",
            }
        };
        Dialog.MeetingControlInfoFormHostInput = {
            Input: {
                id: "meeting_control_info_form_host_input",
                readOnly: true,
                value: "",
            }
        };
        Dialog.MeetingControlInfoFormGroupIdInput = {
            Input: {
                id: "meeting_control_info_form_groupid_input",
                readOnly: true,
                value: "",
            }
        };
        Dialog.MeetingControlInfoForm = {
            Form: {
                id: "meeting_control_info_form",
                classes: [
                    "SView-form",
                    "SView-form-middle"
                ],
                subComponents: [
                    {
                        WithTitleInput: {
                            id: "meeting_control_info_form_theme",
                            title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Theme,
                            subComponents: [
                                Dialog.MeetingControlInfoFormThemeInput
                            ]
                        }
                    },
                    {
                        WithTitleInput: {
                            id: "meeting_control_info_form_host",
                            title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Host,
                            subComponents: [
                                Dialog.MeetingControlInfoFormHostInput
                            ]
                        }
                    },
                    {
                        WithTitleInput: {
                            id: "meeting_control_info_form_groupid",
                            title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupID,
                            subComponents: [
                                Dialog.MeetingControlInfoFormGroupIdInput
                            ]
                        }
                    },
                    {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-default"
                            ],
                            "id": "inviteButton",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Invite,
                            "onClick": "Synergy.Utill.copyToClipBoard()"
                        }
                    }
                ]
            }
        };
        /**************************会议主界面，会议中控制面板***********************************/
        //export const InviteTextInput = {
        //    Input: {
        //        id: "invite_text",
        //        isShow: false,
        //        value: ""
        //    }
        //}
        //会议中控制面板
        Dialog.MeetingControlDialog = {
            "Dialog": {
                "id": "meeting_control_dialog",
                "closeBtnOnClick": "Synergy.UiManager.closeMeetingControlDialog()",
                "hideTitle": true,
                "isShow": false,
                "subComponents": [
                    {
                        Tabs: {
                            id: "meeting_control_tabs",
                            tabContent: [
                                {
                                    dataActive: "true",
                                    title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Attendees,
                                    dataClass: "meeting_control_attendee_tab",
                                    subComponentsId: "meeting_control_attendee_tab",
                                    subComponents: [
                                        Dialog.MeetingControlAttendeeButtonList,
                                        Dialog.MeetingControlAttendeeList
                                    ]
                                },
                                {
                                    title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Attachment,
                                    dataClass: "meeting_control_attachment_tab",
                                    subComponentsId: "meeting_control_attachment_tab",
                                    subComponents: [
                                        Dialog.MeetingControlAttachmentList,
                                        Dialog.FileInput,
                                        {
                                            Button: {
                                                "type": "iconButton",
                                                "classes": [
                                                    "SView-button-iconBtn",
                                                    "SView-file-button"
                                                ],
                                                "imgsrc": "images/meeting/meeting_video_common.png",
                                                "id": "meeting_control_attachment_submit_btn",
                                                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.SelectFile,
                                                "onClick": "Synergy.Main.instance().addFile()",
                                            }
                                        },
                                    ]
                                },
                                {
                                    title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Info,
                                    dataClass: "meeting_control_info_tab",
                                    subComponentsId: "meeting_control_info_tab",
                                    subComponents: [
                                        Dialog.MeetingControlInfoForm
                                    ]
                                }
                            ] // 标签页内容
                        },
                    },
                    //InviteTextInput
                ],
                "buttons": [
                    {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "meeting_control_confirm",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ExitGroup,
                            "onClick": "Synergy.Main.instance().dismissGroup()"
                        }
                    },
                    {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-default"
                            ],
                            "id": "meeting_control_exit",
                            "content": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Cancel,
                            "onClick": "Synergy.UiManager.closeMeetingControlDialog()",
                        }
                    }
                ]
            }
        };
        /**************************申请人员界面***********************************/
        Dialog.ApplyItemMuteVagBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/forbidden_close.png",
                "id": "apply_mute_vag_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.CloseAudioAndVideoOperation,
                "onClick": "",
            }
        };
        Dialog.ApplyItemUnMuteVagBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/forbidden_open.png",
                "id": "apply_unmute_vag_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.OpenAudioAndVideoOperation,
                "onClick": "",
            }
        };
        Dialog.ApplyItemAllowOperateBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_role_common.png",
                "id": "apply_allow_operate_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.AllowOperate,
                "onClick": "",
            }
        };
        Dialog.ApplyItemBanOperateBtn = {
            Button: {
                "type": "iconButton",
                "isShow": false,
                "classes": [
                    "SView-button-iconBtn",
                ],
                "imgsrc": "images/meeting/meeting_role_presentor.png",
                "id": "apply_ban_operate_btn",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.BanOperate,
                "onClick": "",
            }
        };
        Dialog.MeetingApplyItem = {
            Item: {
                id: "",
                itemType: "labelItem",
                text: "",
                subComponents: [
                    Dialog.ApplyItemMuteVagBtn,
                    Dialog.ApplyItemUnMuteVagBtn,
                    Dialog.ApplyItemAllowOperateBtn,
                    Dialog.ApplyItemBanOperateBtn
                ]
            }
        };
        Dialog.ApplyItem = {
            idPre: "apply_",
            applyItemMuteVagBtnPre: "apply_mute_vag_btn_",
            applyItemUnMuteVagBtnPre: "apply_unmute_vag_btn_",
            applyItemAllowOperateBtnPre: "apply_allow_operate_btn_",
            applyItemBanOperateBtnPre: "apply_ban_operate_btn_",
        };
        Dialog.MeetingApplyList = {
            List: {
                id: "meeting_apply_list",
                classes: [
                    "SView-attendee-content"
                ],
                subComponents: [
                //MeetingApplyItem
                ] // 标签页内容
            }
        };
        //申请人员dialog
        Dialog.MeetingApplysDialog = {
            "Dialog": {
                "id": "meeting_apply_dialog",
                "isShow": false,
                "closeBtnOnClick": "Synergy.UiManager.closeMeetingApplysDialog()",
                "title": SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ApplyMembers,
                "subComponents": [
                    Dialog.MeetingApplyList
                ]
            }
        };
        /**************************左下角申请信息提示界面***********************************/
        Dialog.ApplyTipItemAgreeBtn = {
            Button: {
                id: "",
                isShow: true,
                type: "basicButton",
                content: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Agree,
                classes: [
                    "SView-button",
                ],
                onClick: ""
            }
        };
        Dialog.ApplyTipItemAgreedBtn = {
            Button: {
                id: "",
                isShow: false,
                type: "basicButton",
                content: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Ageeed,
                classes: [
                    "SView-button",
                ],
                onClick: ""
            }
        };
        //左下角操作权限提示框
        Dialog.ApplyTipItem = {
            PopMessage: {
                id: "apply_tip_item_",
                content: "",
                subComponents: [
                    Dialog.ApplyTipItemAgreeBtn,
                    Dialog.ApplyTipItemAgreedBtn
                ]
            }
        };
        Dialog.ApplyTipItemConstant = {
            operateIdPre: "apply_tip_item_operate_",
            vagIdPre: "apply_tip_item_vag_",
            operateContentSuffix: "<br>" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ApplyOperation,
            vagContentSuffix: "<br>" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ApplyAudioAndVideoOperation,
            operateAgreeBtnIdPre: "apply_tip_item_operate_agree_btn_",
            operateAgreedlBtnIdPre: "apply_tip_item_operate_agreed_btn_",
            vagAgreeBtnIdPre: "apply_tip_item_vag_agree_btn_",
            vagAgreedBtnIdPre: "apply_tip_item_vag_agreed_btn_"
        };
        Dialog.ApplyTipList = {
            List: {
                id: "apply_tip_list",
                isShow: true,
                classes: [
                    "SView-permissionMessageList"
                ],
                subComponents: [
                    Dialog.ApplyTipItem
                ]
            }
        };
        /**************************各种弹框界面***********************************/
        //loading框
        Dialog.LoadingDialog = {
            "ResultDialog": {
                "id": "loading_dialog",
                "classes": [
                    "SView-info",
                ],
                "icon": {
                    "classes": [
                        "SView-info-icon",
                    ],
                    "imgsrc": "./images/info/loading.png",
                },
                "titleContent": "",
                "detailContent": "", //内容的详细描述
            },
        };
        //info error warning success提示弹框
        Dialog.AlertDialogType = {
            INFO: "info",
            WARNING: "warning",
            ERROR: "error",
            SUCCESS: "success",
            INFO_IMG: "./images/info/info.png",
            WARNING_IMG: "./images/info/warning.png",
            ERROR_IMG: "./images/info/failed.png",
            SUCCESS_IMG: "./images/info/success.png",
        };
        Dialog.AlertDialog = {
            "ResultDialog": {
                "id": "alert_dialog",
                "classes": [
                    "SView-info",
                ],
                "icon": {
                    "classes": [
                        "SView-info-icon",
                    ],
                    "imgsrc": "",
                },
                "titleContent": "",
                "detailContent": "",
                "type": "withButton",
                "button": {
                    Button: {
                        "type": "basicButton",
                        "classes": [
                            "SView-button",
                            "SView-button-primary"
                        ],
                        "id": "alert_dialog_confirm_btn",
                        "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Confirm,
                        "onClick": "Synergy.AlertUtill.hideAlert()"
                    }
                }
            },
        };
        //顶部黑色信息提示框
        Dialog.ToastDialog = {
            "Message": {
                "classes": [
                    "SView-message",
                ],
                "id": "toast_dialog",
                "content": "",
                "messageContentColor": "white",
                "messageBGColor": "black"
            }
        };
        //确认框
        Dialog.ConfirmDialog = {
            "Dialog": {
                "classes": [
                    "SView-dialog",
                ],
                "id": "confirm_dialog",
                "content": "",
                "onClick": "Synergy.AlertUtill.hideConfirm()",
                "showBack": true,
                "title": "",
                "buttons": [{
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-primary"
                            ],
                            "id": "confirm_dialog_confirm_btn",
                            "content": SView[SView.UIManager.languageInfo].languageObj.Prompt.Confim,
                            "onClick": ""
                        }
                    }, {
                        Button: {
                            "type": "basicButton",
                            "classes": [
                                "SView-button",
                                "SView-button-default"
                            ],
                            "id": "confirm_dialog_cancel_btn",
                            "content": SView[SView.UIManager.languageInfo].languageObj.Prompt.Cancle,
                            "onClick": "Synergy.AlertUtill.hideConfirm()"
                        }
                    }]
            }
        };
    })(Dialog = Synergy.Dialog || (Synergy.Dialog = {}));
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class TimGroup {
        constructor() {
            this.inGroup = false;
            this.attendeesObj = {};
        }
        /**
         * 设置会议id
         * @param groupid
         * @param isingroup 用于区别之前是否在会议中
         */
        setGroupID(groupid, isingroup) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_GROUPID, groupid, Synergy.SYNERGY_COOKIE_EXPIRETIME);
            //M3D.Utility.CookieHelper.addCookie("isInGroup", isingroup, Synergy.SYNERGY_COOKIE_EXPIRETIME);
            this.setInGroup(isingroup);
        }
        /***
         * 获取会议id
         * */
        getGroupID() {
            let groupid = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_GROUPID);
            if (!groupid) {
                groupid = '';
            }
            return groupid;
        }
        setInGroup(isInGroup) {
            this.inGroup = isInGroup;
        }
        isInGroup() {
            return this.inGroup;
        }
        /**
         * 设置当前会议名称（主题）
         * @param name
         */
        setName(name) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_NAME, name, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取当前会议名称（主题）
         * */
        getName() {
            let theme = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_NAME);
            if (!theme) {
                theme = '';
            }
            return theme;
        }
        /**
         * 设置当前会议所属者id（群主）
         * @param ownerid
         */
        setOwnerID(ownerid) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_OWNERID, ownerid, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取当前会议所属者id（群主）
         * */
        getOwnerID() {
            let ownerid = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_OWNERID);
            if (!ownerid) {
                ownerid = '';
            }
            return ownerid;
        }
        /**
         * 设置当前会议所属者昵称(群主昵称)
         * @param ownernick
         */
        setOwnerNick(ownernick) {
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_OWNERNICK, ownernick, Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 获取当前会议所属者昵称(群主昵称)
         * */
        getOwnerNick() {
            let ownernick = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_OWNERNICK);
            if (!ownernick) {
                let ownerid = this.getOwnerID();
                ownernick = 'owner_' + ownerid;
            }
            return ownernick;
        }
        /**
         * 创建一个参会者
         * @param userid
         * @param nickname
         * @param vagauth
         */
        createAttendee(userid, nickname, vagauth) {
            let attendee = {
                userid: userid,
                nickname: nickname,
                vagauth: vagauth
            };
            return attendee;
        }
        /**
         * 获取参会者
         * @param userid
         */
        getAttendee(userid) {
            let attendeesObjStr = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES);
            if (attendeesObjStr) {
                this.attendeesObj = JSON.parse(attendeesObjStr);
            }
            let attendeeStr = this.attendeesObj[this.getGroupID() + userid];
            if (attendeeStr != undefined) {
                return JSON.parse(attendeeStr);
            }
            else {
                return "";
            }
        }
        /**
         * 添加参会者
         * @param attendee
         */
        saveAttendee(attendee) {
            let attendeesObjStr = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES);
            if (attendeesObjStr) {
                this.attendeesObj = JSON.parse(attendeesObjStr);
            }
            let key = this.getGroupID() + attendee.userid;
            this.attendeesObj[key] = JSON.stringify(attendee);
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES, JSON.stringify(this.attendeesObj), Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 移除参会者
         * @param userid
         */
        removeAttendee(userid) {
            let attendeesObjStr = M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES);
            if (attendeesObjStr) {
                this.attendeesObj = JSON.parse(attendeesObjStr);
            }
            let oldValue = this.attendeesObj[this.getGroupID() + userid];
            if (oldValue != undefined) {
                delete this.attendeesObj[this.getGroupID() + userid];
            }
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES, JSON.stringify(this.attendeesObj), Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
        /**
         * 重置
         * */
        reSet() {
            this.setGroupID("", false);
            this.setName("");
            this.setOwnerID("");
            this.setOwnerNick("");
            this.attendeesObj = {};
            M3D.Utility.CookieHelper.addCookie(Synergy.SYNERGY_CUR_GROUP_ATTENDEES, JSON.stringify(this.attendeesObj), Synergy.SYNERGY_COOKIE_EXPIRETIME);
        }
    }
    Synergy.TimGroup = TimGroup;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class SynergyMessages {
        /**
         * 解析消息
         * @param msg
         * @param nick
         */
        static parseMsg(msg, nick) {
            let msgObj = JSON.parse(msg);
            let sviewMacros = msgObj.SViewMacros;
            let name = sviewMacros.Name;
            let des = sviewMacros.Description;
            switch (name) {
                case "Camera":
                    Synergy.ParseMessageUtill.parseCameraMsg(sviewMacros.Parameters);
                    //M3D.NotificationCenter.Instance().PostNotification(M3D.NotificationName.MouseStateChange, -1);
                    break;
                case "Shape":
                    Synergy.ParseMessageUtill.ParseShapeMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "View":
                    Synergy.ParseMessageUtill.ParseViewMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "WalkThrough":
                    Synergy.ParseMessageUtill.parseWalkThroughMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "SetRenderMode":
                    Synergy.ParseMessageUtill.parseSetRenderModeMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "Menu":
                    Synergy.ParseMessageUtill.parseMenuMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "GestureNote":
                    Synergy.ParseMessageUtill.parseGestureNoteMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "TextNote":
                    Synergy.ParseMessageUtill.parseTextNoteMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "SequenceNote":
                    Synergy.ParseMessageUtill.parseSequenceNoteMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "Annotation":
                    Synergy.ParseMessageUtill.parseAnnotationMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "MSection":
                    Synergy.ParseMessageUtill.parseMSectionMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "Explosion":
                    Synergy.ParseMessageUtill.parseExplosionMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "Animation":
                    Synergy.ParseMessageUtill.parseSetAnimationMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "Measure":
                    Synergy.ParseMessageUtill.parseSetMeasureMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
                case "File":
                    Synergy.ParseMessageUtill.parseFileMsg(sviewMacros.Parameters);
                    //SynergyCallBack.createTipWindow(nick, des);
                    break;
            }
        }
        /**************************************文件相关******************************************/
        /**
         * 选中模型
         * @param path 模型对应的路径
         *{"SViewMacros":{
         *   "Parameters":"{
         *       "Path":"PATH|0|3|ShapeModel",
         *       "Operator":"SelectShape"
         *       }",
         *   "Name":"Shape",
         *   "Description":"选择模型"
         *   }
         *  }
         */
        static fileOpenOrCloseFile(operator, url, fileid, des) {
            let params = {};
            params["Operator"] = operator;
            params["FileUrl"] = url;
            params["FileID"] = fileid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "File";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************摄相机相关******************************************/
        /**
        * 设置摄像机
        * @param posx 平移X
        * @param posy 平移Y
        * @param posz 平移Z
        * @param rotw 旋转W
        * @param rotx 旋转X
        * @param roty 旋转Y
        * @param rotz 旋转Z
        * @param zoom 缩放
        *{"SViewMacros":{
        *  "Parameters":"{
        *       "State":"{\"PosX\" : 9,\"PosY\" : 11,\"PosZ\" : -775,\"RotW\" : 0.5,\"RotX\" : -0.68,\"RotY\" : 0.52,\"RotZ\" : -0.025,\"Zoom\" : 1.7}",
        *       "Operator":"SetCamera"
        *       }",
        *   "Name":"Camera",
        *   "Description":""
        *   }
        *  }
        */
        static cameraSetCamera(posx, posy, posz, rotw, rotx, roty, rotz, zoom) {
            //组织数据json
            let state = {};
            state["PosX"] = posx;
            state["PosY"] = posy;
            state["PosZ"] = posz;
            state["RotW"] = rotw;
            state["RotX"] = rotx;
            state["RotY"] = roty;
            state["RotZ"] = rotz;
            state["Zoom"] = zoom;
            let params = {};
            params["Operator"] = "SetCamera";
            params["State"] = JSON.stringify(state);
            let macros = {};
            macros["Description"] = "";
            macros["Name"] = "Camera";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************模型相关******************************************/
        /**
         * 选中模型
         * @param path 模型对应的路径
         *{"SViewMacros":{
         *   "Parameters":"{
         *       "Path":"PATH|0|3|ShapeModel",
         *       "Operator":"SelectShape"
         *       }",
         *   "Name":"Shape",
         *   "Description":"选择模型"
         *   }
         *  }
         */
        static shapeSelectShape(path, des) {
            let params = {};
            params["Operator"] = "SelectShape";
            params["Path"] = path;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //设置模型显隐
        static shapeSetShapeVisiable(path, visiable, des) {
            let params = {};
            params["Operator"] = "SetShapeVisiable";
            params["Path"] = path;
            params["Visiable"] = visiable;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //
        static shapeOnlyShow(paths, des) {
            let params = {};
            params["Operator"] = "OnlyShow";
            params["Paths"] = paths;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        // 设置模型颜色
        static shapeSetShapeColor(color, path, des) {
            let params = {};
            params["Operator"] = "SetShapeColor";
            params["Color"] = color;
            params["Path"] = path;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**
         * 复位模型
         * @param path
         * @param des
         */
        static shapeRestoreShape(path, des) {
            let params = {};
            params["Operator"] = "RestoreShape";
            params["Path"] = path;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************menu相关******************************************/
        /**
         *    显隐切换
        **/
        static menuExchangeHideShow(des) {
            let params = {};
            params["Operator"] = "ExchangeHideShow";
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Menu";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**
         * 取消选中模型
         * @param path 模型对应的路径
         *{"SViewMacros":{
         *   "Parameters":"{
         *       "Path":"PATH|0|3|ShapeModel",
         *       "Operator":"UnSelectShape"
         *      }",
         *   "Name":"Shape",
         *   "Description":"取消选择模型"
         *   }
         *  }
         */
        static shapeUnSelectShape(path, direction) {
            let params = {};
            params["Operator"] = "UnSelectShape";
            params["Path"] = path;
            let macros = {};
            macros["Description"] = direction;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**清空选中模型
        *{"SViewMacros":{
        *    "Parameters":"{
        *        "Operator":"ClearSelector"
        *        }",
        *    "Name":"Shape",
        *    "Description":"取消选择模型"
        *    }
        *  }
        */
        static shapeClearSelector(direction) {
            let params = {};
            params["Operator"] = "ClearSelector";
            let macros = {};
            macros["Description"] = "清空选择模型";
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //移动模型
        static shapeSetShapeTransform(path, posx, posy, posz, rotw, rotx, roty, rotz, sclX, sclY, sclZ, des) {
            //组织数据json
            let Tran = {};
            Tran["PosX"] = posx;
            Tran["PosY"] = posy;
            Tran["PosZ"] = posz;
            Tran["RotW"] = rotw;
            Tran["RotX"] = rotx;
            Tran["RotY"] = roty;
            Tran["RotZ"] = rotz;
            Tran["SclX"] = sclX;
            Tran["SclY"] = sclY;
            Tran["SclZ"] = sclZ;
            let params = {};
            params["Operator"] = "SetShapeTransform";
            params["Path"] = path;
            params["Tran"] = JSON.stringify(Tran);
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Shape";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************视图相关******************************************/
        /**复位视图
        *{"SViewMacros":{
        *    "Parameters":"{
        *        "Operator":"RestoreView"
        *        }",
        *    "Name":"View",
        *    "Description":"复位"
        *    }
        *  }
        */
        static viewRestoreView(direction) {
            let params = {};
            params["Operator"] = "RestoreView";
            let macros = {};
            macros["Description"] = direction;
            macros["Name"] = "View";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**切换视图
       *{"SViewMacros":{
       *    "Parameters": "{"Operator":"SetView","Index":-1,"Type":3}"
       *    "Name":"View",
       *    "Description":"切换视图"
       *    }
       *  }
       */
        static viewSetView(viewIndex, viewType, direction) {
            let params = {};
            params["Operator"] = "SetView";
            params["Index"] = viewIndex;
            params["Type"] = viewType;
            let macros = {};
            macros["Description"] = "切换视图";
            macros["Name"] = "View";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //创建视图
        static viewCreateView(ModelViewId, ModelViewName, des) {
            let params = {};
            params["Operator"] = "CreateView";
            params["ModelViewId"] = ModelViewId;
            params["ModelViewName"] = ModelViewName;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "View";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //删除视图
        static viewDeleteView(ModelViewId, des) {
            let params = {};
            params["Operator"] = "DeleteView";
            params["ModelViewId"] = ModelViewId;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "View";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //切换视图
        static viewChangeView(ModelViewId, des) {
            let params = {};
            params["Operator"] = "ChangeView";
            params["ModelViewId"] = ModelViewId;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "View";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************漫游相关******************************************/
        /**
        *开启漫游
        *{
        *    "SViewMacros": {
        *        "Description": "开启漫游",
        *        "Name": "WalkThrough",
        *        "Parameters": "{"Operator":"Show","Camera":"{\n   \"PosX\" : 3130.93457031250,\n   \"PosY\" : 2825.772705078125,\n   \"PosZ\" : 371.6975097656250,\n   \"RotW\" : 0.8804783821105957,\n   \"RotX\" : -0.3647039830684662,\n   \"RotY\" : 0.2798439562320709,\n   \"RotZ\" : 0.1159144788980484,\n   \"Zoom\" : 1.250\n}\n"}"
        *    }
        *}
        **/
        static walkThroughShow(posx, posy, posz, rotw, rotx, roty, rotz, zoom, des) {
            //组织数据json
            let camera = {};
            camera["PosX"] = posx;
            camera["PosY"] = posy;
            camera["PosZ"] = posz;
            camera["RotW"] = rotw;
            camera["RotX"] = rotx;
            camera["RotY"] = roty;
            camera["RotZ"] = rotz;
            camera["Zoom"] = zoom;
            let params = {};
            params["Operator"] = "Show";
            params["Camera"] = JSON.stringify(camera);
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "WalkThrough";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        static walkThroughClose(posx, posy, posz, rotw, rotx, roty, rotz, zoom, direction) {
            //组织数据json
            let camera = {};
            camera["PosX"] = posx;
            camera["PosY"] = posy;
            camera["PosZ"] = posz;
            camera["RotW"] = rotw;
            camera["RotX"] = rotx;
            camera["RotY"] = roty;
            camera["RotZ"] = rotz;
            camera["Zoom"] = zoom;
            let params = {};
            params["Operator"] = "Close";
            params["Camera"] = JSON.stringify(camera);
            let macros = {};
            macros["Description"] = direction;
            macros["Name"] = "WalkThrough";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        static walkThroughSetParameters(UpDirection, Angle, direction) {
            //组织数据json
            let params = {};
            params["Operator"] = "SetParameters";
            params["UpDirection"] = UpDirection + ""; //需要改成string
            params["Angle"] = Angle + ""; //需要改成string
            let macros = {};
            macros["Description"] = direction;
            macros["Name"] = "WalkThrough";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************实体模式显示相关******************************************/
        /**设置实体模式显示
        *{
        *     "SViewMacros": {
        *         "Description": "设置实体模式显示",
        *         "Name": "SetRenderMode",
        *         "Parameters": "{"Operator":"SetMode","Mode":4}"
        *     }
        * }
        **/
        static setRenderModeSetMode(mode, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "SetMode";
            params["Mode"] = mode;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "SetRenderMode";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************动画相关******************************************/
        //播放动画
        static animationPlay(PlaySpeed, IsPlayCamera, Loop, PlayPercent, SingleStep, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "Play";
            params["IsPlayCamera"] = IsPlayCamera;
            params["PlaySpeed"] = PlaySpeed;
            params["Loop"] = Loop;
            params["PlayPercent"] = PlayPercent;
            params["SingleStep"] = SingleStep;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Animation";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //退出播放动画
        static animationStop(des) {
            //组织数据json
            let params = {};
            params["Operator"] = "Stop";
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Animation";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //暂停播放动画
        static animationPause(des) {
            //组织数据json
            let params = {};
            params["Operator"] = "Pause";
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Animation";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //设置动画参数
        static animationSetParameters(playPercent, Loop, SingleStep, PlaySpeed, IsPlayCamera, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "SetParameters";
            if (playPercent !== null && typeof playPercent !== "undefined") {
                params["PlayPercent"] = Number(playPercent);
            }
            if (Loop !== null && typeof Loop !== "undefined") {
                params["Loop"] = (Loop == "true");
            }
            if (SingleStep !== null && typeof SingleStep !== "undefined") {
                params["SingleStep"] = (SingleStep == "true");
            }
            if (PlaySpeed !== null && typeof PlaySpeed !== "undefined") {
                params["PlaySpeed"] = Number(PlaySpeed);
            }
            if (IsPlayCamera !== null && typeof IsPlayCamera !== "undefined") {
                params["IsPlayCamera"] = (IsPlayCamera == "true");
            }
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Animation";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************手势批注相关******************************************/
        //添加手势批注
        static GestureNoteAddGestureNote(isShowSpacePoint, noteStr, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "AddGestureNote";
            params["NoteStr"] = noteStr;
            params["IsShowSpacePoint"] = isShowSpacePoint;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "GestureNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************文本批注相关******************************************/
        //添加文本批注
        static TextNoteAddTextNote(noteStr, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "AddTextNote";
            params["NoteStr"] = noteStr;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "TextNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //更新文本批注
        static TextNoteEditTextNote(guid, textStr, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "EditTextNote";
            params["TextStr"] = textStr;
            params["Guid"] = guid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "TextNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //删除文本批注
        static TextNoteDeleteTextNote(guid, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "DeleteTextNote";
            params["Guid"] = guid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "TextNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //更新文本批注
        static TextNoteUpdatePosition(guid, x, y, z, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "UpdatePosition";
            params["Guid"] = guid;
            params["X"] = x;
            params["Y"] = y;
            params["Z"] = z;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "TextNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //选择批注
        static TextNoteSelectTextNote(guid, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "SelectTextNote";
            params["Guid"] = guid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "TextNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************序号批注相关******************************************/
        //添加序号批注
        static SequenceNoteAddSequenceNote(noteStr, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "AddSequenceNote";
            params["NoteStr"] = noteStr;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "SequenceNote";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************测量相关******************************************/
        //添加测量
        static MeasureAddMeasure(measureStr, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "AddMeasure";
            params["MeasureStr"] = measureStr;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Measure";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //删除测量
        static MeasureDeleteMeasure(guid, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "DeleteMeasure";
            params["Guid"] = guid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Measure";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //更新测量位置
        static MeasureUpdatePosition(guid, x, y, z, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "UpdatePosition";
            params["Guid"] = guid;
            params["X"] = x;
            params["Y"] = y;
            params["Z"] = z;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Measure";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //选择测量
        static MeasureSelectMeasure(guid, des) {
            //组织数据json
            let params = {};
            params["Operator"] = "SelectMeasure";
            params["Guid"] = guid;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Measure";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        static MeasureClearMeasure(type, des) {
            let params = {};
            params["Operator"] = "Clear";
            params["Type"] = type;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Measure";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************爆炸相关******************************************/
        //显示爆炸
        static ExplosionShow(percent, etype, des, paths, level = -1) {
            //组织数据json
            let params = {};
            params["Operator"] = "Show";
            params["Percent"] = percent;
            params["Type"] = etype;
            params["Level"] = level;
            params["Paths"] = paths;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Explosion";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //退出爆炸
        static ExplosionClose(des) {
            //组织数据json
            let params = {};
            params["Operator"] = "Close";
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "Explosion";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        /**************************************剖切相关******************************************/
        //剖切
        static MSection(Show, DirectionX, PercentageX, DirectionY, PercentageY, DirectionZ, PercentageZ, ShowClipPlane, ShowCutPlane, ReverseClip, isSelectClip, selectModels, des) {
            //组织数据json
            let params = {};
            params["Show"] = Show;
            params["DirectionX"] = DirectionX;
            params["PercentageX"] = PercentageX;
            params["DirectionY"] = DirectionY;
            params["PercentageY"] = PercentageY;
            params["DirectionZ"] = DirectionZ;
            params["PercentageZ"] = PercentageZ;
            params["ShowClipPlane"] = ShowClipPlane;
            params["ShowCutPlane"] = ShowCutPlane;
            params["ReverseClip"] = ReverseClip;
            params["UseSelected"] = isSelectClip;
            params["SelectedClipModels"] = selectModels;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "MSection";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
        //清空剖切
        static MSectionClose(des) {
            //组织数据json
            let params = {};
            params["Show"] = false;
            let macros = {};
            macros["Description"] = des;
            macros["Name"] = "MSection";
            macros["Parameters"] = JSON.stringify(params);
            let jsonObj = {};
            jsonObj["SViewMacros"] = macros;
            let jsonStr = JSON.stringify(jsonObj);
            return jsonStr;
        }
    }
    Synergy.SynergyMessages = SynergyMessages;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class ParseMessageUtill {
        /**************************************摄相机相关******************************************/
        /**
         * 解析接收到的关于相机的指令
         * @param parameters
         */
        static parseCameraMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            let state = JSON.parse(jsonPara.State);
            switch (operator) {
                case "GetCamera":
                    break;
                case "SetCamera":
                    let posX = state.PosX;
                    let posY = state.PosY;
                    let posZ = state.PosZ;
                    let rotW = state.RotW;
                    let rotX = state.RotX;
                    let rotY = state.RotY;
                    let rotZ = state.RotZ;
                    let zoom = state.Zoom;
                    break;
            }
        }
        /**************************************模型相关******************************************/
        /**
         * 解析接收到的关于模型的指令
         * @param sview
         * @param parameters
         */
        static ParseShapeMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            let path = jsonPara.Path;
            switch (operator) {
                case "SelectShape":
                    {
                    }
                    break;
                case "UnSelectShape":
                    {
                    }
                    break;
                case "ClearSelector":
                    {
                    }
                    break;
                case "RestoreShape": //复位模型
                    {
                    }
                    break;
                case "SetShapeVisiable": //隐藏模型
                    {
                    }
                    break;
                case "SetShapeTransparent": //设置模型透明
                    {
                    }
                    break;
                case "SetShapeColor": //设置模型颜色
                    {
                    }
                    break;
                case "SetShapeTransform": //移动模型
                    {
                    }
                    break;
                case "OnlyShow": //单独显示
                    {
                    }
                    break;
            }
        }
        /**************************************视图相关******************************************/
        /**
         * 解析接收到的关于视图的指令
         * @param parameters
         */
        static ParseViewMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "RestoreView": //复位
                    {
                    }
                    break;
                case "SetView": //切换视图
                    {
                    }
                    break;
                case "ChangeView": //切换视图
                    {
                    }
                    break;
                case "CreateView":
                    {
                    }
                    break;
                case "RenameView":
                    {
                    }
                    break;
                case "DeleteView":
                    {
                    }
                    break;
            }
        }
        /**************************************漫游相关******************************************/
        /**
         * 解析接收到的关于漫游的指令
         * @param parameters
         */
        static parseWalkThroughMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "Show":
                    {
                    }
                    break;
                case "SetParameters":
                    {
                    }
                    break;
                case "Close":
                    {
                    }
                    break;
            }
        }
        /**************************************实体模式显示相关******************************************/
        static parseSetRenderModeMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "SetMode":
                    {
                    }
                    break;
            }
        }
        /**************************************Menu菜单相关******************************************/
        static parseMenuMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "ExchangeHideShow": //显隐交换
                    {
                    }
                    break;
            }
        }
        /**************************************手势批注相关******************************************/
        static parseGestureNoteMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "AddGestureNote": //添加手势批注
                    {
                    }
                    break;
            }
        }
        /**************************************文本批注相关******************************************/
        static parseTextNoteMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "AddTextNote": //添加文本批注
                    {
                    }
                    break;
                case "EditTextNote": //编辑文本批注
                    {
                    }
                    break;
                case "DeleteTextNote": //删除文本批注
                    {
                    }
                    break;
                case "SelectTextNote": //选择批注
                    {
                    }
                    break;
                case "UpdatePosition": //更新显示位置
                    {
                    }
                    break;
            }
        }
        /**************************************序号批注相关******************************************/
        static parseSequenceNoteMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "AddSequenceNote": //添加序号批注
                    {
                    }
                    break;
                case "EditSequenceNote": //编辑序号批注
                    {
                    }
                    break;
                case "DeleteSequenceNote": //删除序号批注
                    {
                    }
                    break;
                case "UpdatePosition": //更新序号批注
                    {
                    }
                    break;
            }
        }
        /**************************************Annotation相关******************************************/
        static parseAnnotationMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "SelectTextNote": //Select annotation
                    {
                    }
                    break;
            }
        }
        /**************************************剖切相关******************************************/
        static parseMSectionMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            let show = jsonPara.Show;
            let DirectionX = jsonPara.DirectionX;
            let PercentageX = Number(jsonPara.PercentageX) / 100;
            let DirectionY = jsonPara.DirectionY;
            let PercentageY = Number(jsonPara.PercentageY) / 100;
            let DirectionZ = jsonPara.DirectionZ;
            let PercentageZ = Number(jsonPara.PercentageZ) / 100;
            let ShowClipPlane = jsonPara.ShowClipPlane;
            let ShowCutPlane = jsonPara.ShowCutPlane;
            let ReverseClip = jsonPara.ReverseClip;
            let UseSelected = jsonPara.UseSelected;
            let SelectedClipModels = jsonPara.SelectedClipModels;
        }
        /**************************************爆炸相关******************************************/
        static parseExplosionMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "Show": //打开爆炸图
                    {
                        let etype = jsonPara.Type;
                        let Percent = jsonPara.Percent;
                        let level = jsonPara.Level;
                        let paths = jsonPara.Paths;
                        if (!paths) {
                            paths = [];
                        }
                    }
                    break;
                case "Close": //复位模型
                    {
                    }
                    break;
            }
        }
        /**************************************动画相关******************************************/
        static parseSetAnimationMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "Play":
                    {
                    }
                    break;
                case "Pause":
                    {
                    }
                    break;
                case "SetParameters":
                    {
                    }
                    break;
                case "Stop":
                    {
                    }
                    break;
            }
        }
        /**************************************测量相关******************************************/
        static parseSetMeasureMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "AddMeasure":
                    {
                    }
                    break;
                case "UpdatePosition":
                    {
                    }
                    break;
                case "DeleteMeasure":
                    {
                    }
                    break;
                case "SelectMeasure": //选择测量
                    {
                    }
                    break;
                case "Clear":
                    {
                    }
                    break;
            }
        }
        /**************************************视图相关******************************************/
        static parseModelViewMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "ChangeView":
                    {
                    }
                    break;
            }
        }
        /**************************************文件相关******************************************/
        static parseFileMsg(parameters) {
            let jsonPara = JSON.parse(parameters);
            let operator = jsonPara.Operator;
            switch (operator) {
                case "Open":
                    {
                        console.log("-------------执行打开文件--------------");
                    }
                    break;
                case "Close":
                    {
                        console.log("-------------执行关闭文件--------------");
                    }
                    break;
            }
        }
    }
    Synergy.ParseMessageUtill = ParseMessageUtill;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class Receive {
        /**
         * 收到群组资料已变更的通知
         * @param message
         *  {
         *     "SViewMacros": {
         *          "Description": "打开文件",
         *          "Name": "File",
         *          "Parameters": "{"Operator":"Open","FileUrl":"http:\/\/ser......cn\/7.2\/file\/downloadsample?modelid=e86b6107-0e4d-4f58-9770-2abc7076315a"}"
         *      }
         *  }
         */
        static receiveGroupProfileHasChanged(message) {
            console.log("--------------收到群公告更新的通知----------------");
            let notification = message.payload.newGroupProfile.notification; //群公告
            if (notification != null && notification.length > 0) {
                let fromuserid = message.payload.operatorID;
                let attendee = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.idPre + fromuserid);
                let nick = "";
                if (attendee) {
                    nick = attendee.getLabelText();
                }
                if (nick == "" || nick.length == 0 || nick == 'undefind') {
                    nick = "user_" + fromuserid;
                }
                //var nick = $(".group_control_panel_body_attendees_item[data-userid='" + fromuserid + "'] .attendees_item_name").html();
                //if (nick == "" || nick.length == 0) {
                //    nick = "user_" + fromuserid;
                //}
                //清空视图列表
                //$("#sview_view_tree").html("");
                // TODO
                Synergy.SynergyMessages.parseMsg(notification, nick);
            }
        }
        /**
         * 收到会议群组解散的通知
         * @param groupid
         */
        static receiveGroupHasDismissed(groupid) {
            //只有退出的是当前会议时，才往下执行，避免服务端自动退会时的问题
            if (Synergy.Main.instance().getTimGroup().getGroupID() == groupid) {
                //console.log("----------------收到群解散的通知------------------");
                //createTipWindow("", getLocalizedString(language, "SynergyGroup", "HostEndTheMeeting"));
                Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.HostEndTheMeeting);
                Synergy.SynergyTRTC.instance().leave();
                /**
                 * 处理界面显隐
                 * **/
                //initGroup(true, false);
                Synergy.UiManager.initGroupDialogs(true, false);
            }
        }
        /**
         * 收到被设置为管理员的消息处理(允许三维操作)
         * @param message
         */
        static receiveAddAdminRoleMsg(message) {
            //console.log("----------------收到被设置为管理员的通知------------------");
            if (Synergy.Main.instance().getMyProfile().getRole() == Synergy.SYNERGY_MEMBER_ROLE_MEMBER) {
                Synergy.Main.instance().getMyProfile().setRole(Synergy.SYNERGY_MEMBER_ROLE_ADMIN, true);
                Synergy.UiManager.initAdminRoleInterface();
            }
        }
        /**
         * 收到被取消管理员的消息处理(禁止三维操作)
         * @param message
         */
        static receiveBackoutAdminRoleMsg(message) {
            //console.log("----------------收到被取消管理员的通知------------------");
            if (Synergy.Main.instance().getMyProfile().getRole() == Synergy.SYNERGY_MEMBER_ROLE_ADMIN) {
                Synergy.Main.instance().getMyProfile().setRole(Synergy.SYNERGY_MEMBER_ROLE_MEMBER, false);
                Synergy.UiManager.initAdminRoleInterface();
            }
        }
        /**
         *    收到文本消息,进行处理
         **/
        static receiveTextMsg(message) {
            //let sendusernick = message.nick;
            let sendtime = message.time;
            let fromUserId = message.from;
            let attendee = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.idPre + fromUserId);
            let sendusernick = "";
            if (attendee) {
                sendusernick = attendee.getLabelText();
            }
            if (sendusernick == "" || sendusernick.length == 0 || sendusernick == 'undefind') {
                sendusernick = "user_" + fromUserId;
            }
            let textcontent = message.payload.text;
            //处理申请操作权限的申请
            if (textcontent == Synergy.SYNERGY_COMMAND_APPLY_ROLE) {
                //console.log("---------------------处理申请操作权限的申请--------------------");
                /**
                 *  1.添加左下角申请操作权限弹窗
                 *
                 **/
                let applyTip = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipItemConstant.operateIdPre + fromUserId);
                if (applyTip) {
                    return;
                }
                else {
                    let applyTipItem = JSON.parse(JSON.stringify(Synergy.Dialog.ApplyTipItem));
                    //let applyTipItem = Synergy.Dialog.ApplyTipItem;
                    applyTipItem.PopMessage.id = Synergy.Dialog.ApplyTipItemConstant.operateIdPre + fromUserId;
                    applyTipItem.PopMessage.content = sendusernick + Synergy.Dialog.ApplyTipItemConstant.operateContentSuffix;
                    let applyTipList = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id);
                    if (!applyTipList.isShow()) {
                        Synergy.UiManager.openApplyTipList();
                    }
                    //添加点击事件及按钮id
                    //同意按钮
                    applyTipItem.PopMessage.subComponents[0].Button.id = Synergy.Dialog.ApplyTipItemConstant.operateAgreeBtnIdPre + fromUserId;
                    applyTipItem.PopMessage.subComponents[0].Button.onClick = "Synergy.Main.instance().allowMember3DOperate(" + fromUserId + ",true)";
                    //已同意按钮
                    applyTipItem.PopMessage.subComponents[1].Button.id = Synergy.Dialog.ApplyTipItemConstant.operateAgreedlBtnIdPre + fromUserId;
                    //添加节点
                    applyTipList.addSubComponents([applyTipItem]);
                    this.bindCommandItemSlideUp(Synergy.Dialog.ApplyTipItemConstant.operateIdPre + fromUserId);
                }
                /**
                 *  2.申请人列表添加记录
                 **/
                let apply = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyItem.idPre + fromUserId);
                if (apply == null) {
                    //添加
                    let apply_item = JSON.parse(JSON.stringify(Synergy.Dialog.MeetingApplyItem));
                    //let apply_item = Synergy.Dialog.MeetingApplyItem;
                    apply_item.Item.id = Synergy.Dialog.ApplyItem.idPre + fromUserId;
                    apply_item.Item.text = sendusernick;
                    //根据当前申请人的权限状态，初始化申请人列表item图标显隐
                    //如果允许操作按钮是隐藏的，即当前状态为允许操作状态
                    if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + fromUserId).isShow()) {
                        //显示禁止操作按钮
                        apply_item.Item.subComponents[3].Button.isShow = true;
                    }
                    else {
                        //显示允许操作按钮
                        apply_item.Item.subComponents[2].Button.isShow = true;
                    }
                    //如果开启音视频按钮是隐藏的，即当前状态为开启音视频权限状态
                    if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.UnmuteVagBtnPre + fromUserId).isShow()) {
                        //显示取消音视权限频按钮
                        apply_item.Item.subComponents[0].Button.isShow = true;
                    }
                    else {
                        //显示开启音视频按钮
                        apply_item.Item.subComponents[1].Button.isShow = true;
                    }
                    //关闭音视频按钮
                    apply_item.Item.subComponents[0].Button.id = Synergy.Dialog.ApplyItem.applyItemMuteVagBtnPre + fromUserId;
                    apply_item.Item.subComponents[0].Button.onClick = "Synergy.Main.instance().forbidMemberVideoAndAudio(" + fromUserId + ")";
                    //开启音视频权限按钮
                    apply_item.Item.subComponents[1].Button.id = Synergy.Dialog.ApplyItem.applyItemUnMuteVagBtnPre + fromUserId;
                    apply_item.Item.subComponents[1].Button.onClick = "Synergy.Main.instance().allowMemberVideoAndAudio(" + fromUserId + ")";
                    //允许三维操作按钮
                    apply_item.Item.subComponents[2].Button.id = Synergy.Dialog.ApplyItem.applyItemAllowOperateBtnPre + fromUserId;
                    apply_item.Item.subComponents[2].Button.onClick = "Synergy.Main.instance().allowMember3DOperate(" + fromUserId + ")";
                    //禁止三维操作按钮
                    apply_item.Item.subComponents[3].Button.id = Synergy.Dialog.ApplyItem.applyItemBanOperateBtnPre + fromUserId;
                    apply_item.Item.subComponents[3].Button.onClick = "Synergy.Main.instance().forbidMember3DOperate(" + fromUserId + ")";
                    Synergy.Main.instance().getSviewFrame().getUIManager().
                        getElement(Synergy.Dialog.MeetingApplyList.List.id).addSubComponents([apply_item]);
                }
                return;
            }
            //处理申请音视频权限的申请
            if (textcontent == Synergy.SYNERGY_COMMAND_APPLY_VAG) {
                console.log("---------------------处理申请音视频权限的申请--------------------");
                /**
                 *  1.添加左下角申请音视频权限弹窗
                 **/
                let applyTip = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipItemConstant.vagIdPre + fromUserId);
                if (applyTip) {
                    return;
                }
                else {
                    let applyTipItem = JSON.parse(JSON.stringify(Synergy.Dialog.ApplyTipItem));
                    //let applyTipItem = Synergy.Dialog.ApplyTipItem;
                    applyTipItem.PopMessage.id = Synergy.Dialog.ApplyTipItemConstant.vagIdPre + fromUserId;
                    applyTipItem.PopMessage.content = sendusernick + Synergy.Dialog.ApplyTipItemConstant.vagContentSuffix;
                    let applyTipList = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id);
                    if (!applyTipList.isShow()) {
                        Synergy.UiManager.openApplyTipList();
                    }
                    //添加点击事件及按钮id
                    //同意按钮
                    applyTipItem.PopMessage.subComponents[0].Button.id = Synergy.Dialog.ApplyTipItemConstant.vagAgreeBtnIdPre + fromUserId;
                    applyTipItem.PopMessage.subComponents[0].Button.onClick = "Synergy.Main.instance().allowMemberVideoAndAudio(" + fromUserId + ",true)";
                    //已同意按钮
                    applyTipItem.PopMessage.subComponents[1].Button.id = Synergy.Dialog.ApplyTipItemConstant.vagAgreedBtnIdPre + fromUserId;
                    //添加节点
                    applyTipList.addSubComponents([applyTipItem]);
                    this.bindCommandItemSlideUp(Synergy.Dialog.ApplyTipItemConstant.vagIdPre + fromUserId);
                }
                /**
                 *  2.申请人列表添加记录
                 **/
                let apply = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyItem.idPre + fromUserId);
                if (apply == null) {
                    //添加
                    let apply_item = JSON.parse(JSON.stringify(Synergy.Dialog.MeetingApplyItem));
                    //let apply_item = Synergy.Dialog.MeetingApplyItem;
                    apply_item.Item.id = Synergy.Dialog.ApplyItem.idPre + fromUserId;
                    apply_item.Item.text = sendusernick;
                    //根据当前申请人的权限状态，初始化申请人列表item图标显隐
                    //如果允许操作按钮是隐藏的，即当前状态为允许操作状态
                    if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + fromUserId).isShow()) {
                        //显示禁止操作按钮
                        apply_item.Item.subComponents[3].Button.isShow = true;
                    }
                    else {
                        //显示允许操作按钮
                        apply_item.Item.subComponents[2].Button.isShow = true;
                    }
                    //如果开启音视频按钮是隐藏的，即当前状态为开启音视频权限状态
                    if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.UnmuteVagBtnPre + fromUserId).isShow()) {
                        //显示取消音视权限频按钮
                        apply_item.Item.subComponents[0].Button.isShow = true;
                    }
                    else {
                        //显示开启音视频按钮
                        apply_item.Item.subComponents[1].Button.isShow = true;
                    }
                    //关闭音视频按钮
                    apply_item.Item.subComponents[0].Button.id = Synergy.Dialog.ApplyItem.applyItemMuteVagBtnPre + fromUserId;
                    apply_item.Item.subComponents[0].Button.onClick = "Synergy.Main.instance().forbidMemberVideoAndAudio(" + fromUserId + ")";
                    //开启音视频权限按钮
                    apply_item.Item.subComponents[1].Button.id = Synergy.Dialog.ApplyItem.applyItemUnMuteVagBtnPre + fromUserId;
                    apply_item.Item.subComponents[1].Button.onClick = "Synergy.Main.instance().allowMemberVideoAndAudio(" + fromUserId + ")";
                    //允许三维操作按钮
                    apply_item.Item.subComponents[2].Button.id = Synergy.Dialog.ApplyItem.applyItemAllowOperateBtnPre + fromUserId;
                    apply_item.Item.subComponents[2].Button.onClick = "Synergy.Main.instance().allowMember3DOperate(" + fromUserId + ")";
                    //禁止三维操作按钮
                    apply_item.Item.subComponents[3].Button.id = Synergy.Dialog.ApplyItem.applyItemBanOperateBtnPre + fromUserId;
                    apply_item.Item.subComponents[3].Button.onClick = "Synergy.Main.instance().forbidMember3DOperate(" + fromUserId + ")";
                    Synergy.Main.instance().getSviewFrame().getUIManager().
                        getElement(Synergy.Dialog.MeetingApplyList.List.id).addSubComponents([apply_item]);
                }
                return;
            }
            //主持人解除禁言(允许音视频操作)
            if (textcontent == Synergy.SYNERGY_COMMAND_CLOSE_FORBIDDEN) {
                //console.log("---------------------收到主持人（解除禁言）允许音视频操作的命令--------------------");
                /**
                 *  若没有音视频权限，则添加
                 *
                 **/
                if (!Synergy.Main.instance().getMyProfile().getVagAuth()) {
                    let myuserid = Synergy.Main.instance().getMyProfile().getUserID();
                    Synergy.Main.instance().getMyProfile().setVagAuth(true);
                    /**
                     * 更新参会者列表中自己的图标状态
                     */
                    //显示开启本地音频、视频按钮，隐藏申请音视频权限按钮
                    Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.ApplyVagBtnPre + myuserid);
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "EnabledYourAudioAndVideoOperation"));
                    Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.EnabledYourAudioAndVideoOperation);
                }
                return;
            }
            //主持人开启禁言(禁止音视频操作)
            if (textcontent == Synergy.SYNERGY_COMMAND_OPEN_FORBIDDEN) {
                console.log("---------------------收到主持人（开启禁言）禁止音视频操作的命令--------------------");
                /**
                 *  若有音视频权限，则禁止
                 *
                 **/
                if (Synergy.Main.instance().getMyProfile().getVagAuth()) {
                    let myuserid = Synergy.Main.instance().getMyProfile().getUserID();
                    Synergy.Main.instance().getMyProfile().setVagAuth(false);
                    /**
                     * 更新参会者列表中及左上角音视频窗口自己的图标状态
                     */
                    //隐藏参会者列表中打开、关闭本地音频、本地视频按钮,显示申请音视频按钮
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteAudioBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteVideoBtnPre + myuserid);
                    Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.ApplyVagBtnPre + myuserid);
                    //更新左上角音视频窗口自己的图标状态,显示开启本地音频视频按钮，隐藏关闭音频视频按钮
                    Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenAudioBtn.Button.id);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseAudioBtn.Button.id);
                    Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenVideoBtn.Button.id);
                    Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseVideoBtn.Button.id);
                    /**
                     * 取消推送关闭本地视频流
                     */
                    if (Synergy.SynergyTRTC.instance().isPublish()) {
                        Synergy.SynergyTRTC.instance().unpublish().then(function () {
                            if (!Synergy.SynergyTRTC.instance().isPublish()) {
                                if (Synergy.SynergyTRTC.instance().getLocalStream() != null) {
                                    if (Synergy.SynergyTRTC.instance().getLocalStream().isPlaying_) {
                                        Synergy.SynergyTRTC.instance().getLocalStream().stop();
                                        Synergy.SynergyTRTC.instance().getLocalStream().close();
                                    }
                                    Synergy.SynergyTRTC.instance().setLocalStream(null);
                                }
                            }
                        });
                    }
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "ClosedYourAudioAndVideoOperation"));
                    Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ClosedYourAudioAndVideoOperation);
                }
                return;
            }
            //执行三维协同操作
            if (textcontent.indexOf("SViewMacros") > -1) {
                //console.log("--------收到三维操作指令------执行三维操作----" + textcontent);
                /**
                 * 将命令传递到命令模块解析并执行三维操作
                 */
                Synergy.SynergyMessages.parseMsg(textcontent, sendusernick);
                return;
            }
            //排除以上几种情况，则处理文本聊天信息
            let msgText = message.payload.text;
            if (msgText.indexOf("\r\n") > 0) {
                msgText = msgText.replace(/\r\n/g, "<br/>");
            }
            if (msgText.indexOf("\n") > 0) {
                msgText = msgText.replace(/\n/g, "<br/>");
            }
            let leftChatItem = JSON.parse(JSON.stringify(Synergy.Dialog.LeftChatItem));
            //let leftChatItem = Synergy.Dialog.LeftChatItem;
            leftChatItem.Item.name = sendusernick;
            leftChatItem.Item.text = msgText;
            leftChatItem.Item.time = Synergy.Utill.formatDate(sendtime);
            //let LeftChatItem = {
            //    Item: {
            //        itemType: "leftChatItem",
            //        name: sendusernick,
            //        text: msgText,
            //        imgsrc: "images/meeting/sview_conference_chart_other.png",
            //        time: Synergy.Main.formatDate(sendtime)
            //    }
            //}
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatList.List.id).addSubComponents([leftChatItem]);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatDialogInput.Input.id).setValue("");
            let chatDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatDialog.Dialog.id);
            if (!chatDialog.isShow()) {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.badgeId);
            }
            /**
             * 滚动到聊天框底端
             * **/
            //console.log("--------收到聊天信息----------" + textcontent);
            Synergy.AlertUtill.toast(sendusernick + ":" + message.payload.text);
        }
        /**
         * 绑定左下角申请提示轮播效果
         * @param applyTipItemId
         */
        static bindCommandItemSlideUp(applyTipItemId) {
            let fistItem = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id).allSubComponents.values().next().value;
            if (fistItem.getId() == applyTipItemId) {
                setTimeout(function () {
                    Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(applyTipItemId);
                    if (Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id).size() == 0) {
                        Synergy.UiManager.closeApplyTipList();
                    }
                }, 2000);
            }
            else {
                setTimeout(function () { Synergy.Receive.bindCommandItemSlideUp(applyTipItemId); }, 1000);
            }
        }
        /**
         * 收到图片文件的处理
         * @param message
         */
        static receiveImageMsg(message) {
            console.log("--------收到图片附件信息并处理----------");
            let sendusernick = message.nick;
            let sendtime = message.time;
            let fromUserId = message.from;
            if (!sendusernick) {
                sendusernick = "user_" + fromUserId;
            }
            /**
             *  附件聊天框中，添加附件记录，并自动滚动到底端
             **/
            let meetingControlAttachmentItem = Synergy.Dialog.MeetingControlAttachmentItem;
            let imgurl = message.payload.imageInfoArray[0].url;
            meetingControlAttachmentItem.Item.id = message.ID;
            meetingControlAttachmentItem.Item.name = sendusernick;
            meetingControlAttachmentItem.Item.time = Synergy.Utill.formatDate(sendtime);
            meetingControlAttachmentItem.Item.imgsrc = imgurl;
            meetingControlAttachmentItem.Item.onClick = "Synergy.Main.instance().showImgDialog('" + message.payload.imageInfoArray[1].url + "')";
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlAttachmentList.List.id).addSubComponents([meetingControlAttachmentItem]);
        }
        /**
         * 收到其他参会人参会的通知
         * 1、获取参会者成员信息，遍历
         * 2、我是群主时，根据当前会议状态判断是否给客户端发送禁止操作及禁言指令，并初始化参会者item按钮显隐
         * 3、更新会议人数
         * @param userids
         */
        static receiveMembersJoin(userids) {
            let that = this;
            Synergy.SynergyTim.instance().getGroupMemberProfile(Synergy.Main.instance().getTimGroup().getGroupID(), userids).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    let memberList = retdata.attendees;
                    let membertemp;
                    for (let i = 0; i < memberList.length; i++) {
                        let groupMember = memberList[i];
                        membertemp = JSON.parse(JSON.stringify(Synergy.Dialog.MeetingControlAttendeeItem));
                        let memuserid = groupMember.userID;
                        let membernick = groupMember.nick;
                        let myRole = Synergy.Main.instance().getMyProfile().getRole();
                        //加入会议的默认都是普通成员，故不判断其是群主及管理员的情况
                        switch (myRole) {
                            /**
                             * 权限判断
                             * **/
                            case Synergy.SYNERGY_MEMBER_ROLE_OWNER:
                                /**
                                 * 如果允许全员操作按钮为隐藏状态,则目前为允许全员操作
                                 * **/
                                if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlAllowAllMembersBtn.Button.id).isShow()) {
                                    //目前为允许全员操作,显示参会者禁止操作按钮
                                    membertemp.Item.subComponents[8].Button.isShow = true;
                                    //延迟2秒发送是为了等待参会者列表加载完毕
                                    setTimeout(function () {
                                        Synergy.SynergyTim.instance().setGroupMemberRole(Synergy.Main.instance().getTimGroup().getGroupID(), memuserid, TIM.TYPES.GRP_MBR_ROLE_ADMIN).then(function (data) {
                                            let retdata = eval(data);
                                            if (retdata.code == '0') {
                                                console.log("---------- 修改群成员角色成功------");
                                            }
                                            else {
                                                console.error(" 修改群成员角色失败-----" + retdata);
                                            }
                                        }).catch(function (errordata) {
                                            let retdata = eval(errordata);
                                            console.error(" 修改群成员角色失败-----" + retdata);
                                        });
                                    }, 2000);
                                }
                                else {
                                    //目前为禁止全员操作，显示参会者允许操作按钮
                                    membertemp.Item.subComponents[7].Button.isShow = true;
                                }
                                /**
                                 * 如果禁言（禁止音视频）按钮为隐藏状态,则目前为全员禁言状态
                                 * **/
                                if (!Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlMuteAllMembersBtn.Button.id).isShow()) {
                                    //目前为全员禁言状态，显示参会者解除禁言（允许音视频）按钮
                                    membertemp.Item.subComponents[6].Button.isShow = true;
                                    //保存参会者信息，主要用于初始化参会列表时参会者的音视频权限
                                    Synergy.Main.instance().getTimGroup().saveAttendee(Synergy.Main.instance().getTimGroup().createAttendee(memuserid, membernick, false));
                                    //延迟2秒发送是为了等待参会者列表加载完毕
                                    let message = Synergy.SynergyTim.instance().createTextMessage(memuserid, Synergy.SYNERGY_COMMAND_OPEN_FORBIDDEN, TIM.TYPES.CONV_C2C);
                                    setTimeout(function () {
                                        Synergy.SynergyTim.instance().sendMsg(message).then(function (data) {
                                            let retdata = eval(data);
                                            if (retdata.code == '0') {
                                                console.log("---------- 发送禁止音视频指令成功------");
                                            }
                                            else {
                                                console.error(" 发送禁止音视频指令失败-----" + retdata);
                                            }
                                        }).catch(function (errordata) {
                                            let retdata = eval(errordata);
                                            console.error(" 发送禁止音视频指令失败-----" + retdata);
                                        });
                                    }, 2000);
                                }
                                else {
                                    //目前为解除全员禁言状态，由于参会者默认是解除禁言的，所以此时没必要发送指令
                                    //显示禁言按钮
                                    membertemp.Item.subComponents[5].Button.isShow = true;
                                    //保存参会者信息，主要用于初始化参会列表时参会者的音视频权限
                                    Synergy.Main.instance().getTimGroup().saveAttendee(Synergy.Main.instance().getTimGroup().createAttendee(memuserid, membernick, true));
                                }
                                break;
                            case Synergy.SYNERGY_MEMBER_ROLE_ADMIN:
                                break;
                            case Synergy.SYNERGY_MEMBER_ROLE_MEMBER:
                                break;
                        }
                        /**
                         * 添加参会者记录
                         * **/
                        Synergy.UiManager.initAttendeeItem(membertemp, memuserid, membernick);
                        Synergy.Main.instance().getSviewFrame().getUIManager().
                            getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).addSubComponents([membertemp]);
                        //createTipWindow(membernick, getLocalizedString(language, "SynergyGroup", "Join"));
                        Synergy.AlertUtill.toast(membernick + ":" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Join);
                    }
                    /**
                     * 更新右上角参会者数量
                     * **/
                    //let badgeBtn = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.badgeId);
                    //let prenum = badgeBtn.getContent();
                    //badgeBtn.setContent(Number(prenum) + 1);
                    Synergy.UiManager.updateGroupMemberNum(1);
                }
                else {
                    //console.error("获取指定群成员资料失败-----" + retdata);
                    Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                //console.error("获取指定群成员资料失败-----" + retdata);
                Synergy.AlertUtill.alert("", retdata.msg, Synergy.Dialog.AlertDialogType.ERROR);
            });
        }
        /**
        * 收到参会人退会的通知
        * @param userids
        */
        static receiveMembersQuit(userids) {
            for (let i = 0; i < userids.length; i++) {
                let memberuserid = userids[i];
                let membernick = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.idPre + memberuserid).getLabelText();
                /**
                 * 移除参会者列表及申请列表里的记录
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.AttendeeItem.idPre + memberuserid);
                Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.ApplyItem.idPre + memberuserid);
                Synergy.Main.instance().getTimGroup().removeAttendee(memberuserid);
                //createTipWindow(attendee.find(".attendees_item_name").html(), getLocalizedString(language, "SynergyGroup", "ExitGroup"));
                Synergy.AlertUtill.toast(membernick + ":" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.ExitGroup);
            }
            /**
             * 更新右上角参会者数量
             * **/
            //let badgeBtn = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.badgeId);
            //let prenum = badgeBtn.getContent();
            //badgeBtn.setContent(Number(prenum) - userids.length);
            Synergy.UiManager.updateGroupMemberNum(-(userids.length));
        }
        /*******************************************以下是音视频的接收消息处理************************************************************/
        /**
         * 接收到本地音频轨道已经关闭的通知
         * */
        static localAudioTrackHasEnded() {
            //console.log("---------音视频轨道已被关闭,例如麦克风设备被拔出或者故障----------");
            if (Synergy.Main.instance().getMyProfile().getVagAuth()) { //再次判断，防止主持人突然禁止音视频后的界面错误
                /**
                 * 切换按钮显隐
                 * 1、切换左上角视频框按钮显隐
                 * 2、切换参会者列表自己的音视频按钮显隐
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseAudioBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenAudioBtn.Button.id);
                let myuserid = Synergy.Main.instance().getMyProfile().getUserID();
                //显示打开音频按钮
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + myuserid);
                //隐藏关闭音频按钮
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteAudioBtnPre + myuserid);
                //移除音频轨道
                Synergy.SynergyTRTC.instance().muteLocalAudio();
                //createTipWindow("", getLocalizedString(language, "SynergyGroup", "UnableToGetAudioAndVideoData"));
                Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.UnableToGetAudioAndVideoData);
            }
        }
        /**
         * 接收到本地视频轨道已经关闭的通知
         * */
        static localVidioTrackHasEnded() {
            //console.log("---------音视频轨道已被关闭,例如摄像头设备被拔出或者故障----------");
            if (Synergy.Main.instance().getMyProfile().getVagAuth()) { //再次判断，防止主持人突然禁止音视频后的界面错误
                /**
                 * 打开遮罩
                 * 切换按钮显隐
                 * 1、切换左上角视频框按钮显隐
                 * 2、切换参会者列表自己的音视频按钮显隐
                 *
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseVideoBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenVideoBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItem.VideoItem.id + Synergy.Dialog.VideoItem.maskSuffix);
                let myuserid = Synergy.Main.instance().getMyProfile().getUserID();
                //显示打开视频按钮
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + myuserid);
                //隐藏关闭视频按钮
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttendeeItem.MuteVideoBtnPre + myuserid);
                //移除视频轨道
                Synergy.SynergyTRTC.instance().removeVideoTrack();
                Synergy.AlertUtill.toast(SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.UnableToGetAudioAndVideoData);
            }
        }
        /**
         *  接收到远端流增加的通知
         */
        static receiveRemoteStreamAdded(event) {
            console.log("---------接收到远端流增加的通知----------");
            let remoteStream = event.stream;
            let remoteStreamId = remoteStream.getId();
            let remoteStreamUserId = remoteStream.getUserId();
            console.log('远端流增加: ' + remoteStream.getId());
            //订阅远端流
            Synergy.SynergyTRTC.instance().getClient().subscribe(remoteStream);
        }
        /**
        *  接收到本地订阅远端流的通知
        */
        static receiveLocalStreamSubscribed(event) {
            console.log("-------------接收到本地订阅远端流的通知--------------------");
            let remoteStream = event.stream;
            let remoteStreamId = remoteStream.getId();
            let remoteStreamUserId = remoteStream.getUserId();
            let usernickname = "";
            let attendee = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.idPre + remoteStreamUserId);
            if (attendee) {
                usernickname = attendee.getLabelText();
            }
            if (usernickname == "" || usernickname.length == 0 || usernickname == 'undefind') {
                usernickname = "user_" + remoteStreamUserId;
            }
            console.log('远端流订阅成功：' + remoteStream.getId());
            if (Synergy.Main.instance().getMyProfile().getUserID() != remoteStreamUserId) {
                if (Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId) == null) {
                    /**
                     * 1、左上角添加远端流播放框
                     * 2、播放远端流
                     *
                     */
                    let remoteVideoItem = JSON.parse(JSON.stringify(Synergy.Dialog.RemoteVideoItem));
                    //let remoteVideoItem = Synergy.Dialog.RemoteVideoItem;
                    remoteVideoItem.VideoItem.id = Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId;
                    remoteVideoItem.VideoItem.name = usernickname;
                    if (remoteStream.hasVideo()) {
                        remoteVideoItem.VideoItem.isShow = true;
                    }
                    Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoDialog.VideoDialog.id).addSubComponents([remoteVideoItem], 1);
                    if (Synergy.Utill.getBroswer().broswer == 'Chrome' &&
                        Synergy.Utill.getBroswer().version < '72') {
                        remoteStream.resume();
                    }
                    // 播放远端流
                    remoteStream.play(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                }
                else {
                    if (remoteStream.hasVideo()) {
                        Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                    }
                }
            }
        }
        /**
         *  接收到远端流移除事件的通知，当远端用户取消发布流后会收到该通知
         */
        static receiveRemoteStreamRemoved(event) {
            console.log("-------------接收到远端流移除事件的通知--------------------");
            let remoteStream = event.stream;
            let remoteStreamUserId = remoteStream.getUserId();
            if (Synergy.Main.instance().getMyProfile().getUserID() != remoteStreamUserId) {
                remoteStream.stop();
                /**
                 *  移除左上角远端流播放框
                 * **/
                if (Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId) != null) {
                    Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                }
            }
        }
        /**
         *  接收到远端流更新事件的通知，当远端用户添加、移除或更换音视频轨道后会收到该通知。
         */
        static receiveRemoteStreamUpdated(event) {
            console.log("-------------接收到远端流更新事件的通知--------------------");
            let remoteStream = event.stream;
            let remoteStreamUserId = remoteStream.getUserId();
            if (Synergy.Main.instance().getMyProfile().getUserID() != remoteStreamUserId) {
                /**
                 *  添加角远端流播放框
                 *  1、如果不存在，则新添加播放框并播放远端流
                 *  2、如果存在播放框，则根据是否有视频轨道判断是否打开遮罩
                 * **/
                if (Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId) == null) {
                    let remoteVideoItem = JSON.parse(JSON.stringify(Synergy.Dialog.RemoteVideoItem));
                    //let remoteVideoItem = Synergy.Dialog.RemoteVideoItem;
                    let usernickname = "user_" + remoteStreamUserId;
                    let attendee = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.AttendeeItem.idPre + remoteStreamUserId);
                    if (attendee) {
                        usernickname = attendee.getLabelText();
                    }
                    if (usernickname == "" || usernickname.length == 0 || usernickname == 'undefind') {
                        usernickname = "user_" + remoteStreamUserId;
                    }
                    remoteVideoItem.VideoItem.id = remoteVideoItem.VideoItem.id + remoteStreamUserId;
                    remoteVideoItem.VideoItem.name = usernickname;
                    if (remoteStream.hasVideo()) {
                        remoteVideoItem.VideoItem.isShow = true;
                    }
                    Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoDialog.VideoDialog.id).addSubComponents([remoteVideoItem], 1);
                    if (Synergy.Utill.getBroswer().broswer == 'Chrome' &&
                        Synergy.Utill.getBroswer().version < '72') {
                        remoteStream.resume();
                    }
                    // 播放远端流
                    remoteStream.play(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                }
                else {
                    if (remoteStream.hasVideo()) {
                        Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                    }
                    else {
                        Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
                    }
                }
            }
        }
        /**
         *  接收到远端用户禁用视频的通知。
         */
        static receiveRemoteStreamMuteVideo(event) {
            console.log("-------------接收到远端用户禁用视频的通知--------------------");
            let remoteStreamUserId = event.userId;
            if (Synergy.Main.instance().getMyProfile().getUserID() != remoteStreamUserId) {
                /**
                 * 隐藏左上角播放框
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
            }
        }
        /**
        *  接收到远端用户启用视频的通知。
        */
        static receiveRemoteStreamUnmuteVideo(event) {
            console.log("-------------接收到远端用户启用视频的通知--------------------");
            let remoteStreamUserId = event.userId;
            if (Synergy.Main.instance().getMyProfile().getUserID() != remoteStreamUserId) {
                /**
                 * 显示左上角播放框
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.VideoItem.remoteVideoIdPre + remoteStreamUserId);
            }
        }
    }
    Synergy.Receive = Receive;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class Common {
    }
    Synergy.Common = Common;
})(Synergy || (Synergy = {}));
/// <reference path="../../../../lib/ThirdParty/tim/trtc.d.ts" />
var Synergy;
(function (Synergy) {
    class SynergyTRTC {
        constructor() {
            this.sdkAppId_ = Synergy.SYNERGY_APPID;
            this.localStream_ = null;
            this.client_ = null;
            this.userId_ = "";
            this.isPublished_ = false;
            this.isInitialize_ = false;
            this.cameraId = "";
            this.micId = "";
        }
        getClient() {
            return this.client_;
        }
        isPublish() {
            return this.isPublished_;
        }
        getLocalStream() {
            return this.localStream_;
        }
        setLocalStream(localStream) {
            this.localStream_ = localStream;
        }
        static instance() {
            if (Synergy.SynergyTRTC.synergytrtc == null) {
                Synergy.SynergyTRTC.synergytrtc = new Synergy.SynergyTRTC();
            }
            return Synergy.SynergyTRTC.synergytrtc;
        }
        /**
         * 创建客户端
         * @param userid 用户id
         * @param usersig 登录凭证
         */
        createClient(userid, usersig) {
            let that = this;
            that.userId_ = userid;
            this.client_ = TRTC.createClient({
                mode: 'rtc',
                sdkAppId: that.sdkAppId_,
                userId: userid,
                userSig: usersig
            });
            while (that.client_ == null) {
                continue;
            }
            that.handleEvents();
        }
        /**
         * 进入音视频房间
         * @param roomid
         */
        join(roomid) {
            let that = this;
            that.client_.join({ roomId: Number(roomid) }).then(function () {
                console.log('----------------进入音视频房间成功-------------------- ');
            }).catch(function (error) {
                console.error('-------------------进入音视频房间失败------------------ ' + error);
            });
        }
        /**
         * 创建本地流
         * 创建一个本地流 Stream 对象，本地流 Stream 对象通过 publish() 方法发布本地音视频流
         * @param audio 是否开启音频
         * @param video 是否开启视频
         */
        createLocalStream(audio, video) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                that.localStream_ = TRTC.createStream({
                    userId: that.userId_,
                    audio: audio,
                    video: video,
                    mirror: true
                });
                //调用 LocalStream.initialize() 初始化本地音视频流。
                that.localStream_.initialize().then(function () {
                    console.log('----------------初始化本地流成功--------------');
                    that.localStream_.play('local_stream');
                    that.isInitialize_ = true;
                    //在本地流初始化成功后，调用 Client.publish() 方法发布本地流。
                    that.localStream_.on('player-state-changed', function (event) {
                        let etype = event.type;
                        let reason = event.reason;
                        switch (etype) {
                            case 'audio':
                                switch (reason) {
                                    case 'ended': //音视频轨道已被关闭,例如麦克风设备被拔出或者故障
                                        Synergy.Receive.localAudioTrackHasEnded();
                                        break;
                                }
                                break;
                            case 'video':
                                switch (reason) {
                                    case 'ended': //音视频轨道已被关闭,例如摄像头设备被拔出或者故障
                                        Synergy.Receive.localVidioTrackHasEnded();
                                        break;
                                }
                                break;
                        }
                    });
                    // 本地流初始化成功，发布本地流
                    console.log("---------------- 本地流初始化成功，发布本地流-------------------");
                    that.publish().then(function (data) {
                        // 本地流发布成功
                        resolve(data);
                    }).catch(function (error) {
                        reject(error.getId);
                    });
                }).catch(function (error) {
                    that.isInitialize_ = false;
                    that.localStream_ = null;
                    // 本地流初始化失败
                    console.error("---------------- 本地流初始化失败-------------------");
                    //switch (error.name) {
                    //    case 'NotReadableError':
                    //        // 提示用户：暂时无法访问摄像头/麦克风，请确保当前没有其他应用请求访问摄像头/麦克风，并重试。
                    //        break;
                    //    default:
                    //        console.error(error);
                    //        break;
                    //}
                    reject(error);
                });
            });
            return p;
        }
        /**
         * 发布本地流
         * */
        publish() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                if (that.isPublished_) {
                    console.log("---------------- 本地流已经发布成功过-------------------");
                    resolve("");
                }
                // 本地流初始化成功，发布本地流
                that.client_.publish(that.localStream_).then(function (data) {
                    // 本地流发布成功
                    console.log("---------------- 本地流发布成功-------------------");
                    that.isPublished_ = true;
                    resolve(data);
                }).catch(function (error) {
                    // 本地流初始化失败
                    console.error("---------------- 本地流发布失败-------------------");
                    that.isPublished_ = false;
                    reject(error);
                });
            });
            return p;
        }
        /**
         * 停止推流
         * */
        unpublish() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                if (that.isPublished_) {
                    that.client_.unpublish(that.localStream_).then(function () {
                        // 取消发布本地流成功
                        console.log("---------------- 取消发布本地流成功-------------------");
                        that.isPublished_ = false;
                        resolve("");
                    });
                }
                else {
                    resolve("");
                }
            });
            return p;
        }
        /**
         * 退出房间
         * */
        leave() {
            let that = this;
            that.unpublish().then(function () {
                if (!that.isPublished_) {
                    that.client_.leave().then(function () {
                        console.log("------------退出音视频房间--------------");
                        if (that.localStream_ != null) {
                            if (that.localStream_.isPlaying_) {
                                that.localStream_.stop();
                                that.localStream_.close();
                            }
                            that.localStream_ = null;
                        }
                    });
                }
            });
        }
        /**
         * 关闭本地音频
         * */
        muteLocalAudio() {
            this.localStream_.muteAudio();
            console.log("---------------- 关闭本地音频成功-------------------");
            //TRTCClient.removeAudioTrack();
        }
        /**
         * 打开本地音频
         * */
        unmuteLocalAudio() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                if (that.localStream_ == null) {
                    that.createLocalStream(true, false).then(function () {
                        console.log("---------------- 打开本地音频成功-------------------");
                        resolve("");
                    }).catch(function (errorName) {
                        console.error("---------------- 打开本地音频失败-------------------");
                        reject(errorName);
                        that.localStream_ = null;
                    });
                }
                else {
                    if (!that.localStream_.getAudioTrack()) {
                        that.addAudioTrack().then(function () {
                            console.log("---------------- 打开本地音频成功-------------------");
                            resolve("");
                        }).catch(function (errorName) {
                            console.error("---------------- 打开本地音频失败-------------------");
                            reject(errorName);
                        });
                    }
                    else {
                        that.localStream_.unmuteAudio();
                        console.log("---------------- 打开本地音频成功-------------------");
                        resolve("");
                    }
                }
            });
            return p;
        }
        /**
         * 关闭视频
         * */
        muteLocalVideo() {
            //muteVideo 并不会关闭摄像头，此时摄像头的灯仍然是亮着，若想完全关闭视频通话，请使用 removeTrack() 方法
            this.localStream_.muteVideo();
            //TRTCClient.removeVideoTrack();
        }
        /**
         * 打开视频
         * */
        unmuteLocalVideo() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                if (that.localStream_ == null) {
                    that.createLocalStream(false, true).then(function () {
                        resolve("");
                    }).catch(function (errorName) {
                        reject(errorName);
                        that.localStream_ = null;
                    });
                }
                else {
                    if (!that.localStream_.getVideoTrack()) {
                        that.addVideoTrack().then(function () {
                            resolve("");
                        }).catch(function (errorName) {
                            reject(errorName);
                        });
                        ;
                    }
                    else {
                        that.localStream_.unmuteVideo();
                        resolve("");
                    }
                }
            });
            return p;
        }
        /**
         * 添加视频轨道
         * */
        addVideoTrack() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let videoTrack = that.localStream_.getVideoTrack();
                if (!videoTrack) {
                    let videoStream = TRTC.createStream({ userId: that.userId_, audio: false, video: true });
                    videoStream.initialize().then(function () {
                        // 增加视频通话
                        that.localStream_.addTrack(videoStream.getVideoTrack()).then(function () {
                            console.log('add video call success');
                        });
                        resolve("");
                    }).catch(function (error) {
                        reject(error.name);
                    });
                }
            });
            return p;
        }
        /**
         * 移除视频轨道
         * */
        removeVideoTrack() {
            let that = this;
            let videoTrack = that.localStream_.getVideoTrack();
            if (videoTrack) {
                that.localStream_.removeTrack(videoTrack).then(function () {
                    console.log('remove video call success');
                    // 关闭摄像头
                    videoTrack.stop();
                });
            }
        }
        /**
         * 添加音频轨道
         * */
        addAudioTrack() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let audioTrack = that.localStream_.getAudioTrack();
                if (!audioTrack) {
                    let audioStream = TRTC.createStream({ userId: that.userId_, audio: true, video: false });
                    audioStream.initialize().then(function () {
                        // 增加音频通话
                        that.localStream_.addTrack(audioStream.getAudioTrack()).then(function () {
                            console.log('add audio call success');
                        });
                        resolve("");
                    }).catch(function (error) {
                        reject(error.name);
                    });
                }
                else {
                    resolve("");
                }
            });
            return p;
        }
        /**
         * 移除音频轨道
         * */
        removeAudioTrack() {
            let that = this;
            //音频不能通过 removeTrack() 删除，如果您想要禁用音频，建议通过 muteAudio() 方法实现。  
            let audioTrack = that.localStream_.getAudioTrack();
            if (audioTrack) {
                that.localStream_.removeTrack(audioTrack).then(function () {
                    // 关闭音频通话成功，停止audioTrack并释放麦克风资源
                    console.log('remove audio call success');
                    audioTrack.stop();
                });
            }
        }
        /**
         *    检测浏览器是否支持音视频功能
         **/
        checkSystemRequirements() {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                TRTC.checkSystemRequirements().then(function (result) {
                    resolve(result);
                }).catch(function (error) {
                    reject(error);
                });
            });
            return p;
        }
        /***
         * 处理事件的绑定
         * */
        handleEvents() {
            let that = this;
            // 解除所有事件绑定
            that.client_.off("*");
            //添加事件绑定
            /**
             * 当远端流增加时触发
             */
            that.client_.on('stream-added', function (event) {
                Synergy.Receive.receiveRemoteStreamAdded(event);
            });
            /**
             * 当本地订阅远端流时触发
             */
            that.client_.on('stream-subscribed', function (event) {
                Synergy.Receive.receiveLocalStreamSubscribed(event);
            });
            /**
             * 远端流移除事件，当远端用户取消发布流后会收到该通知。
             */
            that.client_.on('stream-removed', function (event) {
                Synergy.Receive.receiveRemoteStreamRemoved(event);
            });
            /**
             * 远端流更新事件，当远端用户添加、移除或更换音视频轨道后会收到该通知。
             */
            that.client_.on('stream-updated', function (event) {
                Synergy.Receive.receiveRemoteStreamUpdated(event);
            });
            /**
             * 远端用户禁用音频通知。
             */
            that.client_.on('mute-audio', function (event) {
                console.log("-------------mute-audio--------------------");
            });
            /**
             * 远端用户启用音频通知。
             */
            that.client_.on('unmute-audio', function (event) {
                console.log("-------------unmute-audio--------------------");
            });
            /**
             * 远端用户禁用视频通知。
             */
            that.client_.on('mute-video', function (event) {
                Synergy.Receive.receiveRemoteStreamMuteVideo(event);
            });
            /**
             * 远端用户启用视频通知。
             */
            that.client_.on('unmute-video', function (event) {
                Synergy.Receive.receiveRemoteStreamUnmuteVideo(event);
            });
        }
    }
    /***
     * 单例模式
     * **/
    SynergyTRTC.synergytrtc = null;
    Synergy.SynergyTRTC = SynergyTRTC;
})(Synergy || (Synergy = {}));
/// <reference path="../../../../lib/ThirdParty/tim/tim-js.d.ts" />
/// <reference path="../../../../lib/ThirdParty/tim/cos-js-sdk-v5.min.d.ts" />
var Synergy;
(function (Synergy) {
    class SynergyTim {
        constructor() {
            //private uiManager: SView.UIManager;
            this.tim = null;
        }
        static instance() {
            if (Synergy.SynergyTim.synergytim == null) {
                Synergy.SynergyTim.synergytim = new Synergy.SynergyTim();
            }
            return Synergy.SynergyTim.synergytim;
        }
        /**
        *   login 成功才会驱动 SDK 触发 SDK_READY 事件
        *   接入侧监听此事件，然后可调用 SDK 发送消息等API，使用 SDK 的各项功能
        *   您可以通过此监听事件获取 SDK 状态
        **/
        onSdkReady(event) {
            //console.log("进入SdkReady回调---------" + JSON.stringify(event));
            //createTipWindow("", getLocalizedString(language, "SynergyGroup", "ServerIsConnected"));
            //Synergy.SynergyTim.instance().updateMyNick("");
        }
        /**
         * SDK 收到推送的单聊、群聊、群提示、群系统通知的新消息
         *  可通过遍历 event.data 获取消息列表数据并渲染到页面
         * @param event
         */
        onMessageReceived(event) {
            // event.data - 存储 Message 对象的数组 - [Message]
            //console.log("进入onMessageReceived回调---------" + JSON.stringify(event.data));
            let messages = eval(event.data);
            for (let i = 0; i < messages.length; i++) {
                let msgtype = messages[i].type;
                switch (msgtype) {
                    case TIM.TYPES.MSG_TEXT: //文本消息
                        Synergy.SynergyTim.instance().parseText(messages[i]);
                        break;
                    case TIM.TYPES.MSG_IMAGE: //图片消息
                        Synergy.SynergyTim.instance().parseImage(messages[i]);
                        break;
                    case TIM.TYPES.MSG_AUDIO: //音频消息
                        break;
                    case TIM.TYPES.MSG_VIDEO: //视频消息
                        break;
                    case TIM.TYPES.MSG_FILE: //文件消息
                        break;
                    case TIM.TYPES.MSG_CUSTOM: //自定义消息
                        break;
                    case TIM.TYPES.MSG_GEO: //位置消息
                        break;
                    case TIM.TYPES.MSG_GRP_TIP: //群提示消息
                        Synergy.SynergyTim.instance().parseGroupTipContent(messages[i]);
                        break;
                    case TIM.TYPES.MSG_GRP_SYS_NOTICE: //群系统通知消息
                        Synergy.SynergyTim.instance().parseGroupSystemNotice(messages[i]);
                        break;
                }
            }
        }
        /**
         * 网络状态发生改变
         * @param event
         */
        onNetStateChange(event) {
            // v2.5.0 起支持
            // event.data.state 当前网络状态，枚举值及说明如下：
            // TIM.TYPES.NET_STATE_CONNECTED - 已接入网络
            // TIM.TYPES.NET_STATE_CONNECTING - 连接中。很可能遇到网络抖动，SDK 在重试。接入侧可根据此状态提示“当前网络不稳定”或“连接中”
            // TIM.TYPES.NET_STATE_DISCONNECTED - 未接入网络。接入侧可根据此状态提示“当前网络不可用”。SDK 仍会继续重试，若用户网络恢复，SDK 会自动同步消息
            //console.log("进入onNetStateChange回调---------网络发生改变" + event.data.state);
            switch (event.data.state) {
                case TIM.TYPES.NET_STATE_CONNECTED:
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "ConnectedToTheNetwork"));
                    console.error("-------------已接入网络-----------");
                    break;
                case TIM.TYPES.NET_STATE_CONNECTING:
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "TheCurrentNetworkIsUnstable"));
                    console.error("-------------当前网络不稳定-----------");
                    break;
                case TIM.TYPES.NET_STATE_DISCONNECTED:
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "TheCurrentNetworkIsUnavailable"));
                    console.error("-------------当前网络不可用-----------");
                    break;
            }
        }
        //初始化tim组件
        timInit() {
            let options = {
                SDKAppID: Synergy.SYNERGY_APPID //即时通信 IM 应用的 SDKAppID
            };
            // 创建 SDK 实例，`TIM.create()`方法对于同一个 `SDKAppID` 只会返回同一份实例
            this.tim = TIM.create(options);
            // 设置 SDK 日志输出级别，详细分级请参见 setLogLevel 接口的说明
            //tim.setLogLevel(0); // 普通级别，日志量较多，接入时建议使用
            this.tim.setLogLevel(1); // release 级别，SDK 输出关键信息，生产环境时建议使用
            // 注册 COS SDK 插件
            this.tim.registerPlugin({ 'cos-js-sdk': window.COS });
            // 接下来可以通过 tim 进行事件绑定和构建 IM 应用
            //onEvent();
        }
        /**
         * 绑定事件
         * */
        onEvent() {
            this.offEvent();
            /****监听事件，例如：*****/
            //SDK 进入 ready 状态时触发
            this.tim.on(TIM.EVENT.SDK_READY, this.onSdkReady);
            //SDK 进入 not ready 状态时触发
            //tim.on(TIM.EVENT.SDK_NOT_READY, onSdkNotReady);
            //接收到推送的单聊、群聊、群提示、群系统通知的新消息
            this.tim.on(TIM.EVENT.MESSAGE_RECEIVED, this.onMessageReceived);
            //SDK 收到消息被撤回的通知
            //tim.on(TIM.EVENT.MESSAGE_REVOKED, onMessageRevoked);
            //SDK 收到对端已读消息的通知，即已读回执
            //tim.on(TIM.EVENT.MESSAGE_READ_BY_PEER, onMessageReadByPeer);
            //会话列表更新
            //tim.on(TIM.EVENT.CONVERSATION_LIST_UPDATED, onConversationListUpdated);
            //收到群组列表更新通知
            //tim.on(TIM.EVENT.GROUP_LIST_UPDATED, onGroupListUpdated);
            //SDK 收到新的群系统通知时触发
            //tim.on(TIM.EVENT.GROUP_SYSTEM_NOTICE_RECEIVED, onGroupSystemNoticeReceived);
            //自己或好友的资料发生变更时触发
            //tim.on(TIM.EVENT.PROFILE_UPDATED, onProfileUpdated);
            // 收到黑名单列表更新通知
            //tim.on(TIM.EVENT.BLACKLIST_UPDATED, onBlacklistUpdated);
            //被踢下线通知监听事件
            //tim.on(TIM.EVENT.KICKED_OUT, onKickedOut);
            // 收到 SDK 发生错误通知
            //tim.on(TIM.EVENT.ERROR, onError);
            // 网络状态发生改变（v2.5.0 起支持）。 
            this.tim.on(TIM.EVENT.NET_STATE_CHANGE, this.onNetStateChange);
            //tim.on("onFriendApplicationListAdded", function (event) {
            //    console.log(event);
            //});
        }
        /**
         * 取消绑定
         * */
        offEvent() {
            //SDK 进入 ready 状态时触发
            this.tim.off(TIM.EVENT.SDK_READY, this.onSdkReady);
            //SDK 进入 not ready 状态时触发
            //tim.off(TIM.EVENT.SDK_NOT_READY, onSdkNotReady);
            //接收到推送的单聊、群聊、群提示、群系统通知的新消息
            this.tim.off(TIM.EVENT.MESSAGE_RECEIVED, this.onMessageReceived);
            //SDK 收到消息被撤回的通知
            //tim.off(TIM.EVENT.MESSAGE_REVOKED, onMessageRevoked);
            //SDK 收到对端已读消息的通知，即已读回执
            //tim.off(TIM.EVENT.MESSAGE_READ_BY_PEER, onMessageReadByPeer);
            //会话列表更新
            //tim.off(TIM.EVENT.CONVERSATION_LIST_UPDATED, onConversationListUpdated);
            //收到群组列表更新通知
            //tim.off(TIM.EVENT.GROUP_LIST_UPDATED, onGroupListUpdated);
            //SDK 收到新的群系统通知时触发
            //tim.off(TIM.EVENT.GROUP_SYSTEM_NOTICE_RECEIVED, onGroupSystemNoticeReceived);
            //自己或好友的资料发生变更时触发
            //tim.off(TIM.EVENT.PROFILE_UPDATED, onProfileUpdated);
            // 收到黑名单列表更新通知
            //tim.off(TIM.EVENT.BLACKLIST_UPDATED, onBlacklistUpdated);
            //被踢下线通知监听事件
            //tim.off(TIM.EVENT.KICKED_OUT, onKickedOut);
            // 收到 SDK 发生错误通知
            //tim.off(TIM.EVENT.ERROR, onError);
            // 网络状态发生改变（v2.5.0 起支持）。 
            this.tim.off(TIM.EVENT.NET_STATE_CHANGE, this.onNetStateChange);
        }
        /**
         * 登录tim控件
         * @param userid
         * @param usersig
         * @param callback
         */
        timLogin(serverurl, userid) {
            let result = {
                code: "1",
                msg: "",
                userid: String(userid)
            };
            let that = this;
            let p = new Promise(function (resolve, reject) {
                //初始化控件
                if (that.tim == null) {
                    that.timInit();
                }
                let getusersigurl = serverurl + "/7.2/tim/login/usersig";
                let param = {
                    userid: String(userid)
                };
                M3D.Utility.HttpMessageHandler.getByParameters(getusersigurl, false, param, M3D.Utility.REQUESTRESPONSETYPE.JSON).then((response) => {
                    if (response) {
                        let retdata = JSON.parse(response);
                        let returnCode = retdata.return;
                        if (returnCode === "0") {
                            userid = retdata.userid;
                            let usersig = retdata.usersig;
                            result.userid = userid;
                            //初始化音视频
                            Synergy.SynergyTRTC.instance().createClient(userid, usersig);
                            let promise = that.tim.login({ userID: userid, userSig: usersig });
                            promise.then(function (imResponse) {
                                console.log(imResponse.data); // 登录成功result.code = '0';
                                result.code = '0';
                                if (imResponse.data.repeatLogin === true) {
                                    // 标识账号已登录，本次登录操作为重复登录。v2.5.1 起支持
                                    //console.log(imResponse.data.errorInfo);
                                }
                                resolve(result);
                            }).catch(function (imError) {
                                console.warn('login error:', imError); // 登录失败的相关信息
                                result.code = imError.code;
                                result.msg = imError.message;
                                reject(result);
                            });
                        }
                        else {
                            //console.log(jsonObject["msg"]);
                            result.code = retdata.return;
                            result.msg = retdata.msg;
                            reject(result);
                        }
                    }
                });
            });
            return p;
        }
        /**
         * 更新群组中我的昵称
         * @param nickname
         */
        updateMyProfile(nickname) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let options = {
                    nick: String(nickname)
                };
                let promise = that.tim.updateMyProfile(options);
                promise.then(function (imResponse) {
                    //console.log("--------------更新个人资料成功："+imResponse.data); // 更新资料成功
                    resolve(imResponse);
                }).catch(function (imError) {
                    //console.warn('updateMyProfile error:', imError); // 更新资料失败的相关信息
                    reject(imError);
                });
            });
            return p;
        }
        /**
         * 发送消息
         * @param message
         */
        sendMsg(message) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    to: message.to,
                    msg: ""
                };
                let promise = that.tim.sendMessage(message);
                promise.then(function (imResponse) {
                    // 发送成功
                    //console.log("-----------发送消息---------" + JSON.stringify(imResponse));
                    result.code = "0";
                    resolve(result);
                }).catch(function (imError) {
                    // 发送失败
                    //console.warn('sendMessage error:', imError);
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 建文本信息
         *     to:消息接收方的 userID 或 groupID
         *     conversationType: 会话类型，取值TIM.TYPES.CONV_C2C（端到端会话）或TIM.TYPES.CONV_GROUP（群组会话）
         *     priority:消息优先级
         *     text:消息文本内容
         * @param to
         * @param text
         * @param conversationType
         */
        createTextMessage(to, text, conversationType) {
            let options = {
                to: String(to),
                conversationType: conversationType,
                priority: TIM.TYPES.MSG_PRIORITY_NORMAL,
                payload: {
                    text: text
                }
            };
            let message = this.tim.createTextMessage(options);
            return message;
        }
        /**
         * 创建图片消息
         * @param to
         * @param file
         */
        createImageMessage(to, file) {
            let options = {
                to: String(to),
                conversationType: TIM.TYPES.CONV_GROUP,
                priority: TIM.TYPES.MSG_PRIORITY_NORMAL,
                payload: {
                    file: file
                },
                onProgress: function (event) {
                    //console.log('----------创建图片信息 file uploading:', event)
                }
            };
            let message = this.tim.createImageMessage(options);
            return message;
        }
        /**
         * 创建群组
         * @param serverurl  服务端url
         * @param name  群组名称，最长30字节
         * @param userid
         * @param host  主持人昵称
         * @param notification 公告
         *    type 群组类型:TIM.TYPES.GRP_WORK：好友工作群，默认
         *         TIM.TYPES.GRP_PUBLIC：陌生人社交群
         *         TIM.TYPES.GRP_MEETING：临时会议群
         *         TIM.TYPES.GRP_AVCHATROOM：直播群
         */
        createGroup(serverurl, name, userid, host, notification) {
            let result = {
                code: "1",
                msg: "",
                groupid: ""
            };
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let getusersigurl = serverurl + "/7.2/tim/group/create";
                let param = {
                    name: name,
                    userid: userid,
                    host: host
                };
                M3D.Utility.HttpMessageHandler.getByParameters(getusersigurl, false, param, M3D.Utility.REQUESTRESPONSETYPE.JSON).then((response) => {
                    if (response) {
                        let retdata = JSON.parse(response);
                        let returnCode = retdata.return;
                        if (returnCode === "0") {
                            let groupID = retdata.groupid;
                            let options = {
                                groupID: groupID,
                                name: name,
                                notification: notification,
                                type: TIM.TYPES.GRP_PUBLIC,
                                memberList: [{
                                        userID: userid
                                    }]
                            };
                            let promise = that.tim.createGroup(options);
                            promise.then(function (imResponse) {
                                //console.log(imResponse.data.group);// 创建的群的资料
                                result.code = '0';
                                result.groupid = groupID;
                                //updateMyProfile(host);
                                that.updateGroupProfile({ groupID: groupID, joinOption: TIM.TYPES.JOIN_OPTIONS_FREE_ACCESS });
                                //初始化事件
                                that.onEvent();
                                //进入音视频房间
                                Synergy.SynergyTRTC.instance().join(groupID);
                                resolve(result);
                            }).catch(function (imError) {
                                //console.error("createGroup error:", imError);// 创建群组失败的相关信息
                                result.code = imError.code;
                                result.msg = imError.message;
                                reject(result);
                            });
                        }
                        else {
                            //console.log(jsonObject["msg"]);
                            result.code = retdata.return;
                            result.msg = retdata.msg;
                            reject(result);
                        }
                    }
                });
            });
            return p;
        }
        /**
         * 解散群组
         * 群主可调用该接口解散群组,群主不能解散好友工作群
         * @param groupID
         */
        dismissGroup(groupID) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: ""
                };
                let promise = that.tim.dismissGroup(groupID);
                promise.then(function (imResponse) {
                    console.log("解散群组:" + imResponse.data.groupID); // 被解散的群组 ID
                    result.code = "0";
                    that.offEvent();
                    //退出音视频房间
                    Synergy.SynergyTRTC.instance().leave();
                    resolve(result);
                }).catch(function (imError) {
                    //console.error("dismissGroup error:", imError);// 解散群组失败的相关信息
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 申请加入群组
         * @param groupid
         * @param applyMessage
         * applyMessage:附言
         *    type:待加入的群组的类型，加入直播群时该字段必填。可选值：
         *         TIM.TYPES.GRP_PUBLIC：陌生人社交群
         *         TIM.TYPES.GRP_MEETING：临时会议群
         *         TIM.TYPES.GRP_AVCHATROOM：直播群
         */
        joinGroup(groupid, applyMessage) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    isingroup: false,
                    msg: ""
                };
                let options = {
                    groupID: String(groupid),
                    applyMessage: applyMessage,
                    type: TIM.TYPES.GRP_PUBLIC
                };
                let promise = that.tim.joinGroup(options);
                promise.then(function (imResponse) {
                    switch (imResponse.data.status) {
                        case TIM.TYPES.JOIN_STATUS_WAIT_APPROVAL: //等待管理员同意
                            //console.log("-------------等待管理员同意");
                            result.msg = "等待管理员同意";
                            result.code = "1";
                            break;
                        case TIM.TYPES.JOIN_STATUS_ALREADY_IN_GROUP: //已经在群中
                            //console.log("-------------已经在群中");
                            result.msg = "已经在群中";
                            result.isingroup = true;
                            result.code = "0";
                            that.onEvent();
                            break;
                        case TIM.TYPES.JOIN_STATUS_SUCCESS: //加群成功
                            //console.log("--------------加群成功，群资料为:" + JSON.stringify(imResponse.data.group)); // 加入的群组资料
                            result.code = "0";
                            result.msg = "加群成功";
                            result.isingroup = false;
                            that.onEvent();
                            break;
                        default:
                            break;
                    }
                    //音视频进入房间
                    Synergy.SynergyTRTC.instance().join(groupid);
                    resolve(result);
                }).catch(function (imError) {
                    console.error("joinGroup error:", imError); // 申请加群失败的相关信息
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 退出群组
         * 群主只能退出好友工作群，退出后该好友工作群无群主。
         * @param groupid
         */
        quitGroup(groupid) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    groupid: ""
                };
                let promise = that.tim.quitGroup(String(groupid));
                promise.then(function (imResponse) {
                    //console.log("退群成功:" + JSON.stringify(imResponse.data)); // 退出成功的群 ID
                    result.code = "0";
                    result.groupid = imResponse.data.groupID;
                    that.offEvent();
                    //退出音视频房间
                    Synergy.SynergyTRTC.instance().leave();
                    resolve(result);
                }).catch(function (imError) {
                    //console.warn('quitGroup error:', imError); // 退出群组失败的相关信息
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         *  修改群资料
         * @param options
         *
         * joinOption 申请加群处理方式
         *      TIM.TYPES.JOIN_OPTIONS_FREE_ACCESS （自由加入）
         *      TIM.TYPES.JOIN_OPTIONS_NEED_PERMISSION （需要验证）
         *      TIM.TYPES.JOIN_OPTIONS_DISABLE_APPLY （禁止加群）
         */
        updateGroupProfile(options) {
            let that = this;
            let promise = that.tim.updateGroupProfile(options);
            promise.then(function (imResponse) {
                // console.log("修改成功后的群组详细资料------------" + JSON.stringify(imResponse.data.group)) // 修改成功后的群组详细资料
            }).catch(function (imError) {
                // console.error("updateGroupProfile error", imError);
            });
        }
        /**
         * 解析文本消息,暂不考虑表情符号
         * @param message
         */
        parseText(message) {
            // 文本消息
            let text = message.payload.text;
            console.log("------收到的文本信息为：" + text);
            Synergy.Receive.receiveTextMsg(message);
        }
        /**
         * 解析图片消息
         * @param message
         */
        parseImage(message) {
            // 唯一标识
            //var uuid = message.payload.uuid;
            // 图片地址，可用于渲染
            let url = message.payload.imageInfoArray[2].url;
            console.log("------解析收到的图片地址为：" + url);
            Synergy.Receive.receiveImageMsg(message);
        }
        /**
         * 解析系统消息
         * @param message
         */
        parseGroupSystemNotice(message) {
            let groupName = message.payload.groupProfile.groupName || message.payload.groupProfile.groupID;
            let that = this;
            switch (message.payload.operationType) {
                case 1:
                    console.log(`${message.payload.operatorID} 申请加入群组：${groupName}`);
                    //加入群组后自动同意加群申请
                    that.handleGroupApplication('Agree', '', message);
                    break;
                case 2:
                    //console.log(`成功加入群组：${groupName}`);
                    //加入后，只给自己推送信息，可初始化群组信息
                    break;
                case 3:
                    //console.log(`申请加入群组：${groupName}被拒绝`);
                    break;
                case 4:
                    //console.log(`被管理员${message.payload.operatorID}踢出群组：${groupName}`);
                    break;
                case 5:
                    console.log(`群：${groupName} 已被${message.payload.operatorID}解散`);
                    //会议解散
                    //groupHasDismissed(message.payload.groupProfile.groupID);
                    Synergy.Receive.receiveGroupHasDismissed(message.payload.groupProfile.groupID);
                    break;
                case 6:
                    //console.log(`${message.payload.operatorID}创建群：${groupName}`);
                    break;
                case 7:
                    //console.log(`${message.payload.operatorID}邀请你加群：${groupName}`);
                    break;
                case 8:
                    //console.log(`你退出群组：${groupName}`);
                    break;
                case 9:
                    console.log(`你被${message.payload.operatorID}设置为群：${groupName}的管理员`);
                    //receiveAddAdminRoleMsg(message);
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "EnabledYourOperationPermissions"));
                    Synergy.Receive.receiveAddAdminRoleMsg(message);
                    break;
                case 10:
                    console.log(`你被${message.payload.operatorID}撤销群：${groupName}的管理员身份`);
                    //receiveBackoutAdminRoleMsg(message);
                    //createTipWindow("", getLocalizedString(language, "SynergyGroup", "ClosedYourOperationPermissions"));
                    Synergy.Receive.receiveBackoutAdminRoleMsg(message);
                    break;
                case 255:
                    //console.log('自定义群系统通知');
                    break;
            }
        }
        /**
         * 解析群提示消息
         * @param message
         */
        parseGroupTipContent(message) {
            switch (message.payload.operationType) {
                case TIM.TYPES.GRP_TIP_MBR_JOIN:
                    console.log(`群成员：${message.payload.userIDList.join(',')}，加入群组`);
                    //自己加群不会给自己推送
                    //membersJoin(message.payload.userIDList);
                    Synergy.Receive.receiveMembersJoin(message.payload.userIDList);
                    break;
                case TIM.TYPES.GRP_TIP_MBR_QUIT:
                    console.log(`群成员：${message.payload.userIDList.join(',')}，退出群组`);
                    //membersQuit(message.payload.userIDList);
                    Synergy.Receive.receiveMembersQuit(message.payload.userIDList);
                    break;
                case TIM.TYPES.GRP_TIP_MBR_KICKED_OUT:
                    // console.log(`群成员：${message.payload.userIDList.join(',')}，被${payload.operatorID}踢出群组`);
                    break;
                case TIM.TYPES.GRP_TIP_MBR_SET_ADMIN:
                    //console.log(`群成员：${message.payload.userIDList.join(',')}，成为管理员`);
                    break;
                case TIM.TYPES.GRP_TIP_MBR_CANCELED_ADMIN:
                    //console.log(`群成员：${message.payload.userIDList.join(',')}，被撤销管理员`);
                    break;
                case TIM.TYPES.GRP_TIP_GRP_PROFILE_UPDATED:
                    console.log('群组资料变更');
                    //groupProfileHasChanged(message);
                    Synergy.Receive.receiveGroupProfileHasChanged(message);
                    break;
                case TIM.TYPES.GRP_TIP_MBR_PROFILE_UPDATED:
                    //console.log("群成员："+JSON.stringify(message.payload.memberList)+"被禁言");
                    break;
                default:
                    // console.log('[群提示消息]');
                    break;
            }
        }
        /**
         * 服务器获取群组列表
         * @param serverurl
         * @param offset
         * @param pagesize
         * @param searchcontent 根据会议名检索
         */
        listGroup(serverurl, offset, pagesize, searchcontent) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    groups: ""
                };
                let getgrouplisturl = serverurl + "/7.2/tim/group/list";
                let param = {
                    offset: offset,
                    pagesize: pagesize,
                    searchcontent: searchcontent
                };
                M3D.Utility.HttpMessageHandler.getByParameters(getgrouplisturl, false, param, M3D.Utility.REQUESTRESPONSETYPE.JSON).then((response) => {
                    if (response) {
                        let retdata = JSON.parse(response);
                        let returnCode = retdata.return;
                        if (returnCode === "0") {
                            result.code = "0";
                            result.groups = retdata.groups;
                            resolve(result);
                        }
                        else {
                            //console.log(jsonObject["msg"]);
                            result.code = retdata.return;
                            result.msg = retdata.msg;
                            reject(result);
                        }
                    }
                });
            });
            return p;
        }
        /**
         *   同意或拒绝
         *   如果一个群有多位管理员，当有人申请加群时，所有在线的管理员都会收到申请加群的群系统通知。
         *   如果某位管理员处理了这个申请（同意或者拒绝），则其他管理员无法重复处理（即不能修改处理的结果）。
         *   handleAction:处理结果 Agree（同意） / Reject（拒绝）
         *   handleMessage:附言
         *   message:申请加群的【群系统通知消息】的消息实例。
         * @param handleAction
         * @param handleMessage
         * @param message
         */
        handleGroupApplication(handleAction, handleMessage, message) {
            let that = this;
            let promise = that.tim.handleGroupApplication({
                handleAction: handleAction,
                handleMessage: handleMessage,
                message: message // 申请加群群系统通知的消息实例
            });
            promise.then(function (imResponse) {
                // console.log(imResponse.data.group); // 群组资料
            }).catch(function (imError) {
                //console.warn('handleGroupApplication error:', imError); // 错误信息
            });
        }
        /**
         * 获取群成员列表
         * @param groupid
         * @param count
         * @param offset
         * 该接口是分页拉取群成员，不能直接用于获取群的总人数。获取群的总人数（memberNum）请使用 getGroupProfile ，获取群成员详细资料请使用getGroupMemberProfile。
         *    count:需要拉取的数量。最大值为100，避免回包过大导致请求失败。若传入超过100，则只拉取前100个
         *    offset:偏移量，默认从0开始拉取
         */
        getGroupMemberList(groupid, count, offset) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    attendees: ""
                };
                // 从v2.6.2 起，该接口支持拉取群成员禁言截止时间戳。
                let promise = that.tim.getGroupMemberList({
                    groupID: String(groupid),
                    count: count,
                    offset: offset
                }); // 从0开始拉取30个群成员
                promise.then(function (imResponse) {
                    // console.log(imResponse.data.memberList); // 群成员列表
                    result.code = "0";
                    result.attendees = imResponse.data.memberList;
                    resolve(result);
                }).catch(function (imError) {
                    // console.warn('getGroupMemberList error:', imError);
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 获取指定群成员资料,每次查询的用户数上限是50。如果传入的数组长度大于50，则只取前50个用户进行查询，其余丢弃。
         * @param groupID
         * @param userIDList
         * @param callback
         */
        getGroupMemberProfile(groupid, useridList) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    attendees: ""
                };
                // 从v2.6.2 起，该接口支持拉取群成员禁言截止时间戳。
                let promise = that.tim.getGroupMemberProfile({
                    groupID: String(groupid),
                    userIDList: useridList
                }); // 从0开始拉取30个群成员
                promise.then(function (imResponse) {
                    // console.log(imResponse.data.memberList); // 群成员列表
                    result.code = "0";
                    result.attendees = imResponse.data.memberList;
                    resolve(result);
                }).catch(function (imError) {
                    // console.warn('getGroupMemberList error:', imError);
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 修改群成员角色
         * @param groupid
         * @param userid
         * @param role
         * role :  TIM.TYPES.GRP_MBR_ROLE_ADMIN（群管理员） 或 TIM.TYPES.GRP_MBR_ROLE_MEMBER（群普通成员）
         */
        setGroupMemberRole(groupid, userid, role) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    userid: String(userid)
                };
                let options = {
                    groupID: String(groupid),
                    userID: String(userid),
                    role: role
                };
                let promise = that.tim.setGroupMemberRole(options);
                promise.then(function (imResponse) {
                    result.code = "0";
                    resolve(result);
                }).catch(function (imError) {
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
        /**
         * 获取群详细资料
         * @param groupid 群组 ID
         * @param groupCustomFieldFilter  群组的自定义字段过滤器，指定需要获取的群组的自定义字段
         */
        getGroupPro(groupid, groupCustomFieldFilter) {
            let that = this;
            let p = new Promise(function (resolve, reject) {
                let result = {
                    code: "1",
                    msg: "",
                    group: ""
                };
                let options = {
                    groupID: String(groupid),
                    groupCustomFieldFilter: groupCustomFieldFilter //数组['key1', 'key2']
                };
                let promise = that.tim.getGroupProfile(options);
                promise.then(function (imResponse) {
                    result.code = "0";
                    result.group = imResponse.data.group;
                    resolve(result);
                }).catch(function (imError) {
                    result.code = imError.code;
                    result.msg = imError.message;
                    reject(result);
                });
            });
            return p;
        }
    }
    SynergyTim.synergytim = null;
    Synergy.SynergyTim = SynergyTim;
})(Synergy || (Synergy = {}));
/// <reference path="../../../../lib/SViewFrame.d.ts" />
var Synergy;
(function (Synergy) {
    class UiManager {
        /**
         * 打开会议面板
         */
        static openMeetingDialog() {
            let meetingDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
            if (meetingDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager()
                    .load(Synergy.Dialog.MeetingDialog);
            }
            else {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
            }
            SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.meetingMainMenuButton.id);
        }
        /**
         * 关闭会议面板
         * */
        static closeMeetingDialog() {
            //Synergy.Main.instance().getSviewFrame().getUIManager()
            //    .removeLabel(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
            SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.meetingMainMenuButton.id);
        }
        /**
        * 打开设置面板
        */
        static openSettingMeetingDialog() {
            this.closeMeetingDialog();
            let settingMeetingDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.SettingMeetingDialog.Dialog.id);
            if (settingMeetingDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager()
                    .load(Synergy.Dialog.SettingMeetingDialog);
            }
            else {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.SettingMeetingDialog.Dialog.id);
            }
            let timNickName = Synergy.Main.instance().getMyProfile().getNickName();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.SettingMeetingDialogFormNicknameInput.Input.id).setValue(timNickName);
        }
        /**
        * 关闭设置面板
        */
        static closeSettingMeetingDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.SettingMeetingDialog.Dialog.id);
            this.openMeetingDialog();
        }
        /**
       * 重置设置面板
       */
        static reSetSettingMeetingDialog() {
            let timNickName = Synergy.Main.instance().getMyProfile().getNickName();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.SettingMeetingDialogFormNicknameInput.Input.id).setValue(timNickName);
        }
        /**
         * 打开创建会议dialog
         * */
        static showCreatMettingDialog() {
            //console.log("--------------会议底层的ui管理器-----------");
            //document.getElementById(Synergy.Dialog.MeetingDialog.MeetingDialog.id).style.display = "none";
            this.closeMeetingDialog();
            let createMeetingDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.CreateMeetingDialog.Dialog.id);
            if (createMeetingDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.CreateMeetingDialog);
            }
            else {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.CreateMeetingDialog.Dialog.id);
            }
            let timNickName = Synergy.Main.instance().getMyProfile().getNickName();
            //let form = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.CreateMeetingForm.Form.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingThemeInput.Input.id)
                .setValue(timNickName + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.WhoseMeeting);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingNicknameInput.Input.id).setValue(timNickName);
            //document.getElementById(Synergy.Dialog.MeetingThemeInput.Input.id).setAttribute("value", timNickName + "的会议");
            //document.getElementById(Synergy.Dialog.MeetingNicknameInput.Input.id).setAttribute("value", timNickName);
        }
        /**
        * 关闭创建会议dialog
        * */
        static closeCreatMettingDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.CreateMeetingDialog.Dialog.id);
            this.openMeetingDialog();
        }
        /**
         * 打开加入会议dialog
         * */
        static showJoinMeetingDialog() {
            //console.log("--------------会议底层的ui管理器-----------");
            this.closeMeetingDialog();
            let joinMeetingDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingDialog.Dialog.id);
            if (joinMeetingDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.JoinMeetingDialog);
            }
            else {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.JoinMeetingDialog.Dialog.id);
            }
            let timNickName = Synergy.Main.instance().getMyProfile().getNickName();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingTabsMeetingIdFormNickNameInput.Input.id).setValue(timNickName);
            //清空会议列表
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.JoinMeetingTabsMeetingListTable.Table.id).removeRow();
            //初始化会议列表
            Synergy.Main.instance().initGroupList();
        }
        /**
       * 关闭加入会议dialog
       * */
        static closeJoinMeetingDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.JoinMeetingDialog.Dialog.id);
            this.openMeetingDialog();
        }
        /**
         * 进入或退出会议后控制相关界面的显隐及会议初始化
         * @param isQuitGroup 是否是退出会议
         * @param isHasInGroup 之前是否已经在会议中，只对加入会议时有效
         */
        static initGroupDialogs(isQuitGroup, isHasInGroup) {
            if (!isQuitGroup) {
                //进入会议
                /**
                * 隐藏会议相关界面
                * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingDialog.MeetingDialog.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.CreateMeetingDialog.Dialog.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.JoinMeetingDialog.Dialog.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.SettingMeetingDialog.Dialog.id);
                /**
                * 隐藏右侧及底部工具栏
                * **/
                this.hideAllRightMenuButton();
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.BottomMenu.BottomMenu.id);
                /**
                 * 隐藏装配面板
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AssemblyDialog.TreeDialog.id);
                /**
                 * 隐藏属性面板
                 **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttributeDialog.AttributeDialog.id);
                /**
                 * 退出剖切todo
                 **/
                /**
                 * 退出爆炸todo
                 **/
                /**
                 * 显示音视频列表
                 **/
                this.showVideoDialog();
                //清空远端音视频框
                Synergy.Main.instance().getSviewFrame().getUIManager().removeLabelByClass(Synergy.Dialog.VideoItem.remoteVideoClass);
                /**
                 * 初始化本地音视频按钮
                 **/
                //打开视频遮罩
                document.getElementById(Synergy.Dialog.LocalVideoItem.VideoItem.id + Synergy.Dialog.VideoItem.maskSuffix).style.display = "block";
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseAudioBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.LocalVideoItemCloseVideoBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenAudioBtn.Button.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.LocalVideoItemOpenVideoBtn.Button.id);
                /**
                 * 初始化基本信息，权限，参会人数等
                 * **/
                this.initGroupBaseInfo(isHasInGroup);
            }
            else {
                //退出会议
                /**
                 * 隐藏会议主界面
                 * */
                this.closeMeetingControlDialog();
                /**
                 * 隐藏聊天界面
                 * */
                this.closeChatDialog();
                /**
                 * 隐藏申请人列表界面
                 * */
                this.closeMeetingApplysDialogOnly();
                /**
                 * 显示右侧工具栏按钮
                 * */
                this.hideAllRightMenuButton();
                this.showPreRightMenuButton();
                /**
                 * 隐藏音视频列表
                 * */
                this.closeVideoDialog();
                /**
                 * 移除显示附件模态框
                 * */
                Synergy.Main.instance().getSviewFrame().getUIManager().removeLabel(Synergy.Dialog.PreviewAttachmentDialog.Dialog.id);
                Synergy.Main.instance().getMyProfile().reSet();
                Synergy.Main.instance().getTimGroup().reSet();
                this.isFirstOpen = true;
            }
        }
        /**
         * 隐藏所有右侧菜单按钮
         * */
        static hideAllRightMenuButton() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.resetButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.meetingApplyButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.viewMenuBtn.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.assemblyTreeButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.attributeInfoButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.meetingMainMenuButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.moreMenuButton.id);
        }
        /**
         * 显示原有的右侧菜单按钮
         * */
        static showPreRightMenuButton() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.resetButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.viewMenuBtn.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.assemblyTreeButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.attributeInfoButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.meetingMainMenuButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.moreMenuButton.id);
        }
        /**
         * 显示有操作权限时的右侧菜单按钮
         * */
        static showHasOperateAuthRightMenuButton() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.resetButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.viewMenuBtn.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.assemblyTreeButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.moreMenuButton.id);
        }
        /**
         * 显示无操作权限时的右侧菜单按钮
         * */
        static showNoOperateAuthRightMenuButton(isOwner) {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id);
            if (!isOwner) {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.RightMenuBtns.meetingApplyButton.id);
            }
        }
        /**
         * 显示群主角色时的会议控制界面的控制按钮
         * */
        static showHasOwnerRoleOperateButton() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlMuteAllMembersBtn.Button.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlAllowAllMembersBtn.Button.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlShowApplyMembersBtn.Button.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlInviteMembersBtn.Button.id);
        }
        /**
         * 显示普通角色时的会议控制界面的控制按钮
         * */
        static showHasCommonRoleOperateButton() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlInviteMembersBtn.Button.id);
        }
        /**
         * 显示音视频dialog
         * */
        static showVideoDialog() {
            let videoDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.VideoDialog.VideoDialog.id);
            if (videoDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.VideoDialog);
            }
            else {
                Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.VideoDialog.VideoDialog.id);
            }
        }
        /**
         * 隐藏音视频dialog
         * */
        static closeVideoDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.VideoDialog.VideoDialog.id);
        }
        /**
         * 初始化会议基本信息，权限等
         * @param isHasInGroup
         */
        static initGroupBaseInfo(isHasInGroup) {
            let that = this;
            //重新获取一遍是因为已经在群组中再次加入群的时候不返回群资料
            Synergy.SynergyTim.instance().getGroupPro(Synergy.Main.instance().getTimGroup().getGroupID(), []).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    console.log("----------获取群详细资料成功-开始初始化各项基本信息--------");
                    let curgroup = retdata.group;
                    /**
                     * 设置群名称
                     * **/
                    Synergy.Main.instance().getTimGroup().setName(curgroup.name);
                    /**
                     * 初始化群主id及昵称，创建会议时已经初始化群主id及昵称，加入会议时无法初始化，故在此初始化一下
                     **/
                    that.initGroupOwnerInfo(curgroup);
                    /**
                     * 初始化会议总人数
                     * */
                    that.initGroupMemberNum(curgroup);
                    /**
                     * 初始化参会者音视频权限
                     */
                    that.initAttendeeVagAuth(curgroup, isHasInGroup);
                    /**
                     * 清空会议中聊天记录
                     **/
                    that.initChatDialog();
                    /**
                     * 清空会议中附件记录与参会人列表
                     **/
                    that.initMeetingControlDialog();
                    /**
                     * 清空申请人列表
                     * */
                    that.initMeetingApplysDialog();
                    /**
                     * 初始化参会者角色（三维操作权限）
                     */
                    that.initAttendeeOperateAuth();
                    /**
                     * 初始化协同模型
                     * **/
                    that.initSynergyModel(curgroup, isHasInGroup);
                    /**
                    * 初始化左下角权限申请提示列表，仅加载，不显示，并清空申请提示列表
                    * */
                    that.initApplyTipList();
                }
                else {
                    console.error("获取群详细资料失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                console.error("获取群详细资料失败-----" + retdata);
            });
        }
        /**
         * 初始化群主信息
         * @param group
         */
        static initGroupOwnerInfo(group) {
            let that = this;
            let ownerid = group.ownerID;
            Synergy.Main.instance().getTimGroup().setOwnerID(ownerid);
            if (Synergy.Main.instance().getMyProfile().getUserID() != group.ownerID) {
                //如果不是群主
                //获取群主的信息
                Synergy.SynergyTim.instance().getGroupMemberProfile(Synergy.Main.instance().getTimGroup().getGroupID(), [ownerid]).then(function (data) {
                    let retdata = eval(data);
                    if (retdata.code == '0') {
                        let ownerGroupProfile = retdata.attendees[0];
                        let ownernick = ownerGroupProfile.nick;
                        Synergy.Main.instance().getTimGroup().setOwnerNick(ownernick);
                    }
                    else {
                        console.error("获取群主资料失败-----" + retdata);
                    }
                }).catch(function (errordata) {
                    let retdata = eval(errordata);
                    console.error("获取群主资料失败-----" + retdata);
                });
            }
            else {
                //如果是群主
                Synergy.Main.instance().getTimGroup().setOwnerNick(Synergy.Main.instance().getMyProfile().getNickName());
            }
        }
        /**
         * 初始化会议总人数
         * @param group
         */
        static initGroupMemberNum(group) {
            let memberNum = group.memberNum;
            //let memberNum = group.memberCount;
            this.updateGroupMemberNum(Number(memberNum), true);
            // Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.badgeId).setContent(memberNum);
        }
        /**
         * 更新参会者人数
         * @param updatecount 增加或减少的个数,减少传递负数
         * @param isInit 是否是初始化
         */
        static updateGroupMemberNum(updatecount, isInit) {
            if (isInit) {
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.badgeId).setContent(updatecount > 0 ? updatecount : 0);
            }
            else {
                let badgeBtn = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.badgeId);
                let prenum = badgeBtn.getContent();
                badgeBtn.setContent((Number(prenum) + updatecount) > 0 ? (Number(prenum) + updatecount) : 0);
            }
        }
        /**
         * 初始化参会者音视频权限
         * @param group
         * @param isHasInGroup
         */
        static initAttendeeVagAuth(group, isHasInGroup) {
            if (Synergy.Main.instance().getMyProfile().getUserID() != group.ownerID) {
                //如果参会者不是群主
                if (!isHasInGroup) {
                    //如果是参会者初次加会，默认开启音视频权限
                    Synergy.Main.instance().getMyProfile().setVagAuth(true);
                }
                else {
                    //如果之前已经在会议中，先判断是否清除了cookie，如果清除了，则默认再次开启音视频权限；否则按之前设定的音视频权限往下进行
                    if (M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_VAG_AUTH) == undefined ||
                        M3D.Utility.CookieHelper.getCookie(Synergy.SYNERGY_MY_PROFILE_VAG_AUTH) == '') {
                        Synergy.Main.instance().getMyProfile().setVagAuth(true);
                    }
                }
            }
            else {
                //如果参会者是群主，群主默认开启音视频权限
                Synergy.Main.instance().getMyProfile().setVagAuth(true);
            }
        }
        /**
         * 初始化参会者操作权限（根据角色信息来判断）
         * */
        static initAttendeeOperateAuth() {
            let that = this;
            Synergy.SynergyTim.instance().getGroupMemberProfile(Synergy.Main.instance().getTimGroup().getGroupID(), [Synergy.Main.instance().getMyProfile().getUserID()]).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    let myGroupProfile = retdata.attendees[0];
                    let myGroupRole = myGroupProfile.role;
                    switch (myGroupRole) {
                        case TIM.TYPES.GRP_MBR_ROLE_OWNER: //我是群主
                            Synergy.Main.instance().getMyProfile().setRole(Synergy.SYNERGY_MEMBER_ROLE_OWNER, true);
                            that.showHasOperateAuthRightMenuButton();
                            that.showHasOwnerRoleOperateButton();
                            break;
                        case TIM.TYPES.GRP_MBR_ROLE_ADMIN: //我是管理员
                            Synergy.Main.instance().getMyProfile().setRole(Synergy.SYNERGY_MEMBER_ROLE_ADMIN, true);
                            that.showHasOperateAuthRightMenuButton();
                            that.showHasCommonRoleOperateButton();
                            break;
                        case TIM.TYPES.GRP_MBR_ROLE_MEMBER: //我是普通成员
                            Synergy.Main.instance().getMyProfile().setRole(Synergy.SYNERGY_MEMBER_ROLE_MEMBER, false);
                            that.showNoOperateAuthRightMenuButton();
                            that.showHasCommonRoleOperateButton();
                            break;
                    }
                }
                else {
                    console.error("获取参会者资料失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                console.error("获取参会者资料失败-----" + retdata);
            });
        }
        /**
         * 初始化协同模型
         * @param group
         * @param isHasInGroup
         */
        static initSynergyModel(group, isHasInGroup) {
            if (Synergy.Main.instance().getMyProfile().getUserID() != group.ownerID || isHasInGroup) {
                //加入别人的会议或者加入自己的会议时需要获取公告同步模型
                let notification = group.notification; //群公告
                if (notification != null && notification.length > 0) {
                    Synergy.SynergyMessages.parseMsg(notification);
                    //清空视图列表
                    // TODO
                    //$("#sview_view_tree").html("");
                }
            }
        }
        /**
         * 初始化左下角权限申请提示列表，仅加载，不显示，并清空申请提示列表
         * */
        static initApplyTipList() {
            //仅群主需要加载
            if (Synergy.Main.instance().getMyProfile().getUserID() == Synergy.Main.instance().getTimGroup().getOwnerID()) {
                let applyTipList = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id);
                if (applyTipList == null) {
                    Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.ApplyTipList);
                }
                //清空申请提示列表
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ApplyTipList.List.id).removeSubComponents();
            }
        }
        /**
         * 打开左下角权限申请提示列表
         * */
        static openApplyTipList() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ApplyTipList.List.id);
        }
        /**
         * 关闭左下角权限申请提示列表
         * */
        static closeApplyTipList() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ApplyTipList.List.id);
        }
        /**
         * 初始化聊天界面，仅加载，不显示，清空聊天记录
         * */
        static initChatDialog() {
            let chatDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatDialog.Dialog.id);
            if (chatDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.ChatDialog);
                //设置聊天面板标题
                let groupname = Synergy.Main.instance().getTimGroup().getName();
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatDialog.Dialog.id).setTitle(groupname);
            }
            //清空聊天内容
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.ChatList.List.id).removeSubComponents();
        }
        /***
         *
         * 打开聊天界面
         * */
        static openChatDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.ChatDialog.Dialog.id);
            //关闭会议中控制面板界面
            this.closeMeetingControlDialog();
            //关闭申请人员列表界面
            this.closeMeetingApplysDialogOnly();
            //隐藏聊天提示红点
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.badgeId);
            //SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id).setHighLight(true);
        }
        /***
         *
         * 关闭聊天界面
         * */
        static closeChatDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.ChatDialog.Dialog.id);
            //SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceCharButton.id).setHighLight(false);
        }
        /**
         * 初始化会议中的控制界面，仅加载，不显示，并清空附件记录
         * */
        static initMeetingControlDialog() {
            let meetingControlDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlDialog.Dialog.id);
            if (meetingControlDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.MeetingControlDialog);
            }
            //初始化会议信息
            let groupname = Synergy.Main.instance().getTimGroup().getName();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlInfoFormThemeInput.Input.id).setValue(groupname);
            let grouphost = Synergy.Main.instance().getTimGroup().getOwnerNick();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlInfoFormHostInput.Input.id).setValue(grouphost);
            let groupid = Synergy.Main.instance().getTimGroup().getGroupID();
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlInfoFormGroupIdInput.Input.id).setValue(groupid);
            //清空附件记录
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlAttachmentList.List.id).removeSubComponents();
        }
        /***
         * 打开会议中的控制界面
         * */
        static openMeetingControlDialog() {
            if (this.isFirstOpen) {
                this.isFirstOpen = false;
                //先清空参会者列表
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).removeSubComponents();
                //初始化参会者列表
                Synergy.AlertUtill.showLoadingDialog();
                this.initAttendees(0);
                Synergy.AlertUtill.hideLoadingDialog();
            }
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingControlDialog.Dialog.id);
            //关闭聊天界面
            this.closeChatDialog();
            //关闭申请人员列表界面
            this.closeMeetingApplysDialogOnly();
            //SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id).setHighLight(true);
        }
        /**
         * 初始化参会者列表
         * */
        static initAttendees(offset) {
            let that = this;
            let timUserId = Synergy.Main.instance().getMyProfile().getUserID();
            Synergy.SynergyTim.instance().getGroupMemberList(Synergy.Main.instance().getTimGroup().getGroupID(), 20, offset).then(function (data) {
                let retdata = eval(data);
                if (retdata.code == '0') {
                    //console.log("获取群成员列表成功--开始初始化参会者列表---");
                    let memberList = retdata.attendees;
                    let memberitem;
                    let myRole = Synergy.Main.instance().getMyProfile().getRole();
                    for (let i = 0; i < memberList.length; i++) {
                        let groupMember = memberList[i];
                        memberitem = JSON.parse(JSON.stringify(Synergy.Dialog.MeetingControlAttendeeItem));
                        let memuserid = groupMember.userID;
                        let membernick = groupMember.nick;
                        let attendee = Synergy.Main.instance().getTimGroup().getAttendee(memuserid);
                        switch (groupMember.role) {
                            case TIM.TYPES.GRP_MBR_ROLE_OWNER: //群主
                                membernick += Synergy.Dialog.AttendeeItem.host;
                                break;
                            case TIM.TYPES.GRP_MBR_ROLE_ADMIN: //群管理员
                                switch (myRole) {
                                    case Synergy.SYNERGY_MEMBER_ROLE_OWNER:
                                        //显示关闭操作权限按钮
                                        memberitem.Item.subComponents[8].Button.isShow = true;
                                        if (attendee != "") {
                                            if (attendee.vagauth) {
                                                //显示关闭音视频全新按钮
                                                memberitem.Item.subComponents[5].Button.isShow = true;
                                            }
                                            else {
                                                //显示开启音视频全新按钮
                                                memberitem.Item.subComponents[6].Button.isShow = true;
                                            }
                                        }
                                        else {
                                            //如果清空缓存了，则attendee为空，默认开启音视频的，故显示关闭音视频按钮
                                            memberitem.Item.subComponents[5].Button.isShow = true;
                                        }
                                        break;
                                }
                                break;
                            case TIM.TYPES.GRP_MBR_ROLE_MEMBER: //群普通成员
                                switch (myRole) {
                                    case Synergy.SYNERGY_MEMBER_ROLE_OWNER:
                                        //显示开启操作权限按钮
                                        memberitem.Item.subComponents[7].Button.isShow = true;
                                        if (attendee != "") {
                                            if (attendee.vagauth) {
                                                //显示关闭音视频全新按钮
                                                memberitem.Item.subComponents[5].Button.isShow = true;
                                            }
                                            else {
                                                //显示开启音视频全新按钮
                                                memberitem.Item.subComponents[6].Button.isShow = true;
                                            }
                                        }
                                        else {
                                            //如果清空缓存了，则attendee为空，默认开启音视频的，故显示关闭音视频按钮
                                            memberitem.Item.subComponents[5].Button.isShow = true;
                                        }
                                        break;
                                }
                                break;
                        }
                        //如果该条参会者记录是自己
                        if (Number(timUserId) === Number(memuserid)) {
                            switch (myRole) {
                                case Synergy.SYNERGY_MEMBER_ROLE_OWNER:
                                    //显示关闭操作权限按钮
                                    memberitem.Item.subComponents[8].Button.isShow = true;
                                    //显示开启音频按钮
                                    memberitem.Item.subComponents[4].Button.isShow = true;
                                    //显示开启视频按钮
                                    memberitem.Item.subComponents[2].Button.isShow = true;
                                    break;
                                case Synergy.SYNERGY_MEMBER_ROLE_ADMIN:
                                    if (Synergy.Main.instance().getMyProfile().getVagAuth()) {
                                        //显示开启音频按钮
                                        memberitem.Item.subComponents[4].Button.isShow = true;
                                        //显示开启视频按钮
                                        memberitem.Item.subComponents[2].Button.isShow = true;
                                    }
                                    else {
                                        //显示申请音视频权限按钮
                                        memberitem.Item.subComponents[0].Button.isShow = true;
                                    }
                                    break;
                                case Synergy.SYNERGY_MEMBER_ROLE_MEMBER:
                                    if (Synergy.Main.instance().getMyProfile().getVagAuth()) {
                                        //显示开启音频按钮
                                        memberitem.Item.subComponents[4].Button.isShow = true;
                                        //显示开启视频按钮
                                        memberitem.Item.subComponents[2].Button.isShow = true;
                                    }
                                    else {
                                        //显示申请音视频权限按钮
                                        memberitem.Item.subComponents[0].Button.isShow = true;
                                    }
                                    break;
                            }
                            membernick += Synergy.Dialog.AttendeeItem.me;
                        }
                        that.initAttendeeItem(memberitem, memuserid, membernick);
                        Synergy.Main.instance().getSviewFrame().getUIManager().
                            getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).addSubComponents([memberitem]);
                    }
                    if (memberList.length == 20) {
                        that.initAttendees(Synergy.Main.instance().getSviewFrame().getUIManager().
                            getElement(Synergy.Dialog.MeetingControlAttendeeList.List.id).size());
                    }
                }
                else {
                    console.error("获取群成员列表失败-----" + retdata);
                }
            }).catch(function (errordata) {
                let retdata = eval(errordata);
                console.error("获取群成员列表失败-----" + retdata);
            });
        }
        /**
         * 初始化参会者信息
         * @param attendeeItem
         * @param memuserid
         * @param membernick
         */
        static initAttendeeItem(attendeeItem, memuserid, membernick) {
            attendeeItem.Item.memuserid = memuserid;
            attendeeItem.Item.text = membernick;
            attendeeItem.Item.id = Synergy.Dialog.AttendeeItem.idPre + memuserid;
            //申请音视频权限按钮
            attendeeItem.Item.subComponents[0].Button.id = Synergy.Dialog.AttendeeItem.ApplyVagBtnPre + memuserid;
            attendeeItem.Item.subComponents[0].Button.onClick = "Synergy.Main.instance().applyVagAuth()";
            //关闭视频按钮
            attendeeItem.Item.subComponents[1].Button.id = Synergy.Dialog.AttendeeItem.MuteVideoBtnPre + memuserid;
            //打开视频按钮
            attendeeItem.Item.subComponents[2].Button.id = Synergy.Dialog.AttendeeItem.UnMuteVideoBtnPre + memuserid;
            //关闭音频按钮
            attendeeItem.Item.subComponents[3].Button.id = Synergy.Dialog.AttendeeItem.MuteAudioBtnPre + memuserid;
            //开启音频按钮
            attendeeItem.Item.subComponents[4].Button.id = Synergy.Dialog.AttendeeItem.UnMuteAudioBtnPre + memuserid;
            //关闭音视频权限按钮
            attendeeItem.Item.subComponents[5].Button.id = Synergy.Dialog.AttendeeItem.MuteVagBtnPre + memuserid;
            attendeeItem.Item.subComponents[5].Button.onClick = "Synergy.Main.instance().forbidMemberVideoAndAudio(" + memuserid + ")";
            //开启音视频权限按钮
            attendeeItem.Item.subComponents[6].Button.id = Synergy.Dialog.AttendeeItem.UnmuteVagBtnPre + memuserid;
            attendeeItem.Item.subComponents[6].Button.onClick = "Synergy.Main.instance().allowMemberVideoAndAudio(" + memuserid + ")";
            //允许操作按钮
            attendeeItem.Item.subComponents[7].Button.id = Synergy.Dialog.AttendeeItem.AllowOperateBtnPre + memuserid;
            attendeeItem.Item.subComponents[7].Button.onClick = "Synergy.Main.instance().allowMember3DOperate(" + memuserid + ")";
            //禁止操作按钮
            attendeeItem.Item.subComponents[8].Button.id = Synergy.Dialog.AttendeeItem.BanOperateBtnPre + memuserid;
            attendeeItem.Item.subComponents[8].Button.onClick = "Synergy.Main.instance().forbidMember3DOperate(" + memuserid + ")";
        }
        /***
         *
         * 关闭会议中的控制界面
         * */
        static closeMeetingControlDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingControlDialog.Dialog.id);
            //SView.Windows.Button.highLightLabel(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id);
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.groupConferenceButton.id).setHighLight(false);
        }
        /**
         * 初始化会议中申请人员界面，仅加载，不显示，并清空申请人员列表
         * */
        static initMeetingApplysDialog() {
            let meetingApplysDialog = Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingApplysDialog.Dialog.id);
            if (meetingApplysDialog == null) {
                Synergy.Main.instance().getSviewFrame().getUIManager().load(Synergy.Dialog.MeetingApplysDialog);
            }
            //清空申请人员列表
            Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.MeetingApplyList.List.id).removeSubComponents();
        }
        /***
         *
         * 打开会议中申请人员界面
         * */
        static openMeetingApplysDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().showLabel(Synergy.Dialog.MeetingApplysDialog.Dialog.id);
            //关闭聊天界面
            this.closeChatDialog();
            //关闭主控制界面
            this.closeMeetingControlDialog();
        }
        /***
         *
         * 关闭会议中申请人员界面
         * */
        static closeMeetingApplysDialogOnly() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingApplysDialog.Dialog.id);
            //this.openMeetingControlDialog();
        }
        static closeMeetingApplysDialog() {
            Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.MeetingApplysDialog.Dialog.id);
            this.openMeetingControlDialog();
        }
        /**
         *  处理是否被设置为管理员的界面显隐
         * */
        static initAdminRoleInterface(isOwner) {
            if (Synergy.Main.instance().getMyProfile().isAllowOperate()) {
                //开启管理员权限
                this.hideAllRightMenuButton();
                this.showHasOperateAuthRightMenuButton();
            }
            else {
                /**
                 * 关闭管理员权限
                 * **/
                this.hideAllRightMenuButton();
                this.showNoOperateAuthRightMenuButton(isOwner);
                /**
                 * 关闭底部操作菜单
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.BottomMenu.BottomMenu.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.moreMenuButton.id).setHighLight(false);
                /**
                 * 关闭左边装配菜单
                 * **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AssemblyDialog.TreeDialog.id);
                Synergy.Main.instance().getSviewFrame().getUIManager().getElement(Synergy.Dialog.RightMenuBtns.assemblyTreeButton.id).setHighLight(false);
                /**
                 * 隐藏属性面板
                 **/
                Synergy.Main.instance().getSviewFrame().getUIManager().hideLabel(Synergy.Dialog.AttributeDialog.AttributeDialog.id);
                /**
                 * 关闭移动模型菜单todo
                 * */
                /**
                 * 关闭批注菜单todo
                 * */
                /**
                 * 关闭动画菜单todo
                 * */
                /**
                 * 关闭测量菜单todo
                 * */
                /**
                 * 关闭剖切菜单todo
                 * */
                /**
                 * 关闭爆炸菜单todo
                 * */
                //todo
            }
        }
    }
    //是否第一次打开控制界面
    UiManager.isFirstOpen = true;
    Synergy.UiManager = UiManager;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class AlertUtill {
        /**
         * 获取UIManager
         * */
        static getUIManager() {
            return Synergy.Main.instance().getSviewFrame().getUIManager();
        }
        /**
         * 显示loading框
         * @param text
         */
        static showLoadingDialog(text) {
            let loading = this.getUIManager().getElement(Synergy.Dialog.LoadingDialog.ResultDialog.id);
            if (loading != null) {
                this.getUIManager().removeLabel(Synergy.Dialog.LoadingDialog.ResultDialog.id);
            }
            if (!text) {
                text = SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Initialize; //初始化..."
            }
            let loadingDialog = JSON.parse(JSON.stringify(Synergy.Dialog.LoadingDialog));
            //let loadingDialog = Synergy.Dialog.LoadingDialog;
            loadingDialog.ResultDialog.detailContent = text;
            this.getUIManager().load(loadingDialog);
        }
        /**
         * 隐藏loading框
         * */
        static hideLoadingDialog() {
            this.getUIManager().removeLabel(Synergy.Dialog.LoadingDialog.ResultDialog.id);
        }
        /**
         * 弹框
         * @param text 描述内容
         * @param title 标题
         * @param type 弹框类型
         */
        static alert(title, text, type) {
            let alertd = this.getUIManager().getElement(Synergy.Dialog.AlertDialog.ResultDialog.id);
            if (alertd != null) {
                this.getUIManager().removeLabel(Synergy.Dialog.AlertDialog.ResultDialog.id);
            }
            let alertDialog = JSON.parse(JSON.stringify(Synergy.Dialog.AlertDialog));
            //let alertDialog = Synergy.Dialog.AlertDialog;
            //if (!type) {
            //    type = Synergy.Dialog.AlertDialogType.INFO;
            //}
            switch (type) {
                case Synergy.Dialog.AlertDialogType.INFO:
                    alertDialog.ResultDialog.icon.imgsrc = Synergy.Dialog.AlertDialogType.INFO_IMG;
                    break;
                case Synergy.Dialog.AlertDialogType.WARNING:
                    alertDialog.ResultDialog.icon.imgsrc = Synergy.Dialog.AlertDialogType.WARNING_IMG;
                    break;
                case Synergy.Dialog.AlertDialogType.ERROR:
                    alertDialog.ResultDialog.icon.imgsrc = Synergy.Dialog.AlertDialogType.ERROR_IMG;
                    break;
                case Synergy.Dialog.AlertDialogType.SUCCESS:
                    alertDialog.ResultDialog.icon.imgsrc = Synergy.Dialog.AlertDialogType.SUCCESS_IMG;
                    break;
                default:
                    alertDialog.ResultDialog.icon.imgsrc = Synergy.Dialog.AlertDialogType.INFO_IMG;
            }
            alertDialog.ResultDialog.titleContent = title;
            alertDialog.ResultDialog.detailContent = text;
            this.getUIManager().load(alertDialog);
        }
        /**
         * 关闭弹框
         * */
        static hideAlert() {
            this.getUIManager().removeLabel(Synergy.Dialog.AlertDialog.ResultDialog.id);
        }
        /**
         * 弹出顶部黑色提示框
         * @param text
         */
        static toast(text) {
            if (!Synergy.Main.instance().getTimGroup().isInGroup()) {
                return;
            }
            let tdialog = this.getUIManager().getElement(Synergy.Dialog.ToastDialog.Message.id);
            if (tdialog != null && tdialog.isShow()) {
                return;
            }
            if (tdialog != null) {
                this.getUIManager().removeLabel(Synergy.Dialog.ToastDialog.Message.id);
            }
            let toastDialog = JSON.parse(JSON.stringify(Synergy.Dialog.ToastDialog));
            //let loadingDialog = Synergy.Dialog.ToastDialog;
            toastDialog.Message.content = text;
            this.getUIManager().load(toastDialog);
            setTimeout(function () { Synergy.AlertUtill.hideToast(); }, 2000);
        }
        static hideToast() {
            this.getUIManager().removeLabel(Synergy.Dialog.ToastDialog.Message.id);
        }
        /**
         * 弹出确认按钮
         * @param title
         * @param text
         * @param confirmfun
         */
        static confirm(title, text, confirmFunName) {
            let confirmd = this.getUIManager().getElement(Synergy.Dialog.ConfirmDialog.Dialog.id);
            if (confirmd != null) {
                this.getUIManager().removeLabel(Synergy.Dialog.ConfirmDialog.Dialog.id);
            }
            let confirmDialog = JSON.parse(JSON.stringify(Synergy.Dialog.ConfirmDialog));
            //let confirmDialog = Synergy.Dialog.ConfirmDialog;
            confirmDialog.Dialog.title = title;
            confirmDialog.Dialog.content = text;
            confirmDialog.Dialog.buttons[0].Button.onClick = confirmFunName;
            this.getUIManager().load(confirmDialog);
        }
        static hideConfirm() {
            this.getUIManager().removeLabel(Synergy.Dialog.ConfirmDialog.Dialog.id);
        }
    }
    Synergy.AlertUtill = AlertUtill;
})(Synergy || (Synergy = {}));
var Synergy;
(function (Synergy) {
    class Utill {
        /**
        * 获取浏览器类型
        * */
        static getBroswer() {
            let sys = {
                edge: "", ie: "", firefox: "", chrome: "", opera: "", safari: ""
            };
            let ua = navigator.userAgent.toLowerCase();
            let s;
            (s = ua.match(/edge\/([\d.]+)/))
                ? (sys.edge = s[1])
                : (s = ua.match(/rv:([\d.]+)\) like gecko/))
                    ? (sys.ie = s[1])
                    : (s = ua.match(/msie ([\d.]+)/))
                        ? (sys.ie = s[1])
                        : (s = ua.match(/firefox\/([\d.]+)/))
                            ? (sys.firefox = s[1])
                            : (s = ua.match(/chrome\/([\d.]+)/))
                                ? (sys.chrome = s[1])
                                : (s = ua.match(/opera.([\d.]+)/))
                                    ? (sys.opera = s[1])
                                    : (s = ua.match(/version\/([\d.]+).*safari/))
                                        ? (sys.safari = s[1])
                                        : 0;
            if (sys.edge)
                return { broswer: 'Edge', version: sys.edge };
            if (sys.ie)
                return { broswer: 'IE', version: sys.ie };
            if (sys.firefox)
                return { broswer: 'Firefox', version: sys.firefox };
            if (sys.chrome)
                return { broswer: 'Chrome', version: sys.chrome };
            if (sys.opera)
                return { broswer: 'Opera', version: sys.opera };
            if (sys.safari)
                return { broswer: 'Safari', version: sys.safari };
            return { broswer: '', version: '0' };
        }
        /**
         * 格式化时间
         * */
        static formatDate(obj) {
            let time = new Date(parseInt(obj) * 1000);
            let y = String(time.getFullYear()); //年
            let m = time.getMonth() + 1; //月
            let mstr = String(m);
            if (m < 10) {
                mstr = '0' + String(m);
            }
            let d = time.getDate(); //日
            let dstr = String(d);
            if (d < 10) {
                dstr = '0' + String(d);
            }
            let h = time.getHours(); //时
            let hstr = String(h);
            if (h < 10) {
                hstr = '0' + String(h);
            }
            let mm = time.getMinutes(); //分
            let mmstr = String(mm);
            if (mm < 10) {
                mmstr = '0' + String(mm);
            }
            let s = time.getSeconds(); //秒
            let sstr = String(s);
            if (s < 10) {
                sstr = '0' + String(s);
            }
            let timeStr = y + "-" + mstr + "-" + dstr + " " + hstr + ":" + mmstr + ":" + sstr;
            return timeStr;
        }
        /**
         * 复制到剪贴板
         * */
        static copyToClipBoard() {
            let clipBoardContent = "";
            clipBoardContent += SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Theme + ":";
            clipBoardContent += Synergy.Main.instance().getSviewFrame().getUIManager()
                .getElement(Synergy.Dialog.MeetingControlInfoFormThemeInput.Input.id).getValue();
            clipBoardContent += ";" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Host + ":";
            clipBoardContent += Synergy.Main.instance().getSviewFrame().getUIManager()
                .getElement(Synergy.Dialog.MeetingControlInfoFormHostInput.Input.id).getValue();
            clipBoardContent += ";" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.GroupID + ":";
            clipBoardContent += Synergy.Main.instance().getSviewFrame().getUIManager()
                .getElement(Synergy.Dialog.MeetingControlInfoFormGroupIdInput.Input.id).getValue();
            clipBoardContent += ";" + SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.NoPassword + "";
            //Synergy.Main.instance().getSviewFrame().getUIManager()
            //    .getElement(Synergy.Dialog.InviteTextInput.Input.id).setValue(clipBoardContent);
            //navigator.clipboard.writeText(clipBoardContent).catch(e => console.error(e));
            let copy = (e) => {
                e.preventDefault();
                e.clipboardData.setData('text/plain', clipBoardContent);
                //console.log('-----------复制成功-------------')
                Synergy.AlertUtill.alert("", SView[SView.UIManager.languageInfo].languageObj.SweetAlert.TimCopyGroupInfo, Synergy.Dialog.AlertDialogType.SUCCESS);
                document.removeEventListener('copy', copy);
            };
            document.addEventListener('copy', copy);
            document.execCommand("Copy");
        }
    }
    Synergy.Utill = Utill;
})(Synergy || (Synergy = {}));
//# sourceMappingURL=SView.Synergy.js.map