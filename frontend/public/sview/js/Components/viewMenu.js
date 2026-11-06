var ViewMenu = {
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
                            "title": SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.solidMode,
                            onClick: "selectRenderMode('solid')",
                        }
                    },
                    //{
                    //    Button: {
                    //        id: "solidWire",
                    //        imgsrc: "./images/renderMode/button_solid_wire.png",
                    //        title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.solidWireMode,
                    //        onClick: "selectRenderMode('solidWire')",
                    //    }
                    //},
                    {
                        Button: {
                            id: "wire",
                            imgsrc: "./images/renderMode/button_wire.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.illustrationMode,
                            onClick: "selectRenderMode('wire')",
                        }
                    },
                    {
                        Button: {
                            id: "transparent",
                            imgsrc: "./images/renderMode/sview_perspective_transparent.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.transparentMode,
                            onClick: "selectRenderMode('transparent')",
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.topView,
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.axisView,
                            onClick: "viewDirection('fromAxis')"
                        }
                    },
                    {
                        Button: {
                            id: "fromLeft",
                            imgsrc: "./images/viewDirection/button_left.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.leftView,
                            onClick: "viewDirection('fromLeft')"
                        }
                    },
                    {
                        Button: {
                            id: "fromFront",
                            imgsrc: "./images/viewDirection/button_front.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.frontView,
                            onClick: "viewDirection('fromFront')"
                        }
                    },
                    {
                        Button: {
                            id: "fromRight",
                            imgsrc: "./images/viewDirection/button_right.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.rightView,
                            onClick: "viewDirection('fromRight')"
                        }
                    },
                    {
                        Button: {
                            id: "fromBack",
                            imgsrc: "./images/viewDirection/button_behind.png",
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.behindView,
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.bottomView,
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
                            id: "notUseButton",
                            isShow: false,
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.perspective,
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.walkThrough,
                            onClick: "selectWalkingMode('walking')",
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
                            title: SView[SView.UIManager.languageInfo].languageObj.ViewMenuTitle.setting,
                            "isShow": M3D.Utility.CookieHelper.getCookie("isSplit") && M3D.Utility.CookieHelper.getCookie("isSplit") == "true" ? false : true,
                            onClick: "commandClickFun('','Option','SView.Commands.OptionCommand')"
                        }
                    }
                ]
            }
        ]
    }
}

////ÂþÓÎÐéÄâÒ¡¸Ë,×óÓÒ
//var JoystickVirtualRocker = {
//    VirtualRocker: {
//        id: "joystick-virtualrocker",
//    }

//}
////ÂþÓÎÐéÄâÒ¡¸Ë,ÉÏÏÂ
//var JoystickUpAndDown = {
//    UpAndDown: {
//        id: "joystick-upanddown",
//    }
//}