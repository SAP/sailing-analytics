package com.sap.sailing.gwt.ui.client.media;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.desktop.partials.media.MediaPageResources;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class GalleryImageHolder extends Composite implements HasClickHandlers {

    private static VideoThumbnailUiBinder uiBinder = GWT.create(VideoThumbnailUiBinder.class);
    private final String imageSourceRef;
    private final Date imageCreateAt;
    interface VideoThumbnailUiBinder extends UiBinder<Widget, GalleryImageHolder> {
    }

    @UiField
    DivElement imageHolderUi;
    @UiField
    Anchor deleteAnchor;
    @UiField
    DivElement overlay;
    @UiField(provided=true)
    MediaMenuIcon imageMenuButton;

    public GalleryImageHolder(ImageDTO image, ClickHandler deleteHandler, TakedownNoticeService takedownNoticeService, String eventName) {
        imageMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForEventGalleryImage");
        initWidget(uiBinder.createAndBindUi(this));
        SharedResources.INSTANCE.mainCss().ensureInjected();
        MediaPageResources.INSTANCE.css().ensureInjected();
        SharedHomeResources.INSTANCE.sharedHomeCss().ensureInjected();
        this.imageSourceRef = image.getSourceRef();
        this.imageCreateAt = image.getCreatedAtDate();
        deleteAnchor.addClickHandler(deleteHandler);
        imageHolderUi.getStyle()
                .setBackgroundImage("url(\"" + UriUtils.fromString(image.getSourceRef()).asString() + "\")");
        imageMenuButton.setData(eventName, image.getSourceRef());
    }

    public boolean isImage(ImageDTO image) {
        return image != null && image.getSourceRef().equals(imageSourceRef)
                && image.getCreatedAtDate().equals(imageCreateAt);
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public void setManageable(boolean manageable) {
        if (manageable) {
            overlay.getStyle().setDisplay(Display.BLOCK);
        } else {
            overlay.getStyle().setDisplay(Display.NONE);
        }
    }

}
