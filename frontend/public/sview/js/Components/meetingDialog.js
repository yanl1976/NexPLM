var MeetingDialog = {
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
                    "onClick": "showMeetingDialog('meetingDialog',SettingMeetingDialog)",
                    "imgsrc": "images/meeting/MeetingSet.png"
                }
            },
            {
                "Button": {
                    "classes": [
                        "SView-button-iconBtn",
                        "meetingExist",
                    ],
                    "onClick": "closeDialog('meetingDialog', 'meetingMainMenu')",
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
                "onClick": "showMeetingDialog('meetingDialog',CreateMeetingDialog)"
            },
            {
                "classes": [
                    "SView-meeting-join"
                ],
                "imgsrc": "images/meeting/meeting_join.png",
                "onClick": "showMeetingDialog('meetingDialog',JoinMeetingDialog)"
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
}
//创建会议
var CreateMeetingDialog = {
    "Dialog": {
        "id": "create_meeting_dialog",
        "closeBtnOnClick": "closeDialog('create_meeting_dialog')",
        "title": "创建会议",
        "subComponents": [
            {
                Form: {
                    id: "creatMeetingForm",
                    classes: [
                        "SView-form",
                        "SView-form-middle"
                    ],
                    subComponents: [
                        {
                            WithBothInput: {
                                id: "creatMeeting",
                                warning: "主题不能为空！",
                                title: "主题",
                                subComponents: [
                                    {
                                        Input: {
                                            id: "creatMeetingInput",
                                            value: "主题",
                                            placeHolder: "请输入主题",
                                            onInput: "checkLength('creatMeetingInput')"
                                        }
                                    }
                                ]
                            }
                        },
                        {
                            WithBothInput: {
                                id: "creatMeeting",
                                title: "昵称",
                                warning: "昵称不能为空！",
                                subComponents: [
                                    {
                                        Input: {
                                            id: "creatMeetingNickName",
                                            value: "昵称",
                                            placeHolder: "请输入昵称",
                                            onInput: "checkLength('creatMeetingNickName')"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            },
        ],
        "buttons": [{
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmExit",
                "content": "确定",
                "onClick": ""
            }

        }]
    }
}
//加入会议
var JoinMeetingDialog = {
    "Dialog": {
        "id": "join_meeting_dialog",
        "hideTitle": true,
        "subComponents": [
            {
                Tabs: {
                    id: "join_meeting_tabs",
                    tabContent: [
                        {
                            dataActive: "true", // 当前选中标签页
                            title: "会议列表",
                            dataClass: "tabTest_1",
                            subComponentsId: "tabTest_1", // 与dataClass保持一致，控制标签页内容显隐
                            subComponents: [
                                {
                                    Table: {
                                        id: "tableTest",
                                        classes: [
                                            "SView-table", // 若不写，默认类名为"SView-table"
                                        ],
                                        cellTitle: ["主题", "会议号", "主持人", "创建时间"], // 表格标题
                                        cellContent: [
                                            ["LYA-AL00 的会议", "65558424", "LYA-AL00", "2022-03-23 15:48:56"],
                                            ["Administrator的会议", "34739797", "DE", "2021-08-02 16:24:05"],
                                        ] // 表格内容
                                    }
                                }
                            ]
                        },
                        {
                            title: "会议号",
                            dataClass: "tabTest_2",
                            subComponentsId: "tabTest_2",
                            subComponents: [{
                                Form: {
                                    id: "creatMeetingForm",
                                    classes: [
                                        "SView-form",
                                        "SView-form-middle"
                                    ],
                                    subComponents: [
                                        {
                                            WithBothInput: {
                                                id: "creatMeeting",
                                                warning: "会议号不能为空！",
                                                title: "会议号",
                                                subComponents: [
                                                    {
                                                        Input: {
                                                            id: "creatMeetingInput",
                                                            value: "会议号",
                                                            placeHolder: "请输入会议号",
                                                            onInput: "checkLength('creatMeetingInput')"
                                                        }
                                                    }
                                                ]
                                            }
                                        },
                                        {
                                            WithBothInput: {
                                                id: "creatMeeting",
                                                title: "昵称",
                                                warning: "昵称不能为空！",
                                                subComponents: [
                                                    {
                                                        Input: {
                                                            id: "creatMeetingNickName",
                                                            value: "昵称",
                                                            placeHolder: "请输入昵称",
                                                            onInput: "checkLength('creatMeetingNickName')"
                                                        }
                                                    }
                                                ]
                                            }
                                        }
                                    ]
                                }
                            }]
                        }
                    ] // 标签页内容
                }
            },
        ],
        "buttons": [{
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmExit",
                "content": "加入会议",
                "onClick": ""
            }
        }, {
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-default"
                ],
                "id": "confirmExit",
                "content": "取消",
                "onClick": "closeDialog('join_meeting_dialog')",
            }
        }]
    }
}
//会议设置
var SettingMeetingDialog = {
    "Dialog": {
        "id": "setting_meeting_dialog",
        "closeBtnOnClick": "closeDialog('setting_meeting_dialog')",
        "title": "会议设置",
        "subComponents": [
            {
                Form: {
                    id: "setMeetingForm",
                    classes: [
                        "SView-form",
                        "SView-form-middle"
                    ],
                    subComponents: [
                        {
                            WithBothInput: {
                                id: "setMeeting",
                                title: "昵称",
                                warning: "昵称不能为空！",
                                subComponents: [
                                    {
                                        Input: {
                                            id: "setMeetingNickName",
                                            value: "昵称",
                                            placeHolder: "请输入昵称",
                                            onInput: "checkLength('setMeetingNickName')"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            },
        ],
        "buttons": [{
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmExit",
                "content": "确定",
                "onClick": ""
            }

        }, {
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmExit",
                "content": "重置",
                "onClick": ""
            }

        }]
    }
}
//参会人列表
var AttendeeList = {
    "AttendeeList": {
        "id": "attendeeList",
        "topBottons": [{
            Button: {
                "type": "iconTextButton_top",
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/forbidden_open.png",
                "id": "forbidden_open",
                "content": "全员禁言",
                "onClick": ""
            }
        }, {
            Button: {
                "type": "iconTextButton_top",
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/meeting_role_presentor.png",
                "id": "meeting_role_presentor",
                "content": "允许全员操作",
                "onClick": "",
            }
        }, {
            Button: {
                "type": "iconTextButton_top",
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/show_apply.png",
                "id": "show_apply",
                "content": "申请人员",
                "onClick": "",
            }
        }, {
            Button: {
                "type": "iconTextButton_top",
                "classes": [
                    "SView-button-attendee",
                ],
                "imgsrc": "images/meeting/add_attender.png",
                "id": "add_attender",
                "content": "邀请参会",
                "onClick": "",
            }
        }],
        attendeeInfo: [
            {
                membernick: "user_11212(我)",
                memuserid: 125,
                buttons: [
                    {
                        Button: {
                            "type": "iconButton",
                            "classes": [
                                "SView-button-iconBtn",
                            ],
                            "imgsrc": "images/meeting/meeting_video_common.png",
                            "id": "meeting_video_common",
                            "title": "开启视频",
                            "onClick": "",
                        }
                    },
                    {
                        Button: {
                            "type": "iconButton",
                            "classes": [
                                "SView-button-iconBtn",
                            ],
                            "imgsrc": "images/meeting/meeting_audio_common.png",
                            "id": "meeting_audio_common",
                            "title": "开启音频",
                            "onClick": "",
                        }
                    }
                ]
            },
            {
                membernick: "user_11212",
                memuserid: 125,
                buttons: [
                ]
            },
            {
                membernick: "user_11212(我)",
                memuserid: 125,
                buttons: [
                    {
                        Button: {
                            "type": "iconButton",
                            "classes": [
                                "SView-button-iconBtn",
                            ],
                            "imgsrc": "images/meeting/vga_apply.png",
                            "id": "vga_apply",
                            "title": "申请开启音视频",
                            "onClick": "",
                        }
                    }
                ]
            },
        ]
    }
}
var MeetingInfo = {
    Form: {
        id: "meetingInfoForm",
        classes: [
            "SView-form",
            "SView-form-middle"
        ],
        subComponents: [
            {
                WithTitleInput: {
                    id: "meetingInfo",
                    title: "主题",
                    subComponents: [
                        {
                            Input: {
                                id: "meetingInfoNickName",
                                readOnly: true,
                                value: "user_11065的会议",
                            }
                        }
                    ]
                }
            },
            {
                WithTitleInput: {
                    id: "meetingInfo1",
                    title: "主持人",
                    subComponents: [
                        {
                            Input: {
                                id: "meetingInfoHost",
                                readOnly: true,
                                value: "user_11065",
                            }
                        }
                    ]
                }
            },
            {
                WithTitleInput: {
                    id: "meetingInfo2",
                    title: "会议号",
                    subComponents: [
                        {
                            Input: {
                                id: "meetingInfoNumber",
                                readOnly: true,
                                value: "69927209",
                            }
                        }
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
                    "content": "邀请参会",
                    "onClick": ""
                }
            }

        ]
    }
}
//会议附件Item
var MeetingFileItem = {
    Item: {
        itemType: "imgItem",
        name: "user_121212",
        time: "2022-10-11",
        imgsrc: "images/meeting/meeting_audio_common.png"
    }

}
//会议附件列表
var MeetingFileList = {
    List: {
        classes: [
            "SView-imgItem-List"
        ],
        subComponents: [
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
            MeetingFileItem,
        ] // 标签页内容
    }
}
//协同会议中的弹窗
var InMeetingDialog = {
    "Dialog": {
        "id": "in_meeting_dialog",
        "hideTitle": true,
        "subComponents": [
            {
                Tabs: {
                    id: "in_meeting_tabs",
                    tabContent: [
                        {
                            dataActive: "true", // 当前选中标签页
                            title: "参会人",
                            dataClass: "in_meeting_tab1",
                            subComponentsId: "in_meeting_tab1", // 与dataClass保持一致，控制标签页内容显隐
                            subComponents: [
                                AttendeeList
                            ]
                        },
                        {
                            title: "会议附件",
                            dataClass: "in_meeting_tab2",
                            subComponentsId: "in_meeting_tab2",
                            subComponents: [
                                MeetingFileList,
                                {
                                    Button: {
                                        "type": "iconButton",
                                        "classes": [
                                            "SView-button-iconBtn",
                                            "SView-file-button"
                                        ],
                                        "imgsrc": "images/meeting/meeting_video_common.png",
                                        "id": "meeting_video_common",
                                        "title": "上传文件",
                                        "onClick": "",
                                    }
                                },
                            ]
                        },
                        {
                            title: "会议信息",
                            dataClass: "in_meeting_tab3",
                            subComponentsId: "in_meeting_tab3",
                            subComponents: [{
                                Form: {
                                    id: "creatMeetingForm",
                                    classes: [
                                        "SView-form",
                                        "SView-form-middle"
                                    ],
                                    subComponents: [
                                        MeetingInfo
                                    ]
                                }
                            }]
                        }
                    ] // 标签页内容
                }
            },
        ],
        "buttons": [{
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-primary"
                ],
                "id": "confirmExit",
                "content": "退出会议",
                "onClick": ""
            }
        }, {
            Button: {
                "type": "basicButton",
                "classes": [
                    "SView-button",
                    "SView-button-default"
                ],
                "id": "confirmExit",
                "content": "取消",
                "onClick": "closeDialog('in_meeting_dialog')",
            }
        }]
    }
}
//申请列表Item
var ApplyItem = {
    Item: {
        itemType: "labelItem",
        text: "user_121212",
        subComponents: [
            {
                Button: {
                    "type": "iconButton",
                    "classes": [
                        "SView-button-iconBtn",
                    ],
                    "imgsrc": "images/meeting/meeting_video_common.png",
                    "id": "meeting_video_common",
                    "title": "开启视频",
                    "onClick": "",
                }
            },
            {
                Button: {
                    "type": "iconButton",
                    "classes": [
                        "SView-button-iconBtn",
                    ],
                    "imgsrc": "images/meeting/meeting_audio_common.png",
                    "id": "meeting_audio_common",
                    "title": "开启音频",
                    "onClick": "",
                }
            }
        ]
    }

}
//申请列表
var ApplyList = {
    List: {
        id: "apply_meeting_list",
        subComponents: [
            ApplyItem
        ] // 标签页内容
    }
}
//申请列表弹窗
var ApplyMeetingDialog = {
    "Dialog": {
        "id": "apply_meeting_dialog",
        "closeBtnOnClick": "closeDialog('apply_meeting_dialog')",
        "title": "申请列表",
        "subComponents": [
            ApplyList
        ]
    }
}
//聊天窗口Item
var LeftChatItem = {
    Item: {
        itemType: "leftChatItem",
        name: "user_121212",
        text: "您好，咨询您一个问题",
        imgsrc: "images/meeting/sview_conference_chart_other.png",
        time: "2022-01-11 17:08:33"
    }

}
//聊天窗口Item
var RightChatItem = {
    Item: {
        itemType: "rightChatItem",
        name: "",
        text: "好的，在呢，您请说！",
        imgsrc: "images/meeting/sview_conference_chart_me.png",
        time: "2022-01-11 17:08:33"
    }

}
//聊天列表
var ChatList = {
    List: {
        id: "chat_list",
        classes: [
            "SView-chat-list"
        ],
        subComponents: [
            LeftChatItem,
            RightChatItem
        ] // 标签页内容
    }
}
//申请列表弹窗
var ChatDialog = {
    "Dialog": {
        "id": "chat_dialog",
        "closeBtnOnClick": "closeDialog('chat_dialog')",
        "title": "user_12254",
        "subComponents": [
            ChatList
        ],
        buttons: [
            {
                Input: {
                    id: "searchContentInput",
                    classes: [
                        "SView-complexInput",

                    ],
                    value: "",
                    placeHolder: "请输入搜索内容",
                    autoComplete: "off"
                },
            },
            {
                InputButton: {
                    id: "searchBtn",
                    onClick: "checkLength('searchContentInput','assemblyTreeDialog')",
                    imgsrc: "",
                    btnTitle: "发送",
                    warning: "发送内容不能为空！"
                }
            }
        ]
    }
}
//视频item
var VideoItem = {
    VideoItem: {
        id: "meetingVideo",
        name: "我",
        subComponents: [
            {
                Button: {
                    "type": "iconButton",
                    "classes": [
                        "SView-button-iconBtn",
                    ],
                    "imgsrc": "images/meeting/sview_conference_menu_audio_off.png",
                    "id": "meeting_audio_common",
                    "title": "开启音频",
                    "onClick": "",
                }
            },
            {
                Button: {
                    "type": "iconButton",
                    "classes": [
                        "SView-button-iconBtn",
                    ],
                    "imgsrc": "images/meeting/sview_conference_menu_video_off.png",
                    "id": "meeting_video_common",
                    "title": "开启视频",
                    "onClick": "",
                }
            },
        ]
    }
}
//视频窗口
var VideoDialog = {
    VideoDialog: {
        id: "meetingVideoDialog",
        subComponents: [
            VideoItem,
            VideoItem,
            VideoItem
        ]
    }
}
//权限提示框
var PermissionMessage = {
    PermissionMessage: {
        id: "PermissionMessageDialog",
        content: "user<br>申请视频权限",
        subComponents: [
            {
                Button: {
                    type: "basicButton",
                    content: "同意",
                    classes: [
                        "SView-button",
                    ]
                }
            }
        ]
    }
}