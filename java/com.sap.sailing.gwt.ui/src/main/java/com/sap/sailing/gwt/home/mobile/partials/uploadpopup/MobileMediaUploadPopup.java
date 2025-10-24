package com.sap.sailing.gwt.home.mobile.partials.uploadpopup;

import java.util.List;
import java.util.function.BiConsumer;

import com.sap.sailing.gwt.home.mobile.places.event.media.MediaViewResources;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.AbstractMediaUploadPopup;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class MobileMediaUploadPopup extends AbstractMediaUploadPopup {

    public MobileMediaUploadPopup(BiConsumer<List<ImageDTO>, List<VideoDTO>> updateImagesAndVideos, SailingServiceAsync sailingService) {
        super(updateImagesAndVideos, sailingService);
        MediaViewResources.INSTANCE.css().ensureInjected();
        addStyleName(MediaViewResources.INSTANCE.css().popup());
    }

    @Override
    protected String getTitleFromFileName(String fileName) {
        return "";
    }
}
