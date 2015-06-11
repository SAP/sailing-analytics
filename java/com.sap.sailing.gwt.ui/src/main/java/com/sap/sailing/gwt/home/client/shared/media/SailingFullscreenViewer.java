package com.sap.sailing.gwt.home.client.shared.media;

import java.util.List;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.sap.sailing.gwt.ui.shared.media.SailingImageDTO;
import com.sap.sse.gwt.client.controls.carousel.ImageCarousel.FullscreenViewer;

public class SailingFullscreenViewer implements FullscreenViewer<SailingImageDTO> {
    private PopupPanel popup = new PopupPanel(true) {
        protected void onPreviewNativeEvent(NativePreviewEvent event) {
            if (event.getTypeInt() == Event.ONKEYDOWN && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
                hide();
            }
        };
    };

    public void show(SailingImageDTO selected, List<SailingImageDTO> images) {
        final SailingGalleryPlayer viewer = new SailingGalleryPlayer(selected, images);

        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                viewer.setHeight(Window.getClientHeight() + "px");
                viewer.setWidth(Window.getClientWidth() + "px");
                viewer.onResize();
            }
        });

        RootPanel.get().getElement().getStyle().setOverflow(Overflow.HIDDEN);
        viewer.setHeight(Window.getClientHeight() + "px");
        viewer.setWidth(Window.getClientWidth() + "px");
        viewer.onResize();
        popup.setWidget(viewer);

        popup.setPopupPositionAndShow(new PositionCallback() {
            @Override
            public void setPosition(int offsetWidth, int offsetHeight) {
                popup.center();
            }
        });
        popup.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                RootPanel.get().getElement().getStyle().setOverflow(Overflow.AUTO);
            }
        });
        viewer.setCloseCommand(new Command() {
            @Override
            public void execute() {
                popup.hide();
            }
        });
    }

}
