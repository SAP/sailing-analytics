package com.sap.sailing.gwt.ui.adminconsole.whatsnew;

import com.sap.sailing.gwt.settings.client.whatsnew.AbstractWhatsNewSettings;

/**
 * AbstractWhatsNewSettings implementation to store the number of characters in the AdminConsole.html change log
 */
public class WhatsNewSettings extends AbstractWhatsNewSettings {

    private static final long serialVersionUID = -8691759258240153877L;

    public static final String PREF_NAME = "sailing.whatsnew.admin";

    public WhatsNewSettings() {
        super();
    }

    public WhatsNewSettings(final Long numberOfCharsOnLastLogin) {
        super(numberOfCharsOnLastLogin);
    }

}
