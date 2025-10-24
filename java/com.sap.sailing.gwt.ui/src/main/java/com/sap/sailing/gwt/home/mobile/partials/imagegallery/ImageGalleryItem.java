package com.sap.sailing.gwt.home.mobile.partials.imagegallery;

import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.mobile.places.event.media.MediaViewResources;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class ImageGalleryItem extends Composite implements HasClickHandlers {

    private static ImageGalleryItemUiBinder uiBinder = GWT.create(ImageGalleryItemUiBinder.class);

    interface ImageGalleryItemUiBinder extends UiBinder<Widget, ImageGalleryItem> {
    }

    @UiField
    DivElement imageUi;
    @UiField
    DivElement overlayUi;
    @UiField
    Button editButtonUi;
    @UiField
    Button deleteButtonUi;
    @UiField(provided=true)
    MediaMenuIcon mediaMenuIcon;
    
    public ImageGalleryItem(final ImageDTO image, final Consumer<ImageDTO> deleteImage, TakedownNoticeService takedownNoticeService, String eventName) {
        MediaViewResources.INSTANCE.css().ensureInjected();
        mediaMenuIcon = new MediaMenuIcon(takedownNoticeService, "takedownRequestForEventGalleryImage");
        initWidget(uiBinder.createAndBindUi(this));
        String backgroundUri = UriUtils.fromString(image.getSourceRef()).asString();
        mediaMenuIcon.setData(eventName, backgroundUri);
        imageUi.getStyle().setBackgroundImage("url('" + backgroundUri + "')");
        overlayUi.getStyle().setVisibility(Visibility.HIDDEN);
        editButtonUi.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                event.stopPropagation();
            }
        });
        editButtonUi.setVisible(false);
        deleteButtonUi.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                event.stopPropagation();
                // TODO translate
                if (Window.confirm("Do you really want to delete this Image?")) {
                    deleteImage.accept(image);
                }
            }
        });
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    public void manageMedia(boolean managed) {
        if (managed) {
            overlayUi.getStyle().setVisibility(Visibility.VISIBLE);
        } else {
            overlayUi.getStyle().setVisibility(Visibility.HIDDEN);
        }
    }

}
