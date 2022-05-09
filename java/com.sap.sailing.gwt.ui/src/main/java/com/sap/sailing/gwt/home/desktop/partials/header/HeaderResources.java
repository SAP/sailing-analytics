package com.sap.sailing.gwt.home.desktop.partials.header;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;
import com.sap.sse.security.ui.authentication.generic.resource.AuthenticationResources;

public interface HeaderResources extends AuthenticationResources {
    public static final HeaderResources INSTANCE = GWT.create(HeaderResources.class);

    @Source("Header.gss")
    LocalCss css();

    @Source("navigation-icon.svg")
    @MimeType("image/svg+xml")
    DataResource navigation();
    
    @Source("navigation-icon_hover.svg")
    @MimeType("image/svg+xml")
    DataResource navigation_hover();
    
    @Source("crown.png")
    @MimeType("image/png")
    DataResource crown();
    
    @Source("icon_premium.svg")
    @MimeType("image/svg+xml")
    DataResource premiumIcon();

    public interface LocalCss extends CssResource {
        String siteheader();
        String siteheader_logo();
        String siteheader_logo_image();
        String siteheader_logo_title();
        String sitenavigation();
        String sitenavigation_link();
        String sitenavigation_linkactive();
        String sitenavigation_search();
        String search();
        String search_textfield();
        String search_button();
        String submit();
        String user_menu_button();
        String loggedin();
        String open();
        String sitenavigation_fixed();
        String sitenavigation_dropdown_container();
        String header_navigation_icon();
        String header_navigation_iconactive();
        String subscriptions();
        String user_menu_premium();
        String premium_feature();
        String premium_hint();
    }
}
