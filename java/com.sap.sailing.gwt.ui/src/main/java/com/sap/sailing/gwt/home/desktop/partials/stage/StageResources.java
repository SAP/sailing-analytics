package com.sap.sailing.gwt.home.desktop.partials.stage;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface StageResources extends ClientBundle {
    public static final StageResources INSTANCE = GWT.create(StageResources.class);

    @Source("Stage.gss")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String countdown();
        String countdown_pane();
        String countdown_pane_count();
        String countdown_pane_unit();
        String stageteaser();
        String stageteaser_background();
        String stageteaser_background_image();
        String stageteaser_content();
        String stageteaser_content_header();
        String stageteaser_content_headerhidden();
        String stageteaser_content_header_subtitle();
        String stageteaser_content_header_title();
        String stageteaser_content_header_countdown();
        String stageteaser_content_band();
        String stageteaser_content_band_item();
        String stageteaser_content_band_text();
        String stageteaser_content_band_text_headline();
        String stageteaser_content_band_text_name();
        String stageteaser_content_band_action();
        String stageteaser_rightbound_bond_item();
        String swipercontainer();
        String swiperslide();
        String stage();
        String stage_teasers();
        String stage_controls();
        String stage_progress();
        String stage_progress_bar();
        String stage_dataBy_logo_container();
    }
    
}