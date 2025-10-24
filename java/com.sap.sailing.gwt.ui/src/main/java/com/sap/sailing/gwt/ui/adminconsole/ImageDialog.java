package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.adminconsole.FileStorageServiceConnectionTestObservable.FileStorageServiceConnectionTestObserver;
import com.sap.sailing.gwt.ui.client.SailingService;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.adminconsole.URLFieldWithFileUpload;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.controls.listedit.ExpandedUiWithCheckboxes;
import com.sap.sse.gwt.client.controls.listedit.StringListInlineEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;

public abstract class ImageDialog extends DataEntryDialog<List<ImageResizingTaskDTO>>
        implements FileStorageServiceConnectionTestObserver {
    private final SailingServiceAsync sailingService;
    
    protected final StringMessages stringMessages;
    protected final URLFieldWithFileUpload imageURLAndUploadComposite;
    protected final Date creationDate;
    protected Label createdAtLabel;
    protected TextBox titleTextBox;
    protected TextBox subtitleTextBox;
    protected TextBox copyrightTextBox;
    protected final VerticalPanel fileInfoVPanel;
    protected final StringListInlineEditorComposite tagsListEditor;
    protected Image image;
    private final ExpandedUiWithCheckboxes<String> expandedUi;
    private final BusyIndicator busyIndicator;
    private int busyCounter;
    private final Map<String, Pair<Integer, Integer>> imageDimensionsMap;

    protected static class ImageParameterValidator implements Validator<List<ImageResizingTaskDTO>> {
        private final StringMessages stringMessages;
        private List<CheckBox> doResize;
        private final FileStorageServiceConnectionTestObservable storageServiceAvailable;

        public ImageParameterValidator(StringMessages stringMessages,
                FileStorageServiceConnectionTestObservable storageServiceAvailable) {
            this.stringMessages = stringMessages;
            this.storageServiceAvailable = storageServiceAvailable;
            this.doResize = new ArrayList<CheckBox>();
        }

        public void setCheckBoxes(List<CheckBox> doResize) {
            this.doResize = doResize;
        }

        private enum CheckBoxStyle {
            Invisible,
            Normal,
            Error
        }

        /*
         * author Robin Fleige (D067799)
         * will return an error message if the image is too small
         * will return an error message if the width-height-ratio is not fitting for a resize
         * will return an error message and show a checkbox to allow resizing if the image is too big
         * will disable the checkbox if there is no working FileStorageService
         */
        @Override
        public String getErrorMessage(final List<ImageResizingTaskDTO> resizingTasks) {
            StringJoiner errorJoiner = new StringJoiner("\n");
            final Map<CheckBox, CheckBoxStyle> checkBoxStyleMap = new HashMap<>();
            for (ImageResizingTaskDTO resizingTask : resizingTasks) {
                String errorMessage = null;
                final ImageDTO imageToValidate = resizingTask.getImage();
                final Integer imageWidth = imageToValidate.getWidthInPx();
                final Integer imageHeight = imageToValidate.getHeightInPx();
                if (imageToValidate.getSourceRef() == null || imageToValidate.getSourceRef().isEmpty()) {
                    errorMessage = stringMessages.pleaseEnterNonEmptyUrlOrUploadImage();
                } else if (imageToValidate.getSourceRef().startsWith("http:")
                        && !imageToValidate.getSourceRef().contains("localhost")
                        && !imageToValidate.getSourceRef().contains("127.0.0.1")) {
                    errorMessage = stringMessages.pleaseUseHttpsForImageUrls();
                } else if (imageWidth == null || imageHeight == null) {
                    errorMessage = stringMessages.couldNotRetrieveImageSizeYet();
                } else {
                    // check if image is too small for resizing
                    errorMessage = "";
                    for (MediaTagConstants mediaTag : MediaTagConstants.values()) {
                        if (imageToValidate.hasTag(mediaTag.getName())
                                && (imageWidth < mediaTag.getMinWidth() || imageHeight < mediaTag.getMinHeight())) {
                            errorMessage += getImageToSmallErrorMessage(mediaTag, stringMessages) + "\n";
                        }
                    }
                    if (errorMessage.equals("")) {// Check if image ratio fits for resizing
                        errorMessage = imageRatioFits(imageToValidate);
                    }
                    if (errorMessage.equals("")) {// check for checkboxes and resizing
                        for (MediaTagConstants mediaTag : MediaTagConstants.values()) {
                            final CheckBox checkBox = getCheckBoxForTag(mediaTag.getName(), imageToValidate);
                            if (imageToValidate.hasTag(mediaTag.getName())
                                    && (imageWidth > mediaTag.getMaxWidth() || imageHeight > mediaTag.getMaxHeight())) {
                                // Image has tag but is not compatible
                                if (!resizingTask.getResizingTask().contains(mediaTag)) { // Image has tag but resizeTask does not
                                    String fileName = ImageDialog.extractFileName(imageToValidate.getSourceRef());
                                    errorMessage += getSizeErrorMessage(fileName, mediaTag, stringMessages) + "\r\n";
                                    checkBoxStyleMap.put(checkBox, CheckBoxStyle.Error);
                                    if (!errorMessage.equals("") && !storageServiceAvailable.getValue()) {
                                        checkBox.setEnabled(false);
                                    }
                                } else {
                                    // Set checkbox to Normal if not already set to Error
                                    checkBoxStyleMap.compute(checkBox, (k, v) -> {
                                        if (v == CheckBoxStyle.Error) {
                                            return CheckBoxStyle.Error;
                                        } else {
                                            return CheckBoxStyle.Normal;
                                        }
                                    });
                                }
                            } else {
                                // Set checkbox to Invisble if not already set to Normal or Error
                                checkBoxStyleMap.compute(checkBox, (k, v) -> {
                                    if (v == null || v == CheckBoxStyle.Invisible) {
                                        return CheckBoxStyle.Invisible;
                                    } else {
                                        return v;
                                    }
                                });
                            }
                        }
                    }
                    if (!errorMessage.equals("") && !storageServiceAvailable.getValue()) {
                        errorMessage += stringMessages.automaticResizeNeedsStorageService() + "\n";
                    }
                }
                if (errorMessage.equals("")) {
                    errorMessage = null;
                }
                if (errorMessage != null) {
                    errorJoiner.add(errorMessage);
                }
            }
            for (Map.Entry<CheckBox, CheckBoxStyle> entry : checkBoxStyleMap.entrySet()) {
                final CheckBox checkBox = entry.getKey();
                switch (entry.getValue()) {
                case Invisible:
                    checkBox.setStyleName(ExpandedUiWithCheckboxes.getInvisibleStyle());
                    checkBox.setValue(false);
                    break;
                case Normal:
                    checkBox.setStyleName(ExpandedUiWithCheckboxes.getNormalStyle());
                    break;
                case Error:
                    checkBox.setStyleName(ExpandedUiWithCheckboxes.getErrorStyle());
                    break;
                }
            }
            return errorJoiner.toString();
        }

        /**
         * Searches for the checkbox for the given tag and returns it
         * 
         * @author Robin Fleige (D067799)
         * 
         * @param tag
         *            the tag, the checkbox for is needed
         * @param imageToValidate
         *            the image with a list of all tags
         * @returns the fitting checkbox for the tag
         */
        private CheckBox getCheckBoxForTag(final String tag, final ImageDTO imageToValidate) {
            final List<String> tags = imageToValidate.getTags();
            CheckBox toReturn = null;
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).equals(tag)) {
                    toReturn = doResize.get(i);
                }
            }
            return toReturn == null ? new CheckBox() : toReturn;
            // new checkbox instead of null, so there is no need for a null check. this will be deleted from garbage
            // collector anyway
        }
        
        /**
         * Calculates if
         * 
         * @author Robin Fleige (D067799)
         * 
         * @param imageToValidate
         *            the image which has to be validated
         * @returns true if the imageRatio fits into the bounds of all its {@link MediaTagConstants}
         */
        private String imageRatioFits(ImageDTO imageToValidate) {
            String errorMessage = "";
            final double ratio = ((double) imageToValidate.getWidthInPx()) / imageToValidate.getHeightInPx();
            for (MediaTagConstants mediaTag : MediaTagConstants.values()) {
                if (imageToValidate.hasTag(mediaTag.getName())) {
                    final double minRatio = ((double) mediaTag.getMaxWidth()) / mediaTag.getMinHeight();
                    final double maxRatio = ((double) mediaTag.getMinWidth()) / mediaTag.getMaxHeight();
                    if (minRatio < ratio || maxRatio > ratio) {
                        errorMessage += stringMessages.imageResizeError(mediaTag.getName(), minRatio, maxRatio, ratio);
                    }
                }
            }
            return errorMessage;
        }
        
        private String getSizeErrorMessage(String fileName, MediaTagConstants mediaTag, StringMessages stringMessages) {
            String errorMessage = stringMessages.imageSizeError(fileName + " (" + mediaTag.getName() + ")", mediaTag.getMinWidth(),
                    mediaTag.getMaxWidth(), mediaTag.getMinHeight(), mediaTag.getMaxHeight());
            return errorMessage;
        }
        
        private String getImageToSmallErrorMessage(MediaTagConstants mediaTag, StringMessages stringMessages) {
            String errorMessage = stringMessages.imageToSmallError(mediaTag.getName(), mediaTag.getMinWidth(),
                    mediaTag.getMinHeight());
            return errorMessage;
        }
    }

    public ImageDialog(Date creationDate, SailingServiceAsync sailingService, StringMessages stringMessages,
            FileStorageServiceConnectionTestObservable storageServiceAvailable,
            DialogCallback<List<ImageResizingTaskDTO>> callback) {
        this(creationDate, sailingService, stringMessages, storageServiceAvailable,
                new ImageParameterValidator(stringMessages, storageServiceAvailable), callback);
    }

    private ImageDialog(Date creationDate, SailingServiceAsync sailingService, StringMessages stringMessages,
            FileStorageServiceConnectionTestObservable storageServiceAvailable, ImageParameterValidator validator,
            DialogCallback<List<ImageResizingTaskDTO>> callback) {
        super(stringMessages.image(), null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.creationDate = creationDate;
        fileInfoVPanel = new VerticalPanel();
        getDialogBox().getWidget().setWidth("730px");
        busyIndicator = new SimpleBusyIndicator();
        imageURLAndUploadComposite = new URLFieldWithFileUpload(stringMessages, true, true, true, "image/*");
        getCancelButton().addClickHandler(clickEvent-> imageURLAndUploadComposite.deleteCurrentFile());
        imageURLAndUploadComposite.addValueChangeHandler(new ValueChangeHandler<Map<String, String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Map<String, String>> event) {
                Map<String, String> imageUrls = event.getValue();
                if (imageUrls == null || imageUrls.isEmpty()) {
                    fileInfoVPanel.clear();
                } else {
                    busyIndicator.setBusy(true);
                    busyCounter = 0;
                    for (final String imageUrl : imageUrls.keySet()) {
                        if (!imageDimensionsMap.containsKey(imageUrl)) {
                            busyCounter += 1;
                            ImageDialog.this.sailingService.resolveImageDimensions(imageUrl,
                                    new AsyncCallback<Pair<Integer, Integer>>() {
                                        @Override
                                        public void onSuccess(Pair<Integer, Integer> imageSize) {
                                            GWT.log("resolve image dimensions - SUCCESS " + imageSize);
                                            imageDimensionsMap.put(imageUrl, imageSize);
                                            busyCounter -= 1;
                                            if (busyCounter <= 0) {
                                                busyIndicator.setBusy(false);
                                            }
                                            validateAndUpdate();
                                        }

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            GWT.log("resolve image dimensions - FAILURE", caught);
                                            busyCounter -= 1;
                                            if (busyCounter <= 0) {
                                                busyIndicator.setBusy(false);
                                            }
                                        }
                                    });
                        }
                    }
                    if (busyCounter <= 0) {
                        busyIndicator.setBusy(false);
                    }
                }
                validateAndUpdate();
            }
        });
        //the observer has to be registered after creating the URLFieldWithFileUpload
        storageServiceAvailable.registerObserver(this);
        expandedUi = new ExpandedUiWithCheckboxes<String>(stringMessages, IconResources.INSTANCE.removeIcon(),
                /* suggestValues */ MediaTagConstants.imageTagSuggestions, stringMessages.enterTagsForTheImage(), 30,
                stringMessages.allowResizing());
        expandedUi.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                validateAndUpdate();
            }
        });
        validator.setCheckBoxes(expandedUi.getCheckBoxes());
        tagsListEditor = new StringListInlineEditorComposite(Collections.<String> emptyList(), expandedUi);
        tagsListEditor.addValueChangeHandler(new ValueChangeHandler<Iterable<String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Iterable<String>> event) {
                validateAndUpdate();
            }
        });
        imageDimensionsMap = new HashMap<>(4);
    }

    /**
     * Creates a {@link ImageResizingTaskDTO} which contains an {@link MediaTagConstants} for every tag that does not fit to the boundaries of the image.
     * All tags that do not imply a resize will stay together on a single {@link ImageDTO} and are not stored in the {@link ImageResizingTaskDTO}.
     * There will be a {@link ImageDTO} for each of the other {@link MediaTagConstants}.
     * For a lookout to further progressing see {@link SailingService#resizeImage(ImageResizingTaskDTO)}
     */
    @Override
    protected List<ImageResizingTaskDTO> getResult() {
        final List<String> tags = new ArrayList<String>();
        for (String tag : tagsListEditor.getValue()) {
            tags.add(tag);
        }
        final List<MediaTagConstants> notCheckedMediaTags = new ArrayList<MediaTagConstants>();
        for (int i = 0; i < tags.size(); i++) {
            if (Arrays.asList(MediaTagConstants.values()).contains(MediaTagConstants.fromName(tags.get(i)))
                    && !expandedUi.getCheckBoxes().get(i).getValue()) {
                notCheckedMediaTags.add(MediaTagConstants.fromName(tags.get(i)));
            }
        }
        final List<MediaTagConstants> mediaTags = new ArrayList<MediaTagConstants>();
        for (int i = 0; i < tags.size(); i++) {
            if (Arrays.asList(MediaTagConstants.values()).contains(MediaTagConstants.fromName(tags.get(i)))
                    && expandedUi.getCheckBoxes().get(i).getValue()) {
                mediaTags.add(MediaTagConstants.fromName(tags.get(i)));
            }
        }
        ArrayList<ImageResizingTaskDTO> results = new ArrayList<>(imageURLAndUploadComposite.getUris().size());
        List<String> uris = imageURLAndUploadComposite.getUris();
        fileInfoVPanel.clear();
        for (int i = 0; i < uris.size(); i++) {
            final String imageURL = uris.get(i);
            final String fileName = ImageDialog.extractFileName(imageURL);
            final ImageDTO image = new ImageDTO(imageURL, creationDate);
            image.setTitle(titleTextBox.getValue());
            image.setSubtitle(subtitleTextBox.getValue());
            image.setCopyright(copyrightTextBox.getValue());
            final Pair<Integer, Integer> dims = imageDimensionsMap.get(imageURL);
            final Label fileInfoText;
            if (dims != null) {
                image.setSizeInPx(dims.getA(), dims.getB());
                fileInfoText = new Label(fileName + " (" + dims.getA() + "x" + dims.getB() + ")");
            } else {
                fileInfoText = new Label(fileName);
            }
            image.setTags(tags);
            final List<MediaTagConstants> resizeTags = new ArrayList<>();
            for (final MediaTagConstants mediaTag : mediaTags) {
                if (imageNeedsResizeForTag(image, mediaTag)) {
                    resizeTags.add(mediaTag);
                    fileInfoText.setText(fileInfoText.getText() + " - " + stringMessages.resize() + " (" + mediaTag.name() + ")");
                }
            }
            for (final MediaTagConstants mediaTag : notCheckedMediaTags) {
                if (imageNeedsResizeForTag(image, mediaTag)) {
                    fileInfoText.setText(fileInfoText.getText() + " - " + stringMessages.resize() + " (" + mediaTag.name() + ")");
                    fileInfoText.setStyleName("errorLabel");
                }
            }
            fileInfoVPanel.add(fileInfoText);
            results.add(new ImageResizingTaskDTO(image, resizeTags));
        }
        return results;
    }
    
    private static String extractFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private static boolean imageNeedsResizeForTag(ImageDTO image, MediaTagConstants mediaTag) {
        final boolean widthExceeded = image != null && image.getWidthInPx() != null && image.getWidthInPx() > mediaTag.getMaxWidth();
        final boolean heightExceeded = image != null && image.getWidthInPx() != null && image.getHeightInPx() > mediaTag.getMaxHeight();
        return widthExceeded || heightExceeded;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }
        Grid grid = new Grid(10, 2);
        grid.setWidget(0, 0, new Label(stringMessages.createdAt() + ":"));
        grid.setWidget(0, 1, createdAtLabel);
        grid.setWidget(1, 0, new HTML("&nbsp;"));
        grid.setWidget(1, 1, busyIndicator);
        grid.setWidget(2,  0, new Label(stringMessages.imageURL() + ":"));
        grid.setWidget(2, 1, imageURLAndUploadComposite);
        grid.setWidget(3, 0, new Label(stringMessages.fileUpload() + ":"));
        grid.setWidget(3, 1, fileInfoVPanel);
        grid.setWidget(4, 0, new HTML("&nbsp;"));
        grid.setWidget(5,  0, new Label(stringMessages.title() + ":"));
        grid.setWidget(5, 1, titleTextBox);
        grid.setWidget(6,  0, new Label(stringMessages.subtitle() + ":"));
        grid.setWidget(6, 1, subtitleTextBox);
        grid.setWidget(7, 0, new Label(stringMessages.copyright() + ":"));
        grid.setWidget(7, 1, copyrightTextBox);
        grid.setWidget(8, 0, new HTML("&nbsp;"));
        grid.setWidget(9, 0, new Label(stringMessages.tags() + ":"));
        grid.setWidget(9, 1, tagsListEditor);
        panel.add(grid);
        return panel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return imageURLAndUploadComposite.getInitialFocusWidget();
    }
    
    @Override
    public void onFileStorageServiceTestPassed() {
        imageURLAndUploadComposite.setUploadEnabled(true);
    }
}
