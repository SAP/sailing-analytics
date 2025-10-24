package com.sap.sailing.gwt.home.desktop.partials.uploadpopup;

import java.util.List;
import java.util.function.BiConsumer;

import com.sap.sailing.gwt.home.desktop.partials.media.MediaPageResources;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.AbstractMediaUploadPopup;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class DesktopMediaUploadPopup extends AbstractMediaUploadPopup {

    public DesktopMediaUploadPopup(BiConsumer<List<ImageDTO>, List<VideoDTO>> updateImagesAndVideos, SailingServiceAsync sailingService) {
        super(updateImagesAndVideos, sailingService);
        MediaPageResources.INSTANCE.css().ensureInjected();
        addStyleName(MediaPageResources.INSTANCE.css().popup());
    }

    @Override
    protected String getTitleFromFileName(String fileName) {
        if (fileName == null) {
            fileName = "";
        }
        final String name;
        if (fileName.contains(".")) {
            name = fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            name = fileName;
        }
        return name;
    }
}
