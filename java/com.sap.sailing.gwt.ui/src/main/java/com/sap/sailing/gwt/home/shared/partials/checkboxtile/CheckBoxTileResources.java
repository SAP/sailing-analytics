package com.sap.sailing.gwt.home.shared.partials.checkboxtile;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface CheckBoxTileResources extends ClientBundle {
    
    public static final CheckBoxTileResources INSTANCE = GWT.create(CheckBoxTileResources.class);

    @Source("CheckBoxTile.gss")
    LocalCss css();
    
    public interface LocalCss extends CssResource {
        String container();
        String label();
        String checkbox();
        String loadingOverlay();
        String spinner();
    }
}
