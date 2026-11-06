var ColorDialog = {
    "Dialog": {
        "id": "color_select_dialog",
        "content": "",
        //"contentId":"color-picker",
        "closeBtnOnClick": "closeDialog('color_select_dialog')",
        "title": SView[SView.UIManager.languageInfo].languageObj.RightMenu.RightMenu_SetColor,
        subComponents: [{
            Colorpicker: {
                id: "color-picker"
            }
        }],
        "buttons": [{
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

        }]
    }
}