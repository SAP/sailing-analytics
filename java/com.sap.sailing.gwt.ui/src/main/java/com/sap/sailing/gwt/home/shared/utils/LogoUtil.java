package com.sap.sailing.gwt.home.shared.utils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeUri;
import com.sap.sailing.gwt.home.communication.event.HasLogo;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sse.gwt.client.media.MediaMenuIcon;

/**
 * Utility class to set logo url on UI elements using a default logo as fallback if no logo is provided.
 */
public abstract class LogoUtil {

    private static final SafeUri DEFAULT_EVENT_LOGO = SharedHomeResources.INSTANCE.defaultEventLogoImage().getSafeUri();

    /**
     * Sets the logo provided by the given {@link EventViewDTO} (or default logo if <code>null</code>) as background
     * image and the {@link EventViewDTO#getDisplayName() events display name} as title of the given UI element.
     * 
     * @param eventLogoUi {@link Element} where the logo is set
     * @param event {@link EventViewDTO} providing data
     * @param logoMenuIcon take-down request menu button to update
     */
    public static void setEventLogo(Element eventLogoUi, HasLogo event, MediaMenuIcon logoMenuIcon) {
        String logoUrl = event.getLogoImage() != null ? event.getLogoImage().getSourceRef() : DEFAULT_EVENT_LOGO.asString();
        setLogoAndTitle(eventLogoUi, logoUrl, event.getDisplayName(), logoMenuIcon);
    }

    private static void setLogoAndTitle(Element logoUi, String logoUrl, String logoTitle, MediaMenuIcon logoMenuIcon) {
        logoUi.getStyle().setBackgroundImage("url(" + logoUrl + ")");
        logoUi.setTitle(logoTitle);
        logoMenuIcon.setData(logoTitle, logoUrl);
    }
}
