package com.sap.sailing.gwt.home.shared.partials.placeholder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface PlaceholderResources extends ClientBundle {
    public static final PlaceholderResources INSTANCE = GWT.create(PlaceholderResources.class);

    @Source("InfoPlaceholder.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String content();
    }
}
