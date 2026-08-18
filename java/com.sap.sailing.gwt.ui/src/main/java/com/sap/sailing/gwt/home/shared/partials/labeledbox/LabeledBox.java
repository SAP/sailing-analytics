package com.sap.sailing.gwt.home.shared.partials.labeledbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class LabeledBox extends Composite {
    @UiField
    SpanElement headerTitleUi;
    @UiField(provided = true)
    Widget childUi;

    private static LabeledBoxUiBinder uiBinder = GWT.create(LabeledBoxUiBinder.class);

    interface LabeledBoxUiBinder extends UiBinder<Widget, LabeledBox> {
    }

    public LabeledBox(final String title, final Widget childUi) {
        this.childUi = childUi;
        initWidget(uiBinder.createAndBindUi(this));
        headerTitleUi.setInnerText(title);
    }

}
