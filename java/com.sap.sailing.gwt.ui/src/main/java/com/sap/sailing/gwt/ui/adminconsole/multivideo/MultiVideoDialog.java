package com.sap.sailing.gwt.ui.adminconsole.multivideo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.VideoMetadataDTO;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.JSDownloadUtils;
import com.sap.sailing.gwt.ui.client.media.JSDownloadUtils.JSDownloadCallback;
import com.sap.sailing.gwt.ui.client.media.JSDownloadUtils.JSHrefCallback;
import com.sap.sailing.gwt.ui.client.media.TimeFormatUtil;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;

/**
 * This class allows to add multiple videos at once, by crawling index sites of a given url, and using the metadata of
 * mp4 files to fill starttime and duration. Additionally it allows to add the new mediatracks directly to the races
 * that are overlapping.
 */
public class MultiVideoDialog extends DialogBox {
    private static final String EMPTY_TEXT = "";
    private static final int DELETE_COLUMN = 0;
    private static final int URL_COLUMN = 1;
    private static final int STATUS_COLUMN = 2;
    private static final int DURATION_COLUMN = 3;
    private static final int STARTTIME_COLUMN = 4;
    private static final int MIMETYPE_COLUMN = 5;
    private static final int RACES_COLUMN = 6;
    
    private static final Logger logger = Logger.getLogger(MultiVideoDialog.class.getName());
    private static final Style STYLE = GWT.<StyleHolder> create(StyleHolder.class).style();
    private StringMessages stringMessages;
    private List<RemoteFileInfo> remoteFiles = new ArrayList<>();
    private FlexTable dataTable;
    private MediaServiceWriteAsync mediaServiceWrite;
    private Button doScanButton;
    private Label statusLabel;
    private SailingServiceAsync sailingService;
    private Button doSaveButton;
    private Runnable afterLinking;
    private ErrorReporter errorReporter;
    protected int offsetTimeInMS;
    private boolean isWorking;

    public MultiVideoDialog(SailingServiceAsync sailingService, MediaServiceWriteAsync mediaServiceWrite,
            StringMessages stringMessages, ErrorReporter errorReporter, Runnable afterLinking) {
        this.stringMessages = stringMessages;
        this.mediaServiceWrite = mediaServiceWrite;
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.afterLinking = afterLinking;
        setGlassEnabled(true);

        FlowPanel mainContent = new FlowPanel();

        Label descriptionLabel = new Label(this.stringMessages.multiVideoDescription());
        descriptionLabel.getElement().getStyle().setPadding(0.5, Unit.EM);
        mainContent.add(descriptionLabel);

        Label indexUrl = new Label(stringMessages.multiVideoURLOfIndex());
        mainContent.add(indexUrl);
        ;
        TextBox urlInput = new TextBox();
        mainContent.add(urlInput);
        doScanButton = new Button(stringMessages.multiVideoScan());
        mainContent.add(doScanButton);
        doScanButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setWorking(true);
                retrieveRemoteFileList(urlInput.getText());
            }
        });

        statusLabel = new Label(stringMessages.multiVideoIdle());
        mainContent.add(statusLabel);

        FlowPanel offsetPanel = new FlowPanel();
        IntegerBox timeOffset = new IntegerBox();
        timeOffset.addValueChangeHandler(new ValueChangeHandler<Integer>() {
            @Override
            public void onValueChange(ValueChangeEvent<Integer> event) {
                Integer value = event.getValue();
                if (value == null) {
                    offsetTimeInMS = 0;
                } else {
                    offsetTimeInMS = value;
                }
                updateUI();
            }
        });
        Label lbl = new Label(stringMessages.multiVideoOffsetInput());
        lbl.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        offsetPanel.add(lbl);
        offsetPanel.add(timeOffset);

        mainContent.add(offsetPanel);

        FlowPanel buttonPanel = new FlowPanel();

        DockPanel dockPanel = new DockPanel();
        add(dockPanel);
        dockPanel.add(new ScrollPanel(mainContent), DockPanel.CENTER);

        dataTable = new FlexTable();
        STYLE.ensureInjected();
        dataTable.addStyleName(STYLE.tableStyle());
        mainContent.add(dataTable);

        Button cancelButton = new Button(stringMessages.close());
        cancelButton.getElement().getStyle().setMargin(3, Unit.PX);
        cancelButton.ensureDebugId("CancelButton");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                MultiVideoDialog.this.hide();
            }
        });

        doSaveButton = new Button(stringMessages.addMediaTrack());
        doSaveButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                setWorking(true);
                startNextLinkingRemoteTask();
            }
        });
        doSaveButton.setEnabled(false);
        buttonPanel.add(doSaveButton);
        buttonPanel.add(cancelButton);

        dockPanel.add(buttonPanel, DockPanel.SOUTH);

        updateUI();
    }

    public void setWorking(boolean working) {
        this.isWorking = working;
        updateUI();
    }

    protected void updateUI() {
        int y = 0;
        dataTable.removeAllRows();
        dataTable.clear();
        dataTable.setWidget(y, DELETE_COLUMN, new Label(stringMessages.addMediaTrack()));
        dataTable.setWidget(y, URL_COLUMN, new Label(stringMessages.url()));
        dataTable.setWidget(y, STATUS_COLUMN, new Label(stringMessages.status()));
        dataTable.setWidget(y, DURATION_COLUMN, new Label(stringMessages.duration()));
        dataTable.setWidget(y, STARTTIME_COLUMN, new Label(stringMessages.startTime()));
        dataTable.setWidget(y, MIMETYPE_COLUMN, new Label(stringMessages.mimeType()));
        dataTable.setWidget(y, RACES_COLUMN, new Label(stringMessages.linkedRaces()));

        for (int row = 0; row < dataTable.getCellCount(0); row++) {
            dataTable.getFlexCellFormatter().addStyleName(y, row, STYLE.tableHeader());
        }

        y++;
        for (RemoteFileInfo remoteFile : remoteFiles) {
            Anchor link = new Anchor(remoteFile.url);
            link.setHref(remoteFile.url);
            link.setTarget("_blank");
            dataTable.setWidget(y, URL_COLUMN, link);
            if (remoteFile.selected || remoteFile.status != EStatus.WAIT_FOR_SAVE) {
                dataTable.setWidget(y, STATUS_COLUMN, new Label(asString(remoteFile.status)));
            } else {
                dataTable.setWidget(y, STATUS_COLUMN, new Label(stringMessages.multiVideoDoNoAdd()));
            }

            CheckBox removeVideo = new CheckBox();
            removeVideo.setEnabled(!remoteFile.isWorking && remoteFile.status == EStatus.WAIT_FOR_SAVE);
            removeVideo.setValue(remoteFile.selected);
            removeVideo.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    remoteFile.selected = event.getValue();
                    updateUI();
                }
            });
            dataTable.setWidget(y, DELETE_COLUMN, removeVideo);

            if (remoteFile.duration == null) {
                dataTable.setWidget(y, DURATION_COLUMN, new Label(EMPTY_TEXT));
            } else {
                TextBox durationInput = new TextBox();
                durationInput.setText(TimeFormatUtil.durationToHrsMinSec(remoteFile.duration));
                durationInput.addValueChangeHandler(new ValueChangeHandler<String>() {

                    @Override
                    public void onValueChange(ValueChangeEvent<String> event) {
                        remoteFile.duration = TimeFormatUtil.hrsMinSecToMilliSeconds(event.getValue());
                    }
                });
                dataTable.setWidget(y, DURATION_COLUMN, durationInput);
            }
            if (remoteFile.startTime == null) {
                dataTable.setWidget(y, STARTTIME_COLUMN, new Label(EMPTY_TEXT));
            } else {
                DateAndTimeInput startTimeInput = new DateAndTimeInput(Accuracy.MILLISECONDS);
                startTimeInput.setValue(new Date(remoteFile.startTime.asMillis() + offsetTimeInMS));
                startTimeInput.addValueChangeHandler(new ValueChangeHandler<Date>() {

                    @Override
                    public void onValueChange(ValueChangeEvent<Date> event) {
                        remoteFile.startTime = new MillisecondsTimePoint(event.getValue().getTime() - offsetTimeInMS);
                    }
                });
                dataTable.setWidget(y, STARTTIME_COLUMN, startTimeInput);
            }
            if (remoteFile.mime == null) {
                if (remoteFile.message == null) {
                    dataTable.setWidget(y, MIMETYPE_COLUMN, new Label(EMPTY_TEXT));
                } else {
                    dataTable.setWidget(y, MIMETYPE_COLUMN, new Label(remoteFile.message));
                }
            } else {
                ListBox mimeTypeBox = new ListBox();
                mimeTypeBox.setMultipleSelect(false);
                mimeTypeBox.addItem(MimeType.mp4.name());
                mimeTypeBox.addItem(MimeType.mp4panorama.name());
                mimeTypeBox.addChangeHandler(new ChangeHandler() {

                    @Override
                    public void onChange(ChangeEvent event) {
                        remoteFile.mime = MimeType.valueOf(mimeTypeBox.getSelectedValue());
                    }
                });
                mimeTypeBox.setSelectedIndex(MimeType.mp4 == remoteFile.mime ? 0 : 1);
                dataTable.setWidget(y, MIMETYPE_COLUMN, mimeTypeBox);
            }
            FlowPanel ft = new FlowPanel();
            dataTable.setWidget(y, RACES_COLUMN, ft);
            Button refresh = new Button(stringMessages.refresh());
            refresh.setEnabled(remoteFile.status == EStatus.WAIT_FOR_SAVE);
            refresh.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    remoteFile.status = EStatus.WAITING_FOR_LINK;
                    setWorking(true);
                    startNextInitializingRemoteTask();
                }
            });
            ft.add(refresh);
            if (remoteFile.candidates == null) {
                // empty flowpanel
            } else if (remoteFile.candidates.isEmpty()) {
                ft.add(new Label(stringMessages.empty()));
            } else {
                for (Entry<RegattaAndRaceIdentifier, Boolean> entry : remoteFile.candidates.entrySet()) {
                    RegattaAndRaceIdentifier candidate = entry.getKey();
                    Boolean selected = entry.getValue();
                    CheckBox cb = new CheckBox(candidate.getRegattaName() + " " + candidate.getRaceName());
                    cb.setValue(selected, false);
                    cb.setEnabled(!remoteFile.isWorking);
                    cb.addStyleName(STYLE.checkboxStyle());
                    ft.add(cb);
                    cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                        @Override
                        public void onValueChange(ValueChangeEvent<Boolean> event) {
                            remoteFile.candidates.put(candidate, event.getValue());
                        }
                    });
                }
            }
            y++;
        }

        doScanButton.setEnabled(!isWorking);
        if (!isWorking) {
            statusLabel.setText(stringMessages.multiVideoIdle());
        }
        if (isShowing()) {
            center();
        }
    }

    private void startNextLinkingRemoteTask() {
        if (!isShowing()) {
            return;
        }
        for (RemoteFileInfo remoteFile : remoteFiles) {
            if (remoteFile.status == EStatus.WAIT_FOR_SAVE && remoteFile.selected) {
                final Set<RegattaAndRaceIdentifier> selectedCandidates = remoteFile.candidates.entrySet().stream()
                        .filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toSet());
                
                MediaTrack mediaTrack = new MediaTrack(remoteFile.url, remoteFile.url,
                        remoteFile.startTime.plus(offsetTimeInMS), remoteFile.duration, remoteFile.mime,
                        selectedCandidates);
                mediaServiceWrite.addMediaTrack(mediaTrack, new AsyncCallback<MediaTrackWithSecurityDTO>() {

                    @Override
                    public void onSuccess(MediaTrackWithSecurityDTO result) {
                        remoteFile.status = EStatus.DONE;
                        remoteFile.isWorking = false;
                        updateUI();
                        startNextLinkingRemoteTask();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        remoteFile.status = EStatus.DONE;
                        remoteFile.isWorking = false;
                        updateUI();
                        startNextLinkingRemoteTask();
                    }
                });
                return;
            }
        }
        afterLinking.run();
    }

    private void startNextInitializingRemoteTask() {
        if (!isShowing()) {
            return;
        }
        // do not start overlapping tasks, as this might need excessive resources for file analysis
        for (RemoteFileInfo remoteFile : remoteFiles) {
            if (remoteFile.isWorking) {
                return;
            }
        }
        doSaveButton.setEnabled(false);
        for (RemoteFileInfo remoteFile : remoteFiles) {
            statusLabel.setText(stringMessages.analyze() + ": " + remoteFile.url);
            statusLabel.setText(remoteFile.url);
            if (remoteFile.status == EStatus.WAITING_FOR_LINK) {
                remoteFile.isWorking = true;
                sailingService.getEvents(new AsyncCallback<List<EventDTO>>() {

                    @Override
                    public void onSuccess(List<EventDTO> result) {
                        Set<RegattaAndRaceIdentifier> candidates = new HashSet<>();
                        collectAllOverlappingRaces(remoteFile, result, candidates);
                        remoteFile.status = EStatus.WAIT_FOR_SAVE;
                        remoteFile.isWorking = false;
                        for(RegattaAndRaceIdentifier candidate:candidates) {
                            remoteFile.candidates.put(candidate, true);
                        }
                        updateUI();
                        startNextInitializingRemoteTask();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(caught.getMessage());
                        remoteFile.status = EStatus.ERROR_LINKING;
                        updateUI();
                    }

                });
                return;
            }
            if (remoteFile.status == EStatus.NOT_ANALYSED) {
                remoteFile.isWorking = true;
                checkMetadata(remoteFile, new AsyncCallback<VideoMetadataDTO>() {
                    @Override
                    public void onSuccess(VideoMetadataDTO result) {
                        if (result.isDownloadable()) {
                            remoteFile.message = result.getMessage();
                            if (result.getDuration() == null) {
                                remoteFile.status = EStatus.ERROR_ANALYZE;
                                remoteFile.isWorking = false;
                                updateUI();
                                startNextInitializingRemoteTask();
                            } else {
                                remoteFile.duration = result.getDuration();
                                if (result.getRecordStartedTime() != null) {
                                    remoteFile.startTime = new MillisecondsTimePoint(result.getRecordStartedTime());
                                }
                                remoteFile.mime = result.isSpherical() ? MimeType.mp4panorama : MimeType.mp4;
                                remoteFile.status = EStatus.GETTING_MEDIATRACK;
                                remoteFile.isWorking = false;
                                updateUI();
                                mediaServiceWrite.getMediaTrackByUrl(remoteFile.url, new AsyncCallback<MediaTrack>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        remoteFile.isWorking = false;
                                        remoteFile.status = EStatus.ERROR_ANALYZE;
                                        updateUI();
                                        startNextInitializingRemoteTask();
                                    }

                                    @Override
                                    public void onSuccess(MediaTrack result) {
                                        if (result == null) {
                                            remoteFile.isWorking = false;
                                            remoteFile.status = EStatus.WAITING_FOR_LINK;
                                        } else {
                                            remoteFile.isWorking = false;
                                            remoteFile.status = EStatus.ALREADY_ADDED;
                                            remoteFile.knownMediaTrack = result;
                                        }
                                        updateUI();
                                        startNextInitializingRemoteTask();
                                    }
                                });
                            }

                        } else {
                            remoteFile.status = EStatus.ERROR_DOWNLOAD;
                            updateUI();
                            startNextInitializingRemoteTask();
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        remoteFile.status = EStatus.ERROR_DOWNLOAD;
                        updateUI();
                    }
                });
                return;
            }
        }
        doSaveButton.setEnabled(true);
        setWorking(false);
    }

    private void collectAllOverlappingRaces(RemoteFileInfo remoteFile, List<EventDTO> result,
            Set<RegattaAndRaceIdentifier> candidates) {
        for (EventDTO event : result) {
            for (LeaderboardGroupDTO groups : event.getLeaderboardGroups()) {
                for (StrippedLeaderboardDTO leaderboard : groups.getLeaderboards()) {
                    for (RaceColumnDTO raceColumn : leaderboard.getRaceList()) {
                        for (FleetDTO fleet : raceColumn.getFleets()) {
                            if (raceColumn.isTrackedRace(fleet)) {
                                RaceDTO race = raceColumn.getRace(fleet);
                                if (race.trackedRace != null) {
                                    Date startTimeWithOffset = remoteFile.startTime.plus(offsetTimeInMS).asDate();
                                    if (race.endOfRace == null || race.endOfRace.after(startTimeWithOffset)) {
                                        if (race.trackedRace.endOfTracking == null
                                                || race.trackedRace.endOfTracking.after(startTimeWithOffset)) {
                                            final long endTimeWithOffset = startTimeWithOffset.getTime()
                                                    + remoteFile.duration.asMillis();
                                            if (race.startOfRace == null
                                                    || race.startOfRace.before(new Date(endTimeWithOffset))) {
                                                if (race.trackedRace.startOfTracking == null
                                                        || race.trackedRace.startOfTracking
                                                                .before(new Date(endTimeWithOffset))) {
                                                    candidates.add(race.getRaceIdentifier());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * For a given url that points to an mp4 video, attempts are made to parse the header, to determine the actual
     * starttime of the video and to check for a 360° flag. The video will be analyzed by the backendserver, either via
     * direct download, or proxied by the client, if a video is only available locally. If the video header cannot be
     * read, default values are used instead.
     */
    private void checkMetadata(RemoteFileInfo file, AsyncCallback<VideoMetadataDTO> asyncCallback) {
        file.status = EStatus.SERVER_ANALYSE;
        updateUI();
        // check on server first
        mediaServiceWrite.checkMetadata(file.url, new AsyncCallback<VideoMetadataDTO>() {

            @Override
            public void onSuccess(VideoMetadataDTO result) {
                if (result.isDownloadable()) {
                    asyncCallback.onSuccess(result);
                } else {
                    file.status = EStatus.CLIENT_ANALYZE;
                    updateUI();
                    checkMetadataOnClient(file, asyncCallback);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                // try on client instead
                checkMetadataOnClient(file, asyncCallback);
            }

            private void checkMetadataOnClient(RemoteFileInfo file, AsyncCallback<VideoMetadataDTO> asyncCallback) {
                JSDownloadUtils.getData(file.url, new JSDownloadCallback() {

                    @Override
                    public void progress(Double current, Double total) {
                        statusLabel.setText(com.sap.sailing.landscape.ui.client.i18n.StringMessages.INSTANCE.transferStarted()
                                + " " + Math.round(current / 1024 / 1024)
                                + "/" + Math.round(total / 1024 / 1024) + " MB");
                    }

                    @Override
                    public void error(Object msg) {
                        asyncCallback.onSuccess(new VideoMetadataDTO(false, null, false, null,
                                msg == null ? EMPTY_TEXT : msg.toString()));
                    }

                    @Override
                    public void complete(Int8Array start, Int8Array end, Double skipped) {
                        statusLabel.setText(stringMessages.analyze());
                        byte[] jStart = new byte[start.byteLength()];
                        for (int i = 0; i < start.byteLength(); i++) {
                            jStart[i] = start.get(i);
                        }
                        byte[] jEnd = new byte[end.byteLength()];
                        for (int i = 0; i < end.byteLength(); i++) {
                            jEnd[i] = end.get(i);
                        }
                        // Due to js represeting everything as 64double, the max safe file is around 4 petabytes
                        mediaServiceWrite.checkMetadata(jStart, jEnd, skipped.longValue(), asyncCallback);
                    }
                });
            }
        });
    }

    private String asString(EStatus status) {
        switch (status) {
        case NOT_ANALYSED:
            return stringMessages.multiVideoNotAnalyzed();
        case ALREADY_ADDED:
            return stringMessages.multiVideoAlreadyKnown();
        case CLIENT_ANALYZE:
            return stringMessages.multiVideoClientIsUploading();
        case DONE:
            return stringMessages.multiVideoFinishedLinking();
        case ERROR_ANALYZE:
            return stringMessages.multiVideoErrorInAnalyzingFile();
        case ERROR_DOWNLOAD:
            return stringMessages.couldNotDownload(EMPTY_TEXT);
        case ERROR_LINKING:
            return stringMessages.error();
        case GETTING_MEDIATRACK:
            return stringMessages.multiVideoObtainingMediaTrack();
        case SERVER_ANALYSE:
            return stringMessages.multiVideoServerIsAnalyzing();
        case WAITING_FOR_LINK:
            return stringMessages.multiVideoWaitingForLinkPhase();
        case WAIT_FOR_SAVE:
            return stringMessages.multiVideoWaitingForSave();
        default:
            return stringMessages.unknown();
        }
    }

    protected void retrieveRemoteFileList(String url) {
        remoteFiles.clear();
        dataTable.clear();
        updateUI();

        JSDownloadUtils.getFileList(url, new JSHrefCallback() {
            @Override
            public void newHref(String foundLink) {
                if (foundLink.equalsIgnoreCase(url)) {
                    return;
                }
                // nginx dummy file in video folder
                if (foundLink.contains("place_videos_here%20folder")) {
                    return;
                }
                remoteFiles.add(new RemoteFileInfo(foundLink));
            }

            @Override
            public void noResult() {
                Notification.notify(stringMessages.serverURLInvalid(), NotificationType.ERROR);
            }

            @Override
            public void complete() {
                updateUI();
                startNextInitializingRemoteTask();
            }

            @Override
            public void error(Object error) {
                logger.log(Level.WARNING, "An error occured while downloading the filelist from the webserver: " + error);
            }
        });
    }

    static interface StyleHolder extends ClientBundle {
        @Source("MultiVideoDialog.css")
        Style style();
    }

    static interface Style extends CssResource {
        String tableStyle();

        String tableHeader();

        String checkboxStyle();
    }

    enum EStatus {
        NOT_ANALYSED,
        WAITING_FOR_LINK,
        SERVER_ANALYSE,
        ERROR_ANALYZE,
        CLIENT_ANALYZE,
        ERROR_DOWNLOAD,
        WAIT_FOR_SAVE,
        DONE,
        ALREADY_ADDED,
        GETTING_MEDIATRACK,
        ERROR_LINKING;
    }

    static class RemoteFileInfo {
        protected boolean selected;
        protected MediaTrack knownMediaTrack;
        protected Map<RegattaAndRaceIdentifier, Boolean> candidates = new HashMap<>();
        protected MimeType mime;
        protected String message;
        protected TimePoint startTime;
        protected Duration duration;
        protected EStatus status = EStatus.NOT_ANALYSED;
        final String url;
        protected boolean isWorking = false;

        public RemoteFileInfo(String foundLink) {
            this.url = foundLink;
        }

    }
}
