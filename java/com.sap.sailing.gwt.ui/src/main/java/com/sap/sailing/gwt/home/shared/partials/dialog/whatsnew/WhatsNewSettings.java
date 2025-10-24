package com.sap.sailing.gwt.home.shared.partials.dialog.whatsnew;

import com.sap.sailing.gwt.settings.client.whatsnew.AbstractWhatsNewSettings;

/**
 * AbstractWhatsNewSettings implementation to store the number of characters in the Home.html change log
 */
public class WhatsNewSettings extends AbstractWhatsNewSettings {

    private static final long serialVersionUID = 2132947244127105543L;

    public static final String PREF_NAME = "sailing.whatsnew";

    public WhatsNewSettings() {
        super();
    }

    public WhatsNewSettings(final Long numberOfCharsOnLastLogin) {
        super(numberOfCharsOnLastLogin);
    }

}
