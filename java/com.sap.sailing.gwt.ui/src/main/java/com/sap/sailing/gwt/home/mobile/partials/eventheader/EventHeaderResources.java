package com.sap.sailing.gwt.home.mobile.partials.eventheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface EventHeaderResources extends ClientBundle {
    public static final EventHeaderResources INSTANCE = GWT.create(EventHeaderResources.class);

    @Source("EventHeader.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String eventheader();
        String eventheader_with_logo();
        String eventheader_info_subtitle();

        String eventheader_info();
        String eventheader_info_title();
        String eventheader_info_title_text();
        String eventheader_info_title_text_label();
        String eventheader_info_subtitlereduced();
        String eventheader_info_subtitle_logo();
        String eventheader_info_subtitle_info();
        String eventheader_info_subtitle_info_date();
        String eventheader_info_subtitle_info_location();
        String eventheader_info_subtitle_dataBy_logo_container();
    }
}
