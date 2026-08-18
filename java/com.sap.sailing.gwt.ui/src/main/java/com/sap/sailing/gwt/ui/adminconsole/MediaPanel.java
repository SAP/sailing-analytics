package com.sap.sailing.gwt.ui.adminconsole;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.KEYUP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.domain.common.media.MediaUtil;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.multivideo.MultiURLChangeDialog;
import com.sap.sailing.gwt.ui.adminconsole.multivideo.MultiVideoDialog;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.NewMediaWithRaceSelectionDialog;
import com.sap.sailing.gwt.ui.client.media.TimeFormatUtil;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.util.NullSafeComparableComparator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

/**
 * Table inspired by http://gwt.google.com/samples/Showcase/Showcase.html#!CwCellTable
 * 
 * @author D047974
 * 
 */
public class MediaPanel extends FlowPanel implements FilterablePanelProvider<MediaTrackWithSecurityDTO> {
    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
    
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final LabeledAbstractFilterablePanel<MediaTrackWithSecurityDTO> filterableMediaTracks;
    private List<MediaTrackWithSecurityDTO> allMediaTracks;
    private final Presenter presenter;
    private final MediaServiceWriteAsync mediaServiceWrite;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final UserService userService;
    private FlushableCellTable<MediaTrackWithSecurityDTO> mediaTracksTable;
    private ListDataProvider<MediaTrackWithSecurityDTO> mediaTrackListDataProvider = new ListDataProvider<>();
    private Date latestDate;
    private RefreshableMultiSelectionModel<MediaTrackWithSecurityDTO> refreshableSelectionModel;
    private final FileStorageServiceConnectionTestObservable storageServiceAvailable;
    private final SelectionCheckboxColumn<MediaTrackWithSecurityDTO> checkColumn;

    public MediaPanel(Presenter presenter, StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.presenter = presenter;
        this.mediaServiceWrite = presenter.getMediaServiceWrite();
        this.stringMessages = stringMessages;
        this.errorReporter = presenter.getErrorReporter();
        this.storageServiceAvailable = new FileStorageServiceConnectionTestObservable(presenter.getSailingService());
        Label lblFilterRaces = new Label(stringMessages.filterMediaByName() + ":");
        lblFilterRaces.setWordWrap(false);
        this.filterableMediaTracks = new LabeledAbstractFilterablePanel<MediaTrackWithSecurityDTO>(lblFilterRaces,
                allMediaTracks, mediaTrackListDataProvider, stringMessages) {
            @Override
            public List<String> getSearchableStrings(MediaTrackWithSecurityDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.title);
                strings.add(t.url);
                if (t.startTime == null) {
                    GWT.log("startTime of media track " + t.title + " undefined");
                } else {
                    strings.add(t.startTime.toString());
                }
                return strings;
            }

            @Override
            public AbstractCellTable<MediaTrackWithSecurityDTO> getCellTable() {
                return mediaTracksTable;
            }
        };
        checkColumn = new SelectionCheckboxColumn<>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(),
                new EntityIdentityComparator<MediaTrackWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(MediaTrackWithSecurityDTO dto1, MediaTrackWithSecurityDTO dto2) {
                        return dto1.dbId.equals(dto2.dbId);
                    }
                    @Override
                    public int hashCode(MediaTrackWithSecurityDTO t) {
                        return t.dbId.hashCode();
                    }
                }, filterableMediaTracks.getAllListDataProvider());
        refreshableSelectionModel = checkColumn.getSelectionModel();
        AccessControlledButtonPanel buttonAndFilterPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.MEDIA_TRACK);
        add(buttonAndFilterPanel);
        allMediaTracks = new ArrayList<>();
        buttonAndFilterPanel.addUnsecuredAction(stringMessages.refresh(), new Command() {
            @Override
            public void execute() {
                presenter.getMediaTracksRefresher().reloadAndCallFillAll();
            }
        });
        buttonAndFilterPanel.addCreateAction(stringMessages.addMediaTrack(), new Command() {
            @Override
            public void execute() {
                addUrlMediaTrack();
            }
        });
        buttonAndFilterPanel.addCreateAction(stringMessages.multiVideoLinking(), new Command() {
            @Override
            public void execute() {
                new MultiVideoDialog(sailingServiceWrite, mediaServiceWrite, stringMessages, errorReporter, new Runnable() {
                    @Override
                    public void run() {
                        presenter.getMediaTracksRefresher().reloadAndCallFillAll();
                    }
                }).center();
            }
        });
        buttonAndFilterPanel.addUpdateAction(stringMessages.multiUrlChangeMediaTrack(),
                refreshableSelectionModel,
                new Command() {
            @Override
            public void execute() {
                final Set<MediaTrackWithSecurityDTO> selected = refreshableSelectionModel.getSelectedSet();
                if (selected.isEmpty()) {
                    Notification.notify(stringMessages.noSelection(), NotificationType.ERROR);
                } else {
                    new MultiURLChangeDialog(mediaServiceWrite, stringMessages, selected, errorReporter, new Runnable() {
                        @Override
                        public void run() {
                            presenter.getMediaTracksRefresher().reloadAndCallFillAll();
                        }
                    }).center();
                }
            }
        });
        buttonAndFilterPanel.addRemoveAction(stringMessages.remove(), refreshableSelectionModel,
                /* with confirmation */ true, new Command() {
            @Override
            public void execute() {
                final List<MediaTrackWithSecurityDTO> selected = new ArrayList<>(refreshableSelectionModel.getSelectedSet());
                for (final MediaTrackWithSecurityDTO track : selected) {
                    removeMediaTrack(track);
                }
            }
        });
        buttonAndFilterPanel.addUnsecuredWidget(lblFilterRaces);
        createMediaTracksTable(userService);
        filterableMediaTracks.getTextBox().ensureDebugId("MediaTracksFilterTextBox");
        filterableMediaTracks
                .setUpdatePermissionFilterForCheckbox(mediaTrack -> userService.hasPermission(mediaTrack, DefaultActions.UPDATE));
        buttonAndFilterPanel.addUnsecuredWidget(filterableMediaTracks);
    }

    private final Displayer<MediaTrackWithSecurityDTO> mediaTracksDisplayer = new Displayer<MediaTrackWithSecurityDTO>() {
        
        @Override
        public void fill(Iterable<MediaTrackWithSecurityDTO> result) {
            fillMediaTracks(result);
        }
    };

    public Displayer<MediaTrackWithSecurityDTO> getMediaTracksDisplayer() {
        return mediaTracksDisplayer;
    }

    public void fillMediaTracks(Iterable<MediaTrackWithSecurityDTO> allMediaTracks) {
        mediaTrackListDataProvider.getList().clear();
        Util.addAll(allMediaTracks, mediaTrackListDataProvider.getList());
        filterableMediaTracks.updateAll(mediaTrackListDataProvider.getList());
        mediaTrackListDataProvider.refresh();
    }

    private void createMediaTracksTable(final UserService userService) {
        mediaTracksTable = new FlushableCellTable<>(1000, tableResources);
        mediaTracksTable.setWidth("100%");
        final ListHandler<MediaTrackWithSecurityDTO> sortHandler = new ListHandler<>(mediaTrackListDataProvider.getList());
        mediaTracksTable.addColumnSortHandler(sortHandler);
        initTableColumns(sortHandler, userService);
        mediaTrackListDataProvider.addDataDisplay(mediaTracksTable);
        add(mediaTracksTable);
        allMediaTracks.clear();
        allMediaTracks.addAll(mediaTrackListDataProvider.getList());
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns(final ListHandler<MediaTrackWithSecurityDTO> sortHandler,
            final UserService userService) {
        final Header<Boolean> selectAllHeader = checkColumn.createHeader();
        mediaTracksTable.addColumn(checkColumn, selectAllHeader);
        mediaTracksTable.setColumnWidth(checkColumn, 40, Unit.PX);
        mediaTracksTable.setSelectionModel(refreshableSelectionModel, checkColumn.getSelectionManager());
        // db id
        Column<MediaTrackWithSecurityDTO, String> dbIdColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new TextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return mediaTrack.dbId;
            }
        };
        dbIdColumn.setSortable(true);
        sortHandler.setComparator(dbIdColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return mediaTrack1.dbId.compareTo(mediaTrack2.dbId);
            }
        });
        mediaTracksTable.addColumn(dbIdColumn, stringMessages.id());
        mediaTracksTable.setColumnWidth(dbIdColumn, 10, Unit.PCT);

        // media title
        Column<MediaTrackWithSecurityDTO, String> titleColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new EditTextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return mediaTrack.title;
            }
        };
        titleColumn.setSortable(true);
        sortHandler.setComparator(titleColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return mediaTrack1.title.compareTo(mediaTrack2.title);
            }
        });
        mediaTracksTable.addColumn(titleColumn, stringMessages.title());
        titleColumn.setFieldUpdater(new FieldUpdater<MediaTrackWithSecurityDTO, String>() {
            public void update(int index, MediaTrackWithSecurityDTO mediaTrack, String newTitle) {
                if (Util.equalStringsWithEmptyIsNull(newTitle, mediaTrack.title)) {
                    return;
                }
                String oldtitle = mediaTrack.title;
                // Called when the user changes the value.
                mediaTrack.title = newTitle;
                mediaServiceWrite.updateTitle(mediaTrack, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(t.toString());
                        mediaTrack.title = oldtitle;
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
        Column<MediaTrackWithSecurityDTO, String> urlColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new EditTextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return mediaTrack.url;
            }
        };
        urlColumn.setSortable(true);
        sortHandler.setComparator(urlColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return mediaTrack1.url.compareTo(mediaTrack2.url);
            }
        });
        mediaTracksTable.addColumn(urlColumn, stringMessages.url());
        urlColumn.setFieldUpdater(new FieldUpdater<MediaTrackWithSecurityDTO, String>() {
            public void update(int index, MediaTrackWithSecurityDTO mediaTrack, String newUrl) {
                // Called when the user changes the value.
                if (Util.equalStringsWithEmptyIsNull(newUrl, mediaTrack.url = newUrl)) {
                    return;
                }
                mediaTrack.url = newUrl;
                mediaServiceWrite.updateUrl(mediaTrack, new AsyncCallback<Void>() {

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

        // assingedRaces

        Column<MediaTrackWithSecurityDTO, String> assignedRacesColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new ClickableTextCell() {
            public void onEnterKeyDown(Context context, Element parent, String value, NativeEvent event,
                    ValueUpdater<String> valueUpdater) {
                String type = event.getType();
                int keyCode = event.getKeyCode();
                boolean enterPressed = KEYUP.equals(type) && keyCode == KeyCodes.KEY_ENTER;
                if (CLICK.equals(type) || enterPressed) {
                    openAssignedRacesDialog(context, parent, valueUpdater);
                }
            }
        }) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                if (mediaTrack.assignedRaces != null) {
                    return listAssignedRaces(mediaTrack);
                } else
                    return "";
            }

        };
        assignedRacesColumn.setSortable(true);
        sortHandler.setComparator(assignedRacesColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return (listAssignedRaces(mediaTrack1)).compareTo(listAssignedRaces(mediaTrack2));
            }
        });
        mediaTracksTable.addColumn(assignedRacesColumn, stringMessages.linkedRaces());
        assignedRacesColumn.setFieldUpdater(new FieldUpdater<MediaTrackWithSecurityDTO, String>() {
            public void update(int index, MediaTrackWithSecurityDTO mediaTrack, String newAssignedRace) {
                // Called when the user changes the value.
                if (newAssignedRace.trim().isEmpty()) {
                    mediaTrack.assignedRaces.clear();
                } else {
                    //no op
                }
                mediaServiceWrite.updateRace(mediaTrack, new AsyncCallback<Void>() {

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
        mediaTracksTable.setColumnWidth(assignedRacesColumn, 100, Unit.PCT);

        // start time
        Column<MediaTrackWithSecurityDTO, String> startTimeColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new EditTextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return mediaTrack.startTime == null ? "" : TimeFormatUtil.DATETIME_FORMAT.format(mediaTrack.startTime
                        .asDate());
            }
        };
        startTimeColumn.setSortable(true);
        sortHandler.setComparator(startTimeColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return MediaUtil.compareDatesAllowingNull(mediaTrack1.startTime, mediaTrack2.startTime);
            }
        });
        startTimeColumn.setFieldUpdater(new FieldUpdater<MediaTrackWithSecurityDTO, String>() {
            public void update(int index, MediaTrackWithSecurityDTO mediaTrack, String newStartTime) {
                // Called when the user changes the value.
                if (newStartTime == null || newStartTime.trim().isEmpty()) {
                    mediaTrack.startTime = null;
                } else {
                    try {
                        mediaTrack.startTime = new MillisecondsTimePoint(TimeFormatUtil.DATETIME_FORMAT
                                .parse(newStartTime.trim()));
                    } catch (IllegalArgumentException e) {
                        errorReporter.reportError(stringMessages.mediaDateFormatError(TimeFormatUtil.DATETIME_FORMAT
                                .toString()));
                    }
                }
                mediaServiceWrite.updateStartTime(mediaTrack, new AsyncCallback<Void>() {

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
        Column<MediaTrackWithSecurityDTO, String> durationColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new EditTextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return TimeFormatUtil.durationToHrsMinSec(mediaTrack.duration);
            }
        };
        durationColumn.setSortable(true);
        sortHandler.setComparator(durationColumn, new Comparator<MediaTrackWithSecurityDTO>() {
            final Comparator<Duration> durationComparator = new NullSafeComparableComparator<>(true);

            public int compare(MediaTrackWithSecurityDTO mediaTrack1, MediaTrackWithSecurityDTO mediaTrack2) {
                return durationComparator.compare(mediaTrack1.duration, mediaTrack2.duration);
            }
        });
        durationColumn.setFieldUpdater(new FieldUpdater<MediaTrackWithSecurityDTO, String>() {
            public void update(int index, MediaTrackWithSecurityDTO mediaTrack, String newDuration) {
                // Called when the user changes the value.
                if (newDuration == null || newDuration.trim().isEmpty()) {
                    mediaTrack.duration = null;
                } else {
                    try {
                        mediaTrack.duration = TimeFormatUtil.hrsMinSecToMilliSeconds(newDuration);
                    } catch (Exception e) {
                        errorReporter.reportError(stringMessages.mediaDateFormatError("Duration hh:mm:ss.xxx"));
                    }
                }
                mediaServiceWrite.updateDuration(mediaTrack, new AsyncCallback<Void>() {

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
        // media type
        Column<MediaTrackWithSecurityDTO, String> mimeTypeColumn = new Column<MediaTrackWithSecurityDTO, String>(
                new TextCell()) {
            @Override
            public String getValue(MediaTrackWithSecurityDTO mediaTrack) {
                return mediaTrack.mimeType == null ? "" : mediaTrack.mimeType.toString();
            }
        };
        mimeTypeColumn.setSortable(true);
        mediaTracksTable.addColumn(mimeTypeColumn, stringMessages.mimeType());
        mediaTracksTable.setColumnWidth(mimeTypeColumn, 100, Unit.PCT);
        sortHandler.setComparator(mimeTypeColumn, (mt1, mt2)->
            (mt1.mimeType == null ? "" : mt1.mimeType.toString()).compareTo(
                    mt2.mimeType == null ? "" : mt2.mimeType.toString()));

        SecuredDTOOwnerColumn.configureOwnerColumns(mediaTracksTable, sortHandler, stringMessages);

        final HasPermissions type = SecuredDomainType.MEDIA_TRACK;

        final AccessControlledActionsColumn<MediaTrackWithSecurityDTO, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionsColumn.addAction(ACTION_DELETE, DELETE, mediaTrack -> {
            if (Window.confirm(stringMessages.reallyRemoveMediaTrack(mediaTrack.title))) {
                removeMediaTrack(mediaTrack);
            }
        });
        final EditOwnershipDialog.DialogConfig<MediaTrackWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, mediaTrack -> mediaTrackListDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<MediaTrackWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, mediaTrack -> mediaTrack.getAccessControlList(),
                stringMessages);
        actionsColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                mediaTrack -> configACL.openDialog(mediaTrack));
        mediaTracksTable.addColumn(actionsColumn, stringMessages.actions());
    }

    protected void removeMediaTrack(MediaTrack mediaTrack) {
        mediaServiceWrite.deleteMediaTrack(mediaTrack, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError(t.toString());
            }

            @Override
            public void onSuccess(Void deleteMediaTrack) {
                presenter.getMediaTracksRefresher().reloadAndCallFillAll();
            }
        });
    }

    private void addUrlMediaTrack() {
        NewMediaWithRaceSelectionDialog dialog = new NewMediaWithRaceSelectionDialog(mediaServiceWrite,
                getDefaultStartTime(), stringMessages, presenter,
                storageServiceAvailable, new DialogCallback<MediaTrack>() {
                    @Override
                    public void cancel() {
                        // no op
                    }

                    @Override
                    public void ok(final MediaTrack mediaTrack) {
                        mediaServiceWrite.addMediaTrack(mediaTrack, new AsyncCallback<MediaTrackWithSecurityDTO>() {
                            @Override
                            public void onFailure(Throwable t) {
                                errorReporter.reportError(t.toString());
                            }

                            @Override
                            public void onSuccess(MediaTrackWithSecurityDTO mediaTrackWithSecurity) {
                                mediaTrack.dbId = mediaTrackWithSecurity.dbId;
                                presenter.getMediaTracksRefresher().add(mediaTrackWithSecurity);
                                presenter.getMediaTracksRefresher().callAllFill();

                            }
                        });
                    }
                });
        dialog.show();
    }
    
    private TimePoint getDefaultStartTime() {
        if (getLatestDate() != null) {
            return new MillisecondsTimePoint(latestDate); 
        } else {
            return MillisecondsTimePoint.now();
        }
    }

    private Date getLatestDate() {
        sailingServiceWrite.getRegattas(new MarkedAsyncCallback<List<RegattaDTO>>(new AsyncCallback<List<RegattaDTO>>() {
            @Override
            public void onSuccess(List<RegattaDTO> result) {
               latestDate = getDateFromLatestRegatta(result); 
            }

            private Date getDateFromLatestRegatta(List<RegattaDTO> result) {
                RegattaDTO latestRegatta = null;
                for (RegattaDTO regatta : result) {
                    if(regatta.getStartDate()!=null){
                        if(latestRegatta == null){
                            latestRegatta = regatta;
                        }else if(regatta.getStartDate().after(latestRegatta.getStartDate())) {
                            latestRegatta = regatta;
                        }
                    }
                }
                return latestRegatta == null ? null : latestRegatta.getStartDate();
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        }));
        return latestDate;
    }



    private String listAssignedRaces(MediaTrackWithSecurityDTO mediaTrack) {
        final String result;
        if (mediaTrack.assignedRaces.size() > 1) {
            result = String.valueOf(mediaTrack.assignedRaces.size());
        } else {
            String value = "";
            for (RegattaAndRaceIdentifier assignedRace : mediaTrack.assignedRaces) {
                value += assignedRace.getRegattaName() + " " + assignedRace.getRaceName() + ", ";
            }
            if (value.length() > 1) {
                result = value.substring(0, value.length() - 2);
            } else {
                result = value;
            }
        }
        return result;
    }

    public void onShow() {
        presenter.getMediaTracksRefresher().callFillAndReloadInitially(getMediaTracksDisplayer());
    }

    public void openAssignedRacesDialog(final Context context, final Element parent,
            final ValueUpdater<String> valueUpdater) {
        final MediaTrackWithSecurityDTO mediaTrack = (MediaTrackWithSecurityDTO) context.getKey();
        final AssignRacesToMediaDialog dialog = new AssignRacesToMediaDialog(presenter, mediaTrack,
                stringMessages, null, new DialogCallback<Set<RegattaAndRaceIdentifier>>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(Set<RegattaAndRaceIdentifier> assignedRaces) {
                        if (assignedRaces.size() >= 0) {
                            String value = "";
                            for (RegattaAndRaceIdentifier assignedRace : assignedRaces) {
                                value = value.concat(assignedRace.getRegattaName() + "    "
                                        + assignedRace.getRaceName() + ",");
                            }
                            mediaTrack.assignedRaces.clear();
                            mediaTrack.assignedRaces.addAll(assignedRaces);
                            valueUpdater.update(value);
                        }

                    }
                });
        presenter.getRegattasRefresher().addDisplayerAndCallFillOnInit(dialog.getRegattasDisplayer());
        dialog.ensureDebugId("AssignedRacesDialog");
        dialog.show();
    }

    @Override
    public AbstractFilterablePanel<MediaTrackWithSecurityDTO> getFilterablePanel() {
        return filterableMediaTracks;
    }
}