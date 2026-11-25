package com.sap.sailing.gwt.managementconsole.partials.header;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sse.security.ui.authentication.generic.resource.AuthenticationResources;

public interface HeaderResources extends AuthenticationResources {

    HeaderResources INSTANCE = GWT.create(HeaderResources.class);

    @Source({ ManagementConsoleResources.COLORS, "Header.gss" })
    Style style();

    @Source("../../resources/sap-logo.png")
    ImageResource sapLogo();

    interface Style extends CssResource {
        String header();

        String logo();

        String menu();

        @ClassName("menu-mobile")
        String menuMobile();

        String active();

        @ClassName("item-active")
        String itemActive();

        String actions();

        String item();

        String account();

        String separator();

    }
}
