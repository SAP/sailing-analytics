package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;

/**
 * A composite showing the list of media images
 * 
 * @author Frank Mittag (C5163974)
 */
public class ImagesListComposite extends Composite {
    private final StringMessages stringMessages;
    private final SailingServiceWriteAsync sailingServiceWrite;
    
    private CellTable<ImageDTO> imageTable;
    private SingleSelectionModel<ImageDTO> imageSelectionModel;
    private ListDataProvider<ImageDTO> imageListDataProvider;
    private final Label noImagesLabel;
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

    public ImagesListComposite(final SailingServiceWriteAsync sailingServiceWrite, final StringMessages stringMessages,
            final FileStorageServiceConnectionTestObservable storageServiceAvailable) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.stringMessages = stringMessages;
        this.storageServiceAvailable = storageServiceAvailable;
        mainPanel = new SimplePanel();
        panel = new VerticalPanel();
        mainPanel.setWidget(panel);

        HorizontalPanel imagesControlsPanel = new HorizontalPanel();
        imagesControlsPanel.setSpacing(5);
        panel.add(imagesControlsPanel);

        Button addPhotoBtn = new Button(stringMessages.addGalleryPhoto());
        addPhotoBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateImageDialog(MediaTagConstants.GALLERY.getName());
            }
        });
        imagesControlsPanel.add(addPhotoBtn);

        Button addStateImageBtn = new Button(stringMessages.addStageImage());
        addStateImageBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateImageDialog(MediaTagConstants.STAGE.getName());
            }
        });
        imagesControlsPanel.add(addStateImageBtn);

        Button addEventTeaseImageBtn = new Button(stringMessages.addTeaserImage());
        addEventTeaseImageBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateImageDialog(MediaTagConstants.TEASER.getName());
            }
        });
        imagesControlsPanel.add(addEventTeaseImageBtn);

        Button addLogoImageBtn = new Button(stringMessages.addEventLogo());
        addLogoImageBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openCreateImageDialog(MediaTagConstants.LOGO.getName());
            }
        });
        imagesControlsPanel.add(addLogoImageBtn);
 
        imageSelectionModel = new SingleSelectionModel<ImageDTO>();
        imageListDataProvider = new ListDataProvider<ImageDTO>();
        imageTable = createImagesTable();
        imageTable.ensureDebugId("ImagesCellTable");
        imageTable.setVisible(false);

        imageSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
            }
        });

        panel.add(imageTable);

        noImagesLabel = new Label(stringMessages.noImagesDefinedYet());
        noImagesLabel.ensureDebugId("NoImagesLabel");
        noImagesLabel.setWordWrap(false);
        panel.add(noImagesLabel);

        initWidget(mainPanel);
    }

    private CellTable<ImageDTO> createImagesTable() {
        CellTable<ImageDTO> table = new BaseCelltable<ImageDTO>(/* pageSize */10000, tableRes);
        imageListDataProvider.addDataDisplay(table);
        table.setWidth("100%");

        AnchorCell anchorCell = new AnchorCell();
        Column<ImageDTO, SafeHtml> shortImageNameColumn = new Column<ImageDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(ImageDTO image) {
                String linkName = "";
                String link = image.getSourceRef();
                if(link.endsWith("/")) {
                    link = link.substring(0, link.length() - 1);
                }
                int index = link.lastIndexOf("/");
                if(index > 0) {
                    linkName =  link.substring(index+1, link.length());
                }
                return ANCHORTEMPLATE.cell(UriUtils.fromString(image.getSourceRef()), linkName);
            }
        };

        TextColumn<ImageDTO> titleColumn = new TextColumn<ImageDTO>() {
            @Override
            public String getValue(ImageDTO image) {
                return image.getTitle() != null ? image.getTitle() : "";
            }
        };
        TextColumn<ImageDTO> sizeColumn = new TextColumn<ImageDTO>() {
            @Override
            public String getValue(ImageDTO image) {
                String imageSize = "";
                if (image.getWidthInPx() != null && image.getHeightInPx() != null) {
                    imageSize = image.getWidthInPx() + " x " + image.getHeightInPx(); 
                }
                return imageSize;
            }
        };

        TextColumn<ImageDTO> createdAtDateColumn = new TextColumn<ImageDTO>() {
            @Override
            public String getValue(ImageDTO image) {
                return DateAndTimeFormatterUtil.formatDateAndTime(image.getCreatedAtDate());
            }
        };

        SafeHtmlCell tagsCell = new SafeHtmlCell();
        Column<ImageDTO, SafeHtml> tagsColumn = new Column<ImageDTO, SafeHtml>(tagsCell) {
            @Override
            public SafeHtml getValue(ImageDTO image) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                int tagsCount = image.getTags().size();
                int i = 1;
                for (String tag : image.getTags()) {
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

        ImagesBarColumn<ImageDTO, ImageConfigImagesBarCell> imageActionColumn = new ImagesBarColumn<ImageDTO, ImageConfigImagesBarCell>(
                new ImageConfigImagesBarCell(stringMessages));
        imageActionColumn.setFieldUpdater(new FieldUpdater<ImageDTO, String>() {
            @Override
            public void update(int index, ImageDTO image, String value) {
                if (ImageConfigImagesBarCell.ACTION_REMOVE.equals(value)) {
                    imageListDataProvider.getList().remove(image);
                    updateTableVisibility();
                } else if (ImageConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    openEditImageDialog(image);
                }
            }
        });

        titleColumn.setSortable(true);
        createdAtDateColumn.setSortable(true);

        table.addColumn(shortImageNameColumn, stringMessages.name());
        table.addColumn(titleColumn, stringMessages.title());
        table.addColumn(createdAtDateColumn, stringMessages.createdAt());
        table.addColumn(sizeColumn, stringMessages.size());
        table.addColumn(tagsColumn, stringMessages.tags());
        table.addColumn(imageActionColumn, stringMessages.actions());
        table.addColumnSortHandler(getImageTableColumnSortHandler(imageListDataProvider.getList(), titleColumn, createdAtDateColumn));
        table.getColumnSortList().push(createdAtDateColumn);

        return table;
    }

    private ListHandler<ImageDTO> getImageTableColumnSortHandler(List<ImageDTO> imageRecords,
            TextColumn<ImageDTO> titleColumn, TextColumn<ImageDTO> createdAtDateColumn) {
        ListHandler<ImageDTO> result = new ListHandler<ImageDTO>(imageRecords);
        result.setComparator(titleColumn, new Comparator<ImageDTO>() {
            @Override
            public int compare(ImageDTO i1, ImageDTO i2) {
                return new NaturalComparator().compare(i1.getTitle(), i2.getTitle());
            }
        });
        result.setComparator(createdAtDateColumn, new Comparator<ImageDTO>() {
            @Override
            public int compare(ImageDTO e1, ImageDTO e2) {
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

    private void openCreateImageDialog(String initialTag) {
        ImageCreateDialog dialog = new ImageCreateDialog(initialTag, sailingServiceWrite, stringMessages,
                storageServiceAvailable, new DialogCallback<List<ImageResizingTaskDTO>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(List<ImageResizingTaskDTO> resizingTasks) {
                        for (ImageResizingTaskDTO resizingTask : resizingTasks) {
                            if (resizingTask.getResizingTask().size() != 0) {
                                callResizingServiceAndUpdateTable(resizingTask, null);
                            } else {
                                imageListDataProvider.getList().add(resizingTask.getImage());
                                updateTableVisibility();
                            }
                        }
                    }
                });
        dialog.show();
    }

    private void openEditImageDialog(final ImageDTO selectedImage) {
        ImageEditDialog dialog = new ImageEditDialog(selectedImage, sailingServiceWrite, stringMessages,
                storageServiceAvailable, new DialogCallback<List<ImageResizingTaskDTO>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(List<ImageResizingTaskDTO> resizingTasks) {
                        for (ImageResizingTaskDTO resizingTask : resizingTasks) {
                            if (resizingTask.getResizingTask().size() != 0) {
                                callResizingServiceAndUpdateTable(resizingTask, selectedImage);
                            } else {
                                imageListDataProvider.getList().remove(selectedImage);
                                imageListDataProvider.getList().add(resizingTask.getImage());
                                updateTableVisibility();
                            }
                        }
                    }
                });
        dialog.show();
    }

    /**
     * Calls the resizing service and updates the imageListDataProvider with the returned ImageDTOs
     * 
     * @author Robin Fleige (D067799)
     * 
     * @param resizingTask
     *            The {@link ImageResizingTaskDTO} that contains the information about resizing. The resizingTask
     *            attribute should not be null or empty at this point.
     * @param originalImage
     *            if called from {@link ImageEditDialog} contains the selected image, that will be replaced by the
     *            returned ImageDTOs
     */
    protected void callResizingServiceAndUpdateTable(ImageResizingTaskDTO resizingTask, ImageDTO originalImage) {
        sailingServiceWrite.resizeImage(resizingTask, new AsyncCallback<Set<ImageDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.resizeUnsuccessful(caught.getMessage()), NotificationType.ERROR);
            }

            @Override
            public void onSuccess(Set<ImageDTO> result) {
                for (ImageDTO image : result) {
                    imageListDataProvider.getList().add(image);
                }
                imageListDataProvider.getList().remove(originalImage);
                updateTableVisibility();
                Notification.notify(stringMessages.resizeSuccessfull(), NotificationType.SUCCESS);
            }
        });

    }

    private void updateTableVisibility() {
        if (imageListDataProvider.getList().isEmpty()) {
            imageTable.setVisible(false);
            noImagesLabel.setVisible(true);
        } else {
            imageTable.setVisible(true);
            noImagesLabel.setVisible(false);
        }
    }
    
    public void fillImages(List<? extends ImageDTO> images) {
        imageSelectionModel.clear();
        imageListDataProvider.getList().clear();
        imageListDataProvider.getList().addAll(images);
        updateTableVisibility();
    }

    public List<ImageDTO> getAllImages() {
        return imageListDataProvider.getList();
    }
}
