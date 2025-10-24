package com.sap.sse.security.ui.client.premium;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.settings.generic.base.AbstractValueSetting;
import com.sap.sse.security.ui.client.premium.settings.AbstractSecuredValueSetting;

/**
 * Helps to create a Checkbox based on settings object as 'normal' or 'secured' one.
 * @param <T> 
 */
public abstract class AbstractCheckboxFactory<T> {

    public Widget createCheckbox(String label, AbstractValueSetting<T> valueSetting) {
        Widget checkbox;
        if (valueSetting instanceof AbstractSecuredValueSetting) {
            checkbox = createPremiumCheckbox(label, valueSetting);
        } else {
            checkbox = new CheckBox(label);
        }
        return checkbox;
    }

    public abstract PremiumCheckBox createPremiumCheckbox(String label, AbstractValueSetting<T> valueSetting);

}
