package com.sap.sailing.gwt.home.mobile.partials.searchresult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface SearchResultResources extends ClientBundle {
    public static final SearchResultResources INSTANCE = GWT.create(SearchResultResources.class);

    @Source("SearchResult.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String searchresult();
        String searchresult_header();
        String searchresult_header_form();
        String searchresult_header_form_label();
        String searchresult_header_form_inputwrapper();
        String searchresult_header_form_input();
        String searchresult_header_form_button();
        String searchresult_amount();
        String searchresult_item();
        String searchresult_item_link();
        String searchresult_item_headline();
        String searchresult_item_event();
        String searchresult_item_event_info();
        String searchresult_item_event_link();
        String searchresult_item_event_separator();
        String searchresult_item_arrow();
    }
}
