package com.sap.sailing.gwt.home.mobile.partials.header;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface HeaderResources extends ClientBundle {
    public static final HeaderResources INSTANCE = GWT.create(HeaderResources.class);

    @Source("Header.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String header();
        String header_logo();
        String header_logo_image();
        String header_title();
        String header_search();
        String header_searchexpanded();
        String header_search_icon();
        String header_searchbox();
        String header_searchbox_form();
        String header_searchbox_form_input();
        String header_searchbox_form_button();
        String header_navigation();
        String header_navigationexpanded();
        String header_navigation_nav();
        String header_navigation_nav_list();
        String header_navigation_nav_list_item();
        String header_navigation_icon();
        String header_navigation_nav_sublist_item();
    }
}
