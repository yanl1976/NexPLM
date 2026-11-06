var treeSerchInput = {
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
                    enterKey: true,
                    placeHolder: SView[SView.UIManager.languageInfo].languageObj.TextNote.InputContent,
                    autoComplete: "off"
                },
            },
            {
                InputButton: {
                    id: "searchBtn",
                    onClick: "checkLength('searchContentInput','assemblyTreeDialog')",
                    imgsrc: "images/search_white.png",
                    btnTitle: "",
                    warning: SView[SView.UIManager.languageInfo].languageObj.TextNote.InputContent
                }
            }
        ]
    }
}
var viewSerchInput = {
    WithBtnInput: {
        id: "searchViewInput",
        classes: [
            "SView-input-withBtn"
        ],
        isShow: false,
        subComponents: [
            {
                Input: {
                    id: "searchViewContentInput",
                    classes: [
                        "SView-complexInput"
                    ],
                    value: "",
                    enterKey: true,
                    placeHolder: SView[SView.UIManager.languageInfo].languageObj.TextNote.InputContent,
                    autoComplete: "off"
                },
            },
            {
                InputButton: {
                    id: "searchViewBtn",
                    onClick: "searchView('searchViewContentInput','sview_view_panel')",
                    imgsrc: "images/search_white.png",
                    btnTitle: "",
                    warning: SView[SView.UIManager.languageInfo].languageObj.TextNote.InputContent
                }
            }
        ]
    }
}
var AssemblyDialog = {
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
            closeBtnOnClick: "closeAssemblyDialog('assemblyTreeDialog', 'assemblyTree')",
            tabContent: [
                {
                    Button: {
                        dataClass: "SView-assemblyTree",
                        id: "assemblyTreeBtn",
                        imgsrc: "images/rightMenuBtn/sview_assembly.png",
                        title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.assembly,
                        onClick: "changeTreeTabs('treeDialogTabs','SView-assemblyTree')",
                        highLight: true
                    }
                },
                {
                    Button: {
                        dataClass: "SView-viewList",
                        id: "viewListBtn",
                        //isDisabled: true,
                        imgsrc: "images/rightMenuBtn/button_view.png",
                        title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.ViewList,
                        onClick: "changeTreeTabs('treeDialogTabs', 'SView-viewList')",
                    }
                }
            ],
        },
        search: {
            subComponents: [
                treeSerchInput,
                viewSerchInput
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
                                            "content": SView[SView.UIManager.languageInfo].languageObj.Assembly.Assembly_add,
                                            "onClick": "addAsemble()"
                                        }
                                    },
                                    {
                                        RightClickBtn: {
                                            "id": "treeLoad",
                                            "content": SView[SView.UIManager.languageInfo].languageObj.Assembly.Assembly_load,
                                            "onClick": "loadOrUnload('install')",
                                        }
                                    },
                                    {
                                        RightClickBtn: {
                                            "id": "treeUninstall",
                                            "content": SView[SView.UIManager.languageInfo].languageObj.Assembly.Assembly_unload,
                                            "onClick": "loadOrUnload('uninstall')",
                                        }
                                    },
                                    {
                                        RightClickBtn: {
                                            "id": "tree_add_to_shapeset",
                                            "content": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_ShapeSet,
                                            "onClick": "addToShapeSet()",
                                            /* isDisabled: true,*/
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
                        content: SView[SView.UIManager.languageInfo].languageObj.MeetingDirection.CreateView,
                        imgsrc: "images/button_snapshot_.png",
                        onClick: "createView()"
                    }
                },
                {
                    ViewList: {
                        id: "sview_view_panel",
                        classes: [
                            "SView-pull-left-div",
                        ],
                        subComponents: [

                        ]
                    }
                }
            ]
        }
    }
}

//创建视图框
//var CreateView = {
//    "Dialog": {
//        "id": "create_view_dialog",
//        "closeBtnOnClick": "closeDialog('create_view_dialog')",
//        "title": SView[SView.UIManager.languageInfo].languageObj.Prompt.ViewName,
//        showBack: true,
//        "subComponents": [
//            {
//                Form: {
//                    id: "createViewForm",
//                    classes: [
//                        "SView-form",
//                        "SView-form-middle"
//                    ],
//                    subComponents: [
//                        {
//                            WithBothInput: {
//                                id: "createView",
//                                title: SView[SView.UIManager.languageInfo].languageObj.Prompt.ViewName,
//                                warning: SView[SView.UIManager.languageInfo].languageObj.Prompt.InputViewName,
//                                subComponents: [
//                                    {
//                                        Input: {
//                                            id: "createViewName",
//                                            value: "",
//                                            placeHolder: SView[SView.UIManager.languageInfo].languageObj.Prompt.InputViewName,
//                                            onInput: "checkLength('createViewName')"
//                                        }
//                                    }
//                                ]
//                            }
//                        }
//                    ]
//                }
//            },
//        ],
//        "buttons": [{
//            Button: {
//                "type": "basicButton",
//                "classes": [
//                    "SView-button",
//                    "SView-button-primary"
//                ],
//                "id": "confirmOK",
//                "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Confirm,
//                "onClick": "createServerView()"
//            }

//        }, {
//            Button: {
//                "type": "basicButton",
//                "classes": [
//                    "SView-button",
//                    "SView-button-default"
//                ],
//                "id": "confirmExit",
//                "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Cancel,
//                "onClick": "closeDialog('create_view_dialog')",
//            }

//        }]
//    }
//}

//编辑视图框
//var EditView = {
//    "Dialog": {
//        "id": "edit_view_dialog",
//        "closeBtnOnClick": "closeDialog('edit_view_dialog')",
//        "title": SView[SView.UIManager.languageInfo].languageObj.Prompt.ViewName,
//        showBack: true,
//        "subComponents": [
//            {
//                Form: {
//                    id: "editViewForm",
//                    classes: [
//                        "SView-form",
//                        "SView-form-middle"
//                    ],
//                    subComponents: [
//                        {
//                            WithBothInput: {
//                                id: "editView",
//                                title: SView[SView.UIManager.languageInfo].languageObj.Prompt.ViewName,
//                                warning: SView[SView.UIManager.languageInfo].languageObj.Prompt.InputViewName,
//                                subComponents: [
//                                    {
//                                        Input: {
//                                            id: "editViewName",
//                                            value: "",
//                                            placeHolder: SView[SView.UIManager.languageInfo].languageObj.Prompt.InputViewName,
//                                            onInput: "checkLength('editViewName')"
//                                        },

//                                    }
//                                ]
//                            }
//                        }, {
//                            Input: {
//                                id: "editViewId",
//                                value: "",
//                                type: "hidden",
//                                isShow: false
//                            },

//                        }, {
//                            Input: {
//                                id: "editViewServerId",
//                                value: "",
//                                type: "hidden",
//                                isShow: false
//                            }
//                        }
//                    ]
//                }
//            },
//        ],
//        "buttons": [{
//            Button: {
//                "type": "basicButton",
//                "classes": [
//                    "SView-button",
//                    "SView-button-primary"
//                ],
//                "id": "confirmOK",
//                "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Confirm,
//                "onClick": "editServerView()"
//            }

//        }, {
//            Button: {
//                "type": "basicButton",
//                "classes": [
//                    "SView-button",
//                    "SView-button-default"
//                ],
//                "id": "confirmExit",
//                "content": SView[SView.UIManager.languageInfo].languageObj.SweetAlert.Cancel,
//                "onClick": "closeDialog('edit_view_dialog')",
//            }

//        }]
//    }
//}

