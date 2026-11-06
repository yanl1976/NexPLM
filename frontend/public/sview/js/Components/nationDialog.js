var Dialog = {
    "Dialog": {
        "classes": [
            "SView-dialog",
            "notation-dialog"
        ],
        "id": "notation-dialog",
        "onClick": "closeDialog('notation-dialog')",
        "title": {
            "classes": [
                "SView-dialog-title-name"
            ],
            "content": "批注"
        },
        "subComponents": [
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
                //"Input": {
                //    "type": "withNone",
                //    "id": "nation-Input",
                //    "classes": [
                //        "SView-input",
                //        "SView-input-withTitle",
                //        "SView-input-warning",
                //    ],
                //    // "title":"批注内容",
                //    "input": {
                //        "placeHolder": "请输入批注内容",
                //        "autoComplete": "off",
                //    },
                //    // "warning": "输入内容不能为空",
                //},
            },
        ],
        "buttons": [{
            "type": "basicButton",
            "classes": [
                "SView-button",
                "SView-button-primary"
            ],
            "content": "确定",
            "onClick": "closeDialog('notation-dialog')"
        },
        {
            "type": "basicButton",
            "classes": [
                "SView-button",
                "SView-button-default"
            ],
            "content": "关闭",
            "onClick": "closeDialog('notation-dialog')"
        }]
    }
}