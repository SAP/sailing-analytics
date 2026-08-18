package com.sap.sailing.gwt.home.shared.partials.multiselection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;

class SuggestedMultiSelectionNotificationToggle extends Composite implements HasValue<Boolean> {

    private static LocalUiBinder uiBinder = GWT.create(LocalUiBinder.class);

    interface LocalUiBinder extends UiBinder<Widget, SuggestedMultiSelectionNotificationToggle> {
    }
    
    @UiField SharedResources res;
    @UiField Label labelUi;
    @UiField CheckBox toggleButtonUi;
    
    public SuggestedMultiSelectionNotificationToggle(String label) {
        initWidget(uiBinder.createAndBindUi(this));
        labelUi.setText(label);
    }
    
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
        return toggleButtonUi.addValueChangeHandler(handler);
    }

    @Override
    public Boolean getValue() {
        return toggleButtonUi.getValue();
    }

    @Override
    public void setValue(Boolean value) {
        toggleButtonUi.setValue(value);
    }

    @Override
    public void setValue(Boolean value, boolean fireEvents) {
        toggleButtonUi.setValue(value, fireEvents);
    }

}
