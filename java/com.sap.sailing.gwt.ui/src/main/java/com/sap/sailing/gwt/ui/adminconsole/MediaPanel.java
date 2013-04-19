package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.client.DataEntryDialog.DialogCallback;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.MediaTrackDialog;
import com.sap.sailing.gwt.ui.client.media.TimeFormatUtil;
import com.sap.sailing.gwt.ui.client.shared.media.MediaTrack;

/**
 * Table inspired by http://gwt.google.com/samples/Showcase/Showcase.html#!CwCellTable
 * 
 * @author D047974
 * 
 */
public class MediaPanel extends FlowPanel {

    private final MediaServiceAsync mediaService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final Grid mediaTracks;
    private CellTable<MediaTrack> mediaTracksTable;
    private ListDataProvider<MediaTrack> mediaTrackListDataProvider = new ListDataProvider<MediaTrack>();

    public MediaPanel(MediaServiceAsync mediaService, ErrorReporter errorReporter, StringMessages stringMessages) {
        this.mediaService = mediaService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        mediaTracks = new Grid();
        mediaTracks.resizeColumns(3);
        add(mediaTracks);
        Button refreshButton = new Button(stringMessages.refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadMediaTracks();
            }

        });
        add(refreshButton);
        Button addUrlButton = new Button(stringMessages.addMediaTrack());
        addUrlButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addUrlMediaTrack();
            }
        });
        add(addUrlButton);

        Button addYoutubeButton = new Button(stringMessages.addYoutubeTrack());
        addYoutubeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addYoutubeMediaTrack();
            }
        });
        add(addYoutubeButton);
        createMediaTracksTable();
    }

    protected void loadMediaTracks() {
        mediaTrackListDataProvider.getList().clear();
        mediaService.getAllMediaTracks(new AsyncCallback<Collection<MediaTrack>>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError(t.toString());
            }

            @Override
            public void onSuccess(Collection<MediaTrack> allMediaTracks) {
                mediaTrackListDataProvider.getList().addAll(allMediaTracks);
                mediaTrackListDataProvider.refresh();
            }
        });

    }

    private void createMediaTracksTable() {
        AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
        // Create a CellTable.

        // Set a key provider that provides a unique key for each contact. If key is
        // used to identify contacts when fields (such as the name and address)
        // change.
        mediaTracksTable = new CellTable<MediaTrack>(1000, tableResources);
        mediaTracksTable.setWidth("100%");

        // Attach a column sort handler to the ListDataProvider to sort the list.
        ListHandler<MediaTrack> sortHandler = new ListHandler<MediaTrack>(mediaTrackListDataProvider.getList());
        mediaTracksTable.addColumnSortHandler(sortHandler);

        // Add a selection model so we can select cells.
        final SelectionModel<MediaTrack> selectionModel = new SingleSelectionModel<MediaTrack>();
        mediaTracksTable.setSelectionModel(selectionModel,
                DefaultSelectionEventManager.<MediaTrack> createDefaultManager());

        // Initialize the columns.
        initTableColumns(selectionModel, sortHandler);

        mediaTrackListDataProvider.addDataDisplay(mediaTracksTable);
        add(mediaTracksTable);
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns(final SelectionModel<MediaTrack> selectionModel,
            ListHandler<MediaTrack> sortHandler) {
        // // Checkbox column. This table will uses a checkbox column for selection.
        // // Alternatively, you can call cellTable.setSelectionEnabled(true) to enable
        // // mouse selection.
        // Column<MediaTrackDTO, Boolean> checkColumn = new Column<ContactInfo, Boolean>(new CheckboxCell(true, false))
        // {
        // @Override
        // public Boolean getValue(ContactInfo object) {
        // // Get the value from the selection model.
        // return selectionModel.isSelected(object);
        // }
        // };
        // cellTable.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        // cellTable.setColumnWidth(checkColumn, 40, Unit.PX);

        // media title
        Column<MediaTrack, String> titleColumn = new Column<MediaTrack, String>(new EditTextCell()) {
            @Override
            public String getValue(MediaTrack mediaTrack) {
                return mediaTrack.title;
            }
        };
        titleColumn.setSortable(true);
        sortHandler.setComparator(titleColumn, new Comparator<MediaTrack>() {
            public int compare(MediaTrack mediaTrack1, MediaTrack mediaTrack2) {
                return mediaTrack1.title.compareTo(mediaTrack2.title);
            }
        });
        mediaTracksTable.addColumn(titleColumn, stringMessages.title());
        titleColumn.setFieldUpdater(new FieldUpdater<MediaTrack, String>() {
            public void update(int index, MediaTrack mediaTrack, String newTitle) {
                // Called when the user changes the value.
                mediaTrack.title = newTitle;
                mediaService.updateTitle(mediaTrack, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(Void allMediaTracks) {
                        mediaTrackListDataProvider.refresh();
                    }
                });
            }
        });
        mediaTracksTable.setColumnWidth(titleColumn, 20, Unit.PCT);

        // url
        Column<MediaTrack, String> urlColumn = new Column<MediaTrack, String>(new EditTextCell()) {
            @Override
            public String getValue(MediaTrack mediaTrack) {
                return mediaTrack.url;
            }
        };
        urlColumn.setSortable(true);
        sortHandler.setComparator(urlColumn, new Comparator<MediaTrack>() {
            public int compare(MediaTrack mediaTrack1, MediaTrack mediaTrack2) {
                return mediaTrack1.url.compareTo(mediaTrack2.url);
            }
        });
        mediaTracksTable.addColumn(urlColumn, stringMessages.url());
        urlColumn.setFieldUpdater(new FieldUpdater<MediaTrack, String>() {
            public void update(int index, MediaTrack mediaTrack, String newUrl) {
                // Called when the user changes the value.
                mediaTrack.url = newUrl;
                mediaService.updateUrl(mediaTrack, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(Void allMediaTracks) {
                        mediaTrackListDataProvider.refresh();
                    }
                });
            }
        });
        mediaTracksTable.setColumnWidth(urlColumn, 100, Unit.PCT);

        // start time
        Column<MediaTrack, String> startTimeColumn = new Column<MediaTrack, String>(new EditTextCell()) {
            @Override
            public String getValue(MediaTrack mediaTrack) {
                return mediaTrack.startTime == null ? "" : TimeFormatUtil.DATETIME_FORMAT.format(mediaTrack.startTime);
            }
        };
        startTimeColumn.setSortable(true);
        sortHandler.setComparator(startTimeColumn, new Comparator<MediaTrack>() {
            public int compare(MediaTrack mediaTrack1, MediaTrack mediaTrack2) {
                return mediaTrack1.startTime.compareTo(mediaTrack2.startTime);
            }
        });
        startTimeColumn.setFieldUpdater(new FieldUpdater<MediaTrack, String>() {
            public void update(int index, MediaTrack mediaTrack, String newStartTime) {
                // Called when the user changes the value.
                mediaTrack.startTime = TimeFormatUtil.DATETIME_FORMAT.parse(newStartTime);
                mediaService.updateStartTime(mediaTrack, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(Void allMediaTracks) {
                        mediaTrackListDataProvider.refresh();
                    }
                });
            }
        });
        mediaTracksTable.addColumn(startTimeColumn, stringMessages.startTime());
        mediaTracksTable.setColumnWidth(startTimeColumn, 100, Unit.PCT);

        // duration
        Column<MediaTrack, String> durationColumn = new Column<MediaTrack, String>(new EditTextCell()) {
            @Override
            public String getValue(MediaTrack mediaTrack) {
                return TimeFormatUtil.milliSecondsToHrsMinSec(mediaTrack.durationInMillis);
            }
        };
        durationColumn.setSortable(true);
        sortHandler.setComparator(durationColumn, new Comparator<MediaTrack>() {
            public int compare(MediaTrack mediaTrack1, MediaTrack mediaTrack2) {
                return Integer.valueOf(mediaTrack1.durationInMillis).compareTo(Integer.valueOf(mediaTrack2.durationInMillis));
            }
        });
        durationColumn.setFieldUpdater(new FieldUpdater<MediaTrack, String>() {
            public void update(int index, MediaTrack mediaTrack, String newDuration) {
                // Called when the user changes the value.
                mediaTrack.durationInMillis = TimeFormatUtil.hrsMinSecToMilliSeconds(newDuration);
                mediaService.updateDuration(mediaTrack, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(Void allMediaTracks) {
                        mediaTrackListDataProvider.refresh();
                    }
                });
            }
        });
        mediaTracksTable.addColumn(durationColumn, stringMessages.duration());
        mediaTracksTable.setColumnWidth(durationColumn, 100, Unit.PCT);

        // delete action
        ImagesBarColumn<MediaTrack, MediaActionBarCell> mediaActionColumn = new ImagesBarColumn<MediaTrack, MediaActionBarCell>(
                new MediaActionBarCell(stringMessages));
        mediaActionColumn.setFieldUpdater(new FieldUpdater<MediaTrack, String>() {
            @Override
            public void update(int index, MediaTrack mediaTrack, String value) {
                if (MediaActionBarCell.ACTION_REMOVE.equals(value)) {
                    if (Window.confirm(stringMessages.reallyRemoveMediaTrack(mediaTrack.title))) {
                        removeMediaTrack(mediaTrack);
                    }
                } 
            }
        });
        mediaTracksTable.addColumn(mediaActionColumn, stringMessages.delete());
        mediaTracksTable.setColumnWidth(mediaActionColumn, 5, Unit.PCT);

    }

    protected void removeMediaTrack(MediaTrack mediaTrack) {
        mediaService.deleteMediaTrack(mediaTrack, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError(t.toString());
            }

            @Override
            public void onSuccess(Void allMediaTracks) {
                loadMediaTracks();
            }
        });
    }

    private void addUrlMediaTrack() {
        Date defaultStartTime = new Date();
        MediaTrackDialog dialog = new MediaTrackDialog(defaultStartTime , stringMessages, new DialogCallback<MediaTrack>() {

            @Override
            public void cancel() {
                // no op
            }

            @Override
            public void ok(final MediaTrack mediaTrack) {
                mediaService.addMediaTrack(mediaTrack, new AsyncCallback<String>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(String dbId) {
                        mediaTrack.dbId = dbId;
                        loadMediaTracks();
                    }
                });

            }
        });
        dialog.show();
    }
    
    protected void addYoutubeMediaTrack() {
        MediaTrackYoutubeDialog dialog = new MediaTrackYoutubeDialog(stringMessages, new DialogCallback<MediaTrack>() {

            @Override
            public void cancel() {
                // no op
            }

            @Override
            public void ok(final MediaTrack mediaTrack) {
                mediaService.addMediaTrack(mediaTrack, new AsyncCallback<String>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                    }

                    @Override
                    public void onSuccess(String dbId) {
                        mediaTrack.dbId = dbId;
                        loadMediaTracks();
                    }
                });

            }
        });
        dialog.show();
    }

    public void onShow() {
        loadMediaTracks();
    }

}
