package com.sap.sailing.gwt.managementconsole.partials.contextmenu;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.sap.sailing.gwt.managementconsole.resources.ManagementConsoleResources;
import com.sap.sse.security.ui.authentication.generic.resource.AuthenticationResources;

public interface ContextMenuResources extends AuthenticationResources {

    ContextMenuResources INSTANCE = GWT.create(ContextMenuResources.class);

    @Source({ ManagementConsoleResources.COLORS, "ContextMenu.gss" })
    Style style();

    public interface Style extends CssResource {

        @ClassName("context-menu-glass")
        String contextMenuGlass();

        @ClassName("context-menu")
        String contextMenu();

        String active();

        String content();

        String header();

        String separator();

        String items();

        @ClassName("primary-item")
        String primaryItem();

        String disabled();

    }
}
