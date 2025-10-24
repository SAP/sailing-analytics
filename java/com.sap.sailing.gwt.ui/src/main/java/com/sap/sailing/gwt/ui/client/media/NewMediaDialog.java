package com.sap.sailing.gwt.ui.client.media;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.media.client.Audio;
import com.google.gwt.media.client.MediaBase;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.VideoMetadataDTO;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.gwt.ui.adminconsole.FileStorageServiceConnectionTestObservable;
import com.sap.sailing.gwt.ui.adminconsole.FileStorageServiceConnectionTestObservable.FileStorageServiceConnectionTestObserver;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.JSDownloadUtils.JSDownloadCallback;
import com.sap.sailing.gwt.ui.common.client.YoutubeApi;
import com.sap.sse.common.Duration;
import com.sap.sse.common.HttpRequestHeaderConstants;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.fileupload.FileUploadConstants;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaSubType;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.adminconsole.EndUploadEvent;
import com.sap.sse.gwt.adminconsole.StartUploadEvent;
import com.sap.sse.gwt.adminconsole.URLFieldWithFileUpload;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.controls.datetime.DateAndTimeInput;
import com.sap.sse.gwt.client.controls.datetime.DateTimeInput.Accuracy;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.formfactor.DeviceDetector;

public class NewMediaDialog extends DataEntryDialog<MediaTrack> implements FileStorageServiceConnectionTestObserver {
    
    private final Logger logger = Logger.getLogger(getClass().getName());

    private static final boolean DONT_FIRE_EVENTS = false;
    private static final Duration DEFAULT_DURATION = new MillisecondsDurationImpl(0L);
    private static final NewMediaDialogResources RESOURCES = NewMediaDialogResources.INSTANCE;
    private static final String PROGRESS_STATUS_URL = "/sailingserver/fileupload/progress";

    protected static class MediaTrackValidator implements Validator<MediaTrack> {
        private final StringMessages stringMessages;

        public MediaTrackValidator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(MediaTrack media) {
            String errorMessage = null;
            if (media.url == null || media.url.trim().isEmpty()) {
                errorMessage = stringMessages.pleaseEnterNonEmptyUrl();
            } else if (media.title == null || media.title.trim().isEmpty()) {
                errorMessage = stringMessages.pleaseEnterA(stringMessages.name());
            } else if (media.mimeType == null) {
                errorMessage = stringMessages.fileTypeNotSupported();
            } else if (media.startTime == null) {
                errorMessage = stringMessages.pleaseEnterA(stringMessages.startTime());
            } else if (media.duration == null) {
                errorMessage = stringMessages.pleaseEnterA(stringMessages.duration());
            }
            return errorMessage;
        }
    }

    protected final StringMessages stringMessages;

    protected MediaTrack mediaTrack = new MediaTrack();

    private URLFieldWithFileUpload urlBox;

    private TextBox nameBox;

    protected DateAndTimeInput startTimeBox;

    private SimplePanel infoLabel; // showing either mime type or youtube id

    private TextBox durationBox;

    private TimePoint defaultStartTime;

    private ListBox mimeTypeListBox;

    private Label infoLabelLabel;

    final private RegattaAndRaceIdentifier raceIdentifier;
    final private Set<RegattaAndRaceIdentifier> assignedRaces;

    final private MediaServiceAsync mediaService;

    private boolean remoteMp4WasStarted;
    private boolean remoteMp4WasFinished;

    private Button defaultTimeButton;
    private Button resetNameButton;

    private SimpleBusyIndicator busyIndicator;

    private String lastCheckedUrl;

    private boolean manuallyEditedStartTime = false;

    public NewMediaDialog(MediaServiceAsync mediaService, TimePoint defaultStartTime, StringMessages stringMessages,
            RegattaAndRaceIdentifier raceIdentifier, FileStorageServiceConnectionTestObservable storageServiceConnection,
            DialogCallback<MediaTrack> dialogCallback) {
        super(stringMessages.addMediaTrack(), "", stringMessages.ok(), stringMessages.cancel(),
                new MediaTrackValidator(stringMessages), dialogCallback);
        RESOURCES.css().ensureInjected();
        this.defaultStartTime = defaultStartTime != null ? defaultStartTime : MillisecondsTimePoint.now();
        this.stringMessages = stringMessages;
        this.raceIdentifier = raceIdentifier;
        this.assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        this.assignedRaces.add(raceIdentifier);
        this.mediaService = mediaService;
        urlBox = new URLFieldWithFileUpload(stringMessages, /* multi */ false, /* initiallyEnabled */ true, /* showUrlAfterUpload */ false, "audio/*,video/*");
        urlBox.addValueChangeHandler(new ValueChangeHandler<Map<String, String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Map<String, String>> event) {
                nameBox.setValue(urlBox.getName());
                mediaTrack.title = nameBox.getValue();
                validateAndUpdate();
            }
        });
        getCancelButton().addClickHandler(clickEvent-> urlBox.deleteCurrentFile());
        nameBox = createTextBox(null);
        nameBox.addStyleName(RESOURCES.css().nameBoxClass());
        nameBox.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                mediaTrack.title = nameBox.getValue();
                validateAndUpdate();
            }
        });
        resetNameButton = new Button();
        resetNameButton.addStyleName("btn-primary");
        resetNameButton.addStyleName(RESOURCES.css().resetNameButtonClass());
        resetNameButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                mediaTrack.title = "";
                nameBox.setValue("");
                validateAndUpdate();
            }
        });
        startTimeBox = new DateAndTimeInput(Accuracy.MILLISECONDS);
        startTimeBox.addStyleName(RESOURCES.css().startTimeTextboxClass());
        startTimeBox.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                manuallyEditedStartTime = true;
                mediaTrack.startTime = new MillisecondsTimePoint(event.getValue());
                validateAndUpdate();
            }
        });
        
        defaultTimeButton = new Button();
        defaultTimeButton.addStyleName("btn-primary");
        defaultTimeButton.setTitle(StringMessages.INSTANCE.resetStartTimeToDefault());
        defaultTimeButton.addStyleName(RESOURCES.css().resetButtonClass());
        defaultTimeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                manuallyEditedStartTime = true;
                mediaTrack.startTime = defaultStartTime;
                refreshUI(); // Force UI update
                validateAndUpdate();
            }
        });
        durationBox = createTextBox(null);
        durationBox.setValue(DEFAULT_DURATION.toString());
        durationBox.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                String duration = durationBox.getValue();
                if (duration != null && !duration.trim().isEmpty()) {
                    try {
                        mediaTrack.duration = TimeFormatUtil.hrsMinSecToMilliSeconds(duration);
                    } catch (NumberFormatException e) {
                        mediaTrack.duration = DEFAULT_DURATION;
                    }
                } else {
                    mediaTrack.duration = DEFAULT_DURATION;
                }
                validateAndUpdate();
            }
        });
        storageServiceConnection.registerObserver(this);
    }

    @Override
    protected void validateAndUpdate() {
        super.validateAndUpdate();
        if (urlBox.getUri() == null || urlBox.getUri().isEmpty()) {
            getOkButton().setEnabled(false);
        }
    }

    @Override
    protected MediaTrack getResult() {
        updateFromUrl();
        connectMediaWithRace();
        return mediaTrack;
    }
    
    protected void connectMediaWithRace() {
        mediaTrack.assignedRaces = assignedRaces;
    }

    private void updateBoxByMimeType() {
        if (mimeTypeListBox != null) {
            for (int i = 0; i < mimeTypeListBox.getItemCount(); i++) {
                String value = mimeTypeListBox.getValue(i);
                if (value != null && !value.isEmpty() && mediaTrack.mimeType == MimeType.valueOf(value)) {
                    mimeTypeListBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (mimeTypeListBox.getSelectedIndex() < 1) {
            mediaTrack.mimeType = null;
        }
    }

    protected void updateFromUrl() {
        String url = urlBox.getUri();
        if (url != null && !url.isEmpty()) {
            boolean urlChanged = !url.equals(lastCheckedUrl);
            lastCheckedUrl = url;
            if (urlChanged) {
                remoteMp4WasStarted = false;
                remoteMp4WasFinished = false;
                manuallyEditedStartTime = false;
                if (mediaTrack.startTime == null) {
                    mediaTrack.startTime = defaultStartTime;
                }
                boolean isVimeoUrl = isVimeoUrl(url);
                String youtubeId = YoutubeApi.getIdByUrl(url);
                if (youtubeId != null) {
                    mediaTrack.url = youtubeId;
                    mediaTrack.mimeType = MimeType.youtube;
                    mediaService.checkYoutubeMetadata(youtubeId, new AsyncCallback<VideoMetadataDTO>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            infoLabel.setWidget(new Label(caught.getMessage()));
                        }
                        @Override
                        public void onSuccess(VideoMetadataDTO result) {
                            if (result.isDownloadable()) {
                                if (result.getDuration() != null) {
                                    mediaTrack.duration = result.getDuration();
                                } else {
                                    mediaTrack.duration = DEFAULT_DURATION;
                                }
                                mediaTrack.title = result.getMessage();
                                refreshUI();
                                validateAndUpdate();
                            } else {
                                infoLabel.setWidget(new Label(result.getMessage()));
                            }
                        }
                    });
                } else if (isVimeoUrl) {
                    mediaTrack.url = url;
                    mediaTrack.mimeType = MimeType.vimeo;
                } else {
                    mediaTrack.url = url;
                    AnchorElement anchor = Document.get().createAnchorElement();
                    anchor.setHref(url);
                    //remove trailing / as well
                    String lastPathSegment = anchor.getPropertyString("pathname").substring(1);
                    MimeType mimeType = MimeType.byExtension(lastPathSegment);
                    if (mimeType != MimeType.unknown) {
                        mediaTrack.mimeType = mimeType;
                    } else {
                        mediaTrack.mimeType = null;
                    }
                }
                refreshUI();
            }
        }
    }

    private boolean isVimeoUrl(String url) {
        try {
            RegExp urlPattern = RegExp.compile("^(.*:)//([A-Za-z0-9\\-\\.]+)(:[0-9]+)?(.*)$");
            MatchResult matchResult = urlPattern.exec(url);
            String host = matchResult.getGroup(2);
            return host.contains("vimeo.com");
        } catch (Exception e) {
            return false;
        }
    }

    //used for audio only tracks, using native mediaelement to determine time
    private void loadMediaDuration() {
        MediaBase mediaBase = Audio.createIfSupported();
        if (mediaBase != null) {
            addLoadMetadataHandler(mediaBase.getMediaElement());
            mediaBase.setPreload(MediaElement.PRELOAD_METADATA);
            mediaBase.setSrc(mediaTrack.url);
            mediaBase.load();
        }
    }

    native void addLoadMetadataHandler(MediaElement mediaElement) /*-{
        var that = this;
        mediaElement.addEventListener('loadedmetadata',
            function() {
                that.@com.sap.sailing.gwt.ui.client.media.NewMediaDialog::loadedmetadata(Lcom/google/gwt/dom/client/MediaElement;)(mediaElement);
            });
    }-*/;

    public void loadedmetadata(MediaElement mediaElement) {
        if (!manuallyEditedStartTime) {
            mediaTrack.startTime = defaultStartTime;
        }
        double duration = mediaElement.getDuration();
        if (duration > 0) {
            mediaTrack.duration = new MillisecondsDurationImpl((long) Math.round(duration * 1000));
        } else {
            mediaTrack.duration = DEFAULT_DURATION;
        }
        refreshUI();
        validateAndUpdate();
    }

    @Override
    protected Widget getAdditionalWidget() {
        NewMediaDialogResources.INSTANCE.css().ensureInjected();
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.addStyleName(NewMediaDialogResources.INSTANCE.css().textfieldSizeClass());
        mainPanel.addStyleName(NewMediaDialogResources.INSTANCE.css().datePickerClass());
        
        Grid formGrid = new Grid(6, 2);
        formGrid.setCellSpacing(3);
        formGrid.setWidget(0, 0, new Label(stringMessages.fileUpload() + ":"));
        formGrid.setWidget(0, 1, urlBox);
        formGrid.setWidget(1, 0, new Label(stringMessages.name() + ":"));
        FlowPanel namePanel = new FlowPanel();
        namePanel.addStyleName(NewMediaDialogResources.INSTANCE.css().fieldGroup());
        namePanel.add(nameBox);
        namePanel.add(resetNameButton);
        formGrid.setWidget(1, 1, namePanel);

        infoLabelLabel = new Label();
        formGrid.setWidget(2, 0, infoLabelLabel);
        infoLabel = new SimplePanel();
        infoLabel.setWidget(new Label(""));
        formGrid.setWidget(2, 1, infoLabel);
        formGrid.setWidget(3, 0, new Label(stringMessages.startTime() + ":"));

        FlowPanel startTimePanel = new FlowPanel();
        startTimePanel.addStyleName(NewMediaDialogResources.INSTANCE.css().fieldGroup());
        startTimePanel.add(startTimeBox);
        startTimePanel.add(defaultTimeButton);
        formGrid.setWidget(3, 1, startTimePanel);

        formGrid.setWidget(4, 0, new Label(stringMessages.duration() + ":"));
        formGrid.setWidget(4, 1, durationBox);
        busyIndicator = new SimpleBusyIndicator();
        formGrid.setWidget(5, 0, busyIndicator);
        mainPanel.add(formGrid);
        FlowPanel progressOverlay = new FlowPanel();
        progressOverlay.addStyleName(NewMediaDialogResources.INSTANCE.css().progressOverlay());
        FlowPanel progressSpinner = new FlowPanel();
        progressSpinner.addStyleName(NewMediaDialogResources.INSTANCE.css().progressSpinner());
        final Label counter = new Label("0%");
        counter.addStyleName(NewMediaDialogResources.INSTANCE.css().progressCounter());
        progressOverlay.add(progressSpinner);
        progressOverlay.add(counter);
        progressOverlay.setVisible(false);
        final Timer t = new Timer() {
            public void run() {
                requestProgressPercentage(this, counter);
            }
        };
        urlBox.setStartUploadEvent(new StartUploadEvent() {
            @Override
            public void startUpload() {
                progressOverlay.setVisible(true);
                t.scheduleRepeating(1000);
            }
        });
        urlBox.setEndUploadEvent(new EndUploadEvent() {
            @Override
            public void endUpload() {
                progressOverlay.setVisible(false);
                t.cancel();
            }
        });
        mainPanel.add(progressOverlay);
        return mainPanel;
    }
    
    private void requestProgressPercentage(final Timer t, final Label counter) {
        final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, PROGRESS_STATUS_URL);
        builder.setHeader(HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getA(), HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getB());
        try {
            builder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    try {
                        String result = response.getText().trim();
                        JSONValue resultJsonValue = JSONParser.parseStrict(result);
                        if (resultJsonValue.isObject().get(FileUploadConstants.PROGRESS_PERCENTAGE) != null) {
                            int percentage = (int) resultJsonValue.isObject()
                                    .get(FileUploadConstants.PROGRESS_PERCENTAGE).isNumber().doubleValue();
                            long theBytesRead = (long) resultJsonValue.isObject()
                                    .get(FileUploadConstants.PROGRESS_BYTE_DONE).isNumber().doubleValue();
                            long theContentLength = (long) resultJsonValue.isObject()
                                    .get(FileUploadConstants.PROGRESS_BYTE_TOTAL).isNumber().doubleValue();
                            if (percentage > 99) {
                                logger.info("Upload complete.");
                                t.cancel();
                            }
                            counter.setText(percentage + "%");
                            if (theContentLength == -1) {
                                counter.setTitle(theBytesRead + " bytes");
                            } else {
                                counter.setTitle(theBytesRead + " / " + theContentLength + " bytes.");
                            }
                        } else {
                            logger.severe("Cannot read result from progress request");
                            t.cancel();
                        }
                    } catch (JSONException e) {
                        logger.log(Level.SEVERE, "Cannot read result from progress request", e);
                        t.cancel();
                    }
                }
                @Override
                public void onError(Request request, Throwable exception) {
                    logger.log(Level.SEVERE, "Error occured while requesting upload status. (1)", exception);
                    t.cancel();
                }
            });
        } catch (RequestException e) {
            logger.log(Level.SEVERE, "Error occured while requesting upload status. (2)", e);
            t.cancel();
        }
    }

    private String sliceBefore(String lastPathSegment, String slicer) {
        int paramSegment = lastPathSegment.indexOf(slicer);
        if (paramSegment > 0) {
            return lastPathSegment.substring(0, paramSegment);
        }
        return lastPathSegment;
    }

    public void setBusy(boolean busy) {
        busyIndicator.setBusy(busy);
    }

    protected void refreshUI() {
        nameBox.setValue(mediaTrack.title, DONT_FIRE_EVENTS);
        if (mediaTrack.isYoutube()) {
            infoLabelLabel.setText(stringMessages.youtubeId() + ":");
            infoLabel.setWidget(new Label(mediaTrack.url));
        } else {
            infoLabelLabel.setText(stringMessages.mimeType() + ":");
            if (mediaTrack.mimeType != null && mediaTrack.mimeType.getMediaSubType() == MediaSubType.mp4) {
                if (!remoteMp4WasStarted) {
                    processMp4(mediaTrack);
                } else if (remoteMp4WasFinished) {
                    manualMimeTypeSelection(null, mediaTrack, MimeType.mp4MimeTypes());
                } else {
                    infoLabel.setWidget(new Label(stringMessages.processingMP4()));
                }
            } else if (mediaTrack.mimeType == MimeType.vimeo) {
              infoLabel.setWidget(new Label(mediaTrack.typeToString()));
            } else {
                manualMimeTypeSelection(null, mediaTrack, new MimeType[] { MimeType.mp4, MimeType.mp4panorama,
                        MimeType.mp4panoramaflip, MimeType.youtube, MimeType.vimeo, MimeType.mov, MimeType.ogg, MimeType.aac, MimeType.mp3 });
            }
        }
        startTimeBox.setValue(mediaTrack.startTime == null ? null : mediaTrack.startTime.asDate(), DONT_FIRE_EVENTS);
        if (mediaTrack.duration != null) {
            durationBox.setValue(TimeFormatUtil.durationToHrsMinSec(mediaTrack.duration), DONT_FIRE_EVENTS);
        } else {
            durationBox.setValue(DEFAULT_DURATION.toString(), DONT_FIRE_EVENTS);
        }
        refreshPopupPosition();
    }

    @Override
    public void show() {
        super.show();
        refreshPopupPosition();
    }

    private void refreshPopupPosition() {
        if (!DeviceDetector.isDesktop()) {
            final DialogBox popup = super.getDialogBox();
            popup.setPopupPositionAndShow((width, height) -> {
                popup.getElement().getStyle().clearTop();
                popup.getElement().getStyle().setBottom(0, Unit.PX);
            });
        }
    }

    @Override
    public void onFileStorageServiceTestPassed() {
        urlBox.setUploadEnabled(true);
    }

    private void processMp4(MediaTrack mediaTrack) {
        this.busyIndicator.setBusy(true);
        remoteMp4WasStarted = true;
        remoteMp4WasFinished = false;
        Label infoLbl = new Label(stringMessages.processingMP4());
        infoLabel.setWidget(infoLbl);
        checkMetadata(mediaTrack.url, infoLbl, new AsyncCallback<VideoMetadataDTO>() {
            @Override
            public void onSuccess(VideoMetadataDTO result) {
                busyIndicator.setBusy(false);
                remoteMp4WasFinished = true;
                mp4MetadataResult(result);
            }
            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Error in backend", caught);
                busyIndicator.setBusy(false);
                remoteMp4WasFinished = true;
                manualMimeTypeSelection(caught.getMessage(), mediaTrack, MimeType.mp4MimeTypes());
            }
        });
    }

    /**
     * For a given url that points to an mp4 video, attempts are made to parse the header, to determine the actual
     * starttime of the video and to check for a 360° flag. The video will be analyzed by the backendserver, either via
     * direct download, or proxied by the client, if a video is only available locally. If the video header cannot be
     * read, default values are used instead.
     */
    private void checkMetadata(String url, Label lbl, AsyncCallback<VideoMetadataDTO> asyncCallback) {
        // check on server first
        mediaService.checkMetadata(mediaTrack.url, new AsyncCallback<VideoMetadataDTO>() {
            @Override
            public void onSuccess(VideoMetadataDTO result) {
                remoteMp4WasFinished = true;
                if (result.isDownloadable()) {
                    busyIndicator.setBusy(false);
                    mp4MetadataResult(result);
                } else {
                    checkMetadataOnClient(url, lbl, asyncCallback);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                // try on client instead
                checkMetadataOnClient(url, lbl, asyncCallback);
            }

            private void checkMetadataOnClient(String url, Label lbl, AsyncCallback<VideoMetadataDTO> asyncCallback) {
                JSDownloadUtils.getData(url, new JSDownloadCallback() {
                    @Override
                    public void progress(Double current, Double total) {
                        lbl.setText(stringMessages.transferStarted() + " " + Math.round(current / 1024 / 1024) + "/"
                                + Math.round(total / 1024 / 1024) + " MB");
                        infoLabel.setWidget(lbl);
                    }

                    @Override
                    public void error(Object msg) {
                        asyncCallback
                                .onSuccess(new VideoMetadataDTO(false, null, false, null, msg == null ? "" : msg.toString()));
                    }

                    @Override
                    public void complete(Int8Array start, Int8Array end, Double skipped) {
                        lbl.setText(stringMessages.analyze());
                        infoLabel.setWidget(lbl);
                        byte[] jStart = new byte[start.byteLength()];
                        for (int i = 0; i < start.byteLength(); i++) {
                            jStart[i] = start.get(i);
                        }
                        byte[] jEnd = new byte[end.byteLength()];
                        for (int i = 0; i < end.byteLength(); i++) {
                            jEnd[i] = end.get(i);
                        }
                        // Due to js represeting everything as 64double, the max safe file is around 4 petabytes
                        mediaService.checkMetadata(jStart, jEnd, skipped.longValue(), asyncCallback);
                    }
                });
            }
        });
    }

    protected void mp4MetadataResult(VideoMetadataDTO result) {
        if (!result.isDownloadable()) {
            Notification.notify(stringMessages.couldNotDownload(mediaTrack.url), NotificationType.ERROR);
            manualMimeTypeSelection(result.getMessage(), mediaTrack, MimeType.mp4MimeTypes());
        } else {
            mediaTrack.duration = result.getDuration();
            if (mediaTrack.duration == null) {
                loadMediaDuration(); // Attempt duration detection with audio channel
            } else {
                mediaTrack.duration = DEFAULT_DURATION;
            }
            mediaTrack.mimeType = result.isSpherical() ? MimeType.mp4panorama : MimeType.mp4;
            if (result.getRecordStartedTime() != null && !manuallyEditedStartTime) {
                mediaTrack.startTime = new MillisecondsTimePoint(result.getRecordStartedTime());
            }
            refreshUI();
            validateAndUpdate();
        }
    }

    private void manualMimeTypeSelection(String message, MediaTrack mediaTrack, MimeType[] proposedMimeTypes) {
        FlowPanel fp = new FlowPanel();
        if (message != null) {
            fp.add(new Label(message));
        }
        mimeTypeListBox = createListBox(false);
        // add empty default value to enable validation
        mimeTypeListBox.addItem(stringMessages.pleaseSelect(), "");
        for (int i = 0; i < proposedMimeTypes.length; i++) {
            mimeTypeListBox.addItem(proposedMimeTypes[i].name());
        }
        mimeTypeListBox.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                mediaTrack.mimeType = MimeType.byName(mimeTypeListBox.getSelectedValue());
                validateAndUpdate();
            }
        });
        fp.add(mimeTypeListBox);
        infoLabel.setWidget(fp);
        updateBoxByMimeType();
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return urlBox.getInitialFocusWidget();
    }

    public void openFileChooserDialog() {
        this.urlBox.fireClickToFileUploadField();
    }
}
