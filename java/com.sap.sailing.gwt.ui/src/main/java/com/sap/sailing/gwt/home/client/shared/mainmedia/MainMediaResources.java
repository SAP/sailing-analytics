package com.sap.sailing.gwt.home.client.shared.mainmedia;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface MainMediaResources extends ClientBundle {
    public static final MainMediaResources INSTANCE = GWT.create(MainMediaResources.class);

    @Source("com/sap/sailing/gwt/home/client/shared/mainmedia/MainMedia.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String media();
        String media_swipecontainer();
        String media_swipewrapper();
        String media_swiperslide();
        String media_photo();
        String media_slideshow_controls();
        String media_slideshow_controlsnext();
        String media_slideshow_controlsprev();
        String media_slideshow_controlsfullsize();
        String mediavideos();
        String mainsection_header_title();
        String videopreview();
        String videopreview_title();
    }
}
