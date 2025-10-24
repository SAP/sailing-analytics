package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.fileupload.FileUploadConstants;
import com.sap.sse.common.fileupload.FileUploadUtil;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.media.MediaType;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.ParallelExecutionCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionHolder;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.gwt.client.panels.HorizontalFlowPanel;
import com.sap.sse.gwt.client.shared.components.CollapsablePanel;

public abstract class AbstractMediaUploadPopup extends DialogBox {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final static String UPLOAD_URL = "/sailingserver/fileupload";
    private final static String DELETE_URL = "/sailingserver/api/v1/file?uri=";
    private final static String STATUS_OK = "OK";
    private final static String STATUS_NOT_OK = "NOK";
    private final static String EMPTY_MESSAGE = "-";
    protected final StringMessages i18n = StringMessages.INSTANCE;
    protected final SharedHomeResources sharedHomeResources = SharedHomeResources.INSTANCE;

    private final BiConsumer<List<ImageDTO>, List<VideoDTO>> updateImagesAndVideos;

    protected final FileUpload upload;
    protected final Button uploadButton;
    protected final FlowPanel content;
    protected final TextBox fileNameInput;
    protected final TextBox urlInput;
    protected final FlowPanel files; 
    private final FlowPanel fileExistingPanel;
    private final Button saveButton;
    private FlowPanel progressOverlay;
    private final Map<String, MediaObject> mediaObjectMap = new LinkedHashMap<>();
    
    private final SailingServiceAsync sailingService;
    
    private static class MediaObject {
        String title;
        String subTitle;
        String copyright;
        MimeType mimeType;
    }

    public AbstractMediaUploadPopup(BiConsumer<List<ImageDTO>, List<VideoDTO>> updateImagesAndVideos, SailingServiceAsync sailingService) {
        this.updateImagesAndVideos = updateImagesAndVideos;
        this.sailingService = sailingService;
        sharedHomeResources.sharedHomeCss().ensureInjected();
        this.addStyleName(sharedHomeResources.sharedHomeCss().popup());
        this.setTitle(i18n.upload());
        Label headerLabel = new Label(i18n.upload());
        HorizontalFlowPanel hFlow = new HorizontalFlowPanel();
        hFlow.add(headerLabel);
        this.setHTML(hFlow.getElement().getInnerHTML());
        this.setGlassEnabled(true);
        this.setAnimationEnabled(true);
        this.setModal(true);
        upload = new FileUpload();
        upload.getElement().setAttribute("accept", "image/*,video/ogg,video/mp4,video/quicktime,video/webm");
        // deactivated camera first feature, because on some devices the file picker option will not be available any more.
        //upload.getElement().setAttribute("capture", "camera");
        upload.getElement().setAttribute("multiple", "multiple");
        upload.setVisible(false);
        upload.setName("file");
        content = new FlowPanel();
        fileNameInput = new TextBox();
        urlInput = new TextBox();
        // Upload form
        final FormPanel uploadForm = new FormPanel();
        // Because we're going to add a FileUpload widget, we'll need to set the
        // form to use the POST method, and multipart MIME encoding.
        uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        uploadForm.setMethod(FormPanel.METHOD_POST);
        uploadForm.setAction(UPLOAD_URL);
        uploadForm.add(upload);
        // Add an event handler to the form.
        uploadForm.addSubmitHandler(new SubmitHandler());
        uploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler());
        content.add(uploadForm);
        final FormPanel metaDataForm = new FormPanel();
        final VerticalPanel metaDataPanel = new VerticalPanel();
        metaDataForm.add(metaDataPanel);
        final Label fileNameLabel = new Label(i18n.fileUpload());
        fileNameLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        metaDataPanel.add(fileNameLabel);
        final FlowPanel fileInputGroup = new FlowPanel();
        fileInputGroup.addStyleName(sharedHomeResources.sharedHomeCss().inputGroup());
        uploadButton = new Button();
        uploadButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                event.stopPropagation();
                upload.click();
            }
        });
        uploadButton.addStyleName(sharedHomeResources.sharedHomeCss().uploadButton());
        // Create a TextBox, giving it a name so that it will be submitted.
        fileNameInput.setName("textBoxFormElement");
        fileNameInput.addStyleName(sharedHomeResources.sharedHomeCss().input());
        fileNameInput.setEnabled(false);
        fileInputGroup.add(fileNameInput);
        fileInputGroup.add(uploadButton);
        metaDataPanel.add(fileInputGroup);
        metaDataPanel.add(new Label("-- " + i18n.or() + " --"));
        final Label urlLabel = new Label(i18n.url());
        urlLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        metaDataPanel.add(urlLabel);
        urlInput.addStyleName(sharedHomeResources.sharedHomeCss().input());
        urlInput.ensureDebugId("urlInput");
        urlInput.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                checkSaveButton();
            }
        });
        urlInput.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                files.clear();
                mediaObjectMap.clear();
                addUri(urlInput.getValue(), "", getMimeType(urlInput.getValue()), true);
                checkSaveButton();
            }
        });
        metaDataPanel.add(urlInput);
        final Label uploadedFiles = new Label(i18n.uploadedFiles());
        uploadedFiles.addStyleName(sharedHomeResources.sharedHomeCss().label());
        metaDataPanel.add(uploadedFiles);
        files = new FlowPanel();
        metaDataPanel.add(files);
        fileExistingPanel = new FlowPanel();
        fileExistingPanel.add(new Label("-- " + i18n.noMediaSelected() + " --"));
        metaDataPanel.add(fileExistingPanel);
        final FlowPanel buttonGroup = new FlowPanel();
        buttonGroup.addStyleName(sharedHomeResources.sharedHomeCss().buttonGroup());
        buttonGroup.addStyleName(sharedHomeResources.sharedHomeCss().right());
        metaDataPanel.add(buttonGroup);
        // Add a 'submit' button.
        final Button cancelButton = new Button(i18n.cancel(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                closePopup(event);
            }
        });
        cancelButton.ensureDebugId("cancelButton");
        buttonGroup.add(cancelButton);
        // Add a 'submit' button.
        saveButton = new Button(i18n.save(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.stopPropagation();
                metaDataForm.submit();
            }
        });
        saveButton.addStyleName(sharedHomeResources.sharedHomeCss().primary());
        saveButton.setEnabled(false);
        buttonGroup.add(saveButton);
        metaDataForm.addSubmitHandler(new FormPanel.SubmitHandler() {
            @Override
            public void onSubmit(SubmitEvent event) {
                fileNameInput.setValue("");
                addMedia();
            }
        });
        content.add(metaDataForm);
        upload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                cleanupTempFileUpload();
                uploadForm.submit();
            }
        });
        progressOverlay = new FlowPanel();
        progressOverlay.ensureDebugId("ProgressOverlay");
        progressOverlay.addStyleName(sharedHomeResources.sharedHomeCss().progressOverlay());
        final FlowPanel progressSpinner = new FlowPanel();
        progressSpinner.addStyleName(sharedHomeResources.sharedHomeCss().progressSpinner());
        progressOverlay.add(progressSpinner);
        progressOverlay.setVisible(false);
        Button headerCancelButton = new Button("X", new ClickHandler() {
            public void onClick(ClickEvent event) {
                closePopup(event);
            }
        });
        headerCancelButton.addStyleName(SharedHomeResources.INSTANCE.sharedHomeCss().headerButton());
        content.add(headerCancelButton);
        content.add(progressOverlay);
        content.ensureDebugId("mediaUploadContent");
        this.add(content);
    }

    private void closePopup(ClickEvent event) {
        event.stopPropagation();
        fileNameInput.setValue("");
        cleanupTempFileUpload();
        hide();
    }

    abstract protected String getTitleFromFileName(String fileName);

    private ListBox initMediaTypes() {
        final ListBox mimeTypeListBox = new ListBox();
        mimeTypeListBox.ensureDebugId("mimeTypeListBox");
        mimeTypeListBox.addItem(MimeType.unknown.name());
        for (MimeType mimeType : MimeType.values()) {
            if (mimeType.isVideo() || mimeType.isImage()) {
                mimeTypeListBox.addItem(mimeType.name());
            }
        }
        mimeTypeListBox.setSelectedIndex(0);
        mimeTypeListBox.addStyleName(sharedHomeResources.sharedHomeCss().select());
        return mimeTypeListBox;
    }

    private void selectMimeTypeInBox(ListBox mimeTypeListBox, MimeType mimeType) {
        for (int i = 0; i < mimeTypeListBox.getItemCount(); i++) {
            if (mimeType.name().equals(mimeTypeListBox.getValue(i))) {
                mimeTypeListBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private MimeType getMimeType(String urlParam) {
        String url;
        if (urlParam == null) {
            url = "";
        } else {
            url = urlParam.trim();
        }
        return MimeType.extractFromUrl(url);
    }

    protected class SubmitHandler implements FormPanel.SubmitHandler {
        @Override
        public void onSubmit(SubmitEvent event) {
            // This event is fired just before the form is submitted. We can take
            // this opportunity to perform validation.
            progressOverlay.setVisible(true);
            fileNameInput.setValue(i18n.loading());
        }
    }

    private class SubmitCompleteHandler implements FormPanel.SubmitCompleteHandler {
        @Override
        public void onSubmitComplete(SubmitCompleteEvent event) {
            // When the form submission is successfully completed, this event is
            // fired. Assuming the service returned a response of type text/html,
            // we can get the result text here (see the FormPanel documentation for
            // further explanation).
            progressOverlay.setVisible(false);
            JSONValue resultJsonValue = JSONParser.parseStrict(FileUploadUtil.getApplicationJsonContentFromHtml(event.getResults()));
            JSONArray resultJson = resultJsonValue.isArray();
            boolean uploadSuccessful = false;
            boolean fileSkipped = false;
            if (resultJson != null) {
                for (int i=0; i<resultJson.size(); i++) {
                    if (resultJson.get(i).isObject().get(FileUploadConstants.FILE_URI) != null) {
                        cleanFormElements();
                        String uri = resultJson.get(i).isObject().get(FileUploadConstants.FILE_URI).isString()
                                .stringValue();
                        String fileNameUnderscoreEncoded = resultJson.get(i).isObject().get(FileUploadConstants.FILE_NAME).isString()
                                .stringValue();
                        // special handling of double underscore in JSON. Double underscores were encoded with hex representation.
                        // In some cases the JSON parser of Apples Safari on mobile devices cannot parse JSON with __. See also bug5127
                        String fileName = fileNameUnderscoreEncoded.replace("%5f%5f", "__");
                        String contentType = resultJson.get(i).isObject().get(FileUploadConstants.CONTENT_TYPE).isString()
                                .stringValue();
                        MimeType mimeType = MimeType.byContentType(contentType);
                        if (!fileName.isEmpty()) {
                            if (mimeType == MimeType.unknown) {
                                fileSkipped = true;
                                logger.log(Level.WARNING, "An unsupported file (" + fileName + " - " + contentType + ") detected. File has been skipped.");
                            } else {
                                uploadSuccessful = true;
                                addUri(uri, fileName, mimeType, false);
                            }
                        }
                    } else {
                        String status = resultJson.get(i).isObject().get(FileUploadConstants.STATUS).isString()
                                .stringValue();
                        String message = resultJson.get(i).isObject().get(FileUploadConstants.MESSAGE).isString()
                                .stringValue();
                        Notification.notify(i18n.fileUploadResult(status, message), NotificationType.ERROR);
                        logger.log(Level.SEVERE, "Submit file failed. Status: " + status + ", message: " + message);
                    }
                }
            }
            String resultMessage = "";
            if (fileSkipped) {
                resultMessage = i18n.notSupportedFileTypesDetected();
            } else if (uploadSuccessful) {
                resultMessage = i18n.uploadSuccessful();
            }
            fileNameInput.setValue(resultMessage);
        }

    }

    public void openFileUpload() {
        upload.click();
    }

    @Override
    public void show() {
        // reset all fields
        resetInput();
        cleanFormElements();
        super.show();
    }

    private void cleanFormElements() {
        fileNameInput.setValue("");
        urlInput.setValue("");
        // progressOverlay.setVisible(false);
    }

    /**
     * Finally add a new MediaTrack.
     */
    private void addMedia() {
        List<ImageDTO> imageList = new ArrayList<>();
        List<VideoDTO> videoList = new ArrayList<>();
        for (Entry<String, MediaObject> mediaObjectEntry : mediaObjectMap.entrySet()) {
            String uri = mediaObjectEntry.getKey();
            final String uploadUrl;
            if (uri == null) {
                uploadUrl = "";
            } else if (!UriUtils.isSafeUri(uri.trim())) {
                logger.warning("Upload url is not valid: " + uri + ". Ignore upload url.");
                uploadUrl = "";
            } else {
                uploadUrl = uri.trim();
            }
            final String inputUrl;
            if (urlInput.getValue() == null) {
                inputUrl = "";
            } else if (!UriUtils.isSafeUri(urlInput.getValue())) {
                logger.warning("Upload url is not valid: " + uri + ". Ignore upload url.");
                inputUrl = "";
            } else {
                inputUrl = urlInput.getValue();
            }
            final String url;
            if (uploadUrl.isEmpty()) {
                url = inputUrl;
            } else {
                url = uploadUrl;
            }
            if (!url.isEmpty()) {
                final MimeType mimeType = mediaObjectEntry.getValue().mimeType;
                hide();
                if (mimeType.mediaType == MediaType.image) {
                    imageList.add(createImage(url));
                    Notification.notify(i18n.imageAdded(), NotificationType.SUCCESS);
                } else if (mimeType.mediaType == MediaType.video) {
                    videoList.add(createVideo(url, null, mimeType));
                    Notification.notify(i18n.videoAdded(), NotificationType.SUCCESS);
                } else {
                    logger.warning("Detected MimeType is not of type video or image. File will be skipped. Found MimeType: " + mimeType);
                    Notification.notify(i18n.fileWithDetectedMimeTypeNotSupported(mimeType.toString()), NotificationType.WARNING);
                }
            } else {
                Notification.notify(i18n.invalidURL(), NotificationType.ERROR);
            }
        }
        if (!imageList.isEmpty() || !videoList.isEmpty()) {
            if (imageList.isEmpty()) { // need not fetch size for any image, can invoke the callback directly:
                updateImagesAndVideos.accept(imageList, videoList);
            } else {
                // For all images, try to obtain the dimensions in order to update the ImageDTOs accordingly.
                // Use a parallel execution holder that waits until all responses have arrived. Log but ignore
                // failures by treating them as null responses, eventually unblocking the parallel execution holder,
                // even if one more images fail to resolve their dimensions.
                final Map<String, ParallelExecutionCallback<Pair<Integer, Integer>>> callbacks = new HashMap<>(imageList.size());
                for (final ImageDTO image : imageList) {
                    final ParallelExecutionCallback<Pair<Integer, Integer>> fetchSizeCallback = new ParallelExecutionCallback<Pair<Integer,Integer>>() {
                        @Override
                        public void onSuccess(Pair<Integer, Integer> t) {
                            if (t != null) {
                                logger.info("Setting image size of "+image.getSourceRef()+" to "+t);
                                image.setSizeInPx(t.getA(), t.getB());
                            }
                            super.onSuccess(t);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            logger.warning("Couldn't get image size for "+image.getSourceRef()+": "+t.getMessage());
                            onSuccess(null); // still call it success to eventually unblock ParallelExecutionHolder
                        }
                    };
                    callbacks.put(image.getSourceRef(), fetchSizeCallback);
                }
                new ParallelExecutionHolder(callbacks.values().toArray(new ParallelExecutionCallback<?>[0])) {
                    @Override
                    protected void handleSuccess() {
                        updateImagesAndVideos.accept(imageList, videoList);
                    }
                    
                    @Override
                    protected void handleFailure(Throwable t) {
                        logger.severe("Shouldn't have come here because callbacks are expected to wrap failure into a success");
                    }
                };
                for (final Entry<String, ParallelExecutionCallback<Pair<Integer, Integer>>> urlAndCallback : callbacks.entrySet()) {
                    sailingService.resolveImageDimensions(urlAndCallback.getKey(), urlAndCallback.getValue());
                }
            }
        } else {
            logger.warning("No image nor video detected. Nothing will be saved.");
            Notification.notify(i18n.noImageOrVideoDetected(), NotificationType.WARNING);
        }
    }

    private ImageDTO createImage(String url) {
        MediaObject mediaObject = mediaObjectMap.get(url);
        final ImageDTO image = new ImageDTO(url, new Date());
        image.setTitle(mediaObject.title);
        image.setSubtitle(mediaObject.subTitle);
        image.setCopyright(mediaObject.copyright);
        Iterable<String> defaultTags = Collections.singletonList(MediaTagConstants.GALLERY.getName());
        image.setTags(defaultTags);
        return image;
    }

    private VideoDTO createVideo(String url, String thumbnailUrl, MimeType mimeType) {
        MediaObject mediaObject = mediaObjectMap.get(url);
        final VideoDTO video = new VideoDTO(url, mimeType, new Date());
        video.setTitle(mediaObject.title);
        video.setSubtitle(mediaObject.subTitle);
        video.setCopyright(mediaObject.copyright);
        video.setThumbnailRef(thumbnailUrl);
        Iterable<String> defaultTags = Collections.singletonList(MediaTagConstants.GALLERY.getName());
        video.setTags(defaultTags);
        return video;
    }

    private void cleanupTempFileUpload() {
        for (String uri: mediaObjectMap.keySet()) {
            MediaObject mediaObject = mediaObjectMap.get(uri);
            if (uri != null 
                    && mediaObject != null 
                    && !Arrays.asList(MimeType.unknown, MimeType.youtube, MimeType.vimeo).contains(mediaObject.mimeType)) {
                String url = DELETE_URL + uri;
                RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.DELETE, url);
                requestBuilder.setCallback(new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        String status = STATUS_NOT_OK;
                        String message = EMPTY_MESSAGE;
                        JSONValue jsonValue = JSONParser.parseStrict(response.getText());
                        JSONArray resultJson = jsonValue.isArray();
                        if (resultJson != null) {
                            if (resultJson.get(0).isObject().get(FileUploadConstants.STATUS) != null) {
                                status = resultJson.get(0).isObject().get(FileUploadConstants.STATUS).isString()
                                        .stringValue();
                                if (!STATUS_OK.equals(status)) {
                                    message = resultJson.get(0).isObject().get(FileUploadConstants.MESSAGE).isString()
                                            .stringValue();
                                }
                            }
                        } else {
                            status = jsonValue.isObject().get(FileUploadConstants.STATUS).isString().stringValue();
                        }
                        if (STATUS_OK.equals(status)) {
                            logger.log(Level.INFO, "Cleanup file successful. URL: " + uri);
                        } else if (EMPTY_MESSAGE.equals(message)) {
                            // No further message from service. Probably the file is not existing any more.
                            Notification.notify(i18n.error(), NotificationType.ERROR);
                            logger.log(Level.SEVERE,
                                    "Cleanup file failed. " + status + ": File with URI could not be removed. URI: " + uri);
                        } else {
                            Notification.notify(i18n.fileUploadResult(status, message), NotificationType.ERROR);
                            logger.log(Level.SEVERE, "Cleanup file failed. Status: " + status + ", message: " + message);
                        }
                    }
    
                    @Override
                    public void onError(Request request, Throwable exception) {
                        Notification.notify(i18n.error(), NotificationType.ERROR);
                        logger.log(Level.SEVERE,
                                "Cleanup file failed. Callback returned with error: " + exception.getMessage(), exception);
                    }
                });
                try {
                    resetInput();
                    cleanFormElements();
                    requestBuilder.send();
                } catch (RequestException e) {
                    Notification.notify(i18n.error(), NotificationType.ERROR);
                    logger.log(Level.SEVERE, "Cleanup file failed. Sending caused an error: " + e.getMessage(), e);
                }
            }
        }
        mediaObjectMap.clear();
    }

    private void resetInput() {
        fileExistingPanel.setVisible(true);
        files.clear();
        saveButton.setEnabled(false);
        // fileNameInput.setEnabled(false);
        urlInput.setEnabled(true);
    }
    
    private void setCollapsebleFilePanelHeader(final CollapsablePanel collapsebleFilePanel, final String fileName, final MimeType mimeType) {
        String header;
        if (fileName != null && !fileName.isEmpty() && mimeType != null) {
            header = fileName + " (" + mimeType.name() + ")";
        } else if (mimeType != null) {
            header = mimeType.name();
        } else {
            header = "n/a";
        }
        collapsebleFilePanel.getHeaderTextAccessor().setText(header);
    }

    private void addUri(String uri, String fileName, MimeType mimeType, boolean enableUrlInput) {
        MediaObject mediaObject = new MediaObject();
        mediaObjectMap.put(uri, mediaObject);
        mediaObject.mimeType = mimeType;
        final String title = getTitleFromFileName(fileName);
        mediaObject.title = title;
        final VerticalPanel vPanel = new VerticalPanel();
        boolean firstCollapsible = true;
        for (int i=0; i < files.getWidgetCount(); i++) {
            if (files.getWidget(i) instanceof CollapsablePanel) {
                firstCollapsible = false;
                ((CollapsablePanel)files.getWidget(i)).setCollapsingEnabled(true);
                break;
            }
        }
        final CollapsablePanel collapsebleFilePanel = new CollapsablePanel(fileName, true);
        setCollapsebleFilePanelHeader(collapsebleFilePanel, title, mimeType);
        collapsebleFilePanel.setContent(vPanel);
        collapsebleFilePanel.setWidth("100%");
        if (firstCollapsible) {
            collapsebleFilePanel.setCollapsingEnabled(false);
            collapsebleFilePanel.setOpen(true);
        }
        final Label fileNameLabel = new Label(fileName);
        fileNameLabel.addStyleName(sharedHomeResources.sharedHomeCss().subTitle());
        //vPanel.add(fileNameLabel);
        final Label titleLabel = new Label(i18n.title());
        titleLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        vPanel.add(titleLabel);
        TextBox titleTextBox = new TextBox();
        titleTextBox.addStyleName(sharedHomeResources.sharedHomeCss().input());
        titleTextBox.setText(title);
        titleTextBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                mediaObject.title = titleTextBox.getValue();
                setCollapsebleFilePanelHeader(collapsebleFilePanel, mediaObject.title, mediaObject.mimeType);
            }
        });
        vPanel.add(titleTextBox);
        final Label subtitleLabel = new Label(i18n.subtitle());
        subtitleLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        vPanel.add(subtitleLabel);
        TextBox subtitleTextBox = new TextBox();
        subtitleTextBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                mediaObject.subTitle = subtitleTextBox.getValue();
            }
        });
        subtitleTextBox.addStyleName(sharedHomeResources.sharedHomeCss().input());
        vPanel.add(subtitleTextBox);
        final Label copyrightLabel = new Label(i18n.copyright());
        copyrightLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        vPanel.add(copyrightLabel);
        TextBox copyrightTextBox = new TextBox();
        copyrightTextBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                mediaObject.copyright = copyrightTextBox.getValue();
            }
        });
        copyrightTextBox.addStyleName(sharedHomeResources.sharedHomeCss().input());
        vPanel.add(copyrightTextBox);
        final Label mimeTypeListLabel = new Label(i18n.mimeType());
        mimeTypeListLabel.addStyleName(sharedHomeResources.sharedHomeCss().label());
        vPanel.add(mimeTypeListLabel);
        final ListBox mediaTypeListBox = initMediaTypes();
        mediaTypeListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                final MimeType mimeType = MimeType.byName(mediaTypeListBox.getSelectedItemText());
                mediaObject.mimeType = mimeType;
                setCollapsebleFilePanelHeader(collapsebleFilePanel, mediaObject.title, mediaObject.mimeType);
            }
        });
        selectMimeTypeInBox(mediaTypeListBox, getMimeType(uri));
        vPanel.add(mediaTypeListBox);
        files.add(collapsebleFilePanel);
        fileExistingPanel.setVisible(false);
        saveButton.setEnabled(true);
        // fileNameInput.setEnabled(true);
        urlInput.setEnabled(enableUrlInput);
        checkSaveButton();
    }

    private void checkSaveButton() {
        boolean urlInputNotEmpty = urlInput.getValue() != null && !urlInput.getValue().trim().isEmpty();
        boolean uriNotEmpty = !mediaObjectMap.isEmpty();
        saveButton.setEnabled(urlInputNotEmpty || uriNotEmpty);
    }

}
