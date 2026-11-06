/* global SView M3D */
var RightMenu;
function initRightMenu() {
  RightMenu = {
    RightMenu: {
      id: "rightMenu",
      classes: ["SView-rightMenu"],
      subComponents: [
        {
          Button: {
            id: "resetButton",
            imgsrc: "images/rightMenuBtn/sview_home.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.RestoreView_li,
            onClick: "excuteReset('resetButton')",
          },
        },
        {
          Button: {
            id: "groupConferenceButton",
            imgsrc: "images/rightMenuBtn/sview_conferenceing.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.synergyMeeting,
            isShow: false,
            subComponents: [
              {
                Badge: {
                  id: "groupConferenceButtonBadge",
                  type: "withContentBadge", //设置徽标类型为文字类型
                  classes: [
                    "SView-badge-number",
                    "SView-badge-bottomRight", // 徽标所在位置：topLeft、topRight、bottomLeft、bottomRight
                  ],
                  content: "1", //若徽标类型为文字，该属性必填
                },
              },
            ],
            onClick: "showMeetingControlMenu('groupConferenceButton')",
          },
        },
        {
          Button: {
            id: "groupConferenceCharButton",
            imgsrc: "images/rightMenuBtn/sview_conference_chart.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.TextMessage,
            isShow: false,
            subComponents: [
              {
                Badge: {
                  id: "groupConferenceCharButtonBadge",
                  type: "dotBadge",
                  isShow: false,
                  classes: [
                    "SView-badge-dot",
                    "SView-badge-bottomRight", // 徽标所在位置：topLeft、topRight、bottomLeft、bottomRight
                  ],
                },
              },
            ],
            onClick: "showChatMenu('groupConferenceCharButton')",
          },
        },
        {
          Button: {
            id: "meetingApplyButton",
            imgsrc: "images/rightMenuBtn/meeting_apply.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.Apply,
            isShow: false,
            onClick: "Synergy.Main.instance().applyOperateAuth()",
          },
        },
        {
          Button: {
            id: "viewMenuBtn",
            imgsrc: "images/rightMenuBtn/button_view.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.ViewList,
            onClick: "showViewMenu('viewMenuBtn')",
          },
        },
        {
          Button: {
            id: "assemblyTree",
            imgsrc: "images/rightMenuBtn/sview_assembly.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.assembly,
            onClick: "showAssemblyTree('assemblyTree')",
          },
        },
        {
          Button: {
            id: "attributeInfo",
            imgsrc: "images/rightMenuBtn/sview_measure_attributes.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.modelproperties,
            onClick: "showAttributeInfo('attributeInfo')",
          },
        },
        {
          Button: {
            id: "meetingMainMenu",
            imgsrc: "images/rightMenuBtn/sview_conference.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.synergyMeeting,
            onClick: "showMeetingMenu('meetingMainMenu')",
            // "isShow": M3D.Utility.CookieHelper.getCookie("isSplit") && M3D.Utility.CookieHelper.getCookie("isSplit") == "true" ? false : true,
            isDisabled: true,
            isShow: false,
          },
        },
        {
          Button: {
            id: "moreMenu",
            imgsrc: "images/rightMenuBtn/button_menugroup.png",
            title: SView[SView.UIManager.languageInfo].languageObj.RightMenuTitle.mainMenu,
            isShow:
              M3D.Utility.CookieHelper.getCookie("isSplit") &&
              M3D.Utility.CookieHelper.getCookie("isSplit") == "true"
                ? false
                : true,
            onClick: "showBottomMenu('moreMenu')",
          },
        },
        {
          Button: {
            id: "annotateMenu",
            imgsrc: "images/moreMenu/button_note.png",
            title: SView[SView.UIManager.languageInfo].languageObj.setting.AnnotationData,
            isShow: true,
            onClick: "",
          },
        },
      ],
    },
  };
}
