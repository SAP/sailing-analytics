package com.sap.sailing.gwt.home.desktop.partials.media;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface MediaPageResources extends ClientBundle {
    public static final MediaPageResources INSTANCE = GWT.create(MediaPageResources.class);

    @Source("MediaPage.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String media();
        String dark();

        String photolist();
        String imggalleryitem();
    }
}
