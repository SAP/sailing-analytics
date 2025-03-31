package com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.user.client.DOM;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.common.client.BoatClassImageResolver;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.SharedSailorProfileResources;

public final class BoatclassElementBuilder {

    private BoatclassElementBuilder() {
    }

    /** @return a boatclass element for mobile display with the boatclass icon and the corresponding name */
    public static Element generateBoatclassElementForMobile(final BoatClassDTO boatclass) {
        SailorProfileMobileResources.INSTANCE.css().ensureInjected();
        Element elem = DOM.createDiv();
        SafeStylesBuilder safeStylesBuilder = new SafeStylesBuilder();
        safeStylesBuilder
                .appendTrustedString(SharedSailorProfileResources.TRUSTED_BUILD_BOAT_CLASS_ICON_WITH_NAME_STYLE_STRING);
        safeStylesBuilder
                .backgroundImage(BoatClassImageResolver.getBoatClassIconResource(boatclass.getName()).getSafeUri());
        elem.setInnerSafeHtml(SharedSailorProfileResources.TEMPLATES
                .buildBoatclassIconWithName(safeStylesBuilder.toSafeStyles(), boatclass.getName()));
        elem.addClassName(SailorProfileMobileResources.INSTANCE.css().boatclassWithNameEntry());
        return elem;
    }
}
