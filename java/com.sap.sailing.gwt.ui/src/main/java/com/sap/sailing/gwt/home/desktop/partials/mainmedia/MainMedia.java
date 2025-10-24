package com.sap.sailing.gwt.home.desktop.partials.mainmedia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.partials.media.SailingFullscreenViewer;
import com.sap.sailing.gwt.ui.client.media.VideoJSPlayer;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.gwt.client.controls.carousel.ImageCarousel;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class MainMedia extends Composite {

    public static final class MutualExclusionPlayHandler {

        private List<VideoJSPlayer> players = new ArrayList<>();

        public void register(VideoJSPlayer videoJSPlayer) {
            players.add(videoJSPlayer);
            videoJSPlayer.addPlayHandler(event -> {
                stopOthers((VideoJSPlayer)event.getSource());
            });
            
        }

        public void stopOthers(VideoJSPlayer me) {
            players.forEach(player -> {
                if (!player.equals(me)) {
                    player.pause();
                }
            });
        }
    }

    private static final int MAX_VIDEO_COUNT = 3;

    @UiField HTMLPanel videosPanel;
    @UiField SimplePanel imageCarouselHolderUi;

    interface MainMediaUiBinder extends UiBinder<Widget, MainMedia> {
    }

    private static MainMediaUiBinder uiBinder = GWT.create(MainMediaUiBinder.class);

    private final DesktopPlacesNavigator navigator;
    
    private final MutualExclusionPlayHandler exclusionPlayHandler = new MutualExclusionPlayHandler();

    public MainMedia(DesktopPlacesNavigator navigator) {
        this.navigator = navigator;
        MainMediaResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void setData(Collection<SailingVideoDTO> videos, ArrayList<SailingImageDTO> photos, TakedownNoticeService takedownNoticeService) {
        final ImageCarousel<SailingImageDTO> imageCarousel = new ImageCarousel<>();
        imageCarousel.registerFullscreenViewer(new SailingFullscreenViewer(navigator, takedownNoticeService));
        Iterator<SailingVideoDTO> videoIterator = videos.iterator();
        int videoCount = 0;
        while (videoCount < MAX_VIDEO_COUNT && videoIterator.hasNext()) {
            SailingVideoDTO videoDTO = videoIterator.next();
            MainMediaVideo video = new MainMediaVideo(videoDTO, exclusionPlayHandler, takedownNoticeService);
            videosPanel.add(video);
            videoCount++;
        }
        for (SailingImageDTO image : photos) {
            imageCarousel.addImage(image);
        }
        imageCarousel.setInfiniteScrolling(true);
        imageCarouselHolderUi.setWidget(imageCarousel);
    }
}
