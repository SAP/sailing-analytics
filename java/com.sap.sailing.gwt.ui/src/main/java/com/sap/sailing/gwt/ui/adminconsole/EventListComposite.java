package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sailing.domain.common.security.SecuredDomainType.EVENT;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupDialog.LeaderboardGroupDescriptor;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.adminconsole.places.leaderboards.LeaderboardGroupsPlace;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.MultipleLinkCell;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AbstractFilterablePlace;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

/**
 * A composite showing the list of all sailing events  
 * @author Frank Mittag (C5163974)
 */
public class EventListComposite extends Composite {
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final CellTable<EventDTO> eventTable;
    private final RefreshableMultiSelectionModel<EventDTO> refreshableEventSelectionModel;
    private final ListDataProvider<EventDTO> eventListDataProvider;
    private final List<EventDTO> allEvents;
    private final Label noEventsLabel;
    protected final LabeledAbstractFilterablePanel<EventDTO> filterTextbox;
    private final Displayer<LeaderboardGroupDTO> leaderboardGroupsDisplayer;
    private final Displayer<EventDTO> eventsDisplayer;
    private Iterable<LeaderboardGroupDTO> availableLeaderboardGroups;
    
    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a target=\"_blank\" href=\"{0}\">{1}</a>")
        SafeHtml cell(SafeUri safeUri, String displayName);
    }

    private static AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    private final AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
    private final Presenter presenter;
    private final PlaceController placeController;
    
    public EventListComposite(final Presenter presenter, final PlaceController placeController,
            final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.userService = presenter.getUserService();
        this.stringMessages = stringMessages;
        this.errorReporter = presenter.getErrorReporter();
        this.presenter = presenter;
        this.placeController = placeController;
        this.availableLeaderboardGroups = Collections.emptyList();
        this.allEvents = new ArrayList<EventDTO>();
        final VerticalPanel panel = new VerticalPanel();
        final AccessControlledButtonPanel buttonPanel = new AccessControlledButtonPanel(userService, EVENT);
        panel.add(buttonPanel);
        leaderboardGroupsDisplayer = new Displayer<LeaderboardGroupDTO>() {
            @Override
            public void fill(Iterable<LeaderboardGroupDTO> result) {
                fillLeaderboardGroups(result);
            }
        };
        eventsDisplayer = new Displayer<EventDTO>() {
            @Override
            public void fill(Iterable<EventDTO> result) {
                fillEvents(result);
            }
        };
        eventListDataProvider = new ListDataProvider<EventDTO>();
        filterTextbox = new LabeledAbstractFilterablePanel<EventDTO>(new Label(stringMessages.filterEventsByName()),
                allEvents, eventListDataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(EventDTO t) {
                List<String> result = new ArrayList<String>();
                result.add(t.getName());
                result.add(t.getVenue().getName());
                for (CourseAreaDTO c : t.getVenue().getCourseAreas()) {
                    result.add(c.getName());
                }
                for (LeaderboardGroupDTO lg : t.getLeaderboardGroups()) {
                    result.add(lg.getName());
                }
                return result;
            }

            @Override
            public AbstractCellTable<EventDTO> getCellTable() {
                return eventTable;
            }
        };
        eventTable = createEventTable(userService.getCurrentUser());
        eventTable.ensureDebugId("EventsCellTable");
        @SuppressWarnings("unchecked")
        final RefreshableMultiSelectionModel<EventDTO> selectionModel = (RefreshableMultiSelectionModel<EventDTO>) eventTable.getSelectionModel();
        refreshableEventSelectionModel = selectionModel;
        eventTable.setVisible(false);
        final Button refresh = buttonPanel.addUnsecuredAction(stringMessages.refresh(), () -> presenter.getEventsRefresher().reloadAndCallFillAll());
        refresh.ensureDebugId("RefreshEventsButton");
        final Button create = buttonPanel.addCreateAction(stringMessages.actionAddEvent(), this::openCreateEventDialog);
        create.ensureDebugId("CreateEventButton");
        final Button remove = buttonPanel.addRemoveAction(stringMessages.remove(), refreshableEventSelectionModel, true,
                () -> removeEvents(refreshableEventSelectionModel.getSelectedSet()));
        remove.ensureDebugId("RemoveEventsButton");
        this.refreshableEventSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                final Set<EventDTO> selectedEvents = refreshableEventSelectionModel.getSelectedSet();
                boolean canDeleteAll = true;
                for (EventDTO eventDTO : selectedEvents) {
                    if (!userService.hasPermission(eventDTO, DefaultActions.DELETE)) {
                        canDeleteAll = false;
                    }
                }
                remove.setEnabled(!selectedEvents.isEmpty() && canDeleteAll);

            }
        });
        buttonPanel.addUnsecuredWidget(new HelpButton(HelpButtonResources.INSTANCE,
                stringMessages.videoGuide(), "https://sapsailing-documentation.s3-eu-west-1.amazonaws.com/adminconsole/CreatingYourFirstEvent.mp4"));
        panel.add(filterTextbox);
        panel.add(eventTable);
        noEventsLabel = new Label(stringMessages.noEventsYet());
        noEventsLabel.ensureDebugId("NoRegattasLabel");
        noEventsLabel.setWordWrap(false);
        panel.add(noEventsLabel);
        initWidget(panel);
        filterTextbox.setUpdatePermissionFilterForCheckbox(event -> userService.hasPermission(event, DefaultActions.UPDATE));
    }

    public Displayer<EventDTO> getEventsDisplayer() {
        return eventsDisplayer;
    }
    
    public Displayer<LeaderboardGroupDTO> getLeaderboardGroupsDisplayer() {
        return leaderboardGroupsDisplayer;
    }

    private CellTable<EventDTO> createEventTable(UserDTO user) {
        FlushableCellTable<EventDTO> table = new FlushableCellTable<EventDTO>(/* pageSize */10000, tableRes);
        eventListDataProvider.addDataDisplay(table);
        table.setWidth("100%");
        SelectionCheckboxColumn<EventDTO> eventSelectionCheckboxColumn = new SelectionCheckboxColumn<EventDTO>(
                tableRes.cellTableStyle().cellTableCheckboxSelected(),
                tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<EventDTO>() {
                    @Override
                    public boolean representSameEntity(EventDTO dto1, EventDTO dto2) {
                        return dto1.id.equals(dto2.id);
                    }
                    @Override
                    public int hashCode(EventDTO t) {
                        return t.id.hashCode();
                    }
                }, filterTextbox.getAllListDataProvider(),table);
        AnchorCell anchorCell = new AnchorCell();
        ListHandler<EventDTO> listHandler = new ListHandler<EventDTO>(eventListDataProvider.getList());
        final TextColumn<EventDTO> eventUUidColumn = new AbstractSortableTextColumn<EventDTO>(
                event -> event.getId() == null ? "<null>" : event.getId().toString(), listHandler);
        Column<EventDTO, SafeHtml> eventNameColumn = new Column<EventDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(EventDTO event) {
                String link = "";
                if(event != null && event.id != null){
                    link = EntryPointLinkFactory.createEventPlaceLink(event.id.toString(), new HashMap<String, String>());
                }
                return ANCHORTEMPLATE.cell(UriUtils.fromString(link), event.getName());
            }
        };
        TextColumn<EventDTO> venueNameColumn = new TextColumn<EventDTO>() {
            @Override
            public String getValue(EventDTO event) {
                return event.getVenue() != null ? event.getVenue().getName() : "";
            }
        };
        TextColumn<EventDTO> startEndDateColumn = new TextColumn<EventDTO>() {
            @Override
            public String getValue(EventDTO event) {
                return DateAndTimeFormatterUtil.formatDateRange(event.startDate, event.endDate);
            }
        };
        TextColumn<EventDTO> isPublicColumn = new TextColumn<EventDTO>() {
            @Override
            public String getValue(EventDTO event) {
                return event.isPublic ? stringMessages.yes() : stringMessages.no();
            }
        };
        SafeHtmlCell courseAreasCell = new SafeHtmlCell();
        Column<EventDTO, SafeHtml> courseAreasColumn = new Column<EventDTO, SafeHtml>(courseAreasCell) {
            @Override
            public SafeHtml getValue(EventDTO event) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                int courseAreasCount = event.getVenue().getCourseAreas().size();
                int i = 1;
                for (CourseAreaDTO courseArea : event.getVenue().getCourseAreas()) {
                    builder.appendEscaped(courseArea.getName() == null ? "null" : courseArea.getName());
                    if (i < courseAreasCount) {
                        builder.appendHtmlConstant(",&nbsp;");
                        // not more than  4 course areas per line
                        if (i % 4 == 0) {
                            builder.appendHtmlConstant("<br>");
                        }
                    }
                    i++;
                }
                return builder.toSafeHtml();
            }
        };
        MultipleLinkCell leaderboardGroupsCell = new MultipleLinkCell(true);
        Column<EventDTO, List<MultipleLinkCell.CellLink>> leaderboardGroupsColumn = new Column<EventDTO, List<MultipleLinkCell.CellLink>>(
                leaderboardGroupsCell) {
            @Override
            public List<MultipleLinkCell.CellLink> getValue(EventDTO event) {
                List<MultipleLinkCell.CellLink> links = new ArrayList<>();
                for (LeaderboardGroupDTO lg : event.getLeaderboardGroups()) {
                    final String leaderboardGroupId = String.valueOf(lg.getId());
                    MultipleLinkCell.CellLink cellLink = new MultipleLinkCell.CellLink(leaderboardGroupId, leaderboardGroupId, lg.getName());
                    links.add(cellLink);
                }
                return links;
            }
        };
        leaderboardGroupsCell.setOnLinkClickHandler(new ValueUpdater<String>() {
            @Override
            public void update(String value) {
                Map<String, String> params = new HashMap<>();
                params.put(AbstractFilterablePlace.FILTER_KEY, URL.encodeQueryString(value));
                params.put(AbstractFilterablePlace.SELECT_EXACT_KEY,  URL.encodeQueryString(value));
                placeController.goTo(new LeaderboardGroupsPlace(params));
            }
        });
        TextColumn<EventDTO> imagesColumn = new TextColumn<EventDTO>() {
            @Override
            public String getValue(EventDTO event) {
                String result = "";
                int imageCount = Util.size(event.getImages());
                if (imageCount > 0) {
                    result = stringMessages.imagesWithCount(imageCount);
                }
                return result;
            }
        };
        TextColumn<EventDTO> videosColumn = new TextColumn<EventDTO>() {
            @Override
            public String getValue(EventDTO event) {
                String result = "";
                int videoCount = Util.size(event.getVideos());
                if (videoCount > 0) {
                    result = stringMessages.videosWithCount(videoCount);
                }
                return result;
            }
        };
        final SecuredDTOOwnerColumn<EventDTO> groupColumn = SecuredDTOOwnerColumn.getGroupOwnerColumn();
        final SecuredDTOOwnerColumn<EventDTO> userColumn = SecuredDTOOwnerColumn.getUserOwnerColumn();
        final AccessControlledActionsColumn<EventDTO, EventConfigImagesBarCell> actionsColumn = create(
                new EventConfigImagesBarCell(stringMessages), userService);
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_UPDATE, UPDATE, this::openEditEventDialog);
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_DELETE, DELETE, event -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveEvent(event.getName()))) {
                removeEvent(event);
            }
        });
        final DialogConfig<EventDTO> config = EditOwnershipDialog.create(userService.getUserManagementWriteService(), EVENT,
                event -> presenter.getEventsRefresher().reloadAndCallFillAll(), stringMessages);
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);
        final EditACLDialog.DialogConfig<EventDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), EVENT, event -> presenter.getEventsRefresher().reloadAndCallFillAll(), stringMessages);
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        final MigrateGroupOwnershipDialog.DialogConfig<EventDTO> migrateDialogConfig = MigrateGroupOwnershipDialog
                .create(userService.getUserManagementService(), (event, dto) -> {
                    sailingServiceWrite.updateGroupOwnerForEventHierarchy(event.id, dto, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorUpdatingOwnership(event.getName()));
                        }

                        @Override
                        public void onSuccess(Void result) {
                            presenter.getEventsRefresher().reloadAndCallFillAll();
                        }
                    });
                });
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_MIGRATE_GROUP_OWNERSHIP_HIERARCHY, CHANGE_OWNERSHIP,
                migrateDialogConfig::openDialog);
        eventNameColumn.setSortable(true);
        venueNameColumn.setSortable(true);
        isPublicColumn.setSortable(true);
        startEndDateColumn.setSortable(true);
        courseAreasColumn.setSortable(true);
        leaderboardGroupsColumn.setSortable(true);
        groupColumn.setSortable(true);
        userColumn.setSortable(true);
        configureTableColumnSortHandler(listHandler, eventSelectionCheckboxColumn,
                eventNameColumn, venueNameColumn, startEndDateColumn, isPublicColumn, courseAreasColumn,
                leaderboardGroupsColumn, groupColumn, userColumn);
        table.addColumn(eventSelectionCheckboxColumn, eventSelectionCheckboxColumn.getHeader());
        table.addColumn(eventNameColumn, stringMessages.event());
        table.addColumn(venueNameColumn, stringMessages.venue());
        table.addColumn(startEndDateColumn, stringMessages.from() + "/" + stringMessages.to());
        table.addColumn(isPublicColumn, stringMessages.isListedOnHomepage());
        table.addColumn(courseAreasColumn, stringMessages.courseAreas());
        table.addColumn(leaderboardGroupsColumn, stringMessages.leaderboardGroups());
        table.addColumn(imagesColumn, stringMessages.images());
        table.addColumn(videosColumn, stringMessages.videos());
        table.addColumn(groupColumn, stringMessages.group());
        table.addColumn(userColumn, stringMessages.user());
        table.addColumn(eventUUidColumn, stringMessages.id());
        table.addColumn(actionsColumn, stringMessages.actions());
        table.setSelectionModel(eventSelectionCheckboxColumn.getSelectionModel(), eventSelectionCheckboxColumn.getSelectionManager());
        table.addColumnSortHandler(listHandler);
        table.getColumnSortList().push(startEndDateColumn);
        return table;
    }
    
    private void configureTableColumnSortHandler(ListHandler<EventDTO> columnSortHandler, SelectionCheckboxColumn<EventDTO> eventSelectionCheckboxColumn,
            Column<EventDTO, SafeHtml> eventNameColumn, TextColumn<EventDTO> venueNameColumn,
            TextColumn<EventDTO> startEndDateColumn, TextColumn<EventDTO> isPublicColumn,
            Column<EventDTO, SafeHtml> courseAreasColumn, Column<EventDTO, List<MultipleLinkCell.CellLink>> leaderboardGroupsColumn,
            SecuredDTOOwnerColumn<EventDTO> groupColumn,
            SecuredDTOOwnerColumn<EventDTO> userColumn) {
        columnSortHandler.setComparator(eventSelectionCheckboxColumn, eventSelectionCheckboxColumn.getComparator());
        columnSortHandler.setComparator(eventNameColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                return new NaturalComparator().compare(e1.getName(), e2.getName());
            }
        });
        columnSortHandler.setComparator(venueNameColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                return new NaturalComparator().compare(e1.getVenue().getName(), e2.getVenue().getName());
            }
        });
        columnSortHandler.setComparator(startEndDateColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                int result;
                if(e1.startDate != null && e2.startDate != null) {
                    result = e2.startDate.compareTo(e1.startDate);
                } else if(e1.startDate == null && e2.startDate != null) {
                    result = 1;
                } else if(e1.startDate != null && e2.startDate == null) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        });
        columnSortHandler.setComparator(isPublicColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                return e1.isPublic == e2.isPublic ? 0 : e1.isPublic ? 1 : -1;
            }
        });
        columnSortHandler.setComparator(courseAreasColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                return e1.getVenue().getCourseAreas().toString().compareTo(e2.getVenue().getCourseAreas().toString());
            }
        });
        columnSortHandler.setComparator(leaderboardGroupsColumn, new Comparator<EventDTO>() {
            @Override
            public int compare(EventDTO e1, EventDTO e2) {
                return e1.getLeaderboardGroups().toString().compareTo(e2.getLeaderboardGroups().toString());
            }
        });
        columnSortHandler.setComparator(groupColumn, groupColumn.getComparator());
        columnSortHandler.setComparator(userColumn, userColumn.getComparator());
    }

    private void removeEvents(Collection<EventDTO> events) {
        if (!events.isEmpty()) {
            Collection<UUID> eventIds = new HashSet<UUID>();
            for (EventDTO event : events) {
                eventIds.add(event.id);
            }
            sailingServiceWrite.removeEvents(eventIds, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error trying to remove the events:" + caught.getMessage());
                }
                @Override
                public void onSuccess(Void result) {
                    for (EventDTO event : events) {
                        presenter.getEventsRefresher().remove(event);
                    }
                    presenter.getEventsRefresher().callAllFill();
                }
            });
        }
    }

    private void removeEvent(final EventDTO event) {
        sailingServiceWrite.removeEvent(event.id, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to remove event " + event.getName() + ": " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                presenter.getEventsRefresher().remove(event);
                presenter.getEventsRefresher().callAllFill();
            }
        });
    }

    private void openCreateEventDialog() {
        List<EventDTO> existingEvents = new ArrayList<EventDTO>(eventListDataProvider.getList());
        final List<LeaderboardGroupDTO> existingLeaderboardGroups = new ArrayList<LeaderboardGroupDTO>();
        Util.addAll(availableLeaderboardGroups, existingLeaderboardGroups);
        EventCreateDialog dialog = new EventCreateDialog(Collections.unmodifiableCollection(existingEvents), existingLeaderboardGroups,
                sailingServiceWrite, stringMessages, new DialogCallback<EventDTO>() {
            @Override
            public void cancel() {
            }

            @Override
            public void ok(final EventDTO newEvent) {
                createNewEvent(newEvent, existingLeaderboardGroups);
            }
        });
        dialog.ensureDebugId("EventCreateDialog");
        dialog.show();
    }
    
    private void openCreateDefaultRegattaDialog(final EventDTO createdEvent) {
        CreateDefaultRegattaDialog dialog = new CreateDefaultRegattaDialog(sailingServiceWrite, stringMessages, errorReporter, new DialogCallback<Void>() {
            @Override
            public void cancel() {
            }

            @Override
            public void ok(Void editedObject) {
                sailingServiceWrite.getRegattas(new AsyncCallback<List<RegattaDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        sailingServiceWrite.getEvents(new AsyncCallback<List<EventDTO>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                openCreateRegattaDialog(Collections.<RegattaDTO>emptyList(), Collections.<EventDTO>emptyList(), createdEvent);
                            }

                            @Override
                            public void onSuccess(List<EventDTO> result) {
                                openCreateRegattaDialog(Collections.<RegattaDTO>emptyList(), Collections.unmodifiableList(result), createdEvent);
                            }
                        });

                    }

                    @Override
                    public void onSuccess(final List<RegattaDTO> existingRegattas) {
                        sailingServiceWrite.getEvents(new AsyncCallback<List<EventDTO>>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                openCreateRegattaDialog(existingRegattas, Collections.<EventDTO>emptyList(), createdEvent);
                            }

                            @Override
                            public void onSuccess(List<EventDTO> result) {
                                openCreateRegattaDialog(existingRegattas, Collections.unmodifiableList(result), createdEvent);
                            }
                        });
                    }
                });
            }
        });
        dialog.ensureDebugId("CreateDefaultRegattaDialog");
        dialog.show();
    }
    
    private void openCreateRegattaDialog(List<RegattaDTO> existingRegattas, List<EventDTO> existingEvents,
            EventDTO createdEvent) {
        RegattaWithSeriesAndFleetsCreateDialog dialog = new RegattaWithSeriesAndFleetsCreateDialog(existingRegattas,
                existingEvents, createdEvent, sailingServiceWrite, userService,
                stringMessages, new CreateRegattaCallback(stringMessages, presenter, existingEvents));
        dialog.ensureDebugId("RegattaCreateDialog");
        dialog.show();
    }
    
    /**
     * @param newEvent the new event as created by the server, already including a valid {@link EventBaseDTO#id} value.
     */
    private void openLeaderboardGroupCreationDialog(final List<LeaderboardGroupDTO> existingLeaderboardGroups, final EventDTO newEvent) {
        LeaderboardGroupCreateDialog leaderboardGroupCreateDialog = new LeaderboardGroupCreateDialog(existingLeaderboardGroups, stringMessages, new DialogCallback<LeaderboardGroupDialog.LeaderboardGroupDescriptor>() {
            @Override
            public void ok(final LeaderboardGroupDescriptor newGroup) {
                        sailingServiceWrite.createLeaderboardGroup(newGroup.getName(), newGroup.getDescription(),
                        newGroup.getDisplayName(), newGroup.isDisplayLeaderboardsInReverseOrder(),
                        newGroup.getOverallLeaderboardDiscardThresholds(), newGroup.getOverallLeaderboardScoringSchemeType(), new MarkedAsyncCallback<LeaderboardGroupDTO>(
                                new AsyncCallback<LeaderboardGroupDTO>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError(stringMessages.errorCreatingLeaderboardGroup(newGroup.getName())
                                                + ": " + t.getMessage());
                                    }
                                    @Override
                                    public void onSuccess(LeaderboardGroupDTO newGroup) {
                                        newEvent.addLeaderboardGroup(newGroup);
                                        // fillEvents() will have replaced newEvent in allEvents by a new copy coming from the server which
                                        // doesn't know about the new leaderboard group yet. An updateEvent call will link the leaderboard group
                                        // to the event on the server
                                        EventDTO matchingEvent = null;
                                        for (EventDTO event : allEvents) {
                                            if (event.id.equals(newEvent.id)) {
                                                matchingEvent = event;
                                            }
                                        }
                                        if (matchingEvent != null) {
                                            updateEvent(matchingEvent, newEvent);
                                        } else {
                                            errorReporter.reportError("Could not find the event with name "+newEvent.getName()+" to which the leaderboardgroup should be added");
                                        }
                                        presenter.getLeaderboardGroupsRefresher().add(newGroup);
                                        presenter.getLeaderboardGroupsRefresher().callAllFill();
                                        openCreateDefaultRegattaDialog(newEvent);
                                    }
                                }));
            }

            @Override
            public void cancel() {
            }
        });
        leaderboardGroupCreateDialog.setFieldsBasedOnEventName(newEvent.getName(), newEvent.getDescription());
        leaderboardGroupCreateDialog.ensureDebugId("LeaderboardGroupCreateDialog");
        leaderboardGroupCreateDialog.show();
    }

    private void openEditEventDialog(final EventDTO selectedEvent) {
        List<EventDTO> existingEvents = new ArrayList<EventDTO>(eventListDataProvider.getList());
        existingEvents.remove(selectedEvent);
        List<LeaderboardGroupDTO> existingLeaderboardGroups = new ArrayList<LeaderboardGroupDTO>();
        Util.addAll(availableLeaderboardGroups, existingLeaderboardGroups);
        EventEditDialog dialog = new EventEditDialog(selectedEvent, Collections.unmodifiableCollection(existingEvents),  
                existingLeaderboardGroups, sailingServiceWrite, stringMessages,
                new DialogCallback<EventDTO>() {
            @Override
            public void cancel() {
            }

            @Override
            public void ok(EventDTO updatedEvent) {
                updateEvent(selectedEvent, updatedEvent);
            }
        }, presenter.getMediaServiceWrite());
        dialog.show();
    }

    private void updateEvent(final EventDTO oldEvent, final EventDTO updatedEvent) {
        Pair<List<CourseAreaDTO>, List<CourseAreaDTO>> courseAreasToAddAndRemove = getCourseAreasToAdd(oldEvent, updatedEvent);
        final List<CourseAreaDTO> courseAreasToAdd = courseAreasToAddAndRemove.getA();
        final List<CourseAreaDTO> courseAreasToRemove = courseAreasToAddAndRemove.getB();
        final List<UUID> updatedEventLeaderboardGroupIds = updatedEvent.getLeaderboardGroupIds();
        sailingServiceWrite.updateEvent(oldEvent.id, oldEvent.getName(), updatedEvent.getDescription(),
                updatedEvent.startDate, updatedEvent.endDate, updatedEvent.getVenue(), updatedEvent.isPublic,
                updatedEventLeaderboardGroupIds, updatedEvent.getOfficialWebsiteURL(), updatedEvent.getBaseURL(),
                updatedEvent.getSailorsInfoWebsiteURLs(), updatedEvent.getImages(), updatedEvent.getVideos(),
                updatedEvent.getWindFinderReviewedSpotsCollectionIds(), new AsyncCallback<EventDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(
                                "Error trying to update sailing event" + oldEvent.getName() + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(EventDTO result) {
                        sailingServiceWrite.createCourseAreas(oldEvent.id, courseAreasToAdd,
                                new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable t) {
                                        errorReporter.reportError("Error trying to add course area to sailing event "
                                                + oldEvent.getName() + ": " + t.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        final UUID[] idsOfCourseAreasToRemove = new UUID[courseAreasToRemove.size()];
                                        int j = 0;
                                        for (CourseAreaDTO courseAreaToRemove : courseAreasToRemove) {
                                            idsOfCourseAreasToRemove[j++] = courseAreaToRemove.getId();
                                        }
                                        sailingServiceWrite.removeCourseAreas(oldEvent.id, idsOfCourseAreasToRemove,
                                                new AsyncCallback<Void>() {
                                                    @Override
                                                    public void onFailure(Throwable t) {
                                                        errorReporter.reportError(
                                                                "Error trying to remove course area from sailing event "
                                                                        + oldEvent.getName() + ": " + t.getMessage());
                                                    }

                                                    @Override
                                                    public void onSuccess(Void result) {
                                                        presenter.getEventsRefresher().reloadAndCallFillAll();
                                                        if (!oldEvent.getName().equals(updatedEvent.getName())) {
                                                            sailingServiceWrite.renameEvent(oldEvent.id,
                                                                    updatedEvent.getName(), new AsyncCallback<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void result) {
                                                                        }

                                                                        @Override
                                                                        public void onFailure(Throwable t) {
                                                                            errorReporter.reportError(
                                                                                    "Error trying to rename sailing event "
                                                                                            + oldEvent.getName() + ": "
                                                                                            + t.getMessage());
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private Pair<List<CourseAreaDTO>, List<CourseAreaDTO>> getCourseAreasToAdd(final EventDTO oldEvent, final EventDTO updatedEvent) {
        List<CourseAreaDTO> courseAreasToAdd = new ArrayList<CourseAreaDTO>(updatedEvent.getVenue().getCourseAreas());
        courseAreasToAdd.removeAll(oldEvent.getVenue().getCourseAreas());
        List<CourseAreaDTO> courseAreasToRemove = new ArrayList<CourseAreaDTO>(oldEvent.getVenue().getCourseAreas());
        courseAreasToRemove.removeAll(updatedEvent.getVenue().getCourseAreas());
        return new Pair<List<CourseAreaDTO>, List<CourseAreaDTO>>(courseAreasToAdd, courseAreasToRemove);
    }

    private void createNewEvent(final EventDTO newEvent, final List<LeaderboardGroupDTO> existingLeaderboardGroups) {
        sailingServiceWrite.createEvent(newEvent.getName(), newEvent.getDescription(), newEvent.startDate, newEvent.endDate,
                newEvent.getVenue().getName(), newEvent.isPublic, newEvent.getVenue().getCourseAreas(), newEvent.getOfficialWebsiteURL(), newEvent.getBaseURL(),
                newEvent.getSailorsInfoWebsiteURLs(), newEvent.getImages(), newEvent.getVideos(), newEvent.getLeaderboardGroupIds(),
                new AsyncCallback<EventDTO>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to create new event " + newEvent.getName() + ": " + t.getMessage());
            }

            @Override
            public void onSuccess(final EventDTO newEvent) {
                presenter.getEventsRefresher().add(newEvent);
                presenter.getEventsRefresher().callAllFill();
                if (newEvent.getLeaderboardGroups().isEmpty()) {
                    // show simple Dialog
                    DataEntryDialog<Void> dialog = new CreateDefaultLeaderboardGroupDialog(
                            sailingServiceWrite, stringMessages, errorReporter, new DialogCallback<Void>() {
                        @Override
                        public void ok(Void editedObject) {
                            openLeaderboardGroupCreationDialog(existingLeaderboardGroups, newEvent);
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                    dialog.ensureDebugId("CreateDefaultLeaderboardGroupConfirmDialog");
                    dialog.show();
                } else {
                    openCreateDefaultRegattaDialog(newEvent);
                }
            }
        });
    }

    public void fillLeaderboardGroups(Iterable<LeaderboardGroupDTO> leaderboardGroups) {
        availableLeaderboardGroups = Util.filter(leaderboardGroups, lg->userService.hasPermission(lg, DefaultActions.UPDATE));
    }

    public void fillEvents(Iterable<EventDTO> events) {
        if (events.iterator().hasNext()) {
            eventTable.setVisible(true);
            noEventsLabel.setVisible(false);
        } else {
            eventTable.setVisible(false);
            noEventsLabel.setVisible(true);
        }
        allEvents.clear();
        events.forEach(allEvents::add);
        filterTextbox.updateAll(allEvents);
        eventTable.redraw();
    }

    public List<EventDTO> getAllEvents() {
        return allEvents;
    }
    
    public RefreshableMultiSelectionModel<EventDTO> getRefreshableMultiSelectionModel() {
        return refreshableEventSelectionModel;
    }
}
