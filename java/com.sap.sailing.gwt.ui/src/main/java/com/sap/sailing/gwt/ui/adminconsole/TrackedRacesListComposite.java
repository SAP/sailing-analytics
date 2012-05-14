package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RegattaDisplayer;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RaceSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.RaceSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.URLFactory;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RaceDTO;
import com.sap.sailing.gwt.ui.shared.DeprecatedRegattaDTO;

/**
 * Shows the currently tracked events/races in a table. Updated if subscribed as an {@link RegattaDisplayer}, e.g., with
 * the {@link AdminConsoleEntryPoint}.
 */
public class TrackedRacesListComposite extends FormPanel implements RegattaDisplayer, RaceSelectionChangeListener {
    private final Set<TrackedRaceChangedListener> raceIsTrackedRaceChangeListener;

    private final boolean multiSelection;

    private boolean dontFireNextSelectionChangeEvent;

    private final SelectionModel<RaceDTO> selectionModel;

    private CellTable<RaceDTO> raceTable;

    private ListDataProvider<RaceDTO> raceList;
    
    private Iterable<RaceDTO> allRaces;

    private VerticalPanel panel;

    private DateTimeFormatRenderer dateFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT));
    private DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG));

    private Label noTrackedRacesLabel = null;

    private final SailingServiceAsync sailingService;
    private final ErrorReporter errorReporter;
    private final RegattaRefresher regattaRefresher;
    private final RaceSelectionProvider raceSelectionProvider;

    private Button btnUntrack = null;
    private Button btnRefresh = null;
    private Button btnRemoveRace = null;

    private TextBox filterRacesTextbox;

    public static class AnchorCell extends AbstractCell<SafeHtml> {

        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    interface AnchorTemplates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<a href=\"{0}\">{1}</a>")
        SafeHtml cell(String url, String displayName);
    }

    private static AnchorTemplates ANCHORTEMPLATE = GWT.create(AnchorTemplates.class);

    public TrackedRacesListComposite(final SailingServiceAsync sailingService, final ErrorReporter errorReporter,
            final RegattaRefresher regattaRefresher, RaceSelectionProvider raceSelectionProvider,
            StringMessages stringConstants, boolean hasMultiSelection) {
        if (regattaRefresher == null) {
            throw new IllegalArgumentException("regattaRefresher must not be null");
        }
        this.sailingService = sailingService;
        this.errorReporter = errorReporter;
        this.regattaRefresher = regattaRefresher;
        this.multiSelection = hasMultiSelection;
        this.raceSelectionProvider = raceSelectionProvider;
        this.raceIsTrackedRaceChangeListener = new HashSet<TrackedRaceChangedListener>();
        raceList = new ListDataProvider<RaceDTO>();
        selectionModel = multiSelection ? new MultiSelectionModel<RaceDTO>()
                : new SingleSelectionModel<RaceDTO>();
        panel = new VerticalPanel();
        setWidget(panel);
        HorizontalPanel filterPanel = new HorizontalPanel();
        panel.add(filterPanel);
        Label lblFilterEvents = new Label(stringConstants.filterRacesByName() + ":");
        lblFilterEvents.setWordWrap(false);
        filterPanel.setSpacing(5);
        filterPanel.add(lblFilterEvents);
        filterPanel.setCellVerticalAlignment(lblFilterEvents, HasVerticalAlignment.ALIGN_MIDDLE);
        filterRacesTextbox = new TextBox();
        filterRacesTextbox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                fillRaceListFromAvailableRacesApplyingFilter();
            }
        });
        filterPanel.add(filterRacesTextbox);

        noTrackedRacesLabel = new Label(stringConstants.noRacesYet());
        noTrackedRacesLabel.setWordWrap(false);
        panel.add(noTrackedRacesLabel);

        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        raceTable = new CellTable<RaceDTO>(/* pageSize */200, tableRes);
        ListHandler<RaceDTO> columnSortHandler = new ListHandler<RaceDTO>(
                raceList.getList());
        TextColumn<RaceDTO> regattaNameColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return raceDTO.getDeprecatedRegatta().getRegatta().name;
            }
        };
        regattaNameColumn.setSortable(true);

        columnSortHandler.setComparator(regattaNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO t1, RaceDTO t2) {
                RegattaDTO eventOne = t1.getEvent();
                RegattaDTO eventTwo = t2.getEvent();
                boolean ascending = isSortedAscending();
                if (eventOne.name.equals(eventTwo.name)) {
                    return 0;
                }
                int val = -1;
                val = (eventOne != null && eventTwo != null && ascending) ? (eventOne.name.compareTo(eventTwo.name))
                        : -(eventTwo.name.compareTo(eventOne.name));
                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = raceTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });

        TextColumn<RaceDTO> deprecatedRegattaNameColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                return raceDTO.getDeprecatedRegatta().boatClass.name;
            }
        };
        deprecatedRegattaNameColumn.setSortable(true);

        columnSortHandler.setComparator(deprecatedRegattaNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO t1, RaceDTO t2) {
                DeprecatedRegattaDTO regattaOne = t1.getDeprecatedRegatta();
                DeprecatedRegattaDTO regattaTwo = t2.getDeprecatedRegatta();
                boolean ascending = isSortedAscending();
                if (regattaOne.boatClass.name.equals(regattaTwo.boatClass.name)) {
                    return 0;
                }
                int val = -1;
                val = (regattaOne != null && regattaTwo != null && ascending) ? (regattaOne.boatClass.name
                        .compareTo(regattaTwo.boatClass.name)) : -(regattaTwo.boatClass.name
                        .compareTo(regattaOne.boatClass.name));
                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = raceTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });

        AnchorCell anchorCell = new AnchorCell();
        Column<RaceDTO, SafeHtml> raceNameColumn = new Column<RaceDTO, SafeHtml>(anchorCell) {
            @Override
            public SafeHtml getValue(RaceDTO raceDTO) {
                if (raceDTO.currentlyTracked == true) {
                    RegattaNameAndRaceName raceIdentifier = (RegattaNameAndRaceName) raceDTO.getRaceIdentifier();
                    String debugParam = Window.Location.getParameter("gwt.codesvr");
                    String link = URLFactory.INSTANCE.encode("/gwt/RaceBoard.html?raceName="
                            + raceIdentifier.getRaceName() + "&regattaName=" + raceIdentifier.getRegattaName()
                            + ((debugParam != null && !debugParam.isEmpty()) ? "&gwt.codesvr=" + debugParam : ""));
                    return ANCHORTEMPLATE.cell(link, raceDTO.name);
                } else {
                    return SafeHtmlUtils.fromString(raceDTO.name);
                }
            }
        };

        raceNameColumn.setSortable(true);

        columnSortHandler.setComparator(raceNameColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO t1, RaceDTO t2) {
                boolean ascending = isSortedAscending();
                if (t1.name.equals(t2.name)) {
                    return 0;
                }
                int val = -1;
                val = (t1 != null && t2 != null && ascending) ? (t1.name.compareTo(t2.name)) : -(t2.name.compareTo(t1.name));
                return val;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = raceTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });

        TextColumn<RaceDTO> raceStartColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                if (raceDTO.startOfRace != null) {
                    return dateFormatter.render(raceDTO.startOfRace) + " "
                            + timeFormatter.render(raceDTO.startOfRace);
                }

                return "";
            }
        };
        raceStartColumn.setSortable(true);

        columnSortHandler.setComparator(raceStartColumn, new Comparator<RaceDTO>() {
            @Override
            public int compare(RaceDTO t1, RaceDTO t2) {
                if (t1.startOfRace != null && t2.startOfRace != null) {
                    boolean ascending = isSortedAscending();
                    int val = -1;
                    val = (t1.startOfRace.after(t2.startOfRace) && ascending) ? 1 : -1;
                    return val;
                }
                return 0;
            }

            private boolean isSortedAscending() {
                ColumnSortList sortList = raceTable.getColumnSortList();
                return sortList.size() > 0 & sortList.get(0).isAscending();
            }
        });

        TextColumn<RaceDTO> raceTrackedColumn = new TextColumn<RaceDTO>() {
            @Override
            public String getValue(RaceDTO raceDTO) {
                if (raceDTO.currentlyTracked == true)
                    return "tracked";

                return "";
            }
        };

        raceTable.addColumn(regattaNameColumn, stringConstants.event());
        raceTable.addColumn(deprecatedRegattaNameColumn, stringConstants.regatta());
        raceTable.addColumn(raceNameColumn, stringConstants.race());
        raceTable.addColumn(raceStartColumn, stringConstants.startTime());
        raceTable.addColumn(raceTrackedColumn, stringConstants.tracked());
        raceTable.setWidth("300px");
        raceTable.setSelectionModel(selectionModel);
        raceTable.setVisible(false);
        panel.add(raceTable);
        raceList.addDataDisplay(raceTable);
        raceTable.addColumnSortHandler(columnSortHandler);
        raceTable.getSelectionModel().addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<RaceDTO> selectedRaces = getSelectedRaces();
                if (selectedRaces.isEmpty()) {
                    btnRemoveRace.setEnabled(false);
                    btnUntrack.setEnabled(false);
                } else {
                    btnRemoveRace.setEnabled(true);
                    btnUntrack.setEnabled(true);
                }
                if (dontFireNextSelectionChangeEvent) {
                    dontFireNextSelectionChangeEvent = false;
                } else {
                    List<RegattaAndRaceIdentifier> selectedRaceIdentifiers = new ArrayList<RegattaAndRaceIdentifier>();
                    for (RaceDTO selectedRace : selectedRaces) {
                        selectedRaceIdentifiers.add(selectedRace.getRaceIdentifier());
                    }
                    TrackedRacesListComposite.this.raceSelectionProvider.setSelection(selectedRaceIdentifiers, TrackedRacesListComposite.this);
                }
            }
        });
        HorizontalPanel trackedRacesButtonPanel = new HorizontalPanel();
        trackedRacesButtonPanel.setSpacing(10);
        panel.add(trackedRacesButtonPanel);
        btnRemoveRace = new Button(stringConstants.remove());
        btnRemoveRace.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                for (RaceDTO selectedRace : getSelectedRaces()) {
                    removeAndUntrackRace(selectedRace);
                }
            }
        });
        btnRemoveRace.setEnabled(false);
        trackedRacesButtonPanel.add(btnRemoveRace);
        btnUntrack = new Button(stringConstants.stopTracking());
        btnUntrack.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent click) {
                for (RaceDTO selectedRace : getSelectedRaces()) {
                    if (selectedRace.currentlyTracked) {
                        stopTrackingRace(selectedRace);
                    }
                }
            }
        });
        btnUntrack.setEnabled(false);
        trackedRacesButtonPanel.add(btnUntrack);

        btnRefresh = new Button(stringConstants.refresh());
        btnRefresh.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                regattaRefresher.fillRegattas();
            }
        });
        trackedRacesButtonPanel.add(btnRefresh);
    }

    private List<RaceDTO> getSelectedRaces() {
        List<RaceDTO> result = new ArrayList<RaceDTO>();
        if (raceList != null) {
            for (RaceDTO race : raceList.getList()) {
                if (selectionModel.isSelected(race)) {
                    result.add(race);
                }
            }
        }
        return result;
    }

    public void selectRaceByIdentifier(RaceIdentifier raceIdentifier) {
        if (raceList != null) {
            for (RaceDTO race : raceList.getList()) {
                RegattaDTO event = race.getEvent();
                if (event.name.equals(raceIdentifier.getRegattaName()) && race.name.equals(raceIdentifier.getRaceName())) {
                    dontFireNextSelectionChangeEvent = true;
                    selectionModel.setSelected(race, true);
                    break;
                }
            }
        }
    }

    public void clearSelection() {
        List<RegattaAndRaceIdentifier> emptySelection =  Collections.emptyList();
        raceSelectionProvider.setSelection(emptySelection, /* listenersNotToNotify */ this);
        if (raceList != null) {
            for (RaceDTO race : raceList.getList()) {
                selectionModel.setSelected(race, false);
            }
        }
    }

    @Override
    public void fillRegattas(List<RegattaDTO> events) {
        if (events.isEmpty()) {
            raceTable.setVisible(false);
            btnUntrack.setVisible(false);
            btnRemoveRace.setVisible(false);
            noTrackedRacesLabel.setVisible(true);
        } else {
            raceTable.setVisible(true);
            btnUntrack.setVisible(true);
            btnUntrack.setEnabled(false);
            btnRemoveRace.setVisible(true);
            btnRemoveRace.setEnabled(false);
            noTrackedRacesLabel.setVisible(false);
        }
        List<RaceDTO> newAllRaces = new ArrayList<RaceDTO>();
        List<RegattaAndRaceIdentifier> newAllRaceIdentifiers = new ArrayList<RegattaAndRaceIdentifier>();
        for (RegattaDTO regatta : events) {
            for (RaceDTO race : regatta.races) {
                if (race != null) {
                    newAllRaces.add(race);
                    newAllRaceIdentifiers.add(race.getRaceIdentifier());
                }
            }
        }
        allRaces = newAllRaces;
        fillRaceListFromAvailableRacesApplyingFilter();
        raceSelectionProvider.setAllRaces(newAllRaceIdentifiers); // have this object be notified; triggers onRaceSelectionChange
    }

    public void addRaceSelectionChangeListener(RaceSelectionChangeListener listener) {
        this.raceSelectionProvider.addRaceSelectionChangeListener(listener);
    }
    
    public void addTrackedRaceChangeListener(TrackedRaceChangedListener listener) {
        this.raceIsTrackedRaceChangeListener.add(listener);
    }

    private void stopTrackingRace(final RaceDTO race) {
        final RegattaNameAndRaceName regattaNameAndRaceName = (RegattaNameAndRaceName) race.getRaceIdentifier();
        sailingService.stopTrackingRace(regattaNameAndRaceName, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Exception trying to stop tracking race " + race.name + "in event "
                        + race.getEvent().name + ": " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                regattaRefresher.fillRegattas();
                for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                    listener.changeTrackingRace(regattaNameAndRaceName, false);
                }
            }
        });
    }

    private void removeAndUntrackRace(final RaceDTO race) {
        final RegattaNameAndRaceName regattaNameAndRaceName = (RegattaNameAndRaceName) race.getRaceIdentifier();
        sailingService.removeAndUntrackRace(regattaNameAndRaceName,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Exception trying to stop tracking race " + race.name + "in event "
                                + race.getEvent().name + ": " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.fillRegattas();
                        for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                            listener.changeTrackingRace(regattaNameAndRaceName, false);
                        }
                    }
                });
    }

    private void fillRaceListFromAvailableRacesApplyingFilter() {
        String text = filterRacesTextbox.getText();
        List<String> wordsToFilter = Arrays.asList(text.split(" "));
        raceList.getList().clear();
        if (text != null && !text.isEmpty()) {
            for (RaceDTO raceDTO : getAllRaces()) {
                boolean failed = false;
                for (String word : wordsToFilter) {
                    String textAsUppercase = word.toUpperCase().trim();
                    if (!raceDTO.getEvent().name.toUpperCase().contains(textAsUppercase)
                            && !raceDTO.getDeprecatedRegatta().boatClass.name.toUpperCase().contains(textAsUppercase)
                            && !raceDTO.name.toUpperCase().contains(textAsUppercase)) {
                        failed = true;
                        break;
                    }
                }
                if (!failed) {
                    raceList.getList().add(raceDTO);
                }
            }
        } else {
            for (RaceDTO raceFromAllRaces : getAllRaces()) {
                raceList.getList().add(raceFromAllRaces);
            }
        }
        // now sort again according to selected criterion
        ColumnSortEvent.fire(raceTable, raceTable.getColumnSortList());
        onRaceSelectionChange(raceSelectionProvider.getSelectedRaces()); // update selection based on underlying domain race selection model
    }

    @Override
    public void onRaceSelectionChange(List<RegattaAndRaceIdentifier> selectedRaces) {
        for (RaceDTO raceFromAllRaces : raceList.getList()) {
            selectionModel.setSelected(raceFromAllRaces, selectedRaces.contains(raceFromAllRaces.getRaceIdentifier()));
        }
    }

    private Iterable<RaceDTO> getAllRaces() {
        return allRaces;
    }
}
