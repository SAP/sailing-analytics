package com.sap.sailing.gwt.home.client.place.event.partials.updatesBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface UpdatesBoxResources extends ClientBundle {
    public static final UpdatesBoxResources INSTANCE = GWT.create(UpdatesBoxResources.class);

    @Source("UpdatesBox.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String box();
        String box_header();
        String box_content();
        String box_content_item();
        String updatesbox();
        String updatesbox_item();
        String updatesbox_item_live();
        String updatesbox_itemlink();
        String updatesbox_item_icon();
        String updatesbox_item_content();
        String updatesbox_item_content_text();
        String updatesbox_item_content_text_boatclass();
    }
}
