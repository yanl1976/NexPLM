
var AttrProperty = {
    Collapse: {
        id: "modelProperty",
        title: SView[SView.UIManager.languageInfo].languageObj.Property.Property,
        detailInfo: []
    }
}

var GeometryProperty = {
    Collapse: {
        id: "modelGeometry",
        title: SView[SView.UIManager.languageInfo].languageObj.Property.Geometric,
        detailInfo: []
    }
}

var FeatureProperty = {
    Collapse: {
        id: "modelModeling",
        title: SView[SView.UIManager.languageInfo].languageObj.Property.Features,
        detailInfo: []
    }
}


var AttributeDialog = {
    AttributeDialog: {
        classes: [
            "SView-attributeInfo-dialog"
        ],
        id: "attributeInfoDialog",
        closeBtnOnClick: "closeDialog('attributeInfoDialog', 'attributeInfo')",
        title: SView[SView.UIManager.languageInfo].languageObj.Property.Property,
        attributeData: [
            AttrProperty,
            GeometryProperty,
            FeatureProperty,
        ]
    }
}