package com.sap.sailing.gwt.ui.adminconsole.desktop.app.footer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface FooterResources extends ClientBundle {
    public static final FooterResources INSTANCE = GWT.create(FooterResources.class);

    @Source("Footer.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
    }
}
