package com.sap.sailing.gwt.ui.client.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.media.client.Video;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.FileStorageServiceConnectionTestObservable;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer;
import com.sap.sailing.gwt.ui.client.media.popup.PopoutWindowPlayer.PlayerCloseListener;
import com.sap.sailing.gwt.ui.client.media.popup.VideoJSWindowPlayer;
import com.sap.sailing.gwt.ui.client.media.popup.YoutubeWindowPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.MediaPlayer;
import com.sap.sailing.gwt.ui.client.media.shared.MediaSynchPlayer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaType;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.formfactor.DeviceDetector;
import com.sap.sse.gwt.client.player.PlayStateListener;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.AbstractComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;

public class MediaPlayerManagerComponent extends AbstractComponent<MediaPlayerSettings> implements PlayStateListener, TimeListener,
        MediaPlayerManager, CloseHandler<Window>, ClosingHandler {

    static interface VideoContainerFactory<T> {
        T createVideoContainer(MediaSynchPlayer videoPlayer, UserService userService, MediaServiceWriteAsync mediaService,
                ErrorReporter errorReporter, PlayerCloseListener playerCloseListener, PopoutListener popoutListener);
    }
    
    private final SimplePanel rootPanel = new SimplePanel();
    private final UserService userService;

    private final Map<MediaTrack, MediaPlayerContainer> activePlayerContainers = new HashMap<MediaTrack, MediaPlayerContainer>();
    private Collection<MediaTrackWithSecurityDTO> assignedMediaTracks = new ArrayList<>();
    private Collection<MediaTrackWithSecurityDTO> overlappingMediaTracks = new ArrayList<>();
    private Map<MediaTrack, Status> mediaTrackStatus = new HashMap<>();

    private final RegattaAndRaceIdentifier raceIdentifier;
    private final RaceTimesInfoProvider raceTimesInfoProvider;
    private final Timer raceTimer;
    private final MediaServiceAsync mediaService;
    private final MediaServiceWriteAsync mediaServiceWrite;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;
    private final UserAgentDetails userAgent;
    private final PopupPositionProvider popupPositionProvider;
    private MediaPlayerSettings settings;
    private final MediaPlayerLifecycle mediaPlayerLifecycle;
    private final RaceDTO raceDto;
    private final FileStorageServiceConnectionTestObservable storageServiceAvailable;

    private List<PlayerChangeListener> playerChangeListener = new ArrayList<>();
    
    /**
     * Used in case a take-down request is to be filed
     */
    private UUID eventId;

    public MediaPlayerManagerComponent(Component<?> parent, ComponentContext<?> context,
            MediaPlayerLifecycle mediaPlayerLifecycle, SailingServiceWriteAsync sailingServiceWrite,
            RegattaAndRaceIdentifier selectedRaceIdentifier, RaceTimesInfoProvider raceTimesInfoProvider,
            Timer raceTimer, MediaServiceAsync mediaService, MediaServiceWriteAsync mediaServiceWrite,
            UserService userService, StringMessages stringMessages, ErrorReporter errorReporter,
            UserAgentDetails userAgent, PopupPositionProvider popupPositionProvider, MediaPlayerSettings settings,
            RaceDTO raceDto, String leaderboardGroupName, UUID eventId) {
        super(parent, context);
        this.eventId = eventId;
        this.mediaPlayerLifecycle = mediaPlayerLifecycle;
        this.userService = userService;
        this.raceIdentifier = selectedRaceIdentifier;
        this.raceTimesInfoProvider = raceTimesInfoProvider;
        this.raceTimer = raceTimer;
        this.raceDto = raceDto;
        this.raceTimer.addPlayStateListener(this);
        this.raceTimer.addTimeListener(this);
        this.playSpeedFactorChanged(raceTimer.getPlaySpeedFactor());
        this.timeChanged(raceTimer.getTime(), null);
        this.playStateChanged(raceTimer.getPlayState(), raceTimer.getPlayMode());
        this.mediaService = mediaService;
        this.mediaServiceWrite = mediaServiceWrite;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.userAgent = userAgent;
        this.popupPositionProvider = popupPositionProvider;
        this.settings = settings;
        this.storageServiceAvailable = new FileStorageServiceConnectionTestObservable(sailingServiceWrite);
        Window.addCloseHandler(this);
        Window.addWindowClosingHandler(this);
        userService.addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
                mediaService.getMediaTracksForRace(getCurrentRace(), getAssignedMediaCallback());
                mediaService.getMediaTracksInTimeRange(getCurrentRace(), getOverlappingMediaCallback());
            }
        }, true);
    }

    private boolean isPotentiallyPlayable(MediaTrack mediaTrack) {
        Status status = mediaTrackStatus.get(mediaTrack);
        return Status.REACHABLE.equals(status) || Status.UNDEFINED.equals(status);
    }

    private void setStatus(final MediaTrack mediaTrack) {
        if (mediaTrack.isYoutube()) {
            mediaTrackStatus.put(mediaTrack, Status.REACHABLE);
        } else {
            Video video = Video.createIfSupported();
            if (video != null) {
                VideoElement mediaReachableTester = video.getVideoElement();
                addLoadMetadataHandler(mediaReachableTester, mediaTrack);
                mediaReachableTester.setPreload(MediaElement.PRELOAD_METADATA);
                mediaReachableTester.setSrc(UriUtils.fromString(mediaTrack.url).asString());
                mediaReachableTester.load();
            } else {
                mediaTrackStatus.put(mediaTrack, Status.CANNOT_PLAY);
            }
        }
    }

    native void addLoadMetadataHandler(MediaElement mediaElement, MediaTrack mediaTrack) /*-{
        var that = this;
        mediaElement
                .addEventListener(
                        'loadedmetadata',
                        function() {
                            that.@com.sap.sailing.gwt.ui.client.media.MediaPlayerManagerComponent::loadedmetadata(Lcom/sap/sailing/domain/common/media/MediaTrack;)(mediaTrack);
                        });
        mediaElement
                .addEventListener(
                        'error',
                        function() {
                            that.@com.sap.sailing.gwt.ui.client.media.MediaPlayerManagerComponent::mediaError(Lcom/sap/sailing/domain/common/media/MediaTrack;)(mediaTrack);
                        });
    }-*/;

    public void loadedmetadata(MediaTrack mediaTrack) {
        mediaTrackStatus.put(mediaTrack, Status.REACHABLE);
    }

    public void mediaError(MediaTrack mediaTrack) {
        mediaTrackStatus.put(mediaTrack, Status.NOT_REACHABLE);
    }

    @Override
    public void playDefault() {
        final MediaTrackWithSecurityDTO defaultVideo = getDefaultMedia(MediaType.video);
        if (defaultVideo != null) {
            playFloatingVideo(defaultVideo);
        } else {
            final MediaTrackWithSecurityDTO defaultAudio = getDefaultMedia(MediaType.audio);
            if (defaultAudio != null) {
                playAudio(defaultAudio);
            }
        }
    }

    private MediaTrackWithSecurityDTO getDefaultMedia(MediaType mediaType) {
        // TODO: implement a better heuristic than just taking the first to come
        for (MediaTrackWithSecurityDTO mediaTrack : assignedMediaTracks) {
            if (mediaTrack.mimeType != null && mediaType.equals(mediaTrack.mimeType.mediaType)
                    && getMediaTrackStatus(mediaTrack).isPotentiallyPlayable()) {
                return mediaTrack;
            }
        }
        return null;
    }

    @Override
    public void playStateChanged(PlayStates playState, PlayModes playMode) {
        switch (playMode) {
        case Replay:
            switch (this.raceTimer.getPlayState()) {
            case Playing:
                startPlaying();
                break;
            case Paused:
                pausePlaying();
            default:
                break;
            }
            break;
        case Live:
            // TODO: Live mode not supported, yet.
            startPlaying();
            break;
        default:
            break;
        }
    }

    @Override
    public void playSpeedFactorChanged(double newPlaySpeedFactor) {
        for (MediaPlayerContainer videoContainer : activePlayerContainers.values()) {
            MediaPlayer videoPlayer = videoContainer.getMediaPlayer();
            videoPlayer.setPlaybackSpeed(newPlaySpeedFactor);
        }
    }

    private void pausePlaying() {
        for (MediaPlayerContainer videoContainer : activePlayerContainers.values()) {
            MediaPlayer videoPlayer = videoContainer.getMediaPlayer();
            if (!videoPlayer.isMediaPaused()) {
                videoPlayer.pauseMedia();
            }
        }
    }

    private void startPlaying() {
        for (MediaPlayerContainer videoContainer : activePlayerContainers.values()) {
            MediaPlayer videoPlayer = videoContainer.getMediaPlayer();
            if (videoPlayer.isMediaPaused() && videoPlayer.isCoveringCurrentRaceTime()) {
                videoPlayer.playMedia();
            }
        }
    }

    @Override
    public void timeChanged(Date newRaceTime, Date oldRaceTime) {
        for (MediaPlayerContainer videoContainer : activePlayerContainers.values()) {
            MediaPlayer videoPlayer = videoContainer.getMediaPlayer();
            ensurePlayState(videoPlayer);
            videoPlayer.raceTimeChanged(newRaceTime);
        }
    }

    /**
     * Wraps the callback handling functions in an object to better document their purpose. onSuccess and onError are
     * simply too generic to tell about their concrete use.
     * 
     * @return
     */
    private AsyncCallback<Iterable<MediaTrackWithSecurityDTO>> getAssignedMediaCallback() {
        return new AsyncCallback<Iterable<MediaTrackWithSecurityDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                notifyStateChange();
                errorReporter.reportError(stringMessages.remoteProcedureCall()+ "getMediaTracksForRace(...) - " +stringMessages.error()
                + caught.getMessage());
                
            }

            @Override
            public void onSuccess(Iterable<MediaTrackWithSecurityDTO> mediaTracks) {
                MediaPlayerManagerComponent.this.assignedMediaTracks.clear();
                Util.addAll(mediaTracks, MediaPlayerManagerComponent.this.assignedMediaTracks);
                for (MediaTrack mediaTrack : MediaPlayerManagerComponent.this.assignedMediaTracks) {
                    setStatus(mediaTrack);
                }
                if (settings.isAutoSelectMedia()) {
                    playDefault();
                }
                notifyStateChange();
            }
        };
    }

    /**
     * Wraps the callback handling functions in an object to better document their purpose. onSuccess and onError are
     * simply too generic to tell about their concrete use.
     */
    private AsyncCallback<Iterable<MediaTrackWithSecurityDTO>> getOverlappingMediaCallback() {
        return new AsyncCallback<Iterable<MediaTrackWithSecurityDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                notifyStateChange();
                errorReporter.reportError(stringMessages.remoteProcedureCall()+ "getMediaTracksForRace(...) - " +stringMessages.error()
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(Iterable<MediaTrackWithSecurityDTO> mediaTracks) {
                MediaPlayerManagerComponent.this.overlappingMediaTracks.clear();
                Util.addAll(mediaTracks, MediaPlayerManagerComponent.this.overlappingMediaTracks);
                for (MediaTrack mediaTrack : MediaPlayerManagerComponent.this.overlappingMediaTracks) {
                    setStatus(mediaTrack);
                }
                notifyStateChange();
            }
        };
    }

    private void notifyStateChange() {
        for(PlayerChangeListener listener:playerChangeListener) {
            listener.notifyStateChange();
        }
    }

    @Override
    public void playAudio(MediaTrackWithSecurityDTO audioTrack) {
        muteAudio();
        playFloatingVideo(audioTrack);
        activePlayerContainers.get(audioTrack).getMediaPlayer().setMuted(false);
    }

    public List<MediaPlayerContainer> getActiveAudioContainers() {
        return activePlayerContainers.entrySet().stream()
                .filter(f -> f.getKey().mimeType.mediaType == MediaType.audio).map(f -> f.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public void muteAudio() {
        for (MediaPlayerContainer player : getActiveAudioContainers()) {
            closeFloatingPlayer(player.getMediaPlayer().getMediaTrack());
        }
    }

    @Override
    public void playFloatingVideo(final MediaTrackWithSecurityDTO videoTrack) {
        MediaPlayerContainer activeVideoContainer = activePlayerContainers.get(videoTrack);
        if (activeVideoContainer == null) {
            FloatingMediaPlayerContainer videoFloatingContainer = createAndWrapVideoPlayer(videoTrack,
                    new VideoContainerFactory<FloatingMediaPlayerContainer>() {
                        @Override
                        public FloatingMediaPlayerContainer createVideoContainer(MediaSynchPlayer videoPlayer,
                                UserService userService, MediaServiceWriteAsync mediaServiceWrite, ErrorReporter errorReporter,
                                PlayerCloseListener playerCloseListener, PopoutListener popoutListener) {
                            FloatingMediaPlayerContainer videoFloatingContainer = new FloatingMediaPlayerContainer(videoPlayer, popupPositionProvider,
                                    userService, mediaServiceWrite, errorReporter, playerCloseListener, popoutListener);
                            return videoFloatingContainer;
                        }
                    });
            registerVideoContainer(videoTrack, videoFloatingContainer);
            notifyStateChange();
        }
    }

    private <T> T createAndWrapVideoPlayer(final MediaTrackWithSecurityDTO videoTrack,
            VideoContainerFactory<T> videoContainerFactory) {
        final PopoutWindowPlayer.PlayerCloseListener playerCloseListener = new PopoutWindowPlayer.PlayerCloseListener() {
            private MediaPlayerContainer videoContainer;

            @Override
            public void playerClosed() {
                if (videoContainer == null) {
                    closeFloatingPlayer(videoTrack);
                } else {
                    registerVideoContainer(videoTrack, videoContainer);
                    videoContainer = null;
                }
            }

            @Override
            public void setVideoContainer(MediaPlayerContainer videoContainer) {
                this.videoContainer = videoContainer;
            }
        };
        PopoutListener popoutListener = new PopoutListener() {
            @Override
            public void popoutVideo(MediaTrackWithSecurityDTO videoTrack) {
                MediaPlayerContainer videoContainer;
                if (videoTrack.isYoutube()) {
                    videoContainer = new YoutubeWindowPlayer(videoTrack, playerCloseListener); // TODO bug6105 make YouTube player show take-down request button
                } else {
                    videoContainer = new VideoJSWindowPlayer(videoTrack, playerCloseListener, raceIdentifier.toString(), eventId);
                }
                playerCloseListener.setVideoContainer(videoContainer);
                closeFloatingPlayer(videoTrack);
            }
        };
        final MediaSynchPlayer videoPlayer;
        if (videoTrack.isYoutube()) {
            videoPlayer = new VideoYoutubePlayer(videoTrack, getRaceStartTime(), raceTimer, userService, raceIdentifier);
        } else {
            videoPlayer = new VideoJSSyncPlayer(videoTrack, getRaceStartTime(), raceTimer, userService, raceIdentifier.toString(), eventId);
        }
        return videoContainerFactory.createVideoContainer(videoPlayer, userService, getMediaServiceWrite(), errorReporter,
                playerCloseListener, popoutListener);
    }

    private void registerVideoContainer(final MediaTrack videoTrack, final MediaPlayerContainer videoContainer) {
        MediaPlayer videoPlayer = videoContainer.getMediaPlayer();
        activePlayerContainers.put(videoTrack, videoContainer);
        synchPlayState(videoPlayer);
        notifyStateChange();
    }

    private TimePoint getRaceStartTime() {
        Date startOfRace = raceTimesInfoProvider.getRaceTimesInfo(getCurrentRace()).startOfRace;
        if (startOfRace != null) {
            return new MillisecondsTimePoint(startOfRace);
        } else {
            return null;
        }
    }
    
    private TimePoint getTrackingStartTime() {
        Date startOfTracking = raceTimesInfoProvider.getRaceTimesInfo(getCurrentRace()).startOfTracking;
        if (startOfTracking != null) {
            return new MillisecondsTimePoint(startOfTracking);
        } else {
            return null;
        }
    }

    private TimePoint getTrackingEndTime() {
        Date endOfTracking = raceTimesInfoProvider.getRaceTimesInfo(getCurrentRace()).endOfTracking;
        if (endOfTracking != null) {
            return new MillisecondsTimePoint(endOfTracking);
        } else {
            return null;
        }
    }

    private TimePoint getCurrentCursorTime() {
        Date raceTimerTime = raceTimer.getTime();
        if (raceTimerTime != null) {
            return new MillisecondsTimePoint(raceTimerTime);
        } else {
            return null;
        }
    }

    @Override
    public void closeFloatingPlayer(MediaTrackWithSecurityDTO videoTrack) {
        MediaPlayerContainer removedVideoContainer = activePlayerContainers.remove(videoTrack);
        if (removedVideoContainer != null) {
            removedVideoContainer.shutDown();
        }
        notifyStateChange();
    }

    private boolean isLive() {
        return raceTimer.getPlayMode() == Timer.PlayModes.Live;
    }

    private void synchPlayState(final MediaPlayer mediaPlayer) {
        mediaPlayer.setPlaybackSpeed(this.raceTimer.getPlaySpeedFactor());
        ensurePlayState(mediaPlayer);
        mediaPlayer.raceTimeChanged(this.raceTimer.getTime());
    }

    private void ensurePlayState(final MediaPlayer mediaPlayer) {
        switch (this.raceTimer.getPlayState()) {
        case Playing:
            if (mediaPlayer.isMediaPaused() && mediaPlayer.isCoveringCurrentRaceTime()) {
                mediaPlayer.playMedia();
            }
            break;
        case Paused:
            if (!mediaPlayer.isMediaPaused()) {
                mediaPlayer.pauseMedia();
            }
        default:
            break;
        }
    }

    @Override
    public void onClose(CloseEvent<Window> arg0) {
        stopAll();
    }

    @Override
    public void onWindowClosing(ClosingEvent arg0) {
        stopAll();
    }

    @Override
    public void stopAll() {
        for (MediaPlayerContainer mediaContainer : new ArrayList<>(activePlayerContainers.values())) {
            // using a copy to prevent a ConcurrentModificationException
            mediaContainer.shutDown();
        }
        activePlayerContainers.clear();
        notifyStateChange();
    }

    @Override
    public void addMediaTrack() {
        final TimePoint currentCursorTime = getCurrentCursorTime();
        final TimePoint defaultStartTime;
        if (getTrackingEndTime() == null) {
            defaultStartTime = TimePoint.now();
        } else if (currentCursorTime != null) {
            defaultStartTime = currentCursorTime;
        } else {
            defaultStartTime = getTrackingStartTime();
        }
        final NewMediaDialog dialog = new NewMediaDialog(mediaService, defaultStartTime,
                MediaPlayerManagerComponent.this.stringMessages, this.getCurrentRace(), storageServiceAvailable,
                new DialogCallback<MediaTrack>() {
                    @Override
                    public void cancel() {
                        // no op
                    }
                    @Override
                    public void ok(final MediaTrack mediaTrack) {
                        MediaPlayerManagerComponent.this.mediaServiceWrite.addMediaTrack(mediaTrack,
                                new AsyncCallback<MediaTrackWithSecurityDTO>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError(t.toString());
                                    }
                                    @Override
                                    public void onSuccess(MediaTrackWithSecurityDTO mediaTrack) {
                                        assignedMediaTracks.add(mediaTrack);
                                        if (!DeviceDetector.isMobile()) {
                                            playFloatingVideo(mediaTrack);
                                        }
                                        Notification.notify(stringMessages.uploadSuccessful(),
                                                NotificationType.SUCCESS);
                                        notifyStateChange();
                                    }
                                });
                    }
                });
        dialog.show();
        if (!DeviceDetector.isDesktop()) {
            dialog.openFileChooserDialog();
        }
    }

    @Override
    public boolean deleteMediaTrack(final MediaTrackWithSecurityDTO mediaTrack) {
        if (Window.confirm(stringMessages.reallyRemoveMediaTrack(mediaTrack.title))) {
            mediaServiceWrite.deleteMediaTrack(mediaTrack, new AsyncCallback<Void>() {

                @Override
                public void onFailure(Throwable t) {
                    errorReporter.reportError(t.toString());
                }

                @Override
                public void onSuccess(Void _void) {
                    assignedMediaTracks.remove(mediaTrack);
                    MediaPlayerManagerComponent.this.closeFloatingPlayer(mediaTrack);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean allowsEditing(MediaTrackWithSecurityDTO mediaTrack) {
        return userService.hasPermission(mediaTrack, DefaultActions.READ)
                && userService.hasPermission(getCurrentRaceDTO(), DefaultActions.UPDATE);
    }

    @Override
    public boolean allowsCreating() {
        return userService.hasCreatePermission(SecuredDomainType.MEDIA_TRACK)
                && userService.hasPermission(getCurrentRaceDTO(), DefaultActions.UPDATE);
    }

    @Override
    public Boolean isPlaying() {
        return !activePlayerContainers.isEmpty();
    }

    @Override
    public void addPlayerChangeListener(PlayerChangeListener playerChangeListener) {
        this.playerChangeListener.add(playerChangeListener);

    }

    @Override
    public Set<MediaTrack> getPlayingVideoTracks() {
        return activePlayerContainers.keySet();
    }

    @Override
    public Collection<MediaTrackWithSecurityDTO> getAssignedMediaTracks() {
        return Collections.unmodifiableCollection(assignedMediaTracks);
    }
    
    @Override
    public Collection<MediaTrackWithSecurityDTO> getOverlappingMediaTracks() {
        removeMediaTracksWhichAreInAssignedMediaTracks();
        return Collections.unmodifiableCollection(overlappingMediaTracks);
    }

    private void removeMediaTracksWhichAreInAssignedMediaTracks() {
        Collection<MediaTrack> temp = new HashSet<MediaTrack>(overlappingMediaTracks);
        for (MediaTrack mediaTrack : temp) {
            if (assignedMediaTracks.contains(mediaTrack)) {
                overlappingMediaTracks.remove(mediaTrack);
            }
        }
    }

    @Override
    public List<MediaTrack> getVideoTracks() {
        return getMediaTracks(MediaType.video);
    }

    @Override
    public List<MediaTrack> getAudioTracks() {
        return getMediaTracks(MediaType.audio);
    }

    private List<MediaTrack> getMediaTracks(MediaType mediaType) {
        return assignedMediaTracks.stream()
                .filter(mediaTrack -> mediaTrack.mimeType != null && mediaType == mediaTrack.mimeType.mediaType)
                .collect(Collectors.toList());
    }

    @Override
    public String getLocalizedShortName() {
        return mediaPlayerLifecycle.getLocalizedShortName();
    }

    @Override
    public boolean hasSettings() {
        return mediaPlayerLifecycle.hasSettings();
    }

    @Override
    public SettingsDialogComponent<MediaPlayerSettings> getSettingsDialogComponent(MediaPlayerSettings settings) {
        return mediaPlayerLifecycle.getSettingsDialogComponent(settings);
    }

    @Override
    public void updateSettings(MediaPlayerSettings newSettings) {
        this.settings = newSettings;
    }

    @Override
    public MediaPlayerSettings getSettings() {
        return settings;
    }

    @Override
    public Widget getEntryWidget() {
        return rootPanel;
    }

    @Override
    public boolean isVisible() {
        return rootPanel.isVisible();
    }

    @Override
    public void setVisible(boolean visibility) {
        rootPanel.setVisible(visibility);
    }

    @Override
    public String getDependentCssClassName() {
        return "media";
    }

    @Override
    public UserAgentDetails getUserAgent() {
        return userAgent;
    }

    @Override
    public RegattaAndRaceIdentifier getCurrentRace() {
        return raceIdentifier;
    }

    @Override
    public MediaServiceAsync getMediaService() {
        return mediaService;
    }

    @Override
    public MediaServiceWriteAsync getMediaServiceWrite() {
        return mediaServiceWrite;
    }

    @Override
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    @Override
    public String getId() {
        return mediaPlayerLifecycle.getComponentId();
    }

    @Override
    public Set<MediaTrackWithSecurityDTO> getPlayingAudioTrack() {
        return getActiveAudioContainers().stream().map(f -> f.getMediaPlayer().getMediaTrack())
                .collect(Collectors.toSet());
    }

    @Override
    public Status getMediaTrackStatus(MediaTrack track) {
        return mediaTrackStatus.getOrDefault(track, Status.UNDEFINED);
    }

    public SecuredDTO getCurrentRaceDTO() {
        return raceDto;
    }
}
