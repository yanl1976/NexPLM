var ExitDialog = {
    "Dialog": {
        "classes": [
            "SView-dialog",
        ],
        "id": "confirm-exit",
        "content": "",
        contentImg: "images/info/confirm.png",
        "showBack": true,
        //SView[SView.UIManager.languageInfo].languageObj.Prompt.Prompt
        "title": "SView",
        "buttons": [

            {
                Button: {
                    "type": "basicButton",
                    "classes": [
                        "SView-button",
                        "SView-button-primary"
                    ],
                    "id": "confirmExit",
                    "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Confirm,
                    "onClick": ""
                }
            },
            {
                Button: {
                    "type": "basicButton",
                    "classes": [
                        "SView-button",
                        "SView-button-default"
                    ],
                    "id": "cancelExit",
                    "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Cancel,
                    "onClick": "closeDialog('confirm-exit')"
                }
            }
        ]
    }
}

var DeleteDialog = {
    "Dialog": {
        "classes": [
            "SView-dialog",
        ],
        "id": "confirm-delete",
        "content": "确认删除吗？",
        "onClick": "closeDialog('confirm-delete')",
        "showBack": true,
        "title": "提示",
        "buttons": [{
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmDelete",
                "content": "确定",
                "onClick": ""
            }

        }, {
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-default"
                ],
                "id": "cancelDelete",
                "content": "取消",
                "onClick": "closeDialog('confirm-delete')"
            }

        }]
    }
}

// 会议设置弹窗
var MeetingNickName = {
    id: "meetingNickNameInput",
    value: "user_123",
    placeHolder: "请输入昵称",
    autoComplete: "off",
    onInput: "checkLength('meetingNickNameInput')",
}
var MeetingSettingDialog = {
    Dialog: {
        classes: [
            "SView-dialog",
        ],
        id: "meetingSetting",
        onClick: "closeMeetingSetting()",
        showBack: false,
        title: "会议设置",
        subComponents: [
            {
                Form: {
                    id: "meetingSettingForm",
                    subComponents: [
                        {
                            WithBothInput: {
                                id: "meetingNickName",
                                classes: [
                                    "SView-input-withTitle",
                                    "SView-input-warning",
                                ],
                                title: "昵称",
                                warning: "输入昵称不能为空！",
                                subComponents: [
                                    {
                                        Input: MeetingNickName
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        ],
        buttons: [{
            Button: {
                type: "basicButton",
                classes: [
                    "SView-button",
                    "SView-button-primary"
                ],
                id: "saveMeetingSetting",
                content: "保存",
                onClick: "saveMeetingSetting()"
            }
        }, {
            Button: {
                type: "basicButton",
                classes: [
                    "SView-button",
                    "SView-button-default"
                ],
                id: "resetMeetingSetting",
                content: "重置",
                onClick: "resetMeetingSetting('meetingNickName')"
            }
        }]
    }
}

// 创建会议弹窗
var MeetingTitle = {
    id: "meetingTitleInput",
    value: "会议主题",
    placeHolder: "请输入会议主题",
    autoComplete: "off",
    onInput: "checkLength('meetingTitleInput')",
}
var CreateMeetingDialog = {
    Dialog: {
        classes: [
            "SView-dialog",
        ],
        id: "createMeeting",
        onClick: "closeCreateMeeting()",
        showBack: false,
        title: {
            classes: [
                "SView-dialog-title-name"
            ],
            content: "创建会议"
        },
        subComponents: [
            {
                Form: {
                    id: "createMeetingForm",
                    subComponents: [
                        {
                            WithBothInput: {
                                id: "meetingTitle",
                                classes: [
                                    "SView-input-withTitle",
                                    "SView-input-warning",
                                ],
                                title: "主题",
                                warning: "输入主题不能为空！",
                                subComponents: [
                                    {
                                        Input: MeetingTitle,
                                    }
                                ]
                            }
                        },
                        {
                            WithBothInput: {
                                id: "meetingNickName",
                                classes: [
                                    "SView-input-withTitle",
                                    "SView-input-warning",
                                ],
                                title: "昵称",
                                warning: "输入昵称不能为空！",
                                subComponents: [
                                    {
                                        Input: MeetingNickName
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        ],
        buttons: [{
            type: "basicButton",
            classes: [
                "SView-button",
                "SView-button-primary"
            ],
            id: "createMeeting",
            content: "创建会议",
            // "onClick": "createMeeting()"
        }]
    }
}

// 加入会议弹窗
var MeetingNum = {
    id: "meetingNumInput",
    value: "",
    placeHolder: "请输入会议号",
    autoComplete: "off",
    onInput: "checkLength('meetingNumInput')",
}
var JoinMeetingDialog = {
    Dialog: {
        classes: [
            "SView-dialog",
        ],
        id: "joinMeeting",
        onClick: "closeJoinMeeting()",
        showBack: false,
        title: {
            classes: [
                "SView-dialog-title-name"
            ],
            content: "加入会议"
        },
        contentClasses: [
            "SView-joinMeeting-dialog",
            "SView-dialog-content",
        ],
        subComponents: [
            {
                Tabs: {
                    classes: [
                        "SView-tabs"
                    ],
                    id: "joinMeetingTabs",
                    tabContent: [
                        {
                            dataActive: "true",
                            title: "会议列表",
                            dataClass: "SView-meetingList",
                            onClick: "changeTabs('joinMeetingTabs','SView-meetingList')",
                            subComponents: [
                                {
                                    Table: {
                                        id: "SView-meetingList",
                                        classes: [
                                            "SView-table",
                                        ]
                                    },
                                }
                            ]
                        },
                        {
                            dataActive: "false",
                            title: "会议号",
                            dataClass: "SView-meetingNum",
                            onClick: "changeTabs('joinMeetingTabs', 'SView-meetingNum')",
                            subComponents: [
                                {
                                    Form: {
                                        id: "SView-meetingNum",
                                        classes: [
                                            "SView-form",
                                        ],
                                        subComponents: [
                                            {
                                                WithBothInput: {
                                                    id: "meetingNum",
                                                    classes: [
                                                        "SView-input-withTitle",
                                                        "SView-input-warning",
                                                    ],
                                                    title: "会议号",
                                                    warning: "输入主题不能为空！",
                                                    subComponents: [
                                                        {
                                                            Input: MeetingNum,
                                                        }
                                                    ]
                                                }
                                            },
                                            {
                                                WithBothInput: {
                                                    id: "meetingNickName",
                                                    classes: [
                                                        "SView-input-withTitle",
                                                        "SView-input-warning",
                                                    ],
                                                    title: "昵称",
                                                    warning: "输入昵称不能为空！",
                                                    subComponents: [
                                                        {
                                                            Input: MeetingNickName
                                                        }
                                                    ]
                                                }
                                            }
                                        ]
                                    }

                                }
                            ]
                        },
                    ]
                }
            },
        ],
        buttons: [{
            type: "basicButton",
            classes: [
                "SView-button",
                "SView-button-primary"
            ],
            id: "joinSelectMeeting",
            content: "加入会议",
            // "onClick": "saveMeetingSetting()"
        }, {
            type: "basicButton",
            classes: [
                "SView-button",
                "SView-button-default"
            ],
            id: "closeJoinMeeting",
            content: "取消",
            onClick: "closeJoinMeeting()"
        }]
    }
}