package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.media.VideoDTO;

/**
 * /** A composite showing the list of media videos
 * 
 * @author Frank Mittag (C5163974)
 */
public class VideosListComposite extends Composite {
    private final StringMessages stringMessages;

    private CellTable<VideoDTO> videoTable;
    private SingleSelectionModel<VideoDTO> videoSelectionModel;
    private ListDataProvider<VideoDTO> videoListDataProvider;
    private final Label noVideosLabel;
    private final FileStorageServiceConnectionTestObservable storageServiceAvailable;

    private final SimplePanel mainPanel;
    private final VerticalPanel panel;

    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a target=\"_blank\" href=\"{0}\">{1}</a>")
        SafeHtml cell(SafeUri url, String displayName);
    }

    private static AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    private final AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

    public VideosListComposite(final StringMessages stringMessages, FileStorageServiceConnectionTestObservable storageServiceAvailable) {
        this.stringMessages = stringMessages;
        this.storageServiceAvailable = storageServiceAvailable;
        mainPanel = new SimplePanel();
        panel = new VerticalPanel();
        mainPanel.setWidget(panel);

        HorizontalPanel videosControlsPanel = new HorizontalPanel();
        videosControlsPanel.setSpacing(5);
        panel.add(videosControlsPanel);

        Button createVideoBtn = new Button("Add gallery video");
        createVideoBtn.ensureDebugId("addVideoBtn");
        createVideoBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateVideoDialog(MediaTagConstants.GALLERY.getName());
            }
        });
        videosControlsPanel.add(createVideoBtn);

        Button addLiveStreamBtn = new Button("Add livestream video");
        addLiveStreamBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateVideoDialog(MediaTagConstants.LIVESTREAM.getName());
            }
        });
        videosControlsPanel.add(addLiveStreamBtn);

        Button addHighlightBtn = new Button("Add highlight video");
        addHighlightBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateVideoDialog(MediaTagConstants.HIGHLIGHT.getName());
            }
        });
        videosControlsPanel.add(addHighlightBtn);

        videoSelectionModel = new SingleSelectionModel<VideoDTO>();
        videoListDataProvider = new ListDataProvider<VideoDTO>();
        videoTable = createVideosTable();
        videoTable.ensureDebugId("VideosCellTable");
        videoTable.setVisible(false);

        videoSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
            }
        });

        panel.add(videoTable);

        noVideosLabel = new Label("No videos defined yet.");
        noVideosLabel.ensureDebugId("NoVideosLabel");
        noVideosLabel.setWordWrap(false);
        panel.add(noVideosLabel);

        initWidget(mainPanel);
    }

    private CellTable<VideoDTO> createVideosTable() {
        CellTable<VideoDTO> table = new BaseCelltable<VideoDTO>(/* pageSize */10000, tableRes);
        videoListDataProvider.addDataDisplay(table);
        table.setWidth("100%");

        AnchorCell anchorCell = new AnchorCell();
        Column<VideoDTO, SafeHtml> videoTitleAsLinkColumn = new Column<VideoDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(VideoDTO video) {
                String linkName = video.getTitle() != null ? video.getTitle() : "";
                if(linkName.length() >= 25) {
                    linkName = linkName.substring(0, 22) + "...";     
                }
                return ANCHORTEMPLATE.cell(UriUtils.fromString(video.getSourceRef()), linkName);
            }
        };

        TextColumn<VideoDTO> createdAtDateColumn = new TextColumn<VideoDTO>() {
            @Override
            public String getValue(VideoDTO video) {
                return DateAndTimeFormatterUtil.formatDateAndTime(video.getCreatedAtDate());
            }
        };

        TextColumn<VideoDTO> mimeTypeColumn = new TextColumn<VideoDTO>() {
            @Override
            public String getValue(VideoDTO video) {
                return video.getMimeType() != null ? video.getMimeType().name() : "";
            }
        };
        
        TextColumn<VideoDTO> localeColumn = new TextColumn<VideoDTO>() {
            @Override
            public String getValue(VideoDTO video) {
                return video.getLocale() != null ? video.getLocale() : "";
            }
        };

        SafeHtmlCell tagsCell = new SafeHtmlCell();
        Column<VideoDTO, SafeHtml> tagsColumn = new Column<VideoDTO, SafeHtml>(tagsCell) {
            @Override
            public SafeHtml getValue(VideoDTO video) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                int tagsCount = video.getTags().size();
                int i = 1;
                for (String tag : video.getTags()) {
                    builder.appendEscaped(tag);
                    if (i < tagsCount) {
                        builder.appendHtmlConstant(",&nbsp;");
                        // not more than 4 tags per line
                        if (i % 4 == 0) {
                            builder.appendHtmlConstant("<br>");
                        }
                    }
                    i++;
                }
                return builder.toSafeHtml();
            }
        };

        ImagesBarColumn<VideoDTO, VideoConfigImagesBarCell> videoActionColumn = new ImagesBarColumn<VideoDTO, VideoConfigImagesBarCell>(
                new VideoConfigImagesBarCell(stringMessages));
        videoActionColumn.setFieldUpdater(new FieldUpdater<VideoDTO, String>() {
            @Override
            public void update(int index, VideoDTO video, String value) {
                if (ImageConfigImagesBarCell.ACTION_REMOVE.equals(value)) {
                    videoListDataProvider.getList().remove(video);
                    updateTableVisisbilty();
                } else if (ImageConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    openEditVideoDialog(video);
                }
            }
        });

        videoTitleAsLinkColumn.setSortable(true);
        createdAtDateColumn.setSortable(true);

        table.addColumn(videoTitleAsLinkColumn, stringMessages.title());
        table.addColumn(createdAtDateColumn, "Created At");
        table.addColumn(mimeTypeColumn, stringMessages.mimeType());
        table.addColumn(localeColumn, "Lang");
        table.addColumn(tagsColumn, "Tags");
        table.addColumn(videoActionColumn, stringMessages.actions());
        table.addColumnSortHandler(getVideoTableColumnSortHandler(videoListDataProvider.getList(), videoTitleAsLinkColumn, createdAtDateColumn));
        table.getColumnSortList().push(createdAtDateColumn);

        return table;
    }

    private ListHandler<VideoDTO> getVideoTableColumnSortHandler(List<VideoDTO> videoRecords,
            Column<VideoDTO, SafeHtml> titleColumn, TextColumn<VideoDTO> createdAtDateColumn) {
        ListHandler<VideoDTO> result = new ListHandler<VideoDTO>(videoRecords);
        result.setComparator(titleColumn, new Comparator<VideoDTO>() {
            @Override
            public int compare(VideoDTO i1, VideoDTO i2) {
                return new NaturalComparator().compare(i1.getTitle(), i2.getTitle());
            }
        });
        result.setComparator(createdAtDateColumn, new Comparator<VideoDTO>() {
            @Override
            public int compare(VideoDTO e1, VideoDTO e2) {
                int result;
                if (e1.getCreatedAtDate() != null && e2.getCreatedAtDate() != null) {
                    result = e2.getCreatedAtDate().compareTo(e1.getCreatedAtDate());
                } else if (e1.getCreatedAtDate() == null && e2.getCreatedAtDate() != null) {
                    result = 1;
                } else if (e1.getCreatedAtDate() != null && e2.getCreatedAtDate() == null) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        });
        return result;
    }

    private void openCreateVideoDialog(String initialTag) {
        VideoCreateDialog dialog = new VideoCreateDialog(initialTag, stringMessages, storageServiceAvailable,
                new DialogCallback<List<VideoDTO>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(List<VideoDTO> newVideos) {
                        videoListDataProvider.getList().addAll(newVideos);
                        updateTableVisisbilty();
                    }
                });
        dialog.ensureDebugId("createVideoDialog");
        dialog.show();
    }

    private void openEditVideoDialog(final VideoDTO selectedVideo) {
        VideoEditDialog dialog = new VideoEditDialog(selectedVideo, stringMessages, storageServiceAvailable,
                new DialogCallback<List<VideoDTO>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(List<VideoDTO> updatedVideos) {
                        videoListDataProvider.getList().remove(selectedVideo);
                        videoListDataProvider.getList().add(updatedVideos.get(0));
                        updateTableVisisbilty();
                    }
                });
        dialog.ensureDebugId("editVideoDialog");
        dialog.show();
    }

    private void updateTableVisisbilty() {
        if (videoListDataProvider.getList().isEmpty()) {
            videoTable.setVisible(false);
            noVideosLabel.setVisible(true);
        } else {
            videoTable.setVisible(true);
            noVideosLabel.setVisible(false);
        }
    }
    
    public void fillVideos(List<? extends VideoDTO> videos) {
        videoSelectionModel.clear();
        videoListDataProvider.getList().clear();
        videoListDataProvider.getList().addAll(videos);
        
        updateTableVisisbilty();
    }

    public List<VideoDTO> getAllVideos() {
        return videoListDataProvider.getList();
    }
}
