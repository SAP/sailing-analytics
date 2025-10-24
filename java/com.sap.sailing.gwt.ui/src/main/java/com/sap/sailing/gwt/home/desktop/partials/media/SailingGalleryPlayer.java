package com.sap.sailing.gwt.home.desktop.partials.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.HasSelectionChangedHandlers;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;
import com.sap.sse.gwt.common.CommonSharedResources;

public class SailingGalleryPlayer extends ResizeComposite implements HasSelectionChangedHandlers, HasClickHandlers {
    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    interface MyBinder extends UiBinder<Widget, SailingGalleryPlayer> {
    }

    @UiField DivElement mainSliderUi;
    @UiField DivElement subSliderUi;

    private boolean autoplay;
    private int selectedIdx;
    private final List<SailingImageDTO> images;

    public SailingGalleryPlayer(SailingImageDTO selected, Collection<SailingImageDTO> images, TakedownNoticeService takedownNoticeService) {
        initWidget(uiBinder.createAndBindUi(this));
        selectedIdx = Math.max(0, Util.indexOf(images, selected));
        this.images = new ArrayList<SailingImageDTO>(images.size());
        for (SailingImageDTO i : images) {
            final DivElement mainImgElement = createMainImgElement(i);
            final MediaMenuIcon mmi = new MediaMenuIcon(takedownNoticeService, "takedownRequestForEventGalleryImage");
            mmi.setFontSize("20px");
            RootPanel.get().add(mmi); // required to attach to the DOM and let the mouse click handler become effective
            mmi.setData(i.getEventLink().getDisplayName(), i.getSourceRef());
            mainImgElement.appendChild(mmi.getElement());
            mainSliderUi.appendChild(mainImgElement);
            subSliderUi.appendChild(createThumbImgElement(i));
            this.images.add(i);
        }
    }

    @Override
    public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
        return addHandler(handler, SelectionChangeEvent.getType());
    }
    
    @Override
    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        mainSliderUi.getStyle().setCursor(Cursor.POINTER);
        return addHandler(handler, ClickEvent.getType());
    }

    public SailingImageDTO getSelectedImage() {
        return images.get(selectedIdx);
    }

    private ImageElement createThumbImgElement(SailingImageDTO i ) {
        ImageElement img = Document.get().createImageElement();
        img.setAttribute("src", i.getSourceRef());
        img.getStyle().setHeight(103, Unit.PX);
        return img;
    }
    
    private DivElement createMainImgElement(SailingImageDTO i) {
        DivElement img = Document.get().createDivElement();
        img.addClassName(CommonSharedResources.INSTANCE.mainCss().media_wrapper());
        img.getStyle().setBackgroundImage("url(\"" + i.getSourceRef() + "\")");
        img.getStyle().setProperty("backgroundSize", "contain");
        img.getStyle().setProperty("backgroundRepeat", "no-repeat");
        img.getStyle().setProperty("backgroundPosition", "center");
        return img;
    }
    
    private void updateCurrentSlide(int currentIndex) {
        this.selectedIdx = currentIndex;
        SelectionChangeEvent.fire(this);
    }
    
    private void onMainSlideClicked(NativeEvent nativeEvent) {
        ClickEvent.fireNativeEvent(nativeEvent, this);
    }

    @Override
    protected void onLoad() {
        _onLoad(this);
        mainSliderUi.getFirstChild().<Element> cast().setTabIndex(0);
        this.focus();
    }

    @Override
    public void onResize() {
        refreshSlider();
    }

    native void refreshSlider() /*-{
	$wnd.$('.mainSlider').slick('setOption', null, null, true);
	$wnd.$('.subSlider').slick('setOption', null, null, true);
    }-*/;

    /**
     * JSNI wrapper that does setup the sliders
     *
     * @param uniqueId
     */
    native void _onLoad(SailingGalleryPlayer player) /*-{
	$wnd
		.$('.mainSlider')
		.slick(
			{
			    lazyLoad : 'ondemand',
			    slidesToShow : 1,
			    slidesToScroll : 1,
			    arrows : false,
			    centerMode : false,
			    speed : 500,
			    autoplaySpeed : 15000,
			    pauseOnHover : true,
			    variableWidth : false,
			    adaptiveHeight : false,
			    asNavFor : '.subSlider',
			    initialSlide : this.@com.sap.sailing.gwt.home.desktop.partials.media.SailingGalleryPlayer::selectedIdx
			});
	$wnd.$('.mainSlider').on('beforeChange',
            $entry(function(event, slick, currentSlide, nextSlide) {
                player.@com.sap.sailing.gwt.home.desktop.partials.media.SailingGalleryPlayer::updateCurrentSlide(I)(nextSlide);
            }));
        $wnd.$('.mainSlider').on('click',
            $entry(function(event) {
                player.@com.sap.sailing.gwt.home.desktop.partials.media.SailingGalleryPlayer::onMainSlideClicked(Lcom/google/gwt/dom/client/NativeEvent;)(event);
            }));
	$wnd
		.$('.subSlider')
		.slick(
			{
			    lazyLoad : 'ondemand',
			    infinite : true,
			    slidesToShow : 1,
			    slidesToScroll : 1,

			    speed : 500,
			    swipeToSlide : true,
			    centerMode : true,
			    asNavFor : '.mainSlider',
			    arrows : false,
			    variableWidth : true,
			    focusOnSelect : true,
			    draggable : false,
			    initialSlide : this.@com.sap.sailing.gwt.home.desktop.partials.media.SailingGalleryPlayer::selectedIdx
			});
    }-*/;

    private native void _slickPlay() /*-{
	$wnd.$('.subSlider').slick('slickPlay').slick('slickNext').slick(
		'setOption', 'autoplay', true); // workaround for bug https://github.com/kenwheeler/slick/issues/1446
        $wnd.$('.subSlider').slick('slickPlay').slick('slickNext').slick(
                'setOption', 'autoplaySpeed', 15000); // workaround for bug https://github.com/kenwheeler/slick/issues/1446
    }-*/;

    private native void _slickPause() /*-{
	$wnd.$('.subSlider').slick('slickPause').slick('setOption', 'autoplay',
		false); // workaround for bug https://github.com/kenwheeler/slick/issues/1446
    }-*/;

    public void toggleAutoplay() {
        autoplay = !autoplay;
        if (autoplay) {
            _slickPlay();
        } else {
            _slickPause();
        }
    }

    public void focus() {
        mainSliderUi.getFirstChild().<Element> cast().focus();
    }

    @Override
    protected void onUnload() {
        if (isAutoplaying())
            toggleAutoplay();
    }
    public boolean isAutoplaying() {
        return autoplay;
    }
}
