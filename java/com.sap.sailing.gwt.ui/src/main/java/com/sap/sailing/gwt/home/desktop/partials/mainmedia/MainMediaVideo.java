package com.sap.sailing.gwt.home.desktop.partials.mainmedia;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.partials.mainmedia.MainMedia.MutualExclusionPlayHandler;
import com.sap.sailing.gwt.home.desktop.utils.LongNamesUtil;
import com.sap.sailing.gwt.home.shared.partials.videoplayer.VideoPlayer;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class MainMediaVideo extends Composite {

    
    interface MainMediaVideoUiBinder extends UiBinder<Widget, MainMediaVideo> {
    }
    
    private static MainMediaVideoUiBinder uiBinder = GWT.create(MainMediaVideoUiBinder.class);

    @UiField SpanElement videoTitle;
    @UiField Element videoTitleWrapper;
    @UiField SimplePanel videoHolderUi;

    final VideoPlayer vJs;

    public MainMediaVideo(SailingVideoDTO video, MutualExclusionPlayHandler exclusionPlayHandler, TakedownNoticeService takedownNoticeService) {
        vJs = exclusionPlayHandler == null
                ? new VideoPlayer(takedownNoticeService, video.getEventRef().getDisplayName())
                : new VideoPlayer(exclusionPlayHandler, takedownNoticeService, video.getEventRef().getDisplayName());
        MainMediaResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));

        // String youtubeUrl = "//www.youtube.com/embed/" + youtubeId + "?modestbranding=1&rel=0&autohide=1&fs=1";
        // if(!showInfo) {
        // youtubeUrl += "&showinfo=0";
        // }

        vJs.setVideo(video);
        vJs.addStyleName(MainMediaResources.INSTANCE.css().videopreview_videocontainer_video());
        videoHolderUi.setWidget(vJs);
        String eventName = video.getTitle();
        if(eventName == null || eventName.isEmpty()) {
            videoTitleWrapper.removeFromParent();
        } else {
            SafeHtml safeHtmlEventName = LongNamesUtil.breakLongName(eventName);
            videoTitle.setInnerSafeHtml(safeHtmlEventName); 
        }

        // addDomHandler(new MouseOverHandler() {
        //
        // @Override
        // public void onMouseOver(MouseOverEvent event) {
        // GWT.log("Duration: " + vJs.getDuration());
        // vJs.setCurrentTime(vJs.getDuration() / 2);
        // }
        // }, MouseOverEvent.getType());

    }
}
