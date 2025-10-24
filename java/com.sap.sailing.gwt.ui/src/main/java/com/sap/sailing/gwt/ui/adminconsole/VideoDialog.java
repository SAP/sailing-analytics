package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.adminconsole.FileStorageServiceConnectionTestObservable.FileStorageServiceConnectionTestObserver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.adminconsole.URLFieldWithFileUpload;
import com.sap.sse.gwt.client.GWTLocaleUtil;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.controls.listedit.GenericStringListInlineEditorComposite;
import com.sap.sse.gwt.client.controls.listedit.StringListInlineEditorComposite;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.media.VideoDTO;

public abstract class VideoDialog extends DataEntryDialog<List<VideoDTO>>
        implements FileStorageServiceConnectionTestObserver {
    protected final StringMessages stringMessages;
    protected final URLFieldWithFileUpload videoURLAndUploadComposite;
    protected final Date creationDate;
    protected Label createdAtLabel;
    protected TextBox subtitleTextBox;
    protected TextBox copyrightTextBox;
    private Grid multiVideoGrid;
    private final TextBox titleTextBox = new TextBox();
    private final ListBox mimeTypeListBox = createMimeTextSelection();
    protected final ListBox localeListBox;
    protected final URLFieldWithFileUpload thumbnailURLAndUploadComposite;
    protected StringListInlineEditorComposite tagsListEditor;
    private final List<VideoTmpData> videoTmpDatas;
    private FlexTable formFlexTable;

    private static class VideoTmpData {
        String uri;
        String fileName;
        String title;
        Integer lengthInSeconds;
        MimeType mimeType;
        @Override
        public String toString() {
            return "VideoTmpData [uri=" + uri + ", fileName=" + fileName + ", title=" + title + ", lengthInSeconds="
                    + lengthInSeconds + ", mimeType=" + mimeType + "]";
        }
    }

    protected static class VideoParameterValidator implements Validator<List<VideoDTO>> {
        private StringMessages stringMessages;

        public VideoParameterValidator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(List<VideoDTO> videosToValidate) {
            StringJoiner errorJoiner = new StringJoiner("\n");
            for (VideoDTO videoToValidate : videosToValidate) {
                String errorMessage = null;

                if (videoToValidate.getSourceRef() == null || videoToValidate.getSourceRef().isEmpty()) {
                    errorMessage = stringMessages.pleaseEnterNonEmptyUrl();
                } else if (videoToValidate.getMimeType() == null) {
                    errorMessage = "You must select the mime type for the video.";
                }
                if (errorMessage != null) {
                    errorJoiner.add(errorMessage);
                }
            }
            return errorJoiner.toString();
        }
    }

    protected void initVideoTmpDatas(String uri, String title, Integer lengthInSeconds, MimeType mimeType) {
        VideoTmpData videoTmpData = new VideoTmpData();
        videoTmpDatas.add(videoTmpData);
        videoTmpData.uri = uri;
        videoTmpData.fileName = extractFileName(uri);
        videoTmpData.title = title;
        videoTmpData.lengthInSeconds = lengthInSeconds;
        videoTmpData.mimeType = mimeType;
        renderMultiFileTable(false);
        setFieldsEnabled(true);
    }

    private String extractFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public VideoDialog(Date createdAtDate, VideoParameterValidator validator, StringMessages stringMessages,
            FileStorageServiceConnectionTestObservable storageServiceAvailable, DialogCallback<List<VideoDTO>> callback) {
        super(stringMessages.video(), null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        this.stringMessages = stringMessages;
        this.creationDate = createdAtDate;
        videoTmpDatas = new ArrayList<VideoDialog.VideoTmpData>();
        getDialogBox().getWidget().setWidth("730px");
        localeListBox = createListBox(false);
        for (String locale : GWTLocaleUtil.getAvailableLocalesAndDefault()) {
            localeListBox.addItem(GWTLocaleUtil.getDecoratedLanguageDisplayNameWithDefaultLocaleSupport(locale),
                    locale == null ? "" : locale);
        }
        videoURLAndUploadComposite = new URLFieldWithFileUpload(stringMessages, true, true, true, "audio/*,video/*");
        videoURLAndUploadComposite.addValueChangeHandler(new ValueChangeHandler<Map<String, String>>() {
            @Override
            public void onValueChange(ValueChangeEvent<Map<String, String>> event) {
                validateAndUpdate();
                videoTmpDatas.clear();
                setFieldsEnabled(false);
                GWT.log("value change event. " + videoURLAndUploadComposite.getValue());
                formFlexTable.getRowFormatter().setVisible(5, false);
                for (Entry<String, String> upload : videoURLAndUploadComposite.getValue().entrySet()) {
                    VideoTmpData videoTmpData = new VideoTmpData();
                    videoTmpDatas.add(videoTmpData);
                    setFieldsEnabled(true);
                    videoTmpData.uri = upload.getKey();
                    videoTmpData.fileName = upload.getValue();
                    videoTmpData.mimeType = MimeType.extractFromUrl(videoTmpData.uri);
                    final String title;
                    if (videoTmpData.fileName.contains(".")) {
                        title = videoTmpData.fileName.substring(0, videoTmpData.fileName.lastIndexOf('.'));
                    } else {
                        title = videoTmpData.fileName;
                    }
                    videoTmpData.title = title;
                }
                renderMultiFileTable(true);
            }
        });
        thumbnailURLAndUploadComposite = new URLFieldWithFileUpload(stringMessages, false, true, true, "image/*");
        tagsListEditor = new StringListInlineEditorComposite(Collections.<String> emptyList(),
                new GenericStringListInlineEditorComposite.ExpandedUi<String>(stringMessages,
                        IconResources.INSTANCE.removeIcon(), /* suggestValues */
                        MediaTagConstants.videoTagSuggestions, stringMessages.enterTagsForTheVideo(), 50));
        // the observer has to be registered after creating the URLFieldWithFileUpload
        storageServiceAvailable.registerObserver(this);
        getCancelButton().addClickHandler(clickEvent-> {
            videoURLAndUploadComposite.deleteCurrentFile();
            thumbnailURLAndUploadComposite.deleteCurrentFile();
        });
    }

    private void renderMultiFileTable(boolean updateMediaInfo) {
        GWT.log("video tmp data size = " + videoTmpDatas.size());
        GWT.log("video tmp data = " + videoTmpDatas);
        if (formFlexTable == null && videoTmpDatas.size() > 0) {
            initFormFlexTable();
        }
        if (formFlexTable != null && videoTmpDatas.size() > 0) {
            if (videoTmpDatas.size() == 1) {
                formFlexTable.getRowFormatter().setVisible(2, true);
                formFlexTable.getRowFormatter().setVisible(3, true);
                formFlexTable.getRowFormatter().setVisible(4, true);
                formFlexTable.getRowFormatter().setVisible(5, false);
                VideoTmpData videoTmpData = videoTmpDatas.get(0);
                titleTextBox.setText(videoTmpData.title);
                GWT.log("video tmp data = " + videoTmpData);
                titleTextBox.addChangeHandler(new ChangeHandler() {
                    @Override
                    public void onChange(ChangeEvent event) {
                        videoTmpData.title = titleTextBox.getValue();
                    }
                });
                mimeTypeListBox.addChangeHandler(new ChangeHandler() {
                    @Override
                    public void onChange(ChangeEvent event) {
                        videoTmpData.mimeType = getSelectedMimeType(mimeTypeListBox);
                    }
                });
                setSelectedMimeType(mimeTypeListBox, videoTmpData.mimeType);
            } else {
                formFlexTable.getRowFormatter().setVisible(2, false);
                formFlexTable.getRowFormatter().setVisible(3, false);
                formFlexTable.getRowFormatter().setVisible(4, false);
                formFlexTable.getRowFormatter().setVisible(5, true);
                Grid multiVideoGrid = new Grid(videoTmpDatas.size() + 1, 3);
                multiVideoGrid.setWidth("100%");
                formFlexTable.setWidget(5, 0, multiVideoGrid);
                // add header
                multiVideoGrid.setWidget(0, 0, new Label(stringMessages.name()));
                multiVideoGrid.setWidget(0, 1, new Label(stringMessages.title()));
                multiVideoGrid.setWidget(0, 2, new Label(stringMessages.mimeType()));
                // process single upload items
                for (int i = 0; i < videoTmpDatas.size(); i++) {
                    VideoTmpData videoTmpData = videoTmpDatas.get(i);
                    TextBox titleTextBox = new TextBox();
                    titleTextBox.setText(videoTmpData.title);
                    titleTextBox.addChangeHandler(new ChangeHandler() {
                        @Override
                        public void onChange(ChangeEvent event) {
                            videoTmpData.title = titleTextBox.getValue();
                        }
                    });
                    Label nameLabel = new Label(videoTmpData.fileName);
                    ListBox mimeTypeListBox = createMimeTextSelection();
                    mimeTypeListBox.addChangeHandler(new ChangeHandler() {
                        @Override
                        public void onChange(ChangeEvent event) {
                            videoTmpData.mimeType = getSelectedMimeType(mimeTypeListBox);
                        }
                    });
                    setSelectedMimeType(mimeTypeListBox, videoTmpData.mimeType);
                    multiVideoGrid.setWidget(i + 1, 0, nameLabel);
                    multiVideoGrid.setWidget(i + 1, 1, titleTextBox);
                    multiVideoGrid.setWidget(i + 1, 2, mimeTypeListBox);
                }
            }
        }
    }

    @Override
    protected List<VideoDTO> getResult() {
        List<VideoDTO> results = new ArrayList<VideoDTO>(videoURLAndUploadComposite.getUris().size());
        for (VideoTmpData videoData : videoTmpDatas) {
            VideoDTO videoDTO = new VideoDTO(videoData.uri, videoData.mimeType, creationDate);
            videoDTO.setTitle(videoData.title);
            videoDTO.setSubtitle(subtitleTextBox.getValue());
            videoDTO.setCopyright(copyrightTextBox.getValue());
            videoDTO.setLocale(getSelectedLocale());
            List<String> tags = new ArrayList<String>();
            for (String tag : tagsListEditor.getValue()) {
                tags.add(tag);
            }
            videoDTO.setTags(tags);
            videoDTO.setThumbnailRef(thumbnailURLAndUploadComposite.getUri());
            videoDTO.setLengthInSeconds(videoData.lengthInSeconds);
            results.add(videoDTO);
        }
        return results;
    }

    private ListBox createMimeTextSelection() {
        ListBox mimeTypeListBox = createListBox(false);
        mimeTypeListBox.ensureDebugId("mimeTypeListBox");
        mimeTypeListBox.addItem(MimeType.unknown.name());
        mimeTypeListBox.addItem(MimeType.mp4.name());
        mimeTypeListBox.addItem(MimeType.mov.name());
        mimeTypeListBox.addItem(MimeType.mp4panorama.name());
        mimeTypeListBox.addItem(MimeType.mp4panoramaflip.name());
        mimeTypeListBox.addItem(MimeType.ogv.name());
        mimeTypeListBox.addItem(MimeType.qt.name());
        mimeTypeListBox.addItem(MimeType.webm.name());
        mimeTypeListBox.addItem(MimeType.youtube.name());
        mimeTypeListBox.addItem(MimeType.vimeo.name());
        return mimeTypeListBox;
    }

    protected void setSelectedMimeType(ListBox mimeTypeListBox, MimeType mimeType) {
        for (int i = 0; i < mimeTypeListBox.getItemCount(); i++) {
            if (mimeTypeListBox.getItemText(i).equals(mimeType.name())) {
                mimeTypeListBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private MimeType getSelectedMimeType(ListBox mimeTypeListBox) {
        MimeType result = null;
        int selectedIndex = mimeTypeListBox.getSelectedIndex();
        if (selectedIndex >= 0) {
            result = MimeType.valueOf(mimeTypeListBox.getSelectedValue());
        }
        return result;
    }

    protected void setSelectedLocale(String locale) {
        for (int i = 0; i < localeListBox.getItemCount(); i++) {
            if (Util.equalsWithNull(localeListBox.getValue(i), locale)) {
                localeListBox.setSelectedIndex(i);
                return;
            }
        }
        localeListBox.setSelectedIndex(0);
    }

    private String getSelectedLocale() {
        return localeListBox.getSelectedValue();
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }
        initFormFlexTable();
        panel.add(formFlexTable);
        return panel;
    }
    
    private void initFormFlexTable() {
        formFlexTable = new FlexTable();
        formFlexTable.ensureDebugId("videoDialogFormTable");
        formFlexTable.setWidget(0, 0, new Label(stringMessages.createdAt() + ":"));
        formFlexTable.setWidget(0, 1, createdAtLabel);
        formFlexTable.setWidget(1, 0, new Label(stringMessages.videoUrl() + ":"));
        formFlexTable.setWidget(1, 1, videoURLAndUploadComposite);
        formFlexTable.setWidget(2, 0, new Label(stringMessages.title() + ":"));
        formFlexTable.setWidget(2, 1, titleTextBox);
        formFlexTable.setWidget(3, 0, new Label(stringMessages.mimeType() + ":"));
        formFlexTable.setWidget(3, 1, mimeTypeListBox);
        formFlexTable.setWidget(5, 0, multiVideoGrid);
        formFlexTable.getFlexCellFormatter().setColSpan(5, 0, 2);
        formFlexTable.getRowFormatter().setVisible(5, false);
        formFlexTable.setWidget(6, 0, new Label(stringMessages.subtitle() + ":"));
        formFlexTable.setWidget(6, 1, subtitleTextBox);
        formFlexTable.setWidget(7, 0, new Label(stringMessages.copyright() + ":"));
        formFlexTable.setWidget(7, 1, copyrightTextBox);
        formFlexTable.setWidget(8, 0, new Label(stringMessages.language() + ":"));
        formFlexTable.setWidget(8, 1, localeListBox);
        formFlexTable.setWidget(9, 0, new Label(stringMessages.tags() + ":"));
        formFlexTable.setWidget(9, 1, tagsListEditor);
        formFlexTable.setWidget(10, 0, new Label(stringMessages.thumbnailUrl() + ":"));
        formFlexTable.setWidget(10, 1, thumbnailURLAndUploadComposite);
        setFieldsEnabled(videoTmpDatas.size() > 0);
    }
    
    private void setFieldsEnabled(boolean enabled) {
        formFlexTable.getRowFormatter().setVisible(2, enabled);
        formFlexTable.getRowFormatter().setVisible(3, enabled);
        formFlexTable.getRowFormatter().setVisible(6, enabled);
        formFlexTable.getRowFormatter().setVisible(7, enabled);
        formFlexTable.getRowFormatter().setVisible(8, enabled);
        formFlexTable.getRowFormatter().setVisible(9, enabled);
        formFlexTable.getRowFormatter().setVisible(10, enabled);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return videoURLAndUploadComposite.getInitialFocusWidget();
    }

    @Override
    public void onFileStorageServiceTestPassed() {
        videoURLAndUploadComposite.setUploadEnabled(true);
        thumbnailURLAndUploadComposite.setUploadEnabled(true);
    }
}
