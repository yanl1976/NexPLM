var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        class ControlsLogger {
        }
        Windows.ControlsLogger = ControlsLogger;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        Windows.FLEX_DIRECTION_ROW = "row";
        Windows.FLEX_DIRECTION_ROW_CLASS = "SView-flex-row";
        Windows.FLEX_DIRECTION_ROW_REVERSE = "row-reverse";
        Windows.FLEX_DIRECTION_ROW_REVERSE_CLASS = "SView-flex-row-reverse";
        Windows.FLEX_DIRECTION_COLUMN = "column";
        Windows.FLEX_DIRECTION_COLUMN_CLASS = "SView-flex-column";
        Windows.FLEX_DIRECTION_COLUMN_REVERSE = "column-reverse";
        Windows.FLEX_DIRECTION_COLUMN_REVERSE_CLASS = "SView-flex-column-reverse";
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         * 控制参数管理
         * */
        class ControlsParameter {
            constructor() {
                this.isOpenLog = true; //是否开启日志
                this.logLevel = 1; //日志等级
                this.demandLoadingAssemblyTree = false; //按装配等级加载装配结构树
            }
        }
        Windows.ControlsParameter = ControlsParameter;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        var _a;
        class UIJson {
        }
        _a = UIJson;
        UIJson.WrongLabel = {
            WrongLabel: {}
        };
        UIJson.AnimationLeftList = {
            AnimationList: {
                "id": "SView-animationList",
                "classes": [
                    "SView-animationList",
                    "SView-animationList-content"
                ],
                "nodeClickFunction": null,
                data: [
                    {
                        id: 0,
                        name: "儿童床安装过程",
                        type: 2,
                        children: [
                            {
                                id: 13,
                                name: "1、安装上层床床板",
                                process: null,
                                steps_id: 0,
                                step_info: "使用两个床侧板夹紧中间床板",
                                type: 1
                            },
                            {
                                id: 12,
                                name: "2、安装上层床床头床尾板",
                                process: null,
                                steps_id: 0,
                                step_info: "先将床板托起，然后需两人同时安装床头床尾板",
                                type: 1
                            },
                            {
                                id: 11,
                                name: "3、安装上层床床头床尾螺钉",
                                process: null,
                                steps_id: 0,
                                step_info: "使用螺栓、垫圈将上层床床头床尾连接（螺栓使用内套筒扳手）",
                                type: 1
                            }
                        ]
                    }
                ],
            }
        };
        // ************************** 徽标测试用例 ************************** //
        // --------- 点徽标测试用例 --------- //
        UIJson.DotBadge = {
            Badge: {
                type: "dotBadge",
                id: "badgeTest",
                classes: [
                    "SView-badge-dot",
                    "SView-badge-topLeft" // 徽标所在位置：topLeft、topRight、bottomLeft、bottomRight
                ],
                content: ""
            }
        };
        // --------- 带文字徽标测试用例 --------- //
        UIJson.ContentBadge = {
            Badge: {
                type: "withContentBadge",
                id: "contentBadgeTest",
                classes: [
                    "SView-badge-content",
                    "SView-badge-bottomRight"
                ],
                content: "test"
            }
        };
        // ************************** 按钮测试用例 ************************** //
        // --------- 图标按钮测试用例 --------- //
        UIJson.IconButton = {
            Button: {
                type: "iconButton",
                id: "snapshot",
                classes: [
                    "SView-button-iconBtn",
                    "SView-button-iconBtn-withBorder",
                ],
                onClick: "iconBtnOnClick('iconButton')",
                highLight: true,
                imgsrc: "./images/button_snapshot_.png",
                title: "新建视图",
                subComponents: [
                    _a.DotBadge
                ] // 可添加徽标、气泡选择器控件
            }
        };
        UIJson.AddButton = {
            Button: {
                id: "addBtn",
                imgsrc: "./images/button_snapshot_.png"
            }
        };
        UIJson.AddButton_1 = {
            Button: {
                id: "addBtn_1",
                imgsrc: "./images/button_walking.png"
            }
        };
        // --------- 基础按钮测试用例 --------- //
        UIJson.BasicButton = {
            Button: {
                type: "basicButton",
                id: "basicBtnTest",
                classes: [
                    "SView-button",
                    "SView-button-small",
                    "SView-button-primary" // 按钮背景颜色：default、primary、success、warning、danger，若不写默认背景颜色primary
                ],
                content: "TestBtn",
                onClick: "baiscBtnOnClick()",
                onTouchStart: "baiscBtnOnClick()",
            }
        };
        // --------- 基础按钮测试用例 --------- //
        UIJson.BasicButton_1 = {
            Button: {
                type: "basicButton",
                id: "basicBtnTest",
                isShow: false,
                classes: [
                    "SView-button",
                    "SView-button-small",
                    "SView-button-primary" // 按钮背景颜色：default、primary、success、warning、danger，若不写默认背景颜色primary
                ],
                content: "TestBtn",
                onClick: "baiscBtnOnClick()"
            }
        };
        // --------- 图标+文字按钮测试用例 --------- //
        UIJson.IconTextButton = {
            Button: {
                type: "iconTextButton_right",
                id: "iconTextBtnTest",
                classes: [
                    "SView-button-iconText",
                    "SView-button-iconLR",
                    "SView-button-success",
                ],
                content: "iconTextBtn",
                imgsrc: "./images/button_snapshot_.png",
                subComponents: [
                    UIJson.DotBadge
                ]
            }
        };
        // --------- 输入框按钮测试用例 --------- //
        UIJson.InputButton = {
            InputButton: {
                id: "textInputButtonTest",
                onClick: "",
                imgsrc: "./images/button_snapshot_.png",
                btnTitle: "Test",
                warning: "Warning Content!",
                warningIsShow: true, // 若不写，默认不显示
            }
        };
        // ************************* 复选框测试用例 ************************* //
        UIJson.Checkbox = {
            Checkbox: {
                type: "labelBack",
                id: "checkboxTest",
                label: "CheckBox1",
                classes: [
                    "SView-checkbox" // 若不写，默认类名为"SView-checkbox"
                ],
                checkboxId: "checkboxLabel" // 复选框input标签id
            }
        };
        // ************************* 输入框测试用例 ************************* //
        // --------- 基础输入框测试用例 --------- //
        UIJson.BasicInput = {
            Input: {
                id: "basicInputTest",
                classes: [
                    "SView-input", // 若不写，默认类名为"SView-input"
                ],
                value: "Test",
                placeHolder: "PlaceHolder",
                oninput: "" // 输入框输入内容检查
            }
        };
        // --------- 带标题输入框测试用例 --------- //
        UIJson.WithTitleInput = {
            WithTitleInput: {
                id: "withTitleInputTest",
                classes: [
                    "SView-input-withTitle", // 若不写，默认类名为"SView-input-withTitle"
                ],
                title: "Test",
                subComponents: [
                    _a.BasicInput
                ]
            }
        };
        // --------- 带警告输入框测试用例 --------- //
        UIJson.WithWarningInput = {
            WithWarningInput: {
                id: "withWarningInputTest",
                classes: [
                    "SView-input-warning", // 若不写，默认类名为"SView-input-warning"
                ],
                warning: "Warning content!",
                warningIsShow: true,
                subComponents: [
                    _a.BasicInput
                ]
            }
        };
        // --------- 带标题+警告输入框测试用例 --------- //
        UIJson.WithBothInput = {
            WithBothInput: {
                id: "withBothInputTest",
                classes: [
                    "SView-input-withBoth" // 若不写，默认类名为"SView-input-withBoth"
                ],
                warningIsShow: true,
                warning: "Warning Content",
                title: "Title",
                subComponents: [
                    _a.BasicInput,
                ]
            }
        };
        // --------- 带按钮输入框测试用例 --------- //
        UIJson.WithBtnInput = {
            WithBtnInput: {
                id: "withBtnInputTest",
                classes: [
                    "SView-input-withBtn", // 若不写默认类名为"SView-input-withBtn"
                ],
                subComponents: [
                    {
                        Input: {
                            id: "btnInput",
                            classes: [
                                "SView-complexInput" // 带按钮输入框需使用该类名
                            ],
                            value: "Test",
                            placeHolder: "PlaceHolder",
                            oninput: "",
                        }
                    },
                    _a.InputButton
                ]
            }
        };
        // ************************* 表单测试用例 ************************* //
        UIJson.Form = {
            Form: {
                id: "formTest",
                classes: [
                    "SView-form" // 若不写，默认类名为"SView-form"
                ],
                subComponents: [
                    _a.WithTitleInput,
                    _a.WithBothInput,
                ]
            }
        };
        // ************************* 弹出框测试用例 ************************* //
        UIJson.BasicDialog = {
            Dialog: {
                id: "dialogTest",
                classes: [
                // "SView-dialog", // 若不写，默认类名为"SView-dialog"
                ],
                showBack: true,
                title: "Test",
                content: "Dialog Content",
                closeBtnIsShow: false,
                closeBtnOnClick: "",
                buttonsIsShow: true,
                buttons: [
                    {
                        Button: {
                            type: "basicButton",
                            id: "basicBtnTest",
                            classes: [
                                "SView-button",
                                "SView-button-mini",
                                "SView-button-warning" // 按钮背景颜色：default、primary、success、warning、danger，若不写默认背景颜色primary
                            ],
                            content: "TestBtn",
                            onClick: "baiscBtnOnClick()"
                        }
                    }
                ],
            }
        };
        UIJson.ComplexDialog = {
            Dialog: {
                id: "complexDialogTest",
                classes: [
                // 可以不写类名
                ],
                showBack: false,
                title: "Test",
                closeBtnIsShow: true,
                closeBtnOnClick: "",
                buttonsIsShow: true,
                buttons: [
                    {
                        Button: {
                            type: "basicButton",
                            id: "basicBtnTest",
                            classes: [
                                "SView-button",
                                "SView-button-mini",
                                "SView-button-warning" // 按钮背景颜色：default、primary、success、warning、danger，若不写默认背景颜色primary
                            ],
                            content: "TestBtn",
                            onClick: "baiscBtnOnClick()"
                        }
                    }
                ],
                subComponents: [
                    _a.Form
                ]
            }
        };
        // ************************* 气泡选择器测试用例 ************************* //
        UIJson.PopoverSelect = {
            PopoverSelect: {
                id: "popoverSelectTest",
                isShow: true,
                classes: [
                    "SView-popoverSelect",
                    "SView-popoverSelect-down", // 弹出方向：up、down
                    // "SView-popoverSelect-tripleWidth", // 三倍宽度
                ],
                subComponents: [
                    _a.AddButton, _a.AddButton, _a.AddButton
                ]
            }
        };
        // ************************* 右键菜单测试用例 ************************* //
        UIJson.RightClickBtn = {
            RightClickBtn: {
                id: "rightClickBtnTest_1",
                imgsrc: "./images/rightClickMenu/modelShowAll.png",
                content: "Test_2"
            }
        };
        UIJson.RightClickMenu = {
            RightClickMenu: {
                id: "rightClickMenuTest",
                classes: [
                    "SView-rightClickMenu" // 若不写，默认类名为"SView-rightClickMenu"
                ],
                // isShow: false, // 右键菜单默认不显示
                subComponents: [
                    {
                        RightClickBtn: {
                            id: "rightClickBtnTest",
                            imgsrc: "./images/rightClickMenu/show-hide.png",
                            content: "Test_1"
                        }
                    },
                    {
                        RightClickBtn: {
                            id: "rightClickBtnTest",
                            imgsrc: "./images/rightClickMenu/show-hide.png",
                            content: "Test_1"
                        }
                    },
                ]
            }
        };
        // ************************* 选择器测试用例 ************************* //
        UIJson.Option_1 = {
            Option: {
                value: 0,
                content: "Option 1",
                selected: true, // 默认选中选项
            }
        };
        UIJson.Option_2 = {
            Option: {
                value: 1,
                content: "Option 2"
            }
        };
        UIJson.Option_3 = {
            Option: {
                value: 2,
                content: "Option 3"
            }
        };
        UIJson.Select = {
            Select: {
                id: "selectTest",
                classes: [
                    "SView-select" // 若不写，默认类名"SView-select"
                ],
                select: {
                    id: "selectId",
                    value: 0
                },
                options: [
                    _a.Option_1,
                    _a.Option_2
                ]
            }
        };
        // ************************* 滑块测试用例 ************************* //
        UIJson.Slider = {
            Slider: {
                id: "silderTest",
                classes: [
                    "SView-slider" // 若不写，默认类名"SView-slider"
                ],
                sliderBar: {
                    id: "sliderBar",
                    min: 20,
                    max: 80,
                    step: 1,
                    value: 30 // 滑条初始值，若不写，默认值为50
                }
            }
        };
        // ************************* 气泡弹出框测试用例 ************************* //
        UIJson.PopoverRow = {
            PopoverRow: {
                id: "popoverRowTest_1",
                subComponents: [
                    {
                        Label: "Row_2",
                    },
                    _a.Slider
                ]
            }
        };
        UIJson.Popover = {
            Popover: {
                id: "popoverTest",
                classes: [
                    "SView-popover",
                    "SView-popover-up" // 弹出方向：up、down
                ],
                subComponents: [
                    {
                        PopoverRow: {
                            id: "popoverRowTest",
                            subComponents: [
                                {
                                    Label: "Row_1",
                                },
                                _a.AddButton
                            ]
                        }
                    },
                    {
                        PopoverRow: {
                            id: "popoverRpwTest_1",
                            subComponents: [
                                _a.AddButton,
                                _a.Slider
                            ]
                        }
                    }
                ]
            }
        };
        // ************************* 开关测试用例 ************************* //
        UIJson.Switch = {
            Switch: {
                id: "switchTest",
                classes: [
                    "SView-switch", // 若不写，默认类名为"SView-switch"
                ],
                checkboxId: "switchCheckBox",
                checked: true,
                dataOn: "ON",
                dataOff: "OFF" // 按钮关闭时，所显示文字，非必须
            }
        };
        // ************************* 表格测试用例 ************************* //
        UIJson.Table = {
            Table: {
                id: "tableTest",
                classes: [
                    "SView-table", // 若不写，默认类名为"SView-table"
                ],
                cellTitle: ["Title1", "Title2", "Title3"],
                cellContent: [
                    ["Content1-1", "Content1-2", "Content1-3"],
                    ["Content2-1", "Content2-2", "Content2-3"]
                ] // 表格内容
            }
        };
        // ************************* 折叠框测试用例 ************************* //
        UIJson.CollapseItem = {
            CollapseItem: {
                id: "listTest_2",
                label: "ItemTitle_3",
                subComponents: [
                    UIJson.IconButton
                ]
            }
        };
        UIJson.Collapse = {
            Collapse: {
                title: "Test",
                id: "collapseTest",
                subComponents: [
                    {
                        CollapseItem: {
                            id: "listTest",
                            label: "ItemTitle_1",
                            subComponents: [
                                _a.Slider
                            ]
                        }
                    },
                    {
                        CollapseItem: {
                            id: "listTest_1",
                            label: "ItemTitle_2",
                            subComponents: [
                                _a.Switch
                            ]
                        }
                    }
                ]
            }
        };
        // ************************* 标签页测试用例 ************************* //
        UIJson.Tabs = {
            Tabs: {
                id: "tabsTest",
                classes: [
                // 可以不写
                ],
                tabContent: [
                    {
                        dataActive: "true",
                        title: "Test_1",
                        dataClass: "tabTest_1",
                        subComponentsId: "tabTest_1",
                        subComponents: [
                            _a.Collapse,
                            _a.Collapse
                        ]
                    },
                    {
                        title: "Test_2",
                        dataClass: "tabTest_2",
                        subComponentsId: "tabTest_2",
                        subComponents: [
                            _a.Form
                        ]
                    }
                ] // 标签页内容
            }
        };
        UIJson.TabDialog = {
            Dialog: {
                id: "tabDialog",
                title: "Test",
                subComponents: [
                // Tabs,
                ],
                buttons: [
                    {
                        Button: {
                            type: "basicButton",
                            id: "basicBtnTest",
                            classes: [
                                "SView-button",
                                "SView-button-mini",
                                "SView-button-warning" // 按钮背景颜色：default、primary、success、warning、danger，若不写默认背景颜色primary
                            ],
                            content: "TestBtn",
                            onClick: "baiscBtnOnClick()"
                        }
                    }
                ],
            }
        };
        UIJson.RightMenu = {
            RightMenu: {
                id: "rightMenu",
                classes: [
                    "SView-rightMenu"
                ],
                subComponents: [
                    {
                        Button: {
                            id: "resetButton",
                            imgsrc: "./images/rightMenuBtn/sview_home.png",
                            title: "复位",
                            onClick: "excuteReset()",
                        },
                    },
                    {
                        Button: {
                            id: "viewMenuBtn",
                            imgsrc: "./images/rightMenuBtn/button_view.png",
                            title: "视图",
                            "classes": [
                                "SView-remove",
                            ],
                            onClick: "showViewMenu('viewMenuBtn')",
                            // "highLight": true,
                        }
                    },
                    {
                        Button: {
                            id: "assemblyTree",
                            "classes": [
                                "SView-remove",
                            ],
                            imgsrc: "./images/rightMenuBtn/sview_assembly.png",
                            title: "装配",
                            onClick: "showAssemblyTree('assemblyTree')",
                        }
                    },
                    {
                        "Button": {
                            "id": "attributeInfo",
                            "imgsrc": "./images/rightMenuBtn/sview_measure_attributes.png",
                            "title": "模型属性",
                            "onClick": "showAttributeInfo('attributeInfo')",
                        }
                    },
                    {
                        "Button": {
                            "id": "meetingMainMenu",
                            "imgsrc": "./images/rightMenuBtn/sview_conference.png",
                            "title": "协同会议",
                            "onClick": "showMeetingMenu('meetingMainMenu')",
                        }
                    },
                    {
                        "Button": {
                            "id": "moreMenu",
                            "imgsrc": "./images/rightMenuBtn/button_menugroup.png",
                            "title": "菜单",
                            "onClick": "showBottomMenu('moreMenu')",
                        }
                    },
                ]
            },
        };
        UIJson.AnnotationDialog = {
            Dialog: {
                classes: [
                    "SView-dialog",
                    "notation-dialog"
                ],
                id: "notation-dialog",
                onClick: "closeDialog('notation-dialog')",
                title: {
                    classes: [
                        "SView-dialog-title-name"
                    ],
                    content: "批注"
                },
                subComponents: [
                    {
                        Form: {
                            id: "textAnnotationForm",
                            classes: [
                                "SView-form",
                                "SView-form-middle"
                            ],
                            subComponents: [
                                {
                                    WithWarningInput: {
                                        id: "textAnnotation",
                                        classes: [
                                            "SView-input-warning"
                                        ],
                                        warning: "输入批注内容不能为空！",
                                        subComponents: [
                                            {
                                                Input: {
                                                    id: "textAnnotationInput",
                                                    value: "批注1",
                                                    placeHolder: "请输入批注内容",
                                                    onInput: "checkLength('textAnnotationInput')"
                                                }
                                            }
                                        ]
                                    }
                                }
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
                        content: "确定",
                        onClick: "closeDialog('notation-dialog')"
                    },
                    {
                        type: "basicButton",
                        classes: [
                            "SView-button",
                            "SView-button-default"
                        ],
                        content: "关闭",
                        onClick: "closeDialog('notation-dialog')"
                    }]
            }
        };
        UIJson.MessageDialog = {
            Message: {
                display: "none",
                type: "withButton",
                classes: [
                    "SView-message",
                    "SView-message-withBtn"
                ],
                id: "withBtnMessage",
                content: "",
            }
        };
        UIJson.ViewMenu = {
            ViewMenu: {
                id: "viewMenu",
                classes: [
                    "SView-viewMenu",
                ],
                subComponents: [
                    {
                        classes: [
                            "SView-renderMode",
                        ],
                        buttons: [
                            {
                                Button: {
                                    id: "solid",
                                    imgsrc: "./images/renderMode/sview_perspective_solid.png",
                                    title: "实体模式",
                                    onClick: "selectRenderMode('solid')",
                                    highLight: true,
                                }
                            },
                            {
                                Button: {
                                    id: "solidWire",
                                    imgsrc: "./images/renderMode/button_solid_wire.png",
                                    title: "实体线框模式",
                                    onClick: "selectRenderMode('solidWire')"
                                }
                            },
                            {
                                Button: {
                                    id: "wire",
                                    imgsrc: "./images/renderMode/button_wire.png",
                                    title: "插图模式",
                                    onClick: "selectRenderMode('wire')"
                                }
                            },
                            {
                                Button: {
                                    id: "transparent",
                                    imgsrc: "./images/renderMode/sview_perspective_transparent.png",
                                    title: "半透明模式",
                                    onClick: "selectRenderMode('transparent')",
                                }
                            },
                        ]
                    },
                    {
                        classes: [
                            "SView-direction"
                        ],
                        buttons: [
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    id: "fromTop",
                                    imgsrc: "./images/viewDirection/button_top.png",
                                    title: "俯视图",
                                    onClick: "viewDirection('fromTop')"
                                }
                            },
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    id: "fromAxis",
                                    imgsrc: "./images/viewDirection/button_zhouce.png",
                                    title: "轴视图",
                                    onClick: "viewDirection('fromAxis')"
                                }
                            },
                            {
                                Button: {
                                    id: "fromLeft",
                                    imgsrc: "./images/viewDirection/button_left.png",
                                    title: "左视图",
                                    onClick: "viewDirection('fromLeft')"
                                }
                            },
                            {
                                Button: {
                                    id: "fromFront",
                                    imgsrc: "./images/viewDirection/button_front.png",
                                    title: "正视图",
                                    onClick: "viewDirection('fromFront')"
                                }
                            },
                            {
                                Button: {
                                    id: "fromRight",
                                    imgsrc: "./images/viewDirection/button_right.png",
                                    title: "右视图",
                                    onClick: "viewDirection('fromRight')"
                                }
                            },
                            {
                                Button: {
                                    id: "fromBack",
                                    imgsrc: "./images/viewDirection/button_behind.png",
                                    title: "后视图",
                                    onClick: "viewDirection('fromBack')"
                                }
                            },
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    id: "fromBottom",
                                    imgsrc: "./images/viewDirection/button_bottom.png",
                                    title: "仰视图",
                                    onClick: "viewDirection('fromBottom')"
                                }
                            },
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    id: "perspectiveView",
                                    imgsrc: "./images/viewDirection/sview_camera_perspective.png",
                                    title: "透视投影",
                                    onClick: "viewDirection('perspectiveView')"
                                }
                            }
                        ]
                    },
                    {
                        classes: [
                            "SView-walkingMode"
                        ],
                        buttons: [
                            {
                                Button: {
                                    id: "walking",
                                    imgsrc: "./images/button_walking.png",
                                    title: "漫游模式",
                                    onClick: "selectWalkingMode('walking')"
                                }
                            },
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    classes: [
                                        "SView-button-iconBtn",
                                        "SView-notUseLi"
                                    ],
                                    imgsrc: "",
                                }
                            },
                            {
                                Button: {
                                    id: "settingMenu",
                                    imgsrc: "./images/button_setting.png",
                                    title: "设置",
                                    onClick: "showSettingMenu()"
                                }
                            }
                        ]
                    }
                ]
            }
        };
        UIJson.TreeDialog = {
            TreeDialog: {
                classes: [
                    "SView-treeDialog"
                ],
                id: "assemblyTreeDialog",
                title: {
                    classes: [
                        "SView-treeDialog-title"
                    ],
                    id: "treeDialogTabs",
                    closeBtn: "closeDialog('assemblyTreeDialog', 'assemblyTree')",
                    tabContent: [
                        {
                            Button: {
                                dataClass: "SView-assemblyTree",
                                id: "assemblyTreeBtn",
                                imgsrc: "./images/rightMenuBtn/sview_assembly.png",
                                title: "装配",
                                onClick: "changeTreeTabs('treeDialogTabs','SView-assemblyTree')",
                                highLight: true
                            }
                        },
                        {
                            Button: {
                                dataClass: "SView-viewList",
                                id: "viewListBtn",
                                imgsrc: "./images/rightMenuBtn/button_view.png",
                                title: "视图",
                                onClick: "changeTreeTabs('treeDialogTabs', 'SView-viewList')",
                            }
                        }
                    ],
                },
                search: {
                    subComponents: [
                        {
                            WithBtnInput: {
                                id: "searchInput",
                                classes: [
                                    "SView-input-withBtn"
                                ],
                                subComponents: [
                                    {
                                        Input: {
                                            id: "searchContentInput",
                                            classes: [
                                                "SView-complexInput"
                                            ],
                                            value: "",
                                            placeHolder: "请输入搜索内容",
                                            autoComplete: "off"
                                        },
                                    },
                                    {
                                        InputButton: {
                                            id: "searchBtn",
                                            onClick: "checkLength('searchContentInput')",
                                            imgsrc: "./images/search_white.png",
                                            btnTitle: "",
                                            warning: "搜索内容不能为空！"
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                },
                content_1: {
                    id: "SView-assemblyTree",
                    dataActive: "true",
                    subComponents: [
                        {
                            Tree: {
                                "id": "SView-tree",
                                "classes": [
                                    "SView-tree",
                                    "SView-tree-content"
                                ],
                                data: [
                                    {
                                        name: "减速器装配",
                                        active: false,
                                        id: "1",
                                        img: "",
                                        children: [
                                            // 模拟数据
                                            {
                                                name: "Asm齿轮轴",
                                                active: false,
                                                id: "2",
                                                img: "",
                                                children: [
                                                    {
                                                        name: "齿轮轴",
                                                        active: false,
                                                        id: 3,
                                                        img: "",
                                                        children: []
                                                    },
                                                    {
                                                        id: 4,
                                                        name: "80轴承",
                                                        children: [],
                                                        active: false,
                                                        img: ""
                                                    }
                                                ]
                                            },
                                            {
                                                name: "主轴装配",
                                                active: false,
                                                id: 21,
                                                img: "",
                                                children: [
                                                    {
                                                        name: "100轴承",
                                                        children: [],
                                                        active: false,
                                                        id: 20,
                                                        img: ""
                                                    },
                                                    {
                                                        id: 19,
                                                        name: "主轴",
                                                        children: [],
                                                        active: false,
                                                        img: ""
                                                    }
                                                ]
                                            },
                                            {
                                                name: "视孔盖",
                                                active: false,
                                                id: 22,
                                                img: "",
                                                children: []
                                            }
                                        ]
                                    }
                                ],
                                treeRightClickMenu: {
                                    RightClickMenu: {
                                        id: "treeRightClickMenu",
                                        classes: [
                                            "SView-rightClickMenu",
                                            "SView-tree-rightClickMenu"
                                        ],
                                        subComponents: [
                                            {
                                                RightClickBtn: {
                                                    "id": "treeAdd",
                                                    "content": "添加"
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeLoad",
                                                    "content": "加载"
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeUninstall",
                                                    "content": "卸载"
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeRemove",
                                                    "content": "删除",
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeCopy",
                                                    "content": "复制",
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeShear",
                                                    "content": "剪切",
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treePaste",
                                                    "content": "粘贴",
                                                }
                                            },
                                            {
                                                RightClickBtn: {
                                                    "id": "treeRename",
                                                    "content": "重命名",
                                                    "onClick": "nodeRename()"
                                                }
                                            }
                                        ]
                                    }
                                }
                            }
                        }
                    ]
                },
                content_2: {
                    id: "SView-viewList",
                    dataActive: "false",
                    subComponents: [
                        {
                            Button: {
                                type: "iconTextButton_left",
                                classes: [
                                    "SView-button-iconText",
                                    "SView-button-iconLR",
                                    "SView-button-primary",
                                    "createNewViewBtn"
                                ],
                                id: "createNewView",
                                content: "新建视图",
                                imgsrc: "./images/button_snapshot_.png"
                            }
                        },
                        {
                            ViewList: {
                                id: "sview_view_panel",
                                classes: [
                                    "SView-pull-left-div",
                                ],
                                data: [
                                    {
                                        id: 111,
                                        name: "视图1",
                                        time: "2022-03-03 16:09:41",
                                        author: "游客",
                                        imgSrc: "https://service.sv3d.cn/data/upload/modelfile/20210205/4b9861c4-2e4b-4abe-b5cf-11b82bf744f5/13ee6732-534c-4336-82ab-a88e47fe6629_1.png"
                                    },
                                    {
                                        id: 222,
                                        name: "俯视图"
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        };
        UIJson.Tree = {
            Tree: {
                "id": "SView-tree",
                "classes": [
                    "SView-tree",
                    "SView-tree-content"
                ],
                data: [
                    {
                        name: "减速器装配",
                        active: false,
                        id: "1",
                        img: "",
                        children: [
                            // 模拟数据
                            {
                                name: "Asm齿轮轴",
                                active: false,
                                id: "2",
                                img: "",
                                children: [
                                    {
                                        name: "齿轮轴",
                                        active: false,
                                        id: 3,
                                        img: "",
                                        children: []
                                    },
                                    {
                                        id: 4,
                                        name: "80轴承",
                                        children: [],
                                        active: false,
                                        img: ""
                                    }
                                ]
                            },
                            {
                                name: "主轴装配",
                                active: false,
                                id: 21,
                                img: "",
                                children: [
                                    {
                                        name: "100轴承",
                                        children: [],
                                        active: false,
                                        id: 20,
                                        img: ""
                                    },
                                    {
                                        id: 19,
                                        name: "主轴",
                                        children: [],
                                        active: false,
                                        img: ""
                                    }
                                ]
                            },
                            {
                                name: "视孔盖",
                                active: false,
                                id: 22,
                                img: "",
                                children: []
                            }
                        ]
                    }
                ],
                treeRightClickMenu: {
                    RightClickMenu: {
                        id: "treeRightClickMenu",
                        classes: [
                            "SView-rightClickMenu",
                            "SView-tree-rightClickMenu"
                        ],
                        subComponents: [
                            {
                                RightClickBtn: {
                                    "id": "treeAdd",
                                    "content": "添加"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeLoad",
                                    "content": "加载"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeUninstall",
                                    "content": "卸载"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeRemove",
                                    "content": "删除",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeCopy",
                                    "content": "复制",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeShear",
                                    "content": "剪切",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treePaste",
                                    "content": "粘贴",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeRename",
                                    "content": "重命名",
                                    "onClick": "nodeRename()"
                                }
                            }
                        ]
                    }
                }
            }
        };
        UIJson.ViewList = {
            ViewList: {
                id: "sview_view_panel",
                classes: [
                    "SView-view-list",
                    "SView-pull-left-div",
                ],
                data: [
                    {
                        id: 111,
                        name: "视图1",
                        time: "2022-03-03 16:09:41",
                        author: "游客",
                        imgSrc: "https://service.sv3d.cn/data/upload/modelfile/20210205/4b9861c4-2e4b-4abe-b5cf-11b82bf744f5/13ee6732-534c-4336-82ab-a88e47fe6629_1.png"
                    },
                    {
                        id: 222,
                        name: "俯视图"
                    }
                ]
            }
        };
        UIJson.AttributeDialog = {
            Dialog: {
                classes: [
                    "SView-attributeInfo-dialog"
                ],
                id: "attributeInfoDialog",
                onClick: "closeDialog('attributeInfoDialog', 'attributeInfo')",
                title: "属性",
                subComponents: [
                    {
                        Collapse: {
                            id: "modelProperty",
                            title: "属性",
                            detailInfo: [
                                {
                                    label: "名称",
                                    value: "Loft Bed-T"
                                },
                                {
                                    label: "name",
                                    value: "Loft Bed-T"
                                },
                                {
                                    label: "商品名称",
                                    value: "全实木上下子母床"
                                },
                                {
                                    label: "编号",
                                    value: "547545100"
                                },
                                {
                                    label: "重量",
                                    value: "150.0kg"
                                },
                                {
                                    label: "货号",
                                    value: "爆款原木"
                                },
                                {
                                    label: "主色系",
                                    value: "原木色"
                                },
                                {
                                    label: "环保等级",
                                    value: "E0级"
                                },
                                {
                                    label: "床垫",
                                    value: "乳胶垫"
                                },
                                {
                                    label: "id",
                                    value: "1013"
                                },
                            ]
                        }
                    },
                    {
                        Collapse: {
                            id: "modelGeometry",
                            title: "几何",
                            detailInfo: [
                                {
                                    label: "体积",
                                    value: "1235285804.105 mm?"
                                },
                                {
                                    label: "面积",
                                    value: "53577515.890 mm?"
                                },
                            ]
                        }
                    },
                    {
                        Collapse: {
                            id: "modelModeling",
                            title: "造型",
                            detailInfo: [
                                {
                                    label: "ID",
                                    value: "5"
                                },
                                {
                                    label: "子模型数量",
                                    value: "23"
                                },
                                {
                                    label: "视图数量",
                                    value: "14"
                                },
                                {
                                    label: "实例数量",
                                    value: "319"
                                },
                                {
                                    label: "PlaceID",
                                    value: "0"
                                },
                                {
                                    label: "ProtoTypeID",
                                    value: "1014"
                                },
                                {
                                    label: "颜色",
                                    value: "0.800,0.800,0.800,1.000"
                                },
                                {
                                    label: "LOD0面片数量",
                                    value: "44414"
                                }
                            ]
                        }
                    },
                ]
            }
        };
        UIJson.addRightMenu = [{
                Button: {
                    id: "resetButton",
                    imgsrc: "./images/rightMenuBtn/sview_home.png",
                    title: "新增",
                    onClick: "excuteReset()",
                },
            }
        ];
        // ---------- 底部主菜单 -----------
        UIJson.BottomMenu = {
            BottomMenu: {
                id: "mainBottomMenu",
                classes: [
                    "SView-bottomMenu",
                ],
                subComponents: [
                    {
                        Button: {
                            id: "move",
                            imgsrc: "./images/moreMenu/sview_movecontrol_translation.png",
                            title: "移动",
                            onClick: "enterBottomMenu(MoveMenu)"
                        }
                    },
                    {
                        Button: {
                            id: "annotate",
                            imgsrc: "./images/moreMenu/button_note.png",
                            title: "批注",
                            onClick: "enterBottomMenu(AnnotateMenu)"
                        }
                    },
                    {
                        Button: {
                            id: "animation",
                            imgsrc: "./images/moreMenu/button_ani_play.png",
                            title: "动画",
                            onClick: "enterBottomMenu(AnimationMenu)",
                        }
                    },
                    {
                        Button: {
                            id: "measure",
                            imgsrc: "./images/moreMenu/button_measure.png",
                            title: "测量",
                            onClick: "enterBottomMenu('distanceMeasure')",
                        }
                    },
                    {
                        Button: {
                            id: "section",
                            imgsrc: "./images/moreMenu/button_section.png",
                            title: "刨切",
                            onClick: "enterBottomMenu('sectionMenu')",
                        }
                    },
                    {
                        Button: {
                            id: "explosive",
                            imgsrc: "./images/moreMenu/button_explosive.png",
                            title: "爆炸",
                            onClick: "enterBottomMenu('explosiveMenu')",
                        }
                    },
                    {
                        Button: {
                            id: "snapshot",
                            imgsrc: "./images/moreMenu/button_snapshot.png",
                            title: "快照",
                            onClick: "snapShot()",
                        }
                    },
                    {
                        Button: {
                            id: "clear",
                            imgsrc: "./images/moreMenu//sview_clear.png",
                            title: "清空",
                            onClick: "showPopoverSelect('clearAllMode')"
                        }
                    }
                ]
            }
        };
        UIJson.addBottomMenu = [{
                Button: {
                    id: "resetButton",
                    imgsrc: "./images/rightMenuBtn/sview_home.png",
                    title: "底部新增",
                    onClick: "excuteReset()",
                },
            }];
        // ---------- 注释主菜单 -----------
        UIJson.AnnotateMenu = {
            BottomMenu: {
                id: "annotateMenu",
                classes: [
                    "SView-bottomMenu",
                ],
                subComponents: [
                    {
                        Button: {
                            id: "gesture",
                            imgsrc: "./images/moreMenu/annotate/sview_gesturenote.png",
                            title: "手势批注",
                            onClick: "enterBottomMenu('gestureMenu', 'annotateMenu')"
                        }
                    },
                    {
                        Button: {
                            id: "textnote",
                            imgsrc: "./images/moreMenu/annotate/sview_textnote.png",
                            title: "文本批注",
                            onClick: "commandClickFun('textnote','AnnotationTextCommand')"
                        }
                    },
                    {
                        Button: {
                            id: "sequence",
                            imgsrc: "./images/moreMenu/annotate/button_sequence.png",
                            title: "序号",
                            onClick: "commandClickFun('sequence','AnnotationNumberCommand')",
                        }
                    },
                    {
                        Button: {
                            id: "component",
                            imgsrc: "./images/moreMenu/annotate/button_component.png",
                            title: "模型名称",
                            onClick: "commandClickFun('distanceMeasure','AnnotationComponentCommand')",
                        }
                    },
                    {
                        Button: {
                            id: "section",
                            imgsrc: "./images/moreMenu/annotate/sview_note_edit.png",
                            title: "编辑",
                            onClick: "commandClickFun('section','AnnotationEditCommand')",
                        }
                    },
                    {
                        Button: {
                            id: "deleteAnnotate",
                            imgsrc: "./images/moreMenu/annotate/sview_clear.png",
                            title: "删除",
                            // "onClick": "enterBottomMenu('explosiveMenu')",
                            onClick: "confirmDelete('deleteAnnotate')",
                        }
                    },
                    {
                        Button: {
                            id: "exitAnnotate",
                            imgsrc: "./images/moreMenu/translation/sview_button_exit.png",
                            title: "退出",
                            // "onClick": "exitBottomMenu(AnnotateMenu)",
                            onClick: "confirmExit('exitAnnotate')",
                        }
                    },
                ]
            }
        };
        // ---------- 移动主菜单 -----------
        UIJson.MoveMenu = {};
        UIJson.BtnMessageDialog = {
            "Message": {
                "type": "withButton",
                "classes": [
                    "SView-message",
                    "SView-message-withBtn"
                ],
                "id": "withBtnMessage",
                "content": "测试",
            }
        };
        UIJson.SuccessInfo = {
            "ResultDialog": {
                "id": "successInfo",
                "classes": [
                    "SView-info",
                ],
                "icon": {
                    "classes": [
                        "SView-info-icon",
                    ],
                    "imgsrc": "./images/info/success.png",
                },
                "titleContent": "成功提示",
                "detailContent": "成功内容的详细描述",
            },
        };
        UIJson.MeetingDialog = {
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
                            "onClick": "showMeetingSettingMenu()",
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
                        "onClick": "showCreateMeeting()"
                    },
                    {
                        "classes": [
                            "SView-meeting-join"
                        ],
                        "imgsrc": "images/meeting/meeting_join.png",
                        "onClick": "showJoinMeeting()"
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
        Windows.UIJson = UIJson;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI控件标签
         *@author	ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Label {
            /**
             *  @description 构造函数
             * */
            constructor() {
                /**
                 * @description 默认不显示
                 */
                this.eleOverLay = false; // 默认不显示
                this.isDisable = false; // 默认不禁用
                this.eleClasses = [];
                this.eleId = "";
                this.eleOnClick = "";
                this.eleOnTouchStart = "";
                this.eleIsShow = true;
                this.eleLabel = null;
            }
            /**
             * @description  创建标签
             * @param elementInfo 标签信息
             */
            /**
             *
             * @param elementInfo
             */
            create(elementInfo) {
                let that = this;
                return new Promise((resolve, reject) => {
                    if (elementInfo) {
                        let tagName = Object.keys(elementInfo)[0];
                        let newElement;
                        if (!that.eleLabel) {
                            //this.eleLabel = document.createElement(tagName);
                            if (!SView.Windows[tagName]) {
                                reject("Failed");
                            }
                            //newElement = new SView.Windows[tagName]();
                            //let newElement = createSViewObject(UI, tagName);
                            //newElement.createLabel(elementInfo);
                            that.createLabel(elementInfo);
                            //for (let key in newElement) {
                            //    that[key] = newElement[key];
                            //}
                        }
                        elementInfo = elementInfo[tagName];
                        //判断是否有id
                        if (elementInfo.id) {
                            that.eleId = elementInfo.id;
                        }
                        if (that.eleId) {
                            that.eleLabel.setAttribute("id", that.eleId);
                        }
                        if (elementInfo.attributeData && elementInfo.attributeData != "") {
                            for (var key in elementInfo.attributeData) {
                                that[key] = elementInfo.attributeData[key];
                                that.eleLabel.setAttribute(key, elementInfo.attributeData[key]);
                            }
                        }
                        //判断是否有class
                        if (elementInfo.classes && elementInfo.classes.length > 0) {
                            //this.eleClasses = elementInfo.classes;
                            that.eleClasses = [];
                            that.eleClasses.push(...elementInfo.classes);
                        }
                        if (that.eleOverLay) {
                            that.eleClasses = [];
                            that.addClass("SView-dialog-background");
                        }
                        else {
                            that.removeClass("SView-dialog-background");
                        }
                        if (elementInfo.isDisabled && elementInfo.isDisabled == true) {
                            that.addClass("SView-disabled");
                        }
                        if (that.eleClasses) {
                            that.initFlexDirection();
                            that.eleLabel.setAttribute("class", that.eleClasses.join(" "));
                        }
                        //判断是否有点击事件
                        if (elementInfo.onClick && typeof elementInfo.onClick == "function") {
                            that.eleLabel.onclick = elementInfo.onClick;
                            that.eleLabel.onclick = function () {
                                elementInfo.onclick(that.eleLabel);
                            };
                        }
                        else if (elementInfo.onClick) {
                            that.eleOnClick = elementInfo.onClick;
                            if (that.eleOnClick) {
                                that.eleLabel.setAttribute("onclick", that.eleOnClick);
                            }
                        }
                        //判断是否有change事件
                        if (elementInfo.onChange && typeof elementInfo.onChange == "function") {
                            that.eleLabel.onchange = elementInfo.onChange;
                            that.eleLabel.onchange = function () {
                                elementInfo.onChange(that.eleLabel);
                            };
                        }
                        else if (elementInfo.onChange) {
                            that.eleOnChange = elementInfo.onChange;
                            if (that.eleOnChange) {
                                that.eleLabel.setAttribute("onchange", that.eleOnChange);
                            }
                        }
                        //判断是否有触摸事件
                        if (elementInfo.onTouchStart) {
                            that.eleOnTouchStart = elementInfo.onTouchStart;
                        }
                        if (that.eleOnTouchStart) {
                            that.eleLabel.setAttribute("onTouchStart", that.eleOnClick);
                        }
                        //判断是否显示
                        if (elementInfo.isShow == "") {
                            // 默认显示
                            that.eleIsShow = true;
                            that.addClass("SView-show");
                            that.removeClass("SView-hide");
                        }
                        if ((typeof elementInfo.isShow) === "boolean") {
                            that.eleIsShow = elementInfo.isShow;
                            if (that.eleIsShow) {
                                that.addClass("SView-show");
                                that.removeClass("SView-hide");
                            }
                            else {
                                that.addClass("SView-hide");
                                that.removeClass("SView-show");
                            }
                        }
                    }
                    resolve(that);
                });
            }
            /**
             * @description 创建选择器标签
             * @param selectInfo  选择器信息
             */
            createLabel(selectInfo) {
                let that = this;
                return new Promise((resolve, reject) => {
                    resolve(that);
                });
            }
            /**
             * @description 设置点击事件
             * @param onClickEvent 点击事件
             */
            setOnClickEvent(onClickEvent, parentNode) {
                if (onClickEvent) {
                    this.eleOnClick = onClickEvent;
                    if ((typeof onClickEvent) === "string") {
                        this.eleLabel.setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        this.eleLabel.onclick = function () {
                            that.eleOnClick(parentNode ? parentNode : that);
                        };
                    }
                }
            }
            /**
             * @description 设置change事件
             * @param onChangeEvent change事件
             */
            setOnChangeEvent(onChangeEvent, parentNode) {
                if (onChangeEvent) {
                    this.eleOnChange = onChangeEvent;
                    if ((typeof onChangeEvent) === "string") {
                        this.eleLabel.setAttribute("onchange", this.eleOnChange);
                    }
                    else if ((typeof onChangeEvent) === "function") {
                        let that = this;
                        this.eleLabel.onchange = function () {
                            that.eleOnChange(parentNode ? parentNode : that);
                        };
                    }
                }
            }
            /**
             * @description 添加标签class，原class保留
             * @param className 标签class名称
             */
            addClass(className) {
                if (className && ((typeof className) === "string") && this.eleClasses.indexOf(className) == -1) {
                    this.eleClasses.push(className);
                    if (this.eleLabel) {
                        this.eleLabel.classList.add(className);
                    }
                }
            }
            /**
             * @description 设置标签class，直接替换，原class被删除
             * @param classNames 标签class名称
             */
            setClasses(classNames) {
                if (classNames && (classNames instanceof Array)) {
                    this.eleClasses = classNames;
                    if (this.eleLabel) {
                        this.eleLabel.setAttribute("class", classNames.join(" "));
                    }
                }
            }
            /**
             * @description 删除标签class
             * @param className 标签名称
             */
            removeClass(className) {
                if (className) {
                    let index = this.eleClasses.indexOf(className);
                    if (index >= 0) {
                        this.eleClasses.splice(index, 1);
                        if (this.eleLabel) {
                            this.eleLabel.classList.remove(className);
                        }
                    }
                }
            }
            /**
             * @description 获取标签class名
             */
            getClasses() {
                if (this.eleClasses) {
                    return this.eleClasses;
                }
                else {
                    return [];
                }
            }
            /**
             * @description 获取标签id名
             */
            getId() {
                return this.eleId;
            }
            /**
             * @description 设置标签id名
             * @param id
             */
            setId(id) {
                if (id && ((typeof id) === "string")) {
                    this.eleId = id;
                    this.eleLabel.setAttribute("id", this.eleId);
                }
            }
            getFlexDirection() {
                return this.flexDirection;
            }
            setFlexDirection(flexDirection) {
                this.flexDirection = flexDirection;
            }
            initFlexDirection() {
                if (this.getFlexDirection()) {
                    switch (this.getFlexDirection()) {
                        case Windows.FLEX_DIRECTION_ROW:
                            this.addClass(Windows.FLEX_DIRECTION_ROW_CLASS);
                            break;
                        case Windows.FLEX_DIRECTION_ROW_REVERSE:
                            this.addClass(Windows.FLEX_DIRECTION_ROW_REVERSE_CLASS);
                            break;
                        case Windows.FLEX_DIRECTION_COLUMN:
                            this.addClass(Windows.FLEX_DIRECTION_COLUMN_CLASS);
                            break;
                        case Windows.FLEX_DIRECTION_COLUMN_REVERSE:
                            this.addClass(Windows.FLEX_DIRECTION_COLUMN_REVERSE_CLASS);
                            break;
                    }
                }
            }
            /**
             * @description 隐藏标签
             * */
            hide() {
                this.eleIsShow = false;
                this.addClass("SView-hide");
                this.removeClass("SView-show");
                //this.eleLabel.style.display = "none";
            }
            /**
            * @description 标签是否显示
            * */
            isShow() {
                return this.eleIsShow;
            }
            /**
             * @description 通过id隐藏标签
             * @param id
             * */
            static hideById(id) {
                if (id && document.getElementById(id)) {
                    let label = document.getElementById(id);
                    label.style.display = "none";
                }
            }
            /**
             * @description 显示标签
             * */
            show() {
                this.eleIsShow = true;
                this.addClass("SView-show");
                this.removeClass("SView-hide");
                //this.eleLabel.style.display = ""
            }
            /**
             * @description 通过id显示标签
             * @param id
             * */
            static showById(id) {
                if (id && document.getElementById(id)) {
                    let label = document.getElementById(id);
                    label.style.display = "";
                }
            }
            /**
            * @description 移除标签
            * */
            delete() {
                this.eleLabel.parentNode.removeChild(this.eleLabel);
                this.eleLabel = null;
            }
            /**
             * @description 通过id移除标签
             * @param id
             * */
            //static removeById(id: string) {
            //    let label = document.getElementById(id);
            //    label.parentNode.removeChild(label);
            //}
            /**
             * @description 设置其他属性
             */
            setAttribute(key, value) {
                this.eleLabel.setAttribute(key, value);
            }
            /**
             * @description 设置是否禁用
             */
            setIsDisable(value) {
                if (value && value == true) {
                    if (this.isDisable == false) {
                        this.addClass("SView-disabled");
                    }
                }
                else {
                    this.removeClass("SView-disabled");
                }
                this.isDisable = value;
            }
            /**
             * @description 获取标签
             */
            getLabel() {
                return this.eleLabel;
            }
            /**
             * @description 添加标签至网页
             */
            addLabel(id) {
                let fatherElement;
                if (id && document.getElementById(id)) {
                    fatherElement = document.getElementById(id);
                }
                else {
                    fatherElement = document.getElementsByTagName("body")[0];
                }
                fatherElement.appendChild(this.eleLabel);
            }
            /**
             * @description 设置基础路径
             */
            static setRootPath(path) {
                this.rootPath = path;
            }
        }
        Label.rootPath = './';
        Windows.Label = Label;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：动画列表
         *@author WZ
         *@date		2022-2-9
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class AnimationList extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.stepsId = 0;
                this.eleClasses = ["SView-animationList"];
                this.animationListData = [];
                this.nodeClickFunction = null;
                this.stepsId = 0;
            }
            setAni(childProcessList) {
                var zNodesTemp = null;
                for (var i = 0; i < childProcessList.length; i++) {
                    var childZNodeTemp = null;
                    if (childProcessList[i].getChildProcessList() && childProcessList[i].getChildProcessList().length > 0) {
                        childZNodeTemp = this.setAni(childProcessList[i].getChildProcessList());
                    }
                    var stepList = childProcessList[i].getStepList();
                    for (var j = 0; j < stepList.length; j++) {
                        if (stepList[j].getType() === 2) {
                            childZNodeTemp != null ? (childZNodeTemp += ('{ name:' + '"' + stepList[j].getName() + '",id:"' + 'processId_' + stepList[j].getId() + '"},')) : (childZNodeTemp = ('{ name:' + '"' + stepList[j].getName() + '",id:"' + 'processId_' + stepList[j].getId() + '" },'));
                        }
                        else {
                            childZNodeTemp != null ? (childZNodeTemp += ('{ name:' + '"' + stepList[j].getName() + '",id:"' + stepList[j].getId() + '",processId:"' + childProcessList[i].getId() + '",steps_id:"' + this.stepsId + '",step_info:"' + stepList[j].getStepInfo() + '" },')) : (childZNodeTemp = ('{ name:' + '"' + stepList[j].getName() + '",id:"' + stepList[j].getId() + '",processId:"' + childProcessList[i].getId() + '",steps_id:"' + this.stepsId + '",step_info:"' + stepList[j].getStepInfo() + '" },'));
                        }
                        ++this.stepsId;
                    }
                    if (childZNodeTemp) {
                        zNodesTemp != null ? (zNodesTemp += ('{name:"' + childProcessList[i].getName() + '",id:"' + 'processId_' + childProcessList[i].getId() + '",open:true,children:[' + childZNodeTemp + ']},')) : (zNodesTemp = ('{name:"' + childProcessList[i].getName() + '",id:"' + 'processId_' + childProcessList[i].getId() + '",open:true,children:[' + childZNodeTemp + ']},'));
                    }
                    else {
                        zNodesTemp != null ? (zNodesTemp += ('{name:"' + childProcessList[i].getName() + '",id:"' + 'processId_' + childProcessList[i].getId() + '",open:true,children:[]},')) : (zNodesTemp = ('{name:"' + childProcessList[i].getName() + '",id:"' + 'processId_' + childProcessList[i].getId() + '",open:true,children:[]},'));
                    }
                }
                return zNodesTemp;
            }
            /**
            * @description 格式化数据
            * @param animationList 树形
            */
            animationDataFormat(taskList) {
                if (taskList != null && taskList.length > 1) {
                    var zNodesTemp = this.setAni(taskList);
                }
                else if (taskList != null && taskList.length == 1) {
                    var zNodesTemp = null;
                    var steps_id = 0;
                    var zNodeTemp = null;
                    if (Object.prototype.toString.call(taskList[0] === '[object Array]')) {
                        var stepList = taskList[0].getStepList();
                        for (var j = 0; j < stepList.length; j++) {
                            zNodeTemp != null ? (zNodeTemp += ('{ name:' + '"' + stepList[j].getName() + '",id:"' + stepList[j].getId() + '",processId:"' + taskList[0].getId() + '",steps_id:"' + steps_id + '",step_info:"' + stepList[j].getStepInfo() + '" },')) : (zNodeTemp = ('{ name:' + '"' + stepList[j].getName() + '",id:"' + stepList[j].getId() + '",processId:"' + taskList[0].getId() + '",steps_id:"' + steps_id + '",step_info:"' + stepList[j].getStepInfo() + '" },'));
                            ++steps_id;
                        }
                    }
                    zNodesTemp != null ? (zNodesTemp += ('{name:"' + taskList[0].getName() + '",id:"' + 'processId_' + taskList[0].getId() + '",open:true,children:[' + zNodeTemp + ']},')) : (zNodesTemp = ('{name:"' + taskList[0].getName() + '",id:"' + 'processId_' + taskList[0].getId() + '",open:true,children:[' + zNodeTemp + ']},'));
                }
                return eval('[' + zNodesTemp.replace(/\r\n/g, "</br>") + ']');
            }
            /**
                * @description 创建树
                * @param animationList 树
                */
            createLabel(animationListInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(animationListInfo)[0];
                    animationListInfo = animationListInfo[tagName];
                    // 外层div
                    let eleDiv = document.createElement("div");
                    let icon = document.createElement("div");
                    let that = this;
                    icon.setAttribute("class", "SView-animationList-icon SView-animationList-iconOpen");
                    icon.onclick = function () {
                        that.showAnimationDialog(this);
                    };
                    if (animationListInfo.nodeClickFunction) {
                        this.nodeClickFunction = animationListInfo.nodeClickFunction;
                    }
                    let animationListObj;
                    // 格式化数据
                    if (!animationListInfo.data[0].children) {
                        animationListObj = this.animationDataFormat(animationListInfo.data);
                        //animationListObj = this.dataFormat(animationListObj);
                    }
                    else {
                        //animationListObj = this.dataFormat(animationListInfo.data);
                    }
                    let eleanimationList = document.createElement("div");
                    eleanimationList.setAttribute("class", "SView-animationListContent");
                    let animationListElement = this.loadanimationList(animationListObj, eleanimationList);
                    eleDiv.appendChild(animationListElement);
                    eleDiv.appendChild(icon);
                    this.eleLabel = eleDiv;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
                * @description 生成列表
                * @param animationList 列表
                */
            loadanimationList(animationListObj, element) {
                let that = this;
                var html1, nodeClass, html, name, imageSrc, image, fold, foldClass, elementNode;
                for (var c = 0; c < animationListObj.length; c++) {
                    nodeClass = "SView-animationList-subItem SView-animationList-show";
                    foldClass = animationListObj[c].children ? animationListObj[c].children.length > 0 ? "animationListDetail animationList-open" : "animationListDetail animationList-close" : "animationListDetail";
                    html = document.createElement("div");
                    html.setAttribute("class", "SView-animationList-item");
                    html1 = document.createElement("div");
                    fold = document.createElement("span");
                    fold.setAttribute("class", foldClass);
                    if (animationListObj[c].children && animationListObj[c].children.length > 0) {
                        fold.onclick = function () {
                            that.dropClick(this);
                        };
                    }
                    name = document.createElement("span");
                    name.setAttribute("class", "node-name");
                    name.setAttribute("node-id", animationListObj[c].id);
                    if (!animationListObj[c].children) {
                        name.setAttribute("id", "SView-node-name-" + animationListObj[c].processId + "-" + animationListObj[c].id);
                        name.setAttribute("node-info", animationListObj[c].step_info);
                        name.setAttribute("steps-id", animationListObj[c].steps_id);
                        name.onclick = function () {
                            that.nodeClick(this);
                        };
                    }
                    name.innerHTML = animationListObj[c].name;
                    image = document.createElement("img");
                    imageSrc = animationListObj[c].img || Windows.Label.rootPath + "images/icon/listDetail.png";
                    image.setAttribute("src", imageSrc);
                    if (!animationListObj[c].children) {
                        fold.appendChild(image);
                    }
                    html1.appendChild(fold);
                    html1.appendChild(name);
                    html.appendChild(html1);
                    element.appendChild(html);
                    if (animationListObj[c].children) {
                        elementNode = document.createElement("div");
                        elementNode.className = nodeClass;
                        nodeClass = "SView-animationList-subItem";
                        html.appendChild(elementNode);
                        this.loadanimationList(animationListObj[c].children, elementNode);
                    }
                }
                return element;
            }
            /**
                * @description 展开折叠
                * @param animationList 树
                */
            dropClick(element) {
                element.className = element.className.indexOf("animationList-close") > -1 ? "animationList-open" : "animationList-close";
                var b = element.parentNode.nextElementSibling;
                b.className =
                    b.className.indexOf("SView-animationList-show") > -1
                        ? "SView-animationList-subItem"
                        : "SView-animationList-subItem SView-animationList-show";
            }
            /**
                * @description 模型选中
                * @param animationList 树
                */
            selectNode(processId, id) {
                let nodeItem = document.getElementById("SView-node-name-" + processId + "-" + id);
                var nodeList = document.getElementsByClassName("SView-animationList")[0].querySelectorAll(".node-name");
                for (var f = 0; f < nodeList.length; f++) {
                    nodeList[f].className = "node-name";
                }
                if (nodeItem.className.indexOf("animationList-node-active") == -1) {
                    nodeItem.className = "node-name animationList-node-active";
                    let data = {
                        name: nodeItem.innerText,
                        steps_id: nodeItem.getAttribute("steps-id"),
                        step_info: nodeItem.getAttribute("node-info"),
                        parentName: nodeItem.parentNode.parentNode.parentNode.previousSibling.getElementsByClassName("node-name")[0].innerText
                    };
                    return data;
                }
                return null;
            }
            /**
            * @description 展开
            * @param
            */
            showAnimationDialog(element) {
                let className = this.eleLabel.className;
                if (element.className.indexOf("SView-animationList-iconOpen") > -1) {
                    element.className = "SView-animationList-icon SView-animationList-iconClose";
                    this.eleLabel.className = className + " SView-animationList-hidden";
                }
                else if (element.className.indexOf("SView-animationList-iconClose") > -1) {
                    element.className = "SView-animationList-icon SView-animationList-iconOpen";
                    this.eleLabel.className = this.eleLabel.className.replace(" SView-animationList-hidden", "");
                }
            }
            /**
             *
             * @description 选中
             * @param node node对象
             * @param type 类型
             */
            nodeClick(node) {
                var nodeList = document.getElementsByClassName("SView-animationList")[0].querySelectorAll(".node-name");
                for (var f = 0; f < nodeList.length; f++) {
                    nodeList[f].className = "node-name";
                }
                if (node.className.indexOf("animationList-node-active") == -1) {
                    node.className = "node-name animationList-node-active";
                    let data = {
                        name: node.innerText,
                        steps_id: node.getAttribute("steps-id"),
                        step_info: node.getAttribute("node-info"),
                        parentName: node.parentNode.parentNode.parentNode.previousSibling.getElementsByClassName("node-name")[0].innerText
                    };
                    this.nodeClickFunction(node, data);
                }
            }
            /**
             * @description 设置点击事件
             */
            setNodeClickFunction(func) {
                if (func instanceof Function) {
                    this.nodeClickFunction = func;
                }
            }
        }
        Windows.AnimationList = AnimationList;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：徽标
         *@author	ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Badge extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.badgeType = "";
                this.badgeContent = "";
                this.badgeBGColor = "";
                this.badgeSize = "";
                this.badgeColor = "";
                this.badgeFontSize = "";
                // 默认类名：右上角点徽标
                this.eleClasses = ["SView-badge-dot", "SView-badge-topRight"];
            }
            /**
             * @description 添加徽标
             * @param badgeInfo  徽标信息
             */
            createLabel(badgeInfo) {
                let tagName = Object.keys(badgeInfo)[0];
                badgeInfo = badgeInfo[tagName];
                let divElement = document.createElement("div");
                // 徽标类型
                if (badgeInfo.type && badgeInfo.type == "") {
                    // 默认徽标类型：点徽标
                    this.badgeType = "dotBadge";
                }
                else {
                    this.badgeType = badgeInfo.type;
                }
                this.badgeContent = badgeInfo.content;
                if (this.badgeType == "withContentBadge") {
                    this.badgeContent = badgeInfo.content;
                    let pElement = document.createElement("p");
                    pElement.innerText = this.badgeContent;
                    divElement.appendChild(pElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置徽标颜色
             * @param color 颜色
             */
            setBackgroundColor(color) {
                this.badgeBGColor = color;
                this.eleLabel.style.backgroundColor = this.badgeBGColor;
            }
            /**
             * @description 设置徽标文字颜色
             * @param color 颜色
             */
            setColor(color) {
                this.badgeColor = color;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.color = this.badgeColor;
            }
            /**
             * @description 设置徽标文字大小
             * @param value 文字大小
             */
            setFontSize(value) {
                if (typeof (value) == "number") {
                    this.badgeFontSize = value + "px";
                }
                else {
                    this.badgeFontSize = value;
                }
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.fontSize = this.badgeFontSize;
            }
            /**
             * @description 设置徽标大小
             * @param value 徽标大小
             */
            setSize(value) {
                if (typeof (value) == "number") {
                    this.badgeSize = value + "px";
                }
                else {
                    this.badgeSize = value;
                }
                this.eleLabel.style.height = this.badgeSize;
                this.eleLabel.style.width = this.badgeSize;
            }
            /**
             * @description 设置徽标显示内容
             * @param content 徽标显示内容
             */
            setContent(content) {
                if (typeof (content) == "number") {
                    this.badgeContent = content.toString();
                }
                else {
                    this.badgeContent = content;
                }
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.innerText = this.badgeContent;
            }
            /**
             * @description 获取徽标显示内容
             * @param content 徽标显示内容
             */
            getContent() {
                return this.badgeContent;
            }
        }
        Windows.Badge = Badge;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@description    UI通用控件：按钮 输入框按钮
         *@author	ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class InputButton extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.btnContent = "";
                this.btnImgsrc = "";
                this.btnWarningIsShow = false; // 默认警告不显示
                this.btnWarningContent = "";
                this.btnFontSize = "";
                this.btnColor = "";
                this.btnBGColor = "";
                this.btnWarningColor = "";
                this.btnWarningFontSize = "";
                this.eleClasses = ["SView-submitBtn"];
            }
            /**
             * @description 创建按钮标签
             * @param submitBtnInfo 提交信息
             */
            createLabel(submitBtnInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(submitBtnInfo)[0];
                    submitBtnInfo = submitBtnInfo[tagName];
                    let divElement = document.createElement("div");
                    ;
                    if (submitBtnInfo.imgsrc && submitBtnInfo.imgsrc != "") {
                        // 若按钮内容为图标
                        this.btnImgsrc = submitBtnInfo.imgsrc;
                        let imgElement = document.createElement("img");
                        imgElement.setAttribute("class", "SView-submitBtn-content");
                        imgElement.setAttribute("src", Windows.Label.rootPath + this.btnImgsrc);
                        divElement.appendChild(imgElement);
                    }
                    if (submitBtnInfo.btnTitle && submitBtnInfo.btnTitle != "") {
                        // 若按钮内容为文字
                        this.btnContent = submitBtnInfo.btnTitle;
                        let titleElement = document.createElement("span");
                        titleElement.setAttribute("class", "SView-submitBnt-content");
                        titleElement.innerText = this.btnContent;
                        divElement.appendChild(titleElement);
                    }
                    // 警示框
                    let warningElement = document.createElement("div");
                    warningElement.setAttribute("class", "SView-warningPopover");
                    warningElement.style.display = "none";
                    let contentElement = document.createElement("p");
                    this.btnWarningContent = submitBtnInfo.warning;
                    contentElement.innerText = this.btnWarningContent;
                    warningElement.appendChild(contentElement);
                    // 三角形
                    let triElement = document.createElement("div");
                    triElement.setAttribute("class", "SView-triangle");
                    warningElement.appendChild(triElement);
                    divElement.appendChild(warningElement);
                    // 警示框显隐
                    if (submitBtnInfo.warningIsShow == true) {
                        this.btnWarningIsShow = true;
                        warningElement.style.display = "";
                    }
                    else {
                        this.btnWarningIsShow = false;
                        warningElement.style.display = "none";
                    }
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置警示框显隐
             * @param isShow 是否显隐
             */
            setWarningIsShow(isShow) {
                let warningElement = this.eleLabel.getElementsByClassName("SView-warningPopover")[0];
                this.btnWarningIsShow = isShow;
                if (this.btnWarningIsShow == true) {
                    warningElement.style.display = "";
                }
                else {
                    warningElement.style.display = "none";
                }
            }
            /**
             * @description 设置按钮标签图标路径
             * @param imgsrc 图片路径
             */
            setImage(imgsrc) {
                this.btnImgsrc = imgsrc;
                let imgElement = this.eleLabel.getElementsByTagName("img")[0];
                imgElement.setAttribute("src", Windows.Label.rootPath + this.btnImgsrc);
            }
            /**
             * @description 设置按钮标签文字内容
             * @param content 文字内容
             */
            setContent(content) {
                this.btnContent = content;
                let pElement = this.eleLabel.getElementsByTagName("span")[0];
                pElement.innerText = this.btnContent;
            }
            /**
             * @description 设置按钮标签警告内容
             * @param content 文字内容
             */
            setWarningContent(content) {
                this.btnWarningContent = content;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.innerText = this.btnWarningContent;
            }
            /**
             * @description 获取按钮文字内容
             */
            getContent() {
                return this.btnContent;
            }
            /**
             * @description 获取按钮图片内容
             */
            getImage() {
                return this.btnImgsrc;
            }
            /**
             * @description 获取按钮警告内容
             */
            getWarning() {
                return this.btnWarningContent;
            }
            /**
             * @description 设置按钮文字大小
             * @param value
             */
            setFontSize(value) {
                if (typeof (value) == "number") {
                    this.btnFontSize = value + "px";
                }
                else {
                    this.btnFontSize = value;
                }
                this.eleLabel.style.fontSize = this.btnFontSize;
            }
            /**
             * @description 设置按钮警示文字大小
             * @param value 警示文字大小
             */
            setWarningFontSize(value) {
                if (typeof (value) == "number") {
                    this.btnWarningFontSize = value + "px";
                }
                else {
                    this.btnWarningFontSize = value;
                }
                this.eleLabel.getElementsByTagName("p")[0].style.fontSize = this.btnWarningFontSize;
            }
            /**
             * @description 设置按钮文字颜色
             * @param color 文字颜色
             */
            setColor(color) {
                this.btnColor = color;
                this.eleLabel.style.color = this.btnColor;
            }
            /**
             * @description  设置按钮警示文字颜色
             * @param color 文字颜色
             */
            setWarningColor(color) {
                this.btnWarningColor = color;
                this.eleLabel.getElementsByTagName("p")[0].style.color = this.btnWarningColor;
            }
            /**
             * @description  设置按钮背景颜色
             * @param color 背景颜色
             */
            setBGColor(color) {
                this.btnBGColor = color;
                this.eleLabel.style.backgroundColor = this.btnBGColor;
            }
        }
        Windows.InputButton = InputButton;
        /**
         * @description 按钮集
         *
         * */
        class Button extends Windows.Label {
            // private buttonBadge: any;
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.btnSubComponents = [];
                this.btnContent = "";
                this.btnImgsrc = "";
                this.btnImgOnclick = "";
                this.btnTitle = "";
                this.btnFontSize = "";
                this.btnColor = "";
                this.btnBGColor = "";
                this.backgroundTopColor = "";
                this.backgroundBottomColor = "";
                this.btnHighLightColor = "rgba(44,135,255, .5)"; // 默认高亮颜色
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.btnType = "";
                // 默认类名：带边框图标按钮
                this.eleClasses = ["SView-button-iconBtn", "SView-button-iconBtn-withBorder"];
                // 按钮默认不高亮
                this.btnHighlight = false;
            }
            /**
             * @description 创建按钮标签
             * @param buttonInfo 按钮信息
             */
            createLabel(buttonInfo) {
                let tagName = Object.keys(buttonInfo)[0];
                buttonInfo = buttonInfo[tagName];
                let divElement = document.createElement("div");
                // 设置按钮属性内容
                if (buttonInfo.type && buttonInfo.type != "") {
                    this.btnType = buttonInfo.type;
                }
                else {
                    // 默认按钮类型，图标按钮
                    this.btnType = "iconButton";
                }
                if (buttonInfo.highLight) {
                    this.btnHighlight = buttonInfo.highLight;
                }
                else {
                    // 默认按钮不高亮
                    this.btnHighlight = false;
                }
                // 按钮是否高亮
                if (this.btnHighlight == true) {
                    //divElement.style.backgroundColor = "#bae7ff";
                    //divElement.style.backgroundColor = "rgba(44,135,255, .5)";
                    if (this.btnType == "colorButton") {
                        buttonInfo.classes.push("SView-color-button-active");
                    }
                    else {
                        divElement.style.backgroundColor = this.btnHighLightColor;
                    }
                }
                if (buttonInfo.imgsrc && buttonInfo.imgsrc != "") {
                    this.btnImgsrc = buttonInfo.imgsrc;
                }
                if (buttonInfo.title && buttonInfo.title != "") {
                    this.btnTitle = buttonInfo.title;
                }
                if (buttonInfo.imgOnClick && buttonInfo.imgOnClick != "") {
                    this.btnImgOnclick = buttonInfo.imgOnClick;
                }
                if (buttonInfo.content && buttonInfo.content != "") {
                    this.btnContent = buttonInfo.content;
                }
                if (buttonInfo.background && buttonInfo.background != "") {
                    this.btnBGColor = buttonInfo.background;
                }
                if (buttonInfo.backgroundTopColor && buttonInfo.backgroundTopColor != "") {
                    this.backgroundTopColor = buttonInfo.backgroundTopColor;
                }
                if (buttonInfo.backgroundBottomColor && buttonInfo.backgroundBottomColor != "") {
                    this.backgroundBottomColor = buttonInfo.backgroundBottomColor;
                }
                // 创建按钮内所含标签
                if (this.btnType == "iconButton") {
                    let imgElement = this.createImage();
                    divElement.appendChild(imgElement);
                }
                else if (this.btnType == "basicButton") {
                    let pElement = this.createContent();
                    divElement.appendChild(pElement);
                }
                else if (this.btnType.split("_")[0] == "iconTextButton") {
                    let childElement = this.createIconText();
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                else if (this.btnType == "colorButton") {
                    let childElement = this.createColor();
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                // 按钮是否带子控件
                if (buttonInfo.subComponents && buttonInfo.subComponents.length > 0) {
                    let subComponents = buttonInfo.subComponents;
                    for (let i = 0; i < subComponents.length; i++) {
                        this.btnSubComponents.push(subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        //let newElement = createSViewObject(UI, tagName);
                        /*newElement.createLabel(subComponents[i][tagName]);*/
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.getLabel();
                        divElement.append(newLabel);
                    }
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
            * @description 创建按钮文字标签
            */
            createContent() {
                let pElement = document.createElement("p");
                pElement.innerText = this.btnContent;
                return pElement;
            }
            /**
             * @description 创建按钮图标标签
             */
            createImage() {
                let imgElement = document.createElement("img");
                if (this.btnImgsrc != "") {
                    imgElement.setAttribute("src", Windows.Label.rootPath + this.btnImgsrc);
                }
                if (this.btnTitle != "") {
                    imgElement.setAttribute("title", this.btnTitle);
                }
                if (this.btnImgOnclick != "") {
                    imgElement.setAttribute("onclick", this.btnImgOnclick);
                }
                return imgElement;
            }
            /**
             * @description 创建按钮文字+图标标签
             */
            createIconText() {
                let childElement = [];
                let btnType = this.btnType.split("_")[1];
                let imgElement = this.createImage();
                // left、right等位置，指图标所在位置
                if (btnType == "left" || btnType == "right" || btnType == undefined) {
                    let spanElement = document.createElement("span");
                    spanElement.innerText = this.btnContent;
                    if (btnType == "right") {
                        childElement.push(spanElement);
                        childElement.push(imgElement);
                    }
                    else {
                        childElement.push(imgElement);
                        childElement.push(spanElement);
                    }
                }
                else if (btnType == "top" || btnType == "bottom") {
                    let pElement = document.createElement("p");
                    pElement.innerText = this.btnContent;
                    if (btnType == "top") {
                        childElement.push(imgElement);
                        childElement.push(pElement);
                    }
                    else {
                        childElement.push(pElement);
                        childElement.push(imgElement);
                    }
                }
                return childElement;
            }
            /**
             * @description 创建颜色按钮
             */
            createColor() {
                let childElement = [];
                let pElement = document.createElement("img");
                pElement.setAttribute("src", Windows.Label.rootPath + "images/icon/select.png");
                let colorElement = document.createElement("div");
                colorElement.setAttribute("class", "SView-colorButton-bg");
                if (this.backgroundTopColor != "" && this.backgroundBottomColor != "") {
                    colorElement.style.background = "linear-gradient(" + this.backgroundTopColor + "," + this.backgroundBottomColor + ")";
                }
                else {
                    colorElement.style.backgroundColor = this.btnBGColor;
                }
                childElement.push(pElement);
                childElement.push(colorElement);
                return childElement;
            }
            /**
             * @description 添加子控件
             * @param subComponents 组件数组
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.childNodes;
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.btnSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.btnSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        //let newElement = createSViewObject(UI, tagName);
                        /*newElement.createLabel(subComponents[i][tagName]);*/
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    //this.eleLabel.appendChild(newLabel);
                    if (this.btnType == "iconTextButton") {
                        this.eleLabel.insertBefore(newLabel, components[position + 1 + i]);
                    }
                    else {
                        this.eleLabel.insertBefore(newLabel, components[position + i]);
                    }
                }
            }
            /**
             * @description 移除子控件（位置从1开始）
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.children;
                let deleteElement;
                if (this.btnType == "iconTextButton") {
                    deleteElement = components[position + 1];
                }
                else {
                    deleteElement = components[position];
                }
                this.btnSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 调整子控件（位置从1开始）
             * @param oldPosition
             * @param newPosition
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.children;
                let changeElement;
                if (this.btnType == "iconTextButton") {
                    changeElement = components[oldPosition + 1];
                }
                else {
                    changeElement = components[oldPosition];
                }
                let elementInfo = this.btnSubComponents[oldPosition - 1];
                this.btnSubComponents.splice(oldPosition - 1, 1);
                this.btnSubComponents.splice(newPosition - 1, 0, elementInfo);
                changeElement.parentNode.removeChild(changeElement);
                //components[newPosition - 1].appendChild(changeElement);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1].nextSibling);
            }
            /**
             * @description 设置按钮文字内容
             * @param content
             */
            setContent(content) {
                this.btnContent = content;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                if (pElement) {
                    pElement.innerText = this.btnContent;
                }
                else if (spanElement) {
                    spanElement.innerText = this.btnContent;
                }
            }
            /**
             * @description 设置按钮图片内容
             * @param imgsrc 图片路径
             * @param title 标题
             * @param imgOnClick 点击的图片
             */
            setImage(imgsrc = "", title = "", imgOnClick = "") {
                let imgElement = this.eleLabel.getElementsByTagName("img")[0];
                if (imgsrc != "") {
                    this.btnImgsrc = imgsrc;
                    imgElement.setAttribute("src", Windows.Label.rootPath + this.btnImgsrc);
                }
                if (title != "") {
                    this.btnTitle = title;
                    imgElement.setAttribute("title", this.btnTitle);
                }
                if (imgOnClick != "") {
                    this.btnImgOnclick = imgOnClick;
                    imgElement.setAttribute("onclick", this.btnImgOnclick);
                }
            }
            /**
             * @description 设置按钮图片内容
             * @param imgOnClick 点击的图片
             */
            setImageClick(imgOnClick) {
                let imgElement = this.eleLabel.getElementsByTagName("img")[0];
                if (imgOnClick) {
                    this.btnImgOnclick = imgOnClick;
                    if ((typeof imgOnClick) === "string") {
                        imgElement.setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        imgElement.onclick = function () {
                            that.btnImgOnclick();
                        };
                    }
                }
            }
            /**
             * @description 设置按钮高亮
             * @param highLight 是否高亮
             */
            setHighLight(highLight) {
                if (highLight == false) {
                    if (this.btnType == "colorButton") {
                        if (this.btnHighlight == true) {
                            this.eleClasses.pop();
                            this.setClasses(this.eleClasses);
                        }
                    }
                    else {
                        this.eleLabel.style.background = this.btnBGColor;
                    }
                }
                else {
                    if (this.btnType == "colorButton") {
                        if (this.btnHighlight == false) {
                            this.eleClasses.push("SView-color-button-active");
                            this.setClasses(this.eleClasses);
                        }
                    }
                    else {
                        this.eleLabel.style.backgroundColor = this.btnHighLightColor;
                    }
                }
                this.btnHighlight = highLight;
            }
            getHighLight() {
                return this.btnHighlight;
            }
            /**
             * @description 设置按钮文字大小
             * @param value 文字大小
             */
            setFontSize(value) {
                if (typeof (value) == "number") {
                    this.btnFontSize = value + "px";
                }
                else {
                    this.btnFontSize = value;
                }
                this.eleLabel.style.fontSize = this.btnFontSize;
            }
            /**
             * @description 设置按钮文字颜色
             * @param color 颜色
             */
            setColor(color) {
                this.btnColor = color;
                this.eleLabel.style.color = this.btnColor;
            }
            /**
             * @description 设置按钮背景颜色
             * @param color 颜色
             */
            setBGColor(color) {
                this.btnBGColor = color;
                this.eleLabel.style.backgroundColor = this.btnBGColor;
            }
            /**
             * @description 设置按钮背景颜色
             * @param color 颜色
             */
            setGradientColor(topCcolor, bottomColor) {
                if (topCcolor != "" && bottomColor != "") {
                    this.backgroundTopColor = topCcolor;
                    this.backgroundBottomColor = bottomColor;
                    this.eleLabel.getElementsByClassName("SView-colorButton-bg")[0].style.background = "linear-gradient(" + this.backgroundTopColor + "," + this.backgroundBottomColor + ")";
                }
            }
            /**
             * @description 设置按钮背景颜色
             * @param color 颜色
             */
            getGradientColor() {
                return [this.backgroundTopColor, this.backgroundBottomColor];
            }
            /**
             * @description 设置按钮高亮颜色
             * @param color 颜色
             */
            setHighLightColor(color) {
                this.btnHighLightColor = color;
                if (this.btnHighlight == true) {
                    this.eleLabel.style.backgroundColor = this.btnHighLightColor;
                }
            }
            /**
             * @description 获取按钮文字内容
             */
            getContent() {
                return this.btnContent;
            }
            /**
             * @description 获取按钮图片内容
             */
            getImage() {
                return [this.btnImgsrc, this.btnTitle, this.btnImgOnclick];
            }
            /**
             * @description 高亮/取消高亮按钮标签
             * @param buttonId
             */
            //highLightLabel(buttonId?: string) {
            //    let highLightBtn;
            //    if (buttonId) {
            //        highLightBtn = document.getElementById(buttonId);
            //    } else {
            //        highLightBtn = this.eleLabel;
            //    }
            //    if (highLightBtn.style.backgroundColor == this.btnBGColor) {
            //        //highLightBtn.style.backgroundColor = "#bae7ff";
            //        //highLightBtn.style.backgroundColor = "rgba(44,135,255, .5)";
            //        highLightBtn.style.backgroundColor = this.highLightLabel;
            //    } else {
            //        highLightBtn.style.backgroundColor = this.btnBGColor;
            //    }
            //}
            /**
                 * @description  高亮/取消高亮按钮标签
                 * @param buttonId 按钮id
                 */
            static highLightLabel(buttonId, hightLight = true) {
                let highLightBtn = document.getElementById(buttonId);
                if (!highLightBtn) {
                    return;
                }
                if (!hightLight) {
                    highLightBtn.style.backgroundColor = "";
                    return;
                }
                if (highLightBtn.style.backgroundColor == "") {
                    // highLightBtn.style.backgroundColor = "#bae7ff";
                    highLightBtn.style.backgroundColor = "rgba(44,135,255, .5)";
                }
                else {
                    highLightBtn.style.backgroundColor = "";
                }
            }
        }
        Windows.Button = Button;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：复选框
         *@author	ZYC
         *@date		2022-2-11
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Checkbox extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.checkboxLabel = "";
                this.checkboxPlaceholder = "";
                this.checkBoxLabelFontSize = "";
                this.checkBoxLabelColor = "";
                this.checkBoxIsChecked = false;
                this.disabled = false;
                // 默认标签在前复选框
                this.checkboxType = "labelFront";
                // 默认类名
                this.eleClasses = ["SView-checkbox"];
            }
            /**
             * @description  创建复选框标签
             * @param checkboxInfo 选中信息
             */
            createLabel(checkboxInfo) {
                let tagName = Object.keys(checkboxInfo)[0];
                checkboxInfo = checkboxInfo[tagName];
                let divElement = document.createElement("div");
                // 创建复选框
                let checkboxElement = document.createElement("input");
                checkboxElement.setAttribute("type", "checkbox");
                checkboxElement.setAttribute("id", checkboxInfo.checkboxId);
                if (checkboxInfo.value && typeof checkboxInfo.value == "boolean") {
                    this.checkBoxIsChecked = checkboxInfo.value;
                    checkboxElement.checked = this.checkBoxIsChecked;
                }
                if (checkboxInfo.disabled && (typeof checkboxInfo.disabled) == "boolean") {
                    this.disabled = true;
                    checkboxElement.setAttribute("disabled", "true");
                }
                // 若无标签
                if (!checkboxInfo.label || checkboxInfo.label == "") {
                    divElement.appendChild(checkboxElement);
                }
                else {
                    if (checkboxInfo.type && checkboxInfo.type != "") {
                        this.checkboxType = checkboxInfo.type;
                    }
                    else {
                        // 默认复选框类型：label在前
                        this.checkboxType = "labelFront";
                    }
                    // 创建标签
                    this.checkboxLabel = checkboxInfo.label;
                    let labelElement = document.createElement("label");
                    labelElement.setAttribute("for", checkboxInfo.checkboxId);
                    labelElement.innerText = this.checkboxLabel;
                    checkboxElement.setAttribute("id", checkboxInfo.checkboxId);
                    if (this.checkboxType == "labelBack") {
                        divElement.appendChild(checkboxElement);
                        divElement.appendChild(labelElement);
                    }
                    else {
                        divElement.appendChild(labelElement);
                        divElement.appendChild(checkboxElement);
                    }
                }
                if (checkboxInfo.placeholder && checkboxInfo.placeholder != "") {
                    this.checkboxPlaceholder = checkboxInfo.placeholder;
                    let pElement = document.createElement("p");
                    pElement.innerText = checkboxInfo.placeholder;
                    divElement.appendChild(pElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置复选框标签内容
             * @param content 内容
             */
            setContent(content) {
                this.checkboxLabel = content;
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.innerText = this.checkboxLabel;
            }
            /**
             * @description 设置提示内容
             * @param content 内容
             */
            setPlaceholder(content) {
                this.checkboxPlaceholder = content;
                let labelElement = this.eleLabel.getElementsByTagName("p")[0];
                labelElement.innerText = this.checkboxPlaceholder;
            }
            /**
             * @description 获取复选框标签内容
             * @param content 内容
             */
            getContent() {
                return this.checkboxLabel;
            }
            /**
             * @description 设置复选框标签字体大小
             * @param value 字体大小
             */
            setFontSize(value) {
                if (typeof (value) == "number") {
                    this.checkBoxLabelFontSize = value + "px";
                }
                else {
                    this.checkBoxLabelFontSize = value;
                }
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.style.fontSize = this.checkBoxLabelFontSize;
            }
            /**
             * @description 设置复选框标签字体颜色
             * @param value 字体颜色
             */
            setColor(color) {
                this.checkBoxLabelColor = color;
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.style.color = this.checkBoxLabelColor;
            }
            /**
             * @description 设置复选框标签是否选中
             * @param value 是否选中
             */
            setValue(value) {
                this.checkBoxIsChecked = value;
                let labelElement = this.eleLabel.getElementsByTagName("input")[0];
                labelElement.checked = this.checkBoxIsChecked;
            }
            /**
             * @description 获取框标签是否选中
             * @param value 是否选中
             */
            getValue() {
                let labelElement = this.eleLabel.getElementsByTagName("input")[0];
                this.checkBoxIsChecked = labelElement.checked;
                return this.checkBoxIsChecked;
            }
            /**
             * @description 设置禁用
             */
            isDisabled(value) {
                let labelElement = this.eleLabel.getElementsByTagName("input")[0];
                this.disabled = value;
                if (value) {
                    labelElement.setAttribute("disabled", this.disabled.toString());
                }
                else {
                    labelElement.removeAttribute("disabled");
                }
            }
        }
        Windows.Checkbox = Checkbox;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
// 列表
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：展开列表
         *@author WZ
         *@date		2022-2-9
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class CollapseItem extends Windows.Label {
            /**
             * 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.listLabelContent = "";
                this.listLabelFontSize = "";
                this.listLabelColor = "";
            }
            /**
             * @description 创建列表
             * @param listInfo 列表
             */
            createLabel(listInfo) {
                let tagName = Object.keys(listInfo)[0];
                listInfo = listInfo[tagName];
                let liElement = document.createElement("li");
                let labelElement = document.createElement("label");
                this.listLabelContent = listInfo.label;
                labelElement.innerHTML = this.listLabelContent;
                let spanElement = document.createElement("span");
                // 子控件
                if (listInfo.subComponents && listInfo.subComponents.length > 0) {
                    let subComponents = listInfo.subComponents;
                    for (let i = 0; i < subComponents.length; i++) {
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        //let newElement = createSViewObject(UI, tagName);
                        /*newElement.createLabel(subComponents[i][tagName]);*/
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        spanElement.appendChild(newLabel);
                    }
                }
                liElement.appendChild(labelElement);
                liElement.appendChild(spanElement);
                this.eleLabel = liElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置列表标签标题内容
             * @param content 内容
             */
            setLabelContent(content) {
                this.listLabelContent = content;
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.innerHTML = this.listLabelContent;
            }
            /**
             * @description 获取列表标签标题内容
             */
            getLabelContent() {
                return this.listLabelContent;
            }
            /**
             * @description 设置列表字体大小
             * @param value
             */
            setLabelFontSize(value) {
                if (typeof (value) == "number") {
                    this.listLabelFontSize = value + "px";
                }
                else {
                    this.listLabelFontSize = value;
                }
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.style.fontSize = this.listLabelFontSize;
            }
            /**
             * @description 设置列表字体颜色
             * @param color 颜色
             */
            setLabelColor(color) {
                this.listLabelColor = color;
                let labelElement = this.eleLabel.getElementsByTagName("label")[0];
                labelElement.style.color = this.listLabelColor;
            }
        }
        Windows.CollapseItem = CollapseItem;
        /**
         * @description 折叠栏
         * */
        class Collapse extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.collapseTitle = "";
                this.collapseDetailLabel = [];
                this.collapseDetailInfo = [];
                this.collapseTitleFontSize = "";
                this.collapseTitleColor = "";
                this.collapseTitleBGColor = "";
                this.collapseDetailLabelFontSize = "";
                this.collapseDetailLabelColor = "";
                this.collapseDetailInfoFontSize = "";
                this.collapseDetailInfoColor = "";
                this.collapseDetailBGColor = "";
                this.collapseSubComponents = [];
                this.collapseShowDetail = false;
                this.eleClasses = ["SView-collapse"];
            }
            /**
             * @description 创建列表
             * @param Collapse 折叠栏信息
             */
            createLabel(collapseInfo) {
                let tagName = Object.keys(collapseInfo)[0];
                collapseInfo = collapseInfo[tagName];
                // 外层div
                let eleDiv = document.createElement("ul");
                if (collapseInfo.dataClass && collapseInfo.dataClass != "") {
                    eleDiv.setAttribute("data-class", collapseInfo.dataClass);
                }
                let eleLi = document.createElement("li");
                // 标题
                let eleTitle = document.createElement("div");
                eleTitle.setAttribute("class", "SView-collapse-item");
                let that = this;
                eleTitle.onclick = function () {
                    that.setCollapseShowDetail();
                };
                let eleIcon = document.createElement("span");
                let eleUl = document.createElement("ul");
                if (collapseInfo.showDetail != null && typeof (collapseInfo.showDetail) == "boolean") {
                    this.collapseShowDetail = collapseInfo.showDetail;
                }
                if (!this.collapseShowDetail) {
                    eleUl.style.display = "none";
                    eleIcon.setAttribute("class", "SView-icon-close");
                }
                else {
                    eleIcon.setAttribute("class", "SView-icon-open");
                }
                let eleSpan = document.createElement("span");
                eleSpan.setAttribute("class", "SView-collapse-title");
                this.collapseTitle = collapseInfo.title;
                eleSpan.innerHTML = this.collapseTitle;
                eleTitle.appendChild(eleSpan);
                eleTitle.appendChild(eleIcon);
                eleUl.setAttribute("class", "SView-collapse-detail");
                // 若存在子控件
                if (collapseInfo.subComponents && collapseInfo.subComponents.length > 0) {
                    let subComponents = collapseInfo.subComponents;
                    for (let i = 0; i < subComponents.length; i++) {
                        this.collapseSubComponents.push(subComponents[i]);
                        // 获取子控件生成类型
                        /*let newLabel = this.setLabel(subComponents[i], this.allSubComponents);*/
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        eleUl.appendChild(newLabel);
                    }
                }
                else {
                    for (let detail in collapseInfo.detailInfo) {
                        let elDetailLi = document.createElement("li");
                        let eleLabel = document.createElement("label");
                        this.collapseDetailLabel.push(collapseInfo.detailInfo[detail].label);
                        eleLabel.innerHTML = collapseInfo.detailInfo[detail].label;
                        this.collapseDetailInfo.push(collapseInfo.detailInfo[detail].value);
                        let eleValue = document.createElement("span");
                        eleValue.innerHTML = collapseInfo.detailInfo[detail].value;
                        elDetailLi.appendChild(eleLabel);
                        elDetailLi.appendChild(eleValue);
                        eleUl.appendChild(elDetailLi);
                    }
                }
                eleLi.appendChild(eleTitle);
                eleLi.appendChild(eleUl);
                eleDiv.appendChild(eleLi);
                this.eleLabel = eleDiv;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed");
                });
            }
            /**
             * @description 设置详情显隐
             */
            setCollapseShowDetail(value) {
                if (value != null) {
                    this.collapseShowDetail = value;
                }
                else {
                    this.collapseShowDetail = !this.collapseShowDetail;
                }
                if (!this.collapseShowDetail) {
                    let detail = this.eleLabel.querySelector(".SView-collapse-detail");
                    if (detail) {
                        detail.style.display = "none";
                    }
                    let icon = this.eleLabel.querySelector(".SView-icon-open");
                    if (icon) {
                        icon.setAttribute("class", "SView-icon-close");
                    }
                }
                else {
                    let detail = this.eleLabel.querySelector(".SView-collapse-detail");
                    if (detail) {
                        detail.style.display = "block";
                    }
                    let icon = this.eleLabel.querySelector(".SView-icon-close");
                    if (icon) {
                        icon.setAttribute("class", "SView-icon-open");
                    }
                }
            }
            /**
             * @description 添加子控件
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0];
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.collapseSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.collapseSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    components.insertBefore(newLabel, components.children[position - 1 + i]);
                }
            }
            /**
             * @description 移除子控件
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].children;
                let deleteElement = components[position - 1];
                this.collapseSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 调整子控件位置
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0];
                let changeElement = components.children[oldPosition - 1];
                let elementInfo = this.collapseSubComponents[oldPosition - 1];
                this.collapseSubComponents.splice(oldPosition - 1, 1);
                this.collapseSubComponents.splice(newPosition - 1, 0, elementInfo);
                components.removeChild(components.children[oldPosition - 1]);
                components.insertBefore(changeElement, components.children[newPosition - 1]);
            }
            /**
             * @description 设置列表标题
             * @param title 标题
             */
            setTitle(title) {
                this.collapseTitle = title;
                let titleElement = this.eleLabel.getElementsByClassName("SView-collapse-title")[0];
                titleElement.innerHTML = this.collapseTitle;
            }
            /**
             * @description 设置列表细节标题标签(位置从1开始)
             * @param position 位置
             * @param title 标题
             */
            setDetailLabel(position, title) {
                this.collapseDetailLabel[position - 1] = title;
                let labelElement = this.eleLabel.getElementsByTagName("label")[position - 1];
                labelElement.innerHTML = this.collapseDetailLabel[position - 1];
            }
            /**
             * @description 设置列表细节内容(位置从1开始)
             * @param position 位置
             * @param content 内容
             */
            setDetailInfo(position, content) {
                this.collapseDetailInfo[position - 1] = content;
                let detailElement = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0];
                let spanElement = detailElement.getElementsByTagName("span");
                spanElement[position - 1].innerHTML = this.collapseDetailInfo[position - 1];
            }
            /**
             * @description 获取列表标题
             */
            getTitle() {
                return this.collapseTitle;
            }
            /**
             * @description 获取列表细节标题
             */
            getDetailLabel() {
                return this.collapseDetailLabel;
            }
            /**
             * @description 获取列表细节内容
             */
            getDetailInfo() {
                return this.collapseDetailInfo;
            }
            /**
             * @description 获取列表子控件
             */
            getSubComponents() {
                return this.collapseSubComponents;
            }
            /**
             * @description 设置列表标题字体大小
             * @param value 字体大小
             */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.collapseTitleFontSize = value + "px";
                }
                else {
                    this.collapseTitleFontSize = value;
                }
                let titleElement = this.eleLabel.getElementsByClassName("SView-collapse-title")[0];
                titleElement.style.fontSize = this.collapseTitleFontSize;
            }
            /**
             * @description 设置列表标题字体颜色
             * @param color 颜色
             */
            setTitleColor(color) {
                this.collapseTitleColor = color;
                let titleElement = this.eleLabel.getElementsByClassName("SView-collapse-title")[0];
                titleElement.style.color = this.collapseTitleColor;
            }
            /**
             * @description 设置列表标题背景颜色
             * @param color 颜色
             */
            setTitleBGColor(color) {
                this.collapseTitleBGColor = color;
                this.eleLabel.style.backgroundColor = this.collapseTitleBGColor;
            }
            /**
             * @description 设置列表细节标题标签字体大小
             * @param value 字体大小
             */
            setDetailLabelFontSize(value) {
                if (typeof (value) == "number") {
                    this.collapseDetailLabelFontSize = value + "px";
                }
                else {
                    this.collapseDetailLabelFontSize = value;
                }
                let labels = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].getElementsByTagName("label");
                for (let i = 0; i < labels.length; i++) {
                    labels[i].style.fontSize = this.collapseDetailLabelFontSize;
                }
            }
            /**
             * @description 设置列表细节标题字体颜色
             * @param color 颜色
             */
            setDetailLabelColor(color) {
                this.collapseDetailLabelColor = color;
                let labels = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].getElementsByTagName("label");
                for (let i = 0; i < labels.length; i++) {
                    labels[i].style.color = this.collapseDetailLabelColor;
                }
            }
            /**
             * @description 设置列表细节内容字体大小
             * @param value 字体大小
             */
            setDetailInfoFontSize(value) {
                if (typeof (value) == "number") {
                    this.collapseDetailInfoFontSize = value + "px";
                }
                else {
                    this.collapseDetailInfoFontSize = value;
                }
                let infos = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].getElementsByTagName("span");
                for (let i = 0; i < infos.length; i++) {
                    infos[i].style.fontSize = this.collapseDetailInfoFontSize;
                }
            }
            /**
             * @description 设置列表细节内容字体颜色
             * @param color 颜色
             */
            setDetailInfoColor(color) {
                this.collapseDetailInfoColor = color;
                let infos = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].getElementsByTagName("span");
                for (let i = 0; i < infos.length; i++) {
                    infos[i].style.color = this.collapseDetailInfoColor;
                }
            }
            /**
             * @description 设置列表细节背景颜色
             * @param color 颜色
             */
            setDetailBGColor(color) {
                this.collapseDetailBGColor = color;
                let details = this.eleLabel.getElementsByClassName("SView-collapse-detail")[0].getElementsByTagName("li");
                for (let i = 0; i < details.length; i++) {
                    details[i].style.backgroundColor = this.collapseDetailBGColor;
                }
            }
        }
        Windows.Collapse = Collapse;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：颜色选择器
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Colorpicker extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.initColor = "rgb(255,0,0)";
                this.allMode = ['hex', 'rgb'];
                this.color = "";
                this.bindElem = null;
                this.elem_wrap = null;
                this.fixedBg = null;
                this.elem_colorPancel = null;
                this.elem_picker = null;
                this.elem_barPicker1 = null;
                this.elem_hexInput = null;
                this.elem_colorPalette = null;
                this.elem_showColor = null;
                this.elem_showModeBtn = null;
                this.elem_inputWrap = null;
                this.pancelLeft = 0;
                this.pancelTop = 0;
                this.downX = 0;
                this.downY = 0;
                this.moveX = 0;
                this.moveY = 0;
                this.pointLeft = 0;
                this.pointTop = 0;
                this.pancel_width = 0;
                this.pancel_height = 0;
                this.current_mode = 'hex'; // input框当前的模式
                this.rgba = { r: 0, g: 0, b: 0, a: 1 };
                this.hsb = { h: 0, s: 100, b: 100 };
                this.util = null;
                this.util = {
                    css: function (elem, obj) {
                        for (var i in obj) {
                            elem.style[i] = obj[i];
                        }
                    },
                    hasClass: function (elem, classN) {
                        var className = elem.getAttribute("class");
                        return className.indexOf(classN) != -1;
                    }
                };
                // 默认类名
                this.eleClasses = ["SView-labelItem"];
                this.elem_wrap = null; // 最外层容器
                this.fixedBg = null; // 拾色器后面固定定位的透明div 用于点击隐藏拾色器
                this.elem_colorPancel = null; // 色彩面板
                this.elem_picker = null; // 拾色器色块按钮
                this.elem_barPicker1 = null; // 颜色条
                this.elem_hexInput = null; // 显示hex的表单
                this.elem_showColor = null; // 显示当前颜色
                this.elem_showModeBtn = null; // 切换输入框模式按钮
                this.elem_inputWrap = null; // 输入框外层容器
                this.pancelLeft = 0;
                this.pancelTop = 0;
                this.downX = 0;
                this.downY = 0;
                this.moveX = 0;
                this.moveY = 0;
                this.pointLeft = 0;
                this.pointTop = 0;
                this.current_mode = 'hex'; // input框当前的模式
                this.rgba = { r: 0, g: 0, b: 0, a: 1 };
                this.hsb = { h: 0, s: 100, b: 100 };
            }
            /**
             * @description  创建列表Item
             * @param attendeeList 列表信息
             */
            createLabel(colorInfo) {
                return new Promise((resolve, reject) => {
                    var _this = this;
                    var rgb = this.initColor.slice(4, -1).split(",");
                    this.rgba.r = Number(rgb[0]);
                    this.rgba.g = Number(rgb[1]);
                    this.rgba.b = Number(rgb[2]);
                    let tagName = Object.keys(colorInfo)[0];
                    colorInfo = colorInfo[tagName];
                    let divElement = document.createElement("div");
                    this.bindElem = divElement;
                    var body = document.getElementsByTagName("body")[0], div = document.createElement("div");
                    div.innerHTML = this.render();
                    divElement.appendChild(div);
                    this.elem_wrap = div;
                    this.fixedBg = div.children[0];
                    this.elem_colorPancel = div.getElementsByClassName("color-pancel")[0];
                    this.elem_picker = div.getElementsByClassName("pickerBtn")[0];
                    this.elem_colorPalette = div.getElementsByClassName("color-palette")[0];
                    this.elem_showColor = div.getElementsByClassName("colorpicker-showColor")[0];
                    this.elem_barPicker1 = div.getElementsByClassName("colorBar-color-picker")[0];
                    /*   this.elem_barPicker2 = div.getElementsByClassName("colorBar-opacity-picker")[0]; */
                    this.elem_hexInput = div.getElementsByClassName("colorpicker-hexInput")[0];
                    this.elem_showModeBtn = div.getElementsByClassName("colorpicker-showModeBtn")[0];
                    this.elem_inputWrap = div.getElementsByClassName("colorpicker-inputWrap")[0];
                    /*  this.elem_opacityPancel = this.elem_barPicker2.parentNode.parentNode.children[1]; */
                    // var rect = this.bindElem.getBoundingClientRect();
                    this.util.css(div, {
                        "position": "absolute",
                        "z-index": 2,
                        "left": "0px",
                        "top": "0px"
                    });
                    this.bindMove(this.elem_colorPancel, this.setPosition, true);
                    this.bindMove(this.elem_barPicker1.parentNode, this.setBar, false);
                    /*  this.bindMove(this.elem_barPicker2.parentNode,this.setBar,false); */
                    this.bindElem.addEventListener("click", function () {
                        _this.show();
                    }, false);
                    this.fixedBg.addEventListener("click", function (e) {
                        _this.hide();
                    }, false);
                    this.elem_showModeBtn.addEventListener("click", function () {
                        _this.switch_current_mode();
                    }, false);
                    this.elem_wrap.addEventListener("input", function (e) {
                        var target = e.target, value = target.value;
                        _this.setColorByInput(value);
                    }, false);
                    this.elem_colorPalette.addEventListener("click", function (e) {
                        if (e.target.tagName.toLocaleLowerCase() == "p") {
                            let colorStr = e.target.style.background;
                            let rgb = colorStr.slice(4, -1).split(",");
                            let rgba = {
                                r: parseInt(rgb[0]),
                                g: parseInt(rgb[1]),
                                b: parseInt(rgb[2])
                            };
                            switch (_this.current_mode) {
                                case "hex":
                                    _this.setColorByInput("#" + _this.rgbToHex(rgba));
                                    _this.getElem_hexInput().value = "#" + _this.rgbToHex(rgba);
                                    break;
                                case 'rgb':
                                    let inputs = _this.elem_wrap.getElementsByTagName("input");
                                    inputs[0].value = rgba.r;
                                    inputs[1].value = rgba.g;
                                    inputs[2].value = rgba.b;
                                    _this.setColorByInput(colorStr);
                                    _this.hsb = _this.rgbToHsb(rgba);
                                    break;
                            }
                        }
                    }, false);
                    if (colorInfo.color && colorInfo.color != '') {
                        this.color = colorInfo.color;
                        this.setColorByInput(this.color);
                    }
                    ;
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            getElem_hexInput() {
                this.elem_hexInput = this.eleLabel.getElementsByClassName("colorpicker-hexInput")[0];
                return this.elem_hexInput;
            }
            render() {
                var tpl = `<div style="position: inherit;z-index: 100;display: flex;">
					<div style='width:180px;padding:10px;background: #f9f9f9;display: flex;flex-flow: row wrap;align-content: space-around;justify-content: space-around;' class='color-palette'>
                    ${this.getPaletteColorsItem()}
                    </div>
					<div class="colorpicker-pancel" style="background: rgb(255, 255, 255);box-sizing: initial; width: 225px; font-family: Menlo;">
						<div style="width: 100%; padding-bottom: 55%; position: relative; border-radius: 2px 2px 0px 0px; overflow: hidden;">
							<div class="color-pancel" style="position: absolute; top: 0px; right: 0px; bottom: 0px; left: 0px; background: rgb(${this.rgba.r},${this.rgba.g},${this.rgba.b})">
								<style>
									.saturation-white {background: -webkit-linear-gradient(to right, #fff, rgba(255,255,255,0));background: linear-gradient(to right, #fff, rgba(255,255,255,0));}
									.saturation-black {background: -webkit-linear-gradient(to top, #000, rgba(0,0,0,0));background: linear-gradient(to top, #000, rgba(0,0,0,0));}
								</style>
								<div class="saturation-white" style="position: absolute; top: 0px; right: 0px; bottom: 0px; left: 0px;">
									<div class="saturation-black" style="position: absolute; top: 0px; right: 0px; bottom: 0px; left: 0px;">
									</div>
									<div class="pickerBtn" style="position: absolute; top: 0%; left: 100%; cursor: default;">
										<div style="width: 12px; height: 12px; border-radius: 6px; box-shadow: rgb(255, 255, 255) 0px 0px 0px 1px inset; transform: translate(-6px, -6px);">
										</div>
									</div>
								</div>
							</div>
						</div>
						<div style="padding: 0 16px 20px;">
							<div class="flexbox-fix" style="display: flex;align-items: center;height: 40px;">
								<div style="width: 32px;">
									<div style="width: 16px; height: 16px; border-radius: 8px; position: relative; overflow: hidden;">
										<div class="colorpicker-showColor" style="position: absolute; top: 0px; right: 0px; bottom: 0px; left: 0px; border-radius: 8px; box-shadow: rgba(0, 0, 0, 0.1) 0px 0px 0px 1px inset; background:rgb(${this.rgba.r},${this.rgba.g},${this.rgba.b}); z-index: 2;"></div>
										<div class="" style="position: absolute; top: 0px; right: 0px; bottom: 0px; left: 0px; background: url(&quot;data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAMUlEQVQ4T2NkYGAQYcAP3uCTZhw1gGGYhAGBZIA/nYDCgBDAm9BGDWAAJyRCgLaBCAAgXwixzAS0pgAAAABJRU5ErkJggg==&quot;) left center;"></div>
									</div>
								</div>
								<div style="-webkit-box-flex: 1; flex: 1 1 0%;"><div style="height: 10px; position: relative;">
									<div style="position: absolute; top: 0px;right: 0px; bottom: 0px; left: 0px;">
										<div class="hue-horizontal" style="padding: 0px 2px; position: relative; height: 100%;">
											<style>
												.hue-horizontal {background: linear-gradient(to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%);background: -webkit-linear-gradient(to right, #f00 0%, #ff0 17%, #0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%);}
												.hue-vertical {background: linear-gradient(to top, #f00 0%, #ff0 17%, #0f0 33%,#0ff 50%, #00f 67%, #f0f 83%, #f00 100%);background: -webkit-linear-gradient(to top, #f00 0%, #ff0 17%,#0f0 33%, #0ff 50%, #00f 67%, #f0f 83%, #f00 100%);}
											</style>
											<div  class="colorBar-color-picker" style="position: absolute; left: 0%;">
												<div style="width: 12px; height: 12px; border-radius: 6px; transform: translate(-6px, -1px); background-color: rgb(248, 248, 248); box-shadow: rgba(0, 0, 0, 0.37) 0px 1px 4px 0px;">
												</div>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
						<div class="flexbox-fix" style="display: flex;">
							<div class="flexbox-fix colorpicker-inputWrap" style="-webkit-box-flex: 1; flex: 1 1 0%; display: flex; margin-left: -6px;">
									${this.getInputTpl()}
							</div>
							<div class="colorpicker-showModeBtn" style="width: 32px; text-align: right; position: relative;">
								<div style="margin-right: -4px;  cursor: pointer; position: relative;">
									<svg viewBox="0 0 24 24" style="width: 24px; height: 24px; border: 1px solid transparent; border-radius: 5px;"><path fill="#333" d="M12,5.83L15.17,9L16.58,7.59L12,3L7.41,7.59L8.83,9L12,5.83Z"></path><path fill="#333" d="M12,18.17L8.83,15L7.42,16.41L12,21L16.59,16.41L15.17,15Z"></path></svg>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>`;
                return tpl;
            }
            getPaletteColorsItem() {
                let str = '';
                let palette = ["rgb(0, 0, 0)", "rgb(67, 67, 67)", "rgb(102, 102, 102)", "rgb(204, 204, 204)", "rgb(217, 217, 217)", "rgb(255, 255, 255)",
                    "rgb(152, 0, 0)", "rgb(255, 0, 0)", "rgb(255, 153, 0)", "rgb(255, 255, 0)", "rgb(0, 255, 0)", "rgb(0, 255, 255)",
                    "rgb(74, 134, 232)", "rgb(0, 0, 255)", "rgb(153, 0, 255)", "rgb(255, 0, 255)", "rgb(230, 184, 175)", "rgb(244, 204, 204)",
                    "rgb(252, 229, 205)", "rgb(255, 242, 204)", "rgb(217, 234, 211)", "rgb(208, 224, 227)", "rgb(201, 218, 248)", "rgb(207, 226, 243)",
                    "rgb(217, 210, 233)", "rgb(234, 209, 220)", "rgb(221, 126, 107)", "rgb(234, 153, 153)", "rgb(249, 203, 156)", "rgb(255, 229, 153)",
                    "rgb(182, 215, 168)", "rgb(162, 196, 201)", "rgb(164, 194, 244)", "rgb(159, 197, 232)", "rgb(180, 167, 214)"];
                palette.forEach(item => str += `<p style='width:20px;height:20px;background:${item};margin:0 5px;border: solid 1px #d0d0d0;'></p>`);
                return str;
            }
            getInputTpl() {
                var current_mode_html = "";
                switch (this.current_mode) {
                    case 'hex':
                        var hex = "#" + this.rgbToHex(this.HSBToRGB(this.hsb));
                        current_mode_html += `
							<div style="padding-left: 6px; width: 100%;">
								<div style="position: relative;">
									<input class="colorpicker-hexInput" value="${hex}" spellcheck="false" style="font-size: 11px; color: rgb(51, 51, 51); width: 100%; border-radius: 2px; border: none; box-shadow: rgb(218, 218, 218) 0px 0px 0px 1px inset; height: 21px; text-align: center;">
									<span style="text-transform: uppercase; font-size: 11px; line-height: 11px; color: rgb(150, 150, 150); text-align: center; display: block; margin-top: 12px;">hex</span>
								</div>
							</div>`;
                        break;
                    case 'rgb':
                        for (var i = 0; i < 3; i++) {
                            current_mode_html +=
                                `<div style="padding-left: 6px; width: 100%;">
								<div style="position: relative;">
									<input class="colorpicker-hexInput" value="${this.rgba['rgb'[i]]}" spellcheck="false" style="font-size: 11px; color: rgb(51, 51, 51); width: 100%; border-radius: 2px; border: none; box-shadow: rgb(218, 218, 218) 0px 0px 0px 1px inset; height: 21px; text-align: center;">
									<span style="text-transform: uppercase; font-size: 11px; line-height: 11px; color: rgb(150, 150, 150); text-align: center; display: block; margin-top: 12px;">${'rgb'[i]}</span>
								</div>
							</div>`;
                        }
                    default:
                }
                return current_mode_html;
            }
            setPosition(x, y) {
                var elem = this.bindElem;
                var top = elem.offsetTop;
                var left = elem.offsetLeft;
                while (elem.offsetParent) {
                    top += elem.offsetParent.offsetTop;
                    left += elem.offsetParent.offsetLeft;
                    elem = elem.offsetParent;
                }
                this.pancel_width = this.elem_colorPancel.offsetWidth;
                this.pancel_height = this.elem_colorPancel.offsetHeight;
                this.pancelLeft = left + this.elem_colorPalette.clientWidth;
                this.pancelTop = top;
                var LEFT = Number(x - this.pancelLeft), TOP = Number(y - this.pancelTop);
                this.pointLeft = Math.max(0, Math.min(LEFT, this.pancel_width));
                this.pointTop = Math.max(0, Math.min(TOP, this.pancel_height));
                this.util.css(this.elem_picker, {
                    left: this.pointLeft + "px",
                    top: this.pointTop + "px"
                });
                this.hsb.s = Number(100 * this.pointLeft / this.pancel_width);
                this.hsb.b = Number(100 * (this.pancel_height - this.pointTop) / this.pancel_height);
                this.setShowColor();
                this.setColorValue(this.rgba);
            }
            setBar(elem, x) {
                var elem_bar = elem.getElementsByTagName("div")[0], rect = elem.getBoundingClientRect(), elem_width = elem.offsetWidth, X = Math.max(0, Math.min(x - rect.x, elem_width));
                if (elem_bar === this.elem_barPicker1) {
                    this.util.css(elem_bar, {
                        left: X + "px"
                    });
                    this.hsb.h = Number(360 * X / elem_width);
                }
                else {
                    this.util.css(elem_bar, {
                        left: X + "px"
                    });
                    this.rgba.a = X / elem_width;
                }
                this.setPancelColor(this.hsb.h);
                this.setShowColor();
                this.setColorValue(this.rgba);
            }
            setPancelColor(h) {
                var rgb = this.HSBToRGB({ h: h, s: 100, b: 100 });
                this.util.css(this.elem_colorPancel, {
                    background: 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' + this.rgba.a + ')'
                });
            }
            setShowColor() {
                var rgb = this.HSBToRGB(this.hsb);
                this.rgba.r = rgb.r;
                this.rgba.g = rgb.g;
                this.rgba.b = rgb.b;
                this.util.css(this.elem_showColor, {
                    background: 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' + this.rgba.a + ')'
                });
            }
            setColorValue(rgb) {
                var hex = "#" + this.rgbToHex(rgb);
                this.elem_inputWrap.innerHTML = this.getInputTpl();
                this.color = hex;
                //this.Opt.change(this.bindElem, hex);
            }
            setValue(rgb) {
                let _this = this;
                switch (_this.current_mode) {
                    case "hex":
                        _this.setColorByInput("#" + _this.rgbToHex(rgb));
                        _this.getElem_hexInput().value = "#" + _this.rgbToHex(rgb);
                        break;
                    case 'rgb':
                        let inputs = _this.elem_wrap.getElementsByTagName("input");
                        inputs[0].value = rgb.r;
                        inputs[1].value = rgb.g;
                        inputs[2].value = rgb.b;
                        _this.setColorByInput("rgb(" + rgb.r + ", " + rgb.g + ", " + rgb.b + ")");
                        _this.hsb = _this.rgbToHsb(rgb);
                        break;
                }
                var hex = "#" + this.rgbToHex(rgb);
                this.color = hex;
            }
            getValue() {
                return this.color;
                //this.Opt.change(this.bindElem, hex);
            }
            setColorByInput(value) {
                var _this = this;
                switch (this.current_mode) {
                    case "hex":
                        value = value.slice(1);
                        if (value.length == 3) {
                            value = '#' + value[0] + value[0] + value[1] + value[1] + value[2] + value[2];
                            this.hsb = this.hexToHsb(value);
                        }
                        else if (value.length == 6) {
                            this.hsb = this.hexToHsb(value);
                        }
                        //_this.elem_hexInput.value = "#" + value;
                        break;
                    case 'rgb':
                        var inputs = this.elem_wrap.getElementsByTagName("input"), rgb = {
                            r: inputs[0].value ? Number(inputs[0].value) : 0,
                            g: inputs[1].value ? Number(inputs[1].value) : 0,
                            b: inputs[2].value ? Number(inputs[2].value) : 0
                        };
                        this.hsb = this.rgbToHsb(rgb);
                }
                this.changeViewByHsb();
            }
            changeViewByHsb() {
                this.pointLeft = Number(this.hsb.s * this.pancel_width / 100);
                this.pointTop = Number((100 - this.hsb.b) * this.pancel_height / 100);
                this.util.css(this.elem_picker, {
                    left: this.pointLeft + "px",
                    top: this.pointTop + "px"
                });
                this.setPancelColor(this.hsb.h);
                this.setShowColor();
                this.util.css(this.elem_barPicker1, {
                    left: this.hsb.h / 360 * (this.elem_barPicker1.parentNode.offsetWidth) + "px"
                });
                var hex = '#' + this.rgbToHex(this.HSBToRGB(this.hsb));
                this.color = hex;
                //this.Opt.change(this.bindElem, hex);
            }
            switch_current_mode() {
                this.current_mode = this.current_mode == 'hex' ? 'rgb' : 'hex';
                this.elem_inputWrap.innerHTML = this.getInputTpl();
            }
            bindMove(elem, fn, bool) {
                var _this = this;
                elem.addEventListener("mousedown", function (e) {
                    _this.downX = e.pageX;
                    _this.downY = e.pageY;
                    bool ? fn.call(_this, _this.downX, _this.downY) : fn.call(_this, elem, _this.downX, _this.downY);
                    document.addEventListener("mousemove", mousemove, false);
                    function mousemove(e) {
                        _this.moveX = e.pageX;
                        _this.moveY = e.pageY;
                        bool ? fn.call(_this, _this.moveX, _this.moveY) : fn.call(_this, elem, _this.moveX, _this.moveY);
                        e.preventDefault();
                    }
                    document.addEventListener("mouseup", mouseup, false);
                    function mouseup(e) {
                        document.removeEventListener("mousemove", mousemove, false);
                        document.removeEventListener("mouseup", mouseup, false);
                    }
                }, false);
            }
            show() {
                this.util.css(this.elem_wrap, {
                    "display": "block"
                });
            }
            hide() {
                this.util.css(this.elem_wrap, {
                    "display": "none"
                });
            }
            HSBToRGB(hsb) {
                var rgb = {
                    r: 0,
                    g: 0,
                    b: 0,
                };
                var h = Math.round(hsb.h);
                var s = Math.round(hsb.s * 255 / 100);
                var v = Math.round(hsb.b * 255 / 100);
                if (s == 0) {
                    rgb.r = rgb.g = rgb.b = v;
                }
                else {
                    var t1 = v;
                    var t2 = (255 - s) * v / 255;
                    var t3 = (t1 - t2) * (h % 60) / 60;
                    if (h == 360)
                        h = 0;
                    if (h < 60) {
                        rgb.r = t1;
                        rgb.b = t2;
                        rgb.g = t2 + t3;
                    }
                    else if (h < 120) {
                        rgb.g = t1;
                        rgb.b = t2;
                        rgb.r = t1 - t3;
                    }
                    else if (h < 180) {
                        rgb.g = t1;
                        rgb.r = t2;
                        rgb.b = t2 + t3;
                    }
                    else if (h < 240) {
                        rgb.b = t1;
                        rgb.r = t2;
                        rgb.g = t1 - t3;
                    }
                    else if (h < 300) {
                        rgb.b = t1;
                        rgb.g = t2;
                        rgb.r = t2 + t3;
                    }
                    else if (h < 360) {
                        rgb.r = t1;
                        rgb.g = t2;
                        rgb.b = t1 - t3;
                    }
                    else {
                        rgb.r = 0;
                        rgb.g = 0;
                        rgb.b = 0;
                    }
                }
                return { r: Math.round(rgb.r), g: Math.round(rgb.g), b: Math.round(rgb.b) };
            }
            rgbToHex(rgb) {
                var hex = [
                    rgb.r.toString(16),
                    rgb.g.toString(16),
                    rgb.b.toString(16)
                ];
                hex.map(function (str, i) {
                    if (str.length == 1) {
                        hex[i] = '0' + str;
                    }
                });
                return hex.join('');
            }
            hexToRgb(hex) {
                var hex = parseInt(((hex.indexOf('#') > -1) ? hex.substring(1) : hex), 16);
                return { r: hex >> 16, g: (hex & 0x00FF00) >> 8, b: (hex & 0x0000FF) };
            }
            hexToHsb(hex) {
                return this.rgbToHsb(this.hexToRgb(hex));
            }
            rgbToHsb(rgb) {
                var hsb = { h: 0, s: 0, b: 0 };
                var min = Math.min(rgb.r, rgb.g, rgb.b);
                var max = Math.max(rgb.r, rgb.g, rgb.b);
                var delta = max - min;
                hsb.b = max;
                hsb.s = max != 0 ? 255 * delta / max : 0;
                if (hsb.s != 0) {
                    if (rgb.r == max)
                        hsb.h = (rgb.g - rgb.b) / delta;
                    else if (rgb.g == max)
                        hsb.h = 2 + (rgb.b - rgb.r) / delta;
                    else
                        hsb.h = 4 + (rgb.r - rgb.g) / delta;
                }
                else
                    hsb.h = -1;
                hsb.h *= 60;
                if (hsb.h < 0)
                    hsb.h += 360;
                hsb.s *= 100 / 255;
                hsb.b *= 100 / 255;
                return hsb;
            }
        }
        Windows.Colorpicker = Colorpicker;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：版本列表Item
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Copyright extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.yearInfo = new Date().getFullYear();
                // 默认类名
                this.eleClasses = ["SView-footer"];
            }
            /**
             * @description  版权信息
             * @param copyrightInfo 列表信息
             */
            createLabel(copyrightInfo) {
                let tagName = Object.keys(copyrightInfo)[0];
                copyrightInfo = copyrightInfo[tagName];
                let divElement = document.createElement("div");
                let html = "<div class='footer'>";
                html += "<div class='footer-gradient'>";
                html += "<div class='copyright'>";
                html += "<span class='copyright-dir'><span class='nowrap'>Copyright &#169; <span class='SView-copyright-yearInfo'>" + this.yearInfo + "</span><a class ='underline' href = 'http://m.hoteamsoft.com' target= '_blank'>HOTEAMSOFT</a>,All rights reserved</span></span>";
                html += "</div>";
                html += "<div class='copyright'>";
                html += "<span id='all'><a class ='underline' href='https://www.hoteamsoft.com/about-us' target='_blank'> Contact Me </a></span>";
                html += "</div></div></div>";
                divElement.innerHTML = html;
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            setYearInfo(value) {
                this.yearInfo = Number(value);
                if (this.eleLabel.getElementsByClassName("SView-copyright-yearInfo").length > 0)
                    this.eleLabel.getElementsByClassName("SView-copyright-yearInfo")[0].innerHTML = value;
            }
        }
        Windows.Copyright = Copyright;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
/// <reference path="./Button.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：弹框
         *@author WZ
         *@date		2022-2-9
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Dialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.dialogTitle = "";
                this.dialogTitleFontSize = "";
                this.dialogTitleColor = "";
                this.dialogContent = "";
                this.dialogContentImg = "";
                this.dialogContentSubComponents = [];
                this.dialogContentFontSize = "";
                this.dialogContentColor = "";
                this.dialogCloseBtnOnClick = null;
                this.dialogCloseBtnIsShow = true; // 默认显示
                this.dialogBottomButtons = [];
                this.dialogBottomButtonsIsShow = true; // 默认显示
                this.isMove = false;
                this.eleClasses = [];
            }
            /**
             * @description 创建弹框
             * @param dialogInfo 对话框信息
             */
            createLabel(dialogInfo) {
                let tagName = Object.keys(dialogInfo)[0];
                dialogInfo = dialogInfo[tagName];
                // 遮罩层
                let eleBack = document.createElement("div");
                // 外层div
                let eleDiv = document.createElement("div");
                if (dialogInfo.content && dialogInfo.content != "") {
                    eleDiv.setAttribute("class", "SView-dialog SView-dialog-confirm");
                }
                else {
                    eleDiv.setAttribute("class", "SView-dialog");
                }
                if (dialogInfo.showBack && dialogInfo.showBack == true) {
                    this.eleOverLay = true;
                }
                else {
                    this.eleOverLay = false;
                }
                if (dialogInfo.isMove && dialogInfo.isMove == true) {
                    this.isMove = true;
                }
                else {
                    this.isMove = false;
                }
                if (dialogInfo.closeBtnOnClick && dialogInfo.closeBtnOnClick != "") {
                    this.dialogCloseBtnOnClick = dialogInfo.closeBtnOnClick;
                }
                let that = this;
                // 关闭图标
                let eleClose = document.createElement("div");
                eleClose.setAttribute("class", "SView-dialog-title-closeBtn SView-button-iconBtn");
                if (this.dialogCloseBtnOnClick) {
                    eleClose.setAttribute("onclick", this.dialogCloseBtnOnClick);
                }
                else {
                    eleClose.addEventListener("click", function () {
                        that.hide();
                    }, false);
                    eleClose.addEventListener("touchend", function () {
                        that.hide();
                    }, false);
                }
                if (dialogInfo.closeBtnIsShow == false) {
                    this.dialogCloseBtnIsShow = false;
                    eleClose.style.display = "none";
                }
                else {
                    // 默认显示弹窗关闭按钮
                    this.dialogCloseBtnIsShow = true;
                    eleClose.style.display = "";
                }
                let eleTitle;
                // 弹框标题
                if (dialogInfo.hideTitle && dialogInfo.hideTitle == true) {
                    eleTitle = "";
                    let eleImg = document.createElement("img");
                    eleImg.setAttribute('src', Windows.Label.rootPath + 'images/meeting/MeetingExist.png');
                    eleClose.appendChild(eleImg);
                    eleDiv.appendChild(eleClose);
                }
                else {
                    eleTitle = document.createElement("div");
                    eleTitle.setAttribute("class", "SView-dialog-title");
                    let eleSpan = document.createElement("span");
                    eleSpan.setAttribute("class", "SView-dialog-title-name");
                    this.dialogTitle = dialogInfo.title;
                    eleSpan.innerHTML = this.dialogTitle;
                    eleTitle.appendChild(eleSpan);
                    let eleImg = document.createElement("img");
                    eleImg.setAttribute('src', Windows.Label.rootPath + 'images/meeting/MeetingExist.png');
                    eleClose.appendChild(eleImg);
                    eleTitle.appendChild(eleClose);
                }
                // 内容部分
                let eleContent = document.createElement("div");
                // 增加内容部分类名
                if (dialogInfo.contentClasses && dialogInfo.contentClasses.length > 0) {
                    eleContent.setAttribute("class", dialogInfo.contentClasses.join(" "));
                }
                else {
                    eleContent.setAttribute("class", "SView-dialog-content");
                }
                // 增加内容部分ID
                if (dialogInfo.contentId && dialogInfo.contentId.length > 0) {
                    eleContent.setAttribute("id", dialogInfo.contentId);
                }
                // 弹窗内容为子控件/文字 
                if (dialogInfo.subComponents && dialogInfo.subComponents.length > 0) {
                    let subComponents = dialogInfo.subComponents;
                    for (var i = 0; i < subComponents.length; i++) {
                        this.dialogContentSubComponents.push(subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        eleContent.appendChild(newLabel);
                    }
                }
                else if (dialogInfo.content && dialogInfo.content != "") {
                    if (dialogInfo.contentImg && dialogInfo.contentImg != "") {
                        let eleImg = document.createElement("img");
                        this.dialogContentImg = dialogInfo.contentImg;
                        eleImg.setAttribute("src", Windows.Label.rootPath + this.dialogContentImg);
                        eleContent.appendChild(eleImg);
                    }
                    let eleText = document.createElement("h4");
                    this.dialogContent = dialogInfo.content;
                    eleText.innerHTML = this.dialogContent;
                    eleContent.appendChild(eleText);
                }
                else if (dialogInfo.contentImg && dialogInfo.contentImg != "") {
                    let eleImg = document.createElement("img");
                    this.dialogContentImg = dialogInfo.contentImg;
                    eleImg.setAttribute("src", Windows.Label.rootPath + this.dialogContentImg);
                    eleContent.appendChild(eleImg);
                }
                // 底部按钮
                let eleButton = null;
                if (dialogInfo.buttons && dialogInfo.buttons.length > 0) {
                    eleButton = document.createElement("div");
                    eleButton.setAttribute("class", "SView-dialog-button");
                    for (let i = 0; i < dialogInfo.buttons.length; i++) {
                        this.dialogBottomButtons.push(dialogInfo.buttons[i]);
                        let tagName = Object.keys(dialogInfo.buttons[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(dialogInfo.buttons[i][tagName].id, newElement);
                        newElement.create(dialogInfo.buttons[i]);
                        let labelElement = newElement.getLabel();
                        eleButton.appendChild(labelElement);
                    }
                    // 底部按钮显隐
                    if (dialogInfo.buttonsIsShow == false) {
                        this.dialogBottomButtonsIsShow = false;
                        eleButton.style.display = "none";
                        eleContent.style.border = "none";
                    }
                    else {
                        // 默认显示弹窗底部按钮
                        this.dialogBottomButtonsIsShow = true;
                        eleButton.style.display = "";
                        eleContent.style.border = "";
                    }
                }
                else {
                    eleContent.style.border = "none";
                }
                if (eleTitle != "") {
                    eleDiv.appendChild(eleTitle);
                }
                eleDiv.appendChild(eleContent);
                if (eleButton) {
                    eleDiv.appendChild(eleButton);
                }
                eleBack.appendChild(eleDiv);
                eleBack.oncontextmenu = function (e) {
                    e.stopPropagation();
                    e.preventDefault();
                    return false;
                };
                this.eleLabel = eleBack;
                if (this.isMove) {
                    this.bindDragEvent();
                }
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 添加弹窗内容子控件（位置从1开始）
            * @param subComponents 子控件
            * @param position 位置
            */
            addSubComponents(subComponents, position) {
                let contentsElement = this.eleLabel.getElementsByClassName("SView-dialog-content")[0];
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.dialogContentSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                        this.dialogContentSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        /*contentsElement[position - 1 + i].append(newLabel);*/
                    }
                    contentsElement.insertBefore(newLabel, contentsElement.children[position - 1 + i]);
                }
            }
            /**
            * @description 添加弹窗底部按钮（位置从1开始）
            * @param buttons 按钮
            * @param position 位置
            */
            addBottomButtons(buttons, position) {
                let btnElement = this.eleLabel.getElementsByClassName("SView-dialog-button")[0];
                let newLabel;
                for (let i = 0; i < buttons.length; i++) {
                    if (buttons[i].constructor.name != "Object") {
                        this.allSubComponents.set(buttons[i].eleId, buttons[i]);
                        newLabel = buttons[i].eleLabel;
                    }
                    else {
                        let newElement = new Windows.Button();
                        this.allSubComponents.set(buttons[i], newElement);
                        newElement.createLabel(buttons[i]);
                        newElement.create(buttons[i]);
                        newLabel = newElement.getLabel();
                    }
                    this.dialogBottomButtons.splice(position - 1 + i, 0, buttons[i]);
                    btnElement.insertBefore(newLabel, btnElement.children[position - 1 + i]);
                }
            }
            /**
            * @description 调整弹窗内容子控件位置（位置从1开始）
            * @param oldPosition 原来的位置
            * @param newPosition 新位置
            */
            changePosition(oldPosition, newPosition) {
                let contentsElement = this.eleLabel.getElementsByClassName("SView-dialog-content")[0];
                let changeElement = contentsElement.children[oldPosition - 1];
                let elementInfo = this.dialogContentSubComponents[oldPosition - 1];
                this.dialogContentSubComponents.splice(oldPosition - 1, 1);
                this.dialogContentSubComponents.splice(newPosition - 1, 0, elementInfo);
                contentsElement.removeChild(contentsElement.children[oldPosition - 1]);
                contentsElement.insertBefore(changeElement, contentsElement.children[newPosition - 1]);
            }
            /**
            * @description 调整弹窗按钮控件位置（位置从1开始）
            * @param oldPosition 原来的位置
            * @param newPosition 新位置
            */
            changeBtnsPosition(oldPosition, newPosition) {
                let btnElement = this.eleLabel.getElementsByClassName("SView-dialog-button")[0];
                let changeElement = btnElement.children[oldPosition - 1];
                let elementInfo = this.dialogBottomButtons[oldPosition - 1];
                this.dialogBottomButtons.splice(oldPosition - 1, 1);
                this.dialogBottomButtons.splice(newPosition - 1, 0, elementInfo);
                btnElement.removeChild(btnElement.children[oldPosition - 1]);
                btnElement.insertBefore(changeElement, btnElement.children[newPosition - 1]);
            }
            /**
            * @description 移除弹窗内容子控件（位置从1开始）
            * @param position 位置
            */
            removeSubComponents(position) {
                let contentsElement = this.eleLabel.getElementsByClassName("SView-dialog-content")[0].children;
                if (position && contentsElement[position - 1]) {
                    let deleteElement = contentsElement[position - 1];
                    this.dialogContentSubComponents.splice(position - 1, 1);
                    // 清空
                    this.allSubComponents.set(deleteElement.id, null);
                    // 删除
                    this.allSubComponents.delete(deleteElement.id);
                    // 移除标签
                    deleteElement.parentNode.removeChild(deleteElement);
                }
                else if (!position) {
                    this.dialogContentSubComponents = [];
                    for (let i = 0; i < contentsElement.length; i++) {
                        let deleteElement = contentsElement[i];
                        // 清空
                        this.allSubComponents.set(deleteElement.id, null);
                        // 删除
                        this.allSubComponents.delete(deleteElement.id);
                        // 移除标签
                        deleteElement.parentNode.removeChild(deleteElement);
                        i--;
                    }
                }
            }
            /**
            * @description 移除弹窗底部按钮（位置从1开始）
            * @param position 位置
            */
            removeBottomButtons(position) {
                let btnElement = this.eleLabel.getElementsByClassName("SView-dialog-button")[0].children;
                let deleteElement = btnElement[position - 1];
                this.dialogBottomButtons.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
            * @description 设置遮罩层显隐
            * @param isShow 是否显隐
            */
            setOverLayIsShow(isShow) {
                this.eleOverLay = isShow;
                if (this.eleOverLay == true) {
                    this.addClass("SView-dialog-background");
                }
                else {
                    this.removeClass("SView-dialog-background");
                }
            }
            /**
            * @description 设置弹窗标题
            * @param title 标题
            */
            setTitle(title) {
                this.dialogTitle = title;
                let titleElement = this.eleLabel.getElementsByClassName("SView-dialog-title-name")[0];
                titleElement.innerHTML = this.dialogTitle;
            }
            /**
            * @description 设置弹窗关闭标签点击事件
            * @param onClick 点击
            */
            setCloseBtnOnClick(onClick) {
                this.dialogCloseBtnOnClick = onClick;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-dialog-title-closeBtn")[0];
                if (typeof (onclick) == "string") {
                    closeBtn.setAttribute("onclick", this.dialogCloseBtnOnClick);
                }
                else {
                    closeBtn.addEventListener("touchend", function () {
                        onClick();
                    }, false);
                    closeBtn.addEventListener("click", function () {
                        onClick();
                    }, false);
                    //(closeBtn as HTMLElement).onclick = function () {
                    //    onClick();
                    //};
                }
            }
            /**
            * @description 设置弹窗内容文字
            * @param content 内容
            */
            setContent(content) {
                this.dialogContent = content;
                let contentElement = this.eleLabel.getElementsByTagName("h4")[0];
                contentElement.innerHTML = this.dialogContent;
            }
            /**
           * @description 设置弹窗内容图片
           * @param content 内容
           */
            setContentImg(imgSrc) {
                this.dialogContentImg = imgSrc;
                let contentElement = this.eleLabel.getElementsByTagName("img")[0];
                contentElement.setAttribute("src", this.dialogContentImg);
            }
            /**
            * @description 设置关闭按钮显隐
            * @param isShow 是否显隐
            */
            setCloseBtnIsShow(isShow) {
                this.dialogCloseBtnIsShow = isShow;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-dialog-title-closeBtn")[0];
                if (this.dialogCloseBtnIsShow == false) {
                    closeBtn.style.display = "none";
                }
                else {
                    closeBtn.style.display = "";
                }
            }
            /**
            * @description 设置底部按钮栏显隐
            * @param isShow 是否显隐
            */
            setBottomBtnIsShow(isShow) {
                this.dialogBottomButtonsIsShow = isShow;
                let bottomBtns = this.eleLabel.getElementsByClassName("SView-dialog-button")[0];
                let content = this.eleLabel.getElementsByClassName("SView-dialog-content")[0];
                if (this.dialogBottomButtonsIsShow == false) {
                    bottomBtns.style.display = "none";
                    content.style.border = "none";
                }
                else {
                    bottomBtns.style.display = "";
                    content.style.border = "";
                }
            }
            /**
            * @description 获取弹窗标题
            */
            getTitle() {
                return this.dialogTitle;
            }
            /**
            * @description 获取弹窗内容文字
            */
            getContentText() {
                return this.dialogContent;
            }
            /**
            * @description 获取弹窗内容子控件
            */
            getContentSubComponents() {
                return this.dialogContentSubComponents;
            }
            /**
            * @description 获取弹窗按钮子控件
            */
            getBottomButtons() {
                return this.dialogBottomButtons;
            }
            /**
             * @description 获取所有子控件集合
             * @returns 所有子控件集合
             * */
            getAllSubComponents() {
                return this.allSubComponents;
            }
            /**
             * @description 获取某个子控件
             * @param id 子控件的id
             */
            getSubComponent(id) {
                return this.allSubComponents.get(id);
            }
            /**
            * 设置弹窗标题字体大小
            * @param value 字体大小
            */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.dialogTitleFontSize = value + "px";
                }
                else {
                    this.dialogTitleFontSize = value;
                }
                let titleElement = this.eleLabel.getElementsByClassName("SView-dialog-title-name")[0];
                titleElement.style.fontSize = this.dialogTitleFontSize;
            }
            /**
            * @description 设置弹窗标题字体颜色
            * @param color 颜色
            */
            setTitleColor(color) {
                this.dialogTitleColor = color;
                let titleElement = this.eleLabel.getElementsByClassName("SView-dialog-title-name")[0];
                titleElement.style.color = this.dialogTitleColor;
            }
            /**
            * @description 设置弹窗内容文字字体大小
            * @param value 字体大小
            */
            setContentFontSize(value) {
                if (typeof (value) == "number") {
                    this.dialogContentFontSize = value + "px";
                }
                else {
                    this.dialogContentFontSize = value;
                }
                let contentElement = this.eleLabel.getElementsByTagName("h4")[0];
                contentElement.style.fontSize = this.dialogContentFontSize;
            }
            /**
            * @description 设置弹窗内容文字颜色
            * @param color 颜色
            */
            setContentColor(color) {
                this.dialogContentColor = color;
                let contentElement = this.eleLabel.getElementsByTagName("h4")[0];
                contentElement.style.color = this.dialogContentColor;
            }
            /**
            * @description 设置弹窗拖动
            * @param 弹窗id id
            */
            bindDragEvent() {
                let that = this;
                var setObj;
                var fixedX = 0;
                var fixedY = 0;
                if (this.eleLabel.querySelector(".SView-dialog-title")) {
                    setObj = this.eleLabel.querySelector(".SView-dialog-title");
                }
                else {
                    setObj = this.eleLabel.querySelector(".SView-transverse-tabs");
                }
                var obj = this.eleLabel.querySelector(".SView-dialog");
                let downFun = function (event) {
                    event.preventDefault();
                    setObj.setCapture && setObj.setCapture(); // IE8 及以下 强制捕获下一次单击事件
                    fixedX = event.pageX - obj.offsetLeft;
                    fixedY = event.pageY - obj.offsetTop;
                    that.dragDiv(obj, event, fixedX, fixedY);
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
                    that.dragDiv(obj, event, fixedX, fixedY);
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
            dragDiv(obj, e, fixedX, fixedY) {
                var _move = true;
                document.addEventListener('mousemove', function (event) {
                    if (_move) {
                        var w = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
                        var h = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
                        var x = event.clientX ? event.clientX : event.pageX;
                        var y = event.clientY ? event.clientY : event.pageY;
                        let left = x - fixedX;
                        let top = y - fixedY;
                        if (left >= (w - obj.offsetWidth)) {
                            left = w - obj.offsetWidth - 2;
                        }
                        if (top >= (h - obj.offsetHeight)) {
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
                }, false);
                document.addEventListener('mouseup', function () {
                    _move = false;
                }, false);
                document.addEventListener('touchmove', function (event) {
                    if (_move) {
                        var touches = event.touches;
                        var w = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
                        var h = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
                        var x = touches[0].clientX ? touches[0].clientX : touches[0].pageX;
                        var y = touches[0].clientY ? touches[0].clientY : touches[0].pageY;
                        let left = x - fixedX;
                        let top = y - fixedY;
                        if (left >= (w - obj.offsetWidth)) {
                            left = w - obj.offsetWidth - 2;
                        }
                        if (top >= (h - obj.offsetHeight)) {
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
                }, false);
                document.addEventListener('touchend', function () {
                    _move = false;
                }, false);
            }
        }
        Windows.Dialog = Dialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：表单
         *@author	ZYC
         *@date		2022-2-10
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Form extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.formSubComponents = [];
                // 默认类名
                this.eleClasses = ["SView-form"];
            }
            /**
             * @description 创建表单
             * @param formInfo 表单信息
             */
            createLabel(formInfo) {
                let tagName = Object.keys(formInfo)[0];
                formInfo = formInfo[tagName];
                let formElement = document.createElement("form");
                // 阻止回车自动提交表单
                formElement.setAttribute("onkeydown", "if(event.keyCode==13){return false;}");
                let subComponents = formInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    this.formSubComponents.push(subComponents[i]);
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    //let newElement = createSViewObject(UI, tagName);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    formElement.appendChild(newLabel);
                }
                this.eleLabel = formElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加表单子控件
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                //console.log(components);
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.formSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                        this.formSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    //components[position - 1 + i].append(newLabel);
                    this.eleLabel.insertBefore(newLabel, this.eleLabel.children[position - 1 + i]);
                }
            }
            /**
             * @description 调整表单子控件
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let changeElement = this.eleLabel.children[oldPosition - 1];
                let elementInfo = this.formSubComponents[oldPosition - 1];
                this.formSubComponents.splice(oldPosition - 1, 1);
                this.formSubComponents.splice(newPosition - 1, 0, elementInfo);
                this.eleLabel.removeChild(this.eleLabel.children[oldPosition - 1]);
                this.eleLabel.insertBefore(changeElement, this.eleLabel.children[newPosition - 1]);
            }
            /**
             * @description 移除表单子控件
             * @param position 位置
             */
            removeSubComponents(position) {
                let deleteElement = this.eleLabel.children[position - 1];
                this.formSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 获取表单子控件列表
             */
            getSubComponents() {
                return this.formSubComponents;
            }
        }
        Windows.Form = Form;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
// 基础输入框
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：输入框
         *@author	ZYC
         *@date		2022-2-09
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Input extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.inputValue = "";
                this.inputFontSize = "";
                this.inputColor = "";
                this.inputPlaceHolder = "";
                this.checkType = "length";
                this.readOnly = "false";
                this.disabled = false;
                this.inputType = "text";
                // 默认类名
                this.eleClasses = ["SView-input"];
            }
            /**
             * @description 创建输入框标签
             * @param inputInfo 输入信息
             */
            createLabel(inputInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(inputInfo)[0];
                    inputInfo = inputInfo[tagName];
                    let inputElement = document.createElement("input");
                    if (inputInfo.inputType && inputInfo.inputType != "") {
                        this.inputType = inputInfo.inputType;
                    }
                    ;
                    inputElement.setAttribute("type", this.inputType);
                    if (this.inputType == "file" && inputInfo.accept && inputInfo.accept != "") {
                        inputElement.setAttribute("accept", inputInfo.accept);
                    }
                    if (inputInfo.value && inputInfo.value != "") {
                        this.inputValue = inputInfo.value;
                        inputElement.setAttribute("value", this.inputValue);
                    }
                    ;
                    if (inputInfo.placeHolder && inputInfo.placeHolder != "") {
                        this.inputPlaceHolder = inputInfo.placeHolder;
                        inputElement.setAttribute("placeholder", this.inputPlaceHolder);
                    }
                    if (inputInfo.disabled && (typeof inputInfo.disabled) == "boolean") {
                        this.disabled = true;
                        inputElement.setAttribute("disabled", "true");
                    }
                    if (inputInfo.readOnly && inputInfo.readOnly == true) {
                        this.readOnly = "true";
                        inputElement.setAttribute("readonly", this.readOnly);
                    }
                    // onInput事件
                    if (inputInfo.onInput && inputInfo.onInput != "") {
                        this.inputOnInput = inputInfo.onInput;
                        inputElement.setAttribute("oninput", this.inputOnInput);
                    }
                    // 最大长度
                    if (inputInfo.maxLength && inputInfo.maxLength != "") {
                        this.maxLength = inputInfo.maxLength;
                        inputElement.setAttribute("maxlength", this.maxLength);
                    }
                    this.eleLabel = inputElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置输入框内容
             * @param value 输入内容
             */
            setValue(value) {
                this.inputValue = value;
                this.eleLabel.value = this.inputValue;
            }
            /**
             * @description 设置禁用
             */
            isDisabled(value) {
                this.disabled = value;
                this.eleLabel.setAttribute("disabled", this.disabled.toString());
            }
            /**
             * @description 设置输入框占位字符
             * @param placeHolder 占位符
             */
            setPlaceHolder(placeHolder) {
                this.inputPlaceHolder = placeHolder;
                this.eleLabel.setAttribute("placeholder", this.inputPlaceHolder);
            }
            /**
             * @description 设置输入框onInput事件
             * @param onInput
             */
            setOnInput(onInput) {
                this.inputOnInput = onInput;
                let that = this;
                if (onInput instanceof Function) {
                    this.eleLabel.oninput = function () {
                        that.inputOnInput();
                    };
                }
                else {
                    this.eleLabel.setAttribute("oninput", this.inputOnInput);
                }
            }
            /**
             * @description 设置输入框校验类型
             * @param onInput
             */
            setCheckType(checkType) {
                this.checkType = checkType;
            }
            /**
             * @description 获取输入框校验类型
             * @param onInput
             */
            getCheckType() {
                return this.checkType;
            }
            /**
             * @description 获取输入框内容
             */
            getValue() {
                return this.eleLabel.value;
            }
            /**
             * @description 获取输入框占位字符
             */
            getPlaceHolder() {
                return this.inputPlaceHolder;
            }
            /**
             * @description 获取输入框onInput事件
             */
            getOnInput() {
                return this.inputOnInput;
            }
            /**
             * @description 设置输入框字体大小
             * @param value 字体大小
             */
            setFontSize(value) {
                if (typeof (value) == "number") {
                    this.inputFontSize = value + "px";
                }
                else {
                    this.inputFontSize = value;
                }
                this.eleLabel.style.fontSize = this.inputFontSize;
            }
            /**
             * @description 设置输入框字体颜色
             * @param color颜色
             */
            setColor(color) {
                this.inputColor = color;
                this.eleLabel.style.color = this.inputColor;
            }
            /**
             * 设置输入类型
             * @param inputType
             */
            setInputType(inputType) {
                if (inputType && inputType != "") {
                    this.inputType = inputType;
                }
                ;
                this.eleLabel.setAttribute("type", this.inputType);
            }
            /**
             * 获取输入类型
             * */
            getInputType() {
                return this.inputType;
            }
            /**
             * 获取输入类型
             * */
            setMaxLength(maxLength) {
                if (maxLength) {
                    this.maxLength = maxLength;
                }
                ;
                this.eleLabel.setAttribute("maxLength", this.maxLength);
            }
            /**
             * 获取输入类型
             * */
            getMaxLength() {
                return this.maxLength;
            }
        }
        Windows.Input = Input;
        // 带标题输入框
        /**
         * @description 带标题输入框
         *
         * */
        class WithTitleInput extends Windows.Label {
            /**
             * @description 构造函数
             *
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.inputTitle = "";
                this.inputTitleColor = "";
                this.inputTitleFontSize = "";
                // 默认类名
                this.eleClasses = ["SView-input-withTitle"];
            }
            /**
             * @description 创建带标题输入框标签
             * @param inputInfo 输入信息
             */
            createLabel(inputInfo) {
                let tagName = Object.keys(inputInfo)[0];
                inputInfo = inputInfo[tagName];
                let divElement = document.createElement("div");
                // 标题部分
                let spanElement = document.createElement("span");
                this.inputTitle = inputInfo.title;
                spanElement.innerText = this.inputTitle;
                divElement.appendChild(spanElement);
                // 子控件部分（输入框）
                let subComponents = inputInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    divElement.appendChild(newLabel);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
             * @description 设置标题
             * @param title 标题
             */
            setTitle(title) {
                this.inputTitle = title;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.innerText = this.inputTitle;
            }
            /**
             * @description 获取标题
             */
            getTitle() {
                return this.inputTitle;
            }
            /**
             * @description 设置标题字体大小
             * @param value 字体大小
             */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.inputTitleFontSize = value + "px";
                }
                else {
                    this.inputTitleFontSize = value;
                }
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.fontSize = this.inputTitleFontSize;
            }
            /**
             * @description 设置标题字体颜色
             * @param color 颜色
             */
            setTitleColor(color) {
                this.inputTitleColor = color;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.color = this.inputTitleColor;
            }
        }
        Windows.WithTitleInput = WithTitleInput;
        // 带警告输入框
        /**
         * @description 带警告输入框
         * */
        class WithWarningInput extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.inputWarning = "";
                this.inputWarningFontSize = "";
                this.inputWarningColor = "";
                this.inputWarningIsShow = false; // 警告默认不显示
                // 默认类名
                this.eleClasses = ["SView-input-warning"];
            }
            /**
             * @description 创建带警告输入框标签
             * @param inputInfo 输入信息
             */
            createLabel(inputInfo) {
                let tagName = Object.keys(inputInfo)[0];
                inputInfo = inputInfo[tagName];
                let divElement = document.createElement("div");
                // 子控件部分（输入框）
                let subComponents = inputInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    divElement.appendChild(newLabel);
                }
                // 警告部分
                let pElement = document.createElement("p");
                this.inputWarning = inputInfo.warning;
                pElement.innerText = this.inputWarning;
                divElement.appendChild(pElement);
                //if (inputElement.value.length == 0) {
                //    pElement.style.display = ""
                //} else {
                //    pElement.style.display = "none";
                //}
                // 设置警告显隐
                if (inputInfo.warningIsShow == true) {
                    this.inputWarningIsShow = true;
                    pElement.style.display = "";
                }
                else {
                    this.inputWarningIsShow = false;
                    pElement.style.display = "none";
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
             * @description 设置警告内容显隐
             * @param isShow 显隐
             */
            setWarningIsShow(isShow) {
                this.inputWarningIsShow = isShow;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                if (this.inputWarningIsShow == true) {
                    pElement.style.display = "";
                }
                else {
                    pElement.style.display = "none";
                }
            }
            getWarningIsShow() {
                return this.inputWarningIsShow;
            }
            /**
             * @description 设置警告内容
             * @param warning 警告内容
             */
            setWarning(warning) {
                this.inputWarning = warning;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.innerText = this.inputWarning;
            }
            /**
             * @description 获取警告内容
             */
            getWarning() {
                return this.inputWarning;
            }
            /**
             * @description 设置警告字体大小
             * @param value 字体大小
             */
            setWarningFontSize(value) {
                if (typeof (value) == "number") {
                    this.inputWarningFontSize = value + "px";
                }
                else {
                    this.inputWarningFontSize = value;
                }
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.fontSize = this.inputWarningFontSize;
            }
            /**
             * @description 设置警告字体颜色
             * @param color 颜色
             */
            setWarningColor(color) {
                this.inputWarningColor = color;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.color = this.inputWarningColor;
            }
        }
        Windows.WithWarningInput = WithWarningInput;
        // 带标题及警告输入框
        /**
         * @description 带标题及警告输入框
         *
         * */
        class WithBothInput extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.inputTitle = "";
                this.inputTitleFontSize = "";
                this.inputTitleColor = "";
                this.inputWarningIsShow = false; // 默认警告不显示
                this.inputWarning = "";
                this.inputWarningFontSize = "";
                this.inputWarningColor = "";
                // 默认类名
                this.eleClasses = ["SView-input-withBoth"];
            }
            /**
             * @description 创建带标题输入框标签
             * @param inputInfo 输入信息
             */
            createLabel(inputInfo) {
                let tagName = Object.keys(inputInfo)[0];
                inputInfo = inputInfo[tagName];
                let divElement = document.createElement("div");
                // 标题部分
                let spanElement = document.createElement("span");
                this.inputTitle = inputInfo.title;
                spanElement.innerText = this.inputTitle;
                divElement.appendChild(spanElement);
                // 子控件部分（输入框）
                let subComponents = inputInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    divElement.appendChild(newLabel);
                }
                // 警告部分
                let pElement = document.createElement("p");
                this.inputWarning = inputInfo.warning;
                pElement.innerText = this.inputWarning;
                divElement.appendChild(pElement);
                //if (inputElement.value.length == 0) {
                //    pElement.style.display = ""
                //} else {
                //    pElement.style.display = "none";
                //}
                // 设置警告显隐
                if (inputInfo.warningIsShow == true) {
                    this.inputWarningIsShow = true;
                    pElement.style.display = "";
                }
                else {
                    this.inputWarningIsShow = false;
                    pElement.style.display = "none";
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
             * @description 设置警告内容显隐
             * @param isShow 显隐
             */
            setWarningIsShow(isShow) {
                this.inputWarningIsShow = isShow;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                if (this.inputWarningIsShow == true) {
                    pElement.style.display = "";
                }
                else {
                    pElement.style.display = "none";
                }
            }
            /**
             * @description 设置标题
             * @param title 标题
             */
            setTitle(title) {
                this.inputTitle = title;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.innerText = this.inputTitle;
            }
            /**
             * @description 设置警告内容
             * @param warning 警告内容
             */
            setWarning(warning) {
                this.inputWarning = warning;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.innerText = this.inputWarning;
            }
            /**
             * @description 获取标题
             */
            getTitle() {
                return this.inputTitle;
            }
            /**
             * @description 获取警告内容
             */
            getWarning() {
                return this.inputWarning;
            }
            /**
             * @description 设置标题字体大小
             * @param value 字体大小
             */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.inputTitleFontSize = value + "px";
                }
                else {
                    this.inputTitleFontSize = value;
                }
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.fontSize = this.inputTitleFontSize;
            }
            /**
             * @description 设置标题字体颜色
             * @param color 颜色
             */
            setTitleColor(color) {
                this.inputTitleColor = color;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.color = this.inputTitleColor;
            }
            /**
             * @description 设置警告字体大小
             * @param value 字体大小
             */
            setWarningFontSize(value) {
                if (typeof (value) == "number") {
                    this.inputWarningFontSize = value + "px";
                }
                else {
                    this.inputWarningFontSize = value;
                }
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.fontSize = this.inputWarningFontSize;
            }
            /**
             * @description 设置警告字体颜色
             * @param color 颜色
             */
            setWarningColor(color) {
                this.inputWarningColor = color;
                let pElement = this.eleLabel.getElementsByTagName("p")[0];
                pElement.style.color = this.inputWarningColor;
            }
        }
        Windows.WithBothInput = WithBothInput;
        // 带按钮输入框
        /**
         * @description 带按钮输入框
         * */
        class WithBtnInput extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                // 默认类名
                this.eleClasses = ["SView-input-withBtn"];
            }
            /**
             * @description 增加标签
             * @param inputInfo 输入信息
             */
            createLabel(inputInfo) {
                let tagName = Object.keys(inputInfo)[0];
                inputInfo = inputInfo[tagName];
                let divElement = document.createElement("div");
                // 子控件部分（输入框+按钮）
                let subComponents = inputInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    divElement.appendChild(newLabel);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
        }
        Windows.WithBtnInput = WithBtnInput;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：列表Item
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Item extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.itemSubComponents = [];
                this.itemType = "labelItem";
                this.labelText = "";
                this.itemValue = "";
                // 默认类名
                this.eleClasses = ["SView-labelItem"];
            }
            /**
             * @description  创建列表Item
             * @param attendeeList 列表信息
             */
            createLabel(listItem) {
                let tagName = Object.keys(listItem)[0];
                listItem = listItem[tagName];
                let divElement = document.createElement("div");
                if (listItem.memuserid) {
                    divElement.setAttribute("data-userId", listItem.memuserid);
                }
                if (listItem.value) {
                    this.itemValue = listItem.value;
                    divElement.setAttribute("data-value", this.itemValue);
                }
                if (listItem.bigImgSrc) {
                    divElement.setAttribute("data-bigImgSrc", listItem.bigImgSrc);
                }
                this.itemType = listItem.itemType && listItem.itemType !== "" ? listItem.itemType : this.itemType;
                this.eleClasses = ["SView-" + this.itemType];
                // 创建按钮内所含标签
                if (this.itemType === "imgItem") {
                    let childElement = this.createImgItem(listItem);
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                else if (this.itemType === "labelItem") {
                    let childElement = this.createLabelItem(listItem);
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                else if (this.itemType === "viewItem") {
                    let childElement = this.createViewItem(listItem);
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                else if (this.itemType === "leftChatItem") {
                    let childElement = this.createChatItem(listItem);
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                else if (this.itemType === "rightChatItem") {
                    let childElement = this.createChatItem(listItem);
                    for (let i = 0; i < childElement.length; i++) {
                        divElement.appendChild(childElement[i]);
                    }
                }
                this.eleLabel = divElement;
                if (listItem.flexDirection) {
                    this.setFlexDirection(listItem.flexDirection);
                }
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createImgItem(listItem) {
                let childElement = [];
                let imgElement = document.createElement("img");
                let detailElement = document.createElement("div");
                let nameElement = document.createElement("p");
                let timeElement = document.createElement("p");
                detailElement.appendChild(nameElement);
                detailElement.appendChild(timeElement);
                if (listItem.imgsrc !== "") {
                    imgElement.setAttribute("src", listItem.imgsrc);
                }
                if (listItem.name !== "") {
                    nameElement.innerText = listItem.name;
                }
                if (listItem.time !== "") {
                    timeElement.innerText = listItem.time;
                }
                childElement.push(imgElement);
                childElement.push(detailElement);
                return childElement;
            }
            createLabelItem(listItem) {
                let childElement = [];
                if (listItem.text && listItem.text !== "") {
                    this.labelText = listItem.text;
                    let eleLabel = document.createElement("label");
                    eleLabel.setAttribute("title", listItem.text);
                    eleLabel.setAttribute("class", "SView-labelItem-label");
                    eleLabel.innerHTML = listItem.text;
                    childElement.push(eleLabel);
                }
                let subComponents = listItem.subComponents;
                if (subComponents && subComponents.length > 0) {
                    let eleSubComponents = document.createElement("span");
                    for (let i = 0; i < subComponents.length; i++) {
                        let eleSpan = document.createElement("span");
                        eleSubComponents.appendChild(eleSpan);
                        // 增加子控件
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        this.itemSubComponents.push(newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        eleSpan.appendChild(newLabel);
                    }
                    childElement.push(eleSubComponents);
                }
                return childElement;
            }
            createChatItem(listItem) {
                let childElement = [];
                let imgElement = document.createElement("img");
                let detailElement = document.createElement("div");
                let spanElement = document.createElement("span");
                let timeElement = document.createElement("span");
                timeElement.setAttribute("class", "SView-time");
                detailElement.setAttribute("class", "SView-chat-bubble");
                if (listItem.imgsrc && listItem.imgsrc !== "") {
                    imgElement.setAttribute("src", Windows.Label.rootPath + listItem.imgsrc);
                }
                if (listItem.text && listItem.text !== "") {
                    detailElement.innerText = listItem.text;
                }
                if (listItem.name && listItem.name !== "") {
                    spanElement.innerText = listItem.name;
                }
                if (listItem.time && listItem.time !== "") {
                    timeElement.innerText = listItem.time;
                }
                spanElement.appendChild(timeElement);
                childElement.push(detailElement);
                childElement.push(spanElement);
                childElement.push(imgElement);
                return childElement;
            }
            createViewItem(listItem) {
                let childElement = [];
                return childElement;
            }
            getLabelText() {
                return this.labelText;
            }
            getValue() {
                return this.itemValue;
            }
            setLabelText(text) {
                this.labelText = text;
                if (this.eleLabel.getElementsByClassName("SView-labelItem-label").length > 0)
                    this.eleLabel.getElementsByClassName("SView-labelItem-label")[0].innerHTML = this.labelText;
            }
            /**
             * @description 隐藏标签
             * */
            hide() {
                this.eleIsShow = false;
                this.addClass("SView-hide");
                this.removeClass("SView-show");
                if (this.eleLabel) {
                    this.eleLabel.parentElement.classList.add("SView-hide");
                    this.eleLabel.parentElement.classList.remove("SView-show");
                }
                //this.eleLabel.style.display = "none";
            }
            /**
             * @description 显示标签
             * */
            show() {
                this.eleIsShow = true;
                this.addClass("SView-show");
                this.removeClass("SView-hide");
                //this.eleLabel.style.display = ""
                if (this.eleLabel) {
                    this.eleLabel.parentElement.classList.add("SView-show");
                    this.eleLabel.parentElement.classList.remove("SView-hide");
                }
            }
            getSubComponents() {
                return this.allSubComponents;
            }
        }
        Windows.Item = Item;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：许可页面
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class License extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.allSubComponents = new Map();
                // 默认类名
                this.eleClasses = ["SView-License"];
            }
            /**
             * @description  创建列表Item
             * @param licenseInfo 列表信息
             */
            createLabel(licenseInfo) {
                let tagName = Object.keys(licenseInfo)[0];
                licenseInfo = licenseInfo[tagName];
                let divElement = document.createElement("div");
                let childElement = this.createItem(licenseInfo);
                for (let i = 0; i < childElement.length; i++) {
                    divElement.appendChild(childElement[i]);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createItem(licenseInfo) {
                let childElement = [];
                let bodyheader = document.createElement("div");
                bodyheader.setAttribute("class", "SView-license-bodyheader");
                let headername = document.createElement("div");
                headername.setAttribute("class", "SView-license-headername");
                headername.innerHTML = licenseInfo.productName;
                let expiretime = document.createElement("div");
                expiretime.setAttribute("class", "SView-license-expiretime");
                expiretime.innerHTML = licenseInfo.time;
                let usermessages = document.createElement("div");
                usermessages.setAttribute("class", "SView-license-usermessages");
                usermessages.innerHTML = licenseInfo.customer;
                let useremail = document.createElement("div");
                useremail.setAttribute("class", "SView-license-useremail");
                useremail.innerHTML = licenseInfo.customerEmail;
                let licenseid = document.createElement("div");
                licenseid.setAttribute("class", "SView-license-licenseid");
                licenseid.innerHTML = licenseInfo.ID;
                let licenseCreateDate = document.createElement("div");
                licenseCreateDate.setAttribute("class", "SView-license-licenseCreateDate");
                licenseCreateDate.innerHTML = licenseInfo.createDateTitle;
                let licenseCreator = document.createElement("div");
                licenseCreator.setAttribute("class", "SView-license-licenseCreator");
                licenseCreator.innerHTML = licenseInfo.creatorTitle;
                let licenseCreateDateSpan = document.createElement("span");
                licenseCreateDateSpan.setAttribute("class", "SView-license-licenseCreateDateSpan");
                licenseCreateDateSpan.innerHTML = licenseInfo.createDate;
                let licenseCreatorSpan = document.createElement("span");
                licenseCreatorSpan.setAttribute("class", "SView-license-licenseCreatorSpan");
                licenseCreatorSpan.innerHTML = licenseInfo.creator;
                licenseCreator.appendChild(licenseCreatorSpan);
                licenseCreateDate.appendChild(licenseCreateDateSpan);
                bodyheader.appendChild(headername);
                bodyheader.appendChild(expiretime);
                bodyheader.appendChild(usermessages);
                bodyheader.appendChild(useremail);
                bodyheader.appendChild(licenseid);
                let copyright = {
                    Copyright: {
                        id: "SView-copyright"
                    }
                };
                let tagName = Object.keys(copyright)[0];
                let newElement = new SView.Windows[tagName]();
                this.allSubComponents.set("SView-copyright", newElement);
                newElement.create(copyright);
                let newLabel = newElement.eleLabel;
                //let html = "<div class='footer'>";
                //html += "<div class='footer-gradient'>";
                //html += "<div class='copyright'>";
                //html += "<span class='copyright-dir'><span class='nowrap'>Copyright &#169; 2022<a class ='underline' href = 'http://m.hoteamsoft.com' target= '_blank'>HOTEAMSOFT</a>,All rights reserved</span></span>";
                //html += "</div>";
                //html += "<div class='copyright'>";
                //html += "<span id='all'><a class ='underline' href='https://www.hoteamsoft.com/about-us' target='_blank'> Contact Me </a></span>";
                //html += "</div></div></div>";
                childElement.push(bodyheader);
                childElement.push(licenseCreateDate);
                childElement.push(licenseCreator);
                childElement.push(newLabel);
                return childElement;
            }
            /**
             * 设置产品及版本信息
             * @param productName
             * @param version
             */
            setHeaderName(productName, version) {
                if (this.eleLabel.getElementsByClassName("SView-license-headername").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-headername")[0].innerHTML = productName + " v" + version;
            }
            /**
             * 设置到期时间
             * @param expiretime
             */
            setExpiretime(expiretime) {
                if (this.eleLabel.getElementsByClassName("SView-license-expiretime").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-expiretime")[0].innerHTML = expiretime;
            }
            /**
             * 设置用户名称
             * @param customer
             */
            setCustomer(customer) {
                if (this.eleLabel.getElementsByClassName("SView-license-usermessages").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-usermessages")[0].innerHTML = customer;
            }
            /**
            * 设置邮箱
            * @param customer
            */
            setEmail(email) {
                if (this.eleLabel.getElementsByClassName("SView-license-useremail").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-useremail")[0].innerHTML = email;
            }
            /**
             * 设置ID
             * @param licenseid
             */
            setID(licenseid) {
                if (this.eleLabel.getElementsByClassName("SView-license-licenseid").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-licenseid")[0].innerHTML = "NO." + licenseid;
            }
            /**
             * 设置制作时间
             * @param createDate
             */
            setCreateDate(createDate) {
                if (this.eleLabel.getElementsByClassName("SView-license-licenseCreateDateSpan").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-licenseCreateDateSpan")[0].innerHTML = " " + createDate;
            }
            /**
            * 设置制作工具
            * @param creator
            */
            setCreator(creator) {
                if (this.eleLabel.getElementsByClassName("SView-license-licenseCreatorSpan").length > 0)
                    this.eleLabel.getElementsByClassName("SView-license-licenseCreatorSpan")[0].innerHTML = " " + creator;
            }
        }
        Windows.License = License;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：列表
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class List extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.listSubComponents = [];
                this.maxCount = null;
                this.isSelect = false; //点击是否有选中效果
                this.isMultiselect = false; //是否多选
                this.listValue = "";
                // 默认类名
                this.eleClasses = ["SView-labelItem-List"];
            }
            size() {
                return this.allSubComponents.size;
            }
            /**
             * @description  创建列表
             * @param attendeeList 列表信息
             */
            createLabel(list) {
                let tagName = Object.keys(list)[0];
                list = list[tagName];
                let divElement = document.createElement("ul");
                let subComponents = list.subComponents;
                let that = this;
                if (list.primaryTitle && list.primaryTitle != "") {
                    let primaryLi = document.createElement("li");
                    let primaryEle = document.createElement("h2");
                    primaryEle.innerText = list.primaryTitle;
                    primaryLi.appendChild(primaryEle);
                    divElement.appendChild(primaryLi);
                }
                if (list.secondaryTitle && list.secondaryTitle != "") {
                    let secondaryLi = document.createElement("li");
                    let secondaryEle = document.createElement("h3");
                    secondaryEle.innerText = list.secondaryTitle;
                    secondaryLi.appendChild(secondaryEle);
                    divElement.appendChild(secondaryLi);
                }
                if (list.maxCount && (typeof list.maxCount) == 'number') {
                    this.maxCount = list.maxCount > 0 ? list.maxCount : null;
                }
                if (list.isMultiselect) {
                    that.isMultiselect = list.isMultiselect;
                }
                if (list.isSelect) {
                    that.isSelect = list.isSelect;
                }
                for (let i = 0; i < subComponents.length; i++) {
                    if (this.maxCount && i == this.maxCount) {
                        break;
                    }
                    this.listSubComponents.push(subComponents[i]);
                    let eleLi = document.createElement("li");
                    divElement.appendChild(eleLi);
                    // 增加子控件
                    let tagName = Object.keys(subComponents[i])[0];
                    if (subComponents[i][tagName].isWarp) {
                        eleLi.setAttribute("class", "SView-listItem-content");
                    }
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    eleLi.appendChild(newLabel);
                    eleLi.onclick = function () {
                        that.itemClick(newElement);
                    };
                }
                this.eleLabel = divElement;
                if (list.flexDirection) {
                    this.setFlexDirection(list.flexDirection);
                }
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 新增列表子控件
            * @param subComponents 子控件
            * @param position
            */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("li");
                let that = this;
                for (let i = 0; i < subComponents.length; i++) {
                    if (this.maxCount && i == this.maxCount) {
                        break;
                    }
                    this.listSubComponents.push(subComponents[i]);
                    let eleLi = document.createElement("li");
                    // 增加子控件
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    eleLi.appendChild(newLabel);
                    eleLi.onclick = function () {
                        that.itemClick(newElement);
                    };
                    if (position) {
                        this.eleLabel.insertBefore(eleLi, components[position - 1 + i]);
                    }
                    else {
                        this.eleLabel.appendChild(eleLi);
                    }
                    if (this.maxCount) {
                        this.removeSubComponents(1);
                    }
                }
            }
            itemClick(item) {
                if (this.isMultiselect) {
                    if (item.eleClasses.length > 0 && item.eleClasses[0] == "SView-list-item-active") {
                        item.setClasses([""]);
                    }
                    else {
                        item.setClasses(["SView-list-item-active"]);
                    }
                }
                else {
                    if (this.isSelect) {
                        if (this.eleLabel.querySelector(".SView-list-item-active")) {
                            this.eleLabel.querySelector(".SView-list-item-active").className = "";
                        }
                        item.setClasses(["SView-list-item-active"]);
                    }
                }
                if ((typeof this.eleOnClick) === "string") {
                    item.eleLabel.setAttribute("onclick", this.eleOnClick);
                }
                else {
                    this.eleOnClick(item);
                }
            }
            /**
             * @description 选中某个item
             * @param item
             */
            selectItem(item) {
                if (this.isMultiselect) {
                    if (item.eleClasses.length > 0 && item.eleClasses[0] == "SView-list-item-active") {
                        item.setClasses([""]);
                    }
                    else {
                        item.setClasses(["SView-list-item-active"]);
                    }
                }
                else {
                    if (this.isSelect) {
                        if (this.eleLabel.querySelector(".SView-list-item-active")) {
                            this.eleLabel.querySelector(".SView-list-item-active").className = "";
                        }
                        item.setClasses(["SView-list-item-active"]);
                    }
                }
            }
            /**
             * @description 取消选中某个item
             * @param item
             */
            unselectItem(item) {
                if (item && item.eleClasses.length > 0 && item.eleClasses[0] == "SView-list-item-active") {
                    item.setClasses([""]);
                }
            }
            /**
             * @description 清空选中
             * */
            clearSelect() {
                if (this.eleLabel.querySelector(".SView-list-item-active")) {
                    this.eleLabel.querySelector(".SView-list-item-active").className = "";
                }
            }
            /**
            * @description 获取选中值
            */
            getValue() {
                this.listValue = "";
                let childs = this.allSubComponents;
                childs.forEach((value, key) => {
                    if (value.eleClasses.length > 0 && value.eleClasses[0] == "SView-list-item-active")
                        this.listValue += this.listValue != "" ? "," + value.getValue() : value.getValue();
                });
                return this.listValue;
            }
            /**
            * @description 设置点击事件
            * @param onClickEvent 点击事件
            */
            setOnClickEvent(onClickEvent, parentNode) {
                if (onClickEvent) {
                    this.eleOnClick = onClickEvent;
                }
            }
            /**
            * @description 调整菜单栏子控件位置
            * @param oldPosition
            * @param newPosition
            */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.querySelectorAll("li");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.listSubComponents[oldPosition - 1];
                this.listSubComponents.splice(oldPosition - 1, 1);
                this.listSubComponents.splice(newPosition - 1, 0, elementInfo);
                components[oldPosition - 1].parentNode.removeChild(components[oldPosition - 1]);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
            * @description 删除菜单栏子控件
            * @param position 位置
            */
            removeSubComponents(position) {
                let components = this.eleLabel.querySelectorAll("li");
                if (position) {
                    if (components.length > 0) {
                        let deleteElement = components[position - 1];
                        this.listSubComponents.splice(position - 1, 1);
                        // 清空
                        this.allSubComponents.set(deleteElement.children[0].id, null);
                        // 删除
                        this.allSubComponents.delete(deleteElement.children[0].id);
                        // 移除标签
                        deleteElement.parentNode.removeChild(deleteElement);
                    }
                }
                else {
                    if (components.length > 0) {
                        let deleteElement = components[0];
                        this.listSubComponents = [];
                        // 删除
                        this.allSubComponents.clear();
                        // 移除标签
                        deleteElement.parentNode.innerHTML = "";
                    }
                }
            }
            /**
             *
             * @description 根据设置的ID 删除菜单栏子控件
             * @param key id
             */
            removeSubComponent(key) {
                let components = this.eleLabel.querySelectorAll("li");
                if (components.length > 0) {
                    for (let i = 0; i < components.length; i++) {
                        if (components[i].children[0].id === key) {
                            let deleteElement = components[i];
                            this.listSubComponents.splice(i, 1);
                            // 移除标签
                            deleteElement.parentNode.removeChild(deleteElement);
                            // 清空
                            this.allSubComponents.set(key, null);
                            // 删除
                            this.allSubComponents.delete(key);
                            break;
                        }
                    }
                }
            }
            getAllSubComponents() {
                return this.allSubComponents;
            }
        }
        Windows.List = List;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：气泡弹框
         *@author	ZYC
         *@date		2022-2-11
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Popover extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.popoverSubComponents = [];
                this.popoverIsShow = false; // 默认不显示
                // 默认样式：向上弹出
                this.eleClasses = ["SView-popover", "SView-popover-down"];
            }
            /**
            * @description 创建气泡弹出框
            * @param popoverInfo 弹出信息
            */
            createLabel(popoverInfo) {
                let tagName = Object.keys(popoverInfo)[0];
                popoverInfo = popoverInfo[tagName];
                let divElement = document.createElement("div");
                // 创建表格
                let tableElement = document.createElement("table");
                divElement.appendChild(tableElement);
                // 创建表格行
                let subComponents = popoverInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName_1 = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName_1]();
                    this.allSubComponents.set(subComponents[i][tagName_1].id, newElement);
                    this.popoverSubComponents.push(subComponents[i]);
                    newElement.create(subComponents[i]);
                    tableElement.appendChild(newElement.eleLabel);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 添加气泡弹出框子控件
            * @param subComponents 子控件
            * @param position 位置
            */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("tr");
                let newLabel;
                for (let i = 0; i < subComponents.length; i++) {
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.popoverSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.popoverSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    this.eleLabel.getElementsByTagName("table")[0].insertBefore(newLabel, components[position - 1 + i]);
                }
            }
            /**
            * @description 调整气泡弹出框子控件位置
            * @param oldPosition 老位置
            * @param newPosition 新位置
            */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("tr");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.popoverSubComponents[oldPosition - 1];
                this.popoverSubComponents.splice(oldPosition - 1, 1);
                this.popoverSubComponents.splice(newPosition - 1, 0, elementInfo);
                changeElement.parentNode.removeChild(changeElement);
                this.eleLabel.getElementsByTagName("table")[0].insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
            * @description 移除气泡弹出框子控件
            * @param position 位置
            */
            removeSubcomponents(position) {
                let components = this.eleLabel.getElementsByTagName("tr");
                let deleteElement = components[position - 1];
                this.popoverSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
            * @description 获取气泡弹出框子控件列表
            */
            getSubComponents() {
                return this.popoverSubComponents;
            }
        }
        Windows.Popover = Popover;
        /**
         * @description 爆炸行
         * */
        class PopoverRow extends Windows.Label {
            constructor() {
                super(...arguments);
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.popoverRowLabel = "";
                this.popoverRowSubComponents = [];
                this.popoverRowLabelFontSize = "";
                this.popoverRowLabelColor = "";
            }
            /**
             * @description 创建标签
             * @param rowInfo 行信息
             */
            createLabel(rowInfo) {
                let tagName = Object.keys(rowInfo)[0];
                rowInfo = rowInfo[tagName];
                let rowElement = document.createElement("tr");
                // 创建表格行内元素
                for (let i = 0; i < rowInfo.subComponents.length; i++) {
                    let cellElement = document.createElement("td");
                    let keyName = Object.keys(rowInfo.subComponents[i])[0];
                    // 若为标签
                    if (keyName == "Label") {
                        cellElement.setAttribute("class", "SView-popover-label");
                        this.popoverRowLabel = rowInfo.subComponents[i].Label;
                        cellElement.innerText = this.popoverRowLabel;
                    }
                    else {
                        let newElement = new SView.Windows[keyName]();
                        this.allSubComponents.set(rowInfo.subComponents[i][keyName].id, newElement);
                        this.popoverRowSubComponents.push(rowInfo.subComponents[i]);
                        newElement.create(rowInfo.subComponents[i]);
                        cellElement.appendChild(newElement.eleLabel);
                    }
                    rowElement.appendChild(cellElement);
                }
                this.eleLabel = rowElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
           * @description 添加子控件
           * @param subComponents 子控件
           * @param position 位置
           */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("td");
                let newLabel;
                for (let i = 0; i < subComponents.length; i++) {
                    let cellElement = document.createElement("td");
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.popoverRowSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.popoverRowSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    cellElement.appendChild(newLabel);
                    this.eleLabel.insertBefore(cellElement, components[position - 1 + i]);
                }
            }
            /**
            * @description 调整子控件位置
            * @param oldPosition 老位置
            * @param newPosition 新位置
            */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("td");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.popoverRowSubComponents[oldPosition - 1];
                this.popoverRowSubComponents.splice(oldPosition - 1, 1);
                this.popoverRowSubComponents.splice(newPosition - 1, 0, elementInfo);
                changeElement.parentNode.removeChild(changeElement);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
            * @description 移除子控件
            * @param position 位置
            */
            removeSubcomponents(position) {
                let components = this.eleLabel.getElementsByTagName("td");
                let deleteElement = components[position - 1];
                this.popoverRowSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
            * @description 获取子控件列表
            */
            getSubComponents() {
                return this.popoverRowSubComponents;
            }
            /**
            * @description 设置气泡弹出框标题(位置从1开始)
            * @param title 标题
            * @param position 位置
            */
            setTitle(title) {
                this.popoverRowLabel = title;
                let cellElement = this.eleLabel.getElementsByClassName("SView-popover-label")[0];
                cellElement.innerText = this.popoverRowLabel;
            }
            /**
            * @description 获取气泡弹出框标题列表
            */
            getTitle() {
                return this.popoverRowLabel;
            }
            /**
            * @description 设置标题字体大小
            * @param value 字体大小
            */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.popoverRowLabelFontSize = value + "px";
                }
                else {
                    this.popoverRowLabelFontSize = value;
                }
                let cellElement = this.eleLabel.getElementsByClassName("SView-popover-label")[0];
                cellElement.style.fontSize = this.popoverRowLabelFontSize;
            }
            /**
            * @description 设置标题字体颜色
            * @param color 颜色
            */
            setTitleColor(color) {
                this.popoverRowLabelColor = color;
                let cellElement = this.eleLabel.getElementsByClassName("SView-popover-label")[0];
                cellElement.style.color = this.popoverRowLabelColor;
            }
        }
        Windows.PopoverRow = PopoverRow;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：气泡选择器
         *@author	ZYC
         *@date		2022-2-11
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class PopoverSelect extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.popoverSelectSubComponents = [];
                this.popoverSelectIsShow = false; // 默认不显示
                // 默认气泡选择器：向上弹出，单倍宽度
                this.eleClasses = ["SView-popoverSelect", "SView-popoverSelect-up"];
            }
            /**
             * @description 创建气泡选择器标签
             * @param popoverSelectInfo 气泡选择信息
             */
            createLabel(popoverSelectInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(popoverSelectInfo)[0];
                    popoverSelectInfo = popoverSelectInfo[tagName];
                    let divElement = document.createElement("div");
                    let eleUl = document.createElement("ul");
                    divElement.appendChild(eleUl);
                    // 增加子控件
                    let subComponents = popoverSelectInfo.subComponents;
                    for (let i = 0; i < subComponents.length; i++) {
                        let eleLi = document.createElement("li");
                        eleUl.appendChild(eleLi);
                        // 增加子控件
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.popoverSelectSubComponents.push(subComponents[i]);
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        eleLi.setAttribute("class", " ");
                        newLabel.setAttribute("class", newElement.eleClasses.join(" "));
                        eleLi.appendChild(newLabel);
                    }
                    let that = this;
                    document.onclick = function () {
                        that.hide();
                    };
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加子控件
             * @param subComponests 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("li");
                let newLabel;
                for (let i = 0; i < subComponents.length; i++) {
                    let eleLi = document.createElement("li");
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        eleLi.setAttribute("class", subComponents[i].eleClasses.join(" "));
                        newLabel.setAttribute("class", "");
                        this.popoverSelectSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.popoverSelectSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                        eleLi.setAttribute("class", newElement.eleClasses.join(" "));
                        newLabel.setAttribute("class", "");
                    }
                    eleLi.appendChild(newLabel);
                    this.eleLabel.getElementsByTagName("ul")[0].insertBefore(eleLi, components[position - 1 + i]);
                }
            }
            /**
             * @description 移除子控件
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.getElementsByTagName("li");
                let deleteElement = components[position - 1];
                this.popoverSelectSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 调整子控件位置(位置从1开始)
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("li");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.popoverSelectSubComponents[oldPosition - 1];
                this.popoverSelectSubComponents.splice(oldPosition - 1, 1);
                this.popoverSelectSubComponents.splice(newPosition - 1, 0, elementInfo);
                changeElement.parentNode.removeChild(changeElement);
                this.eleLabel.getElementsByTagName("ul")[0].insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
             * @description 获取子控件列表
             */
            getSubComponents() {
                return this.popoverSelectSubComponents;
            }
        }
        Windows.PopoverSelect = PopoverSelect;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：右键菜单
         *@author	ZYC
         *@date		2022-2-16
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class RightClickMenu extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.rightClickMenuSubComponents = [];
                // 所有子控件集合
                this.allSubComponents = new Map();
                // 默认类名
                this.eleClasses = ["SView-rightClickMenu"];
            }
            /**
             * @description 创建右键菜单
             * @param rightClickMenuInfo 右键点击菜单信息
             */
            createLabel(rightClickMenuInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(rightClickMenuInfo)[0];
                    rightClickMenuInfo = rightClickMenuInfo[tagName];
                    let ulElement = document.createElement("ul");
                    let subComponents = rightClickMenuInfo.subComponents;
                    for (let i = 0; i < subComponents.length; i++) {
                        this.rightClickMenuSubComponents.push(subComponents[i]);
                        let tagNameBtn = Object.keys(subComponents[i])[0];
                        let newElement = new RightClickBtn();
                        this.allSubComponents.set(subComponents[i][tagNameBtn].id, newElement);
                        newElement.create(subComponents[i]);
                        let liElement = newElement.getLabel();
                        ulElement.appendChild(liElement);
                    }
                    this.eleLabel = ulElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加右键菜单按钮（位置从1开始）
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("li");
                for (let i = 0; i < subComponents.length; i++) {
                    let liElement;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        liElement = subComponents[i].eleLabel;
                        this.rightClickMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.rightClickMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let newElement = new RightClickBtn();
                        this.allSubComponents.set(subComponents[i].id, newElement);
                        newElement.create(subComponents[i]);
                        liElement = newElement.eleLabel;
                    }
                    this.eleLabel.insertBefore(liElement, components[position - 1 + i]);
                }
            }
            /**
             * @description 调整右键菜单按钮位置
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("li");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.rightClickMenuSubComponents[oldPosition - 1];
                this.rightClickMenuSubComponents.splice(oldPosition - 1, 1);
                this.rightClickMenuSubComponents.splice(newPosition - 1, 0, elementInfo);
                this.eleLabel.removeChild(components[oldPosition - 1]);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
             * @description 移除右键菜单按钮
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.getElementsByTagName("li");
                let deleteElement = components[position - 1];
                this.rightClickMenuSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 获取右键菜单按钮列表
             */
            getSubComponents() {
                return this.rightClickMenuSubComponents;
            }
        }
        Windows.RightClickMenu = RightClickMenu;
        /**
         * @description 右键点击按钮
         *
         * */
        class RightClickBtn extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.rightClickBtnContent = "";
                this.rightClickBtnContentFontSize = "";
                this.rightClickBtnContentColor = "";
                this.rightClickBtnImgsrc = "";
            }
            /**
             * @description 创建右键菜单按钮标签
             * @param rightClickBtnInfo 右键点击按钮信息
             */
            createLabel(rightClickBtnInfo) {
                let tagName = Object.keys(rightClickBtnInfo)[0];
                rightClickBtnInfo = rightClickBtnInfo[tagName];
                let liElement = document.createElement("li");
                if (rightClickBtnInfo.imgsrc && rightClickBtnInfo.imgsrc != "") {
                    let imgElement = document.createElement("img");
                    this.rightClickBtnImgsrc = rightClickBtnInfo.imgsrc;
                    imgElement.setAttribute("src", Windows.Label.rootPath + this.rightClickBtnImgsrc);
                    liElement.appendChild(imgElement);
                }
                if (rightClickBtnInfo.content && rightClickBtnInfo.content != "") {
                    let spanElement = document.createElement("span");
                    this.rightClickBtnContent = rightClickBtnInfo.content;
                    spanElement.innerText = this.rightClickBtnContent;
                    liElement.appendChild(spanElement);
                }
                this.eleLabel = liElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
             * @description 设置按钮内容
             * @param content 内容
             */
            setContent(content) {
                this.rightClickBtnContent = content;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                if (spanElement) {
                    spanElement.innerText = this.rightClickBtnContent;
                }
            }
            /**
             * @description 设置按钮图片路径
             * @param imgsrc 图片路径
             */
            setImage(imgsrc) {
                this.rightClickBtnImgsrc = imgsrc;
                let imgElement = this.eleLabel.getElementsByTagName("img")[0];
                if (imgElement) {
                    imgElement.setAttribute("src", Windows.Label.rootPath + this.rightClickBtnImgsrc);
                }
            }
            /**
             * @description  获取按钮内容
             */
            getContent() {
                return this.rightClickBtnContent;
            }
            /**
             * @description 获取按钮图片路径
             */
            getImage() {
                return this.rightClickBtnImgsrc;
            }
            /**
             * @description 设置按钮内容字体大小
             * @param value
             */
            setContentFontSize(value) {
                if (typeof (value) == "number") {
                    this.rightClickBtnContentFontSize = value + "px";
                }
                else {
                    this.rightClickBtnContentFontSize = value;
                }
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.fontSize = this.rightClickBtnContentFontSize;
            }
            /**
             * @description 设置按钮内容字体颜色
             * @param color
             */
            setContentColor(color) {
                this.rightClickBtnContentColor = color;
                let spanElement = this.eleLabel.getElementsByTagName("span")[0];
                spanElement.style.color = this.rightClickBtnContentColor;
            }
        }
        Windows.RightClickBtn = RightClickBtn;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：选择器
         *@author	ZYC
         *@date		2022-2-08
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Select extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.selectOptions = [];
                this.selectPlaceHolder = "";
                this.disabled = false;
                this.eleClasses = ["SView-select"];
            }
            /**
             * @description 创建选择器标签
             * @param selectInfo  选择器信息
             */
            createLabel(selectInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(selectInfo)[0];
                    selectInfo = selectInfo[tagName];
                    // 选择器
                    let selectElement = document.createElement("select");
                    if (selectInfo.value) {
                        this.selectValue = selectInfo.value;
                        selectElement.value = selectInfo.value.toString();
                    }
                    if (selectInfo.placeHolder && selectInfo.placeHolder != "") {
                        this.selectPlaceHolder = selectInfo.placeHolder;
                        selectElement.setAttribute("placeholder", this.selectPlaceHolder);
                    }
                    if (selectInfo.disabled && (typeof selectInfo.disabled) == "boolean") {
                        this.disabled = true;
                        selectElement.setAttribute("disabled", "true");
                    }
                    // 选项
                    let options = selectInfo.options;
                    for (let i = 0; i < options.length; i++) {
                        this.selectOptions.push(options[i]);
                        let newElement = new Option();
                        this.allSubComponents.set(options[i].id, newElement);
                        newElement.create(options[i]);
                        let newLabel = newElement.getLabel();
                        if (newLabel.value == this.selectValue) {
                            newLabel.setAttribute("selected", "selected");
                        }
                        selectElement.appendChild(newLabel);
                    }
                    this.eleLabel = selectElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加选项标签
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("option");
                for (let i = 0; i < subComponents.length; i++) {
                    let optionElement;
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        optionElement = subComponents[i].eleLabel;
                        this.selectOptions.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.selectOptions.splice(position - 1 + i, 0, subComponents[i]);
                        let newElement = new Option();
                        this.allSubComponents.set(subComponents[i].id, newElement);
                        newElement.create(subComponents[i]);
                        optionElement = newElement.eleLabel;
                    }
                    this.eleLabel.insertBefore(optionElement, components[position - 1 + i]);
                }
            }
            /**
             * @description 调整选项位置
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("option");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.selectOptions[oldPosition - 1];
                this.selectOptions.splice(oldPosition - 1, 1);
                this.selectOptions.splice(newPosition - 1, 0, elementInfo);
                this.eleLabel.removeChild(components[oldPosition - 1]);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
             * @description 移除选项
             * @param position 位置
             */
            removeSubComponents(position) {
                let index = -1;
                let deleteElement = null;
                let components = this.eleLabel.getElementsByTagName("option");
                if (position instanceof Option) {
                    for (let i = 0; i < components.length; i++) {
                        let option = components[i];
                        if (option.value === position.getValue()) {
                            index = i + 1;
                            deleteElement = option;
                            break;
                        }
                    }
                }
                else {
                    index = position;
                    deleteElement = components[index - 1];
                }
                if (deleteElement) {
                    this.selectOptions.splice(index - 1, 1);
                    // 清空
                    this.allSubComponents.set(deleteElement.id, null);
                    // 删除
                    this.allSubComponents.delete(deleteElement.id);
                    // 移除标签
                    deleteElement.parentNode.removeChild(deleteElement);
                }
            }
            /**
             * @description 设置选中值
             * @param value 选项值
             */
            setValue(value) {
                this.selectValue = value;
                this.eleLabel.value = this.selectValue;
            }
            /**
            * @description 获取选中值
            */
            getValue() {
                this.selectValue = this.eleLabel.value;
                return this.selectValue;
            }
            /**
             * @description 设置禁用
             */
            isDisabled(value) {
                this.disabled = value;
                this.eleLabel.disabled = value;
            }
            /**
             * @description 获取选项列表
             */
            getSubComponents() {
                return this.selectOptions;
            }
        }
        Windows.Select = Select;
        /**
         * @description选项
         * */
        class Option extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.optionContent = "";
                this.optionValue = "";
            }
            /**
             * @description 创建选项标签
             * @param selectInfo  选择信息
             */
            createLabel(optionInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(optionInfo)[0];
                    optionInfo = optionInfo[tagName];
                    let optionElement = document.createElement("option");
                    //let imgElement
                    //if (options[i].imgsrc) {
                    //    imgElement = document.createElement("img");
                    //    imgElement.setAttribute("src", options[i].imgsrc);
                    //    optionElement.appendChild(imgElement);
                    //}
                    if (optionInfo.value || optionInfo.value == 0) {
                        this.optionValue = optionInfo.value;
                        optionElement.setAttribute("value", this.optionValue);
                    }
                    if (optionInfo.selected) {
                        optionElement.setAttribute("selected", "selected");
                    }
                    if (optionInfo.content || optionInfo.content == 0) {
                        this.optionContent = optionInfo.content;
                        optionElement.innerText = this.optionContent;
                    }
                    this.eleLabel = optionElement;
                    resolve(this);
                    reject("创建失败");
                });
            }
            /**
             * @description 设置选项内容
             * @param content 内容
             */
            setContent(content) {
                this.optionContent = content;
                this.eleLabel.innerText = this.optionContent;
            }
            /**
             * @description 设置选项值
             * @param value 选项值
             */
            setValue(value) {
                if (typeof (value) == "number") {
                    this.optionValue = value.toString();
                }
                else {
                    this.optionValue = value;
                }
                this.eleLabel.setAttribute("value", this.optionValue);
            }
            /**
             * @description 获取选项内容
             */
            getContent() {
                return this.optionContent;
            }
            /**
             * @description 获取选项值
             */
            getValue() {
                return this.optionValue;
            }
        }
        Windows.Option = Option;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：可编辑选项选择器
         *@author	ZYC
         *@date		2022-2-08
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class SelectChoices extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.selectOptions = [];
                this.addOptionFun = null;
                this.removeOptionFun = null;
                this.isMultiselect = false;
                this.selectChoicesType = "leftText";
                this.eleClasses = ["SView-choices-select"];
            }
            /**
             * @description 创建选择器标签
             * @param selectInfo  选择器信息
             */
            createLabel(selectInfo) {
                let that = this;
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(selectInfo)[0];
                    selectInfo = selectInfo[tagName];
                    //外层div
                    let divElement = document.createElement("div");
                    // 选择器
                    let selectElement = document.createElement("div");
                    selectElement.setAttribute("class", "SView-choices-input");
                    selectElement.onclick = function () {
                        event.stopPropagation();
                        that.showOrhideOption();
                    };
                    if (selectInfo.type && selectInfo.type != "") {
                        that.selectChoicesType = selectInfo.type;
                    }
                    if (selectInfo.isMultiselect) {
                        that.isMultiselect = selectInfo.isMultiselect;
                    }
                    // 选项
                    let eleUI = document.createElement("ul");
                    eleUI.setAttribute("class", "SView-choices-option SView-choices-option-" + that.selectChoicesType);
                    divElement.appendChild(selectElement);
                    if (selectInfo.addOption) {
                        let addOptionJson = selectInfo.addOption.OptionChoices;
                        addOptionJson.id = "addOptionChoices";
                        addOptionJson.imgSrc = selectInfo.addOption.imgSrc ? selectInfo.addOption.imgSrc : '';
                        let addOption = new OptionChoices();
                        addOption.setParent(that);
                        addOption.create(selectInfo.addOption);
                        addOption.setOptionClick(that.addOption);
                        that.allSubComponents.set(addOptionJson.id, addOption);
                        eleUI.appendChild(addOption.eleLabel);
                    }
                    let selectOption = null;
                    if (selectInfo.options && selectInfo.options.length > 0) {
                        let options = selectInfo.options;
                        for (let i = 0; i < options.length; i++) {
                            let option = new OptionChoices();
                            option.setParent(that);
                            option.create(options[i]);
                            option.setImageClick(that.removeOption);
                            that.allSubComponents.set(option.getId(), option);
                            if (options[i].value == that.selectValue) {
                                selectOption = option;
                            }
                            eleUI.appendChild(option.eleLabel);
                        }
                    }
                    if (selectInfo.value) {
                        that.selectOption(selectOption, that);
                    }
                    else {
                        selectElement.innerText = selectInfo.placeholder ? selectInfo.placeholder : "";
                    }
                    divElement.appendChild(eleUI);
                    that.eleLabel = divElement;
                    resolve(that);
                });
            }
            /**
             * @description 设置添加选项的点击事件
             */
            setAddOptionFun(func) {
                if (func && (typeof func) == "function") {
                    this.addOptionFun = func;
                }
            }
            /**
             * @description 添加选项的点击
             */
            addOption(option, that) {
                if (that.addOptionFun) {
                    that.addOptionFun(that);
                }
            }
            /**
             * @description 设置删除选项的点击事件
             */
            setRemoveOptionFun(func) {
                if (func && (typeof func) == "function") {
                    this.removeOptionFun = func;
                }
            }
            /**
             * @description 删除选项的点击
             */
            removeOption(info, that) {
                if (that.removeOptionFun) {
                    that.removeOptionFun(info);
                }
            }
            /**
             * @description 下拉选项的点击
             */
            selectOption(option, that) {
                if (option.eleId === "addOptionChoices") {
                    return;
                }
                let childs = that.allSubComponents;
                if (!this.isMultiselect) {
                    childs.forEach((value, key) => {
                        value.setClasses([""]);
                    });
                }
                that.setValue(option.getValue());
                if (!this.isMultiselect) {
                    that.showOrhideOption();
                }
                if (that.eleOnChange && (typeof that.eleOnChange) == "function") {
                    that.eleOnChange(that);
                }
            }
            showOrhideOption() {
                let showOrHideFun = function () {
                    if (ele.style.display == "block") {
                        document.removeEventListener("click", showOrHideFun);
                        ele.style.display = "none";
                    }
                    document.removeEventListener("click", showOrHideFun);
                };
                let ele = this.eleLabel.getElementsByClassName("SView-choices-option")[0];
                if (ele.style.display == "block") {
                    document.removeEventListener("click", showOrHideFun);
                    ele.style.display = "none";
                }
                else {
                    document.addEventListener("click", showOrHideFun);
                    ele.style.display = "block";
                }
            }
            /**
             * @description 添加选项标签
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(options, position) {
                let components = this.eleLabel.getElementsByClassName("SView-choices-option")[0];
                let that = this;
                for (let i = 0; i < options.length; i++) {
                    let option = new OptionChoices();
                    option.setParent(that);
                    option.create(options[i]);
                    option.setImageClick(that.removeOption);
                    that.allSubComponents.set(option.getId(), option);
                    components.appendChild(option.eleLabel);
                }
            }
            /**
             * @description 移除选项
             * @param position 位置
             */
            removeSubComponents(value) {
                let index = -1;
                let deleteElement = null;
                let components = this.eleLabel.getElementsByClassName("SView-choices-option")[0].children;
                for (let i = 0; i < components.length; i++) {
                    let option = components[i];
                    if (option.getAttribute("data-value") === value.getValue()) {
                        index = i + 1;
                        deleteElement = option;
                        break;
                    }
                }
                let values = this.getValue().split(",");
                this.selectOptions.splice(index - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
                if (values.indexOf(value.getValue()) >= 0) {
                    this.selectValue = values.splice(values.indexOf(value.getValue()), 1).join(",");
                    let input = this.eleLabel.getElementsByClassName("SView-choices-input")[0];
                    input.setAttribute("data-value", this.getValue());
                    input.innerHTML = this.getSelectText();
                }
            }
            /**
             * @description 设置选中值
             * @param value 选项值
             */
            setValue(value) {
                let childs = this.allSubComponents;
                childs.forEach((v, key) => {
                    if (this.isMultiselect) {
                        if (v.getValue() === value) {
                            if (v.eleClasses.length > 0 && v.eleClasses[0] == "SView-choices-option-active") {
                                v.setClasses([""]);
                            }
                            else {
                                v.setClasses(["SView-choices-option-active"]);
                            }
                        }
                    }
                    else {
                        if (v.getValue() === value) {
                            v.setClasses(["SView-choices-option-active"]);
                        }
                        else {
                            v.setClasses([""]);
                        }
                    }
                });
                let input = this.eleLabel.getElementsByClassName("SView-choices-input")[0];
                input.setAttribute("data-value", this.getValue());
                input.innerHTML = this.getSelectText();
                input.setAttribute("title", this.getSelectText());
                this.selectValue = this.getValue();
                //(<HTMLInputElement>this.eleLabel).value = this.selectValue;
            }
            getSelectedOptions() {
                let childs = this.allSubComponents;
                let options = [];
                childs.forEach((value, key) => {
                    if (value.eleClasses.length > 0 && value.eleClasses[0] == "SView-choices-option-active")
                        options.push(value);
                });
                return options;
            }
            /**
            * @description 获取选中值
            */
            getValue() {
                this.selectValue = "";
                let childs = this.allSubComponents;
                childs.forEach((value, key) => {
                    if (value.eleClasses.length > 0 && value.eleClasses[0] == "SView-choices-option-active")
                        this.selectValue += this.selectValue != "" ? "," + value.getValue() : value.getValue();
                });
                return this.selectValue;
            }
            /**
            * @description 获取选中内容
            */
            getSelectText() {
                this.selectText = "";
                let childs = this.allSubComponents;
                childs.forEach((value, key) => {
                    if (value.eleClasses.length > 0 && value.eleClasses[0] == "SView-choices-option-active")
                        this.selectText += this.selectText != "" ? "," + value.getContent() : value.getContent();
                });
                return this.selectText;
            }
            /**
             * @description 获取选项列表
             */
            getSubComponents() {
                return this.selectOptions;
            }
            /**
           * @description 获取选项列表
           */
            getAllSubComponents() {
                return this.allSubComponents;
            }
            editSubComponentsID(id, newId) {
                if (this.allSubComponents.has(id)) {
                    let sub = this.allSubComponents.get(id);
                    this.allSubComponents.delete(id);
                    this.allSubComponents.set(newId, sub);
                }
            }
        }
        Windows.SelectChoices = SelectChoices;
        class OptionChoices extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.selectChoicesType = "leftText";
                this.parentSelect = null;
                this.eleClasses = [];
            }
            setParent(select) {
                this.parentSelect = select;
            }
            /**
             * @description 创建选择器标签
             * @param selectInfo  选择器信息
             */
            createLabel(optionJson) {
                let that = this;
                return new Promise((resolve, reject) => {
                    let options = optionJson.OptionChoices;
                    let option = document.createElement("li");
                    let label = document.createElement("label");
                    let img = document.createElement("img");
                    label.setAttribute("title", options.label);
                    label.innerText = options.label;
                    if (options.imgSrc) {
                        let imgSrc = options.imgSrc;
                        img.setAttribute('src', Windows.Label.rootPath + imgSrc);
                        that.image = imgSrc;
                        if (options.removeOptionFun && options.removeOptionFun != "") {
                            img.setAttribute("onclick", options.removeOptionFun);
                        }
                        option.appendChild(img);
                    }
                    option.appendChild(label);
                    that.text = options.label;
                    option.setAttribute("data-value", options.value + "");
                    that.optionValue = options.value + "";
                    option.addEventListener("click", function (e) {
                        that.parentSelect.selectOption(that, that.parentSelect);
                    }, true);
                    that.eleLabel = option;
                    resolve(that);
                });
            }
            /**
             * @description 设置选项内容
             * @param content 内容
             */
            setText(content) {
                this.text = content;
                this.eleLabel.getElementsByTagName("label")[0].innerText = this.text;
                this.eleLabel.getElementsByTagName("label")[0].setAttribute("title", this.text);
                let values = this.parentSelect.getValue().split(",");
                let value = this.getValue();
                if (values.indexOf(value) >= 0) {
                    this.parentSelect.selectValue = values.splice(values.indexOf(value), 1, value).join(",");
                    let input = this.parentSelect.eleLabel.getElementsByClassName("SView-choices-input")[0];
                    input.setAttribute("data-value", this.parentSelect.getValue());
                    input.setAttribute("title", this.parentSelect.getSelectText());
                    input.innerHTML = this.parentSelect.getSelectText();
                }
            }
            /**
             * @description 设置选项内容
             * @param content 内容
             */
            setImage(img) {
                this.image = img;
                if (this.eleLabel.getElementsByTagName("img").length > 0) {
                    this.eleLabel.getElementsByTagName("img")[0].setAttribute('src', Windows.Label.rootPath + this.image);
                }
            }
            /**
             * @description 设置选项值
             * @param value 选项值
             */
            setValue(value) {
                this.parentSelect.editSubComponentsID(this.optionValue, value.toString());
                let values = this.parentSelect.getValue().split(",");
                if (values.indexOf(value.toString()) >= 0) {
                    this.parentSelect.selectValue = values.splice(values.indexOf(value.toString()), 1, value.toString()).join(",");
                }
                if (typeof (value) == "number") {
                    this.optionValue = value.toString();
                }
                else {
                    this.optionValue = value;
                }
                this.eleLabel.setAttribute("data-value", this.optionValue);
            }
            /**
             * @description 获取选项内容
             */
            getContent() {
                return this.text;
            }
            /**
             * @description 获取选项值
             */
            getValue() {
                let value = this.optionValue;
                //value全部转为string
                if (value) {
                    value = value.toString();
                }
                return this.optionValue;
            }
            setOptionClick(fun) {
                let that = this;
                this.eleLabel.addEventListener("click", function () {
                    fun(that, that.parentSelect);
                }, false);
            }
            setImageClick(fun) {
                let that = this;
                if (this.eleLabel.getElementsByTagName("img").length > 0) {
                    this.eleLabel.getElementsByTagName("img")[0].addEventListener("click", function (event) {
                        event.preventDefault();
                        event.stopPropagation();
                        fun(that, that.parentSelect);
                    }, true);
                }
            }
        }
        Windows.OptionChoices = OptionChoices;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：滑块
         *@author	ZYC
         *@date		2022-2-08
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Slider extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 默认滑块，最小值：0、最大值：100、步长：1、当前值：50
                this.sliderMinNum = "0";
                this.sliderMaxNum = "100";
                this.sliderStepNum = "1";
                this.sliderValueNum = "50";
                this.eleClasses = ["SView-slider"];
            }
            /**
             * @description 创建滑块标签
             * @param sliderInfo  滑块信息
             */
            createLabel(sliderInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(sliderInfo)[0];
                    sliderInfo = sliderInfo[tagName];
                    let divElement = document.createElement("div");
                    // 添加滑动条
                    let inputInfo = sliderInfo.sliderBar;
                    if (inputInfo.min) {
                        this.sliderMinNum = inputInfo.min;
                    }
                    if (inputInfo.max) {
                        this.sliderMaxNum = inputInfo.max;
                    }
                    if (inputInfo.step) {
                        this.sliderStepNum = inputInfo.step;
                    }
                    if (inputInfo.value != undefined && inputInfo.value >= 0) {
                        this.sliderValueNum = inputInfo.value;
                    }
                    let inputElement = document.createElement("input");
                    inputElement.setAttribute("type", "range");
                    inputElement.setAttribute("id", inputInfo.id);
                    inputElement.setAttribute("min", this.sliderMinNum);
                    inputElement.setAttribute("max", this.sliderMaxNum);
                    inputElement.setAttribute("step", this.sliderStepNum);
                    inputElement.setAttribute("value", this.sliderValueNum);
                    inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(this.sliderMinNum)) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
                    divElement.appendChild(inputElement);
                    // 滑动滑块时，颜色填充
                    // 电脑端
                    if (inputInfo.sliderChange) {
                        inputElement.setAttribute("oninput", inputInfo.sliderChange);
                        //inputElement.setAttribute("ontouchmove", inputInfo.sliderChange);
                    }
                    else {
                        inputElement.oninput = function () {
                            inputElement.style.backgroundSize = (this.value - this.min) * 100 / (this.max - this.min) + "% 100%";
                        };
                        // 手机端
                        //inputElement.ontouchmove = function () {
                        //    inputElement.style.backgroundSize = ((this as any).value - (this as any).min) * 100 / ((this as any).max - (this as any).min) + "% 100%";
                        //}
                    }
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置滑块最小值
             * @param min 最小值
             */
            setMin(min) {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                if (typeof (min) == "number") {
                    this.sliderMinNum = min.toString();
                }
                else {
                    this.sliderMinNum = min;
                }
                inputElement.setAttribute("min", this.sliderMinNum);
                inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(this.sliderMinNum)) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
                inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(this.sliderMinNum)) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
            }
            /**
             * @description 设置滑块最大值
             * @param max 最大值
             */
            setMax(max) {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                if (typeof (max) == "number") {
                    this.sliderMaxNum = max.toString();
                }
                else {
                    this.sliderMaxNum = max;
                }
                inputElement.setAttribute("max", this.sliderMaxNum);
                inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(this.sliderMinNum)) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
            }
            /**
             * @description 设置滑块步长
             * @param step 步长
             */
            setStep(step) {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                if (typeof (step) == "number") {
                    this.sliderStepNum = step.toString();
                }
                else {
                    this.sliderStepNum = step;
                }
                inputElement.setAttribute("step", this.sliderStepNum);
            }
            /**
             * @description 设置滑块初始数值
             * @param value 数值
             */
            setInitValue(value) {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                if (typeof (value) == "number") {
                    this.sliderValueNum = value.toString();
                }
                else {
                    this.sliderValueNum = value;
                }
                inputElement.setAttribute("value", this.sliderValueNum);
                inputElement.setAttribute("style", "background-size: " + Number(this.sliderValueNum) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
                inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(this.sliderMinNum)) * 100 / (Number(this.sliderMaxNum) - Number(this.sliderMinNum)) + "% 100%");
            }
            /**
             * @description 设置滑块数值
             * @param value 数值
             */
            setValue(value) {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                if (typeof (value) == "number") {
                    this.sliderValueNum = value.toString();
                }
                else {
                    this.sliderValueNum = value;
                }
                inputElement.value = this.sliderValueNum;
                inputElement.setAttribute("style", "background-size: " + Number(this.sliderValueNum) * 100 / (Number(inputElement.max) - Number(inputElement.min)) + "% 100%");
                inputElement.setAttribute("style", "background-size: " + (Number(this.sliderValueNum) - Number(inputElement.min)) * 100 / (Number(inputElement.max) - Number(inputElement.min)) + "% 100%");
            }
            /**
             * @description 获取滑块最小值
             */
            getMin() {
                return this.sliderMinNum;
            }
            /**
             * @description 获取滑块最大值
             */
            getMax() {
                return this.sliderMaxNum;
            }
            /**
             * @description 获取滑块步长
             */
            getStep() {
                return this.sliderStepNum;
            }
            /**
             * @description 获取滑块初始数值
             */
            getInitValue() {
                return this.sliderValueNum;
            }
            /**
             * @description 设置change事件
             * @param onChangeEvent change事件
             */
            setOnChangeEvent(onChangeEvent) {
                if (onChangeEvent) {
                    if ((typeof onChangeEvent) === "string") {
                        this.eleLabel.getElementsByTagName("input")[0].setAttribute("oninput", onChangeEvent);
                    }
                    else {
                        this.eleLabel.getElementsByTagName("input")[0].oninput = onChangeEvent;
                    }
                }
            }
            /**
             * 获取滑块当前数值
             */
            getValue() {
                let inputElement = this.eleLabel.getElementsByTagName("input")[0];
                return inputElement.value;
            }
        }
        Windows.Slider = Slider;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：开关
         *@author	ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Switch extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.switchOn = false; // 默认开关关闭
                this.eleClasses = ["SView-switch"];
            }
            /**
             * @description 创建开关
             * @param switchInfo 开关信息
             */
            createLabel(switchInfo) {
                let tagName = Object.keys(switchInfo)[0];
                switchInfo = switchInfo[tagName];
                let divElement = document.createElement("div");
                // 判断复选框
                let eleInput = document.createElement("input");
                eleInput.setAttribute("class", "SView-switch-checkbox");
                eleInput.setAttribute("id", switchInfo.checkboxId);
                eleInput.setAttribute("type", "checkbox");
                divElement.appendChild(eleInput);
                // 开关是否打开
                if (switchInfo.checked == true) {
                    this.switchOn = true;
                    eleInput.checked = this.switchOn;
                }
                else {
                    this.switchOn = false;
                    eleInput.checked = this.switchOn;
                }
                // 开关标签
                let eleLabel = document.createElement("label");
                eleLabel.setAttribute("class", "SView-switch-label");
                eleLabel.setAttribute("for", switchInfo.checkboxId);
                divElement.appendChild(eleLabel);
                // 开关内部提示
                let eleInner = document.createElement("span");
                let innerDataOn = switchInfo.dataOn;
                let innerDataOff = switchInfo.dataOff;
                eleInner.setAttribute("class", "SView-switch-inner");
                if (switchInfo.dataOn != null) {
                    eleInner.setAttribute("data-on", innerDataOn);
                }
                if (switchInfo.dataOff != null) {
                    eleInner.setAttribute("data-off", innerDataOff);
                }
                eleLabel.appendChild(eleInner);
                // 开关按钮
                let eleButton = document.createElement("span");
                eleButton.setAttribute("class", "SView-switch-button");
                eleLabel.appendChild(eleButton);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置开关状态
             * @param on 是否开
             */
            setOn(on) {
                this.switchOn = on;
                let switchElement = this.eleLabel.getElementsByTagName("input")[0];
                switchElement.checked = this.switchOn;
            }
        }
        Windows.Switch = Switch;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：表格
         *@author	ZYC
         *@date		2022-2-15
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Table extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.tableContent = [];
                this.tableTitle = [];
                // 默认类名
                this.eleClasses = ["SView-table"];
            }
            /**
            * @description 创建表格
            * @param tableInfo 表格信息
            */
            createLabel(tableInfo) {
                let tagName = Object.keys(tableInfo)[0];
                tableInfo = tableInfo[tagName];
                let tableElement = document.createElement("table");
                if (tableInfo.cellTitle && tableInfo.cellTitle.length > 0) {
                    this.tableTitle = tableInfo.cellTitle;
                } //else {
                //    this.tableTitle = ["主题", "会议号", "主持人", "创建时间"];
                //}
                if (tableInfo.cellContent && tableInfo.cellContent.length > 0) {
                    this.tableContent = tableInfo.cellContent;
                } //else {
                //    this.tableContent = [
                //        [
                //            "主题", "会议号", "主持人", "创建时间"
                //        ], [
                //            "user_1的会议", "80939491", "user_1", "2022-02-15 14:55:24"
                //        ], [
                //            "user_2的会议", "22575297", "user_2", "2022-01-15 15:32:16"
                //        ], [
                //            "Administrator的会议", "34739797", "Administrator", "2022-01-13 09:13:56"
                //        ]
                //    ]
                //}
                // 表头部分
                let trTitleElement = document.createElement("tr");
                trTitleElement.setAttribute("class", "SView-table-title");
                tableElement.appendChild(trTitleElement);
                for (let i = 0; i < this.tableTitle.length; i++) {
                    // 表格单元格
                    let thElement = document.createElement("th");
                    thElement.innerText = this.tableTitle[i];
                    trTitleElement.appendChild(thElement);
                }
                // 内容部分
                for (let i = 0; i < this.tableContent.length; i++) {
                    // 表格行
                    let trElement = document.createElement("tr");
                    trElement.setAttribute("class", "SView-table-content");
                    trElement.onclick = function () {
                        var nodeList = trElement.parentNode.querySelectorAll(".SView-table-content");
                        for (var f = 0; f < nodeList.length; f++) {
                            if (nodeList[f].className.indexOf("SView-table-content-active") > -1) {
                                nodeList[f].className = "SView-table-content";
                                break;
                            }
                        }
                        trElement.className = "SView-table-content SView-table-content-active";
                    };
                    for (let j = 0; j < this.tableContent[i].length; j++) {
                        // 表格单元格
                        let tdElement = document.createElement("td");
                        tdElement.innerText = this.tableContent[i][j];
                        trElement.appendChild(tdElement);
                    }
                    tableElement.appendChild(trElement);
                }
                this.eleLabel = tableElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 设置表格标题（位置从1开始）
            * @param position 位置
            * @param title 标题
            */
            setTitle(position, title) {
                this.tableTitle[position - 1] = title;
                let titleElement = this.eleLabel.getElementsByTagName("th");
                titleElement[position - 1].innerText = this.tableTitle[position - 1];
            }
            /**
            * @description 设置表格内容（位置从1开始）
            * @param row 行
            * @param col 列
            * @param title 标题
            */
            setContent(row, col, content) {
                this.tableContent[row - 1][col - 1] = content;
                let rowElement = this.eleLabel.getElementsByClassName("SView-table-content")[row - 1];
                let colElement = rowElement.getElementsByTagName("td")[col - 1];
                colElement.innerText = this.tableContent[row - 1][col - 1];
            }
            /**
            *  @description  添加内容表格行（位置从1开始）
            * @param position 位置
            * @param content 内容
            */
            addRow(content, position) {
                let rows = this.eleLabel.getElementsByClassName("SView-table-content");
                for (let i = 0; i < content.length; i++) {
                    this.tableContent.splice(position - 1 + i, 0, content[i]);
                    let trElement = document.createElement("tr");
                    trElement.setAttribute("class", "SView-table-content");
                    trElement.onclick = function () {
                        var nodeList = trElement.parentNode.querySelectorAll(".SView-table-content");
                        for (var f = 0; f < nodeList.length; f++) {
                            if (nodeList[f].className.indexOf("SView-table-content-active") > -1) {
                                nodeList[f].className = "SView-table-content";
                                break;
                            }
                        }
                        trElement.className = "SView-table-content SView-table-content-active";
                    };
                    for (let j = 0; j < content[i].length; j++) {
                        // 表格单元格
                        let tdElement = document.createElement("td");
                        tdElement.innerText = content[i][j];
                        trElement.appendChild(tdElement);
                    }
                    //rows[position - 1 + i].append(trElement);
                    this.eleLabel.insertBefore(trElement, rows[position - 1 + i]);
                }
            }
            /**
            *  @description 移除内容表格行（位置从1开始）
            * @param position 位置
            */
            removeRow(position) {
                let rows = this.eleLabel.getElementsByClassName("SView-table-content");
                if (rows.length > 0) {
                    if (position) {
                        this.tableContent.splice(position - 1, 1);
                        rows[position - 1].parentNode.removeChild(rows[position - 1]);
                    }
                    else {
                        var first = rows[0];
                        while (first) {
                            first.remove();
                            first = rows[0] ? rows[0] : null;
                        }
                        this.tableContent = [];
                    }
                }
            }
            /**
            *  @description 调整表格行位置（位置从1开始）
            * @param oldPosition 老位置
            * @param newPosition 新位置
            */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByClassName("SView-table-content");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.tableContent[oldPosition - 1];
                this.tableContent.splice(oldPosition - 1, 1);
                this.tableContent.splice(newPosition - 1, 0, elementInfo);
                this.eleLabel.removeChild(components[oldPosition - 1]);
                this.eleLabel.insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
            *  @description  获取表格内容
            */
            getContent() {
                return this.tableContent;
            }
            /**
            *  @description 获取表格标题
            */
            getTitle() {
                return this.tableTitle;
            }
            getActiveTr() {
                if (this.eleLabel.getElementsByClassName("SView-table-content-active").length > 0) {
                    return this.eleLabel.getElementsByClassName("SView-table-content-active")[0];
                }
                else {
                    return null;
                }
            }
        }
        Windows.Table = Table;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：标签
         *@author	ZYC
         *@date		2022-2-16
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Tabs extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.tabsTitles = [];
                this.tabsTitlesFontSize = "";
                this.tabsType = "transverse";
                this.isClick = false;
                this.tabClickFun = null;
                // 默认类名
                this.eleClasses = ["SView-tabs"];
            }
            /**
             * @description 创建标签
             * @param tabsInfo 标签
             */
            createLabel(tabsInfo) {
                let tagName = Object.keys(tabsInfo)[0];
                tabsInfo = tabsInfo[tagName];
                let divElement = document.createElement("div");
                let tabsDivElement = document.createElement("div");
                if (tabsInfo.type && tabsInfo.type != "")
                    this.tabsType = tabsInfo.type;
                tabsDivElement.setAttribute("class", "SView-" + this.tabsType + "-tabs");
                let tabsContentvElement = document.createElement("div");
                tabsContentvElement.setAttribute("class", "SView-" + this.tabsType + "-contents");
                let that = this;
                tabsContentvElement.onscroll = function () {
                    that.scrollFun(tabsContentvElement);
                };
                divElement.appendChild(tabsDivElement);
                divElement.appendChild(tabsContentvElement);
                let tabLabel = tabsInfo.tabContent;
                for (let i = 0; i < tabLabel.length; i++) {
                    // 标签页标签部分
                    let tabElement = document.createElement("div");
                    this.tabsTitles.push(tabLabel[i].title);
                    if (tabLabel[i].imgSrc && tabLabel[i].imgSrc != "") {
                        let imgEle = document.createElement("img");
                        imgEle.setAttribute("src", Windows.Label.rootPath + tabLabel[i].imgSrc);
                        tabElement.appendChild(imgEle);
                    }
                    if (tabLabel[i].isShow != null && tabLabel[i].isShow == false) {
                        tabElement.style.display = "none";
                    }
                    if (tabLabel[i].disable != null && tabLabel[i].disable == true) {
                        tabElement.style.pointerEvents = "none";
                        tabElement.style.opacity = "0.6";
                    }
                    let spanElement = document.createElement("span");
                    spanElement.innerText = tabLabel[i].title;
                    tabElement.appendChild(spanElement);
                    tabElement.setAttribute("data-class", tabLabel[i].dataClass);
                    if (tabLabel[i].dataActive == "true") {
                        tabElement.setAttribute("data-active", tabLabel[i].dataActive);
                    }
                    else {
                        tabElement.setAttribute("data-active", "false");
                    }
                    if (tabLabel[i].onClick) {
                        tabElement.setAttribute("onclick", tabLabel[i].onClick);
                    }
                    else {
                        tabElement.onclick = function (event) {
                            event.stopPropagation();
                            event.preventDefault();
                            that.scrollActiveTab(tabLabel[i].dataClass);
                        };
                    }
                    // 增加子控件（标签页内容部分）
                    let subComponents = tabLabel[i].subComponents;
                    let subComponentsDiv = null;
                    if (tabLabel[i].dataClass && tabLabel[i].dataClass != "") {
                        subComponentsDiv = document.createElement("div");
                        subComponentsDiv.setAttribute("id", tabLabel[i].dataClass);
                    }
                    for (let j = 0; j < subComponents.length; j++) {
                        let tagName = Object.keys(subComponents[j])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[j][tagName].id, newElement);
                        newElement.create(subComponents[j]);
                        let newLabel = newElement.getLabel();
                        subComponentsDiv.appendChild(newLabel);
                    }
                    if (subComponentsDiv != null) {
                        if (tabLabel[i].dataActive != "true") {
                            subComponentsDiv.style.display = "none";
                        }
                        subComponentsDiv.setAttribute("class", "SView-tabs-content");
                        tabsContentvElement.appendChild(subComponentsDiv);
                    }
                    // 判断现用标签页
                    if (tabLabel[i].dataActive == "true") {
                        tabElement.setAttribute("class", "SView-" + this.tabsType + "-tab" + " SView-tab-active");
                    }
                    else {
                        tabElement.setAttribute("class", "SView-" + this.tabsType + "-tab");
                    }
                    tabsDivElement.appendChild(tabElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            setTabClickFun(clickFun) {
                this.tabClickFun = clickFun;
            }
            /**
             * @description 设置标签标题（位置从1开始）
             * @param title 标题
             * @param position 位置
             */
            setTitle(title, position) {
                this.tabsTitles[position - 1] = title;
                let tabsTitle = this.eleLabel.getElementsByClassName("SView-" + this.tabsType + "-tab")[position - 1];
                tabsTitle.innerText = this.tabsTitles[position - 1];
            }
            /**
             * @description 获取标签标题
             */
            getTitle() {
                return this.tabsTitles;
            }
            /**
             * @description 设置标签标题字体大小
             * @param value 字体大小
             */
            setTitleFontSize(value) {
                if (typeof (value) == "number") {
                    this.tabsTitlesFontSize = value + "px";
                }
                else {
                    this.tabsTitlesFontSize = value;
                }
                let tabsTitle = this.eleLabel.getElementsByClassName("SView-" + this.tabsType + "-tab");
                for (let i = 0; i < tabsTitle.length; i++) {
                    tabsTitle[i].style.fontSize = this.tabsTitlesFontSize;
                }
            }
            getActiveTab() {
                if (this.eleLabel.getElementsByClassName("SView-tab-active").length > 0) {
                    return this.eleLabel.getElementsByClassName("SView-tab-active")[0].getAttribute("data-class");
                }
                else {
                    return null;
                }
            }
            setActiveTab(dataClass) {
                let that = this;
                let tabElement = this.eleLabel.getElementsByClassName("SView-" + this.tabsType + "-tabs")[0].children;
                for (let i = 0; i < tabElement.length; i++) {
                    let tabContent = document.getElementById(tabElement[i].getAttribute("data-class"));
                    // 更改选中标签样式
                    if (tabElement[i].getAttribute("data-class") != dataClass) {
                        tabElement[i].setAttribute("class", "SView-" + this.tabsType + "-tab");
                        tabElement[i].setAttribute("data-active", "false");
                        // 关闭该标签内容
                        tabContent.style.display = "none";
                    }
                    else {
                        tabElement[i].setAttribute("data-active", "true");
                        tabElement[i].setAttribute("class", "SView-" + this.tabsType + "-tab SView-tab-active");
                        // 显示该标签
                        tabContent.style.display = "";
                    }
                }
                if (this.tabClickFun) {
                    that.tabClickFun(dataClass);
                }
            }
            scrollActiveTab(dataClass) {
                if (this.tabsType == "vertical") {
                    this.isClick = true;
                    let item = document.getElementById(dataClass);
                    let wrapper = item.parentNode;
                    if (item.offsetTop <= wrapper.offsetTop) {
                        wrapper.scrollTop = 0;
                    }
                    else {
                        wrapper.scrollTop = item.offsetTop;
                    }
                }
                this.setActiveTab(dataClass);
            }
            scrollFun(element) {
                if (this.isClick) {
                    this.isClick = false;
                }
                else {
                    var top = element.scrollTop;
                    let childEle = element.children;
                    let scrollHeight = element.scrollHeight; // 内容总高度
                    let clientHeight = element.offsetHeight; // 容器高度
                    let dataClass;
                    for (let i = 0; i < childEle.length; i++) {
                        let offsetTop = (childEle[i].offsetTop - 43) > 0 ? (childEle[i].offsetTop - 43) : 0;
                        let offsetBot = childEle[i].offsetHeight + offsetTop;
                        if (scrollHeight == (top + clientHeight)) {
                            dataClass = childEle[childEle.length - 1].getAttribute("id");
                            this.setActiveTab(dataClass);
                            return;
                        }
                        else if (top >= offsetTop && top < offsetBot) {
                            dataClass = childEle[i].getAttribute("id");
                            this.setActiveTab(dataClass);
                            return;
                        }
                    }
                }
            }
        }
        Windows.Tabs = Tabs;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：树
         *@author WZ
         *@date		2022-2-9
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Tree extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.rightClickMenuId = "";
                this.treeRightClickMenu = {
                    RightClickMenu: {
                        id: "treeRightClickMenu",
                        classes: [
                            "SView-rightClickMenu",
                            "SView-tree-rightClickMenu"
                        ],
                        subComponents: [
                            {
                                RightClickBtn: {
                                    "id": "treeAdd",
                                    "content": "添加"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeLoad",
                                    "content": "加载"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeUninstall",
                                    "content": "卸载"
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeRemove",
                                    "content": "删除",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeCopy",
                                    "content": "复制",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeShear",
                                    "content": "剪切",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treePaste",
                                    "content": "粘贴",
                                }
                            },
                            {
                                RightClickBtn: {
                                    "id": "treeRename",
                                    "content": "重命名",
                                    "onClick": "nodeRename()"
                                }
                            }
                        ]
                    }
                };
                this.eleClasses = ["SView-tree"];
                this.treeData = [];
                this.rightClickMenuId = "treeRightClickMenu";
            }
            /**
                * @description 格式化数据
                * @param Tree 树形
                */
            dataFormat(treeInfo) {
                var b = "", c = 1;
                e(treeInfo);
                function d(d) {
                    var e = "";
                    return (e = b + "-" + d);
                }
                function e(treeInfo) {
                    var f, g;
                    for (f = 0; f < treeInfo.length; f++) {
                        g = treeInfo[f];
                        b = d(f);
                        g.group = b;
                        if (g.children.length > 0) {
                            c++;
                            e(g.children);
                        }
                        c--;
                        b = b.replace(/-\d+$/g, "");
                    }
                }
                return treeInfo;
            }
            /**
                * @description 创建树
                * @param Tree 树
                */
            createLabel(treeInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(treeInfo)[0];
                    treeInfo = treeInfo[tagName];
                    // 外层div
                    let eleDiv = document.createElement("div");
                    // 格式化数据
                    //let treeObj = this.dataFormat(treeInfo.data);
                    let eleTree = document.createElement("div");
                    eleTree.setAttribute("class", "SView-treeContent");
                    //let treeElement = this.loadTree(treeObj, eleTree);
                    //eleDiv.appendChild(treeElement);
                    // 底部菜单栏子控件数量检查
                    let treeRightClickMenu = treeInfo.treeRightClickMenu ? treeInfo.treeRightClickMenu : this.treeRightClickMenu;
                    // 增加子控件
                    let keyName = Object.keys(treeRightClickMenu)[0];
                    let newElement = new SView.Windows[keyName]();
                    this.allSubComponents.set(treeRightClickMenu[keyName].id, newElement);
                    this.rightClickMenuId = treeRightClickMenu[keyName].id;
                    newElement.create(treeRightClickMenu);
                    eleDiv.appendChild(newElement.eleLabel);
                    this.eleLabel = eleDiv;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
                * @description 生成树
                * @param Tree 树
                */
            loadTree(treeObj, element) {
                let that = this;
                var isChecked, checkedClass, nodeNum, nodeClass, html, button, image, icon, elementNode;
                for (var c = 0; c < treeObj.length; c++) {
                    isChecked = "";
                    checkedClass = "icon__round--false";
                    nodeNum = treeObj[c].group.split('0').length - 1;
                    if (treeObj[c].checked) {
                        checkedClass = "icon__round--true";
                        isChecked = "checked = true";
                    }
                    nodeClass = "SView-tree-subItem SView-tree-show";
                    html = document.createElement("div");
                    button = '<span class="SView-tree-dropdownList" ><span onclick="dropClick(this)" class="tree-open"></span></span>';
                    if (treeObj[c].children.length == 0) {
                        button = "";
                        image = "";
                    }
                    image = treeObj[c].children.length > 0
                        ? treeObj[c].img || Windows.Label.rootPath + "images/icon/icon_asm.png"
                        : treeObj[c].img || Windows.Label.rootPath + "images/icon/icon_part.png";
                    icon = '<span class="SView-tree-prefix"><img src="' + image + '"></img></span>';
                    html.innerHTML =
                        '<div class="SView-tree-item " num=' + nodeNum + '>' +
                            '<span class="leftCheckbox">' +
                            '<span class="icon__round ' +
                            checkedClass +
                            '" onclick="checkboxClick(this)"></span></span>' +
                            button +
                            '<span class="node-name" node-id="' + treeObj[c].id + '" id="SView-node-name-' + treeObj[c].id + '" onclick="nodeClick(this)" oncontextmenu="nodeRightClick(this,' + that.rightClickMenuId + ')">' +
                            icon +
                            '<input type="text" onblur="saveNodeName(' + treeObj[c].id + ')"  class="SView-node-input" id="SView-node-input-' + treeObj[c].id + '" value="' + treeObj[c].name + '"/><div class="SView-tree-name">' +
                            treeObj[c].name +
                            "</div>" +
                            '</span></div>';
                    element.appendChild(html);
                    elementNode = document.createElement("div");
                    elementNode.className = nodeClass;
                    html.appendChild(elementNode);
                    isChecked = !1;
                    nodeClass = "SView-tree-subItem";
                    if (treeObj[c].children.length > 0) {
                        this.loadTree(treeObj[c].children, elementNode);
                    }
                }
                return element;
            }
            /**
                * @description 模型显示隐藏
                * @param Tree 树
                */
            showNode(id) {
                let nodeItem = document.getElementById("SView-node-name-" + id);
                this.checkboxClick(nodeItem);
            }
            /**
                * @description 模型选中
                * @param Tree 树
                */
            selectNode(id, type) {
                let nodeItem = document.getElementById("SView-node-name-" + id);
                this.nodeClick(nodeItem, type);
            }
            // 选中
            /**
             *
             * @description 选中
             * @param node node对象
             * @param type 类型
             */
            nodeClick(node, type) {
                var nodeList = document.getElementsByClassName("SView-tree")[0].querySelectorAll(".node-name");
                for (var f = 0; f < nodeList.length; f++) {
                    nodeList[f].className = "node-name";
                }
                if (!type) {
                    node.className = "node-name tree-node-active";
                }
            }
            // 多选框选中
            /**
             *
             * @description 多选框选中
             * @param selectNode 选中对象
             */
            checkboxClick(selectNode) {
                var elementSibling, treeItemList, treeItem, checkbox;
                if (selectNode.className.indexOf("icon__round--false") > -1 || selectNode.className.indexOf("icon__round--part") > -1) {
                    elementSibling = selectNode.parentNode.parentNode.nextElementSibling;
                    treeItemList = selectNode.parentNode.parentNode.nextElementSibling.querySelectorAll(".SView-tree-item");
                    if (treeItemList.length > 0) {
                        for (var f = 0; f < treeItemList.length; f++) {
                            treeItem = treeItemList[f];
                            checkbox = treeItem.querySelector(".icon__round");
                            checkbox.className = "icon__round icon__round--true";
                        }
                    }
                    selectNode.className = "icon__round icon__round--true";
                    this.setChecked(true, selectNode.parentNode.parentNode);
                }
                else if (selectNode.className.indexOf("icon__round--true") > -1) {
                    elementSibling = selectNode.parentNode.parentNode.nextElementSibling;
                    treeItemList = selectNode.parentNode.parentNode.nextElementSibling.querySelectorAll(".SView-tree-item");
                    if (treeItemList.length > 0) {
                        for (f = 0; f < treeItemList.length; f++) {
                            treeItem = treeItemList[f];
                            checkbox = treeItem.querySelector(".icon__round");
                            checkbox.className = "icon__round icon__round--false";
                        }
                    }
                    selectNode.className = "icon__round icon__round--false";
                    this.setChecked(false, selectNode.parentNode.parentNode);
                }
            }
            // 关联选中状态
            /**
             *
             * @description 关联选中状态
             * @param type 类型
             * @param node 选中对象
             */
            setChecked(type, node) {
                var parentNode = node.parentNode.parentNode;
                var sibling = parentNode.querySelectorAll(".icon__round");
                if (type) {
                    var flag = true;
                    for (var i = 0; i < sibling.length; i++) {
                        if (sibling[i].className.indexOf("icon__round--false") > -1) {
                            flag = false;
                            break;
                        }
                    }
                    if (parentNode.previousElementSibling) {
                        if (flag) {
                            parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--true";
                        }
                        else {
                            parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--part";
                        }
                    }
                }
                else {
                    var flag = true;
                    for (var i = 0; i < sibling.length; i++) {
                        if (sibling[i].className.indexOf("icon__round--true") > -1) {
                            flag = false;
                            break;
                        }
                    }
                    if (parentNode.previousElementSibling) {
                        if (flag) {
                            parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--false";
                        }
                        else {
                            parentNode.previousElementSibling.querySelector(".icon__round").className = "icon__round icon__round--part";
                        }
                    }
                }
                if (Number(node.getAttribute("num")) > 2) {
                    this.setChecked(type, parentNode.previousElementSibling);
                }
            }
            search(...paras) {
                //待实现
                if (typeof (paras[0]) == "number") {
                    let nodeItem = document.getElementById("SView-node-name-" + paras[0]);
                    this.nodeClick(nodeItem);
                }
                else if (typeof (paras[0]) == "string") {
                    var nodeList = document.getElementsByClassName("SView-tree")[0].querySelectorAll(".node-name");
                    for (var f = 0; f < nodeList.length; f++) {
                        if (nodeList[f].querySelector(".SView-tree-name").innerHTML.indexOf(paras[0]) != -1) {
                            if (nodeList[f].className.indexOf("tree-node-active") == -1) {
                                let nodeItem = document.getElementById("SView-node-name-" + nodeList[f].getAttribute("node-id"));
                                this.nodeClick(nodeItem);
                                break;
                            }
                        }
                    }
                }
            }
            /**
             * @description 插入装配树
             * @param treeData 将要插入的数据
             * @param parent 将要插入的节点
             */
            insert(treeData, parent) {
                //待实现
            }
            /**
             * @description 获取节点的数据信息
             * @param id 节点对应的ModelId
             */
            getInfo(id) {
                //待实现
            }
            /**
             * @description 删除树结构某节点
             * @param id 节点对应的ModelId
             */
            remove(id) {
                //待实现
            }
            /**
             * @description 更新树结构某节点
             * @param id 节点对应的ModelId
             * @param treeInfo 节点对应的数据
             */
            update(id, treeInfo) {
                //待实现
            }
        }
        Windows.Tree = Tree;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：上下摇杆
         *@author	WZ
         *@date		2022-2-10
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class UpAndDown extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.mouseDownState = false;
                this.mouseOrTauch = true; //true代表mouse，false代表touch
                this.firstTouch = true; //true代表前后左右的摇杆，false代表上下的摇杆
                this.onMove = null;
                this.onMouseup = null;
                SView;
                // 默认类名
                this.eleClasses = ["joystick-topanddown"];
            }
            /**
             * @description 创建表单
             * @param formInfo 摇杆信息
             */
            createLabel(joystickInfo) {
                let tagName = Object.keys(joystickInfo)[0];
                joystickInfo = joystickInfo[tagName];
                let divElement = document.createElement("div");
                let childElement = this.createJoystick(joystickInfo);
                for (let i = 0; i < childElement.length; i++) {
                    divElement.appendChild(childElement[i]);
                }
                let that = this;
                divElement.addEventListener("mousedown", function (event) {
                    that.mouseDown(event);
                    that.mouseOrTauch = true;
                }, false);
                divElement.addEventListener("touchstart", function (event) {
                    that.mouseDown(event);
                    that.mouseOrTauch = false;
                }, false);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createJoystick(joystickInfo) {
                let childElement = [];
                let topAndDownBack = document.createElement("div");
                let topAndDownFront = document.createElement("div");
                let backImg = document.createElement("img");
                let frontImg = document.createElement("img");
                topAndDownBack.setAttribute("class", "TopAndDownBack");
                topAndDownFront.setAttribute("class", "TopAndDownFront");
                topAndDownFront.setAttribute("id", "joystickCenterImg");
                backImg.setAttribute("src", Windows.Label.rootPath + 'images/joystick_top_down.png');
                frontImg.setAttribute("src", Windows.Label.rootPath + 'images/joystick_center.png');
                topAndDownBack.appendChild(backImg);
                topAndDownFront.appendChild(frontImg);
                childElement.push(topAndDownBack);
                childElement.push(topAndDownFront);
                return childElement;
            }
            mouseDown(event) {
                let that = this;
                document.getElementById(that.eleId).style.opacity = "1";
                that.mouseDownState = true;
                let moveEvent = function (event) {
                    let parent = document.getElementById(that.eleId);
                    let div = document.getElementById("joystickCenterImg");
                    let centerx = document.getElementById("joystickCenterImg").offsetLeft + parent.offsetLeft + document.getElementById("joystickCenterImg").offsetWidth / 2; //中心点X左右
                    let centery = document.getElementById("joystickCenterImg").offsetTop + parent.offsetTop + document.getElementById("joystickCenterImg").offsetHeight / 2; //中心点Y坐标
                    let y1 = parent.offsetTop; //底层图片上边界坐标
                    let y2 = y1 + parent.offsetHeight; //底层图片下边界坐标
                    let x1 = parent.offsetLeft; //底层图片左边界图标
                    let x2 = x1 + parent.offsetWidth; //底层图片右边界图标
                    if (event.touches) {
                        if (event.touches.length === 1) {
                            that.firstTouch = false;
                        }
                        else if (event.touches.length === 2) {
                            let e1 = event.touches[0];
                            let e2 = event.touches[1];
                            if (e1.pageX >= x1 && e1.pageX <= x2 && e1.pageY >= y1 && e1.pageY <= y2) {
                                that.firstTouch = false;
                            }
                            else if (e2.pageX >= x1 && e2.pageX <= x2 && e2.pageY >= y1 && e2.pageY <= y2) {
                                that.firstTouch = true;
                            }
                        }
                    }
                    event.preventDefault();
                    event.stopPropagation();
                    let mouseY = event.clientY; //鼠标Y轴坐标
                    if (that.mouseOrTauch) {
                        mouseY = event.clientY; //鼠标Y轴坐标
                    }
                    else {
                        if (that.firstTouch) {
                            mouseY = event.touches[1].pageY; //鼠标Y轴坐标
                        }
                        else {
                            mouseY = event.touches[0].pageY; //鼠标Y轴坐标
                        }
                    }
                    let distance = 0;
                    let isUp = 3;
                    if (that.mouseDownState) {
                        //sview.joyStickOption.isJoysticPressing = true;
                        if (mouseY > centery && mouseY <= y2) {
                            div.style.transform = "matrix(1, 0, 0, 1, 0, " + (mouseY - centery) + ")";
                            distance = mouseY - centery;
                            //that.startMove(distance);
                            isUp = 1;
                        }
                        else if (mouseY > centery && mouseY > y2) {
                            distance = (y2 - y1) / 2;
                            isUp = 1;
                        }
                        else if (mouseY < centery && mouseY >= y1) {
                            div.style.transform = "matrix(1, 0, 0, 1, 0, " + -(centery - mouseY) + ")";
                            distance = centery - mouseY;
                            /*that.startMove(distance);*/
                            isUp = 0;
                        }
                        else if (mouseY < centery && mouseY < y1) {
                            distance = (y2 - y1) / 2;
                            isUp = 0;
                        }
                        //sview.SetWalkThroughUpOrDownAndSpeed(isUp, distance / ((y2 - y1) / 2));
                    }
                    if (!event.defaultPrevented) {
                        event.preventDefault();
                    }
                    if (that.onMove) {
                        that.onMove(isUp, distance / ((y2 - y1) / 2));
                    }
                };
                let mouseupEvent = function () {
                    let div = document.getElementById("joystickCenterImg");
                    document.getElementById(that.eleId).style.opacity = "0.8";
                    div.style.transform = "matrix(1, 0, 0, 1, 0, 0)";
                    that.mouseDownState = false;
                    document.removeEventListener("mousemove", moveEvent, false);
                    document.removeEventListener("mouseup", mouseupEvent, false);
                    if (that.onMouseup) {
                        that.onMouseup();
                    }
                };
                document.addEventListener("mousemove", moveEvent, false);
                document.addEventListener("mouseup", mouseupEvent, false);
                document.getElementById(that.eleId).addEventListener("touchmove", moveEvent, false);
                document.getElementById(that.eleId).addEventListener("touchend", mouseupEvent, false);
                moveEvent(event);
            }
            startMove(targetPosition) {
                var div = document.getElementById("joystickCenterImg");
                var timer = null;
                clearInterval(timer);
                var speed = 0;
                if (targetPosition < 0) {
                    speed = -10;
                }
                else {
                    speed = 10;
                }
                timer = setInterval(function () {
                    if (div.parentNode.offsetTop + 5 == targetPosition) {
                        clearInterval(timer);
                    }
                    else {
                        div.style.transform = "matrix(1, 0, 0, 1, 0, " + targetPosition + ")";
                    }
                }, 17);
            }
            /**
            * @description 设置移动事件
            * @param onMove 移动事件
            */
            setOnMove(onMove) {
                if (onMove) {
                    this.onMove = onMove;
                }
            }
            /**
            * @description 设置鼠标抬起事件
            * @param onMouseUp 抬起事件
            */
            setOnMouseup(onMouseUp) {
                if (onMouseUp) {
                    this.onMouseup = onMouseUp;
                }
            }
            setMouseOrTauch(value) {
                this.mouseOrTauch = value;
            }
            getMouseOrTauch() {
                return this.mouseOrTauch;
            }
            setMouseDownState(value) {
                this.mouseOrTauch = value;
            }
            getMouseDownState() {
                return this.mouseDownState;
            }
        }
        Windows.UpAndDown = UpAndDown;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：视图列表
         *@author WZ
         *@date		2022-3-3
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class ViewList extends Windows.Label {
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.eleClasses = ["SView-pull-left-div"];
            }
            /**
             * 创建视图列表
             * @param ViewList
             */
            createLabel(viewListInfo) {
                let tagName = Object.keys(viewListInfo)[0];
                viewListInfo = viewListInfo[tagName];
                // 外层div
                let eleDiv = document.createElement("div");
                let viewListEle = document.createElement("div");
                viewListEle.setAttribute("class", "SView-pull-left-viewList");
                var childElement = this.loadOrUpdateView(viewListInfo.subComponents);
                for (let i = 0; i < childElement.length; i++) {
                    viewListEle.appendChild(childElement[i]);
                }
                eleDiv.appendChild(viewListEle);
                this.eleLabel = eleDiv;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 获取全部列表
            */
            getAllSubComponents() {
                return this.eleLabel.querySelector(".SView-pull-left-viewList");
            }
            /**
                * @description 生成视图或更新视图
                * @param viewListInfo
                * @param id
                */
            loadOrUpdateView(subComponents, id = null) {
                var childElement = [];
                for (let i = 0; i < subComponents.length; i++) {
                    // 增加子控件
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    newElement.setParent(this);
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    childElement.push(newLabel);
                }
                if (id) {
                    //document.getElementById(id).querySelector(".SView-pull-left-viewList").innerHTML = "";
                    for (let i = 0; i < childElement.length; i++) {
                        this.eleLabel.querySelector(".SView-pull-left-viewList").appendChild(childElement[i]);
                    }
                }
                else {
                    return childElement;
                }
            }
            /**
            * @description 添加子控件
            */
            addSubComponents(subComponents) {
                for (let i = 0; i < subComponents.length; i++) {
                    // 增加子控件
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    newElement.setParent(this);
                    this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    if (this.eleLabel.querySelector(".SView-pull-left-viewList").childNodes.length > 0) {
                        this.eleLabel.querySelector(".SView-pull-left-viewList").insertBefore(newLabel, this.eleLabel.querySelector(".SView-pull-left-viewList").childNodes[0]);
                    }
                    else {
                        this.eleLabel.querySelector(".SView-pull-left-viewList").appendChild(newLabel);
                    }
                }
            }
            /**
            * @description 删除子控件
            * @param id ID
            */
            removeSubComponents(id) {
                let deleteElement = this.eleLabel.querySelector("#" + id);
                // 清空
                this.allSubComponents.set(id, null);
                // 删除
                this.allSubComponents.delete(id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 设置点击事件
             * @param onClickEvent 点击事件
             */
            setOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleOnClick = onClickEvent;
                }
            }
            /**
             * @description 设置删除点击事件
             * @param onClickEvent 点击事件
             */
            setRemoveOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleRemoveOnClick = onClickEvent;
                }
            }
            /**
             * @description 设置编辑点击事件
             * @param onClickEvent 点击事件
             */
            setEditOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleEditOnClick = onClickEvent;
                }
            }
            /**
             * @description 设置默认点击事件
             * @param onClickEvent 点击事件
             */
            setTagOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleTagOnClick = onClickEvent;
                }
            }
            onListClick(item, that, type) {
                if (type == "select") {
                    that.eleOnClick(item);
                }
                if (type == "edit") {
                    that.eleEditOnClick(item);
                }
                if (type == "remove") {
                    that.eleRemoveOnClick(item);
                }
                if (type == "tag") {
                    that.eleTagOnClick(item);
                }
            }
        }
        Windows.ViewList = ViewList;
        class ViewItem extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.viewItemData = {};
                this.parentList = null;
                this.isSelected = false;
                // 默认类名
                this.eleClasses = ["SView-pull-left-item"];
            }
            setParent(viewList) {
                this.parentList = viewList;
            }
            /**
             * @description  创建列表Item
             * @param attendeeList 列表信息
             */
            createLabel(listItem) {
                let tagName = Object.keys(listItem)[0];
                listItem = listItem[tagName];
                let divElement = document.createElement("div");
                let childElement = this.createOrUpdateViewItem(listItem);
                for (let i = 0; i < childElement.length; i++) {
                    divElement.appendChild(childElement[i]);
                }
                this.eleLabel = divElement;
                this.eleLabel.setAttribute("view-id", listItem.id);
                let that = this;
                this.eleLabel.addEventListener("click", function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    that.parentList.onListClick(that, that.parentList, "select");
                }, false);
                //this.eleLabel.setAttribute("onclick", "selectView(" + JSON.stringify(listItem.id).replace(/\"/g, "'") + ", event)");
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createOrUpdateViewItem(listItem, id = null) {
                this.viewItemData = listItem;
                let that = this;
                let childElement = [];
                let headerEle = document.createElement("div");
                let nameEle = document.createElement("div");
                let spanEle = document.createElement("span");
                let editEle = document.createElement("img");
                let viewImgEle = document.createElement("img");
                let editDivEle = document.createElement("div");
                let notagEle = document.createElement("img");
                let removeEle = document.createElement("img");
                let centerEle = document.createElement("div");
                let imgEle = document.createElement("img");
                let bottomEle = document.createElement("div");
                let creatorEle = document.createElement("span");
                let timeEle = document.createElement("span");
                nameEle.appendChild(spanEle);
                headerEle.setAttribute("class", "SView-view-item-header");
                nameEle.setAttribute("class", "SView-view-name");
                editDivEle.setAttribute("class", "SView-edit-view-name");
                if (listItem.isActivated) {
                    notagEle.setAttribute("src", Windows.Label.rootPath + 'images/icon/tag.png');
                }
                else {
                    notagEle.setAttribute("src", Windows.Label.rootPath + 'images/icon/notag.png');
                }
                notagEle.setAttribute("class", 'SView-view-item-tag');
                notagEle.addEventListener("click", function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    that.parentList.onListClick(that, that.parentList, "tag");
                }, false);
                //notagEle.setAttribute("onclick", "editView(" + JSON.stringify(listItem).replace(/\"/g, "'") + ",2, event)");
                spanEle.innerHTML = listItem.name;
                headerEle.appendChild(nameEle);
                headerEle.appendChild(editDivEle);
                childElement.push(headerEle);
                editDivEle.appendChild(notagEle);
                if (listItem.isServerView && listItem.isServerView == 1) {
                    editEle.setAttribute("src", Windows.Label.rootPath + 'images/icon/edit.png');
                    removeEle.setAttribute("src", Windows.Label.rootPath + 'images/icon/remove.png');
                    removeEle.setAttribute("class", 'SView-view-item-remove');
                    editEle.setAttribute("class", 'SView-view-item-edit');
                    nameEle.appendChild(editEle);
                    editDivEle.appendChild(removeEle);
                    removeEle.addEventListener("click", function (e) {
                        e.preventDefault();
                        e.stopPropagation();
                        that.parentList.onListClick(that, that.parentList, "remove");
                    }, false);
                    editEle.addEventListener("click", function (e) {
                        e.preventDefault();
                        e.stopPropagation();
                        that.parentList.onListClick(that, that.parentList, "edit");
                    }, false);
                    //removeEle.setAttribute("onclick", "editView(" + JSON.stringify(listItem).replace(/\"/g, "'") + ",3, event)");
                    //editEle.setAttribute("onclick", "editView(" + JSON.stringify(listItem).replace(/\"/g, "'") + ",1, event)");
                }
                if (listItem.imgSrc && listItem.imgSrc != '') {
                    centerEle.setAttribute("class", "SView-view-item-center");
                    imgEle.setAttribute("class", "SView-view_img");
                    imgEle.setAttribute("src", listItem.imgSrc);
                    centerEle.appendChild(imgEle);
                    childElement.push(centerEle);
                    bottomEle.setAttribute("class", "SView-view-item-bottom");
                    creatorEle.setAttribute("class", "SView-view_creator");
                    creatorEle.innerHTML = listItem.author;
                    timeEle.setAttribute("class", "SView-view_time");
                    timeEle.innerHTML = listItem.time;
                    bottomEle.appendChild(creatorEle);
                    bottomEle.appendChild(timeEle);
                    childElement.push(bottomEle);
                }
                else {
                    viewImgEle.setAttribute("src", Windows.Label.rootPath + 'images/rightMenuBtn/button_view.png');
                    viewImgEle.style.marginLeft = "5px";
                    nameEle.insertBefore(viewImgEle, spanEle);
                }
                if (id) {
                    document.getElementById(id).innerHTML = "";
                    for (let i = 0; i < childElement.length; i++) {
                        document.getElementById(id).appendChild(childElement[i]);
                    }
                }
                else {
                    return childElement;
                }
            }
            updateName(name) {
                if (name != "") {
                    this.viewItemData["name"] = name;
                    this.eleLabel.querySelector(".SView-view-name").querySelector("span").innerHTML = name;
                }
            }
            updateActivated(isActivated) {
                this.viewItemData["isActivated"] = isActivated;
                if (isActivated) {
                    this.eleLabel.querySelector(".SView-view-item-tag").setAttribute("src", Windows.Label.rootPath + 'images/icon/tag.png');
                }
                else {
                    this.eleLabel.querySelector(".SView-view-item-tag").setAttribute("src", Windows.Label.rootPath + 'images/icon/notag.png');
                }
            }
            setIsSelected(isSelected) {
                this.isSelected = isSelected;
                if (isSelected) {
                    this.addClass("SView-pull-left-item-active");
                }
                else {
                    this.removeClass("SView-pull-left-item-active");
                }
            }
            /**
             * @description 设置点击事件
             * @param onClickEvent 点击事件
             */
            setOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleOnClick = onClickEvent;
                    if ((typeof onClickEvent) === "string") {
                        this.eleLabel.setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        this.eleLabel.onclick = function () {
                            that.eleOnClick(that.viewItemData);
                        };
                    }
                }
            }
            /**
             * @description 设置删除点击事件
             * @param onClickEvent 点击事件
             */
            setRemoveOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleRemoveOnClick = onClickEvent;
                    if ((typeof onClickEvent) === "string") {
                        this.eleLabel.querySelector('.SView-view-item-remove').setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        this.eleLabel.querySelector('.SView-view-item-remove').onclick = function () {
                            that.eleOnClick(that.viewItemData);
                        };
                    }
                }
            }
            /**
             * @description 设置编辑点击事件
             * @param onClickEvent 点击事件
             */
            setEditOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleEditOnClick = onClickEvent;
                    if ((typeof onClickEvent) === "string") {
                        this.eleLabel.querySelector('.SView-view-item-edit').setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        this.eleLabel.querySelector('.SView-view-item-edit').onclick = function () {
                            that.eleOnClick(that.viewItemData);
                        };
                    }
                }
            }
            /**
             * @description 设置默认点击事件
             * @param onClickEvent 点击事件
             */
            setTagOnClickEvent(onClickEvent) {
                if (onClickEvent) {
                    this.eleTagOnClick = onClickEvent;
                    if ((typeof onClickEvent) === "string") {
                        this.eleLabel.querySelector('.SView-view-item-tag').setAttribute("onclick", this.eleOnClick);
                    }
                    else {
                        let that = this;
                        this.eleLabel.querySelector('.SView-view-item-tag').onclick = function () {
                            that.eleOnClick(that.viewItemData);
                        };
                    }
                }
            }
        }
        Windows.ViewItem = ViewItem;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI通用控件：左右摇杆
         *@author	WZ
         *@date		2022-2-10
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class VirtualRocker extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.mouseDownState = false;
                this.mouseOrTauch = true; //true代表mouse，false代表touch
                this.firstTouch = true; //true代表前后左右的摇杆，false代表上下的摇杆
                this.onMove = null;
                this.onMouseup = null;
                this.ahgle = 0; //角度
                this.distance = 0; //距离
                this.computeDirection = function (obj) {
                    var rAngle = obj.angle.radian;
                    //1弧度=180/pai 度 1度=pai/180 
                    var angle45 = Math.PI / 4;
                    var angle90 = Math.PI / 2;
                    var direction, directionX, directionY;
                    if (rAngle > angle45 &&
                        rAngle < (angle45 * 3)) {
                        direction = 'up';
                    }
                    else if (rAngle > -angle45 &&
                        rAngle <= angle45) {
                        direction = 'left';
                    }
                    else if (rAngle > (-angle45 * 3) &&
                        rAngle <= -angle45) {
                        direction = 'down';
                    }
                    else {
                        direction = 'right';
                    }
                    if (rAngle > -angle90 && rAngle < angle90) {
                        directionX = 'left';
                    }
                    else {
                        directionX = 'right';
                    }
                    if (rAngle > 0) {
                        directionY = 'up';
                    }
                    else {
                        directionY = 'down';
                    }
                    //if (obj.force > this.options.threshold) {
                    //    var oldDirection = {};
                    //    for (var a in this.direction)
                    //        this.direction.hasOwnProperty(a) && (u[a] = this.direction[a]);
                    //    var f = {};
                    //    this.direction = {
                    //        x: s,
                    //        y: o,
                    //        angle: i
                    //    },
                    //        e.direction = this.direction;
                    //    for (var a in u)
                    //        u[a] === this.direction[a] && (f[a] = !0);
                    //    if (f.x && f.y && f.angle)
                    //        return e;
                    //    (!f.x || !f.y) && this.trigger("plain", e),
                    //        f.x || this.trigger("plain:" + s, e),
                    //        f.y || this.trigger("plain:" + o, e),
                    //        f.angle || this.trigger("dir dir:" + i, e)
                    //}
                    return obj;
                };
                SView;
                // 默认类名
                this.eleClasses = ["SView-virtualRocker"];
            }
            /**
             * @description 创建表单
             * @param formInfo 摇杆信息
             */
            createLabel(joystickInfo) {
                let that = this;
                let tagName = Object.keys(joystickInfo)[0];
                joystickInfo = joystickInfo[tagName];
                let divElement = document.createElement("div");
                let content = document.createElement("div");
                let childElement = this.createJoystick(joystickInfo);
                for (let i = 0; i < childElement.length; i++) {
                    content.appendChild(childElement[i]);
                }
                content.setAttribute("class", "VirtualRockerContent");
                content.setAttribute("id", "VirtualRockerContent");
                divElement.appendChild(content);
                content.addEventListener("mousedown", function (event) {
                    that.mouseDown(event);
                    that.mouseOrTauch = true;
                }, false);
                content.addEventListener("touchstart", function (event) {
                    that.mouseDown(event);
                    that.mouseOrTauch = false;
                }, false);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createJoystick(joystickInfo) {
                let childElement = [];
                let VirtualRockerBack = document.createElement("div");
                let VirtualRockerFront = document.createElement("div");
                let backImg = document.createElement("img");
                let frontImg = document.createElement("img");
                VirtualRockerBack.setAttribute("class", "VirtualRockerBack");
                VirtualRockerFront.setAttribute("class", "VirtualRockerFront");
                VirtualRockerFront.setAttribute("id", "VirtualRockerImg");
                backImg.setAttribute("src", Windows.Label.rootPath + 'images/SViewrocker.png');
                frontImg.setAttribute("src", Windows.Label.rootPath + 'images/front.png');
                VirtualRockerBack.appendChild(backImg);
                VirtualRockerFront.appendChild(frontImg);
                childElement.push(VirtualRockerBack);
                childElement.push(VirtualRockerFront);
                return childElement;
            }
            mouseDown(event) {
                let contentDocument = document.getElementById("VirtualRockerContent");
                let that = this;
                that.mouseDownState = true;
                let moveEvent = function (event) {
                    var pos = {
                        x: 0,
                        y: 0
                    };
                    if (event.touches && event.touches.length > 0) {
                        if (event.touches && event.touches.length === 1) {
                            pos.x = event.touches[0].pageX;
                            pos.y = event.touches[0].pageY;
                        }
                        else if (event.touches && event.touches.length === 2) {
                        }
                    }
                    else {
                        pos.x = event.pageX;
                        pos.y = event.pageY;
                    }
                    let parent = document.getElementById("VirtualRockerContent");
                    let positionRect = parent.getBoundingClientRect();
                    let position = {
                        x: positionRect.left,
                        y: positionRect.top
                    };
                    let size = 150 / 2;
                    var dist = that.getDistance(pos, position);
                    var angle = that.angle(pos, position);
                    var rAngle = that.radians(angle);
                    if (dist > size) {
                        dist = size;
                        pos = that.findCoord(position, dist, angle);
                    }
                    var xPosition = pos.x - position.x;
                    var yPosition = pos.y - position.y;
                    let frontPosition = {
                        x: xPosition,
                        y: yPosition
                    };
                    that.applyPosition(document.getElementById("VirtualRockerImg"), frontPosition);
                    document.getElementById("VirtualRockerContent").style.opacity = "1";
                    var toSend = {
                        distance: dist,
                        angle: {
                            radian: rAngle,
                            degree: angle
                        }
                    };
                    toSend.angle = {
                        radian: that.radians(180 - angle),
                        degree: 180 - angle
                    };
                    if (that.onMove) {
                        that.onMove(toSend);
                    }
                    if (!event.defaultPrevented) {
                        event.preventDefault();
                    }
                };
                let mouseupEvent = function () {
                    event.preventDefault();
                    event.stopPropagation();
                    document.getElementById("VirtualRockerContent").style.opacity = "0.5";
                    that.mouseDownState = false;
                    let frontPosition = {
                        x: 0,
                        y: 0
                    };
                    that.applyPosition(document.getElementById("VirtualRockerImg"), frontPosition);
                    document.removeEventListener("mousemove", moveEvent, false);
                    document.removeEventListener("mouseup", mouseupEvent, false);
                    if (that.onMouseup) {
                        that.onMouseup();
                    }
                };
                document.addEventListener("mousemove", moveEvent, false);
                document.addEventListener("mouseup", mouseupEvent, false);
                document.getElementById(that.eleId).addEventListener("touchmove", moveEvent, false);
                document.getElementById(that.eleId).addEventListener("touchend", mouseupEvent, false);
                moveEvent(event);
            }
            /**
            * @description 设置移动事件
            * @param onMove 移动事件
            */
            setOnMove(onMove) {
                if (onMove) {
                    this.onMove = onMove;
                }
            }
            /**
            * @description 设置鼠标抬起事件
            * @param onMouseUp 抬起事件
            */
            setOnMouseup(onMouseUp) {
                if (onMouseUp) {
                    this.onMouseup = onMouseUp;
                }
            }
            setMouseOrTauch(value) {
                this.mouseOrTauch = value;
            }
            getMouseOrTauch() {
                return this.mouseOrTauch;
            }
            setMouseDownState(value) {
                this.mouseOrTauch = value;
            }
            getMouseDownState() {
                return this.mouseDownState;
            }
            getDistance(p1, p2) {
                const dx = p2.x - p1.x;
                const dy = p2.y - p1.y;
                return Math.sqrt((dx * dx) + (dy * dy));
            }
            ;
            angle(p1, p2) {
                const dx = p2.x - p1.x;
                const dy = p2.y - p1.y;
                return this.degrees(Math.atan2(dy, dx));
            }
            ;
            findCoord(p, d, a) {
                const b = { x: 0, y: 0 };
                a = this.radians(a);
                b.x = p.x - d * Math.cos(a);
                b.y = p.y - d * Math.sin(a);
                return b;
            }
            ;
            radians(a) {
                return a * (Math.PI / 180);
            }
            ;
            applyPosition(el, pos) {
                if (pos.top || pos.right || pos.bottom || pos.left) {
                    el.style.top = pos.top;
                    el.style.right = pos.right;
                    el.style.bottom = pos.bottom;
                    el.style.left = pos.left;
                }
                else {
                    el.style.left = pos.x + 'px';
                    el.style.top = pos.y + 'px';
                }
            }
            ;
            degrees(a) {
                return a * (180 / Math.PI);
            }
            ;
        }
        Windows.VirtualRocker = VirtualRocker;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：版本列表Item
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class About extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.productName = "SView for HTML5 Enterprise ";
                this.productVersion = "";
                this.M3DVersion = "";
                // 默认类名
                this.eleClasses = ["SView-about"];
            }
            /**
             * @description  创建列表Item
             * @param attendeeList 列表信息
             */
            createLabel(aboutInfo) {
                let tagName = Object.keys(aboutInfo)[0];
                aboutInfo = aboutInfo[tagName];
                let divElement = document.createElement("div");
                let childElement = this.createAbout(aboutInfo);
                for (let i = 0; i < childElement.length; i++) {
                    divElement.appendChild(childElement[i]);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createAbout(aboutInfo) {
                let childElement = [];
                let imgElement = document.createElement("img");
                let proElement = document.createElement("h3");
                let vElement = document.createElement("h3");
                let M3DElement = document.createElement("p");
                vElement.setAttribute("class", "SView-about-productVersion");
                M3DElement.setAttribute("class", "SView-about-M3DVersion");
                //let bottomEle = document.createElement("div");
                if (aboutInfo.productVersion && aboutInfo.productVersion !== "") {
                    this.productVersion = aboutInfo.productVersion;
                    vElement.innerHTML = "v" + this.productVersion;
                }
                if (aboutInfo.M3DVersion && aboutInfo.M3DVersion !== "") {
                    this.M3DVersion = aboutInfo.M3DVersion;
                    M3DElement.innerHTML = "M3D v" + this.M3DVersion;
                }
                if (aboutInfo.name && aboutInfo.name !== "") {
                    this.productName = aboutInfo.name;
                }
                let copyright = {
                    Copyright: {
                        id: "SView-copyright"
                    }
                };
                let tagName = Object.keys(copyright)[0];
                let newElement = new SView.Windows[tagName]();
                this.allSubComponents.set("SView-copyright", newElement);
                newElement.create(copyright);
                let newLabel = newElement.eleLabel;
                //let html = "<div class='footer'>";
                //html += "<div class='footer-gradient'>";
                //html += "<div class='copyright'>";
                //html += "<span class='copyright-dir'><span class='nowrap'>Copyright &#169; 2022<a class ='underline' href = 'http://m.hoteamsoft.com' target= '_blank'>HOTEAMSOFT</a>,All rights reserved</span></span>";
                //html += "</div>";
                //html += "<div class='copyright'>";
                //html += "<span id='all'><a class ='underline' href='https://www.hoteamsoft.com/about-us' target='_blank'> Contact Me </a></span>";
                //html += "</div></div></div>";
                imgElement.setAttribute("src", Windows.Label.rootPath + 'images/icon/sview_logo.png');
                proElement.innerText = this.productName;
                //bottomEle.innerHTML = html;
                childElement.push(imgElement);
                childElement.push(proElement);
                childElement.push(vElement);
                childElement.push(M3DElement);
                childElement.push(newLabel);
                return childElement;
            }
            setProductVersion(value) {
                this.productVersion = value;
                if (this.eleLabel.getElementsByClassName("SView-about-productVersion").length > 0)
                    this.eleLabel.getElementsByClassName("SView-about-productVersion")[0].innerHTML = "v" + this.productVersion;
            }
            setM3DVersion(value) {
                this.M3DVersion = value;
                if (this.eleLabel.getElementsByClassName("SView-about-M3DVersion").length > 0)
                    this.eleLabel.getElementsByClassName("SView-about-M3DVersion")[0].innerHTML = "M3D v" + this.M3DVersion;
            }
        }
        Windows.About = About;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：设置列表Item
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class SettingItem extends Windows.Item {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 默认类名
                this.eleClasses = ["SView-labelItem"];
            }
            getElement() {
                if (this.itemSubComponents.length == 0) {
                    return this;
                }
                if (this.itemSubComponents.length == 1) {
                    return this.itemSubComponents[0];
                }
            }
            setValue(value) {
                let ele = this.getElement();
                if (ele) {
                    ele.setValue(value);
                }
            }
            getValue() {
                let ele = this.getElement();
                if (ele) {
                    return ele.getValue();
                }
                return null;
            }
            setOnClickEvent(onclickFun) {
                let ele = this.getElement();
                if (ele) {
                    ele.setOnClickEvent(onclickFun, this);
                }
            }
            isDisabled(value) {
                let ele = this.getElement();
                if (ele) {
                    ele.isDisabled(value);
                }
            }
            setOnChangeEvent(onchangeFun) {
                let ele = this.getElement();
                if (ele) {
                    ele.setOnChangeEvent(onchangeFun, this);
                }
            }
        }
        Windows.SettingItem = SettingItem;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI基础控件：版本列表Item
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class VersionItem extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.itemTitle = "";
                this.itemContent = "";
                // 默认类名
                this.eleClasses = ["SView-VersionItem"];
            }
            /**
             * @description  创建列表Item
             * @param attendeeList 列表信息
             */
            createLabel(listItem) {
                let tagName = Object.keys(listItem)[0];
                listItem = listItem[tagName];
                let divElement = document.createElement("div");
                let childElement = this.createItem(listItem);
                for (let i = 0; i < childElement.length; i++) {
                    divElement.appendChild(childElement[i]);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            createItem(listItem) {
                let childElement = [];
                let contentElement = document.createElement("div");
                let titleElement = document.createElement("span");
                let timeElement = document.createElement("span");
                titleElement.setAttribute("class", "SView-version-title");
                timeElement.setAttribute("class", "SView-version-time");
                contentElement.setAttribute("class", "SView-version-content");
                if (listItem.content && listItem.content !== "") {
                    this.itemContent = listItem.content;
                    contentElement.innerHTML = this.itemContent;
                }
                if (listItem.title && listItem.title !== "") {
                    this.itemTitle = listItem.title;
                    titleElement.innerHTML = listItem.title;
                }
                if (listItem.time && listItem.time !== "") {
                    timeElement.innerText = listItem.time;
                }
                childElement.push(titleElement);
                childElement.push(timeElement);
                childElement.push(contentElement);
                return childElement;
            }
            getItemTitle() {
                return this.itemTitle;
            }
            setItemTitle(text) {
                this.itemTitle = text;
                if (this.eleLabel.getElementsByClassName("SView-version-title").length > 0)
                    this.eleLabel.getElementsByClassName("SView-version-title")[0].innerHTML = this.itemContent;
            }
            getItemContent() {
                return this.itemContent;
            }
            setItemContent(text) {
                this.itemContent = text;
                if (this.eleLabel.getElementsByClassName("SView-version-content").length > 0)
                    this.eleLabel.getElementsByClassName("SView-version-content")[0].innerHTML = this.itemContent;
            }
        }
        Windows.VersionItem = VersionItem;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI提示控件：消息提示框
         *@author	ZYC
         *@date		2022-2-09
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class Message extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.messageContent = "";
                this.messageContentFontSize = "";
                this.messageContentColor = "";
                this.messageBGColor = "";
                this.messageCloseBtnOnClick = "";
                this.messageCloseBtnIsShow = true; // 默认显示
                // 默认不带按钮消息提示框
                this.eleClasses = ["SView-message"];
            }
            /**
             * @description 创建消息提示
             * @param messageInfo 消息信息
             */
            createLabel(messageInfo) {
                let tagName = Object.keys(messageInfo)[0];
                messageInfo = messageInfo[tagName];
                // 是否带关闭按钮
                let messageInfoType = messageInfo.type;
                // 提示最外部边框
                let divElement = document.createElement("div");
                // 提示信息部分
                let spanElement = document.createElement("span");
                spanElement.setAttribute("class", "SView-message-content");
                if (messageInfo.messageBGColor && messageInfo.messageBGColor != "") {
                    this.messageBGColor = messageInfo.messageBGColor;
                    spanElement.style.backgroundColor = messageInfo.messageBGColor;
                }
                if (messageInfo.messageContentColor && messageInfo.messageContentColor != "") {
                    this.messageContentColor = messageInfo.messageContentColor;
                    spanElement.style.color = messageInfo.messageContentColor;
                }
                this.messageContent = messageInfo.content;
                spanElement.innerText = this.messageContent;
                divElement.appendChild(spanElement);
                // 提示按钮部分
                if (messageInfoType == "withButton") {
                    let btnElement = document.createElement("span");
                    btnElement.setAttribute("class", "SView-message-closeBtn SView-button-iconBtn");
                    this.messageCloseBtnOnClick = messageInfo.closeBtnOnClick;
                    btnElement.setAttribute("onclick", this.messageCloseBtnOnClick);
                    let imgElement = document.createElement("img");
                    imgElement.setAttribute("src", Windows.Label.rootPath + "images/meeting/MeetingExist.png");
                    btnElement.appendChild(imgElement);
                    divElement.appendChild(btnElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置消息提示内容
             * @param content 内容
             */
            setContent(content) {
                this.messageContent = content;
                let spanElement = this.eleLabel.getElementsByClassName("SView-message-content")[0];
                spanElement.innerText = this.messageContent;
            }
            /**
             * @description 设置关闭按钮点击事件
             * @param onClick 点击
             */
            setCloseBtnOnClick(onClick) {
                this.messageCloseBtnOnClick = onClick;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-message-closeBtn")[0];
                if (onClick) {
                    closeBtn.setAttribute("onclick", this.messageCloseBtnOnClick);
                }
            }
            /**
             * @description 获取消息提示内容
             */
            getContent() {
                return this.messageContent;
            }
            /**
             * @description 获取关闭按钮点击事件
             */
            getCloseBtnOnClick() {
                return this.messageCloseBtnOnClick;
            }
            /**
             * @description 设置关闭按钮显隐
             * @param isShow 是否显隐
             */
            setCloseBtnIsShow(isShow) {
                this.messageCloseBtnIsShow = isShow;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-message-closeBtn")[0];
                if (this.messageCloseBtnIsShow === true) {
                    closeBtn.style.display = "";
                }
                else {
                    closeBtn.style.display = "none";
                }
            }
            /**
             * @description 设置消息提示内容字体大小
             * @param value 内容
             */
            setContentFontSize(value) {
                if (typeof (value) == "number") {
                    this.messageContentFontSize = value + "px";
                }
                else {
                    this.messageContentFontSize = value;
                }
                let spanElement = this.eleLabel.getElementsByClassName("SView-message-content")[0];
                spanElement.style.fontSize = this.messageContentFontSize;
            }
            /**
             * @description 设置消息提示内容字体颜色
             * @param color 颜色
             */
            setContentColor(color) {
                this.messageContentColor = color;
                let spanElement = this.eleLabel.getElementsByClassName("SView-message-content")[0];
                spanElement.style.color = this.messageContentColor;
            }
            /**
             * @description 设置消息提示背景颜色
             * @param color 颜色
             */
            setBGColor(color) {
                let spanElement = this.eleLabel.getElementsByClassName("SView-message-content")[0];
                this.messageBGColor = color;
                spanElement.style.backgroundColor = this.messageBGColor;
            }
        }
        Windows.Message = Message;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI提示控件：气泡提示框
         *@author	ZYC
         *@date		2022-2-09
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class PopMessage extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.allSubComponents = new Map();
                this.messageContent = "";
                this.messageContentFontSize = "";
                this.messageContentColor = "";
                this.messageBGColor = "";
                this.messageIcon = "images/meeting/logo.png";
                this.messageIconIsShow = true; // 默认显示
                this.eleClasses = ["SView-permission-message"];
            }
            /**
             * @description 创建消息提示
             * @param messageInfo 消息信息
             */
            createLabel(messageInfo) {
                let tagName = Object.keys(messageInfo)[0];
                messageInfo = messageInfo[tagName];
                this.messageIconIsShow = messageInfo.iconIsShow ? messageInfo.iconIsShow : this.messageIconIsShow;
                this.messageIcon = messageInfo.messageIcon ? messageInfo.messageIcon : this.messageIcon;
                // 提示最外部边框
                let divElement = document.createElement("div");
                if (this.messageIconIsShow) {
                    let imgElement = document.createElement("img");
                    imgElement.setAttribute("src", Windows.Label.rootPath + this.messageIcon);
                    divElement.appendChild(imgElement);
                }
                // 提示信息部分
                let spanElement = document.createElement("span");
                spanElement.setAttribute("class", "SView-permissionMessage-content");
                this.messageContent = messageInfo.content;
                spanElement.innerHTML = this.messageContent;
                divElement.appendChild(spanElement);
                if (messageInfo.subComponents && messageInfo.subComponents.length > 0) {
                    let btnElement = document.createElement("span");
                    btnElement.setAttribute("class", "SView-permissionMessage-button");
                    let subComponents = messageInfo.subComponents;
                    for (let i = 0; i < messageInfo.subComponents.length; i++) {
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        btnElement.appendChild(newLabel);
                    }
                    divElement.appendChild(btnElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置消息提示内容
             * @param content 内容
             */
            setContent(content) {
                this.messageContent = content;
                let spanElement = this.eleLabel.getElementsByClassName("SView-permissionMessage-content")[0];
                spanElement.innerHTML = this.messageContent;
            }
            /**
             * @description 获取消息提示内容
             */
            getContent() {
                return this.messageContent;
            }
            /**
             * @description 设置关闭按钮显隐
             * @param isShow 是否显隐
             */
            setCloseBtnIsShow(isShow) {
                this.messageIconIsShow = isShow;
                let closeBtn = this.eleLabel.getElementsByTagName("img")[0];
                if (this.messageIconIsShow === true) {
                    closeBtn.style.display = "";
                }
                else {
                    closeBtn.style.display = "none";
                }
            }
            /**
             * @description 设置消息提示内容字体大小
             * @param value 内容
             */
            setContentFontSize(value) {
                if (typeof (value) == "number") {
                    this.messageContentFontSize = value + "px";
                }
                else {
                    this.messageContentFontSize = value;
                }
                let spanElement = this.eleLabel.getElementsByClassName("SView-permissionMessage-content")[0];
                spanElement.style.fontSize = this.messageContentFontSize;
            }
            /**
             * @description 设置消息提示内容字体颜色
             * @param color 颜色
             */
            setContentColor(color) {
                this.messageContentColor = color;
                let spanElement = this.eleLabel.getElementsByClassName("SView-permissionMessage-content")[0];
                spanElement.style.color = this.messageContentColor;
            }
            /**
             * @description 设置消息提示背景颜色
             * @param color 颜色
             */
            setBGColor(color) {
                let spanElement = this.eleLabel;
                this.messageBGColor = color;
                spanElement.style.backgroundColor = this.messageBGColor;
            }
        }
        Windows.PopMessage = PopMessage;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
/// <reference path="../basicComponents/Button.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        let ResultDialogIcon;
        (function (ResultDialogIcon) {
            ResultDialogIcon["SUCCESSICON"] = "success";
            ResultDialogIcon["FAILEDICON"] = "failed";
            ResultDialogIcon["LOADINGICON"] = "loading";
            ResultDialogIcon["WARNINGICON"] = "warning";
        })(ResultDialogIcon = Windows.ResultDialogIcon || (Windows.ResultDialogIcon = {}));
        /**
         *@file
         *@brief    UI提示控件：成功/失败/加载弹出提示
         *@author	ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class ResultDialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.resultImgsrc = "";
                this.resultImgClasses = "";
                this.resultTitle = "";
                this.resultDetail = "";
                this.isShowTitle = true;
                this.cancleBtStr = "";
                // 默认类名：不带按钮结果提示框
                this.eleClasses = ["SView-info"];
            }
            /**
             * @description 创建成功/失败/加载提示
             * @param resultDialogInfo 结果对话框信息
             */
            createLabel(resultDialogInfo) {
                let tagName = Object.keys(resultDialogInfo)[0];
                resultDialogInfo = resultDialogInfo[tagName];
                // 带按钮/不带按钮
                let resultDialogInfoType = resultDialogInfo.type;
                // 提示最外部边框
                let divElement = document.createElement("div");
                // 提示图标部分
                let iconDiv = document.createElement("div");
                let iconImg = document.createElement("img");
                let iconImgsrc;
                if (resultDialogInfo.icon.imgsrc && resultDialogInfo.icon.imgsrc != "") {
                    iconImgsrc = resultDialogInfo.icon.imgsrc;
                }
                else {
                    switch (resultDialogInfo.icon.type) {
                        case ResultDialogIcon.FAILEDICON:
                            iconImgsrc = "images/info/failed.png";
                            break;
                        case ResultDialogIcon.LOADINGICON:
                            iconImgsrc = "images/info/loading.gif";
                            break;
                        case ResultDialogIcon.WARNINGICON:
                            iconImgsrc = "images/info/warning.png";
                            break;
                        default:
                            iconImgsrc = "images/info/success.png";
                    }
                }
                iconImg.setAttribute("src", Windows.Label.rootPath + iconImgsrc);
                let iconImgClass = resultDialogInfo.icon.imgClasses;
                if (iconImgClass != null) {
                    this.resultImgClasses = resultDialogInfo.icon.imgClasses.join(" ");
                    iconImg.setAttribute("class", this.resultImgClasses);
                }
                iconDiv.setAttribute("class", "SView-info-icon");
                iconDiv.appendChild(iconImg);
                divElement.appendChild(iconDiv);
                // 提示标题部分
                let titleP = document.createElement("p");
                this.resultTitle = resultDialogInfo.titleContent;
                titleP.setAttribute("class", "SView-info-title");
                titleP.innerText = this.resultTitle;
                this.isShowTitle = resultDialogInfo.isShowTitle == false ? false : true;
                if (this.isShowTitle)
                    divElement.appendChild(titleP);
                // 提示详细描述部分
                let detailP = document.createElement("p");
                this.resultDetail = resultDialogInfo.detailContent;
                detailP.setAttribute("class", "SView-info-detail");
                detailP.innerText = this.resultDetail;
                divElement.appendChild(detailP);
                // 提示按钮部分
                if (resultDialogInfoType == "withButton") {
                    let button = new Windows.Button();
                    this.allSubComponents.set(resultDialogInfo.button.Button.id, button);
                    button.create(resultDialogInfo.button);
                    let newLabel = button.eleLabel;
                    divElement.appendChild(newLabel);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置图标类名
             * @param imgClasses 图标类名
             */
            setImgClasses(imgClasses) {
                if (typeof (imgClasses) == "string") {
                    this.resultImgClasses = imgClasses;
                }
                else {
                    this.resultImgClasses = imgClasses.join(" ");
                }
                let imgDiv = this.eleLabel.getElementsByClassName("SView-info-icon")[0];
                let img = imgDiv.getElementsByTagName("img")[0];
                img.setAttribute("class", this.resultImgClasses);
            }
            /**
             * @description 设置图标路径
             * @param imgsrc 图标路径
             */
            setImgsrc(imgsrc) {
                this.resultImgsrc = imgsrc;
                let imgDiv = this.eleLabel.getElementsByClassName("SView-info-icon")[0];
                let img = imgDiv.getElementsByTagName("img")[0];
                img.setAttribute("src", Windows.Label.rootPath + this.resultImgsrc);
            }
            /**
             * @description 设置提示标题
             * @param title 标题
             */
            setTitle(title) {
                this.resultTitle = title;
                let titleElement = this.eleLabel.getElementsByClassName("SView-info-title")[0];
                titleElement.innerText = this.resultTitle;
            }
            /**
             * @description 设置提示细节描述
             * @param detail 细节
             */
            setDetail(detail) {
                this.resultDetail = detail;
                let detailElement = this.eleLabel.getElementsByClassName("SView-info-detail")[0];
                detailElement.innerText = this.resultDetail;
            }
            /**
             * @description 获取提示图标路径
             */
            getImgsrc() {
                return this.resultImgsrc;
            }
            /**
            * @description 设置取消按键
            * @param btStr 取消按键内容
            */
            setCancleBt(btStr) {
                this.cancleBtStr = btStr;
                let button = new Windows.Button();
                this.allSubComponents.set(btStr.Button.id, button);
                button.create(btStr);
                let newLabel = button.eleLabel;
                this.eleLabel.appendChild(newLabel);
            }
            /**
             * @description 获取提示图标类名
             */
            getImgClasses() {
                return this.resultImgClasses;
            }
            /**
             * @description 获取提示标题
             */
            getTitle() {
                return this.resultTitle;
            }
            /**
             * @description 获取提示细节
             */
            getDetail() {
                return this.resultDetail;
            }
        }
        Windows.ResultDialog = ResultDialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：参会人列表
         *@author WZ
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class AttendeeList extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                // 默认类名
                this.eleClasses = ["SView-attendeeList"];
            }
            /**
             * @description  创建参会人列表
             * @param attendeeList 参会人信息
             */
            createLabel(attendeeList) {
                let tagName = Object.keys(attendeeList)[0];
                attendeeList = attendeeList[tagName];
                let divElement = document.createElement("div");
                // 参会人列表按钮部分
                let buttonElement = document.createElement("ul");
                buttonElement.setAttribute("class", "SView-attendee-button");
                // 按钮信息
                let topBtns = attendeeList.topBottons;
                for (let i = 0; i < topBtns.length; i++) {
                    let eleLi = document.createElement("li");
                    buttonElement.appendChild(eleLi);
                    let tagName = Object.keys(topBtns[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(topBtns[i][tagName].id, newElement);
                    newElement.create(topBtns[i]);
                    let newLabel = newElement.eleLabel;
                    eleLi.appendChild(newLabel);
                }
                divElement.appendChild(buttonElement);
                // 参会人列表
                let contentElement = document.createElement("ul");
                contentElement.setAttribute("class", "SView-attendee-content");
                let attendeeInfo = attendeeList.attendeeInfo;
                for (let detail of attendeeInfo) {
                    let elDetailLi = document.createElement("li");
                    elDetailLi.setAttribute("data-userid", detail.memuserid);
                    let eleLabel = document.createElement("label");
                    eleLabel.innerHTML = detail.membernick;
                    elDetailLi.appendChild(eleLabel);
                    if (detail.buttons && detail.buttons.length > 0) {
                        let eleButtons = document.createElement("span");
                        for (let i = 0; i < detail.buttons.length; i++) {
                            let eleSpan = document.createElement("span");
                            eleButtons.appendChild(eleSpan);
                            // 增加子控件
                            let tagName = Object.keys(detail.buttons[i])[0];
                            let newElement = new SView.Windows[tagName]();
                            this.allSubComponents.set(detail.buttons[i][tagName].id, newElement);
                            newElement.create(detail.buttons[i]);
                            let newLabel = newElement.eleLabel;
                            eleSpan.appendChild(newLabel);
                        }
                        elDetailLi.appendChild(eleButtons);
                    }
                    contentElement.appendChild(elDetailLi);
                }
                divElement.appendChild(contentElement);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
        }
        Windows.AttendeeList = AttendeeList;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
/// <reference path="../basicComponents/Dialog.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：属性弹框
         *@author WZ
         *@date		2022-2-8
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class AttributeDialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
            }
            /**
             * 创建属性弹框
             * @param attributeDialog
             */
            createLabel(attributeDialog) {
                let tagName = Object.keys(attributeDialog)[0];
                let dialogElement = new Windows.Dialog();
                this.allSubComponents.set(attributeDialog[tagName].id, dialogElement);
                let dialog = { Dialog: attributeDialog.AttributeDialog };
                dialogElement.create(dialog);
                let labelElement = dialogElement.eleLabel;
                let attributeInfo = attributeDialog[tagName].attributeData;
                for (let i = 0; i < attributeInfo.length; i++) {
                    let tagName = Object.keys(attributeInfo[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(attributeInfo[i][tagName].id, newElement);
                    newElement.create(attributeInfo[i]);
                    labelElement.getElementsByClassName("SView-dialog-content")[0].appendChild(newElement.eleLabel);
                }
                this.eleLabel = labelElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
        }
        Windows.AttributeDialog = AttributeDialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：底部菜单栏
         *@author ZYC
         *@date		2022-2-09
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class BottomMenu extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.bottomMenuSubComponents = [];
                // 默认类名
                this.eleClasses = ["SView-bottomMenu"];
            }
            /**
             * @description 创建底部菜单栏
             * @param bottomMenuInfo 底部按钮信息
             */
            createLabel(bottomMenuInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(bottomMenuInfo)[0];
                    bottomMenuInfo = bottomMenuInfo[tagName];
                    let divElement = document.createElement("div");
                    let eleUl = document.createElement("ul");
                    divElement.appendChild(eleUl);
                    if (bottomMenuInfo.number != null || bottomMenuInfo.number != 0) {
                        // 底部菜单栏子控件数量检查
                        let subComponents = bottomMenuInfo.subComponents;
                        for (let i = 0; i < subComponents.length; i++) {
                            this.bottomMenuSubComponents.push(subComponents[i]);
                            let eleLi = document.createElement("li");
                            eleUl.appendChild(eleLi);
                            // 增加子控件
                            let tagName = Object.keys(subComponents[i])[0];
                            let newElement = new SView.Windows[tagName]();
                            this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                            newElement.create(subComponents[i]);
                            let newLabel = newElement.eleLabel;
                            eleLi.appendChild(newLabel);
                        }
                    }
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加底部菜单栏子控件（位置从1开始）
             * @param subComponents 子控件
             * @param position 位置
             */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("li");
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    let eleLi = document.createElement("li");
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.bottomMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.bottomMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    eleLi.appendChild(newLabel);
                    this.eleLabel.getElementsByTagName("ul")[0].insertBefore(eleLi, components[position - 1 + i]);
                }
            }
            /**
             * @description 调整底部菜单栏子控件位置（位置从1开始）
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByTagName("li");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.bottomMenuSubComponents[oldPosition - 1];
                this.bottomMenuSubComponents.splice(oldPosition - 1, 1);
                this.bottomMenuSubComponents.splice(newPosition - 1, 0, elementInfo);
                components[oldPosition - 1].parentNode.removeChild(components[oldPosition - 1]);
                this.eleLabel.getElementsByTagName("ul")[0].insertBefore(changeElement, components[newPosition - 1]);
            }
            /**
             * @description 移除底部菜单栏子控件（位置从1开始）
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.getElementsByTagName("li");
                let deleteElement = components[position - 1];
                this.bottomMenuSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
             * @description 获取底部菜单栏子控件
             */
            getSubComponents() {
                return this.bottomMenuSubComponents;
            }
        }
        Windows.BottomMenu = BottomMenu;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
/// <reference path="../basicComponents/Button.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：会议弹窗
         *@author ZYC
         *@date		2022-2-16
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
        */
        class MeetingDialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.meetingDialogTitleBtn = [];
                this.eleClasses = ["SView-meeting"];
            }
            /**
             * @description 创建会议弹框
             * @param meetingDialogInfo 会议对话信息
             */
            createLabel(meetingDialogInfo) {
                let tagName = Object.keys(meetingDialogInfo)[0];
                meetingDialogInfo = meetingDialogInfo[tagName];
                let divElement = document.createElement("div");
                // 会议弹窗标题部分
                let titleElement = document.createElement("div");
                titleElement.setAttribute("class", "SView-meeting-title");
                // 标题中所含图标按钮
                let titleBtns = meetingDialogInfo.titleBtns;
                for (let i = 0; i < titleBtns.length; i++) {
                    this.meetingDialogTitleBtn.push(titleBtns[i]);
                    let newElement = new Windows.Button();
                    this.allSubComponents.set(titleBtns[i].Button.id, newElement);
                    newElement.create(titleBtns[i]);
                    let newLabel = newElement.eleLabel;
                    titleElement.appendChild(newLabel);
                }
                divElement.appendChild(titleElement);
                // 会议弹窗中内容部分
                let contentElement = document.createElement("div");
                contentElement.setAttribute("class", "SView-meeting-content");
                // 内容中所含图片按钮
                let imgBtns = meetingDialogInfo.images;
                let otherElement = document.createElement("div");
                otherElement.setAttribute("class", "SView-meeting-other");
                for (let i = 0; i < imgBtns.length; i++) {
                    let imgDivElement = document.createElement("div");
                    imgDivElement.setAttribute("class", imgBtns[i].classes.join(" "));
                    imgDivElement.setAttribute("onclick", imgBtns[i].onClick);
                    let imgElement = document.createElement("img");
                    imgElement.setAttribute("src", Windows.Label.rootPath + imgBtns[i].imgsrc);
                    imgDivElement.appendChild(imgElement);
                    if (i == 0) {
                        contentElement.appendChild(imgDivElement);
                    }
                    else {
                        otherElement.appendChild(imgDivElement);
                    }
                }
                contentElement.appendChild(otherElement);
                divElement.appendChild(contentElement);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 添加标题处按钮（位置从1开始）
             * @param titleBtns 标题按钮
             * @param position 位置
             */
            addTitleBtns(titleBtns, position) {
                let components = this.eleLabel.getElementsByClassName("SView-meeting-title")[0].children;
                for (let i = 0; i < titleBtns.length; i++) {
                    this.meetingDialogTitleBtn.splice(position - 1 + i, 0, titleBtns[i]);
                    let newElement = new Windows.Button();
                    this.allSubComponents.set(titleBtns[i].Button.id, newElement);
                    newElement.create(titleBtns[i]);
                    components[position - 1 + i].append(newElement.eleLabel);
                }
            }
            /**
             * @description 调整标题处按钮位置（位置从1开始）
             * @param oldPosition 老位置
             * @param newPosition 新位置
             */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.getElementsByClassName("SView-meeting-title")[0].children;
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.meetingDialogTitleBtn[oldPosition - 1];
                this.meetingDialogTitleBtn.splice(oldPosition - 1, 1);
                this.meetingDialogTitleBtn.splice(newPosition - 1, 0, elementInfo);
                this.eleLabel.removeChild(components[oldPosition - 1]);
                components[newPosition - 1].append(changeElement);
            }
            /**
             * @description 移除标题处按钮（位置从1开始）
             * @param position 位置
             */
            removeSubComponents(position) {
                let components = this.eleLabel.getElementsByClassName("SView-meeting-title")[0].children;
                let deleteElement = components[position - 1];
                this.meetingDialogTitleBtn.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
        }
        Windows.MeetingDialog = MeetingDialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：右侧菜单栏
         *@author ZYC
         *@date		2022-1-24
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class RightMenu extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // private eleButtons: Button[];
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.rightMenuSubComponents = [];
                // 默认类名
                this.eleClasses = ["SView-rightMenu"];
            }
            /**
             * @description  创建右侧菜单栏
             * @param rightMenuInfo 右侧菜单信息
             */
            createLabel(rightMenuInfo) {
                let tagName = Object.keys(rightMenuInfo)[0];
                rightMenuInfo = rightMenuInfo[tagName];
                let divElement = document.createElement("div");
                let eleUl = document.createElement("ul");
                divElement.appendChild(eleUl);
                if (rightMenuInfo.number != null || rightMenuInfo.number != 0) {
                    // 创建右侧菜单栏按钮
                    let subComponents = rightMenuInfo.subComponents;
                    for (let i = 0; i < rightMenuInfo.subComponents.length; i++) {
                        this.rightMenuSubComponents.push(rightMenuInfo.subComponents[i]);
                        let eleLi = document.createElement("li");
                        eleUl.appendChild(eleLi);
                        // 增加子控件
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        let newLabel = newElement.eleLabel;
                        eleLi.appendChild(newLabel);
                    }
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 新增菜单栏子控件
            * @param subComponents 子控件
            * @param position
            */
            addSubComponents(subComponents, position) {
                let components = this.eleLabel.getElementsByTagName("li");
                for (let i = 0; i < subComponents.length; i++) {
                    let newLabel;
                    let eleLi = document.createElement("li");
                    if (subComponents[i].constructor.name != "Object") {
                        this.allSubComponents.set(subComponents[i].eleId, subComponents[i]);
                        newLabel = subComponents[i].eleLabel;
                        this.rightMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                    }
                    else {
                        this.rightMenuSubComponents.splice(position - 1 + i, 0, subComponents[i]);
                        /*components[position - 1 + i].append(eleLi);*/
                        // 增加子控件
                        let tagName = Object.keys(subComponents[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                        newElement.create(subComponents[i]);
                        newLabel = newElement.eleLabel;
                    }
                    eleLi.appendChild(newLabel);
                    this.eleLabel.getElementsByTagName("ul")[0].insertBefore(eleLi, components[position - 1 + i]);
                }
            }
            /**
            * @description 调整菜单栏子控件位置
            * @param oldPosition
            * @param newPosition
            */
            changePosition(oldPosition, newPosition) {
                let components = this.eleLabel.querySelectorAll("li");
                let changeElement = components[oldPosition - 1];
                let elementInfo = this.rightMenuSubComponents[oldPosition - 1];
                this.rightMenuSubComponents.splice(oldPosition - 1, 1);
                this.rightMenuSubComponents.splice(newPosition - 1, 0, elementInfo);
                components[oldPosition - 1].parentNode.removeChild(components[oldPosition - 1]);
                this.eleLabel.getElementsByTagName("ul")[0].insertBefore(changeElement, components[newPosition - 1]);
                /*components[newPosition - 1].append(changeElement);*/
            }
            /**
            * @description 调整菜单栏子控件
            * @param position 位置
            */
            removeSubComponents(position) {
                let components = this.eleLabel.querySelectorAll("li");
                let deleteElement = components[position - 1];
                this.rightMenuSubComponents.splice(position - 1, 1);
                // 清空
                this.allSubComponents.set(deleteElement.id, null);
                // 删除
                this.allSubComponents.delete(deleteElement.id);
                // 移除标签
                deleteElement.parentNode.removeChild(deleteElement);
            }
            /**
            * @description 获取菜单栏子控件
            */
            getSubComponents() {
                return this.rightMenuSubComponents;
            }
        }
        Windows.RightMenu = RightMenu;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         * @description 设置对话框
         * */
        class SettingDialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map(); // 所有子控件集合1
            }
        }
        Windows.SettingDialog = SettingDialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    UI专用控件：装配树弹窗
         *@author	ZYC
         *@date		2022-2-19
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class TreeDialog extends Windows.Label {
            /**
             *  构造函数
             * */
            constructor() {
                super();
                // 所有子控件集合
                this.allSubComponents = new Map();
                this.treeDialogCloseBtnOnClick = "";
                this.treeDialogCloseBtnIsShow = true; // 默认显示
                // 默认类名
                this.eleClasses = ["SView-treeDialog"];
            }
            /**
             * @description 创建装配树标签页
             * @param  treeDialogInfo 树形对象信息
             * @returns TreeDialog对象
             */
            createLabel(treeDialogInfo) {
                let tagName = Object.keys(treeDialogInfo)[0];
                treeDialogInfo = treeDialogInfo[tagName];
                let divElement = document.createElement("div");
                //------------------- 标题部分 ------------------------
                let titleElement = document.createElement("div");
                let titleInfo = treeDialogInfo.title;
                if (titleInfo.classes && titleInfo.classes.length > 0) {
                    titleElement.setAttribute("class", titleInfo.classes.join(" "));
                }
                else {
                    titleElement.setAttribute("class", "SView-treeDialog-title");
                }
                divElement.appendChild(titleElement);
                // 标题标签页部分
                let tabsElement = document.createElement("div");
                tabsElement.setAttribute("class", "SView-treeDialog-tabs");
                tabsElement.setAttribute("id", titleInfo.id);
                titleElement.appendChild(tabsElement);
                // 标签页按钮
                for (let i = 0; i < titleInfo.tabContent.length; i++) {
                    let tagName = Object.keys(titleInfo.tabContent[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(titleInfo.tabContent[i][tagName].id, newElement);
                    newElement.create(titleInfo.tabContent[i]);
                    let newLabel = newElement.eleLabel;
                    newLabel.setAttribute("data-class", titleInfo.tabContent[i][tagName].dataClass);
                    tabsElement.appendChild(newLabel);
                }
                // 标题关闭图标
                let closeElement = document.createElement("div");
                closeElement.setAttribute("class", "SView-treeDialog-closeBtn");
                titleElement.appendChild(closeElement);
                let btnElement = document.createElement("div");
                btnElement.setAttribute("class", "SView-button-iconBtn treeExit");
                if (titleInfo.closeBtnIsShow || titleInfo.closeBtnIsShow == true) {
                    this.treeDialogCloseBtnIsShow = true;
                    closeElement.style.display = "";
                }
                else if (titleInfo.closeBtnIsShow == false) {
                    this.treeDialogCloseBtnIsShow = false;
                    closeElement.style.display = "none";
                }
                this.treeDialogCloseBtnOnClick = titleInfo.closeBtnOnClick;
                btnElement.setAttribute("onclick", this.treeDialogCloseBtnOnClick);
                closeElement.appendChild(btnElement);
                let btnImgElement = document.createElement("img");
                btnImgElement.setAttribute("src", Windows.Label.rootPath + "images/meeting/MeetingExist.png");
                btnElement.appendChild(btnImgElement);
                //------------------- 搜索框部分 ------------------------
                let searchInfo = treeDialogInfo.search;
                let searchElement = document.createElement("div");
                searchElement.setAttribute("class", "SView-treeDialog-search");
                divElement.appendChild(searchElement);
                for (let i = 0; i < searchInfo.subComponents.length; i++) {
                    let tagName = Object.keys(searchInfo.subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    this.allSubComponents.set(searchInfo.subComponents[i][tagName].id, newElement);
                    newElement.create(searchInfo.subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    searchElement.appendChild(newLabel);
                }
                //--------------------- 内容部分 -----------------------
                let contentElement = document.createElement("div");
                contentElement.setAttribute("class", "SView-treeDialog-content");
                divElement.appendChild(contentElement);
                let scrollDivElement = null;
                for (let key in treeDialogInfo) {
                    let keyName = key.split("_")[0];
                    if (keyName == "content") {
                        let contentDivElement = document.createElement("div");
                        contentDivElement.setAttribute("id", treeDialogInfo[key].id);
                        contentElement.appendChild(contentDivElement);
                        // 判断是否显示当前内容
                        if (treeDialogInfo[key].dataActive == "true") {
                            contentDivElement.style.display = "";
                            contentDivElement.setAttribute("data-active", "true");
                        }
                        else {
                            contentDivElement.style.display = "none";
                            contentDivElement.setAttribute("data-activc", "false");
                        }
                        for (let j = 0; j < treeDialogInfo[key].subComponents.length; j++) {
                            let tagName = Object.keys(treeDialogInfo[key].subComponents[j])[0];
                            if (tagName == "Tree") {
                                scrollDivElement = document.createElement("div");
                                scrollDivElement.setAttribute("class", "SView-tree-scroll");
                                contentDivElement.appendChild(scrollDivElement);
                            }
                            let newElement = new SView.Windows[tagName]();
                            newElement.create(treeDialogInfo[key].subComponents[j]);
                            this.allSubComponents.set(treeDialogInfo[key].subComponents[j][tagName].id, newElement);
                            let newLabel = newElement.eleLabel;
                            contentDivElement.appendChild(newLabel);
                        }
                        contentElement.appendChild(contentDivElement);
                    }
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
             * @description 设置装配树关闭按钮点击事件
             * @param  onClick 点击
             */
            setCloseBtnOnClick(onClick) {
                this.treeDialogCloseBtnOnClick = onClick;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-treeDialog-closeBtn")[0].getElementsByTagName("div")[0];
                closeBtn.setAttribute("onclick", this.treeDialogCloseBtnOnClick);
            }
            /**
             * @description 设置装配树关闭按钮显隐
             * @param  isShow 是否显隐
             */
            setCloseBtnIsShow(isShow) {
                this.treeDialogCloseBtnIsShow = isShow;
                let closeBtn = this.eleLabel.getElementsByClassName("SView-treeDialog-closeBtn")[0];
                if (this.treeDialogCloseBtnIsShow == true) {
                    closeBtn.style.display = "";
                }
                else if (this.treeDialogCloseBtnIsShow == false) {
                    closeBtn.style.display = "none";
                }
            }
            /**
             * @description 获取装配树关闭按钮点击事件
             */
            getCloseBtnOnClick() {
                return this.treeDialogCloseBtnOnClick;
            }
        }
        Windows.TreeDialog = TreeDialog;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    会议视频窗口
         *@author	ZYC
         *@date		2022-2-19
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class VideoDialog extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.allSubComponents = new Map();
                this.eleClasses = ["SView-meetingVideo"];
            }
            /**
             * @description 会议视频窗口
             * @param meetingVideoInfo 会议视频信息
             */
            createLabel(meetingVideoInfo) {
                let tagName = Object.keys(meetingVideoInfo)[0];
                meetingVideoInfo = meetingVideoInfo[tagName];
                let divElement = document.createElement("div");
                let leftIcon = document.createElement("div");
                let rightVideo = document.createElement("div");
                leftIcon.setAttribute("class", "SView-close-icon");
                leftIcon.onclick = function () {
                    if (leftIcon.className == "SView-close-icon") {
                        leftIcon.className = "SView-open-icon";
                        rightVideo.style.display = "none";
                    }
                    else {
                        leftIcon.className = "SView-close-icon";
                        rightVideo.style.display = "flex";
                    }
                };
                rightVideo.setAttribute("class", "SView-video-list");
                let subComponents = meetingVideoInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    if (subComponents[i][tagName].id) {
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    }
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    rightVideo.appendChild(newLabel);
                }
                divElement.appendChild(leftIcon);
                divElement.appendChild(rightVideo);
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
            /**
            * @description 新增列表子控件
            * @param subComponents 子控件
            * @param position
            */
            addSubComponents(subComponents, position) {
                let rightVideo = this.eleLabel.getElementsByClassName("SView-video-list")[0];
                for (let i = 0; i < subComponents.length; i++) {
                    let tagName = Object.keys(subComponents[i])[0];
                    let newElement = new SView.Windows[tagName]();
                    if (subComponents[i][tagName].id) {
                        this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                    }
                    newElement.create(subComponents[i]);
                    let newLabel = newElement.eleLabel;
                    rightVideo.appendChild(newLabel);
                }
            }
        }
        Windows.VideoDialog = VideoDialog;
        /**
         * @description选项
         * */
        class VideoItem extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.allSubComponents = new Map();
                this.eleClasses = ["SView-video-item"];
            }
            /**
             * @description 创建视频Item
             * @param videoInfo  视频信息
             */
            createLabel(videoInfo) {
                return new Promise((resolve, reject) => {
                    let tagName = Object.keys(videoInfo)[0];
                    videoInfo = videoInfo[tagName];
                    let divElement = document.createElement("div");
                    if (videoInfo.subComponents && videoInfo.subComponents.length > 0) {
                        let subComponents = videoInfo.subComponents;
                        for (let i = 0; i < subComponents.length; i++) {
                            let tagName = Object.keys(subComponents[i])[0];
                            let newElement = new SView.Windows[tagName]();
                            if (subComponents[i][tagName].id) {
                                this.allSubComponents.set(subComponents[i][tagName].id, newElement);
                            }
                            newElement.create(subComponents[i]);
                            divElement.appendChild(newElement.eleLabel);
                        }
                    }
                    let nameElement = document.createElement("div");
                    nameElement.setAttribute("class", "SView-video-name");
                    if (videoInfo.name && videoInfo.name != "")
                        nameElement.innerText = videoInfo.name;
                    let videoElement = document.createElement("div");
                    videoElement.setAttribute("class", "SView-video-body");
                    videoElement.setAttribute("id", videoInfo.id + "_mask");
                    videoElement.style.display = "none";
                    divElement.appendChild(nameElement);
                    divElement.appendChild(videoElement);
                    this.eleLabel = divElement;
                    resolve(this);
                    reject("创建失败");
                });
            }
        }
        Windows.VideoItem = VideoItem;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
/// <reference path="../classLabels/Label.ts" />
var SView;
(function (SView) {
    let Windows;
    (function (Windows) {
        /**
         *@file
         *@brief    视图菜单栏
         *@author	ZYC
         *@date		2022-2-19
         *@version	1.0
         *@Copyright All Rights Reserved. Copyright (C) 2022 HOTEAMSOFT, Ltd
         */
        class ViewMenu extends Windows.Label {
            /**
             * @description 构造函数
             * */
            constructor() {
                super();
                this.allSubComponents = new Map();
                this.eleClasses = ["SView-viewMenu"];
            }
            /**
             * @description 增加视图菜单栏
             * @param viewMenuInfo 视图菜单信息
             */
            createLabel(viewMenuInfo) {
                let tagName = Object.keys(viewMenuInfo)[0];
                viewMenuInfo = viewMenuInfo[tagName];
                let divElement = document.createElement("div");
                let subComponents = viewMenuInfo.subComponents;
                for (let i = 0; i < subComponents.length; i++) {
                    let subDivElement = document.createElement("div");
                    subDivElement.setAttribute("class", subComponents[i].classes);
                    let ulElement = document.createElement("ul");
                    subDivElement.appendChild(ulElement);
                    let subButtons = subComponents[i].buttons;
                    for (let j = 0; j < subButtons.length; j++) {
                        let liElement = document.createElement("li");
                        let tagName = Object.keys(subButtons[i])[0];
                        let newElement = new SView.Windows[tagName]();
                        if (subButtons[j][tagName].id) {
                            this.allSubComponents.set(subButtons[j][tagName].id, newElement);
                        }
                        newElement.create(subButtons[j]);
                        liElement.appendChild(newElement.eleLabel);
                        ulElement.appendChild(liElement);
                    }
                    divElement.appendChild(subDivElement);
                }
                this.eleLabel = divElement;
                return new Promise((resolve, reject) => {
                    resolve(this);
                    reject("Failed!");
                });
            }
        }
        Windows.ViewMenu = ViewMenu;
    })(Windows = SView.Windows || (SView.Windows = {}));
})(SView || (SView = {}));
//# sourceMappingURL=SViewControls.js.map