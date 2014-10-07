package com.sap.sailing.gwt.ui.client.media;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.gwt.ui.client.media.shared.AbstractMediaPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.VideoSynchPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.WithWidget;
import com.sap.sse.gwt.client.player.Timer;

public class VideoYoutubePlayer extends AbstractMediaPlayer implements VideoSynchPlayer, MediaSynchAdapter, WithWidget {

    private interface DeferredAction {
        void execute();
    }

    private final Panel videoContainer;

    private static int videoCounter;

    private final TimePoint raceStartTime;

    private final Timer raceTimer;

    private EditFlag editFlag;

    private YoutubeVideoControl videoControl;

    /**
     * Required to indicate whether this control has been requested to close. In this case it must not call any player
     * functions anymore not to cause null-access error due to missing DOM elements.
     * 
     */
    private boolean closing = false;

    private final List<DeferredAction> deferredActions = new ArrayList<DeferredAction>();

    public VideoYoutubePlayer(final MediaTrack videoTrack, TimePoint raceStartTime, final boolean showControls,
            Timer raceTimer) {
        super(videoTrack);
        this.raceTimer = raceTimer;
        this.raceStartTime = raceStartTime;

        this.videoContainer = new SimplePanel();
        this.videoContainer.setStyleName("Youtube-Video-Panel");
        final String videoContainerId = "videoContainer-" + videoTrack.url + ++videoCounter;
        this.videoContainer.getElement().setId(videoContainerId);
        this.videoContainer
                .getElement()
                .setInnerText(
                        "When the Youtube video doesn't show up, click the popout button at the upper right corner to open the video in a dedicated browser window.");
        this.videoContainer.addAttachHandler(new Handler() {

            @Override
            public void onAttachOrDetach(AttachEvent event) {
                // The videoContainer must be attached to the DOM before the Youtube player can be created.
                // If not, Youtube API won't find the container which is referenced by its id attribute.
                if (event.isAttached()) {
                    videoControl = new YoutubeVideoControl(videoTrack.url, videoContainerId, showControls);
                    for (DeferredAction deferredAction : deferredActions) {
                        deferredAction.execute();
                    }
                } else {
                    closing = true;
                }
            }
        });

    }

    private void defer(DeferredAction deferredAction) {
        deferredActions.add(deferredAction);
    }

    @Override
    public void shutDown() {
        if (!this.closing) {
            if (videoControl != null) {
                videoControl.pause();
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        shutDown();
                    }
                });
            }
        }
    }

    @Override
    public long getOffset() {
        return getMediaTrack().startTime.asMillis() - raceStartTime.asMillis();
    }

    @Override
    public void changeOffsetBy(long delta) {
        getMediaTrack().startTime = getMediaTrack().startTime.plus(delta);
        forceAlign();
    }

    @Override
    public void updateOffset() {
        getMediaTrack().startTime = new MillisecondsTimePoint(raceTimer.getTime().getTime()
                - getCurrentMediaTimeMillis());
    }

    @Override
    public void pauseRace() {
        raceTimer.pause();
    }

    @Override
    public void setControlsVisible(final boolean isVisible) {
        if (!this.closing) {
            if (videoControl != null) {
                videoControl.setControlsVisible(isVisible);
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        setControlsVisible(isVisible);
                    }
                });
            }
        }
    }

    @Override
    public boolean isMediaPaused() {
        if (this.closing) {
            return true;
        }
        if (videoControl != null) {
            return videoControl.isPaused();
        } else {
            return true;
        }

    }

    @Override
    public void pauseMedia() {
        if (!isEditing() && !this.closing) {
            if (videoControl != null) {
                videoControl.pause();
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        pauseMedia();
                    }
                });
            }
        }
    }

    @Override
    public void playMedia() {
        if (!isEditing() && !this.closing) {
            if (videoControl != null) {
                videoControl.play();
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        playMedia();
                    }
                });
            }
        }
    }

    @Override
    public double getDuration() {
        if (videoControl != null && !this.closing) {
            return videoControl.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public double getCurrentMediaTime() {
        if (videoControl != null && !this.closing) {
            return videoControl.getCurrentTime();
        } else {
            return 0;
        }
    }

    @Override
    public void setCurrentMediaTime(final double mediaTime) {
        if (!this.closing) {
            if (videoControl != null) {
                videoControl.setCurrentTime(mediaTime);
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        setCurrentMediaTime(mediaTime);
                    }
                });
            }
        }
    }

    @Override
    public void setPlaybackSpeed(final double newPlaySpeedFactor) {
        if (!this.closing) {
            if (videoControl != null) {
                videoControl.setPlaybackSpeed(newPlaySpeedFactor);
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        setPlaybackSpeed(newPlaySpeedFactor);
                    }
                });
            }
        }
    }

    @Override
    public void setMuted(final boolean isToBeMuted) {
        if (!this.closing) {
            if (videoControl != null) {
                videoControl.setMuted(isToBeMuted);
            } else {
                defer(new DeferredAction() {
                    public void execute() {
                        setMuted(isToBeMuted);
                    }
                });
            }
        }
    }

    @Override
    protected void alignTime() {
        if (!isEditing()) {
            super.alignTime();
        }
    }

    @Override
    public Widget getWidget() {
        return videoContainer;
    }

    @Override
    public void setEditFlag(EditFlag editFlag) {
        this.editFlag = editFlag;
    }

    private boolean isEditing() {
        return editFlag != null && editFlag.isEditing();
    }

}
