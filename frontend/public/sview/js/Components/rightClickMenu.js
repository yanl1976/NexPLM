var RightClickMenu;
var RightClickSelectMenu;
function initRightClickMenu() {
    RightClickMenu = {
        "RightClickMenu": {
            "id": "rightClickMenu",
            "classes": [
                "SView-rightClickMenu"
            ],
            "subComponents": [
                //{
                //    "RightClickBtn": {
                //        "id": "SingleShow",
                //        "imgsrc": "images/rightClickMenu/modelSelectShow.png",
                //        "content": SView[language].languageObj.RightMenu.RightMenu_Single_display,
                //        "onClick": "commandClickFun('','SingleShow','SView.Commands.ModelSingleShowCommand')"
                //    }
                //},
                {
                    "RightClickBtn": {
                        "id": "showOrHidden",
                        "imgsrc": "images/rightClickMenu/show-hide.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Explicit_exchange,
                        "onClick": "commandClickFun('','ModelShowOrHide','SView.Commands.ModelShowOrHideCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "showAll",
                        "imgsrc": "images/rightClickMenu/modelShowAll.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Show_all,
                        "onClick": "commandClickFun('','ShowAllModels','SView.Commands.ShowAllModelsCommand')"

                    }
                },
                {
                    "RightClickBtn": {
                        "id": "alignMiddle",
                        "imgsrc": "images/rightClickMenu/sview_perspective_box.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Centered_display,
                        "onClick": "commandClickFun('','AdaptiveViewPerspective','SView.Commands.ModelCenterCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "setting",
                        "imgsrc": "images/rightClickMenu/button_setting.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Setting,
                        "isShow": M3D.Utility.CookieHelper.getCookie("isSplit") && M3D.Utility.CookieHelper.getCookie("isSplit") == "true" ? false : true,
                        "onClick": "commandClickFun('','Option','SView.Commands.OptionCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "attributes",
                        "imgsrc": "images/rightClickMenu/sview_measure_attributes.png",
                        "content": SView[language].languageObj.Property.Property,
                        "onClick": "showAttributeInfo('attributeInfo')"
                    }
                },
            ]
        }
    }
    RightClickSelectMenu = {
        "RightClickMenu": {
            "id": "rightClickSelectMenu",
            "classes": [
                "SView-rightClickMenu"
            ],
            "subComponents": [
                {
                    "RightClickBtn": {
                        "id": "rightMenu_hide",
                        "imgsrc": "images/rightClickMenu/show-hide.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Hide,
                        "onClick": "commandClickFun('','ModelHideCommand','SView.Commands.ModelHideCommand')"

                    }
                },
                {
                    "RightClickBtn": {
                        "id": "rightMenu_single_display",
                        "imgsrc": "images/rightClickMenu/modelSelectShow.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Single_display,
                        "onClick": "commandClickFun('','ModelSingleShowCommand','SView.Commands.ModelSingleShowCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "set_color",
                        "imgsrc": "images/rightClickMenu/sview_display_singlemodel_color.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_SetColor,
                        // "onClick": "showSettingColor()", // 设置颜色
                        "onClick": "commandClickFun('','SetColor','SView.Commands.ModelSetColorCommand')",
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "reset_new",
                        "imgsrc": "images/rightClickMenu/button_select_reset_new.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_ResetSelect,
                        "onClick": "commandClickFun('','Reposition','SView.Commands.MoveRestoreCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "select_align_middle",
                        "imgsrc": "images/rightClickMenu/sview_perspective_box.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_Centered_display,
                        "onClick": "commandClickFun('','AdaptiveViewPerspective','SView.Commands.ModelCenterCommand')"
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "add_to_shapeset",
                        "imgsrc": "images/rightClickMenu/sview_perspective_box.png",
                        "content": SView[language].languageObj.RightMenu.RightMenu_ShapeSet,
                        "onClick": "commandClickFun('','ShapeSetCommand','SView.Commands.ShapeSetCommand')",
                        /* isDisabled: true,*/
                    }
                },
                {
                    "RightClickBtn": {
                        "id": "select_attributes",
                        "imgsrc": "images/rightClickMenu/sview_measure_attributes.png",
                        "content": SView[language].languageObj.Property.Property,
                        "onClick": "showAttributeInfo('attributeInfo')"
                    }
                },
            ]
        }
    }
}
RightClickSelectAnnotateMenu = {
    "RightClickMenu": {
        "id": "rightClickSelectAnnotateMenu",
        "classes": [
            "SView-rightClickMenu"
        ],
        "subComponents": [
            {
                "RightClickBtn": {
                    "id": "editAnnotate",
                    "imgsrc": "images/moreMenu/annotate/sview_note_edit.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_edit,
                    "onClick": "eidtAnnotate()",
                }
            },
            {
                "RightClickBtn": {
                    "id": "delAnnotate",
                    "imgsrc": "images/moreMenu/annotate/sview_clear.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_del,
                    "onClick": "commandClickFun(null,'Delete',null,M3D.Shapes.ShapeType.ANNOTATION_NOTE)",
                }
            }
        ]
    }
}
RightClickSelectMeasureMenu = {
    "RightClickMenu": {
        "id": "rightClickSelectMeasureMenu",
        "classes": [
            "SView-rightClickMenu"
        ],
        "subComponents": [
            {
                "RightClickBtn": {
                    "id": "delMeasure",
                    "imgsrc": "images/moreMenu/annotate/sview_clear.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_del,
                    "onClick": "commandClickFun(null,'Delete',null,M3D.Shapes.ShapeType.MEASURE_BASE)",
                }
            }
        ]
    }
}
RightClickSelectRingMenu = {
    "RightClickMenu": {
        "id": "rightClickSelectRingMenu",
        "classes": [
            "SView-rightClickMenu"
        ],
        "subComponents": [
            {
                "RightClickBtn": {
                    "id": "ring_rightMenu_hide",
                    "imgsrc": "images/rightClickMenu/show-hide.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_Hide,
                    "onClick": "commandClickFun('','ModelHideCommand','SView.Commands.ModelHideCommand')"

                }
            },
            {
                "RightClickBtn": {
                    "id": "ring_rightMenu_single_display",
                    "imgsrc": "images/rightClickMenu/modelSelectShow.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_Single_display,
                    "onClick": "commandClickFun('','ModelSingleShowCommand','SView.Commands.ModelSingleShowCommand')"
                }
            },
            {
                "RightClickBtn": {
                    "id": "ring_reset_new",
                    "imgsrc": "images/rightClickMenu/button_select_reset_new.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_ResetSelect,
                    "onClick": "commandClickFun('','Reposition','SView.Commands.MoveRestoreCommand')"
                }
            },
            {
                "RightClickBtn": {
                    "id": "ring_select_align_middle",
                    "imgsrc": "images/rightClickMenu/sview_perspective_box.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_Centered_display,
                    "onClick": "commandClickFun('','AdaptiveViewPerspective','SView.Commands.ModelCenterCommand')"
                }
            },
            {
                "RightClickBtn": {
                    "id": "ring_select_attributes",
                    "imgsrc": "images/rightClickMenu/sview_measure_attributes.png",
                    "content": SView[SView.UIManager.languageInfo].languageObj.Property.Property,
                    "onClick": "showAttributeInfo('attributeInfo')"
                }
            },
        ]
    }
}