package com.sap.sailing.gwt.settings.client.whatsnew;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.LongSetting;

/**
 * Abstract setting implementation to store the number of characters in a change log when the last login occurred. This
 * is needed to determine whether to show a 'What's New' dialog to the user after login or not.
 */
public abstract class AbstractWhatsNewSettings extends AbstractGenericSerializableSettings {

    private static final long serialVersionUID = -8669347808456747826L;

    private transient LongSetting numberOfCharsOnLastLogin;

    protected AbstractWhatsNewSettings() {
        super();
    }

    protected AbstractWhatsNewSettings(final Long numberOfCharsOnLastLogin) {
        this();
        this.numberOfCharsOnLastLogin.setValue(numberOfCharsOnLastLogin);
    }

    @Override
    protected void addChildSettings() {
        numberOfCharsOnLastLogin = new LongSetting("numberOfCharsOnLastLogin", this, 0l);
    }

    public Long getNumberOfCharsOnLastLogin() {
        return numberOfCharsOnLastLogin.getValue();
    }

}