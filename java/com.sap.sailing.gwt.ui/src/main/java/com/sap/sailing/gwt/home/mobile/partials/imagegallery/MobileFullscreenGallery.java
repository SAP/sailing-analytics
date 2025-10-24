package com.sap.sailing.gwt.home.mobile.partials.imagegallery;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.gwt.client.controls.carousel.ImageCarousel.FullscreenViewer;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class MobileFullscreenGallery implements FullscreenViewer<SailingImageDTO>{
    
    private static MobileFullscreenGalleryUiBinder uiBinder = GWT.create(MobileFullscreenGalleryUiBinder.class);

    interface MobileFullscreenGalleryUiBinder extends UiBinder<LayoutPanel, MobileFullscreenGallery> {
    }
    
    interface Style extends CssResource {
        String popup();
        String content();
        String is_autoplaying();
    }
    
    @UiField Style style;
    @UiField(provided=true) SimpleLayoutPanel contentUi = new FullscreenContentPanel();
    @UiField Image autoplayActionUi;
    
    private final FullscreenPopupPanel popup = new FullscreenPopupPanel();
    private final LayoutPanel mainPanel;
    private MobileGalleryPlayer playerUi = null;
    private final TakedownNoticeService takedownNoticeService;
    
    public MobileFullscreenGallery(TakedownNoticeService takedownNoticeService) {
        popup.setWidget(mainPanel = uiBinder.createAndBindUi(this));
        popup.addStyleName(style.popup());
        this.takedownNoticeService = takedownNoticeService;
    }
    
    @UiHandler("closeActionUi")
    void onCloseActionClicked(ClickEvent event) {
        popup.hide();
    }
    
    @UiHandler("autoplayActionUi")
    void onAutoplayActionClicked(ClickEvent event) {
        if (playerUi != null) {
            playerUi.toggleAutoplay();
            autoplayActionUi.setStyleName(style.is_autoplaying(), playerUi.isAutoplaying());
        }
    }
    
    public HandlerRegistration addCloseHandler(CloseHandler<PopupPanel> handler) {
        return popup.addCloseHandler(handler);
    }
    
    @Override
    public void show(SailingImageDTO selectedImage, Collection<SailingImageDTO> imageList) {
        contentUi.setWidget(playerUi = new MobileGalleryPlayer(selectedImage, imageList, takedownNoticeService));
        autoplayActionUi.setStyleName(style.is_autoplaying(), playerUi.isAutoplaying());
        contentUi.getWidget().getElement().addClassName(style.content());
        mainPanel.onResize();
        popup.showPopupPanel();
    }
    
    private class FullscreenPopupPanel extends PopupPanel {
        private FullscreenPopupPanel() {
            super(false, true);
            this.addCloseHandler(new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                    RootPanel.get().getElement().getStyle().setOverflow(Overflow.AUTO);
                    contentUi.getWidget().getElement().removeClassName(style.content());
                }
            });
        }
        
        protected void onPreviewNativeEvent(NativePreviewEvent event) {
            if (event.getTypeInt() == Event.ONKEYDOWN && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                FullscreenPopupPanel.this.hide();
                event.cancel();
                return;
            }
            event.consume();
        };
        
        private void showPopupPanel() {
            RootPanel.get().getElement().getStyle().setOverflow(Overflow.HIDDEN);
            FullscreenPopupPanel.this.show();
        }
    }
    
    private class FullscreenContentPanel extends SimpleLayoutPanel {
        private HandlerRegistration windowResizeHandler;
        
        @Override
        public void onResize() {
            popup.getWidget().getElement().getStyle().setWidth(Window.getClientWidth(), Unit.PX);
            popup.getWidget().getElement().getStyle().setHeight(Window.getClientHeight(), Unit.PX);
            super.onResize();
        }
        
        @Override
        protected void onLoad() {
            windowResizeHandler = Window.addResizeHandler(new ResizeHandler() {
                @Override
                public void onResize(ResizeEvent event) {
                    mainPanel.onResize();
                }
            });
        }
        
        @Override
        protected void onUnload() {
            if (windowResizeHandler != null) {
                windowResizeHandler.removeHandler();
            }
        }
    }
}
