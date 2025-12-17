package com.sap.sailing.gwt.home.mobile.partials.socialfooter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface SocialFooterResources extends ClientBundle {
    public static final SocialFooterResources INSTANCE = GWT.create(SocialFooterResources.class);

    @Source("SocialFooter.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String socialfooter();
        String socialfooter_heading();
        String socialfooter_item();
        String socialfooter_item_contentwrapper();
        String socialfooter_item_icon();
        String socialfooter_item_text();
        String socialfooter_item_text_top();
        String socialfooter_item_text_bottom();
        String socialfooter_items();
    }
}
