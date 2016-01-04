package com.sap.sailing.gwt.home.mobile.partials.imagegallery;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.media.ImageDTO;

public class MobileGalleryPlayer extends ResizeComposite {
   
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    interface MyBinder extends UiBinder<Widget, MobileGalleryPlayer> {
    }

    @UiField DivElement mainSliderUi;

    private int selectedIdx;
    private boolean autoplay;

    public MobileGalleryPlayer(ImageDTO selected, Collection<? extends ImageDTO> images) {
        initWidget(uiBinder.createAndBindUi(this));
        selectedIdx = Math.max(selectedIdx, Util.indexOf(images, selected));
        for (ImageDTO i : images) {
            mainSliderUi.appendChild(createMainImgElement(i));
        }
    }
    
    private DivElement createMainImgElement(ImageDTO i) {
        DivElement img = Document.get().createDivElement();
        img.getStyle().setBackgroundImage("url(\"" + i.getSourceRef() + "\")");
        img.getStyle().setProperty("backgroundSize", "contain");
        img.getStyle().setProperty("backgroundRepeat", "no-repeat");
        img.getStyle().setProperty("backgroundPosition", "center");
        return img;
    }

    @Override
    protected void onLoad() {
        _onLoad();
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                gotoSlider(selectedIdx);
            }
        });
    }

    @Override
    public void onResize() {
        refreshSlider();
    }

    native void gotoSlider(int index) /*-{
	$wnd.$('.mainSlider').slick('slickGoTo', index, true);
	$wnd.$('.mainSlider').slick('setPosition');
    }-*/;

    native void refreshSlider() /*-{
	$wnd.$('.mainSlider').slick('setOption', null, null, true);
    }-*/;

    /**
     * JSNI wrapper that does setup the sliders
     *
     * @param uniqueId
     */
    native void _onLoad() /*-{
	$wnd.$('.mainSlider').slick({
	    lazyLoad : 'ondemand',
	    slidesToShow : 1,
	    slidesToScroll : 1,
	    arrows : false,
	    centerMode : false,
	    variableWidth : false,
	    adaptiveHeight : false,
	});
    }-*/;
    
    private native void _slickPlay() /*-{
    $wnd.$('.mainSlider').slick('slickPlay').slick('slickNext').slick(
            'setOption', 'autoplay', true); // workaround for bug https://github.com/kenwheeler/slick/issues/1446
    }-*/;
    
    private native void _slickPause() /*-{
        $wnd.$('.mainSlider').slick('slickPause').slick('setOption',
                'autoplay', false); // workaround for bug https://github.com/kenwheeler/slick/issues/1446
    }-*/;
    
    public void toggleAutoplay() {
        autoplay = !autoplay;
        if (autoplay) {
            _slickPlay();
        } else {
            _slickPause();
        }
    }
    
    public boolean isAutoplaying() {
        return autoplay;
    }
}
