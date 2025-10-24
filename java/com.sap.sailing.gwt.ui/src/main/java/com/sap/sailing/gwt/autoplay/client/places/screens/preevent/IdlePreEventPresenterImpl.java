package com.sap.sailing.gwt.autoplay.client.places.screens.preevent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.home.shared.utils.Countdown;
import com.sap.sailing.gwt.home.shared.utils.Countdown.CountdownListener;
import com.sap.sailing.gwt.home.shared.utils.Countdown.RemainingTime;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.media.ImageDTO;

public class IdlePreEventPresenterImpl extends AutoPlayPresenterConfigured<IdlePreEventPlace>
        implements IdlePreEventNextView.IdlePreEventNextPresenter, CountdownListener {
    private static final int IMAGE_SWITCH_DELAY = 10000;
    private final IdlePreEventNextView view;
    private Timer updateImage;
    private Timer updateText;
    private Countdown countdown;
    private Date currentStartTime;

    public IdlePreEventPresenterImpl(IdlePreEventPlace place, AutoPlayClientFactory clientFactory,
            IdlePreEventNextView slide2ViewImpl) {
        super(place, clientFactory);
        this.view = slide2ViewImpl;
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        updateEventImage();
        view.startingWith(this, panel);
        updateImage = new Timer() {

            @Override
            public void run() {
                updateEventImage();
            }
        };
        updateImage.scheduleRepeating(IMAGE_SWITCH_DELAY);

        updateText = new Timer() {

            @Override
            public void run() {
                updateCountdown();
            }

        };
        updateText.scheduleRepeating(1000);
    }

    protected void updateCountdown() {
        EventDTO event = getSlideCtx().getEvent();
        if (event.startDate != null) {
            if (!event.startDate.equals(currentStartTime)) {
                currentStartTime = event.startDate;
                if (countdown != null) {
                    countdown.cancel();
                }
                this.countdown = new Countdown(new MillisecondsTimePoint(currentStartTime), this);
            }
        } else {
            view.setStartIn(null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        updateImage.cancel();
        updateText.cancel();
        countdown.cancel();
    }

    protected void updateEventImage() {
        List<SailingImageDTO> teaserHighlight = new ArrayList<>();
        List<SailingImageDTO> bigScreenImages = new ArrayList<>();
        for (SailingImageDTO imageDTO : getSlideCtx().getEvent().getImages()) {
            if (imageDTO.getTags().contains(MediaTagConstants.BIGSCREEN.getName())) {
                bigScreenImages.add(imageDTO);
            } else if (imageDTO.getTags().contains(MediaTagConstants.TEASER.getName())) {
                teaserHighlight.add(imageDTO);
            } else if (imageDTO.getTags().contains(MediaTagConstants.HIGHLIGHT.getName())) {
                teaserHighlight.add(imageDTO);
            }
        }
        List<SailingImageDTO> usedImages;
        if (bigScreenImages.isEmpty()) {
            if (teaserHighlight.isEmpty()) {
                usedImages = getSlideCtx().getEvent().getImages();
            } else {
                usedImages = teaserHighlight;
            }
        } else {
            usedImages = bigScreenImages;
        }

        if (!usedImages.isEmpty()) {
            int selected = 0;
            if (usedImages.size() > 1) {
                Random r = new Random();
                selected = r.nextInt(usedImages.size());
            }
            ImageDTO imageToUseDTO = usedImages.get(selected);
            if (imageToUseDTO != null) {
                final StringBuilder thumbnailUrlBuilder = new StringBuilder("url('");
                thumbnailUrlBuilder.append(UriUtils.fromString(imageToUseDTO.getSourceRef()).asString());
                thumbnailUrlBuilder.append("')");
                view.setBackgroudImage(thumbnailUrlBuilder.toString());
            }
        }
    }

    @Override
    public void changed(RemainingTime major, RemainingTime minor) {
        view.setStartIn(major);
    }
}
