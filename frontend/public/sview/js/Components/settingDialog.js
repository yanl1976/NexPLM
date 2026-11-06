function initSetting() {
    //文档通用
    var DocumentCommon = {
        List: {
            id: "documentCommon",
            primaryTitle: "通用",
            classes: [
                "SView-setting-List"
            ],
            subComponents: [
                {
                    Item: {
                        itemType: "labelItem",
                        text: "观察模式",
                        id: "observationModeItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('observationMode', 'number'),
                                    id: "observationMode",
                                    onChange: "selectChange('observationMode')",
                                    options: [
                                        {
                                            Option: {
                                                value: 3,
                                                content: "自由观察",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "动态观察",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "约束观察",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "向上方向",
                        id: "verticalAxisItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('verticalAxis', 'number'),
                                    id: "verticalAxis",
                                    onChange: "selectChange('verticalAxis')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "+x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "-x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "+y",
                                                selected: "selected"
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "-y",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "+z",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "-z",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "选中效果",
                        id: "selectedEffectItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('selectedEffect', 'number'),
                                    id: "selectedEffect",
                                    onChange: "selectChange('selectedEffect')",
                                    options: [
                                        {
                                            Option: {
                                                value: 1,
                                                content: "轮廓",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "颜色",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "高亮",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "突出显示",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "Xray",
                                            }
                                        },
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isUseCullFaceItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isUseCullFace",
                                    type: "labelBack",
                                    value: getSettingValue('selectedEffect', 'boolean'),
                                    onChange: "checkBoxChange('selectedEffect')",
                                    label: "背面剔除",
                                    placeholder: "",
                                    checkboxId: "isUseCullFaceCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                }
            ]
        }
    }
    //文档漫游
    var DocumentRoaming = {
        List: {
            id: "documentRoaming",
            primaryTitle: "漫游",
            classes: [
                "SView-setting-List"
            ],
            subComponents: [

                {
                    Item: {
                        id: "isOpenRoamingItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isOpenRoaming",
                                    type: "labelBack",
                                    value: getSettingValue('isOpenRoaming', 'boolean'),
                                    onChange: "checkBoxChange('isOpenRoaming')",
                                    label: "漫游",
                                    placeholder: "",
                                    checkboxId: "isOpenRoamingCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "漫游速度",
                        id: "roamingSpeedItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('roamingSpeed', 'number'),
                                    id: "roamingSpeed",
                                    onChange: "selectChange('roamingSpeed')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "0.05x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "0.25x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "0.5x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: "3",
                                                content: "0.75x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "1x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "2x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 6,
                                                content: "4x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 7,
                                                content: "6x",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "视野大小",
                        id: "fieldSizeItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('fieldSize', 'number'),
                                    id: "fieldSize",
                                    onChange: "selectChange('fieldSize')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "50°",
                                                selected: "selected"
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "60°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "70°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "80°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "90°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "100°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 6,
                                                content: "110°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 7,
                                                content: "120°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 8,
                                                content: "130°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 9,
                                                content: "140°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 10,
                                                content: "150°",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },

            ]
        }
    }
    //系统通用
    var SystemCommon = {
        List: {
            id: "systemCommon",
            primaryTitle: "通用",
            secondaryTitle: "通用设置",
            classes: [
                "SView-setting-List"
            ],
            subComponents: [
                {
                    Item: {
                        id: "isMultipleSelectedItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isMultipleSelected",
                                    type: "labelBack",
                                    value: getSettingValue('isMultipleSelected', 'boolean'),
                                    onChange: "checkBoxChange('isMultipleSelected')",
                                    label: "多选",
                                    placeholder: "",
                                    checkboxId: "isMultipleSelectedCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isSpecifyRotationItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isSpecifyRotation",
                                    type: "labelBack",
                                    value: getSettingValue('isSpecifyRotation', 'boolean'),
                                    onChange: "checkBoxChange('isSpecifyRotation')",
                                    label: "指定旋转",
                                    placeholder: "",
                                    checkboxId: "isSpecifyRotationCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isSelectedCenterItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isSelectedCenter",
                                    type: "labelBack",
                                    value: getSettingValue('isSelectedCenter', 'boolean'),
                                    onChange: "checkBoxChange('isSelectedCenter')",
                                    label: "选中居中",
                                    placeholder: "",
                                    checkboxId: "isSelectedCenterCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isContinuousRotationItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isContinuousRotation",
                                    type: "labelBack",
                                    value: getSettingValue('isContinuousRotation', 'boolean'),
                                    onChange: "checkBoxChange('isContinuousRotation')",
                                    label: "持续旋转",
                                    placeholder: "",
                                    checkboxId: "isContinuousRotationCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isOnlyLoadAssyItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isOnlyLoadAssy",
                                    type: "labelBack",
                                    value: getSettingValue('isOnlyLoadAssy', 'boolean'),
                                    onChange: "checkBoxChange('isOnlyLoadAssy')",
                                    label: "打开时仅打开装配结构",
                                    placeholder: "",
                                    checkboxId: "isOnlyLoadAssyCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isReverseWheelDirectionItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isReverseWheelDirection",
                                    type: "labelBack",
                                    value: getSettingValue('isReverseWheelDirection', 'boolean'),
                                    onChange: "checkBoxChange('isReverseWheelDirection')",
                                    label: "多选",
                                    placeholder: "",
                                    checkboxId: "isReverseWheelDirectionCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isMultipleSelectedItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isMultipleSelected",
                                    type: "labelBack",
                                    value: getSettingValue('isMultipleSelected', 'boolean'),
                                    onChange: "checkBoxChange('isMultipleSelected')",
                                    label: "多选",
                                    placeholder: "",
                                    checkboxId: "isMultipleSelectedCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isMultipleSelectedItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isMultipleSelected",
                                    type: "labelBack",
                                    value: getSettingValue('isMultipleSelected', 'boolean'),
                                    onChange: "checkBoxChange('isMultipleSelected')",
                                    label: "多选",
                                    placeholder: "",
                                    checkboxId: "isMultipleSelectedCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isMultipleSelectedItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isMultipleSelected",
                                    type: "labelBack",
                                    value: getSettingValue('isMultipleSelected', 'boolean'),
                                    onChange: "checkBoxChange('isMultipleSelected')",
                                    label: "多选",
                                    placeholder: "",
                                    checkboxId: "isMultipleSelectedCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "向上方向",
                        id: "verticalAxisItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('verticalAxis', 'number'),
                                    id: "verticalAxis",
                                    onChange: "selectChange('verticalAxis')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "+x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "-x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "+y",
                                                selected: "selected"
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "-y",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "+z",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "-z",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "选中效果",
                        id: "selectedEffectItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('selectedEffect', 'number'),
                                    id: "selectedEffect",
                                    onChange: "selectChange('selectedEffect')",
                                    options: [
                                        {
                                            Option: {
                                                value: 1,
                                                content: "轮廓",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "颜色",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "高亮",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "突出显示",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "Xray",
                                            }
                                        },
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        id: "isUseCullFaceItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isUseCullFace",
                                    type: "labelBack",
                                    value: getSettingValue('selectedEffect', 'boolean'),
                                    onChange: "checkBoxChange('selectedEffect')",
                                    label: "背面剔除",
                                    placeholder: "",
                                    checkboxId: "isUseCullFaceCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                }
            ]
        }
    }
    //文档漫游
    var DocumentRoaming = {
        List: {
            id: "documentRoaming",
            primaryTitle: "漫游",
            classes: [
                "SView-setting-List"
            ],
            subComponents: [

                {
                    Item: {
                        id: "isOpenRoamingItem",
                        itemType: "labelItem",
                        subComponents: [
                            {
                                Checkbox: {
                                    id: "isOpenRoaming",
                                    type: "labelBack",
                                    value: getSettingValue('isOpenRoaming', 'boolean'),
                                    onChange: "checkBoxChange('isOpenRoaming')",
                                    label: "漫游",
                                    placeholder: "",
                                    checkboxId: "isOpenRoamingCheckbox" // 复选框input标签id
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "漫游速度",
                        id: "roamingSpeedItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('roamingSpeed', 'number'),
                                    id: "roamingSpeed",
                                    onChange: "selectChange('roamingSpeed')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "0.05x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "0.25x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "0.5x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: "3",
                                                content: "0.75x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "1x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "2x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 6,
                                                content: "4x",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 7,
                                                content: "6x",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },
                {
                    Item: {
                        itemType: "labelItem",
                        text: "视野大小",
                        id: "fieldSizeItem",
                        subComponents: [
                            {
                                Select: {
                                    value: getSettingValue('fieldSize', 'number'),
                                    id: "fieldSize",
                                    onChange: "selectChange('fieldSize')",
                                    options: [
                                        {
                                            Option: {
                                                value: 0,
                                                content: "50°",
                                                selected: "selected"
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 1,
                                                content: "60°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 2,
                                                content: "70°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 3,
                                                content: "80°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 4,
                                                content: "90°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 5,
                                                content: "100°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 6,
                                                content: "110°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 7,
                                                content: "120°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 8,
                                                content: "130°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 9,
                                                content: "140°",
                                            }
                                        },
                                        {
                                            Option: {
                                                value: 10,
                                                content: "150°",
                                            }
                                        }
                                    ]
                                }
                            },
                        ]
                    }
                },

            ]
        }
    }
    //左侧tabs
    var SystemTabs = {
        Tabs: {
            id: "settingSystemtMenuTabs",
            type: "vertical",
            tabContent: [
                {
                    dataActive: "true",
                    title: "通用",
                    dataClass: "SView-system-common",
                    onClick: "",
                    subComponents: [
                        //DocumentCommon,
                        //DocumentCommon1
                    ]
                },
                {
                    title: "显示",
                    dataClass: "SView-system-show",
                    onClick: "",
                    subComponents: [

                    ]
                },
                {
                    title: "性能",
                    dataClass: "SView-system-performance",
                    onClick: "",
                    subComponents: [

                    ]
                },
                {
                    title: "调试",
                    dataClass: "SView-system-debug",
                    onClick: "",
                    subComponents: [

                    ]
                },
                {
                    title: "功能",
                    dataClass: "SView-system-command",
                    onClick: "",
                    subComponents: [

                    ]
                },
                {
                    title: "日志",
                    dataClass: "SView-system-log",
                    onClick: "",
                    subComponents: [

                    ]
                },
            ]
        }
    }
    //左侧tabs
    var DocumentTabs = {
        Tabs: {
            id: "settingDocumentMenuTabs",
            type: "vertical",
            tabContent: [
                {
                    dataActive: "true",
                    title: "通用",
                    dataClass: "SView-document-common",
                    onClick: "scrollToFun('SView-document-common','settingDocumentMenuTabs')",
                    subComponents: [
                        DocumentCommon,
                    ]
                },
                {
                    title: "漫游",
                    dataClass: "SView-document-roaming",
                    onClick: "scrollToFun('SView-document-roaming','settingDocumentMenuTabs') ",
                    subComponents: [
                        DocumentRoaming,
                    ]
                }
            ]
        }
    }
    //头部切换tab
    var HeaderTabs = {
        Tabs: {
            id: "settingMenuTabs",
            tabContent: [
                {
                    dataActive: "true",
                    title: "文档",
                    dataClass: "SView-settingDocument",
                    subComponents: [
                        DocumentTabs

                    ]
                },
                {
                    title: "系统",
                    dataClass: "SView-settingSystem",
                    subComponents: [
                        SystemTabs
                    ]
                }
            ]
        }
    }
    var SettingDialogInfo = {
        Dialog: {
            "hideTitle": true,
            classes: [
                "SView-dialog",
                "SView-settingMenu-dialog"
            ],
            showBack: true,
            id: "settingMenuDialog",
            closeBtnOnClick: "closeDialog('settingMenuDialog')",
            subComponents: [
                HeaderTabs
            ],
            buttons: [{
                Button: {
                    type: "basicButton",
                    classes: [
                        "SView-button",
                        "SView-button-default"
                    ],
                    id: "confirmSetting",
                    content: "恢复默认设置",
                }
            }, {
                Button: {
                    type: "basicButton",
                    classes: [
                        "SView-button",
                        "SView-button-primary"
                    ],
                    id: "confirmSetting",
                    content: "导入",
                }
            }, {
                Button: {
                    type: "basicButton",
                    classes: [
                        "SView-button",
                        "SView-button-primary"
                    ],
                    id: "confirmSetting",
                    content: "导出",
                }
            }]
        }
    }
    return SettingDialogInfo;
}