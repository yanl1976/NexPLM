/* global SView */
//选择集
var SelectChoices = {
  SelectChoices: {
    id: "mainBottomMenu",
    placeholder: "请选择",
    addOption: {
      label: "新增选项",
      addOptionFun: "confirmDelete('deleteAnnotate')",
    },
    options: [
      {
        label: "选择集1",
        value: 1,
        removeOptionFun: "confirmDelete('deleteAnnotate')",
      },
      {
        label: "选择集2",
        value: 2,
        removeOptionFun: "confirmDelete('deleteAnnotate')",
      },
    ],
  },
};

// ---------- 底部主菜单 -----------
var MainBottomMenu = {
  BottomMenu: {
    id: "mainBottomMenu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "move",
          imgsrc: "images/moreMenu/sview_movecontrol_translation.png",
          title: SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_Move,
          // "onClick": "enterBottomMenu(MoveMenu)"
          onClick: "commandClickFun('','SelectMove')",
        },
      },
      {
        Button: {
          id: "annotate",
          imgsrc: "images/moreMenu/button_note.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle._note,
          onClick: "enterBottomMenu(AnnotateMenu)",
        },
      },
      {
        Button: {
          id: "animation",
          imgsrc: "images/moreMenu/button_ani_play.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.animation,
          //"onClick": "enterBottomMenu(AnimationMenu)",
          onClick: "commandClickFun('','PlayAnimationCommand')",
          //"onClick": "confirmPlayAnimation('PlayAnimationCommand')",
        },
      },
      {
        Button: {
          id: "measure",
          imgsrc: "images/moreMenu/button_measure.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.measure,
          onClick: "enterBottomMenu(MeasureMenu)",
        },
      },
      {
        Button: {
          id: "section",
          imgsrc: "images/moreMenu/button_section.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.lipq,
          onClick: "commandClickFun('','ClipPlaneCommand')",
        },
      },
      {
        Button: {
          id: "explosive",
          imgsrc: "images/moreMenu/button_explosive.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.li_explosive,
          onClick: "commandClickFun('', 'ExplosionCommand')",
        },
      },
      {
        Button: {
          id: "snapshot",
          imgsrc: "images/moreMenu/button_snapshot.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.snapshot,
          onClick: "commandClickFun('', 'FastPicture')",
        },
      },
      {
        Button: {
          id: "clear",
          imgsrc: "images/moreMenu/sview_clear.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle.clear,
          onClick: "showPopoverSelect('clearAllMode')",
          subComponents: [
            {
              PopoverSelect: {
                id: "clearAllMode",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "clearAnnotates",
                      imgsrc: "images/moreMenu/button_note.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle
                          .ClearAnnotation,
                      onClick:
                        "commandClickFun(null,'Clear',null,M3D.Shapes.ShapeType.ANNOTATION_NOTE)",
                      isShow: false,
                    },
                  },
                  {
                    Button: {
                      id: "clearMeasurements",
                      imgsrc: "images/moreMenu/button_measure.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle.ClearMeasure,
                      onClick:
                        "commandClickFun(null,'Clear',null,M3D.Shapes.ShapeType.MEASURE_BASE)",
                    },
                  },
                  {
                    Button: {
                      id: "clearSectionPlanes",
                      imgsrc: "images/moreMenu/button_section.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ClearMenuTitle.ClearSection,
                      onClick: "commandClickFun(null,'Clear',null,M3D.Shapes.ShapeType.SECTION)",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
    ],
  },
};

// ---------- 批注主菜单 -----------
var AnnotateMenu = {
  BottomMenu: {
    id: "annotate_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "gesture",
          imgsrc: "images/moreMenu/annotate/sview_gesturenote.png",
          title: SView[SView.UIManager.languageInfo].languageObj.BottomMenuTitle._scaleL,
          commandName: "HandDrawn",
          // "onClick": "enterBottomMenu(GestureMenu, AnnotateMenu)",
          onClick: "commandClickFun('gesture', 'PaintBrush')",
        },
      },
      {
        Button: {
          id: "textnote",
          imgsrc: "images/moreMenu/annotate/sview_textnote.png",
          title: SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_text,
          commandName: "Annotation",
          onClick: "commandClickFun('textnote','Annotation')",
        },
      },
      {
        Button: {
          id: "sequence",
          imgsrc: "images/moreMenu/annotate/button_sequence.png",
          title: SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.note_order,
          commandName: "NumberAnnotation",
          onClick: "commandClickFun('sequence','AnnotationNumber')",
        },
      },
      //{
      //    "Button": {
      //        "id": "component",
      //        "imgsrc": "images/moreMenu/annotate/button_component.png",
      //        "title": SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.note_component,
      //        "commandName": "AnnotationComponent",
      //        isDisabled: true,
      //        "onClick": "commandClickFun('component','AnnotationComponent')",
      //    }
      //},
      {
        Button: {
          id: "editButton",
          imgsrc: "images/moreMenu/annotate/sview_note_edit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_edit,
          //"onClick": "eidtAnnotate()",
          onClick: "commandClickFun('editButton','AnnotationEdit')",
        },
      },
      {
        Button: {
          id: "deleteAnnotate",
          imgsrc: "images/moreMenu/annotate/sview_clear.png",
          title: SView[SView.UIManager.languageInfo].languageObj.NoteMenuTitle.node_del,
          // "onClick": "enterBottomMenu('explosiveMenu')",
          onClick:
            "commandClickFun('deleteAnnotate','Delete',null,M3D.Shapes.ShapeType.ANNOTATION_NOTE)",
        },
      },
      {
        Button: {
          id: "saveAnnotate",
          imgsrc: "images/moreMenu/annotate/sview_button_save.svg",
          title: SView[SView.UIManager.languageInfo].languageObj.SynergyGroup.Save,
          // "onClick": "exitBottomMenu(AnnotateMenu)",
          onClick: "",
        },
      },
      {
        Button: {
          id: "annotateMenu2",
          imgsrc: "images/moreMenu/button_note.png",
          title: SView[SView.UIManager.languageInfo].languageObj.setting.AnnotationData,
          // "onClick": "exitBottomMenu(AnnotateMenu)",
          onClick: "",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          // "onClick": "exitBottomMenu(AnnotateMenu)",
          onClick: "confirmExit('AnnotateMenu')",
        },
      },
    ],
  },
};
// ----------距离测量菜单 -----------
var MeasureMenu = {
  BottomMenu: {
    id: "measure_distance_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "measure_distance_select",
          imgsrc: "images/moreMenu/measure/sview_measure_distance.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Type_distance,
          onClick: "showPopoverSelect('measure_select')",
          subComponents: [
            {
              PopoverSelect: {
                id: "measure_select",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "measure_distance",
                      imgsrc: "images/moreMenu/measure/sview_measure_distance.png",
                      highLight: true,
                      commandName: "MeasureDistance",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_distance,
                      onClick: "hidePopoverSelect('measure_select')",
                    },
                  },
                  {
                    Button: {
                      id: "measure_angle",
                      imgsrc: "images/moreMenu/measure/sview_measure_angle.png",
                      commandName: "MeasureAngle",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Angle,
                      onClick: "enterBottomMenu(MeasureAngleMenu,MeasureMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_diameter",
                      imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
                      commandName: "AnnotationComponent",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Circle,
                      onClick: "enterBottomMenu(MeasureDiameterMenu,MeasureMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_attributes",
                      imgsrc: "images/moreMenu/measure/sview_measure_attributes.png",
                      commandName: "AnnotationComponent",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Property,
                      onClick: "enterBottomMenu(MeasureAttributesMenu,MeasureMenu)",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "measure_dd",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_dd.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_PntToPnt,
          onClick:
            "commandClickFun('measure_dd','MeasureDistance','SView.Commands.MeasureDistanceCommand',1)",
        },
      },
      {
        Button: {
          id: "measure_dl",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_dl.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_PntToLine,
          onClick:
            "commandClickFun('measure_dl','MeasureDistance','SView.Commands.MeasureDistanceCommand',2)",
        },
      },
      {
        Button: {
          id: "measure_ps",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_ps.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_PntToFace,
          onClick:
            "commandClickFun('measure_ps','MeasureDistance','SView.Commands.MeasureDistanceCommand',3)",
        },
      },
      {
        Button: {
          id: "measure_ll",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_ll.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_LineToLine,
          onClick:
            "commandClickFun('measure_ll','MeasureDistance','SView.Commands.MeasureDistanceCommand',4)",
        },
      },
      {
        Button: {
          id: "measure_ls",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_ls.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_LineToFace,
          onClick:
            "commandClickFun('measure_ls','MeasureDistance','SView.Commands.MeasureDistanceCommand',5)",
        },
      },
      {
        Button: {
          id: "measure_ss",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_ss.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_FaceToFace,
          onClick:
            "commandClickFun('measure_ss','MeasureDistance','SView.Commands.MeasureDistanceCommand',6)",
        },
      },
      {
        Button: {
          id: "measure_thickness",
          imgsrc: "images/moreMenu/measure/sview_measure_distance_thickness.png",
          commandName: "MeasureDistance",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_Thickness,
          onClick:
            "commandClickFun('measure_thickness','MeasureDistance','SView.Commands.MeasureDistanceCommand',56)",
        },
      },
      {
        Button: {
          id: "deleteDistanceMeasure",
          imgsrc: "images/moreMenu/annotate/sview_clear.png",
          title: SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_del,
          onClick:
            "commandClickFun('deleteDistanceMeasure','Delete',null,M3D.Shapes.ShapeType.MEASURE_BASE)",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "confirmExit('MeasureMenu')",
        },
      },
    ],
  },
};
// ----------角度测量菜单 -----------
var MeasureAngleMenu = {
  BottomMenu: {
    id: "measure_angle_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "measure_angle_select",
          imgsrc: "images/moreMenu/measure/sview_measure_angle.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Type_Angle,
          onClick: "showPopoverSelect('angle_select')",
          subComponents: [
            {
              PopoverSelect: {
                id: "angle_select",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "measure_distance",
                      imgsrc: "images/moreMenu/measure/sview_measure_distance.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_distance,
                      onClick: "enterBottomMenu(MeasureMenu,MeasureAngleMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_angle",
                      imgsrc: "images/moreMenu/measure/sview_measure_angle.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Angle,
                      highLight: true,
                      onClick: "hidePopoverSelect('measure_select')",
                    },
                  },
                  {
                    Button: {
                      id: "measure_diameter",
                      imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Circle,
                      onClick: "enterBottomMenu(MeasureDiameterMenu,MeasureAngleMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_attributes",
                      imgsrc: "images/moreMenu/measure/sview_measure_attributes.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Property,
                      onClick: "enterBottomMenu(MeasureAttributesMenu,MeasureAngleMenu)",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "angle_ll",
          imgsrc: "images/moreMenu/measure/sview_measure_angle_ll.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Angle_LineToLine,
          commandName: "MeasureAngle",
          onClick:
            "commandClickFun('angle_ll','MeasureAngle','SView.Commands.MeasureAngleCommand',50)",
        },
      },
      {
        Button: {
          id: "angle_lf",
          imgsrc: "images/moreMenu/measure/sview_measure_angle_lf.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Angle_LineToFace,
          commandName: "MeasureAngle",
          onClick:
            "commandClickFun('angle_lf','MeasureAngle','SView.Commands.MeasureAngleCommand',51)",
        },
      },
      {
        Button: {
          id: "angle_ff",
          imgsrc: "images/moreMenu/measure/sview_measure_angle_ff.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Angle_FaceToFace,
          commandName: "MeasureAngle",
          onClick:
            "commandClickFun('angle_ff','MeasureAngle','SView.Commands.MeasureAngleCommand',52)",
        },
      },
      {
        Button: {
          id: "deleteAngleMeasure",
          imgsrc: "images/moreMenu/annotate/sview_clear.png",
          title: SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_del,
          onClick:
            "commandClickFun('deleteAngleMeasure','Delete',null,M3D.Shapes.ShapeType.MEASURE_BASE)",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "confirmExit('MeasureAngleMenu')",
        },
      },
    ],
  },
};
// ----------圆相关测量菜单 -----------
var MeasureDiameterMenu = {
  BottomMenu: {
    id: "measure_diameter_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "measure_diameter_select",
          imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Type_Circle,
          onClick: "showPopoverSelect('diameter_select')",
          subComponents: [
            {
              PopoverSelect: {
                id: "diameter_select",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "measure_distance",
                      imgsrc: "images/moreMenu/measure/sview_measure_distance.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_distance,
                      onClick: "enterBottomMenu(MeasureMenu,MeasureDiameterMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_angle",
                      imgsrc: "images/moreMenu/measure/sview_measure_angle.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Angle,
                      onClick: "enterBottomMenu(MeasureAngleMenu,MeasureDiameterMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_diameter",
                      imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Circle,
                      highLight: true,
                      onClick: "hidePopoverSelect('measure_select')",
                    },
                  },
                  {
                    Button: {
                      id: "measure_attributes",
                      imgsrc: "images/moreMenu/measure/sview_measure_attributes.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Property,
                      onClick: "enterBottomMenu(MeasureAttributesMenu,MeasureDiameterMenu)",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "circle_center",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_circle_center.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Circle_Center_Arc,
          onClick:
            "commandClickFun('circle_center','MeasureDiametre','SView.Commands.MeasureDiametreCommand',1)",
        },
      },
      {
        Button: {
          id: "point_center",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_point_center.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Circle_Center_Point,
          onClick:
            "commandClickFun('point_center','MeasureDiametre','SView.Commands.MeasureDiametreCommand',2)",
        },
      },
      {
        Button: {
          id: "diameter",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
          title: SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Diameter,
          onClick:
            "commandClickFun('diameter','MeasureDiametre','SView.Commands.MeasureDiametreCommand',3)",
        },
      },
      {
        Button: {
          id: "radius",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_radius.png",
          title: SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Radius,
          onClick:
            "commandClickFun('radius','MeasureDiametre','SView.Commands.MeasureDiametreCommand',4)",
        },
      },
      {
        Button: {
          id: "shaft_shaft",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_shaft_shaft.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_ShaftToShaft,
          onClick:
            "commandClickFun('shaft_shaft','MeasureDiametre','SView.Commands.MeasureDiametreCommand',5)",
        },
      },
      {
        Button: {
          id: "center_center",
          commandName: "MeasureDiametre",
          imgsrc: "images/moreMenu/measure/sview_measure_center_center.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
              .measure_Distance_CenterToCenter,
          onClick:
            "commandClickFun('center_center','MeasureDiametre','SView.Commands.MeasureDiametreCommand',6)",
        },
      },
      {
        Button: {
          id: "deleteDiametreMeasure",
          imgsrc: "images/moreMenu/annotate/sview_clear.png",
          title: SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_del,
          onClick:
            "commandClickFun('deleteDiametreMeasure','Delete',null,M3D.Shapes.ShapeType.MEASURE_BASE)",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "confirmExit('MeasureDiameterMenu')",
        },
      },
    ],
  },
};
// ----------属性测量菜单 -----------
var MeasureAttributesMenu = {
  BottomMenu: {
    id: "measure_attributes_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "measure_attributes_select",
          imgsrc: "images/moreMenu/measure/sview_measure_attributes.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Type_Property,
          onClick: "showPopoverSelect('attributes_select')",
          subComponents: [
            {
              PopoverSelect: {
                id: "attributes_select",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "measure_distance",
                      imgsrc: "images/moreMenu/measure/sview_measure_distance.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_distance,
                      onClick: "enterBottomMenu(MeasureMenu,MeasureAttributesMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_angle",
                      imgsrc: "images/moreMenu/measure/sview_measure_angle.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Angle,
                      onClick: "enterBottomMenu(MeasureAngleMenu,MeasureAttributesMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_diameter",
                      imgsrc: "images/moreMenu/measure/sview_measure_diameter.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Circle,
                      onClick: "enterBottomMenu(MeasureDiameterMenu,MeasureAttributesMenu)",
                    },
                  },
                  {
                    Button: {
                      id: "measure_attributes",
                      imgsrc: "images/moreMenu/measure/sview_measure_attributes.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle
                          .measure_Type_Property,
                      highLight: true,
                      onClick: "hidePopoverSelect('measure_select')",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "porperty_dot",
          commandName: "MeasureProperty",
          imgsrc: "images/moreMenu/measure/sview_measure_porperty_dot.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Property_Point,
          onClick:
            "commandClickFun('porperty_dot','MeasureProperty','SView.Commands.MeasurePropertyCommand',100)",
        },
      },
      {
        Button: {
          id: "porperty_line",
          commandName: "MeasureProperty",
          imgsrc: "images/moreMenu/measure/sview_measure_porperty_line.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Property_Line,
          onClick:
            "commandClickFun('porperty_line','MeasureProperty','SView.Commands.MeasurePropertyCommand',101)",
        },
      },
      {
        Button: {
          id: "porperty_surface",
          commandName: "MeasureProperty",
          imgsrc: "images/moreMenu/measure/sview_measure_porperty_surface.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Property_Face,
          onClick:
            "commandClickFun('porperty_surface','MeasureProperty','SView.Commands.MeasurePropertyCommand',103)",
        },
      },
      {
        Button: {
          id: "porperty_model",
          commandName: "MeasureProperty",
          imgsrc: "images/moreMenu/measure/sview_measure_porperty_model.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.MeasureMenuTitle.measure_Property_Model,
          onClick:
            "commandClickFun('porperty_model','MeasureProperty','SView.Commands.MeasurePropertyCommand',105)",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "confirmExit('MeasureAttributesMenu')",
        },
      },
    ],
  },
};
//爆炸菜单
var ExplosiveMenu = {
  BottomMenu: {
    id: "explosive_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "explosive_global",
          imgsrc: "images/moreMenu/explosive/sview_explosive_global.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.ExplosiveMenuTitle.section_bar_main,
          onClick: "showPopoverSelect('explosiveSelect')",
          highLight: true,
          subComponents: [
            {
              PopoverSelect: {
                id: "explosiveSelect",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "centerExplosive",
                      imgsrc: "images/moreMenu/explosive/sview_explosive_global.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ExplosiveMenuTitle
                          .explosive_center,
                      onClick: "explosiveClickFun('centerExplosive', 0)",
                      highLight: true,
                    },
                  },
                  {
                    Button: {
                      id: "xExplosive",
                      imgsrc: "images/moreMenu/explosive/sview_explosive_yzdirection.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ExplosiveMenuTitle
                          .explosive_by_x,
                      onClick: "explosiveClickFun('xExplosive', 1)",
                    },
                  },
                  {
                    Button: {
                      id: "yExplosive",
                      imgsrc: "images/moreMenu/explosive/sview_explosive_xzdirection.png",
                      title:
                        SView[SView.UIManager.languageInfo].languageObj.ExplosiveMenuTitle
                          .explosive_by_y,
                      onClick: "explosiveClickFun('yExplosive', 2)",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Slider: {
          id: "explosive_slider",
          sliderBar: {
            id: "explosive_sliderBar",
            min: 0,
            max: 100,
            step: 1,
            value: 0,
            sliderChange: "sliderExplosiveChange('explosive_sliderBar','ExplosionValue')",
          },
        },
      },
      {
        Select: {
          id: "explosive_select",
          value: null,
          classes: ["SView-select", "explosiveGrade"],
          options: [
            {
              Option: {
                value: -1,
                content: SView[SView.UIManager.languageInfo].languageObj.setting.Unit_of_none,
              },
            },
            {
              Option: {
                value: 0,
                content: "0",
              },
            },
            {
              Option: {
                value: 1,
                content: 1,
              },
            },
            {
              Option: {
                value: 2,
                content: 2,
              },
            },
            {
              Option: {
                value: 3,
                content: 3,
              },
            },
            {
              Option: {
                value: 4,
                content: 4,
              },
            },
            {
              Option: {
                value: 5,
                content: 5,
              },
            },
          ],
          onChange: "explosiveSelectClickFun('explosive_select')",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "confirmExit('ExplosiveMenu')",
        },
      },
    ],
  },
};
//剖切菜单
var SectioneMenu = {
  BottomMenu: {
    id: "sectione_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "sectione_global",
          imgsrc: "images/moreMenu/section/button_yzsection.png",
          title: SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_bar_main,
          imgOnClick: "showPopoverSelect('sectionePopover')",
          subComponents: [
            {
              Popover: {
                id: "sectionePopover",
                isShow: false,
                classes: [
                  "SView-popover",
                  "SView-popover-up", // 弹出方向：up、down
                ],
                subComponents: [
                  {
                    PopoverRow: {
                      id: "popoverRowSection_1",
                      subComponents: [
                        {
                          Button: {
                            id: "yzSection",
                            imgsrc: "images/moreMenu/section/button_yzsection.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_yzsection,
                            onClick: "sectionClickFun('yzSection', 'YZSectionIsVisible')",
                          },
                        },
                        {
                          Button: {
                            id: "yzSectionReverse_1",
                            imgsrc: "images/moreMenu/section/button_sectionreversal.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_bar_main,
                            onClick: "sectionClickFun('yzSectionReverse_1','YZSectionIsReverse')",
                          },
                        },
                        {
                          Slider: {
                            id: "section_slider_1",
                            sliderBar: {
                              id: "section_sliderBar_1",
                              min: 0,
                              max: 100,
                              step: 1,
                              value: 0,
                              sliderChange:
                                "sliderClipChange('section_sliderBar_1','YZSectionPercent')",
                            },
                          },
                        },
                      ],
                    },
                  },
                  {
                    PopoverRow: {
                      id: "popoverRowSection_2",
                      subComponents: [
                        {
                          Button: {
                            id: "xySection",
                            imgsrc: "images/moreMenu/section/button_xysection.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_xysection,
                            onClick: "sectionClickFun('xySection', 'XYSectionIsVisible')",
                          },
                        },
                        {
                          Button: {
                            id: "xySectionReverse",
                            imgsrc: "images/moreMenu/section/button_sectionreversal.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_bar_main,
                            onClick: "sectionClickFun('xySectionReverse', 'XYSectionIsReverse')",
                          },
                        },
                        {
                          Slider: {
                            id: "section_slider_2",
                            sliderBar: {
                              id: "section_sliderBar_2",
                              min: 0,
                              max: 100,
                              step: 1,
                              value: 0,
                              sliderChange:
                                "sliderClipChange('section_sliderBar_2','XYSectionPercent')",
                            },
                          },
                        },
                      ],
                    },
                  },
                  {
                    PopoverRow: {
                      id: "popoverRowSection_3",
                      subComponents: [
                        {
                          Button: {
                            id: "zxSection",
                            imgsrc: "images/moreMenu/section/button_zxsection.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_xzsection,
                            onClick: "sectionClickFun('zxSection', 'ZXSectionIsVisible')",
                          },
                        },
                        {
                          Button: {
                            id: "zxSectionReverse",
                            imgsrc: "images/moreMenu/section/button_sectionreversal.png",
                            title:
                              SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle
                                .section_bar_main,
                            onClick: "sectionClickFun('zxSectionReverse', 'ZXSectionIsReverse')",
                          },
                        },
                        {
                          Slider: {
                            id: "section_slider_3",
                            sliderBar: {
                              id: "section_sliderBar_3",
                              min: 0,
                              max: 100,
                              step: 1,
                              value: 0,
                              sliderChange:
                                "sliderClipChange('section_sliderBar_3','ZXSectionPercent')",
                            },
                          },
                        },
                      ],
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Slider: {
          id: "all_section_slider",
          sliderBar: {
            id: "all_section_sliderBar",
            min: 0,
            max: 100,
            step: 1,
            value: 0,
            sliderChange: "sliderClipChange('all_section_sliderBar','YZSectionPercent')",
          },
        },
      },
      {
        Button: {
          id: "allSectionReverse",
          imgsrc: "images/moreMenu/section/button_sectionreversal.png",
          title: SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_bar_main,
          onClick: "sectionClickFun('allSectionReverse','IsAllReverse')",
        },
      },
      {
        Button: {
          id: "sectionPlane",
          imgsrc: "images/moreMenu/section/button_showsectionplane.png",
          title: SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_plane,
          onClick: "sectionClickFun('sectionPlane','IsShowClipPlane')",
        },
      },
      {
        Button: {
          id: "coverPlane",
          imgsrc: "images/moreMenu/section/sview_section_showcappingplane.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_cappingPlane,
          onClick: "sectionClickFun('coverPlane','IsShowCappingPlane')",
        },
      },
      {
        Button: {
          id: "addSection",
          imgsrc: "images/moreMenu/section/button_add.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_addselected,
          subComponents: [
            {
              Badge: {
                id: "addSectionBadge",
                type: "withContentBadge", //设置徽标类型为文字类型
                classes: [
                  "SView-badge-number",
                  "SView-section-badge-topRight", // 徽标所在位置：topLeft、topRight、bottomLeft、bottomRight
                ],
                content: "12343", //若徽标类型为文字，该属性必填
              },
            },
          ],
          onClick: "sectionSelectedShapesBtClickFun('addSection','add')",
        },
      },
      {
        Button: {
          id: "clearSection",
          imgsrc: "images/moreMenu/section/button_clear.png",
          title:
            SView[SView.UIManager.languageInfo].languageObj.SectionMenuTitle.section_clearselected,
          onClick: "sectionSelectedShapesBtClickFun('clearSection','clear')",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "exitClipBottomMenu(SectioneMenu)",
        },
      },
    ],
  },
};
// ---------- 移动主菜单 -----------
var MoveMenu = {
  BottomMenu: {
    id: "move_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "move_global",
          imgsrc: "images/moreMenu/translation/sview_dragger_translation.png",
          title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.trans,
          imgOnClick: "showPopoverSelect('move_type')",
          subComponents: [
            {
              PopoverSelect: {
                id: "move_type",
                isShow: false,
                subComponents: [
                  {
                    Button: {
                      id: "translation",
                      imgsrc: "images/moreMenu/translation/sview_dragger_translation.png",
                      title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.trans,
                      highLight: true,
                      onClick: "selectImg('translation','move_global')",
                    },
                  },
                  {
                    Button: {
                      id: "rotation",
                      imgsrc: "images/moreMenu/translation/sview_dragger_rotation.png",
                      title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.rot,
                      onClick: "selectImg('rotation','move_global')",
                    },
                  },
                  {
                    Button: {
                      id: "scaling",
                      imgsrc: "images/moreMenu/translation/sview_dragger_scaling.png",
                      title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.sca,
                      onClick: "selectImg('scaling','move_global')",
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "translation_dir",
          imgsrc: "images/moreMenu/translation/ssview_dragger_planetranslation.png",
          title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.dragger_face,
          highLight: true,
          onClick: "selectTranslationDir('translation_dir',0)",
        },
      },
      {
        Button: {
          id: "move_x",
          imgsrc: "images/moreMenu/translation/button_yzsection.png",
          title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.yzsection,
          onClick: "selectTranslationDir('move_x',1)",
        },
      },
      {
        Button: {
          id: "move_y",
          imgsrc: "images/moreMenu/translation/button_zxsection.png",
          title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.zxsection,
          onClick: "selectTranslationDir('move_y',2)",
        },
      },
      {
        Button: {
          id: "move_z",
          imgsrc: "images/moreMenu/translation/button_xysection.png",
          title: SView[SView.UIManager.languageInfo].languageObj.DraggerMenuTitle.xysection,
          onClick: "selectTranslationDir('move_z',3)",
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "exitBottomMenu(MoveMenu)",
        },
      },
    ],
  },
};
// ---------- 手势批注主菜单 -----------
//var GestureMenu = {
//    "BottomMenu": {
//        "id": "gesture_menu",
//        "classes": [
//            "SView-bottomMenu",
//        ],
//        "subComponents": [
//            {
//                "Button": {
//                    "id": "gesture_global",
//                    "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3d_freedom.png",
//                    "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.freedom,
//                    "imgOnClick": "showPopoverSelect('gestureType')",
//                    "subComponents": [{
//                        "PopoverSelect": {
//                            "id": "gestureType",
//                            "isShow": false,
//                            "subComponents": [
//                                {
//                                    "Button": {
//                                        "id": "gesture3d_freedom",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3d_freedom.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.freedom,
//                                        "highLight": true,
//                                        "onClick": "selectImg('gesture3d_freedom','gesture_global')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture3d_circle",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3d_circle.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.circle,
//                                        "onClick": "selectImg('gesture3d_circle','gesture_global')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture3d_rect",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3d_rect.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.rect,
//                                        "onClick": "selectImg('gesture3d_rect','gesture_global')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture3d_triangle",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3d_triangle.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.triangle,
//                                        "onClick": "selectImg('gesture3d_triangle','gesture_global')"
//                                    }
//                                },
//                            ]
//                        }
//                    }
//                    ]
//                }
//            },
//            {
//                "Button": {
//                    "id": "gesture_color_info",
//                    "imgsrc": "images/moreMenu/annotate/gesture/orange.png",
//                    "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_bar_second_main,
//                    "imgOnClick": "showPopoverSelect('gesture_color')",
//                    "subComponents": [{
//                        "PopoverSelect": {
//                            "id": "gesture_color",
//                            "isShow": false,
//                            "classes": [
//                                "SView-popoverSelect",
//                                "SView-popoverSelect-up",
//                                "SView-popoverSelect-tripleWidth"
//                            ],
//                            "subComponents": [
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_black",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/black.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_black,
//                                        "onClick": "selectImg('gesture_color_black','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_red",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/red.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_red,
//                                        "onClick": "selectImg('gesture_color_red','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_blue",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/blue.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_blue,
//                                        "onClick": "selectImg('gesture_color_blue','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_green",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/green.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_green,
//                                        "onClick": "selectImg('gesture_color_green','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_yellow",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/yellow.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_yellow,
//                                        "onClick": "selectImg('gesture_color_yellow','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_orange",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/orange.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_orange,
//                                        "highLight": true,
//                                        "onClick": "selectImg('gesture_color_orange','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_gray",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/gray.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_gray,
//                                        "onClick": "selectImg('gesture_color_gray','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_purple",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/purple.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_purple,
//                                        "onClick": "selectImg('gesture_color_purple','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_brown",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/brown.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_brown,
//                                        "onClick": "selectImg('gesture_color_brown','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_pink",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/pink.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_pink,
//                                        "onClick": "selectImg('gesture_color_pink','gesture_color_info')"
//                                    }
//                                },
//                                {
//                                    "Button": {
//                                        "id": "gesture_color_default",
//                                        "imgsrc": "images/moreMenu/annotate/gesture/default.png",
//                                        "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_white,
//                                        "onClick": "selectImg('gesture_color_default','gesture_color_info')"
//                                    }
//                                },
//                            ]
//                        }
//                    }]
//                }
//            },
//            {
//                "Button": {
//                    "id": "sview_undo",
//                    "imgsrc": "images/moreMenu/annotate/gesture/sview_undo.png",
//                    "title": SView[SView.UIManager.languageInfo].languageObj.GestureMenuTitle.gesture_undo,
//                    "onClick": "undoGestureNote()",
//                }
//            },
//            {
//                "Button": {
//                    "id": "button_gesture3D_confirm",
//                    "imgsrc": "images/moreMenu/annotate/gesture/button_gesture3D_confirm.png",
//                    "title": SView[SView.UIManager.languageInfo].languageObj.Prompt.Confim,
//                    "onClick": "confirmGestureNote()",
//                }
//            },
//            {
//                "Button": {
//                    "id": "gesture_width_exit",
//                    "imgsrc": "images/moreMenu/translation/sview_button_exit.png",
//                    "title": SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
//                    "onClick": "exitGestureMenu(GestureMenu,AnnotateMenu)",
//                }
//            },
//        ]
//    }
//}
//动画菜单
var AnimationMenu = {
  BottomMenu: {
    id: "animation_menu",
    classes: ["SView-bottomMenu"],
    subComponents: [
      {
        Button: {
          id: "ani_play",
          imgsrc: "images/moreMenu/animation/button_ani_play.png",
          title: SView[SView.UIManager.languageInfo].languageObj.AnimationMenuTitle.start,
          onClick: "commandClickFun('ani_play','AnimationCommand')",
        },
      },
      {
        Slider: {
          id: "ani_slider",
          sliderBar: {
            id: "ani_sliderBar",
            min: 0,
            max: 100,
            step: 1,
            value: 0,
          },
        },
      },
      {
        Button: {
          id: "ani_pre",
          imgsrc: "images/moreMenu/animation/sview_ani_pre.png",
          title: SView[SView.UIManager.languageInfo].languageObj.AnimationMenuTitle.backward,
          onClick: "commandClickFun('AnimationMenu')",
        },
      },
      {
        Button: {
          id: "ani_next",
          imgsrc: "images/moreMenu/animation/sview_ani_next.png",
          title: SView[SView.UIManager.languageInfo].languageObj.AnimationMenuTitle.forward,
          onClick: "commandClickFun('AnimationMenu')",
        },
      },
      {
        Button: {
          id: "ani_more",
          imgsrc: "images/moreMenu/animation/sview_ani_more.png",
          title: SView[SView.UIManager.languageInfo].languageObj.AnimationMenuTitle.more,
          imgOnClick: "showPopoverSelect('animation_popover')",
          subComponents: [
            {
              Popover: {
                id: "animation_popover",
                isShow: false,
                classes: [
                  "SView-popover",
                  "SView-popover-up", // 弹出方向：up、down
                  "animationSetting",
                ],
                subComponents: [
                  {
                    PopoverRow: {
                      id: "play_loop",
                      subComponents: [
                        {
                          Label:
                            SView[SView.UIManager.languageInfo].languageObj.Animation
                              .Animation_Loop,
                        },
                        {
                          Checkbox: {
                            id: "play_loop_checkbox",
                            checkboxId: "play_loop_checkbox_label", // 复选框input标签id
                          },
                        },
                      ],
                    },
                  },
                  {
                    PopoverRow: {
                      id: "play_speed",
                      subComponents: [
                        {
                          Label:
                            SView[SView.UIManager.languageInfo].languageObj.Animation
                              .Animation_Speed,
                        },
                        {
                          Select: {
                            id: "play_speed_select",
                            value: 1,
                            classes: ["SView-select", "play_speed_grade"],
                            options: [
                              {
                                Option: {
                                  value: 0.5,
                                  content: "0.5x",
                                },
                              },
                              {
                                Option: {
                                  value: 1,
                                  content: "1x",
                                },
                              },
                              {
                                Option: {
                                  value: 2,
                                  content: "2x",
                                },
                              },
                              {
                                Option: {
                                  value: 4,
                                  content: "4x",
                                },
                              },
                              {
                                Option: {
                                  value: 8,
                                  content: "8x",
                                },
                              },
                            ],
                            onClick: "commandClickFun()",
                          },
                        },
                      ],
                    },
                  },
                  {
                    PopoverRow: {
                      id: "auto_walking",
                      subComponents: [
                        {
                          Label:
                            SView[SView.UIManager.languageInfo].languageObj.Animation
                              .Animation_Auto_WalkThrough,
                        },
                        {
                          Checkbox: {
                            id: "auto_walking_checkbox",
                            checkboxId: "auto_walking_checkbox_label", // 复选框input标签id
                          },
                        },
                      ],
                    },
                  },
                ],
              },
            },
          ],
        },
      },
      {
        Button: {
          id: "exitAnnotate",
          imgsrc: "images/moreMenu/translation/sview_button_exit.png",
          title: SView[SView.UIManager.languageInfo].languageObj.Prompt.Exit,
          onClick: "exitBottomMenu(AnimationMenu)",
        },
      },
    ],
  },
};

var AlignmentCameraBtn = {
  BottomMenu: {
    id: "alignmentCameraBtn",
    classes: ["SView-rightMenu"],
    subComponents: [
      {
        Button: {
          id: "alignmentCamera",
          imgsrc: "images/moreMenu/alignmentCamera.png",
          title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.AlignmentCamera,
          onClick:
            "commandClickFun('alignmentCamera','AlignmentCamera','SView.Commands.AlignmentCameraCommand')",
        },
      },
    ],
  },
};
