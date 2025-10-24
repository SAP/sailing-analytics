package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.panels.CustomizableFilterablePanel;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public abstract class AbstractTrackedRacesListComposite extends AbstractCompositeComponent<TrackedRacesSettings> {

    protected final long DEFAULT_LIVE_DELAY_IN_MILLISECONDS = 5000;

    private final boolean multiSelection;

    protected RefreshableSelectionModel<RaceDTO> refreshableSelectionModel;
    
    protected final Set<TrackedRaceChangedListener> raceIsTrackedRaceChangeListener;
    
    protected CellTable<RaceDTO> raceTable;

    private ListDataProvider<RaceDTO> raceList;

    private Iterable<RaceDTO> allRaces;

    private Label noTrackedRacesLabel;

    protected final SailingServiceWriteAsync sailingService;
    protected final ErrorReporter errorReporter;
    protected final Refresher<RegattaDTO> regattaRefresher;
    protected final StringMessages stringMessages;

    private Button btnRefresh;

    protected CustomizableFilterablePanel<RaceDTO> filterablePanelRaces;

    protected TrackedRacesSettings settings;

    private ListBox listBoxRegattas;

    protected final UserService userService;
    
    private final Displayer<RegattaDTO> regattasDisplayer;

    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    public AbstractTrackedRacesListComposite(Component<?> parent, ComponentContext<?> context,
            final Presenter presenter, final StringMessages stringMessages, boolean hasMultiSelection) {
        super(parent, context);
        this.regattasDisplayer = result->fillRegattas(result);
        this.raceIsTrackedRaceChangeListener = new HashSet<TrackedRaceChangedListener>();
        this.sailingService = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.regattaRefresher = presenter.getRegattasRefresher();
        this.multiSelection = hasMultiSelection;
        this.stringMessages = stringMessages;
        this.userService = presenter.getUserService();
    }

    public void setRegattaFilterValue(String regattaName) {
        for (int i = 0; i < listBoxRegattas.getItemCount(); i++) {
            if (listBoxRegattas.getValue(i).equals(regattaName)) {
                listBoxRegattas.setSelectedIndex(i);
                // Firing change event on combobox to filter
                DomEvent.fireNativeEvent(Document.get().createChangeEvent(), listBoxRegattas);
                return;
            }
        }

        // Set 'All' option in case there are no tracked race related to regatta
        if (listBoxRegattas.getItemCount() > 0) {
            listBoxRegattas.setSelectedIndex(0);
            DomEvent.fireNativeEvent(Document.get().createChangeEvent(), listBoxRegattas);
        }
    }
    
    protected void createUI() {
        settings = new TrackedRacesSettings();
        settings.setDelayToLiveInSeconds(DEFAULT_LIVE_DELAY_IN_MILLISECONDS / 1000l);
        VerticalPanel panel = new VerticalPanel();
        initWidget(panel);
        HorizontalPanel filterPanel = new HorizontalPanel();
        panel.add(filterPanel);
        noTrackedRacesLabel = new Label(stringMessages.noRacesYet());
        noTrackedRacesLabel.setWordWrap(false);
        panel.add(noTrackedRacesLabel);
        TableWrapper<RaceDTO, RefreshableSelectionModel<RaceDTO>> raceTableWrapper = new TrackedRacesTableWrapper(sailingService, stringMessages, errorReporter, multiSelection, /* enablePager */ true);
        raceTable = raceTableWrapper.getTable();
        raceTable.setPageSize(1000);
        raceTable.ensureDebugId("TrackedRacesCellTable");
        Label lblFilterRaces = new Label(stringMessages.filterRaces()+":");
        lblFilterRaces.setWordWrap(false);
        lblFilterRaces.getElement().getStyle().setFontWeight(FontWeight.BOLD);
        lblFilterRaces.getElement().getStyle().setMarginRight(10, Unit.PX);
        filterPanel.add(lblFilterRaces);
        filterPanel.setCellVerticalAlignment(lblFilterRaces, HasVerticalAlignment.ALIGN_MIDDLE);
        raceList = raceTableWrapper.getDataProvider();
        filterablePanelRaces = new CustomizableFilterablePanel<RaceDTO>(allRaces, raceList, stringMessages) {
            @Override
            public List<String> getSearchableStrings(RaceDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.boatClass);
                strings.add(t.getRegattaName());
                return strings;
            }

            @Override
            public AbstractCellTable<RaceDTO> getCellTable() {
                return raceTable;
            }
        };
        raceTableWrapper.registerSelectionModelOnNewDataProvider(filterablePanelRaces.getAllListDataProvider());
        Label lblFilterByRegatta = new Label(stringMessages.filterByRegatta());
        lblFilterByRegatta.setWordWrap(false);
        listBoxRegattas = new ListBox();
        listBoxRegattas.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                filterablePanelRaces.filter();
            }
        });
        filterablePanelRaces.add(lblFilterByRegatta, listBoxRegattas, new Filter<RaceDTO>() {
            @Override
            public boolean matches(RaceDTO t) {
                return listBoxRegattas.getSelectedIndex() == 0 /* All */ || Util.equalsWithNull(listBoxRegattas.getSelectedValue(), t.getRegattaName());
            }
            @Override
            public String getName() {
                return "TrackedRacesByRegattaFilter";
            }
        });
        Label lblFilterRacesByName = new Label(stringMessages.filterByNameOrBoatClass() + ":");
        lblFilterRacesByName.setWordWrap(false);
        filterablePanelRaces.add(lblFilterRacesByName);
        filterablePanelRaces.addDefaultTextBox();
        filterablePanelRaces.getTextBox().ensureDebugId("TrackedRacesFilterTextBox");
        filterPanel.add(filterablePanelRaces);
        filterPanel.setCellVerticalAlignment(filterablePanelRaces, HasVerticalAlignment.ALIGN_MIDDLE);
        filterablePanelRaces.setUpdatePermissionFilterForCheckbox(race -> userService.hasPermission(race, DefaultActions.UPDATE));
        // selection model wiring
        refreshableSelectionModel = raceTableWrapper.getSelectionModel();
        setupTableColumns(stringMessages, raceTableWrapper.getColumnSortHandler());
        raceTable.setWidth("100%");
        raceTable.setVisible(false);
        panel.add(raceTableWrapper);
        refreshableSelectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Set<RaceDTO> selectedRaces = refreshableSelectionModel.getSelectedSet();
                makeControlsReactToSelectionChange(selectedRaces);
            }
        });
        HorizontalPanel trackedRacesButtonPanel = new HorizontalPanel();
        trackedRacesButtonPanel.setSpacing(10);
        panel.add(trackedRacesButtonPanel);
        btnRefresh = new Button(stringMessages.refresh());
        btnRefresh.ensureDebugId("RefreshButton");
        btnRefresh.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                regattaRefresher.reloadAndCallFillAll();
            }
        });
        trackedRacesButtonPanel.add(btnRefresh);
        addControlButtons(trackedRacesButtonPanel);
    }
    
    abstract protected void makeControlsReactToSelectionChange(Set<RaceDTO> selectedRaces);

    abstract protected void makeControlsReactToFillRegattas(Iterable<RegattaDTO> regattas);

    abstract protected void addControlButtons(HorizontalPanel trackedRacesButtonPanel);

    private void setupTableColumns(final StringMessages stringMessages, ListHandler<RaceDTO> columnSortHandler) {
        TextColumn<RaceDTO> regattaNameColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return raceDTO.getRegattaName();
            }
        };
        regattaNameColumn.setSortable(true);
        columnSortHandler.setComparator(regattaNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return new NaturalComparator().compare(r1.getRegattaName(), r2.getRegattaName());
            }
        });

        TextColumn<RaceDTO> boatClassNameColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return raceDTO.boatClass == null ? "" : raceDTO.boatClass;
            }
        };
        boatClassNameColumn.setSortable(true);
        columnSortHandler.setComparator(boatClassNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return new NaturalComparator(false).compare(r1.boatClass, r2.boatClass);
            }
        });

        AnchorCell anchorCell = new AnchorCell();
        Column<RaceDTO, SafeHtml> raceNameColumn = new Column<RaceDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(RaceDTO raceDTO) {
                return SafeHtmlUtils.fromString(raceDTO.getName());
            }
        };
        raceNameColumn.setSortable(true);
        columnSortHandler.setComparator(raceNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return new NaturalComparator().compare(r1.getName(), r2.getName());
            }
        });
        TextColumn<RaceDTO> raceStartColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                final String result;
                if (raceDTO.startOfRace != null) {
                    result = DateAndTimeFormatterUtil.defaultDateFormatter.render(raceDTO.startOfRace) + " " + 
                             DateAndTimeFormatterUtil.defaultTimeFormatter.render(raceDTO.startOfRace);
                } else {
                    result = "";
                }
                return result;
            }
        };
        raceStartColumn.setSortable(true);
        columnSortHandler.setComparator(raceStartColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return Comparator.nullsLast(Comparator.<Date>naturalOrder()).compare(r1.startOfRace, r2.startOfRace);
            }
        });
        TextColumn<RaceDTO> hasWindDataColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return (raceDTO.trackedRace != null && raceDTO.trackedRace.hasWindData == true)
                        ? stringMessages.yes()
                        : stringMessages.no();
            }
        };
        hasWindDataColumn.setSortable(true);
        columnSortHandler.setComparator(hasWindDataColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return Boolean.valueOf(hasWindData(r1)).compareTo(hasWindData(r2));
            }

            private boolean hasWindData(RaceDTO race) {
                return race.trackedRace != null && race.trackedRace.hasWindData == true;
            }
        });
        TextColumn<RaceDTO> hasGPSDataColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return (raceDTO.trackedRace != null && raceDTO.trackedRace.hasGPSData == true)
                    ? stringMessages.yes()
                    : stringMessages.no();
            }
        };
        hasGPSDataColumn.setSortable(true);
        columnSortHandler.setComparator(hasGPSDataColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                return Boolean.valueOf(hasGPSData(r1)).compareTo(hasGPSData(r2));
            }

            private boolean hasGPSData(RaceDTO race) {
                return race.trackedRace != null && race.trackedRace.hasGPSData == true;
            }
        });
        TextColumn<RaceDTO> raceStatusColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return raceDTO.status == null ? "" : raceDTO.status.toString();
            }
        };
        raceStatusColumn.setSortable(true);
        columnSortHandler.setComparator(raceStatusColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                if (r1.status != null && r2.status != null) {
                    if (r1.status.status == TrackedRaceStatusEnum.LOADING
                            && r2.status.status == TrackedRaceStatusEnum.LOADING) {
                        return Double.valueOf(r1.status.loadingProgress).compareTo(r2.status.loadingProgress);
                    }
                    return Integer.valueOf(r1.status.status.getOrder()).compareTo(r2.status.status.getOrder());
                }

                return r1.status == null ? (r2.status == null ? 0 : -1) : 1;
            }
        });
        TextColumn<RaceDTO> raceLiveDelayColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                if (raceDTO.isTracked && raceDTO.trackedRace != null && raceDTO.trackedRace.delayToLiveInMs > 0) {
                    return "" + raceDTO.trackedRace.delayToLiveInMs / 1000;
                }
                return "";
            }
        };
        raceLiveDelayColumn.setSortable(true);
        columnSortHandler.setComparator(raceLiveDelayColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO r1, RaceDTO r2) {
                Long r1Delay = getDelay(r1);
                Long r2Delay = getDelay(r2);
                if (r1Delay != null && r2Delay != null) {
                    return r1Delay.compareTo(r2Delay);
                }

                return r1Delay == null ? (r2Delay == null ? 0 : -1) : 1;
            }

            private Long getDelay(RaceDTO race) {
                return race.isTracked && race.trackedRace != null ? race.trackedRace.delayToLiveInMs : null;
            }
        });
        raceTable.addColumn(regattaNameColumn, stringMessages.regatta());
        raceTable.addColumn(boatClassNameColumn, stringMessages.boatClass());
        raceTable.addColumn(raceNameColumn, stringMessages.race());
        raceTable.addColumn(raceStartColumn, stringMessages.startTime());
        raceTable.addColumn(hasWindDataColumn, stringMessages.windData());
        raceTable.addColumn(hasGPSDataColumn, stringMessages.gpsData());
        raceTable.addColumn(raceStatusColumn, stringMessages.status());
        raceTable.addColumn(raceLiveDelayColumn, stringMessages.delayInSeconds());

        SecuredDTOOwnerColumn.configureOwnerColumns(raceTable, columnSortHandler, stringMessages);

        final HasPermissions type = SecuredDomainType.TRACKED_RACE;
        final AccessControlledActionsColumn<RaceDTO, RegattaConfigImagesBarCell> actionsColumn = create(
                new RegattaConfigImagesBarCell(stringMessages), userService);
        final DialogConfig<RaceDTO> config = EditOwnershipDialog.create(userService.getUserManagementWriteService(), type,
                race -> {}, stringMessages);
        actionsColumn.addAction(EventConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, config::openOwnershipDialog);

        final EditACLDialog.DialogConfig<RaceDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, regatta -> {},
                stringMessages);
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);

        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_DELETE, DefaultActions.DELETE,
                this::removeAndUntrackRace);

        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_STOP_TRACKING, DefaultActions.UPDATE,
                this::stopTrackingRace);

        raceTable.addColumn(actionsColumn, stringMessages.actions());
    }

    private void removeAndUntrackRace(final RaceDTO race) {
        final RegattaNameAndRaceName name = (RegattaNameAndRaceName) race.getRaceIdentifier();
        sailingService.removeAndUntrackRaces(Arrays.asList(name),
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages
                                .errorRemovingRace(name != null ? name.toString() : "<null>", caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.reloadAndCallFillAll();
                        for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                            listener.racesRemoved(Arrays.asList(name));
                        }
                    }
                }));
    }

    void stopTrackingRace(final RaceDTO race) {
        final RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier();
        sailingService.stopTrackingRaces(Arrays.asList(raceIdentifier),
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.errorStoppingRaceTracking(
                                raceIdentifier != null ? raceIdentifier.toString() : "<null>", caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.reloadAndCallFillAll();
                        for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                            listener.racesStoppedTracking(Arrays.asList(raceIdentifier));
                        }
                    }
                }));
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public SettingsDialogComponent<TrackedRacesSettings> getSettingsDialogComponent(TrackedRacesSettings settings) {
        return new TrackedRacesSettingsDialogComponent<TrackedRacesSettings>(settings, stringMessages);
    }

    @Override
    public void updateSettings(TrackedRacesSettings newSettings) {
        settings.setDelayToLiveInSeconds(newSettings.getDelayToLiveInSeconds());

        // set the new delay to all selected races
        List<RegattaAndRaceIdentifier> raceIdentifiersToUpdate = new ArrayList<RegattaAndRaceIdentifier>();
        for (RaceDTO raceDTO : refreshableSelectionModel.getSelectedSet()) {
            raceIdentifiersToUpdate.add(raceDTO.getRaceIdentifier());
        }

        if (raceIdentifiersToUpdate != null && !raceIdentifiersToUpdate.isEmpty()) {
            sailingService.updateRacesDelayToLive(raceIdentifiersToUpdate, settings.getDelayToLiveInSeconds() * 1000l,
                    new MarkedAsyncCallback<Void>(
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(
                                            "Exception trying to set the delay to live for the selected tracked races: "
                                                    + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    regattaRefresher.reloadAndCallFillAll();
                                }
                            }
                    ));
        }
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.trackedRaces();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    public void addRaceSelectionChangeHandler(Handler handler) {
        refreshableSelectionModel.addSelectionChangeHandler(handler);
    }

    public RaceDTO getRaceByIdentifier(RaceIdentifier raceIdentifier) {
        RaceDTO result = null;
        if (raceList != null) {
            for (RaceDTO race : raceList.getList()) {
                if (race.getRaceIdentifier().equals(raceIdentifier)) {
                    result = race;
                    break;
                }
            }
        }
        return result;
    }

    public void selectRaceByIdentifier(RegattaAndRaceIdentifier raceIdentifier) {
        if (allRaces != null) {
            for (RaceDTO race : allRaces) {
                String regattaName = race.getRegattaName();
                if (regattaName.equals(raceIdentifier.getRegattaName())
                        && race.getName().equals(raceIdentifier.getRaceName())) {
                    refreshableSelectionModel.setSelected(race, true);
                    break;
                }
            }
        }
    }

    public void clearSelection() {
        refreshableSelectionModel.clear();
    }
    
    public Displayer<RegattaDTO> getRegattasDisplayer() {
        return regattasDisplayer;
    }

    /**
     * @param regattas
     */
    public void fillRegattas(Iterable<RegattaDTO> regattas) {
        makeControlsReactToFillRegattas(regattas);
        displayRaceTableUI(regattas);
        final List<RaceDTO> newAllRaces = new ArrayList<RaceDTO>();
        final List<String> regattaNames = new ArrayList<>();
        for (RegattaDTO regatta : regattas) {
            for (RaceDTO race : regatta.races) {
                if (race != null) {
                    if (raceIsToBeAddedToList(race)) {
                        // We need only those regatta names which are available
                        // at tracking table
                        if (!regattaNames.contains(regatta.getName())) {
                            regattaNames.add(regatta.getName());
                        }
                        newAllRaces.add(race);
                    }
                }
            }
        }
        refreshListBoxRegattas(regattaNames);
        allRaces = newAllRaces;
        filterablePanelRaces.updateAll(allRaces);
    }

    private void refreshListBoxRegattas(List<String> regattaNames) {
        final String lastSelectedRegattaName = listBoxRegattas.getSelectedValue();
        listBoxRegattas.clear();
        listBoxRegattas.addItem(stringMessages.all(), "");
        regattaNames.stream().sorted().forEach(regatta -> listBoxRegattas.addItem(regatta, regatta));
        restoreListBoxRegattasSelection(lastSelectedRegattaName);
    }

    private void restoreListBoxRegattasSelection(String lastSelectedRegattaName) {
        for (int i = 0; i < listBoxRegattas.getItemCount(); i++) {
            if (listBoxRegattas.getValue(i).equals(lastSelectedRegattaName)) {
                listBoxRegattas.setSelectedIndex(i);
                break;
            }
        }
    }

    private void displayRaceTableUI(Iterable<RegattaDTO> regattas) {
        if (Util.isEmpty(regattas)) {
            hideRaceTable();
        } else {
            showRaceTable();
        }
    }

    private void showRaceTable() {
        raceTable.setVisible(true);
        noTrackedRacesLabel.setVisible(false);
    }

    private void hideRaceTable() {
        raceTable.setVisible(false);
        noTrackedRacesLabel.setVisible(true);
    }

    /**
     * Allows applying some sort of filter to the process of adding races. Defaults to true in standard implementation.
     * Override for custom behavior
     */
    protected boolean raceIsToBeAddedToList(RaceDTO race) {
        return true;
    }
    
    public RefreshableSelectionModel<RaceDTO> getSelectionModel() {
        return refreshableSelectionModel;
    }

}
